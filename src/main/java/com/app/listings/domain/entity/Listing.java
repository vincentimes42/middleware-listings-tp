package com.app.listings.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Listing entity - minimal skeleton.
 *
 * Add your business fields here (title, description, price, category, active,
 * negotiable, streetAddress, city, postalCode, country, latitude, longitude,
 * owner, etc.), plus their getters and setters.
 * Note: setters are FLUENT (each setter returns `this`).
 *
 * Example of a fluent setter:
 *     public Listing withTitle(String title) { this.title = title; return this; }
 */
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
