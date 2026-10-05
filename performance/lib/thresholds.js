import { SLO } from './config.js';
import { ENDPOINTS } from './api.js';

/** Pass/fail rules shared by smoke and load tests. A failed threshold makes k6 exit with code 99 (CI goes red). */
export function standardThresholds(slo = SLO) {
  return {
    http_req_failed: [`rate<${slo.errorRate}`],
    'http_req_duration{kind:read}': [`p(95)<${slo.browseP95}`],
    'http_req_duration{kind:write}': [`p(95)<${slo.writeP95}`],
    purchase_journey_ms: [`p(95)<${slo.journeyP95}`],
    checks: [`rate>${slo.checks}`],
    business_errors: ['rate<0.01'],
    ...endpointBreakdown(),
  };
}

/**
 * k6 only keeps per-endpoint numbers for sub-metrics that have a threshold. These always-true
 * thresholds ("max>=0") exist just so the report can show one latency row per endpoint.
 */
export function endpointBreakdown() {
  const t = {};
  for (const name of ENDPOINTS) t[`http_req_duration{name:${name}}`] = ['max>=0'];
  return t;
}

export const TREND_STATS = ['count', 'avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'];
