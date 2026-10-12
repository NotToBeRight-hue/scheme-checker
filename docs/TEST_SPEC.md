# TEST_SPEC.md — v3 prototype acceptance
Not executed in this handoff. Frozen evaluation date for rule fixtures: 2026-10-12 Asia/Kolkata.

1. EQ equal TRUE
2. NE unequal TRUE
3. GT equal FALSE and greater TRUE
4. GTE equal TRUE
5. LT equal FALSE and lower TRUE
6. LTE equal TRUE
7. IN member TRUE
8. NOT_IN absent TRUE
9. BETWEEN inside TRUE
10. BETWEEN min/max inclusive TRUE
11. AND all TRUE
12. AND contains FALSE -> FALSE
13. OR contains TRUE -> TRUE
14. OR all FALSE -> FALSE
15. AND UNKNOWN no FALSE -> UNKNOWN
16. OR UNKNOWN no TRUE -> UNKNOWN
17. Nested groups truth table
18. Three GROUP levels accepted
19. Four GROUP levels rejected
20. Unsupported operator rejected
21. Type mismatch rejected
22. Exact BigDecimal boundary, no floating drift
23. Missing decision-critical input -> UNKNOWN -> HTTP 422, no history
24. All leaf explanations present even after definitive logical outcome

Implement individually named JUnit 5 rule tests and real request/persistence assertions where relevant. Also test register/login/logout, expired JWT, CSRF bootstrap/login/logout/mutations, RBAC, cross-user history/suggestions isolation, per-account and per-IP login rate limits, profile/answers optimistic conflict, shared-question preservation, MySQL migrations/constraints, rule publication immutability/atomicity, history snapshot/staleness, no real unverified scheme seeds, citizen/admin browser journeys, keyboard/360px/768px/1440px and both themes. Use Testcontainers MySQL, Spring MockMvc, Vitest/RTL and Playwright as practical. 88 extended formal tests deferred. Do not claim pass until actually executed.
