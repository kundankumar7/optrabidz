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

## Dense-flow splits

| Original diagram | Initial publication | Initial defect | Replacement | Class | Desktop | Mobile | Contrast | Jira PNG | Disposition |
|---|---|---|---|---|---|---|---|---|---|
| `kan-30-financial-error-flow` | 1584×2118 opaque PNG | One canvas mixed API and provider ingress responsibilities and compressed review paths | `kan-30-financial-request-error-flow` | Split | Pass | Pass | Pass | Pass | Focused API request flow published |
| `kan-30-financial-error-flow` | 1584×2118 opaque PNG | One canvas mixed API and provider ingress responsibilities and compressed review paths | `kan-30-financial-webhook-error-flow` | Split | Pass | Pass | Pass | Pass | Focused provider webhook flow published |
| `kan-32-webhook-replay-flow` | 2446×4194 transparent PNG | Tall combined ingress, transaction, replay classification, and audit canvas | `kan-32-webhook-replay-ingress` | Split | Pass | Pass | Pass | Pass | Focused ingress and atomic-claim flow published |
| `kan-32-webhook-replay-flow` | 2446×4194 transparent PNG | Tall combined ingress, transaction, replay classification, and audit canvas | `kan-32-webhook-replay-outcomes` | Split | Pass | Pass | Pass | Pass | Transaction and existing-claim outcomes published |

## Payment and settlement redesigns

| Diagram ID | Initial publication | Initial defect | Class | Desktop | Mobile | Contrast | Jira PNG | Disposition |
|---|---|---|---|---|---|---|---|---|
| `kan-35-payment-error-boundary` | 868×1576 transparent PNG | Transparent raster and dense scoped-lookup fan-out | Redesign | Pass | Pass | Pass | Pass | Narrow scoped-lookup and error-rendering stages published |
| `kan-35-payment-state-errors` | 1784×1199 transparent PNG | Wide mixed creation and completion outcomes | Redesign | Pass | Pass | Pass | Pass | Creation and completion outcomes separated |
| `kan-37-settlement-error-boundary` | 1283×1912 transparent PNG | Dense role/lookup fan-out and transparent publication | Redesign | Pass | Pass | Pass | Pass | Role-first lookup and disclosure-safe error stages published |
| `kan-37-settlement-confirmation-state` | 986×1044 transparent PNG | Overlapping branch labels and combined effect nodes | Redesign | Pass | Pass | Pass | Pass | Confirmation, reload classification, and effects separated |

## Accepted Mermaid normalization

| Diagram ID | Initial publication | Initial defect | Class | Desktop | Mobile | Contrast | Jira PNG | Disposition |
|---|---|---|---|---|---|---|---|---|
| `kan-33-single-error-contract` | 2532×3164 opaque PNG plus SVG | Per-diagram renderer configuration and inconsistent export | Regenerate | Pass | Pass | Pass | Pass | Regenerated; topology preserved |
| `kan-33-legacy-deletion-boundary` | 1898×1906 opaque PNG plus SVG | Per-diagram renderer configuration and inconsistent export | Regenerate | Pass | Pass | Pass | Pass | Regenerated; topology preserved |
| `kan-34-repayment-error-boundary` | 3168×2400 opaque PNG plus accepted SVG | Accepted layout used duplicated renderer configuration | Regenerate | Pass | Pass | Pass | Pass | Accepted layout retained |
| `kan-34-repayment-transition-state` | 2840×2804 opaque PNG plus accepted SVG | Accepted layout used duplicated renderer configuration | Regenerate | Pass | Pass | Pass | Pass | Accepted layout retained |
| `kan-42-real-http-boundary` | 2352×1194 opaque PNG plus SVG | Per-diagram renderer configuration and inconsistent export | Regenerate | Pass | Pass | Pass | Pass | Regenerated; topology preserved |

The stable architecture/database diagrams, accepted Mermaid SVGs, and dense
KAN-30, KAN-32, KAN-35, KAN-37, and KAN-43 flows are added to this record as
their bounded remediation task begins. Final verification requires one row for
every ID in `docs/architecture/diagram-publication/inventory.json`.
