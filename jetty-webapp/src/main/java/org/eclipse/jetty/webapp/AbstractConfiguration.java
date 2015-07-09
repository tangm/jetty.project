//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.webapp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AbstractConfiguration implements Configuration
{
    private final String _name;
    private final Set<String> _dependsOn;
    private final Set<String> _dependents;

    protected AbstractConfiguration(String name)
    {
        this(name,null,null);
    }
    
    protected AbstractConfiguration(String name, String[] dependsOn)
    {
        this(name,dependsOn,null);
    }
    
    protected AbstractConfiguration(String name,String[] dependsOn, String[] dependents)
    {
        _name=name;
        _dependsOn=Collections.unmodifiableSet(dependsOn==null?Collections.emptySet():new HashSet<>(Arrays.asList(dependsOn)));
        _dependents=Collections.unmodifiableSet(dependents==null?Collections.emptySet():new HashSet<>(Arrays.asList(dependents)));
    }
    
    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public int hashCode()
    {
        return _name.hashCode();
    }
    
    @Override 
    public boolean equals(Object obj) 
    {
        return obj instanceof AbstractConfiguration && ((AbstractConfiguration)obj)._name.equals(_name);
    };
    
    @Override
    public Set<String> getDependsOn()
    {
        return _dependsOn;
    }

    @Override
    public Set<String> getDependents()
    {
        return _dependents;
    }
    
    public void preConfigure(WebAppContext context) throws Exception
    {
    }

    public void configure(WebAppContext context) throws Exception
    {
    }

    public void postConfigure(WebAppContext context) throws Exception
    {
    }

    public void deconfigure(WebAppContext context) throws Exception
    {
    }

    public void destroy(WebAppContext context) throws Exception
    {
    }

    public void cloneConfigure(WebAppContext template, WebAppContext context) throws Exception
    {
    }

}
