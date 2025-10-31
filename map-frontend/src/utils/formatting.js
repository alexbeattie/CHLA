/**
 * Formatting Utility Functions
 * Pure functions for formatting provider data
 */

/**
 * Format description text for better readability
 * @param {string} description - Raw description text
 * @returns {string} Formatted description
 */
export function formatDescription(description) {
  if (!description) return "";

  // Clean up the description text
  let cleanDescription = description;

  // Clean up formatting without removing directional words
  cleanDescription = cleanDescription
    .replace(/,/g, ", ") // Add space after commas
    .replace(/\s+/g, " ") // Normalize whitespace
    .trim();

  // Convert to proper title case (capitalize first letter of each word)
  cleanDescription = cleanDescription
    .toLowerCase()
    .replace(/\b\w/g, (l) => l.toUpperCase());

  return cleanDescription;
}

/**
 * Format insurance information for better readability
 * @param {string|Array|Object} insurance - Insurance data
 * @returns {string} Formatted insurance text
 */
export function formatInsurance(insurance) {
  if (!insurance) return "";

  try {
    // Try to parse as JSON first
    const parsed = JSON.parse(insurance);
    if (Array.isArray(parsed)) {
      return parsed.join(", ");
    } else if (typeof parsed === "object") {
      return Object.values(parsed).join(", ");
    }
  } catch (e) {
    // If not JSON, treat as string
  }

  // Clean up insurance text
  let cleanInsurance = insurance;

  // Remove all brackets, braces and quotes
  cleanInsurance = cleanInsurance
    .replace(/[\[\]{}]/g, "") // Remove all brackets and braces
    .replace(/['"]/g, "") // Remove all quotes
    .replace(/\s*,\s*/g, ", ") // Normalize comma spacing
    .replace(/\s+/g, " ") // Normalize whitespace
    .trim();

  return cleanInsurance;
}

/**
 * Format languages information for better readability
 * @param {string|Array|Object} languages - Languages data
 * @returns {string} Formatted languages text
 */
export function formatLanguages(languages) {
  if (!languages) return "";

  try {
    // Try to parse as JSON first
    const parsed = JSON.parse(languages);
    if (Array.isArray(parsed)) {
      return parsed.join(", ");
    } else if (typeof parsed === "object") {
      return Object.values(parsed).join(", ");
    }
  } catch (e) {
    // If not JSON, treat as string
  }

  // Clean up languages text
  let cleanLanguages = languages;

  // Remove all brackets, braces and quotes
  cleanLanguages = cleanLanguages
    .replace(/[\[\]{}]/g, "") // Remove all brackets and braces
    .replace(/['"]/g, "") // Remove all quotes
    .replace(/\s*,\s*/g, ", ") // Normalize comma spacing
    .replace(/\s+/g, " ") // Normalize whitespace
    .trim();

  return cleanLanguages;
}

/**
 * Format hours object into readable text
 * @param {Object} hoursObj - Hours object with day keys
 * @returns {string} Formatted hours text (newline-separated)
 */
export function formatHoursObject(hoursObj) {
  if (!hoursObj || typeof hoursObj !== 'object') {
    return "Hours not available";
  }
  
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  const formattedHours = [];
  
  days.forEach(day => {
    const dayHours = hoursObj[day.toLowerCase()] || hoursObj[day];
    if (dayHours && dayHours !== 'Closed' && dayHours !== '') {
      formattedHours.push(`${day}: ${dayHours}`);
    }
  });
  
  if (formattedHours.length === 0) {
    return "Hours not available";
  }
  
  return formattedHours.join('\n');
}

/**
 * Format hours data for display
 * @param {string|Object} hours - Hours data (string or object)
 * @returns {string} Formatted hours text
 */
export function formatHours(hours) {
  if (!hours) return "Hours not available";
  
  // If it's already a string, return it
  if (typeof hours === 'string') {
    return hours;
  }
  
  // If it's an object, try to format it nicely
  if (typeof hours === 'object') {
    try {
      // Try to parse as JSON if it's a stringified object
      if (typeof hours === 'string') {
        const parsed = JSON.parse(hours);
        return formatHoursObject(parsed);
      }
      
      // If it's already an object, format it
      return formatHoursObject(hours);
    } catch (e) {
      console.warn("Could not parse hours object:", hours);
      return "Hours not available";
    }
  }
  
  return "Hours not available";
}

