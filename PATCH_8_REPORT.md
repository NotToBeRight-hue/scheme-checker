# PATCH 8 REPORT

Date: **2026-10-13, Asia/Kolkata**. Baseline: Checkpoint 7 verified.

**CHECKPOINT 8: PATCH 8 — VERIFIED.** Automatic matching for the eight published PDF fictional demo schemes is implemented, integrated into the existing citizen interface and running from the current packaged backend. Matching previews create no eligibility-history records. No AI functionality or other feature was started.

## 1. Exact files changed

| File | Change |
| --- | --- |
| `backend/src/main/java/in/project/schemes/Api.java` | Added shared-question retrieval, atomic shared-answer saving and read-only matching preview endpoints. Extracted existing answer persistence and fact assembly into shared helpers while retaining individual-check behavior. Added strict eight-scheme selection, revision consistency, explanation labels and production guards. |
| `frontend/src/main.tsx` | Added matching navigation and citizen screen using existing question controls, form/card styles, API client and disclaimers. Added save-and-match, preview-saved, reload, classification/explanation/missing-question cards and individual-workflow links. Existing styles/navigation behavior retained. |
| `backend/src/test/java/in/project/schemes/MatchingIntegrationTest.java` | Added seven API/MySQL integration tests for eight-result selection, shared-answer reuse, unknown/false/OR classification, current rule revisions, exact decimals, validation/transaction rollback, history preservation, security and production guards. |
| `frontend/src/matching.test.tsx` | Added three focused UI tests for prefill/read-only preview, one shared save with returned revision, result clearing and conflict handling without retry. Uses the existing installed test dependencies. |
| `frontend/e2e/matching.spec.ts` | Added a complete real-browser citizen matching journey, mobile/desktop screenshots, stored-answer reuse and history/individual-check assertions. |
| `PATCH_8_REPORT.md` | Added this report. |
| `IMPLEMENTATION_REPORT.md` | Appended actual results and checkpoint status. |

No dependency/configuration, schema/migration, security/authentication, eligibility-engine, demo specification/seed, existing test or real-scheme research file changed. The source hash comparison confirms only the seven paths above changed. Generated build/test output and ignored temporary verification scripts/logs were refreshed.

## 2. Matching API and data flow

All endpoints are under `/api/v1` and use the existing authenticated session and error structures.

| Endpoint | Contract and behavior |
| --- | --- |
| `GET /matching/questions` | Returns `{revision, questions, disclaimer}`. Questions are the unique active union of the eight PDF schemes' links, including existing ID, label, type, options, question revision and this authenticated user's saved value. Exactly 14 questions are required. |
| `PUT /matching/answers` | Accepts `{expectedProfileRevision, answers:[{questionId,value}]}` through the existing exact JSON parser. Saves the full shared-question snapshot atomically and advances the profile revision once. Returns `{revision}`. Null or omitted demo answers are cleared; answers outside the union are rejected and unrelated stored answers are retained. |
| `GET /matching/preview?expectedProfileRevision=<revision>` | Evaluates saved facts without saving answers or eligibility history. Returns `{profileRevision, results, disclaimer}` with exactly one result per PDF scheme. A stale revision returns the existing `409 STALE_REVISION`. |

Each result contains `schemeId`, `schemeName`, `benefits`, `synthetic`, `ruleVersionId`, `classification`, `explanations` and `missingQuestions`. Missing rows identify `fieldKey` and label. Explanation rows retain the engine's node/field/operator/expected/actual/result data and add `questionLabel` for the new matching interface only.

Selection uses the existing manifest's exact eight prefixed names, a unique corresponding database record, synthetic flag, specification marker and an actual published rule pointer/status. It is not a query for every synthetic or every visible catalog entry. The ninth browser-test fixture, old eight templates, draft and unrelated records cannot enter matching. If a required fixture is missing/conflicting/inactive or the 14-question union is unavailable, the request fails with `409 MATCHING_UNAVAILABLE`; it does not silently omit a scheme or invent a result.

The profile is locked before reading answers. Required scheme records are read-locked in consistent manifest order while their current published rule revisions are evaluated. Answer writes take the existing exclusive profile lock. Thus a preview uses one coherent profile revision and stable published versions. No cache, new engine, database table, bulk history insert or automatic retry was introduced.

## 3. Shared-question handling

The existing 14 definitions and IDs are reused: age; state; annual family income; college enrollment; gender; higher-education enrollment; occupation; unemployment; skill-training enrollment; certified disability percentage; currently studying; first-time entrepreneur; class; exam percentage.

The form loads existing shared answers, retains explicit unknown choices and sends decimal input strings unchanged. Save-and-match performs one shared PUT followed by one preview GET using the returned revision. Preview-saved performs a GET only. Unsaved edits clear earlier results and disable preview-saved until saved. Controls are disabled during requests to prevent edits being mistaken for the answers evaluated by an in-flight preview. Reload is explicit after conflicts; failed saves are not retried.

No personal profile income, DOB/age, state or occupation is silently copied into these questionnaire answers. A test fills the profile but leaves the questionnaire blank and obtains eight NEEDS_INFORMATION results. Existing fact assembly remains available for authoritative current rules, exactly as it is in individual evaluation; it does not populate missing question facts from profile fields.

Both save workflows call the existing shared validation/persistence helper. Integer age and inclusive 0–100 percentage validation, exact decimal-string storage, maximum lengths, supported types/options, active/linked checks and duplicate-answer rejection remain enforced. Invalid batches roll back answer changes and revision advancement. Individual answer-saving omission semantics are retained; full matching saves clear only omitted answers in the matching union.

All scheme/category/question definitions and published versions are unchanged. Shared answers naturally appear in existing individual questionnaires and vice versa.

## 4. Classification and interface

Every preview invokes the existing `RuleEngine.evaluate` against the current published rule JSON and supported fact types. Its truth maps directly:

| Engine result | Matching classification |
| --- | --- |
| TRUE | ELIGIBLE |
| FALSE | NOT_ELIGIBLE |
| UNKNOWN | NEEDS_INFORMATION |

For the current AND rules, a definite false dominates missing information. With no definite false and a missing decisive fact, the result is NEEDS_INFORMATION. Missing is never converted to zero, false or a positive answer. Existing AND/OR behavior is preserved: a rollback-only integration test temporarily publishes an OR rule in the isolated test database, verifies TRUE/UNKNOWN/FALSE behavior and the returned current rule ID, then rolls back every test change. No live published rule was modified.

The citizen screen displays each scheme's fictional name/benefit, classification, definite failing conditions or missing questions, every leaf explanation, published rule version and a link to existing details/checks. FICTIONAL / DEMO ONLY notices, existing fictional badges and the existing preliminary-screening disclaimer remain visible. It uses existing cards, typography, colors and rectangular buttons. No new styling, animation, icons, fabricated metrics or government approval claims were added.

The individual API contract is preserved: a decisive unknown individual evaluation still returns `422 INPUT_REQUIRED` without history; a definitive individual check still creates its normal evaluation snapshot. Matching's NEEDS_INFORMATION is only a preview classification.

## 5. History and data preservation evidence

The preview contains no persistence call for an evaluation. Saving shared answers updates only authenticated-user answers/profile revision, just as individual answer saving does; prior history snapshots remain immutable, although their existing stale indicator can reflect changed answers.

The new integration suite compares the entire history table before and after repeated previews, plus scheme and rule tables. They match. For one citizen, matching leaves zero history entries; an explicit individual check creates exactly one; another matching preview leaves that entry and snapshot unchanged. The browser journey independently obtains positive, negative and missing classifications with no history, then verifies that only an individual check adds one entry.

Live development verification used authenticated read-only questionnaire/preview requests. SHA-256 comparisons before and after startup/verification match for all **18 scheme rows, 17 rule rows, 16 question rows, 36 links, both existing evaluation rows/snapshots, the existing stored answer and suggestion**. This includes the original browser fixture and converted draft. Application scheme definitions and citizen stored data were not modified by this patch. Ordinary login audit events were appended by authenticated verification; no historical audit row was changed.

## 6. Commands executed and actual results

All results below were executed on 2026-10-13 using existing cached/installed tools. No dependency installation was performed.

| Check | Command/environment | Actual result |
| --- | --- | --- |
| Full backend regression | Cached Maven 3.9.11: `mvn.cmd -o -Dmaven.repo.local=C:/Users/ABHISHEK/.m2/repository -Djacoco.append=false test`; DB_URL privately set to distinct TEST_DB_URL; RUN_DB_TESTS=true, USE_TESTCONTAINERS=false | Exit 0, BUILD SUCCESS. **92 passed: 39 existing API + 11 demo integration + 7 matching integration + 35 engine; zero failures/errors/skips.** Surefire XML counts inspected. |
| Backend packaging | Same Maven: `mvn.cmd -o -Dmaven.repo.local=C:/Users/ABHISHEK/.m2/repository -DskipTests package` | Exit 0. Current JAR packaged; this packaging command skipped tests and is not counted as a test run. |
| Full frontend suite | Portable existing Node 24: `node node_modules/vitest/vitest.mjs run` | Exit 0. **17 passed across 6 files** (14 existing + 3 matching). |
| TypeScript | `node node_modules/typescript/bin/tsc --noEmit --incremental false` | Exit 0. |
| Frontend build | `node node_modules/vite/bin/vite.js build` | Exit 0. |
| Browser regression | `node node_modules/@playwright/test/cli.js test --config=../.tools/patch8-playwright.config.mjs`; current JAR on 8081 against isolated TEST_DB_URL, temporary Vite proxy on 5174 | Exit 0. **17 passed**, zero skipped/flaky/unexpected: eight demo journeys, new matching journey, existing citizen/admin workflows and six viewport/theme cases. |
| Authenticated development HTTP | Existing admin session; GET questionnaire and preview on 8080 | Fourteen questions and eight results for IDs 11–18, current rule IDs 10–17. All NEEDS_INFORMATION for the admin's empty questionnaire; browser fixture excluded. No answer/history write. |
| Actual production-profile HTTP guards | Current JAR on temporary 8082 using isolated test DB and `--spring.profiles.active=prod`; authenticated GET questions/preview and CSRF-valid PUT answers | **404 for all three endpoints**, no fictional data or answer/history write. This is a profile-guard test, not a production deployment. |
| Data preservation | Before/after read-only MySQL row hashes | All seven compared data sets match. |
| Visual review | Browser screenshots at 360 and 1440 pixels | Both inspected; existing design retained. Browser overflow assertion passed at 360 pixels. |

Tests cover exact `300000.00000000000000001` JSON input remaining above the Student threshold after storage/retrieval, inclusive age/income boundaries, each requested classification, false-plus-unknown, current rule revisions and OR semantics, atomic validation/conflicts, authentication/CSRF/admin RBAC, caller isolation, production restrictions, eight-only selection, individual checks and history preservation. Existing Patches 1–3 and demo-regression suites passed.

Intermediate verification failures were resolved: the new UI tests initially used DOM matchers without importing the existing installed matcher integration, and a prefill assertion needed to wait for the existing asynchronous load/effect. The first cached Maven compilation encountered the existing compiler-resource-close error; the retry succeeded without configuration/dependency changes. An initial production PUT probe had a stale CSRF token and received 403; refreshing the token before the final probe yielded the verified production guard's 404. No failed or unexecuted check is reported as passed.

Ignored evidence: `.tools/patch8-backend.log`, `patch8-package.log`, `patch8-frontend.log`, `patch8-typecheck.log`, `patch8-build.log`, `patch8-browser.log`, `patch8-browser-results.json`, `patch8-http-evidence.json`, `patch8-app-before.json`, `patch8-app-preservation.json`; backend Surefire XML and browser screenshots under generated test output. No new coverage percentage claim is made.

## 7. Remaining limitations and handoff

No unresolved blocker. Matching is intentionally limited to the eight fictional PDF development/test schemes and saved self-reported questionnaire answers. It does not verify documents, offer payouts, make applications, include the browser fixture or replace the long-term real-scheme requirement. Preview results are not persisted; users choose the existing individual workflow when they want a history entry.

Docker/Testcontainers, real production/TLS deployment, load benchmarking and a full assistive-technology audit were not run. The actual production-profile guard was exercised locally on the isolated test DB. Temporary verification servers were closed; the development backend remains on 8080 with the current build and the existing frontend remains on 5173.

Architecture, schema, authentication/JWT/CSRF/RBAC, all nine operators, rule semantics, published definitions, Patches 1–3, AI limits, UI style rules, unrelated records and real-scheme research are preserved. No AI assistant or subsequent patch was implemented.

**CHECKPOINT 8: PATCH 8 — VERIFIED.** Stopped after this patch. Awaiting explicit approval before the AI assistant or any other feature.
