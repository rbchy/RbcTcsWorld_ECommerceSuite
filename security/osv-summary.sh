#!/usr/bin/env bash
# One annotation per vulnerable package: name, version, number of advisories, highest CVSS score.
f="${1:-osv-results.json}"
[ -s "$f" ] || { echo "::notice title=OSV-Scanner::no results file (scan failed or nothing to scan)"; exit 0; }
jq -r '
  [ .results[]?.packages[]? | {
      pkg: "\(.package.ecosystem) \(.package.name)@\(.package.version)",
      ids: [ .groups[]?.ids[0] ],
      max: ([ .groups[]?.max_severity | select(. != null and . != "") | tonumber ] | max // 0)
  } ] as $p
  | "::notice title=OSV-Scanner::\($p | length) vulnerable package(s), \([$p[].ids[]] | length) advisories",
    ($p | sort_by(-.max)[]
      | "::\(if .max >= 9 then "error" elif .max >= 7 then "warning" else "notice" end) title=OSV \(.pkg)::max CVSS \(.max) - \(.ids | join(", "))")
' "$f"
