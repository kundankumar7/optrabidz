# Identity Module

[Back to the module catalogue](README.md)

Capability: [Identity and access](../capabilities/identity-access.md)

## Purpose

Own accounts, roles, activation state, and profile-completeness state without
owning HTTP authentication mechanics.

## Entry points

There is no identity controller. `IdentityCommandPort` and `IdentityQueryPort`
are called by security, participation, governance, and related application
services.

## Application and domain

`AccountApplicationService` creates, activates, deactivates, and updates
profile status. `AccountQueryService` returns account snapshots. Domain models
protect account, role, and profile state transitions.

## Persistence

`AccountEntity`, `RoleEntity`, and `ProfileEntity` are mapped through
`AccountPersistenceMapper`; `AccountRepositoryAdapter` implements the domain
repository over `JpaAccountRepository`.

## Events

Account registration is published through the shared
`AccountRegisteredEvent`; the identity package defines no module-local event.

## Dependencies

Production source directly imports `common` error and event contracts.

## Security and errors

Identity owns account state, while the security module owns credentials,
sessions, password policy, and route authentication. Identity errors cover
missing accounts and account/profile state conflicts.

## Verification

Two focused module tests cover account application and query behavior; broader
registration and authentication tests exercise it through security.

## Known gaps

Identity has no external identity-provider federation, email verification,
self-service recovery, or independent public API.
