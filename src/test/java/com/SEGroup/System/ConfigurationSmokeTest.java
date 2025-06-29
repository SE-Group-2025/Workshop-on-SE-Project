// src/test/java/com/SEGroup/system/ConfigurationSmokeTest.java
package com.SEGroup.System;

import com.SEGroup.Infrastructure.Repositories.StoreRepository;
import com.SEGroup.Infrastructure.Repositories.TransactionRepository;
import com.SEGroup.Infrastructure.Repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("db")
class ConfigurationSmokeTest {

    @Autowired DataSource           dataSource;
    @Autowired UserRepository       users;
    @Autowired
    StoreRepository stores;
    @Autowired
    TransactionRepository transactions;

    @Test
    void springContextLoadsAndDbIsReachable() throws Exception {
        assertThat(dataSource.getConnection()).isNotNull();
        assertThat(users).isNotNull();
        assertThat(stores).isNotNull();
        assertThat(transactions).isNotNull();
    }
}