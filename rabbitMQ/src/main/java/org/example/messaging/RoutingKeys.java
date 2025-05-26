package org.example.messaging;
public final class RoutingKeys {

    private RoutingKeys() {}

    public static final String SPORTS_EXCHANGE = "sports.events";
    public static final String RESULTS = "event.results";
    public static final String RESULT_ADDED = "event.results.added";
    public static final String REFEREE_LOGIN = "referee.login";
    public static final String REFEREE_LOGOUT = "referee.logout";
    public static final String REFEREE_REGISTERED = "referee.registered";
    public static final String SYSTEM_NOTIFICATIONS = "system.notifications";
    public static final String MAIN_QUEUE = "main.queue";
}