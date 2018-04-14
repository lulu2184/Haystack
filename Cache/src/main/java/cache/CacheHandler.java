package cache;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.io.IOUtils;
import redis.clients.jedis.Jedis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class CacheHandler implements HttpHandler {
    Jedis jedis = new Jedis();
    boolean debug = false;
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.startBlocking();
        // into worker threads
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String filepath_root = "/Users/youx/Developer/photos/";

        if (exchange.getRequestMethod().equalToString("GET")) {
            String path = exchange.getRelativePath().substring(1);
            String[] paths = path.split("/");

            String key = paths[3];
            String lvid = paths[2];
            String physicalURL = paths[1];
            String cacheURL = paths[0];
            System.out.println("pid: " + key);

            // TODO: about to change key
            if (jedis.exists(key + "key")) {
                // return photo from cache
                byte[] photo = jedis.get((key + "key").getBytes());
                if (debug) {
                    writePhoto(filepath_root + "test" + key, photo);
                }
                exchange.getOutputStream().write(photo);
                exchange.endExchange();
            } else {
                // connect store
                HttpURLConnection con = null;
                try {
                    URL url = new URL(String.format("http://%s/%s/%s/%s",
                            physicalURL, cacheURL, lvid, key));
                    con = (HttpURLConnection) url.openConnection();
                } catch (IOException e) {
                    System.err.println("Failed to connect to " + physicalURL);
                }

                try {
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "application/json");
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }

                try {
                    int code = con.getResponseCode();
                    byte[] photo = IOUtils.toByteArray(con.getInputStream());
                    if (code == 200) {
                        // store into cache
                        storePhotoIntoCache(key, photo);
                        if (debug) {
                            writePhoto(filepath_root + "test" + key, photo);
                        }
                        exchange.getOutputStream().write(photo);
                        exchange.endExchange();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    private void storePhotoIntoCache(String key, byte[] photo) {
        byte[] key_byte = (key + "key").getBytes();
        jedis.set(key_byte, photo);
        jedis.expire(key + "key", 60);
    }

    private void writePhoto(String path, byte[] photo) {
        try {
            RandomAccessFile output = new RandomAccessFile(path, "rw");
            output.write(photo);
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
