# KAN-39 Documentation Audit

This delivery record classifies every historical Markdown file before the
stable documentation hierarchy is built. Jira, pull requests, commits, and Git
retain delivery history; the repository will retain only current guidance.

## Content disposition

| File | Disposition | Stable target | Reusable facts | Asset disposition |
|---|---|---|---|---|
| `architecture/work-items/KAN-25-documentation-information-architecture/design.md` | `DISTILL_REMOVE` | `docs/README.md` | Task-oriented navigation | Remove after distillation |
| `architecture/work-items/KAN-25-documentation-information-architecture/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `architecture/work-items/KAN-39-diagram-publication-quality/audit.md` | `ACTIVE_RECORD` | none | Verification evidence | Retain through delivery |
| `architecture/work-items/KAN-39-diagram-publication-quality/design.md` | `ACTIVE_RECORD` | none | Approved KAN-39 scope | Retain through delivery |
| `architecture/work-items/KAN-39-diagram-publication-quality/implementation-plan.md` | `ACTIVE_RECORD` | none | Approved execution plan | Retain through delivery |
| `database/work-items/KAN-12-migration-policy/design.md` | `DISTILL_REMOVE` | `docs/database/migrations.md` | Flyway ownership and migration policy | Remove after distillation |
| `database/work-items/KAN-12-migration-policy/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `database/work-items/KAN-14-database-foundation-release/design.md` | `DISTILL_REMOVE` | `docs/database/README.md` | Database release boundary | Remove after distillation |
| `database/work-items/KAN-14-database-foundation-release/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-17-foundation/design.md` | `DISTILL_REMOVE` | `docs/api/errors.md` | Error-system separation | Remove after distillation |
| `error-handling/work-items/KAN-20-neutral-contract/implementation-plan.md` | `DISTILL_REMOVE` | `docs/api/errors.md` | Transport-neutral error contract | Remove |
| `error-handling/work-items/KAN-21-rest-adapter/implementation-plan.md` | `DISTILL_REMOVE` | `docs/api/errors.md` | REST mapping responsibility | Remove |
| `error-handling/work-items/KAN-22-mvc-adapter/implementation-plan.md` | `DISTILL_REMOVE` | `docs/api/errors.md` | MVC boundary responsibility | Remove |
| `error-handling/work-items/KAN-23-security-adapter/implementation-plan.md` | `DISTILL_REMOVE` | `docs/security/README.md` | Security error adapter boundary | Remove |
| `error-handling/work-items/KAN-24-module-migration/design.md` | `DISTILL_REMOVE` | `docs/api/errors.md` | Module-owned error definitions | Remove all assets |
| `error-handling/work-items/KAN-24-module-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-26-classification-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Classification public codes | Remove all assets |
| `error-handling/work-items/KAN-26-classification-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-27-governance-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Governance public codes | Remove all assets |
| `error-handling/work-items/KAN-27-governance-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-28-marketplace-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Marketplace public codes | Remove all assets |
| `error-handling/work-items/KAN-28-marketplace-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-29-notification-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Notification public codes | Remove all assets |
| `error-handling/work-items/KAN-29-notification-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-30-financial-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Financial public codes and disclosure rule | Remove all assets |
| `error-handling/work-items/KAN-30-financial-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-31-financial-security-boundary/design.md` | `DISTILL_REMOVE` | `docs/security/README.md` | Authentication resolves at the security boundary | Remove all assets |
| `error-handling/work-items/KAN-31-financial-security-boundary/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-32-webhook-replay-protection/design.md` | `DISTILL_REMOVE` | `docs/security/README.md` | Webhook verification and replay protection | Remove all assets |
| `error-handling/work-items/KAN-32-webhook-replay-protection/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-33-legacy-exception-removal/design.md` | `DISTILL_REMOVE` | `docs/api/errors.md` | Single public error contract | Remove all assets |
| `error-handling/work-items/KAN-33-legacy-exception-removal/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-34-repayment-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Repayment public codes | Remove all assets |
| `error-handling/work-items/KAN-34-repayment-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-35-payment-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Payment public codes | Remove all assets |
| `error-handling/work-items/KAN-35-payment-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-36-secure-webhook-ingress/design.md` | `DISTILL_REMOVE` | `docs/security/README.md` | Signed webhook ingress | Remove all assets |
| `error-handling/work-items/KAN-36-secure-webhook-ingress/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-37-settlement-error-migration/design.md` | `DISTILL_REMOVE` | `docs/api/error-catalogue.md` | Settlement public codes | Remove all assets |
| `error-handling/work-items/KAN-37-settlement-error-migration/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-42-real-http-smoke/design.md` | `DISTILL_REMOVE` | `docs/api/errors.md` | Real-port error contract verification | Remove all assets |
| `error-handling/work-items/KAN-42-real-http-smoke/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |
| `error-handling/work-items/KAN-43-openapi-error-catalogue/design.md` | `MIGRATE_DIAGRAM` | `docs/api/errors.md` | Error contract publication flow | Move and rename diagram |
| `error-handling/work-items/KAN-43-openapi-error-catalogue/implementation-plan.md` | `DISTILL_REMOVE` | none | none | Remove |

## Disposition totals

- `ACTIVE_RECORD`: 3 Markdown files
- `MIGRATE_DIAGRAM`: 1 Markdown owner and its diagram set
- `DISTILL_REMOVE`: 40 Markdown files
- Unclassified Markdown files: 0

## Current implementation truth baseline

This baseline compares stable guidance with repository sources before delivery
history is removed. `PARTIAL` means the implemented boundary exists but the
named production capability or documentation coverage is incomplete.

| Topic | Classification | Authoritative repository evidence | Documentation result |
|---|---|---|---|
| Build and runtime | `IMPLEMENTED` | `pom.xml`; `.github/workflows/backend-ci.yml` | Java 21, Spring Boot 3.3.2, Maven unit tests, and PostgreSQL integration tests verified |
| Modular structure | `PARTIAL` | `src/main/java/com/project/optrabidz/`; `src/test/java/com/project/optrabidz/architecture/ExceptionArchitectureTest.java` | Capability packages verified; `documentation` adapter added to map; repository-wide dependency enforcement is not claimed |
| Runtime profiles | `IMPLEMENTED` | `src/main/resources/application.properties`; `application-dev.properties`; `application-prod.properties` | Development admin bootstrap and financial adapters distinguished from baseline notification channels; production datasource requirements retained |
| HTTP and OpenAPI boundary | `IMPLEMENTED` | module `api/*Controller.java`; `documentation/openapi/`; `documentation/security/` | `/api/v1` routes verified; configured Swagger entry corrected to `/swagger-ui.html`; documentation remains fail-closed outside an enabled profile |
| Success and error contracts | `IMPLEMENTED` | `common/api/response/`; `common/api/error/`; `common/error/`; module `application/error/` catalogues | Success envelope and RFC 9457 responsibilities verified; generated catalogue parity remains enforced |
| Session security | `IMPLEMENTED` | `security/infrastructure/config/SecurityConfig.java`; session filters; security HTTP tests | Server-side session, CSRF, matched route rules, security Problem Details, and current public listing reads documented |
| JWT and OAuth2 | `PLANNED` | No corresponding dependency or production implementation | Described only as possible future adapters |
| Flyway schema ownership | `IMPLEMENTED` | `db/migration/V1__baseline.sql`; database migration integration tests; JPA configuration | Flyway ownership and Hibernate validation verified |
| ER diagram coverage | `PARTIAL` | 35 V1 tables compared with 34 unique entities in `database/assets/er-diagram-source.md` | Missing `login_attempt` recorded; V1 SQL named as complete source until Task 5 remediation |
| Outbox and audit | `IMPLEMENTED` | `common/event/`; `common/outbox/`; `audit/`; outbox and audit tests | Atomic outbox write, `SKIP LOCKED` dispatch, retry, audit persistence, request correlation, and masking verified |
| Notifications | `PARTIAL` | `notification/`; dispatcher and API tests | In-app persistence, subscriptions, delivery attempts, retry, sandbox email, and sandbox push exist; no external provider or broker is claimed |
| Payments and webhooks | `PARTIAL` | `financial/`; financial and webhook integration tests | Local/sandbox strategies, HMAC-style verification, bounded ingress, and database replay claims exist; no real-money provider is claimed |
| Logging and observability | `PARTIAL` | `common/observability/`; security and audit tests | Request/security MDC and sensitive-data masking exist; centralized aggregation, metrics dashboards, and alerting are not implemented |
| AOP cross-cutting adapters | `PLANNED` | Spring AOP dependency in `pom.xml`; no production `@Aspect` implementation | Stable guidance does not claim AOP-based logging, audit, security, or transaction policy |
| Redis caching | `PLANNED` | No Redis dependency or production implementation | Not described as current architecture |
| Verification | `IMPLEMENTED` | Surefire/Failsafe configuration; Testcontainers support; CI workflow; MockMvc and real-port HTTP tests | Unit, PostgreSQL integration, MVC boundary, real-port smoke, documentation, and architecture checks verified as distinct layers |

### Corrections found by the baseline

- Use `/swagger-ui.html`, which is the configured development entry point.
- Include the `documentation` adapter package in the architecture capability
  map without treating it as a business-domain module.
- State that general module dependency enforcement remains incomplete.
- Distinguish development admin and payment configuration from notification
  channel defaults.
- Add `login_attempt` when the surviving ER set is remediated.
- Keep Kafka, Redis, JWT, OAuth2, external notification providers, AOP policy,
  centralized observability, and real-money processing out of current-state
  claims.

## Execution evidence

| Stage | Evidence | Result |
|---|---|---|
| Quality gates | 44 work-item Markdown files compared with the disposition table | 0 unclassified |
| Quality gates | `DocumentationStructureValidatorTest`, `DiagramPublicationValidatorTest`, and `DiagramPublicationTest` | Pass |
| Architecture prototype | Shared renderer and publication tests | Pass |
| Architecture prototype | 980-pixel desktop preview | Pass: labels, routing, and hierarchy readable |
| Architecture prototype | 390-pixel phone preview | Pass: seven-node overview readable without zoom |
| Architecture prototype | Revised KAN-34-aligned SVG | Approved |
| Architecture prototype | GitHub desktop and mobile web | Pass |
| Architecture prototype | GitHub Mobile SVG | Pass |
| Architecture prototype | GitHub Mobile PNG fallback | Pass: cache-safe final filename displays the approved diagram |
| Architecture prototype | Jira 2400-pixel PNG | Pass: approved fallback is the only KAN-39 attachment |
| Stable hierarchy | Task-oriented portal and seven topic guides | Pass: current guidance is reachable without Jira-key navigation |
| Stable hierarchy | Generated public error catalogue | Pass: moved under `docs/api` and snapshot parity verified |
| Stable hierarchy | Structure and repository link checks | Pass |
| Truth baseline | Build, profiles, modules, HTTP, security, errors, schema, delivery, integrations, and tests compared with repository sources | 16 topics classified; six stable-guide corrections identified |
| Truth baseline | V1 table inventory compared with ER source | 35 schema tables; 34 diagram entities; `login_attempt` queued for Task 5 |
| Truth baseline | Future-infrastructure scan | No production Kafka, Redis, JWT, OAuth2, or `@Aspect` implementation |
| Historical cleanup | 41 completed work-item Markdown records | Removed after their current guidance was distilled; only the three active KAN-39 records remain |
| Historical cleanup | 57 historical diagram assets reviewed | 54 obsolete assets removed; the three-file public error-contract set migrated to `docs/api/assets/` |
| Historical cleanup | Stable Markdown dependency scan | No stable page links to `work-items/`, removed Jira-key paths, Mermaid source files, or inline Mermaid blocks |
| Historical cleanup | Diagram publication inventory | Reduced to 13 stable reader-facing diagrams with stable owners |
| Historical cleanup | Structure, link, publication, and catalogue tests | Pass |
| Historical cleanup | `npm run diagrams:check` and `git diff --check` | Pass |

Temporary review sheets remain under `target/documentation-review/` and are
never committed.
