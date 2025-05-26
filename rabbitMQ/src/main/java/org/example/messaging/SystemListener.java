package org.example.messaging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.model.EventMessage;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SystemListener implements IMessageListener {

    private static final Logger logger = LogManager.getLogger(SystemListener.class);

    private final CopyOnWriteArrayList<IMessageListener> callbacks = new CopyOnWriteArrayList<>();

    @Override
    public void onMessageReceived(String x,EventMessage message) {
        logger.info("🎯 [SYSTEM] Received notification: {}", message.getMessage());
        processMessage(message);
        notifyCallbacks(message);
    }

    private void processMessage(EventMessage message) {
        logger.info("Processing: {}", message.getMessageType());
        logger.info("System updated successfully");
    }

    private void notifyCallbacks(EventMessage message) {
        if (!callbacks.isEmpty()) {
            logger.info("Notifying {} registered callbacks", callbacks.size());

            for (IMessageListener callback : callbacks) {
                try {
                    callback.onMessageReceived("",message);
                } catch (Exception e) {
                    logger.error("Error notifying callback", e);
                }
            }
        }
    }

    public void addCallback(IMessageListener callback) {
        callbacks.add(callback);
        logger.info("Added notification callback. Total callbacks: {}", callbacks.size());
    }

    public void removeCallback(IMessageListener callback) {
        callbacks.remove(callback);
        logger.info("Removed notification callback. Total callbacks: {}", callbacks.size());
    }
}
