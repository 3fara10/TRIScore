package org.example.repository;

import org.example.model.Entity;
import org.example.exceptions.ValidationException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Generic repository interface that defines standard CRUD operations.
 *
 * @param <ID> The type of the entity identifier
 * @param <T> The type of the entity, which must extend Entity<ID>
 */
public interface IRepository<ID, T extends Entity<ID>> {

    /**
     * @param id - the id of the entity to be returned
     *                 id must not be null
     * @return the entity with the specified id
     *         or null if there is no entity with the given id
     * @throws IllegalArgumentException - if id is null
     */
    CompletableFuture<Optional<T>> findOneAsync(ID id);

    /**
     * @return all entities
     */
    CompletableFuture<Iterable<T>> findAllAsync();

    /**
     * saves the given entity in repository
     * @param entity - entity must not be null
     * @return null - if the given entity is saved
     *                otherwise returns the entity (id already exists)
     * @throws ValidationException - if the entity is not valid
     * @throws IllegalArgumentException - if the given entity is null
     */
    CompletableFuture<Optional<T>> addAsync(T entity);

    /**
     * removes the entity with the specified id
     * @param id - id must not be null
     * @return the removed entity or null if there is no entity with the given id
     * @throws IllegalArgumentException - if the given id is null
     */
    CompletableFuture<Optional<T>> deleteAsync(ID id);

    /**
     *
     * @param entity - entity must not be null
     * @return null - if the entity is updated
     *                otherwise returns the entity - (e.g. id does not exist)
     * @throws IllegalArgumentException - if the given entity is null
     * @throws ValidationException - if the entity is not valid
     */
    CompletableFuture<Optional<T>> updateAsync(T entity,T newEntity);
}