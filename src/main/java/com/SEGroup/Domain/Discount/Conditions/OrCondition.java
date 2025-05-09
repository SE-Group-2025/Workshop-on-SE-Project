package com.SEGroup.Domain.Discount.Conditions;

import com.SEGroup.Domain.ProductCatalog.StoreSearchEntry;

import java.util.List;
import java.util.function.Predicate;

public class OrCondition extends CompositeCondition{

    public OrCondition(List<Predicate<StoreSearchEntry[]>> conditions){
        super(conditions);
    }

    @Override
    public boolean test(StoreSearchEntry[] entries) {
        for (Predicate<StoreSearchEntry[]> condition : conditions) {
            if (condition.test(entries)) {
                return true;
            }
        }
        return false;
    }
}
