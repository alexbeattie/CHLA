# Home Page Redesign Spec

This spec mirrors the updated iOS home screen direction so it can be recreated in Figma or refined further before a wider design system pass.

## Goals

- Keep `Ask KiNDD` as the clearest primary action.
- Preserve fast navigation to `Near Me`, `Regions`, `Browse`, and `Map`.
- Make `LA County Regional Centers` look geographic and trustworthy.
- Keep the home page readable above the fold on an iPhone screen.

## Screen Structure

1. Hero
2. Ask KiNDD card
3. Quick Actions
4. LA County Regional Centers map card
5. Service Type cards
6. Optional detected center card
7. Footer utility links
8. Floating KiNDD action button

## Section Details

### Hero

- Soft indigo-to-lavender gradient wash.
- Top row:
  - `Los Angeles County` pill on the left.
  - overflow menu on the right.
- Center stack:
  - KiNDD logo
  - `Developmental Services Navigator`
  - supporting sentence: `Find services, understand regional center coverage, and get guided help faster.`

### Ask KiNDD Card

- Large white card with subtle purple outline and soft shadow.
- Left icon: gradient square with sparkles glyph.
- Main copy:
  - Title: `Ask KiNDD`
  - Body: personalized guidance around services, next steps, and regional center questions.
- Right affordance: circular chevron.
- Prompt chips:
  - `Find ABA near me`
  - `Early intervention`
  - `Which regional center serves me?`

### Quick Actions

- Title: `Quick Actions`
- Supporting copy: direct wayfinding into map and browse flows.
- Four equal tiles:
  - `Near Me`
  - `Regions`
  - `Browse`
  - `Map`

### LA County Regional Centers Card

- White rounded card with realistic mini map preview.
- Header:
  - Title: `LA County Regional Centers`
  - Description: explain that the card shows real service boundaries.
  - CTA: `View Map`
- Optional status chip when user context exists:
  - `Likely region`
  - acronym chip, for example `FDLRC`
- Map area:
  - realistic base map
  - softly tinted regional polygons
  - labeled center badges
  - stronger emphasis on the likely region when known
- Bottom-left glass overlay:
  - `Real service area map`
  - center count

### Service Type Grid

- Title: `Find by Service Type`
- Supporting copy: common care paths.
- Two-column card grid:
  - `ABA Therapy`
  - `Speech`
  - `Occupational`
  - `Physical`

### Detected Center Card

- Only shown when a center is inferred from location.
- Secondary emphasis compared to the map card.
- Includes:
  - center acronym badge
  - full center name
  - phone
  - chevron

### Footer

- Divider
- Two utility pills:
  - `About KiNDD`
  - `FAQ`

### Floating Action

- Persistent gradient circular button with sparkles icon.
- Sits above the liquid tab bar without collision.

## Visual Notes For Figma

- Prefer large-radius white cards over dense panels.
- Use the same regional-center colors as the app map.
- Keep shadows very soft and avoid heavy borders.
- Favor compact supporting text over long paragraphs.
- The regional centers module should feel like a true map preview, not an infographic.

## Source Files

- Layout: `chla-ios/CHLA-iOS/Views/HomeView.swift`
- Map preview component: `chla-ios/CHLA-iOS/Views/Components/RegionalCentersMiniMapCard.swift`
- Full regions map reference: `chla-ios/CHLA-iOS/Views/RegionalCenterMapView.swift`
