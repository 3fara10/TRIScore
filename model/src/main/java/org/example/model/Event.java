package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an Event entity in the system.
 * This class extends the base Entity class with a UUID type identifier.
 * It adds event-specific fields such as name.
 */
@jakarta.persistence.Entity
@Table(name="events")
public class Event extends Entity<UUID> {
    /**
     * The name of the event.
     */
    @Column(name="name",nullable = false)
    private String name;

    /**
     * Default constructor that creates an Event with null ID.
     * Required for frameworks that use default constructors.
     */
    public Event() {
        super(UUID.randomUUID());
    }

    /**
     * Constructs a new Event with the specified name.
     *
     * @param name The name of the event
     */
    public Event(String name) {
        super(UUID.randomUUID());
        this.name = name;
    }

    /**
     * Constructs a new Event with the specified ID and name.
     *
     * @param id   The id of the event
     * @param name The name of the event
     */
    public Event(UUID id, String name) {
        super(id);
        this.name = name;
    }

    /**
     * Constructs a new Event with the specified ID.
     *
     * @param id The id of the event
     */
    public Event(UUID id) {
        super(id);
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
     * @param obj The object to compare with this event
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        Event event = (Event) obj;
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
        return "Event{" + super.toString() + ", name='" + name + "'}";
    }
}