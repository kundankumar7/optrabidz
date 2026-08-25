# KAN-34 Repayment and Installment Error Migration Implementation Plan

> **Execution prerequisite:** Follow this plan task-by-task. Write each failing
> test before its production change, run the focused verification after every
> task, and stop for review before merging.

**Goal:** Migrate repayment, installment, and repayment-progress failures to
the approved financial-owned error contract without changing successful API,
payment, schedule, notification, audit, or transaction behavior.

**Architecture:** Spring Security continues to supply caller identity while
`FinancialService` selects role policy and the appropriate ownership-scoped
repository query. PostgreSQL conditional updates remain the concurrency
authority, and the shared RFC 9457 adapter renders only allowlisted public
error descriptors.

**Tech stack:** Java 21, Spring Boot 3.3.2, Spring Security, Bean Validation,
Spring Data JPA, PostgreSQL, JUnit 5, AssertJ, Mockito, MockMvc,
Testcontainers, ArchUnit, Maven Surefire, and Maven Failsafe.

**Spec:**
`docs/error-handling/work-items/KAN-34-repayment-error-migration/design.md`

## Global constraints

- Preserve all routes, request parameter names, successful DTOs, success
  statuses, session and CSRF behavior, repayment calculations, schedules,
  payable states, payment rules, outbox, notification, and audit behavior.
- Use exactly the five public errors approved in the KAN-34 specification.
- Reject an ineligible role before resolving a profile or loading a requested
  repayment, installment, or agreement.
- Establish authority through role-specific persistence queries; do not load
  globally and compare ownership afterward.
- Within each endpoint family, missing and non-owned identifiers must produce
  identical public Problem Details.
- Keep unrestricted lookups for administrator reads and trusted internal
  payment, expiry, and overdue flows only.
- Keep initial non-payable state separate from a lost conditional transition:
  `REPAYMENT_INSTALLMENT_NOT_PAYABLE` versus `REPAYMENT_STATE_CONFLICT`.
- A canonical active intent and a same-intent paid installment are idempotent
  success and must not publish duplicate repayment events.
- Protected diagnostics must never enter public Problem Details or expected
  warning logs.
- Unexpected persistence and runtime failures remain on the sanitized generic
  500 path.
- Do not add dependencies, Flyway migrations, optimistic locking, JWT/OAuth2,
  new deployable services, or repayment business-policy redesign.
- Do not remove the shared legacy exception stack; KAN-33 owns final removal.

---

## File structure

### Neutral repayment contract

- Extend `FinancialErrors.java` with four repayment-specific descriptors; the
  existing `FINANCIAL_OPERATION_NOT_ALLOWED` descriptor is reused.
- Rewrite the three repayment exceptions as final `ApplicationException`
  subclasses and add `RepaymentStateConflictException.java`.
- Delete `InvalidRepaymentStateException.java` and
  `FinancialAccessException.java` only after their final callers are removed.
- Add `FinancialRepaymentErrorContractTest.java` and narrow ArchUnit coverage.

### Ownership-scoped persistence

- Extend repayment, installment, and agreement repository ports with
  startup- and investor-scoped identifier lookups.
- Implement the lookups in their Spring Data repositories and adapters.
- Add PostgreSQL integration tests for owned, non-owned, and missing records.

### API request-shape validation

- Add immutable `RepaymentInstallmentQuery.java` in the financial API adapter.
- Bind it with `@Valid @ModelAttribute` in the three installment-list routes.
- Keep accepted filter meaning in `FinancialService`; remove only the legacy
  mutually-exclusive-filter exception construction.

### Application and transition selection

- Refactor only repayment-related paths in `FinancialService.java`.
- Select scoped repositories before state evaluation.
- Classify zero-row installment transitions and return before duplicate event
  publication on same-intent success.
- Preserve public service method signatures and trusted internal helpers.

### Verification

- Extend unit, repository, MockMvc, disclosure, rollback, concurrency, and
  architecture tests.
- Remove only repayment entries from the frozen legacy allowlist.
- Run focused suites first, then the complete unit and integration suites.

---

### Task 1: Define and guard the repayment error contract

**Files:**

- Modify: `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/exception/RepaymentNotFoundException.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/exception/RepaymentInstallmentNotFoundException.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/exception/RepaymentInstallmentNotPayableException.java`
- Create: `src/main/java/com/project/optrabidz/financial/application/exception/RepaymentStateConflictException.java`
- Delete after caller migration: `src/main/java/com/project/optrabidz/financial/application/exception/InvalidRepaymentStateException.java`
- Delete after caller migration: `src/main/java/com/project/optrabidz/financial/application/exception/FinancialAccessException.java`
- Create: `src/test/java/com/project/optrabidz/financial/application/FinancialRepaymentErrorContractTest.java`
- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify when ArchUnit reports obsolete entries: `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f`

**Interfaces:**

- Consumes `ErrorDescriptor`, `ErrorCategory`, and `ApplicationException` from
  `com.project.optrabidz.common.error`.
- Produces `REPAYMENT_NOT_FOUND`, `REPAYMENT_INSTALLMENT_NOT_FOUND`,
  `REPAYMENT_INSTALLMENT_NOT_PAYABLE`, and `REPAYMENT_STATE_CONFLICT`.
- Reuses `FINANCIAL_OPERATION_NOT_ALLOWED` and
  `FinancialOperationNotAllowedException`.

- [x] **Step 1: Write the failing descriptor and exception contract test**

Create a parameterized source containing these exact values:

```java
private static Stream<Arguments> repaymentDescriptors() {
    return Stream.of(
            arguments(FinancialErrors.REPAYMENT_NOT_FOUND,
                    "REPAYMENT_NOT_FOUND", ErrorCategory.NOT_FOUND,
                    "The requested repayment was not found"),
            arguments(FinancialErrors.REPAYMENT_INSTALLMENT_NOT_FOUND,
                    "REPAYMENT_INSTALLMENT_NOT_FOUND", ErrorCategory.NOT_FOUND,
                    "The requested repayment installment was not found"),
            arguments(FinancialErrors.REPAYMENT_INSTALLMENT_NOT_PAYABLE,
                    "REPAYMENT_INSTALLMENT_NOT_PAYABLE", ErrorCategory.CONFLICT,
                    "The repayment installment cannot be paid in its current state"),
            arguments(FinancialErrors.REPAYMENT_STATE_CONFLICT,
                    "REPAYMENT_STATE_CONFLICT", ErrorCategory.CONFLICT,
                    "The repayment state no longer permits this operation")
    );
}
```

Assert `code()`, `category()`, and `publicMessage()`. Construct each exception
with `protected-repayment-sentinel`; assert the sentinel is present in
`getMessage()` and absent from `descriptor().publicMessage()`. Assert these
diagnostic codes:

```text
RepaymentNotFoundException                FINANCIAL.REPAYMENT.NOT.FOUND
RepaymentInstallmentNotFoundException     FINANCIAL.REPAYMENT.INSTALLMENT.NOT.FOUND
RepaymentInstallmentNotPayableException   FINANCIAL.REPAYMENT.INSTALLMENT.NOT.PAYABLE
RepaymentStateConflictException           FINANCIAL.REPAYMENT.STATE.CONFLICT
```

- [x] **Step 2: Run the contract test and verify failure**

```powershell
.\mvnw.cmd -Dtest=FinancialRepaymentErrorContractTest test
```

Expected: compilation fails because the four descriptors and conflict
exception do not exist.

- [x] **Step 3: Add the descriptors and typed exceptions**

Add the exact descriptors from Step 1. Each exception follows this structure,
substituting its descriptor and diagnostic code:

```java
public final class RepaymentNotFoundException extends ApplicationException {
    public RepaymentNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.REPAYMENT_NOT_FOUND,
                "FINANCIAL.REPAYMENT.NOT.FOUND",
                diagnosticMessage
        );
    }
}
```

Do not add constructors that accept public messages, HTTP statuses, or legacy
`ErrorCode` values.

- [x] **Step 4: Add the repayment-specific architecture rule**

```java
@ArchTest
static final ArchRule REPAYMENT_EXCEPTIONS_USE_NEUTRAL_ERROR_CONTRACT =
        noClasses()
                .that().haveNameMatching(
                        ".*\\.(RepaymentNotFound|RepaymentInstallmentNotFound|"
                                + "RepaymentInstallmentNotPayable|"
                                + "RepaymentStateConflict)Exception"
                )
                .should().dependOnClassesThat().resideInAPackage(
                        "..common.api.exception.."
                )
                .as("migrated repayment exceptions must use the neutral error contract");
```

Remove only obsolete frozen violations for the migrated repayment exception
classes. Do not declare the complete financial package migrated until KAN-33.

- [x] **Step 5: Run focused contract and architecture tests**

```powershell
.\mvnw.cmd "-Dtest=FinancialRepaymentErrorContractTest,ExceptionArchitectureTest" test
```

Expected: PASS.

- [x] **Step 6: Commit the contract boundary**

```powershell
git add src/main/java/com/project/optrabidz/financial/application/error `
  src/main/java/com/project/optrabidz/financial/application/exception `
  src/test/java/com/project/optrabidz/financial/application/FinancialRepaymentErrorContractTest.java `
  src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
  src/test/resources/archunit-store
git commit -m "refactor(KAN-34): define neutral repayment errors"
```

---

### Task 2: Add participant-scoped persistence lookups

**Files:**

- Modify: `src/main/java/com/project/optrabidz/financial/domain/repository/RepaymentRepository.java`
- Modify: `src/main/java/com/project/optrabidz/financial/domain/repository/RepaymentInstallmentRepository.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/repository/JpaRepaymentRepository.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/repository/JpaRepaymentInstallmentRepository.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/repository/RepaymentRepositoryAdapter.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/repository/RepaymentInstallmentRepositoryAdapter.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/domain/repository/AgreementRepository.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/infrastructure/repository/JpaAgreementRepository.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/infrastructure/repository/AgreementRepositoryAdapter.java`
- Create: `src/test/java/com/project/optrabidz/financial/infrastructure/repository/RepaymentRepositoryIT.java`
- Create: `src/test/java/com/project/optrabidz/financial/infrastructure/repository/RepaymentInstallmentRepositoryIT.java`
- Create: `src/test/java/com/project/optrabidz/marketplace/infrastructure/repository/AgreementRepositoryIT.java`

**Interfaces:**

```java
Optional<Repayment> findByIdForStartup(Long repaymentId, Long startupId);
Optional<Repayment> findByIdForInvestor(Long repaymentId, Long investorId);
Optional<RepaymentInstallment> findByIdForStartup(Long installmentId, Long startupId);
Optional<RepaymentInstallment> findByIdForInvestor(Long installmentId, Long investorId);
Optional<Agreement> findByIdForStartup(Long agreementId, Long startupId);
Optional<Agreement> findByIdForInvestor(Long agreementId, Long investorId);
```

- [x] **Step 1: Write failing PostgreSQL ownership tests**

Use `PostgresJpaIntegrationTestSupport` and `PostgresTestDataFixture` to create
two agreements with different startup and investor participants. Persist one
repayment and installment for the first agreement. For each of the three
resource types, assert this pattern:

```java
assertThat(repository.findByIdForStartup(resourceId, firstStartupId)).isPresent();
assertThat(repository.findByIdForStartup(resourceId, secondStartupId)).isEmpty();
assertThat(repository.findByIdForInvestor(resourceId, firstInvestorId)).isPresent();
assertThat(repository.findByIdForInvestor(resourceId, secondInvestorId)).isEmpty();
assertThat(repository.findByIdForStartup(Long.MAX_VALUE, firstStartupId)).isEmpty();
```

Also prove the existing `findById(resourceId)` remains available for trusted
administrator and internal paths.

- [x] **Step 2: Run repository tests and verify failure**

```powershell
.\mvnw.cmd -Pintegration-tests `
  -Dtest=FinancialRepaymentErrorContractTest `
  "-Dit.test=RepaymentRepositoryIT,RepaymentInstallmentRepositoryIT,AgreementRepositoryIT" `
  verify
```

Expected: compilation fails because the scoped methods are absent.

- [x] **Step 3: Add repayment and agreement lookups**

Add derived Spring Data methods matching the entity property names:

```java
Optional<Repayment> findByRepaymentIdAndStartupId(Long repaymentId, Long startupId);
Optional<Repayment> findByRepaymentIdAndInvestorId(Long repaymentId, Long investorId);
Optional<Agreement> findByAgreementIdAndStartupId(Long agreementId, Long startupId);
Optional<Agreement> findByAgreementIdAndInvestorId(Long agreementId, Long investorId);
```

Map their results through the existing persistence mappers. Repository
adapters must not throw application exceptions or select role policy.

- [x] **Step 4: Add installment ownership queries**

Use explicit JPQL joins so the installment query enforces ownership in the
database:

```java
@Query("""
        select ri
        from RepaymentInstallment ri
        join Repayment r on r.repaymentId = ri.repaymentId
        where ri.repaymentInstallmentId = :installmentId
          and r.startupId = :startupId
        """)
Optional<RepaymentInstallment> findByIdForStartup(
        @Param("installmentId") Long installmentId,
        @Param("startupId") Long startupId);
```

Add the investor equivalent using `r.investorId`. Map both through
`RepaymentInstallmentRepositoryAdapter`.

- [x] **Step 5: Run the PostgreSQL ownership tests**

Run the command from Step 2 again.

Expected: PASS for owned, non-owned, missing, and trusted-global cases.

- [x] **Step 6: Commit the persistence boundary**

```powershell
git add src/main/java/com/project/optrabidz/financial/domain/repository `
  src/main/java/com/project/optrabidz/financial/infrastructure/repository `
  src/main/java/com/project/optrabidz/marketplace/domain/repository/AgreementRepository.java `
  src/main/java/com/project/optrabidz/marketplace/infrastructure/repository/JpaAgreementRepository.java `
  src/main/java/com/project/optrabidz/marketplace/infrastructure/repository/AgreementRepositoryAdapter.java `
  src/test/java/com/project/optrabidz/financial/infrastructure/repository `
  src/test/java/com/project/optrabidz/marketplace/infrastructure/repository/AgreementRepositoryIT.java
git commit -m "refactor(KAN-34): scope repayment lookups"
```

---

### Task 3: Move installment filter conflict to Bean Validation

**Files:**

- Create: `src/main/java/com/project/optrabidz/financial/api/RepaymentInstallmentQuery.java`
- Modify: `src/main/java/com/project/optrabidz/financial/api/FinancialController.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/FinancialService.java`
- Create: `src/test/java/com/project/optrabidz/financial/api/RepaymentInstallmentQueryTest.java`
- Modify: `src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java`

**Interfaces:**

```java
public record RepaymentInstallmentQuery(
        RepaymentInstallmentState installmentState,
        RepaymentInstallmentPaymentView paymentView,
        int page,
        int size
) {
    @AssertTrue(message = "Use either installmentState or paymentView, not both")
    public boolean isFilterSelectionValid() {
        return installmentState == null || paymentView == null;
    }
}
```

- [x] **Step 1: Write the failing query-model test**

Use the Jakarta validator and assert zero violations for neither filter and
each single filter, then assert one violation with the exact message when both
filters are present:

```java
RepaymentInstallmentQuery query = new RepaymentInstallmentQuery(
        RepaymentInstallmentState.NOT_STARTED,
        RepaymentInstallmentPaymentView.UNPAID,
        1,
        20
);
assertThat(validator.validate(query))
        .singleElement()
        .extracting(ConstraintViolation::getMessage)
        .isEqualTo("Use either installmentState or paymentView, not both");
```

- [x] **Step 2: Add a failing MockMvc validation test**

Call each of the three installment-list endpoint variants with both filters.
Assert HTTP 400, `application/problem+json`, code `VALIDATION_ERROR`, and no
financial service interaction. Keep the existing single-filter success tests.

- [x] **Step 3: Run the focused tests and verify failure**

```powershell
.\mvnw.cmd "-Dtest=RepaymentInstallmentQueryTest,FinancialApiIT" test
```

Expected: the query model is absent and the current conflict is thrown by the
application service through the legacy envelope.

- [x] **Step 4: Bind the immutable query model**

For all three installment-list methods, replace the four individual query
parameters with:

```java
@Valid @ModelAttribute RepaymentInstallmentQuery query
```

Pass `query.installmentState()`, `query.paymentView()`, `query.page()`, and
`query.size()` to the unchanged service signatures. Configure record
component defaults using compact constructor normalization:

```java
public RepaymentInstallmentQuery {
    page = page < 1 ? 1 : page;
    size = size == 0 ? 20 : size;
}
```

Remove the mutual-exclusion branch and legacy `ApiException`/`ErrorCode`
imports from `FinancialService.resolveInstallmentStates`. Keep its accepted
filter-to-state mapping unchanged.

- [x] **Step 5: Run query and API validation tests**

Run the command from Step 3 again.

Expected: PASS with the shared framework `VALIDATION_ERROR` response.

- [x] **Step 6: Commit the API validation boundary**

```powershell
git add src/main/java/com/project/optrabidz/financial/api `
  src/main/java/com/project/optrabidz/financial/application/FinancialService.java `
  src/test/java/com/project/optrabidz/financial/api
git commit -m "refactor(KAN-34): validate repayment filters at API boundary"
```

---

### Task 4: Select repayment authority before resource state

**Files:**

- Modify: `src/main/java/com/project/optrabidz/financial/application/FinancialService.java`
- Modify: `src/test/java/com/project/optrabidz/financial/application/FinancialServiceTest.java`
- Delete after final caller removal: `src/main/java/com/project/optrabidz/financial/application/exception/FinancialAccessException.java`

**Interfaces:**

- Consumes all six scoped methods from Task 2.
- Reuses `FinancialOperationNotAllowedException` for pre-lookup role denial.
- Preserves every public `FinancialService` signature.
- Produces entity-specific uniform 404 results after permitted-role selection.

- [x] **Step 1: Write failing role-before-lookup tests**

For each list route and both payment-intent creation methods, pass an
ineligible role and assert `FinancialOperationNotAllowedException`. Verify no
profile, repayment, installment, agreement, state, or active-intent
repository call occurs.

For identifier reads, assert these selections:

```text
ADMIN     -> unrestricted findById
STARTUP   -> startup profile, then findByIdForStartup
INVESTOR  -> investor profile, then findByIdForInvestor
```

An empty scoped result must throw the relevant typed 404 without calling a
second unrestricted lookup.

- [x] **Step 2: Write failing progress and payment-authority tests**

Prove repayment progress selects the agreement by caller role before querying
the progress projection. Prove only `STARTUP` can create repayment payment
intents, and that both repayment-level and installment-level creation return
their entity-specific 404 for another startup's resource before checking
payable state.

- [x] **Step 3: Run application tests and verify failure**

```powershell
.\mvnw.cmd `
  "-Dtest=FinancialRepaymentErrorContractTest,FinancialServiceTest,ExceptionArchitectureTest" `
  test
```

Expected: failures expose global-load-then-authorize behavior and remaining
`FinancialAccessException` callers.

- [x] **Step 4: Add role-selected lookup helpers**

Use this pattern for repayment reads:

```java
private Repayment getRepaymentForViewer(
        Long accountId,
        RoleType roleType,
        Long repaymentId
) {
    Optional<Repayment> result = switch (roleType) {
        case ADMIN -> repaymentRepository.findById(repaymentId);
        case STARTUP -> repaymentRepository.findByIdForStartup(
                repaymentId, getStartupByAccount(accountId).getStartupId());
        case INVESTOR -> repaymentRepository.findByIdForInvestor(
                repaymentId, getInvestorByAccount(accountId).getInvestorId());
    };
    return result.orElseThrow(() -> repaymentNotFound(repaymentId));
}
```

Add equivalent helpers for installments and progress agreements. Keep
unrestricted `getRepayment`, `getRepaymentInstallment`, and `getAgreement` for
trusted internal flows only. Diagnostic messages may include bounded IDs and
the fixed phrase `unavailable in permitted scope`.

- [x] **Step 5: Apply authority-first selection to public operations**

Use viewer helpers for reads. Use `ensureFinancialRole` before profile or
resource lookup in lists and payment-intent creation. For installment payment
intent creation, load the installment directly through
`findByIdForStartup`; then load its repayment through
`findByIdForStartup` before state or active-intent evaluation.

For repayment-level creation, load the repayment through startup scope, select
the next payable installment, and call a private authorized helper so the
public role check and repayment lookup are not repeated.

Remove `ensureRepaymentVisible`, `ensureAgreementVisible`, and repayment
callers of `ensureRole`. Delete `FinancialAccessException` after `rg` confirms
there are no production callers.

- [x] **Step 6: Run focused contract, service, and architecture tests**

Run the command from Step 3 again.

Expected: PASS with role denial before lookup and neutral scoped 404 behavior.

- [x] **Step 7: Commit authority-first application selection**

```powershell
git add src/main/java/com/project/optrabidz/financial/application `
  src/test/java/com/project/optrabidz/financial/application/FinancialServiceTest.java `
  src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
  src/test/resources/archunit-store
git commit -m "refactor(KAN-34): enforce repayment ownership in queries"
```

---

### Task 5: Classify installment transition races and idempotency

**Files:**

- Modify: `src/main/java/com/project/optrabidz/financial/application/FinancialService.java`
- Delete: `src/main/java/com/project/optrabidz/financial/application/exception/InvalidRepaymentStateException.java`
- Modify: `src/test/java/com/project/optrabidz/financial/application/FinancialServiceTest.java`
- Modify: `src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java`

**Interfaces:**

- `markPaymentInProgress == 1` means the transaction owns the transition.
- `markPaid == 1` means first successful confirmation.
- A zero-row result is reloaded and classified, never silently ignored.
- Same canonical intent or same-intent paid state is idempotent success.
- An incompatible state throws `RepaymentStateConflictException` inside the
  existing transaction.

- [x] **Step 1: Write failing creation-race tests**

For payment-intent creation, stub `markPaymentInProgress` to return zero.
Assert:

```text
latest PAYMENT_IN_PROGRESS + canonical active intent -> same intent response
latest PAID/PAYMENT_FAILED/CANCELLED                -> REPAYMENT_STATE_CONFLICT
missing latest installment                          -> installment 404
```

For conflict outcomes, verify no repayment refresh or downstream event
publication. Preserve initial non-payable tests for states observed before an
intent is created; they must use `REPAYMENT_INSTALLMENT_NOT_PAYABLE`.

- [x] **Step 2: Write failing confirmation idempotency tests**

When `markPaid` returns zero, reload the installment:

```text
PAID + same confirmedPaymentIntentId       -> return before refresh/event
PAID + different confirmedPaymentIntentId  -> REPAYMENT_STATE_CONFLICT
PAYMENT_FAILED/OVERDUE/CANCELLED            -> REPAYMENT_STATE_CONFLICT
missing                                    -> installment 404
```

Assert exactly one `RepaymentInstallmentPaidEvent` for the first successful
transition and zero for same-intent replay. Assert a conflict propagates so
the transaction can roll back payment state.

- [x] **Step 3: Run focused tests and verify failure**

```powershell
.\mvnw.cmd "-Dtest=FinancialRepaymentErrorContractTest,FinancialServiceTest" test
```

Expected: current code ignores a zero-row payment-in-progress update and
continues to publish after a same-intent zero-row paid result.

- [x] **Step 4: Implement deterministic zero-row classification**

After `markPaymentInProgress`, branch on the row count. A zero-row branch must
reload the installment and the canonical active intent. Return the canonical
intent only when the latest state is `PAYMENT_IN_PROGRESS`; otherwise throw
`RepaymentStateConflictException`.

For confirmation, make same-intent classification explicit:

```java
private boolean alreadyPaidBySameIntent(Long installmentId, Long paymentIntentId) {
    RepaymentInstallment latest = getRepaymentInstallment(installmentId);
    if (latest.getInstallmentState() == RepaymentInstallmentState.PAID
            && paymentIntentId.equals(latest.getConfirmedPaymentIntentId())) {
        return true;
    }
    throw new RepaymentStateConflictException(
            "Repayment installment conditional transition rejected"
    );
}
```

When it returns `true`, return immediately from business confirmation before
repayment refresh and event publication.

- [x] **Step 5: Remove legacy transition translation**

Remove unused `applyRepaymentTransition` and delete
`InvalidRepaymentStateException`. Do not catch `IllegalStateException` around
repository calls. Unexpected failures must propagate to the generic sanitized
500 adapter.

- [x] **Step 6: Run service and existing concurrency scenarios**

```powershell
.\mvnw.cmd "-Dtest=FinancialRepaymentErrorContractTest,FinancialServiceTest" test
.\mvnw.cmd -Pintegration-tests -DskipUnitTests `
  "-Dit.test=FinancialApiIT" verify
```

Expected: PASS, including canonical active-intent creation and same-intent
confirmation without duplicate effects.

- [x] **Step 7: Commit transition classification**

```powershell
git add src/main/java/com/project/optrabidz/financial/application `
  src/test/java/com/project/optrabidz/financial/application/FinancialServiceTest.java `
  src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java
git commit -m "fix(KAN-34): classify repayment transition races"
```

---

### Task 6: Lock the HTTP disclosure and rollback contract

**Files:**

- Modify: `src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java`
- Modify: `src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java`
- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify when required: `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f`

**Interfaces:**

- Shared Problem Details fields: `status`, `code`, `title`, `detail`, and
  `instance`/request metadata already defined by the shared adapter.
- Missing and non-owned comparisons ignore only request-specific instance
  values; status, code, title, detail, content type, and shape must match.
- Session authentication and CSRF remain unchanged.

- [x] **Step 1: Add exact Problem Details tests**

Cover all five descriptors with their approved HTTP mappings:

```text
FINANCIAL_OPERATION_NOT_ALLOWED          403
REPAYMENT_NOT_FOUND                      404
REPAYMENT_INSTALLMENT_NOT_FOUND          404
REPAYMENT_INSTALLMENT_NOT_PAYABLE        409
REPAYMENT_STATE_CONFLICT                 409
```

Assert `application/problem+json`, the exact public detail, and absence of
`protected-repayment-sentinel`, class names, SQL fragments, and arbitrary
cause messages.

- [x] **Step 2: Add disclosure-equivalence tests**

For repayment, repayment installments, one installment, repayment progress,
and both payment-intent creation endpoint families, call one missing ID and
one other-participant ID. Parse both responses and assert equal public status,
code, title, detail, content type, and JSON field set.

- [x] **Step 3: Add rollback and side-effect assertions**

Force an incompatible zero-row `markPaid` outcome after payment attempt and
intent transitions. Assert HTTP 409 and verify database state retains the
pre-request payment, installment, and repayment values. Assert no new
repayment outbox, notification, or audit row and no duplicate paid event.

- [x] **Step 4: Preserve the security boundary**

Retain tests proving unauthenticated requests are rejected by Spring Security
and unsafe session-authenticated methods require CSRF. Do not assert JWT or
OAuth2 behavior in this story.

- [x] **Step 5: Run HTTP, security, architecture, and PostgreSQL tests**

```powershell
.\mvnw.cmd `
  "-Dtest=FinancialRepaymentErrorContractTest,FinancialServiceTest,RepaymentInstallmentQueryTest,ExceptionArchitectureTest" `
  test
.\mvnw.cmd -Pintegration-tests -DskipUnitTests `
  "-Dit.test=FinancialApiIT,FinancialSecurityApiIT,RepaymentRepositoryIT,RepaymentInstallmentRepositoryIT,AgreementRepositoryIT" `
  verify
```

Expected: PASS with neutral disclosure, rollback, session-security, and scoped
PostgreSQL behavior.

- [x] **Step 6: Commit the boundary verification**

```powershell
git add src/test/java/com/project/optrabidz/financial `
  src/test/java/com/project/optrabidz/security/api/FinancialSecurityApiIT.java `
  src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
  src/test/resources/archunit-store
git commit -m "test(KAN-34): verify repayment error boundaries"
```

---

### Task 7: Complete regression verification and review handoff

**Files:**

- Modify: `docs/error-handling/work-items/KAN-34-repayment-error-migration/design.md`
- Modify: `docs/error-handling/work-items/KAN-34-repayment-error-migration/implementation-plan.md`

- [x] **Step 1: Prove legacy repayment dependencies are gone**

```powershell
rg -n "ApiException|ErrorCode|FinancialAccessException|InvalidRepaymentStateException" `
  src/main/java/com/project/optrabidz/financial
```

Expected: no repayment production dependency remains. Any non-repayment
legacy result must correspond to KAN-33 scope and must not be removed here.

- [x] **Step 2: Run the complete unit suite**

```powershell
.\mvnw.cmd test
```

Expected: BUILD SUCCESS.

- [x] **Step 3: Run the complete integration suite**

```powershell
.\mvnw.cmd -Pintegration-tests verify
```

Expected: BUILD SUCCESS with Docker/Testcontainers available.

- [x] **Step 4: Validate documentation and patch hygiene**

```powershell
.\mvnw.cmd -Dtest=DocumentationLinksTest test
git diff --check
git status --short
```

Expected: documentation test passes, `git diff --check` emits no errors, and
only intentional KAN-34 files are present.

- [x] **Step 5: Update completion evidence**

Change the design status to `Implemented; awaiting review`. Check acceptance
criteria only when supported by the executed tests. Record focused and full
verification commands and results in the pull-request description and Jira;
do not claim an unexecuted check passed.

- [x] **Step 6: Commit documentation evidence**

```powershell
git add docs/error-handling/work-items/KAN-34-repayment-error-migration
git commit -m "docs(KAN-34): record repayment migration verification"
```

- [ ] **Step 7: Push and open the review gate**

```powershell
git push -u origin feature/KAN-34-repayment-error-migration
gh pr create --base develop `
  --head feature/KAN-34-repayment-error-migration `
  --title "KAN-34: migrate repayment and installment errors" `
  --body-file .github/pull_request_body.md
```

The PR targets `develop`, never `main`. Do not merge it until review approval
is explicitly recorded.

## Implementation approval gate

No production-code task starts until this implementation plan is reviewed and
approved. After approval, execution proceeds inline in the current checkout
with test-first checkpoints and Jira updates after each meaningful stage.
