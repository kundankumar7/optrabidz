import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import Ajv from "ajv";

import { contrastRatio } from "./contrast.mjs";
import { resolveReferences } from "./resolve-references.mjs";

const schemaRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..", "schema");
const ajv = new Ajv({ allErrors: true, strict: false });
const validators = {
  document: ajv.compile(readSchema("dtcg-2025.10.schema.json")),
  accessibility: ajv.compile(readSchema("accessibility-contract.schema.json")),
  status: ajv.compile(readSchema("status-contract.schema.json")),
  elevation: ajv.compile(readSchema("elevation-contract.schema.json")),
  responsive: ajv.compile(readSchema("responsive-contract.schema.json")),
};

const categoryMinimums = {
  normalText: 4.5,
  largeText: 3,
  nonText: 3,
};
const fontWeightNames = new Set([
  "thin",
  "hairline",
  "extra-light",
  "ultra-light",
  "light",
  "normal",
  "regular",
  "book",
  "medium",
  "semi-bold",
  "demi-bold",
  "bold",
  "extra-bold",
  "ultra-bold",
  "black",
  "heavy",
  "extra-black",
  "ultra-black",
]);
const strokeStyleNames = new Set([
  "solid",
  "dashed",
  "dotted",
  "double",
  "groove",
  "ridge",
  "outset",
  "inset",
]);

export function validateDocument(assembled, contracts) {
  const issues = [];
  validateAgainstSchema(validators.document, assembled.document, "document-schema", issues);

  let resolvedTokens = new Map();
  try {
    resolvedTokens = resolveReferences(assembled.document);
  } catch (error) {
    issues.push(issue("reference-resolution", "<document>", error.message));
  }

  for (const [logicalId, token] of resolvedTokens) {
    if (!token.type) {
      issues.push(issue("unresolved-type", logicalId, "Token has no resolvable effective type."));
      continue;
    }
    const valueIssue = validateResolvedValue(token.type, token.value);
    if (valueIssue) {
      issues.push(issue(valueIssue.code, logicalId, valueIssue.message));
    }
  }

  if (contracts.accessibility) {
    validateAccessibility(
      assembled.theme,
      resolvedTokens,
      contracts.accessibility,
      issues,
    );
  }
  if (contracts.status) {
    validateStatus(resolvedTokens, contracts.status, issues);
  }
  if (contracts.elevation) {
    validateElevation(resolvedTokens, contracts.elevation, issues);
  }
  if (contracts.responsive) {
    validateResponsive(resolvedTokens, contracts.responsive, issues);
  }

  issues.sort(compareIssues);
  return {
    valid: issues.length === 0,
    issues,
    resolvedTokens,
    fingerprint: assembled.fingerprint,
  };
}

export function validateThemeParity(light, dark) {
  const issues = [];
  let lightTokens;
  let darkTokens;

  try {
    lightTokens = resolveReferences(light.document);
    darkTokens = resolveReferences(dark.document);
  } catch (error) {
    return [issue("theme-resolution", "<document>", error.message)];
  }

  const allPaths = new Set([...lightTokens.keys(), ...darkTokens.keys()]);
  for (const logicalId of [...allPaths].sort()) {
    const lightToken = lightTokens.get(logicalId);
    const darkToken = darkTokens.get(logicalId);
    if (!lightToken || !darkToken) {
      issues.push(
        issue(
          "theme-path-missing",
          logicalId,
          `${logicalId} must exist in both light and dark themes.`,
        ),
      );
    } else if (lightToken.type !== darkToken.type) {
      issues.push(
        issue(
          "theme-type-mismatch",
          logicalId,
          `Light type ${lightToken.type} differs from dark type ${darkToken.type}.`,
        ),
      );
    }
  }
  return issues;
}

function validateAccessibility(theme, tokens, contract, issues) {
  if (!validateAgainstSchema(validators.accessibility, contract, "accessibility-contract-schema", issues)) {
    return;
  }

  const seenIds = new Set();
  for (const pair of contract.contrastPairs) {
    if (seenIds.has(pair.id)) {
      issues.push(issue("duplicate-contrast-id", pair.id, `Duplicate contrast ID ${pair.id}.`));
    }
    seenIds.add(pair.id);

    const requiredMinimum = categoryMinimums[pair.category];
    if (pair.minimum < requiredMinimum) {
      issues.push(
        issue(
          "contrast-threshold",
          pair.id,
          `${pair.category} requires at least ${requiredMinimum}:1.`,
        ),
      );
    }

    if (!pair.themes.includes("light") || !pair.themes.includes("dark")) {
      issues.push(
        issue(
          "contrast-theme-coverage",
          pair.id,
          "Contrast evidence must cover both light and dark themes.",
        ),
      );
    }

    const foreground = tokens.get(pair.foreground);
    const background = tokens.get(pair.background);
    if (!foreground || !background) {
      issues.push(
        issue(
          "missing-contrast-token",
          pair.id,
          `Missing contrast token ${!foreground ? pair.foreground : pair.background}.`,
        ),
      );
      continue;
    }
    if (foreground.type !== "color" || background.type !== "color") {
      issues.push(issue("contrast-token-type", pair.id, "Contrast pairs require color tokens."));
      continue;
    }

    if (pair.themes.includes(theme)) {
      const ratio = contrastRatio(foreground.value, background.value);
      if (ratio + Number.EPSILON < pair.minimum) {
        issues.push(
          issue(
            "contrast-ratio",
            pair.id,
            `Contrast ${ratio.toFixed(2)}:1 is below ${pair.minimum}:1.`,
          ),
        );
      }
    }
  }
}

function validateStatus(tokens, contract, issues) {
  if (!validateAgainstSchema(validators.status, contract, "status-contract-schema", issues)) {
    return;
  }

  for (const [statusName, status] of Object.entries(contract.statuses)) {
    for (const [role, logicalId] of Object.entries(status.tokens)) {
      const token = tokens.get(logicalId);
      if (!token) {
        issues.push(
          issue("missing-status-token", `${statusName}.${role}`, `Missing status token ${logicalId}.`),
        );
      } else if (token.type !== "color") {
        issues.push(
          issue("status-token-type", `${statusName}.${role}`, `${logicalId} must be a color token.`),
        );
      }
    }
  }
}

function validateElevation(tokens, contract, issues) {
  if (!validateAgainstSchema(validators.elevation, contract, "elevation-contract-schema", issues)) {
    return;
  }

  for (const [levelName, level] of Object.entries(contract.levels)) {
    validateContractToken(tokens, level.surface, "color", `${levelName}.surface`, issues);
    validateContractToken(tokens, level.border, "color", `${levelName}.border`, issues);
    if (level.shadow !== null) {
      validateContractToken(tokens, level.shadow, "shadow", `${levelName}.shadow`, issues);
    }
  }
}

function validateResponsive(tokens, contract, issues) {
  if (!validateAgainstSchema(validators.responsive, contract, "responsive-contract-schema", issues)) {
    return;
  }

  const evidenceWidths = new Set(contract.evidence.widths);
  const seenIds = new Set();
  const seenTokens = new Set();
  let previousThreshold = -Infinity;

  for (const breakpoint of contract.breakpoints) {
    if (seenIds.has(breakpoint.id)) {
      issues.push(
        issue("duplicate-responsive-id", breakpoint.id, `Duplicate responsive ID ${breakpoint.id}.`),
      );
    }
    seenIds.add(breakpoint.id);

    if (seenTokens.has(breakpoint.token)) {
      issues.push(
        issue("duplicate-responsive-token", breakpoint.id, `Duplicate responsive token ${breakpoint.token}.`),
      );
    }
    seenTokens.add(breakpoint.token);

    if (breakpoint.threshold <= previousThreshold) {
      issues.push(
        issue(
          "responsive-threshold-order",
          breakpoint.id,
          "Responsive thresholds must be strictly increasing.",
        ),
      );
    }
    previousThreshold = breakpoint.threshold;

    const token = tokens.get(breakpoint.token);
    if (!token) {
      issues.push(
        issue("missing-responsive-token", breakpoint.id, `Missing responsive token ${breakpoint.token}.`),
      );
    } else if (token.type !== "dimension") {
      issues.push(
        issue(
          "responsive-token-type",
          breakpoint.id,
          `${breakpoint.token} must resolve to a dimension, not ${token.type}.`,
        ),
      );
    } else if (token.value.unit !== "px" || token.value.value !== breakpoint.threshold) {
      issues.push(
        issue(
          "responsive-token-value",
          breakpoint.id,
          `${breakpoint.token} must equal ${breakpoint.threshold}px.`,
        ),
      );
    }

    const expectedBoundary = {
      below: breakpoint.threshold - 1,
      exact: breakpoint.threshold,
      above: breakpoint.threshold + 1,
    };
    for (const [position, expectedWidth] of Object.entries(expectedBoundary)) {
      const actualWidth = breakpoint.boundaryEvidence[position];
      if (actualWidth !== expectedWidth) {
        issues.push(
          issue(
            "responsive-boundary-evidence",
            `${breakpoint.id}.${position}`,
            `${position} evidence must be ${expectedWidth}px, not ${actualWidth}px.`,
          ),
        );
      }
      if (!evidenceWidths.has(expectedWidth)) {
        issues.push(
          issue(
            "responsive-evidence-width",
            `${breakpoint.id}.${position}`,
            `Evidence widths must include ${expectedWidth}px.`,
          ),
        );
      }
    }
  }
}

function validateContractToken(tokens, logicalId, expectedType, contractPath, issues) {
  const token = tokens.get(logicalId);
  if (!token) {
    issues.push(
      issue("missing-elevation-token", contractPath, `Missing elevation token ${logicalId}.`),
    );
  } else if (token.type !== expectedType) {
    issues.push(
      issue(
        "elevation-token-type",
        contractPath,
        `${logicalId} must resolve to ${expectedType}, not ${token.type}.`,
      ),
    );
  }
}

function validateResolvedValue(type, value) {
  switch (type) {
    case "color":
      return isColor(value)
        ? undefined
        : { code: "invalid-color", message: "Color must be a resolved DTCG sRGB object." };
    case "dimension":
      return isDimension(value)
        ? undefined
        : { code: "invalid-dimension", message: "Dimension requires a numeric value and px or rem." };
    case "number":
      return typeof value === "number"
        ? undefined
        : { code: "invalid-number", message: "Number token must resolve to a number." };
    case "typography":
      return isTypography(value)
        ? undefined
        : {
            code: "invalid-typography",
            message: "Typography requires valid family, size, weight, and unitless line height.",
          };
    case "fontFamily":
      return isFontFamily(value)
        ? undefined
        : { code: "invalid-font-family", message: "Font family must be a string or string array." };
    case "fontWeight":
      return isFontWeight(value)
        ? undefined
        : { code: "invalid-font-weight", message: "Font weight is outside the supported range." };
    case "duration":
      return isDuration(value)
        ? undefined
        : { code: "invalid-duration", message: "Duration requires a numeric value and ms or s." };
    case "cubicBezier":
      return isCubicBezier(value)
        ? undefined
        : {
            code: "invalid-cubic-bezier",
            message: "Cubic Bézier requires four numbers with x coordinates from 0 to 1.",
          };
    case "strokeStyle":
      return isStrokeStyle(value)
        ? undefined
        : { code: "invalid-stroke-style", message: "Stroke style is not a valid DTCG value." };
    case "border":
      return isBorder(value)
        ? undefined
        : { code: "invalid-border", message: "Border requires valid color, width, and style values." };
    case "transition":
      return isTransition(value)
        ? undefined
        : {
            code: "invalid-transition",
            message: "Transition requires valid duration, delay, and timing function values.",
          };
    case "shadow":
      return isShadow(value)
        ? undefined
        : { code: "invalid-shadow", message: "Shadow is not a valid DTCG shadow value." };
    case "gradient":
      return isGradient(value)
        ? undefined
        : { code: "invalid-gradient", message: "Gradient requires valid color-stop objects." };
    default:
      return undefined;
  }
}

function validateAgainstSchema(validator, value, code, issues) {
  if (validator(value)) {
    return true;
  }
  for (const error of validator.errors ?? []) {
    issues.push(issue(code, error.instancePath || "<root>", error.message ?? "Schema validation failed."));
  }
  return false;
}

function isColor(value) {
  return (
    isObject(value) &&
    value.colorSpace === "srgb" &&
    Array.isArray(value.components) &&
    value.components.length === 3 &&
    value.components.every(
      (component) => Number.isFinite(component) && component >= 0 && component <= 1,
    ) &&
    (value.alpha === undefined ||
      (Number.isFinite(value.alpha) && value.alpha >= 0 && value.alpha <= 1)) &&
    (value.hex === undefined || /^#[0-9a-f]{6}(?:[0-9a-f]{2})?$/i.test(value.hex))
  );
}

function isDimension(value) {
  return (
    isObject(value) &&
    Number.isFinite(value.value) &&
    (value.unit === "px" || value.unit === "rem")
  );
}

function isFontFamily(value) {
  return (
    (typeof value === "string" && value.length > 0) ||
    (Array.isArray(value) &&
      value.length > 0 &&
      value.every((item) => typeof item === "string" && item.length > 0))
  );
}

function isFontWeight(value) {
  return (
    (Number.isFinite(value) && value >= 1 && value <= 1000) ||
    (typeof value === "string" && fontWeightNames.has(value))
  );
}

function isDuration(value) {
  return (
    isObject(value) &&
    Number.isFinite(value.value) &&
    (value.unit === "ms" || value.unit === "s")
  );
}

function isCubicBezier(value) {
  return (
    Array.isArray(value) &&
    value.length === 4 &&
    value.every(Number.isFinite) &&
    value[0] >= 0 &&
    value[0] <= 1 &&
    value[2] >= 0 &&
    value[2] <= 1
  );
}

function isStrokeStyle(value) {
  if (typeof value === "string") {
    return strokeStyleNames.has(value);
  }
  return (
    isObject(value) &&
    Array.isArray(value.dashArray) &&
    value.dashArray.length > 0 &&
    value.dashArray.every(isDimension) &&
    ["round", "butt", "square"].includes(value.lineCap)
  );
}

function isBorder(value) {
  return (
    isObject(value) &&
    isColor(value.color) &&
    isDimension(value.width) &&
    isStrokeStyle(value.style)
  );
}

function isTransition(value) {
  return (
    isObject(value) &&
    isDuration(value.duration) &&
    isDuration(value.delay) &&
    isCubicBezier(value.timingFunction)
  );
}

function isShadow(value) {
  if (Array.isArray(value)) {
    return value.length > 0 && value.every(isShadowObject);
  }
  return isShadowObject(value);
}

function isShadowObject(value) {
  return (
    isObject(value) &&
    isColor(value.color) &&
    isDimension(value.offsetX) &&
    isDimension(value.offsetY) &&
    isDimension(value.blur) &&
    isDimension(value.spread) &&
    (value.inset === undefined || typeof value.inset === "boolean")
  );
}

function isGradient(value) {
  return (
    Array.isArray(value) &&
    value.length > 0 &&
    value.every(
      (stop) => isObject(stop) && isColor(stop.color) && Number.isFinite(stop.position),
    )
  );
}

function isTypography(value) {
  return (
    isObject(value) &&
    isFontFamily(value.fontFamily) &&
    isDimension(value.fontSize) &&
    isFontWeight(value.fontWeight) &&
    isDimension(value.letterSpacing) &&
    Number.isFinite(value.lineHeight)
  );
}

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function issue(code, pathValue, message) {
  return { code, path: pathValue, message };
}

function compareIssues(left, right) {
  return (
    left.code.localeCompare(right.code) ||
    left.path.localeCompare(right.path) ||
    left.message.localeCompare(right.message)
  );
}

function readSchema(fileName) {
  return JSON.parse(readFileSync(path.join(schemaRoot, fileName), "utf8"));
}
