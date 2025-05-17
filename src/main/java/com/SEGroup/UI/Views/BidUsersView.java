
package com.SEGroup.UI.Views;

import com.SEGroup.UI.Constants.BidRequest;
import com.SEGroup.UI.MainLayout;
import com.SEGroup.UI.Presenter.BidUsersPresenter;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.router.*;

import java.util.List;

@Route(value = "product/:productId/:storeName/bids", layout = MainLayout.class)
@PageTitle("Bid Requests")
public class BidUsersView extends VerticalLayout implements BeforeEnterObserver {

    // switch to BidRequest

    public final Grid<BidRequest> usersGrid = new Grid<>(BidRequest.class,false);
    private BidUsersPresenter presenter;


    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        String productId = event.getRouteParameters().get("productId").orElse("");
        String storeName = event.getRouteParameters().get("storeName").orElse("");
        if (productId.isEmpty() || storeName.isEmpty()) {
            Notification.show("Invalid URL parameters", 3000, Position.MIDDLE);
            return;
        }

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        add(new H2("Bid Requests for Product " + productId + " in store " + storeName));

        // e-mail column
        usersGrid.addColumn(BidRequest::email)
                .setHeader("User Email")
                .setAutoWidth(true);

        // amount column
        usersGrid.addColumn(req -> String.format("$%.2f", req.amount()))
                .setHeader("Offer Amount")
                .setAutoWidth(true);



        // actions column
        usersGrid.addComponentColumn(req -> {
            Button accept = new Button("Accept");
            accept.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            accept.addClickListener(e -> {
                presenter.acceptBid(req.email(), req.amount());

            });

            Button reject = new Button("Reject");
            reject.addThemeVariants(ButtonVariant.LUMO_ERROR);
            reject.addClickListener(e -> presenter.rejectBid(req.email(), req.amount()));

            return new HorizontalLayout(accept, reject);
        }).setHeader("Actions");

        add(usersGrid);

        // wire up your presenter
        presenter = new BidUsersPresenter(this, storeName, productId);
        presenter.loadBidUsers();
    }

    public void displayBidUsers(List<BidRequest> bids) {
        usersGrid.setItems(bids);
    }

    public void showSuccess(String msg) {
        Notification.show(msg, 3000, Position.MIDDLE);
    }
    public void showError(String msg) {
        Notification.show(msg, 3000, Position.MIDDLE);
    }
}