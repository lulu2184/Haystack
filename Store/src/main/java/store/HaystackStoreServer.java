package store;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.xnio.Options;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HaystackStoreServer {

    public static void main(String[] args) {
        HttpHandler storeHandler = new StoreHandler();
        Undertow server = Undertow.builder()
                .setWorkerOption(Options.WORKER_IO_THREADS, 80)
                .setWorkerOption(Options.WORKER_TASK_CORE_THREADS, 400)
                .addHttpListener(4443, "0.0.0.0")
                .setHandler(new EagerFormParsingHandler().setNext(storeHandler))
                .build();
        server.start();
        System.out.println(getPublicIpAddress());

        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(40);
        manager.setDefaultMaxPerRoute(20);
        HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(manager).setConnectionManagerShared(true);

        try (CloseableHttpClient client = clientBuilder.build()) {
            String dir_ip = "128.2.13.138";
            String port = "8080";
//            int volumn_num = 10;
            HttpPost post = new HttpPost("http://" + dir_ip + ":" + port + "/servers/store?dns="
                    + getPublicIpAddress() + ":4443");
            client.execute(post);
            CloseableHttpResponse response = client.execute(post);
            int code = response.getStatusLine().getStatusCode();
            System.out.println("store register result: " + code);
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPublicIpAddress() {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec("hostname -I");
            InputStream in = proc.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(in, "GBK"));
            String ip_str = br.readLine();

            String[] ips = ip_str.split(" ");
            return ips[0];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
