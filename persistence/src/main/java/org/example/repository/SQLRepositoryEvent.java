package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class SQLRepositoryEvent implements IRepositoryEvent {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger(SQLRepositoryEvent.class);


    public SQLRepositoryEvent(Properties props) {
        logger.info("Initializing SQLRepositoryEvent with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Event> findOne(UUID id) {
        logger.traceEntry("Finding event with id {}", id);
        try {
            if (id == null) {
                logger.error("Attempted to find event with null id");
                throw new ValidationException("Attempted to find event with null id");
            }
            try (Connection connection = dbUtils.getConnection()) {

                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }
                try (PreparedStatement preStmt = connection.prepareStatement("SELECT * FROM events WHERE id = ?")) {
                    preStmt.setString(1, id.toString());
                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            String name = result.getString("name");
                            Event event = new Event(id, name);
                            logger.traceExit("Found event: {}", event);
                            return Optional.of(event);
                        } else {
                            logger.traceExit("No event found with id {}", id);
                            return Optional.empty();
                        }
                    }
                }

            } catch (SQLException e) {
                logger.error("Database error while finding event: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Event> findAll() {
        logger.traceEntry("Finding all events");

        try (Connection connection = dbUtils.getConnection()) {
            if (connection == null) {
                logger.error("Couldn't connect to the database");
                throw new RepositoryException("Couldn't connect to the database.");
            }

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
        if (event == null) {
            logger.error("Attempted to add null event");
            throw new ValidationException("Attempted to add null event");
        }

        try {
            Event eventToSave = event;

            if (eventToSave.getId() == null) {
                eventToSave=generateEvent(event);
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                try (PreparedStatement preStmt = connection.prepareStatement(
                        "INSERT INTO events (id, name) VALUES (?, ?)")) {
                    preStmt.setString(1, eventToSave.getId().toString());
                    preStmt.setString(2, eventToSave.getName());

                    int rowsAffected = preStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully saved event with id {}", eventToSave.getId());
                        return Optional.of(eventToSave);
                    } else {
                        logger.error("Failed to insert event, no rows affected.");
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while adding event: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }


    @Override
    public Optional<Event> update(Event event,Event newEvent){
        logger.traceEntry("Updating event {}", event);
        try {
            if (event == null) {
                logger.error("Attempted to update null event");
                throw new ValidationException("Attempted to update null event");
            }

            if (event.getId() == null) {
                logger.error("Attempted to update event with null id");
                throw new ValidationException("Cannot update event with null id");
            }

            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                try (PreparedStatement preStmt = connection.prepareStatement(
                        "UPDATE events SET name = ? WHERE id = ?")) {

                    preStmt.setString(1, newEvent.getName());
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
        throw new UnsupportedOperationException("This method is not implemented yet");
    }

    private Event generateEvent(Event event) {
        event.setId(UUID.randomUUID());
        logger.info("Created new event with id {event.GetId()}");
        Optional<Event> existingEvent = this.findOne(event.getId());
        if (existingEvent.isPresent())
        {
            logger.warn("GUID collision detected: {event.GetId()}");
            event.setId(UUID.randomUUID());
        }
        return event;
    }

}

