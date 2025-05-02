package org.example.service;

import java.util.UUID;

public interface IService extends AutoCloseable{
    void registerObserver(IObserver observer, UUID refereeId) throws Exception;

    void unregisterObserver(UUID refereeId) throws Exception;

    void close() throws Exception;
}
