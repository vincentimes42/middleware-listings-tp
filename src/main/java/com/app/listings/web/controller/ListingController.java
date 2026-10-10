package com.app.listings.web.controller;

import com.app.listings.web.dto.ListingRequestDto;
import com.app.listings.web.dto.ListingResponseDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LISTINGS controller - base version (before versioning).
 *
 * To do:
 *  1. split this controller into V1 and V2 (@RequestMapping(version = "V1"/"V2"))
 *  2. expose the private fields visibility rule (V2)
 *  3. add JSON + XML content negotiation (produces/consumes)
 *  4. add the owner-only rule on PUT / DELETE (403)
 *  5. add the active=false rule (hidden for others, 404 on detail)
 *  6. add filters + pagination
 *  7. implement versioning header + param + fallback (see exercise.md Step 6)
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
