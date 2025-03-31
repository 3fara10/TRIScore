package org.example.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IResultService {
    CompletableFuture<Void> addResultAsync(UUID participantId, UUID eventId, int points);
}
