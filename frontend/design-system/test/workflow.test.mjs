import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const frontendRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
  "..",
);
const workflowPath = path.resolve(
  frontendRoot,
  "..",
  ".github",
  "workflows",
  "frontend-ci.yml",
);

test("frontend CI runs every token gate with pinned actions", async () => {
  const workflow = await readFile(workflowPath, "utf8");

  assert.match(workflow, /pull_request:[\s\S]*develop[\s\S]*main/u);
  assert.match(workflow, /push:[\s\S]*develop[\s\S]*main/u);
  assert.match(
    workflow,
    /actions\/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1/u,
  );
  assert.match(
    workflow,
    /actions\/setup-node@820762786026740c76f36085b0efc47a31fe5020/u,
  );
  assert.match(workflow, /node-version: "24"/u);
  assert.match(workflow, /cache: npm/u);
  assert.match(workflow, /cache-dependency-path: frontend\/package-lock\.json/u);
  assert.match(workflow, /working-directory: frontend/u);

  const commands = [
    "npm ci",
    "npm run tokens:test",
    "npm run tokens:validate",
    "npm run tokens:check",
    "git diff --exit-code -- src/styles/generated/tokens.css",
  ];
  const positions = commands.map((command) => workflow.indexOf(`run: ${command}`));
  assert.ok(positions.every((position) => position >= 0));
  assert.deepEqual(positions, [...positions].sort((left, right) => left - right));
});
