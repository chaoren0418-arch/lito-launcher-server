#!/usr/bin/env python3
"""由 generated/db-access.json＋review.json 產生部署用權限清單與 GRANT 範本（T104 / Issue #302）。

規則（與 docs/design/database.md 一致）：
- 只採正式路徑：排除 Modern*、gated_ranges、excluded_tables、偵測方式為 whitelist:string-only 的紀錄、庫別 unknown。
- 讀取：每個表一筆 SELECT；寫入：只有 review.json write_allowed 列的表（character_lito、lito_battle_settlement）。
- information_schema 查詢、GET_LOCK／RELEASE_LOCK 不需另外授權（MySQL 只回有權限的物件）。
- 表必須在 schema 傾印中存在；MySQL 對不存在的表做表級 GRANT 會失敗，缺 schema 的表另列。

輸出：
  generated/grants.sql            GRANT 範本（庫名用 380_db／380_lito／380_ac 佔位，部署時替換）
  generated/table-inventory.tsv   每張表一列：庫別、權限、啟動必讀、欄位、證據
  --update-doc <md>               把兩段產出寫進 Markdown 的 GENERATED 標記之間
  --ref-schema group=path         另外比對其他傾印（例如 1-伺服器 db/380_db.sql）是否有該表／欄位，只印出不寫檔
用法：py -3 tools/db-grants/gen_grants.py [--update-doc docs/design/database.md]
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import scan_db_access as scan  # noqa: E402

DB_LABEL = {"static": "380_db", "lito": "380_lito", "character": "380_ac"}
DB_TITLE = {"static": "靜態庫（database.static.database，預設 380_db）",
            "lito": "LiTo 控制庫（database.lito.database，預設 380_lito）",
            "character": "來源角色庫（lito_source_server.character_database，每個啟用來源一個）"}


def in_ranges(rec, ranges):
    name = rec["file"].split("/")[-1]
    return next((r for r in ranges if r["file"] == name and r["from"] <= rec["line"] <= r["to"]), None)


def build(root: Path):
    data = json.loads((HERE / "generated/db-access.json").read_text(encoding="utf-8"))
    review = json.loads((HERE / "review.json").read_text(encoding="utf-8"))
    schemas = scan.load_schemas(root / scan.DB_REL)
    tables: dict[tuple, dict] = {}
    dropped = defaultdict(lambda: defaultdict(list))
    blocked_writes = defaultdict(set)
    for r in data["records"]:
        key = (r["db"], r["table"])
        why = None
        if any(r["class"].startswith(p) for p in review["exclude_class_prefix"]):
            why = "Modern*"
        elif r["reach"] == "unreached":
            why = "正式入口不可達"
        elif r["method"] == "whitelist:string-only":
            why = "只有字串相同（非表名用途）"
        elif r["db"] in ("unknown", "information_schema", "modern"):
            why = "庫別 " + r["db"]
        elif r["table"] in review["excluded_tables"]:
            why = "excluded_tables"
        elif in_ranges(r, review["gated_ranges"]):
            why = "lito_mysql 區段"
        if why:
            dropped[key][why].append(f'{r["class"]}.java:{r["line"]}')
            continue
        op = r["op"].upper()
        if op != "SELECT":
            if op not in review["write_allowed"].get(r["table"], []):
                blocked_writes[key].add(f'{op}@{r["class"]}.java:{r["line"]}')
                continue
        t = tables.setdefault(key, {"db": r["db"], "table": r["table"], "privs": set(), "startup": [],
                                    "columns": set(), "evidence": [], "in_schema": r["in_schema"],
                                    "engine": r["engine"]})
        t["privs"].add(op)
        t["columns"] |= {c for c in r["columns"] if c != "*"} | set(r.get("getter_columns", []))
        if "*" in r["columns"]:
            t["columns"].add("*")
        ev = f'{r["class"]}.java:{r["line"]}'
        if ev not in t["evidence"]:
            t["evidence"].append(ev)
        st = r["class"] in review["startup_classes"] or in_ranges(r, review["startup_ranges"])
        if st and ev not in t["startup"]:
            t["startup"].append(ev)
    for inc in review.get("manual_includes", []):
        key = (inc["db"], inc["table"])
        t = tables.setdefault(key, {"db": inc["db"], "table": inc["table"], "privs": set(), "startup": [],
                                    "columns": set(), "evidence": [], "in_schema": inc["table"] in schemas.get(inc["db"], {}),
                                    "engine": ""})
        t["privs"] |= set(inc["privs"])
        t["evidence"].append("人工補充：" + inc["evidence"])
        t["manual"] = True
    for name, why in review.get("startup_tables", {}).items():
        for t in tables.values():
            if t["table"] == name and not t["startup"]:
                t["startup"].append("人工：" + why)
    for name in review.get("optional_tables", {}):
        for t in tables.values():
            if t["table"] == name:
                t["optional"] = review["optional_tables"][name]
    for t in tables.values():
        known_cols = []
        for g in [t["db"]]:
            known_cols = schemas.get(g, {}).get(t["table"], {}).get("columns", [])
        if known_cols:
            t["unknown_columns"] = sorted(c for c in t["columns"] if c != "*" and c not in known_cols)
            t["columns"] = {c for c in t["columns"] if c == "*" or c in known_cols}
        else:
            t["unknown_columns"] = []
    return tables, dropped, blocked_writes, review


def grants_sql(tables):
    out = ["-- 由 tools/db-grants/gen_grants.py 產生；請勿手改，改 review.json 後重產。",
           "-- 佔位：380_db＝database.static.database、380_lito＝database.lito.database、",
           "--       380_ac＝每個 enabled=1 來源的 character_database（每個來源各複製一份「來源角色庫」段並換庫名）。",
           "-- 帳號：'lito_shared'@'127.0.0.1' 只是範例；Host 依實際連線來源設定，不要用 '%'。",
           "-- 前提：表必須已存在（MySQL 對不存在的表做表級 GRANT 會失敗）；character_lito 需已有 last_result_id 欄（db/lito_battle_settlement.sql）與積分欄位（db/lito_character_ranking.sql）。",
           "CREATE USER IF NOT EXISTS 'lito_shared'@'127.0.0.1' IDENTIFIED BY '<由營運設定>';", ""]
    for g in ("static", "lito", "character"):
        rows = sorted((t for t in tables.values() if t["db"] == g), key=lambda t: t["table"])
        out.append(f"-- ===== {DB_TITLE[g]}：{len(rows)} 表 =====")
        for t in rows:
            privs = ", ".join(p for p in ("SELECT", "INSERT", "UPDATE") if p in t["privs"])
            note = "" if t["in_schema"] or t["table"] == "character_lito" else "  -- 注意：repo 傾印無此表，部署前確認存在"
            line = f"GRANT {privs} ON `{DB_LABEL[g]}`.`{t['table']}` TO 'lito_shared'@'127.0.0.1';{note}"
            if t.get("optional"):
                line = "-- 選用（表存在才執行）：" + line
            out.append(line)
        out.append("")
    return "\n".join(out).rstrip() + "\n"


def inventory_rows(tables):
    rows = []
    for g in ("static", "lito", "character"):
        for t in sorted((t for t in tables.values() if t["db"] == g), key=lambda t: t["table"]):
            cols = sorted(t["columns"] - {"*"})
            col_text = ("全部欄位（SELECT *）；程式取用：" if "*" in t["columns"] else "") + ("、".join(cols) if cols else ("—" if "*" not in t["columns"] else "（未逐欄取用）"))
            rows.append({"db": DB_LABEL[g], "table": t["table"],
                         "privs": "/".join(p for p in ("SELECT", "INSERT", "UPDATE") if p in t["privs"])
                         + ("（選用）" if t.get("optional") else "") + ("（人工補充）" if t.get("manual") else ""),
                         "startup": "是" if t["startup"] else "否", "columns": col_text,
                         "evidence": "、".join(t["startup"][:3] or t["evidence"][:3]) + ("…" if len(t["evidence"]) > 3 else ""),
                         "unknown": "、".join(t["unknown_columns"])})
    return rows


def inventory_md(rows):
    head = ["| 庫 | 表 | 權限 | 啟動即讀 | 讀取／寫入欄位（掃描） | 證據 |", "|---|---|---|---|---|---|"]
    body = [f'| {r["db"]} | `{r["table"]}` | {r["privs"]} | {r["startup"]} | {r["columns"]} | {r["evidence"]} |'
            for r in rows]
    return "\n".join(head + body) + "\n"


def replace_block(text, name, content):
    pat = re.compile(r"(<!-- BEGIN GENERATED:" + name + r" -->\n).*?(<!-- END GENERATED:" + name + r" -->)", re.S)
    if not pat.search(text):
        raise SystemExit(f"找不到 GENERATED:{name} 標記")
    return pat.sub(lambda m: m.group(1) + content + m.group(2), text)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=str(HERE.parent.parent))
    ap.add_argument("--update-doc")
    ap.add_argument("--ref-schema", action="append", default=[], help="group=path，例如 static=D:/.../380_db.sql")
    args = ap.parse_args(argv)
    root = Path(args.root)
    tables, dropped, blocked, review = build(root)
    sql = grants_sql(tables)
    rows = inventory_rows(tables)
    gen = HERE / "generated"
    (gen / "grants.sql").write_text(sql, encoding="utf-8", newline="\n")
    with (gen / "table-inventory.tsv").open("w", encoding="utf-8", newline="\n") as fh:
        fh.write("db\ttable\tprivileges\tstartup\tcolumns\tevidence\tcolumns_not_in_repo_schema\n")
        for r in rows:
            fh.write("\t".join([r["db"], r["table"], r["privs"], r["startup"], r["columns"], r["evidence"], r["unknown"]]) + "\n")
    with (gen / "excluded.tsv").open("w", encoding="utf-8", newline="\n") as fh:
        fh.write("db\ttable\treason\tsites\n")
        for (db, t), reasons in sorted(dropped.items()):
            if (db, t) in tables and set(reasons) <= {"只有字串相同（非表名用途）", "Modern*", "lito_mysql 區段"}:
                continue
            for why, sites in sorted(reasons.items()):
                fh.write(f"{db}\t{t}\t{why}\t{'、'.join(sorted(set(sites))[:5])}\n")
        for (db, t), sites in sorted(blocked.items()):
            fh.write(f"{db}\t{t}\t寫入不授權（共用模式唯讀）\t{'、'.join(sorted(sites)[:5])}\n")
    counts = {g: sum(1 for t in tables.values() if t["db"] == g) for g in DB_LABEL}
    print(json.dumps({"tables": counts, "grants": sum(counts.values()),
                      "blocked_write_tables": len(blocked)}, ensure_ascii=False))
    for ref in args.ref_schema:
        group, path = ref.split("=", 1)
        scan.SCHEMA_FILES = {group: [Path(path).name]}
        ref_tabs = scan.load_schemas(Path(path).parent).get(group, {})
        for t in sorted((t for t in tables.values() if t["db"] == group), key=lambda t: t["table"]):
            rt = ref_tabs.get(t["table"])
            if not rt:
                print(f"[ref {group}] 缺表 {t['table']}")
                continue
            miss = sorted(c for c in t["columns"] if c != "*" and c not in rt["columns"])
            if miss:
                print(f"[ref {group}] {t['table']} 缺欄位 {len(miss)}：{','.join(miss)}")
    if args.update_doc:
        doc = Path(args.update_doc)
        text = doc.read_text(encoding="utf-8")
        text = replace_block(text, "inventory", inventory_md(rows))
        text = replace_block(text, "grants", "```sql\n" + sql + "```\n")
        doc.write_text(text, encoding="utf-8", newline="\n")
        print(f"updated {doc}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
