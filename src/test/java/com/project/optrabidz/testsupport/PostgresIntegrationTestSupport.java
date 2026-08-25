package com.project.optrabidz.testsupport;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresIntegrationTestSupport {
    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        SharedPostgresContainer.registerProperties(registry);
    }
}
