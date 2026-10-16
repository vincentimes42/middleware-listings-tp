package com.app.listings;

import com.app.listings.domain.repository.ListingRepository;
import com.app.listings.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test template (starting point).
 *
 * This test only checks that the Spring context starts correctly:
 * PostgreSQL, Liquibase (v1.0 changelog applied),
 * domain and persistence beans. This is the minimal foundation of the whole exercise.
 *
 * NB: it assumes a running PostgreSQL reachable at `jdbc:postgresql://localhost:5435/listings`
 * (i.e. `docker compose up` in this folder), and that the `app_user` / `listing`
 * tables have been created by Liquibase.
 *
 * To be extended: cover all the endpoints and business rules of exercise.md
 * (core Steps 1·2·3·5·6·8, plus the bonus steps 4, 7, 9):
 * happy path JSON/XML, 403 ownership, 422 validation,
 * active=false visibility, V1/V2 (private fields), versioning fallback,
 * Nominatim fallback, cumulative Liquibase changelogs, etc.
 *
 * NB: Spring Boot 4 introduced new web testing mechanisms
 * (see org.springframework.boot.test.web / test.http). Adapt your
 * testing approach accordingly (WebTestClient, localTestWebServer, etc.).
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListingsSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Test
    @DisplayName("Spring context starts (PostgreSQL + Liquibase + beans)")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBean(ListingsApplication.class)).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(listingRepository).isNotNull();
    }
}
