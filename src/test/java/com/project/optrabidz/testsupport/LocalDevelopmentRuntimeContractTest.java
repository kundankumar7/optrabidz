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

    private record ProcessResult(int exitCode, String output) {
    }
}
