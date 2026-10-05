// Environment settings. Override any value on the command line:
//   k6 run -e BASE_URL=http://localhost:8081 -e DURATION=10m performance/tests/load.js
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
export const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@rbctcsworld.com';
export const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'Admin@12345';
export const PASSWORD = 'Password1!';
export const CARD_OK = '4242424242424242';
export const REPORT_DIR = __ENV.REPORT_DIR || 'performance/reports';

/** Service level objectives used by every test (stress / spike relax some of them on purpose). */
export const SLO = {
  browseP95: 500,      // ms - catalog, search, product page, reviews
  writeP95: 1000,      // ms - cart, order, payment (they lock rows and write audit data)
  journeyP95: 3000,    // ms - complete purchase: cart -> quote -> order -> pay
  errorRate: 0.01,     // < 1% failed HTTP requests
  checks: 0.99,        // > 99% of functional checks pass
};

/** "Think time" between user actions so virtual users behave like people, not like a loop. */
export function think(min = 1, max = 3) {
  return min + Math.random() * (max - min);
}
