// LOAD: the expected busy-day traffic. 80% browsing, 20% buying, ramp up -> hold -> ramp down.
// Answers: "Do we meet our SLOs at normal peak?"
//   k6 run performance/tests/load.js                       (40 + 10 users, ~8 min)
//   k6 run -e BROWSERS=80 -e BUYERS=20 -e HOLD=10m performance/tests/load.js
import { browse, buy, prepare } from '../lib/journeys.js';
import { standardThresholds, TREND_STATS } from '../lib/thresholds.js';
import { buildSummary } from '../lib/report.js';

const BROWSERS = Number(__ENV.BROWSERS || 40);
const BUYERS = Number(__ENV.BUYERS || 10);
const HOLD = __ENV.HOLD || '5m';

const ramp = (target) => [
  { duration: '2m', target },   // warm-up (JIT, connection pools, caches)
  { duration: HOLD, target },   // steady state - the numbers that matter
  { duration: '1m', target: 0 },
];

export const options = {
  scenarios: {
    browsers: { executor: 'ramping-vus', exec: 'browser', startVUs: 0, stages: ramp(BROWSERS), gracefulRampDown: '30s' },
    buyers: { executor: 'ramping-vus', exec: 'buyer', startVUs: 0, stages: ramp(BUYERS), gracefulRampDown: '30s' },
  },
  thresholds: standardThresholds(),
  summaryTrendStats: TREND_STATS,
};

export const setup = prepare;
export const browser = browse;
export const buyer = buy;
export const handleSummary = (data) => buildSummary('load', data);
