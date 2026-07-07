# App Store Connect Metadata

## Basic Information


| Field | Value |
| -------------------- | ------------------------------ |
| App Name | KiNDD Resources |
| Subtitle | Find Regional Center Resources |
| Bundle ID | com.nddresources.map |
| SKU | ndd-resource-map-001 |
| Primary Language | English (U.S.) |
| Category (Primary) | Medical |
| Category (Secondary) | Navigation |
| Age Rating | 4+ |
| Copyright | © 2025 NDD Resources |


## Subtitle (30 characters max)

```
Find help. Know your next step
```

(Previous: "Find Regional Center Resources" - also fits if preferred.)

## Promotional Text (170 characters max, editable without review)

```
From diagnosis to services in place - KiNDD matches your family to your regional center, suggests your next step, and answers your questions.
```

## App Description

```
KiNDD helps Los Angeles County families navigate developmental disability services - from the day of diagnosis to services in place.

Enter your ZIP code and KiNDD matches you to your regional center, shows its service area on the map, and suggests your next step - whether that is requesting an intake evaluation, preparing for an appointment, or getting more from an IPP.

ASK KiNDD
• A caring assistant that answers questions about ABA, speech, occupational, and physical therapy, regional centers, eligibility, and funding
• Understands where your family is in the journey and tailors guidance to match
• Recommends real providers near you - tap a card to call or get directions

FIND SERVICES
• Interactive map of 600+ verified resources across LA County
• Filter by regional center area, therapy type, ages served, and insurance
• Detailed provider profiles with services, contact info, and directions
• Search by ZIP code, city, or provider name

ALWAYS ONE TAP AWAY
• Home screen widget with your regional center's phone number
• Siri and Spotlight shortcuts: "Call my regional center" and "Ask KiNDD"

REGIONAL CENTERS COVERED
• North Los Angeles County (NLACRC)
• Eastern Los Angeles (ELARC)
• South Central Los Angeles (SCLARC)
• Westside (WRC)
• Frank D. Lanterman (FDLRC)
• Harbor (HRC)
• San Gabriel/Pomona (SGPRC)

Designed for families, caregivers, and clinicians. KiNDD is a community resource and is not affiliated with the California Department of Developmental Services or any regional center. KiNDD provides navigation help, not medical advice.

Questions or feedback: support@kinddhelp.com
```

## Keywords (100 characters max)

```
regional center,autism,ABA,speech therapy,developmental,disability,early intervention,IPP,IEP,LA
```

## URLs


| Field | URL |
| ---------------- | -------------------------------------------------------------- |
| Privacy Policy | [https://kinddhelp.com/privacy](https://kinddhelp.com/privacy) |
| Terms of Service | [https://kinddhelp.com/terms](https://kinddhelp.com/terms) |
| Support URL | [https://kinddhelp.com/about](https://kinddhelp.com/about) |
| Marketing URL | [https://kinddhelp.com](https://kinddhelp.com) |


## App Review Information

### Notes for Reviewer

```
This app helps families find developmental disability services in Los Angeles County. It connects to our API at api.kinddhelp.com for verified provider data.

The Ask KiNDD feature is an AI assistant (Anthropic Claude, served through AWS Bedrock) that answers questions about navigating regional center services. Answers are grounded in our curated provider database and public regional center information. The app presents itself as navigation help, not medical advice, and includes appropriate disclaimers.

Location permission is optional; every feature works by entering a ZIP code instead. No login is required to use the app.
```

### Contact Information

- First Name: Alex
- Last Name: Beattie
- Email: [support@kinddhelp.com](mailto:support@kinddhelp.com)
- Phone: (Your phone number)

## Version Information


| Field | Value |
| ---------- | --------------- |
| Version | 1.4.0 |
| Build | 1 |


## What's New - 1.4.0 (paste into App Store Connect)

```
KiNDD 1.4.0 is a complete redesign built around one idea: meeting your family where you are.

NEW
- Ask KiNDD now understands where you are in the journey - just diagnosed, waiting for intake, or already receiving services - and tailors its guidance to match
- Your Next Step: the home screen suggests the one action that matters most right now, with a call button and coaching for the conversation
- Home screen widget: your regional center's phone number, one tap away
- Siri and Spotlight shortcuts: "Call my regional center" and "Ask KiNDD"
- Providers mentioned in chat answers appear as cards you can tap for details, directions, and contact info

IMPROVED
- A calmer, warmer design across every screen, in light and dark mode
- The map now leads the home screen, showing your regional center's service area at a glance
- Smoother chat with clearer, better-formatted answers
- Faster onboarding: enter your ZIP, see your regional center matched on the map, and you're in
- Better support for VoiceOver and Dynamic Type
```

## What to Test - 1.4.0 (paste into TestFlight)

```
This build is a full redesign. Please try:
1. Fresh onboarding (More tab -> Restart Welcome Setup): ZIP entry, the regional center match screen, and the journey question
2. The home screen: map overlays, service chips, the Your Next Step card, and the Ask KiNDD bar
3. Chat: ask about services near you, then tap the provider cards in the answer
4. Add the KiNDD widget to your home screen and tap it
5. Search "regional center" in Spotlight and try the shortcuts
6. Dark mode on every screen
```


## Screenshots Location

Screenshots are saved at:
`/Users/alexbeattie/Developer/CHLA/chla-ios/screenshots/`

Required for 6.7" display (1290 x 2796):

- 01_main_map_6.7.png
- 02_regional_centers_6.7.png
- 03_resources_list_6.7.png
- 04_about_6.7.png
- 05_map_providers_6.7.png

## App Icon Location

`/Users/alexbeattie/Developer/CHLA/chla-ios/CHLA-iOS/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon.png`

---

## Checklist for App Store Connect

1. [ ] Go to [https://appstoreconnect.apple.com](https://appstoreconnect.apple.com)
2. [ ] Click "+" → "New App"
3. [ ] Fill in:
  - Platform: iOS
  - Name: NDD Resource Map
  - Primary Language: English (U.S.)
  - Bundle ID: com.nddresources.map
  - SKU: ndd-resource-map-001
4. [ ] Add App Information:
  - Subtitle
  - Category
  - Age Rating
5. [ ] Add Privacy Policy URL
6. [ ] Upload Screenshots
7. [ ] Write Description
8. [ ] Add Keywords
9. [ ] Save and prepare for submission

