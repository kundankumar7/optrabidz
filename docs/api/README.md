# API Guide

[Back to the documentation portal](../README.md)

## HTTP Boundary

Public application endpoints use the `/api/v1` prefix. Controllers translate
HTTP input and output; business rules remain in application services and
authentication remains in the security boundary.

Swagger UI is available only when explicitly enabled. The development profile
publishes it at `/swagger-ui/index.html`; production configuration disables it
by default.

## Response Contracts

Successful controller responses currently use a typed envelope containing:

- `success`
- `data`
- `meta.requestId`
- `meta.timestamp`

Errors use RFC 9457 Problem Details. Success formatting and error formatting
are separate contracts; clients must not expect error payloads inside the
success envelope.

Read the [error contract](errors.md) for the boundary rules and the generated
[public error catalogue](error-catalogue.md) for individual codes.

## Compatibility

The `/api/v1` prefix is the compatibility boundary. Additive response fields
are preferred within a version. Breaking request, response, authentication, or
semantic changes require an explicit compatibility and migration decision.

The OpenAPI document describes the public HTTP contract. Internal diagnostic
codes, stack traces, entity structures, and provider secrets are not part of
that contract.
