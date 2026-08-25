# KAN-33 Legacy Exception-Stack Removal Implementation Plan

**Goal:** Remove the obsolete HTTP-coupled exception stack and make the
transport-neutral RFC 9457 error path the only production failure contract.

**Architecture:** `RestExceptionHandler` becomes the single Spring MVC
exception boundary. Expected application and framework mappings remain
unchanged, while every unclassified exception is logged once and rendered as
the same sanitized `INTERNAL_SERVER_ERROR` Problem Details response. Spring
Security keeps its existing filter-chain adapters.

**Tech stack:** Java 21, Spring Boot 3.3.2, Spring MVC, Spring Security,
JUnit 5, MockMvc, AssertJ, Logback test appenders, ArchUnit 1.4.2, Maven,
Flyway, PostgreSQL, and Testcontainers.

**Spec:**
[`design.md`](design.md)

## Global constraints

- Keep every approved application, validation, framework, and security error
  mapping unchanged.
- Return unexpected failures as `application/problem+json` with HTTP 500,
  type `urn:optrabidz:problem:internal-server-error`, title
  `Internal server error`, detail `An unexpected error occurred`, and code
  `INTERNAL_SERVER_ERROR`.
- Never expose Java exception messages, types, causes, stack traces, rejected
  values, SQL details, credentials, signatures, or secrets in the public body.
- Keep request-ID header, body property, and instance URN correlated.
- Log an unexpected failure once with its throwable; rely on the existing MDC
  request ID, method, and path rather than copying request data into the log
  message.
- Preserve `ApiResponse.success(...)`, `SuccessResponse`, `Meta`, and all
  successful `/api/v1` response bodies until KAN-41.
- Do not change authentication, authorization, session, CSRF, JWT/OAuth2,
  persistence, Flyway migrations, payment behavior, audit behavior, outbox,
  notifications, or dependencies.
- Do not retain an executable fixture for the legacy error envelope.

## File responsibility map

| File | Responsibility in KAN-33 |
| --- | --- |
| `src/main/java/com/project/optrabidz/common/api/error/FrameworkProblem.java` | Owns the fixed internal-server-error public descriptor. |
| `src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java` | Logs and renders every unclassified MVC exception. |
| `src/main/java/com/project/optrabidz/common/api/error/ProblemDetailsFactory.java` | Remains the sole Problem Details constructor; no change is expected. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java` | Proves expected mappings and all unexpected runtime categories. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerLoggingTest.java` | Proves one error log with MDC correlation and no request data added to the message. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerContextTest.java` | Proves Spring discovers the advice and selects it in a real MVC slice. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerFrameworkTest.java` | Removes obsolete competing-advice registration and preserves framework mappings. |
| `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerValidationTest.java` | Removes obsolete competing-advice registration and preserves validation mappings. |
| `src/test/java/com/project/optrabidz/common/api/response/ApiResponseTest.java` | Proves success construction remains and the legacy error factory is absent. |
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Replaces the frozen waiver with unconditional transport and legacy-stack rules. |
| `src/main/java/com/project/optrabidz/common/api/response/ApiResponse.java` | Loses only legacy error construction. |
| `src/main/java/com/project/optrabidz/common/api/exception/` | Deleted obsolete exception base, code catalogue, field DTO, and handler. |
| `src/main/java/com/project/optrabidz/common/api/response/ErrorResponse.java` | Deleted obsolete failure envelope. |
| `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookVerificationException.java` | Deleted unused legacy subclass. |
| `src/main/java/com/project/optrabidz/financial/application/exception/UnsupportedPaymentWebhookEventException.java` | Deleted unused legacy subclass. |
| `src/test/resources/archunit.properties` | Deleted stale freeze-store configuration. |
| `src/test/resources/archunit-store/` | Deleted the single stored rule and its six obsolete violations. |

---

### Task 1: Establish the sanitized unexpected-failure boundary

**Files:**

- Modify: `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java`
- Create: `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerLoggingTest.java`
- Create: `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerContextTest.java`
- Modify: `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerFrameworkTest.java`
- Modify: `src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerValidationTest.java`
- Modify: `src/main/java/com/project/optrabidz/common/api/error/FrameworkProblem.java`
- Modify: `src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java`

**Interfaces:**

- Consumes: `ProblemDetailsFactory.createFramework(FrameworkProblem,
  List<ValidationViolation>, HttpServletRequest)` and the existing
  `RequestMetadataFilter` MDC/header behavior.
- Produces: `RestExceptionHandler.handleUnexpectedException(Exception,
  HttpServletRequest)` returning `ResponseEntity<Object>` and
  `FrameworkProblem.INTERNAL_SERVER_ERROR`.

- [ ] **Step 1: Replace the legacy-envelope test with failing unexpected-error contract tests**

  Remove the `GlobalExceptionHandler` import and registration from all three
  standalone handler test classes. In `RestExceptionHandlerTest`, replace
  `leavesLegacyExceptionsOnTheLegacyEnvelope()` with the following
  parameterized contract:

  ```java
  @ParameterizedTest
  @ValueSource(strings = {"argument", "state", "null", "runtime"})
  void sanitizesEveryUnexpectedRuntimeFailure(String failure) throws Exception {
      String requestId = "unexpected-" + failure;

      mockMvc.perform(get("/test/unexpected/{failure}", failure)
                      .header("X-Request-Id", requestId))
              .andExpect(status().isInternalServerError())
              .andExpect(content().contentTypeCompatibleWith(
                      MediaType.APPLICATION_PROBLEM_JSON
              ))
              .andExpect(header().string("X-Request-Id", requestId))
              .andExpect(jsonPath("$.type").value(
                      "urn:optrabidz:problem:internal-server-error"
              ))
              .andExpect(jsonPath("$.title").value("Internal server error"))
              .andExpect(jsonPath("$.status").value(500))
              .andExpect(jsonPath("$.detail").value(
                      "An unexpected error occurred"
              ))
              .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
              .andExpect(jsonPath("$.requestId").value(requestId))
              .andExpect(jsonPath("$.instance").value(
                      "urn:optrabidz:request:" + requestId
              ))
              .andExpect(jsonPath("$.timestamp").value(
                      "2026-08-15T04:00:00Z"
              ))
              .andExpect(jsonPath("$.violations").doesNotExist())
              .andExpect(content().string(not(containsString(
                      "password=hunter2"
              ))))
              .andExpect(content().string(not(containsString(
                      "IllegalArgumentException"
              ))))
              .andExpect(content().string(not(containsString(
                      "IllegalStateException"
              ))))
              .andExpect(content().string(not(containsString(
                      "NullPointerException"
              ))));
  }
  ```

  Add this probe endpoint to `FailureProbeController`:

  ```java
  @GetMapping("/test/unexpected/{failure}")
  void unexpected(@PathVariable String failure) {
      RuntimeException exception = switch (failure) {
          case "argument" -> new IllegalArgumentException(
                  "password=hunter2"
          );
          case "state" -> new IllegalStateException(
                  "jdbc:postgresql://private-host"
          );
          case "null" -> new NullPointerException(
                  "credential was null"
          );
          case "runtime" -> new RuntimeException(
                  "provider-secret-value"
          );
          default -> new IllegalArgumentException("unknown test failure");
      };
      throw exception;
  }
  ```

- [ ] **Step 2: Add a failing MVC component-discovery test**

  Create `RestExceptionHandlerContextTest` as a Web MVC slice. Import only the
  handler dependencies; allow `@WebMvcTest` to discover the advice itself.

  ```java
  @WebMvcTest(controllers = RestExceptionHandlerContextTest.FailureController.class)
  @AutoConfigureMockMvc(addFilters = false)
  @Import({ProblemDetailsFactory.class, ValidationViolationMapper.class})
  class RestExceptionHandlerContextTest {
      @Autowired
      private MockMvc mockMvc;

      @Test
      void discoversTheSingleMvcAdviceForUnexpectedFailures() throws Exception {
          mockMvc.perform(get("/test/context-unexpected"))
                  .andExpect(status().isInternalServerError())
                  .andExpect(content().contentTypeCompatibleWith(
                          MediaType.APPLICATION_PROBLEM_JSON
                  ))
                  .andExpect(jsonPath("$.code").value(
                          "INTERNAL_SERVER_ERROR"
                  ))
                  .andExpect(jsonPath("$.detail").value(
                          "An unexpected error occurred"
                  ));
      }

      @RestController
      static final class FailureController {
          @GetMapping("/test/context-unexpected")
          void fail() {
              throw new RuntimeException("private context detail");
          }
      }
  }
  ```

- [ ] **Step 3: Add a failing one-log-event test**

  Create `RestExceptionHandlerLoggingTest` with a Logback `ListAppender`. Build
  standalone MockMvc with `RestExceptionHandler` and `RequestMetadataFilter`,
  attach the appender to the handler logger in `@BeforeEach`, and detach it in
  `@AfterEach`.

  ```java
  @Test
  void logsUnexpectedFailureOnceWithMdcCorrelation() throws Exception {
      mockMvc.perform(get("/test/logged-unexpected")
                      .header("X-Request-Id", "log-request-123"))
              .andExpect(status().isInternalServerError());

      assertThat(appender.list).singleElement().satisfies(event -> {
          assertThat(event.getLevel()).isEqualTo(Level.ERROR);
          assertThat(event.getFormattedMessage())
                  .isEqualTo("Unhandled MVC exception");
          assertThat(event.getThrowableProxy().getClassName())
                  .isEqualTo(RuntimeException.class.getName());
          assertThat(event.getMDCPropertyMap())
                  .containsEntry("requestId", "log-request-123")
                  .containsEntry("method", "GET")
                  .containsEntry("path", "/test/logged-unexpected");
      });
  }
  ```

  Use this probe endpoint:

  ```java
  @RestController
  static final class LoggingProbeController {
      @GetMapping("/test/logged-unexpected")
      void fail() {
          throw new RuntimeException("internal diagnostic");
      }
  }
  ```

  The fixed log message must not concatenate the HTTP path, headers, request
  body, exception message, or user input; structured correlation comes from
  MDC and the throwable is retained for internal diagnosis.

- [ ] **Step 4: Run the new tests and capture the RED result**

  ```powershell
  .\mvnw.cmd -q "-Dtest=RestExceptionHandlerTest,RestExceptionHandlerContextTest,RestExceptionHandlerLoggingTest" test
  ```

  Expected: FAIL because `RestExceptionHandler` has no generic unexpected
  mapping; the request either propagates or is rendered by the legacy advice.

- [ ] **Step 5: Add the fixed internal descriptor**

  Add the following final entry to `FrameworkProblem`:

  ```java
  INTERNAL_SERVER_ERROR(
          new HttpErrorMapping(
                  HttpStatus.INTERNAL_SERVER_ERROR,
                  "Internal server error"
          ),
          "An unexpected error occurred"
  );
  ```

- [ ] **Step 6: Implement the minimal generic MVC handler**

  Add an SLF4J logger and this method to `RestExceptionHandler`:

  ```java
  private static final Logger log = LoggerFactory.getLogger(
          RestExceptionHandler.class
  );

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpectedException(
          Exception exception,
          HttpServletRequest request
  ) {
      log.error("Unhandled MVC exception", exception);
      return frameworkResponse(
              FrameworkProblem.INTERNAL_SERVER_ERROR,
              List.of(),
              new HttpHeaders(),
              request
      );
  }
  ```

  Do not add separate `IllegalArgumentException`, `IllegalStateException`, or
  `NullPointerException` handlers. Expected client/business failures must use
  explicit validation/framework mappings or `ApplicationException`.

- [ ] **Step 7: Run focused handler, logging, framework, validation, and factory tests**

  ```powershell
  .\mvnw.cmd -q "-Dtest=RestExceptionHandlerTest,RestExceptionHandlerContextTest,RestExceptionHandlerLoggingTest,RestExceptionHandlerFrameworkTest,RestExceptionHandlerValidationTest,ProblemDetailsFactoryTest" test
  ```

  Expected: PASS. Application, validation, and framework assertions remain
  byte-for-byte equivalent to their pre-KAN-33 expectations.

- [ ] **Step 8: Commit the unexpected-failure boundary**

  ```powershell
  git add -- src/main/java/com/project/optrabidz/common/api/error/FrameworkProblem.java src/main/java/com/project/optrabidz/common/api/error/RestExceptionHandler.java src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerTest.java src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerContextTest.java src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerLoggingTest.java src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerFrameworkTest.java src/test/java/com/project/optrabidz/common/api/error/RestExceptionHandlerValidationTest.java
  git commit -m "feat(KAN-33): sanitize unexpected API failures"
  ```

---

### Task 2: Delete the legacy stack and make architecture enforcement strict

**Files:**

- Create: `src/test/java/com/project/optrabidz/common/api/response/ApiResponseTest.java`
- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify: `src/main/java/com/project/optrabidz/common/api/response/ApiResponse.java`
- Delete: `src/main/java/com/project/optrabidz/common/api/response/ErrorResponse.java`
- Delete: `src/main/java/com/project/optrabidz/common/api/exception/ApiException.java`
- Delete: `src/main/java/com/project/optrabidz/common/api/exception/ErrorCode.java`
- Delete: `src/main/java/com/project/optrabidz/common/api/exception/ErrorField.java`
- Delete: `src/main/java/com/project/optrabidz/common/api/exception/GlobalExceptionHandler.java`
- Delete: `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookVerificationException.java`
- Delete: `src/main/java/com/project/optrabidz/financial/application/exception/UnsupportedPaymentWebhookEventException.java`
- Delete: `src/test/resources/archunit.properties`
- Delete: `src/test/resources/archunit-store/stored.rules`
- Delete: `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f`

**Interfaces:**

- Consumes: the sanitized handler and fixed descriptor from Task 1.
- Produces: `ApiResponse` with only success/meta responsibilities and
  unconditional ArchUnit protection against the removed stack.

- [ ] **Step 1: Add a failing success-preservation and error-removal test**

  Create `ApiResponseTest`:

  ```java
  class ApiResponseTest {
      @Test
      void preservesTheExistingSuccessContract() {
          MockHttpServletRequest request = new MockHttpServletRequest();
          request.setAttribute(
                  ApiResponse.REQUEST_ID_ATTRIBUTE,
                  "success-request-123"
          );

          SuccessResponse<String> response = ApiResponse.success("ok", request);

          assertThat(response.success()).isTrue();
          assertThat(response.data()).isEqualTo("ok");
          assertThat(response.meta().requestId())
                  .isEqualTo("success-request-123");
          assertThat(response.meta().timestamp()).isNotNull();
      }

      @Test
      void exposesNoLegacyErrorFactory() {
          assertThat(ApiResponse.class.getDeclaredMethods())
                  .noneMatch(method -> method.getName().equals("error"));
      }
  }
  ```

- [ ] **Step 2: Replace the frozen rule and add failing legacy-stack guards**

  Remove the `FreezingArchRule.freeze` import. Change
  `BUSINESS_EXCEPTIONS_ARE_TRANSPORT_NEUTRAL` to the direct rule:

  ```java
  @ArchTest
  static final ArchRule BUSINESS_EXCEPTIONS_ARE_TRANSPORT_NEUTRAL =
          noClasses()
                  .that().resideInAnyPackage("..domain..", "..application..")
                  .and().haveSimpleNameEndingWith("Exception")
                  .should().dependOnClassesThat().resideInAnyPackage(
                          "..common.api..",
                          "org.springframework.http..",
                          "org.springframework.security..",
                          "org.springframework.web..",
                          "jakarta.servlet.."
                  )
                  .as("domain and application exceptions must remain transport-neutral");
  ```

  Add these production-wide rules:

  ```java
  @ArchTest
  static final ArchRule LEGACY_API_EXCEPTION_PACKAGE_IS_ABSENT =
          noClasses()
                  .should().resideInAPackage("..common.api.exception..")
                  .as("the removed legacy API exception package must stay absent");

  @ArchTest
  static final ArchRule PRODUCTION_CODE_DOES_NOT_DEPEND_ON_LEGACY_API_EXCEPTIONS =
          noClasses()
                  .should().dependOnClassesThat().resideInAPackage(
                          "..common.api.exception.."
                  )
                  .as("production code must use the neutral error contract");

  @ArchTest
  static final ArchRule COMPETING_GLOBAL_EXCEPTION_HANDLER_IS_ABSENT =
          noClasses()
                  .should().haveSimpleName("GlobalExceptionHandler")
                  .as("RestExceptionHandler is the single MVC error boundary");
  ```

- [ ] **Step 3: Run the deletion guards and capture the RED result**

  ```powershell
  .\mvnw.cmd -q "-Dtest=ApiResponseTest,ExceptionArchitectureTest" test
  ```

  Expected: FAIL because `ApiResponse.error(...)`, the legacy package, the
  competing handler, the two transport-coupled webhook exceptions, and the
  frozen violations still exist.

- [ ] **Step 4: Remove only the error factory from `ApiResponse`**

  Delete imports for `ErrorCode`, `ErrorField`, and `List`, then delete the
  complete `error(...)` method. Preserve these signatures unchanged:

  ```java
  public static <T> SuccessResponse<T> success(
          T data,
          HttpServletRequest request
  )

  public static Meta meta(HttpServletRequest request)
  ```

  Keep `REQUEST_ID_ATTRIBUTE` in this class; KAN-41 owns moving that remaining
  metadata responsibility.

- [ ] **Step 5: Delete the obsolete production types atomically**

  Delete the four files in `common/api/exception`, `ErrorResponse.java`, and
  the two unused webhook exception files listed above. Do not translate those
  unused subclasses into new neutral exceptions: no production reference
  constructs or catches them.

- [ ] **Step 6: Delete the complete freeze-store configuration and data**

  Delete `archunit.properties`, `stored.rules`, and the single UUID-named
  violation file. No empty directory or replacement waiver is created.

- [ ] **Step 7: Run focused cleanup and architecture tests**

  ```powershell
  .\mvnw.cmd -q "-Dtest=ApiResponseTest,ExceptionArchitectureTest,RestExceptionHandlerTest,RestExceptionHandlerContextTest,RestExceptionHandlerLoggingTest,RestExceptionHandlerFrameworkTest,RestExceptionHandlerValidationTest,ProblemDetailsFactoryTest" test
  ```

  Expected: PASS without a freeze-store system property or generated baseline.

- [ ] **Step 8: Run explicit deletion and disclosure scans**

  ```powershell
  $deletedPaths = @(
      'src/main/java/com/project/optrabidz/common/api/exception/ApiException.java',
      'src/main/java/com/project/optrabidz/common/api/exception/ErrorCode.java',
      'src/main/java/com/project/optrabidz/common/api/exception/ErrorField.java',
      'src/main/java/com/project/optrabidz/common/api/exception/GlobalExceptionHandler.java',
      'src/main/java/com/project/optrabidz/common/api/response/ErrorResponse.java',
      'src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookVerificationException.java',
      'src/main/java/com/project/optrabidz/financial/application/exception/UnsupportedPaymentWebhookEventException.java',
      'src/test/resources/archunit.properties',
      'src/test/resources/archunit-store/stored.rules',
      'src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f'
  )
  $remaining = $deletedPaths | Where-Object { Test-Path -LiteralPath $_ }
  if ($remaining) { $remaining; throw 'Legacy paths remain' }

  rg -n "import com\.project\.optrabidz\.common\.api\.exception|new GlobalExceptionHandler|ApiResponse\.error|ErrorResponse" src/main/java src/test/java
  if ($LASTEXITCODE -eq 0) { throw 'Legacy references remain' }
  if ($LASTEXITCODE -gt 1) { throw 'Reference scan failed' }
  ```

  Expected: every path is absent and the reference scan returns no matches.

- [ ] **Step 9: Commit the atomic deletion and enforcement change**

  ```powershell
  git add -A -- src/main/java/com/project/optrabidz/common/api src/main/java/com/project/optrabidz/financial/application/exception src/test/java/com/project/optrabidz/common/api src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java src/test/resources/archunit.properties src/test/resources/archunit-store
  git commit -m "refactor(KAN-33): remove legacy exception stack"
  ```

---

### Task 3: Verify adjacent contracts and the complete application

**Files:**

- Verify only; production changes are not expected.

**Interfaces:**

- Consumes: the single MVC error boundary and strict architecture rules from
  Tasks 1 and 2.
- Produces: reproducible unit, security, Flyway, PostgreSQL, publication, and
  protected-scope evidence for the exact branch head.

- [ ] **Step 1: Run focused common-error and security-adapter tests**

  ```powershell
  .\mvnw.cmd -B "-Dtest=RestExceptionHandlerTest,RestExceptionHandlerContextTest,RestExceptionHandlerLoggingTest,RestExceptionHandlerFrameworkTest,RestExceptionHandlerValidationTest,ProblemDetailsFactoryTest,ApiResponseTest,ExceptionArchitectureTest,ProblemAuthenticationEntryPointTest,ProblemAccessDeniedHandlerTest,SecurityProblemResponseWriterTest" test
  ```

  Expected: BUILD SUCCESS. MVC failures use Problem Details; Spring Security
  401/403 adapters retain their approved codes and bodies.

- [ ] **Step 2: Run the complete unit suite**

  ```powershell
  .\mvnw.cmd -B test
  ```

  Expected: BUILD SUCCESS with no generated ArchUnit freeze store.

- [ ] **Step 3: Run Flyway clean-schema verification against PostgreSQL**

  ```powershell
  .\mvnw.cmd -B -DskipTests=true "-Dit.test=DatabaseMigrationIT,FlywayBaselineMigrationIT" verify -Pintegration-tests
  ```

  Expected: BUILD SUCCESS; Flyway migrations create and validate a clean
  PostgreSQL schema without any KAN-33 database change.

- [ ] **Step 4: Run the complete PostgreSQL integration profile**

  ```powershell
  .\mvnw.cmd -B verify -Pintegration-tests
  ```

  Expected: BUILD SUCCESS across API, security, audit, financial, notification,
  repository, outbox, and migration integration tests.

- [ ] **Step 5: Run documentation and repository-quality checks**

  ```powershell
  .\mvnw.cmd -q "-Dtest=DocumentationLinksTest" test
  git diff --check
  git status --short
  ```

  Expected: documentation links and whitespace checks pass. The status output
  contains only intentional KAN-33 changes or is clean after the two commits.

- [ ] **Step 6: Compare the exact branch head with its remote**

  ```powershell
  git push
  git rev-parse HEAD
  git rev-parse '@{upstream}'
  ```

  Expected: both hashes are identical. GitHub unit and PostgreSQL integration
  checks run against that exact commit before delivery is considered verified.

## Completion evidence

Record these facts with the delivered change:

- focused handler, disclosure, logging, context, architecture, and security
  test results;
- complete unit result;
- Flyway clean-schema and complete PostgreSQL integration results;
- deleted-path and legacy-reference scan results;
- absence of the ArchUnit freeze store;
- unchanged successful response and Spring Security contracts; and
- exact local, remote, and CI commit hash.
