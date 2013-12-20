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

import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.deltaspike.core.api.exclude.Exclude;
import org.os890.ds.addon.spring.spi.BeanFilter;

@Exclude
public class DefaultBeanFilter implements BeanFilter
{
    private final boolean exposeDeltaSpikeBeans;
    private final boolean exposeInternalSpringBeans;

    public DefaultBeanFilter()
    {
        this.exposeDeltaSpikeBeans = Boolean.parseBoolean(ConfigResolver.getPropertyValue("exposeDeltaSpikeBeansToSpring", "true"));
        this.exposeInternalSpringBeans = Boolean.parseBoolean(ConfigResolver.getPropertyValue("exposeInternalSpringBeansToCdi", "false"));
    }

    @Override
    public boolean exposeCdiBeanToSpring(Class<?> beanClass)
    {
        return !beanClass.getName().startsWith("org.os890.ds.addon.spring") &&
                !(!exposeDeltaSpikeBeans && beanClass.getName().startsWith("org.apache.deltaspike."));
    }

    @Override
    public boolean exposeSpringBeanToCdi(Class<?> beanClass)
    {
        return !(!exposeInternalSpringBeans && beanClass.getName().startsWith("org.springframework."));
    }
}
