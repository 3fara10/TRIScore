package org.example.service;


import org.example.model.Event;
import org.example.model.Referee;

import java.util.UUID;

public interface IAuthentificationService extends IService {
    Referee login(String username, String password, IObserver observer) throws Exception;

    void logout(UUID refereeId) throws Exception;

    Referee registerReferee(String name, Event event, String username, String password, IObserver observer) throws Exception;
}
