# API Guide

[Back to the documentation portal](../README.md)

## HTTP Boundary

Public application endpoints use the `/api/v1` prefix. Controllers translate
HTTP input and output; business rules remain in application services and
authentication remains in the security boundary.

Swagger UI is available only when explicitly enabled. The development profile
publishes its configured entry at `/swagger-ui.html`; production configuration
disables it by default.

## Response Contracts

Successful reads and ordinary updates return their concrete response DTO or
`PageResponse<T>` directly. Controllers use `ResponseEntity<T>` only when the
HTTP response needs an explicit status or header.

- Operations documented as `201 Created` return the created representation. A
  `Location` header identifies its retrieval URI when one exists; registration
  and payment-attempt creation do not invent one.
- Successful commands that have no representation return `204 No Content` and
  an empty body.
- The request correlation identifier is carried by the `X-Request-Id` response
  header, not embedded in successful payloads.

Errors use RFC 9457 Problem Details. Success DTOs and error documents are
separate contracts; clients must not expect `success`, `data`, or `meta`
wrapper fields.

Read the [error contract](errors.md) for the boundary rules and the generated
[public error catalogue](error-catalogue.md) for individual codes.

## Compatibility

The `/api/v1` prefix is the compatibility boundary. Additive response fields
are preferred within a version. Breaking request, response, authentication, or
semantic changes require an explicit compatibility and migration decision.

The OpenAPI document describes the public HTTP contract. Internal diagnostic
codes, stack traces, entity structures, and provider secrets are not part of
that contract.
