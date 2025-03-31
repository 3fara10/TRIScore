package org.example.service;

import java.util.UUID;
import java.io.Closeable;

public interface IService extends Closeable{
    void registerObserver(IObserver observer, UUID refereeId);

    void unregisterObserver(UUID refereeId);

    void close();
}
