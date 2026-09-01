package com.project.optrabidz.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

final class SharedPostgresContainer {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("optrabidz_test")
            .withUsername("optrabidz")
            .withPassword("optrabidz");

    static {
        POSTGRES.start();
    }

    private SharedPostgresContainer() {
    }

    static PostgreSQLContainer<?> getInstance() {
        return POSTGRES;
    }

    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
    }
}
