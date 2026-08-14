# KAN-20: Neutral Error Contract Implementation Plan

**Goal:** Introduce a framework-free error contract and enforce the first
exception architecture boundaries without changing any existing HTTP response.

**Source:** KAN-20 and
`docs/design/KAN-17-exception-handling-foundation-design.md`, sections 5, 6.1,
11, 13, and 14.

**Architecture:** The new `common.error` package contains only Java value types
and a reusable application exception. A strict ArchUnit rule protects that new
package. A frozen migration rule records existing HTTP-coupled module
exceptions, rejects new violations immediately, and shrinks as later stories
migrate the legacy code.

**Technology:** Java 21, Maven, JUnit 5, AssertJ, ArchUnit 1.4.2, Spring Boot
3.3.2, Testcontainers, and PostgreSQL 16.

## Global constraints

- The branch is `feature/KAN-20-neutral-error-contract`, based on verified
  `develop` commit `7589e9ce04b0c210942e45f2050d71c950dccf0d`.
- `main` is not modified.
- Existing endpoint statuses, content types, bodies, and security responses do
  not change.
- `ApiException`, `ErrorCode`, `ErrorField`, `ErrorResponse`, and
  `GlobalExceptionHandler` remain until later migration stories.
- No production module exception is migrated in KAN-20.
- ArchUnit is test-scoped and is not packaged in the application artifact.
- No database migration, datasource setting, scheduler setting, or runtime
  profile changes.
- Every implementation step follows RED, minimal GREEN, refactor, and focused
  verification before a commit.

## File map

| File | Responsibility |
|---|---|
| `pom.xml` | Pin ArchUnit 1.4.2 and add its JUnit 5 integration in test scope. |
| `src/main/java/com/project/optrabidz/common/error/ErrorCategory.java` | Transport-neutral error classification. |
| `src/main/java/com/project/optrabidz/common/error/ErrorDescriptor.java` | Immutable public code, category, and safe fixed message. |
| `src/main/java/com/project/optrabidz/common/error/ErrorDetail.java` | Immutable allowlisted field issue. |
| `src/main/java/com/project/optrabidz/common/error/ApplicationException.java` | Descriptor plus protected diagnostic context and optional cause. |
| `src/test/java/com/project/optrabidz/common/error/ErrorDescriptorTest.java` | Value-contract and validation tests. |
| `src/test/java/com/project/optrabidz/common/error/ApplicationExceptionTest.java` | Diagnostic separation, defensive-copy, cause, and extension tests. |
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Strict neutral-package rule and frozen legacy migration rule. |
| `src/test/resources/archunit.properties` | Version-controlled freeze-store policy. |
| `src/test/resources/archunit-store/` | Generated baseline of known legacy violations. |

---

## Task 1: Establish the architecture-test dependency and legacy debt baseline

**Produces:** A checked-in ArchUnit migration rule that fails for any new
transport dependency while allowing only the exact legacy violations already
present at the branch baseline.

### Steps

- [x] Confirm the branch, clean worktree, and exact base.

  ```powershell
  git branch --show-current
  git status --short
  git rev-parse HEAD
  git rev-parse origin/develop
  ```

  Expected: branch `feature/KAN-20-neutral-error-contract`; clean worktree;
  local base and `origin/develop` both resolve to the recorded base unless a
  reviewed rebase decision is made before implementation.

- [x] Add the version property under the existing Maven properties.

  ```xml
  <archunit.version>1.4.2</archunit.version>
  ```

- [x] Add the test-only dependency beside the other test dependencies.

  ```xml
  <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
      <version>${archunit.version}</version>
      <scope>test</scope>
  </dependency>
  ```

- [x] Create
  `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
  first with the unfrozen rule below.

  ```java
  package com.project.optrabidz.architecture;

  import com.tngtech.archunit.core.importer.ImportOption;
  import com.tngtech.archunit.junit.AnalyzeClasses;
  import com.tngtech.archunit.junit.ArchTest;
  import com.tngtech.archunit.lang.ArchRule;

  import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

  @AnalyzeClasses(
          packages = "com.project.optrabidz",
          importOptions = ImportOption.DoNotIncludeTests.class
  )
  class ExceptionArchitectureTest {
      @ArchTest
      static final ArchRule BUSINESS_EXCEPTIONS_ARE_TRANSPORT_NEUTRAL =
              noClasses()
                      .that().resideInAnyPackage("..domain..", "..application..")
                      .and().haveSimpleNameEndingWith("Exception")
                      .should().dependOnClassesThat().resideInAnyPackage(
                              "..common.api..",
                              "org.springframework.http..",
                              "org.springframework.web..",
                              "jakarta.servlet.."
                      )
                      .as("domain and application exceptions must remain transport-neutral");
  }
  ```

- [x] Run the strict rule to capture meaningful RED evidence.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
  ```

  Expected: FAIL listing current exception dependencies on `ApiException`,
  `ErrorCode`, or Spring HTTP types. A missing dependency or test-discovery
  error is not acceptable RED evidence.

- [x] Wrap only the legacy rule with `FreezingArchRule`.

  ```java
  import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
  ```

  Replace the rule assignment with:

  ```java
  static final ArchRule BUSINESS_EXCEPTIONS_ARE_TRANSPORT_NEUTRAL =
          freeze(noClasses()
                  .that().resideInAnyPackage("..domain..", "..application..")
                  .and().haveSimpleNameEndingWith("Exception")
                  .should().dependOnClassesThat().resideInAnyPackage(
                          "..common.api..",
                          "org.springframework.http..",
                          "org.springframework.web..",
                          "jakarta.servlet.."
                  )
                  .as("domain and application exceptions must remain transport-neutral"));
  ```

- [x] Create `src/test/resources/archunit.properties`.

  ```properties
  freeze.store.default.path=src/test/resources/archunit-store
  freeze.store.default.allowStoreCreation=false
  freeze.store.default.allowStoreUpdate=false
  ```

- [x] Initialize the baseline once from the reviewed current violations.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" `
    "-Darchunit.freeze.store.default.allowStoreCreation=true" `
    "-Darchunit.freeze.store.default.allowStoreUpdate=true" test
  ```

  Expected: PASS and generated tracked files under
  `src/test/resources/archunit-store/`. Review their contents; they may contain
  class names and dependency descriptions only, never credentials or runtime
  data.

- [x] Prove ordinary execution cannot recreate or silently update the store.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
  git status --short
  ```

  Expected: PASS with no newly generated changes after the initialized store is
  staged. Later migration stories must explicitly allow a one-time store update
  when removing recorded violations.

- [x] Commit this independently reviewable guardrail.

  ```powershell
  git add pom.xml `
    src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
    src/test/resources/archunit.properties `
    src/test/resources/archunit-store
  git commit -m "test: establish exception architecture guardrails (KAN-20)"
  ```

---

## Task 2: Add neutral descriptor and detail value types

**Consumes:** Java 21 and the architecture-test setup from Task 1.

**Produces:**

```java
new ErrorDescriptor(String code, ErrorCategory category, String publicMessage)
new ErrorDetail(String field, String issue)
```

### Steps

- [x] Create
  `src/test/java/com/project/optrabidz/common/error/ErrorDescriptorTest.java`
  with focused tests before creating production types.

  ```java
  package com.project.optrabidz.common.error;

  import org.junit.jupiter.api.Test;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  class ErrorDescriptorTest {
      @Test
      void exposesStablePublicContract() {
          ErrorDescriptor descriptor = new ErrorDescriptor(
                  "LISTING_NOT_FOUND",
                  ErrorCategory.NOT_FOUND,
                  "The listing was not found"
          );

          assertThat(descriptor.code()).isEqualTo("LISTING_NOT_FOUND");
          assertThat(descriptor.category()).isEqualTo(ErrorCategory.NOT_FOUND);
          assertThat(descriptor.publicMessage()).isEqualTo("The listing was not found");
      }

      @Test
      void rejectsMissingOrUnstableDescriptorValues() {
          assertThatThrownBy(() -> new ErrorDescriptor(null, ErrorCategory.NOT_FOUND, "Safe"))
                  .isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(() -> new ErrorDescriptor("listing-not-found", ErrorCategory.NOT_FOUND, "Safe"))
                  .isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(() -> new ErrorDescriptor("LISTING_NOT_FOUND", null, "Safe"))
                  .isInstanceOf(NullPointerException.class);
          assertThatThrownBy(() -> new ErrorDescriptor("LISTING_NOT_FOUND", ErrorCategory.NOT_FOUND, " "))
                  .isInstanceOf(IllegalArgumentException.class);
      }

      @Test
      void createsImmutableAllowlistedDetail() {
          ErrorDetail detail = new ErrorDetail("title", "must not be blank");

          assertThat(detail.field()).isEqualTo("title");
          assertThat(detail.issue()).isEqualTo("must not be blank");
      }

      @Test
      void rejectsIncompleteDetail() {
          assertThatThrownBy(() -> new ErrorDetail(" ", "must not be blank"))
                  .isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(() -> new ErrorDetail("title", null))
                  .isInstanceOf(IllegalArgumentException.class);
      }
  }
  ```

- [x] Run the focused test and record RED caused only by missing neutral types.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ErrorDescriptorTest" test
  ```

- [x] Create `ErrorCategory.java` with the categories approved in KAN-17.

  ```java
  package com.project.optrabidz.common.error;

  public enum ErrorCategory {
      VALIDATION,
      AUTHENTICATION,
      AUTHORIZATION,
      NOT_FOUND,
      CONFLICT,
      BUSINESS_RULE,
      INTERNAL
  }
  ```

- [x] Create `ErrorDescriptor.java`.

  ```java
  package com.project.optrabidz.common.error;

  import java.util.Objects;
  import java.util.regex.Pattern;

  public record ErrorDescriptor(
          String code,
          ErrorCategory category,
          String publicMessage
  ) {
      private static final Pattern PUBLIC_CODE = Pattern.compile("[A-Z][A-Z0-9_]*");

      public ErrorDescriptor {
          if (code == null || !PUBLIC_CODE.matcher(code).matches()) {
              throw new IllegalArgumentException(
                      "code must use upper snake case and must not be blank"
              );
          }
          Objects.requireNonNull(category, "category must not be null");
          if (publicMessage == null || publicMessage.isBlank()) {
              throw new IllegalArgumentException("publicMessage must not be blank");
          }
          publicMessage = publicMessage.strip();
      }
  }
  ```

- [x] Create `ErrorDetail.java`.

  ```java
  package com.project.optrabidz.common.error;

  public record ErrorDetail(String field, String issue) {
      public ErrorDetail {
          if (field == null || field.isBlank()) {
              throw new IllegalArgumentException("field must not be blank");
          }
          if (issue == null || issue.isBlank()) {
              throw new IllegalArgumentException("issue must not be blank");
          }
          field = field.strip();
          issue = issue.strip();
      }
  }
  ```

- [x] Run focused tests and the architecture guardrail.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ErrorDescriptorTest,ExceptionArchitectureTest" test
  ```

  Expected: PASS. The types import only Java packages.

- [x] Commit the neutral value contract.

  ```powershell
  git add src/main/java/com/project/optrabidz/common/error `
    src/test/java/com/project/optrabidz/common/error/ErrorDescriptorTest.java
  git commit -m "feat: add neutral error descriptors (KAN-20)"
  ```

---

## Task 3: Add the transport-neutral application exception

**Consumes:** `ErrorDescriptor` and `ErrorDetail` from Task 2.

**Produces:**

```java
new ApplicationException(
    ErrorDescriptor descriptor,
    String diagnosticCode,
    String diagnosticMessage,
    List<ErrorDetail> details,
    Throwable cause
)
```

and convenience constructors without details or cause.

### Steps

- [x] Create
  `src/test/java/com/project/optrabidz/common/error/ApplicationExceptionTest.java`
  before the production class.

  ```java
  package com.project.optrabidz.common.error;

  import org.junit.jupiter.api.Test;

  import java.util.ArrayList;
  import java.util.List;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  class ApplicationExceptionTest {
      private static final ErrorDescriptor LISTING_NOT_FOUND = new ErrorDescriptor(
              "LISTING_NOT_FOUND",
              ErrorCategory.NOT_FOUND,
              "The listing was not found"
      );

      @Test
      void keepsPublicContractSeparateFromProtectedDiagnostics() {
          IllegalStateException cause = new IllegalStateException("database detail");
          List<ErrorDetail> source = new ArrayList<>(List.of(
                  new ErrorDetail("listingId", "was not found")
          ));

          ApplicationException exception = new ApplicationException(
                  LISTING_NOT_FOUND,
                  "MARKETPLACE.LISTING_LOOKUP_FAILED",
                  "Listing 42 was absent during bid placement",
                  source,
                  cause
          );
          source.clear();

          assertThat(exception.descriptor()).isSameAs(LISTING_NOT_FOUND);
          assertThat(exception.diagnosticCode())
                  .isEqualTo("MARKETPLACE.LISTING_LOOKUP_FAILED");
          assertThat(exception.getMessage())
                  .isEqualTo("Listing 42 was absent during bid placement")
                  .isNotEqualTo(exception.descriptor().publicMessage());
          assertThat(exception.details())
                  .containsExactly(new ErrorDetail("listingId", "was not found"));
          assertThat(exception.getCause()).isSameAs(cause);
          assertThatThrownBy(() -> exception.details().add(
                  new ErrorDetail("probe", "must be rejected")
          )).isInstanceOf(UnsupportedOperationException.class);
      }

      @Test
      void defaultsOptionalDetailsAndCause() {
          ApplicationException exception = new ApplicationException(
                  LISTING_NOT_FOUND,
                  "MARKETPLACE.LISTING_NOT_FOUND",
                  "Listing lookup returned no row"
          );

          assertThat(exception.details()).isEmpty();
          assertThat(exception.getCause()).isNull();
      }

      @Test
      void rejectsInvalidDiagnosticContract() {
          assertThatThrownBy(() -> new ApplicationException(
                  LISTING_NOT_FOUND, "invalid code", "Diagnostic"
          )).isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(() -> new ApplicationException(
                  LISTING_NOT_FOUND, "MARKETPLACE.FAILURE", " "
          )).isInstanceOf(IllegalArgumentException.class);
          assertThatThrownBy(() -> new ApplicationException(
                  null, "MARKETPLACE.FAILURE", "Diagnostic"
          )).isInstanceOf(NullPointerException.class);
      }

      @Test
      void supportsModuleOwnedExceptionTypes() {
          ListingNotFoundException exception = new ListingNotFoundException(42L);

          assertThat(exception.descriptor()).isSameAs(LISTING_NOT_FOUND);
          assertThat(exception.getMessage()).contains("42");
      }

      private static final class ListingNotFoundException extends ApplicationException {
          private ListingNotFoundException(long listingId) {
              super(
                      LISTING_NOT_FOUND,
                      "MARKETPLACE.LISTING_NOT_FOUND",
                      "Listing " + listingId + " was not found"
              );
          }
      }
  }
  ```

- [x] Run the focused test and record RED caused by missing
  `ApplicationException`.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ApplicationExceptionTest" test
  ```

- [x] Create `ApplicationException.java`.

  ```java
  package com.project.optrabidz.common.error;

  import java.util.List;
  import java.util.Objects;
  import java.util.regex.Pattern;

  public class ApplicationException extends RuntimeException {
      private static final Pattern DIAGNOSTIC_CODE =
              Pattern.compile("[A-Z][A-Z0-9_.-]*");

      private final ErrorDescriptor descriptor;
      private final String diagnosticCode;
      private final List<ErrorDetail> details;

      public ApplicationException(
              ErrorDescriptor descriptor,
              String diagnosticCode,
              String diagnosticMessage
      ) {
          this(descriptor, diagnosticCode, diagnosticMessage, List.of(), null);
      }

      public ApplicationException(
              ErrorDescriptor descriptor,
              String diagnosticCode,
              String diagnosticMessage,
              Throwable cause
      ) {
          this(descriptor, diagnosticCode, diagnosticMessage, List.of(), cause);
      }

      public ApplicationException(
              ErrorDescriptor descriptor,
              String diagnosticCode,
              String diagnosticMessage,
              List<ErrorDetail> details
      ) {
          this(descriptor, diagnosticCode, diagnosticMessage, details, null);
      }

      public ApplicationException(
              ErrorDescriptor descriptor,
              String diagnosticCode,
              String diagnosticMessage,
              List<ErrorDetail> details,
              Throwable cause
      ) {
          super(requireDiagnosticMessage(diagnosticMessage), cause);
          this.descriptor = Objects.requireNonNull(
                  descriptor,
                  "descriptor must not be null"
          );
          if (diagnosticCode == null
                  || !DIAGNOSTIC_CODE.matcher(diagnosticCode).matches()) {
              throw new IllegalArgumentException(
                      "diagnosticCode must use uppercase segments"
              );
          }
          this.diagnosticCode = diagnosticCode;
          this.details = details == null ? List.of() : List.copyOf(details);
      }

      public ErrorDescriptor descriptor() {
          return descriptor;
      }

      public String diagnosticCode() {
          return diagnosticCode;
      }

      public List<ErrorDetail> details() {
          return details;
      }

      private static String requireDiagnosticMessage(String diagnosticMessage) {
          if (diagnosticMessage == null || diagnosticMessage.isBlank()) {
              throw new IllegalArgumentException(
                      "diagnosticMessage must not be blank"
              );
          }
          return diagnosticMessage.strip();
      }
  }
  ```

- [x] Run the neutral-contract tests and architecture baseline.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=ErrorDescriptorTest,ApplicationExceptionTest,ExceptionArchitectureTest" `
    test
  ```

  Expected: PASS with the legacy freeze store unchanged.

- [x] Commit the application exception.

  ```powershell
  git add `
    src/main/java/com/project/optrabidz/common/error/ApplicationException.java `
    src/test/java/com/project/optrabidz/common/error/ApplicationExceptionTest.java
  git commit -m "feat: add transport-neutral application exception (KAN-20)"
  ```

---

## Task 4: Enforce the strict neutral-package boundary

**Consumes:** The complete `common.error` package from Tasks 2 and 3.

**Produces:** A non-frozen rule that permits dependencies only on Java and the
neutral package itself. The class importer excludes test output so same-package
JUnit and AssertJ dependencies are not mistaken for production dependencies.

### Steps

- [x] Add this second rule to `ExceptionArchitectureTest`.

  ```java
  @ArchTest
  static final ArchRule NEUTRAL_ERROR_CONTRACT_IS_FRAMEWORK_FREE =
          classes()
                  .that().resideInAPackage("..common.error..")
                  .should().onlyDependOnClassesThat().resideInAnyPackage(
                          "java..",
                          "..common.error.."
                  )
                  .as("the neutral error contract may depend only on Java and itself");
  ```

  Add the static import:

  ```java
  import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
  ```

- [x] Prove the strict rule detects an adapter dependency. Temporarily add the
  following probe to `ErrorDescriptor` without committing it:

  ```java
  import org.springframework.http.HttpStatus;

  private static final HttpStatus ARCHITECTURE_TEST_PROBE = HttpStatus.BAD_REQUEST;
  ```

- [x] Run the architecture test and record mutation RED.

  ```powershell
  .\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
  ```

  Expected: FAIL naming the forbidden `HttpStatus` dependency. Failure caused
  by compilation or freeze-store configuration is not valid evidence.

- [x] Remove the temporary import and field exactly; confirm the production
  diff contains no probe.

  ```powershell
  rg -n "ARCHITECTURE_TEST_PROBE|HttpStatus" `
    src/main/java/com/project/optrabidz/common/error
  ```

  Expected: no matches.

- [x] Run architecture and neutral-contract tests again.

  ```powershell
  .\mvnw.cmd -B `
    "-Dtest=ExceptionArchitectureTest,ErrorDescriptorTest,ApplicationExceptionTest" `
    test
  git diff --check
  ```

  Expected: PASS and a clean diff check.

- [x] Commit the strict rule.

  ```powershell
  git add src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java
  git commit -m "test: enforce neutral error package boundary (KAN-20)"
  ```

---

## Task 5: Run regression, disclosure, packaging, and scope gates

### Steps

- [x] Confirm ArchUnit is test-scoped and resolves to 1.4.2.

  ```powershell
  .\mvnw.cmd -B dependency:tree "-Dincludes=com.tngtech.archunit"
  ```

  Expected: `archunit-junit5:1.4.2:test`; no compile or runtime ArchUnit entry.

- [x] Run all unit and architecture tests.

  ```powershell
  .\mvnw.cmd -B test
  ```

  Expected: BUILD SUCCESS with zero failures and errors.

- [x] Run the production-path PostgreSQL integration suite.

  ```powershell
  .\mvnw.cmd -B verify -Pintegration-tests
  ```

  Expected: BUILD SUCCESS; unit and architecture tests pass again, all existing
  PostgreSQL integration tests pass, Flyway validates V1, and no schema change
  is applied beyond the existing migration.

- [x] Verify the packaged JAR does not contain ArchUnit classes.

  ```powershell
  jar tf target\optrabidz-0.0.1-SNAPSHOT.jar |
    Select-String 'com/tngtech/archunit'
  ```

  Expected: no matches.

- [x] Run explicit dependency and disclosure scans.

  ```powershell
  rg -n "org\.springframework|jakarta\.servlet|jakarta\.persistence|com\.fasterxml" `
    src/main/java/com/project/optrabidz/common/error
  rg -n "getMessage\(\).*public|publicMessage.*getMessage" `
    src/main/java/com/project/optrabidz/common/error `
    src/test/java/com/project/optrabidz/common/error
  ```

  Expected: no framework imports and no code that treats diagnostic messages as
  public messages.

- [x] Audit the exact branch scope.

  ```powershell
  git diff --check origin/develop...HEAD
  git diff --name-only origin/develop...HEAD
  git status --short
  git log --oneline origin/develop..HEAD
  ```

  Expected: only the plan, Maven test dependency, neutral contract, focused
  tests, ArchUnit configuration, and frozen-store files. There must be no
  changes to controllers, security configuration, handlers, business modules,
  database migrations, runtime configuration, or CI.

- [x] Record exact test totals, the initialized legacy violation count, the
  zero-new-violation result, and the verified branch SHA in KAN-20.

- [x] Push the branch and open a pull request into `develop` only after all
  local gates pass. Require both exact-head GitHub checks before review.

  ```powershell
  git push -u origin feature/KAN-20-neutral-error-contract
  $prBody = @'
  ## Jira

  [KAN-20](https://0707manna0895.atlassian.net/browse/KAN-20)

  ## Change

  - Adds the transport-neutral `common.error` contract.
  - Adds strict neutral-package architecture enforcement.
  - Freezes existing exception coupling so no new violations can be added.

  ## Evidence

  - Architecture policy RED and mutation RED were recorded.
  - Focused neutral-contract and architecture tests pass.
  - Full unit and PostgreSQL integration suites pass.
  - ArchUnit is test-scoped and absent from the application JAR.

  ## Boundary

  Existing HTTP and security responses are unchanged. Legacy exception types
  remain for later migration stories. No database or runtime configuration
  changes are included.

  ## Rollback

  Revert this PR through a reviewed follow-up PR.
  '@
  gh pr create `
    --base develop `
    --head feature/KAN-20-neutral-error-contract `
    --title "KAN-20: Introduce the transport-neutral error contract" `
    --body $prBody
  ```

## Rollback

Revert the KAN-20 merge through a reviewed pull request. Because this story
changes no endpoint, database, or runtime configuration, rollback removes the
new neutral types, test-only ArchUnit dependency, and architecture baseline
without data recovery or compatibility work.

## References

- [ArchUnit 1.4.2 release](https://github.com/TNG/ArchUnit/releases/tag/v1.4.2)
- [ArchUnit freezing rules guide](https://www.archunit.org/userguide/html/000_Index.html#_freezing_arch_rules)
- `docs/design/KAN-17-exception-handling-foundation-design.md`
