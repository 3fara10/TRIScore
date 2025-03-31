package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.Referee;
import org.example.repository.IRepositoryReferee;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuthentificationService extends Service implements IAuthentificationService {
    private final IRepositoryReferee refereeRepository;
    private final IPasswordService passwordService;
    private static final Logger logger = LogManager.getLogger();

    public AuthentificationService(IRepositoryReferee refereeRepository, IPasswordService passwordService) {
        logger.info("Initializing AuthenticationService");
        if (refereeRepository == null)
            throw new IllegalArgumentException("refereeRepository cannot be null");
        if (passwordService == null)
            throw new IllegalArgumentException("passwordService cannot be null");
        this.refereeRepository = refereeRepository;
        this.passwordService = passwordService;
    }

    @Override
    public CompletableFuture<Referee> loginAsync(String username, String password, IObserver observer) {
        logger.info("Attempting login for user: " + username);

        if (username == null || username.isEmpty() || password == null || password.isEmpty())
            throw new IllegalArgumentException("Username and password cannot be empty");

        return refereeRepository.findByUsernameAsync(username)
                .thenCompose(refereeOpt -> {
                    if (!refereeOpt.isPresent()) {
                        logger.error("Login failed: User not found: " + username);
                        throw new IllegalStateException("Invalid username or password");
                    }

                    Referee referee = refereeOpt.get();

                    if (!passwordService.verifyPassword(password, referee.getPassword())) {
                        logger.error("Login failed: Invalid password for user: " + username);
                        throw new IllegalStateException("Invalid username or password");
                    }

                    registerObserver(observer, referee.getId());
                    logger.info("User logged in successfully: " + username);
                    return CompletableFuture.completedFuture(referee);
                });
    }

    @Override
    public CompletableFuture<Void> logoutAsync(UUID refereeId) {
        logger.info("Logging out referee ID: " + refereeId);

        unregisterObserver(refereeId);
        logger.info("Referee ID " + refereeId + " logged out successfully");

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Referee> registerRefereeAsync(String name, Event event, String username, String password, IObserver observer) {
        logger.info("Registering new referee: " + name + ", username: " + username);

        String hashedPassword = passwordService.hashPassword(password);
        Referee referee = new Referee(name, event, username, hashedPassword);

        return refereeRepository.addAsync(referee)
                .thenApply(result -> {
                    if (!result.isPresent()) {
                        logger.error("Failed to add referee: " + name);
                        throw new IllegalStateException("Failed to register referee");
                    }

                    logger.info("Referee registered successfully: " + name + ", ID: " + referee.getId());
                    notifyObservers();
                    return referee;
                });
    }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
            // Dispose managed resources if needed
        }
        super.dispose(disposing);
    }
}
