package com.SEGroup.UI.Views;

import com.SEGroup.UI.MainLayout;
import com.SEGroup.UI.Presenter.RatingStorePresenter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Route(value = "stores/:storeName", layout = MainLayout.class)
@PageTitle("Store Details")
public class StoreView extends VerticalLayout implements BeforeEnterObserver {
    private List<CatalogView.Product> allProducts = fakeProducts();

    private final Div catalogContainer = new Div();
    public RatingView ratingView;
    public String storeName;
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String name = event.getRouteParameters()
                .get("storeName")
                .orElse("");
        storeName = name;
        Store store = fakeStores().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElse(null);

        if (store == null) {
            add(new Span("Store not found"));
            return;
        }

        // header
        H2 title = new H2(store.name());
        title.getStyle().set("text-decoration", "underline")
                .set("margin-bottom", "0.5em");
        add(title);

        // owner + rating line
        HorizontalLayout meta = new HorizontalLayout(
                new Span("Store owner: " + store.owner()),
                new Span("Rating: " + store.rating())
        );
        meta.getStyle().set("gap", "1em");
        add(meta);

        // description
        add(new Paragraph(store.description()));
        // inside beforeEnter(...)
        add(new Span("Your rating:"));
        ratingView = new RatingView();
        add(ratingView);
        ratingView.addClickListener(evt -> new RatingStorePresenter(this,name));
        catalogContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "0.9em");
        add(catalogContainer);
        this.allProducts.stream()
                .forEach(p -> {
                    RouterLink link = new RouterLink(
                            ProductInStoreView.class,
                            new RouteParameters(Map.of(
                                    "id1", getStoreName(),
                                    "id2", p.id(),
                                    "img", encode(p.imageUrl()))));
                    link.getElement().getStyle()
                            .set("text-decoration", "none")
                            .set("color", "inherit");
                    link.add(createCard(p));
                    catalogContainer.add(link);
                });
    }
    private Div createCard(CatalogView.Product p) {
        Div card = new Div();
        card.getStyle()
                .set("width", "200px")
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("padding", "0.75em")
                .set("background", "#fff")
                .set("box-shadow", "0 3px 8px rgba(0,0,0,0.06)")
                .set("transition", "transform .12s")
                .set("cursor", "pointer");

        card.addAttachListener(e ->
                card.getElement().executeJs(
                        "this.onmouseenter = _ => this.style.transform='scale(1.03)';" +
                                "this.onmouseleave = _ => this.style.transform='';"));

        Image img = new Image(p.imageUrl(), "product");
        img.setWidth("100%");
        img.getStyle().set("border-radius", "4px");

        Span name = new Span(p.name());
        Span price = new Span("$" + p.price());
        price.getStyle().set("font-weight", "600");

        Button cart = new Button(VaadinIcon.CART.create(), click ->
                Notification.show(p.name() + " added", 2000, Notification.Position.BOTTOM_CENTER));
        cart.addThemeVariants(ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_SMALL);

        HorizontalLayout bottom = new HorizontalLayout(price, cart);
        bottom.setWidthFull();
        bottom.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bottom.setAlignItems(FlexComponent.Alignment.CENTER);

        card.add(img, name, bottom);
        return card;
    }

    private List<Store> fakeStores() {
        return List.of(
                new Store("Adults +18 store", "Goseph", 5, "This is a store for adults only! …"),
                new Store("Toy store",          "Laura", 2, "This is a store for kids, …"),
                new Store("iHerb store",       "Kaplan", 1, "Selling herbs …")
        );
    }
    private List<CatalogView.Product> fakeProducts() {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new CatalogView.Product(
                        "P" + i,
                        "Art Image " + i,
                        i * 10,
                        "https://picsum.photos/seed/" + i + "/380/280",
                        i % 3 == 0 ? "Sports" : i % 3 == 1 ? "Office" : "Home"
                )).toList();
    }
    public String getStoreName(){
        return storeName;
    }
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    record Store(String name, String owner, int rating, String description) {}

}