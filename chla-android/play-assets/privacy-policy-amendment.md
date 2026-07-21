# Privacy policy amendment checklist

The current public policy must be revised before Google Play submission. This
checklist records facts that the published policy and Terms should cover; final
wording requires privacy-owner or counsel approval.

## Correct identity and platforms

- Use the current product name: **KiNDD - NDD Resource Navigator**.
- Describe both the Android and iOS applications accurately.
- Replace Android-inaccurate Apple Maps/MapKit or Mapbox-only statements with
  the actual platform services, including Google Maps Platform and Google Play
  services on Android.

## Data users may provide

- Search terms and resource filters.
- ZIP code and optional approximate location.
- Age group, diagnosis, insurance, regional-center, audience, and journey-stage
  selections when used.
- Ask KiNDD prompts and the resulting AI interactions.
- A fixed reason when a user reports an AI response.

Explain which profile fields remain only on the device, which fields are sent
with a request, and that chat history is not intentionally persisted by the
Android app itself.

## Processing and service providers

- KiNDD application servers and database/operational logging.
- AWS Bedrock and the applicable model provider for Ask KiNDD processing.
- Google Maps Platform and Google Play services for maps, network/device
  metadata, diagnostics, IP address, a pseudonymous SDK identifier, and
  configuration-dependent map interactions.
- The device's geocoding service for deriving a ZIP from an optional approximate
  location; depending on the device implementation, this may use a network
  service.
- Tavily and Langfuse only to the extent they are enabled in the production path;
  describe what they receive rather than listing them generically.

## Required policy details

- Purposes for each category of data.
- Whether providing the data is optional and what functionality is unavailable
  without it.
- Retention periods or criteria for API requests, logs, chat/AI telemetry, and
  submitted reports.
- A usable contact and process for access or deletion requests even though the
  app does not create accounts.
- Security practices, international or third-party processing as applicable,
  children's/family use, and policy-change notices.
- A current effective date.

## Product and health scope

- KiNDD provides resource navigation and general educational information.
- It is not a medical device and does not diagnose, treat, cure, or prevent any
  condition.
- Users should consult a qualified healthcare professional for medical advice,
  diagnosis, or treatment and confirm provider eligibility and availability
  directly.

## Release gate

Do not mark the Play Data safety or Health Apps declarations final, or submit a
closed/open/production release, until the revised policy is live at
`https://kinddhelp.org/privacy` and matches the production application and
vendor configuration.
