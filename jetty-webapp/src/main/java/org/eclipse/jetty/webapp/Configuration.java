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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /* ------------------------------------------------------------------------------- */
    /** Set the default configurations classes for the server
     * @param server The server to set them on.
     * @param configurationClass Configuration class names
     */
    public static void setDefault(Server server, String... configurationClass)
    {
        if (configurationClass.length==0)
            server.setAttribute(Configuration.ATTR,null);
        else
        {
            server.setAttribute(Configuration.ATTR,
                    instantiate(server,Arrays.asList(configurationClass).stream()).stream()
                    .map(c->{return c.getClass().getName();})
                    .collect(Collectors.toList()));
        }
    }

    /* ------------------------------------------------------------------------------- */
    /** Add the default configurations classes for the server.
     * <p>If no previous default set, initialize from {@link WebAppContext#DEFAULT_CONFIGURATION_CLASSES}
     * @param server The server to add them to.
     * @param configurationClass Configuration class names
     */
    public static void addDefault(Server server, String... configurationClass)
    {
        if (configurationClass.length==0)
            return;
        server.setAttribute(Configuration.ATTR,
                instantiate(server,Stream.concat(streamFrom(server),Arrays.asList(configurationClass).stream())).stream()
                .map(c->{return c.getClass().getName();})
                .collect(Collectors.toList()));
    }

    /* ------------------------------------------------------------------------------- */
    /** Get the default configurations for the server
     * @param server The server 
     * @return Configuration class names or null if no default set.
     */
    public static String[] getDefaults(Server server)
    {
        if (server.getAttribute(Configuration.ATTR)==null)
            return null;
        List<String> configurations = findDefaults(server);
        return configurations.stream().map(c->{return c.getClass().getName();}).collect(Collectors.toList()).toArray(new String[configurations.size()]);
    }

    /* ------------------------------------------------------------------------------- */
    /** Find the default configurations for the server
     * @param server The server 
     * @return List of Configuration instances either from {@link Configuration#ATTR} or {@link WebAppContext#DEFAULT_CONFIGURATION_CLASSES}
     */
    public static List<String> findDefaults(Server server)
    {
        return instantiate(server,streamFrom(server)).stream()
        .map(c->{return c.getClass().getName();})
        .collect(Collectors.toList());
    }

    /* ------------------------------------------------------------------------------- */
    public static Collection<? extends Configuration> instantiateDefaults(Server server)
    {
        return instantiate(server,streamFrom(server.getAttribute(Configuration.ATTR)));
    }

    /* ------------------------------------------------------------------------------- */
    public static List<? extends Configuration> instantiate(Server server,Stream<String> configurationClasses)
    {
        Map<String,Configuration> n2c = new HashMap<>();
        
        List<Configuration> configurations = new ArrayList<>();
        
        // Add the configurations
        configurationClasses.forEach(s->
        {
            try
            {
                Class<?> clazz = Loader.loadClass(server.getClass(), String.valueOf(s)); 
                if (!Configuration.class.isAssignableFrom(clazz))
                    throw new IllegalStateException("!Configuration: "+clazz);
                
                Configuration c = ((Class<? extends Configuration>)clazz).newInstance();
                if (n2c.containsKey(c.getName()))
                    configurations.set(configurations.indexOf(n2c.get(c.getName())),c);
                else
                    configurations.add(c);
                n2c.put(c.getName(),c);
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        return configurations;   
    }
    
    /* ------------------------------------------------------------------------------- */
    public static Stream<String> streamFrom(Object attr)
    {
        if (attr==null)
            return Arrays.asList(WebAppContext.DEFAULT_CONFIGURATION_CLASSES).stream();
        if (attr instanceof String)
            return Arrays.asList(StringUtil.csvSplit((String)attr)).stream();
        if (attr instanceof Collection<?>)
            return ((Collection<String>)attr).stream();
        if (attr instanceof String[])
            return Arrays.asList((String[])attr).stream();
        
        throw new IllegalStateException("Unknown default for "+Configuration.ATTR+"="+attr);
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
