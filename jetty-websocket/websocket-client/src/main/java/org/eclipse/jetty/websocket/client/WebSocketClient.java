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

package org.eclipse.jetty.websocket.client;

import java.io.IOException;
import java.net.CookieStore;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.api.extensions.ExtensionFactory;
import org.eclipse.jetty.websocket.client.http.WebSocketUpgradeRequest;
import org.eclipse.jetty.websocket.client.io.ConnectionManager;
import org.eclipse.jetty.websocket.client.io.UpgradeListener;
import org.eclipse.jetty.websocket.client.masks.Masker;
import org.eclipse.jetty.websocket.client.masks.RandomMasker;
import org.eclipse.jetty.websocket.common.SessionFactory;
import org.eclipse.jetty.websocket.common.SessionListener;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.common.WebSocketSessionFactory;
import org.eclipse.jetty.websocket.common.events.EventDriver;
import org.eclipse.jetty.websocket.common.events.EventDriverFactory;
import org.eclipse.jetty.websocket.common.extensions.WebSocketExtensionFactory;
import org.eclipse.jetty.websocket.common.scopes.WebSocketContainerScope;

/**
 * WebSocketClient provides a means of establishing connections to remote websocket endpoints.
 */
public class WebSocketClient extends ContainerLifeCycle implements SessionListener, WebSocketContainerScope
{
    private static final Logger LOG = Log.getLogger(WebSocketClient.class);
    
    // From HttpClient
    private HttpClient httpClient;
    
//    private final SslContextFactory sslContextFactory;
//    private ByteBufferPool bufferPool;
//    private Executor executor;
//    private Scheduler scheduler;
//    private SocketAddress bindAddress;
//    private long connectTimeout = SelectorManager.DEFAULT_CONNECT_TIMEOUT;
//    private boolean dispatchIO = true; // REMOVE

    // Other
    private final WebSocketPolicy policy = WebSocketPolicy.newClientPolicy();
    private final WebSocketExtensionFactory extensionRegistry;
    private final EventDriverFactory eventDriverFactory;
    private final SessionFactory sessionFactory;
    private final DecoratedObjectFactory objectFactory;
    private Masker masker;

    public WebSocketClient()
    {
        this(new HttpClient());
    }
    
    public WebSocketClient(HttpClient httpClient)
    {
        this(httpClient, new DecoratedObjectFactory());
    }
    
    public WebSocketClient(HttpClient httpClient, DecoratedObjectFactory objectFactory)
    {
        this.httpClient = httpClient;

        this.objectFactory = objectFactory;
        this.extensionRegistry = new WebSocketExtensionFactory(this);
        this.masker = new RandomMasker();
        this.eventDriverFactory = new EventDriverFactory(policy);
        this.sessionFactory = new WebSocketSessionFactory(this);
    }
    
    // below are old
    public WebSocketClient(Executor executor)
    {
        this(null,executor);
    }
    
    public WebSocketClient(ByteBufferPool bufferPool)
    {
        this(null,null,bufferPool);
    }

    public WebSocketClient(SslContextFactory sslContextFactory)
    {
        this(sslContextFactory,null);
    }

    public WebSocketClient(SslContextFactory sslContextFactory, Executor executor)
    {
        this(sslContextFactory,executor,new MappedByteBufferPool());
    }
    
    public WebSocketClient(WebSocketContainerScope scope)
    {
        this(scope.getSslContextFactory(), scope.getExecutor(), scope.getBufferPool(), scope.getObjectFactory());
    }
    
    public WebSocketClient(WebSocketContainerScope scope, SslContextFactory sslContextFactory)
    {
        this(sslContextFactory, scope.getExecutor(), scope.getBufferPool(), scope.getObjectFactory());
    }

    public WebSocketClient(SslContextFactory sslContextFactory, Executor executor, ByteBufferPool bufferPool)
    {
        this(sslContextFactory, executor, bufferPool, new DecoratedObjectFactory());
    }

    public WebSocketClient(SslContextFactory sslContextFactory, Executor executor, ByteBufferPool bufferPool, DecoratedObjectFactory objectFactory)
    {
        this.httpClient = new HttpClient(sslContextFactory);
        this.httpClient.setExecutor(executor);
        
        this.objectFactory = objectFactory;
        this.extensionRegistry = new WebSocketExtensionFactory(this);
        
        this.masker = new RandomMasker();
        this.eventDriverFactory = new EventDriverFactory(policy);
        this.sessionFactory = new WebSocketSessionFactory(this);
    }
    
    public WebSocketClient(WebSocketContainerScope scope, EventDriverFactory eventDriverFactory, SessionFactory sessionFactory)
    {
        this.httpClient = new HttpClient(scope.getSslContextFactory());
        this.httpClient.setExecutor(scope.getExecutor());

        this.objectFactory = new DecoratedObjectFactory();
        this.extensionRegistry = new WebSocketExtensionFactory(this);

        this.masker = new RandomMasker();
        this.eventDriverFactory = eventDriverFactory;
        this.sessionFactory = sessionFactory;
    }

    public Future<Session> connect(Object websocket, URI toUri) throws IOException
    {
        ClientUpgradeRequest request = new ClientUpgradeRequest(toUri);
        request.setRequestURI(toUri);
        request.setLocalEndpoint(websocket);

        return connect(websocket,toUri,request);
    }

    /**
     * Connect to remote websocket endpoint
     * 
     * @param websocket the websocket object
     * @param toUri the websocket uri to connect to
     * @param request the upgrade request information
     * @param upgradeListener does nothing
     * @return the future for the session, available on success of connect
     * @throws IOException if unable to connect
     * @deprecated {@link UpgradeListener} no longer supported, no alternative available
     */
    @Deprecated
    public Future<Session> connect(Object websocket, URI toUri, ClientUpgradeRequest request, UpgradeListener upgradeListener) throws IOException
    {
        return connect(websocket,toUri,request,null);
    }

    public Future<Session> connect(Object websocket, URI toUri, ClientUpgradeRequest request) throws IOException
    {
        if (!isStarted())
        {
            throw new IllegalStateException(WebSocketClient.class.getSimpleName() + "@" + this.hashCode() + " is not started");
        }

        // Validate websocket URI
        if (!toUri.isAbsolute())
        {
            throw new IllegalArgumentException("WebSocket URI must be absolute");
        }

        if (StringUtil.isBlank(toUri.getScheme()))
        {
            throw new IllegalArgumentException("WebSocket URI must include a scheme");
        }

        String scheme = toUri.getScheme().toLowerCase(Locale.ENGLISH);
        if (("ws".equals(scheme) == false) && ("wss".equals(scheme) == false))
        {
            throw new IllegalArgumentException("WebSocket URI scheme only supports [ws] and [wss], not [" + scheme + "]");
        }

        request.setRequestURI(toUri);
        request.setLocalEndpoint(websocket);

        // Validate Requested Extensions
        for (ExtensionConfig reqExt : request.getExtensions())
        {
            if (!extensionRegistry.isAvailable(reqExt.getName()))
            {
                throw new IllegalArgumentException("Requested extension [" + reqExt.getName() + "] is not installed");
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("connect websocket {} to {}",websocket,toUri);

        init();
        
        WebSocketUpgradeRequest wsReq = new WebSocketUpgradeRequest(this,httpClient,request);
        return wsReq.sendAsync();
    }
    
    @Override
    protected void doStart() throws Exception
    {
        if(!httpClient.isStarted())
            addBean(httpClient);

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
        if (ShutdownThread.isRegistered(this))
        {
            ShutdownThread.deregister(this);
        }

        super.doStop();
        
        if (LOG.isDebugEnabled())
            LOG.debug("Stopped {}",this);
    }

    @Deprecated
    public boolean isDispatchIO()
    {
        return httpClient.isDispatchIO();
    }

    /**
     * Return the number of milliseconds for a timeout of an attempted write operation.
     * 
     * @return number of milliseconds for timeout of an attempted write operation
     */
    public long getAsyncWriteTimeout()
    {
        return this.policy.getAsyncWriteTimeout();
    }

    public SocketAddress getBindAddress()
    {
        return httpClient.getBindAddress();
    }

    public ByteBufferPool getBufferPool()
    {
        return httpClient.getByteBufferPool();
    }

    @Deprecated
    public ConnectionManager getConnectionManager()
    {
        throw new UnsupportedOperationException("ConnectionManager is no longer supported");
    }

    public long getConnectTimeout()
    {
        return httpClient.getConnectTimeout();
    }

    public CookieStore getCookieStore()
    {
        return httpClient.getCookieStore();
    }
    
    public EventDriverFactory getEventDriverFactory()
    {
        return eventDriverFactory;
    }

    public Executor getExecutor()
    {
        return httpClient.getExecutor();
    }

    public ExtensionFactory getExtensionFactory()
    {
        return extensionRegistry;
    }

    public Masker getMasker()
    {
        return masker;
    }

    /**
     * Get the maximum size for buffering of a binary message.
     * 
     * @return the maximum size of a binary message buffer.
     */
    public int getMaxBinaryMessageBufferSize()
    {
        return this.policy.getMaxBinaryMessageBufferSize();
    }

    /**
     * Get the maximum size for a binary message.
     * 
     * @return the maximum size of a binary message.
     */
    public long getMaxBinaryMessageSize()
    {
        return this.policy.getMaxBinaryMessageSize();
    }

    /**
     * Get the max idle timeout for new connections.
     * 
     * @return the max idle timeout in milliseconds for new connections.
     */
    public long getMaxIdleTimeout()
    {
        return this.policy.getIdleTimeout();
    }

    /**
     * Get the maximum size for buffering of a text message.
     * 
     * @return the maximum size of a text message buffer.
     */
    public int getMaxTextMessageBufferSize()
    {
        return this.policy.getMaxTextMessageBufferSize();
    }

    /**
     * Get the maximum size for a text message.
     * 
     * @return the maximum size of a text message.
     */
    public long getMaxTextMessageSize()
    {
        return this.policy.getMaxTextMessageSize();
    }

    @Override
    public DecoratedObjectFactory getObjectFactory()
    {
        return this.objectFactory;
    }

    public Set<WebSocketSession> getOpenSessions()
    {
        return Collections.unmodifiableSet(new HashSet<>(getBeans(WebSocketSession.class)));
    }

    public WebSocketPolicy getPolicy()
    {
        return this.policy;
    }

    public Scheduler getScheduler()
    {
        return httpClient.getScheduler();
    }
    
    public SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }

    /**
     * @return the {@link SslContextFactory} that manages TLS encryption
     * @see #WebSocketClient(SslContextFactory)
     */
    public SslContextFactory getSslContextFactory()
    {
        return httpClient.getSslContextFactory();
    }

    private synchronized void init() throws IOException
    {
        if (!ShutdownThread.isRegistered(this))
        {
            ShutdownThread.register(this);
        }

        // TODO: reevaluate
//        if (executor == null)
//        {
//            QueuedThreadPool threadPool = new QueuedThreadPool();
//            String name = WebSocketClient.class.getSimpleName() + "@" + hashCode();
//            threadPool.setName(name);
//            threadPool.setDaemon(daemon);
//            executor = threadPool;
//            addManaged(threadPool);
//        }
//        else
//        {
//            addBean(executor,false);
//        }
    }

    /**
     * Factory method for new ConnectionManager (used by other projects like cometd)
     * 
     * @return the ConnectionManager instance to use
     * @deprecated use HttpClient instead
     */
    @Deprecated
    protected ConnectionManager newConnectionManager()
    {
        throw new UnsupportedOperationException("ConnectionManager is no longer supported");
    }

    @Override
    public void onSessionClosed(WebSocketSession session)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Session Closed: {}",session);
        removeBean(session);
    }

    @Override
    public void onSessionOpened(WebSocketSession session)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Session Opened: {}",session);
        addManaged(session);
    }
    
    public void setAsyncWriteTimeout(long ms)
    {
        this.policy.setAsyncWriteTimeout(ms);
    }

    /**
     * @param bindAddress the address to bind to
     * @deprecated use {@link #setBindAddress(SocketAddress)} instead
     */
    @Deprecated
    public void setBindAdddress(SocketAddress bindAddress)
    {
        setBindAddress(bindAddress);
    }

    public void setBindAddress(SocketAddress bindAddress)
    {
        this.httpClient.setBindAddress(bindAddress);
    }

    public void setBufferPool(ByteBufferPool bufferPool)
    {
        this.httpClient.setByteBufferPool(bufferPool);
    }

    /**
     * Set the timeout for connecting to the remote server.
     * 
     * @param ms
     *            the timeout in milliseconds
     */
    public void setConnectTimeout(long ms)
    {
        this.httpClient.setConnectTimeout(ms);
    }

    public void setCookieStore(CookieStore cookieStore)
    {
        this.httpClient.setCookieStore(cookieStore);
    }
    
    public void setDaemon(boolean daemon)
    {
        // do nothing
    }

    @Deprecated
    public void setDispatchIO(boolean dispatchIO)
    {
        this.httpClient.setDispatchIO(dispatchIO);
    }

    public void setExecutor(Executor executor)
    {
        this.httpClient.setExecutor(executor);
    }

    public void setMasker(Masker masker)
    {
        this.masker = masker;
    }

    public void setMaxBinaryMessageBufferSize(int max)
    {
        this.policy.setMaxBinaryMessageBufferSize(max);
    }
    
    /**
     * Set the max idle timeout for new connections.
     * <p>
     * Existing connections will not have their max idle timeout adjusted.
     * 
     * @param ms
     *            the timeout in milliseconds
     */
    public void setMaxIdleTimeout(long ms)
    {
        this.policy.setIdleTimeout(ms);
    }

    public void setMaxTextMessageBufferSize(int max)
    {
        this.policy.setMaxTextMessageBufferSize(max);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException
    {
        dumpThis(out);
        dump(out, indent, getOpenSessions());
    }
}
