# Government Scheme Selector and Eligibility Assistance System

Government Scheme Selector uses React 19 / Vite 8 / TypeScript, Java 17 / Spring Boot 4.1.1, MySQL 8.4 and Flyway. It includes citizen and administrator workflows, a shared 14-question questionnaire, deterministic matching for eight illustrative schemes, individual eligibility checks, immutable history, and light/dark dashboards using the supplied PNG assets. AI assistance is postponed. Scheme information is illustrative and is not an official government benefit program.

## Which files should I keep?

The table below describes the actual files in this folder. Keep the source, tests, configuration and documentation together for a maintainable project. Generated output is not source code. No files were deleted while preparing this inventory.

### Required project files

| File or folder | Why it is needed |
| --- | --- |
| `backend/src/main/java/` | Backend APIs, authentication/security, domain models, deterministic eligibility engine and seeding workflows. |
| `backend/src/main/resources/application.yaml` | Backend configuration. |
| `backend/src/main/resources/db/migration/` | Flyway schema migration and database integrity rules. Keep migration history intact. |
| `backend/src/main/resources/demo/eight-schemes.json` | Definitions for the eight illustrative schemes and shared questionnaire. |
| `backend/src/test/` | Backend regression, security, precision, publication and matching tests. Required for verification, although not loaded by the running app. |
| `backend/pom.xml` | Maven dependencies, build and test configuration. |
| `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/` | Maven Wrapper for reproducible builds on Windows and other platforms. The hidden `.mvn` folder must be included. |
| `frontend/src/` | React components, API integration, exact numeric serialization, themes and frontend tests. |
| `frontend/public/assets/` | The four supplied hero/logo PNGs used by both themes. |
| `frontend/e2e/` | Browser workflow and responsive/cleanup tests. Required for verification. |
| `frontend/index.html` | Frontend entry point and initial theme resolution. |
| `frontend/package.json`, `frontend/package-lock.json` | Dependency manifest and reproducible dependency versions. Keep the lockfile. |
| `frontend/tsconfig.json`, `frontend/vite.config.ts`, `frontend/vitest.config.ts`, `frontend/playwright.config.ts` | TypeScript, development/build, unit-test and browser-test configuration. |
| `scripts/` | Environment generation/loading, startup and verification commands documented below. |
| `compose.yaml`, `compose.init/` | Optional Docker-based MySQL setup, including test-database initialization. Keep for the documented portable setup. |
| `.env.example` | Shareable environment template. It does not replace a configured private `.env`. |
| `.gitignore` | Prevents local secrets, dependencies and generated output from being committed. |
| `README.md`, `docs/` | Setup instructions, project specifications, contracts, security guidance, tests and source research. Not runtime files, but keep with the maintained project. |

### Keep locally or archive separately

| File or folder | Guidance |
| --- | --- |
| `.env` | Required by the current local startup/test scripts. Contains private configuration and credentials. Keep privately; never commit, upload or include in a shared ZIP. Recreating it requires valid configuration and secrets. |
| `.git/` | Repository history, branch and remote configuration. Keep in your working checkout. It is not needed in a source ZIP, but deleting it removes local Git history/configuration. |
| `.tools/mysql-data/` | Persistent local MySQL database, including accounts, schemes, answers, history and audit records. **Do not delete as cleanup.** Back up the database before any deliberate replacement or migration; do not share this directory. |
| `.tools/mysql-8.4.11-winx64/` | Portable MySQL installation used by this machine's current setup. Keep until a replacement database service is configured and verified. |
| `.tools/node-v24.21.0-win-x64/` | Portable Node installation detected by the frontend/verification scripts. Removable only after a compatible Node 24 installation is available on PATH. |
| `IMPLEMENTATION_REPORT.md`, `FINAL_PROJECT_VERIFICATION_REPORT.md` | Current implementation and verification records. Keep for project handover and known limitations. |
| `PATCH_4_REPORT.md`, `PATCH_4B_REPORT.md`, `PATCH_8_REPORT.md`, `PATCH_9_UI_UX_REPORT.md`, `DEMO_SCHEME_REPLACEMENT_REPORT.md` | Historical patch/research evidence. Not needed to execute the app; archive outside the runtime package if desired. Preserve these records rather than treating them as disposable cache. |
| `CODEX_START_HERE.md` | Original development instructions. Optional for runtime; keep or archive for future development context. |
| `PACKAGE_VERIFICATION.json`, `verification/evidence.json` | Earlier verification records, not executable configuration. They contain older results; consult the latest verification report for current results. Optional in a runtime package; archive if desired. |

### Generated or optional files that can be removed

Stop any process using the relevant files first. Deleting generated output removes its evidence/artifacts until they are regenerated; it does not remove application source.

| File or folder | What happens if removed / how to recreate |
| --- | --- |
| `frontend/node_modules/` | Installed dependencies. Recreate with `npm ci` in `frontend/`; downloading dependencies may require network access. Stop the frontend first. |
| `frontend/dist/` | Production frontend build. Recreate with `npm run build`. Keep if a running deployment serves this folder. |
| `frontend/test-results/` | Generated screenshots, traces and browser results. Archive needed evidence first; recreate by running browser tests. |
| `frontend/debug.log` and other generated `*.log` files | Logs. Remove after stopping their writers and saving any needed diagnostics. Logs can contain sensitive information; do not share blindly. |
| `backend/target/` | Compiled classes, JAR, test and coverage reports. Recreate with Maven. **The current backend runs a JAR from this folder**, so stop it before deletion and rebuild before restarting. |
| `.vscode/settings.json` / `.vscode/` | Optional editor preferences; not required to run/build the application. |
| Root `.github/` | Currently empty; removable if still empty. Future GitHub workflow/configuration files would need separate review. |
| `backend/.github/modernize/java-upgrade/` | Generated Java-upgrade extension logs and tool-recording hooks, not application/build source. Optional after ending any active extension upgrade session; archive its logs if needed. |
| `.tools/*.log`, `.tools/*.pid`, `.tools/*.zip` | Local logs, process-ID records and downloaded archives. Archives can be removed after extraction; PID records after their processes stop. Do not act on stale PID files without checking the actual process. |
| `.tools/*.mjs`, `.tools/*.json`, `.tools/*.txt` | Local one-off verification helpers and evidence. Not part of the normal documented startup. Archive evidence you want to retain; deleting helpers prevents reproducing their exact custom checks until restored. |

**Do not delete `.tools/` as a whole:** it contains the persistent database and tools currently used by this installation. Do not remove a Docker `mysql_data` volume as filesystem cleanup either; that also stores database records.

### What to include when sharing source

Include `backend/` without `target/`, `frontend/` without `node_modules/`, `dist/`, `test-results/` and logs, plus `scripts/`, `docs/`, `compose.yaml`, `compose.init/`, `.env.example`, `.gitignore`, this README and relevant reports. Include `backend/.mvn/` and `frontend/public/assets/`, even though they are easy to overlook.

Exclude `.env`, `.git/`, `.tools/`, database files/volumes, dependencies, build/test output and credentials. A deployment package has different requirements: it needs the actual built frontend/backend artifacts and securely supplied configuration.

## Windows setup

Install Java 17, Node 24 LTS and Docker with Compose. Maven is downloaded by the committed Maven Wrapper. From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/New-LocalEnv.ps1
docker compose up -d
powershell -ExecutionPolicy Bypass -File scripts/Start-Backend.ps1
```

In a second terminal:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/Start-Frontend.ps1
```

Open http://localhost:5173. Register a citizen account, or sign in using ADMIN_EMAIL and ADMIN_PASSWORD in the private local `.env`. The first startup provisions one administrator only if none exists. Remove ADMIN_PASSWORD after provisioning. Do not commit `.env`. The checked-in `.env.example` documents all variables. PowerShell scripts load environment values; Spring itself does not load `.env` automatically.

On this machine, verification used isolated portable MySQL 8.4.11 at 127.0.0.1:3307, with schemes_demo for the app and schemes for integration fixtures, and Node 24.21.0 under ignored `.tools/`, because Docker is unavailable. The existing local `.env` is configured for that instance. Those downloaded binaries and private credentials are not source dependencies or delivery artifacts. For Compose, use port 3306 in DB_URL. Do not delete the existing database to switch setups; use a fresh database or export/import deliberately.

## Other platforms

Copy `.env.example` to `.env`, generate secrets and export its values into the backend process environment. Run `docker compose up -d`, then `cd backend && ./mvnw spring-boot:run`. In another terminal run `cd frontend && npm ci && npm run dev`. Both platforms use Vite's same-origin `/api` proxy to port 8080. Development cookies work over HTTP; HTTPS requests receive Secure cookies. Production must use HTTPS and route `/api` to the backend on the same origin.

## Verification

```powershell
# Engine tests, frontend tests, build. Integration tests are explicitly skipped.
powershell -ExecutionPolicy Bypass -File scripts/Verify.ps1
# Add request, security, persistence and concurrency tests against TEST_DB_URL in configured MySQL.
powershell -ExecutionPolicy Bypass -File scripts/Verify.ps1 -Database
# With application servers running and Chromium installed (browser mode skips npm ci and JAR repackaging to avoid Windows server file locks):
cd frontend
npx playwright install chromium
cd ..
powershell -ExecutionPolicy Bypass -File scripts/Verify.ps1 -Database -Browser
# With Docker available, isolated Testcontainers MySQL instead of configured database:
powershell -ExecutionPolicy Bypass -File scripts/Verify.ps1 -Containers
```

Backend equivalent: load `.env`, set DB_URL to TEST_DB_URL and RUN_DB_TESTS=true and run `backend/mvnw.cmd test` **from the backend directory**. USE_TESTCONTAINERS=true selects an isolated MySQL 8.4.11 container. Test databases receive synthetic fixtures and random test accounts/drafts; do not run tests on production data. Unit engine fixtures use 2026-10-12. Runtime evaluations use the current Asia/Kolkata date. JaCoCo reports are generated under `backend/target/site/jacoco/`; browser results and responsive screenshots under `frontend/test-results/`.

## Workflows

Citizens can register/sign in/out, complete partial profiles, search/filter/paginate published schemes, answer typed scheme questions, run deterministic eligibility checks, inspect every condition, read immutable/stale history and submit/view suggestions. Missing decision-critical input returns 422 without recording a check; outdated revisions return 409 without retry.

Administration provides overview, categories, typed questions, draft scheme metadata and linked questions, validated JSON rule drafts and atomic publication, suggestion review, accepted-suggestion conversion to drafts and sanitized audit logs. Published questions and rule versions are immutable. Published metadata is also frozen to preserve its meaning; revised metadata uses a new draft scheme. New rule versions can be published for an existing scheme. Synthetic publication and catalog visibility are restricted to dev/test. Genuine publication requires an official HTTPS gov.in/nic.in subdomain, verified date, criterion mappings, documented exceptions and reviewer approval; the administrator must actually verify the source.

There are exactly eleven domain tables plus Flyway history. JPA handles aggregate mutations; JDBC is used for read-only dashboard counts. MySQL triggers enforce immutable evaluations and published rules. The local Compose database disables binary logging to allow trigger creation without global privileges; production migration credentials must support trigger creation and should be separate from restricted runtime DML credentials. Configure Spring Flyway's user/password separately in production. Do not enable dev/test profiles in production.

Authentication uses 15-minute signed JWTs in HttpOnly host-only cookies. JWTs are never returned in JSON or stored by the client. BCrypt hashes SHA-256-prehashed passwords so the full 8–128-character range is preserved. CSRF cookies rotate during registration/login/logout. Login limiting uses IP and account windows in this single-process prototype. Logout deletes the browser cookie; previously copied JWTs remain valid until their short expiry, consistent with stateless sessions and no session table.

See [docs/IMPLEMENTED_CONTRACTS.md](docs/IMPLEMENTED_CONTRACTS.md) for exact additional DTO choices and [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md) for executed checks and limitations. The original v3 documents remain authoritative.
