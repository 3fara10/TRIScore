package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Participant;
import org.example.utils.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class SQLRepositoryParticipant implements IRepositoryParticipant {

    private JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryParticipant(Properties props) {
        logger.info("Initializing SQLRepositoryParticipant with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Participant> findOne(Long id) {
        logger.traceEntry("Finding participant with id {}", id);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM participants WHERE id = ?")) {
            preStmt.setLong(1, id);
            try (ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    String name = result.getString("name");

                    Participant participant = new Participant(id, name);
                    logger.traceExit("Found participant: {}", participant);
                    return Optional.of(participant);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding participant with id {}: {}", id, e);
        }
        logger.traceExit("No participant found with id {}", id);
        return Optional.empty();
    }

    @Override
    public Iterable<Participant> findAll() {
        logger.traceEntry("Finding all participants");
        Connection con = dbUtils.getConnection();
        List<Participant> participants = new ArrayList<>();
        try (PreparedStatement preStmt = con.prepareStatement("SELECT * FROM participants");
             ResultSet result = preStmt.executeQuery()) {

            while (result.next()) {
                Long id = result.getLong("id");
                String name = result.getString("name");

                Participant participant = new Participant(id, name);
                participants.add(participant);
            }
        } catch (SQLException e) {
            logger.error("Error finding all participants: {}", e);
        }
        logger.traceExit("Found {} participants", participants.size());
        return participants;
    }

    @Override
    public Optional<Participant> add(Participant participant) {
        logger.traceEntry("Saving participant {}", participant);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "INSERT INTO participants (name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {

            preStmt.setString(1, participant.getName());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows == 0) {
                logger.error("Creating participant failed, no rows affected.");
                return Optional.empty();
            }

            try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    Participant savedParticipant = new Participant(id, participant.getName());
                    logger.traceExit("Saved participant with id {}", id);
                    return Optional.of(savedParticipant);
                } else {
                    logger.error("Creating participant failed, no ID obtained.");
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving participant {}: {}", participant, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Participant> update(Participant participant) {
        logger.traceEntry("Updating participant {}", participant);
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement(
                "UPDATE participants SET name = ? WHERE id = ?")) {

            preStmt.setString(1, participant.getName());
            preStmt.setLong(2, participant.getId());

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Updated participant with id {}", participant.getId());
                return Optional.of(participant);
            }
        } catch (SQLException e) {
            logger.error("Error updating participant {}: {}", participant, e);
        }
        logger.traceExit("No participant updated with id {}", participant.getId());
        return Optional.empty();
    }

    @Override
    public Optional<Participant> delete(Long id) {
        logger.traceEntry("Deleting participant with id {}", id);
        Optional<Participant> participant = findOne(id);
        if (participant.isEmpty()) {
            logger.traceExit("No participant found with id {}, nothing to delete", id);
            return Optional.empty();
        }
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("DELETE FROM participants WHERE id = ?")) {
            preStmt.setLong(1, id);

            int affectedRows = preStmt.executeUpdate();
            if (affectedRows > 0) {
                logger.traceExit("Deleted participant with id {}", id);
                return participant;
            }
        } catch (SQLException e) {
            logger.error("Error deleting participant with id {}: {}", id, e);
        }
        logger.traceExit("No participant deleted with id {}", id);
        return Optional.empty();
    }
}