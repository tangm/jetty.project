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

package org.eclipse.jetty.websocket.client.http;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpConversation;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response.CompleteListener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionUpgrader;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.api.extensions.ExtensionFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.ClientUpgradeResponse;
import org.eclipse.jetty.websocket.client.NoOpEndpoint;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.client.io.WebSocketClientConnection;
import org.eclipse.jetty.websocket.common.AcceptHash;
import org.eclipse.jetty.websocket.common.SessionFactory;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.common.events.EventDriver;
import org.eclipse.jetty.websocket.common.events.EventDriverFactory;
import org.eclipse.jetty.websocket.common.extensions.ExtensionStack;

public class WebSocketUpgradeRequest extends HttpRequest implements CompleteListener, HttpConnectionUpgrader
{
    private static final Logger LOG = Log.getLogger(WebSocketUpgradeRequest.class);

    private final WebSocketClient wsClient;
    private final HttpClient httpClient;
    private final CompletableFuture<Session> fut;
    private final int port;
    private List<ExtensionConfig> extensions;
    private List<String> subProtocols;
    private Object localEndpoint;

    public WebSocketUpgradeRequest(WebSocketClient wsClient, HttpClient httpClient, ClientUpgradeRequest request)
    {
        this(wsClient,httpClient,request.getRequestURI());

        // Copy values into place
        this.extensions = new ArrayList<>(request.getExtensions());
        this.subProtocols = new ArrayList<>(request.getSubProtocols());
        this.localEndpoint = request.getLocalEndpoint();
        if(StringUtil.isNotBlank(request.getOrigin()))
            this.header(HttpHeader.ORIGIN,request.getOrigin());
    }

    public WebSocketUpgradeRequest(WebSocketClient wsClient, HttpClient httpClient, URI uri)
    {
        super(httpClient,new HttpConversation(),uri);
        this.fut = new CompletableFuture<Session>();
        this.httpClient = httpClient;
        this.wsClient = wsClient;
        this.extensions = new ArrayList<>();
        this.subProtocols = new ArrayList<>();

        this.port = normalizePort(uri);
    }

    private final String genRandomKey()
    {
        byte[] bytes = new byte[16];
        ThreadLocalRandom.current().nextBytes(bytes);
        return new String(B64Code.encode(bytes));
    }

    private EventDriverFactory getEventDriverFactory()
    {
        return wsClient.getEventDriverFactory();
    }

    private ExtensionFactory getExtensionFactory()
    {
        return wsClient.getExtensionFactory();
    }

    public List<ExtensionConfig> getExtensions()
    {
        return extensions;
    }

    public Object getLocalEndpoint()
    {
        if (localEndpoint == null)
        {
            return new NoOpEndpoint();
        }
        return localEndpoint;
    }

    @Override
    public int getPort()
    {
        return port;
    }

    private SessionFactory getSessionFactory()
    {
        return wsClient.getSessionFactory();
    }

    public List<String> getSubProtocols()
    {
        return subProtocols;
    }

    private WebSocketPolicy getWebSocketPolicy()
    {
        return wsClient.getPolicy();
    }

    private void initWebSocketHeaders()
    {
        method(HttpMethod.GET);
        version(HttpVersion.HTTP_1_1);

        // The Upgrade Headers
        header(HttpHeader.UPGRADE,"websocket");
        header(HttpHeader.CONNECTION,"Upgrade");

        // The WebSocket Headers
        header(HttpHeader.SEC_WEBSOCKET_KEY,genRandomKey());
        header(HttpHeader.SEC_WEBSOCKET_VERSION,"13");

        // (Per the hybi list): Add no-cache headers to avoid compatibility issue.
        // There are some proxies that rewrite "Connection: upgrade"
        // to "Connection: close" in the response if a request doesn't contain
        // these headers.
        header(HttpHeader.PRAGMA,"no-cache");
        header(HttpHeader.CACHE_CONTROL,"no-cache");

        // handle "Sec-WebSocket-Extensions"
        if (!getExtensions().isEmpty())
        {
            for (ExtensionConfig ext : getExtensions())
            {
                header(HttpHeader.SEC_WEBSOCKET_EXTENSIONS,ext.getParameterizedName());
            }
        }

        // handle "Sec-WebSocket-Protocol"
        if (!getSubProtocols().isEmpty())
        {
            for (String protocol : getSubProtocols())
            {
                header(HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL,protocol);
            }
        }
    }

    private int normalizePort(URI uri)
    {
        if (uri.getPort() >= 1)
        {
            return uri.getPort();
        }

        if (uri.getScheme().equalsIgnoreCase("wss"))
        {
            return 443;
        }

        return 80;
    }

    @Override
    public void onComplete(Result result)
    {
        if (result.isFailed())
        {
            if(result.getFailure()!=null)
                LOG.warn("General Failure", result.getFailure());
            if(result.getRequestFailure()!=null)
                LOG.warn("Request Failure", result.getRequestFailure());
            if(result.getResponseFailure()!=null)
                LOG.warn("Response Failure", result.getResponseFailure());
            fut.completeExceptionally(result.getFailure());
        }
    }

    @Override
    public ContentResponse send() throws InterruptedException, TimeoutException, ExecutionException
    {
        throw new RuntimeException("Working with raw ContentResponse is invalid for WebSocket");
    }

    @Override
    public void send(final CompleteListener listener)
    {
        initWebSocketHeaders();

        /*
        Origin origin = new Origin(getScheme(), getHost(), getPort());

        HttpDestination dest = new HttpDestination(httpClient, origin)
        {
            @Override
            public void send()
            {
                TODO: send(WebSocketUpgradeRequest.this, listeners);
            }
        };
        */

        super.send(listener);
    }

    public CompletableFuture<Session> sendAsync()
    {
        send(this);
        return fut;
    }

    public void setExtensions(List<ExtensionConfig> extensions)
    {
        this.extensions = extensions;
    }

    public void setLocalEndpoint(Object localEndpoint)
    {
        this.localEndpoint = localEndpoint;
    }

    public void setSubProtocols(List<String> subprotocols)
    {
        this.subProtocols = subprotocols;
    }

    @Override
    public void upgrade(HttpResponse response, HttpConnectionOverHTTP conn)
    {
        if (!this.getHeaders().get(HttpHeader.UPGRADE).equalsIgnoreCase("websocket"))
        {
            // Not my upgrade
            throw new HttpResponseException("Not WebSocket Upgrade",response);
        }

        // Check the Accept hash
        String reqKey = this.getHeaders().get(HttpHeader.SEC_WEBSOCKET_KEY);
        String expectedHash = AcceptHash.hashKey(reqKey);
        String respHash = response.getHeaders().get(HttpHeader.SEC_WEBSOCKET_ACCEPT);

        if (expectedHash.equalsIgnoreCase(respHash) == false)
        {
            throw new HttpResponseException("Invalid Sec-WebSocket-Accept hash",response);
        }

        // We can upgrade
        EndPoint endp = conn.getEndPoint();

        WebSocketClientConnection connection = new WebSocketClientConnection(wsClient,endp);

        URI requestURI = this.getURI();

        EventDriver websocket = getEventDriverFactory().wrap(getLocalEndpoint());
        WebSocketSession session = getSessionFactory().createSession(requestURI,websocket,connection);
        session.setUpgradeRequest(new ClientUpgradeRequest(this));
        session.setUpgradeResponse(new ClientUpgradeResponse(response));
        connection.addListener(session);

        ExtensionStack extensionStack = new ExtensionStack(getExtensionFactory());
        List<ExtensionConfig> extensions = new ArrayList<>();
        HttpField extField = response.getHeaders().getField(HttpHeader.SEC_WEBSOCKET_EXTENSIONS);
        if(extField != null)
        {
            String[] extValues = extField.getValues();
            if (extValues != null)
            {
                for (String extVal : extValues)
                {
                    QuotedStringTokenizer tok = new QuotedStringTokenizer(extVal,",");
                    while (tok.hasMoreTokens())
                    {
                        extensions.add(ExtensionConfig.parse(tok.nextToken()));
                    }
                }
            }
        }
        extensionStack.negotiate(extensions);

        extensionStack.configure(connection.getParser());
        extensionStack.configure(connection.getGenerator());

        // Setup Incoming Routing
        connection.setNextIncomingFrames(extensionStack);
        extensionStack.setNextIncoming(session);

        // Setup Outgoing Routing
        session.setOutgoingHandler(extensionStack);
        extensionStack.setNextOutgoing(connection);

        session.addManaged(extensionStack);
        session.setFuture(fut);

        wsClient.addManaged(session);

        // Now swap out the connection
        endp.upgrade(connection);
    }
}
