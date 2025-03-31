package org.example.repository;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.ValidationException;
import org.example.model.Event;
import org.example.model.Referee;
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

public class SQLRepositoryReferee implements IRepositoryReferee {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryReferee(Properties props) {
        logger.info("Initializing SQLRepositoryReferee with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public CompletableFuture<Optional<Referee>> findOneAsync(UUID id) {
        logger.traceEntry("Finding referee with id {}", id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Verify the integrity of the parameter
                if (id == null) {
                    logger.error("Attempted to find referee with null id");
                    throw new ValidationException("Attempted to find referee with null id");
                }

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Query data with joins to get related Event
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "SELECT r.id as referee_id, r.name as referee_name, r.username, r.password, " +
                                    "e.id as event_id, e.name as event_name " +
                                    "FROM referees r " +
                                    "JOIN events e ON r.event_id = e.id " +
                                    "WHERE r.id = ?")) {

                        preStmt.setString(1, id.toString());
                        try (ResultSet result = preStmt.executeQuery()) {
                            if (result.next()) {
                                // Extract event data
                                UUID eventId = UUID.fromString(result.getString("event_id"));
                                String eventName = result.getString("event_name");
                                Event event = new Event(eventId, eventName);

                                // Extract referee data
                                String refereeName = result.getString("referee_name");
                                String username = result.getString("username");
                                String password = result.getString("password");

                                // Create and return referee object
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
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Iterable<Referee>> findAllAsync() {
        logger.traceEntry("Finding all referees");

        return CompletableFuture.supplyAsync(() -> {
            // 1. Connect to the database
            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                // 2. Query data with join
                List<Referee> referees = new ArrayList<>();
                try (PreparedStatement preStmt = connection.prepareStatement(
                        "SELECT r.id as referee_id, r.name as referee_name, r.username, r.password, " +
                                "e.id as event_id, e.name as event_name " +
                                "FROM referees r " +
                                "JOIN events e ON r.event_id = e.id");
                     ResultSet result = preStmt.executeQuery()) {

                    while (result.next()) {
                        // Extract event data
                        UUID eventId = UUID.fromString(result.getString("event_id"));
                        String eventName = result.getString("event_name");
                        Event event = new Event(eventId, eventName);

                        // Extract referee data
                        UUID refereeId = UUID.fromString(result.getString("referee_id"));
                        String refereeName = result.getString("referee_name");
                        String username = result.getString("username");
                        String password = result.getString("password");

                        // Create and add referee object
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
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Optional<Referee>> addAsync(Referee referee) {
        logger.traceEntry("Saving referee {}", referee);

        if (referee == null) {
            logger.error("Attempted to add null referee");
            return CompletableFuture.failedFuture(
                    new ValidationException("Attempted to add null referee"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {

                // 2. Generate ID if needed
                Referee refereeToSave = referee;
                if (refereeToSave.getId() == null) {
                    refereeToSave = generateReferee(referee);
                }

                // 3. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    String eventId = refereeToSave.getEvent().getId().toString();
                    try (PreparedStatement checkStmt = connection.prepareStatement(
                            "SELECT COUNT(*) FROM referees WHERE event_id = ?")) {

                        checkStmt.setString(1, eventId);

                        try (ResultSet resultSet = checkStmt.executeQuery()) {
                            if (resultSet.next() && resultSet.getInt(1) > 0) {
                                logger.warn("Event with id {} already has a referee assigned", eventId);
                                throw new ValidationException("This event already has a referee assigned");
                            }
                        }
                    }
                    // 4. Execute insert query
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "INSERT INTO referees (id, name, event_id, username, password) " +
                                    "VALUES (?, ?, ?, ?, ?)")) {

                        preStmt.setString(1, refereeToSave.getId().toString());
                        preStmt.setString(2, refereeToSave.getName());
                        preStmt.setString(3, refereeToSave.getEvent().getId().toString());
                        preStmt.setString(4, refereeToSave.getUsername());
                        preStmt.setString(5, refereeToSave.getPassword());

                        int rowsAffected = preStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            logger.traceExit("Successfully saved referee with id {}", refereeToSave.getId());
                            return Optional.of(refereeToSave);
                        } else {
                            logger.error("Failed to insert referee, no rows affected.");
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
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Optional<Referee>> updateAsync(Referee referee, Referee newReferee) {
        logger.traceEntry("Updating referee {}", referee);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Validate inputs
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

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Execute update query
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
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Optional<Referee>> deleteAsync(UUID id) {
        logger.traceEntry("Deleting referee with id {}", id);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Validate ID
                if (id == null) {
                    logger.error("Attempted to delete referee with null id");
                    throw new ValidationException("Referee id cannot be null");
                }

                // 2. First, get the referee that will be deleted
                Optional<Referee> refereeToDelete = findOneAsync(id).join();
                if (refereeToDelete.isEmpty()) {
                    logger.debug("No referee found with id {}, nothing to delete", id);
                    return Optional.empty();
                }

                // 3. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database.");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 4. Execute delete with OUTPUT in a single statement
                    // SQL Server specific syntax using OUTPUT
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "DELETE FROM referees OUTPUT DELETED.id, DELETED.name, DELETED.event_id, DELETED.username, DELETED.password WHERE id = ?")) {

                        preStmt.setString(1, id.toString());

                        try (ResultSet resultSet = preStmt.executeQuery()) {
                            if (resultSet.next()) {
                                // We already have the complete referee object from the previous findOneAsync call
                                logger.traceExit("Deleted referee with id {}", id);
                                return refereeToDelete;
                            } else {
                                logger.debug("No referee found with id {}, nothing to delete", id);
                                return Optional.empty();
                            }
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
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Iterable<Referee>> findByEventIdAsync(UUID eventId) {
        logger.traceEntry("Finding referees for event with id {}", eventId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Validate event ID
                if (eventId == null) {
                    logger.error("Attempted to find referees with null event id");
                    throw new ValidationException("Event id cannot be null");
                }

                // 2. Connect to the database
                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    // 3. Query referees by event ID
                    List<Referee> referees = new ArrayList<>();
                    try (PreparedStatement preStmt = connection.prepareStatement(
                            "SELECT r.id as referee_id, r.name as referee_name, r.username, r.password, " +
                                    "e.id as event_id, e.name as event_name " +
                                    "FROM referees r " +
                                    "JOIN events e ON r.event_id = e.id " +
                                    "WHERE e.id = ?")) {

                        preStmt.setString(1, eventId.toString());

                        try (ResultSet result = preStmt.executeQuery()) {
                            while (result.next()) {
                                // Get event data (will be the same for all rows)
                                String eventName = result.getString("event_name");
                                Event event = new Event(eventId, eventName);

                                // Get referee data
                                UUID refereeId = UUID.fromString(result.getString("referee_id"));
                                String refereeName = result.getString("referee_name");
                                String username = result.getString("username");
                                String password = result.getString("password");

                                // Create and add referee
                                Referee referee = new Referee(refereeId, refereeName, event, username, password);
                                referees.add(referee);
                            }
                        }
                    }

                    logger.info("Found {} referees for event {}", referees.size(), eventId);
                    return referees;

                } catch (SQLException e) {
                    logger.error("Database error while finding referees by event id: {}", e.getMessage(), e);
                    throw new RepositoryException("Database error: " + e.getMessage(), e);
                }
            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            }
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Optional<Referee>> findByUsernameAsync(String username) {
        logger.traceEntry("Finding referee with username {}", username);

        return CompletableFuture.supplyAsync(() -> {
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
        }, ThreadPoolManager.getRepositoryExecutor());
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