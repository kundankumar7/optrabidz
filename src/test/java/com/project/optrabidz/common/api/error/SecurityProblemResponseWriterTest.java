package com.project.optrabidz.common.api.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityProblemResponseWriterTest {
    private static final Instant NOW =
            Instant.parse("2026-08-15T04:00:00Z");

    private final ObjectMapper objectMapper =
            Jackson2ObjectMapperBuilder.json().build();
    private final SecurityProblemResponseWriter writer =
            new SecurityProblemResponseWriter(
                    new ProblemDetailsFactory(
                            Clock.fixed(NOW, ZoneOffset.UTC)
                    ),
                    objectMapper
            );

    @ParameterizedTest
    @MethodSource("securityResponses")
    void writesExactSecurityProblemResponse(
            SecurityProblem problem,
            int status,
            String type,
            String title,
            String detail
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(problem, request, response);

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).isEqualTo(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        JsonNode body = objectMapper.readTree(
                response.getContentAsByteArray()
        );
        assertThat(body.get("type").asText()).isEqualTo(type);
        assertThat(body.get("title").asText()).isEqualTo(title);
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("detail").asText()).isEqualTo(detail);
        assertThat(body.get("instance").asText()).isEqualTo(
                "urn:optrabidz:request:request-123"
        );
        assertThat(body.get("code").asText()).isEqualTo(problem.name());
        assertThat(body.get("requestId").asText()).isEqualTo("request-123");
        assertThat(body.get("timestamp").asText()).isEqualTo(NOW.toString());
        assertThat(body.has("violations")).isFalse();
        assertThat(body.properties())
                .extracting(entry -> entry.getKey())
                .containsExactlyInAnyOrder(
                        "type",
                        "title",
                        "status",
                        "detail",
                        "instance",
                        "code",
                        "requestId",
                        "timestamp"
                );
    }

    @ParameterizedTest
    @MethodSource("securityProblems")
    void neverCopiesUnrelatedRequestData(SecurityProblem problem)
            throws Exception {
        String secret = "secret-csrf-token";
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/internal/" + secret
        );
        request.addHeader("X-Request-Id", "request-123");
        request.addHeader("Authorization", "Bearer " + secret);
        request.setCookies(new Cookie("XSRF-TOKEN", secret));
        request.setAttribute("principalEmail", secret + "@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(problem, request, response);

        assertThat(response.getContentAsString())
                .doesNotContain(secret)
                .doesNotContain("Authorization")
                .doesNotContain("principalEmail");
    }

    private static Stream<Arguments> securityResponses() {
        return Stream.of(
                Arguments.of(
                        SecurityProblem.AUTHENTICATION_REQUIRED,
                        401,
                        "urn:optrabidz:problem:authentication-required",
                        "Authentication required",
                        "Authentication is required"
                ),
                Arguments.of(
                        SecurityProblem.AUTHORIZATION_FAILED,
                        403,
                        "urn:optrabidz:problem:authorization-failed",
                        "Access denied",
                        "You are not authorized to perform this action"
                ),
                Arguments.of(
                        SecurityProblem.CSRF_VALIDATION_FAILED,
                        403,
                        "urn:optrabidz:problem:csrf-validation-failed",
                        "Request security validation failed",
                        "Request security validation failed"
                )
        );
    }

    private static Stream<SecurityProblem> securityProblems() {
        return Stream.of(SecurityProblem.values());
    }
}
