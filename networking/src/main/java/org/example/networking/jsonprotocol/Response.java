package org.example.networking.jsonprotocol;

import org.example.networking.dto.ParticipantDTO;
import org.example.networking.dto.ParticipantResultDTO;
import org.example.networking.dto.RefereeDTO;
import org.example.networking.dto.ResultDTO;

import java.io.Serializable;

public class Response implements Serializable {
    private ResponseType type;
    private String errorMessage;
    private RefereeDTO referee;
    private ResultDTO result;
    private ParticipantDTO[] participants;
    private ParticipantResultDTO[] participantResults;
    private int totalPoints;

    // Default constructor required for JSON serialization
    public Response() {
    }

    // Getters and setters
    public ResponseType getType() {
        return type;
    }

    public void setType(ResponseType type) {
        this.type = type;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public RefereeDTO getReferee() {
        return referee;
    }

    public void setReferee(RefereeDTO referee) {
        this.referee = referee;
    }

    public ResultDTO getResult() {
        return result;
    }

    public void setResult(ResultDTO result) {
        this.result = result;
    }

    public ParticipantDTO[] getParticipants() {
        return participants;
    }

    public void setParticipants(ParticipantDTO[] participants) {
        this.participants = participants;
    }

    public ParticipantResultDTO[] getParticipantResults() {
        return participantResults;
    }

    public void setParticipantResults(ParticipantResultDTO[] participantResults) {
        this.participantResults = participantResults;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    @Override
    public String toString() {
        return String.format("Response[type=%s, error=%s, referee=%s, result=%s, participants=%d, participantResults=%d, totalPoints=%d]",
                type,
                errorMessage != null ? errorMessage : "null",
                referee != null ? referee.toString() : "null",
                result != null ? result.toString() : "null",
                participants != null ? participants.length : 0,
                participantResults != null ? participantResults.length : 0,
                totalPoints);
    }
}