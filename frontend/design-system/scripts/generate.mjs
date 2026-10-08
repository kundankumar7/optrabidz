import path from "node:path";
import { pathToFileURL } from "node:url";

import { assembleTheme } from "./lib/assemble-theme.mjs";
import { generateCss } from "./lib/generate-css.mjs";
import {
  generateFigmaBundle,
  writeTemporaryFigmaBundle,
} from "./lib/generate-figma-bundle.mjs";
import { designSystemPaths } from "./lib/paths.mjs";
import { writeIfChanged } from "./lib/serialize-output.mjs";

export async function generateArtifacts(inputs = {}) {
  const tokenRoot = path.resolve(inputs.tokenRoot ?? designSystemPaths.tokenRoot);
  const cssOutputPath = path.resolve(
    inputs.cssOutputPath ?? designSystemPaths.generatedCssPath,
  );
  const [light, dark] = await Promise.all([
    assembleTheme(tokenRoot, "light"),
    assembleTheme(tokenRoot, "dark"),
  ]);
  const css = generateCss(light, dark);
  const figmaBundle = generateFigmaBundle(light, dark);

  return {
    light,
    dark,
    css,
    figmaBundle,
    files: new Map([[cssOutputPath, css]]),
  };
}

async function main(arguments_) {
  if (arguments_.length === 2 && arguments_[0] === "--write-figma-bundle") {
    const artifacts = await generateArtifacts();
    const result = await writeTemporaryFigmaBundle(arguments_[1], artifacts.figmaBundle);
    console.log(`${result}: ${arguments_[1]}`);
    return;
  }
  if (arguments_.length !== 1 || arguments_[0] !== "--write") {
    throw new TypeError(
      "Generation requires --write or --write-figma-bundle with an absolute external path.",
    );
  }

  const artifacts = await generateArtifacts();
  for (const [filePath, content] of artifacts.files) {
    const result = await writeIfChanged(filePath, content);
    console.log(`${result}: ${path.relative(designSystemPaths.repositoryRoot, filePath)}`);
  }
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  await main(process.argv.slice(2));
}
