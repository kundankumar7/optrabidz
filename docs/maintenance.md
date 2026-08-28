# Documentation Maintenance Map

[Back to the documentation portal](README.md)

Use this map after a code or configuration change. The repository source is
authoritative; update the corresponding guide and its tests in the same pull
request.

| Change area | Authoritative sources | Documentation to review |
|---|---|---|
| Java, Spring, dependencies, test profiles | `pom.xml`, `.github/workflows/` | Root README, getting started, operations |
| Runtime profiles and configuration | `src/main/resources/application*.properties` | Getting started, security, operations |
| Modules and dependency rules | `src/main/java/com/project/optrabidz/`, `src/test/java/com/project/optrabidz/architecture/` | Architecture, decisions |
| HTTP routes and OpenAPI | `**/api/*Controller.java`, `documentation/openapi/`, `documentation/security/` | API guide, security |
| Authentication and authorization | `security/infrastructure/config/`, `security/infrastructure/web/` | Security guide |
| Success and error contracts | `common/api/response/`, `common/api/error/`, `common/error/`, module `application/error/` catalogues | API guide, error contract, generated error catalogue |
| Schema and persistence | `src/main/resources/db/migration/`, module `infrastructure/entity/` and `infrastructure/repository/` packages | Database guide, migrations, ER diagrams |
| Events, audit, and delivery | `common/event/`, `common/outbox/`, `audit/`, `notification/` | Architecture, operations, outbox decision |
| Payment and webhook behavior | `financial/`, financial integration tests | Security, operations, API error catalogue |
| Diagram publication | `docs/architecture/diagram-publication/inventory.json`, diagram assets, render scripts | Owning guide and diagram publication guide |

## Update Rule

1. Change code and tests.
2. Identify affected rows in this map.
3. Update only current guidance; keep delivery discussion in Jira and the pull
   request.
4. Regenerate catalogues and diagrams from their canonical sources.
5. Run documentation structure, link, catalogue, and diagram checks.

Do not describe planned infrastructure as implemented. Kafka, Redis, JWT,
OAuth2, external notification providers, and real-money payment processing are
not present in the current repository.
