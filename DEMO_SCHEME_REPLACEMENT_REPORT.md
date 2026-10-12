# DEMO SCHEME REPLACEMENT REPORT

Date: **2026-10-13, Asia/Kolkata**. **Checkpoint: VERIFIED.**

All eight fictional PDF schemes are published in the development application. The original eight templates are hidden from its citizen catalog without changing or deleting their records. The other published browser-test scheme remains visible and its converted draft remains unpublished.

## Specification reviewed

Read all **10 pages** of `C:/Users/ABHISHEK/Downloads/Eight_Demo_Schemes_Full_Specification.pdf` before application changes. SHA-256: `a6b07702b6dbe35a05f733c590033313e1a87ad1067b917df9e055ac73fec06b`. Complete page-content text was extracted using existing Python standard-library PDF stream decoding; native PDF rendering dependencies were unavailable. The PDF's fictional data was used as the specification under the user's request. No real-policy research was performed.

## Exact files changed

| File | Change |
| --- | --- |
| `backend/src/main/resources/demo/eight-schemes.json` | Added the eight exact descriptions, categories, benefits, document lists, questionnaire definitions, input constraints, AND conditions and PDF page mappings. |
| `backend/src/main/java/in/project/schemes/DemoSchemes.java` | Added development/test fixture installation through existing category/question/scheme/rule/publication methods, locked duplicate prevention, precise legacy-template recognition and scoped demo numeric-input handling. |
| `backend/src/main/java/in/project/schemes/Bootstrap.java` | Runs the replacement fixture installer during development startup, including an already-populated database. Original bootstrap records remain available for historical references. |
| `backend/src/main/java/in/project/schemes/Api.java` | Excludes identified original templates from catalog results; decodes answer JSON through the existing exact parser and validates/canonicalizes new demo numeric answers. Existing HTTP paths, request fields and response structures retained. |
| `backend/src/test/java/in/project/schemes/DemoSchemesIntegrationTest.java` | Added 11 backend integration tests covering all eight schemes, input constraints, exact decimal submission/storage/retrieval, history, preservation, repeated seeding and production guards. |
| `frontend/e2e/demo-schemes.spec.ts` | Added eight browser journeys covering PDF scheme details, questionnaire input, boundaries, positive/negative/missing results, explanations and history. |
| `frontend/e2e/workflows.spec.ts` | Updated the existing citizen regression to select Student Education Support and answer its four PDF questions instead of the retired Learning Support template. |
| `DEMO_SCHEME_REPLACEMENT_REPORT.md` | Added this report. |
| `IMPLEMENTATION_REPORT.md` | Appended this checkpoint's actual results and limitations. |

Private environment files, dependencies, schema/Flyway migrations, authentication/security files, RuleEngine, application UI/styling/navigation, existing focused frontend tests and real-scheme research reports were not changed. Generated build/test files and ignored `.tools` verification artifacts were refreshed.

## Eight published schemes and rule mappings

Every displayed scheme name begins **FICTIONAL / DEMO ONLY:**. Every benefit explicitly states that it is an invented demonstration amount, with no payout or entitlement. Descriptions and guidance identify the schemes as fictional. `synthetic=true`, `reviewerApproved=false`, no government source/verification claim. All conditions in each row are combined using the existing **AND** group.

| Scheme / application ID / PDF page | Category | Fictional benefit | Deterministic conditions |
| --- | --- | --- | --- |
| Student Education Support / 11 / 2 | Education | Rs 12,000/year | Age BETWEEN [18,25]; state EQ TN; college enrollment EQ true; annual family income LTE 300000. |
| Women Higher Education Grant / 12 / 3 | Education | Rs 15,000/year | Gender EQ female; age BETWEEN [18,30]; higher-education enrollment EQ true; annual family income LTE 400000. |
| Farmer Family Assistance / 13 / 4 | Agriculture | Rs 8,000/year | Occupation EQ farmer; age GTE 21; state EQ TN; annual family income LTE 250000. |
| Senior Citizen Welfare Support / 14 / 5 | Social Welfare | Rs 1,500/month | Age GTE 60; state EQ TN; annual family income LTE 200000. |
| Youth Skill Development Grant / 15 / 6 | Skill Development | Rs 10,000 one-time | Age BETWEEN [18,35]; unemployed EQ true; skill-training enrollment EQ true. |
| Disability Education Assistance / 16 / 7 | Education | Rs 20,000/year | Certified disability percentage GTE 40; age BETWEEN [16,30]; currently studying EQ true. |
| Small Business Startup Support / 17 / 8 | Entrepreneurship | Rs 25,000 one-time | Age BETWEEN [21,45]; first-time entrepreneur EQ true; annual family income LTE 500000. |
| School Excellence Scholarship / 18 / 9 | Education | Rs 6,000/year | Current class IN [10,11,12]; previous exam percentage GTE 75; state EQ TN; annual family income LTE 300000. |

Published rule IDs are **10–17**, respectively. Every leaf maps to its PDF page and criterion in `sourceMapping`. Thresholds/range values use supported exact decimal strings. Residence, family income and age are explicit questionnaire answers; personal profile income/DOB/occupation are not substituted for the PDF's requested answers. No extra residence/income condition was added to schemes that do not specify it.

All true yields ELIGIBLE; a definitive false yields NOT_ELIGIBLE; decisive missing information yields the existing `422 INPUT_REQUIRED`, with no evaluation-history insert. Existing FALSE dominance in AND and all other engine semantics remain intact. Each condition contributes its existing operator/expected/actual/result explanation.

## Questions and answer types

There are **14 shared demo question definitions**, linked to the appropriate schemes in the manifest. All labels start `Demo:`. Existing shared-answer behavior is retained and displayed by the existing citizen form.

| Key / question | Existing answer type / constraint |
| --- | --- |
| age / Age (integer years) | NUMBER; server rejects fractional years. |
| state / State of residence | SELECT; existing Indian state/UT codes, including TN; blank is unknown. |
| income / Annual family income (decimal INR) | NUMBER; exact decimal input, not substituted with profile income. |
| college / Currently enrolled in college | BOOLEAN; Yes/No/Unknown. |
| gender / Gender | SELECT: female, male, other; blank unknown. |
| higher / Higher education enrollment | BOOLEAN; Yes/No/Unknown. |
| occupation / Occupation | SELECT: farmer, other; blank unknown. |
| unemployed / Currently unemployed | BOOLEAN; Yes/No/Unknown. |
| training / Skill training enrollment | BOOLEAN; Yes/No/Unknown. |
| disability / Certified disability percentage (0-100) | NUMBER; inclusive input range 0–100. |
| studying / Currently studying | BOOLEAN; Yes/No/Unknown. |
| first / First-time entrepreneur | BOOLEAN; Yes/No/Unknown. |
| class / Current class | SELECT: 10, 11, 12, other; blank unknown. |
| marks / Previous exam percentage (0-100) | NUMBER; inclusive input range 0–100. |

The existing generic NUMBER control remains. Server validation is authoritative for integer ages and percentage limits. No new question type, schema column or engine operator was introduced. Empty/null inputs remain missing. Demo numeric JSON tokens are decoded exactly and stored as the already-supported decimal strings, preventing MySQL JSON-number rounding. Excessive exponent expansion uses the existing exact-decimal length guard; no unbounded plain-string expansion is performed. Normal unrelated questions retain their existing validation and storage handling.

## Display-only document guidance

| Scheme | Required document text |
| --- | --- |
| Student | Age proof; college bonafide certificate; income certificate; residence proof. |
| Women | Identity proof; college enrollment certificate; income certificate. |
| Farmer | Identity proof; residence proof; income certificate; farming occupation declaration. |
| Senior | Age proof; residence proof; income certificate. |
| Youth | Age proof; unemployment self-declaration; skill-training enrollment proof. |
| Disability | Disability certificate; age proof; school or college bonafide certificate. |
| Business | Age proof; income certificate; first-time entrepreneur declaration; business proposal. |
| School | School bonafide certificate; previous examination marksheet; residence proof; income certificate. |

These lists appear in existing application/document guidance with an explicit instruction not to submit documents, Aadhaar numbers or bank details. No upload, application, payout or official verification feature was added.

## Inspection, retirement and historical preservation

Inspected all ten original application records, including complete metadata, linked questions, every rule version, evaluation/suggestion counts and scheme/rule audit actions before changes:

| Original ID | Identity / purpose | Final treatment |
| --- | --- | --- |
| 1–8 | Learning Support, Training Access, Senior Wellbeing, Rural Enterprise, Student Equipment, Employment Readiness, Household Support, Skills Transition; exact Bootstrap synthetic fixtures | Hidden only from citizen catalog results. |
| 9 | `Synthetic Browser Demo 1791821533618`; published browser-validation fixture; one existing evaluation | Row/rule/evaluation unchanged; still visible. |
| 10 | `Synthetic Converted Browser Draft 1791821534214`; browser suggestion-conversion draft | Unchanged and unpublished. |

Catalog hiding requires the exact original name set, synthetic flag, original description/benefits/guidance and absent source URL. A broad synthetic-name prefix or all-synthetic filter was not used. No retirement schema field, destructive deletion, pointer clearing, published-rule mutation or metadata rewrite was needed. Original direct detail routes remain available for historical references; the requested retirement concerns catalog visibility. Admin inventory still includes retained historical schemes.

Read-only SHA-256 comparisons before and after installation/repeated startup match for all ten original scheme rows, their nine rule rows, original shared question, eight links, both existing evaluation records/snapshots, eighteen pre-existing audit rows and the existing suggestion record. New fixture/audit rows were appended. Unrelated records 9 and 10 are included in the unchanged ten-row comparison.

A Windows text-stream encoding error initially produced `0?100` in two newly-created demo labels. Those two labels were corrected to ASCII `0-100` within this patch, with revision increments and `DEMO_LABEL_ENCODING_CORRECTED` audit events in the development/test data. The repair checked their NUMBER type and exclusive links to the new marked fixtures. No original question, answer, eligibility threshold, published rule revision or evaluation snapshot was changed. The published-question edit API remains protected; its guard was not relaxed.

## Duplicate prevention and publication

Development Bootstrap installs the manifest even when existing schemes are present. Installation reuses existing categories/questions and calls the existing scheme, rule validation and publication methods. The enclosing transaction makes fixture installation atomic. Locking the existing active administrator row serializes simultaneous installers. Each stable prefixed scheme name must resolve to exactly one published synthetic fixture with the expected specification marker; a conflicting/incomplete record causes failure rather than duplication or silent overwrite.

Repeated seeding tests preserve scheme/question counts and all pre-call audit/history rows. Actual development backend restarts retain scheme IDs 11–18 and rule IDs 10–17, with exactly eight replacement fixtures, **18 total retained scheme rows** and **16 total questions**. The citizen catalog contains **9 entries**: eight PDF schemes plus the preserved published browser fixture. The eight original templates and the preserved draft are absent from catalog results.

Production Bootstrap does not run the installer. Existing production catalog/detail suppression and synthetic publication guards remain. A new production-profile guard test confirms the installer is a no-op and synthetic fixtures cannot be retrieved or listed. These fictional schemes do not satisfy or replace the five-real-scheme roadmap.

## Tests executed and actual results

Final checks were executed on 2026-10-13 without dependency installation.

| Check | Actual final result |
| --- | --- |
| Cached offline Maven `package`, isolated TEST_DB_URL, RUN_DB_TESTS=true, USE_TESTCONTAINERS=false, JaCoCo append=false | Exit 0; BUILD SUCCESS; **85 passed: 39 existing API integration + 35 engine + 11 new demo integration tests; zero failures/errors/skips.** Current JAR packaged. |
| Existing frontend Vitest suite | Exit 0; **14 passed across 5 files**. |
| TypeScript `tsc --noEmit --incremental false` | Exit 0. |
| Vite production build | Exit 0. |
| Playwright against current JAR on isolated test DB (8081), temporary Vite proxy (5174) | Exit 0; **16 passed**, zero skipped/flaky/unexpected: eight PDF journeys, existing citizen/admin workflows and six viewport/theme keyboard cases. |
| Authenticated development API inspection (8080) | All eight details/questionnaires/publication IDs match the manifest; original eight hidden; ID 9 visible; ID 10 remains draft; 18 admin rows / 9 catalog rows. |
| Original-data hash checks and repeated development startup | All seven captured original-data comparisons match; eight replacement schemes retain IDs with no duplicate schemes/questions. |

Each new backend scheme case verifies catalog visibility, descriptions/benefits/document text, question types/options, PDF mappings, positive outcomes, each independently false condition, each independently missing answer, inclusive range endpoints and full explanations/history. School tests cover classes 10/11/12; percentages cover 0, threshold and 100. Income tests include a numeric JSON value only `0.00000000000000001` above each limit and confirm exact retrieval and a negative verdict. Invalid fractional ages, out-of-range percentages and excessive numeric exponent expansion reject without partial answer/profile changes. UNKNOWN attempts do not add history.

The eight browser journeys each save the complete questionnaire, obtain positive and negative explanations, exercise a missing answer and confirm exactly two history entries. Existing admin publication/review/audit and citizen profile/suggestion/logout workflows pass. Browser regressions write only to the isolated test database; application API inspection uses existing admin credentials privately and does not enter identity/bank/document data.

Intermediate failures were resolved: new-test table/source-mapping handling, a Spring proxy-field test setup, browser navigation selectors and verdict display text, percentage-label encoding, and exact numeric answer storage. Maven initially reported a cached-JAR compiler-resource-close error; retry succeeded without changing dependencies/configuration. No failed/unexecuted run is counted as passed. Harmless JVM class-sharing and Node color-environment warnings remain.

Evidence is in ignored `.tools/demo-backend.log`, `demo-frontend.log`, `demo-typecheck.log`, `demo-build.log`, `demo-browser-final.log`, `demo-browser-results.json`, `demo-live-verification.json`, `demo-app-before.json` and `demo-app-final-preservation.json`, plus backend Surefire and frontend browser output. No new coverage percentage claim is made.

## Remaining limits and checkpoint

No unresolved blocker. Docker/Testcontainers, production deployment/TLS and a full screen-reader audit were not executed. The PDF was read completely as text; native visual rendering was unavailable. Browser tests use real MySQL through the isolated existing test schema. The development backend now runs the packaged current source, preserving Patches 1–3 rather than the previously stale binary.

Authentication/JWT/CSRF/RBAC, schema, nine eligibility operators, TRUE/FALSE/UNKNOWN behavior, UI design, AI limits, real-scheme research and unrelated schemes are preserved. No automatic matching, AI assistant or real-government scheme was added.

**DEMO SCHEME REPLACEMENT CHECKPOINT: VERIFIED.** Stopped after this patch. Awaiting explicit user approval before further development.
