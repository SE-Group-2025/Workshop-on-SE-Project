package com.SEGroup.UI;

import com.SEGroup.Infrastructure.ExternalPaymentAndShippingService;
import com.SEGroup.Infrastructure.PasswordEncoder;
import com.SEGroup.Infrastructure.Repositories.*;
import com.SEGroup.Infrastructure.Repositories.JpaDatabase.*;
import com.SEGroup.Infrastructure.Repositories.RepositoryData.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * • First ever run on an empty DB → heavy demo dump + bootstrap script.<br>
 * • Later restarts                → only the bootstrap script (idempotent).
 */
@Component
public class UnifiedDataSeeder implements ApplicationListener<ApplicationReadyEvent> {

    /* ---------------------------------------------------------- */
    /*  Logger                                                    */
    /* ---------------------------------------------------------- */
    private static final Logger log = LoggerFactory.getLogger(UnifiedDataSeeder.class);

    /* ---------------------------------------------------------- */
    /*  Repositories & infrastructure                             */
    /* ---------------------------------------------------------- */
    private UserRepository           users;
    private StoreRepository          stores;
    private ProductCatalogRepository catalog;
    private TransactionRepository    transactions;
    private final GuestRepository    guests;
    private final ExternalPaymentAndShippingService shippingService;
    private final InitFileRunner     initFileRunner;     // JSON-script runner
    /* ---------------------------------------------------------- */
    /*  Raw JPA repos – wrapped in @PostConstruct                 */
    /* ---------------------------------------------------------- */
    @Autowired private JpaUserRepository        jpaUserRepository;
    @Autowired private JpaStoreRepository       jpaStoreRepository;
    @Autowired private JpaTransactionRepository jpaTransactionRepository;


    /* ---------------- constructor ---------------- */
    @Autowired
    public UnifiedDataSeeder(UserRepository                    users,
                             StoreRepository                   stores,
                             ProductCatalogRepository          catalog,
                             TransactionRepository             transactions,
                             GuestRepository                   guests,
                             ExternalPaymentAndShippingService shippingService,
                             InitFileRunner                    initFileRunner) {

        this.users           = users;
        this.stores          = stores;
        this.catalog         = catalog;
        this.transactions    = transactions;
        this.guests          = guests;
        this.shippingService = shippingService;
        this.initFileRunner  = initFileRunner;
    }


    /*  Wrap the raw JPA implementations in our repository abstractions */
    @PostConstruct
    void wireRepos() {
        this.users        = new UserRepository(new DbUserData(jpaUserRepository));
        this.stores       = new StoreRepository(new DbStoreData(jpaStoreRepository));
        this.transactions = new TransactionRepository(new DbTransactionData(jpaTransactionRepository));
        /* catalog already wired correctly */
    }

    /* ---------------------------------------------------------- */
    /*  APPLICATION STARTUP                                       */
    /* ---------------------------------------------------------- */
    @Override
    @Transactional          // rollback if FIRST heavy-seed fails
    public void onApplicationEvent(ApplicationReadyEvent event) {

        /* ----------------------------------------------------------
         * 0) Is the database brand-new?
         *    -----------------------------------------
         * IMPORTANT: do this *before* we insert the six baseline
         * demo users, otherwise the test will never be true.
         * ---------------------------------------------------------- */
        boolean freshDb = users.getAllEmails().isEmpty();
        log.info("[seeder] freshDb = {}", freshDb);

        /* ----------------------------------------------------------
         * 1) Always make sure the six baseline accounts exist
         * ---------------------------------------------------------- */
        log.info("[seeder] Ensuring baseline demo accounts…");
        ensureUser("u1@example.com", "p1");
        ensureUser("u2@example.com", "p2");
        ensureUser("u3@example.com", "p3");
        ensureUser("u4@example.com", "p4");
        ensureUser("u5@example.com", "p5");
        ensureUser("u6@example.com", "p6");

        /* ----------------------------------------------------------
         * 2) Publish repositories to the ServiceLocator
         * ---------------------------------------------------------- */
        ServiceLocator.initialize(
                guests, users, transactions, stores, catalog, shippingService);

        /* ----------------------------------------------------------
         * 3) Re-play the bootstrap JSON script on *every* boot
         *    (it is written to be idempotent)
         * ---------------------------------------------------------- */
        applyBootstrapJson();        //  ←  **DON’T comment this out**

        /* ----------------------------------------------------------
         * 4) Insert the heavy demo data once, on a truly empty DB
         * ---------------------------------------------------------- */
        if (freshDb) {
            seedHeavyDemoData();
        }

        log.info("[seeder] ✓ Startup data seeding complete.");
    }

    /* ---------------------------------------------------------- */
    /*  HELPERS                                                   */
    /* ---------------------------------------------------------- */

    private void ensureUser(String email, String rawPwd) {
        if (users.getAllEmails().contains(email)) return;
        users.addUser(email, email, new PasswordEncoder().encrypt(rawPwd));
    }

    private void applyBootstrapJson() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("bootstrap/initial-state.json")) {

            if (in == null) {
                log.warn("[seeder] (bootstrap/initial-state.json missing)");
                return;
            }

            initFileRunner.replayFile(in);
            log.info("[seeder] ✓ bootstrap script applied");

        } catch (Exception ex) {
            // keep running – just log the error
            log.error("[seeder] ❌ bootstrap replay failed: {}", ex.getMessage(), ex);
        }
    }

    /* ---------------------------------------------------------- */
    /*  === HEAVY DEMO DATA – UNCHANGED FROM YOUR ORIGINAL ===    */
    /* ---------------------------------------------------------- */

    private void seedHeavyDemoData() {
        log.info("[seeder] No users found → inserting heavy demo data…");
        PasswordEncoder encoder = new PasswordEncoder();

        /************ 1) USERS *******************************************/
        users.addUser("System Admin",   "admin@demo.com",       encoder.encrypt("admin123"));
        users.addUser("Demo Owner",     "owner@demo.com",       encoder.encrypt("demo123"));
        users.addUser("Co-owner",       "co-owner@demo.com",    encoder.encrypt("demo123"));
        users.addUser("Tech Store",     "tech@demo.com",        encoder.encrypt("demo123"));
        users.addUser("Fashion Owner",  "fashion@demo.com",     encoder.encrypt("demo123"));
        users.addUser("Home Owner",     "home@demo.com",        encoder.encrypt("demo123"));
        users.addUser("Regular User",   "user@demo.com",        encoder.encrypt("demo123"));
        users.addUser("Shopper One",    "shopper1@demo.com",    encoder.encrypt("demo123"));
        users.addUser("Shopper Two",    "shopper2@demo.com",    encoder.encrypt("demo123"));
        users.addUser("Tech Fan",       "tech.fan@demo.com",    encoder.encrypt("demo123"));
        users.addUser("Fashion Lover",  "fashionista@demo.com", encoder.encrypt("demo123"));

        /************ 2) STORES ******************************************/
        String demoStore    = "Demo Store";
        String techStore    = "Tech Gadgets";
        String fashionStore = "Fashion Hub";
        String homeStore    = "Home Essentials";

        stores.createStore(demoStore, "owner@demo.com");
        stores.appointOwner  (demoStore, "owner@demo.com", "co-owner@demo.com", false);
        stores.updateStoreDescription(demoStore, "owner@demo.com",
                "Your one-stop shop for premium electronics, fashion and home goods.");

        stores.createStore(techStore, "tech@demo.com");
        stores.appointManager(techStore, "tech@demo.com", "user@demo.com",
                List.of("VIEW_ONLY", "MANAGE_PRODUCTS"), false);
        stores.updateStoreDescription(techStore, "tech@demo.com",
                "Cutting-edge technology at affordable prices.");

        stores.createStore(fashionStore, "fashion@demo.com");
        stores.updateStoreDescription(fashionStore, "fashion@demo.com",
                "Trendy fashion items from top designers.");

        stores.createStore(homeStore, "home@demo.com");
        stores.updateStoreDescription(homeStore, "home@demo.com",
                "Everything you need to make your house a home.");

        /************ 3) CATALOG *****************************************/
        catalog.addCatalogProduct("TECH-001", "Smartphone X",      "BrandA",
                "Flagship smartphone with 6.1\" OLED display, 12 MP camera, 5G connectivity.",
                List.of("Electronics", "Phones"));
        catalog.addCatalogProduct("TECH-002", "Laptop Pro",        "Dell",
                "High-performance laptop: latest CPU, 32 GB RAM, 1 TB SSD, dedicated GPU.",
                List.of("Electronics", "Computers"));
        catalog.addCatalogProduct("TECH-003", "Wireless Earbuds",  "Acme Audio",
                "Premium earbuds with ANC, transparency mode, wireless charging case.",
                List.of("Electronics", "Audio"));
        catalog.addCatalogProduct("TECH-004", "Smart Watch Elite", "BrandC",
                "Smart-watch with cellular, ECG monitor, premium bands.",
                List.of("Electronics", "Wearables"));
        catalog.addCatalogProduct("TECH-005", "4K Gaming Monitor", "BrandD",
                "34\" ultrawide curved monitor, 165 Hz, G-Sync.",
                List.of("Electronics", "Displays"));

        catalog.addCatalogProduct("FASH-001", "Designer T-Shirt",  "BrandE",
                "Limited-edition designer tee on organic cotton.",
                List.of("Clothing", "T-Shirts"));
        catalog.addCatalogProduct("FASH-002", "Leather Jacket",    "BrandF",
                "Hand-crafted genuine leather jacket with quilted lining.",
                List.of("Clothing", "Outerwear"));
        catalog.addCatalogProduct("FASH-003", "Running Shoes",     "BrandG",
                "Responsive cushioning, breathable upper – perfect for runners.",
                List.of("Footwear", "Sports"));
        catalog.addCatalogProduct("FASH-004", "Signature Tote Bag","BrandH",
                "Premium leather-trimmed canvas tote.",
                List.of("Accessories", "Bags"));
        catalog.addCatalogProduct("FASH-005", "Silk Scarf",        "BrandI",
                "Hand-painted luxurious silk scarf.",
                List.of("Accessories", "Scarves"));

        catalog.addCatalogProduct("HOME-001", "Coffee Maker Deluxe","BrandJ",
                "Fully automatic coffee maker: grinder, timer, thermal carafe.",
                List.of("Home", "Kitchen"));
        catalog.addCatalogProduct("HOME-002", "Smart Vacuum Robot", "BrandK",
                "AI-powered vacuum with mapping and scheduling.",
                List.of("Home", "Cleaning"));
        catalog.addCatalogProduct("HOME-003", "Luxury Bedding Set", "BrandL",
                "1000-thread-count Egyptian cotton bedding set.",
                List.of("Home", "Bedroom"));
        catalog.addCatalogProduct("HOME-004", "Chef's Knife Set",   "BrandM",
                "Hand-forged chef’s knives with premium steel blades.",
                List.of("Home", "Kitchen"));
        catalog.addCatalogProduct("HOME-005", "Smart Home Hub",     "BrandN",
                "Central hub for all smart devices, voice control, security.",
                List.of("Home", "Smart Home"));

        /************ 4) STORE PRODUCTS **********************************/
        String p1 = stores.addProductToStore("owner@demo.com", demoStore, "TECH-001", "Smartphone X Pro",
                "Flagship smartphone with 6.7\" AMOLED, 108 MP camera, 5G.", 999.99, 10, true,
                getProductImage("phone"), List.of("Electronics", "Phones"));
        stores.startAuction("owner@demo.com", demoStore, p1, 100.00,
                new Date(System.currentTimeMillis() + 5 * 60_000L));   // 5 min

        String p2 = stores.addProductToStore("owner@demo.com", demoStore, "TECH-002", "Laptop Pro Max",
                "Latest CPU, 32 GB RAM, 1 TB SSD – ideal for designers & gamers.", 1499.99, 5, true,
                getProductImage("laptop"), List.of("Electronics", "Computers"));

        String p3 = stores.addProductToStore("owner@demo.com", demoStore, "HOME-001", "Coffee Maker Deluxe",
                "Barista-quality coffee maker: grinder, timer, carafe.", 129.99, 20, true,
                getProductImage("coffee"), List.of("Home", "Kitchen"));

        String p4 = stores.addProductToStore("owner@demo.com", demoStore, "FASH-002", "Premium Leather Jacket",
                "Hand-crafted genuine leather jacket, quilted lining.", 349.99, 8, true,
                getProductImage("jacket"), List.of("Clothing", "Outerwear"));

        /************ 7) DEMO TRANSACTIONS *******************************/
        transactions.addTransaction(List.of(p1, p3), 1129.98, demoStore, "user@demo.com");
        transactions.addTransaction(List.of(p2),      1499.99, demoStore, "shopper1@demo.com");
        transactions.addTransaction(List.of(p4),       349.99, demoStore, "shopper2@demo.com");

        log.info("[seeder] Heavy demo-data insertion complete.");
    }

    /* ---------------------------------------------------------- */
    /*  STOCK-PHOTO PICKER – your original code (unchanged)       */
    /* ---------------------------------------------------------- */
    private String getProductImage(String keyword) {
        String key = keyword == null ? "" : keyword.toLowerCase();
        if (key.contains("phone"))   return "https://images.unsplash.com/photo-1512499617640-c2f999098c63?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("laptop"))  return "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("earbud") || key.contains("audio") || key.contains("headphone"))
            return "https://images.unsplash.com/photo-1585386959984-a41552231617?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("watch"))   return "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("monitor")) return "https://images.unsplash.com/photo-1587825140708-030e382b97c8?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("jacket"))  return "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("shirt") || key.contains("tee") || key.contains("graphic"))
            return "https://images.unsplash.com/photo-1523289333742-bea4fa21c8f8?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("shoe"))    return "https://images.unsplash.com/photo-1511974035430-5de47d3b95da?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("bag") || key.contains("tote"))
            return "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("scarf"))   return "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("coffee"))  return "https://images.unsplash.com/photo-1511920170033-f8396924c348?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("vacuum") || key.contains("robot"))
            return "https://images.unsplash.com/photo-1615540122321-9b9371bd3432?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("bedding") || key.contains("sheet"))
            return "https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("knife"))   return "https://images.unsplash.com/photo-1526040652367-ac003a0475fe?auto=format&fit=crop&w=400&h=400&q=80";
        if (key.contains("smart") && key.contains("home"))
            return "https://images.unsplash.com/photo-1518444024084-31c5455b03d7?auto=format&fit=crop&w=400&h=400&q=80";
        return "https://images.unsplash.com/photo-1585386959984-a41552231617?auto=format&fit=crop&w=400&h=400&q=80";
    }
}
