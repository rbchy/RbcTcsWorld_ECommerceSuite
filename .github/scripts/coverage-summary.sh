#!/usr/bin/env bash
# Reads JaCoCo's CSV and prints total line/branch coverage plus the 5 least covered packages as annotations.
f="${1:-backend/target/site/jacoco/jacoco.csv}"
[ -f "$f" ] || { echo "::warning title=Coverage::no JaCoCo report at $f"; exit 0; }
python3 - "$f" <<'PY'
import csv, sys, collections
rows = list(csv.DictReader(open(sys.argv[1])))
def pct(c, m): return 100.0 * c / (c + m) if c + m else 100.0
t = collections.Counter()
pk = collections.defaultdict(collections.Counter)
for r in rows:
    for k in ("LINE", "BRANCH", "INSTRUCTION", "METHOD"):
        for kind in ("COVERED", "MISSED"):
            v = int(r[f"{k}_{kind}"]); t[f"{k}_{kind}"] += v; pk[r["PACKAGE"]][f"{k}_{kind}"] += v
line = pct(t["LINE_COVERED"], t["LINE_MISSED"]); br = pct(t["BRANCH_COVERED"], t["BRANCH_MISSED"])
print(f"::notice title=Coverage (JaCoCo)::lines {line:.1f}% ({t['LINE_COVERED']}/{t['LINE_COVERED']+t['LINE_MISSED']}) | branches {br:.1f}% | methods {pct(t['METHOD_COVERED'], t['METHOD_MISSED']):.1f}% | instructions {pct(t['INSTRUCTION_COVERED'], t['INSTRUCTION_MISSED']):.1f}%")
low = sorted(pk.items(), key=lambda kv: pct(kv[1]["LINE_COVERED"], kv[1]["LINE_MISSED"]))[:5]
print("::notice title=Least covered packages::" + " | ".join(
    f"{p.split('.')[-1]} {pct(c['LINE_COVERED'], c['LINE_MISSED']):.0f}% lines, {pct(c['BRANCH_COVERED'], c['BRANCH_MISSED']):.0f}% branches" for p, c in low))
PY
