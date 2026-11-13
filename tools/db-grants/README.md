# tools/db-grants — 由程式掃描產生資料庫權限清單（T104）

說明與結果在 [`docs/design/database.md`](../../docs/design/database.md)。全部是靜態分析：不連資料庫、不執行 Java。

| 檔案 | 作用 |
|---|---|
| `scan_db_access.py` | 掃 `lito伺服器/LiTo381-Server/src/main/java` 的 SQL 字串與表名白名單 → `generated/db-access.{json,tsv}` |
| `review.json` | 人工核對：排除 Modern*／`lito_mysql` 區段／誤判字串、寫入白名單、正式 JAR 才有的讀取、選用表。每項附行號 |
| `gen_grants.py` | 套用 `review.json` → `generated/grants.sql`、`table-inventory.tsv`、`excluded.tsv`；`--update-doc` 更新設計文件；`--ref-schema group=path` 比對其他傾印（例如 1-伺服器 現行 380_db）缺表／缺欄位 |
| `jar_tables.py` | 讀正式 JAR 的 class 常數池，列出「JAR 有、清單沒有」與「清單有、JAR 沒有」 |

```
py -3 tools/db-grants/scan_db_access.py
py -3 tools/db-grants/gen_grants.py --update-doc docs/design/database.md
py -3 tools/db-grants/jar_tables.py <正式 JAR>
```

改了伺服器 SQL 之後重跑前兩行；`generated/` 有變化就要一起提交（輸出不含時間戳，同一份原始碼重跑結果相同）。

`generated/` 或 `review.json` 有變化時，再跑 `py -3 tools/acceptance/m4-schema-precheck.py --update-sql`（T128）：M4 實驗庫的核對與授權腳本（`tools/acceptance/m4-db-verify.sql`、`m4-db-prepare.sql`）的需求清單由同一份資料產生，CI 的 `acceptance-tools` 會檢查兩邊是否同步。

`review.json` 的 `gated_ranges`、`startup_ranges` 是以「檔名＋行號區間」比對的：對應的方法搬檔或前面插入大量程式後要跟著改，否則「啟動即讀」會全部變成「否」、`lito_mysql` 區段的排除也會失效（2026-11-13 `Accounts.java` → `LegacyMySqlRepository.java` 就發生過，T128 已修）。
