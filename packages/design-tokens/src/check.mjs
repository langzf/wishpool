import { colors, cssVariables, radius, spacing, typography } from "./index.js";

const requiredColors = [
  "primary",
  "onPrimary",
  "secondary",
  "accent",
  "background",
  "foreground",
  "muted",
  "border",
  "destructive",
  "ring"
];

for (const key of requiredColors) {
  if (!/^#[0-9A-F]{6}$/i.test(colors[key] ?? "")) {
    throw new Error(`Invalid color token: ${key}`);
  }
}

for (const [key, value] of Object.entries(spacing)) {
  if (!Number.isInteger(value) || value <= 0 || value % 4 !== 0) {
    throw new Error(`Spacing token must follow a 4px grid: ${key}`);
  }
}

for (const [key, value] of Object.entries(radius)) {
  if (!Number.isInteger(value) || value < 0) {
    throw new Error(`Invalid radius token: ${key}`);
  }
}

if (!typography.headingFamily.includes("Fredoka") || !typography.bodyFamily.includes("Nunito")) {
  throw new Error("Typography tokens must preserve the WishPool family type pairing.");
}

if (!cssVariables["--color-primary"] || !cssVariables["--font-body"]) {
  throw new Error("CSS variables must expose color and font tokens.");
}

console.log(`validated ${Object.keys(colors).length} color tokens`);
