# KAN-39 — Documentation Experience and Diagram Publication

**Status:** Draft for renewed review

**Date:** 2026-08-27

**Jira:** [KAN-39](https://0707manna0895.atlassian.net/browse/KAN-39)

## 1. Why KAN-39 Was Reopened

The first KAN-39 delivery improved file pairing, generated SVG output, PNG
fallbacks, and automated publication checks. It did not solve the reader's
experience across the repository.

The repository still has three material problems:

1. direct `.mmd` links can open a renderer whose large canvas makes labels
   unreadable;
2. technically valid SVG files can still shrink below practical phone size;
3. stable engineering guidance is buried among historical work-item documents.

Passing a structural test is therefore not enough. The renewed KAN-39 scope
must make the complete documentation set navigable, visually consistent, and
readable on the surfaces where reviewers actually use it.

## 2. Outcomes

KAN-39 will deliver:

- a concise repository entry point;
- a stable topic-based documentation hierarchy;
- a restrained OptraBidz diagram language;
- reader-facing SVG diagrams without reader-facing Mermaid source links;
- deliberate desktop, mobile-web, GitHub Mobile, and Jira verification; and
- objective publication checks without pretending automation proves visual
  quality.

KAN-39 does not change production behavior, business rules, APIs, database
schemas, authentication, or authorization.

## 3. Documentation Information Architecture

The durable documentation hierarchy is:

```text
README.md
CONTRIBUTING.md
docs/
  README.md
  getting-started/
  architecture/
  api/
  database/
  security/
  operations/
  decisions/
```

The root README is a concise product and repository entry point. It explains
what OptraBidz does, states its boundaries, shows one architecture overview,
and routes readers to stable topic pages.

Durable technical decisions belong in the topic guides or in short decision
records. Completed implementation plans are not permanent product
documentation. Before obsolete work-item files are removed, any still-valid
decision, operational instruction, or contract is distilled into the durable
hierarchy. Jira, pull requests, commits, and Git history preserve delivery
evidence.

No documentation-site framework is introduced in this phase. Plain Markdown
and repository assets remain the publication system.

## 4. Reader Surfaces

Every reader-facing diagram must be considered on all of these surfaces:

| Surface | Publication target |
|---|---|
| GitHub desktop website | Embedded SVG at normal README width |
| GitHub mobile website | The same embedded SVG at phone width |
| GitHub Mobile app | The same Markdown page, verified on a real device |
| Jira | Attached high-resolution opaque PNG |
| Local IDE or checkout | SVG, PNG fallback, and editable source |

GitHub documents support for repository images, including SVG, but do not
provide a contractual guarantee that every GitHub Mobile release behaves
identically to the website. A real GitHub Mobile check is therefore an
acceptance step, not an assumption.

## 5. Visual Policy — Clarity Over Branding

KAN-39 will not introduce a new diagram brand or redesign readable diagrams for
cosmetic uniformity. The accepted KAN-34 diagrams are the reference for clear
flow presentation: a light canvas, obvious reading direction, readable labels,
and restrained outcome colours.

Implementation may improve only the qualities that affect comprehension or
publication:

- remove excessive empty canvas;
- enlarge text that becomes unreadable at embedded desktop or phone width;
- simplify connector routing and eliminate avoidable crossings;
- group multi-stage flows clearly;
- split a dense diagram when one canvas cannot remain readable;
- retain explicit labels so meaning never depends on colour alone;
- use an opaque background and sufficient contrast; and
- keep typography, spacing, and node treatment internally consistent within a
  figure.

The KAN-34 appearance is a reference for flow diagrams, not a template forced
onto architecture maps, state diagrams, or ER views. Each diagram type keeps
the smallest notation that answers its reader's question.

Project-specific value comes from the documented architecture, trust
boundaries, state transitions, and engineering decisions. It does not require
decorative branding, gradients, stock icons, shadows, or a novel colour palette.
Any future repository-wide aesthetic change requires a separate visual review
and explicit approval.

## 6. Diagram Grammar

Different questions require different diagram types. One generic flowchart
template will not be stretched across the repository.

| Question | Diagram type | Layout rule |
|---|---|---|
| Who interacts with the system? | System context | Compact top-to-bottom boundary view |
| How is the monolith divided? | Module map | Stable grouped modules; dependencies shown selectively |
| How does one request execute? | Request flow | Numbered stages with a single reading direction |
| How does state change? | State transition | States first; guards and outcomes on labelled edges |
| How is data related? | ER view | One bounded data concern per figure |
| How is an error disclosed? | Boundary/error view | Trust boundary before lookup and disclosure outcome |

Large diagrams are split by reader question. An overview links to focused
details; it does not attempt to contain every class, table, adapter, and event.

## 7. Mobile Readability Contract

A diagram does not pass merely because SVG can be zoomed.

For the embedded, normal page-width view:

- overview figures target a view-box width around 720 units;
- primary labels must remain approximately 12 CSS pixels or larger at a
  390-pixel viewport;
- secondary labels must remain approximately 11 CSS pixels or larger;
- an overview contains no more than seven primary nodes;
- mobile readers must understand the main relationship without pinch-zoom;
- dense details move to a separate focused figure; and
- horizontal scrolling is not the normal reading path.

The exact viewport result is checked from the rendered page. Source font sizes
alone are not accepted as proof.

## 8. Source and Output Strategy

There are two supported source paths.

### 8.1 Constrained Mermaid source

Mermaid remains appropriate for simple request flows and state transitions
when its automatic layout produces a compact result. The repository retains
the `.mmd` file for maintainers, then deterministically generates SVG and PNG.

The `.mmd` file is not linked as the reader-facing diagram.

### 8.2 Curated SVG source

Stable architecture overviews and other composition-sensitive diagrams may use
a curated SVG as the editable source. This is preferable to forcing a poor
automatic layout merely to keep every diagram in Mermaid.

A curated SVG must:

- use named classes and shared visual tokens;
- contain a title and description;
- avoid scripts, external resources, and `foreignObject`;
- use meaningful groups and comments so it remains maintainable; and
- generate its Jira PNG from the same SVG.

The diagram inventory records which source path each figure uses. A diagram
must never have two competing canonical sources, as happened when an
architecture `.mmd` file and a separately authored overview SVG represented
the same figure.

## 9. Reader-Facing Publication Contract

Every stable Markdown page follows this pattern:

1. a short paragraph explains what question the diagram answers;
2. the page embeds the SVG using a relative repository path;
3. meaningful alt text summarizes the relationship;
4. a clearly labelled high-resolution PNG link is available as fallback; and
5. a short textual interpretation follows the figure.

Reader pages do not contain Mermaid code fences and do not direct readers to
`.mmd` files. Source files remain discoverable through the maintenance guide
and inventory.

## 10. Representative Prototype Gate

The architecture overview is the compatibility fixture for the new system.
Before repository-wide conversion it will be redesigned as:

- a compact system overview containing clients, application boundary,
  business core, durable data, and asynchronous delivery;
- focused module and event-delivery diagrams linked from the overview rather
  than compressed inside it; and
- an adjacent textual explanation.

The prototype must be reviewed on:

- GitHub desktop web;
- GitHub mobile web at approximately 390 pixels;
- the GitHub Mobile app on the user's phone;
- Jira PNG preview; and
- local light and dark surroundings.

Bulk diagram conversion cannot start until this prototype is approved.

## 11. Verification

Automated checks will verify objective properties:

- every local image reference resolves;
- each inventory entry has exactly one canonical source;
- reader-facing Markdown does not link `.mmd` files;
- SVG files contain a `viewBox`, title, description, and explicit background;
- SVG files contain no unsafe or externally loaded content;
- generated PNG files are opaque and meet the export dimensions;
- diagrams use the approved token set; and
- temporary clipboard assets are absent.

Visual evidence will verify what automation cannot:

- normal-width label readability;
- connector clarity and reading order;
- useful whitespace rather than empty canvas;
- light/dark surrounding contrast;
- GitHub Mobile rendering; and
- Jira preview quality.

The implementation will generate a compact review sheet showing representative
desktop, phone-width, and Jira outputs. A human reviewer approves that evidence.

## 12. Delivery Sequence

1. Discuss and approve the visual direction and this renewed design.
2. Write and approve the implementation plan.
3. Redesign the architecture compatibility fixture.
4. Review the fixture on every required surface.
5. Distil durable documentation into the approved hierarchy.
6. Convert, split, or retire the remaining diagrams and historical documents.
7. Run structural checks and produce visual evidence.
8. Open a reviewable pull request with documentation, evidence, and Jira
   updates.

## 13. Acceptance Criteria

KAN-39 is complete when:

- the stable documentation hierarchy is complete and navigable;
- obsolete work-item documents have been distilled and removed where safe;
- every reader-facing diagram follows the visual notation approved during the
  prototype review;
- every diagram has exactly one canonical source;
- no reader-facing page links Mermaid source;
- the architecture prototype and converted figures pass desktop, mobile web,
  GitHub Mobile, Jira, and local review;
- automated documentation checks pass;
- no production behavior changed; and
- Jira and the pull request contain concise verification evidence.
