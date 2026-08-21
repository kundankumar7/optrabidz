# OptraBidz Documentation

This portal separates current system guidance from the historical records of
the work items that introduced it.

## Start Here

- [System architecture](architecture/README.md)
- [Database design](database/README.md)

## Find Documentation by Task

| Task | Start here |
|---|---|
| Understand system boundaries | [Architecture](architecture/README.md) |
| Change the database safely | [Database migrations](database/migrations.md) |
| Review why a change was made | [Work-item index](#work-item-index) |

## Work-item Index

| Jira | Subject | Repository record |
|---|---|---|
| [KAN-12](https://0707manna0895.atlassian.net/browse/KAN-12) | Database migration policy | [Design and implementation plan](database/work-items/KAN-12-migration-policy/) |
| [KAN-14](https://0707manna0895.atlassian.net/browse/KAN-14) | Database foundation release | [Design and implementation plan](database/work-items/KAN-14-database-foundation-release/) |
| [KAN-25](https://0707manna0895.atlassian.net/browse/KAN-25) | Documentation information architecture | [Design and implementation plan](architecture/work-items/KAN-25-documentation-information-architecture/) |

Additional work-item records are added here as their existing files move into
their canonical subject directories.

## Documentation Conventions

- Stable references describe the current system and live directly under their
  engineering subject.
- Historical designs and delivery evidence live under
  `<subject>/work-items/<Jira-key>-<slug>/`.
- Editable diagram sources and rendered images stay beside the document that
  owns them.
- Repository-relative links and images are validated by the unit test suite.
