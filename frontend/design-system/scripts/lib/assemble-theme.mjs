import { createHash } from "node:crypto";
import path from "node:path";

import Ajv from "ajv";

import { loadJson, loadManifest } from "./load-json.mjs";
import { designSystemPaths } from "./paths.mjs";
import { flattenTokenPaths } from "./token-paths.mjs";

const SCHEMA_URN = "urn:optrabidz:design-tokens:schema:2025.10";
let tokenValidatorPromise;

export async function assembleTheme(tokenRoot, theme) {
  const absoluteTokenRoot = path.resolve(tokenRoot);
  const manifest = await loadManifest(absoluteTokenRoot);

  if (theme !== "light" && theme !== "dark") {
    throw new TypeError(`Unsupported theme ${theme}. Expected light or dark.`);
  }

  const sourceFiles = manifest.baseSources.map((source) =>
    resolveSourcePath(absoluteTokenRoot, source),
  );
  const themeFile = resolveSourcePath(absoluteTokenRoot, manifest.themes[theme]);
  const baseDocuments = await Promise.all(sourceFiles.map(loadJson));
  const themeDocument = await loadJson(themeFile);

  for (let index = 0; index < baseDocuments.length; index += 1) {
    await validateSourceDocument(baseDocuments[index], sourceFiles[index]);
  }
  await validateSourceDocument(themeDocument, themeFile);

  const document = { $schema: SCHEMA_URN };
  const seenBasePaths = new Set();

  for (let index = 0; index < baseDocuments.length; index += 1) {
    const sourceTokens = flattenTokenPaths(baseDocuments[index]);
    for (const logicalId of sourceTokens.keys()) {
      if (seenBasePaths.has(logicalId)) {
        throw new Error(`Duplicate logical path ${logicalId} across base sources.`);
      }
      seenBasePaths.add(logicalId);
    }
    mergeGroups(document, baseDocuments[index]);
  }

  const overlayTokens = flattenTokenPaths(themeDocument);
  for (const logicalId of overlayTokens.keys()) {
    if (!isAllowedOverlay(logicalId, manifest.allowedOverlayRoots)) {
      throw new Error(`Overlay path ${logicalId} is not allowed by the manifest.`);
    }
    if (!seenBasePaths.has(logicalId)) {
      throw new Error(`Overlay path ${logicalId} has no semantic base token.`);
    }
  }
  mergeGroups(document, themeDocument, { replaceTokens: true });

  const tokens = flattenTokenPaths(document);
  const fingerprint = fingerprintDocument({ theme, document });

  return {
    theme,
    document,
    tokens,
    sourceFiles: [...sourceFiles, themeFile],
    fingerprint,
  };
}

function resolveSourcePath(tokenRoot, sourcePath) {
  if (typeof sourcePath !== "string" || path.isAbsolute(sourcePath)) {
    throw new TypeError("Manifest source paths must be relative strings.");
  }

  const resolved = path.resolve(tokenRoot, sourcePath);
  const relative = path.relative(tokenRoot, resolved);
  if (relative === ".." || relative.startsWith(`..${path.sep}`) || path.isAbsolute(relative)) {
    throw new RangeError(`Manifest source escapes the token root: ${sourcePath}.`);
  }
  return resolved;
}

async function validateSourceDocument(document, sourceFile) {
  if (!document || typeof document !== "object" || Array.isArray(document)) {
    throw new TypeError(`Token source ${sourceFile} must contain a JSON object.`);
  }
  if (document.$schema !== SCHEMA_URN) {
    throw new TypeError(`Token source ${sourceFile} must declare ${SCHEMA_URN}.`);
  }

  const validateTokens = await getTokenValidator();
  if (!validateTokens(document)) {
    throw new TypeError(
      `Token schema validation failed for ${sourceFile}: ${formatSchemaErrors(validateTokens.errors)}.`,
    );
  }

  const tokens = flattenTokenPaths(document);
  for (const { logicalId, token, inheritedType } of tokens.values()) {
    const effectiveLocalType = token.$type ?? inheritedType;
    if (
      effectiveLocalType === "color" &&
      Object.hasOwn(token, "$value") &&
      !isReference(token.$value) &&
      !isSrgbColor(token.$value)
    ) {
      throw new TypeError(`Color value at ${logicalId} must be a DTCG sRGB object.`);
    }
  }
}

function getTokenValidator() {
  tokenValidatorPromise ??= (async () => {
    const { designSystemRoot } = designSystemPaths;
    const schema = await loadJson(path.join(designSystemRoot, "schema", "dtcg-2025.10.schema.json"));
    return new Ajv({ allErrors: true, strict: false }).compile(schema);
  })();
  return tokenValidatorPromise;
}

function mergeGroups(target, source, options = {}) {
  for (const [key, value] of Object.entries(source)) {
    if (key === "$schema") {
      continue;
    }

    if (key.startsWith("$")) {
      target[key] = structuredClone(value);
      continue;
    }

    if (isToken(value)) {
      if (!options.replaceTokens && Object.hasOwn(target, key)) {
        throw new Error(`Duplicate logical path encountered while merging ${key}.`);
      }
      target[key] = structuredClone(value);
      continue;
    }

    if (!target[key]) {
      target[key] = {};
    } else if (isToken(target[key])) {
      throw new Error(`Cannot merge group ${key} over an existing token.`);
    }
    mergeGroups(target[key], value, options);
  }
}

function isAllowedOverlay(logicalId, allowedRoots) {
  return allowedRoots.some(
    (root) => logicalId === root || logicalId.startsWith(`${root}.`),
  );
}

function fingerprintDocument(value) {
  return `sha256:${createHash("sha256").update(stableStringify(value)).digest("hex")}`;
}

function stableStringify(value) {
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(",")}]`;
  }
  if (value !== null && typeof value === "object") {
    return `{${Object.keys(value)
      .sort()
      .map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`)
      .join(",")}}`;
  }
  return JSON.stringify(value);
}

function isToken(value) {
  return (
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value) &&
    (Object.hasOwn(value, "$value") || Object.hasOwn(value, "$ref"))
  );
}

function isReference(value) {
  return (
    (typeof value === "string" && /^\{[^{}]+\}$/.test(value)) ||
    (value !== null &&
      typeof value === "object" &&
      !Array.isArray(value) &&
      typeof value.$ref === "string")
  );
}

function isSrgbColor(value) {
  return (
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value) &&
    value.colorSpace === "srgb" &&
    Array.isArray(value.components) &&
    value.components.length === 3 &&
    value.components.every((component) =>
      typeof component === "number" ? component >= 0 && component <= 1 : isReference(component),
    ) &&
    (value.alpha === undefined ||
      (typeof value.alpha === "number" && value.alpha >= 0 && value.alpha <= 1)) &&
    (value.hex === undefined || /^#[0-9A-F]{6}(?:[0-9A-F]{2})?$/.test(value.hex))
  );
}

function formatSchemaErrors(errors = []) {
  return errors
    .map((error) => {
      const detail = error.params?.missingProperty ?? error.params?.additionalProperty;
      return `${error.instancePath || "<root>"} ${error.message}${detail ? ` (${detail})` : ""}`;
    })
    .join("; ");
}
