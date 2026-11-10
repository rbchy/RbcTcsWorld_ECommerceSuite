<div align="center">

# 🛒 RbcTcsWorld E-Commerce Suite

### An Amazon-inspired online store built *to be tested*: Spring Boot 4 + React + PostgreSQL, with 1,489 automated tests behind 18 quality gates on every push

[![Typing SVG](https://readme-typing-svg.demolab.com/?font=Fira+Code&size=18&pause=1500&color=F97316&center=true&vCenter=true&width=900&repeat=true&lines=Java+21+%C2%B7+Spring+Boot+4.1+%C2%B7+React+%C2%B7+PostgreSQL+16;1%2C489+tests+%C2%B7+18+%2F+18+quality+gates+%C2%B7+GitHub+Actions+%2B+Jenkins;97.3%25+line+coverage+%C2%B7+89.3%25+mutation+score+%C2%B7+129+%2F+129+requirements+traced)](https://git.io/typing-svg)

<!-- ============ STATUS BAR ============ -->
[![CI](https://github.com/rbchy/RbcTcsWorld_ECommerceSuite/actions/workflows/ci.yml/badge.svg)](https://github.com/rbchy/RbcTcsWorld_ECommerceSuite/actions/workflows/ci.yml)
![Tests](https://img.shields.io/badge/Tests-1%2C489%20%C2%B7%200%20failed-22c55e?style=for-the-badge&logo=checkmarx&logoColor=white)
![Gates](https://img.shields.io/badge/Quality%20gates-18%20%2F%2018-22c55e?style=for-the-badge&logo=githubactions&logoColor=white)
[![Coverage](https://img.shields.io/badge/Line%20coverage-97.3%25-22c55e?style=for-the-badge)](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/coverage/)
[![Mutation](https://img.shields.io/badge/Mutation%20score-89.3%25-22c55e?style=for-the-badge)](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/mutation/)
[![Allure](https://img.shields.io/badge/Allure-live%20report-FF6C37?style=for-the-badge)](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=for-the-badge)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Selenium](https://img.shields.io/badge/Selenium-4.49-43B02A?style=for-the-badge&logo=selenium&logoColor=white)
![Playwright](https://img.shields.io/badge/Playwright-5%20targets-2EAD33?style=for-the-badge&logo=playwright&logoColor=white)
![Appium](https://img.shields.io/badge/Appium-Android%20%2B%20iOS-662D91?style=for-the-badge&logo=appium&logoColor=white)
![Cucumber](https://img.shields.io/badge/Cucumber-7-23D96C?style=for-the-badge&logo=cucumber&logoColor=white)
![k6](https://img.shields.io/badge/k6-load-7D64FF?style=for-the-badge&logo=k6&logoColor=white)
![OWASP ZAP](https://img.shields.io/badge/OWASP%20ZAP-DAST-00549E?style=for-the-badge)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-pipeline-D24939?style=for-the-badge&logo=jenkins&logoColor=white)

![Last Commit](https://img.shields.io/github/last-commit/rbchy/RbcTcsWorld_ECommerceSuite?style=flat-square&color=f97316&label=Last%20commit)
![Repo Size](https://img.shields.io/github/repo-size/rbchy/RbcTcsWorld_ECommerceSuite?style=flat-square&color=155e75&label=Repo%20size)
![Merged PRs](https://img.shields.io/github/issues-pr-closed/rbchy/RbcTcsWorld_ECommerceSuite?style=flat-square&color=6d28d9&label=Closed%20PRs)

</div>

---

## 📌 At a Glance

<table>
<tr>
<td width="50%" valign="top">

**🙋 What this is**

An Amazon-inspired online store with the features a real shop needs:
- catalog, cart, and checkout with a delivery address;
- Stripe or mock payment, shipping and returns;
- reviews, wishlist, guest checkout, an address book, size/colour variants and a faceted filter;
- flash deals, invoice PDFs, two-factor sign-in, and a saved-card wallet (tokens only);
- a **store back office** with customers, coupons, multi-warehouse inventory, reports, a payment ledger, and an append-only **audit trail** inspired by 21 CFR Part 11 / ALCOA+.

The application is the *test object*. The real work is the test system around it:

- **unit → API → BDD → UI → phone** tests;
- contract, accessibility, performance, DAST and dependency scans;
- mutation testing;
- a traceability matrix that fails the build when a requirement loses its last test.

It runs on **GitHub Actions and on Jenkins** (Mac). A public demo runs on one free cloud server with HTTPS ([deploy/](deploy/), guide: [ORACLE_CLOUD_BN](docs/deploy/ORACLE_CLOUD_BN.md)).

</td>
<td width="50%" valign="top">

**📍 Quick facts**

| | |
|---|---|
| 🧪 **Tests** | 1,489: 772 backend · 444 API/DB/BDD/UI · 265 Playwright · 8 Appium |
| 🚦 **Gates** | 18 / 18 green on `main` (10 Nov 2026) |
| 📏 **Coverage** | 97.3 % lines · 94.2 % branches |
| 🧬 **Mutation** | 89.3 % of 1,151 mutants killed |
| 📋 **Requirements** | 129 / 129 traced to tests |
| 🐞 **Defects logged** | 29, each with evidence, root cause, fix and regression test |
| 🌍 **UI** | 6 languages · installable PWA · WCAG 2.1 AA |
| 🔁 **CI** | GitHub Actions (5 workflows) + Jenkins on macOS ARM |

</td>
</tr>
</table>

---

## 🚦 Project Status Board

<img src="docs/images/project-status.svg" alt="Project status board: 1,489 automated tests (1,486 passed, 3 skipped by design), 18 of 18 CI gates passed, 97.3 % line coverage, 89.3 % mutation score, 129 of 129 requirements traced" width="100%">

[![CI](https://github.com/rbchy/RbcTcsWorld_ECommerceSuite/actions/workflows/ci.yml/badge.svg)](https://github.com/rbchy/RbcTcsWorld_ECommerceSuite/actions/workflows/ci.yml)
![Last commit](https://img.shields.io/github/last-commit/rbchy/RbcTcsWorld_ECommerceSuite?label=last%20commit)
![Open issues](https://img.shields.io/github/issues/rbchy/RbcTcsWorld_ECommerceSuite?label=open%20issues)
![Open PRs](https://img.shields.io/github/issues-pr/rbchy/RbcTcsWorld_ECommerceSuite?label=open%20PRs)

The board shows the GitHub Actions run of `main` on 23 Oct 2026 (commit `f1a74ed`). The CI badge above is live;
the board is redrawn after a release with [`docs/images/make_readme_cards.py`](docs/images/make_readme_cards.py).

## 🥧 Breakdown

```mermaid
pie showData
    title 1,489 automated tests by level
    "Backend unit + integration" : 772
    "API, DB, BDD, Selenium UI" : 444
    "Playwright (5 browsers/devices)" : 265
    "Appium (Android + iOS)" : 8
```

```mermaid
pie showData
    title 129 traced requirements by area
    "Orders and inventory" : 16
    "Identity and access" : 16
    "Non-functional" : 17
    "Catalog" : 14
    "Storefront UI" : 14
    "Pricing, coupons, payments" : 13
    "Reviews and wishlist" : 11
    "Store admin back office" : 8
    "Fulfilment and returns" : 6
    "Customer e-mails" : 4
    "Audit trail" : 4
    "Cart" : 3
    "Address book" : 3
```

Every requirement links to its tests in the [traceability matrix](docs/qa/TRACEABILITY_MATRIX.md); CI fails if one has none.

```mermaid
xychart-beta
    title "Tests executed per CI job (main, 10 Nov 2026)"
    x-axis ["Backend", "API/DB/BDD/UI", "Playwright", "Docker smoke", "UI Firefox", "UI Edge", "Android", "iOS"]
    y-axis "Tests" 0 --> 800
    bar [772, 444, 265, 63, 59, 59, 4, 4]
```

<sub>Firefox and Edge rerun the 59 Selenium UI tests in other browsers, and the Docker job smoke-tests the containers. These runs are not counted again in the 1,489.</sub>

```mermaid
xychart-beta
    title "Test-quality metrics (bars) vs the gate that fails the build (line), %"
    x-axis ["Line coverage", "Branch coverage", "Mutation score"]
    y-axis "Percent" 80 --> 100
    bar [97.3, 94.2, 89.3]
    line [97, 88, 84]
```

## 📈 GitHub Stats

<img src="docs/images/github-stats.svg" alt="GitHub stats: 217 commits on 18 active days since 4 October 2026, 50 merged pull requests, 0 open, 354 CI workflow runs, about 40k lines of code; Java 74.3 %, JavaScript 15.9 %, TypeScript 3.9 %, CSS 2.4 %, other 3.5 %" width="100%">

![Commits](https://img.shields.io/github/commit-activity/t/rbchy/RbcTcsWorld_ECommerceSuite?label=commits)
![Commit activity](https://img.shields.io/github/commit-activity/m/rbchy/RbcTcsWorld_ECommerceSuite?label=commits%20this%20month)
![Merged PRs](https://img.shields.io/github/issues-pr-closed/rbchy/RbcTcsWorld_ECommerceSuite?label=closed%20PRs)
![Top language](https://img.shields.io/github/languages/top/rbchy/RbcTcsWorld_ECommerceSuite)
![Code size](https://img.shields.io/github/languages/code-size/rbchy/RbcTcsWorld_ECommerceSuite)
![Stars](https://img.shields.io/github/stars/rbchy/RbcTcsWorld_ECommerceSuite?style=social)

The badges update live from GitHub; the card above is a snapshot of the same day as the status board.

---

## Reviewing this in 5 minutes

1. **Latest build:** the screenshot below (Jenkins build 54, all gates passed), or the
   [GitHub Actions runs](https://github.com/rbchy/RbcTcsWorld_ECommerceSuite/actions).
2. **Live reports:** [all reports](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/reports/) · [Allure](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/) · [Cucumber](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/cucumber/) ·
   [JaCoCo coverage](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/coverage/) ·
   [PIT mutation](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/mutation/) ·
   [Playwright](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/playwright/)
3. **Defects the tests actually found:** [Defect Reports](docs/qa/DEFECT_REPORTS.md) - 29 entries, each with
   evidence, root cause, fix and regression test.
4. **How release decisions are made:** [Test Strategy](docs/qa/TEST_STRATEGY.md) ·
   [Risk Register](docs/qa/RISK_REGISTER.md) · [Test Summary Report (GO / NO-GO)](docs/qa/TEST_SUMMARY_REPORT.md)
   · [UAT Plan](docs/qa/UAT_PLAN.md) (10 tasks for non-tester users, each traced to its requirements)
5. **Code to read first:** the [pipeline](.github/workflows/ci.yml), the
   [flash-sale concurrency test](automation/src/test/java/com/rbctcsworld/ecommerce/qa/tests/api/InventoryConcurrencyTest.java),
   the [API contract test](automation/src/test/java/com/rbctcsworld/ecommerce/qa/tests/api/ApiContractTest.java) and the
   [access-control matrix](automation/src/test/java/com/rbctcsworld/ecommerce/qa/tests/security/AccessControlMatrixTest.java).
6. **Why it is built this way:** [Architecture Decision Records](docs/adr/) - nine short records, each with the alternatives
   and the price paid. Commands: `make help` · [Contributing](CONTRIBUTING.md) · [MIT License](LICENSE)

![QA Reports page of Jenkins build 54](docs/images/jenkins-qa-reports.png)
*One page per Jenkins build with every gate, generated by [`ci/qa_dashboard.py`](ci/qa_dashboard.py).*

---

## 🏗️ System Architecture

```mermaid
flowchart LR
    subgraph clients["Clients"]
        B["Browser / PWA<br/>6 languages"]
        PH["Phones<br/>Android Chrome · iOS Safari"]
        ADM["Store admin<br/>back office"]
    end
    subgraph app["Application"]
        FE["React + Vite storefront<br/>nginx"]
        API["Spring Boot 4.1 API :8081<br/>Spring Security + JWT · OpenAPI"]
    end
    subgraph data["Data and services"]
        PG[("PostgreSQL 16<br/>Flyway · CHECK constraints")]
        RD[("Redis<br/>login lockout counters")]
        MP["SMTP / Mailpit<br/>order + reset e-mails"]
        PAY["Payments<br/>mock gateway · Stripe test mode"]
    end
    B --> FE
    PH --> FE
    ADM --> FE
    FE -->|REST + Bearer token| API
    API --> PG
    API --> RD
    API -->|transactional outbox| MP
    API --> PAY
    CADDY["Caddy (public demo)<br/>HTTPS, only 80/443"] -.-> FE
```

### 📦 Order lifecycle (the state machine the tests guard)

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PLACED
    PLACED --> PAID : pay
    PLACED --> CANCELLED : cancel
    PAID --> SHIPPED : ship
    PAID --> CANCELLED : cancel (refund)
    SHIPPED --> DELIVERED : deliver
    DELIVERED --> RETURN_REQUESTED : request return
    RETURN_REQUESTED --> RETURNED : approve (refund)
    RETURN_REQUESTED --> DELIVERED : reject
    RETURNED --> [*]
    CANCELLED --> [*]
```

Every other move is rejected with **409** (`OrderStatus.ALLOWED`), and a DB CHECK constraint backs it up. DEF-013 was a test that *looked* like it guarded "a paid order cannot be paid again" but would have passed even if the card were charged. Mutation testing found it.

### 🛒 Checkout through the API

```mermaid
sequenceDiagram
    autonumber
    participant C as Customer (UI / test)
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant PAY as Payment gateway
    participant MAIL as Mail outbox
    C->>API: POST /api/auth/login
    API-->>C: access + refresh token
    C->>API: POST /api/cart/items {product, qty}
    C->>API: POST /api/checkout/quote {address, coupon}
    API-->>C: subtotal, discount, tax, shipping, total
    C->>API: POST /api/orders {coupon, shipping address}
    API->>DB: atomic conditional UPDATE stock (never below 0) + order PLACED
    API-->>C: 201 PLACED
    C->>API: POST /api/orders/{id}/pay {card}
    API->>PAY: charge (approved / declined / insufficient funds)
    API->>DB: order PAID, payment ledger row, audit entry
    API->>MAIL: order confirmation (outbox)
    API-->>C: 200 PAID, or 400 bad card / 402 declined / 409 not payable
```

---

## Quality gates

A gate fails the build. Nothing is "reported only" unless the table says so.

| # | Gate | Threshold | Current | Tool |
|---|---|---|---|---|
| 1 | Backend unit + integration | 100 % pass | 772 / 772 | JUnit 5, Mockito, H2 + Flyway |
| 2 | API, database, BDD, UI | 100 % pass | 444 / 444 | REST Assured, JDBC, Cucumber 7, Selenium 4, Mailpit |
| 3 | Line / branch coverage | >= 97 % / >= 88 % | 97.3 % / 94.2 % | JaCoCo |
| 4 | Mutation score | >= 84 % | 89.3 % (test strength 94.7 %) | PIT |
| 5 | API contract, consumer side | every response matches its schema | 12 strict JSON Schemas | json-schema-validator |
| 6 | API contract, provider side | no breaking change vs. approved baseline | 0 (gate has its own self-test) | openapi-diff |
| 7 | Accessibility | 0 serious / critical | 0 on 23 pages | axe-core, WCAG 2.1 A + AA |
| 8 | Cross-browser | 100 % pass | 59 / 59 in Firefox and Edge | Selenium, CI matrix |
| 9 | Performance | SLO thresholds (p95, error rate, no overselling) | p95 16-72 ms, 0 % errors; JMeter 100 users p95 77 ms | k6, JMeter (manual workflow) |
| 10 | DAST | 0 Medium / High | 0 | OWASP ZAP API scan |
| 11 | Dependencies | 0 critical (CVSS >= 9) and 0 open exceptions; lower severities are reported as warnings | 0 critical; 0 of any severity after the 10 Nov upgrade (complete SBOMs, self-test passed) | OSV-Scanner on CycloneDX SBOMs + npm lockfile |
| 12 | Containers | stack healthy, smoke tests pass | 63 / 63 | Docker Compose |
| 13 | Traceability | every requirement has a test | 129 / 129 | `docs/qa/check_rtm.py` |
| 14 | Pipeline definition | Jenkinsfile valid | validated by a real Jenkins | declarative linter |
| 15 | Public demo stack | HTTPS, only 80/443 open, demo safeguards, nightly reset | 15 / 15 smoke checks | Docker Compose, Caddy, `deploy/smoke.sh` (GitHub Actions only: needs ports 80/443) |
| 16 | Playwright (TypeScript) | 100 % pass, visual baselines match | 262 / 262 + 3 skipped by design (Chromium, Firefox, WebKit, Pixel 7, iPhone 14) | Playwright, axe-core |
| 17 | Phone: Android | 100 % pass (one rerun, reported as flaky) | 4 / 4 in Chrome on Android 14 | Appium 3 + UiAutomator2, Android emulator |
| 18 | Phone: iOS | 100 % pass (one rerun, reported as flaky) | 4 / 4 in Safari on the newest iPhone simulator | Appium 3 + XCUITest, macOS runner |

Thresholds are set just under the measured value on purpose: they stop regressions without inviting
tests that exist only to move a number.

---

## Test architecture

```mermaid
flowchart TB
  subgraph fast["Fast, every push (seconds)"]
    U["Unit + integration<br/>772 tests"]
    M["Mutation testing<br/>1151 mutants"]
  end
  subgraph service["Against the running app (PostgreSQL)"]
    A["API tests<br/>REST Assured clients"]
    C["Contract<br/>JSON Schema + OpenAPI diff"]
    D["Database checks<br/>read-only JDBC"]
    B["BDD<br/>52 Cucumber scenarios"]
    S["Security<br/>JWT, RBAC/IDOR matrix, injection"]
  end
  subgraph ui["Browser and phone (fewest tests)"]
    W["59 Selenium UI tests<br/>Chrome, Firefox, Edge, Safari"]
    P["Playwright<br/>5 browsers and devices"]
    X["Accessibility<br/>axe-core, 23 pages"]
    R["Appium<br/>Android Chrome, iOS Safari"]
  end
  subgraph nonfunc["Non-functional"]
    K["k6: smoke, flash sale, catalog"]
    Z["ZAP + OSV"]
  end
  subgraph people["People"]
    UAT["User acceptance<br/>10 tasks, non-tester users"]
  end
  fast --> service --> ui --> people
  service --> nonfunc
```

Design choices behind it:

- **The pyramid is real, not drawn.** Business rules (prices, tax, coupons, order state machine, return
  window) are tested at the unit level; the browser tests only check what a customer sees. Preconditions
  for UI tests (a paid order, a delivered order, a wishlist) are created through the API, so a UI test
  never clicks through five pages to reach the one it is testing.
- **Mutation testing over coverage.** Coverage was 96 % when PIT showed that "a paid order cannot be paid
  again" passed even with the charge happening (`anyLong()` does not match `null`, DEF-013).
  Coverage says the code ran; mutation testing says a test would notice if it were wrong.
- **Contracts from both sides.** Consumer schemas use `additionalProperties: false`, so a leaked field
  (customer e-mail in public tracking, a full card number) fails a test. The provider side diffs the live
  OpenAPI document against an approved baseline; changing the contract is a deliberate, reviewed step.
- **Concurrency is tested, not assumed.** 300 buyers race for 50 units; exactly 50 orders succeed and
  stock ends at 0 (atomic conditional `UPDATE` plus a CHECK constraint). k6 repeats this in every build.
- **Stable locators and waits.** `data-testid` attributes only, explicit waits only, no `Thread.sleep`.
  A flaky test is treated as a defect and gets a report (DEF-008 to DEF-010, DEF-019).
- **Risk drives scope.** Each requirement is linked to a risk and a test in the
  [traceability matrix](docs/qa/TRACEABILITY_MATRIX.md); CI fails if a requirement loses its last test.

---

## Pipeline

```mermaid
flowchart LR
  push[git push / PR] --> unit[Unit + integration<br/>JaCoCo gate]
  push --> pit[PIT mutation<br/>score gate]
  push --> lint[Jenkinsfile lint<br/>real Jenkins]
  unit --> e2e[API + DB + BDD + UI<br/>contract, a11y, k6]
  unit --> xb[UI in Firefox + Edge<br/>matrix]
  unit --> docker[Docker images<br/>44 smoke tests]
  unit --> sec[ZAP + OSV<br/>OpenAPI breaking-change gate]
  e2e --> pages[GitHub Pages<br/>Allure, Cucumber, coverage, PIT]
  pit --> pages
```

The same stages run in a [Jenkinsfile](Jenkinsfile) with parameters (`BROWSER`, `RUN_UI`, `RUN_MUTATION`,
`RUN_SECURITY`). GitHub jobs are pinned to `ubuntu-24.04`; a weekly [runner canary](.github/workflows/runner-canary.yml)
runs the same pipeline on the next image first. Browser tests get one rerun, and a pass on the rerun is reported
as FLAKY with a count instead of disappearing. Two CI systems on two CPU architectures (Linux x86 and macOS ARM) have already paid for
themselves: four defects appeared on only one of them (DEF-014, DEF-015, DEF-019, DEF-020).

---

## What the tests found

All entries are in [Defect Reports](docs/qa/DEFECT_REPORTS.md). A selection:

| ID | Found by | Defect | Outcome |
|---|---|---|---|
| DEF-003 / 004 | Attack tests | No brute-force lockout; login timing revealed which e-mails exist | Lockout + constant-time login |
| DEF-005 | OSV scan | 21 vulnerable libraries | 0, with a CVE gate in CI |
| DEF-007 | k6 report | Catalog returned every product in one response | Pagination, response -91 %, p95 22 -> 9 ms |
| DEF-013 | PIT | A test that could never fail | Fixed + 30 targeted tests, score 65.5 -> 84.9 % |
| DEF-016 | Selenium | Fast typing showed results of an older search | Request guard + debounce |
| DEF-017 | ZAP + API test | `?=x` query string returned 500 | 400, regression test with a raw HTTP client |
| DEF-018 | axe-core (written first) | All 8 pages failed WCAG 2.1 AA (contrast, labels, page language) | 0 serious / critical |
| DEF-020 | Jenkins on macOS | Edge / Firefox drivers could not be downloaded | Selenium 4.25 -> 4.49 |
| DEF-021 | Runner canary | The dependency scan itself went blind on a rate limit and missed a CVSS 9.1 library | SBOM-based scan with completeness check and self-test |

```mermaid
pie showData title 29 logged defects by severity
    "Critical" : 1
    "High" : 8
    "Medium" : 15
    "Low" : 5
```

```mermaid
pie showData title 29 logged defects by where the fault was
    "Product (the shop)" : 14
    "Test code" : 8
    "Test infrastructure / reporting" : 7
```

<sub>Half of the defects were in the tests or the pipeline, not the shop. Each one would have hidden a real problem (a test that could never fail, a scan that went blind, a report that said "passed" for a failed build), so each got the same report, root cause and regression test as a product bug.</sub>

Two critical Spring CVEs had no patch for Spring Boot 3.5. They were accepted for a limited time (reason,
expiry date, guard test) and then removed by upgrading to Spring Boot 4.1 on a branch. The gates caught
three regressions before the merge (a new Tomcat CVE, DEF-016, DEF-017).

---

## Known limitations

Stating these is part of the test report, not a footnote.

- **Payments run on the mock gateway by default.** Stripe (`PAYMENT_PROVIDER=stripe`) is contract-tested
  against stripe-mock on every build; real test-mode runs need a `STRIPE_TEST_SECRET_KEY` secret. The card
  form is the shop's own (a production shop would use Stripe Elements), and there are no Stripe webhooks yet.
- **Order e-mails are English plain text** and the SMTP channel has no authentication or TLS yet (enough
  for Mailpit; a real mail service needs both).
- **Tokens live in the browser's localStorage.** An XSS bug could read them; the strict CSP and React's
  escaping reduce that risk. HttpOnly cookies would remove it but bring CSRF handling with them.
- **A logged-out access token stays valid until it expires** (at most 15 minutes). The refresh token is
  revoked at once; a denylist for access tokens was judged not worth a database call on every request.
- **Automated accessibility rules find part of the problems.** Keyboard-only, screen-reader and zoom
  checks are manual items on the release checklist.
- **Safari runs only on a Mac** (Jenkins `BROWSER=safari`, no headless mode). Phones are an Android emulator and an
  iOS simulator in CI (Appium), not physical devices; the guide shows how to plug in a real phone.
- **k6 numbers come from CI runners** and are for comparison between builds, not capacity planning.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Spring 7, Jackson 3, Tomcat 11), Spring Security + JWT, PostgreSQL 16, Flyway |
| Storefront | React + Vite |
| Test automation | JUnit 5, REST Assured, Selenium 4 (Page Objects), Cucumber 7 + PicoContainer, axe-core, JDBC |
| Test quality | JaCoCo, PIT |
| Non-functional | k6, OWASP ZAP, OSV-Scanner, openapi-diff |
| Reporting | Allure (trend history, HTTP attachments, failure categories), Cucumber HTML, per-build QA dashboard |
| Delivery | Docker (multi-stage, non-root), Docker Compose, GitHub Actions, Jenkins |

---

## Run it

**Whole application, one command** (PostgreSQL + Redis + Mailpit + backend + storefront):

```bash
docker compose --profile app up -d --build        # storefront http://localhost:5173, API http://localhost:8081
docker compose --profile app down                 # add -v to delete the database
```

Or double-click the launcher in `desktop/` (Mac `.command`, Windows `.bat`, Linux `.sh`); it starts Docker if needed
and opens the store. In Chrome or Edge, **Install app** in the top bar adds it as a desktop app.

**Development** (backend from Eclipse or Maven, live-reloading UI):

```bash
docker compose up -d                              # PostgreSQL only
cd backend && mvn spring-boot:run                 # API on 8081; Flyway creates tables and demo data
cd frontend && npm install && npm run dev         # UI on 5173
```

| | |
|---|---|
| Backend API | http://localhost:8081 (8080 stays free for Jenkins) · OpenAPI description at `/v3/api-docs` |
| PostgreSQL | localhost:5432, database / user / password `ecommerce` |
| Admin (dev only) | `admin@rbctcsworld.com` / `Admin@12345`, override with `ADMIN_EMAIL`, `ADMIN_PASSWORD` |
| Coupons | `WELCOME10`, `SAVE5`, `EXPIRED20`, `FUTURE15`, `DISABLED` |
| Test cards | `4242424242424242` approved · `4000000000000002` declined · `4000000000009995` insufficient funds |

## Run the tests

`mvn test` in the root folder runs the backend tests only. The automation suite needs the backend on 8081
(and the storefront on 5173 for `ui` and `a11y`).

```bash
mvn -f backend/pom.xml verify                                   # unit + integration + coverage gate
mvn -f backend/pom.xml -Pmutation -Djacoco.skip=true test-compile org.pitest:pitest-maven:mutationCoverage   # PIT
mvn -f automation/pom.xml clean test -DexcludedGroups=ui,mobile        # API, contract, DB, BDD, security
mvn -f automation/pom.xml clean test -Dgroups=ui                # UI + accessibility, headless Chrome
mvn -f automation/pom.xml clean test -Dgroups=ui -Dbrowser=edge # chrome | firefox | edge | safari
mvn -f automation/pom.xml clean test -Dcucumber.filter.tags="@flagship"   # register-to-refund journey
mvn -f automation/pom.xml allure:serve                          # open the Allure report
```

Other tags: `contract`, `a11y`, `db`, `security`, `concurrency`, `payment`, `returns`, `notification`.
E-mails the shop sends land in Mailpit: http://localhost:8025.
k6 scripts and how to run them: [performance](docs/modules/PERFORMANCE_K6_BN.md).

---

## Repository layout

```text
backend/        Spring Boot API, unit + integration tests, PIT profile
frontend/       React storefront + store-admin panel, 6 languages, installable PWA (data-testid everywhere)
desktop/        One-click launchers for Mac, Windows and Linux
automation/     API clients, Page Objects, Cucumber features, contract schemas, test data fixtures
performance/    k6 scripts with SLO thresholds
security/       ZAP rules, SBOM + OSV scan with self-test, version check
ci/             QA dashboard generator for Jenkins
docs/qa/        Test strategy, plan, risk register, RTM, defect reports, test summary report
docs/modules/   Module guides (Bangla), one per feature or test layer
```

## Documentation

**QA documents (English):** [Strategy](docs/qa/TEST_STRATEGY.md) · [Plan](docs/qa/TEST_PLAN.md) ·
[Risks](docs/qa/RISK_REGISTER.md) · [Traceability (129 requirements)](docs/qa/TRACEABILITY_MATRIX.md) ·
[Defects (29)](docs/qa/DEFECT_REPORTS.md) · [Test Summary Report](docs/qa/TEST_SUMMARY_REPORT.md) · [UAT Plan](docs/qa/UAT_PLAN.md) · [UAT Summary](docs/qa/UAT_SUMMARY.md)

**Help (Bangla), start here:** [HELP_BN](docs/HELP_BN.md) - running on every device, menus, login, admin, tests, reports, Jenkins, troubleshooting.

**Module guides (Bangla):**

| Area | Guide |
|---|---|
| Foundation, cart, orders + inventory, checkout + payment, fulfilment + returns, reviews + wishlist | [Module 0](docs/modules/MODULE_00_FOUNDATION_BN.md) · [1](docs/modules/MODULE_01_CART_BN.md) · [2](docs/modules/MODULE_02_ORDERS_INVENTORY_BN.md) · [3](docs/modules/MODULE_03_CHECKOUT_PAYMENT_BN.md) · [4](docs/modules/MODULE_04_FULFILLMENT_RETURNS_BN.md) · [5](docs/modules/MODULE_05_REVIEWS_WISHLIST_BN.md) |
| Storefront and UI tests, cross-browser | [FRONTEND_UI_TESTS_BN](docs/modules/FRONTEND_UI_TESTS_BN.md) |
| Menu, store-admin panel, six languages, help, desktop | [STOREFRONT_UI_BN](docs/modules/STOREFRONT_UI_BN.md) |
| Login sessions: refresh tokens, Redis login lockout | [AUTH_SESSIONS_BN](docs/modules/AUTH_SESSIONS_BN.md) |
| Forgot password: reset link by e-mail | [PASSWORD_RESET_BN](docs/modules/PASSWORD_RESET_BN.md) |
| Audit trail (append-only, hash chain; inspired by Part 11 / ALCOA+) | [AUDIT_TRAIL_BN](docs/modules/AUDIT_TRAIL_BN.md) |
| Store admin back office: banner, sub-menus, customers, coupons, reports, payments, advanced search | [ADMIN_BACK_OFFICE_BN](docs/modules/ADMIN_BACK_OFFICE_BN.md) |
| Multi-warehouse inventory: allocation, split shipments, transfers, no overselling | [MULTI_WAREHOUSE_BN](docs/modules/MULTI_WAREHOUSE_BN.md) |
| Delivery address at checkout | [SHIPPING_ADDRESS_BN](docs/modules/SHIPPING_ADDRESS_BN.md) |
| Stripe payments (3 test layers), order e-mails (transactional outbox, Mailpit) | [PAYMENTS_NOTIFICATIONS_BN](docs/modules/PAYMENTS_NOTIFICATIONS_BN.md) |
| API contract and accessibility | [API_CONTRACT_A11Y_BN](docs/modules/API_CONTRACT_A11Y_BN.md) |
| Performance (k6) | [PERFORMANCE_K6_BN](docs/modules/PERFORMANCE_K6_BN.md) |
| Security | [SECURITY_BN](docs/modules/SECURITY_BN.md) |
| Mutation testing | [MUTATION_BN](docs/modules/MUTATION_BN.md) |
| CI/CD, Docker, Jenkins | [CICD_BN](docs/modules/CICD_BN.md) |
| Allure reporting | [ALLURE_REPORT_BN](docs/modules/ALLURE_REPORT_BN.md) |
| Spring Boot 3.5 -> 4.1 upgrade | [SPRING_BOOT_4_BN](docs/modules/SPRING_BOOT_4_BN.md) |
| QA documents explained | [QA_DOCUMENTS_BN](docs/modules/QA_DOCUMENTS_BN.md) |

**Eclipse:** File -> Import -> Maven -> Existing Maven Projects -> select this folder.
After a pull: right-click the project -> Maven -> Update Project.

---

No test suite proves the absence of defects. This one is built to find the expensive ones early,
show the evidence, and make the release decision explicit.

---

## 🔗 Related Projects

| Project | Stack |
|---|---|
| 💊 [PharmaPack QMS — Serialization & Line Control](https://github.com/rbchy/RbcTcsWorld_PharmaPackQMS_Serialization) | Spring Boot · React · L3 serialization · 78 / 78 in CI |
| 💊 [PharmaPack QMS](https://github.com/rbchy/RbcTcsWorld_PharmaPackQMS) | Spring Boot QMS: AQL, deviations, CAPA · CI |
| ☕ [Bhojanaloy POS](https://github.com/rbchy/BhojanAloy-Food-Coffee-Shop-POS-System) | Java Swing + Spring Boot · MySQL · 76 / 76 in CI |
| 🏔️ [NorthFace × Automation Exercise BDD](https://github.com/rbchy/NorthFace_AutomationExercise-BDD-Test-Automation) | Selenium · Cucumber · RestAssured · CI |
| 📱 [WDIO Native Demo App – Appium](https://github.com/rbchy/WebdriverIO-Native-DemoApp-MobileTestAutomationJavaAppiumTestNG) | Appium 2 · TestNG · Android emulator in CI |

<sub>All projects: [github.com/rbchy](https://github.com/rbchy)</sub>

---

## 👤 Author

<div align="center">

**Ranajit Baran Chowdhury (RB Chowdhury)**
QA Automation Engineer / SDET · West Norriton, Pennsylvania, USA

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/rbchy/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/rbchy)
[![Website](https://img.shields.io/badge/Website-rbchy.github.io%2Frbchy-0d9488?style=for-the-badge&logo=githubpages&logoColor=white)](https://rbchy.github.io/rbchy/)
[![Portfolio](https://img.shields.io/badge/Portfolio-Website-blueviolet?style=for-the-badge&logo=wix&logoColor=white)](https://rbc6543.wixsite.com/rbc-portfolio)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:chyranajit@gmail.com)

<sub>MIT licensed — see [LICENSE](LICENSE).</sub>

⭐ *If this project helps you, a star on the repo is appreciated.*

</div>
