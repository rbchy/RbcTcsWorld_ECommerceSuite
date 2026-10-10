#!/usr/bin/env bash
# One annotation per vulnerable package: version, advisories, highest CVSS score and the lowest version
# that fixes ALL of its advisories (the version to upgrade to).
# Exit code 1 for any package with CVSS >= FAIL_AT (default 9.0 = critical) - with or without a fix.
# The only way past the gate is to upgrade, or to accept the risk explicitly in osv-scanner.toml
# (reason + guard test + expiry date). Lower severities are reported, not blocking.
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
      src: ($src | sub("/target/bom\\.cdx\\.json$"; "/pom.xml") | sub(".*/(?<d>[^/]+/[^/]+)$"; "\(.d)")),
      ids: [ .groups[]?.ids[0] ],
      max: ([ .groups[]?.max_severity | select(. != null and . != "") | tonumber ] | max // 0),
      fix: ([ .vulnerabilities[]?.affected[]? | select(.package.name == $name) | .ranges[]? | fixes($cur)[] ]
            | sort_by(vkey) | last // "no fix published")
  } ] as $p
  | "::notice title=OSV-Scanner::\($p | length) vulnerable package(s), \([$p[].ids[]] | length) advisories",
    ($p | sort_by(-.max)[]
      | "::\(if .max >= 9 then "error" elif .max >= 7 then "warning" else "notice" end) title=OSV \(.pkg)::max CVSS \(.max) | fixed in \(.fix) | \(.src) | \(.ids | join(", "))")
' "$f"

# Accepted exceptions (osv-scanner.toml) are listed, so they stay visible on every run
for t in $(find . -name osv-scanner.toml -not -path "*/node_modules/*" 2>/dev/null); do
  grep -E '^id|^ignoreUntil' "$t" | paste - - | sed -E 's/id = "([^"]+)".*ignoreUntil = ([0-9-]+)/\1 until \2/' \
    | sed "s|^|::notice title=Accepted risk ($t)::|"
done

# Critical advisories: their CVE id and title, so the triage can start from the run page
jq -r '.results[]?.packages[]? | .package.name as $n | .vulnerabilities as $v | .groups[]?
  | select(((.max_severity // "") | if . == "" then 0 else tonumber end) >= 9) | .ids[0] as $id
  | ([$v[]? | select(.id == $id)][0]) as $x
  | "::notice title=Advisory \($id)::\($n) | \([$x.aliases[]?] | join(", ")) | \($x.summary // "no summary")"' "$f"

FAIL_AT="${FAIL_AT:-9}"
blocking=$(jq --argjson t "$FAIL_AT" '
  def vkey: split(".") | map(capture("^(?<n>[0-9]+)").n // "0" | tonumber);
  def fixes($cur): .events as $e
    | [ range(0; $e | length) | select($e[.].introduced != null) | {i: $e[.].introduced, f: ($e[. + 1].fixed // null)} ]
    | map(select(.f != null and (.i | vkey) <= ($cur | vkey) and ($cur | vkey) < (.f | vkey)) | .f);
  [ .results[]?.packages[]? | .package.name as $name | .package.version as $cur
    | select(([ .groups[]?.max_severity | select(. != null and . != "") | tonumber ] | max // 0) >= $t)
    ] | length' "$f")
if [ "${blocking:-0}" -gt 0 ]; then
  echo "::error title=OSV-Scanner::$blocking package(s) with CVSS >= $FAIL_AT - upgrade (see 'fixed in'), or if not exploitable here add a documented exception with an expiry date to osv-scanner.toml"
  exit 1
fi
