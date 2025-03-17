package org.example.repository;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.example.model.Event;
import org.example.utils.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class SQLRepositoryEvent implements IRepositoryEvent {

    private JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryEvent(Properties props) {
        logger.info("Initializing SQLRepositoryEvent with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Event> findOne(Long id) {
        logger.traceEntry("Finding event with id {}", id);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM events WHERE id = ?")) {
            preStmt.setLong(1, id);
            try (ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    String name = result.getString("name");
                    Event event = new Event(id, name);
                    logger.traceExit("Found event: {}", event);
                    return Optional.of(event);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding event with id {}: {}", id, e);
        }
        logger.traceExit("No event found with id {}", id);
        return Optional.empty();
    }

    @Override
    public Iterable<Event> findAll() {
        logger.traceEntry("Finding all events");
        Connection con = dbUtils.getConnection();
        List<Event> events = new ArrayList<>();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM events");
             ResultSet result = preStmt.executeQuery()) {

            while (result.next()) {
                Long id = result.getLong("id");
                String name = result.getString("name");

                Event event = new Event(id, name);
                events.add(event);
            }
        } catch (SQLException e) {
            logger.error("Error finding all events: {}", e);
        }
        logger.traceExit("Found {} events", events.size());
        return events;
    }

    @Override
    public Optional<Event> add(Event event) {
        logger.traceEntry("Saving event {}", event);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "INSERT INTO events (name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {

            preStmt.setString(1, event.getName());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows == 0) {
                logger.error("Creating event failed, no rows affected.");
                return Optional.empty();
            }

            try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    // Create a new Event with the generated id
                    Event savedEvent = new Event(id, event.getName());
                    logger.traceExit("Saved event with id {}", id);
                    return Optional.of(savedEvent);
                } else {
                    logger.error("Creating event failed, no ID obtained.");
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving event {}: {}", event, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Event> update(Event event) {
        logger.traceEntry("Updating event {}", event);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "UPDATE events SET name = ? WHERE id = ?")) {

            preStmt.setString(1, event.getName());
            preStmt.setLong(2, event.getId());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Updated event with id {}", event.getId());
                return Optional.of(event);
            }
        } catch (SQLException e) {
            logger.error("Error updating event {}: {}", event, e);
        }
        logger.traceExit("No event updated with id {}", event.getId());
        return Optional.empty();
    }

    @Override
    public Optional<Event> delete(Long id) {
        logger.traceEntry("Deleting event with id {}", id);
        Optional<Event> event = findOne(id);
        if (event.isEmpty()) {
            logger.traceExit("No event found with id {}, nothing to delete", id);
            return Optional.empty();
        }

        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("DELETE FROM events WHERE id = ?")) {
            preStmt.setLong(1, id);

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Deleted event with id {}", id);
                return event;
            }
        } catch (SQLException e) {
            logger.error("Error deleting event with id {}: {}", id, e);
        }
        logger.traceExit("No event deleted with id {}", id);
        return Optional.empty();
    }
}

