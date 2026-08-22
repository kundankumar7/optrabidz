# KAN-28 — Marketplace Error Migration

**Status:** Approved design baseline

**Date:** 2026-08-22

**Jira:** [KAN-28](https://0707manna0895.atlassian.net/browse/KAN-28)

**Parent epic:** KAN-16 — Establish production exception-handling foundation

## 1. Outcome

Migrate expected marketplace failures from the legacy HTTP-coupled
`ApiException` and `ErrorCode` model to the transport-neutral
`ApplicationException` contract.

Listings, bids, and agreements will expose stable, allowlisted errors through
the existing RFC 9457 REST adapter. Spring Security will enforce authentication
for actor-required marketplace endpoints, while public listing discovery and
listing detail remain anonymously accessible.

## 2. Scope

KAN-28 includes:

- a marketplace-owned public error catalogue;
- typed listing, bid, agreement, authorization, state, and funding-model
  exceptions;
- migration of marketplace services, specifications, policies, and controllers;
- a narrow Spring Security matcher correction for actor-required marketplace
  endpoints;
- precise handling of concurrent bid-acceptance conflicts;
- focused contract, service, specification, API, security, persistence,
  architecture, and regression tests; and
- removal of all `ApiException` and legacy `ErrorCode` dependencies from the
  marketplace module.

KAN-28 does not include:

- JWT, OAuth2, session-policy, role, CSRF, or general authorization redesign;
- changes to listing, bid, agreement, or recommendation business rules;
- database, Flyway, persistence-schema, or seed-data changes;
- financial or notification error migration;
- payment, notification, logging, audit, outbox, or worker redesign;
- real-port or deployed smoke-test implementation;
- removal of legacy exception infrastructure still used by other modules; or
- new runtime dependencies, AOP, messaging, or caching.

## 3. Chosen architecture

The marketplace module owns one `MarketplaceErrors` catalogue and typed
application exceptions. The existing neutral core, RFC 9457 REST adapter, and
Spring Security response writer remain unchanged.

```text
HTTP request
    |
    +-- actor-required marketplace endpoint
    |       -> Spring Security
    |           +-- anonymous -> shared AUTHENTICATION_REQUIRED 401
    |           `-- authenticated -> controller
    |
    `-- public listing browse or detail endpoint
            -> controller, with an optional authenticated actor
                    |
                    `-- marketplace service/specification
                            +-- expected failure
                            |       -> typed ApplicationException
                            |               -> existing REST adapter
                            |                       -> RFC 9457 Problem Details
                            `-- success -> existing success response
```

This architecture was selected over:

1. one generic marketplace exception with reason strings, which would obscure
   business meaning and encourage message-based tests;
2. a global catalogue containing marketplace codes, which would weaken module
   ownership and couple unrelated capabilities; and
3. controller-local authentication exceptions, which duplicate Spring
   Security, bypass its audit boundary, and retain legacy HTTP coupling.

## 4. Authentication boundary and future compatibility

Spring Security will enforce authentication for:

- listing create, update, publish, and close operations;
- recommended listings;
- all bid commands and queries;
- accepted-bid queries; and
- all agreement queries.

The existing `/startups/**` and `/investors/**` rules continue protecting their
current actor-specific endpoints. Anonymous access remains available for:

- browsing open listings; and
- requesting listing detail, subject to the existing visibility rule.

Controller-local `requirePrincipal` methods are removed. Missing authentication
on a protected endpoint is therefore handled before controller execution by the
existing Spring Security entry point, which returns the standard
`AUTHENTICATION_REQUIRED` 401 Problem Details response and records the existing
security audit.

These request matchers describe access requirements rather than the credential
mechanism. Marketplace services continue receiving only `accountId` and
`role`. A future Spring Security story may replace session authentication with
JWT/OAuth2 and adapt the authenticated identity to the application's principal
contract without redesigning marketplace services.

KAN-28 does not introduce a speculative authentication abstraction. The
current principal already serves as the web-security adaptation point.

## 5. Responsibility boundaries

- `MarketplaceErrors` owns stable public codes, categories, and safe details.
- Typed exceptions select one descriptor and retain identifiers, actions, and
  observed state only in protected diagnostics.
- Controllers delegate expected failures and do not construct error bodies.
- Spring Security owns authentication failures for protected endpoints.
- Marketplace services and specifications own resource, permission, lifecycle,
  duplicate, and funding-model failures.
- The REST adapter remains the only constructor of application Problem Details.
- Expected marketplace failures are not logged again by the REST adapter.
- Unexpected invariant or persistence failures are not relabelled as expected
  marketplace errors.
- Successful writes, event publication, outbox behavior, and finance-port calls
  retain their existing transactional boundaries.

## 6. Public error catalogue

| Code | Category | HTTP | Public detail |
|---|---|---:|---|
| `LISTING_NOT_FOUND` | `NOT_FOUND` | 404 | The requested listing was not found |
| `BID_NOT_FOUND` | `NOT_FOUND` | 404 | The requested bid was not found |
| `AGREEMENT_NOT_FOUND` | `NOT_FOUND` | 404 | The requested agreement was not found |
| `MARKETPLACE_ACCESS_DENIED` | `AUTHORIZATION` | 403 | You are not authorized to perform this marketplace action |
| `LISTING_STATE_CONFLICT` | `CONFLICT` | 409 | The requested action conflicts with the current listing state |
| `BID_STATE_CONFLICT` | `CONFLICT` | 409 | The requested action conflicts with the current bid state |
| `BID_ALREADY_EXISTS` | `CONFLICT` | 409 | An active bid already exists for this listing |
| `BID_ACCEPTANCE_CONFLICT` | `CONFLICT` | 409 | The bid cannot be accepted in the current marketplace state |
| `UNSUPPORTED_FUNDING_MODEL` | `BUSINESS_RULE` | 422 | The requested funding model is not supported |

Entity names already identify the marketplace concepts, so the public codes do
not repeat `MARKETPLACE_` unnecessarily. The prefix remains on
`MARKETPLACE_ACCESS_DENIED` because an unqualified access-denied code would be
too generic.

`HttpErrorMapping` continues deriving HTTP status and title from the neutral
category. Marketplace descriptors do not import or store HTTP types.

## 7. Typed failures and migration mapping

The existing marketplace exception classes migrate to `ApplicationException`.
`BidAlreadyAcceptedException` is replaced by the more accurate
`BidAcceptanceConflictException`, because the current exception also represents
closed listings, competing acceptance, and one-accepted-bid concurrency
conflicts.

| Failure | Neutral result |
|---|---|
| Listing lookup fails | `LISTING_NOT_FOUND` |
| Bid lookup fails | `BID_NOT_FOUND` |
| Agreement lookup fails | `AGREEMENT_NOT_FOUND` |
| Actor role, ownership, or visibility check fails | `MARKETPLACE_ACCESS_DENIED` |
| Listing action is invalid for its current state | `LISTING_STATE_CONFLICT` |
| Bid action is invalid for its current state | `BID_STATE_CONFLICT` |
| Investor already has an active bid for the listing | `BID_ALREADY_EXISTS` |
| Listing cannot accept the bid or another bid wins concurrently | `BID_ACCEPTANCE_CONFLICT` |
| No policy supports the requested funding model | `UNSUPPORTED_FUNDING_MODEL` |

An anonymous request for a non-public listing continues to fail the existing
visibility rule with `MARKETPLACE_ACCESS_DENIED`. KAN-28 does not redesign
resource-enumeration or listing-visibility policy.

Malformed JSON, invalid enum syntax, blank fields, and structural DTO
violations remain MVC validation failures with status 400. A syntactically
valid but unsupported funding model is a semantic business-rule failure with
status 422.

## 8. Disclosure policy

Every marketplace Problem Details response contains only the existing public
properties:

- `type`;
- `title`;
- `status`;
- `detail`;
- `instance`;
- `code`;
- `requestId`; and
- `timestamp`.

Public responses do not contain account, participant, listing, bid, or
agreement IDs; roles; actual internal states; action names; class names;
database constraint names; raw exception messages; stack traces; or diagnostic
codes. Those values may appear only in protected diagnostic context where they
are useful and non-secret.

## 9. Failure and transaction behavior

- Known resource, permission, lifecycle, and duplicate checks occur before
  mutation or event publication where the current flow permits it.
- Existing broad conversion of arbitrary `IllegalStateException` failures into
  state conflicts is removed. Specifications cover known state preconditions;
  an unexpected domain invariant failure remains an internal 500 and rolls back.
- The conditional listing-state update remains the primary concurrent
  bid-acceptance guard.
- Only verified acceptance-related constraint violations may be translated to
  `BID_ACCEPTANCE_CONFLICT`. An unrelated `DataIntegrityViolationException`
  remains unexpected and rolls back rather than being mislabeled as 409.
- A rejected operation must not save a new aggregate, publish an event, append
  outbox work, or invoke the finance agreement port.
- Successful transaction, event, outbox, and finance behavior remains
  unchanged.

## 10. Public contract and compatibility

Every migrated expected marketplace failure uses
`application/problem+json` and the existing RFC 9457 response shape.

Intentional compatibility changes are limited to marketplace failures:

- the legacy success/error envelope becomes Problem Details;
- generic `RESOURCE_NOT_FOUND`, `AUTHORIZATION_FAILED`, and
  `INVALID_STATE_TRANSITION` codes become entity-specific codes;
- the inaccurate `BID_ALREADY_ACCEPTED` code becomes
  `BID_ACCEPTANCE_CONFLICT` where appropriate;
- a valid but unsupported funding model changes from structural 400 to semantic
  422; and
- anonymous access to actor-required marketplace endpoints is rejected by the
  shared Spring Security boundary before controller execution.

Successful statuses, response bodies, pagination, persistence, events,
recommendations, governance decisions, and finance integration remain
unchanged.

## 11. Verification strategy

### 11.1 Catalogue and exception tests

- Assert every descriptor's exact code, category, and public detail.
- Assert every typed exception selects the intended descriptor and a stable
  uppercase diagnostic code.
- Prove identifiers, roles, states, raw messages, and diagnostics do not enter
  generated Problem Details.

### 11.2 Service and specification tests

- Cover listing, bid, and agreement not-found paths separately.
- Cover role, ownership, visibility, listing-state, bid-state, duplicate-bid,
  acceptance-conflict, and unsupported-model failures.
- Verify rejected commands do not save, publish events, append outbox work, or
  invoke the finance agreement port.
- Verify unexpected invariant failures are not relabelled as expected errors.

### 11.3 API and security tests

- Assert exact status, `application/problem+json`, code, safe detail, instance,
  request ID, and timestamp for representative failures from each family.
- Assert the legacy `success` and `error` envelope fields are absent.
- Prove public listing browse and detail remain anonymously accessible.
- Prove actor-required marketplace endpoints return the existing shared 401
  response before controller execution when unauthenticated.
- Preserve CSRF behavior for authenticated state-changing requests.

### 11.4 Persistence, architecture, and regression tests

- Verify competing or repeated bid acceptance produces the approved 409 while
  unrelated persistence failures are not translated.
- Assert marketplace production code contains no `ApiException` or legacy
  `ErrorCode` reference.
- Assert marketplace application errors contain no HTTP, servlet, Spring Web,
  or `common.api` dependency.
- Run focused marketplace tests, the complete unit suite, and the complete
  PostgreSQL integration suite against unchanged Flyway V1.

MockMvc and Testcontainers remain test-only tools. The small real-port HTTP
suite approved by KAN-17 remains a later dedicated epic story rather than being
mixed into this module migration.

## 12. Delivery sequence

One KAN-28 feature branch targets `develop` and remains reviewable in four
implementation slices:

1. add the marketplace catalogue, typed exceptions, and contract tests;
2. migrate listing failures and correct the marketplace authentication
   boundary;
3. migrate bid and agreement failures, including precise acceptance-conflict
   handling and mutation-safety tests; and
4. add API, security, persistence, and architecture regressions and run full
   verification.

No production implementation begins until the written specification and the
subsequent implementation plan are reviewed and approved.

## 13. Acceptance criteria

- [ ] Every expected marketplace failure uses the approved catalogue.
- [ ] Actor-required marketplace endpoints are enforced by Spring Security.
- [ ] Public listing browse and detail behavior remains available anonymously.
- [ ] Marketplace controllers contain no local legacy authentication error.
- [ ] Marketplace production code contains no `ApiException` or legacy
      `ErrorCode` dependency.
- [ ] Marketplace application errors remain transport-neutral.
- [ ] Listing, bid, agreement, access, state, duplicate, acceptance, and
      funding-model failures use their approved status semantics.
- [ ] Public responses exclude internal identifiers, roles, states, action
      names, class names, constraint names, raw messages, and diagnostics.
- [ ] Known failures occur before prohibited writes and side effects.
- [ ] Unexpected invariant and unrelated database failures are not mislabeled
      as expected marketplace errors.
- [ ] Successful marketplace, governance, event, outbox, and finance behavior
      remains unchanged.
- [ ] JWT/OAuth2 can later replace session authentication without redesigning
      marketplace services.
- [ ] Full unit and PostgreSQL integration verification passes with unchanged
      Flyway V1.
- [ ] No out-of-scope database, authentication-strategy, financial,
      notification, logging, audit, worker, runtime, or deployment change is
      included.
