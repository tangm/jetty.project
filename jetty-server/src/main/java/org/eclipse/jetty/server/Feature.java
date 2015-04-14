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


/* ------------------------------------------------------------ */
/** <p>Enable/Disable a Feature on a Server and/or Context.</p>
 * <p>A Feature instance is used to carry out all the phases required to 
 * enable a feature on a context including:
 * <ol>
 *   <li>PreEnabling the feature. Typically 
 *   this is by configuring the classpath/classloader and/or adding Configuration
 *   mechanisms so that feature dependent classes can be consulted when determining if
 *   the feature will be enabled.</li>
 *   <li>Enabling the feature, which often will use discovery of usages of
 *   the feature (eg Annotations) before commiting the context to use the feature.</li>
 * </ol>
 * <p>A feature can be added to all contexts of a server by adding
 * the Feature instance as a bean on the server:
 * <pre>
 *    Server server = new Server();
 *    server.addBean(new MyFeature());
 * </pre>
 * Alternately, a Feature may be added to just a single context:
 * <pre>
 *    ContextHandler context = new ContextHandler(server);
 *    context.addBean(new MyFeature());
 * </pre>
 * When starting a context, the Features added to both the server and context 
 * are used, but only a single Feature instance enabled per {@link #getKey()} value 
 * is used with Context Features taking precedence over Server Features.
 * </p>
 * <p>
 * Note that adding a Feature does not necessarily enable the feature, instead it 
 * allows either the explicit or automatic enabling of the feature.   Other 
 * configuration, discoveries (eg Annotations) may be used by the Feature to 
 * determine if it will be enabled or not.  Conventions for such extra Configuration
 * are provided by {@link AbstractFeature} and further specialised for
 * Web Applications by {@link org.eclipse.jetty.webapp.WebAppContext}.
 * 
 */
public interface Feature
{
    /* ------------------------------------------------------------ */
    /** Get they Key used to identify the feature.
     * Only one feature per key value may be enabled on a Context.
     * @return The feature Key string. Typically the package name of the feature.
     */
    String getKey();

    /* ------------------------------------------------------------ */
    /** PreEnabling the feature. 
     * Typically this is by configuring the classpath/classloader and/or adding Configuration
     * mechanisms so that feature dependent classes can be consulted when determining if
     * the feature will be enabled.
     * @param context The context to preenable
     * @return True if the feature can be enabled, false if not.
     */
    boolean preEnable(ContextHandler context);
    
    /* ------------------------------------------------------------ */
    /** Enable the feature on a Context.
     * @param context The context to enable.
     * @return True if the feature was enabled, false if gracefully not enable (eg by configuration)
     * @throws Exception If a problem was encountered enabling the feature.
     */
    boolean enable(ContextHandler context) throws Exception;
}
