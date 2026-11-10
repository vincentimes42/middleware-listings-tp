package com.app.listings.application.service;

import com.app.listings.domain.entity.Listing;

/** Listing use-cases. Replace {@code Object dto} with your own DTO types. */
public interface ListingService {

    Listing create(Long ownerId, Object dto);

    Listing findById(Long id, Long requestingUserId);

    void update(Long id, Long requestingUserId, Object dto);

    void delete(Long id, Long requestingUserId);
}
