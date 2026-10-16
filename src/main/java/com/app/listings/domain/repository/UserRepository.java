package com.app.listings.domain.repository;

import com.app.listings.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * User repository — PROVIDED (see exercise.md Step 1).
 *
 * The two derived queries below are the ones the skeleton already calls
 * from {@code UserServiceImpl} (email uniqueness on registration, and
 * lookup by email for the {@code AppUserDetailsService}). The `existsByEmail`
 * index on `email` is declared in the v1.0.1 changelog.
 *
 * (If you later add a V2 lookup by `username`, add `findByName` here.)
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    User findByEmail(String email);
}
