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
/** Enable visibility of JAAS classes from within a WebApplication 
 */
public class JaasFeature extends WebAppFeature
{
    public static final String ENABLE_KEY=JaasFeature.class.getPackage().getName()+".jaas";
    
    public JaasFeature()
    {
        super(ENABLE_KEY,"org.eclipse.jetty.jaas.");
    }
}
