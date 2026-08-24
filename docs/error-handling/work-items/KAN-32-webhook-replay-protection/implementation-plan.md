# KAN-32 Payment Webhook Replay Protection Implementation Plan

**Status:** Implementation complete; awaiting pull-request review

> **Execution requirement:** Implement this plan task by task with TDD,
> verification, and review checkpoints. Do not merge this branch into
> `develop` until the pull request is explicitly approved.

**Goal:** Process each authenticated provider webhook at most once, safely
acknowledge identical duplicates, reject identity collisions, and commit the
replay claim with financial state and outbox work atomically.

**Architecture:** Authentication and strict parsing remain before the database
transaction. A transport-neutral fingerprint factory creates a versioned
semantic event; a transactional replay service claims it through an application
port backed by PostgreSQL `INSERT ... ON CONFLICT DO NOTHING RETURNING`, invokes
financial processing only for the owner, and marks the claim `PROCESSED` before
commit. The existing outbox dispatcher derives business audit after commit.

**Tech Stack:** Java 21, Spring Boot 3.3.2, Spring transactions,
`NamedParameterJdbcTemplate`, PostgreSQL, Flyway baseline schema, Jackson,
JUnit 5, Mockito, MockMvc, ArchUnit, and Testcontainers 1.21.4.

**Spec:**
`docs/error-handling/work-items/KAN-32-webhook-replay-protection/design.md`

## Global constraints

- Work only on `feature/KAN-32-webhook-replay-protection` from approved base
  commit `5c99b135d4b2b4b164eb926e7dfff8483c87a6a4`.
- Do not edit `V1__baseline.sql`; the existing replay table and unique
  constraint are sufficient.
- Add no Maven dependency and do not change the project version.
- Verify HMAC and strictly parse before starting replay persistence.
- Use one `REQUIRED` transaction for replay claim, financial transition,
  outbox write, and `PROCESSED` completion.
- Keep the downstream business-audit write outside that transaction; it is
  produced by the existing outbox dispatcher.
- Return empty HTTP 204 for both first success and identical duplicate.
- Never persist raw bodies, request headers, signatures, secrets, or unknown
  provider fields.
- Never expose or log the semantic hash, provider event ID, provider diagnostic,
  exception message, class, cause, or stack trace.
- Persist bounded normalized provider failure diagnostics only in the protected
  replay JSONB payload; pass stable safe failure values to business state and
  outbox events.
- Keep servlet and HTTP types out of financial application replay types and
  PostgreSQL/Jackson details behind the replay-store port.
- Use the established review gates: plan approval, inline-execution approval,
  PR approval, then merge into `develop`; never merge into `main`.

## File map

### New production files

| File | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/financial/application/replay/PaymentWebhookReplayContent.java` | Immutable normalized semantic content |
| `src/main/java/com/project/optrabidz/financial/application/replay/PaymentWebhookReplayEvent.java` | Semantic content plus SHA-256 fingerprint |
| `src/main/java/com/project/optrabidz/financial/application/replay/PaymentWebhookReplayState.java` | Transport-neutral persisted-state enum |
| `src/main/java/com/project/optrabidz/financial/application/replay/StoredPaymentWebhookReplayEvent.java` | Stored replay identity, state, and semantic event |
| `src/main/java/com/project/optrabidz/financial/application/replay/PaymentWebhookReplayFingerprintFactory.java` | Deterministic versioned fingerprint creation using Java only |
| `src/main/java/com/project/optrabidz/financial/application/port/PaymentWebhookReplayStore.java` | Application persistence contract |
| `src/main/java/com/project/optrabidz/financial/application/PaymentWebhookReplayService.java` | Transactional claim, classification, processing, and completion |
| `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookReplayCollisionException.java` | Typed collision using the existing safe public descriptor |
| `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookReplayStateException.java` | Typed fail-closed error for unexpected committed replay state |
| `src/main/java/com/project/optrabidz/financial/infrastructure/repository/PostgresPaymentWebhookReplayStore.java` | PostgreSQL atomic-claim adapter and protected JSONB mapping |

### Modified production files

| File | Change |
|---|---|
| `src/main/java/com/project/optrabidz/financial/application/PaymentProviderWebhookIngressService.java` | Verify, parse, fingerprint, then call replay service; return `void` |
| `src/main/java/com/project/optrabidz/financial/application/PaymentProviderWebhookService.java` | Substitute stable safe failure values before financial processing |
| `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java` | Add the sanitized internal replay-processing descriptor |
| `src/main/java/com/project/optrabidz/financial/application/port/PaymentWebhookSecurityAuditor.java` | Add sanitized replay-collision operation |
| `src/main/java/com/project/optrabidz/financial/api/PaymentWebhookHttpIngress.java` | Return `void` and audit collision separately |
| `src/main/java/com/project/optrabidz/financial/api/PaymentProviderWebhookController.java` | Return empty 204 and remove success envelope/media production |
| `src/main/java/com/project/optrabidz/financial/infrastructure/audit/AuditPaymentWebhookSecurityAuditor.java` | Delegate fixed replay-collision action |
| `src/main/java/com/project/optrabidz/audit/application/SecurityAuditService.java` | Persist bounded replay-collision security evidence |

### Test files

| File | Responsibility |
|---|---|
| `src/test/java/com/project/optrabidz/financial/application/replay/PaymentWebhookReplayFingerprintFactoryTest.java` | Fingerprint determinism and field sensitivity |
| `src/test/java/com/project/optrabidz/financial/infrastructure/repository/PaymentWebhookReplayStoreIT.java` | Real PostgreSQL claim, duplicate, collision data, concurrency, and rollback |
| `src/test/java/com/project/optrabidz/financial/application/PaymentWebhookReplayServiceTest.java` | Owner/duplicate/collision/invariant orchestration |
| `src/test/java/com/project/optrabidz/financial/application/PaymentProviderWebhookIngressServiceTest.java` | Updated verify-parse-fingerprint-process ordering |
| `src/test/java/com/project/optrabidz/financial/application/PaymentProviderWebhookServiceTest.java` | Safe failure-value substitution |
| `src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookControllerTest.java` | Empty 204 adapter contract |
| `src/test/java/com/project/optrabidz/financial/api/PaymentWebhookHttpIngressTest.java` | Collision audit routing and void delegation |
| `src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookApiIT.java` | Real first, duplicate, collision, rollback/retry, disclosure, outbox, and audit flows |
| `src/test/java/com/project/optrabidz/financial/infrastructure/audit/AuditPaymentWebhookSecurityAuditorTest.java` | Safe provider/request delegation for collision |
| `src/test/java/com/project/optrabidz/audit/application/SecurityAuditServiceTest.java` | Bounded collision audit record |
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Replay boundary rules |

---

### Task 1: Versioned semantic replay fingerprint

**Files:**

- Create the five `financial/application/replay` files listed in the file map.
- Test:
  `src/test/java/com/project/optrabidz/financial/application/replay/PaymentWebhookReplayFingerprintFactoryTest.java`

**Interfaces:**

- Consumes:
  `PaymentProviderWebhookCommand` after strict parsing and normalization.
- Produces:

```java
public record PaymentWebhookReplayContent(
        int fingerprintVersion,
        String providerCode,
        String providerEventId,
        PaymentProviderWebhookEventType eventType,
        Long paymentAttemptId,
        String providerPaymentId,
        String providerFailureCode,
        String providerFailureMessage
) {}

public record PaymentWebhookReplayEvent(
        PaymentWebhookReplayContent content,
        String payloadHash
) {}

public enum PaymentWebhookReplayState {
    RECEIVED,
    PROCESSED,
    FAILED,
    IGNORED
}

public record StoredPaymentWebhookReplayEvent(
        long replayEventId,
        PaymentWebhookReplayState state,
        PaymentWebhookReplayEvent event
) {}

@Component
public final class PaymentWebhookReplayFingerprintFactory {
    public PaymentWebhookReplayEvent create(
            PaymentProviderWebhookCommand command
    );
}
```

- [x] **Step 1: Write fingerprint contract tests**

Create tests named:

```java
@Test
void identicalNormalizedCommandsProduceTheSameVersionOneHash() {
    PaymentWebhookReplayEvent first = factory.create(confirmedCommand(
            "evt-1001", "provider-payment-1001"));
    PaymentWebhookReplayEvent second = factory.create(confirmedCommand(
            "evt-1001", "provider-payment-1001"));

    assertThat(first).isEqualTo(second);
    assertThat(first.content().fingerprintVersion()).isEqualTo(1);
    assertThat(first.payloadHash()).matches("[0-9a-f]{64}");
}

@Test
void everyImmutableSemanticFieldChangesTheReplayIdentity() {
    PaymentWebhookReplayEvent baseline = factory.create(confirmedCommand(
            "evt-1001", "provider-payment-1001"));

    assertThat(factory.create(confirmedCommand(
            "evt-1002", "provider-payment-1001"))).isNotEqualTo(baseline);
    assertThat(factory.create(confirmedCommand(
            "evt-1001", "provider-payment-1002"))).isNotEqualTo(baseline);
    assertThat(factory.create(new PaymentProviderWebhookCommand(
            "CARD", PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
            1001L, "provider-payment-1001", null, null, "evt-1001"
    ))).isNotEqualTo(baseline);
}

@Test
void providerFailureDiagnosticsAreNormalizedAndFingerprintProtected() {
    PaymentWebhookReplayEvent first = factory.create(failedCommand(
            "upi_declined", " Provider declined "));
    PaymentWebhookReplayEvent normalized = factory.create(failedCommand(
            "UPI_DECLINED", "Provider declined"));
    PaymentWebhookReplayEvent changed = factory.create(failedCommand(
            "UPI_DECLINED", "Provider declined after retry"));

    assertThat(first).isEqualTo(normalized);
    assertThat(changed).isNotEqualTo(first);
    assertThat(first.content().providerFailureMessage())
            .isEqualTo("Provider declined");
}
```

- [x] **Step 2: Run the focused test and confirm RED**

Run:

```powershell
.\mvnw.cmd -Dtest=PaymentWebhookReplayFingerprintFactoryTest test
```

Expected: compilation fails because the replay types and factory do not exist.

- [x] **Step 3: Implement immutable content and deterministic hashing**

In `PaymentWebhookReplayFingerprintFactory`, construct version-1 content from
the already normalized command. Encode every field with an explicit length so
null and concatenation cannot collide:

```java
private static final int FINGERPRINT_VERSION = 1;

public PaymentWebhookReplayEvent create(PaymentProviderWebhookCommand command) {
    PaymentWebhookReplayContent content = new PaymentWebhookReplayContent(
            FINGERPRINT_VERSION,
            command.providerCode(),
            command.providerEventId(),
            command.eventType(),
            command.paymentAttemptId(),
            command.providerPaymentId(),
            command.failureCode(),
            command.failureMessage()
    );
    return new PaymentWebhookReplayEvent(content, sha256(canonicalBytes(content)));
}

private void writeNullableString(DataOutputStream output, String value)
        throws IOException {
    if (value == null) {
        output.writeInt(-1);
        return;
    }
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    output.writeInt(bytes.length);
    output.write(bytes);
}
```

Write the version as `int`, the attempt ID as `long`, and all other values in
the declared record order with `writeNullableString`. Use Java's guaranteed
`SHA-256` algorithm and lowercase `HexFormat`. Wrap impossible algorithm or
byte-stream failures in a fixed-message `IllegalStateException` containing no
event data.

- [x] **Step 4: Run focused tests and confirm GREEN**

```powershell
.\mvnw.cmd -Dtest=PaymentWebhookReplayFingerprintFactoryTest test
```

Expected: all fingerprint tests pass.

- [x] **Step 5: Commit the semantic boundary**

```powershell
git add src/main/java/com/project/optrabidz/financial/application/replay src/test/java/com/project/optrabidz/financial/application/replay
git commit -m "feat(KAN-32): add semantic webhook fingerprint"
```

---

### Task 2: Atomic PostgreSQL replay-store adapter

**Files:**

- Create:
  `src/main/java/com/project/optrabidz/financial/application/port/PaymentWebhookReplayStore.java`
- Create:
  `src/main/java/com/project/optrabidz/financial/infrastructure/repository/PostgresPaymentWebhookReplayStore.java`
- Test:
  `src/test/java/com/project/optrabidz/financial/infrastructure/repository/PaymentWebhookReplayStoreIT.java`

**Interfaces:**

```java
public interface PaymentWebhookReplayStore {
    OptionalLong tryClaim(PaymentWebhookReplayEvent event, Instant receivedAt);

    Optional<StoredPaymentWebhookReplayEvent> findByIdentity(
            String providerCode,
            String providerEventId
    );

    void markProcessed(
            long replayEventId,
            long paymentIntentId,
            long paymentAttemptId,
            Instant processedAt
    );
}
```

- [x] **Step 1: Write real-PostgreSQL adapter tests**

Extend `PostgresJpaIntegrationTestSupport`, import the adapter and Jackson
auto-configuration, inject `PaymentWebhookReplayStore`, `JdbcTemplate`, and
`PlatformTransactionManager`, and use unique event IDs per test. Build adapter
fixtures with baseline provider `RAZORPAY`; unlike `UPI`, it is reference data
available in the JPA slice without running the sandbox application initializer.

Create these test cases:

```java
@Test
void firstClaimPersistsOnlyAllowlistedNormalizedContent() {
    PaymentWebhookReplayEvent event = confirmedEvent("evt-store-first");

    OptionalLong claim = store.tryClaim(event, NOW);

    assertThat(claim).isPresent();
    Map<String, Object> row = jdbcTemplate.queryForMap("""
            select processing_state::text as state, payload_hash,
                   payload::text as payload, failure_message
            from payment_webhook_event
            where payment_webhook_event_id = ?
            """, claim.getAsLong());
    assertThat(row.get("state")).isEqualTo("RECEIVED");
    assertThat(row.get("payload_hash")).isEqualTo(event.payloadHash());
    assertThat(row.get("payload").toString())
            .contains("fingerprintVersion")
            .doesNotContain("signature", "rawBody", "headers", "secret");
    assertThat(row.get("failure_message")).isNull();
}

@Test
void duplicateClaimReturnsEmptyAndLoadsTheCommittedEvent() {
    PaymentWebhookReplayEvent event = confirmedEvent("evt-store-duplicate");
    long id = store.tryClaim(event, NOW).orElseThrow();
    store.markProcessed(id, PAYMENT_INTENT_ID, PAYMENT_ATTEMPT_ID, NOW.plusSeconds(1));

    assertThat(store.tryClaim(event, NOW.plusSeconds(2))).isEmpty();
    assertThat(store.findByIdentity("RAZORPAY", "evt-store-duplicate"))
            .contains(new StoredPaymentWebhookReplayEvent(
                    id, PaymentWebhookReplayState.PROCESSED, event));
}

@Test
void rollbackRemovesTheClaimSoTheSameIdentityCanBeClaimedAgain() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.executeWithoutResult(status -> {
        assertThat(store.tryClaim(confirmedEvent("evt-store-rollback"), NOW))
                .isPresent();
        status.setRollbackOnly();
    });

    assertThat(store.tryClaim(
            confirmedEvent("evt-store-rollback"), NOW.plusSeconds(1)))
            .isPresent();
}
```

Add a two-thread test using `CountDownLatch` and two `TransactionTemplate`
instances. Both workers call `tryClaim` for the same event and commit. Assert
exactly one result is present, the other is empty, one database row exists,
and the empty claimant can load the winner. Annotate the rollback and
concurrency tests with
`@Transactional(propagation = Propagation.NOT_SUPPORTED)` so their explicit
worker transactions do not join the default test-managed transaction.

- [x] **Step 2: Run the adapter IT and confirm RED**

```powershell
.\mvnw.cmd -Pintegration-tests -Dit.test=PaymentWebhookReplayStoreIT verify
```

Expected: compilation fails because the port and adapter do not exist.

- [x] **Step 3: Implement the atomic adapter**

Use `NamedParameterJdbcTemplate`. The claim SQL must be:

```java
private static final String CLAIM_SQL = """
        insert into payment_webhook_event (
            provider_code, provider_event_id, event_type,
            payment_attempt_id, processing_state, received_at,
            payload_hash, payload, failure_message
        ) values (
            :providerCode, :providerEventId, :eventType,
            :paymentAttemptId, cast('RECEIVED' as payment_webhook_processing_state_enum),
            :receivedAt, :payloadHash, cast(:payload as jsonb), null
        )
        on conflict (provider_code, provider_event_id) do nothing
        returning payment_webhook_event_id
        """;
```

Serialize only `PaymentWebhookReplayContent` with the injected `ObjectMapper`.
On lookup, select `processing_state`, `payload_hash`, and `payload`, deserialize
the JSONB into `PaymentWebhookReplayContent`, and reconstruct the stored event.
Use a fixed diagnostic message if JSON serialization or deserialization fails;
never append JSON or identifiers.

Completion SQL must enforce the expected lifecycle:

```sql
update payment_webhook_event
set processing_state = cast('PROCESSED' as payment_webhook_processing_state_enum),
    payment_intent_id = :paymentIntentId,
    payment_attempt_id = :paymentAttemptId,
    processed_at = :processedAt
where payment_webhook_event_id = :replayEventId
  and processing_state = cast('RECEIVED' as payment_webhook_processing_state_enum)
```

Require exactly one updated row; otherwise throw
`IllegalStateException("Payment webhook replay completion invariant failed")`.

- [x] **Step 4: Run adapter tests and confirm GREEN**

```powershell
.\mvnw.cmd -Pintegration-tests -Dit.test=PaymentWebhookReplayStoreIT verify
```

Expected: first claim, duplicate lookup, concurrency, completion, protected
payload, and rollback/retry tests pass against Testcontainers PostgreSQL.

- [x] **Step 5: Commit the persistence adapter**

```powershell
git add src/main/java/com/project/optrabidz/financial/application/port/PaymentWebhookReplayStore.java src/main/java/com/project/optrabidz/financial/infrastructure/repository/PostgresPaymentWebhookReplayStore.java src/test/java/com/project/optrabidz/financial/infrastructure/repository/PaymentWebhookReplayStoreIT.java
git commit -m "feat(KAN-32): persist atomic webhook replay claims"
```

---

### Task 3: Transactional owner, duplicate, collision, and safe failure processing

**Files:**

- Create:
  `src/main/java/com/project/optrabidz/financial/application/PaymentWebhookReplayService.java`
- Create:
  `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookReplayCollisionException.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/PaymentProviderWebhookIngressService.java`
- Modify:
  `src/main/java/com/project/optrabidz/financial/application/PaymentProviderWebhookService.java`
- Test:
  `src/test/java/com/project/optrabidz/financial/application/PaymentWebhookReplayServiceTest.java`
- Modify tests:
  `PaymentProviderWebhookIngressServiceTest.java` and
  `PaymentProviderWebhookServiceTest.java`

**Interfaces:**

```java
@Service
public class PaymentWebhookReplayService {
    @Transactional
    public void handle(
            PaymentProviderWebhookCommand command,
            PaymentWebhookReplayEvent replayEvent
    );
}
```

`PaymentWebhookReplayCollisionException` extends `ApplicationException` and
uses `FinancialErrors.PAYMENT_WEBHOOK_PAYLOAD_INVALID`, diagnostic code
`FINANCIAL.WEBHOOK.PAYLOAD.REPLAY_COLLISION`, and fixed diagnostic message
`Authenticated webhook event identity collision`.

- [x] **Step 1: Write orchestration tests**

Cover the following exact behavior:

```java
@Test
void ownerProcessesFinancialChangeAndMarksReplayProcessed() {
    when(store.tryClaim(event, any(Instant.class)))
            .thenReturn(OptionalLong.of(REPLAY_ID));
    when(webhookService.handle(command)).thenReturn(response);
    when(response.paymentIntentId()).thenReturn(PAYMENT_INTENT_ID);
    when(response.paymentAttemptId()).thenReturn(PAYMENT_ATTEMPT_ID);

    service.handle(command, event);

    InOrder order = inOrder(store, webhookService);
    order.verify(store).tryClaim(eq(event), any(Instant.class));
    order.verify(webhookService).handle(command);
    order.verify(store).markProcessed(
            eq(REPLAY_ID), eq(PAYMENT_INTENT_ID), eq(PAYMENT_ATTEMPT_ID),
            any(Instant.class));
}

@Test
void identicalProcessedDuplicateSkipsFinancialProcessing() {
    when(store.tryClaim(event, any(Instant.class))).thenReturn(OptionalLong.empty());
    when(store.findByIdentity("UPI", "evt-1001"))
            .thenReturn(Optional.of(new StoredPaymentWebhookReplayEvent(
                    REPLAY_ID, PaymentWebhookReplayState.PROCESSED, event)));

    service.handle(command, event);

    verifyNoInteractions(webhookService);
    verify(store, never()).markProcessed(anyLong(), anyLong(), anyLong(), any());
}

@Test
void processedIdentityWithDifferentContentThrowsCollision() {
    when(store.tryClaim(event, any(Instant.class))).thenReturn(OptionalLong.empty());
    when(store.findByIdentity("UPI", "evt-1001"))
            .thenReturn(Optional.of(new StoredPaymentWebhookReplayEvent(
                    REPLAY_ID, PaymentWebhookReplayState.PROCESSED,
                    differentEvent)));

    assertThatThrownBy(() -> service.handle(command, event))
            .isInstanceOf(PaymentWebhookReplayCollisionException.class);
    verifyNoInteractions(webhookService);
}
```

Also test that `RECEIVED`, `FAILED`, and `IGNORED` committed states and a
missing conflict row each throw the fixed, sanitized
`PaymentWebhookReplayStateException`, and that a financial-service exception
prevents `markProcessed`.

- [x] **Step 2: Run focused tests and confirm RED**

```powershell
.\mvnw.cmd -Dtest=PaymentWebhookReplayServiceTest,PaymentProviderWebhookIngressServiceTest,PaymentProviderWebhookServiceTest test
```

Expected: compilation or assertions fail because replay orchestration and safe
failure substitution are absent.

- [x] **Step 3: Implement transactional classification**

The service algorithm must be:

```java
@Transactional
public void handle(PaymentProviderWebhookCommand command,
                   PaymentWebhookReplayEvent replayEvent) {
    OptionalLong claim = replayStore.tryClaim(replayEvent, Instant.now());
    if (claim.isEmpty()) {
        classifyExisting(replayEvent);
        return;
    }

    PaymentAttemptResponse response = webhookService.handle(command);
    replayStore.markProcessed(
            claim.getAsLong(),
            response.paymentIntentId(),
            response.paymentAttemptId(),
            Instant.now()
    );
}
```

`classifyExisting` first requires a stored record, then requires
`PaymentWebhookReplayState.PROCESSED`, then compares the complete stored and
incoming `PaymentWebhookReplayEvent`. Equality returns normally; inequality
throws `PaymentWebhookReplayCollisionException`.

Update ingress ordering to:

```java
verifier.verify(envelope);
PaymentProviderWebhookCommand command = eventParser.parse(
        envelope.providerCode(), envelope.rawBody());
PaymentWebhookReplayEvent replayEvent = fingerprintFactory.create(command);
replayService.handle(command, replayEvent);
```

Change ingress `handle` to `void`.

- [x] **Step 4: Separate protected provider diagnostics from business values**

Keep the original normalized diagnostics in the command for fingerprinting and
protected replay JSONB. In `PaymentProviderWebhookService`, use:

```java
private static final String SAFE_PROVIDER_FAILURE_CODE =
        "PROVIDER_REPORTED_FAILURE";
private static final String SAFE_PROVIDER_FAILURE_MESSAGE =
        "Payment provider reported that the payment failed";

return financialService.failProviderPaymentAttempt(
        command.providerCode(),
        command.paymentAttemptId(),
        SAFE_PROVIDER_FAILURE_CODE,
        SAFE_PROVIDER_FAILURE_MESSAGE
);
```

Update the failure delegation test to verify the safe constants and explicitly
verify that `UPI_DECLINED` and `Provider declined` are not passed to
`FinancialService`.

- [x] **Step 5: Run focused tests and confirm GREEN**

```powershell
.\mvnw.cmd -Dtest=PaymentWebhookReplayServiceTest,PaymentProviderWebhookIngressServiceTest,PaymentProviderWebhookServiceTest test
```

Expected: all replay orchestration, order, invariant, and safe-value tests pass.

- [x] **Step 6: Commit transactional application processing**

```powershell
git add src/main/java/com/project/optrabidz/financial/application src/test/java/com/project/optrabidz/financial/application
git commit -m "feat(KAN-32): process claimed webhooks transactionally"
```

---

### Task 4: Empty acknowledgement and sanitized replay-collision audit

**Files:**

- Modify the controller, HTTP ingress, auditor port, audit adapter, and
  `SecurityAuditService` listed in the file map.
- Modify their five corresponding unit-test files listed in the file map.

**Interfaces:**

Add:

```java
void recordReplayCollision(String providerCode, String requestId);
```

The security action is exactly `PAYMENT_WEBHOOK_REPLAY_COLLISION`; object type
is `PAYMENT_PROVIDER`, object ID is the allowlisted configured provider or
`UNKNOWN`, outcome is `DENIED`, and details remain
`{"category":"PAYMENT_WEBHOOK"}`.

- [x] **Step 1: Write HTTP and audit RED tests**

Controller test:

```java
MvcResult result = mockMvc.perform(post(
                "/api/v1/payment-providers/UPI/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""))
        .andReturn();

assertThat(result.getResponse().getContentType()).isNull();
```

HTTP-ingress test: make application ingress throw
`PaymentWebhookReplayCollisionException`; verify only
`auditor.recordReplayCollision("UPI", REQUEST_ID)` executes, then verify the
same exception is rethrown.

Audit tests: capture the `AuditRecord` and assert action, provider, request ID,
fixed category, null IP/user-agent, and absence of event ID, hash, raw payload,
diagnostic code/message, signature, and exception data.

- [x] **Step 2: Run focused tests and confirm RED**

```powershell
.\mvnw.cmd -Dtest=PaymentProviderWebhookControllerTest,PaymentWebhookHttpIngressTest,AuditPaymentWebhookSecurityAuditorTest,SecurityAuditServiceTest test
```

Expected: tests fail because the route still returns a success envelope and no
collision audit operation exists.

- [x] **Step 3: Implement empty 204 and collision auditing**

Controller contract:

```java
@PostMapping(
        value = "/{providerCode}/webhooks",
        consumes = MediaType.APPLICATION_JSON_VALUE
)
@ResponseStatus(HttpStatus.NO_CONTENT)
public void handleProviderWebhook(
        @PathVariable String providerCode,
        HttpServletRequest request) {
    httpIngress.handle(providerCode, request);
}
```

Remove `ApiResponse`, `SuccessResponse`, `PaymentAttemptResponse`, and
`produces`. Change HTTP ingress to `void`. Catch
`PaymentWebhookReplayCollisionException` before the broader payload-invalid
catch, invoke `recordReplayCollision`, and rethrow.

Add `SecurityAuditService.recordPaymentWebhookReplayCollision` through the
existing bounded `saveWebhookSecurityEvent` helper. Do not pass an event ID,
hash, exception, reason, body, or header to any audit method.

- [x] **Step 4: Run focused tests and confirm GREEN**

```powershell
.\mvnw.cmd -Dtest=PaymentProviderWebhookControllerTest,PaymentWebhookHttpIngressTest,AuditPaymentWebhookSecurityAuditorTest,SecurityAuditServiceTest test
```

Expected: empty response and sanitized audit tests pass.

- [x] **Step 5: Commit the HTTP and security-audit boundary**

```powershell
git add src/main/java/com/project/optrabidz/financial/api src/main/java/com/project/optrabidz/financial/application/port/PaymentWebhookSecurityAuditor.java src/main/java/com/project/optrabidz/financial/infrastructure/audit src/main/java/com/project/optrabidz/audit/application/SecurityAuditService.java src/test/java/com/project/optrabidz/financial/api src/test/java/com/project/optrabidz/financial/infrastructure/audit src/test/java/com/project/optrabidz/audit/application/SecurityAuditServiceTest.java
git commit -m "feat(KAN-32): acknowledge replay-safe webhooks"
```

---

### Task 5: Real PostgreSQL HTTP, concurrency, rollback, outbox, and audit proof

**Files:**

- Modify:
  `src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookApiIT.java`

**Interfaces:**

- Exercise the production MockMvc filter chain, HMAC verifier, strict parser,
  replay service, PostgreSQL adapter, financial repositories, outbox writer,
  and shared RFC 9457 adapter.
- Use the existing test secret and signed timestamp/body helper.
- Build settlement and repayment payment references with
  `PostgresTestDataFixture`, then insert a `PAYMENT_PENDING` intent and
  `INITIATED` UPI attempt through bounded test helper SQL.

Use this concrete fixture shape for a settlement attempt; the repayment helper
uses `createRepaymentInstallmentReference`, `payment_purpose = 'REPAYMENT'`, a
null settlement ID, and the returned installment ID:

```java
private PaymentAttemptFixture createSettlementAttempt(String label) {
    Instant now = Instant.now();
    PostgresTestDataFixture.PaymentReference reference =
            new PostgresTestDataFixture(jdbcTemplate, now)
                    .createSettlementReference(label);
    Long intentId = jdbcTemplate.queryForObject("""
            insert into payment_intent (
                payment_purpose, settlement_id, repayment_installment_id,
                payer_account_id, payee_account_id, amount, currency_code,
                payment_state, idempotency_key, created_at, expires_at
            ) values (
                'SETTLEMENT', ?, null, ?, ?, 550000.00, 'INR',
                'PAYMENT_PENDING', ?, ?, ?
            ) returning payment_intent_id
            """, Long.class,
            reference.referenceId(), reference.payerAccountId(),
            reference.payeeAccountId(), "webhook-" + UUID.randomUUID(),
            Timestamp.from(now.minusSeconds(30)),
            Timestamp.from(now.plusSeconds(900)));
    Long attemptId = jdbcTemplate.queryForObject("""
            insert into payment_attempt (
                payment_intent_id, provider_code, method_type,
                provider_order_id, attempt_state, created_at, initiated_at,
                provider_payload
            ) values (?, 'UPI', 'UPI', ?, 'INITIATED', ?, ?, cast('{}' as jsonb))
            returning payment_attempt_id
            """, Long.class, intentId, "order-" + UUID.randomUUID(),
            Timestamp.from(now.minusSeconds(20)),
            Timestamp.from(now.minusSeconds(10)));
    return new PaymentAttemptFixture(intentId, attemptId, reference.referenceId());
}

private record PaymentAttemptFixture(
        long paymentIntentId,
        long paymentAttemptId,
        long referenceId
) {}
```

- [x] **Step 1: Replace the old success assumptions with empty-204 tests**

Add a helper returning an authenticated request:

```java
private MockHttpServletRequestBuilder authenticatedWebhook(
        String providerCode,
        String body,
        String requestId
) throws Exception {
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    return webhook(providerCode, body)
            .header("X-Request-Id", requestId)
            .header("X-Payment-Timestamp", timestamp)
            .header("X-Payment-Signature", signature(timestamp, body));
}
```

First-delivery test assertions:

```java
mockMvc.perform(authenticatedWebhook("UPI", body, requestId))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

assertThat(stateOfAttempt(paymentAttemptId)).isEqualTo("CONFIRMED");
assertThat(stateOfReplay("UPI", providerEventId)).isEqualTo("PROCESSED");
assertThat(outboxCountForPaymentIntent(paymentIntentId)).isEqualTo(1);
```

- [x] **Step 2: Add sequential and concurrent duplicate tests**

For sequential delivery, perform the identical signed semantic body twice and
assert both responses are empty 204, one replay row exists, and the relevant
outbox count remains one.

For concurrency, synchronize two `Callable<Integer>` requests with
`CountDownLatch`, execute them through a two-thread `ExecutorService`, and
assert:

```java
assertThat(statuses).containsExactlyInAnyOrder(204, 204);
assertThat(replayCount("UPI", providerEventId)).isEqualTo(1);
assertThat(outboxCountForPaymentIntent(paymentIntentId)).isEqualTo(1);
```

Declare
`@SpyBean private PaymentProviderWebhookService webhookService;` for this test
and verify `handle` is called once. Reset the spy with
`Mockito.reset(webhookService)` in `finally` so context reuse cannot leak
interactions.

- [x] **Step 3: Add collision and unexpected-state tests**

Process one valid body, then sign and send a body with the same provider event
ID but a different provider payment ID. Assert HTTP 400,
`PAYMENT_WEBHOOK_PAYLOAD_INVALID`, unchanged financial/outbox counts, and one
`PAYMENT_WEBHOOK_REPLAY_COLLISION` security record containing neither provider
event ID nor payload/hash/diagnostics.

Insert a committed `RECEIVED` row with a unique event ID, then send its
identical authenticated body. Assert sanitized HTTP 500, zero financial/outbox
change, and absence of the event ID and protected data from the response.

- [x] **Step 4: Add forced completion-failure rollback and retry test**

Declare `@SpyBean private PaymentWebhookReplayStore replayStore;`. Allow real
claim and lookup behavior, but make the first `markProcessed` call throw the fixed exception
`IllegalStateException("forced replay completion failure")`.

After the first request, assert sanitized HTTP 500 and:

```java
assertThat(stateOfAttempt(paymentAttemptId)).isEqualTo("INITIATED");
assertThat(stateOfIntent(paymentIntentId)).isEqualTo("PAYMENT_PENDING");
assertThat(replayCount("UPI", providerEventId)).isZero();
assertThat(outboxCountForPaymentIntent(paymentIntentId)).isZero();
```

Reset the spy and repeat the same authenticated body. Assert empty 204,
confirmed financial state, one `PROCESSED` replay row, and one outbox event.

- [x] **Step 5: Add provider-diagnostic separation test**

Use a repayment attempt and a `PAYMENT_FAILED` body containing
`UPI_DECLINED_PRIVATE` and `Provider diagnostic must remain protected`.
After HTTP 204, assert:

- replay JSONB contains the bounded normalized provider diagnostics;
- `payment_attempt.failure_code` is `PROVIDER_REPORTED_FAILURE`;
- `payment_attempt.failure_message` is
  `Payment provider reported that the payment failed`;
- `payment_intent`, repayment installment, and outbox payload contain only the
  safe business message;
- ordinary audit and security audit do not contain the protected diagnostics;
  and
- `payment_webhook_event.failure_message` is null.

- [x] **Step 6: Prove downstream audit derives once**

Inject `OutboxDispatcher`, call `dispatchPending()` until the tested outbox
event is processed, and query `audit_record` joined by that outbox `event_id`.
Assert one expected finance action exists. Invoke `AuditEventHandler.process`
again with the loaded outbox event and assert the count remains one, confirming
the existing application idempotency check backed by
`UNIQUE (event_id, action)`.

- [x] **Step 7: Run the API integration class and confirm GREEN**

```powershell
.\mvnw.cmd -Pintegration-tests -Dit.test=PaymentProviderWebhookApiIT verify
```

Expected: authentication rejection, strict parsing, first delivery, duplicate,
concurrency, collision, invariant, rollback/retry, protected diagnostics,
outbox, audit, and disclosure tests all pass.

- [x] **Step 8: Commit end-to-end proof**

```powershell
git add src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookApiIT.java
git commit -m "test(KAN-32): prove replay-safe webhook processing"
```

---

### Task 6: Architecture guards and complete verification

**Files:**

- Modify:
  `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Update after successful execution:
  `docs/error-handling/work-items/KAN-32-webhook-replay-protection/implementation-plan.md`

**Interfaces:**

- Application replay packages may depend on Java, financial application
  commands/errors/ports, neutral common errors, Spring stereotype/transaction
  annotations, and existing response DTOs only.
- Controller must not depend on fingerprint, replay store, Jackson, JPA, or
  JDBC types.
- Replay-store port must not depend on servlet, HTTP, Jackson, Spring Data,
  JDBC, or PostgreSQL packages.

- [x] **Step 1: Add architecture rules**

Add rules equivalent to:

```java
@ArchTest
static final ArchRule WEBHOOK_REPLAY_PORT_IS_INFRASTRUCTURE_NEUTRAL =
        noClasses()
                .that().resideInAPackage("..financial.application.port..")
                .and().haveSimpleName("PaymentWebhookReplayStore")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.servlet..",
                        "org.springframework.http..",
                        "org.springframework.jdbc..",
                        "org.springframework.data..",
                        "com.fasterxml.jackson..",
                        "org.postgresql.."
                );

@ArchTest
static final ArchRule WEBHOOK_CONTROLLER_DOES_NOT_OWN_REPLAY_POLICY =
        noClasses()
                .that().haveSimpleName("PaymentProviderWebhookController")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..financial.application.replay..",
                        "..financial.infrastructure.repository..",
                        "org.springframework.jdbc..",
                        "org.springframework.data.."
                );
```

Retain all existing architecture rules.

- [x] **Step 2: Run architecture and focused unit suites**

```powershell
.\mvnw.cmd -Dtest=ExceptionArchitectureTest,PaymentWebhookReplayFingerprintFactoryTest,PaymentWebhookReplayServiceTest,PaymentProviderWebhookIngressServiceTest,PaymentProviderWebhookServiceTest,PaymentProviderWebhookControllerTest,PaymentWebhookHttpIngressTest,AuditPaymentWebhookSecurityAuditorTest,SecurityAuditServiceTest test
```

Expected: all focused unit and architecture tests pass.

- [x] **Step 3: Run the complete unit suite**

```powershell
.\mvnw.cmd test
```

Expected: all unit and architecture tests pass with no failure or error.

- [x] **Step 4: Run the complete PostgreSQL integration suite**

```powershell
.\mvnw.cmd -Pintegration-tests verify
```

Expected: all Testcontainers integration tests pass with no failure or error.

- [x] **Step 5: Run disclosure and scope scans**

```powershell
rg -n "ApiResponse|SuccessResponse<PaymentAttemptResponse>|produces = MediaType.APPLICATION_JSON_VALUE" src/main/java/com/project/optrabidz/financial/api/PaymentProviderWebhookController.java
rg -n "rawBody|signature|headers|secret|providerEventId|payloadHash|failureMessage" src/main/java/com/project/optrabidz/financial/infrastructure/audit src/main/java/com/project/optrabidz/audit/application/SecurityAuditService.java
git diff 5c99b135d4b2b4b164eb926e7dfff8483c87a6a4 -- src/main/resources/db/migration/V1__baseline.sql pom.xml
git diff --check
```

Expected:

- the controller scan returns no match;
- audit production code contains no prohibited replay values except method
  declarations already approved for generic payload auditing;
- baseline migration and Maven dependency/version diff is empty; and
- `git diff --check` reports nothing.

- [x] **Step 6: Record execution evidence**

Mark completed plan checkboxes only after their commands pass. Add the final
unit/integration counts, Java distribution/version, PostgreSQL container
version, and tested commit SHA under a new `Execution evidence` section in this
file. Do not include workstation paths, usernames, secrets, tokens, process
IDs, heap dumps, or process/tool attribution.

- [x] **Step 7: Commit architecture and verification records**

```powershell
git add src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java docs/error-handling/work-items/KAN-32-webhook-replay-protection/implementation-plan.md
git commit -m "test(KAN-32): enforce webhook replay boundaries"
```

## Review and release gate

After all tasks pass:

1. push `feature/KAN-32-webhook-replay-protection`;
2. create a PR targeting `develop` with KAN-32 in the title;
3. attach test evidence and the rendered architecture diagram to Jira without
   temporary clipboard duplicates;
4. move KAN-32 to review;
5. wait for explicit PR approval;
6. merge only into `develop` after approval; and
7. never merge this story directly into `main`.

## Execution evidence

- Tested implementation commit: `6411d96fbe50cce91cdbfbf80c0b832309b9d264`.
- Complete Maven verification: `311` unit and architecture tests passed;
  `111` PostgreSQL integration tests passed; zero failures, errors, or skips.
- Focused replay HTTP integration verification: `8` tests passed.
- Runtime: Oracle JDK Java `21.0.11` (64-bit Server VM).
- Database: PostgreSQL `16.14` in Testcontainers, with Flyway baseline
  validation and migration verification.
- Disclosure scans found no obsolete response envelope/media declaration in
  the webhook controller and no protected replay values in the security-audit
  production paths.
- Scope comparison confirmed no changes to `V1__baseline.sql` or `pom.xml`.
- `git diff --check` completed without whitespace errors.
