package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.exceptions.RepositoryException;
import org.example.model.Result;
import org.example.model.Event;
import org.example.model.Participant;
import org.example.utils.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class SQLRepositoryResult implements IRepositoryResult {

    private JdbcUtils dbUtils;
    private SQLRepositoryParticipant participantRepository;
    private SQLRepositoryEvent eventRepository;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryResult(Properties props) {
        logger.info("Initializing SQLRepositoryResult with properties: {}", props);
        dbUtils = new JdbcUtils(props);
        participantRepository = new SQLRepositoryParticipant(props);
        eventRepository = new SQLRepositoryEvent(props);
    }

    @Override
    public Optional<Result> findOne(Long id) {
        logger.traceEntry("Finding result with id {}", id);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM results WHERE id = ?")) {
            preStmt.setLong(1, id);
            try (ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    Long participantId = result.getLong("participant_id");
                    Long eventId = result.getLong("event_id");
                    int points = result.getInt("points");

                    // Get the associated Participant and Event using their repositories
                    Optional<Participant> participantOpt = participantRepository.findOne(participantId);
                    Optional<Event> eventOpt = eventRepository.findOne(eventId);

                    if (participantOpt.isEmpty()) {
                        logger.error("Could not find Participant with id {} for Result {}", participantId, id);
                        return Optional.empty();
                    }

                    if (eventOpt.isEmpty()) {
                        logger.error("Could not find Event with id {} for Result {}", eventId, id);
                        return Optional.empty();
                    }

                    Result resultObj = new Result(id, eventOpt.get(), participantOpt.get(), points);
                    logger.traceExit("Found result: {}", resultObj);
                    return Optional.of(resultObj);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding result with id {}: {}", id, e);
        }
        logger.traceExit("No result found with id {}", id);
        return Optional.empty();
    }

    @Override
    public Iterable<Result> findAll() {
        logger.traceEntry("Finding all results");
        Connection con = dbUtils.getConnection();
        List<Result> results = new ArrayList<>();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM results");
             ResultSet result = preStmt.executeQuery()) {

            while (result.next()) {
                Long id = result.getLong("id");
                Long participantId = result.getLong("participant_id");
                Long eventId = result.getLong("event_id");
                int points = result.getInt("points");

                // Get the associated Participant and Event using their repositories
                Optional<Participant> participantOpt = participantRepository.findOne(participantId);
                Optional<Event> eventOpt = eventRepository.findOne(eventId);

                if (participantOpt.isEmpty()) {
                    logger.error("Could not find Participant with id {} for Result {}", participantId, id);
                    continue;
                }

                if (eventOpt.isEmpty()) {
                    logger.error("Could not find Event with id {} for Result {}", eventId, id);
                    continue;
                }

                Result resultObj = new Result(id, eventOpt.get(), participantOpt.get(), points);
                results.add(resultObj);
            }
        } catch (SQLException e) {
            logger.error("Error finding all results: {}", e);
        }
        logger.traceExit("Found {} results", results.size());
        return results;
    }

    @Override
    public Optional<Result> add(Result result) {
        logger.traceEntry("Adding result {}", result);

        // Verify that Participant and Event exist in their repositories
        Participant participant = result.getParticipant();
        Event event = result.getEvent();

        Optional<Participant> existingParticipant = participantRepository.findOne(participant.getId());
        if (existingParticipant.isEmpty()) {
            logger.error("Cannot add result: Participant with id {} does not exist", participant.getId());
            return Optional.empty();
        }

        Optional<Event> existingEvent = eventRepository.findOne(event.getId());
        if (existingEvent.isEmpty()) {
            logger.error("Cannot add result: Event with id {} does not exist", event.getId());
            return Optional.empty();
        }

        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "INSERT INTO results (participant_id, event_id, points) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            preStmt.setLong(1, participant.getId());
            preStmt.setLong(2, event.getId());
            preStmt.setInt(3, result.getPoints());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows == 0) {
                logger.error("Creating result failed, no rows affected.");
                return Optional.empty();
            }

            try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    // Create a new Result with the generated id
                    Result savedResult = new Result(id, event, participant, result.getPoints());
                    logger.traceExit("Saved result with id {}", id);
                    return Optional.of(savedResult);
                } else {
                    logger.error("Creating result failed, no ID obtained.");
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving result {}: {}", result, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Result> update(Result result) {
        logger.traceEntry("Updating result {}", result);

        // Verify that Participant and Event exist in their repositories
        Participant participant = result.getParticipant();
        Event event = result.getEvent();

        Optional<Participant> existingParticipant = participantRepository.findOne(participant.getId());
        if (existingParticipant.isEmpty()) {
            logger.error("Cannot update result: Participant with id {} does not exist", participant.getId());
            return Optional.empty();
        }

        Optional<Event> existingEvent = eventRepository.findOne(event.getId());
        if (existingEvent.isEmpty()) {
            logger.error("Cannot update result: Event with id {} does not exist", event.getId());
            return Optional.empty();
        }

        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "UPDATE results SET participant_id = ?, event_id = ?, points = ? WHERE id = ?")) {

            preStmt.setLong(1, participant.getId());
            preStmt.setLong(2, event.getId());
            preStmt.setInt(3, result.getPoints());
            preStmt.setLong(4, result.getId());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Updated result with id {}", result.getId());
                return Optional.of(result);
            }
        } catch (SQLException e) {
            logger.error("Error updating result {}: {}", result, e);
        }
        logger.traceExit("No result updated with id {}", result.getId());
        return Optional.empty();
    }

    @Override
    public Optional<Result> delete(Long id) {
        logger.traceEntry("Deleting result with id {}", id);
        Optional<Result> result = findOne(id);
        if (result.isEmpty()) {
            logger.traceExit("No result found with id {}, nothing to delete", id);
            return Optional.empty();
        }

        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("DELETE FROM results WHERE id = ?")) {
            preStmt.setLong(1, id);

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Deleted result with id {}", id);
                return result;
            }
        } catch (SQLException e) {
            logger.error("Error deleting result with id {}: {}", id, e);
        }
        logger.traceExit("No result deleted with id {}", id);
        return Optional.empty();
    }


    public Optional<Result> findByParticipantAndEvent(Long participantId, Long eventId) {
        logger.traceEntry("Finding result for participant id {} and event id {}", participantId, eventId);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "SELECT * FROM results WHERE participant_id = ? AND event_id = ?")) {
            preStmt.setLong(1, participantId);
            preStmt.setLong(2, eventId);
            try (ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    Long id = result.getLong("id");
                    int points = result.getInt("points");

                    // Get the associated Participant and Event using their repositories
                    Optional<Participant> participantOpt = participantRepository.findOne(participantId);
                    Optional<Event> eventOpt = eventRepository.findOne(eventId);

                    if (participantOpt.isEmpty()) {
                        logger.error("Could not find Participant with id {} for Result", participantId);
                        return Optional.empty();
                    }

                    if (eventOpt.isEmpty()) {
                        logger.error("Could not find Event with id {} for Result", eventId);
                        return Optional.empty();
                    }

                    Result resultObj = new Result(id, eventOpt.get(), participantOpt.get(), points);
                    logger.traceExit("Found result: {}", resultObj);
                    return Optional.of(resultObj);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding result for participant id {} and event id {}: {}",
                    participantId, eventId, e);
        }
        logger.traceExit();
        return Optional.empty();
    }
}