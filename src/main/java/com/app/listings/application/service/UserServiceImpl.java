package com.app.listings.application.service;

import com.app.listings.domain.Role;
import com.app.listings.domain.entity.User;
import com.app.listings.domain.exception.EmailAlreadyExistsException;
import com.app.listings.domain.repository.UserRepository;
import com.app.listings.web.dto.UserRequestDto;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * UserService implementation. PROVIDED (see exercise.md Step 1).
 *
 * This class already:
 *  - {@link #register(UserRequestDto)}: enforces email uniqueness
 *    (throws {@link EmailAlreadyExistsException} - the
 *    {@code GlobalExceptionHandler} maps it to 409), hashes the
 *    password via the provided {@link PasswordEncoder} bean (BCrypt),
 *    and stamps `registration_date` with the current instant.
 *  - {@link #findById(Long)} / {@link #findByEmail(String)}: lookups
 *    used by the controller and by the {@code AppUserDetailsService}
 *    (see {@code SecurityConfig}) for Basic-auth.
 *
 * The student's only job in Step 1 is to wire the controller
 * ({@code AuthController}) to this service - see the step's Hints and
 * Acceptance checklist in exercise.md.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(UserRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        User user = new User()
                .withEmail(request.getEmail())
                .withUsername(request.getUsername())
                .withPasswordHash(passwordEncoder.encode(request.getPassword()))
                .withRole(Role.SELLER)
                .withRegistrationDate(Instant.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("No user with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        User found = userRepository.findByEmail(email);
        if (found == null) {
            throw new UsernameNotFoundException(email);
        }
        return found;
    }
}
