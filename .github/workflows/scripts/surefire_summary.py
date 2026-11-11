#!/usr/bin/env python3
"""Summarise Surefire XML: totals plus every skipped / failed / errored case.

Usage: surefire_summary.py <surefire-reports dir> <out.json> <label>
Used by build.yml to compare skip counts with and without the CI MySQL fixture
(Issue #334 / T119).
"""
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def main() -> None:
    reports, out, label = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3]
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    cases = {"skipped": [], "failures": [], "errors": []}
    classes_skipped = set()
    classes_run = set()
    for xml in sorted(reports.glob("TEST-*.xml")):
        root = ET.parse(xml).getroot()
        for case in root.iter("testcase"):
            name = f"{case.get('classname')}#{case.get('name')}"
            totals["tests"] += 1
            kind = None
            for child, key in (("skipped", "skipped"), ("failure", "failures"), ("error", "errors")):
                node = case.find(child)
                if node is not None:
                    kind = key
                    message = (node.get("message") or (node.text or "")).strip().splitlines()
                    cases[key].append({"case": name, "message": message[0][:300] if message else ""})
                    break
            if kind:
                totals[kind] += 1
            if kind == "skipped":
                classes_skipped.add(case.get("classname"))
            else:
                classes_run.add(case.get("classname"))
    summary = {
        "label": label,
        "totals": totals,
        "classes_with_skips": len(classes_skipped),
        "classes_fully_skipped": len(classes_skipped - classes_run),
        **cases,
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(summary, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"[{label}] tests={totals['tests']} failures={totals['failures']} "
          f"errors={totals['errors']} skipped={totals['skipped']} "
          f"classes_fully_skipped={summary['classes_fully_skipped']}")
    for key in ("failures", "errors"):
        for item in cases[key]:
            print(f"  {key[:-1].upper()}: {item['case']} :: {item['message']}")


if __name__ == "__main__":
    main()
