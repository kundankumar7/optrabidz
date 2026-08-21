# KAN-27 Governance Error Migration Implementation Plan

**Goal:** Replace governance-module legacy exceptions with a grouped,
transport-neutral error contract and disclosure-safe admin recovery failures.

**Architecture:** Internal `GovernanceRuleCode` values remain the detailed
policy vocabulary. `GovernanceErrors` maps denied decisions to five stable
public descriptors, while the existing REST adapter remains the sole RFC 9457
response builder.

**Tech stack:** Java 21, Spring Boot 3.3, Spring MVC, JUnit 5, Mockito,
AssertJ, MockMvc, ArchUnit, Maven, Testcontainers, PostgreSQL 16

**Spec:** [KAN-27 design](design.md)

## Global constraints

- Base implementation work on the latest verified `develop`.
- Use `feature/KAN-27-governance-error-migration`; the pull request targets
  `develop`, never `main`.
- Move KAN-27 to **In Progress** only when implementation begins, **In Review**
  when the pull request opens, and **Done** only after merge and verification.
- Do not change the Flyway baseline, database schema, runtime properties,
  dependencies, security matchers, CSRF rules, or successful response model.
- Do not redesign admin reinstatement, bootstrap, transfer, revocation,
  credentials, notifications, payments, logging, or audit behavior.
- Public Problem Details never contain tokens, configuration values, account
  IDs, roles, states, module names, action names, internal rule codes,
  exception messages, or diagnostic codes.
- Preserve constant-work recovery-token comparison with
  `MessageDigest.isEqual`.
- Every production change begins with a focused failing test and ends with a
  focused passing test before commit.

---

## Execution gate

Run this only after the implementation plan is approved:

```powershell
git fetch origin
git switch develop
if ((git rev-parse HEAD) -ne (git rev-parse origin/develop)) {
    throw 'local develop must equal origin/develop'
}
$expected = @(
    '?? docs/error-handling/work-items/KAN-27-governance-error-migration/design.md',
    '?? docs/error-handling/work-items/KAN-27-governance-error-migration/implementation-plan.md'
)
$actual = @(git status --porcelain --untracked-files=all)
if (Compare-Object $expected $actual) {
    throw 'only the two approved KAN-27 documents may be pending'
}
git switch -c feature/KAN-27-governance-error-migration
git add docs/error-handling/work-items/KAN-27-governance-error-migration
git commit -m "docs: plan governance error migration (KAN-27)"
```

Verify the new branch is based on `origin/develop`, then transition KAN-27 from
**To Do** to **In Progress**. Stop if the base differs, the worktree contains an
unapproved path, or Jira does not show the expected starting status.

---

## File map

| Path | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/governance/application/error/GovernanceErrors.java` | Owns the five public descriptors and exhaustive internal-rule mapping. |
| `src/main/java/com/project/optrabidz/governance/application/common/GovernanceException.java` | Converts one denied decision into a protected neutral application exception. |
| `src/main/java/com/project/optrabidz/governance/application/admin/exception/AdminRecoveryAccessDeniedException.java` | Uses one public recovery descriptor with cause-specific protected diagnostics. |
| `src/main/java/com/project/optrabidz/governance/application/admin/exception/AdminAuthorityUnavailableException.java` | Represents the no-active-authority transfer conflict. |
| `src/main/java/com/project/optrabidz/governance/application/admin/AdministrativeAuthorityGuard.java` | Raises neutral governance/admin exceptions before mutations. |
| `src/main/java/com/project/optrabidz/governance/api/AdminRecoveryController.java` | Classifies recovery boundary failures without constructing response bodies. |
| `src/test/java/com/project/optrabidz/governance/application/GovernanceErrorContractTest.java` | Freezes descriptor values, rule mapping, allowed-decision rejection, and disclosure boundaries. |
| `src/test/java/com/project/optrabidz/governance/application/admin/AdministrativeAuthorityGuardTest.java` | Verifies recovery and active-authority guard behavior. |
| `src/test/java/com/project/optrabidz/governance/api/AdminRecoveryControllerTest.java` | Verifies every recovery-access branch and constant-work success path delegation. |
| `src/test/java/com/project/optrabidz/governance/api/AdminRecoveryApiIT.java` | Verifies real Spring MVC Problem Details against the PostgreSQL test context. |
| `src/test/java/com/project/optrabidz/marketplace/api/MarketplaceApiIT.java` | Migrates the existing eligibility-denial API assertion. |
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Adds governance to the legacy-dependency prohibition. |
| `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f` | Removes only the resolved frozen violations owned by the migrated governance exception. |
| `docs/error-handling/README.md` | Adds KAN-27 to the error-handling history after implementation. |
| `docs/error-handling/work-items/KAN-27-governance-error-migration/implementation-plan.md` | Records executed checks and review evidence. |

---

## Task 1: Add the grouped governance error contract

**Files:**

- Create: `src/main/java/com/project/optrabidz/governance/application/error/GovernanceErrors.java`
- Modify: `src/main/java/com/project/optrabidz/governance/application/common/GovernanceException.java`
- Create: `src/test/java/com/project/optrabidz/governance/application/GovernanceErrorContractTest.java`

**Interfaces:**

- Produces: `GovernanceErrors.forRule(GovernanceRuleCode)` returning an
  `ErrorDescriptor` for every denied rule.
- Produces: `GovernanceException(GovernanceDecision)` extending
  `ApplicationException`.
- Consumes: existing `ErrorDescriptor`, `ErrorCategory`,
  `ApplicationException`, `GovernanceDecision`, and `GovernanceRuleCode`.

- [x] **Step 1: Write the failing contract tests**

Create `GovernanceErrorContractTest` with exact descriptor assertions and a
parameterized mapping table. The table must contain every rule except
`ALLOWED`:

```java
@ParameterizedTest
@MethodSource("deniedRuleMappings")
void mapsEveryDeniedRuleToItsApprovedDescriptor(
        GovernanceRuleCode rule,
        ErrorDescriptor expected
) {
    assertThat(GovernanceErrors.forRule(rule)).isSameAs(expected);
}

static Stream<Arguments> deniedRuleMappings() {
    return Stream.of(
            arguments(ACCOUNT_NOT_FOUND, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(ROLE_MISMATCH, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(ACCOUNT_NOT_ACTIVE, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(PROFILE_INCOMPLETE, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(STARTUP_ACTOR_NOT_FOUND, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(INVESTOR_ACTOR_NOT_FOUND, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(STARTUP_CLASSIFICATION_REQUIRED, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(INVESTOR_PREFERENCE_REQUIRED, GOVERNANCE_ACTION_NOT_ELIGIBLE),
            arguments(ADMIN_AUTHORITY_REQUIRED, GOVERNANCE_ACTION_NOT_PERMITTED),
            arguments(NEUTRALITY_VIOLATION, GOVERNANCE_ACTION_NOT_PERMITTED),
            arguments(SYSTEM_BOUNDARY_VIOLATION, GOVERNANCE_ACTION_NOT_PERMITTED),
            arguments(RECOVERY_MODE_REQUIRED, ADMIN_RECOVERY_ACCESS_DENIED),
            arguments(LIFECYCLE_RULE_SKIPPED, GOVERNANCE_STATE_CONFLICT),
            arguments(LIFECYCLE_RULE_FAILED, GOVERNANCE_STATE_CONFLICT)
    );
}
```

Also assert:

```java
assertThatThrownBy(() -> GovernanceErrors.forRule(ALLOWED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ALLOWED is not a governance failure");

GovernanceDecision denied = GovernanceDecision.deny(
        ROLE_MISMATCH,
        "private-role-context",
        "Expected STARTUP but found ADMIN"
);
GovernanceException failure = new GovernanceException(denied);
assertThat(failure.descriptor()).isSameAs(GOVERNANCE_ACTION_NOT_ELIGIBLE);
assertThat(failure.diagnosticCode()).isEqualTo("GOVERNANCE.ROLE_MISMATCH");
assertThat(failure.getMessage()).contains("ADMIN");
assertThat(failure.descriptor().publicMessage())
        .doesNotContain("ADMIN", "private-role-context", "ROLE_MISMATCH");
```

Test each descriptor against the exact code, category, and public detail from
the approved design.

- [x] **Step 2: Run the focused test and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=GovernanceErrorContractTest" test
```

Expected RED: `GovernanceErrors` does not exist and the legacy
`GovernanceException` is not an `ApplicationException`.

- [x] **Step 3: Implement `GovernanceErrors`**

Create five `public static final ErrorDescriptor` constants with the exact
approved values. Implement an exhaustive switch:

```java
public static ErrorDescriptor forRule(GovernanceRuleCode ruleCode) {
    Objects.requireNonNull(ruleCode, "ruleCode must not be null");
    return switch (ruleCode) {
        case ACCOUNT_NOT_FOUND, ROLE_MISMATCH, ACCOUNT_NOT_ACTIVE,
                PROFILE_INCOMPLETE, STARTUP_ACTOR_NOT_FOUND,
                INVESTOR_ACTOR_NOT_FOUND, STARTUP_CLASSIFICATION_REQUIRED,
                INVESTOR_PREFERENCE_REQUIRED -> GOVERNANCE_ACTION_NOT_ELIGIBLE;
        case ADMIN_AUTHORITY_REQUIRED, NEUTRALITY_VIOLATION,
                SYSTEM_BOUNDARY_VIOLATION -> GOVERNANCE_ACTION_NOT_PERMITTED;
        case RECOVERY_MODE_REQUIRED -> ADMIN_RECOVERY_ACCESS_DENIED;
        case LIFECYCLE_RULE_SKIPPED, LIFECYCLE_RULE_FAILED ->
                GOVERNANCE_STATE_CONFLICT;
        case ALLOWED -> throw new IllegalArgumentException(
                "ALLOWED is not a governance failure"
        );
    };
}
```

The descriptor declarations must use the design's exact public strings and
categories; do not import HTTP or Spring types.

- [x] **Step 4: Migrate `GovernanceException` in place**

Replace the legacy superclass and remove the unused string constructor:

```java
public final class GovernanceException extends ApplicationException {
    public GovernanceException(GovernanceDecision decision) {
        super(
                GovernanceErrors.forRule(requireDenied(decision).code()),
                "GOVERNANCE." + requireDenied(decision).code().name(),
                requireDenied(decision).message()
        );
    }

    private static GovernanceDecision requireDenied(GovernanceDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision.allowed()) {
            throw new IllegalArgumentException(
                    "allowed decision cannot create a governance exception"
            );
        }
        return decision;
    }
}
```

Do not copy `GovernanceViolation` values into public `ErrorDetail` entries.

- [x] **Step 5: Run the focused tests and verify GREEN**

```powershell
.\mvnw.cmd -B "-Dtest=GovernanceErrorContractTest" test
```

Expected GREEN: all descriptor, mapping, allowed-decision, and protected-data
assertions pass.

- [x] **Step 6: Commit the contract slice**

```powershell
git add src/main/java/com/project/optrabidz/governance/application/error `
        src/main/java/com/project/optrabidz/governance/application/common/GovernanceException.java `
        src/test/java/com/project/optrabidz/governance/application/GovernanceErrorContractTest.java
git commit -m "feat: add governance error contract (KAN-27)"
```

---

## Task 2: Migrate admin authority and recovery failures

**Files:**

- Create: `src/main/java/com/project/optrabidz/governance/application/admin/exception/AdminRecoveryAccessDeniedException.java`
- Create: `src/main/java/com/project/optrabidz/governance/application/admin/exception/AdminAuthorityUnavailableException.java`
- Modify: `src/main/java/com/project/optrabidz/governance/application/admin/AdministrativeAuthorityGuard.java`
- Modify: `src/main/java/com/project/optrabidz/governance/api/AdminRecoveryController.java`
- Create: `src/test/java/com/project/optrabidz/governance/application/admin/AdministrativeAuthorityGuardTest.java`
- Create: `src/test/java/com/project/optrabidz/governance/api/AdminRecoveryControllerTest.java`

**Interfaces:**

- Produces: `AdminRecoveryAccessDeniedException.recoveryModeDisabled()`,
  `.tokenNotConfigured()`, `.tokenMissing()`, and `.tokenRejected()`.
- Produces: public no-argument `AdminAuthorityUnavailableException()`.
- Consumes: `GovernanceErrors.ADMIN_RECOVERY_ACCESS_DENIED` and
  `GovernanceErrors.ADMIN_AUTHORITY_UNAVAILABLE`.

- [x] **Step 1: Write failing authority-guard tests**

Use a mocked `AdminAuthorityQueryPort`. Cover these exact branches:

```java
assertThatThrownBy(() -> guard.assertRecoveryTransferAllowed(false))
        .isInstanceOf(AdminRecoveryAccessDeniedException.class)
        .extracting(failure -> ((ApplicationException) failure).descriptor())
        .isSameAs(GovernanceErrors.ADMIN_RECOVERY_ACCESS_DENIED);

when(adminAuthorityQueryPort.activeAdminExists()).thenReturn(false);
assertThatThrownBy(() -> guard.assertRecoveryTransferAllowed(true))
        .isInstanceOf(AdminAuthorityUnavailableException.class);

when(adminAuthorityQueryPort.activeAdminExists()).thenReturn(true);
assertThatCode(() -> guard.assertRecoveryTransferAllowed(true))
        .doesNotThrowAnyException();
```

For `assertActiveAdmin`, verify false produces a `GovernanceException` mapped
to `GOVERNANCE_ACTION_NOT_PERMITTED`, while true returns normally.

- [x] **Step 2: Write failing recovery-controller tests**

Construct the controller with mocked `AdminAuthorityTransferService` and a real
mutable `AdminBootstrapProperties`. Call `transferAdminAuthority` directly with
a valid request and `MockHttpServletRequest`.

Cover these cases separately:

1. recovery mode disabled;
2. recovery token configuration null;
3. recovery token configuration blank;
4. request header null;
5. request header blank;
6. incorrect request token; and
7. matching token delegates once and returns the existing success response.

For every rejected case, assert the descriptor is the same
`ADMIN_RECOVERY_ACCESS_DENIED`, the expected protected diagnostic code is
selected, and `verifyNoInteractions(transferService)` succeeds. Never assert a
diagnostic containing either token value.

- [x] **Step 3: Run focused tests and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=AdministrativeAuthorityGuardTest,AdminRecoveryControllerTest" test
```

Expected RED: neutral admin exception types do not exist and both production
classes still throw the legacy API exception.

- [x] **Step 4: Implement the typed admin exceptions**

`AdminRecoveryAccessDeniedException` has a private constructor and four public
factories. Use these exact diagnostic codes:

| Factory | Diagnostic code | Diagnostic message |
|---|---|---|
| `recoveryModeDisabled()` | `GOVERNANCE.RECOVERY.MODE_DISABLED` | `Admin recovery mode is disabled` |
| `tokenNotConfigured()` | `GOVERNANCE.RECOVERY.TOKEN_NOT_CONFIGURED` | `Admin recovery token is not configured` |
| `tokenMissing()` | `GOVERNANCE.RECOVERY.TOKEN_MISSING` | `Admin recovery token was not submitted` |
| `tokenRejected()` | `GOVERNANCE.RECOVERY.TOKEN_REJECTED` | `Submitted admin recovery token was rejected` |

All factories select the same public descriptor. No constructor or factory
accepts a token value.

`AdminAuthorityUnavailableException` selects
`ADMIN_AUTHORITY_UNAVAILABLE`, diagnostic code
`GOVERNANCE.ADMIN_AUTHORITY.UNAVAILABLE`, and protected message
`No active admin authority exists to transfer`.

- [x] **Step 5: Migrate `AdministrativeAuthorityGuard`**

Replace direct `ApiException` construction:

```java
if (!recoveryModeEnabled) {
    throw AdminRecoveryAccessDeniedException.recoveryModeDisabled();
}
if (!adminAuthorityQueryPort.activeAdminExists()) {
    throw new AdminAuthorityUnavailableException();
}
```

For `assertActiveAdmin`, create a denied `GovernanceDecision` using
`ADMIN_AUTHORITY_REQUIRED`; do not include `accountId` in its message or public
details. Remove all legacy imports.

- [x] **Step 6: Migrate `AdminRecoveryController`**

Keep the existing check order and constant-work comparison. Replace the three
legacy branches with four neutral branches:

```java
if (!properties.isRecoveryMode()) {
    throw AdminRecoveryAccessDeniedException.recoveryModeDisabled();
}
if (properties.getRecoveryToken() == null
        || properties.getRecoveryToken().isBlank()) {
    throw AdminRecoveryAccessDeniedException.tokenNotConfigured();
}
if (recoveryToken == null || recoveryToken.isBlank()) {
    throw AdminRecoveryAccessDeniedException.tokenMissing();
}
if (!secureEquals(properties.getRecoveryToken(), recoveryToken)) {
    throw AdminRecoveryAccessDeniedException.tokenRejected();
}
```

Do not change the request mapping, recovery header name, validation, success
response, transfer command, or `secureEquals` implementation.

- [x] **Step 7: Run focused tests and verify GREEN**

```powershell
.\mvnw.cmd -B "-Dtest=GovernanceErrorContractTest,AdministrativeAuthorityGuardTest,AdminRecoveryControllerTest" test
```

Expected GREEN: every guard and recovery branch passes, delegation occurs only
for the matching token, and no test accesses raw secret values through the
exception.

- [x] **Step 8: Commit the admin-recovery slice**

```powershell
git add src/main/java/com/project/optrabidz/governance/application/admin `
        src/main/java/com/project/optrabidz/governance/api/AdminRecoveryController.java `
        src/test/java/com/project/optrabidz/governance/application/admin `
        src/test/java/com/project/optrabidz/governance/api/AdminRecoveryControllerTest.java
git commit -m "refactor: migrate governance recovery failures (KAN-27)"
```

---

## Task 3: Verify public API and architecture boundaries

**Files:**

- Create: `src/test/java/com/project/optrabidz/governance/api/AdminRecoveryApiIT.java`
- Modify: `src/test/java/com/project/optrabidz/marketplace/api/MarketplaceApiIT.java`
- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify: `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f`

**Interfaces:**

- Consumes: the Task 1 descriptors, Task 2 recovery exceptions, existing
  `ProblemDetailsFactory`, `RestExceptionHandler`, and Testcontainers support.
- Produces: end-to-end evidence for the RFC 9457 contract and a permanent
  architecture guard for governance.

- [x] **Step 1: Migrate the marketplace eligibility API assertion first**

In `publishingRequiresStartupClassificationWhenGovernanceRequiresIt`, replace
the legacy 403-envelope assertions with:

```java
.andExpect(status().isUnprocessableEntity())
.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
.andExpect(jsonPath("$.status").value(422))
.andExpect(jsonPath("$.code").value("GOVERNANCE_ACTION_NOT_ELIGIBLE"))
.andExpect(jsonPath("$.detail").value(
        "The requested action does not satisfy governance eligibility rules"
))
.andExpect(jsonPath("$.success").doesNotExist())
.andExpect(jsonPath("$.error").doesNotExist());
```

Supply an `X-Request-Id` and assert `requestId`, `instance`, and `timestamp`.
Assert that the body does not contain the internal startup eligibility message.

- [x] **Step 2: Write `AdminRecoveryApiIT`**

Extend `ApiIntegrationTestSupport` and add:

```java
@TestPropertySource(properties = {
        "optrabidz.admin.bootstrap.recovery-mode=true",
        "optrabidz.admin.bootstrap.recovery-token=test-recovery-token-27"
})
class AdminRecoveryApiIT extends ApiIntegrationTestSupport {
    @Autowired
    private JdbcTemplate jdbcTemplate;
}
```

The rejected-token test posts a valid transfer request with an incorrect test
token and asserts 403, `application/problem+json`, exact type/title/code/detail,
request metadata, and absence of both test token strings and diagnostic text.

The unavailable-authority test first asserts:

```java
assertThat(jdbcTemplate.queryForObject(
        "select count(*) from admin where admin_state = 'ACTIVE'",
        Integer.class
)).isZero();
```

Then post the same valid body with `test-recovery-token-27` and assert the exact
409 `ADMIN_AUTHORITY_UNAVAILABLE` Problem Details. Verify the active-admin count
remains zero after the request, proving the failure occurred before mutation.

Use only obviously synthetic test credentials. Do not log or persist either
header value in test evidence.

- [x] **Step 3: Extend the architecture rule**

Add `"..governance.."` to `MIGRATED_MODULES_DO_NOT_USE_LEGACY_API_EXCEPTIONS`.
Keep the frozen transport-neutral exception rule unchanged. Remove only the
five obsolete `GovernanceException` entries from its violation store; never
enable automatic store creation or updates.

- [x] **Step 4: Run the governance unit tests and focused integration tests**

```powershell
.\mvnw.cmd -B "-Dtest=GovernanceErrorContractTest,AdministrativeAuthorityGuardTest,AdminRecoveryControllerTest,ExceptionArchitectureTest" test
.\mvnw.cmd -B verify -Pintegration-tests "-Dit.test=AdminRecoveryApiIT,MarketplaceApiIT"
```

Expected GREEN: all focused tests pass. `AdminRecoveryApiIT` performs no
successful transfer and leaves the active-admin count unchanged.

- [x] **Step 5: Run explicit disclosure and legacy-dependency scans**

```powershell
if (rg -n "\b(ApiException|ErrorCode)\b" src/main/java/com/project/optrabidz/governance) {
    throw 'legacy governance exception dependency remains'
}
if (rg -n "getErrorCode\(|common\.api\.exception" src/main/java/com/project/optrabidz/governance) {
    throw 'legacy governance API coupling remains'
}
git diff --check
```

Expected: both searches produce no matches and `git diff --check` exits zero.

- [x] **Step 6: Commit the API and architecture slice**

```powershell
git add src/test/java/com/project/optrabidz/governance/api/AdminRecoveryApiIT.java `
        src/test/java/com/project/optrabidz/marketplace/api/MarketplaceApiIT.java `
        src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
        src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f
git commit -m "test: verify governance error migration (KAN-27)"
```

---

## Task 4: Complete regression, documentation, and review handoff

**Files:**

- Modify: `docs/error-handling/README.md`
- Modify: `docs/error-handling/work-items/KAN-27-governance-error-migration/implementation-plan.md`

**Interfaces:**

- Consumes: the complete KAN-27 branch and all prior focused evidence.
- Produces: full regression evidence, navigable documentation, and a reviewed
  pull request targeting `develop`.

- [x] **Step 1: Run the complete unit suite**

```powershell
.\mvnw.cmd -B test
```

Expected: every unit and architecture test passes with zero failures and zero
errors. Record the final test count in this plan.

- [x] **Step 2: Run the complete PostgreSQL integration suite**

Confirm Docker is available, then run:

```powershell
docker version
.\mvnw.cmd -B verify -Pintegration-tests
```

Expected: all unit and PostgreSQL integration tests pass; Flyway applies the
unchanged V1 baseline successfully. Record both final counts.

- [x] **Step 3: Verify protected scope and repository state**

```powershell
$base = git merge-base origin/develop HEAD
git diff --check $base..HEAD
git diff --name-status $base..HEAD
git diff --exit-code $base..HEAD -- `
    pom.xml `
    .github/workflows `
    src/main/resources/db/migration/V1__baseline.sql `
    src/main/resources/application.properties `
    src/main/resources/application-dev.properties `
    src/main/resources/application-prod.properties `
    src/test/resources/application-test.properties
rg -n "\b(ApiException|ErrorCode)\b" src/main/java/com/project/optrabidz/governance
git status --short
```

Expected: only approved KAN-27 paths differ; protected paths have no diff; the
legacy scan is empty; the worktree becomes clean after the evidence commit.

- [x] **Step 4: Update documentation and record evidence**

Update `docs/error-handling/README.md` to state that governance uses the
neutral contract and add one KAN-27 work-item link. In this plan, check only
steps actually executed and record:

- focused RED and GREEN results;
- focused integration results;
- complete unit and integration counts;
- legacy/disclosure scan results;
- protected-path verification; and
- exact branch head reviewed.

Do not add machine-specific paths, usernames, tokens, AI/tool references, or
temporary log filenames.

- [x] **Step 5: Commit documentation evidence**

```powershell
git add docs/error-handling/README.md `
        docs/error-handling/work-items/KAN-27-governance-error-migration
git commit -m "docs: record governance migration evidence (KAN-27)"
```

- [ ] **Step 6: Push and open the pull request**

```powershell
git push -u origin feature/KAN-27-governance-error-migration
gh pr create `
    --base develop `
    --head feature/KAN-27-governance-error-migration `
    --title "KAN-27: Migrate governance failures to the neutral error contract" `
    --body-file .git\KAN-27-pr-body.md
```

The PR body contains only a professional summary, intentional response changes,
verification counts, risk/rollback notes, and the Jira key. Keep the body file
inside `.git`; never add it to the repository.

- [ ] **Step 7: Update Jira to In Review and wait for review**

After confirming the PR targets `develop` and CI is green, add the PR/testing
evidence to KAN-27 and transition it from **In Progress** to **In Review**.
Do not mark the story Done or merge during this step.

- [ ] **Step 8: Merge only after review approval and re-verify**

After approval, merge the exact reviewed head into `develop`. Fetch remote
state, verify the merge contains that head, confirm required checks remain
successful, and run the appropriate final repository checks. Add concise
completion evidence to KAN-27, transition it to **Done**, and remove the merged
feature branch. `main` remains unchanged.

---

## Execution evidence

- Verified base: `origin/develop` at
  `29d44f591b513adc76deed7133ce36207b616076`.
- Verified implementation head before this documentation-only evidence commit:
  `16c7658f020a9447c8ded2316ba9d9d923d6dc04`.
- Contract RED: compilation failed because the governance catalogue did not
  exist. Contract GREEN: 17/17 descriptor and rule-mapping tests passed.
- Admin recovery RED: compilation failed because the typed recovery exceptions
  did not exist. Combined contract and admin GREEN: 29/29 tests passed.
- Marketplace RED: the legacy assertion expected HTTP 403 while the migrated
  endpoint returned the approved HTTP 422 Problem Details response.
- Architecture: 3/3 rules passed. Exactly five obsolete frozen violations
  belonging to the migrated `GovernanceException` were removed; no waiver or
  automatic store update was enabled.
- Focused verification: 32/32 governance and architecture tests passed; 9/9
  PostgreSQL API tests passed across admin recovery and marketplace.
- Complete unit suite: 226/226 passed with zero failures, errors, or skips.
- Complete PostgreSQL integration suite: 76/76 passed with zero failures,
  errors, or skips; Flyway applied and validated V1 against PostgreSQL 16.
- Disclosure checks confirmed that public responses omit recovery tokens,
  diagnostic codes, internal messages, and legacy response envelopes.
- Governance legacy dependency scans returned no matches.
- Protected configuration, Flyway, Maven, and CI diffs are empty.
- Flyway V1 blob: `8784c468aa169952a87e726303d03abae4376add`,
  identical to `origin/develop`.
- `main` and `origin/main` both remain at
  `bc7727b0b2e09ebbfef8b9c6c5dc729cd4aab4fb`.

## Completion checklist

- [x] Approved grouped descriptors and mapping are implemented exactly.
- [x] Every non-`ALLOWED` governance rule has a tested mapping.
- [x] Recovery denial variants have indistinguishable public contracts.
- [x] No recovery token or configuration value enters public or diagnostic data.
- [x] Missing active authority produces the approved 409 response.
- [x] Existing successful governance/admin behavior remains unchanged.
- [x] Governance production code has no legacy exception dependency.
- [x] Architecture, unit, focused integration, and full integration tests pass.
- [x] Flyway V1 and protected configuration paths are unchanged.
- [ ] PR targets `develop`; `main` is unchanged.
- [ ] Jira status and evidence match the actual delivery stage.
