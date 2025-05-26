package org.example.messaging;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.logging.log4j.LogManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.Logger;
import org.example.messaging.model.EventMessage;


public class RabbitMQPublisher implements IMessagePublisher {

    private static final Logger logger = LogManager.getLogger(RabbitMQPublisher.class);

    private final ObjectMapper objectMapper;
    private Connection connection;
    private Channel channel;
    private volatile boolean isHealthy = false;

    public RabbitMQPublisher(String host, int port, String username, String password) {
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

            channel.exchangeDeclare(RoutingKeys.SPORTS_EXCHANGE, "topic", true);
            channel.queueDeclare(RoutingKeys.MAIN_QUEUE, true, false, false, null);
            channel.queueBind(RoutingKeys.MAIN_QUEUE, RoutingKeys.SPORTS_EXCHANGE, "#");

            isHealthy = true;
            logger.info(" RabbitMQ Publisher initialized successfully");

        } catch (Exception e) {
            isHealthy = false;
            logger.error(" Failed to initialize RabbitMQ Publisher: {}", e.getMessage(), e);
        }
    }

    @Override
    public void publishMessage(String routingKey, EventMessage message) throws MessagingException {
        if (!isHealthy()) {
            throw new MessagingException("Publisher is not healthy");
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            channel.basicPublish(RoutingKeys.SPORTS_EXCHANGE, routingKey, null, jsonMessage.getBytes("UTF-8"));
            logger.info(" Published [{}]: {}", routingKey, message.getMessage());

        } catch (Exception e) {
            logger.error("Error publishing [{}]: {}", routingKey, e.getMessage(), e);
            throw new MessagingException("Failed to publish message", e);
        }
    }

    @Override
    public void publishResultMessage(EventMessage message) throws MessagingException {
        publishMessage(RoutingKeys.RESULT_ADDED, message);
    }

    @Override
    public void publishRefereeMessage(EventMessage message) throws MessagingException {
        String routingKey = switch (message.getMessageType()) {
            case REFEREE_LOGIN -> RoutingKeys.REFEREE_LOGIN;
            case REFEREE_LOGOUT -> RoutingKeys.REFEREE_LOGOUT;
            case REFEREE_REGISTERED -> RoutingKeys.REFEREE_REGISTERED;
            default -> "referee.unknown";
        };
        publishMessage(routingKey, message);
    }

    @Override
    public void publishNotification(EventMessage message) throws MessagingException {
        publishMessage(RoutingKeys.SYSTEM_NOTIFICATIONS, message);
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
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
            logger.info(" RabbitMQ Publisher closed");
        } catch (Exception e) {
            logger.error(" Error closing Publisher: {}", e.getMessage(), e);
            throw e;
        }
    }
}