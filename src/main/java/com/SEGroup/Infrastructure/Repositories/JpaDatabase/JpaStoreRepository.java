package com.SEGroup.Infrastructure.Repositories.JpaDatabase;

import com.SEGroup.Domain.Store.Store;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile({"db", "prod"})
public interface JpaStoreRepository extends JpaRepository<Store, String> {
    Store findByName(String name);
    boolean existsByName(String name);
    @Query(value = """
    SELECT s.* FROM stores s
    JOIN store_owners o ON s.name = o.store_name
    WHERE o.email = :ownerEmail
    """, nativeQuery = true)
    List<Store> getStoresOwnedBy(@Param("ownerEmail") String OwnerEmail);

}