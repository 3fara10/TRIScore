package org.example.rest.controller;

import org.example.model.Event;
import org.example.repository.IRepositoryEvent;
import org.example.repository.RepositoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5177")
@RestController
@RequestMapping("/project/events")
public class EventController {

    @Autowired
    private IRepositoryEvent eventRepository;

    @RequestMapping(method= RequestMethod.GET)
    public ResponseEntity<?> getAll() {
        try {
            Iterable<Event> events = eventRepository.findAll();
            return new ResponseEntity<>(events, HttpStatus.OK);
        } catch (RepositoryException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        try {
            Optional<Event> eventOpt = eventRepository.findOne(id);
            if (eventOpt.isEmpty()) {
                return new ResponseEntity<>("Event not found", HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(eventOpt.get(), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid UUID format", HttpStatus.BAD_REQUEST);
        } catch (RepositoryException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> create(@RequestBody Event event) {
        try {
            //event.setId(null);
            Optional<Event> savedEventOpt = eventRepository.add(event);
            if (savedEventOpt.isEmpty()) {
                return new ResponseEntity<>("Failed to create event", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            Event savedEvent = savedEventOpt.get();
            return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
        } catch (RepositoryException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> update(@RequestBody Event newEvent, @PathVariable UUID id) {
        try {
            Event existingEvent = eventRepository.findOne(id).get();
            Optional<Event> updatedEventOpt = eventRepository.update(existingEvent, newEvent);
            if (updatedEventOpt.isEmpty()) {
                return new ResponseEntity<>("Failed to update event", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(updatedEventOpt.get(), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid UUID format", HttpStatus.BAD_REQUEST);
        } catch (RepositoryException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            Optional<Event> deletedEventOpt = eventRepository.delete(id);
            if (deletedEventOpt.isEmpty()) {
                return new ResponseEntity<>("Failed to delete event", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(deletedEventOpt.get(), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid UUID format", HttpStatus.BAD_REQUEST);
        } catch (UnsupportedOperationException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String generalError(Exception e) {
        return "An unexpected error occurred: " + e.getMessage();
    }
}
