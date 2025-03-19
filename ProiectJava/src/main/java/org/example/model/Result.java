package org.example.model;

import java.util.Objects;

/**
 * Represents a Result entity in the system.
 * This class extends the base Entity class with a Long type identifier.
 * It stores the relationship between an Event, a Participant, and the points scored.
 */
public class Result extends Entity<Long>{
    /**
     * The event associated with this result.
     */
    private Event event;

    /**
     * The participant who achieved this result.
     */
    private Participant participant;

    /**
     * The number of points scored by the participant in the event.
     */
    private int points;

    /**
     * Default constructor that creates a Result with null ID.
     * Required for frameworks that use default constructors (e.g., ORM frameworks).
     */
    public Result(){
        super(null);
    }

    /**
     * Constructs a new Result with the specified ID, event, participant, and points.
     *
     * @param id The unique identifier for this result
     * @param event The event associated with this result
     * @param participant The participant who achieved this result
     * @param points The number of points scored
     */
    public Result(Long id, Event event, Participant participant, int points) {
        super(id);
        this.event = event;
        this.participant = participant;
        this.points = points;
    }

    /**
     * Gets the event associated with this result.
     *
     * @return The associated event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Sets the event for this result.
     *
     * @param event The event to associate with this result
     */
    public void setEvent(Event event) {
        this.event = event;
    }

    /**
     * Gets the participant who achieved this result.
     *
     * @return The participant associated with this result
     */
    public Participant getParticipant() {
        return participant;
    }

    /**
     * Sets the participant for this result.
     *
     * @param participant The participant who achieved this result
     */
    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    /**
     * Gets the number of points scored.
     *
     * @return The points scored by the participant
     */
    public int getPoints() {
        return points;
    }

    /**
     * Sets the number of points scored.
     *
     * @param points The points to set for this result
     */
    public void setPoints(int points) {
        this.points = points;
    }

    /**
     * Determines whether this result is equal to another object.
     * Results are considered equal if they have the same ID, event, participant, and points.
     *
     * @param o The object to compare with this result
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Result result = (Result) o;
        return points == result.points && Objects.equals(event, result.event) && Objects.equals(participant, result.participant);
    }

    /**
     * Generates a hash code for this result based on its ID, event, participant, and points.
     *
     * @return The hash code value for this result
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), event, participant, points);
    }

    /**
     * Returns a string representation of this result.
     *
     * @return A string containing the result's ID, event, participant, and points
     */
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