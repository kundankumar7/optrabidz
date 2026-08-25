package com.project.optrabidz.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.address=127.0.0.1"
)
@ActiveProfiles("test")
public abstract class RealHttpIntegrationTestSupport {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        SharedPostgresContainer.registerProperties(registry);
    }

    protected final RealHttpClient newClient() {
        CookieManager cookies = new CookieManager(
                null,
                CookiePolicy.ACCEPT_ALL
        );
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new RealHttpClient(client, cookies);
    }

    protected final JsonNode readJson(HttpResponse<String> response)
            throws JsonProcessingException {
        return objectMapper.readTree(response.body());
    }

    protected final class RealHttpClient {
        private final HttpClient client;
        private final CookieManager cookies;

        private RealHttpClient(HttpClient client, CookieManager cookies) {
            this.client = client;
            this.cookies = cookies;
        }

        public HttpResponse<String> get(
                String path,
                Map<String, String> headers
        ) throws IOException, InterruptedException {
            return send("GET", path, null, headers);
        }

        public HttpResponse<String> post(
                String path,
                Object body,
                Map<String, String> headers
        ) throws IOException, InterruptedException {
            return send("POST", path, body, headers);
        }

        public HttpResponse<String> postWithoutBody(
                String path,
                Map<String, String> headers
        ) throws IOException, InterruptedException {
            return send("POST", path, null, headers);
        }

        public String requiredCookie(String name) {
            String value = cookies.getCookieStore().getCookies().stream()
                    .filter(cookie -> cookie.getName().equals(name))
                    .map(HttpCookie::getValue)
                    .findFirst()
                    .orElse(null);
            assertThat(value).as("cookie %s", name).isNotBlank();
            return value;
        }

        private HttpResponse<String> send(
                String method,
                String path,
                Object body,
                Map<String, String> headers
        ) throws IOException, InterruptedException {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                    .timeout(REQUEST_TIMEOUT)
                    .header(
                            "Accept",
                            "application/json, application/problem+json"
                    );
            headers.forEach(request::header);

            if (body == null) {
                request.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                request.header("Content-Type", "application/json");
                request.method(method, HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)
                ));
            }

            return client.send(
                    request.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
        }
    }

    private URI uri(String path) {
        assertThat(path).startsWith("/");
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
