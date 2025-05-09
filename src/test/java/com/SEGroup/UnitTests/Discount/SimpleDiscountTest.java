package com.SEGroup.UnitTests.Discount;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SimpleDiscountTest {

    @Test
    public void testDiscountByProduct() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> categories = new ArrayList<>();
        categories.add("electronics");

        catalog.addCatalogProduct("c1", "Phone", "Apple", "IPhone 16 pro", categories);
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 1000.0, 1, 4.5, "Phone");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.PRODUCT, "p1");     // By product ID
        Discount discount = new SimpleDiscount(15, scope);                                // 15% discount

        double result = discount.calculate(entries, catalog);
        assertEquals(150.0, result, 0.001);                                         // 15% of 1000 is 150
    }

    // 50% discount on dairy products
    @Test
    public void testDiscountByCategory() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> categories = new ArrayList<>();
        categories.add("dairy");

        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Lactose-free", categories);
        catalog.addCatalogProduct("c2", "Cheese", "Tnuva", "Yellow cheese", categories);

        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 1, 4.5, "Milk");
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 20.0, 2, 4.5, "Cheese");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");     // By category
        Discount discount = new SimpleDiscount(50, scope);                                    // 50% discount

        double result = discount.calculate(entries, catalog);
        assertEquals(25.0, result, 0.001);                                         // 50% of (10 + 20 * 2) is 25
    }


    // %20 discount on the entire store!
    @Test
    public void testDiscountByStore() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> dairyCategory = new ArrayList<>();
        dairyCategory.add("dairy");

        List<String> sweetsCategory = new ArrayList<>();
        sweetsCategory.add("sweets");

        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Lactose-free", dairyCategory);
        catalog.addCatalogProduct("c2", "Chocolate", "Elite", "Dubai Chocolate", sweetsCategory);

        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 3, 4.5, "Milk");
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 50.0, 2, 4.5, "Cheese");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.STORE, "YuvalStore");     // By store
        Discount discount = new SimpleDiscount(20, scope);                                    // 20% discount

        double result = discount.calculate(entries, catalog);
        assertEquals(26.0, result, 0.001);                                         // 20% of (10 * 3 + 50 * 2) is 26
    }




}
