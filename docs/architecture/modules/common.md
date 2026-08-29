# Common Module

[Back to the module catalogue](README.md)

## Purpose

Provide shared Problem Details mapping, response and pagination contracts,
request metadata, observability helpers, domain-event publication, and the
transactional outbox runtime.

## Entry points

`RestExceptionHandler` is the shared MVC failure boundary. Filters add request
metadata and diagnostic context. `OutboxDispatcher` is a scheduled runtime
entry point rather than an HTTP endpoint.

## Application and domain

`ApplicationException`, error descriptors and categories form the
transport-neutral error contract. `EventPublisher` and `DomainEvent` form the
shared event boundary.

## Persistence

`OutboxEvent` and `OutboxEventRepository` store durable post-commit work.
Business persistence remains owned by capability modules.

## Events

`SpringEventPublisher`, `OutboxWriter`, metadata resolution, the dispatcher,
and `OutboxEventProcessor` connect in-process publication to durable processing.

## Dependencies

Current source imports `identity` and `security`. This reverse coupling is
recorded technical debt; `common` is not yet a dependency-free kernel.

## Security and errors

`ProblemDetailsFactory`, validation mapping, and the security response writer
produce safe public failures. Sensitive-data masking and MDC helpers support
server-side diagnostics.

## Verification

Eighteen module tests cover error, response, observability, and outbox behavior.

## Known gaps

`ApiResponse` still mixes success formatting, legacy error support, metadata,
and request-ID concerns. KAN-41 tracks separating those responsibilities before
retiring the wrapper.
