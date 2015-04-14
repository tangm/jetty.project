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

package org.eclipse.jetty.websocket.jsr356.server.deploy;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@HandlesTypes({ ServerApplicationConfig.class, ServerEndpoint.class, Endpoint.class })
public class WebSocketServerContainerInitializer implements ServletContainerInitializer
{
    private static final Logger LOG = Log.getLogger(WebSocketServerContainerInitializer.class);

    public WebSocketServerContainerInitializer()
    {
    }

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException
    {
        if (LOG.isDebugEnabled())
            LOG.debug("onStartup {} classes={}",context,c==null?0:c.size());
        context.setAttribute(WebSocketFeature.CLASSES_KEY,c);
    }

}
