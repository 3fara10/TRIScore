package org.example.service;

import org.example.model.Event;
import org.example.model.Referee;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IAuthentificationService {
    CompletableFuture<Referee> loginAsync(String username, String password, IObserver observer);

    CompletableFuture<Void> logoutAsync(UUID refereeId);

    CompletableFuture<Referee> registerRefereeAsync(String name, Event event, String username, String password, IObserver observer);
}
