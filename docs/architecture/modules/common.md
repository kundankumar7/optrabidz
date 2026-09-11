# Common Module

[Back to the module catalogue](README.md)

Capability: [Platform support](../capabilities/platform-support.md)

## Purpose

Provide shared Problem Details mapping, pagination contracts, request
correlation, observability helpers, domain-event publication, and the
transactional outbox runtime. Capability modules own their concrete success
DTOs.

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

## HTTP contracts

The common module owns shared pagination and RFC 9457 failure structures. It
does not own a universal success envelope. Controllers return capability DTOs
directly unless they need explicit HTTP status or headers. Request correlation
remains an observability concern exposed through `X-Request-Id`, not a success
payload field.

## Verification

Architecture tests keep API controllers independent of the retired response
package and lock the supported controller return-style inventory. Error,
observability, pagination, and outbox behavior remain covered independently.

## Known gaps

The module still imports `identity` and `security`, so it is not yet a
dependency-free platform kernel. Retiring the universal success envelope does
not remove that separate reverse-coupling debt.
