package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.messaging.MessagingConfig;
import org.example.messaging.MessagingService;
import org.example.model.Event;
import org.example.model.Referee;
import org.example.networking.utils.AbstractServer;
import org.example.networking.utils.JsonConcurrentServer;
import org.example.networking.utils.ServerException;
import org.example.repository.*;
import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IPasswordService;
import org.example.service.IResultService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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

        AnnotationConfigApplicationContext context = null;
        MessagingService messagingService = null;
        try {
            logger.info("Initializing Spring context for messaging...");
            context = new AnnotationConfigApplicationContext();
            context.register(MessagingConfig.class);

            setSystemProperties(serverProps);

            context.refresh();
            messagingService = context.getBean(MessagingService.class);

            logger.info("Spring context initialized successfully");

        } catch (Exception e) {
            logger.error(" Failed to initialize Spring context: {}", e.getMessage(), e);
            logger.warn("⚠  Continuing without RabbitMQ support...");
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
        //authenticationService.registerReferee("TestUser",new Event(UUID.fromString("49991ac7-5e18-44d2-bd63-5a4758e69fb4"), "inot"),"test", "1234", null);
        for(Referee referee : repositoryReferee.findAll()){
            System.out.println(referee.toString());
        }
        if (messagingService != null) {
            ((AuthentificationService) authenticationService).setMessagingService(messagingService);
            ((ResultService) resultService).setMessagingService (messagingService);
            logger.info("MessagingService injected into services");
        }


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


        if (context != null) {
            final AnnotationConfigApplicationContext finalContext = context;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info(" Shutting down Spring context...");
                finalContext.close();
            }));
        }
        server.start();

    }


    private static void setSystemProperties(Properties props) {
        props.forEach((key, value) -> {
            if (key.toString().startsWith("rabbitmq.")) {
                System.setProperty(key.toString(), value.toString());
            }
        });
    }

}
