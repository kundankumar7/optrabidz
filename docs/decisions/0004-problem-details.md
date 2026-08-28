# 0004: Map Neutral Errors to RFC 9457 Problem Details

**Status:** Accepted

## Context

Business rules, Spring MVC, and Spring Security can fail at different layers.
Building response maps in controllers creates inconsistent status codes,
leaks framework concerns into application services, and makes public errors
hard to govern.

## Decision

Application exceptions carry a stable public descriptor separately from
internal diagnostic information. HTTP adapters map descriptors, validation
failures, authentication failures, and access denials to one RFC 9457 Problem
Details contract. A generated catalogue publishes the supported public codes.

## Consequences

- Clients receive stable codes and consistent response fields.
- Internal logs can retain useful diagnostics without exposing them publicly.
- Controllers remain focused on HTTP adaptation.
- Every new public code needs catalogue, service, adapter, and documentation
  verification.

## Alternatives Considered

- Controller-specific error responses were rejected because they duplicate
  policy and drift over time.
- A single generic error was rejected because clients need safe,
  machine-readable distinctions.
- Exposing exception messages was rejected because messages are unstable and
  may disclose implementation details.
