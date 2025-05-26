package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.model.EventMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RabbitMQConsumer implements IMessageConsumer {

    private static final Logger logger = LogManager.getLogger(RabbitMQConsumer.class);

    private final ObjectMapper objectMapper;
    private Connection connection;
    private Channel channel;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<IMessageListener>> listeners;
    private volatile boolean isHealthy = false;

    public RabbitMQConsumer(String host, int port, String username, String password) {
        this.listeners = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setAutomaticRecoveryEnabled(true);

            connection = factory.newConnection();
            channel = connection.createChannel();

            isHealthy = true;
            logger.info("RabbitMQ Consumer initialized successfully");

        } catch (Exception e) {
            isHealthy = false;
            logger.error("Failed to initialize RabbitMQ Consumer: {}", e.getMessage(), e);
        }
    }

    @Override
    public void subscribe(String routingKey, IMessageListener listener) throws MessagingException {
        if (!isHealthy()) {
            throw new MessagingException("Consumer is not healthy");
        }

        listeners.computeIfAbsent(routingKey, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.info(" Subscribed listener to: {}", routingKey);
    }

    @Override
    public void unsubscribe(String routingKey) throws MessagingException {
        listeners.remove(routingKey);
        logger.info("Unsubscribed from: {}", routingKey);
    }

    @Override
    public void startConsuming() throws MessagingException {
        if (!isHealthy()) {
            throw new MessagingException("Consumer is not healthy");
        }

        try {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    String routingKey = delivery.getEnvelope().getRoutingKey();

                    EventMessage eventMessage = objectMapper.readValue(message, EventMessage.class);
                    notifyListeners(routingKey, eventMessage);

                } catch (Exception e) {
                    logger.error(" Error processing message: {}", e.getMessage(), e);
                }
            };

            channel.basicConsume(RoutingKeys.MAIN_QUEUE, true, deliverCallback, consumerTag -> {});
            logger.info("Started consuming messages");

        } catch (Exception e) {
            logger.error(" Failed to start consuming: {}", e.getMessage(), e);
            throw new MessagingException("Failed to start consuming", e);
        }
    }

    private void notifyListeners(String routingKey, EventMessage message) {
        listeners.values().forEach(listenerList -> {
            for (IMessageListener listener : listenerList) {
                try {
                    listener.onMessageReceived("",message);
                } catch (Exception e) {
                    logger.error(" Error notifying listener: {}", e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void stopConsuming() throws MessagingException {
        logger.info("Stopped consuming");
    }

    @Override
    public boolean isHealthy() {
        return isHealthy && connection != null && connection.isOpen() &&
                channel != null && channel.isOpen();
    }

    @Override
    public void close() throws Exception {
        try {
            isHealthy = false;
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            listeners.clear();
            logger.info(" RabbitMQ Consumer closed");
        } catch (Exception e) {
            logger.error(" Error closing Consumer: {}", e.getMessage(), e);
            throw e;
        }
    }
}