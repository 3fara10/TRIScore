package org.example.utils;

import java.util.concurrent.*;

public class ThreadPoolManager {
    private static final ExecutorService REPOSITORY_EXECUTOR = new ThreadPoolExecutor(
            5,
            20,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            r -> {
                Thread t = new Thread(r);
                t.setName("RepositoryThread-" + t.getId());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static Executor getRepositoryExecutor() {
        return REPOSITORY_EXECUTOR;
    }

    public static void shutdownExecutors() {
        try {
            ForkJoinPool.commonPool().shutdown();
            if (!ForkJoinPool.commonPool().awaitTermination(5, TimeUnit.SECONDS)) {
                ForkJoinPool.commonPool().shutdownNow();
            }

            if (REPOSITORY_EXECUTOR != null && !REPOSITORY_EXECUTOR.isShutdown()) {
                REPOSITORY_EXECUTOR.shutdown();
                if (!REPOSITORY_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    REPOSITORY_EXECUTOR.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}