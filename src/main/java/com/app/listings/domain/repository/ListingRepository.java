package com.app.listings.domain.repository;

import com.app.listings.domain.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

/** Listing queries - exercise.md Steps 2, 3 and 9. */
public interface ListingRepository extends JpaRepository<Listing, Long> {
}
