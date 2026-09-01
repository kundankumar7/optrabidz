# 0003: Use a Transactional Outbox

**Status:** Accepted

## Context

Business state, audit records, and notifications must not disagree when a
transaction commits but an in-process or external delivery step fails.
Publishing directly after commit also loses work if the process stops between
the database update and publication.

## Decision

Write domain events to an outbox in the same database transaction as business
state. A scheduled dispatcher locks committed records, invokes registered audit
and notification processors, and records retry state. Notification delivery is
claimed and retried separately.

## Consequences

- A successful transaction leaves durable delivery work.
- Dispatch is eventually consistent and consumers must be idempotent.
- Operators need backlog, retry, and failed-delivery visibility.
- The database currently acts as the durable queue; a future broker would be
  an adapter and migration decision, not a replacement for atomic event
  capture.

## Alternatives Considered

- Direct synchronous delivery was rejected because external failure would
  couple business transactions to notification availability.
- In-memory events alone were rejected because they are lost on process
  failure.
- Kafka was deferred because the current workload does not yet justify its
  infrastructure and operating cost.
