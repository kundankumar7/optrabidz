# Marketplace Module

[Back to the module catalogue](README.md)

Capability: [Marketplace](../capabilities/marketplace.md)

## Purpose

Own funding listings, bids, accepted agreements, debt terms, discovery, and
investor recommendation while protecting actor ownership and lifecycle rules.

## Entry points

`ListingController`, `BidController`, and `AgreementController` expose listing,
recommendation, bid-action, accepted-bid, and agreement operations under
`/api/v1`.

## Application and domain

Listing, bid, agreement, and discovery services coordinate factories, funding
model policies, recommendation handlers, visibility rules, and state-transition
specifications. Domain models own listing and bid states and debt terms.

## Persistence

JPA entities and adapters persist listings, bids, agreements, and their debt
terms through three domain repositories.

## Events

Listing publication/closure, bid submission/withdrawal/rejection/acceptance,
and agreement creation events feed audit, notification, governance, and finance
work.

## Dependencies

Direct imports reach `classification`, `common`, `governance`, `identity`,
`participation`, and `security`. Finance integration is behind
`FinanceAgreementPort`.

## Security and errors

Route rules establish the caller class; ownership and visibility specifications
remain authoritative inside services. The current standard listing collection
and detail reads are anonymous by route configuration; business-policy review
is deliberately deferred.

## Verification

Eleven module tests cover controllers, services, factories, policies,
recommendation, state transitions, and persistence behavior.

## Known gaps

Marketplace search is database-backed and local. Anonymous listing access and
broader business rules require a later product/security review, not an incidental
documentation change.
