package org.example.repository;

import org.example.model.Event;
/**
 * Repository interface for Event entities.
 * Extends the generic repository interface with Long as the ID type and Event as the entity type.
 * This interface can be extended with Event-specific data access methods as needed.
 */
public interface IRepositoryEvent extends IRepository<Long,Event> {
    // Event-specific methods can be added here
}

