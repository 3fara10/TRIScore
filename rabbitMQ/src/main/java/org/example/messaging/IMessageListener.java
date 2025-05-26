package org.example.messaging;

import org.example.messaging.model.EventMessage;

@FunctionalInterface
public interface IMessageListener {
    void onMessageReceived(String routingKey,EventMessage message);
}