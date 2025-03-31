package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.exceptions.RepositoryException;
import org.example.repository.IRepositoryResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ResultService extends Service implements IResultService {
    private static final Logger logger = LogManager.getLogger();
    private final IRepositoryResult resultRepository;

    public ResultService(IRepositoryResult resultRepository) {
        if (resultRepository == null)
            throw new IllegalArgumentException("resultRepository cannot be null");
        this.resultRepository = resultRepository;
    }

    @Override
    public CompletableFuture<Void> addResultAsync(UUID participantId, UUID eventId, int points) {
        logger.info("Adding result: Participant ID: " + participantId + ", Event ID: " + eventId + ", Points: " + points);

        if (points < 0)
            throw new IllegalArgumentException("Points must be a positive number");

        try {
            return resultRepository.addOrUpdateResultAsync(participantId, eventId, points)
                    .thenRun(this::notifyObservers);
        } catch (RepositoryException ex) {
            logger.error("Error adding result", ex);
            throw ex;
        }
    }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
            // Dispose managed resources if needed
        }
        super.dispose(disposing);
    }
}