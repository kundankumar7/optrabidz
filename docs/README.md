# Documentation

This portal contains current engineering guidance. Start with the task you
need to complete; delivery history and review discussion belong in Jira and
pull requests rather than the permanent documentation path.

## Understand the system

| Question | Read |
|---|---|
| What are the system boundaries and major flows? | [Architecture](architecture/README.md) |
| How are the business capabilities divided? | [Capability map](architecture/capabilities/README.md) |
| How is application data related? | [Database guide](database/README.md) |
| Why were important architectural choices made? | [Architecture decisions](decisions/README.md) |

## Change or verify the system

| Task | Read |
|---|---|
| Set up, run, or test the project | [Getting started](getting-started/README.md) |
| Change a module or inspect its ownership | [Module catalogue](architecture/modules/README.md) |
| Integrate with the HTTP API | [API guide](api/README.md) |
| Interpret or add an API error | [Error contract](api/errors.md) and [error catalogue](api/error-catalogue.md) |
| Change a migration or verify persistence | [Database guide](database/README.md) |
| Review authentication and authorization | [Security guide](security/README.md) |
| Configure or operate the service | [Operations guide](operations/README.md) |
| Update engineering documentation | [Documentation maintenance map](maintenance.md) |

## Documentation Rules

- Describe the current system, not the sequence of tickets that produced it.
- Keep task tracking in Jira and code-review discussion in pull requests.
- Record durable architectural choices as decision records.
- Publish diagrams as SVG with a high-resolution PNG fallback.
- Never include credentials, secret values, personal paths, or internal
  approval instructions.

See the [diagram publication guide](architecture/diagram-publication.md) when
adding or changing a diagram.
