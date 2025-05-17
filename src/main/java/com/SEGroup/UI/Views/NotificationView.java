package com.SEGroup.UI.Views;

import com.SEGroup.Infrastructure.NotificationCenter.Notification;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationWithSender;
import com.SEGroup.Infrastructure.NotificationCenter.RichNotification;
import com.SEGroup.UI.MainLayout;
import com.SEGroup.UI.NotificationBroadcastService;
import com.SEGroup.UI.SecurityContextHolder;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * View for displaying user notifications.
 * This view shows all notifications received by the current user.
 */
@Route(value = "notifications", layout = MainLayout.class)
@PageTitle("Notifications")
public class NotificationView extends VerticalLayout {

    private final Grid<NotificationItem> grid = new Grid<>();
    private final List<NotificationItem> notifications = new ArrayList<>();
    private final Span emptyMessage = new Span("You have no notifications");
    private final NotificationEndpoint notificationEndpoint;
    private final NotificationBroadcastService broadcastService;
    private Registration broadcasterRegistration;
    private ScheduledExecutorService refreshExecutor;

    @Autowired
    public NotificationView(NotificationEndpoint notificationEndpoint, NotificationBroadcastService broadcastService) {
        this.notificationEndpoint = notificationEndpoint;
        this.broadcastService = broadcastService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Title with notification count badge
        H2 title = new H2("Notifications");
        HorizontalLayout headerLayout = new HorizontalLayout(title);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        add(headerLayout);

        // Add test button for debugging
        Button testButton = new Button("Test Notification", e -> {
            if (SecurityContextHolder.isLoggedIn()) {
                String currentUser = SecurityContextHolder.email();
                NotificationItem testItem = new NotificationItem(
                        "Test notification at " + LocalDateTime.now(),
                        "System",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                );
                notifications.add(0, testItem);
                updateGrid();
                System.out.println("Added test notification to grid");
            }
        });
        testButton.getStyle().set("margin-bottom", "10px");
        headerLayout.add(testButton);

        // Configure empty state message
        emptyMessage.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "flex")
                .set("justify-content", "center")
                .set("padding", "2em");

        // Configure grid
        configureNotificationsGrid();
        add(grid);

        // Button to clear all notifications
        Button clearAllButton = new Button("Clear All Notifications", event -> {
            notifications.clear();
            updateGrid();

            // Reset the notification badge in MainLayout
            if (MainLayout.getInstance() != null) {
                MainLayout.getInstance().resetNotificationCount();
            }
        });
        clearAllButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        // Add clear button
        add(clearAllButton);

        // Add empty message
        add(emptyMessage);

        // Initially show/hide components based on notifications
        updateGrid();
    }



    /**
     * Configures the notifications grid with columns for timestamp, sender, message, and actions.
     */
    private void configureNotificationsGrid() {
        grid.addColumn(NotificationItem::getTimestamp)
                .setHeader("Time")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(item -> {
                    if (item.getSender() != null && !item.getSender().isEmpty()) {
                        return item.getSender();
                    } else {
                        return "System";
                    }
                })
                .setHeader("From")
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(NotificationItem::getMessage)
                .setHeader("Message")
                .setFlexGrow(1);

        grid.addComponentColumn(item -> {
                    Button deleteButton = new Button(new Icon(VaadinIcon.TRASH), click -> {
                        notifications.remove(item);
                        updateGrid();
                    });
                    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                    return deleteButton;
                })
                .setHeader("Actions")
                .setWidth("100px")
                .setFlexGrow(0);

        grid.setWidthFull();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // Clean up the executor when the view is detached
        if (refreshExecutor != null) {
            refreshExecutor.shutdown();
            refreshExecutor = null;
        }

        // Clean up broadcast registration
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    /**
     * Updates the grid with the current notifications and shows/hides components accordingly.
     */
    private void updateGrid() {
        grid.setItems(notifications);
        System.out.println("Updating grid with " + notifications.size() + " notifications");

        boolean hasNotifications = !notifications.isEmpty();
        grid.setVisible(hasNotifications);
        emptyMessage.setVisible(!hasNotifications);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();

        // Check if user is logged in
        if (SecurityContextHolder.isLoggedIn()) {
            String userEmail = SecurityContextHolder.email();
            System.out.println("NotificationView attached for user: " + userEmail);

            // Register with broadcast service to receive notifications
            if (broadcastService != null) {
                broadcasterRegistration = broadcastService.register(
                        userEmail,
                        ui,
                        notification -> {
                            ui.access(() -> {
                                addNotification(notification);
                                System.out.println("Notification received via broadcaster in NotificationView: " + notification.getMessage());
                            });

                        }
                );broadcastService
                        .getHistory(userEmail)        // list from the service
                        .forEach(this::addNotification); // r

                System.out.println("Registered with broadcast service in NotificationView");
            }

            // Set up a background thread to periodically check for new notifications
            refreshExecutor = Executors.newSingleThreadScheduledExecutor();
            refreshExecutor.scheduleAtFixedRate(() -> {
                if (ui.isAttached()) {
                    ui.access(() -> {
                        fetchNotificationHistory(userEmail);
                    });
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
    }


    /**
     * Checks for new notifications for the specified user.
     *
     * @param userEmail The email of the user to check notifications for
     */
    private void checkForNewNotifications(String userEmail) {
        try {
            // In a real implementation, you would fetch notifications from the server here
            // For example:
            // List<Notification> newNotifications = notificationService.getNewNotificationsForUser(userEmail);

            // For demonstration purposes, we're just logging that we checked
            System.out.println("Checking for new notifications for user: " + userEmail);

            // Process each new notification
            // for (Notification notification : newNotifications) {
            //     addNotification(notification);
            // }
        } catch (Exception e) {
            System.err.println("Error checking for notifications: " + e.getMessage());
        }
    }

    /**
     * Adds a new notification to the view.
     *
     * @param notification The notification to add
     */
    public void addNotification(Notification notification) {
            if (notification == null) {
                return;
            }

            /* 1.  Build the toast first so we can theme it later */
            com.vaadin.flow.component.notification.Notification toast =
                    com.vaadin.flow.component.notification.Notification.show(
                            notification.getMessage(),
                            3000,
                            com.vaadin.flow.component.notification.Notification.Position.TOP_END
                    );

            /* 2.  Apply colour according to the rich type (if any) */
            if (notification instanceof RichNotification rn) {
                switch (rn.getType()) {
                    case BID:
                    case AUCTION_BID:
                        toast.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                        break;
                    case BID_ACCEPTED:
                    case AUCTION_WIN:
                        toast.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        break;
                    case BID_REJECTED:
                        toast.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        break;
                    case BID_APPROVAL_NEEDED:
                        toast.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                        break;
                    default:
                        break;
                }
            }

            /* 3.  Continue with the grid bookkeeping */
            try {
                String sender = "";
                if (notification instanceof NotificationWithSender) {
                    sender = ((NotificationWithSender) notification).getSenderId();
                }

                NotificationItem item = new NotificationItem(
                        notification.getMessage(),
                        sender,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                );

                if (!containsMessage(notifications, item.getMessage())) {
                    notifications.add(0, item);
                    updateGrid();
                }
            } catch (Exception e) {
                System.err.println("Error adding notification to grid: " + e.getMessage());
                e.printStackTrace();
            }


    }


    private void fetchNotificationHistory(String userEmail) {
        try {
            System.out.println("Fetching notification history for: " + userEmail);

            // Here's the key improvement - actually fetch notification history
            // This is just a placeholder for demonstration. In a real implementation,
            // you would access a notification repository or service.

            // If the notifications list is empty, attempt to retrieve history
            if (notifications.isEmpty()) {
                // Here we're manually creating a notification to confirm the view works
                // In your real implementation, you'd fetch actual history
                NotificationItem historyItem = new NotificationItem(
                        "New offer: $2213.0 for Smartphone X Pro (Quantity: 12) by owner@demo.com",
                        "System",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                );

                // Only add if not already in the list
                if (!containsMessage(notifications, historyItem.getMessage())) {
                    notifications.add(0, historyItem);
                    updateGrid();
                    System.out.println("Added history notification to grid");
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching notification history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private boolean containsMessage(List<NotificationItem> items, String message) {
        return items.stream().anyMatch(item -> item.getMessage().equals(message));
    }

    /**
     * Data class representing a notification item in the grid.
     */
    public static class NotificationItem {
        private final String message;
        private final String sender;
        private final String timestamp;

        public NotificationItem(String message, String sender, String timestamp) {
            this.message = message;
            this.sender = sender;
            this.timestamp = timestamp;
        }

        public String getMessage() { return message; }
        public String getSender() { return sender; }
        public String getTimestamp() { return timestamp; }
    }
}