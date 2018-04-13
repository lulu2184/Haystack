package com.haystack.webserver;

import io.undertow.Undertow;

public class Server
{
    public static void main( String[] args )
    {
        try {
            Undertow server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(new UploadHandler()).build();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
