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

package org.eclipse.jetty.quickstart;

import java.io.FileOutputStream;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * QuickStartWar
 *
 */
public class QuickStartWebApp extends WebAppContext
{
    private static final Logger LOG = Log.getLogger(QuickStartWebApp.class);
    
    public static final Configuration[] __configurations = new Configuration[] 
    {
        new org.eclipse.jetty.quickstart.QuickStartConfiguration(),
        new org.eclipse.jetty.plus.webapp.EnvConfiguration(),
        new org.eclipse.jetty.plus.webapp.PlusConfiguration(),
        new org.eclipse.jetty.webapp.JettyWebXmlConfiguration()
    };
    
    private boolean _preconfigure=false;
    private boolean _autoPreconfigure=false;
    private boolean _startWebapp=false;
    private PreconfigureDescriptorProcessor _preconfigProcessor;

    public static final Configuration[] __preconfiguration = new Configuration[]
    { 
        new org.eclipse.jetty.webapp.WebInfConfiguration(), 
        new org.eclipse.jetty.webapp.WebXmlConfiguration(),
        new org.eclipse.jetty.webapp.MetaInfConfiguration(), 
        new org.eclipse.jetty.webapp.FragmentConfiguration(),
        new org.eclipse.jetty.plus.webapp.EnvConfiguration(), 
        new org.eclipse.jetty.plus.webapp.PlusConfiguration(),
        new org.eclipse.jetty.annotations.AnnotationConfiguration()
    };
    
    public QuickStartWebApp()
    {
        super();
        setConfigurations(__preconfiguration);
        setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",".*\\.jar");
    }

    public boolean isPreconfigure()
    {
        return _preconfigure;
    }

    /* ------------------------------------------------------------ */
    /** Preconfigure webapp
     * @param preconfigure  If true, then starting the webapp will generate 
     * the WEB-INF/quickstart-web.xml rather than start the webapp.
     */
    public void setPreconfigure(boolean preconfigure)
    {
        _preconfigure = preconfigure;
    }

    public boolean isAutoPreconfigure()
    {
        return _autoPreconfigure;
    }
    
    public void setAutoPreconfigure(boolean autoPrecompile)
    {
        _autoPreconfigure = autoPrecompile;
    }
    
    @Override
    protected void startWebapp() throws Exception
    {
        if (isPreconfigure())
            generateQuickstartWebXml(_preconfigProcessor.getXML());
        
        if (_startWebapp)
            super.startWebapp();
    }
    
    
    
    @Override
    protected void stopWebapp() throws Exception
    {
        if (!_startWebapp)
            return;
        
        super.stopWebapp();
    }

    @Override
    protected void doStart() throws Exception
    {
        // unpack and Adjust paths.
        Resource war = null;
        Resource dir = null;

        Resource base = getBaseResource();
        if (base==null)
            base=Resource.newResource(getWar());

        if (base.isDirectory())
            dir=base;
        else if (base.toString().toLowerCase().endsWith(".war"))
        {
            war=base;
            String w=war.toString();
            dir=Resource.newResource(w.substring(0,w.length()-4));

            if (!dir.exists())
            {                       
                LOG.info("Quickstart Extract " + war + " to " + dir);
                dir.getFile().mkdirs();
                JarResource.newJarResource(war).copyTo(dir.getFile());
            }

            setWar(null);
            setBaseResource(dir);
        }
        else 
            throw new IllegalArgumentException();


        Resource qswebxml=dir.addPath("/WEB-INF/quickstart-web.xml");
        
        if (isPreconfigure())
        {
            _preconfigProcessor = new PreconfigureDescriptorProcessor();
            getMetaData().addDescriptorProcessor(_preconfigProcessor);
            _startWebapp=false;
        }
        else if (qswebxml.exists())
        {
            setConfigurations(__configurations);
            _startWebapp=true;
        }
        else if (_autoPreconfigure)
        {   
            LOG.info("Quickstart preconfigure: {}(war={},dir={})",this,war,dir);

            _preconfigProcessor = new PreconfigureDescriptorProcessor();    
            getMetaData().addDescriptorProcessor(_preconfigProcessor);
            setPreconfigure(true);
            _startWebapp=true;
        }
        else
            _startWebapp=true;
            
        super.doStart();
    }


    public void generateQuickstartWebXml(String extraXML) throws Exception
    {
        Resource descriptor = getWebInf().addPath(QuickStartDescriptorGenerator.DEFAULT_QUICKSTART_DESCRIPTOR_NAME);
        if (!descriptor.exists())
            descriptor.getFile().createNewFile();
        QuickStartDescriptorGenerator generator = new QuickStartDescriptorGenerator(this, extraXML);
        try (FileOutputStream fos = new FileOutputStream(descriptor.getFile()))
        {
            generator.generateQuickStartWebXml(fos);
        }
    }

  
}
