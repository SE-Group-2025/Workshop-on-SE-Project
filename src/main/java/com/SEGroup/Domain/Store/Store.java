package com.SEGroup.Domain.Store;
import com.SEGroup.DTO.ShoppingProductDTO;
import com.SEGroup.Domain.Conditions.*;
import com.SEGroup.Domain.Discount.ConditionalDiscount;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountType;
import com.SEGroup.Domain.Discount.Numerical.MaxDiscount;
import com.SEGroup.Domain.Discount.Numerical.NumericalComposite;
import com.SEGroup.Domain.Discount.SimpleDiscount;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.*;
/*
 * Represents a store, including its products, owners, managers, and policies.
 */
@Entity
@Table(name = "stores")
public class Store {
    //fields
    @Id
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "email")
    private String founderEmail;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "balance")
    private double balance;

    private final AtomicInteger inStoreProductId = new AtomicInteger(-1);

    @Column(name = "description")
    private String description="";

    //products and reviews
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKeyColumn(name = "product_key")
    @JoinColumn(name = "store_name") // foreign key בטבלת ShoppingProduct
    private Map<String, ShoppingProduct> products = new java.util.concurrent.ConcurrentHashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKeyColumn(name = "review_id") // המפתח במפה
    @JoinColumn(name = "store_name")  // foreign key בטבלת Review
    private final Map<String, Review> reviewIdToReview = new java.util.concurrent.ConcurrentHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "user_id")
    @CollectionTable(name = "store_ratings", joinColumns = @JoinColumn(name = "store_name"))
    private final Map<String, Rating> ratings = new java.util.concurrent.ConcurrentHashMap<>();



    // Disocunt and policy fields
    @Embedded
    private PurchasePolicy purchasePolicy;

    @Transient
    private NumericalComposite discounts;

    //Owners and managers
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "email")
    @CollectionTable(name = "store_owners", joinColumns = @JoinColumn(name = "store_name"))
    @Column(name = "appointed_by")
    private final Map<String, String> ownersAppointer = new java.util.concurrent.ConcurrentHashMap<>(); // email → appointedBy

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "store_name") // foreign key בטבלת ManagerData
    @MapKey(name = "email")
    private final Map<String, ManagerData> managers = new java.util.concurrent.ConcurrentHashMap<>(); // email → metadata
    protected Store() {

    }

    public Store(String name, String founderEmail) {
        //field
        this.name = name;
        this.founderEmail = founderEmail;
        this.isActive = true;
        this.balance = 0.0;

        //product and reviews
        this.products = new java.util.concurrent.ConcurrentHashMap<>();
        this.ratings.clear();
        this.reviewIdToReview.clear();
        // Disocunt and policy fields
        this.purchasePolicy = new PurchasePolicy(0,0);
        //genislav to do
        this.discounts = new MaxDiscount(new ArrayList<>());
    }

    // Getters
    public String getName() { return name; }
    public String getfounderEmail() { return founderEmail; }
    public boolean isActive() { return isActive; }
    public double getBalance() { return balance; }


    // Setters
    public void setName(String name) {
        if (name != null && !name.isEmpty())
            this.name = name;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    /*
     * Sets the store to inactive.
        * This method should be called when the store is closed.
        *
     */
    public void close() {
        this.isActive = false;
    }
    /*
     * Sets the store to active.
        * This method should be called when the store is opened.
        *
     */
    public void open(){
        this.isActive = true;
    }
    // Products (ShoppingProduct) 4.1
    /*
     * Adds a product to the store.
     *
     * @param email        The email of the user adding the product.
     * @param storeName    The name of the store.
     * @param catalogID    The catalog ID of the product.
     * @param product_name The name of the product.
     * @param description  The description of the product.
     * @param price        The price of the product.
     * @param quantity     The quantity of the product.
     * @return The ID of the added product.
     */
    public String addProductToStore(String email, String storeName, String catalogID,String product_name, String description, double price, int quantity,boolean isAdmin, String imageURL,List<String> categories){
        if(quantity == 0)
            throw new IllegalArgumentException("quantity cannot be 0 ");
        if (isOwnerOrHasManagerPermissions(email) || isAdmin) {
            String productId = String.valueOf(inStoreProductId.incrementAndGet());
            productId += "_" + storeName;
            ShoppingProduct product = new ShoppingProduct(storeName, catalogID,productId, product_name, description, price, quantity, imageURL,categories);
            products.put(productId, product);
            return productId;
        }
        //wont get here
        return null;
    }

    public ShoppingProduct getProduct(String productId) {
        return products.get(productId);
    }
    public void updateProduct(String productId, ShoppingProduct product) {
        this.products.put(productId, product);
    }
    public ShoppingProduct updateShoppingProduct(String catalogID, double price, String description){
        ShoppingProduct product = getProduct(catalogID);
        if (product == null) {
            throw new RuntimeException("Product not found in store");
        }
        product.setPrice(price);
        product.setDescription(description); // assuming description is name; change if needed
        return  product;
    }
    public void removeProduct(String productId) {
        products.remove(productId);
    }

    public Collection<ShoppingProduct> getAllProducts() {
        return products.values();
    }

    public void addToBalance(double amount){
        this.balance += amount;
    }

    public List<String> getAllWorkers() {
        List<String> allWorkers = new ArrayList<>();
        allWorkers.add(founderEmail);
        allWorkers.addAll(ownersAppointer.keySet());
        allWorkers.addAll(managers.keySet());
        return allWorkers;
    }



    /*
     * Submits a bid to a shopping item.
        *
        * @param itemName   The name of the item.
        * @param bidAmount  The amount of the bid.
        * @param bidderEmail The email of the bidder.
        * @return true if the bid was successfully submitted, false otherwise.
     */
    public Integer submitBidToShoppingItem(String itemName, double bidAmount, String bidderEmail) {
        ShoppingProduct product = products.get(itemName);

        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }

        if (bidAmount <= 0 || bidderEmail == null || bidderEmail.isBlank()) {
            throw new IllegalArgumentException("Invalid bid amount or bidder email");
        }

        Integer bidId = product.addBid(bidderEmail, bidAmount);
        return bidId;
    }

    /*
     * Submits an auction offer for a product.
        *
        * @param productId   The ID of the product.
        * @param offerAmount The amount of the offer.
        * @param bidderEmail The email of the bidder.
        * @return true if the offer was successfully submitted, false otherwise.
     */
    public boolean submitAuctionOffer(String productId, double offerAmount, String bidderEmail) {
        ShoppingProduct product = products.get(productId);
        if (product == null || product.getAuction() == null) {
            return false;
        }


        Auction auction = product.getAuction();
        return auction.submitBid(bidderEmail, offerAmount);
    }
    public void closeAuction(String productId) {
        ShoppingProduct product = products.get(productId);
        if (product == null || product.getAuction() == null) {
            throw new IllegalArgumentException("Product not found");
        }
        product.closeAuction();
    }
    /*
     * checks if a given email is the owner of the store
        *
        * @param email The email to check.
        * @return true if the email is the owner, false otherwise.
     */
    public boolean isOwner(String email) {
        return founderEmail.equals(email) || ownersAppointer.containsKey(email);
    }
    /*
     * checks if a given email is the owner or has manager permissions
        *
        * @param email The email to check.
        * @return true if the email is the owner or has manager permissions, false otherwise.
     */
    public boolean isOwnerOrHasManagerPermissions(String email){
        if (!isOwner(email) && !hasManagerPermission(email, ManagerPermission.MANAGE_PRODUCTS)) {
            throw new RuntimeException("User is not authorized to update products");
        }
        return true;
    }
    public boolean isOwnerOrHasManagerBidPermission(String email, ManagerPermission permission){
        if (!isOwner(email) && !hasManagerPermission(email, permission)) {
            throw new RuntimeException("User is not authorized to update products");
        }
        return true;
    }
    //Management 4.3
    /*
     * Appoints a new owner for the store.
        *
        * @param appointerEmail The email of the appointer.
        * @param newOwnerEmail  The email of the new owner.
        * @return true if the appointment was successful, false otherwise.
     */
    public boolean appointOwner(String appointerEmail, String newOwnerEmail,boolean isAdmin) {
        // Only Owner can appoint owner
        if (!isOwner(appointerEmail) && !isAdmin)
            throw new IllegalArgumentException("appointer email is not owner of this store");

        // Can't reappoint
        if (ownersAppointer.containsKey(newOwnerEmail) || founderEmail.equals(newOwnerEmail))
            throw new IllegalArgumentException("appointer email is already owner of this store");

        ownersAppointer.put(newOwnerEmail, appointerEmail);
        return true;
    }
    //4.4 part A
    /*
     * Removes an owner from the store.
        *
        * @param removerEmail   The email of the remover.
        * @param ownerToRemove  The email of the owner to remove.
        * @return true if the removal was successful, false otherwise.
     */
    public boolean removeOwner(String removerEmail, String ownerToRemove,boolean isAdmin) {
        if (!ownersAppointer.containsKey(ownerToRemove) && !isAdmin)
            throw new IllegalArgumentException("owner email is not owner of this store");

        String appointedBy = ownersAppointer.get(ownerToRemove);
        if (!removerEmail.equals(appointedBy) && !isAdmin)
            throw new IllegalArgumentException("appointer email is not owner of this store");

        removeAppointedCascade(ownerToRemove);
        ownersAppointer.remove(ownerToRemove);
        return true;
    }
    //4.4 part B
    /*
     * Removes an owner from the store.
        *
        * @param removerEmail   The email of the remover.
        * @param ownerToRemove  The email of the owner to remove.
        * @return true if the removal was successful, false otherwise.
     */
    private void removeAppointedCascade(String email) {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, String> entry : ownersAppointer.entrySet()) {
            if (entry.getValue().equals(email)) toRemove.add(entry.getKey());
        }
        for (String e : toRemove) {
            removeAppointedCascade(e);
            ownersAppointer.remove(e);
        }

        // Remove the managers too
        managers.entrySet().removeIf(entry -> entry.getValue().getAppointedBy().equals(email));
    }
    //4.5
    /*
     * Resigns the ownership of the store.
        *
        * @param ownerEmail The email of the owner resigning.
        * @return true if the resignation was successful, false otherwise.
     */
    public boolean resignOwnership(String ownerEmail) {
        if (founderEmail.equals(ownerEmail)) {
            throw new IllegalArgumentException("ownerEmail is already founder of this store");
        }
        if (!ownersAppointer.containsKey(ownerEmail)) {
            throw new IllegalArgumentException("owner email is not owner of this store");
        }
        removeAppointedCascade(ownerEmail);
        ownersAppointer.remove(ownerEmail);
        return true;
    }
    //4.6
    /*
     * Appoints a new manager for the store.
        *
        * @param ownerEmail    The email of the owner appointing the manager.
        * @param managerEmail  The email of the new manager.
        * @param permissions   The permissions granted to the manager.
        * @return true if the appointment was successful, false otherwise.
     */
    public boolean appointManager(String ownerEmail, String managerEmail, Set<ManagerPermission> permissions, boolean isAdmin) {
        if ((!isOwner(ownerEmail) && !isAdmin) || managers.containsKey(managerEmail) )
            throw new IllegalArgumentException("manager email is not owner of this store");
        managers.put(managerEmail, new ManagerData(managerEmail,ownerEmail, permissions));
        return true;
    }
    //4.7
    /*
     * Changes the permissions of a manager.
        *
        * @param ownerEmail    The email of the owner changing the permissions.
        * @param managerEmail  The email of the manager.
        * @param newPermissions The new permissions to be granted to the manager.
        * @return true if the permissions were successfully changed, false otherwise.
     */
    public boolean updateManagerPermissions(String ownerEmail, String managerEmail, Set<ManagerPermission> newPermissions) {
        ManagerData manager = managers.get(managerEmail);
        if (manager == null || !manager.getAppointedBy().equals(ownerEmail))
            throw new IllegalArgumentException("manager email is not owner of this store");
        manager.setPermissions(newPermissions);
        return true;
    }
    //4.11 Part A
    /*
     * Retrieves all owners of the store.
        *
        * @return A list of emails of all owners.
     */
    public List<String> getAllOwners() {
        Set<String> owners = new HashSet<>(ownersAppointer.keySet());
        owners.add(founderEmail);
        // Convert the set to a list and sort it
        List<String> ownersList = new ArrayList<>(owners);
        // Return an unmodifiable list to prevent external modification
        return ownersList;
    }
    //4.11 Part B
    /*
     * Retrieves all managers of the store.
        *
        * @return A list of emails of all managers.
     */
    public List<String> getAllManagers() {
        return Collections.unmodifiableList(new ArrayList<>(managers.keySet()));
    }
    //4.11 Part C
    /*
     * Checks if a manager has a specific permission.
        *
        * @param managerEmail The email of the manager.
        * @param permission   The permission to check.
        * @return true if the manager has the permission, false otherwise.
     */
    public boolean hasManagerPermission(String managerEmail, ManagerPermission permission) {
        ManagerData manager = managers.get(managerEmail);
        if (manager == null) {
            return false; // לא מוגדר כמנהל בכלל
        }
        return manager.hasTheRightPermission(permission);
    }

    //4.11 Part D
    /*
     * Retrieves the permissions of a manager.
        *
        * @param managerEmail The email of the manager.
        * @return A list of permissions granted to the manager.
     */
    public List<String> getManagerPermissions(String managerEmail) {
        ManagerData manager = managers.get(managerEmail);
        if (manager == null) {
            return Collections.emptyList();
        }
        List<String> permissionStrings = new ArrayList<>();
        for (ManagerPermission permission : manager.getPermissions()) {
            permissionStrings.add(permission.name());
        }
        return Collections.unmodifiableList(permissionStrings);
    }

    /*
     * rates the store.
        *
        * @param raterEmail The email of the rater.
        * @param score      The score given to the store (1-5).
        * @param review     The review text.
     */
    public void rateStore(String raterEmail , int score , String review ) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Rating msut be 1-5 ");

        }
        ratings.put(raterEmail, new Rating(score, review));

    }
    public ShoppingProduct rateProduct(String email,String productID, int rating, String review) {
        ShoppingProduct product = getProduct(productID);
        if (product == null) {
            throw new RuntimeException("Product not found in store ");
        }
        if (rating == 1 || rating > 5) {
            throw new IllegalArgumentException(("Rating must be between 1-5"));
        }
        product.addRating(email, rating, review);
        return product;
    }
    //rateStore
    /*
     * Retrieves the rating of the store.
        *
        * @return The average rating of the store.
     */
    public double averageRating() {
        if (ratings.isEmpty()) return 0.0;
        return ratings.values().stream()
                .mapToInt(r -> r.score)
                .average()
                .orElse(0.0);
    }

    /*
     * checks if a user has rated the store.
        *
        * @param email The email of the user.
        * @return true if the user has rated the store, false otherwise.
     */
    public boolean hasRated(String email) {
        return ratings.containsKey(email);
    }

    public Integer getProductQuantity(String productId){
        ShoppingProduct product = products.get(productId);
        if (product != null) {
            return product.getQuantity();
        }
        return null;
    }

    @Override
    public String toString() {
        return "Store{" +
                ", name='" + name + '\'' +
                ", ownerEmail='" + founderEmail + '\'' +
                ", isActive=" + isActive +
                ", balance=" + balance +
                ", products=" + products.keySet() +
                '}';
    }
    /**
     * Gets the store description.
     *
     * @return The store description.
     */
    public String getDescription() {
        return description;
    }
    /**
     * Sets the store description.
     *
     * @param description The new description for the store.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    public Map<String,Rating> getRatings() {
        return Collections.unmodifiableMap(ratings);
    }
    /**
     * Place a bid on a live auction.
     */
    public boolean bidOnAuction(String productId, String bidderEmail, double amount, Integer quantity) {
        ShoppingProduct p = requireProduct(productId);
        if (p.getAuction() == null)
            throw new RuntimeException("No auction running");
        return p.getAuction().submitBid(bidderEmail, amount);
    }
    /**
     * Get the current snapshot of this product's auction.
     */
    public Auction getAuctionInfo(String productId) {
        ShoppingProduct p = requireProduct(productId);
        return p.getAuction();
    }
    public void startAuction(String productId, double startingPrice, Date durationMillis) {
        ShoppingProduct p = requireProduct(productId);
        if (!isOwnerOrHasManagerPermissions(founderEmail))
            throw new RuntimeException("Not authorized");
        p.startAuction(startingPrice, durationMillis);
    }
    private ShoppingProduct requireProduct(String productId) {
        ShoppingProduct p = products.get(productId);
        if (p == null) throw new IllegalArgumentException("Unknown product " + productId);
        return p;
    }



    public List<String> getAllBidManagers() {
        List<String> bidManagers = new ArrayList<>();

        // Add all owners (including founder)
        bidManagers.add(founderEmail);
        bidManagers.addAll(ownersAppointer.keySet());

        // Add all managers who have MANAGE_BIDS permission
        for (Map.Entry<String, ManagerData> entry : managers.entrySet()) {
            if (entry.getValue().getPermissions().contains(ManagerPermission.MANAGE_BIDS)) {
                bidManagers.add(entry.getKey());
            }
        }

        return bidManagers;
    }

    public Auction getProductAuction(String productId) {
        ShoppingProduct product = products.get(productId);
        if (product != null) {
            return product.getAuction();
        }
        return null;
    }

    public List<Bid> getProductBids(String productId) {
        ShoppingProduct product = products.get(productId);
        if (product != null) {
            return product.getBids();
        }
        return null;
    }

    public List<Bid> getAllBids(){
        List<Bid> allBids = new ArrayList<>();
        for (ShoppingProduct product : products.values()) {
            allBids.addAll(product.getBids());
        }
        return allBids;
    }
    public Map<String, Rating> getAllRatings() {
        return Collections.unmodifiableMap(ratings);
    }
    public Map<String, Rating> getAllProductRatings(String productId) {
        ShoppingProduct p = products.get(productId);
        if (p != null) {
            return p.getAllRatings();
        }
        else{
            throw new RuntimeException("Product " + productId + " not found");
        }
    }
    public void giveManagementComment(String reviewerId,String reviewId, String comment) {
        if(isOwner(reviewerId)) {
            Review review = reviewIdToReview.get(reviewId);
            if (review != null) {
                review.setStoreComment(comment);
            } else {
                throw new IllegalArgumentException("Review not found for the given reviewer ID");
            }
        } else {
            throw new IllegalArgumentException("Only owners can give management comments");
        }
    }
    public List<Review> getStoreReviewsByUser(String userId) {
        List<Review> userReviews = new ArrayList<>();
        for (Review review : reviewIdToReview.values()) {
            if (review.getReviewerId().equals(userId)) {
                userReviews.add(review);
            }
        }
        return userReviews;
    }
    public List<Review> getAllStoreReviews() {
        return new ArrayList<>(reviewIdToReview.values());
    }
    public void giveStoreReview(String reviewerName, String reviewText, int rating) {
        String reviewerId = String.valueOf(inStoreProductId.incrementAndGet());
        Review review = new Review(reviewerId, reviewerName, reviewText, rating);
        reviewIdToReview.put(reviewerId, review);
    }
    public Review getStoreReviewById(String reviewId) {
        return reviewIdToReview.get(reviewId);
    }
    public void addSimpleDiscountToEntireStore(String operatorEmail,int percentage,String Coupon){
        if(!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");
        this.discounts.add(new SimpleDiscount(DiscountType.STORE,percentage,null,Coupon));
    }
    public void addSimpleDiscountToEntireCategoryInStore(String operatorEmail, String category, int percentage, String coupon) {
        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");
        discounts.add(new SimpleDiscount(DiscountType.CATEGORY, percentage,category, coupon));
    }

    public void addSimpleDiscountToSpecificProductInStorePercentage(String operatorEmail, String productId, int percentage, String coupon) {
        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");
        discounts.add(new SimpleDiscount(DiscountType.PRODUCT, percentage,productId, coupon));
    }

    public void addConditionalDiscountToEntireStore(String operatorEmail, int percentage,int minPrice ,String coupon) {
        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");
        discounts.add(new ConditionalDiscount(DiscountType.STORE, percentage,  minPrice,-1,-1,null, coupon));
    }

    public void addConditionalDiscountToEntireCategoryInStore(String operatorEmail, String category, int percentage,int minPrice , String coupon) {
        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");
        discounts.add(new ConditionalDiscount(DiscountType.CATEGORY, percentage, minPrice,-1,-1,category ,coupon));
    }

    public void addConditionalDiscountToSpecificProductInStorePercentage(String operatorEmail, String productId, int percentage,int minPrice,int minAmount, int maxAmount, String coupon) {
        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");
        discounts.add(new ConditionalDiscount(DiscountType.PRODUCT, percentage, minPrice,minAmount, maxAmount,productId, coupon));
    }

    public void addConditionalDiscountToSpecificProductInStorePercentage(String operatorEmail, String productId, int percentage, int minPrice, int minAmount, String coupon) {
        addConditionalDiscountToSpecificProductInStorePercentage(operatorEmail, productId, percentage, minPrice, minAmount, Integer.MAX_VALUE, coupon);
    }

    public void addLogicalCompositeConditionalDiscountToSpecificProductInStorePercentage(
            String operatorEmail,
            String productId,
            int percentage,
            int minPrice,
            List<String> productids,
            List<Integer> minAmounts,
            String coupon,
            String logicType) {

        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");

        // Validate input parameters
        if (productids == null || minAmounts == null || productids.size() != minAmounts.size()) {
            throw new IllegalArgumentException("Product IDs and amounts lists must be non-null and same size");
        }

        if (productids.isEmpty()) {
            throw new IllegalArgumentException("At least one product condition must be specified");
        }

        // Create individual conditions for each product-amount pair
        List<Condition> conditions = new ArrayList<>();

        // Add minimum price condition if minPrice > 0
        if (minPrice > 0) {
            Condition minPriceCondition = createMinPriceCondition(minPrice);
            conditions.add(minPriceCondition);
        }

        // Add product-amount conditions
        for (int i = 0; i < productids.size(); i++) {
            String conditionProductId = productids.get(i);
            Integer requiredAmount = minAmounts.get(i);

            // Create a condition that checks if the basket contains the specified product with at least the required amount
            Condition productAmountCondition = new Condition() {
                @Override
                public boolean isSatisfiedBy(List<ShoppingProduct> products, List<Integer> productAmounts) {
                    for (int j = 0; j < products.size(); j++) {
                        ShoppingProduct product = products.get(j);
                        Integer amount = productAmounts.get(j);

                        // Check if this product matches the condition and has sufficient quantity
                        if (product.getProductId().equals(conditionProductId) && amount >= requiredAmount) {
                            return true;
                        }
                    }
                    return false;
                }
            };

            conditions.add(productAmountCondition);
        }

        // Create the appropriate composite condition based on logic type
        CompositeCondition compositeDiscount;
        switch (logicType.toUpperCase()) {
            case "AND":
                compositeDiscount = new AndCondition(
                        conditions,
                        DiscountType.PRODUCT,
                        percentage,
                        productId,
                        coupon
                );
                break;
            case "OR":
                compositeDiscount = new OrCondition(
                        conditions,
                        DiscountType.PRODUCT,
                        percentage,
                        productId,
                        coupon
                );
                break;
            case "XOR":
                compositeDiscount = new XorCondition(
                        conditions,
                        DiscountType.PRODUCT,
                        percentage,
                        productId,
                        coupon
                );
                break;
            default:
                throw new IllegalArgumentException("Logic type must be AND, OR, or XOR");
        }

        // Add the composite discount to the store's discount list
        // Assuming you have a discounts collection in your store class
        discounts.add(compositeDiscount);
    }

    public void addLogicalCompositeConditionalDiscountToSpecificProductInStorePercentage(
            String operatorEmail,
            String productId,
            int percentage,
            int minPrice,
            List<String> productids,
            List<Integer> minAmounts,
            List<Integer> maxAmounts,
            String coupon,
            String logicType) {

        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");

        if (maxAmounts == null || maxAmounts.stream().allMatch(x -> x == null)) {
            addLogicalCompositeConditionalDiscountToSpecificProductInStorePercentage(operatorEmail, productId, percentage, minPrice, productids, minAmounts, coupon, logicType);
            return;
        }

            // Validate input parameters
        if (productids == null || minAmounts == null || productids.size() != minAmounts.size()) {
            throw new IllegalArgumentException("Product IDs and amounts lists must be non-null and same size");
        }

        if (productids.isEmpty()) {
            throw new IllegalArgumentException("At least one product condition must be specified");
        }

        // Create individual conditions for each product-amount pair
        List<Condition> conditions = new ArrayList<>();

        // Add minimum price condition if minPrice > 0
        if (minPrice > 0) {
            Condition minPriceCondition = createMinPriceCondition(minPrice);
            conditions.add(minPriceCondition);
        }

        // Add product-amount conditions
        for (int i = 0; i < productids.size(); i++) {
            String conditionProductId = productids.get(i);
            Integer requiredMinAmount = minAmounts.get(i);
            Integer requiredMaxAmount = maxAmounts.get(i);

            // Create a condition that checks if the basket contains the specified product with at least the required amount
            Condition productAmountCondition = new Condition() {
                @Override
                public boolean isSatisfiedBy(List<ShoppingProduct> products, List<Integer> productAmounts) {
                    for (int j = 0; j < products.size(); j++) {
                        ShoppingProduct product = products.get(j);
                        Integer amount = productAmounts.get(j);

                        // Check if this product matches the condition and has sufficient quantity
                        if (product.getProductId().equals(conditionProductId) && amount >= requiredMinAmount && amount <= requiredMaxAmount) {
                            return true;
                        }
                    }
                    return false;
                }
            };

            conditions.add(productAmountCondition);
        }

        // Create the appropriate composite condition based on logic type
        CompositeCondition compositeDiscount;
        switch (logicType.toUpperCase()) {
            case "AND":
                compositeDiscount = new AndCondition(
                        conditions,
                        DiscountType.PRODUCT,
                        percentage,
                        productId,
                        coupon
                );
                break;
            case "OR":
                compositeDiscount = new OrCondition(
                        conditions,
                        DiscountType.PRODUCT,
                        percentage,
                        productId,
                        coupon
                );
                break;
            case "XOR":
                compositeDiscount = new XorCondition(
                        conditions,
                        DiscountType.PRODUCT,
                        percentage,
                        productId,
                        coupon
                );
                break;
            default:
                throw new IllegalArgumentException("Logic type must be AND, OR, or XOR");
        }

        // Add the composite discount to the store's discount list
        // Assuming you have a discounts collection in your store class
        discounts.add(compositeDiscount);
    }

    public void addLogicalCompositeConditionalDiscountToEntireStore(
            String operatorEmail,
            int percentage,
            int minPrice,
            List<String> productIds,
            List<Integer> minAmounts,
            String coupon,
            String logicType) {

        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");

        if (productIds == null || minAmounts == null || productIds.size() != minAmounts.size()) {
            throw new IllegalArgumentException("Product IDs and amounts lists must be non-null and same size");
        }

        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("At least one product condition must be specified");
        }

        List<Condition> conditions = new ArrayList<>();

        if (minPrice > 0) {
            conditions.add(createMinPriceCondition(minPrice));
        }

        for (int i = 0; i < productIds.size(); i++) {
            String pid = productIds.get(i);
            int qty = minAmounts.get(i);

            Condition c = new Condition() {
                @Override
                public boolean isSatisfiedBy(List<ShoppingProduct> products, List<Integer> productAmounts) {
                    for (int j = 0; j < products.size(); j++) {
                        if (products.get(j).getProductId().equals(pid) && productAmounts.get(j) >= qty)
                            return true;
                    }
                    return false;
                }
            };

            conditions.add(c);
        }

        CompositeCondition discount;
        switch (logicType.toUpperCase()) {
            case "AND":
                discount = new AndCondition(conditions, DiscountType.STORE, percentage, null, coupon);
                break;
            case "OR":
                discount = new OrCondition(conditions, DiscountType.STORE, percentage, null, coupon);
                break;
            case "XOR":
                discount = new XorCondition(conditions, DiscountType.STORE, percentage, null, coupon);
                break;
            default:
                throw new IllegalArgumentException("Logic type must be AND, OR, or XOR");
        }

        discounts.add(discount);
    }

    public void addLogicalCompositeConditionalDiscountToEntireStore(
            String operatorEmail,
            int percentage,
            int minPrice,
            List<String> productIds,
            List<Integer> minAmounts,
            List<Integer> maxAmounts,
            String coupon,
            String logicType) {

        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");

        if (maxAmounts == null || maxAmounts.stream().allMatch(x -> x == null)) {
            addLogicalCompositeConditionalDiscountToEntireStore(operatorEmail, percentage, minPrice, productIds, minAmounts, coupon, logicType);
            return;
        }

        if (productIds == null || minAmounts == null || productIds.size() != minAmounts.size() || productIds.size() != maxAmounts.size()) {
            throw new IllegalArgumentException("Product IDs and amounts lists must be non-null and same size");
        }

        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("At least one product condition must be specified");
        }

        List<Condition> conditions = new ArrayList<>();

        if (minPrice > 0) {
            conditions.add(createMinPriceCondition(minPrice));
        }

        for (int i = 0; i < productIds.size(); i++) {
            String pid = productIds.get(i);
            int qtyMin = minAmounts.get(i);
            int qtyMax = maxAmounts.get(i);

            Condition c = new Condition() {
                @Override
                public boolean isSatisfiedBy(List<ShoppingProduct> products, List<Integer> productAmounts) {
                    for (int j = 0; j < products.size(); j++) {
                        if (products.get(j).getProductId().equals(pid) && productAmounts.get(j) >= qtyMin && productAmounts.get(j) <= qtyMax)
                            return true;
                    }
                    return false;
                }
            };

            conditions.add(c);
        }

        CompositeCondition discount;
        switch (logicType.toUpperCase()) {
            case "AND":
                discount = new AndCondition(conditions, DiscountType.STORE, percentage, null, coupon);
                break;
            case "OR":
                discount = new OrCondition(conditions, DiscountType.STORE, percentage, null, coupon);
                break;
            case "XOR":
                discount = new XorCondition(conditions, DiscountType.STORE, percentage, null, coupon);
                break;
            default:
                throw new IllegalArgumentException("Logic type must be AND, OR, or XOR");
        }

        discounts.add(discount);
    }


    public void addLogicalCompositeConditionalDiscountToEntireCategoryInStore(
            String operatorEmail,
            String category,
            int percentage,
            int minPrice,
            List<String> productIds,
            List<Integer> minAmounts,
            String coupon,
            String logicType) {

        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");

        if (productIds == null || minAmounts == null || productIds.size() != minAmounts.size()) {
            throw new IllegalArgumentException("Product IDs and amounts lists must be non-null and same size");
        }

        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("At least one product condition must be specified");
        }

        List<Condition> conditions = new ArrayList<>();

        if (minPrice > 0) {
            conditions.add(createMinPriceCondition(minPrice));
        }

        for (int i = 0; i < productIds.size(); i++) {
            String pid = productIds.get(i);
            int qty = minAmounts.get(i);

            Condition c = new Condition() {
                @Override
                public boolean isSatisfiedBy(List<ShoppingProduct> products, List<Integer> productAmounts) {
                    for (int j = 0; j < products.size(); j++) {
                        if (products.get(j).getProductId().equals(pid) && productAmounts.get(j) >= qty)
                            return true;
                    }
                    return false;
                }
            };

            conditions.add(c);
        }

        CompositeCondition discount;
        switch (logicType.toUpperCase()) {
            case "AND":
                discount = new AndCondition(conditions, DiscountType.CATEGORY, percentage, category, coupon);
                break;
            case "OR":
                discount = new OrCondition(conditions, DiscountType.CATEGORY, percentage, category, coupon);
                break;
            case "XOR":
                discount = new XorCondition(conditions, DiscountType.CATEGORY, percentage, category, coupon);
                break;
            default:
                throw new IllegalArgumentException("Logic type must be AND, OR, or XOR");
        }

        discounts.add(discount);
    }

    public void addLogicalCompositeConditionalDiscountToEntireCategoryInStore(
            String operatorEmail,
            String category,
            int percentage,
            int minPrice,
            List<String> productIds,
            List<Integer> minAmounts,
            List<Integer> maxAmounts,
            String coupon,
            String logicType) {

        if (!isOwnerOrHasManagerPermissions(operatorEmail))
            throw new IllegalArgumentException("Only owners can control discount");

        if (maxAmounts == null || maxAmounts.stream().allMatch(x -> x == null)) {
            addLogicalCompositeConditionalDiscountToEntireCategoryInStore(operatorEmail, category, percentage, minPrice, productIds, minAmounts, coupon, logicType);
            return;
        }

        if (productIds == null || minAmounts == null || productIds.size() != minAmounts.size() || productIds.size() != maxAmounts.size()) {
            throw new IllegalArgumentException("Product IDs and amounts lists must be non-null and same size");
        }

        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("At least one product condition must be specified");
        }

        List<Condition> conditions = new ArrayList<>();

        if (minPrice > 0) {
            conditions.add(createMinPriceCondition(minPrice));
        }

        for (int i = 0; i < productIds.size(); i++) {
            String pid = productIds.get(i);
            int qtyMin = minAmounts.get(i);
            int qtyMax = maxAmounts.get(i);

            Condition c = new Condition() {
                @Override
                public boolean isSatisfiedBy(List<ShoppingProduct> products, List<Integer> productAmounts) {
                    for (int j = 0; j < products.size(); j++) {
                        if (products.get(j).getProductId().equals(pid) && productAmounts.get(j) >= qtyMin && productAmounts.get(j) <= qtyMax) {
                            return true;
                        }
                    }
                    return false;
                }
            };

            conditions.add(c);
        }

        CompositeCondition discount;
        switch (logicType.toUpperCase()) {
            case "AND":
                discount = new AndCondition(conditions, DiscountType.CATEGORY, percentage, category, coupon);
                break;
            case "OR":
                discount = new OrCondition(conditions, DiscountType.CATEGORY, percentage, category, coupon);
                break;
            case "XOR":
                discount = new XorCondition(conditions, DiscountType.CATEGORY, percentage, category, coupon);
                break;
            default:
                throw new IllegalArgumentException("Logic type must be AND, OR, or XOR");
        }

        discounts.add(discount);
    }

    /**
     * Sets the store discounts to a MaxDiscount composed of the given discounts.
     *
     * @param discountList List of discounts to be combined with MaxDiscount.
     */
    public void addMaxDiscounts(List<Discount> discountList) {
        if (!isOwnerOrHasManagerPermissions(founderEmail)) {
            throw new IllegalArgumentException("Only owners can control discount");
        }
        this.discounts = new MaxDiscount(discountList);
    }


    /**
     * Calculate the maximum discount amount for the given product based on all store discounts.
     * @param product the shopping product with quantity
     * @return the discount amount (money) for this product
     */
    public double calculateDiscount(ShoppingProduct product,int quantity) {
        return this.discounts.calculate(product, quantity);
    }
    public void applyCoupon(String coupon) {
        this.discounts.applyCoupon(coupon);
    }
    public void activateConditionDiscount(int minPrice){
        this.discounts.activateDiscount(minPrice);
    }
    public void deactivateConditionDiscount(){
        this.discounts.deactivateDiscount();
    }
    public Map<String, Double> calculateDiscountForBasket(Map<ShoppingProduct, Integer> productsWithQuantities) {
        return this.discounts.calculateDiscountForBasket(productsWithQuantities);
    }
    // Helper method to create a minimum price condition if needed
    private Condition createMinPriceCondition(int minPrice) {
        return new Condition() {
            @Override
            public boolean isSatisfiedBy(List<ShoppingProduct> products, List<Integer> amounts) {
                double totalPrice = 0.0;
                for (int i = 0; i < products.size(); i++) {
                    totalPrice += products.get(i).getPrice() * amounts.get(i);
                }
                return totalPrice >= minPrice;
            }
        };
    }
    public void setDiscounts(NumericalComposite discounts) {
        this.discounts = discounts;
    }
    public NumericalComposite getDiscounts() {return this.discounts;}
}
