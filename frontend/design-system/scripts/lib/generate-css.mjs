import { resolveReferences } from "./resolve-references.mjs";
import { logicalToCssName } from "./token-paths.mjs";

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

export function generateCss(light, dark) {
  assertThemePair(light, dark);

  const lightDeclarations = buildThemeDeclarations(light);
  const darkDeclarations = buildThemeDeclarations(dark);
  const lightOverrides = selectDifferences(lightDeclarations, darkDeclarations);
  const darkOverrides = selectDifferences(darkDeclarations, lightDeclarations);
  const responsiveDeclarations = buildResponsiveTypographyDeclarations(light);
  const reducedMotionDeclarations = buildReducedMotionDeclarations(light);
  const compactBreakpoint = formatCssValue(
    resolveReferences(light.document).get("responsive.breakpoint.compact"),
  );

  const sections = [
    "/* Generated file. Do not edit directly; update design-system token sources. */",
    serializeRule(":root", lightDeclarations, { colorScheme: "light" }),
    serializeMedia(
      "(prefers-color-scheme: dark)",
      serializeRule(":root:not([data-theme])", darkOverrides, { colorScheme: "dark" }),
    ),
    serializeRule(':root[data-theme="light"]', lightOverrides, { colorScheme: "light" }),
    serializeRule(':root[data-theme="dark"]', darkOverrides, { colorScheme: "dark" }),
  ];

  if (responsiveDeclarations.size > 0) {
    sections.push(
      serializeMedia(
        `(max-width: ${compactBreakpoint})`,
        serializeRule(":root", responsiveDeclarations),
      ),
    );
  }

  if (reducedMotionDeclarations.size > 0) {
    sections.push(
      serializeMedia(
        "(prefers-reduced-motion: reduce)",
        serializeRule(":root", reducedMotionDeclarations),
      ),
    );
  }

  return `${sections.join("\n\n")}\n`;
}

function assertThemePair(light, dark) {
  if (light?.theme !== "light" || dark?.theme !== "dark") {
    throw new TypeError("CSS generation requires assembled light and dark themes.");
  }

  const lightIds = [...light.tokens.keys()].sort();
  const darkIds = [...dark.tokens.keys()].sort();
  if (lightIds.length !== darkIds.length || lightIds.some((id, index) => id !== darkIds[index])) {
    throw new Error("Light and dark themes must expose identical token paths.");
  }
}

function buildThemeDeclarations(theme) {
  const resolved = resolveReferences(theme.document);
  const declarations = new Map();

  for (const logicalId of [...resolved.keys()].sort()) {
    for (const [name, value] of formatToken(logicalId, resolved.get(logicalId))) {
      if (declarations.has(name)) {
        throw new Error(`Duplicate CSS custom property ${name}.`);
      }
      declarations.set(name, value);
    }
  }

  return sortMap(declarations);
}

function buildResponsiveTypographyDeclarations(theme) {
  const resolved = resolveReferences(theme.document);
  const desktop = buildThemeDeclarations(theme);
  const mobile = new Map();

  for (const [logicalId, record] of resolved) {
    if (record.type !== "typography") {
      continue;
    }
    const value = resolveModeValue(logicalId, theme.tokens, resolved, "mobile");
    for (const [name, cssValue] of formatToken(logicalId, { type: "typography", value })) {
      if (desktop.get(name) !== cssValue) {
        mobile.set(name, cssValue);
      }
    }
  }

  return sortMap(mobile);
}

function buildReducedMotionDeclarations(theme) {
  const resolved = resolveReferences(theme.document);
  const none = resolved.get("motion.duration.none");
  if (!none) {
    return new Map();
  }

  const declarations = new Map();
  for (const [logicalId, record] of resolved) {
    if (record.type === "transition") {
      declarations.set(`${logicalToCssName(logicalId)}-duration`, formatCssValue(none));
    }
  }
  return sortMap(declarations);
}

function resolveModeValue(logicalId, tokens, resolved, mode, stack = []) {
  if (stack.includes(logicalId)) {
    throw new Error(`Mode reference cycle: ${[...stack, logicalId].join(" -> ")}.`);
  }
  const record = tokens.get(logicalId);
  if (!record) {
    throw new ReferenceError(`Unresolved token reference ${logicalId}.`);
  }
  const rawValue = Object.hasOwn(record.token, "$ref") ? { $ref: record.token.$ref } : record.token.$value;
  return resolveModeFragment(rawValue, tokens, resolved, mode, [...stack, logicalId]);
}

function resolveModeFragment(value, tokens, resolved, mode, stack) {
  const reference = referenceTarget(value);
  if (reference) {
    const modeReference =
      mode === "mobile" && reference.endsWith(".desktop")
        ? reference.replace(/\.desktop$/u, ".mobile")
        : reference;
    const targetId = tokens.has(modeReference) ? modeReference : reference;
    const target = resolved.get(targetId);
    if (!target) {
      throw new ReferenceError(`Unresolved token reference ${targetId}.`);
    }
    return target.type === "typography"
      ? resolveModeValue(targetId, tokens, resolved, mode, stack)
      : structuredClone(target.value);
  }
  if (Array.isArray(value)) {
    return value.map((item) => resolveModeFragment(item, tokens, resolved, mode, stack));
  }
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [
        key,
        resolveModeFragment(item, tokens, resolved, mode, stack),
      ]),
    );
  }
  return value;
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

function formatToken(logicalId, record) {
  const name = logicalToCssName(logicalId);
  if (record.type === "typography") {
    return TYPOGRAPHY_PROPERTIES.map(([property, suffix]) => [
      `${name}-${suffix}`,
      formatCompositeValue(record.value[property], property),
    ]);
  }
  if (record.type === "strokeStyle" && typeof record.value !== "string") {
    return formatStrokeStyle(name, record.value);
  }
  if (record.type === "border") {
    return formatBorder(name, record.value);
  }
  if (record.type === "transition") {
    const entries = TRANSITION_PROPERTIES.map(([property, suffix]) => [
      `${name}-${suffix}`,
      formatCompositeValue(record.value[property], property),
    ]);
    entries.push([
      name,
      `var(${name}-duration) var(${name}-timing-function) var(${name}-delay)`,
    ]);
    return entries;
  }
  return [[name, formatCssValue(record)]];
}

function formatCssValue(record) {
  if (!record || typeof record.type !== "string") {
    throw new TypeError("Resolved token values require an effective type.");
  }
  const { type, value } = record;
  switch (type) {
    case "color":
      return formatColor(value);
    case "fontFamily":
      return formatFontFamilyValue(value);
    case "fontWeight":
      return typeof value === "string" ? value : formatNumber(value);
    case "number":
      return formatNumber(value);
    case "dimension":
    case "duration":
      return formatDimension(value);
    case "cubicBezier":
      return `cubic-bezier(${value.map(formatNumber).join(", ")})`;
    case "strokeStyle":
      return value;
    case "shadow":
      return (Array.isArray(value) ? value : [value]).map(formatShadow).join(", ");
    case "gradient":
      return value.map(formatGradientStop).join(", ");
    case "border":
    case "typography":
    case "transition":
      throw new TypeError(`${type} values must be decomposed into CSS properties.`);
    default:
      throw new TypeError(`Unsupported CSS token type ${type}.`);
  }
}

function formatCompositeValue(value, property) {
  switch (property) {
    case "fontFamily":
      return formatFontFamilyValue(value);
    case "fontSize":
    case "letterSpacing":
    case "duration":
    case "delay":
      return formatDimension(value);
    case "fontWeight":
      return typeof value === "string" ? value : formatNumber(value);
    case "lineHeight":
      return formatNumber(value);
    case "timingFunction":
      return `cubic-bezier(${value.map(formatNumber).join(", ")})`;
    default:
      throw new TypeError(`Unsupported composite property ${property}.`);
  }
}

function formatStrokeStyle(name, value) {
  return [
    [`${name}-dash-array`, value.dashArray.map(formatDimension).join(" ")],
    [`${name}-line-cap`, value.lineCap],
  ];
}

function formatBorder(name, value) {
  const color = formatColor(value.color);
  const width = formatDimension(value.width);
  const entries = [
    [`${name}-color`, color],
    [`${name}-width`, width],
  ];

  if (typeof value.style === "string") {
    entries.push([`${name}-style`, value.style], [name, `${width} ${value.style} ${color}`]);
  } else {
    entries.push(...formatStrokeStyle(name, value.style));
  }
  return entries;
}

function formatShadow(value) {
  const parts = [value.offsetX, value.offsetY, value.blur, value.spread]
    .map(formatDimension)
    .concat(formatColor(value.color));
  if (value.inset) {
    parts.unshift("inset");
  }
  return parts.join(" ");
}

function formatGradientStop(value) {
  return `${formatColor(value.color)} ${formatNumber(value.position * 100)}%`;
}

function formatColor(value) {
  if (!value || value.colorSpace !== "srgb" || !Array.isArray(value.components)) {
    throw new TypeError("CSS color output requires a resolved DTCG sRGB color.");
  }
  if ((value.alpha ?? 1) === 1 && typeof value.hex === "string") {
    return value.hex.toUpperCase();
  }
  const channels = value.components.map((component) => Math.round(component * 255));
  return `rgb(${channels.join(" ")} / ${formatNumber(value.alpha ?? 1)})`;
}

function formatDimension(value) {
  if (!value || typeof value.value !== "number" || typeof value.unit !== "string") {
    throw new TypeError("CSS dimension output requires a numeric value and unit.");
  }
  return `${formatNumber(value.value)}${value.unit}`;
}

function formatFontFamilyValue(value) {
  const families = Array.isArray(value) ? value : [value];
  return families.map(formatFontFamily).join(", ");
}

function formatFontFamily(value) {
  return /\s/u.test(value) ? JSON.stringify(value) : value;
}

function formatNumber(value) {
  if (!Number.isFinite(value)) {
    throw new TypeError("CSS numeric output requires a finite number.");
  }
  return String(Object.is(value, -0) ? 0 : value);
}

function selectDifferences(source, comparison) {
  return sortMap(
    new Map([...source].filter(([name, value]) => comparison.get(name) !== value)),
  );
}

function serializeRule(selector, declarations, options = {}) {
  const lines = [];
  if (options.colorScheme) {
    lines.push(`  color-scheme: ${options.colorScheme};`);
  }
  for (const [name, value] of declarations) {
    lines.push(`  ${name}: ${value};`);
  }
  return `${selector} {\n${lines.join("\n")}\n}`;
}

function serializeMedia(condition, rule) {
  return `@media ${condition} {\n${rule
    .split("\n")
    .map((line) => `  ${line}`)
    .join("\n")}\n}`;
}

function sortMap(values) {
  return new Map([...values].sort(([left], [right]) => left.localeCompare(right, "en")));
}
