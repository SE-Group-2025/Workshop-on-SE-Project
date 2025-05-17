package com.SEGroup.UI;

import com.SEGroup.Domain.*;
import com.SEGroup.Domain.Report.ReportCenter;
import com.SEGroup.Service.*;
import com.SEGroup.Domain.*;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationCenter;
import com.SEGroup.Infrastructure.PasswordEncoder;
import com.SEGroup.Infrastructure.Security;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.*;
import javax.crypto.SecretKey;

/**
 * A centralized service locator for retrieving singleton service instances.
 * Use this to inject services into Presenters or UI layers.
 */
public class ServiceLocator {

    // Core Dependencies
    private static ApplicationContext applicationContext;
    private static final Security security = new Security();
    private static final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private static IAuthenticationService authService;

    // Notification components
    private static NotificationEndpoint notificationEndpoint;
    private static NotificationSender notificationSender;
    private static DirectNotificationSender directNotificationSender;
    private static NotificationCenter notificationCenter;

    // Repositories
    private static IUserRepository userRepository;
    private static IGuestRepository guestRepository;
    private static ITransactionRepository transactionRepository;
    private static IStoreRepository storeRepository;
    private static IProductCatalog productCatalog;
    private static IPaymentGateway paymentGateway;
    // Services
    private static GuestService guestService;
    private static UserService userService;
    private static StoreService storeService;
    private static TransactionService transactionService;
    private static IShippingService shippingService;

    // Notifaction
    private static NotificationCenter notificationCenter;
    private static ReportCenter reportCenter;
    public static void initialize(IGuestRepository guests,
                                  IUserRepository users,
                                  ITransactionRepository transactions,
                                  IStoreRepository stores,
                                  IProductCatalog catalog,
                                  IPaymentGateway gateway) {
        guestRepository = guests;
        userRepository = users;
        transactionRepository = transactions;
        storeRepository = stores;
        productCatalog = catalog;
        paymentGateway = gateway;

        // Initialize Security
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        security.setKey(key);
        authService = new SecurityAdapter(security, passwordEncoder);
        notificationCenter = new NotificationCenter(authService);
        reportCenter = new ReportCenter();
        guestService = new GuestService(guestRepository, authService);
        userService = new UserService(guestService, userRepository, authService,reportCenter);
        storeService = new StoreService(storeRepository, productCatalog, authService, userRepository, notificationCenter);
        shippingService = mock(IShippingService.class);
        transactionService = new TransactionService(authService, paymentGateway, transactionRepository, storeRepository, userRepository, shippingService, notificationCenter);
    }

    public static NotificationCenter getNotificationCenter() {
        if (applicationContext != null) {
            return applicationContext.getBean(NotificationCenter.class);
        }
        return notificationCenter;  // your old fallback
    }


    public static DirectNotificationSender getDirectNotificationSender() {
        // 1) if we have a Spring context, always pull the real bean
        if (applicationContext != null) {
            return applicationContext.getBean(DirectNotificationSender.class);
        }
        // 2) otherwise (e.g. in pure‐unit‐test mode), fall back
        if (directNotificationSender == null) {
            directNotificationSender = new DirectNotificationSender();
        }
        return directNotificationSender;
    }

    public static IAuthenticationService getAuthenticationService() {
        return authService;
    }

    public static GuestService getGuestService() {
        return guestService;
    }

    public static UserService getUserService() {
        return userService;
    }

    public static StoreService getStoreService() {
        return storeService;
    }

    public static TransactionService getTransactionService() {
        return transactionService;
    }
    // Add these methods after your existing methods in ServiceLocator.java

    // Method to access the Spring application context
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    // Setter method to configure application context
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
        System.out.println("Application context set in ServiceLocator");

        // If we have an application context, try to get beans from it
        if (context != null) {
            try {
                // These lines are optional - only if you want to try getting beans directly
                if (directNotificationSender == null) {
                    directNotificationSender = context.getBean(DirectNotificationSender.class);
                }
            } catch (Exception e) {
                System.err.println("Could not get beans from context: " + e.getMessage());
            }
        }
    }

    // Helper method to get NotificationEndpoint
    public static NotificationEndpoint getNotificationEndpoint() {
        if (notificationEndpoint == null && notificationCenter != null) {
            try {
                // Try to get endpoint directly
                java.lang.reflect.Field field = notificationCenter.getClass().getDeclaredField("endpoint");
                field.setAccessible(true);
                notificationEndpoint = (NotificationEndpoint) field.get(notificationCenter);
                System.out.println("Successfully extracted NotificationEndpoint from NotificationCenter");
            } catch (Exception e) {
                System.err.println("Cannot access NotificationEndpoint: " + e.getMessage());
            }
        }
        return notificationEndpoint;
    }
}