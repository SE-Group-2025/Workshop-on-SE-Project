package com.SEGroup.Domain.Discount;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import java.util.function.Predicate;

public class ConditionalDiscount implements Discount{
    private final Predicate<StoreSearchEntry[]> condition;  // The condition for activating the action
    private final Discount discount;                        // The action to be performed if the condition is met
    private boolean lastConditionResult = false;
    private double lastDiscountValue;

    public ConditionalDiscount(Predicate<StoreSearchEntry[]> condition, Discount discount) {
        this.condition = condition;
        this.discount = discount;
    }

    @Override
    public double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog){
        if (condition.test(entries)) {
            lastConditionResult = true;
            lastDiscountValue = discount.calculate(entries, catalog);
        }
        else{
            lastConditionResult = false;
            lastDiscountValue = 0;
        }

        return lastDiscountValue;
    }

    @Override
    public String getDescription() {
        if (lastConditionResult) {
            return "Conditional discount applied: " + lastDiscountValue;
        } else {
            return "No discount applied (condition not met)";
        }
    }
}
