package org.example.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
            new ThreadPoolExecutor.CallerRunsPolicy() // politica de respingere
    );

    public static Executor getRepositoryExecutor() {
        return REPOSITORY_EXECUTOR;
    }

    public static void shutdownExecutors() {
        REPOSITORY_EXECUTOR.shutdown();
        try {
            if (!REPOSITORY_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                REPOSITORY_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            REPOSITORY_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}