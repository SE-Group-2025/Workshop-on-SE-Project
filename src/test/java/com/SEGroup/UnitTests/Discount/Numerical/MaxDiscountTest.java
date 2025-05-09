package com.SEGroup.UnitTests.Discount.Numerical;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.Numerical.MaxDiscount;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MaxDiscountTest {

    @Test
    public void testMaxDiscount_BetweenPastaAndMilk() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> pasta = List.of("pasta");
        List<String> dairy = List.of("dairy");

        catalog.addCatalogProduct("c1", "Pasta 1", "Barilla", "500g", pasta);
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 20.0, 3, 4.0, "Pasta 1");

        catalog.addCatalogProduct("c2", "Milk", "Tnuva", "1% fat", dairy);
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 10.0, 4, 4.0, "Milk");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        DiscountScope pastaScope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "pasta");
        Discount pastaDiscount = new SimpleDiscount(5, pastaScope); // 5% of 60 = 3.0

        DiscountScope milkScope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount milkDiscount = new SimpleDiscount(17, milkScope); // 17% of 40 = 6.8

        Discount maxDiscount = new MaxDiscount(List.of(pastaDiscount, milkDiscount));

        double result = maxDiscount.calculate(entries, catalog);

        assertEquals(6.8, result, 0.001); // The maximum discount is 6.8 from the milk discount
    }

}
