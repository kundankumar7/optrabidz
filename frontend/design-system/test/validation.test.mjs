import assert from "node:assert/strict";
import { test } from "node:test";

const schemaUrn = "urn:optrabidz:design-tokens:schema:2025.10";

function color(hex, components) {
  return { colorSpace: "srgb", components, alpha: 1, hex };
}

function asTheme(document, theme = "light") {
  return {
    theme,
    document,
    tokens: new Map(),
    sourceFiles: [],
    fingerprint: `sha256:${theme}`,
  };
}

async function loadValidationUtilities() {
  const [references, validation, contrast] = await Promise.all([
    import("../scripts/lib/resolve-references.mjs"),
    import("../scripts/lib/validate-document.mjs"),
    import("../scripts/lib/contrast.mjs"),
  ]);
  return { ...references, ...validation, ...contrast };
}

test("resolves $root, closest-parent types, and referenced-token types", async () => {
  const document = {
    $schema: schemaUrn,
    space: {
      $type: "dimension",
      $root: { $value: { value: 16, unit: "px" } },
      compact: { $value: { value: 8, unit: "px" } },
    },
    palette: {
      ink: { $type: "color", $value: color("#000000", [0, 0, 0]) },
    },
    alias: {
      ink: { $value: "{palette.ink}" },
      inkpointer: { $ref: "#/palette/ink/$value" },
    },
  };
  const { resolveEffectiveType } = await loadValidationUtilities();

  assert.equal(resolveEffectiveType("space.$root", document), "dimension");
  assert.equal(resolveEffectiveType("space.compact", document), "dimension");
  assert.equal(resolveEffectiveType("alias.ink", document), "color");
  assert.equal(resolveEffectiveType("alias.inkpointer", document), "color");
});

test("resolves curly aliases, JSON Pointers, and property-level references", async () => {
  const document = {
    $schema: schemaUrn,
    base: {
      ink: { $type: "color", $value: color("#000000", [0, 0, 0]) },
      step: { $type: "number", $value: 16 },
    },
    alias: {
      curly: { $value: "{base.ink}" },
      pointer: { $type: "color", $ref: "#/base/ink/$value" },
      dimension: {
        $type: "dimension",
        $value: {
          value: { $ref: "#/base/step/$value" },
          unit: "px",
        },
      },
    },
  };
  const { resolveReferences } = await loadValidationUtilities();
  const resolved = resolveReferences(document);

  assert.deepEqual(resolved.get("alias.curly").value, color("#000000", [0, 0, 0]));
  assert.deepEqual(resolved.get("alias.pointer").value, color("#000000", [0, 0, 0]));
  assert.deepEqual(resolved.get("alias.dimension").value, { value: 16, unit: "px" });
});

test("materializes $extends with inherited tokens, local additions, and overrides", async () => {
  const document = {
    $schema: schemaUrn,
    base: {
      $type: "dimension",
      small: { $value: { value: 8, unit: "px" } },
      medium: { $value: { value: 16, unit: "px" } },
    },
    spacious: {
      $extends: "{base}",
      medium: { $value: { value: 24, unit: "px" } },
      large: { $value: { value: 32, unit: "px" } },
    },
    pointer: {
      $extends: "#/base",
    },
  };
  const { resolveReferences } = await loadValidationUtilities();
  const resolved = resolveReferences(document);

  assert.deepEqual(resolved.get("spacious.small").value, { value: 8, unit: "px" });
  assert.deepEqual(resolved.get("spacious.medium").value, { value: 24, unit: "px" });
  assert.equal(resolved.get("spacious.large").type, "dimension");
  assert.deepEqual(resolved.get("pointer.small").value, { value: 8, unit: "px" });
});

test("rejects reference cycles, extension cycles, and reference type mismatches", async () => {
  const { resolveReferences } = await loadValidationUtilities();

  assert.throws(
    () =>
      resolveReferences({
        $schema: schemaUrn,
        a: { $value: "{b}" },
        b: { $value: "{a}" },
      }),
    /reference cycle.*a.*b/i,
  );

  assert.throws(
    () =>
      resolveReferences({
        $schema: schemaUrn,
        composite: {
          $type: "dimension",
          $value: {
            value: { $ref: "#/composite/$value/unit" },
            unit: { $ref: "#/composite/$value/value" },
          },
        },
      }),
    /reference cycle.*composite/i,
  );

  assert.throws(
    () =>
      resolveReferences({
        $schema: schemaUrn,
        groupa: { $extends: "{groupb}" },
        groupb: { $extends: "{groupa}" },
      }),
    /extension cycle.*groupa.*groupb/i,
  );

  assert.throws(
    () =>
      resolveReferences({
        $schema: schemaUrn,
        base: { $type: "color", $value: color("#000000", [0, 0, 0]) },
        alias: { $type: "number", $value: "{base}" },
      }),
    /type mismatch.*alias.*number.*color/i,
  );
});

test("reports unresolved types and invalid color, dimension, and typography values", async () => {
  const { validateDocument } = await loadValidationUtilities();
  const report = validateDocument(
    asTheme({
      $schema: schemaUrn,
      unresolved: { $value: 1 },
      badcolor: { $type: "color", $value: "#000000" },
      baddimension: { $type: "dimension", $value: { value: 1, unit: "em" } },
      badtype: {
        $type: "typography",
        $value: {
          fontFamily: ["General Sans", "Arial", "sans-serif"],
          fontSize: { value: 16, unit: "px" },
          fontWeight: 400,
          lineHeight: { value: 24, unit: "px" },
        },
      },
    }),
    {},
  );

  assert.equal(report.valid, false);
  assert.ok(report.issues.some((issue) => issue.code === "unresolved-type" && issue.path === "unresolved"));
  assert.ok(report.issues.some((issue) => issue.code === "invalid-color" && issue.path === "badcolor"));
  assert.ok(report.issues.some((issue) => issue.code === "invalid-dimension" && issue.path === "baddimension"));
  assert.ok(report.issues.some((issue) => issue.code === "invalid-typography" && issue.path === "badtype"));
});

test("rejects malformed values for every supported DTCG scalar and composite type", async () => {
  const { validateDocument } = await loadValidationUtilities();
  const report = validateDocument(
    asTheme({
      $schema: schemaUrn,
      badfamily: { $type: "fontFamily", $value: [] },
      badweight: { $type: "fontWeight", $value: "semibold" },
      badduration: { $type: "duration", $value: { value: 200, unit: "px" } },
      badcurve: { $type: "cubicBezier", $value: [1.2, 0, 1, 1] },
      badstroke: { $type: "strokeStyle", $value: "wavy" },
      badborder: {
        $type: "border",
        $value: { color: color("#000000", [0, 0, 0]), width: 1, style: "solid" },
      },
      badtransition: {
        $type: "transition",
        $value: {
          duration: { value: 200, unit: "ms" },
          delay: { value: 0, unit: "ms" },
          timingFunction: [2, 0, 1, 1],
        },
      },
      badshadow: {
        $type: "shadow",
        $value: {
          color: color("#000000", [0, 0, 0]),
          offsetX: { value: 0, unit: "px" },
          offsetY: { value: 1, unit: "px" },
          blur: { value: 2, unit: "px" },
        },
      },
      badgradient: {
        $type: "gradient",
        $value: [{ color: color("#000000", [0, 0, 0]) }],
      },
      badtypography: {
        $type: "typography",
        $value: {
          fontFamily: "Manrope",
          fontSize: { value: 16, unit: "px" },
          fontWeight: 400,
          lineHeight: 1.5,
        },
      },
    }),
    {},
  );

  for (const code of [
    "invalid-font-family",
    "invalid-font-weight",
    "invalid-duration",
    "invalid-cubic-bezier",
    "invalid-stroke-style",
    "invalid-border",
    "invalid-transition",
    "invalid-shadow",
    "invalid-gradient",
    "invalid-typography",
  ]) {
    assert.ok(report.issues.some((issue) => issue.code === code), `Missing ${code}`);
  }
});

test("reports missing and type-mismatched paths between light and dark themes", async () => {
  const { validateThemeParity } = await loadValidationUtilities();
  const light = asTheme({
    $schema: schemaUrn,
    color: {
      primary: { $type: "color", $value: color("#000000", [0, 0, 0]) },
      secondary: { $type: "color", $value: color("#FFFFFF", [1, 1, 1]) },
    },
  });
  const dark = asTheme(
    {
      $schema: schemaUrn,
      color: {
        primary: { $type: "dimension", $value: { value: 1, unit: "px" } },
      },
    },
    "dark",
  );
  const issues = validateThemeParity(light, dark);

  assert.ok(issues.some((issue) => issue.code === "theme-type-mismatch" && issue.path === "color.primary"));
  assert.ok(issues.some((issue) => issue.code === "theme-path-missing" && issue.path === "color.secondary"));
});

test("validates contrast thresholds, theme coverage, token references, and status communication", async () => {
  const document = {
    $schema: schemaUrn,
    color: {
      $type: "color",
      neutral: {
        black: { $value: color("#000000", [0, 0, 0]) },
        white: { $value: color("#FFFFFF", [1, 1, 1]) },
      },
      text: { primary: { $value: "{color.neutral.black}" } },
      surface: { canvas: { $value: "{color.neutral.white}" } },
      status: {
        overdue: {
          background: { $value: "{color.neutral.white}" },
          foreground: { $value: "{color.neutral.black}" },
          border: { $value: "{color.neutral.black}" },
          icon: { $value: "{color.neutral.black}" },
        },
      },
    },
  };
  const accessibility = {
    schemaVersion: 1,
    contrastPairs: [
      {
        id: "primary-text-on-canvas",
        foreground: "color.text.primary",
        background: "color.surface.canvas",
        category: "normalText",
        minimum: 4.5,
        themes: ["light", "dark"],
      },
    ],
  };
  const status = {
    schemaVersion: 1,
    statuses: {
      overdue: {
        tokens: {
          background: "color.status.overdue.background",
          foreground: "color.status.overdue.foreground",
          border: "color.status.overdue.border",
          icon: "color.status.overdue.icon",
        },
        labelRequired: true,
        nonColorCue: "iconOrText",
      },
    },
  };
  const { validateDocument } = await loadValidationUtilities();

  assert.equal(validateDocument(asTheme(document), { accessibility, status }).valid, true);

  const invalidAccessibility = structuredClone(accessibility);
  invalidAccessibility.contrastPairs.push({
    ...structuredClone(invalidAccessibility.contrastPairs[0]),
    minimum: 3,
    themes: ["light"],
  });
  invalidAccessibility.contrastPairs[0].foreground = "color.text.missing";
  const invalidStatus = structuredClone(status);
  invalidStatus.statuses.overdue.labelRequired = false;
  delete invalidStatus.statuses.overdue.nonColorCue;

  const report = validateDocument(asTheme(document), {
    accessibility: invalidAccessibility,
    status: invalidStatus,
  });

  assert.ok(report.issues.some((issue) => issue.code === "duplicate-contrast-id"));
  assert.ok(report.issues.some((issue) => issue.code === "missing-contrast-token"));
  assert.ok(report.issues.some((issue) => issue.code === "contrast-threshold"));
  assert.ok(report.issues.some((issue) => issue.code === "contrast-theme-coverage"));
  assert.ok(report.issues.some((issue) => issue.code === "status-contract-schema"));
});

test("validates elevation contract token existence and effective types", async () => {
  const { validateDocument } = await loadValidationUtilities();
  const elevation = {
    schemaVersion: 1,
    levels: {
      none: { surface: "color.surface.canvas", border: "color.border.subtle", shadow: null },
      low: { surface: "color.surface.canvas", border: "color.border.subtle", shadow: "shadow.low" },
      medium: { surface: "color.surface.canvas", border: "color.border.subtle", shadow: "shadow.missing" },
      high: { surface: "color.surface.canvas", border: "space.small", shadow: "shadow.low" },
    },
  };
  const report = validateDocument(
    asTheme({
      $schema: schemaUrn,
      color: {
        $type: "color",
        surface: { canvas: { $value: color("#FFFFFF", [1, 1, 1]) } },
        border: { subtle: { $value: color("#000000", [0, 0, 0]) } },
      },
      shadow: {
        low: {
          $type: "shadow",
          $value: {
            color: color("#00000080", [0, 0, 0]),
            offsetX: { value: 0, unit: "px" },
            offsetY: { value: 1, unit: "px" },
            blur: { value: 2, unit: "px" },
            spread: { value: 0, unit: "px" },
          },
        },
      },
      space: { small: { $type: "dimension", $value: { value: 4, unit: "px" } } },
    }),
    { elevation },
  );

  assert.ok(
    report.issues.some(
      (issue) => issue.code === "missing-elevation-token" && issue.path === "medium.shadow",
    ),
  );
  assert.ok(
    report.issues.some(
      (issue) => issue.code === "elevation-token-type" && issue.path === "high.border",
    ),
  );
});

test("validates responsive token values, ordering, and exact boundary evidence", async () => {
  const { validateDocument } = await loadValidationUtilities();
  const document = {
    $schema: schemaUrn,
    responsive: {
      breakpoint: {
        $type: "dimension",
        compact: { $value: { value: 680, unit: "px" } },
        comfortable: { $value: { value: 920, unit: "px" } },
        wide: { $value: { value: 960, unit: "px" } },
      },
    },
  };
  const responsive = {
    schemaVersion: 1,
    strategy: "contentDriven",
    evidence: {
      reference: "responsive-foundation-stress-review-v2",
      themes: ["light", "dark"],
      widths: [679, 680, 681, 919, 920, 921, 959, 960, 961],
      requiredContent: ["navigation", "cardGrid", "denseData", "form", "decisionDialog", "readableMeasure"],
    },
    breakpoints: [
      breakpoint("compact", 680),
      breakpoint("comfortable", 920),
      breakpoint("wide", 960),
    ],
  };

  assert.equal(validateDocument(asTheme(document), { responsive }).valid, true);

  const invalid = structuredClone(responsive);
  invalid.breakpoints[0].id = "comfortable";
  invalid.breakpoints[1].threshold = 900;
  invalid.breakpoints[2].boundaryEvidence.exact = 959;
  invalid.evidence.widths = invalid.evidence.widths.filter((width) => width !== 961);
  const report = validateDocument(asTheme(document), { responsive: invalid });

  assert.ok(report.issues.some((issue) => issue.code === "duplicate-responsive-id"));
  assert.ok(report.issues.some((issue) => issue.code === "responsive-token-value"));
  assert.ok(report.issues.some((issue) => issue.code === "responsive-boundary-evidence"));
  assert.ok(report.issues.some((issue) => issue.code === "responsive-evidence-width"));
});

function breakpoint(id, threshold) {
  return {
    id,
    token: `responsive.breakpoint.${id}`,
    threshold,
    condition: "maxWidthInclusive",
    boundaryEvidence: { below: threshold - 1, exact: threshold, above: threshold + 1 },
    structuralChanges: ["contentDrivenTransition"],
  };
}

test("calculates WCAG contrast from DTCG sRGB colors", async () => {
  const { contrastRatio } = await loadValidationUtilities();

  assert.equal(contrastRatio(color("#000000", [0, 0, 0]), color("#FFFFFF", [1, 1, 1])), 21);
});
