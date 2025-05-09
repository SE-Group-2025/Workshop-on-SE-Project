package com.SEGroup.UI.Views;

import com.SEGroup.DTO.StoreDTO;
import com.SEGroup.UI.MainLayout;
import com.SEGroup.UI.Presenter.AllStoresPresenter;
import com.SEGroup.UI.ServiceLocator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.util.List;
import java.util.ResourceBundle;

@Route(value = "stores", layout = MainLayout.class)
@PageTitle("Store Listings")
public class AllStoresView extends VerticalLayout {

    private AllStoresPresenter allStoresPresenter;
    public AllStoresView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        // Title with icon
        Icon storeIcon = VaadinIcon.LIST.create();
        storeIcon.setSize("24px");
        storeIcon.getStyle().set("margin-right", "0.5em");

        H2 title = new H2("Store listings");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.8em")
                .set("text-shadow", "1px 1px 2px rgba(0,0,0,0.2)");

        HorizontalLayout titleBar = new HorizontalLayout(storeIcon, title);
        titleBar.setAlignItems(FlexComponent.Alignment.CENTER);

        Anchor nextPage = new Anchor("#", "next page");
        nextPage.getStyle().set("margin-top", "0.5em");

        HorizontalLayout topBar = new HorizontalLayout(titleBar, createSpacer(), nextPage);
        topBar.setWidthFull();
        topBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Sort bar
        Span sortLabel = new Span("sort by : ");
        Anchor ratingLink = new Anchor("#", "Rating");
        ratingLink.getStyle().set("color", "red");

        HorizontalLayout sortBar = new HorizontalLayout(sortLabel, ratingLink);
        sortBar.setSpacing(true);

        add(topBar, sortBar);

        // Store cards
        fakeStores().forEach(store -> add(createStoreCard(store)));
        // View stores in backend
//        allStoresPresenter = new AllStoresPresenter();
//        allStoresPresenter.getStores().stream().map(store -> {
//            add(createStoreCard(store));
//            return null;
//        });

    }

    private Component createStoreCard(Store store) {
        HorizontalLayout card = new HorizontalLayout();
        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "1em")
                .set("width", "600px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");
        card.setAlignItems(Alignment.START);

        Icon storeIcon = VaadinIcon.STORAGE.create();
        storeIcon.setSize("80px");
        storeIcon.getStyle().set("margin-right", "1em").set("color", "#9e9e9e");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        H4 name = new H4(store.name());

        HorizontalLayout meta = new HorizontalLayout();
        Anchor ownerLink = new Anchor("#", store.owner());
        ownerLink.getStyle().set("color", "gray");

        Anchor ratingLink = new Anchor("#", "Rating: " + store.rating());
        ratingLink.getStyle().set("color", "gray");

        meta.add(new Span("Store owner : "), ownerLink, new Text("   "), ratingLink);

        HorizontalLayout infoLine = new HorizontalLayout();
        Anchor infoAnchor = new Anchor("#", "Info : ");
        infoAnchor.getStyle().set("color", "gray");
        Span infoText = new Span(store.description());
        infoLine.add(infoAnchor, infoText);


        Button viewButton = new Button("View Store", e ->
                UI.getCurrent().navigate(StoreView.class, new RouteParameters("storeName", store.name()))
        );
        viewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        content.add(name, meta, infoLine, viewButton);
        card.add(storeIcon, content);
        return card;
    }

    private Component createSpacer() {
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");
        return spacer;
    }

    private List<Store> fakeStores() {
        return List.of(
                new Store("Adults +18 store", "Goseph", 5, "This is a store for adults only! Get in at your own risk"),
                new Store("Toy store", "Laura", 2, "This is a store for kids, if you are not a kid grow up!"),
                new Store("iHerb store", "Kaplan", 1, "Selling herbs for the lefties only if you are not a facist and follow bibii")
        );
    }

    public record Store(String name, String owner, double rating, String description) {}
}
