import { readFile, writeFile, access, mkdir } from 'node:fs/promises';
import { constants as fsConstants, existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import process from 'node:process';
import sharp from 'sharp';

const repository = process.cwd();
const inventoryPath = path.join(
  repository,
  'docs',
  'architecture',
  'diagram-publication',
  'inventory.json',
);
const packageJsonPath = path.join(repository, 'package.json');
const argumentsList = process.argv.slice(2);
const checkOnly = argumentsList.includes('--check');
const requestedId = optionValue(argumentsList, '--id');

const inventory = JSON.parse(await readFile(inventoryPath, 'utf8'));
const packageJson = JSON.parse(await readFile(packageJsonPath, 'utf8'));
const configPath = resolveRepositoryPath(inventory.renderer.config);

if (inventory.schemaVersion !== 1) {
  throw new Error(`Unsupported inventory schema: ${inventory.schemaVersion}`);
}
if (packageJson.devDependencies?.[inventory.renderer.packageName]
    !== inventory.renderer.version) {
  throw new Error(
    `Renderer version mismatch for ${inventory.renderer.packageName}`,
  );
}

const config = JSON.parse(await readFile(configPath, 'utf8'));
if (config.htmlLabels !== false) {
  throw new Error('Mermaid config must set root-level htmlLabels to false');
}
if (config.securityLevel !== 'strict') {
  throw new Error('Mermaid config must use strict securityLevel');
}

const entries = requestedId
  ? inventory.diagrams.filter((entry) => entry.id === requestedId)
  : inventory.diagrams;

if (requestedId && entries.length !== 1) {
  throw new Error(`Unknown diagram id: ${requestedId}`);
}

for (const entry of entries) {
  await validateEntryInputs(entry);
}

if (checkOnly) {
  console.log(`Validated ${entries.length} diagram publication entries.`);
  process.exit(0);
}

for (const entry of entries) {
  await renderEntry(entry);
}

console.log(`Rendered ${entries.length} diagram publication entries.`);

async function renderEntry(entry) {
  const svgPath = resolveRepositoryPath(entry.githubSvg);
  const sourcePath = resolveRepositoryPath(entry.source);

  await mkdir(path.dirname(svgPath), { recursive: true });
  if (entry.sourceType === 'MERMAID_FILE') {
    const cliPath = path.join(
      repository,
      'node_modules',
      '@mermaid-js',
      'mermaid-cli',
      'src',
      'cli.js',
    );
    await access(cliPath, fsConstants.R_OK);
    const result = spawnSync(process.execPath, [
      cliPath,
      '-i', sourcePath,
      '-o', svgPath,
      '-b', 'white',
      '-c', configPath,
    ], {
      cwd: repository,
      env: rendererEnvironment(),
      stdio: 'inherit',
    });
    if (result.error) {
      throw result.error;
    }
    if (result.status !== 0) {
      throw new Error(`Mermaid render failed for ${entry.id}`);
    }
    await preserveNativeLabelSpacing(svgPath);
  }

  if (entry.jiraPngRequired) {
    const pngPath = resolveRepositoryPath(entry.jiraPng);
    await mkdir(path.dirname(pngPath), { recursive: true });
    await sharp(svgPath, { density: 192 })
      .flatten({ background: '#FFFFFF' })
      .resize({ width: 2400, withoutEnlargement: false })
      .png()
      .toFile(pngPath);
  }
}

async function preserveNativeLabelSpacing(svgPath) {
  const svg = await readFile(svgPath, 'utf8');
  if (svg.includes('xml:space="preserve"')) {
    return;
  }
  const updated = svg.replace(/<svg\b/, '<svg xml:space="preserve"');
  if (updated === svg) {
    throw new Error(`Rendered output is not an SVG: ${svgPath}`);
  }
  await writeFile(svgPath, updated, 'utf8');
}

async function validateEntryInputs(entry) {
  if (!entry.id || !entry.owner || !entry.source || !entry.githubSvg) {
    throw new Error('Every diagram requires id, owner, source, and githubSvg');
  }
  if (!['MERMAID_FILE', 'HAND_AUTHORED_SVG'].includes(entry.sourceType)) {
    throw new Error(`Unsupported source type for ${entry.id}`);
  }
  if (entry.jiraPngRequired && !entry.jiraPng) {
    throw new Error(`Required Jira PNG is missing for ${entry.id}`);
  }
  await access(resolveRepositoryPath(entry.owner), fsConstants.R_OK);
  await access(resolveRepositoryPath(entry.source), fsConstants.R_OK);
  if (entry.sourceType === 'HAND_AUTHORED_SVG') {
    await access(resolveRepositoryPath(entry.githubSvg), fsConstants.R_OK);
  }
}

function resolveRepositoryPath(relativePath) {
  if (!relativePath || path.isAbsolute(relativePath)) {
    throw new Error(`Repository-relative path required: ${relativePath}`);
  }
  const resolved = path.resolve(repository, relativePath);
  const relative = path.relative(repository, resolved);
  if (relative.startsWith('..') || path.isAbsolute(relative)) {
    throw new Error(`Path escapes repository: ${relativePath}`);
  }
  return resolved;
}

function optionValue(args, option) {
  const index = args.indexOf(option);
  if (index < 0) {
    return null;
  }
  if (!args[index + 1] || args[index + 1].startsWith('--')) {
    throw new Error(`${option} requires a value`);
  }
  return args[index + 1];
}

function rendererEnvironment() {
  if (process.env.PUPPETEER_EXECUTABLE_PATH) {
    return process.env;
  }
  if (process.platform !== 'win32') {
    return process.env;
  }
  const candidates = [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  ];
  const executable = candidates.find((candidate) => existsSync(candidate));
  return executable
    ? { ...process.env, PUPPETEER_EXECUTABLE_PATH: executable }
    : process.env;
}
