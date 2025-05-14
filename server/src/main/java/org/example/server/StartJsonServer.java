package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.networking.utils.AbstractServer;
import org.example.networking.utils.JsonConcurrentServer;
import org.example.networking.utils.ServerException;
import org.example.repository.*;
import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IPasswordService;
import org.example.service.IResultService;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class StartJsonServer {
    private static Logger logger = LogManager.getLogger(StartJsonServer.class);
    public static void main(String[] args) throws Exception {
        Properties serverProps=new Properties();
        try {
            serverProps.load(StartJsonServer.class.getResourceAsStream("/server.properties"));
            logger.info("Server properties set. {} ", serverProps);
        } catch (IOException e) {
            logger.error("Cannot find chatserver.properties "+e);
            logger.debug("Looking for file in "+(new File(".")).getAbsolutePath());
        }


        IRepositoryEvent repositoryEvent = new HibernateRepositoryEvent(serverProps);
        IRepositoryParticipant repositoryParticipant = new HibernateRepositoryParticipant(serverProps);
        IRepositoryResult repositoryResult = new HibernateRepositoryResult(serverProps);
        IRepositoryReferee repositoryReferee = new SQLRepositoryReferee(serverProps);
        IRepositoryDTO repositoryDTO = new SQLRepositoryDTO(serverProps);

        IPasswordService passwordService = new BcryptPasswordService(12);
        IAuthentificationService authenticationService = new AuthentificationService(repositoryReferee, passwordService);
        IParticipantService participantService = new ParticipantService(repositoryDTO);
        IResultService resultService = new ResultService(repositoryResult);

        //authenticationService.registerReferee("lala",new Event(UUID.fromString("611B905C-90A6-43B2-8316-7488EA08DFF2"),"alergat"),"anto","anto",null);

        int ServerPort=0;
        try {
            ServerPort = Integer.parseInt(serverProps.getProperty("server.port"));
        }catch (NumberFormatException nef){
            logger.error("Wrong  Port Number"+nef.getMessage());
        }
        logger.debug("Starting server on port: "+ServerPort);
        AbstractServer server = new JsonConcurrentServer(ServerPort,authenticationService,participantService,resultService);
        try {
            server.start();
            logger.debug("Server started ...");
        } catch (ServerException e) {
            logger.error("Error starting the server" + e.getMessage());
        }
    }

}
