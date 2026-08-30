# Capability Views

[Back to architecture](../README.md)

Capabilities group related modules for navigation. They do not change the
current modular-monolith deployment or transfer ownership between modules.

| Capability | Modules | Read when changing |
|---|---|---|
| [Identity and access](identity-access.md) | `security`, `identity`, `participation` | Authentication, accounts, roles, sessions, or participant profiles |
| [Marketplace](marketplace.md) | `classification`, `marketplace`, `governance` | Eligibility, discovery, bidding, agreements, or lifecycle authority |
| [Finance and payments](finance-payments.md) | `financial` | Settlement, repayment, payment execution, providers, or webhooks |
| [Platform support](platform-support.md) | `common`, `audit`, `notification`, `documentation` | Shared contracts, outbox work, traceability, delivery, or API documentation |

Use the [module catalogue](../modules/README.md) for precise ownership and the
[dependency view](../module-dependencies.md) for current source-level coupling.
