# Nonprofit Registration Task Brief

Assignment for the operator hired to stand up KiNDD - NDD Resource Navigator as a US nonprofit and pursue federal tax-exempt status. This brief is US-generic. State of incorporation is determined in the first workstream, not assumed.

You will operate against the shared Drive folder:

<https://drive.google.com/drive/folders/1FMphfJJ10m4WVNLeTgr4P8EG_T1fbr1Q?usp=share_link>

All deliverables land in `04_Nonprofit_Registration/`. Status updates go in the Slack channel under post 5.

---

## 1. Role And Mission

Role: Nonprofit Registration Operator
Engagement type: project-based, hourly or fixed scope (owner to confirm)
Reports to: KiNDD product owner
Working language: English

Mission: Take KiNDD - NDD Resource Navigator from "founder-led project" to a properly incorporated US nonprofit with the documents, identifiers, and policies needed to pursue federal 501(c)(3) tax-exempt status and accept charitable donations within the law.

You are not the organization's legal counsel. You coordinate filings, drafts, vendor selection, and admin. Anything that constitutes legal advice (final bylaws review, fiduciary duties guidance, complex tax positions, conflict-of-interest opinions on a specific case) goes to licensed counsel hired separately. Flag those clearly and stop work on the affected line until counsel responds.

---

## 2. Project Background You Need To Know

KiNDD is a care-navigation platform for families seeking neurodevelopmental disorder (NDD) services. Today it includes:

- A web map and provider directory.
- An iOS app branded "NDD Resources / KiNDD".
- A research RAG assistant built over public NDD sources.
- A Postgres/PostGIS operational database.

Read these before you start, all in the Drive folder under `00_Start_Here` and `01_Product_And_Clinical_Context`:

- `Start_Here_KiNDD_NDD_Resource_Navigator_Onboarding.md`
- `Project_Overview.md`
- `Platform_Strategy_Brief.md`
- `Regional_Centers_Concept.md`
- `Clinical_Team_Handoff.md`

If anything in those documents contradicts what is in this brief, the docs win on product facts; this brief wins on process and deliverables.

---

## 3. Primary Objectives

Deliver these in order:

1. A signed decision memo on entity structure (nonprofit corporation, fiscal sponsorship, or other) and state of incorporation.
2. A clean, filed certificate of incorporation in the chosen state.
3. A federal Employer Identification Number (EIN).
4. Adopted bylaws and an initial board with at least the legally required minimum directors.
5. An adopted conflict-of-interest policy and initial board consent / organizational resolutions.
6. A submitted federal tax-exemption application (IRS Form 1023 or 1023-EZ where eligible) and tracking of its status.
7. Completed state-level charitable registration and any fundraising compliance required for the states the organization will solicit in.
8. An operating bank account, basic bookkeeping setup, and donation-handling path.
9. A privacy and compliance memo specific to NDD-adjacent information KiNDD will handle.

The bar for "done" on each objective is in section 9.

---

## 4. Constraints And Guardrails

- Do not provide legal advice unless you are licensed and explicitly engaged for that. Coordinate with counsel where required.
- Do not handle production credentials. The product owner controls AWS, RDS, Pinecone, Cohere, OpenAI, Firecrawl, and Slack secrets.
- Do not post raw database files, `.env` content, or PII/PHI in the Slack channel or in any unrestricted Drive folder.
- Do not commit to a state of incorporation or filing fees without an approved decision memo (workstream A).
- Do not sign vendor contracts, registered agent contracts, or banking documents without the product owner's written approval.
- All filings, drafts, receipts, and tracking IDs land in `04_Nonprofit_Registration/` and are referenced in your weekly Slack update.

---

## 5. Workstreams

Each workstream lists: objective, inputs needed, tasks, outputs, and acceptance criteria. Run A first and finish it before starting C. B, D, and K can be parallelized.

### Workstream A: Discovery and Decision Memo

Objective: Pick the right legal vehicle and the right home state.

Tasks:

1. Read sections 2 and 3 of this brief and the docs listed in section 2.
2. Interview the product owner (target: 60 minutes) to capture:
   - Founder location and existing professional ties.
   - Where the organization will primarily operate, hire, and fundraise.
   - Whether the founder wants a standalone nonprofit corporation, a fiscal sponsorship arrangement (e.g., under an existing 501(c)(3)), or a hybrid (LLC subsidiary, etc.).
   - Risk tolerance for board composition and personal liability.
3. Draft a 2-3 page decision memo covering:
   - Recommended structure (with one or two alternatives ranked).
   - Recommended state of incorporation (state of operation, Delaware, or other) and trade-offs.
   - High-level cost and timeline estimate.
   - Open questions for legal counsel.
4. Review the memo with the product owner, capture decisions, and update it in place.

Outputs (in `04_Nonprofit_Registration/A_Discovery/`):

- `Decision_Memo_v1.md`
- `Founder_Interview_Notes.md`

Acceptance: The product owner has signed off (in Slack thread + memo footer) on entity type and state of incorporation.

### Workstream B: Name And Brand Checks

Objective: Confirm the organization name is available and defensible before filing.

Tasks:

1. Confirm the legal name with the product owner (default: `KiNDD` or `KiNDD - NDD Resource Navigator`; fallback options identified in workstream A).
2. Run availability checks in the chosen state's corporate name database.
3. Run a USPTO TESS preliminary trademark search for the name and any clear variants.
4. Confirm `.org` and `.com` domain status and the existing `kinddhelp.com` hold.
5. Document any conflicts, near-conflicts, or notable risks.
6. Recommend whether to file a state trademark, federal trademark, or defer.

Outputs (in `04_Nonprofit_Registration/B_Name_Brand/`):

- `Name_Availability_Report.md`
- `Domain_And_Trademark_Notes.md`

Acceptance: Owner-approved legal name and a clear go / no-go on the name with documented evidence.

### Workstream C: Incorporation Filing

Objective: File the certificate of incorporation in the approved state.

Tasks:

1. Identify the state filing authority, fee schedule, and processing time.
2. Choose a registered agent (in-state employee, founder, or a paid service). Document trade-offs and cost.
3. Draft the certificate / articles of incorporation. Include purpose language compatible with future 501(c)(3) status (charitable / educational purposes, dissolution clause, prohibited activities clause).
4. Have the product owner review and sign.
5. File electronically where possible. Pay filing fees with an approved payment method.
6. Store the stamped/approved filing and any state confirmations in Drive.

Outputs (in `04_Nonprofit_Registration/C_Incorporation/`):

- `Articles_Of_Incorporation_Final.pdf`
- `Filing_Confirmation_<state>.pdf`
- `Registered_Agent_Engagement.md`

Acceptance: State confirms the entity exists and a copy of the stamped filing is in Drive.

### Workstream D: EIN

Objective: Obtain a federal Employer Identification Number.

Tasks:

1. After incorporation is filed, request the EIN via IRS Form SS-4 (online if eligible).
2. Use the responsible party identified by the product owner.
3. Save the IRS confirmation letter (`CP 575`) to Drive.

Outputs (in `04_Nonprofit_Registration/D_EIN/`):

- `EIN_Confirmation_CP575.pdf`
- `EIN_Notes.md` (records who is the responsible party and why)

Acceptance: EIN issued by the IRS and stored in Drive.

### Workstream E: Bylaws And Board

Objective: Adopt bylaws and seat the founding board.

Tasks:

1. Draft bylaws aligned with the chosen state's nonprofit corporate code and IRS 501(c)(3) requirements (purpose statement, board size and election, officer roles, meeting and quorum rules, indemnification, amendment process, dissolution).
2. Identify the initial board (minimum required by state law; recommend at least three unrelated directors for 501(c)(3) credibility).
3. Confirm each director's willingness in writing.
4. Schedule and hold the organizational meeting (or by unanimous written consent).
5. Capture the meeting minutes.

Outputs (in `04_Nonprofit_Registration/E_Bylaws_Board/`):

- `Bylaws_v1_Adopted.md` (and signed `.pdf`)
- `Initial_Board_Roster.md`
- `Director_Consent_Letters/` (one per director)
- `Organizational_Meeting_Minutes.md`

Acceptance: Bylaws are adopted, the board is seated, and the minutes are stored in Drive.

### Workstream F: Conflict-Of-Interest Policy And Initial Resolutions

Objective: Adopt a conflict-of-interest policy and the resolutions the IRS expects to see in the 1023 record.

Tasks:

1. Draft a conflict-of-interest policy modeled on the IRS Form 1023 sample.
2. Collect signed acknowledgments from each director.
3. Prepare initial board resolutions: open a bank account, authorize a treasurer, accept the bylaws, accept the COI policy, accept the EIN, designate the principal office, and authorize the 1023 filing.
4. Adopt by board action.

Outputs (in `04_Nonprofit_Registration/F_Policies/`):

- `Conflict_Of_Interest_Policy.md` + signed acknowledgments
- `Initial_Board_Resolutions.md` (signed)

Acceptance: All directors have signed the COI acknowledgment and the resolutions are signed.

### Workstream G: Federal Tax-Exemption Application (IRS)

Objective: Submit the 1023 or 1023-EZ as appropriate and track to determination.

Tasks:

1. Eligibility check for the 1023-EZ using the IRS eligibility worksheet (assets, projected gross receipts, foreign activity, prior status, etc.). Document the result.
2. Prepare the application:
   - 1023-EZ: complete the online form via Pay.gov.
   - Full 1023: draft narrative description of activities, financial data (actual + 3-year projections), compensation table, conflicts disclosures, and required schedules.
3. Pay the application fee from an approved account.
4. Submit and capture the IRS receipt.
5. Track the application weekly until the determination letter is issued. Respond to any IRS information requests within the IRS-stated window.
6. Upon receipt of the determination letter, store it in Drive and notify the product owner in Slack.

Outputs (in `04_Nonprofit_Registration/G_IRS_Exemption/`):

- `1023_Eligibility_Decision.md`
- `1023_Application_Package.pdf`
- `IRS_Receipt.pdf`
- `IRS_Determination_Letter.pdf` (final)

Acceptance: IRS determination letter received, scanned, and stored in Drive.

### Workstream H: State Charitable Registration And Fundraising Compliance

Objective: Register in every state where the organization will actively solicit donations.

Tasks:

1. Identify the states the organization intends to solicit in over the next 12 months (web-based solicitation generally counts in many states).
2. For each state, determine the registering authority (Attorney General or Secretary of State usually), the form, the fee, and any required exhibits (bylaws, IRS determination, financials).
3. Submit registrations. Track renewals and annual reports in a single calendar.
4. Add the required disclosure statements to donation forms and the website where the law requires them.

Outputs (in `04_Nonprofit_Registration/H_State_Charitable/`):

- `State_Solicitation_Plan.md`
- `Registrations/<state>/` per state (filings + receipts)
- `Annual_Renewal_Calendar.md`

Acceptance: All target states are either registered or formally exempt from registration with documentation.

### Workstream I: Banking And Bookkeeping

Objective: Stand up a nonprofit bank account and an entry-level bookkeeping system.

Tasks:

1. Choose a bank with reasonable nonprofit account terms. Open an account in the legal name using the EIN, articles, and bylaws.
2. Set up basic bookkeeping (recommended: a cloud accounting tool with a nonprofit chart of accounts).
3. Set up a donation intake path (recommended: a low-fee donation platform that issues tax receipts).
4. Document who has banking access and what the dual-control rule is for payments above a threshold.

Outputs (in `04_Nonprofit_Registration/I_Banking/`):

- `Bank_Account_Setup_Confirmation.pdf`
- `Bookkeeping_Setup_Notes.md`
- `Donation_Intake_Plan.md`

Acceptance: Account is funded with a token deposit, bookkeeping records show that deposit, and the donation intake path is testable end-to-end.

### Workstream J: Data, Privacy, And NDD-Adjacent Compliance Memo

Objective: A short, practical memo that says what the organization can and cannot do with the data it handles today, and what changes if scope expands.

Tasks:

1. Inventory the data KiNDD already handles (provider directory, regional center mapping, RAG research artifacts) using the docs in `05_Technical_Reference/` and `Clinical_Team_Handoff.md`.
2. Confirm there is no PHI / patient-level data in the current snapshots. If there is, escalate immediately and stop further sharing until resolved.
3. Identify privacy obligations that already apply (state privacy laws, app-store privacy requirements, donor data protections).
4. Flag activities that would change the compliance picture (collecting user accounts, accepting clinical data, ingesting controlled-access research datasets, entering BAAs).
5. Recommend a short list of policies to publish on the website (privacy policy, terms of use, data-use statement, donor privacy statement).
6. Recommend whether to retain counsel with health-tech experience for an audit before any expansion.

Outputs (in `04_Nonprofit_Registration/J_Privacy_Compliance/`):

- `Data_Privacy_Compliance_Memo.md`
- `Public_Policy_Drafts/` (privacy policy, terms, donor privacy statement)

Acceptance: Product owner has reviewed the memo and decided which policies to publish.

### Workstream K: Project Hygiene (Ongoing)

Objective: Keep everyone informed without flooding the Slack channel.

Tasks:

- Weekly status update posted as a reply under Slack post 5, in the format below.
- Maintain a `04_Nonprofit_Registration/STATUS.md` that mirrors the latest weekly update.
- Maintain `04_Nonprofit_Registration/RUNBOOK.md` with: current state of each workstream, who owns each external account (registered agent, IRS Pay.gov login, bank login, accounting tool, donation platform, trademark counsel), and where credentials live (password manager entry name only, never the credential itself).
- Track all costs in `04_Nonprofit_Registration/Budget.md` (estimate vs. actual).

Weekly status update format:

```text
Week of YYYY-MM-DD

Done this week
- ...

In progress
- ...

Blocked / needs decision
- ...

Next week
- ...

Drive: <links to any new outputs>
```

Acceptance: Weekly update posted on time, RUNBOOK and Budget current.

---

## 6. Questions You Must Answer Before Filing

Document the answers in `04_Nonprofit_Registration/A_Discovery/Open_Questions.md` and resolve with the product owner.

- Legal name and any "doing business as" names.
- State of incorporation and reason.
- Principal office address.
- Registered agent (in-house or paid service).
- Initial board members and officers.
- Whether the founder will be compensated and how that interacts with 501(c)(3) public-charity status.
- Initial program activities described in plain language for the 1023.
- Projected revenue and expenses for the first three fiscal years.
- Fiscal year end (calendar or other).
- Geographic scope of operations and fundraising.
- Whether the organization will ever handle PHI, controlled-access research data, or sensitive data about minors. If yes, what changes operationally and legally.
- Whether the iOS app will collect any user account information that changes the privacy posture.
- Insurance needs (general liability, D&O).

---

## 7. Required Source Documents And Owner Inputs

Request these from the product owner during workstream A and store them under `04_Nonprofit_Registration/Inputs/` (read access only):

- Founder identification documents needed for incorporation (state-dependent).
- Bank account opening identification (state-dependent).
- Confirmed legal name and DBA names.
- Confirmed registered office address.
- Names, emails, and signed consents for initial directors and officers.
- Payment method for filing fees and IRS application fee.
- Decision on outside counsel and accounting firm (or "defer").

---

## 8. Suggested Timeline

| Week    | Milestone                                                           |
| ------- | ------------------------------------------------------------------- |
| 1       | Onboarding, workstream A decision memo drafted                      |
| 2       | A decision memo signed, B name checks complete                      |
| 3       | C articles filed, D EIN obtained                                    |
| 4       | E bylaws drafted, board confirmed                                   |
| 5       | E organizational meeting held, F COI policy and resolutions adopted |
| 6-8     | G 1023 (or 1023-EZ) prepared and submitted                          |
| 6-10    | H state charitable registrations submitted in priority states       |
| 7-9     | I banking and bookkeeping live                                      |
| 8-10    | J privacy/compliance memo and draft public policies delivered       |
| Ongoing | K weekly updates, IRS correspondence, renewal calendar              |

This is a working estimate. State processing times can slip the schedule; surface slippage in the weekly update.

---

## 9. Deliverables And Acceptance Criteria Summary

The engagement is complete when:

- Drive folder `04_Nonprofit_Registration/` contains every output listed in section 5.
- The organization has a state-stamped certificate of incorporation, an EIN, adopted bylaws, a seated board, a COI policy, and signed initial resolutions.
- The IRS Form 1023 / 1023-EZ has been submitted (determination is out of operator control, but submission is in scope).
- State charitable registrations have been submitted in every target solicitation state, or formal exemption letters have been collected.
- An operating bank account exists and a bookkeeping system is in use.
- A privacy/compliance memo has been delivered and reviewed by the product owner.
- Slack post 5 contains an "engagement complete" reply with links to the artifacts above.

---

## 10. Reporting And Cadence

- Weekly Slack update on Fridays before 17:00 in the channel under Slack post 5, in the format from workstream K.
- Bi-weekly 30-minute sync with the product owner; agenda in `04_Nonprofit_Registration/SYNC_NOTES.md`.
- Any blocker that prevents progress for more than 3 business days must be escalated to the product owner with a recommendation.
- IRS or state correspondence with deadlines: notify the product owner within 24 hours of receipt with a recommendation and the deadline.

---

## 11. What Is Out Of Scope

- Engineering changes to the KiNDD product (web, iOS, Android, backend, RAG).
- Direct fundraising or donor outreach.
- Tax filings beyond the initial exemption application (the first Form 990 is a separate engagement unless extended).
- Hiring decisions for other roles.
- Legal opinions; those go to retained counsel.

---

## 12. First-Week Checklist For The Operator

- [ ] Confirm Drive folder access, especially `04_Nonprofit_Registration/`.
- [ ] Confirm Slack channel access and post a one-line "I am starting" reply under Slack post 5.
- [ ] Read all documents listed in section 2 of this brief.
- [ ] Schedule the founder interview for workstream A.
- [ ] Stand up `04_Nonprofit_Registration/STATUS.md`, `RUNBOOK.md`, `Budget.md`, and `SYNC_NOTES.md` shells.
- [ ] Post the first weekly status update on Friday in the format from workstream K.
