# Documentation

This portal contains current engineering guidance. Start with the task you
need to complete; delivery history and review discussion belong in Jira and
pull requests rather than the permanent documentation path.

## Find What You Need

| I need to... | Read... |
|---|---|
| Set up and run the project | [Getting started](getting-started/README.md) |
| Understand components and data flow | [Architecture](architecture/README.md) |
| Integrate with the HTTP API | [API guide](api/README.md) |
| Interpret or add an API error | [Error contract](api/errors.md) and [error catalogue](api/error-catalogue.md) |
| Understand or change the schema | [Database guide](database/README.md) |
| Review authentication and authorization | [Security guide](security/README.md) |
| Configure or operate the service | [Operations guide](operations/README.md) |
| Understand an important design choice | [Architecture decisions](decisions/README.md) |

## Documentation Rules

- Describe the current system, not the sequence of tickets that produced it.
- Keep task tracking in Jira and code-review discussion in pull requests.
- Record durable architectural choices as decision records.
- Publish diagrams as SVG with a high-resolution PNG fallback.
- Never include credentials, secret values, personal paths, or internal
  approval instructions.

See the [diagram publication guide](architecture/diagram-publication.md) when
adding or changing a diagram.
