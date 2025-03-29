package org.example.repository;

import org.example.model.Participant;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Participant entities.
 * Extends the generic repository interface with Long as the ID type and Event as the entity type.
 * This interface can be extended with Event-specific data access methods as needed.
 */
public interface IRepositoryParticipant extends IRepository <UUID,Participant> {
    // Specific methods can be added here
}
