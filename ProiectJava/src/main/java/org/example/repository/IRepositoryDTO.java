package org.example.repository;

import org.example.model.ParticipantResultDTO;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IRepositoryDTO {
    CompletableFuture<Iterable<ParticipantResultDTO>> getAllParticipantsSortedByNameWithPointsAsync();

    CompletableFuture<Iterable<ParticipantResultDTO>> getParticipantsWithResultsForEventAsync(UUID eventId);

    CompletableFuture<Integer> getTotalPointsForParticipantAsync(UUID participantId);
}
