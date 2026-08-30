# Module Dependencies

[Back to architecture](README.md)

This is the current production-import graph derived from Java source. It is
descriptive, not an assertion that every dependency is desirable. A dependency
means at least one class imports a type from the named top-level module.

| Module | Direct dependencies |
|---|---|
| `audit` | `common` |
| `classification` | `common`, `security` |
| `common` | `identity`, `security` |
| `documentation` | `classification`, `common`, `financial`, `governance`, `identity`, `marketplace`, `notification`, `participation`, `security` |
| `financial` | `audit`, `common`, `governance`, `identity`, `marketplace`, `participation`, `security` |
| `governance` | `classification`, `common`, `identity`, `participation`, `security` |
| `identity` | `common` |
| `marketplace` | `classification`, `common`, `governance`, `identity`, `participation`, `security` |
| `notification` | `common`, `security` |
| `participation` | `classification`, `common`, `identity`, `security` |
| `security` | `audit`, `common`, `identity`, `participation` |

## How to read the graph

- `documentation` intentionally aggregates public error catalogues owned by
  multiple modules. That read-only composition explains its wide import set.
- `financial` has the broadest business dependency surface because it combines
  agreement lookup, actor ownership, governance, payment, security, and audit
  boundaries.
- `common` currently imports identity and security types. That reverse coupling
  is existing technical debt and must not be hidden by an idealized diagram.
- Ports reduce some infrastructure coupling, but repository-wide architectural
  enforcement is not yet complete.

`ArchitectureModuleCatalogTest` derives these edges directly from production
imports. The small module catalogue records only intentional capability and
owner-page assignments; it does not duplicate generated dependency facts.
