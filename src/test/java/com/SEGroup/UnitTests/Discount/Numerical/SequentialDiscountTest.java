package com.SEGroup.UnitTests.Discount.Numerical;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;
import com.SEGroup.Domain.Discount.Discount;
import com.SEGroup.Domain.Discount.DiscountScope;
import com.SEGroup.Domain.Discount.Numerical.SequentialDiscount;
import com.SEGroup.Domain.Discount.SimpleDiscount;
import com.SEGroup.Infrastructure.Repositories.InMemoryProductCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SequentialDiscountTest {

    @Test
    public void testSequentialDiscount_5AndThen20Percent() {
        InMemoryProductCatalog catalog = new InMemoryProductCatalog();

        List<String> dairy = List.of("dairy");

        catalog.addCatalogProduct("c1", "Milk", "Tnuva", "Fresh", dairy);
        catalog.addStoreProductEntry("c1", "YuvalStore", "p1", 100.0, 1, 4.5, "Milk");

        StoreSearchEntry[] entries = catalog.search("", new ArrayList<>(), "YuvalStore", null).toArray(new StoreSearchEntry[0]);

        DiscountScope dairyScope = new DiscountScope(DiscountScope.ScopeType.CATEGORY, "dairy");
        Discount dairyDiscount = new SimpleDiscount(5, dairyScope);

        DiscountScope storeScope = new DiscountScope(DiscountScope.ScopeType.STORE, "YuvalStore");
        Discount storeDiscount = new SimpleDiscount(20, storeScope);

        Discount combinedDiscount = new SequentialDiscount(List.of(dairyDiscount, storeDiscount));

        double result = combinedDiscount.calculate(entries, catalog);

        // expected discount calculation:
        // final price = 0.8 * 0.95 * 100 = 76
        // total discount = 100 - 76 = 24

        assertEquals(24.0, result, 0.001);
    }

}
