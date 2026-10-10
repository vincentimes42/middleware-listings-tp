package com.app.listings.domain.repository;

import com.app.listings.domain.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Listing repository. Only JpaRepository is exposed by the skeleton.
 * Add your domain-specific queries here (e.g. findByCategory,
 * findByOwner, geo search by lat/lng, pagination, filters,
 * active=false visibility, etc.) as needed by exercise.md Steps 2, 6 and 9.
 */
public interface ListingRepository extends JpaRepository<Listing, Long> {
    // TODO: add your specific queries.
}
