# KAN-22: Spring MVC Problem Details Implementation Plan

**Goal:** Standardize deterministic Spring MVC validation and protocol
failures as safe RFC 9457 Problem Details responses without changing business
exceptions, Spring Security failures, or the generic unexpected-error policy.

**Source:** KAN-22 and
`docs/design/KAN-17-exception-handling-foundation-design.md`, especially
sections 6.2, 7, 8, 11, 13, and 14.

**Architecture:** `RestExceptionHandler` will extend Spring MVC's supported
`ResponseEntityExceptionHandler` and explicitly override only the failure
families approved by KAN-22. `FrameworkProblem` will be the allowlist of stable
web failure descriptors, `ValidationViolationMapper` will translate validation
metadata without rejected values, and `ProblemDetailsFactory` will remain the
only component that constructs public response bodies.

**Technology:** Java 21, Spring Boot 3.3.2, Spring Framework 6.1.11, Jakarta
Bean Validation, RFC 9457 `ProblemDetail`, JUnit 5, AssertJ, MockMvc,
Testcontainers, and PostgreSQL 16.

## Global constraints

- Work on `feature/KAN-22-mvc-problem-details`, based on verified `develop`
  commit `cefd0973c430a9be8480f2ce17213a0faac1d086`.
- `main` is not modified; the pull request targets `develop`.
- The required framework mappings are exactly 400 validation, 400 malformed
  request, 404 endpoint not found, 405 method not allowed, 406 not acceptable,
  and 415 unsupported media type.
- Generic unexpected 500 handling, Spring Security adapters, module exception
  migration, scheduled-worker policies, OpenAPI, and real-port smoke tests
  remain separate stories.
- `ProblemDetailsFactory` remains the only production component that creates a
  `ProblemDetail` response body.
- Public responses never contain rejected values, request bodies, raw parser
  messages, exception messages, causes, stack traces, Java types, SQL details,
  credentials, tokens, cookies, authorization headers, or infrastructure data.
- Constraint messages come from a fixed allowlist. Unknown constraint types
  use `is invalid`; raw interpolated validation messages are not copied.
- Nested and indexed field paths are retained only from safe property,
  parameter, and container metadata. Method and class names are excluded.
- Spring-provided response headers are preserved, while the response content
  type is always `application/problem+json` for in-scope failures.
- The existing request ID is reused across the response header, Problem Details
  body, instance URN, and logging context.
- No Maven dependency, Flyway migration, database schema, runtime property,
  security configuration, success response, logging policy, or audit behavior
  changes in this story.
- Every implementation task follows RED, minimal GREEN, refactor, focused
  verification, and an independently reviewable commit.

## Stable framework problem catalogue

| Enum value and public code | Status | Title | Detail |
|---|---:|---|---|
| `VALIDATION_ERROR` | 400 | Request validation failed | One or more request values are invalid |
| `MALFORMED_REQUEST` | 400 | Malformed request | The request body is malformed |
| `ENDPOINT_NOT_FOUND` | 404 | Endpoint not found | The requested endpoint is unavailable |
| `METHOD_NOT_ALLOWED` | 405 | Method not allowed | The HTTP method is not supported for this endpoint |
| `NOT_ACCEPTABLE` | 406 | Response type not acceptable | The requested response media type is not available |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Unsupported media type | The request media type is not supported |

Each `type` is derived from the code as
`urn:optrabidz:problem:<lowercase-hyphenated-code>`.

## File map

| File | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/common/api/error/FrameworkProblem.java` | Fixed allowlist of framework problem codes, HTTP mappings, titles, and public details. |
| `src/main/java/com/project/optrabidz/common/api/error/ValidationViolation.java` | Public `field`/`message` response value with nonblank invariants. |
| `src/main/java/com/project/optrabidz/common/api/error/ValidationViolationMapper.java` | Safe translation and deterministic ordering for binding, method, and constraint violations. |
| `src/main/java/com/project/optrabidz/common/api/error/ProblemDetailsFactory.java` | Shared construction of application and framework Problem Details. |
| `src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java` | Supported Spring MVC interception and response-header preservation. |
| `src/main/java/com/project/optrabidz/common/api/exception/GlobalExceptionHandler.java` | Legacy fallback with only the three migrated overlapping handlers removed. |
| `src/test/java/com/project/optrabidz/common/api/error/FrameworkProblemTest.java` | Catalogue completeness and exact public values. |
| `src/test/java/com/project/optrabidz/common/api/error/ValidationViolationMapperTest.java` | Safe messages, paths, fallback behavior, and ordering. |
| `src/test/java/com/project/optrabidz/common/api/error/ProblemDetailsFactoryTest.java` | Framework body construction, validation extension, and disclosure checks. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerValidationTest.java` | MockMvc DTO, nested, collection, method, missing-input, and type-mismatch contracts. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerFrameworkTest.java` | MockMvc malformed JSON, 404, 405, 406, 415, header, and success regressions. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java` | Existing application-exception, request-ID, precedence, and legacy-coexistence regression suite. |
| `docs/design/KAN-22-mvc-problem-details-implementation-plan.md` | Approved execution checklist and verification evidence. |

---

## Task 1: Define the framework problem catalogue and factory path

**Consumes:** Existing `HttpErrorMapping`, `ProblemDetailsFactory`,
`RequestIdProvider`, and RFC 9457 response contract.

**Produces:**

```java
enum FrameworkProblem {
    VALIDATION_ERROR(...),
    MALFORMED_REQUEST(...),
    ENDPOINT_NOT_FOUND(...),
    METHOD_NOT_ALLOWED(...),
    NOT_ACCEPTABLE(...),
    UNSUPPORTED_MEDIA_TYPE(...);

    String code();
    HttpErrorMapping mapping();
    String detail();
}
```

```java
ProblemDetail createFramework(
        FrameworkProblem problem,
        List<ValidationViolation> violations,
        HttpServletRequest request
);
```

### Steps

- [x] Confirm the clean baseline before creating the feature branch.

  ```powershell
  git branch --show-current
  git status --short
  git rev-parse HEAD
  git rev-parse origin/develop
  ```

  Expected: branch `develop`, clean except for this approved plan, and both
  revisions equal `cefd0973c430a9be8480f2ce17213a0faac1d086`.

- [x] Create the meaningful task branch and confirm `main` is untouched.

  ```powershell
  git switch -c feature/KAN-22-mvc-problem-details
  git rev-parse main
  git rev-parse origin/main
  ```

  Expected: the new branch starts at the verified `develop` commit; local and
  remote `main` remain unchanged.

- [x] Add `FrameworkProblemTest` first as a parameterized exact-contract test.

  ```java
  @ParameterizedTest
  @MethodSource("frameworkProblems")
  void definesStableFrameworkProblem(
          FrameworkProblem problem,
          HttpStatus status,
          String title,
          String detail
  ) {
      assertThat(problem.code()).isEqualTo(problem.name());
      assertThat(problem.mapping().status()).isEqualTo(status);
      assertThat(problem.mapping().title()).isEqualTo(title);
      assertThat(problem.detail()).isEqualTo(detail);
  }
  ```

  The method source contains all six rows from the stable catalogue above and
  asserts `FrameworkProblem.values()` contains exactly those six values.

- [x] Extend `ProblemDetailsFactoryTest` with framework and validation cases.

  ```java
  @Test
  void createsFrameworkProblemWithSafeViolations() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Request-Id", "request-123");

      ProblemDetail problem = factory.createFramework(
              FrameworkProblem.VALIDATION_ERROR,
              List.of(new ValidationViolation(
                      "items[0].amount",
                      "must be greater than zero"
              )),
              request
      );

      assertThat(problem.getStatus()).isEqualTo(400);
      assertThat(problem.getType()).isEqualTo(
              URI.create("urn:optrabidz:problem:validation-error")
      );
      assertThat(problem.getProperties())
              .containsEntry("code", "VALIDATION_ERROR")
              .containsEntry("requestId", "request-123")
              .containsKey("timestamp")
              .containsKey("violations");
  }
  ```

  Add a second test proving a non-validation framework problem has no
  `violations`. For disclosure, place `password=hunter2` in the mock request
  URI and an unrelated request header, create `MALFORMED_REQUEST`, and assert
  `problem.toString()` does not contain that sentinel. The factory API has no
  exception, parser message, or request-body parameter.

- [x] Run focused tests and preserve meaningful RED evidence.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=FrameworkProblemTest,ProblemDetailsFactoryTest" test
  ```

  Expected: compilation fails because `FrameworkProblem`,
  `ValidationViolation`, and `createFramework` do not exist. Environment or
  test-discovery failures are not valid RED evidence.

- [x] Create `ValidationViolation` with trimmed, nonblank components.

  ```java
  public record ValidationViolation(String field, String message) {
      public ValidationViolation {
          if (field == null || field.isBlank()) {
              throw new IllegalArgumentException("field must not be blank");
          }
          if (message == null || message.isBlank()) {
              throw new IllegalArgumentException("message must not be blank");
          }
          field = field.strip();
          message = message.strip();
      }
  }
  ```

- [x] Create package-private `FrameworkProblem`. Each enum value owns an
  existing `HttpErrorMapping` plus its fixed public detail; `code()` returns
  `name()`. Constructor arguments must match the catalogue exactly.

- [x] Refactor `ProblemDetailsFactory` through one private construction method.

  ```java
  ProblemDetail createFramework(
          FrameworkProblem frameworkProblem,
          List<ValidationViolation> violations,
          HttpServletRequest request
  ) {
      Objects.requireNonNull(frameworkProblem, "frameworkProblem must not be null");
      Objects.requireNonNull(violations, "violations must not be null");
      return createProblem(
              frameworkProblem.code(),
              frameworkProblem.detail(),
              frameworkProblem.mapping(),
              List.copyOf(violations),
              request
      );
  }
  ```

  The existing `create(ApplicationException, HttpErrorMapping,
  HttpServletRequest)` delegates to the same private method with an empty
  violation list. The private method sets `violations` only when the list is
  nonempty. It never accepts an exception message or cause.

- [x] Run catalogue, factory, existing REST, neutral-contract, and architecture
  tests.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=FrameworkProblemTest,ProblemDetailsFactoryTest,RestExceptionHandlerTest,ApplicationExceptionTest,ExceptionArchitectureTest" test
  ```

  Expected: PASS; the existing KAN-21 application-error response is byte-shape
  compatible and still has no `violations` property.

- [x] Commit the allowlisted framework response model.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/error/FrameworkProblem.java `
    src/main/java/com/project/optrabidz/common/api/error/ValidationViolation.java `
    src/main/java/com/project/optrabidz/common/api/error/ProblemDetailsFactory.java `
    src/test/java/com/project/optrabidz/common/api/error/FrameworkProblemTest.java `
    src/test/java/com/project/optrabidz/common/api/error/ProblemDetailsFactoryTest.java
  git commit -m "feat: define safe MVC problem catalogue (KAN-22)"
  ```

---

## Task 2: Map validation metadata without exposing submitted values

**Consumes:** Spring `BindingResult`, `HandlerMethodValidationException`,
Jakarta `ConstraintViolation`, and `ValidationViolation`.

**Produces:**

```java
public final class ValidationViolationMapper {
    List<ValidationViolation> fromBindingResult(BindingResult result);
    List<ValidationViolation> fromMethodValidation(
            HandlerMethodValidationException exception
    );
    List<ValidationViolation> fromConstraintViolations(
            Set<? extends ConstraintViolation<?>> violations
    );
    ValidationViolation missing(String field);
    ValidationViolation typeMismatch(String field);
}
```

### Steps

- [x] Add `ValidationViolationMapperTest` with real Spring error values whose
  rejected values and default messages contain a sentinel secret.

  ```java
  @Test
  void mapsKnownAndUnknownFieldErrorsWithoutRejectedValues() {
      BeanPropertyBindingResult result =
              new BeanPropertyBindingResult(new Object(), "request");
      result.addError(new FieldError(
              "request", "email", "secret@example.test", false,
              new String[]{"Email.request.email", "Email"}, null,
              "secret@example.test is not valid"
      ));
      result.addError(new FieldError(
              "request", "custom", "token-123", false,
              new String[]{"UnknownConstraint"}, null,
              "token-123 failed internal rule"
      ));

      assertThat(mapper.fromBindingResult(result)).containsExactly(
              new ValidationViolation("custom", "is invalid"),
              new ValidationViolation("email", "must be a well-formed email address")
      );
  }
  ```

  Add tests for nested/indexed paths, method-parameter names, constraint paths
  that contain a method node, null inputs, duplicate violations, and
  deterministic field/message ordering.

- [x] Run the mapper test and preserve RED caused only by the missing mapper.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ValidationViolationMapperTest" test
  ```

- [x] Create the safe constraint-message allowlist.

  ```java
  private static final Map<String, String> SAFE_MESSAGES = Map.ofEntries(
          Map.entry("NotNull", "must not be null"),
          Map.entry("NotBlank", "must not be blank"),
          Map.entry("NotEmpty", "must not be empty"),
          Map.entry("Email", "must be a well-formed email address"),
          Map.entry("Size", "size is outside the allowed range"),
          Map.entry("Min", "must be at least the minimum allowed value"),
          Map.entry("DecimalMin", "must be at least the minimum allowed value"),
          Map.entry("Max", "must not exceed the maximum allowed value"),
          Map.entry("DecimalMax", "must not exceed the maximum allowed value"),
          Map.entry("Positive", "must be greater than zero"),
          Map.entry("PositiveOrZero", "must be zero or greater"),
          Map.entry("Negative", "must be less than zero"),
          Map.entry("NegativeOrZero", "must be zero or less"),
          Map.entry("Pattern", "has an invalid format"),
          Map.entry("Past", "must be in the past"),
          Map.entry("PastOrPresent", "must be in the past or present"),
          Map.entry("Future", "must be in the future"),
          Map.entry("FutureOrPresent", "must be in the present or future")
  );
  ```

  Match only the exact constraint name or Spring codes beginning with
  `<constraint>.`. Unknown codes return `is invalid`. Never call
  `getDefaultMessage()` or `ConstraintViolation#getMessage()`.

  ```java
  private String safeMessage(String[] codes) {
      if (codes == null) {
          return "is invalid";
      }
      return SAFE_MESSAGES.entrySet().stream()
              .filter(entry -> Arrays.stream(codes).anyMatch(code ->
                      code.equals(entry.getKey())
                              || code.startsWith(entry.getKey() + ".")
              ))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse("is invalid");
  }
  ```

  For Jakarta violations, pass only
  `violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()`
  into the same allowlist lookup; never pass the violation object or its
  interpolated message into `ValidationViolation`.

- [x] Implement safe field extraction.

  - `FieldError#getField()` supplies DTO paths such as
    `debtTerms.requestedAmount` and `items[0].value`.
  - Object-level errors use `_request`.
  - Handler-method validation uses an explicit `@RequestParam`,
    `@RequestHeader`, `@PathVariable`, `@CookieValue`, or `@RequestPart` name;
    then the Java parameter name; then `arg<index>`.
  - Jakarta paths retain `PROPERTY` and `PARAMETER` nodes, attach container
    indices, and skip `METHOD`, `CONSTRUCTOR`, `RETURN_VALUE`,
    `CROSS_PARAMETER`, and `BEAN` nodes.
  - Blank results use `_request`.

- [x] Normalize all returned lists with one helper.

  ```java
  private List<ValidationViolation> sortedDistinct(
          Stream<ValidationViolation> violations
  ) {
      return violations
              .distinct()
              .sorted(Comparator
                      .comparing(ValidationViolation::field)
                      .thenComparing(ValidationViolation::message))
              .toList();
  }
  ```

  `missing(field)` returns `<field>: is required`; `typeMismatch(field)` returns
  `<field>: has an invalid type`. Both normalize a blank field to `_request`.

- [x] Run mapper, factory, and architecture tests.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=ValidationViolationMapperTest,ProblemDetailsFactoryTest,ExceptionArchitectureTest" test
  ```

  Expected: PASS; sentinel rejected values and arbitrary default messages are
  absent from every serialized or string representation asserted by the tests.

- [x] Commit the validation boundary mapper.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/error/ValidationViolationMapper.java `
    src/test/java/com/project/optrabidz/common/api/error/ValidationViolationMapperTest.java
  git commit -m "feat: sanitize MVC validation violations (KAN-22)"
  ```

---

## Task 3: Route validation and binding failures through the shared contract

**Consumes:** `FrameworkProblem.VALIDATION_ERROR`,
`ValidationViolationMapper`, `ProblemDetailsFactory`, and Spring MVC 6.1
handler overrides.

**Produces:** A highest-precedence `RestExceptionHandler` that extends
`ResponseEntityExceptionHandler` and handles approved validation families.

### Steps

- [x] Create `RestExceptionHandlerValidationTest` with standalone MockMvc, the
  real `RequestMetadataFilter`, both controller advices, a fixed factory clock,
  and test-only controller/record types under `src/test`.

  ```java
  mockMvc = MockMvcBuilders
          .standaloneSetup(new ValidationProbeController())
          .setControllerAdvice(
                  new GlobalExceptionHandler(),
                  new RestExceptionHandler(
                          fixedFactory,
                          new ValidationViolationMapper()
                  )
          )
          .addFilters(new RequestMetadataFilter())
          .build();
  ```

  Probe endpoints must cover:

  ```java
  @PostMapping(path = "/test/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
  void body(@Valid @RequestBody ValidationProbe request) {}

  @GetMapping("/test/required-parameter")
  void requiredParameter(@RequestParam String accountId) {}

  @GetMapping("/test/required-header")
  void requiredHeader(@RequestHeader("X-Required") String value) {}

  @GetMapping("/test/type-mismatch")
  void typeMismatch(@RequestParam Long count) {}

  @GetMapping("/test/method-validation")
  void methodValidation(@RequestParam @Positive Integer amount) {}
  ```

  `ValidationProbe` contains a direct field, a nested `@Valid` object, and a
  `List<@NotBlank String>` so the tests prove safe nested and indexed paths.

- [x] Assert the exact validation response contract for every probe:
  `application/problem+json`, status 400, validation type/title/detail/code,
  request ID header/body equality, ISO timestamp, opaque instance, and sorted
  `violations`. Add negative assertions for submitted passwords, tokens,
  malformed values, raw exception text, and Java types.

- [x] Run the validation MockMvc test and preserve RED.

  ```powershell
  .\mvnw.cmd -B "-Dtest=RestExceptionHandlerValidationTest" test
  ```

  Expected: responses use the legacy envelope or Spring defaults because the
  new handler does not yet intercept these exceptions.

- [x] Change `RestExceptionHandler` to extend
  `ResponseEntityExceptionHandler`, inject `ValidationViolationMapper`, and
  preserve the existing `ApplicationException` method unchanged in public
  behavior.

- [x] Add one response helper that casts `WebRequest` to `ServletWebRequest`,
  delegates body creation to `ProblemDetailsFactory`, copies the supplied
  `HttpHeaders`, overrides content type to `application/problem+json`, and uses
  the catalogue status.

  ```java
  private ResponseEntity<Object> frameworkResponse(
          FrameworkProblem problem,
          List<ValidationViolation> violations,
          HttpHeaders headers,
          WebRequest webRequest
  ) {
      HttpServletRequest request =
              ((ServletWebRequest) webRequest).getRequest();
      ProblemDetail body = problemDetailsFactory.createFramework(
              problem,
              violations,
              request
      );
      return ResponseEntity.status(problem.mapping().status())
              .headers(headers)
              .contentType(MediaType.APPLICATION_PROBLEM_JSON)
              .body(body);
  }
  ```

- [x] Override the exact Spring 6.1 validation hooks:

  - `handleMethodArgumentNotValid` uses `fromBindingResult`;
  - `handleHandlerMethodValidationException` uses `fromMethodValidation`;
  - `handleMissingServletRequestParameter` and
    `handleMissingServletRequestPart` use `missing(name)`;
  - `handleServletRequestBindingException` extracts a
    `MissingRequestHeaderException` header name or uses `_request`;
  - `handleTypeMismatch` extracts
    `MethodArgumentTypeMismatchException#getName()` or uses `_request`.

  Do not override the deprecated-for-removal `handleBindException` hook in
  Spring Framework 6.1. Supported method-argument, method-validation, and type
  mismatch hooks cover this story's request-binding contract without adding a
  production deprecation warning.

- [x] Add an explicit `@ExceptionHandler(ConstraintViolationException.class)`
  using `fromConstraintViolations`, because this exception is not covered by
  `ResponseEntityExceptionHandler`.

- [x] Run validation, existing REST, factory, mapper, and architecture tests.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=RestExceptionHandlerValidationTest,RestExceptionHandlerTest,ValidationViolationMapperTest,ProblemDetailsFactoryTest,ExceptionArchitectureTest" test
  ```

  Expected: PASS; the KAN-21 application response and the unaffected legacy
  `IllegalArgumentException` response remain unchanged.

- [x] Commit the validation MVC adapter.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java `
    src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerValidationTest.java `
    src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java
  git commit -m "feat: render MVC validation problem details (KAN-22)"
  ```

---

## Task 4: Route deterministic framework and protocol failures

**Consumes:** The shared framework response helper and five non-validation
`FrameworkProblem` values.

**Produces:** Safe malformed JSON, 404, 405, 406, and 415 responses while
preserving Spring protocol headers.

### Steps

- [x] Create `RestExceptionHandlerFrameworkTest` with the same advice, fixed
  clock, request filter, and standalone MockMvc setup. Spring Framework 6.1
  raises `NoHandlerFoundException` for unknown routes without using the
  deprecated `setThrowExceptionIfNoHandlerFound` switch.

- [x] Add exact MockMvc scenarios:

  - invalid JSON sent to an `application/json` request-body endpoint;
  - GET of an unmapped path;
  - POST to a GET-only endpoint, asserting `Allow` contains `GET`;
  - `Accept: application/xml` for a JSON-only endpoint;
  - `Content-Type: text/plain` for a JSON-only request-body endpoint, asserting
    the Spring-provided accepted-media header is retained when present;
  - a valid JSON request and a valid GET response proving success status/body
    behavior is unchanged.

- [x] For every error, assert the exact catalogue status, type, title, detail,
  code, content type, request ID, timestamp, and instance. Assert no
  `violations` for non-validation failures and no raw JSON/parser sentinel,
  media-type implementation detail, handler signature, or Java type.

- [x] Run the framework MockMvc test and preserve RED.

  ```powershell
  .\mvnw.cmd -B "-Dtest=RestExceptionHandlerFrameworkTest" test
  ```

  Expected: the five scenarios return Spring/legacy responses instead of the
  approved contract.

- [x] Override the exact Spring 6.1 hooks and pass their supplied headers to
  the shared helper:

  ```java
  handleHttpMessageNotReadable(...)       -> MALFORMED_REQUEST
  handleNoHandlerFoundException(...)      -> ENDPOINT_NOT_FOUND
  handleNoResourceFoundException(...)     -> ENDPOINT_NOT_FOUND
  handleHttpRequestMethodNotSupported(...) -> METHOD_NOT_ALLOWED
  handleHttpMediaTypeNotAcceptable(...)   -> NOT_ACCEPTABLE
  handleHttpMediaTypeNotSupported(...)    -> UNSUPPORTED_MEDIA_TYPE
  ```

  Each call uses `List.of()`; no exception-derived message becomes response
  data. Do not override other `ResponseEntityExceptionHandler` families in
  this story.

- [x] Run both MVC suites plus the existing adapter regressions.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=RestExceptionHandlerFrameworkTest,RestExceptionHandlerValidationTest,RestExceptionHandlerTest,FrameworkProblemTest,ProblemDetailsFactoryTest,ValidationViolationMapperTest" test
  ```

  Expected: PASS. Temporarily dropping `.headers(headers)` must make the 405
  header assertion fail; restore it and retain that mutation as causal
  evidence.

- [x] Commit deterministic framework mappings.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java `
    src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerFrameworkTest.java
  git commit -m "feat: standardize MVC framework failures (KAN-22)"
  ```

---

## Task 5: Remove overlaps, verify the complete branch, and prepare review

**Consumes:** All completed KAN-22 production and test components.

**Produces:** One unambiguous MVC failure path and reproducible exact-head
evidence for the pull request into `develop`.

### Steps

- [x] Remove only these overlapping methods from `GlobalExceptionHandler`:

  ```text
  handleValidationException(MethodArgumentNotValidException, ...)
  handleConstraintViolation(ConstraintViolationException, ...)
  handleUnreadableBody(HttpMessageNotReadableException, ...)
  ```

  Remove the now-unused `FieldError` import and `toErrorField` helper. Preserve
  `ApiException`, `IllegalArgumentException`, `IllegalStateException`,
  `NullPointerException`, and `Exception` handlers unchanged.

- [x] Run a source guard proving the overlap is gone and the legacy fallback
  remains.

  ```powershell
  rg -n `
    "MethodArgumentNotValidException|ConstraintViolationException|HttpMessageNotReadableException|handleUnexpected|handleApiException" `
    src/main/java/com/project/optrabidz/common/api/exception/GlobalExceptionHandler.java
  ```

  Expected: only `handleUnexpected` and `handleApiException` from this search
  remain.

- [x] Run the complete focused KAN-22 suite.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=FrameworkProblemTest,ValidationViolationMapperTest,ProblemDetailsFactoryTest,RestExceptionHandlerTest,RestExceptionHandlerValidationTest,RestExceptionHandlerFrameworkTest,ApplicationExceptionTest,ExceptionArchitectureTest" test
  ```

  Expected: all tests pass with zero failures and zero errors.

- [x] Run the complete unit suite.

  ```powershell
  .\mvnw.cmd -B test
  ```

  Expected: all unit, parameterized, architecture, and MockMvc tests pass with
  zero failures and zero errors.

- [x] With Docker Engine running, run the complete PostgreSQL integration
  suite.

  ```powershell
  .\mvnw.cmd -B verify -Pintegration-tests
  ```

  Expected: Flyway applies the unchanged V1 migration to PostgreSQL 16 and all
  unit plus integration tests pass with zero failures and zero errors.

- [x] Prove scope and repository hygiene.

  ```powershell
  git status --short
  git diff --check origin/develop...HEAD
  git diff --name-status origin/develop...HEAD
  git diff origin/develop...HEAD -- `
    pom.xml `
    .github/workflows `
    src/main/resources `
    src/main/resources/db/migration `
    src/main/java/com/project/optrabidz/common/error `
    src/main/java/com/project/optrabidz/security
  ```

  Expected: no whitespace errors and no changes to dependencies, CI, runtime
  configuration, Flyway, the neutral contract, or security. The feature diff
  is limited to the approved REST adapter, three removed legacy overlaps,
  focused tests, and this plan.

- [x] Inspect the packaged JAR to prove test probes are not shipped.

  ```powershell
  jar tf target\optrabidz-0.0.1-SNAPSHOT.jar | `
    Select-String "ValidationProbe|FrameworkProbe|src/test"
  ```

  Expected: no output.

- [x] Record executed commands, exact test counts, results, branch head SHA,
  RED/GREEN evidence, header-preservation mutation, and approved deviations in
  an `Execution evidence` section in this file.

- [x] Commit only the cleanup and finalized evidence.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/api/exception/GlobalExceptionHandler.java `
    docs/design/KAN-22-mvc-problem-details-implementation-plan.md
  git commit -m "refactor: retire migrated MVC error handlers (KAN-22)"
  ```

- [ ] Push and open a pull request into `develop`; do not merge it.

  ```powershell
  git push -u origin feature/KAN-22-mvc-problem-details
  gh pr create `
    --base develop `
    --head feature/KAN-22-mvc-problem-details `
    --title "KAN-22 Standardize Spring MVC problem details" `
    --body-file .github/pull_request_template.md
  ```

  If no reusable PR body exists, create the body from the verified scope,
  contract table, RED/GREEN evidence, security review, complete test results,
  and rollback statement; do not add a one-use project file.

- [ ] Verify the remote PR head equals the locally tested head and wait for
  `Unit Tests` and `PostgreSQL Integration Tests` on that exact head. Present
  the PR and exact diff for user review. Do not merge.

## Execution evidence

Recorded on 2026-08-15 before publication.

### Baseline and branch

- The clean baseline at `cefd0973c430a9be8480f2ce17213a0faac1d086`
  passed `mvnw.cmd -B test`: 87 tests, zero failures and zero errors.
- Work was isolated on `feature/KAN-22-mvc-problem-details` from that exact
  `develop` commit. Local and remote `main` remained unchanged at
  `bc7727b0b2e09ebbfef8b9c6c5dc729cd4aab4fb`.
- The implemented slices before the final cleanup commit end at
  `226315385c174b79b27036dcd6b244c277d86096`. The complete worktree, including
  the cleanup below, is the tree exercised by the final local suites.

### RED/GREEN evidence

- Catalogue/factory RED: compilation failed because `FrameworkProblem` did not
  exist. GREEN: the focused catalogue, factory, adapter, neutral-contract, and
  architecture set passed 21 tests.
- Validation mapper RED: compilation failed because
  `ValidationViolationMapper` did not exist. GREEN: the focused mapper set
  passed 13 tests.
- Validation MVC RED: the existing handler constructor could not accept the new
  mapper. GREEN: validation MVC and its supporting regressions passed 22 tests.
- Framework MVC RED: five protocol scenarios returned Spring's default
  `about:blank` response rather than the approved catalogue. GREEN: the combined
  MVC set passed 32 tests after the explicit Spring 6.1 overrides.
- Header mutation: temporarily omitting `.headers(headers)` made the exact 405
  test fail because `Allow` was missing. Restoring the supplied framework
  headers returned the test to GREEN.

### Final local verification

- Source guard found only `handleApiException` and `handleUnexpected`; the three
  migrated legacy overlaps and their unsafe default-message mapping are absent.
- Focused KAN-22 suite: 38 tests, zero failures and zero errors.
- `mvnw.cmd -B test`: 114 tests across 26 classes, zero failures, errors, or
  skips.
- `mvnw.cmd -B verify -Pintegration-tests`: the same 114 unit tests plus 64
  integration tests across 16 classes, zero failures, errors, or skips. A real
  PostgreSQL 16.14 Testcontainer started, and Flyway validated the single V1
  migration before the integration suite. Total verification time was 2:54.
- `git diff --check` passed. The protected-area diff was empty for `pom.xml`,
  workflows, runtime resources, Flyway, the neutral error contract, and
  security. V1 retained blob `8784c468aa169952a87e726303d03abae4376add`.
- JAR inspection returned no `ValidationProbe`, `FrameworkProbe`, or `src/test`
  entries.

### Approved implementation adaptations

- `handleBindException` was not overridden because that hook is deprecated in
  Spring Framework 6.1; the supported validation and binding hooks cover the
  approved cases.
- The deprecated DispatcherServlet throw-if-no-handler switch was not used.
  Spring 6.1's `NoResourceFoundException` path provides the tested 404 mapping.

The pull-request number, published head SHA, and exact-head CI results are
recorded in the PR after publication so this file does not require a
self-referential evidence commit. No merge is part of KAN-22 execution.

## Review gates

1. Written Jira specification approved — complete.
2. This implementation plan approved — required before creating the branch or
   changing production/test code.
3. Inline execution approved — required before running RED tests or writing
   implementation code.
4. Pull request reviewed and explicitly approved — required before merge into
   `develop`.
5. `main` remains unchanged throughout KAN-22.
