#!/usr/bin/env python3
"""靜態掃描 LiTo 伺服器 Java 原始碼的資料庫存取（T104 / Issue #302）。

不連資料庫、不執行 Java。做法：
1. 解析 src/main/java 每個 .java 的字串字面值（含 \\uXXXX、text block），依敘述（; { }）分組。
2. 同一敘述內的字面值依序串接成 SQL 文字，擷取 FROM／JOIN／INTO／UPDATE 後的表名與 SELECT／INSERT／UPDATE 欄位。
3. 表名白名單：db/*.sql 傾印中的 CREATE TABLE／INSERT INTO 表名；字面值剛好等於白名單表名、
   且同敘述出現 checkTable／resourceTable／*TABLE* 等表名參數提示時，也算一次存取（抓 checkTable(..., "accounts") 這類）。
4. 從 SharedResourceMain 做類別層級可達性（識別字引用），標出正式 JAR 入口可達的類別。

輸出（固定排序、不含時間戳，可重跑比對）：
  generated/db-access.json  每一筆存取（檔案:行、表、庫別、讀寫、欄位、可達性、偵測方式）
  generated/db-access.tsv   同上的扁平表
用法：py -3 tools/db-grants/scan_db_access.py [--root <repo>]
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
SERVER_REL = Path("lito伺服器/LiTo381-Server")
SRC_REL = SERVER_REL / "src/main/java/local/lito381/server"
DB_REL = SERVER_REL / "db"

# 庫別 → 該庫 schema 來源檔（repo 內）。static=database.static.database（380_db），
# character=每個來源角色庫（380_ac 系列），lito=database.lito.database（380_lito）。
SCHEMA_FILES = {
    "static": ["lito_static.sql"],
    "character": ["380_ac_1.sql"],
    "lito": ["lito_source_server.sql", "lito_battle_settlement.sql", "lito_commands.sql",
             "lito_room_policy.sql", "lito_resource_compact.sql", "380_lito_seed_example.sql",
             "lito_gm_travel.sql", "lito_gm_polyme.sql"],
    "modern": ["modern_match_settlement.sql", "modern_pet_race_settlement.sql", "modern_world_policy.sql"],
}

ENTRY_FORMAL = "SharedResourceMain"
ENTRY_MODERN = "ModernStandaloneMain"

NAME = r"[A-Za-z0-9_\u0080-￿●]+"
TABLE_REF = re.compile(
    r"(?i)\b(FROM|JOIN|INTO|UPDATE|TABLE|DESCRIBE)\s+(?:`?(" + NAME + r")`?\s*\.\s*)?`?(" + NAME + r")`?"
    r"(?:\s+(?:AS\s+)?(?!(?:WHERE|SET|ON|LEFT|RIGHT|INNER|JOIN|LIMIT|ORDER|GROUP|VALUES|USING|WITH|SELECT|UNION|HAVING|FOR|LOCK)\b)([A-Za-z_][A-Za-z0-9_]*))?")
GETTER = re.compile(r"\.get(?:Int|String|Long|Byte|Double|Boolean|Short|Float|Object|BigDecimal|Timestamp|Date|Bytes)\(\s*\"([^\"]+)\"")
TABLE_HINT = re.compile(r"checkTable|resourceTable|exactResourceTable|qualified\s*\(|[A-Z_]*TABLES?[A-Z_]*\b|\btables?\b|Table\s*\(")
SQL_WORDS = {"select", "from", "where", "join", "on", "and", "or", "not", "null", "is", "as", "in", "limit", "order",
             "by", "group", "left", "right", "inner", "outer", "count", "sum", "max", "min", "if", "ifnull", "coalesce",
             "abs", "least", "greatest", "distinct", "case", "when", "then", "else", "end", "asc", "desc", "values",
             "set", "update", "insert", "into", "delete", "duplicate", "key", "like", "between", "exists", "union",
             "all", "true", "false", "current_timestamp", "having", "cast", "unsigned", "signed", "char", "concat",
             "lower", "upper", "length", "left", "substring", "replace", "ignore", "dual", "interval", "now"}


# ---------------------------------------------------------------- schema
def load_schemas(db_dir: Path):
    """回傳 {group: {table: {"columns": [...], "engine": str|None, "file": name, "line": n}}}"""
    out: dict[str, dict[str, dict]] = {}
    create = re.compile(r"CREATE TABLE(?: IF NOT EXISTS)?\s+(?:`?(" + NAME + r")`?\.)?`(" + NAME + r")`\s*\(")
    insert = re.compile(r"INSERT(?: IGNORE)? INTO\s+(?:`?(" + NAME + r")`?\.)?`(" + NAME + r")`")
    for group, files in SCHEMA_FILES.items():
        tables: dict[str, dict] = {}
        for fname in files:
            path = db_dir / fname
            if not path.is_file():
                continue
            lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
            i = 0
            while i < len(lines):
                line = lines[i]
                m = create.search(line)
                if m:
                    name = m.group(2)
                    cols, engine, j = [], None, i + 1
                    while j < len(lines) and not lines[j].lstrip().startswith(")"):
                        cm = re.match(r"\s*`(" + NAME + r")`\s", lines[j])
                        if cm:
                            cols.append(cm.group(1))
                        j += 1
                    if j < len(lines):
                        em = re.search(r"ENGINE\s*=\s*(\w+)", lines[j])
                        engine = em.group(1) if em else None
                    tables[name] = {"columns": cols, "engine": engine, "file": fname, "line": i + 1}
                    i = j
                else:
                    im = insert.search(line)
                    if im and im.group(2) not in tables:
                        tables[im.group(2)] = {"columns": [], "engine": None, "file": fname, "line": i + 1,
                                               "note": "只見 INSERT，未見 DDL"}
                i += 1
        out[group] = tables
    return out


# ---------------------------------------------------------------- java lexer
UNICODE_ESC = re.compile(r"\\u+([0-9a-fA-F]{4})")
STR_ESC = {"n": "\n", "t": "\t", "r": "\r", "b": "\b", "f": "\f", "0": "\0", "s": " ", "'": "'", '"': '"', "\\": "\\"}


def lex(text: str):
    """產生 ('lit', value, line) / ('code', text, line) / ('end', ch, line) 片段。"""
    text = UNICODE_ESC.sub(lambda m: chr(int(m.group(1), 16)), text)
    i, n, line = 0, len(text), 1
    buf, buf_line = [], 1

    def flush():
        nonlocal buf
        if buf:
            yield ("code", "".join(buf), buf_line)
            buf = []

    while i < n:
        c = text[i]
        if c == "\n":
            line += 1
            buf.append(c)
            i += 1
        elif text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            j = n if j < 0 else j + 2
            line += text.count("\n", i, j)
            i = j
        elif text.startswith('"""', i):
            yield from flush()
            start_line = line
            j = text.find('"""', i + 3)
            while j > 0 and text[j - 1] == "\\":
                j = text.find('"""', j + 3)
            j = n if j < 0 else j
            raw = text[i + 3:j]
            line += raw.count("\n")
            body = raw.split("\n", 1)[1] if "\n" in raw else raw
            yield ("lit", re.sub(r"\\(.)", lambda m: STR_ESC.get(m.group(1), m.group(1)), body), start_line)
            i = j + 3
            buf_line = line
        elif c == '"':
            yield from flush()
            j, val = i + 1, []
            while j < n and text[j] != '"':
                if text[j] == "\\" and j + 1 < n:
                    val.append(STR_ESC.get(text[j + 1], text[j + 1]))
                    j += 2
                else:
                    val.append(text[j])
                    j += 1
            yield ("lit", "".join(val), line)
            i = j + 1
            buf_line = line
        elif c == "'":
            j = i + 1
            if j < n and text[j] == "\\":
                j += 2
            else:
                j += 1
            k = text.find("'", j)
            buf.append(" ")
            i = (k + 1) if k >= 0 and k - j < 8 else i + 1
        elif c in ";{}":
            yield from flush()
            yield ("end", c, line)
            i += 1
            buf_line = line
        else:
            if not buf:
                buf_line = line
            buf.append(c)
            i += 1
    yield from flush()


def statements(text: str):
    """把片段依 ; { } 切成敘述；每個敘述 = [(kind, value, line), ...]。"""
    cur = []
    for piece in lex(text):
        if piece[0] == "end":
            if cur:
                yield cur
            cur = []
        else:
            cur.append(piece)
    if cur:
        yield cur


# ---------------------------------------------------------------- SQL extraction
def split_top(s: str):
    depth, cur, parts = 0, [], []
    for ch in s:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(cur))
            cur = []
        else:
            cur.append(ch)
    parts.append("".join(cur))
    return [p.strip() for p in parts if p.strip()]


def idents(expr: str):
    """回傳 expr 中的 (alias, column) 參考；alias 可為 None。"""
    expr = re.sub(r"'[^']*'", " ", expr)
    expr = re.split(r"(?i)\s+AS\s+", expr)[0]
    out = []
    for m in re.finditer(r"`?(" + NAME + r")`?\s*\.\s*`?(" + NAME + r"|\*)`?|`?(" + NAME + r"|\*)`?", expr):
        if m.group(2):
            out.append((m.group(1), m.group(2)))
        else:
            tok = m.group(3)
            if tok == "*" or (not tok.isdigit() and tok.lower() not in SQL_WORDS and not re.match(r"^\d", tok)):
                out.append((None, tok))
    return out


def analyse_sql(sql: str):
    """回傳 [(db_qualifier, table, op, alias, keyword)] 與 {table_or_alias: set(columns)}。"""
    refs = []
    for m in TABLE_REF.finditer(sql):
        kw, qual, table, alias = m.group(1).upper(), m.group(2), m.group(3), m.group(4)
        before = sql[max(0, m.start() - 40):m.start()].upper()
        after = sql[m.end():m.end() + 30]
        if kw == "INTO":
            if not re.search(r"(INSERT|REPLACE)(\s+IGNORE)?\s*$", before):
                continue
            op = "insert"
        elif kw == "UPDATE":
            if alias and alias.upper() == "SET":
                alias = None
            if not (re.match(r"\s*SET\b", after, re.I) or alias):
                continue
            if before.rstrip().endswith(("KEY", "FOR")):
                continue
            op = "update"
        elif kw == "FROM" and re.search(r"DELETE(\s+\w+)?\s*$", before):
            op = "delete"
        elif kw in ("TABLE",):
            if not re.search(r"(LOCK|ALTER|CREATE|DROP|TRUNCATE|RENAME|SHOW\s+CREATE)\s*$", before):
                continue
            op = "ddl:" + re.search(r"(\w+)\s*$", before).group(1).lower()
        else:
            op = "select"
        if table.upper() in ("SELECT", "DUAL") or table.startswith("?"):
            continue
        if alias and alias.upper() in ("SET",):
            alias = None
        refs.append((qual, table, op, alias, kw))

    cols: dict[str, set] = defaultdict(set)
    # SELECT 清單（第一層）
    for sm in re.finditer(r"(?is)\bSELECT\s+(.*?)\s+FROM\b", sql):
        body = sm.group(1)
        if body.upper().startswith("DISTINCT "):
            body = body[9:]
        for part in split_top(body):
            for alias, col in idents(part):
                cols[alias or ""].add(col)
    # INSERT 欄位
    for im in re.finditer(r"(?is)\bINTO\s+(?:`?" + NAME + r"`?\.)?`?(" + NAME + r")`?\s*\(([^)]*)\)", sql):
        for c in split_top(im.group(2)):
            cols["@" + im.group(1)].add(c.strip("` "))
    # UPDATE ... SET a=?,b=?
    for um in re.finditer(r"(?is)\bUPDATE\s+(?:`?" + NAME + r"`?\.)?`?(" + NAME + r")`?(?:\s+\w+)?\s+SET\s+(.*?)(?:\s+WHERE\b|$)", sql):
        for part in split_top(um.group(2)):
            c = part.split("=")[0].strip().strip("`")
            if "." in c:
                c = c.split(".")[-1]
            cols["@" + um.group(1)].add(c)
    # WHERE / ON 等處的 alias.col
    for am in re.finditer(r"\b([A-Za-z_]\w*)\.`?(" + NAME + r")`?", sql):
        cols[am.group(1)].add(am.group(2))
    return refs, cols


# ---------------------------------------------------------------- main scan
def class_refs(code: str, classes: set[str]):
    return {tok for tok in re.findall(r"\b[A-Z][A-Za-z0-9_]*\b", code) if tok in classes}


def reach(graph: dict[str, set], start: str):
    seen, todo = set(), [start]
    while todo:
        c = todo.pop()
        if c in seen or c not in graph:
            continue
        seen.add(c)
        todo.extend(graph[c] - seen)
    return seen


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=str(HERE.parent.parent), help="repo 根目錄")
    ap.add_argument("--out", default=str(HERE / "generated"))
    args = ap.parse_args(argv)
    root = Path(args.root)
    src, dbdir, out = root / SRC_REL, root / DB_REL, Path(args.out)
    review = json.loads((HERE / "review.json").read_text(encoding="utf-8"))
    helpers = review.get("helpers", {})

    schemas = load_schemas(dbdir)
    known: dict[str, list[str]] = defaultdict(list)
    for g, tabs in schemas.items():
        for t in tabs:
            known[t].append(g)

    files = sorted(src.glob("*.java"))
    # 型別名 → 所在檔（一個 .java 可宣告多個型別，例如 Accounts.java 內有 LegacyMySqlRepository）
    decl = re.compile(r"\b(?:class|interface|record|enum)\s+([A-Z][A-Za-z0-9_]*)")
    owner: dict[str, str] = {}
    for f in files:
        owner.setdefault(f.stem, f.stem)
        body = f.read_text(encoding="utf-8", errors="replace")
        body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\\n])*\"", " ", body, flags=re.S)
        for m in decl.finditer(body):
            owner.setdefault(m.group(1), f.stem)
    classes = set(owner)
    graph: dict[str, set] = {}
    records = []
    getters: dict[str, set] = defaultdict(set)
    for f in files:
        text = f.read_text(encoding="utf-8", errors="replace")
        rel = f.relative_to(root).as_posix()
        code_all = []
        file_helpers = helpers.get(f.name, {})
        for st in statements(text):
            code = " ".join(v for k, v, _ in st if k == "code")
            code_all.append(code)
            for gm in GETTER.finditer("".join(('"%s"' % v) if k == "lit" else v for k, v, _ in st)):
                getters[f.stem].add(gm.group(1))
            lits = []
            for k, v, ln in st:
                if k == "lit":
                    lits.append((v, ln, False))
                else:
                    for pat, table in file_helpers.items():
                        for hm in re.finditer(pat, v):
                            lits.append((" `" + table + "` ", ln + v.count("\n", 0, hm.start()), True))
            if not lits:
                continue
            # %s 代入：text block 後面的表名字面值依序填入
            parts = [v for v, _, _ in lits]
            pos = next((i for i, p in enumerate(parts) if "%s" in p), None)
            if pos is not None:
                head, tail = parts[:pos + 1], parts[pos + 1:]
                fill = [p for p in tail if p in known]
                joined = " ".join(head + [p for p in tail if p not in known])
                for t in fill:
                    joined = joined.replace("%s", "`" + t + "`", 1)
                sql = joined
            else:
                sql = " ".join(parts)
            # resourceDatabase + ".`表`" 這種寫法：庫名在變數裡，串起來會變成 "FROM  .`表`"
            sql = re.sub(r"(?i)\b(FROM|JOIN|INTO|UPDATE)\s+\.", r"\1 ", sql)
            first_line = lits[0][1]
            # JOIN 片段常放在變數（例如 stateJoins），所以 FROM／JOIN 也觸發分析；
            # 不在白名單的表名只在字面值以 SQL 動詞開頭時保留，避免英文訊息（"update failed"）誤判。
            refs, cols = analyse_sql(sql) if re.search(
                r"(?i)\b(select|insert|update|delete|replace|alter|create|drop|truncate|lock|join|from)\b", sql) else ([], {})
            has_verb = any(re.match(r"(?i)\s*(select|insert|update|delete|replace|lock|alter|create)\b", v)
                           for v, _, _ in lits)
            seen_tables = set()
            for qual, table, op, alias, kw in refs:
                if table not in known and not table.startswith(("lito_", "character_")) and (qual or "").lower() != "information_schema":
                    if not has_verb or table.upper() == table or len(table) < 2:
                        continue
                tcols = set(cols.get("@" + table, set()))
                if alias:
                    tcols |= cols.get(alias, set())
                single = len({r[1] for r in refs}) == 1
                if single or not alias:
                    tcols |= {c for c in cols.get("", set()) if (table not in known) or c == "*" or
                              any(c in schemas[g].get(table, {}).get("columns", []) for g in known[table]) or
                              not any(schemas[g].get(table, {}).get("columns") for g in known[table])}
                if single:
                    tcols |= cols.get(table, set())
                line = next((ln for v, ln, _ in lits if table in v), first_line)
                records.append({"file": rel, "class": f.stem, "line": line, "table": table,
                                "qualifier": qual or "", "op": op, "columns": sorted(tcols),
                                "method": "sql", "sql": re.sub(r"\s+", " ", sql).strip()[:400]})
                seen_tables.add(table)
            if refs:  # SQL 敘述裡其他反引號白名單表名（例如子查詢、動態 FROM）
                for bt in re.findall(r"`(" + NAME + r")`", sql):
                    if bt in known and bt not in seen_tables:
                        line = next((ln for v, ln, _ in lits if bt in v), first_line)
                        records.append({"file": rel, "class": f.stem, "line": line, "table": bt, "qualifier": "",
                                        "op": "select", "columns": [], "method": "sql-backtick",
                                        "sql": re.sub(r"\s+", " ", sql).strip()[:400]})
                        seen_tables.add(bt)
            for v, ln, virtual in lits:
                if virtual or v not in known or v in seen_tables:
                    continue
                hint = "table-arg" if TABLE_HINT.search(code) else "string-only"
                records.append({"file": rel, "class": f.stem, "line": ln, "table": v, "qualifier": "",
                                "op": "select" if hint == "table-arg" else "?", "columns": [],
                                "method": "whitelist:" + hint, "sql": re.sub(r"\s+", " ", code).strip()[:200]})
        graph[f.stem] = {owner[c] for c in class_refs(" ".join(code_all), classes)} - {f.stem}

    formal = reach(graph, ENTRY_FORMAL)
    modern = reach(graph, ENTRY_MODERN)
    shared_tables = set(review.get("routed_to_lito", []))
    db_override = review.get("db_overrides", {})

    def group_of(r):
        q = r["qualifier"].lower()
        if q == "information_schema":
            return "information_schema"
        t = r["table"]
        if t in db_override:
            return db_override[t]
        if t in shared_tables:
            return "lito"
        gs = known.get(t, [])
        for g in ("lito", "character", "static", "modern"):
            if g in gs:
                return g
        if t.startswith("lito_"):
            return "lito?"
        return "unknown"

    for r in records:
        r["db"] = group_of(r)
        r["also_in"] = [g for g in known.get(r["table"], []) if g != r["db"]]
        r["reach"] = ("formal" if r["class"] in formal else "") + ("+modern" if r["class"] in modern else "")
        r["reach"] = r["reach"].lstrip("+") or "unreached"
        sch = None
        for g in ([r["db"]] + r["also_in"]):
            sch = schemas.get(g, {}).get(r["table"]) or sch
            if sch:
                break
        r["in_schema"] = bool(sch)
        r["engine"] = (sch or {}).get("engine") or ""
        if "*" in r["columns"] and sch and sch.get("columns"):
            r["getter_columns"] = sorted(getters[r["class"]] & set(sch["columns"]))
        if sch and sch.get("columns"):
            r["unknown_columns"] = sorted(c for c in r["columns"] if c != "*" and c not in sch["columns"])
    records.sort(key=lambda r: (r["db"], r["table"], r["file"], r["line"], r["op"]))

    out.mkdir(parents=True, exist_ok=True)
    meta = {"generator": "tools/db-grants/scan_db_access.py", "source": SRC_REL.as_posix(),
            "java_files": len(files), "formal_reachable_classes": len(formal),
            "modern_reachable_classes": len(modern),
            "schema_tables": {g: len(t) for g, t in schemas.items()}}
    # 一筆一行，方便 diff；不含時間戳，重跑結果相同
    body = ",\n".join(json.dumps(r, ensure_ascii=False, sort_keys=True) for r in records)
    (out / "db-access.json").write_text(
        '{"meta": ' + json.dumps(meta, ensure_ascii=False, sort_keys=True) + ',\n"records": [\n' + body + "\n]}\n",
        encoding="utf-8", newline="\n")
    with (out / "db-access.tsv").open("w", encoding="utf-8", newline="\n") as fh:
        fh.write("db\ttable\top\treach\tmethod\tfile:line\tcolumns\tin_schema\tengine\n")
        for r in records:
            fh.write("\t".join([r["db"], r["table"], r["op"], r["reach"], r["method"],
                                f'{r["file"].split("/")[-1]}:{r["line"]}',
                                ",".join(r["columns"] + [f"get:{c}" for c in r.get("getter_columns", [])]),
                                "Y" if r["in_schema"] else "N", r["engine"]]) + "\n")
    print(json.dumps(meta, ensure_ascii=False))
    print(f"records={len(records)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
