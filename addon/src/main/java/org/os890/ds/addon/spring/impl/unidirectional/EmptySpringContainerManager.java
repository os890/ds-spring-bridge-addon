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
package org.os890.ds.addon.spring.impl.unidirectional;

import org.apache.deltaspike.core.api.exclude.Exclude;
import org.os890.ds.addon.spring.impl.SpringBridgeExtension;
import org.os890.ds.addon.spring.spi.SpringContainerManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.util.HashMap;
import java.util.Map;

//optional - configure it if a one-way bridge (injection of cdi-beans in spring-beans) is enough.
//ensure that the cdi-container gets started before the spring-container
@Exclude
public class EmptySpringContainerManager implements SpringContainerManager
{
    private static Map<String, Bean<?>> foundCdiBeans;
    private static BeanManager foundBeanManager;

    private ConfigurableApplicationContext context;

    @Override
    public boolean isContainerStarted()
    {
        return this.context != null;
    }

    @Override
    public ConfigurableApplicationContext getStartedContainer()
    {
        return this.context;
    }

    @Override
    public ConfigurableApplicationContext bootContainer(BeanFactoryPostProcessor... beanFactoryPostProcessors)
    {
        foundCdiBeans = new HashMap<String, Bean<?>>(SpringBridgeExtension.getCdiBeans());
        foundBeanManager = SpringBridgeExtension.getBeanManager();

        this.context = new AbstractApplicationContext()
        {
            @Override
            protected void refreshBeanFactory() throws BeansException, IllegalStateException
            {
            }

            @Override
            protected void closeBeanFactory()
            {
            }

            @Override
            public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException
            {
                return new DefaultListableBeanFactory()
                {
                    @Override
                    public String[] getBeanDefinitionNames()
                    {
                        return new String[]{};
                    }
                };
            }
        };
        return this.context;
    }

    static Map<String, Bean<?>> getFoundCdiBeans()
    {
        return foundCdiBeans;
    }

    static BeanManager getFoundBeanManager()
    {
        return foundBeanManager;
    }
}
