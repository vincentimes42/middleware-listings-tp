package com.app.listings.web.dto;

import com.app.listings.domain.Role;

import java.time.Instant;

/** Account response - provided. Never exposes the password hash. */
public class UserResponseDto {

    private Long id;
    private String email;
    private String username;
    private Role role;
    private Instant registrationDate;

    public static UserResponseDto from(com.app.listings.domain.entity.User user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDto()
                .withId(user.getId())
                .withEmail(user.getEmail())
                .withUsername(user.getUsername())
                .withRole(user.getRole())
                .withRegistrationDate(user.getRegistrationDate());
    }

    protected UserResponseDto() {
        // needed by Jackson
    }

    public Long getId() {
        return id;
    }

    public UserResponseDto withId(Long id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserResponseDto withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public UserResponseDto withUsername(String username) {
        this.username = username;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public UserResponseDto withRole(Role role) {
        this.role = role;
        return this;
    }

    public Instant getRegistrationDate() {
        return registrationDate;
    }

    public UserResponseDto withRegistrationDate(Instant registrationDate) {
        this.registrationDate = registrationDate;
        return this;
    }

}
