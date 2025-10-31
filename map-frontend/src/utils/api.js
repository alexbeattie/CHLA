/**
 * API Utility Functions
 * Pure functions for API interactions
 */

/**
 * Get the API base URL from environment variable
 * @returns {string} API base URL
 */
export function getApiRoot() {
  return import.meta.env.VITE_API_BASE_URL || "";
}

