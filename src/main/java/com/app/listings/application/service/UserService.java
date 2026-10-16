package com.app.listings.application.service;

import com.app.listings.domain.entity.User;
import com.app.listings.web.dto.UserRequestDto;

/**
 * User service - application port. PROVIDED (see exercise.md Step 1).
 *
 * The interface and {@code UserServiceImpl} are already complete:
 * registration (unique email, hashed password) and the two lookups.
 *
 * The student's job in Step 1 is to wire the controller
 * ({@code AuthController}) and the Basic-auth chain to call this service
 * - see the step's "Hints" and "Acceptance checklist" in exercise.md.
 *
 * The raw {@code password} argument (and the same-named field on
 * {@link UserRequestDto}) is a *request*-only value: it is hashed and
 * stored; the entity itself exposes only the hash, never the plaintext.
 */
public interface UserService {

    /**
     * Register a new account (Step 1.1 - 201).
     *
     * @throws com.app.listings.domain.exception.EmailAlreadyExistsException
     *         if the email is already taken (Step 1.2 - 409).
     */
    User register(UserRequestDto request);

    User findById(Long id);

    User findByEmail(String email);
}
