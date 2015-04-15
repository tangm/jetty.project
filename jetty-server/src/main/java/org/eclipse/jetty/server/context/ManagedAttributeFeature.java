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

package org.eclipse.jetty.server.context;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.AbstractFeature;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/* ------------------------------------------------------------ */
/** Enable Jetty style JMX MBeans from within a Context 
 */
public class ManagedAttributeFeature extends AbstractFeature
{
    private static final Logger LOG = Log.getLogger(ManagedAttributeFeature.class);

    public static final String ENABLE_KEY=ContextHandler.MANAGED_ATTRIBUTES;
    
    public ManagedAttributeFeature()
    {
        super(ENABLE_KEY);
    }

    @Override
    protected boolean doPreEnable(ContextHandler context)
    {
        return true;
    }

    @Override
    protected boolean doEnable(final ContextHandler context, boolean force) throws Exception
    {
        final ServletContext servletContext = context.getServletContext();
        String managed = servletContext.getInitParameter(ENABLE_KEY);
        if (managed != null)
        {
            // What names are managed?
            final Set<String> managedAttributes=new HashSet<>();
            String[] attributes = StringUtil.split(managed);
            for (String attribute : attributes)
                managedAttributes.add(attribute);

            if (LOG.isDebugEnabled())
                LOG.debug("managedAttributes {}",managedAttributes);
            
            // Update existing attributes
            Enumeration<String> e = servletContext.getAttributeNames();
            while (e.hasMoreElements())
            {
                String name = e.nextElement();
                if (managedAttributes.contains(name))
                    updateBean(context,null,servletContext.getAttribute(name));
            }

            // Track attribute changes
            context.addEventListener(new ServletContextAttributeListener()
            {
                @Override
                public void attributeReplaced(ServletContextAttributeEvent event)
                {
                    if (managedAttributes.contains(event.getName()))
                        updateBean(context,event.getValue(),servletContext.getAttribute(event.getName()));
                }
                
                @Override
                public void attributeRemoved(ServletContextAttributeEvent event)
                {
                    if (managedAttributes.contains(event.getName()))
                        updateBean(context,event.getValue(),null);                    
                }
                
                @Override
                public void attributeAdded(ServletContextAttributeEvent event)
                {
                    if (managedAttributes.contains(event.getName()))
                        updateBean(context,null,event.getValue());    
                }
            });
            
            
            // Delete beans on destruction
            context.addEventListener(new ServletContextListener()
            {
                @Override
                public void contextInitialized(ServletContextEvent sce)
                {                    
                }
                
                @Override
                public void contextDestroyed(ServletContextEvent sce)
                {
                    Enumeration<String> e = servletContext.getAttributeNames();
                    while (e.hasMoreElements())
                    {
                        String name = e.nextElement();
                        if (managedAttributes.contains(name))
                            updateBean(context,servletContext.getAttribute(name),null);
                    }
                }
            });
            
            return true;
        }
        
        return false;
    }
    
    private static void updateBean(ContextHandler context,Object oldBean,Object newBean)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("update {}->{} on {}",oldBean,newBean,context);
        context.updateBean(oldBean,newBean,false);
    }
}
