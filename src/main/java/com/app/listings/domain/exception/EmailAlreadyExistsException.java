package com.app.listings.domain.exception;

/**
 * Thrown when a registration attempt reuses an existing email
 * (exercise.md Step 1.2 - 409 Conflict). Catch it in the service
 * layer or in {@code UserServiceImpl.create}; map it to a 409
 * response via {@link com.app.listings.web.advice.GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }

}
