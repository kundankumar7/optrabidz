import { readdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const ignoredDirectories = new Set([".git", "build", "node_modules", "target"]);
const prohibitedNames = [
  { label: "design transfer bundle", pattern: /^figma\.tokens\.json$/iu },
  { label: "generated transfer bundle", pattern: /\.bundle\.json$/iu },
  { label: "local design file", pattern: /\.fig$/iu },
  {
    label: "local state artifact",
    pattern: /(^|[-_.])(state|sync[-_.]?state)([-_.]|$)/iu,
  },
  {
    label: "local backup or laboratory artifact",
    pattern: /(^|[-_.])(backup|laboratory|lab)([-_.]|$)/iu,
  },
];

export async function findRepositoryPolicyViolations(repositoryRoot) {
  const absoluteRoot = path.resolve(repositoryRoot);
  const violations = [];
  await visitDirectory(absoluteRoot, absoluteRoot, violations);
  return violations.sort((left, right) => left.path.localeCompare(right.path));
}

async function visitDirectory(directory, repositoryRoot, violations) {
  const entries = await readdir(directory, { withFileTypes: true });
  entries.sort((left, right) => left.name.localeCompare(right.name));

  for (const entry of entries) {
    if (entry.isDirectory() && ignoredDirectories.has(entry.name)) {
      continue;
    }

    const absolutePath = path.join(directory, entry.name);
    const match = prohibitedNames.find(({ pattern }) => pattern.test(entry.name));
    if (match) {
      violations.push({
        path: path.relative(repositoryRoot, absolutePath).split(path.sep).join("/"),
        reason: match.label,
      });
    }

    if (entry.isDirectory()) {
      await visitDirectory(absolutePath, repositoryRoot, violations);
    }
  }
}

function parseRoot(arguments_) {
  if (arguments_.length === 0) {
    return path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..", "..");
  }
  if (arguments_.length === 2 && arguments_[0] === "--root" && path.isAbsolute(arguments_[1])) {
    return path.resolve(arguments_[1]);
  }
  throw new TypeError("Usage: repository-policy.mjs [--root <absolute-path>]");
}

async function main(arguments_) {
  const repositoryRoot = parseRoot(arguments_);
  const violations = await findRepositoryPolicyViolations(repositoryRoot);
  if (violations.length > 0) {
    for (const violation of violations) {
      console.error(`${violation.path}: ${violation.reason}`);
    }
    process.exitCode = 1;
    return;
  }
  console.log("Repository artifact policy passed.");
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  await main(process.argv.slice(2));
}
