package com.project.optrabidz.documentation.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.documentation.error.PublicErrorCatalogue;
import com.project.optrabidz.documentation.error.PublicErrorDefinition;
import com.project.optrabidz.testsupport.RealHttpIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiProblemDetailsIT extends RealHttpIntegrationTestSupport {

    private static final String PROBLEM_SCHEMA =
            "#/components/schemas/ProblemDetails";
    private static final String VALIDATION_SCHEMA =
            "#/components/schemas/ValidationProblemDetails";

    @Test
    void specificationReferencesMatchRealProblemDetailsResponses()
            throws Exception {
        RealHttpClient client = newClient();
        HttpResponse<String> specification = client.get(
                "/v3/api-docs",
                Map.of()
        );

        assertThat(specification.statusCode()).isEqualTo(200);
        JsonNode openApi = readJson(specification);
        assertComponentResponse(
                openApi,
                "ValidationProblem",
                VALIDATION_SCHEMA
        );
        assertComponentResponse(
                openApi,
                "NotFoundProblem",
                PROBLEM_SCHEMA
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1auth~1login/post/responses/400",
                "ValidationProblem"
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1auth~1login/post/responses/401",
                "UnauthorizedProblem"
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1me/get/responses/401",
                "UnauthorizedProblem"
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1funding-listings~1{listingId}/get"
                        + "/responses/400",
                "ValidationProblem"
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1funding-listings~1{listingId}/get"
                        + "/responses/404",
                "NotFoundProblem"
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1funding-listings/post/responses/401",
                "UnauthorizedProblem"
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1funding-listings/post/responses/403",
                "ForbiddenProblem"
        );
        assertOperationResponse(
                openApi,
                "/paths/~1api~1v1~1funding-listings/post/responses/422",
                "UnprocessableEntityProblem"
        );

        HttpResponse<String> validation = client.post(
                "/api/v1/auth/register",
                Map.of(
                        "email", "not-an-email",
                        "password", "Password01",
                        "role", "STARTUP"
                ),
                Map.of("X-Request-Id", "kan-43-validation")
        );
        assertRuntimeParity(
                openApi,
                validation,
                "VALIDATION_ERROR",
                "ValidationProblem",
                "kan-43-validation"
        );
        assertThat(readJson(validation).path("violations").isArray()).isTrue();

        HttpResponse<String> missingListing = client.get(
                "/api/v1/funding-listings/" + Long.MAX_VALUE,
                Map.of("X-Request-Id", "kan-43-not-found")
        );
        assertRuntimeParity(
                openApi,
                missingListing,
                "LISTING_NOT_FOUND",
                "NotFoundProblem",
                "kan-43-not-found"
        );
    }

    private void assertRuntimeParity(
            JsonNode openApi,
            HttpResponse<String> response,
            String code,
            String responseComponent,
            String requestId
    ) throws Exception {
        PublicErrorDefinition definition = PublicErrorCatalogue.createDefault()
                .entries()
                .stream()
                .filter(entry -> entry.code().equals(code))
                .findFirst()
                .orElseThrow();
        JsonNode body = readJson(response);

        assertThat(response.statusCode()).isEqualTo(definition.status());
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(value -> assertThat(value)
                        .startsWith("application/problem+json"));
        assertThat(response.headers().firstValue("X-Request-Id"))
                .contains(requestId);
        assertThat(body.path("status").asInt()).isEqualTo(definition.status());
        assertThat(body.path("code").asText()).isEqualTo(definition.code());
        assertThat(body.path("title").asText()).isEqualTo(definition.title());
        assertThat(body.path("detail").asText()).isEqualTo(definition.detail());
        assertThat(body.path("type").asText())
                .isEqualTo(definition.type().toString());
        assertThat(body.path("requestId").asText()).isEqualTo(requestId);

        JsonNode required = openApi.at(
                "/components/schemas/ProblemDetails/required"
        );
        assertThat(required).isNotNull();
        for (JsonNode field : required) {
            assertThat(body.hasNonNull(field.asText()))
                    .as("required Problem Details field %s", field.asText())
                    .isTrue();
        }
        assertThat(body.has("success")).isFalse();
        assertThat(body.has("error")).isFalse();
        assertThat(response.body())
                .doesNotContainIgnoringCase(
                        "exception",
                        "stackTrace",
                        "diagnostic",
                        "secret"
                );
        assertThat(openApi.at(
                "/components/responses/" + responseComponent
                        + "/headers/X-Request-Id/$ref"
        ).asText()).isEqualTo("#/components/headers/RequestIdHeader");
    }

    private void assertComponentResponse(
            JsonNode openApi,
            String responseComponent,
            String schemaReference
    ) {
        assertThat(openApi.at(
                "/components/responses/" + responseComponent
                        + "/content/application~1problem+json/schema/$ref"
        ).asText()).isEqualTo(schemaReference);
    }

    private void assertOperationResponse(
            JsonNode openApi,
            String pointer,
            String responseComponent
    ) {
        assertThat(openApi.at(pointer + "/$ref").asText())
                .isEqualTo("#/components/responses/" + responseComponent);
    }
}
