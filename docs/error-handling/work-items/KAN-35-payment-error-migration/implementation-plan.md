# KAN-35 Payment Intent and Attempt Error Migration Implementation Plan

> **Execution prerequisite:** Follow this plan task-by-task with tests written
> before production changes and a verification checkpoint before each commit.

**Goal:** Migrate payment-intent and payment-attempt failures to the approved
financial-owned neutral contract without changing payment rules or success
behavior.

**Architecture:** Keep orchestration in `FinancialService`, add the approved
descriptors and typed exceptions, and replace load-then-authorize payment
lookups with operation-specific repository queries. The existing shared RFC
9457 adapter renders the failures, while conditional PostgreSQL transitions
and their transaction boundaries remain unchanged.

**Tech Stack:** Java 21, Spring Boot 3.3.2, Spring Security, Spring Data JPA,
PostgreSQL, Flyway, JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers, ArchUnit,
Maven Surefire, and Maven Failsafe.

**Spec:**
`docs/error-handling/work-items/KAN-35-payment-error-migration/design.md`

## Global constraints

- Preserve all existing payment rules, routes, success DTOs, success statuses,
  conditional updates, transactions, webhook replay, outbox, and audit
  behavior.
- Use exactly the eight codes, categories, and public details approved in the
  KAN-35 specification.
- Authenticate first and establish resource authority through scoped lookup
  before evaluating state, expiry, method, or provider details.
- Missing and non-owned resources must be publicly indistinguishable.
- Wrong-provider callbacks return `PAYMENT_ATTEMPT_NOT_FOUND`; expose
  `PAYMENT_PROVIDER_MISMATCH` only after browser/administrator authority is
  established.
- Public responses use only allowlisted descriptor text. Provider text,
  diagnostic text, domain exception messages, SQL, class names, causes, and
  stack traces must not enter Problem Details.
- Unexpected runtime and persistence failures remain on the generic sanitized
  500 path.
- Do not add dependencies, Flyway migrations, optimistic locking, attempt
  uniqueness, JWT/OAuth2, or payment-rule redesign.
- Do not mark the complete financial module as migrated while settlement and
  repayment still use the legacy stack.

---

## File structure

### Neutral payment contract

- Modify
  `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java`
  to own the eight descriptors.
- Rewrite the existing payment exception classes under
  `financial/application/exception` to extend `ApplicationException`.
- Create `PaymentStateConflictException.java` and
  `PaymentProviderMismatchException.java` for distinct 409 and 422 failures.
- Delete `InvalidPaymentStateException.java` after all payment callers move to
  `PaymentStateConflictException`.
- Create
  `src/test/java/com/project/optrabidz/financial/application/FinancialPaymentErrorContractTest.java`
  to lock descriptor and diagnostic contracts.

### Scoped persistence

- Modify the two domain repository ports and their Spring Data/adaptor
  implementations.
- Extend `PaymentIntentRepositoryIT.java` and create
  `PaymentAttemptRepositoryIT.java` for PostgreSQL-backed scope tests.
- Keep global `findById` for explicit administrator and already-authorized
  internal reloads.

### Application selection

- Modify `FinancialService.java` only where payment intent/attempt behavior is
  selected.
- Update `FinancialServiceTest.java` to verify ownership precedence, state
  selection, provider behavior, idempotency, and protected diagnostics.
- Do not extract a new service or refactor unrelated settlement/repayment
  methods in this story.

### HTTP and architecture verification

- Update `FinancialApiIT.java` and `PaymentProviderWebhookApiIT.java` to assert
  RFC 9457 contracts and disclosure behavior.
- Modify `ExceptionArchitectureTest.java` with a payment-exception-specific
  legacy-dependency rule.
- Preserve existing webhook, outbox, audit, security, and concurrent payment
  integration tests.

---

### Task 1: Define and guard the neutral payment error contract

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentIntentNotFoundException.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentAttemptNotFoundException.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentIntentExpiredException.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentIntentNotActiveException.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentAlreadyConfirmedException.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/exception/UnsupportedPaymentMethodException.java`
- Create:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentStateConflictException.java`
- Create:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentProviderMismatchException.java`
- Delete after caller migration:
  `src/main/java/com/project/optrabidz/financial/application/exception/InvalidPaymentStateException.java`
- Test:
  `src/test/java/com/project/optrabidz/financial/application/FinancialPaymentErrorContractTest.java`
- Modify:
  `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`

**Interfaces:**

- Consumes: `ErrorDescriptor`, `ErrorCategory`, and `ApplicationException` from
  `com.project.optrabidz.common.error`.
- Produces: eight constants on `FinancialErrors` and typed exception
  constructors accepting a protected `String diagnosticMessage`.
- Produces: cause-aware constructors on `PaymentStateConflictException` for
  sanitizing caught domain transition failures.

- [ ] **Step 1: Write the failing descriptor and exception contract test**

Create a parameterized test whose data is exactly:

```java
private static Stream<Arguments> descriptors() {
    return Stream.of(
            arguments(FinancialErrors.PAYMENT_INTENT_NOT_FOUND,
                    "PAYMENT_INTENT_NOT_FOUND", ErrorCategory.NOT_FOUND,
                    "The requested payment intent was not found"),
            arguments(FinancialErrors.PAYMENT_ATTEMPT_NOT_FOUND,
                    "PAYMENT_ATTEMPT_NOT_FOUND", ErrorCategory.NOT_FOUND,
                    "The requested payment attempt was not found"),
            arguments(FinancialErrors.PAYMENT_INTENT_EXPIRED,
                    "PAYMENT_INTENT_EXPIRED", ErrorCategory.CONFLICT,
                    "The payment intent has expired"),
            arguments(FinancialErrors.PAYMENT_INTENT_NOT_ACTIVE,
                    "PAYMENT_INTENT_NOT_ACTIVE", ErrorCategory.CONFLICT,
                    "The payment intent is not active"),
            arguments(FinancialErrors.PAYMENT_ALREADY_CONFIRMED,
                    "PAYMENT_ALREADY_CONFIRMED", ErrorCategory.CONFLICT,
                    "The payment has already been confirmed"),
            arguments(FinancialErrors.PAYMENT_STATE_CONFLICT,
                    "PAYMENT_STATE_CONFLICT", ErrorCategory.CONFLICT,
                    "The payment state no longer permits this operation"),
            arguments(FinancialErrors.PAYMENT_METHOD_UNSUPPORTED,
                    "PAYMENT_METHOD_UNSUPPORTED", ErrorCategory.BUSINESS_RULE,
                    "The selected payment method is not supported"),
            arguments(FinancialErrors.PAYMENT_PROVIDER_MISMATCH,
                    "PAYMENT_PROVIDER_MISMATCH", ErrorCategory.BUSINESS_RULE,
                    "The payment attempt cannot be handled by this provider")
    );
}
```

Assert `code()`, `category()`, and `publicDetail()` for each descriptor. Add a
second parameterized source mapping exception factories to these exact
diagnostic codes:

```text
PaymentIntentNotFoundException      FINANCIAL.PAYMENT.INTENT.NOT_FOUND
PaymentAttemptNotFoundException     FINANCIAL.PAYMENT.ATTEMPT.NOT_FOUND
PaymentIntentExpiredException       FINANCIAL.PAYMENT.INTENT.EXPIRED
PaymentIntentNotActiveException     FINANCIAL.PAYMENT.INTENT.NOT.ACTIVE
PaymentAlreadyConfirmedException    FINANCIAL.PAYMENT.ALREADY.CONFIRMED
PaymentStateConflictException       FINANCIAL.PAYMENT.STATE.CONFLICT
UnsupportedPaymentMethodException   FINANCIAL.PAYMENT.METHOD.UNSUPPORTED
PaymentProviderMismatchException    FINANCIAL.PAYMENT.PROVIDER.MISMATCH
```

For every factory, pass `protected-provider-sentinel`, assert the expected
descriptor and diagnostic code, and assert that the sentinel appears only in
`getMessage()`, never in `descriptor().publicDetail()`.

- [ ] **Step 2: Run the new contract test and verify failure**

Run:

```powershell
.\mvnw.cmd -Dtest=FinancialPaymentErrorContractTest test
```

Expected: compilation fails because the eight payment descriptors and the two
new exception classes do not yet exist.

- [ ] **Step 3: Add the exact descriptors and transport-neutral exceptions**

Add the descriptor constants exactly as listed in Step 1. Rewrite each
existing payment exception using this shape:

```java
public final class PaymentIntentNotFoundException extends ApplicationException {
    public PaymentIntentNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_INTENT_NOT_FOUND,
                "FINANCIAL.PAYMENT.INTENT.NOT_FOUND",
                diagnosticMessage
        );
    }
}
```

Use the mapping table from Step 1 for every other class. Implement the
cause-aware state-conflict constructor as:

```java
public PaymentStateConflictException(String diagnosticMessage, Throwable cause) {
    super(
            FinancialErrors.PAYMENT_STATE_CONFLICT,
            "FINANCIAL.PAYMENT.STATE.CONFLICT",
            diagnosticMessage,
            cause
    );
}
```

Do not copy a caught exception message into `diagnosticMessage`.

- [ ] **Step 4: Add the payment-specific architecture rule**

Add an ArchUnit rule matching these migrated exception names and forbidding a
dependency on `..common.api.exception..`:

```java
@ArchTest
static final ArchRule PAYMENT_EXCEPTIONS_USE_NEUTRAL_ERROR_CONTRACT =
        noClasses()
                .that().haveNameMatching(
                        ".*\\.(PaymentIntentNotFound|PaymentAttemptNotFound|"
                                + "PaymentIntentExpired|PaymentIntentNotActive|"
                                + "PaymentAlreadyConfirmed|PaymentStateConflict|"
                                + "UnsupportedPaymentMethod|PaymentProviderMismatch)Exception"
                )
                .should().dependOnClassesThat().resideInAPackage(
                        "..common.api.exception.."
                )
                .as("migrated payment exceptions must use the neutral error contract");
```

Do not add `..financial..` to the complete migrated-module rule.

- [ ] **Step 5: Run contract and architecture tests**

Run:

```powershell
.\mvnw.cmd -Dtest=FinancialPaymentErrorContractTest,ExceptionArchitectureTest test
```

Expected: PASS.

- [ ] **Step 6: Commit the neutral contract**

```powershell
git add src/main/java/com/project/optrabidz/financial/application/error `
  src/main/java/com/project/optrabidz/financial/application/exception `
  src/test/java/com/project/optrabidz/financial/application/FinancialPaymentErrorContractTest.java `
  src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java
git commit -m "refactor(KAN-35): define neutral payment errors"
```

---

### Task 2: Add operation-specific payment repository lookups

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/financial/domain/repository/PaymentIntentRepository.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/domain/repository/PaymentAttemptRepository.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/infrastructure/repository/JpaPaymentIntentRepository.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/infrastructure/repository/JpaPaymentAttemptRepository.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/infrastructure/repository/PaymentIntentRepositoryAdapter.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/infrastructure/repository/PaymentAttemptRepositoryAdapter.java`
- Test:
  `src/test/java/com/project/optrabidz/financial/infrastructure/repository/PaymentIntentRepositoryIT.java`
- Create:
  `src/test/java/com/project/optrabidz/financial/infrastructure/repository/PaymentAttemptRepositoryIT.java`

**Interfaces:**

- Produces:
  `Optional<PaymentIntent> findByIdForParticipant(Long paymentIntentId, Long accountId)`.
- Produces:
  `Optional<PaymentIntent> findByIdForPayer(Long paymentIntentId, Long payerAccountId)`.
- Produces:
  `Optional<PaymentAttempt> findByIdForPayer(Long paymentAttemptId, Long payerAccountId)`.
- Produces:
  `Optional<PaymentAttempt> findByIdForProvider(Long paymentAttemptId, String providerCode)`.
- Preserves both repositories' existing global `findById` and conditional
  transition methods.

- [ ] **Step 1: Write failing PostgreSQL scope tests**

Extend `PaymentIntentRepositoryIT` with one saved intent whose payer and payee
are different accounts, then assert:

```java
assertThat(repository.findByIdForParticipant(intentId, payerId)).isPresent();
assertThat(repository.findByIdForParticipant(intentId, payeeId)).isPresent();
assertThat(repository.findByIdForParticipant(intentId, unrelatedId)).isEmpty();
assertThat(repository.findByIdForPayer(intentId, payerId)).isPresent();
assertThat(repository.findByIdForPayer(intentId, payeeId)).isEmpty();
```

Create `PaymentAttemptRepositoryIT` using
`PostgresJpaIntegrationTestSupport`. Persist one intent and one attempt, then
assert:

```java
assertThat(repository.findByIdForPayer(attemptId, payerId)).isPresent();
assertThat(repository.findByIdForPayer(attemptId, payeeId)).isEmpty();
assertThat(repository.findByIdForPayer(attemptId, unrelatedId)).isEmpty();
assertThat(repository.findByIdForProvider(attemptId, "local")).isPresent();
assertThat(repository.findByIdForProvider(attemptId, "SANDBOX_CARD")).isEmpty();
```

Load the persistence adapters explicitly:

```java
@Import({
        FinancialPersistenceMapper.class,
        PaymentIntentRepositoryAdapter.class,
        PaymentAttemptRepositoryAdapter.class
})
class PaymentAttemptRepositoryIT extends PostgresJpaIntegrationTestSupport {
}
```

Create the intent references with
`PostgresTestDataFixture.createSettlementReference("payment-scope")`, derive
`payerId` and `payeeId` from the returned `PaymentReference`, and use a
different reference's payer as `unrelatedId`. Rely on the existing
transactional `@DataJpaTest` rollback for cleanup.

- [ ] **Step 2: Run the repository integration tests and verify failure**

Run:

```powershell
.\mvnw.cmd -Pintegration-tests `
  -Dtest=FinancialPaymentErrorContractTest `
  -Dit.test=PaymentIntentRepositoryIT,PaymentAttemptRepositoryIT verify
```

Expected: compilation fails because the four scoped port methods do not exist.

- [ ] **Step 3: Add scoped methods to the domain ports and JPA repositories**

Add the four exact port signatures from the Interfaces section. Add these
Spring Data queries:

```java
@Query("""
        select pi from PaymentIntent pi
        where pi.paymentIntentId = :paymentIntentId
          and (pi.payerAccountId = :accountId or pi.payeeAccountId = :accountId)
        """)
Optional<PaymentIntent> findForParticipant(
        @Param("paymentIntentId") Long paymentIntentId,
        @Param("accountId") Long accountId);

Optional<PaymentIntent> findByPaymentIntentIdAndPayerAccountId(
        Long paymentIntentId,
        Long payerAccountId);
```

```java
@Query("""
        select pa from PaymentAttempt pa
        where pa.paymentAttemptId = :paymentAttemptId
          and exists (
              select pi.paymentIntentId from PaymentIntent pi
              where pi.paymentIntentId = pa.paymentIntentId
                and pi.payerAccountId = :payerAccountId
          )
        """)
Optional<PaymentAttempt> findForPayer(
        @Param("paymentAttemptId") Long paymentAttemptId,
        @Param("payerAccountId") Long payerAccountId);

Optional<PaymentAttempt> findByPaymentAttemptIdAndProviderCodeIgnoreCase(
        Long paymentAttemptId,
        String providerCode);
```

Map these methods through the repository adapters without adding policy or
exception translation there.

- [ ] **Step 4: Run the repository integration tests**

Run the command from Step 2 again.

Expected: PASS, including payer-versus-payee and case-insensitive provider
scope.

- [ ] **Step 5: Commit the scoped persistence boundary**

```powershell
git add src/main/java/com/project/optrabidz/financial/domain/repository `
  src/main/java/com/project/optrabidz/financial/infrastructure/repository `
  src/test/java/com/project/optrabidz/financial/infrastructure/repository
git commit -m "refactor(KAN-35): scope payment resource lookups"
```

---

### Task 3: Migrate payment application error selection

**Files:**

- Modify:
  `src/main/java/com/project/optrabidz/financial/application/FinancialService.java`
- Delete:
  `src/main/java/com/project/optrabidz/financial/application/exception/InvalidPaymentStateException.java`
- Test:
  `src/test/java/com/project/optrabidz/financial/application/FinancialServiceTest.java`

**Interfaces:**

- Consumes the four scoped repository methods introduced in Task 2.
- Consumes the eight descriptors and exceptions introduced in Task 1.
- Preserves all public `FinancialService` method signatures.
- Produces deterministic authority-first payment failure selection.

- [ ] **Step 1: Replace payment unit-test expectations with neutral failures**

Update Mockito stubs so ordinary intent reads use
`findByIdForParticipant`, attempt creation uses `findByIdForPayer`, local
attempt actions use the attempt payer lookup, and provider callbacks use the
provider lookup.

Add focused tests proving:

```text
missing participant intent       -> PAYMENT_INTENT_NOT_FOUND
non-owned participant intent     -> PAYMENT_INTENT_NOT_FOUND
non-owned payer intent           -> PAYMENT_INTENT_NOT_FOUND
non-owned local attempt          -> PAYMENT_ATTEMPT_NOT_FOUND
wrong-provider callback attempt  -> PAYMENT_ATTEMPT_NOT_FOUND
owned non-local local action     -> PAYMENT_PROVIDER_MISMATCH
unsupported method/currency      -> PAYMENT_METHOD_UNSUPPORTED
confirmed intent                 -> PAYMENT_ALREADY_CONFIRMED
expired intent                   -> PAYMENT_INTENT_EXPIRED
other inactive intent            -> PAYMENT_INTENT_NOT_ACTIVE
opposite attempt terminal state  -> PAYMENT_STATE_CONFLICT
```

For each failure assert the `ApplicationException.descriptor()` identity, not
the protected diagnostic message. Verify wrong-provider callback tests never
invoke the global attempt lookup or load the linked intent.

- [ ] **Step 2: Run the application unit tests and verify failure**

Run:

```powershell
.\mvnw.cmd -Dtest=FinancialServiceTest,FinancialPaymentErrorContractTest test
```

Expected: FAIL because `FinancialService` still performs global payment
lookups and throws legacy ownership/provider errors.

- [ ] **Step 3: Implement authority-first intent selection**

Use explicit administrator and ordinary-caller branches:

```java
private PaymentIntent getVisiblePaymentIntent(
        Long accountId, RoleType roleType, Long paymentIntentId) {
    Optional<PaymentIntent> result = roleType == RoleType.ADMIN
            ? paymentIntentRepository.findById(paymentIntentId)
            : paymentIntentRepository.findByIdForParticipant(
                    paymentIntentId, accountId);
    return result.orElseThrow(() -> new PaymentIntentNotFoundException(
            "Payment intent unavailable for participant lookup"
    ));
}

private PaymentIntent getActionablePaymentIntent(
        Long accountId, RoleType roleType, Long paymentIntentId) {
    Optional<PaymentIntent> result = roleType == RoleType.ADMIN
            ? paymentIntentRepository.findById(paymentIntentId)
            : paymentIntentRepository.findByIdForPayer(
                    paymentIntentId, accountId);
    return result.orElseThrow(() -> new PaymentIntentNotFoundException(
            "Payment intent unavailable for payer lookup"
    ));
}
```

Use the first helper for intent reads and the second for attempt creation.
Remove `ensurePaymentIntentVisible` and the payment-intent use of
`ensurePaymentActor`; leave unrelated financial authorization methods alone.

- [ ] **Step 4: Implement authority-first attempt selection**

For local browser actions, load globally only for administrators; otherwise
use `findByIdForPayer`. For callbacks, use `findByIdForProvider` directly:

```java
private PaymentAttempt getActorPaymentAttempt(
        Long accountId, RoleType roleType, Long paymentAttemptId) {
    Optional<PaymentAttempt> result = roleType == RoleType.ADMIN
            ? paymentAttemptRepository.findById(paymentAttemptId)
            : paymentAttemptRepository.findByIdForPayer(
                    paymentAttemptId, accountId);
    return result.orElseThrow(() -> new PaymentAttemptNotFoundException(
            "Payment attempt unavailable for payer lookup"
    ));
}

private PaymentAttempt getProviderPaymentAttempt(
        String providerCode, Long paymentAttemptId) {
    return paymentAttemptRepository.findByIdForProvider(
                    paymentAttemptId, providerCode)
            .orElseThrow(() -> new PaymentAttemptNotFoundException(
                    "Payment attempt unavailable for provider lookup"
            ));
}
```

After browser authority is known, reject a local endpoint used for a non-local
attempt with `PaymentProviderMismatchException`. Never perform this mismatch
check before callback provider scoping.

- [ ] **Step 5: Migrate state, method, and transition failures**

Make state classification return `ApplicationException` and select in this
order:

```java
if (paymentIntent.getPaymentState() == PaymentState.PAYMENT_CONFIRMED) {
    return new PaymentAlreadyConfirmedException(
            "Payment intent is already confirmed"
    );
}
if (paymentIntent.getPaymentState() == PaymentState.PAYMENT_EXPIRED
        || !paymentIntent.getExpiresAt().isAfter(Instant.now())) {
    return new PaymentIntentExpiredException(
            "Payment intent is expired"
    );
}
return new PaymentIntentNotActiveException(
        "Payment intent is not active"
);
```

Use `UnsupportedPaymentMethodException` only for an unavailable
provider/method/currency configuration. Replace incompatible attempt terminal
state with `PaymentStateConflictException`.

When converting `IllegalStateException` from a domain transition, use a fixed
diagnostic such as `Payment attempt transition rejected for current state` and
retain the original exception only as the cause. Never concatenate
`exception.getMessage()` into descriptor or diagnostic text.

- [ ] **Step 6: Preserve idempotency and transactional rollback**

Keep the current conditional SQL call order. If the attempt update affects
zero rows:

- return success for confirm-on-confirmed;
- return success for fail-on-failed; and
- otherwise throw `PaymentStateConflictException`.

If the intent update affects zero rows, reload it and classify confirmed,
expired, or other inactive state. Allow the existing transaction to roll back
the preceding attempt update and joined side effects.

- [ ] **Step 7: Run payment application and architecture tests**

Run:

```powershell
.\mvnw.cmd `
  -Dtest=FinancialPaymentErrorContractTest,FinancialServiceTest,ExceptionArchitectureTest `
  test
```

Expected: PASS. Also run:

```powershell
rg -n "InvalidPaymentStateException|PaymentIntentNotFoundException extends ApiException|PaymentAttemptNotFoundException extends ApiException|UnsupportedPaymentMethodException extends ApiException" src/main/java
```

Expected: no matches.

- [ ] **Step 8: Commit the application migration**

```powershell
git add src/main/java/com/project/optrabidz/financial/application `
  src/test/java/com/project/optrabidz/financial/application
git commit -m "refactor(KAN-35): migrate payment error selection"
```

---

### Task 4: Verify RFC 9457, disclosure, concurrency, and regressions

**Files:**

- Modify:
  `src/test/java/com/project/optrabidz/financial/api/FinancialApiIT.java`
- Modify:
  `src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookApiIT.java`

**Interfaces:**

- Consumes the unchanged HTTP routes and shared `RestExceptionHandler`.
- Produces integration evidence for exact RFC 9457 fields, uniform ownership
  disclosure, provider scoping, and preserved terminal-state behavior.

- [ ] **Step 1: Add a reusable payment Problem Details assertion**

In `FinancialApiIT`, add a helper returning `ResultMatcher[]` with these exact
assertions:

```java
private ResultMatcher[] paymentProblem(
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

- [ ] **Step 2: Write missing-versus-non-owned API tests**

For both intent read and attempt mutation, send one missing identifier and one
existing identifier owned by another authenticated account. Supply different
`X-Request-ID` values, remove `requestId`, `timestamp`, and `instance` from the
parsed JSON trees, then assert the remaining bodies are equal.

Both intent requests must be 404 with:

```text
title  = Resource not found
code   = PAYMENT_INTENT_NOT_FOUND
detail = The requested payment intent was not found
```

Both attempt requests must be 404 with:

```text
title  = Resource not found
code   = PAYMENT_ATTEMPT_NOT_FOUND
detail = The requested payment attempt was not found
```

- [ ] **Step 3: Write state, method, and disclosure API tests**

Cover the exact 409/422 contract rows. Inject a sentinel such as
`provider-secret-diagnostic-sentinel` into protected provider failure or test
exception context and assert the serialized response does not contain it.
Continue asserting the allowed descriptor detail.

Update legacy assertions such as `$.success`, `$.error.code`, and
`$.error.message` only for the migrated payment-intent/attempt failures. Do not
rewrite unrelated settlement and repayment legacy assertions in KAN-35.

- [ ] **Step 4: Add provider-callback anti-enumeration coverage**

In `PaymentProviderWebhookApiIT`, submit an authenticated, replay-safe event
whose provider code does not own the referenced attempt. Assert:

```text
HTTP 404
code   = PAYMENT_ATTEMPT_NOT_FOUND
detail = The requested payment attempt was not found
```

Assert the body does not contain the real attempt provider, provider payment
ID, event ID, replay hash, signature, payload, or protected diagnostic text.
Verify the replay claim and financial mutations roll back.

- [ ] **Step 5: Re-run existing concurrent payment scenarios**

Keep and execute the current integration tests proving:

- two concurrent confirms both receive 200 and create business effects once;
- two concurrent failures both receive 200;
- concurrent confirm versus fail receives exactly one 200 and one 409; and
- attempt, intent, settlement/repayment, outbox, and audit state agree with the
  winning result.

For the losing confirm/fail response, additionally assert
`PAYMENT_STATE_CONFLICT` and its allowlisted detail where the attempt terminal
state is incompatible.

- [ ] **Step 6: Run focused unit and integration verification**

Run:

```powershell
.\mvnw.cmd -Pintegration-tests `
  -Dtest=FinancialPaymentErrorContractTest,FinancialServiceTest,ExceptionArchitectureTest `
  -Dit.test=PaymentIntentRepositoryIT,PaymentAttemptRepositoryIT,FinancialApiIT,PaymentProviderWebhookApiIT `
  verify
```

Expected: PASS with no skipped named tests and no leaked sentinel values.

- [ ] **Step 7: Run the complete repository verification**

Confirm Docker is available, then run:

```powershell
docker info
.\mvnw.cmd verify -Pintegration-tests
```

Expected: Docker reports a running engine; every unit, architecture, and
Testcontainers integration test passes.

- [ ] **Step 8: Commit the API and regression evidence**

```powershell
git add src/test/java/com/project/optrabidz/financial/api
git commit -m "test(KAN-35): verify payment error contracts"
```

---

### Task 5: Record completion evidence and prepare review

**Files:**

- Modify:
  `docs/error-handling/work-items/KAN-35-payment-error-migration/design.md`
- Modify:
  `docs/error-handling/work-items/KAN-35-payment-error-migration/implementation-plan.md`

**Interfaces:**

- Consumes the exact commands and results from Task 4.
- Produces a reviewable evidence record; it does not change production
  behavior.

- [ ] **Step 1: Inspect the complete branch diff**

Run:

```powershell
git status --short
git diff --check develop...HEAD
git diff --stat develop...HEAD
git log --oneline develop..HEAD
```

Expected: no whitespace errors, no unrelated files, and only KAN-35 commits.

- [ ] **Step 2: Verify legacy payment dependencies are absent**

Run:

```powershell
rg -n "extends ApiException|ErrorCode" `
  src/main/java/com/project/optrabidz/financial/application/exception/Payment*Exception.java `
  src/main/java/com/project/optrabidz/financial/application/exception/UnsupportedPaymentMethodException.java
```

Expected: no matches. Do not require unrelated settlement and repayment
exceptions to pass this scoped search.

- [ ] **Step 3: Update documentation status and evidence**

Change the design status to `Implemented and verified` only after Task 4's
complete verification passes. Append the exact successful focused and full
Maven commands, their test totals, and the tested commit hash to this plan.
Leave unchecked acceptance items unchanged if evidence is missing.

- [ ] **Step 4: Commit and publish the evidence**

```powershell
git add docs/error-handling/work-items/KAN-35-payment-error-migration
git commit -m "docs(KAN-35): record verification evidence"
git push origin feature/KAN-35-payment-error-migration
```

- [ ] **Step 5: Prepare the pull request without merging**

Create a pull request from `feature/KAN-35-payment-error-migration` into
`develop`. Its description must summarize the neutral contract, scoped
ownership, provider anti-enumeration rule, preserved concurrency behavior,
tests run, and the separately deferred concurrent-attempt-creation risk.

Move KAN-35 to the review status and add the PR link and verification summary
to Jira. Do not merge until the pull request has been explicitly reviewed and
approved.
