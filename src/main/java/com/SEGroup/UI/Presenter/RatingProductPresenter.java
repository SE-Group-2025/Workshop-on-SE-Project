package com.SEGroup.UI.Presenter;

import com.SEGroup.DTO.ShoppingProductDTO;
import com.SEGroup.Service.Result;
import com.SEGroup.Service.StoreService;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.ProductInStoreView;

import java.util.List;

public class RatingProductPresenter {

    private ProductInStoreView view;
    private final StoreService storeService = ServiceLocator.getStoreService();

    public RatingProductPresenter(ProductInStoreView view,
                                  String storeName,
                                  String productId) {
        this.view = view;
        Result<List<ShoppingProductDTO>> res =
                storeService.searchProducts(
                        /* query */     "",
                        /* filters */   List.of(),
                        /* storeName */ storeName,
                        /* categories */List.of()
                );

        if (res.isSuccess()) {
            // find the one with matching ID
            for (ShoppingProductDTO product : res.getData()) {
                if (product.getProductId().equals(productId)) {
                    storeService.rateProduct("", product.getStoreName(), productId, this.view.ratingView.getScore(), "");

                }
            }
        }
    }
}