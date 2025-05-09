package com.SEGroup.UnitTests.Discount.Conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class AndConditionTest {

    // have discount of 5% on bakery products if there are at least 5 rolls and 2 breads in the store
    @Test
    public void testAndCondition_BreadAndRollsCondition() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> bakery = new ArrayList<>();
        bakery.add("bakery");

        catalog.addCatalogProduct("c1", "Roll", "YuvalBread", "Soft rolls", bakery);
        catalog.addStoreProductEntry("c1", "YuvalStore", "roll1", 3.0, 5, 4.5, "Roll");

        catalog.addCatalogProduct("c2", "Bread", "YuvalBread", "Whole wheat", bakery);
        catalog.addStoreProductEntry("c2", "YuvalStore", "bread1", 6.0, 2, 4.0, "Bread");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> has5Rolls = arr -> {
            int totalRolls = 0;
            for (StoreSearchEntry e : arr) {
                if (e.getName().toLowerCase().contains("roll")) {
                    totalRolls += e.getQuantity();
                }
            }
            return totalRolls >= 5;
        };

        Predicate<StoreSearchEntry[]> has2Breads = arr -> {
            int totalBreads = 0;
            for (StoreSearchEntry e : arr) {
                if (e.getName().toLowerCase().contains("bread")) {
                    totalBreads += e.getQuantity();
                }
            }
            return totalBreads >= 2;
        };

        Predicate<StoreSearchEntry[]> andCondition = new AndCondition(List.of(has5Rolls, has2Breads));

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "bakery");
        Discount bakeryDiscount = new SimpleDiscount(5, scope);

        Discount conditional = new ConditionalDiscount(andCondition, bakeryDiscount);

        double result = conditional.calculate(entries, catalog);

        assertEquals(1.35, result, 0.001); // 5% of (15+12) is 1.35
    }

}
