import { realpath } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

export function resolveDesignSystemPaths(moduleUrl) {
  const moduleDirectory = path.dirname(fileURLToPath(moduleUrl));
  const designSystemRoot = path.resolve(moduleDirectory, "..", "..");
  const frontendRoot = path.dirname(designSystemRoot);
  const repositoryRoot = path.dirname(frontendRoot);

  return {
    repositoryRoot,
    frontendRoot,
    designSystemRoot,
    tokenRoot: path.join(designSystemRoot, "tokens"),
    generatedCssPath: path.join(
      frontendRoot,
      "src",
      "styles",
      "generated",
      "tokens.css",
    ),
  };
}

export async function assertOutsideRepository(candidatePath, repositoryRoot) {
  if (!path.isAbsolute(candidatePath)) {
    throw new TypeError("Output requires an absolute path outside the repository.");
  }

  const [canonicalCandidate, canonicalRepository] = await Promise.all([
    canonicalizeWithMissingSegments(candidatePath),
    realpath(repositoryRoot),
  ]);

  if (isSameOrDescendant(canonicalCandidate, canonicalRepository)) {
    throw new RangeError("Output path must remain outside the repository.");
  }

  return candidatePath;
}

async function canonicalizeWithMissingSegments(candidatePath) {
  let current = path.resolve(candidatePath);
  const missingSegments = [];

  while (true) {
    try {
      const existingAncestor = await realpath(current);
      return path.join(existingAncestor, ...missingSegments.reverse());
    } catch (error) {
      if (error.code !== "ENOENT") {
        throw error;
      }

      const parent = path.dirname(current);
      if (parent === current) {
        throw error;
      }

      missingSegments.push(path.basename(current));
      current = parent;
    }
  }
}

function isSameOrDescendant(candidatePath, repositoryRoot) {
  const candidate = normalizeForComparison(candidatePath);
  const repository = normalizeForComparison(repositoryRoot);
  const relative = path.relative(repository, candidate);

  return (
    relative === "" ||
    (relative !== ".." &&
      !relative.startsWith(`..${path.sep}`) &&
      !path.isAbsolute(relative))
  );
}

function normalizeForComparison(candidatePath) {
  const normalized = path.normalize(candidatePath);
  return process.platform === "win32" ? normalized.toLowerCase() : normalized;
}
