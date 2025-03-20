package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.ValidationException;
import org.example.model.Event;
import org.example.model.Referee;
import org.example.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class SQLRepositoryReferee implements IRepositoryReferee {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryReferee(Properties props) {
        logger.info("Initializing SQLRepositoryReferee with properties: {}", props);
        this.dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Referee> findOne(UUID id) {
        logger.traceEntry("Finding referee with id {}", id);

        try {
            if (id == null) {
                logger.error("Attempted to find referee with null id");
                throw new ValidationException("Referee id cannot be null");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // Using JOIN to get referee and event data in one query
                String sql = "SELECT r.id as referee_id, r.name as referee_name, " +
                        "r.username, r.password, " +
                        "e.id as event_id, e.name as event_name " +
                        "FROM referees r " +
                        "JOIN events e ON r.event_id = e.id " +
                        "WHERE r.id = ?";

                try (PreparedStatement preStmt = connection.prepareStatement(sql)) {
                    preStmt.setString(1, id.toString());

                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            String refereeName = result.getString("referee_name");
                            String username = result.getString("username");
                            String password = result.getString("password");
                            UUID eventId = UUID.fromString(result.getString("event_id"));
                            String eventName = result.getString("event_name");

                            Event event = new Event(eventId, eventName);
                            Referee referee = new Referee(id, refereeName, event, username, password);

                            logger.traceExit("Found referee: {}", referee);
                            return Optional.of(referee);
                        } else {
                            logger.traceExit("No referee found with id {}", id);
                            return Optional.empty();
                        }
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while finding referee: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Referee> findAll() {
        logger.traceEntry("Finding all referees");

        try (Connection connection = dbUtils.getConnection()) {
            if (connection == null) {
                logger.error("Couldn't connect to the database");
                throw new RepositoryException("Couldn't connect to the database.");
            }

            // Using JOIN to get all referees with their events
            String sql = "SELECT r.id as referee_id, r.name as referee_name, " +
                    "r.username, r.password, " +
                    "e.id as event_id, e.name as event_name " +
                    "FROM referees r " +
                    "JOIN events e ON r.event_id = e.id";

            List<Referee> referees = new ArrayList<>();
            try (PreparedStatement preStmt = connection.prepareStatement(sql);
                 ResultSet result = preStmt.executeQuery()) {

                while (result.next()) {
                    UUID refereeId = UUID.fromString(result.getString("referee_id"));
                    String refereeName = result.getString("referee_name");
                    String username = result.getString("username");
                    String password = result.getString("password");
                    UUID eventId = UUID.fromString(result.getString("event_id"));
                    String eventName = result.getString("event_name");

                    Event event = new Event(eventId, eventName);
                    Referee referee = new Referee(refereeId, refereeName, event, username, password);

                    referees.add(referee);
                }
            }

            logger.traceExit("Found {} referees", referees.size());
            return referees;
        } catch (SQLException e) {
            logger.error("Database error while finding all referees: {}", e.getMessage(), e);
            throw new RepositoryException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> add(Referee referee) {
        logger.traceEntry("Saving referee {}", referee);

        try {
            if (referee == null) {
                logger.error("Attempted to add null referee");
                throw new ValidationException("Referee cannot be null");
            }

            if (referee.getEvent() == null || referee.getEvent().getId() == null) {
                logger.error("Attempted to add referee with null event or event id");
                throw new ValidationException("Referee must have an event with valid id");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // First, check if the event exists
                String checkSql = "SELECT 1 FROM events WHERE id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, referee.getEvent().getId().toString());
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            logger.error("Cannot add referee with non-existent event {}", referee.getEvent().getId());
                            throw new ValidationException("Cannot add referee with non-existent event");
                        }
                    }
                }

                // Generate ID if needed
                if (referee.getId() == null) {
                    referee.setId(UUID.randomUUID());

                    // Check for collision
                    String collisionSql = "SELECT 1 FROM referees WHERE id = ?";
                    try (PreparedStatement collisionStmt = connection.prepareStatement(collisionSql)) {
                        collisionStmt.setString(1, referee.getId().toString());
                        try (ResultSet collisionResult = collisionStmt.executeQuery()) {
                            if (collisionResult.next()) {
                                logger.warn("UUID collision detected: {}", referee.getId());
                                referee.setId(UUID.randomUUID());
                            }
                        }
                    }
                }

                // Insert the referee
                String insertSql = "INSERT INTO referees (id, name, event_id, username, password) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, referee.getId().toString());
                    insertStmt.setString(2, referee.getName());
                    insertStmt.setString(3, referee.getEvent().getId().toString());
                    insertStmt.setString(4, referee.getUsername());
                    insertStmt.setString(5, referee.getPassword());

                    int rowsAffected = insertStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully saved referee with id {}", referee.getId());
                        return Optional.of(referee);
                    } else {
                        logger.error("Failed to insert referee, no rows affected");
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while adding referee: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> update(Referee referee) {
        logger.traceEntry("Updating referee {}", referee);

        try {
            if (referee == null) {
                logger.error("Attempted to update null referee");
                throw new ValidationException("Referee cannot be null");
            }

            if (referee.getId() == null) {
                logger.error("Attempted to update referee with null id");
                throw new ValidationException("Cannot update referee with null id");
            }

            if (referee.getEvent() == null || referee.getEvent().getId() == null) {
                logger.error("Attempted to update referee with null event or event id");
                throw new ValidationException("Referee must have an event with valid id");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // Check if the event exists
                String checkSql = "SELECT 1 FROM events WHERE id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, referee.getEvent().getId().toString());
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            logger.error("Cannot update referee with non-existent event {}", referee.getEvent().getId());
                            throw new ValidationException("Cannot update referee with non-existent event");
                        }
                    }
                }

                // Update the referee
                String updateSql = "UPDATE referees SET name = ?, event_id = ?, username = ?, password = ? WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setString(1, referee.getName());
                    updateStmt.setString(2, referee.getEvent().getId().toString());
                    updateStmt.setString(3, referee.getUsername());
                    updateStmt.setString(4, referee.getPassword());
                    updateStmt.setString(5, referee.getId().toString());

                    int rowsAffected = updateStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully updated referee with id {}", referee.getId());
                        return Optional.of(referee);
                    } else {
                        logger.error("Failed to update referee with id {}, no rows affected", referee.getId());
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while updating referee: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> delete(UUID id) {
        logger.traceEntry("Deleting referee with id {}", id);

        try {
            if (id == null) {
                logger.error("Attempted to delete referee with null id");
                throw new ValidationException("Referee id cannot be null");
            }

            // Find the referee first to return it after deletion
            Optional<Referee> refereeToDelete = findOne(id);
            if (refereeToDelete.isEmpty()) {
                logger.traceExit("No referee found with id {}, nothing to delete", id);
                return Optional.empty();
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                String deleteSql = "DELETE FROM referees WHERE id = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, id.toString());

                    int rowsAffected = deleteStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully deleted referee with id {}", id);
                        return refereeToDelete;
                    } else {
                        logger.warn("Inconsistent state: referee with id {} was found but could not be deleted", id);
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while deleting referee: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }
}