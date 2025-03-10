package org.example.repository;

import org.example.model.Participant;

public interface IRepositoryParticipanti extends IRepository <Participant> {
    Participant authenticate(String username, String password);
    Participant findByUsername(String username);
    boolean updateCredentials(int participantId, String username, String password);
}
