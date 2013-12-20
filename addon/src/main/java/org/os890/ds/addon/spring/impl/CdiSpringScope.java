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
import org.apache.deltaspike.core.api.provider.BeanManagerProvider;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.util.Collections;
import java.util.Map;

@Exclude
public class CdiSpringScope implements Scope
{
    private volatile BeanManager beanManager;
    private final BeanManager fallbackBeanManager;
    private final Map<String, Bean<?>> cdiBeansForSpringRegistration;

    public CdiSpringScope(BeanManager beanManager, Map<String, Bean<?>> cdiBeansForSpringRegistration)
    {
        this.fallbackBeanManager = beanManager;
        this.cdiBeansForSpringRegistration = Collections.unmodifiableMap(cdiBeansForSpringRegistration);
    }

    public Object get(String name, ObjectFactory objectFactory)
    {
        Bean<?> bean = this.cdiBeansForSpringRegistration.get(name);

        if (bean == null && name.contains("#"))
        {
            name = name.substring(0, name.indexOf("#")); //needed for ApplicationContext#getBean(Class)
            bean = this.cdiBeansForSpringRegistration.get(name);
        }

        if (bean == null)
        {
            return null;
        }

        BeanManager currentBeanManager = findCurrentBeanManager();

        CreationalContext<?> context = currentBeanManager.createCreationalContext(bean);
        return currentBeanManager.getReference(bean, bean.getBeanClass(), context);
    }

    private BeanManager findCurrentBeanManager()
    {
        try
        {
            if (this.beanManager == null)
            {
                //not available during bootstrapping but afterwards it's the correct one
                // (some weld based servers use a different one after the bootstrapping process)
                this.beanManager = BeanManagerProvider.getInstance().getBeanManager();
            }
        }
        catch (Exception e)
        {
            return this.fallbackBeanManager;
        }
        return this.beanManager;
    }

    public Object remove(String name)
    {
        //not needed
        //cdi will handle normal-scoped beans and for dependent-scoped beans we don't have the instance to destroy
        return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback)
    {
        //not needed - done by the cdi-container
    }

    @Override
    public Object resolveContextualObject(String key)
    {
        return null; //not needed
    }

    @Override
    public String getConversationId()
    {
        return null; //not needed
    }
}
