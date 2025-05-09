package com.SEGroup.UnitTests.Discount.Numerical;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.Numerical.SumDiscount;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SumDiscountTest {

    @Test
    public void testSumDiscount_SeparateScopes() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> dairy = List.of("dairy");
        List<String> vegetables = List.of("vegetables");

        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Fresh", dairy);
        catalog.addCatalogProduct("c2", "Cheese", "Tnuva", "Yellow", dairy);
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 10.0, 2, 4.0, "Milk");   // 20
        catalog.addStoreProductEntry("c2", "YuvalStore", "p2", 10.0, 2, 4.0, "Cheese"); // 20

        catalog.addCatalogProduct("c3", "Tomatoes", "OrganicFarm", "Red", vegetables);
        catalog.addStoreProductEntry("c3", "YuvalStore", "p3", 20.0, 3, 4.5, "Tomatoes"); // 60

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        DiscountScope dairyScope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount dairyDiscount = new SimpleDiscount(10, dairyScope);

        DiscountScope vegScope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "vegetables");
        Discount vegDiscount = new SimpleDiscount(20, vegScope);

        Discount totalDiscount = new SumDiscount(List.of(dairyDiscount, vegDiscount));

        double result = totalDiscount.calculate(entries, catalog);

        assertEquals(16.0, result, 0.001); // 10% of 40 + 20% of 60 = 4 + 12 = 16
    }
}
