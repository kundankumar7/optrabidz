# System Architecture

[Back to the documentation portal](../README.md)

## Current References

- [Editable modular-monolith overview](overview.mmd)
- [Diagram publication guide](diagram-publication.md)
- [High-resolution PNG for Jira and offline review](assets/optrabidz-architecture-overview.png)

<a href="assets/optrabidz-architecture-overview.svg">
  <img src="assets/optrabidz-architecture-overview.svg" alt="OptraBidz modular-monolith architecture overview">
</a>

The application is deployed as one Spring Boot process while business areas
remain separated into modules with explicit application, domain, persistence,
event, and integration boundaries.

## Work-item History

| Jira | Decision record | Delivery plan |
|---|---|---|
| [KAN-25](https://0707manna0895.atlassian.net/browse/KAN-25) | [Documentation information architecture](work-items/KAN-25-documentation-information-architecture/design.md) | [Implementation plan](work-items/KAN-25-documentation-information-architecture/implementation-plan.md) |
| [KAN-39](https://0707manna0895.atlassian.net/browse/KAN-39) | [Reviewer-quality diagram publication](work-items/KAN-39-diagram-publication-quality/design.md) | [Implementation plan](work-items/KAN-39-diagram-publication-quality/implementation-plan.md) |
