package org.example.repository;

import org.example.model.Entity;

import java.util.Collection;
import java.util.Iterator;

public interface IRepository<T extends Entity> {

    void add(T entity);
    void update(T entity);
    void delete(T entity);
    Collection<T> getAll();
    T getEntity(int id);
    Iterator<T> iterator();

}
