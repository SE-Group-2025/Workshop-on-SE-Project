package com.SEGroup.Domain.Discount.Conditions;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;

import java.util.List;
import java.util.function.Predicate;

public abstract class CompositeCondition implements Predicate<StoreSearchEntry[]> {
    protected final List<Predicate<StoreSearchEntry[]>> conditions;

    public CompositeCondition(List<Predicate<StoreSearchEntry[]>> conditions) {
        this.conditions = conditions;
    }
}
