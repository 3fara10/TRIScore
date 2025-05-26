package org.example.server;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.IMessageListener;
import org.example.messaging.MessagingService;
import org.example.messaging.model.EventMessage;
import org.example.messaging.model.MessageType;  // ADD THIS IMPORT
import org.example.service.IObserver;
import org.example.service.IService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Service
public class Service implements IService, IMessageListener {
    protected static final ConcurrentHashMap<UUID, IObserver> observers=new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger();
    protected boolean disposed;

    @Autowired
    MessagingService messagingService;

    Service() {
        logger.info("Initializing Service");
    }

    @PostConstruct
    public void init() {
        if (messagingService != null) {
            try {
                //this service is a listener
                messagingService.addNotificationCallback(this);
                logger.info("Registered Service as notification callback with messaging service");
            } catch (Exception e) {
                logger.error("Failed to register Service as callback: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("MessagingService not available - callbacks will not work");
        }
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

        //rabbit mq
        if (messagingService != null) {
            messagingService.publishSystemNotification("Data updated - please refresh");
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

    private void notifyObserversFromRabbitMQ() {
        logger.info(" Notifying ALL {} connected observers from RabbitMQ", observers.size());

        if (observers.isEmpty()) {
            logger.warn("No observers registered to notify");
            return;
        }

        for (var entry : observers.entrySet()) {
            UUID refereeId = entry.getKey();
            IObserver observer = entry.getValue();

            CompletableFuture.runAsync(() -> {
                try {
                    logger.debug("Notifying observer for referee ID: {}", refereeId);
                    observer.update();
                    logger.debug(" Successfully notified observer for referee ID: {}", refereeId);
                } catch (Exception ex) {
                    logger.error(" Error notifying observer for referee ID {}: {}",
                            refereeId, ex.getMessage(), ex);
                }
            });
        }

        logger.info("✅ Finished notifying all {} observers from RabbitMQ", observers.size());
    }

    @Override
    public void onMessageReceived(String routingKey, EventMessage message) {
        logger.info(" [SERVICE] Received RabbitMQ notification: {} - {}", message.getMessageType(), message.getMessage());

        if (message.getMessageType() == MessageType.RESULT_ADDED || message.getMessageType() == MessageType.RESULT_UPDATED || message.getMessageType() == MessageType.REFEREE_LOGIN || message.getMessageType() == MessageType.REFEREE_LOGOUT) {
            logger.info("📢 Notifying all {} observers due to {} event from RabbitMQ", observers.size(), message.getMessageType());
            notifyObserversFromRabbitMQ();
        } else {
            logger.debug("Ignoring message type: {}", message.getMessageType());
        }
    }
}