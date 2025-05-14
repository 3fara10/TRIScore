package org.example.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.model.Referee;
import org.example.utils.HibernateUtils;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Repository
public class HibernateRepositoryReferee implements IRepositoryReferee {

    private final HibernateUtils hibernateUtils;
    private static final Logger logger = LogManager.getLogger(HibernateRepositoryReferee.class);

    @Autowired
    public HibernateRepositoryReferee(Properties props) {
        logger.info("Initializing HibernateRepositoryReferee with properties: {}", props);
        hibernateUtils = new HibernateUtils(props);
    }

    @Override
    public Optional<Referee> findOne(UUID id) {
        logger.traceEntry("Finding referee with id {}", id);

        if (id == null) {
            logger.error("Attempted to find referee with null id");
            throw new ValidationException("Attempted to find referee with null id");
        }

        try {
            Referee referee = hibernateUtils.getSessionFactory().fromSession(session -> session.find(Referee.class, id));
            logger.traceExit("Found referee: {}", referee);
            return Optional.ofNullable(referee);
        } catch (Exception e) {
            logger.error("Error finding referee with id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Error finding referee: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Referee> findAll() {
        logger.traceEntry("Finding all referees");

        try {
            List<Referee> referees = hibernateUtils.getSessionFactory().fromSession(session -> {
                Query<Referee> query = session.createQuery("FROM Referee", Referee.class);
                return query.list();
            });

            logger.info("Found {} referees", referees.size());
            logger.traceExit();
            return referees;
        } catch (Exception e) {
            logger.error("Error finding all referees: {}", e.getMessage(), e);
            throw new RepositoryException("Error finding all referees: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> add(Referee referee) {
        logger.traceEntry("Saving referee {}", referee);

        if (referee == null) {
            logger.error("Attempted to add null referee");
            throw new ValidationException("Attempted to add null referee");
        }

        try {
            Referee refereeToSave = new Referee();
            if (refereeToSave.getId() == null) {
                refereeToSave = generateReferee(referee);
            } else {
                refereeToSave = referee;
            }

            if (refereeToSave.getEvent() == null || refereeToSave.getEvent().getId() == null) {
                logger.error("Attempted to add referee with null or invalid event");
                throw new ValidationException("Referee must have a valid event with non-empty ID");
            }

            Referee finalRefereeToSave1 = refereeToSave;
            Event event = hibernateUtils.getSessionFactory().fromSession(session ->
                    session.find(Event.class, finalRefereeToSave1.getEvent().getId()));

            if (event == null) {
                logger.error("Attempted to add referee with non-existent event: {}", refereeToSave.getEvent().getId());
                throw new ValidationException("Event with ID " + refereeToSave.getEvent().getId() + " does not exist");
            }

            Referee finalRefereeToSave = refereeToSave;
            hibernateUtils.getSessionFactory().inTransaction(session -> {
                session.persist(finalRefereeToSave);
                logger.info("Successfully saved referee with id {}", finalRefereeToSave.getId());
            });

            logger.traceExit();
            return Optional.of(refereeToSave);
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error adding referee: {}", e.getMessage(), e);
            throw new RepositoryException("Error adding referee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> update(Referee referee, Referee newReferee) {
        logger.traceEntry("Updating referee {} with new values from {}", referee, newReferee);

        if (referee == null) {
            logger.error("Attempted to update null referee");
            throw new ValidationException("Attempted to update null referee");
        }

        if (referee.getId() == null) {
            logger.error("Attempted to update referee with null id");
            throw new ValidationException("Cannot update referee with null id");
        }

        try {
            final UUID refereeId = referee.getId();
            final String newName = newReferee.getName();
            final Event newEvent = newReferee.getEvent();
            final String newUsername = newReferee.getUsername();
            final String newPassword = newReferee.getPassword();

            // Verifică dacă noul eveniment există în baza de date (dacă este diferit)
            if (newEvent != null && newEvent.getId() != null &&
                    (referee.getEvent() == null || !referee.getEvent().getId().equals(newEvent.getId()))) {
                Event event = hibernateUtils.getSessionFactory().fromSession(session ->
                        session.find(Event.class, newEvent.getId()));

                if (event == null) {
                    logger.error("Attempted to update referee with non-existent event: {}", newEvent.getId());
                    throw new ValidationException("Event with ID " + newEvent.getId() + " does not exist");
                }
            }

            Referee updatedReferee = hibernateUtils.getSessionFactory().fromTransaction(session -> {
                Referee existingReferee = session.get(Referee.class, refereeId);
                if (existingReferee == null) {
                    logger.error("Referee with id {} not found", refereeId);
                    return null;
                }

                existingReferee.setName(newName);
                if (newEvent != null) {
                    existingReferee.setEvent(newEvent);
                }
                existingReferee.setUsername(newUsername);
                existingReferee.setPassword(newPassword);

                session.merge(existingReferee);
                logger.info("Successfully updated referee with id {}", refereeId);
                return existingReferee;
            });

            logger.traceExit();
            return Optional.ofNullable(updatedReferee);
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error updating referee: {}", e.getMessage(), e);
            throw new RepositoryException("Error updating referee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> delete(UUID id) {
        logger.traceEntry("Deleting referee with id {}", id);

        if (id == null) {
            logger.error("Attempted to delete referee with null id");
            throw new ValidationException("Attempted to delete referee with null id");
        }

        try {
            return hibernateUtils.getSessionFactory().fromTransaction(session -> {
                Referee refereeToDelete = session.get(Referee.class, id);
                if (refereeToDelete == null) {
                    logger.warn("Referee with id {} not found for deletion", id);
                    return Optional.empty();
                }

                // Salvăm o copie a arbitrului înainte de ștergere
                Referee deletedReferee = new Referee(
                        refereeToDelete.getId(),
                        refereeToDelete.getName(),
                        refereeToDelete.getEvent(),
                        refereeToDelete.getUsername(),
                        refereeToDelete.getPassword()
                );

                session.remove(refereeToDelete);
                logger.info("Successfully deleted referee with id {}", id);

                return Optional.of(deletedReferee);
            });
        } catch (Exception e) {
            logger.error("Error deleting referee with id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Error deleting referee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Referee> findByUsername(String username) {
        logger.traceEntry("Finding referee with username {}", username);

        try {
            if (username == null || username.trim().isEmpty()) {
                logger.error("Attempted to find referee with null or empty username");
                throw new ValidationException("Username cannot be null or empty");
            }

            Referee referee = hibernateUtils.getSessionFactory().fromSession(session -> {
                Query<Referee> query = session.createQuery(
                        "FROM Referee r WHERE r.username = :username",
                        Referee.class
                );
                query.setParameter("username", username);
                return query.uniqueResult();
            });

            logger.traceExit("Found referee: {}", referee);
            return Optional.ofNullable(referee);
        } catch (ValidationException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error finding referee by username: {}", e.getMessage(), e);
            throw new RepositoryException("Error finding referee by username: " + e.getMessage(), e);
        }
    }

    private Referee generateReferee(Referee referee) {
        UUID newId = UUID.randomUUID();
        logger.info("Generated new referee with id {}", newId);

        // Verifică dacă ID-ul există deja
        if (findOne(newId).isPresent()) {
            logger.warn("UUID collision detected: {}", newId);
            newId = UUID.randomUUID();
            logger.info("Generated another UUID: {}", newId);
        }

        return new Referee(
                newId,
                referee.getName(),
                referee.getEvent(),
                referee.getUsername(),
                referee.getPassword()
        );
    }
}