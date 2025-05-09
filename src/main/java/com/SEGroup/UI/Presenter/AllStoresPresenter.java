package com.SEGroup.UI.Presenter;

import com.SEGroup.DTO.StoreDTO;
import com.SEGroup.Service.Result;
import com.SEGroup.Service.StoreService;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.AllStoresView;
import com.sun.source.tree.BreakTree;

import java.util.LinkedList;
import java.util.List;

public class AllStoresPresenter {
    private StoreService storeService;
    public AllStoresPresenter(){
        this.storeService = ServiceLocator.getStoreService();
    }
    public List<AllStoresView.Store> getStores(){
        Result<List<StoreDTO>> result = this.storeService.viewAllStores();
        List<AllStoresView.Store> stores = new LinkedList<>();
        for (StoreDTO storeDTO: result.getData()){
            stores.add(new AllStoresView.Store(storeDTO.getName(),storeDTO.getFounderEmail(),storeDTO.getAvgRating(), "Desc"));
        }
        return stores;
    }
}
