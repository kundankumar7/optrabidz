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

Identity, security, participation, classification, governance, marketplace,
notification, and the migrated financial payment and settlement paths use the
neutral contract. The current module catalogue and login-disclosure
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
| [KAN-29](https://0707manna0895.atlassian.net/browse/KAN-29) | [Notification error migration design](work-items/KAN-29-notification-error-migration/design.md) and [implementation plan](work-items/KAN-29-notification-error-migration/implementation-plan.md) |
| [KAN-30](https://0707manna0895.atlassian.net/browse/KAN-30) | [Financial error migration design](work-items/KAN-30-financial-error-migration/design.md) and [implementation plan](work-items/KAN-30-financial-error-migration/implementation-plan.md) |
| [KAN-31](https://0707manna0895.atlassian.net/browse/KAN-31) | [Financial security-boundary design](work-items/KAN-31-financial-security-boundary/design.md) and [implementation plan](work-items/KAN-31-financial-security-boundary/implementation-plan.md) |
| [KAN-32](https://0707manna0895.atlassian.net/browse/KAN-32) | [Payment webhook replay-protection design](work-items/KAN-32-webhook-replay-protection/design.md) and [implementation plan](work-items/KAN-32-webhook-replay-protection/implementation-plan.md) |
| [KAN-35](https://0707manna0895.atlassian.net/browse/KAN-35) | [Payment intent and attempt error migration design](work-items/KAN-35-payment-error-migration/design.md) |
| [KAN-36](https://0707manna0895.atlassian.net/browse/KAN-36) | [Secure webhook-ingress design](work-items/KAN-36-secure-webhook-ingress/design.md) and [implementation plan](work-items/KAN-36-secure-webhook-ingress/implementation-plan.md) |
| [KAN-37](https://0707manna0895.atlassian.net/browse/KAN-37) | [Settlement error migration design](work-items/KAN-37-settlement-error-migration/design.md) and [implementation plan](work-items/KAN-37-settlement-error-migration/implementation-plan.md) |
| [KAN-34](https://0707manna0895.atlassian.net/browse/KAN-34) | [Repayment and installment error migration design](work-items/KAN-34-repayment-error-migration/design.md) |
| [KAN-33](https://0707manna0895.atlassian.net/browse/KAN-33) | [Legacy exception-stack removal design](work-items/KAN-33-legacy-exception-removal/design.md) |
