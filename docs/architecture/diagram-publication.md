# Diagram Publication

This reference defines how repository diagrams are stored, rendered, reviewed,
and published. The goal is readable engineering documentation on GitHub, Jira,
desktop, and mobile—not merely a valid image file.

## Publication model

Each published diagram is declared in
[`diagram-publication/diagram-publications.json`](diagram-publication/diagram-publications.json).

| Asset | Role |
|---|---|
| Editable source | Canonical definition. Mermaid diagrams use `.mmd`; composition-sensitive architecture and ER diagrams use the published SVG itself as the curated source. |
| SVG | GitHub embed. It must be scalable, accessible, opaque, and self-contained. |
| PNG | Jira and offline companion. It must be opaque, at least 2000×600 pixels, and generated from the declared SVG. |

Renderer versions are owned by `package.json` and `package-lock.json`; the
publication catalogue contains only diagram ownership and output paths.

GitHub Mobile can retain a previously opened raster preview by repository path.
When a published PNG has already reached reviewers and its visual content is
replaced, give the approved fallback a new meaningful filename and update its
inventory and owner links. Do not depend on a query parameter for cache
invalidation.

## Render and validate

Install the pinned tooling after cloning or changing the lock file:

```powershell
npm ci
```

Render every publication entry:

```powershell
npm run diagrams:render
```

Render one entry while editing it:

```powershell
npm run diagrams:render -- --id <diagram-id>
```

Validate the checked-in assets without rewriting them:

```powershell
npm run diagrams:check
.\mvnw.cmd -q "-Dtest=DiagramPublicationValidatorTest,DiagramPublicationTest" test
```

Mermaid entries regenerate both SVG and PNG output. `CURATED_SVG`
entries retain the accepted SVG unchanged and only regenerate the PNG
companion.

## Required properties

A publication entry passes only when:

- its primary owner and every declared consumer embed the same SVG;
- source, SVG, and PNG paths exist inside the repository;
- the SVG has a `viewBox`, non-empty `title` and `desc`, and an explicit opaque
  background;
- the SVG contains no `script`, `foreignObject`, external reference, or CSS
  import;
- the PNG is readable, opaque, and at least 2000×600 pixels; and
- no temporary clipboard image is published under `docs/`.

Structural checks cannot prove readability. `npm run diagrams:preview` creates
980-pixel desktop, 390-pixel phone, and dark-surround previews under the
untracked `target/documentation-review/` directory. Confirm that:

- the reading direction is obvious;
- labels remain legible and do not clip or overlap;
- connectors and branch labels remain distinguishable;
- foreground and connector contrast works on light and dark surrounding pages;
- no important relationship is hidden by scaling; and
- the full-resolution PNG is suitable for Jira attachment and offline review.

## Choose the remediation

| Classification | Use when | Action |
|---|---|---|
| Pass | The accepted SVG already satisfies the contract and reads clearly | Preserve the SVG and publish or refresh its PNG companion |
| Regenerate | The layout is sound but output format, background, or shared rendering is inconsistent | Render from the existing source with the shared configuration |
| Redesign | The diagram clips, wastes space, branches poorly, or becomes hard to scan | Change grouping, direction, spacing, or labels before rendering |
| Split | One canvas contains independent flows that cannot remain readable at page width | Publish smaller diagrams with one review responsibility each |

Do not increase resolution to conceal a poor layout. Overview and process-flow
diagrams must remain understandable in the normal phone-width embed. For dense
ER slices, the entity grouping and cardinality map must remain distinguishable
at phone width, while field-level detail may use the embedded SVG's lossless
tap-to-open zoom. If the relationship structure itself needs zoom, redesign or
split the diagram.

## Adding a diagram

1. Place the editable source beside its owning document or in that document's
   `assets/` directory.
2. Add a unique publication entry with its source type, SVG, PNG, primary
   owner, and any consumers.
3. Embed the same SVG in the primary owner and every consumer; add a clearly
   labelled PNG link in the primary owner.
4. Render the selected publication entry.
5. Run the structural checks and complete the desktop/mobile review.
6. Record the result in the applicable work-item audit or review evidence.

If validation fails, correct the declared source or publication asset. Do not
allowlist a known defect or edit generated output independently of its source.

