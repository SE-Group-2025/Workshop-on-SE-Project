// src/main/java/com/SEGroup/Infrastructure/Bootstrap/UnifiedDataSeeder.java
package com.SEGroup.UI;

import com.SEGroup.Infrastructure.ExternalPaymentAndShippingService;
import com.SEGroup.Infrastructure.PasswordEncoder;
import com.SEGroup.Infrastructure.Repositories.*;
import com.SEGroup.Infrastructure.Repositories.JpaDatabase.*;
import com.SEGroup.Infrastructure.Repositories.RepositoryData.DbProductCatalog;
import com.SEGroup.Infrastructure.Repositories.RepositoryData.DbStoreData;
import com.SEGroup.Infrastructure.Repositories.RepositoryData.DbTransactionData;
import com.SEGroup.Infrastructure.Repositories.RepositoryData.DbUserData;
import com.SEGroup.Service.LoggerWrapper;
import com.SEGroup.UI.ServiceLocator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * This component runs on every startup (no @Profile).
 * It looks for existing users; if none exist, it seeds:
 *    1) users
 *    2) stores + owners/managers
 *    3) catalog products
 *    4) store‐product entries
 *    5) ratings (stores + products)
 *    6) ServiceLocator.initialize(...) so that all services can grab their repos
 *
 * On subsequent restarts (prod or any other profile), once users exist
 * it simply returns and does nothing.
 */
@Component
public class UnifiedDataSeeder implements ApplicationListener<ApplicationReadyEvent> {

    private  UserRepository users;
    private  StoreRepository stores;
    private  ProductCatalogRepository catalog;
    private  TransactionRepository transactions;
    private final GuestRepository guests;
    private final ExternalPaymentAndShippingService shippingService;
    private final InitFileRunner initFileRunner;
    // JSON-script runner
    public UnifiedDataSeeder(
            UserRepository users,
            StoreRepository stores,
            ProductCatalogRepository catalog,
            TransactionRepository transactions,
            GuestRepository guests,
            ExternalPaymentAndShippingService shippingService,
            InitFileRunner initFileRunner
    ) {
        this.users = users;
        this.stores = stores;
        this.catalog = catalog;
        this.transactions = transactions;
        this.guests = guests;
        this.shippingService = shippingService;
        this.initFileRunner = initFileRunner;
    }
    @Autowired
    private JpaUserRepository jpaUserRepository;
    @Autowired
    private JpaStoreRepository jpaStoreRepository;
    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

    @PostConstruct
    public void init() {
        //discount available
        this.users = new UserRepository(new DbUserData(jpaUserRepository));
        this.stores = new StoreRepository(new DbStoreData(jpaStoreRepository));
        this.transactions = new TransactionRepository(new DbTransactionData(jpaTransactionRepository));
        // קריאות ל-service/DTO/Seeder methods
    }


    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 1) If there is already at least one user in the DB, skip everything.
        if (users.getAllEmails().size() > 1) {
            System.out.println("[seeder] Users already exist → skipping demo data.");
            // Still initialize ServiceLocator for everyone else:
            ServiceLocator.initialize(
                    guests, users, transactions, stores, catalog, shippingService
            );
            return;
        }
        System.out.println("[seeder] No users found → inserting demo data…");
        PasswordEncoder encoder = new PasswordEncoder();

        /************************************************************************
         * STEP 1: CREATE USERS
         ************************************************************************/
        System.out.println("[seeder] 1) Adding users...");
        // Admin
        users.addUser("System Admin",   "admin@demo.com",     encoder.encrypt("admin123"));

        // Store owners / co-owners / managers
        users.addUser("Demo Owner",       "owner@demo.com",     encoder.encrypt("demo123"));
        users.addUser("Co-owner",         "co-owner@demo.com",  encoder.encrypt("demo123"));
        users.addUser("Tech Store Owner", "tech@demo.com",      encoder.encrypt("demo123"));
        users.addUser("Fashion Owner",    "fashion@demo.com",   encoder.encrypt("demo123"));
        users.addUser("Home Owner",       "home@demo.com",      encoder.encrypt("demo123"));

        // Regular shoppers
        users.addUser("Regular User",    "user@demo.com",      encoder.encrypt("demo123"));
        users.addUser("Shopper One",     "shopper1@demo.com",  encoder.encrypt("demo123"));
        users.addUser("Shopper Two",     "shopper2@demo.com",  encoder.encrypt("demo123"));
        users.addUser("Tech Enthusiast", "tech.fan@demo.com",  encoder.encrypt("demo123"));
        users.addUser("Fashion Lover",   "fashionista@demo.com",encoder.encrypt("demo123"));

        /************************************************************************
         * STEP 2: CREATE STORES + ASSIGN OWNERS/MANAGERS
         ************************************************************************/
        System.out.println("[seeder] 2) Creating stores and assigning roles...");
        // Demo Store
        String demoStore = "Demo Store";
        stores.createStore(demoStore, "owner@demo.com");
        stores.appointOwner(demoStore, "owner@demo.com",     "co-owner@demo.com",     false);
        stores.updateStoreDescription(
                demoStore,
                "owner@demo.com",
                "Your one-stop shop for premium electronics, fashion, and home goods."
        );

        // Tech Store
        String techStore = "Tech Gadgets";
        stores.createStore(techStore, "tech@demo.com");
        stores.appointManager(techStore, "tech@demo.com", "user@demo.com",
                List.of("VIEW_ONLY","MANAGE_PRODUCTS"), false);
        stores.updateStoreDescription(
                techStore,
                "tech@demo.com",
                "Cutting-edge technology at affordable prices. Everything a tech enthusiast needs."
        );

        // Fashion Store
        String fashionStore = "Fashion Hub";
        stores.createStore(fashionStore, "fashion@demo.com");
        stores.updateStoreDescription(
                fashionStore,
                "fashion@demo.com",
                "Trendy fashion items from top designers. Stay stylish with our curated collections."
        );

        // Home Store
        String homeStore = "Home Essentials";
        stores.createStore(homeStore, "home@demo.com");
        stores.updateStoreDescription(
                homeStore,
                "home@demo.com",
                "Everything you need to make your house a home—quality appliances, bedding, and decor."
        );

        /************************************************************************
         * STEP 3: ADD CATALOG PRODUCTS
         ************************************************************************/
        System.out.println("[seeder] 3) Adding catalog products...");
        // Tech
        catalog.addCatalogProduct("TECH-001", "Smartphone X",       "BrandA",
                "Flagship smartphone with 6.1\" OLED display, 12 MP camera, 5G connectivity.",
                List.of("Electronics","Phones"));
        catalog.addCatalogProduct("TECH-002", "Laptop Pro",         "Dell",
                "High-performance laptop: latest CPU, 32GB RAM, 1TB SSD, dedicated GPU.",
                List.of("Electronics","Computers"));
        catalog.addCatalogProduct("TECH-003", "Wireless Earbuds",   "Acme Audio",
                "Premium earbuds: ANC, transparency mode, wireless charging case.",
                List.of("Electronics","Audio"));
        catalog.addCatalogProduct("TECH-004", "Smart Watch Elite",  "BrandC",
                "Smartwatch w/ cellular, ECG monitor, premium bands.",
                List.of("Electronics","Wearables"));
        catalog.addCatalogProduct("TECH-005", "4K Gaming Monitor",  "BrandD",
                "34\" ultrawide curved monitor, 165Hz, G-Sync.",
                List.of("Electronics","Displays"));

        // Fashion
        catalog.addCatalogProduct("FASH-001", "Designer T-Shirt",   "BrandE",
                "Limited edition designer tee on organic cotton.",
                List.of("Clothing","T-Shirts"));
        catalog.addCatalogProduct("FASH-002", "Leather Jacket",     "BrandF",
                "Handcrafted genuine leather jacket with quilted lining.",
                List.of("Clothing","Outerwear"));
        catalog.addCatalogProduct("FASH-003", "Running Shoes",      "BrandG",
                "Responsive cushioning, breathable upper—perfect for runners.",
                List.of("Footwear","Sports"));
        catalog.addCatalogProduct("FASH-004", "Signature Tote Bag", "BrandH",
                "Premium leather-trimmed canvas tote.",
                List.of("Accessories","Bags"));
        catalog.addCatalogProduct("FASH-005", "Silk Scarf",         "BrandI",
                "Hand-painted luxurious silk scarf.",
                List.of("Accessories","Scarves"));

        // Home
        catalog.addCatalogProduct("HOME-001", "Coffee Maker Deluxe","BrandJ",
                "Fully automatic coffee maker: grinder, timer, thermal carafe.",
                List.of("Home","Kitchen"));
        catalog.addCatalogProduct("HOME-002", "Smart Vacuum Robot", "BrandK",
                "AI-powered vacuum with mapping and scheduling.",
                List.of("Home","Cleaning"));
        catalog.addCatalogProduct("HOME-003", "Luxury Bedding Set", "BrandL",
                "1000‐thread‐count Egyptian cotton bedding set.",
                List.of("Home","Bedroom"));
        catalog.addCatalogProduct("HOME-004", "Chef's Knife Set",   "BrandM",
                "Hand‐forged chef’s knives with premium steel blades.",
                List.of("Home","Kitchen"));
        catalog.addCatalogProduct("HOME-005", "Smart Home Hub",     "BrandN",
                "Central hub for all smart devices, voice control, security.",
                List.of("Home","Smart Home"));

        /************************************************************************
         * STEP 4: ADD STORE‐PRODUCT ENTRIES
         ************************************************************************/
        System.out.println("[seeder] 4) Adding store‐product entries...");

        // ––– Demo Store products –––
        System.out.println("  • Demo Store items...");
        String p1 = stores.addProductToStore(
                "owner@demo.com", demoStore,
                "TECH-001",
                "Smartphone X Pro",
                "Flagship smartphone with 6.7\" AMOLED, 108MP camera, 5G.",
                999.99, 10, true,
                getProductImage("phone"),
                List.of("Electronics","Phones")
        );
        stores.startAuction(
                "owner@demo.com", demoStore, p1,
                100.00,
                new Date(System.currentTimeMillis() + 5 * 60_000L) // 5 minutes from now
        );

        String p2 = stores.addProductToStore(
                "owner@demo.com", demoStore,
                "TECH-002",
                "Laptop Pro Max",
                "Latest CPU, 32GB RAM, 1TB SSD, ideal for designers & gamers.",
                1_499.99, 5, true,
                getProductImage("laptop"),
                List.of("Electronics","Computers")
        );

        String p3 = stores.addProductToStore(
                "owner@demo.com", demoStore,
                "HOME-001",
                "Coffee Maker Deluxe",
                "Barista‐quality coffee maker: grinder, timer, carafe.",
                129.99, 20, true,
                getProductImage("coffee"),
                List.of("Home","Kitchen")
        );

        String p4 = stores.addProductToStore(
                "owner@demo.com", demoStore,
                "FASH-002",
                "Premium Leather Jacket",
                "Handcrafted genuine leather jacket, quilted lining.",
                349.99, 8, true,
                getProductImage("jacket"),
                List.of("Clothing","Outerwear")
        );

        // Register Demo Store items into the catalog so they appear in searches:
        catalog.addStoreProductEntryWithImage(
                "TECH-001", demoStore, p1, 999.99, 10, 4.8, "Smartphone X Pro",
                getProductImage("phone")
        );
        catalog.addStoreProductEntryWithImage(
                "TECH-002", demoStore, p2, 1_499.99, 5, 4.6, "Laptop Pro Max",
                getProductImage("laptop")
        );
        catalog.addStoreProductEntryWithImage(
                "HOME-001", demoStore, p3, 129.99, 20, 4.5, "Coffee Maker Deluxe",
                getProductImage("coffee")
        );
        catalog.addStoreProductEntryWithImage(
                "FASH-002", demoStore, p4, 349.99, 8, 4.7, "Premium Leather Jacket",
                getProductImage("jacket")
        );

        // ––– Tech Store products –––
        System.out.println("  • Tech Store items...");
        String p5 = stores.addProductToStore(
                "tech@demo.com", techStore,
                "TECH-001",
                "Smartphone X Limited Edition",
                "Exclusive version: extended warranty, premium color.",
                1_049.99, 15, true,
                getProductImage("phone"),
                List.of("Electronics","Phones")
        );
        String p6 = stores.addProductToStore(
                "tech@demo.com", techStore,
                "TECH-002",
                "Laptop Pro Developer Edition",
                "Linux‐preinstalled, extended battery, free carrying case.",
                1_649.99, 8, true,
                getProductImage("laptop"),
                List.of("Electronics","Computers")
        );
        String p7 = stores.addProductToStore(
                "tech@demo.com", techStore,
                "TECH-003",
                "Wireless Earbuds Pro",
                "ANC, transparency mode, wireless charging case.",
                179.99, 30, true,
                getProductImage("earbud"),
                List.of("Electronics","Audio")
        );
        String p8 = stores.addProductToStore(
                "tech@demo.com", techStore,
                "TECH-004",
                "Smart Watch Elite GPS",
                "Smartwatch with cellular, ECG monitor, premium bands.",
                349.99, 12, true,
                getProductImage("watch"),
                List.of("Electronics","Wearables")
        );
        String p9 = stores.addProductToStore(
                "tech@demo.com", techStore,
                "TECH-005",
                "Ultra Gaming Monitor",
                "34\" ultrawide curved monitor, 165Hz, G-Sync.",
                699.99, 7, true,
                getProductImage("monitor"),
                List.of("Electronics","Displays")
        );

        catalog.addStoreProductEntryWithImage(
                "TECH-001", techStore, p5, 1_049.99, 15, 4.9, "Smartphone X Limited Edition",
                getProductImage("phone")
        );
        catalog.addStoreProductEntryWithImage(
                "TECH-002", techStore, p6, 1_649.99, 8, 4.7, "Laptop Pro Developer Edition",
                getProductImage("laptop")
        );
        catalog.addStoreProductEntryWithImage(
                "TECH-003", techStore, p7, 179.99, 30, 4.6, "Wireless Earbuds Pro",
                getProductImage("earbud")
        );
        catalog.addStoreProductEntryWithImage(
                "TECH-004", techStore, p8, 349.99, 12, 4.8, "Smart Watch Elite GPS",
                getProductImage("watch")
        );
        catalog.addStoreProductEntryWithImage(
                "TECH-005", techStore, p9, 699.99, 7, 4.9, "Ultra Gaming Monitor",
                getProductImage("monitor")
        );

        // ––– Fashion Store products –––
        System.out.println("  • Fashion Store items...");
        String p10 = stores.addProductToStore(
                "fashion@demo.com", fashionStore,
                "FASH-001",
                "Designer Graphic Tee",
                "Limited edition designer tee on organic cotton.",
                59.99, 100, true,
                getProductImage("shirt"),
                List.of("Clothing","T-Shirts")
        );
        String p11 = stores.addProductToStore(
                "fashion@demo.com", fashionStore,
                "FASH-002",
                "Vintage Leather Jacket",
                "Classic distressed leather jacket.",
                329.99, 15, true,
                getProductImage("jacket"),
                List.of("Clothing","Outerwear")
        );
        String p12 = stores.addProductToStore(
                "fashion@demo.com", fashionStore,
                "FASH-003",
                "Performance Running Shoes",
                "Responsive cushioning, breathable materials.",
                149.99, 50, true,
                getProductImage("shoe"),
                List.of("Footwear","Sports")
        );
        String p13 = stores.addProductToStore(
                "fashion@demo.com", fashionStore,
                "FASH-004",
                "Signature Tote Bag",
                "Premium leather-trimmed canvas tote.",
                199.99, 25, true,
                getProductImage("bag"),
                List.of("Accessories","Bags")
        );
        String p14 = stores.addProductToStore(
                "fashion@demo.com", fashionStore,
                "FASH-005",
                "Designer Silk Scarf",
                "Hand-painted luxurious silk scarf.",
                89.99, 40, true,
                getProductImage("scarf"),
                List.of("Accessories","Scarves")
        );

        catalog.addStoreProductEntryWithImage(
                "FASH-001", fashionStore, p10, 59.99, 100, 4.5, "Designer Graphic Tee",
                getProductImage("shirt")
        );
        catalog.addStoreProductEntryWithImage(
                "FASH-002", fashionStore, p11, 329.99, 15, 4.8, "Vintage Leather Jacket",
                getProductImage("jacket")
        );
        catalog.addStoreProductEntryWithImage(
                "FASH-003", fashionStore, p12, 149.99, 50, 4.6, "Performance Running Shoes",
                getProductImage("shoe")
        );
        catalog.addStoreProductEntryWithImage(
                "FASH-004", fashionStore, p13, 199.99, 25, 4.7, "Signature Tote Bag",
                getProductImage("bag")
        );
        catalog.addStoreProductEntryWithImage(
                "FASH-005", fashionStore, p14, 89.99, 40, 4.4, "Designer Silk Scarf",
                getProductImage("scarf")
        );

        // ––– Home Store products –––
        System.out.println("  • Home Store items...");
        String p15 = stores.addProductToStore(
                "home@demo.com", homeStore,
                "HOME-001",
                "Barista Coffee Maker",
                "Professional coffee maker with grinder, frother, pressure control.",
                199.99, 18, true,
                getProductImage("coffee"),
                List.of("Home","Kitchen")
        );
        String p16 = stores.addProductToStore(
                "home@demo.com", homeStore,
                "HOME-002",
                "Smart Robot Vacuum",
                "AI-powered mapping vacuum with scheduling and app control.",
                299.99, 10, true,
                getProductImage("vacuum"),
                List.of("Home","Cleaning")
        );
        String p17 = stores.addProductToStore(
                "home@demo.com", homeStore,
                "HOME-003",
                "Premium Egyptian Cotton Bedding",
                "1000 thread count Egyptian cotton bedding set.",
                249.99, 25, true,
                getProductImage("bedding"),
                List.of("Home","Bedroom")
        );
        String p18 = stores.addProductToStore(
                "home@demo.com", homeStore,
                "HOME-004",
                "Professional Knife Collection",
                "Hand-forged chef’s knives with premium steel blades.",
                179.99, 15, true,
                getProductImage("knife"),
                List.of("Home","Kitchen")
        );
        String p19 = stores.addProductToStore(
                "home@demo.com", homeStore,
                "HOME-005",
                "Smart Home Control Center",
                "Central hub for all smart home devices, voice control, security.",
                159.99, 20, true,
                getProductImage("smarthome"),
                List.of("Home","Smart Home")
        );

        catalog.addStoreProductEntryWithImage(
                "HOME-001", homeStore, p15, 199.99, 18, 4.7, "Barista Coffee Maker",
                getProductImage("coffee")
        );
        catalog.addStoreProductEntryWithImage(
                "HOME-002", homeStore, p16, 299.99, 10, 4.5, "Smart Robot Vacuum",
                getProductImage("vacuum")
        );
        catalog.addStoreProductEntryWithImage(
                "HOME-003", homeStore, p17, 249.99, 25, 4.9, "Premium Egyptian Cotton Bedding",
                getProductImage("bedding")
        );
        catalog.addStoreProductEntryWithImage(
                "HOME-004", homeStore, p18, 179.99, 15, 4.8, "Professional Knife Collection",
                getProductImage("knife")
        );
        catalog.addStoreProductEntryWithImage(
                "HOME-005", homeStore, p19, 159.99, 20, 4.6, "Smart Home Control Center",
                getProductImage("smarthome")
        );

        /************************************************************************
         * STEP 5: ADD RATINGS & REVIEWS
         ************************************************************************/
        System.out.println("[seeder] 5) Adding ratings & reviews...");

        // Demo Store ratings
        stores.rateStore("user@demo.com",      demoStore, 5, "Excellent selection and fast shipping!");
        stores.rateStore("shopper1@demo.com",  demoStore, 4, "Good prices and quality products.");
        stores.rateStore("shopper2@demo.com",  demoStore, 5, "Great customer service and range.");
        stores.rateStore("tech.fan@demo.com",  demoStore, 4, "Wide variety of tech goodies.");

        // Tech Store ratings
        stores.rateStore("user@demo.com",      techStore, 5, "Best tech store ever!");
        stores.rateStore("shopper1@demo.com",  techStore, 5, "Amazing selection of gadgets.");
        stores.rateStore("shopper2@demo.com",  techStore, 4, "Great pricing on high-end tech.");

        // Fashion Store ratings
        stores.rateStore("user@demo.com",       fashionStore, 4, "Trendy items at reasonable prices.");
        stores.rateStore("fashionista@demo.com",fashionStore, 5, "Love these designer collections!");
        stores.rateStore("shopper1@demo.com",   fashionStore, 5, "High-quality items and fast delivery.");

        // Home Store ratings
        stores.rateStore("user@demo.com",      homeStore, 5, "Great selection of home goods!");
        stores.rateStore("shopper2@demo.com",  homeStore, 4, "Quality kitchen products at good prices.");

        // Demo Store product ratings
        stores.rateProduct("user@demo.com",       demoStore, p1, 5, "Best smartphone I've ever owned!");
        stores.rateProduct("shopper1@demo.com",   demoStore, p1, 4, "Great phone with excellent features.");
        stores.rateProduct("tech.fan@demo.com",   demoStore, p1, 5, "Incredible performance and camera!");
        stores.rateProduct("user@demo.com",       demoStore, p2, 5, "Perfect for design work—fast & powerful!");
        stores.rateProduct("tech.fan@demo.com",   demoStore, p2, 4, "Great performance for dev & gaming.");
        stores.rateProduct("shopper1@demo.com",   demoStore, p3, 5, "Makes delicious coffee, easy to use!");
        stores.rateProduct("shopper2@demo.com",   demoStore, p4, 4, "Quality leather, comfortable fit.");

        // Tech Store product ratings
        stores.rateProduct("user@demo.com",       techStore, p5, 5, "Limited edition is worth it!");
        stores.rateProduct("shopper1@demo.com",   techStore, p5, 5, "Amazing exclusive features.");
        stores.rateProduct("tech.fan@demo.com",   techStore, p7, 5, "Daily driver—sound quality is amazing!");
        stores.rateProduct("shopper2@demo.com",   techStore, p8, 4, "Loved the health features on this watch.");
        stores.rateProduct("shopper1@demo.com",   techStore, p9, 5, "Incredible gaming experience on this monitor!");

        // Fashion Store product ratings
        stores.rateProduct("fashionista@demo.com",fashionStore,p10,5, "Super stylish and comfortable!");
        stores.rateProduct("shopper1@demo.com",   fashionStore,p11,4, "Beautiful leather jacket, fits perfectly.");
        stores.rateProduct("user@demo.com",       fashionStore,p13,5, "Elegant tote, very roomy!");
        stores.rateProduct("fashionista@demo.com",fashionStore,p12,4, "Great running shoes, super comfy!");
        stores.rateProduct("shopper2@demo.com",   fashionStore,p14,5, "Gorgeous scarf—many compliments!");

        // Home Store product ratings
        stores.rateProduct("shopper2@demo.com",   homeStore, p15, 5, "Best coffee at home!");
        stores.rateProduct("user@demo.com",       homeStore, p16, 4, "Vacuum works great with pets.");
        stores.rateProduct("shopper1@demo.com",   homeStore, p17, 5, "Softest sheets ever!");
        stores.rateProduct("user@demo.com",       homeStore, p18, 4, "Knives are professional quality.");
        stores.rateProduct("shopper2@demo.com",   homeStore, p19, 5, "Great hub for all smart devices!");

        /************************************************************************
         * STEP 6: OPTIONAL “ENHANCED” CATALOG PRODUCTS
         ************************************************************************/
        System.out.println("[seeder] 6) Adding enhanced catalog items...");
        catalog.addCatalogProduct("TECH-006", "Noise-Cancelling Headphones","BrandZ","",
                List.of("Electronics","Audio","Headphones","Noise Cancelling","Wireless","Premium","Studio"));
        catalog.addCatalogProduct("TECH-007", "Professional DSLR Camera","BrandY","",
                List.of("Cameras","DSLR","Professional","Full-Frame","4K","Photography"));
        catalog.addCatalogProduct("TECH-008", "Gaming Console Pro","BrandX","",
                List.of("Gaming","Consoles","Next-Gen","Ray Tracing","8K","Entertainment"));
        catalog.addCatalogProduct("FASH-006", "Designer Sunglasses","BrandQ","",
                List.of("Accessories","Eyewear","Sunglasses","Designer","Polarized","UV Protection"));
        catalog.addCatalogProduct("FASH-007", "Waterproof Hiking Boots","BrandP","",
                List.of("Footwear","Outdoor","Hiking","Waterproof","Adventure","Durable"));
        catalog.addCatalogProduct("HOME-006", "Air Purifier Premium","BrandR","",
                List.of("Appliances","Air Purifiers","HEPA","UV Sterilization","Smart"));
        catalog.addCatalogProduct("HOME-007", "Smart Refrigerator","BrandS","",
                List.of("Kitchen","Appliances","Refrigerators","Smart","Energy Efficient"));
        catalog.addCatalogProduct("SPORT-001","Carbon Fiber Road Bike","BrandT","",
                List.of("Cycling","Bikes","Road","Carbon Fiber","Professional","Racing"));
        catalog.addCatalogProduct("SPORT-002","Smart Fitness Tracker","BrandU","",
                List.of("Wearables","Fitness","Health","GPS","Tracking","Coaching"));
        catalog.addCatalogProduct("BEAUTY-001","Premium Skincare Set","BrandV","",
                List.of("Beauty","Skincare","Anti-Aging","Premium","Serums","Moisturizers"));

        /* TRANSACTION ---------------------------------------------- */
        System.out.println("Initializing transactions...");
        // Create a few transactions for the demo store
        transactions.addTransaction(
                List.of(p1, p3), // products
                1129.98, // total price
                demoStore, // store
                "user@demo.com" // buyer
        );

        transactions.addTransaction(
                List.of(p2),
                1499.99,
                demoStore,
                "shopper1@demo.com"
        );

        transactions.addTransaction(
                List.of(p4),
                349.99,
                demoStore,
                "shopper2@demo.com"
        );

        transactions.addTransaction(
                List.of(p5, p6),
                2699.98,
                techStore,
                "tech.fan@demo.com"
        );

        transactions.addTransaction(
                List.of(p10, p13),
                259.98,
                fashionStore,
                "fashionista@demo.com"
        );

        transactions.addTransaction(
                List.of(p15),
                199.99,
                homeStore,
                "shopper2@demo.com"
        );


        /************************************************************************
         * STEP 7: INITIALIZE SERVICE LOCATOR
         ************************************************************************/
        System.out.println("[seeder] 7) Initializing ServiceLocator...");
        ServiceLocator.initialize(
                guests, users, transactions, stores, catalog, shippingService
        );

        System.out.println("[seeder] Demo data insertion complete!");
    }

    /**
     * Simple helper that picks a “stock photo” URL based on a keyword.
     * Exactly the same logic you had in your old BootstrapData / DataInitializer.
     */
    private String getProductImage(String keyword) {
        String key = (keyword == null ? "" : keyword.toLowerCase());
        if (key.contains("phone")) {
            return "https://images.unsplash.com/photo-1512499617640-c2f999098c63?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("laptop")) {
            return "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("earbud") || key.contains("audio") || key.contains("headphone")) {
            return "https://images.unsplash.com/photo-1585386959984-a41552231617?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("watch")) {
            return "https://images.unsplash.com/photo-1519741497674-611481863552?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("monitor")) {
            return "https://images.unsplash.com/photo-1587825140708-030e382b97c8?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("jacket")) {
            return "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("shirt") || key.contains("tee") || key.contains("graphic")) {
            return "https://images.unsplash.com/photo-1523289333742-bea4fa21c8f8?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("shoe")) {
            return "https://images.unsplash.com/photo-1511974035430-5de47d3b95da?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("bag") || key.contains("tote")) {
            return "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("scarf")) {
            return "https://images.unsplash.com/photo-1519681393784-d120267933ba?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("coffee")) {
            return "https://images.unsplash.com/photo-1511920170033-f8396924c348?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("vacuum") || key.contains("robot")) {
            return "https://images.unsplash.com/photo-1615540122321-9b9371bd3432?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("bedding") || key.contains("sheet")) {
            return "https://images.unsplash.com/photo-1540518614846-7eded433c457?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("knife")) {
            return "https://images.unsplash.com/photo-1526040652367-ac003a0475fe?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        if (key.contains("smart") && key.contains("home")) {
            return "https://images.unsplash.com/photo-1518444024084-31c5455b03d7?"
                    + "auto=format&fit=crop&w=400&h=400&q=80";
        }
        return "https://images.unsplash.com/photo-1585386959984-a41552231617?"
                + "auto=format&fit=crop&w=400&h=400&q=80";
    }
}
