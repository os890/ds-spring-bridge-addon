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
package org.os890.ds.addon.spring.impl.bidirectional;

import org.apache.deltaspike.core.api.exclude.Exclude;
import org.os890.ds.addon.spring.spi.SpringContainerManager;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ContextLoader;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Map;

@Exclude
public class WebappAwareSpringContainerManager implements SpringContainerManager, ServletContextListener
{
    private ContextLoader springContextLoader = new ContextLoader();
    private ConfigurableApplicationContext configurableApplicationContext;
    private MockServletContext servletContext = new MockServletContext(); //needed because spring gets bootstrapped during the cdi-bootstrapping

    public WebappAwareSpringContainerManager()
    {
        servletContext.setAttribute("contextInitializerClasses", CdiAwareApplicationContextInitializer.class.getName());
        addCustomAttributes(servletContext);
    }

    protected void addCustomAttributes(ServletContext servletContext)
    {
        //override if needed
    }

    @Override
    public boolean isContainerStarted()
    {
        return this.configurableApplicationContext != null;
    }

    @Override
    public ConfigurableApplicationContext getStartedContainer()
    {
        return configurableApplicationContext;
    }

    @Override
    public ConfigurableApplicationContext bootContainer(BeanFactoryPostProcessor... beanFactoryPostProcessors)
    {
        configurableApplicationContext = (ConfigurableApplicationContext)
            springContextLoader.initWebApplicationContext(this.servletContext);

        //add spring-specific config-params here (don't use the web.xml)

        return configurableApplicationContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        //add (spring-)attributes to real context
        for (Map.Entry<String, String> entry : servletContext.getAttributes().entrySet())
        {
            event.getServletContext().setAttribute(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        springContextLoader.closeWebApplicationContext(sce.getServletContext());
    }
}
