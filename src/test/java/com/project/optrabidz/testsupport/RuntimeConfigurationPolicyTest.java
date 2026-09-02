package com.project.optrabidz.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeConfigurationPolicyTest {
    private static final Path REPOSITORY_ROOT = Path.of("").toAbsolutePath().normalize();

    private static final List<String> PRIVILEGED_SWITCHES = List.of(
            "optrabidz.admin.bootstrap.enabled",
            "optrabidz.admin.recovery.enabled",
            "optrabidz.financial.local-provider.enabled",
            "optrabidz.financial.sandbox-providers.enabled",
            "optrabidz.financial.webhook.providers.UPI.enabled",
            "optrabidz.financial.webhook.providers.CARD.enabled"
    );

    private static final List<String> SECRET_KEYS = List.of(
            "OPTRABIDZ_DATASOURCE_PASSWORD",
            "OPTRABIDZ_ADMIN_BOOTSTRAP_PASSWORD",
            "OPTRABIDZ_ADMIN_RECOVERY_TOKEN",
            "OPTRABIDZ_UPI_WEBHOOK_SECRET",
            "OPTRABIDZ_CARD_WEBHOOK_SECRET"
    );

    @Test
    void sharedBaselineKeepsPrivilegedCapabilitiesDisabled() throws IOException {
        Properties baseline = load("src/main/resources/application.properties");

        assertThat(baseline).doesNotContainKeys("spring.profiles.active", "spring.profiles.default");
        assertThat(PRIVILEGED_SWITCHES)
                .allSatisfy(key -> assertThat(baseline.getProperty(key))
                        .as(key)
                        .isIn(null, "false"));
    }

    @Test
    void developmentProfileUsesOptionalLocalEnvironmentAndDisabledOptIns() throws IOException {
        Properties development = load("src/main/resources/application-dev.properties");

        assertThat(development.getProperty("spring.config.import"))
                .isEqualTo("optional:file:.env[.properties]");
        assertThat(development).doesNotContainKeys("spring.profiles.active", "spring.profiles.default");
        assertThat(PRIVILEGED_SWITCHES)
                .allSatisfy(key -> assertThat(development.getProperty(key))
                        .as(key)
                        .matches("\\$\\{[A-Z0-9_]+:false}"));
    }

    @Test
    void trackedRuntimeProfilesContainNoOperationalSecretFallbacks() throws IOException {
        Properties baseline = load("src/main/resources/application.properties");
        Properties development = load("src/main/resources/application-dev.properties");
        Properties production = load("src/main/resources/application-prod.properties");

        assertSafeSecretProperty(baseline, "optrabidz.admin.recovery.token");
        assertSafeSecretProperty(development, "spring.datasource.password");
        assertSafeSecretProperty(development, "optrabidz.admin.bootstrap.password");
        assertSafeSecretProperty(development, "optrabidz.admin.recovery.token");
        assertSafeSecretProperty(development, "optrabidz.financial.webhook.providers.UPI.active-secret");
        assertSafeSecretProperty(development, "optrabidz.financial.webhook.providers.CARD.active-secret");
        assertSafeSecretProperty(production, "spring.datasource.password");
    }

    @Test
    void environmentTemplateUsesDisabledSwitchesAndBlankSecrets() throws IOException {
        Path template = REPOSITORY_ROOT.resolve(".env.example");
        assertThat(template).isRegularFile();

        Properties environment = PropertiesLoaderUtils.loadProperties(new FileSystemResource(template));

        assertThat(List.of(
                "OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED",
                "OPTRABIDZ_ADMIN_RECOVERY_ENABLED",
                "OPTRABIDZ_LOCAL_PROVIDER_ENABLED",
                "OPTRABIDZ_SANDBOX_PROVIDERS_ENABLED",
                "OPTRABIDZ_UPI_WEBHOOK_ENABLED",
                "OPTRABIDZ_CARD_WEBHOOK_ENABLED"
        )).allSatisfy(key -> assertThat(environment.getProperty(key)).as(key).isEqualTo("false"));
        assertThat(SECRET_KEYS)
                .allSatisfy(key -> assertThat(environment.getProperty(key)).as(key).isEmpty());
    }

    @Test
    void environmentFilesAreIgnoredExceptForTheTemplate() throws Exception {
        assertThat(isIgnored(".env")).isTrue();
        assertThat(isIgnored(".env.local")).isTrue();
        assertThat(isIgnored(".env.example")).isFalse();
    }

    private static Properties load(String relativePath) throws IOException {
        Path path = REPOSITORY_ROOT.resolve(relativePath);
        assertThat(path).isRegularFile();
        return PropertiesLoaderUtils.loadProperties(new FileSystemResource(path));
    }

    private static void assertSafeSecretProperty(Properties properties, String key) {
        String configuredValue = properties.getProperty(key);
        assertThat(configuredValue)
                .as(key)
                .satisfies(value -> assertThat(value == null
                        || value.isBlank()
                        || value.matches("\\$\\{[A-Z0-9_]+(?::)?}"))
                        .isTrue());
    }

    private static boolean isIgnored(String relativePath) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "git", "check-ignore", "--quiet", "--no-index", relativePath
        ).directory(REPOSITORY_ROOT.toFile()).start();
        return process.waitFor() == 0;
    }
}
