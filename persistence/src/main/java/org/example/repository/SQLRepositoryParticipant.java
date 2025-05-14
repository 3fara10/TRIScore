package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Participant;
import org.example.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLRepositoryParticipant implements IRepositoryParticipant {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryParticipant(Properties props) {
        logger.info("Initializing SQLRepositoryParticipant with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Participant> findOne(UUID id) {
        logger.traceEntry("Finding participant with id {}", id);
            try {
                if (id == null) {
                    logger.error("Attempted to find participant with null id");
                    throw new ValidationException("Attempted to find participant with null id");
                }
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }
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
    }

    @Override
    public Iterable<Participant> findAll() {
        logger.traceEntry("Finding all participants");

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
    }

    @Override
    public Optional<Participant> add(Participant participant) {
        logger.traceEntry("Saving participant {}", participant);

        if (participant == null) {
            logger.error("Attempted to add null participant");
            throw new ValidationException("Attempted to add null participant");
        }
        
            try {
                Participant participantToSave = participant;
                if (participantToSave.getId() == null) {
                    participantToSave = generateParticipant(participant);
                }
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }
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
    }

    @Override
    public Optional<Participant> update(Participant participant, Participant newParticipant) {
        logger.traceEntry("Updating participant {}", participant);
        
            try {
                if (participant == null) {
                    logger.error("Attempted to update null participant");
                    throw new ValidationException("Attempted to update null participant");
                }

                if (participant.getId() == null) {
                    logger.error("Attempted to update participant with null id");
                    throw new ValidationException("Cannot update participant with null id");
                }
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "UPDATE participants SET name = ? WHERE id = ?")) {

                        preStmt.setString(1, newParticipant.getName());
                        preStmt.setString(2, participant.getId().toString());

                        int rowsAffected = preStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            logger.traceExit("Successfully updated participant with id {}", participant.getId());
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
    }

    @Override
    public Optional<Participant> delete(UUID id) {
       throw new UnsupportedOperationException("This method is not implemented yet.");
    }

    private Participant generateParticipant(Participant participant) {
        UUID newId = UUID.randomUUID();
        logger.info("Created new participant with id {}", newId);

        try {
            // Check if the generated ID already exists to avoid collisions
            Optional<Participant> existingParticipant = findOne(newId);
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