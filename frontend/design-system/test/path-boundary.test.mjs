import assert from "node:assert/strict";
import { mkdtemp, mkdir, rm, symlink } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { after, before, test } from "node:test";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));
const expectedDesignSystemRoot = path.dirname(testDirectory);
const expectedFrontendRoot = path.dirname(expectedDesignSystemRoot);
const expectedRepositoryRoot = path.dirname(expectedFrontendRoot);

let temporaryRoot;

before(async () => {
  temporaryRoot = await mkdtemp(path.join(tmpdir(), "ob-path-boundary-"));
});

after(async () => {
  if (temporaryRoot) {
    await rm(temporaryRoot, { recursive: true, force: true });
  }
});

async function loadPathUtilities() {
  return import("../scripts/lib/paths.mjs");
}

test("resolves repository paths from the module URL instead of the current directory", async () => {
  const originalDirectory = process.cwd();
  const unrelatedDirectory = path.join(temporaryRoot, "unrelated-working-directory");
  await mkdir(unrelatedDirectory);

  try {
    process.chdir(unrelatedDirectory);
    const { resolveDesignSystemPaths } = await loadPathUtilities();
    const resolved = resolveDesignSystemPaths(
      new URL("../scripts/lib/paths.mjs", import.meta.url).href,
    );

    assert.deepEqual(resolved, {
      repositoryRoot: expectedRepositoryRoot,
      frontendRoot: expectedFrontendRoot,
      designSystemRoot: expectedDesignSystemRoot,
      tokenRoot: path.join(expectedDesignSystemRoot, "tokens"),
      generatedCssPath: path.join(
        expectedFrontendRoot,
        "src",
        "styles",
        "generated",
        "tokens.css",
      ),
    });
  } finally {
    process.chdir(originalDirectory);
  }
});

test("accepts an absolute output path in the operating-system temporary directory", async () => {
  const { assertOutsideRepository } = await loadPathUtilities();
  const candidate = path.join(temporaryRoot, "bundle.json");

  assert.equal(
    await assertOutsideRepository(candidate, expectedRepositoryRoot),
    candidate,
  );
});

test("rejects the repository root and the design-system directory", async () => {
  const { assertOutsideRepository } = await loadPathUtilities();

  await assert.rejects(
    assertOutsideRepository(expectedRepositoryRoot, expectedRepositoryRoot),
    /outside the repository/i,
  );
  await assert.rejects(
    assertOutsideRepository(expectedDesignSystemRoot, expectedRepositoryRoot),
    /outside the repository/i,
  );
});

test("rejects relative paths even when traversal would leave the repository", async () => {
  const { assertOutsideRepository } = await loadPathUtilities();

  await assert.rejects(
    assertOutsideRepository("../bundle.json", expectedRepositoryRoot),
    /absolute path/i,
  );
});

test("rejects a symlink that resolves into the repository", async (context) => {
  const { assertOutsideRepository } = await loadPathUtilities();
  const linkPath = path.join(temporaryRoot, "repository-symbolic-link");

  try {
    await symlink(expectedRepositoryRoot, linkPath, "dir");
  } catch (error) {
    if (process.platform === "win32" && error.code === "EPERM") {
      context.skip("Windows symbolic links require Developer Mode or elevation");
      return;
    }
    throw error;
  }

  await assert.rejects(
    assertOutsideRepository(path.join(linkPath, "bundle.json"), expectedRepositoryRoot),
    /outside the repository/i,
  );
});

test("rejects a Windows junction that resolves into the repository", async (context) => {
  if (process.platform !== "win32") {
    context.skip("Windows junction behavior");
    return;
  }

  const { assertOutsideRepository } = await loadPathUtilities();
  const junctionPath = path.join(temporaryRoot, "repository-junction");
  await symlink(expectedRepositoryRoot, junctionPath, "junction");

  await assert.rejects(
    assertOutsideRepository(path.join(junctionPath, "bundle.json"), expectedRepositoryRoot),
    /outside the repository/i,
  );
});

test("rejects an inside path when only the Windows drive-letter case differs", async (context) => {
  if (process.platform !== "win32") {
    context.skip("Windows drive-letter behavior");
    return;
  }

  const { assertOutsideRepository } = await loadPathUtilities();
  const alternateCase = `${expectedDesignSystemRoot[0].toLowerCase()}${expectedDesignSystemRoot.slice(1)}`;

  await assert.rejects(
    assertOutsideRepository(alternateCase, expectedRepositoryRoot),
    /outside the repository/i,
  );
});

test("does not confuse a sibling directory that merely shares the repository prefix", async () => {
  const { assertOutsideRepository } = await loadPathUtilities();
  const sibling = `${expectedRepositoryRoot}-output`;

  assert.equal(
    await assertOutsideRepository(path.join(sibling, "bundle.json"), expectedRepositoryRoot),
    path.join(sibling, "bundle.json"),
  );
});

test("rejects a not-yet-created output whose nearest existing ancestor is inside Git", async () => {
  const { assertOutsideRepository } = await loadPathUtilities();
  const candidate = path.join(
    expectedRepositoryRoot,
    "not-created",
    "nested",
    "bundle.json",
  );

  await assert.rejects(
    assertOutsideRepository(candidate, expectedRepositoryRoot),
    /outside the repository/i,
  );
});
