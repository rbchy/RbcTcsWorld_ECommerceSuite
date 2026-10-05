// STRESS: keep adding users step by step until the system degrades. The goal is to FIND the limit,
// not to pass: watch at which step p95 and errors jump. The test stops itself when more than 10%
// of requests fail (abortOnFail), so a broken system is not hammered for nothing.
//   k6 run performance/tests/stress.js
import { browse, buy, prepare } from '../lib/journeys.js';
import { endpointBreakdown, TREND_STATS } from '../lib/thresholds.js';
import { buildSummary } from '../lib/report.js';

const STEP = __ENV.STEP || '2m';
const steps = (max) => [
  { duration: STEP, target: Math.round(max * 0.25) },
  { duration: STEP, target: Math.round(max * 0.5) },
  { duration: STEP, target: Math.round(max * 0.75) },
  { duration: STEP, target: max },
  { duration: '1m', target: 0 },   // recovery: does it come back to normal?
];

export const options = {
  scenarios: {
    browsers: { executor: 'ramping-vus', exec: 'browser', stages: steps(Number(__ENV.MAX_BROWSERS || 200)) },
    buyers: { executor: 'ramping-vus', exec: 'buyer', stages: steps(Number(__ENV.MAX_BUYERS || 50)) },
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.10', abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{kind:read}': ['p(95)<2000'],
    'http_req_duration{kind:write}': ['p(95)<4000'],
    business_errors: ['rate<0.01'],   // even under stress, money must never be wrong
    ...endpointBreakdown(),
  },
  summaryTrendStats: TREND_STATS,
};

export const setup = prepare;
export const browser = browse;
export const buyer = buy;
export const handleSummary = (data) => buildSummary('stress', data);
