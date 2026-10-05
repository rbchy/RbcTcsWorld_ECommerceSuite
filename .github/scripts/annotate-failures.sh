#!/usr/bin/env bash
# Prints every failed/errored JUnit test of the given surefire report folders as a GitHub annotation.
for dir in "$@"; do
python3 - "$dir" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
for f in glob.glob(sys.argv[1] + '/TEST-*.xml'):
    for tc in ET.parse(f).getroot().iter('testcase'):
        for kind in ('failure', 'error'):
            e = tc.find(kind)
            if e is not None:
                msg = ' '.join((e.get('message') or e.text or '').split())[:600].replace('%', '%25')
                print(f"::error title={tc.get('classname', '').split('.')[-1]}.{tc.get('name')}::{msg}")
PY
done
