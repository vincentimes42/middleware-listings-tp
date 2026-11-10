package com.app.listings.domain.repository;

import com.app.listings.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provided (exercise.md Step 1). */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    User findByEmail(String email);
}
