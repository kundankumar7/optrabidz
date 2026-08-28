# 0002: Flyway Owns Schema Evolution

**Status:** Accepted

## Context

Automatic ORM schema mutation is difficult to review, reproduce, and recover
across environments. Production-grade database changes need ordered history,
checksums, explicit SQL, and a rehearsable upgrade path.

## Decision

Flyway is the only component that creates or upgrades the application schema.
Versioned migrations are immutable after use. Hibernate runs with
`ddl-auto=validate` and fails startup when mappings disagree with the migrated
schema.

## Consequences

- Schema changes are code-reviewed and repeatable.
- Fresh and existing databases converge through the same ordered history.
- Destructive or populated-database changes require explicit data migration
  and recovery planning.
- Developers must create a new migration instead of editing an applied one.

## Alternatives Considered

- Hibernate schema generation was rejected because it does not provide the
  required review and release history.
- Manual environment-specific SQL was rejected because it creates drift and
  incomplete migration records.
