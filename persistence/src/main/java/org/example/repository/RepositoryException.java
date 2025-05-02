package org.example.repository;

import java.sql.SQLException;

/**
 * Exception thrown by repository operations.
 * Wraps SQL and validation exceptions to provide a consistent exception type
 * from the repository layer.
 */
public class RepositoryException extends RuntimeException {

    /**
     * Constructs a new repository exception with the specified detail message.
     *
     * @param message the detail message
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Constructs a new repository exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new repository exception with the specified cause and a
     * detail message of (cause==null ? null : cause.toString()).
     *
     * @param cause the cause of this exception
     */
    public RepositoryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new repository exception with the specified detail message and
     * SQL exception cause.
     *
     * @param message the detail message
     * @param sqlException the SQL exception that caused this repository exception
     */
    public RepositoryException(String message, SQLException sqlException) {
        super(message, sqlException);
    }
}