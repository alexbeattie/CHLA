# Google Play Data safety draft

This worksheet is a code-informed draft for the Play Console form. It is not a
final legal determination. The privacy owner must confirm vendor configuration,
retention, deletion, and whether each processor qualifies as a service provider
before submission.

## App-level answers supported by the current Android code

- Data is encrypted in transit: **Yes**. App API and linked production endpoints
  use HTTPS.
- Users can create an account: **No**.
- Users can request account deletion: **Not applicable** because the app does not
  create user accounts.
- Ads: **No**. No advertising SDK is included.
- Background location: **No**.
- Device location permission: **Optional**. If it is declined or unavailable,
  the user must enter a ZIP code to complete the onboarding profile.
- Data sale: **Requires organization confirmation**; the code alone does not
  establish the Play Console answer.

## Data types to declare conservatively

| Play data category | Collected | Shared | Optional | Purpose | Evidence / confirmation needed |
| --- | --- | --- | --- | --- | --- |
| Approximate location | Yes | Confirm | No | App functionality | A ZIP is required to complete the profile. Foreground coarse location is optional: onboarding passes a coarse fix to the device's geocoding service to derive a ZIP, which may use a network service depending on the device implementation; user-initiated nearby provider discovery sends coarse coordinates to the KiNDD API. |
| Precise location | No | No | Not applicable | Not applicable | This release does not request fine or background location permission, and its fused-location requests use coarse-compatible accuracy. |
| Health information | Yes | Confirm | Yes | App functionality | Age group, diagnosis, insurance, journey context, and health-related chat prompts may be submitted by a user. Confirm the exact Play classification for insurance and journey fields. |
| In-app search history | Yes | Confirm | Yes | App functionality | Resource search text and filters are sent to KiNDD APIs. |
| Other user-generated content | Yes | Confirm | Yes | App functionality; safety | Ask KiNDD prompts and AI-response reports are sent to KiNDD services. Reports use a fixed reason, the reported assistant response, and a one-way digest of a short-lived answer-bound fingerprint; they do not request free-text explanations or use a client IP/device identifier for report throttling. |
| App interactions | Yes | Confirm | No | App functionality; analytics | Google Maps SDK may collect map interactions and request metadata. Confirm current SDK configuration and the applicable Play service-provider treatment. |
| Device or other IDs | Yes | Confirm | No | App functionality; analytics | Google Maps SDK documents a pseudonymous SDK identifier and IP address processing for usage measurement and service improvement. Confirm the applicable Play service-provider treatment. |
| Crash logs / diagnostics | Yes | Confirm | No | App functionality; diagnostics | Google Maps SDK documents stack traces and crash metrics. No separate Firebase Crashlytics SDK is present. Confirm the applicable Play service-provider treatment. |

“Shared” above must be reconciled with Play's service-provider exceptions. Do not
change an answer to “not shared” solely because a processor acts on KiNDD's
behalf; verify the applicable Play definition and contracts first.

## Processors and destinations to verify

- KiNDD API (`api.kinddhelp.com`): request logging, database persistence,
  retention, deletion, and operator access.
- AWS Bedrock / Anthropic: Ask KiNDD prompt and response processing, region,
  model-invocation logging, abuse monitoring, and retention configuration.
- Google Maps Platform / Google Play services: current automatic collection and
  controller/service-provider role.
- Device geocoding service: implementation/provider, whether coordinates leave
  the device during ZIP lookup, retention, and controller/service-provider role.
- Tavily: whether any Android-originated chat path sends user-entered text and
  what is retained.
- Langfuse: whether production is enabled, which prompt/response fields are
  transmitted, access controls, region, and retention.

## Health Apps declaration draft

- App category: health information / health-services navigation.
- The app helps users find developmental services and obtain general educational
  information.
- Whether the app is a regulated medical device, and the related declaration
  wording, require privacy-owner or counsel confirmation before submission.
- The store description includes the required medical disclaimer and directs
  users to a qualified healthcare professional for medical advice.
- Device location permission is optional. A ZIP is still required to complete
  the profile, and approximate location is used for Regional Center matching
  and user-initiated nearby-resource discovery.

## Required confirmations before Play submission

1. Privacy owner approves the classifications and the final “collected” and
   “shared” answers.
2. Backend owner documents retention/deletion for searches, chat prompts,
   AI-response reports, and operational logs.
3. Vendor configuration for AWS Bedrock, Tavily, Langfuse, and Google Maps is
   confirmed against production.
4. The public privacy policy is updated before a closed, open, or production
   Play release.
5. The Play form is reviewed again whenever an SDK, API payload, or production
   telemetry configuration changes.
