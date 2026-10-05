# Performance tests (k6)

| Test | What it answers | Default shape | Run |
|---|---|---|---|
| `smoke.js` | Do the scripts and the system work at all? (every CI build) | 1 browser + 1 buyer, 30 s | `k6 run performance/tests/smoke.js` |
| `load.js` | Do we meet the SLOs at a normal busy peak? | 40 browsers + 10 buyers, 2 m ramp, 5 m hold | `k6 run performance/tests/load.js` |
| `stress.js` | Where is the breaking point, and does it recover? | steps to 200 + 50 users, aborts above 10 % errors | `k6 run performance/tests/stress.js` |
| `spike.js` | Does a sudden rush (5 to 250 users in 10 s) break it? | spike, hold 1 m, recovery | `k6 run performance/tests/spike.js` |
| `soak.js` | Does it degrade over time (leaks, pools, growing tables)? | 20 + 5 users for 30 m | `k6 run -e DURATION=1h performance/tests/soak.js` |
| `flash-sale.js` | Is it correct under concurrency? Exactly STOCK orders, no overselling | 100 buyers, 20 units, all at once | `k6 run performance/tests/flash-sale.js` (every CI build) |

Run from the repository root with the backend on `http://localhost:8081` (change it with `-e BASE_URL=...`).

## SLOs (thresholds) - `lib/config.js`
- read requests (catalog, search, product, reviews): p95 < 500 ms
- write requests (cart, order, payment): p95 < 1000 ms
- complete purchase (cart, quote, order, pay; think time excluded): p95 < 3000 ms
- failed HTTP requests < 1 %, functional checks > 99 %, business errors (wrong totals) < 1 %

A failed threshold makes k6 exit with code 99, so CI turns red.

## Reports
Every run writes `performance/reports/<test>-report.html` (no internet needed) and `<test>-summary.json`.
In CI they are attached to the run as the `k6-reports` artifact. The heavy tests run from
**Actions > Performance (manual) > Run workflow**.

## Layout
```
lib/config.js      URLs, credentials, SLOs, think time
lib/api.js         one function per endpoint, tagged by name (ids grouped) and kind (read/write)
lib/journeys.js    browse (anonymous) and buy (register, cart, quote, order, pay) + business metrics
lib/thresholds.js  shared pass/fail rules + per-endpoint breakdown
lib/report.js      console + HTML + JSON summary
tests/*.js         the six tests above
```
