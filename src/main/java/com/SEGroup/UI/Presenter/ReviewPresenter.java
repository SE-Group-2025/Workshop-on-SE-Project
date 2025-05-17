package com.SEGroup.UI.Presenter;

import com.SEGroup.DTO.RatingDto;
import com.SEGroup.Domain.Store.Store;
import com.SEGroup.Infrastructure.Repositories.StoreRepository;
import com.SEGroup.Service.StoreService;
import com.SEGroup.UI.SecurityContextHolder;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.ReviewView;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ReviewPresenter {

    private final ReviewView reviewView;
    private final StoreService storeService = ServiceLocator.getStoreService();

    public ReviewPresenter(ReviewView reviewView) {
        this.reviewView = reviewView;
    }

    public void loadReviews(String storeName) {
            List<RatingDto> ratings = storeService.getStoreRatings(SecurityContextHolder.token(),storeName).getData();

            List<ReviewView.StoreRatingDisplay> displayList = ratings.stream()
                    .map(entry -> new ReviewView.StoreRatingDisplay(entry.getRaterEmail(), entry.getScore(), entry.getReview()))
                    .collect(Collectors.toList());
            this.reviewView.grid.setItems(displayList);
    }
}
