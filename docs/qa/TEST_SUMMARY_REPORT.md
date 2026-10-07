# Test Summary Report - Release 1.0

| | |
|---|---|
| Release | 1.0 (Modules 0-5, storefront UI, performance and security hardening) |
| Build | `main`, CI run on 2026-10-07 (GitHub Actions) and Jenkins build #7 on macOS (all gates green) |
| Prepared by | RB Chowdhury, QA Lead |
| Plan | [Test Plan](TEST_PLAN.md) |

## 1. Recommendation

**GO for release 1.0.** All exit criteria are met, every requirement is covered, and no defect is open.
The last open defect, [DEF-007](DEFECT_REPORTS.md#def-007) (catalog without pagination), was fixed test-first
before sign-off: response size -91 %, constant regardless of catalog size, and on the large local
catalog the endpoint p95 dropped from 22 ms to 9 ms (-59 %).

## 2. Exit criteria

| # | Criterion | Target | Result | |
|---|---|---|---|---|
| 1 | Automated tests on CI | 100 % pass | **548 / 548** passed, 0 skipped (+44 smoke against Docker) | ✅ |
| 2 | Requirement coverage | >= 95 % | **59 / 59 = 100 %** | ✅ |
| 3 | Open Critical / High defects | 0 | **0** | ✅ |
| 4 | k6 smoke + load SLOs | all pass | all pass | ✅ |
| 5 | k6 flash sale | orders = stock, stock 0 | 15 / 15 (CI), 50 / 50 (local, 300 buyers) | ✅ |
| 6 | OWASP ZAP | 0 Medium / High | **0** (Low only, informational) | ✅ |
| 7 | OSV-Scanner | 0 unaccepted CVSS >= 9 | **0 open** (was 21); 2 accepted, guarded, expire 2026-11-05 | ✅ |
| 8 | Code coverage (JaCoCo) | >= 96 % lines, >= 85 % branches | **96.9 % lines, 86.6 % branches** | ✅ |
| 9 | Mutation score (PIT, business logic) | >= 84 % | **84.9 %** (was 65.5 %); test strength 97.5 % | ✅ |

## 3. Test execution

| Suite | Tests | Passed | Failed | Skipped | Runs against |
|---|---|---|---|---|---|
| Backend unit + integration | 279 | 279 | 0 | 0 | H2 (PostgreSQL mode) + Flyway |
| Automation: API, DB, security, BDD, UI | 269 | 269 | 0 | 0 | Backend + PostgreSQL 16 + headless Chrome |
| **Total** | **548** | **548** | **0** | **0** | |
| Smoke against the Docker images | 44 | 44 | 0 | 0 | `docker compose --profile app` (postgres + backend + nginx storefront) |

The automation total contains 43 Cucumber scenarios and 12 Selenium UI tests; the rest are API,
database and security tests. Figures come from the CI "Test totals" annotations (surefire XML).

## 4. Performance

| Test | Environment | Load | Key results | Thresholds |
|---|---|---|---|---|
| Load | CI runner (2 CPU) | 40 browsing + 10 buying users, 8 min | 18,701 requests, 0 % failed, read p95 2.2 ms, write p95 7.2 ms, purchase journey p95 28 ms, 786 orders paid | all pass |
| Load | MacBook (Docker) | same | 18,641 requests, 0 % failed, p95 20 ms, purchase journey p95 81 ms, 771 orders paid | all pass |
| Flash sale | MacBook | 300 buyers, 50 units, same moment | 50 created, 250 rejected (409), 0 unexpected, stock 0; order p95 279 ms | all pass |
| Flash sale | MacBook | 100 buyers, 20 units | 20 created, 80 rejected, stock 0; order p95 112 ms | all pass |
| Smoke + flash sale | CI, every push | 2 users; 60 buyers / 15 units | 0 % failed, checks 100 % | all pass |
| Catalog (DEF-007) | CI, every push | 10 visitors, 20 s | response 32,927 -> 2,998 bytes p95 (234 -> 20 products) | all pass |
| Load after DEF-007 fix | MacBook (Docker) | 40 + 10 users, 8 min | 18,645 requests, 0 % failed, p95 16 ms; `GET /api/products` p95 22 -> 9 ms (-59 %) | all pass |

Observation: order latency grows with the number of simultaneous buyers of the **same** product
(112 ms -> 279 ms from 100 to 300 buyers) because the row update is serialised. This is the expected
cost of correctness and is far below the 3-second target.

## 5. Security

| Activity | Result |
|---|---|
| Code review + attack tests | 2 High/Medium issues found and fixed (brute force DEF-003, timing enumeration DEF-004) |
| JWT attacks (alg none, edited payload, guessed secret, malformed headers) | all rejected |
| Access-control matrix (19 endpoints x 3 roles) | all as designed |
| Injection, path traversal, oversized input, malformed bodies | no success, no 5xx |
| OWASP ZAP API scan (OpenAPI, logged in) | 0 Medium/High; 1 Low fixed (CORP header) |
| OSV-Scanner | 21 vulnerable packages / 85 advisories -> 0 (Spring Boot 3.5.16 + pinned patches) |

## 6. Defects

| Severity | Found | Fixed | Open |
|---|---|---|---|
| Critical | 1 | 1 | 0 |
| High | 4 | 4 | 0 |
| Medium | 7 | 7 | 0 |
| Low | 3 | 3 | 0 |
| **Total** | **15** | **15** | **0** |

8 product defects, 5 test-code defects, 2 test-infrastructure defects. Every closed product defect has a regression test.
Details: [Defect Reports](DEFECT_REPORTS.md).

## 7. Residual risks accepted for release 1.0

| Risk | Why accepted | Follow-up |
|---|---|---|
| Login lock is per instance (memory) | One backend instance | Redis when scaling out |
| Logged-out token valid until expiry (1 h) | Short expiry | Refresh tokens / denylist |
| Spring Boot 3.5 out of OSS support | Patched libraries pinned and monitored in CI | Migrate to Spring Boot 4 |
| CVE-2026-47884 in spring-webmvc 6.2.19 (CVSS 9.8, no 6.2.x fix) | Exploitable only through XsltView; this API renders no views. Guard test fails the build if one is added | Exception expires 2026-11-05: upgrade or re-assess |
| GHSA-j9f9-w8pj-32f8 in spring-webmvc 6.2.19 (CVSS 9.8, Server-Sent Events, no open-source 6.2.x fix) | Exploitable only when the app streams SSE; this API streams none. Guard test fails the build if an SSE or WebMvc.fn endpoint is added | Same expiry and plan: Spring Framework 7 / Spring Boot 4 |
| 8 surviving mutants (PIT) | 5 are equivalent (cannot change behaviour, e.g. Luhn `d > 9` vs `d >= 9`), 3 are defensive or housekeeping code | Reviewed one by one in docs/modules/MUTATION_BN.md |
| Mock payment gateway | No real money in this release | Provider sandbox contract tests |
| Registration reveals that an e-mail exists | Usability; common e-commerce trade-off | Re-evaluate with product owner |

## 8. Lessons learned

1. **Assert state, not just status codes:** a 200 response hid that product updates were ignored (DEF-001).
2. **Run on the CI operating system early:** a Mac-only green suite hid a Linux typing bug (DEF-008).
3. **Fix flaky tests at the root:** stale-element handling instead of retries (DEF-009).
4. **Performance reports find design problems:** the per-endpoint table exposed the missing pagination (DEF-007),
   which was then fixed test-first: tests red on the unfixed code, green after the fix, before/after measured
   in the same environment.
5. **Verify that a "fixed" version really exists:** an advisory pointed to an unpublished Tomcat release (DEF-005).
6. **Test the tests:** 96 % line coverage hid a test that could not fail (DEF-013). Mutation testing raised the
   score from 65.5 % to 84.9 % with 30 targeted tests, and branch coverage from 78.8 % to 86.6 % as a side effect.
7. **Make CI explain itself:** failures, totals, k6, ZAP and OSV results are annotations on the run page,
   so a red build can be understood without downloading logs.
