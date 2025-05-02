package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.ParticipantResult;
import org.example.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class SQLRepositoryDTO implements IRepositoryDTO {

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryDTO(Properties props) {
        logger.info("Initializing SQLRepositoryDTO with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public Iterable<ParticipantResult> getAllParticipantsSortedByNameWithTotalPoints() {
        logger.debug("Getting all participants sorted by name with points directly from database");

            try {
                List<ParticipantResult> results = new ArrayList<>();

                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    String sql = "SELECT p.id AS ParticipantId, p.name AS ParticipantName, " +
                            "COALESCE(SUM(r.points), 0) AS TotalPoints " +
                            "FROM participants p " +
                            "LEFT JOIN results r ON p.id = r.participant_id " +
                            "GROUP BY p.id, p.name " +
                            "ORDER BY p.name";

                    try (PreparedStatement preStmt = connection.prepareStatement(sql);
                         ResultSet resultSet = preStmt.executeQuery()) {

                        while (resultSet.next()) {
                            UUID participantId = UUID.fromString(resultSet.getString("ParticipantId"));
                            String participantName = resultSet.getString("ParticipantName");
                            int points = resultSet.getInt("TotalPoints");

                            ParticipantResult dto = new ParticipantResult();
                            dto.setParticipantID(participantId);
                            dto.setParticipantName(participantName);
                            dto.setPoints(points);

                            results.add(dto);
                        }
                    }
                }

                logger.debug("Found {} participants with points", results.size());
                return results;

            } catch (SQLException e) {
                logger.error("Database error while getting participants with points: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
    }

    @Override
    public Iterable<ParticipantResult> getParticipantsWithResultsForEvent(UUID eventId) {
        logger.debug("Getting participants with results for event ID: {} directly from database", eventId);

            try {
                if (eventId == null) {
                    logger.error("Attempted to get participants for an event with null id");
                    throw new ValidationException("Event id cannot be null");
                }

                List<ParticipantResult> results = new ArrayList<>();

                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    String sql = "SELECT p.id AS ParticipantId, p.name AS ParticipantName, " +
                            "SUM(r.points) AS Points " +
                            "FROM participants p " +
                            "JOIN results r ON p.id = r.participant_id " +
                            "WHERE r.event_id = ? " +
                            "GROUP BY p.id, p.name " +
                            "ORDER BY SUM(r.points) DESC";

                    try (PreparedStatement preStmt = connection.prepareStatement(sql)) {
                        preStmt.setString(1, eventId.toString());

                        try (ResultSet resultSet = preStmt.executeQuery()) {
                            while (resultSet.next()) {
                                UUID participantId = UUID.fromString(resultSet.getString("ParticipantId"));
                                String participantName = resultSet.getString("ParticipantName");
                                int points = resultSet.getInt("Points");

                                ParticipantResult dto = new ParticipantResult();
                                dto.setParticipantID(participantId);
                                dto.setParticipantName(participantName);
                                dto.setPoints(points);

                                results.add(dto);
                            }
                        }
                    }
                }

                logger.debug("Found {} participants with results for event ID: {}", results.size(), eventId);
                return results;

            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            } catch (SQLException e) {
                logger.error("Database error while getting participants with results: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
    }

}