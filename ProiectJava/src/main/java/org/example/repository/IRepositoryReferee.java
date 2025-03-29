package org.example.repository;

import org.example.model.Referee;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Referee entities.
 * Extends the generic repository interface with Long as the ID type and Event as the entity type.
 * This interface can be extended with Event-specific data access methods as needed.
 */
public interface IRepositoryReferee extends IRepository<UUID,Referee> {
    CompletableFuture<Iterable<Referee>> findByEventIdAsync(UUID eventId);

    CompletableFuture<Optional<Referee>> findByUsernameAsync(String username);
    // Specific methods can be added here
}
