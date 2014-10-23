/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.os890.ds.addon.spring.impl;

import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.util.ClassUtils;
import org.apache.deltaspike.core.util.ExceptionUtils;
import org.apache.deltaspike.core.util.ServiceUtils;
import org.apache.deltaspike.core.util.bean.BeanBuilder;
import org.apache.deltaspike.core.util.bean.ImmutablePassivationCapableBean;
import org.apache.deltaspike.core.util.metadata.AnnotationInstanceProvider;
import org.apache.deltaspike.core.util.metadata.builder.AnnotatedTypeBuilder;
import org.apache.deltaspike.core.util.metadata.builder.ContextualLifecycle;
import org.os890.ds.addon.spring.impl.bidirectional.ManualSpringContainerManager;
import org.os890.ds.addon.spring.spi.BeanFilter;
import org.os890.ds.addon.spring.spi.SpringContainerManager;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

//other scopes than singleton and prototype require a proper proxy-config (ScopedProxyMode.TARGET_CLASS) for spring
public class SpringBridgeExtension implements Extension
{
    private List<Bean<?>> cdiBeansForSpring = new ArrayList<Bean<?>>();
    private static ThreadLocal<BeanFactoryPostProcessor> currentBeanFactoryPostProcessor = new ThreadLocal<BeanFactoryPostProcessor>();

    private EditableConfigurableApplicationContextProxy springContext;

    private List<BeanFilter> beanFilterList = new ArrayList<BeanFilter>();

    public void init(@Observes BeforeBeanDiscovery beforeBeanDiscovery)
    {
        beanFilterList.addAll(ServiceUtils.loadServiceImplementations(BeanFilter.class));
    }

    //for supporting producers ProcessBean would be needed
    //however later on Bean#getBeanClass is used which returns the producer-class and not the return-type of the producer (like #getTypes)
    public void recordBeans(@Observes ProcessBean pb)
    {
        Bean bean = pb.getBean();

        if (!isSpringAdapterBean(bean) && !isFilteredCdiBean(bean.getBeanClass()))
        {
            this.cdiBeansForSpring.add(bean);
        }
    }

    private boolean isFilteredCdiBean(Class beanClass)
    {
        for (BeanFilter beanFilter : this.beanFilterList)
        {
            if (!beanFilter.exposeCdiBeanToSpring(beanClass))
            {
                return true;
            }
        }
        return false;
    }

    //TODO improve ds -> simplify this method
    private boolean isSpringAdapterBean(Bean bean) //don't add spring-bean adapters back to spring
    {
        if (bean instanceof ImmutablePassivationCapableBean)
        {
            for (Field field : bean.getClass().getSuperclass().getDeclaredFields())
            {
                if (ContextualLifecycle.class.isAssignableFrom(field.getType()))
                {
                    field.setAccessible(true);

                    try
                    {
                        if (field.get(bean) instanceof SpringAwareBeanLifecycle)
                        {
                            return true;
                        }
                    }
                    catch (IllegalAccessException e)
                    {
                        throw ExceptionUtils.throwAsRuntimeException(e);
                    }
                }
            }
        }
        return false;
    }

    public void initContainerBridge(@Observes AfterBeanDiscovery abd, BeanManager beanManager)
    {
        this.springContext = new EditableConfigurableApplicationContextProxy(resolveSpringContext(abd, beanManager));

        if (this.springContext == null)
        {
            abd.addDefinitionError(new IllegalStateException("no spring-context found/created"));
            return;
        }

        for (String beanName : this.springContext.getBeanDefinitionNames())
        {
            BeanDefinition beanDefinition = this.springContext.getBeanFactory().getBeanDefinition(beanName);

            String name = beanDefinition.getBeanClassName();

            if (name == null) //can be null in case of config-files as spring bean
            {
                continue;
            }

            Class<?> beanClass = ClassUtils.tryToLoadClassForName(name);

            if (CdiSpringScope.class.getName().equals(beanDefinition.getScope()) || isFilteredSpringBean(beanClass))
            {
                continue; //don't add cdi-beans registered in spring back to cdi
            }

            abd.addBean(createBeanAdapter(beanClass, beanName, beanDefinition, this.springContext, beanManager, abd));
        }

        this.beanFilterList.clear();
        this.cdiBeansForSpring.clear();
    }

    private boolean isFilteredSpringBean(Class<?> beanClass)
    {
        for (BeanFilter beanFilter : this.beanFilterList)
        {
            if (!beanFilter.exposeSpringBeanToCdi(beanClass))
            {
                return true;
            }
        }
        return false;
    }

    private ConfigurableApplicationContext resolveSpringContext(AfterBeanDiscovery abd, BeanManager beanManager)
    {
        List<SpringContainerManager> scmList = ServiceUtils.loadServiceImplementations(SpringContainerManager.class);

        //TODO cleanup
        Map<String, Bean<?>> cdiBeansForSpringMap = new HashMap<String, Bean<?>>();
        for (Bean<?> bean : this.cdiBeansForSpring)
        {
            if (bean.getName() != null)
            {
                cdiBeansForSpringMap.put(bean.getName(), bean);
            }

            Set<Type> beanTypes = new HashSet<Type>(bean.getTypes());
            beanTypes.remove(Object.class);
            beanTypes.remove(Serializable.class);

            Type beanType = beanTypes.size() == 1 ? beanTypes.iterator().next() : null;

            if (beanType instanceof Class) { //to support producers
                cdiBeansForSpringMap.put(((Class) beanType).getName(), bean);
            } else { //fallback since spring doesn't support multiple types
                cdiBeansForSpringMap.put(bean.getBeanClass().getName(), bean);
            }
        }


        BeanFactoryPostProcessor beanFactoryPostProcessor =  new CdiAwareBeanFactoryPostProcessor(beanManager, cdiBeansForSpringMap);

        try
        {
            currentBeanFactoryPostProcessor.set(beanFactoryPostProcessor);

            if (scmList.isEmpty())
            {
                return null;
            }
            if (scmList.size() == 1)
            {
                return scmList.iterator().next().bootContainer(beanFactoryPostProcessor);
            }
            if (scmList.size() > 2)
            {
                abd.addDefinitionError(new IllegalStateException(scmList.size() + " spring-context-resolvers found"));
            }
            else //2 are found -> use the custom one
            {
                for (SpringContainerManager containerManager : scmList)
                {
                    if (containerManager instanceof ManualSpringContainerManager)
                    {
                        continue;
                    }

                    if (containerManager.isContainerStarted())
                    {
                        return containerManager.getStartedContainer();
                    }
                    return containerManager.bootContainer(beanFactoryPostProcessor);
                }
            }
            return null;
        }
        finally
        {
            currentBeanFactoryPostProcessor.set(null);
            currentBeanFactoryPostProcessor.remove();
        }
    }

    private <T> Bean<T> createBeanAdapter(Class<T> beanClass, String beanName, BeanDefinition beanDefinition,
                                          ConfigurableApplicationContext applicationContext, BeanManager bm,
                                          AfterBeanDiscovery abd)
    {
        String beanScope = beanDefinition.getScope();
        ContextualLifecycle lifecycleAdapter = new SpringAwareBeanLifecycle(applicationContext, beanName, beanScope);

        List<Annotation> cdiQualifiers = tryToMapToCdiQualifier(beanName, beanDefinition, abd);

        //we don't need to handle (remove) interceptor annotations, because BeanBuilder >won't< add them (not supported)
        BeanBuilder<T> beanBuilder = new BeanBuilder<T>(bm)
                .readFromType(new AnnotatedTypeBuilder<T>().readFromType(beanClass).create())
                .name(beanName)
                .beanLifecycle(lifecycleAdapter)
                .injectionPoints(Collections.<InjectionPoint>emptySet())
                .scope(Dependent.class) //the instance (or proxy) returned by spring shouldn't bootContainer proxied
                .passivationCapable(true)
                .alternative(false)
                .nullable(true);

        if (!cdiQualifiers.isEmpty())
        {
            beanBuilder.addQualifiers(cdiQualifiers);
        }

        boolean typeObjectFound = false;
        for (Type type : beanBuilder.getTypes())
        {
            if (Object.class.equals(type))
            {
                typeObjectFound = true;
            }
        }

        if (!typeObjectFound)
        {
            beanBuilder.addType(Object.class); //java.lang.Object needs to be present (as type) in any case
        }

        return beanBuilder.create();
    }

    //TODO test it
    private List<Annotation> tryToMapToCdiQualifier(String beanName,
                                                    BeanDefinition beanDefinition,
                                                    AfterBeanDiscovery abd)
    {
        List<Annotation> cdiQualifiers = new ArrayList<Annotation>();
        if (beanDefinition instanceof AbstractBeanDefinition)
        {
            boolean unsupportedQualifierFound = false;
            for (AutowireCandidateQualifier springQualifier : ((AbstractBeanDefinition) beanDefinition).getQualifiers())
            {
                Class qualifierClass = ClassUtils.tryToLoadClassForName(springQualifier.getTypeName());

                if (qualifierClass == null)
                {
                    unsupportedQualifierFound = true;
                    break;
                }

                if (Annotation.class.isAssignableFrom(qualifierClass))
                {
                    Map<String, Object> qualifierValues = new HashMap<String, Object>();
                    String methodName;
                    Object methodValue;
                    for (Method annotationMethod : qualifierClass.getDeclaredMethods())
                    {
                        methodName = annotationMethod.getName();
                        methodValue = springQualifier.getAttribute(methodName);

                        if (methodValue != null)
                        {
                            qualifierValues.put(methodName, methodValue);
                        }

                    }

                    cdiQualifiers.add(AnnotationInstanceProvider.of(qualifierClass, qualifierValues));
                }
                else
                {
                    unsupportedQualifierFound = true;
                    break;
                }
            }
            if (unsupportedQualifierFound)
            {
                abd.addDefinitionError(new IllegalStateException(beanName + " can't be added"));
            }
        }
        return cdiQualifiers;
    }

    ApplicationContext getApplicationContext()
    {
        return springContext;
    }

    //only allowed after the bootstrapping process
    //needed by other bridges which have to merge different context instances
    public static void updateSpringContext(ConfigurableApplicationContext springContext)
    {
        ApplicationContext context = BeanProvider.getContextualReference(SpringBridgeExtension.class).getApplicationContext();

        if (context instanceof EditableConfigurableApplicationContextProxy)
        {
            ((EditableConfigurableApplicationContextProxy)context).setWrapped(springContext);
        }
    }

    public static BeanFactoryPostProcessor getBeanFactoryPostProcessor()
    {
        return currentBeanFactoryPostProcessor.get();
    }
}
