package org.example.model;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

public class ParticipantResult extends Entity<UUID> {

    private String participantName;

    private UUID participantID;

    private int points;

    private Event event;

    public ParticipantResult(UUID id, String participantName, int points) {
        super(id);
        this.participantID = id;
        this.participantName = participantName;
        this.points = points;
    }

    public ParticipantResult(UUID id, String participantName, int points, Event event) {
        super(id);
        this.participantID = id;
        this.participantName = participantName;
        this.points = points;
        this.event = event;
    }

    public ParticipantResult() {
        super(UUID.randomUUID());
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public UUID getParticipantID() {
        return participantID;
    }

    public void setParticipantID(UUID participantID) {
        this.participantID = participantID;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticipantResult that)) return false;
        if (!super.equals(o)) return false;
        return points == that.points &&
                Objects.equals(participantName, that.participantName) &&
                Objects.equals(participantID, that.participantID) &&
                Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), participantName, participantID, points, event);
    }

    @Override
    public String toString() {
        return "ParticipantResult{" +
                "id=" + getId() +
                ", participantName='" + participantName + '\'' +
                ", participantID=" + participantID +
                ", points=" + points +
                ", event=" + (event != null ? event.getId() : "null") +
                '}';
    }
}