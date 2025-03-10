package org.example.repository;

import org.example.model.Proba;

public interface IRepositoryProbe extends IRepository<Proba> {
    Proba authenticate(String username, String password);
    Proba findByUsername(String username);
    boolean updateCredentials(int participantId, String username, String password);
}
