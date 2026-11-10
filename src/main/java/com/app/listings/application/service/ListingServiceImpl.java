package com.app.listings.application.service;

import com.app.listings.domain.entity.Listing;
import org.springframework.stereotype.Service;

/** Listing business rules - exercise.md Steps 2, 3, 7 and 8. */
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
