package com.SEGroup.UI;

import com.SEGroup.Infrastructure.NotificationCenter.Notification;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationWithSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct; // Changed from javax to jakarta

/**
 * Adapter that connects domain notifications to UI components
 * without requiring changes to domain code.
 */
@Component
public class NotificationAdapter {

    private final NotificationEndpoint notificationEndpoint;
    private final NotificationBroadcastService broadcastService;
    private final com.SEGroup.UI.NotificationCenter uiNotificationCenter;
    private Disposable globalSubscription;

    @Autowired
    public NotificationAdapter(
            NotificationEndpoint notificationEndpoint,
            NotificationBroadcastService broadcastService,
            @Qualifier("uiNotificationCenter") com.SEGroup.UI.NotificationCenter uiNotificationCenter) {
        this.notificationEndpoint = notificationEndpoint;
        this.broadcastService = broadcastService;
        this.uiNotificationCenter = uiNotificationCenter;
    }

    @PostConstruct
    public void init() {
        try {
            // Subscribe to ALL notifications from the domain NotificationEndpoint
            // and route them to our UI notification systems
            globalSubscription = notificationEndpoint
                    .subscribe("*") // If this doesn't work, we'll need another approach
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            notification -> {
                                // Forward notification to broadcast service
                                broadcastService.broadcast(notification.getReceiverId(), notification);

                                // Also format and send through UI notification center for toast messages
                                uiNotificationCenter.notify(formatNotificationMessage(notification));
                            },
                            error -> System.err.println("Error in global notification subscription: " + error.getMessage())
                    );
            System.out.println("Global notification subscription initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize global notification subscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Format a notification message for display
     */
    private String formatNotificationMessage(Notification notification) {
        if (notification instanceof NotificationWithSender) {
            NotificationWithSender nws = (NotificationWithSender) notification;
            return nws.getSenderId() + ": " + notification.getMessage();
        }
        return notification.getMessage();
    }
}