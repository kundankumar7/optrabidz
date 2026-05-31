# OptraBidz

OptraBidz is a web-based startup funding marketplace where startups publish
funding requirements and investors submit competing funding bids with proposed
terms.

The system is designed as a neutral coordination platform. It gives structure,
visibility, lifecycle rules, notifications, and audit records around the funding
negotiation process, while leaving final business decisions with the
participating users.

OptraBidz focuses on clear marketplace responsibilities: startup listing,
investor bidding, agreement recording, settlement and repayment outcome
tracking, notifications, governance, and auditability.

OptraBidz does not act as a lender, broker, escrow service, legal authority,
credit scoring engine, or real-money payment processor.

## Quick Start

Use Java 21 and provide a PostgreSQL database named `optrabidz`. If Docker is
available, start PostgreSQL with:

```powershell
docker run --name optrabidz-postgres -e POSTGRES_DB=optrabidz -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Then open Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## Project Purpose

Startups often need flexible funding options, but comparing offers from
different investors becomes difficult when each offer is handled through private
and unstructured communication. OptraBidz gives that process a clear structure:

- Startups describe funding needs through controlled listing records.
- Investors submit bids with comparable proposed terms.
- Startups can review competing offers without the system deciding for them.
- Agreements, settlement outcomes, repayment outcomes, notifications, and audit
  records are captured for traceability.

The main idea is neutrality. OptraBidz supports transparency and rule
enforcement, but it does not select, rank, approve, or financially judge bids.

## Project Scope

### Included

| Area | What the system supports |
|---|---|
| Accounts and sessions | Registration, login, logout, password change, active-session tracking, and role-aware access control |
| Role separation | Startup, investor, and admin responsibilities are kept separate |
| Profiles | Startup, investor, and admin profile records with completion checks |
| Classification | Startup classifications and investor preferences used for eligibility and listing discovery |
| Funding listings | Startup-created listing lifecycle from draft to published and closed states |
| Bids | Investor bid submission, withdrawal, rejection, acceptance, and accepted-bid exclusivity |
| Agreements | Agreement records created after a startup accepts a bid |
| Settlement and repayment | Local/sandbox payment-intent and payment-attempt flows for recording declared outcomes |
| Governance | Eligibility rules, lifecycle expiry rules, and admin authority transfer |
| Notifications | In-app notifications, sandbox email, sandbox push, subscriptions, retries, and delivery attempts |
| Audit | Business, security, governance, finance, and system audit records |
| Observability | Request IDs, security context in logs, operational logging, and sensitive-data masking |
| Verification | Automated unit and integration tests, with GitHub Actions for continuous verification |

### Not Included

| Area | Scope decision |
|---|---|
| Real fund transfer | OptraBidz records declared outcomes; it does not hold, move, verify, or settle real money |
| Financial intermediation | The platform is not a lender, broker, escrow agent, or guarantor |
| Bid decision automation | The system does not choose, approve, or financially judge bids |
| Credit scoring or risk scoring | Investor/startup creditworthiness is not calculated by OptraBidz |
| Legal enforcement | Agreements are recorded as platform outcomes, not enforced as legal contracts |
| KYC, AML, fraud automation | Identity/compliance verification is outside this version's scope |
| SMS/WhatsApp providers | Notification support focuses on in-app, email, and push channels |
| Real payment gateway | Local and sandbox payment providers are used because real fund processing is outside scope |

## Actor Model

| Actor | Main responsibility | Boundary |
|---|---|---|
| Startup | Create a startup profile, publish funding listings, review bids, and accept or reject bids | Cannot submit investor bids or act as an admin |
| Investor | Maintain an investor profile, set preferences, discover listings, and submit bids | Cannot create startup listings or decide other investors' bids |
| Admin | Review platform audit records and manage admin authority recovery/transfer | Cannot create listings, submit bids, decide funding outcomes, or override bid competitiveness |
| Notification services | Deliver configured notifications | Current channels are in-app, sandbox email, and sandbox push |

## What This Repository Includes

The server is organized by business area under
`src/main/java/com/project/optrabidz`.

| Module | Responsibility |
|---|---|
| `security` | Authentication, sessions, CSRF support, password handling, current-user endpoint |
| `identity` | Account identity and role state |
| `participation` | Startup, investor, and admin profile records |
| `classification` | Startup classifications and investor preference rules |
| `marketplace` | Funding listings, bids, agreements, marketplace visibility, and listing discovery |
| `financial` | Settlement, repayment, payment intents, local/sandbox providers, and HMAC webhook verification |
| `governance` | Eligibility rules, lifecycle expiry, admin bootstrap, and admin authority transfer |
| `notification` | Notification rules, feed endpoints, subscriptions, delivery dispatcher, sandbox email, and sandbox push |
| `audit` | Event-backed audit records and admin audit search |
| `common` | Standard responses, exceptions, outbox, domain events, and observability helpers |

## Main Workflow

1. A user registers with a role: startup, investor, or admin.
2. A startup completes its profile and classification details.
3. An investor completes its profile and investor preference details.
4. The startup creates a funding listing in draft state.
5. The startup publishes the listing when it is ready for investor visibility.
6. Eligible investors view open listings and submit bids with proposed terms.
7. The startup reviews visible bid attributes and decides whether to accept or
   reject a bid.
8. When one bid is accepted, OptraBidz creates an agreement and applies the
   accepted-bid exclusivity rule to the remaining active bids.
9. Settlement and repayment records track declared outcomes through local or
   sandbox payment flows.
10. Important user and system actions create notification and audit records.

## Architecture

OptraBidz uses a modular monolith architecture: one Spring Boot application,
separated into clear business modules. This keeps the project simple to run
while still making the domain boundaries visible.

| Layer | Responsibility |
|---|---|
| Access and security layer | Request controllers, sessions, CSRF handling, role checks, and standard responses |
| Application layer | Use cases, workflow coordination, permissions, policies, and business services |
| Domain layer | Business models, lifecycle states, invariants, and state-transition rules |
| Persistence layer | JPA entities, repository adapters, and PostgreSQL storage |
| Event layer | Outbox events used by audit and notification processors |
| Integration adapters | Local/sandbox payment strategies and sandbox notification channels |

<a href="docs/assets/optrabidz-architecture-overview.svg">
  <img src="docs/assets/optrabidz-architecture-overview.svg?v=20260531-1040" alt="OptraBidz architecture diagram">
</a>

Editable diagram source: [`docs/architecture.mmd`](docs/architecture.mmd).

## API Reference

All application endpoints are versioned under `/api/v1`.

When the application runs locally, Swagger UI is generated from the controller
mappings and request/response DTOs. It is a developer-facing page for reviewing
available endpoints, request bodies, response formats, status codes, and
schemas. After starting the application locally, it is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

The table below is a route map for the main business areas.

| Business area | Purpose | Route groups |
|---|---|---|
| Authentication | Register, login, logout, and change password | `/api/v1/auth/*` |
| Current user | Return the logged-in user's identity and role context | `/api/v1/me` |
| Startup profiles | Create, view, and update startup profile data | `/api/v1/startups/*` |
| Investor profiles | Create, view, and update investor profile data | `/api/v1/investors/*` |
| Classification | Manage startup classifications and investor preferences | `/api/v1/startup-classifications/*`, `/api/v1/investor-preferences/*` |
| Funding listings | Create, update, publish, close, list, and view funding listings | `/api/v1/funding-listings/*`, `/api/v1/startups/me/funding-listings` |
| Bids | Submit, view, withdraw, reject, and accept bids | `/api/v1/bids/*`, `/api/v1/investors/me/bids/*` |
| Agreements | View agreement records for startups and investors | `/api/v1/agreements/*`, `/api/v1/startups/me/agreements`, `/api/v1/investors/me/agreements` |
| Finance | View settlements, repayments, installments, payment intents, and attempts | `/api/v1/settlements/*`, `/api/v1/repayments/*`, `/api/v1/repayment-installments/*`, `/api/v1/payment-intents/*` |
| Payment simulation | Confirm or fail local payment attempts and receive sandbox webhooks | `/api/v1/payment-attempts/*`, `/api/v1/payment-providers/*/webhooks` |
| Notifications | View notifications, mark read, delete, and manage subscriptions | `/api/v1/notifications/*`, `/api/v1/notification-subscriptions/*` |
| Audit and admin recovery | Search audit records and transfer admin authority during recovery | `/api/v1/admin/audit-records`, `/api/v1/admin/recovery/*` |

## Database Design

| File | Purpose |
|---|---|
| [`docs/database/optrabidz-schema.sql`](docs/database/optrabidz-schema.sql) | Full PostgreSQL schema reference |
| [`docs/database/er-diagram.md`](docs/database/er-diagram.md) | ER diagram reference |

## Security Model

- Authentication is handled through Spring Security.
- Login uses a server-side session with the `JSESSIONID` cookie.
- State-changing requests are protected by CSRF handling.
- Startup, investor, and admin operations are separated by role checks.
- Account/session state is checked before protected access.
- Failed login and authorization failures are recorded in the audit system.
- Sensitive values are masked in logs and audit details.

## External Providers

OptraBidz uses controlled local/sandbox adapters so the system can be reviewed
and tested without paid accounts or live financial services.

| Area | Implementation |
|---|---|
| Email notification | Sandbox email channel |
| Push notification | Sandbox push channel |
| In-app notification | Stored notification feed inside OptraBidz |
| Payment flow | Local provider plus sandbox UPI/card strategies |
| Webhook verification | HMAC verification for sandbox provider callbacks |
| SMS/WhatsApp | Not configured |
| KYC/verification | Not configured |

## Technology Stack

- Java 21
- Spring Boot 3.3.2
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven Wrapper
- Swagger UI
- JUnit 5
- Testcontainers
- GitHub Actions

## Running Locally

This section is for developers or reviewers who want to run the server on their
machine. The application needs PostgreSQL because it persists accounts,
profiles, listings, bids, agreements, audit records, notifications, and payment
simulation records.

Use Java 21, then provide a PostgreSQL database named `optrabidz`. No private
database backup is required. The application can start with an empty database;
the database schema reference is included under `docs/database/`.

If PostgreSQL is already installed, create the database locally. If Docker is
available, the following command can start a local PostgreSQL container:

```powershell
docker run --name optrabidz-postgres -e POSTGRES_DB=optrabidz -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

For macOS or Linux:

```bash
./mvnw spring-boot:run
```

The default local database settings are:

| Property | Default |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/optrabidz` |
| `spring.datasource.username` | `postgres` |
| `spring.datasource.password` | `postgres` |

These settings can be overridden with environment variables:

| Environment variable | Purpose |
|---|---|
| `OPTRABIDZ_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `OPTRABIDZ_DATASOURCE_USERNAME` | PostgreSQL username |
| `OPTRABIDZ_DATASOURCE_PASSWORD` | PostgreSQL password |
| `OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED` | Enable or disable local admin bootstrap |
| `OPTRABIDZ_ADMIN_BOOTSTRAP_EMAIL` | Bootstrap admin email |
| `OPTRABIDZ_ADMIN_BOOTSTRAP_PASSWORD` | Bootstrap admin password |
| `OPTRABIDZ_ADMIN_RECOVERY_TOKEN` | Local admin recovery token |
| `OPTRABIDZ_UPI_WEBHOOK_SECRET` | Sandbox UPI webhook HMAC secret |
| `OPTRABIDZ_CARD_WEBHOOK_SECRET` | Sandbox card webhook HMAC secret |

After startup, open Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Optional manual schema initialization:

```powershell
psql -U postgres -d optrabidz -f docs/database/optrabidz-schema.sql
```

## Testing

Run the fast verification command:

```powershell
.\mvnw.cmd test
```

Run the full integration test command:

```powershell
.\mvnw.cmd verify -Pintegration-tests
```

The integration command uses Testcontainers PostgreSQL, so Docker must be
running. The fast test command is the same command used by CI.

The test suite is expected to complete with:

```text
0 failures, 0 errors
```

## Continuous Integration

GitHub Actions verifies the project on push, pull request, and manual trigger.
The workflow uses Java 21 and runs:

```bash
./mvnw -B test
```

The workflow file is stored under `.github/workflows/`.
