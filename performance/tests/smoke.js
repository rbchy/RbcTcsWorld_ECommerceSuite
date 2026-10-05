// SMOKE: 1 browser + 1 buyer for a short time. Proves the scripts and the system work before any
// heavy test, and runs on every CI build. Strict thresholds: with no load, everything must be fast.
//   k6 run performance/tests/smoke.js
import { browse, buy, prepare } from '../lib/journeys.js';
import { standardThresholds, TREND_STATS } from '../lib/thresholds.js';
import { buildSummary } from '../lib/report.js';

const DURATION = __ENV.DURATION || '30s';

export const options = {
  scenarios: {
    browsers: { executor: 'constant-vus', exec: 'browser', vus: 1, duration: DURATION },
    buyers: { executor: 'constant-vus', exec: 'buyer', vus: 1, duration: DURATION },
  },
  thresholds: standardThresholds(),
  summaryTrendStats: TREND_STATS,
};

export const setup = prepare;
export const browser = browse;
export const buyer = buy;
export const handleSummary = (data) => buildSummary('smoke', data);
