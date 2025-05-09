package com.SEGroup.UI.Presenter;

import com.SEGroup.Service.StoreService;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.StoreView;

public class RatingStorePresenter {
    private StoreView storeView;
    private final StoreService storeService = ServiceLocator.getStoreService();
    private final String       storeName;
//    private final Consumer<Integer> onSuccess;
//    private final Consumer<String>  onError;

//    public RatingStorePresenter(StoreView storeView,
//                                String storeName,
//                                Consumer<Integer> onSuccess,
//                                Consumer<String> onError) {
//        this.storeName = storeName;
//        this.onSuccess = onSuccess;
//        this.onError   = onError;
//        bind(storeView);

    public RatingStorePresenter(StoreView storeView,
                                String storeName) {
        this.storeName = storeName;
        bind(storeView);
    }

    private void bind(StoreView storeView) {
        storeView.ratingView.addClickListener(evt -> {
            int score = storeView.ratingView.getScore();
            storeService.rateStore("",storeName, score,"Great");
//            Result<Void> result = storeService.rateStore("",storeName, score,"Great");
//            if (result.isSuccess()) {
//                onSuccess.accept(score);
//            } else {
//                onError.accept(result.getErrorMessage());
//            }
        });
    }
}
