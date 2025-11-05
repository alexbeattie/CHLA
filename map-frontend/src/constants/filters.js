/**
 * Filter constants
 */

// IMPORTANT: Must match ProviderV2.DIAGNOSIS_CHOICES in models.py
export const DIAGNOSIS_OPTIONS = [
  "Autism Spectrum Disorder",
  "Global Development Delay",
  "Intellectual Disability",
  "Speech and Language Disorder",
  "ADHD",
  "Sensory Processing Disorder",
  "Down Syndrome",
  "Cerebral Palsy",
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

export const LA_COUNTY_CENTER = {
  lat: 34.0522,
  lng: -118.2437,
};

export const LA_COUNTY_BOUNDS = [
  [-119.1, 33.3], // Southwest
  [-117.5, 34.9], // Northeast
];

