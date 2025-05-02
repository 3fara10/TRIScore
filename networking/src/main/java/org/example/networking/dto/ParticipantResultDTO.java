package org.example.networking.dto;

import java.io.Serializable;
import java.util.UUID;

public class ParticipantResultDTO implements Serializable {
    private UUID id;
    private UUID participantId;
    private String participantName;
    private int points;

    public ParticipantResultDTO() {
    }

    public ParticipantResultDTO(UUID id, UUID participantId, String participantName, int points) {
        this.id = id;
        this.participantId = participantId;
        this.participantName = participantName;
        this.points = points;
    }

    public ParticipantResultDTO(UUID id) {
        this(id, null, "", 0);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}