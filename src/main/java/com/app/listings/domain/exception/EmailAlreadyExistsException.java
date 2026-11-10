package com.app.listings.domain.exception;

/** Duplicate email on registration (mapped to 409). */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }

}
