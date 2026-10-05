// CATALOG (DEF-007 regression): anonymous visitors open the product list and search.
// Before the fix GET /api/products returned the WHOLE catalog, so size and latency grew with every product.
// Measures response size (bytes, items) and latency of the catalog endpoint.
//   k6 run performance/tests/catalog.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL } from '../lib/config.js';
import { TREND_STATS } from '../lib/thresholds.js';
import { buildSummary } from '../lib/report.js';

const bytes = new Trend('catalog_bytes');
const items = new Trend('catalog_items');

export const options = {
  scenarios: {
    visitors: { executor: 'constant-vus', vus: Number(__ENV.VUS || 10), duration: __ENV.DURATION || '30s' },
  },
  thresholds: {
    catalog_bytes: ['p(95)<10000'],                                   // one page, not the whole shop
    'http_req_duration{name:GET /api/products}': ['p(95)<100'],
    http_req_failed: ['rate<0.01'],
  },
  summaryTrendStats: TREND_STATS,
};

export function setup() {
  const r = http.get(`${BASE_URL}/api/products?size=1`);
  // Fail fast with a clear message instead of 30 s of "connection refused" and misleading numbers
  if (r.status === 0) throw new Error(`Backend not reachable on ${BASE_URL} - start it first: mvn -f backend/pom.xml spring-boot:run`);
  if (r.status !== 200) throw new Error(`GET /api/products answered ${r.status}`);
  console.log(`catalog total (X-Total-Count): ${r.headers['X-Total-Count'] || 'header missing - unpaginated endpoint (DEF-007 not fixed)'}`);
}

export default function () {
  const r = http.get(`${BASE_URL}/api/products`, { tags: { name: 'GET /api/products' } });
  check(r, { 'catalog 200': (x) => x.status === 200 });
  if (r.status === 200) {
    bytes.add(r.body.length);
    items.add(r.json().length);
  }
  sleep(0.2);
}

export const handleSummary = (data) => buildSummary('catalog', data);
