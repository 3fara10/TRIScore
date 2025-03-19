package org.example.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Participant entity in the system.
 * This class extends the base Entity class with a Long type identifier.
 * It stores participant-specific information such as name.
 */
public class Participant extends Entity<UUID>{
    /**
     * The name of the participant.
     */
    private String name;

    /**
     * Default constructor that creates a Participant with null ID.
     * Required for frameworks that use default constructors (e.g., ORM frameworks).
     */
    public Participant() {
        super(null);
    }


    /**
     * Constructs a new Participant with the specified ID and name.
     *
     * @param id The unique identifier for this participant
     */
    public Participant(UUID id){
        super(id);
    }



    /**
     * Constructs a new Participant with the specified ID and name.
     *
     * @param id The unique identifier for this participant
     * @param name The name of the participant
     */
    public Participant(UUID id, String name) {
        super(id);
        this.name = name;
    }





    /**
     * Determines whether this participant is equal to another object.
     * Participants are considered equal if they have the same ID and name.
     *
     * @param o The object to compare with this participant
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Participant that = (Participant) o;
        return Objects.equals(name, that.name);
    }


    /**
     * Generates a hash code for this participant based on its ID and name.
     *
     * @return The hash code value for this participant
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }


    /**
     * Returns a string representation of this participant.
     *
     * @return A string containing the participant's ID and name
     */
    @Override
    public String toString() {
        return "Participant{" +
                super.toString()+
                ", name='" + name + '\'' +
                '}';
    }


    /**
     * Gets the name of the participant.
     *
     * @return The participant's name
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the name of the participant.
     *
     * @param name The name to set for this participant
     */
    public void setName(String name) {
        this.name = name;
    }
}