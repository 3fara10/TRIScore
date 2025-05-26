package org.example.repository;

import java.lang.UnsupportedOperationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Participant;
import org.example.utils.HibernateUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
@Repository
public class HibernateRepositoryParticipant implements IRepositoryParticipant {

    private final HibernateUtils hibernateUtils;
    private static final Logger logger = LogManager.getLogger(HibernateRepositoryParticipant.class);

    @Autowired
    public HibernateRepositoryParticipant(Properties props) {
        logger.info("Initializing HibernateRepositoryParticipant with properties: {}", props);
        hibernateUtils = new HibernateUtils(props);
    }

    @Override
    public Optional<Participant> findOne(UUID id) {
        logger.traceEntry("Finding participant with id {}", id);

        if (id == null) {
            logger.error("Attempted to find participant with null id");
            throw new ValidationException("Attempted to find participant with null id");
        }

        try {
            Participant participant = hibernateUtils.getSessionFactory().fromSession(session -> session.find(Participant.class, id));
            logger.traceExit("Found participant: {}", participant);
            return Optional.ofNullable(participant);
        } catch (Exception e) {
            logger.error("Error finding participant with id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Error finding participant: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Participant> findAll() {
        logger.traceEntry("Finding all participants");

        try {
            List<Participant> participants = hibernateUtils.getSessionFactory().fromSession(session -> {
                Query<Participant> query = session.createQuery("FROM Participant", Participant.class);
                return query.list();
            });

            logger.info("Found {} participants", participants.size());
            logger.traceExit();
            return participants;
        } catch (Exception e) {
            logger.error("Error finding all participants: {}", e.getMessage(), e);
            throw new RepositoryException("Error finding all participants: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> add(Participant participant) {
        logger.traceEntry("Saving participant {}", participant);

        if (participant == null) {
            logger.error("Attempted to add null participant");
            throw new ValidationException("Attempted to add null participant");
        }
        try {
            Participant participantToSave = participant;
            if (participantToSave.getId() == null) {
                participantToSave = generateParticipant(participant);
            }
            Participant finalParticipantToSave = participantToSave;
            hibernateUtils.getSessionFactory().inTransaction(session -> {
                session.persist(finalParticipantToSave);
                logger.info("Successfully saved participant with id {}", finalParticipantToSave.getId());
            });

            logger.traceExit();
            return Optional.of(participantToSave);
        } catch (Exception e) {
            logger.error("Error adding participant: {}", e.getMessage(), e);
            throw new RepositoryException("Error adding participant: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> update(Participant participant, Participant newParticipant) {
        logger.traceEntry("Updating participant {} with new values from {}", participant, newParticipant);

        if (participant == null) {
            logger.error("Attempted to update null participant");
            throw new ValidationException("Attempted to update null participant");
        }

        if (participant.getId() == null) {
            logger.error("Attempted to update participant with null id");
            throw new ValidationException("Cannot update participant with null id");
        }

        try {
            final UUID participantId = participant.getId();
            final String newName = newParticipant.getName();

            Participant updatedParticipant = hibernateUtils.getSessionFactory().fromTransaction(session -> {
                Participant existingParticipant = session.get(Participant.class, participantId);
                if (existingParticipant == null) {
                    logger.error("Participant with id {} not found", participantId);
                    return null;
                }
                existingParticipant.setName(newName);
                session.merge(existingParticipant);
                logger.info("Successfully updated participant with id {}", participantId);
                return existingParticipant;
            });
            logger.traceExit();
            return Optional.ofNullable(updatedParticipant);
        } catch (Exception e) {
            logger.error("Error updating participant: {}", e.getMessage(), e);
            throw new RepositoryException("Error updating participant: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> delete(UUID id) {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }

    private Participant generateParticipant(Participant participant) {
        Participant newParticipant = new Participant(UUID.randomUUID(), participant.getName());
        logger.info("Generated new participant with id {}", newParticipant.getId());

        if (findOne(newParticipant.getId()).isPresent()) {
            logger.warn("UUID collision detected: {}", newParticipant.getId());
            newParticipant.setId(UUID.randomUUID());
            logger.info("Generated another UUID: {}", newParticipant.getId());
        }

        return newParticipant;
    }
}