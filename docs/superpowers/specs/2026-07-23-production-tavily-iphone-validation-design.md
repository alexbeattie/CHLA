# Production Tavily and Physical iPhone Validation Design

## Goal

Enable grounded current-facts search in the live KiNDD backend and prove the
result through the production API and the KiNDD app running on Alex's physical
iPhone.

## Current State

- Production is `chla-api-docker2` in `us-west-2`.
- The environment is Ready and Green on application version
  `app-68a2-260722_144248007813`.
- That deployed revision already contains the Tavily client, current-facts
  routing, and `TAVILY_API_KEY` environment lookup.
- `kindd/prod/tavily-api-key` does not currently exist in AWS Secrets Manager.
- `TAVILY_API_KEY` is not currently configured in Elastic Beanstalk.
- The physical-device Debug and Release builds both use
  `https://api.kinddhelp.com/api/llm`.
- Xcode currently sees `Alex iPhone`, but the device is offline.

## Approach

Use a configuration-only production rollout. Do not deploy application code,
merge branches, or package the dirty local checkout.

1. Create or retrieve a dedicated Tavily production API key through Tavily's
   authenticated dashboard.
2. Store it in AWS Secrets Manager as `kindd/prod/tavily-api-key`.
3. Copy the secret into the Elastic Beanstalk application environment as
   `TAVILY_API_KEY` using an in-memory AWS SDK operation. Do not print, persist,
   or pass the plaintext key through chat, shell history, a repository file, or
   a command-line argument.
4. Wait for the environment configuration update to finish and return to
   Ready/Green.
5. Verify the production current-facts request returns a `**Sources**` section
   with at least one Tavily-returned URL.
6. Run the deterministic classic eval with Tavily injected only into that
   process from Secrets Manager; do not add the production secret to a local
   `.env` file.
7. Connect, unlock, and trust the physical iPhone, build/install KiNDD through
   Xcode, submit the same current-facts question, and capture visible on-device
   proof of the sourced answer.

## Secret Handling

The Tavily key is sensitive.

- The key must never appear in console output, tool output, screenshots, chat,
  documentation, git diffs, or process arguments.
- The AWS secret value is read and forwarded in memory.
- Verification may report only presence, metadata, and service behavior.
- Local development does not retain the production key after the eval process
  exits.

## Production Mutation and Rollback

The only live mutation is adding `TAVILY_API_KEY` to the Elastic Beanstalk
application environment. Elastic Beanstalk may restart or replace application
processes while applying the configuration.

Before the mutation, record environment status, health, version, and the set of
environment-variable names. Afterward, poll until the environment is
Ready/Green and verify the normal health endpoint before testing chat.

If health degrades or chat requests fail unexpectedly:

1. remove only the new `TAVILY_API_KEY` option from the environment;
2. wait for Ready/Green;
3. verify the normal health endpoint again; and
4. keep the Secrets Manager secret for diagnosis unless the key itself is
   suspected compromised.

## Production API Proof

Use a synthetic, non-personal prompt:

```text
Who is the current director of the UCLA autism center?
```

Success requires:

- HTTP 200 from the production chat endpoint;
- a direct answer that does not claim a changing fact without evidence;
- a visible `**Sources**` section;
- at least one valid HTTPS URL returned from the live search context; and
- no raw HTML or unsupported mobile Markdown.

The local LangSmith regression command should then score
`current-facts-web-search` as passing `sources_cited`. Any other failing case is
reported separately rather than hidden.

## Physical iPhone Proof

The iPhone test uses the production API already embedded in the physical-device
build. It does not require TestFlight.

Success requires:

- Xcode reports `Alex iPhone` online and available as a run destination;
- the KiNDD target builds, installs, and launches on the device;
- the production current-facts prompt can be submitted from Ask KiNDD;
- the completed answer visibly contains the grounded source link or Sources
  section on the device; and
- the backend trace or log time aligns with the on-device request.

If the device remains offline, production API verification may complete, but
the task remains incomplete until physical-device proof is captured.

## Out of Scope

- Shipping a new TestFlight build.
- Deploying local backend code.
- Modifying iOS API-routing code.
- Storing Tavily credentials in git, local `.env` files, or GitHub Actions.
- Claiming the full 12-case eval suite passes if only the current-facts case is
  verified.
