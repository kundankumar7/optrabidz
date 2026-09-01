package com.project.optrabidz.documentation.openapi;

import com.project.optrabidz.documentation.error.PublicErrorCatalogue;
import com.project.optrabidz.documentation.error.PublicErrorDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "optrabidz.documentation.api-docs-enabled",
        havingValue = "true"
)
public class OpenApiProblemDetailsConfiguration {

    private static final String PROBLEM_SCHEMA =
            "#/components/schemas/ProblemDetails";
    private static final String VALIDATION_SCHEMA =
            "#/components/schemas/ValidationProblemDetails";
    private static final String REQUEST_ID_HEADER =
            "#/components/headers/RequestIdHeader";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    public OpenApiCustomizer problemDetailsCustomizer() {
        return openApi -> {
            Components components = components(openApi);
            addSchemas(components);
            addHeader(components);
            addResponses(components);
        };
    }

    private static Components components(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        return openApi.getComponents();
    }

    private static void addSchemas(Components components) {
        ObjectSchema problem = new ObjectSchema();
        problem.setRequired(List.of(
                "type",
                "title",
                "status",
                "detail",
                "instance",
                "code",
                "requestId",
                "timestamp"
        ));
        problem.addProperty("type", string("uri"));
        problem.addProperty("title", string(null));
        problem.addProperty("status", statusSchema());
        problem.addProperty("detail", string(null));
        problem.addProperty("instance", string("uri"));
        problem.addProperty("code", codeSchema());
        problem.addProperty("requestId", string(null));
        problem.addProperty("timestamp", string("date-time"));

        ObjectSchema violation = new ObjectSchema();
        violation.setRequired(List.of("field", "message"));
        violation.addProperty("field", string(null));
        violation.addProperty("message", string(null));

        ArraySchema violations = new ArraySchema();
        violations.setItems(reference(
                "#/components/schemas/ValidationViolation"
        ));
        ObjectSchema validationExtension = new ObjectSchema();
        validationExtension.setRequired(List.of("violations"));
        validationExtension.addProperty("violations", violations);

        ComposedSchema validation = new ComposedSchema();
        validation.setAllOf(List.of(
                reference(PROBLEM_SCHEMA),
                validationExtension
        ));

        components.addSchemas("ProblemDetails", problem);
        components.addSchemas("ValidationViolation", violation);
        components.addSchemas("ValidationProblemDetails", validation);
    }

    private static void addHeader(Components components) {
        components.addHeaders(
                "RequestIdHeader",
                new Header()
                        .description("Request correlation identifier")
                        .schema(string(null))
        );
    }

    private static void addResponses(Components components) {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse(
                "BadRequestProblem",
                response("400 Bad Request", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "ValidationProblem",
                response("400 Request Validation", VALIDATION_SCHEMA)
        );
        responses.addApiResponse(
                "UnauthorizedProblem",
                response("401 Unauthorized", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "ForbiddenProblem",
                response("403 Forbidden", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "NotFoundProblem",
                response("404 Not Found", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "MethodNotAllowedProblem",
                response("405 Method Not Allowed", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "NotAcceptableProblem",
                response("406 Not Acceptable", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "ConflictProblem",
                response("409 Conflict", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "UnsupportedMediaTypeProblem",
                response("415 Unsupported Media Type", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "UnprocessableEntityProblem",
                response("422 Unprocessable Entity", PROBLEM_SCHEMA)
        );
        responses.addApiResponse(
                "InternalServerProblem",
                response("500 Internal Server Error", PROBLEM_SCHEMA)
        );
        responses.forEach(components::addResponses);
    }

    private static ApiResponse response(
            String description,
            String schemaReference
    ) {
        MediaType mediaType = new MediaType();
        mediaType.setSchema(reference(schemaReference));
        Content content = new Content();
        content.addMediaType(PROBLEM_JSON, mediaType);
        return new ApiResponse()
                .description(description)
                .content(content)
                .addHeaderObject(
                        "X-Request-Id",
                        new Header().$ref(REQUEST_ID_HEADER)
                );
    }

    private static StringSchema codeSchema() {
        StringSchema code = string(null);
        code.setEnum(
                PublicErrorCatalogue.createDefault().entries().stream()
                        .map(PublicErrorDefinition::code)
                        .toList()
        );
        return code;
    }

    private static IntegerSchema statusSchema() {
        IntegerSchema status = new IntegerSchema();
        status.setFormat("int32");
        status.setMinimum(BigDecimal.valueOf(400));
        status.setMaximum(BigDecimal.valueOf(599));
        return status;
    }

    private static StringSchema string(String format) {
        StringSchema schema = new StringSchema();
        schema.setFormat(format);
        return schema;
    }

    private static Schema<?> reference(String reference) {
        return new Schema<>().$ref(reference);
    }
}
