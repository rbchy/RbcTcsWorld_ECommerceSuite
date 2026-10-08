# Product Risk Register

Risk score = **Likelihood (1-3) x Impact (1-3)**. 7-9 = High, 4-6 = Medium, 1-3 = Low.
The score decides test depth (see [Test Strategy](TEST_STRATEGY.md) section 4). "Coverage" lists the
test layers that mitigate the risk; "Residual" is what remains after mitigation.

| ID | Risk | L | I | Score | Mitigation (tests) | Coverage | Residual |
|---|---|---|---|---|---|---|---|
| R-01 | Wrong total charged (discount, tax, shipping, rounding) | 2 | 3 | **6** | Boundary tables for free shipping and tax rounding; coupon maths; order total = quote total checked under load | Unit, Integration, API, BDD, UI, k6 (`business_errors`) | Low |
| R-02 | Stock oversold when many customers buy the last units | 3 | 3 | **9** | Atomic conditional `UPDATE` + CHECK constraint; 10-buyer race test; k6 flash sale (100-300 buyers) in every CI build; DB invariant "no negative stock" | Integration, API, k6, DB | Low |
| R-03 | Customer reads or changes another customer's cart, order, review or wishlist (IDOR) | 2 | 3 | **6** | Owner check inside every repository query; IDOR test per resource; access-control matrix | Unit, Integration, API | Low |
| R-04 | Customer reaches admin functions (privilege escalation) | 2 | 3 | **6** | Role read from DB, not token; mass-assignment test; 19 x 3 access matrix; forged-token tests | Integration, API (security) | Low |
| R-05 | Password guessing (brute force) | 3 | 3 | **9** | Lockout 5 failures / 15 min -> 429 + Retry-After (found missing: [DEF-003](DEFECT_REPORTS.md#def-003)) | Unit, Integration, API | Low (lock is per instance) |
| R-06 | Attacker discovers which e-mails have accounts | 2 | 2 | **4** | Same message for unknown e-mail and wrong password; equal response time ([DEF-004](DEFECT_REPORTS.md#def-004)); lockout also for unknown e-mails | Unit, API (timing) | Low; registration still says "already registered" (accepted) |
| R-07 | Forged or tampered JWT accepted | 1 | 3 | **3** | Signature + issuer + expiry check; `alg:none`, edited payload, guessed secret tests; start-up refuses the public dev secret in `prod` | Unit, Integration, API | Low |
| R-08 | Card data stored or logged | 1 | 3 | **3** | Only last 4 digits stored; DB scan for card/CVV columns; `toString` never prints card data | Unit, DB | Low |
| R-09 | Double charge or charge without order change | 2 | 3 | **6** | Second pay -> 409; declined attempt recorded but order stays PLACED; refund on cancel | Unit, Integration, API, BDD | Low |
| R-10 | Order moves to an impossible status (e.g. ship unpaid, cancel shipped) | 2 | 2 | **4** | State machine table of all transitions; timeline must match status | Unit, API, DB | Low |
| R-11 | Return refunds the wrong amount or restocks damaged goods | 2 | 2 | **4** | Decision table on three levels; 30-day boundary with fixed clock | Unit, API, BDD | Low |
| R-12 | Coupon used more often than allowed | 2 | 2 | **4** | Once per customer, global atomic counter, failed order does not consume the coupon | Unit, Integration, API | Low |
| R-13 | Fake or manipulated reviews / wrong average | 2 | 2 | **4** | Verified purchase only; one per customer; moderation; average recalculated under a row lock; 8 concurrent reviews; DB drift query over all products | Unit, Integration, API, concurrency, DB | Low |
| R-14 | Stored XSS through reviews | 2 | 3 | **6** | Payload stored and returned as plain data (API test); React escapes it on screen (verified in an exploratory UI check) | API, exploratory UI | Low: add an automated UI assertion |
| R-15 | Known vulnerable library shipped | 3 | 3 | **9** | OSV-Scanner every push, build fails on fixable CVSS >= 9; 21 vulnerable packages reduced to 0 ([DEF-005](DEFECT_REPORTS.md#def-005)) | CI (SCA), guard test | Low: Spring Boot 4.1 (in OSS support), 0 vulnerable packages, 0 accepted exceptions; Tomcat/Jackson patch pins until Boot ships them |
| R-16 | Injection (SQL, path traversal) | 1 | 3 | **3** | Parameterised JPA queries; injection payload tests; ZAP active scan with FAIL rules | API (security), ZAP | Low |
| R-17 | Internal details leak in errors or actuator | 2 | 2 | **4** | Global JSON error handler; tests for stack traces, actuator endpoints, headers | Integration, API, ZAP | Low |
| R-18 | Slow pages at peak traffic | 2 | 2 | **4** | k6 load with SLO thresholds; stress and spike on demand | k6 | Low: catalog paginated, response size gated in CI ([DEF-007](DEFECT_REPORTS.md#def-007)) |
| R-19 | UI does not reflect API rules (button works but rule ignored) | 2 | 2 | **4** | 12 Selenium journeys incl. coupon, declined card, return, review, wishlist | UI | Low |
| R-20 | Flaky automation hides real failures or blocks releases | 2 | 2 | **4** | Explicit waits only, stale-safe page objects, per-test data, failure annotations in CI ([DEF-008](DEFECT_REPORTS.md#def-008), [DEF-009](DEFECT_REPORTS.md#def-009)); PIT mutation testing finds assertions that cannot fail ([DEF-013](DEFECT_REPORTS.md#def-013)) | Framework design, PIT | Low |
| R-21 | Schema change breaks existing data | 1 | 3 | **3** | Flyway migrations run in every integration test and every CI database; Hibernate `validate` | Integration, CI | Low |
| R-22 | An API change silently breaks the storefront or another client | 2 | 2 | **4** | 12 strict JSON Schemas (consumer side); OpenAPI breaking-change gate with self-test (provider side) | API, CI | Low |
| R-23 | Customers with disabilities cannot use the shop (and ADA legal exposure) | 2 | 3 | **6** | axe-core WCAG 2.1 AA checks on 8 pages in every build ([DEF-018](DEFECT_REPORTS.md#def-018)) | UI | Medium: automated rules cover only part of WCAG; manual keyboard / screen-reader checks per release |

## Heat map

| Impact \ Likelihood | 1 Low | 2 Medium | 3 High |
|---|---|---|---|
| **3 High** | R-07, R-08, R-16, R-21 | R-01, R-03, R-04, R-09, R-14, R-23 | **R-02, R-05, R-15** |
| **2 Medium** | | R-06, R-10, R-11, R-12, R-13, R-17, R-18, R-19, R-20, R-22 | |
| **1 Low** | | | |

The three red risks (oversell, brute force, vulnerable libraries) each have an automated gate
that runs on every push.
