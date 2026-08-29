# Classification Module

[Back to the module catalogue](README.md)

## Purpose

Own startup classification entries and investor preference entries together
with profile, allowed-type, cardinality, integrity, and uniqueness rules.

## Entry points

`StartupClassificationController` and `InvestorPreferenceController` expose
create, replace, remove, and current-actor query operations under `/api/v1`.

## Application and domain

The two services coordinate commands through rule engines. Type policies and
cardinality, integrity, and uniqueness specifications protect the two profile
models before persistence.

## Persistence

Domain repositories are implemented by JPA repositories, repository adapters,
two entities, and `ClassificationPersistenceMapper`.

## Events

Accepted changes publish `StartupClassificationChangedEvent` or
`InvestorPreferenceChangedEvent` for downstream audit and notification work.

## Dependencies

The module imports `common` contracts and the authenticated actor abstraction
from `security`. Participant lookup is inverted behind
`ParticipationActorQueryPort`.

## Security and errors

Controllers use the authenticated actor; services reject missing profiles,
duplicates, missing entries, and rule violations through `ClassificationErrors`.

## Verification

Seven module tests exercise controller, service, rule, and persistence behavior.

## Known gaps

Allowed classification vocabularies are application policies rather than an
administrator-managed reference-data capability.
