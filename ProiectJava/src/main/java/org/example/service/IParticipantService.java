package org.example.service;

import org.example.model.ParticipantResultDTO;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IParticipantService {
    CompletableFuture<Iterable<ParticipantResultDTO>> getAllParticipantsSortedByNameAsync();

    CompletableFuture<Iterable<ParticipantResultDTO>> getParticipantsWithResultsForEventAsync(UUID eventId);

    CompletableFuture<Integer> getTotalPointsForParticipantAsync(UUID participantId);
}
