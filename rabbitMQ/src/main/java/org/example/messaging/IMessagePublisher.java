package org.example.messaging;

import org.example.messaging.model.EventMessage;

public interface IMessagePublisher extends AutoCloseable {
    void publishMessage(String routingKey, EventMessage message) throws MessagingException;
    void publishResultMessage(EventMessage message) throws MessagingException;
    void publishRefereeMessage(EventMessage message) throws MessagingException;
    void publishNotification(EventMessage message) throws MessagingException;
    boolean isHealthy();
}