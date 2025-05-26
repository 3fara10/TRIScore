package org.example.messaging.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    RESULT_ADDED("result.added"),
    RESULT_UPDATED("result.updated"),
    REFEREE_LOGIN("referee.login"),
    REFEREE_LOGOUT("referee.logout"),
    REFEREE_REGISTERED("referee.registered"),
    SYSTEM_NOTIFICATION("system.notification");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}