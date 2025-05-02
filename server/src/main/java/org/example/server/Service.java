package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.service.IObserver;
import org.example.service.IService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Service implements IService {
    protected static final ConcurrentHashMap<UUID, IObserver> observers=new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger();
    protected boolean disposed;

    protected Service() {
        logger.info("Initializing Service");
    }

    @Override
    public void registerObserver(IObserver observer, UUID refereeId) {
        if (observer == null)
            throw new IllegalArgumentException("Observer cannot be null");

        observers.put(refereeId, observer);
        logger.info("Observer registered for referee ID " + refereeId);
    }

    @Override
    public void unregisterObserver(UUID refereeId) {
        observers.remove(refereeId);
        logger.info("Observer unregistered for referee ID " + refereeId);
    }

    protected void notifyObservers() {
        logger.debug("Notifying {} observers", observers.size());

        for (var observer : observers.values()) {
            CompletableFuture.runAsync(() -> {
                try {
                    observer.update();
                } catch (Exception ex) {
                    logger.error("Error notifying observer: {}", ex.getMessage(), ex);
                }
            });
        }
    }

    @Override
    public void close() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (!disposed) {
            if (disposing) {
                observers.clear();
            }
            disposed = true;
        }
    }
}
