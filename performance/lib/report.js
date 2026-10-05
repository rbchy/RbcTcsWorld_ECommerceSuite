import { BASE_URL, REPORT_DIR } from './config.js';

/**
 * Builds the end-of-test output without any internet download:
 *   - console summary (stdout)
 *   - <test>-report.html  (open in a browser, attach to a bug or a CI run)
 *   - <test>-summary.json (raw numbers for trend tracking)
 */
export function buildSummary(testName, data) {
  return {
    stdout: text(testName, data),
    [`${REPORT_DIR}/${testName}-report.html`]: html(testName, data),
    [`${REPORT_DIR}/${testName}-summary.json`]: JSON.stringify(data, null, 2),
  };
}

const ms = (v) => (v === undefined || v === null ? '-' : `${v.toFixed(v < 10 ? 2 : 0)} ms`);
const pct = (v) => (v === undefined ? '-' : `${(v * 100).toFixed(2)} %`);
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function thresholdRows(data) {
  const rows = [];
  for (const [metric, m] of Object.entries(data.metrics)) {
    if (!m.thresholds) continue;
    for (const [expr, res] of Object.entries(m.thresholds)) {
      if (expr === 'max>=0') continue;                       // breakdown helpers, not real rules
      rows.push({ metric, expr, ok: res.ok });
    }
  }
  return rows.sort((a, b) => Number(a.ok) - Number(b.ok));   // failures first
}

function endpointRows(data) {
  return Object.entries(data.metrics)
    .filter(([k, m]) => k.startsWith('http_req_duration{name:') && m.values && m.values.count !== 0)
    .map(([k, m]) => ({ name: k.slice('http_req_duration{name:'.length, -1), v: m.values }))
    .sort((a, b) => (b.v['p(95)'] || 0) - (a.v['p(95)'] || 0));
}

function headline(data) {
  const v = (name, field) => (data.metrics[name] && data.metrics[name].values ? data.metrics[name].values[field] : undefined);
  return {
    requests: v('http_reqs', 'count'),
    rps: v('http_reqs', 'rate'),
    failed: v('http_req_failed', 'rate'),
    p95: v('http_req_duration', 'p(95)'),
    p99: v('http_req_duration', 'p(99)'),
    checks: v('checks', 'rate'),
    vusMax: v('vus_max', 'max'),
    iterations: v('iterations', 'count'),
    journeyP95: v('purchase_journey_ms', 'p(95)'),
    ordersPaid: v('orders_paid', 'count'),
  };
}

function text(testName, data) {
  const h = headline(data);
  const t = thresholdRows(data);
  const failed = t.filter((r) => !r.ok);
  const lines = [
    '',
    `=== k6 ${testName} @ ${BASE_URL} ===`,
    `requests ${h.requests ?? '-'} (${h.rps ? h.rps.toFixed(1) : '-'}/s) | failed ${pct(h.failed)} | p95 ${ms(h.p95)} | p99 ${ms(h.p99)} | checks ${pct(h.checks)} | max VUs ${h.vusMax ?? '-'}`,
    h.ordersPaid !== undefined ? `orders paid ${h.ordersPaid} | purchase journey p95 ${ms(h.journeyP95)}` : '',
    '',
    ...endpointRows(data).map((r) => `  ${r.name.padEnd(34)} p95 ${ms(r.v['p(95)']).padStart(9)}  max ${ms(r.v.max).padStart(9)}  n=${r.v.count ?? ''}`),
    '',
    ...t.map((r) => `  ${r.ok ? 'PASS' : 'FAIL'}  ${r.metric}  ${r.expr}`),
    '',
    failed.length ? `RESULT: ${failed.length} threshold(s) FAILED` : 'RESULT: all thresholds passed',
    `HTML report: ${REPORT_DIR}/${testName}-report.html`,
    '',
  ];
  return lines.filter((l, i, a) => !(l === '' && a[i - 1] === '')).join('\n');
}

function html(testName, data) {
  const h = headline(data);
  const t = thresholdRows(data);
  const failed = t.filter((r) => !r.ok).length;
  const tile = (label, value, bad) => `<div class="tile${bad ? ' bad' : ''}"><span>${label}</span><b>${value}</b></div>`;
  const custom = Object.entries(data.metrics)
    .filter(([k, m]) => !k.includes('{') && !k.startsWith('http_') && !['vus', 'vus_max', 'iterations', 'iteration_duration', 'data_sent', 'data_received', 'checks'].includes(k) && m.values)
    .map(([k, m]) => `<tr><td>${esc(k)}</td><td>${esc(m.type)}</td><td>${esc(Object.entries(m.values).map(([a, b]) => `${a}=${typeof b === 'number' ? +b.toFixed(2) : b}`).join('  '))}</td></tr>`).join('');

  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>k6 ${esc(testName)} report</title>
<style>
:root{--ink:#1f2933;--muted:#6b7785;--line:#e3e8ee;--ok:#1d6b3a;--okbg:#e7f5ec;--bad:#8a1c1c;--badbg:#fdecea;--bg:#f5f6f8}
body{font-family:system-ui,-apple-system,"Segoe UI",Arial,sans-serif;margin:0;background:var(--bg);color:var(--ink)}
main{max-width:1100px;margin:auto;padding:24px 16px 48px}h1{font-size:22px;margin:0 0 4px}h2{font-size:16px;margin:28px 0 8px}
.muted{color:var(--muted)}.banner{padding:12px 16px;border-radius:10px;font-weight:600;margin:16px 0}
.banner.ok{background:var(--okbg);color:var(--ok)}.banner.bad{background:var(--badbg);color:var(--bad)}
.tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px}
.tile{background:#fff;border-radius:10px;padding:12px 14px;box-shadow:0 1px 4px rgba(0,0,0,.06)}.tile span{display:block;font-size:12px;color:var(--muted)}.tile b{font-size:20px}
.tile.bad b{color:var(--bad)}table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,.06)}
th,td{text-align:left;padding:8px 10px;border-bottom:1px solid var(--line);font-size:14px}th{background:#eef1f5;font-size:12px;text-transform:uppercase;color:var(--muted)}
td.num{text-align:right;font-variant-numeric:tabular-nums}.pass{color:var(--ok);font-weight:700}.fail{color:var(--bad);font-weight:700}
</style></head><body><main>
<h1>k6 performance report - ${esc(testName)}</h1>
<p class="muted">${esc(BASE_URL)} - ${esc(new Date().toISOString())}</p>
<div class="banner ${failed ? 'bad' : 'ok'}">${failed ? `${failed} threshold(s) failed` : 'All thresholds passed'}</div>
<div class="tiles">
${tile('Requests', h.requests ?? '-')}${tile('Throughput', h.rps ? `${h.rps.toFixed(1)}/s` : '-')}
${tile('Failed requests', pct(h.failed), h.failed > 0.01)}${tile('p95 latency', ms(h.p95))}${tile('p99 latency', ms(h.p99))}
${tile('Checks passed', pct(h.checks), h.checks < 0.99)}${tile('Max virtual users', h.vusMax ?? '-')}
${h.ordersPaid !== undefined ? tile('Orders paid', h.ordersPaid) + tile('Purchase journey p95', ms(h.journeyP95)) : ''}
</div>
<h2>Thresholds (pass/fail rules)</h2>
<table><tr><th>Result</th><th>Metric</th><th>Rule</th></tr>
${t.map((r) => `<tr><td class="${r.ok ? 'pass' : 'fail'}">${r.ok ? 'PASS' : 'FAIL'}</td><td>${esc(r.metric)}</td><td>${esc(r.expr)}</td></tr>`).join('')}
</table>
<h2>Latency per endpoint (slowest p95 first)</h2>
<table><tr><th>Endpoint</th><th>Requests</th><th>avg</th><th>median</th><th>p90</th><th>p95</th><th>p99</th><th>max</th></tr>
${endpointRows(data).map((r) => `<tr><td>${esc(r.name)}</td><td class="num">${r.v.count ?? '-'}</td><td class="num">${ms(r.v.avg)}</td><td class="num">${ms(r.v.med)}</td><td class="num">${ms(r.v['p(90)'])}</td><td class="num">${ms(r.v['p(95)'])}</td><td class="num">${ms(r.v['p(99)'])}</td><td class="num">${ms(r.v.max)}</td></tr>`).join('')}
</table>
<h2>Business metrics</h2>
<table><tr><th>Metric</th><th>Type</th><th>Values</th></tr>${custom}</table>
</main></body></html>`;
}
