package com.SEGroup.UI.Presenter;

import com.SEGroup.DTO.ShoppingProductDTO;
import com.SEGroup.DTO.StoreDTO;
import com.SEGroup.Domain.ProductCatalog.CatalogProduct;
import com.SEGroup.Service.Result;
import com.SEGroup.Service.StoreService;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.CatalogView;
import com.SEGroup.UI.Views.StoreView;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.textfield.TextField;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchProductPresenter {

    private final CatalogView catalogView;
    private final StoreService storeService;

    public SearchProductPresenter(CatalogView catalogView,
                                  TextField searchField,
                                  Button searchBtn) {
        this.catalogView  = catalogView;
        this.storeService = ServiceLocator.getStoreService();
        bind(searchField, searchBtn);
    }

    private void bind(TextField searchField, Button searchBtn) {
        // click on the magnifier:
        searchBtn.addClickListener(e ->
                doSearch(searchField.getValue())
        );
        // press Enter:
        searchField.addKeyDownListener(Key.ENTER, ev ->
                doSearch(searchField.getValue())
        );
    }

    private void doSearch(String query) {
        // empty query? show all:
        if (query == null || query.isBlank()) {
            catalogView.showProducts(catalogView.getAllProducts());
            return;
        }
        // for fake products
//        var lower = query.toLowerCase();
//        List<CatalogView.Product> filtered =
//                catalogView.getAllProducts().stream()
//                        .filter(p -> p.name().toLowerCase().contains(lower))
//                        .collect(Collectors.toList());
//
//        catalogView.showProducts(filtered);
        List<CatalogView.Product> products = new LinkedList<>();
        for(CatalogProduct cp : storeService.viewPublicProductCatalog().getData()){
            products.add(new CatalogView.Product(cp.getCatalogID(),cp.getName(), 10.0,"",cp.getBrand()));
        }
        catalogView.showProducts(products);
//        for(StoreDTO storeDTO: ServiceLocator.getStoreService().viewAllStores().getData()) {
//            // for real products
//            Result<List<ShoppingProductDTO>> result =
//                    storeService.searchProducts(
//                            query,
//                            List.of(),     // no extra filters for now
//                            storeDTO.getName(),          // all stores
//                            List.of()      // no category filters
//                    );
//
//            if (result.isSuccess()) {
//                List<CatalogView.Product> products =
//                        result.getData()
//                                .stream()
//                                .map(dto -> new CatalogView.Product(
//                                        dto.getProductId(),
//                                        dto.getName(),
//                                        dto.getPrice(),
//                                        null,
//                                        dto.getCategory()
//                                ))
//                                .collect(Collectors.toList());
//
//                catalogView.showProducts(products);
//            } else {
//                Notification.show(
//                        "Search failed: " + result.getErrorMessage(),
//                        2500,
//                        Position.MIDDLE
//                );
//            }
//        }
    }
}
