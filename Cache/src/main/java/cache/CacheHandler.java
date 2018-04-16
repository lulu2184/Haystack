package cache;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.apache.commons.io.IOUtils;
import redis.clients.jedis.HostAndPort;
//import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class CacheHandler implements HttpHandler {
//    Jedis jedis = new Jedis();
    private static Set<HostAndPort> nodes = initial();
    JedisCluster cluster;
    boolean debug = false;

    private static Set<HostAndPort> initial() {
        Set<HostAndPort> nodes  = new HashSet<>();
        nodes.add(new HostAndPort("128.2.100.165", 7000));
        nodes.add(new HostAndPort("128.2.100.165", 7001));
        nodes.add(new HostAndPort("128.2.100.165", 7002));
        nodes.add(new HostAndPort("128.2.100.166", 7003));
        nodes.add(new HostAndPort("128.2.100.166", 7004));
        nodes.add(new HostAndPort("128.2.100.166", 7005));
        nodes.add(new HostAndPort("128.2.100.167", 7006));
        nodes.add(new HostAndPort("128.2.100.167", 7007));
        nodes.add(new HostAndPort("128.2.100.167", 7008));
        return nodes;
    }

    public CacheHandler() {
        System.out.println(nodes.size());
        cluster = new JedisCluster(nodes);
    }

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
            if (paths.length != 3) {
                badRequest(exchange);
            } else {
                String key = paths[2];
                String lvid = paths[1];
                String physicalURL = paths[0];
//            String cacheURL = paths[0];
                System.out.println("pid: " + key);

                // TODO: about to change key
                if (cluster.exists(key + "key")) {
                    // return photo from cache
                    byte[] photo = cluster.get((key + "key").getBytes());
                    if (debug) {
                        writePhoto(filepath_root + "test" + key, photo);
                    }
                    exchange.getOutputStream().write(photo);
                    exchange.endExchange();
                } else {
                    // connect store
                    HttpURLConnection con = null;
                    try {
                        URL url = new URL(String.format("http://%s/%s/%s",
                                physicalURL, lvid, key));
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
                        if (code == 200) {
                            // store into cache
                            byte[] photo = IOUtils.toByteArray(con.getInputStream());
                            storePhotoIntoCache(key, photo);
                            if (debug) {
                                writePhoto(filepath_root + "test" + key, photo);
                            }
                            exchange.getOutputStream().write(photo);
                            exchange.endExchange();
                        } else if (code == 404) {
                            notFound(exchange);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void notFound(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.endExchange();
    }

    private void badRequest(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        exchange.endExchange();
    }

    private void storePhotoIntoCache(String key, byte[] photo) {
        byte[] key_byte = (key + "key").getBytes();
        cluster.set(key_byte, photo);
        cluster.expire(key + "key", 60);
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
