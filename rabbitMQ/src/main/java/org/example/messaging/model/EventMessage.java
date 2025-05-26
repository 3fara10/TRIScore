package org.example.messaging.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

public class EventMessage {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("messageType")
    private MessageType messageType;

    @JsonProperty("eventId")
    private UUID eventId;

    @JsonProperty("participantId")
    private UUID participantId;

    @JsonProperty("participantName")
    private String participantName;

    @JsonProperty("points")
    private Integer points;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("refereeId")
    private UUID refereeId;

    @JsonProperty("refereeName")
    private String refereeName;

    public EventMessage() {
        this.id = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
    }

    public EventMessage(MessageType messageType, String message) {
        this();
        this.messageType = messageType;
        this.message = message;
    }

    public static class Builder {
        private EventMessage message = new EventMessage();

        public Builder messageType(MessageType type) {
            message.messageType = type;
            return this;
        }

        public Builder message(String msg) {
            message.message = msg;
            return this;
        }

        public Builder eventId(UUID eventId) {
            message.eventId = eventId;
            return this;
        }

        public Builder participant(UUID id, String name) {
            message.participantId = id;
            message.participantName = name;
            return this;
        }

        public Builder points(Integer points) {
            message.points = points;
            return this;
        }

        public Builder referee(UUID id, String name) {
            message.refereeId = id;
            message.refereeName = name;
            return this;
        }

        public EventMessage build() {
            return message;
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventMessage that = (EventMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("EventMessage{id=%s, type=%s, message='%s', timestamp=%s}",
                id, messageType, message, timestamp);
    }
}