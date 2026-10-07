import path from "node:path";

import { assembleTheme } from "./lib/assemble-theme.mjs";
import { loadJson, loadManifest } from "./lib/load-json.mjs";
import { designSystemPaths } from "./lib/paths.mjs";
import { validateDocument, validateThemeParity } from "./lib/validate-document.mjs";

const { tokenRoot } = designSystemPaths;
const manifest = await loadManifest(tokenRoot);
const [light, dark] = await Promise.all([
  assembleTheme(tokenRoot, "light"),
  assembleTheme(tokenRoot, "dark"),
]);
const contracts = await loadContracts(tokenRoot, manifest.contracts);
const reports = [
  validateDocument(light, contracts),
  validateDocument(dark, contracts),
];
const issues = [...reports.flatMap((report) => report.issues), ...validateThemeParity(light, dark)];

if (issues.length > 0) {
  for (const validationIssue of issues) {
    console.error(`${validationIssue.code} ${validationIssue.path}: ${validationIssue.message}`);
  }
  process.exitCode = 1;
} else {
  console.log(`Validated light ${light.fingerprint} and dark ${dark.fingerprint}.`);
}

async function loadContracts(root, contractPaths) {
  return Object.fromEntries(
    await Promise.all(
      Object.entries(contractPaths).map(async ([name, contractPath]) => [
        name,
        await loadJson(path.resolve(root, contractPath)),
      ]),
    ),
  );
}
