import { flattenTokenPaths } from "./token-paths.mjs";

const CURLY_REFERENCE = /^\{([^{}]+)\}$/;

export function resolveReferences(document) {
  const engine = createResolutionEngine(document);
  const resolved = new Map();

  for (const logicalId of [...engine.tokens.keys()].sort()) {
    resolved.set(logicalId, engine.resolveToken(logicalId));
  }
  return resolved;
}

export function resolveEffectiveType(tokenPath, document) {
  return createResolutionEngine(document).resolveType(tokenPath);
}

function createResolutionEngine(document) {
  const materializedDocument = materializeExtensions(document);
  const tokens = flattenTokenPaths(materializedDocument);
  const typeCache = new Map();
  const valueCache = new Map();

  function resolveType(logicalId, stack = []) {
    if (typeCache.has(logicalId)) {
      return typeCache.get(logicalId);
    }
    const record = requireToken(logicalId);
    if (stack.includes(logicalId)) {
      throw new Error(`Reference cycle while resolving type: ${[...stack, logicalId].join(" -> ")}.`);
    }

    const declaredType = record.token.$type ?? record.inheritedType;
    const referencedId = wholeReferenceTarget(record.token, materializedDocument);
    const referencedType = referencedId
      ? resolveType(referencedId, [...stack, logicalId])
      : undefined;

    if (declaredType && referencedType && declaredType !== referencedType) {
      throw new TypeError(
        `Type mismatch at ${logicalId}: declared ${declaredType}, referenced ${referencedType}.`,
      );
    }

    const effectiveType = record.token.$type ?? referencedType ?? record.inheritedType;
    typeCache.set(logicalId, effectiveType);
    return effectiveType;
  }

  function resolveToken(logicalId, stack = []) {
    if (valueCache.has(logicalId)) {
      return valueCache.get(logicalId);
    }
    if (stack.includes(logicalId)) {
      throw new Error(`Reference cycle: ${[...stack, logicalId].join(" -> ")}.`);
    }

    const record = requireToken(logicalId);
    const nextStack = [...stack, logicalId];
    let value;

    if (Object.hasOwn(record.token, "$ref")) {
      value = resolvePointerValue(record.token.$ref, nextStack);
    } else {
      value = resolveEmbeddedValue(record.token.$value, nextStack);
    }

    const resolved = {
      logicalId,
      type: resolveType(logicalId),
      value,
      token: record.token,
    };
    valueCache.set(logicalId, resolved);
    return resolved;
  }

  function resolveEmbeddedValue(value, stack) {
    const curlyMatch = typeof value === "string" ? value.match(CURLY_REFERENCE) : null;
    if (curlyMatch) {
      return structuredClone(resolveToken(curlyMatch[1], stack).value);
    }
    if (isReferenceObject(value)) {
      return resolvePointerValue(value.$ref, stack);
    }
    if (Array.isArray(value)) {
      return value.map((item) => resolveEmbeddedValue(item, stack));
    }
    if (isObject(value)) {
      return Object.fromEntries(
        Object.entries(value).map(([key, item]) => [key, resolveEmbeddedValue(item, stack)]),
      );
    }
    return value;
  }

  function resolvePointerValue(pointer, stack) {
    const segments = parsePointer(pointer);
    const targetTokenId = tokenTargetFromPointer(segments, materializedDocument);
    const target = readPointer(materializedDocument, segments, pointer);

    if (targetTokenId && isToken(target)) {
      return structuredClone(resolveToken(targetTokenId, stack).value);
    }
    return structuredClone(resolveEmbeddedValue(target, stack));
  }

  function requireToken(logicalId) {
    const record = tokens.get(logicalId);
    if (!record) {
      throw new ReferenceError(`Unresolved token reference ${logicalId}.`);
    }
    return record;
  }

  return { materializedDocument, tokens, resolveToken, resolveType };
}

function materializeExtensions(document) {
  const source = structuredClone(document);
  const groupCache = new Map();

  function resolveGroup(segments, stack = []) {
    const groupId = segments.join(".") || "<root>";
    if (groupCache.has(groupId)) {
      return structuredClone(groupCache.get(groupId));
    }
    if (stack.includes(groupId)) {
      throw new Error(`Extension cycle: ${[...stack, groupId].join(" -> ")}.`);
    }

    const rawGroup = readSegments(source, segments, groupId);
    if (!isObject(rawGroup) || isToken(rawGroup)) {
      throw new TypeError(`Extension target ${groupId} must be a group.`);
    }

    let result = {};
    if (rawGroup.$extends !== undefined) {
      const targetSegments = groupReferenceSegments(rawGroup.$extends);
      const target = readSegments(source, targetSegments, rawGroup.$extends);
      if (!isObject(target) || isToken(target)) {
        throw new TypeError(`$extends at ${groupId} must reference a group.`);
      }
      result = resolveGroup(targetSegments, [...stack, groupId]);
    }

    const local = {};
    for (const [key, value] of Object.entries(rawGroup)) {
      if (key === "$extends") {
        continue;
      }
      if (!key.startsWith("$") && !isToken(value)) {
        local[key] = resolveGroup([...segments, key], [...stack, groupId]);
      } else {
        local[key] = structuredClone(value);
      }
    }

    result = mergeGroupValues(result, local);
    groupCache.set(groupId, structuredClone(result));
    return result;
  }

  return resolveGroup([]);
}

function mergeGroupValues(base, local) {
  const result = structuredClone(base);
  for (const [key, value] of Object.entries(local)) {
    if (
      Object.hasOwn(result, key) &&
      isObject(result[key]) &&
      isObject(value) &&
      !isToken(result[key]) &&
      !isToken(value) &&
      !key.startsWith("$")
    ) {
      result[key] = mergeGroupValues(result[key], value);
    } else {
      result[key] = structuredClone(value);
    }
  }
  return result;
}

function wholeReferenceTarget(token, document) {
  if (Object.hasOwn(token, "$ref")) {
    const segments = parsePointer(token.$ref);
    const targetId = tokenTargetFromPointer(segments, document);
    if (!targetId) {
      return undefined;
    }
    const targetSegments = targetId.split(".");
    const suffix = segments.slice(targetSegments.length);
    return suffix.length === 0 || (suffix.length === 1 && suffix[0] === "$value")
      ? targetId
      : undefined;
  }

  const match = typeof token.$value === "string" ? token.$value.match(CURLY_REFERENCE) : null;
  return match?.[1];
}

function tokenTargetFromPointer(segments, document) {
  let current = document;
  for (let index = 0; index < segments.length; index += 1) {
    if (!isObject(current) && !Array.isArray(current)) {
      return undefined;
    }
    current = current[segments[index]];
    if (isToken(current)) {
      return segments.slice(0, index + 1).join(".");
    }
  }
  return undefined;
}

function groupReferenceSegments(reference) {
  if (typeof reference !== "string") {
    throw new TypeError("$extends must be a reference string.");
  }
  const curly = reference.match(CURLY_REFERENCE);
  if (curly) {
    return curly[1].split(".");
  }
  return parsePointer(reference);
}

function parsePointer(pointer) {
  if (typeof pointer !== "string" || !pointer.startsWith("#/")) {
    throw new TypeError(`Invalid JSON Pointer reference ${pointer}.`);
  }
  return pointer
    .slice(2)
    .split("/")
    .map((segment) => segment.replaceAll("~1", "/").replaceAll("~0", "~"));
}

function readPointer(document, segments, originalPointer) {
  return readSegments(document, segments, originalPointer);
}

function readSegments(document, segments, label) {
  let current = document;
  for (const segment of segments) {
    if (
      (typeof current !== "object" || current === null) ||
      !Object.hasOwn(current, segment)
    ) {
      throw new ReferenceError(`Unresolved reference ${label}.`);
    }
    current = current[segment];
  }
  return current;
}

function isReferenceObject(value) {
  return isObject(value) && typeof value.$ref === "string";
}

function isToken(value) {
  return isObject(value) && (Object.hasOwn(value, "$value") || Object.hasOwn(value, "$ref"));
}

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}
