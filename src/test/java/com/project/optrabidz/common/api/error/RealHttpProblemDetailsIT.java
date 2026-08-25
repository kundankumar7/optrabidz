package com.project.optrabidz.common.api.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.testsupport.RealHttpIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RealHttpProblemDetailsIT extends RealHttpIntegrationTestSupport {
    private static final String PASSWORD = "Password01";

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
}
