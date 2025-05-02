package org.example.service;


import org.example.model.ParticipantResult;

import java.util.UUID;

public interface IParticipantService {
    Iterable<ParticipantResult> getAllParticipantsSortedByNameWithTotalPoints() throws Exception;

    Iterable<ParticipantResult> getParticipantsWithResultsForEvent(UUID eventId) throws Exception;
}
