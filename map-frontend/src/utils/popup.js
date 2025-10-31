/**
 * Popup HTML Generation Utilities
 * Functions for creating Mapbox popup HTML content
 */

import { formatDescription, formatInsurance, formatLanguages, formatHours } from './formatting.js';

/**
 * Helper function to check if data exists and is not empty/null
 * @param {*} value - Value to check
 * @returns {boolean}
 */
function hasData(value) {
  return value && value !== "[]" && value !== "null" && value !== "" && value !== "{}";
}

/**
 * Create simple popup HTML content for a provider/location
 * @param {Object} item - Provider or location object
 * @returns {string} HTML string for popup content
 */
export function createSimplePopup(item) {
  console.log("Creating simple popup for item:", item);
  
  const title = item.name || item.regional_center || "Location";
  const phone = item.phone || item.telephone;
  const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${item.latitude},${item.longitude}`;

  // Handle address formatting
  let fullAddress = "";
  if (item.address || item.city || item.state || item.zip_code) {
    try {
      if (item.address && typeof item.address === "string" && item.address.startsWith("{")) {
        const addressData = JSON.parse(item.address);
        if (typeof addressData === "object") {
          fullAddress = [addressData.street, addressData.city, addressData.state, addressData.zip]
            .filter(Boolean)
            .join(", ");
        }
      } else {
        fullAddress = [item.address, item.city, item.state, item.zip_code]
          .filter(Boolean)
          .join(", ");
      }
    } catch (e) {
      fullAddress = [item.address, item.city, item.state, item.zip_code]
        .filter(Boolean)
        .join(", ");
    }
  }

  return `
    <div class="provider-popup" style="
      padding: 24px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      max-width: 360px;
      background: white;
      border-radius: 8px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      overflow: visible;
    ">
      <!-- Header -->
      <div style="
        border-bottom: 1px solid #dee2e6;
        padding-bottom: 16px;
        margin-bottom: 20px;
      ">
        <h5 style="
          margin: 0 0 8px 0;
          color: #212529;
          font-size: 18px;
          font-weight: 600;
          line-height: 1.3;
        ">${title}</h5>
        ${item.type && String(item.type).toLowerCase() !== "main" ? `
          <span style="
            background: #f8f9fa;
            color: #6c757d;
            padding: 3px 8px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: 0.3px;
          ">${item.type}</span>
        ` : ""}
      </div>

      <!-- Content -->
      <div style="margin-bottom: 20px;">
        ${fullAddress ? `
          <div style="
            display: flex;
            align-items: flex-start;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Address</span>
            <div style="color: #212529; font-size: 14px; line-height: 1.4;">${fullAddress}</div>
          </div>
        ` : ""}

        ${phone ? `
          <div style="
            display: flex;
            align-items: center;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Phone</span>
            <a href="tel:${String(phone).replace(/[^\d+]/g, "")}" style="
              color: #004877;
              text-decoration: none;
              font-size: 14px;
              font-weight: 500;
            ">${phone}</a>
          </div>
        ` : ""}

        ${hasData(item.website) ? `
          <div style="
            display: flex;
            align-items: center;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Website</span>
            <a href="${item.website.startsWith("http") ? item.website : "https://" + item.website}" 
               target="_blank" 
               rel="noopener" 
               style="
                 color: #004877;
                 text-decoration: none;
                 font-size: 14px;
                 font-weight: 500;
               ">${item.website.replace(/^https?:\/\//, "").replace(/^www\./, "")}</a>
          </div>
        ` : ""}

        ${hasData(item.hours) ? `
          <div style="
            display: flex;
            align-items: flex-start;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Hours</span>
            <div style="color: #212529; font-size: 14px; line-height: 1.4; white-space: pre-wrap;">${formatHours(item.hours)}</div>
          </div>
        ` : ""}

        ${hasData(item.description) ? `
          <div style="
            display: flex;
            align-items: flex-start;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Services</span>
            <div style="color: #212529; font-size: 14px; line-height: 1.4;">${formatDescription(item.description)}</div>
          </div>
        ` : ""}

        ${hasData(item.insurance_accepted) ? `
          <div style="
            display: flex;
            align-items: flex-start;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Insurance</span>
            <div style="color: #212529; font-size: 14px; line-height: 1.4;">${formatInsurance(item.insurance_accepted)}</div>
          </div>
        ` : ""}

        ${hasData(item.languages_spoken) ? `
          <div style="
            display: flex;
            align-items: flex-start;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Languages</span>
            <div style="color: #212529; font-size: 14px; line-height: 1.4;">${formatLanguages(item.languages_spoken)}</div>
          </div>
        ` : ""}

        ${item.distance ? `
          <div style="
            display: flex;
            align-items: center;
            gap: 16px;
            margin-bottom: 16px;
            padding: 0;
          ">
            <span style="
              color: #6c757d;
              font-size: 13px;
              font-weight: 500;
              min-width: 70px;
              text-transform: uppercase;
              letter-spacing: 0.3px;
            ">Distance</span>
            <div style="color: #212529; font-size: 14px; font-weight: 500;">${item.distance.toFixed(1)} miles</div>
          </div>
        ` : ""}
      </div>

      <!-- Actions -->
      <div style="
        display: flex;
        gap: 8px;
        margin-top: 20px;
        border-top: 1px solid #dee2e6;
        padding-top: 16px;
      ">
        <a href="${mapsUrl}" target="_blank" style="
          background: #004877;
          color: white;
          padding: 8px 16px;
          border-radius: 4px;
          text-decoration: none;
          font-size: 13px;
          font-weight: 500;
          flex: 1;
          text-align: center;
          transition: background-color 0.2s;
        " onmouseover="this.style.background='#003861'" onmouseout="this.style.background='#004877'">
          Directions
        </a>

        ${phone ? `
          <a href="tel:${phone}" style="
            background: #6c757d;
            color: white;
            padding: 8px 16px;
            border-radius: 4px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 500;
            flex: 1;
            text-align: center;
            transition: background-color 0.2s;
          " onmouseover="this.style.background='#5a6268'" onmouseout="this.style.background='#6c757d'">
            Call
          </a>
        ` : ""}

        ${hasData(item.website) ? `
          <a href="${item.website.startsWith("http") ? item.website : "https://" + item.website}" target="_blank" style="
            background: #6c757d;
            color: white;
            padding: 8px 16px;
            border-radius: 4px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 500;
            flex: 1;
            text-align: center;
            transition: background-color 0.2s;
          " onmouseover="this.style.background='#5a6268'" onmouseout="this.style.background='#6c757d'">
            Website
          </a>
        ` : ""}
      </div>
    </div>
  `;
}

/**
 * Create Regional Center popup HTML content
 * @param {string} name - Regional center name
 * @param {Object} options - Optional data sources
 * @param {Object} options.serviceAreas - Service areas GeoJSON
 * @param {Array} options.regionalCenters - Regional centers array
 * @returns {string} HTML string for popup content
 */
export function createRegionalCenterPopup(name, { serviceAreas = null, regionalCenters = [] } = {}) {
  console.log("Creating popup for regional center:", name);

  // Hardcoded data for LA Regional Centers
  const regionalCenterData = {
    "North Los Angeles County Regional Center": {
      address: "15400 Sherman Way, Suite 170",
      city: "Van Nuys",
      state: "CA",
      zip_code: "91406",
      telephone: "(818) 778-1900",
      website: "https://www.nlacrc.org",
    },
    "San Gabriel/Pomona Regional Center": {
      address: "75 Rancho Camino Drive",
      city: "Pomona",
      state: "CA",
      zip_code: "91766",
      telephone: "(909) 620-7722",
      website: "https://www.sgprc.org",
    },
    "Eastern Los Angeles Regional Center": {
      address: "1000 S. Fremont Ave",
      city: "Alhambra",
      state: "CA",
      zip_code: "91803",
      telephone: "(626) 299-4700",
      website: "https://www.elarc.org",
    },
    "Westside Regional Center": {
      address: "5901 Green Valley Circle, Suite 320",
      city: "Culver City",
      state: "CA",
      zip_code: "90230",
      telephone: "(310) 258-4000",
      website: "https://www.westsiderc.org",
    },
    "Frank D. Lanterman Regional Center": {
      address: "3303 Wilshire Blvd., Suite 700",
      city: "Los Angeles",
      state: "CA",
      zip_code: "90010",
      telephone: "(213) 383-1300",
      website: "https://www.lanterman.org",
    },
    "South Central Los Angeles Regional Center": {
      address: "2500 S. Western Avenue",
      city: "Los Angeles",
      state: "CA",
      zip_code: "90018",
      telephone: "(213) 744-7000",
      website: "https://www.sclarc.org",
    },
    "Harbor Regional Center": {
      address: "21231 Hawthorne Boulevard",
      city: "Torrance",
      state: "CA",
      zip_code: "90503",
      telephone: "(310) 540-1711",
      website: "https://www.harborrc.org",
    },
  };

  // Get the hardcoded data for this regional center
  const hardcodedData = regionalCenterData[name] || {};

  // Attempt to find matching regional center from serviceAreas for richer metadata
  let rc = null;
  try {
    const features = serviceAreas?.features || [];
    rc = features.find(
      (f) => f?.properties?.regional_center?.toLowerCase() === name?.toLowerCase()
    )?.properties;
  } catch {}

  // Also check regionalCenters array for more complete data
  const rcData = regionalCenters?.find(
    (r) => r.regional_center?.toLowerCase() === name?.toLowerCase()
  );

  // Merge data sources, preferring rcData, then rc, then hardcodedData
  const phone = rcData?.telephone || rc?.telephone || hardcodedData.telephone || "";
  const address = rcData?.address || rc?.address || hardcodedData.address || "";
  const city = rcData?.city || rc?.city || hardcodedData.city || "";
  const state = rcData?.state || rc?.state || hardcodedData.state || "";
  const zip = rcData?.zip_code || rc?.zip_code || hardcodedData.zip_code || "";
  const fullAddress = [address, city, state, zip].filter(Boolean).join(", ");

  let website = rcData?.website || rc?.website || hardcodedData.website || "";
  if (website && !website.startsWith("http")) {
    website = `https://${website}`;
  }

  const lat = rcData?.latitude || rc?.latitude;
  const lng = rcData?.longitude || rc?.longitude;

  // Clean phone for tel link
  const phoneClean = phone ? phone.replace(/[^\d+]/g, "") : "";

  // Get website hostname for display
  let websiteDisplay = "";
  if (website) {
    try {
      const u = new URL(website);
      websiteDisplay = u.hostname.replace(/^www\./, "");
    } catch (_) {
      websiteDisplay = website.replace(/^https?:\/\//, "").replace(/^www\./, "");
    }
  }

  console.log("Regional center data found:", { name, phone, address, website });

  return `
    <div class="provider-popup" style="
      padding: 16px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      max-width: 320px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    ">
      <!-- Header -->
      <div style="
        border-bottom: 2px solid #f8f9fa;
        padding-bottom: 12px;
        margin-bottom: 16px;
      ">
        <h5 style="
          margin: 0 0 4px 0;
          color: #2c3e50;
          font-size: 18px;
          font-weight: 700;
          line-height: 1.3;
        ">${name}</h5>
      </div>

      <!-- Content -->
      <div style="margin-bottom: 16px;">
        ${
          fullAddress
            ? `
          <div style="
            margin-bottom: 12px;
            padding: 8px 12px;
            background: #f8f9fa;
            border-radius: 8px;
            border-left: 3px solid #007bff;
          ">
            <div style="
              color: #495057;
              font-size: 13px;
              font-weight: 500;
              margin-bottom: 2px;
            ">📍 Address</div>
            <div style="color: #6c757d; font-size: 14px;">${fullAddress}</div>
          </div>
        `
            : ""
        }

        ${
          phone
            ? `
          <div style="
            margin-bottom: 12px;
            padding: 8px 12px;
            background: #f8f9fa;
            border-radius: 8px;
            border-left: 3px solid #28a745;
          ">
            <div style="
              color: #495057;
              font-size: 13px;
              font-weight: 500;
              margin-bottom: 2px;
            ">📞 Phone</div>
            <div style="color: #6c757d; font-size: 14px;">
              <a href="tel:${phoneClean}" style="color:#0d6efd; text-decoration:none;">${phone}</a>
            </div>
          </div>
        `
            : ""
        }

        ${
          website
            ? `
          <div style="
            margin-bottom: 12px;
            padding: 8px 12px;
            background: #f8f9fa;
            border-radius: 8px;
            border-left: 3px solid #0d6efd;
          ">
            <div style="
              color: #495057;
              font-size: 13px;
              font-weight: 500;
              margin-bottom: 2px;
            ">🌐 Website</div>
            <div style="color: #6c757d; font-size: 14px;">
              <a href="${website}" target="_blank" rel="noopener" style="color:#0d6efd; text-decoration:none;">${websiteDisplay}</a>
            </div>
          </div>
        `
            : ""
        }
      </div>

      <!-- Actions -->
      <div style="
        display: flex;
        gap: 8px;
        margin-top: 16px;
        border-top: 1px solid #f8f9fa;
        padding-top: 16px;
      ">
        ${
          fullAddress || (lat && lng)
            ? `
          <a href="https://www.google.com/maps/dir/?api=1&destination=${
            lat && lng ? `${lat},${lng}` : encodeURIComponent(fullAddress)
          }" target="_blank" style="
            background: #007bff;
            color: white;
            padding: 10px 16px;
            border-radius: 8px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            flex: 1;
            text-align: center;
            transition: background-color 0.2s;
          " onmouseover="this.style.background='#0056b3'" onmouseout="this.style.background='#007bff'">
            🗺️ Directions
          </a>
        `
            : ""
        }

        ${
          phone
            ? `
          <a href="tel:${phoneClean}" style="
            background: #28a745;
            color: white;
            padding: 10px 16px;
            border-radius: 8px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            flex: 1;
            text-align: center;
            transition: background-color 0.2s;
          " onmouseover="this.style.background='#1e7e34'" onmouseout="this.style.background='#28a745'">
            📞 Call
          </a>
        `
            : ""
        }

        ${
          website
            ? `
          <a href="${website}" target="_blank" style="
            background: #6f42c1;
            color: white;
            padding: 10px 16px;
            border-radius: 8px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            flex: 1;
            text-align: center;
            transition: background-color 0.2s;
          " onmouseover="this.style.background='#5a2d91'" onmouseout="this.style.background='#6f42c1'">
            🌐 Website
          </a>
        `
            : ""
        }
      </div>
    </div>
  `;
}

