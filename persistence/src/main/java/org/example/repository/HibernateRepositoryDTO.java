package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.ParticipantResult;
import org.example.utils.HibernateUtils;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Repository
public class HibernateRepositoryDTO implements IRepositoryDTO {

    private final HibernateUtils hibernateUtils;
    private static final Logger logger = LogManager.getLogger(HibernateRepositoryDTO.class);

    @Autowired
    public HibernateRepositoryDTO(Properties props) {
        logger.info("Initializing HibernateRepositoryDTO with properties: {}", props);
        hibernateUtils = new HibernateUtils(props);
    }

    @Override
    public Iterable<ParticipantResult> getAllParticipantsSortedByNameWithTotalPoints() {
        logger.debug("Getting all participants sorted by name with total points");

        try {
            return hibernateUtils.getSessionFactory().fromSession(session -> {
                // Query nativ pentru a obține participanții cu punctele totale
                String sql = "SELECT p.id AS participantId, p.name AS participantName, " +
                        "COALESCE(SUM(r.points), 0) AS points " +
                        "FROM participants p " +
                        "LEFT JOIN results r ON p.id = r.participant_id " +
                        "GROUP BY p.id, p.name " +
                        "ORDER BY p.name";

                // Creăm și executăm query-ul nativ
                @SuppressWarnings("unchecked")
                NativeQuery<Object[]> query = session.createNativeQuery(sql);
                List<Object[]> resultList = query.list();

                // Transformăm rezultatele în obiecte ParticipantResult
                List<ParticipantResult> participantResults = new ArrayList<>();
                for (Object[] row : resultList) {
                    UUID participantId = UUID.fromString((String) row[0]);
                    String participantName = (String) row[1];
                    int points = 0;
                    if (row[2] != null) {
                        if (row[2] instanceof Long) {
                            points = ((Long) row[2]).intValue();
                        } else if (row[2] instanceof Integer) {
                            points = (Integer) row[2];
                        } else if (row[2] instanceof BigDecimal) {
                            points = ((BigDecimal) row[2]).intValue();
                        }
                    }

                    ParticipantResult participantResult = new ParticipantResult(participantId, participantName, points);
                    participantResults.add(participantResult);
                }

                logger.debug("Found {} participants with points", participantResults.size());
                return participantResults;
            });
        } catch (Exception e) {
            logger.error("Error getting participants with points: {}", e.getMessage(), e);
            throw new RepositoryException("Error getting participants with points: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<ParticipantResult> getParticipantsWithResultsForEvent(UUID eventId) {
        logger.debug("Getting participants with results for event ID: {}", eventId);

        try {
            if (eventId == null) {
                logger.error("Attempted to get participants for an event with null id");
                throw new ValidationException("Event id cannot be null");
            }

            return hibernateUtils.getSessionFactory().fromSession(session -> {
                // Query nativ pentru a obține participanții cu rezultate pentru un eveniment specific
                String sql = "SELECT p.id AS participantId, p.name AS participantName, " +
                        "SUM(r.points) AS points " +
                        "FROM participants p " +
                        "JOIN results r ON p.id = r.participant_id " +
                        "WHERE r.event_id = :eventId " +
                        "GROUP BY p.id, p.name " +
                        "ORDER BY SUM(r.points) DESC";

                // Creăm și executăm query-ul nativ
                @SuppressWarnings("unchecked")
                NativeQuery<Object[]> query = session.createNativeQuery(sql);
                query.setParameter("eventId", eventId.toString());
                List<Object[]> resultList = query.list();

                // Transformăm rezultatele în obiecte ParticipantResult
                List<ParticipantResult> participantResults = new ArrayList<>();
                for (Object[] row : resultList) {
                    UUID participantId = UUID.fromString((String) row[0]);
                    String participantName = (String) row[1];
                    int points = 0;
                    if (row[2] != null) {
                        if (row[2] instanceof Long) {
                            points = ((Long) row[2]).intValue();
                        } else if (row[2] instanceof Integer) {
                            points = (Integer) row[2];
                        } else if (row[2] instanceof BigDecimal) {
                            points = ((BigDecimal) row[2]).intValue();
                        }
                    }

                    // Creăm și un obiect Event pentru a-l atașa la ParticipantResult
                    Event event = session.get(Event.class, eventId);

                    ParticipantResult participantResult = new ParticipantResult(participantId, participantName, points);
                    participantResult.setEvent(event);
                    participantResults.add(participantResult);
                }

                logger.debug("Found {} participants with results for event ID: {}", participantResults.size(), eventId);
                return participantResults;
            });
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw new RepositoryException("Validation error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error getting participants with results: {}", e.getMessage(), e);
            throw new RepositoryException("Error getting participants with results: " + e.getMessage(), e);
        }
    }
}