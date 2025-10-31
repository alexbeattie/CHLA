/**
 * Color Utility Functions
 * Pure functions for color conversions and generation
 */

/**
 * Convert HSL color to hexadecimal string
 * @param {number} h - Hue (0-360)
 * @param {number} s - Saturation (0-100)
 * @param {number} l - Lightness (0-100)
 * @returns {string} Hex color string (e.g., "#ff5733")
 */
export function hslToHex(h, s, l) {
  const sNorm = s / 100;
  const lNorm = l / 100;
  const k = (n) => (n + h / 30) % 12;
  const a = sNorm * Math.min(lNorm, 1 - lNorm);
  const f = (n) =>
    lNorm - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));
  const toHex = (x) =>
    Math.round(x * 255)
      .toString(16)
      .padStart(2, "0");
  return `#${toHex(f(0))}${toHex(f(8))}${toHex(f(4))}`;
}

/**
 * Generate a consistent color from a string (e.g., ZIP code)
 * Uses string hashing to create a deterministic hue
 * @param {string} str - String to hash
 * @param {number} saturation - Saturation (0-100), default 45
 * @param {number} lightness - Lightness (0-100), default 65
 * @returns {string} Hex color string
 */
export function stringToColor(str, saturation = 45, lightness = 65) {
  let hue = 0;
  for (let i = 0; i < str.length; i++) {
    hue = (hue * 31 + str.charCodeAt(i)) % 360;
  }
  return hslToHex(hue, saturation, lightness);
}

