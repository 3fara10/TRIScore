package org.example.repository;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.Referee;
import org.example.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLRepositoryReferee implements IRepositoryReferee {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryReferee(Properties props) {
        logger.info("Initializing SQLRepositoryReferee with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Referee> findOne(UUID id) {
        logger.traceEntry("Finding referee with id {}", id);
            try {
                if (id == null) {
                    logger.error("Attempted to find referee with null id");
                    throw new ValidationException("Attempted to find referee with null id");
                }
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "SELECT r.id as referee_id, r.name as referee_name, r.username, r.password, " +
                                    "e.id as event_id, e.name as event_name " +
                                    "FROM referees r " +
                                    "JOIN events e ON r.event_id = e.id " +
                                    "WHERE r.id = ?")) {

                        preStmt.setString(1, id.toString());
                        try (ResultSet result = preStmt.executeQuery()) {
                            if (result.next()) {
                                UUID eventId = UUID.fromString(result.getString("event_id"));
                                String eventName = result.getString("event_name");
                                Event event = new Event(eventId, eventName);

                                String refereeName = result.getString("referee_name");
                                String username = result.getString("username");
                                String password = result.getString("password");

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

                List<Referee> referees = new ArrayList<>();
                try (PreparedStatement preStmt = connection.prepareStatement(
                        "SELECT r.id as referee_id, r.name as referee_name, r.username, r.password, " +
                                "e.id as event_id, e.name as event_name " +
                                "FROM referees r " +
                                "JOIN events e ON r.event_id = e.id");
                     ResultSet result = preStmt.executeQuery()) {

                    while (result.next()) {
                        UUID eventId = UUID.fromString(result.getString("event_id"));
                        String eventName = result.getString("event_name");
                        Event event = new Event(eventId, eventName);

                        UUID refereeId = UUID.fromString(result.getString("referee_id"));
                        String refereeName = result.getString("referee_name");
                        String username = result.getString("username");
                        String password = result.getString("password");

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

    /**
     * Adds a new referee to the database
     * @param referee The referee to add
     * @return Optional containing the referee if operation failed, empty Optional if successful
     */
    public Optional<Referee> add(Referee referee) {
        logger.debug("Saving referee {}", referee);

        try {
            if (referee == null) {
                logger.error("Attempted to add null referee");
                throw new IllegalArgumentException("Attempted to add null referee");
            }

            if (referee.getEvent() == null || referee.getEvent().getId().equals(UUID.randomUUID())) {
                logger.error("Attempted to add referee with null or invalid event");
                throw new ValidationException("Referee must have a valid event with non-empty ID");
            }

            if (referee.getId().equals(UUID.randomUUID())) {
                referee = generateReferee(referee);
            }

            try (Connection connection = dbUtils.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO referees (id, name, event_id, username, password) " +
                                "VALUES (?, ?, ?, ?, ?)")) {

                    statement.setString(1, referee.getId().toString());
                    statement.setString(2, referee.getName());
                    statement.setString(3, referee.getEvent().getId().toString());
                    statement.setString(4, referee.getUsername());
                    statement.setString(5, referee.getPassword());

                    int rowsAffected = statement.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.debug("Successfully saved referee with id {}", referee.getId());
                        return Optional.of(referee); // Success, return empty Optional
                    } else {
                        logger.error("Failed to insert referee, no rows affected.");
                        return Optional.empty(); // Failure, return the referee
                    }
                }
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        } catch (SQLException e) {
            logger.error("Database error while adding referee: {}", e.getMessage(), e);
            throw new RepositoryException("Database error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while adding referee: {}", e.getMessage(), e);
            throw new RepositoryException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> update(Referee referee, Referee newReferee) {
        logger.traceEntry("Updating referee {}", referee);
        
            try {
                if (referee == null) {
                    logger.error("Attempted to update null referee");
                    throw new ValidationException("Attempted to update null referee");
                }

                if (referee.getId() == null) {
                    logger.error("Attempted to update referee with null id");
                    throw new ValidationException("Cannot update referee with null id");
                }

                if (referee.getEvent() == null || referee.getEvent().getId() == null) {
                    logger.error("Attempted to update referee with null or invalid event");
                    throw new ValidationException("Referee must have a valid event with non-empty ID");
                }

                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "UPDATE referees " +
                                    "SET name = ?, event_id = ?, username = ?, password = ? " +
                                    "WHERE id = ?")) {

                        preStmt.setString(1, newReferee.getName());
                        preStmt.setString(2, newReferee.getEvent().getId().toString());
                        preStmt.setString(3, newReferee.getUsername());
                        preStmt.setString(4, newReferee.getPassword());
                        preStmt.setString(5, referee.getId().toString());

                        int rowsAffected = preStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            logger.traceExit("Successfully updated referee with id {}", referee.getId());
                            return Optional.of(newReferee);
                        } else {
                            logger.error("Failed to update referee with id {}, no rows affected.", referee.getId());
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
        throw new UnsupportedOperationException("This method is not implemented yet");
    }

    @Override
    public Optional<Referee> findByUsername(String username) {
        logger.traceEntry("Finding referee with username {}", username);
        
            try {
                // 1. Validate username
                if (username == null || username.trim().isEmpty()) {
                    logger.error("Attempted to find referee with null or empty username");
                    throw new ValidationException("Username cannot be null or empty");
                }

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Query referee by username
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "SELECT r.id as referee_id, r.name as referee_name, r.username, r.password, " +
                                    "e.id as event_id, e.name as event_name " +
                                    "FROM referees r " +
                                    "JOIN events e ON r.event_id = e.id " +
                                    "WHERE r.username = ?")) {

                        preStmt.setString(1, username);

                        try (ResultSet result = preStmt.executeQuery()) {
                            if (result.next()) {
                                // Get event data
                                UUID eventId = UUID.fromString(result.getString("event_id"));
                                String eventName = result.getString("event_name");
                                Event event = new Event(eventId, eventName);

                                // Get referee data
                                UUID refereeId = UUID.fromString(result.getString("referee_id"));
                                String refereeName = result.getString("referee_name");
                                String password = result.getString("password");

                                // Create referee
                                Referee referee = new Referee(refereeId, refereeName, event, username, password);

                                logger.traceExit("Found referee: {}", referee);
                                return Optional.of(referee);
                            } else {
                                logger.traceExit("No referee found with username {}", username);
                                return Optional.empty();
                            }
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Database error while finding referee by username: {}", e.getMessage(), e);
                    throw new RepositoryException("Database error: " + e.getMessage(), e);
                }
            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            }
    }


    private Referee generateReferee(Referee referee) {
        UUID newId = UUID.randomUUID();
        logger.info("Created new referee with id {}", newId);

        try (Connection connection = dbUtils.getConnection()) {
            if (connection != null) {
                try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM referees WHERE id = ?")) {
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

        return new Referee(newId, referee.getName(), referee.getEvent(), referee.getUsername(), referee.getPassword());
    }
}