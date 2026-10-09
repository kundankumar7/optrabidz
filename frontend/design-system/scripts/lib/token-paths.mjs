const PROJECT_SEGMENT = /^[a-z0-9][a-z0-9_-]*$/;

export function flattenTokenPaths(document) {
  const tokens = new Map();
  visitGroup(document, [], tokens, undefined);
  return tokens;
}

export function logicalToFigmaName(logicalId) {
  return parseLogicalId(logicalId).join("/");
}

export function logicalToCssName(logicalId) {
  return `--ob-${parseLogicalId(logicalId).join("-")}`;
}

function visitGroup(group, segments, tokens, inheritedType) {
  if (!isObject(group)) {
    throw new TypeError(`Group ${formatPath(segments)} must be an object.`);
  }

  const localType = typeof group.$type === "string" ? group.$type : inheritedType;
  const entries = Object.entries(group).filter(([key]) => !key.startsWith("$"));
  assertNames(entries.map(([key]) => key), segments);

  if (group.$root !== undefined) {
    visitToken(group.$root, [...segments, "$root"], tokens, localType);
  }

  for (const [key, value] of entries) {
    const childSegments = [...segments, key];
    if (isToken(value)) {
      visitToken(value, childSegments, tokens, localType);
    } else {
      visitGroup(value, childSegments, tokens, localType);
    }
  }
}

function visitToken(token, segments, tokens, inheritedType) {
  if (!isObject(token) || (!Object.hasOwn(token, "$value") && !Object.hasOwn(token, "$ref"))) {
    throw new TypeError(`Token ${formatPath(segments)} must contain $value or $ref.`);
  }

  const childKeys = Object.keys(token).filter((key) => !key.startsWith("$"));
  if (childKeys.length > 0) {
    throw new TypeError(`Token ${formatPath(segments)} cannot contain child groups.`);
  }

  const logicalId = segments.join(".");
  if (tokens.has(logicalId)) {
    throw new Error(`Duplicate logical path ${logicalId}.`);
  }

  tokens.set(logicalId, {
    logicalId,
    segments,
    token,
    inheritedType,
  });
}

function assertNames(names, parentSegments) {
  const folded = new Map();

  for (const name of names) {
    const lower = name.toLowerCase();
    if (folded.has(lower)) {
      throw new Error(
        `Case collision at ${formatPath([...parentSegments, lower])}: ${folded.get(lower)} and ${name}.`,
      );
    }
    folded.set(lower, name);
  }

  for (const name of names) {
    if (
      name.startsWith("$") ||
      name.includes("{") ||
      name.includes("}") ||
      name.includes(".") ||
      !PROJECT_SEGMENT.test(name)
    ) {
      throw new Error(`Prohibited token or group name ${formatPath([...parentSegments, name])}.`);
    }
  }
}

function parseLogicalId(logicalId) {
  if (typeof logicalId !== "string" || logicalId.length === 0) {
    throw new TypeError("Logical identifier must be a non-empty string.");
  }

  const segments = logicalId.split(".");
  if (segments.some((segment) => !PROJECT_SEGMENT.test(segment) && segment !== "$root")) {
    throw new TypeError(`Invalid logical identifier ${logicalId}.`);
  }
  return segments;
}

function isToken(value) {
  return isObject(value) && (Object.hasOwn(value, "$value") || Object.hasOwn(value, "$ref"));
}

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function formatPath(segments) {
  return segments.length === 0 ? "<root>" : segments.join(".");
}
