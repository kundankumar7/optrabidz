# Diagram Publication

This reference defines how repository diagrams are stored, rendered, reviewed,
and published. The goal is readable engineering documentation on GitHub, Jira,
desktop, and mobile—not merely a valid image file.

## Publication model

Each published diagram is declared in
[`diagram-publication/inventory.json`](diagram-publication/inventory.json).

| Asset | Role |
|---|---|
| Editable source | Canonical definition. Mermaid diagrams use `.mmd`; accepted hand-authored architecture and ER diagrams retain their documented source convention. |
| SVG | GitHub embed. It must be scalable, accessible, opaque, and self-contained. |
| PNG | Jira and offline companion. It must be opaque, at least 2000×600 pixels, and generated from the declared SVG. |

The renderer uses Mermaid CLI 11.16.0 and Sharp 0.35.4. Both versions are
pinned in `package-lock.json` so local and CI output is reproducible.

## Render and validate

Install the pinned tooling after cloning or changing the lock file:

```powershell
npm ci
```

Render every inventory entry:

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

Mermaid entries regenerate both SVG and PNG output. `HAND_AUTHORED_SVG`
entries retain the accepted SVG unchanged and only regenerate the PNG
companion.

## Required properties

A publication entry passes only when:

- its owner document embeds the declared SVG;
- source, SVG, and required PNG paths exist inside the repository;
- the SVG has a `viewBox`, non-empty `title` and `desc`, and an explicit opaque
  background;
- the SVG contains no `script`, `foreignObject`, external reference, or CSS
  import;
- the PNG is readable, opaque, and at least 2000×600 pixels; and
- no temporary clipboard image is published under `docs/`.

Structural checks cannot prove readability. Review every changed diagram at
normal desktop width and at a 358-pixel mobile content width. Confirm that:

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

Do not increase resolution to conceal a poor layout. If a reviewer needs zoom
to understand the normal GitHub embed, redesign or split it.

## Adding a diagram

1. Place the editable source beside its owning document or in that document's
   `assets/` directory.
2. Add a unique inventory entry with its owner, source type, SVG, PNG, and
   remediation classification.
3. Embed the SVG in the owner document and add a clearly labelled PNG link.
4. Render the selected inventory entry.
5. Run the structural checks and complete the desktop/mobile review.
6. Record the result in the applicable work-item audit or review evidence.

If validation fails, correct the declared source or publication asset. Do not
allowlist a known defect or edit generated output independently of its source.

