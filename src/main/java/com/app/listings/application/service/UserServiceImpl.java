package com.app.listings.application.service;

import com.app.listings.domain.entity.User;
import org.springframework.stereotype.Service;

/**
 * UserService implementation.
 * To be completed according to exercise.md (Step 1 - User Accounts).
 */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public User create(String email, String username, String password) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public User findById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public User findByEmail(String email) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
