# KAN-26 — Classification Error Migration

**Status:** Approved design baseline

**Date:** 2026-08-21

**Jira:** [KAN-26](https://0707manna0895.atlassian.net/browse/KAN-26)

**Parent epic:** KAN-16 — Establish production exception-handling foundation

## 1. Outcome

Migrate expected failures in the `classification` module from the legacy
HTTP-coupled `ApiException` and `ErrorCode` model to the transport-neutral
`ApplicationException` contract.

Startup classifications and investor preferences will expose distinct,
allowlisted errors through the existing RFC 9457 REST adapter. Successful
responses, persistence, events, and modules outside classification remain
unchanged.

## 2. Scope

KAN-26 includes:

- a module-owned catalogue containing startup-classification and
  investor-preference error descriptors;
- actor-specific typed application exceptions;
- migration of expected profile-prerequisite, duplicate, missing-entry, and
  rule-violation failures;
- safe public details separated from protected diagnostic context;
- focused catalogue, service, API-contract, architecture, and regression tests;
  and
- removal of all legacy exception dependencies from the classification module.

KAN-26 does not include:

- migration or removal of legacy errors still used by other modules;
- changes to authentication, authorization, CSRF, or endpoint permissions;
- classification rule, cardinality, or lifecycle redesign;
- database, Flyway, persistence-schema, or seed-data changes;
- event schema, transaction, or successful response changes;
- generic unexpected-500 handling; or
- new dependencies, AOP, messaging, caching, or runtime configuration.

## 3. Chosen architecture

The classification module owns one `ClassificationErrors` catalogue and typed
exceptions that express business intent. The existing neutral core and REST
adapter remain unchanged.

```text
HTTP request
    |
    +-- malformed JSON or invalid request fields
    |       -> MVC validation adapter -> 400 Problem Details
    |
    `-- structurally valid request
            -> classification service and rule engine
                    -> typed ApplicationException
                            -> existing REST adapter
                                    -> RFC 9457 Problem Details
```

This design was selected over:

1. constructing `ApplicationException` directly at each throw site, which
   duplicates descriptors and obscures business meaning; and
2. adding classification errors to a global catalogue, which weakens module
   ownership and couples unrelated business capabilities.

## 4. Responsibility boundaries

- `ClassificationErrors` owns stable public codes, categories, and details.
- Typed classification exceptions own stable diagnostic codes and protected
  diagnostic messages.
- Services explicitly identify profile prerequisites, duplicates, and missing
  entries before performing domain mutations.
- Startup specifications and type policies raise startup rule violations;
  investor specifications and type policies raise investor rule violations.
- Public response construction reads only the allowlisted descriptor.
- Account IDs, participant IDs, classification types and values, class names,
  raw exception messages, and diagnostic codes are not public response data.
- Classification error types do not depend on Spring Web, servlet APIs, HTTP
  status types, `common.api`, `ApiException`, or legacy `ErrorCode`.
- Expected failures are not logged again by the REST adapter.
- Unexpected `IllegalStateException` failures are not broadly caught or
  relabelled as business errors. Explicit precondition checks cover known
  failure paths; an unexpected domain-state failure remains an internal error.

## 5. Public error catalogue

### 5.1 Startup classifications

| Code | Category | HTTP | Public detail |
|---|---|---:|---|
| `STARTUP_CLASSIFICATION_PROFILE_REQUIRED` | `BUSINESS_RULE` | 422 | Create a startup profile before managing classifications |
| `STARTUP_CLASSIFICATION_ALREADY_EXISTS` | `CONFLICT` | 409 | The startup classification already exists |
| `STARTUP_CLASSIFICATION_NOT_FOUND` | `NOT_FOUND` | 404 | The requested startup classification was not found |
| `STARTUP_CLASSIFICATION_RULE_VIOLATION` | `BUSINESS_RULE` | 422 | The startup classification does not satisfy classification rules |

### 5.2 Investor preferences

| Code | Category | HTTP | Public detail |
|---|---|---:|---|
| `INVESTOR_PREFERENCE_PROFILE_REQUIRED` | `BUSINESS_RULE` | 422 | Create an investor profile before managing preferences |
| `INVESTOR_PREFERENCE_ALREADY_EXISTS` | `CONFLICT` | 409 | The investor preference already exists |
| `INVESTOR_PREFERENCE_NOT_FOUND` | `NOT_FOUND` | 404 | The requested investor preference was not found |
| `INVESTOR_PREFERENCE_RULE_VIOLATION` | `BUSINESS_RULE` | 422 | The investor preference does not satisfy preference rules |

The existing `HttpErrorMapping` determines status and title from the neutral
category. A descriptor does not store an HTTP status.

## 6. Status semantics

- `400 Bad Request` remains reserved for malformed JSON and structural request
  validation such as blank or missing fields.
- `409 Conflict` represents an entry that already exists in the current
  profile state.
- `404 Not Found` represents a requested classification or preference entry
  that does not exist for removal.
- `422 Unprocessable Content` represents a structurally valid instruction that
  cannot be processed because its participant profile is absent or a
  classification business rule rejects it.

These decisions follow the HTTP status semantics in
[RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html). The response contract
follows [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html): stable machine
fields carry client semantics, while `detail` remains safe human-readable text
rather than a debugging channel.

## 7. Typed failures and migration mapping

The generic legacy exceptions `ClassificationAlreadyExistsException` and
`InvalidClassificationException` are replaced by actor-specific neutral
exceptions:

- `StartupClassificationProfileRequiredException`;
- `StartupClassificationAlreadyExistsException`;
- `StartupClassificationNotFoundException`;
- `StartupClassificationRuleViolationException`;
- `InvestorPreferenceProfileRequiredException`;
- `InvestorPreferenceAlreadyExistsException`;
- `InvestorPreferenceNotFoundException`; and
- `InvestorPreferenceRuleViolationException`.

Each typed exception extends `ApplicationException`, selects exactly one
descriptor, uses an uppercase segmented diagnostic code, and may include
protected identifiers or rejected values only in its diagnostic message.

The service and rule mapping is:

| Current failure location | Neutral result |
|---|---|
| Startup account has no startup participant | `STARTUP_CLASSIFICATION_PROFILE_REQUIRED` |
| Investor account has no investor participant | `INVESTOR_PREFERENCE_PROFILE_REQUIRED` |
| Add operation finds an existing entry | Actor-specific `*_ALREADY_EXISTS` |
| Remove operation cannot find the requested entry | Actor-specific `*_NOT_FOUND` |
| Integrity, cardinality, uniqueness, or type policy rejects validly shaped input | Actor-specific `*_RULE_VIOLATION` or `*_ALREADY_EXISTS` according to the rule |

Uniqueness specifications use the actor-specific duplicate exception rather
than the general rule-violation exception. This preserves the more precise 409
contract.

## 8. Public contract and compatibility

Every migrated expected failure uses `application/problem+json` with the
existing properties:

- `type`;
- `title`;
- `status`;
- `detail`;
- `instance`;
- `code`;
- `requestId`; and
- `timestamp`.

Intentional compatibility changes are limited to classification failures:

- the legacy success/error envelope becomes RFC 9457 Problem Details;
- generic `RESOURCE_NOT_FOUND`, `DUPLICATE_OPERATION`, and `VALIDATION_ERROR`
  codes become the actor-specific codes in this design; and
- structurally valid classification-rule failures use semantic 422 rather than
  structural 400.

Successful statuses, response bodies, transactions, repository writes, and
published classification-change events remain unchanged.

## 9. Verification strategy

### 9.1 Catalogue and exception tests

- Assert every descriptor's exact code, category, and public detail.
- Assert every typed exception selects the intended descriptor and diagnostic
  code.
- Prove protected identifiers and rejected values are absent from generated
  Problem Details.

### 9.2 Service and rule tests

- Cover profile prerequisite, duplicate, missing-entry, integrity,
  cardinality, uniqueness, and type-policy failures separately for startup and
  investor flows.
- Verify known failures occur before repository writes and event publication.
- Preserve explicit successful add, replace, remove, and query behaviour.
- Verify expected precondition paths without broad-catching unexpected domain
  exceptions.

### 9.3 API and architecture tests

- Assert `application/problem+json`, HTTP status, stable code, safe detail, and
  request metadata for representative startup and investor failures.
- Preserve MVC validation coverage showing malformed or blank input remains
  `VALIDATION_ERROR` with status 400.
- Assert the classification module has no reference to `ApiException` or
  legacy `ErrorCode`.
- Assert classification application and domain errors have no HTTP, servlet,
  Spring Web, or `common.api` dependency.

### 9.4 Regression verification

- Run focused classification tests during development.
- Run the complete unit suite.
- Run the complete PostgreSQL integration suite against the unchanged Flyway
  V1 schema.

## 10. Delivery sequence

One KAN-26 feature branch targets `develop` and remains reviewable in four
implementation slices:

1. add the classification catalogue, typed exceptions, and focused contract
   tests;
2. migrate startup services, rules, policies, and tests;
3. migrate investor services, rules, policies, and tests; and
4. update API and architecture regressions and run complete verification.

The common neutral contract, REST adapter, legacy users outside classification,
and `main` branch are unchanged by this story.

## 11. Acceptance criteria

- [ ] Startup classification failures use the approved startup catalogue.
- [ ] Investor preference failures use the approved investor catalogue.
- [ ] Profile prerequisite, duplicate, missing-entry, and rule failures produce
  their approved categories and HTTP statuses.
- [ ] Malformed and structurally invalid requests remain 400 validation errors.
- [ ] Public responses contain no identifiers, rejected values, class names,
  raw exception messages, or diagnostic codes.
- [ ] Known failure paths do not write repositories or publish events.
- [ ] Unexpected domain-state failures are not mislabeled as expected errors.
- [ ] The classification module contains no `ApiException` or legacy
  `ErrorCode` dependency.
- [ ] Modules outside classification retain their current behaviour.
- [ ] Existing successful classification API and event behaviour remains
  unchanged.
- [ ] Full unit and PostgreSQL integration verification passes with unchanged
  Flyway V1.
- [ ] No out-of-scope schema, dependency, security, runtime configuration, or
  deployment change is included.
