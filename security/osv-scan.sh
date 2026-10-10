#!/usr/bin/env bash
# Dependency scan (SCA) that cannot go silently blind - used by GitHub Actions and Jenkins.
#
# Why not "osv-scanner --recursive ./" on the pom.xml files (the old way, DEF-021): OSV-Scanner resolves Maven
# transitive dependencies itself, from Maven Central. When Central answers 429 (rate limit) it logs a warning,
# falls back to the DIRECT dependencies only and still reports success - the automation module then had 14
# instead of 130 packages, and 5 vulnerable libraries (one CVSS 9.1) went unseen.
#
# Now:
#  1. Maven resolves the full dependency tree (it retries, and fails the build if a download fails) and writes a
#     CycloneDX SBOM per module, test scope included (test libraries run in CI with network and secrets).
#  2. OSV-Scanner scans exactly these SBOMs + the npm lockfile - no resolution of its own.
#  3. Completeness check: every SBOM must hold a plausible number of components.
#  4. Self-test: a fixture SBOM with a known-vulnerable library (log4j-core 2.14.1, Log4Shell) MUST be reported,
#     otherwise the vulnerability database was not reachable and a "0 findings" result would be worthless.
# Output: osv-results.json (read by security/osv-summary.sh, which applies the CVSS gate).
set -euo pipefail
cd "$(dirname "$0")/.."
OSV_IMAGE="ghcr.io/google/osv-scanner:v2.3.0@sha256:6421cdd773a54fad5fd991895b4f3840beba55a2f30a8182185919e13979a2c8"
CYCLONEDX="org.cyclonedx:cyclonedx-maven-plugin:2.9.3:makeBom"
MIN_COMPONENTS="${MIN_COMPONENTS:-50}"

for m in backend automation; do
  mvn -B -q -f "$m/pom.xml" "$CYCLONEDX" -DincludeTestScope=true -DoutputFormat=json -DoutputName=bom.cdx
  n=$(jq '[.components[]?] | length' "$m/target/bom.cdx.json")
  echo "::notice title=SBOM $m::$n components (CycloneDX, test scope included)"
  if [ "$n" -lt "$MIN_COMPONENTS" ]; then
    echo "::error title=SBOM $m::only $n components (expected >= $MIN_COMPONENTS) - dependency resolution incomplete, scan result not trustworthy"
    exit 1
  fi
done

osv() { docker run --rm -v "$PWD:/src" "$OSV_IMAGE" scan source "$@" || true; }   # exit 1 = findings; the gate is osv-summary.sh

osv --config=/src/backend/osv-scanner.toml \
    -L /src/backend/target/bom.cdx.json -L /src/automation/target/bom.cdx.json -L /src/frontend/package-lock.json \
    --format=json --output=/src/osv-results.json

osv -L /src/security/osv-selftest/bom.cdx.json --format=json --output=/src/osv-selftest.json
found=$(jq '[.results[]?.packages[]? | select(.package.name == "org.apache.logging.log4j:log4j-core")] | length' osv-selftest.json 2>/dev/null || echo 0)
if [ "$found" -lt 1 ]; then
  echo "::error title=OSV self-test::known-vulnerable log4j-core 2.14.1 was NOT reported - vulnerability database unreachable, result not trustworthy"
  exit 1
fi
echo "::notice title=OSV self-test::known-vulnerable fixture detected (log4j-core 2.14.1) - scanner and database work"
rm -f osv-selftest.json
