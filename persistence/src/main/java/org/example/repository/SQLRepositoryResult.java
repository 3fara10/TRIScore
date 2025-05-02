package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.Participant;
import org.example.model.Result;
import org.example.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
                throw new ValidationException("Attempted to find result with null id");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                try (PreparedStatement preStmt = connection.prepareStatement(
                        "SELECT r.id as result_id, r.points, " +
                                "e.id as event_id, e.name as event_name, " +
                                "p.id as participant_id, p.name as participant_name " +
                                "FROM results r " +
                                "JOIN events e ON r.event_id = e.id " +
                                "JOIN participants p ON r.participant_id = p.id " +
                                "WHERE r.id = ?")) {

                    preStmt.setString(1, id.toString());
                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            UUID eventId = UUID.fromString(result.getString("event_id"));
                            String eventName = result.getString("event_name");
                            Event event = new Event(eventId, eventName);

                            UUID participantId = UUID.fromString(result.getString("participant_id"));
                            String participantName = result.getString("participant_name");
                            Participant participant = new Participant(participantId, participantName);

                            int points = result.getInt("points");

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

            List<Result> results = new ArrayList<>();
            try (PreparedStatement preStmt = connection.prepareStatement(
                    "SELECT r.id as result_id, r.points, " +
                            "e.id as event_id, e.name as event_name, " +
                            "p.id as participant_id, p.name as participant_name " +
                            "FROM results r " +
                            "JOIN events e ON r.event_id = e.id " +
                            "JOIN participants p ON r.participant_id = p.id");
                 ResultSet result = preStmt.executeQuery()) {

                while (result.next()) {
                    UUID eventId = UUID.fromString(result.getString("event_id"));
                    String eventName = result.getString("event_name");
                    Event event = new Event(eventId, eventName);

                    UUID participantId = UUID.fromString(result.getString("participant_id"));
                    String participantName = result.getString("participant_name");
                    Participant participant = new Participant(participantId, participantName);

                    UUID resultId = UUID.fromString(result.getString("result_id"));
                    int points = result.getInt("points");

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
        logger.traceEntry("Saving result {}", result);

        if (result == null) {
            logger.error("Attempted to add null result");
            throw new ValidationException("Attempted to add null result");
        }

        try {
            if (result.getEvent() == null || result.getEvent().getId() == null) {
                logger.error("Attempted to add result with null or invalid event");
                throw new ValidationException("Result must have a valid event with non-empty ID");
            }

            if (result.getParticipant() == null || result.getParticipant().getId() == null) {
                logger.error("Attempted to add result with null or invalid participant");
                throw new ValidationException("Result must have a valid participant with non-empty ID");
            }

            Result resultToSave = result;
            if (resultToSave.getId() == null) {
                resultToSave = generateResult(result);
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                try (PreparedStatement preStmt = connection.prepareStatement(
                        "INSERT INTO results (id, event_id, participant_id, points) " +
                                "VALUES (?, ?, ?, ?)")) {

                    preStmt.setString(1, resultToSave.getId().toString());
                    preStmt.setString(2, resultToSave.getEvent().getId().toString());
                    preStmt.setString(3, resultToSave.getParticipant().getId().toString());
                    preStmt.setInt(4, resultToSave.getPoints());

                    int rowsAffected = preStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully saved result with id {}", resultToSave.getId());
                        return Optional.of(resultToSave);
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
    public Optional<Result> update(Result result, Result newResult) {
        logger.traceEntry("Updating result {}", result);

        try {
            if (result == null) {
                logger.error("Attempted to update null result");
                throw new ValidationException("Attempted to update null result");
            }

            if (result.getId() == null) {
                logger.error("Attempted to update result with null id");
                throw new ValidationException("Cannot update result with null id");
            }

            if (result.getEvent() == null || result.getEvent().getId() == null) {
                logger.error("Attempted to update result with null or invalid event");
                throw new ValidationException("Result must have a valid event with non-empty ID");
            }

            if (result.getParticipant() == null || result.getParticipant().getId() == null) {
                logger.error("Attempted to update result with null or invalid participant");
                throw new ValidationException("Result must have a valid participant with non-empty ID");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }
                try (PreparedStatement preStmt = connection.prepareStatement(
                        "UPDATE results " +
                                "SET event_id = ?, participant_id = ?, points = ? " +
                                "WHERE id = ?")) {

                    preStmt.setString(1, newResult.getEvent().getId().toString());
                    preStmt.setString(2, newResult.getParticipant().getId().toString());
                    preStmt.setInt(3, newResult.getPoints());
                    preStmt.setString(4, result.getId().toString());

                    int rowsAffected = preStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully updated result with id {}", result.getId());
                        return Optional.of(newResult);
                    } else {
                        logger.error("Failed to update result with id {}, no rows affected.", result.getId());
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
        throw new UnsupportedOperationException("This method is not implemented");
    }


    @Override
    public Optional<Result> findByEventAndParticipant(UUID eventId, UUID participantId) {
        logger.traceEntry("Finding result for event {} and participant {}", eventId, participantId);

        try {
            if (eventId == null) {
                logger.error("Attempted to find result with null event id");
                throw new ValidationException("Event id cannot be null");
            }

            if (participantId == null) {
                logger.error("Attempted to find result with null participant id");
                throw new ValidationException("Participant id cannot be null");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                try (PreparedStatement preStmt = connection.prepareStatement(
                        "SELECT r.id as result_id, r.points, " +
                                "e.id as event_id, e.name as event_name, " +
                                "p.id as participant_id, p.name as participant_name " +
                                "FROM results r " +
                                "JOIN events e ON r.event_id = e.id " +
                                "JOIN participants p ON r.participant_id = p.id " +
                                "WHERE e.id = ? AND p.id = ?")) {

                    preStmt.setString(1, eventId.toString());
                    preStmt.setString(2, participantId.toString());

                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            String eventName = result.getString("event_name");
                            Event event = new Event(eventId, eventName);

                            String participantName = result.getString("participant_name");
                            Participant participant = new Participant(participantId, participantName);

                            UUID resultId = UUID.fromString(result.getString("result_id"));
                            int points = result.getInt("points");

                            Result resultObj = new Result(resultId, event, participant, points);

                            logger.traceExit("Found result: {}", resultObj);
                            return Optional.of(resultObj);
                        } else {
                            logger.info("No result found for event {} and participant {}", eventId, participantId);
                            return Optional.empty();
                        }
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while finding result by event and participant: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> addOrUpdateResult(UUID participantId, UUID eventId, int points) {
        logger.traceEntry("Adding or updating result for Participant ID: {}, Event ID: {}, Points: {}",
                participantId, eventId, points);

        try {
            if (points < 0) {
                logger.error("Points must be a positive number");
                throw new ValidationException("Points must be a positive number");
            }

            if (participantId == null) {
                logger.error("Participant ID cannot be null");
                throw new ValidationException("Participant ID cannot be null");
            }

            if (eventId == null) {
                logger.error("Event ID cannot be null");
                throw new ValidationException("Event ID cannot be null");
            }

            Optional<Result> existingResult = findByEventAndParticipant(eventId, participantId);

            if (existingResult.isPresent()) {
                Result result = existingResult.get();
                Result newResult = new Result(result.getId(), result.getEvent(), result.getParticipant(), result.getPoints() + points);

                update(result, newResult);
                logger.info("Updated existing result with ID {}", result.getId());

                return Optional.of(newResult);
            } else {
                Event event = null;
                Participant participant = null;

                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "SELECT (SELECT COUNT(*) FROM participants WHERE id = ?) AS participantExists, " +
                                    "(SELECT COUNT(*) FROM events WHERE id = ?) AS eventExists, " +
                                    "(SELECT name FROM participants WHERE id = ?) AS participantName, " +
                                    "(SELECT name FROM events WHERE id = ?) AS eventName")) {

                        preStmt.setString(1, participantId.toString());
                        preStmt.setString(2, eventId.toString());
                        preStmt.setString(3, participantId.toString());
                        preStmt.setString(4, eventId.toString());

                        try (ResultSet resultSet = preStmt.executeQuery()) {
                            if (resultSet.next()) {
                                boolean participantExists = resultSet.getInt("participantExists") > 0;
                                boolean eventExists = resultSet.getInt("eventExists") > 0;

                                if (!participantExists) {
                                    logger.error("Participant with ID {} not found", participantId);
                                    throw new ValidationException("Participant with ID " + participantId + " not found");
                                }

                                if (!eventExists) {
                                    logger.error("Event with ID {} not found", eventId);
                                    throw new ValidationException("Event with ID " + eventId + " not found");
                                }

                                String participantName = resultSet.getString("participantName");
                                String eventName = resultSet.getString("eventName");

                                participant = new Participant(participantId, participantName);
                                event = new Event(eventId, eventName);
                            }
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Database error while checking entities: {}", e.getMessage(), e);
                    throw new RepositoryException("Database error: " + e.getMessage(), e);
                }

                Result newResult = new Result(event, participant, points);

                if (newResult.getId() == null) {
                    newResult = generateResult(newResult);
                }

                Optional<Result> addResult = add(newResult);
                logger.info("Created new result with ID {}", newResult.getId());

                return Optional.of(newResult);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error while adding/updating result: {}", e.getMessage(), e);
            throw new RepositoryException("Error: " + e.getMessage(), e);
        }
    }

    private Result generateResult(Result result) {
        UUID newId = UUID.randomUUID();
        logger.info("Created new result with id {}", newId);

        try (Connection connection = dbUtils.getConnection()) {
            if (connection != null) {
                try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM results WHERE id = ?")) {
                    stmt.setString(1, newId.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            logger.warn("UUID collision detected: {}", newId);
                            newId = UUID.randomUUID();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking for UUID collision: {}", e.getMessage(), e);
        }

        return new Result(newId, result.getEvent(), result.getParticipant(), result.getPoints());
    }
}