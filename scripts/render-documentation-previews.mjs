import { access, mkdir, readFile } from 'node:fs/promises';
import { constants as fsConstants } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import sharp from 'sharp';

const repository = process.cwd();
const reviewRoot = path.resolve(repository, 'target', 'documentation-review');
const inventoryPath = path.resolve(
  repository,
  'docs',
  'architecture',
  'diagram-publication',
  'inventory.json',
);
const requestedId = optionValue(process.argv.slice(2), '--id');
const inventory = JSON.parse(await readFile(inventoryPath, 'utf8'));
const entries = requestedId
  ? inventory.diagrams.filter((entry) => entry.id === requestedId)
  : inventory.diagrams;

if (requestedId && entries.length !== 1) {
  throw new Error(`Unknown diagram id: ${requestedId}`);
}

for (const entry of entries) {
  const svgPath = resolveRepositoryPath(entry.githubSvg);
  await access(svgPath, fsConstants.R_OK);
  const outputDirectory = resolveReviewPath(entry.id);
  await mkdir(outputDirectory, { recursive: true });
  await renderPreview(svgPath, path.join(outputDirectory, 'desktop-980.png'), 980);
  await renderPreview(svgPath, path.join(outputDirectory, 'phone-390.png'), 390);
}

console.log(`Generated previews for ${entries.length} diagram entries.`);

async function renderPreview(source, destination, width) {
  await sharp(source, { density: 192 })
    .flatten({ background: '#FFFFFF' })
    .resize({ width, withoutEnlargement: false })
    .png()
    .toFile(destination);
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

function resolveReviewPath(id) {
  if (!/^[a-z0-9][a-z0-9-]*$/.test(id)) {
    throw new Error(`Unsafe diagram id: ${id}`);
  }
  const resolved = path.resolve(reviewRoot, id);
  if (!resolved.startsWith(`${reviewRoot}${path.sep}`)) {
    throw new Error(`Preview path escapes review directory: ${id}`);
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
