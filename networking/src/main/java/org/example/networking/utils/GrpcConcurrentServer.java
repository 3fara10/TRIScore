package org.example.networking.utils;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.networking.grpcprotocol.ClientGrpcWorker;
import org.example.networking.grpcprotocol.CompetitionServiceImpl;
import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

import java.io.IOException;
import java.net.Socket;

/**
 * gRPC-based server implementation.
 * Similar to ChatRpcConcurrentServer in the RPC implementation.
 */
public class GrpcConcurrentServer extends AbstractServer {
    private static final Logger logger = LogManager.getLogger(GrpcConcurrentServer.class);

    private final IAuthentificationService authService;
    private final IParticipantService participantService;
    private final IResultService resultService;
    private Server server;

    /**
     * Create a new gRPC server with the specified services.
     */
    public GrpcConcurrentServer(int port, IAuthentificationService authService, IParticipantService participantService, IResultService resultService) {
        super(port);
        this.authService = authService;
        this.participantService = participantService;
        this.resultService = resultService;
        logger.debug("Creating GrpcConcurrentServer...");
    }

    /**
     * Start the gRPC server.
     * Overrides the standard start method to handle gRPC specifics.
     */
    @Override
    public void start() throws ServerException {
        try {
            logger.info("Starting gRPC server on port {}", super.getPort());

            // Create the service implementation that already extends BindableService
            CompetitionServiceImpl serviceImpl = new CompetitionServiceImpl(authService, participantService, resultService);

            // Build and start the server
            server = ServerBuilder.forPort(super.getPort())
                    .addService(serviceImpl)  // No cast needed
                    .build()
                    .start();

            logger.info("gRPC Server started, listening on port {}", super.getPort());

            // Keep the server running until shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down gRPC server due to JVM shutdown");
                try {
                    stop();
                } catch (ServerException e) {
                    logger.error("Error stopping server: {}", e.getMessage());
                }
            }));

            // Block until server is terminated
            server.awaitTermination();

        } catch (IOException e) {
            logger.error("Error starting gRPC server: {}", e.getMessage(), e);
            throw new ServerException("Failed to start gRPC server", e);
        } catch (InterruptedException e) {
            logger.error("gRPC server interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new ServerException("gRPC server interrupted", e);
        }
    }

    /**
     * Stop the gRPC server.

    @Override
    public void stop() throws ServerException {
        if (server != null) {
            logger.info("Shutting down gRPC server...");
            server.shutdown();
            logger.info("gRPC server stopped");
        }
    }

    /**
     * Process a client request.
     * This method is not used in gRPC, but must be implemented to satisfy the AbstractServer contract.
     */
    @Override
    protected void processRequest(Socket client) {
        // Not used in gRPC, as gRPC handles connections internally
    }
}