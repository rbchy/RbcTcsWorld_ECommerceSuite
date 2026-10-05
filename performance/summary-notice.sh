#!/usr/bin/env bash
# Prints the key k6 numbers of a test as a GitHub annotation (visible on the run page without opening logs).
# Usage: performance/summary-notice.sh <test-name>
f="performance/reports/$1-summary.json"
[ -f "$f" ] || { echo "no summary for $1"; exit 0; }
jq -r --arg t "$1" '
  def v(m; k): (.metrics[m].values[k] // null);
  def r(x): if x == null then "-" else ((x * 10 | round) / 10 | tostring) end;
  [ .metrics | to_entries[] | select(.value.thresholds != null) | .key as $m
    | .value.thresholds | to_entries[] | select(.key != "max>=0") | select(.value.ok == false) | "\($m) \(.key)" ] as $failed
  | "::notice title=k6 \($t)::requests \(v("http_reqs";"count")) (\(r(v("http_reqs";"rate")))/s)"
    + " | failed \(r((v("http_req_failed";"rate") // 0) * 100))%"
    + " | p95 \(r(v("http_req_duration";"p(95)"))) ms | p99 \(r(v("http_req_duration";"p(99)"))) ms"
    + " | read p95 \(r(.metrics["http_req_duration{kind:read}"].values["p(95)"] // null)) ms"
    + " | write p95 \(r(.metrics["http_req_duration{kind:write}"].values["p(95)"] // null)) ms"
    + " | journey p95 \(r(v("purchase_journey_ms";"p(95)"))) ms | orders paid \(v("orders_paid";"count") // "-")"
    + " | checks \(r((v("checks";"rate") // 0) * 100))%"
    + (if v("flash_orders_created";"count") != null then " | flash created \(v("flash_orders_created";"count")) rejected \(v("flash_orders_rejected";"count")) stock left \(v("flash_stock_left";"count"))" else "" end)
    + " | thresholds failed: \(if ($failed|length)==0 then "none" else ($failed|join(", ")) end)"
' "$f"
