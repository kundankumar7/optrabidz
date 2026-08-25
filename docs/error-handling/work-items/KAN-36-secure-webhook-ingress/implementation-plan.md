# KAN-36 — Secure Webhook Ingress Implementation Plan

**Status:** Implemented, verified, reviewed, and merged into `develop`.

**Goal:** Make payment-provider webhook configuration and ingress secure by
default while preserving the current successful financial behavior until
KAN-32 adds replay persistence and a minimal acknowledgement.

**Architecture:** A transport-only controller delegates bounded HTTP capture to
an ingress adapter. An application ingress service resolves and executes a
provider verifier over exact bytes before a dedicated strict parser creates the
normalized financial command. Typed neutral failures use the shared RFC 9457
adapter, and a financial audit port records only sanitized rejection metadata.

**Tech stack:** Java 21, Spring Boot 3.3.2, Spring MVC, Spring Security 6.3,
Jackson 2.x, Jakarta Validation, HMAC-SHA-256, RFC 9457 `ProblemDetail`, JUnit 5,
Mockito, MockMvc, AssertJ, ArchUnit, Testcontainers, PostgreSQL 16, Flyway,
Maven Wrapper, and GitHub Actions.

**Specification:** [`design.md`](design.md)

## Global constraints

- Work only on `feature/KAN-36-secure-webhook-ingress`, created from verified
  `develop` commit `d00cb1d9aff7ae2e510571a90f03928c5bab5820`.
- Pull requests target `develop`; do not modify or merge into `main`.
- Do not add, remove, or upgrade dependencies.
- Do not change Flyway, database tables, financial state rules, provider SDKs,
  user authentication, JWT/OAuth2, CSRF policy, or the current successful
  webhook response body.
- Keep the webhook session-public and CSRF-exempt; provider HMAC remains its
  independent machine-authentication boundary.
- Persistent replay detection and the minimal acknowledgement belong to
  KAN-32 and must not appear in this implementation.
- The exact raw body limit is 64 KiB. Provider code, timestamp header,
  signature header, provider identifiers, failure code, and failure message
  limits are respectively 32, 20, 80, 128, 64, and 512 characters.
- The local HMAC canonical form is ASCII epoch seconds, one `.` byte, then the
  exact body bytes; the default freshness tolerance is five minutes.
- Public pre-authentication failure is always
  `PAYMENT_WEBHOOK_REJECTED` 400. Authenticated schema/field failure is always
  `PAYMENT_WEBHOOK_PAYLOAD_INVALID` 400.
- Never expose or ordinarily audit secrets, signatures, raw bodies,
  unrestricted provider text, credentials, cookies, authorization headers,
  diagnostics, exception messages, causes, class names, SQL, or stack traces.
- Use focused RED → minimal GREEN → regression verification for every runtime
  change and make small Jira-keyed commits.
- Do not merge without review approval and exact-head CI success.

## File map

| Path | Responsibility |
|---|---|
| `src/main/resources/application*.properties` | Explicit profile behavior and environment-specific provider configuration. |
| `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/PaymentWebhookProperties.java` | Type-safe limits, freshness, provider secrets, and rotation settings. |
| `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/PaymentWebhookConfigurationPolicy.java` | Cross-field and environment safety validation at startup. |
| Local/sandbox provider classes | Require an explicit dev/test profile in addition to their feature flag. |
| `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java` | Two allowlisted public webhook descriptors. |
| `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhook*Exception.java` | Transport-neutral rejection and payload-invalid failures. |
| `src/main/java/com/project/optrabidz/financial/application/command/PaymentProviderWebhookEnvelope.java` | Immutable pre-authentication exact bytes and allowlisted protocol values. |
| `src/main/java/com/project/optrabidz/financial/api/PaymentWebhookHttpRequestReader.java` | Provider-code, content-length, bounded stream, and header extraction. |
| `src/main/java/com/project/optrabidz/financial/application/port/PaymentProviderWebhookSignatureVerifier.java` | Provider-verification port over the pre-authentication envelope. |
| `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/HmacPaymentProviderWebhookSignatureVerifier.java` | Timestamped HMAC, freshness, constant-time comparison, and secret rotation. |
| `src/main/java/com/project/optrabidz/financial/application/port/PaymentProviderWebhookEventParser.java` | Strict post-authentication parser port. |
| `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/StrictPaymentProviderWebhookEventParser.java` | Dedicated constrained Jackson reader and DTO validation. |
| `src/main/java/com/project/optrabidz/financial/application/PaymentProviderWebhookIngressService.java` | Verify → parse → financial use-case order. |
| `src/main/java/com/project/optrabidz/financial/api/PaymentWebhookHttpIngress.java` | Outer HTTP orchestration and exactly-once sanitized rejection auditing. |
| `src/main/java/com/project/optrabidz/financial/api/PaymentProviderWebhookController.java` | Route mapping and success rendering only. |
| `src/main/java/com/project/optrabidz/financial/application/port/PaymentWebhookSecurityAuditor.java` | Transport-neutral webhook security-audit port. |
| `src/main/java/com/project/optrabidz/financial/infrastructure/audit/AuditPaymentWebhookSecurityAuditor.java` | Adapter into the existing audit subsystem. |
| `src/main/java/com/project/optrabidz/audit/application/SecurityAuditService.java` | Best-effort bounded webhook security records. |
| Focused tests listed per task | Configuration, request limits, verifier, parser, ordering, disclosure, integration, and architecture coverage. |

---

## Task 1: Remove unsafe profile fallback and isolate development providers

**Files:**

- Modify: `src/main/resources/application.properties`
- Modify: `src/main/java/com/project/optrabidz/financial/api/LocalPaymentSimulationController.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/strategy/LocalPaymentStrategy.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/local/LocalPaymentProviderInitializer.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/sandbox/SandboxCardPaymentStrategy.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/sandbox/SandboxUpiPaymentStrategy.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/sandbox/SandboxPaymentProviderInitializer.java`
- Create: `src/test/java/com/project/optrabidz/financial/infrastructure/provider/PaymentProviderProfileBoundaryTest.java`

**Interfaces:**

- Consumes: existing feature flags
  `optrabidz.financial.local-provider.enabled` and
  `optrabidz.financial.sandbox-providers.enabled`.
- Produces: local/sandbox components that require both an enabled property and
  an active `dev` or `test` profile.

- [x] **Step 1: Write the failing profile-boundary test**

  Use `ApplicationContextRunner` with a minimal configuration importing each
  conditional component. Assert these exact cases:

  ```java
  assertThat(contextWithoutProfile)
          .doesNotHaveBean(LocalPaymentStrategy.class)
          .doesNotHaveBean(SandboxUpiPaymentStrategy.class);

  assertThat(contextWithProdProfileAndEnabledFlags)
          .doesNotHaveBean(LocalPaymentStrategy.class)
          .doesNotHaveBean(SandboxUpiPaymentStrategy.class);

  assertThat(contextWithDevProfileAndEnabledFlags)
          .hasSingleBean(LocalPaymentStrategy.class)
          .hasSingleBean(SandboxUpiPaymentStrategy.class);
  ```

  Read packaged `application.properties` in the same test and assert it does
  not contain `spring.profiles.active`.

- [x] **Step 2: Run the test and verify RED**

  ```powershell
  .\mvnw.cmd -Dtest=PaymentProviderProfileBoundaryTest test
  ```

  Expected: failure because an enabled property can currently create provider
  beans without a dev/test profile and the packaged file activates `dev`.

- [x] **Step 3: Apply the minimal profile boundary**

  Delete this property and its unsafe-default comment:

  ```properties
  spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
  ```

  Add the same explicit environment boundary to all six local/sandbox
  components while preserving their property condition:

  ```java
  @Profile({"dev", "test"})
  @ConditionalOnProperty(
          name = "optrabidz.financial.sandbox-providers.enabled",
          havingValue = "true"
  )
  ```

  Use the local-provider property on the local controller, strategy, and
  initializer; use the sandbox property on the three sandbox components.

- [x] **Step 4: Run focused tests and verify GREEN**

  ```powershell
  .\mvnw.cmd -Dtest=PaymentProviderProfileBoundaryTest test
  ```

  Expected: all profile matrix cases pass.

- [x] **Step 5: Commit the isolated change**

  ```powershell
  git add src/main/resources/application.properties src/main/java/com/project/optrabidz/financial src/test/java/com/project/optrabidz/financial/infrastructure/provider/PaymentProviderProfileBoundaryTest.java
  git commit -m "fix(KAN-36): isolate development payment providers"
  ```

## Task 2: Bind and validate webhook provider configuration

**Files:**

- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/PaymentWebhookProperties.java`
- Create: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/PaymentWebhookConfigurationPolicy.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/application-dev.properties`
- Modify: `src/main/resources/application-prod.properties`
- Modify: `src/test/resources/application-test.properties`
- Create: `src/test/java/com/project/optrabidz/financial/infrastructure/provider/webhook/PaymentWebhookConfigurationPolicyTest.java`

**Interfaces:**

- Consumes: Spring Boot `@ConfigurationProperties`, `DataSize`, `Duration`,
  `Environment`, and the explicit environment profiles from Task 1.
- Produces:
  `Optional<ProviderConfiguration> enabledProvider(String providerCode)`,
  `DataSize maxBodySize()`, and `Duration timestampTolerance()`.

- [x] **Step 1: Write the failing configuration-policy matrix**

  Cover these exact outcomes with `ApplicationContextRunner`:

  | Profile/configuration | Expected result |
  |---|---|
  | provider disabled, no secret | starts |
  | provider enabled, no active secret | startup failure |
  | provider enabled, active secret shorter than 32 UTF-8 bytes | startup failure |
  | previous secret present, no expiry | startup failure |
  | previous secret present, expired at startup | startup failure |
  | valid active and future-expiring previous secret | starts |
  | `prod` with a `dev-only-` or `test-only-` secret | startup failure |
  | `dev` with explicit development secret | starts |

  Verify exception messages identify the property/provider but never include a
  secret value.

- [x] **Step 2: Run the test and verify RED**

  ```powershell
  .\mvnw.cmd -Dtest=PaymentWebhookConfigurationPolicyTest test
  ```

  Expected: compilation failure because the structured provider configuration
  and startup policy do not exist.

- [x] **Step 3: Introduce the structured property contract**

  Implement the following shape under
  `optrabidz.financial.webhook`:

  ```java
  @Validated
  @ConfigurationProperties(prefix = "optrabidz.financial.webhook")
  public class PaymentWebhookProperties {
      private DataSize maxBodySize = DataSize.ofKilobytes(64);
      private Duration timestampTolerance = Duration.ofMinutes(5);
      private Map<String, ProviderConfiguration> providers = new HashMap<>();

      public Optional<ProviderConfiguration> enabledProvider(String providerCode) {
          ProviderConfiguration provider = providers.get(normalize(providerCode));
          return provider != null && provider.isEnabled()
                  ? Optional.of(provider)
                  : Optional.empty();
      }

      public DataSize getMaxBodySize() { return maxBodySize; }
      public void setMaxBodySize(DataSize value) { maxBodySize = value; }
      public Duration getTimestampTolerance() { return timestampTolerance; }
      public void setTimestampTolerance(Duration value) { timestampTolerance = value; }
      public Map<String, ProviderConfiguration> getProviders() {
          return Map.copyOf(providers);
      }
      public void setProviders(Map<String, ProviderConfiguration> value) {
          providers = normalizeProviders(value);
      }

      private static String normalize(String providerCode) {
          return providerCode == null ? "" : providerCode.strip().toUpperCase(Locale.ROOT);
      }

      private static Map<String, ProviderConfiguration> normalizeProviders(
              Map<String, ProviderConfiguration> value) {
          Map<String, ProviderConfiguration> normalized = new LinkedHashMap<>();
          if (value != null) {
              value.forEach((code, provider) ->
                      normalized.put(normalize(code), provider));
          }
          return normalized;
      }

      public static final class ProviderConfiguration {
          private boolean enabled;
          private String activeSecret;
          private String previousSecret;
          private Instant previousSecretValidUntil;

          public boolean isEnabled() { return enabled; }
          public void setEnabled(boolean value) { enabled = value; }
          public String getActiveSecret() { return activeSecret; }
          public void setActiveSecret(String value) { activeSecret = value; }
          public String getPreviousSecret() { return previousSecret; }
          public void setPreviousSecret(String value) { previousSecret = value; }
          public Instant getPreviousSecretValidUntil() {
              return previousSecretValidUntil;
          }
          public void setPreviousSecretValidUntil(Instant value) {
              previousSecretValidUntil = value;
          }
      }
  }
  ```

  `PaymentWebhookConfigurationPolicy` validates every enabled provider after
  binding. It requires at least 32 UTF-8 secret bytes, validates the
  previous-secret/expiry pair, rejects an already-expired previous secret at
  startup, and rejects development/test marker secrets outside dev/test.

- [x] **Step 4: Replace the flat secret properties**

  Base properties:

  ```properties
  optrabidz.financial.webhook.max-body-size=64KB
  optrabidz.financial.webhook.timestamp-tolerance=PT5M
  ```

  Development and test configure enabled UPI/CARD entries using external-value
  overrides and clearly non-production fallback values at least 32 bytes long:

  ```properties
  optrabidz.financial.webhook.providers.UPI.enabled=true
  optrabidz.financial.webhook.providers.UPI.active-secret=${OPTRABIDZ_UPI_WEBHOOK_SECRET:dev-only-upi-webhook-secret-material-001}
  optrabidz.financial.webhook.providers.CARD.enabled=true
  optrabidz.financial.webhook.providers.CARD.active-secret=${OPTRABIDZ_CARD_WEBHOOK_SECRET:dev-only-card-webhook-secret-material-001}
  ```

  Production contains no fallback secret and keeps all repository-defined
  providers disabled. A deployment enables a provider and supplies its secret
  externally.

- [x] **Step 5: Run focused configuration tests**

  ```powershell
  .\mvnw.cmd "-Dtest=PaymentWebhookConfigurationPolicyTest,PaymentProviderProfileBoundaryTest" test
  ```

  Expected: safe matrices pass and failure messages contain no configured
  secret text.

- [x] **Step 6: Commit the configuration contract**

  ```powershell
  git add src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook src/main/resources/application*.properties src/test/resources/application-test.properties src/test/java/com/project/optrabidz/financial/infrastructure/provider/webhook/PaymentWebhookConfigurationPolicyTest.java
  git commit -m "feat(KAN-36): validate webhook provider configuration"
  ```

## Task 3: Add neutral webhook errors and bounded exact HTTP capture

**Files:**

- Create: `src/main/java/com/project/optrabidz/financial/application/error/FinancialErrors.java`
- Create: `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookRejectedException.java`
- Create: `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookPayloadInvalidException.java`
- Create: `src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhookRejectionReason.java`
- Create: `src/main/java/com/project/optrabidz/financial/application/command/PaymentProviderWebhookEnvelope.java`
- Create: `src/main/java/com/project/optrabidz/financial/api/PaymentWebhookHttpRequestReader.java`
- Create: `src/test/java/com/project/optrabidz/financial/application/FinancialWebhookErrorContractTest.java`
- Create: `src/test/java/com/project/optrabidz/financial/api/PaymentWebhookHttpRequestReaderTest.java`

**Interfaces:**

- Consumes: limits from `PaymentWebhookProperties` and servlet request streams.
- Produces:
  `PaymentProviderWebhookEnvelope read(String providerCode,
  HttpServletRequest request)` containing normalized provider code, defensive
  exact body bytes, one timestamp, and one signature.

- [x] **Step 1: Freeze the public descriptor contract with failing tests**

  Assert exact descriptors:

  ```java
  assertThat(FinancialErrors.PAYMENT_WEBHOOK_REJECTED)
          .isEqualTo(new ErrorDescriptor(
                  "PAYMENT_WEBHOOK_REJECTED",
                  ErrorCategory.VALIDATION,
                  "The webhook request was rejected"));

  assertThat(FinancialErrors.PAYMENT_WEBHOOK_PAYLOAD_INVALID)
          .isEqualTo(new ErrorDescriptor(
                  "PAYMENT_WEBHOOK_PAYLOAD_INVALID",
                  ErrorCategory.VALIDATION,
                  "The webhook payload is invalid"));
  ```

  Verify both typed exceptions extend `ApplicationException` and expose no
  dynamic public detail.

- [x] **Step 2: Write bounded-reader RED tests**

  Cover valid exact non-ASCII bytes, `Content-Length` above 65,536, chunked
  body of 65,537 bytes, empty body, invalid/overlong provider code, missing,
  repeated, and overlong timestamp/signature headers. For every rejection,
  assert `PAYMENT_WEBHOOK_REJECTED` and confirm no unrestricted headers are
  present in the envelope.

- [x] **Step 3: Run focused tests and verify RED**

  ```powershell
  .\mvnw.cmd "-Dtest=FinancialWebhookErrorContractTest,PaymentWebhookHttpRequestReaderTest" test
  ```

  Expected: compilation failure because the new contracts do not exist.

- [x] **Step 4: Implement immutable envelope and bounded reading**

  The envelope must defensively copy bytes on construction and access:

  ```java
  public record PaymentProviderWebhookEnvelope(
          String providerCode,
          byte[] rawBody,
          String timestamp,
          String signature
  ) {
      public PaymentProviderWebhookEnvelope {
          rawBody = rawBody.clone();
      }

      @Override
      public byte[] rawBody() {
          return rawBody.clone();
      }
  }
  ```

  The reader checks declared length, calls `readNBytes(maxBytes + 1)`, rejects
  an extra byte, reads only `X-Payment-Timestamp` and
  `X-Payment-Signature`, and requires exactly one bounded value for each.
  It does not decode JSON or verify HMAC.

- [x] **Step 5: Run focused tests and verify GREEN**

  ```powershell
  .\mvnw.cmd "-Dtest=FinancialWebhookErrorContractTest,PaymentWebhookHttpRequestReaderTest" test
  ```

- [x] **Step 6: Commit the ingress primitives**

  ```powershell
  git add src/main/java/com/project/optrabidz/financial/application/error src/main/java/com/project/optrabidz/financial/application/exception/PaymentWebhook* src/main/java/com/project/optrabidz/financial/application/command/PaymentProviderWebhookEnvelope.java src/main/java/com/project/optrabidz/financial/api/PaymentWebhookHttpRequestReader.java src/test/java/com/project/optrabidz/financial/application/FinancialWebhookErrorContractTest.java src/test/java/com/project/optrabidz/financial/api/PaymentWebhookHttpRequestReaderTest.java
  git commit -m "feat(KAN-36): bound exact webhook request capture"
  ```

## Task 4: Authenticate timestamped exact bytes with safe secret rotation

**Files:**

- Modify: `src/main/java/com/project/optrabidz/financial/application/port/PaymentProviderWebhookSignatureVerifier.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/port/PaymentProviderWebhookSignatureVerifierRegistry.java`
- Modify: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/HmacPaymentProviderWebhookSignatureVerifier.java`
- Modify: `src/test/java/com/project/optrabidz/financial/infrastructure/provider/webhook/HmacPaymentProviderWebhookSignatureVerifierTest.java`
- Create: `src/test/java/com/project/optrabidz/financial/application/port/PaymentProviderWebhookSignatureVerifierRegistryTest.java`

**Interfaces:**

- Consumes: `PaymentProviderWebhookEnvelope` and enabled provider configuration.
- Produces: `void verify(PaymentProviderWebhookEnvelope envelope)`; success
  means the exact body and timestamp are authenticated.

- [x] **Step 1: Replace legacy verifier tests with the approved protocol matrix**

  Use a fixed clock at `2026-08-24T12:00:00Z`. Test:

  - timestamp exactly now and correct active-secret signature passes;
  - one changed UTF-8 body byte fails;
  - missing, repeated, malformed, stale, and future timestamps fail;
  - wrong, non-hex, wrong-length, or wrong-prefix signatures fail;
  - valid previous secret before expiry passes;
  - previous secret at/after expiry fails;
  - unknown/disabled provider registry resolution fails identically; and
  - every failure is `PaymentWebhookRejectedException` with the same public
    descriptor.

- [x] **Step 2: Run verifier tests and verify RED**

  ```powershell
  .\mvnw.cmd "-Dtest=HmacPaymentProviderWebhookSignatureVerifierTest,PaymentProviderWebhookSignatureVerifierRegistryTest" test
  ```

- [x] **Step 3: Implement timestamped HMAC verification**

  Change the port to consume the envelope. Build canonical bytes without
  converting the body to text:

  ```java
  private byte[] canonicalBytes(String timestamp, byte[] body) {
      byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.US_ASCII);
      ByteBuffer canonical = ByteBuffer.allocate(prefix.length + body.length);
      canonical.put(prefix);
      canonical.put(body);
      return canonical.array();
  }
  ```

  Parse epoch seconds strictly, require it between
  `now.minus(tolerance)` and `now.plus(tolerance)`, decode exactly 32 signature
  bytes after `sha256=`, calculate active and eligible previous candidates,
  and compare with `MessageDigest.isEqual`. Evaluate both configured candidate
  comparisons before combining them with non-short-circuit boolean OR.

  Inject a fixed `Clock` through a package-private test constructor while the
  Spring constructor uses `Clock.systemUTC()`.

- [x] **Step 4: Make registry failure uniform**

  `resolve(providerCode)` must throw
  `PaymentWebhookRejectedException(PROVIDER_UNAVAILABLE)` rather than
  `UnsupportedPaymentMethodException`. The public descriptor remains identical
  to other authentication failures.

- [x] **Step 5: Run verifier and configuration tests**

  ```powershell
  .\mvnw.cmd "-Dtest=HmacPaymentProviderWebhookSignatureVerifierTest,PaymentProviderWebhookSignatureVerifierRegistryTest,PaymentWebhookConfigurationPolicyTest" test
  ```

- [x] **Step 6: Commit the provider verifier**

  ```powershell
  git add src/main/java/com/project/optrabidz/financial/application/port src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/HmacPaymentProviderWebhookSignatureVerifier.java src/test/java/com/project/optrabidz/financial/application/port src/test/java/com/project/optrabidz/financial/infrastructure/provider/webhook/HmacPaymentProviderWebhookSignatureVerifierTest.java
  git commit -m "feat(KAN-36): authenticate timestamped webhook bytes"
  ```

## Task 5: Parse a strict event only after authentication

**Files:**

- Create: `src/main/java/com/project/optrabidz/financial/application/port/PaymentProviderWebhookEventParser.java`
- Create: `src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/StrictPaymentProviderWebhookEventParser.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/dto/request/PaymentProviderWebhookRequest.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/command/PaymentProviderWebhookCommand.java`
- Create: `src/test/java/com/project/optrabidz/financial/infrastructure/provider/webhook/StrictPaymentProviderWebhookEventParserTest.java`

**Interfaces:**

- Consumes: authenticated exact body bytes and normalized provider code.
- Produces:
  `PaymentProviderWebhookCommand parse(String providerCode, byte[] rawBody)`
  containing only normalized business fields; raw bytes and HTTP headers are
  absent.

- [x] **Step 1: Write the strict-parser RED matrix**

  Assert valid confirmed and failed events parse. Assert these authenticated
  payloads throw `PaymentWebhookPayloadInvalidException`: duplicate keys,
  unknown keys, trailing JSON, invalid types, payment-attempt numeric overflow,
  unsupported event type, nesting beyond 8, JSON string beyond 512, absent or
  non-positive payment-attempt ID, absent provider-event ID, and field limits
  above 128/64/512.

- [x] **Step 2: Run the parser test and verify RED**

  ```powershell
  .\mvnw.cmd -Dtest=StrictPaymentProviderWebhookEventParserTest test
  ```

- [x] **Step 3: Build a dedicated constrained reader**

  Use a private webhook-only mapper rather than mutating the application-wide
  mapper:

  ```java
  JsonFactory factory = JsonFactory.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .streamReadConstraints(StreamReadConstraints.builder()
                  .maxNestingDepth(8)
                  .maxStringLength(512)
                  .maxNumberLength(19)
                  .build())
          .build();

  ObjectReader reader = JsonMapper.builder(factory)
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build()
          .readerFor(PaymentProviderWebhookRequest.class);
  ```

  Apply Jakarta constraints to the request record, run `Validator.validate`,
  perform event-dependent required-field checks, and translate all expected
  parse/validation failures to `PaymentWebhookPayloadInvalidException` without
  embedding payload values.

- [x] **Step 4: Remove transport data from the financial command**

  Delete `rawPayload` and `headers` from `PaymentProviderWebhookCommand` and
  change the DTO conversion to:

  ```java
  public PaymentProviderWebhookCommand toCommand(String providerCode) {
      return new PaymentProviderWebhookCommand(
              providerCode,
              eventType,
              paymentAttemptId,
              providerPaymentId,
              failureCode,
              failureMessage,
              providerEventId
      );
  }
  ```

- [x] **Step 5: Run strict-parser tests and compile impacted tests**

  ```powershell
  .\mvnw.cmd -Dtest=StrictPaymentProviderWebhookEventParserTest test
  .\mvnw.cmd -DskipTests compile
  ```

- [x] **Step 6: Commit the parser boundary**

  ```powershell
  git add src/main/java/com/project/optrabidz/financial/application/port/PaymentProviderWebhookEventParser.java src/main/java/com/project/optrabidz/financial/infrastructure/provider/webhook/StrictPaymentProviderWebhookEventParser.java src/main/java/com/project/optrabidz/financial/application/dto/request/PaymentProviderWebhookRequest.java src/main/java/com/project/optrabidz/financial/application/command/PaymentProviderWebhookCommand.java src/test/java/com/project/optrabidz/financial/infrastructure/provider/webhook/StrictPaymentProviderWebhookEventParserTest.java
  git commit -m "feat(KAN-36): parse authenticated webhook events strictly"
  ```

## Task 6: Enforce verify-before-parse orchestration and thin controller

**Files:**

- Create: `src/main/java/com/project/optrabidz/financial/application/PaymentProviderWebhookIngressService.java`
- Create: `src/main/java/com/project/optrabidz/financial/application/port/PaymentWebhookSecurityAuditor.java`
- Create: `src/main/java/com/project/optrabidz/financial/api/PaymentWebhookHttpIngress.java`
- Modify: `src/main/java/com/project/optrabidz/financial/application/PaymentProviderWebhookService.java`
- Modify: `src/main/java/com/project/optrabidz/financial/api/PaymentProviderWebhookController.java`
- Modify: `src/test/java/com/project/optrabidz/financial/application/PaymentProviderWebhookServiceTest.java`
- Create: `src/test/java/com/project/optrabidz/financial/application/PaymentProviderWebhookIngressServiceTest.java`
- Modify: `src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookControllerTest.java`
- Create: `src/test/java/com/project/optrabidz/financial/api/PaymentWebhookHttpIngressTest.java`

**Interfaces:**

- Consumes: request reader, verifier registry, strict parser, existing financial
  webhook service, and audit port.
- Produces:
  `PaymentAttemptResponse handle(PaymentProviderWebhookEnvelope envelope)` in
  the application ingress service and
  `PaymentAttemptResponse handle(String providerCode, HttpServletRequest
  request)` at the outer HTTP ingress.

- [x] **Step 1: Write the orchestration RED tests**

  Verify exact call order for a valid envelope:

  ```java
  InOrder order = inOrder(verifier, parser, webhookService);
  order.verify(verifier).verify(envelope);
  order.verify(parser).parse("UPI", envelope.rawBody());
  order.verify(webhookService).handle(command);
  ```

  When provider resolution or verification fails, assert the parser and
  financial service have zero interactions. When parsing fails, assert the
  financial service has zero interactions.

- [x] **Step 2: Write HTTP facade and thin-controller RED tests**

  Verify the facade audits and rethrows one rejection for reader, verifier, and
  parser failures. Verify the controller delegates once and neither depends on
  `ObjectMapper` nor accesses signature headers. Preserve the current
  `SuccessResponse<PaymentAttemptResponse>` for valid processing.

- [x] **Step 3: Run focused tests and verify RED**

  ```powershell
  .\mvnw.cmd "-Dtest=PaymentProviderWebhookIngressServiceTest,PaymentWebhookHttpIngressTest,PaymentProviderWebhookControllerTest,PaymentProviderWebhookServiceTest" test
  ```

- [x] **Step 4: Implement the application order**

  ```java
  public PaymentAttemptResponse handle(PaymentProviderWebhookEnvelope envelope) {
      PaymentProviderWebhookSignatureVerifier verifier =
              signatureVerifierRegistry.resolve(envelope.providerCode());
      verifier.verify(envelope);
      PaymentProviderWebhookCommand command = eventParser.parse(
              envelope.providerCode(),
              envelope.rawBody()
      );
      return webhookService.handle(command);
  }
  ```

  Remove verifier registry and verification from
  `PaymentProviderWebhookService`; it now handles only an authenticated command
  and financial branching.

- [x] **Step 5: Implement outer auditing and controller delegation**

  `PaymentWebhookHttpIngress` catches only
  `PaymentWebhookRejectedException` and
  `PaymentWebhookPayloadInvalidException`, calls the corresponding
  `PaymentWebhookSecurityAuditor` method with safe provider code and request
  ID, then rethrows. Unexpected failures keep the generic 500 path.

  The controller becomes transport-only:

  ```java
  public SuccessResponse<PaymentAttemptResponse> handleProviderWebhook(
          @PathVariable String providerCode,
          HttpServletRequest request) {
      return ApiResponse.success(httpIngress.handle(providerCode, request), request);
  }
  ```

- [x] **Step 6: Run focused tests and verify GREEN**

  ```powershell
  .\mvnw.cmd "-Dtest=PaymentProviderWebhookIngressServiceTest,PaymentWebhookHttpIngressTest,PaymentProviderWebhookControllerTest,PaymentProviderWebhookServiceTest" test
  ```

- [x] **Step 7: Commit the separated orchestration**

  ```powershell
  git add src/main/java/com/project/optrabidz/financial/application src/main/java/com/project/optrabidz/financial/api src/test/java/com/project/optrabidz/financial/application src/test/java/com/project/optrabidz/financial/api
  git commit -m "refactor(KAN-36): separate webhook ingress responsibilities"
  ```

## Task 7: Add sanitized audit and public disclosure integration coverage

**Files:**

- Create: `src/main/java/com/project/optrabidz/financial/infrastructure/audit/AuditPaymentWebhookSecurityAuditor.java`
- Modify: `src/main/java/com/project/optrabidz/audit/application/SecurityAuditService.java`
- Create: `src/test/java/com/project/optrabidz/financial/infrastructure/audit/AuditPaymentWebhookSecurityAuditorTest.java`
- Modify: `src/test/java/com/project/optrabidz/audit/application/SecurityAuditServiceTest.java`
- Create: `src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookApiIT.java`
- Modify: `src/test/java/com/project/optrabidz/security/api/SecurityApiIT.java`

**Interfaces:**

- Consumes: `PaymentWebhookSecurityAuditor`, `SecurityAuditService`, shared
  Problem Details rendering, real Spring Security chain, and test-profile HMAC
  configuration.
- Produces: best-effort bounded audit records and exact public 400 contracts.

- [x] **Step 1: Write audit RED tests**

  Assert rejected and payload-invalid actions persist only:

  ```json
  {"category":"PAYMENT_WEBHOOK","outcome":"DENIED"}
  ```

  The record may contain the configured normalized provider code and request
  ID. Assert it does not contain supplied signature, timestamp, raw payload,
  unknown provider text, secret, diagnostic reason, cookie, authorization
  value, exception message, cause, or stack trace. Simulate audit persistence
  failure and assert the adapter does not replace the original public failure.

- [x] **Step 2: Write real-boundary RED integration tests**

  For unknown provider, missing signature, malformed signature, stale
  timestamp, altered body, and malformed unauthenticated input, assert the
  exact same response:

  ```java
  .andExpect(status().isBadRequest())
  .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
  .andExpect(jsonPath("$.code").value("PAYMENT_WEBHOOK_REJECTED"))
  .andExpect(jsonPath("$.detail").value("The webhook request was rejected"));
  ```

  Sign duplicate-key, unknown-field, and invalid-type payloads correctly and
  assert `PAYMENT_WEBHOOK_PAYLOAD_INVALID` 400. Verify no response contains any
  input signature, secret marker, raw body fragment, diagnostic code, class
  name, cause, or stack trace.

- [x] **Step 3: Run focused tests and verify RED**

  ```powershell
  .\mvnw.cmd "-Dtest=AuditPaymentWebhookSecurityAuditorTest,SecurityAuditServiceTest" test
  .\mvnw.cmd -Pintegration-tests -DskipITs=false "-Dit.test=PaymentProviderWebhookApiIT,SecurityApiIT" verify
  ```

  Expected: the unit tests fail because webhook audit methods do not exist;
  integration assertions fail against the legacy 403/dynamic-message contract.

- [x] **Step 4: Implement the audit adapter**

  `AuditPaymentWebhookSecurityAuditor` converts an unconfigured or invalid
  provider path to `UNKNOWN` and delegates two bounded actions:

  ```java
  securityAuditService.recordPaymentWebhookRejected(safeProviderCode, requestId);
  securityAuditService.recordPaymentWebhookPayloadInvalid(safeProviderCode, requestId);
  ```

  `SecurityAuditService` uses its existing best-effort `saveSafely` policy and
  fixed details. It never receives signature, body, headers, secret, or
  diagnostic reason.

- [x] **Step 5: Update the legacy webhook security assertion**

  Replace the existing `SecurityApiIT` expectation of legacy 403
  `AUTHORIZATION_FAILED` and dynamic message with uniform RFC 9457 400
  `PAYMENT_WEBHOOK_REJECTED`. Keep the assertion that browser session and CSRF
  are not required.

- [x] **Step 6: Run focused audit and integration tests**

  ```powershell
  .\mvnw.cmd "-Dtest=AuditPaymentWebhookSecurityAuditorTest,SecurityAuditServiceTest" test
  .\mvnw.cmd -Pintegration-tests -DskipITs=false "-Dit.test=PaymentProviderWebhookApiIT,SecurityApiIT" verify
  ```

- [x] **Step 7: Commit audit and disclosure controls**

  ```powershell
  git add src/main/java/com/project/optrabidz/financial/infrastructure/audit src/main/java/com/project/optrabidz/audit/application/SecurityAuditService.java src/test/java/com/project/optrabidz/financial/infrastructure/audit src/test/java/com/project/optrabidz/audit/application/SecurityAuditServiceTest.java src/test/java/com/project/optrabidz/financial/api/PaymentProviderWebhookApiIT.java src/test/java/com/project/optrabidz/security/api/SecurityApiIT.java
  git commit -m "feat(KAN-36): audit webhook rejection safely"
  ```

## Task 8: Enforce architecture, run complete verification, and prepare review

**Files:**

- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify: `docs/error-handling/work-items/KAN-36-secure-webhook-ingress/implementation-plan.md`
- Modify: `docs/error-handling/README.md`

**Interfaces:**

- Consumes: all preceding runtime and test deliverables.
- Produces: architecture guards, recorded evidence, and a reviewable PR to
  `develop`.

- [x] **Step 1: Add architecture guards**

  Add rules proving:

  ```java
  noClasses()
          .that().haveSimpleName("PaymentProviderWebhookController")
          .should().dependOnClassesThat().resideInAnyPackage(
                  "com.fasterxml.jackson..",
                  "..financial.application.port.."
          );
  ```

  Also prove `PaymentProviderWebhookCommand` has no dependency on
  `jakarta.servlet`, Spring Web, or HTTP header types, and the two new webhook
  exceptions do not depend on `..common.api..`.

- [x] **Step 2: Run architecture and complete unit verification**

  ```powershell
  .\mvnw.cmd test
  ```

  Expected: all unit, configuration, verifier, parser, controller, audit, and
  ArchUnit tests pass.

- [x] **Step 3: Run complete PostgreSQL integration verification**

  Confirm Docker Engine is running, then execute:

  ```powershell
  .\mvnw.cmd -Pintegration-tests -DskipITs=false verify
  ```

  Expected: Testcontainers PostgreSQL starts, Flyway V1 validates, and every
  integration test passes without schema changes.

- [x] **Step 4: Run repository and disclosure guards**

  ```powershell
  git diff --check
  rg -n "spring\.profiles\.active=.*dev|local-upi-webhook-secret|local-card-webhook-secret" src/main/resources
  rg -n "rawPayload|Map<String, String> headers|ObjectMapper" src/main/java/com/project/optrabidz/financial/application src/main/java/com/project/optrabidz/financial/api/PaymentProviderWebhookController.java
  git status --short --branch
  ```

  Expected: whitespace check passes; unsafe legacy secret/profile patterns and
  transport data are absent; only intentional reviewed changes remain.

- [x] **Step 5: Update documentation evidence and commit**

  Record exact test counts, commands, commit SHA, and any non-blocking warning
  in this plan. Update its status to `Implemented and verified` and ensure the
  KAN-36 README row links both design and implementation plan.

  ```powershell
  git add docs/error-handling/work-items/KAN-36-secure-webhook-ingress docs/error-handling/README.md src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java
  git commit -m "test(KAN-36): verify secure webhook ingress"
  ```

### Local verification evidence

- `./mvnw.cmd test` — 295 tests passed; 0 failures, 0 errors, 0 skipped.
- `./mvnw.cmd -Pintegration-tests -DskipITs=false verify` — 99 PostgreSQL 16/Testcontainers integration tests passed; Flyway V1 validated; 0 failures, 0 errors, 0 skipped.
- Focused webhook suites covered configuration/profile isolation, bounded exact-byte capture, timestamped HMAC and rotation, strict parsing, orchestration order, sanitized audit behavior, uniform disclosure, architecture rules, and the existing financial success flow.
- Runtime implementation commits: `ecb8a65`, `0343593`, `b88ef7c`, `904da79`, `0a7dfcb`, `3768a9f`, and `4726c76`.
- Review pull request: [#29 — KAN-36: Secure payment webhook ingress](https://github.com/kundankumar7/optrabidz/pull/29), targeting `develop`.
- Non-blocking build warnings remain limited to existing Byte Buddy dynamic-agent notices and an unreferenced Logback `FILE` appender; neither affected test outcomes.

- [x] **Step 6: Push and create the review PR**

  ```powershell
  git push -u origin feature/KAN-36-secure-webhook-ingress
  gh pr create --base develop --head feature/KAN-36-secure-webhook-ingress --title "KAN-36: Secure payment webhook ingress" --body "## Summary
  - make financial runtime configuration safe by default
  - authenticate bounded exact webhook bytes before strict parsing
  - add uniform Problem Details and sanitized security auditing

  ## Verification
  - focused configuration, ingress, verifier, parser, audit, and disclosure tests
  - complete Maven unit and architecture suite
  - complete Testcontainers PostgreSQL integration suite

  ## Scope boundary
  KAN-32 owns replay persistence and the minimal acknowledgement. No database, Flyway, financial business-rule, user-authentication, JWT/OAuth2, or provider-SDK change is included.

  Jira: KAN-36"
  ```

- [ ] **Step 7: Wait for exact-head CI and review approval**

  Confirm all required GitHub checks pass for the PR head SHA. Move KAN-36 to
  In Review and add the PR link plus verification summary in Jira. Do not merge
  until review approval is explicitly recorded.
