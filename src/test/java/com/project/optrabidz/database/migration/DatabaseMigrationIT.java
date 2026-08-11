package com.project.optrabidz.database.migration;

import com.project.optrabidz.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationIT extends PostgresIntegrationTestSupport {

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesFlywayBeforeHibernateValidation() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.generate-ddl", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class))
                .isFalse();

        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("spring.flyway.validate-on-migrate", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("spring.flyway.clean-disabled", Boolean.class))
                .isTrue();

        assertThat(environment.getProperty("spring.sql.init.mode")).isNull();
        assertThat(environment.getProperty("spring.sql.init.schema-locations")).isNull();

        Integer successfulVersionOneMigrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = true",
                Integer.class
        );
        String accountTable = jdbcTemplate.queryForObject(
                "select to_regclass('public.account')",
                String.class
        );

        assertThat(successfulVersionOneMigrations).isEqualTo(1);
        assertThat(accountTable).isEqualTo("account");
    }
}
