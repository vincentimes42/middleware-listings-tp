package com.app.listings.web.controller;

import com.app.listings.web.dto.ListingRequestDto;
import com.app.listings.web.dto.ListingResponseDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Listing endpoints - exercise.md Steps 2-3, split per version in Step 6a. */
@RestController
@RequestMapping("/listings")
public class ListingController {

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ListingResponseDto create(@RequestBody ListingRequestDto request) {
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
