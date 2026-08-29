# Security Module

[Back to the module catalogue](README.md)

## Purpose

Own credential provisioning, password login, server-side sessions, login
attempts, current-principal lookup, CSRF, route policy, and safe security
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
The security module has no outbox processor of its own.

## Dependencies

Direct imports reach `audit`, `common`, `identity`, and `participation`.

## Security and errors

Password encoding, login throttling, active-session validation, route roles,
CSRF, authentication entry points, and access-denied handling remain inside
security adapters. Controllers consume authenticated identity but do not
authenticate it.

## Verification

Eight module tests cover authentication, authorization responses, filters,
session state, credentials, and HTTP security behavior.

## Known gaps

Authentication is session-based. JWT resource-server support, OAuth2 login,
token key management, revocation, refresh, provider linking, and migration are
not implemented and require a dedicated security design.
