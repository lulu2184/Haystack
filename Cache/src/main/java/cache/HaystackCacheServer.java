package cache;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.xnio.Options;

import java.io.IOException;

public class HaystackCacheServer {

    public static void main(String[] args) {
        HttpHandler cacheHandler = new CacheHandler();
        Undertow server = Undertow.builder()
                .setWorkerOption(Options.WORKER_IO_THREADS, 80)
                .setWorkerOption(Options.WORKER_TASK_CORE_THREADS, 400)
                .addHttpListener(4442, "0.0.0.0")
                .setHandler(cacheHandler)
                .build();
        server.start();

//        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
//        manager.setMaxTotal(40);
//        manager.setDefaultMaxPerRoute(20);
//        HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(manager).setConnectionManagerShared(true);
//
//        // TODO: Replace dir_ip&port
//        try (CloseableHttpClient client = clientBuilder.build()) {
//            String dir_ip = "127.0.0.1";
//            String port = "4444";
//            HttpPost post = new HttpPost("http://" + dir_ip + ":" + port + "/");
//            client.execute(post);
////            CloseableHttpResponse response = client.execute(post);
////            int code = response.getStatusLine().getStatusCode();
////            System.out.println(response);
////            response.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
