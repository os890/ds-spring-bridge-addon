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
package org.os890.test.ds.addon.spring;

import junit.framework.Assert;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.api.provider.DependentProvider;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.os890.test.ds.addon.spring.bean.cdi.ApplicationScopedCdiBean;
import org.os890.test.ds.addon.spring.bean.cdi.DependentCdiBean;
import org.os890.test.ds.addon.spring.bean.spring.NamedSpringBean;
import org.os890.test.ds.addon.spring.bean.spring.PrototypeSpringBean;
import org.os890.test.ds.addon.spring.bean.spring.SingletonSpringBean;

import javax.inject.Inject;

@RunWith(CdiTestRunner.class)
public class SpringToCdiTest
{
    @Inject
    private ApplicationScopedCdiBean applicationScopedCdiBean;

    @Test
    public void directSpringBeanLookupTest()
    {
        Assert.assertNotNull(BeanProvider.getContextualReference(SingletonSpringBean.class));
        Assert.assertNotNull(BeanProvider.getContextualReference(PrototypeSpringBean.class));
        Assert.assertNotNull(BeanProvider.getContextualReference(NamedSpringBean.class));
    }

    @Test
    public void applicationScopedCdiBeanTest()
    {
        Assert.assertNotNull(this.applicationScopedCdiBean);
        Assert.assertNotNull(this.applicationScopedCdiBean.getSingletonSpringBean());
        Assert.assertNotNull(this.applicationScopedCdiBean.getPrototypeSpringBean());
        Assert.assertNotNull(this.applicationScopedCdiBean.getNamedSpringBean());

        String singletonSpringBeanValue = this.applicationScopedCdiBean.getSingletonSpringBean().getValue();
        String prototypeSpringBeanValue = this.applicationScopedCdiBean.getPrototypeSpringBean().getValue();
        String namedSpringBeanValue = this.applicationScopedCdiBean.getNamedSpringBean().getValue();

        Assert.assertNotNull(singletonSpringBeanValue);
        Assert.assertNotNull(prototypeSpringBeanValue);
        Assert.assertNotNull(namedSpringBeanValue);

        //values won't change with additional lookups
        Assert.assertEquals(singletonSpringBeanValue,
            BeanProvider.getContextualReference(ApplicationScopedCdiBean.class).getSingletonSpringBean().getValue());
        Assert.assertEquals(prototypeSpringBeanValue,
            BeanProvider.getContextualReference(ApplicationScopedCdiBean.class).getPrototypeSpringBean().getValue());
        Assert.assertEquals(namedSpringBeanValue,
            BeanProvider.getContextualReference(ApplicationScopedCdiBean.class).getNamedSpringBean().getValue());
    }

    @Test
    public void dependentScopedCdiBeanTest()
    {
        DependentProvider<DependentCdiBean> dependentCdiBeanHolder = BeanProvider.getDependent(DependentCdiBean.class);
        DependentCdiBean dependentCdiBean = dependentCdiBeanHolder.get();

        Assert.assertNotNull(dependentCdiBean);
        Assert.assertNotNull(dependentCdiBean.getSingletonSpringBean());
        Assert.assertNotNull(dependentCdiBean.getPrototypeSpringBean());
        Assert.assertNotNull(dependentCdiBean.getNamedSpringBean());

        String singletonSpringBeanValue = dependentCdiBean.getSingletonSpringBean().getValue();
        String prototypeSpringBeanValue = dependentCdiBean.getPrototypeSpringBean().getValue();
        String namedSpringBeanValue = dependentCdiBean.getNamedSpringBean().getValue();

        Assert.assertNotNull(singletonSpringBeanValue);
        Assert.assertNotNull(prototypeSpringBeanValue);
        Assert.assertNotNull(namedSpringBeanValue);

        dependentCdiBeanHolder.destroy(); //owb as well as weld don't handle that correctly for injected "external"-beans

        //values of prototype spring-beans will change

        dependentCdiBeanHolder = BeanProvider.getDependent(DependentCdiBean.class);
        dependentCdiBean = dependentCdiBeanHolder.get();

        Assert.assertEquals(singletonSpringBeanValue, dependentCdiBean.getSingletonSpringBean().getValue());
        Assert.assertNotSame(prototypeSpringBeanValue, dependentCdiBean.getPrototypeSpringBean().getValue());
        Assert.assertEquals(namedSpringBeanValue, dependentCdiBean.getNamedSpringBean().getValue());
    }

    @Test
    public void releasePrototypeSpringBeanTest()
    {
        PrototypeSpringBean.setDestroyed(false);
        BeanProvider.getDependent(PrototypeSpringBean.class).destroy();

        Assert.assertTrue(PrototypeSpringBean.isDestroyed());
    }
}
