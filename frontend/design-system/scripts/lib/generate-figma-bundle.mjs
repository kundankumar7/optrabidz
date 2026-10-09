import { createHash } from "node:crypto";
import path from "node:path";

import { assertOutsideRepository, designSystemPaths } from "./paths.mjs";
import { resolveReferences } from "./resolve-references.mjs";
import { writeIfChanged } from "./serialize-output.mjs";
import { logicalToFigmaName } from "./token-paths.mjs";

const COLLECTION_NAMES = [
  "Core",
  "Semantic / Light",
  "Semantic / Dark",
  "Layout / Mobile",
  "Layout / Tablet",
  "Layout / Desktop",
];

const SEMANTIC_PREFIXES = [
  "color.surface.",
  "color.text.",
  "color.border.",
  "color.icon.",
  "color.focus.",
  "color.shadow.",
  "color.scrollbar.",
  "color.action.",
  "color.status.",
  "typography.",
  "focus.ring.",
  "shadow.low",
  "shadow.medium",
  "shadow.high",
  "layer.",
  "responsive.",
  "density.",
  "interaction.",
  "motion.feedback",
  "motion.orientation",
  "motion.deliberate",
];

const TYPOGRAPHY_PROPERTIES = [
  ["fontFamily", "font-family"],
  ["fontSize", "font-size"],
  ["fontWeight", "font-weight"],
  ["letterSpacing", "letter-spacing"],
  ["lineHeight", "line-height"],
];

const TRANSITION_PROPERTIES = [
  ["duration", "duration"],
  ["delay", "delay"],
  ["timingFunction", "timing-function"],
];

export function generateFigmaBundle(light, dark) {
  assertThemePair(light, dark);

  const bundle = {
    schemaVersion: 1,
    sourceFingerprint: fingerprintSources(light, dark),
    collections: [
      collection("Core", buildCoreVariables(light)),
      collection("Semantic / Light", buildSemanticColorVariables(light)),
      collection("Semantic / Dark", buildSemanticColorVariables(dark)),
      collection("Layout / Mobile", buildLayoutVariables(light, "mobile")),
      collection("Layout / Tablet", buildLayoutVariables(light, "tablet")),
      collection("Layout / Desktop", buildLayoutVariables(light, "desktop")),
    ],
    textStyles: buildTextStyles(light),
    evidence: buildEvidence(light, dark),
  };

  if (!bundle.collections.every((item, index) => item.name === COLLECTION_NAMES[index])) {
    throw new Error("Figma collection order does not match the approved contract.");
  }
  return bundle;
}

export async function writeTemporaryFigmaBundle(outputPath, bundle) {
  if (!path.isAbsolute(outputPath)) {
    throw new TypeError("Figma bundle output requires an absolute path outside the repository.");
  }
  await assertOutsideRepository(outputPath, designSystemPaths.repositoryRoot);
  return writeIfChanged(outputPath, `${JSON.stringify(bundle, null, 2)}\n`);
}

function assertThemePair(light, dark) {
  if (light?.theme !== "light" || dark?.theme !== "dark") {
    throw new TypeError("Figma generation requires assembled light and dark themes.");
  }
  const lightIds = [...light.tokens.keys()].sort();
  const darkIds = [...dark.tokens.keys()].sort();
  if (lightIds.length !== darkIds.length || lightIds.some((id, index) => id !== darkIds[index])) {
    throw new Error("Light and dark themes must expose identical token paths.");
  }
}

function collection(name, variables) {
  return { name, variables: [...variables].sort(compareLogicalIds) };
}

function buildCoreVariables(theme) {
  const variables = [];
  for (const [logicalId, record] of theme.tokens) {
    if (isSemantic(logicalId)) {
      continue;
    }
    const type = effectiveType(record);
    if (isSimpleType(type)) {
      variables.push(createVariable(logicalId, type, rawValue(record), theme.tokens));
    }
  }
  return variables;
}

function buildSemanticColorVariables(theme) {
  const variables = [];
  for (const [logicalId, record] of theme.tokens) {
    if (!logicalId.startsWith("color.") || !isSemantic(logicalId)) {
      continue;
    }
    variables.push(
      createVariable(logicalId, effectiveType(record), rawValue(record), theme.tokens),
    );
  }
  return variables;
}

function buildLayoutVariables(theme, viewport) {
  const variables = [];
  for (const [logicalId, record] of theme.tokens) {
    if (!isSemantic(logicalId) || logicalId.startsWith("color.")) {
      continue;
    }
    const type = effectiveType(record);
    if (type === "typography") {
      for (const [property, suffix] of TYPOGRAPHY_PROPERTIES) {
        const propertyId = `${logicalId}.${suffix}`;
        const value = resolveCompositeProperty(theme.tokens, logicalId, property, viewport);
        variables.push(
          createVariable(propertyId, propertyType(property), value, theme.tokens),
        );
      }
    } else if (type === "transition") {
      for (const [property, suffix] of TRANSITION_PROPERTIES) {
        const propertyId = `${logicalId}.${suffix}`;
        const value = resolveCompositeProperty(theme.tokens, logicalId, property, viewport);
        variables.push(
          createVariable(propertyId, propertyType(property), value, theme.tokens),
        );
      }
    } else if (isSimpleType(type)) {
      variables.push(
        createVariable(
          logicalId,
          type,
          selectViewportReference(rawValue(record), theme.tokens, viewport),
          theme.tokens,
        ),
      );
    }
  }
  return variables;
}

function buildTextStyles(theme) {
  const styles = [];
  const typographyIds = [...theme.tokens]
    .filter(([, record]) => effectiveType(record) === "typography")
    .map(([logicalId]) => logicalId)
    .sort();

  for (const viewport of ["mobile", "tablet", "desktop"]) {
    for (const logicalId of typographyIds) {
      styles.push({
        logicalId,
        name: `${capitalize(viewport)}/${logicalToFigmaName(logicalId)}`,
        viewport,
        properties: Object.fromEntries(
          TYPOGRAPHY_PROPERTIES.map(([property, suffix]) => [
            property,
            `${logicalId}.${suffix}`,
          ]),
        ),
      });
    }
  }
  return styles;
}

function buildEvidence(light, dark) {
  const resolved = resolveReferences(light.document);
  const breakpointIds = [...resolved.keys()]
    .filter((logicalId) => logicalId.startsWith("responsive.breakpoint."))
    .sort(
      (left, right) =>
        resolved.get(left).value.value - resolved.get(right).value.value ||
        left.localeCompare(right, "en"),
    );
  const statusRoles = new Set();
  for (const logicalId of light.tokens.keys()) {
    if (logicalId.startsWith("color.status.")) {
      statusRoles.add(logicalId.split(".")[2]);
    }
  }
  const elevationLevels = ["high", "low", "medium"].filter((level) =>
    light.tokens.has(`shadow.${level}`),
  );

  return {
    themeParity: {
      logicalTokenCount: light.tokens.size,
      lightFingerprint: light.fingerprint,
      darkFingerprint: dark.fingerprint,
    },
    responsive: {
      breakpoints: breakpointIds.map((logicalId) => {
        const value = resolved.get(logicalId).value;
        return { logicalId, value: value.value, unit: value.unit };
      }),
    },
    status: { roles: [...statusRoles].sort() },
    elevation: { levels: elevationLevels },
    typography: {
      viewports: ["mobile", "tablet", "desktop"],
      styleCount: [...light.tokens.values()].filter(
        (record) => effectiveType(record) === "typography",
      ).length,
    },
  };
}

function createVariable(logicalId, tokenType, value, tokens) {
  const alias = referenceTarget(value);
  const variable = {
    logicalId,
    name: logicalToFigmaName(logicalId),
    type: figmaType(tokenType, value, tokens),
    description: `Canonical token: ${logicalId}`,
  };
  if (alias) {
    variable.alias = alias;
  } else {
    variable.value = figmaValue(tokenType, value);
  }
  return variable;
}

function resolveCompositeProperty(tokens, logicalId, property, viewport, stack = []) {
  if (stack.includes(logicalId)) {
    throw new Error(`Composite reference cycle: ${[...stack, logicalId].join(" -> ")}.`);
  }
  const record = tokens.get(logicalId);
  if (!record) {
    throw new ReferenceError(`Unresolved composite token ${logicalId}.`);
  }
  const value = rawValue(record);
  const reference = referenceTarget(value);
  if (reference) {
    return resolveCompositeProperty(tokens, reference, property, viewport, [...stack, logicalId]);
  }
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new TypeError(`Composite token ${logicalId} has no ${property} property.`);
  }
  return selectViewportReference(value[property], tokens, viewport);
}

function selectViewportReference(value, tokens, viewport) {
  const reference = referenceTarget(value);
  if (!reference || viewport !== "mobile" || !reference.endsWith(".desktop")) {
    return value;
  }
  const mobileReference = reference.replace(/\.desktop$/u, ".mobile");
  return tokens.has(mobileReference) ? `{${mobileReference}}` : value;
}

function figmaType(tokenType, value, tokens) {
  if (tokenType === "color") {
    return "COLOR";
  }
  if (tokenType === "fontFamily" || tokenType === "cubicBezier" || tokenType === "strokeStyle") {
    return "STRING";
  }
  if (tokenType === "fontWeight") {
    return resolveFontWeightType(value, tokens) === "string" ? "STRING" : "FLOAT";
  }
  return "FLOAT";
}

function resolveFontWeightType(value, tokens, stack = []) {
  const reference = referenceTarget(value);
  if (!reference) {
    return typeof value;
  }
  if (stack.includes(reference)) {
    throw new Error(`Font-weight reference cycle: ${[...stack, reference].join(" -> ")}.`);
  }
  const target = tokens.get(reference);
  if (!target) {
    throw new ReferenceError(`Unresolved font-weight reference ${reference}.`);
  }
  return resolveFontWeightType(rawValue(target), tokens, [...stack, reference]);
}

function figmaValue(tokenType, value) {
  if (tokenType === "color") {
    const [r, g, b] = value.components;
    return { r, g, b, a: value.alpha ?? 1 };
  }
  if (tokenType === "dimension" || tokenType === "duration") {
    return value.value;
  }
  if (tokenType === "fontFamily") {
    return Array.isArray(value) ? value[0] : value;
  }
  if (tokenType === "cubicBezier") {
    return value.join(", ");
  }
  if (tokenType === "strokeStyle" && typeof value !== "string") {
    return JSON.stringify(value);
  }
  return value;
}

function propertyType(property) {
  switch (property) {
    case "fontFamily":
      return "fontFamily";
    case "fontWeight":
      return "fontWeight";
    case "timingFunction":
      return "cubicBezier";
    case "lineHeight":
      return "number";
    case "fontSize":
    case "letterSpacing":
      return "dimension";
    case "duration":
    case "delay":
      return "duration";
    default:
      throw new TypeError(`Unsupported composite property ${property}.`);
  }
}

function effectiveType(record) {
  return record.token.$type ?? record.inheritedType;
}

function rawValue(record) {
  return Object.hasOwn(record.token, "$ref")
    ? { $ref: record.token.$ref }
    : record.token.$value;
}

function referenceTarget(value) {
  if (typeof value === "string") {
    return value.match(/^\{([^{}]+)\}$/u)?.[1];
  }
  if (value && typeof value === "object" && typeof value.$ref === "string") {
    if (!value.$ref.startsWith("#/")) {
      throw new TypeError(`Unsupported token pointer ${value.$ref}.`);
    }
    const segments = value.$ref
      .slice(2)
      .split("/")
      .map((segment) => segment.replaceAll("~1", "/").replaceAll("~0", "~"));
    const valueIndex = segments.indexOf("$value");
    return segments.slice(0, valueIndex === -1 ? segments.length : valueIndex).join(".");
  }
  return undefined;
}

function isSemantic(logicalId) {
  return SEMANTIC_PREFIXES.some((prefix) => logicalId.startsWith(prefix));
}

function isSimpleType(type) {
  return [
    "color",
    "number",
    "dimension",
    "duration",
    "fontFamily",
    "fontWeight",
    "cubicBezier",
    "strokeStyle",
  ].includes(type);
}

function fingerprintSources(light, dark) {
  return `sha256:${createHash("sha256")
    .update(JSON.stringify([light.fingerprint, dark.fingerprint]))
    .digest("hex")}`;
}

function compareLogicalIds(left, right) {
  return left.logicalId.localeCompare(right.logicalId, "en");
}

function capitalize(value) {
  return `${value[0].toUpperCase()}${value.slice(1)}`;
}
