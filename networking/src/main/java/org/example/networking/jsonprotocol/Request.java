package org.example.networking.jsonprotocol;

import org.example.networking.dto.ParticipantDTO;
import org.example.networking.dto.ParticipantResultDTO;
import org.example.networking.dto.RefereeDTO;
import org.example.networking.dto.ResultDTO;

import java.io.Serializable;
import java.util.UUID;

public class Request implements Serializable {
    private RequestType type;
    private RefereeDTO referee;
    private ParticipantDTO participant;
    private ResultDTO result;
    private ParticipantResultDTO participantResult;
    private UUID refereeId;
    private UUID eventId;
    private UUID participantId;

    // Default constructor required for JSON serialization
    public Request() {
    }

    // Getters and setters
    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public RefereeDTO getReferee() {
        return referee;
    }

    public void setReferee(RefereeDTO referee) {
        this.referee = referee;
    }

    public ParticipantDTO getParticipant() {
        return participant;
    }

    public void setParticipant(ParticipantDTO participant) {
        this.participant = participant;
    }

    public ResultDTO getResult() {
        return result;
    }

    public void setResult(ResultDTO result) {
        this.result = result;
    }

    public ParticipantResultDTO getParticipantResult() {
        return participantResult;
    }

    public void setParticipantResult(ParticipantResultDTO participantResult) {
        this.participantResult = participantResult;
    }

    public UUID getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(UUID refereeId) {
        this.refereeId = refereeId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }

    @Override
    public String toString() {
        return String.format("Request[type=%s, referee=%s, participant=%s, result=%s, participantResult=%s, refereeId=%s, eventId=%s, participantId=%s]",
                type,
                referee,
                participant,
                result,
                participantResult,
                refereeId,
                eventId,
                participantId);
    }
}