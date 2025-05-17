package com.SEGroup.UI;

import com.SEGroup.Infrastructure.NotificationCenter.Notification;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Service to broadcast notifications to Vaadin UIs.
 * This handles the push functionality for real-time notifications.
 */
@Service
public class NotificationBroadcastService {
    private final Executor executor = Executors.newSingleThreadExecutor();

    // Store both UI and listener together for each user
    private static class UIListenerPair {
        final UI ui;
        final Consumer<Notification> listener;

        UIListenerPair(UI ui, Consumer<Notification> listener) {
            this.ui = ui;
            this.listener = listener;
        }
    }

    private final Map<String, Map<Integer, UIListenerPair>> listeners = new ConcurrentHashMap<>();
    /* ------------------- new: per-user history ---------------------------- */
    private final ConcurrentHashMap<String,
            java.util.List<Notification>> history = new ConcurrentHashMap<>();

    /**
     * Register a UI to receive notifications for a specific user.
     *
     * @param userId The ID of the user to receive notifications
     * @param ui The UI instance to update
     * @param listener The callback to handle the notification
     * @return A registration object that can be used to remove the listener
     */
    public Registration register(String userId, UI ui, Consumer<Notification> listener) {
        // Get or create a map of UI listeners for this user
        Map<Integer, UIListenerPair> userListeners = listeners.computeIfAbsent(
                userId, id -> new ConcurrentHashMap<>());

        // Add this UI's listener with a reference to the UI instance
        userListeners.put(ui.getUIId(), new UIListenerPair(ui, listener));

        // Return a registration object to remove this listener
        return () -> {
            synchronized (listeners) {
                Map<Integer, UIListenerPair> userMap = listeners.get(userId);
                if (userMap != null) {
                    userMap.remove(ui.getUIId());
                    if (userMap.isEmpty()) {
                        listeners.remove(userId);
                    }
                }
            }
        };
    }

    /**
     * Broadcast a notification to all registered UIs for a specific user.
     *
     * @param userId The ID of the user to send the notification to
     * @param notification The notification to send
     */
    /* --------------------------------------------------------------------- */
    /* NEW broadcast – pushes the event *and* saves it for later retrieval   */
    /* --------------------------------------------------------------------- */
    public void broadcast(String userId, Notification notification) {

        if (userId == null || notification == null) return;

        /* 1) remember it --------------------------------------------------- */
        history.computeIfAbsent(userId, k -> java.util.Collections.synchronizedList(
                new java.util.ArrayList<>())).add(notification);

        /* 2) push it to every live UI ------------------------------------- */
        Map<Integer, UIListenerPair> userListeners = listeners.get(userId);
        if (userListeners != null) {
            executor.execute(() -> userListeners.values().forEach(pair -> {
                if (pair.ui != null && pair.ui.isAttached()) {
                    pair.ui.access(() -> pair.listener.accept(notification));
                }
            }));
        }
    }

    /* --------------------------------------------------------------------- */
    /* helper - the NotificationView fetches the accumulated list            */
    /* --------------------------------------------------------------------- */
    public java.util.List<Notification> getHistory(String userId) {
        return history.getOrDefault(userId, java.util.Collections.emptyList());
    }

}