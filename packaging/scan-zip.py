#!/usr/bin/env python3
"""Independent zip scan for T040. Exit 1 if forbidden entries exist."""
from __future__ import annotations

import sys
import zipfile
from pathlib import Path


def forbidden_reason(name: str) -> str | None:
    unix = name.replace("\\", "/").strip("/")
    leaf = unix.rsplit("/", 1)[-1]
    if leaf == "LiTo.bin":
        return "LiTo.bin"
    if leaf == "server.properties":
        return "config/server.properties"
    for part in unix.split("/"):
        low = part.lower()
        if low in {"pak", "map", "sprite", "sprites"}:
            return f"pak/map/sprite ({unix})"
        if low.endswith((".pak", ".map", ".spr", ".sprite", ".til", ".idx")):
            return f"pak/map/sprite ({unix})"
        if part == "candidates":
            return f"db/candidates ({unix})"
    return None


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("usage: scan-zip.py <package.zip>", file=sys.stderr)
        return 2
    zip_path = Path(argv[1])
    with zipfile.ZipFile(zip_path) as zf:
        names = [n.replace("\\", "/") for n in zf.namelist() if not n.endswith("/")]
    hits = []
    for name in names:
        reason = forbidden_reason(name)
        if reason:
            hits.append(f"{reason}\t{name}")
    print(f"ZIP={zip_path.as_posix()}")
    print(f"ZIP_BYTES={zip_path.stat().st_size}")
    print(f"ZIP_ENTRY_COUNT={len(names)}")
    print("ZIP_ENTRIES_BEGIN")
    for name in sorted(names):
        print(name)
    print("ZIP_ENTRIES_END")
    if hits:
        print("FORBIDDEN_SCAN=fail")
        for hit in hits:
            print(hit)
        return 1
    print("FORBIDDEN_SCAN=pass")
    print("FORBIDDEN_ABSENT=LiTo.bin,server.properties,pak/map/sprite,db/candidates")
    has_example = any(n.endswith("server.properties.example") for n in names)
    has_packaged_entry = any(n.endswith("啟動套件伺服器.bat") for n in names)
    has_packaged_ps1 = any(n.endswith("tools/Start-PackagedServer.ps1") for n in names)
    has_wrapper = any(n.endswith("lito伺服器/1.啟動伺服器.bat") for n in names)
    has_start_bat = any(n.endswith("LiTo381-Server/Start-LiTo381-Server.bat") for n in names)
    has_official_ps1 = any(n.endswith("tools/Start-LiToServer.ps1") for n in names)
    has_jar = any(n.endswith("LiTo381-Server.jar") for n in names)
    print(f"HAS_EXAMPLE={has_example}")
    print(f"HAS_PACKAGE_BAT={has_packaged_entry}")
    print(f"HAS_PACKAGE_PS1={has_packaged_ps1}")
    print(f"HAS_WRAPPER_BAT={has_wrapper}")
    print(f"HAS_START_BAT={has_start_bat}")
    print(f"HAS_OFFICIAL_START_PS1={has_official_ps1}")
    print(f"HAS_SERVER_JAR={has_jar}")
    if not all(
        [
            has_example,
            has_packaged_entry,
            has_packaged_ps1,
            has_wrapper,
            has_start_bat,
            has_official_ps1,
            has_jar,
        ]
    ):
        print("STARTUP_CHAIN=incomplete")
        return 1
    print("STARTUP_CHAIN=ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
