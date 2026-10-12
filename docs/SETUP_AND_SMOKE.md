# SETUP_AND_SMOKE.md
1. Check Java 17, Node 24, Docker Compose, MySQL 8.4 image, Maven wrapper. Install and resolve versions; pin compatible patches and create package-lock.json after successful npm install. Existing pom.xml is a starter and must be verified/updated.
2. Create local `.env` from `.env.example`; use long random JWT secret, never commit it. Start DB via `docker compose up -d` (verify compose paths). Run Flyway migrations on real MySQL, not H2, and inspect exactly 11 domain tables plus Flyway history.
3. Run backend build/tests via `./mvnw test` (create Maven Wrapper if absent); start backend. Run `npm ci` (after lockfile generation), `npm run build`, `npm run dev` in frontend. Verify `/api/v1/auth/csrf`, auth cookie, CSRF protection, DB persistence.
4. Execute citizen and admin browser journeys including real API integration, synthetic scheme labels, rule publication, suggestions, history and theme/mobile states.
5. Run rule 24 tests and security/concurrency/DB checks. Record exact command, exit code, date, failures and environment in IMPLEMENTATION_REPORT.md. Never claim unexecuted tests passed.
