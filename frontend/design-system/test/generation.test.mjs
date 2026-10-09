import assert from "node:assert/strict";
import { cp, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { after, test } from "node:test";

import { flattenTokenPaths } from "../scripts/lib/token-paths.mjs";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));
const designSystemRoot = path.resolve(testDirectory, "..");
const tokenRoot = path.join(designSystemRoot, "tokens");
const temporaryRoots = [];

after(async () => {
  await Promise.all(
    temporaryRoots.map((directory) => rm(directory, { recursive: true, force: true })),
  );
});

async function createTemporaryRoot() {
  const root = await mkdtemp(path.join(tmpdir(), "ob-generation-"));
  temporaryRoots.push(root);
  return root;
}

async function loadGenerationUtilities() {
  const [assembly, css, output, generator] = await Promise.all([
    import("../scripts/lib/assemble-theme.mjs"),
    import("../scripts/lib/generate-css.mjs"),
    import("../scripts/lib/serialize-output.mjs"),
    import("../scripts/generate.mjs"),
  ]);
  return { ...assembly, ...css, ...output, ...generator };
}

async function generateActualCss() {
  const { assembleTheme, generateCss } = await loadGenerationUtilities();
  const [light, dark] = await Promise.all([
    assembleTheme(tokenRoot, "light"),
    assembleTheme(tokenRoot, "dark"),
  ]);
  return generateCss(light, dark);
}

function withAdditionalTokens(theme, additions) {
  const document = structuredClone(theme.document);
  document.fixture = additions;
  return { ...theme, document, tokens: flattenTokenPaths(document) };
}

function color(hex, components, alpha = 1) {
  return { colorSpace: "srgb", components, alpha, hex };
}

test("emits deterministic theme selectors whose explicit choices override the system fallback", async () => {
  const css = await generateActualCss();
  const systemIndex = css.indexOf("@media (prefers-color-scheme: dark)");
  const explicitLightIndex = css.indexOf(':root[data-theme="light"]');
  const explicitDarkIndex = css.indexOf(':root[data-theme="dark"]');

  assert.match(css, /^\/\* Generated file/u);
  assert.match(css, /:root \{/u);
  assert.match(css, /:root:not\(\[data-theme\]\)/u);
  assert.ok(systemIndex > 0);
  assert.ok(explicitLightIndex > systemIndex);
  assert.ok(explicitDarkIndex > explicitLightIndex);
  assert.match(css, /--ob-color-surface-canvas: #F4F0E8;/u);
  assert.match(css, /--ob-color-scrollbar-thumb: #767971;/u);

  const rootBlock = css.slice(css.indexOf(":root {"), css.indexOf("}\n", css.indexOf(":root {")));
  const propertyNames = [...rootBlock.matchAll(/\s+(--ob-[a-z0-9-]+):/gu)].map((match) => match[1]);
  assert.deepEqual(propertyNames, [...propertyNames].sort());
});

test("converts typography, responsive values, and reduced motion into usable CSS variables", async () => {
  const css = await generateActualCss();

  assert.match(
    css,
    /--ob-typography-display-xl-font-family: "Cabinet Grotesk", Arial, sans-serif;/u,
  );
  assert.match(css, /--ob-typography-display-xl-font-size: 96px;/u);
  assert.match(css, /--ob-typography-display-xl-line-height: 0\.916667;/u);
  assert.match(css, /@media \(max-width: 680px\)[\s\S]*--ob-typography-display-xl-font-size: 54px;/u);
  assert.match(css, /@media \(prefers-reduced-motion: reduce\)[\s\S]*--ob-motion-feedback-duration: 0ms;/u);
});

test("returns byte-identical CSS and keeps generation in memory", async () => {
  const outputRoot = await createTemporaryRoot();
  const cssOutputPath = path.join(outputRoot, "tokens.css");
  const { generateArtifacts } = await loadGenerationUtilities();

  const first = await generateArtifacts({ tokenRoot, cssOutputPath });
  const second = await generateArtifacts({ tokenRoot, cssOutputPath });

  assert.equal(first.css, second.css);
  assert.equal(first.files.get(cssOutputPath), first.css);
  await assert.rejects(readFile(cssOutputPath, "utf8"), { code: "ENOENT" });
});

test("uses the manifest CSS output when no explicit output override is supplied", async () => {
  const root = await createTemporaryRoot();
  const temporaryTokenRoot = path.join(root, "tokens");
  await cp(tokenRoot, temporaryTokenRoot, { recursive: true });

  const manifestPath = path.join(temporaryTokenRoot, "manifest.tokens.json");
  const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
  manifest.outputs.css = "../generated/manifest-tokens.css";
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");

  const { generateArtifacts } = await loadGenerationUtilities();
  const artifacts = await generateArtifacts({ tokenRoot: temporaryTokenRoot });
  const expectedPath = path.resolve(temporaryTokenRoot, manifest.outputs.css);

  assert.deepEqual([...artifacts.files.keys()], [expectedPath]);
});

test("rejects a manifest CSS output outside the frontend root", async () => {
  const root = await createTemporaryRoot();
  const temporaryTokenRoot = path.join(root, "design-system", "tokens");
  await cp(tokenRoot, temporaryTokenRoot, { recursive: true });

  const manifestPath = path.join(temporaryTokenRoot, "manifest.tokens.json");
  const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
  manifest.outputs.css = "../../../outside.css";
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");

  const { generateArtifacts } = await loadGenerationUtilities();
  await assert.rejects(
    generateArtifacts({ tokenRoot: temporaryTokenRoot }),
    /manifest output escapes the frontend root/i,
  );
});

test("writes only changed bytes and rejects stale generated files without modifying them", async () => {
  const outputRoot = await createTemporaryRoot();
  const outputPath = path.join(outputRoot, "nested", "tokens.css");
  const { verifyGeneratedFile, writeIfChanged } = await loadGenerationUtilities();

  assert.equal(await writeIfChanged(outputPath, "first\n"), "created");
  assert.equal(await writeIfChanged(outputPath, "first\n"), "unchanged");
  assert.equal(await writeIfChanged(outputPath, "second\n"), "updated");
  await verifyGeneratedFile(outputPath, "second\n");

  await writeFile(outputPath, "stale\n", "utf8");
  await assert.rejects(
    verifyGeneratedFile(outputPath, "second\n"),
    /generated file is stale.*tokens\.css/i,
  );
  assert.equal(await readFile(outputPath, "utf8"), "stale\n");
});

test("the drift check fails without rewriting stale output", async () => {
  const outputRoot = await createTemporaryRoot();
  const cssOutputPath = path.join(outputRoot, "tokens.css");
  await writeFile(cssOutputPath, "stale\n", "utf8");

  const { checkGeneratedArtifacts } = await import("../scripts/check.mjs");
  await assert.rejects(
    checkGeneratedArtifacts({ tokenRoot, cssOutputPath }),
    /generated file is stale.*tokens\.css/i,
  );
  assert.equal(await readFile(cssOutputPath, "utf8"), "stale\n");
});

test("serializes every scalar and composite shape accepted by token validation", async () => {
  const { assembleTheme, generateCss } = await loadGenerationUtilities();
  const [baseLight, baseDark] = await Promise.all([
    assembleTheme(tokenRoot, "light"),
    assembleTheme(tokenRoot, "dark"),
  ]);
  const additions = {
    "single-family": { $type: "fontFamily", $value: "General Sans" },
    "named-weight": { $type: "fontWeight", $value: "bold" },
    "dashed-stroke": {
      $type: "strokeStyle",
      $value: {
        dashArray: [
          { value: 2, unit: "px" },
          { value: 4, unit: "px" },
        ],
        lineCap: "round",
      },
    },
    "solid-border": {
      $type: "border",
      $value: {
        color: color("#151714", [21 / 255, 23 / 255, 20 / 255]),
        width: { value: 1, unit: "px" },
        style: "solid",
      },
    },
    "dashed-border": {
      $type: "border",
      $value: {
        color: color("#151714", [21 / 255, 23 / 255, 20 / 255]),
        width: { value: 2, unit: "px" },
        style: {
          dashArray: [
            { value: 3, unit: "px" },
            { value: 5, unit: "px" },
          ],
          lineCap: "square",
        },
      },
    },
    shadows: {
      $type: "shadow",
      $value: [
        {
          color: color("#000000", [0, 0, 0], 0.4),
          offsetX: { value: 0, unit: "px" },
          offsetY: { value: 2, unit: "px" },
          blur: { value: 6, unit: "px" },
          spread: { value: -2, unit: "px" },
        },
        {
          color: color("#000000", [0, 0, 0], 0.2),
          offsetX: { value: 0, unit: "px" },
          offsetY: { value: 0, unit: "px" },
          blur: { value: 2, unit: "px" },
          spread: { value: 0, unit: "px" },
          inset: true,
        },
      ],
    },
    "gradient-stops": {
      $type: "gradient",
      $value: [
        { color: color("#151714", [21 / 255, 23 / 255, 20 / 255]), position: 0 },
        { color: color("#FFFDF8", [1, 253 / 255, 248 / 255]), position: 1 },
      ],
    },
    "single-family-typography": {
      $type: "typography",
      $value: {
        fontFamily: "General Sans",
        fontSize: { value: 16, unit: "px" },
        fontWeight: "bold",
        letterSpacing: { value: 0, unit: "px" },
        lineHeight: 1.5,
      },
    },
  };

  const css = generateCss(
    withAdditionalTokens(baseLight, additions),
    withAdditionalTokens(baseDark, additions),
  );

  assert.match(css, /--ob-fixture-single-family: "General Sans";/u);
  assert.match(css, /--ob-fixture-named-weight: bold;/u);
  assert.match(css, /--ob-fixture-dashed-stroke-dash-array: 2px 4px;/u);
  assert.match(css, /--ob-fixture-dashed-stroke-line-cap: round;/u);
  assert.match(css, /--ob-fixture-solid-border: 1px solid #151714;/u);
  assert.match(css, /--ob-fixture-dashed-border-width: 2px;/u);
  assert.match(css, /--ob-fixture-dashed-border-dash-array: 3px 5px;/u);
  assert.match(css, /--ob-fixture-dashed-border-line-cap: square;/u);
  assert.match(
    css,
    /--ob-fixture-shadows: 0px 2px 6px -2px rgb\(0 0 0 \/ 0\.4\), inset 0px 0px 2px 0px rgb\(0 0 0 \/ 0\.2\);/u,
  );
  assert.match(css, /--ob-fixture-gradient-stops: #151714 0%, #FFFDF8 100%;/u);
  assert.match(
    css,
    /--ob-fixture-single-family-typography-font-family: "General Sans";/u,
  );
  assert.match(css, /--ob-fixture-single-family-typography-font-weight: bold;/u);
});

test("generates a deterministic Figma bundle for the approved free-plan collections", async () => {
  const { assembleTheme } = await loadGenerationUtilities();
  const { generateFigmaBundle } = await import(
    "../scripts/lib/generate-figma-bundle.mjs"
  );
  const [light, dark] = await Promise.all([
    assembleTheme(tokenRoot, "light"),
    assembleTheme(tokenRoot, "dark"),
  ]);

  const first = generateFigmaBundle(light, dark);
  const second = generateFigmaBundle(light, dark);

  assert.deepEqual(first, second);
  assert.equal(first.schemaVersion, 1);
  assert.match(first.sourceFingerprint, /^sha256:[a-f0-9]{64}$/u);
  assert.deepEqual(
    first.collections.map((collection) => collection.name),
    [
      "Core",
      "Semantic / Light",
      "Semantic / Dark",
      "Layout / Mobile",
      "Layout / Tablet",
      "Layout / Desktop",
    ],
  );

  const core = first.collections[0];
  const lightSemantic = first.collections[1];
  const mobile = first.collections[3];
  const desktop = first.collections[5];
  const primitiveColor = core.variables.find(
    (variable) => variable.logicalId === "color.neutral.warm.900",
  );
  const displayFont = core.variables.find(
    (variable) => variable.logicalId === "font.family.display",
  );
  const productFont = core.variables.find(
    (variable) => variable.logicalId === "font.family.product",
  );
  const semanticColor = lightSemantic.variables.find(
    (variable) => variable.logicalId === "color.surface.canvas",
  );
  const mobileDisplaySize = mobile.variables.find(
    (variable) => variable.logicalId === "typography.display.xl.font-size",
  );
  const desktopDisplaySize = desktop.variables.find(
    (variable) => variable.logicalId === "typography.display.xl.font-size",
  );

  assert.equal(primitiveColor.name, "color/neutral/warm/900");
  assert.equal(primitiveColor.type, "COLOR");
  assert.deepEqual(Object.keys(primitiveColor.value).sort(), ["a", "b", "g", "r"]);
  assert.equal(displayFont.value, "Cabinet Grotesk");
  assert.equal(productFont.value, "General Sans");
  assert.equal(semanticColor.name, "color/surface/canvas");
  assert.equal(semanticColor.type, "COLOR");
  assert.equal(semanticColor.alias, "color.neutral.warm.050");
  assert.equal(mobileDisplaySize.name, "typography/display/xl/font-size");
  assert.equal(mobileDisplaySize.type, "FLOAT");
  assert.equal(mobileDisplaySize.alias, "font.size.display.xl.mobile");
  assert.equal(desktopDisplaySize.alias, "font.size.display.xl.desktop");

  const mobileDisplayStyle = first.textStyles.find(
    (style) =>
      style.logicalId === "typography.display.xl" && style.viewport === "mobile",
  );
  assert.equal(mobileDisplayStyle.name, "Mobile/typography/display/xl");
  assert.deepEqual(mobileDisplayStyle.properties, {
    fontFamily: "typography.display.xl.font-family",
    fontSize: "typography.display.xl.font-size",
    fontWeight: "typography.display.xl.font-weight",
    letterSpacing: "typography.display.xl.letter-spacing",
    lineHeight: "typography.display.xl.line-height",
  });
  assert.equal(first.textStyles.length, 36);

  assert.equal(first.evidence.themeParity.logicalTokenCount, light.tokens.size);
  assert.deepEqual(
    first.evidence.responsive.breakpoints.map((breakpoint) => breakpoint.logicalId),
    [
      "responsive.breakpoint.compact",
      "responsive.breakpoint.comfortable",
      "responsive.breakpoint.wide",
    ],
  );
  assert.deepEqual(first.evidence.status.roles, [
    "completed",
    "error",
    "information",
    "neutral",
    "overdue",
    "pending",
    "success",
    "warning",
  ]);
  assert.deepEqual(first.evidence.elevation.levels, ["high", "low", "medium"]);

  const allVariables = first.collections.flatMap((item) => item.variables);
  for (const item of first.collections) {
    assert.equal(
      new Set(item.variables.map((variable) => variable.logicalId)).size,
      item.variables.length,
      `${item.name} contains a duplicate logical identifier`,
    );
    assert.equal(
      new Set(item.variables.map((variable) => variable.name)).size,
      item.variables.length,
      `${item.name} contains a duplicate visible name`,
    );
  }
  for (const variable of allVariables.filter((item) => item.alias)) {
    assert.ok(
      allVariables.some(
        (target) =>
          target.logicalId === variable.alias && target.type === variable.type,
      ),
      `${variable.logicalId} has no type-compatible alias target`,
    );
  }

  const serialized = JSON.stringify(first);
  assert.doesNotMatch(
    serialized,
    /figmaId|createdAt|timestamp|(?:[A-Z]:\\\\|\/Users\/|\/home\/)/iu,
  );
});

test("writes a Figma bundle only to an explicit absolute path outside Git", async () => {
  const { assembleTheme } = await loadGenerationUtilities();
  const { generateFigmaBundle, writeTemporaryFigmaBundle } = await import(
    "../scripts/lib/generate-figma-bundle.mjs"
  );
  const [light, dark] = await Promise.all([
    assembleTheme(tokenRoot, "light"),
    assembleTheme(tokenRoot, "dark"),
  ]);
  const bundle = generateFigmaBundle(light, dark);
  const outputRoot = await createTemporaryRoot();
  const outputPath = path.join(outputRoot, "figma.tokens.json");

  await assert.rejects(
    writeTemporaryFigmaBundle("figma.tokens.json", bundle),
    /absolute path outside the repository/i,
  );
  await assert.rejects(
    writeTemporaryFigmaBundle(
      path.join(designSystemRoot, "figma.tokens.json"),
      bundle,
    ),
    /outside the repository/i,
  );
  assert.equal(await writeTemporaryFigmaBundle(outputPath, bundle), "created");
  assert.equal(await writeTemporaryFigmaBundle(outputPath, bundle), "unchanged");
  assert.deepEqual(JSON.parse(await readFile(outputPath, "utf8")), bundle);
});

test("preserves the target type for named font-weight aliases in Figma", async () => {
  const { assembleTheme } = await loadGenerationUtilities();
  const { generateFigmaBundle } = await import(
    "../scripts/lib/generate-figma-bundle.mjs"
  );
  const [baseLight, baseDark] = await Promise.all([
    assembleTheme(tokenRoot, "light"),
    assembleTheme(tokenRoot, "dark"),
  ]);
  const additions = {
    "named-weight": { $type: "fontWeight", $value: "bold" },
    "named-weight-alias": {
      $type: "fontWeight",
      $value: "{fixture.named-weight}",
    },
  };

  const bundle = generateFigmaBundle(
    withAdditionalTokens(baseLight, additions),
    withAdditionalTokens(baseDark, additions),
  );
  const alias = bundle.collections[0].variables.find(
    (variable) => variable.logicalId === "fixture.named-weight-alias",
  );

  assert.equal(alias.type, "STRING");
  assert.equal(alias.alias, "fixture.named-weight");
});
