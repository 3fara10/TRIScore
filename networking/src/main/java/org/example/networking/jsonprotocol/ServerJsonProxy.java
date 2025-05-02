package org.example.networking.jsonprotocol;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.*;
import org.example.networking.dto.DTOUtils;
import org.example.service.IObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JSON-based proxy implementation for remote service calls.
 * This class handles the communication with the server through JSON protocol.
 */
public class ServerJsonProxy implements IServerJsonProxy {
    private final String host;
    private final int port;
    private IObserver client;
    private BufferedReader input;
    private PrintWriter output;
    private Socket connection;
    private final BlockingQueue<Response> responses;
    private volatile boolean finished;
    private CountDownLatch responseLatch;
    private Thread readerThread;

    private static final Logger logger = LogManager.getLogger(ServerJsonProxy.class);
    private Referee referee;
    private final Gson gsonFormatter;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new proxy with the specified host and port.
     *
     * @param host The server host
     * @param port The server port
     */
    public ServerJsonProxy(String host, int port) {
        this.host = host;
        this.port = port;
        this.responses = new LinkedBlockingQueue<>();
        this.gsonFormatter = new Gson();
    }

    /**
     * Logs in a referee to the system.
     *
     * @param username The referee's username
     * @param password The referee's password
     * @param observer The observer to be notified of updates
     * @return The logged in referee
     */
    @Override
    public Referee login(String username, String password, IObserver observer) throws Exception {
        initializeConnection();
        Referee referee = new Referee(username, password);
        sendRequest(JsonProtocolUtils.createLoginRequest(referee));
        Response response = readResponse();

        if (response.getType() == ResponseType.OK) {
            this.client = observer;
            this.referee = new Referee(
                    response.getReferee().getId(),
                    response.getReferee().getName(),
                    new Event(response.getReferee().getEventId(), response.getReferee().getEventName()),
                    response.getReferee().getUsername(),
                    response.getReferee().getPassword()
            );
            logger.debug("Stored current referee: {}, Event: {}",
                    this.referee.getName(), this.referee.getEvent().getName());
            return this.referee;
        }

        if (response.getType() == ResponseType.ERROR) {
            String err = response.getErrorMessage();
            closeConnection();
            throw new Exception(err);
        }

        throw new Exception("Unknown response type: " + response.getType());
    }

    /**
     * Logs out a referee from the system.
     *
     * @param refereeId The ID of the referee to log out
     */
    @Override
    public void logout(UUID refereeId) throws Exception {
        try {
            if (this.referee == null) {
                logger.warn("Attempted logout with no active referee");
                return;
            }

            sendRequest(JsonProtocolUtils.createLogoutRequest(referee));
            Response response = readResponse();
            closeConnection();
            referee = null;
        } catch (Exception ex) {
            logger.error("Error during logout", ex);
            referee = null;
            throw ex;
        }
    }

    /**
     * Adds a new result to the system.
     *
     * @param participantId The participant ID
     * @param eventId The event ID
     * @param points The points scored
     * @return An Optional containing the result if successful
     */
    @Override
    public Optional<Result> addResult(UUID participantId, UUID eventId, int points) throws Exception {
        try {
            if (input == null || output == null || connection == null || !connection.isConnected()) {
                throw new IllegalStateException("Not logged in. Please login first.");
            }

            Result result = new Result(new Event(eventId), new Participant(participantId), points);
            sendRequest(JsonProtocolUtils.createAddResultRequest(result));
            Response response = readResponse();

            if (response.getType() == ResponseType.RESULT_ADDED) {
                if (response.getResult() != null) {
                    return Optional.of(DTOUtils.getFromDTO(response.getResult()));
                }

                return Optional.of(new Result(
                        UUID.randomUUID(),
                        new Event(eventId, ""),
                        new Participant(participantId, ""),
                        points)
                );
            }

            throw new Exception(response.getErrorMessage() != null ?
                    response.getErrorMessage() : "Unknown error adding result");
        } catch (Exception ex) {
            logger.error("Error in addResult: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Gets all participants sorted by name with their total points.
     *
     * @return An Iterable of ParticipantResult objects
     * @throws Exception If retrieving the participants fails
     */
    @Override
    public Iterable<ParticipantResult> getAllParticipantsSortedByNameWithTotalPoints() throws Exception {
        try {
            if (input == null || output == null || connection == null || !connection.isConnected()) {
                throw new IllegalStateException("Not logged in. Please login first.");
            }

            sendRequest(JsonProtocolUtils.createGetAllParticipantsSortedByNameRequest());
            Response response = readResponse();
            logger.debug("getAllParticipantsSortedByName response: Type={}, Participants count={}",
                    response.getType(),
                    (response.getParticipants() != null ? response.getParticipants().length : 0));

            if (response.getType() == ResponseType.OK || response.getType() == ResponseType.DATA_FOUND) {
                if (response.getParticipants() != null) {
                    List<ParticipantResult> results = new ArrayList<>();
                    for (var p : response.getParticipants()) {
                        ParticipantResult pr = new ParticipantResult(p.getId(), p.getName(), 0);
                        pr.setParticipantID(p.getId());
                        results.add(pr);
                    }
                    return results;
                }

                if (response.getParticipantResults() != null) {
                    List<ParticipantResult> results = new ArrayList<>();
                    for (var pr : response.getParticipantResults()) {
                        results.add(DTOUtils.getFromDTO(pr));
                    }
                    return results;
                }
            }

            if (response.getType() == ResponseType.ERROR) {
                throw new Exception(response.getErrorMessage());
            }

            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error in getAllParticipantsSortedByName", e);
            throw e;
        }
    }

    /**
     * Gets participants with results for a specific event.
     *
     * @param eventId The event ID
     * @return An Iterable of ParticipantResult objects
     * @throws Exception If retrieving the participants fails
     */
    @Override
    public Iterable<ParticipantResult> getParticipantsWithResultsForEvent(UUID eventId) throws Exception {
        try {
            if (eventId.equals(UUID.randomUUID())) {
                logger.error("getParticipantsWithResultsForEvent called with empty event ID");

                if (this.referee != null && this.referee.getEvent() != null) {
                    eventId = this.referee.getEvent().getId();
                    logger.debug("Using current referee's event ID: {}", eventId);
                } else {
                    throw new IllegalArgumentException("No valid event ID provided");
                }
            }

            if (input == null || output == null || connection == null || !connection.isConnected()) {
                throw new IllegalStateException("Not logged in. Please login first.");
            }

            sendRequest(JsonProtocolUtils.createGetParticipantsWithResultsForEventRequest(eventId));
            Response response = readResponse();

            logger.debug("getParticipantsWithResultsForEvent response: Type={}, ParticipantResults count={}",
                    response.getType(),
                    (response.getParticipantResults() != null ? response.getParticipantResults().length : 0));

            if (response.getType() == ResponseType.ERROR) {
                throw new Exception(response.getErrorMessage());
            }

            if ((response.getType() == ResponseType.OK || response.getType() == ResponseType.DATA_FOUND)
                    && response.getParticipantResults() != null) {
                List<ParticipantResult> results = new ArrayList<>();
                for (var pr : response.getParticipantResults()) {
                    results.add(DTOUtils.getFromDTO(pr));
                }
                return results;
            }

            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error in getParticipantsWithResultsForEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Registers an observer for updates.
     *
     * @param observer The observer to register
     * @param refereeId The referee ID to associate with the observer
     * @throws Exception If registration fails
     */
    @Override
    public void registerObserver(IObserver observer, UUID refereeId) throws Exception {
        sendRequest(JsonProtocolUtils.createRegisterObserverRequest(refereeId));
        Response response = readResponse();

        if (response.getType() == ResponseType.ERROR) {
            throw new Exception(response.getErrorMessage());
        }
    }

    /**
     * Unregisters an observer.
     *
     * @param refereeId The referee ID associated with the observer
     * @throws Exception If unregistration fails
     */
    @Override
    public void unregisterObserver(UUID refereeId) throws Exception {
        sendRequest(JsonProtocolUtils.createUnregisterObserverRequest(refereeId));
        Response response = readResponse();

        if (response.getType() == ResponseType.ERROR) {
            throw new Exception(response.getErrorMessage());
        }
    }

    /**
     * Registers a new referee in the system.
     *
     * @param name The referee's name
     * @param evt The event to associate with the referee
     * @param username The referee's username
     * @param password The referee's password
     * @param observer The observer to be notified of updates
     * @return The registered referee
     * @throws Exception If registration fails
     */
    @Override
    public Referee registerReferee(String name, Event evt, String username, String password, IObserver observer) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Handles update notifications from the server.
     *
     * @param response The response containing the update
     */
    private void handleUpdate(Response response) {
        logger.debug("handleUpdate called with {}", response);

        if (client == null) {
            logger.warn("No client registered for updates");
            return;
        }

        if (response.getType() == ResponseType.UPDATE ||
                response.getType() == ResponseType.RESULT_ADDED ||
                response.getType() == ResponseType.OK ||
                response.getType() == ResponseType.DATA_FOUND) {

            logger.debug("Processing update notification of type: {}", response.getType());
            try {
                client.update();
            } catch (Exception ex) {
                logger.error("Error processing update for type {}", response.getType(), ex);
            }
        } else if (response.getType() == ResponseType.OBSERVER_REGISTERED) {
            logger.debug("Observer registered notification");
            try {
                client.update();
            } catch (Exception ex) {
                logger.error("Error processing observer registered update", ex);
            }
        } else if (response.getType() == ResponseType.UNREGISTER_OBSERVER_REGISTERED) {
            logger.debug("Unregister observer notification");
            try {
                client.update();
            } catch (Exception ex) {
                logger.error("Error processing unregister observer update", ex);
            }
        }
    }

    /**
     * Determines if a response is an update notification.
     *
     * @param response The response to check
     * @return true if the response is an update notification, false otherwise
     */
    private boolean isUpdate(Response response) {
        return response != null && (
                response.getType() == ResponseType.UPDATE ||
                        response.getType() == ResponseType.OBSERVER_REGISTERED ||
                        response.getType() == ResponseType.UNREGISTER_OBSERVER_REGISTERED ||
                        response.getType() == ResponseType.DATA_FOUND);
    }

    /**
     * Closes the connection to the server.
     */
    private void closeConnection() {
        try {
            finished = true;

            if (input != null) {
                input.close();
                input = null;
            }

            if (output != null) {
                output.close();
                output = null;
            }

            if (connection != null) {
                connection.close();
                connection = null;
            }

            client = null;
        } catch (Exception e) {
            logger.error("Error during connection closure", e);
        }
    }

    /**
     * Initializes the connection to the server.
     *
     * @throws Exception If connection initialization fails
     */
    private void initializeConnection() throws Exception {
        try {
            connection = new Socket(host, port);
            output = new PrintWriter(connection.getOutputStream());
            input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            finished = false;
            startReader();
        } catch (Exception e) {
            logger.error("Error initializing connection", e);
            throw e;
        }
    }

    /**
     * Reads a response from the server.
     *
     * @return The response from the server
     * @throws Exception If reading the response fails
     */
    private Response readResponse() throws Exception {
        Response response = null;
        try {
            do {
                responseLatch = new CountDownLatch(1);
                responseLatch.await();

                synchronized (responses) {
                    if (!responses.isEmpty()) {
                        response = responses.poll();
                    }
                }
            } while (response != null && isUpdate(response));
        } catch (Exception e) {
            logger.error("Error reading response", e);
            throw e;
        }

        return response;
    }

    /**
     * Starts the reader thread for receiving responses from the server.
     */
    private void startReader() {
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
            try {
                readerThread.join();
            } catch (InterruptedException e) {
                logger.error("Error joining reader thread", e);
            }
        }

        this.readerThread = new Thread(this::run);
        this.readerThread.start();
    }

    /**
     * Sends a request to the server.
     *
     * @param request The request to send
     * @throws Exception If sending the request fails
     */
    private void sendRequest(Request request) throws Exception {
        try {
            lock.lock();
            try {
                String jsonRequest = gsonFormatter.toJson(request);
                logger.debug("Sending request {}", jsonRequest);
                output.println(jsonRequest);
                output.flush();
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            throw new Exception("Error sending object " + e);
        }
    }

    /**
     * Reader thread run method that processes incoming responses from the server.
     */
    private void run() {
        while (!finished) {
            if (connection == null || !connection.isConnected()) {
                logger.debug("Socket is disposed or disconnected. Stopping communication.");
                finished = true;
                break;
            }

            try {
                logger.debug("Waiting for response from server...");
                String responseJson = input.readLine();
                if (responseJson == null || responseJson.isEmpty()) {
                    logger.debug("Received empty response, continuing...");
                    continue;
                }

                logger.debug("Raw response received: {}", responseJson);
                Response response = gsonFormatter.fromJson(responseJson, Response.class);
                logger.debug("Deserialized response: {}", response);

                if (isUpdate(response)) {
                    logger.debug("Processing as update notification");
                    handleUpdate(response);
                } else {
                    logger.debug("Adding response to queue (current count: {})", responses.size());
                    synchronized (responses) {
                        responses.add(response);
                        logger.debug("New queue count: {}", responses.size());
                    }

                    logger.debug("Signaling response latch");
                    responseLatch.countDown();
                    logger.debug("Response latch signaled");
                }
            } catch (IOException e) {
                logger.debug("Connection closed: {}", e.getMessage());
                finished = true;
            } catch (Exception e) {
                finished = true;
                logger.error("Error in run method: {}", e.getMessage());
                logger.error("Stack trace: {}", e.getStackTrace());
                try {
                    responseLatch.countDown();
                } catch (Exception ex) {
                    logger.error("Error counting down response latch: {}", ex.getMessage());
                }
            }
        }
    }

    /**
     * Closes the connection and resources when the proxy is closed.
     */
    @Override
    public void close() throws Exception {
        try {
            closeConnection();
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.interrupt();
                readerThread.join();
            }

            client = null;
            referee = null;

            if (responses != null) {
                responses.clear();
            }

            logger.debug("ServerJsonProxy successfully closed");
        } catch (Exception ex) {
            logger.error("Error during close: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}