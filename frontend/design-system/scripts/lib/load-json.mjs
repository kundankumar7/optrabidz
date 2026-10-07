import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import path from "node:path";

import Ajv from "ajv";

import { designSystemPaths } from "./paths.mjs";

let manifestValidatorPromise;

export async function loadJson(filePath) {
  const absolutePath = path.resolve(filePath);

  try {
    return JSON.parse(await readFile(absolutePath, "utf8"));
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new SyntaxError(`Invalid JSON in ${absolutePath}: ${error.message}`, {
        cause: error,
      });
    }
    throw error;
  }
}

export async function loadManifest(tokenRoot) {
  const manifestPath = path.join(path.resolve(tokenRoot), "manifest.tokens.json");
  const manifest = await loadJson(manifestPath);
  const { validateManifest, schemaChecksum } = await getManifestValidator();
  if (!validateManifest(manifest)) {
    throw new TypeError(
      `Manifest schema validation failed: ${formatSchemaErrors(validateManifest.errors)}.`,
    );
  }
  assertManifestShape(manifest);
  if (manifest.projectSchema.checksum !== schemaChecksum) {
    throw new Error(
      `Project schema checksum mismatch: manifest has ${manifest.projectSchema.checksum}, expected ${schemaChecksum}.`,
    );
  }
  return manifest;
}

function getManifestValidator() {
  manifestValidatorPromise ??= (async () => {
    const { designSystemRoot } = designSystemPaths;
    const schemaPath = path.join(designSystemRoot, "schema", "manifest.schema.json");
    const tokenSchemaPath = path.join(designSystemRoot, "schema", "dtcg-2025.10.schema.json");
    const [schema, tokenSchemaBytes] = await Promise.all([
      loadJson(schemaPath),
      readFile(tokenSchemaPath),
    ]);
    return {
      validateManifest: new Ajv({ allErrors: true, strict: false }).compile(schema),
      schemaChecksum: `sha256:${createHash("sha256").update(tokenSchemaBytes).digest("hex")}`,
    };
  })();
  return manifestValidatorPromise;
}

function assertManifestShape(manifest) {
  if (!manifest || typeof manifest !== "object" || Array.isArray(manifest)) {
    throw new TypeError("Token manifest must be a JSON object.");
  }
  if (manifest.schemaVersion !== 1 || manifest.dtcgVersion !== "2025.10") {
    throw new TypeError("Token manifest must declare schemaVersion 1 and DTCG 2025.10.");
  }
  if (!Array.isArray(manifest.baseSources) || manifest.baseSources.length === 0) {
    throw new TypeError("Token manifest must declare an ordered baseSources array.");
  }
  if (
    !manifest.themes ||
    Object.keys(manifest.themes).sort().join(",") !== "dark,light" ||
    typeof manifest.themes.light !== "string" ||
    typeof manifest.themes.dark !== "string"
  ) {
    throw new TypeError("Token manifest must declare exactly light and dark theme sources.");
  }
  if (!Array.isArray(manifest.allowedOverlayRoots)) {
    throw new TypeError("Token manifest must declare allowedOverlayRoots.");
  }
}

function formatSchemaErrors(errors = []) {
  return errors
    .map((error) => {
      const detail = error.params?.missingProperty ?? error.params?.additionalProperty;
      return `${error.instancePath || "<root>"} ${error.message}${detail ? ` (${detail})` : ""}`;
    })
    .join("; ");
}
