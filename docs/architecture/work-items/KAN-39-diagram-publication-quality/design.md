# KAN-39 — Reviewer-Quality Diagram Publication

**Status:** Written specification approved

**Date:** 2026-08-27

**Jira:** [KAN-39](https://0707manna0895.atlassian.net/browse/KAN-39)

## 1. Problem

Repository diagrams do not currently follow one publication standard. Several
Mermaid diagrams are embedded as low-resolution or transparent PNG files, some
complex flows are compressed into unreadable layouts, and only part of the
diagram inventory has scalable SVG output. Existing documentation tests prove
that links resolve, but they do not protect source/output pairing, contrast,
Jira export quality, or realistic desktop and mobile readability.

This is a documentation-quality defect. It does not change application
behavior or architecture.

## 2. Decision

Use a controlled dual-format publication model:

1. an editable source remains the canonical diagram definition;
2. GitHub documentation embeds an opaque, scalable SVG;
3. a high-resolution, opaque PNG is retained for Jira and offline review;
4. dense diagrams are simplified, reoriented, or split instead of relying on
   resolution alone; and
5. automated structural checks and a manual visual review jointly form the
   quality gate.

Native Mermaid Markdown rendering is not the publication target because Jira
support and renderer behavior are inconsistent. Hand-authored SVG-only assets
are also rejected because they are expensive to maintain and can drift from
their editable source.

## 3. Publication Contract

| Concern | Contract |
|---|---|
| Editable source | Mermaid-backed diagrams retain a colocated `.mmd` source. Existing architecture or ER source conventions remain valid when documented. |
| GitHub asset | Markdown embeds an SVG with a `viewBox`, explicit neutral background, readable labels, and no unsafe external content. |
| Jira asset | The same source produces a high-resolution PNG with an explicit opaque background. |
| Placement | Work-item assets remain in that work item's `assets/` directory. Stable architecture and database assets remain in their existing topic asset directories. |
| Traceability | Source, SVG, and Jira PNG use the same descriptive base filename where the source is Mermaid. |
| Complexity | A diagram is split or redesigned when normal page-width rendering makes its labels or connectors unreadable. |
| Existing assets | Compliant architecture and database SVGs are verified and retained rather than rewritten for cosmetic uniformity. |

Vector output is necessary but not sufficient. An oversized SVG that becomes
unreadable at normal page width still fails this contract.

## 4. Repository Audit Baseline

The initial audit found:

- 17 Mermaid-backed PNG diagrams under error-handling work-item records;
- only six of those diagrams also have matching SVG output;
- 11 are currently PNG-only;
- five PNG assets use transparency, reducing connector contrast in dark mode;
- multiple tall or dense diagrams scale labels below practical review size;
- architecture and database SVGs are generally scalable and readable; and
- existing tests validate links but do not enforce diagram publication
  quality.

The audit covers every diagram referenced by repository documentation, not
only error-handling diagrams.

## 5. Remediation Classification

Every referenced diagram receives exactly one classification:

| Classification | Meaning | Action |
|---|---|---|
| Pass | Scalable, opaque, readable, safe, and correctly linked | Retain and record verification |
| Regenerate | Layout is acceptable but required source/output or background properties are missing | Generate compliant SVG and PNG outputs and update the embed |
| Redesign | Clipping, weak contrast, excessive whitespace, or branching harms comprehension | Change orientation, grouping, labels, or flow before rendering |
| Split | One canvas combines independent flows that cannot remain readable at normal width | Publish smaller diagrams with focused responsibilities |

The audit is expected to retain most architecture and database SVGs, regenerate
several straightforward historical flows, and redesign or split the densest
payment, webhook, and settlement diagrams. Final classification is based on
rendered evidence rather than filename or format alone.

## 6. Automated Validation

A documentation-focused test will inventory referenced diagram assets and
enforce objective rules:

- every local diagram reference resolves;
- a Mermaid-backed embedded diagram uses SVG;
- required `.mmd`, `.svg`, and Jira `.png` siblings exist and share a base
  filename;
- SVG output contains a `viewBox` and an explicit background;
- SVG output does not contain scripts, external resource references, or
  `foreignObject` content;
- Jira PNG output is opaque and meets the documented minimum resolution;
- temporary clipboard assets are not published; and
- the repository documentation policy describes the same rules.

Existing link-validation tests remain responsible for general Markdown links.
The new checks complement them rather than duplicate their purpose.

Automated limits may identify suspicious aspect ratios or dimensions, but they
must not pretend to prove human readability. Layout quality remains a visual
review responsibility.

## 7. Manual Visual Review

Representative output from every remediation class will be reviewed at:

- normal GitHub desktop width;
- mobile-width GitHub rendering;
- light and dark surrounding themes; and
- Jira PNG preview size.

The reviewer confirms that labels can be read without browser zoom, connectors
remain visible, branch labels do not overlap or clip, and each diagram has a
clear reading direction. Audit evidence records the checked asset and result.

## 8. Documentation Changes

Implementation will:

- update active Markdown embeds from PNG to SVG where required;
- keep a clearly labelled PNG link for Jira and offline review;
- revise the earlier diagram-publication guidance introduced by KAN-25;
- add KAN-39 to the architecture work-item index; and
- avoid rewriting historical prose or implementation commands unless they
  conflict with the current publication contract.

The repository remains the durable engineering record. Jira receives concise
progress, classification, verification, and delivery updates without
duplicating the complete specification.

## 9. Scope Boundaries

KAN-39 includes the repository-wide inventory, diagram remediation, publication
policy, test-only safeguards, and visual-review evidence.

KAN-39 excludes:

- production Java or runtime behavior changes;
- business rules, API contracts, authentication, or authorization changes;
- unrelated documentation restructuring;
- cosmetic rewriting of diagrams that already pass; and
- changes to application dependencies unless a build-time rendering tool is
  separately justified by the implementation plan.

## 10. Delivery and Rollback

Work is isolated on the `docs/KAN-39-diagram-quality` branch and targets
`develop`. The pull request must present the audit classification, generated
asset changes, automated test results, and manual visual evidence.

Because the change is documentation and test focused, rollback uses normal Git
reversion. Editable sources remain available, so any individual rendered asset
can also be regenerated or corrected without reconstructing the design.

## 11. Acceptance Criteria

KAN-39 is complete when:

- every referenced repository diagram has an audit result;
- GitHub embeds use compliant, readable SVG assets;
- Mermaid-backed diagrams retain matching editable sources and Jira PNGs;
- dense diagrams have been simplified or split where necessary;
- automated publication checks and existing documentation-link tests pass;
- desktop, mobile, theme, and Jira evidence demonstrates practical
  readability; and
- Jira and repository documentation contain the final verification summary.
