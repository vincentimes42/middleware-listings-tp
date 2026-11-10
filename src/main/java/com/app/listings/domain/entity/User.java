package com.app.listings.domain.entity;

import com.app.listings.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Seller account - provided (exercise.md Step 1). Fluent setters ({@code withX}). */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(name = "registration_date", nullable = false)
    private Instant registrationDate;

    public User() {
        // required by JPA
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public User withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public User withUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public User withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public User withRole(Role role) {
        this.role = role;
        return this;
    }

    public Instant getRegistrationDate() {
        return registrationDate;
    }

    public User withRegistrationDate(Instant registrationDate) {
        this.registrationDate = registrationDate;
        return this;
    }

}
