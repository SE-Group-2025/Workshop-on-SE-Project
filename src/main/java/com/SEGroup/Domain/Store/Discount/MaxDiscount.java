package com.SEGroup.Domain.Store.Discount;


import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import java.util.List;

/**
 * A discount that returns the maximum value from a set of discounts.
 */
public class MaxDiscount extends NumericalComposite {
    private final List<Discount> discounts;
    private double lastComputedDiscount = 0;          // Stores last calculated discount for reporting
    private String Description = "None";              // Description of the best discount


    public MaxDiscount(List<Discount> discounts) {
        super(discounts);
        this.discounts = discounts;
    }

    @Override
    public double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog) {
        double maxDiscount = 0.0;
        String Desc = "None";

        for (Discount discount : discounts) {
            double discountValue = discount.calculate(entries, catalog);
            if (discountValue > maxDiscount) {
                maxDiscount = discountValue;
                Desc = discount.getDescription();
            }
        }

        lastComputedDiscount = maxDiscount;
        Description = Desc;
        return maxDiscount;
    }

    @Override
    public String getDescription() {
        return "Maximum discount: "+ lastComputedDiscount + " from " + Description;
    }
}
