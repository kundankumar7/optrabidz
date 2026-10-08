import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";

export async function writeIfChanged(filePath, content) {
  const current = await readOptionalFile(filePath);
  if (current === content) {
    return "unchanged";
  }

  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, content, "utf8");
  return current === undefined ? "created" : "updated";
}

export async function verifyGeneratedFile(filePath, expected) {
  const current = await readOptionalFile(filePath);
  if (current === undefined) {
    throw new Error(`Generated file is missing: ${filePath}. Run npm run tokens:build.`);
  }
  if (current !== expected) {
    throw new Error(`Generated file is stale: ${filePath}. Run npm run tokens:build.`);
  }
}

async function readOptionalFile(filePath) {
  try {
    return await readFile(filePath, "utf8");
  } catch (error) {
    if (error.code === "ENOENT") {
      return undefined;
    }
    throw error;
  }
}
