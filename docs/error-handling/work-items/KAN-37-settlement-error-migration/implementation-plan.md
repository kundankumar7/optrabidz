# KAN-37 Settlement Error Migration Implementation Plan

> **Execution prerequisite:** Follow this plan task-by-task with tests written
> before production changes and a verification checkpoint before each commit.

**Goal:** Migrate settlement failures to the approved financial-owned neutral
contract while preserving settlement, payment, repayment, outbox,
notification, audit, and success behavior.

**Architecture:** Keep authorization and orchestration in `FinancialService`,
enforce participant ownership through role-specific settlement repository
queries, and preserve the atomic pending-to-confirmed PostgreSQL update. The
shared RFC 9457 adapter renders the four allowlisted errors, while the existing
transaction rolls back payment and downstream effects on settlement conflict.

**Tech Stack:** Java 21, Spring Boot 3.3.2, Spring Security, Spring Data JPA,
PostgreSQL, Flyway, JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers,
ArchUnit, Maven Surefire, and Maven Failsafe.

**Spec:**
`docs/error-handling/work-items/KAN-37-settlement-error-migration/design.md`

## Global constraints

- Preserve existing routes, request bodies, success DTOs, success statuses,
  session and CSRF behavior, payment rules, conditional updates,
  transactions, repayment calculations, outbox, notification, and audit
  behavior.
- Use exactly the four codes, categories, and public details approved in the
  KAN-37 specification.
- Reject a role that cannot perform the operation before settlement lookup.
- Establish resource authority through a scoped query before evaluating
  settlement state or expiry.
- Missing and non-owned settlements must be publicly indistinguishable.
- Keep unrestricted settlement lookup only for administrator reads and
  trusted internal confirmation reached through an authorized payment intent.
- `SETTLEMENT_NOT_PAYABLE` is an initial-state error;
  `SETTLEMENT_STATE_CONFLICT` is a conditional-transition conflict.
- Same-payment-intent confirmation remains idempotent without duplicate
  repayment or event effects.
- A competing settlement transition rolls back payment attempt and intent
  changes and commits no repayment, outbox, notification, or audit effect.
- Public responses use only allowlisted descriptor text. Diagnostic text,
  provider data, domain messages, SQL, causes, and stack traces must not enter
  Problem Details.
- Unexpected runtime and persistence failures remain on the generic sanitized
  500 path.
- Do not add dependencies, Flyway migrations, optimistic locking, JWT/OAuth2,
  service extraction, or business-policy redesign.
- Do not migrate repayment exceptions or declare the complete financial
  module migrated in this story.

---

## File structure

### Neutral settlement contract

- Modify
  `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java`
  to own the four approved descriptors.
- Rewrite `SettlementNotFoundException.java` and
  `SettlementNotPayableException.java` as `ApplicationException` subclasses.
- Create `SettlementStateConflictException.java` and
  `FinancialOperationNotAllowedException.java`.
- Delete `InvalidSettlementStateException.java` after its unused helper is
  removed.
- Create `FinancialSettlementErrorContractTest.java` to lock descriptor and
  protected-diagnostic contracts.

### Scoped persistence

- Modify `SettlementRepository.java`, `JpaSettlementRepository.java`, and
  `SettlementRepositoryAdapter.java` with startup- and investor-scoped single
  resource lookups.
- Create `SettlementRepositoryIT.java` for PostgreSQL-backed ownership tests.
- Preserve global lookup, page queries, expiry, and conditional confirmation.

### Application selection

- Modify only settlement-related paths in `FinancialService.java`.
- Update `FinancialServiceTest.java` for role-before-lookup, scoped selection,
  initial state, conditional conflict, and idempotency.
- Leave repayment selection and `FinancialAccessException` callers unchanged
  for KAN-34.

### HTTP, rollback, and architecture verification

- Update `FinancialApiIT.java` for exact Problem Details, equal missing and
  non-owned bodies, role denial, state conflict, rollback, and regression.
- Update `ExceptionArchitectureTest.java` and its frozen violation store only
  for the migrated settlement exceptions.
- Preserve the existing payment concurrency, expiry, notification, and audit
  integration scenarios.

---

### Task 1: Define and guard the neutral settlement error contract

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/SettlementNotFoundException.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/SettlementNotPayableException.java`
- Create:
  `src/main/java/com/project/optrabidz/financial/application/exception/SettlementStateConflictException.java`
- Create:
  `src/main/java/com/project/optrabidz/financial/application/exception/FinancialOperationNotAllowedException.java`
- Delete after caller removal:
  `src/main/java/com/project/optrabidz/financial/application/exception/InvalidSettlementStateException.java`
- Create:
  `src/test/java/com/project/optrabidz/financial/application/FinancialSettlementErrorContractTest.java`
- Modify:
  `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify when ArchUnit reports obsolete frozen entries:
  `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f`

**Interfaces:**

- Consumes `ErrorDescriptor`, `ErrorCategory`, and `ApplicationException` from
  `com.project.optrabidz.common.error`.
- Produces four constants on `FinancialErrors`.
- Produces typed exception constructors accepting protected
  `String diagnosticMessage`.
- `FinancialOperationNotAllowedException` is used only before a settlement
  resource lookup in KAN-37 paths.

- [ ] **Step 1: Write the failing descriptor and exception contract test**

Create a parameterized descriptor test with exactly this source:

```java
private static Stream<Arguments> descriptors() {
    return Stream.of(
            arguments(FinancialErrors.FINANCIAL_OPERATION_NOT_ALLOWED,
                    "FINANCIAL_OPERATION_NOT_ALLOWED",
                    ErrorCategory.AUTHORIZATION,
                    "This financial operation is not allowed"),
            arguments(FinancialErrors.SETTLEMENT_NOT_FOUND,
                    "SETTLEMENT_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested settlement was not found"),
            arguments(FinancialErrors.SETTLEMENT_NOT_PAYABLE,
                    "SETTLEMENT_NOT_PAYABLE",
                    ErrorCategory.CONFLICT,
                    "The settlement cannot be paid in its current state"),
            arguments(FinancialErrors.SETTLEMENT_STATE_CONFLICT,
                    "SETTLEMENT_STATE_CONFLICT",
                    ErrorCategory.CONFLICT,
                    "The settlement state no longer permits this operation")
    );
}
```

Assert `code()`, `category()`, and `publicMessage()`. Add exception factories
and assert these exact diagnostic codes:

```text
FinancialOperationNotAllowedException  FINANCIAL.OPERATION.NOT.ALLOWED
SettlementNotFoundException            FINANCIAL.SETTLEMENT.NOT.FOUND
SettlementNotPayableException          FINANCIAL.SETTLEMENT.NOT.PAYABLE
SettlementStateConflictException       FINANCIAL.SETTLEMENT.STATE.CONFLICT
```

Pass `protected-settlement-sentinel` to every factory. Assert it is present in
`getMessage()` and absent from `descriptor().publicMessage()`.

- [ ] **Step 2: Run the contract test and verify failure**

Run:

```powershell
.\mvnw.cmd -Dtest=FinancialSettlementErrorContractTest test
```

Expected: compilation fails because the descriptors and new exception classes
do not exist.

- [ ] **Step 3: Add the exact descriptors and exceptions**

Add the four constants exactly as shown in Step 1. Use this shape for each
exception, substituting the approved descriptor and diagnostic code:

```java
public final class SettlementNotFoundException extends ApplicationException {
    public SettlementNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.SETTLEMENT_NOT_FOUND,
                "FINANCIAL.SETTLEMENT.NOT.FOUND",
                diagnosticMessage
        );
    }
}
```

`SettlementStateConflictException` needs only the diagnostic-message
constructor because the conflict is classified from trusted reloaded state,
not translated from arbitrary caught exception text.

- [ ] **Step 4: Add the settlement-specific architecture rule**

Add this narrow rule:

```java
@ArchTest
static final ArchRule SETTLEMENT_EXCEPTIONS_USE_NEUTRAL_ERROR_CONTRACT =
        noClasses()
                .that().haveNameMatching(
                        ".*\\.(SettlementNotFound|SettlementNotPayable|"
                                + "SettlementStateConflict|"
                                + "FinancialOperationNotAllowed)Exception"
                )
                .should().dependOnClassesThat().resideInAPackage(
                        "..common.api.exception.."
                )
                .as("migrated settlement exceptions must use the neutral error contract");
```

Do not add `..financial..` to the complete migrated-module rule. Remove only
the frozen legacy entries for `SettlementNotFoundException` and
`SettlementNotPayableException`. Keep repayment and general financial legacy
entries frozen.

- [ ] **Step 5: Run contract and architecture tests**

Run:

```powershell
.\mvnw.cmd "-Dtest=FinancialSettlementErrorContractTest,ExceptionArchitectureTest" test
```

Expected: PASS.

- [ ] **Step 6: Commit the neutral contract**

```powershell
git add src/main/java/com/project/optrabidz/financial/application/error `
  src/main/java/com/project/optrabidz/financial/application/exception `
  src/test/java/com/project/optrabidz/financial/application/FinancialSettlementErrorContractTest.java `
  src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
  src/test/resources/archunit-store
git commit -m "refactor(KAN-37): define neutral settlement errors"
```

---

### Task 2: Add role-specific settlement repository lookups

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/financial/domain/repository/SettlementRepository.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/infrastructure/repository/JpaSettlementRepository.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/infrastructure/repository/SettlementRepositoryAdapter.java`
- Create:
  `src/test/java/com/project/optrabidz/financial/infrastructure/repository/SettlementRepositoryIT.java`

**Interfaces:**

- Produces
  `Optional<Settlement> findByIdForStartup(Long settlementId, Long startupId)`.
- Produces
  `Optional<Settlement> findByIdForInvestor(Long settlementId, Long investorId)`.
- Preserves `findById`, `findByAgreementId`, both page queries,
  `confirmPending`, and `expireExpiredPending` unchanged.

- [ ] **Step 1: Write failing PostgreSQL scope tests**

Create `SettlementRepositoryIT` extending
`PostgresJpaIntegrationTestSupport`, import `FinancialPersistenceMapper` and
`SettlementRepositoryAdapter`, and use `PostgresTestDataFixture` to create two
agreements. Save one pending settlement for the first agreement and assert:

```java
assertThat(repository.findByIdForStartup(
        settlementId, firstAgreement.startupId())).isPresent();
assertThat(repository.findByIdForStartup(
        settlementId, secondAgreement.startupId())).isEmpty();
assertThat(repository.findByIdForInvestor(
        settlementId, firstAgreement.investorId())).isPresent();
assertThat(repository.findByIdForInvestor(
        settlementId, secondAgreement.investorId())).isEmpty();
```

Also assert both scoped methods return empty for `Long.MAX_VALUE`. Keep the
existing global lookup assertion to prove the trusted path remains available.

- [ ] **Step 2: Run the repository test and verify failure**

Run:

```powershell
.\mvnw.cmd -Pintegration-tests `
  -Dtest=FinancialSettlementErrorContractTest `
  -Dit.test=SettlementRepositoryIT verify
```

Expected: compilation fails because the two scoped methods do not exist.

- [ ] **Step 3: Add scoped port and JPA methods**

Add the exact domain-port signatures from the Interfaces section. Add Spring
Data methods whose property names match the settlement entity:

```java
Optional<Settlement> findBySettlementIdAndStartupId(
        Long settlementId,
        Long startupId);

Optional<Settlement> findBySettlementIdAndInvestorId(
        Long settlementId,
        Long investorId);
```

Map each through `SettlementRepositoryAdapter`:

```java
@Override
public Optional<Settlement> findByIdForStartup(
        Long settlementId,
        Long startupId
) {
    return jpaSettlementRepository
            .findBySettlementIdAndStartupId(settlementId, startupId)
            .map(mapper::toDomain);
}
```

Repeat with the investor method. Do not add policy or exception translation
to the adapter.

- [ ] **Step 4: Run the repository integration test**

Run the command from Step 2 again.

Expected: PASS for owned, non-owned, missing, and global lookup cases against
PostgreSQL.

- [ ] **Step 5: Commit the scoped persistence boundary**

```powershell
git add src/main/java/com/project/optrabidz/financial/domain/repository/SettlementRepository.java `
  src/main/java/com/project/optrabidz/financial/infrastructure/repository/JpaSettlementRepository.java `
  src/main/java/com/project/optrabidz/financial/infrastructure/repository/SettlementRepositoryAdapter.java `
  src/test/java/com/project/optrabidz/financial/infrastructure/repository/SettlementRepositoryIT.java
git commit -m "refactor(KAN-37): scope settlement lookups"
```

---

### Task 3: Migrate settlement authorization and state selection

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/financial/application/FinancialService.java`
- Delete:
  `src/main/java/com/project/optrabidz/financial/application/exception/InvalidSettlementStateException.java`
- Modify:
  `src/test/java/com/project/optrabidz/financial/application/FinancialServiceTest.java`

**Interfaces:**

- Consumes the four typed exceptions from Task 1.
- Consumes both scoped repository methods from Task 2.
- Preserves every public `FinancialService` signature.
- Produces authority-first settlement reads and payment-intent creation.
- Preserves global trusted settlement lookup for internal confirmation.

- [ ] **Step 1: Write failing role and scoped-lookup service tests**

Add tests proving:

```text
getSettlement ADMIN     -> repository.findById
getSettlement STARTUP   -> startup profile, then findByIdForStartup
getSettlement INVESTOR  -> investor profile, then findByIdForInvestor
create intent INVESTOR  -> investor profile, then findByIdForInvestor
create intent STARTUP   -> FINANCIAL_OPERATION_NOT_ALLOWED before any profile
                           or settlement repository call
```

For missing and non-owned scoped results, assert `SettlementNotFoundException`
and the `SETTLEMENT_NOT_FOUND` descriptor. Verify state and active-intent
repositories are never called after lookup failure.

For an investor-owned settlement, preserve the existing active-intent reuse
and create-new behavior.

- [ ] **Step 2: Write failing initial-state and transition-conflict tests**

For payment-intent creation, test a non-pending settlement and an expired
pending settlement. Both must throw `SettlementNotPayableException` with
descriptor `SETTLEMENT_NOT_PAYABLE`.

For payment confirmation, stub `confirmPending` to return zero, then reload:

```text
CONFIRMED + same paymentIntentId       -> idempotent success
CONFIRMED + different paymentIntentId  -> SettlementStateConflictException
EXPIRED                                -> SettlementStateConflictException
FAILED                                 -> SettlementStateConflictException
CANCELLED                              -> SettlementStateConflictException
missing                                -> SettlementNotFoundException
```

For every conflict, verify no repayment save and no
`SettlementConfirmedEvent` publication. For same-intent replay, verify no
duplicate repayment save or event publication.

Stub the repository to throw an unexpected runtime or data-access exception
from a settlement lookup and assert the service does not translate it into an
expected settlement descriptor. The shared adapter's existing generic-500
tests remain responsible for sanitizing that unclassified failure.

- [ ] **Step 3: Run the application tests and verify failure**

Run:

```powershell
.\mvnw.cmd `
  "-Dtest=FinancialSettlementErrorContractTest,FinancialServiceTest,ExceptionArchitectureTest" `
  test
```

Expected: failures show global load-then-authorize behavior, legacy access
errors, and `SettlementNotPayableException` for conditional conflicts.

- [ ] **Step 4: Implement authority-first settlement selection**

Replace `getSettlement` plus `ensureSettlementVisible` with one role-selected
helper:

```java
private Settlement getVisibleSettlement(
        Long accountId,
        RoleType roleType,
        Long settlementId
) {
    Optional<Settlement> result = switch (roleType) {
        case ADMIN -> settlementRepository.findById(settlementId);
        case STARTUP -> settlementRepository.findByIdForStartup(
                settlementId,
                getStartupByAccount(accountId).getStartupId()
        );
        case INVESTOR -> settlementRepository.findByIdForInvestor(
                settlementId,
                getInvestorByAccount(accountId).getInvestorId()
        );
    };
    return result.orElseThrow(() -> new SettlementNotFoundException(
            "Settlement unavailable for authorized participant lookup"
    ));
}
```

Use it for the public single-settlement read. For settlement payment-intent
creation, check `roleType == INVESTOR` first and otherwise throw
`FinancialOperationNotAllowedException`. Resolve the investor profile, call
`findByIdForInvestor`, and return the uniform settlement 404 when empty.
Remove the manual investor-ID comparison and its `FinancialAccessException`.

Change the settlement list methods to use
`FinancialOperationNotAllowedException` for their pre-lookup role denial.
Do not change `ensureRole` because repayment paths still use it for KAN-34.

- [ ] **Step 5: Implement deterministic settlement-state selection**

Retain `ensureSettlementPayable`, but make both branches throw the neutral
`SettlementNotPayableException` with fixed diagnostic messages. Do not expose
the state or expiry value publicly.

Replace the settlement overload of
`ensureAlreadyConfirmedBySameIntent` with:

```java
private void ensureSettlementConfirmationOutcome(
        Settlement original,
        Long paymentIntentId
) {
    Settlement latest = getSettlement(original.getSettlementId());
    if (latest.getSettlementState() == SettlementState.SETTLEMENT_CONFIRMED
            && paymentIntentId.equals(latest.getConfirmedPaymentIntentId())) {
        return;
    }
    throw new SettlementStateConflictException(
            "Settlement conditional confirmation rejected for current state"
    );
}
```

Call this helper only when `confirmPending` returns zero. Keep repayment
creation and `SettlementConfirmedEvent` publication only after a successful
one-row update. Preserve the global `getSettlement(Long)` helper for this
trusted internal reload.

- [ ] **Step 6: Remove the dead raw-message transition path**

Delete the unused `applySettlementTransition(Runnable)` method, its import,
and `InvalidSettlementStateException.java`. Do not modify the repayment or
payment transition helpers.

- [ ] **Step 7: Run service, contract, and architecture tests**

Run the command from Step 3 again.

Expected: PASS. Then run:

```powershell
rg -n "InvalidSettlementStateException|SettlementNotFoundException extends ApiException|SettlementNotPayableException extends ApiException|ensureSettlementVisible|Investor can pay only own settlement" src/main/java
```

Expected: no matches.

- [ ] **Step 8: Commit the application migration**

```powershell
git add src/main/java/com/project/optrabidz/financial/application `
  src/test/java/com/project/optrabidz/financial/application/FinancialServiceTest.java
git commit -m "refactor(KAN-37): migrate settlement error selection"
```

---

### Task 4: Verify Problem Details, rollback, and regressions

**Files:**

- Modify:
  `src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java`
- Verify unchanged behavior in:
  `src/test/java/com/project/optrabidz/notification/api/NotificationApiIT.java`
- Verify unchanged behavior in:
  `src/test/java/com/project/optrabidz/financial/infrastructure/repository/FinancialExpiryRepositoryIT.java`

**Interfaces:**

- Consumes unchanged settlement and local payment routes.
- Consumes the shared RFC 9457 `RestExceptionHandler`.
- Produces integration evidence for disclosure equivalence, authorization
  precedence, state conflict, transactional rollback, and idempotency.

- [ ] **Step 1: Add a reusable settlement Problem Details assertion**

Add or generalize a helper in `FinancialApiIT` that asserts:

```java
private ResultMatcher[] settlementProblem(
        int status, String title, String code, String detail, String requestId) {
    return new ResultMatcher[] {
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
            jsonPath("$.title").value(title),
            jsonPath("$.status").value(status),
            jsonPath("$.detail").value(detail),
            jsonPath("$.instance").value("urn:optrabidz:request:" + requestId),
            jsonPath("$.code").value(code),
            jsonPath("$.requestId").value(requestId),
            jsonPath("$.timestamp").isString(),
            jsonPath("$.diagnosticCode").doesNotExist(),
            jsonPath("$.exception").doesNotExist(),
            jsonPath("$.stackTrace").doesNotExist()
    };
}
```

Reuse the existing generic helper instead if its exact assertions already
match. Do not duplicate equivalent helpers.

- [ ] **Step 2: Write missing-versus-non-owned API tests**

For both settlement read and settlement payment-intent creation, send one
missing ID and one existing ID owned by another participant. Use different
`X-Request-ID` values, remove `requestId`, `timestamp`, and `instance` from
parsed JSON trees, and assert the remaining bodies are equal.

All four responses must be 404 with:

```text
title  = Resource not found
code   = SETTLEMENT_NOT_FOUND
detail = The requested settlement was not found
```

Assert no settlement state, expiry, investor/startup ID, amount, agreement ID,
or protected diagnostic sentinel appears in either body.

Add successful reads for the owning startup, owning investor, and an
administrator. The administrator request must prove the existing global-read
behavior remains available without granting administrator payment authority.

- [ ] **Step 3: Write role-before-lookup and initial-state API tests**

Replace only the migrated assertions in
`financeEndpointsRejectWrongActorsAndRoles`. A startup attempting to create a
settlement payment intent must receive:

```text
HTTP   = 403
title  = Access denied
code   = FINANCIAL_OPERATION_NOT_ALLOWED
detail = This financial operation is not allowed
```

Use both a real settlement ID and `Long.MAX_VALUE`; after normalizing request
metadata, the bodies must be equal. This proves role denial precedes lookup.

For an owning investor, update a settlement to an initial terminal or expired
state before payment-intent creation and assert `SETTLEMENT_NOT_PAYABLE` 409
with its exact allowlisted detail.

- [ ] **Step 4: Write the forced conditional-conflict rollback test**

Create a pending settlement, payment intent, and local attempt. Before local
confirmation, execute a direct test-only SQL update:

```sql
update settlement
set settlement_state = 'SETTLEMENT_CANCELLED',
    cancelled_at = current_timestamp
where settlement_id = ?
  and settlement_state = 'SETTLEMENT_PENDING'
```

Confirm the attempt through the real HTTP endpoint. Assert 409 with:

```text
code   = SETTLEMENT_STATE_CONFLICT
detail = The settlement state no longer permits this operation
```

Query PostgreSQL after the request and assert:

```text
payment_attempt.attempt_state remains the pre-confirm active state
payment_intent.payment_state remains PAYMENT_PENDING
settlement remains SETTLEMENT_CANCELLED
no repayment exists for the agreement
no SettlementConfirmedEvent row exists in event_outbox for the settlement
no settlement-confirmed notification exists
no SETTLEMENT_CONFIRMED audit record exists
```

Use these exact query shapes with the IDs created by the test:

```sql
select attempt_state::text
from payment_attempt
where payment_attempt_id = ?;

select payment_state::text
from payment_intent
where payment_intent_id = ?;

select count(*)
from repayment
where agreement_id = ?;

select count(*)
from event_outbox
where event_type = 'SettlementConfirmedEvent'
  and payload ->> 'settlementId' = ?;

select count(*)
from notification
where event_type = 'SettlementConfirmedEvent'
  and entity_type = 'SETTLEMENT'
  and entity_id = ?;

select count(*)
from audit_record
where event_type = 'SettlementConfirmedEvent'
  and action = 'SETTLEMENT_CONFIRMED'
  and object_type = 'SETTLEMENT'
  and object_id = ?;
```

Convert numeric IDs to strings for JSON/text predicates. This test proves the
real Spring transaction rolls back payment changes and joined effects after
the settlement update loses its condition.

- [ ] **Step 5: Preserve same-intent idempotency and success effects**

Keep `concurrentLocalSettlementConfirmationCreatesRepaymentOnlyOnce` and add
database assertions that one repayment and one settlement-confirmed outbox
event exist. Both concurrent confirm responses remain 200. Do not loosen the
existing payment confirm-versus-fail assertions.

Run `NotificationApiIT` so the successful settlement event still produces
the expected notification and audit behavior. The rollback scenario must not
dispatch anything because no outbox event is committed.

- [ ] **Step 6: Run focused unit and integration verification**

Run:

```powershell
.\mvnw.cmd -Pintegration-tests `
  "-Dtest=FinancialSettlementErrorContractTest,FinancialServiceTest,ExceptionArchitectureTest" `
  "-Dit.test=SettlementRepositoryIT,FinancialExpiryRepositoryIT,FinancialApiIT,NotificationApiIT" `
  verify
```

Expected: PASS with no skipped named tests and no leaked sentinel values.

- [ ] **Step 7: Run the complete repository verification**

Confirm Docker is running, then run:

```powershell
docker info
.\mvnw.cmd verify -Pintegration-tests
```

Expected: Docker reports a running engine; every unit, architecture, and
Testcontainers integration test passes.

- [ ] **Step 8: Commit the API and rollback evidence**

```powershell
git add src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java
git commit -m "test(KAN-37): verify settlement error contracts"
```

---

### Task 5: Record verification evidence and prepare review

**Files:**

- Modify:
  `docs/error-handling/work-items/KAN-37-settlement-error-migration/design.md`
- Modify:
  `docs/error-handling/work-items/KAN-37-settlement-error-migration/implementation-plan.md`
- Modify:
  `docs/error-handling/README.md`

**Interfaces:**

- Consumes exact commands and results from Task 4.
- Produces a reviewable evidence record and pull request; it does not change
  runtime behavior.

- [ ] **Step 1: Inspect the complete branch diff**

Run:

```powershell
git status --short
git diff --check develop...HEAD
git diff --stat develop...HEAD
git log --oneline develop..HEAD
```

Expected: no whitespace errors, no unrelated files, and only KAN-37 commits.

- [ ] **Step 2: Verify legacy settlement dependencies are absent**

Run:

```powershell
rg -n "extends ApiException|ErrorCode|InvalidSettlementStateException" `
  src/main/java/com/project/optrabidz/financial/application/exception `
  -g "Settlement*Exception.java" `
  -g "FinancialOperationNotAllowedException.java" `
  -g "InvalidSettlementStateException.java"
```

Expected: no matches. Do not require repayment or general
`FinancialAccessException` paths to pass this search.

- [ ] **Step 3: Record exact verification evidence**

Only after the complete suite passes:

- change the design status to `Implemented and verified`;
- check only acceptance criteria supported by evidence;
- append the tested commit hash, exact focused/full commands, test totals,
  Docker version, and PostgreSQL Testcontainers version to this plan; and
- keep any unsupported acceptance item unchecked and explain the gap.

- [ ] **Step 4: Commit and publish the evidence**

```powershell
git add docs/error-handling
git commit -m "docs(KAN-37): record verification evidence"
git push origin feature/KAN-37-settlement-error-migration
```

- [ ] **Step 5: Prepare the pull request without merging**

Create a pull request from `feature/KAN-37-settlement-error-migration` into
`develop`. Summarize the neutral settlement contract, scoped ownership,
role-before-lookup rule, state/conflict distinction, rollback guarantee, exact
tests run, and unchanged repayment scope.

Move KAN-37 to the review status and add the PR link plus verification summary
to Jira. Do not merge until the pull request has been reviewed and approved.
