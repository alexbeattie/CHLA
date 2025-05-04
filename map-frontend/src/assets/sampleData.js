// Sample categories
export const sampleCategories = [
  { id: 1, name: 'Healthcare' },
  { id: 2, name: 'Education' },
  { id: 3, name: 'Recreation' },
  { id: 4, name: 'Support Groups' },
  { id: 5, name: 'Therapy Services' }
];

// Sample locations
export const sampleLocations = [
  {
    id: 1,
    name: "Children's Hospital Los Angeles",
    address: "4650 Sunset Blvd",
    city: "Los Angeles",
    state: "CA",
    zip_code: "90027",
    latitude: 34.0977,
    longitude: -118.2913,
    description: "Premier pediatric hospital offering comprehensive care",
    category: 1,
    category_name: "Healthcare",
    rating: 4.8,
    phone: "(323) 660-2450",
    email: "info@chla.org",
    website: "https://www.chla.org"
  },
  {
    id: 2,
    name: "UCLA Center for Autism",
    address: "300 UCLA Medical Plaza",
    city: "Los Angeles",
    state: "CA",
    zip_code: "90095",
    latitude: 34.0665,
    longitude: -118.4438,
    description: "Specialized center for autism assessment and treatment",
    category: 5,
    category_name: "Therapy Services",
    rating: 4.5,
    phone: "(310) 825-0867",
    website: "https://www.semel.ucla.edu/autism"
  },
  {
    id: 3,
    name: "Therapy West",
    address: "8717 Venice Blvd",
    city: "Los Angeles",
    state: "CA",
    zip_code: "90034",
    latitude: 34.0276,
    longitude: -118.3826,
    description: "Pediatric therapy center offering OT, PT, and speech services",
    category: 5,
    category_name: "Therapy Services",
    rating: 4.7,
    phone: "(310) 837-3141",
    website: "https://therapywest.org"
  },
  {
    id: 4,
    name: "Special Education Resource Center",
    address: "333 S Beaudry Ave",
    city: "Los Angeles",
    state: "CA",
    zip_code: "90017",
    latitude: 34.0582,
    longitude: -118.2612,
    description: "Resources for families with children in special education",
    category: 2,
    category_name: "Education",
    rating: 4.2,
    phone: "(213) 241-6701",
    website: "https://achieve.lausd.net/sped"
  },
  {
    id: 5,
    name: "Shane's Inspiration Playground",
    address: "4800 Crystal Springs Dr",
    city: "Los Angeles",
    state: "CA",
    zip_code: "90027",
    latitude: 34.1222,
    longitude: -118.2944,
    description: "Inclusive playground accessible to all children",
    category: 3,
    category_name: "Recreation",
    rating: 4.9,
    website: "https://shanesinspiration.org"
  },
  {
    id: 6,
    name: "Autism Society Los Angeles",
    address: "4351 Valley Blvd",
    city: "Los Angeles",
    state: "CA",
    zip_code: "90032",
    latitude: 34.0732,
    longitude: -118.1768,
    description: "Support group for families affected by autism",
    category: 4,
    category_name: "Support Groups",
    rating: 4.6,
    phone: "(562) 804-5556",
    website: "https://autismsocietyla.org"
  }
];

// Sample user profile
export const sampleUserProfile = {
  id: 1,
  age: 7,
  address: "123 Main St, Los Angeles, CA 90012",
  diagnosis: "autism",
  diagnosis_display: "Autism",
  other_diagnosis: "",
  latitude: 34.0522,
  longitude: -118.2437
};