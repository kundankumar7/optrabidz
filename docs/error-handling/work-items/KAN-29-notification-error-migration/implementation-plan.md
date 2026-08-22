# KAN-29 Notification Error Migration Implementation Plan

> **For implementers:** Execute this plan sequentially. Preserve every review
> gate, use the documented RED → GREEN cycles, and stop before merge until the
> pull request receives explicit approval.

**Goal:** Replace notification-module legacy API exceptions with two
notification-owned neutral errors while preserving ownership privacy and all
existing notification behavior.

**Architecture:** `NotificationErrors` owns two fixed public descriptors.
Typed `ApplicationException` subclasses carry protected diagnostics, conditional
account-scoped SQL remains the ownership boundary, Spring Security continues
handling anonymous access, and the existing RFC 9457 adapter renders responses.

**Tech Stack:** Java 21 (Temurin), Spring Boot 3.3.2, Spring Security, Spring JDBC,
JUnit 5, MockMvc, AssertJ, ArchUnit, Maven Failsafe, Testcontainers, PostgreSQL 16

**Spec:** `docs/error-handling/work-items/KAN-29-notification-error-migration/design.md`

## Global Constraints

- Work only on `feature/KAN-29-notification-error-migration`, based on the
  reviewed `origin/develop` commit containing specification commit
  `8aef4469729f6823273a191af93ee2b1b977684b`.
- Target `develop`; do not modify or merge into `main`.
- Keep KAN-29 assigned to Kumar Kundan and under parent KAN-16.
- Keep KAN-29 **To Do** until inline execution is approved; then move it to
  **In Progress**. Move it to **In Review** only after the pull request opens.
- Do not change Spring Security matchers, session policy, CSRF, JWT, OAuth2, or
  role policy.
- Do not change Flyway V1, database schema, dependencies, runtime properties,
  CI, outbox, delivery, provider, retry, worker, audit, or logging behavior.
- Do not introduce Kafka, AOP, caching, or speculative notification ports.
- Preserve the existing anonymous `AUTHENTICATION_REQUIRED` 401 response.
- Preserve one conditional SQL mutation containing resource ID and account ID;
  do not add a preceding existence query.
- Treat only a zero-row conditional mutation as an expected not-found failure.
  Do not translate unrelated SQL or runtime failures.
- Never expose account, recipient, notification, subscription, or event IDs;
  endpoints, public keys, auth secrets, ownership facts, raw messages, stack
  traces, or diagnostic codes in Problem Details.
- Real-port HTTP smoke tests remain a later dedicated KAN-16 story.
- Use `apply_patch` or an IDE for file edits. Do not rewrite production files
  through shell redirection.
- Each production slice begins with a failing test, ends with focused GREEN
  evidence, receives a Jira comment, and is committed separately.

---

## File map

| Path | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/notification/application/error/NotificationErrors.java` | Owns the two stable public notification descriptors. |
| `src/main/java/com/project/optrabidz/notification/application/exception/NotificationNotFoundException.java` | Carries notification-recipient lookup diagnostics using the neutral contract. |
| `src/main/java/com/project/optrabidz/notification/application/exception/NotificationSubscriptionNotFoundException.java` | Carries subscription lookup diagnostics using the neutral contract. |
| `src/main/java/com/project/optrabidz/notification/application/exception/NotificationAccessDeniedException.java` | Delete; the class is unused and ownership remains hidden behind 404. |
| `src/main/java/com/project/optrabidz/notification/application/NotificationService.java` | Selects the correct typed failure after a zero-row conditional mutation. |
| `src/main/java/com/project/optrabidz/notification/api/NotificationController.java` | Delegates authenticated calls without constructing legacy authentication errors. |
| `src/test/java/com/project/optrabidz/notification/application/NotificationErrorContractTest.java` | Verifies descriptors, typed failures, diagnostic codes, and disclosure separation. |
| `src/test/java/com/project/optrabidz/notification/api/NotificationApiIT.java` | Verifies PostgreSQL-backed ownership privacy, Problem Details, unchanged successes, and anonymous 401 behavior. |
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Permanently prohibits notification dependencies on the legacy exception package. |
| `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f` | Removes exactly six obsolete notification frozen violations. |
| `docs/error-handling/README.md` | Links the approved design and this implementation plan. |
| `docs/error-handling/work-items/KAN-29-notification-error-migration/implementation-plan.md` | Tracks execution evidence and review state. |

---

## Task 0: Verify the execution gate

**Files:** None

**Interfaces:**
- Consumes: approved specification commit
  `8aef4469729f6823273a191af93ee2b1b977684b` and user inline-execution approval.
- Produces: a clean, synchronized feature branch and Jira **In Progress** state.

- [ ] **Step 1: Confirm the explicit start gate**

Do not execute this task until the user states that the KAN-29 implementation
plan and inline execution are approved.

- [ ] **Step 2: Verify branch, ancestry, remote head, and clean state**

```powershell
git fetch origin
$branch = git branch --show-current
$head = git rev-parse HEAD
$remoteHead = git rev-parse origin/feature/KAN-29-notification-error-migration
$base = git merge-base HEAD origin/develop
$mainBefore = git rev-parse origin/main

if ($branch -ne 'feature/KAN-29-notification-error-migration') {
    throw "Unexpected branch: $branch"
}
if ($head -ne $remoteHead) {
    throw 'Local and remote feature heads differ'
}
if ($base -ne (git rev-parse origin/develop)) {
    throw 'Feature branch is not based on current origin/develop'
}
if (git status --porcelain) {
    throw 'Worktree must be clean before implementation'
}
git merge-base --is-ancestor 8aef4469729f6823273a191af93ee2b1b977684b HEAD
if ($LASTEXITCODE -ne 0) {
    throw 'Approved KAN-29 specification is missing'
}
"origin/main checkpoint: $mainBefore"
```

Expected: every guard passes and `origin/main` equals the preserved release
head at the start of execution.

- [ ] **Step 3: Synchronize Jira**

Move KAN-29 from **To Do** to **In Progress** and add a comment containing the
approved plan commit, exact branch head, exact `origin/develop` merge base, and
`origin/main` checkpoint. State explicitly that no merge is authorized.

---

## Task 1: Add the notification descriptor catalogue

**Files:**
- Create: `src/main/java/com/project/optrabidz/notification/application/error/NotificationErrors.java`
- Create: `src/test/java/com/project/optrabidz/notification/application/NotificationErrorContractTest.java`

**Interfaces:**
- Consumes: `ErrorDescriptor(String, ErrorCategory, String)` from the neutral
  common contract.
- Produces: `NotificationErrors.NOTIFICATION_NOT_FOUND` and
  `NotificationErrors.NOTIFICATION_SUBSCRIPTION_NOT_FOUND`.

- [ ] **Step 1: Write the failing descriptor contract test**

Create `NotificationErrorContractTest` with the following initial content:

```java
package com.project.optrabidz.notification.application;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.project.optrabidz.notification.application.error.NotificationErrors.NOTIFICATION_NOT_FOUND;
import static com.project.optrabidz.notification.application.error.NotificationErrors.NOTIFICATION_SUBSCRIPTION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NotificationErrorContractTest {
    @ParameterizedTest
    @MethodSource("descriptors")
    void exposesApprovedPublicDescriptors(
            ErrorDescriptor descriptor,
            String code,
            String publicMessage
    ) {
        assertThat(descriptor).isEqualTo(new ErrorDescriptor(
                code,
                ErrorCategory.NOT_FOUND,
                publicMessage
        ));
    }

    private static Stream<Arguments> descriptors() {
        return Stream.of(
                arguments(
                        NOTIFICATION_NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "The requested notification was not found"
                ),
                arguments(
                        NOTIFICATION_SUBSCRIPTION_NOT_FOUND,
                        "NOTIFICATION_SUBSCRIPTION_NOT_FOUND",
                        "The requested notification subscription was not found"
                )
        );
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

```powershell
./mvnw -q -Dtest=NotificationErrorContractTest test
```

Expected RED: test compilation fails because `NotificationErrors` and its two
descriptor fields do not exist. A pass is invalid and must be investigated.

- [ ] **Step 3: Add the minimal descriptor catalogue**

Create `NotificationErrors.java`:

```java
package com.project.optrabidz.notification.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

public final class NotificationErrors {
    public static final ErrorDescriptor NOTIFICATION_NOT_FOUND =
            new ErrorDescriptor(
                    "NOTIFICATION_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested notification was not found"
            );

    public static final ErrorDescriptor NOTIFICATION_SUBSCRIPTION_NOT_FOUND =
            new ErrorDescriptor(
                    "NOTIFICATION_SUBSCRIPTION_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested notification subscription was not found"
            );

    private NotificationErrors() {
    }
}
```

- [ ] **Step 4: Run focused GREEN verification**

```powershell
./mvnw -q -Dtest=NotificationErrorContractTest test
```

Expected GREEN: two parameterized descriptor cases pass with zero failures and
errors.

- [ ] **Step 5: Commit and update Jira**

```powershell
git add -- `
  src/main/java/com/project/optrabidz/notification/application/error/NotificationErrors.java `
  src/test/java/com/project/optrabidz/notification/application/NotificationErrorContractTest.java
git commit -m "feat: add notification error catalogue (KAN-29)"
```

Add the exact RED reason, GREEN count, and commit SHA to KAN-29.

---

## Task 2: Migrate typed service failures and prove ownership privacy

**Files:**
- Modify: `src/test/java/com/project/optrabidz/notification/application/NotificationErrorContractTest.java`
- Modify: `src/test/java/com/project/optrabidz/notification/api/NotificationApiIT.java`
- Modify: `src/main/java/com/project/optrabidz/notification/application/exception/NotificationNotFoundException.java`
- Create: `src/main/java/com/project/optrabidz/notification/application/exception/NotificationSubscriptionNotFoundException.java`
- Modify: `src/main/java/com/project/optrabidz/notification/application/NotificationService.java`

**Interfaces:**
- Consumes: the two Task 1 descriptors and existing account-scoped conditional
  SQL mutations.
- Produces: `NotificationNotFoundException(String diagnosticMessage)` and
  `NotificationSubscriptionNotFoundException(String diagnosticMessage)`.

- [ ] **Step 1: Add failing PostgreSQL API ownership tests**

In `NotificationApiIT`, add imports for `MvcResult`, `ResultMatcher`,
`java.util.Locale`, and the static MockMvc `content` and `header` matchers. Add
these two tests:

```java
@Test
void notificationMutationHidesMissingDeletedAndWrongOwnerResources()
        throws Exception {
    AuthenticatedClient owner = registerAndLogin(RoleType.STARTUP);
    AuthenticatedClient other = registerAndLogin(RoleType.INVESTOR);
    long ownerAccountId = accountIdBySessionClient(owner);
    outboxDispatcher.dispatchPending();

    long recipientId = jdbcTemplate.queryForObject("""
            select r.recipient_id
            from notification_recipient r
            join notification n on n.notification_id = r.notification_id
            where r.account_id = ?
              and n.notification_name = 'ACCOUNT_REGISTERED'
            """, Long.class, ownerAccountId);

    String wrongOwnerRequestId = "kan-29-notification-private";
    MvcResult wrongOwner = mockMvc.perform(patch(
                    "/api/v1/notifications/{recipientId}/read", recipientId)
                    .session(other.session())
                    .cookie(other.xsrfCookie())
                    .header("X-CSRF-TOKEN", other.csrfToken())
                    .header("X-Request-Id", wrongOwnerRequestId))
            .andExpectAll(notificationProblem(
                    "NOTIFICATION_NOT_FOUND",
                    "The requested notification was not found",
                    wrongOwnerRequestId
            ))
            .andReturn();

    assertBodyExcludes(
            wrongOwner,
            Long.toString(ownerAccountId),
            Long.toString(recipientId),
            "NOTIFICATION.RECIPIENT.NOT_FOUND"
    );
    assertThat(jdbcTemplate.queryForObject("""
            select read_status::text
            from notification_recipient
            where recipient_id = ?
            """, String.class, recipientId)).isEqualTo("UNREAD");

    mockMvc.perform(delete("/api/v1/notifications/{recipientId}", recipientId)
                    .session(owner.session())
                    .cookie(owner.xsrfCookie())
                    .header("X-CSRF-TOKEN", owner.csrfToken()))
            .andExpect(status().isOk());

    String deletedRequestId = "kan-29-notification-deleted";
    mockMvc.perform(patch("/api/v1/notifications/{recipientId}/read", recipientId)
                    .session(owner.session())
                    .cookie(owner.xsrfCookie())
                    .header("X-CSRF-TOKEN", owner.csrfToken())
                    .header("X-Request-Id", deletedRequestId))
            .andExpectAll(notificationProblem(
                    "NOTIFICATION_NOT_FOUND",
                    "The requested notification was not found",
                    deletedRequestId
            ));

    String missingRequestId = "kan-29-notification-missing";
    mockMvc.perform(delete("/api/v1/notifications/{recipientId}", Long.MAX_VALUE)
                    .session(owner.session())
                    .cookie(owner.xsrfCookie())
                    .header("X-CSRF-TOKEN", owner.csrfToken())
                    .header("X-Request-Id", missingRequestId))
            .andExpectAll(notificationProblem(
                    "NOTIFICATION_NOT_FOUND",
                    "The requested notification was not found",
                    missingRequestId
            ));
}

@Test
void subscriptionRevocationHidesMissingRevokedAndWrongOwnerResources()
        throws Exception {
    AuthenticatedClient owner = registerAndLogin(RoleType.STARTUP);
    AuthenticatedClient other = registerAndLogin(RoleType.INVESTOR);
    long subscriptionId = createSubscription(
            owner,
            "PUSH",
            "https://push.example.com/subscription/kan-29-private",
            "kan-29-public-key",
            "kan-29-auth-secret"
    );

    String wrongOwnerRequestId = "kan-29-subscription-private";
    MvcResult wrongOwner = mockMvc.perform(delete(
                    "/api/v1/notification-subscriptions/{subscriptionId}",
                    subscriptionId)
                    .session(other.session())
                    .cookie(other.xsrfCookie())
                    .header("X-CSRF-TOKEN", other.csrfToken())
                    .header("X-Request-Id", wrongOwnerRequestId))
            .andExpectAll(notificationProblem(
                    "NOTIFICATION_SUBSCRIPTION_NOT_FOUND",
                    "The requested notification subscription was not found",
                    wrongOwnerRequestId
            ))
            .andReturn();

    assertBodyExcludes(
            wrongOwner,
            Long.toString(subscriptionId),
            "kan-29-private",
            "kan-29-public-key",
            "kan-29-auth-secret",
            "NOTIFICATION.SUBSCRIPTION.NOT_FOUND"
    );
    assertThat(jdbcTemplate.queryForObject("""
            select subscription_state::text
            from notification_subscription
            where subscription_id = ?
            """, String.class, subscriptionId)).isEqualTo("ACTIVE");

    mockMvc.perform(delete(
                    "/api/v1/notification-subscriptions/{subscriptionId}",
                    subscriptionId)
                    .session(owner.session())
                    .cookie(owner.xsrfCookie())
                    .header("X-CSRF-TOKEN", owner.csrfToken()))
            .andExpect(status().isOk());

    String revokedRequestId = "kan-29-subscription-revoked";
    mockMvc.perform(delete(
                    "/api/v1/notification-subscriptions/{subscriptionId}",
                    subscriptionId)
                    .session(owner.session())
                    .cookie(owner.xsrfCookie())
                    .header("X-CSRF-TOKEN", owner.csrfToken())
                    .header("X-Request-Id", revokedRequestId))
            .andExpectAll(notificationProblem(
                    "NOTIFICATION_SUBSCRIPTION_NOT_FOUND",
                    "The requested notification subscription was not found",
                    revokedRequestId
            ));

    String missingRequestId = "kan-29-subscription-missing";
    mockMvc.perform(delete(
                    "/api/v1/notification-subscriptions/{subscriptionId}",
                    Long.MAX_VALUE)
                    .session(owner.session())
                    .cookie(owner.xsrfCookie())
                    .header("X-CSRF-TOKEN", owner.csrfToken())
                    .header("X-Request-Id", missingRequestId))
            .andExpectAll(notificationProblem(
                    "NOTIFICATION_SUBSCRIPTION_NOT_FOUND",
                    "The requested notification subscription was not found",
                    missingRequestId
            ));
}
```

Replace the existing `createSubscription` helper with:

```java
private long createSubscription(
        AuthenticatedClient client,
        String channelType,
        String endpoint,
        String publicKey,
        String authSecret
) throws Exception {
    String response = mockMvc.perform(post("/api/v1/notification-subscriptions")
                    .session(client.session())
                    .cookie(client.xsrfCookie())
                    .header("X-CSRF-TOKEN", client.csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of(
                            "channelType", channelType,
                            "endpoint", endpoint,
                            "publicKey", publicKey == null ? "" : publicKey,
                            "authSecret", authSecret == null ? "" : authSecret
                    ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message")
                    .value("Notification subscription saved"))
            .andExpect(jsonPath("$.data.subscriptionId").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(response)
            .path("data")
            .path("subscriptionId")
            .asLong();
}
```

Existing callers may ignore the returned ID. Add these shared assertions:

```java
private ResultMatcher[] notificationProblem(
        String code,
        String detail,
        String requestId
) {
    return new ResultMatcher[] {
            status().isNotFound(),
            content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_PROBLEM_JSON),
            header().string("X-Request-Id", requestId),
            jsonPath("$.type").value(
                    "urn:optrabidz:problem:"
                            + code.toLowerCase(Locale.ROOT).replace('_', '-')),
            jsonPath("$.title").value("Resource not found"),
            jsonPath("$.status").value(404),
            jsonPath("$.detail").value(detail),
            jsonPath("$.instance").value(
                    "urn:optrabidz:request:" + requestId),
            jsonPath("$.code").value(code),
            jsonPath("$.requestId").value(requestId),
            jsonPath("$.timestamp").isString(),
            jsonPath("$.success").doesNotExist(),
            jsonPath("$.error").doesNotExist()
    };
}

private void assertBodyExcludes(
        MvcResult result,
        String... protectedValues
) throws Exception {
    assertThat(result.getResponse().getContentAsString())
            .doesNotContain(protectedValues);
}
```

- [ ] **Step 2: Run the PostgreSQL API tests and verify RED**

```powershell
./mvnw -q -DskipUnitTests -Pintegration-tests `
  -Dit.test=NotificationApiIT failsafe:integration-test failsafe:verify
```

Expected RED: the new tests expect RFC 9457 Problem Details and distinct
notification/subscription codes but receive the legacy response and generic
`RESOURCE_NOT_FOUND` code. Existing successful methods remain green.

- [ ] **Step 3: Extend the contract test with failing typed-failure cases**

Add these imports and members to `NotificationErrorContractTest`:

```java
import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.notification.application.exception.NotificationNotFoundException;
import com.project.optrabidz.notification.application.exception.NotificationSubscriptionNotFoundException;

import java.util.function.Function;
```

```java
private static final String PROTECTED_DIAGNOSTIC =
        "accountId=901 recipientId=902 subscriptionId=903 endpoint=secret";

@ParameterizedTest
@MethodSource("failures")
void typedFailuresKeepProtectedDiagnosticsOutOfPublicMessages(
        Function<String, ApplicationException> factory,
        ErrorDescriptor descriptor,
        String diagnosticCode
) {
    ApplicationException failure = factory.apply(PROTECTED_DIAGNOSTIC);

    assertThat(failure.descriptor()).isSameAs(descriptor);
    assertThat(failure.diagnosticCode()).isEqualTo(diagnosticCode);
    assertThat(failure.getMessage()).contains(PROTECTED_DIAGNOSTIC);
    assertThat(failure.descriptor().publicMessage())
            .doesNotContain(
                    "accountId=901",
                    "recipientId=902",
                    "subscriptionId=903",
                    "endpoint=secret"
            );
}

private static Stream<Arguments> failures() {
    return Stream.of(
            arguments(
                    (Function<String, ApplicationException>)
                            NotificationNotFoundException::new,
                    NOTIFICATION_NOT_FOUND,
                    "NOTIFICATION.RECIPIENT.NOT_FOUND"
            ),
            arguments(
                    (Function<String, ApplicationException>)
                            NotificationSubscriptionNotFoundException::new,
                    NOTIFICATION_SUBSCRIPTION_NOT_FOUND,
                    "NOTIFICATION.SUBSCRIPTION.NOT_FOUND"
            )
    );
}
```

- [ ] **Step 4: Run the contract test and verify RED**

```powershell
./mvnw -q -Dtest=NotificationErrorContractTest test
```

Expected RED: test compilation fails because the existing
`NotificationNotFoundException` lacks the diagnostic constructor and
`NotificationSubscriptionNotFoundException` does not exist.

- [ ] **Step 5: Implement the two typed neutral exceptions**

Replace `NotificationNotFoundException` with:

```java
package com.project.optrabidz.notification.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.notification.application.error.NotificationErrors;

public final class NotificationNotFoundException extends ApplicationException {
    public NotificationNotFoundException(String diagnosticMessage) {
        super(
                NotificationErrors.NOTIFICATION_NOT_FOUND,
                "NOTIFICATION.RECIPIENT.NOT_FOUND",
                diagnosticMessage
        );
    }
}
```

Create `NotificationSubscriptionNotFoundException`:

```java
package com.project.optrabidz.notification.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.notification.application.error.NotificationErrors;

public final class NotificationSubscriptionNotFoundException
        extends ApplicationException {
    public NotificationSubscriptionNotFoundException(String diagnosticMessage) {
        super(
                NotificationErrors.NOTIFICATION_SUBSCRIPTION_NOT_FOUND,
                "NOTIFICATION.SUBSCRIPTION.NOT_FOUND",
                diagnosticMessage
        );
    }
}
```

- [ ] **Step 6: Select failures only after zero-row mutations**

Import `NotificationSubscriptionNotFoundException` in `NotificationService`.
Replace the three zero-row branches with these exact diagnostics:

```java
if (updated == 0) {
    throw new NotificationNotFoundException(
            "Notification read mutation matched no active owned recipient: "
                    + "recipientId=" + recipientId
                    + " accountId=" + accountId
    );
}
```

```java
if (updated == 0) {
    throw new NotificationNotFoundException(
            "Notification delete mutation matched no active owned recipient: "
                    + "recipientId=" + recipientId
                    + " accountId=" + accountId
    );
}
```

```java
if (updated == 0) {
    throw new NotificationSubscriptionNotFoundException(
            "Subscription revoke mutation matched no active owned subscription: "
                    + "subscriptionId=" + subscriptionId
                    + " accountId=" + accountId
    );
}
```

Do not add a lookup query or a catch block around any JDBC operation.

- [ ] **Step 7: Run focused GREEN verification**

```powershell
./mvnw -q -Dtest=NotificationErrorContractTest test
./mvnw -q -DskipUnitTests -Pintegration-tests `
  -Dit.test=NotificationApiIT failsafe:integration-test failsafe:verify
```

Expected GREEN: all four contract invocations and all NotificationApiIT methods
pass. Wrong-owner rows remain unchanged; missing/deleted/revoked paths use the
approved safe 404 codes.

- [ ] **Step 8: Commit and update Jira**

```powershell
git add -- `
  src/main/java/com/project/optrabidz/notification/application/NotificationService.java `
  src/main/java/com/project/optrabidz/notification/application/exception/NotificationNotFoundException.java `
  src/main/java/com/project/optrabidz/notification/application/exception/NotificationSubscriptionNotFoundException.java `
  src/test/java/com/project/optrabidz/notification/application/NotificationErrorContractTest.java `
  src/test/java/com/project/optrabidz/notification/api/NotificationApiIT.java
git commit -m "refactor: migrate notification lookup failures (KAN-29)"
```

Add the exact RED/GREEN results, ownership-mutation checks, disclosure checks,
and commit SHA to KAN-29.

---

## Task 3: Remove controller legacy handling and enforce the architecture boundary

**Files:**
- Modify: `src/test/java/com/project/optrabidz/notification/api/NotificationApiIT.java`
- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify: `src/main/java/com/project/optrabidz/notification/api/NotificationController.java`
- Delete: `src/main/java/com/project/optrabidz/notification/application/exception/NotificationAccessDeniedException.java`
- Modify: `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f`

**Interfaces:**
- Consumes: existing `/api/v1/notifications/**` and
  `/api/v1/notification-subscriptions/**` authenticated matchers plus Task 2
  neutral failures.
- Produces: a notification module with zero legacy exception-package
  dependencies and an unchanged shared anonymous 401 boundary.

- [ ] **Step 1: Add the anonymous-boundary characterization test**

Add to `NotificationApiIT`:

```java
@Test
void anonymousNotificationQueryUsesSharedAuthenticationBoundary()
        throws Exception {
    String requestId = "kan-29-notification-authentication";

    mockMvc.perform(get("/api/v1/notifications/me")
                    .header("X-Request-Id", requestId))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.requestId").value(requestId))
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());
}
```

Run it before changing the controller:

```powershell
./mvnw -q -DskipUnitTests -Pintegration-tests `
  -Dit.test=NotificationApiIT#anonymousNotificationQueryUsesSharedAuthenticationBoundary `
  failsafe:integration-test failsafe:verify
```

Expected characterization GREEN: Spring Security already returns the shared
401 before controller execution. This locks behavior; it is not the RED signal.

- [ ] **Step 2: Make the permanent architecture rule fail**

Add `"..notification.."` to
`MIGRATED_MODULES_DO_NOT_USE_LEGACY_API_EXCEPTIONS` in
`ExceptionArchitectureTest` and run:

```powershell
./mvnw -q -Dtest=ExceptionArchitectureTest test
```

Expected RED: the rule identifies the controller's legacy imports/construction
and the unused legacy access-denied exception. The frozen rule may also report
the six now-obsolete stored notification violations because automatic store
updates are disabled.

- [ ] **Step 3: Remove controller-local authentication construction**

In `NotificationController`:

- remove the `ApiException` and `ErrorCode` imports;
- remove the private `requirePrincipal` method; and
- replace each two-line `user = requirePrincipal(principal)` flow by passing
  `principal.getAccountId()` directly to the existing service call.

For example, `markRead` becomes:

```java
notificationService.markRead(principal.getAccountId(), recipientId);
return ApiResponse.success(
        new MessageData("Notification marked as read"),
        httpRequest
);
```

Apply the same mechanical change to feed, summary, mark-all-read, delete,
subscription create, and subscription revoke. Do not change paths, DTOs,
success messages, statuses, or SecurityConfig.

Delete `NotificationAccessDeniedException.java`; repository-wide search proves
it has no caller.

- [ ] **Step 4: Remove exactly the six obsolete frozen entries**

Before editing the store, verify the baseline:

```powershell
$store = 'src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f'
$before = (Select-String -LiteralPath $store -Pattern 'notification').Count
if ($before -ne 6) {
    throw "Expected 6 notification frozen violations, found $before"
}
```

Delete only those six lines. Then verify:

```powershell
$after = (Select-String -LiteralPath $store -Pattern 'notification').Count
if ($after -ne 0) {
    throw "Notification frozen violations remain: $after"
}
```

- [ ] **Step 5: Run architecture, source, and focused API GREEN checks**

```powershell
./mvnw -q -Dtest=ExceptionArchitectureTest test

if (rg -n "common\.api\.exception|ApiException|ErrorCode" `
        src/main/java/com/project/optrabidz/notification) {
    throw 'Notification still depends on the legacy exception package'
}

if (rg -n "NotificationAccessDeniedException" src/main src/test) {
    throw 'Deleted access-denied exception still has a caller'
}

./mvnw -q -DskipUnitTests -Pintegration-tests `
  -Dit.test=NotificationApiIT failsafe:integration-test failsafe:verify
```

Expected GREEN: all three architecture rules pass, both scans are empty, and
all NotificationApiIT methods pass, including the unchanged anonymous 401.

- [ ] **Step 6: Commit and update Jira**

```powershell
git add -- `
  src/main/java/com/project/optrabidz/notification/api/NotificationController.java `
  src/main/java/com/project/optrabidz/notification/application/exception/NotificationAccessDeniedException.java `
  src/test/java/com/project/optrabidz/notification/api/NotificationApiIT.java `
  src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
  src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f
git commit -m "test: enforce notification error boundaries (KAN-29)"
```

Add the characterization result, architecture RED/GREEN evidence, exact
six-entry removal, source scans, focused API result, and commit SHA to KAN-29.

---

## Task 4: Complete regression, documentation, and review handoff

**Files:**
- Modify: `docs/error-handling/README.md`
- Modify: `docs/error-handling/work-items/KAN-29-notification-error-migration/implementation-plan.md`
- Temporary, untracked: `.git/KAN-29-pr-body.md`

**Interfaces:**
- Consumes: Tasks 1–3, unchanged Flyway V1, and the complete Maven verification
  profiles.
- Produces: a clean reviewed feature head, synchronized Jira evidence, and one
  pull request targeting `develop`.

- [ ] **Step 1: Run the complete unit and architecture suite**

```powershell
./mvnw -B test
```

Expected: build success with zero failures/errors. Inspect every Surefire XML
report and record the exact total rather than relying only on console output.

- [ ] **Step 2: Run the complete PostgreSQL integration suite**

```powershell
./mvnw -B verify -Pintegration-tests
```

Expected: build success with zero failures/errors against PostgreSQL
Testcontainers. Inspect every Failsafe XML report and record the exact total.

- [ ] **Step 3: Run scope, disclosure, architecture, and migration guards**

```powershell
git diff --check origin/develop...HEAD

if (rg -n "common\.api\.exception|ApiException|ErrorCode" `
        src/main/java/com/project/optrabidz/notification) {
    throw 'Legacy notification exception dependency remains'
}

if ((Select-String `
        -LiteralPath 'src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f' `
        -Pattern 'notification').Count -ne 0) {
    throw 'Notification frozen architecture debt remains'
}

$v1 = 'src/main/resources/db/migration/V1__baseline_schema.sql'
$baseV1 = git rev-parse "origin/develop:$v1"
$headV1 = git rev-parse "HEAD:$v1"
if ($baseV1 -ne $headV1) {
    throw 'Flyway V1 changed outside KAN-29 scope'
}

$protected = @(
    'pom.xml',
    '.github/workflows/ci.yml',
    'src/main/resources/application.properties',
    'src/main/resources/application-dev.properties',
    'src/main/resources/application-test.properties',
    'src/main/resources/application-prod.properties',
    'src/test/resources/application-test.properties',
    'src/main/resources/db/migration/V1__baseline_schema.sql'
)
$changed = git diff --name-only origin/develop...HEAD
$unexpected = $changed | Where-Object { $_ -in $protected }
if ($unexpected) {
    throw "Protected files changed: $($unexpected -join ', ')"
}
```

Expected: all guards pass; notification has no legacy dependency; Flyway V1,
build, CI, and runtime configuration remain byte-identical to `origin/develop`.

- [ ] **Step 4: Finalize documentation evidence**

Update the KAN-29 row in `docs/error-handling/README.md` so it links both the
design and implementation plan. In this plan, mark only completed checkboxes
and add an execution-evidence section containing:

- exact RED and GREEN commands/results per task;
- exact unit/architecture and integration totals;
- the six-entry architecture cleanup result;
- zero-result legacy scans;
- unchanged V1 and protected-file evidence; and
- the ordered implementation commit SHAs.

Run:

```powershell
./mvnw -q -Dtest=DocumentationLinksTest test
git diff --check
```

Expected: documentation links and diff checks pass.

- [ ] **Step 5: Commit final evidence and push**

```powershell
git add -- `
  docs/error-handling/README.md `
  docs/error-handling/work-items/KAN-29-notification-error-migration/implementation-plan.md
git commit -m "docs: record notification migration evidence (KAN-29)"
git push origin feature/KAN-29-notification-error-migration
```

Verify the worktree is clean and local/remote feature heads match.

- [ ] **Step 6: Create the pull request without merging**

Write `.git/KAN-29-pr-body.md` with these sections and exact verified values:

```markdown
## Summary
- add notification-owned neutral error descriptors and typed failures
- preserve ownership privacy with endpoint-specific safe 404 responses
- remove notification legacy exception dependencies without redesigning workers

## Intentional API changes
- expected notification mutation failures now use RFC 9457 Problem Details
- RESOURCE_NOT_FOUND becomes NOTIFICATION_NOT_FOUND or NOTIFICATION_SUBSCRIPTION_NOT_FOUND

## Preserved behavior
- anonymous access remains the shared AUTHENTICATION_REQUIRED 401 boundary
- successful feed, subscription, outbox, delivery, retry, provider, and audit behavior is unchanged

## Verification
- focused RED/GREEN evidence recorded in KAN-29
- complete unit and architecture suite passed
- complete PostgreSQL integration suite passed
- notification legacy scan and frozen architecture debt are empty

## Risk and rollback
- no database, Flyway, dependency, configuration, or asynchronous redesign
- roll back by reverting this PR as one unit; no database rollback is required
```

Create the PR:

```powershell
gh pr create `
  --base develop `
  --head feature/KAN-29-notification-error-migration `
  --title "KAN-29: Migrate notification failures to the neutral error contract" `
  --body-file .git/KAN-29-pr-body.md
```

Move KAN-29 to **In Review** and add the PR URL, exact base/head SHAs, commit
list, test totals, intentional API changes, preserved behavior, risk, and
rollback statement. Do not merge.

- [ ] **Step 7: Wait for explicit PR approval and close safely**

After the user explicitly approves the exact PR head:

```powershell
git fetch origin
$approvedHead = git rev-parse origin/feature/KAN-29-notification-error-migration
$prHead = gh pr view --json headRefOid --jq .headRefOid
if ($prHead -ne $approvedHead) {
    throw 'PR head changed after review'
}

gh pr merge --merge --delete-branch --match-head-commit $approvedHead
git switch develop
git pull --ff-only origin develop
```

Verify the merge commit has the previous `develop` head and approved feature
head as parents, rerun both complete Maven suites on merged `develop`, confirm
`main` and `origin/main` remain unchanged, delete any remaining local feature
branch, add final Jira evidence, and move KAN-29 to **Done**.

If any verification fails, stop; keep KAN-29 out of Done and open a focused
defect or corrective commit for review.

---

## Final acceptance audit

- [ ] Approved catalogue contains exactly two notification-owned descriptors.
- [ ] Typed failures use the approved descriptors and protected diagnostics.
- [ ] Missing/deleted/revoked and wrong-owner resources are indistinguishable
      within their endpoint family.
- [ ] Wrong-owner requests leave owner rows unchanged.
- [ ] Anonymous access retains the existing shared 401 response.
- [ ] Notification production code has zero legacy exception dependencies.
- [ ] Exactly six obsolete notification frozen violations are removed.
- [ ] Unexpected SQL/runtime failures are not translated to 404.
- [ ] Successful notification and asynchronous behavior remains unchanged.
- [ ] Flyway V1, dependencies, CI, runtime properties, and SecurityConfig remain
      unchanged.
- [ ] Complete unit/architecture and PostgreSQL integration suites pass.
- [ ] Jira contains design, approval, RED/GREEN, PR, review, merge, and final
      verification evidence.
- [ ] `main` remains unchanged; only the approved PR is merged into `develop`.
