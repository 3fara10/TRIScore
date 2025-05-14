package org.example.rest.start;

import org.example.model.Event;
import org.example.rest.client.EventClient;
import org.example.rest.controller.ServiceException;
import org.springframework.web.client.RestClientException;

public class StartEventRestClient {
    private final static EventClient eventClient = new EventClient();

    public static void main(String[] args) {
        Event eventT = new Event("Test Event");
        try {
            System.out.println("Adding a new event: " + eventT);
            show(() -> System.out.println(eventClient.create(eventT)));
            System.out.println("\nPrinting all events ...");
            show(() -> {
                Event[] results = eventClient.getAll();
                for (Event e : results) {
                    System.out.println(e.getId() + ": " + e.getName());
                }
            });
        } catch (RestClientException ex) {
            System.out.println("Exception ... " + ex.getMessage());
        }

        System.out.println("\nInfo for event with id=" + eventT.getId());
        show(() -> System.out.println(eventClient.getById(eventT.getId())));

        System.out.println("\nUpdating event with id=" + eventT.getId());
        show(() -> System.out.println(eventClient.update(eventT)));

    }

    private static void show(Runnable task) {
        try {
            task.run();
        } catch (ServiceException e) {
            System.out.println("Service exception: " + e);
        }
    }
}
