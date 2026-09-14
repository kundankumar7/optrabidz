package com.project.optrabidz.documentation.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.testsupport.RealHttpIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiSuccessContractIT extends RealHttpIntegrationTestSupport {
    private static final Set<String> PUBLISHED_OPERATIONS = Set.of(
            "GET /api/v1/admin/audit-records",
            "GET /api/v1/agreements/{agreementId}",
            "GET /api/v1/agreements/{agreementId}/repayment-progress",
            "POST /api/v1/auth/change-password",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/logout",
            "POST /api/v1/auth/register",
            "GET /api/v1/bids",
            "POST /api/v1/bids",
            "GET /api/v1/bids/{bidId}",
            "POST /api/v1/bids/{bidId}/actions/accept",
            "POST /api/v1/bids/{bidId}/actions/reject",
            "POST /api/v1/bids/{bidId}/actions/withdraw",
            "GET /api/v1/funding-listings",
            "POST /api/v1/funding-listings",
            "GET /api/v1/funding-listings/recommended",
            "GET /api/v1/funding-listings/{listingId}",
            "PATCH /api/v1/funding-listings/{listingId}",
            "GET /api/v1/funding-listings/{listingId}/accepted-bid",
            "POST /api/v1/funding-listings/{listingId}/actions/close",
            "POST /api/v1/funding-listings/{listingId}/actions/publish",
            "GET /api/v1/investor-preferences/me",
            "DELETE /api/v1/investor-preferences/me",
            "POST /api/v1/investor-preferences",
            "PUT /api/v1/investor-preferences/me",
            "GET /api/v1/investors/me",
            "PATCH /api/v1/investors/me",
            "POST /api/v1/investors",
            "GET /api/v1/investors/me/agreements",
            "GET /api/v1/investors/me/bids",
            "GET /api/v1/investors/me/bids/by-listing/{listingId}",
            "GET /api/v1/investors/me/repayment-installments",
            "GET /api/v1/investors/me/repayments",
            "GET /api/v1/investors/me/settlements",
            "GET /api/v1/me",
            "POST /api/v1/notification-subscriptions",
            "DELETE /api/v1/notification-subscriptions/{subscriptionId}",
            "GET /api/v1/notifications/me",
            "PATCH /api/v1/notifications/me/read-all",
            "GET /api/v1/notifications/me/summary",
            "DELETE /api/v1/notifications/{recipientId}",
            "PATCH /api/v1/notifications/{recipientId}/read",
            "POST /api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
            "POST /api/v1/payment-attempts/{paymentAttemptId}/actions/local-fail",
            "GET /api/v1/payment-intents/{paymentIntentId}",
            "POST /api/v1/payment-intents/{paymentIntentId}/attempts",
            "POST /api/v1/payment-providers/{providerCode}/webhooks",
            "GET /api/v1/repayment-installments/{installmentId}",
            "POST /api/v1/repayment-installments/{installmentId}/payment-intents",
            "GET /api/v1/repayments/{repaymentId}",
            "GET /api/v1/repayments/{repaymentId}/installments",
            "POST /api/v1/repayments/{repaymentId}/payment-intents",
            "GET /api/v1/settlements/{settlementId}",
            "POST /api/v1/settlements/{settlementId}/payment-intents",
            "GET /api/v1/startup-classifications/me",
            "DELETE /api/v1/startup-classifications/me",
            "POST /api/v1/startup-classifications",
            "PUT /api/v1/startup-classifications/me",
            "GET /api/v1/startups/me",
            "PATCH /api/v1/startups/me",
            "POST /api/v1/startups",
            "GET /api/v1/startups/me/agreements",
            "GET /api/v1/startups/me/funding-listings",
            "GET /api/v1/startups/me/repayment-installments",
            "GET /api/v1/startups/me/repayments",
            "GET /api/v1/startups/me/settlements"
    );

    private static final Map<String, String> CREATED_RESPONSE_SCHEMAS = Map.of(
            "POST /api/v1/auth/register", "SignupResponse",
            "POST /api/v1/startups", "StartupResponse",
            "POST /api/v1/investors", "InvestorResponse",
            "POST /api/v1/funding-listings", "ListingResponse",
            "POST /api/v1/bids", "BidResponse",
            "POST /api/v1/settlements/{settlementId}/payment-intents",
            "PaymentIntentResponse",
            "POST /api/v1/repayments/{repaymentId}/payment-intents",
            "PaymentIntentResponse",
            "POST /api/v1/repayment-installments/{installmentId}/payment-intents",
            "PaymentIntentResponse",
            "POST /api/v1/payment-intents/{paymentIntentId}/attempts",
            "PaymentAttemptResponse"
    );

    private static final Set<String> CREATED_OPERATIONS =
            CREATED_RESPONSE_SCHEMAS.keySet();

    private static final Set<String> NO_CONTENT_OPERATIONS = Set.of(
            "POST /api/v1/auth/logout",
            "POST /api/v1/auth/change-password",
            "POST /api/v1/startup-classifications",
            "PUT /api/v1/startup-classifications/me",
            "DELETE /api/v1/startup-classifications/me",
            "POST /api/v1/investor-preferences",
            "PUT /api/v1/investor-preferences/me",
            "DELETE /api/v1/investor-preferences/me",
            "PATCH /api/v1/notifications/{recipientId}/read",
            "DELETE /api/v1/notifications/{recipientId}",
            "DELETE /api/v1/notification-subscriptions/{subscriptionId}",
            "POST /api/v1/payment-providers/{providerCode}/webhooks"
    );

    private static final Set<String> CREATED_WITH_LOCATION_OPERATIONS = Set.of(
            "POST /api/v1/startups",
            "POST /api/v1/investors",
            "POST /api/v1/funding-listings",
            "POST /api/v1/bids",
            "POST /api/v1/settlements/{settlementId}/payment-intents",
            "POST /api/v1/repayments/{repaymentId}/payment-intents",
            "POST /api/v1/repayment-installments/{installmentId}/payment-intents"
    );

    @Test
    void specificationPublishesTheCompleteSuccessContract() throws Exception {
        JsonNode openApi = specification();

        assertThat(publishedOperations(openApi))
                .containsExactlyInAnyOrderElementsOf(PUBLISHED_OPERATIONS);
        assertThat(operationsWithResponse(openApi, "201"))
                .containsExactlyInAnyOrderElementsOf(CREATED_OPERATIONS);
        assertThat(operationsWithResponse(openApi, "204"))
                .containsExactlyInAnyOrderElementsOf(NO_CONTENT_OPERATIONS);

        CREATED_OPERATIONS.forEach(operation -> {
            JsonNode created = response(openApi, operation, "201");
            assertThat(responseSchema(created).path("$ref").asText())
                    .as("%s response schema", operation)
                    .isEqualTo("#/components/schemas/"
                            + CREATED_RESPONSE_SCHEMAS.get(operation));
            assertThat(created.path("headers").has("Location"))
                    .as("%s Location header", operation)
                    .isEqualTo(CREATED_WITH_LOCATION_OPERATIONS.contains(operation));
        });
        NO_CONTENT_OPERATIONS.forEach(operation -> {
            JsonNode noContent = response(openApi, operation, "204");
            assertThat(noContent.path("content").isMissingNode()
                    || noContent.path("content").isEmpty())
                    .as("%s must not publish a response body", operation)
                    .isTrue();
        });

        JsonNode directObject = response(openApi, "/api/v1/me", "get", "200");
        assertThat(resolveSchema(openApi, directObject).path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder(
                        "role", "accountState", "profileStatus", "actorType", "actorExists")
                .doesNotContain("success", "data", "meta", "csrfToken");

        JsonNode directPage = response(
                openApi, "/api/v1/funding-listings", "get", "200");
        assertThat(responseSchema(directPage).path("$ref").asText())
                .startsWith("#/components/schemas/PageResponse");
        assertThat(resolveSchema(openApi, directPage).path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder(
                        "items", "page", "size", "totalItems", "totalPages");

        JsonNode created = response(
                openApi, "/api/v1/funding-listings", "post", "201");
        assertThat(created.path("headers").path("Location").isMissingNode())
                .isFalse();
        assertThat(resolveSchema(openApi, created).path("properties").fieldNames())
                .toIterable()
                .contains("listingId")
                .doesNotContain("success", "data", "meta", "csrfToken");

        JsonNode noContent = response(
                openApi, "/api/v1/auth/logout", "post", "204");
        assertThat(noContent.path("content").isMissingNode()
                || noContent.path("content").isEmpty()).isTrue();

        assertThat(openApi.at("/components/schemas/SuccessResponse").isMissingNode())
                .isTrue();
        assertThat(openApi.at("/components/schemas/ProblemDetails").isMissingNode())
                .isFalse();
        assertThat(openApi.at("/components/schemas/ValidationProblemDetails").isMissingNode())
                .isFalse();
        assertThat(openApi.at(
                "/paths/~1api~1v1~1auth~1login/post/responses/401/$ref"
        ).asText()).isEqualTo("#/components/responses/UnauthorizedProblem");

        assertNoForbiddenSuccessProperties(openApi.path("components").path("schemas"));
    }

    private JsonNode specification() throws Exception {
        HttpResponse<String> response = newClient().get("/v3/api-docs", Map.of());
        assertThat(response.statusCode()).isEqualTo(200);
        return readJson(response);
    }

    private Set<String> operationsWithResponse(JsonNode openApi, String responseCode) {
        Set<String> operations = new HashSet<>();
        openApi.path("paths").properties().forEach(pathEntry ->
                pathEntry.getValue().properties().forEach(operationEntry -> {
                    if (operationEntry.getValue().path("responses").has(responseCode)) {
                        operations.add(operationEntry.getKey().toUpperCase()
                                + " " + pathEntry.getKey());
                    }
                })
        );
        return operations;
    }

    private Set<String> publishedOperations(JsonNode openApi) {
        Set<String> operations = new HashSet<>();
        Set<String> httpMethods = Set.of(
                "get", "put", "post", "delete", "patch", "head", "options", "trace"
        );
        openApi.path("paths").properties().forEach(pathEntry ->
                pathEntry.getValue().properties().forEach(operationEntry -> {
                    if (pathEntry.getKey().startsWith("/api/v1/")
                            && httpMethods.contains(operationEntry.getKey())) {
                        operations.add(operationEntry.getKey().toUpperCase()
                                + " " + pathEntry.getKey());
                    }
                })
        );
        return operations;
    }

    private JsonNode response(JsonNode openApi,
                              String path,
                              String method,
                              String status) {
        JsonNode response = openApi.path("paths").path(path)
                .path(method).path("responses").path(status);
        assertThat(response.isMissingNode())
                .as("%s %s must publish HTTP %s", method.toUpperCase(), path, status)
                .isFalse();
        return response;
    }

    private JsonNode response(JsonNode openApi, String operation, String status) {
        int separator = operation.indexOf(' ');
        return response(
                openApi,
                operation.substring(separator + 1),
                operation.substring(0, separator).toLowerCase(),
                status
        );
    }

    private JsonNode resolveSchema(JsonNode openApi, JsonNode response) {
        JsonNode schema = responseSchema(response);
        String reference = schema.path("$ref").asText();
        if (reference.startsWith("#/components/schemas/")) {
            return openApi.path("components").path("schemas")
                    .path(reference.substring(reference.lastIndexOf('/') + 1));
        }
        return schema;
    }

    private JsonNode responseSchema(JsonNode response) {
        JsonNode content = response.path("content");
        JsonNode mediaType = content.has("application/json")
                ? content.path("application/json")
                : content.elements().next();
        return mediaType.path("schema");
    }

    private void assertNoForbiddenSuccessProperties(JsonNode schemas) {
        Iterator<Map.Entry<String, JsonNode>> entries =
                schemas.properties().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, JsonNode> entry = entries.next();
            if (entry.getKey().contains("ProblemDetails")) {
                continue;
            }
            JsonNode properties = entry.getValue().path("properties");
            assertThat(properties.fieldNames()).toIterable()
                    .as("schema %s", entry.getKey())
                    .doesNotContain("success", "data", "meta", "csrfToken");
        }
    }
}
