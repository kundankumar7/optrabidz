package com.project.optrabidz.configuration;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationMetadataContractTest {

    private static final Path METADATA_FILE = Path.of(
            "target", "classes", "META-INF", "spring-configuration-metadata.json");

    private static final List<String> DOCUMENTED_PROPERTIES = List.of(
            "optrabidz.admin.bootstrap.enabled",
            "optrabidz.admin.bootstrap.email",
            "optrabidz.admin.bootstrap.password",
            "optrabidz.admin.bootstrap.public-display-name",
            "optrabidz.admin.bootstrap.organization-label",
            "optrabidz.admin.recovery.enabled",
            "optrabidz.admin.recovery.token",
            "optrabidz.documentation.api-docs-enabled",
            "optrabidz.documentation.swagger-ui-enabled",
            "optrabidz.documentation.management-port-enabled",
            "optrabidz.documentation.access",
            "optrabidz.governance.eligibility.require-startup-classification-for-listing",
            "optrabidz.governance.eligibility.require-investor-preferences-for-bidding",
            "optrabidz.governance.lifecycle.expiry-batch-size",
            "optrabidz.governance.lifecycle.scheduler.enabled",
            "optrabidz.governance.lifecycle.scheduler.initial-delay-ms",
            "optrabidz.governance.lifecycle.scheduler.fixed-delay-ms",
            "optrabidz.marketplace.listing.default-expiry-days",
            "optrabidz.outbox.dispatcher.enabled",
            "optrabidz.outbox.dispatcher.initial-delay-ms",
            "optrabidz.outbox.dispatcher.fixed-delay-ms",
            "optrabidz.outbox.dispatcher.batch-size",
            "optrabidz.outbox.dispatcher.worker-id",
            "optrabidz.notification.dispatcher.enabled",
            "optrabidz.notification.dispatcher.initial-delay-ms",
            "optrabidz.notification.dispatcher.fixed-delay-ms",
            "optrabidz.notification.dispatcher.batch-size",
            "optrabidz.notification.dispatcher.max-attempts",
            "optrabidz.notification.dispatcher.worker-id",
            "optrabidz.notification.channels.email.enabled",
            "optrabidz.notification.channels.push.enabled",
            "optrabidz.security.session-duration",
            "optrabidz.security.max-login-failures",
            "optrabidz.financial.settlement.expiry-minutes",
            "optrabidz.financial.payment-intent.expiry-minutes",
            "optrabidz.financial.local-provider.enabled",
            "optrabidz.financial.sandbox-providers.enabled",
            "optrabidz.financial.webhook.max-body-size",
            "optrabidz.financial.webhook.timestamp-tolerance",
            "optrabidz.financial.webhook.providers.UPI.enabled",
            "optrabidz.financial.webhook.providers.UPI.active-secret",
            "optrabidz.financial.webhook.providers.UPI.previous-secret",
            "optrabidz.financial.webhook.providers.UPI.previous-secret-valid-until",
            "optrabidz.financial.webhook.providers.CARD.enabled",
            "optrabidz.financial.webhook.providers.CARD.active-secret",
            "optrabidz.financial.webhook.providers.CARD.previous-secret",
            "optrabidz.financial.webhook.providers.CARD.previous-secret-valid-until"
    );

    private static final List<String> SECRET_PROPERTIES = List.of(
            "optrabidz.admin.bootstrap.password",
            "optrabidz.admin.recovery.token",
            "optrabidz.financial.webhook.providers.UPI.active-secret",
            "optrabidz.financial.webhook.providers.UPI.previous-secret",
            "optrabidz.financial.webhook.providers.CARD.active-secret",
            "optrabidz.financial.webhook.providers.CARD.previous-secret"
    );

    @Test
    void publishesMetadataForSupportedConfigurationProperties() throws IOException {
        Map<String, JsonNode> properties = metadataProperties();

        assertThat(properties).containsKeys(DOCUMENTED_PROPERTIES.toArray(String[]::new));
        assertThat(DOCUMENTED_PROPERTIES)
                .allSatisfy(name -> assertThat(properties.get(name).path("description").asText())
                        .as(name)
                        .isNotBlank());
    }

    @Test
    void publishesTypesAndDefaultsForSupplementalProperties() throws IOException {
        Map<String, JsonNode> properties = metadataProperties();

        assertProperty(properties, "optrabidz.security.session-duration", "java.time.Duration", "PT8H");
        assertProperty(properties, "optrabidz.security.max-login-failures", "java.lang.Integer", 5);
        assertProperty(properties, "optrabidz.governance.lifecycle.scheduler.enabled", "java.lang.Boolean", true);
        assertProperty(properties, "optrabidz.governance.lifecycle.scheduler.initial-delay-ms", "java.lang.Long", 60000L);
        assertProperty(properties, "optrabidz.governance.lifecycle.scheduler.fixed-delay-ms", "java.lang.Long", 300000L);
        assertProperty(properties, "optrabidz.marketplace.listing.default-expiry-days", "java.lang.Integer", 14);
        assertProperty(properties, "optrabidz.outbox.dispatcher.enabled", "java.lang.Boolean", true);
        assertProperty(properties, "optrabidz.outbox.dispatcher.initial-delay-ms", "java.lang.Long", 5000L);
        assertProperty(properties, "optrabidz.outbox.dispatcher.fixed-delay-ms", "java.lang.Long", 5000L);
        assertProperty(properties, "optrabidz.outbox.dispatcher.batch-size", "java.lang.Integer", 50);
        assertProperty(properties, "optrabidz.outbox.dispatcher.worker-id", "java.lang.String", "");
        assertProperty(properties, "optrabidz.notification.dispatcher.enabled", "java.lang.Boolean", true);
        assertProperty(properties, "optrabidz.notification.dispatcher.initial-delay-ms", "java.lang.Long", 5000L);
        assertProperty(properties, "optrabidz.notification.dispatcher.fixed-delay-ms", "java.lang.Long", 5000L);
        assertProperty(properties, "optrabidz.notification.dispatcher.batch-size", "java.lang.Integer", 50);
        assertProperty(properties, "optrabidz.notification.dispatcher.max-attempts", "java.lang.Integer", 3);
        assertProperty(properties, "optrabidz.notification.dispatcher.worker-id", "java.lang.String", "");
        assertProperty(properties, "optrabidz.notification.channels.email.enabled", "java.lang.Boolean", true);
        assertProperty(properties, "optrabidz.notification.channels.push.enabled", "java.lang.Boolean", true);
        assertProperty(properties, "optrabidz.financial.settlement.expiry-minutes", "java.lang.Long", 30L);
        assertProperty(properties, "optrabidz.financial.payment-intent.expiry-minutes", "java.lang.Long", 15L);
        assertProperty(properties, "optrabidz.financial.webhook.providers.UPI.enabled", "java.lang.Boolean", false);
        assertType(properties, "optrabidz.financial.webhook.providers.UPI.active-secret", "java.lang.String");
        assertType(properties, "optrabidz.financial.webhook.providers.UPI.previous-secret", "java.lang.String");
        assertType(properties, "optrabidz.financial.webhook.providers.UPI.previous-secret-valid-until", "java.time.Instant");
        assertProperty(properties, "optrabidz.financial.webhook.providers.CARD.enabled", "java.lang.Boolean", false);
        assertType(properties, "optrabidz.financial.webhook.providers.CARD.active-secret", "java.lang.String");
        assertType(properties, "optrabidz.financial.webhook.providers.CARD.previous-secret", "java.lang.String");
        assertType(properties, "optrabidz.financial.webhook.providers.CARD.previous-secret-valid-until", "java.time.Instant");
    }

    @Test
    void neverPublishesSecretDefaultsOrTheStaleRepaymentSetting() throws IOException {
        Map<String, JsonNode> properties = metadataProperties();

        assertThat(SECRET_PROPERTIES)
                .allSatisfy(name -> {
                    JsonNode property = properties.get(name);
                    assertThat(property).as(name).isNotNull();
                    assertThat(property.has("defaultValue")).as(name).isFalse();
                });
        assertThat(properties).doesNotContainKey("optrabidz.financial.repayment.expiry-days");
    }

    private static Map<String, JsonNode> metadataProperties() throws IOException {
        assertThat(METADATA_FILE)
                    .as("generated Spring configuration metadata")
                    .isRegularFile();

        try (InputStream input = Files.newInputStream(METADATA_FILE)) {
            JsonNode metadata = JsonMapper.builder().build().readTree(input);
            Map<String, JsonNode> properties = new LinkedHashMap<>();
            metadata.path("properties").forEach(property -> {
                String name = property.path("name").asText();
                assertThat(properties.put(name, property))
                        .as("duplicate metadata entry for " + name)
                        .isNull();
            });
            return properties;
        }
    }

    private static void assertType(Map<String, JsonNode> properties, String name, String type) {
        JsonNode property = properties.get(name);
        assertThat(property).as(name).isNotNull();
        assertThat(property.path("type").asText()).as(name + " type").isEqualTo(type);
    }

    private static void assertProperty(
            Map<String, JsonNode> properties,
            String name,
            String type,
            Object defaultValue
    ) {
        assertType(properties, name, type);
        JsonNode property = properties.get(name);
        JsonNode actualDefault = property.path("defaultValue");
        if (defaultValue instanceof String expected) {
            assertThat(actualDefault.asText()).as(name + " default").isEqualTo(expected);
        } else if (defaultValue instanceof Integer expected) {
            assertThat(actualDefault.asInt()).as(name + " default").isEqualTo(expected);
        } else if (defaultValue instanceof Long expected) {
            assertThat(actualDefault.asLong()).as(name + " default").isEqualTo(expected);
        } else if (defaultValue instanceof Boolean expected) {
            assertThat(actualDefault.asBoolean()).as(name + " default").isEqualTo(expected);
        } else {
            throw new IllegalArgumentException("Unsupported expected default type: " + defaultValue.getClass());
        }
    }
}
