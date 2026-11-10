package com.app.listings.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Listing entity - fields and the owner relation: exercise.md Steps 2 and 5. Fluent setters ({@code withX}), as in {@link User}. */
@Entity
@Table(name = "listing")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected Listing() {
    }

    public Long getId() {
        return id;
    }
}
