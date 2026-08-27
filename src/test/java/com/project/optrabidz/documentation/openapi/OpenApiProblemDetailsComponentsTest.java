package com.project.optrabidz.documentation.openapi;

import com.project.optrabidz.documentation.error.PublicErrorCatalogue;
import com.project.optrabidz.documentation.error.PublicErrorDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiProblemDetailsComponentsTest {

    private static final String PROBLEM_SCHEMA =
            "#/components/schemas/ProblemDetails";
    private static final String VALIDATION_SCHEMA =
            "#/components/schemas/ValidationProblemDetails";
    private static final String REQUEST_ID_HEADER =
            "#/components/headers/RequestIdHeader";

    @Test
    void publishesTheExactProblemDetailsWireSchema() {
        OpenAPI openApi = customizedOpenApi();
        Schema<?> problem = openApi.getComponents()
                .getSchemas().get("ProblemDetails");

        assertThat(problem.getRequired()).containsExactlyInAnyOrder(
                "type", "title", "status", "detail", "instance",
                "code", "requestId", "timestamp"
        );
        assertThat(problem.getProperties()).containsOnlyKeys(
                "type", "title", "status", "detail", "instance",
                "code", "requestId", "timestamp"
        );
        assertThat(problem.getAdditionalProperties()).isNull();
        assertStringProperty(problem, "type", "uri");
        assertStringProperty(problem, "instance", "uri");
        assertStringProperty(problem, "timestamp", "date-time");
        Schema<?> status = property(problem, "status");
        assertThat(status.getType()).isEqualTo("integer");
        assertThat(status.getFormat()).isEqualTo("int32");
        assertThat(status.getMinimum().intValue()).isEqualTo(400);
        assertThat(status.getMaximum().intValue()).isEqualTo(599);

        List<String> publishedCodes = property(problem, "code").getEnum()
                .stream()
                .map(Object::toString)
                .toList();
        assertThat(publishedCodes).containsExactlyElementsOf(
                PublicErrorCatalogue.createDefault().entries().stream()
                        .map(PublicErrorDefinition::code)
                        .toList()
        );
    }

    @Test
    void composesValidationProblemsWithoutClosingTheBaseSchema() {
        OpenAPI openApi = customizedOpenApi();
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        ComposedSchema validation = (ComposedSchema) schemas.get(
                "ValidationProblemDetails"
        );

        assertThat(validation.getAllOf()).hasSize(2);
        assertThat(validation.getAllOf().getFirst().get$ref())
                .isEqualTo(PROBLEM_SCHEMA);
        Schema<?> extension = validation.getAllOf().get(1);
        assertThat(extension.getRequired()).containsExactly("violations");
        assertThat(extension.getProperties()).containsOnlyKeys("violations");
        assertThat(property(extension, "violations").getItems().get$ref())
                .isEqualTo("#/components/schemas/ValidationViolation");

        Schema<?> violation = schemas.get("ValidationViolation");
        assertThat(violation.getRequired())
                .containsExactlyInAnyOrder("field", "message");
        assertThat(violation.getProperties()).containsOnlyKeys("field", "message");
        assertStringProperty(violation, "field", null);
        assertStringProperty(violation, "message", null);
    }

    @Test
    void publishesAllReusableProblemResponsesAndRequestIdHeaders() {
        OpenAPI openApi = customizedOpenApi();
        Map<String, ExpectedResponse> expected = expectedResponses();

        assertThat(openApi.getComponents().getResponses())
                .containsOnlyKeys(expected.keySet());
        expected.forEach((name, contract) -> {
            ApiResponse response = openApi.getComponents()
                    .getResponses().get(name);
            assertThat(response.getDescription())
                    .startsWith(Integer.toString(contract.status()));
            assertThat(response.getContent()).containsOnlyKeys(
                    "application/problem+json"
            );
            assertThat(response.getContent()
                    .get("application/problem+json")
                    .getSchema().get$ref())
                    .isEqualTo(contract.schemaReference());
            assertThat(response.getHeaders()).containsOnlyKeys("X-Request-Id");
            assertThat(response.getHeaders().get("X-Request-Id").get$ref())
                    .isEqualTo(REQUEST_ID_HEADER);
        });

        Schema<?> requestId = openApi.getComponents()
                .getHeaders().get("RequestIdHeader").getSchema();
        assertThat(requestId.getType()).isEqualTo("string");
        assertThat(openApi.getComponents().getHeaders()
                .get("RequestIdHeader").getDescription()).isNotBlank();
    }

    @Test
    void schemaPublicationContainsOnlyApprovedPublicPropertyNames() {
        OpenAPI openApi = customizedOpenApi();
        Set<String> approved = Set.of(
                "type", "title", "status", "detail", "instance",
                "code", "requestId", "timestamp", "violations",
                "field", "message"
        );

        openApi.getComponents().getSchemas().values().forEach(schema ->
                assertSchemaUsesOnly(schema, approved)
        );
    }

    private static OpenAPI customizedOpenApi() {
        OpenAPI openApi = new OpenAPI();
        new OpenApiProblemDetailsConfiguration()
                .problemDetailsCustomizer()
                .customise(openApi);
        return openApi;
    }

    private static void assertSchemaUsesOnly(
            Schema<?> schema,
            Set<String> approved
    ) {
        if (schema.getProperties() != null) {
            assertThat(schema.getProperties().keySet()).isSubsetOf(approved);
            schema.getProperties().values().forEach(property -> {
                assertThat(property.getExample()).isNull();
                assertSchemaUsesOnly(property, approved);
            });
        }
        if (schema.getItems() != null) {
            assertSchemaUsesOnly(schema.getItems(), approved);
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(part -> assertSchemaUsesOnly(part, approved));
        }
    }

    private static void assertStringProperty(
            Schema<?> parent,
            String name,
            String format
    ) {
        Schema<?> property = property(parent, name);
        assertThat(property.getType()).isEqualTo("string");
        assertThat(property.getFormat()).isEqualTo(format);
    }

    private static Schema<?> property(Schema<?> parent, String name) {
        return (Schema<?>) parent.getProperties().get(name);
    }

    private static Map<String, ExpectedResponse> expectedResponses() {
        Map<String, ExpectedResponse> responses = new LinkedHashMap<>();
        responses.put("BadRequestProblem", response(400, PROBLEM_SCHEMA));
        responses.put("ValidationProblem", response(400, VALIDATION_SCHEMA));
        responses.put("UnauthorizedProblem", response(401, PROBLEM_SCHEMA));
        responses.put("ForbiddenProblem", response(403, PROBLEM_SCHEMA));
        responses.put("NotFoundProblem", response(404, PROBLEM_SCHEMA));
        responses.put("MethodNotAllowedProblem", response(405, PROBLEM_SCHEMA));
        responses.put("NotAcceptableProblem", response(406, PROBLEM_SCHEMA));
        responses.put("ConflictProblem", response(409, PROBLEM_SCHEMA));
        responses.put("UnsupportedMediaTypeProblem", response(415, PROBLEM_SCHEMA));
        responses.put("UnprocessableEntityProblem", response(422, PROBLEM_SCHEMA));
        responses.put("InternalServerProblem", response(500, PROBLEM_SCHEMA));
        return responses;
    }

    private static ExpectedResponse response(int status, String schema) {
        return new ExpectedResponse(status, schema);
    }

    private record ExpectedResponse(int status, String schemaReference) {
    }
}
