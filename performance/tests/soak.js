// SOAK (endurance): moderate load for a long time. Finds what only appears over time:
// memory leaks, connection-pool exhaustion, growing tables slowing queries, expiring tokens.
// Compare the first and the last minutes in the report: latency should NOT creep up.
//   k6 run -e DURATION=1h performance/tests/soak.js
import { browse, buy, prepare } from '../lib/journeys.js';
import { standardThresholds, TREND_STATS } from '../lib/thresholds.js';
import { buildSummary } from '../lib/report.js';

const DURATION = __ENV.DURATION || '30m';

export const options = {
  scenarios: {
    browsers: { executor: 'constant-vus', exec: 'browser', vus: Number(__ENV.BROWSERS || 20), duration: DURATION },
    buyers: { executor: 'constant-vus', exec: 'buyer', vus: Number(__ENV.BUYERS || 5), duration: DURATION },
  },
  thresholds: standardThresholds(),
  summaryTrendStats: TREND_STATS,
};

export const setup = prepare;
export const browser = browse;
export const buyer = buy;
export const handleSummary = (data) => buildSummary('soak', data);
