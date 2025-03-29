package org.example.repository;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.ValidationException;
import org.example.model.Participant;
import org.example.utils.JdbcUtils;
import org.example.utils.ThreadPoolManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SQLRepositoryParticipant implements IRepositoryParticipant {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryParticipant(Properties props) {
        logger.info("Initializing SQLRepositoryParticipant with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public CompletableFuture<Optional<Participant>> findOneAsync(UUID id) {
        logger.traceEntry("Finding participant with id {}", id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Verify the integrity of the parameter
                if (id == null) {
                    logger.error("Attempted to find participant with null id");
                    throw new ValidationException("Attempted to find participant with null id");
                }

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Query data
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "SELECT * FROM participants WHERE id = ?")) {

                        preStmt.setString(1, id.toString());
                        try (ResultSet result = preStmt.executeQuery()) {
                            if (result.next()) {
                                String name = result.getString("name");
                                Participant participant = new Participant(id, name);

                                logger.traceExit("Found participant: {}", participant);
                                return Optional.of(participant);
                            } else {
                                logger.traceExit("No participant found with id {}", id);
                                return Optional.empty();
                            }
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Database error while finding participant: {}", e.getMessage(), e);
                    throw new RepositoryException("Database error: " + e.getMessage(), e);
                }
            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            }
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Iterable<Participant>> findAllAsync() {
        logger.traceEntry("Finding all participants");

        return CompletableFuture.supplyAsync(() -> {
            // 1. Connect to the database
            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // 2. Query data
                List<Participant> participants = new ArrayList<>();
                try (PreparedStatement preStmt = connection.prepareStatement("SELECT * FROM participants");
                     ResultSet result = preStmt.executeQuery()) {

                    while (result.next()) {
                        UUID id = UUID.fromString(result.getString("id"));
                        String name = result.getString("name");
                        Participant participant = new Participant(id, name);
                        participants.add(participant);
                    }
                }
                logger.traceExit("Found {} participants", participants.size());
                return participants;

            } catch (SQLException e) {
                logger.error("Database error while finding all participants: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Optional<Participant>> addAsync(Participant participant) {
        logger.traceEntry("Saving participant {}", participant);

        if (participant == null) {
            logger.error("Attempted to add null participant");
            return CompletableFuture.failedFuture(
                    new ValidationException("Attempted to add null participant"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Generate ID if needed
                Participant participantToSave = participant;
                if (participantToSave.getId() == null) {
                    participantToSave = generateParticipant(participant);
                }

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Execute insert query
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "INSERT INTO participants (id, name) VALUES (?, ?)")) {

                        preStmt.setString(1, participantToSave.getId().toString());
                        preStmt.setString(2, participantToSave.getName());

                        int rowsAffected = preStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            logger.traceExit("Successfully saved participant with id {}", participantToSave.getId());
                            return Optional.of(participantToSave);
                        } else {
                            logger.error("Failed to insert participant, no rows affected.");
                            return Optional.empty();
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Database error while adding participant: {}", e.getMessage(), e);
                    throw new RepositoryException("Database error: " + e.getMessage(), e);
                }
            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            }
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Optional<Participant>> updateAsync(Participant participant, Participant newParticipant) {
        logger.traceEntry("Updating participant {}", participant);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Validate inputs
                if (participant == null) {
                    logger.error("Attempted to update null participant");
                    throw new ValidationException("Attempted to update null participant");
                }

                if (participant.getId() == null) {
                    logger.error("Attempted to update participant with null id");
                    throw new ValidationException("Cannot update participant with null id");
                }

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Execute update query
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "UPDATE participants SET name = ? WHERE id = ?")) {

                        preStmt.setString(1, newParticipant.getName());
                        preStmt.setString(2, participant.getId().toString());

                        int rowsAffected = preStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            logger.traceExit("Successfully updated participant with id {}", participant.getId());
                            // Return the updated participant
                            Participant updatedParticipant = new Participant(participant.getId(), newParticipant.getName());
                            return Optional.of(updatedParticipant);
                        } else {
                            logger.error("Failed to update participant with id {}, no rows affected.", participant.getId());
                            return Optional.empty();
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Database error while updating participant: {}", e.getMessage(), e);
                    throw new RepositoryException("Database error: " + e.getMessage(), e);
                }
            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            }
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Optional<Participant>> deleteAsync(UUID id) {
        logger.traceEntry("Deleting participant with id {}", id);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Validate ID
                if (id == null) {
                    logger.error("Attempted to delete participant with null id");
                    throw new ValidationException("Participant id cannot be null");
                }

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Execute delete with OUTPUT in a single statement
                    // SQL Server specific syntax using OUTPUT
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "DELETE FROM participants OUTPUT DELETED.id, DELETED.name WHERE id = ?")) {

                        preStmt.setString(1, id.toString());

                        try (ResultSet resultSet = preStmt.executeQuery()) {
                            if (resultSet.next()) {
                                // Create the deleted participant from the OUTPUT clause data
                                UUID deletedId = UUID.fromString(resultSet.getString("id"));
                                String name = resultSet.getString("name");
                                Participant deletedParticipant = new Participant(deletedId, name);

                                logger.traceExit("Deleted participant with id {}", id);
                                return Optional.of(deletedParticipant);
                            } else {
                                logger.debug("No participant found with id {}, nothing to delete", id);
                                return Optional.empty();
                            }
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Database error while deleting participant: {}", e.getMessage(), e);
                    throw new RepositoryException("Database error: " + e.getMessage(), e);
                }
            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            }
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    private Participant generateParticipant(Participant participant) {
        UUID newId = UUID.randomUUID();
        logger.info("Created new participant with id {}", newId);

        try {
            // Check if the generated ID already exists to avoid collisions
            Optional<Participant> existingParticipant = findOneAsync(newId).join();
            if (existingParticipant.isPresent()) {
                logger.warn("UUID collision detected: {}", newId);
                newId = UUID.randomUUID();
            }
        } catch (Exception e) {
            logger.error("Error checking for UUID collision: {}", e.getMessage(), e);
        }

        return new Participant(newId, participant.getName());
    }
}