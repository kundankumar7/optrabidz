# KAN-42 Real-Port HTTP Problem Details Smoke Verification Implementation Plan

**Goal:** Verify the approved Problem Details contract through a real random
localhost port while preserving the existing MockMvc suite as the primary
detailed API test layer.

**Architecture:** A dedicated Spring Boot `RANDOM_PORT` integration-test base
uses Java 21 `HttpClient` with an isolated cookie manager. It reuses the shared
PostgreSQL Testcontainer and Flyway path, exercises the production servlet and
security filters, and keeps the deterministic unexpected-failure probe under
`src/test` only.

**Tech stack:** Java 21, Spring Boot 3.3.2, JUnit 5, AssertJ, Jackson,
Testcontainers PostgreSQL 16, Flyway, Maven Surefire/Failsafe

**Spec:** [KAN-42 approved design](design.md)

## Global constraints

- No production Java source, runtime property, dependency, workflow, database
  schema, or Flyway migration changes.
- Keep MockMvc as the detailed API integration layer; this suite samples only
  the approved real-HTTP boundary.
- Bind only a random loopback port and reuse `SharedPostgresContainer`.
- Use Java 21 `HttpClient`; do not introduce `TestRestTemplate` or another HTTP
  client dependency.
- Use isolated cookie state, explicit request timeouts, and
  `HttpClient.Redirect.NEVER`.
- Load the fault probe only from the KAN-42 test context and keep it out of the
  production artifact.
- Preserve all current routes, success payloads, session/CSRF policy, error
  codes, business rules, and anonymous listing access.

---

## File map

| File | Responsibility |
|---|---|
| `src/test/java/com/project/optrabidz/testsupport/SharedPostgresContainer.java` | Own the singleton PostgreSQL container and its dynamic Spring datasource registration |
| `src/test/java/com/project/optrabidz/testsupport/PostgresIntegrationTestSupport.java` | Continue to configure mock-web integration contexts through the shared registration method |
| `src/test/java/com/project/optrabidz/testsupport/RealHttpIntegrationTestSupport.java` | Own random-port URI creation, Java HTTP transport, isolated cookies, JSON serialization, and bounded requests |
| `src/test/java/com/project/optrabidz/common/api/error/RealHttpProblemDetailsIT.java` | Own the representative success, expected-failure, request-correlation, disclosure, and unexpected-failure assertions |
| `docs/error-handling/work-items/KAN-42-real-http-smoke/design.md` | Record delivered status and acceptance evidence after verification |

## Task 1: Real HTTP harness and successful session round trip

**Files:**

- Modify: `src/test/java/com/project/optrabidz/testsupport/SharedPostgresContainer.java`
- Modify: `src/test/java/com/project/optrabidz/testsupport/PostgresIntegrationTestSupport.java`
- Create: `src/test/java/com/project/optrabidz/testsupport/RealHttpIntegrationTestSupport.java`
- Create: `src/test/java/com/project/optrabidz/common/api/error/RealHttpProblemDetailsIT.java`

**Interfaces:**

- `SharedPostgresContainer.registerProperties(DynamicPropertyRegistry)`
  registers the existing singleton's URL, username, password, and driver.
- `RealHttpIntegrationTestSupport.newClient()` returns one isolated
  `RealHttpClient`.
- `RealHttpClient.get(String, Map<String, String>)`,
  `post(String, Object, Map<String, String>)`, and
  `postWithoutBody(String, Map<String, String>)` return
  `HttpResponse<String>` without following redirects.
- `RealHttpClient.requiredCookie(String)` returns a named cookie or fails the
  test with a diagnostic assertion.
- `RealHttpIntegrationTestSupport.readJson(HttpResponse<String>)` parses the
  response with Spring's configured `ObjectMapper`.

- [x] **Step 1: Add the first real-port test before its support exists**

Create `RealHttpProblemDetailsIT` with a successful registration, login, and
`/me` round trip:

```java
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
```

- [x] **Step 2: Run the focused test and capture RED**

Run:

```powershell
.\mvnw.cmd -q '-Dtest=TestingSetupTest' '-Dit.test=RealHttpProblemDetailsIT' '-DskipITs=false' verify
```

Expected: test compilation fails because
`RealHttpIntegrationTestSupport` and `RealHttpClient` do not exist.

- [x] **Step 3: Centralize the existing PostgreSQL property registration**

Add this method to `SharedPostgresContainer`:

```java
static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
            "spring.datasource.driver-class-name",
            POSTGRES::getDriverClassName
    );
}
```

Import `DynamicPropertyRegistry`, then replace the four registrations in
`PostgresIntegrationTestSupport.registerPostgresProperties` with:

```java
SharedPostgresContainer.registerProperties(registry);
```

Remove the now-unused container field and `PostgreSQLContainer` import from
`PostgresIntegrationTestSupport`. Do not change its annotations.

- [x] **Step 4: Implement the real-port transport support**

Create `RealHttpIntegrationTestSupport` with these members and exact transport
defaults:

```java
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
```

- [x] **Step 5: Run focused GREEN and the existing session integration test**

Run:

```powershell
.\mvnw.cmd -q '-Dtest=TestingSetupTest' '-Dit.test=RealHttpProblemDetailsIT,SecurityApiIT' '-DskipITs=false' verify
```

Expected: both integration classes pass; logs show the application binding a
non-zero random port and Flyway validating the PostgreSQL schema.

- [x] **Step 6: Commit the transport slice**

```powershell
git add src/test/java/com/project/optrabidz/testsupport/SharedPostgresContainer.java src/test/java/com/project/optrabidz/testsupport/PostgresIntegrationTestSupport.java src/test/java/com/project/optrabidz/testsupport/RealHttpIntegrationTestSupport.java src/test/java/com/project/optrabidz/common/api/error/RealHttpProblemDetailsIT.java
git commit -m "test(KAN-42): add real HTTP session harness"
```

## Task 2: Expected Problem Details over the wire

**Files:**

- Modify: `src/test/java/com/project/optrabidz/common/api/error/RealHttpProblemDetailsIT.java`

**Interfaces:**

- Consumes the Task 1 `RealHttpClient` methods and `readJson`.
- Produces `assertProblem(...)`, a private test assertion that verifies the
  shared wire-level invariants without moving contract decisions into the
  transport support.

- [x] **Step 1: Add the shared Problem Details assertion**

Add this test-local method:

```java
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
```

- [x] **Step 2: Add 400, 401, 403, 404, and 409 tests**

Add five independent tests using unique request IDs and emails:

```java
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
            response, 400, "VALIDATION_ERROR", requestId, rejectedEmail
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
            response, 401, "AUTHENTICATION_REQUIRED", requestId,
            bearerSecret, "Authorization"
    );
}

@Test
void missingCsrfHeaderUsesCsrfProblemDetails() throws Exception {
    RealHttpClient client = newClient();
    String email = uniqueEmail("real-http-csrf");
    assertThat(register(client, email).statusCode()).isEqualTo(201);
    assertThat(login(client, email).statusCode()).isEqualTo(200);
    assertThat(client.get("/api/v1/me", Map.of()).statusCode()).isEqualTo(200);
    String csrfSecret = client.requiredCookie("XSRF-TOKEN");
    String requestId = "kan-42-csrf";

    HttpResponse<String> response = client.postWithoutBody(
            "/api/v1/auth/logout",
            Map.of("X-Request-Id", requestId)
    );

    assertProblem(
            response, 403, "CSRF_VALIDATION_FAILED", requestId, csrfSecret
    );

    HttpResponse<String> successfulLogout = client.postWithoutBody(
            "/api/v1/auth/logout",
            Map.of("X-CSRF-TOKEN", csrfSecret)
    );
    assertThat(successfulLogout.statusCode()).isEqualTo(200);
    assertThat(readJson(successfulLogout).path("success").asBoolean()).isTrue();
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
            response, 404, "LISTING_NOT_FOUND", requestId,
            Long.toString(Long.MAX_VALUE), "MARKETPLACE.LISTING.NOT.FOUND"
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
            response, 409, "EMAIL_ALREADY_REGISTERED", requestId, email
    );
}
```

- [x] **Step 3: Run the focused suite**

```powershell
.\mvnw.cmd -q '-Dtest=TestingSetupTest' '-Dit.test=RealHttpProblemDetailsIT' '-DskipITs=false' verify
```

Expected: six tests pass: one successful real session plus the five expected
failure contracts. If a real boundary exposes a contract mismatch, preserve
the RED evidence and correct only test transport configuration unless the
approved production contract is genuinely broken.

- [x] **Step 4: Commit the expected-failure slice**

```powershell
git add src/test/java/com/project/optrabidz/common/api/error/RealHttpProblemDetailsIT.java
git commit -m "test(KAN-42): verify expected problems over HTTP"
```

## Task 3: Sanitized unexpected failure over the wire

**Files:**

- Modify: `src/test/java/com/project/optrabidz/common/api/error/RealHttpProblemDetailsIT.java`

**Interfaces:**

- The nested test configuration produces one `FaultProbeController` bean.
- `GET /api/v1/notifications/__test/problem-details-fault` is present only in
  this test context and is authenticated by the unchanged production security
  chain.

- [x] **Step 1: Add the 500 test before registering the probe**

```java
private static final String FAULT_PATH =
        "/api/v1/notifications/__test/problem-details-fault";
private static final String FAULT_SENTINEL =
        "kan-42-password=secret jdbc:postgresql://private-host";

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
            response, 500, "INTERNAL_SERVER_ERROR", requestId,
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
```

- [x] **Step 2: Run RED without the test-only route**

```powershell
.\mvnw.cmd -q '-Dtest=TestingSetupTest' '-Dit.test=RealHttpProblemDetailsIT' '-DskipITs=false' verify
```

Expected: the new test fails with an endpoint-not-found response because the
fault route has not been registered.

- [x] **Step 3: Register the isolated fault controller**

Annotate the test class with `@Import(FaultProbeConfiguration.class)` and add:

```java
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
```

Use imports from `org.springframework.boot.test.context.TestConfiguration`,
`org.springframework.context.annotation.Bean`,
`org.springframework.context.annotation.Import`,
`org.springframework.web.bind.annotation.GetMapping`, and
`org.springframework.web.bind.annotation.RestController`. Do not add the path
to `SecurityConfig`; the existing `/api/v1/notifications/**` matcher must
authenticate it.

- [x] **Step 4: Add and run the anonymous security-boundary check**

Add this separate test:

```java
@Test
void faultProbeRetainsTheProductionAuthenticationBoundary() throws Exception {
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
```

Then run:

```powershell
.\mvnw.cmd -q '-Dtest=TestingSetupTest' '-Dit.test=RealHttpProblemDetailsIT' '-DskipITs=false' verify
```

Expected: all KAN-42 tests pass; the anonymous probe receives 401 and the
authenticated probe receives the fixed sanitized 500.

- [x] **Step 5: Prove the probe cannot enter the production artifact**

```powershell
.\mvnw.cmd -q '-DskipTests' package
jar tf target/optrabidz-0.0.1-SNAPSHOT.jar | Select-String -Pattern "RealHttp|FaultProbe"
```

Expected: the package succeeds and `Select-String` produces no output.

- [x] **Step 6: Commit the unexpected-failure slice**

```powershell
git add src/test/java/com/project/optrabidz/common/api/error/RealHttpProblemDetailsIT.java
git commit -m "test(KAN-42): verify sanitized 500 over HTTP"
```

## Task 4: Complete verification and delivery evidence

**Files:**

- Modify: `docs/error-handling/work-items/KAN-42-real-http-smoke/design.md`

**Interfaces:**

- Consumes all KAN-42 tests and existing repository verification profiles.
- Produces an accurate delivered status and checked acceptance criteria; no
  new runtime interface.

- [x] **Step 1: Run focused test and documentation checks**

```powershell
.\mvnw.cmd -q '-Dtest=TestingSetupTest' '-Dit.test=RealHttpProblemDetailsIT' '-DskipITs=false' verify
.\mvnw.cmd -q '-Dtest=DocumentationLinksTest,DocumentationLinkValidatorTest' test
```

Expected: both commands exit successfully.

- [x] **Step 2: Run the complete unit suite**

```powershell
.\mvnw.cmd -B test
```

Expected: all unit and architecture tests pass with zero failures and errors.

- [x] **Step 3: Run the complete PostgreSQL integration profile**

```powershell
.\mvnw.cmd -B verify -Pintegration-tests
```

Expected: all Failsafe integration tests pass, including
`RealHttpProblemDetailsIT`, with zero failures and errors.

- [x] **Step 4: Run scope and repository checks**

```powershell
git diff --check
git status --short
git diff --name-only origin/develop...HEAD
```

Expected: no whitespace errors; only the approved test-support, KAN-42 test,
and KAN-42 documentation files are present.

- [x] **Step 5: Record exact verification evidence**

Update `design.md` only after the commands pass:

- set status to `Implemented and verified; ready for review.`;
- check only acceptance criteria proven by the final commands;
- record test totals and the exact tested commit; and
- keep future work, OpenAPI, KAN-41, JWT/OAuth2, and deployed smoke testing out
  of the delivered claims.

- [x] **Step 6: Commit the verification record**

```powershell
git add docs/error-handling/work-items/KAN-42-real-http-smoke/design.md
git commit -m "docs(KAN-42): record real HTTP verification evidence"
```

- [ ] **Step 7: Publish for CI and review exact-head checks**

```powershell
git push -u origin HEAD
gh run list --branch docs/KAN-42-real-http-smoke --limit 5
```

Expected: the remote branch points to the local head and both the unit and
PostgreSQL integration workflows pass for that exact commit before the change
is considered ready for merge review.
