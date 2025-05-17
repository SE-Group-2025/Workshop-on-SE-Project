package com.SEGroup.UI;

import com.SEGroup.Infrastructure.NotificationCenter.Notification;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationWithSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications from the UI layer.
 * Uses the domain NotificationEndpoint directly.
 */
@Service
public class NotificationSender {

    private final NotificationEndpoint notificationEndpoint;

    @Autowired
    public NotificationSender(NotificationEndpoint notificationEndpoint) {
        this.notificationEndpoint = notificationEndpoint;
    }

    /**
     * Send a system notification to a user
     */
    public void sendSystemNotification(String receiverId, String message) {
        try {
            Notification notification = new Notification(message, receiverId);
            notificationEndpoint.publish(notification);
            System.out.println("System notification sent to " + receiverId + ": " + message);
        } catch (Exception e) {
            System.err.println("Error sending system notification: " + e.getMessage());
        }
    }

    /**
     * Send a notification from one user to another
     */
    public void sendUserNotification(String receiverId, String message, String senderId) {
        try {
            NotificationWithSender notification = new NotificationWithSender(message, receiverId, senderId);
            notificationEndpoint.publish(notification);
            System.out.println("User notification sent from " + senderId + " to " + receiverId + ": " + message);
        } catch (Exception e) {
            System.err.println("Error sending user notification: " + e.getMessage());
        }
    }
}