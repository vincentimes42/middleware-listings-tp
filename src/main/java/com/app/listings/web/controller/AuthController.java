package com.app.listings.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUTH controller - non-versioned endpoints.
 * To be completed according to exercise.md Step 1:
 *  - POST /auth/register : create the account (201, 409, 422)
 *  - GET  /auth/me       : current HTTP-Basic authenticated account (200 / 401)
 *
 * Note: there is NO login endpoint - authentication is HTTP Basic (Step 1.4).
 */
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
