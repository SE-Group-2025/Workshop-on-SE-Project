package com.SEGroup.UI.Views;

import com.SEGroup.DTO.ShoppingProductDTO;
import com.SEGroup.DTO.StoreDTO;
import com.SEGroup.Domain.User.Role;
import com.SEGroup.UI.MainLayout;
import com.SEGroup.UI.Presenter.StorePresenter;
import com.SEGroup.UI.Presenter.RatingStorePresenter;
import com.SEGroup.UI.SecurityContextHolder;
import com.SEGroup.UI.ServiceLocator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "store", layout = MainLayout.class)
@PageTitle("Store View")
public class StoreView extends VerticalLayout implements HasUrlParameter<String> {
    private final Grid<ShoppingProductDTO> productsGrid = new Grid<>(ShoppingProductDTO.class, false);
    private StorePresenter presenter;
    private String storeName;
    private StoreDTO storeData;
    private final DecimalFormat ratingFormat = new DecimalFormat("0.0");

    // Rating view from the main branch
    public RatingView ratingView;

    // Store header components
    private final H2 storeNameHeader = new H2();
    private final Paragraph storeDescription = new Paragraph();
    private final Span storeRating = new Span();

    private final Button manageStoreBtn    = new Button("Manage Store", VaadinIcon.COGS.create());
    private final Button addProductBtn     = new Button("Add Product", VaadinIcon.PLUS.create());
    public  final Button ownersBtn         = new Button("Manage Owners", VaadinIcon.USERS.create());
    public  final Button rolesBtn          = new Button("Manage Permissions", VaadinIcon.KEY.create());
    public  final Button showReviewsBtn    = new Button("Show Reviews", VaadinIcon.KEY.create());
    private final HorizontalLayout adminButtons =
            new HorizontalLayout(manageStoreBtn, addProductBtn, ownersBtn, rolesBtn);
    // Search and filter components
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<>("Category");
    private final NumberField minPriceFilter = new NumberField("Min");
    private final NumberField maxPriceFilter = new NumberField("Max");
    private final ComboBox<String> ratingFilter = new ComboBox<>("Rating");
    private final Button reviewButton = new Button("Review", VaadinIcon.PLUS.create());
    private final TextField commentField = new TextField();

    private HorizontalLayout header = new HorizontalLayout();
    public String comment;

    private List<ShoppingProductDTO> allStoreProducts = new ArrayList<>();
    private final Div catalogContainer = new Div();

    public StoreView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.storeName = parameter;
        this.presenter = new StorePresenter(this, storeName);

        // Initialize UI
        removeAll();
        initializeUI();

        // Load data
        presenter.loadStoreDetails();
        presenter.loadStoreProducts();
    }

    private void initializeUI() {
        // Store header
        HorizontalLayout storeHeader = createStoreHeader();
        add(storeHeader);

        // Add rating view from the main branch
        add(new Span("Your rating:"));
        ratingView = new RatingView();
        add(ratingView);
        // Add comment review
        add(new Span("Your comment:"));
        add(commentField);
        add(reviewButton);
        reviewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        RatingStorePresenter ratingStorePresenter = new RatingStorePresenter(this, storeName);
        reviewButton.addClickListener(evt ->{comment = commentField.getValue();
            ratingStorePresenter.bind();});




        // Search and filter bar
        HorizontalLayout searchAndFilterBar = createSearchAndFilterBar();
        add(searchAndFilterBar);

        // Products grid with configuration
        configureProductsGrid();
        add(productsGrid);

        // Sort options
        add(createSortBar());

        // Add navigation back to marketplace
        Button backToMarketBtn = new Button("Back to Marketplace", VaadinIcon.ARROW_LEFT.create());
        backToMarketBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backToMarketBtn.addClickListener(e -> UI.getCurrent().navigate("catalog"));
        add(backToMarketBtn);
    }

    private HorizontalLayout createStoreHeader() {
        // Store icon/logo
        Icon storeIcon = VaadinIcon.STORAGE.create();
        storeIcon.setSize("50px");
        storeIcon.getStyle().set("margin-right", "20px");

        // Store info
        VerticalLayout storeInfo = new VerticalLayout();
        storeInfo.setPadding(false);
        storeInfo.setSpacing(false);

        storeNameHeader.getStyle().set("margin", "0");
        storeDescription.getStyle().set("margin-top", "5px");
        storeRating.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)")
                .set("display", "flex")
                .set("align-items", "center");

        Icon starIcon = VaadinIcon.STAR.create();
        starIcon.setSize("16px");
        starIcon.getStyle().set("margin-right", "4px");
        storeRating.add(starIcon);

        // 1) Build a little title‐and‐buttons bar:
        HorizontalLayout titleBar = new HorizontalLayout(storeNameHeader, ownersBtn, rolesBtn, showReviewsBtn);
        titleBar.setAlignItems(FlexComponent.Alignment.CENTER);
        titleBar.setSpacing(true);

        // 2) Put titleBar above description & rating:
        storeInfo.add(
                titleBar,
                storeDescription,
                storeRating
        );

        // 3) Wire up the buttons exactly as before:
        manageStoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        manageStoreBtn.addClickListener(e ->
                UI.getCurrent().navigate("store-management/" + storeName)
        );

        ownersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ownersBtn.addClickListener(e ->
                UI.getCurrent().navigate("store/" + storeName + "/owners")
        );

        rolesBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        rolesBtn.addClickListener(e ->
                UI.getCurrent().navigate("store/" + storeName + "/roles")
        );

        showReviewsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        showReviewsBtn.addClickListener(e ->
                UI.getCurrent().navigate(ReviewView.class, URLEncoder.encode(storeName, StandardCharsets.UTF_8).replace("+", " ")));

        // 4) Leave the adminButtons container for legacy use, but *don't* add it here:
        adminButtons.setVisible(false); // you'll still use its children in displayStore()

        // 5) Finally, only add icon + storeInfo to the outer header
        header.add(storeIcon, storeInfo);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "20px")
                .set("border-radius", "8px");

        return header;
    }

    private HorizontalLayout createSearchAndFilterBar() {
        // Search field
        searchField.setPlaceholder("Search products in this store...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> filterProducts());
        searchField.setWidth("350px");

        // Category filter
        categoryFilter.setPlaceholder("All Categories");
        categoryFilter.addValueChangeListener(e -> filterProducts());
        categoryFilter.setClearButtonVisible(true);

        // Price filters
        HorizontalLayout priceFilterLayout = new HorizontalLayout();
        priceFilterLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        Span priceLabel = new Span("Price: ");
        minPriceFilter.setPlaceholder("Min");
        minPriceFilter.setWidth("80px");
        minPriceFilter.setMin(0);
        minPriceFilter.setStep(1);
        minPriceFilter.addValueChangeListener(e -> filterProducts());

        maxPriceFilter.setPlaceholder("Max");
        maxPriceFilter.setWidth("80px");
        maxPriceFilter.setMin(0);
        maxPriceFilter.setStep(1);
        maxPriceFilter.addValueChangeListener(e -> filterProducts());

        priceFilterLayout.add(priceLabel, minPriceFilter, maxPriceFilter);

        // Rating filter
        ratingFilter.setItems("3+", "4+", "4.5+");
        ratingFilter.setPlaceholder("Any Rating");
        ratingFilter.setClearButtonVisible(true);
        ratingFilter.addValueChangeListener(e -> filterProducts());

        // Reset filters button
        Button resetFiltersBtn = new Button("Clear All Filters", new Icon(VaadinIcon.CLOSE));
        resetFiltersBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        resetFiltersBtn.addClickListener(e -> resetFilters());

        // Layout for all filter components
        HorizontalLayout filterBar = new HorizontalLayout(
                searchField, categoryFilter, priceFilterLayout, ratingFilter, resetFiltersBtn);
        filterBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        filterBar.setWidthFull();
        filterBar.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "10px")
                .set("border-radius", "8px");

        return filterBar;
    }

    private void configureProductsGrid() {
        // Configure grid columns
        productsGrid.addColumn(ShoppingProductDTO::getName)
                .setHeader("Product")
                .setSortable(true)
                .setAutoWidth(true)
                .setFlexGrow(1);

        productsGrid.addColumn(ShoppingProductDTO::getDescription)
                .setHeader("Description")
                .setSortable(true)
                .setAutoWidth(true)
                .setFlexGrow(1);

        productsGrid.addColumn(dto -> {
                    var cats = dto.getCategories();
                    return cats.isEmpty() ? "—" : String.join(", ", cats);
                })
                .setHeader("Categories")
                .setSortable(true)
                .setAutoWidth(true);

        // Format rating to one decimal place
        productsGrid.addColumn(product -> {
                    double rating = product.getAvgRating();
                    return ratingFormat.format(rating);
                })
                .setHeader("Rating")
                .setSortable(true)
                .setAutoWidth(true);

        // Image column using renderer
        productsGrid.addColumn(new ComponentRenderer<>(product -> {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Image image = new Image(product.getImageUrl(), product.getName());
                image.setHeight("60px");
                image.setWidth("60px");
                image.getStyle().set("object-fit", "contain");
                return image;
            } else {
                Div placeholder = new Div();
                placeholder.setText("No image");
                placeholder.getStyle()
                        .set("width", "60px")
                        .set("height", "60px")
                        .set("display", "flex")
                        .set("align-items", "center")
                        .set("justify-content", "center")
                        .set("background-color", "#f5f5f5")
                        .set("color", "#666")
                        .set("font-size", "10px");
                return placeholder;
            }
        })).setHeader("Image").setWidth("80px").setFlexGrow(0);

        // Actions column
        productsGrid.addColumn(new ComponentRenderer<>(this::createProductActions))
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        // Click to view product details
        productsGrid.addItemClickListener(event -> {
            if (event.getColumn() != null && !event.getColumn().getKey().equals("actions")) {
                navigateToProductDetails(event.getItem());
            }
        });

        productsGrid.setSelectionMode(Grid.SelectionMode.NONE);
        productsGrid.setHeight("500px");
    }

    private Component createSortBar() {
        Button sortByNameAsc = new Button("Name (A-Z)");
        sortByNameAsc.addClickListener(e -> sortProductsByName(true));

        Button sortByNameDesc = new Button("Name (Z-A)");
        sortByNameDesc.addClickListener(e -> sortProductsByName(false));

        Button sortByPriceAsc = new Button("Price (Low-High)");
        sortByPriceAsc.addClickListener(e -> sortProductsByPrice(true));

        Button sortByPriceDesc = new Button("Price (High-Low)");
        sortByPriceDesc.addClickListener(e -> sortProductsByPrice(false));

        Button sortByRating = new Button("Top Rated");
        sortByRating.addClickListener(e -> sortProductsByRating());

        HorizontalLayout sortBar = new HorizontalLayout();
        sortBar.add(new H5("Sort by:"), sortByNameAsc, sortByNameDesc,
                sortByPriceAsc, sortByPriceDesc, sortByRating);
        sortBar.setSpacing(true);
        sortBar.setPadding(true);
        sortBar.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        sortBar.setWidthFull();

        return sortBar;
    }

    private Component createProductActions(ShoppingProductDTO product) {
        Button viewButton = new Button(new Icon(VaadinIcon.EYE));
        viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewButton.getElement().setAttribute("title", "View Details");
        viewButton.addClickListener(e -> navigateToProductDetails(product));

        Button addToCartButton = new Button(new Icon(VaadinIcon.CART));
        addToCartButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        addToCartButton.getElement().setAttribute("title", "Add to Cart");
        addToCartButton.addClickListener(e -> presenter.addToCart(product.getProductId()));

        HorizontalLayout actions = new HorizontalLayout(viewButton, addToCartButton);
        actions.setSpacing(true);
        return actions;
    }

    private void showAddProductDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Product");

        // Create form fields
        TextField catalogIdField = new TextField("Catalog ID");
        catalogIdField.setRequiredIndicatorVisible(true);
        catalogIdField.setHelperText("Format: TECH-001, FASH-002, HOME-003");

        TextField nameField = new TextField("Product Name");
        nameField.setRequiredIndicatorVisible(true);

        TextArea descriptionField = new TextArea("Description");

        NumberField priceField = new NumberField("Price");
        priceField.setRequiredIndicatorVisible(true);
        priceField.setMin(0);
        priceField.setStep(0.01);
        priceField.setPrefixComponent(new Span("$"));

        NumberField quantityField = new NumberField("Quantity");
        quantityField.setRequiredIndicatorVisible(true);
        quantityField.setMin(1);
        quantityField.setStep(1);
        quantityField.setValue(1.0);

        TextArea ImageField = new TextArea("ImgUrl");

        // Create layout
        VerticalLayout dialogLayout = new VerticalLayout(
                catalogIdField, nameField, descriptionField, priceField, quantityField, ImageField);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);

        // Footer buttons
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button saveButton = new Button("Add Product", e -> {
            if (catalogIdField.isEmpty() || nameField.isEmpty() ||
                    priceField.isEmpty() || quantityField.isEmpty() || ImageField.isEmpty()) {
                showError("Please fill all required fields");
                return;
            }

            try {
                presenter.addProduct(
                        catalogIdField.getValue(),
                        nameField.getValue(),
                        descriptionField.getValue(),
                        priceField.getValue(),
                        quantityField.getValue().intValue(),
                        ImageField.getValue()
                );
                dialog.close();
            } catch (Exception ex) {
                showError("Error adding product: " + ex.getMessage());
            }
        });

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }

    // Event handlers and utility methods
    private void navigateToStoreManagement() {
        UI.getCurrent().navigate("store-management/" + storeName);
    }

    private void navigateToProductDetails(ShoppingProductDTO product) {
        UI.getCurrent().navigate("product/" + product.getProductId() + "/" + storeName);
    }

    private void resetFilters() {
        searchField.clear();
        categoryFilter.clear();
        minPriceFilter.clear();
        maxPriceFilter.clear();
        ratingFilter.clear();

        // Reset to show all products
        productsGrid.setItems(allStoreProducts);
        showSuccess("All filters cleared");
    }

    private void filterProducts() {
        if (allStoreProducts == null || allStoreProducts.isEmpty()) {
            return;
        }

        List<ShoppingProductDTO> filteredProducts = new ArrayList<>(allStoreProducts);

        // Apply text search
        String searchText = searchField.getValue();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String normalizedSearch = searchText.toLowerCase().trim();
            filteredProducts = filteredProducts.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(normalizedSearch)) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(normalizedSearch)))
                    .collect(Collectors.toList());
        }

        // Apply category filter
        if (categoryFilter.getValue() != null) {
            String category = categoryFilter.getValue();
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getCategories() != null && p.getCategories().contains(category))
                    .collect(Collectors.toList());
        }

        // Apply price filters
        if (minPriceFilter.getValue() != null) {
            double minPrice = minPriceFilter.getValue();
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getPrice() >= minPrice)
                    .collect(Collectors.toList());
        }

        if (maxPriceFilter.getValue() != null) {
            double maxPrice = maxPriceFilter.getValue();
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        // Apply rating filter
        if (ratingFilter.getValue() != null) {
            String ratingValue = ratingFilter.getValue();
            double minRating = Double.parseDouble(ratingValue.replace("+", ""));
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getAvgRating() >= minRating)
                    .collect(Collectors.toList());
        }

        // Update the grid with filtered products
        productsGrid.setItems(filteredProducts);

        // Show message if no products found
        if (filteredProducts.isEmpty() && !allStoreProducts.isEmpty()) {
            showInfo("No products match your filter criteria");
        }
    }

    private void sortProductsByName(boolean ascending) {
        List<ShoppingProductDTO> currentItems = new ArrayList<>();
        productsGrid.getListDataView().getItems().forEach(currentItems::add);

        if (ascending) {
            currentItems.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        } else {
            currentItems.sort((p1, p2) -> p2.getName().compareToIgnoreCase(p1.getName()));
        }

        productsGrid.setItems(currentItems);
    }

    // URL encoder helper from the main branch
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void sortProductsByPrice(boolean ascending) {
        List<ShoppingProductDTO> currentItems = new ArrayList<>();
        productsGrid.getListDataView().getItems().forEach(currentItems::add);

        if (ascending) {
            currentItems.sort((p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice()));
        } else {
            currentItems.sort((p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));
        }

        productsGrid.setItems(currentItems);
    }

    private void sortProductsByRating() {
        List<ShoppingProductDTO> currentItems = new ArrayList<>();
        productsGrid.getListDataView().getItems().forEach(currentItems::add);

        currentItems.sort((p1, p2) -> Double.compare(p2.getAvgRating(), p1.getAvgRating()));

        productsGrid.setItems(currentItems);
    }

    // Methods called by the Presenter
    public void displayStore(StoreDTO store) {
        this.storeData = store;

        // Update store header using getters
        storeNameHeader.setText(store.getName());
        storeDescription.setText(store.getDescription());
        storeRating.setText(ratingFormat.format(store.getAvgRating()) + " / 5.0");

        try {
            // Check if current user is owner to show admin buttons
            boolean isOwner = presenter.isCurrentUserOwner();

            // Check if user has store owner role (even if not owner of this specific store)
            boolean hasStoreOwnerRole = false;
            boolean isAdmin = false;

            if (SecurityContextHolder.isLoggedIn()) {
                isAdmin = SecurityContextHolder.isAdmin();

                // Check if the user has the STORE_OWNER role in their roles
                String email = SecurityContextHolder.email();
                Set<Role> roles = ServiceLocator.getUserService().rolesOf(email);
                hasStoreOwnerRole = roles != null && roles.stream()
                        .anyMatch(role -> role == Role.STORE_OWNER);

                System.out.println("User roles: " + roles);
            }

            System.out.println("Current user is admin: " + isAdmin);
            System.out.println("Current user is store owner by role check: " + hasStoreOwnerRole);
            System.out.println("Current user is owner of this store: " + isOwner);

            // Show buttons if the user is either an admin, has store owner role, or is owner of this store
            // 1) global store-management bits for any admin or STORE_OWNER role:
            boolean canManageStore = hasStoreOwnerRole || isAdmin;
            manageStoreBtn.setVisible(canManageStore);
            addProductBtn .setVisible(canManageStore);

            // 2) per-store owner bits only if they actually own this store:
            ownersBtn.setVisible(isOwner);
            rolesBtn .setVisible(isOwner);

            // 3) show the whole adminButtons bar if *any* child is visible
            adminButtons.setVisible(canManageStore || isOwner);
        } catch (Exception e) {
            System.err.println("Error checking roles: " + e.getMessage());
            e.printStackTrace();
        }

        // Update page title
        UI.getCurrent().getPage().setTitle(store.getName() + " - Store");
    }

    public void displayProducts(List<ShoppingProductDTO> products) {
        this.allStoreProducts = new ArrayList<>(products);
        productsGrid.setItems(products);

        // Extract distinct categories for the filter
        List<String> categories = products.stream()
                .flatMap(product -> product.getCategories() != null ?
                        product.getCategories().stream() :
                        java.util.stream.Stream.empty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Update category filter
        categoryFilter.setItems(
                allStoreProducts.stream()
                        .flatMap(p -> p.getCategories().stream())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );

        if (products.isEmpty()) {
            showInfo("No products available in this store");
        }
    }

    public String getStoreName() {
        return storeName;
    }

    /**
     * Called by the presenter when the current user is a store-owner.
     * This will add a "Manage Owners" button into the header.
     */
    public void showManagingOwnersButton() {
        Button manageOwners = new Button("Manage Owners", VaadinIcon.USERS.create());
        manageOwners.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        manageOwners.addClickListener(e ->
                UI.getCurrent().navigate("store/" + storeName + "/owners")
        );
        header.add(manageOwners);
    }

    /**
     * Called by the presenter when the current user is a store-owner.
     * This will add a "Manage Permissions" button into the header.
     */
    public void showManagingRolesButton() {
        Button managePermissions = new Button("Manage Permissions", VaadinIcon.KEY.create());
        managePermissions.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        managePermissions.addClickListener(e ->
                UI.getCurrent().navigate("store/" + storeName + "/roles")
        );
        header.add(managePermissions);
    }

    public void showReviewButton() {
        Button reviews = new Button("Show Reviews", VaadinIcon.KEY.create());
        reviews.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        reviews.addClickListener(e ->
                UI.getCurrent().navigate("store/"+storeName+"/reviews")
        );
        header.add(reviews);
    }


    // Notification methods
    public void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public void showInfo(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }

    public String getComment(){
        return comment;
    }
}