package com.SEGroup.UI.Presenter;

import com.SEGroup.DTO.AuctionDTO;
import com.SEGroup.DTO.BidDTO;
import com.SEGroup.DTO.ShoppingProductDTO;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationCenter;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationType;
import com.SEGroup.Service.Result;
import com.SEGroup.Service.StoreService;
import com.SEGroup.Service.UserService;
import com.SEGroup.UI.DirectNotificationSender;
import com.SEGroup.UI.SecurityContextHolder;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.ProductView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class ProductPresenter {
    private static final Logger logger = Logger.getLogger(ProductPresenter.class.getName());
    private Registration pollRegistration;
    private final ProductView view;
    private final String productId;
    private final String storeName;
    private final StoreService storeService;
    private final UserService userService;
    private ShoppingProductDTO product;
    private AuctionDTO auction;
    private final DirectNotificationSender notificationSender;
    private final NotificationCenter notificationCenter;

    // Bid approval tracking - will be refreshed from server each time
    private int totalOwnersCount = 0;
    private int currentApprovals = 0;

    public ProductPresenter(ProductView view, String productId, String storeName, DirectNotificationSender notificationSender) {
        this.view = view;
        this.productId = productId;
        this.storeName = storeName;
        this.storeService = ServiceLocator.getStoreService();
        this.userService = ServiceLocator.getUserService();
        this.notificationCenter = ServiceLocator.getNotificationCenter();
        this.notificationSender = notificationSender;
    }

    public AuctionDTO getAuction() {
        return auction;
    }
    private void notify(NotificationType t,
                        String target,
                        String text,
                        double price,
                        String extra)
    {
        notificationSender.send(t, target, text, price, productId, extra);
    }




    /**
     * Sends a notification to all store owners
     * @param message The message to send
     * @return The number of owners notified
     */
    private int notifyStoreOwners(String message) {
        try {
            Result<List<String>> ownersResult = storeService.getAllOwners(
                    SecurityContextHolder.token(),
                    storeName,
                    SecurityContextHolder.email()
            );

            if (ownersResult.isSuccess() && ownersResult.getData() != null) {
                int count = 0;
                for (String ownerEmail : ownersResult.getData()) {
                    notificationSender.sendSystemNotification(ownerEmail, message);
                    count++;
                }
                logger.info("Notified " + count + " store owners: " + message);
                return count;
            }
        } catch (Exception e) {
            logger.severe("Error notifying store owners: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Loads the total count of store owners for approval tracking
     * This ensures we always have the latest count from the server
     */
    private void loadOwnersCount() {
        try {
            Result<List<String>> ownersResult = storeService.getAllOwners(
                    SecurityContextHolder.token(),
                    storeName,
                    SecurityContextHolder.email()
            );

            if (ownersResult.isSuccess() && ownersResult.getData() != null) {
                this.totalOwnersCount = ownersResult.getData().size();
                logger.info("Store has " + totalOwnersCount + " owners for approval tracking");
            } else {
                logger.warning("Failed to load owners count: " +
                        (ownersResult.isSuccess() ? "Empty result" : ownersResult.getErrorMessage()));
            }
        } catch (Exception e) {
            logger.severe("Error loading owners count: " + e.getMessage());
        }
    }

    /**
     * Loads the current approval count for a specific bid from the server
     * @param bidderEmail The bidder's email
     * @param bidPrice The bid price
     * @return Number of current approvals
     */
    private int loadCurrentApprovals(String bidderEmail, double bidPrice) {
        try {
            // This would be replaced with an actual call to your backend service
            // For example: storeService.getBidApprovalCount(storeName, productId, bidderEmail, bidPrice);
            // For now, we'll simulate this by returning our in-memory counter
            return currentApprovals;
        } catch (Exception e) {
            logger.severe("Error loading current approvals: " + e.getMessage());
            return 0;
        }
    }


    /**
     * Loads auction information for the current product
     */
    public void loadAuctionInfo() {
        try {
            String token = SecurityContextHolder.token();
            if (token == null || !SecurityContextHolder.isLoggedIn()) {
                return; // Can't load auction info without authentication
            }

            // Try to get auction end date - this determines if an auction exists
            Result<Date> endDateResult = storeService.getAuctionEndDate(token, storeName, productId);

            // If we can't get an end date, there's no auction
            if (!endDateResult.isSuccess() || endDateResult.getData() == null) {
                return;
            }

            Date endDate = endDateResult.getData();

            // If auction has ended, process end and don't display auction UI
            if (endDate.before(new Date())) {
                processAuctionEnd();
                return;
            }

            // Get highest bid for the auction
            Result<BidDTO> highestBidResult = storeService.getAuctionHighestBidByProduct(token, storeName, productId);

            // Determine starting and current price
            double startingPrice = 0.0;
            Double highestBid = null;
            String highestBidder = null;

            // If we have the product info, use its price as starting price
            if (product != null) {
                startingPrice = product.getPrice();
            }

            // If there's a highest bid, use it
            if (highestBidResult.isSuccess() && highestBidResult.getData() != null) {
                highestBid = highestBidResult.getData().getPrice();
                highestBidder = highestBidResult.getData().getBidderEmail();
            }

            // Calculate time remaining
            long timeRemaining = Math.max(0, endDate.getTime() - System.currentTimeMillis());

            // Create the auction DTO
            auction = new AuctionDTO(
                    storeName,
                    productId,
                    startingPrice,
                    highestBid,
                    highestBidder,
                    endDate,
                    timeRemaining
            );

            // Display the auction info in the view
            view.displayAuctionInfo(auction);

            // Set up polling for real-time updates
            if (timeRemaining > 0) {
                scheduleAuctionRefresh();
            }
        } catch (Exception e) {
            logger.severe("Error loading auction info: " + e.getMessage());
        }
    }

    /**
     * Sets up automatic polling to refresh auction data
     */
// --- add at the top with the other fields ---------------
    private void scheduleAuctionRefresh() {
        // Avoid stacking listeners
        if (pollRegistration != null) {        // already polling – nothing to do
            return;
        }

        UI currentUI = UI.getCurrent();
        if (currentUI != null) {
            currentUI.setPollInterval(5000);   // 5 sec

            pollRegistration = currentUI.addPollListener(event -> {
                if (auction == null) {         // auction was removed
                    currentUI.setPollInterval(-1);
                    pollRegistration.remove();
                    pollRegistration = null;
                    return;
                }

                if (auction.getEndTime().before(new Date())) {
                    processAuctionEnd();
                    currentUI.setPollInterval(-1);
                    pollRegistration.remove();
                    pollRegistration = null;
                } else {
                    loadAuctionInfo();         // single refresh
                }
            });
        }
    }

    /**
     * Processes the end of an auction by notifying winners
     */
    private void processAuctionEnd() {
        try {
            if (auction != null && auction.getHighestBidder() != null && auction.getHighestBid() != null) {
                // Notify the winner
                notify(NotificationType.AUCTION_WIN, auction.getHighestBidder(), null, auction.getHighestBid(), null);

                // Update UI to show the auction has ended
                view.showSuccess("Auction has ended! The winner is " +
                        auction.getHighestBidder() + " with a bid of $" + auction.getHighestBid());
            } else {
                // No winner (no valid bids)
                view.showInfo("Auction has ended with no valid bids. The item was not sold.");
            }
        } catch (Exception e) {
            logger.severe("Error processing auction end: " + e.getMessage());
        }
    }

    /**
     * Loads the product details from the service and updates the view.
     */
    public void loadProductDetails() {
        try {
            logger.info("Loading product details for: " + productId + " in store: " + storeName);

            Result<ShoppingProductDTO> productResult = storeService.getProductFromStore(
                    SecurityContextHolder.token(),
                    storeName,
                    productId
            );

            if (productResult.isSuccess() && productResult.getData() != null) {
                this.product = productResult.getData();
                view.displayProduct(product);
                logger.info("Successfully loaded product: " + product.getName());

                // Load owners count for bid approval tracking
                loadOwnersCount();
            } else {
                // Fallback method - try to find the product in all products
                logger.info("Product not found directly, trying fallback approach...");
                Result<List<ShoppingProductDTO>> allResult = storeService.getAllProducts();

                if (allResult.isSuccess()) {
                    Optional<ShoppingProductDTO> foundProduct = allResult.getData().stream()
                            .filter(p -> p.getProductId().equals(productId) && p.getStoreName().equals(storeName))
                            .findFirst();

                    if (foundProduct.isPresent()) {
                        this.product = foundProduct.get();
                        view.displayProduct(this.product);
                        logger.info("Found product using fallback: " + this.product.getName());

                        // Load owners count for bid approval tracking
                        loadOwnersCount();
                    } else {
                        view.showError("Product not found");
                        logger.warning("Product not found with fallback either");
                    }
                } else {
                    view.showError("Error loading product: " +
                            (productResult.isSuccess() ? "No data found" : productResult.getErrorMessage()));
                }
            }
        } catch (Exception e) {
            logger.severe("Error loading product details: " + e.getMessage());
            e.printStackTrace();
            view.showError("Error loading product: " + e.getMessage());
        }
    }

    /**
     * Starts an auction for the current product
     * @param startingPrice The starting price
     * @param endDate The end date/time
     */
    public void startAuction(double startingPrice, Date endDate) {
        // Check user permissions
        if (!SecurityContextHolder.isLoggedIn()) {
            view.showError("You must be logged in to start an auction");
            return;
        }

        if (!isOwner()) {
            view.showError("Only store owners can start auctions");
            return;
        }

        String token = SecurityContextHolder.token();
        Result<Void> r = storeService.startAuction(token, storeName, productId, startingPrice, endDate);

        if (r.isSuccess()) {
            view.showSuccess("Auction started successfully! Notifications will be sent to interested users.");

            // Notify other store owners
            String notifyMsg = SecurityContextHolder.email() + " has started an auction for " +
                    getProductName() + " with starting price $" + startingPrice +
                    ". Ending on: " + endDate;
            notifyStoreOwners(notifyMsg);

            loadAuctionInfo();  // Refresh the auction bar
        } else {
            view.showError("Failed to start auction: " + r.getErrorMessage());
        }
    }

    /**
     * Adds the current product to the user's cart
     */
    public void addToCart() {
        try {
            logger.info("Adding to cart: " + productId + " from store: " + storeName);
            String token = SecurityContextHolder.token();
            logger.info("Current token: " + (token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null"));

            // For guests, create a session if needed
            if (token == null || token.isEmpty()) {
                logger.info("No token found, creating guest session");
                Result<String> guestResult = userService.guestLogin();
                if (guestResult.isSuccess()) {
                    token = guestResult.getData();
                    logger.info("Created guest token: " + token.substring(0, Math.min(10, token.length())) + "...");
                    // Store token in session for future use
                    SecurityContextHolder.storeGuestToken(token);
                } else {
                    view.showError("Failed to create guest session: " + guestResult.getErrorMessage());
                    return;
                }
            }

            // Add to cart using the token
            logger.info("Adding to cart with token");
            Result<String> result = userService.addToCart(token, productId, storeName);

            if (result.isSuccess()) {
                view.showSuccess("Added to cart!");
                logger.info("Successfully added to cart");
            } else {
                view.showError("Error: " + result.getErrorMessage());
                logger.warning("Failed to add to cart: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.severe("Exception adding to cart: " + e.getMessage());
            e.printStackTrace();
            view.showError("Error adding to cart: " + e.getMessage());
        }
    }

    /**
     * Load all bids for the current product (regular bids, not auction)
     * @return List of bids or empty list if none found
     */
    public Result<List<BidDTO>> loadProductBids() {
        try {
            return storeService.getProductBids(
                    SecurityContextHolder.token(),
                    storeName,
                    productId
            );
        } catch (Exception e) {
            logger.severe("Error loading bids: " + e.getMessage());
            return Result.failure("Failed to load bids: " + e.getMessage());
        }
    }

    /**
     * Accept a bid from a specific user - requires approval from all owners
     * @param bidderEmail Email of the bidder
     * @param bidPrice Price of the bid
     * @return Success or failure result
     */
    public Result<Void> acceptBid(String bidderEmail, double bidPrice) {
        try {
            if (!isOwner()) {
                return Result.failure("Only store owners can accept bids");
            }

            // Reset approval counter for this new approval process
            currentApprovals = 1; // Including this owner

            // Get all other owners to request approval
            Result<List<String>> ownersResult = storeService.getAllOwners(
                    SecurityContextHolder.token(),
                    storeName,
                    SecurityContextHolder.email()
            );

            if (ownersResult.isSuccess() && ownersResult.getData() != null) {
                List<String> owners = ownersResult.getData();
                totalOwnersCount = owners.size();

                // If I'm the only owner, accept immediately
                if (totalOwnersCount <= 1) {
                    // Notify bidder their bid was accepted
                    String acceptMessage = "Your offer of $" + bidPrice + " for " + getProductName() +
                            " has been accepted by " + storeName + "!";
                    sendSimpleNotification(bidderEmail, acceptMessage);

                    return Result.success(null);
                }

                // Request approval from all other owners
                for (String ownerEmail : owners) {
                    if (!ownerEmail.equals(SecurityContextHolder.email())) {
                        // Send approval request to each owner
                        notify(NotificationType.BID_APPROVAL_NEEDED, ownerEmail, null, bidPrice, bidderEmail);
                    }
                }

                // Record my approval
                notify(NotificationType.BID_APPROVAL_OK, null, null, bidPrice, bidderEmail);

                // Check if all approvals are already received (e.g., from a database)
                currentApprovals = loadCurrentApprovals(bidderEmail, bidPrice);

                // If all owners have now approved, finalize
                if (currentApprovals >= totalOwnersCount) {
                    finalizeAcceptedBid(bidderEmail, bidPrice);
                }

                return Result.success(null);
            } else {
                return Result.failure("Failed to get store owners");
            }
        } catch (Exception e) {
            logger.severe("Error accepting bid: " + e.getMessage());
            return Result.failure("Failed to accept bid: " + e.getMessage());
        }
    }

    /**
     * Finalizes an accepted bid after all approvals
     */
    private void finalizeAcceptedBid(String bidderEmail, double bidPrice) {
        // Final notification to bidder
        String acceptMessage = "Your offer of $" + bidPrice + " for " + getProductName() +
                " has been accepted by all owners of " + storeName + "! You can proceed to checkout.";
        sendSimpleNotification(bidderEmail, acceptMessage);

        // Notify all owners
        String finalMsg = "Bid acceptance complete: " + getProductName() +
                " will be sold to " + bidderEmail + " for $" + bidPrice;
        notifyStoreOwners(finalMsg);
    }

    /**
     * Records an owner's approval for a bid
     * In a real implementation, this would persist the approval in a database
     */
    public void recordOwnerApproval(String ownerEmail, String bidderEmail, double bidPrice) {
        // Increment approval counter
        currentApprovals++;

        // Notify about this approval
        notify(NotificationType.BID_APPROVAL_OK, null, null, bidPrice, bidderEmail);

        // If all owners have now approved, finalize
        if (currentApprovals >= totalOwnersCount) {
            finalizeAcceptedBid(bidderEmail, bidPrice);
        }
    }

    /**
     * Places a bid in an auction with validation
     */
    public void placeBid(double amount) {
        // Check if user is logged in
        if (!SecurityContextHolder.isLoggedIn()) {
            view.showError("You must be logged in to place a bid");
            return;
        }

        // Save previous highest bidder to notify if outbid
        String previousBidder = null;
        Double previousBid = null;

        if (auction != null) {
            previousBidder = auction.getHighestBidder();
            previousBid = auction.getHighestBid();
        }

        // Validate bid amount against current highest bid
        if (auction != null && auction.getHighestBid() != null && amount <= auction.getHighestBid()) {
            view.showError("Your bid must be higher than the current highest bid ($" + auction.getHighestBid() + ")");
            return;
        }

        // Submit the bid
        Result<Void> r = storeService.submitBidToShoppingItem(
                SecurityContextHolder.token(),
                storeName,
                productId,
                amount
        );

        if (r.isSuccess()) {
            view.showSuccess("Bid placed successfully!");

            notify(NotificationType.AUCTION_BID, null, null, amount, null);

            // Notify previous highest bidder they've been outbid
            if (previousBidder != null && previousBid != null &&
                    !previousBidder.equals(SecurityContextHolder.email())) {
                notify(NotificationType.AUCTION_OUTBID, previousBidder, null, previousBid, null);
            }

            // Immediately refresh auction data to show updated state
            loadAuctionInfo();
        } else {
            view.showError("Failed to place bid: " + r.getErrorMessage());
        }
    }

    /**
     * Submit a bid offer for a product (regular bid, not auction)
     */
    public void bidBuy(double amount) {
        // Check if user is logged in
        if (!SecurityContextHolder.isLoggedIn()) {
            view.showError("You must be logged in to make an offer");
            return;
        }

        // Validate inputs
        if (amount <= 0) {
            view.showError("Price must be a positive number");
            return;
        }

        // Submit bid offer
        Result<Void> res = storeService.submitBidToShoppingItem(
                SecurityContextHolder.token(),
                storeName,
                productId,
                amount
        );

        if (res.isSuccess()) {
            view.showSuccess("Your offer has been submitted! Store owners will be notified.");

            // Notify store owners about the new bid
            notify(NotificationType.BID, null, null, amount, null);

            // Show detailed message to user
            if (product != null) {
                String productName = product.getName();
                view.showInfo("You offered $" + amount + " for " + productName + ". " +
                        "The store owner will review your offer soon.");
            }
        } else {
            view.showError("Problem with your offer: " + res.getErrorMessage());
        }
    }

    /**
     * Gets the product name
     */
    public String getProductName() {
        return product != null
                ? product.getName()
                : "product";
    }

    /**
     * Checks if the current user is a store owner
     */
    public boolean isOwner() {
        String me = SecurityContextHolder.email();
        if (me == null || me.isEmpty()) {
            return false;
        }

        try {
            return storeService.isOwner(me, storeName);
        } catch (Exception e) {
            // if the user isn't in the owners list (or any error), treat as not owner
            return false;
        }
    }

    /**
     * Gets the product ID
     */
    public String getProductId() {
        return productId;
    }

    private void sendSimpleNotification(String receiverId, String message) {
        notificationSender.sendSystemNotification(receiverId, message);
    }

    /**
     * Open bids view for store owners
     */
    public void viewAllBids() {
        if (!isOwner()) {
            view.showError("Only store owners can view all bids");
            return;
        }

        view.openBidsDialog();
    }

    /**
     * Reject a regular bid and notify the bidder.
     */
    public void rejectBid(String bidderEmail, double bidPrice) {
        // plain red toast for the bidder
        notify(NotificationType.BID_REJECTED, bidderEmail, null, bidPrice, null);
    }

    /**
     * Send a counter-offer to the bidder.
     *
     * @param bidderEmail  customer’s e-mail
     * @param counterPrice new price you propose
     * @param note         optional free-text message (may be null / empty)
     */
    public void counterBid(String bidderEmail, double counterPrice, String note) {
        notify(NotificationType.BID_COUNTER, bidderEmail, note, counterPrice, null);
    }

    /**
     * Gets the store name
     */
    public String getStoreName() {
        return storeName;
    }
}