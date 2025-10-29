/**
 * Popups Composable
 * Handles popup HTML generation and formatting for map markers
 * Extracted from MapView.vue to reduce component complexity
 */

import type { Provider } from '@/stores/providerStore';

export interface PopupItem {
  name?: string;
  regional_center?: string;
  title?: string;
  type?: string;
  phone?: string;
  telephone?: string;
  latitude: number;
  longitude: number;
  address?: string | object;
  city?: string;
  state?: string;
  zip_code?: string;
  description?: string;
  insurance?: string | string[];
  languages?: string | string[];
  hours?: string | object;
  website?: string;
  email?: string;
  [key: string]: any;
}

export interface RegionalCenterData {
  name: string;
  phone?: string;
  website?: string;
  service_area?: string;
  description?: string;
}

export function usePopups() {
  /**
   * Format hours data for display
   */
  const formatHours = (hours: any): string => {
    if (!hours) return "Hours not available";

    // If it's already a string, return it
    if (typeof hours === 'string') {
      return hours;
    }

    // If it's an object, try to format it nicely
    if (typeof hours === 'object') {
      try {
        return formatHoursObject(hours);
      } catch (e) {
        console.warn("Could not parse hours object:", hours);
        return "Hours not available";
      }
    }

    return "Hours not available";
  };

  /**
   * Format hours object into readable text
   */
  const formatHoursObject = (hoursObj: any): string => {
    if (!hoursObj || typeof hoursObj !== 'object') {
      return "Hours not available";
    }

    const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
    const formattedHours: string[] = [];

    days.forEach(day => {
      const dayHours = hoursObj[day.toLowerCase()] || hoursObj[day];
      if (dayHours && dayHours !== 'Closed' && dayHours !== '') {
        formattedHours.push(`${day}: ${dayHours}`);
      }
    });

    if (formattedHours.length === 0) {
      return "Hours not available";
    }

    return formattedHours.join('\n');
  };

  /**
   * Format description text
   */
  const formatDescription = (description: any): string => {
    if (!description) return "";

    // If it's an array, join with newlines
    if (Array.isArray(description)) {
      return description.join("\n");
    }

    // If it's a string, return it
    if (typeof description === "string") {
      return description;
    }

    return "";
  };

  /**
   * Format insurance information
   */
  const formatInsurance = (insurance: any): string => {
    if (!insurance) return "Not specified";

    // If it's an array, join with commas
    if (Array.isArray(insurance)) {
      return insurance.filter(Boolean).join(", ") || "Not specified";
    }

    // If it's a string that looks like JSON array
    if (typeof insurance === "string") {
      try {
        const parsed = JSON.parse(insurance);
        if (Array.isArray(parsed)) {
          return parsed.filter(Boolean).join(", ") || "Not specified";
        }
        return insurance;
      } catch (e) {
        return insurance;
      }
    }

    return "Not specified";
  };

  /**
   * Format languages spoken
   */
  const formatLanguages = (languages: any): string => {
    if (!languages) return "Not specified";

    // If it's an array, join with commas
    if (Array.isArray(languages)) {
      return languages.filter(Boolean).join(", ") || "Not specified";
    }

    // If it's a string that looks like JSON array
    if (typeof languages === "string") {
      try {
        const parsed = JSON.parse(languages);
        if (Array.isArray(parsed)) {
          return parsed.filter(Boolean).join(", ") || "Not specified";
        }
        return languages;
      } catch (e) {
        return languages;
      }
    }

    return "Not specified";
  };

  /**
   * Helper function to check if data exists and is not empty/null
   */
  const hasData = (value: any): boolean => {
    return value && value !== "[]" && value !== "null" && value !== "" && value !== "{}";
  };

  /**
   * Format full address from item data
   */
  const formatAddress = (item: PopupItem): string => {
    if (!item.address && !item.city && !item.state && !item.zip_code) {
      return "";
    }

    try {
      // Handle JSON-encoded address object
      if (item.address && typeof item.address === "string" && item.address.startsWith("{")) {
        const addressData = JSON.parse(item.address);
        if (typeof addressData === "object") {
          return [addressData.street, addressData.city, addressData.state, addressData.zip]
            .filter(Boolean)
            .join(", ");
        }
      }

      // Handle normal address fields
      return [item.address, item.city, item.state, item.zip_code]
        .filter(Boolean)
        .join(", ");
    } catch (e) {
      return [item.address, item.city, item.state, item.zip_code]
        .filter(Boolean)
        .join(", ");
    }
  };

  /**
   * Create simple popup content for providers
   */
  const createSimplePopup = (item: PopupItem): string => {
    console.log("Creating simple popup for item:", item);

    const title = item.name || item.regional_center || "Location";
    const phone = item.phone || item.telephone;
    const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${item.latitude},${item.longitude}`;
    const fullAddress = formatAddress(item);

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
            ">
              <span style="
                color: #6c757d;
                font-size: 13px;
                font-weight: 500;
                min-width: 70px;
                text-transform: uppercase;
                letter-spacing: 0.3px;
              ">Phone</span>
              <a href="tel:${phone}" style="
                color: #0d6efd;
                text-decoration: none;
                font-size: 14px;
                font-weight: 500;
              ">${phone}</a>
            </div>
          ` : ""}

          ${item.website ? `
            <div style="
              display: flex;
              align-items: center;
              gap: 16px;
              margin-bottom: 16px;
            ">
              <span style="
                color: #6c757d;
                font-size: 13px;
                font-weight: 500;
                min-width: 70px;
                text-transform: uppercase;
                letter-spacing: 0.3px;
              ">Website</span>
              <a href="${item.website}" target="_blank" style="
                color: #0d6efd;
                text-decoration: none;
                font-size: 14px;
                font-weight: 500;
              ">Visit Website</a>
            </div>
          ` : ""}

          ${hasData(item.insurance) ? `
            <div style="
              display: flex;
              align-items: flex-start;
              gap: 16px;
              margin-bottom: 16px;
            ">
              <span style="
                color: #6c757d;
                font-size: 13px;
                font-weight: 500;
                min-width: 70px;
                text-transform: uppercase;
                letter-spacing: 0.3px;
              ">Insurance</span>
              <div style="color: #212529; font-size: 14px; line-height: 1.4;">${formatInsurance(item.insurance)}</div>
            </div>
          ` : ""}

          ${hasData(item.languages) ? `
            <div style="
              display: flex;
              align-items: flex-start;
              gap: 16px;
              margin-bottom: 16px;
            ">
              <span style="
                color: #6c757d;
                font-size: 13px;
                font-weight: 500;
                min-width: 70px;
                text-transform: uppercase;
                letter-spacing: 0.3px;
              ">Languages</span>
              <div style="color: #212529; font-size: 14px; line-height: 1.4;">${formatLanguages(item.languages)}</div>
            </div>
          ` : ""}

          ${hasData(item.hours) ? `
            <div style="
              display: flex;
              align-items: flex-start;
              gap: 16px;
              margin-bottom: 16px;
            ">
              <span style="
                color: #6c757d;
                font-size: 13px;
                font-weight: 500;
                min-width: 70px;
                text-transform: uppercase;
                letter-spacing: 0.3px;
              ">Hours</span>
              <div style="
                color: #212529;
                font-size: 14px;
                line-height: 1.6;
                white-space: pre-line;
              ">${formatHours(item.hours)}</div>
            </div>
          ` : ""}

          ${item.description ? `
            <div style="
              margin-top: 16px;
              padding-top: 16px;
              border-top: 1px solid #dee2e6;
            ">
              <div style="
                color: #495057;
                font-size: 14px;
                line-height: 1.6;
                white-space: pre-line;
              ">${formatDescription(item.description)}</div>
            </div>
          ` : ""}
        </div>

        <!-- Actions -->
        <div style="
          display: flex;
          gap: 8px;
          padding-top: 16px;
          border-top: 1px solid #dee2e6;
        ">
          <a href="${mapsUrl}" target="_blank" style="
            flex: 1;
            padding: 10px 16px;
            background: #0d6efd;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 500;
            text-align: center;
            transition: background 0.2s;
          ">Get Directions</a>
        </div>
      </div>
    `;
  };

  /**
   * Create regional center popup content
   */
  const createRegionalCenterPopup = (name: string, data?: RegionalCenterData): string => {
    // Hardcoded regional center data (TODO: move to constants or API)
    const regionalCenterDetails: Record<string, RegionalCenterData> = {
      "Harbor": {
        name: "Harbor Regional Center",
        phone: "(310) 540-1711",
        website: "https://www.harborrc.org/",
        service_area: "South Bay, Long Beach, and surrounding areas",
        description: "Serving individuals with developmental disabilities in the Harbor area."
      },
      "South Central Los Angeles": {
        name: "South Central Los Angeles Regional Center",
        phone: "(213) 744-7000",
        website: "https://www.sclarc.org/",
        service_area: "South Los Angeles and surrounding communities",
        description: "Providing services to individuals with developmental disabilities in South LA."
      },
      "Westside": {
        name: "Westside Regional Center",
        phone: "(310) 258-4000",
        website: "https://www.westsiderc.org/",
        service_area: "West Los Angeles, Santa Monica, Malibu",
        description: "Serving the Westside communities with developmental disability services."
      },
      "Eastern Los Angeles": {
        name: "Eastern Los Angeles Regional Center",
        phone: "(626) 299-4700",
        website: "https://www.elarc.org/",
        service_area: "East LA, San Gabriel Valley, Pomona Valley",
        description: "Supporting individuals with developmental disabilities in Eastern LA County."
      },
      "North Los Angeles County": {
        name: "North Los Angeles County Regional Center",
        phone: "(818) 778-1900",
        website: "https://www.nlacrc.org/",
        service_area: "San Fernando Valley, Santa Clarita Valley",
        description: "Providing services to North LA County communities."
      },
      "San Gabriel/Pomona": {
        name: "San Gabriel/Pomona Regional Center",
        phone: "(626) 854-3000",
        website: "https://www.sgprc.org/",
        service_area: "San Gabriel Valley and Pomona Valley",
        description: "Serving the eastern communities of Los Angeles County."
      },
      "Frank D. Lanterman": {
        name: "Frank D. Lanterman Regional Center",
        phone: "(213) 383-1300",
        website: "https://www.lanterman.org/",
        service_area: "Central and Northeast LA",
        description: "One of the largest regional centers serving LA County."
      }
    };

    const centerData = data || regionalCenterDetails[name] || {
      name: name,
      phone: "Contact information not available",
      website: "",
      service_area: "Service area information not available",
      description: ""
    };

    return `
      <div class="regional-center-popup" style="
        padding: 24px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
        max-width: 400px;
        background: white;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      ">
        <!-- Header -->
        <div style="
          border-bottom: 2px solid #0d6efd;
          padding-bottom: 16px;
          margin-bottom: 20px;
        ">
          <h4 style="
            margin: 0 0 4px 0;
            color: #212529;
            font-size: 20px;
            font-weight: 700;
            line-height: 1.3;
          ">${centerData.name}</h4>
          <span style="
            background: #e7f1ff;
            color: #0d6efd;
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
          ">Regional Center</span>
        </div>

        <!-- Content -->
        <div style="margin-bottom: 20px;">
          ${centerData.service_area ? `
            <div style="
              background: #f8f9fa;
              padding: 12px 16px;
              border-radius: 6px;
              margin-bottom: 16px;
            ">
              <div style="
                color: #6c757d;
                font-size: 11px;
                font-weight: 600;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                margin-bottom: 6px;
              ">Service Area</div>
              <div style="
                color: #212529;
                font-size: 14px;
                line-height: 1.5;
                font-weight: 500;
              ">${centerData.service_area}</div>
            </div>
          ` : ""}

          ${centerData.phone ? `
            <div style="
              display: flex;
              align-items: center;
              gap: 12px;
              margin-bottom: 12px;
            ">
              <svg width="16" height="16" fill="#6c757d" viewBox="0 0 16 16">
                <path d="M3.654 1.328a.678.678 0 0 0-1.015-.063L1.605 2.3c-.483.484-.661 1.169-.45 1.77a17.568 17.568 0 0 0 4.168 6.608 17.569 17.569 0 0 0 6.608 4.168c.601.211 1.286.033 1.77-.45l1.034-1.034a.678.678 0 0 0-.063-1.015l-2.307-1.794a.678.678 0 0 0-.58-.122l-2.19.547a1.745 1.745 0 0 1-1.657-.459L5.482 8.062a1.745 1.745 0 0 1-.46-1.657l.548-2.19a.678.678 0 0 0-.122-.58L3.654 1.328zM1.884.511a1.745 1.745 0 0 1 2.612.163L6.29 2.98c.329.423.445.974.315 1.494l-.547 2.19a.678.678 0 0 0 .178.643l2.457 2.457a.678.678 0 0 0 .644.178l2.189-.547a1.745 1.745 0 0 1 1.494.315l2.306 1.794c.829.645.905 1.87.163 2.611l-1.034 1.034c-.74.74-1.846 1.065-2.877.702a18.634 18.634 0 0 1-7.01-4.42 18.634 18.634 0 0 1-4.42-7.009c-.362-1.03-.037-2.137.703-2.877L1.885.511z"/>
              </svg>
              <a href="tel:${centerData.phone}" style="
                color: #0d6efd;
                text-decoration: none;
                font-size: 15px;
                font-weight: 500;
              ">${centerData.phone}</a>
            </div>
          ` : ""}

          ${centerData.website ? `
            <div style="
              display: flex;
              align-items: center;
              gap: 12px;
              margin-bottom: 12px;
            ">
              <svg width="16" height="16" fill="#6c757d" viewBox="0 0 16 16">
                <path d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8zm7.5-6.923c-.67.204-1.335.82-1.887 1.855A7.97 7.97 0 0 0 5.145 4H7.5V1.077zM4.09 4a9.267 9.267 0 0 1 .64-1.539 6.7 6.7 0 0 1 .597-.933A7.025 7.025 0 0 0 2.255 4H4.09zm-.582 3.5c.03-.877.138-1.718.312-2.5H1.674a6.958 6.958 0 0 0-.656 2.5h2.49zM4.847 5a12.5 12.5 0 0 0-.338 2.5H7.5V5H4.847zM8.5 5v2.5h2.99a12.495 12.495 0 0 0-.337-2.5H8.5zM4.51 8.5a12.5 12.5 0 0 0 .337 2.5H7.5V8.5H4.51zm3.99 0V11h2.653c.187-.765.306-1.608.338-2.5H8.5zM5.145 12c.138.386.295.744.468 1.068.552 1.035 1.218 1.65 1.887 1.855V12H5.145zm.182 2.472a6.696 6.696 0 0 1-.597-.933A9.268 9.268 0 0 1 4.09 12H2.255a7.024 7.024 0 0 0 3.072 2.472zM3.82 11a13.652 13.652 0 0 1-.312-2.5h-2.49c.062.89.291 1.733.656 2.5H3.82zm6.853 3.472A7.024 7.024 0 0 0 13.745 12H11.91a9.27 9.27 0 0 1-.64 1.539 6.688 6.688 0 0 1-.597.933zM8.5 12v2.923c.67-.204 1.335-.82 1.887-1.855.173-.324.33-.682.468-1.068H8.5zm3.68-1h2.146c.365-.767.594-1.61.656-2.5h-2.49a13.65 13.65 0 0 1-.312 2.5zm2.802-3.5a6.959 6.959 0 0 0-.656-2.5H12.18c.174.782.282 1.623.312 2.5h2.49zM11.27 2.461c.247.464.462.98.64 1.539h1.835a7.024 7.024 0 0 0-3.072-2.472c.218.284.418.598.597.933zM10.855 4a7.966 7.966 0 0 0-.468-1.068C9.835 1.897 9.17 1.282 8.5 1.077V4h2.355z"/>
              </svg>
              <a href="${centerData.website}" target="_blank" style="
                color: #0d6efd;
                text-decoration: none;
                font-size: 15px;
                font-weight: 500;
              ">Visit Website</a>
            </div>
          ` : ""}

          ${centerData.description ? `
            <div style="
              margin-top: 16px;
              padding-top: 16px;
              border-top: 1px solid #dee2e6;
            ">
              <div style="
                color: #495057;
                font-size: 14px;
                line-height: 1.6;
              ">${centerData.description}</div>
            </div>
          ` : ""}
        </div>

        <!-- Action -->
        ${centerData.website ? `
          <div style="padding-top: 16px; border-top: 1px solid #dee2e6;">
            <a href="${centerData.website}" target="_blank" style="
              display: block;
              padding: 12px 20px;
              background: #0d6efd;
              color: white;
              text-decoration: none;
              border-radius: 6px;
              font-size: 14px;
              font-weight: 600;
              text-align: center;
              transition: background 0.2s;
            ">Learn More</a>
          </div>
        ` : ""}
      </div>
    `;
  };

  return {
    formatHours,
    formatHoursObject,
    formatDescription,
    formatInsurance,
    formatLanguages,
    formatAddress,
    hasData,
    createSimplePopup,
    createRegionalCenterPopup
  };
}
