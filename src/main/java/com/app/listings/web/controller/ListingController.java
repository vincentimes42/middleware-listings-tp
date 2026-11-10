package com.app.listings.web.controller;

import com.app.listings.web.dto.ListingRequestDto;
import com.app.listings.web.dto.ListingResponseDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LISTINGS controller - base version (before versioning).
 *
 * To do (step numbers of exercise.md):
 *  - Step 2  : POST, GET list (pagination + filters), GET detail (404)
 *  - Step 3  : PUT / DELETE, owner-only rule (403), active=false (hidden, 404)
 *  - Step 6a : split into ListingControllerV1 / ListingControllerV2
 *              (version = "V1" / "V2", query parameter ?version= only)
 *  - Step 6b : owner-only fields in V2
 *  - Step 4  : (bonus) JSON + XML content negotiation
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
