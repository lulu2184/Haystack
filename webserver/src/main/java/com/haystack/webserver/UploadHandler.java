package com.haystack.webserver;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class UploadHandler implements HttpHandler {
    private DirectoryConnector dirConnector;

    public UploadHandler() {
        dirConnector = new DirectoryConnector();
    }

    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.getRequestMethod().equals(Methods.POST)) {
            // POST request: upload an image.

            // Read raw photo data in POST request.
            byte[] data = null;
            try {
                exchange.startBlocking();
                if (exchange.isInIoThread()) {
                    exchange.dispatch(this);
                    return;
                } else {
                    data = IOUtils.toByteArray(exchange.getInputStream());
                }
            } catch( IOException e ) {
                e.printStackTrace( );
            }

            // Generate a new photo id
            long pid = (new Date()).getTime() * 37 + ((new Random()).nextInt() % 1001);
            List<LogicalVolume> logicalVolumes = dirConnector.getWritableVolume(5);
            boolean success = false;
            for (LogicalVolume logicalVolume : logicalVolumes) {
                dirConnector.storePhotoRecord(pid, logicalVolume);
                if (storePhotoInStorage(pid, data, logicalVolume)) {
                    ResponseCodeHandler.HANDLE_200.handleRequest(exchange);
                    success = true;
                }
            }
            if (!success)
                ResponseCodeHandler.HANDLE_500.handleRequest(exchange);
        }  else if (exchange.getRequestMethod().equals(Methods.GET)) {
            // GET request: get an image uploaded before.

            String pid = exchange.getQueryParameters().get("pid").getFirst();

            PhotoIndexEntry photoEntry = dirConnector.getPhotoRecord(Integer.valueOf(pid));
            if (photoEntry != null) {
                byte[] data = getPhotoData(photoEntry);
                if (data == null) {
                    ResponseCodeHandler.HANDLE_500.handleRequest(exchange);
                } else {
                    exchange.getResponseSender().send(ByteBuffer.wrap(data));
                    ResponseCodeHandler.HANDLE_200.handleRequest(exchange);
                }
            } else {
                // Image not found.
                ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
            }
        }
    }

    private static boolean storePhotoInStorage(long pid, byte[] data, LogicalVolume logicalVolume) {
        boolean success = false;
        for (String physicalUrl : logicalVolume.physicalVolumes) {
            HttpURLConnection con;
            try {
                URL url = new URL(String.format("%s/%d/%d",
                        physicalUrl, logicalVolume.logicalNum, pid));
                con = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                System.err.println("Failed to connect to " + physicalUrl);
                continue;
            }

            try {
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
            } catch (ProtocolException e) {
                e.printStackTrace();
                continue;
            }

            DataOutputStream out = null;
            try {
                out = new DataOutputStream(con.getOutputStream());
                out.write(data);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            } finally {
                try {
                    out.close();
                } catch (NullPointerException | IOException e) {}
            }

            String response;
            int code;
            try {
                code = con.getResponseCode();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                response = content.toString();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            JSONObject resJson = new JSONObject(response);
            boolean isFull = resJson.getBoolean("isFull");
            boolean isAvailable = resJson.getBoolean("isAvailable");
            if (code == 200 && !isFull && isAvailable) {
                success = true;
            }
        }
        return success;
    }

    private static byte[] getPhotoData(PhotoIndexEntry entry) {
        for (String physicalUrl : entry.physicalMachines) {
            HttpURLConnection con;
            try {
                URL url = new URL(String.format("%s/%s/%d/%d",
                        entry.cacheUrl, physicalUrl, entry.logicalId, entry.pid));
                con = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                System.err.println("Failed to connect to " + physicalUrl);
                continue;
            }

            try {
                con.setRequestMethod("GET");
                con.setRequestProperty("Content-Type", "application/json");
            } catch (ProtocolException e) {
                e.printStackTrace();
                continue;
            }

            byte[] response;
            int code;
            try {
                code = con.getResponseCode();
                response = IOUtils.toByteArray(con.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            if (code == 200) {
                return response;
            }
        }
        return null;
    }
}
