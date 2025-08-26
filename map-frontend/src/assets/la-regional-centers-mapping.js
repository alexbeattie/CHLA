// Los Angeles County Regional Centers Service Area Mapping
// Based on available information about service areas

const laRegionalCenters = {
  "Eastern Los Angeles Regional Center": {
    phone: "626-299-4700",
    address: "1000 S. Fremont Ave, Unit 35, Alhambra, CA 91803",
    website: "https://www.elarc.org",
    serviceAreas: [
      "Alhambra", "Bell", "Bell Gardens", "Commerce", "Cudahy", 
      "East Los Angeles", "Huntington Park", "Maywood", "Montebello",
      "South Gate", "Vernon"
    ],
    // Approximate zip codes based on service areas
    zipCodes: [
      // Alhambra
      "91801", "91802", "91803", "91804", "91896", "91899",
      // Bell
      "90201",
      // Bell Gardens
      "90201", "90202",
      // Commerce
      "90022", "90023", "90040", "90091",
      // Cudahy
      "90201",
      // East Los Angeles
      "90022", "90023", "90063",
      // Huntington Park
      "90255",
      // Maywood
      "90270",
      // Montebello
      "90640",
      // South Gate
      "90280",
      // Vernon
      "90058"
    ]
  },
  
  "Frank D. Lanterman Regional Center": {
    phone: "213-383-1300",
    address: "3303 Wilshire Blvd, Suite 700, Los Angeles, CA 90010",
    website: "https://www.lanterman.org",
    serviceAreas: [
      "Central Los Angeles", "Hollywood", "Glendale", "Burbank",
      "Pasadena", "Eagle Rock", "Highland Park"
    ],
    // Approximate zip codes based on service areas
    zipCodes: [
      // Central LA
      "90004", "90005", "90006", "90010", "90017", "90018", "90019", "90020",
      "90026", "90027", "90028", "90029", "90036", "90038", "90057",
      // Hollywood
      "90068", "90078", "90028", "90038", "90046",
      // Glendale
      "91201", "91202", "91203", "91204", "91205", "91206", "91207", "91208",
      "91209", "91210", "91214", "91221", "91222", "91224", "91225", "91226",
      // Burbank
      "91501", "91502", "91503", "91504", "91505", "91506", "91507", "91508",
      "91510", "91521", "91522", "91523", "91526",
      // Pasadena
      "91101", "91102", "91103", "91104", "91105", "91106", "91107", "91108",
      "91109", "91110", "91114", "91115", "91116", "91117", "91118", "91121",
      "91123", "91124", "91125", "91126", "91129", "91182", "91184", "91185",
      "91188", "91189", "91199",
      // Eagle Rock
      "90041", "90042",
      // Highland Park
      "90042", "90065"
    ]
  },
  
  "Harbor Regional Center": {
    phone: "310-540-1711",
    address: "21231 Hawthorne Blvd, Torrance, CA 90503",
    website: "https://www.harborrc.org",
    serviceAreas: [
      "Torrance", "Long Beach", "San Pedro", "Wilmington", "Carson",
      "Harbor City", "Lomita", "Palos Verdes", "Redondo Beach",
      "Manhattan Beach", "Hermosa Beach", "El Segundo", "Hawthorne",
      "Gardena", "Lawndale"
    ],
    // Approximate zip codes based on service areas
    zipCodes: [
      // Torrance
      "90501", "90502", "90503", "90504", "90505", "90506", "90507", "90508",
      "90509", "90510",
      // Long Beach (partial - southern areas)
      "90802", "90803", "90804", "90805", "90806", "90807", "90808", "90809",
      "90810", "90813", "90814", "90815", "90822", "90831", "90832", "90833",
      "90834", "90835", "90840", "90842", "90844", "90846", "90847", "90848",
      "90853", "90888", "90899",
      // San Pedro
      "90731", "90732", "90733", "90734",
      // Wilmington
      "90744", "90748",
      // Carson
      "90745", "90746", "90747", "90749", "90810", "90895",
      // Harbor City
      "90710",
      // Lomita
      "90717",
      // Palos Verdes Peninsula
      "90274", "90275",
      // Redondo Beach
      "90277", "90278",
      // Manhattan Beach
      "90266", "90267",
      // Hermosa Beach
      "90254",
      // El Segundo
      "90245",
      // Hawthorne
      "90250", "90251",
      // Gardena
      "90247", "90248", "90249",
      // Lawndale
      "90260", "90261"
    ]
  },
  
  "North Los Angeles Regional Center": {
    phone: "818-778-1900",
    address: "9200 Oakdale Ave, Suite 100, Chatsworth, CA 91311",
    website: "https://www.nlacrc.org",
    serviceAreas: [
      "San Fernando Valley", "Santa Clarita Valley", "Antelope Valley"
    ],
    // Approximate zip codes based on service areas
    zipCodes: [
      // San Fernando Valley
      "91301", "91302", "91303", "91304", "91305", "91306", "91307", "91308",
      "91309", "91310", "91311", "91313", "91316", "91324", "91325", "91326",
      "91327", "91328", "91329", "91330", "91331", "91333", "91334", "91335",
      "91337", "91340", "91341", "91342", "91343", "91344", "91345", "91346",
      "91350", "91351", "91352", "91353", "91354", "91355", "91356", "91357",
      "91364", "91365", "91367", "91371", "91372", "91380", "91381", "91382",
      "91383", "91384", "91385", "91386", "91387", "91390", "91392", "91393",
      "91394", "91395", "91396", "91401", "91402", "91403", "91404", "91405",
      "91406", "91407", "91408", "91409", "91410", "91411", "91412", "91413",
      "91416", "91423", "91426", "91436", "91470", "91482", "91495", "91496",
      "91499", "91601", "91602", "91603", "91604", "91605", "91606", "91607",
      "91608", "91609", "91610", "91611", "91612", "91614", "91615", "91616",
      "91617", "91618",
      // Santa Clarita Valley
      "91321", "91350", "91351", "91354", "91355", "91380", "91381", "91382",
      "91383", "91384", "91385", "91386", "91387", "91390",
      // Antelope Valley
      "93534", "93535", "93536", "93543", "93544", "93550", "93551", "93552",
      "93553", "93591"
    ]
  },
  
  "San Gabriel/Pomona Regional Center": {
    phone: "909-620-7722",
    address: "75 Rancho Camino Drive, Pomona, CA 91766",
    website: "https://www.sgprc.org",
    serviceAreas: [
      "San Gabriel Valley (eastern portion)", "Pomona Valley", "Diamond Bar",
      "Claremont", "La Verne", "San Dimas", "Glendora", "Covina",
      "West Covina", "Baldwin Park", "El Monte", "South El Monte",
      "Temple City", "Rosemead", "San Gabriel", "Monterey Park"
    ],
    // Approximate zip codes based on service areas
    zipCodes: [
      // Pomona
      "91766", "91767", "91768", "91769",
      // Diamond Bar
      "91765", "91789",
      // Claremont
      "91711",
      // La Verne
      "91750",
      // San Dimas
      "91773",
      // Glendora
      "91740", "91741",
      // Covina
      "91722", "91723", "91724",
      // West Covina
      "91790", "91791", "91792", "91793",
      // Baldwin Park
      "91706",
      // El Monte
      "91731", "91732", "91733", "91734", "91735",
      // South El Monte
      "91733",
      // Temple City
      "91780",
      // Rosemead
      "91770", "91771", "91772",
      // San Gabriel
      "91775", "91776", "91778",
      // Monterey Park
      "91754", "91755", "91756"
    ]
  },
  
  "South Central Los Angeles Regional Center": {
    phone: "213-744-7000",
    address: "2500 S. Western Ave, Los Angeles, CA 90018",
    website: "https://www.sclarc.org",
    serviceAreas: [
      "South Los Angeles", "Watts", "Compton", "Lynwood", "Inglewood",
      "Lennox", "Hyde Park", "Crenshaw", "Baldwin Hills"
    ],
    // Approximate zip codes based on service areas
    zipCodes: [
      // South LA
      "90001", "90002", "90003", "90007", "90008", "90011", "90015", "90016",
      "90018", "90037", "90043", "90044", "90047", "90056", "90059", "90061",
      "90062", "90089",
      // Watts
      "90002", "90059", "90061",
      // Compton
      "90220", "90221", "90222", "90223", "90224",
      // Lynwood
      "90262",
      // Inglewood
      "90301", "90302", "90303", "90304", "90305", "90306", "90307", "90308",
      "90309", "90310", "90311", "90312", "90313", "90397", "90398",
      // Lennox
      "90304",
      // Hyde Park
      "90043", "90305",
      // Crenshaw
      "90008", "90016", "90018", "90019", "90043", "90056",
      // Baldwin Hills
      "90008", "90016", "90056"
    ]
  },
  
  "Westside Regional Center": {
    phone: "310-258-4000",
    address: "5901 Green Valley Circle, Suite 320, Culver City, CA 90230",
    website: "https://www.westsiderc.org",
    serviceAreas: [
      "West Los Angeles", "Santa Monica", "Culver City", "Marina del Rey",
      "Venice", "Mar Vista", "Del Rey", "Playa del Rey", "Playa Vista",
      "Westchester", "Cheviot Hills", "Pico-Robertson", "Beverly Hills",
      "West Hollywood", "Brentwood", "Pacific Palisades", "Malibu"
    ],
    // Approximate zip codes based on service areas
    zipCodes: [
      // West LA
      "90024", "90025", "90049", "90064", "90066", "90067", "90073", "90077",
      "90095",
      // Santa Monica
      "90401", "90402", "90403", "90404", "90405", "90406", "90407", "90408",
      "90409", "90410", "90411",
      // Culver City
      "90230", "90231", "90232", "90233",
      // Marina del Rey
      "90292", "90295",
      // Venice
      "90291", "90294",
      // Mar Vista
      "90066",
      // Del Rey
      "90066",
      // Playa del Rey
      "90293",
      // Playa Vista
      "90094",
      // Westchester
      "90045",
      // Cheviot Hills
      "90034", "90035", "90064",
      // Pico-Robertson
      "90034", "90035",
      // Beverly Hills
      "90209", "90210", "90211", "90212", "90213",
      // West Hollywood
      "90046", "90048", "90069",
      // Brentwood
      "90049", "90272",
      // Pacific Palisades
      "90272",
      // Malibu
      "90263", "90264", "90265"
    ]
  }
};

// Function to find Regional Center by zip code
function findRegionalCenterByZip(zipCode) {
  for (const [centerName, centerData] of Object.entries(laRegionalCenters)) {
    if (centerData.zipCodes.includes(zipCode)) {
      return {
        name: centerName,
        phone: centerData.phone,
        address: centerData.address,
        website: centerData.website
      };
    }
  }
  return null;
}

// Convert to GeoJSON format (you'll need to add actual coordinates for each zip code)
// This is a template structure
const regionalCentersGeoJSON = {
  "type": "FeatureCollection",
  "features": Object.entries(laRegionalCenters).map(([name, data]) => ({
    "type": "Feature",
    "properties": {
      "name": name,
      "phone": data.phone,
      "address": data.address,
      "serviceAreas": data.serviceAreas,
      "zipCodes": data.zipCodes
    },
    "geometry": {
      "type": "MultiPolygon",
      "coordinates": [] // You'll need to add actual polygon coordinates for each zip code area
    }
  }))
};

// Export the data
export { laRegionalCenters, findRegionalCenterByZip, regionalCentersGeoJSON };