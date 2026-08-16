# KAN-25 Documentation Information Architecture

Status: Written specification awaiting review

Date: 2026-08-16

Jira: [KAN-25](https://0707manna0895.atlassian.net/browse/KAN-25)

## 1. Problem

The repository currently stores unrelated designs and implementation plans in a
single `docs/design` directory. As the number of work items grows, readers must
already know a filename or Jira key to find information. Global diagram assets
also lose a clear owner.

The documentation needs to support two different uses without mixing them:

1. explain the system as it works now; and
2. preserve the decisions and delivery evidence for a Jira work item.

## 2. Decision

Documentation will be organized by engineering subject. Each subject will
separate stable reference material from historical work-item records.

Jira keys remain in work-item directory names for traceability, but Jira is not
the primary navigation model. A reader should be able to find error-handling or
database documentation without knowing which ticket introduced it.

## 3. Target Structure

Only directories with real content will be created.

```text
docs/
|-- README.md
|-- architecture/
|   |-- README.md
|   |-- overview.mmd
|   |-- assets/
|   `-- work-items/
|       `-- KAN-25-documentation-information-architecture/
|           |-- design.md
|           `-- implementation-plan.md
|-- database/
|   |-- README.md
|   |-- migrations.md
|   |-- er-diagram.md
|   |-- er-diagram-source.md
|   |-- assets/
|   `-- work-items/
|       |-- KAN-12-migration-policy/
|       `-- KAN-14-database-foundation-release/
`-- error-handling/
    |-- README.md
    `-- work-items/
        |-- KAN-17-foundation/
        |-- KAN-20-neutral-contract/
        |-- KAN-21-rest-adapter/
        |-- KAN-22-mvc-adapter/
        |-- KAN-23-security-adapter/
        `-- KAN-24-module-migration/
            |-- design.md
            |-- implementation-plan.md
            `-- assets/
                |-- architecture.mmd
                |-- architecture.png
                |-- login-flow.mmd
                `-- login-flow.png
```

## 4. Document Classes

### 4.1 Stable reference documents

Stable references describe the current system and use semantic filenames rather
than Jira keys. Examples include `migrations.md`, `overview.mmd`, and each topic
`README.md`.

A stable reference must not require readers to reconstruct the current design
from several implementation plans. When later work changes the system, the
relevant stable reference is updated in the same delivery.

### 4.2 Work-item records

Work-item records explain why and how a bounded change was delivered. Their
directory follows this format:

```text
<topic>/work-items/<Jira-key>-<short-kebab-case-slug>/
```

Standard filenames inside that directory are:

- `design.md` for the approved design;
- `implementation-plan.md` for the approved delivery checklist and evidence;
- `assets/` for diagrams owned by that work item.

Not every ticket requires repository documentation. Small fixes remain in Jira,
commits, tests, and pull requests. A work-item directory is justified when a
change introduces an architectural decision, operational policy, migration,
security model, or substantial multi-step implementation.

## 5. Navigation

`docs/README.md` will be the documentation portal and provide three routes:

1. **By subject** — architecture, database, error handling, and future topics.
2. **By task** — understand the system, change the schema, add an API error, or
   review delivery history.
3. **By Jira key** — a compact index linking each documented work item to its
   canonical repository directory and Jira issue.

Every topic directory will contain a `README.md` that:

- states the purpose and boundaries of the topic;
- links stable current-system references first;
- lists work-item history separately and in numerical order; and
- links back to `docs/README.md`.

The repository root `README.md` will link to the documentation portal and retain
direct links only for the most important onboarding material.

## 6. Diagram Policy

Diagram source and rendered output will be stored together under the owning
document's `assets/` directory.

- Mermaid `.mmd` is the editable source when Mermaid is appropriate.
- PNG or SVG is the delivery format embedded in Markdown and Jira.
- PNG is preferred for Jira and mobile compatibility when SVG support is
  uncertain.
- Filenames describe the diagram within its local work-item context; the parent
  directory supplies the Jira identity.
- A diagram without a clear owning document is moved to the applicable stable
  architecture topic rather than a global catch-all assets folder.

## 7. Migration Rules

The reorganization will be mechanical and reviewable:

1. Start from the latest verified `develop` after KAN-24 is merged.
2. Move tracked files with `git mv` to retain history.
3. Repair repository-relative Markdown links and image references in the same
   commit that moves their targets.
4. Add navigation indexes after paths are stable.
5. Do not rewrite existing Git history or rename Jira issues.
6. Do not leave redirect files at old paths unless an external published link is
   known to depend on that path.
7. Verify that Git recognizes moves as renames where content is unchanged.

## 8. Automated Validation

An existing-JUnit repository test will validate local Markdown links and local
image targets beneath `docs/` and the root `README.md`.

The validator will:

- ignore external HTTP(S), email, and same-document anchor links;
- remove query strings and fragments before resolving local targets;
- decode URL-escaped path characters;
- resolve paths relative to the containing Markdown file;
- reject targets that escape the repository root; and
- report the source document and broken target in failures.

Using the existing test stack avoids a new package manager, runtime, build
plugin, or external link-checking service. External URL availability is outside
KAN-25 because it is network-dependent and can make CI nondeterministic.

## 9. Review and Verification

The implementation must prove:

- no tracked document is lost;
- every old document has one canonical destination;
- every local Markdown link and image target resolves;
- topic and Jira indexes link to the correct destinations;
- rendered KAN-24 PNG diagrams remain visible on GitHub mobile;
- the documentation-link test passes on Windows and GitHub Actions; and
- the complete existing unit and PostgreSQL integration suites remain green.

## 10. Delivery Boundaries

KAN-25 is a separate branch and pull request. It will be implemented only after
KAN-24 is merged into `develop`, preventing documentation restructuring from
obscuring KAN-24 application changes.

KAN-25 will not change:

- production Java behavior;
- database schema or Flyway migrations;
- application configuration;
- runtime dependencies;
- API contracts; or
- deployment behavior.

## 11. Acceptance Criteria

KAN-25 is complete when:

1. `docs/README.md` supports navigation by subject, task, and Jira key.
2. Stable references are separated from work-item history.
3. existing documents are relocated with history-preserving moves;
4. work-item assets reside beside their owning documents;
5. all repository-relative documentation links resolve;
6. an automated test detects broken local Markdown and image links;
7. GitHub mobile renders the checked-in KAN-24 diagrams;
8. the relevant documentation and application test suites pass; and
9. the pull request contains no KAN-24 implementation changes.
