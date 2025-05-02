package org.example.networking.dto;

import java.io.Serializable;
import java.util.UUID;

public class ResultDTO implements Serializable {
    private UUID id;
    private UUID eventId;
    private String eventName;
    private UUID participantId;
    private String participantName;
    private int points;

    public ResultDTO() {
    }

    public ResultDTO(UUID id, UUID eventId, String eventName, UUID participantId, String participantName, int points) {
        this.id = id;
        this.eventId = eventId;
        this.eventName = eventName;
        this.participantId = participantId;
        this.participantName = participantName;
        this.points = points;
    }

    public ResultDTO(UUID id) {
        this(id, UUID.randomUUID(), "", UUID.randomUUID(), "", 0);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
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