package com.SEGroup.Domain.Store;
import com.SEGroup.Domain.Discount.Discount;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Represents a store, including its products, owners, managers, and policies.
 */

public class Store {
    //fields
    private static int idCounter = 0;
    private final int id;
    private String name;
    private String founderEmail;
    private boolean isActive;
    private double balance;
    private int numOfitems;
    private final AtomicInteger inStoreProductId = new AtomicInteger(-1);

    //products and reviews
    private Map<String, ShoppingProduct> products = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<String> reviews = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Rating> ratings = new java.util.concurrent.ConcurrentHashMap<>();


    /*
     * Represents a rating given to the store, including the score and review.
     */
    public static final class Rating{


        final int score;
        final String review ;
        Rating (int s , String r ){score =s;review =r; }
    }
    // Disocunt and policy fields
    private PurchasePolicy purchasePolicy;
    private List<Discount> discounts;

    //Owners and managers
    private final Map<String, String> ownersAppointer = new java.util.concurrent.ConcurrentHashMap<>(); // email → appointedBy
    private final Map<String, ManagerData> managers = new java.util.concurrent.ConcurrentHashMap<>(); // email → metadata

    public Store(String name, String founderEmail) {
        //field
        this.id = ++idCounter;
        this.name = name;
        this.founderEmail = founderEmail;
        this.isActive = true;
        this.balance = 0.0;

        //product and reviews
        this.products = new java.util.concurrent.ConcurrentHashMap<>();
        this.ratings.clear();
        this.reviews.clear();

        // Disocunt and policy fields
        this.purchasePolicy = new PurchasePolicy(0,0);
        this.discounts = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
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
    public String addProductToStore(String email, String storeName, String catalogID,String product_name, String description, double price, int quantity){
        if(quantity == 0)
            throw new IllegalArgumentException("quantity cannot be 0 ");
        if (isOwnerOrHasManagerPermissions(email)) {
            String productId = String.valueOf(inStoreProductId.incrementAndGet());
            ShoppingProduct product = new ShoppingProduct(storeName, catalogID,productId, product_name, description, price, quantity);
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

    public void removeProduct(String productId) {
        products.remove(productId);
    }

    public Collection<ShoppingProduct> getAllProducts() {
        return products.values();
    }

    public void addToBalance(double amount){
        this.balance += amount;
    }
    /*
     * Submits a bid to a shopping item.
        *
        * @param itemName   The name of the item.
        * @param bidAmount  The amount of the bid.
        * @param bidderEmail The email of the bidder.
        * @return true if the bid was successfully submitted, false otherwise.
     */
    public boolean submitBidToShoppingItem(String itemName, double bidAmount, String bidderEmail) {
        ShoppingProduct product = products.get(itemName);

        if (product == null) {
            return false;
        }

        if (bidAmount <= 0 || bidderEmail == null || bidderEmail.isBlank()) {
            return false;
        }

        product.addBid(bidderEmail, bidAmount);
        return true;
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
    //Management 4.3
    /*
     * Appoints a new owner for the store.
        *
        * @param appointerEmail The email of the appointer.
        * @param newOwnerEmail  The email of the new owner.
        * @return true if the appointment was successful, false otherwise.
     */
    public boolean appointOwner(String appointerEmail, String newOwnerEmail) {
        // Only Owner can appoint owner
        if (!isOwner(appointerEmail))
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
    public boolean removeOwner(String removerEmail, String ownerToRemove) {
        if (!ownersAppointer.containsKey(ownerToRemove))
            throw new IllegalArgumentException("owner email is not owner of this store");

        String appointedBy = ownersAppointer.get(ownerToRemove);
        if (!removerEmail.equals(appointedBy))
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
    public boolean appointManager(String ownerEmail, String managerEmail, Set<ManagerPermission> permissions) {
        if (!isOwner(ownerEmail) || managers.containsKey(managerEmail))
            throw new IllegalArgumentException("manager email is not owner of this store");
        managers.put(managerEmail, new ManagerData(ownerEmail, permissions));
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
                "id=" + id +
                ", name='" + name + '\'' +
                ", ownerEmail='" + founderEmail + '\'' +
                ", isActive=" + isActive +
                ", balance=" + balance +
                ", products=" + products.keySet() +
                '}';
    }
}
