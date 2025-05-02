package org.example.networking.jsonprotocol;

import org.example.model.ParticipantResult;
import org.example.model.Referee;
import org.example.model.Result;
import org.example.networking.dto.DTOUtils;
import org.example.networking.dto.ParticipantResultDTO;

import java.util.Arrays;
import java.util.UUID;

/**
 * Utility class for creating standardized JSON protocol messages.
 * Contains factory methods for creating different types of requests and responses.
 */
public class JsonProtocolUtils {

    /**
     * Creates a standard OK response.
     *
     * @return A response with type OK
     */
    public static Response createOkResponse() {
        Response response = new Response();
        response.setType(ResponseType.OK);
        return response;
    }

    /**
     * Creates an error response with the specified error message.
     *
     * @param errorMessage The error message to include
     * @return A response with type ERROR and the specified error message
     */
    public static Response createErrorResponse(String errorMessage) {
        Response response = new Response();
        response.setType(ResponseType.ERROR);
        response.setErrorMessage(errorMessage);
        return response;
    }

    /**
     * Creates a login response with the specified referee information.
     *
     * @param referee The referee who has logged in
     * @return A response containing the referee's DTO
     */
    public static Response createLoginResponse(Referee referee) {
        Response response = new Response();
        response.setType(ResponseType.OK);
        response.setReferee(DTOUtils.getDTO(referee));
        return response;
    }

    /**
     * Creates a response for a successful result addition.
     *
     * @param result The result that was added
     * @return A response containing the result's DTO
     */
    public static Response createAddResultResponse(Result result) {
        Response response = new Response();
        response.setType(ResponseType.RESULT_ADDED);
        response.setResult(DTOUtils.getDTO(result));
        return response;
    }

    /**
     * Creates a response containing participant results.
     *
     * @param participantResults The array of participant results
     * @return A response containing DTOs for all participant results
     */
    public static Response createParticipantResultsResponse(ParticipantResult[] participantResults) {
        Response response = new Response();
        response.setType(ResponseType.OK);
        response.setParticipantResults(
                Arrays.stream(participantResults)
                        .map(DTOUtils::getDTO)
                        .toArray(ParticipantResultDTO[]::new)
        );
        return response;
    }

    /**
     * Creates a login request for the specified referee.
     *
     * @param referee The referee trying to log in
     * @return A request with type LOGIN and the referee's information
     */
    public static Request createLoginRequest(Referee referee) {
        Request request = new Request();
        request.setType(RequestType.LOGIN);
        request.setReferee(DTOUtils.getDTO(referee));
        return request;
    }

    /**
     * Creates a logout request for the specified referee.
     *
     * @param referee The referee trying to log out
     * @return A request with type LOGOUT and the referee's information
     */
    public static Request createLogoutRequest(Referee referee) {
        Request request = new Request();
        request.setType(RequestType.LOGOUT);
        request.setReferee(DTOUtils.getDTO(referee));
        return request;
    }

    /**
     * Creates a request to add a new result.
     *
     * @param result The result to add
     * @return A request with type ADD_RESULT and the result information
     */
    public static Request createAddResultRequest(Result result) {
        Request request = new Request();
        request.setType(RequestType.ADD_RESULT);
        request.setResult(DTOUtils.getDTO(result));
        return request;
    }

    /**
     * Creates a request to get all participants sorted by name.
     *
     * @return A request with type GET_ALL_PARTICIPANTS_SORTED
     */
    public static Request createGetAllParticipantsSortedByNameRequest() {
        Request request = new Request();
        request.setType(RequestType.GET_ALL_PARTICIPANTS_SORTED);
        return request;
    }

    /**
     * Creates a request to get participants with results for a specific event.
     *
     * @param eventId The ID of the event
     * @return A request with type GET_PARTICIPANTS_WITH_RESULTS_FOR_EVENT and the event ID
     */
    public static Request createGetParticipantsWithResultsForEventRequest(UUID eventId) {
        Request request = new Request();
        request.setType(RequestType.GET_PARTICIPANTS_WITH_RESULTS_FOR_EVENT);
        request.setEventId(eventId);
        return request;
    }

    /**
     * Creates a request to register an observer for a referee.
     *
     * @param refereeId The ID of the referee
     * @return A request with type REGISTER_OBSERVER and the referee ID
     */
    public static Request createRegisterObserverRequest(UUID refereeId) {
        Request request = new Request();
        request.setType(RequestType.REGISTER_OBSERVER);
        request.setRefereeId(refereeId);
        return request;
    }

    /**
     * Creates a request to unregister an observer for a referee.
     *
     * @param refereeId The ID of the referee
     * @return A request with type UNREGISTER_OBSERVER and the referee ID
     */
    public static Request createUnregisterObserverRequest(UUID refereeId) {
        Request request = new Request();
        request.setType(RequestType.UNREGISTER_OBSERVER);
        request.setRefereeId(refereeId);
        return request;
    }
}