package org.example.repository;

import org.example.model.ParticipantResult;

import java.util.UUID;

public interface IRepositoryDTO {
    Iterable<ParticipantResult> getAllParticipantsSortedByNameWithTotalPoints();

    Iterable<ParticipantResult> getParticipantsWithResultsForEvent(UUID eventId);

}
