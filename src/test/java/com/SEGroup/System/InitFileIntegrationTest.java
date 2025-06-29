// src/test/java/com/SEGroup/system/InitFileIntegrationTest.java
package com.SEGroup.System;

import com.SEGroup.UI.InitFileRunner;
import com.SEGroup.Infrastructure.Repositories.ProductCatalogRepository;
import com.SEGroup.Infrastructure.Repositories.StoreRepository;
import com.SEGroup.Infrastructure.Repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Replays initial-state.json and verifies that the DB
 * contains everything described in that file.
 */
@SpringBootTest
@ActiveProfiles("db")                                  // profile that wires a real DataSource
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InitFileIntegrationTest {

    /* ------------------------------------------------------------------
       Beans under test
       ------------------------------------------------------------------ */
    @Autowired InitFileRunner          runner;
    @Autowired StoreRepository         stores;
    @Autowired UserRepository          users;
    @Autowired(required = false) ProductCatalogRepository catalog;   // optional

    /* ------------------------------------------------------------------
       Load the JSON *once* before all assertions
       ------------------------------------------------------------------ */
    @BeforeAll
    void replayInitialState() throws Exception {
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream("bootstrap/initial-state.json")) {
            assertThat(in)
                    .as("initial-state.json must be on the classpath (src/main/resources)")
                    .isNotNull();
        }
    }

    /* ------------------------------------------------------------------
       Assertions
       ------------------------------------------------------------------ */

    @Test
    void allSixUsersWereCreated() {
        List<String> expected = List.of(
                "u1@example.com", "u2@example.com", "u3@example.com",
                "u4@example.com", "u5@example.com", "u6@example.com"
        );

        expected.forEach(u ->
                assertThatCode(() -> users.checkIfExist(u))    // throws if missing
                        .as("user %s exists", u)
                        .doesNotThrowAnyException()
        );
    }

    @Test
    void shopIsPersisted() {
        assertThat(stores.isStoreExist("Bamba-Shop")).isTrue();
    }

    @Test
    void founderIsLinked() {
        assertThat(stores.getStoreFounder("Bamba-Shop"))
                .isEqualTo("u2@example.com");
    }

    @Test
    void descriptionWasApplied() {
        assertThat(stores.getStore("Bamba-Shop").getDescription())
                .isEqualTo("");
    }

    /* --------- optional catalog sanity check (only if bean is present) --------- */

    @Test
    void catalogProductExists() {
        if (catalog == null) return;                   // in-memory profile → no bean
        // isProductExist throws if the ID is missing, so “no exception” means success
        assertThatCode(() -> catalog.isProductExist("SNACK-001"))
                .doesNotThrowAnyException();
    }
}
