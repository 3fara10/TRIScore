package org.example.model;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Entity<ID> {
    /**
     * The unique identifier for this entity.
     */
    @Id
    @Column(name = "id")
    protected ID id;

    /**
     * Constructs a new Entity with the specified identifier.
     *
     * @param id The unique identifier for this entity
     */
    public Entity(ID id) {
        this.id = id;
    }

    /**
     * Retrieves the unique identifier of this entity.
     *
     * @return The unique identifier
     */
    public ID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this entity.
     *
     * @param id The unique identifier to set
     */
    public void setId(ID id) {
        this.id = id;
    }

    /**
     * Returns a string representation of this entity.
     *
     * @return A string containing the entity's ID
     */
    @Override
    public String toString() {
        return "id=" + id;
    }

    /**
     * Determines whether this entity is equal to another object.
     * Entities are considered equal if they have the same non-null ID.
     *
     * @param obj The object to compare with this entity
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Entity<?> entity = (Entity<?>) obj;
        return id != null && id.equals(entity.getId());
    }

    /**
     * Generates a hash code for this entity based on its ID.
     *
     * @return The hash code value for this entity
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

