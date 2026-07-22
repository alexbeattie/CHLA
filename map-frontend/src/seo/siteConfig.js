/**
 * Centralized SEO configuration for the public site.
 *
 * This file is the single source of truth for:
 *   - canonical domain
 *   - site title pattern
 *   - per-route titles, descriptions, and Open Graph metadata
 *
 * The values here are consumed by:
 *   - index.html defaults at build time (via the SEO composable on initial mount)
 *   - the useSeo composable for per-route updates
 *   - the prerender step (Puppeteer captures the final DOM after useSeo runs)
 */

export const SITE_NAME = 'KiNDD'
export const SITE_TITLE = 'NDD Resource Map - KiNDD'
export const CANONICAL_ORIGIN = 'https://kinddhelp.org'

export const DEFAULT_DESCRIPTION =
  'Find neurodevelopmental disorder (NDD) services and Regional Center providers across Los Angeles County. Free interactive resource map.'

export const DEFAULT_OG_IMAGE = `${CANONICAL_ORIGIN}/og-image.jpg`
export const DEFAULT_TWITTER_IMAGE = `${CANONICAL_ORIGIN}/twitter-card.jpg`

const REGIONAL_CENTER_PAGES = {
  'san-gabriel-pomona': {
    label: 'San Gabriel/Pomona Regional Center',
    description:
      'San Gabriel/Pomona Regional Center serves eastern Los Angeles County and the San Gabriel Valley. Find NDD providers, ZIP codes, and contact information.',
  },
  harbor: {
    label: 'Harbor Regional Center',
    description:
      'Harbor Regional Center serves the South Bay and coastal Los Angeles County. Find NDD providers, ZIP codes, and contact information.',
  },
  'north-la-county': {
    label: 'North Los Angeles County Regional Center',
    description:
      'North LA County Regional Center serves the Antelope Valley and northern Los Angeles County. Find NDD providers, ZIP codes, and contact information.',
  },
  'eastern-la': {
    label: 'Eastern Los Angeles Regional Center',
    description:
      'Eastern Los Angeles Regional Center serves communities east of downtown LA. Find NDD providers, ZIP codes, and contact information.',
  },
  'south-central-la': {
    label: 'South Central Los Angeles Regional Center',
    description:
      'South Central Los Angeles Regional Center serves south-central LA communities. Find NDD providers, ZIP codes, and contact information.',
  },
  westside: {
    label: 'Westside Regional Center',
    description:
      'Westside Regional Center serves West LA, Santa Monica, and surrounding areas. Find NDD providers, ZIP codes, and contact information.',
  },
  lanterman: {
    label: 'Frank D. Lanterman Regional Center',
    description:
      'Frank D. Lanterman Regional Center serves parts of the San Fernando and San Gabriel Valleys. Find NDD providers, ZIP codes, and contact information.',
  },
}

/**
 * Per-route metadata keyed by canonical path.
 * Paths must match what the router resolves to (no trailing slash except `/`).
 * Dynamic regional center pages are resolved at runtime via `getSeoForPath`.
 */
const ROUTE_SEO = {
  '/': {
    title: SITE_TITLE,
    description: DEFAULT_DESCRIPTION,
  },
  '/about': {
    title: `About - ${SITE_TITLE}`,
    description:
      'Learn how KiNDD helps families find neurodevelopmental disorder (NDD) services across Los Angeles County Regional Centers.',
  },
  '/faq': {
    title: `FAQ - ${SITE_TITLE}`,
    description:
      'Answers to common questions about Regional Centers, NDD services, insurance, and how to use the KiNDD resource map.',
  },
  '/clinicians': {
    title: `For Clinicians - ${SITE_TITLE}`,
    description:
      'Resources for clinicians and care teams referring families to neurodevelopmental disorder (NDD) services across Los Angeles County.',
  },
  '/privacy': {
    title: `Privacy Policy - ${SITE_TITLE}`,
    description:
      'Privacy policy for the KiNDD website, iOS app, and Android app. Learn how location, AI chat, and other user information is handled.',
  },
  '/terms': {
    title: `Terms of Service - ${SITE_TITLE}`,
    description:
      'Terms of service for KiNDD - NDD Resource Map. Read the conditions for using the site.',
  },
  '/regional-centers': {
    title: `Regional Centers - ${SITE_TITLE}`,
    description:
      'Los Angeles County is served by 7 Regional Centers. Find the Regional Center that covers your ZIP code and explore available NDD services.',
  },
}

/**
 * Resolve SEO metadata for a given path.
 * Falls back to homepage metadata when the path is not recognized.
 *
 * @param {string} path
 * @returns {{title: string, description: string, canonical: string, ogImage: string, twitterImage: string}}
 */
export function getSeoForPath(path) {
  const normalized = normalizePath(path)
  const rcMatch = normalized.match(/^\/regional-centers\/([^/]+)$/)

  let base
  if (rcMatch) {
    const slug = rcMatch[1]
    const rcMeta = REGIONAL_CENTER_PAGES[slug]
    if (rcMeta) {
      base = {
        title: `${rcMeta.label} - ${SITE_TITLE}`,
        description: rcMeta.description,
      }
    } else {
      base = ROUTE_SEO['/regional-centers']
    }
  } else {
    base = ROUTE_SEO[normalized] || ROUTE_SEO['/']
  }

  return {
    title: base.title,
    description: base.description,
    canonical: `${CANONICAL_ORIGIN}${normalized === '/' ? '/' : normalized}`,
    ogImage: DEFAULT_OG_IMAGE,
    twitterImage: DEFAULT_TWITTER_IMAGE,
  }
}

/**
 * Public, prerenderable routes.
 * Private routes (login, providers, test-onboarding) are intentionally excluded.
 */
export const PRERENDER_ROUTES = [
  '/',
  '/about',
  '/faq',
  '/clinicians',
  '/privacy',
  '/terms',
  '/regional-centers',
  ...Object.keys(REGIONAL_CENTER_PAGES).map((slug) => `/regional-centers/${slug}`),
]

function normalizePath(path) {
  if (!path) {
    return '/'
  }
  const withoutQuery = path.split('?')[0].split('#')[0]
  if (withoutQuery === '' || withoutQuery === '/') {
    return '/'
  }
  return withoutQuery.replace(/\/+$/, '')
}
