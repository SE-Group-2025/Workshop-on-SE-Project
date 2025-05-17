package com.SEGroup.UI.Views;

import com.SEGroup.DTO.StoreCardDto;
import com.SEGroup.UI.FormatUtils;
import com.SEGroup.UI.MainLayout;
import com.SEGroup.UI.Presenter.AllStoresPresenter;
import com.SEGroup.UI.SecurityContextHolder;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "stores", layout = MainLayout.class)
@PageTitle("Store Listings")
public class AllStoresView extends VerticalLayout {

    private final AllStoresPresenter presenter;
    private final VerticalLayout storeCardsContainer = new VerticalLayout();
    private boolean showMyStoresOnly = false;
    private final Button toggleButton = new Button("Show My Stores");
    private final Button sortByRatingButton = new Button("Sort by Rating");

    public AllStoresView() {
        this.presenter = new AllStoresPresenter(this);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        // Header
        setupHeader();

        // Store cards container
        storeCardsContainer.setPadding(false);
        storeCardsContainer.setSpacing(true);
        storeCardsContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        add(storeCardsContainer);

        // Add navigation back to marketplace
        Button backToMarketplaceBtn = new Button("Back to Marketplace", VaadinIcon.ARROW_LEFT.create());
        backToMarketplaceBtn.addClickListener(e -> UI.getCurrent().navigate("catalog"));
        add(backToMarketplaceBtn);

        // Initial data load
        presenter.loadStores(false);
    }

    private void setupHeader() {
        // Create store button (visible only when logged in)
        Button createStoreBtn = new Button("Create Store", VaadinIcon.PLUS.create());
        createStoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createStoreBtn.setVisible(SecurityContextHolder.isLoggedIn());
        createStoreBtn.addClickListener(e -> openCreateStoreDialog());

        // Toggle button for "My Stores" vs "All Stores"
        toggleButton.setVisible(SecurityContextHolder.isLoggedIn());
        toggleButton.addClickListener(e -> {
            showMyStoresOnly = !showMyStoresOnly;
            toggleButton.setText(showMyStoresOnly ? "Show All Stores" : "Show My Stores");
            presenter.loadStores(showMyStoresOnly);
        });

        // Sort by rating button
        sortByRatingButton.addClickListener(e -> presenter.sortByRating());

        // Title with icon
        Icon listIcon = VaadinIcon.LIST.create();
        listIcon.setSize("24px");
        H2 title = new H2("Store Listings");

        HorizontalLayout titleSection = new HorizontalLayout(listIcon, title);
        titleSection.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(
                titleSection,
                createSpacer(),
                sortByRatingButton,
                toggleButton,
                createStoreBtn
        );
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        add(header);
    }

    private void openCreateStoreDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create New Store");

        TextField storeName = new TextField("Store Name");
        storeName.setWidthFull();
        storeName.setRequired(true);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        Button createBtn = new Button("Create", e -> {
            presenter.createStore(storeName.getValue());
            dialog.close();
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttons = new HorizontalLayout(cancelBtn, createBtn);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();

        VerticalLayout content = new VerticalLayout(storeName, buttons);
        content.setPadding(true);
        content.setSpacing(true);

        dialog.add(content);
        dialog.open();
    }

    /**
     * Displays the provided list of stores as cards.
     */
    public void displayStores(List<StoreCardDto> stores) {
        storeCardsContainer.removeAll();

        if (stores.isEmpty()) {
            storeCardsContainer.add(createEmptyStoresMessage());
            return;
        }

        for (StoreCardDto store : stores) {
            storeCardsContainer.add(createStoreCard(store));
        }
    }

    private Component createEmptyStoresMessage() {
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span message = new Span(showMyStoresOnly
                ? "You don't have any stores yet"
                : "No stores found");
        message.getStyle()
                .set("font-size", "1.2em")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "2em 0");

        if (showMyStoresOnly && SecurityContextHolder.isLoggedIn()) {
            Button createBtn = new Button("Create Your First Store", e -> openCreateStoreDialog());
            createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            layout.add(message, createBtn);
        } else {
            layout.add(message);
        }

        return layout;
    }

    private Component createStoreCard(StoreCardDto store) {
        HorizontalLayout card = new HorizontalLayout();
        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "1em")
                .set("width", "600px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
                .set("transition", "all 0.2s ease");

        card.getElement().executeJs(
                "this.onmouseenter = function() {" +
                        "this.style.transform = 'translateY(-3px)';" +
                        "this.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)';" +
                        "};" +
                        "this.onmouseleave = function() {" +
                        "this.style.transform = '';" +
                        "this.style.boxShadow = '0 2px 4px rgba(0,0,0,0.05)';" +
                        "};"
        );

        // Store icon
        Icon icon = VaadinIcon.STORAGE.create();
        icon.setSize("80px");
        icon.getStyle().set("color", "#9e9e9e");

        // Content section
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        H4 name = new H4(store.name());
        Span owner = new Span("Store owner: " + store.owner());

        // Format rating to one decimal place
        String formattedRating = FormatUtils.formatRating(store.rating());
        Span rating = new Span("Rating: " + formattedRating);

        // Stars for rating
        int fullStars = (int)Math.floor(store.rating());
        boolean hasHalfStar = store.rating() - fullStars >= 0.5;

        StringBuilder starsString = new StringBuilder();
        for (int i = 0; i < fullStars; i++) {
            starsString.append("★");
        }
        if (hasHalfStar) {
            starsString.append("⯨");
        }
        for (int i = 0; i < 5 - fullStars - (hasHalfStar ? 1 : 0); i++) {
            starsString.append("☆");
        }

        Span stars = new Span(starsString.toString());
        stars.getStyle().set("color", "gold").set("letter-spacing", "2px");

        HorizontalLayout ratingLayout = new HorizontalLayout(rating, stars);
        ratingLayout.setSpacing(true);
        ratingLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Paragraph desc = new Paragraph(store.description() != null && !store.description().isEmpty()
                ? store.description()
                : "No description available");

        Button viewBtn = new Button("View Store", e -> {
            // Use direct path navigation instead of RouteParameters
            UI.getCurrent().navigate(StoreView.class,store.name());
        });
        viewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        content.add(name, owner, ratingLayout, desc, viewBtn);
        card.add(icon, content);

        // Make the entire card clickable
        card.addClickListener(e -> UI.getCurrent().navigate("store/" + store.name()));

        return card;
    }

    private Component createSpacer() {
        Div d = new Div();
        d.getStyle().set("flex-grow", "1");
        return d;
    }

    /**
     * Shows a success notification.
     */
    public void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * Shows an informational notification.
     */
    public void showInfo(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    /**
     * Shows an error notification.
     */
    public void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}