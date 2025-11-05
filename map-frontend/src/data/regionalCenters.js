// Regional Centers Data for LA County
// Used for landing pages and filtering

export const REGIONAL_CENTERS = {
  'san-gabriel-pomona': {
    name: 'San Gabriel/Pomona Regional Center',
    shortName: 'San Gabriel/Pomona RC',
    slug: 'san-gabriel-pomona',
    tagline: 'Serving Eastern LA County and the San Gabriel Valley',
    description: '<p><strong>San Gabriel/Pomona Regional Center</strong> provides comprehensive services to individuals with developmental disabilities in eastern Los Angeles County. With over 30,000 individuals served, we are committed to helping people with developmental disabilities achieve their fullest potential.</p><p>Our service area covers a diverse region including the San Gabriel Valley, eastern LA communities, and portions of the Pomona Valley.</p>',
    color: '#4caf50',
    colorDark: '#3a8a3f',
    providerCount: 78,
    citiesCount: 25,
    population: '1.2M',
    phone: '626-299-4700',
    phoneFormatted: '(626) 299-4700',
    website: 'https://www.sgprc.org',
    address: '75 Rancho Camino Drive, Pomona, CA 91766',
    highlights: [
      'Largest Regional Center in LA County',
      'Bilingual services (English/Spanish)',
      'Family Resource Centers',
      'Early intervention programs',
      'Transition services for young adults',
      'Respite care programs'
    ],
    // cities and zipCodes now fetched from API
    cities: [],
    zipCodes: []
  },
  
  'harbor': {
    name: 'Harbor Regional Center',
    shortName: 'Harbor RC',
    slug: 'harbor',
    tagline: 'Serving South Bay and Coastal Communities',
    description: '<p><strong>Harbor Regional Center</strong> serves individuals with developmental disabilities in the South Bay and coastal areas of Los Angeles County. We provide coordinated services and supports to help individuals achieve independence and community inclusion.</p><p>Our service area includes beach communities, the Palos Verdes Peninsula, and southern LA County.</p>',
    color: '#2196f3',
    colorDark: '#1976d2',
    providerCount: 45,
    citiesCount: 18,
    population: '900K',
    phone: '310-540-1711',
    phoneFormatted: '(310) 540-1711',
    website: 'https://www.harborrc.org',
    address: '21231 Hawthorne Blvd, Torrance, CA 90503',
    highlights: [
      'Coastal and South Bay coverage',
      'Family support services',
      'Transition planning',
      'Employment services',
      'Respite care',
      'Behavior intervention programs'
    ],
    // cities and zipCodes now fetched from API
    cities: [],
    zipCodes: []
  },
  
  'north-la-county': {
    name: 'North Los Angeles County Regional Center',
    shortName: 'North LA County RC',
    slug: 'north-la-county',
    tagline: 'Serving Antelope Valley and Northern Communities',
    description: '<p><strong>North Los Angeles County Regional Center</strong> provides services throughout the northern region of LA County, including the Antelope Valley. We are dedicated to providing quality services and supports to enhance the lives of individuals with developmental disabilities.</p><p>Our service area includes high desert communities and northern LA County regions.</p>',
    color: '#f1c40f',
    colorDark: '#c9950f',
    providerCount: 32,
    citiesCount: 12,
    population: '650K',
    phone: '661-267-2300',
    phoneFormatted: '(661) 267-2300',
    website: 'https://www.nlacrc.org',
    address: '15400 Sherman Way, Suite 170, Van Nuys, CA 91406',
    highlights: [
      'Antelope Valley services',
      'Rural service delivery',
      'Family support programs',
      'Early Start services',
      'Community integration',
      'Employment development'
    ],
    // cities and zipCodes now fetched from API
    cities: [],
    zipCodes: []
  },
  
  'eastern-la': {
    name: 'Eastern Los Angeles Regional Center',
    shortName: 'Eastern LA RC',
    slug: 'eastern-la',
    tagline: 'Serving Communities East of Downtown LA',
    description: '<p><strong>Eastern Los Angeles Regional Center</strong> serves individuals with developmental disabilities east of downtown Los Angeles. We provide culturally sensitive services to diverse communities throughout eastern LA County.</p><p>Our bilingual staff serve predominantly Latino communities with culturally appropriate supports and services.</p>',
    color: '#ff9800',
    colorDark: '#e68a00',
    providerCount: 52,
    citiesCount: 15,
    population: '800K',
    phone: '562-806-5400',
    phoneFormatted: '(562) 806-5400',
    website: 'https://www.elarc.org',
    address: '1000 S. Fremont Ave., Building A-9 East, Alhambra, CA 91803',
    highlights: [
      'Bilingual/bicultural services',
      'Latino community focus',
      'Family Resource Centers',
      'Parent training programs',
      'Community living support',
      'Vocational services'
    ],
    // cities and zipCodes now fetched from API
    cities: [],
    zipCodes: []
  },
  
  'south-central-la': {
    name: 'South Central Los Angeles Regional Center',
    shortName: 'South Central LA RC',
    slug: 'south-central-la',
    tagline: 'Serving South-Central LA Communities',
    description: '<p><strong>South Central Los Angeles Regional Center</strong> provides services to individuals with developmental disabilities in south-central Los Angeles. We are committed to culturally sensitive, quality services that promote independence and community inclusion.</p><p>We serve diverse communities in south-central LA with comprehensive support services.</p>',
    color: '#f44336',
    colorDark: '#d32f2f',
    providerCount: 48,
    citiesCount: 10,
    population: '700K',
    phone: '213-744-7000',
    phoneFormatted: '(213) 744-7000',
    website: 'https://www.sclarc.org',
    address: '2500 W. Century Blvd., Los Angeles, CA 90047',
    highlights: [
      'Urban service delivery',
      'Multicultural services',
      'Family empowerment programs',
      'Early intervention',
      'Community supports',
      'Crisis intervention'
    ],
    // cities and zipCodes now fetched from API
    cities: [],
    zipCodes: []
  },
  
  'westside': {
    name: 'Westside Regional Center',
    shortName: 'Westside RC',
    slug: 'westside',
    tagline: 'Serving West LA, Santa Monica, and Surrounding Areas',
    description: '<p><strong>Westside Regional Center</strong> provides services to individuals with developmental disabilities on the Westside of Los Angeles County. We offer comprehensive services designed to help individuals live as independently as possible.</p><p>Our service area includes affluent west LA communities and coastal areas with access to extensive resources and services.</p>',
    color: '#e91e63',
    colorDark: '#c2185b',
    providerCount: 56,
    citiesCount: 14,
    population: '850K',
    phone: '310-258-4000',
    phoneFormatted: '(310) 258-4000',
    website: 'https://www.westsiderc.org',
    address: '5901 Green Valley Circle, Suite 320, Culver City, CA 90230',
    highlights: [
      'Westside community focus',
      'Inclusive recreation programs',
      'Supported employment',
      'Independent living services',
      'Family support network',
      'Transition programs'
    ],
    // cities and zipCodes now fetched from API
    cities: [],
    zipCodes: []
  },
  
  'lanterman': {
    name: 'Frank D. Lanterman Regional Center',
    shortName: 'Lanterman RC',
    slug: 'lanterman',
    tagline: 'Serving San Fernando and San Gabriel Valleys',
    description: '<p><strong>Frank D. Lanterman Regional Center</strong> serves individuals with developmental disabilities in parts of the San Fernando and San Gabriel Valleys. Named after Frank Lanterman, the father of California\'s developmental services system, we continue his legacy of advocacy and service.</p><p>We provide comprehensive services to diverse communities across northwest LA County.</p>',
    color: '#9c27b0',
    colorDark: '#7b1fa2',
    providerCount: 62,
    citiesCount: 20,
    population: '1.1M',
    phone: '213-383-1300',
    phoneFormatted: '(213) 383-1300',
    website: 'https://www.lanterman.org',
    address: '3303 Wilshire Blvd., Suite 700, Los Angeles, CA 90010',
    highlights: [
      'Named after Frank Lanterman',
      'Multi-valley coverage',
      'Diverse language services',
      'Self-determination programs',
      'Person-centered planning',
      'Innovation grants'
    ],
    // cities and zipCodes now fetched from API
    cities: [],
    zipCodes: []
  }
};

export const getAllRegionalCenters = () => {
  return Object.values(REGIONAL_CENTERS);
};

export const getRegionalCenterBySlug = (slug) => {
  return REGIONAL_CENTERS[slug];
};

// DEPRECATED: Use API query instead
// GET /api/regional-centers/by_zip_code/?zip_code={zip}
// This function uses hardcoded ZIP codes which may be out of date
// export const getRegionalCenterByZip = (zipCode) => {
//   const rc = Object.values(REGIONAL_CENTERS).find(rc => 
//     rc.zipCodes.includes(zipCode)
//   );
//   return rc || null;
// };

