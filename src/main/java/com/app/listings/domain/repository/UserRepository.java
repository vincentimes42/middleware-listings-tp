package com.app.listings.domain.repository;

import com.app.listings.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * User repository. Only JpaRepository is exposed by the skeleton.
 * Add your domain-specific queries here (e.g. findByEmail, existsByEmail, ...)
 * as needed by exercise.md Step 1.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    // TODO: add your specific queries.
}
