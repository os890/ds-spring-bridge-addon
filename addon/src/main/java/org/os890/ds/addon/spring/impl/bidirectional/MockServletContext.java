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

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

//based on myfaces-test
@Exclude
class MockServletContext implements ServletContext
{
    private Hashtable attributes = new Hashtable();

    MockServletContext()
    {
        // this class is only accessible via getInstance
    }

    Map<String, String> getAttributes()
    {
        return new HashMap<String, String>(attributes);
    }

    public Object getAttribute(String name)
    {
        return attributes.get(name);
    }

    public Enumeration getAttributeNames()
    {
        return attributes.keys();
    }

    public ServletContext getContext(String uripath)
    {
        return this;
    }

    public String getContextPath()
    {
        return "mocked"; //customize it - if needed
    }

    public String getInitParameter(String name)
    {
        return (String)attributes.get(name);
    }

    public Enumeration getInitParameterNames()
    {
        return new StringTokenizer(""); // 'standard' empty Enumeration
    }

    public int getMajorVersion()
    {
        return 3; //customize it - if needed
    }

    public int getMinorVersion()
    {
        return 0;
    }

    public String getMimeType(String file)
    {
        return null;
    }

    public RequestDispatcher getNamedDispatcher(String name)
    {
        return null;
    }

    public String getRealPath(String path)
    {
        return "mockRealPath";
    }

    public RequestDispatcher getRequestDispatcher(String path)
    {
        return null;
    }

    public URL getResource(String path) throws MalformedURLException
    {
        return MockServletContext.class.getResource(path);
    }

    public InputStream getResourceAsStream(String path)
    {
        return MockServletContext.class.getResourceAsStream(path);
    }

    public Set getResourcePaths(String path)
    {
        return null;
    }

    public String getServerInfo()
    {
        return "mockServer";
    }

    public Servlet getServlet(String name) throws ServletException
    {
        return null;
    }

    public String getServletContextName()
    {
        return null;
    }

    public Enumeration getServletNames()
    {
        return null;
    }

    public Enumeration getServlets()
    {
        return null;
    }

    public void log(String msg)
    {
        // nothing to do
    }

    public void log(Exception exception, String msg)
    {
        // nothing to do
    }

    public void log(String message, Throwable throwable)
    {
        // nothing to do
    }

    public void removeAttribute(String name)
    {
        attributes.remove(name);
    }

    @SuppressWarnings("unchecked")
    public void setAttribute(String name, Object object)
    {
        attributes.put(name, object);
    }
}
