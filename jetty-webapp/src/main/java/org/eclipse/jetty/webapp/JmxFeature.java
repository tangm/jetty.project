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

import org.eclipse.jetty.webapp.WebAppFeature;


/* ------------------------------------------------------------ */
/** Enable visibility of Jetty style JMX MBeans from within a WebApplication 
 */
public class JmxFeature extends WebAppFeature
{
    public static final String ENABLE_KEY=JmxFeature.class.getPackage().getName()+".jmx";
    
    public JmxFeature()
    {
        super(ENABLE_KEY,true,null,
              new String[] {
              "org.eclipse.jetty.jmx.",         
              "org.eclipse.jetty.util.annotation."},
              new String[] {
              "-org.eclipse.jetty.jmx.",         
              "-org.eclipse.jetty.util.annotation."});
    }
}
