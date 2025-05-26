package org.example.repository;

import java.lang.UnsupportedOperationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.Participant;
import org.example.model.Result;
import org.example.utils.HibernateUtils;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Repository
public class HibernateRepositoryResult implements IRepositoryResult {

    private final HibernateUtils hibernateUtils;
    private static final Logger logger = LogManager.getLogger(HibernateRepositoryResult.class);

    @Autowired
    public HibernateRepositoryResult(Properties props) {
        logger.info("Initializing HibernateRepositoryResult with properties: {}", props);
        hibernateUtils = new HibernateUtils(props);
    }

    @Override
    public Optional<Result> findOne(UUID id) {
        logger.traceEntry("Finding result with id {}", id);
        if (id == null) {
            logger.error("Attempted to find result with null id");
            throw new ValidationException("Attempted to find result with null id");
        }

        try {
            Result result = hibernateUtils.getSessionFactory().fromSession(session -> session.find(Result.class, id));
            logger.traceExit("Found result: {}", result);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            logger.error("Error finding result with id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Error finding result: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Result> findAll() {
        logger.traceEntry("Finding all results");

        try {
            List<Result> results = hibernateUtils.getSessionFactory().fromSession(session -> {
                Query<Result> query = session.createQuery("FROM Result", Result.class);
                return query.list();
            });
            logger.info("Found {} results", results.size());
            logger.traceExit();
            return results;
        } catch (Exception e) {
            logger.error("Error finding all results: {}", e.getMessage(), e);
            throw new RepositoryException("Error finding all results: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> add(Result result) {
        logger.traceEntry("Saving result {}", result);

        if (result == null) {
            logger.error("Attempted to add null result");
            throw new ValidationException("Attempted to add null result");
        }

        if (result.getEvent() == null || result.getEvent().getId() == null) {
            logger.error("Attempted to add result with null or invalid event");
            throw new ValidationException("Result must have a valid event with non-empty ID");
        }

        if (result.getParticipant() == null || result.getParticipant().getId() == null) {
            logger.error("Attempted to add result with null or invalid participant");
            throw new ValidationException("Result must have a valid participant with non-empty ID");
        }

        try {
            Result resultToSave = result;
            if (resultToSave.getId() == null) {
                resultToSave = generateResult(result);
            }
             UUID eventId = result.getEvent().getId();
             UUID participantId = result.getParticipant().getId();
             Result new_event = resultToSave;

            hibernateUtils.getSessionFactory().inTransaction(session -> {
                Event event = session.get(Event.class, eventId);
                if (event == null) {
                    logger.error("Event with id {} not found", eventId);
                    throw new ValidationException("Event with id " + eventId + " not found");
                }

                Participant participant = session.get(Participant.class, participantId);
                if (participant == null) {
                    logger.error("Participant with id {} not found", participantId);
                    throw new ValidationException("Participant with id " + participantId + " not found");
                }
                new_event.setEvent(event);
                new_event.setParticipant(participant);
                session.persist(new_event);
                logger.info("Successfully saved result with id {}", new_event.getId());
            });

            logger.traceExit();
            return Optional.of(resultToSave);
        } catch (Exception e) {
            logger.error("Error adding result: {}", e.getMessage(), e);
            throw new RepositoryException("Error adding result: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> update(Result result, Result newResult) {
        logger.traceEntry("Updating result {} with new values from {}", result, newResult);

        if (result == null) {
            logger.error("Attempted to update null result");
            throw new ValidationException("Attempted to update null result");
        }

        if (result.getId() == null) {
            logger.error("Attempted to update result with null id");
            throw new ValidationException("Cannot update result with null id");
        }

        if (newResult.getEvent() == null || newResult.getEvent().getId() == null) {
            logger.error("Attempted to update result with null or invalid event");
            throw new ValidationException("Result must have a valid event with non-empty ID");
        }

        if (newResult.getParticipant() == null || newResult.getParticipant().getId() == null) {
            logger.error("Attempted to update result with null or invalid participant");
            throw new ValidationException("Result must have a valid participant with non-empty ID");
        }

        try {
            Result updatedResult = hibernateUtils.getSessionFactory().fromTransaction(session -> {
                Result existingResult = session.get(Result.class,  result.getId());
                if (existingResult == null) {
                    logger.error("Result with id {} not found",  result.getId());
                    return null;
                }

                Event event = session.get(Event.class, newResult.getEvent().getId());
                if (event == null) {
                    logger.error("Event with id {} not found", newResult.getEvent().getId());
                    throw new ValidationException("Event with id " + newResult.getEvent().getId() + " not found");
                }

                Participant participant = session.get(Participant.class, newResult.getParticipant().getId());
                if (participant == null) {
                    logger.error("Participant with id {} not found", newResult.getParticipant().getId());
                    throw new ValidationException("Participant with id " + newResult.getParticipant().getId() + " not found");
                }

                existingResult.setEvent(event);
                existingResult.setParticipant(participant);
                existingResult.setPoints(newResult.getPoints());
                session.merge(existingResult);
                logger.info("Successfully updated result with id {}", result.getId());
                return existingResult;
            });

            logger.traceExit();
            return Optional.ofNullable(updatedResult);
        } catch (Exception e) {
            logger.error("Error updating result: {}", e.getMessage(), e);
            throw new RepositoryException("Error updating result: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> delete(UUID id) {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }

    @Override
    public Optional<Result> findByEventAndParticipant(UUID eventId, UUID participantId) {
        logger.traceEntry("Finding result for event {} and participant {}", eventId, participantId);

        if (eventId == null) {
            logger.error("Attempted to find result with null event id");
            throw new ValidationException("Event id cannot be null");
        }

        if (participantId == null) {
            logger.error("Attempted to find result with null participant id");
            throw new ValidationException("Participant id cannot be null");
        }

        try {
            List<Result> results = hibernateUtils.getSessionFactory().fromSession(session -> {
                Query<Result> query = session.createQuery("FROM Result r WHERE r.event.id = :eventId AND r.participant.id = :participantId", Result.class);
                query.setParameter("eventId", eventId);
                query.setParameter("participantId", participantId);

                return query.getResultList();
            });

            if (results.isEmpty()) {
                logger.info("No result found for event {} and participant {}", eventId, participantId);
                return Optional.empty();
            } else {
                Result result = results.getFirst();
                logger.info("Found result with id {} for event {} and participant {}",
                        result.getId(), eventId, participantId);
                logger.traceExit();
                return Optional.of(result);
            }
        } catch (Exception e) {
            logger.error("Error finding result by event and participant: {}", e.getMessage(), e);
            throw new RepositoryException("Error finding result: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Result> addOrUpdateResult(UUID participantId, UUID eventId, int points) {
        logger.traceEntry("Adding or updating result for participant {} in event {} with {} points",
                participantId, eventId, points);

        if (participantId == null) {
            logger.error("Participant ID cannot be null");
            throw new ValidationException("Participant ID cannot be null");
        }

        if (eventId == null) {
            logger.error("Event ID cannot be null");
            throw new ValidationException("Event ID cannot be null");
        }

        if (points < 0) {
            logger.error("Points must be a positive number");
            throw new ValidationException("Points must be a positive number");
        }

        Optional<Result> existingResult = findByEventAndParticipant(eventId, participantId);

        if (existingResult.isPresent()) {
            Result result = existingResult.get();
            Result newResult = new Result(result.getId(), result.getEvent(), result.getParticipant(), result.getPoints() + points);

            return update(result, newResult);
        } else {
            try {
                Event event = hibernateUtils.getSessionFactory().fromSession(session -> session.get(Event.class, eventId));
                if (event == null) {
                    logger.error("Event with id {} not found", eventId);
                    throw new ValidationException("Event with id " + eventId + " not found");
                }

                Participant participant = hibernateUtils.getSessionFactory().fromSession(session -> session.get(Participant.class, participantId));
                if (participant == null) {
                    logger.error("Participant with id {} not found", participantId);
                    throw new ValidationException("Participant with id " + participantId + " not found");
                }

                Result newResult = new Result(event, participant, points);
                return add(newResult);
            } catch (Exception e) {
                logger.error("Error creating new result: {}", e.getMessage(), e);
                throw new RepositoryException("Error creating new result: " + e.getMessage(), e);
            }
        }
    }

    private Result generateResult(Result result) {
        Result newResult = new Result(UUID.randomUUID(), result.getEvent(), result.getParticipant(), result.getPoints());
        logger.info("Generated new result with id {}", newResult.getId());

        if (findOne(newResult.getId()).isPresent()) {
            logger.warn("UUID collision detected: {}", newResult.getId());
            newResult.setId(UUID.randomUUID());
            logger.info("Generated another UUID: {}", newResult.getId());
        }

        return newResult;
    }
}