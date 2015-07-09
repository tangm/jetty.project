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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.StringUtil;

/* ------------------------------------------------------------------------------- */
/** Base Class for WebApplicationContext Configuration.
 * This class can be extended to customize or extend the configuration
 * of the WebApplicationContext. 
 */
public interface Configuration 
{
    public final static String ATTR="org.eclipse.jetty.webapp.configuration";

    public static void addDefault(Server server, String... configurationClass)
    {
        addDefault(server,Arrays.asList(configurationClass).stream().map(cc->
        {
            Configuration c;
            try
            {
                c = ((Class<? extends Configuration>)Loader.loadClass(server.getClass(),cc)).newInstance();
                return c;
            }
            catch (Exception e)
            {   
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()).toArray(new Configuration[0]));
    }
    
    public static void addDefault(Server server, Configuration... configuration)
    {
        List<Configuration> configurations = new ArrayList<>();
        Object attr = server.getAttribute(Configuration.ATTR);

        // Look for server default as collection of strings or instances
        Stream<? extends Object> stream=null;
        if (attr instanceof String)
            stream=Arrays.asList(StringUtil.csvSplit((String)attr)).stream();
        else if (attr instanceof Collection<?>)
            stream=((Collection<?>)attr).stream();
        else if (attr instanceof String[])
            stream=Arrays.asList((String[])attr).stream();
        else if (attr instanceof Configuration[])
            stream=Arrays.asList((Configuration[])attr).stream();
        
        // Add the configurations
        if (stream!=null)
        {
            stream.forEach(o->
            {
                if (o instanceof Configuration)
                    configurations.add((Configuration)o);
                else
                {
                    try
                    {
                        configurations.add((Configuration)Loader.loadClass(server.getClass(), String.valueOf(o)).newInstance());
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        
        configurations.addAll(Arrays.asList(configuration));
        server.setAttribute(Configuration.ATTR,configurations);
    }

    /* ------------------------------------------------------------------------------- */
    public static String[] getDefaultClasses(Server server)
    {
        List<Configuration> configurations = new ArrayList<>();
        Object attr = server.getAttribute(Configuration.ATTR);

        // Look for server default as collection of strings or instances
        Stream<? extends Object> stream=null;
        if (attr instanceof String)
            stream=Arrays.asList(StringUtil.csvSplit((String)attr)).stream();
        else if (attr instanceof Collection<?>)
            stream=((Collection<?>)attr).stream();
        else if (attr instanceof String[])
            stream=Arrays.asList((String[])attr).stream();
        else if (attr instanceof Configuration[])
            stream=Arrays.asList((Configuration[])attr).stream();
        
        // Add the configurations
        if (stream!=null)
        {
            stream.forEach(o->
            {
                if (o instanceof Configuration)
                    configurations.add((Configuration)o);
                else
                {
                    try
                    {
                        configurations.add((Configuration)Loader.loadClass(server.getClass(), String.valueOf(o)).newInstance());
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        server.setAttribute(Configuration.ATTR,configurations);
        return configurations.toArray(new String[configurations.size()]);
        
    }
    
    /* ------------------------------------------------------------------------------- */
    public String getName();

    /* ------------------------------------------------------------------------------- */
    public Set<String> getDependsOn();

    /* ------------------------------------------------------------------------------- */
    public Set<String> getDependents();
    
    /* ------------------------------------------------------------------------------- */
    /** Set up for configuration.
     * <p>
     * Typically this step discovers configuration resources
     * @param context The context to configure
     * @throws Exception if unable to pre configure
     */
    public void preConfigure (WebAppContext context) throws Exception;
    
    
    /* ------------------------------------------------------------------------------- */
    /** Configure WebApp.
     * <p>
     * Typically this step applies the discovered configuration resources to
     * either the {@link WebAppContext} or the associated {@link MetaData}.
     * @param context The context to configure
     * @throws Exception if unable to configure
     */
    public void configure (WebAppContext context) throws Exception;
    
    
    /* ------------------------------------------------------------------------------- */
    /** Clear down after configuration.
     * @param context The context to configure
     * @throws Exception if unable to post configure
     */
    public void postConfigure (WebAppContext context) throws Exception;
    
    /* ------------------------------------------------------------------------------- */
    /** DeConfigure WebApp.
     * This method is called to undo all configuration done. This is
     * called to allow the context to work correctly over a stop/start cycle
     * @param context The context to configure
     * @throws Exception if unable to deconfigure
     */
    public void deconfigure (WebAppContext context) throws Exception;

    /* ------------------------------------------------------------------------------- */
    /** Destroy WebApp.
     * This method is called to destroy a webappcontext. It is typically called when a context 
     * is removed from a server handler hierarchy by the deployer.
     * @param context The context to configure
     * @throws Exception if unable to destroy
     */
    public void destroy (WebAppContext context) throws Exception;
    

    /* ------------------------------------------------------------------------------- */
    /** Clone configuration instance.
     * <p>
     * Configure an instance of a WebAppContext, based on a template WebAppContext that 
     * has previously been configured by this Configuration.
     * @param template The template context
     * @param context The context to configure
     * @throws Exception if unable to clone
     */
    public void cloneConfigure (WebAppContext template, WebAppContext context) throws Exception;
}
