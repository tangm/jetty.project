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
import java.util.List;

import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;

public class ClientUpgradeResponse extends UpgradeResponse
{
    private List<ExtensionConfig> extensions;

    public ClientUpgradeResponse()
    {
        super();
    }
    
    public ClientUpgradeResponse(HttpResponse response)
    {
        super();
        setStatusCode(response.getStatus());
        setStatusReason(response.getReason());

        HttpFields fields = response.getHeaders();
        for (HttpField field : fields)
        {
            addHeader(field.getName(),field.getValue());
        }

        this.extensions = ExtensionConfig.parseEnum(fields.getValues("Sec-WebSocket-Extensions"));
        setAcceptedSubProtocol(fields.get("Sec-WebSocket-Protocol"));
    }
    
    @Override
    public List<ExtensionConfig> getExtensions()
    {
        return this.extensions;
    }

    @Override
    public void sendForbidden(String message) throws IOException
    {
        throw new UnsupportedOperationException("Not supported on client implementation");
    }
}
