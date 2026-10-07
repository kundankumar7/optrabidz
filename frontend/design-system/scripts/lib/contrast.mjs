export function contrastRatio(foreground, background) {
  assertSrgbColor(foreground, "foreground");
  assertSrgbColor(background, "background");

  if ((background.alpha ?? 1) !== 1) {
    throw new TypeError("Contrast background must be opaque.");
  }

  const foregroundAlpha = foreground.alpha ?? 1;
  const compositedForeground = foreground.components.map(
    (component, index) =>
      component * foregroundAlpha + background.components[index] * (1 - foregroundAlpha),
  );
  const lighter = Math.max(
    relativeLuminance(compositedForeground),
    relativeLuminance(background.components),
  );
  const darker = Math.min(
    relativeLuminance(compositedForeground),
    relativeLuminance(background.components),
  );

  return (lighter + 0.05) / (darker + 0.05);
}

function relativeLuminance(components) {
  const [red, green, blue] = components.map((component) =>
    component <= 0.04045
      ? component / 12.92
      : ((component + 0.055) / 1.055) ** 2.4,
  );
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

function assertSrgbColor(value, label) {
  if (
    !value ||
    value.colorSpace !== "srgb" ||
    !Array.isArray(value.components) ||
    value.components.length !== 3 ||
    !value.components.every(
      (component) => typeof component === "number" && component >= 0 && component <= 1,
    ) ||
    (value.alpha !== undefined &&
      (typeof value.alpha !== "number" || value.alpha < 0 || value.alpha > 1))
  ) {
    throw new TypeError(`Contrast ${label} must be a resolved DTCG sRGB color.`);
  }
}
