# KAN-39 Documentation Experience Implementation Plan

**Status:** Approved — execution in progress

**Goal:** Provide two clear documentation routes, publish a complete and
readable architecture/database view set, and verify database documentation
against the effective Flyway-migrated PostgreSQL schema.

**Architecture:** The documentation portal separates understanding the system
from changing or verifying it. System-wide views lead to four reusable
capability views and then to the 11 module references; database navigation leads
from an end-to-end relational journey to 11 focused views. Publication tests
enforce a lean neutral diagram catalogue, canonical reusable SVG/PNG pairs, and
the nine justified architecture questions. PostgreSQL integration tests apply
all Flyway migrations and verify documentation against the effective
catalogue.

**Tech stack:** Markdown, SVG, PNG, Node.js, Mermaid CLI 11.16.0, Sharp 0.35.4,
Java 21, Jackson, ImageIO, JUnit 5, AssertJ, Maven Surefire/Failsafe, Flyway,
PostgreSQL 16, Testcontainers

**Spec:** [KAN-39 renewed design](design.md)

## Global Constraints

- Do not change production Java, business rules, APIs, security, database
  schemas, runtime dependencies, or deployment behavior.
- Preserve the accepted KAN-34 appearance for flow diagrams; improve only
  clarity, spacing, routing, contrast, output quality, and mobile composition.
- Do not introduce a repository-wide diagram brand, decorative palette,
  gradients, shadows, stock icons, or cosmetic rewrites.
- GitHub documentation embeds SVG; Jira and offline review use an opaque
  high-resolution PNG fallback.
- Reader-facing Markdown must not link `.mmd` files or contain Mermaid fences.
- Every published diagram has exactly one canonical source: constrained
  Mermaid or curated SVG.
- Dense diagrams are split instead of relying on zoom.
- Stable guidance is distilled before obsolete work-item documents or assets
  are removed.
- GitHub Mobile is a real-device acceptance surface, not an inferred result.
- Temporary review images belong under `target/documentation-review/` and are
  never committed.
- Flyway remains the sole schema authority; a committed schema projection must
  not become permanent documentation.
- Keep `schema-manifest.json` until catalogue verification replaces every test
  that consumes it, then remove it in the same reviewed checkpoint.
- Generated schema diagnostics belong under
  `target/documentation-verification/` and are never committed.
- Machine catalogues contain only intentional configuration; generated counts,
  dependencies, review status, and tool-specific labels are not committed.
- A page does not receive a diagram unless the visual answers a distinct
  relationship, hierarchy, sequence, state, or boundary question.
- Use short, human-readable commit subjects with `KAN-39`; do not use robotic
  scope prefixes or AI-related wording.
- Do not merge the pull request without normal review.

---

## File Map

### Stable documentation

| File | Responsibility |
|---|---|
| `README.md` | Concise product, scope, architecture, and documentation entry point |
| `docs/README.md` | Task-oriented documentation portal without a work-item catalogue |
| `docs/getting-started/README.md` | Prerequisites, profiles, local startup, and verification |
| `docs/architecture/README.md` | Current system, module, persistence, and event-delivery views |
| `docs/architecture/module-catalog.json` | Minimal module-to-capability and module-to-owner build mapping; not reader navigation |
| `docs/architecture/capabilities/README.md` | Capability map linking four shared views to all 11 modules |
| `docs/architecture/capabilities/*.md` | Reusable identity/access, marketplace, finance/payment, and platform-support views |
| `docs/architecture/modules/*.md` | Current module ownership and change/verification references |
| `docs/api/README.md` | API boundary, documentation profile, response, and versioning guidance |
| `docs/api/errors.md` | Current neutral exception and RFC 9457 contract |
| `docs/api/error-catalogue.md` | Generated public error catalogue moved from `docs/error-handling/` |
| `docs/database/README.md` | Database navigation and schema ownership |
| `docs/database/migrations.md` | Flyway authoring and recovery policy |
| `docs/database/relationship-journey.md` | End-to-end relational narrative and overview figure |
| `docs/database/views/README.md` | Question-based chooser for 11 focused database views |
| `docs/database/views/*.md` | One bounded relationship question and its exact semantics per page |
| `docs/database/reference/README.md` | Database notation, verification method, and non-FK semantics |
| `docs/security/README.md` | Current session security boundary and replaceable authentication adapters |
| `docs/operations/README.md` | Profiles, configuration, outbox workers, Docker, and operational checks |
| `docs/decisions/README.md` | Durable decision-record index |
| `docs/decisions/0001-modular-monolith.md` | Why the current deployment remains a modular monolith |
| `docs/decisions/0002-flyway-schema-ownership.md` | Why Flyway owns schema evolution |
| `docs/decisions/0003-transactional-outbox.md` | Why audit and notification delivery use committed outbox records |
| `docs/decisions/0004-problem-details.md` | Why transport-neutral errors map to RFC 9457 at adapters |

### Diagram publication

| File | Responsibility |
|---|---|
| `docs/architecture/diagram-publication.md` | Durable source, render, fallback, and review policy |
| `docs/architecture/diagram-publication/diagram-publications.json` | Neutral build catalogue for canonical diagram sources, SVG/PNG outputs, owners, and consumers |
| `docs/architecture/diagram-publication/mermaid-config.json` | Pinned safe Mermaid configuration |
| `scripts/render-documentation-diagrams.mjs` | Render Mermaid SVG and derive PNG from either source type |
| `scripts/render-documentation-previews.mjs` | Generate untracked desktop and phone review sheets |
| `docs/architecture/assets/optrabidz-system-overview.svg` | Compact curated architecture source and GitHub image |
| `docs/architecture/assets/optrabidz-system-overview.png` | High-resolution fallback derived from the SVG with a cache-safe publication identity |
| `docs/architecture/assets/optrabidz-module-map.svg` | Focused module view when approved by the disposition audit |
| `docs/architecture/assets/optrabidz-module-map.png` | Jira fallback derived from the module SVG |
| `docs/architecture/assets/optrabidz-event-delivery.svg` | Focused transaction/outbox/delivery view when approved |
| `docs/architecture/assets/optrabidz-event-delivery.png` | Jira fallback derived from the event SVG |

### Validation and evidence

| File | Responsibility |
|---|---|
| `src/test/java/com/project/optrabidz/documentation/DocumentationStructureValidator.java` | Validate stable hierarchy and reader-facing Markdown rules |
| `src/test/java/com/project/optrabidz/documentation/DocumentationStructureValidatorTest.java` | Fixture-test structure violations |
| `src/test/java/com/project/optrabidz/documentation/DocumentationStructureTest.java` | Apply structure rules to the repository |
| `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidator.java` | Validate canonical sources and safe published outputs |
| `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidatorTest.java` | Fixture-test source and output rules |
| `src/test/java/com/project/optrabidz/documentation/DiagramPublicationTest.java` | Apply diagram rules to the repository |
| `src/test/java/com/project/optrabidz/documentation/DocumentationLinksTest.java` | Preserve the repository-wide broken-link gate |
| `src/test/java/com/project/optrabidz/documentation/database/PostgresSchemaIntrospector.java` | Read tables, columns, keys, checks, partial indexes, and triggers from PostgreSQL |
| `src/test/java/com/project/optrabidz/documentation/database/DatabaseSchemaSnapshot.java` | Immutable effective-schema model used by documentation verification |
| `src/test/java/com/project/optrabidz/documentation/database/DatabaseDocumentationContractIT.java` | Apply Flyway and compare human documentation with the effective schema |
| `src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueMarkdownSnapshotTest.java` | Track the moved public catalogue path |
| `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md` | Record content disposition and review results during delivery |

---

## Completed Delivery History

Tasks 1 through 5B record already completed checkpoints. They retain the file
and field names used at execution time and are not instructions for remaining
work. The corrected active plan begins at Task 5C and replaces the legacy
module and diagram inventories before further publication work.

## Task 1: Lock the Documentation and Canonical-Source Rules

**Files:**

- Create: `src/test/java/com/project/optrabidz/documentation/DocumentationStructureValidator.java`
- Create: `src/test/java/com/project/optrabidz/documentation/DocumentationStructureValidatorTest.java`
- Create: `src/test/java/com/project/optrabidz/documentation/DocumentationStructureTest.java`
- Modify: `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidator.java`
- Modify: `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidatorTest.java`
- Modify: `docs/architecture/diagram-publication/inventory.json`
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`

**Interfaces:**

- `DocumentationStructureValidator.findViolations(Path root)` returns an
  ordered `List<Violation>`.
- `DocumentationStructureValidator.Violation` contains normalized `path` and
  `reason` values.
- Diagram `sourceType` is exactly `MERMAID_FILE` or `CURATED_SVG`.
- For `CURATED_SVG`, `source` and `githubSvg` resolve to the same path.
- For `MERMAID_FILE`, `source` ends in `.mmd` and differs from `githubSvg`.

- [ ] **Step 1: Add failing structure fixtures**

Create isolated `@TempDir` repositories proving these exact violations:

```java
assertThat(DocumentationStructureValidator.findViolations(repository))
        .extracting(DocumentationStructureValidator.Violation::reason)
        .contains(
                "required documentation entry does not exist",
                "reader-facing Markdown links Mermaid source",
                "reader-facing Markdown contains a Mermaid fence",
                "stable documentation links a work-item implementation plan");
```

Passing fixtures must include relative SVG/PNG links, fenced Java or shell
examples, external links, and ordinary `.md` links.

- [ ] **Step 2: Add failing canonical-source fixtures**

Extend `DiagramPublicationValidatorTest` with:

```java
assertThat(DiagramPublicationValidator.findViolations(repository))
        .extracting(DiagramPublicationValidator.Violation::reason)
        .contains(
                "curated SVG source must equal the published SVG",
                "Mermaid source must use the .mmd extension",
                "diagram source type is unsupported");
```

- [ ] **Step 3: Run the focused tests and confirm RED**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationStructureValidatorTest,DiagramPublicationValidatorTest" test
```

Expected: compilation or assertion failure for the new rules.

- [ ] **Step 4: Implement the minimal validators**

Use these records and signatures:

```java
final class DocumentationStructureValidator {
    static List<Violation> findViolations(Path repositoryRoot) throws IOException;
    record Violation(String path, String reason) {}
}

enum SourceType {
    MERMAID_FILE,
    CURATED_SVG
}
```

Scan `README.md` and Markdown beneath `docs/`, excluding fenced code before
matching links. Treat Markdown containing `work-items/` as stable only when the
source itself is not under a `work-items` directory. Require the eight approved
topic entries from the specification. Sort violations by path and reason.

- [ ] **Step 5: Create the content-disposition audit**

Add one row for every Markdown file beneath `docs/**/work-items/` with exactly
one disposition:

- `DISTILL_REMOVE` — current guidance moves to a stable topic; history remains
  in Jira, pull requests, commits, and Git;
- `MIGRATE_GUIDE` — the document contains a coherent current guide worth
  rewriting under a stable topic;
- `MIGRATE_DIAGRAM` — one diagram answers a durable reader question and moves
  with a stable guide;
- `ACTIVE_RECORD` — only the in-progress KAN-39 design, plan, and audit.

Each row records the target stable file or `none`, the reusable facts, and the
asset disposition. No file may remain unclassified.

- [ ] **Step 6: Run tests and commit the safeguards**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationStructureValidatorTest,DiagramPublicationValidatorTest" test
git add src/test/java/com/project/optrabidz/documentation docs/architecture/diagram-publication docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md
git commit -m "Define documentation quality gates (KAN-39)"
```

Expected: fixture tests pass. Repository-wide structure enforcement is added
only after the stable hierarchy exists.

---

## Task 2: Prove the Architecture Publication Prototype

**Files:**

- Modify: `README.md`
- Modify: `docs/architecture/README.md`
- Delete after replacement: `docs/architecture/overview.mmd`
- Delete after replacement:
  `docs/architecture/assets/optrabidz-architecture-overview.svg`
- Delete after replacement:
  `docs/architecture/assets/optrabidz-architecture-overview.png`
- Create: `docs/architecture/assets/optrabidz-system-overview.svg`
- Create: `docs/architecture/assets/optrabidz-system-overview.png`
- Create only when needed to keep the overview compact:
  `docs/architecture/assets/optrabidz-module-map.svg`
- Create matching PNG when the module map is created
- Create only when needed to keep the overview compact:
  `docs/architecture/assets/optrabidz-event-delivery.svg`
- Create matching PNG when the event view is created
- Modify: `docs/architecture/diagram-publication/inventory.json`
- Modify: `scripts/render-documentation-diagrams.mjs`
- Create: `scripts/render-documentation-previews.mjs`
- Modify: `package.json`
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`

**Interfaces:**

- The overview contains no more than seven primary nodes: clients, HTTP and
  security boundary, modular monolith, PostgreSQL, outbox, audit, and
  notification delivery.
- Module names or event details move to focused figures when they cannot remain
  readable in the overview.
- `npm run diagrams:preview` writes one directory per inventory ID beneath
  `target/documentation-review/`; the prototype command selects the exact
  `architecture-system-overview` ID.

- [ ] **Step 1: Add the curated-source inventory fixture and confirm RED**

Set the architecture entry to `CURATED_SVG` with the SVG itself as both
`source` and `githubSvg`. Run:

```powershell
.\mvnw.cmd -q "-Dtest=DiagramPublicationTest" test
```

Expected: failure until the competing `.mmd` source link and old asset mapping
are removed.

- [ ] **Step 2: Build the compact light-canvas overview**

Preserve the existing light, readable documentation appearance. Use one
top-to-bottom reading direction, explicit labels, an opaque background,
`<title>`, `<desc>`, and no scripts, external resources, `foreignObject`,
gradients, shadows, or decorative icons. Do not include class-level detail.

- [ ] **Step 3: Split only when readability requires it**

If the seven-node overview still fails at normal phone width, create the module
map and event-delivery figures listed above. The overview then links to those
focused questions instead of shrinking its labels.

- [ ] **Step 4: Generate PNG fallbacks and review sheets**

Add this script entry:

```json
"diagrams:preview": "node scripts/render-documentation-previews.mjs"
```

First update the main renderer so `MERMAID_FILE` invokes Mermaid before Sharp,
while `CURATED_SVG` skips Mermaid and derives the PNG directly from its SVG
source. The preview script must read the same inventory, use Sharp to render
980-pixel and 390-pixel previews onto an opaque background, and refuse output
paths outside `target/documentation-review/`.

Run:

```powershell
npm run diagrams:render -- --id architecture-system-overview
npm run diagrams:preview -- --id architecture-system-overview
npm run diagrams:check
```

- [ ] **Step 5: Update reader-facing architecture pages**

Embed the SVG, link the PNG as `High-resolution PNG fallback`, add a short
textual interpretation, and remove the reader-facing
`.mmd` link. Do not mention local approval procedure or tooling authorship.

- [ ] **Step 6: Run the prototype tests**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationLinksTest,DocumentationLinkValidatorTest,DiagramPublicationValidatorTest,DiagramPublicationTest" test
npm run diagrams:check
```

Expected: all commands pass.

- [ ] **Step 7: Commit and publish the prototype for surface review**

```powershell
git add README.md docs/architecture package.json package-lock.json scripts/render-documentation-previews.mjs
git commit -m "Make the architecture overview readable (KAN-39)"
git push -u origin fix/KAN-39-documentation-experience
```

Open or update the draft pull request. Verify the SVG on GitHub desktop and
mobile web, then verify the same page in the GitHub Mobile app. Attach the
purposefully named Jira fallback, such as
`KAN-39-architecture-overview-approved.png`, and record pass/fail evidence in
Jira and `audit.md`. Do not begin Task 3 until the prototype passes.

---

## Task 3: Build the Stable Documentation Hierarchy

**Files:**

- Modify: `README.md`
- Replace: `docs/README.md`
- Create the stable files listed in the File Map
- Move: `docs/error-handling/error-catalogue.md` to
  `docs/api/error-catalogue.md`
- Distil: `docs/error-handling/README.md` into `docs/api/errors.md`
- Modify:
  `src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueMarkdownSnapshotTest.java`
- Modify: any generator or test constant that still targets the old catalogue
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`

**Interfaces:**

- `docs/README.md` routes by reader task, not Jira key.
- The catalogue generator writes `docs/api/error-catalogue.md`.
- Security guidance describes current session authentication and replaceable
  adapters; it does not claim JWT or OAuth2 is implemented.
- Operations guidance names configuration variables but never contains secret
  values or machine-specific paths.

- [ ] **Step 1: Write failing repository structure assertions**

Create `DocumentationStructureTest`:

```java
@Test
void repositoryDocumentationFollowsTheStableHierarchy() throws Exception {
    assertThat(DocumentationStructureValidator.findViolations(Path.of(".")))
            .isEmpty();
}
```

Run:

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationStructureTest" test
```

Expected: FAIL because the approved hierarchy is incomplete and stable pages
still link work-item plans.

- [ ] **Step 2: Create the topic entry points**

Write the exact responsibilities from the File Map. Reuse verified current
facts from the disposition audit. Do not copy delivery chronology, approval
gates, local workspace paths, or completed implementation commands into stable
guidance.

- [ ] **Step 3: Move the public error reference**

Update the snapshot test target to:

```java
private static final Path CATALOGUE = Path.of(
        "docs", "api", "error-catalogue.md");
```

Run the existing controlled regeneration and parity test:

```powershell
.\mvnw.cmd -q "-Dtest=ErrorCatalogueMarkdownSnapshotTest" "-Doptrabidz.update-error-catalogue=true" test
.\mvnw.cmd -q "-Dtest=ErrorCatalogueMarkdownSnapshotTest" test
```

- [ ] **Step 4: Condense the root README and portal**

Keep product purpose, explicit non-goals, one architecture figure, the shortest
working quick start, and links to stable topics. Remove duplicated Swagger,
setup, architecture, and work-item-index sections. State accurately that API
documentation is enabled by the `dev` profile rather than universally.

- [ ] **Step 5: Add durable decision records**

Each decision record contains `Context`, `Decision`, `Consequences`, and
`Alternatives considered`. Distil only implemented decisions; future Kafka,
Redis, JWT, OAuth2, and real payment integrations remain roadmap statements in
the relevant topic, not current architecture claims.

- [ ] **Step 6: Run structure, link, and catalogue tests**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationStructureValidatorTest,DocumentationStructureTest,DocumentationLinksTest,DocumentationLinkValidatorTest,ErrorCatalogueMarkdownSnapshotTest" test
```

Expected: all commands pass for the new hierarchy while historical directories
remain temporarily available for Task 4.

- [ ] **Step 7: Commit the stable guidance**

```powershell
git add README.md docs/getting-started docs/architecture docs/api docs/database docs/security docs/operations docs/decisions docs/README.md src/test/java/com/project/optrabidz/documentation
git commit -m "Organize current engineering guidance (KAN-39)"
```

---

## Task 4: Distil and Remove Historical Work-Item Noise

**Files:**

- Remove or migrate files under `docs/**/work-items/` according to `audit.md`
- Remove `docs/error-handling/` after its stable content is migrated
- Modify all stable Markdown links affected by removals
- Modify: `docs/architecture/diagram-publication/inventory.json`
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`

**Interfaces:**

- Task 4 consumes the complete disposition table from Task 1.
- Stable guidance is checked against authoritative code, configuration,
  migrations, tests, and CI before historical explanations are removed.
- Each reviewed topic is classified as implemented, partially implemented,
  planned, or outdated; only implemented facts are stated as current behavior.
- `ACTIVE_RECORD` protects only the current KAN-39 design, implementation plan,
  and audit until delivery completes.
- A `MIGRATE_DIAGRAM` asset must have a stable owner before its historical
  directory is removed.

- [x] **Step 1: Establish the code-to-documentation truth baseline**

Compare every stable topic with its authoritative repository sources:

- Maven build and Java/Spring versions;
- application profiles and configuration;
- modules, controllers, and public HTTP routes;
- session authentication, authorization, and security adapters;
- exception catalogues and Problem Details mapping;
- Flyway migrations, entities, and repository relationships;
- outbox, audit, notification, payment, webhook, and scheduled processing; and
- unit, integration, CI, and operational constraints.

Record the source paths, classification, and any correction in `audit.md`.
Future Kafka, Redis, JWT, OAuth2, external notification providers, and
real-money payment processing remain explicitly unimplemented until code and
tests prove otherwise.

- [x] **Step 2: Verify every work-item file has a disposition**

Run a PowerShell comparison between `rg --files docs | rg 'work-items'` and the
audit table. Expected: zero unclassified files. Record counts for each
disposition in `audit.md`.

- [x] **Step 3: Migrate approved durable diagrams and explanations**

Move each `MIGRATE_DIAGRAM` source and output to its stable topic `assets/`
directory, rename it for the reader question rather than its Jira key, and
update the inventory owner. Preserve KAN-34-style flow appearance where the
source is a flow diagram.

- [x] **Step 4: Remove distilled historical records and orphan assets**

Delete only `DISTILL_REMOVE` and completed `MIGRATE_*` files. Do not remove the
active KAN-39 record. Use `rg` before each directory removal to prove no stable
Markdown link still targets it.

- [x] **Step 5: Prove no stable page depends on work-item history**

```powershell
rg -n "work-items/|\.mmd(?:[)#?]|$)|```mermaid" README.md docs --glob "*.md"
.\mvnw.cmd -q "-Dtest=DocumentationStructureTest,DocumentationLinksTest,DocumentationLinkValidatorTest" test
```

Expected: `rg` returns only links inside the active KAN-39 record; tests pass.

- [x] **Step 6: Commit the historical cleanup**

```powershell
git add -A docs
git commit -m "Remove obsolete delivery records (KAN-39)"
```

---

## Task 5: Remediate the Surviving Diagram Set

**Files:**

- Modify only diagram sources, SVGs, PNGs, and owner pages surviving Task 4
- Modify: `docs/architecture/diagram-publication/inventory.json`
- Modify: `docs/architecture/diagram-publication.md`
- Modify: `scripts/render-documentation-diagrams.mjs`
- Modify: `scripts/render-documentation-previews.mjs`
- Modify: diagram validator and tests
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`

**Interfaces:**

- The inventory contains no historical or orphan diagram entry.
- Mermaid sources generate SVG and PNG; curated SVG sources generate PNG only.
- Owner Markdown embeds SVG and links PNG; it never links source.
- Rendering `--check` performs no writes.

- [x] **Step 1: Make the inventory represent only stable reader questions**

Remove entries whose owner was deleted. Update migrated owner/source/output
paths. Reject duplicate IDs, duplicate published SVGs, and output paths outside
the repository in validator fixtures.

- [x] **Step 2: Redesign the surviving relational views**

Redesign every surviving ER asset as a focused relational view using the
approved KAN-34 visual language. Publish it under a descriptive cache-safe path
so GitHub Mobile cannot reuse the previous artwork. Render and preview the
complete stable inventory; the renderer itself selects Mermaid only for
`MERMAID_FILE` entries:

```powershell
npm run diagrams:render
npm run diagrams:preview
```

Preserve schema meaning while improving typography, contrast, routing, labels,
and canvas use. Confirm each replacement differs from its previous SVG and PNG;
do not count a regenerated fallback as a redesign.

- [x] **Step 3: Derive fallbacks from curated SVG sources**

For each `CURATED_SVG`, use Sharp to flatten the same SVG onto an opaque
background and write a 2400-pixel-wide PNG. Never maintain a separately drawn
PNG.

- [x] **Step 4: Complete manual visual evidence**

For every surviving diagram, record desktop, 390-pixel preview, dark
surrounding contrast, and Jira PNG results. The architecture fixture already
has a separate GitHub Mobile result; repeat device review for any figure whose
layout materially differs.

- [x] **Step 5: Run publication tests and commit**

```powershell
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=DiagramPublicationValidatorTest,DiagramPublicationTest,DocumentationStructureTest,DocumentationLinksTest,DocumentationLinkValidatorTest" test
git add docs scripts package.json package-lock.json src/test/java/com/project/optrabidz/documentation
git commit -m "Publish the stable diagram set (KAN-39)"
```

---

## Task 5A: Correct the Architecture Coverage

The first Task 5 publication pass is historical evidence, not final acceptance.
Review found that it restyled the surviving figures without documenting the
complete module model.

- [x] **Step 1: Build the code-to-documentation inventory**

  Record all 11 production modules, their owner pages, source/test roots,
  controller and service surfaces, repositories, events/outbox boundaries,
  security adapters, tests, and current direct imports. Enforce the inventory
  against Java source so a new or moved surface fails documentation checks.

- [x] **Step 2: Publish the layered architecture entry points**

  Add distinct system-context, runtime, complete-module, dependency, and
  cross-cutting flow pages. Do not compress all concerns into the overview.

- [x] **Step 3: Publish one owned page for every module**

  Each page must cover purpose, entry points, application/domain rules,
  persistence, events, dependencies, security/error boundaries, verification,
  and known gaps using repository evidence.

- [x] **Step 4: Verify module coverage and navigation**

  Extend documentation tests to require every inventory owner page and its
  standard reviewer sections, then run structure and link checks.

## Task 5B: Correct the Database Information Model

- [x] **Step 1: Build the Flyway relationship manifest**

  Capture all 35 tables and 46 foreign keys with child/parent columns,
  nullability, and delete behaviour. Record material checks, partial indexes,
  triggers, and intentional non-FK correlations separately.

- [x] **Step 2: Publish the relational journey and question chooser**

  Provide a mobile-readable overview and route each reviewer question to a
  focused relationship view.

- [x] **Step 3: Redesign focused relationship views from the manifest**

  Show exact relationship semantics and adjacent invariants. Never depict a
  correlation or trigger rule as a foreign key. The manifest comparison found
  all 46 foreign-key relationships already present in the approved focused
  views, so the images were preserved. Their reference tables now expose exact
  constraint names, nullability, and delete behavior, while all six non-FK
  correlations are listed separately.

- [x] **Step 4: Verify complete schema semantics and all reader surfaces**

  Compare the manifest with Flyway and verify the published set on desktop,
  phone, GitHub Mobile, Jira, and local light/dark surroundings.

  Automated manifest comparison, local 980-pixel, local 390-pixel,
  dark-surround, GitHub desktop, GitHub mobile-web, Jira PNG, and Confluence
  checks pass. Native GitHub Mobile was confirmed on the same published set.

---

The Task 5A and 5B checkmarks record completed intermediate deliveries. They do
not represent final KAN-39 acceptance: review found missing architecture
figures, compressed database navigation, and a transitional schema manifest in
the reader path. Tasks 5C through 5G close those gaps before Task 6 begins.

## Remaining Implementation

## Task 5C: Establish the Two Reader Routes

**Files:**

- Modify: `docs/README.md`
- Modify: `docs/architecture/README.md`
- Create: `docs/architecture/capabilities/README.md`
- Create: `docs/architecture/capabilities/identity-access.md`
- Create: `docs/architecture/capabilities/marketplace.md`
- Create: `docs/architecture/capabilities/finance-payments.md`
- Create: `docs/architecture/capabilities/platform-support.md`
- Create: `docs/architecture/module-catalog.json`
- Delete after migration: `docs/architecture/modules/inventory.json`
- Modify: `docs/architecture/modules/README.md`
- Modify: all 11 files under `docs/architecture/modules/`
- Modify: `src/test/java/com/project/optrabidz/documentation/ArchitectureDocumentationCoverageTest.java`
- Create: `src/test/java/com/project/optrabidz/documentation/ArchitectureModuleCatalogTest.java`
- Delete after migration: `src/test/java/com/project/optrabidz/documentation/ArchitectureModuleInventoryTest.java`

**Interfaces:**

- `docs/README.md` exposes `Understand the system` and `Change or verify the
  system` as the two primary routes.
- Each module catalogue item contains only `name`, `capability`, and
  `ownerPage`.
- Each module page links exactly one shared capability page.
- Capability grouping aids navigation but does not hide any module boundary.
- Source/test roots, surface counts, and dependencies are derived by tests and
  are not committed as expected JSON values.

- [x] **Step 1: Write failing route and capability-ownership tests**

Extend `ArchitectureDocumentationCoverageTest` with the approved capability
pages and require the two reader routes:

```java
private static final List<String> CAPABILITY_PAGES = List.of(
        "capabilities/identity-access.md",
        "capabilities/marketplace.md",
        "capabilities/finance-payments.md",
        "capabilities/platform-support.md");

assertThat(Files.readString(Path.of("docs", "README.md")))
        .contains("## Understand the system")
        .contains("## Change or verify the system");
```

Create `ArchitectureModuleCatalogTest` to read `module-catalog.json`, require
exactly one catalogue entry for every top-level production package, and require
a `capability` value from `identity-access`, `marketplace`,
`finance-payments`, or `platform-support`. Keep the existing source scan for
dependencies and require every derived module edge in
`module-dependencies.md`. Do not persist source-file, surface, or test counts;
they are diagnostic measurements rather than architecture contracts.

- [x] **Step 2: Run the focused tests and confirm the red phase**

```powershell
.\mvnw.cmd -q "-Dtest=ArchitectureDocumentationCoverageTest,ArchitectureModuleCatalogTest" test
```

Expected: FAIL because the capability pages and inventory fields do not exist.

- [x] **Step 3: Publish the two routes and capability ownership**

Map modules as follows:

| Capability | Modules |
|---|---|
| Identity and access | `security`, `identity`, `participation` |
| Marketplace | `classification`, `marketplace`, `governance` |
| Finance and payments | `financial` |
| Platform support | `common`, `audit`, `notification`, `documentation` |

Create the capability index and minimal module catalogue, link each module page
to its capability page, and ensure the module index still lists all 11 modules
individually. Delete `modules/inventory.json` after the test no longer consumes
it; any derived metrics used for diagnostics belong under `target/`.

- [x] **Step 4: Run navigation, structure, and link verification**

```powershell
.\mvnw.cmd -q "-Dtest=ArchitectureDocumentationCoverageTest,ArchitectureModuleCatalogTest,DocumentationStructureTest,DocumentationLinksTest" test
```

Expected: PASS.

- [x] **Step 5: Commit the independently reviewable navigation change**

```powershell
git add docs/README.md docs/architecture src/test/java/com/project/optrabidz/documentation
git commit -m "Clarify architecture reading paths (KAN-39)"
```

**Checkpoint:** The two reader routes and capability ownership are independently
reviewable before architecture assets change.

---

## Task 5D: Publish the Justified Architecture View Set

**Files:**

- Create: `docs/architecture/assets/system-context.svg` and
  `docs/architecture/assets/system-context.png`
- Create: `docs/architecture/assets/runtime-topology.svg` and
  `docs/architecture/assets/runtime-topology.png`
- Create: `docs/architecture/assets/complete-module-map.svg` and
  `docs/architecture/assets/complete-module-map.png`
- Create: `docs/architecture/assets/module-dependencies.svg` and
  `docs/architecture/assets/module-dependencies.png`
- Create: `docs/architecture/capabilities/assets/identity-access.svg` and
  `docs/architecture/capabilities/assets/identity-access.png`
- Create: `docs/architecture/capabilities/assets/marketplace.svg` and
  `docs/architecture/capabilities/assets/marketplace.png`
- Create: `docs/architecture/capabilities/assets/finance-payments.svg` and
  `docs/architecture/capabilities/assets/finance-payments.png`
- Create: `docs/architecture/flows/assets/request-security.svg` and
  `docs/architecture/flows/assets/request-security.png`
- Create: `docs/architecture/flows/assets/event-delivery.svg` and
  `docs/architecture/flows/assets/event-delivery.png`
- Modify: `docs/architecture/system-context.md`
- Modify: `docs/architecture/runtime.md`
- Modify: `docs/architecture/modules/README.md`
- Modify: `docs/architecture/module-dependencies.md`
- Modify: all four capability pages
- Modify: all three flow pages
- Create: `docs/architecture/diagram-publication/diagram-publications.json`
- Delete after migration: `docs/architecture/diagram-publication/inventory.json`
- Modify: `docs/architecture/diagram-publication.md`
- Modify: `docs/maintenance.md`
- Modify: `scripts/render-documentation-diagrams.mjs`
- Modify: `scripts/render-documentation-previews.mjs`
- Modify: `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidator.java`
- Modify: `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidatorTest.java`
- Modify: `src/test/java/com/project/optrabidz/documentation/ArchitectureDocumentationCoverageTest.java`
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`

**Interfaces:**

- A new figure exists only when it makes a specific relationship, hierarchy,
  sequence, state, or boundary materially clearer.
- Each publication declares neutral `svg` and `png` paths, one
  `primaryOwner`, and optional `consumers`.
- A canonical figure may be reused by consumers; duplicate drawings of the
  same question are rejected.
- Diagrams describe current source/configuration/test evidence and label known
  gaps separately.
- SVG is embedded on GitHub; its PNG is derived from the same SVG.

- [x] **Step 1: Write failing publication-catalogue tests**

Update validator fixtures to require the version-two neutral contract:

```json
{
  "id": "api-public-error-contract-flow",
  "sourceType": "CURATED_SVG",
  "source": "docs/api/assets/public-error-contract-flow.svg",
  "svg": "docs/api/assets/public-error-contract-flow.svg",
  "png": "docs/api/assets/public-error-contract-flow.png",
  "primaryOwner": "docs/api/errors.md",
  "consumers": ["docs/architecture/flows/error-disclosure.md"]
}
```

Reject legacy `githubSvg`, `jiraPng`, `jiraPngRequired`, `remediation`, and
renderer-version fields. Require the primary owner and every consumer to embed
the same SVG.

In `ArchitectureDocumentationCoverageTest`, require the approved architecture
questions by stable ID without requiring a unique figure for every page:

```java
private static final Set<String> REQUIRED_ARCHITECTURE_FIGURES = Set.of(
        "architecture-system-context",
        "architecture-runtime-topology",
        "architecture-module-capability-map",
        "architecture-module-dependencies",
        "architecture-identity-access",
        "architecture-marketplace",
        "architecture-finance-payments",
        "architecture-request-security",
        "architecture-event-delivery");
```

- [x] **Step 2: Run the focused tests and confirm the legacy contract fails**

```powershell
.\mvnw.cmd -q "-Dtest=DiagramPublicationValidatorTest,ArchitectureDocumentationCoverageTest" test
```

Expected: FAIL because the current catalogue uses the legacy schema.

- [x] **Step 3: Record the diagram disposition before drawing**

Use `module-catalog.json`, production imports, Spring configuration,
controllers, repositories, outbox processors, security adapters, and matching
tests to update the claims in every owner page. Record `IMPLEMENTED`,
`PARTIAL`, or `PLANNED` in `audit.md`; never draw a planned Kafka, Redis, JWT,
OAuth2, or real-payment component as current.

For every candidate, record its reader question, evidence, primary owner,
consumers, and `KEEP`, `REDESIGN`, `REUSE`, or `REMOVE` decision. A page does
not receive a diagram merely because it exists.

- [x] **Step 4: Create and publish the system-wide and capability figures**

Create the nine justified core figures: system context, runtime topology,
complete module/capability map, module dependencies, identity/access,
marketplace, finance/payments, request/security, and event delivery. Reuse the
existing public-error-contract figure on the error-disclosure page. Let the
platform-support page consume the module map or event-delivery figure where
relevant rather than drawing a generic duplicate.

Keep one reading direction, readable normal-width labels, explicit boundary
names, opaque backgrounds, and no decorative palette. Migrate every retained
entry to `diagram-publications.json`; package versions remain owned by
`package.json` and `package-lock.json`. The render scripts use the fixed
`diagram-publication/mermaid-config.json` path rather than duplicating renderer
configuration in the publication catalogue.

- [ ] **Step 5: Render and verify every architecture surface**

```powershell
npm run diagrams:render
npm run diagrams:preview
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=ArchitectureDocumentationCoverageTest,DiagramPublicationTest,DocumentationLinksTest" test
```

Review the 980-pixel and 390-pixel sheets before committing. Then confirm the
published pages on GitHub desktop and GitHub Mobile and the named PNGs in Jira.

- [x] **Step 6: Commit the architecture checkpoint**

```powershell
git add docs/architecture scripts src/test/java/com/project/optrabidz/documentation
git commit -m "Complete the architecture views (KAN-39)"
```

**Checkpoint:** The complete architecture view set is independently reviewable
before database pages change.

---

## Task 5E: Split Database Documentation into Focused Views

**Files:**

- Modify: `docs/database/README.md`
- Modify: `docs/database/relationship-journey.md`
- Create: `docs/database/views/README.md`
- Create: `docs/database/views/identity-access.md`
- Create: `docs/database/views/participant-profile.md`
- Create: `docs/database/views/marketplace-bidding.md`
- Create: `docs/database/views/agreement-acceptance.md`
- Create: `docs/database/views/settlement.md`
- Create: `docs/database/views/repayment-schedule.md`
- Create: `docs/database/views/payment-intent.md`
- Create: `docs/database/views/payment-processing.md`
- Create: `docs/database/views/payment-webhook.md`
- Create: `docs/database/views/notification-delivery.md`
- Create: `docs/database/views/outbox-audit.md`
- Create: `docs/database/reference/README.md`
- Create: `docs/database/assets/relationship-journey.svg`
- Create: `docs/database/assets/relationship-journey.png`
- Delete after migration: `docs/database/er-diagram.md`
- Modify: `docs/architecture/diagram-publication/diagram-publications.json`
- Modify: `src/test/java/com/project/optrabidz/documentation/DatabaseDocumentationNavigationTest.java`
- Modify: `src/test/java/com/project/optrabidz/documentation/DatabaseRelationshipDocumentationTest.java`

**Interfaces:**

- `README.md` routes by reviewer question and does not expose the raw manifest.
- The relationship journey provides orientation; each focused page owns one
  existing database figure and its exact relationship table.
- `reference/README.md` owns notation, verification method, material database
  invariants, and intentional non-FK correlation rules.

- [ ] **Step 1: Write failing split-navigation tests**

Require `views/README.md`, the 11 named focused pages, the relationship-journey
figure, and the absence of a `schema-manifest.json` link in `README.md`:

```java
assertThat(entryPoint)
        .contains("(relationship-journey.md)")
        .contains("(views/README.md)")
        .contains("(reference/README.md)")
        .doesNotContain("schema-manifest.json");
```

- [ ] **Step 2: Run the focused tests and confirm the red phase**

```powershell
.\mvnw.cmd -q "-Dtest=DatabaseDocumentationNavigationTest,DatabaseRelationshipDocumentationTest" test
```

Expected: FAIL because the focused-page hierarchy is not present.

- [ ] **Step 3: Split the monolithic ER page without changing schema claims**

Create these pages: `identity-access.md`, `participant-profile.md`,
`marketplace-bidding.md`, `agreement-acceptance.md`, `settlement.md`,
`repayment-schedule.md`, `payment-intent.md`, `payment-processing.md`,
`payment-webhook.md`, `notification-delivery.md`, and `outbox-audit.md`.
Move each existing figure and relationship table to exactly one owner page,
update inventory owners, and delete `er-diagram.md` only after all links move.

- [ ] **Step 4: Add orientation and reference material**

Publish a compact relational-journey SVG/PNG showing the six existing stages.
Create `reference/README.md` for FK notation, nullability, delete actions,
checks, partial indexes, triggers, and the six intentional correlations. Keep
the manifest file temporarily for tests but remove it from reader navigation.

- [ ] **Step 5: Verify navigation, publication, and mobile readability**

```powershell
npm run diagrams:render
npm run diagrams:preview
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=DatabaseDocumentationNavigationTest,DatabaseRelationshipDocumentationTest,DiagramPublicationTest,DocumentationLinksTest" test
```

Expected: PASS. Review the journey and every focused page at desktop and phone
width before committing.

- [ ] **Step 6: Commit the database reader-experience checkpoint**

```powershell
git add -A docs/database docs/architecture/diagram-publication src/test/java/com/project/optrabidz/documentation
git commit -m "Improve database documentation navigation (KAN-39)"
```

**Checkpoint:** The database navigation and focused-page split are independently
reviewable before schema-verification infrastructure changes.

---

## Task 5F: Replace the Transitional Schema Manifest

**Files:**

- Create: `src/test/java/com/project/optrabidz/documentation/database/DatabaseSchemaSnapshot.java`
- Create: `src/test/java/com/project/optrabidz/documentation/database/PostgresSchemaIntrospector.java`
- Create: `src/test/java/com/project/optrabidz/documentation/database/DatabaseDocumentationContractIT.java`
- Modify: `src/test/java/com/project/optrabidz/documentation/DatabaseDocumentationNavigationTest.java`
- Delete after parity passes: `src/test/java/com/project/optrabidz/documentation/DatabaseSchemaManifestTest.java`
- Delete after parity passes: `src/test/java/com/project/optrabidz/documentation/DatabaseRelationshipDocumentationTest.java`
- Delete after parity passes: `src/test/java/com/project/optrabidz/documentation/DatabaseDiagramCoverageTest.java`
- Delete after parity passes: `docs/database/schema-manifest.json`
- Modify: `docs/database/reference/README.md`

**Interfaces:**

- `PostgresSchemaIntrospector.read(Connection)` returns the effective schema
  after all Flyway migrations.
- `DatabaseDocumentationContractIT` runs under the existing
  `integration-tests` profile and compares that schema directly with human
  documentation.
- Diagnostic JSON is written only to
  `target/documentation-verification/schema-report.json`.

- [ ] **Step 1: Define the immutable schema model**

Create records for the facts currently represented by the manifest:

```java
public record DatabaseSchemaSnapshot(
        List<String> tables,
        List<ForeignKey> foreignKeys,
        List<NamedObject> uniqueConstraints,
        List<NamedObject> checkConstraints,
        List<NamedObject> partialIndexes,
        List<NamedObject> triggers) {

    public record ForeignKey(String name, String childTable,
            List<String> childColumns, String parentTable,
            List<String> parentColumns, boolean nullable, String onDelete) {}

    public record NamedObject(String name, String table, List<String> columns) {}
}
```

- [ ] **Step 2: Write a failing PostgreSQL catalogue parity test**

In `DatabaseDocumentationContractIT`, migrate a
`postgres:16-alpine` Testcontainer with Flyway, call the introspector, and
temporarily compare the result with `schema-manifest.json`. Assert the current
transition baseline of 35 tables, 46 foreign keys, 25 unique constraints, 57
check constraints, 19 partial indexes, and 12 triggers so deletion cannot occur
after a partial extraction.

- [ ] **Step 3: Implement catalogue introspection**

Read user tables and constraints from `pg_class`, `pg_namespace`,
`pg_constraint`, `pg_attribute`, `pg_index`, and `pg_trigger`. Use
`pg_get_constraintdef`, `pg_get_indexdef`, and `pg_get_triggerdef` for stable
definitions; exclude PostgreSQL internal objects and `flyway_schema_history`.
Map `confdeltype` to `NO ACTION`, `RESTRICT`, `CASCADE`, `SET NULL`, or
`SET DEFAULT`, preserving composite-column order with ordinality.

- [ ] **Step 4: Prove parity before removing the manifest**

```powershell
.\mvnw.cmd -q verify -Pintegration-tests "-Dit.test=DatabaseDocumentationContractIT"
```

Expected: PASS with exact parity for tables, foreign keys, unique/check
constraints, partial indexes, and triggers. If any category differs, keep the
manifest and correct the introspector; do not weaken the comparison.

- [ ] **Step 5: Move documentation checks onto the effective schema**

Make the integration test verify:

- every database table appears in the relational journey or a focused view;
- every foreign key has one exact relationship row with its name, columns,
  nullability, and delete action;
- checks, partial indexes, and triggers referenced by documentation exist; and
- each documented non-FK correlation references existing tables and columns
  while remaining visibly labelled as a correlation.

Write a deterministic diagnostic snapshot to `target/` for failed-build
inspection, never as an expected input.

- [ ] **Step 6: Remove the duplicate schema projection and obsolete parsers**

Delete the committed manifest and the three regex/manifest-dependent tests.
Keep the fast navigation-only assertions in
`DatabaseDocumentationNavigationTest`; effective-schema assertions belong to
the integration test.

- [ ] **Step 7: Run both fast and PostgreSQL verification**

```powershell
.\mvnw.cmd -q test
.\mvnw.cmd -q verify -Pintegration-tests
git ls-files | rg "schema-manifest.json|target/documentation-verification"
```

Expected: both Maven phases pass and the final command returns no tracked
manifest or generated diagnostic output.

- [ ] **Step 8: Commit the schema-verification checkpoint**

```powershell
git add -A docs/database src/test/java/com/project/optrabidz/documentation
git commit -m "Verify database docs from PostgreSQL (KAN-39)"
```

**Checkpoint:** Catalogue parity and manifest removal are independently
reviewable before the complete current-reality audit.

---

## Task 5G: Audit the Complete Current Documentation

**Files:**

- Modify only when evidence is stale: stable pages under `docs/` and their
  canonical diagram sources
- Modify: `docs/architecture/module-catalog.json`
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`
- Modify when required: documentation tests under
  `src/test/java/com/project/optrabidz/documentation/`

**Interfaces:**

- Stable pages describe current implementation, not planned production
  features.
- Every architecture/database figure is reachable from the two reader routes.
- Inventory facts, prose, diagrams, code, configuration, and tests agree.

- [ ] **Step 1: Re-scan production and test surfaces**

Regenerate or manually verify the module inventory against `src/main/java`,
`src/main/resources`, `src/test/java`, `pom.xml`, Docker configuration, and CI.
Check every durable claim about security, payments, outbox delivery,
notifications, audit, exception handling, persistence, and profiles.

- [ ] **Step 2: Classify and correct every mismatch**

Record each reviewed capability as `IMPLEMENTED`, `PARTIAL`, or `PLANNED` in
`audit.md`. Correct stale prose or diagrams; do not change production code or
silently promote a future component into the current architecture.

- [ ] **Step 3: Verify complete navigation and asset ownership**

```powershell
rg -n "schema-manifest|er-diagram\.md|\.mmd\)" README.md docs --glob "*.md"
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=ArchitectureDocumentationCoverageTest,ArchitectureModuleCatalogTest,DatabaseDocumentationNavigationTest,DocumentationStructureTest,DocumentationLinksTest,DiagramPublicationTest" test
```

Expected: no obsolete reader link; every remaining match is an intentional
historical statement inside the active KAN-39 record; all tests pass.

- [ ] **Step 4: Review every diagram surface as a set**

Generate the complete 980-pixel and 390-pixel review sheets. Verify GitHub
desktop, GitHub Mobile, Jira PNG, Confluence, and local light/dark surroundings.
Record each named figure and result rather than claiming one sample represents
the whole set.

- [ ] **Step 5: Commit the current-reality audit**

```powershell
git add docs src/test/java/com/project/optrabidz/documentation
git commit -m "Align documentation with the current system (KAN-39)"
```

Only after this checkpoint is reviewed may Task 6 begin.

---

## Task 6: Complete Repository and Surface Verification

**Files:**

- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`
- Modify only when verification finds a defect: affected stable docs, sources,
  render scripts, or test utilities

**Interfaces:**

- Verification evidence names the command, result, surface, and reviewed asset.
- Generated screenshots remain untracked.
- Jira receives concise evidence; the repository does not contain personal
  approval instructions or AI/tooling references.

- [ ] **Step 1: Run deterministic documentation checks**

```powershell
npm ci
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=DocumentationStructureValidatorTest,DocumentationStructureTest,DocumentationLinksTest,DocumentationLinkValidatorTest,DiagramPublicationValidatorTest,DiagramPublicationTest,ErrorCatalogueMarkdownSnapshotTest" test
git diff --check
```

Expected: all commands pass and `git diff --check` reports no whitespace error.

- [ ] **Step 2: Run the unmodified full test suite**

```powershell
.\mvnw.cmd test
```

Expected: PASS. If an unrelated pre-existing failure reproduces on `develop`,
record that evidence separately instead of weakening or excluding the test in
KAN-39.

- [ ] **Step 3: Verify repository hygiene**

```powershell
git status --short
git ls-files | rg "(^|/)(target|node_modules)/|clipboard-|hs_err_pid|replay_pid"
rg -n "C:\\Users\\|agentic|Codex|AI-generated" README.md docs .github --glob "*.md" --glob "!**/work-items/**"
```

Expected: no generated review directory, crash log, temporary clipboard file,
machine path, or authorship/process leakage is tracked or published.

- [ ] **Step 4: Complete final surface review**

Review the draft pull request on GitHub desktop, mobile web, and GitHub Mobile.
Open the named Jira PNG attachments. Confirm navigation from root README to
every stable topic, readable normal-width figures, correct light/dark
surroundings, and no raw Mermaid source presentation.

- [ ] **Step 5: Record evidence and prepare the reviewable branch**

Update `audit.md` and Jira with final counts, commands, and surface results.
Then:

```powershell
git add docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md
git commit -m "Record documentation verification (KAN-39)"
git push
```

Update the pull-request description with the stable hierarchy, removed-history
count, surviving-diagram count, commands, GitHub Mobile result, Jira preview
result, and rollback guidance. Leave the pull request unmerged for review.
