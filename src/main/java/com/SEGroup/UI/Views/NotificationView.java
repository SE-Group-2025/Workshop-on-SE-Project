package com.SEGroup.UI.Views;

import com.SEGroup.UI.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "notification", layout = MainLayout.class)
public class NotificationView extends VerticalLayout {

    public NotificationView() {
        // full-width, little top padding
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // 1) Page title
        H1 pageTitle = new H1("Notifications");
        add(pageTitle);

        // 2) Unread section
        H3 unread = new H3("Unread Notifications");
        unread.getStyle().set("text-decoration", "underline");
        add(unread);

        // three unread cards
        add(createNotificationCard("Sale on ps5",     "Temu",       "2 min ago", false));
        add(createNotificationCard("Notification Text","store name","2 min ago", false));
        add(createNotificationCard("Notification Text","store name","6 min ago", false));

        // 3) Read section
        H3 read = new H3("Read Notifications");
        read.getStyle().set("text-decoration", "underline")
                .set("margin-top", "2em");
        add(read);

        // one read card
        add(createNotificationCard("Notification Text", "store name", "2 month ago", true));
    }

    private Component createNotificationCard(String title,
                                             String subtitle,
                                             String timestamp,
                                             boolean isRead) {
        // horizontal “card”
        HorizontalLayout card = new HorizontalLayout();
        card.setWidth("400px");
        card.getStyle()
                .set("padding", "1em")
                .set("border", "1px solid #ccc")
                .set("border-radius", "1em")
                .set("background-color", isRead ? "#f5f5f5" : "#ffffff")
                .set("align-items", "center");

        // left icon
        Icon icon = VaadinIcon.BELL.create();
        icon.getStyle().set("flex-shrink", "0");

        // title + subtitle stacked
        VerticalLayout texts = new VerticalLayout();
        texts.setPadding(false);
        texts.setSpacing(false);
        texts.add(new Span(title), new Span(subtitle));
        texts.getStyle().set("margin-left", "1em")
                .set("flex-grow", "1");

        // timestamp on the right
        Span time = new Span(timestamp);
        time.getStyle().set("margin-left", "auto");

        card.add(icon, texts, time);
        return card;
    }
}
