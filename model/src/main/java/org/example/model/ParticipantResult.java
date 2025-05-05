package org.example.model;

import java.util.Objects;
import java.util.UUID;

public class ParticipantResult extends Entity<UUID>
{
    private String ParticipantName;
    private UUID ParticipantID;
    private int points;

    public ParticipantResult(UUID id, String participantName, int points)
    {
        super(id);
        ParticipantID=id;
        ParticipantName = participantName;
        this.points = points;
    }

    public ParticipantResult()
    {
        super(null);
    }

    public String getParticipantName() {
        return ParticipantName;
    }

    public void setParticipantName(String participantName) {
        ParticipantName = participantName;
    }

    public UUID getParticipantID() {
        return ParticipantID;
    }

    public void setParticipantID(UUID participantID) {
        ParticipantID = participantID;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticipantResult that)) return false;
        if (!super.equals(o)) return false;
        return points == that.points && Objects.equals(ParticipantName, that.ParticipantName) && Objects.equals(ParticipantID, that.ParticipantID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ParticipantName, ParticipantID, points);
    }
}