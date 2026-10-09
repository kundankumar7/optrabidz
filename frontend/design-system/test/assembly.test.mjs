import assert from "node:assert/strict";
import { cp, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { after, test } from "node:test";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));
const fixtureDirectory = path.join(testDirectory, "fixtures", "assembly");
const temporaryRoots = [];

after(async () => {
  await Promise.all(
    temporaryRoots.map((directory) => rm(directory, { recursive: true, force: true })),
  );
});

async function createTokenRoot() {
  const root = await mkdtemp(path.join(tmpdir(), "ob-assembly-"));
  temporaryRoots.push(root);
  await cp(fixtureDirectory, root, { recursive: true });
  return root;
}

async function readJson(filePath) {
  return JSON.parse(await readFile(filePath, "utf8"));
}

async function writeJson(filePath, value) {
  await writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

async function loadAssemblyUtilities() {
  const [assembly, tokenPaths] = await Promise.all([
    import("../scripts/lib/assemble-theme.mjs"),
    import("../scripts/lib/token-paths.mjs"),
  ]);
  return { ...assembly, ...tokenPaths };
}

test("assembles base sources in manifest order and exactly one selected theme", async () => {
  const tokenRoot = await createTokenRoot();
  const { assembleTheme } = await loadAssemblyUtilities();
  const light = await assembleTheme(tokenRoot, "light");
  const dark = await assembleTheme(tokenRoot, "dark");

  assert.deepEqual(
    light.sourceFiles.map((filePath) => path.relative(tokenRoot, filePath)),
    ["primitives.tokens.json", "semantic.tokens.json", path.join("themes", "light.tokens.json")],
  );
  assert.equal(light.theme, "light");
  assert.equal(light.tokens.get("color.text.primary").token.$value, "{color.neutral.warm.900}");
  assert.equal(dark.tokens.get("color.text.primary").token.$value, "{color.neutral.warm.000}");
});

test("rejects duplicate logical paths across base sources", async () => {
  const tokenRoot = await createTokenRoot();
  const semanticPath = path.join(tokenRoot, "semantic.tokens.json");
  const semantic = await readJson(semanticPath);
  semantic.color.neutral = {
    warm: {
      "900": {
        $type: "color",
        $value: {
          colorSpace: "srgb",
          components: [0, 0, 0],
          alpha: 1,
          hex: "#000000",
        },
      },
    },
  };
  await writeJson(semanticPath, semantic);

  const { assembleTheme } = await loadAssemblyUtilities();
  await assert.rejects(assembleTheme(tokenRoot, "light"), /duplicate logical path.*color\.neutral\.warm\.900/i);
});

test("rejects a theme overlay outside its declared semantic roots", async () => {
  const tokenRoot = await createTokenRoot();
  const lightPath = path.join(tokenRoot, "themes", "light.tokens.json");
  const light = await readJson(lightPath);
  light.color.neutral = {
    warm: {
      "900": {
        $type: "color",
        $value: {
          colorSpace: "srgb",
          components: [0, 0, 0],
          alpha: 1,
          hex: "#000000",
        },
      },
    },
  };
  await writeJson(lightPath, light);

  const { assembleTheme } = await loadAssemblyUtilities();
  await assert.rejects(assembleTheme(tokenRoot, "light"), /overlay path.*color\.neutral\.warm\.900.*not allowed/i);
});

test("rejects prohibited characters and case-colliding names", async () => {
  const tokenRoot = await createTokenRoot();
  const primitivePath = path.join(tokenRoot, "primitives.tokens.json");
  const primitives = await readJson(primitivePath);
  primitives.color["bad.name"] = { $type: "number", $value: 1 };
  await writeJson(primitivePath, primitives);

  const { assembleTheme } = await loadAssemblyUtilities();
  await assert.rejects(assembleTheme(tokenRoot, "light"), /prohibited.*bad\.name/i);

  delete primitives.color["bad.name"];
  primitives.color.Neutral = { $type: "number", sample: { $value: 1 } };
  await writeJson(primitivePath, primitives);
  await assert.rejects(assembleTheme(tokenRoot, "light"), /case collision.*color\.neutral/i);
});

test("maps logical identifiers mechanically to Figma and CSS names", async () => {
  const { logicalToCssName, logicalToFigmaName } = await loadAssemblyUtilities();

  assert.equal(logicalToFigmaName("color.text.primary"), "color/text/primary");
  assert.equal(logicalToCssName("color.text.primary"), "--ob-color-text-primary");
  assert.throws(() => logicalToCssName("color..primary"), /logical identifier/i);
});

test("produces a deterministic fingerprint and preserves unknown extensions", async () => {
  const tokenRoot = await createTokenRoot();
  const { assembleTheme } = await loadAssemblyUtilities();
  const first = await assembleTheme(tokenRoot, "light");
  const second = await assembleTheme(tokenRoot, "light");

  assert.match(first.fingerprint, /^sha256:[a-f0-9]{64}$/);
  assert.equal(first.fingerprint, second.fingerprint);
  assert.deepEqual(
    first.document.color.neutral.warm["900"].$extensions,
    { "org.optrabidz.evidence": { source: "approved" } },
  );
});

test("rejects a bare hexadecimal string where a DTCG sRGB object is required", async () => {
  const tokenRoot = await createTokenRoot();
  const primitivePath = path.join(tokenRoot, "primitives.tokens.json");
  const primitives = await readJson(primitivePath);
  primitives.color.neutral.warm["900"].$value = "#151714";
  await writeJson(primitivePath, primitives);

  const { assembleTheme } = await loadAssemblyUtilities();
  await assert.rejects(assembleTheme(tokenRoot, "light"), /color value.*sRGB object/i);
});

test("enforces the project-maintained manifest schema", async () => {
  const tokenRoot = await createTokenRoot();
  const manifestPath = path.join(tokenRoot, "manifest.tokens.json");
  const manifest = await readJson(manifestPath);
  delete manifest.normativeSource;
  await writeJson(manifestPath, manifest);

  const { assembleTheme } = await loadAssemblyUtilities();
  await assert.rejects(assembleTheme(tokenRoot, "light"), /manifest schema.*normativeSource/i);
});

test("rejects a manifest whose project-schema checksum has drifted", async () => {
  const tokenRoot = await createTokenRoot();
  const manifestPath = path.join(tokenRoot, "manifest.tokens.json");
  const manifest = await readJson(manifestPath);
  manifest.projectSchema.checksum = `sha256:${"0".repeat(64)}`;
  await writeJson(manifestPath, manifest);

  const { assembleTheme } = await loadAssemblyUtilities();
  await assert.rejects(assembleTheme(tokenRoot, "light"), /project schema checksum mismatch/i);
});

test("enforces the project-maintained DTCG schema before assembly", async () => {
  const tokenRoot = await createTokenRoot();
  const primitivePath = path.join(tokenRoot, "primitives.tokens.json");
  const primitives = await readJson(primitivePath);
  primitives.color.neutral.warm["900"].unexpected = true;
  await writeJson(primitivePath, primitives);

  const { assembleTheme } = await loadAssemblyUtilities();
  await assert.rejects(assembleTheme(tokenRoot, "light"), /token schema.*unexpected/i);
});
