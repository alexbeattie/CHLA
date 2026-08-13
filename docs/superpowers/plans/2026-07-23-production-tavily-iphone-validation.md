# Production Tavily and Physical iPhone Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Configure Tavily for the live KiNDD backend and prove a grounded current-facts answer through both the production API and the KiNDD app on Alex's physical iPhone.

**Architecture:** Perform a configuration-only rollout against the already-deployed Tavily-capable backend. Keep the API key out of chat, source control, local environment files, console output, and command arguments; store it in AWS Secrets Manager and forward it in memory to Elastic Beanstalk. Verify backend health, current-facts citations, the deterministic eval, and finally the production-connected physical iPhone app.

**Tech Stack:** Tavily dashboard, AWS Secrets Manager, AWS Elastic Beanstalk, Django/Bedrock production API, LangSmith evals, Xcode 26, `xcrun devicectl`, SwiftUI iOS app.

## Global Constraints

- Do not deploy application code, merge branches, or package the dirty local checkout.
- The only live application mutation is adding `TAVILY_API_KEY` to `chla-api-docker2`.
- Never expose the Tavily key in chat, logs, screenshots, tool output, shell history, repository files, local `.env` files, or process arguments.
- Use the AWS profile `personal` and region `us-west-2`.
- Store the key as `kindd/prod/tavily-api-key`.
- Require Elastic Beanstalk to return to Ready/Green and verify the ordinary API health path before testing chat.
- Use only the synthetic prompt `Who is the current director of the UCLA autism center?`.
- Do not claim the full 12-case suite passes if only the current-facts case passes.
- Do not claim physical-device completion until visible on-device behavior and a corresponding backend request are observed.

---

## Operational Surfaces

- Tavily dashboard: creates or exposes the dedicated production key.
- AWS Secrets Manager: stores `kindd/prod/tavily-api-key`.
- Elastic Beanstalk environment `chla-api-docker2`: receives `TAVILY_API_KEY`.
- `https://api.kinddhelp.com/api/llm/ask/`: production proof endpoint.
- `maplocation/llm/management/commands/run_llm_evals.py`: deterministic regression command.
- `chla-ios/CHLA-iOS.xcodeproj`: physical-device build.
- `chla-ios/CHLA-iOS/Services/LLMService.swift`: already routes physical builds to production.

No repository source file needs modification.

### Task 1: Create and Securely Store the Tavily Production Key

**Files:**
- Modify: none

**Interfaces:**
- Produces: AWS secret `kindd/prod/tavily-api-key` containing one Tavily API key.
- Security boundary: the plaintext moves only through browser clipboard/paste and AWS-managed fields; the agent never reads or prints it.

- [ ] **Step 1: Open the authenticated Tavily dashboard**

Navigate to the official Tavily dashboard in the selected browser. If sign-in,
account creation, CAPTCHA, billing, or legal acceptance is required, stop at
that private/consequential gate and ask the user to complete it.

Expected: the dashboard shows API-key management without exposing a key in
tool output.

- [ ] **Step 2: Ask for action-time confirmation**

Immediately before generating a new API key or configuring an existing key for
ongoing production access, ask the user to confirm these batched actions:

```text
Create or use a Tavily production API key, store it in AWS Secrets Manager as
kindd/prod/tavily-api-key, and grant the live Elastic Beanstalk backend ongoing
access through TAVILY_API_KEY.
```

Expected: explicit user confirmation.

- [ ] **Step 3: Copy the key without reading it**

Use Tavily's Copy control. Do not inspect page storage, clipboard contents,
input values, or any DOM/screenshot region that renders the plaintext key.

Expected: the key is in the browser clipboard but absent from tool output.

- [ ] **Step 4: Store the key in AWS Secrets Manager**

Open the AWS Secrets Manager create-secret flow for `us-west-2`, choose a plain
text secret, paste with the browser's normal paste action, and create:

```text
kindd/prod/tavily-api-key
```

Do not use a description containing credentials. Do not enable automatic
rotation unless Tavily provides a supported rotation mechanism.

Expected: AWS confirms the secret name. The plaintext value is never read back.

- [ ] **Step 5: Verify only secret metadata**

Run:

```bash
aws secretsmanager describe-secret \
  --profile personal \
  --region us-west-2 \
  --secret-id kindd/prod/tavily-api-key \
  --query '{Name:Name,LastChangedDate:LastChangedDate}' \
  --output json
```

Expected: name `kindd/prod/tavily-api-key` and a current change timestamp.

### Task 2: Configure Tavily in Production with Rollback Protection

**Files:**
- Modify: none

**Interfaces:**
- Consumes: `kindd/prod/tavily-api-key`.
- Produces: Elastic Beanstalk option `TAVILY_API_KEY` on `chla-api-docker2`.
- Rollback: remove only `TAVILY_API_KEY` from the environment.

- [ ] **Step 1: Capture the production baseline**

Run:

```bash
aws elasticbeanstalk describe-environments \
  --profile personal \
  --region us-west-2 \
  --environment-names chla-api-docker2 \
  --query 'Environments[0].{Status:Status,Health:Health,VersionLabel:VersionLabel,CNAME:CNAME}' \
  --output json
```

Expected: `Status` is `Ready` and `Health` is `Green`.

- [ ] **Step 2: Apply the key using an in-memory AWS SDK operation**

Run this code without printing the key:

```python
import boto3

session = boto3.Session(profile_name="personal", region_name="us-west-2")
secrets = session.client("secretsmanager")
beanstalk = session.client("elasticbeanstalk")

key = secrets.get_secret_value(
    SecretId="kindd/prod/tavily-api-key"
)["SecretString"]
if not key.strip():
    raise RuntimeError("Tavily secret is empty")

response = beanstalk.update_environment(
    EnvironmentName="chla-api-docker2",
    OptionSettings=[
        {
            "Namespace": "aws:elasticbeanstalk:application:environment",
            "OptionName": "TAVILY_API_KEY",
            "Value": key,
        }
    ],
)
print(
    {
        "EnvironmentName": response["EnvironmentName"],
        "Status": response["Status"],
        "VersionLabel": response["VersionLabel"],
    }
)
```

Expected: `chla-api-docker2` enters an updating state without revealing the
secret.

- [ ] **Step 3: Poll until Ready/Green**

Poll `describe-environments` at intervals no longer than 30 seconds until:

```json
{"Status": "Ready", "Health": "Green"}
```

If the environment becomes Red or does not recover, remove only the new option:

```python
beanstalk.update_environment(
    EnvironmentName="chla-api-docker2",
    OptionsToRemove=[
        {
            "Namespace": "aws:elasticbeanstalk:application:environment",
            "OptionName": "TAVILY_API_KEY",
        }
    ],
)
```

- [ ] **Step 4: Verify config name and normal health**

List environment option names and assert `TAVILY_API_KEY` is present without
requesting values. Then run:

```bash
curl --fail --silent --show-error \
  https://api.kinddhelp.com/api/regional-centers/ \
  >/dev/null
```

Expected: option name present and curl exits 0.

### Task 3: Prove Production Current-Facts Grounding and Eval Behavior

**Files:**
- Modify: none

**Interfaces:**
- Consumes: live `TAVILY_API_KEY` configuration.
- Produces: sanitized production response evidence and a LangSmith experiment result.

- [ ] **Step 1: Call the production chat endpoint**

POST this JSON:

```json
{
  "query": "Who is the current director of the UCLA autism center?",
  "context": {},
  "locale": "en"
}
```

to:

```text
https://api.kinddhelp.com/api/llm/ask/
```

Save the response only to a temporary file created with `mktemp`; remove that
temporary file after validation.

- [ ] **Step 2: Validate the production response**

Parse the response and assert:

```python
answer = payload["answer"]
assert "**Sources**" in answer
assert "https://" in answer
assert "<div" not in answer
assert "<br" not in answer
```

Print only:

```text
production_current_facts=http_200,sources_present,https_url_present,ios_markdown_clean
```

Expected: all assertions pass.

- [ ] **Step 3: Run the deterministic classic eval with an ephemeral key**

Use one Python process to:

1. retrieve the Tavily key from Secrets Manager;
2. set `os.environ["TAVILY_API_KEY"]` in memory;
3. call Django's `run_llm_evals` command with
   `runtime="classic"`, `skip_judge=True`, and `json=True`; and
4. exit without writing the key locally.

Expected: the `current-facts-web-search` case passes `sources_cited`. Report
any other failing case separately.

- [ ] **Step 4: Record the LangSmith experiment link**

Keep the experiment name and dashboard URL returned by the command. Do not
include credentials or raw private traces in the handoff.

### Task 4: Build, Install, and Verify on Alex iPhone

**Files:**
- Modify: none

**Interfaces:**
- Consumes: `chla-ios/CHLA-iOS.xcodeproj`, scheme `CHLA-iOS`, destination name `Alex iPhone`.
- Produces: installed KiNDD app and visible sourced current-facts answer on the device.

- [ ] **Step 1: Wait for the device to become available**

Run:

```bash
xcrun xctrace list devices
```

Expected: `Alex iPhone` appears under `Devices`, not `Devices Offline`.

If it is offline, ask the user to connect, unlock, trust the Mac, and enable
Developer Mode if prompted.

- [ ] **Step 2: Resolve the physical-device destination**

Run:

```bash
xcodebuild \
  -project chla-ios/CHLA-iOS.xcodeproj \
  -scheme CHLA-iOS \
  -showdestinations
```

Expected: one available iOS destination matching `Alex iPhone`.

- [ ] **Step 3: Build for the physical device**

Use a temporary DerivedData directory and the exact device name:

```bash
KINDD_DERIVED_DATA=$(mktemp -d /tmp/kindd-iphone-build.XXXXXX)
xcodebuild \
  -project chla-ios/CHLA-iOS.xcodeproj \
  -scheme CHLA-iOS \
  -configuration Debug \
  -destination "platform=iOS,name=Alex iPhone" \
  -derivedDataPath "$KINDD_DERIVED_DATA" \
  build
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: Install and launch KiNDD**

Locate the built `.app`, then run:

```bash
xcrun devicectl device install app \
  --device "Alex iPhone" \
  "$KINDD_DERIVED_DATA/Build/Products/Debug-iphoneos/CHLA-iOS.app"

xcrun devicectl device process launch \
  --device "Alex iPhone" \
  com.navigator.kindd
```

Expected: install and launch succeed.

- [ ] **Step 5: Submit the production current-facts prompt**

Use iPhone Mirroring or another visible device surface when available. Open Ask
KiNDD and submit exactly:

```text
Who is the current director of the UCLA autism center?
```

If the Mac cannot control or display the physical device, ask the user to
submit that exact prompt on the phone while the agent observes the corresponding
backend request time.

- [ ] **Step 6: Capture visible proof**

Verify the completed on-device answer contains a Sources section or clickable
source link. Capture a screenshot only if it does not expose personal
notifications, location, account data, or unrelated conversations.

Expected: grounded source is visibly present on the actual iPhone.

- [ ] **Step 7: Correlate the backend request**

Use the production trace/log surface to confirm a chat request at the observed
device-test time. Report only timestamp, runtime, success/failure, and trace ID
if safe; do not expose prompt/answer content beyond the synthetic test.

Expected: one successful production request aligns with the physical-device
interaction.

### Task 5: Final Verification and Handoff

**Files:**
- Modify: none

**Interfaces:**
- Consumes: Tasks 1-4 evidence.
- Produces: gate-by-gate completion report.

- [ ] **Step 1: Recheck production health**

Confirm `chla-api-docker2` is Ready/Green and the normal API health request
still exits 0.

- [ ] **Step 2: Recheck secret/config metadata**

Confirm:

```text
Secrets Manager: kindd/prod/tavily-api-key exists
Elastic Beanstalk: TAVILY_API_KEY option name exists
```

Do not retrieve or print either value.

- [ ] **Step 3: Report each gate separately**

Report:

```text
Production configuration: confirmed
Production API current-facts grounding: confirmed or failed with exact reason
LangSmith deterministic eval: experiment name and per-case result
Physical iPhone build/install: confirmed or failed with exact reason
Physical iPhone sourced answer: confirmed or still awaiting visible proof
Repository deployment: none
Repository source changes: none
```

- [ ] **Step 4: Preserve rollback instructions**

Include the exact `OptionsToRemove` target:

```text
Namespace: aws:elasticbeanstalk:application:environment
OptionName: TAVILY_API_KEY
EnvironmentName: chla-api-docker2
```
