# KAN-39 Reviewer-Quality Diagram Publication Implementation Plan

**Goal:** Make every diagram referenced by repository documentation readable,
scalable, reproducible, safe to embed on GitHub, and available as an opaque
high-resolution PNG when Jira compatibility requires it.

**Architecture:** A versioned diagram inventory connects each owner document to
its editable source, GitHub SVG, and Jira PNG. A pinned development-only Node
renderer produces consistent assets, while Java test utilities enforce the
publication contract without adding anything to the production runtime.
Readability is completed by focused redesign and recorded desktop/mobile visual
review rather than treating file format as proof of quality.

**Tech stack:** Mermaid CLI 11.16.0, Node.js, Sharp 0.35.4, SVG, PNG, Java 21,
Jackson, ImageIO, JUnit 5, AssertJ, Maven Surefire

**Spec:** [KAN-39 design](design.md)

## Global constraints

- Preserve application behavior, APIs, security, business rules, database
  state, dependencies, and production runtime configuration.
- Keep editable sources canonical; never repair only a generated image.
- Embed opaque SVG on GitHub and publish an opaque high-resolution PNG for
  Jira/offline review where the inventory requires it.
- Use one pinned renderer configuration with root-level `htmlLabels: false`,
  because diagram-specific `flowchart.htmlLabels` is deprecated and did not
  remove `foreignObject` labels in the current outputs.
- Reject scripts, external resources, and `foreignObject` elements from
  published SVGs.
- Retain compliant hand-authored architecture and ER SVGs; do not mechanically
  replace them with lower-quality Mermaid output.
- Split or redesign a dense diagram when normal page-width rendering remains
  unreadable after regeneration.
- Keep work-item assets with their owner and do not add clipboard, temporary,
  machine-specific, or generated audit screenshots to version control.
- Write short imperative commit subjects in plain language and place the Jira
  key at the end; avoid mechanical type/scope prefixes in this delivery.
- Use test-first RED/GREEN checkpoints and one reviewable commit per task.

---

## File map

### New publication infrastructure

| File | Responsibility |
|---|---|
| `package.json` | Pin the documentation-only renderer and expose deterministic render/check commands |
| `package-lock.json` | Lock the complete documentation rendering toolchain |
| `scripts/render-documentation-diagrams.mjs` | Read the inventory, render selected Mermaid SVGs, and flatten every required Jira PNG onto white |
| `docs/architecture/diagram-publication/mermaid-config.json` | Hold the shared safe theme, root-level plain-text labels, spacing, font, and security settings |
| `docs/architecture/diagram-publication/inventory.json` | Map stable diagram IDs to owner, source, SVG, PNG, source type, and remediation action |
| `docs/architecture/diagram-publication.md` | Explain the durable source/render/review workflow |
| `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md` | Record all initial classifications and final human readability results |

### New test utilities

| File | Responsibility |
|---|---|
| `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidator.java` | Parse the inventory and return deterministic structural violations |
| `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidatorTest.java` | Prove manifest, SVG-safety, PNG, embed, path, and temporary-file rules with isolated fixtures |
| `src/test/java/com/project/optrabidz/documentation/DiagramPublicationTest.java` | Apply the validator to the checked-in repository after remediation |

### Existing documentation to modify

| File group | Change |
|---|---|
| `.gitignore` | Ignore `node_modules/` without weakening existing rules |
| `docs/README.md` | Link the stable diagram-publication reference and KAN-39 record |
| `docs/architecture/README.md` | Link the stable policy and completed implementation plan |
| `docs/architecture/work-items/KAN-25-documentation-information-architecture/design.md` | Supersede the earlier PNG/SVG preference with the KAN-39 publication contract |
| `docs/architecture/overview.mmd` and `docs/architecture/assets/` | Preserve the accepted overview layout and add its Jira PNG |
| `docs/database/er-diagram.md` and `docs/database/assets/` | Preserve the accepted ER SVGs and add labelled Jira PNG links |
| KAN-24, KAN-29, KAN-30, KAN-31, KAN-32, KAN-33, KAN-34, KAN-35, KAN-36, KAN-37, KAN-42, and KAN-43 design/assets | Publish the remediated SVG/PNG pairs and update active embeds |

## Inventory and remediation map

| Owner | Diagram(s) | Planned action |
|---|---|---|
| Architecture overview | `optrabidz-architecture-overview` | Pass layout; add PNG and inventory evidence |
| Database ER reference | 11 context SVGs | Pass layouts; add opaque PNG companions and evidence |
| KAN-24 | `architecture`, `login-flow` | Regenerate with shared configuration |
| KAN-29 | `error-flow` | Regenerate with shared configuration |
| KAN-30 | `financial-error-flow` | Split into authenticated request and provider webhook flows |
| KAN-31 | `authentication-flow` | Redesign wide left-to-right canvas into a mobile-safe top-to-bottom flow |
| KAN-32 | `webhook-replay-flow` | Split ingress/claim from duplicate/collision outcomes |
| KAN-33 | `single-error-contract`, `legacy-deletion-boundary` | Preserve layout; regenerate without HTML labels |
| KAN-34 | `repayment-error-boundary`, `repayment-transition-state` | Preserve the accepted layout; regenerate without HTML labels |
| KAN-35 | `payment-error-boundary`, `payment-state-errors` | Redesign spacing and branches; remove transparency |
| KAN-36 | `webhook-ingress-flow` | Regenerate with shared configuration |
| KAN-37 | `settlement-error-boundary`, `settlement-confirmation-state` | Redesign clipped/overlapping branches and remove transparency |
| KAN-42 | `real-http-boundary` | Preserve layout; regenerate without HTML labels |
| KAN-43 | `error-contract-publication` | Reorient the wide publication pipeline for normal/mobile page widths |

---

## Task 1: Establish the pinned renderer and fixture-tested validator

**Files:**

- Modify: `.gitignore`
- Create: `package.json`
- Create: `package-lock.json`
- Create: `scripts/render-documentation-diagrams.mjs`
- Create: `docs/architecture/diagram-publication/mermaid-config.json`
- Create: `docs/architecture/diagram-publication/inventory.json`
- Create: `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidator.java`
- Create: `src/test/java/com/project/optrabidz/documentation/DiagramPublicationValidatorTest.java`

**Interfaces:**

- `DiagramPublicationValidator.findViolations(Path repositoryRoot)` returns an
  ordered `List<Violation>` for inventory and published-asset defects.
- `Violation` contains normalized `diagramId`, `path`, and `reason` strings.
- `npm run diagrams:render -- --id <diagram-id>` renders one inventory entry;
  without `--id`, it renders all entries whose source type is `MERMAID_FILE`.
- `npm run diagrams:check` runs the renderer in validation-only mode and must
  not modify files.

- [ ] **Step 1: Write failing validator fixture tests**

Create tests that build repositories beneath `@TempDir` and require these exact
violations:

```java
assertThat(DiagramPublicationValidator.findViolations(repository))
        .extracting(DiagramPublicationValidator.Violation::reason)
        .contains(
                "owner document does not embed the declared SVG",
                "SVG is missing viewBox",
                "SVG is missing an explicit background",
                "SVG contains forbidden foreignObject",
                "SVG contains forbidden script",
                "PNG contains transparent pixels",
                "PNG width must be at least 2000 pixels",
                "temporary clipboard asset is published"
        );
```

Add passing fixtures for an internal-only SVG, an opaque 2400-pixel PNG, an
HTML `<img>` embed, a Markdown image embed, a documented source path that is not
the SVG's sibling, and an entry with `jiraPngRequired: false`.

- [ ] **Step 2: Run the fixture tests and confirm RED**

Run:

```powershell
.\mvnw.cmd -q "-Dtest=DiagramPublicationValidatorTest" test
```

Expected: compilation fails because `DiagramPublicationValidator` does not
exist.

- [ ] **Step 3: Implement the minimal secure validator**

Use Jackson to read this schema:

```java
record Inventory(int schemaVersion, Renderer renderer,
                 List<DiagramEntry> diagrams) {}

record Renderer(String packageName, String version, String config) {}

record DiagramEntry(String id, String owner, String source,
                    String sourceType, String githubSvg, String jiraPng,
                    boolean jiraPngRequired, String remediation) {}

record Violation(String diagramId, String path, String reason) {}
```

Resolve every path against the normalized repository root and reject escapes.
Parse SVG as XML with external entities, DTD loading, and XInclude disabled.
Require `viewBox`, an explicit white/neutral root or first background shape,
and no `script`, `foreignObject`, non-fragment `href`, or non-fragment
`xlink:href`. Use `ImageIO` to require PNG format, width at least 2000 pixels,
height at least 600 pixels, and fully opaque pixels. Scan owner Markdown for
the declared SVG and scan tracked documentation asset names for
`clipboard-*`/`codex-clipboard-*` patterns.

- [ ] **Step 4: Add the pinned rendering toolchain**

Create `package.json` with private documentation tooling only:

```json
{
  "name": "optrabidz-documentation-tooling",
  "private": true,
  "scripts": {
    "diagrams:render": "node scripts/render-documentation-diagrams.mjs",
    "diagrams:check": "node scripts/render-documentation-diagrams.mjs --check"
  },
  "devDependencies": {
    "@mermaid-js/mermaid-cli": "11.16.0",
    "sharp": "0.35.4"
  }
}
```

The renderer must invoke the local `mmdc` executable with `-b white` and the
shared configuration. Sharp then reads the SVG, flattens it onto `#FFFFFF`,
and writes a 2400-pixel-wide PNG without changing aspect ratio. `--check`
validates tool/config/inventory inputs and exits without writing.

The shared Mermaid configuration must set root-level `htmlLabels` to `false`,
`securityLevel` to `strict`, base theme colors with dark text/connectors on a
white background, Arial-compatible fonts at 22 px, linear curves, and
review-friendly node/rank spacing. Do not leave deprecated
`flowchart.htmlLabels` in the configuration.

- [ ] **Step 5: Generate and inspect the lock file**

Run:

```powershell
npm install --package-lock-only
npm ci
npm audit --omit=optional
```

Expected: `package-lock.json` pins Mermaid CLI 11.16.0 and Sharp 0.35.4;
installation succeeds; audit output is reviewed and any unresolved finding is
recorded before delivery.

- [ ] **Step 6: Run the fixture tests and renderer check**

Run:

```powershell
.\mvnw.cmd -q "-Dtest=DiagramPublicationValidatorTest" test
npm run diagrams:check
```

Expected: both commands pass; the repository-wide gate is intentionally added
only after all inventory entries are remediated.

- [ ] **Step 7: Commit the publication foundation**

```powershell
git add .gitignore package.json package-lock.json scripts docs/architecture/diagram-publication src/test/java/com/project/optrabidz/documentation
git commit -m "Add diagram publishing checks (KAN-39)"
```

---

## Task 2: Regenerate the straightforward historical flows

**Files:**

- Modify/create KAN-24 `architecture.*` and `login-flow.*` assets
- Modify/create KAN-29 `error-flow.*` assets
- Modify/create KAN-31 `authentication-flow.*` assets
- Modify/create KAN-36 `webhook-ingress-flow.*` assets
- Modify the four owning `design.md` files
- Modify: `docs/architecture/diagram-publication/inventory.json`
- Modify: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`

**Interfaces:** Each owner embeds its declared SVG, links its declared Jira
PNG, and links the editable `.mmd` source. Existing architectural meaning and
error contracts remain unchanged.

- [x] **Step 1: Add inventory entries and record the initial audit**

Assign stable IDs `kan-24-module-architecture`, `kan-24-login-flow`,
`kan-29-notification-error-flow`, `kan-31-authentication-flow`, and
`kan-36-webhook-ingress-flow`. Record the original dimensions, transparency,
embed format, `REGENERATE`/`REDESIGN` classification, and the observed review
defect in `audit.md`.

- [x] **Step 2: Add accessibility metadata and mobile-safe layout**

Add `accTitle` and `accDescr` to every source. Preserve the documented behavior
and error contracts. Start with topology-preserving regeneration; where the
required 390-pixel review still fails, use a narrower equivalent decision tree
or outcome summary. Change KAN-31 from its wide `LR` direction to a stacked
`TB` layout, using short labels and no multi-purpose node.

- [x] **Step 3: Render the five SVG/PNG pairs**

Run one pinned command per ID:

```powershell
npm run diagrams:render -- --id kan-24-module-architecture
npm run diagrams:render -- --id kan-24-login-flow
npm run diagrams:render -- --id kan-29-notification-error-flow
npm run diagrams:render -- --id kan-31-authentication-flow
npm run diagrams:render -- --id kan-36-webhook-ingress-flow
```

Expected: each ID has an opaque SVG with no `foreignObject` plus an opaque
2400-pixel-wide PNG.

- [x] **Step 4: Replace owner-document PNG embeds**

Use this publication pattern in every owner:

```html
<a href="assets/example.svg">
  <img src="assets/example.svg" alt="Specific diagram meaning">
</a>
```

Follow it with links labelled `Editable diagram source` and
`High-resolution PNG for Jira and offline review`.

- [x] **Step 5: Verify links and visual quality**

Run:

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationLinksTest,DocumentationLinkValidatorTest,DiagramPublicationValidatorTest" test
```

Open every SVG at normal desktop width and a 390-pixel viewport, then open its
PNG against a dark surrounding page. Record pass/fail for label readability,
connector contrast, clipping, overlap, and reading direction in `audit.md`.

- [ ] **Step 6: Commit the straightforward remediations**

```powershell
git add docs/error-handling/work-items/KAN-24-module-migration docs/error-handling/work-items/KAN-29-notification-error-migration docs/error-handling/work-items/KAN-31-financial-security-boundary docs/error-handling/work-items/KAN-36-secure-webhook-ingress docs/architecture/diagram-publication docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md
git commit -m "Refresh historical flow diagrams (KAN-39)"
```

---

## Task 3: Normalize the accepted Mermaid SVG layouts

**Files:**

- Modify KAN-33 `single-error-contract.*` and `legacy-deletion-boundary.*`
- Modify KAN-34 `repayment-error-boundary.*` and
  `repayment-transition-state.*`
- Modify KAN-42 `real-http-boundary.*`
- Modify their three owning designs
- Modify the inventory and audit record

**Interfaces:** Preserve the approved node/edge meaning and accepted KAN-34
layout. Replace only deprecated per-diagram label configuration, unsafe SVG
label output, and inconsistent PNG exports.

- [x] **Step 1: Add inventory/audit entries and accessibility metadata**

Use stable IDs based on the existing base filenames. Add `accTitle` and a safe,
specific `accDescr`; remove duplicated inline theme configuration so the shared
configuration is authoritative.

- [x] **Step 2: Render all five pairs with the pinned toolchain**

Run `npm run diagrams:render -- --id <id>` for each entry. Expected: the
accepted topology remains recognizable, SVG output contains no HTML label
elements, and Jira PNG output is opaque.

- [x] **Step 3: Verify output equivalence and readability**

Compare source node/edge meaning before and after rendering. Review both KAN-34
diagrams first as the accepted visual baseline, then KAN-33 and KAN-42 at
desktop/mobile widths. Update the audit table with exact results.

- [x] **Step 4: Run documentation tests and commit**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationLinksTest,DocumentationLinkValidatorTest,DiagramPublicationValidatorTest" test
git add docs/error-handling/work-items/KAN-33-legacy-exception-removal docs/error-handling/work-items/KAN-34-repayment-error-migration docs/error-handling/work-items/KAN-42-real-http-smoke docs/architecture/diagram-publication docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md
git commit -m "Standardize Mermaid diagram output (KAN-39)"
```

---

## Task 4: Split the combined KAN-30 financial canvas

**Files:**

- Delete: KAN-30 `assets/financial-error-flow.mmd`
- Delete: KAN-30 `assets/financial-error-flow.png`
- Create: KAN-30 `assets/financial-request-error-flow.{mmd,svg,png}`
- Create: KAN-30 `assets/financial-webhook-error-flow.{mmd,svg,png}`
- Modify: KAN-30 `design.md`
- Modify the inventory and audit record

**Interfaces:** The request diagram covers authenticated caller through
application decision, Problem Details, conditional transition, outbox, audit,
and success. The webhook diagram covers bounded bytes, verification, strict
parsing, persistent identity, duplicate acknowledgement, first-delivery
processing, and provider acknowledgement.

- [x] **Step 1: Record the original combined-canvas failure**

Capture the original aspect ratio and observed tiny-label/excess-whitespace
failure in the audit, then add the two replacement inventory entries with
`SPLIT` remediation.

- [x] **Step 2: Write two focused Mermaid sources**

Both sources use `TB`, `accTitle`, `accDescr`, short node labels, and at most
one responsibility per node. Preserve every original boundary; do not invent
new application behavior or combine the provider verifier with parsing.

- [x] **Step 3: Render and embed both diagram pairs**

Run the renderer for both IDs. Replace the single KAN-30 embed with two titled
subsections and the standard SVG/source/PNG link pattern. Remove the obsolete
combined source and PNG after no document references them.

- [x] **Step 4: Verify and commit**

Run the focused documentation tests, visually review both widths and Jira PNGs,
update the audit, then commit:

```powershell
git commit -m "Split the financial error flow (KAN-39)"
```

---

## Task 5: Split the KAN-32 replay-protection canvas

**Files:**

- Delete: KAN-32 `assets/webhook-replay-flow.{mmd,png}`
- Create: KAN-32 `assets/webhook-replay-ingress.{mmd,svg,png}`
- Create: KAN-32 `assets/webhook-replay-outcomes.{mmd,svg,png}`
- Modify: KAN-32 `design.md`
- Modify the inventory and audit record

**Interfaces:** `webhook-replay-ingress` ends at the atomic PostgreSQL claim.
`webhook-replay-outcomes` begins with the claim result and distinguishes first
delivery, identical duplicate, semantic collision, unexpected committed state,
rollback, acknowledgement, and post-commit audit behavior.

- [x] **Step 1: Add the split inventory and initial failure evidence**

Record the original 2446×4194 transparent output and classify it `SPLIT`.

- [x] **Step 2: Write and render the two bounded sources**

Use short labels and `TB`; keep signature verification before parsing and
fingerprinting, and preserve transactional/post-commit boundaries. Render both
SVG/PNG pairs using their stable IDs.

- [x] **Step 3: Replace the owner embed and retire obsolete assets**

Publish two clearly titled sections with standard links. Delete the combined
assets only after `rg -n "webhook-replay-flow" docs` reports no active owner
reference outside historical implementation commands being intentionally
updated.

- [x] **Step 4: Verify and commit**

Run focused documentation tests, complete the four-context visual review, and
commit:

```powershell
git commit -m "Split the webhook replay flow (KAN-39)"
```

---

## Task 6: Redesign the KAN-35 payment diagrams

**Files:**

- Modify KAN-35 `payment-error-boundary.{mmd,png}` and add matching SVG
- Modify KAN-35 `payment-state-errors.{mmd,png}` and add matching SVG
- Modify: KAN-35 `design.md`
- Modify the inventory and audit record

**Interfaces:** Preserve scoped lookup selection, disclosure-equivalent 404s,
state evaluation, conditional transitions, typed financial exceptions, and
idempotent/rollback distinctions.

- [x] **Step 1: Add RED audit evidence and inventory entries**

Record transparent-background and tall-layout failures and classify both
entries `REDESIGN`.

- [x] **Step 2: Simplify source layout without changing semantics**

Group request/scoped lookup/error rendering in the boundary diagram. Group
creation outcomes separately from confirmation/failure outcomes in the state
diagram. Use short edge labels and keep each canvas within one reading
direction.

- [x] **Step 3: Render, embed, review, and commit**

Generate both pairs, change embeds to SVG, add source/PNG links, run focused
tests, record desktop/mobile/theme/Jira results, and commit:

```powershell
git commit -m "Improve payment error diagrams (KAN-39)"
```

---

## Task 7: Redesign the KAN-37 settlement diagrams

**Files:**

- Modify KAN-37 `settlement-error-boundary.{mmd,png}` and add matching SVG
- Modify KAN-37 `settlement-confirmation-state.{mmd,png}` and add matching SVG
- Modify: KAN-37 `design.md`
- Modify the inventory and audit record

**Interfaces:** Preserve role-first lookup selection, disclosure-equivalent
not-found behavior, payable-state selection, conditional confirmation,
same-intent idempotency, competing-intent conflict, rollback, and absence of
duplicate effects.

- [x] **Step 1: Add RED audit evidence and inventory entries**

Record the transparent background, clipped/overlapping branch labels, and
current dimensions; classify both entries `REDESIGN`.

- [x] **Step 2: Rebuild the layouts around explicit stages**

Use one top-to-bottom stage per authorization, lookup, state, transition, and
effect boundary. Move long error codes into terminal nodes rather than edge
labels and keep branch labels to short state names.

- [x] **Step 3: Render, embed, review, and commit**

Generate both pairs, publish SVG/source/PNG links, run focused tests, record
all visual checks, and commit:

```powershell
git commit -m "Improve settlement error diagrams (KAN-39)"
```

---

## Task 8: Reorient the KAN-43 publication architecture

**Files:**

- Modify KAN-43 `error-contract-publication.{mmd,svg,png}`
- Modify: KAN-43 `design.md`
- Modify the inventory and audit record

**Interfaces:** Preserve module/framework/security sources, normalization,
conflict detection, fail-closed exposure, OpenAPI publication, Markdown
publication, and parity/inventory/disclosure verification.

- [x] **Step 1: Record the wide-layout defect and inventory entry**

Classify the existing wide SVG as `REDESIGN`; vector format does not override
normal/mobile page-width readability.

- [x] **Step 2: Change to a layered top-to-bottom publication flow**

Place owned sources, adapter, policy, outputs, and tests in five readable
stages. Keep OpenAPI and Markdown as sibling outputs without creating a wide
full-page row.

- [x] **Step 3: Render, verify, and commit**

Generate the pair, update the standard links, run focused tests, record visual
results, and commit:

```powershell
git commit -m "Simplify the error catalogue diagram (KAN-39)"
```

---

## Task 9: Complete stable architecture and database Jira exports

**Files:**

- Modify: `docs/architecture/README.md`
- Create: `docs/architecture/assets/optrabidz-architecture-overview.png`
- Modify: `docs/database/er-diagram.md`
- Create: 11 PNG files matching the existing database SVG base filenames
- Modify the inventory and audit record

**Interfaces:** Existing hand-authored SVGs remain the GitHub publication and
source of truth for raster export. The database's editable Mermaid reference
remains `docs/database/er-diagram-source.md`; no schema relationship changes.

- [x] **Step 1: Inventory and structurally verify the accepted SVGs**

Add one architecture and 11 database entries with source type
`HAND_AUTHORED_SVG`. Require a `viewBox`, title, description, opaque
background, internal-only references, and a valid owner embed. Record layout
classification `PASS` before generating companions.

- [x] **Step 2: Generate opaque high-resolution PNG companions**

Use Sharp through the inventory renderer to flatten each SVG on white and
write a 2400-pixel-wide PNG. Do not regenerate or reformat the accepted SVGs.

- [x] **Step 3: Add concise PNG links and complete visual review**

Add a labelled Jira/offline PNG link below the architecture diagram and each
ER diagram without duplicating the SVG embed. Review representative narrow,
wide, and dense ER contexts at desktop/mobile widths plus every generated PNG
for clipping and contrast.

- [x] **Step 4: Verify and commit**

```powershell
.\mvnw.cmd -q "-Dtest=DocumentationLinksTest,DocumentationLinkValidatorTest,DiagramPublicationValidatorTest" test
git commit -m "Add PNG copies of stable diagrams (KAN-39)"
```

---

## Task 10: Activate the repository quality gate and publication reference

**Files:**

- Create: `src/test/java/com/project/optrabidz/documentation/DiagramPublicationTest.java`
- Create: `docs/architecture/diagram-publication.md`
- Finalize: `docs/architecture/work-items/KAN-39-diagram-publication-quality/audit.md`
- Modify: `docs/architecture/work-items/KAN-25-documentation-information-architecture/design.md`
- Modify: `docs/architecture/README.md`
- Modify: `docs/README.md`
- Modify: KAN-39 `design.md` and `implementation-plan.md` status/evidence only

**Interfaces:** The repository test calls
`DiagramPublicationValidator.findViolations(Path.of("").toAbsolutePath())` and
requires an empty result. The stable reference owns renderer commands,
publication rules, troubleshooting, and the human checklist.

- [ ] **Step 1: Write the failing repository-wide test**

```java
package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DiagramPublicationTest {
    @Test
    void publishedDiagramsMeetTheRepositoryContract() throws Exception {
        Path repository = Path.of("").toAbsolutePath().normalize();

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .as("diagram publication violations")
                .isEmpty();
    }
}
```

- [ ] **Step 2: Run the gate and correct every reported structural defect**

```powershell
.\mvnw.cmd -q "-Dtest=DiagramPublicationTest" test
```

Expected RED until every manifest entry, embed, SVG, PNG, and temporary-name
rule is satisfied; then GREEN without allowlisting a known defect.

- [ ] **Step 3: Publish the stable operating reference**

Document `npm ci`, inventory selection, `npm run diagrams:render`, the pinned
versions, SVG/PNG roles, accessibility metadata, minimum dimensions, source
ownership, visual checklist, and how to classify pass/regenerate/redesign/split.
State that KAN-39 supersedes only KAN-25's earlier delivery-format preference;
the rest of KAN-25's information architecture remains valid.

- [ ] **Step 4: Complete the audit and navigation**

Every inventory ID must have initial defect/classification, final asset paths,
desktop result, mobile result, contrast result, Jira result, and disposition.
Link the stable policy and KAN-39 record from both architecture navigation and
the documentation portal.

- [ ] **Step 5: Run focused documentation verification**

```powershell
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=DiagramPublicationValidatorTest,DiagramPublicationTest,DocumentationLinkValidatorTest,DocumentationLinksTest" test
git diff --check
```

Expected: every command passes and no generated asset differs from its declared
source/inventory contract.

- [ ] **Step 6: Run full project verification**

```powershell
.\mvnw.cmd -B clean test
.\mvnw.cmd -B -Pintegration-tests verify
```

Expected: complete unit and PostgreSQL integration suites pass with Docker
available. No production source file or runtime dependency appears in the
KAN-39 diff.

- [ ] **Step 7: Review repository and secret hygiene**

```powershell
git status --short
git diff --check
git diff --name-only develop...HEAD
git ls-files | rg "(^|/)(node_modules|target)/|clipboard|\.env$|hs_err_pid|replay_pid"
```

Expected: only KAN-39 documentation tooling, tests, documentation, and diagram
assets are present; the final scan returns no forbidden tracked artifact.

- [ ] **Step 8: Commit final governance and verification evidence**

```powershell
git add docs src/test package.json package-lock.json scripts .gitignore
git commit -m "Finish diagram quality checks (KAN-39)"
```

The pull request summary must report the inventory count, remediation counts,
focused/full test results, representative desktop/mobile evidence, Jira export
evidence, and confirmation that production behavior is unchanged.
