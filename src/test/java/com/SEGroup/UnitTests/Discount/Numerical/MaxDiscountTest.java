package com.SEGroup.UnitTests.Discount.Numerical;

import com.SEGroup.Domain.Discount.DiscountType;
import com.SEGroup.Domain.Discount.Numerical.MaxDiscount;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Domain.Store.ShoppingProduct;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaxDiscountTest {

    @Test
    public void shouldCalculateMaxDiscountForSingleProduct_WithMultipleDiscounts() {
        List<String> pastaCats = List.of("pasta");

        ShoppingProduct pasta = new ShoppingProduct(
                "StoreA", "cat1", "p1", "Pasta", "desc", 20.0, 3, "", pastaCats
        ); // total 60

        SimpleDiscount discount5Percent = new SimpleDiscount(DiscountType.CATEGORY, 5, "pasta", null);
        SimpleDiscount discount10Percent = new SimpleDiscount(DiscountType.STORE, 10, null, null);

        MaxDiscount maxDiscount = new MaxDiscount(List.of(discount5Percent, discount10Percent));

        double discountAmount = maxDiscount.calculate(pasta, 3);

        // 5% of 60 = 3.0, 10% of 60 = 6.0, max is 6.0
        assertEquals(54.0, discountAmount, 0.001);
    }

    @Test
    public void shouldReturnZeroDiscount_WhenNoDiscountsApply() {
        List<String> categories = List.of("electronics");

        ShoppingProduct product = new ShoppingProduct(
                "StoreA", "cat1", "p1", "Gadget", "desc", 50.0, 2, "", categories
        );

        // Empty MaxDiscount (no discounts)
        MaxDiscount maxDiscount = new MaxDiscount(List.of());

        double discountAmount = maxDiscount.calculate(product, 2);

        assertEquals(100, discountAmount, 0.001);
    }

    @Test
    public void shouldCalculateMaxDiscountForBasket_CorrectlyIdentifyMaxDiscount() {
        List<String> pastaCats = List.of("pasta");
        List<String> dairyCats = List.of("dairy");

        ShoppingProduct pasta = new ShoppingProduct(
                "StoreA", "cat1", "p1", "Pasta", "desc", 20.0, 0, "", pastaCats
        ); // price 20 per unit

        ShoppingProduct milk = new ShoppingProduct(
                "StoreA", "cat2", "p2", "Milk", "desc", 10.0, 0, "", dairyCats
        ); // price 10 per unit

        SimpleDiscount pastaDiscount = new SimpleDiscount(DiscountType.CATEGORY, 5, "pasta", null);
        SimpleDiscount milkDiscount = new SimpleDiscount(DiscountType.CATEGORY, 17, "dairy", null);

        MaxDiscount maxDiscount = new MaxDiscount(List.of(pastaDiscount, milkDiscount));

        // Map products to quantities in the basket (note: quantities can differ from product.getQuantity())
        Map<ShoppingProduct, Integer> basket = new HashMap<>();
        basket.put(pasta, 3);  // total 60
        basket.put(milk, 4);   // total 40

        double maxDiscountAmount = maxDiscount.calculateDiscountForBasket(basket);

        // Expect max discount 6.8 from milk discount (17% of 40)
        assertEquals(43.0, maxDiscountAmount, 0.001);
    }


    @Test
    public void shouldCalculateMaxDiscountWithCoupon_OnlyActiveDiscountApplies() {
        List<String> pastaCats = List.of("pasta");

        ShoppingProduct pasta = new ShoppingProduct(
                "StoreA", "cat1", "p1", "Pasta", "desc", 20.0, 3, "", pastaCats
        );

        SimpleDiscount discount5PercentInactive = new SimpleDiscount(DiscountType.CATEGORY, 5, "pasta", "COUPON1");
        SimpleDiscount discount10PercentActive = new SimpleDiscount(DiscountType.STORE, 10, null, null);

        MaxDiscount maxDiscount = new MaxDiscount(List.of(discount5PercentInactive, discount10PercentActive));

        // Apply coupon only to the inactive discount
        maxDiscount.applyCoupon("COUPON1");

        double discountAmount = maxDiscount.calculate(pasta, 3);

        // The 5% discount is activated by coupon, but 10% store discount is active by default
        // max between 5% of 60=3 and 10% of 60=6 is 6
        assertEquals(54.0, discountAmount, 0.001);
    }

    @Test
    public void shouldReturnZeroDiscount_WhenBasketIsEmpty() {
        MaxDiscount maxDiscount = new MaxDiscount(List.of());
        double discountAmount = maxDiscount.calculateDiscountForBasket(new HashMap<>());
        assertEquals(0.0, discountAmount, 0.001);
    }

}
