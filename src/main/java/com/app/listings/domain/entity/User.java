package com.app.listings.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User entity - minimal skeleton.
 *
 * Add your business fields here (email, username, passwordHash, role,
 * registrationDate, etc.), plus their getters and setters.
 * Note: setters are FLUENT (each setter returns `this`).
 *
 * Example of a fluent setter:
 *     public User withEmail(String email) { this.email = email; return this; }
 */
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected User() {
    }

    public Long getId() {
        return id;
    }
}
