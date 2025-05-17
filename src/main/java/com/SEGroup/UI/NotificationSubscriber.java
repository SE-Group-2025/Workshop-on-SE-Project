package com.SEGroup.UI;

import com.SEGroup.Infrastructure.NotificationCenter.Notification;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationWithSender;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

/**
 * Component that subscribes to notifications for the current user
 * and displays them in the UI.
 */
@SpringComponent
@UIScope
public class NotificationSubscriber {

    private final NotificationEndpoint notificationEndpoint;
    private final NotificationBroadcastService broadcastService;
    private final MainLayout mainLayout;
    private Disposable endpointSubscription;
    private Registration broadcastRegistration;

    @Autowired
    public NotificationSubscriber(NotificationEndpoint notificationEndpoint,
                                  NotificationBroadcastService broadcastService) {
        this.notificationEndpoint = notificationEndpoint;
        this.broadcastService = broadcastService;
        this.mainLayout = MainLayout.getInstance();
    }

    /**
     * Initialize the notification subscriber when the component is constructed.
     */
    @PostConstruct
    public void init() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.addDetachListener(event -> {
                cleanupSubscriptions();
            });
        }

        // Subscribe to notifications if user is logged in
        if (SecurityContextHolder.isLoggedIn()) {
            subscribeToNotifications(SecurityContextHolder.email());
        }
    }

    private void cleanupSubscriptions() {
        if (endpointSubscription != null && !endpointSubscription.isDisposed()) {
            endpointSubscription.dispose();
        }

        if (broadcastRegistration != null) {
            broadcastRegistration.remove();
        }
    }

    /**
     * Subscribe to notifications for the given user ID.
     * Stores UI reference to ensure UI updates happen correctly.
     *
     * @param userId The user ID to subscribe for
     */
    /**
     * Subscribe to notifications for the given user ID.
     * Stores UI reference to ensure UI updates happen correctly.
     *
     * @param userId The user ID to subscribe for
     */
    public void subscribeToNotifications(String userId) {
        cleanupSubscriptions();

        UI currentUI = UI.getCurrent();
        if (currentUI != null) {
            System.out.println("DEBUG: Subscribing to notifications for user: " + userId);

            try {
                // Try to get endpoint from ServiceLocator as backup
                NotificationEndpoint endpoint = this.notificationEndpoint;
                if (endpoint == null) {
                    endpoint = ServiceLocator.getNotificationEndpoint();
                    System.out.println("Using endpoint from ServiceLocator");
                }

                if (endpoint != null) {
                    // Subscribe using endpoint
                    endpointSubscription = endpoint
                            .subscribe(userId)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    notification -> {
                                        if (currentUI.isAttached()) {
                                            System.out.println("Received notification via endpoint: " + notification.getMessage());
                                            currentUI.access(() -> processNotification(notification));
                                        }
                                    },
                                    error -> System.err.println("Error in notification subscription: " + error.getMessage()),
                                    () -> System.out.println("Notification subscription completed for user: " + userId)
                            );
                } else {
                    System.out.println("WARNING: NotificationEndpoint not available!");
                }

                // Always register with broadcast service as primary/backup
                broadcastRegistration = broadcastService.register(
                        userId,
                        currentUI,
                        notification -> {
                            System.out.println("Received notification via broadcast: " + notification.getMessage());
                            processNotification(notification);
                        }
                );

                System.out.println("Successfully subscribed to notifications for: " + userId);
            } catch (Exception e) {
                System.err.println("ERROR subscribing to notifications: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Cannot subscribe to notifications: no UI available");
        }
    }

    /**
     * Process a received notification by displaying it in the UI
     * and incrementing the notification counter.
     *
     * @param notification The notification to process
     */
    private void processNotification(Notification notification) {
        String message = notification.getMessage();
        String sender = "";

        // Extract sender if present
        if (notification instanceof NotificationWithSender) {
            sender = ((NotificationWithSender) notification).getSenderId();
            message = sender + ": " + message;
        }

        // Update UI with the notification
        if (mainLayout != null) {
            mainLayout.handleNewNotification(message);
        }

        // Show a toast notification with appropriate styling
        com.vaadin.flow.component.notification.Notification toast =
                com.vaadin.flow.component.notification.Notification.show(
                        message,
                        3000,
                        com.vaadin.flow.component.notification.Notification.Position.TOP_END
                );
        toast.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }
}