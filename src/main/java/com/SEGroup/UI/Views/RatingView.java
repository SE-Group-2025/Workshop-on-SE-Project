package com.SEGroup.UI.Views;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.List;

public class RatingView extends HorizontalLayout {

    public int score;
    public RatingView() {
        // no extra padding/gaps beyond what we set below
        setPadding(false);
        setSpacing(false);
        getStyle().set("gap", "0.2em");

        int maxStars = 5;
        List<Icon> stars = new ArrayList<>(maxStars);
        for (int i = 1; i <= maxStars; i++) {
            Icon star = VaadinIcon.STAR.create();
            star.setSize("24px");
            // start “empty”:
            star.getStyle()
                    .set("cursor", "pointer")
                    .set("color", "#E0E0E0");
            // attach index for later
            star.getElement().setAttribute("data-index", String.valueOf(i));

            // click listener:
            star.addClickListener(evt -> {
                int clicked = Integer.parseInt(star.getElement()
                        .getAttribute("data-index"));
                this.score = clicked;
                // color all stars ≤ clicked amber, rest grey:
                for (Icon s : stars) {
                    int idx = Integer.parseInt(s.getElement()
                            .getAttribute("data-index"));
                    s.getStyle().set("color", idx <= clicked
                            ? "#FFC107"   // filled
                            : "#E0E0E0"   // empty
                    );
                }
                Notification.show("You submitted: " + clicked + " ★", 1500, Position.MIDDLE);

            });

            stars.add(star);
            add(star);
        }

    }
    public int getScore(){
        return this.score;
    }
}
