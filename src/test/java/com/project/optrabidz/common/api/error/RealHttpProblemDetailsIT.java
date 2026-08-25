package com.project.optrabidz.common.api.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.testsupport.RealHttpIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RealHttpProblemDetailsIT.FaultProbeConfiguration.class)
class RealHttpProblemDetailsIT extends RealHttpIntegrationTestSupport {
    private static final String PASSWORD = "Password01";
    private static final String FAULT_PATH =
            "/api/v1/notifications/__test/problem-details-fault";
    private static final String FAULT_SENTINEL =
            "kan-42-password=secret jdbc:postgresql://private-host";

    @Test
    void registrationLoginAndMeUseARealPortAndCookieStore() throws Exception {
        RealHttpClient client = newClient();
        String email = uniqueEmail("real-http-success");

        HttpResponse<String> registration = register(client, email);
        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(readJson(registration).path("success").asBoolean()).isTrue();

        HttpResponse<String> login = login(client, email);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(client.requiredCookie("JSESSIONID")).isNotBlank();

        HttpResponse<String> me = client.get("/api/v1/me", Map.of());
        JsonNode meBody = readJson(me);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(meBody.path("success").asBoolean()).isTrue();
        assertThat(meBody.path("data").path("role").asText())
                .isEqualTo("STARTUP");
        assertThat(client.requiredCookie("XSRF-TOKEN")).isNotBlank();
    }

    @Test
    void invalidRegistrationUsesValidationProblemDetails() throws Exception {
        RealHttpClient client = newClient();
        String requestId = "kan-42-validation";
        String rejectedEmail = "not-an-email";

        HttpResponse<String> response = client.post(
                "/api/v1/auth/register",
                Map.of(
                        "email", rejectedEmail,
                        "password", PASSWORD,
                        "role", "STARTUP"
                ),
                Map.of("X-Request-Id", requestId)
        );

        JsonNode body = assertProblem(
                response,
                400,
                "VALIDATION_ERROR",
                requestId,
                rejectedEmail
        );
        assertThat(body.path("violations").isArray()).isTrue();
    }

    @Test
    void anonymousProtectedRequestUsesAuthenticationProblemDetails()
            throws Exception {
        RealHttpClient client = newClient();
        String requestId = "kan-42-authentication";
        String bearerSecret = "real-http-secret-token";

        HttpResponse<String> response = client.get(
                "/api/v1/me",
                Map.of(
                        "X-Request-Id", requestId,
                        "Authorization", "Bearer " + bearerSecret
                )
        );

        assertProblem(
                response,
                401,
                "AUTHENTICATION_REQUIRED",
                requestId,
                bearerSecret,
                "Authorization"
        );
    }

    @Test
    void missingCsrfHeaderUsesCsrfProblemDetails() throws Exception {
        RealHttpClient client = newClient();
        String email = uniqueEmail("real-http-csrf");
        assertThat(register(client, email).statusCode()).isEqualTo(201);
        assertThat(login(client, email).statusCode()).isEqualTo(200);
        assertThat(client.get("/api/v1/me", Map.of()).statusCode())
                .isEqualTo(200);
        String csrfSecret = client.requiredCookie("XSRF-TOKEN");
        String requestId = "kan-42-csrf";

        HttpResponse<String> response = client.postWithoutBody(
                "/api/v1/auth/logout",
                Map.of("X-Request-Id", requestId)
        );

        assertProblem(
                response,
                403,
                "CSRF_VALIDATION_FAILED",
                requestId,
                csrfSecret
        );

        HttpResponse<String> successfulLogout = client.postWithoutBody(
                "/api/v1/auth/logout",
                Map.of("X-CSRF-TOKEN", csrfSecret)
        );
        assertThat(successfulLogout.statusCode()).isEqualTo(200);
        assertThat(readJson(successfulLogout).path("success").asBoolean())
                .isTrue();
    }

    @Test
    void missingListingUsesApplicationProblemDetails() throws Exception {
        RealHttpClient client = newClient();
        String requestId = "kan-42-listing-not-found";

        HttpResponse<String> response = client.get(
                "/api/v1/funding-listings/" + Long.MAX_VALUE,
                Map.of("X-Request-Id", requestId)
        );

        assertProblem(
                response,
                404,
                "LISTING_NOT_FOUND",
                requestId,
                Long.toString(Long.MAX_VALUE),
                "MARKETPLACE.LISTING.NOT.FOUND"
        );
    }

    @Test
    void duplicateRegistrationUsesConflictProblemDetails() throws Exception {
        RealHttpClient client = newClient();
        String email = uniqueEmail("real-http-conflict");
        assertThat(register(client, email).statusCode()).isEqualTo(201);
        String requestId = "kan-42-conflict";

        HttpResponse<String> response = client.post(
                "/api/v1/auth/register",
                Map.of(
                        "email", email,
                        "password", PASSWORD,
                        "role", "STARTUP"
                ),
                Map.of("X-Request-Id", requestId)
        );

        assertProblem(
                response,
                409,
                "EMAIL_ALREADY_REGISTERED",
                requestId,
                email
        );
    }

    @Test
    void unexpectedFailureIsSanitizedAcrossTheRealHttpBoundary()
            throws Exception {
        RealHttpClient client = newClient();
        String email = uniqueEmail("real-http-fault");
        assertThat(register(client, email).statusCode()).isEqualTo(201);
        assertThat(login(client, email).statusCode()).isEqualTo(200);
        String requestId = "kan-42-internal-server-error";

        HttpResponse<String> response = client.get(
                FAULT_PATH,
                Map.of("X-Request-Id", requestId)
        );

        JsonNode body = assertProblem(
                response,
                500,
                "INTERNAL_SERVER_ERROR",
                requestId,
                FAULT_SENTINEL,
                "RuntimeException",
                "private-host",
                "password=secret"
        );
        assertThat(body.path("title").asText())
                .isEqualTo("Internal server error");
        assertThat(body.path("detail").asText())
                .isEqualTo("An unexpected error occurred");
        assertThat(body.has("violations")).isFalse();
    }

    @Test
    void faultProbeRetainsTheProductionAuthenticationBoundary()
            throws Exception {
        String requestId = "kan-42-fault-auth";
        assertProblem(
                newClient().get(
                        FAULT_PATH,
                        Map.of("X-Request-Id", requestId)
                ),
                401,
                "AUTHENTICATION_REQUIRED",
                requestId
        );
    }

    private HttpResponse<String> register(RealHttpClient client, String email)
            throws Exception {
        return client.post("/api/v1/auth/register", Map.of(
                "email", email,
                "password", PASSWORD,
                "role", "STARTUP"
        ), Map.of());
    }

    private HttpResponse<String> login(RealHttpClient client, String email)
            throws Exception {
        return client.post("/api/v1/auth/login", Map.of(
                "email", email,
                "password", PASSWORD
        ), Map.of());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private JsonNode assertProblem(
            HttpResponse<String> response,
            int status,
            String code,
            String requestId,
            String... excludedValues
    ) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(value -> assertThat(value)
                        .startsWith("application/problem+json"));
        assertThat(response.headers().firstValue("X-Request-Id"))
                .contains(requestId);

        JsonNode body = readJson(response);
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asText()).isEqualTo(code);
        assertThat(body.path("requestId").asText()).isEqualTo(requestId);
        assertThat(body.path("instance").asText())
                .isEqualTo("urn:optrabidz:request:" + requestId);
        assertThat(body.has("success")).isFalse();
        assertThat(body.has("error")).isFalse();
        for (String excludedValue : excludedValues) {
            assertThat(response.body()).doesNotContain(excludedValue);
        }
        return body;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FaultProbeConfiguration {
        @Bean
        FaultProbeController faultProbeController() {
            return new FaultProbeController();
        }
    }

    @RestController
    static final class FaultProbeController {
        @GetMapping(FAULT_PATH)
        void fail() {
            throw new RuntimeException(FAULT_SENTINEL);
        }
    }
}
