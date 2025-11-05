# SEO Optimization Strategy for CHLA Provider Map

## Executive Summary

**Project:** KINDD - CHLA ABA Provider Map  
**Domain:** https://kinddhelp.com  
**Purpose:** Help families find autism and developmental disability services in Los Angeles County  
**Target Audience:** Parents, caregivers, healthcare providers, social workers  
**Geographic Focus:** Los Angeles County, California (7 Regional Centers)

---

## Target Keywords & Search Intent

### Primary Keywords (High Priority)
1. **"ABA therapy Los Angeles"** - 5,400/mo searches
2. **"autism services Los Angeles"** - 2,900/mo searches
3. **"developmental disability services LA"** - 1,300/mo searches
4. **"Regional Center providers Los Angeles"** - 880/mo searches
5. **"ABA providers near me"** - 6,600/mo searches (location-based)
6. **"autism therapy California"** - 3,600/mo searches

### Secondary Keywords (Medium Priority)
7. "behavioral therapy children Los Angeles"
8. "autism diagnosis Los Angeles"
9. "developmental delays services California"
10. "Regional Center services map"
11. "children's therapy providers LA County"
12. "ABA therapy insurance California"

### Long-Tail Keywords (Specific Geographic)
13. "San Gabriel Pomona Regional Center providers"
14. "Harbor Regional Center services"
15. "Westside Regional Center therapy"
16. "Frank D Lanterman Regional Center map"
17. "[City name] ABA therapy providers"
18. "[ZIP code] autism services"

### Brand/Entity Keywords
19. "Children's Hospital Los Angeles provider map"
20. "CHLA autism resources"
21. "KINDD provider map"

---

## Current State Analysis

### ❌ Issues Found
- Generic title: "Map Location Finder"
- No meta description
- No Open Graph tags (social sharing)
- No structured data/schema markup
- No robots.txt
- No sitemap.xml
- Missing canonical URL
- No Twitter Card tags
- No favicon variations (mobile/tablet)
- No keywords meta tag
- Missing language/locale tags
- No preconnect for external resources (Mapbox, APIs)

---

## SEO Optimization Plan

### 1. HTML Meta Tags Enhancement

#### Title Tag Strategy
```html
<title>Find ABA Therapy & Autism Services in Los Angeles | KINDD by CHLA</title>
```

**Formula:** [Action] + [Primary Service] + [Location] + [Brand]  
**Length:** 50-60 characters (optimal for mobile & desktop SERPs)

#### Meta Description
```html
<meta name="description" content="Locate ABA therapy providers and autism services across Los Angeles County's 7 Regional Centers. Free interactive map by Children's Hospital Los Angeles. Find qualified providers accepting insurance near you.">
```

**Formula:** [Value Prop] + [Geographic Coverage] + [Authority] + [CTA]  
**Length:** 150-160 characters

#### Keywords Meta Tag (minimal SEO value, but helpful for internal reference)
```html
<meta name="keywords" content="ABA therapy Los Angeles, autism services, developmental disability, Regional Center providers, children's therapy, Los Angeles County, CHLA, behavioral therapy">
```

---

### 2. Open Graph Tags (Social Media Optimization)

```html
<!-- Open Graph / Facebook -->
<meta property="og:type" content="website">
<meta property="og:url" content="https://kinddhelp.com/">
<meta property="og:title" content="Find ABA Therapy & Autism Services in Los Angeles | KINDD by CHLA">
<meta property="og:description" content="Locate ABA therapy providers and autism services across Los Angeles County. Free interactive map by Children's Hospital Los Angeles.">
<meta property="og:image" content="https://kinddhelp.com/og-image.jpg">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:site_name" content="KINDD - CHLA Provider Map">
<meta property="og:locale" content="en_US">

<!-- Twitter Card -->
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:url" content="https://kinddhelp.com/">
<meta name="twitter:title" content="Find ABA Therapy & Autism Services in Los Angeles | KINDD">
<meta name="twitter:description" content="Locate ABA therapy providers across LA County. Free interactive map by Children's Hospital Los Angeles.">
<meta name="twitter:image" content="https://kinddhelp.com/twitter-card.jpg">
<meta name="twitter:site" content="@CHLA">
```

---

### 3. Structured Data (JSON-LD Schema.org)

#### Organization Schema
```json
{
  "@context": "https://schema.org",
  "@type": "Organization",
  "name": "Children's Hospital Los Angeles",
  "alternateName": "CHLA",
  "url": "https://kinddhelp.com",
  "logo": "https://kinddhelp.com/assets/chla-logo.svg",
  "description": "We create hope and build healthier futures. Find ABA therapy and autism services across Los Angeles County.",
  "address": {
    "@type": "PostalAddress",
    "addressLocality": "Los Angeles",
    "addressRegion": "CA",
    "addressCountry": "US"
  },
  "sameAs": [
    "https://www.chla.org"
  ]
}
```

#### WebApplication Schema
```json
{
  "@context": "https://schema.org",
  "@type": "WebApplication",
  "name": "KINDD Provider Map",
  "url": "https://kinddhelp.com",
  "description": "Interactive map to find ABA therapy providers and autism services across Los Angeles County's 7 Regional Centers",
  "applicationCategory": "HealthApplication",
  "operatingSystem": "Web Browser",
  "offers": {
    "@type": "Offer",
    "price": "0",
    "priceCurrency": "USD"
  },
  "featureList": [
    "Find ABA therapy providers by ZIP code",
    "Search autism services by location",
    "Filter by insurance accepted",
    "View Regional Center service areas",
    "Get driving directions",
    "Mobile-friendly interface"
  ],
  "areaServed": {
    "@type": "GeoCircle",
    "geoMidpoint": {
      "@type": "GeoCoordinates",
      "latitude": "34.0522",
      "longitude": "-118.2437"
    },
    "geoRadius": "50000"
  }
}
```

#### Local Business Schema (for providers - dynamic per provider)
```json
{
  "@context": "https://schema.org",
  "@type": "MedicalBusiness",
  "name": "[Provider Name]",
  "address": "[Provider Address]",
  "telephone": "[Provider Phone]",
  "url": "[Provider Website]",
  "geo": {
    "@type": "GeoCoordinates",
    "latitude": "[lat]",
    "longitude": "[lng]"
  },
  "medicalSpecialty": "ABA Therapy"
}
```

---

### 4. robots.txt

```txt
# KINDD Provider Map - Robots.txt
User-agent: *
Allow: /

# Block admin/internal routes
Disallow: /admin
Disallow: /.env
Disallow: /api/

# Allow search engines to crawl public assets
Allow: /assets/

# Sitemap location
Sitemap: https://kinddhelp.com/sitemap.xml

# Crawl delay (optional, prevents server overload)
Crawl-delay: 1
```

---

### 5. sitemap.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
  
  <!-- Main page -->
  <url>
    <loc>https://kinddhelp.com/</loc>
    <lastmod>2025-11-05</lastmod>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
  
  <!-- Static pages (if any) -->
  <url>
    <loc>https://kinddhelp.com/about</loc>
    <lastmod>2025-11-05</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.8</priority>
  </url>
  
  <!-- Dynamic provider pages (optional - generated server-side) -->
  <!-- Can be generated via backend script from providers_v2 table -->
  
</urlset>
```

---

### 6. Additional HTML Head Elements

#### Canonical URL
```html
<link rel="canonical" href="https://kinddhelp.com/">
```

#### Language & Locale
```html
<html lang="en-US">
<meta http-equiv="content-language" content="en-US">
```

#### Mobile Optimization
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="default">
<meta name="apple-mobile-web-app-title" content="KINDD">
```

#### Theme Color (Brand)
```html
<meta name="theme-color" content="#004877">
<meta name="msapplication-TileColor" content="#004877">
```

#### Favicons (Multiple Sizes)
```html
<link rel="icon" type="image/x-icon" href="/favicon.ico">
<link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
<link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
<link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
<link rel="manifest" href="/site.webmanifest">
```

#### Preconnect (Performance + SEO)
```html
<!-- Preconnect to external resources for faster loading -->
<link rel="preconnect" href="https://api.mapbox.com">
<link rel="dns-prefetch" href="https://api.mapbox.com">
<link rel="preconnect" href="https://api.kinddhelp.com">
<link rel="dns-prefetch" href="https://api.kinddhelp.com">
<link rel="preconnect" href="https://cdn.jsdelivr.net">
```

#### Author & Copyright
```html
<meta name="author" content="Children's Hospital Los Angeles">
<meta name="copyright" content="Children's Hospital Los Angeles">
```

#### Geo Targeting
```html
<meta name="geo.region" content="US-CA">
<meta name="geo.placename" content="Los Angeles">
<meta name="geo.position" content="34.0522;-118.2437">
<meta name="ICBM" content="34.0522, -118.2437">
```

---

### 7. Content Optimization Strategy

#### H1 Tag (On-page, not in `<head>`)
```html
<h1>Find ABA Therapy & Autism Services in Los Angeles County</h1>
```

#### Semantic HTML
- Use proper heading hierarchy (H1 → H2 → H3)
- Use `<article>`, `<section>`, `<nav>` tags
- Add `aria-label` for accessibility (also helps SEO)

#### Alt Text for Images
```html
<img src="chla-logo.svg" alt="Children's Hospital Los Angeles logo">
```

#### Internal Linking
- Link to provider detail pages (if created)
- Link to Regional Center pages (if created)
- Add "About" or "How It Works" page

---

### 8. Performance Optimization (SEO Factor)

#### Core Web Vitals Targets
- **LCP (Largest Contentful Paint):** < 2.5s
- **FID (First Input Delay):** < 100ms
- **CLS (Cumulative Layout Shift):** < 0.1

#### Optimization Techniques
1. **Image optimization:** WebP format, lazy loading
2. **Code splitting:** Vue lazy loading for routes
3. **Minification:** CSS, JS via Vite build
4. **Caching:** CloudFront cache headers
5. **Compression:** Gzip/Brotli enabled on S3

---

### 9. Local SEO Strategy

#### Google My Business (if applicable)
- Create/claim GMB listing for CHLA
- Link to kinddhelp.com

#### Local Citations
- Ensure NAP (Name, Address, Phone) consistency
- Submit to:
  - Yelp
  - Healthcare directories
  - Local LA directories
  - Autism resource directories

#### Local Content
- Mention specific LA neighborhoods
- Reference the 7 Regional Centers by name
- Include ZIP codes in content

---

### 10. Analytics & Tracking

#### Google Analytics 4
```html
<!-- Google tag (gtag.js) -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXXXXX');
</script>
```

#### Google Search Console
- Verify ownership
- Submit sitemap
- Monitor search performance

#### Bing Webmaster Tools
- Verify ownership
- Submit sitemap

---

## Implementation Priority

### Phase 1: Critical (Immediate)
1. ✅ Update `<title>` tag
2. ✅ Add meta description
3. ✅ Add Open Graph tags
4. ✅ Add canonical URL
5. ✅ Create robots.txt
6. ✅ Add structured data (Organization + WebApplication)

### Phase 2: Important (Week 1)
7. Create sitemap.xml
8. Add favicon variations
9. Add theme color meta tags
10. Add preconnect tags
11. Add geo-targeting meta tags

### Phase 3: Enhancements (Week 2)
12. Create OG image (1200x630px)
13. Create Twitter Card image (1200x675px)
14. Submit to Google Search Console
15. Submit to Bing Webmaster Tools
16. Set up Google Analytics 4

### Phase 4: Content & Growth (Ongoing)
17. Add blog/resources section
18. Create Regional Center landing pages
19. Add provider detail pages
20. Build backlinks from autism/healthcare directories
21. Social media sharing strategy

---

## Success Metrics

### 3-Month Goals
- Indexed in Google Search Console: 100%
- Organic traffic: +200%
- Keyword rankings: Top 20 for primary keywords
- Page load time: < 2.5s (mobile)
- Core Web Vitals: All "Good" ratings

### 6-Month Goals
- Organic traffic: +500%
- Keyword rankings: Top 10 for primary keywords
- Backlinks: 20+ quality backlinks
- Domain Authority: 30+
- Monthly organic users: 1,000+

### 12-Month Goals
- Organic traffic: +1000%
- Keyword rankings: Top 3 for primary keywords
- Monthly organic users: 5,000+
- Featured snippets: 5+ keywords
- Local pack ranking: Top 3 for "ABA therapy Los Angeles"

---

## Technical SEO Checklist

- [ ] HTTPS enabled (✅ Already done)
- [ ] Mobile-friendly (✅ Already done)
- [ ] Fast loading (< 3s)
- [ ] Valid HTML
- [ ] No broken links
- [ ] XML sitemap
- [ ] Robots.txt
- [ ] Structured data
- [ ] Canonical URLs
- [ ] 301 redirects for old URLs
- [ ] Descriptive URLs (if routing added)
- [ ] Alt text on images
- [ ] Proper heading hierarchy
- [ ] Internal linking
- [ ] Breadcrumbs (if applicable)

---

## Content Marketing Strategy

### Blog Topics (SEO + User Value)
1. "How to Choose an ABA Therapy Provider in Los Angeles"
2. "Understanding Regional Centers: A Parent's Guide"
3. "Does Insurance Cover ABA Therapy in California?"
4. "7 Signs Your Child May Benefit from ABA Therapy"
5. "Navigating the Regional Center System in LA County"
6. "What to Expect in Your First ABA Therapy Session"
7. "ABA Therapy Costs: What LA Families Need to Know"
8. "Finding Bilingual ABA Providers in Los Angeles"

### Resource Pages
- Regional Center directory
- Insurance guide
- FAQ page
- Glossary of terms
- Provider checklist

---

## Competitive Analysis

### Competitors to Monitor
1. Psychology Today (therapist directory)
2. Autism Speaks resource locator
3. Regional Center websites
4. Local ABA provider websites
5. Healthcare directories (Zocdoc, Healthgrades)

### Differentiation Strategy
- **Geographic focus:** LA County specific
- **CHLA authority:** Hospital affiliation
- **User experience:** Interactive map (not just list)
- **Mobile-first:** Optimized for parents on-the-go
- **Free:** No subscription or registration required

---

## Budget Considerations

### Free/Low-Cost Tools
- Google Search Console (Free)
- Bing Webmaster Tools (Free)
- Google Analytics (Free)
- Schema markup generators (Free)
- Sitemap generators (Free)

### Paid Tools (Optional)
- SEMrush or Ahrefs ($99-$399/mo)
- Google Ads for keyword research ($0 budget, just use keyword planner)
- Professional OG image creation (Canva Pro $13/mo or Figma Free)

---

## Next Steps

1. **Immediate:** Implement Phase 1 (meta tags, OG tags, structured data)
2. **This week:** Create robots.txt, sitemap.xml, favicons
3. **Next week:** Submit to search engines, set up analytics
4. **Ongoing:** Monitor performance, adjust strategy

---

**Last Updated:** November 5, 2025  
**Document Owner:** Development Team  
**Review Frequency:** Monthly

