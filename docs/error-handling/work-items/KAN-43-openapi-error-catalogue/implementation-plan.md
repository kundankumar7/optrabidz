# KAN-43 OpenAPI Problem Details Contract and Public Error Catalogue Implementation Plan

**Goal:** Publish the existing safe Problem Details behavior through a
verified OpenAPI contract and deterministic repository error catalogue while
keeping documentation unavailable by default in production.

**Architecture:** Module catalogues remain the owners of framework-free error
descriptors. A top-level documentation adapter explicitly composes module,
framework, and security definitions into one immutable public catalogue;
Springdoc and Markdown publication derive from that catalogue. A dedicated,
higher-priority documentation security chain enforces a validated fail-closed
exposure policy without introducing dependencies from feature modules back to
the documentation adapter.

**Tech stack:** Java 21, Spring Boot 3.3.2, Spring Security 6, Springdoc 2.6.0,
OpenAPI 3, JUnit 5, AssertJ, ArchUnit 1.4.2, MockMvc, Java `HttpClient`, Maven
Surefire/Failsafe, Testcontainers PostgreSQL 16, Flyway

**Spec:** [KAN-43 design](design.md)

## Global constraints

- Preserve every existing runtime status, code, title, detail, type URN,
  request-ID rule, validation shape, and disclosure boundary.
- Do not change successful response bodies or retire
  `com.project.optrabidz.common.api.response.ApiResponse`; KAN-41 owns that
  work.
- Do not change authentication technology, session or CSRF behavior, roles,
  anonymous business access, business rules, database schema, Flyway, CI, or
  dependency versions.
- Keep `common.error` framework-free and prevent domain, application, feature,
  and common code from depending on the top-level documentation adapter.
- Use explicit immutable production registration; reflection is test-only.
- Disable OpenAPI JSON and Swagger UI in base configuration and by default in
  production. Production UI stays disabled; explicitly enabled production JSON
  requires an authenticated Spring Security context.
- Keep `springdoc.use-management-port=false` and protect JSON, YAML, Swagger
  configuration, UI, redirect, and Swagger WebJar routes.
- Publish only fixed safe public definitions. Never include diagnostic context,
  raw exception messages, rejected values, stack traces, database identifiers,
  credentials, secrets, or signatures.
- Add no custom controller-annotation framework. Use the fully qualified
  Swagger `ApiResponse` annotation where the project `ApiResponse` name would
  collide.
- Follow test-first RED/GREEN/refactor checkpoints and make one reviewable
  commit per task.

---

## File map

### Existing source files to modify

| File group | Responsibility after KAN-43 |
|---|---|
| `classification/application/error/ClassificationErrors.java` | Expose the eight classification descriptors through `descriptors()` |
| `identity/application/error/IdentityErrors.java` | Expose the three identity descriptors |
| `security/application/error/SecurityErrors.java` | Expose the seven security-application descriptors |
| `financial/application/error/FinancialErrors.java` | Expose the nineteen financial descriptors |
| `participation/application/error/{Admin,Investor,Participation,Startup}Errors.java` | Expose the eight participation descriptors across their four owners |
| `marketplace/application/error/MarketplaceErrors.java` | Expose the nine marketplace descriptors |
| `notification/application/error/NotificationErrors.java` | Expose the two notification descriptors |
| `governance/application/error/GovernanceErrors.java` | Expose the five governance descriptors |
| `common/api/error/FrameworkProblem.java` | Make the seven fixed framework definitions readable by the outer adapter |
| `common/api/error/ProblemDetailsFactory.java` | Reuse one public type-URN function without changing output |
| `security/api/{AuthController,MeController}.java` | Reference representative reusable OpenAPI failures |
| `marketplace/api/ListingController.java` | Reference representative validation, authentication, authorization, not-found, and business-rule failures |
| `src/main/resources/application*.properties` | Declare the exposure matrix and map it to Springdoc properties |
| `architecture/ExceptionArchitectureTest.java` | Prevent inward dependencies on the documentation adapter |
| `testsupport/RealHttpIntegrationTestSupport.java` | Continue to supply bounded real-port requests; change only if an authenticated GET helper is genuinely missing |
| `docs/error-handling/README.md` | Link the stable catalogue and document its update command |
| `docs/README.md` | Link the stable catalogue from the documentation portal |

### New production files

| File | Responsibility |
|---|---|
| `common/api/error/ProblemTypeUri.java` | Derive the exact public type URN from an approved code |
| `documentation/error/PublicErrorDefinition.java` | Hold one normalized safe public contract and its owners |
| `documentation/error/PublicErrorCatalogue.java` | Explicitly compose, validate, sort, and deduplicate all sources |
| `documentation/error/ErrorCatalogueMarkdownRenderer.java` | Render stable Markdown from catalogue entries without filesystem side effects |
| `documentation/openapi/OpenApiProblemDetailsConfiguration.java` | Register schemas, headers, and reusable responses when JSON documentation is enabled |
| `documentation/security/DocumentationExposureProperties.java` | Bind the application-owned documentation switches and access mode |
| `documentation/security/DocumentationExposureValidator.java` | Reject unsafe or contradictory profile/property combinations at startup |
| `documentation/security/DocumentationSecurityConfiguration.java` | Apply the dedicated ordered filter chain to every documentation route |

### New test and documentation files

| File | Responsibility |
|---|---|
| `common/api/error/ProblemTypeUriTest.java` | Prove unchanged and locale-independent type URNs |
| `documentation/error/ErrorCatalogueInventoryTest.java` | Compare every public descriptor field with explicit module collections and aggregate composition |
| `documentation/error/PublicErrorCatalogueTest.java` | Prove normalization, ordering, exact duplicate merging, and conflict rejection |
| `documentation/error/ErrorCatalogueMarkdownSnapshotTest.java` | Regenerate only when explicitly requested and otherwise enforce byte-for-byte catalogue parity |
| `documentation/openapi/OpenApiProblemDetailsComponentsTest.java` | Verify schemas, responses, headers, code enumeration, and disclosure allowlist without a server |
| `documentation/openapi/OpenApiProblemDetailsIT.java` | Fetch real OpenAPI JSON and compare it with representative real runtime failures |
| `documentation/security/DocumentationExposureValidatorTest.java` | Verify valid and invalid policy combinations without starting the application |
| `documentation/security/DocumentationExposureIT.java` | Verify actual route authorization and profile behavior through the Spring Security chains |
| `docs/error-handling/error-catalogue.md` | Stable generated public error reference, one row per unique code |

## Task 1: Complete the explicit error-source inventory and centralize type URNs

**Files:**

- Modify: the 11 module `*Errors.java` catalogue files listed in the file map
- Modify: `src/main/java/com/project/optrabidz/common/api/error/FrameworkProblem.java`
- Modify: `src/main/java/com/project/optrabidz/common/api/error/ProblemDetailsFactory.java`
- Create: `src/main/java/com/project/optrabidz/common/api/error/ProblemTypeUri.java`
- Create: `src/test/java/com/project/optrabidz/common/api/error/ProblemTypeUriTest.java`
- Create: `src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueInventoryTest.java`

**Interfaces:**

- Every module catalogue produces
  `public static List<ErrorDescriptor> descriptors()`.
- `ProblemTypeUri.fromCode(String)` produces a normalized `URI`.
- `FrameworkProblem` exposes public `code()`, `mapping()`, and `detail()`
  methods; its values do not change.
- The inventory test invokes `descriptors()` reflectively only under
  `src/test`.

- [x] **Step 1: Write the type-URN and module-inventory tests**

Create `ProblemTypeUriTest`:

```java
package com.project.optrabidz.common.api.error;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemTypeUriTest {
    @Test
    void derivesTheExistingPublicTypeUrn() {
        assertThat(ProblemTypeUri.fromCode("PAYMENT_STATE_CONFLICT"))
                .isEqualTo(URI.create(
                        "urn:optrabidz:problem:payment-state-conflict"
                ));
    }

    @Test
    void derivationDoesNotDependOnTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(ProblemTypeUri.fromCode("INTERNAL_SERVER_ERROR"))
                    .hasToString(
                            "urn:optrabidz:problem:internal-server-error"
                    );
        } finally {
            Locale.setDefault(original);
        }
    }
}
```

In `ErrorCatalogueInventoryTest`, declare the exact owners:

```java
private static final List<Class<?>> MODULE_CATALOGUES = List.of(
        ClassificationErrors.class,
        IdentityErrors.class,
        SecurityErrors.class,
        FinancialErrors.class,
        AdminErrors.class,
        InvestorErrors.class,
        ParticipationErrors.class,
        StartupErrors.class,
        MarketplaceErrors.class,
        NotificationErrors.class,
        GovernanceErrors.class
);
```

For each class, collect fields that are `public static` and exactly
`ErrorDescriptor`, invoke `descriptors()`, and assert identity-set equality.
Also assert the total declared field count is 61 so accidental test-scope
reduction is visible.

- [x] **Step 2: Run the focused tests and capture RED**

Run:

```powershell
.\mvnw.cmd -q "-Dtest=ProblemTypeUriTest,ErrorCatalogueInventoryTest" test
```

Expected: test compilation fails because `ProblemTypeUri` and the eleven
`descriptors()` methods do not exist.

- [x] **Step 3: Add the explicit immutable module collections**

Each method returns `List.of(...)` in declaration order. Use this exact
inventory:

| Catalogue | Descriptor constants |
|---|---|
| `ClassificationErrors` | `STARTUP_CLASSIFICATION_PROFILE_REQUIRED`, `STARTUP_CLASSIFICATION_ALREADY_EXISTS`, `STARTUP_CLASSIFICATION_NOT_FOUND`, `STARTUP_CLASSIFICATION_RULE_VIOLATION`, `INVESTOR_PREFERENCE_PROFILE_REQUIRED`, `INVESTOR_PREFERENCE_ALREADY_EXISTS`, `INVESTOR_PREFERENCE_NOT_FOUND`, `INVESTOR_PREFERENCE_RULE_VIOLATION` |
| `IdentityErrors` | `ACCOUNT_NOT_FOUND`, `ACCOUNT_STATE_CONFLICT`, `PROFILE_STATE_CONFLICT` |
| `SecurityErrors` | `INVALID_CREDENTIALS`, `CURRENT_PASSWORD_INVALID`, `EMAIL_ALREADY_REGISTERED`, `CREDENTIAL_NOT_FOUND`, `PASSWORD_POLICY_VIOLATION`, `SELF_REGISTRATION_NOT_ALLOWED`, `AUTHORIZATION_FAILED` |
| `FinancialErrors` | `FINANCIAL_OPERATION_NOT_ALLOWED`, `SETTLEMENT_NOT_FOUND`, `SETTLEMENT_NOT_PAYABLE`, `SETTLEMENT_STATE_CONFLICT`, `REPAYMENT_NOT_FOUND`, `REPAYMENT_INSTALLMENT_NOT_FOUND`, `REPAYMENT_INSTALLMENT_NOT_PAYABLE`, `REPAYMENT_STATE_CONFLICT`, `PAYMENT_INTENT_NOT_FOUND`, `PAYMENT_ATTEMPT_NOT_FOUND`, `PAYMENT_INTENT_EXPIRED`, `PAYMENT_INTENT_NOT_ACTIVE`, `PAYMENT_ALREADY_CONFIRMED`, `PAYMENT_STATE_CONFLICT`, `PAYMENT_METHOD_UNSUPPORTED`, `PAYMENT_PROVIDER_MISMATCH`, `PAYMENT_WEBHOOK_REJECTED`, `PAYMENT_WEBHOOK_PAYLOAD_INVALID`, `PAYMENT_WEBHOOK_PROCESSING_FAILED` |
| `AdminErrors` | `ACTIVE_ADMIN_ALREADY_EXISTS`, `ADMIN_AUTHORITY_ALREADY_GRANTED`, `ACTIVE_ADMIN_NOT_FOUND` |
| `InvestorErrors` | `INVESTOR_ALREADY_EXISTS`, `INVESTOR_NOT_FOUND` |
| `ParticipationErrors` | `AUTHORIZATION_FAILED` |
| `StartupErrors` | `STARTUP_ALREADY_EXISTS`, `STARTUP_NOT_FOUND` |
| `MarketplaceErrors` | `LISTING_NOT_FOUND`, `BID_NOT_FOUND`, `AGREEMENT_NOT_FOUND`, `MARKETPLACE_ACCESS_DENIED`, `LISTING_STATE_CONFLICT`, `BID_STATE_CONFLICT`, `BID_ALREADY_EXISTS`, `BID_ACCEPTANCE_CONFLICT`, `UNSUPPORTED_FUNDING_MODEL` |
| `NotificationErrors` | `NOTIFICATION_NOT_FOUND`, `NOTIFICATION_SUBSCRIPTION_NOT_FOUND` |
| `GovernanceErrors` | `GOVERNANCE_ACTION_NOT_ELIGIBLE`, `GOVERNANCE_ACTION_NOT_PERMITTED`, `GOVERNANCE_STATE_CONFLICT`, `ADMIN_RECOVERY_ACCESS_DENIED`, `ADMIN_AUTHORITY_UNAVAILABLE` |

Example implementation shape:

```java
public static List<ErrorDescriptor> descriptors() {
    return List.of(
            ACCOUNT_NOT_FOUND,
            ACCOUNT_STATE_CONFLICT,
            PROFILE_STATE_CONFLICT
    );
}
```

Import only `java.util.List`; do not add Spring or documentation dependencies.

- [x] **Step 4: Add the shared type-URN function and safe framework access**

Create:

```java
package com.project.optrabidz.common.api.error;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public final class ProblemTypeUri {
    private ProblemTypeUri() {
    }

    public static URI fromCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        return URI.create(
                "urn:optrabidz:problem:"
                        + code.toLowerCase(Locale.ROOT).replace('_', '-')
        );
    }
}
```

Replace the private slug construction in `ProblemDetailsFactory` with
`ProblemTypeUri.fromCode(code)` and remove only the now-unused `Locale` import
and private method. Make `FrameworkProblem` and its three accessors public;
leave values, mappings, titles, and details byte-for-byte unchanged.

- [x] **Step 5: Run the focused and existing factory tests**

Run:

```powershell
.\mvnw.cmd -q "-Dtest=ProblemTypeUriTest,ProblemDetailsFactoryTest,ErrorCatalogueInventoryTest" test
```

Expected: all tests pass; inventory reports exactly 61 module fields.

- [x] **Step 6: Commit the complete source inventory**

```powershell
git add src/main/java/com/project/optrabidz/common/api/error `
  src/main/java/com/project/optrabidz/*/application/error `
  src/main/java/com/project/optrabidz/participation/application/error `
  src/test/java/com/project/optrabidz/common/api/error/ProblemTypeUriTest.java `
  src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueInventoryTest.java
git commit -m "refactor(KAN-43): expose complete public error sources"
```

## Task 2: Build the normalized public catalogue and protect dependency direction

**Files:**

- Create: `src/main/java/com/project/optrabidz/documentation/error/PublicErrorDefinition.java`
- Create: `src/main/java/com/project/optrabidz/documentation/error/PublicErrorCatalogue.java`
- Create: `src/test/java/com/project/optrabidz/documentation/error/PublicErrorCatalogueTest.java`
- Modify: `src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueInventoryTest.java`
- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`

**Interfaces:**

- `PublicErrorDefinition.fromModule(String, ErrorDescriptor)` normalizes a
  module descriptor.
- `PublicErrorDefinition.fromFramework(FrameworkProblem)` and
  `fromSecurity(SecurityProblem)` normalize transport problems.
- `sameContract(PublicErrorDefinition)` compares code, status, title, detail,
  and type but not owners.
- `withSources(SortedSet<String>)` returns a defensively copied definition.
- `PublicErrorCatalogue.createDefault()` composes all approved sources.
- `PublicErrorCatalogue.from(List<PublicErrorDefinition>)` supports focused
  conflict tests.
- `entries()` returns an immutable list sorted by code.

- [x] **Step 1: Write normalization, duplicate, conflict, and aggregate tests**

Use exact assertions:

```java
@Test
void normalizesModuleErrorsThroughTheRuntimeMapping() {
    PublicErrorDefinition definition = PublicErrorDefinition.fromModule(
            "marketplace",
            MarketplaceErrors.LISTING_NOT_FOUND
    );

    assertThat(definition.code()).isEqualTo("LISTING_NOT_FOUND");
    assertThat(definition.status()).isEqualTo(404);
    assertThat(definition.title()).isEqualTo("Resource not found");
    assertThat(definition.detail())
            .isEqualTo("The requested listing was not found");
    assertThat(definition.type())
            .hasToString("urn:optrabidz:problem:listing-not-found");
    assertThat(definition.category())
            .contains(ErrorCategory.NOT_FOUND);
    assertThat(definition.sources()).containsExactly("marketplace");
}

@Test
void mergesOnlyIdenticalDuplicateContracts() {
    PublicErrorCatalogue catalogue = PublicErrorCatalogue.createDefault();

    PublicErrorDefinition authorization = catalogue.entries().stream()
            .filter(entry -> entry.code().equals("AUTHORIZATION_FAILED"))
            .findFirst()
            .orElseThrow();

    assertThat(authorization.sources()).containsExactly(
            "participation",
            "security-application",
            "spring-security"
    );
    assertThat(catalogue.entries()).hasSize(69);
}

@Test
void rejectsTwoMeaningsForOneCode() {
    PublicErrorDefinition first = definition(
            "DUPLICATE_CODE", 409, "Request conflict", "First meaning", "a"
    );
    PublicErrorDefinition second = definition(
            "DUPLICATE_CODE", 422, "Business rule violation", "Second meaning", "b"
    );

    assertThatThrownBy(() -> PublicErrorCatalogue.from(List.of(first, second)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DUPLICATE_CODE")
            .hasMessageContaining("a")
            .hasMessageContaining("b");
}
```

Also assert unique codes, sorted codes, immutable entries, immutable sources,
all seven `FrameworkProblem` values, and all three `SecurityProblem` values.

- [x] **Step 2: Run the catalogue tests and capture RED**

```powershell
.\mvnw.cmd -q "-Dtest=PublicErrorCatalogueTest,ErrorCatalogueInventoryTest" test
```

Expected: compilation fails because the normalized catalogue types do not
exist.

- [x] **Step 3: Implement the safe normalized record**

Use this exact shape:

```java
public record PublicErrorDefinition(
        String code,
        int status,
        String title,
        String detail,
        URI type,
        Optional<ErrorCategory> category,
        SortedSet<String> sources
) {
    public PublicErrorDefinition {
        code = requireText(code, "code");
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be 400..599");
        }
        title = requireText(title, "title");
        detail = requireText(detail, "detail");
        type = Objects.requireNonNull(type, "type must not be null");
        category = Objects.requireNonNull(
                category,
                "category must not be null"
        );
        Objects.requireNonNull(sources, "sources must not be null");
        TreeSet<String> sourceCopy = new TreeSet<>();
        for (String source : sources) {
            sourceCopy.add(requireText(source, "source"));
        }
        if (sourceCopy.isEmpty()) {
            throw new IllegalArgumentException("sources must not be empty");
        }
        sources = Collections.unmodifiableSortedSet(sourceCopy);
    }

    public static PublicErrorDefinition fromModule(
            String source,
            ErrorDescriptor descriptor
    ) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        HttpErrorMapping mapping = HttpErrorMapping.forCategory(
                descriptor.category()
        );
        return definition(
                source,
                descriptor.code(),
                mapping,
                descriptor.publicMessage(),
                Optional.of(descriptor.category())
        );
    }

    public static PublicErrorDefinition fromFramework(
            FrameworkProblem problem
    ) {
        Objects.requireNonNull(problem, "problem must not be null");
        return definition(
                "spring-mvc",
                problem.code(),
                problem.mapping(),
                problem.detail(),
                Optional.empty()
        );
    }

    public static PublicErrorDefinition fromSecurity(SecurityProblem problem) {
        Objects.requireNonNull(problem, "problem must not be null");
        ErrorCategory category = problem == SecurityProblem.AUTHENTICATION_REQUIRED
                ? ErrorCategory.AUTHENTICATION
                : ErrorCategory.AUTHORIZATION;
        return definition(
                "spring-security",
                problem.code(),
                problem.mapping(),
                problem.detail(),
                Optional.of(category)
        );
    }

    public boolean sameContract(PublicErrorDefinition other) {
        return code.equals(other.code)
                && status == other.status
                && title.equals(other.title)
                && detail.equals(other.detail)
                && type.equals(other.type);
    }

    public PublicErrorDefinition withSources(SortedSet<String> merged) {
        return new PublicErrorDefinition(
                code, status, title, detail, type, category, merged
        );
    }

    private static PublicErrorDefinition definition(
            String source,
            String code,
            HttpErrorMapping mapping,
            String detail,
            Optional<ErrorCategory> category
    ) {
        return new PublicErrorDefinition(
                code,
                mapping.status().value(),
                mapping.title(),
                detail,
                ProblemTypeUri.fromCode(code),
                category,
                new TreeSet<>(Set.of(source))
        );
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
```

For `SecurityProblem`, map `AUTHENTICATION_REQUIRED` to
`ErrorCategory.AUTHENTICATION`, and `AUTHORIZATION_FAILED` plus
`CSRF_VALIDATION_FAILED` to `ErrorCategory.AUTHORIZATION`. Framework problems
use `Optional.empty()` because 405, 406, and 415 have no neutral category.

- [x] **Step 4: Implement explicit composition and conflict detection**

`createDefault()` must call every module's `descriptors()` explicitly, then
append `FrameworkProblem.values()` and `SecurityProblem.values()`. Use these
exact public source labels:

| Catalogue | Source label |
|---|---|
| `ClassificationErrors` | `classification` |
| `IdentityErrors` | `identity` |
| `SecurityErrors` | `security-application` |
| `FinancialErrors` | `financial` |
| `AdminErrors` | `participation-admin` |
| `InvestorErrors` | `participation-investor` |
| `ParticipationErrors` | `participation` |
| `StartupErrors` | `participation-startup` |
| `MarketplaceErrors` | `marketplace` |
| `NotificationErrors` | `notification` |
| `GovernanceErrors` | `governance` |
| `FrameworkProblem` | `spring-mvc` |
| `SecurityProblem` | `spring-security` |

Group candidates by code in a `TreeMap`; reduce each group only when every
candidate has the same external contract; merge owners into a `TreeSet`.
Return `List.copyOf(...)`.

No `Class`, `Field`, classpath scanner, Spring bean scan, or reflection API may
appear under `src/main`.

- [x] **Step 5: Add aggregate inventory and architecture enforcement**

Extend `ErrorCatalogueInventoryTest` to compare the union of all declared
module fields plus all framework/security values with the sources represented
by `PublicErrorCatalogue.createDefault()`.

Add this ArchUnit boundary:

```java
@ArchTest
static final ArchRule PRODUCTION_CODE_DOES_NOT_DEPEND_INWARD_ON_DOCUMENTATION =
        noClasses()
                .that().resideOutsideOfPackage("..documentation..")
                .should().dependOnClassesThat()
                .resideInAPackage("..documentation..")
                .as("documentation is an outer adapter, never an inward dependency");
```

- [x] **Step 6: Run focused and architecture tests**

```powershell
.\mvnw.cmd -q "-Dtest=PublicErrorCatalogueTest,ErrorCatalogueInventoryTest,ExceptionArchitectureTest" test
```

Expected: all tests pass and the default catalogue has 69 unique codes.

- [x] **Step 7: Commit the catalogue boundary**

```powershell
git add src/main/java/com/project/optrabidz/documentation/error `
  src/test/java/com/project/optrabidz/documentation/error `
  src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java
git commit -m "feat(KAN-43): compose the public error catalogue"
```

## Task 3: Generate and lock the stable Markdown catalogue

**Files:**

- Create: `src/main/java/com/project/optrabidz/documentation/error/ErrorCatalogueMarkdownRenderer.java`
- Create: `src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueMarkdownSnapshotTest.java`
- Create: `docs/error-handling/error-catalogue.md`
- Modify: `docs/error-handling/README.md`
- Modify: `docs/README.md`

**Interfaces:**

- `ErrorCatalogueMarkdownRenderer.render(List<PublicErrorDefinition>)`
  returns the complete normalized Markdown with `\n` line endings and a final
  newline.
- System property `optrabidz.update-error-catalogue=true` explicitly permits
  the snapshot test to rewrite the checked-in catalogue before comparison.
- With the property absent, the test is read-only and fails on any byte drift.

- [x] **Step 1: Write the renderer and snapshot tests first**

The renderer test must assert the exact header and escaping behavior:

```java
String rendered = ErrorCatalogueMarkdownRenderer.render(List.of(definition));

assertThat(rendered).isEqualTo("""
        # Public Error Catalogue

        This file is generated from the application-owned public error definitions.
        Do not add diagnostic or secret values.

        | Code | Category | HTTP | Title | Safe detail | Type | Sources |
        |---|---|---:|---|---|---|---|
        | `LISTING_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested listing was not found | `urn:optrabidz:problem:listing-not-found` | `marketplace` |
        """);
```

The snapshot test uses:

```java
private static final Path CATALOGUE = Path.of(
        "docs", "error-handling", "error-catalogue.md"
);

@Test
void checkedInCatalogueMatchesTheRuntimeDefinitions() throws Exception {
    String expected = ErrorCatalogueMarkdownRenderer.render(
            PublicErrorCatalogue.createDefault().entries()
    );
    if (Boolean.getBoolean("optrabidz.update-error-catalogue")) {
        Files.writeString(CATALOGUE, expected, StandardCharsets.UTF_8);
    }
    assertThat(Files.readString(CATALOGUE, StandardCharsets.UTF_8))
            .isEqualTo(expected);
}
```

- [x] **Step 2: Run the snapshot test and capture RED**

```powershell
.\mvnw.cmd -q "-Dtest=ErrorCatalogueMarkdownSnapshotTest" test
```

Expected: compilation fails until the renderer exists, then the snapshot fails
because `docs/error-handling/error-catalogue.md` is absent.

- [x] **Step 3: Implement deterministic rendering**

Sort defensively by code even though the catalogue is already sorted. Escape
backslash, pipe, carriage return, and newline in Markdown cells. Render absent
category as `TRANSPORT`. Join sorted sources with `, ` inside one code span.
Do not add a generation timestamp, local path, commit hash, or environment
value because those would make the snapshot non-deterministic.

- [x] **Step 4: Generate the checked-in catalogue explicitly**

```powershell
.\mvnw.cmd -q "-Dtest=ErrorCatalogueMarkdownSnapshotTest" `
  "-Doptrabidz.update-error-catalogue=true" test
```

Expected: the test writes 69 sorted rows and passes the immediate comparison.

- [x] **Step 5: Link the stable reference and regeneration command**

Add `error-catalogue.md` under the current-system section of
`docs/error-handling/README.md`, not only under work-item history. Document the
same Maven command from Step 4. Add the catalogue to the `Start Here` or task
lookup table in `docs/README.md`.

- [x] **Step 6: Verify snapshot, links, and disclosure text**

```powershell
.\mvnw.cmd -q "-Dtest=ErrorCatalogueMarkdownSnapshotTest,DocumentationLinksTest,DocumentationLinkValidatorTest" test
rg -ni "^\\|.*(password hash|credential value|secret value|signature value|stack trace|raw exception|exception class|diagnostic context|database identifier)" `
  docs/error-handling/error-catalogue.md
```

Expected: tests pass and the disclosure scan returns no catalogue rows. Safe
public authentication descriptions may still use ordinary words such as
`password` or `credential`; the scan targets internal values and diagnostics,
not legitimate client-facing error semantics. A descriptive introductory
warning may contain the word `secret` only if it is not part of the generated
table.

- [x] **Step 7: Commit the stable catalogue**

```powershell
git add src/main/java/com/project/optrabidz/documentation/error/ErrorCatalogueMarkdownRenderer.java `
  src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueMarkdownSnapshotTest.java `
  docs/README.md docs/error-handling/README.md docs/error-handling/error-catalogue.md
git commit -m "docs(KAN-43): publish the public error catalogue"
```

## Task 4: Publish reusable OpenAPI schemas and response components

**Files:**

- Create: `src/main/java/com/project/optrabidz/documentation/openapi/OpenApiProblemDetailsConfiguration.java`
- Create: `src/test/java/com/project/optrabidz/documentation/openapi/OpenApiProblemDetailsComponentsTest.java`

**Interfaces:**

- `OpenApiProblemDetailsConfiguration.problemDetailsCustomizer()` returns an
  `OpenApiCustomizer` only when
  `optrabidz.documentation.api-docs-enabled=true`.
- Components are named `ProblemDetails`, `ValidationViolation`, and
  `ValidationProblemDetails`.
- Response components are named `BadRequestProblem`, `ValidationProblem`,
  `UnauthorizedProblem`, `ForbiddenProblem`, `NotFoundProblem`,
  `MethodNotAllowedProblem`, `NotAcceptableProblem`, `ConflictProblem`,
  `UnsupportedMediaTypeProblem`, `UnprocessableEntityProblem`, and
  `InternalServerProblem`.
- Every response uses `application/problem+json` and the reusable
  `X-Request-Id` header.

- [x] **Step 1: Write the component-level OpenAPI test**

Construct an empty `OpenAPI`, run the customizer, and assert:

```java
Schema<?> problem = openApi.getComponents()
        .getSchemas().get("ProblemDetails");

assertThat(problem.getRequired()).containsExactlyInAnyOrder(
        "type", "title", "status", "detail", "instance",
        "code", "requestId", "timestamp"
);
assertThat(problem.getProperties()).containsOnlyKeys(
        "type", "title", "status", "detail", "instance",
        "code", "requestId", "timestamp"
);
assertThat(problem.getProperties().get("code").getEnum())
        .containsExactlyElementsOf(
                PublicErrorCatalogue.createDefault().entries().stream()
                        .map(PublicErrorDefinition::code)
                        .toList()
        );
```

Assert `ValidationProblemDetails` uses `allOf` with the base schema and an
object requiring `violations`; the array items reference
`ValidationViolation`, whose only properties are required `field` and
`message`. Assert all eleven response names, exact statuses represented by
their names, content type, schema reference, and header reference. Scan all
schema/property/example names for `exception`, `trace`, `stack`, `diagnostic`,
`secret`, `password`, `signature`, and `databaseId`.

- [x] **Step 2: Run the component test and capture RED**

```powershell
.\mvnw.cmd -q "-Dtest=OpenApiProblemDetailsComponentsTest" test
```

Expected: compilation fails because the OpenAPI configuration does not exist.

- [x] **Step 3: Build the three schemas from the real wire contract**

Use Swagger model `Schema` objects, not runtime DTOs. Required formats:

- `type` and `instance`: `type=string`, `format=uri`;
- `status`: `type=integer`, `format=int32`, minimum 400, maximum 599;
- `timestamp`: `type=string`, `format=date-time`;
- `code`: sorted enum from `PublicErrorCatalogue.entries()`; and
- `violations`: required array of `ValidationViolation` references.

Leave `additionalProperties` unspecified on the base schema. Setting it to
`false` would make `ValidationProblemDetails` invalid when `allOf` adds the
`violations` property. Instead, component tests assert the exact named
properties and runtime disclosure tests assert the actual serialized allowlist.

- [x] **Step 4: Build reusable headers and responses**

Create one component header named `RequestIdHeader` with a non-blank string
schema. Every response header uses
`#/components/headers/RequestIdHeader`. Use the base schema for every response
except `ValidationProblem`, which references `ValidationProblemDetails`.

The base 400 response remains valid for malformed and module validation
problems. Operation annotations may select the stricter validation response
only when that operation's documented 400 case is request validation.

- [x] **Step 5: Run the component and catalogue tests**

```powershell
.\mvnw.cmd -q "-Dtest=OpenApiProblemDetailsComponentsTest,PublicErrorCatalogueTest,ErrorCatalogueMarkdownSnapshotTest" test
```

Expected: all tests pass with exactly 69 allowable codes and eleven reusable
responses.

- [x] **Step 6: Commit the OpenAPI components**

```powershell
git add src/main/java/com/project/optrabidz/documentation/openapi `
  src/test/java/com/project/optrabidz/documentation/openapi/OpenApiProblemDetailsComponentsTest.java
git commit -m "feat(KAN-43): publish reusable Problem Details components"
```

## Task 5: Enforce the fail-closed documentation exposure policy

**Files:**

- Create: `src/main/java/com/project/optrabidz/documentation/security/DocumentationExposureProperties.java`
- Create: `src/main/java/com/project/optrabidz/documentation/security/DocumentationExposureValidator.java`
- Create: `src/main/java/com/project/optrabidz/documentation/security/DocumentationSecurityConfiguration.java`
- Create: `src/test/java/com/project/optrabidz/documentation/security/DocumentationExposureValidatorTest.java`
- Create: `src/test/java/com/project/optrabidz/documentation/security/DocumentationExposureIT.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/application-dev.properties`
- Modify: `src/main/resources/application-prod.properties`
- Modify: `src/test/resources/application-test.properties`

**Interfaces:**

- `DocumentationExposureProperties` binds booleans `apiDocsEnabled`,
  `swaggerUiEnabled`, and `managementPortEnabled`, plus enum `Access` with
  `DISABLED`, `PUBLIC`, and `AUTHENTICATED`.
- The dedicated chain has `@Order(1)` and matches only documentation paths.
  It authorizes the API-doc and UI path families independently so a disabled UI
  cannot leak WebJar assets while JSON remains enabled. The existing
  application chain remains unchanged and handles all other requests.
- The chain reuses `ActiveSessionFilter`, `SecurityMdcFilter`,
  `ProblemAuthenticationEntryPoint`, and `ProblemAccessDeniedHandler` so
  authenticated documentation follows existing session validity, audit, MDC,
  and Problem Details behavior.

- [x] **Step 1: Write the policy validation test matrix**

Test this complete table by constructing the properties and validator directly:

| Profile | JSON | UI | Management port | Access | Expected |
|---|---:|---:|---:|---|---|
| base | false | false | false | DISABLED | valid |
| dev | true | true | false | PUBLIC | valid |
| test | true | false | false | PUBLIC | valid |
| prod | false | false | false | AUTHENTICATED | valid |
| prod | true | false | false | AUTHENTICATED | valid |
| any | true | false | false | DISABLED | invalid |
| any | false | true | false | PUBLIC | invalid |
| any | true | true | false | AUTHENTICATED | invalid |
| prod | true | true | false | PUBLIC | invalid |
| any | true | false | true | AUTHENTICATED | invalid |

Every invalid case must assert a message naming the contradictory properties.

- [x] **Step 2: Write actual route tests before configuration exists**

In `DocumentationExposureIT`, use focused nested Spring Boot contexts or the
existing PostgreSQL integration support to assert:

- test profile: anonymous `GET /v3/api-docs` is 200 JSON;
- test profile: `/swagger-ui.html`, `/swagger-ui/index.html`, and direct
  `/webjars/swagger-ui/5.17.14/swagger-ui.css` are not served;
- disabled override: JSON, YAML, Swagger config, UI, and WebJar paths are not
  accessible for anonymous or authenticated callers;
- authenticated override: anonymous JSON receives the existing
  `AUTHENTICATION_REQUIRED` Problem Details response; and
- authenticated override: a registered and logged-in caller receives 200.

Do not assert only one path; parameterize the full path allowlist.

- [x] **Step 3: Run policy tests and capture RED**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationExposureValidatorTest" `
  "-Dit.test=DocumentationExposureIT" "-DskipITs=false" verify
```

Expected: compilation fails because exposure properties, validator, and
security chain do not exist.

- [x] **Step 4: Add the application-owned properties and exact profile matrix**

Base `application.properties`:

```properties
optrabidz.documentation.api-docs-enabled=false
optrabidz.documentation.swagger-ui-enabled=false
optrabidz.documentation.management-port-enabled=false
optrabidz.documentation.access=DISABLED
springdoc.api-docs.enabled=${optrabidz.documentation.api-docs-enabled}
springdoc.swagger-ui.enabled=${optrabidz.documentation.swagger-ui-enabled}
springdoc.use-management-port=${optrabidz.documentation.management-port-enabled}
springdoc.writer-with-order-by-keys=true
```

Development overrides:

```properties
optrabidz.documentation.api-docs-enabled=true
optrabidz.documentation.swagger-ui-enabled=true
optrabidz.documentation.access=PUBLIC
```

Test overrides:

```properties
optrabidz.documentation.api-docs-enabled=true
optrabidz.documentation.swagger-ui-enabled=false
optrabidz.documentation.access=PUBLIC
```

Production overrides:

```properties
optrabidz.documentation.api-docs-enabled=${OPTRABIDZ_API_DOCS_ENABLED:false}
optrabidz.documentation.swagger-ui-enabled=false
optrabidz.documentation.management-port-enabled=false
optrabidz.documentation.access=AUTHENTICATED
```

- [x] **Step 5: Implement typed binding and fail-fast validation**

Use a record annotated with `@ConfigurationProperties` and register it with
`@EnableConfigurationProperties` from the security configuration. The
validator rejects:

- management-port enablement under every profile;
- enabled JSON with `DISABLED` access;
- enabled UI while JSON is disabled;
- enabled UI with any access other than `PUBLIC`; and
- enabled UI or public access while the `prod` profile is active.

Disabled JSON with `AUTHENTICATED` access is valid for the production default.

- [x] **Step 6: Implement the isolated documentation filter chain**

Match these families exactly:

```java
private static final String[] API_DOC_PATHS = {
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml"
};

private static final String[] UI_PATHS = {
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/webjars/swagger-ui/**"
};
```

Configure `@Order(1)`, a security matcher containing the union of both path
families, session policy `IF_REQUIRED`, disabled form login/basic/logout, the
existing Problem Details handlers, `ActiveSessionFilter` before
`AuthorizationFilter`, and `SecurityMdcFilter` after the active-session filter.
CSRF may be disabled on this chain because every matched documentation route
is read-only and the chain exposes no mutation endpoint.

Authorize the families separately:

- when `apiDocsEnabled=false`, apply `denyAll` to `API_DOC_PATHS`; otherwise
  apply `permitAll` or `authenticated` from the access mode;
- when `swaggerUiEnabled=false`, apply `denyAll` to `UI_PATHS`; otherwise apply
  the validated `PUBLIC` access mode; and
- finish the matched chain with `anyRequest().denyAll()`.

Do not add documentation matchers to the existing feature security
configuration.

- [x] **Step 7: Run validator, route, security, and existing session tests**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationExposureValidatorTest" `
  "-Dit.test=DocumentationExposureIT,SecurityApiIT" `
  "-DskipITs=false" verify
```

Expected: all tests pass; existing application routes still use the original
security chain.

- [x] **Step 8: Commit the exposure boundary**

```powershell
git add src/main/java/com/project/optrabidz/documentation/security `
  src/test/java/com/project/optrabidz/documentation/security `
  src/main/resources/application.properties `
  src/main/resources/application-dev.properties `
  src/main/resources/application-prod.properties `
  src/test/resources/application-test.properties
git commit -m "feat(KAN-43): secure OpenAPI exposure by environment"
```

## Task 6: Reference representative operations and verify real contract parity

**Files:**

- Modify: `src/main/java/com/project/optrabidz/security/api/AuthController.java`
- Modify: `src/main/java/com/project/optrabidz/security/api/MeController.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/api/ListingController.java`
- Create: `src/test/java/com/project/optrabidz/documentation/openapi/OpenApiProblemDetailsIT.java`
- Modify only if required: `src/test/java/com/project/optrabidz/testsupport/RealHttpIntegrationTestSupport.java`

**Interfaces:**

- Controller annotations reference literal component paths such as
  `#/components/responses/ValidationProblem`; they do not import any
  `com.project.optrabidz.documentation` type.
- `OpenApiProblemDetailsIT` extends `RealHttpIntegrationTestSupport`, retrieves
  `/v3/api-docs`, and compares its components with actual real-port responses.

- [x] **Step 1: Write the real-port OpenAPI and runtime-parity test**

The test must prove:

```java
HttpResponse<String> specification = client.get("/v3/api-docs", Map.of());
assertThat(specification.statusCode()).isEqualTo(200);
JsonNode openApi = readJson(specification);

assertThat(openApi.at("/components/schemas/ProblemDetails/required"))
        .isNotNull();
assertThat(openApi.at("/components/responses/NotFoundProblem/content/application~1problem+json/schema/$ref").asText())
        .isEqualTo("#/components/schemas/ProblemDetails");
```

Send one invalid registration request and one missing-listing request through
the same real client. For each response, compare status, content type, `code`,
`title`, `detail`, `type`, request-ID header/body equality, and required field
presence with the appropriate OpenAPI schema and catalogue entry. Assert the
body contains no legacy `success`/`error`, exception, trace, diagnostic, or
secret field.

Also assert representative operation response references at these JSON
pointers:

- `/paths/~1api~1v1~1auth~1login/post/responses/400`;
- `/paths/~1api~1v1~1auth~1login/post/responses/401`;
- `/paths/~1api~1v1~1me/get/responses/401`;
- `/paths/~1api~1v1~1funding-listings~1{listingId}/get/responses/400`;
- `/paths/~1api~1v1~1funding-listings~1{listingId}/get/responses/404`;
- `/paths/~1api~1v1~1funding-listings/post/responses/401`;
- `/paths/~1api~1v1~1funding-listings/post/responses/403`; and
- `/paths/~1api~1v1~1funding-listings/post/responses/422`.

- [x] **Step 2: Run the real-port test and capture RED**

```powershell
.\mvnw.cmd -q "-Dtest=TestingSetupTest" `
  "-Dit.test=OpenApiProblemDetailsIT" "-DskipITs=false" verify
```

Expected: the component schemas exist, but representative operation response
references are absent.

- [x] **Step 3: Add explicit standard annotations to the selected methods**

Use `@io.swagger.v3.oas.annotations.responses.ApiResponses` and the fully
qualified nested `ApiResponse`. Example for `getListing`:

```java
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                ref = "#/components/responses/ValidationProblem"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                ref = "#/components/responses/NotFoundProblem"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                ref = "#/components/responses/InternalServerProblem"
        )
})
@GetMapping("/funding-listings/{listingId}")
public SuccessResponse<ListingResponse> getListing(...) {
```

Apply the approved representative matrix:

| Method | Reusable failures |
|---|---|
| `AuthController.login` | `ValidationProblem` 400, `UnauthorizedProblem` 401, `InternalServerProblem` 500 |
| `MeController.getCurrentUser` | `UnauthorizedProblem` 401, `InternalServerProblem` 500 |
| `ListingController.getListing` | `ValidationProblem` 400, `NotFoundProblem` 404, `InternalServerProblem` 500 |
| `ListingController.createListing` | `ValidationProblem` 400, `UnauthorizedProblem` 401, `ForbiddenProblem` 403, `UnprocessableEntityProblem` 422, `InternalServerProblem` 500 |

Do not annotate successful response schemas in KAN-43 and do not import
Swagger's `ApiResponse` simple name.

- [x] **Step 4: Run component, real-port, controller, and architecture tests**

```powershell
.\mvnw.cmd -q "-Dtest=OpenApiProblemDetailsComponentsTest,ExceptionArchitectureTest" `
  "-Dit.test=OpenApiProblemDetailsIT,RealHttpProblemDetailsIT" `
  "-DskipITs=false" verify
```

Expected: all tests pass; controllers have no dependency on the top-level
documentation package and success payloads remain untouched.

- [x] **Step 5: Commit representative operation publication**

```powershell
git add src/main/java/com/project/optrabidz/security/api `
  src/main/java/com/project/optrabidz/marketplace/api/ListingController.java `
  src/test/java/com/project/optrabidz/documentation/openapi/OpenApiProblemDetailsIT.java `
  src/test/java/com/project/optrabidz/testsupport/RealHttpIntegrationTestSupport.java
git commit -m "test(KAN-43): verify OpenAPI against real Problem Details"
```

If the real HTTP support did not require a change, omit it from `git add`.

## Task 7: Complete verification, lifecycle evidence, and review publication

**Files:**

- Modify: `docs/error-handling/work-items/KAN-43-openapi-error-catalogue/design.md`
- Modify: `docs/error-handling/work-items/KAN-43-openapi-error-catalogue/implementation-plan.md`
- Modify: `docs/error-handling/README.md`
- Modify: `docs/README.md`

**Interfaces:**

- Design acceptance criteria and this plan's checkboxes reflect verified
  evidence only.
- The pull request targets `develop` and contains only KAN-43 contract,
  exposure, tests, and documentation changes.

- [x] **Step 1: Run the complete unit suite**

```powershell
.\mvnw.cmd clean test
```

Expected: every Surefire test passes, including inventory, catalogue,
renderer, OpenAPI components, exposure validation, architecture, and existing
regressions.

- [x] **Step 2: Run the complete PostgreSQL integration profile**

```powershell
.\mvnw.cmd verify -Pintegration-tests
```

Expected: every Failsafe test passes, including documentation exposure,
real-port OpenAPI parity, real Problem Details, Flyway, security, marketplace,
financial, notification, and audit integration tests.

- [x] **Step 3: Verify documentation, generated parity, and diagrams**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationLinksTest,DocumentationLinkValidatorTest,ErrorCatalogueMarkdownSnapshotTest" test
git diff --check
git status --short
```

Open the checked-in SVG and PNG at original resolution. Expected: every label
is readable, links resolve, the Markdown snapshot matches, whitespace is clean,
and only intentional KAN-43 files remain.

- [x] **Step 4: Verify exposure using packaged profile settings**

Run the focused exposure integration test once more after `clean`:

```powershell
.\mvnw.cmd -q "-Dtest=TestingSetupTest" `
  "-Dit.test=DocumentationExposureIT,OpenApiProblemDetailsIT" `
  "-DskipITs=false" verify
```

Expected: base/disabled surfaces are unreachable, test JSON is public while UI
is absent, authenticated JSON rejects anonymous access, and no management-port
publication exists.

- [x] **Step 5: Record exact evidence without transient process language**

Update the design status to implemented and verified only after all commands
pass. Record commit ID, test counts, profile matrix result, catalogue unique
code count, and documentation verification. Check completed plan boxes only
for steps supported by repository or CI evidence.

- [x] **Step 6: Commit and push final documentation evidence**

```powershell
git add docs/error-handling/work-items/KAN-43-openapi-error-catalogue `
  docs/error-handling/README.md docs/README.md
git commit -m "docs(KAN-43): record OpenAPI contract verification"
git push -u origin feature/KAN-43-openapi-error-catalogue
```

- [ ] **Step 7: Create the review pull request**

```powershell
gh pr create `
  --base develop `
  --head feature/KAN-43-openapi-error-catalogue `
  --title "KAN-43: publish the verified Problem Details contract" `
  --body "## Summary
- compose every safe module, framework, and security error into one public catalogue
- publish reusable OpenAPI Problem Details schemas and responses
- enforce fail-closed documentation exposure by environment
- verify OpenAPI, Markdown, security, and real HTTP parity

## Verification
- complete unit suite passed
- complete PostgreSQL integration profile passed
- documentation links and deterministic catalogue snapshot passed
- base, development/test, and authenticated production-style exposure checks passed

## Scope
No successful-response, authentication-mechanism, business-rule, database, dependency-version, or CI workflow changes."
```

Expected: GitHub creates a pull request from the exact feature head to
`develop`; Jira associates it through the `KAN-43` key.

- [ ] **Step 8: Confirm exact-head CI and update Jira evidence**

```powershell
$headSha = git rev-parse HEAD
gh run list --branch feature/KAN-43-openapi-error-catalogue `
  --workflow ci.yml --limit 5
```

Expected: unit and PostgreSQL integration checks succeed for `$headSha`.
Record the pull request, exact commit, check results, catalogue count, and
exposure matrix on KAN-43.
