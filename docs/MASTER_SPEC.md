# MASTER SPECIFICATION v3.0 — AUTHORITATIVE

## Status and priority
**Codex is authorized to implement the whole prototype.** This v3 package is the sole source of truth. Older draft v0.1, previous v2 handoff, and the unrelated Work-mode v1.0 ZIP are superseded and are NOT implementation references. If details are missing, Codex should implement consistent, secure DTOs and document choices, rather than change frozen decisions.

**Not a working product:** Source code here is only a minimal, unverified starting scaffold. Codex must build, test and integrate everything. No builds/tests or government source checks are claimed passed by this ZIP.

## Goal and boundaries
A public-facing future Indian Government Scheme Selector and Eligibility Assistance System. Current deliverable: locally running complete prototype. Offer discovery, explainable preliminary screening, source links, document guidance and citizen suggestions. Never claim official government affiliation, official approval, submitted applications, benefit entitlement or legal advice. No Aadhaar, uploads, payment, AI/LLM eligibility, multilingual features or real government scheme claims without verification.

## Stack
Modular monolith. React 19 + Vite 8 + TypeScript frontend, CSS Modules/design tokens. Java 17 Spring Boot 4.1.1 planning target (verify actual compatible released versions during dependency resolution), Maven Wrapper, Spring Web, Security, Data JPA/Hibernate, Validation, Flyway, MySQL Connector/J JDBC. MySQL 8.4 LTS via Compose. JPA for aggregate CRUD; JDBC permitted for read-only dashboard projections. REST `/api/v1` same-origin Vite `/api` proxy. Node 24 LTS. Use lockfiles after actual installs. No unnecessary microservices or dependencies.

## Exactly 11 DOMAIN tables
1 `users` (role/status/credential), 2 `user_profiles` (DOB/state/annual income/education/occupation/revision), 3 `profile_answers` (typed answers), 4 `scheme_categories`, 5 `schemes`, 6 `questions`, 7 `scheme_questions`, 8 `scheme_rule_versions`, 9 `eligibility_evaluations`, 10 `admin_audit_logs`, 11 `scheme_suggestions`.
BIGINT UNSIGNED AUTO_INCREMENT PKs; JSON IDs decimal strings; FK/unique/index constraints; immutable published rules and evaluation snapshots. `flyway_schema_history` is an infrastructure table, not a twelfth domain table. Do NOT substitute UUID IDs, `user_roles`, `auth_sessions`, `saved_schemes`, `eligibility_runs` or `eligibility_results` tables from the conflicting v1 package. Sessions use short-lived JWT cookie; saved schemes/checklist are not required persistent features in this baseline.

## Citizen experience
Register/login/logout; authenticated dashboard (real profile completeness, recent checks, next actions, no fake metrics), profile with DOB/state/annual income/education/occupation, browse/search/filter/paginate published schemes, scheme details (benefits, eligibility, official source if verified, how to apply, disclaimer), scheme-specific typed questions, one-scheme eligibility check with full explanations and missing-input guidance, history list/detail with immutable facts and stale indicators, new-scheme and criterion-correction suggestions and own suggestion statuses. Partial profiles are allowed; unknown values are not silently zero/false. Accessible, responsive, usable loading/empty/error/conflict/success states.

## Lean admin
Overview, manage scheme/category/question metadata, create/edit draft JSON rules, validate and atomically publish immutable versions, review suggestions with PENDING/UNDER_REVIEW/NEEDS_INFO/ACCEPTED/REJECTED and review notes, convert accepted suggestion to a draft (never auto-publish), sanitized audit logs. No complex analytics, user management or bulk imports.

## Dataset
5–10 schemes overall initially, preferably 8 clearly marked **fictional synthetic** development schemes. Never make up genuine official criteria. No genuine schemes verified or approved at handoff. A real scheme can be published only after official URL, verified date, source-to-each-criterion mapping, exceptions, and admin reviewer approval. Synthetic seeds only in development/test, never public production catalog.

## Rule engine
Admin-authored deterministic schema-validated JSON tree; nested AND/OR groups maximum 3 GROUP levels (including root), leaf unique nodeId, operators EQ, NE, GT, GTE, LT, LTE, IN, NOT_IN, BETWEEN inclusive. Strict types and BigDecimal currency. TRUE/FALSE/UNKNOWN logic; AND FALSE dominates, OR TRUE dominates. Evaluate all leaves for complete explanations. Final TRUE=ELIGIBLE, FALSE=NOT_ELIGIBLE; decision-critical UNKNOWN=422 INPUT_REQUIRED and NO history row. No hardcoded scheme logic or LLM. MULTI_SELECT answer storage/display allowed but not eligibility conditions in MVP. Use fixed Asia/Kolkata evaluation date and UTC timestamps; completed age from DOB, Feb 29 -> Mar 1 in nonleap years as provisional project default only. Source-specific age exceptions require verification.

## Concurrency/history
`expectedRevision` for profile and `expectedProfileRevision` for scheme answers and evaluation; revalidate profile revision and active rule version inside evaluation-save transaction; conflicts 409 STALE_REVISION and no history. No automatic retry. Published rules immutable. History snapshots immutable; compute stale on read from profile revision, published rule version and relevant date-sensitive change.

## Security
BCrypt 8–128-character passwords; 15-min HS256 JWT (>=256-bit env secret, iss/aud/sub/role/iat/exp), HttpOnly host-only cookie Path=/api SameSite=Lax Secure in HTTPS, no localStorage/sessionStorage or JWT JSON. CSRF XSRF-TOKEN cookie + X-XSRF-TOKEN header on mutations, including login/register/logout; bootstrap and rotation. Backend role/ownership checks and active user verification, controlled admin provisioning (never public admin signup), IP/account login rate limits, sanitized errors/logs/audits. Preserve any existing auth/AI limits; no AI component is required.

## User interface
Light: #EDF0F2 canvas, #F7F8F8 surface, #253443 text, #286E70 accent. Dark: #151B22 canvas, #222B35 surface, #E5EAF0 text, #63ADA8 accent. Respect system preference initially, persist chosen theme. NO purple gradients, gradients generally, pill buttons, fake metrics/counts/reviews, vague hero, emoji UI icons, em dashes in site copy, AI-slop imagery/copy, stock-looking fake visuals, cursor animation or excessive scroll animations. Clear professional typography, responsive and accessible keyboard interactions, focus states and no console errors.

## Acceptance
24 individual engine tests in `docs/TEST_SPEC.md`, essential security/CSRF/RBAC/ownership/DB/concurrency tests, actual end-to-end citizen and admin workflows, migrations on MySQL 8.4, builds and startup, persistence after restart, responsive/light/dark checks. 88 old extended acceptance cases are deferred for prototype. >=80% critical engine/backend coverage is target, not claimed achieved. Keep evidence honest. Codex must not stop at a scaffold, disconnected screens or a plan.
