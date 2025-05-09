package com.SEGroup.UnitTests.Discount;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscountScopeTest {

    @Test
    public void testMatchByProductID() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();
        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Fresh", List.of("dairy"));
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 1, 4.0, "Milk");

        StoreSearchEntry entry = catalog.search("", List.of(), "YuvalStore", null).get(0);
        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.PRODUCT, "p1");

        assertTrue(scope.matches(entry, catalog));
    }

    @Test
    public void testMatchByStoreName() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();
        catalog.addCatalogProduct("c2", "Cheese", "Tnuva", "Yellow", List.of("dairy"));
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 20.0, 1, 4.2, "Cheese");

        StoreSearchEntry entry = catalog.search("", List.of(), "YuvalStore", null).get(0);
        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.STORE, "YuvalStore");

        assertTrue(scope.matches(entry, catalog));
    }

    @Test
    public void testMatchByCategory() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();
        catalog.addCatalogProduct("c3", "Tomato", "Organic", "Fresh tomato", List.of("vegetables"));
        catalog.addStoreProductEntry("c3", "YuvalStore", "p3", 5.0, 2, 4.5, "Tomato");

        StoreSearchEntry entry = catalog.search("", List.of(), "YuvalStore", null).get(0);
        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "vegetables");

        assertTrue(scope.matches(entry, catalog));
    }

    @Test
    public void testMismatchByCategory() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();
        catalog.addCatalogProduct("c4", "Soda", "ColaBrand", "Sparkling drink", List.of("drinks"));
        catalog.addStoreProductEntry("c4", "YuvalStore", "p4", 7.0, 1, 4.1, "Soda");

        StoreSearchEntry entry = catalog.search("", List.of(), "YuvalStore", null).get(0);
        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");

        assertFalse(scope.matches(entry, catalog));
    }
}
