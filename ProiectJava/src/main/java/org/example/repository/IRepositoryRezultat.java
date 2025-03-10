package org.example.repository;

import org.example.model.Rezultat;

public interface IRepositoryRezultat extends IRepository<Rezultat> {
    Rezultat authenticate(String username, String password);
    Rezultat findByUsername(String username);
    boolean updateCredentials(int participantId, String username, String password);
}
