package org.example.repository;

import org.example.model.Albitru;
import org.example.model.Participant;

public interface IRepositoryAlbitri extends IRepository<Albitru> {
    Albitru authenticate(String username, String password);
    Albitru findByUsername(String username);
    boolean updateCredentials(int participantId, String username, String password);
}
