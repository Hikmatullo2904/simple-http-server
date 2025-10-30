package uz.hikmatullo.httpserver.runtime;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Central place for executors used by the server.
 * - VIRTUAL_EXECUTOR: virtual-thread-per-task executor (Java 21)
 * - CPU_EXECUTOR: fixed platform-thread pool for CPU-bound jobs
 */
public final class ExecutorsHolder {
    private ExecutorsHolder() {
    }


    public static final ExecutorService VIRTUAL_EXECUTOR =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());


    public static final ExecutorService CPU_EXECUTOR =
            Executors.newFixedThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors())
            );


    public static void shutdownAll() {
// Shutdown virtual executor first (it will interrupt none of the carrier threads but will stop accepting tasks)
        try {
            VIRTUAL_EXECUTOR.shutdown();
            if (!VIRTUAL_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                VIRTUAL_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            VIRTUAL_EXECUTOR.shutdownNow();
        }


        try {
            CPU_EXECUTOR.shutdown();
            if (!CPU_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                CPU_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            CPU_EXECUTOR.shutdownNow();
        }
    }
}