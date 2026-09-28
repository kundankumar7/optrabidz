# Security Module

[Back to the module catalogue](README.md)

Capability: [Identity and access](../capabilities/identity-access.md)

## Purpose

Own credential provisioning, password login and rotation, server-side sessions,
login attempts, current-principal lookup, CSRF, route policy, and safe security
failure adapters.

## Entry points

`AuthController` exposes registration, login, logout, and password change.
`MeController` exposes the current authenticated view. `SecurityConfig`,
`ActiveSessionFilter`, and `CsrfCookieFilter` form the inbound security adapter.

## Application and domain

`AuthenticationService` coordinates registration, credentials, attempts,
sessions, and audit. `CredentialProvisioningService` supports controlled
credential creation; `MeService` composes the current user response. Domain
models own credential and session state.

## Persistence

Credential, login-attempt, and session entities are mapped to three domain
repositories through JPA adapters.

## Events

Registration publishes the shared account event through identity/common flows.
Successful password rotation publishes `CredentialPasswordChangedEvent` in the
same transaction as the credential and session changes. The common outbox
dispatcher later passes that event to the audit module's security policy; the
security module does not own an outbox processor.

## Dependencies

Direct imports reach `audit`, `common`, `identity`, and `participation`.

## Security and errors

Password encoding, login throttling, active-session validation, route roles,
CSRF, authentication entry points, and access-denied handling remain inside
security adapters. Controllers consume authenticated identity but do not
authenticate it.

## Verification

Module and PostgreSQL integration tests cover authentication, authorization
responses, filters, password rotation, session invalidation, safe outbox data,
audit idempotency, credentials, and HTTP security behavior.

## Known gaps

Authentication is session-based. JWT resource-server support, OAuth2 login,
token key management, revocation, refresh, provider linking, and migration are
not implemented and require a dedicated security design.
