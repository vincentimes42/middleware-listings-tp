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

/** Context smoke test - needs PostgreSQL on localhost:5435 (README, Build & test). More tests: exercise.md Appendix C. */
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
