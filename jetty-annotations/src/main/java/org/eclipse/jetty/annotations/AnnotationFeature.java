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

package org.eclipse.jetty.annotations;

import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebAppFeature;

public class AnnotationFeature extends WebAppFeature
{
    public static final String ENABLE_KEY=AnnotationFeature.class.getPackage().getName();
    
    public AnnotationFeature()
    {
        super(ENABLE_KEY);
    }

    @Override
    protected boolean doPreEnableWebApp(WebAppContext webapp)
    {
        super.doPreEnableWebApp(webapp);
        AnnotationConfiguration configuration = new AnnotationConfiguration();
        if (!webapp.addConfiguration(configuration,JettyWebXmlConfiguration.class))
            webapp.addConfiguration(configuration);
        return true;
    }
}
