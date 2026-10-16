package com.app.listings.config;

import com.app.listings.application.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Basic security — PROVIDED skeleton (see exercise.md Step 1, hint).
 *
 * What this file gives you (so the skeleton boots green on day 1 and the
 * Step 1 wiring work is focused on the controller, not on Security plumbing):
 *
 *  - a `PasswordEncoder` bean (BCrypt, per Step 1.5).
 *  - a `UserDetailsService` that loads by email from the `UserRepository`
 *    (via the provided `UserService`), so Basic auth already works:
 *    `curl -u alice@test:secret123 http://localhost:8080/auth/me` will
 *    return 401 until you have created `alice@test` and wired the controller.
 *    That's a good "does Security work?" smoke test the moment you start.
 *  - a `SecurityFilterChain` that exposes `/auth/register`,
 *    `/actuator/**`, `/v3/api-docs/**`, and `/swagger-ui/**` (public)
 *    and requires authentication for the rest. Step 3 refines this.
 *
 * What the student still does in Step 1 (the only remaining work):
 *  - wire `AuthController` (`/auth/register`, `/auth/me`) to the provided
 *    `UserService` + DTOs — the "wiring" the step is teaching.
 *  - tighten the `SecurityFilterChain` in Step 3 (public read, `SELLER`
 *    write, non-owner `403`).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Step 1.5: the `PasswordEncoder` used for hashing at register time. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Step 1.4 (half of it): the provider that resolves a Basic-auth
     * login (email) to the stored password hash. `UserServiceImpl`
     * already hashes, so the returned BCrypt hash compares against the
     * plaintext the client sends.
     */
    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return email -> {
            var u = userService.findByEmail(email);
            if (u == null) {
                throw new UsernameNotFoundException(email);
            }
            return User.withUsername(u.getEmail())
                    .password(u.getPasswordHash())
                    // `roles()` on `User` expects names without the
                    // `ROLE_` prefix; the enum's value ("SELLER") maps
                    // directly.
                    .roles(u.getRole() == null ? "SELLER" : u.getRole().name())
                    .build();
        };
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        http
            .authenticationProvider(daoAuthenticationProvider)
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> {})
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register", "/actuator/**", "/v3/api-docs/**",
                                 "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
