package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Referee;
import org.example.model.Event;
import org.example.utils.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class SQLRepositoryReferee implements IRepositoryReferee {

    private JdbcUtils dbUtils;
    private SQLRepositoryEvent eventRepository;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryReferee(Properties props) {
        logger.info("Initializing SQLRepositoryReferee with properties: {}", props);
        dbUtils = new JdbcUtils(props);
        eventRepository = new SQLRepositoryEvent(props);
    }

    @Override
    public Optional<Referee> findOne(Long id) {
        logger.traceEntry("Finding referee with id {}", id);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM referees WHERE id = ?")) {
            preStmt.setLong(1, id);
            try (ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    String name = result.getString("name");
                    Long eventId = result.getLong("event_id");
                    String username = result.getString("username");
                    String password = result.getString("password");


                    Optional<Event> eventOpt = eventRepository.findOne(eventId);
                    if (eventOpt.isEmpty()) {
                        logger.error("Could not find Event with id {} for Referee {}", eventId, id);
                        return Optional.empty();
                    }

                    Referee referee = new Referee(id, name, eventOpt.get(), username, password);
                    logger.traceExit("Found referee: {}", referee);
                    return Optional.of(referee);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding referee with id {}: {}", id, e);
        }
        logger.traceExit("No referee found with id {}", id);
        return Optional.empty();
    }

    @Override
    public Iterable<Referee> findAll() {
        logger.traceEntry("Finding all referees");
        Connection con = dbUtils.getConnection();
        List<Referee> referees = new ArrayList<>();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM referees");
             ResultSet result = preStmt.executeQuery()) {

            while (result.next()) {
                Long id = result.getLong("id");
                String name = result.getString("name");
                Long eventId = result.getLong("event_id");
                String username = result.getString("username");
                String password = result.getString("password");

                // Get the associated Event using EventRepository
                Optional<Event> eventOpt = eventRepository.findOne(eventId);
                if (eventOpt.isEmpty()) {
                    logger.error("Could not find Event with id {} for Referee {}", eventId, id);
                    continue;
                }

                Referee referee = new Referee(id, name, eventOpt.get(), username, password);
                referees.add(referee);
            }
        } catch (SQLException e) {
            logger.error("Error finding all referees: {}", e);
        }
        logger.traceExit("Found {} referees", referees.size());
        return referees;
    }

    @Override
    public Optional<Referee> add(Referee referee) {
        logger.traceEntry("Adding referee {}", referee);
        Event event = referee.getEvent();
        Optional<Event> eventOpt = eventRepository.findOne(event.getId());
        if (eventOpt.isEmpty()) {
            logger.error("Cannot add referee: Event with id {} does not exist", event.getId());
            return Optional.empty();
        }

        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "INSERT INTO referees (name, event_id, username, password) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            preStmt.setString(1, referee.getName());
            preStmt.setLong(2, event.getId());
            preStmt.setString(3, referee.getUsername());
            preStmt.setString(4, referee.getPassword());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows == 0) {
                logger.error("Creating referee failed, no rows affected.");
                return Optional.empty();
            }

            try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    // Create a new Referee with the generated id
                    Referee savedReferee = new Referee(id, referee.getName(), event,
                            referee.getUsername(), referee.getPassword());
                    logger.traceExit("Saved referee with id {}", id);
                    return Optional.of(savedReferee);
                } else {
                    logger.error("Creating referee failed, no ID obtained.");
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving referee {}: {}", referee, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Referee> update(Referee referee) {
        logger.traceEntry("Updating referee {}", referee);

        // Ensure Event exists in the database
        Event event = referee.getEvent();
        Optional<Event> eventOpt = eventRepository.findOne(event.getId());
        if (eventOpt.isEmpty()) {
            logger.error("Cannot update referee: Event with id {} does not exist", event.getId());
            return Optional.empty();
        }

        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "UPDATE referees SET name = ?, event_id = ?, username = ?, password = ? WHERE id = ?")) {

            preStmt.setString(1, referee.getName());
            preStmt.setLong(2, event.getId());
            preStmt.setString(3, referee.getUsername());
            preStmt.setString(4, referee.getPassword());
            preStmt.setLong(5, referee.getId());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Updated referee with id {}", referee.getId());
                return Optional.of(referee);
            }
        } catch (SQLException e) {
            logger.error("Error updating referee {}: {}", referee, e);
        }
        logger.traceExit("No referee updated with id {}", referee.getId());
        return Optional.empty();
    }

    @Override
    public Optional<Referee> delete(Long id) {
        logger.traceEntry("Deleting referee with id {}", id);
        Optional<Referee> referee = findOne(id);
        if (referee.isEmpty()) {
            logger.traceExit("No referee found with id {}, nothing to delete", id);
            return Optional.empty();
        }

        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("DELETE FROM referees WHERE id = ?")) {
            preStmt.setLong(1, id);

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Deleted referee with id {}", id);
                return referee;
            }
        } catch (SQLException e) {
            logger.error("Error deleting referee with id {}: {}", id, e);
        }
        logger.traceExit("No referee deleted with id {}", id);
        return Optional.empty();
    }

}