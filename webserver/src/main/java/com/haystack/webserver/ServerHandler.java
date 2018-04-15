package com.haystack.webserver;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class ServerHandler implements HttpHandler{
    private static String CACHE = "cache";
    private static String STORE = "store";
    private static String TYPE = "type";
    private static String DNS = "dns";

    private DirectoryConnector directoryConnector;

    public ServerHandler(DirectoryConnector directoryConnector) {
        this.directoryConnector = directoryConnector;
    }

    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String[] path = exchange.getRelativePath().split("/");
        if (path.length < 3) {
            exchange.setStatusCode(400);
            return;
        }

        String type = path[2];
        if (type == null || (!type.equals(CACHE) && !type.equals(STORE))) {
            exchange.setStatusCode(400);
            return;
        }

        HttpString requestMethod = exchange.getRequestMethod();
        if (type.equals(CACHE)) {
            String dns = exchange.getQueryParameters().get(DNS).getFirst();
            if (dns == null)
                exchange.setStatusCode(400);
            else {
                if (requestMethod.equals(Methods.POST))
                    directoryConnector.addCacheServer(dns);
                else if (requestMethod.equals(Methods.DELETE))
                    directoryConnector.deleteCacheServer(dns);
            }

        } else if (type.equals(STORE)) {
            Deque<String> queryParameter = exchange.getQueryParameters().get(DNS);
            if (queryParameter == null) {
                exchange.setStatusCode(400);
                return;
            }
            List<String> dns = Arrays.asList(queryParameter.getFirst().split(","));
            if (requestMethod.equals(Methods.POST)) {
                if (!directoryConnector.addStoreServers(dns)) {
                    exchange.setStatusCode(400);
                    exchange.getResponseSender().send("Failed to add duplicated store server.");
                }
            }
        }
    }
}
