package com.project.optrabidz.testsupport;

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
}
