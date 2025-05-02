package org.example.service;

import org.example.model.Result;

import java.util.Optional;
import java.util.UUID;

public interface IResultService {
    Optional<Result> addResult(UUID participantId, UUID eventId, int points) throws Exception;
}
