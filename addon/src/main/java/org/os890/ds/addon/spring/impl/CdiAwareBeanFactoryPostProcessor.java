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

import org.apache.deltaspike.core.api.exclude.Exclude;
import org.apache.deltaspike.core.util.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.Nonbinding;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Exclude
public class CdiAwareBeanFactoryPostProcessor implements BeanFactoryPostProcessor
{
    private final BeanManager beanManager;
    private Map<String, Bean<?>> cdiBeansForSpringRegistration = new HashMap<String, Bean<?>>();

    public CdiAwareBeanFactoryPostProcessor(BeanManager beanManager, Map<String, Bean<?>> cdiBeansForSpringRegistration)
    {
        this.beanManager = beanManager;
        this.cdiBeansForSpringRegistration = new HashMap<String, Bean<?>>(cdiBeansForSpringRegistration);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;

        try
        {
            for (Bean<?> bean : this.cdiBeansForSpringRegistration.values())
            {
                BeanDefinition beanDefinition = createSpringBeanDefinition(bean);
                String beanName = createBeanName(bean, beanDefinition, beanDefinitionRegistry);
                beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
            }

            beanFactory.registerScope(CdiSpringScope.class.getName(), new CdiSpringScope(this.beanManager, this.cdiBeansForSpringRegistration));
        }
        catch (Exception e)
        {
            throw ExceptionUtils.throwAsRuntimeException(e);
        }
    }

    private BeanDefinition createSpringBeanDefinition(Bean<?> cdiBean) throws Exception
    {
        AbstractBeanDefinition beanDefinition = new GenericBeanDefinition();
        Set<Type> beanTypes = new HashSet<Type>(cdiBean.getTypes());
        beanTypes.remove(Object.class);
        beanTypes.remove(Serializable.class);

        Type beanType = beanTypes.size() == 1 ? beanTypes.iterator().next() : null;

        if (beanType instanceof Class) { //to support producers
            beanDefinition.setBeanClass((Class)beanType);
        } else { //fallback since spring doesn't support multiple types
            beanDefinition.setBeanClass(cdiBean.getBeanClass());
        }

        beanDefinition.setScope(CdiSpringScope.class.getName());

        for (Annotation qualifier : cdiBean.getQualifiers())
        {
            if (Any.class.equals(qualifier.annotationType()) || Default.class.equals(qualifier.annotationType()))
            {
                continue;
            }
            //currently only simple qualifiers are supported
            AutowireCandidateQualifier springQualifier = new AutowireCandidateQualifier(qualifier.annotationType());

            for (Method annotationMethod : qualifier.annotationType().getDeclaredMethods())
            {
                if (!annotationMethod.isAnnotationPresent(Nonbinding.class))
                {
                    springQualifier.setAttribute(annotationMethod.getName(), annotationMethod.invoke(qualifier));
                }
            }
            beanDefinition.addQualifier(springQualifier);
        }
        beanDefinition.setLazyInit(true);
        return beanDefinition;
    }

    private String createBeanName(Bean<?> bean, BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
    {
        String beanName = bean.getName();

        if (beanName == null)
        {
            return BeanDefinitionReaderUtils.generateBeanName(beanDefinition, registry);
        }
        return beanName;
    }
}
