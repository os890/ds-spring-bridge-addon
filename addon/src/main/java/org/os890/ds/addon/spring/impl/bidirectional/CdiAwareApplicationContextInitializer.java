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
import org.os890.ds.addon.spring.impl.CdiAwareBeanFactoryPostProcessor;
import org.os890.ds.addon.spring.impl.SpringBridgeExtension;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

//only needed for using a two-way-bridge in web-apps - web.xml config:
/*
    <context-param>
        <param-name>contextInitializerClasses</param-name>
        <param-value>org.os890.ds.addon.spring.impl.bidirectional.CdiAwareApplicationContextInitializer</param-value>
    </context-param>
 */
//alternative: use WebappAwareSpringContainerManager -> no web.xml config is needed
@Exclude
public class CdiAwareApplicationContextInitializer implements ApplicationContextInitializer
{
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext)
    {
        applicationContext.addBeanFactoryPostProcessor(new CdiAwareBeanFactoryPostProcessor(
            SpringBridgeExtension.getBeanManager(), SpringBridgeExtension.getCdiBeans()));
    }
}
