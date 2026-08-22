# KAN-28 Marketplace Error Migration Implementation Plan

**Goal:** Replace marketplace legacy exceptions with a module-owned,
transport-neutral error contract while moving actor-required endpoint checks to
the existing Spring Security boundary.

**Architecture:** `MarketplaceErrors` owns nine stable descriptors and typed
marketplace exceptions carry protected diagnostics. Spring Security rejects
anonymous access before protected controllers, while the existing REST adapter
remains the sole RFC 9457 response builder.

**Tech stack:** Java 21, Spring Boot 3.3, Spring MVC, Spring Security, JUnit 5,
Mockito, AssertJ, MockMvc, ArchUnit, Maven, Testcontainers, PostgreSQL 16

**Spec:** [KAN-28 design](design.md)

## Global constraints

- Work only on `feature/KAN-28-marketplace-error-migration`; the pull request
  targets `develop`, never `main`.
- Keep `main`, Flyway V1, the database schema, dependencies, runtime properties,
  session policy, roles, CSRF policy, and successful response model unchanged.
- Public listing browse and detail remain anonymous; actor-required marketplace
  operations are authenticated by Spring Security.
- Do not implement JWT or OAuth2 in KAN-28. Preserve a mechanism-independent
  endpoint boundary so a later security story can replace the session mechanism.
- Public Problem Details never contain identifiers, roles, internal states,
  action names, class names, constraint names, raw messages, or diagnostic codes.
- Expected failures must stop before prohibited writes, event publication,
  outbox work, and finance-port calls.
- Do not broadly relabel unexpected domain or database failures as expected
  marketplace conflicts.
- Every production change begins with a focused failing test and ends with a
  focused passing test before commit.
- Move KAN-28 to **In Progress** only when inline implementation begins,
  **In Review** only after the pull request opens with green CI, and **Done**
  only after approved merge and final verification.

---

## Execution gate

Run only after this plan and inline execution are explicitly approved:

```powershell
git fetch origin
git switch feature/KAN-28-marketplace-error-migration
if (git status --porcelain) {
    throw 'KAN-28 worktree must be clean before implementation'
}
if ((git merge-base HEAD origin/develop) -ne (git rev-parse origin/develop)) {
    throw 'KAN-28 branch must still contain the latest reviewed origin/develop'
}
if ((git rev-parse main) -ne (git rev-parse origin/main)) {
    throw 'local and remote main must remain aligned before implementation'
}
```

Verify Jira shows KAN-28 assigned to Kumar Kundan, parent KAN-16, and status
**To Do**. Transition it to **In Progress**, add an implementation-start
comment containing the exact branch base, and then begin Task 1. Stop if any
gate differs; do not repair an unexpected state silently.

---

## File map

| Path | Responsibility |
|---|---|
| `src/main/java/com/project/optrabidz/marketplace/application/error/MarketplaceErrors.java` | Owns the nine approved public descriptors. |
| `src/main/java/com/project/optrabidz/marketplace/application/exception/*.java` | Carries typed protected diagnostics through `ApplicationException`. |
| `src/main/java/com/project/optrabidz/security/infrastructure/config/SecurityConfig.java` | Enforces authentication for actor-required marketplace endpoints. |
| `src/main/java/com/project/optrabidz/marketplace/api/ListingController.java` | Removes duplicated principal rejection while preserving optional listing-detail identity. |
| `src/main/java/com/project/optrabidz/marketplace/api/BidController.java` | Relies on the authenticated Spring Security boundary. |
| `src/main/java/com/project/optrabidz/marketplace/api/AgreementController.java` | Relies on the authenticated Spring Security boundary. |
| `src/main/java/com/project/optrabidz/marketplace/application/ListingService.java` | Maps listing lookup, role, visibility, and lifecycle failures without broad exception conversion. |
| `src/main/java/com/project/optrabidz/marketplace/application/BidService.java` | Maps bid lookup, duplicate, lifecycle, and acceptance conflicts while preserving transaction safety. |
| `src/main/java/com/project/optrabidz/marketplace/application/AgreementService.java` | Maps agreement lookup and access failures. |
| `src/main/java/com/project/optrabidz/marketplace/application/MarketplaceDiscoveryService.java` | Maps recommendation-role failures. |
| `src/main/java/com/project/optrabidz/marketplace/application/policy/FundingModelPolicyResolver.java` | Maps a valid unsupported model to the approved 422 failure. |
| `src/main/java/com/project/optrabidz/marketplace/application/specification/*.java` | Raises typed access and lifecycle failures before mutation. |
| `src/test/java/com/project/optrabidz/marketplace/application/MarketplaceErrorContractTest.java` | Freezes descriptor, exception, and disclosure behavior. |
| `src/test/java/com/project/optrabidz/marketplace/application/ListingServiceTest.java` | Verifies listing failure ordering and unexpected-invariant behavior. |
| `src/test/java/com/project/optrabidz/marketplace/application/BidServiceTest.java` | Verifies bid acceptance, side-effect, and persistence-failure behavior. |
| `src/test/java/com/project/optrabidz/marketplace/application/AgreementServiceTest.java` | Verifies agreement lookup and role failures. |
| `src/test/java/com/project/optrabidz/marketplace/application/specification/MarketplaceSpecificationTest.java` | Verifies typed lifecycle, ownership, and visibility rules. |
| `src/test/java/com/project/optrabidz/marketplace/api/MarketplaceSecurityIT.java` | Freezes the public/protected endpoint authentication matrix. |
| `src/test/java/com/project/optrabidz/marketplace/api/MarketplaceApiIT.java` | Verifies marketplace RFC 9457 responses against PostgreSQL. |
| `src/test/java/com/project/optrabidz/marketplace/infrastructure/repository/MarketplaceRepositoryIT.java` | Verifies the conditional listing-state concurrency guard. |
| `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Adds marketplace to the permanent legacy-dependency prohibition. |
| `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f` | Removes only resolved frozen marketplace violations. |
| `docs/error-handling/README.md` | Records marketplace as migrated after verification. |
| `docs/error-handling/work-items/KAN-28-marketplace-error-migration/implementation-plan.md` | Records executed evidence and review state. |

---

## Task 1: Add the marketplace error contract

**Files:**

- Create: `src/main/java/com/project/optrabidz/marketplace/application/error/MarketplaceErrors.java`
- Modify: all nine existing files under
  `src/main/java/com/project/optrabidz/marketplace/application/exception/`
- Create: `src/main/java/com/project/optrabidz/marketplace/application/exception/BidAcceptanceConflictException.java`
- Create: `src/test/java/com/project/optrabidz/marketplace/application/MarketplaceErrorContractTest.java`

**Interfaces:**

- Produces: nine `public static final ErrorDescriptor` constants named exactly
  like their public codes.
- Produces: marketplace exception types extending `ApplicationException`.
- Consumes: `ApplicationException`, `ErrorDescriptor`, and `ErrorCategory` from
  the existing neutral core.

- [ ] **Step 1: Write the failing descriptor and disclosure test**

Create a parameterized catalogue table:

```java
static Stream<Arguments> descriptors() {
    return Stream.of(
            arguments(LISTING_NOT_FOUND, "LISTING_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested listing was not found"),
            arguments(BID_NOT_FOUND, "BID_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested bid was not found"),
            arguments(AGREEMENT_NOT_FOUND, "AGREEMENT_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested agreement was not found"),
            arguments(MARKETPLACE_ACCESS_DENIED, "MARKETPLACE_ACCESS_DENIED",
                    ErrorCategory.AUTHORIZATION,
                    "You are not authorized to perform this marketplace action"),
            arguments(LISTING_STATE_CONFLICT, "LISTING_STATE_CONFLICT",
                    ErrorCategory.CONFLICT,
                    "The requested action conflicts with the current listing state"),
            arguments(BID_STATE_CONFLICT, "BID_STATE_CONFLICT",
                    ErrorCategory.CONFLICT,
                    "The requested action conflicts with the current bid state"),
            arguments(BID_ALREADY_EXISTS, "BID_ALREADY_EXISTS",
                    ErrorCategory.CONFLICT,
                    "An active bid already exists for this listing"),
            arguments(BID_ACCEPTANCE_CONFLICT, "BID_ACCEPTANCE_CONFLICT",
                    ErrorCategory.CONFLICT,
                    "The bid cannot be accepted in the current marketplace state"),
            arguments(UNSUPPORTED_FUNDING_MODEL, "UNSUPPORTED_FUNDING_MODEL",
                    ErrorCategory.BUSINESS_RULE,
                    "The requested funding model is not supported")
    );
}
```

For each row assert exact code, category, and public message. Construct every
exception and assert its descriptor and diagnostic code. Include a diagnostic
containing `listing=101`, `bid=501`, `role=ADMIN`, and `state=CLOSED`; prove
none of those strings appears in `descriptor().publicMessage()`.

- [ ] **Step 2: Run the contract test and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=MarketplaceErrorContractTest" test
```

Expected RED: `MarketplaceErrors` and `BidAcceptanceConflictException` do not
exist, and the existing exceptions are not neutral `ApplicationException`
types.

- [ ] **Step 3: Implement `MarketplaceErrors`**

Declare exactly the nine descriptors from Step 1. The class is `final`, has a
private constructor, and imports no HTTP, servlet, Spring, or `common.api`
type:

```java
public final class MarketplaceErrors {
    public static final ErrorDescriptor LISTING_NOT_FOUND = new ErrorDescriptor(
            "LISTING_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested listing was not found"
    );

    private MarketplaceErrors() {
    }
}
```

Repeat the explicit declaration for every table row; do not generate codes
from class names or exception messages.

- [ ] **Step 4: Migrate the exception classes without breaking callers**

Use this exact pattern for the initial contract slice:

```java
public final class ListingNotFoundException extends ApplicationException {
    public ListingNotFoundException(String diagnosticMessage) {
        super(
                MarketplaceErrors.LISTING_NOT_FOUND,
                "MARKETPLACE.LISTING.NOT_FOUND",
                diagnosticMessage
        );
    }
}
```

Apply these exact descriptor and diagnostic-code pairs while temporarily
preserving current `String diagnosticMessage` constructors so the branch stays
compilable between slices:

| Exception | Descriptor | Diagnostic code |
|---|---|---|
| `ListingNotFoundException` | `LISTING_NOT_FOUND` | `MARKETPLACE.LISTING.NOT_FOUND` |
| `BidNotFoundException` | `BID_NOT_FOUND` | `MARKETPLACE.BID.NOT_FOUND` |
| `AgreementNotFoundException` | `AGREEMENT_NOT_FOUND` | `MARKETPLACE.AGREEMENT.NOT_FOUND` |
| `MarketplaceAccessException` | `MARKETPLACE_ACCESS_DENIED` | `MARKETPLACE.ACCESS.DENIED` |
| `InvalidListingStateException` | `LISTING_STATE_CONFLICT` | `MARKETPLACE.LISTING.STATE_CONFLICT` |
| `InvalidBidStateException` | `BID_STATE_CONFLICT` | `MARKETPLACE.BID.STATE_CONFLICT` |
| `BidAlreadyExistsException` | `BID_ALREADY_EXISTS` | `MARKETPLACE.BID.ALREADY_EXISTS` |
| `BidAlreadyAcceptedException` | `BID_ACCEPTANCE_CONFLICT` | `MARKETPLACE.BID.ACCEPTANCE_CONFLICT` |
| `UnsupportedFundingModelException` | `UNSUPPORTED_FUNDING_MODEL` | `MARKETPLACE.FUNDING_MODEL.UNSUPPORTED` |

Create `BidAcceptanceConflictException` with the same acceptance descriptor
and diagnostic code. The old class remains only until Task 3 migrates every
caller and test.

- [ ] **Step 5: Run focused and existing marketplace unit tests**

```powershell
.\mvnw.cmd -B "-Dtest=MarketplaceErrorContractTest,MarketplaceSpecificationTest,BidServiceTest" test
```

Expected GREEN: the contract test and all pre-existing marketplace unit tests
pass; no public descriptor contains diagnostic input.

- [ ] **Step 6: Commit the contract slice**

```powershell
git add src/main/java/com/project/optrabidz/marketplace/application/error `
        src/main/java/com/project/optrabidz/marketplace/application/exception `
        src/test/java/com/project/optrabidz/marketplace/application/MarketplaceErrorContractTest.java
git commit -m "feat: add marketplace error contract (KAN-28)"
```

Update Jira with the focused RED/GREEN result and commit SHA; keep the story
**In Progress**.

---

## Task 2: Migrate listing failures and enforce authentication at the boundary

**Files:**

- Modify: `src/main/java/com/project/optrabidz/security/infrastructure/config/SecurityConfig.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/api/ListingController.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/api/BidController.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/api/AgreementController.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/application/ListingService.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/application/MarketplaceDiscoveryService.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/application/policy/FundingModelPolicyResolver.java`
- Modify: listing and visibility specifications under
  `src/main/java/com/project/optrabidz/marketplace/application/specification/`
- Create: `src/test/java/com/project/optrabidz/marketplace/application/ListingServiceTest.java`
- Modify: `src/test/java/com/project/optrabidz/marketplace/application/specification/MarketplaceSpecificationTest.java`
- Create: `src/test/java/com/project/optrabidz/marketplace/api/MarketplaceSecurityIT.java`

**Interfaces:**

- Produces: Spring Security authentication enforcement for actor-required
  marketplace endpoints.
- Preserves: anonymous `GET /api/v1/funding-listings` and
  `GET /api/v1/funding-listings/{listingId}`.
- Consumes: Task 1 descriptors and the existing
  `ProblemAuthenticationEntryPoint`.

- [ ] **Step 1: Write the failing authentication-matrix integration test**

Create `MarketplaceSecurityIT extends ApiIntegrationTestSupport`. Assert an
anonymous browse request returns 200 and an anonymous missing listing detail
returns `LISTING_NOT_FOUND` 404 rather than 401. Parameterize protected GETs:

```java
@ParameterizedTest
@ValueSource(strings = {
        "/api/v1/funding-listings/recommended",
        "/api/v1/bids/1",
        "/api/v1/funding-listings/1/accepted-bid",
        "/api/v1/agreements/1"
})
void anonymousActorRequiredQueriesUseSharedAuthenticationBoundary(String path)
        throws Exception {
    mockMvc.perform(get(path).header("X-Request-Id", "kan-28-security"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.requestId").value("kan-28-security"));
}
```

Also post a validly shaped listing request as anonymous with `.with(csrf())`
and assert the same 401. Using CSRF in this test isolates authentication from
the independently preserved CSRF policy.

- [ ] **Step 2: Write failing listing behavior tests**

In `MarketplaceSpecificationTest`, assert listing lifecycle failures select
`LISTING_STATE_CONFLICT` and visibility/ownership failures select
`MARKETPLACE_ACCESS_DENIED`.

In the new `ListingServiceTest`, mock collaborators and cover:

```java
when(listingRepository.findById(101L)).thenReturn(Optional.empty());
assertThatThrownBy(() -> service.getListingDetails(101L, null, null))
        .isInstanceOf(ListingNotFoundException.class)
        .extracting(failure -> ((ApplicationException) failure).descriptor())
        .isSameAs(MarketplaceErrors.LISTING_NOT_FOUND);
```

For unexpected-invariant behavior, allow the mocked lifecycle specification to
return normally for a non-draft aggregate, call `updateDraftListing`, assert
the domain `IllegalStateException` remains unwrapped, and verify
`listingRepository.save` plus `eventPublisher.publish` were never called.

Test `FundingModelPolicyResolver.resolve(FundingModel.EQUITY)` and assert
`UNSUPPORTED_FUNDING_MODEL` with `BUSINESS_RULE` category.

- [ ] **Step 3: Run the focused tests and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=ListingServiceTest,MarketplaceSpecificationTest" test
.\mvnw.cmd -B verify -Pintegration-tests "-Dit.test=MarketplaceSecurityIT"
```

Expected RED: protected bid/agreement/recommendation paths reach controllers or
application failures instead of the shared 401, and listing call sites still
use legacy diagnostic constructors and broad invariant conversion.

- [ ] **Step 4: Add the exact Spring Security matchers**

Add these rules before `.anyRequest().permitAll()` and after the existing
public authentication/webhook rules:

```java
.requestMatchers(
        "/api/v1/bids",
        "/api/v1/bids/**",
        "/api/v1/agreements/**"
).authenticated()
.requestMatchers(
        HttpMethod.GET,
        "/api/v1/funding-listings/recommended",
        "/api/v1/funding-listings/*/accepted-bid"
).authenticated()
.requestMatchers(
        HttpMethod.POST,
        "/api/v1/funding-listings",
        "/api/v1/funding-listings/*/actions/**"
).authenticated()
.requestMatchers(
        HttpMethod.PATCH,
        "/api/v1/funding-listings/*"
).authenticated()
```

Do not add a blanket `/api/v1/funding-listings/**` authentication rule because
it would break public browse and detail access. Do not change session creation,
CSRF, entry-point, denied-handler, or role matchers.

- [ ] **Step 5: Remove duplicate controller authentication exceptions**

Delete `requirePrincipal` plus legacy imports from all three marketplace
controllers. Protected methods use the non-null principal supplied after the
filter boundary:

```java
return ApiResponse.success(
        bidService.getBidById(
                principal.getAccountId(),
                principal.getRole(),
                bidId
        ),
        httpRequest
);
```

Keep listing detail's existing nullable-principal adaptation exactly:

```java
principal == null ? null : principal.getAccountId(),
principal == null ? null : principal.getRole()
```

- [ ] **Step 6: Migrate listing, discovery, and funding-model call sites**

Use identifier-rich protected diagnostics for not-found failures:

```java
.orElseThrow(() -> new ListingNotFoundException(
        "Funding listing " + listingId + " was not found"
));
```

Keep role, ownership, and visibility reasons protected inside
`MarketplaceAccessException`; the public descriptor remains fixed. Change the
resolver to:

```java
throw new UnsupportedFundingModelException(
        "No marketplace policy supports funding model " + fundingModel
);
```

Remove `applyListingTransition`. After the existing specifications pass, call
`listing.updateDraft`, `listing.publish`, and `listing.close` directly. Do not
catch `IllegalStateException` in the service.

- [ ] **Step 7: Run focused unit and integration tests**

```powershell
.\mvnw.cmd -B "-Dtest=MarketplaceErrorContractTest,ListingServiceTest,MarketplaceSpecificationTest" test
.\mvnw.cmd -B verify -Pintegration-tests "-Dit.test=MarketplaceSecurityIT"
```

Expected GREEN: public endpoints are not 401; every listed protected endpoint
uses the shared 401; listing failures select approved descriptors; unexpected
domain invariants remain unexpected.

- [ ] **Step 8: Commit the listing and security-boundary slice**

```powershell
git add src/main/java/com/project/optrabidz/security/infrastructure/config/SecurityConfig.java `
        src/main/java/com/project/optrabidz/marketplace `
        src/test/java/com/project/optrabidz/marketplace/application/ListingServiceTest.java `
        src/test/java/com/project/optrabidz/marketplace/application/specification/MarketplaceSpecificationTest.java `
        src/test/java/com/project/optrabidz/marketplace/api/MarketplaceSecurityIT.java
git commit -m "refactor: migrate marketplace listing failures (KAN-28)"
```

Update Jira with the endpoint matrix and focused test evidence.

---

## Task 3: Migrate bid and agreement failures safely

**Files:**

- Modify: `src/main/java/com/project/optrabidz/marketplace/application/BidService.java`
- Modify: `src/main/java/com/project/optrabidz/marketplace/application/AgreementService.java`
- Modify: bid, agreement, ownership, and visibility specifications under
  `src/main/java/com/project/optrabidz/marketplace/application/specification/`
- Delete: `src/main/java/com/project/optrabidz/marketplace/application/exception/BidAlreadyAcceptedException.java`
- Modify: `src/test/java/com/project/optrabidz/marketplace/application/BidServiceTest.java`
- Create: `src/test/java/com/project/optrabidz/marketplace/application/AgreementServiceTest.java`
- Modify: `src/test/java/com/project/optrabidz/marketplace/application/specification/MarketplaceSpecificationTest.java`
- Modify: `src/test/java/com/project/optrabidz/marketplace/infrastructure/repository/MarketplaceRepositoryIT.java`

**Interfaces:**

- Produces: `BidAcceptanceConflictException` for precondition and conditional
  update conflicts.
- Preserves: existing transaction, event, outbox, agreement, and finance-port
  success behavior.
- Removes: broad `IllegalStateException` and
  `DataIntegrityViolationException` conversion.

- [ ] **Step 1: Write failing bid and agreement tests**

Rename all `BidAlreadyAcceptedException` assertions to
`BidAcceptanceConflictException`. Assert the descriptor is
`BID_ACCEPTANCE_CONFLICT` for both a closed listing and an already-accepted bid.

Add a zero-row conditional update test:

```java
when(listingRepository.markAgreementReachedIfOpen(
        eq(LISTING_ID), any(Instant.class)))
        .thenReturn(0);

assertThatThrownBy(() -> service.acceptBid(
        STARTUP_ACCOUNT_ID,
        RoleType.STARTUP,
        BID_ID,
        new BidActionRequest("Looks good", "CONFIRM")
))
        .isInstanceOf(BidAcceptanceConflictException.class)
        .extracting(failure -> ((ApplicationException) failure).descriptor())
        .isSameAs(MarketplaceErrors.BID_ACCEPTANCE_CONFLICT);

verify(bidRepository, never()).saveAndFlush(any());
verify(agreementRepository, never()).save(any());
verify(financeAgreementPort, never()).onAgreementCreated(any());
verify(eventPublisher, never()).publish(any());
```

Add a mocked unrelated persistence failure from `saveAndFlush` and assert the
same `DataIntegrityViolationException` escapes rather than becoming a 409.

Create `AgreementServiceTest` covering missing agreement and wrong-role access;
assert `AGREEMENT_NOT_FOUND` and `MARKETPLACE_ACCESS_DENIED` respectively.

In `MarketplaceRepositoryIT`, verify
`markAgreementReachedIfOpen(listingId, instant)` returns `1` for an open listing
and `0` on a second invocation after the state changed. Assert the persisted
state is `AGREEMENT_REACHED`.

- [ ] **Step 2: Run focused tests and preserve RED evidence**

```powershell
.\mvnw.cmd -B "-Dtest=BidServiceTest,AgreementServiceTest,MarketplaceSpecificationTest" test
.\mvnw.cmd -B verify -Pintegration-tests "-Dit.test=MarketplaceRepositoryIT"
```

Expected RED: the renamed acceptance exception is not used, the broad database
catch mislabels the unrelated failure, and agreement lookup still uses the
legacy diagnostic constructor.

- [ ] **Step 3: Migrate bid and agreement call sites**

Use `BID_NOT_FOUND`, `AGREEMENT_NOT_FOUND`, `BID_ALREADY_EXISTS`,
`BID_STATE_CONFLICT`, and `MARKETPLACE_ACCESS_DENIED` through their typed
exceptions. Keep identifiers and actual states only in diagnostic messages.

Replace both acceptance-spec branches and the conditional-update branch with:

```java
throw new BidAcceptanceConflictException(
        "Listing " + listing.getListingId()
                + " cannot accept bid " + bid.getBidId()
                + " in the current marketplace state"
);
```

Delete `BidAlreadyAcceptedException` only after `rg` confirms no production or
test caller remains.

- [ ] **Step 4: Remove broad exception conversion**

Delete `applyBidTransition` and call the aggregate transition directly after
its corresponding specification passes:

```java
bid.withdraw(now);
bid.reject(now);
bid.accept(now);
```

Remove the `try/catch (DataIntegrityViolationException)` around bid acceptance.
Keep the conditional `markAgreementReachedIfOpen` result as the recognised
concurrency boundary. This ensures unrelated persistence faults roll back and
remain unexpected instead of being exposed as a false 409.

- [ ] **Step 5: Run focused unit and PostgreSQL tests**

```powershell
.\mvnw.cmd -B "-Dtest=MarketplaceErrorContractTest,ListingServiceTest,BidServiceTest,AgreementServiceTest,MarketplaceSpecificationTest" test
.\mvnw.cmd -B verify -Pintegration-tests "-Dit.test=MarketplaceRepositoryIT"
```

Expected GREEN: all typed failure and no-side-effect assertions pass; the
conditional update returns the expected values against PostgreSQL; unrelated
persistence failures are not translated.

- [ ] **Step 6: Commit the bid and agreement slice**

```powershell
git add src/main/java/com/project/optrabidz/marketplace/application `
        src/test/java/com/project/optrabidz/marketplace/application `
        src/test/java/com/project/optrabidz/marketplace/infrastructure/repository/MarketplaceRepositoryIT.java
git commit -m "refactor: migrate marketplace bid and agreement failures (KAN-28)"
```

Add Jira evidence for mutation safety and the PostgreSQL conditional-update
test.

---

## Task 4: Verify the public Marketplace Problem Details contract

**Files:**

- Modify: `src/test/java/com/project/optrabidz/marketplace/api/MarketplaceApiIT.java`

**Interfaces:**

- Consumes: Tasks 1–3 exceptions, the existing REST adapter, security boundary,
  Testcontainers support, and marketplace fixture helpers.
- Produces: end-to-end evidence for the approved RFC 9457 marketplace contract.

- [ ] **Step 1: Replace legacy envelope assertions with an exact helper**

Add a helper that always checks the complete public boundary:

```java
private ResultMatcher[] marketplaceProblem(
        int status,
        String code,
        String detail,
        String instance,
        String requestId
) {
    return new ResultMatcher[] {
            result -> assertThat(result.getResponse().getStatus()).isEqualTo(status),
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
            jsonPath("$.status").value(status),
            jsonPath("$.code").value(code),
            jsonPath("$.detail").value(detail),
            jsonPath("$.instance").value(instance),
            jsonPath("$.requestId").value(requestId),
            jsonPath("$.timestamp").isString(),
            jsonPath("$.success").doesNotExist(),
            jsonPath("$.error").doesNotExist()
    };
}
```

If the existing test style makes an array awkward, use one local assertion
method over `MvcResult`; retain the same exact fields.

- [ ] **Step 2: Add representative tests for every catalogue family**

Use existing authenticated-client and lifecycle helpers to cover these exact
results:

| Scenario | Expected result |
|---|---|
| unknown listing detail | 404 `LISTING_NOT_FOUND` |
| unknown bid query | 404 `BID_NOT_FOUND` |
| unknown agreement query | 404 `AGREEMENT_NOT_FOUND` |
| wrong role or non-owner | 403 `MARKETPLACE_ACCESS_DENIED` |
| update/publish/close in invalid listing state | 409 `LISTING_STATE_CONFLICT` |
| withdraw/reject/accept in invalid bid state | 409 `BID_STATE_CONFLICT` |
| second active bid by the same investor | 409 `BID_ALREADY_EXISTS` |
| acceptance after another bid wins | 409 `BID_ACCEPTANCE_CONFLICT` |
| valid `EQUITY` listing request without a policy | 422 `UNSUPPORTED_FUNDING_MODEL` |

Every request supplies a unique synthetic `X-Request-Id`. For at least one
failure in each family, assert the raw body excludes the relevant numeric ID,
role, state name, internal diagnostic code, and legacy error message.

- [ ] **Step 3: Run the API test and preserve RED evidence**

```powershell
.\mvnw.cmd -B verify -Pintegration-tests "-Dit.test=MarketplaceApiIT"
```

Expected RED before assertion migration: the two existing marketplace failure
tests still expect the legacy `success/error` envelope, and missing catalogue
families lack exact Problem Details coverage.

- [ ] **Step 4: Complete the API assertions and verify GREEN**

```powershell
.\mvnw.cmd -B verify -Pintegration-tests "-Dit.test=MarketplaceApiIT,MarketplaceSecurityIT,MarketplaceRepositoryIT"
```

Expected GREEN: every marketplace error family uses the exact approved status,
code, safe detail, request metadata, and content type; success flows remain
green.

- [ ] **Step 5: Commit the API contract slice**

```powershell
git add src/test/java/com/project/optrabidz/marketplace/api/MarketplaceApiIT.java
git commit -m "test: verify marketplace problem details (KAN-28)"
```

Update Jira with focused API counts and intentional compatibility changes.

---

## Task 5: Enforce architecture and legacy removal

**Files:**

- Modify: `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java`
- Modify: `src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f`

**Interfaces:**

- Produces: a permanent prohibition against marketplace use of
  `common.api.exception`.
- Preserves: the frozen financial violations until the financial migration
  story resolves them.

- [ ] **Step 1: Make the architecture rule fail for marketplace**

Add `"..marketplace.."` to
`MIGRATED_MODULES_DO_NOT_USE_LEGACY_API_EXCEPTIONS` before deleting frozen
entries. Run:

```powershell
.\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
```

Expected RED if any production legacy reference remains. Do not enable ArchUnit
store creation or automatic updates.

- [ ] **Step 2: Remove exactly the resolved frozen entries**

Delete only lines describing the nine legacy marketplace exception classes,
their `ApiException` constructor calls, and their `ErrorCode` field access.
The current baseline contains 27 such marketplace entries. Keep every financial
entry unchanged.

- [ ] **Step 3: Run architecture and explicit source scans**

```powershell
.\mvnw.cmd -B "-Dtest=ExceptionArchitectureTest" test
if (rg -n "\b(ApiException|ErrorCode)\b" src/main/java/com/project/optrabidz/marketplace) {
    throw 'legacy marketplace exception dependency remains'
}
if (rg -n "BidAlreadyAcceptedException" src/main/java src/test) {
    throw 'obsolete acceptance exception remains'
}
git diff --check
```

Expected GREEN: all architecture rules pass, both scans are empty, and the
diff check exits zero.

- [ ] **Step 4: Commit the architecture slice**

```powershell
git add src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java `
        src/test/resources/archunit-store/5c2f7ae8-7609-459a-8ad7-49f65df73f4f
git commit -m "test: enforce marketplace error boundaries (KAN-28)"
```

Add the architecture and empty legacy-scan evidence to Jira.

---

## Task 6: Complete regression, documentation, and review handoff

**Files:**

- Modify: `docs/error-handling/README.md`
- Modify: `docs/error-handling/work-items/KAN-28-marketplace-error-migration/implementation-plan.md`

**Interfaces:**

- Consumes: the complete KAN-28 feature branch and all focused evidence.
- Produces: full regression evidence and one reviewable pull request targeting
  `develop`.

- [x] **Step 1: Run the complete unit suite**

```powershell
.\mvnw.cmd -B test
```

Expected: all unit and architecture tests pass with zero failures and errors.
Record the final count in the execution-evidence section added during this
step; it must be at least the pre-change baseline of 227.

- [x] **Step 2: Run the complete PostgreSQL integration suite**

```powershell
docker version
.\mvnw.cmd -B verify -Pintegration-tests
```

Expected: Docker is available; all unit and PostgreSQL integration tests pass;
Flyway applies and validates unchanged V1. The integration count must be at
least the pre-change baseline of 76.

- [x] **Step 3: Verify scope, disclosure, and protected paths**

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
rg -n "\b(ApiException|ErrorCode)\b" src/main/java/com/project/optrabidz/marketplace
git status --short
```

Expected: only approved KAN-28 paths differ; protected paths have no diff; the
legacy scan is empty. `SecurityConfig` is the only approved runtime
configuration-class change.

- [x] **Step 4: Update navigation and execution evidence**

Update `docs/error-handling/README.md` so the current-system paragraph includes
marketplace and the KAN-28 row links both design and implementation plan.

Append an execution-evidence section to this plan containing only observed
facts:

- focused RED and GREEN results per task;
- focused unit and integration counts;
- complete unit and integration counts;
- legacy, disclosure, and architecture results;
- protected-path and Flyway V1 verification;
- exact `origin/develop` base and reviewed feature head; and
- Jira and pull-request state.

Do not add machine-specific paths, usernames, tokens, temporary log names,
tool references, or unverified claims.

- [ ] **Step 5: Commit and push the evidence**

```powershell
git add docs/error-handling/README.md `
        docs/error-handling/work-items/KAN-28-marketplace-error-migration/implementation-plan.md
git commit -m "docs: record marketplace migration evidence (KAN-28)"
git push origin feature/KAN-28-marketplace-error-migration
```

- [ ] **Step 6: Open one pull request into `develop`**

Prepare `.git/KAN-28-pr-body.md` with a professional summary, intentional
response changes, security-boundary matrix, verification counts, risk and
rollback notes, and Jira key. Then run:

```powershell
gh pr create `
    --base develop `
    --head feature/KAN-28-marketplace-error-migration `
    --title "KAN-28: Migrate marketplace failures to the neutral error contract" `
    --body-file .git\KAN-28-pr-body.md
```

Verify the PR base is `develop`, the head equals the tested commit, the diff is
limited to approved scope, and required CI checks pass.

- [ ] **Step 7: Update Jira to In Review and wait**

Add the PR link, exact reviewed head, test counts, intentional contract changes,
and rollback summary to KAN-28. Transition **In Progress → In Review** only
after CI is green. Do not merge or mark Done during this step.

- [ ] **Step 8: Merge only after explicit PR approval**

After review approval, re-check that the PR head and CI results are unchanged,
merge into `develop`, fetch remote state, and verify the merge contains the
reviewed head. Add final evidence to Jira, transition KAN-28 to **Done**, and
delete the merged local and remote feature branch. `main` remains unchanged.

---

## Execution evidence

Observed on the approved feature branch before review handoff:

- Branch base and current remote `develop`:
  `b0aa4adab85c07f2e435980298ed550aea44db95`.
- Verified implementation head before the evidence-only documentation commit:
  `54518e4d8595e79b5ba515b47589750375f320de`.
- Task 1 RED failed compilation before the module contract existed; GREEN passed
  26 focused marketplace tests.
- Task 2 RED produced one unit failure and five security failures; GREEN passed
  26 focused unit tests and 6 security integration tests.
- Task 3 RED produced five focused failures; GREEN passed 33 focused unit tests
  and 5 repository integration tests.
- Task 4 RED retained one legacy-envelope assertion failure; GREEN passed 21
  focused integration tests: 10 API, 6 security, and 5 repository tests.
- Task 5 RED rejected 27 obsolete frozen marketplace violations; GREEN passed
  all 3 architecture tests. The financial frozen baseline remained unchanged.
- The first full integration run exposed 13 order-dependent CSRF test failures.
  The isolated RED sequence failed 3 of 9 tests. After replacing the mutating
  test helper with the real cookie/header exchange, the same sequence passed
  9 of 9 tests.
- Final regression passed 253 unit and architecture tests and 86 PostgreSQL
  integration tests with zero failures, errors, or skipped tests.
- Marketplace production code has zero `ApiException` or `ErrorCode`
  references, and public contract tests reject protected identifiers and
  diagnostics.
- Protected build, CI, Flyway, and runtime-property paths have no diff. Flyway
  V1 has the same blob `8784c468aa169952a87e726303d03abae4376add`
  at the branch base and verified implementation head.
- Local and remote `main` remain at
  `bc7727b0b2e09ebbfef8b9c6c5dc729cd4aab4fb`.
- Jira is **In Progress**. No pull request has been opened or merged at this
  evidence checkpoint.

---

## Completion checklist

- [x] Approved descriptors and typed marketplace exceptions are implemented.
- [x] Public listing browse and detail remain anonymous.
- [x] Actor-required marketplace endpoints use the shared authentication
      boundary and 401 Problem Details.
- [x] Marketplace controllers contain no local legacy authentication error.
- [x] Listing, bid, agreement, access, state, duplicate, acceptance, and
      unsupported-model failures use approved semantics.
- [x] Known failures stop before prohibited writes and side effects.
- [x] Unexpected domain and unrelated persistence failures are not mislabeled.
- [x] Marketplace production code has no legacy exception dependency.
- [x] Public responses contain no protected diagnostic context.
- [x] Architecture, focused, full unit, and PostgreSQL integration tests pass.
- [x] Flyway V1, dependencies, runtime properties, sessions, roles, CSRF, and
      `main` remain unchanged.
- [ ] Pull request targets `develop` and Jira matches the actual delivery stage.
