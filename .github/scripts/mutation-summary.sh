#!/usr/bin/env bash
# Reads PIT's mutations.xml and prints the mutation score, the per-class table and the surviving mutants
# as GitHub annotations (also readable as plain text in Jenkins).
f="${1:-backend/target/pit-reports/mutations.xml}"
[ -f "$f" ] || { echo "::warning title=Mutation testing::no PIT report at $f"; exit 0; }
python3 - "$f" "${2:-}" <<'PY'
import sys, collections, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
detected_states = {"KILLED", "TIMED_OUT", "MEMORY_ERROR", "RUN_ERROR"}
total = collections.Counter(); per_class = collections.defaultdict(collections.Counter); survivors = []
for m in root.iter("mutation"):
    st = m.get("status"); cls = m.findtext("mutatedClass").split(".")[-1].split("$")[0]
    total[st] += 1; per_class[cls][st] += 1
    if st in ("SURVIVED", "NO_COVERAGE"):
        survivors.append((st, cls, m.findtext("mutatedMethod"), m.findtext("lineNumber"),
                          (m.findtext("description") or "").strip()))
n = sum(total.values()); det = sum(total[s] for s in detected_states)
covered = n - total["NO_COVERAGE"]
score = 100.0 * det / n if n else 100.0
strength = 100.0 * det / covered if covered else 100.0
print(f"::notice title=Mutation testing (PIT)::mutation score {score:.1f}% ({det}/{n} mutants killed) | "
      f"test strength {strength:.1f}% (of mutants in covered code) | survived {total['SURVIVED']} | "
      f"no coverage {total['NO_COVERAGE']} | timed out {total['TIMED_OUT']}")
rows = []
for cls, c in per_class.items():
    cn = sum(c.values()); cd = sum(c[s] for s in detected_states)
    rows.append((100.0 * cd / cn, cls, cd, cn, c["SURVIVED"], c["NO_COVERAGE"]))
rows.sort()
print("::notice title=Weakest classes (mutation score)::" + " | ".join(
    f"{cls} {p:.0f}% ({d}/{t}, {s} survived, {nc} uncovered)" for p, cls, d, t, s, nc in rows[:6]))
print("\nclass                        score   killed/total  survived  no-coverage")
for p, cls, d, t, s, nc in sorted(rows, key=lambda r: r[1]):
    print(f"{cls:28s} {p:5.1f}%   {d:4d}/{t:<4d}      {s:4d}      {nc:4d}")
# the surviving mutants in one annotation (GitHub keeps ~4 KB per annotation: short descriptions),
# and the not-covered ones counted per method (those are executed by integration tests, not unit tests)
def short(d):
    for a, b in (("removed conditional - replaced equality check with false", "if(==) forced false"),
                 ("removed conditional - replaced equality check with true", "if(==) forced true"),
                 ("removed conditional - replaced comparison check with false", "if(<,>) forced false"),
                 ("removed conditional - replaced comparison check with true", "if(<,>) forced true"),
                 ("changed conditional boundary", "boundary < to <="), ("negated conditional", "negated if"),
                 ("replaced return value with", "return"), ("removed call to ", "no call "), ("com/rbctcsworld/ecommerce/", "")):
        d = d.replace(a, b)
    return d.split(" for ")[0]
surv = sorted([x for x in survivors if x[0] == "SURVIVED"], key=lambda x: (x[1], int(x[3])))
print(f"::notice title=Survived ({len(surv)})::" + "%0A".join(f"{c}.{m}:{l} {short(d)}" for _, c, m, l, d in surv))
nc = collections.Counter(f"{c}.{m}" for st, c, m, l, d in survivors if st == "NO_COVERAGE")
print(f"::notice title=Not covered by unit tests ({sum(nc.values())})::" + ", ".join(f"{k} x{v}" for k, v in sorted(nc.items())))
limit = int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2] else 15
print(f"\nSurviving mutants ({len(survivors)}):")
for i, (st, cls, meth, line, desc) in enumerate(sorted(survivors, key=lambda x: (x[1], int(x[3])))):
    print(f"  {st:11s} {cls}.{meth}():{line}  {desc}")
    if i < limit:  # GitHub shows at most 10 warnings per step
        print(f"::warning title=Surviving mutant ({st.lower().replace('_', ' ')})::{cls}.{meth}() line {line}: {desc}")
PY
