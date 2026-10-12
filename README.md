# Government Scheme Selector and Eligibility Assistance System

Complete local v3 prototype: React 19 / Vite 8 / TypeScript, Java 17 / Spring Boot 4.1.1, MySQL 8.4 and Flyway. The development catalog starts with eight clearly fictional synthetic schemes. No genuine government scheme has been verified or approved. This is preliminary assistance, not government affiliation, entitlement, legal advice or application submission.

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
