package com.haystack.webserver;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;

public class Server
{
    public static void main( String[] args )
    {
        try {
            DirectoryConnector directoryConnector = new DirectoryConnector();
            ImageHandler imageHandler = new ImageHandler(directoryConnector);
            ServerHandler serverHandler = new ServerHandler(directoryConnector);

            RoutingHandler routingHandler = Handlers.routing()
                    .get("/img", imageHandler)
                    .post("/img", imageHandler)
                    .delete("/img", imageHandler)
                    .post("/servers/{type}", serverHandler)
                    .delete("/servers/{type}", serverHandler);

            Undertow server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(routingHandler).build();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
