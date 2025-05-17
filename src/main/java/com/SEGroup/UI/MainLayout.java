package com.SEGroup.UI;
import com.SEGroup.Domain.User.Role;
import com.SEGroup.Infrastructure.NotificationCenter.NotificationEndpoint;
import com.SEGroup.UI.Views.*;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Set;



@SpringComponent
@UIScope
public class MainLayout extends AppLayout {
    private final NotificationCenter nc;
    private final NotificationEndpoint notificationEndpoint;   // <<— new
    private Disposable notificationSubscription;              // <<— new
    private Button signInBtn;
    private Button signUpBtn;
    private Span greeting;
    private String userEmail;
    private String userName;
    static MainLayout instance;
    private String sessionKey;
    private Tab baseCatalogTab;
    public static Button searchBtn;
    public static TextField search;

    // Permission management buttons
    private Button manageStoreBtn;
    private Button addProductBtn;
    private Button ownersBtn;
    private Button rolesBtn;

    // Notification button with badge
    private Button notificationBtn;
    private Span notificationBadge;
    private int notificationCount = 0;
    private Tabs tabs;

    private final Button logout = new Button("Logout");

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        nc.register(UI.getCurrent());

        // Check permissions after navigation
        checkRolesAndUpdateUI();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (notificationSubscription != null && !notificationSubscription.isDisposed()) {
            notificationSubscription.dispose();
        }

        nc.unregister(UI.getCurrent());

        super.onDetach(event);
    }


    @Autowired
    public MainLayout(NotificationCenter nc, NotificationEndpoint notificationEndpoint) {
        this.nc = nc;
        this.notificationEndpoint = notificationEndpoint;
        instance = this;

        // Logo setup
        Image logoImage = new Image("/images/icon_eCommerce.png", "Logo");
        logoImage.setHeight("60px");
        logoImage.getStyle().set("max-height", "100%");

        // Wrap the logo in a RouterLink to "homeView"
        RouterLink homeLink = new RouterLink("", HomeView.class);
        homeLink.add(logoImage);
        homeLink.getStyle().set("display", "flex").set("align-items", "center");

        // Title with icon
        HorizontalLayout titleWithIcon = new HorizontalLayout(homeLink);
        titleWithIcon.setAlignItems(FlexComponent.Alignment.CENTER);

        // Enhanced search functionality
        search = new TextField();
        search.setPlaceholder("Search products...");
        search.setWidth("300px");
        search.getStyle()
                .set("border-radius", "20px")
                .set("padding", "0.3em 1em");

        // Add key press listener for Enter key
        search.addKeyPressListener(Key.ENTER, e -> performSearch());

        searchBtn = new Button(VaadinIcon.SEARCH.create());
        searchBtn.getStyle()
                .set("border-radius", "50%")
                .set("background-color", "#3f3f46")
                .set("color", "#fff")
                .set("min-width", "40px")
                .set("min-height", "40px");
        searchBtn.addClickListener(e -> performSearch());

        HorizontalLayout searchBar = new HorizontalLayout(search, searchBtn);
        searchBar.setSpacing(true);
        searchBar.setAlignItems(FlexComponent.Alignment.CENTER);

        greeting = new Span("Hello Guest");

        greeting.getStyle()
                .set("color", "#1976d2")
                .set("margin-left", "1em");

        // Auth buttons
        HorizontalLayout auth = new HorizontalLayout();
        auth.setSpacing(true);
        auth.setAlignItems(FlexComponent.Alignment.CENTER);
        signInBtn = new Button("Sign in", e -> UI.getCurrent().navigate("signin"));
        signUpBtn = new Button("Sign up", e -> UI.getCurrent().navigate("signup"));
        auth.add(
                signInBtn,
                signUpBtn,
                greeting
        );

        // Initialize permission buttons (initially hidden)
        manageStoreBtn = new Button("Manage Store", e -> UI.getCurrent().navigate("store-management"));
        addProductBtn = new Button("Add Product", e -> showAddProductDialog());
        ownersBtn = new Button("Manage Owners", e -> UI.getCurrent().navigate("owners"));
        rolesBtn = new Button("Manage Permissions", e -> UI.getCurrent().navigate("roles"));

        // Hide all permission buttons by default
        manageStoreBtn.setVisible(false);
        addProductBtn.setVisible(false);
        ownersBtn.setVisible(false);
        rolesBtn.setVisible(false);

        // Add permission buttons to a layout
        HorizontalLayout permissionButtons = new HorizontalLayout(
                manageStoreBtn, addProductBtn, ownersBtn, rolesBtn
        );
        permissionButtons.setSpacing(true);
        permissionButtons.setAlignItems(FlexComponent.Alignment.CENTER);

        // Create notification button with badge
        notificationBtn = createNotificationButton();

        // Cart icon with badge
        Button cartButton = new Button(new Icon(VaadinIcon.CART), e -> UI.getCurrent().navigate("cart"));
        cartButton.getStyle()
                .set("background-color", "transparent")
                .set("color", "#1976d2")
                .set("border", "none");

        // Action buttons layout
        HorizontalLayout actionButtons = new HorizontalLayout(
                notificationBtn,
                cartButton
        );
        actionButtons.setSpacing(true);
        actionButtons.setAlignItems(FlexComponent.Alignment.CENTER);

        // Top navbar
        HorizontalLayout navbar = new HorizontalLayout(
                titleWithIcon,
                searchBar,
                createSpacer(),
                actionButtons,
                auth,
                logout
        );
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setWidthFull();
        navbar.setPadding(true);
        navbar.setSpacing(true);
        navbar.setHeight("80px");
        navbar.getStyle().set("min-height", "80px");

        Div navbarWrapper = new Div(navbar);
        navbarWrapper.setWidthFull();
        navbarWrapper.setHeight("80px");
        navbarWrapper.getStyle()
                .set("background-color", "#e0e0e0")
                .set("padding", "0 2em")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
                .set("overflow", "hidden");

        addToNavbar(navbarWrapper);


        refreshHeader();          // first paint = guest

        logout.addClickListener(e -> {
            SecurityContextHolder.closeSession();
            refreshHeader();
            UI.getCurrent().navigate("signin");
        });
        baseCatalogTab = nav("Base Catalog", GeneralCatalogView.class, VaadinIcon.BARCODE);
        baseCatalogTab.setVisible(false);
        // Navigation drawer
        tabs = new Tabs(
                nav("Home", HomeView.class, VaadinIcon.HOME),
                nav("MarketPlace", CatalogView.class, VaadinIcon.LIST),
                nav("My Cart", CartView.class, VaadinIcon.CART),
                baseCatalogTab,
                nav("All Stores", AllStoresView.class, VaadinIcon.STORAGE),
                nav("Notifications", NotificationView.class, VaadinIcon.BELL),
                nav("Purchase History", PurchaseHistoryView.class, VaadinIcon.ARCHIVE),
                nav("Admin Panel", AdminView.class, VaadinIcon.USER)
        );

        tabs.getTabAt(6).setVisible(false);
        tabs.getTabAt(7).setVisible(false);

        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        addToDrawer(tabs);
        setPrimarySection(Section.DRAWER);
        instance = this;

        // Add navigation listener to check permissions after each navigation
        UI.getCurrent().addAfterNavigationListener(event -> {
            checkRolesAndUpdateUI();
        });

        // Subscribe to notification events
        subscribeToNotifications();

        // Initialize with guest login
        sessionKey = ServiceLocator.getUserService().guestLogin().getData();
    }

    public static String getFullErrorMessage(Exception ex){
        StringBuilder sb = new StringBuilder();
        List<StackTraceElement> stackTrace = List.of(ex.getStackTrace());
        sb.append(ex.getMessage()).append("\n");
        sb.append("stackTrace:\n");
        for (StackTraceElement element : stackTrace) {
            sb.append("\t at ").append(element.toString()).append("\n");
        }
        sb.append("Caused by: ").append(ex.getCause()).append("\n");
        return sb.toString();
    }

    private Button createNotificationButton() {
        Button button = new Button(new Icon(VaadinIcon.BELL), e -> UI.getCurrent().navigate("notifications"));
        button.getStyle()
                .set("background-color", "transparent")
                .set("color", "#1976d2")
                .set("border", "none")
                .set("position", "relative");

        // Create notification badge
        notificationBadge = new Span("0");
        notificationBadge.getStyle()
                .set("background-color", "red")
                .set("color", "white")
                .set("border-radius", "50%")
                .set("width", "20px")
                .set("height", "20px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "12px")
                .set("position", "absolute")
                .set("top", "0")
                .set("right", "0");

        // Hide badge when count is 0
        notificationBadge.setVisible(false);

        button.getElement().appendChild(notificationBadge.getElement());
        return button;
    }

    public void increaseNotificationCount() {
        notificationCount++;
        notificationBadge.setText(String.valueOf(notificationCount));
        notificationBadge.setVisible(true);
    }

    public void resetNotificationCount() {
        notificationCount = 0;
        notificationBadge.setText("0");
        notificationBadge.setVisible(false);
    }

    private void subscribeToNotifications() {
        // This would subscribe to notification events from the backend
        // When a notification is received, increase the count
        // Implementation depends on how your notification system works
    }

    private void performSearch() {
        String query = search.getValue().trim();
        if (!query.isEmpty()) {
            // Navigate to the catalog view with search query
            UI.getCurrent().navigate(
                    "catalog",
                    new QueryParameters(Map.of("search", List.of(query)))
            );
        }
    }

    // Show add product dialog
    private void showAddProductDialog() {
        // Add your dialog code here
        Notification.show("Add product dialog would show here");
    }

    public static MainLayout getInstance() {
        return instance;
    }

    @Override

    protected void onAttach(AttachEvent event) {
        super.onAttach(event);

        // Unsubscribe first if already subscribed
        if (notificationSubscription != null && !notificationSubscription.isDisposed()) {
            notificationSubscription.dispose();
            notificationSubscription = null;
        }

        // Subscribe to notifications if user is logged in
        if (SecurityContextHolder.isLoggedIn()) {
            String user = SecurityContextHolder.email();
            System.out.println("MainLayout subscribing to notifications for: " + user);

            UI ui = event.getUI();
            try {
                notificationSubscription = notificationEndpoint
                        .subscribe(user)
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                notification -> {
                                    if (ui.isAttached()) {
                                        ui.access(() -> handleNewNotification(notification.getMessage()));
                                    }
                                },
                                error -> System.err.println("Error in MainLayout notification subscription: " + error.getMessage()),
                                () -> System.out.println("MainLayout notification subscription completed for user: " + user)
                        );

                System.out.println("Successfully subscribed to notifications in MainLayout");
            } catch (Exception e) {
                System.err.println("Failed to subscribe to notifications in MainLayout: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private Component createSpacer() {
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");
        return spacer;
    }

    private Tab nav(String caption, Class<? extends Component> viewClass, VaadinIcon icon) {
        RouterLink link = new RouterLink("", viewClass);
        link.add(icon.create(), new Span(caption));
        link.getElement().getStyle()
                .set("align-items", "center")
                .set("gap", "0.5em")
                .set("display", "flex")
                .set("padding", "0.5em")
                .set("cursor", "pointer");
        return new Tab(link);
    }

    public void switchToSignedInMode(String userName) {
        this.userName = userName;

        refreshHeader(); // Call refreshHeader to update the UI
        checkRolesAndUpdateUI(); // Also check roles
    }

    public void refreshUI() {
        boolean loggedIn = SecurityContextHolder.isLoggedIn();
        System.out.println("Refreshing UI - Is logged in: " + loggedIn);
        if (loggedIn) {
            String email = SecurityContextHolder.email();
            // Force refresh all components
            UI.getCurrent().getPage().executeJs("location.reload();");
        }
    }

    public void refreshHeader() {
        boolean loggedIn = SecurityContextHolder.isLoggedIn();

        System.out.println("Security check - Is logged in: " + loggedIn);
        if (loggedIn) {
            String email = SecurityContextHolder.email();

            // Try to get username if available
            String displayName = email;
            try {
                if (userName != null && !userName.isEmpty()) {
                    displayName = userName + " (" + email + ")";
                }
            } catch (Exception e) {
                System.out.println("Error fetching username: " + e.getMessage());
            }

            System.out.println("User email: " + email);
            System.out.println("Token present: " + (SecurityContextHolder.token() != null));

            greeting.setText("Hello " + displayName);
        } else {
            greeting.setText("Hello Guest");
        }

        logout.setVisible(loggedIn);
        signInBtn.setVisible(!loggedIn);
        signUpBtn.setVisible(!loggedIn);
        notificationBtn.setVisible(loggedIn); // Only show notification button for logged-in users
    }

    public void switchToSignedInMode() {
        // Show notifications
        Notification notification = Notification.show("Welcome " + this.userName, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        // Hide sign up and sign in buttons
        if (this.userName == null && this.userEmail != null) {
            // Fall back to email if username is null
            this.userName = this.userEmail;
        } else if (this.userName == null && this.userEmail == null) {
            return;
        }

        signInBtn.setVisible(false);
        signUpBtn.setVisible(false);

        // Change hello guest to hello user
        greeting.setText("Hello " + this.userName);
        greeting.getStyle()
                .set("color", "#1976d2")
                .set("margin-left", "1em");

        // Make notification button visible
        notificationBtn.setVisible(true);

        // Check roles and update permission buttons
        checkRolesAndUpdateUI();
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserEmail() {
        return userEmail;
    }

    // Add this method to check roles and update UI accordingly
    /**
     * Enable/disable all management buttons + the Base Catalog tab
     * based on whether the current user is an admin or store-owner.
     */
    public void checkRolesAndUpdateUI() {
        boolean loggedIn = SecurityContextHolder.isLoggedIn();

        if (loggedIn) {
            tabs.getTabAt(6).setVisible(true);

            try {
                String email = SecurityContextHolder.email();

                // 1) Admin?
                boolean isAdmin = SecurityContextHolder.isAdmin();

                // 2) Global STORE_OWNER role? (you may not be using this)
                boolean isGlobalOwner = false;
                try {
                    isGlobalOwner = ServiceLocator
                            .getUserService()
                            .rolesOf(email)
                            .contains(Role.STORE_OWNER);
                } catch (Exception e) {
                    System.err.println("Error checking global roles: " + e.getMessage());
                }

                // 3) Owns at least one store?
                boolean ownsStore = false;
                try {
                    ownsStore = ! ServiceLocator
                            .getStoreService()
                            .listStoresOwnedBy(email)
                            .isEmpty();
                } catch (Exception e) {
                    System.err.println("Error fetching owned stores: " + e.getMessage());
                }

                // 4) Permission buttons
                boolean canManage = isAdmin || isGlobalOwner || ownsStore;
                manageStoreBtn.setVisible(canManage);
                addProductBtn .setVisible(canManage);
                ownersBtn     .setVisible(canManage);
                rolesBtn      .setVisible(canManage);

                // 5) Base Catalog tab only for admins or store-owners (global or per-store)
                baseCatalogTab.setVisible(isAdmin || isGlobalOwner || ownsStore);
                //todo: when isadmin will work properly we shell change to: tabs.getTabAt(7).setVisible(isadmin);
                tabs.getTabAt(7).setVisible(isAdmin);
                // Notifications always once logged in
                notificationBtn.setVisible(true);

            } catch (Exception e) {
                System.err.println("Error in checkRolesAndUpdateUI: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // hide everything when logged out
            tabs.getTabAt(6).setVisible(false);
            tabs.getTabAt(7).setVisible(false);
            manageStoreBtn  .setVisible(false);
            addProductBtn   .setVisible(false);
            ownersBtn       .setVisible(false);
            rolesBtn        .setVisible(false);
            notificationBtn .setVisible(false);
            baseCatalogTab  .setVisible(false);
        }
    }

    // Method to handle a new notification
    public void handleNewNotification(String message) {
        try {
            // Increase notification count
            increaseNotificationCount();

            // Show a toast notification
            Notification toast = Notification.show(
                    message,
                    3000,
                    com.vaadin.flow.component.notification.Notification.Position.TOP_END
            );
            toast.addThemeVariants(NotificationVariant.LUMO_PRIMARY);

            System.out.println("Notification displayed: " + message);
        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
        }
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}