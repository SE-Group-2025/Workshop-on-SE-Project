package com.SEGroup.UI.Views;

import com.SEGroup.UI.MainLayout;
import com.SEGroup.UI.Presenter.SearchProductPresenter;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.*;
import org.springframework.security.core.parameters.P;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route(value = "catalog", layout = MainLayout.class)
@PageTitle("Product catalog")
public class CatalogView extends VerticalLayout {

    private List<Product> allProducts;
    private List<Product> searchedProducts;
    private final Div catalogContainer = new Div();
    private final HorizontalLayout priceFilterLayout = new HorizontalLayout();
    private final HorizontalLayout categoryFilterLayout = new HorizontalLayout();
    private SearchProductPresenter searchProductPresenter;
    private boolean priceVisible = false;
    private boolean categoryVisible = false;


    public CatalogView() {
        this.addAttachListener(evt -> {
            // the parent of this view is your MainLayout
            this.getParent()
                    .filter(p -> p instanceof MainLayout)
                    .map(p -> (MainLayout)p)
                    .ifPresent(main -> {
                        new SearchProductPresenter(this, main.search, main.searchBtn);
                    });
        });
        this.allProducts = fakeProducts();
        this.searchedProducts = fakeProducts();
        setWidthFull();
        setPadding(true);
        setSpacing(false);

        // Title
        H3 title = new H3("Product catalog");
        title.getStyle()
                .set("margin", "0 0 1em 0")
                .set("text-align", "left");
        add(title);

        // Filter buttons
        Button priceBtn = new Button("Price", e -> {
            priceVisible = !priceVisible;
            priceFilterLayout.setVisible(priceVisible);
        });
        Button categoryBtn = new Button("Category", e -> {
            categoryVisible = !categoryVisible;
            categoryFilterLayout.setVisible(categoryVisible);
        });
        priceBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        categoryBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Span slash = new Span("/");
        HorizontalLayout sortBar = new HorizontalLayout(
                new Span("sort by :"),
                priceBtn, slash,
                new Span("Rating"), slash,
                categoryBtn
        );
        sortBar.setAlignItems(FlexComponent.Alignment.CENTER);
        add(sortBar);

        // Price filter
        NumberField maxPriceField = new NumberField();
        maxPriceField.setPlaceholder("Max price");
        double max = searchedProducts.stream().mapToDouble(Product::price).max().orElse(0);
        maxPriceField.setMin(0);
        maxPriceField.setMax(max);
        maxPriceField.setStep(1);

        Button applyPrice = new Button("Apply", e -> updateCatalog(maxPriceField.getValue(), null));
        priceFilterLayout.add(maxPriceField, applyPrice);
        priceFilterLayout.setVisible(false);
        add(priceFilterLayout);

        // Category filter
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setPlaceholder("Category");
        categoryCombo.setItems(searchedProducts.stream()
                .map(Product::category)
                .distinct()
                .collect(Collectors.toList()));
        Button applyCategory = new Button("Apply", e -> updateCatalog(null, categoryCombo.getValue()));
        categoryFilterLayout.add(categoryCombo, applyCategory);
        categoryFilterLayout.setVisible(false);
        add(categoryFilterLayout);

        // Catalog grid container
        catalogContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "0.9em");
        add(catalogContainer);

        updateCatalog(null, null);
    }

    private void updateCatalog(Double maxPrice, String category) {
        double limit = maxPrice != null ? maxPrice : searchedProducts.stream().mapToDouble(Product::price).max().orElse(Double.MAX_VALUE);
        catalogContainer.removeAll();
        searchedProducts.stream()
                .filter(p -> p.price() <= limit)
                .filter(p -> category == null || p.category().equals(category))
                .forEach(p -> {
                    RouterLink link = new RouterLink(
                            "View",
                            ProductView.class,
                            new RouteParameters(Map.of(
                                    "id", p.id(),
                                    "img", encode(p.imageUrl()))));
                    link.getElement().getStyle()
                            .set("text-decoration", "none")
                            .set("color", "inherit");
                    link.add(createCard(p));
                    catalogContainer.add(link);
                });
    }

    private Div createCard(Product p) {
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<Product> fakeProducts() {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new Product(
                        "P" + i,
                        "Art Image " + i,
                        i * 10,
                        "https://picsum.photos/seed/" + i + "/380/280",
                        i % 3 == 0 ? "Sports" : i % 3 == 1 ? "Office" : "Home"
                )).toList();
    }
    public void showProducts(List<Product> products) {
        this.searchedProducts = products;
        updateCatalog(null, null);
    }
    public List<Product> getAllProducts(){
        return this.allProducts;
    }

    public record Product(String id, String name, double price, String imageUrl, String category) {}
}
