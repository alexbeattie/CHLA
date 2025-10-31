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

