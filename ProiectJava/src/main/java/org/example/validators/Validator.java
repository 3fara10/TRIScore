package org.example.validators;

import org.example.exceptions.ValidationException;

public interface Validator<E> {
    void validate(E entity) throws ValidationException;
}

