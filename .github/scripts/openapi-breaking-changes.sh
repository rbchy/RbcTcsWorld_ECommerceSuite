#!/usr/bin/env bash
# Provider-side API contract gate: compares the OpenAPI description of the running backend with the approved
# baseline (docs/api/openapi.json) using openapi-diff. A breaking change (removed endpoint, removed response
# field, new required request field, changed type ...) fails the build; compatible changes are reported.
# A deliberate contract change is approved by refreshing the baseline (workflow "API baseline").
# Also runs a self-test each time: a copy of the baseline without /api/products MUST be reported as breaking.
set -u
BASE="${1:-docs/api/openapi.json}"; CURRENT="${2:-security/zap/openapi.json}"
IMAGE="openapitools/openapi-diff:latest"   # published only as "latest"; the self-test below guards it
[ -f "$CURRENT" ] || { echo "::warning title=API contract::no current OpenAPI file at $CURRENT"; exit 0; }
if [ ! -f "$BASE" ]; then
  echo "::warning title=API contract::no baseline at $BASE yet - run the 'API baseline' workflow to approve the current contract"; exit 0
fi
work=$(mktemp -d); chmod 777 "$work"
norm() { jq -S 'del(.servers)' "$1"; }          # the server URL differs per environment; key order is irrelevant
norm "$BASE" > "$work/base.json"; norm "$CURRENT" > "$work/current.json"
diff_specs() {  # old new -> exit code of openapi-diff (1 = incompatible)
  docker run --rm -v "$work:/specs" "$IMAGE" "/specs/$1" "/specs/$2" --fail-on-incompatible --markdown "/specs/$3" >/dev/null 2>"$work/err.txt"
}
# self-test: the gate must catch a removed endpoint
jq 'del(.paths["/api/products"])' "$work/base.json" > "$work/broken.json"
if diff_specs base.json broken.json selftest.md; then
  echo "::error title=API contract::self-test failed - openapi-diff did not report a removed endpoint as breaking"; exit 1
fi
diff_specs base.json current.json result.md; code=$?
summary=$(grep -E '^#+ |^- ' "$work/result.md" 2>/dev/null | head -25 | sed 's/%/%25/g' | paste -sd'\n' | sed ':a;N;$!ba;s/\n/%0A/g')
if [ "$code" -eq 0 ]; then
  if cmp -s "$work/base.json" "$work/current.json"; then
    echo "::notice title=API contract::OpenAPI unchanged against the approved baseline ($(jq '.paths|length' "$work/current.json") paths)"
  else
    echo "::notice title=API contract::compatible changes only (refresh the baseline to approve them)%0A$summary"
  fi
  exit 0
fi
if [ "$code" -eq 1 ]; then
  echo "::error title=API contract::BREAKING change against docs/api/openapi.json - clients would fail%0A$summary"
else
  echo "::error title=API contract::openapi-diff could not run (exit $code): $(head -c 300 "$work/err.txt")"
fi
exit 1
