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
| ER diagram coverage | `IMPLEMENTED` | 35 Flyway-migrated PostgreSQL tables compared with the relational journey and focused database views; `DatabaseDocumentationContractIT` | Eleven focused relational views use the approved KAN-34 visual language; `login_attempt` is shown as a standalone immutable security log without an invented foreign key |
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
- Add `login_attempt` when the surviving ER set is remediated. Resolved in Task 5 with automated Flyway-to-ER coverage.
- Keep Kafka, Redis, JWT, OAuth2, external notification providers, AOP policy,
  centralized observability, and real-money processing out of current-state
  claims.

### Module ownership catalogue

`docs/architecture/module-catalog.json` records only intentional module,
capability, and owner-page assignments. Generated source, test, surface, and
dependency counts are no longer committed. `ArchitectureModuleCatalogTest`
derives the current top-level modules and import edges directly from production
source and verifies the human dependency guide.

The current dependency guide still discloses reverse `common` coupling, the
documentation adapter's broad catalogue composition, and the financial
module's cross-capability dependencies instead of presenting an idealized
architecture.

## Architecture figure disposition

| Reader question | Current evidence | Status | Owner | Decision |
|---|---|---|---|---|
| Who calls the system and where are its trust boundaries? | Spring MVC/security configuration, webhook adapters, PostgreSQL configuration | `IMPLEMENTED` | `architecture/system-context.md` | `REDESIGN` as a focused system-context view |
| What runs inside one application instance? | `OptrabidzApplication`, Spring configuration, scheduled workers | `IMPLEMENTED` | `architecture/runtime.md` | `KEEP` as a dedicated runtime view |
| Which module owns each capability? | `module-catalog.json`, eleven production packages | `IMPLEMENTED` | `architecture/modules/README.md` | `KEEP` as one complete module map |
| Which source dependencies cross modules? | Production Java imports verified by `ArchitectureModuleCatalogTest` | `PARTIAL` | `architecture/module-dependencies.md` | `KEEP`; show current reverse coupling rather than an ideal graph |
| Where do authentication and business authorization occur? | Security filters/configuration, identity and participant ports, boundary tests | `IMPLEMENTED` | `architecture/capabilities/identity-access.md` | `KEEP`; label JWT and OAuth2 as planned only |
| How does a listing become an agreement? | Classification, marketplace, governance services and lifecycle tests | `IMPLEMENTED` | `architecture/capabilities/marketplace.md` | `KEEP` as a guarded lifecycle |
| How do obligations reach provider callbacks? | Financial services, adapters, webhook and replay tests | `PARTIAL` | `architecture/capabilities/finance-payments.md` | `KEEP`; label real-money processing as absent |
| How does a user request cross the security boundary? | Route policy, session/CSRF filters, controllers, application authorization | `IMPLEMENTED` | `architecture/flows/request-security.md` | `KEEP` as a responsibility sequence |
| How are audit and notification effects delivered? | Transactional outbox, dispatchers, processors, retry tests | `PARTIAL` | `architecture/flows/event-delivery.md` | `KEEP`; no Kafka or external provider claim |
| How are public errors disclosed? | Existing API Problem Details figure and catalogue tests | `IMPLEMENTED` | `api/errors.md` | `REUSE` in the architecture error flow |
| Does platform support need a separate generic diagram? | Same outbox/delivery evidence as the event flow | `IMPLEMENTED` | `architecture/capabilities/platform-support.md` | `REUSE` event-delivery; reject a duplicate drawing |

## Stable diagram review

Every entry was rendered from its declared canonical source. Desktop and phone
previews use 980- and 390-pixel widths. The eleven database views were
redesigned as focused relational maps rather than republishing the previous ER
artwork. Their new descriptive asset paths also prevent GitHub Mobile from
reusing a stale cached image. Dense phone previews preserve the relationship
map; field detail remains lossless through the linked SVG.

A post-approval review found directional connectors that stopped in whitespace
or entered along a target border. The affected architecture SVGs were repaired,
and the publication validator now enforces declared targets, boundary contact,
and perpendicular entry. The regenerated assets passed the reopened review.

| Diagram | Repository truth reviewed | Desktop | Phone | Dark surround | Jira PNG |
|---|---|---|---|---|---|
| System context | Callers, user HTTP boundary, signed webhook boundary, modular monolith, PostgreSQL, and external adapters | Pass | Inline readable | Pass | 2400×3200 generated; arrow repair approved |
| Runtime topology | One JVM, adapter layers, scheduled responsibilities, and PostgreSQL | Pass | Inline readable | Pass | 2400×3267 generated; arrow repair approved |
| Module ownership map | All eleven modules grouped into four reader capabilities | Pass | Inline readable | Pass | 2400×3267 generated; reviewer approved |
| Current module dependencies | All source-derived direct imports, including reverse `common` coupling | Pass | Inline readable | Pass | 2400×2880 generated; reviewer approved |
| Identity and access | Security adapter, authenticated actor, identity/participation facts, and service authorization | Pass | Inline readable | Pass | 2400×3133 generated; arrow repair approved |
| Marketplace lifecycle | Classification and governance guards through listing, discovery, bid, and agreement | Pass | Inline readable | Pass | 2400×3133 generated; arrow repair approved |
| Finance and payments | Agreement obligations, intents, attempts, signed callbacks, and replay protection | Pass | Inline readable | Pass | 2400×3267 generated; arrow repair approved |
| Request and security | Route policy, server-side session/CSRF, controller adaptation, service rules, and rejection | Pass | Inline readable | Pass | 2400×3467 generated; arrow repair approved |
| Event delivery | Atomic outbox write, claim, audit/notification processors, and retryable delivery | Pass | Inline readable | Pass | 2400×3533 generated; reviewer approved |
| Account access and security | V1 account, role, credential, session, admin, and standalone `login_attempt` | Pass | Relationship map + SVG detail | Pass | 2400×1720 |
| Participant profile | V1 account-owned profile, startup, investor, and detail-table FKs | Pass | Relationship map + SVG detail | Pass | 2400×1509 |
| Marketplace listing and bidding | V1 listing, debt terms, bid, investor, and partial accepted-bid rule | Pass | Relationship map + SVG detail | Pass | 2400×1520 |
| Agreement acceptance and terms | V1 agreement FKs, unique bid, debt terms, and consistency triggers | Pass | Relationship map + SVG detail | Pass | 2400×1337 |
| Settlement | V1 agreement and participant FKs plus consistency trigger | Pass | Relationship map + SVG detail | Pass | 2400×1300 |
| Repayment schedule | V1 agreement, participant, repayment, and installment relationships | Pass | Relationship map + SVG detail | Pass | 2400×1560 |
| Payment intent | V1 nullable purpose sources and payer/payee account FKs | Pass | Relationship map + SVG detail | Pass | 2400×1440 |
| Payment attempt and provider | V1 intent, provider, method, and attempt relationships | Pass | Relationship map + SVG detail | Pass | 2400×1300 |
| Payment webhook | V1 provider idempotency and nullable payment references | Pass | Relationship map + SVG detail | Pass | 2400×1300 |
| Notification delivery | V1 recipient, channel, attempt, subscription, and account FKs | Pass | Relationship map + SVG detail | Pass | 2400×1337 |
| Outbox and audit correlation | V1 audit actor FK and non-FK `event_id` correlation | Pass | Relationship map + SVG detail | Pass | 2400×1400 |
| Public error contract | Catalogue merge behavior, OpenAPI publication, Markdown snapshot, and separate HTTP exposure boundary | Pass | Inline readable | Pass | 2400×2133 |

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
| Truth baseline | Flyway-migrated PostgreSQL table inventory compared with published database guidance | Initially 34 diagram entities; Task 5 now covers all 35 and enforces parity in `DatabaseDocumentationContractIT` |
| Truth baseline | Future-infrastructure scan | No production Kafka, Redis, JWT, OAuth2, or `@Aspect` implementation |
| Historical cleanup | 41 completed work-item Markdown records | Removed after their current guidance was distilled; only the three active KAN-39 records remain |
| Historical cleanup | 57 historical diagram assets reviewed | 54 obsolete assets removed; the three-file public error-contract set migrated to `docs/api/assets/` |
| Historical cleanup | Stable Markdown dependency scan | No stable page links to `work-items/`, removed Jira-key paths, Mermaid source files, or inline Mermaid blocks |
| Historical cleanup | Diagram publication catalogue | Migrated to a neutral version-two owner/consumer contract |
| Historical cleanup | Structure, link, publication, and catalogue tests | Pass |
| Historical cleanup | `npm run diagrams:check` and `git diff --check` | Pass |
| Stable diagram set | `npm run diagrams:render` and `npm run diagrams:preview` | 21 canonical SVGs, 21 generated 2400-pixel PNGs, and 63 untracked review previews produced |
| Stable diagram set | Eleven database relational views | Redesigned with the approved KAN-34 white-and-blue visual language, stronger type, restrained relationship labels, and new cache-safe asset paths |
| Stable diagram set | Desktop, 390-pixel phone, and dark-surround inspection | Pass for all 13 entries; no clipping, collision, ambiguous routing, or transparent canvas |
| Stable diagram set | Public error-contract architecture | Corrected catalogue generation and HTTP documentation exposure into separate responsibilities |
| Stable diagram set | `DatabaseDocumentationContractIT` | Pass: all 35 Flyway-migrated PostgreSQL tables represented |
| Stable diagram set | Inventory, publication, structure, and link test gate | Pass |
| Architecture coverage | Layered entry points | System context, runtime, module catalogue, current dependency graph, request security, event delivery, and error disclosure published as separate reviewer questions |
| Architecture coverage | Module owner pages | All 11 production modules document purpose, entry points, application/domain rules, persistence, events, dependencies, security/errors, verification, and known gaps |
| Architecture coverage | `ArchitectureModuleCatalogTest` and `ArchitectureDocumentationCoverageTest` | Pass: source-derived module dependencies, minimal ownership catalogue, two reader routes, four capability pages, and all module owner sections agree |
| Architecture coverage | Documentation, link, catalogue, OpenAPI, exposure, and diagram test selection | Pass against PostgreSQL Testcontainers; Flyway V1 applied and Hibernate validation completed |
| Architecture coverage | Architecture publication set | Nine justified architecture figures plus the reused public-error figure; no generic platform-support duplicate |
| Database information model | Flyway-migrated PostgreSQL 16 catalogue | 35 tables, 46 FKs with nullability/delete behavior, 25 unique constraints, 57 checks, 19 partial indexes, and 12 triggers extracted directly from the effective schema |
| Database information model | `DatabaseDocumentationContractIT` | Pass: documentation compared with the effective PostgreSQL schema; no committed schema projection remains |
| Database information model | Relational journey and question chooser | All 35 tables placed across six business stages; eleven focused relationship questions linked |
| Database information model | `DatabaseDocumentationNavigationTest` | Pass: fast reader-navigation and entry-point boundaries enforced |
| Database information model | Focused relationship comparison | 46/46 Flyway foreign keys already represented; zero visual topology omissions, so approved SVG/PNG assets were preserved |
| Database information model | Exact relationship semantics | Every FK reference row now records constraint name, nullability, and `ON DELETE` behavior; all 6 intentional non-FK correlations are listed separately |
| Database information model | `DatabaseDocumentationContractIT` | Pass: focused references are checked directly against PostgreSQL catalogue facts, including all 6 intentional non-FK correlations |
| Database surface verification | Focused database contract, navigation, publication, structure, and link test selection | Pass |
| Database surface verification | `npm run diagrams:check` and `npm run diagrams:preview` | Revalidation now runs through the neutral 21-entry publication catalogue |
| Database surface verification | Eleven database views at 980 pixels, 390 pixels, and on a dark surrounding | Pass: opaque canvases, readable relationship maps, no clipping, collision, or ambiguous routing |
| Database surface verification | GitHub desktop and 390-pixel mobile web | Pass: all 11 SVGs loaded at their intrinsic resolution; no raw Mermaid presentation |
| Database surface verification | Jira named PNG viewer | Pass: the 2400×3000 architecture fixture opens without clipping; database PNGs use the same validated opaque 2400-pixel publication path |
| Database surface verification | Confluence delivery page | Pass: current KAN-39 status, PR link, ownership boundaries, database counts, and checkpoint history render correctly |
| Database surface verification | Native GitHub Mobile | Pass: user-device review confirmed the database page, embedded figures, and linked detail render correctly |
| Repository wording | PNG fallback labels | Pass: stable GitHub documentation uses the vendor-neutral `High-resolution PNG fallback`; Jira-specific wording remains in delivery evidence only |
| Database reader experience | Question-oriented hierarchy | Pass: one journey, one chooser, 11 focused owner pages, and one schema reference replace the 280-line ER page |
| Database reader experience | Duplicate schema projection removal | Pass: committed manifest removed; deterministic diagnostics are generated only under ignored `target/documentation-verification/` |
| Database reader experience | Relationship ownership | Pass: all 11 existing database publications now have one focused primary owner; schema claims remain unchanged |
| Database reader experience | Relational journey publication | Pass: desktop, 390-pixel phone, dark-surround, PNG, connector geometry, and catalogue checks |
| Database reader experience | Focused documentation gate | Pass: all 35 tables, all 46 foreign keys, all 6 intentional correlations, links, navigation, and publication contracts verified |
| Database reader experience | Task 5E checkpoint review | Approved after GitHub desktop/mobile review; proceed to transitional manifest replacement |
| Database reader experience | Task 5F effective-schema verification | Pass: unit and complete PostgreSQL integration profiles verify documentation from the Flyway-migrated PostgreSQL catalogue |
| Database reader experience | Task 5F checkpoint review | Approved after repository, PR, Jira, Confluence, local verification, and GitHub CI review; proceed to complete current-reality audit |

Temporary review sheets remain under `target/documentation-review/` and are
never committed.
