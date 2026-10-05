// SPIKE: a sudden rush (TV advert, flash-sale e-mail): from 5 to 250 users in 10 seconds,
// then back down. Checks that the system survives the jump AND recovers afterwards.
//   k6 run performance/tests/spike.js
import { browse, buy, prepare } from '../lib/journeys.js';
import { endpointBreakdown, TREND_STATS } from '../lib/thresholds.js';
import { buildSummary } from '../lib/report.js';

const PEAK = Number(__ENV.PEAK || 250);

export const options = {
  scenarios: {
    browsers: {
      executor: 'ramping-vus', exec: 'browser', startVUs: 5,
      stages: [
        { duration: '30s', target: 5 },                     // normal
        { duration: '10s', target: PEAK },                  // spike!
        { duration: '1m', target: PEAK },                   // stay at the peak
        { duration: '10s', target: 5 },                     // drop
        { duration: '1m', target: 5 },                      // recovery period
      ],
    },
    buyers: { executor: 'constant-vus', exec: 'buyer', vus: 5, duration: '3m' },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    'http_req_duration{kind:read}': ['p(95)<3000'],
    business_errors: ['rate<0.01'],
    ...endpointBreakdown(),
  },
  summaryTrendStats: TREND_STATS,
};

export const setup = prepare;
export const browser = browse;
export const buyer = buy;
export const handleSummary = (data) => buildSummary('spike', data);
