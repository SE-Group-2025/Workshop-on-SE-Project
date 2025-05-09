package com.SEGroup.UI.Views;

import com.SEGroup.UI.MainLayout;
//import com.SEGroup.UI.Presenter.RatingProductPresenter;
import com.SEGroup.UI.Presenter.RatingProductPresenter;
import com.SEGroup.UI.Presenter.RatingStorePresenter;
import com.SEGroup.UI.ServiceLocator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Route(value = "store/:id1/product/:id2/:img", layout = MainLayout.class)
@PageTitle("Product details")
public class ProductInStoreView extends VerticalLayout implements HasUrlParameter<String> , BeforeEnterObserver  {

    public RatingView ratingView;
    public RatingProductPresenter ratingProductPresenter;
    /* tiny demo “repository” ---------------------------------- */
    private static final Map<String, Product> repo = Map.of(
            "P1",  new Product("P1",  "Red Gun",   50, "Kaplan Store", 4),
            "P2",  new Product("P2",  "Blue Gun",  30, "Laura Store",  2),
            "P3",  new Product("P3",  "Green Gun", 20, "Joseph Store", 5)
    );

    /* UI fields ----------------------------------------------- */
    private final Image pic   = new Image("", "img");
    private final H2    name  = new H2();
    private final Span  meta  = new Span();
    private final Span  stars = new Span();
    private final Button add  = new Button("Add to cart", VaadinIcon.CART.create());
    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        setAlignItems(Alignment.CENTER);
        setPadding(true);
        pic.setWidth("320px");
        pic.getStyle().set("border-radius", "8px");

        add(new VerticalLayout(name, pic, stars, meta, add));
        add.addClickListener(e -> Notification.show("Added"));
        ratingView = new RatingView();
        add("Your rating:");
        add(ratingView);
        String storeName  = beforeEnterEvent.getRouteParameters()
                .get("id1").orElse("");
        String productId  = beforeEnterEvent.getRouteParameters()
                .get("id2").orElse("");
        ratingProductPresenter = new RatingProductPresenter(this, storeName, productId);
    }

//    public ProductInStoreView() {
//        setAlignItems(Alignment.CENTER);
//        setPadding(true);
//        pic.setWidth("320px");
//        pic.getStyle().set("border-radius", "8px");
//
//        add(new VerticalLayout(name, pic, stars, meta, add));
//        add.addClickListener(e -> Notification.show("Added"));
//        ratingView = new RatingView();
//        add("Your rating:");
//        add(ratingView);
//         ratingProductPresenter = new RatingProductPresenter(this,);
//        ratingView.addClickListener(evt -> new RatingProductPresenter(this,"1"));
//    }

    /* read both parameters ------------------------------------ */
    @Override
    public void setParameter(BeforeEvent e, @OptionalParameter String id) {
        String imgEncoded = e.getRouteParameters().get("img").orElse("");
        String img = URLDecoder.decode(imgEncoded, StandardCharsets.UTF_8);

        /* try repo first – otherwise fall back to anonymous card */
        Product p = id != null ? repo.get(id) : null;

        if (p == null) {                               // still show picture!
            name.setText("Unknown product");
            meta.setText("ID " + id);
            stars.setText("");
            pic.setSrc(img);
            return;
        }

        name.setText(p.name());
        meta.setText("ID " + p.id() + " • " + p.store() + " • $" + p.price());
        stars.setText("★".repeat(p.rating()) + "☆".repeat(5 - p.rating()));
        pic.setSrc(img);
    }




    private record Product(String id, String name, int price,
                           String store, int rating) {}
}
