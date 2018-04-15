package store;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.apache.commons.io.IOUtils;
import redis.clients.jedis.Jedis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.zip.Adler32;

public class StoreHandler implements HttpHandler {
    private Jedis jedis = new Jedis();
    final long MAX_SIZE = 10737418240L;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.startBlocking();
        // into worker threads
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String path = exchange.getRelativePath().substring(1);
        String[] paths = path.split("/");
//        String filepath_root = "/Users/youx/Developer/photos/";
        String filepath_root = "/afs/andrew.cmu.edu/usr12/youx/photos/";


        // Upload img
        if (exchange.getRequestMethod().equalToString("POST")) {
            if (paths.length != 2) {
                badRequest(exchange);
            } else {
                byte[] photo = IOUtils.toByteArray(exchange.getInputStream());

                String lvid = paths[0];
                String filepath =  filepath_root + lvid;
                long key = Long.parseLong(paths[1]);
                boolean success = upload(filepath, key, photo);
                if (success)
                    exchange.getResponseSender().send("ok");
                else {
                    System.out.println("No storage!");
                    noStorage(exchange);
                }
            }
        } else if (exchange.getRequestMethod().equalToString("GET")) {
            if (paths.length != 2) {
                badRequest(exchange);
            } else {
                String lvid = paths[0];
                String filepath = filepath_root + lvid;
                long key = Long.parseLong(paths[1]);
                byte[] photo = readPhoto(filepath, key);
                if(photo.length == 0) {
                    System.out.println("Photo does not exist!");
                    notFound(exchange);
                } else {
                    exchange.getOutputStream().write(photo);
                    exchange.endExchange();
                }
            }

        } else if (exchange.getRequestMethod().equalToString("DELETE")) {
            if (paths.length != 2) {
                badRequest(exchange);
            } else {
                long key = Long.parseLong(paths[1]);
                boolean success = delete(key);
                if (success)
                    exchange.getResponseSender().send("ok");
                else {
                    System.out.println("Photo not found!");
                    notFound(exchange);
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

    private void noStorage(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
        exchange.endExchange();
    }

    private boolean upload(String filepath, long key, byte[] photo) {
        int header_magic_num = 123456789;
        int size = photo.length;
        int footer_magic_num = 987654321;
        char alternate_key = 'n';
        int flag = 1;
        long offset;

        // calculate checksum
        Adler32 ad = new Adler32();
        ad.update(photo);
        long checksum = ad.getValue();

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(filepath, "rw");
            offset = raf.length();
            int current_len = 4 + 8 + 2 + 4 + 4 + size + 4 + 8;

            // check is storage enough
            if (offset + current_len > MAX_SIZE)
                return false;


            raf.seek(raf.length());
            //4 bytes
            raf.writeInt(header_magic_num);

            //8 bytes
            raf.writeLong(key);

            //2 bytes
            raf.writeChar(alternate_key);

            //4 bytes
            raf.writeInt(flag);
            //4 bytes
            raf.writeInt(size);
            // size bytes
            raf.write(photo);
            //4 bytes
            raf.writeInt(footer_magic_num);
            //8 bytes
            raf.writeLong(checksum);

//            int mod = 8 - current_len % 8;
//            raf.write(mod);
            int mod = 0;
            raf.close();

            // save offset into redis
            int final_size = current_len + mod;
            String[] attrs = {((Long)offset).toString(), ((Integer)final_size).toString(), "true"};
            jedis.lpush(((Long)key).toString(), attrs);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

    }

    private byte[] readPhoto(String filepath, long key) {
        List<String> list = jedis.lrange(((Long)key).toString(),0,-1);
        byte[] photo = {};
        // mapping fails
        if (list.size() != 3) {
            System.out.println("mapping fails " + list.size());
            return photo;
        }

        long offset = Long.parseLong(list.get(2));
        int size = Integer.parseInt(list.get(1));

        // photo is deleted
        if (list.get(0).equals("false")) {
            System.out.println("photo is deleted");
            return photo;
        }

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(filepath, "r");
            raf.seek(offset);
            byte[] needle = new byte[size];
            int hasRead = raf.read(needle);
            // size wrong
            if (hasRead == -1) {
                System.out.println("size wrong");
                return photo;
            }

            int length = needle.length;
//            int end = length - 13;
//            int start = 22;

            photo = new byte[length-34];
            for (int i = 0; i < length-34; ++i) {
                photo[i] = needle[i + 22];
            }

//            RandomAccessFile output = new RandomAccessFile("/Users/youx/Developer/photos/test", "rw");
//            System.out.println(photo.length);
//            output.write(photo);
//            output.close();

        } catch (FileNotFoundException e) {
            return photo;
        } catch (IOException e) {
            return photo;
        }

        return photo;
    }

    private boolean delete(long key) {
        String key_str = ((Long)key).toString();
        if (jedis.exists(key_str)) {
            jedis.del(key_str);
            return true;
        } else
            return false;
    }
}
