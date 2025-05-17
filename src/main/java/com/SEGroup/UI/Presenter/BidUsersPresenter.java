package com.SEGroup.UI.Presenter;

import com.SEGroup.DTO.BidDTO;
import com.SEGroup.Infrastructure.NotificationCenter.Notification;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationType;
import com.SEGroup.Service.TransactionService;
import com.SEGroup.UI.DirectNotificationSender;
import com.SEGroup.UI.SecurityContextHolder;
import com.SEGroup.UI.Constants.BidRequest;
import com.SEGroup.UI.Views.BidUsersView;
import com.SEGroup.Service.Result;
import com.SEGroup.Service.StoreService;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.NotificationView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Presenter for loading and displaying users who bid on a product.
 */
public class BidUsersPresenter {
    private final BidUsersView view;
    private final StoreService storeService;
    private final String storeName;
    private final String productId;
//    private NotificationView notificationView;
    private final TransactionService transactionService;


    public BidUsersPresenter(BidUsersView view, String storeName, String productId) {
        this.view = view;
        this.storeName = storeName;
        this.productId = productId;
        this.storeService = ServiceLocator.getStoreService();
//        notificationView = new NotificationView();
        this.transactionService = ServiceLocator.getTransactionService();

    }

    /**
     * Fetches the list of users who have requested to bid and updates the view.
     */
    public void loadBidUsers() {
        Result<List<BidDTO>> r = storeService.getProductBids(SecurityContextHolder.token()
                ,this.storeName, this.productId);

        if (r.isSuccess()) {
            List<BidRequest> requests = r.getData().stream()
                    .map(dto -> new BidRequest(dto.getBidderEmail(), dto.getPrice()))
                    .toList();
            view.displayBidUsers(requests);
        } else {
            view.showError("Failed to load bids: " + r.getErrorMessage());
        }
    }
    public void acceptBid(String userEmail, double amount) {
        Result<Void> res = transactionService.acceptBid(SecurityContextHolder.token(),
                this.storeName,new BidDTO(userEmail,this.productId,amount));
        if (res.isSuccess()) {
            // send green “bid accepted” notification to the bidder
            DirectNotificationSender dns = ServiceLocator.getDirectNotificationSender();
            dns.send(NotificationType.BID_ACCEPTED, userEmail, null, amount, productId, null);

            view.displayBidUsers(
                    view.usersGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
                            .filter(b -> !b.email().equals(userEmail))
                            .collect(Collectors.toList()));
            view.showSuccess("Accepted " + userEmail + "’s bid of $" + amount);
        }
        else {
            view.showError("Could not accept bid: " + res.getErrorMessage());
        }
//        Result<Void> res = storeService.submitBidToShoppingItem(
//                SecurityContextHolder.token(),
//                storeName,
//                productId,
//                amount
//        );
//        if (res.isSuccess()) {
//            view.displayBidUsers(view.usersGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
//                    .filter(b -> !b.email().equals(userEmail))
//                    .collect(Collectors.toList()));
//            view.showSuccess("Accepted " + userEmail + "’s bid of $" + amount);
//        } else {
//            view.showError("Could not accept bid: " + res.getErrorMessage());
//        }
    }

    public void rejectBid(String userEmail, double amount) {
        Result<Void> res = transactionService.rejectBid(SecurityContextHolder.token(), this.storeName,new BidDTO(userEmail,this.productId,amount));
        if (res.isSuccess()) {
            // send red “bid rejected” notification to the bidder
            DirectNotificationSender dns = ServiceLocator.getDirectNotificationSender();
            dns.send(NotificationType.BID_REJECTED, userEmail, null, amount, productId, null);

            view.displayBidUsers(
                    view.usersGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
                            .filter(b -> !b.email().equals(userEmail))
                            .collect(Collectors.toList()));
            view.showSuccess("Rejected " + userEmail + "’s bid of $" + amount);
        }
        else {
            view.showError("Could not reject bid: " + res.getErrorMessage());
        }
    }
}