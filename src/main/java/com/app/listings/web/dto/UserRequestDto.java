package com.app.listings.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REQUEST DTO for POST /auth/register — provided.
 *
 * Used by the controller for the registration endpoint (see
 * exercise.md Step 1, user story). Fields map 1:1 to the domain
 * {@link com.app.listings.domain.entity.User} identity attributes; the
 * raw {@code password} is never persisted, only the BCrypt hash is stored
 * (see {@code user.password_hash}).
 */
public class UserRequestDto {

    @Email(message = "a valid email is required")
    @NotBlank(message = "email is required")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "username is required")
    @Size(min = 1, max = 100)
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 128, message = "password must be at least 6 characters")
    private String password;

    protected UserRequestDto() {
        // needed by Jackson
    }

    public UserRequestDto(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public UserRequestDto withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public UserRequestDto withUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserRequestDto withPassword(String password) {
        this.password = password;
        return this;
    }

}
