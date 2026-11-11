#!/usr/bin/env python3
"""Build the CI-only MySQL fixture (Issue #334 / T119).

Inputs are repository files only:
  db/lito_static.sql            -> static database `380_db` (shared game templates, no accounts)
  db/380_ac_1.sql               -> schema ONLY for the source character databases;
                                   every INSERT (real player rows) is dropped
  db/lito_*.sql, 380_lito_seed_example.sql, lito_resource_compact.sql
                                -> LiTo control database `380_lito`
  db/ci/fake_source_data.sql    -> a handful of fake accounts/characters per source
  tools/db-grants/generated/grants.sql
                                -> production least-privilege grants, rewritten for the CI user

Output: numbered .sql files in --out (a runner temp directory, never committed).
The CI credentials are fixed, non-secret test values passed on the command line.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

DB = Path(__file__).resolve().parent.parent  # lito伺服器/LiTo381-Server/db
CI = DB / "ci"
REPO = DB.parent.parent.parent  # repository root
STATIC = "380_db"
CONTROL = "380_lito"
# Mirrors db/lito_source_server.sql seed: three enabled sources.
SOURCES = [(1, "太陽神", "380_ac"), (2, "月亮神", "380_ac_1"), (3, "伺服器3", "380_ac_2")]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")


def create_statement(text: str, table: str) -> str:
    """Return the single CREATE TABLE statement for `table` (no data)."""
    pattern = re.compile(
        r"CREATE TABLE (?:IF NOT EXISTS )?(?:`[^`]+`\.)?`" + re.escape(table) + r"`.*?;\n",
        re.S)
    found = pattern.findall(text)
    if len(found) != 1:
        sys.exit(f"expected one CREATE TABLE {table}, found {len(found)}")
    return found[0]


def static_database() -> str:
    text = read(DB / "lito_static.sql")
    head, sep, rest = text.partition("\n/*")
    if head.count("`lito_static`") != 2:
        sys.exit("lito_static.sql header changed; refusing to rename blindly")
    return head.replace("`lito_static`", f"`{STATIC}`") + sep + rest


def source_schema(database: str) -> str:
    text = read(DB / "380_ac_1.sql")
    out: list[str] = []
    dropped = 0
    header_seen = 0
    for line in text.splitlines(keepends=True):
        if line.startswith("INSERT INTO"):
            if not line.rstrip().endswith(";"):
                sys.exit("multi-line INSERT in 380_ac_1.sql; schema extraction unsafe")
            dropped += 1
            continue
        if header_seen < 2 and "`380_ac_1`" in line:
            line = line.replace("`380_ac_1`", f"`{database}`")
            header_seen += 1
        out.append(line)
    if header_seen != 2:
        sys.exit("380_ac_1.sql header changed; refusing to rename blindly")
    if dropped == 0:
        sys.exit("no INSERT rows found; input format changed")
    body = "".join(out)
    if re.search(r"^\s*(INSERT|REPLACE)\b", body, re.M | re.I):
        sys.exit("player rows survived schema extraction")
    # character_lito: same definition as 1-伺服器/db/380_ac.sql:983-992.
    body += f"""
USE `{database}`;
CREATE TABLE IF NOT EXISTS `character_lito` (
  `char_obj_id` int(10) UNSIGNED NOT NULL,
  `char_name` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `wins` bigint(20) UNSIGNED NOT NULL DEFAULT 0,
  `losses` bigint(20) UNSIGNED NOT NULL DEFAULT 0,
  `version` bigint(20) UNSIGNED NOT NULL DEFAULT 0,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`char_obj_id`) USING BTREE
) ENGINE = MyISAM CHARACTER SET = utf8 COLLATE = utf8_general_ci;
"""
    fake = read(CI / "fake_source_data.sql").replace("@@SOURCE_DB@@", database)
    return body + "\n" + fake


def control_database() -> str:
    parts = [f"CREATE DATABASE IF NOT EXISTS `{CONTROL}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n",
             f"USE `{CONTROL}`;\nSET NAMES utf8mb4;\n"]
    static = read(DB / "lito_static.sql")
    # Same five-table layout that 380_lito_seed_example.sql expects.
    for table in ("lito_tournament_shop", "lito_battle_rule", "lito_battle_object", "lito_battle_spawn"):
        parts.append(create_statement(static, table).replace("ENGINE=MyISAM", "ENGINE=InnoDB"))
    # Column set of 1-伺服器/db/380_lito.sql:94-104 (not present in this repo's SQL).
    parts.append("""CREATE TABLE IF NOT EXISTS `lito_battle_player_spawn` (
  `mode_id` tinyint(3) UNSIGNED NOT NULL,
  `map_id` smallint(5) UNSIGNED NOT NULL,
  `spawn_index` tinyint(3) UNSIGNED NOT NULL,
  `x` smallint(5) UNSIGNED NOT NULL,
  `y` smallint(5) UNSIGNED NOT NULL,
  `enabled` tinyint(3) UNSIGNED NOT NULL DEFAULT 1,
  `note` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`mode_id`, `map_id`, `spawn_index`),
  INDEX `idx_player_spawn_map` (`map_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
""")
    parts.append(read(DB / "380_lito_seed_example.sql"))

    source_sql = read(DB / "lito_source_server.sql")
    parts.append(create_statement(source_sql, "lito_source_server"))
    rows = ",".join(f"({i},'{name}','{db}',1)" for i, name, db in SOURCES)
    rest = ",".join(f"({i},'伺服器{i}',NULL,0)" for i in range(len(SOURCES) + 1, 9))
    parts.append(f"INSERT INTO `{CONTROL}`.`lito_source_server` "
                 f"(`source_id`,`display_name`,`character_database`,`enabled`) VALUES {rows},{rest};\n")

    parts.append(read(DB / "lito_room_policy.sql"))
    parts.append(read(DB / "lito_commands.sql"))
    parts.append(f"USE `{CONTROL}`;\n" + read(DB / "lito_resource_compact.sql"))

    settlement = read(DB / "lito_battle_settlement.sql")
    if settlement.count("SET @lito_source = '380_ac_1';") != 1:
        sys.exit("lito_battle_settlement.sql migration block changed")
    create, _, migration = settlement.partition("SET @lito_source = '380_ac_1';")
    parts.append(create)
    for _, _, db in SOURCES:
        parts.append(f"SET @lito_source = '{db}';" + migration.split("-- 現場已")[0])
    return "\n".join(parts)


def grants(user: str, password: str) -> str:
    text = read(REPO / "tools" / "db-grants" / "generated" / "grants.sql")
    account = f"'{user}'@'%'"
    text = text.replace("'lito_shared'@'127.0.0.1'", account)
    text = text.replace("'<由營運設定>'", "'" + password.replace("'", "''") + "'")
    lines = text.splitlines(keepends=True)
    out = []
    for line in lines:
        if line.startswith("GRANT") and "`380_ac`." in line:
            for _, _, db in SOURCES:
                out.append(line.replace("`380_ac`.", f"`{db}`."))
        else:
            out.append(line)
    return "".join(out) + "FLUSH PRIVILEGES;\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    files = {"10-static.sql": static_database()}
    for index, (_, _, db) in enumerate(SOURCES):
        files[f"2{index}-source-{db}.sql"] = source_schema(db)
    files["30-control.sql"] = control_database()
    files["90-grants.sql"] = grants(args.user, args.password)
    for name, text in files.items():
        (args.out / name).write_text(text, encoding="utf-8")
        print(f"wrote {args.out / name} ({len(text.encode('utf-8'))} bytes)")


if __name__ == "__main__":
    main()
