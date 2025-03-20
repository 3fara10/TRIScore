package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.ValidationException;
import org.example.model.Result;
import org.example.model.Event;
import org.example.model.Participant;
import org.example.utils.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class SQLRepositoryResult implements IRepositoryResult {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryResult(Properties props) {
        logger.info("Initializing SQLRepositoryResult with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Result> findOne(UUID id) {
        logger.traceEntry("Finding result with id {}", id);

        try {
            if (id == null) {
                logger.error("Attempted to find result with null id");
                throw new ValidationException("Result id cannot be null");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // Using JOINs to get result, participant, and event data in a single query
                String sql = "SELECT r.id as result_id, r.points, " +
                        "p.id as participant_id, p.name as participant_name, " +
                        "e.id as event_id, e.name as event_name " +
                        "FROM results r " +
                        "JOIN participants p ON r.participant_id = p.id " +
                        "JOIN events e ON r.event_id = e.id " +
                        "WHERE r.id = ?";

                try (PreparedStatement preStmt = connection.prepareStatement(sql)) {
                    preStmt.setString(1, id.toString());

                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            int points = result.getInt("points");

                            // Extract participant data
                            UUID participantId = UUID.fromString(result.getString("participant_id"));
                            String participantName = result.getString("participant_name");
                            Participant participant = new Participant(participantId, participantName);

                            // Extract event data
                            UUID eventId = UUID.fromString(result.getString("event_id"));
                            String eventName = result.getString("event_name");
                            Event event = new Event(eventId, eventName);

                            Result resultObj = new Result(id, event, participant, points);
                            logger.traceExit("Found result: {}", resultObj);
                            return Optional.of(resultObj);
                        } else {
                            logger.traceExit("No result found with id {}", id);
                            return Optional.empty();
                        }
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while finding result: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Result> findAll() {
        logger.traceEntry("Finding all results");

        try (Connection connection = dbUtils.getConnection()) {
            if (connection == null) {
                logger.error("Couldn't connect to the database");
                throw new RepositoryException("Couldn't connect to the database.");
            }

            // Using JOINs to get all results with their participants and events
            String sql = "SELECT r.id as result_id, r.points, " +
                    "p.id as participant_id, p.name as participant_name, " +
                    "e.id as event_id, e.name as event_name " +
                    "FROM results r " +
                    "JOIN participants p ON r.participant_id = p.id " +
                    "JOIN events e ON r.event_id = e.id";

            List<Result> results = new ArrayList<>();
            try (PreparedStatement preStmt = connection.prepareStatement(sql);
                 ResultSet result = preStmt.executeQuery()) {

                while (result.next()) {
                    UUID resultId = UUID.fromString(result.getString("result_id"));
                    int points = result.getInt("points");

                    // Extract participant data
                    UUID participantId = UUID.fromString(result.getString("participant_id"));
                    String participantName = result.getString("participant_name");
                    Participant participant = new Participant(participantId, participantName);

                    // Extract event data
                    UUID eventId = UUID.fromString(result.getString("event_id"));
                    String eventName = result.getString("event_name");
                    Event event = new Event(eventId, eventName);

                    Result resultObj = new Result(resultId, event, participant, points);
                    results.add(resultObj);
                }
            }

            logger.traceExit("Found {} results", results.size());
            return results;
        } catch (SQLException e) {
            logger.error("Database error while finding all results: {}", e.getMessage(), e);
            throw new RepositoryException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> add(Result result) {
        logger.traceEntry("Adding result {}", result);

        try {
            if (result == null) {
                logger.error("Attempted to add null result");
                throw new ValidationException("Result cannot be null");
            }

            if (result.getParticipant() == null || result.getParticipant().getId() == null) {
                logger.error("Attempted to add result with null participant or participant id");
                throw new ValidationException("Result must have a participant with valid id");
            }

            if (result.getEvent() == null || result.getEvent().getId() == null) {
                logger.error("Attempted to add result with null event or event id");
                throw new ValidationException("Result must have an event with valid id");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // Verify that participant exists
                String checkParticipantSql = "SELECT 1 FROM participants WHERE id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkParticipantSql)) {
                    checkStmt.setString(1, result.getParticipant().getId().toString());
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            logger.error("Cannot add result: Participant with id {} does not exist",
                                    result.getParticipant().getId());
                            throw new ValidationException("Cannot add result with non-existent participant");
                        }
                    }
                }

                // Verify that event exists
                String checkEventSql = "SELECT 1 FROM events WHERE id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkEventSql)) {
                    checkStmt.setString(1, result.getEvent().getId().toString());
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            logger.error("Cannot add result: Event with id {} does not exist",
                                    result.getEvent().getId());
                            throw new ValidationException("Cannot add result with non-existent event");
                        }
                    }
                }

                // Generate ID if needed
                if (result.getId() == null) {
                    UUID newId = UUID.randomUUID();

                    // Check for collision
                    String collisionSql = "SELECT 1 FROM results WHERE id = ?";
                    try (PreparedStatement collisionStmt = connection.prepareStatement(collisionSql)) {
                        collisionStmt.setString(1, newId.toString());
                        try (ResultSet collisionResult = collisionStmt.executeQuery()) {
                            if (collisionResult.next()) {
                                logger.warn("UUID collision detected: {}", newId);
                                newId = UUID.randomUUID();
                            }
                        }
                    }

                    result.setId(newId);
                }

                // Insert the result
                String insertSql = "INSERT INTO results (id, participant_id, event_id, points) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, result.getId().toString());
                    insertStmt.setString(2, result.getParticipant().getId().toString());
                    insertStmt.setString(3, result.getEvent().getId().toString());
                    insertStmt.setInt(4, result.getPoints());

                    int rowsAffected = insertStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully saved result with id {}", result.getId());
                        return Optional.of(result);
                    } else {
                        logger.error("Failed to insert result, no rows affected.");
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while adding result: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> update(Result result) {
        logger.traceEntry("Updating result {}", result);

        try {
            if (result == null) {
                logger.error("Attempted to update null result");
                throw new ValidationException("Result cannot be null");
            }

            if (result.getId() == null) {
                logger.error("Attempted to update result with null id");
                throw new ValidationException("Cannot update result with null id");
            }

            if (result.getParticipant() == null || result.getParticipant().getId() == null) {
                logger.error("Attempted to update result with null participant or participant id");
                throw new ValidationException("Result must have a participant with valid id");
            }

            if (result.getEvent() == null || result.getEvent().getId() == null) {
                logger.error("Attempted to update result with null event or event id");
                throw new ValidationException("Result must have an event with valid id");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // Verify that result exists
                String checkResultSql = "SELECT 1 FROM results WHERE id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkResultSql)) {
                    checkStmt.setString(1, result.getId().toString());
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            logger.error("Cannot update non-existent result with id {}", result.getId());
                            return Optional.empty();
                        }
                    }
                }

                // Verify that participant exists
                String checkParticipantSql = "SELECT 1 FROM participants WHERE id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkParticipantSql)) {
                    checkStmt.setString(1, result.getParticipant().getId().toString());
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            logger.error("Cannot update result: Participant with id {} does not exist",
                                    result.getParticipant().getId());
                            throw new ValidationException("Cannot update result with non-existent participant");
                        }
                    }
                }

                // Verify that event exists
                String checkEventSql = "SELECT 1 FROM events WHERE id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkEventSql)) {
                    checkStmt.setString(1, result.getEvent().getId().toString());
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            logger.error("Cannot update result: Event with id {} does not exist",
                                    result.getEvent().getId());
                            throw new ValidationException("Cannot update result with non-existent event");
                        }
                    }
                }

                // Update the result
                String updateSql = "UPDATE results SET participant_id = ?, event_id = ?, points = ? WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setString(1, result.getParticipant().getId().toString());
                    updateStmt.setString(2, result.getEvent().getId().toString());
                    updateStmt.setInt(3, result.getPoints());
                    updateStmt.setString(4, result.getId().toString());

                    int rowsAffected = updateStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully updated result with id {}", result.getId());
                        return Optional.of(result);
                    } else {
                        logger.error("Failed to update result with id {}, no rows affected", result.getId());
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while updating result: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> delete(UUID id) {
        logger.traceEntry("Deleting result with id {}", id);

        try {
            if (id == null) {
                logger.error("Attempted to delete result with null id");
                throw new ValidationException("Result id cannot be null");
            }

            // Find the result first to return it after deletion
            Optional<Result> resultToDelete = findOne(id);
            if (resultToDelete.isEmpty()) {
                logger.traceExit("No result found with id {}, nothing to delete", id);
                return Optional.empty();
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                String deleteSql = "DELETE FROM results WHERE id = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, id.toString());

                    int rowsAffected = deleteStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully deleted result with id {}", id);
                        return resultToDelete;
                    } else {
                        logger.warn("Inconsistent state: result with id {} was found but could not be deleted", id);
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while deleting result: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    public Optional<Result> findByParticipantAndEvent(UUID participantId, UUID eventId) {
        logger.traceEntry("Finding result for participant id {} and event id {}", participantId, eventId);

        try {
            if (participantId == null) {
                logger.error("Attempted to find result with null participant id");
                throw new ValidationException("Participant id cannot be null");
            }

            if (eventId == null) {
                logger.error("Attempted to find result with null event id");
                throw new ValidationException("Event id cannot be null");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // Using JOINs to get result, participant, and event data in a single query
                String sql = "SELECT r.id as result_id, r.points, " +
                        "p.id as participant_id, p.name as participant_name, " +
                        "e.id as event_id, e.name as event_name " +
                        "FROM results r " +
                        "JOIN participants p ON r.participant_id = p.id " +
                        "JOIN events e ON r.event_id = e.id " +
                        "WHERE p.id = ? AND e.id = ?";

                try (PreparedStatement preStmt = connection.prepareStatement(sql)) {
                    preStmt.setString(1, participantId.toString());
                    preStmt.setString(2, eventId.toString());

                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            UUID resultId = UUID.fromString(result.getString("result_id"));
                            int points = result.getInt("points");

                            String participantName = result.getString("participant_name");
                            Participant participant = new Participant(participantId, participantName);

                            String eventName = result.getString("event_name");
                            Event event = new Event(eventId, eventName);

                            Result resultObj = new Result(resultId, event, participant, points);
                            logger.traceExit("Found result: {}", resultObj);
                            return Optional.of(resultObj);
                        } else {
                            logger.traceExit("No result found for participant id");
                            return Optional.empty();
                        }
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while finding result by participant and event: {}",
                        e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }
}