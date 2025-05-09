package com.SEGroup.Domain.Discount.Numerical;


import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import java.util.List;

/**
 * A discount that returns the maximum value from a set of discounts.
 */
public class MaxDiscount extends NumericalComposite {
    private double lastComputedDiscount = 0;          // Stores last calculated discount for reporting
    private String Description = "None";              // Description of the best discount


    public MaxDiscount(List<Discount> discounts) {
        super(discounts);
    }

    @Override
    public double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog) {
        double maxDiscount = 0.0;
        String Desc = "None";

        for (Discount discount : discountList) {
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
