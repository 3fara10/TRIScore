package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.ParticipantResult;
import org.example.model.Referee;
import org.example.model.Result;
import org.example.networking.dto.DTOUtils;
import org.example.networking.dto.ParticipantResultDTO;
import org.example.service.IAuthentificationService;
import org.example.service.IObserver;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

/**
 * Controller class for the triathlon client application.
 * Handles communication between the UI and the server services.
 */
public class ClientCtrl implements IObserver {
    public interface UpdateListener {
        void onUpdate(ClientEvent event);
    }

    private final List<UpdateListener> listeners = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(ClientCtrl.class);

    private final IAuthentificationService authenticationServer;
    private final IParticipantService participantServer;
    private final IResultService resultServer;

    private Referee currentReferee;

    public ClientCtrl(
            IAuthentificationService authenticationServer,
            IParticipantService participantServer,
            IResultService resultServer) {
        this.authenticationServer = authenticationServer;
        this.participantServer = participantServer;
        this.resultServer = resultServer;
        logger.debug("TriathlonClientCtrl initialized");
    }

    public void addUpdateListener(UpdateListener listener) {
        listeners.add(listener);
    }

    public void removeUpdateListener(UpdateListener listener) {
        listeners.remove(listener);
    }


    public void login(String username, String password) throws Exception {
        try {
            this.currentReferee = authenticationServer.login(username, password, this);
            logger.debug("Login succeeded for referee: {}", currentReferee.getName());
            notifyListeners(ClientEvent.LOGIN_SUCCESSFUL);
        } catch (Exception ex) {
            logger.error("Login failed", ex);
            throw ex;
        }
    }

    public void logout() throws Exception {
        if (currentReferee == null) {
            logger.warn("Attempted logout without active user");
            return;
        }

        try {
            authenticationServer.logout(currentReferee.getId());
            logger.debug("Logout successful");
            notifyListeners(ClientEvent.LOGOUT_SUCCESSFUL);
            currentReferee = null;
        } catch (Exception ex) {
            logger.error("Logout failed", ex);
            throw ex;
        }
    }

    public Referee registerReferee(String name, Event evt, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    public CompletableFuture<Result> addResultAsync(UUID participantId, int points) {
        if (currentReferee == null) {
            logger.warn("Attempted to add result without active referee");
            CompletableFuture<Result> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("No active referee"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID eventId = currentReferee.getEvent().getId();
                logger.debug("Adding result on background thread for participant {} in event {} with points {}",
                        participantId, eventId, points);

                Optional<Result> resultOption = resultServer.addResult(participantId, eventId, points);

                if (resultOption.isPresent()) {
                    Result result = resultOption.get();
                    logger.debug("Result added for participant {} with points {}", participantId, points);
                    return result;
                }

                throw new Exception("Failed to add result - no result returned");
            } catch (Exception ex) {
                logger.error("Error adding result: {}", ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        });
    }


    public ParticipantResultDTO[] getAllParticipantsSortedByName() throws Exception {
        try {
            Iterable<ParticipantResult> participants = participantServer.getAllParticipantsSortedByNameWithTotalPoints();

            ParticipantResult[] participantsArray = StreamSupport
                    .stream(participants.spliterator(), false)
                    .toArray(ParticipantResult[]::new);

            logger.debug("Retrieved {} participants sorted by name", participantsArray.length);
            return DTOUtils.getDTO(participantsArray);
        } catch (Exception ex) {
            logger.error("Error retrieving sorted participants", ex);
            throw ex;
        }
    }


    public ParticipantResultDTO[] getParticipantsWithResultsForEvent() throws Exception {
        if (currentReferee == null) {
            logger.warn("Attempted to get participants without active referee");
            return new ParticipantResultDTO[0];
        }

        try {
            Iterable<ParticipantResult> participants =
                    participantServer.getParticipantsWithResultsForEvent(currentReferee.getEvent().getId());

            // Convert Iterable to array
            ParticipantResult[] participantsArray = StreamSupport
                    .stream(participants.spliterator(), false)
                    .toArray(ParticipantResult[]::new);

            return DTOUtils.getDTO(participantsArray);
        } catch (Exception ex) {
            logger.error("Error getting participants with results", ex);
            throw ex;
        }
    }

    @Override
    public void update() {
        logger.debug("Received update from service");
        notifyListeners(ClientEvent.EVENT_STATUS_CHANGED);
    }


    protected void notifyListeners(ClientEvent event) {
        listeners.forEach(listener -> listener.onUpdate(event));
        logger.debug("Update event triggered: {}", event);
    }


    public Referee getCurrentReferee() {
        return currentReferee;
    }


    public enum ClientEvent {
        LOGIN_SUCCESSFUL,
        LOGOUT_SUCCESSFUL,
        EVENT_STATUS_CHANGED
    }
}