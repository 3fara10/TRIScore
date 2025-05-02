package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.networking.jsonprotocol.IServerJsonProxy;
import org.example.networking.jsonprotocol.ServerJsonProxy;
import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class StartJsonClient {

    public static void main(String[] args) {
        Logger logger = LogManager.getLogger(StartJsonClient.class);
        Properties clientProps=new Properties();
        try {
            clientProps.load(StartJsonClient.class.getResourceAsStream("/client.properties"));
            logger.info("Client properties set. ");
        } catch (IOException e) {
            logger.error("Cannot find client.properties "+e);
            logger.debug("Looking for file in "+(new File(".")).getAbsolutePath());
        }

        String serverIP=clientProps.getProperty("server.host");
        int serverPort=0;
        try{
            serverPort=Integer.parseInt(clientProps.getProperty("server.port"));
        }catch(NumberFormatException ex){
            logger.error("Wrong port number "+ex.getMessage());
        }
        logger.info("Using server IP "+serverIP);
        logger.info("Using server port "+serverPort);
        IServerJsonProxy sharedProxy = new ServerJsonProxy(serverIP, serverPort);
        IAuthentificationService authenticationServer = sharedProxy;
        IParticipantService participantServer = sharedProxy;
        IResultService resultService = sharedProxy;

        ClientCtrl ctrl=new ClientCtrl(authenticationServer,participantServer,resultService);
        LoginWindow logWin=new LoginWindow(ctrl);
        logWin.setSize(400,400);
        logWin.setLocation(150,150);
        logWin.setVisible(true);

    }
}
