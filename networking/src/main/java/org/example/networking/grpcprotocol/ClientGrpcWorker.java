package org.example.networking.grpcprotocol;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.ParticipantResult;
import org.example.model.Referee;
import org.example.model.Result;
import org.example.networking.grpcprotocol.generated.*;
import org.example.service.IAuthentificationService;
import org.example.service.IObserver;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

/**
 * Implementation of the gRPC service logic that handles client requests.
 * This class is used by CompetitionServiceImpl.
 */
public class ClientGrpcWorker implements IObserver {
    private static final Logger logger = LogManager.getLogger(ClientGrpcWorker.class);

    private final IAuthentificationService authService;
    private final IParticipantService participantService;
    private final IResultService resultService;

    // Map to keep track of active client connections for updates
    private final Map<UUID, StreamObserver<UpdateNotification>> clientObservers = new ConcurrentHashMap<>();

    /**
     * Create a new service implementation with the specified services.
     */
    public ClientGrpcWorker(
            IAuthentificationService authService,
            IParticipantService participantService,
            IResultService resultService) {
        this.authService = authService;
        this.participantService = participantService;
        this.resultService = resultService;
        logger.info("Initialized ClientGrpcWorker with services");
    }

    // Remove @Override annotation
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            logger.info("Login request from: {}", request.getUsername());

            try {
                // Attempt to login
                Referee referee = authService.login(
                        request.getUsername(),
                        request.getPassword(),
                        this
                );

                // Create successful response
                LoginResponse response = LoginResponse.newBuilder()
                        .setType(LoginResponse.ResponseType.OK)
                        .setReferee(GrpcProtocolUtils.toGrpcReferee(referee))
                        .build();

                responseObserver.onNext(response);
                logger.info("Login successful for user: {}", request.getUsername());

            } catch (Exception e) {
                // Create error response
                LoginResponse response = LoginResponse.newBuilder()
                        .setType(LoginResponse.ResponseType.ERROR)
                        .setError(e.getMessage())
                        .build();

                responseObserver.onNext(response);
                logger.error("Login failed for user: {}: {}", request.getUsername(), e.getMessage());
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Unexpected error in login: {}", e.getMessage(), e);
            LoginResponse response = LoginResponse.newBuilder()
                    .setType(LoginResponse.ResponseType.ERROR)
                    .setError("Server error: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Remove @Override annotation
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            UUID refereeId = UUID.fromString(request.getRefereeId().getValue());
            logger.info("Logout request for: {}", refereeId);

            try {
                // Attempt to logout
                authService.logout(refereeId);

                // Remove client stream if exists
                clientObservers.remove(refereeId);

                // Create successful response
                LogoutResponse response = LogoutResponse.newBuilder()
                        .setType(LogoutResponse.ResponseType.OK)
                        .build();

                responseObserver.onNext(response);
                logger.info("Logout successful for: {}", refereeId);

            } catch (Exception e) {
                // Create error response
                LogoutResponse response = LogoutResponse.newBuilder()
                        .setType(LogoutResponse.ResponseType.ERROR)
                        .setError(e.getMessage())
                        .build();

                responseObserver.onNext(response);
                logger.error("Logout failed for: {}: {}", refereeId, e.getMessage());
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Unexpected error in logout: {}", e.getMessage(), e);
            LogoutResponse response = LogoutResponse.newBuilder()
                    .setType(LogoutResponse.ResponseType.ERROR)
                    .setError("Server error: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Remove @Override annotation
    public void registerReferee(RegisterRefereeRequest request, StreamObserver<RegisterRefereeResponse> responseObserver) {
        try {
            logger.info("Register referee request for: {}", request.getUsername());

            try {
                // Create event from request
                Event event = new Event(
                        UUID.fromString(request.getEventId().getValue()),
                        request.getEventName()
                );

                // Attempt to register referee
                Referee referee = authService.registerReferee(
                        request.getName(),
                        event,
                        request.getUsername(),
                        request.getPassword(),
                        this
                );

                // Create successful response
                RegisterRefereeResponse response = RegisterRefereeResponse.newBuilder()
                        .setType(RegisterRefereeResponse.ResponseType.OK)
                        .setReferee(GrpcProtocolUtils.toGrpcReferee(referee))
                        .build();

                responseObserver.onNext(response);
                logger.info("Referee registration successful for: {}", request.getUsername());

            } catch (Exception e) {
                // Create error response
                RegisterRefereeResponse response = RegisterRefereeResponse.newBuilder()
                        .setType(RegisterRefereeResponse.ResponseType.ERROR)
                        .setError(e.getMessage())
                        .build();

                responseObserver.onNext(response);
                logger.error("Referee registration failed for: {}: {}", request.getUsername(), e.getMessage());
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Unexpected error in registerReferee: {}", e.getMessage(), e);
            RegisterRefereeResponse response = RegisterRefereeResponse.newBuilder()
                    .setType(RegisterRefereeResponse.ResponseType.ERROR)
                    .setError("Server error: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Remove @Override annotation
    public void getAllParticipantsSortedByName(GetAllParticipantsRequest request,
                                               StreamObserver<GetAllParticipantsResponse> responseObserver) {
        try {
            logger.info("Get all participants sorted by name request");

            try {
                // Fetch participants
                Iterable<ParticipantResult> participants =
                        participantService.getAllParticipantsSortedByNameWithTotalPoints();

                // Create response builder
                GetAllParticipantsResponse.Builder responseBuilder = GetAllParticipantsResponse.newBuilder()
                        .setType(GetAllParticipantsResponse.ResponseType.DATA_FOUND);

                // Add each participant to the response
                StreamSupport.stream(participants.spliterator(), false)
                        .map(GrpcProtocolUtils::toGrpcParticipantResult)
                        .forEach(responseBuilder::addParticipants);

                responseObserver.onNext(responseBuilder.build());
                logger.info("Get all participants request successful");

            } catch (Exception e) {
                // Create error response
                GetAllParticipantsResponse response = GetAllParticipantsResponse.newBuilder()
                        .setType(GetAllParticipantsResponse.ResponseType.ERROR)
                        .setError(e.getMessage())
                        .build();

                responseObserver.onNext(response);
                logger.error("Get all participants request failed: {}", e.getMessage());
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Unexpected error in getAllParticipantsSortedByName: {}", e.getMessage(), e);
            GetAllParticipantsResponse response = GetAllParticipantsResponse.newBuilder()
                    .setType(GetAllParticipantsResponse.ResponseType.ERROR)
                    .setError("Server error: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Remove @Override annotation
    public void getParticipantsWithResultsForEvent(GetParticipantsForEventRequest request,
                                                   StreamObserver<GetParticipantsForEventResponse> responseObserver) {
        try {
            UUID eventId = UUID.fromString(request.getEventId().getValue());
            logger.info("Get participants with results for event: {}", eventId);

            try {
                // Fetch participants with results for the event
                Iterable<ParticipantResult> participants =
                        participantService.getParticipantsWithResultsForEvent(eventId);

                // Create response builder
                GetParticipantsForEventResponse.Builder responseBuilder = GetParticipantsForEventResponse.newBuilder()
                        .setType(GetParticipantsForEventResponse.ResponseType.DATA_FOUND);

                // Add each participant to the response
                StreamSupport.stream(participants.spliterator(), false)
                        .map(GrpcProtocolUtils::toGrpcParticipantResult)
                        .forEach(responseBuilder::addParticipants);

                responseObserver.onNext(responseBuilder.build());
                logger.info("Get participants with results for event request successful");

            } catch (Exception e) {
                // Create error response
                GetParticipantsForEventResponse response = GetParticipantsForEventResponse.newBuilder()
                        .setType(GetParticipantsForEventResponse.ResponseType.ERROR)
                        .setError(e.getMessage())
                        .build();

                responseObserver.onNext(response);
                logger.error("Get participants with results for event request failed: {}", e.getMessage());
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Unexpected error in getParticipantsWithResultsForEvent: {}", e.getMessage(), e);
            GetParticipantsForEventResponse response = GetParticipantsForEventResponse.newBuilder()
                    .setType(GetParticipantsForEventResponse.ResponseType.ERROR)
                    .setError("Server error: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Remove @Override annotation
    public void addResult(AddResultRequest request, StreamObserver<AddResultResponse> responseObserver) {
        try {
            UUID participantId = UUID.fromString(request.getParticipantId().getValue());
            UUID eventId = UUID.fromString(request.getEventId().getValue());
            int points = request.getPoints();

            logger.info("Add result request for participant: {}, event: {}, points: {}",
                    participantId, eventId, points);

            try {
                // Add result
                Optional<Result> resultOpt = resultService.addResult(participantId, eventId, points);

                if (resultOpt.isPresent()) {
                    // Create successful response
                    AddResultResponse response = AddResultResponse.newBuilder()
                            .setType(AddResultResponse.ResponseType.RESULT_ADDED)
                            .setResult(GrpcProtocolUtils.toGrpcResult(resultOpt.get()))
                            .build();

                    responseObserver.onNext(response);
                    logger.info("Add result request successful");

                    // Notify all clients about the update
                    notifyAllClients(UpdateNotification.NotificationType.UPDATE);
                } else {
                    // Create error response
                    AddResultResponse response = AddResultResponse.newBuilder()
                            .setType(AddResultResponse.ResponseType.ERROR)
                            .setError("Failed to add result")
                            .build();

                    responseObserver.onNext(response);
                    logger.error("Add result request failed: Result not added");
                }

            } catch (Exception e) {
                // Create error response
                AddResultResponse response = AddResultResponse.newBuilder()
                        .setType(AddResultResponse.ResponseType.ERROR)
                        .setError(e.getMessage())
                        .build();

                responseObserver.onNext(response);
                logger.error("Add result request failed: {}", e.getMessage());
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Unexpected error in addResult: {}", e.getMessage(), e);
            AddResultResponse response = AddResultResponse.newBuilder()
                    .setType(AddResultResponse.ResponseType.ERROR)
                    .setError("Server error: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Remove @Override annotation
    public void registerForUpdates(RegisterForUpdatesRequest request,
                                   StreamObserver<UpdateNotification> responseObserver) {
        try {
            UUID refereeId = UUID.fromString(request.getRefereeId().getValue());
            logger.info("Register for updates request from: {}", refereeId);

            // Store the stream for sending future updates
            clientObservers.put(refereeId, responseObserver);

            // Send initial update notification
            UpdateNotification notification = UpdateNotification.newBuilder()
                    .setType(UpdateNotification.NotificationType.OBSERVER_REGISTERED)
                    .build();

            responseObserver.onNext(notification);
            logger.info("Register for updates request successful for: {}", refereeId);

            // Note: We don't call onCompleted() here as we want to keep the stream open

        } catch (Exception e) {
            logger.error("Unexpected error in registerForUpdates: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    /**
     * Implementation of IObserver.update() which is called when data changes.
     */
    @Override
    public void update() {
        logger.info("Received update notification, will notify clients");
        notifyAllClients(UpdateNotification.NotificationType.UPDATE);
    }

    /**
     * Helper method to notify all connected clients.
     */
    private void notifyAllClients(UpdateNotification.NotificationType type) {
        UpdateNotification notification = UpdateNotification.newBuilder()
                .setType(type)
                .build();

        // Create a copy of the keys to avoid concurrent modification
        List<UUID> clientKeys = new ArrayList<>(clientObservers.keySet());

        // Track clients to remove due to errors
        List<UUID> clientsToRemove = new ArrayList<>();

        // Send notification to all connected clients
        for (UUID clientId : clientKeys) {
            StreamObserver<UpdateNotification> observer = clientObservers.get(clientId);
            if (observer == null) continue; // Skip if observer was removed

            try {
                observer.onNext(notification);
                logger.debug("Sent update notification to client: {}", clientId);
            } catch (Exception e) {
                logger.error("Failed to send update to client: {}: {}", clientId, e.getMessage());
                clientsToRemove.add(clientId);
            }
        }

        // Remove failed clients after the iteration is complete
        for (UUID clientId : clientsToRemove) {
            clientObservers.remove(clientId);
        }
    }
}