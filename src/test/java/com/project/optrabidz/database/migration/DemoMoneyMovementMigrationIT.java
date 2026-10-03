package com.project.optrabidz.database.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DemoMoneyMovementMigrationIT {
    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("optrabidz_demo_money_movement_migration_test")
                    .withUsername("optrabidz")
                    .withPassword("optrabidz");

    @BeforeEach
    void resetDatabase() throws Exception {
        try (Connection connection = postgres.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute("drop schema public cascade");
            statement.execute("create schema public");
        }
    }

    @Test
    void upgradesPopulatedVersionOneDatabaseToVersionThreeWithoutReset() throws Exception {
        Flyway versionOne = flyway(MigrationVersion.fromVersion("1"));
        versionOne.migrate();

        try (Connection connection = postgres.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    insert into payment_provider(provider_code, display_name, enabled)
                    values ('EXISTING', 'Existing Provider', false)
                    """);
        }

        Flyway current = flyway(null);
        current.migrate();

        assertThat(current.info().current()).isNotNull();
        assertThat(current.info().current().getVersion().getVersion()).isEqualTo("3");

        try (Connection connection = postgres.createConnection("")) {
            assertThat(singleInt(connection, """
                    select count(*)
                    from flyway_schema_history
                    where version in ('1', '2', '3') and success = true
                    """)).isEqualTo(3);
            assertThat(singleInt(connection, """
                    select count(*) from payment_provider where provider_code = 'EXISTING' and enabled = false
                    """)).isEqualTo(1);
            assertThat(singleInt(connection, """
                    select count(*) from payment_provider_method
                    where provider_code = 'DEMO' and currency_code = 'INR'
                      and method_type::text in ('UPI', 'CARD') and enabled = true
                    """)).isEqualTo(2);
        }
    }

    @Test
    void createsPayoutSchemaAndEnforcesCapturedBindingProviderConsistency() throws Exception {
        Flyway current = flyway(null);
        current.migrate();

        try (Connection connection = postgres.createConnection("")) {
            assertThat(singleString(connection, "select to_regclass('public.payment_account_binding')"))
                    .isEqualTo("payment_account_binding");
            assertThat(singleString(connection, "select to_regclass('public.payout_transfer')"))
                    .isEqualTo("payout_transfer");
            assertThat(enumValues(connection, "settlement_state_enum"))
                    .contains("SETTLEMENT_PAYOUT_PENDING");
            assertThat(enumValues(connection, "repayment_installment_status_enum"))
                    .contains("PAYOUT_PENDING");
            assertThat(enumValues(connection, "payment_account_binding_status_enum"))
                    .containsExactly("PENDING_VERIFICATION", "VERIFIED", "DEACTIVATED");
            assertThat(enumValues(connection, "payout_transfer_status_enum"))
                    .containsExactly("PENDING", "PROCESSING", "CONFIRMED", "FAILED");
            assertThat(singleInt(connection, """
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'payout_transfer'
                      and column_name = 'currency_code'
                    """))
                    .isEqualTo(1);
            assertThat(singleInt(connection, """
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'payment_account_binding'
                      and column_name = 'lock_version'
                      and is_nullable = 'NO'
                    """))
                    .isEqualTo(1);

            assertThat(singleString(connection, """
                    select pg_get_constraintdef(oid)
                    from pg_constraint
                    where conname = 'fk_payout_transfer_binding_provider'
                    """))
                    .contains("FOREIGN KEY (payment_account_binding_id, provider_code)")
                    .contains("REFERENCES payment_account_binding(payment_account_binding_id, provider_code)");
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private static int singleInt(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }

    private static String singleString(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private static List<String> enumValues(Connection connection, String typeName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                select enumlabel
                from pg_enum
                join pg_type on pg_type.oid = pg_enum.enumtypid
                where pg_type.typname = ?
                order by enumsortorder
                """)) {
            statement.setString(1, typeName);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
                return values;
            }
        }
    }

}
