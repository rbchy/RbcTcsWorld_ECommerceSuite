// FLASH SALE (correctness under concurrency): BUYERS customers, each with 1 unit in the cart,
// press "Place order" at the same moment for a product that has only STOCK units.
// A fast system that oversells is worse than a slow one, so this test checks the INVARIANTS:
//   exactly STOCK orders succeed (201), all others get 409 "insufficient stock",
//   nothing else happens (no 500, no timeout), and the stock ends at exactly 0 - never negative.
//   k6 run performance/tests/flash-sale.js
//   k6 run -e BUYERS=300 -e STOCK=50 performance/tests/flash-sale.js
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';
import * as api from '../lib/api.js';
import { endpointBreakdown, TREND_STATS } from '../lib/thresholds.js';
import { buildSummary } from '../lib/report.js';

const BUYERS = Number(__ENV.BUYERS || 100);
const STOCK = Number(__ENV.STOCK || 20);

const created = new Counter('flash_orders_created');      // 201
const soldOut = new Counter('flash_orders_rejected');      // 409
const unexpected = new Counter('flash_unexpected_status'); // anything else
const oversold = new Counter('flash_oversold_units');      // stock < 0 or more orders than stock
const stockLeft = new Counter('flash_stock_left');

export const options = {
  setupTimeout: '5m',
  scenarios: {
    // every VU runs exactly once, all start together
    rush: { executor: 'per-vu-iterations', vus: BUYERS, iterations: 1, maxDuration: '2m' },
  },
  thresholds: {
    ...endpointBreakdown(),            // first, so the real rule below for POST /api/orders replaces its placeholder
    flash_orders_created: [`count==${STOCK}`],
    flash_orders_rejected: [`count==${BUYERS - STOCK}`],
    flash_unexpected_status: ['count==0'],
    flash_oversold_units: ['count==0'],
    flash_stock_left: ['count==0'],
    'http_req_duration{name:POST /api/orders}': ['p(95)<3000'],
  },
  summaryTrendStats: TREND_STATS,
};

/** Product with STOCK units, and BUYERS customers who each already have 1 unit in the cart. */
export function setup() {
  if (BUYERS <= STOCK) throw new Error('BUYERS must be greater than STOCK, otherwise nothing is tested');
  const admin = api.adminToken();
  const productId = api.createProduct(admin, 'Flash Sale Console', 299.0, STOCK);
  const buyers = [];
  for (let i = 0; i < BUYERS; i++) {
    const c = api.newCustomer('flash');
    if (!c) throw new Error('registration failed during setup');
    const r = api.addToCart(c.token, productId, 1);
    if (r.status !== 201) throw new Error(`add to cart failed: ${r.status} ${r.body}`);
    buyers.push(c.token);
  }
  return { productId, buyers };
}

export default function (data) {
  const token = data.buyers[exec.vu.idInTest - 1];
  // 409 is the CORRECT answer for buyers who were too late, so it is not counted as a failed request
  const r = api.placeOrder(token, { responseCallback: api.expect(201, 409) });
  // add 0 or 1 to every counter, so each metric always exists and its threshold is evaluated
  created.add(r.status === 201 ? 1 : 0);
  soldOut.add(r.status === 409 ? 1 : 0);
  unexpected.add(r.status === 201 || r.status === 409 ? 0 : 1);
  check(r, { 'order 201 or 409': (x) => x.status === 201 || x.status === 409 });
}

/** After the rush: read the final stock through the public API. */
export function teardown(data) {
  const stock = api.product(data.productId).json('stock');
  stockLeft.add(stock);                // threshold: must be exactly 0
  oversold.add(stock < 0 ? -stock : 0);
  check(stock, { 'stock never negative': (s) => s >= 0, 'stock fully sold': (s) => s === 0 });
}

export const handleSummary = (data) => buildSummary('flash-sale', data);
