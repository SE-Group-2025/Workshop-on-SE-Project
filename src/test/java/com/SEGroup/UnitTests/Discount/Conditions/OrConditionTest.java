package com.SEGroup.UnitTests.Discount.Conditions;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.ConditionalDiscount;
import com.SEGroup.Domain.Discount.Conditions.OrCondition;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class OrConditionTest {

    @Test
    public void testOrCondition_CottageOrYogurtCondition() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> dairy = List.of("dairy");

        catalog.addCatalogProduct("c1", "Cottage Cheese", "Tnuva", "3% fat", dairy);
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 5.0, 3, 4.0, "Cottage Cheese");

        catalog.addCatalogProduct("c2", "Yogurt", "Tnuva", "Vanilla", dairy);
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 4.0, 1, 4.0, "Yogurt");

        catalog.addCatalogProduct("c3", "Milk", "Tnuva", "1% fat", dairy);
        catalog.addStoreProductEntry("c3", "YuvalStore", "p3", 6.0, 1, 4.0, "Milk");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        Predicate<StoreSearchEntry[]> has3Cottage = arr -> {
            int total = 0;
            for (StoreSearchEntry e : arr) {
                if (e.getName().toLowerCase().contains("cottage")) {
                    total += e.getQuantity();
                }
            }
            return total >= 3;
        };

        Predicate<StoreSearchEntry[]> has2Yogurts = arr -> {
            int total = 0;
            for (StoreSearchEntry e : arr) {
                if (e.getName().toLowerCase().contains("yogurt")) {
                    total += e.getQuantity();
                }
            }
            return total >= 2;
        };

        Predicate<StoreSearchEntry[]> orCondition = new OrCondition(List.of(has3Cottage, has2Yogurts));

        DiscountScope scope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount dairyDiscount = new SimpleDiscount(5, scope);
        Discount conditional = new ConditionalDiscount(orCondition, dairyDiscount);

        double result = conditional.calculate(entries, catalog);
        assertEquals(1.25, result, 0.001); // 5% of (15+4+6) is 1.25
    }


}
