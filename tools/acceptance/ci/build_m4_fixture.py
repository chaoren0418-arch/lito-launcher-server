#!/usr/bin/env python3
"""CI 專用：把 repo 內的 CI fixture（db/ci/build_ci_database.py）改成 M4 實驗 schema 名稱（T128 / Issue #379）。

用途只有一個：讓 .github/workflows/acceptance-tools.yml 在拋棄式 MySQL 8.0（埠 43324）上實際執行
m4-db-prepare.sql／m4-db-verify.sql／m4-seed-exp.sql，確認語法與守門邏輯。不是 P24Lab 的資料，也不用在實機。

輸出（--out，runner 暫存目錄，不進 git）：
  10-static.sql   db/lito_static.sql        → `p24_380db_lito_exp`
  20-source.sql   db/380_ac_1.sql 只取結構  → `p24_accounts_lito_exp`（外加 CI 假帳號兩組）
  30-control.sql  db/lito_*.sql              → `p24_lito_lito_exp`
  40-regress.sql  把 character_lito 退回 T103b 之前、路由改回正式庫名、刪掉兩個 GM 指令列，
                  製造 m4-db-prepare.sql 與 m4-seed-exp.sql 要處理的狀態
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
sys.path.insert(0, str(REPO / "lito伺服器" / "LiTo381-Server" / "db" / "ci"))
import build_ci_database as ci  # noqa: E402

STATIC = "p24_380db_lito_exp"
CONTROL = "p24_lito_lito_exp"
SOURCE = "p24_accounts_lito_exp"

REGRESS = f"""-- CI only: put the fixture back into the state the experiment databases are expected to be in before T128's scripts run.
ALTER TABLE `{SOURCE}`.`character_lito`
  DROP COLUMN ranking_points, DROP COLUMN disconnects, DROP COLUMN monthly_period,
  DROP COLUMN monthly_ranking_points, DROP COLUMN monthly_wins, DROP COLUMN monthly_losses, DROP COLUMN last_result_id;
UPDATE `{CONTROL}`.`lito_source_server` SET character_database = '380_ac_1', enabled = 1 WHERE source_id = 1;
INSERT INTO `{CONTROL}`.`lito_source_server` (source_id, display_name, character_database, enabled) VALUES (2, 'CI-2', '380_ac_2', 1)
  ON DUPLICATE KEY UPDATE character_database = '380_ac_2', enabled = 1;
DELETE FROM `{CONTROL}`.`commands` WHERE `name` IN ('item', 'spawn');
"""


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    ci.STATIC, ci.CONTROL = STATIC, CONTROL
    ci.SOURCES = [(1, "太陽神", SOURCE)]
    ci.INNODB_CHARACTER_LITO = {}
    control = ci.control_database()
    for old, new in (("`380_lito`", f"`{CONTROL}`"), ("`380_db`", f"`{STATIC}`")):
        control = control.replace(old, new)
    for forbidden in ("`380_lito`", "`380_db`", "`380_ac"):
        if forbidden in control:
            sys.exit(f"control fixture still references {forbidden}")
    files = {
        "10-static.sql": ci.static_database(),
        "20-source.sql": ci.source_schema(SOURCE),
        "30-control.sql": control,
        "40-regress.sql": REGRESS,
    }
    args.out.mkdir(parents=True, exist_ok=True)
    for name, text in files.items():
        (args.out / name).write_text(text, encoding="utf-8", newline="\n")
        print(f"wrote {args.out / name} ({len(text.encode('utf-8'))} bytes)")


if __name__ == "__main__":
    main()
