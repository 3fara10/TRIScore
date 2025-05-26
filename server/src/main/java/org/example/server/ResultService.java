package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.MessagingService;
import org.example.model.Result;
import org.example.repository.IRepositoryResult;
import org.example.repository.RepositoryException;
import org.example.service.IResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class ResultService extends Service implements IResultService {
    private static final Logger logger = LogManager.getLogger();
    private final IRepositoryResult resultRepository;

    @Autowired(required = false)
    public MessagingService messagingService;

    @Autowired
    public ResultService(IRepositoryResult resultRepository) {
        if (resultRepository == null)
            throw new IllegalArgumentException("resultRepository cannot be null");
        this.resultRepository = resultRepository;
    }

    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public Optional<Result> addResult(UUID participantId, UUID eventId, int points) throws Exception {
        logger.info("Adding result: Participant ID: {}, Event ID: {}, Points: {}",
                participantId, eventId, points);

        if (points < 0)
            throw new IllegalArgumentException("Points must be a positive number");

        try {
            Optional<Result> result = resultRepository.addOrUpdateResult(participantId, eventId, points);

            if (result.isPresent()) {
                Result resultObj = result.get();
                notifyObservers();

                //rabbitmq
                if (messagingService != null && resultObj.getParticipant() != null) {
                    String participantName = resultObj.getParticipant().getName();
                    messagingService.publishResultEvent(participantId, participantName, points, eventId, "added");
                }
                logger.info(" Result added and notifications sent (Observer + RabbitMQ)");
            }

            return result;

        } catch (Exception ex) {
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