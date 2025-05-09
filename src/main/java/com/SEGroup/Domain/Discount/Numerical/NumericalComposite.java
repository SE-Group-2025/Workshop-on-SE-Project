package com.SEGroup.Domain.Discount.Numerical;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import java.util.List;

public abstract class NumericalComposite implements Discount {
    protected final List<Discount> discountList;

    public NumericalComposite(List<Discount> discountList) {
        this.discountList = discountList;
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        for (Discount d : discountList) {
            builder.append("- ").append(d.getDescription()).append("\n");
        }
        return builder.toString();
    }

    public abstract double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog);

}
