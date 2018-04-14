package store;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.xnio.Options;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class HaystackStoreServer {

    public static void main(String[] args) {
        HttpHandler storeHandler = new StoreHandler();
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            Undertow server = Undertow.builder()
                    .setWorkerOption(Options.WORKER_IO_THREADS, 80)
                    .setWorkerOption(Options.WORKER_TASK_CORE_THREADS, 400)
                    .addHttpListener(4443, "0.0.0.0")
                    .setHandler(new EagerFormParsingHandler().setNext(storeHandler))
                    .build();
            server.start();
            System.out.println(inetAddress.getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(40);
        manager.setDefaultMaxPerRoute(20);
        HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(manager).setConnectionManagerShared(true);

        // TODO: Replace dir_ip&port
        try (CloseableHttpClient client = clientBuilder.build()) {
            String dir_ip = "127.0.0.1";
            String port = "4444";
            int volumn_num = 10;
            HttpPost post = new HttpPost("http://" + dir_ip + ":" + port + "/" + volumn_num);
            client.execute(post);
//            CloseableHttpResponse response = client.execute(post);
//            int code = response.getStatusLine().getStatusCode();
//            System.out.println(response);
//            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
