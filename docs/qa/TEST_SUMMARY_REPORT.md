# Test Summary Report - Release 1.0

| | |
|---|---|
| Release | 1.0 (Modules 0-5, storefront UI, performance and security hardening) |
| Build | `main`, CI run on 2026-10-04 (GitHub Actions) |
| Prepared by | RB Chowdhury, QA Lead |
| Plan | [Test Plan](TEST_PLAN.md) |

## 1. Recommendation

**GO for release 1.0**, with one Medium defect accepted for the next release:
[DEF-007](DEFECT_REPORTS.md#def-007) (catalog has no pagination). It has no functional impact at the
current catalog size and is tracked as the only gap in the traceability matrix.

## 2. Exit criteria

| # | Criterion | Target | Result | |
|---|---|---|---|---|
| 1 | Automated tests on CI | 100 % pass | **493 / 493** passed | ✅ |
| 2 | Requirement coverage | >= 95 % | **55 / 56 = 98 %** (gap: REQ-CAT-04) | ✅ |
| 3 | Open Critical / High defects | 0 | **0** | ✅ |
| 4 | k6 smoke + load SLOs | all pass | all pass | ✅ |
| 5 | k6 flash sale | orders = stock, stock 0 | 15 / 15 (CI), 50 / 50 (local, 300 buyers) | ✅ |
| 6 | OWASP ZAP | 0 Medium / High | **0** (Low only, informational) | ✅ |
| 7 | OSV-Scanner | 0 fixable CVSS >= 9 | **0 vulnerable packages** (was 21) | ✅ |

## 3. Test execution

| Suite | Tests | Passed | Failed | Skipped | Runs against |
|---|---|---|---|---|---|
| Backend unit + integration | 240 | 240 | 0 | 0 | H2 (PostgreSQL mode) + Flyway |
| Automation: API, DB, security, BDD, UI | 253 | 253 | 0 | 0 | Backend + PostgreSQL 16 + headless Chrome |
| **Total** | **493** | **493** | **0** | **0** | |

The automation total contains 43 Cucumber scenarios and 11 Selenium UI tests; the rest are API,
database and security tests. Figures come from the CI "Test totals" annotations (surefire XML).

## 4. Performance

| Test | Environment | Load | Key results | Thresholds |
|---|---|---|---|---|
| Load | CI runner (2 CPU) | 40 browsing + 10 buying users, 8 min | 18,701 requests, 0 % failed, read p95 2.2 ms, write p95 7.2 ms, purchase journey p95 28 ms, 786 orders paid | all pass |
| Load | MacBook (Docker) | same | 18,641 requests, 0 % failed, p95 20 ms, purchase journey p95 81 ms, 771 orders paid | all pass |
| Flash sale | MacBook | 300 buyers, 50 units, same moment | 50 created, 250 rejected (409), 0 unexpected, stock 0; order p95 279 ms | all pass |
| Flash sale | MacBook | 100 buyers, 20 units | 20 created, 80 rejected, stock 0; order p95 112 ms | all pass |
| Smoke + flash sale | CI, every push | 2 users; 60 buyers / 15 units | 0 % failed, checks 100 % | all pass |

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
| High | 3 | 3 | 0 |
| Medium | 5 | 4 | 1 (DEF-007) |
| Low | 3 | 3 | 0 |
| **Total** | **12** | **11** | **1** |

8 product defects, 4 test-code defects. Every closed product defect has a regression test.
Details: [Defect Reports](DEFECT_REPORTS.md).

## 7. Residual risks accepted for release 1.0

| Risk | Why accepted | Follow-up |
|---|---|---|
| Catalog without pagination (DEF-007) | Small catalog today; reads still fast | Release 1.1 |
| Login lock is per instance (memory) | One backend instance | Redis when scaling out |
| Logged-out token valid until expiry (1 h) | Short expiry | Refresh tokens / denylist |
| Spring Boot 3.5 out of OSS support | Patched libraries pinned and monitored in CI | Migrate to Spring Boot 4 |
| Mock payment gateway | No real money in this release | Provider sandbox contract tests |
| Registration reveals that an e-mail exists | Usability; common e-commerce trade-off | Re-evaluate with product owner |

## 8. Lessons learned

1. **Assert state, not just status codes:** a 200 response hid that product updates were ignored (DEF-001).
2. **Run on the CI operating system early:** a Mac-only green suite hid a Linux typing bug (DEF-008).
3. **Fix flaky tests at the root:** stale-element handling instead of retries (DEF-009).
4. **Performance reports find design problems:** the per-endpoint table exposed the missing pagination (DEF-007).
5. **Verify that a "fixed" version really exists:** an advisory pointed to an unpublished Tomcat release (DEF-005).
6. **Make CI explain itself:** failures, totals, k6, ZAP and OSV results are annotations on the run page,
   so a red build can be understood without downloading logs.
