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
import org.apache.deltaspike.core.util.metadata.builder.ContextualLifecycle;
import org.springframework.context.ConfigurableApplicationContext;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

@Exclude
class SpringAwareBeanLifecycle implements ContextualLifecycle
{
    private final ConfigurableApplicationContext applicationContext;
    private final String beanName; //unique spring-bean-id
    private final boolean prototypeScope;

    public SpringAwareBeanLifecycle(ConfigurableApplicationContext applicationContext, String beanName, String scope)
    {
        this.applicationContext = applicationContext;
        this.beanName = beanName;
        this.prototypeScope = "prototype".equalsIgnoreCase(scope);
    }

    @Override
    public Object create(Bean bean, CreationalContext creationalContext)
    {
        return this.applicationContext.getBean(this.beanName);
    }

    //currently only works in combination with direct lookups (see releasePrototypeSpringBeanTest)
    // and not if it is dependent to a bean which gets destroyed -> workaround use manual lookup + DependentProvider#destroy
    @Override
    public void destroy(Bean bean, Object instance, CreationalContext creationalContext)
    {
        //only trigger destroy of prototype-beans - spring is responsible for the rest
        if (this.prototypeScope)
        {
            this.applicationContext.getBeanFactory().destroyBean(this.beanName, instance);
        }
    }
}
