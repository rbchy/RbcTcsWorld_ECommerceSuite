# RbcTcsWorld E-Commerce QA Automation Platform

[![CI](https://github.com/rbchy/RbcTcsWorld_ECommerceSuite/actions/workflows/ci.yml/badge.svg)](https://github.com/rbchy/RbcTcsWorld_ECommerceSuite/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue) ![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-green) ![Tests](https://img.shields.io/badge/tests-292-brightgreen)
[![Allure Report](https://img.shields.io/badge/Allure-live%20report-orange)](https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/)

**292 automated tests** (172 backend unit/integration + 120 API, database, concurrency and BDD) covering a full
register-to-refund journey.

An Amazon-inspired (not a copy) e-commerce platform built QA-first: a Spring Boot backend plus a
layered automation framework (unit, integration, API, BDD, UI, security).

## Modules

| # | Module | Status | Docs (Bangla) |
|---|---|---|---|
| 0 | Foundation: roles, JSON errors, correct status codes, seed data | Done | [docs/modules/MODULE_00_FOUNDATION_BN.md](docs/modules/MODULE_00_FOUNDATION_BN.md) |
| 1 | Shopping cart | Done | [docs/modules/MODULE_01_CART_BN.md](docs/modules/MODULE_01_CART_BN.md) |
| 2 | Orders + inventory: atomic stock reservation, cancel, audit trail, admin API | Done | [docs/modules/MODULE_02_ORDERS_INVENTORY_BN.md](docs/modules/MODULE_02_ORDERS_INVENTORY_BN.md) |
| 3 | Checkout: price breakdown (discount, shipping, 6% tax), coupons, mock payment gateway, refunds | Done | [docs/modules/MODULE_03_CHECKOUT_PAYMENT_BN.md](docs/modules/MODULE_03_CHECKOUT_PAYMENT_BN.md) |
| 4 | Order state machine, shipping, tracking timeline (incl. public tracking), returns with refund/restock rules | Done | [docs/modules/MODULE_04_FULFILLMENT_RETURNS_BN.md](docs/modules/MODULE_04_FULFILLMENT_RETURNS_BN.md) |
| - | Allure reporting: Epics/Features, HTTP attachments, failure categories, trend history, published on GitHub Pages | Done | [docs/modules/ALLURE_REPORT_BN.md](docs/modules/ALLURE_REPORT_BN.md) |
| 5 | Product reviews + ratings, wishlist | Planned | |

## Ports and accounts (development)

| Thing | Value |
|---|---|
| Backend API | http://localhost:8081 (8080 is left free for Jenkins) |
| Frontend | http://localhost:5173 (proxies `/api` to 8081) |
| PostgreSQL | localhost:5432, db/user/password `ecommerce` |
| Seeded admin | `admin@rbctcsworld.com` / `Admin@12345` (override with `ADMIN_EMAIL`, `ADMIN_PASSWORD`) |
| Seeded coupons | `WELCOME10` (10%, once per customer), `SAVE5` ($5, min $25), `EXPIRED20`, `FUTURE15`, `DISABLED` |
| Test cards | `4242424242424242` approved, `4000000000000002` declined, `4000000000009995` insufficient funds |

## Run locally

```bash
docker compose up -d                              # PostgreSQL
cd backend && mvn spring-boot:run                 # API on 8081, Flyway creates tables + demo products
cd frontend && npm install && npm run dev         # UI on 5173
```

## Tests

> The root build (`mvn test` in this folder) runs **backend tests only**. The automation suite needs the
> backend running on 8081 first (and the frontend on 5173 for `ui` tests), so run it explicitly as below,
> or with `mvn test -Pe2e`. Otherwise every API test fails with `Connection refused`.

```bash
mvn -f backend/pom.xml test                       # unit + integration (H2, no Docker needed)
mvn -f automation/pom.xml test -DexcludedGroups=ui   # API + BDD against the running backend
mvn -f automation/pom.xml test -Dgroups=ui -Dheadless=false   # UI (backend + frontend running)
mvn -f automation/pom.xml test -Dgroups=db         # database validation (PostgreSQL running)
mvn -f automation/pom.xml test -Dgroups=concurrency   # flash-sale overselling test
mvn -f automation/pom.xml test -Dgroups=payment     # mock payment gateway
mvn -f automation/pom.xml test -Dgroups=security    # IDOR, RBAC, card-data storage
mvn -f automation/pom.xml test -Dgroups=returns     # return decision table
mvn -f automation/pom.xml test -Dcucumber.filter.tags="@flagship"   # full register-to-refund journey
```

Reports:
- **Allure** (graphs, Epics/Features, every HTTP request/response, Gherkin steps, screenshots on UI failure):
  `mvn -f automation/pom.xml allure:serve` after a test run (use `mvn clean test` so old results do not mix in).
  CI publishes it on every push to `main`: **https://rbchy.github.io/RbcTcsWorld_ECommerceSuite/**
- Cucumber HTML: `automation/target/cucumber-report.html`; raw JUnit XML: `automation/target/surefire-reports`.

## Eclipse

File → Import → Maven → Existing Maven Projects → select this folder → Finish.
After pulling new modules: right-click project → Maven → Update Project (Alt+F5).

## Architecture

- Backend: Spring Boot 3.5, Java 21, PostgreSQL, Flyway, Spring Security + JWT (roles CUSTOMER / ADMIN).
- Automation: JUnit 5, REST Assured (API clients), JDBC (read-only DB validation), Selenium (Page Objects), Cucumber + PicoContainer.
- Frontend: React + Vite.
- CI: GitHub Actions (backend tests, then API automation against a live backend + PostgreSQL), Jenkinsfile.

No automation suite can guarantee finding every defect; the goal is risk-based, layered coverage.
