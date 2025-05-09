package com.SEGroup.Domain.Discount.Numerical;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import java.util.List;


public class SequentialDiscount extends NumericalComposite {

    public SequentialDiscount(List<Discount> discounts) {
        super(discounts);
    }

    @Override
    public double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog) {
        double totalBefore = 0;
        for (StoreSearchEntry e : entries) {
            totalBefore += e.getPrice() * e.getQuantity();
        }

        double ratio = 1.0;

        for (Discount d : discountList) {
            double value = d.calculate(entries, catalog);
            double fraction = 1.0 - (value / totalBefore);
            ratio *= fraction;
        }

        double totalAfter = totalBefore * ratio;
        return totalBefore - totalAfter;    }

}
