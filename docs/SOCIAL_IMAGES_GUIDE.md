# Social Media Images Guide

## Required Images for SEO & Social Sharing

These images are referenced in `index.html` but need to be created and placed in `/map-frontend/public/`.

---

## 1. Open Graph Image (Facebook, LinkedIn)

### **File:** `og-image.jpg`
### **Location:** `/map-frontend/public/og-image.jpg`
### **Dimensions:** 1200 x 630 pixels (1.91:1 ratio)
### **Format:** JPG or PNG (JPG preferred for smaller file size)
### **Max File Size:** < 8 MB (aim for < 300 KB)

### Content Requirements:
- **Background:** CHLA dark blue (#004877) or gradient
- **Logo:** KINDD logo or CHLA logo
- **Main Text:** "Find ABA Therapy Providers in Los Angeles"
- **Subtext:** "Free Interactive Map by Children's Hospital LA"
- **Visual:** Map outline of LA County or marker icon
- **Branding:** CHLA butterfly icon (if space permits)

### Safe Zones:
- Keep text/logos at least 40px from edges
- Facebook may crop differently on mobile vs desktop

### Tools to Create:
1. **Canva** (Free): https://canva.com
   - Search template: "Open Graph Image"
   - Use 1200x630px custom size
   
2. **Figma** (Free): https://figma.com
   - Create frame: 1200x630px
   - Export as JPG (quality 85%)

3. **Online Generator**: https://www.opengraph.xyz/
   - Quick template-based generator

### Quick Canva Instructions:
```
1. Go to Canva.com
2. Click "Create a design"
3. Custom size: 1200 x 630 px
4. Search for "Social Media Banner" templates
5. Customize with:
   - CHLA brand colors (#004877, #FFC923, etc.)
   - Upload CHLA logo from /map-frontend/src/assets/
   - Add text: Main headline + subtext
   - Add map visual or location icon
6. Download as JPG
7. Save to /map-frontend/public/og-image.jpg
```

---

## 2. Twitter Card Image

### **File:** `twitter-card.jpg`
### **Location:** `/map-frontend/public/twitter-card.jpg`
### **Dimensions:** 1200 x 675 pixels (16:9 ratio) OR 1200 x 600 pixels (2:1 ratio)
### **Format:** JPG or PNG
### **Max File Size:** < 5 MB (aim for < 300 KB)

### Content Requirements:
- Similar to og-image but adjusted for Twitter's aspect ratio
- Can be slightly more concise with text
- Same brand colors and logo

### Note:
You can often reuse the same og-image.jpg for Twitter if you adjust the safe zones.

---

## 3. Favicon Set

### **Files Needed:**
- `favicon.ico` (16x16, 32x32 multi-resolution)
- `favicon-16x16.png`
- `favicon-32x32.png`
- `apple-touch-icon.png` (180x180)
- `android-chrome-192x192.png` (192x192)
- `android-chrome-512x512.png` (512x512)

### **Location:** `/map-frontend/public/`

### Easy Method: Use a Generator
**Recommended:** https://realfavicongenerator.net/

#### Steps:
1. Go to https://realfavicongenerator.net/
2. Upload your master icon:
   - Use KINDD logo or CHLA butterfly icon
   - Minimum 512x512px PNG
   - Transparent background preferred
   - High contrast design works best
3. Customize options:
   - iOS: Choose background color (#004877)
   - Android: Choose theme color (#004877)
   - Windows: Choose tile color (#004877)
4. Generate favicons
5. Download the package
6. Extract all files to `/map-frontend/public/`

### Manual Creation (Advanced):
If you have the CHLA logo in SVG or high-res PNG:
1. Open in image editor (Photoshop, GIMP, Figma)
2. Create artboards for each size
3. Export each size as PNG
4. Use online tool to convert to .ico: https://convertio.co/png-ico/

---

## 4. PWA Icons (Optional but Recommended)

Already configured in `site.webmanifest`, just need the files:

- `android-chrome-192x192.png` (192x192)
- `android-chrome-512x512.png` (512x512)

These are used when users "Add to Home Screen" on mobile.

---

## Quick Start: Temporary Placeholder

If you need to deploy immediately without custom images, create simple placeholders:

### Using ImageMagick (if installed):
```bash
cd /Users/alexbeattie/Developer/CHLA/map-frontend/public

# Create og-image placeholder
convert -size 1200x630 xc:"#004877" \
  -fill white -pointsize 72 -gravity center \
  -annotate +0-50 "KINDD" \
  -pointsize 36 -annotate +0+50 "Find ABA Therapy in LA" \
  og-image.jpg

# Create twitter-card placeholder
convert -size 1200x675 xc:"#004877" \
  -fill white -pointsize 72 -gravity center \
  -annotate +0-50 "KINDD" \
  -pointsize 36 -annotate +0+50 "Find ABA Therapy in LA" \
  twitter-card.jpg
```

### Or Use Online Placeholder Service:
```
https://placehold.co/1200x630/004877/white?text=KINDD+Provider+Map
```

---

## CHLA Brand Guidelines Reference

### Colors (from chla_brand_guidelines.md):

**Primary:**
- Dark Blue: #004877 (RGB: 0, 90, 151)

**Secondary (Accent):**
- Yellow: #FFC923
- Green: #4DAA50
- Light Blue: #0D9DDB
- Purple: #805791
- Orange: #FF7F00
- Red: #EA1D36
- Teal: #097C8A

### Typography:
- Primary: Futura Std Bold (for headlines)
- Fallback: Arial Bold, Calibri Bold

### Logo:
- Location: `/map-frontend/src/assets/chla-logo.svg`
- Must maintain clear space around logo
- Do not alter colors or proportions

---

## Design Tips for Social Images

### 1. Keep It Simple
- One clear message
- High contrast text
- Avoid busy backgrounds

### 2. Mobile Preview
- Most users see on mobile
- Test at small sizes
- Ensure text is readable

### 3. Brand Consistency
- Use CHLA brand colors
- Include recognizable logo
- Maintain professional appearance

### 4. Call to Action
- Make benefit clear: "Find", "Locate", "Discover"
- Show value: "Free", "Interactive", "Easy to Use"

### 5. Test Before Publishing
- Preview in Facebook Debugger
- Check Twitter Card Validator
- View on mobile device

---

## Testing Your Images

### After creating and uploading:

1. **Build and Deploy:**
   ```bash
   cd map-frontend
   npm run build
   # Deploy to S3/CloudFront
   ```

2. **Clear Cache:**
   ```bash
   aws cloudfront create-invalidation \
     --distribution-id E2W6EECHUV4LMM \
     --paths "/og-image.jpg" "/twitter-card.jpg" "/*"
   ```

3. **Test with Facebook Debugger:**
   - Go to: https://developers.facebook.com/tools/debug/
   - Enter: https://kinddhelp.com
   - Click "Scrape Again"
   - Verify image displays correctly

4. **Test with Twitter Card Validator:**
   - Go to: https://cards-dev.twitter.com/validator
   - Enter: https://kinddhelp.com
   - Verify card preview

5. **Test with LinkedIn Post Inspector:**
   - Go to: https://www.linkedin.com/post-inspector/
   - Enter: https://kinddhelp.com
   - Click "Inspect"

---

## File Checklist

Before deploying, ensure these files exist in `/map-frontend/public/`:

- [ ] `og-image.jpg` (1200x630)
- [ ] `twitter-card.jpg` (1200x675)
- [ ] `favicon.ico`
- [ ] `favicon-16x16.png`
- [ ] `favicon-32x32.png`
- [ ] `apple-touch-icon.png` (180x180)
- [ ] `android-chrome-192x192.png`
- [ ] `android-chrome-512x512.png`
- [ ] `site.webmanifest` (✅ Already created)
- [ ] `robots.txt` (✅ Already created)
- [ ] `sitemap.xml` (✅ Already created)

---

## Example: og-image.jpg Layout

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  [CHLA Logo]                    [Butterfly Icon]   │
│                                                     │
│                                                     │
│            Find ABA Therapy Providers              │
│               in Los Angeles County                │
│                                                     │
│                                                     │
│         Free Interactive Map • 370+ Providers      │
│          Powered by Children's Hospital LA         │
│                                                     │
│                   [Map Visual]                     │
│                                                     │
└─────────────────────────────────────────────────────┘
    1200px width x 630px height
    Background: CHLA Blue (#004877)
    Text: White with yellow accent
```

---

## Resources

### Design Tools
- **Canva:** https://canva.com (Free tier available)
- **Figma:** https://figma.com (Free for individuals)
- **Photopea:** https://www.photopea.com/ (Free Photoshop alternative, browser-based)

### Generators
- **Favicon Generator:** https://realfavicongenerator.net/
- **OG Image Generator:** https://www.opengraph.xyz/
- **Social Media Sizes:** https://sproutsocial.com/insights/social-media-image-sizes-guide/

### Validators
- **Facebook Debugger:** https://developers.facebook.com/tools/debug/
- **Twitter Card Validator:** https://cards-dev.twitter.com/validator
- **LinkedIn Inspector:** https://www.linkedin.com/post-inspector/

### Learning
- **Open Graph Protocol:** https://ogp.me/
- **Twitter Cards Guide:** https://developer.twitter.com/en/docs/twitter-for-websites/cards/overview/abouts-cards

---

**Priority:** High  
**Estimated Time:** 30-60 minutes  
**Skills Needed:** Basic graphic design (Canva templates make this easy)  
**Impact:** Significantly improves social sharing and click-through rates

---

**Last Updated:** November 5, 2025  
**Next Step:** Create images and test social sharing

