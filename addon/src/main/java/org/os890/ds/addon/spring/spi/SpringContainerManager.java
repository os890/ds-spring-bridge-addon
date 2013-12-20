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
package org.os890.ds.addon.spring.spi;

import org.apache.deltaspike.core.spi.activation.Deactivatable;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

public interface SpringContainerManager extends Deactivatable
{
    /**
     * If the container was started externally, the bridge just works in one direction
     * (injection of spring-beans into cdi beansI
     * @return true if a container is started externally, false otherwise
     */
    boolean isContainerStarted();

    ConfigurableApplicationContext getStartedContainer();

    ConfigurableApplicationContext bootContainer(BeanFactoryPostProcessor... beanFactoryPostProcessors);
}
