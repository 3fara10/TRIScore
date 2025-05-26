package org.example.messaging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    private static final Logger logger = LogManager.getLogger(MessagingConfig.class);

    @Value("${rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${rabbitmq.port:5672}")
    private int rabbitPort;

    @Value("${rabbitmq.username:guest}")
    private String rabbitUsername;

    @Value("${rabbitmq.password:guest}")
    private String rabbitPassword;

    @Bean
    public IMessagePublisher messagePublisher() {
        try {
            logger.info("Creating RabbitMQ Publisher: {}:{}", rabbitHost, rabbitPort);
            return new RabbitMQPublisher(rabbitHost, rabbitPort, rabbitUsername, rabbitPassword);
        } catch (Exception e) {
            logger.error("Failed to create RabbitMQ Publisher: {}", e.getMessage(), e);
            return new NoOpMessagePublisher();
        }
    }

    @Bean
    public IMessageConsumer messageConsumer() {
        try {
            logger.info("Creating RabbitMQ Consumer: {}:{}", rabbitHost, rabbitPort);
            return new RabbitMQConsumer(rabbitHost, rabbitPort, rabbitUsername, rabbitPassword);
        } catch (Exception e) {
            logger.error("Failed to create RabbitMQ Consumer: {}", e.getMessage(), e);
            return new NoOpMessageConsumer();
        }
    }

    @Bean
    public org.example.messaging.SystemListener systemListener() {
        return new org.example.messaging.SystemListener();
    }

    @Bean
    public MessagingService messagingService(IMessagePublisher publisher,
                                             IMessageConsumer consumer,
                                             org.example.messaging.SystemListener systemListener) {
        return new MessagingService(publisher, consumer, systemListener);
    }


    private static class NoOpMessagePublisher implements IMessagePublisher {
        private static final Logger logger = LogManager.getLogger(NoOpMessagePublisher.class);

        @Override
        public void publishMessage(String routingKey, org.example.messaging.model.EventMessage message) {
            logger.warn("📝 No-op: would publish [{}]: {}", routingKey, message.getMessage());
        }

        @Override
        public void publishResultMessage(org.example.messaging.model.EventMessage message) {
            publishMessage("event.results", message);
        }

        @Override
        public void publishRefereeMessage(org.example.messaging.model.EventMessage message) {
            publishMessage("referee.events", message);
        }

        @Override
        public void publishNotification(org.example.messaging.model.EventMessage message) {
            publishMessage("system.notifications", message);
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() {}
    }

    private static class NoOpMessageConsumer implements IMessageConsumer {
        @Override
        public void subscribe(String routingKey, IMessageListener listener) {}
        @Override
        public void unsubscribe(String routingKey) {}
        @Override
        public void startConsuming() {}
        @Override
        public void stopConsuming() {}
        @Override
        public boolean isHealthy() { return true; }
        @Override
        public void close() {}
    }
}
