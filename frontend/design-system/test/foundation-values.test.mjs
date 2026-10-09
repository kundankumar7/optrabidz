import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

import Ajv from "ajv";

const testRoot = path.dirname(fileURLToPath(import.meta.url));
const tokenRoot = path.resolve(testRoot, "..", "tokens");

const palette = {
  "000": "#FFFDF8",
  "025": "#FAF7F1",
  "050": "#F4F0E8",
  "100": "#EAE4DA",
  "150": "#D5D1C8",
  "200": "#CBC7BE",
  "300": "#B2B3AC",
  "400": "#92978F",
  "500": "#767971",
  "525": "#727870",
  "600": "#62665F",
  "700": "#4C4F49",
  "750": "#343931",
  "800": "#262724",
  "825": "#222620",
  "850": "#1B1E1A",
  "875": "#1A1C18",
  "900": "#151714",
  "950": "#121410",
  "1000": "#10120F",
};

const typography = {
  "display.xl": { desktop: [96, 0.916667, -5.952], mobile: [54, 1.037037, -3.348] },
  "display.lg": { desktop: [72, 0.944444, -3.96], mobile: [44, 1.068182, -2.42] },
  "heading.lg": { desktop: [44, 1.090909, -1.98], mobile: [34, 1.147059, -1.53] },
  "heading.md": { desktop: [32, 1.1875, -1.12], mobile: [28, 1.214286, -0.98] },
  "heading.sm": { desktop: [24, 1.25, -0.6], mobile: [21, 1.285714, -0.525] },
  "body.lg": { desktop: [18, 1.611111, 0], mobile: [18, 1.611111, 0] },
  "body.md": { desktop: [16, 1.625, 0], mobile: [16, 1.625, 0] },
  "body.sm": { desktop: [14, 1.571429, 0], mobile: [14, 1.571429, 0] },
  label: { desktop: [12, 1.333333, 0.96], mobile: [12, 1.333333, 0.96] },
  "number.lg": { desktop: [36, 1.111111, -1.26], mobile: [34, 1.117647, -1.19] },
};

test("preserves every approved warm-neutral color as a DTCG sRGB object", async () => {
  const primitives = await readTokenFile("primitives.tokens.json");
  const warm = primitives.color.neutral.warm;

  assert.equal(warm.$type, "color");
  assert.deepEqual(Object.keys(warm).filter((key) => !key.startsWith("$")), Object.keys(palette));

  for (const [step, hex] of Object.entries(palette)) {
    const value = warm[step].$value;
    assert.equal(value.colorSpace, "srgb", step);
    assert.equal(value.hex, hex, step);
    assert.equal(value.alpha, 1, step);
    assert.deepEqual(value.components, hexComponents(hex), step);
  }
});

test("pins approved font families, fallbacks, weights, and responsive numeric values", async () => {
  const primitives = await readTokenFile("primitives.tokens.json");

  assert.deepEqual(tokenValue(primitives, "font.family.display"), [
    "Cabinet Grotesk",
    "Arial",
    "sans-serif",
  ]);
  assert.deepEqual(tokenValue(primitives, "font.family.product"), [
    "General Sans",
    "Arial",
    "sans-serif",
  ]);
  assert.deepEqual(
    {
      regular: tokenValue(primitives, "font.weight.regular"),
      medium: tokenValue(primitives, "font.weight.medium"),
      semibold: tokenValue(primitives, "font.weight.semibold"),
      bold: tokenValue(primitives, "font.weight.bold"),
    },
    { regular: 400, medium: 500, semibold: 600, bold: 700 },
  );

  for (const [role, modes] of Object.entries(typography)) {
    for (const [mode, [size, lineHeight, letterSpacing]] of Object.entries(modes)) {
      assert.deepEqual(tokenValue(primitives, `font.size.${role}.${mode}`), {
        value: size,
        unit: "px",
      });
      assert.equal(tokenValue(primitives, `font.line-height.${role}.${mode}`), lineHeight);
      assert.deepEqual(tokenValue(primitives, `font.letter-spacing.${role}.${mode}`), {
        value: letterSpacing,
        unit: "px",
      });
    }
  }
});

test("defines semantic typography without duplicating approved primitive values", async () => {
  const semantic = await readTokenFile("semantic.tokens.json");

  for (const role of Object.keys(typography)) {
    const displayRole = role.startsWith("display.") || role.startsWith("heading.");
    const weight = displayRole ? "medium" : role === "label" ? "medium" : "regular";
    assert.deepEqual(tokenValue(semantic, `typography.${role}`), {
      fontFamily: `{font.family.${displayRole ? "display" : "product"}}`,
      fontSize: `{font.size.${role}.desktop}`,
      fontWeight: `{font.weight.${weight}}`,
      letterSpacing: `{font.letter-spacing.${role}.desktop}`,
      lineHeight: `{font.line-height.${role}.desktop}`,
    });
  }

  assert.equal(tokenValue(semantic, "typography.title"), "{typography.heading.sm}");
  assert.equal(tokenValue(semantic, "typography.caption"), "{typography.body.sm}");
});

test("pins the shared geometry, icon, motion, target, shadow, and layer scales", async () => {
  const [primitives, semantic] = await Promise.all([
    readTokenFile("primitives.tokens.json"),
    readTokenFile("semantic.tokens.json"),
  ]);

  for (const value of [0, 4, 8, 12, 16, 24, 32, 48, 64, 96]) {
    assert.deepEqual(tokenValue(primitives, `space.${value}`), { value, unit: "px" });
  }
  for (const value of [0, 8, 12, 16, 24, 999]) {
    assert.deepEqual(tokenValue(primitives, `radius.${value}`), { value, unit: "px" });
  }
  for (const value of [1, 2, 3]) {
    assert.deepEqual(tokenValue(primitives, `border.width.${value}`), { value, unit: "px" });
  }

  assert.deepEqual(tokenValue(primitives, "icon.size.sm"), { value: 20, unit: "px" });
  assert.deepEqual(tokenValue(primitives, "icon.size.md"), { value: 24, unit: "px" });
  assert.equal(tokenValue(primitives, "icon.stroke.regular"), 1.75);
  assert.deepEqual(tokenValue(primitives, "motion.duration.fast"), { value: 120, unit: "ms" });
  assert.deepEqual(tokenValue(primitives, "motion.duration.base"), { value: 180, unit: "ms" });
  assert.deepEqual(tokenValue(primitives, "motion.duration.slow"), { value: 280, unit: "ms" });
  assert.deepEqual(tokenValue(primitives, "motion.easing.standard"), [0.2, 0, 0, 1]);
  assert.deepEqual(tokenValue(primitives, "size.target.minimum"), { value: 24, unit: "px" });
  assert.deepEqual(tokenValue(primitives, "size.target.primary"), { value: 44, unit: "px" });

  assert.equal(tokenValue(semantic, "focus.ring.width"), "{focus.width}");
  assert.equal(tokenValue(semantic, "focus.ring.offset"), "{focus.offset}");
  assert.deepEqual(
    Object.fromEntries(
      Object.entries({ base: 0, raised: 10, sticky: 100, dropdown: 300, overlay: 500, dialog: 600, notification: 700, blocking: 800 })
        .map(([name, value]) => [name, tokenValue(semantic, `layer.${name}`)]),
    ),
    { base: 0, raised: 10, sticky: 100, dropdown: 300, overlay: 500, dialog: 600, notification: 700, blocking: 800 },
  );

  assert.deepEqual(tokenValue(semantic, "shadow.low"), shadowValue("low"));
  assert.deepEqual(tokenValue(semantic, "shadow.medium"), shadowValue("medium"));
  assert.deepEqual(tokenValue(semantic, "shadow.high"), shadowValue("high"));
});

test("defines complete and symmetric semantic light and dark color overlays", async () => {
  const [semantic, light, dark] = await Promise.all([
    readTokenFile("semantic.tokens.json"),
    readTokenFile(path.join("themes", "light.tokens.json")),
    readTokenFile(path.join("themes", "dark.tokens.json")),
  ]);
  const semanticColors = colorTokenValues(semantic.color);
  const lightColors = colorTokenValues(light.color);
  const darkColors = colorTokenValues(dark.color);

  assert.deepEqual(Object.keys(lightColors), Object.keys(semanticColors));
  assert.deepEqual(Object.keys(darkColors), Object.keys(semanticColors));
  assert.notDeepEqual(lightColors, darkColors);

  for (const theme of [lightColors, darkColors]) {
    assert.equal(theme["status.error.background"], "{color.surface.inverse}");
    assert.equal(theme["status.error.foreground"], "{color.text.inverse}");
    assert.equal(theme["status.error.border"], "{color.border.inverse}");
    assert.equal(theme["status.error.icon"], "{color.icon.inverse}");
    assert.equal(theme["status.overdue.background"], "{color.surface.inverse}");
    assert.equal(theme["status.overdue.foreground"], "{color.text.inverse}");
    assert.equal(theme["status.overdue.border"], "{color.border.inverse}");
    assert.equal(theme["status.overdue.icon"], "{color.icon.inverse}");
  }
});

test("defines accessible native scrollbar colors without forcing scrollbar width", async () => {
  const [semantic, light, dark, accessibility] = await Promise.all([
    readTokenFile("semantic.tokens.json"),
    readTokenFile(path.join("themes", "light.tokens.json")),
    readTokenFile(path.join("themes", "dark.tokens.json")),
    readContract("accessibility.contract.json"),
  ]);

  assert.equal(tokenValue(semantic, "color.scrollbar.thumb"), "{color.neutral.warm.500}");
  assert.equal(tokenValue(semantic, "color.scrollbar.track"), "{color.neutral.warm.500}");
  assert.equal(tokenValue(light, "color.scrollbar.thumb"), "{color.neutral.warm.500}");
  assert.equal(tokenValue(light, "color.scrollbar.track"), "{color.surface.canvas}");
  assert.equal(tokenValue(dark, "color.scrollbar.thumb"), "{color.neutral.warm.600}");
  assert.equal(tokenValue(dark, "color.scrollbar.track"), "{color.surface.canvas}");
  assert.deepEqual(
    accessibility.contrastPairs.find(({ id }) => id === "scrollbar-thumb-on-track"),
    {
      id: "scrollbar-thumb-on-track",
      foreground: "color.scrollbar.thumb",
      background: "color.scrollbar.track",
      category: "nonText",
      minimum: 3,
      themes: ["light", "dark"],
    },
  );
});

test("defines machine-readable accessibility, status, and elevation contracts", async () => {
  const [accessibility, status, elevation] = await Promise.all([
    readContract("accessibility.contract.json"),
    readContract("status.contract.json"),
    readContract("elevation.contract.json"),
  ]);

  assert.equal(accessibility.schemaVersion, 1);
  assert.ok(accessibility.contrastPairs.length >= 10);
  assert.equal(new Set(accessibility.contrastPairs.map(({ id }) => id)).size, accessibility.contrastPairs.length);
  for (const pair of accessibility.contrastPairs) {
    assert.deepEqual(pair.themes, ["light", "dark"]);
    assert.equal(pair.minimum, pair.category === "normalText" ? 4.5 : 3);
  }

  assert.deepEqual(Object.keys(status.statuses), [
    "success", "warning", "error", "information", "pending", "overdue", "completed", "neutral",
  ]);
  for (const [name, contract] of Object.entries(status.statuses)) {
    assert.equal(contract.labelRequired, true, name);
    assert.equal(contract.nonColorCue, "iconOrText", name);
    assert.deepEqual(contract.tokens, {
      background: `color.status.${name}.background`,
      foreground: `color.status.${name}.foreground`,
      border: `color.status.${name}.border`,
      icon: `color.status.${name}.icon`,
    });
  }

  assert.deepEqual(elevation.levels, {
    none: { surface: "color.surface.default", border: "color.border.default", shadow: null },
    low: { surface: "color.surface.raised", border: "color.border.subtle", shadow: "shadow.low" },
    medium: { surface: "color.surface.raised", border: "color.border.default", shadow: "shadow.medium" },
    high: { surface: "color.surface.overlay", border: "color.border.strong", shadow: "shadow.high" },
  });
});

test("pins content-driven responsive thresholds and complete boundary evidence", async () => {
  const [semantic, contract, schema] = await Promise.all([
    readTokenFile("semantic.tokens.json"),
    readContract("responsive.contract.json"),
    readSchema("responsive-contract.schema.json"),
  ]);
  const validate = new Ajv({ allErrors: true, strict: false }).compile(schema);

  assert.equal(validate(contract), true, JSON.stringify(validate.errors));
  assert.deepEqual(contract.evidence.widths, [
    320, 360, 390, 480, 600, 679, 680, 681, 768, 900,
    919, 920, 921, 959, 960, 961, 1024, 1280, 1440,
  ]);
  assert.deepEqual(contract.evidence.themes, ["light", "dark"]);
  assert.deepEqual(
    contract.breakpoints.map(({ id, token, threshold, boundaryEvidence }) => ({
      id,
      token,
      threshold,
      boundaryEvidence,
    })),
    [
      {
        id: "compact",
        token: "responsive.breakpoint.compact",
        threshold: 680,
        boundaryEvidence: { below: 679, exact: 680, above: 681 },
      },
      {
        id: "comfortable",
        token: "responsive.breakpoint.comfortable",
        threshold: 920,
        boundaryEvidence: { below: 919, exact: 920, above: 921 },
      },
      {
        id: "wide",
        token: "responsive.breakpoint.wide",
        threshold: 960,
        boundaryEvidence: { below: 959, exact: 960, above: 961 },
      },
    ],
  );
  assert.deepEqual(tokenValue(semantic, "responsive.breakpoint.compact"), { value: 680, unit: "px" });
  assert.deepEqual(tokenValue(semantic, "responsive.breakpoint.comfortable"), { value: 920, unit: "px" });
  assert.deepEqual(tokenValue(semantic, "responsive.breakpoint.wide"), { value: 960, unit: "px" });
});

async function readTokenFile(fileName) {
  return JSON.parse(await readFile(path.join(tokenRoot, fileName), "utf8"));
}

async function readContract(fileName) {
  return JSON.parse(await readFile(path.resolve(tokenRoot, "..", "contracts", fileName), "utf8"));
}

async function readSchema(fileName) {
  return JSON.parse(await readFile(path.resolve(tokenRoot, "..", "schema", fileName), "utf8"));
}

function tokenValue(document, logicalId) {
  let current = document;
  for (const segment of logicalId.split(".")) {
    current = current?.[segment];
  }
  assert.ok(current && Object.hasOwn(current, "$value"), `Missing ${logicalId}`);
  return current.$value;
}

function hexComponents(hex) {
  return [1, 3, 5].map((index) => Number.parseInt(hex.slice(index, index + 2), 16) / 255);
}

function shadowValue(level) {
  return {
    color: "{color.shadow.default}",
    offsetX: "{shadow.geometry.offset.x}",
    offsetY: `{shadow.geometry.${level}.y}`,
    blur: `{shadow.geometry.${level}.blur}`,
    spread: `{shadow.geometry.${level}.spread}`,
  };
}

function colorTokenValues(group, prefix = "") {
  const values = {};
  for (const [key, value] of Object.entries(group)) {
    if (key.startsWith("$")) continue;
    const logicalId = prefix ? `${prefix}.${key}` : key;
    if (Object.hasOwn(value, "$value")) {
      values[logicalId] = value.$value;
    } else {
      Object.assign(values, colorTokenValues(value, logicalId));
    }
  }
  return values;
}
