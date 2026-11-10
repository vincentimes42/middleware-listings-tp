package com.app.listings.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Account endpoints - to wire in exercise.md Step 1. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/register")
    public Object register() {
        throw new UnsupportedOperationException("Not implemented yet - exercise.md Step 1.1");
    }

    @GetMapping("/me")
    public Object me() {
        throw new UnsupportedOperationException("Not implemented yet - exercise.md Step 1.4");
    }
}
