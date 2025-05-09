package com.SEGroup.Domain.Discount.Conditions;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;


import java.util.List;
import java.util.function.Predicate;

public class XorCondition extends CompositeCondition {

    private final Predicate<StoreSearchEntry[]> tieBreaker;

    public XorCondition(List<Predicate<StoreSearchEntry[]>> conditions) {
        this(conditions, null);
    }

    public XorCondition(List<Predicate<StoreSearchEntry[]>> conditions, Predicate<StoreSearchEntry[]> tieBreaker) {
        super(conditions);
        this.tieBreaker = tieBreaker;
    }

    @Override
    public boolean test(StoreSearchEntry[] entries) {
        int trueCount = 0;

        for (Predicate<StoreSearchEntry[]> condition : conditions) {
            if (condition.test(entries)) {
                trueCount++;
            }
        }

        if (trueCount == 1) return true;
        if (trueCount > 1 && tieBreaker != null) {
            return tieBreaker.test(entries);
        }

        return false;
    }

}
