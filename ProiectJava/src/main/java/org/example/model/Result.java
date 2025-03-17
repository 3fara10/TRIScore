package org.example.model;

import java.util.Objects;

public class Result extends Entity<Long>{
    private Event event;
    private Participant participant;
    private int points;

    public Result(){
        super(null);
    }
    public Result(Long id, Event event, Participant participant, int points) {
        super(id);
        this.event = event;
        this.participant = participant;
        this.points = points;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
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
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Result result = (Result) o;
        return points == result.points && Objects.equals(event, result.event) && Objects.equals(participant, result.participant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), event, participant, points);
    }

    @Override
    public String toString() {
        return "Result{" +
                super.toString() +
                ", event=" + event +
                ", participant=" + participant +
                ", points=" + points +
                '}';
    }
}