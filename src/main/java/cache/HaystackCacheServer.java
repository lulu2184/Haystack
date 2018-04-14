package cache;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.xnio.Options;

public class HaystackCacheServer {

    public static void main(String[] args) {
        HttpHandler cacheHandler = new CacheHandler();
        Undertow server = Undertow.builder()
                .setWorkerOption(Options.WORKER_IO_THREADS, 80)
                .setWorkerOption(Options.WORKER_TASK_CORE_THREADS, 400)
                .addHttpListener(4442, "localhost")
                .setHandler(cacheHandler)
                .build();
        server.start();
    }
}
