package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Service implements IService{
    protected final ConcurrentHashMap<UUID, IObserver> observers;
    private static final Logger logger = LogManager.getLogger();
    protected boolean disposed;

    protected Service() {
        logger.info("Initializing Service");
        observers = new ConcurrentHashMap<>();
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
        for (IObserver observer : observers.values()) {
            try {
                observer.update();
            } catch (Exception ex) {
                logger.error("Error notifying observer", ex);
            }
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
