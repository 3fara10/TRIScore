package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Result;
import org.example.repository.IRepositoryResult;
import org.example.repository.RepositoryException;
import org.example.service.IResultService;

import java.util.Optional;
import java.util.UUID;

public class ResultService extends Service implements IResultService {
    private static final Logger logger = LogManager.getLogger();
    private final IRepositoryResult resultRepository;

    public ResultService(IRepositoryResult resultRepository) {
        if (resultRepository == null)
            throw new IllegalArgumentException("resultRepository cannot be null");
        this.resultRepository = resultRepository;
    }

    public Optional<Result> addResult(UUID participantId, UUID eventId, int points) {
        logger.info("Adding result: Participant ID: " + participantId + ", Event ID: " + eventId + ", Points: " + points);

        if (points < 0)
            throw new IllegalArgumentException("Points must be a positive number");

        try {
            Optional<Result> result=resultRepository.addOrUpdateResult(participantId, eventId, points);
            notifyObservers();
            return result;
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