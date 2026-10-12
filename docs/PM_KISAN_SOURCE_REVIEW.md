# PM-KISAN source review for Patch 4

Review date: **2026-10-13, Asia/Kolkata**. Status: **publication blocked**.

This is a research and implementation-review artifact. It is not an approved scheme definition, an executable eligibility rule, a government certification or a citizen-facing questionnaire. No PM-KISAN category, question, scheme, rule or publication was created by this patch. The symbolic criterion names below are proposed mapping identifiers, not database IDs or published rule leaves.

## Official evidence and retrieval limits

Only Government of India sources were used to support this review. Verification date means the date the material was inspected, not its publication date or proof that no subsequent amendment exists.

| Reference | Official URL and section | Document date / access evidence | Inspected |
| --- | --- | --- | --- |
| PORTAL | [PM-KISAN](https://pmkisan.gov.in/) - scheme summary, exclusions, Important Note, Farmers Corner | Direct retrieval failed repeatedly. Government-hosted content was available through the search index, including the verification-hold notice. This was not a successful live-page fetch. | 2026-10-13 |
| OG | [Revised operational guidelines on the official fw subdomain](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf) | Full 12-page PDF retrieved; cover says revised 29.03.2020. The [user-specified main-host URL](https://pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf) returned gateway errors. Byte identity between hosts was not checked. The official [fw navigation](https://fw.pmkisan.gov.in/) links the main-host path. | 2026-10-13 |
| EXPANSION | [Ministry PIB release](https://www.pib.gov.in/PressReleasePage.aspx?PRID=1604017&lang=2&reg=48) - expansion paragraph | 22.02.2020; inspected indexed government text. Supersedes the original two-hectare restriction. | 2026-10-13 |
| CUTOFF | [Lok Sabha answer 343](https://sansad.in/getFile/loksabhaquestions/annex/184/AU343_dkn7oT.pdf?source=pqals) - answer (a)-(d), PDF pages 1-2 | 04.02.2025; full PDF retrieved. Addresses land cutoff, succession, verification and applications. | 2026-10-13 |
| REGIONAL | [Lok Sabha answer 205](https://sansad.in/getFile/loksabhaquestions/annex/183/AS205_jgP2NW.pdf?source=pqals) - statement (c)-(d), PDF pages 2-3 | 10.12.2024; full PDF retrieved. Covers land-seeding relaxation and alternative land documents. | 2026-10-13 |
| MEGHALAYA | [Ministry PIB decision](https://www.pib.gov.in/newsite/PrintRelease.aspx?lang=2&reg=48&relid=195087) - procedures (1)-(2) | 27.11.2019; inspected indexed government text; direct fetch failed. Includes regional clan/community certification. | 2026-10-13 |
| FARMER-ID | [Ministry PIB parliamentary reply](https://www.pib.gov.in/newsite/erelcontent.aspx?lang=2&reg=48&relid=285538) - Farmer ID paragraph | 10.03.2026; full page retrieved. New-registration requirement is conditional on registry rollout. | 2026-10-13 |
| REGISTRY | [Ministry PIB registry update](https://www.pib.gov.in/PressReleasePage.aspx?PRID=2244626&lang=1&reg=3) - annexure | 24.03.2026; full page retrieved. Lists generated IDs by state; not a current PM-KISAN mandate schedule. | 2026-10-13 |
| CONTINUATION | [Ministry PIB Cabinet release](https://www.pib.gov.in/newsite/erelcontent.aspx?lang=2&reg=48&relid=292444) - continuation and scheme-summary paragraphs | 31.07.2026; full page retrieved. Confirms continuation for 2026-27 through 2030-31 and the annual benefit. | 2026-10-13 |
| REGISTER | [Official new-farmer registration](https://www.pmkisan.gov.in/RegistrationFormupdated.aspx) - entry form and authentication guidance | Inspected indexed government text. No identity details entered and no registration attempted. | 2026-10-13 |

The 21.06.2019 operational PDF was also inspected but is older than OG. The original FAQ still contains the former land-size restriction and was not used to author current criteria. The linked revised FAQ and letters/circulars pages could not be retrieved, including attempts through the official fw host. Native HTTP retrieval also failed. No third-party summaries, individual beneficiary certificates or unofficial calculators were used as policy evidence.

## Source-to-criterion mapping

Each source reference above contains its full official URL. Section references below identify the relevant provision. All rows were reviewed on **2026-10-13**. "Representable" means the existing types/operators could express that clause after accurate questions are authored; it does not mean a complete PM-KISAN rule was implemented or verified.

| Proposed identifier | Verified clause or guidance | Official source / section | Representation and unresolved issue |
| --- | --- | --- | --- |
| benefit | Rs 6,000 annually in three equal Rs 2,000 instalments, through DBT. | [CONTINUATION](https://www.pib.gov.in/newsite/erelcontent.aspx?lang=2&reg=48&relid=292444), scheme summary; [CUTOFF](https://sansad.in/getFile/loksabhaquestions/annex/184/AU343_dkn7oT.pdf?source=pqals), answer opening | Scheme metadata; no payment calculation or entitlement promise. |
| family | Husband, wife and minor children comprise the relevant family. | [OG](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf), section 3 | Family-wide questions required. A citizen's individual occupation/profile cannot substitute for all family members. |
| cultivable-land | Cultivable landholding is required; a general tenant/landless route is not established. | [CUTOFF](https://sansad.in/getFile/loksabhaquestions/annex/184/AU343_dkn7oT.pdf?source=pqals), land paragraph; [REGIONAL](https://sansad.in/getFile/loksabhaquestions/annex/183/AS205_jgP2NW.pdf?source=pqals), statement (c)-(d) | Ordinary and approved alternative proof must be separate branches. See blocker B1. |
| land-size | The previous two-hectare cap was removed. | [EXPANSION](https://www.pib.gov.in/PressReleasePage.aspx?PRID=1604017&lang=2&reg=48), expansion paragraph | Do not add a maximum-area threshold or reuse the outdated FAQ. |
| ordinary-cutoff | Ordinary ownership before or on 01.02.2019; death-based succession excepted. | [CUTOFF](https://sansad.in/getFile/loksabhaquestions/annex/184/AU343_dkn7oT.pdf?source=pqals), land paragraph | DATE LTE and a succession branch are supported. A mutation/certificate issue date must not be assumed to be the date rights accrued. |
| land-transfer | Post-cutoff ordinary sale/gift/partition transferees are excluded; transferors require retained land. | [OG](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf), sections 5.3.3-5.3.4 | Distinguish acquisition basis and retained ownership. Do not treat every transfer as death-based succession. |
| institutional | Institutional landholders are excluded. | [PORTAL](https://pmkisan.gov.in/), Scheme Exclusion item 1 | Family/holding BOOLEAN EQ false; unknown stays unset. |
| constitutional | Any family member holding a constitutional post, currently or formerly, excludes the family. | [PORTAL](https://pmkisan.gov.in/), Scheme Exclusion | Family-wide BOOLEAN; "currently" alone is insufficient. |
| elected-office | Current/former ministers, MPs, state legislators, municipal mayors and district-panchayat chairs are excluded. | [PORTAL](https://pmkisan.gov.in/), Scheme Exclusion | Question must enumerate the offices, rather than exclude every elected village representative. |
| government-employment | Serving/retired government, PSE, attached/autonomous-office and regular local-body employees are excluded; MTS/Class IV/Group D excepted. | [PORTAL](https://pmkisan.gov.in/), Scheme Exclusion | Aggregate only disqualifying members. One exempt member must not override another disqualifying member. |
| pension | Monthly pension of at least Rs 10,000 excludes the covered pensioner; MTS/Class IV/Group D excepted. | [PORTAL](https://pmkisan.gov.in/), Scheme Exclusion | Member-specific pension/exemption or an exact aggregate question. Do not turn this into an annual-income cap. |
| income-tax | A family member paying income tax in the last assessment year excludes the family. | [PORTAL](https://pmkisan.gov.in/), Scheme Exclusion | Family-wide BOOLEAN; distinguish tax paid from merely filing a return. |
| professional | Registered, practising doctors, engineers, lawyers, chartered accountants and architects are excluded. | [PORTAL](https://pmkisan.gov.in/), Scheme Exclusion | Capture both registration and practice, rather than qualifications alone. |
| nri-new | NRI farmer families under the Income Tax Act are excluded for newly uploaded beneficiaries. | [OG](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf), section 4.1(c) | New/existing status and authoritative tax-residency meaning matter; do not substitute citizenship or physical absence. No unrestricted interpretation for existing beneficiaries adopted. |
| manipur-proof | Tribal cultivation certificate requires village authority issuance and sub-divisional authentication. | [OG](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf), section 5.5.1 | State/land-basis branch plus confirmation flags. Collect no certificate. Temporal interaction remains B1. |
| nagaland-permanent | Community land under permanent cultivation requires verified village certification and district countersignature. | [OG](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf), section 5.5.2(1) | Separate from Jhum. No generic "North-East exemption" flag. See B1. |
| nagaland-jhum | Certification applies; census inclusion has succession/family-partition exceptions. | [OG](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf), section 5.5.2(2) | Census proviso exception must not automatically become a universal ownership-cutoff waiver. See B1. |
| jharkhand-proof | Approved lineage, verified possession and district countersignature provide the alternative proof. | [OG](https://fw.pmkisan.gov.in/Documents/RevisedPM-KISANOperationalGuidelines%28English%29.pdf), section 5.6 | Separate approval flags without lineage uploads. See B1. |
| meghalaya-proof | Clan/community documents follow regional validation and District Agriculture Officer countersignature. | [MEGHALAYA](https://www.pib.gov.in/newsite/PrintRelease.aspx?lang=2&reg=48&relid=195087), procedures (1)-(2) | Khasi/Jaintia and Garo procedures differ; clan paths also require clan-head list/NOC. Full current temporal mapping not closed. |
| fra-proof | FRA patta holders are included subject to other eligibility conditions. | [REGIONAL](https://sansad.in/getFile/loksabhaquestions/annex/183/AS205_jgP2NW.pdf?source=pqals), statement (c)-(d)(v) | Do not exclude valid FRA holdings for lack of an ordinary title, or automatically waive the cutoff. See B1. |
| land-seeding | Land-seeding condition is relaxed in North-Eastern states except Assam. | [REGIONAL](https://sansad.in/getFile/loksabhaquestions/annex/183/AS205_jgP2NW.pdf?source=pqals), statement (c)-(d)(i) | Relaxation of seeding is not exemption from all landholding/exclusion conditions. |
| ekyc-bank-verification | e-KYC, land seeding and Aadhaar-bank seeding are verification/payment requirements, subject to specific relaxation above. | [CUTOFF](https://sansad.in/getFile/loksabhaquestions/annex/184/AU343_dkn7oT.pdf?source=pqals), page 2 verification paragraph | Status guidance only until the screening/readiness distinction is reviewed. No Aadhaar, bank fields or identity uploads. |
| farmer-id | Farmer ID required for new registrations in registry-rollout states; other states may register without it. | [FARMER-ID](https://www.pib.gov.in/newsite/erelcontent.aspx?lang=2&reg=48&relid=285538), Farmer ID paragraph | Conditional workflow, not a universal exclusion. Current state applicability unresolved: B2. |
| duplicate-hold | Suspected post-cutoff/duplicate-family cases may have benefits withheld pending physical verification. | [PORTAL](https://pmkisan.gov.in/), Important Note | Pending is not a permanent ineligibility finding. Use official Know Your Status; see limitation B3. |
| official-application | Self-registration is ongoing; State/UT verification precedes approval and processing. | [CUTOFF](https://sansad.in/getFile/loksabhaquestions/annex/184/AU343_dkn7oT.pdf?source=pqals), page 2 registration paragraph; [REGISTER](https://www.pmkisan.gov.in/RegistrationFormupdated.aspx) | Direct applicants to the official portal; CSC/Block/Tehsil assistance is described in FARMER-ID. No submission by this project. |

## Unresolved publication prerequisites

### B1 - Temporal mapping for alternative landholding paths

The ordinary cutoff and death-based succession exception are corroborated after the original guidelines' five-year wording. The verified documents establish regional/FRA proof alternatives, but the review did not establish a complete, current decision table tying the date of rights, record entry, certification and succession to every alternative path. In particular, the Nagaland census exception is specifically a census exception; it cannot be assumed to waive another provision. An individualized beneficiary certificate is not an authoritative general rule.

This is a gap in the evidence and proposed mapping, not a claim that the legislation necessarily contradicts itself or that these applicants are ineligible. Applying the ordinary title rule universally risks rejecting valid applicants; accepting any alternative certificate without temporal criteria risks omitting a mandatory condition. A generic "all government requirements met" question would hide the gap rather than implement it.

Needed: authoritative current instructions clarifying the temporal treatment of the approved alternative paths, including FRA recognition and the Nagaland provisos. No narrower geographic or applicant cohort has been substituted without approval.

### B2 - Current Farmer ID applicability

The March 2026 reply establishes a conditional new-registration requirement. The March state table records generated IDs, not the operative PM-KISAN mandate for each state on 2026-10-13. The linked circulars and revised FAQ could not be inspected, and no complete current applicability schedule was verified. The review therefore does not infer mandatory status from a state's registry count or freeze a March rollout list as current policy.

Needed: an official current applicability source and a reviewed distinction between substantive preliminary eligibility and registration/payment readiness. This is not a proposed new operator, identity collection feature or government integration.

### B3 - Official verification remains external

The existing engine implements TRUE/FALSE/UNKNOWN and records definitive screening verdicts. It cannot establish official land, identity, tax, duplicate-family or payment status. Known pending verification must not be mislabeled as NOT_ELIGIBLE; absence of payment-readiness evidence must not be presented as official approval. The published-rule gate checks source text and approval flags but cannot adjudicate these policy questions.

The preliminary-screening versus readiness treatment must be settled in the scheme definition and guidance before publication. No extra verdict, mandatory artificial unanswered question or government-verification claim was introduced. No finding is made that the existing nine operators need modification.

## Existing infrastructure inspected

- `backend/src/main/java/in/project/schemes/Api.java`: draft metadata and `linkQuestions()`, typed question creation, exact rule drafts, source mappings, locked atomic publication, scheme questions/answers and evaluation.
- `backend/src/main/java/in/project/schemes/RuleEngine.java`: nine operators, maximum three GROUP levels, exact decimal normalization, complete explanations and three-valued logic.
- `frontend/src/main.tsx`: admin metadata/questions/rules, citizen typed inputs with Unknown options, questionnaire saving, evaluation errors and official-source/guidance rendering.
- `docs/IMPLEMENTED_CONTRACTS.md`: `sourceMapping` maps leaf IDs to source-section strings; immutable published metadata/questions/rules; no new schema is needed for ordinary fact questions.

The verified core exclusions are expressible using existing BOOLEAN/SELECT/DATE/NUMBER questions and AND/OR trees. A final tree, depth audit, concrete question wording and one-to-one leaf mapping have not been completed because the publication prerequisites remain open. Engine capability alone does not verify a national policy mapping.

If a missing answer is decision-critical, the current workflow returns HTTP 422 `INPUT_REQUIRED` and saves no evaluation. Existing FALSE-dominates-AND and TRUE-dominates-OR behavior must remain: not every omitted noncritical answer forces 422.

## Required pilot verification after prerequisites are resolved

The following are **unexecuted PM-KISAN test cases**, not passing results:

1. Metadata uses genuine PM-KISAN information, national scope, official HTTPS sources, inspected dates and a source for every actual rule leaf. No reviewer-approved flag until the complete mapping is reviewed.
2. Positive ordinary case and verified death-succession case; land size above two hectares alone does not fail; unrelated annual income alone does not introduce a fabricated cap.
3. Negative case for each family-wide exclusion, including a disqualifying second family member; MTS/Class IV/Group D exceptions do not exempt the family from other exclusions.
4. Pension boundary below/at Rs 10,000 using exact decimals, registered-but-not-practising professional, and applicable NRI new-beneficiary handling.
5. Separate regional, lineage, FRA, Jhum census and succession/partition exception cases once their complete mappings are established.
6. Unknown in each decision-critical branch returns 422 `INPUT_REQUIRED`, identifies the actual missing question and creates no history; known exclusions retain existing logical dominance.
7. Citizen questionnaire saves/reloads typed answers without defaulting missing booleans to false and without Aadhaar, bank details or document uploads.
8. Explanations contain every condition; snapshots retain exact rule/fact values; ordinary revision conflicts and immutable published versions remain protected.
9. Incomplete source mappings, unapproved metadata and inactive linked questions reject publication atomically; approved complete mapping publishes through the existing admin endpoint only.
10. Conditional Farmer ID and pending verification are accurately distinguished from substantive exclusions; guidance links to official application/status workflows.

No rule, seed/import script or publishable partial payload accompanies this review. Reusing only the ordinary exclusion checklist would not satisfy the approved complete-pilot scope.
