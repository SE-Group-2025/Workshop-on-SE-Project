package com.SEGroup.UI;

import com.SEGroup.Infrastructure.NotificationCenter.*;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class DirectNotificationSender {

    @Autowired
    private com.SEGroup.Infrastructure.NotificationCenter.NotificationCenter notificationCenter;

    @Autowired
    private NotificationSender uiNotificationSender;

    @Autowired
    private com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint notificationEndpoint;

    @Autowired
    private NotificationBroadcastService broadcastService;

    private static final Logger LOGGER =
            Logger.getLogger(DirectNotificationSender.class.getName());

    /**
     * Send a system notification to a user using the domain notification center
     * with multiple fallback mechanisms
     */
    public void sendSystemNotification(String receiverId, String message) {
        if (receiverId == null || receiverId.isEmpty() || message == null) {
            LOGGER.warning("Cannot send notification: Invalid receiver or message");
            return;
        }

        LOGGER.info("Attempting to send notification to " + receiverId);
        boolean notificationSent = false;

        // STRATEGY 1: Try Domain NotificationCenter
        try {
            NotificationCenter nc = getDomainNotificationCenter();
            if (nc != null) {
                nc.sendSystemNotification(receiverId, message);
                notificationSent = true;
                LOGGER.info("Notification sent via domain NotificationCenter");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Domain NotificationCenter failed: " + e.getMessage(), e);
        }

        // STRATEGY 2: Try UI-based notification sender
        try {
            NotificationSender sender = getUiNotificationSender();
            if (sender != null) {
                sender.sendSystemNotification(receiverId, message);
                notificationSent = true;
                LOGGER.info("Notification sent via UI NotificationSender");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "UI NotificationSender failed: " + e.getMessage(), e);
        }

        // STRATEGY 3: Try direct endpoint publishing
        try {
            NotificationEndpoint endpoint = getNotificationEndpoint();
            if (endpoint != null) {
                Notification notification = new Notification(message, receiverId);
                endpoint.publish(notification);
                notificationSent = true;
                LOGGER.info("Notification sent via direct endpoint publish");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Direct endpoint publish failed: " + e.getMessage(), e);
        }

        // STRATEGY 4: Use broadcast service directly
        try {
            NotificationBroadcastService broadcast = getBroadcastService();
            if (broadcast != null) {
                Notification notification = new Notification(message, receiverId);
                broadcast.broadcast(receiverId, notification);
                notificationSent = true;
                LOGGER.info("Notification sent via broadcast service");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Broadcast service failed: " + e.getMessage(), e);
        }

        if (!notificationSent) {
            LOGGER.severe("CRITICAL: All notification mechanisms failed for " + receiverId);
        }
    }

    /**
     * Send a notification from one user to another with fallback mechanisms
     */
    public void sendUserNotification(String receiverId, String message, String senderId) {
        // Similar structure to sendSystemNotification with multiple fallbacks
        if (receiverId == null || receiverId.isEmpty() || message == null) {
            LOGGER.warning("Cannot send user notification: Invalid parameters");
            return;
        }

        boolean notificationSent = false;

        // Try domain notification center with fallbacks
        try {
            NotificationCenter nc = getDomainNotificationCenter();
            if (nc != null) {
                nc.sendUserNotification(SecurityContextHolder.token(), receiverId, message, senderId);
                notificationSent = true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Domain notification center failed for user notification", e);
        }

        // Create and directly publish notification if needed
        if (!notificationSent) {
            try {
                NotificationWithSender notification = new NotificationWithSender(message, receiverId, senderId);

                // Try endpoint
                NotificationEndpoint endpoint = getNotificationEndpoint();
                if (endpoint != null) {
                    endpoint.publish(notification);
                    notificationSent = true;
                }

                // Try broadcast
                if (!notificationSent) {
                    NotificationBroadcastService broadcast = getBroadcastService();
                    if (broadcast != null) {
                        broadcast.broadcast(receiverId, notification);
                        notificationSent = true;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "All notification mechanisms failed for user notification", e);
            }
        }
    }

    // Service acquisition methods with multiple fallback strategies
    private NotificationCenter getDomainNotificationCenter() {
        if (notificationCenter != null) {
            return notificationCenter;
        }

        try {
            // Try ServiceLocator first
            notificationCenter = ServiceLocator.getNotificationCenter();

            // If still null, try ApplicationContext
            if (notificationCenter == null && ServiceLocator.getApplicationContext() != null) {
                notificationCenter = ServiceLocator.getApplicationContext().getBean(NotificationCenter.class);
            }

            return notificationCenter;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get NotificationCenter", e);
            return null;
        }
    }

    private NotificationSender getUiNotificationSender() {
        if (uiNotificationSender != null) {
            return uiNotificationSender;
        }

        try {
            if (ServiceLocator.getApplicationContext() != null) {
                uiNotificationSender = ServiceLocator.getApplicationContext().getBean(NotificationSender.class);
            }
            return uiNotificationSender;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get NotificationSender", e);
            return null;
        }
    }

    private NotificationEndpoint getNotificationEndpoint() {
        if (notificationEndpoint != null) {
            return notificationEndpoint;
        }

        try {
            // Try ServiceLocator first
            notificationEndpoint = ServiceLocator.getNotificationEndpoint();

            // If still null, try reflection on NotificationCenter
            if (notificationEndpoint == null) {
                NotificationCenter nc = getDomainNotificationCenter();
                if (nc != null) {
                    try {
                        java.lang.reflect.Field field = nc.getClass().getDeclaredField("endpoint");
                        field.setAccessible(true);
                        notificationEndpoint = (NotificationEndpoint) field.get(nc);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to extract endpoint via reflection", e);
                    }
                }
            }

            // If still null, try ApplicationContext
            if (notificationEndpoint == null && ServiceLocator.getApplicationContext() != null) {
                notificationEndpoint = ServiceLocator.getApplicationContext().getBean(NotificationEndpoint.class);
            }

            return notificationEndpoint;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get NotificationEndpoint", e);
            return null;
        }
    }

    private NotificationBroadcastService getBroadcastService() {
        if (broadcastService != null) {
            return broadcastService;
        }

        try {
            if (ServiceLocator.getApplicationContext() != null) {
                broadcastService = ServiceLocator.getApplicationContext().getBean(NotificationBroadcastService.class);
            }
            return broadcastService;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get BroadcastService", e);
            return null;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Rich-notification helper                                               */
    /* ---------------------------------------------------------------------- */
    public void send(NotificationType type,
                     String receiverId,            // null⇢broadcast to owners (see below)
                     String message,
                     double price,
                     String productId,
                     String extra)
    {
        if (message == null || message.isBlank())
            message = type.name();                 // fallback – never null

        String senderEmail = SecurityContextHolder.email(); // may be null for system

        RichNotification n = new RichNotification(
                receiverId, senderEmail, message,
                type, price, productId, extra);

        // use the SAME four-step fallback you already implemented
        try { NotificationCenter nc = getDomainNotificationCenter();
            if (nc != null) { nc.sendSystemNotification(receiverId, message); return; }
        } catch (Exception ignore) {}

        try { NotificationSender ui = getUiNotificationSender();
            if (ui != null)    { ui.sendSystemNotification(receiverId, message); return; }
        } catch (Exception ignore) {}

        try { NotificationEndpoint ep = getNotificationEndpoint();
            if (ep != null)    { ep.publish(n); return; }
        } catch (Exception ignore) {}

        try { NotificationBroadcastService bc = getBroadcastService();
            if (bc != null)    { bc.broadcast(receiverId, n); return; }
        } catch (Exception ignore) {}

        LOGGER.severe("ALL notification channels failed – " + n);
    }

    /** Shortcut when only text matters (no price / product) */
    public void send(NotificationType t, String receiverId, String msg) {
        send(t, receiverId, msg, 0, null, null);
    }

}