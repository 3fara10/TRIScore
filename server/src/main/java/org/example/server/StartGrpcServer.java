package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.networking.utils.GrpcConcurrentServer;
import org.example.networking.utils.ServerException;
import org.example.repository.*;
import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IPasswordService;
import org.example.service.IResultService;

import java.io.IOException;
import java.util.Properties;

/**
 * Startup class for the gRPC server.
 */
public class StartGrpcServer {
    private static final Logger logger = LogManager.getLogger(StartGrpcServer.class);

    public static void main(String[] args) {
        // Load server properties
        Properties serverProps = new Properties();
        try {
            serverProps.load(StartGrpcServer.class.getResourceAsStream("/server.properties"));
            logger.info("Server properties set. {}", serverProps);
        } catch (IOException e) {
            logger.error("Cannot find server.properties: {}", e.getMessage());
            logger.debug("Looking for file in {}", (new java.io.File(".")).getAbsolutePath());
            return;
        }

        // Initialize repositories
        IRepositoryEvent repositoryEvent = new SQLRepositoryEvent(serverProps);
        IRepositoryParticipant repositoryParticipant = new SQLRepositoryParticipant(serverProps);
        IRepositoryResult repositoryResult = new SQLRepositoryResult(serverProps);
        IRepositoryReferee repositoryReferee = new SQLRepositoryReferee(serverProps);
        IRepositoryDTO repositoryDTO = new SQLRepositoryDTO(serverProps);

        // Initialize services
        IPasswordService passwordService = new BcryptPasswordService(12);
        IAuthentificationService authenticationService = new AuthentificationService(repositoryReferee, passwordService);
        IParticipantService participantService = new ParticipantService(repositoryDTO);
        IResultService resultService = new ResultService(repositoryResult);

        // Get server port
        int serverPort = 0;
        try {
            serverPort = Integer.parseInt(serverProps.getProperty("server.port"));
        } catch (NumberFormatException nef) {
            logger.error("Wrong Port Number: {}", nef.getMessage());
            return;
        }

        logger.debug("Starting gRPC server on port: {}", serverPort);

        // Create and start gRPC server
        GrpcConcurrentServer server = new GrpcConcurrentServer(serverPort, authenticationService, participantService, resultService);

        try {
            server.start();
            logger.debug("gRPC Server started...");
        } catch (ServerException e) {
            logger.error("Error starting the gRPC server: {}", e.getMessage());
        }
    }
}