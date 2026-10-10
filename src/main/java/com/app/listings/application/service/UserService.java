package com.app.listings.application.service;

import com.app.listings.domain.entity.User;

/**
 * User service - application port.
 * Implement the business logic:
 *   - registration (unique email, password hashing)
 *   - authentication
 *   - retrieval of the current user
 */
public interface UserService {

    User create(String email, String username, String password);

    User findById(Long id);

    User findByEmail(String email);
}
