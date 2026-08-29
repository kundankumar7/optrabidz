# Module Catalogue

[Back to architecture](../README.md)

The catalogue mirrors the eleven top-level production packages. Counts are
derived from source and tests by `ArchitectureModuleInventoryTest`; they are a
navigation aid, not a quality score.

| Module | Primary responsibility | Source / tests |
|---|---|---:|
| [`audit`](audit.md) | Durable business and security audit records | 20 / 4 |
| [`classification`](classification.md) | Startup classifications and investor preferences | 61 / 7 |
| [`common`](common.md) | Shared HTTP, error, observability, event, and outbox infrastructure | 36 / 18 |
| [`documentation`](documentation.md) | Public error catalogue and OpenAPI/security adapters | 7 / 21 |
| [`financial`](financial.md) | Settlements, repayments, payment attempts, and webhooks | 119 / 30 |
| [`governance`](governance.md) | Authority, eligibility, boundary, and lifecycle rules | 41 / 6 |
| [`identity`](identity.md) | Accounts, roles, and profile state | 26 / 2 |
| [`marketplace`](marketplace.md) | Listings, bids, agreements, and recommendation | 110 / 11 |
| [`notification`](notification.md) | Notification subscriptions and channel delivery | 49 / 5 |
| [`participation`](participation.md) | Administrator, startup, and investor records | 53 / 5 |
| [`security`](security.md) | Credentials, sessions, authentication, CSRF, and route policy | 49 / 8 |

Use [module dependencies](../module-dependencies.md) for the current import
graph. The machine-readable inventory is in `inventory.json`.
