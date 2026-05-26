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
  const typeLabel = item.type || item.category_name || "Service";

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
    <article class="kindd-map-popup">
      <div class="kindd-map-popup__eyebrow">
        <span class="kindd-map-popup__icon">
          <i class="bi bi-hospital-fill" aria-hidden="true"></i>
        </span>
        <span>${typeLabel}</span>
      </div>

      <h3 class="kindd-map-popup__title">${name}</h3>
      
      ${address ? `
        <p class="kindd-map-popup__line">
          <i class="bi bi-geo-alt-fill" aria-hidden="true"></i>
          <span>${address}</span>
        </p>
      ` : ''}
      
      ${phone || website ? `
        <div class="kindd-map-popup__meta">
          ${phone ? `
            <a href="tel:${phoneClean}" class="kindd-map-popup__link">
              <i class="bi bi-telephone-fill" aria-hidden="true"></i>
              <span>${phone}</span>
            </a>
          ` : ''}
          ${website ? `
            <a href="${website}" target="_blank" rel="noopener noreferrer" class="kindd-map-popup__link">
              <i class="bi bi-globe" aria-hidden="true"></i>
              <span>Website</span>
            </a>
          ` : ''}
        </div>
      ` : ''}
      
      ${item.distance ? `
        <p class="kindd-map-popup__distance">${item.distance.toFixed(1)} miles away</p>
      ` : ''}
      
      <div class="kindd-map-popup__actions">
        <a href="${googleMapsUrl}" target="_blank" rel="noopener" class="kindd-map-popup__button kindd-map-popup__button--primary">Google Maps</a>
        <a href="${appleMapsUrl}" target="_blank" rel="noopener" class="kindd-map-popup__button kindd-map-popup__button--dark">Apple Maps</a>
        ${phone ? `
          <a href="tel:${phoneClean}" class="kindd-map-popup__button kindd-map-popup__button--success">Call Now</a>
        ` : ''}
      </div>
    </article>
  `;
}

