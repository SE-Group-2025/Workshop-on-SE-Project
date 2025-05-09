// ui/MainLayout.java
package com.SEGroup.UI;
import com.SEGroup.Domain.IPaymentGateway;
import com.SEGroup.Infrastructure.MockPaymentGateway;
import com.SEGroup.Infrastructure.Repositories.*;
import com.SEGroup.UI.Views.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;


public class MainLayout extends AppLayout {

    private Button signInBtn;
    private Button signUpBtn;
    private  Span   greeting;
    private String userName;
    static MainLayout instance;
    public static Button searchBtn;
    public static TextField search;
    public MainLayout() {
        ServiceLocator.initialize(
                new GuestRepository(),
                new UserRepository(),
                new TransactionRepository(),
                new StoreRepository(),
                new InMemoryProductCatalog(),
                new MockPaymentGateway() {
                }
        );

        // Add logo image
        Image logoImage = new Image("/images/icon_eCommerce.png", "Logo");
        logoImage.setHeight("60px");
        logoImage.getStyle().set("max-height", "100%");

// Title and logo layout
        HorizontalLayout titleWithIcon = new HorizontalLayout(logoImage);
        titleWithIcon.setAlignItems(FlexComponent.Alignment.CENTER);

//        H2 title = new H2("eCommerce System");
//        title.getStyle()
//                .set("margin", "0")
//                .set("font-size", "1.8em")
//                .set("text-shadow", "1px 1px 2px rgba(0,0,0,0.2)");

        search = new TextField();
        search.setPlaceholder("Search");
        search.setWidth("200px");
        search.getStyle()
                .set("border-radius", "20px")
                .set("padding", "0.3em 1em")
                .set("background-color", "#fff");
        searchBtn = new Button(VaadinIcon.SEARCH.create());
        searchBtn.getStyle()
                .set("border-radius", "50%")
                .set("background-color", "#3f3f46")
                .set("color", "#fff")
                .set("min-width", "40px")
                .set("min-height", "40px");
        HorizontalLayout searchBar = new HorizontalLayout(search, searchBtn);
        searchBar.setSpacing(true);
        searchBar.setAlignItems(FlexComponent.Alignment.CENTER);
        greeting =  new Span("hello Guest");
        greeting.getStyle()
                .set("color", "#1976d2")
                .set("margin-left", "1em");

        HorizontalLayout auth = new HorizontalLayout();
        auth.setSpacing(true);
        auth.setAlignItems(FlexComponent.Alignment.CENTER);
        signInBtn = new Button("sign in", e -> UI.getCurrent().navigate("signin"));
        signUpBtn = new Button("sign up", e -> UI.getCurrent().navigate("signup"));
        auth.add(
                signInBtn,
                signUpBtn,
                greeting
        );


        HorizontalLayout navbar = new HorizontalLayout(titleWithIcon, searchBar, createSpacer(), auth);
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


        Tabs tabs = new Tabs(
                nav("Catalog", CatalogView.class, VaadinIcon.LIST),
                nav("My cart", CartView.class, VaadinIcon.CART),
                nav("All Stores", AllStoresView.class, VaadinIcon.STORAGE),
                nav("Notifications", NotificationView.class, VaadinIcon.BELL)
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        addToDrawer(tabs);
        setPrimarySection(Section.DRAWER);
        instance = this;
    }

    public static MainLayout getInstance() {
        return instance;
    }




    private void addToNavbar(H2 title, TextField search, Component spacer, Component component) {
        HorizontalLayout navbar = new HorizontalLayout(title, search, spacer, component);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setWidthFull();
        addToNavbar(navbar);
    }

    private Component createSpacer() {
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");
        return spacer;
    }

    private Component authButtons() {
        HorizontalLayout box = new HorizontalLayout();
        Button signIn  = new Button("sign in",  e -> UI.getCurrent().navigate("signin"));
        Button signUp  = new Button("sign up",  e -> UI.getCurrent().navigate("signup"));
        Button logout  = new Button("logout",   e -> {/* TODO: implement logout */});
        box.add(signUp, signIn /* , logout when user logged */);
        return box;
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

    public void switchToSignedInMode(){
        //show notifications
        Notification.show("Welcome " + this.userName, 3000, Notification.Position.MIDDLE);
//        hide sign up and sign in buttons
        if (this.userName == null) {
            return;
        }
        signInBtn.setVisible(false);
        signUpBtn.setVisible(false);
        //change hello guest to hello user
        greeting.setText("hello " + this.userName);
        greeting.getStyle()
                .set("color", "#1976d2")
                .set("margin-left", "1em");
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
