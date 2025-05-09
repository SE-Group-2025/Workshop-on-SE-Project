package com.SEGroup.Domain.Discount.Numerical;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import java.util.List;

public class SumDiscount extends NumericalComposite {

    public SumDiscount(List<Discount> discounts) {
        super(discounts);
    }

    @Override
    public double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog) {
        double sum = 0.0;

        for (Discount discount : discountList) {
            sum += discount.calculate(entries, catalog);
        }
        return sum;
    }

}
