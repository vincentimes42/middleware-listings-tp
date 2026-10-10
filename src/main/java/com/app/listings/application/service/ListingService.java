package com.app.listings.application.service;

import com.app.listings.domain.entity.Listing;

/**
 * Listing service - application port.
 * Implement the business logic:
 *   - CRUD (create, read, update, delete)
 *   - visibility rules (private fields depending on role)
 *   - activation / deactivation
 *   - ownership check (403 if not owner)
 *   - geolocation (Nominatim consumption with fallback)
 */
public interface ListingService {

    Listing create(Long ownerId, Object dto);

    Listing findById(Long id, Long requestingUserId);

    void update(Long id, Long requestingUserId, Object dto);

    void delete(Long id, Long requestingUserId);
}
