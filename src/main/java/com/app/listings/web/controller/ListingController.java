package com.app.listings.web.controller;

import com.app.listings.web.dto.ListingRequestDto;
import com.app.listings.web.dto.ListingResponseDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LISTINGS controller - base version (before versioning).
 *
 * To do (exercise.md):
 *  - Step 2: wire the five endpoints, pagination (Page) + filters
 *    (category, maxPrice), 201 + Location, 204 on delete
 *  - Step 3: anonymous reads (caller = null); owner-only and hidden-listing
 *    rules are enforced in the service
 *  - Step 6: declare each endpoint for version "1" and "2" (query parameter
 *    ?version=V1|V2, configured in ApiVersioningConfig), V2 visibility
 *  - Step 9 (bonus): distance search on the V2 list
 *
 * The response DTO illustrates the domain/DTO separation.
 */
@RestController
@RequestMapping("/listings")
public class ListingController {

    /* TODO: inject the listing service + the current-user service */

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ListingResponseDto create(@RequestBody ListingRequestDto request) {
        // TODO: implement the call to the business service
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @GetMapping
    public List<ListingResponseDto> list() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @GetMapping("/{id}")
    public ListingResponseDto get(@PathVariable Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @PutMapping("/{id}")
    public ListingResponseDto update(@PathVariable Long id, @RequestBody ListingRequestDto request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
