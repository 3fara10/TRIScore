package org.example.messaging;


public interface IMessageConsumer extends AutoCloseable {

    void subscribe(String routingKey, IMessageListener listener) throws MessagingException;

    void unsubscribe(String routingKey) throws MessagingException;

    void startConsuming() throws MessagingException;

    void stopConsuming() throws MessagingException;

    boolean isHealthy();
}