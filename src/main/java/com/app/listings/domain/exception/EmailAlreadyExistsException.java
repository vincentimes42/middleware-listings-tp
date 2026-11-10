package com.app.listings.domain.exception;

/**
 * Thrown when a registration attempt reuses an existing email
 * (exercise.md Step 1 - 409 Conflict). PROVIDED: thrown by
 * {@code UserServiceImpl.register} and already mapped to 409 by
 * {@link com.app.listings.web.advice.GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }

}
