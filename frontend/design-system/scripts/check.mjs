import path from "node:path";
import { pathToFileURL } from "node:url";

import { generateArtifacts } from "./generate.mjs";
import { designSystemPaths } from "./lib/paths.mjs";
import { verifyGeneratedFile } from "./lib/serialize-output.mjs";

export async function checkGeneratedArtifacts(inputs = {}) {
  const artifacts = await generateArtifacts(inputs);
  for (const [filePath, content] of artifacts.files) {
    await verifyGeneratedFile(filePath, content);
  }
  return artifacts;
}

async function main() {
  const artifacts = await checkGeneratedArtifacts();
  for (const filePath of artifacts.files.keys()) {
    console.log(`verified: ${path.relative(designSystemPaths.repositoryRoot, filePath)}`);
  }
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  await main();
}
