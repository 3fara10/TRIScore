package org.example.repository;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.ValidationException;
import org.example.model.Participant;
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

public class SQLRepositoryParticipant implements IRepositoryParticipant {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryParticipant(Properties props) {
        logger.info("Initializing SQLRepositoryParticipant with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Optional<Participant> findOne(UUID id) {
        logger.traceEntry("Finding participant with id {}", id);

        try {
            //1.Verify the integrity of the parameter(double-check)
            if (id == null) {
                logger.error("Attempted to find participant with null id");
                throw new ValidationException("Attempted to find participant with null id");
            }

            //2.Trying to connect to the database
            try(Connection connection=dbUtils.getConnection()) {

                if (connection == null) {
                    logger.error("Couldn't connect to the database");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                //3.Trying to get the right data
                try (PreparedStatement preStmt = connection.prepareStatement("SELECT * FROM participants WHERE id = ?")) {
                    preStmt.setString(1, id.toString());
                    try (ResultSet result = preStmt.executeQuery()) {
                        if (result.next()) {
                            String name = result.getString("name");
                            Participant participant = new Participant(id, name);
                            logger.traceExit("Found participant: {}", participant);
                            return Optional.of(participant);
                        }else {
                            logger.traceExit("No participant found with id {}", id);
                            return Optional.empty();
                        }
                    }
                }

            } catch (SQLException e) {
                logger.error("Database error while finding participant: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        }catch(ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Participant> findAll() {
        logger.traceEntry("Finding all participants");

        //1.Trying to connect to the database
        try(Connection connection=dbUtils.getConnection()) {
            if (connection == null) {
                logger.error("Couldn't connect to the database");
                throw new RepositoryException("Couldn't connect to the database.");
            }

            //2.Trying to get the right data
            List<Participant> participants = new ArrayList<>();
            try (PreparedStatement preStmt = connection.prepareStatement("SELECT * FROM participants");
                 ResultSet result = preStmt.executeQuery()) {
                while (result.next()) {
                    UUID id = UUID.fromString(result.getString("id"));
                    String name = result.getString("name");
                    Participant participant = new Participant(id, name);
                    participants.add(participant);
                }
            }
            logger.traceExit("Found {} participants", participants.size());
            return participants;

        } catch (SQLException e) {
            logger.error("Database error while finding all participants: {}", e.getMessage(), e);
            throw new RepositoryException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> add(Participant participant) {
        logger.traceEntry("Saving participant {}", participant);

        try {
            //1.Verify the integrity of the parameter(double-check)
            if (participant == null) {
                logger.error("Attempted to add null participant");
                throw new ValidationException("Attempted to add null participant");
            }

            //2.Verify if the participant has an id
            if (participant.getId() == null) {
                participant = generateParticipant(participant);
            }

            //3.Try to connect to the database
            try(Connection connection=dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                //4.Try to execute the query
                try (PreparedStatement preStmt = connection.prepareStatement(
                        "INSERT INTO participants (id, name) VALUES (?, ?)")) {
                    preStmt.setString(1, participant.getId().toString());
                    preStmt.setString(2, participant.getName());

                    int rowsAffected = preStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully saved participant with id {}", participant.getId());
                        return Optional.of(participant);
                    } else {
                        logger.error("Failed to insert participant, no rows affected.");
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while adding participant: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        }catch(ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> update(Participant participant){
        logger.traceEntry("Updating participant {}", participant);

        try {
            //1.Verify the integrity of the parameter(double-check)
            if (participant == null) {
                logger.error("Attempted to update null participant");
                throw new ValidationException("Attempted to update null participant");
            }

            //2.Verify if the participant has an id
            if (participant.getId() == null) {
                logger.error("Attempted to update participant with null id");
                throw new ValidationException("Cannot update participant with null id");
            }

            //3.Try to connect to the database
            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                //4.Try to execute the query
                try (PreparedStatement preStmt = connection.prepareStatement(
                        "UPDATE participants SET name = ? WHERE id = ?")) {

                    preStmt.setString(1, participant.getName());
                    preStmt.setString(2, participant.getId().toString());

                    int rowsAffected = preStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.traceExit("Successfully updated participant with id {}", participant.getId());
                        return Optional.of(participant);
                    } else {
                        logger.error("Failed to update participant with id {}, no rows affected.", participant.getId());
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while updating participant: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> delete(UUID id) {
        logger.traceEntry("Deleting participant with id {}", id);

        try {
            // Validate the ID
            if (id == null) {
                logger.error("Attempted to delete participant with null id");
                throw new ValidationException("Participant id cannot be null");
            }

            // Find the participant first to return it after deletion
            Optional<Participant> participantToDelete = findOne(id);
            if (participantToDelete.isEmpty()) {
                logger.traceExit("No participant found with id {}, nothing to delete", id);
                return Optional.empty();
            }

            // Connect to the database and delete the participant
            try (Connection connection = dbUtils.getConnection()) {
                if (connection == null) {
                    logger.error("Couldn't connect to the database.");
                    throw new RepositoryException("Couldn't connect to the database.");
                }

                try (PreparedStatement preStmt = connection.prepareStatement("DELETE FROM participants WHERE id = ?")) {
                    preStmt.setString(1, id.toString());

                    int affectedRows = preStmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.traceExit("Deleted participant with id {}", id);
                        return participantToDelete;
                    } else {
                        logger.warn("Inconsistent state: participant with id {} was found but could not be deleted", id);
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.error("Database error while deleting participant: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        }
    }

    private Participant generateParticipant(Participant participant) {
        participant.setId(UUID.randomUUID());
        logger.info("Created new participant with id {}", participant.getId());

        //Based on "the probability of generating two identical UUIDs consecutively is astronomically low"
        if(this.findOne(participant.getId()).isPresent()){
            logger.warn("UUID collision detected: {}", participant.getId());
            participant.setId(UUID.randomUUID());
        }
        return participant;
    }
}