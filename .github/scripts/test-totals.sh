#!/usr/bin/env bash
# Usage: test-totals.sh <label> <surefire dir> - prints run/failed/errors/skipped as an annotation.
python3 - "$1" "$2" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
label, d = sys.argv[1], sys.argv[2]
t = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
classes = 0
for f in glob.glob(d + "/TEST-*.xml"):
    r = ET.parse(f).getroot(); classes += 1
    for k in t: t[k] += int(r.get(k, 0))
print(f"::notice title=Test totals {label}::{t['tests']} tests in {classes} classes | failures {t['failures']} | errors {t['errors']} | skipped {t['skipped']}")
PY
