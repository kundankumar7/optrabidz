package com.project.optrabidz.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDevelopmentRuntimeContractTest {
    private static final Path REPOSITORY_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path ROOT_README = REPOSITORY_ROOT.resolve("README.md");
    private static final Path GETTING_STARTED =
            REPOSITORY_ROOT.resolve("docs/getting-started/README.md");
    private static final Path OPERATIONS =
            REPOSITORY_ROOT.resolve("docs/operations/README.md");
    private static final Path MIGRATIONS =
            REPOSITORY_ROOT.resolve("docs/database/migrations.md");
    private static final String CONTRACT_PASSWORD = "local-contract-password";

    @Test
    void composeDefinesOnlyThePostgresDependency() throws Exception {
        JsonNode configuration = resolvedComposeConfiguration();

        assertThat(configuration.path("name").asText()).isEqualTo("optrabidz");
        assertThat(configuration.path("services").propertyNames())
                .containsExactly("postgres");
    }

    @Test
    void composePinsPostgres16AndUsesProjectScopedStorage() throws Exception {
        JsonNode configuration = resolvedComposeConfiguration();
        JsonNode postgres = configuration.path("services").path("postgres");

        assertThat(postgres.path("image").asText()).isEqualTo("postgres:16-alpine");
        assertThat(postgres.path("volumes").get(0).path("source").asText())
                .isEqualTo("postgres-data");
        assertThat(postgres.path("volumes").get(0).path("target").asText())
                .isEqualTo("/var/lib/postgresql/data");
        assertThat(configuration.path("volumes").propertyNames())
                .containsExactly("postgres-data");
        assertThat(configuration.path("volumes").path("postgres-data").path("name").asText())
                .isEqualTo("optrabidz_postgres-data");
    }

    @Test
    void composeRequiresExternalDatabasePassword() throws Exception {
        ProcessResult result = runCompose(false);

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output())
                .contains("OPTRABIDZ_DATASOURCE_PASSWORD")
                .doesNotContain(CONTRACT_PASSWORD);
    }

    @Test
    void composePublishesTheLocalDatabasePortAndDefinesHealthCheck() throws Exception {
        JsonNode postgres = resolvedComposeConfiguration().path("services").path("postgres");
        JsonNode publishedPort = postgres.path("ports").get(0);

        assertThat(publishedPort.path("published").asText()).isEqualTo("5432");
        assertThat(publishedPort.path("target").asInt()).isEqualTo(5432);
        assertThat(publishedPort.path("protocol").asText()).isEqualTo("tcp");
        assertThat(postgres.path("healthcheck").path("test").toString())
                .contains("pg_isready", "optrabidz");
        assertThat(postgres.path("healthcheck").path("interval").asText()).isEqualTo("10s");
        assertThat(postgres.path("healthcheck").path("timeout").asText()).isEqualTo("5s");
        assertThat(postgres.path("healthcheck").path("retries").asInt()).isEqualTo(5);
    }

    @Test
    void environmentTemplateMatchesLocalDatabaseContract() throws IOException {
        Properties environment = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource(REPOSITORY_ROOT.resolve(".env.example"))
        );

        assertThat(environment.getProperty("OPTRABIDZ_DATASOURCE_URL"))
                .isEqualTo("jdbc:postgresql://localhost:5432/optrabidz");
        assertThat(environment.getProperty("OPTRABIDZ_DATASOURCE_USERNAME"))
                .isEqualTo("postgres");
        assertThat(environment.getProperty("OPTRABIDZ_DATASOURCE_PASSWORD")).isEmpty();
    }

    @Test
    void rootReadmeLinksToCanonicalGettingStartedGuide() throws IOException {
        String readme = Files.readString(ROOT_README);

        assertThat(readme)
                .contains("[Getting Started](docs/getting-started/README.md)")
                .doesNotContain("docker run");
    }

    @Test
    void gettingStartedExplainsDockerAsLocalDatabasePackaging() throws IOException {
        String guide = Files.readString(GETTING_STARTED);

        assertThat(guide)
                .contains("Docker Compose", "PostgreSQL 16", "IntelliJ", "Maven")
                .doesNotContain("docker run");
    }

    @Test
    void gettingStartedDocumentsNativePostgresAlternative() throws IOException {
        String guide = Files.readString(GETTING_STARTED);

        assertThat(guide)
                .contains("Native PostgreSQL 16")
                .contains("OPTRABIDZ_DATASOURCE_URL")
                .contains("OPTRABIDZ_DATASOURCE_USERNAME")
                .contains("OPTRABIDZ_DATASOURCE_PASSWORD");
    }

    @Test
    void gettingStartedProvidesOneOrderedCloneToLoginJourney() throws IOException {
        String guide = Files.readString(GETTING_STARTED);

        assertAppearsInOrder(
                guide,
                ".env.example",
                "docker compose up -d postgres",
                "/actuator/health/readiness",
                "/swagger-ui.html",
                "OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=true",
                "/api/v1/auth/login",
                "/api/v1/me",
                "OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=false");
    }

    @Test
    void operationsDistinguishStopFromDestructiveReset() throws IOException {
        String operations = Files.readString(OPERATIONS).toLowerCase();

        assertThat(operations)
                .contains("docker compose stop postgres")
                .contains("docker compose start postgres")
                .contains("../database/migrations.md")
                .doesNotContain("docker compose down --volumes");
    }

    @Test
    void resetGuidanceNamesOnlyProjectScopedLocalResources() throws IOException {
        String migrations = Files.readString(MIGRATIONS).toLowerCase();

        assertThat(migrations)
                .contains("optrabidz_postgres-data")
                .contains("docker compose down --volumes")
                .doesNotContain("docker system prune")
                .doesNotContain("docker volume prune")
                .doesNotContain("docker rm -f");
    }

    @Test
    void adminWalkthroughRequiresBootstrapDisableAndSecretRemoval() throws IOException {
        String guide = Files.readString(GETTING_STARTED);

        assertAppearsInOrder(
                guide,
                "OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=true",
                "/api/v1/auth/login",
                "/api/v1/me",
                "OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=false",
                "OPTRABIDZ_ADMIN_BOOTSTRAP_EMAIL=",
                "OPTRABIDZ_ADMIN_BOOTSTRAP_PASSWORD=",
                "OPTRABIDZ_ADMIN_BOOTSTRAP_DISPLAY_NAME=",
                "OPTRABIDZ_ADMIN_BOOTSTRAP_ORGANIZATION=");
    }

    private static JsonNode resolvedComposeConfiguration() throws Exception {
        ProcessResult result = runCompose(true);
        assertThat(result.exitCode()).as(result.output()).isZero();
        return JsonMapper.builder().build().readTree(result.output());
    }

    private static ProcessResult runCompose(boolean supplyPassword) throws Exception {
        Path emptyEnvironment = REPOSITORY_ROOT.resolve("target/local-runtime-contract.env");
        Files.createDirectories(emptyEnvironment.getParent());
        Files.writeString(emptyEnvironment, "", StandardCharsets.UTF_8);

        ProcessBuilder builder = new ProcessBuilder(
                "docker", "compose",
                "--env-file", emptyEnvironment.toString(),
                "-f", REPOSITORY_ROOT.resolve("compose.yaml").toString(),
                "config", "--format", "json"
        ).directory(REPOSITORY_ROOT.toFile()).redirectErrorStream(true);
        builder.environment().remove("OPTRABIDZ_DATASOURCE_PASSWORD");
        if (supplyPassword) {
            builder.environment().put("OPTRABIDZ_DATASOURCE_PASSWORD", CONTRACT_PASSWORD);
        }

        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.waitFor(), output);
    }

    private static void assertAppearsInOrder(String content, String... markers) {
        int previousIndex = -1;
        for (String marker : markers) {
            int currentIndex = content.indexOf(marker, previousIndex + 1);
            assertThat(currentIndex)
                    .as("expected '%s' after the previous journey step", marker)
                    .isGreaterThan(previousIndex);
            previousIndex = currentIndex;
        }
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
