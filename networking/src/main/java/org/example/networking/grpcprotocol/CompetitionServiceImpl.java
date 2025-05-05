package org.example.networking.grpcprotocol;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.networking.grpcprotocol.generated.*;
import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

/**
 * Implementation of the Competition gRPC service.
 * This class handles all the gRPC requests related to the competition system.
 * It delegates the actual business logic to the ClientGrpcWorker.
 */
public class CompetitionServiceImpl extends CompetitionServiceGrpc.CompetitionServiceImplBase {
    private static final Logger logger = LogManager.getLogger(CompetitionServiceImpl.class);

    private final ClientGrpcWorker worker;

    /**
     * Create a new Competition service implementation with the specified services.
     */
    public CompetitionServiceImpl(
            IAuthentificationService authService,
            IParticipantService participantService,
            IResultService resultService) {

        this.worker = new ClientGrpcWorker(authService, participantService, resultService);
        logger.info("CompetitionServiceImpl initialized with services");
    }

    /**
     * Handle login requests.
     */
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        logger.debug("Received login request, delegating to worker");
        worker.login(request, responseObserver);
    }

    /**
     * Handle logout requests.
     */
    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        logger.debug("Received logout request, delegating to worker");
        worker.logout(request, responseObserver);
    }

    /**
     * Handle referee registration requests.
     */
    @Override
    public void registerReferee(RegisterRefereeRequest request, StreamObserver<RegisterRefereeResponse> responseObserver) {
        logger.debug("Received register referee request, delegating to worker");
        worker.registerReferee(request, responseObserver);
    }

    /**
     * Handle requests to get all participants sorted by name.
     */
    @Override
    public void getAllParticipantsSortedByName(GetAllParticipantsRequest request,
                                               StreamObserver<GetAllParticipantsResponse> responseObserver) {
        logger.debug("Received get all participants request, delegating to worker");
        worker.getAllParticipantsSortedByName(request, responseObserver);
    }

    /**
     * Handle requests to get participants with results for a specific event.
     */
    @Override
    public void getParticipantsWithResultsForEvent(GetParticipantsForEventRequest request,
                                                   StreamObserver<GetParticipantsForEventResponse> responseObserver) {
        logger.debug("Received get participants for event request, delegating to worker");
        worker.getParticipantsWithResultsForEvent(request, responseObserver);
    }

    /**
     * Handle requests to add a result.
     */
    @Override
    public void addResult(AddResultRequest request, StreamObserver<AddResultResponse> responseObserver) {
        logger.debug("Received add result request, delegating to worker");
        worker.addResult(request, responseObserver);
    }

    /**
     * Handle requests to register for updates.
     * This creates a server streaming connection that sends notifications when data changes.
     */
    @Override
    public void registerForUpdates(RegisterForUpdatesRequest request,
                                   StreamObserver<UpdateNotification> responseObserver) {
        logger.debug("Received register for updates request, delegating to worker");
        worker.registerForUpdates(request, responseObserver);
    }
}