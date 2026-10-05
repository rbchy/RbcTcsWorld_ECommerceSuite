#!/usr/bin/env bash
# One annotation per vulnerable package: version, advisories, highest CVSS score and the lowest version
# that fixes ALL of its advisories (the version to upgrade to).
f="${1:-osv-results.json}"
[ -s "$f" ] || { echo "::notice title=OSV-Scanner::no results file (scan failed or nothing to scan)"; exit 0; }
jq -r '
  def vkey: split(".") | map(capture("^(?<n>[0-9]+)").n // "0" | tonumber);
  # [introduced, fixed) pairs of a range that contain the version we use -> that range'"'"'s fix
  def fixes($cur): .events as $e
    | [ range(0; $e | length) | select($e[.].introduced != null) | {i: $e[.].introduced, f: ($e[. + 1].fixed // null)} ]
    | map(select(.f != null and (.i | vkey) <= ($cur | vkey) and ($cur | vkey) < (.f | vkey)) | .f);
  [ .results[]? | .source.path as $src | .packages[]? | .package.name as $name | .package.version as $cur | {
      pkg: "\(.package.name)@\(.package.version)",
      src: ($src | sub(".*/(?<d>[^/]+/[^/]+)$"; "\(.d)")),
      ids: [ .groups[]?.ids[0] ],
      max: ([ .groups[]?.max_severity | select(. != null and . != "") | tonumber ] | max // 0),
      fix: ([ .vulnerabilities[]?.affected[]? | select(.package.name == $name) | .ranges[]? | fixes($cur)[] ]
            | sort_by(vkey) | last // "no fix published")
  } ] as $p
  | "::notice title=OSV-Scanner::\($p | length) vulnerable package(s), \([$p[].ids[]] | length) advisories",
    ($p | sort_by(-.max)[]
      | "::\(if .max >= 9 then "error" elif .max >= 7 then "warning" else "notice" end) title=OSV \(.pkg)::max CVSS \(.max) | fixed in \(.fix) | \(.src) | \(.ids | join(", "))")
' "$f"
