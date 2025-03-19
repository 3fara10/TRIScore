package org.example.model;

import java.util.Objects;

/**
 * Represents an Event entity in the system.
 * This class extends the base Entity class with a Long type identifier.
 * It adds event-specific fields such as name.
 */
public class Event extends Entity<Long>{
    /**
     * The name of the event.
     */
    private String name;

    /**
     * Default constructor that creates an Event with null ID.
     * Required for frameworks that use default constructors.
     */
    public Event() {
        super(null);
    }

    /**
     * Constructs a new Event with the specified ID and name.
     *
     * @param id The unique identifier for this event
     * @param name The name of the event
     */
    public Event(Long id, String name) {
        super(id);
        this.name = name;
    }

    /**
     * Gets the name of the event.
     *
     * @return The event name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event.
     *
     * @param name The name to set for this event
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Determines whether this event is equal to another object.
     * Events are considered equal if they have the same ID and name.
     *
     * @param o The object to compare with this event
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Event event = (Event) o;
        return Objects.equals(name, event.name);
    }

    /**
     * Generates a hash code for this event based on its ID and name.
     *
     * @return The hash code value for this event
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    /**
     * Returns a string representation of this event.
     *
     * @return A string containing the event's ID and name
     */
    @Override
    public String toString() {
        return "Event{" + super.toString() +
                ", name='" + name + '\'' +
                '}';
    }
}