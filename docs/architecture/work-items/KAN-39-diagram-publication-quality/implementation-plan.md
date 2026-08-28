# KAN-39 Documentation Experience Implementation Plan

**Status:** Awaiting review

**Goal:** Replace the noisy historical documentation tree with concise current
guidance and publish every surviving diagram as a readable SVG with a verified
PNG fallback.

**Architecture:** Restructure documentation before remediating diagrams so the
team does not polish assets whose historical owners will be removed. A compact
architecture prototype proves the publication contract on GitHub desktop,
mobile web, GitHub Mobile, and Jira before the remaining content is migrated.
Repository tests enforce navigation, canonical diagram sources, safe SVGs,
opaque PNGs, and the absence of reader-facing Mermaid-source links.

**Tech stack:** Markdown, SVG, PNG, Node.js, Mermaid CLI 11.16.0, Sharp 0.35.4,
Java 21, Jackson, ImageIO, JUnit 5, AssertJ, Maven Surefire

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
| `docs/api/README.md` | API boundary, documentation profile, response, and versioning guidance |
| `docs/api/errors.md` | Current neutral exception and RFC 9457 contract |
| `docs/api/error-catalogue.md` | Generated public error catalogue moved from `docs/error-handling/` |
| `docs/database/README.md` | Database navigation and schema ownership |
| `docs/database/migrations.md` | Flyway authoring and recovery policy |
| `docs/database/er-diagram.md` | Context-sized ER diagrams |
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
| `docs/architecture/diagram-publication/inventory.json` | Surviving reader-facing diagram inventory only |
| `docs/architecture/diagram-publication/mermaid-config.json` | Pinned safe Mermaid configuration |
| `scripts/render-documentation-diagrams.mjs` | Render Mermaid SVG and derive PNG from either source type |
| `scripts/render-documentation-previews.mjs` | Generate untracked desktop and phone review sheets |
| `docs/architecture/assets/optrabidz-system-overview.svg` | Compact curated architecture source and GitHub image |
| `docs/architecture/assets/optrabidz-system-overview-jira.png` | Jira fallback derived from the SVG with a cache-safe publication identity |
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
| `src/test/java/com/project/optrabidz/documentation/error/ErrorCatalogueMarkdownSnapshotTest.java` | Track the moved public catalogue path |
| `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md` | Record content disposition and review results during delivery |

---

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
- Create: `docs/architecture/assets/optrabidz-system-overview-jira.png`
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

Embed the SVG, link the PNG as `High-resolution PNG for Jira and offline
review`, add a short textual interpretation, and remove the reader-facing
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

- [ ] **Step 1: Establish the code-to-documentation truth baseline**

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

- [ ] **Step 2: Verify every work-item file has a disposition**

Run a PowerShell comparison between `rg --files docs | rg 'work-items'` and the
audit table. Expected: zero unclassified files. Record counts for each
disposition in `audit.md`.

- [ ] **Step 3: Migrate approved durable diagrams and explanations**

Move each `MIGRATE_DIAGRAM` source and output to its stable topic `assets/`
directory, rename it for the reader question rather than its Jira key, and
update the inventory owner. Preserve KAN-34-style flow appearance where the
source is a flow diagram.

- [ ] **Step 4: Remove distilled historical records and orphan assets**

Delete only `DISTILL_REMOVE` and completed `MIGRATE_*` files. Do not remove the
active KAN-39 record. Use `rg` before each directory removal to prove no stable
Markdown link still targets it.

- [ ] **Step 5: Prove no stable page depends on work-item history**

```powershell
rg -n "work-items/|\.mmd(?:[)#?]|$)|```mermaid" README.md docs --glob "*.md"
.\mvnw.cmd -q "-Dtest=DocumentationStructureTest,DocumentationLinksTest,DocumentationLinkValidatorTest" test
```

Expected: `rg` returns only links inside the active KAN-39 record; tests pass.

- [ ] **Step 6: Commit the historical cleanup**

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

- [ ] **Step 1: Make the inventory represent only stable reader questions**

Remove entries whose owner was deleted. Update migrated owner/source/output
paths. Reject duplicate IDs, duplicate published SVGs, and output paths outside
the repository in validator fixtures.

- [ ] **Step 2: Regenerate straightforward Mermaid diagrams**

Render and preview the complete stable inventory; the renderer itself selects
Mermaid only for `MERMAID_FILE` entries:

```powershell
npm run diagrams:render
npm run diagrams:preview
```

Preserve existing meaning and accepted appearance. Change layout only when the
desktop or phone preview demonstrates clipping, overlap, unreadable labels,
excessive empty canvas, or confusing routing.

- [ ] **Step 3: Derive fallbacks from curated SVG sources**

For each `CURATED_SVG`, use Sharp to flatten the same SVG onto an opaque
background and write a 2400-pixel-wide PNG. Never maintain a separately drawn
PNG.

- [ ] **Step 4: Complete manual visual evidence**

For every surviving diagram, record desktop, 390-pixel preview, dark
surrounding contrast, and Jira PNG results. The architecture fixture already
has a separate GitHub Mobile result; repeat device review for any figure whose
layout materially differs.

- [ ] **Step 5: Run publication tests and commit**

```powershell
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=DiagramPublicationValidatorTest,DiagramPublicationTest,DocumentationStructureTest,DocumentationLinksTest,DocumentationLinkValidatorTest" test
git add docs scripts package.json package-lock.json src/test/java/com/project/optrabidz/documentation
git commit -m "Publish the stable diagram set (KAN-39)"
```

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
