package com.SEGroup.UI;

import com.SEGroup.Domain.*;
import com.SEGroup.Domain.Report.ReportCenter;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint;
import com.SEGroup.Service.*;
import com.SEGroup.Domain.*;
import com.SEGroup.Infrastructure.PasswordEncoder;

import com.SEGroup.Infrastructure.Security;
import com.SEGroup.Infrastructure.SecurityAdapter;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationCenter;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import static org.mockito.Mockito.*;
import javax.crypto.SecretKey;
import java.io.InputStream;

/**
 * A centralized service locator for retrieving singleton service instances.
 * Use this to inject services into Presenters or UI layers.
 */
@Component
public class ServiceLocator implements ApplicationContextAware {

    // Core Dependencies
    private static ApplicationContext applicationContext;
    private static final Security security = new Security();
    private static final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private static IAuthenticationService authService;

    // Notification components
    private static NotificationEndpoint notificationEndpoint;
    private static NotificationSender notificationSender;
    private static DirectNotificationSender directNotificationSender;
    private static INotificationCenter notificationCenter;

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
    private static InitFileRunner initFileRunner;
    // Notifaction
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
        transactionService = new TransactionService(authService, paymentGateway, transactionRepository, storeRepository, userRepository, shippingService, notificationCenter,guestRepository);
        initFileRunner = new InitFileRunner(guestService,userService,storeService);
        applyBootstrapJson();
    }
    private static void applyBootstrapJson() {
        try (InputStream in = ServiceLocator.class.getClassLoader()
                .getResourceAsStream("bootstrap/initial-state.json")) {

            if (in == null) {
                LoggerWrapper.warning("[seeder] (bootstrap/initial-state.json missing)");
                return;
            }

            initFileRunner.replayFile(in);
            LoggerWrapper.info("[seeder] ✓ bootstrap script applied");

        } catch (Exception ex) {
            LoggerWrapper.error(ex.getMessage(), ex);
            // keep running – just log the error
        }
    }
    public static INotificationCenter getNotificationCenter() {
        if (applicationContext != null) {
            return applicationContext.getBean(INotificationCenter.class);
        }
        return notificationCenter;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    public static DirectNotificationSender getDirectNotificationSender() {
        // ① if we already have Spring – always use the real bean
        if (applicationContext != null) {
            return applicationContext.getBean(DirectNotificationSender.class);
        }

        // ② fallback only in pure unit tests - no Spring container
        if (directNotificationSender == null) {
            directNotificationSender = new DirectNotificationSender();

            // Manually inject dependencies for test environment
            directNotificationSender.endpoint = getNotificationEndpoint();

            // Create a simple broadcast service if needed
            if (applicationContext == null) {
                System.out.println("Creating temporary broadcast service for DirectNotificationSender");
                directNotificationSender.broadcast = new NotificationBroadcastService();
            }

            directNotificationSender.domainCenter = getNotificationCenter();
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


    public static NotificationEndpoint getNotificationEndpoint() {
        /* ① primary – grab the singleton managed by Spring */
        if (applicationContext != null) {
            return applicationContext.getBean(NotificationEndpoint.class);
        }

        /* ② fallback – old reflective trick for pure-unit-tests */
        if (notificationEndpoint == null && notificationCenter != null) {
            try {
                var f = notificationCenter.getClass().getDeclaredField("endpoint");
                f.setAccessible(true);
                notificationEndpoint = (NotificationEndpoint) f.get(notificationCenter);
            } catch (Exception e) {
                System.err.println("Cannot access NotificationEndpoint: " + e.getMessage());
            }
        }
        return notificationEndpoint;
    }

    public static BidApprovalManager getBidApprovalManager() {
        return applicationContext.getBean(BidApprovalManager.class);
    }



}