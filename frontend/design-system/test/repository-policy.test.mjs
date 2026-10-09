import assert from "node:assert/strict";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { after, before, test } from "node:test";
import { spawnSync } from "node:child_process";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));
const policyScript = path.resolve(
  testDirectory,
  "..",
  "scripts",
  "repository-policy.mjs",
);

let temporaryRoot;

before(async () => {
  temporaryRoot = await mkdtemp(path.join(tmpdir(), "ob-repository-policy-"));
});

after(async () => {
  if (temporaryRoot) {
    await rm(temporaryRoot, { recursive: true, force: true });
  }
});

function runPolicy(root) {
  return spawnSync(process.execPath, [policyScript, "--root", root], {
    encoding: "utf8",
  });
}

function runGit(root, arguments_) {
  const result = spawnSync("git", arguments_, {
    cwd: root,
    encoding: "utf8",
  });
  assert.equal(result.status, 0, result.stderr);
}

test("accepts a repository containing only maintained source artifacts", async () => {
  const root = path.join(temporaryRoot, "clean");
  await mkdir(path.join(root, "frontend", "design-system"), { recursive: true });
  await writeFile(path.join(root, "frontend", "design-system", "README.md"), "# Maintained\n");

  const result = runPolicy(root);

  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /repository artifact policy passed/i);
});

test("rejects private transfer, state, backup, and laboratory artifacts", async () => {
  const root = path.join(temporaryRoot, "blocked");
  const prohibitedPaths = [
    "figma.tokens.json",
    "design-sync-state.json",
    "token-export.bundle.json",
    "pre-migration-backup.fig",
    "typography-laboratory.html",
  ];
  await mkdir(root, { recursive: true });
  await Promise.all(
    prohibitedPaths.map((fileName) => writeFile(path.join(root, fileName), "private\n")),
  );

  const result = runPolicy(root);

  assert.equal(result.status, 1);
  for (const fileName of prohibitedPaths) {
    assert.match(result.stderr, new RegExp(fileName.replaceAll(".", "\\."), "u"));
  }
});

test("does not inspect dependency, build-output, or version-control directories", async () => {
  const root = path.join(temporaryRoot, "ignored-build-output");
  const ignoredDirectories = [".git", "node_modules", "target", "build"];
  await Promise.all(
    ignoredDirectories.map(async (directory) => {
      const directoryPath = path.join(root, directory);
      await mkdir(directoryPath, { recursive: true });
      await writeFile(path.join(directoryPath, "temporary-backup.fig"), "generated\n");
    }),
  );

  const result = runPolicy(root);

  assert.equal(result.status, 0, result.stderr);
});

test("rejects force-added private artifacts inside ignored build directories", async () => {
  const root = path.join(temporaryRoot, "tracked-build-output");
  const prohibitedPath = path.join(root, "build", "pre-migration-backup.fig");
  await mkdir(path.dirname(prohibitedPath), { recursive: true });
  await writeFile(prohibitedPath, "private\n");
  runGit(root, ["init", "--quiet"]);
  runGit(root, ["add", "--force", "build/pre-migration-backup.fig"]);

  const result = runPolicy(root);

  assert.equal(result.status, 1);
  assert.match(result.stderr, /build\/pre-migration-backup\.fig/u);
});
