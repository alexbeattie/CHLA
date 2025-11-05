/**
 * Minimal Popup for Provider Markers
 * Simple, lightweight popup without complex dependencies
 */

/**
 * Create minimal popup HTML for a provider/location
 * @param {Object} item - Provider or location object
 * @returns {string} HTML string for minimal popup
 */
export function createMinimalPopup(item) {
  const name = item.name || item.regional_center || "Location";
  const phone = item.phone || item.telephone || "";
  const website = item.website || "";
  const lat = item.latitude || 0;
  const lng = item.longitude || 0;
  
  // Format phone for tel: link
  const phoneClean = phone.replace(/[^\d+]/g, "");
  
  // Clean address formatting - handle JSON or string
  let address = "";
  if (item.address) {
    try {
      // Check if address is JSON string
      if (typeof item.address === "string" && item.address.trim().startsWith("{")) {
        const addressObj = JSON.parse(item.address);
        address = [addressObj.street, addressObj.city, addressObj.state, addressObj.zip]
          .filter(Boolean)
          .join(", ");
      } else {
        // Plain text address - clean it up
        address = item.address.replace(/\n/g, ", ").replace(/,\s*,/g, ",").trim();
      }
    } catch (e) {
      // If parsing fails, just use as-is and clean it
      address = item.address.replace(/\n/g, ", ").replace(/,\s*,/g, ",").trim();
    }
  }
  
  // Map URLs
  const googleMapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
  const appleMapsUrl = `https://maps.apple.com/?daddr=${lat},${lng}`;
  
  return `
    <div style="
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      padding: 12px;
      min-width: 200px;
      max-width: 280px;
    ">
      <div style="
        font-weight: 600;
        font-size: 15px;
        color: #212529;
        margin-bottom: 8px;
        line-height: 1.3;
      ">${name}</div>
      
      ${address ? `
        <div style="
          font-size: 13px;
          color: #6c757d;
          margin-bottom: 8px;
          line-height: 1.4;
        ">${address}</div>
      ` : ''}
      
      ${phone || website ? `
        <div style="
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 13px;
          margin-bottom: 8px;
          gap: 8px;
        ">
          ${phone ? `
            <a href="tel:${phoneClean}" style="
              color: #004877;
              text-decoration: none;
              font-weight: 500;
            ">📞 ${phone}</a>
          ` : '<span></span>'}
          ${website ? `
            <a href="${website}" target="_blank" rel="noopener noreferrer" style="
              color: #004877;
              text-decoration: none;
              font-weight: 500;
              white-space: nowrap;
            ">🌐 Website</a>
          ` : ''}
        </div>
      ` : ''}
      
      ${item.distance ? `
        <div style="
          font-size: 12px;
          color: #6c757d;
          margin-bottom: 8px;
        ">${item.distance.toFixed(1)} miles away</div>
      ` : ''}
      
      <div style="
        display: flex;
        flex-direction: column;
        gap: 6px;
        margin-top: 10px;
      ">
        <div style="
          display: flex;
          gap: 6px;
        ">
          <a href="${googleMapsUrl}" target="_blank" rel="noopener" style="
            background: #4285f4;
            color: white;
            padding: 6px 12px;
            border-radius: 4px;
            text-decoration: none;
            font-size: 11px;
            font-weight: 500;
            text-align: center;
            flex: 1;
          ">Google Maps</a>
          <a href="${appleMapsUrl}" target="_blank" rel="noopener" style="
            background: #000000;
            color: white;
            padding: 6px 12px;
            border-radius: 4px;
            text-decoration: none;
            font-size: 11px;
            font-weight: 500;
            text-align: center;
            flex: 1;
          ">Apple Maps</a>
        </div>
        ${phone ? `
          <a href="tel:${phoneClean}" style="
            background: #28a745;
            color: white;
            padding: 6px 12px;
            border-radius: 4px;
            text-decoration: none;
            font-size: 12px;
            font-weight: 500;
            text-align: center;
          ">📞 Call Now</a>
        ` : ''}
      </div>
    </div>
  `;
}

