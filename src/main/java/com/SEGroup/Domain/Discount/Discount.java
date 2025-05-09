package com.SEGroup.Domain.Discount;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

public interface Discount {

    /**
     * * Calculates the discount amount to be applied
     * @param entries           An array of products in the basket (from a specific store).
     * @param catalog           catalog The product catalog
     * @return                  The total discount amount.
     **/
    double calculate(StoreSearchEntry[] entries, InMemoryProductCatalog catalog);

    /**
     * Returns a description of the discount.
     *
     * @return      A string describing the discount logic or conditions.
     */
    String getDescription();

}

