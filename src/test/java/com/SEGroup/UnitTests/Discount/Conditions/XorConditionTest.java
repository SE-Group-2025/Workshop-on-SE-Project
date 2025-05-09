package com.SEGroup.UnitTests.Discount.Conditions;
import com.SEGroup.Domain.Discount.ConditionalDiscount;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Conditions.XorCondition;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class XorConditionTest {

    @Test
    public void testXorConditionByCategoryWithoutTieBreaker() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> dairy = new ArrayList<>();
        dairy.add("dairy");
        List<String> bakery = new ArrayList<>();
        bakery.add("bakery");

        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Fresh Milk", dairy);
        catalog.addCatalogProduct("c2", "Bread", "YuvalBread", "Sliced bread", bakery);

        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 1, 4.0, "Milk");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> hasDairy = arr -> {
            for (StoreSearchEntry e : arr) {
                List<String> categories = catalog.getCategoriesOfProduct(e.getCatalogID());
                if (categories.contains("dairy")) return true;
            }
            return false;
        };

        Predicate<StoreSearchEntry[]> hasBakery = arr -> {
            for (StoreSearchEntry e : arr) {
                List<String> categories = catalog.getCategoriesOfProduct(e.getCatalogID());
                if (categories.contains("bakery")) return true;
            }
            return false;
        };

        Predicate<StoreSearchEntry[]> xorCondition = new XorCondition(List.of(hasDairy, hasBakery), hasDairy);

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount dairyDiscount = new SimpleDiscount(10, scope);
        Discount conditional = new ConditionalDiscount(xorCondition, dairyDiscount);

        double result = conditional.calculate(entries, catalog);
        assertEquals(1.0, result, 0.001); // 10% of 10 is 1
    }

    @Test
    public void testXorConditionByCategoryWithCustomTieBreaker() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> dairy = new ArrayList<>();
        dairy.add("dairy");

        List<String> bakery = new ArrayList<>();
        bakery.add("bakery");

        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Fresh Milk", dairy);
        catalog.addCatalogProduct("c2", "Bread", "YuvalBread", "Sliced bread", bakery);

        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 1, 4.0, "Milk");   // dairy
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 20.0, 1, 4.0, "Bread");  // bakery

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> hasDairy = arr -> {
            for (StoreSearchEntry e : arr) {
                List<String> categories = catalog.getCategoriesOfProduct(e.getCatalogID());
                if (categories.contains("dairy")) return true;
            }
            return false;
        };

        Predicate<StoreSearchEntry[]> hasBakery = arr -> {
            for (StoreSearchEntry e : arr) {
                List<String> categories = catalog.getCategoriesOfProduct(e.getCatalogID());
                if (categories.contains("bakery")) return true;
            }
            return false;
        };

        Predicate<StoreSearchEntry[]> preferAlphabeticallyFirst = arr -> {
            boolean dairyExists = false;
            boolean bakeryExists = false;

            for (StoreSearchEntry e : arr) {
                List<String> categories = catalog.getCategoriesOfProduct(e.getCatalogID());
                if (categories.contains("dairy")) dairyExists = true;
                if (categories.contains("bakery")) bakeryExists = true;
            }

            if (dairyExists && bakeryExists) {
                return "bakery".compareTo("dairy") < 0;  // bakery < dairy --> true
            }

            return dairyExists;
        };

        Predicate<StoreSearchEntry[]> xorCondition = new XorCondition(List.of(hasDairy, hasBakery), preferAlphabeticallyFirst);

        Discount dairyDiscount = new SimpleDiscount(10, new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy"));
        Discount bakeryDiscount = new SimpleDiscount(10, new DiscountScope(DiscountScope.ScopeType.CATEGORY, "bakery"));

        Discount selectedDiscount = bakeryDiscount;
        Discount conditional = new ConditionalDiscount(xorCondition, selectedDiscount);

        double result = conditional.calculate(entries, catalog);

        assertEquals(2.0, result, 0.001); // 10% of 20 is 2
    }


}
