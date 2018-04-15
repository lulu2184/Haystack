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

public class ImageHandler implements HttpHandler {
    private DirectoryConnector dirConnector;

    public ImageHandler(DirectoryConnector dirConnector) {
        this.dirConnector = dirConnector;
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
                    exchange.setStatusCode(200);
                    exchange.getResponseSender().send("pid: " + String.valueOf(pid));
                    success = true;
                    break;
                }
            }
            if (!success)
                exchange.setStatusCode(500);
        }  else if (exchange.getRequestMethod().equals(Methods.GET)) {
            // GET request: get an image uploaded before.
            String pid;
            try {
                pid = exchange.getQueryParameters().get("pid").getFirst();
            } catch (NullPointerException e) {
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("Invalid query parameter.");
                return;
            }

            PhotoIndexEntry photoEntry;
            try {
                photoEntry = dirConnector.getPhotoRecord(Long.valueOf(pid));
            } catch (NumberFormatException e) {
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("Invalid pid format.");
                return;
            }
            if (photoEntry != null) {
                byte[] data = getPhotoData(photoEntry);
                if (data == null) {
                    exchange.setStatusCode(500);
                } else {
                    exchange.setStatusCode(200);
                    exchange.getResponseSender().send(ByteBuffer.wrap(data));
                }
            } else {
                // Image not found.
                exchange.setStatusCode(404);
            }
        } else if (exchange.getRequestMethod().equals(Methods.DELETE)) {
            // DELETE request: delete an image with specific pid.

            String pidString = exchange.getQueryParameters().get("pid").getFirst();
            try {
                long pid = Long.valueOf(pidString);
                PhotoIndexEntry photoEntry = dirConnector.deletePhotoRecord(pid);
                if (photoEntry == null) {
                    exchange.setStatusCode(404);
                    exchange.getResponseSender().send("Image not found.");
                    return;
                }
                deletePhotoInStorage(pid, photoEntry.cacheUrl, photoEntry.logicalId);
                for (String storeUrl : photoEntry.physicalMachines) {
                    deletePhotoInStorage(pid, storeUrl, photoEntry.logicalId);
                }
                exchange.setStatusCode(200);
            } catch (NumberFormatException e) {
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("Invalid pid format.");
            }

        }
    }

    private boolean storePhotoInStorage(long pid, byte[] data, LogicalVolume logicalVolume) {
        boolean success = false;
        for (String physicalUrl : logicalVolume.physicalVolumes) {
            HttpURLConnection con;
            String urlString = "http://" + physicalUrl + "/" + String.valueOf(logicalVolume.logicalNum)
                    + "/" + String.valueOf(pid);
            System.out.println("POST URL to Store: " + urlString);
            try {
                URL url = new URL(urlString);
                con = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                System.err.println("Failed to connect to " + physicalUrl);
                e.printStackTrace();
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

            System.out.println("[Response for POST]" + response);
//            JSONObject resJson = new JSONObject(response);
//            boolean isFull = resJson.getBoolean("isFull");
//            boolean isAvailable = resJson.getBoolean("isAvailable");
//            if (code == 200 && !isFull && isAvailable) {
            if (code == 200) {
                success = true;
            }
            else if (code == 503) {
                dirConnector.markVolumeAsUnwritable(logicalVolume.logicalNum);
            }
        }
        return success;
    }

    private byte[] getPhotoData(PhotoIndexEntry entry) {
        for (String physicalUrl : entry.physicalMachines) {
            HttpURLConnection con;
            try {
                String urlString = String.format("http://%s/%s/%d/%d",
                        entry.cacheUrl, physicalUrl, entry.logicalId, entry.pid);
                System.out.println(urlString);
                URL url = new URL(urlString);
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

    private static boolean deletePhotoInStorage(long pid, String destinationHost, int logicalVolume) {
        HttpURLConnection con;
        try {
            String urlString = String.format("http://%s/%d/%d",
                    destinationHost, logicalVolume, pid);
            System.out.println(urlString);
            URL url = new URL(urlString);
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            System.err.println("Failed to connect to " + destinationHost);
            return false;
        }

        try {
            con.setRequestMethod("DELETE");
            con.setRequestProperty("Content-Type", "application/json");
        } catch (ProtocolException e) {
            e.printStackTrace();
            return false;
        }

        int code;
        try {
            code = con.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return (code == 200);
    }
}
