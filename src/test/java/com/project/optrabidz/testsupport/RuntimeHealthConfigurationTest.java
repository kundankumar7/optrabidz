package com.project.optrabidz.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class RuntimeHealthConfigurationTest {

    private static final Path APPLICATION_PROPERTIES =
            Path.of("src", "main", "resources", "application.properties");

    @Test
    void exposesOnlyStatusOnlyHealthProbes() throws IOException {
        Properties properties = loadApplicationProperties();

        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health");
        assertThat(properties.getProperty("management.endpoint.health.probes.enabled"))
                .isEqualTo("true");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("never");
    }

    @Test
    void separatesApplicationLivenessFromDatabaseReadiness() throws IOException {
        Properties properties = loadApplicationProperties();

        assertThat(properties.getProperty("management.endpoint.health.group.liveness.include"))
                .isEqualTo("livenessState");
        assertThat(properties.getProperty("management.endpoint.health.group.readiness.include"))
                .isEqualTo("readinessState,db");
    }

    private Properties loadApplicationProperties() throws IOException {
        return PropertiesLoaderUtils.loadProperties(
                new FileSystemResource(APPLICATION_PROPERTIES));
    }
}
