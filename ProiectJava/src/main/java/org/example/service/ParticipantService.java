package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.ParticipantResultDTO;
import org.example.repository.IRepositoryDTO;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ParticipantService extends Service implements IParticipantService{
    private static final Logger logger = LogManager.getLogger();
    private final IRepositoryDTO repositoryDto;

    public ParticipantService(IRepositoryDTO repositoryDto) {
        this.repositoryDto = repositoryDto;
    }

    @Override
    public CompletableFuture<Iterable<ParticipantResultDTO>> getAllParticipantsSortedByNameAsync() {
        logger.info("Getting all participants sorted by name");

        try {
            return repositoryDto.getAllParticipantsSortedByNameWithPointsAsync();
        } catch (Exception ex) {
            logger.error("Error getting participants", ex);
            throw ex;
        }
    }

    @Override
    public CompletableFuture<Iterable<ParticipantResultDTO>> getParticipantsWithResultsForEventAsync(UUID eventId) {
        logger.info("Getting participants with results for event ID: " + eventId);

        try {
            return repositoryDto.getParticipantsWithResultsForEventAsync(eventId);
        } catch (Exception ex) {
            logger.error("Error getting participants with results for event ID: " + eventId, ex);
            throw ex;
        }
    }

    @Override
    public CompletableFuture<Integer> getTotalPointsForParticipantAsync(UUID participantId) {
        logger.info("Getting total points for participant ID: " + participantId);
        try {
            return repositoryDto.getTotalPointsForParticipantAsync(participantId);
        } catch (Exception ex) {
            logger.error("Error getting total points for participant ID: " + participantId, ex);
            throw ex;
        }
    }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
        }
        super.dispose(disposing);
    }
}
