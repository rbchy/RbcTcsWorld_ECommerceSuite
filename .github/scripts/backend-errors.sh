#!/usr/bin/env bash
# Shows each distinct unhandled server error of the backend log (500s) as an annotation: the request,
# the exception and its first stack frames - so a 500 found by a test or by ZAP explains itself on the run page.
f="${1:-backend.log}"
[ -f "$f" ] || exit 0
python3 - "$f" <<'PY'
import sys, re
lines = open(sys.argv[1], errors="replace").read().splitlines()
seen = {}
for i, l in enumerate(lines):
    if "Unhandled error on" not in l:
        continue
    req = l.split("Unhandled error on", 1)[1].strip()
    block = lines[i + 1:i + 60]
    exc = block[0].strip() if block else "?"
    frames = [b.strip() for b in block[1:] if b.strip().startswith("at ")][:4]
    causes = [b.strip() for b in block if b.strip().startswith("Caused by:")]
    key = exc.split(":")[0]
    if key in seen:
        seen[key][0] += 1
        continue
    seen[key] = [1, req, exc, frames, causes[-1] if causes else ""]
if seen:
    print(f"::warning title=Backend 500s::{sum(v[0] for v in seen.values())} unhandled server error(s), {len(seen)} distinct")
for key, (n, req, exc, frames, cause) in list(seen.items())[:5]:
    msg = f"{n}x, first on {req}%0A{exc}%0A" + "%0A".join(frames) + (f"%0A{cause}" if cause else "")
    print(f"::warning title=Backend 500 ({key.split('.')[-1]})::{msg[:3500]}")
PY
