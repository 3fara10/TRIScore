package org.example.networking.utils;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.ParticipantResult;
import org.example.model.Referee;
import org.example.model.Result;
import org.example.networking.dto.DTOUtils;
import org.example.networking.jsonprotocol.*;
import org.example.service.IAuthentificationService;
import org.example.service.IObserver;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

public class ClientJsonWorker implements Runnable, IObserver {
    private IAuthentificationService authServer;
    private IResultService resultServer;
    private IParticipantService participantServer;

    private Socket connection;
    private BufferedReader input;
    private PrintWriter output;
    private Gson gsonFormatter;
    private volatile boolean connected;

    private static final Logger logger = LogManager.getLogger(ClientJsonWorker.class);

    public ClientJsonWorker(IAuthentificationService authService, IResultService resultService, IParticipantService participantService, Socket connection) {
        this.authServer = authService;
        this.resultServer = resultService;
        this.participantServer = participantService;
        this.connection = connection;
        this.gsonFormatter = new Gson();

        try {
            output = new PrintWriter(connection.getOutputStream());
            input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            connected = true;
        } catch (IOException e) {
            logger.error("Error creating client worker", e);
        }
    }

    @Override
    public void run() {
        while (connected) {
            try {
                String requestJson = input.readLine();
                if (requestJson == null || requestJson.isEmpty()) continue;

                logger.debug("Received json request {}", requestJson);
                Request request = gsonFormatter.fromJson(requestJson, Request.class);
                logger.debug("Deserialized Request {}", request);

                Response response = handleRequest(request);
                if (response != null) {
                    sendResponse(response);
                }
            } catch (IOException e) {
                logger.error("Run error {}", e.getMessage());
                if (e.getCause() != null) {
                    logger.error("Run inner error {}", e.getCause().getMessage());
                }
                logger.error(e);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }

        try {
            input.close();
            output.close();
            connection.close();
        } catch (IOException e) {
            logger.error("Error closing connection", e);
        }
    }

    private static final Response okResponse = JsonProtocolUtils.createOkResponse();

    private Response handleRequest(Request request) {
        Response response = null;

        if (request.getType() == RequestType.LOGIN) {
            logger.debug("Login request...");
            if (request.getReferee() == null) return JsonProtocolUtils.createErrorResponse("Invalid referee");

            Referee referee = DTOUtils.getFromDTO(request.getReferee());
            try {
                synchronized (authServer) {
                    Referee loggedReferee = authServer.login(referee.getUsername(), referee.getPassword(), this);
                    return JsonProtocolUtils.createLoginResponse(loggedReferee);
                }
            } catch (Exception e) {
                connected = false;
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        if (request.getType() == RequestType.LOGOUT) {
            logger.debug("Logout request");
            if (request.getReferee() == null)
                return JsonProtocolUtils.createErrorResponse("Invalid referee");

            try {
                synchronized (authServer) {
                    authServer.logout(request.getReferee().getId());
                }
                connected = false;
                return okResponse;
            } catch (Exception e) {
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        if (request.getType() == RequestType.REGISTER_REFEREE) {
            logger.debug("Register referee request");
            if (request.getReferee() == null)
                return JsonProtocolUtils.createErrorResponse("Invalid referee");

            try {
                Referee referee = DTOUtils.getFromDTO(request.getReferee());
                Event event = new Event(request.getReferee().getEventId(), request.getReferee().getEventName());

                synchronized (authServer) {
                    Referee newReferee = authServer.registerReferee(referee.getName(), event, referee.getUsername(),
                            referee.getPassword(), this);

                    return JsonProtocolUtils.createLoginResponse(newReferee);
                }
            } catch (Exception e) {
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        if (request.getType() == RequestType.ADD_RESULT) {
            logger.debug("Add result request");
            if (request.getResult() == null)
                return JsonProtocolUtils.createErrorResponse("Invalid result");

            try {
                synchronized (resultServer) {
                    Optional<Result> resultOption = resultServer.addResult(
                            request.getResult().getParticipantId(),
                            request.getResult().getEventId(),
                            request.getResult().getPoints());

                    return JsonProtocolUtils.createAddResultResponse(resultOption.orElse(null));
                }
            } catch (Exception e) {
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        if (request.getType() == RequestType.GET_ALL_PARTICIPANTS_SORTED) {
            logger.debug("Get all participants sorted request");
            try {
                synchronized (participantServer) {
                    ParticipantResult[] participantsWithResults = StreamSupport
                            .stream(participantServer.getAllParticipantsSortedByNameWithTotalPoints().spliterator(), false)
                            .toArray(ParticipantResult[]::new);
                    return JsonProtocolUtils.createParticipantResultsResponse(participantsWithResults);
                }
            } catch (Exception e) {
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        if (request.getType() == RequestType.GET_PARTICIPANTS_WITH_RESULTS_FOR_EVENT) {
            logger.debug("Get participants with results request");
            try {
                UUID eventId = request.getEventId();
                if (eventId.equals(UUID.randomUUID()) && request.getParticipant() != null) {
                    eventId = request.getParticipant().getId();
                }

                synchronized (participantServer) {
                    ParticipantResult[] participantsWithResults = StreamSupport
                            .stream(participantServer.getParticipantsWithResultsForEvent(eventId).spliterator(), false)
                            .toArray(ParticipantResult[]::new);
                    return JsonProtocolUtils.createParticipantResultsResponse(participantsWithResults);
                }
            } catch (Exception e) {
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        if (request.getType() == RequestType.REGISTER_OBSERVER) {
            logger.debug("Register observer request");
            if (request.getReferee() == null)
                return JsonProtocolUtils.createErrorResponse("Invalid referee");

            try {
                synchronized (authServer) {
                    authServer.registerObserver(this, request.getReferee().getId());
                    return okResponse;
                }
            } catch (Exception e) {
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        if (request.getType() == RequestType.UNREGISTER_OBSERVER) {
            logger.debug("Unregister observer request");
            if (request.getReferee() == null)
                return JsonProtocolUtils.createErrorResponse("Invalid referee");

            try {
                synchronized (authServer) {
                    authServer.unregisterObserver(request.getReferee().getId());
                    return okResponse;
                }
            } catch (Exception e) {
                return JsonProtocolUtils.createErrorResponse(e.getMessage());
            }
        }

        return response;
    }

    private void sendResponse(Response response) throws IOException {
        String jsonString = gsonFormatter.toJson(response);
        logger.debug("sending response {}", jsonString);
        synchronized (output) {
            output.println(jsonString);
            output.flush();
        }
    }

    @Override
    public void update() {
        try {
            Response updateResponse = new Response();
            updateResponse.setType(ResponseType.UPDATE);

            sendResponse(updateResponse);
            logger.debug("Sent update notification to client");
        } catch (Exception e) {
            logger.error("Update failed", e);
        }
    }
}