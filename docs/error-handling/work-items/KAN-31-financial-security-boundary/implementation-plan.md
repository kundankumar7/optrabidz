# KAN-31 — Financial Security Boundary Implementation Plan

**Status:** Implemented and locally verified; awaiting pull-request review

**Goal:** Require authentication at the Spring Security boundary for every
financial user route, then remove controller-local authentication guards while
preserving business authorization, CSRF, provider-webhook, and response
behavior.

**Architecture:** `SecurityConfig` becomes the sole route-level user
authentication gate for the uncovered financial endpoint families. The shared
`ProblemAuthenticationEntryPoint` renders anonymous failures before controller
execution; financial controllers consume `AuthenticatedUserPrincipal`, and
`FinancialService` continues owning resource-level authorization.

**Tech stack:** Java 21, Spring Boot 3.3.2, Spring Security 6.3, RFC 9457
`ProblemDetail`, JUnit 5, Spring Security Test, MockMvc, ArchUnit, Testcontainers,
PostgreSQL 16, Flyway, Maven Wrapper, and GitHub Actions.

**Specification:**
[`design.md`](design.md)

## Global constraints

- Work only on `feature/KAN-31-financial-security-boundary`, created from
  verified `develop` commit `71a252b3a8f4d7cca40ff1e44da94deb171f90d4`.
- Pull requests target `develop`; KAN-31 never changes or merges into `main`.
- Do not add, remove, or upgrade dependencies.
- Do not change session creation, JWT, OAuth2, CSRF enablement, principal
  structure, password authentication, or role semantics.
- Do not change financial business rules, persistence, Flyway V1, payment
  providers, webhook signatures, payloads, outbox, audit, logging, cache,
  messaging, or unrelated endpoint policy.
- Keep payment-provider webhooks session-public and CSRF-exempt; provider
  signature verification remains mandatory.
- Keep resource ownership and financial permission decisions in
  `FinancialService`.
- Do not migrate financial business exceptions or remove shared legacy error
  infrastructure; KAN-30 owns that work.
- Preserve the allowlisted RFC 9457 authentication and CSRF contracts and never
  expose credentials, cookies, session IDs, CSRF values, signatures, secrets,
  raw payloads, principal internals, exception messages, causes, or stack
  traces.
- Use TDD for production changes: focused RED, minimal GREEN, regression tests,
  then a small commit.
- Do not merge the pull request without review approval and exact-head CI
  success.

## File map

| Path | Responsibility |
|---|---|
| `src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java` | Real-filter-chain tests for financial authentication, CSRF, safe Problem Details, authenticated continuation, and webhook isolation. |
| `src/main/java/com/project/optrabidz/security/infrastructure/config/SecurityConfig.java` | Explicitly requires authentication for the five financial user route families while preserving webhook order. |
| `src/main/java/com/project/optrabidz/financial/api/FinancialController.java` | Consumes the established principal without authenticating callers. |
| `src/main/java/com/project/optrabidz/financial/api/LocalPaymentSimulationController.java` | Consumes the established principal without authenticating callers and retains its property boundary. |
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Prevents the two financial user controllers from depending on legacy exception types. |
| `src/test/java/com/project/optrabidz/security/api/SecurityApiIT.java` | Existing shared security and provider-webhook regression coverage. |
| `src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java` | Existing authenticated financial success, ownership, payment, settlement, repayment, and local-simulation coverage. |
| `docs/error-handling/work-items/KAN-31-financial-security-boundary/implementation-plan.md` | Tracks approved execution steps and verification evidence. |

---

## Task 1: Freeze the financial authentication boundary with failing tests

**Files:**

- Create:
  `src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java`
- Read:
  `src/test/java/com/project/optrabidz/security/api/SecurityApiIT.java`
- Read:
  `src/test/java/com/project/optrabidz/testsupport/ApiIntegrationTestSupport.java`

**Consumes:** The existing `ApiIntegrationTestSupport`, request-ID policy,
Spring Security Test dependency, and shared RFC 9457 security contract.

**Produces:** A focused integration-test class that fails while financial user
routes still reach controller-local legacy authentication guards.

- [x] **Step 1: Verify the isolated baseline**

  ```powershell
  git status --short --branch
  git branch --show-current
  git rev-parse HEAD
  git rev-parse origin/develop
  git rev-parse origin/main
  ```

  Expected: branch is `feature/KAN-31-financial-security-boundary`; the worktree
  is clean; `origin/develop` remains `71a252b3a8f4d7cca40ff1e44da94deb171f90d4`;
  `origin/main` remains
  `bc7727b0b2e09ebbfef8b9c6c5dc729cd4aab4fb`; and `HEAD` contains only the
  approved KAN-31 documentation after that develop base.

- [x] **Step 2: Create the focused anonymous-read contract test**

  Create `FinancialSecurityApiIT` in package
  `com.project.optrabidz.security.api`, extending
  `ApiIntegrationTestSupport`. Add these exact imports and tests:

  ```java
  package com.project.optrabidz.security.api;

  import com.project.optrabidz.identity.domain.model.RoleType;
  import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
  import jakarta.servlet.http.Cookie;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.params.ParameterizedTest;
  import org.junit.jupiter.params.provider.MethodSource;
  import org.springframework.http.MediaType;
  import org.springframework.test.web.servlet.MvcResult;

  import java.util.stream.Stream;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  class FinancialSecurityApiIT extends ApiIntegrationTestSupport {
      private static final String REQUEST_ID = "financial-security-request-123";

      @ParameterizedTest
      @MethodSource("protectedFinancialReadPaths")
      void financialReadRequiresAuthentication(String path) throws Exception {
          MvcResult result = mockMvc.perform(get(path)
                          .header("X-Request-Id", REQUEST_ID)
                          .header("Authorization", "Bearer secret-financial-token"))
                  .andExpect(status().isUnauthorized())
                  .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                  .andExpect(jsonPath("$.type").value(
                          "urn:optrabidz:problem:authentication-required"))
                  .andExpect(jsonPath("$.title").value("Authentication required"))
                  .andExpect(jsonPath("$.status").value(401))
                  .andExpect(jsonPath("$.detail").value("Authentication is required"))
                  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                  .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                  .andExpect(jsonPath("$.success").doesNotExist())
                  .andExpect(jsonPath("$.error").doesNotExist())
                  .andReturn();

          assertThat(result.getResponse().getContentAsString())
                  .doesNotContain("secret-financial-token")
                  .doesNotContain("Authorization")
                  .doesNotContain("ApiException");
      }

      static Stream<String> protectedFinancialReadPaths() {
          return Stream.of(
                  "/api/v1/settlements/1",
                  "/api/v1/repayments/1",
                  "/api/v1/repayment-installments/1",
                  "/api/v1/payment-intents/1"
          );
      }
  }
  ```

- [x] **Step 3: Add an unsafe-route authentication test with valid application CSRF**

  Add this method to the same class. Prime the application's cookie-based CSRF
  repository, then echo the token through the configured request header to
  isolate authentication from CSRF-filter ordering:

  ```java
  @Test
  void paymentAttemptActionRequiresAuthenticationAfterCsrfValidation()
          throws Exception {
      MvcResult csrfPrimingResult = mockMvc.perform(get("/api/v1/funding-listings"))
              .andExpect(status().isOk())
              .andReturn();
      Cookie xsrfCookie = csrfPrimingResult.getResponse().getCookie("XSRF-TOKEN");
      assertThat(xsrfCookie).isNotNull();

      mockMvc.perform(post(
                      "/api/v1/payment-attempts/1/actions/local-confirm")
                      .cookie(xsrfCookie)
                      .header("X-CSRF-TOKEN", xsrfCookie.getValue())
                      .header("X-Request-Id", REQUEST_ID))
              .andExpect(status().isUnauthorized())
              .andExpect(content().contentType(
                      MediaType.APPLICATION_PROBLEM_JSON))
              .andExpect(jsonPath("$.code").value(
                      "AUTHENTICATION_REQUIRED"))
              .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
              .andExpect(jsonPath("$.success").doesNotExist())
              .andExpect(jsonPath("$.error").doesNotExist());
  }
  ```

- [x] **Step 4: Run the focused tests and preserve meaningful RED evidence**

  ```powershell
  .\mvnw.cmd -B -Pintegration-tests `
    "-Dit.test=FinancialSecurityApiIT" verify
  ```

  Expected RED: the requests reach financial controller null guards and return
  the legacy JSON error envelope or legacy content type instead of the shared
  RFC 9457 response. There must be no compilation, container, or unrelated
  application-startup failure.

- [x] **Step 5: Commit the RED contract**

  ```powershell
  git add src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java
  git commit -m "test: expose financial authentication boundary gap (KAN-31)"
  ```

---

## Task 2: Enforce authentication in the Spring Security route policy

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/security/infrastructure/config/SecurityConfig.java:39-74`
- Test:
  `src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java`
- Regression:
  `src/test/java/com/project/optrabidz/security/api/SecurityApiIT.java`

**Consumes:** The failing KAN-31 contract and existing
`ProblemAuthenticationEntryPoint` wiring.

**Produces:** Method-independent authenticated matchers for all five financial
user route families, ordered after the provider-webhook `permitAll` rule and
before `.anyRequest().permitAll()`.

- [x] **Step 1: Add the minimal route matcher**

  In `SecurityConfig.authorizeHttpRequests`, immediately after the provider
  webhook `permitAll` matcher, add:

  ```java
  .requestMatchers(
          "/api/v1/settlements/**",
          "/api/v1/repayments/**",
          "/api/v1/repayment-installments/**",
          "/api/v1/payment-intents/**",
          "/api/v1/payment-attempts/**"
  ).authenticated()
  ```

  Do not broaden this to `/api/v1/**`. Do not change the existing webhook,
  marketplace, profile, classification, admin, or fallback matchers.

- [x] **Step 2: Run the focused authentication contract**

  ```powershell
  .\mvnw.cmd -B -Pintegration-tests `
    "-Dit.test=FinancialSecurityApiIT" verify
  ```

  Expected GREEN: 5 tests pass; each request is stopped by Spring Security and
  uses `application/problem+json` without the legacy envelope.

- [x] **Step 3: Run shared security regressions**

  ```powershell
  .\mvnw.cmd -B -Pintegration-tests `
    "-Dit.test=SecurityApiIT" verify
  ```

  Expected: existing authentication, authorization, CSRF, audit-facing, and
  provider-webhook tests pass. In particular, the provider webhook remains
  reachable without a browser session or CSRF token and fails through provider
  signature verification when the signature is missing.

- [x] **Step 4: Commit the security policy change**

  ```powershell
  git add src/main/java/com/project/optrabidz/security/infrastructure/config/SecurityConfig.java
  git commit -m "fix: secure financial user endpoints (KAN-31)"
  ```

---

## Task 3: Remove controller-local authentication responsibility

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/financial/api/FinancialController.java:3-246`
- Modify:
  `src/main/java/com/project/optrabidz/financial/api/LocalPaymentSimulationController.java:3-56`
- Modify:
  `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Test:
  `src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java`

**Consumes:** The now-enforced route policy and established
`AuthenticatedUserPrincipal`.

**Produces:** Financial user controllers that map requests and delegate but do
not authenticate callers or construct authentication failures.

- [x] **Step 1: Add a focused architecture rule first**

  Add the following rule to `ExceptionArchitectureTest`:

  ```java
  @ArchTest
  static final ArchRule FINANCIAL_USER_CONTROLLERS_DO_NOT_USE_LEGACY_ERRORS =
          noClasses()
                  .that().resideInAPackage("..financial.api..")
                  .and().haveNameMatching(
                          ".*\\.(FinancialController|LocalPaymentSimulationController)"
                  )
                  .should().dependOnClassesThat().resideInAPackage(
                          "..common.api.exception.."
                  )
                  .as("financial user controllers must delegate authentication to Spring Security");
  ```

- [x] **Step 2: Run the architecture rule and verify RED**

  ```powershell
  .\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
  ```

  Expected RED: the new rule reports the `ApiException` and `ErrorCode`
  dependencies from exactly `FinancialController` and
  `LocalPaymentSimulationController`. It must not report
  `PaymentProviderWebhookController`, which remains outside this prerequisite.

- [x] **Step 3: Remove authentication construction from `FinancialController`**

  Remove these imports:

  ```java
  import com.project.optrabidz.common.api.exception.ApiException;
  import com.project.optrabidz.common.api.exception.ErrorCode;
  ```

  In every endpoint, remove:

  ```java
  AuthenticatedUserPrincipal user = requirePrincipal(principal);
  ```

  Replace only the local variable reads:

  ```java
  user.getAccountId()
  user.getRole()
  ```

  with:

  ```java
  principal.getAccountId()
  principal.getRole()
  ```

  Delete the complete private `requirePrincipal` method. Do not change endpoint
  paths, request parameters, response wrappers, status behavior, or service
  signatures.

- [x] **Step 4: Remove authentication construction from the local simulator**

  Apply the same import, local-variable, and private-helper removal to
  `LocalPaymentSimulationController`. Preserve `@ConditionalOnProperty` and its
  current property name, expected value, and default behavior exactly.

- [x] **Step 5: Run architecture and focused security tests**

  ```powershell
  .\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
  .\mvnw.cmd -B -Pintegration-tests `
    "-Dit.test=FinancialSecurityApiIT" verify
  ```

  Expected GREEN: architecture rules pass; anonymous financial user requests
  still stop at Spring Security; neither controller depends on the legacy error
  package.

- [x] **Step 6: Confirm the source boundary mechanically**

  ```powershell
  rg -n "requirePrincipal|ApiException|ErrorCode" `
    src/main/java/com/project/optrabidz/financial/api/FinancialController.java `
    src/main/java/com/project/optrabidz/financial/api/LocalPaymentSimulationController.java
  ```

  Expected: no matches. Do not require the same result for the webhook
  controller or the complete financial module; KAN-30 will migrate those
  remaining legacy dependencies.

- [x] **Step 7: Commit the controller cleanup**

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/financial/api/FinancialController.java `
    src/main/java/com/project/optrabidz/financial/api/LocalPaymentSimulationController.java `
    src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java
  git commit -m "refactor: remove financial controller authentication guards (KAN-31)"
  ```

---

## Task 4: Preserve authenticated, CSRF, and provider trust boundaries

**Files:**

- Modify:
  `src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java`
- Regression:
  `src/test/java/com/project/optrabidz/security/api/SecurityApiIT.java`
- Regression:
  `src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java`

**Consumes:** The completed route and controller boundaries.

**Produces:** Proof that authentication moved without altering application,
CSRF, local-simulation, or provider-webhook behavior.

- [x] **Step 1: Add an authenticated continuation test**

  Add this method to `FinancialSecurityApiIT`:

  ```java
  @Test
  void authenticatedFinancialRequestReachesTheApplicationBoundary()
          throws Exception {
      AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

      mockMvc.perform(get("/api/v1/settlements/9223372036854775807")
                      .session(investor.session())
                      .cookie(investor.xsrfCookie()))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.error.code").value(
                      "RESOURCE_NOT_FOUND"));
  }
  ```

  The temporary legacy business-error assertion is intentional: it proves the
  authenticated request passed Spring Security and reached the unchanged
  financial service. KAN-30 will migrate that business response separately.

- [x] **Step 2: Add authenticated CSRF-pass and CSRF-reject tests**

  Add these methods:

  ```java
  @Test
  void csrfValidAuthenticatedPaymentMutationReachesFinancialService()
          throws Exception {
      AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

      mockMvc.perform(post("/api/v1/payment-intents/9223372036854775807/attempts")
                      .session(investor.session())
                      .cookie(investor.xsrfCookie())
                      .header("X-CSRF-TOKEN", investor.csrfToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{}"))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.error.code").value(
                      "RESOURCE_NOT_FOUND"));
  }

  @Test
  void authenticatedPaymentMutationStillRequiresCsrf() throws Exception {
      AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

      mockMvc.perform(post("/api/v1/payment-intents/1/attempts")
                      .session(investor.session())
                      .cookie(investor.xsrfCookie())
                      .header("X-Request-Id", REQUEST_ID)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{}"))
              .andExpect(status().isForbidden())
              .andExpect(content().contentType(
                      MediaType.APPLICATION_PROBLEM_JSON))
              .andExpect(jsonPath("$.code").value(
                      "CSRF_VALIDATION_FAILED"))
              .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
  }
  ```

- [x] **Step 3: Run focused boundary tests**

  ```powershell
  .\mvnw.cmd -B -Pintegration-tests `
    "-Dit.test=FinancialSecurityApiIT,SecurityApiIT" verify
  ```

  Expected: the complete focused authentication, CSRF, disclosure, and webhook
  set passes. No request leaks the bearer marker or security internals.

- [x] **Step 4: Run existing financial API regressions**

  ```powershell
  .\mvnw.cmd -B -Pintegration-tests `
    "-Dit.test=FinancialApiIT" verify
  ```

  Expected: authenticated settlement, repayment, installment, payment intent,
  payment attempt, ownership, provider webhook, and local simulation flows pass
  unchanged against PostgreSQL and Flyway V1.

- [x] **Step 5: Commit the boundary regression coverage**

  ```powershell
  git add src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java
  git commit -m "test: verify financial security boundaries (KAN-31)"
  ```

---

## Task 5: Complete verification and prepare the review handoff

**Files:**

- Modify:
  `docs/error-handling/work-items/KAN-31-financial-security-boundary/implementation-plan.md`
- Verify: all KAN-31 production, test, and documentation files

**Consumes:** All prior implementation tasks.

**Produces:** Exact verification evidence, a clean review branch, and a pull
request targeting `develop` without any merge.

- [x] **Step 1: Run the complete unit and architecture suite**

  ```powershell
  .\mvnw.cmd -B test
  ```

  Expected: every unit, documentation, and architecture test passes with zero
  failures and errors.

- [x] **Step 2: Run the complete PostgreSQL integration suite**

  ```powershell
  .\mvnw.cmd -B -Pintegration-tests verify
  ```

  Expected: all Testcontainers PostgreSQL integration tests pass against
  unchanged Flyway V1. Record exact test totals and duration in this plan.

- [x] **Step 3: Inspect the final scope and repository state**

  ```powershell
  git status --short --branch
  git diff --check origin/develop...HEAD
  git diff --stat origin/develop...HEAD
  git diff --name-status origin/develop...HEAD
  git log --oneline origin/develop..HEAD
  git rev-parse origin/main
  ```

  Expected: only KAN-31 documentation, focused tests, `SecurityConfig`, the two
  financial user controllers, and the scoped architecture test differ from
  `develop`; the worktree is clean; `main` remains unchanged.

- [x] **Step 4: Record final evidence in this plan**

  Change the plan status to `Implemented and locally verified; awaiting
  pull-request review`. Check completed steps and append exact focused/full
  test totals, final branch head, unchanged `origin/main`, and the final diff
  scope. Do not add machine-specific paths, credentials, internal process
  commentary, raw logs, or temporary files.

- [x] **Step 5: Commit and push the evidence**

  ```powershell
  git add docs/error-handling/work-items/KAN-31-financial-security-boundary/implementation-plan.md
  git commit -m "docs: record financial security verification (KAN-31)"
  git push origin feature/KAN-31-financial-security-boundary
  ```

- [x] **Step 6: Create the pull request without merging**

  ```powershell
  $prBody = @"
  ## Summary
  - require authentication for every financial user route family
  - remove controller-local authentication guards
  - preserve CSRF, business authorization, and provider-signature boundaries

  ## Verification
  - focused financial security integration tests passed
  - complete unit and architecture suite passed
  - complete PostgreSQL integration suite passed

  ## Risk and rollback
  Route-policy and controller-boundary change only. Payment-provider webhooks
  remain session-public, CSRF-exempt, and signature-verified. Roll back by
  reverting this pull request.
  "@

  gh pr create `
    --base develop `
    --head feature/KAN-31-financial-security-boundary `
    --title "KAN-31: Enforce financial authentication at the security boundary" `
    --body $prBody
  ```

  Stop after the PR, exact-head CI, and Jira review handoff; do not merge.

## Completion gate

KAN-31 is ready for pull-request review only when:

- the five financial user route families require authentication;
- both financial user controllers contain no authentication null guard or
  legacy authentication-error construction;
- anonymous, authenticated, CSRF, webhook, local-simulation, disclosure, and
  architecture tests pass;
- the full unit/architecture and PostgreSQL integration suites pass;
- the final diff contains no KAN-30 business-exception migration or unrelated
  change;
- the remote branch and Jira evidence reference the exact reviewed head; and
- `main` remains unchanged.

## Verification evidence

- Meaningful RED: 5 focused requests reached the two financial controllers and
  returned the legacy JSON error envelope instead of the shared RFC 9457
  authentication response. Container and application startup were successful.
- Route-policy GREEN: `FinancialSecurityApiIT` passed 5/5 and the existing
  `SecurityApiIT` passed 10/10.
- Controller-boundary RED: the architecture rule reported exactly four legacy
  dependency accesses from `FinancialController.requirePrincipal` and
  `LocalPaymentSimulationController.requirePrincipal`.
- Controller-boundary GREEN: all 4 architecture rules passed, and the source
  scan found no `requirePrincipal`, `ApiException`, or `ErrorCode` in the two
  financial user controllers.
- Focused final security regression: 18/18 passed in one application context
  (`FinancialSecurityApiIT` 8/8 and `SecurityApiIT` 10/10).
- Existing financial API regression: 14/14 passed.
- Complete unit, documentation, and architecture suite: 258/258 passed with no
  failures, errors, or skips in 24.374 seconds.
- Complete PostgreSQL/Testcontainers integration suite: 97/97 passed with no
  failures, errors, or skips in 2 minutes 1 second; Flyway V1 remained
  unchanged.
- Verified implementation head before this evidence commit: `ab72218`.
- Verified base: `origin/develop` at
  `71a252b3a8f4d7cca40ff1e44da94deb171f90d4`.
- Verified protected production baseline: `origin/main` at
  `bc7727b0b2e09ebbfef8b9c6c5dc729cd4aab4fb`.
- Review pull request: [#27](https://github.com/kundankumar7/optrabidz/pull/27),
  targeting `develop` without a merge.
- Final scope contains only KAN-31 documentation and diagram assets, the five
  financial route matchers, the two financial user-controller cleanups, the
  scoped architecture rule, and focused financial security tests.
- The test plan uses the application's cookie/header CSRF flow instead of the
  session-based test request processor, preventing repository contamination
  when security integration classes share one application context.
