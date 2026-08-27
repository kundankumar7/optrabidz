# KAN-39 Diagram Publication Audit

This record tracks the initial defect, remediation class, and final human
readability result for each repository diagram. Automated checks validate
structure; the visual columns record normal-width review rather than claiming
that dimensions alone prove readability.

## First remediation batch

| Diagram ID | Initial publication | Initial defect | Class | Desktop | Mobile | Contrast | Jira PNG | Disposition |
|---|---|---|---|---|---|---|---|---|
| `kan-24-module-architecture` | 552×1228 opaque PNG | Narrow low-resolution raster embedded on GitHub | Regenerate | Pass | Pass | Pass | Pass | Regenerated with concise labels |
| `kan-24-login-flow` | 1506×884 opaque PNG | Raster-only publication | Regenerate | Pass | Pass | Pass | Pass | Regenerated with a narrow protected-cause summary |
| `kan-29-notification-error-flow` | 1302×1178 opaque PNG | Raster-only publication | Regenerate | Pass | Pass | Pass | Pass | Redesigned as a narrow outcome decision tree |
| `kan-31-authentication-flow` | 1729×478 opaque PNG | Wide layout shrinks both trust boundaries on mobile | Redesign | Pass | Pass | Pass | Pass | Stacked independent trust boundaries |
| `kan-36-webhook-ingress-flow` | 1584×898 opaque PNG | Raster-only publication with wide internal rows | Regenerate | Pass | Pass | Pass | Pass | Regenerated with vertically ordered failure outcomes |

The first render exposed a rasterization defect: Mermaid's native SVG labels
split words across `tspan` elements and the PNG renderer discarded their
leading spaces. The shared publisher now adds inherited XML whitespace
preservation before creating the Jira PNG. The final checks above use the
corrected assets at desktop width, 358 content pixels inside a 390-pixel mobile
viewport, and the full 2400-pixel opaque PNG.

## Remaining inventory

The stable architecture/database diagrams, accepted Mermaid SVGs, and dense
KAN-30, KAN-32, KAN-35, KAN-37, and KAN-43 flows are added to this record as
their bounded remediation task begins. Final verification requires one row for
every ID in `docs/architecture/diagram-publication/inventory.json`.
