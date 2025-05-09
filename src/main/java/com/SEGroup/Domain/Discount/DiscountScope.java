package com.SEGroup.Domain.Discount;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

/**
 * Represents the scope of a discount – whether it's applied to a specific product, category, or store.
 */
public class DiscountScope {
    public enum ScopeType {
        PRODUCT,
        CATEGORY,
        STORE
    }

    private final ScopeType type;
    private final String value;

    public DiscountScope(ScopeType type, String value) {
        this.type = type;
        this.value = value;
    }

    public ScopeType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    /**
     * Checks whether the entry matches the scope using product ID, store, or category.
     *
     * @param entry   The product entry in the basket.
     * @param catalog The catalog to use when checking for categories.
     * @return true if the entry matches the scope condition.
     */
    public boolean matches(StoreSearchEntry entry, InMemoryProductCatalog catalog) {
        if (type == ScopeType.PRODUCT) {
            return entry.getProductID().equals(value);
        } else if (type == ScopeType.STORE) {
            return entry.getStoreName().equals(value);
        } else if (type == ScopeType.CATEGORY) {
            String catalogID = entry.getCatalogID();
            return catalog.getCategoriesOfProduct(catalogID).contains(value.toLowerCase());
        }
        return false;
    }

}
