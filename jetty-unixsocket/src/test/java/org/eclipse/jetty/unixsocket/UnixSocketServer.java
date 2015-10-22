package org.eclipse.jetty.unixsocket;

import org.eclipse.jetty.server.Server;

public class UnixSocketServer
{
    public static void main (String... args) throws Exception
    {
        Server server = new Server();
        
        UnixSocketConnector connector = new UnixSocketConnector(server);
        server.addConnector(connector);
        
        server.start();
        server.join();
    }
}
