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

package org.eclipse.jetty.plus.webapp;

import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebAppFeature;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

public class PlusFeature extends WebAppFeature
{
    public static final String ENABLE_KEY="org.eclipse.jetty.plus";
    
    public PlusFeature()
    {
        super(ENABLE_KEY);
    }

    @Override
    protected boolean doPreEnableWebApp(WebAppContext webapp)
    {
        super.doPreEnableWebApp(webapp);
        PlusConfiguration configuration = new PlusConfiguration();
        
        if (!webapp.addConfiguration(FragmentConfiguration.class,configuration) &&
            !webapp.addConfiguration(MetaInfConfiguration.class,configuration) &&
            !webapp.addConfiguration(WebXmlConfiguration.class,configuration) &&
            !webapp.addConfiguration(WebInfConfiguration.class,configuration))
            webapp.addConfiguration(configuration);
        return true;
    }
}
