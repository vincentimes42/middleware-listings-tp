package com.app.listings.application.service;

import com.app.listings.domain.entity.User;
import com.app.listings.web.dto.UserRequestDto;

/** Account use-cases - provided (exercise.md Step 1). */
public interface UserService {

    /** @throws com.app.listings.domain.exception.EmailAlreadyExistsException if the email is taken */
    User register(UserRequestDto request);

    User findById(Long id);

    User findByEmail(String email);
}
