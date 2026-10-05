#!/usr/bin/env bash
# Turns the ZAP JSON report into GitHub annotations: one line per finding (risk, name, count, first URL).
f="${1:-security/zap/zap-report.json}"
[ -f "$f" ] || { echo "::warning title=ZAP::no report produced"; exit 0; }
jq -r '
  [.site[].alerts[]] as $a
  | ($a | map(select((.riskcode|tonumber) >= 2)) | length) as $high
  | "::notice title=OWASP ZAP::\($a|length) alert types (\($high) medium/high). See the zap-report artifact.",
    ($a[] | select((.riskcode|tonumber) >= 1)
      | "::\(if (.riskcode|tonumber) >= 2 then "warning" else "notice" end) title=ZAP \(.riskdesc)::[\(.pluginid)] \(.alert) - \(.count) instance(s), e.g. \(.instances[0].method) \(.instances[0].uri)")
' "$f"
