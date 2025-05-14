package org.example.repository;

import java.lang.UnsupportedOperationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Event;
import org.example.utils.HibernateUtils;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Repository
public class HibernateRepositoryEvent implements IRepositoryEvent {

    private final HibernateUtils hibernateUtils;
    private static final Logger logger = LogManager.getLogger(HibernateRepositoryEvent.class);

    @Autowired
    public HibernateRepositoryEvent(Properties props) {
        logger.info("Initializing HibernateRepositoryEvent with properties: {}", props);
        hibernateUtils = new HibernateUtils(props);
    }

    @Override
    public Optional<Event> findOne(UUID id) {
        logger.traceEntry("Finding event with id {}", id);

        if (id == null) {
            logger.error("Attempted to find event with null id");
            throw new ValidationException("Attempted to find event with null id");
        }

        try {
            Event event = hibernateUtils.getSessionFactory().fromSession(session -> session.find(Event.class, id));
            logger.traceExit("Found event: {}", event);
            return Optional.ofNullable(event);
        } catch (Exception e) {
            logger.error("Error finding event with id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Error finding event: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<Event> findAll() {
        logger.traceEntry("Finding all events");

        try {
            List<Event> events = hibernateUtils.getSessionFactory().fromSession(session -> {
                Query<Event> query = session.createQuery("FROM Event", Event.class);
                return query.list();
            });
            logger.info("Found {} events", events.size());
            logger.traceExit();
            return events;
        } catch (Exception e) {
            logger.error("Error finding all events: {}", e.getMessage(), e);
            throw new RepositoryException("Error finding all events: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Event> add(Event event) {
        logger.traceEntry("Saving event {}", event);

        if (event == null) {
            logger.error("Attempted to add null event");
            throw new ValidationException("Attempted to add null event");
        }

        try {
            Event eventToSave = event;
            if (eventToSave.getId() == null || eventToSave.getId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                eventToSave = generateEvent(event);
            }
            Event new_event = eventToSave;
            hibernateUtils.getSessionFactory().inTransaction(session -> {
                session.persist(new_event);
                logger.info("Successfully saved event with id {}", new_event.getId());
            });
            logger.traceExit();
            return Optional.of(eventToSave);
        } catch (Exception e) {
            logger.error("Error adding event: {}", e.getMessage(), e);
            throw new RepositoryException("Error adding event: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Event> update(Event event, Event newEvent) {
        logger.traceEntry("Updating event {} with new values from {}", event, newEvent);

        if (event == null) {
            logger.error("Attempted to update null event");
            throw new ValidationException("Attempted to update null event");
        }

        if (event.getId() == null) {
            logger.error("Attempted to update event with null id");
            throw new ValidationException("Cannot update event with null id");
        }

        try {
             UUID eventId = event.getId();
             String newName = newEvent.getName();
            Event updatedEvent = hibernateUtils.getSessionFactory().fromTransaction(session -> {
                Event existingEvent = session.get(Event.class, eventId);
                if (existingEvent == null) {
                    logger.error("Event with id {} not found", eventId);
                    return null;
                }
                existingEvent.setName(newName);
                session.merge(existingEvent);
                logger.info("Successfully updated event with id {}", eventId);
                return existingEvent;
            });

            logger.traceExit();
            return Optional.ofNullable(updatedEvent);
        } catch (Exception e) {
            logger.error("Error updating event: {}", e.getMessage(), e);
            throw new RepositoryException("Error updating event: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Event> delete(UUID id) {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }

    private Event generateEvent(Event event) {
        Event newEvent = new Event(UUID.randomUUID(), event.getName());
        logger.info("Generated new event with id {}", newEvent.getId());

        if (findOne(newEvent.getId()).isPresent()) {
            logger.warn("UUID collision detected: {}", newEvent.getId());
            newEvent.setId(UUID.randomUUID());
            logger.info("Generated another UUID: {}", newEvent.getId());
        }

        return newEvent;
    }
}