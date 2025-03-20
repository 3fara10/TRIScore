package org.example.repository;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.ValidationException;
import org.example.model.Event;
import org.example.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.List;


public class SQLRepositoryEvent implements IRepositoryEvent {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryEvent(Properties props) {
        logger.info("Initializing SQLRepositoryEvent with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Event> findOne(UUID id) {
        logger.traceEntry("Finding event with id {}", id);

        try {
            //1.Verify the integrity of the parameter(double-check)
            if (id == null) {
                logger.error("Attempted to find event with null id");
                throw new ValidationException("Attempted to find event with null id");
            }

            //2.Trying to connect to the database
            try(Connection connection=dbUtils.getConnection()) {

                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                //3.Trying to get the right data
                try (PreparedStatement preStmt = connection.prepareStatement("SELECT * FROM events WHERE id = ?")) {
                    preStmt.setString(1, id.toString());
                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            String name = result.getString("name");
                            Event event = new Event(id, name);
                            logger.traceExit("Found event: {}", event);
                            return Optional.of(event);
                        }else {
                            logger.traceExit("No event found with id {}", id);
                            return Optional.empty();
                        }
                    }
                }

            } catch (SQLException e) {
                logger.error("Database error while finding event: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        }catch(ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Event> findAll() {
        logger.traceEntry("Finding all events");

        //1.Trying to connect to the database
        try(Connection connection=dbUtils.getConnection()) {
            if (connection == null) {
                logger.error("Couldn't connect to the database");
                throw new RepositoryException("Couldn't connect to the database.");
            }

            //2.Trying to get the right data
            List<Event> events = new ArrayList<>();
            try (PreparedStatement preStmt = connection.prepareStatement("SELECT * FROM events");
                 ResultSet result = preStmt.executeQuery()) {
                while (result.next()) {
                    UUID id = UUID.fromString(result.getString("id"));
                    String name = result.getString("name");
                    Event event = new Event(id, name);
                    events.add(event);
                }
            }
            logger.traceExit("Found {} events", events.size());
            return events;

        } catch (SQLException e) {
            logger.error("Database error while finding all events: {}", e.getMessage(), e);
            throw new RepositoryException("Database error: " + e.getMessage(), e);
        }
    }



    @Override
    public Optional<Event> add(Event event) {
        logger.traceEntry("Saving event {}", event);

        try {
            //1.Verify the integrity of the parameter(double-check)
            if (event == null) {
                logger.error("Attempted to add null event");
                throw new ValidationException("Attempted to add null event");
            }

            //2.Verify if the event has an id
            if (event.getId() == null) {
                event = generateEvent(event);
            }

            //3.Try to connect to the database
            try(Connection connection=dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                //4.Try to execute the query
                try (PreparedStatement preStmt = connection.prepareStatement(
                        "INSERT INTO events (id, name) VALUES (?, ?)")) {
                    preStmt.setString(1, event.getId().toString());
                    preStmt.setString(2, event.getName());

                    int rowsAffected = preStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully saved event with id {}", event.getId());
                        return Optional.of(event);
                    } else {
                        logger.error("Failed to insert event, no rows affected.");
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while adding event: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        }catch(ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }


    @Override
    public Optional<Event> update(Event event){
        logger.traceEntry("Updating event {}", event);

        try {
            //1.Verify the integrity of the parameter(double-check)
            if (event == null) {
                logger.error("Attempted to update null event");
                throw new ValidationException("Attempted to update null event");
            }

            //2.Verify if the event has an id
            if (event.getId() == null) {
                logger.error("Attempted to update event with null id");
                throw new ValidationException("Cannot update event with null id");
            }

            //3.Try to connect to the database
            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                //4.Try to execute the query
                try (PreparedStatement preStmt = connection.prepareStatement(
                        "UPDATE events SET name = ? WHERE id = ?")) {

                    preStmt.setString(1, event.getName());
                    preStmt.setString(2, event.getId().toString());

                    int rowsAffected = preStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully updated event with id {}", event.getId());
                        return Optional.of(event);
                    } else {
                        logger.error("Failed to update event with id {}, no rows affected.", event.getId());
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while updating event: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Event> delete(UUID id) {
        logger.traceEntry("Deleting event with id {}", id);

        try {
            // Validate the ID
            if (id == null) {
                logger.error("Attempted to delete event with null id");
                throw new ValidationException("Event id cannot be null");
            }

            // Find the event first to return it after deletion
            Optional<Event> eventToDelete = findOne(id);
            if (eventToDelete.isEmpty()) {
                logger.traceExit("No event found with id {}, nothing to delete", id);
                return Optional.empty();
            }

            // Connect to the database and delete the event
            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                try (PreparedStatement preStmt = connection.prepareStatement("DELETE FROM events WHERE id = ?")) {
                    preStmt.setString(1, id.toString());

                    int affectedRows = preStmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.traceExit("Deleted event with id {}", id);
                        return eventToDelete;
                    } else {
                        logger.warn("Inconsistent state: event with id {} was found but could not be deleted", id);
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while deleting event: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    private Event generateEvent(Event event) {
        event.setId(UUID.randomUUID());
        logger.info("Created new event with id {}", event.getId());

        //Based on "the probability of generating two identical UUIDs consecutively is astronomically low"
        if(this.findOne(event.getId()).isPresent()){
            logger.warn("UUID collision detected: {}", event.getId());
            event.setId(UUID.randomUUID());
        }
        return event;
    }

}

