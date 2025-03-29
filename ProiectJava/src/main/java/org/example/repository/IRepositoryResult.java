package org.example.repository;

import org.example.model.Result;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Result entities.
 * Extends the generic repository interface with Long as the ID type and Event as the entity type.
 * This interface can be extended with Event-specific data access methods as needed.
 */
public interface IRepositoryResult extends IRepository<UUID,Result> {
    CompletableFuture<Iterable<Result>> findByEventIdAsync(UUID eventId);

    CompletableFuture<Iterable<Result>> findByParticipantIdAsync(UUID participantId);

    CompletableFuture<Optional<Result>> findByEventAndParticipantAsync(UUID eventId, UUID participantId);

    CompletableFuture<Optional<Result>> addOrUpdateResultAsync(UUID participantId, UUID eventId, int points);
    // Specific methods can be added here
}
