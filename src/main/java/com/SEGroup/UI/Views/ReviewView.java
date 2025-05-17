package com.SEGroup.UI.Views;
import com.SEGroup.UI.MainLayout;
import com.SEGroup.Domain.Store.Store;
import com.SEGroup.Infrastructure.Repositories.StoreRepository;
import com.SEGroup.Service.StoreService;
import com.SEGroup.UI.Presenter.ReviewPresenter;
import com.SEGroup.UI.ServiceLocator;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "store/reviews", layout = MainLayout.class)
public class ReviewView extends VerticalLayout implements HasUrlParameter<String> {

    public final Grid<StoreRatingDisplay> grid = new Grid<>(StoreRatingDisplay.class);
    private final ReviewPresenter reviewPresenter = new ReviewPresenter(this);

    public ReviewView() {

        setSpacing(true);
        setPadding(true);

        grid.setColumns("reviewerEmail", "rating", "reviewText");
        add(grid);
    }

    @Override
    public void setParameter(BeforeEvent event, String storeName) {
        add(new H2("Reviews for “" + storeName + "”"));
        reviewPresenter.loadReviews(storeName);
    }


    public static class StoreRatingDisplay {
        private String reviewerEmail;
        private int rating;
        private String reviewText;

        public StoreRatingDisplay(String reviewerEmail, int rating, String reviewText) {
            this.reviewerEmail = reviewerEmail;
            this.rating = rating;
            this.reviewText = reviewText;
        }

        public String getReviewerEmail() { return reviewerEmail; }
        public int getRating() { return rating; }
        public String getReviewText() { return reviewText; }
    }


}