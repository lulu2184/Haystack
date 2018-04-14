package store;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import org.xnio.Options;

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
    }
}
