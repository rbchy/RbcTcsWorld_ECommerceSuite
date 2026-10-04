# QA Test Strategy

## Layers
1. Unit tests for business logic.
2. API contract and negative tests using REST Assured.
3. UI end-to-end tests using Selenium Page Objects.
4. Cucumber BDD for business-readable scenarios.
5. Database validation through JDBC/integration tests.
6. Performance tests with a future JMeter/Gatling suite.
7. CI gates for build, test and artifact publishing.

## Core scenarios
Authentication, product CRUD, search, authorization, cart, checkout, order lifecycle, payment failure/retry, inventory constraints, coupon validation, shipping, return/refund and audit events.
