# Slack Channel Setup Packet

Operator-facing instructions to set up the KiNDD - NDD Resource Navigator stakeholder Slack channel, organize the shared Google Drive folder, rename documents for stakeholder readability, and post the welcome/sequence messages.

This packet is for the person standing up the channel and the Drive folder. The clinical-handoff context lives in [docs/CLINICAL_TEAM_HANDOFF.md](./CLINICAL_TEAM_HANDOFF.md) and the curated reading order lives in [docs/NONPROFIT_ONBOARDING_PACKET.md](./NONPROFIT_ONBOARDING_PACKET.md).

---

## 1. Inputs You Need Before Starting

Collect these once. You will paste them into the Slack posts and Drive folder description below.

| Input                                                 | Example                                                                                   | Where it goes             |
| ----------------------------------------------------- | ----------------------------------------------------------------------------------------- | ------------------------- |
| Slack channel name                                    | `#kindd-stakeholders`                                                                     | Slack channel creation    |
| Slack channel purpose                                 | `Stakeholder coordination for KiNDD - NDD Resource Navigator`                             | Slack channel description |
| Google Drive handoff folder URL                       | `https://drive.google.com/drive/folders/1FMphfJJ10m4WVNLeTgr4P8EG_T1fbr1Q?usp=share_link` | Posts 2 and 3             |
| Product owner name and Slack handle                   | `<name> @<slack>`                                                                         | Welcome post + Post 6     |
| Engineering on-call name and Slack handle             | `<name> @<slack>`                                                                         | Post 6                    |
| Clinical liaison name and Slack handle                | `<name> @<slack>`                                                                         | Post 6                    |
| Nonprofit registration operator name and Slack handle | `<name> @<slack>`                                                                         | Post 5 + Post 6           |
| Data snapshot date stamp                              | `YYYYMMDD`                                                                                | Post 3                    |

If any input is unknown, leave the placeholder in angle brackets so it is obvious during review.

---

## 2. Google Drive Folder Layout

Open the handoff folder:

<https://drive.google.com/drive/folders/1FMphfJJ10m4WVNLeTgr4P8EG_T1fbr1Q?usp=share_link>

Inside that folder, create the following subfolders in this exact order. The numeric prefix forces correct sort order in the Drive UI.

- `00_Start_Here`
- `01_Product_And_Clinical_Context`
- `02_Slack_Channel_Posts`
- `03_Data_Snapshots_Restricted`
- `04_Nonprofit_Registration`
- `05_Technical_Reference`
- `99_Archive_Do_Not_Post`

Inside `03_Data_Snapshots_Restricted`, create two subfolders:

- `Postgres`
- `RAG`

Access control:

- Restrict `03_Data_Snapshots_Restricted` to the smallest reasonable group: product owner, engineering on-call, nonprofit operator if they need it, and named clinical reviewers.
- All other subfolders can use the same access list as the parent handoff folder.
- Do not post raw dump files or `.env` content into Slack at any time. Slack links to Drive, never the other way around.

---

## 3. Document Upload And Rename Map

Upload these repo documents to Drive with stakeholder-friendly filenames. The left column is the path inside the repo. The right column is the target Drive path under the handoff folder.

| Source in repo                                                                            | Drive destination                                                     |
| ----------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| [docs/NONPROFIT_ONBOARDING_PACKET.md](./NONPROFIT_ONBOARDING_PACKET.md)                   | `00_Start_Here/Start_Here_KiNDD_NDD_Resource_Navigator_Onboarding.md` |
| [docs/CLINICAL_TEAM_HANDOFF.md](./CLINICAL_TEAM_HANDOFF.md)                               | `00_Start_Here/Clinical_Team_Handoff.md`                              |
| [README.md](../README.md)                                                                 | `01_Product_And_Clinical_Context/Project_Overview.md`                 |
| [docs/PLATFORM_STRATEGY_BRIEF.md](./PLATFORM_STRATEGY_BRIEF.md)                           | `01_Product_And_Clinical_Context/Platform_Strategy_Brief.md`          |
| [docs/REGIONAL_CENTERS_CONCEPT.md](./REGIONAL_CENTERS_CONCEPT.md)                         | `01_Product_And_Clinical_Context/Regional_Centers_Concept.md`         |
| [chla-ios/README.md](../chla-ios/README.md)                                               | `01_Product_And_Clinical_Context/iOS_App_Overview.md`                 |
| [chla-ios/HOME_PAGE_REDESIGN_SPEC.md](../chla-ios/HOME_PAGE_REDESIGN_SPEC.md)             | `01_Product_And_Clinical_Context/iOS_Home_Page_Redesign_Spec.md`      |
| [chla-ios/APP_STORE_SUBMISSION.md](../chla-ios/APP_STORE_SUBMISSION.md)                   | `01_Product_And_Clinical_Context/App_Store_Submission_Copy.md`        |
| [docs/SLACK_CHANNEL_SETUP_PACKET.md](./SLACK_CHANNEL_SETUP_PACKET.md)                     | `02_Slack_Channel_Posts/Slack_Channel_Setup_Packet.md`                |
| [docs/DATA_EXPORT_AND_DRIVE_UPLOAD_RUNBOOK.md](./DATA_EXPORT_AND_DRIVE_UPLOAD_RUNBOOK.md) | `03_Data_Snapshots_Restricted/Data_Export_And_Upload_Runbook.md`      |
| [docs/NONPROFIT_REGISTRATION_TASK_BRIEF.md](./NONPROFIT_REGISTRATION_TASK_BRIEF.md)       | `04_Nonprofit_Registration/Nonprofit_Registration_Task_Brief.md`      |
| [autism_rag/README.md](../autism_rag/README.md)                                           | `05_Technical_Reference/NDD_RAG_System_Overview.md`                   |
| [ADMIN_DATA_ENTRY_GUIDE.md](../ADMIN_DATA_ENTRY_GUIDE.md)                                 | `05_Technical_Reference/Admin_Data_Entry_Guide.md`                    |
| [docs/API_AND_DATA_ISSUES.md](./API_AND_DATA_ISSUES.md)                                   | `05_Technical_Reference/API_And_Data_Quality_Issues.md`               |

Notes:

- Upload the source files as either `.md` or export them to Google Docs format. Markdown is preferred so the originals are easy to keep in sync; Docs is acceptable if the stakeholders prefer in-Drive comments.
- Preserve the rename exactly as listed. The new names are the names stakeholders should see.
- Do not edit the contents while uploading. Edits should happen in the repo and be re-uploaded.

---

## 4. Data Snapshots Placement

Before posting in Slack, the data exports must be produced and uploaded per the runbook in [docs/DATA_EXPORT_AND_DRIVE_UPLOAD_RUNBOOK.md](./DATA_EXPORT_AND_DRIVE_UPLOAD_RUNBOOK.md).

Target Drive locations:

- `03_Data_Snapshots_Restricted/Postgres/kindd_postgres_full_YYYYMMDD.dump`
- `03_Data_Snapshots_Restricted/Postgres/kindd_postgres_full_YYYYMMDD.sha256`
- `03_Data_Snapshots_Restricted/RAG/ndd_rag_artifacts_YYYYMMDD.tar.gz`
- `03_Data_Snapshots_Restricted/RAG/pinecone_index_inventory_YYYYMMDD.json`

Replace `YYYYMMDD` with the actual UTC date of the export.

---

## 5. Channel Creation

1. Create the Slack channel using the inputs from section 1.
2. Set the channel topic to: `KiNDD - NDD Resource Navigator stakeholder coordination`.
3. Set the channel description to: `Stakeholder coordination, clinical handoff, data snapshots, and nonprofit registration progress for KiNDD - NDD Resource Navigator. Drive folder is the source of truth.`
4. Add the people identified in section 1 plus any other approved stakeholders.
5. Do not enable cross-workspace sharing until access scope is confirmed.

---

## 6. Posts To Make, In Order

Post these in the order shown. Pin the welcome post. Reply to the welcome post in a thread when posting the data snapshot details so the access link is not in the main channel scroll.

All posts use plain text labels rather than emojis. Keep the inputs from section 1 substituted before posting.

### Welcome post (pin this)

```text
Welcome to the KiNDD - NDD Resource Navigator stakeholder channel.

What this channel is for
- Stakeholder coordination across clinical, product, engineering, and nonprofit setup.
- Sharing context docs and data snapshot access from the Drive folder.
- Tracking the nonprofit registration workstream.

What this channel is not
- Not a place to post raw database files, API keys, or PHI.
- Not a substitute for the Drive folder. The Drive folder is the source of truth.

Owners
- Product owner: <name> @<slack>
- Engineering on-call: <name> @<slack>
- Clinical liaison: <name> @<slack>
- Nonprofit registration operator: <name> @<slack>

Start here
- Read the start-here doc in the Drive folder under 00_Start_Here.
- Then follow the recommended presentation order in that doc.

Drive folder: <paste handoff folder URL>
```

### Post 1: Start here and presentation order

```text
Start here

The curated onboarding packet is in Drive at:
00_Start_Here/Start_Here_KiNDD_NDD_Resource_Navigator_Onboarding.md

Then read in this order:
1. 01_Product_And_Clinical_Context/Project_Overview.md
2. 01_Product_And_Clinical_Context/Platform_Strategy_Brief.md
3. 01_Product_And_Clinical_Context/Regional_Centers_Concept.md
4. 01_Product_And_Clinical_Context/iOS_App_Overview.md
5. 01_Product_And_Clinical_Context/iOS_Home_Page_Redesign_Spec.md
6. 01_Product_And_Clinical_Context/App_Store_Submission_Copy.md
7. 00_Start_Here/Clinical_Team_Handoff.md
8. 05_Technical_Reference/NDD_RAG_System_Overview.md
9. 05_Technical_Reference/Admin_Data_Entry_Guide.md
10. 05_Technical_Reference/API_And_Data_Quality_Issues.md
```

### Post 2: Drive folder map

```text
Drive folder map

Handoff folder: <paste handoff folder URL>

00_Start_Here              -> Onboarding packet and clinical handoff
01_Product_And_Clinical_Context -> Product, strategy, and clinical context docs
02_Slack_Channel_Posts     -> This packet, kept for reference
03_Data_Snapshots_Restricted -> Postgres and RAG snapshots, restricted access
04_Nonprofit_Registration  -> Task brief for the nonprofit operator and outputs
05_Technical_Reference     -> Implementation, data entry, and known issues
99_Archive_Do_Not_Post     -> Historical material, not for stakeholder reading

Always link to Drive from this channel. Do not attach raw dumps, .env files, or credentials.
```

### Post 3: Data snapshots and access rules

```text
Data snapshots

A full Postgres/PostGIS operational dump and a current RAG corpus snapshot are produced per the runbook in 03_Data_Snapshots_Restricted/Data_Export_And_Upload_Runbook.md.

Snapshot date: YYYYMMDD

Postgres
- 03_Data_Snapshots_Restricted/Postgres/kindd_postgres_full_YYYYMMDD.dump
- 03_Data_Snapshots_Restricted/Postgres/kindd_postgres_full_YYYYMMDD.sha256

RAG
- 03_Data_Snapshots_Restricted/RAG/ndd_rag_artifacts_YYYYMMDD.tar.gz
- 03_Data_Snapshots_Restricted/RAG/pinecone_index_inventory_YYYYMMDD.json

Access rules
- Access to 03_Data_Snapshots_Restricted is by request only.
- Reply in thread with your name and what you need access for.
- Do not redistribute, re-upload, or attach these files outside the approved Drive folder.
```

### Post 4: Clinical review instructions

```text
Clinical review

Cadence
- Monday: 3-5 items to review (data slices, screens, RAG questions).
- Mid-week: clinicians reply in thread with findings.
- Friday: product/eng summarize what changed.

Labels (use as the first word of your reply)
- Data: data quality issue (wrong info, missing provider, bad mapping).
- Clinical: clinical accuracy or wording issue.
- Product: UX or workflow suggestion.

PHI or named patient information
- Do not post in-channel. DM the product owner instead.
```

### Post 5: Nonprofit registration task handoff

```text
Nonprofit registration workstream

Operator: <name> @<slack>

Brief: 04_Nonprofit_Registration/Nonprofit_Registration_Task_Brief.md

The brief covers jurisdiction discovery, name and brand checks, incorporation, EIN, bylaws, board setup, federal exemption path, state charitable registration, banking, bookkeeping, and a privacy/compliance review for NDD-adjacent material.

Status updates: reply in thread under this post weekly. Drop major outputs into 04_Nonprofit_Registration as they are produced.
```

### Post 6: Owners and escalation

```text
Owners and escalation

- Product owner: <name> @<slack>
- Engineering on-call: <name> @<slack>
- Clinical liaison: <name> @<slack>
- Data/DB questions: <name> @<slack>
- Research RAG questions: <name> @<slack>
- Nonprofit registration: <name> @<slack>

Escalation
- Anything urgent or PHI-adjacent: DM the product owner.
- Anything that looks like a data leak, credential exposure, or security issue: DM the engineering on-call.
```

---

## 7. After Posting Checklist

- [ ] Channel created with the agreed name, topic, description, and members.
- [ ] Welcome post pinned.
- [ ] Posts 1 through 6 published in order in the main channel.
- [ ] Drive folder layout matches section 2.
- [ ] All docs in section 3 are uploaded with the new names.
- [ ] Postgres and RAG snapshots are uploaded per section 4 and the data runbook.
- [ ] `03_Data_Snapshots_Restricted` access is limited to approved stakeholders.
- [ ] Nonprofit operator confirmed they have access to `04_Nonprofit_Registration`.
- [ ] First weekly review post scheduled.

If you cannot complete any item, leave a placeholder reply in the welcome thread describing what is missing and who can resolve it.
