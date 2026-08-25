# KAN-21: RFC 9457 REST Error Adapter Implementation Plan

**Goal:** Translate `ApplicationException` into a safe, consistent RFC 9457
Problem Details response at the HTTP boundary without changing legacy exception
responses.

**Source:** KAN-21 and
`docs/error-handling/work-items/KAN-17-foundation/design.md`, sections 6.2, 7,
8, 11, 13, and 14.

**Architecture:** `HttpErrorMapping` owns the exhaustive neutral-category to
HTTP mapping. `ProblemDetailsFactory` is the only component that creates the
public body and uses only allowlisted descriptor data plus request metadata.
The ordered `RestExceptionHandler` handles `ApplicationException` before the
legacy catch-all advice; all other exceptions continue through the legacy
handler until their later migration stories.

**Technology:** Java 21, Spring Boot 3.3.2, Spring Framework `ProblemDetail`,
JUnit 5, AssertJ, MockMvc, ArchUnit, Testcontainers, and PostgreSQL 16.

## Global constraints

- Work on `feature/KAN-21-rfc9457-error-adapter`, based on verified `develop`
  commit `68318d03ea84b19cd5c3f1befaf65131f67bc5ae`.
- `main` is not modified; the pull request targets `develop`.
- KAN-21 handles `ApplicationException` only. Validation, Spring MVC framework
  failures, Spring Security failures, worker failures, and module migrations
  remain separate stories.
- `ApiException`, `ErrorCode`, `ErrorResponse`, and `GlobalExceptionHandler`
  remain unchanged in production code.
- Public response construction never reads `ApplicationException#getMessage`,
  `diagnosticCode()`, `getCause()`, stack traces, request bodies, rejected
  values, or infrastructure/provider details.
- The Problem Details extension allowlist is exactly `code`, `requestId`, and
  `timestamp` for this story. `violations` belongs to the validation story.
- `instance` is `urn:optrabidz:request:<requestId>`; it is not a raw URL,
  request path, database identifier, or business identifier.
- `X-Request-Id`, the response-body `requestId`, and the request identifier used
  by MDC must represent the same validated value.
- No database migration, dependency addition, runtime property, security
  configuration, logging policy, audit record, or public success response is
  changed.
- Every implementation task follows RED, minimal GREEN, refactor, focused
  verification, and an independently reviewable commit.

## File map

| File | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/common/api/error/HttpErrorMapping.java` | Exhaustive category-to-status and title mapping. |
| `src/main/java/com/project/optrabidz/common/api/error/ProblemDetailsFactory.java` | Sole construction point for allowlisted public Problem Details. |
| `src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java` | Highest-precedence MVC advice for `ApplicationException` only. |
| `src/test/java/com/project/optrabidz/common/api/error/HttpErrorMappingTest.java` | Parameterized mapping completeness and null-contract tests. |
| `src/test/java/com/project/optrabidz/common/api/error/ProblemDetailsFactoryTest.java` | Exact body, correlation, timestamp, type URI, and disclosure tests. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java` | MockMvc serialization, content type, request ID, advice precedence, and legacy-coexistence tests. |
| `docs/error-handling/work-items/KAN-21-rest-adapter/implementation-plan.md` | Approved scope, execution checklist, and verification evidence. |

---

## Task 1: Implement the exhaustive HTTP mapping

**Produces:**

```java
public record HttpErrorMapping(HttpStatus status, String title) {
    public static HttpErrorMapping forCategory(ErrorCategory category);
}
```

### Steps

- [x] Confirm the branch, clean baseline, and exact parent.

  ```powershell
  git branch --show-current
  git status --short
  git rev-parse HEAD
  git rev-parse origin/develop
  ```

  Expected: `feature/KAN-21-rfc9457-error-adapter`; the only planned change is
  this plan; both revisions are
  `68318d03ea84b19cd5c3f1befaf65131f67bc5ae` before implementation commits.

- [x] Create
  `src/test/java/com/project/optrabidz/common/api/error/HttpErrorMappingTest.java`
  first.

  ```java
  package com.project.optrabidz.common.api.error;

  import com.project.optrabidz.common.error.ErrorCategory;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.params.ParameterizedTest;
  import org.junit.jupiter.params.provider.Arguments;
  import org.junit.jupiter.params.provider.MethodSource;
  import org.springframework.http.HttpStatus;

  import java.util.stream.Stream;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatNullPointerException;

  class HttpErrorMappingTest {
      @ParameterizedTest
      @MethodSource("categoryMappings")
      void mapsEveryNeutralCategory(
              ErrorCategory category,
              HttpStatus status,
              String title
      ) {
          HttpErrorMapping mapping = HttpErrorMapping.forCategory(category);

          assertThat(mapping.status()).isEqualTo(status);
          assertThat(mapping.title()).isEqualTo(title);
      }

      @Test
      void rejectsMissingCategory() {
          assertThatNullPointerException()
                  .isThrownBy(() -> HttpErrorMapping.forCategory(null));
      }

      private static Stream<Arguments> categoryMappings() {
          return Stream.of(
                  Arguments.of(ErrorCategory.VALIDATION,
                          HttpStatus.BAD_REQUEST, "Request validation failed"),
                  Arguments.of(ErrorCategory.AUTHENTICATION,
                          HttpStatus.UNAUTHORIZED, "Authentication required"),
                  Arguments.of(ErrorCategory.AUTHORIZATION,
                          HttpStatus.FORBIDDEN, "Access denied"),
                  Arguments.of(ErrorCategory.NOT_FOUND,
                          HttpStatus.NOT_FOUND, "Resource not found"),
                  Arguments.of(ErrorCategory.CONFLICT,
                          HttpStatus.CONFLICT, "Request conflict"),
                  Arguments.of(ErrorCategory.BUSINESS_RULE,
                          HttpStatus.UNPROCESSABLE_ENTITY,
                          "Business rule violation"),
                  Arguments.of(ErrorCategory.INTERNAL,
                          HttpStatus.INTERNAL_SERVER_ERROR,
                          "Internal server error")
          );
      }
  }
  ```

- [x] Run the focused test and preserve meaningful RED evidence.

  ```powershell
  .\mvnw.cmd -B "-Dtest=HttpErrorMappingTest" test
  ```

  Expected: FAIL because `HttpErrorMapping` does not exist. Test discovery or
  environment failure is not acceptable RED evidence.

- [x] Create
  `src/main/java/com/project/optrabidz/common/api/error/HttpErrorMapping.java`.

  ```java
  package com.project.optrabidz.common.api.error;

  import com.project.optrabidz.common.error.ErrorCategory;
  import org.springframework.http.HttpStatus;

  import java.util.Objects;

  public record HttpErrorMapping(HttpStatus status, String title) {
      public HttpErrorMapping {
          Objects.requireNonNull(status, "status must not be null");
          if (title == null || title.isBlank()) {
              throw new IllegalArgumentException("title must not be blank");
          }
          title = title.strip();
      }

      public static HttpErrorMapping forCategory(ErrorCategory category) {
          return switch (Objects.requireNonNull(
                  category,
                  "category must not be null"
          )) {
              case VALIDATION -> new HttpErrorMapping(
                      HttpStatus.BAD_REQUEST,
                      "Request validation failed"
              );
              case AUTHENTICATION -> new HttpErrorMapping(
                      HttpStatus.UNAUTHORIZED,
                      "Authentication required"
              );
              case AUTHORIZATION -> new HttpErrorMapping(
                      HttpStatus.FORBIDDEN,
                      "Access denied"
              );
              case NOT_FOUND -> new HttpErrorMapping(
                      HttpStatus.NOT_FOUND,
                      "Resource not found"
              );
              case CONFLICT -> new HttpErrorMapping(
                      HttpStatus.CONFLICT,
                      "Request conflict"
              );
              case BUSINESS_RULE -> new HttpErrorMapping(
                      HttpStatus.UNPROCESSABLE_ENTITY,
                      "Business rule violation"
              );
              case INTERNAL -> new HttpErrorMapping(
                      HttpStatus.INTERNAL_SERVER_ERROR,
                      "Internal server error"
              );
          };
      }
  }
  ```

- [x] Run the focused test and architecture guardrail.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=HttpErrorMappingTest,ExceptionArchitectureTest" test
  ```

  Expected: PASS. The existing `common.error` package remains framework-free;
  HTTP types exist only in the REST adapter.

- [x] Commit the independently reviewable mapping.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/error/HttpErrorMapping.java `
    src/test/java/com/project/optrabidz/common/api/error/HttpErrorMappingTest.java
  git commit -m "feat: map neutral errors to HTTP semantics (KAN-21)"
  ```

---

## Task 2: Build the allowlisted Problem Details factory

**Consumes:** `ApplicationException`, `ErrorDescriptor`, `HttpErrorMapping`,
`RequestIdProvider`, and `HttpServletRequest`.

**Produces:**

```java
public ProblemDetail create(
        ApplicationException exception,
        HttpErrorMapping mapping,
        HttpServletRequest request
);
```

### Steps

- [x] Create
  `src/test/java/com/project/optrabidz/common/api/error/ProblemDetailsFactoryTest.java`
  with a fixed UTC clock.

  ```java
  package com.project.optrabidz.common.api.error;

  import com.project.optrabidz.common.error.ApplicationException;
  import com.project.optrabidz.common.error.ErrorCategory;
  import com.project.optrabidz.common.error.ErrorDescriptor;
  import org.junit.jupiter.api.Test;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ProblemDetail;
  import org.springframework.mock.web.MockHttpServletRequest;

  import java.net.URI;
  import java.time.Clock;
  import java.time.Instant;
  import java.time.ZoneOffset;

  import static org.assertj.core.api.Assertions.assertThat;

  class ProblemDetailsFactoryTest {
      private static final Instant NOW =
              Instant.parse("2026-08-15T04:00:00Z");
      private static final ErrorDescriptor DESCRIPTOR = new ErrorDescriptor(
              "LISTING_NOT_FOUND",
              ErrorCategory.NOT_FOUND,
              "The requested funding listing is unavailable"
      );

      private final ProblemDetailsFactory factory = new ProblemDetailsFactory(
              Clock.fixed(NOW, ZoneOffset.UTC)
      );

      @Test
      void createsTheApprovedPublicContract() {
          MockHttpServletRequest request = new MockHttpServletRequest(
                  "GET",
                  "/api/v1/listings/42"
          );
          request.addHeader("X-Request-Id", "request-123");
          ApplicationException exception = new ApplicationException(
                  DESCRIPTOR,
                  "MARKETPLACE.LISTING_LOOKUP_FAILED",
                  "Database row 42 was absent"
          );

          ProblemDetail problem = factory.create(
                  exception,
                  HttpErrorMapping.forCategory(ErrorCategory.NOT_FOUND),
                  request
          );

          assertThat(problem.getType()).isEqualTo(
                  URI.create("urn:optrabidz:problem:listing-not-found")
          );
          assertThat(problem.getTitle()).isEqualTo("Resource not found");
          assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
          assertThat(problem.getDetail()).isEqualTo(
                  "The requested funding listing is unavailable"
          );
          assertThat(problem.getInstance()).isEqualTo(
                  URI.create("urn:optrabidz:request:request-123")
          );
          assertThat(problem.getProperties())
                  .containsOnlyKeys("code", "requestId", "timestamp")
                  .containsEntry("code", "LISTING_NOT_FOUND")
                  .containsEntry("requestId", "request-123")
                .containsEntry("timestamp", NOW.toString());
      }

      @Test
      void neverCopiesProtectedDiagnosticsIntoTheProblem() {
          String secret = "password=hunter2 table=credential";
          MockHttpServletRequest request = new MockHttpServletRequest();
          ApplicationException exception = new ApplicationException(
                  DESCRIPTOR,
                  "DATABASE.POSTGRES_CONSTRAINT",
                  secret,
                  new IllegalStateException("jdbc:postgresql://internal-host")
          );

          ProblemDetail problem = factory.create(
                  exception,
                  HttpErrorMapping.forCategory(ErrorCategory.NOT_FOUND),
                  request
          );

          assertThat(problem.toString())
                  .doesNotContain(secret)
                  .doesNotContain(exception.diagnosticCode())
                  .doesNotContain("internal-host")
                  .doesNotContain("IllegalStateException");
      }
  }
  ```

- [x] Run the focused test and preserve RED caused by the missing factory.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ProblemDetailsFactoryTest" test
  ```

  Expected: FAIL because `ProblemDetailsFactory` does not exist.

- [x] Create
  `src/main/java/com/project/optrabidz/common/api/error/ProblemDetailsFactory.java`.

  ```java
  package com.project.optrabidz.common.api.error;

  import com.project.optrabidz.common.error.ApplicationException;
  import com.project.optrabidz.common.error.ErrorDescriptor;
  import com.project.optrabidz.common.observability.RequestIdProvider;
  import jakarta.servlet.http.HttpServletRequest;
  import org.springframework.http.ProblemDetail;
  import org.springframework.stereotype.Component;

  import java.net.URI;
  import java.time.Clock;
  import java.util.Locale;
  import java.util.Objects;

  @Component
  public final class ProblemDetailsFactory {
      private final Clock clock;

      public ProblemDetailsFactory() {
          this(Clock.systemUTC());
      }

      ProblemDetailsFactory(Clock clock) {
          this.clock = Objects.requireNonNull(clock, "clock must not be null");
      }

      public ProblemDetail create(
              ApplicationException exception,
              HttpErrorMapping mapping,
              HttpServletRequest request
      ) {
          Objects.requireNonNull(exception, "exception must not be null");
          Objects.requireNonNull(mapping, "mapping must not be null");

          ErrorDescriptor descriptor = exception.descriptor();
          String requestId = RequestIdProvider.resolveOrCreate(request);
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                  mapping.status(),
                  descriptor.publicMessage()
          );

          problem.setType(URI.create(
                  "urn:optrabidz:problem:" + toProblemSlug(descriptor.code())
          ));
          problem.setTitle(mapping.title());
          problem.setInstance(URI.create(
                  "urn:optrabidz:request:" + requestId
          ));
          problem.setProperty("code", descriptor.code());
          problem.setProperty("requestId", requestId);
          problem.setProperty("timestamp", clock.instant().toString());
          return problem;
      }

      private String toProblemSlug(String code) {
          return code.toLowerCase(Locale.ROOT).replace('_', '-');
      }
  }
  ```

- [x] Run the mapping, factory, neutral-contract, and architecture tests.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=HttpErrorMappingTest,ProblemDetailsFactoryTest,ApplicationExceptionTest,ExceptionArchitectureTest" test
  ```

  Expected: PASS. Review `ProblemDetailsFactory` to confirm no protected
  exception accessor is used except `descriptor()`.

- [x] Commit the public response factory.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/error/ProblemDetailsFactory.java `
    src/test/java/com/project/optrabidz/common/api/error/ProblemDetailsFactoryTest.java
  git commit -m "feat: create allowlisted problem details (KAN-21)"
  ```

---

## Task 3: Route application exceptions without disturbing legacy errors

**Consumes:** `ProblemDetailsFactory` and
`HttpErrorMapping.forCategory(ErrorCategory)`.

**Produces:** A highest-precedence controller advice that handles only
`ApplicationException` and returns `ResponseEntity<ProblemDetail>` with
`application/problem+json`.

### Steps

- [x] Create
  `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java`
  with a test-only probe controller. Configure standalone MockMvc with both the
  new advice and the real legacy `GlobalExceptionHandler`, plus the real
  `RequestMetadataFilter`.

  ```java
  package com.project.optrabidz.common.api.error;

  import com.fasterxml.jackson.databind.JsonNode;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import com.project.optrabidz.common.api.exception.GlobalExceptionHandler;
  import com.project.optrabidz.common.api.response.RequestMetadataFilter;
  import com.project.optrabidz.common.error.ApplicationException;
  import com.project.optrabidz.common.error.ErrorCategory;
  import com.project.optrabidz.common.error.ErrorDescriptor;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.springframework.http.MediaType;
  import org.springframework.test.web.servlet.MockMvc;
  import org.springframework.test.web.servlet.MvcResult;
  import org.springframework.test.web.servlet.setup.MockMvcBuilders;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.RestController;

  import java.time.Clock;
  import java.time.Instant;
  import java.time.ZoneOffset;
  import java.util.UUID;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatCode;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  class RestExceptionHandlerTest {
      private static final String PUBLIC_MESSAGE =
              "The requested funding listing is unavailable";
      private static final String INTERNAL_MESSAGE =
              "password=hunter2 table=credential row=42";

      private MockMvc mockMvc;
      private ObjectMapper objectMapper;

      @BeforeEach
      void setUp() {
          ProblemDetailsFactory factory = new ProblemDetailsFactory(
                  Clock.fixed(
                          Instant.parse("2026-08-15T04:00:00Z"),
                          ZoneOffset.UTC
                  )
          );
          mockMvc = MockMvcBuilders
                  .standaloneSetup(new FailureProbeController())
                  .setControllerAdvice(
                          new GlobalExceptionHandler(),
                          new RestExceptionHandler(factory)
                  )
                  .addFilters(new RequestMetadataFilter())
                  .build();
          objectMapper = new ObjectMapper();
      }

      @Test
      void rendersApplicationExceptionAsProblemDetails() throws Exception {
          mockMvc.perform(get("/test/application-error")
                          .header("X-Request-Id", "request-123"))
                  .andExpect(status().isNotFound())
                  .andExpect(content().contentTypeCompatibleWith(
                          MediaType.APPLICATION_PROBLEM_JSON
                  ))
                  .andExpect(header().string("X-Request-Id", "request-123"))
                  .andExpect(jsonPath("$.type").value(
                          "urn:optrabidz:problem:listing-not-found"
                  ))
                  .andExpect(jsonPath("$.title").value("Resource not found"))
                  .andExpect(jsonPath("$.status").value(404))
                  .andExpect(jsonPath("$.detail").value(PUBLIC_MESSAGE))
                  .andExpect(jsonPath("$.instance").value(
                          "urn:optrabidz:request:request-123"
                  ))
                  .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"))
                  .andExpect(jsonPath("$.requestId").value("request-123"))
                  .andExpect(jsonPath("$.timestamp").value(
                          "2026-08-15T04:00:00Z"
                  ))
                  .andExpect(jsonPath("$.diagnosticCode").doesNotExist())
                  .andExpect(jsonPath("$.violations").doesNotExist())
                  .andExpect(content().string(
                          org.hamcrest.Matchers.not(
                                  org.hamcrest.Matchers.containsString(
                                          INTERNAL_MESSAGE
                                  )
                          )
                  ));
      }

      @Test
      void replacesInvalidRequestIdAndKeepsHeaderAndBodyEqual()
              throws Exception {
          MvcResult result = mockMvc.perform(get("/test/application-error")
                          .header("X-Request-Id", "invalid request id!"))
                  .andExpect(status().isNotFound())
                  .andReturn();

          String headerRequestId = result.getResponse()
                  .getHeader("X-Request-Id");
          JsonNode body = objectMapper.readTree(
                  result.getResponse().getContentAsString()
          );

          assertThat(headerRequestId).isNotBlank();
          assertThatCode(() -> UUID.fromString(headerRequestId))
                  .doesNotThrowAnyException();
          assertThat(body.path("requestId").asText())
                  .isEqualTo(headerRequestId);
          assertThat(body.path("instance").asText())
                  .isEqualTo("urn:optrabidz:request:" + headerRequestId);
      }

      @Test
      void leavesLegacyExceptionsOnTheLegacyEnvelope() throws Exception {
          mockMvc.perform(get("/test/legacy-error")
                          .header("X-Request-Id", "legacy-123"))
                  .andExpect(status().isBadRequest())
                  .andExpect(content().contentTypeCompatibleWith(
                          MediaType.APPLICATION_JSON
                  ))
                  .andExpect(jsonPath("$.success").value(false))
                  .andExpect(jsonPath("$.error.code").value(
                          "VALIDATION_ERROR"
                  ))
                  .andExpect(jsonPath("$.error.message").value(
                          "legacy request rejected"
                  ))
                  .andExpect(jsonPath("$.type").doesNotExist());
      }

      @RestController
      static final class FailureProbeController {
          private static final ErrorDescriptor LISTING_NOT_FOUND =
                  new ErrorDescriptor(
                          "LISTING_NOT_FOUND",
                          ErrorCategory.NOT_FOUND,
                          PUBLIC_MESSAGE
                  );

          @GetMapping("/test/application-error")
          void applicationError() {
              throw new ApplicationException(
                      LISTING_NOT_FOUND,
                      "MARKETPLACE.LISTING_LOOKUP_FAILED",
                      INTERNAL_MESSAGE,
                      new IllegalStateException(
                              "jdbc:postgresql://internal-host"
                      )
              );
          }

          @GetMapping("/test/legacy-error")
          void legacyError() {
              throw new IllegalArgumentException("legacy request rejected");
          }
      }
  }
  ```

- [x] Run the focused test and preserve RED caused by the missing new advice.

  ```powershell
  .\mvnw.cmd -B "-Dtest=RestExceptionHandlerTest" test
  ```

  Expected: FAIL because `RestExceptionHandler` does not exist. After the class
  exists, removing its explicit order must make the application-error scenario
  fail against the legacy catch-all; record that mutation as causal evidence.

- [x] Create
  `src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java`.

  ```java
  package com.project.optrabidz.common.api.error;

  import com.project.optrabidz.common.error.ApplicationException;
  import jakarta.servlet.http.HttpServletRequest;
  import org.springframework.core.Ordered;
  import org.springframework.core.annotation.Order;
  import org.springframework.http.MediaType;
  import org.springframework.http.ProblemDetail;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.ExceptionHandler;
  import org.springframework.web.bind.annotation.RestControllerAdvice;

  import java.util.Objects;

  @Order(Ordered.HIGHEST_PRECEDENCE)
  @RestControllerAdvice
  public final class RestExceptionHandler {
      private final ProblemDetailsFactory problemDetailsFactory;

      public RestExceptionHandler(
              ProblemDetailsFactory problemDetailsFactory
      ) {
          this.problemDetailsFactory = Objects.requireNonNull(
                  problemDetailsFactory,
                  "problemDetailsFactory must not be null"
          );
      }

      @ExceptionHandler(ApplicationException.class)
      public ResponseEntity<ProblemDetail> handleApplicationException(
              ApplicationException exception,
              HttpServletRequest request
      ) {
          HttpErrorMapping mapping = HttpErrorMapping.forCategory(
                  exception.descriptor().category()
          );
          ProblemDetail problem = problemDetailsFactory.create(
                  exception,
                  mapping,
                  request
          );

          return ResponseEntity.status(mapping.status())
                  .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                  .body(problem);
      }
  }
  ```

- [x] Run the complete focused KAN-21 set.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=HttpErrorMappingTest,ProblemDetailsFactoryTest,RestExceptionHandlerTest,ApplicationExceptionTest,ExceptionArchitectureTest" test
  ```

  Expected: PASS. Confirm the new test-only endpoints exist only under
  `src/test` and that the application handler contains no catch-all mapping.

- [x] Commit the ordered REST adapter and its contract tests.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java `
    src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java
  git commit -m "feat: render application errors as problem details (KAN-21)"
  ```

---

## Task 4: Verify the full change and prepare the review

**Produces:** Reproducible unit, architecture, MVC, PostgreSQL integration, and
diff evidence for the exact branch head.

### Steps

- [x] Run the complete unit suite from the Maven wrapper.

  ```powershell
  .\mvnw.cmd -B test
  ```

  Expected: all unit, parameterized, MockMvc, and ArchUnit tests pass with zero
  failures and zero errors.

- [x] With Docker Engine running, run the complete PostgreSQL integration
  suite.

  ```powershell
  .\mvnw.cmd -B verify -Pintegration-tests
  ```

  Expected: Flyway migrates a clean PostgreSQL 16 Testcontainers database and
  all integration tests pass with zero failures and zero errors.

- [x] Prove the change remains inside KAN-21.

  ```powershell
  git status --short
  git diff --check origin/develop...HEAD
  git diff --name-status origin/develop...HEAD
  git diff origin/develop...HEAD -- `
    pom.xml `
    src/main/resources `
    src/main/resources/db/migration `
    src/main/java/com/project/optrabidz/common/api/exception `
    src/main/java/com/project/optrabidz/common/error
  ```

  Expected: no whitespace errors; no change to Maven dependencies, runtime
  configuration, Flyway migrations, the legacy handler/types, or the neutral
  contract. The feature diff contains only the three adapter classes, three
  focused tests, and this plan.

- [x] Record in this plan the executed commands, exact test counts, results,
  branch head SHA, and any approved deviations. Mark a checkbox complete only
  after its evidence exists.

- [x] Commit the reviewed plan and verification evidence.

  ```powershell
  git add docs/design/KAN-21-rfc9457-rest-error-adapter-implementation-plan.md
  git commit -m "docs: record KAN-21 verification evidence"
  ```

- [x] Push the feature branch and open a pull request into `develop`.

  ```powershell
  git push -u origin feature/KAN-21-rfc9457-error-adapter
  gh pr create `
    --base develop `
    --head feature/KAN-21-rfc9457-error-adapter `
    --title "KAN-21 Implement RFC 9457 REST error adapter" `
    --body-file .github/pull_request_template.md
  ```

  If the repository has no reusable PR body file, create the PR body directly
  from the verified scope, RED/GREEN evidence, test results, security notes,
  and rollback statement; do not add a project file solely for one PR.

- [x] Verify the remote PR head equals the locally tested head and wait for both
  required CI checks. Make the exact diff and PR available for review before
  merge.

## Execution evidence

- Baseline: local and remote `develop` were
  `68318d03ea84b19cd5c3f1befaf65131f67bc5ae` before implementation.
- Mapping RED: test compilation failed only because `HttpErrorMapping` was
  absent. GREEN: 8 mapping tests and 2 architecture tests passed.
- Factory RED: test compilation failed only because `ProblemDetailsFactory`
  was absent. GREEN: the mapping, factory, neutral-contract, and architecture
  set passed 16 of 16 tests.
- MVC RED: test compilation failed only because `RestExceptionHandler` was
  absent. The first implementation run then revealed a numeric epoch timestamp
  at the JSON boundary. A focused RED proved the factory held an `Instant`; the
  factory now stores the ISO-8601 string required by the public contract.
- Advice-order mutation: removing `@Order` caused both application-error tests
  to return legacy 500 responses instead of 404. Restoring the annotation made
  the complete focused set pass 19 of 19 tests.
- Full unit command: `.\mvnw.cmd -B test` — 87 tests, 0 failures, 0 errors,
  0 skipped, build success.
- Full integration command: `.\mvnw.cmd -B verify -Pintegration-tests` — the
  same 87 unit tests plus 64 PostgreSQL integration tests passed with 0
  failures, 0 errors, and 0 skipped tests; build success.
- Database evidence: Testcontainers used PostgreSQL 16.14 and Flyway validated
  and applied the single unchanged V1 migration to a clean schema.
- Scope evidence: protected Maven, runtime configuration, Flyway, legacy error,
  and neutral error paths have no diff. The packaged JAR contains no test probe.
- Local verification runtime: Java 21.0.11. The required GitHub checks used
  Temurin 21 and passed on the reviewed pull-request head.
- Verified implementation head before this evidence commit:
  `4edf9479e49e198e05da9b851369be5a71544995`.
- No scope deviation was required. ISO timestamp normalization is a wire-format
  correction inside the approved contract.
- Pull request: `https://github.com/kundankumar7/optrabidz/pull/18`, reviewed
  and merged into `develop`.
- GitHub exact-head evidence for `4d3f0183c482a4baeb0001c9c1e5f811471b42f8`:
  both push and pull-request runs passed `Unit Tests` and `PostgreSQL
  Integration Tests` under Temurin 21.
- This final evidence commit changes only this plan. GitHub reruns the required
  checks on the resulting final PR head; the live PR check status is the
  authoritative exact-head record.

## Delivery controls

1. Written Jira specification approved — complete.
2. This implementation plan approved — required before production code.
3. Test-first implementation and verification completed.
4. Pull request reviewed and approved before merge into `develop`.
5. `main` remains unchanged throughout KAN-21.
