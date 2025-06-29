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
    SELECT * FROM STORES s
    WHERE s.name IN (
        SELECT so.store_name FROM STORE_OWNERS so WHERE so.email = :email
        UNION
        SELECT sm.store_name FROM STORE_MANAGERS sm WHERE sm.manager_email = :email
    )
    OR s.email = :email
    """, nativeQuery = true)
    List<Store> getStoresOwnedBy(@Param("email") String email);

}