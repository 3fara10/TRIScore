package org.example.networking.dto;

import java.io.Serializable;
import java.util.UUID;

public class EventDTO implements Serializable {
    private UUID id;
    private String name;

    public EventDTO() {
    }

    public EventDTO(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public EventDTO(UUID id) {
        this(id, "");
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
}