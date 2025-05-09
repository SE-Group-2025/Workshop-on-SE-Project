package com.SEGroup.UnitTests.Discount;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.ConditionalDiscount;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class ConditionalDiscountTest {

    @Test
    public void testConditionMetDiscountApplied() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> vegetables = new ArrayList<>();
        vegetables.add("vegetables");

        catalog.addCatalogProduct("c1", "Tomato", "Shufersal", "Red Tomato", vegetables);
        catalog.addCatalogProduct("c2", "Cucumber", "Shufersal", "Green Cucumber", vegetables);

        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 5, 4.0, "Tomato");     // 50
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 80.0, 2, 4.5, "Cucumber");   // 160

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> condition = arr -> {
            double total = 0;
            for (StoreSearchEntry e : arr)
                total += e.getPrice() * e.getQuantity();
            return total > 200;
        };

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.PRODUCT, "p1");
        Discount inner = new SimpleDiscount(10, scope);
        Discount conditional = new ConditionalDiscount(condition, inner);

        double result = conditional.calculate(entries, catalog);
        assertEquals(5.0, result, 0.001); // 10% of 50 is 5
    }

    @Test
    public void testConditionNotMetDiscountApplied() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> vegetables = new ArrayList<>();
        vegetables.add("vegetables");

        catalog.addCatalogProduct("c1", "Tomato", "Shufersal", "Red Tomato", vegetables);
        catalog.addCatalogProduct("c2", "Cucumber", "Shufersal", "Green Cucumber", vegetables);

        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 1, 4.0, "Tomato");     // 10
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 18.9, 10, 4.5, "Cucumber");   // 189

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> condition = arr -> {
            double total = 0;
            for (StoreSearchEntry e : arr)
                total += e.getPrice() * e.getQuantity();
            return total > 200;
        };

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.PRODUCT, "p1");
        Discount inner = new SimpleDiscount(10, scope);
        Discount conditional = new ConditionalDiscount(condition, inner);

        double result = conditional.calculate(entries, catalog);
        assertEquals(0.0, result, 0.001); // No discount applied since condition is not met
    }


    @Test
    public void testConditionOnDairyProducts() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> dairy = new ArrayList<>();
        dairy.add("dairy");

        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Fresh Milk", dairy);
        catalog.addCatalogProduct("c2", "Cheese", "Tnuva", "Yellow Cheese", dairy);

        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 1, 4.0, "Milk");     // 10
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 20.0, 1, 4.0, "Cheese");   // 20

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null)
                .toArray(new StoreSearchEntry[0]);

        // Condition: At least 2 dairy products
        Predicate<StoreSearchEntry[]> condition = arr -> {
            int count = 0;
            for (StoreSearchEntry e : arr) {
                List<String> categories = catalog.getCategoriesOfProduct(e.getCatalogID());
                if (categories.contains("dairy"))
                    count += 1;
            }
            return count >= 2;
        };

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount inner = new SimpleDiscount(10, scope);
        Discount conditional = new ConditionalDiscount(condition, inner);

        double result = conditional.calculate(entries, catalog);
        assertEquals(3.0, result, 0.001); // 10% of (10 + 20) is 3
    }
}