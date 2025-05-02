package org.example.repository;

import org.example.model.Result;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Result entities.
 * Extends the generic repository interface with Long as the ID type and Event as the entity type.
 * This interface can be extended with Event-specific data access methods as needed.
 */
public interface IRepositoryResult extends IRepository<UUID, Result> {
    Optional<Result> findByEventAndParticipant(UUID eventId, UUID participantId);

    Optional<Result> addOrUpdateResult(UUID participantId, UUID eventId, int points);
}
