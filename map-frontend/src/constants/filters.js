/**
 * Filter constants
 */

// IMPORTANT: Must match ProviderV2.DIAGNOSIS_CHOICES in models.py
// Note: Removed ADHD, Down Syndrome, Cerebral Palsy per user request
export const DIAGNOSIS_OPTIONS = [
  "Autism Spectrum Disorder",
  "Global Development Delay",
  "Intellectual Disability",
  "Speech and Language Disorder",
  "Sensory Processing Disorder",
  "Other",
];

// IMPORTANT: Must match ProviderV2.THERAPY_TYPE_CHOICES in models.py
export const THERAPY_OPTIONS = [
  "ABA therapy",
  "Speech therapy",
  "Occupational therapy",
  "Physical therapy",
  "Feeding therapy",
  "Parent child interaction therapy/parent training behavior management",
];

// IMPORTANT: Must match ProviderV2.INSURANCE_CHOICES in models.py
// Ordered by most common for developmental services in LA County
export const INSURANCE_OPTIONS = [
  "Medi-Cal",
  "Regional Center",
  "Blue Cross",
  "Blue Shield",
  "Anthem",
  "Aetna",
  "Cigna",
  "Kaiser Permanente",
  "United Healthcare",
  "Health Net",
  "L.A. Care",
  "CalOptima",
  "Molina",
  "Magellan",
  "Medicaid",
  "Medicare",
  "Beacon",
  "MHN",
  "Optum",
  "Humana",
  "Tricare",
  "Inland Empire Health Plan",
  "The Holman Group",
  "United Behavioral Health",
  "Covered California",
  "Self-determination programs",
  "Kaiser/Easterseal",
  // Note: "Private Pay" and "None" excluded - all providers accept private pay
];

export const LA_COUNTY_CENTER = {
  lat: 34.0522,
  lng: -118.2437,
};

export const LA_COUNTY_BOUNDS = [
  [-119.1, 33.3], // Southwest
  [-117.5, 34.9], // Northeast
];

