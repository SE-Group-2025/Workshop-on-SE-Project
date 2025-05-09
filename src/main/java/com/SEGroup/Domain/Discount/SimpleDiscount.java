package com.SEGroup.Domain.Discount;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

public class SimpleDiscount implements Discount{
    private final double percent;
    private final DiscountScope scope;

    public SimpleDiscount(double percent, DiscountScope scope) {
        this.percent = percent;
        this.scope = scope;
    }

    @Override
    public double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog) {
        double totalDiscount = 0.0;

        for (StoreSearchEntry entry : entries) {
            if (scope.matches(entry, catalog)) {
                double price = entry.getPrice();
                int quantity = entry.getQuantity();
                totalDiscount += quantity * price * (percent / 100);
            }
        }

        return totalDiscount;
    }

    @Override
    public String getDescription() {
        return percent + "% discount on " + scope.getType() + ": " + scope.getValue();
    }
}
