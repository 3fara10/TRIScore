package org.example.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Referee entity in the system.
 * This class extends the base Entity class with a UUID type identifier.
 * It stores referee-specific information such as name, associated event,
 * and authentication credentials.
 */
public class Referee extends Entity<UUID> {
    /**
     * The name of the referee.
     */
    private String name;

    /**
     * The event that this referee is assigned to.
     */
    private Event event;

    /**
     * The username for referee authentication.
     */
    private String username;

    /**
     * The password for referee authentication.
     */
    private String password;

    /**
     * Default constructor that creates a Referee with empty ID.
     * Required for frameworks that use default constructors (e.g., ORM frameworks).
     */
    public Referee() {
        super(UUID.randomUUID());
    }

    /**
     * Constructs a new Referee with the specified name, event, username, and password.
     *
     * @param name     The name of the referee
     * @param newEvent The event that this referee is assigned to
     * @param username The username for referee authentication
     * @param password The password for referee authentication
     */
    public Referee(String name, Event newEvent, String username, String password) {
        super(UUID.randomUUID());
        this.name = name;
        this.event = newEvent;
        this.username = username;
        this.password = password;
    }

    /**
     * Constructs a new Referee with the specified ID, name, event, username, and password.
     *
     * @param id       The unique identifier for this referee
     * @param name     The name of the referee
     * @param newEvent The event that this referee is assigned to
     * @param username The username for referee authentication
     * @param password The password for referee authentication
     */
    public Referee(UUID id, String name, Event newEvent, String username, String password) {
        super(id);
        this.name = name;
        this.event = newEvent;
        this.username = username;
        this.password = password;
    }

    /**
     * Constructs a new Referee with the specified username and password.
     *
     * @param username The username for referee authentication
     * @param password The password for referee authentication
     */
    public Referee(String username, String password) {
        super(UUID.randomUUID());
        this.username = username;
        this.password = password;
        this.event = new Event();
        this.name = "";
    }

    /**
     * Constructs a new Referee with the specified ID.
     *
     * @param id The unique identifier for this referee
     */
    public Referee(UUID id) {
        super(id);
    }

    /**
     * Gets the name of the referee.
     *
     * @return The referee's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the referee.
     *
     * @param name The name to set for this referee
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the event that this referee is assigned to.
     *
     * @return The associated event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Sets the event for this referee.
     *
     * @param newEvent The event to assign to this referee
     */
    public void setEvent(Event newEvent) {
        this.event = newEvent;
    }

    /**
     * Gets the username for referee authentication.
     *
     * @return The referee's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username for referee authentication.
     *
     * @param username The username to set for this referee
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password for referee authentication.
     *
     * @return The referee's password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for referee authentication.
     *
     * @param password The password to set for this referee
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Determines whether this referee is equal to another object.
     * Referees are considered equal if they have the same ID, name, event,
     * username, and password.
     *
     * @param obj The object to compare with this referee
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        Referee referee = (Referee) obj;
        return Objects.equals(name, referee.name) &&
                Objects.equals(event, referee.event) &&
                Objects.equals(username, referee.username) &&
                Objects.equals(password, referee.password);
    }

    /**
     * Generates a hash code for this referee based on its ID, name, event,
     * username, and password.
     *
     * @return The hash code value for this referee
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, event, username, password);
    }

    /**
     * Returns a string representation of this referee.
     *
     * @return A string containing the referee's name, event, username, and password
     */
    @Override
    public String toString() {
        return "Referee{name='" + name + "', event=" + event + ", username='" + username + "', password='" + password + "'}";
    }
}