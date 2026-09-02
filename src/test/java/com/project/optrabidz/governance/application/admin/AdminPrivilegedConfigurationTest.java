package com.project.optrabidz.governance.application.admin;

import com.project.optrabidz.governance.api.AdminRecoveryController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AdminPrivilegedConfigurationTest {
    private static final String VALID_PASSWORD = "BootstrapPassword01";
    private static final String VALID_RECOVERY_TOKEN = "recovery-token-material-000000000001";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PrivilegedConfiguration.class)
            .withBean(AdminBootstrapService.class, () -> mock(AdminBootstrapService.class))
            .withBean(AdminAuthorityTransferService.class, () -> mock(AdminAuthorityTransferService.class));

    @Test
    void disabledPrivilegedModesRegisterNoRuntimeComponents() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(AdminBootstrapRunner.class)
                .doesNotHaveBean(AdminRecoveryController.class));
    }

    @Test
    void validBootstrapRegistersOnlyTheBootstrapRunner() {
        contextRunner.withPropertyValues(validBootstrapProperties())
                .run(context -> assertThat(context)
                        .hasSingleBean(AdminBootstrapRunner.class)
                        .doesNotHaveBean(AdminRecoveryController.class));
    }

    @Test
    void validRecoveryRegistersOnlyTheRecoveryController() {
        contextRunner.withPropertyValues(
                        "optrabidz.admin.recovery.enabled=true",
                        "optrabidz.admin.recovery.token=" + VALID_RECOVERY_TOKEN
                )
                .run(context -> assertThat(context)
                        .doesNotHaveBean(AdminBootstrapRunner.class)
                        .hasSingleBean(AdminRecoveryController.class));
    }

    @Test
    void malformedBootstrapEmailFailsWithoutDisclosingSubmittedValue() {
        String malformedEmail = "private-bootstrap-address";

        contextRunner.withPropertyValues(
                        "optrabidz.admin.bootstrap.enabled=true",
                        "optrabidz.admin.bootstrap.email=" + malformedEmail,
                        "optrabidz.admin.bootstrap.password=" + VALID_PASSWORD,
                        "optrabidz.admin.bootstrap.public-display-name=Platform Administrator",
                        "optrabidz.admin.bootstrap.organization-label=OptraBidz Governance"
                )
                .run(context -> assertFailedWithoutSecrets(context.getStartupFailure(), malformedEmail));
    }

    @Test
    void weakBootstrapPasswordFailsWithoutDisclosingSubmittedValue() {
        String weakPassword = "private-bootstrap-password";

        contextRunner.withPropertyValues(
                        "optrabidz.admin.bootstrap.enabled=true",
                        "optrabidz.admin.bootstrap.email=bootstrap-admin@example.test",
                        "optrabidz.admin.bootstrap.password=" + weakPassword,
                        "optrabidz.admin.bootstrap.public-display-name=Platform Administrator",
                        "optrabidz.admin.bootstrap.organization-label=OptraBidz Governance"
                )
                .run(context -> assertFailedWithoutSecrets(context.getStartupFailure(), weakPassword));
    }

    @Test
    void incompleteBootstrapConfigurationFailsBeforeRunnerExecution() {
        contextRunner.withPropertyValues(
                        "optrabidz.admin.bootstrap.enabled=true",
                        "optrabidz.admin.bootstrap.email=bootstrap-admin@example.test",
                        "optrabidz.admin.bootstrap.password=" + VALID_PASSWORD
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void invalidRecoveryTokenLengthFailsWithoutDisclosingTheToken() {
        String shortToken = "private-short-recovery-token";

        contextRunner.withPropertyValues(
                        "optrabidz.admin.recovery.enabled=true",
                        "optrabidz.admin.recovery.token=" + shortToken
                )
                .run(context -> assertFailedWithoutSecrets(context.getStartupFailure(), shortToken));
    }

    @Test
    void missingRecoveryTokenFailsBeforeControllerAvailability() {
        contextRunner.withPropertyValues("optrabidz.admin.recovery.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void oversizedRecoveryTokenFailsWithoutDisclosingTheToken() {
        String oversizedToken = "r".repeat(513);

        contextRunner.withPropertyValues(
                        "optrabidz.admin.recovery.enabled=true",
                        "optrabidz.admin.recovery.token=" + oversizedToken
                )
                .run(context -> assertFailedWithoutSecrets(context.getStartupFailure(), oversizedToken));
    }

    @Test
    void bootstrapAndRecoveryCannotBeEnabledTogether() {
        contextRunner.withPropertyValues(validBootstrapProperties())
                .withPropertyValues(
                        "optrabidz.admin.recovery.enabled=true",
                        "optrabidz.admin.recovery.token=" + VALID_RECOVERY_TOKEN
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable rootCause = rootCause(context.getStartupFailure());
                    assertThat(rootCause)
                            .hasMessageContaining("bootstrap")
                            .hasMessageContaining("recovery");
                    assertThat(rootCause).hasMessageNotContaining(VALID_PASSWORD);
                    assertThat(rootCause).hasMessageNotContaining(VALID_RECOVERY_TOKEN);
                });
    }

    private static String[] validBootstrapProperties() {
        return new String[] {
                "optrabidz.admin.bootstrap.enabled=true",
                "optrabidz.admin.bootstrap.email=bootstrap-admin@example.test",
                "optrabidz.admin.bootstrap.password=" + VALID_PASSWORD,
                "optrabidz.admin.bootstrap.public-display-name=Platform Administrator",
                "optrabidz.admin.bootstrap.organization-label=OptraBidz Governance"
        };
    }

    private static void assertFailedWithoutSecrets(Throwable startupFailure, String... secrets) {
        assertThat(startupFailure).isNotNull();
        Throwable rootCause = rootCause(startupFailure);
        for (String secret : secrets) {
            assertThat(rootCause).hasMessageNotContaining(secret);
        }
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AdminBootstrapProperties.class, AdminRecoveryProperties.class})
    @ComponentScan(
            basePackages = {
                    "com.project.optrabidz.governance.application.admin",
                    "com.project.optrabidz.governance.api"
            },
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = {
                            "com\\.project\\.optrabidz\\.governance\\.application\\.admin\\.AdminPrivilegedConfigurationPolicy",
                            "com\\.project\\.optrabidz\\.governance\\.application\\.admin\\.AdminBootstrapRunner",
                            "com\\.project\\.optrabidz\\.governance\\.api\\.AdminRecoveryController"
                    }
            )
    )
    static class PrivilegedConfiguration {
    }
}
