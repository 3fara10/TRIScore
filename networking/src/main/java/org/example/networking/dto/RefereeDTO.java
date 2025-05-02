package org.example.networking.dto;

import java.io.Serializable;
import java.util.UUID;

public class RefereeDTO implements Serializable {
    private UUID id;
    private String name;
    private String username;
    private String password;
    private UUID eventId;
    private String eventName;

    public RefereeDTO(String username, String password) {
        this(null, "", username, password, null, "");
    }

    public RefereeDTO(UUID id, String name, String username, String password, UUID eventId, String eventName) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public RefereeDTO() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
}
