# Governance Module

[Back to the module catalogue](README.md)

Capability: [Marketplace](../capabilities/marketplace.md)

## Purpose

Centralize administrative authority, eligibility, neutrality, system-boundary,
visibility, cross-lifecycle, and scheduled lifecycle decisions.

## Entry points

`AdminRecoveryController` exposes token-governed authority transfer. Other
modules call governance controllers and ports inside the process.
`LifecycleExpiryScheduler` is the scheduled entry point.

## Application and domain

Administrator bootstrap and transfer services protect singular active
authority. Eligibility, boundary, neutrality, visibility, and lifecycle
services return governance decisions or violations instead of embedding these
rules in HTTP controllers.

## Persistence

The module owns no JPA entity. It reads and changes participant or capability
state through ports implemented by owning modules.

## Events

Authority transfer and lifecycle enforcement publish
`AdminAuthorityTransferredEvent` and `LifecycleRuleEnforcedEvent`.

## Dependencies

Direct imports reach `classification`, `common`, `identity`, `participation`,
and `security`. Finance and marketplace integrate through governance ports.

## Security and errors

Recovery mode, configured token, administrative authority, and fail-closed
guards protect transfer. `GovernanceErrors` maps public rule failures.

## Verification

Six module tests cover bootstrap, transfer, eligibility, lifecycle, boundary,
and visibility behavior.

## Known gaps

Development bootstrap defaults are unsuitable for shared environments. Complex
policy administration and multi-administrator governance are not implemented.
