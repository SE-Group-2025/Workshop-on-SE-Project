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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Replays <classpath>bootstrap/initial-state.json against the real services
 * so the data is written to the DB at startup or from tests.
 */
@Component
@Profile({ "prod", "db" })      // only when a DB profile is active
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
                          StoreService storeSrv,
                          ObjectMapper mapper) {

        this.guestSrv = guestSrv;
        this.userSrv  = userSrv;
        this.storeSrv = storeSrv;
        this.mapper   = mapper;
    }

    /* ------------------------------------------------------------------ */
    /*  Spring-Boot entry point                                           */
    /* ------------------------------------------------------------------ */
    @Override
    public void run(String... args) throws Exception {
        try (InputStream in = bootstrapJson.getInputStream()) {
            replay(in);                     // <<< NO line-splitting!
        } catch (FileNotFoundException ex) {
            log.warn("[InitRunner] bootstrap/initial-state.json not found – skipping");
        }
    }

    /* ===================================================================
     *  PUBLIC helper – tests / UnifiedDataSeeder can call this directly
     * ===================================================================*/
    public void replay(InputStream jsonStream) throws Exception {

        ArrayNode steps = (ArrayNode) mapper.readTree(jsonStream);
        Map<String, String> token = new HashMap<>();          // email → session

        for (JsonNode step : steps) {

            String    useCase  = step.get("useCase").asText();
            ArrayNode argArray = (ArrayNode) step.withArray("args");

            // flatten to List<JsonNode>
            List<JsonNode> a = new ArrayList<>();
            argArray.elements().forEachRemaining(a::add);

            switch (useCase) {

                /* ------------- registration / auth -------------------- */

                case "guest-registration" -> {
                    String email = a.get(0).asText();
                    String pwd   = a.get(1).asText();
                    userSrv.register(email, email, pwd);   // name=email to keep it simple
                }

                case "login" -> {
                    String email = a.get(0).asText();
                    String pwd   = a.get(1).asText();
                    String tk    = userSrv.login(email, pwd).getData();
                    token.put(email, tk);
                }

                case "logout" -> {
                    String tk = token.get(a.get(0).asText());
                    if (tk != null) userSrv.logout(tk);    // ignore if never logged in
                }

                /* ---------------- store operations ------------------- */

                case "open-shop" -> {
                    String owner     = a.get(0).asText();
                    String storeName = a.get(1).asText();

                    Result<Void> r = storeSrv.createStore(token.get(owner), storeName);
                    failIfNeeded("open-shop", r);

                    // optional patch object – not implemented yet
                    if (a.size() > 2 && !a.get(2).isNull()) {
                        log.info("[InitRunner] open-shop patch present – skipped");
                    }
                }

                case "add-product-to-store" -> {
                    String owner     = a.get(0).asText();
                    String storeName = a.get(1).asText();

                    Result<String> r = storeSrv.addProductToStore(
                            token.get(owner),
                            storeName,
                            a.get(2).asText(),     // catalog-id
                            a.get(3).asText(),     // name
                            a.get(4).asText(),     // description
                            a.get(5).asDouble(),   // price
                            a.get(6).asInt(),      // qty
                            null);                 // imageUrl
                    failIfNeeded("add-product-to-store", r);
                }

                case "appoint-owner" -> {
                    Result<Void> r = storeSrv.appointOwner(
                            token.get(a.get(0).asText()),     // session
                            a.get(1).asText(),                // store
                            a.get(2).asText());               // new owner
                    failIfNeeded("appoint-owner", r);
                }

                case "add-product-to-catalog" -> {
                    String id    = a.get(0).asText();
                    String name  = a.get(1).asText();
                    String brand = a.get(2).asText();
                    String desc  = a.get(3).asText();
                    List<String> cats =
                            mapper.convertValue(a.get(4), new TypeReference<List<String>>() {});
                    Result<String> r = storeSrv.addProductToCatalog(id, name, brand, desc, cats);
                    failIfNeeded("add-product-to-catalog", r);
                }

                case "appoint-manager" -> {
                    String owner       = a.get(0).asText();
                    List<String> perms = mapper.convertValue(
                            a.get(3), new TypeReference<List<String>>() {});
                    Result<Void> r = storeSrv.appointManager(
                            token.get(owner),
                            a.get(1).asText(),              // store
                            a.get(2).asText(),              // new manager
                            perms);
                    failIfNeeded("appoint-manager", r);
                }

                default -> throw new IllegalArgumentException("Unknown useCase: " + useCase);
            }
        }

        log.info("[InitRunner] ✔ {} bootstrap steps replayed", steps.size());
    }

    /* ===================================================================
     *  Optional helper – keeps your “allow # comments” behaviour
     * ===================================================================*/
    public void replayFile(InputStream jsonStream) {
        try {
            String json = new BufferedReader(new InputStreamReader(jsonStream, StandardCharsets.UTF_8))
                    .lines()
                    .map(String::trim)
                    .filter(l -> !(l.isEmpty() || l.startsWith("#")))   // keep comment support
                    .collect(Collectors.joining());
            replay(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            log.error("[InitRunner] ❌ bootstrap replay failed: {}", ex.getMessage(), ex);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Utility                                                           */
    /* ------------------------------------------------------------------ */
    private static <T> void failIfNeeded(String uc, Result<T> r) {
        if (!r.isSuccess()) {
            throw new IllegalStateException(
                    "Bootstrap step '" + uc + "' failed: " + r.getErrorMessage());
        }
    }
}
