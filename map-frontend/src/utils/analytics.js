/**
 * Google Analytics 4 Integration
 * 
 * Setup Instructions:
 * 1. Create a GA4 property at https://analytics.google.com
 * 2. Get your Measurement ID (format: G-XXXXXXXXXX)
 * 3. Set VITE_GA_MEASUREMENT_ID in your .env files:
 *    - .env.local (for local dev, optional)
 *    - .env.production (for production, required)
 * 4. Uncomment the initAnalytics() call in main.js
 */

const GA_MEASUREMENT_ID = import.meta.env.VITE_GA_MEASUREMENT_ID;

/**
 * Initialize Google Analytics
 */
export function initAnalytics() {
  // Only initialize in production or if explicitly enabled
  const isDev = import.meta.env.DEV;
  
  if (!GA_MEASUREMENT_ID) {
    console.warn('⚠️ Google Analytics not configured. Set VITE_GA_MEASUREMENT_ID in .env');
    return;
  }

  if (isDev) {
    console.log('📊 Analytics disabled in development mode');
    return;
  }

  // Load Google Analytics script
  const script = document.createElement('script');
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`;
  document.head.appendChild(script);

  // Initialize gtag
  window.dataLayer = window.dataLayer || [];
  function gtag() {
    window.dataLayer.push(arguments);
  }
  window.gtag = gtag;

  gtag('js', new Date());
  gtag('config', GA_MEASUREMENT_ID, {
    // Enhanced measurement features
    send_page_view: true,
    cookie_flags: 'SameSite=None;Secure',
    // Privacy-friendly settings
    anonymize_ip: true,
    // Custom parameters
    custom_map: {
      dimension1: 'user_zip_code',
      dimension2: 'regional_center',
      dimension3: 'search_type'
    }
  });

  console.log('✅ Google Analytics initialized:', GA_MEASUREMENT_ID);
}

/**
 * Track page views (for SPA navigation)
 */
export function trackPageView(pagePath, pageTitle) {
  if (!window.gtag) return;

  window.gtag('event', 'page_view', {
    page_path: pagePath,
    page_title: pageTitle
  });
}

/**
 * Track custom events
 */
export function trackEvent(eventName, eventParams = {}) {
  if (!window.gtag) return;

  window.gtag('event', eventName, eventParams);
}

/**
 * Track provider search
 */
export function trackProviderSearch(zipCode, regionalCenter, resultCount) {
  trackEvent('search_providers', {
    search_term: zipCode,
    regional_center: regionalCenter,
    result_count: resultCount,
    search_type: 'zip_code'
  });
}

/**
 * Track provider selection/click
 */
export function trackProviderClick(providerId, providerName, distance) {
  trackEvent('select_provider', {
    provider_id: providerId,
    provider_name: providerName,
    distance_miles: distance
  });
}

/**
 * Track directions request
 */
export function trackGetDirections(providerId, providerName, method) {
  trackEvent('get_directions', {
    provider_id: providerId,
    provider_name: providerName,
    direction_method: method // 'google_maps' or 'apple_maps'
  });
}

/**
 * Track filter usage
 */
export function trackFilterApplied(filterType, filterValue) {
  trackEvent('apply_filter', {
    filter_type: filterType,
    filter_value: filterValue
  });
}

/**
 * Track map interactions
 */
export function trackMapInteraction(interactionType, details = {}) {
  trackEvent('map_interaction', {
    interaction_type: interactionType,
    ...details
  });
}

/**
 * Track phone call clicks
 */
export function trackPhoneCall(providerId, providerName) {
  trackEvent('click_to_call', {
    provider_id: providerId,
    provider_name: providerName
  });
}

/**
 * Track website visits
 */
export function trackWebsiteVisit(providerId, providerName, website) {
  trackEvent('visit_provider_website', {
    provider_id: providerId,
    provider_name: providerName,
    website_url: website
  });
}

/**
 * Track user engagement milestones
 */
export function trackEngagement(milestoneType, value) {
  trackEvent('user_engagement', {
    engagement_type: milestoneType,
    engagement_value: value
  });
}

