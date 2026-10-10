package com.app.listings.application.service;

import com.app.listings.domain.entity.Listing;
import org.springframework.stereotype.Service;

/**
 * ListingService implementation.
 * To be completed according to exercise.md (Steps 2-3 - CRUD & security,
 * Step 6 - versioning, Step 7 - geolocation).
 */
@Service
public class ListingServiceImpl implements ListingService {

    @Override
    public Listing create(Long ownerId, Object dto) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Listing findById(Long id, Long requestingUserId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void update(Long id, Long requestingUserId, Object dto) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(Long id, Long requestingUserId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
