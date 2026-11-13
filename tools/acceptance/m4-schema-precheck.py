#!/usr/bin/env python3
"""M4 實驗環境開機前靜態預檢（T128 / Issue #379）。唯讀：不連資料庫、不執行 Java、不改傾印檔。

輸入
  1. tools/db-grants/generated/db-access.json＋review.json（經 gen_grants.build 套用後的正式路徑存取清單）
  2. 三份 SQL 傾印（只讀，不複製進 repo）：
       static    = D:\\BinEngine\\environments\\p24\\sql\\380_db.sql   → 匯入成 p24_380db_lito_exp
       lito      = D:\\BinEngine\\environments\\p24\\sql\\380_lito.sql → 需要時匯入成 p24_380lito_lito_exp
       character = D:\\BinEngine\\environments\\p24\\sql\\380_ac.sql   → 只當參考（p24_accounts_lito_exp 不是由它建的）
  3. tools/acceptance/m4-precheck-rules.json（資料列檢查與判讀備註）

輸出
  docs/acceptance/m4-schema-precheck.md               人看的結果
  tools/acceptance/generated/m4-required-columns.tsv  每個「庫別／表／欄位」一列（m4-db-verify.sql 的來源）
  --update-sql：把需求清單與 GRANT 清單寫進 m4-db-verify.sql／m4-db-prepare.sql 的 GENERATED 區塊

用法
  py -3 tools/acceptance/m4-schema-precheck.py --update-sql
  py -3 tools/acceptance/m4-schema-precheck.py --static <380_db.sql> --lito <380_lito.sql> --character <380_ac.sql>
  py -3 tools/acceptance/m4-schema-precheck.py --strict     # 有「會擋啟動」的缺口時退出碼 1
輸出不含時間戳；同一份原始碼＋同一份傾印重跑結果相同。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(ROOT / "tools" / "db-grants"))
import gen_grants  # noqa: E402

GROUPS = ("static", "lito", "character")
DEFAULT_DUMPS = {
    "static": r"D:\BinEngine\environments\p24\sql\380_db.sql",
    "lito": r"D:\BinEngine\environments\p24\sql\380_lito.sql",
    "character": r"D:\BinEngine\environments\p24\sql\380_ac.sql",
}
GROUP_TITLE = {
    "static": "靜態庫（`database.static.database`）",
    "lito": "LiTo 控制庫（`database.lito.database`）",
    "character": "來源角色庫（`lito_source_server.character_database`）",
}
EXP_SCHEMA = {
    "static": "`p24_380db_lito_exp`（#284 主控 2026-11-07 02:43 裁定：由本傾印匯入）",
    "lito": "`p24_lito_lito_exp`（#83 建立）；不相容時改由本傾印匯入 `p24_380lito_lito_exp`",
    "character": "`p24_accounts_lito_exp`（#83 由 P24 的 `p24_accounts` 複製結構；**不是**由本傾印建立，本欄只當參考）",
}
SQL_VAR = {"static": "@m4_static", "lito": "@m4_lito", "character": "@m4_ac"}
NAME = r"[A-Za-z0-9_\u0080-\uffff●]+"
CREATE = re.compile(r"^CREATE TABLE(?: IF NOT EXISTS)?\s+(?:`?" + NAME + r"`?\.)?`(" + NAME + r")`\s*\(")
COLUMN = re.compile(r"^\s*`(" + NAME + r")`\s+(.*)$")
INSERT = re.compile(r"^INSERT(?: IGNORE)? INTO\s+(?:`?" + NAME + r"`?\.)?`(" + NAME + r")`\s+VALUES\s*\((.*)\);\s*$")


# ---------------------------------------------------------------- 傾印
def parse_dump(path: Path, want_rows: set[str]):
    """回傳 (info, tables, rows)。tables[name] = {columns, engine, line, strict_cols}；rows 只收 want_rows 指定的表。"""
    raw = path.read_bytes()
    text = raw.decode("utf-8", errors="replace")
    lines = text.splitlines()
    tables: dict[str, dict] = {}
    rows: dict[str, list[list]] = {t: [] for t in want_rows}
    insert_count: Counter = Counter()
    zero_date, create_db = [], []
    i = 0
    while i < len(lines):
        line = lines[i]
        m = CREATE.match(line)
        if m:
            name, cols, strict, j = m.group(1), [], [], i + 1
            while j < len(lines) and not lines[j].startswith(")"):
                cm = COLUMN.match(lines[j])
                if cm:
                    cols.append(cm.group(1))
                    rest = cm.group(2)
                    if "NOT NULL" in rest and "DEFAULT" not in rest and "AUTO_INCREMENT" not in rest:
                        strict.append(cm.group(1))
                    if "'0000-00-00" in rest:
                        zero_date.append(f"{name}.{cm.group(1)}（第 {j + 1} 行）")
                j += 1
            em = re.search(r"ENGINE\s*=\s*(\w+)", lines[j]) if j < len(lines) else None
            tables[name] = {"columns": cols, "engine": em.group(1) if em else None, "line": i + 1, "strict_cols": strict}
            i = j + 1
            continue
        if line.startswith("INSERT"):
            im = INSERT.match(line)
            if im:
                insert_count[im.group(1)] += 1
                if im.group(1) in rows:
                    rows[im.group(1)].append(split_values(im.group(2)))
        elif re.match(r"^\s*(CREATE DATABASE|CREATE SCHEMA|USE\s|DROP DATABASE)", line, re.I):
            create_db.append(f"第 {i + 1} 行：{line.strip()[:80]}")
        i += 1
    head = "\n".join(lines[:16])
    ver = re.search(r"Source Server Version\s*:\s*(.+)", head)
    schema = re.search(r"Source Schema\s*:\s*(.+)", head)
    info = {"path": str(path), "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest().upper(),
            "tables": len(tables), "engines": Counter(t["engine"] for t in tables.values()),
            "source_version": ver.group(1).strip() if ver else "（檔頭無）",
            "source_schema": schema.group(1).strip() if schema else "（檔頭無）",
            "zero_date": zero_date, "create_db": create_db,
            "definer": sum(1 for ln in lines if "DEFINER" in ln and not ln.startswith("INSERT")),
            "inserts": insert_count}
    return info, tables, rows


def split_values(body: str) -> list:
    """把單列 VALUES 內容拆成 Python 值（字串去引號、NULL→None、其餘保留原字面）。"""
    out, cur, i, n, quoted = [], [], 0, len(body), False
    in_str = False
    while i < n:
        ch = body[i]
        if in_str:
            if ch == "\\" and i + 1 < n:
                cur.append(body[i + 1])
                i += 2
                continue
            if ch == "'":
                if i + 1 < n and body[i + 1] == "'":
                    cur.append("'")
                    i += 2
                    continue
                in_str = False
            else:
                cur.append(ch)
        elif ch == "'":
            in_str, quoted, cur = True, True, []
        elif ch == ",":
            out.append(finish(cur, quoted))
            cur, quoted = [], False
        elif not quoted:
            cur.append(ch)
        i += 1
    out.append(finish(cur, quoted))
    return out


def finish(cur, quoted):
    s = "".join(cur)
    if quoted:
        return s
    s = s.strip()
    return None if s.upper() == "NULL" else s


# ---------------------------------------------------------------- 需求清單
def requirements(root: Path, rules: dict):
    tables, _dropped, _blocked, _review = gen_grants.build(root)
    out = []
    for g in GROUPS:
        for t in sorted((t for t in tables.values() if t["db"] == g), key=lambda t: t["table"]):
            cols = sorted({c for c in t["columns"] if c != "*"} | set(t.get("unknown_columns", [])))
            out.append({"group": g, "table": t["table"],
                        "privs": [p for p in ("SELECT", "INSERT", "UPDATE") if p in t["privs"]],
                        "startup": bool(t["startup"]), "optional": t.get("optional"), "manual": bool(t.get("manual")),
                        "star": "*" in t["columns"], "columns": cols,
                        "tolerated": [c for c in cols if c in rules.get("tolerated_columns", {}).get(g + "." + t["table"], [])],
                        "evidence": (t["startup"][:2] or t["evidence"][:2])})
    return out


def compare(reqs, dumps):
    """回傳 findings[group] = {missing_tables: [...], missing_columns: [...]}（欄位比對不分大小寫，同 MySQL）。"""
    findings = {}
    for g in GROUPS:
        tabs = dumps[g][1] if g in dumps else None
        mt, mc = [], []
        if tabs is not None:
            lower = {k.lower(): v for k, v in tabs.items()}
            for r in (r for r in reqs if r["group"] == g):
                dt = lower.get(r["table"].lower())
                if dt is None:
                    mt.append(r)
                    continue
                have = {c.lower() for c in dt["columns"]}
                miss = [c for c in r["columns"] if c.lower() not in have]
                if miss:
                    mc.append((r, miss))
        findings[g] = {"missing_tables": mt, "missing_columns": mc, "compared": tabs is not None}
    return findings


def blocking(r, missing=None):
    """缺口會不會擋啟動：啟動即讀、非選用，而且缺的不全是程式能容忍的欄位。"""
    if r["optional"] or r["manual"]:
        return False
    if missing is not None and all(c in r["tolerated"] for c in missing):
        return False
    return r["startup"]


# ---------------------------------------------------------------- 資料列檢查
def run_row_checks(rules, dumps):
    results = []
    for chk in rules.get("row_checks", []):
        g, table = chk["group"], chk["table"]
        if g not in dumps:
            results.append((chk, "未比對（未提供傾印）", ""))
            continue
        _info, tabs, rows = dumps[g]
        if table not in tabs:
            results.append((chk, "缺表", ""))
            continue
        cols = [c.lower() for c in tabs[table]["columns"]]
        col = chk["column"].lower()
        if col not in cols:
            results.append((chk, f"缺欄位 `{chk['column']}`", ""))
            continue
        idx = cols.index(col)
        values = [r[idx] for r in rows.get(table, []) if idx < len(r)]
        show_cols = [c.lower() for c in chk.get("show", [])]
        if chk["kind"] == "values_present":
            have = {str(v) for v in values}
            missing = [str(v) for v in chk["values"] if str(v) not in have]
            detail = ""
            if show_cols:
                want = {str(v) for v in chk["values"]}
                parts = []
                for r in rows.get(table, []):
                    if idx < len(r) and str(r[idx]) in want:
                        parts.append(str(r[idx]) + "→" + "/".join(
                            str(r[cols.index(c)]) for c in show_cols if c in cols and cols.index(c) < len(r)))
                detail = "；".join(parts)
            status = "齊全" if not missing else "缺 " + "、".join(missing)
            results.append((chk, status, detail))
        elif chk["kind"] == "list":
            parts = []
            for r in rows.get(table, []):
                parts.append("/".join(str(r[cols.index(c)]) for c in ([col] + show_cols) if c in cols and cols.index(c) < len(r)))
            results.append((chk, f"{len(parts)} 列", "；".join(parts[:40]) + ("…" if len(parts) > 40 else "")))
        elif chk["kind"] == "count":
            results.append((chk, f"{len(values)} 列", ""))
        else:
            raise SystemExit(f"未知的 row_checks.kind：{chk['kind']}")
    return results


# ---------------------------------------------------------------- 輸出
def md_report(reqs, dumps, findings, row_results, rules, access_meta):
    notes = rules.get("notes", {})
    L = ["# M4 實驗環境開機前靜態預檢（schema／欄位／資料列）", "",
         "由 `tools/acceptance/m4-schema-precheck.py` 產生，請勿手改；改程式、`tools/db-grants/review.json` 或 "
         "`tools/acceptance/m4-precheck-rules.json` 後重跑。", "",
         "- 性質：**靜態比對**（python 讀原始碼掃描結果與 SQL 傾印檔），沒有連任何資料庫、沒有啟動伺服器。",
         "- 「程式需要」＝`tools/db-grants` 掃描 `src/main` 後套用 `review.json` 的正式路徑存取清單"
         f"（{access_meta}）。`SELECT *` 的表只列程式實際取用的欄位。",
         "- 「會擋啟動」＝該表在啟動路徑上被讀取（`review.json` 的 startup_classes／startup_ranges），缺表或缺欄位會讓伺服器在 "
         "`[LiTo] ready` 之前退出；其餘是執行到該功能時才會出錯。",
         "- 傾印檔只讀取、沒有複製進 repo；實驗庫實際內容以 `tools/acceptance/m4-db-verify.sql` 連線查到的為準（待驗證）。", ""]

    L += ["## 1. 輸入", "", "| 庫別 | 傾印檔 | bytes | SHA-256 | 檔頭來源 | 表數 | 引擎 | 對應實驗 schema |", "|---|---|---:|---|---|---:|---|---|"]
    for g in GROUPS:
        if g not in dumps:
            L.append(f"| {g} | （未提供） | | | | | | {EXP_SCHEMA[g]} |")
            continue
        info = dumps[g][0]
        eng = "、".join(f"{k} {v}" for k, v in sorted(info["engines"].items(), key=lambda kv: str(kv[0])))
        L.append(f"| {g} | `{info['path']}` | {info['bytes']} | `{info['sha256']}` | {info['source_schema']}／MySQL {info['source_version']} "
                 f"| {info['tables']} | {eng} | {EXP_SCHEMA[g]} |")
    L.append("")

    L += ["## 2. 結論摘要", "", "| 庫別 | 程式需要的表 | 傾印缺表 | 其中會擋啟動 | 缺欄位的表 | 缺欄位數 | 其中會擋啟動的表 |", "|---|---:|---:|---:|---:|---:|---:|"]
    total_block = 0
    for g in GROUPS:
        f = findings[g]
        need = sum(1 for r in reqs if r["group"] == g)
        if not f["compared"]:
            L.append(f"| {g} | {need} | 未比對 | | | | |")
            continue
        bt = sum(1 for r in f["missing_tables"] if blocking(r))
        bc = sum(1 for r, m in f["missing_columns"] if blocking(r, m))
        total_block += bt + bc
        L.append(f"| {g} | {need} | {len(f['missing_tables'])} | {bt} | {len(f['missing_columns'])} | "
                 f"{sum(len(m) for _, m in f['missing_columns'])} | {bc} |")
    L += ["", f"會擋啟動的缺口合計：**{total_block} 項**（缺表＋缺欄位的表）。各項判讀見下節；"
          "`character` 列比的是 `380_ac.sql` 參考傾印，`p24_accounts_lito_exp` 的實況要用 §6 的清單連線查。", ""]

    L += ["## 3. 缺表與缺欄位（逐庫）", ""]
    for g in GROUPS:
        f = findings[g]
        L += [f"### {g}：{GROUP_TITLE[g]}", ""]
        if not f["compared"]:
            L += ["未提供傾印，未比對。", ""]
            continue
        if not f["missing_tables"] and not f["missing_columns"]:
            L += ["缺表 **0 項**、缺欄位 **0 項**。", ""]
            continue
        L += [f"缺表 **{len(f['missing_tables'])} 項**：", ""]
        if f["missing_tables"]:
            L += ["| 表 | 權限 | 會擋啟動 | 證據 | 判讀 |", "|---|---|---|---|---|"]
            for r in f["missing_tables"]:
                tag = "選用" if r["optional"] else ("人工補充" if r["manual"] else "")
                L.append(f"| `{r['table']}` | {'/'.join(r['privs'])}{'（' + tag + '）' if tag else ''} | "
                         f"{'是' if blocking(r) else '否'} | {'、'.join(r['evidence'])} | "
                         f"{notes.get(g + '.' + r['table'], '')} |")
            L.append("")
        L += [f"缺欄位 **{sum(len(m) for _, m in f['missing_columns'])} 項**（{len(f['missing_columns'])} 張表）：", ""]
        if f["missing_columns"]:
            L += ["| 表 | 缺少的欄位 | 會擋啟動 | 證據 | 判讀 |", "|---|---|---|---|---|"]
            for r, miss in f["missing_columns"]:
                L.append(f"| `{r['table']}` | {'、'.join('`' + c + '`' for c in miss)} | "
                         f"{'是' if blocking(r, miss) else '否（可容忍）' if r['startup'] and not r['optional'] else '否'} | {'、'.join(r['evidence'])} | "
                         f"{notes.get(g + '.' + r['table'], '')} |")
            L.append("")

    L += ["## 4. 引擎與匯入相容性", "", "| 檢查 | 結果 | 依據 |", "|---|---|---|"]
    for g, table, allowed, why in (("lito", "lito_battle_settlement", ("InnoDB",), "`SettlementSql.check` 要求 InnoDB"),
                                   ("character", "character_lito", ("InnoDB", "MyISAM"), "`SharedDatabase.check` 要求 InnoDB 或 MyISAM")):
        if g not in dumps:
            continue
        t = dumps[g][1].get(table)
        res = "缺表" if not t else (f"{t['engine']}（符合）" if t["engine"] in allowed else f"{t['engine']}（**不符**，需要 {'／'.join(allowed)}）")
        L.append(f"| {g} `{table}` 引擎 | {res} | {why} |")
    if "lito" in dumps:
        want = {"skill_value", "buff", "projectile", "map", "zone", "portal", "npc_identity", "map_page", "release", "pending"}
        have = {t[14:] for t in dumps["lito"][1] if t.startswith("lito_resource_")}
        res = "剛好 10 張（符合）" if have == want else f"**不符**：多 {sorted(have - want)}、少 {sorted(want - have)}"
        L.append(f"| lito `lito_resource_*` 表集合 | {res} | `ResourceDatabase.load` 要求剛好這 10 張，多或少都拒絕啟動（ResourceDatabase.java:33-38） |")
    for g in GROUPS:
        if g not in dumps:
            continue
        info = dumps[g][0]
        L.append(f"| {g} 傾印內的 `CREATE DATABASE`／`USE`／`DROP DATABASE` | "
                 f"{'**' + str(len(info['create_db'])) + ' 處**：' + '；'.join(info['create_db'][:5]) if info['create_db'] else '0 處（匯入目標完全由命令列指定的 schema 決定）'} "
                 "| #284 02:43 裁定第 1 點：不得建立或覆蓋其他庫 |")
        L.append(f"| {g} 傾印內的 `DEFINER`（view／trigger／routine） | {info['definer']} 處 | 匯入帳號不需要 SUPER |")
        zd = info["zero_date"]
        L.append(f"| {g} 零日期預設值（`DEFAULT '0000-00-00…'`） | "
                 f"{'**' + str(len(zd)) + ' 處**：' + '、'.join(zd[:6]) if zd else '0 處'} | "
                 "MySQL 8.0 預設 `sql_mode`（`NO_ZERO_DATE`＋嚴格模式）會讓這種 `CREATE TABLE` 失敗；P24Lab 的 `mysql.ini` 設的是 `sql-mode=NO_ENGINE_SUBSTITUTION`（2026-11-13 唯讀確認設定檔），不受影響。在其他 8.0 實例匯入前要先調整 `sql_mode` |")
    L.append("")

    L += ["## 5. 資料列檢查（程式或驗收步驟依賴的現有資料）", "", "| 庫別 | 表 | 檢查 | 結果 | 內容 | 用途／依據 |", "|---|---|---|---|---|---|"]
    for chk, status, detail in row_results:
        want = ("值存在：" + "、".join(str(v) for v in chk.get("values", []))) if chk["kind"] == "values_present" else \
            ("列出現有列" if chk["kind"] == "list" else "列數")
        L.append(f"| {chk['group']} | `{chk['table']}`.`{chk['column']}` | {want} | {status} | {detail} | {chk.get('why', '')} |")
    L.append("")

    L += ["## 6. `p24_accounts_lito_exp`：開機前要用 `information_schema` 查的表與欄位", "",
          "#83 的角色庫是從 P24 的 `p24_accounts` 複製結構，沒有傾印可以靜態比對。下表是程式對來源角色庫的全部需求；"
          "`tools/acceptance/m4-db-verify.sql` 的 GENERATED 區塊由同一份清單產生，會逐欄對 `information_schema.COLUMNS` 查並列出缺少的項目。",
          "", "| 表 | 權限 | 會擋啟動 | 需要的欄位 | `380_ac.sql` 參考傾印 |", "|---|---|---|---|---|"]
    ref = dumps["character"][1] if "character" in dumps else {}
    ref_lower = {k.lower(): v for k, v in ref.items()}
    for r in (r for r in reqs if r["group"] == "character"):
        cols = "、".join(f"`{c}`" for c in r["columns"]) or "（只檢查表存在）"
        if r["star"]:
            cols = "`*`；" + cols
        dt = ref_lower.get(r["table"].lower())
        if not ref:
            state = "未比對"
        elif dt is None:
            state = "缺表"
        else:
            miss = [c for c in r["columns"] if c.lower() not in {x.lower() for x in dt["columns"]}]
            state = "齊全" if not miss else "缺 " + "、".join(miss)
        tag = "（選用）" if r["optional"] else ""
        L.append(f"| `{r['table']}`{tag} | {'/'.join(r['privs'])} | {'是' if blocking(r) else '否'} | {cols} | {state} |")
    L += ["", "靜態庫與控制庫的同類清單在 `tools/acceptance/generated/m4-required-columns.tsv`（三個庫別全部列出），`m4-db-verify.sql` 一併檢查。", ""]

    if rules.get("footnotes"):
        L += ["## 7. 判讀補充", ""] + [f"- {x}" for x in rules["footnotes"]] + [""]
    return "\n".join(L).rstrip() + "\n", total_block


def write_tsv(reqs, path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as fh:
        fh.write("group\ttable\tcolumn\tprivileges\tstartup\toptional\n")
        for r in reqs:
            for c in ([""] + r["columns"]):
                fh.write("\t".join([r["group"], r["table"], c, "/".join(r["privs"]), "Y" if r["startup"] else "N",
                                    "Y" if (r["optional"] or r["manual"] or c in r["tolerated"]) else "N"]) + "\n")


def sql_quote(s: str) -> str:
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def derived_block(reqs, tables_only: bool) -> str:
    """m4-db-verify.sql 用：需求清單寫成衍生表本體（SELECT … UNION ALL …），不建任何表，整份核對維持唯讀。"""
    out = ["-- 由 tools/acceptance/m4-schema-precheck.py --update-sql 產生；請勿手改。"]
    rows = []
    for r in reqs:
        for c in ([""] if tables_only else [""] + r["columns"]):
            tolerated = c != "" and c in r.get("tolerated", [])
            vals = [r["group"], r["table"], c, "/".join(r["privs"]), "Y" if r["startup"] else "N",
                    "Y" if (r["optional"] or r["manual"] or tolerated) else "N"]
            if not rows:
                names = ("grp", "tbl", "col", "privs", "startup", "optional")
                rows.append("SELECT " + ", ".join(f"{sql_quote(v)} AS {n}" for v, n in zip(vals, names)))
            else:
                rows.append("UNION ALL SELECT " + ", ".join(sql_quote(v) for v in vals))
    return "\n".join(out + rows) + "\n"


def v2_block(reqs) -> str:
    """m4-db-verify.sql 的 V2：每張需求表一個唯讀查詢，只輸出缺少的表或欄位。"""
    conv = "CONVERT({0} USING utf8mb4) COLLATE utf8mb4_general_ci"
    warn = "IF((@m4_warn := @m4_warn + 1) > 0, 'WARN', 'WARN')"
    fail = "IF((@m4_fail := @m4_fail + 1) > 0, 'FAIL', 'FAIL')"
    out = ["-- 由 tools/acceptance/m4-schema-precheck.py --update-sql 產生；請勿手改。"]
    for r in reqs:
        var, tbl = SQL_VAR[r["group"]], sql_quote(r["table"])
        table_optional = "Y" if (r["optional"] or r["manual"]) else "N"
        rows = [f"SELECT '' AS col, '{table_optional}' AS optional"]
        for c in r["columns"]:
            rows.append(f"UNION ALL SELECT {sql_quote(c)}, '{'Y' if (table_optional == 'Y' or c in r['tolerated']) else 'N'}'")
        status = f"IF(x.optional = 'N', {fail}, {warn})" if r["startup"] else warn
        out += [
            f"SET @m4_te = (SELECT COUNT(*) FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA = CONVERT({var} USING utf8) COLLATE utf8_general_ci"
            f" AND {conv.format('t.TABLE_NAME')} = {conv.format(tbl)});",
            f"SELECT 'V2' AS check_id, IF(x.col = '', '缺表', '缺欄位') AS finding, {sql_quote(r['group'])} AS grp, {var} AS schema_name, {tbl} AS tbl, x.col, "
            f"{sql_quote('/'.join(r['privs']))} AS privs, '{'Y' if r['startup'] else 'N'}' AS startup, x.optional, {status} AS status",
            "FROM (" + " ".join(rows) + ") x",
            f"LEFT JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA = CONVERT({var} USING utf8) COLLATE utf8_general_ci"
            f" AND {conv.format('c.TABLE_NAME')} = {conv.format(tbl)} AND {conv.format('c.COLUMN_NAME')} = {conv.format('x.col')}",
            "WHERE (x.col = '' AND @m4_te = 0) OR (x.col <> '' AND @m4_te = 1 AND c.COLUMN_NAME IS NULL);",
        ]
    return "\n".join(out) + "\n"


def grant_block(reqs) -> str:
    """m4-db-prepare.sql 用：每張表一個受守門的表級 GRANT（表不存在就略過並記錄）。"""
    out = ["-- 由 tools/acceptance/m4-schema-precheck.py --update-sql 產生；請勿手改。來源：tools/db-grants（review.json 套用後）。"]
    for r in reqs:
        privs = ", ".join(r["privs"])
        out.append(f"CALL_M4_GRANT({SQL_VAR[r['group']]}, {sql_quote(r['table'])}, {sql_quote(privs)}, "
                   f"{sql_quote('Y' if (r['optional'] or r['manual']) else 'N')});")
    return "\n".join(out) + "\n"


def expand_grants(block: str) -> str:
    """把 CALL_M4_GRANT(...) 巨集展開成純 SQL（mysql 用戶端沒有巨集，也不建立預存程序）。"""
    out = []
    pat = re.compile(r"^CALL_M4_GRANT\((@\w+), ('(?:[^']|'')*'), ('(?:[^']|'')*'), '([YN])'\);$")
    for line in block.splitlines():
        m = pat.match(line)
        if not m:
            out.append(line)
            continue
        var, tbl, privs, opt = m.groups()
        out += [
            f"SET @m4_t = {tbl}; SET @m4_p = {privs}; SET @m4_s = {var};",
            "SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) COLLATE utf8_general_ci"
            " AND TABLE_NAME = CONVERT(@m4_t USING utf8) COLLATE utf8_general_ci);",
            "SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));",
            "SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');",
            "PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;",
            f"SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '{'(選用)' if opt == 'Y' else ''}', '; ')));",
        ]
    return "\n".join(out) + "\n"


def replace_block(path: Path, name: str, content: str):
    text = path.read_text(encoding="utf-8")
    pat = re.compile(r"(-- BEGIN GENERATED:" + name + r"\n).*?(-- END GENERATED:" + name + r")", re.S)
    if not pat.search(text):
        raise SystemExit(f"{path} 找不到 GENERATED:{name} 標記")
    path.write_text(pat.sub(lambda m: m.group(1) + content + m.group(2), text), encoding="utf-8", newline="\n")


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=str(ROOT))
    for g in GROUPS:
        ap.add_argument("--" + g, default=DEFAULT_DUMPS[g], help=f"{g} 傾印檔（只讀）；寫成 --{g}= 表示略過")
    ap.add_argument("--rules", default=str(HERE / "m4-precheck-rules.json"))
    ap.add_argument("--out", default=str(ROOT / "docs" / "acceptance" / "m4-schema-precheck.md"))
    ap.add_argument("--tsv", default=str(HERE / "generated" / "m4-required-columns.tsv"))
    ap.add_argument("--update-sql", action="store_true", help="更新 m4-db-verify.sql／m4-db-prepare.sql 的 GENERATED 區塊")
    ap.add_argument("--strict", action="store_true", help="有會擋啟動的缺口時退出碼 1")
    args = ap.parse_args(argv)
    root = Path(args.root)
    rules = json.loads(Path(args.rules).read_text(encoding="utf-8")) if Path(args.rules).is_file() else {}
    reqs = requirements(root, rules)
    want_rows = {g: {c["table"] for c in rules.get("row_checks", []) if c["group"] == g} for g in GROUPS}
    dumps = {}
    for g in GROUPS:
        p = getattr(args, g)
        if not p:
            continue
        path = Path(p)
        if not path.is_file():
            raise SystemExit(f"找不到 {g} 傾印檔：{path}（不存在就傳 --{g}= 略過，結果會標成未比對）")
        dumps[g] = parse_dump(path, want_rows[g])
    findings = compare(reqs, dumps)
    row_results = run_row_checks(rules, dumps)
    counts = Counter(r["group"] for r in reqs)
    meta = "、".join(f"{g} {counts[g]} 表" for g in GROUPS) + f"，共 {sum(len(r['privs']) for r in reqs)} 筆表級權限"
    text, total_block = md_report(reqs, dumps, findings, row_results, rules, meta)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text, encoding="utf-8", newline="\n")
    write_tsv(reqs, Path(args.tsv))
    if args.update_sql:
        replace_block(HERE / "m4-db-verify.sql", "required", v2_block(reqs))
        replace_block(HERE / "m4-db-verify.sql", "required_tables", derived_block(reqs, True))
        replace_block(HERE / "m4-db-prepare.sql", "grants", expand_grants(grant_block(reqs)))
    summary = {g: {"missing_tables": len(findings[g]["missing_tables"]),
                   "missing_columns": sum(len(m) for _, m in findings[g]["missing_columns"])}
               for g in GROUPS if findings[g]["compared"]}
    print(json.dumps({"required": dict(counts), "findings": summary, "startup_blocking": total_block,
                      "out": str(out)}, ensure_ascii=False))
    return 1 if (args.strict and total_block) else 0


if __name__ == "__main__":
    sys.exit(main())
