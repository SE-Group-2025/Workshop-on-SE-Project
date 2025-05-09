package com.SEGroup.UnitTests.Discount.Conditions;
import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.ConditionalDiscount;
import com.SEGroup.Domain.Discount.Conditions.AndCondition;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class CompositeConditionTest {

    @Test
    public void testCompositeCondition_ShoppingOver100AndHas3Pastas() {
        InMemoryProductCatalog catalog;
        catalog = new InMemoryProductCatalog();

        List<String> dairy = List.of("dairy");
        List<String> pasta = List.of("pasta");

        catalog.addCatalogProduct("c1", "Pasta 1", "Barilla", "500g", pasta);
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 20.0, 2, 4.0, "Pasta 1");

        catalog.addCatalogProduct("c2", "Pasta 2", "Barilla", "Spaghetti - 500g", pasta);
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 25.0, 1, 4.0, "Pasta 2");

        catalog.addCatalogProduct("c3", "Milk", "Tnuva", "1% fat", dairy);
        catalog.addStoreProductEntry("c3", "YuvalStore", "p3", 10.0, 10, 4.0, "Milk");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> basketOver100 = arr -> {
            double total = 0;
            for (StoreSearchEntry e : arr) {
                total += e.getPrice() * e.getQuantity();
            }
            return total > 100;
        };

        Predicate<StoreSearchEntry[]> has3Pastas = arr -> {
            int count = 0;
            for (StoreSearchEntry e : arr) {
                if (e.getName().toLowerCase().contains("pasta")) {
                    count += e.getQuantity();
                }
            }
            return count >= 3;
        };

        Predicate<StoreSearchEntry[]> condition = new AndCondition(List.of(basketOver100, has3Pastas));

        DiscountScope dairyScope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount dairyDiscount = new SimpleDiscount(5, dairyScope);

        Discount conditional = new ConditionalDiscount(condition, dairyDiscount);

        double result = conditional.calculate(entries, catalog);

        assertEquals(5.0, result, 0.001);
    }

    @Test
    public void testCompositeCondition_OnlyOneConditionTrue_NoDiscount() {
        InMemoryProductCatalog catalog;
        catalog = new InMemoryProductCatalog();

        List<String> dairy = List.of("dairy");
        List<String> pasta = List.of("pasta");

        catalog.addCatalogProduct("c1", "Pasta 1", "Barilla", "500g", pasta);
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 20.0, 2, 4.0, "Pasta 1");

        catalog.addCatalogProduct("c2", "Pasta 2", "Barilla", "Spaghetti - 500g", pasta);
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 25.0, 1, 4.0, "Pasta 2");

        catalog.addCatalogProduct("c3", "Milk", "Tnuva", "1% fat", dairy);
        catalog.addStoreProductEntry("c3", "YuvalStore", "p3", 10.0, 1, 4.0, "Milk");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> basketOver100 = arr -> {
            double total = 0;
            for (StoreSearchEntry e : arr) {
                total += e.getPrice() * e.getQuantity();
            }
            return total > 100;
        };

        Predicate<StoreSearchEntry[]> has3Pastas = arr -> {
            int count = 0;
            for (StoreSearchEntry e : arr) {
                if (e.getName().toLowerCase().contains("pasta")) {
                    count += e.getQuantity();
                }
            }
            return count >= 3;
        };

        Predicate<StoreSearchEntry[]> condition = new AndCondition(List.of(basketOver100, has3Pastas));

        DiscountScope dairyScope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount dairyDiscount = new SimpleDiscount(5, dairyScope);

        Discount conditional = new ConditionalDiscount(condition, dairyDiscount);

        double result = conditional.calculate(entries, catalog);

        assertEquals(0.0, result, 0.001);
    }


}
