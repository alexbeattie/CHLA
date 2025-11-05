// Regional Centers Data for LA County
// Used for landing pages and filtering

export const REGIONAL_CENTERS = {
  'san-gabriel-pomona': {
    name: 'San Gabriel/Pomona Regional Center',
    shortName: 'San Gabriel/Pomona RC',
    slug: 'san-gabriel-pomona',
    tagline: 'Serving Eastern LA County and the San Gabriel Valley',
    description: '<p><strong>San Gabriel/Pomona Regional Center</strong> provides comprehensive services to individuals with developmental disabilities in eastern Los Angeles County. With over 30,000 individuals served, we are committed to helping people with developmental disabilities achieve their fullest potential.</p><p>Our service area covers a diverse region including the San Gabriel Valley, eastern LA communities, and portions of the Pomona Valley.</p>',
    color: '#004877',
    colorDark: '#003355',
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
    cities: [
      'Pomona', 'Claremont', 'La Verne', 'San Dimas', 'Glendora', 
      'Azusa', 'Covina', 'West Covina', 'Baldwin Park', 'Irwindale',
      'Duarte', 'Monrovia', 'Arcadia', 'Temple City', 'Rosemead',
      'San Gabriel', 'Alhambra', 'Monterey Park', 'Montebello', 'Pico Rivera',
      'South El Monte', 'El Monte', 'La Puente', 'Walnut', 'Diamond Bar'
    ],
    zipCodes: [
      '91001', '91006', '91007', '91010', '91016', '91024', '91030', '91731',
      '91732', '91733', '91740', '91741', '91744', '91745', '91746', '91748',
      '91750', '91754', '91755', '91759', '91766', '91767', '91768', '91773',
      '91775', '91776', '91780', '91789', '91790', '91791', '91792'
    ]
  },
  
  'harbor': {
    name: 'Harbor Regional Center',
    shortName: 'Harbor RC',
    slug: 'harbor',
    tagline: 'Serving South Bay and Coastal Communities',
    description: '<p><strong>Harbor Regional Center</strong> serves individuals with developmental disabilities in the South Bay and coastal areas of Los Angeles County. We provide coordinated services and supports to help individuals achieve independence and community inclusion.</p><p>Our service area includes beach communities, the Palos Verdes Peninsula, and southern LA County.</p>',
    color: '#0D9DDB',
    colorDark: '#0877a3',
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
    cities: [
      'Torrance', 'Redondo Beach', 'Manhattan Beach', 'Hermosa Beach', 
      'El Segundo', 'Hawthorne', 'Gardena', 'Lawndale', 'Inglewood',
      'Carson', 'Long Beach (partial)', 'San Pedro', 'Wilmington',
      'Lomita', 'Palos Verdes Estates', 'Rancho Palos Verdes', 
      'Rolling Hills', 'Rolling Hills Estates'
    ],
    zipCodes: [
      '90245', '90250', '90254', '90260', '90261', '90266', '90274', '90275',
      '90277', '90278', '90290', '90291', '90292', '90293', '90301', '90302',
      '90303', '90304', '90305', '90501', '90502', '90503', '90504', '90505',
      '90710', '90731', '90732', '90744', '90745', '90746', '90747'
    ]
  },
  
  'north-la-county': {
    name: 'North Los Angeles County Regional Center',
    shortName: 'North LA County RC',
    slug: 'north-la-county',
    tagline: 'Serving Antelope Valley and Northern Communities',
    description: '<p><strong>North Los Angeles County Regional Center</strong> provides services throughout the northern region of LA County, including the Antelope Valley. We are dedicated to providing quality services and supports to enhance the lives of individuals with developmental disabilities.</p><p>Our service area includes high desert communities and northern LA County regions.</p>',
    color: '#4DAA50',
    colorDark: '#3a8a3f',
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
    cities: [
      'Lancaster', 'Palmdale', 'Santa Clarita', 'Canyon Country',
      'Newhall', 'Saugus', 'Valencia', 'Castaic', 'Stevenson Ranch',
      'Acton', 'Agua Dulce', 'Lake Los Angeles'
    ],
    zipCodes: [
      '91310', '91321', '91350', '91351', '91354', '91355', '91380', '91381',
      '91382', '91383', '91384', '91385', '91386', '91387', '91390', '93510',
      '93532', '93534', '93535', '93536', '93543', '93544', '93550', '93551',
      '93552', '93553', '93560', '93563', '93591'
    ]
  },
  
  'eastern-la': {
    name: 'Eastern Los Angeles Regional Center',
    shortName: 'Eastern LA RC',
    slug: 'eastern-la',
    tagline: 'Serving Communities East of Downtown LA',
    description: '<p><strong>Eastern Los Angeles Regional Center</strong> serves individuals with developmental disabilities east of downtown Los Angeles. We provide culturally sensitive services to diverse communities throughout eastern LA County.</p><p>Our bilingual staff serve predominantly Latino communities with culturally appropriate supports and services.</p>',
    color: '#FF7F00',
    colorDark: '#cc6600',
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
    cities: [
      'Alhambra', 'Bell', 'Bell Gardens', 'Commerce', 'Cudahy',
      'Downey', 'East Los Angeles', 'Huntington Park', 'Maywood',
      'Montebello', 'Monterey Park', 'Norwalk', 'Pico Rivera',
      'South Gate', 'Vernon', 'Whittier'
    ],
    zipCodes: [
      '90022', '90023', '90040', '90058', '90201', '90220', '90221', '90222',
      '90240', '90241', '90242', '90255', '90270', '90280', '90601', '90602',
      '90603', '90604', '90605', '90606', '90607', '90608', '90638', '90640',
      '90650', '90660', '90670', '90680', '91754', '91755', '91770', '91803'
    ]
  },
  
  'south-central-la': {
    name: 'South Central Los Angeles Regional Center',
    shortName: 'South Central LA RC',
    slug: 'south-central-la',
    tagline: 'Serving South-Central LA Communities',
    description: '<p><strong>South Central Los Angeles Regional Center</strong> provides services to individuals with developmental disabilities in south-central Los Angeles. We are committed to culturally sensitive, quality services that promote independence and community inclusion.</p><p>We serve diverse communities in south-central LA with comprehensive support services.</p>',
    color: '#805791',
    colorDark: '#5f4169',
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
    cities: [
      'Los Angeles (South Central)', 'Watts', 'Compton', 'Lynwood',
      'Paramount', 'South Los Angeles', 'Florence-Graham',
      'West Athens', 'Westmont', 'Willowbrook'
    ],
    zipCodes: [
      '90001', '90002', '90003', '90007', '90011', '90018', '90037', '90044',
      '90047', '90059', '90061', '90062', '90089', '90220', '90221', '90222',
      '90262', '90280', '90303', '90304', '90305', '90723'
    ]
  },
  
  'westside': {
    name: 'Westside Regional Center',
    shortName: 'Westside RC',
    slug: 'westside',
    tagline: 'Serving West LA, Santa Monica, and Surrounding Areas',
    description: '<p><strong>Westside Regional Center</strong> provides services to individuals with developmental disabilities on the Westside of Los Angeles County. We offer comprehensive services designed to help individuals live as independently as possible.</p><p>Our service area includes affluent west LA communities and coastal areas with access to extensive resources and services.</p>',
    color: '#EA1D36',
    colorDark: '#b71629',
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
    cities: [
      'Santa Monica', 'Culver City', 'West Los Angeles', 'Mar Vista',
      'Venice', 'Playa del Rey', 'Marina del Rey', 'Westwood',
      'Brentwood', 'Pacific Palisades', 'Malibu', 'Beverly Hills',
      'West Hollywood', 'Century City'
    ],
    zipCodes: [
      '90024', '90025', '90026', '90027', '90028', '90029', '90031', '90033',
      '90034', '90035', '90036', '90038', '90039', '90046', '90048', '90049',
      '90056', '90064', '90066', '90067', '90068', '90069', '90073', '90077',
      '90094', '90095', '90210', '90211', '90212', '90230', '90232', '90263',
      '90265', '90272', '90291', '90292', '90293', '90401', '90402', '90403',
      '90404', '90405'
    ]
  },
  
  'lanterman': {
    name: 'Frank D. Lanterman Regional Center',
    shortName: 'Lanterman RC',
    slug: 'lanterman',
    tagline: 'Serving San Fernando and San Gabriel Valleys',
    description: '<p><strong>Frank D. Lanterman Regional Center</strong> serves individuals with developmental disabilities in parts of the San Fernando and San Gabriel Valleys. Named after Frank Lanterman, the father of California\'s developmental services system, we continue his legacy of advocacy and service.</p><p>We provide comprehensive services to diverse communities across northwest LA County.</p>',
    color: '#097C8A',
    colorDark: '#065e68',
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
    cities: [
      'Burbank', 'Glendale', 'Pasadena', 'Altadena', 'La Cañada Flintridge',
      'San Fernando', 'North Hollywood', 'Studio City', 'Sherman Oaks',
      'Van Nuys', 'Encino', 'Tarzana', 'Woodland Hills', 'Canoga Park',
      'Reseda', 'Northridge', 'Chatsworth', 'Granada Hills', 'Porter Ranch',
      'Sylmar'
    ],
    zipCodes: [
      '90004', '90005', '90006', '90010', '90012', '90013', '90014', '90015',
      '90017', '90019', '90020', '90021', '90057', '91001', '91006', '91007',
      '91011', '91020', '91040', '91042', '91101', '91103', '91104', '91105',
      '91106', '91107', '91108', '91201', '91202', '91203', '91204', '91205',
      '91206', '91207', '91208', '91214', '91302', '91303', '91304', '91306',
      '91307', '91311', '91316', '91324', '91325', '91326', '91330', '91331',
      '91335', '91340', '91342', '91343', '91344', '91345', '91352', '91356',
      '91364', '91367', '91401', '91402', '91403', '91405', '91406', '91423'
    ]
  }
};

export const getAllRegionalCenters = () => {
  return Object.values(REGIONAL_CENTERS);
};

export const getRegionalCenterBySlug = (slug) => {
  return REGIONAL_CENTERS[slug];
};

export const getRegionalCenterByZip = (zipCode) => {
  const rc = Object.values(REGIONAL_CENTERS).find(rc => 
    rc.zipCodes.includes(zipCode)
  );
  return rc || null;
};

