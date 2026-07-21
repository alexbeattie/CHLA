# Android iPhone Visual Parity Design

**Status:** Approved on 2026-07-20

## Purpose

Make the Android app read as the same KiNDD product as the current iPhone app. The current Android foundation has strong behavioral coverage but presents that behavior through mostly stock Material 3 screens. This phase ports the live iPhone composition, hierarchy, brand treatment, and interaction model into native Jetpack Compose while preserving the verified Android API, profile, discovery, localization, privacy, and accessibility behavior.

The checked-in SwiftUI implementation and fresh iPhone 16 Pro simulator captures are the visual source of truth. Older screenshots and the superseded home redesign document are supporting history only when they conflict with the current running iPhone build.

## Product Decisions

- Target near-literal iPhone parity across onboarding, Home, Map, Regions, List, Chat, More, and Settings.
- Use native Compose and Google Maps; Python and backend changes are outside this visual phase.
- Substitute Android-native mechanics only where iOS-only APIs have no stable Compose equivalent. The resulting silhouette, hierarchy, color, density, and interaction role must still match.
- Replace the standard five-item Material navigation bar with the iPhone information architecture: Home, Map, centered Ask KiNDD action, Regions, List, and More.
- Treat Chat as a modal sheet opened from the centered Ask action and Home capsule, not as a permanent destination tab.
- Keep the already verified profile, discovery, provider, map, and persistence behavior intact.
- A screen is not complete because unit or device tests are green. It is complete only after a current-iPhone/current-Android screenshot comparison passes at the target viewport.

## Success Criteria

1. At first glance, Home and Map have the same product identity and composition as the live iPhone build.
2. Shared chrome, cards, search surfaces, chips, buttons, and typography use one reusable KiNDD Compose system rather than screen-local approximations.
3. Android preserves 48dp touch targets, TalkBack semantics, font scaling, dark theme, reduced motion, and reduced-transparency fallbacks.
4. Existing discovery and profile tests continue to pass, and new screenshot baselines prevent visual regression.
5. The final verified APK is installed in the Pixel 8 emulator and left open for review.

## Non-Goals

- Rewriting Android in Python or another framework
- Changing production API contracts or backend data
- Changing the iPhone app
- Pixel-identical reproduction of Apple system status/navigation bars
- Depending on proprietary Apple blur, SF Symbols, or MapKit rendering
- Broad cleanup unrelated to the visual-parity surfaces

## Architecture

The work uses a hybrid parity system plus screen vertical slices:

```text
Current SwiftUI views + live iPhone captures
                 |
                 v
       KiNDD visual contract
                 |
        +--------+---------+
        |                  |
        v                  v
 Compose design system   Screenshot baselines
        |
        v
 Home / Map / Onboarding / List / Chat / More
        |
        v
 Existing ViewModels, DiscoveryStore, repositories, and APIs
```

Presentation changes remain above the existing state and repository boundaries. Screen composables receive the same state and callbacks they use today. Visual work must not duplicate network calls, move profile authority into UI, or create separate Map and List discovery state.

## KiNDD Compose Design System

Create focused reusable primitives under `ui/design` and role-based tokens under `ui/theme`.

### Color and material

- Brand indigo `#6366F1`, deep indigo `#4F46E5`, violet `#8B5CF6`, purple `#A855F7`, pink `#EC4899`, and matched green `#10B981` remain canonical.
- AI actions use a violet-to-pink gradient. Primary actions use indigo-to-violet.
- Page canvas is a neutral adaptive grouped surface with a restrained indigo wash at the top.
- Cards are neutral adaptive surfaces. Lavender is an accent wash, not the default fill for unrelated content.
- Translucent chrome uses a blurred or frosted surface when supported. The fallback is a high-opacity adaptive surface with a bright hairline and paired soft shadows.
- Therapy roles receive distinct colors matching iPhone: ABA indigo, Speech pink, Occupational violet, Physical purple, with stable fallbacks for additional therapies.

### Geometry and spacing

- Compact chip/capsule radius: fully rounded.
- Selection-row radius: 14dp.
- Standard card radius: 20dp.
- Hero radius: 26dp.
- Sheet radius: 28dp.
- Page gutters: 18dp where the iPhone uses 18pt.
- Standard vertical rhythm: 10, 14, and 22dp tiers rather than repeating large 16-24dp blocks everywhere.
- Visual content may be compact while its semantic touch target remains at least 48dp.

### Typography and iconography

- Use Android system typography with a rounded display treatment for brand headings and calls to action; body copy remains optimized for reading.
- Define explicit display, title, body, metadata, overline, and capsule styles with size-specific tracking and leading.
- Add the KiNDD logo as an in-app drawable.
- Use Android vector equivalents with consistent weight and optical size; do not reuse generic legacy CHLA blue/gold imagery.

### Interaction

- Shared press feedback begins on touch-down: scale 0.97 for controls and 0.985 for cards, with opacity fallback when motion is reduced.
- Default navigation and sheet transitions are critically damped and interruptible. Momentum bounce is reserved for gesture-driven sheet settling.
- Haptics are limited to meaningful selection, successful match, error, and destructive confirmation events.
- Reduced Motion replaces spatial transitions with short cross-fades. Reduced transparency uses solid adaptive surfaces with clear borders.

### Reusable components

- `KiNDDFloatingNavBar`
- `KiNDDTopWash`
- `KiNDDLogoHeader`
- `KiNDDCard` variants: neutral, hero, matched, list row, destructive
- `KiNDDPrimaryCapsule`, `KiNDDSecondaryCapsule`, `KiNDDIconAction`
- `KiNDDSearchOverlay`
- `KiNDDServiceTile` and `KiNDDServiceTag`
- `KiNDDSelectionCard` and `KiNDDSegmentedChoice`
- `KiNDDLoadingOverlay`, `KiNDDEmptyState`, `KiNDDErrorBanner`

## Navigation and App Shell

The main content is edge-to-edge with enough bottom content inset to remain readable behind floating chrome. The bottom capsule contains Home, Map, a visually distinct centered sparkle Ask action, Regions, List, and More. The selected destination uses brand tint and a restrained selected surface; inactive icons remain secondary. Labels are exposed to TalkBack and may appear as tooltips or selected-state text without turning the capsule into a standard Material navigation bar.

The Ask action opens Chat in a rounded modal sheet. More exposes Settings and informational destinations. Back and predictive-back behavior remain Android-native. The floating bar may temporarily hide during an intentional full-screen map gesture and returns predictably.

## Screen Contracts

### Onboarding

- Five centered pages with compact capsule progress indicators and the ambient top wash.
- Welcome uses the KiNDD logo, rounded display title, centered explanation, and a segmented family/clinician selector.
- ZIP uses a tinted location icon tile, compact centered field, indigo focus ring, and secondary location action.
- Regional-center match uses a real service-area map hero with highlighted polygon and overlaid matched-center card; unmatched/offline states retain the same hero geometry with clear status.
- Journey and age choices use icon-led selection cards with selected wash, stroke, and checkmark.
- Back is a light capsule; Continue is a gradient capsule with arrow. Transitions respect Reduce Motion.

### Home

- Compact KiNDD logo header with Los Angeles County pill and overflow menu.
- A 340dp regional-center map hero is the primary surface. It includes service-area polygons, an Explore control, and an overlaid matched-center or ZIP-entry card.
- Matched card includes overline, green matched state, center name, concise role explanation, phone action, and Details action.
- Four compact colored service tiles follow: ABA, Speech, Occupational, and Physical.
- Journey-specific next-step card uses overline, strong title, detail copy, primary action, and Ask KiNDD action.
- Guided-question rows and information footer follow below.
- A persistent frosted `Ask KiNDD anything...` capsule floats above navigation.

### Map

- Map is full-bleed and remains the dominant surface.
- Search and filter controls float over the map as compact frosted chrome; they do not consume half the viewport in a vertical form stack.
- Active filters are summarized compactly and expand into the existing filter sheet.
- Location, refresh, and map-mode controls form a right-side floating stack.
- Provider markers use KiNDD/therapy/regional-center roles rather than default red pins. Existing coordinate filtering remains authoritative.
- Regional-center and resource-count badges float above the bottom navigation inset.
- Loading retains map context; recoverable errors appear as a nonblocking banner with Retry.

### Regions

- Regions is restored as a first-class destination.
- It displays the live regional-center service-area map and iPhone-equivalent selection/detail treatment using the existing regional-center API and already verified flexible ZIP decoding.
- Selected regions use their canonical color and expose clear detail/provider actions.

### Provider List

- Compact header/search treatment replaces the tall solid app bar and form stack.
- Sort and filter actions remain immediately accessible; active filters use role-colored removable tags.
- Cards use neutral grouped surfaces, strong provider name, provider-type capsule, compact distance badge, two-line address, up to three therapy tags, phone action when available, and regional-center badge.
- Address rendering must normalize structured or serialized address values before display; malformed JSON-like strings are never user-facing.
- Existing loading, empty, stale-results, and Retry behavior remains, rendered through the shared state components.

### Chat

- Chat opens as a rounded sheet from the centered sparkle action or Home capsule.
- Toolbar carries sparkle identity plus available history and conversation actions without duplicating the title in the empty state.
- Welcome uses the AI gradient avatar and compact suggestion capsules.
- User bubbles use the primary gradient; assistant content uses a neutral card with violet edge and readable full-width Markdown.
- Composer is a rounded multiline surface with send/stop gradient state. Attachment or microphone controls appear only where the underlying Android capability is functional; unavailable controls are not decorative dead buttons.
- Existing safe response and API behavior is preserved.

### More and Settings

- More follows the iPhone information architecture and links to Settings, About, FAQ, privacy, terms, and regional-center information.
- Settings becomes a compact grouped list rather than a large card per row.
- Existing profile edit/reset behavior remains prominent and safe.
- Privacy and terms rows must navigate to working content or be explicitly unavailable; no inert rows.

## Data, Loading, and Error Behavior

- UI consumes existing `StateFlow` state and callbacks; it does not introduce a second source of truth.
- Map and List continue to share one discovery session and latest-wins request behavior.
- Existing results stay visible during refresh.
- Map loading and recoverable errors overlay retained geography rather than replacing it.
- ZIP invalid, unmatched, offline, location-denied, and server-error states remain distinct.
- Raw response bodies, exception strings, ZIP values, and profile data are not logged.
- The visual phase may normalize provider address presentation locally but may not silently discard records.

## Accessibility and Adaptation

- Every interactive control keeps a 48dp minimum semantic target.
- Icon-only chrome has explicit TalkBack labels and selected-state descriptions.
- Layouts are validated at 1.0x and 1.3x font scale and on a narrow phone viewport.
- Service identity never relies on color alone.
- Light and dark themes preserve readable contrast.
- Reduced Motion and reduced-transparency fallbacks are first-class component behavior.
- Spanish uses the same hierarchy without clipping or inaccessible truncation.

## Verification Strategy

### Automated behavior

- Retain all existing unit and connected-device tests for profile, persistence, discovery, Map/List parity, localization, accessibility, and API contracts.
- Add focused tests for navigation information architecture, Chat sheet routing, Home hero callbacks, Regions destination, address normalization, and preserved shared state.

### Visual regression

- Capture deterministic Compose screenshot baselines for onboarding, Home, Map, Regions, List, Chat, and Settings in light theme.
- Add dark-theme and large-text baselines for shared chrome and the highest-risk screens.
- Fix clocks, animations, locale, font scale, insets, and data fixtures so comparisons are stable.
- Store or generate the approved current-iPhone reference captures alongside a parity manifest documenting viewport and state.
- Review each Android screenshot side-by-side with its matching iPhone reference. Tests guard regression; human comparison establishes the initial baseline.

### Live emulator acceptance

- Install the exact verified debug APK on the Pixel 8 API 36 emulator.
- Verify onboarding, Home, Map markers/tiles, Regions, List results, Chat opening, Settings, profile persistence, and offline recovery.
- Capture final emulator screenshots for the same states as the iPhone references.
- Leave the emulator running on the redesigned Home screen for review.

## Delivery Order

1. Tokens, typography, shared surfaces, buttons, and icon assets.
2. Floating navigation and edge-to-edge app shell.
3. Home and Map, because they are the most visible parity failures.
4. Onboarding and Regions.
5. Provider List and address presentation.
6. Chat sheet and composer.
7. More, Settings, and shared state surfaces.
8. Screenshot baselines, full verification, APK install, and simulator handoff.

Each step must leave the app buildable and preserve existing functional behavior. Shared primitives are extracted instead of growing large screen files.
