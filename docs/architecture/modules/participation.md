# Participation Module

[Back to the module catalogue](README.md)

## Purpose

Own administrator, startup, and investor records plus participant-specific
profile data and completeness evaluation.

## Entry points

`StartupController` and `InvestorController` expose create, current-profile,
and patch operations. Administrator provisioning and authority queries are
internal ports used by governance and security flows.

## Application and domain

Startup, investor, and administrator services coordinate actor authorization
and repository changes. Profile-completeness strategies evaluate participant
data and synchronize the resulting profile status after commit.

## Persistence

JPA entities and adapters persist administrator state, startup legal and web
presence data, and investor web presences through three domain repositories.

## Events

Profile changes publish `ParticipationProfileChangedEvent`.
`ParticipationProfileStatusSyncHandler` reacts after commit to update identity
profile status.

## Dependencies

Direct imports reach `classification`, `common`, `identity`, and `security`.
`ParticipationActorQueryAdapter` implements classification's actor-query port.

## Security and errors

Services enforce participant role and self-ownership. Administrator errors
distinguish active-authority conflicts internally while public mapping remains
stable and safe.

## Verification

Five module tests cover startup, investor, administrator, completeness, and
profile synchronization behavior.

## Known gaps

The administrator model supports active and revoked states, but full
reactivation and multi-administrator product workflows require a dedicated
business-design story.
