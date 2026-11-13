#!/usr/bin/env python3
"""Derive a CI-only server.properties from config/server.properties.example.

Only the database connection keys are changed; every other key keeps the
example value so the test exercises the same strict UnifiedConfiguration
key set as a real deployment.  The output goes to a runner temp directory
and is never committed.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

SERVER = Path(__file__).resolve().parent.parent.parent  # lito伺服器/LiTo381-Server


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--jdbc-base-url", required=True)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    args = parser.parse_args()
    overrides = {
        "database.jdbc.base.url": args.jdbc_base_url,
        "database.user": args.user,
        "database.password": args.password,
    }
    seen: set[str] = set()
    lines = []
    example = (SERVER / "config" / "server.properties.example").read_text(encoding="utf-8")
    for line in example.splitlines():
        key = line.split("=", 1)[0].strip()
        if not line.lstrip().startswith("#") and "=" in line and key in overrides:
            line = f"{key}={overrides[key]}"
            seen.add(key)
        lines.append(line)
    missing = set(overrides) - seen
    if missing:
        sys.exit(f"server.properties.example lacks keys: {sorted(missing)}")
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {args.out}")


if __name__ == "__main__":
    main()
