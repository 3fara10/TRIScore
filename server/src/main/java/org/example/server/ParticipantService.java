package org.example.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.ParticipantResult;
import org.example.repository.IRepositoryDTO;
import org.example.service.IParticipantService;

import java.util.UUID;

public class ParticipantService extends Service implements IParticipantService {
    private static final Logger logger = LogManager.getLogger();
    private final IRepositoryDTO repositoryDto;

    public ParticipantService(IRepositoryDTO repositoryDto) {
        this.repositoryDto = repositoryDto;
    }

    @Override
    public Iterable<ParticipantResult> getAllParticipantsSortedByNameWithTotalPoints() {
        logger.info("Getting all participants sorted by name");

        try {
            return repositoryDto.getAllParticipantsSortedByNameWithTotalPoints();
        } catch (Exception ex) {
            logger.error("Error getting participants", ex);
            throw ex;
        }
    }

    @Override
    public Iterable<ParticipantResult> getParticipantsWithResultsForEvent(UUID eventId) {
        logger.info("Getting participants with results for event ID: " + eventId);

        try {
            return repositoryDto.getParticipantsWithResultsForEvent(eventId);
        } catch (Exception ex) {
            logger.error("Error getting participants with results for event ID: " + eventId, ex);
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
