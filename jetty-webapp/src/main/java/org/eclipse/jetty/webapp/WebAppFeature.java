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

import java.util.ServiceLoader;

import javax.servlet.ServletContainerInitializer;

import org.eclipse.jetty.server.AbstractFeature;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.StringUtil;


/* ------------------------------------------------------------ */
/** A WebApplication Feature.
 * <p>Extends the {@link AbstractFeature} with the ability to pre-enable
 * WebAppContext mechanisms such as:
 * <ul>
 * <li>{@link WebAppContext#addSystemClass(String)}</li>
 * <li>{@link WebAppContext#addServerClass(String)}</li>
 * <li>{@link ServletContainerInitializer}</li>
 * </ul>
 * Also provides convenience methods with {@link ContextHandler} 
 * narrowed to {@link WebAppContext}.
 */
public class WebAppFeature extends AbstractFeature
{
    private final String[] _systemClasses;
    private final String[] _serverClasses;
    private final String _sci;


    /* ------------------------------------------------------------ */
    /**
     * @param enableKey  The key used to identify the feature.
     */
    public WebAppFeature(String enableKey)
    {
        this(enableKey,true,null,(String[])null,(String[])null);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param enableKey  The key used to identify the feature.
     * @param packageOrClass A package or class that will be added as a System class pattern and 
     * as a negative server class pattern by {@link #doPreEnable(ContextHandler)}. This exposes the class or package 
     * and prevents it being replaced by the web application.
     * @param serverClasses Server class patterns  comma separated list to add to the webapp in {@link #doPreEnable(ContextHandler)} with {@link WebAppContext#addServerClass(String)}
     */
    public WebAppFeature(String enableKey, String packageOrClass)
    {
        this(enableKey,true,null,new String[]{packageOrClass},new String[] {"-"+packageOrClass});
        if (packageOrClass.startsWith("-") || packageOrClass.contains(",") || packageOrClass.contains(":"))
            throw new IllegalArgumentException();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param enableKey  The key used to identify the feature.
     * @param systemClasses System class patterns as comma separated list to add to the webapp in {@link #doPreEnable(ContextHandler)} with {@link WebAppContext#addSystemClass(String)}
     * @param serverClasses Server class patterns  comma separated list to add to the webapp in {@link #doPreEnable(ContextHandler)} with {@link WebAppContext#addServerClass(String)}
     */
    public WebAppFeature(String enableKey, String systemClasses, String serverClasses)
    {
        this(enableKey,true,null,StringUtil.split(systemClasses),StringUtil.split(serverClasses));
    }

    /* ------------------------------------------------------------ */
    /**
     * @param enableKey  The key used to identify the feature.
     * @param servletContainerInitializerClass The class name of a {@link ServletContainerInitializer}, which 
     * will always be exposed in the class loader by a call to {@link #preEnable(ContextHandler)} 
     * @param systemClasses System class patterns as comma separated list to add to the webapp in {@link #doPreEnable(ContextHandler)} with {@link WebAppContext#addSystemClass(String)}
     * @param serverClasses Server class patterns  comma separated list to add to the webapp in {@link #doPreEnable(ContextHandler)} with {@link WebAppContext#addServerClass(String)}
     */
    public WebAppFeature(String enableKey ,String servletContainerInitializerClass, String systemClasses, String serverClasses)
    {
        this(enableKey,true,servletContainerInitializerClass,StringUtil.split(systemClasses),StringUtil.split(serverClasses));
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param enableKey  The key used to identify the feature.
     * @param enableDefault The default to enable/disable if specific configuration is not found.
     * @param servletContainerInitializerClass The class name of a {@link ServletContainerInitializer}, which 
     * will always be exposed in the class loader by a call to {@link #preEnable(ContextHandler)} 
     * @param systemClasses System class patterns to add to the webapp in {@link #doPreEnable(ContextHandler)} with {@link WebAppContext#addSystemClass(String)}
     * @param serverClasses Server class patterns to add to the webapp in {@link #doPreEnable(ContextHandler)} with {@link WebAppContext#addServerClass(String)}
     */
    public WebAppFeature(String enableKey, Boolean enableDefault,String servletContainerInitializerClass, String[] systemClasses, String[] serverClasses)
    {
        super(enableKey,enableDefault);
        _systemClasses=systemClasses;
        _serverClasses=serverClasses;
        _sci=servletContainerInitializerClass;
    }

    /* ------------------------------------------------------------ */
    /** 
     * Pre-enable Context.
     * <p>Exposes any {@link ServletContainerInitializer} as a System & non Server class before normal
     * preEnable handling.  The initializer class is always exposed even if the feature cannot be enabled as
     * it needs to be loaded by the java {@link ServiceLoader} mechanism regardless.</p>
     * @see org.eclipse.jetty.server.AbstractFeature#preEnable(org.eclipse.jetty.server.handler.ContextHandler)
     */
    @Override
    public boolean preEnable(ContextHandler context)
    {
        if (_sci!=null && context instanceof WebAppContext)
        {
            WebAppContext webapp = (WebAppContext)context;
            webapp.prependSystemClass(_sci);
            webapp.prependServerClass("-"+_sci);
        }
        return super.preEnable(context);
    }

    /* ------------------------------------------------------------ */
    /** Pre Enable Context Feature
     * <p>If the passed context is a {@link WebAppContext}, then add any known 
     * system and server classes before calling {@link #doPreEnableWebApp(WebAppContext)}</p>
     * @see org.eclipse.jetty.server.AbstractFeature#doPreEnable(org.eclipse.jetty.server.handler.ContextHandler)
     */
    @Override
    protected boolean doPreEnable(ContextHandler context)
    {
        if (context instanceof WebAppContext)
        {
            WebAppContext webapp = (WebAppContext)context;
            
            if (_systemClasses!=null)
                for (String c : _systemClasses)
                {
                    if (c.startsWith("-"))
                        webapp.prependSystemClass(c);
                    else
                        webapp.addSystemClass(c);
                }
            if (_serverClasses!=null)
                for (String c : _serverClasses)
                {
                    if (c.startsWith("-"))
                        webapp.prependServerClass(c);
                    else
                        webapp.addServerClass(c);
                }
            
            return doPreEnableWebApp(webapp);
        }
        
        return false;
    }

    /* ------------------------------------------------------------ */
    /** Pre enable the Web Application Feature
     * @param webapp The Web Application to pre-enable
     * @return True if the feature can be enabled.
     * @see #doPreEnable(ContextHandler);
     */
    protected boolean doPreEnableWebApp(WebAppContext webapp)
    {          
        return true;
    }

    /* ------------------------------------------------------------ */
    /** Enable Web Application Feature.
     * <p>If the passed context is a {@link WebAppContext}, then call
     * {@link #doEnableWebApp(WebAppContext, boolean)}.</p>
     * @see org.eclipse.jetty.server.AbstractFeature#doEnable(org.eclipse.jetty.server.handler.ContextHandler, boolean)
     */
    @Override
    protected boolean doEnable(ContextHandler context, boolean force) throws Exception
    {
        if (context instanceof WebAppContext)
            return doEnableWebApp((WebAppContext)context,force);
        return false;
    }
    
    /* ------------------------------------------------------------ */
    /** Enable the Web Application Feature
     * @param webapp The Web Application to enable the feature on.
     * @param force If true, then force enabling.
     * @return True if the feature was enabled, false if it was gracefully not enabled.
     * @throws Exception If there was a problem enabling the feature.
     * @see #doEnable(ContextHandler, boolean)
     */
    protected boolean doEnableWebApp(WebAppContext webapp, boolean force) throws Exception
    {
        return true;
    }

}
