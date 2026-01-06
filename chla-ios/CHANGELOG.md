# Changelog

All notable changes to the KiNDD iOS app will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-01-06

### ✨ Major New Feature: AI-Powered Chat (Ask KiNDD)

#### Streaming AI Chat

- Real-time streaming responses powered by AWS Bedrock (Claude 3.5)
- Location-aware answers using your GPS-detected ZIP code
- Knows which Regional Center serves your area
- Contextual responses based on your diagnosis, insurance, child's age

#### Smart Action Buttons

- AI responses include tappable action buttons
- "Find ABA Providers" → Opens filtered provider list
- "Regional Centers" → View all 7 LA County centers
- "Open Map" → Jump to map view
- No more copy/paste - tap to navigate!

#### Quick Prompt Capsules

- Horizontal scrolling bar with 8 common questions
- Find providers, Assessment, My RC, Early Start, Insurance, Waitlists, Age 3 transition, Speech
- Tap any capsule to ask instantly

#### Clickable Links

- Phone numbers are tappable (opens dialer)
- URLs are clickable (opens browser)
- Markdown formatting (bold, lists, headers)

#### Message Actions

- 👍 Like / 👎 Dislike responses
- 📋 Copy individual messages
- 📤 Share messages or entire chat
- Export formatted conversation with date, location, regional center

### 🎨 UI Improvements

- Wider chat bubbles for better readability
- Smoother streaming animation (50ms batched updates)
- Subtle transitions when streaming completes

### 🔧 Technical

- AWS Strands Agent SDK integration with custom tools
- Semantic search with pgvector embeddings
- SSE (Server-Sent Events) for real-time streaming
- Location-to-ZIP reverse geocoding

---

## [1.1.0] - 2026-01-05

### ✨ New Features

#### Beautiful New Home Screen

- Brand new homepage featuring the KiNDD logo and branding
- Inline ZIP code search with instant regional center detection
- Quick action cards: Near Me, Map, and Browse
- Therapy type cards for quick filtering (ABA, Speech, OT, PT)
- Your Regional Center card (when location available)
- At-a-glance stats: 370+ providers, 7 regional centers

#### Personalization

- Easy access to "Change My Preferences" via sparkle ✨ icon
- Re-run onboarding anytime to update your preferences

#### Improved Navigation

- New 5-tab layout: Home, Map, Regions, List, More
- Auto-show controls on Map after 5 seconds of hiding
- "Show Controls" button when map UI is hidden

#### Therapy Type Filtering

- Tap therapy cards on Home to filter provider list
- Visual filter chips show active filters
- Easy one-tap filter removal

### 🐛 Bug Fixes

- Fixed navigation controls sometimes not returning on Map view
- Fixed ZIP code search returning incorrect results for some areas

### 🎨 UI/UX Improvements

- Clean, light gradient hero section
- KiNDD branding throughout the app
- Improved readability and accessibility
- App display name shortened to "KiNDD" (was truncated before)

### 📱 Technical

- Updated to version 1.1.0 (build 3)
- Added KiNDD logo to asset catalog
- Improved state management for filters

---

## [1.0.1] - 2025-12-15

### Initial Release

- Interactive map with 370+ developmental disability service providers
- 7 LA County Regional Centers with boundaries
- Location-based search
- Provider filtering by therapy type, age group, diagnosis
- Provider detail views with contact information
- Directions integration with Apple Maps
- Onboarding flow for personalization

---

## App Store Release Notes

### Version 1.2.0

```
What's New in Version 1.2.0

🤖 NEW: Ask KiNDD - AI-Powered Chat
• Get instant answers about developmental services
• AI knows your location and suggests your Regional Center
• Streaming responses in real-time
• Tappable phone numbers and website links

⚡ Smart Action Buttons
• AI suggests relevant actions you can tap
• "Find ABA Providers" - jumps to filtered list
• "Regional Centers" - view all 7 LA County centers
• No more copy/paste!

💬 Quick Prompts
• 8 common questions at your fingertips
• Ask about assessments, waitlists, insurance, and more
• One tap to get answers

📱 Better Experience
• Wider chat bubbles for readability
• Smooth streaming animations
• Export and share conversations
• Like/dislike responses
```

### Version 1.1.0

```
What's New in Version 1.1.0

🏠 New Home Screen
• Beautiful new homepage with KiNDD branding
• Quick ZIP code search with instant regional center detection
• One-tap access to therapy type filters
• Quick action cards for Near Me, Map, and Browse

✨ Personalization
• Easily update your preferences anytime
• New sparkle menu for quick settings access

🗺️ Improved Navigation
• Better map controls that auto-show after hiding
• Smoother transitions between tabs

🐛 Bug Fixes
• Fixed navigation controls occasionally not returning
• Improved ZIP code search accuracy
```
