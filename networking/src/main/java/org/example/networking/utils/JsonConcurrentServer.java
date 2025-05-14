package org.example.networking.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

import java.net.Socket;

public class JsonConcurrentServer extends AbsConcurrentServer {
    private IAuthentificationService authenticationService;
    private IResultService resultService;
    private IParticipantService participantService;
    private ClientJsonWorker worker;

    private static Logger logger = LogManager.getLogger(JsonConcurrentServer.class);

    public JsonConcurrentServer(int port, IAuthentificationService authenticationService, IParticipantService participantService, IResultService resultService) {
        super(port);
        this.authenticationService = authenticationService;
        this.resultService = resultService;
        this.participantService = participantService;
        logger.debug("Creating JsonServer...");
    }

    @Override
    protected Thread createWorker(Socket client) {
       worker=new ClientJsonWorker(authenticationService,resultService,participantService, client);
       return new Thread(worker);
    }
}
