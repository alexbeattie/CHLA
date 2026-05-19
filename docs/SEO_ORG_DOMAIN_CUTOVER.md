# `.org` Canonical Domain Cutover Runbook

Application code has been switched to treat `https://kinddhelp.org/` as the
canonical public domain. Prerendered HTML now ships per-route titles, meta
descriptions, OpenGraph/Twitter tags, sitemap, and robots all pointing at the
`.org` host.

This runbook covers the AWS / DNS side of the cutover. It is intentionally
small and reversible. Treat it as higher risk than app code changes.

## What changed in code

- `map-frontend/vite.config.js` runs `vite-plugin-prerender` against public
  routes during production builds, writing static HTML for each public route.
- `map-frontend/src/seo/siteConfig.js` is the single source of truth for site
  title, canonical origin, per-route titles, and per-route descriptions.
- `map-frontend/src/composables/useSeo.js` applies per-route head tags so the
  prerender step snapshots correct metadata.
- `map-frontend/index.html`, `public/robots.txt`, `public/sitemap.xml`, and
  `public/site.webmanifest` use `https://kinddhelp.org/` and the
  `NDD Resource Map - KiNDD` title pattern.
- User-facing references to `kinddhelp.com` in `PrivacyPolicyView.vue`,
  `TermsOfServiceView.vue`, and `pdfGenerator.js` were updated to `.org`.
- `api.kinddhelp.com` is unchanged. It is a backend infrastructure identifier
  and stays on `.com` until a dedicated infrastructure rename.

## Verification before infrastructure work

After building locally, confirm the prerendered HTML is correct:

```bash
cd map-frontend
npm run build
grep -E '<title>|rel="canonical"' dist/about/index.html
grep -E '<title>|rel="canonical"' dist/regional-centers/harbor/index.html
```

Each prerendered route should now have:

- a route-specific `<title>` ending in `- KiNDD`
- a route-specific `<meta name="description">`
- a `<link rel="canonical" href="https://kinddhelp.org/...">`
- real body text in the static HTML (not just `<div id="app"></div>`)

## AWS / DNS cutover steps

Do these in the listed order. Do not skip the certificate step. Order matters.

### 1. Register `kinddhelp.org` (if not already controlled)

- Confirm registrar ownership of `kinddhelp.org` and `www.kinddhelp.org`.
- Move DNS for `kinddhelp.org` to Route 53 if it is not already there.
- Create the public hosted zone in Route 53 if needed.

### 2. Request the ACM certificate

CloudFront only consumes certificates from `us-east-1`.

- In ACM (`us-east-1`), request a public certificate for:
  - `kinddhelp.org`
  - `www.kinddhelp.org`
- Use DNS validation.
- Add the CNAME records to the Route 53 zone and wait for issuance.

### 3. Update the existing CloudFront distribution

The current distribution serves `kinddhelp.com` and is referenced by
`map-frontend/deploy.sh` and `.github/copilot-instructions.md` as
`E2W6EECHUV4LMM`.

- Add `kinddhelp.org` and `www.kinddhelp.org` to the distribution's
  "Alternate domain names (CNAMEs)" list.
- Attach the new `.org` certificate so CloudFront can serve both `.com` and
  `.org` until cutover is complete.
- Keep behaviors and origins unchanged.

### 3a. Add a directory-index rewrite (REQUIRED for prerender to be served)

Prerender writes each route as `dist/<route>/index.html`. S3 / CloudFront
serves `dist/about/index.html` for `/about/` (trailing slash) but does NOT
serve it for `/about` (no trailing slash). Crawlers and the canonical URLs
the app emits use the no-trailing-slash form. Without the rewrite, crawlers
will hit `/about`, get back the root SPA shell, and the prerender work is
wasted.

Attach a CloudFront Function on the **viewer-request** event of the existing
distribution that maps no-extension URIs to `/<uri>/index.html`:

```js
function handler(event) {
  var request = event.request;
  var uri = request.uri;
  if (uri === '/' || uri.endsWith('.html')) {
    return request;
  }
  if (uri.endsWith('/')) {
    request.uri = uri + 'index.html';
    return request;
  }
  var hasExtension = /\.[a-zA-Z0-9]+$/.test(uri);
  if (!hasExtension) {
    request.uri = uri + '/index.html';
  }
  return request;
}
```

After attaching the function, smoke-test:

```bash
curl -I https://kinddhelp.org/about
curl -I https://kinddhelp.org/regional-centers/harbor
```

Both should return `HTTP/2 200` with response bodies that contain
route-specific `<title>` and visible page text.

This rewrite is also fine to attach before the `.org` records resolve,
because it just rewrites paths; it does not change host headers.

### 4. Create Route 53 alias records for `.org`

In the `kinddhelp.org` hosted zone:

- `A` (alias, IPv4) record for the apex pointing at the CloudFront distribution.
- `AAAA` (alias, IPv6) record for the apex pointing at the same distribution.
- `A` and `AAAA` aliases for `www.kinddhelp.org` pointing at the same
  distribution. Optionally redirect `www` to apex with a separate S3 or
  CloudFront Function (see step 6).

### 5. Verify `.org` serves the app

Before cutting `.com` over:

```bash
curl -I https://kinddhelp.org/
curl -L https://kinddhelp.org/about | head -40
```

Confirm:

- `HTTP/2 200`
- HTML body contains route-specific `<title>` and visible page text
- `rel="canonical"` points at the `.org` URL

### 6. Permanent 301 from `.com` to `.org`

This is the step that consolidates SEO ranking signals onto `.org`.

Pick one of the following implementations. Option A is preferred because it
avoids spinning up another origin.

#### Option A (preferred): CloudFront Function on the `.com` distribution

1. Create a second CloudFront distribution that only serves the `.com` and
   `www.kinddhelp.com` host headers. Reuse the existing certificate covering
   `.com`.
2. Attach a CloudFront Function on the viewer-request event that returns a
   301 to `https://kinddhelp.org` + the original URI and query string.
3. Update Route 53 `kinddhelp.com` records to point at the redirect-only
   distribution.

CloudFront Function (viewer-request, JavaScript runtime 2.0) reference:

```js
function handler(event) {
  var request = event.request;
  var qs = request.querystring;
  var query = '';
  var keys = Object.keys(qs);
  if (keys.length) {
    var parts = [];
    for (var i = 0; i < keys.length; i++) {
      var key = keys[i];
      var value = qs[key].value;
      parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
    }
    query = '?' + parts.join('&');
  }
  var location = 'https://kinddhelp.org' + request.uri + query;
  return {
    statusCode: 301,
    statusDescription: 'Moved Permanently',
    headers: { location: { value: location } }
  };
}
```

#### Option B: S3 website redirect

1. Create a separate S3 bucket configured for static website hosting with the
   "Redirect requests" setting pointed at `kinddhelp.org` over HTTPS.
2. Front it with a small CloudFront distribution for HTTPS support.
3. Point Route 53 `kinddhelp.com` records at that distribution.

Option B works but uses more moving parts and an extra distribution.

### 7. Re-point Route 53 `.com` records

Once the redirect-only distribution from step 6 is healthy:

- Update `kinddhelp.com` and `www.kinddhelp.com` Route 53 records to point at
  the new redirect-only distribution.
- Keep the `.com` certificate attached to that distribution.

### 8. Verify the 301 chain

```bash
curl -I https://kinddhelp.com/
curl -I https://www.kinddhelp.com/about
```

Each should respond with `HTTP/2 301` and a `location: https://kinddhelp.org/...`
that matches the original path.

### 9. Search Console handoff

- Add `https://kinddhelp.org` and `https://www.kinddhelp.org` as properties in
  Google Search Console.
- Submit `https://kinddhelp.org/sitemap.xml`.
- In the existing `kinddhelp.com` property, use the Change of Address tool to
  point at the new `.org` property.

### 10. Update repo references after the cutover lands

Lower priority follow-up, only after `.org` is the live canonical:

- `README.md`: update live application URLs to `.org`.
- `map-frontend/deploy.sh`: update the "Testing frontend" curl URL.
- Backend `CORS_ALLOWED_ORIGINS` and `CSRF_TRUSTED_ORIGINS`: add `kinddhelp.org`
  and `www.kinddhelp.org` alongside the existing `.com` entries until the
  redirect step lands; once `.com` only 301s, the `.com` origins can be
  removed.

## Rollback

If anything in the cutover misbehaves:

- Revert the Route 53 `kinddhelp.com` records to the original CloudFront
  distribution. `.com` will resume serving the app directly.
- The `.org` records can stay; they will just continue to point at the app and
  duplicate-serve until you address it.
- App code does not need a revert. The repo is already canonical-`.org`; the
  prerendered tags reflect the intended end state.

## Notes

- Prerendering can be skipped locally by setting `DISABLE_PRERENDER=1` before
  `npm run build`. CI/CD should leave it enabled so deployed artifacts include
  the static HTML.
- `api.kinddhelp.com` is intentionally unchanged. Renaming the API host is a
  separate, higher-risk task.
- `og-image.jpg` and `twitter-card.jpg` continue to be served from the same
  hosting bucket. They now resolve at `https://kinddhelp.org/og-image.jpg`
  once `.org` is live.
