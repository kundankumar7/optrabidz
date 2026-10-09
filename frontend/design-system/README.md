# Design system maintenance

This directory is the maintained source for OptraBidz visual foundations. Product components consume semantic tokens; they do not define independent colour, typography, spacing, motion, or elevation values.

## Source hierarchy

1. `tokens/manifest.tokens.json` declares the supported standard revision, source files, themes, contracts, and generated outputs.
2. `tokens/primitives.tokens.json` contains raw scales and brand values. Components must not consume primitives directly.
3. `tokens/semantic.tokens.json` assigns stable product meaning to primitive values.
4. `tokens/themes/*.tokens.json` overrides only the semantic colour roots allowed by the manifest. Light and dark themes must expose the same paths and types.
5. `contracts/*.contract.json` records accessibility, status, elevation, and responsive requirements that validation enforces.
6. `src/styles/generated/tokens.css` is generated output. Application code consumes its `--ob-*` custom properties.

The logical token identifier is lowercase dot notation, for example `color.text.primary`. Each segment uses lowercase letters, digits, `_`, or `-`. Generation maps that identifier to `color/text/primary` in design tooling and `--ob-color-text-primary` in CSS. Keep the logical identifier identical across design and frontend systems.

## Schema provenance

The supported profile follows the Design Tokens Community Group Format Module 2025.10. The project-maintained schema is not an official DTCG schema. Its revision and SHA-256 checksum are pinned in the manifest; changing the schema requires a revision increment, a new checksum, conformance coverage, and an explicit migration. See `schema/README.md` for the adoption policy.

## Commands

Run these commands from `frontend` with Node 24 and npm 11:

```powershell
npm ci
npm run tokens:test
npm run tokens:validate
npm run tokens:build
npm run tokens:check
npm run tokens:policy
```

`tokens:build` is the only command that writes the tracked CSS output. Run it after an approved source change and commit the resulting CSS in the same change. `tokens:check` regenerates in memory and fails on drift without writing. `tokens:policy` rejects local transfer, state, backup, and laboratory artifacts from the repository.

## Change rules

- Change primitives only when the underlying scale or approved brand value changes.
- Prefer an existing semantic token. Add a semantic token only when a reusable product meaning exists across components or states.
- A component-specific token is admitted only when at least two component instances need the same role, existing semantics would become misleading, and both themes plus relevant interaction states are defined. Record its purpose in the pull request.
- Never hard-code a token value in a component to bypass a missing semantic role.
- Preserve light/dark path and type parity. Theme switching may change appearance, not hierarchy, layout, behavior, or meaning.
- Treat generated files as read-only. Change their sources or generator, regenerate, validate, and review the diff.
- Deprecate before removal: retain the old identifier for one release, document its replacement, migrate all consumers, then remove it in a separate reviewed change. Do not silently repurpose an existing identifier.

## Responsive and accessibility evidence

Breakpoints are content-driven contracts, not device labels. Any threshold change must update the responsive token and contract together, preserve evidence at the exact threshold and one pixel on each side, and cover navigation, cards, dense data, forms, decision dialogs, and readable measure in both themes. Contrast, status, focus, reduced-motion, and interaction-target requirements must continue to pass semantic validation.

## Licences and private artifacts

Typeface maintenance and delivery restrictions are documented in `fonts/README.md` and `licenses/Fontshare-ITF-FFL.txt`; font binaries must never be committed. Icon provenance is recorded in `licenses/Lucide-ISC.txt`.

Design transfer bundles, local state, editable design backups, experiments, review laboratories, and tool workspaces belong outside the repository. Do not weaken the policy by adding broad ignore rules. If a new private artifact class appears, extend the policy test with a precise filename rule.
