package com.SEGroup.Service;

import java.util.List;

import com.SEGroup.DTO.BasketDTO;
import com.SEGroup.Domain.IAuthenticationService;
import com.SEGroup.Domain.IUserRepository;
import com.SEGroup.Domain.User.ShoppingCart;
import com.SEGroup.Domain.User.User;
import com.SEGroup.Infrastructure.PasswordEncoder;

/**
 * UserService handles user-related operations such as login, registration, cart management, and user deletion.
 * It interacts with the authentication service, user repository, and guest service.
 */
public class UserService {

    private final GuestService guestService;
    private final IUserRepository userRepository;
    private final IAuthenticationService authenticationService;
    private PasswordEncoder passwordEncoder;

    /**
     * Constructs a new UserService instance with the provided dependencies.
     *
     * @param guestService The service responsible for managing guest sessions.
     * @param userRepository The repository for managing user data.
     * @param authenticationService The service responsible for user authentication.
     */
    public UserService(GuestService guestService, IUserRepository userRepository,
                       IAuthenticationService authenticationService) {
        this.guestService = guestService;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
        passwordEncoder = new PasswordEncoder();
    }

    /**
     * Creates a guest session.
     * Logs the creation of the guest session.
     *
     * @return A Result object containing the guest session ID if successful, or an error message.
     */
    public Result<String> guestLogin() {
        LoggerWrapper.info("Creating a guest session.");  // Log the creation of guest session
        return guestService.createGuestSession();
    }

    /**
     * Registers a new user with the provided details.
     * Logs the registration process.
     *
     * @param username The username of the user to register.
     * @param email The email of the user to register.
     * @param password The password of the user to register.
     * @return A Result object indicating success or failure of the registration.
     */
    public Result<Void> register(String username, String email, String password) {
        try {
            userRepository.addUser(username, email, passwordEncoder.encrypt(password));
            LoggerWrapper.info("User registered successfully: " + username + ", Email: " + email);  // Log successful registration
            return Result.success(null);
        } catch (Exception e) {
            LoggerWrapper.error("Failed to register user: " + e.getMessage(), e);  // Log error on failure
            return Result.failure("Failed to register user: " + e.getMessage());
        }
    }

    /**
     * Logs in a user with the provided email and password.
     * Logs the login process.
     *
     * @param email The email of the user trying to login.
     * @param password The password of the user trying to login.
     * @return A Result object containing the session key if successful, or an error message.
     */
    public Result<String> login(String email, String password) {
        try {
            User user = userRepository.findUserByEmail(email);
            authenticationService.matchPassword(user.getPassword(), password);
            String sessionKey = authenticationService.authenticate(email);
            LoggerWrapper.info("User logged in successfully: " + email);  // Log successful login
            return Result.success(sessionKey);
        } catch (Exception e) {
            LoggerWrapper.error("Failed to login user: " + e.getMessage(), e);  // Log error on failure
            return Result.failure("Failed to login ");
        }
    }

    /**
     * Logs out a user by invalidating their session key.
     * Logs the logout process.
     *
     * @param sessionKey The session key of the user to log out.
     * @return A Result object indicating success or failure of the logout operation.
     */
    public Result<Void> logout(String sessionKey) {
        try {
            authenticationService.checkSessionKey(sessionKey);
            authenticationService.invalidateSession(sessionKey);
            LoggerWrapper.info("User logged out successfully. Session key: " + sessionKey);  // Log successful logout
            return Result.success(null);
        } catch (Exception e) {
            LoggerWrapper.error("Failed to logout: " + e.getMessage(), e);  // Log error on failure
            return Result.failure("Failed to logout: " + e.getMessage());
        }
    }

    /**
     * Deletes a user based on the provided email.
     * Logs the user deletion process.
     *
     * @param email The email of the user to delete.
     * @return A Result object indicating success or failure of the deletion.
     */
    public Result<Void> deleteUser(String email) {
        try {
            User user = userRepository.findUserByEmail(email);
            if (user == null) {
                LoggerWrapper.info("User not found: " + email);  // Log user not found scenario
                return Result.failure("User not found: " + email);
            }
            userRepository.deleteUser(email);
            LoggerWrapper.info("User deleted successfully: " + email);  // Log successful deletion
            return Result.success(null);
        } catch (Exception e) {
            LoggerWrapper.error("Failed to delete user: " + e.getMessage(), e);  // Log error on failure
            return Result.failure("Failed to delete user: " + e.getMessage());
        }
    }

    public Result<String> addToCart(String sessionKey,
                                    String productId,
                                    String storeName) {
        try {
            authenticationService.checkSessionKey(sessionKey);
            String owner = authenticationService.getUserBySession(sessionKey);
            if (owner.startsWith("g-")) {
                ShoppingCart cart = guestService.cart(sessionKey);
                cart.add(storeName, productId, 1);
            } else {
                User user = userRepository.findUserByEmail(owner);
                user.addToCart(storeName, productId);
            }
            return Result.success("Added item to cart successfully!");
        } catch (Exception e) {
            return Result.failure("Cannot add to cart: " + e.getMessage());
        }
    }
    /**
     * Purchases the shopping cart for a user.
     * Logs the cart purchase process.
     *
     * @param sessionKey The session key of the authenticated user.
     * @param email The email of the user making the purchase.
     * @return A Result object indicating success or failure of the purchase operation.
     */
    public Result<Void> purchaseShoppingCart(String sessionKey, String email) {
        try {
            authenticationService.checkSessionKey(sessionKey);
            userRepository.checkIfExist(email);

            // WILL ADD METHODS TO HERE

            LoggerWrapper.info("Purchased shopping cart for user: " + email);  // Log cart purchase
            return Result.success(null);
        } catch (Exception e) {
            LoggerWrapper.error("Failed to purchase shopping cart: " + e.getMessage(), e);  // Log error on failure
            return Result.failure("Failed to purchase shopping cart: " + e.getMessage());
        }
    }

    /**
     * Removes a product from the user's cart.
     * Logs the removal of the product from the cart.
     *
     * @param sessionKey The session key of the authenticated user.
     * @param productId The ID of the product to remove from the cart.
     * @param storeName The name of the store where the product is located.
     * @return A Result object indicating success or failure of the operation.
     */
    public Result<String> removeFromCart(String sessionKey,
                                         String productId,
                                         String storeName) {
        return changeCartQuantity(sessionKey, productId, storeName, 0);
    }

    /**
     * Modifies the quantity of a product in the user's cart.
     * Logs the modification of the product quantity.
     *
     * @param sessionKey The session key of the authenticated user.
     * @param productId The ID of the product to modify.
     * @param storeName The name of the store where the product is located.
     * @param newQty The new quantity of the product.
     * @return A Result object indicating success or failure of the operation.
     */
    public Result<String> changeCartQuantity(String sessionKey,
                                             String productId,
                                             String storeName,
                                             int newQty) {
        try {
            authenticationService.checkSessionKey(sessionKey);
            String owner = authenticationService.getUserBySession(sessionKey);

            if (owner.startsWith("g-")) {
                ShoppingCart cart = guestService.cart(sessionKey);
                cart.changeQty(storeName, productId, newQty);
            } else {
                User user = userRepository.findUserByEmail(owner);
                user.cart().changeQty(storeName, productId, newQty);
            }

            return Result.success("Cart updated");
        } catch (Exception e) {
            return Result.failure("Cannot update cart: " + e.getMessage());
        }
    }



    public Result<List<BasketDTO>> getUserCart(String sessionKey, String email) {
        try {
            authenticationService.checkSessionKey(sessionKey);
            LoggerWrapper.info("Retrieved user cart: " + email);
            return Result.success(userRepository.getUserCart(email));
        } catch (Exception e) {
            LoggerWrapper.error("Error retrieving user cart: " + e.getMessage(), e);  // Log error on failure
            return Result.failure(e.getMessage());
        }
    }


    public Result<String> getUserName(String sessionKey, String email){
        try {
            authenticationService.checkSessionKey(sessionKey);
            LoggerWrapper.info("Retrieved user name: " + email);
            return Result.success(userRepository.getUserName(email));
        } catch (Exception e) {
            LoggerWrapper.error("Error retrieving user name: " + e.getMessage(), e);  // Log error on failure
            return Result.failure(e.getMessage());
        }
    }

}