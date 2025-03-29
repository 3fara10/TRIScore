package org.example.repository;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.ValidationException;
import org.example.model.ParticipantResultDTO;
import org.example.utils.JdbcUtils;
import org.example.utils.ThreadPoolManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SQLRepositoryDTO implements IRepositoryDTO{

    private final JdbcUtils dbUtils;
    private static final Logger logger = LogManager.getLogger();

    public SQLRepositoryDTO(Properties props) {
        logger.info("Initializing SQLRepositoryDTO with properties: {}", props);
        dbUtils = new JdbcUtils(props);
    }

    @Override
    public CompletableFuture<Iterable<ParticipantResultDTO>> getAllParticipantsSortedByNameWithPointsAsync() {
        logger.debug("Getting all participants sorted by name with points directly from database");

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ParticipantResultDTO> results = new ArrayList<>();

                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    String sql = "SELECT p.id AS ParticipantId, p.name AS ParticipantName, " +
                            "COALESCE(SUM(r.points), 0) AS Points " +
                            "FROM participants p " +
                            "LEFT JOIN results r ON p.id = r.participant_id " +
                            "GROUP BY p.id, p.name " +
                            "ORDER BY p.name";

                    try (PreparedStatement preStmt = connection.prepareStatement(sql);
                         ResultSet resultSet = preStmt.executeQuery()) {

                        while (resultSet.next()) {
                            UUID participantId = UUID.fromString(resultSet.getString("ParticipantId"));
                            String participantName = resultSet.getString("ParticipantName");
                            int points = resultSet.getInt("Points");

                            ParticipantResultDTO dto = new ParticipantResultDTO();
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
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Iterable<ParticipantResultDTO>> getParticipantsWithResultsForEventAsync(UUID eventId) {
        logger.debug("Getting participants with results for event ID: {} directly from database", eventId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (eventId == null) {
                    logger.error("Attempted to get participants for an event with null id");
                    throw new ValidationException("Event id cannot be null");
                }

                List<ParticipantResultDTO> results = new ArrayList<>();

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

                                ParticipantResultDTO dto = new ParticipantResultDTO();
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
        }, ThreadPoolManager.getRepositoryExecutor());
    }

    @Override
    public CompletableFuture<Integer> getTotalPointsForParticipantAsync(UUID participantId) {
        logger.debug("Getting total points for participant ID: {} directly from database", participantId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (participantId == null) {
                    logger.error("Attempted to get points for participant with null id");
                    throw new ValidationException("Participant id cannot be null");
                }

                try (Connection connection = dbUtils.getConnection()) {
                    if (connection == null) {
                        logger.error("Couldn't connect to the database");
                        throw new RepositoryException("Couldn't connect to the database.");
                    }

                    String sql = "SELECT " +
                            "(SELECT COUNT(*) FROM participants WHERE id = ?) AS ParticipantExists, " +
                            "COALESCE((SELECT SUM(points) FROM results WHERE participant_id = ?), 0) AS TotalPoints";

                    try (PreparedStatement preStmt = connection.prepareStatement(sql)) {
                        preStmt.setString(1, participantId.toString());
                        preStmt.setString(2, participantId.toString());

                        try (ResultSet resultSet = preStmt.executeQuery()) {
                            if (resultSet.next()) {
                                int participantExists = resultSet.getInt("ParticipantExists");

                                if (participantExists == 0) {
                                    logger.error("Participant with ID {} not found", participantId);
                                    throw new ValidationException("Participant with ID " + participantId + " not found");
                                }

                                int totalPoints = resultSet.getInt("TotalPoints");
                                logger.debug("Total points for participant ID {}: {}", participantId, totalPoints);
                                return totalPoints;
                            } else {
                                logger.error("Unexpected error: query did not return any results");
                                throw new RuntimeException("Unexpected error when querying database");
                            }
                        }
                    }
                }
            } catch (ValidationException e) {
                logger.error("Validation error: {}", e.getMessage(), e);
                throw new RepositoryException("Validation error: " + e.getMessage(), e);
            } catch (SQLException e) {
                logger.error("Database error while getting total points: {}", e.getMessage(), e);
                throw new RepositoryException("Database error: " + e.getMessage(), e);
            }
        }, ThreadPoolManager.getRepositoryExecutor());
    }
}