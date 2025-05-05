package org.example.networking.grpcprotocol;


import org.example.model.Referee;
import org.example.networking.grpcprotocol.generated.Result;

import java.util.UUID;

/**
 * Utility class for working with gRPC protocol.
 * Contains helper methods for converting between model and gRPC objects.
 */
public class GrpcProtocolUtils {

    /**
     * Convert a Java UUID to a gRPC UUID.
     */
    public static org.example.networking.grpcprotocol.generated.UUID toGrpcUuid(UUID id) {
        return org.example.networking.grpcprotocol.generated.UUID.newBuilder()
                .setValue(id.toString())
                .build();
    }

    /**
     * Convert a gRPC UUID to a Java UUID.
     */
    public static UUID fromGrpcUuid(org.example.networking.grpcprotocol.generated.UUID uuid) {
        return UUID.fromString(uuid.getValue());
    }

    /**
     * Convert a model Referee to a gRPC Referee.
     */
    public static org.example.networking.grpcprotocol.generated.Referee toGrpcReferee(Referee referee) {
        return org.example.networking.grpcprotocol.generated.Referee.newBuilder()
                .setId(toGrpcUuid(referee.getId()))
                .setName(referee.getName())
                .setEventId(toGrpcUuid(referee.getEvent().getId()))
                .setEventName(referee.getEvent().getName())
                .setUsername(referee.getUsername())
                .setPassword("") // Don't send the password hash back
                .build();
    }

    /**
     * Convert a gRPC Referee to a model Referee.
     */
    public static org.example.model.Referee fromGrpcReferee(org.example.networking.grpcprotocol.generated.Referee grpcReferee) {
        return new org.example.model.Referee(
                fromGrpcUuid(grpcReferee.getId()),
                grpcReferee.getName(),
                new org.example.model.Event(
                        fromGrpcUuid(grpcReferee.getEventId()),
                        grpcReferee.getEventName()
                ),
                grpcReferee.getUsername(),
                grpcReferee.getPassword()
        );
    }

    /**
     * Convert a model Result to a gRPC Result.
     */
    public static Result toGrpcResult(org.example.model.Result result) {
        return org.example.networking.grpcprotocol.generated.Result.newBuilder()
                .setId(toGrpcUuid(result.getId()))
                .setParticipantId(toGrpcUuid(result.getParticipant().getId()))
                .setParticipantName(result.getParticipant().getName())
                .setEventId(toGrpcUuid(result.getEvent().getId()))
                .setEventName(result.getEvent().getName())
                .setPoints(result.getPoints())
                .build();
    }

    /**
     * Convert a gRPC Result to a model Result.
     */
    public static org.example.model.Result fromGrpcResult(org.example.networking.grpcprotocol.generated.Result grpcResult) {
        return new org.example.model.Result(
                fromGrpcUuid(grpcResult.getId()),
                new org.example.model.Event(
                        fromGrpcUuid(grpcResult.getEventId()),
                        grpcResult.getEventName()
                ),
                new org.example.model.Participant(
                        fromGrpcUuid(grpcResult.getParticipantId())
                ),
                grpcResult.getPoints()
        );
    }

    /**
     * Convert a model ParticipantResult to a gRPC ParticipantResult.
     */
    public static org.example.networking.grpcprotocol.generated.ParticipantResult toGrpcParticipantResult(org.example.model.ParticipantResult participantResult) {
        org.example.networking.grpcprotocol.generated.ParticipantResult.Builder builder = org.example.networking.grpcprotocol.generated.ParticipantResult.newBuilder()
                .setId(toGrpcUuid(participantResult.getId()))
                .setParticipantId(toGrpcUuid(participantResult.getParticipantID()))
                .setName(participantResult.getParticipantName())
                .setPoints(participantResult.getPoints());

        return builder.build();
    }

    /**
     * Convert a gRPC ParticipantResult to a model ParticipantResult.
     */
    public static org.example.model.ParticipantResult fromGrpcParticipantResult(org.example.networking.grpcprotocol.generated.ParticipantResult grpcParticipantResult) {
        org.example.model.ParticipantResult result = new org.example.model.ParticipantResult(
                fromGrpcUuid(grpcParticipantResult.getId()),
                grpcParticipantResult.getName(),
                grpcParticipantResult.getPoints()
        );

        result.setParticipantID(fromGrpcUuid(grpcParticipantResult.getParticipantId()));

        return result;
    }

    /**
     * Create a login request.
     */
    public static org.example.networking.grpcprotocol.generated.LoginRequest createLoginRequest(String username, String password) {
        return org.example.networking.grpcprotocol.generated.LoginRequest.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build();
    }

    /**
     * Create a logout request.
     */
    public static org.example.networking.grpcprotocol.generated.LogoutRequest createLogoutRequest(UUID refereeId) {
        return org.example.networking.grpcprotocol.generated.LogoutRequest.newBuilder()
                .setRefereeId(toGrpcUuid(refereeId))
                .build();
    }

    /**
     * Create an OK login response.
     */
    public static org.example.networking.grpcprotocol.generated.LoginResponse createOkLoginResponse(org.example.model.Referee referee) {
        return org.example.networking.grpcprotocol.generated.LoginResponse.newBuilder()
                .setType(org.example.networking.grpcprotocol.generated.LoginResponse.ResponseType.OK)
                .setReferee(toGrpcReferee(referee))
                .build();
    }

    /**
     * Create an error login response.
     */
    public static org.example.networking.grpcprotocol.generated.LoginResponse createErrorLoginResponse(String errorMessage) {
        return org.example.networking.grpcprotocol.generated.LoginResponse.newBuilder()
                .setType(org.example.networking.grpcprotocol.generated.LoginResponse.ResponseType.ERROR)
                .setError(errorMessage)
                .build();
    }

    /**
     * Create an OK logout response.
     */
    public static org.example.networking.grpcprotocol.generated.LogoutResponse createOkLogoutResponse() {
        return org.example.networking.grpcprotocol.generated.LogoutResponse.newBuilder()
                .setType(org.example.networking.grpcprotocol.generated.LogoutResponse.ResponseType.OK)
                .build();
    }

    /**
     * Create an error logout response.
     */
    public static org.example.networking.grpcprotocol.generated.LogoutResponse createErrorLogoutResponse(String errorMessage) {
        return org.example.networking.grpcprotocol.generated.LogoutResponse.newBuilder()
                .setType(org.example.networking.grpcprotocol.generated.LogoutResponse.ResponseType.ERROR)
                .setError(errorMessage)
                .build();
    }
}