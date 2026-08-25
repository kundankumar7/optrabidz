# OptraBidz Documentation

This portal separates current system guidance from the historical records of
the work items that introduced it.

## Start Here

- [System architecture](architecture/README.md)
- [Database design](database/README.md)
- [Error handling](error-handling/README.md)

## Find Documentation by Task

| Task | Start here |
|---|---|
| Understand system boundaries | [Architecture](architecture/README.md) |
| Change the database safely | [Database migrations](database/migrations.md) |
| Add or change an API error | [Error handling](error-handling/README.md) |
| Review why a change was made | [Work-item index](#work-item-index) |

## Work-item Index

| Jira | Subject | Repository record |
|---|---|---|
| [KAN-12](https://0707manna0895.atlassian.net/browse/KAN-12) | Database migration policy | [Design and implementation plan](database/work-items/KAN-12-migration-policy/) |
| [KAN-14](https://0707manna0895.atlassian.net/browse/KAN-14) | Database foundation release | [Design and implementation plan](database/work-items/KAN-14-database-foundation-release/) |
| [KAN-17](https://0707manna0895.atlassian.net/browse/KAN-17) | Exception-handling foundation | [Design](error-handling/work-items/KAN-17-foundation/design.md) |
| [KAN-20](https://0707manna0895.atlassian.net/browse/KAN-20) | Neutral error contract | [Implementation plan](error-handling/work-items/KAN-20-neutral-contract/implementation-plan.md) |
| [KAN-21](https://0707manna0895.atlassian.net/browse/KAN-21) | RFC 9457 REST adapter | [Implementation plan](error-handling/work-items/KAN-21-rest-adapter/implementation-plan.md) |
| [KAN-22](https://0707manna0895.atlassian.net/browse/KAN-22) | MVC Problem Details adapter | [Implementation plan](error-handling/work-items/KAN-22-mvc-adapter/implementation-plan.md) |
| [KAN-23](https://0707manna0895.atlassian.net/browse/KAN-23) | Spring Security Problem Details adapter | [Implementation plan](error-handling/work-items/KAN-23-security-adapter/implementation-plan.md) |
| [KAN-24](https://0707manna0895.atlassian.net/browse/KAN-24) | Module error migration | [Design and implementation plan](error-handling/work-items/KAN-24-module-migration/) |
| [KAN-25](https://0707manna0895.atlassian.net/browse/KAN-25) | Documentation information architecture | [Design and implementation plan](architecture/work-items/KAN-25-documentation-information-architecture/) |
| [KAN-31](https://0707manna0895.atlassian.net/browse/KAN-31) | Financial security boundary | [Design and implementation plan](error-handling/work-items/KAN-31-financial-security-boundary/) |
| [KAN-43](https://0707manna0895.atlassian.net/browse/KAN-43) | OpenAPI Problem Details contract and public error catalogue | [Design and implementation plan](error-handling/work-items/KAN-43-openapi-error-catalogue/) |

Additional work-item records are added here as their existing files move into
their canonical subject directories.

## Documentation Conventions

- Stable references describe the current system and live directly under their
  engineering subject.
- Historical designs and delivery evidence live under
  `<subject>/work-items/<Jira-key>-<slug>/`.
- Editable diagram sources and rendered images stay beside the document that
  owns them.
- Repository-relative links, images, and command file references are validated
  by the unit test suite.

Before publication, review every changed document for:

- project-focused language without transient execution or local-workspace
  instructions;
- lifecycle status and future-tense statements that match the delivered state;
- code examples, commands, paths, and test evidence that match the reviewed
  implementation; and
- temporary files, machine-specific paths, credentials, or other information
  that does not belong in the repository.
