package com.app.listings.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Basic security (HTTP Basic, no session) - minimal level required by the subject.
 *
 * To be completed according to exercise.md:
 *  - Step 1: UserDetailsService loading users from UserRepository (passwords hashed)
 *  - Step 3: public GET /listings*, authenticated SELLER on POST/PUT/DELETE /listings,
 *            /auth/register and /actuator/** public
 *  (the 403 owner-only rule on PUT/DELETE comes from the service layer, not here)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // HTTP Basic authentication (no form login, no session). Step 1.4.
            .httpBasic(basic -> {})
            .authorizeHttpRequests(auth -> auth
                    // TODO: refine the rules according to exercise.md Step 3
                    .requestMatchers("/auth/register", "/actuator/**", "/v3/api-docs/**",
                            "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .anyRequest().authenticated()
            );
        return http.build();
    }
}
