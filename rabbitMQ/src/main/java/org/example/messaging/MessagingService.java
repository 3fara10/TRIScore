package org.example.messaging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.model.EventMessage;
import org.example.messaging.model.MessageType;

import java.util.UUID;
public class MessagingService {

    private static final Logger logger = LogManager.getLogger(MessagingService.class);

    private final IMessagePublisher publisher;
    private final IMessageConsumer consumer;
    private final SystemListener systemListener;

    public MessagingService(IMessagePublisher publisher,
                            IMessageConsumer consumer,
                            SystemListener systemListener) {
        this.publisher = publisher;
        this.consumer = consumer;
        this.systemListener = systemListener;
    }

    @PostConstruct
    public void initialize() {
        try {
            consumer.subscribe("all", systemListener);
            consumer.startConsuming();
            logger.info("Messaging service initialized with single listener");

        } catch (Exception e) {
            logger.error("Failed to initialize messaging service: {}", e.getMessage(), e);
        }
    }

    public void addNotificationCallback(IMessageListener callback) {
        systemListener.addCallback(callback);
    }

    public void publishResultEvent(UUID participantId, String participantName,
                                   int points, UUID eventId, String action) {
        try {
            MessageType messageType = "added".equals(action) ? MessageType.RESULT_ADDED : MessageType.RESULT_UPDATED;
            EventMessage message = EventMessage.builder().messageType(messageType).message(String.format("Result %s: %s scored %d points", action, participantName, points)).participant(participantId, participantName).points(points).eventId(eventId).build();
            publisher.publishResultMessage(message);
            logger.info("Published result event: {} for {}", action, participantName);

        } catch (Exception e) {
            logger.error("Failed to publish result event: {}", e.getMessage(), e);
        }
    }

    public void publishRefereeEvent(UUID refereeId, String refereeName,
                                    UUID eventId, String action) {
        try {
            MessageType messageType = switch (action.toLowerCase()) {
                case "login" -> MessageType.REFEREE_LOGIN;
                case "logout" -> MessageType.REFEREE_LOGOUT;
                case "registered" -> MessageType.REFEREE_REGISTERED;
                default -> throw new IllegalArgumentException("Unknown referee action: " + action);
            };

            EventMessage message = EventMessage.builder().messageType(messageType).message(String.format("Referee %s: %s", action, refereeName)).referee(refereeId, refereeName).eventId(eventId).build();
            publisher.publishRefereeMessage(message);
            logger.info("Published referee event: {} for {}", action, refereeName);

        } catch (Exception e) {
            logger.error("Failed to publish referee event: {}", e.getMessage(), e);
        }
    }

    public void publishSystemNotification(String message) {
        try {
            EventMessage eventMessage = EventMessage.builder().messageType(MessageType.SYSTEM_NOTIFICATION).message(message).build();
            publisher.publishNotification(eventMessage);
            logger.info("Published system notification: {}", message);

        } catch (Exception e) {
            logger.error("Failed to publish system notification: {}", e.getMessage(), e);
        }
    }

    public boolean isHealthy() {
        return publisher.isHealthy() && consumer.isHealthy();
    }

    @PreDestroy
    public void cleanup() {
        try {
            consumer.stopConsuming();
            consumer.close();
            publisher.close();
            logger.info("Messaging service cleaned up");
        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }
}
