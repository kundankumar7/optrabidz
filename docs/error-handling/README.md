# Error Handling

[Back to the documentation portal](../README.md)

## Current System

Expected application failures use a transport-neutral core:

1. each module owns allowlisted error descriptors and typed exceptions;
2. `ApplicationException` carries the public descriptor separately from
   protected diagnostic context;
3. the MVC and Spring Security adapters translate failures at the HTTP
   boundary; and
4. clients receive RFC 9457 `application/problem+json` responses without
   internal identifiers, secrets, stack traces, or raw exception messages.

Identity, security, participation, classification, governance, and marketplace
use the neutral contract. The current module catalogue and login-disclosure
policy are documented in the [KAN-24 module migration design](work-items/KAN-24-module-migration/design.md).

## Work-item History

| Jira | Record |
|---|---|
| [KAN-17](https://0707manna0895.atlassian.net/browse/KAN-17) | [Exception-handling foundation design](work-items/KAN-17-foundation/design.md) |
| [KAN-20](https://0707manna0895.atlassian.net/browse/KAN-20) | [Neutral error contract implementation](work-items/KAN-20-neutral-contract/implementation-plan.md) |
| [KAN-21](https://0707manna0895.atlassian.net/browse/KAN-21) | [RFC 9457 REST adapter implementation](work-items/KAN-21-rest-adapter/implementation-plan.md) |
| [KAN-22](https://0707manna0895.atlassian.net/browse/KAN-22) | [MVC Problem Details implementation](work-items/KAN-22-mvc-adapter/implementation-plan.md) |
| [KAN-23](https://0707manna0895.atlassian.net/browse/KAN-23) | [Spring Security Problem Details implementation](work-items/KAN-23-security-adapter/implementation-plan.md) |
| [KAN-24](https://0707manna0895.atlassian.net/browse/KAN-24) | [Module error migration design](work-items/KAN-24-module-migration/design.md) and [implementation plan](work-items/KAN-24-module-migration/implementation-plan.md) |
| [KAN-26](https://0707manna0895.atlassian.net/browse/KAN-26) | [Classification error migration design](work-items/KAN-26-classification-error-migration/design.md) and [implementation plan](work-items/KAN-26-classification-error-migration/implementation-plan.md) |
| [KAN-27](https://0707manna0895.atlassian.net/browse/KAN-27) | [Governance error migration design](work-items/KAN-27-governance-error-migration/design.md) and [implementation plan](work-items/KAN-27-governance-error-migration/implementation-plan.md) |
| [KAN-28](https://0707manna0895.atlassian.net/browse/KAN-28) | [Marketplace error migration design](work-items/KAN-28-marketplace-error-migration/design.md) and [implementation plan](work-items/KAN-28-marketplace-error-migration/implementation-plan.md) |
