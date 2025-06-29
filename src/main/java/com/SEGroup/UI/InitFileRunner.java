package com.SEGroup.UI;

import com.SEGroup.Service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Replays <classpath>bootstrap/initial-state.json against the real services
 * so the data is written to the DB at startup or from tests.
 */
@Component
// Removed @Profile so it always runs
public class InitFileRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitFileRunner.class);

    /* ------------------------------------------------------------------ */
    /*  Services & infrastructure                                         */
    /* ------------------------------------------------------------------ */
    private final GuestService guestSrv;
    private final UserService  userSrv;
    private final StoreService storeSrv;
    private final ObjectMapper mapper;

    /* ------------------------------------------------------------------ */
    /*  Bootstrap file location                                           */
    /* ------------------------------------------------------------------ */
    @Value("classpath:bootstrap/initial-state.json")
    private Resource bootstrapJson;

    /* ------------------------------------------------------------------ */
    /*  ctor                                                               */
    /* ------------------------------------------------------------------ */
    @Autowired
    public InitFileRunner(GuestService guestSrv,
                          UserService  userSrv,
                          StoreService storeSrv) {

        this.guestSrv = guestSrv;
        this.userSrv  = userSrv;
        this.storeSrv = storeSrv;
        this.mapper   = new ObjectMapper();
    }

    /* ------------------------------------------------------------------ */
    /*  Spring-Boot entry point                                           */
    /* ------------------------------------------------------------------ */
    @Override
    public void run(String... args) throws Exception {
        log.info("[InitRunner] ===== STARTING BOOTSTRAP =====");
        log.info("[InitRunner] Looking for bootstrap file at: {}", bootstrapJson.getDescription());

        try {
            if (!bootstrapJson.exists()) {
                log.error("[InitRunner] ❌ Bootstrap file does not exist: {}", bootstrapJson.getDescription());
                log.info("[InitRunner] Skipping bootstrap - file not found");
                return;
            }

            try (InputStream in = bootstrapJson.getInputStream()) {
                log.info("[InitRunner] ✓ Found bootstrap file, starting replay...");
                replay(in);
                log.info("[InitRunner] ===== BOOTSTRAP COMPLETE =====");
            }
        } catch (FileNotFoundException ex) {
            log.warn("[InitRunner] bootstrap/initial-state.json not found – skipping");
        } catch (Exception ex) {
            log.error("[InitRunner] ❌ Failed to execute bootstrap: {}", ex.getMessage(), ex);
            throw ex; // Re-throw to fail startup if critical
        }
    }

    /* ===================================================================
     *  PUBLIC helper – tests / UnifiedDataSeeder can call this directly
     * ===================================================================*/
    @Transactional
    public void replay(InputStream jsonStream) throws Exception {

        // Verify services are properly initialized
        verifyServices();

        ArrayNode steps = (ArrayNode) mapper.readTree(jsonStream);
        Map<String, String> token = new HashMap<>();          // email → session

        log.info("[InitRunner] Starting replay of {} steps", steps.size());

        for (int i = 0; i < steps.size(); i++) {
            JsonNode step = steps.get(i);

            String useCase = step.get("useCase").asText();
            ArrayNode argArray = (ArrayNode) step.withArray("args");

            log.info("[InitRunner] Step {}/{}: Executing useCase: {}", i + 1, steps.size(), useCase);

            // flatten to List<JsonNode>
            List<JsonNode> a = new ArrayList<>();
            argArray.elements().forEachRemaining(a::add);

            try {
                switch (useCase) {

                    /* ------------- registration / auth -------------------- */

                    case "guest-registration" -> {
                        String email = a.get(0).asText();
                        String pwd = a.get(1).asText();
                        log.info("[InitRunner] Registering user: {}", email);

                        try {
                            Result<?> result = userSrv.register(email, email, pwd);   // name=email to keep it simple
                            if (!result.isSuccess()) {
                                log.warn("[InitRunner] Registration failed for {}: {} (might already exist)",
                                        email, result.getErrorMessage());
                                // Don't fail completely - user might already exist from UnifiedDataSeeder
                            } else {
                                log.info("[InitRunner] ✓ Successfully registered: {}", email);
                            }
                        } catch (Exception e) {
                            log.warn("[InitRunner] Registration exception for {}: {} (might already exist)",
                                    email, e.getMessage());
                            // Continue - user might already exist
                        }
                    }

                    case "login" -> {
                        String email = a.get(0).asText();
                        String pwd = a.get(1).asText();
                        log.info("[InitRunner] Logging in user: {}", email);

                        Result<String> result = userSrv.login(email, pwd);
                        if (result.isSuccess()) {
                            String tk = result.getData();
                            token.put(email, tk);
                            log.info("[InitRunner] ✓ Successfully logged in: {} (token stored)", email);
                        } else {
                            log.error("[InitRunner] ❌ Login failed for {}: {}", email, result.getErrorMessage());
                            throw new IllegalStateException("Login failed for " + email + ": " + result.getErrorMessage());
                        }
                    }

                    case "logout" -> {
                        String email = a.get(0).asText();
                        String tk = token.get(email);
                        if (tk != null) {
                            userSrv.logout(tk);
                            token.remove(email);
                            log.info("[InitRunner] ✓ Logged out user: {}", email);
                        } else {
                            log.warn("[InitRunner] No token found for logout: {} (might not be logged in)", email);
                        }
                    }

                    /* ---------------- store operations ------------------- */

                    case "open-shop" -> {
                        String owner = a.get(0).asText();
                        String storeName = a.get(1).asText();
                        String ownerToken = token.get(owner);

                        log.info("[InitRunner] Opening shop '{}' for owner: {}", storeName, owner);

                        if (ownerToken == null) {
                            throw new IllegalStateException("No token found for owner: " + owner +
                                    ". Make sure they're logged in first. Available tokens: " + token.keySet());
                        }

                        Result<Void> r = storeSrv.createStore(ownerToken, storeName);
                        if (!r.isSuccess()) {
                            log.error("[InitRunner] ❌ Failed to create store '{}': {}", storeName, r.getErrorMessage());
                        } else {
                            log.info("[InitRunner] ✓ Successfully created store: {}", storeName);
                        }
                        failIfNeeded("open-shop", r);

                        // optional patch object – not implemented yet
                        if (a.size() > 2 && !a.get(2).isNull()) {
                            log.info("[InitRunner] open-shop patch present – skipped");
                        }
                    }

                    case "add-product-to-catalog" -> {
                        String id = a.get(0).asText();
                        String name = a.get(1).asText();
                        String brand = a.get(2).asText();
                        String desc = a.get(3).asText();
                        List<String> cats = mapper.convertValue(a.get(4), new TypeReference<List<String>>() {});

                        log.info("[InitRunner] Adding product to catalog: {} (ID: {})", name, id);

                        Result<String> r = storeSrv.addProductToCatalog(id, name, brand, desc, cats);
                        if (!r.isSuccess()) {
                            log.error("[InitRunner] ❌ Failed to add product to catalog '{}': {}", id, r.getErrorMessage());
                        } else {
                            log.info("[InitRunner] ✓ Successfully added to catalog: {} -> {}", id, r.getData());
                        }
                        failIfNeeded("add-product-to-catalog", r);
                    }

                    case "add-product-to-store" -> {
                        String owner = a.get(0).asText();
                        String storeName = a.get(1).asText();
                        String catalogId = a.get(2).asText();
                        String productName = a.get(3).asText();
                        String ownerToken = token.get(owner);

                        log.info("[InitRunner] Adding product '{}' (catalog: {}) to store '{}' by owner '{}'",
                                productName, catalogId, storeName, owner);

                        if (ownerToken == null) {
                            throw new IllegalStateException("No token found for owner: " + owner +
                                    ". Make sure they're logged in first. Available tokens: " + token.keySet());
                        }

                        Result<String> r = storeSrv.addProductToStore(
                                ownerToken,
                                storeName,
                                catalogId,           // catalog-id
                                a.get(3).asText(),   // name
                                a.get(4).asText(),   // description
                                a.get(5).asDouble(), // price
                                a.get(6).asInt(),    // qty
                                null);               // imageUrl

                        if (!r.isSuccess()) {
                            log.error("[InitRunner] ❌ Failed to add product '{}' to store '{}': {}",
                                    productName, storeName, r.getErrorMessage());
                        } else {
                            log.info("[InitRunner] ✓ Successfully added product to store: {} (product ID: {})",
                                    productName, r.getData());
                        }
                        failIfNeeded("add-product-to-store", r);
                    }

                    case "appoint-owner" -> {
                        String currentOwner = a.get(0).asText();
                        String storeName = a.get(1).asText();
                        String newOwner = a.get(2).asText();
                        String ownerToken = token.get(currentOwner);

                        log.info("[InitRunner] Appointing '{}' as owner of store '{}' by current owner '{}'",
                                newOwner, storeName, currentOwner);

                        if (ownerToken == null) {
                            throw new IllegalStateException("No token found for current owner: " + currentOwner +
                                    ". Available tokens: " + token.keySet());
                        }

                        Result<Void> r = storeSrv.appointOwner(ownerToken, storeName, newOwner);
                        if (!r.isSuccess()) {
                            log.error("[InitRunner] ❌ Failed to appoint '{}' as owner: {}", newOwner, r.getErrorMessage());
                        } else {
                            log.info("[InitRunner] ✓ Successfully appointed '{}' as owner of '{}'", newOwner, storeName);
                        }
                        failIfNeeded("appoint-owner", r);
                    }

                    case "appoint-manager" -> {
                        String owner = a.get(0).asText();
                        String storeName = a.get(1).asText();
                        String newManager = a.get(2).asText();
                        List<String> perms = mapper.convertValue(a.get(3), new TypeReference<List<String>>() {});
                        String ownerToken = token.get(owner);

                        log.info("[InitRunner] Appointing '{}' as manager of store '{}' with permissions: {} by owner '{}'",
                                newManager, storeName, perms, owner);

                        if (ownerToken == null) {
                            throw new IllegalStateException("No token found for owner: " + owner +
                                    ". Available tokens: " + token.keySet());
                        }

                        Result<Void> r = storeSrv.appointManager(ownerToken, storeName, newManager, perms);
                        if (!r.isSuccess()) {
                            log.error("[InitRunner] ❌ Failed to appoint '{}' as manager: {}", newManager, r.getErrorMessage());
                        } else {
                            log.info("[InitRunner] ✓ Successfully appointed '{}' as manager of '{}'", newManager, storeName);
                        }
                        failIfNeeded("appoint-manager", r);
                    }

                    default -> throw new IllegalArgumentException("Unknown useCase: " + useCase);
                }

            } catch (Exception e) {
                log.error("[InitRunner] ❌ Step {}/{} failed (useCase: {}): {}", i + 1, steps.size(), useCase, e.getMessage());
                log.error("[InitRunner] Current tokens available: {}", token.keySet());
                throw e; // Re-throw to fail the entire process
            }
        }

        log.info("[InitRunner] ✅ {} bootstrap steps replayed successfully", steps.size());
        log.info("[InitRunner] Final active tokens: {}", token.keySet());
    }

    /* ===================================================================
     *  Optional helper – keeps your "allow # comments" behaviour
     * ===================================================================*/
    public void replayFile(InputStream jsonStream) {
        try {
            log.info("[InitRunner] Processing JSON file with comment support...");
            String json = new BufferedReader(new InputStreamReader(jsonStream, StandardCharsets.UTF_8))
                    .lines()
                    .map(String::trim)
                    .filter(l -> !(l.isEmpty() || l.startsWith("#")))   // keep comment support
                    .collect(Collectors.joining());

            log.debug("[InitRunner] Processed JSON (comments removed): {}", json);
            replay(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        } catch (Exception ex) {
            log.error("[InitRunner] ❌ bootstrap replay failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Bootstrap replay failed", ex);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Utility methods                                                   */
    /* ------------------------------------------------------------------ */

    private void verifyServices() {
        log.info("[InitRunner] Verifying services are properly injected...");

        if (guestSrv == null) {
            log.error("[InitRunner] ❌ GuestService is null!");
            throw new IllegalStateException("GuestService not properly injected");
        }
        if (userSrv == null) {
            log.error("[InitRunner] ❌ UserService is null!");
            throw new IllegalStateException("UserService not properly injected");
        }
        if (storeSrv == null) {
            log.error("[InitRunner] ❌ StoreService is null!");
            throw new IllegalStateException("StoreService not properly injected");
        }
        if (mapper == null) {
            log.error("[InitRunner] ❌ ObjectMapper is null!");
            throw new IllegalStateException("ObjectMapper not properly injected");
        }

        log.info("[InitRunner] ✓ All services properly injected");
    }

    private static <T> void failIfNeeded(String uc, Result<T> r) {
        if (!r.isSuccess()) {
            throw new IllegalStateException(
                    "Bootstrap step '" + uc + "' failed: " + r.getErrorMessage());
        }
    }
}