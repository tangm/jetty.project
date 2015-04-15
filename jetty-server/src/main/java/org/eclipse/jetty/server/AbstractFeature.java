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

package org.eclipse.jetty.server;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


/* ------------------------------------------------------------ */
/** Abstract Feature implementation.
 * <p>AbstractFeature encapsulates the common conventions for 
 * enabling and disabling Features by configuration.</p>
 * <p>Adding an {@link AbstractFeature} to a server or context will typically
 * use discovery techniques to automatically determine if the feature
 * should be enabled for a context.  For example 
 * {@link #doPreEnable(ContextHandler)} may be used to setup scanning for
 * specific Annotations, which if discovered will be used by 
 * {@link #doEnable(ContextHandler, boolean)} to decide to enable the feature.
 * </p>
 * <p>However, an {@link AbstractFeature} may be explicitly enabled or disabled
 * by configuration.  The {@link #getKey()} value may be used as an attribute or
 * init-parameter name to set a boolean to either force enabling or disabling 
 * of a feature.
 * </p>
 * <p>
 * The following example will allow automatic enabling of a feature on all 
 * contexts except for a specific context which disables the feature:
 * <pre>
 *    Server server = new Server();
 *    Feature feature = new MyFeature();
 *    server.addBean(feature);
 *    ...
 *    ContextHandler context = new ContextHandler(server);
 *    context.setAttribute(feature.getKey(),Boolean.FALSE);
 * </pre>
 * The following example adds a feature to a contexts and overrules the 
 * automatic enabling to force the feature to be enabled:
 * <pre>
 *    Server server = new Server();
 *    Feature feature = new MyFeature();
 *    server.addBean(feature);
 *    ...
 *    ContextHandler context = new ContextHandler(server);
 *    Feature feature = new MyFeature();
 *    context.addBean(feature);
 *    context.setAttribute(feature.getKey(),Boolean.TRUE);
 * </pre>
 */
public abstract class AbstractFeature implements Feature
{    
    private static final Logger LOG = Log.getLogger(AbstractFeature.class);

    private final String _enableKey;
    private final Boolean _enableDefault;

    /* ------------------------------------------------------------ */
    /**
     * @param enableKey  The key used to identify the feature.
     */
    public AbstractFeature(String enableKey)
    {
        this(enableKey,true);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param enableKey  The key used to identify the feature. If null the Features package name is used.
     * @param enableDefault The default for enabling the feature if specific configuration is not found.
     */
    public AbstractFeature(String enableKey, boolean enableDefault)
    {
        super();
        _enableKey = enableKey==null?this.getClass().getPackage().getName():enableKey;
        _enableDefault = enableDefault;
    }

    /* ------------------------------------------------------------ */
    @Override 
    public String getKey()
    {
        return _enableKey;
    }
    
    /* ------------------------------------------------------------ */
    /** Pre-enable by Configuration.
     * Look for a boolean to determine if {@link #doPreEnable(ContextHandler)} should
     * be called. The {@link #getKey()} value is used to look for the boolean configuration
     * as a context attribute and then if not found as a server attribute. If an attribute is
     * not found then then enableDefault value passed in the constructor is used.
     * Only if the configured boolean is true is {@link #doPreEnable(ContextHandler)} called and
     * its return value is returned from this method.
     * @see org.eclipse.jetty.server.Feature#preEnable(org.eclipse.jetty.server.handler.ContextHandler)
     */
    @Override
    public boolean preEnable(ContextHandler context)
    {
        Object enable = context.getAttribute(_enableKey);
        if (enable==null && context.getServer()!=null)
            enable=context.getServer().getAttribute(_enableKey);
        if (enable==null)
            enable=_enableDefault;
        
        if (LOG.isDebugEnabled())
            LOG.debug("preEnable {}={}",this,enable);
        
        if (TypeUtil.isTrue(enable))
        {
            return doPreEnable(context);
        }
        return false;
    }

    /* ------------------------------------------------------------ */
    /** Do the pre enabling of the feature.
     * This method is implemented by specific features and is
     * called by {@link #preEnable(ContextHandler)} only if an
     * enabling boolean attribute is found or if the default is
     * to enable.
     * @param context The context to pre-enable
     * @return True if the feature may be enabled.
     */
    abstract protected boolean doPreEnable(ContextHandler context);
    
    /* ------------------------------------------------------------ */
    /** Enable a feature by Configuration.
     * The {@link #getKey()} value is used to look for a context init parameter, which if it 
     * evaluates to false, the feature is not enabled.
     * The {@link #doEnable(ContextHandler, boolean)} method is called to do the enabling of
     * the feature, passing forced as true if and only if enabling was indicated by a true
     * boolean as an attribute or init parameter.    If an enabling boolean is not set to
     * either true or false, then {@link #doEnable(ContextHandler, boolean)} can determine
     * itself if the feature will be enabled. 
     * 
     * @see org.eclipse.jetty.server.Feature#enable(org.eclipse.jetty.server.handler.ContextHandler)
     */
    @Override
    public boolean enable(ContextHandler context) throws Exception
    {
        // Is feature explicitly turned off by init param?
        String init = context.getInitParameter(_enableKey);
        if (LOG.isDebugEnabled())
            LOG.debug("enable {}={}",this,init);
        if (TypeUtil.isFalse(init))
            return false;

        // Was feature explicitly turned on or just defaulted?
        Object enable = context.getAttribute(_enableKey);
        if (enable==null)
            enable=init;
        if (enable==null && context.getServer()!=null)
            enable=context.getServer().getAttribute(_enableKey);
        if (LOG.isDebugEnabled())
            LOG.debug("enable {} force={}",this,enable);
        boolean force = TypeUtil.isTrue(enable);
        
        return doEnable(context,force);    
    }

    /* ------------------------------------------------------------ */
    /** Enable the Feature
     * @param context The context to enable the feature on.
     * @param force If true, the Feature will always be enabled. If false, the Feature
     * can decide by inspecting the context if it will be enabled or not.
     * @return True if the feature was enabled. False if the feature was gracefully not
     * enabled.
     * @throws Exception If there was a problem enabling the feature.
     */
    abstract protected boolean doEnable(ContextHandler context, boolean force) throws Exception;
    
    
    @Override
    public String toString()
    {
        return String.format("%s@%x{%s}",this.getClass().getSimpleName(),System.identityHashCode(this),_enableKey);
    }
}
