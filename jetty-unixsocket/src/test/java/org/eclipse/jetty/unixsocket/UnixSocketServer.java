package org.eclipse.jetty.unixsocket;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class UnixSocketServer
{
    public static void main (String... args) throws Exception
    {
        Server server = new Server();
        
        UnixSocketConnector connector = new UnixSocketConnector(server);
        server.addConnector(connector);
        
        server.setHandler(new AbstractHandler()
        {

            @Override
            protected void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                response.setStatus(200);
                response.getWriter().write("Hello World");
            }
            
        });
        
        server.start();
        server.join();
    }
}
