#!/usr/bin/env python3
"""交叉檢查：正式 JAR 的 class 常數池字串引用到的表，是否都在 generated/table-inventory.tsv（T104 / Issue #302）。

src/main 與正式 0.6.71 JAR 之間有 tools/*/build.py 的 bytecode patch；這支工具只讀 JAR（zip）裡的
local/lito381/server/*.class，解析常數池的 CONSTANT_String，不執行任何 Java。
判定：字串剛好等於白名單表名，或 SQL 字串中 FROM／JOIN／INTO／UPDATE 後的表名。排除 Modern* 類別。
用法：py -3 tools/db-grants/jar_tables.py <正式 JAR 路徑>
輸出：印出「JAR 有、清單沒有」與「清單有、JAR 沒有」兩組，附類別名。
"""
from __future__ import annotations

import re
import struct
import sys
import zipfile
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import scan_db_access as scan  # noqa: E402


def string_constants(data: bytes):
    if data[:4] != b"\xca\xfe\xba\xbe":
        return []
    count = struct.unpack(">H", data[8:10])[0]
    utf8, strings, i, idx = {}, [], 10, 1
    while idx < count:
        tag = data[i]
        if tag == 1:
            ln = struct.unpack(">H", data[i + 1:i + 3])[0]
            utf8[idx] = data[i + 3:i + 3 + ln].decode("utf-8", errors="replace")
            i += 3 + ln
        elif tag in (7, 8, 16, 19, 20):
            if tag == 8:
                strings.append(struct.unpack(">H", data[i + 1:i + 3])[0])
            i += 3
        elif tag == 15:
            i += 4
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            i += 5
        elif tag in (5, 6):
            i += 9
            idx += 1
        else:
            raise ValueError(f"unknown constant tag {tag}")
        idx += 1
    out = [utf8.get(s, "") for s in strings]
    # invokedynamic 字串串接的 recipe 存在 BootstrapMethods 參數（CONSTANT_String），已含在上面。
    return out


def main(argv=None):
    argv = argv or sys.argv[1:]
    if not argv:
        print(__doc__)
        return 2
    jar = Path(argv[0])
    root = HERE.parent.parent
    schemas = scan.load_schemas(root / scan.DB_REL)
    known = {t for g, tabs in schemas.items() if g != "modern" for t in tabs} | {"character_lito"}
    inv = {}
    for line in (HERE / "generated/table-inventory.tsv").read_text(encoding="utf-8").splitlines()[1:]:
        c = line.split("\t")
        inv[c[1]] = c[0]
    found = defaultdict(set)
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if not (name.startswith("local/lito381/server/") and name.endswith(".class")):
                continue
            cls = name.rsplit("/", 1)[1][:-6].split("$")[0]
            if cls.startswith("Modern"):
                continue
            for s in string_constants(z.read(name)):
                if s in known:
                    found[s].add(cls)
                    continue
                for m in scan.TABLE_REF.finditer(s.replace("\u0001", " ")):
                    if m.group(3) in known:
                        found[m.group(3)].add(cls)
                for t in re.findall(r"`(" + scan.NAME + r")`", s):  # 例如 SettlementSql 的 "`.`lito_battle_settlement`"
                    if t in known:
                        found[t].add(cls)
    extra = sorted(t for t in found if t not in inv)
    missing = sorted(t for t in inv if t not in found)
    print(f"jar={jar.name} 引用表={len(found)} 清單表={len(inv)}")
    print("== JAR 有、清單沒有（需人工判定：誤判字串／lito_mysql 閘住／真的漏）")
    for t in extra:
        print(f"{t}\t{','.join(sorted(found[t]))}")
    print("== 清單有、JAR 常數池沒找到（可能是 src/main 新增或動態組字）")
    for t in missing:
        print(f"{inv[t]}\t{t}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
