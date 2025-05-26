package org.example.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.MessagingService;
import org.example.model.Event;
import org.example.model.Referee;
import org.example.repository.IRepositoryReferee;
import org.example.service.IAuthentificationService;
import org.example.service.IObserver;
import org.example.service.IPasswordService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class AuthentificationService extends Service implements IAuthentificationService {
    private final IRepositoryReferee refereeRepository;
    private final IPasswordService passwordService;
    private static final Logger logger = LogManager.getLogger();

    @Autowired(required = false)
    public MessagingService messagingService;

    @Autowired
    public AuthentificationService(IRepositoryReferee refereeRepository, IPasswordService passwordService) {
        logger.info("Initializing AuthenticationService");
        if (refereeRepository == null) throw new IllegalArgumentException("refereeRepository cannot be null");
        if (passwordService == null) throw new IllegalArgumentException("passwordService cannot be null");
        this.refereeRepository = refereeRepository;
        this.passwordService = passwordService;
    }

    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public Referee login(String username, String password, IObserver observer) {
        try {
            logger.info("Attempting login for user: {}", username);

            if (username == null || username.isEmpty() || password == null || password.isEmpty())
                throw new IllegalArgumentException("Username and password cannot be empty");

            for (Map.Entry<UUID, IObserver> entry : observers.entrySet()) {
                UUID registeredRefereeId = entry.getKey();
                IObserver registeredObserver = entry.getValue();

                Optional<Referee> existingReferee = refereeRepository.findOne(registeredRefereeId);
                if (existingReferee.isPresent() && existingReferee.get().getUsername().equals(username)) {
                    logger.warn("Login failed: user already logged in: {}", username);
                    throw new IllegalArgumentException("User already logged in");
                }
            }

            Optional<Referee> refereeOpt = refereeRepository.findByUsername(username);
            if (refereeOpt.isEmpty()) {
                logger.error("Login failed: User not found: {}", username);
                throw new IllegalStateException("Invalid username or password");
            }

            Referee referee = refereeOpt.get();

            if (observers.containsKey(referee.getId()) && (observers.get(referee.getId()) == null)) {
                logger.warn("Login failed: user already logged in: {}", username);
                throw new IllegalArgumentException("User already logged in");
            }

            if (!passwordService.verifyPassword(password, referee.getPassword())) {
                logger.error("Login failed: Invalid password for user: {}", username);
                throw new IllegalStateException("Invalid username or password");
            }

            registerObserver(observer, referee.getId());
            logger.info("User logged in successfully: {}", username);

            //rabbit mq
            if (messagingService != null) {
                UUID eventId = referee.getEvent() != null ? referee.getEvent().getId() : null;
                messagingService.publishRefereeEvent(referee.getId(), referee.getName(), eventId, "login");
            }
            logger.info(" User logged in successfully (Observer + RabbitMQ): {}", username);

            return referee;
        }catch (IllegalArgumentException ex){
            throw new IllegalArgumentException("Invalid username or password");
        }
    }

    @Override
    public void logout(UUID refereeId) {
        logger.info("Logging out referee ID: {}", refereeId);

        unregisterObserver(refereeId);
        logger.info("Referee ID {} logged out successfully", refereeId);

        //rabbit mq
        if (messagingService != null && refereeId!=null) {
            Referee referee = this.refereeRepository.findOne(refereeId).get();
            UUID eventId = referee.getEvent() != null ? referee.getEvent().getId() : null;
            messagingService.publishRefereeEvent(refereeId, referee.getName(), eventId, "logout");
        }
    }

    @Override
    public Referee registerReferee(String name, Event event, String username, String password, IObserver observer) {
        logger.info("Registering new referee: {}, username: {}", name, username);

        String hashedPassword = passwordService.hashPassword(password);
        Referee referee = new Referee(name, event, username, hashedPassword);

        Optional<Referee> result = refereeRepository.add(referee);

        if (result.isEmpty()) {
            logger.error("Failed to add referee: {}", name);
            throw new IllegalStateException("Failed to register referee");
        }

        logger.info("Referee registered successfully: {}, ID: {}", name, referee.getId());
         notifyObservers();
         //rabbit mq
        if (messagingService != null) {
            UUID eventId = event != null ? event.getId() : null;
            messagingService.publishRefereeEvent(referee.getId(), name, eventId, "registered");
        }

        return referee;
    }

    @Override
    public void registerObserver(IObserver observer, UUID refereeId) {
        logger.info("Registering observer for referee ID: {refereeId}");

        if (observer == null) {
            logger.warn("Attempted to register null observer for referee ID: {refereeId}");
        } else {
            super.registerObserver(observer, refereeId);
            logger.debug("Observer registered successfully for referee ID: {refereeId}");
        }

    }

    @Override
    public void unregisterObserver(UUID refereeId) {
        logger.info("Unregistering observer for referee ID: {refereeId}");
        super.unregisterObserver(refereeId);
        logger.debug("Observer unregistered successfully for referee ID: {refereeId}");
    }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
            logger.info("Disposing AuthenticationService");
        }
        super.dispose(disposing);
    }
}