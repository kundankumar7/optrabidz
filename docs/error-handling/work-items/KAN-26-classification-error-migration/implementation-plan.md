# KAN-26: Classification Error Migration Implementation Plan

**Status:** Ready for implementation

**Goal:** Migrate expected classification failures to the neutral
`ApplicationException` contract while preserving successful startup and
investor classification behaviour.

**Source:** [KAN-26 design](design.md)

**Architecture:** The classification module owns one fixed descriptor catalogue
and actor-specific typed exceptions. Services and rule specifications select
those exceptions; the existing REST adapter remains the only HTTP translation
boundary.

**Tech stack:** Java 21, Spring Boot 3.3.2, Spring MVC `ProblemDetail`, JUnit 5,
AssertJ, Mockito, MockMvc, ArchUnit, Testcontainers, PostgreSQL 16, Flyway, and
the Maven Wrapper.

## Global constraints

- Work only on `feature/KAN-26-classification-error-migration`, based on
  `develop` commit `82119acf414d261973516170567001268ab3e769`.
- The pull request targets `develop`; `main` remains unchanged.
- Do not add, remove, or upgrade dependencies.
- Do not change Flyway V1, the database schema, seed data, runtime profiles,
  CI, authentication, authorization, CSRF, or endpoint permissions.
- Do not change transactions, repositories, event schemas, or successful
  response contracts.
- Do not migrate or remove legacy exception infrastructure used outside the
  classification module.
- Public responses use only fixed descriptor data. Account IDs, participant
  IDs, classification types and values, class names, diagnostic codes, causes,
  and raw exception messages remain protected.
- Do not broadly catch `IllegalStateException`. Explicit application
  preconditions cover expected failures; unexpected domain state remains an
  internal error.
- Use focused RED, minimal GREEN, focused regression, and an intentional commit
  for each implementation slice.

## File map

### Shared classification contract

| Path | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/classification/application/error/ClassificationErrors.java` | Eight fixed public descriptors. |
| `src/main/java/com/project/optrabidz/classification/application/exception/StartupClassificationProfileRequiredException.java` | Missing startup participant prerequisite. |
| `src/main/java/com/project/optrabidz/classification/application/exception/StartupClassificationAlreadyExistsException.java` | Duplicate startup entry. |
| `src/main/java/com/project/optrabidz/classification/application/exception/StartupClassificationNotFoundException.java` | Missing startup entry on removal. |
| `src/main/java/com/project/optrabidz/classification/application/exception/StartupClassificationRuleViolationException.java` | Startup integrity, cardinality, or type-policy failure. |
| `src/main/java/com/project/optrabidz/classification/application/exception/InvestorPreferenceProfileRequiredException.java` | Missing investor participant prerequisite. |
| `src/main/java/com/project/optrabidz/classification/application/exception/InvestorPreferenceAlreadyExistsException.java` | Duplicate investor entry. |
| `src/main/java/com/project/optrabidz/classification/application/exception/InvestorPreferenceNotFoundException.java` | Missing investor entry on removal. |
| `src/main/java/com/project/optrabidz/classification/application/exception/InvestorPreferenceRuleViolationException.java` | Investor integrity, cardinality, or type-policy failure. |
| `src/test/java/com/project/optrabidz/classification/application/ClassificationErrorContractTest.java` | Freezes descriptors, typed exceptions, and protected diagnostics. |

### Startup slice

| Path | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/classification/application/StartupClassificationService.java` | Selects startup prerequisite, duplicate, and not-found failures. |
| `src/main/java/com/project/optrabidz/classification/application/specification/StartupClassificationUniquenessSpec.java` | Selects the startup duplicate failure. |
| `src/main/java/com/project/optrabidz/classification/application/specification/StartupClassificationIntegritySpec.java` | Selects startup rule violations. |
| `src/main/java/com/project/optrabidz/classification/application/specification/StartupClassificationCardinalitySpec.java` | Selects startup cardinality violations. |
| `src/main/java/com/project/optrabidz/classification/application/policy/DefaultStartupClassificationTypePolicy.java` | Selects startup value-policy violations. |
| `src/test/java/com/project/optrabidz/classification/application/StartupClassificationServiceTest.java` | Verifies service failures, side-effect guards, and success. |
| `src/test/java/com/project/optrabidz/classification/application/StartupClassificationRuleTest.java` | Verifies startup uniqueness and rule mappings. |
| `src/test/java/com/project/optrabidz/classification/api/StartupClassificationApiIT.java` | Startup RFC 9457 and success regressions. |

### Investor slice

| Path | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/classification/application/InvestorPreferenceService.java` | Selects investor prerequisite, duplicate, and not-found failures. |
| `src/main/java/com/project/optrabidz/classification/application/specification/InvestorPreferenceUniquenessSpec.java` | Selects the investor duplicate failure. |
| `src/main/java/com/project/optrabidz/classification/application/specification/InvestorPreferenceIntegritySpec.java` | Selects investor rule violations. |
| `src/main/java/com/project/optrabidz/classification/application/specification/InvestorPreferenceCardinalitySpec.java` | Selects investor cardinality violations. |
| `src/main/java/com/project/optrabidz/classification/application/policy/DefaultInvestorPreferenceTypePolicy.java` | Selects investor value-policy violations. |
| `src/test/java/com/project/optrabidz/classification/application/InvestorPreferenceServiceTest.java` | Verifies service failures, side-effect guards, and success. |
| `src/test/java/com/project/optrabidz/classification/application/InvestorPreferenceRuleTest.java` | Verifies investor uniqueness and rule mappings. |
| `src/test/java/com/project/optrabidz/classification/api/InvestorPreferenceApiIT.java` | Investor RFC 9457 and success regressions. |

The obsolete `ClassificationAlreadyExistsException.java` and
`InvalidClassificationException.java` are deleted after both actor slices are
green and a reference scan finds no remaining consumer.

### Contract and documentation verification

| Path | Responsibility |
|---|---|
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Adds classification to the migrated-module rule. |
| `docs/error-handling/README.md` | Records classification as a neutral-contract module and links KAN-26. |
| `docs/error-handling/work-items/KAN-26-classification-error-migration/implementation-plan.md` | Tracks completed steps and exact verification evidence. |

---

## Task 1: Add the classification error contract

**Consumes:** `ApplicationException`, `ErrorDescriptor`, and `ErrorCategory`.

**Produces:** `ClassificationErrors` and the eight typed exceptions with the
constructor signatures defined below.

- [ ] **Step 1: Write the failing contract test**

Create `ClassificationErrorContractTest` and freeze all eight descriptors. The
startup assertions begin with:

```java
assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_PROFILE_REQUIRED)
        .isEqualTo(new ErrorDescriptor(
                "STARTUP_CLASSIFICATION_PROFILE_REQUIRED",
                ErrorCategory.BUSINESS_RULE,
                "Create a startup profile before managing classifications"
        ));
assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_ALREADY_EXISTS)
        .isEqualTo(new ErrorDescriptor(
                "STARTUP_CLASSIFICATION_ALREADY_EXISTS",
                ErrorCategory.CONFLICT,
                "The startup classification already exists"
        ));
assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_NOT_FOUND)
        .isEqualTo(new ErrorDescriptor(
                "STARTUP_CLASSIFICATION_NOT_FOUND",
                ErrorCategory.NOT_FOUND,
                "The requested startup classification was not found"
        ));
assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_RULE_VIOLATION)
        .isEqualTo(new ErrorDescriptor(
                "STARTUP_CLASSIFICATION_RULE_VIOLATION",
                ErrorCategory.BUSINESS_RULE,
                "The startup classification does not satisfy classification rules"
        ));
```

Freeze the remaining descriptors with these exact values:

| Constant | Code | Category | Public detail |
|---|---|---|---|
| `INVESTOR_PREFERENCE_PROFILE_REQUIRED` | `INVESTOR_PREFERENCE_PROFILE_REQUIRED` | `BUSINESS_RULE` | Create an investor profile before managing preferences |
| `INVESTOR_PREFERENCE_ALREADY_EXISTS` | `INVESTOR_PREFERENCE_ALREADY_EXISTS` | `CONFLICT` | The investor preference already exists |
| `INVESTOR_PREFERENCE_NOT_FOUND` | `INVESTOR_PREFERENCE_NOT_FOUND` | `NOT_FOUND` | The requested investor preference was not found |
| `INVESTOR_PREFERENCE_RULE_VIOLATION` | `INVESTOR_PREFERENCE_RULE_VIOLATION` | `BUSINESS_RULE` | The investor preference does not satisfy preference rules |

Assert typed failure separation with representative protected values:

```java
StartupClassificationAlreadyExistsException failure =
        new StartupClassificationAlreadyExistsException("SECTOR", "SECRET-VALUE");

assertThat(failure.descriptor())
        .isSameAs(ClassificationErrors.STARTUP_CLASSIFICATION_ALREADY_EXISTS);
assertThat(failure.diagnosticCode())
        .isEqualTo("CLASSIFICATION.STARTUP.ALREADY_EXISTS");
assertThat(failure.getMessage()).contains("SECTOR", "SECRET-VALUE");
assertThat(failure.descriptor().publicMessage())
        .doesNotContain("SECTOR", "SECRET-VALUE");
```

Instantiate all eight exceptions and assert these diagnostic codes:

| Exception | Diagnostic code |
|---|---|
| `StartupClassificationProfileRequiredException(Long accountId)` | `CLASSIFICATION.STARTUP.PROFILE_REQUIRED` |
| `StartupClassificationAlreadyExistsException(String type, String value)` | `CLASSIFICATION.STARTUP.ALREADY_EXISTS` |
| `StartupClassificationNotFoundException(String type, String value)` | `CLASSIFICATION.STARTUP.NOT_FOUND` |
| `StartupClassificationRuleViolationException(String diagnosticMessage)` | `CLASSIFICATION.STARTUP.RULE_VIOLATION` |
| `InvestorPreferenceProfileRequiredException(Long accountId)` | `CLASSIFICATION.INVESTOR.PROFILE_REQUIRED` |
| `InvestorPreferenceAlreadyExistsException(String type, String value)` | `CLASSIFICATION.INVESTOR.ALREADY_EXISTS` |
| `InvestorPreferenceNotFoundException(String type, String value)` | `CLASSIFICATION.INVESTOR.NOT_FOUND` |
| `InvestorPreferenceRuleViolationException(String diagnosticMessage)` | `CLASSIFICATION.INVESTOR.RULE_VIOLATION` |

- [ ] **Step 2: Run the contract test and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=ClassificationErrorContractTest" test
```

Expected RED: compilation fails because the catalogue and typed exceptions do
not exist.

- [ ] **Step 3: Implement the fixed catalogue and typed exceptions**

Create `ClassificationErrors` as a non-instantiable class. Each constant must
exactly match the code, category, and public detail in the design. Implement
each exception with a fixed descriptor and diagnostic code. The duplicate
pattern is:

```java
public final class StartupClassificationAlreadyExistsException
        extends ApplicationException {

    public StartupClassificationAlreadyExistsException(String type, String value) {
        super(
                ClassificationErrors.STARTUP_CLASSIFICATION_ALREADY_EXISTS,
                "CLASSIFICATION.STARTUP.ALREADY_EXISTS",
                "Startup classification already exists: " + type + "=" + value
        );
    }
}
```

Profile-required constructors mention the protected account ID only in the
diagnostic message. Not-found constructors mention the protected type and
value only in the diagnostic message. Rule-violation constructors accept the
existing protected rule message and never copy it into the descriptor.

- [ ] **Step 4: Run focused GREEN and diff checks**

```powershell
.\mvnw.cmd -B "-Dtest=ClassificationErrorContractTest" test
git diff --check
```

Expected: the contract test passes and the diff check is clean.

- [ ] **Step 5: Commit the neutral classification contract**

```powershell
git add -- src/main/java/com/project/optrabidz/classification/application/error src/main/java/com/project/optrabidz/classification/application/exception src/test/java/com/project/optrabidz/classification/application/ClassificationErrorContractTest.java
git commit -m "feat: add classification error contract (KAN-26)"
```

---

## Task 2: Migrate startup classification failures

**Consumes:** Task 1 startup exceptions, `StartupClassificationRepository`,
`ParticipationActorQueryPort`, `StartupClassificationRuleEngine`, and
`EventPublisher`.

**Produces:** Startup services and rules no longer emit a legacy exception.

- [ ] **Step 1: Write failing startup service tests**

Create `StartupClassificationServiceTest` with Mockito. Cover missing startup
participant, duplicate add, missing removal, and one successful add. The
missing-participant guard is:

```java
when(participationActorQueryPort.findStartupIdByAccountId(41L))
        .thenReturn(Optional.empty());

assertThatThrownBy(() -> service.addClassification(
        new AddStartupClassificationCommand(41L, "SECTOR", "FINTECH")
)).isInstanceOf(StartupClassificationProfileRequiredException.class);

verifyNoInteractions(startupClassificationRepository, startupClassificationRuleEngine);
verify(eventPublisher, never()).publish(any());
```

For a duplicate, return startup ID `7L` and a profile containing
`SECTOR=FINTECH`; assert `StartupClassificationAlreadyExistsException` and no
rule validation, save, or event. For removal of a missing entry, assert
`StartupClassificationNotFoundException` and no save or event. The success test
asserts rule validation, `saveAll`, one `StartupClassificationChangedEvent`,
and the unchanged success message.

- [ ] **Step 2: Write failing startup rule tests**

Create `StartupClassificationRuleTest` and test the concrete specifications
without Spring:

```java
StartupClassificationProfile duplicate = StartupClassificationProfile.establish(
        7L,
        List.of(
                StartupClassification.create("SECTOR", "FINTECH"),
                StartupClassification.create("SECTOR", "FINTECH")
        )
);

assertThatThrownBy(() -> new StartupClassificationUniquenessSpec().validate(duplicate))
        .isInstanceOf(StartupClassificationAlreadyExistsException.class);
```

Use `StartupClassificationIntegritySpec(List.of())` with a nonblank entry to
exercise the unsupported-type path. Use a test policy with
`maxAllowedPerType()` returning `1` and a two-entry profile to exercise
cardinality. Call `DefaultStartupClassificationTypePolicy.validateValue(" ")`
directly for the value-policy path. Each non-duplicate case must assert
`StartupClassificationRuleViolationException`.

- [ ] **Step 3: Update startup API contract tests**

In `StartupClassificationApiIT`, replace legacy duplicate and profile-required
envelope assertions with RFC 9457 assertions. The duplicate response must
assert:

```java
.andExpect(status().isConflict())
.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
.andExpect(jsonPath("$.status").value(409))
.andExpect(jsonPath("$.code").value("STARTUP_CLASSIFICATION_ALREADY_EXISTS"))
.andExpect(jsonPath("$.detail").value("The startup classification already exists"))
.andExpect(jsonPath("$.requestId").isNotEmpty())
.andExpect(jsonPath("$.timestamp").isNotEmpty())
.andExpect(jsonPath("$.success").doesNotExist())
.andExpect(jsonPath("$.error").doesNotExist());
```

Change the startup-without-profile case to 422,
`STARTUP_CLASSIFICATION_PROFILE_REQUIRED`, and
`Create a startup profile before managing classifications`. Add a removal of a
nonexistent entry asserting 404 and `STARTUP_CLASSIFICATION_NOT_FOUND`. Add a
blank `classificationValue` request asserting 400, `VALIDATION_ERROR`,
`application/problem+json`, and a nonempty `violations` array. Assert protected
request value `FINTECH` is absent from the duplicate error body. Preserve the
existing success, role, and CSRF assertions.

- [ ] **Step 4: Run startup tests and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=StartupClassificationServiceTest,StartupClassificationRuleTest" test
.\mvnw.cmd -B -Pintegration-tests "-Dit.test=StartupClassificationApiIT" failsafe:integration-test failsafe:verify
```

Expected RED: unit tests observe legacy exception types and the API test
observes the legacy envelope/status.

- [ ] **Step 5: Migrate startup service and rule throw sites**

In `StartupClassificationService`:

```java
private Long resolveStartupId(Long accountId) {
    return participationActorQueryPort.findStartupIdByAccountId(accountId)
            .orElseThrow(() ->
                    new StartupClassificationProfileRequiredException(accountId));
}
```

Replace the duplicate guard with
`StartupClassificationAlreadyExistsException(type, value)` and the removal
guard with `StartupClassificationNotFoundException(type, value)`. Do not add a
broad catch around `profile.declare`, `replaceAll`, or `revoke`.

Update startup uniqueness, integrity, cardinality, and default type policy to
use the startup-specific exceptions. Pass existing rule messages only as
protected diagnostic text.

- [ ] **Step 6: Run startup GREEN and regression tests**

```powershell
.\mvnw.cmd -B "-Dtest=ClassificationErrorContractTest,StartupClassificationServiceTest,StartupClassificationRuleTest" test
.\mvnw.cmd -B -Pintegration-tests "-Dit.test=StartupClassificationApiIT" failsafe:integration-test failsafe:verify
$legacyStartup = @(
  rg -n "\b(ApiException|ErrorCode|ClassificationAlreadyExistsException|InvalidClassificationException)\b" src/main/java/com/project/optrabidz/classification/application/StartupClassificationService.java src/main/java/com/project/optrabidz/classification/application/policy/DefaultStartupClassificationTypePolicy.java
  rg -n -g "StartupClassification*.java" "\b(ApiException|ErrorCode|ClassificationAlreadyExistsException|InvalidClassificationException)\b" src/main/java/com/project/optrabidz/classification/application/specification
)
if ($legacyStartup) { throw "legacy startup exception dependency remains: $legacyStartup" }
git diff --check
```

Expected: focused unit and PostgreSQL API tests pass; the reference scan prints
no match in startup files; diff check is clean.

- [ ] **Step 7: Commit the startup slice**

```powershell
git add -- src/main/java/com/project/optrabidz/classification/application/StartupClassificationService.java src/main/java/com/project/optrabidz/classification/application/specification/StartupClassification* src/main/java/com/project/optrabidz/classification/application/policy/DefaultStartupClassificationTypePolicy.java src/test/java/com/project/optrabidz/classification/application/StartupClassificationServiceTest.java src/test/java/com/project/optrabidz/classification/application/StartupClassificationRuleTest.java src/test/java/com/project/optrabidz/classification/api/StartupClassificationApiIT.java
git commit -m "refactor: migrate startup classification errors (KAN-26)"
```

---

## Task 3: Migrate investor preference failures

**Consumes:** Task 1 investor exceptions, `InvestorPreferenceRepository`,
`ParticipationActorQueryPort`, `InvestorPreferenceRuleEngine`, and
`EventPublisher`.

**Produces:** Investor services and rules no longer emit a legacy exception;
the two generic legacy classification exceptions are removed.

- [ ] **Step 1: Write failing investor service and rule tests**

Create `InvestorPreferenceServiceTest` with the same four independently named
scenarios as the startup service, using:

```java
when(participationActorQueryPort.findInvestorIdByAccountId(41L))
        .thenReturn(Optional.empty());

assertThatThrownBy(() -> service.addPreference(
        new AddInvestorPreferenceCommand(41L, "SECTOR", "FINTECH")
)).isInstanceOf(InvestorPreferenceProfileRequiredException.class);

verifyNoInteractions(investorPreferenceRepository, investorPreferenceRuleEngine);
verify(eventPublisher, never()).publish(any());
```

Create `InvestorPreferenceRuleTest`. Use a duplicate profile for
`InvestorPreferenceUniquenessSpec`, an empty policy list for the unsupported
type path, a test policy capped at one entry for cardinality, and direct blank
value validation for `DefaultInvestorPreferenceTypePolicy`. Assert the precise
investor duplicate or rule-violation type in every case.

- [ ] **Step 2: Update investor API contract tests**

In `InvestorPreferenceApiIT`, replace the legacy duplicate response with 409,
`application/problem+json`, code `INVESTOR_PREFERENCE_ALREADY_EXISTS`, detail
`The investor preference already exists`, populated `requestId` and
`timestamp`, and no `success` or `error` property. Assert protected request
value `FINTECH` is absent from the error body.

Change the investor-without-profile case to 422,
`INVESTOR_PREFERENCE_PROFILE_REQUIRED`, and
`Create an investor profile before managing preferences`. Add removal of a
nonexistent preference asserting 404 and `INVESTOR_PREFERENCE_NOT_FOUND`. Add a
blank `preferenceValue` request asserting 400, `VALIDATION_ERROR`,
`application/problem+json`, and a nonempty `violations` array. Preserve the
existing success, role, and CSRF assertions.

- [ ] **Step 3: Run investor tests and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=InvestorPreferenceServiceTest,InvestorPreferenceRuleTest" test
.\mvnw.cmd -B -Pintegration-tests "-Dit.test=InvestorPreferenceApiIT" failsafe:integration-test failsafe:verify
```

Expected RED: unit tests observe legacy exception types and the API test
observes the legacy envelope/status.

- [ ] **Step 4: Migrate investor service and rule throw sites**

Use `InvestorPreferenceProfileRequiredException(accountId)` in
`resolveInvestorId`, `InvestorPreferenceAlreadyExistsException(type, value)` in
duplicate guards and uniqueness rules, `InvestorPreferenceNotFoundException`
for missing removal targets, and `InvestorPreferenceRuleViolationException`
for integrity, cardinality, and type-policy failures. Do not add a broad domain
exception catch.

- [ ] **Step 5: Delete the obsolete generic exceptions and prove isolation**

Delete:

```text
src/main/java/com/project/optrabidz/classification/application/exception/ClassificationAlreadyExistsException.java
src/main/java/com/project/optrabidz/classification/application/exception/InvalidClassificationException.java
```

Then run:

```powershell
if (rg -n "\b(ClassificationAlreadyExistsException|InvalidClassificationException|ApiException|ErrorCode)\b" src/main/java/com/project/optrabidz/classification) { throw 'legacy classification exception dependency remains' }
```

Expected: no exception is thrown and no match is printed.

- [ ] **Step 6: Run classification unit and investor API tests GREEN**

```powershell
.\mvnw.cmd -B "-Dtest=ClassificationErrorContractTest,StartupClassificationServiceTest,StartupClassificationRuleTest,InvestorPreferenceServiceTest,InvestorPreferenceRuleTest" test
.\mvnw.cmd -B -Pintegration-tests "-Dit.test=InvestorPreferenceApiIT" failsafe:integration-test failsafe:verify
git diff --check
```

Expected: all focused classification unit tests and the investor PostgreSQL API
test pass; diff check is clean.

- [ ] **Step 7: Commit the investor slice**

```powershell
git add -A -- src/main/java/com/project/optrabidz/classification src/test/java/com/project/optrabidz/classification/application src/test/java/com/project/optrabidz/classification/api/InvestorPreferenceApiIT.java
git commit -m "refactor: migrate investor preference errors (KAN-26)"
```

---

## Task 4: Enforce the boundary and complete verification

**Consumes:** Tasks 1–3, ArchUnit, documentation validation, and unchanged
PostgreSQL/Flyway infrastructure.

**Produces:** An enforced classification boundary, topic navigation, and
complete verification evidence.

- [ ] **Step 1: Extend the architecture boundary**

Add `"..classification.."` to
`MIGRATED_MODULES_DO_NOT_USE_LEGACY_API_EXCEPTIONS` in
`ExceptionArchitectureTest`:

```java
.that().resideInAnyPackage(
        "..identity..",
        "..security..",
        "..participation..",
        "..classification.."
)
```

Run:

```powershell
.\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
```

Expected: all architecture rules pass without adding a freeze-store waiver.

- [ ] **Step 2: Update the error-handling topic index**

Update `docs/error-handling/README.md` so the current-system text includes
classification and the work-item table links both KAN-26 documents:

```markdown
| [KAN-26](https://0707manna0895.atlassian.net/browse/KAN-26) | [Classification error migration design](work-items/KAN-26-classification-error-migration/design.md) and [implementation plan](work-items/KAN-26-classification-error-migration/implementation-plan.md) |
```

Run:

```powershell
.\mvnw.cmd -B "-Dtest=DocumentationLinksTest" test
```

Expected: the documentation link test passes.

- [ ] **Step 3: Run the complete unit suite**

```powershell
.\mvnw.cmd -B test
```

Expected: `BUILD SUCCESS` with zero failures and errors.

- [ ] **Step 4: Run the complete PostgreSQL integration suite**

```powershell
.\mvnw.cmd -B verify -Pintegration-tests
```

Expected: `BUILD SUCCESS` with zero failures and errors.

- [ ] **Step 5: Perform final scope and safety checks**

```powershell
if (rg -n "\b(ClassificationAlreadyExistsException|InvalidClassificationException|ApiException|ErrorCode)\b" src/main/java/com/project/optrabidz/classification) { throw 'legacy classification exception dependency remains' }
git diff --check origin/develop...HEAD
git diff --name-only origin/develop...HEAD -- src/main/resources pom.xml .github/workflows
git status --short
```

Expected: no legacy match, clean diff, no protected configuration path, and no
uncommitted file.

- [ ] **Step 6: Record evidence and commit the final slice**

Update this plan with the exact focused, unit, and integration test totals and
the verified commit range. Then commit the architecture rule, documentation
index, and execution evidence:

```powershell
git add -- src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java docs/error-handling
git commit -m "test: verify classification error migration (KAN-26)"
```

- [ ] **Step 7: Push the review branch and open the pull request**

```powershell
git push -u origin feature/KAN-26-classification-error-migration
$body = "## Summary`n- add actor-specific classification error descriptors`n- migrate startup and investor expected failures to ApplicationException`n- expose classification failures as safe RFC 9457 Problem Details`n- enforce the classification exception boundary with ArchUnit`n`n## Verification`n- focused classification unit tests passed`n- focused PostgreSQL API integration tests passed`n- complete unit suite passed`n- complete PostgreSQL integration suite passed`n`n## Risk and rollback`nClassification failure responses intentionally change from the legacy envelope to RFC 9457. Successful responses, persistence, events, Flyway V1, and other modules remain unchanged. Roll back by reverting this pull request."
gh pr create --base develop --head feature/KAN-26-classification-error-migration --title "KAN-26: Migrate classification failures to the neutral error contract" --body $body
```

The pull request is merged only after review is complete and required checks
pass.

## Execution evidence

Record the verified base/head commit IDs, RED failure causes, focused test
totals, complete unit and integration totals, architecture result, unchanged V1
blob, and pull-request URL here during Task 4.
