#!/usr/bin/env python3
"""Build public/assets/geo/ca_regional_centers.geojson.

Every California regional center outside Los Angeles County serves whole
counties, so those service areas are county outlines merged per center. LA
County is split among seven centers; their boundaries come from
public/assets/geo/la_rc_7.geojson.

Usage (needs shapely):
  python3 map-frontend/scripts/build_ca_regional_centers.py
"""

import json
from pathlib import Path

from shapely.geometry import Polygon, mapping, shape
from shapely.ops import unary_union

REPO = Path(__file__).resolve().parents[2]
COUNTIES = REPO / "maplocation" / "ca_counties.geojson"
LA_CENTERS = REPO / "map-frontend" / "public" / "assets" / "geo" / "la_rc_7.geojson"
OUT = REPO / "map-frontend" / "public" / "assets" / "geo" / "ca_regional_centers.geojson"

# Catchment counties per DDS. Los Angeles County centers use la_rc_7 instead.
# Office rows mirror the regional_centers table; office_type "Field" means the
# table has no main office for that center.
CENTERS = [
    {"name": "Alta California Regional Center", "acronym": "ACRC", "color": "#26a69a",
     "counties": ["Alpine", "Colusa", "El Dorado", "Nevada", "Placer", "Sacramento", "Sierra", "Sutter", "Yolo", "Yuba"],
     "office": ("Main", "2241 Harvard Street, Ste. 100", "Sacramento", "95815", "(916) 978-6400", "altaregional.org", 38.606023, -121.437173)},
    {"name": "Central Valley Regional Center", "acronym": "CVRC", "color": "#8d6e63",
     "counties": ["Fresno", "Kings", "Madera", "Mariposa", "Merced", "Tulare"],
     "office": ("Field", "5441 West Cypress Avenue", "Visalia", "93277", "(559) 738-2200", "www.cvrc.org", 36.325155, -119.352463)},
    {"name": "Far Northern Regional Center", "acronym": "FNRC", "color": "#5c6bc0",
     "counties": ["Butte", "Glenn", "Lassen", "Modoc", "Plumas", "Shasta", "Siskiyou", "Tehama", "Trinity"],
     "office": ("Main", "1900 Churn Creek Road, Ste. 319", "Redding", "96002", "(530) 222-4791", "www.farnorthernrc.org", 40.579248, -122.354053)},
    {"name": "Golden Gate Regional Center", "acronym": "GGRC", "color": "#ffb300",
     "counties": ["Marin", "San Francisco", "San Mateo"],
     "office": ("Main", "1355 Market Street, Ste. 220", "San Francisco", "94103", "(415) 546-9222", "www.ggrc.org", 37.776948, -122.417006)},
    {"name": "Inland Regional Center", "acronym": "IRC", "color": "#ef6c00",
     "counties": ["Riverside", "San Bernardino"],
     "office": ("Main", "1365 South Waterman Avenue", "San Bernardino", "92408", "(909) 890-3000", "www.inlandrc.org", 34.075389, -117.278652)},
    {"name": "Kern Regional Center", "acronym": "KRC", "color": "#c0ca33",
     "counties": ["Inyo", "Kern", "Mono"],
     "office": ("Field", "825 North Downs, Ste. D", "Ridgecrest", "93555", "(760) 375-9512", "www.kernrc.org", 35.636997, -117.68775)},
    {"name": "North Bay Regional Center", "acronym": "NBRC", "color": "#7e57c2",
     "counties": ["Napa", "Solano", "Sonoma"],
     "office": ("Main", "610 Airpark Road", "Napa", "94558", "(707) 256-1100", "www.nbrc.net", 38.218198, -122.269063)},
    {"name": "Redwood Coast Regional Center", "acronym": "RCRC", "color": "#2e7d32",
     "counties": ["Del Norte", "Humboldt", "Lake", "Mendocino"],
     "office": ("Field", "270 Chestnut Street, Ste. A", "Fort Bragg", "95437", "(707) 964-6387", "www.redwoodcoastrc.org", 39.436304, -123.803333)},
    {"name": "Regional Center of the East Bay", "acronym": "RCEB", "color": "#00897b",
     "counties": ["Alameda", "Contra Costa"],
     "office": ("Main", "500 Davis Street, Ste. 100", "San Leandro", "94577", "(510) 618-6100", "www.rceb.org", 37.724223, -122.160823)},
    {"name": "Regional Center of Orange County", "acronym": "RCOC", "color": "#f57f17",
     "counties": ["Orange"],
     "office": ("Main", "1525 North Tustin Avenue", "Santa Ana", "92705", "(714) 796-5100", "www.rcocdd.com", 33.758623, -117.834155)},
    {"name": "San Andreas Regional Center", "acronym": "SARC", "color": "#039be5",
     "counties": ["Monterey", "San Benito", "Santa Clara", "Santa Cruz"],
     "office": ("Main", "300 Orchard City Drive, Ste. 170", "Campbell", "95008", "(408) 374-9960", "www.sarc.org", 37.286235, -121.944083)},
    {"name": "San Diego Regional Center", "acronym": "SDRC", "color": "#d81b60",
     "counties": ["Imperial", "San Diego"],
     "office": ("Field", "2727 Hoover Avenue, Ste. 100", "National City", "91950", "(619) 336-6600", "www.sdrc.org", 32.658389, -117.103578)},
    {"name": "Tri-Counties Regional Center", "acronym": "TCRC", "color": "#6d4c41",
     "counties": ["San Luis Obispo", "Santa Barbara", "Ventura"],
     "office": ("Main", "520 East Montecito Street", "Santa Barbara", "93103", "(805) 962-7881", "www.tri-counties.org", 34.420707, -119.686748)},
    {"name": "Valley Mountain Regional Center", "acronym": "VMRC", "color": "#43a047",
     "counties": ["Amador", "Calaveras", "San Joaquin", "Stanislaus", "Tuolumne"],
     "office": ("Main", "702 North Aurora Street", "Stockton", "95202", "(209) 473-0951", "www.vmrc.net", 37.96191, -121.281678)},
    {"name": "Eastern Los Angeles Regional Center", "acronym": "ELARC", "color": "#ff9800", "slug": "eastern-la",
     "office": ("Main", "1000 South Fremont Avenue", "Alhambra", "91802", "(626) 299-4700", "www.elarc.org", 34.080352, -118.151849)},
    {"name": "Frank D. Lanterman Regional Center", "acronym": "FDLRC", "color": "#9c27b0", "slug": "lanterman",
     "office": ("Main", "3303 Wilshire Blvd., Ste. 700", "Los Angeles", "90010", "(213) 383-1300", "www.lanterman.org", 34.061772, -118.294495)},
    {"name": "Harbor Regional Center", "acronym": "HRC", "color": "#2196f3", "slug": "harbor",
     "office": ("Main", "21231 Hawthorne Blvd.", "Torrance", "90503", "(310) 540-1711", "www.harborrc.org", 33.836732, -118.353594)},
    {"name": "North Los Angeles County Regional Center", "acronym": "NLACRC", "color": "#f1c40f", "slug": "north-la-county",
     "office": ("Main", "15400 Sherman Way, Ste. 170", "Van Nuys", "91406", "(818) 778-1900", "www.nlacrc.org", 34.201126, -118.468492)},
    {"name": "San Gabriel/Pomona Regional Center", "acronym": "SG/PRC", "color": "#4caf50", "slug": "san-gabriel-pomona",
     "office": ("Main", "75 Rancho Camino Drive", "Pomona", "91766", "(909) 620-7722", "www.sgprc.org", 34.027688, -117.756777)},
    {"name": "South Central Los Angeles Regional Center", "acronym": "SCLARC", "color": "#f44336", "slug": "south-central-la",
     "office": ("Main", "2500 South Western Avenue", "Los Angeles", "90018", "(213) 744-7000", "www.sclarc.org", 34.032682, -118.308972)},
    {"name": "Westside Regional Center", "acronym": "WRC", "color": "#e91e63", "slug": "westside",
     "office": ("Main", "5901 Green Valley Circle, Ste. 320", "Culver City", "90230", "(310) 258-4000", "www.westsiderc.org", 33.983957, -118.392)},
]

COUNTY_TOLERANCE = 0.004
# ca_counties.geojson leaves hairline gaps between neighbors; SEAM closes
# them. Pieces smaller than MIN_PART (deg^2) are slivers, not islands.
SEAM = 0.003
MIN_PART = 0.00005
LA_TOLERANCE = 0.0008


def solid(geom):
    parts = [Polygon(p.exterior) for p in getattr(geom, "geoms", [geom]) if p.area >= MIN_PART]
    return unary_union(parts)


def rounded(geom, places=5):
    def walk(c):
        if isinstance(c[0], (int, float)):
            return [round(c[0], places), round(c[1], places)]
        return [walk(x) for x in c]

    g = mapping(geom)
    return {"type": g["type"], "coordinates": walk(g["coordinates"])}


def main():
    counties = {f["properties"]["name"]: shape(f["geometry"]).buffer(0) for f in json.load(COUNTIES.open())["features"]}
    la = {f["properties"]["REGIONALCENTER"]: shape(f["geometry"]).buffer(0) for f in json.load(LA_CENTERS.open())["features"]}

    assigned = [c for rc in CENTERS for c in rc.get("counties", [])]
    missing = set(counties) - set(assigned) - {"Los Angeles"}
    dupes = {c for c in assigned if assigned.count(c) > 1}
    assert not missing and not dupes, (missing, dupes)

    features = []
    for rc in CENTERS:
        if "counties" in rc:
            geom = solid(unary_union([counties[c].buffer(SEAM) for c in rc["counties"]]).buffer(-SEAM))
            geom = geom.simplify(COUNTY_TOLERANCE, preserve_topology=True)
            counties_served = rc["counties"]
        else:
            geom = la[rc["name"]].simplify(LA_TOLERANCE, preserve_topology=True)
            counties_served = ["Los Angeles"]
        office_type, address, city, zip_code, phone, website, lat, lng = rc["office"]
        features.append({
            "type": "Feature",
            "properties": {
                "name": rc["name"],
                "acronym": rc["acronym"],
                "color": rc["color"],
                "slug": rc.get("slug"),
                "counties": counties_served,
                "officeType": office_type,
                "address": address,
                "city": city,
                "zip": zip_code,
                "phone": phone,
                "website": "https://" + website.removeprefix("https://").removeprefix("http://"),
                "officeLng": lng,
                "officeLat": lat,
            },
            "geometry": rounded(geom),
        })

    OUT.write_text(json.dumps({"type": "FeatureCollection", "features": features}, separators=(",", ":")))
    print(f"wrote {OUT.relative_to(REPO)}: {len(features)} centers, {OUT.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
