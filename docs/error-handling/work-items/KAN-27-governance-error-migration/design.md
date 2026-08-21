# KAN-27 — Governance Error Migration

**Status:** Approved design baseline

**Date:** 2026-08-21

**Jira:** [KAN-27](https://0707manna0895.atlassian.net/browse/KAN-27)

**Parent epic:** KAN-16 — Establish production exception-handling foundation

## 1. Outcome

Migrate governance failures from the legacy HTTP-coupled `ApiException` and
`ErrorCode` model to the transport-neutral `ApplicationException` contract.

Governance decisions remain detailed inside the application, while clients
receive a small, stable catalogue grouped by the action they can take next.
Admin recovery failures use a disclosure-safe response that never reveals
whether recovery mode, server configuration, or the submitted token caused the
denial.

## 2. Scope

KAN-27 includes:

- a governance-owned public error catalogue;
- deterministic mapping from internal `GovernanceRuleCode` values to grouped
  public errors;
- migration of `GovernanceException` to the neutral exception contract;
- typed admin-recovery and unavailable-authority exceptions;
- removal of direct legacy exception construction from
  `AdminRecoveryController` and `AdministrativeAuthorityGuard`;
- focused governance decision, recovery-access, service, API-contract,
  disclosure, architecture, and regression tests; and
- removal of all `ApiException` and legacy `ErrorCode` dependencies from the
  governance module.

KAN-27 does not include:

- administrator reinstatement, reactivation, or record redesign;
- changes to admin bootstrap, transfer, revocation, or credential semantics;
- database, Flyway, persistence-schema, or seed-data changes;
- authentication, authorization, CSRF, or endpoint-permission redesign;
- migration of marketplace, financial, notification, or other module errors;
- scheduled-worker retry, logging, or audit redesign;
- removal of legacy exception infrastructure still used by other modules; or
- new dependencies, AOP, messaging, caching, or runtime configuration.

## 3. Chosen architecture

The governance module keeps its detailed decision model and adds a small public
catalogue at the exception boundary.

```text
Governed operation
    |
    +-- GovernanceDecision.allowed = true
    |       -> preserve the successful operation
    |
    `-- GovernanceDecision.allowed = false
            -> GovernanceException
                    -> map internal GovernanceRuleCode
                       to grouped GovernanceErrors descriptor
                            -> existing REST adapter
                                    -> RFC 9457 Problem Details

Admin recovery request
    |
    +-- recovery access rejected for any reason
    |       -> AdminRecoveryAccessDeniedException
    |               -> one disclosure-safe 403 response
    |
    `-- access accepted but no authority can be transferred
            -> AdminAuthorityUnavailableException
                    -> 409 state-conflict response
```

The grouped catalogue was selected over:

1. one generic governance code, which gives clients too little information to
   choose a remedy; and
2. publishing every `GovernanceRuleCode`, which exposes internal policy detail,
   creates an unnecessarily large public API, and makes internal rule changes
   breaking changes.

## 4. Responsibility boundaries

- `GovernanceErrors` owns stable public codes, categories, and safe details.
- `GovernanceRuleCode` remains an internal policy vocabulary and is not a
  public API contract.
- `GovernanceException` selects a grouped public descriptor from a denied
  `GovernanceDecision` and stores the internal rule and decision message only
  as protected diagnostics.
- `AdminRecoveryAccessDeniedException` represents every recovery-access denial.
  Its factory methods may distinguish internal diagnostic causes, but its
  public descriptor is always identical.
- `AdminAuthorityUnavailableException` represents an accepted transfer request
  that conflicts with the absence of an active administrator authority.
- `AdminRecoveryController` validates the recovery boundary but owns no HTTP
  error body construction.
- `AdministrativeAuthorityGuard` expresses governance failures through neutral
  exceptions and remains independent of Spring Web and servlet APIs.
- The existing REST adapter remains the sole constructor of application
  Problem Details.
- Recovery tokens, configured secrets, submitted header values, account IDs,
  roles, states, action names, module names, internal rule codes, raw exception
  messages, and diagnostic codes are never public response data.
- Expected governance failures are not logged again by the REST adapter.

## 5. Public error catalogue

| Code | Category | HTTP | Public detail |
|---|---|---:|---|
| `GOVERNANCE_ACTION_NOT_ELIGIBLE` | `BUSINESS_RULE` | 422 | The requested action does not satisfy governance eligibility rules |
| `GOVERNANCE_ACTION_NOT_PERMITTED` | `AUTHORIZATION` | 403 | The requested action is not permitted by governance policy |
| `GOVERNANCE_STATE_CONFLICT` | `CONFLICT` | 409 | The requested action conflicts with the current governed state |
| `ADMIN_RECOVERY_ACCESS_DENIED` | `AUTHORIZATION` | 403 | Admin recovery access was denied |
| `ADMIN_AUTHORITY_UNAVAILABLE` | `CONFLICT` | 409 | No active administrator authority is available for transfer |

`HttpErrorMapping` continues to determine the HTTP status and title from the
neutral category. Governance descriptors do not import or store HTTP types.

## 6. Internal-to-public mapping

| Internal governance rule | Public error |
|---|---|
| `ACCOUNT_NOT_FOUND` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `ROLE_MISMATCH` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `ACCOUNT_NOT_ACTIVE` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `PROFILE_INCOMPLETE` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `STARTUP_ACTOR_NOT_FOUND` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `INVESTOR_ACTOR_NOT_FOUND` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `STARTUP_CLASSIFICATION_REQUIRED` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `INVESTOR_PREFERENCE_REQUIRED` | `GOVERNANCE_ACTION_NOT_ELIGIBLE` |
| `ADMIN_AUTHORITY_REQUIRED` | `GOVERNANCE_ACTION_NOT_PERMITTED` |
| `NEUTRALITY_VIOLATION` | `GOVERNANCE_ACTION_NOT_PERMITTED` |
| `SYSTEM_BOUNDARY_VIOLATION` | `GOVERNANCE_ACTION_NOT_PERMITTED` |
| `RECOVERY_MODE_REQUIRED` | `ADMIN_RECOVERY_ACCESS_DENIED` |
| `LIFECYCLE_RULE_SKIPPED` | `GOVERNANCE_STATE_CONFLICT` |
| `LIFECYCLE_RULE_FAILED` | `GOVERNANCE_STATE_CONFLICT` |

`ALLOWED` is not a failure and must never be accepted by the exception mapping.
Attempting to construct `GovernanceException` from an allowed decision is a
programming error and fails immediately before any response mapping.

Grouping deliberately prevents a client from discovering whether an account
exists, which role or state was observed, or which profile prerequisite is
missing. Detailed decisions remain available to trusted in-process callers
that explicitly request evaluation rather than assertion.

## 7. Admin recovery disclosure policy

The following internal causes all produce the exact same public contract:

- recovery mode is disabled;
- the server recovery token is absent or blank;
- the request header is missing or blank; and
- the submitted token does not match.

Internally, stable diagnostic codes distinguish these causes for protected
operations and testing. Diagnostics never contain the configured token, the
submitted token, request headers, credentials, or raw secret values.

Token comparison continues to use `MessageDigest.isEqual`; KAN-27 does not
weaken or replace the constant-work comparison path.

Once access is accepted, a missing active authority produces
`ADMIN_AUTHORITY_UNAVAILABLE` with status 409. This intentionally replaces the
legacy generic 404 because the transfer operation conflicts with current
system state; it is not a request to retrieve an administrator resource.

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

Intentional compatibility changes are limited to governance failures:

- the legacy error envelope becomes RFC 9457 Problem Details;
- generic authorization, not-found, and invalid-state codes become the grouped
  governance codes in this design;
- governance eligibility denials use semantic 422;
- governed state conflicts use 409; and
- every recovery-access denial becomes indistinguishable in its public body.

Successful statuses, response bodies, transactions, repository operations,
admin authority transitions, and published events remain unchanged.

## 9. Failure-flow behavior

- A denied governance assertion fails before the protected marketplace or
  governance mutation continues.
- Evaluation methods continue returning complete internal
  `GovernanceDecision` objects to trusted in-process consumers.
- Assertion methods translate denied decisions to the neutral exception
  contract without altering evaluation rules.
- Recovery access is checked before `AdminAuthorityTransferService` is called.
- An unavailable authority is detected before revocation, credential changes,
  account deactivation, replacement-admin creation, or event publication.
- Structural request validation remains owned by the MVC adapter and continues
  returning 400 validation errors.
- Unexpected exceptions are not caught and relabelled as expected governance
  failures.

## 10. Verification strategy

### 10.1 Catalogue and mapping tests

- Assert each descriptor's exact code, category, and public detail.
- Assert every non-`ALLOWED` `GovernanceRuleCode` maps to exactly one approved
  descriptor.
- Assert `ALLOWED` cannot construct a governance failure.
- Assert internal messages, violation context, identifiers, roles, states, and
  rule codes are absent from generated Problem Details.

### 10.2 Governance behavior tests

- Cover eligibility, authority, neutrality, system-boundary, recovery-mode,
  and lifecycle mappings.
- Preserve allowed decision behavior.
- Verify denied assertions stop downstream mutation or event publication.
- Verify evaluation methods retain their existing detailed internal decisions.

### 10.3 Recovery and API tests

- Add direct controller tests for disabled mode, missing configuration, missing
  header, blank header, incorrect token, and accepted token.
- Prove all rejected access variants return the same type, title, status, code,
  and detail; request-specific metadata may differ.
- Prove no token or configuration value appears in response fields.
- Add a real Spring MVC/PostgreSQL integration path for representative recovery
  denial and unavailable-authority failures.
- Assert exact content type, status, stable code, safe detail, and request
  metadata rather than checking status alone.

### 10.4 Architecture and regression tests

- Assert governance production code contains no `ApiException` or legacy
  `ErrorCode` reference.
- Assert governance application errors contain no HTTP, servlet, Spring Web, or
  `common.api` dependency.
- Run focused governance and affected marketplace tests.
- Run the complete unit suite.
- Run the complete PostgreSQL integration suite against the unchanged Flyway
  V1 schema.

## 11. Delivery sequence

One KAN-27 feature branch will target `develop` and remain reviewable in four
implementation slices:

1. add the grouped governance catalogue, neutral exception mapping, and
   focused contract tests;
2. migrate governance assertions and affected marketplace consumer tests;
3. migrate and verify admin-recovery access and authority failures; and
4. add API, disclosure, and architecture regressions and run complete
   verification.

The common neutral contract, REST adapter, database, runtime configuration,
legacy users outside governance, and `main` branch remain unchanged.

## 12. Acceptance criteria

- [ ] Every denied governance rule maps to the approved grouped catalogue.
- [ ] `ALLOWED` cannot be converted into an exception.
- [ ] Recovery mode and token failure variants have one indistinguishable 403
      public response.
- [ ] Recovery token and configuration values never enter public or diagnostic
      data.
- [ ] Missing active authority produces the approved 409 state-conflict error.
- [ ] Eligibility, permission, and lifecycle denials use their approved status
      semantics.
- [ ] Detailed `GovernanceDecision` results remain available to trusted
      in-process evaluators.
- [ ] Denied assertions prevent downstream mutations and event publication.
- [ ] Successful governance, marketplace, and admin-recovery behavior remains
      unchanged.
- [ ] The governance module contains no `ApiException` or legacy `ErrorCode`
      dependency.
- [ ] Governance application errors remain transport-neutral.
- [ ] Public responses contain no identifiers, roles, states, module names,
      action names, internal rule codes, raw exception messages, or diagnostic
      codes.
- [ ] Full unit and PostgreSQL integration verification passes with unchanged
      Flyway V1.
- [ ] No out-of-scope database, security-policy, lifecycle, notification,
      payment, logging, audit, runtime configuration, or deployment change is
      included.
