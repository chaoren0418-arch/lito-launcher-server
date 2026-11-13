# Issue #379（T128）：M4 驗收準備——實驗環境預檢與自動化腳本

2026-11-13，本機執行器（claude-local），分支 `task/379`。

## 結論

完成，但有一項請主控決定（見「未完成事項」）。能在開機前查出來的問題已查完，重複性操作寫成腳本放在 `tools/acceptance/`，Codex 的執行順序在 [`tools/acceptance/README.md`](../../tools/acceptance/README.md)（一行一條命令）。本機沒有啟動 Java、沒有連任何資料庫、沒有啟動 P24Lab。

開機前就能確定的四件事：

1. **`380_db.sql` 快照對新 JAR 沒有結構缺口。** 程式需要的 56 張靜態表與欄位都在，包含 #335 提到的 `spawnlist_time`、`系統_受擊無動作gfx`，以及 #284 第二輪卡住的 `裝備_armor_魔法.id`。#284 02:43 裁定的做法（由快照匯入 `p24_380db_lito_exp`）在結構上可行。
2. **`character_lito` 缺 T103b 的 6 個欄位，不補就開不了機。** `380_ac.sql` 參考傾印沒有這 6 欄；`p24_accounts_lito_exp` 是 #83 在 T103b 之前建的，預期相同。新 JAR 會以 `character_lito … lacks ranking columns` 拒絕啟動（`SharedDatabase.java:76-80`、`SettlementSql.java:45-46`）。`m4-db-prepare.sql` 會補。
3. **`380_lito.sql` 快照的 `commands` 沒有 `polyme`、`move`、`item`、`spawn`**（只有名稱是 `1`／`2`／`3`／`de`／`loc` 的五列），`lito_source_server` 指向正式來源庫名。改用這份快照建控制庫時，前者由 `m4-seed-exp.sql` 補、後者由 `m4-db-prepare.sql` 改成只啟用一條、指向 `p24_accounts_lito_exp`。
4. **`tools/db-grants` 的人工設定因為搬檔失效了。** `review.json` 用「`Accounts.java`＋行號」標示啟動區段與 `lito_mysql` 區段，儲存層搬到 `LegacyMySqlRepository.java` 之後全部對不上：直接重產會讓 20 多張表的「啟動即讀」變成「否」。已改成新檔名與行號再重產。

## 未完成事項

- **客戶端啟動腳本 `m4-start-client`（RUNBOOK P5）沒有做。** T127（#378）在本任務進行中合併，它的報告建議「若主控同意」由 T128 加這支腳本。它要啟動 `LiTo.bin`、設三個環境變數、對第二視窗做記憶體內修補；本機規定不能執行也無法驗證，而且不在 T128 原任務範圍。**A03 之後需要遊戲視窗的步驟仍然是「阻塞：P5」**。請主控決定：另開任務（建議 `codex-local`，因為要實機試），或接受 T127 提的另一案（登入器加外部伺服器模式）。
- 近戰武器／防具沒有放進 seed（RUNBOOK D7 的後半）；同血盟（D9）只填 `Clanname`，是否足夠待實機。細節在 README「做不到或沒有做的項目」。

## 做了什麼

### 1. 重產存取清單（#335 記錄的脫節）

- 依 `tools/db-grants/README.md` 重跑 `scan_db_access.py`＋`gen_grants.py --update-doc docs/design/database.md`。
- 審查差異後改了 `tools/db-grants/review.json`：
  - `gated_ranges`、`startup_ranges` 改成 `LegacyMySqlRepository.java:351-485`、`:488-692`、`:238-350`；`SharedDatabase.check` 改 `:70-87`、`SettlementSql.check` 改 `:45-47`。
  - `startup_classes` 加上 `NativeSkillTargetMetadata` 與它呼叫的四個載入器（`CombatDatabase.java:80` → `NativeSkillTargetMetadata.java:8、11`）。所以新增的兩張表是**啟動即讀**：`spawnlist_time` 讀不到會中止啟動；`系統_受擊無動作gfx` 讀不到只記 WARNING（`NativeSourceHitReaction.java:12-15`）。
- 重產後與 main 的實質差異（其餘都是 `Accounts.java` → `LegacyMySqlRepository.java` 的行號變化）：

  | 項目 | 變化 |
  |---|---|
  | 靜態庫 GRANT | 54 → 56 表：加 `spawnlist_time`、`系統_受擊無動作gfx` |
  | `character_lito` 欄位 | 加 `ranking_points`、`disconnects`、`monthly_period`、`monthly_ranking_points`、`monthly_wins`、`monthly_losses` |
  | `skills` 取用欄位 | 加 `area`、`attr`、`castgfx2`、`id`、`ranged`、`target`、`target_to`、`through` |
  | `spawnlist_npc` | 加 `respawn_delay` |
  | `裝備_armor_greater_類型`、`裝備_weapon_greater_類型` | 加 `裝備說明` |

- #335 提醒「新增的兩張表要先確認 CI 靜態庫有」：`db/lito_static.sql:7155`、`:53275` 有這兩張表，CI fixture 的表級 GRANT 不會失敗。
- `docs/design/database.md` 開頭加了一段重產註記：兩個 GENERATED 區塊是新行號，其餘各節內文的 `S/Accounts.java:NNNN` 仍是搬移前的行號（沒有逐一改）。

### 2. `m4-schema-precheck.py` 與執行結果

- 輸入：步驟 1 的存取清單（套用 `review.json` 之後：靜態 56 表、控制 19 表、角色 25 表，104 筆表級權限）＋三份傾印（只讀，沒有複製進 repo）。
- 結果寫在 [`docs/acceptance/m4-schema-precheck.md`](../acceptance/m4-schema-precheck.md)：

  | 庫別 | 傾印 | 缺表 | 缺欄位 | 會擋啟動 |
  |---|---|---:|---:|---|
  | 靜態 | `380_db.sql`（181 表，SHA-256 `0875F7CF…`） | 0 | 0 | 0 項 |
  | 控制 | `380_lito.sql`（19 表，`78B6940C…`） | 0 | 0 | 0 項 |
  | 角色（參考） | `380_ac.sql`（70 表，`F70078A7…`） | 1（選用表 `server_紀錄_獎勵`） | 7 | 1 項：`character_lito` 的 6 欄。另 1 欄 `character_buff.snapshot_version` 程式能容忍（`SourceBuffRead.java:10-23`） |

- 另外檢查：引擎（收據表 InnoDB、`character_lito` MyISAM，都符合）、`lito_resource_*` 剛好 10 張、`lito_resource_release` 的 `qualified_version`＝`0.6.61`、三份傾印都沒有 `CREATE DATABASE`／`USE`／`DEFINER`、資料列（GM 魔杖 42501、11 項消耗品、魔法娃娃 55000 與其能力 102、技能 4／26／29／87／186、商店的 40015／40088 都在）。
- `p24_accounts_lito_exp` 沒有傾印可比：§6 列出 25 張表與欄位，同一份清單產生 `m4-db-verify.sql` 的 V2（每張表一個只讀查詢，對 `information_schema` 逐欄查）。

### 3. `m4-db-prepare.sql`／`m4-db-verify.sql`

- 每個檔案開頭：`SELECT @@port` 斷言 43324、三個 schema 名稱必須以 `_lito_exp` 結尾且互不相同、核對 `@@server_uuid`（#83／#284 記錄的值）。守門失敗時用戶端停在 `Unknown database 'm4_guard__<原因>'`；之後每一句寫入也各自檢查 `@m4_guard`，加了 `--force` 也不會執行。
- prepare：`character_lito` 不存在才建立 → 補 `last_result_id`（同 `db/lito_battle_settlement.sql`）→ 補 6 個積分欄位（同 `db/lito_character_ranking.sql` 的寫法）→ 收據表不存在才建立 → 來源路由只留一條啟用、指向實驗角色庫 → 授權。全部先查 `information_schema`，可重複執行；不刪資料、不撤權、不建立帳號或 schema。
- GRANT 清單由步驟 1 產生（100 張表）。預設靜態庫逐表授權（順便驗證正式部署清單）；`@m4_grant_mode='schema'` 時改授整庫 SELECT（#284 裁定第 2 點）。表不存在就略過並列在輸出。
- verify：只有 SELECT。V1 schema、V2 表與欄位、V3 引擎、V4 資源表與 `lito_room_policy`、V5 路由、V6 GM 指令列、V7 授權（含越界與寫入權限檢查）、V8 筆數基線、V9／V9b 資料前置狀態；最後一列 `M4-VERIFY: PASS／FAIL`。涵蓋 RUNBOOK P4 的全部條件。
- 匯入快照的命令在 README「C. 資料庫」。

### 4. 資料前置（`m4-seed-exp.sql`）

一開始 T127 還沒合併，需求先由 CHECKLIST 與 G02、G20–G25 自行推出；T127 合併後改成對照 RUNBOOK 的 D1–D14，補了同血盟（D9）、可選的第 3／4 個帳號（D13）、變形卷軸（D14 的替代）。每一項的判斷依據（類別與行號）列在 README「資料前置」表，摘要：

| RUNBOOK | 判斷依據 | 寫入 |
|---|---|---|
| D3 GM 資格 | 只看 `accounts.access_level`（`LegacyMySqlRepository.java:707-718`、`SourceAdministration.java:20-33`），指令門檻 200；不看 `characters.AccessLevel` | GM 帳號 `access_level=200` |
| D4 GM 指令 | 以 `name` 查控制庫 `commands`（`LegacyMySqlRepository.java:720-723`、`GmPolyMe.java:23-30`、`GmTravel.java:36-40`、`LiToGmCommands.java:26-35`） | 補 `polyme`／`loc`／`move`／`item`／`spawn`／`de` 中缺少的列 |
| D2 八角色翻頁 | `ORDER BY char_name LIMIT 8`，不看 `character_slot`（`LegacyMySqlRepository.java:969-983`） | 帳號 A 加 7 個角色 |
| D1、D6 等級、龍騎士／幻術師 | `characters.Type`＝5／6（`BattleClassGfx.java:5-6`） | 全部等級 52；`M4DK`（帳號 A）、`M4IL`（帳號 B） |
| D7、D8、D10、D11 技能 | `character_skills` JOIN `skills`，等級 1..28（`LegacyMySqlRepository.java:2688-2691`、`NativeSkillMastery.java:14-23`） | 4、26、29、87、186 與材料 40318、49157 |
| D9 同血盟 | 進場時匯入 `characters.Clanname`（`ClientSession.java:94074` → `GameHub.java:5172-5194`），`sameClan` 比對它（`GameHub.java:5274-5279`） | `M4A4`（A）與 `M4B2`（B）的 `Clanname`＝`M4CLAN` |
| D5、D12、D14 道具 | 背包 JOIN 靜態庫樣板（`LegacyMySqlRepository.java:8992`）；魔杖以 `classname='wand.Storm_Walk'` 查（`GmTravel.java:16-17`） | 魔杖（item_id 執行時從靜態庫查）、55000＋金幣、40015、40088 |
| D13 | — | 可選帳號 C、D 各一個角色 |

- 帳號與密碼全部是佔位符（`__M4_…__`），由私密檔 `scratch/m4/private/m4-vars.sql` 代入（範本 `m4-seed-vars.example.sql`）；repo 內沒有任何帳密。佔位符沒換掉時守門會中止。
- 只新增缺少的列：不 UPDATE、不 DELETE、不改既有帳號的密碼與權限。

### 5. PowerShell 腳本

`m4-prepare.ps1`、`m4-sql-snapshot.ps1`、`m4-sql-diff.ps1`、`m4-listen-check.ps1`、`m4-wal-check.ps1`、`m4-evidence-index.ps1`，共用守門在 `m4-common.ps1`。全部 UTF-8 BOM、不用 `&&`、相容 Windows PowerShell 5.1。

- 新 JAR 的 `UnifiedConfiguration.KEYS` 與 #284 的 22 鍵**完全相同**（`UnifiedConfiguration.java:7`；沒有增減）。`m4-prepare.ps1` 每次會從原始碼重新取出比對，有增減會印出來。
- `m4-sql-diff.ps1` 的判定規則同 `SettlementSql.next`（`SettlementSql.java:29-38`）與 CHECKLIST「戰績預期值」：勝 +250、敗 +50、斷線敗 −50 且 `disconnects+1`、積分最低 0、`version` 每筆 +1、`last_result_id`＝最後一筆新收據、月計同月累加、換月從 0 起算；沒有新收據的角色必須完全沒變。
- `m4-listen-check.ps1` 斷言**每個來源四個監聽**（同 RUNBOOK A02；見「與任務說明不同的地方」）。
- 命名契約照 CHECKLIST：`scratch/m4/run-<UTC>/`、`docs/acceptance/m4-<sha8>/`、`NN-`／`G<編號>-<序>-`／`V<序>-`、`evidence/sql-<before|after>-<步驟>.txt`。
- 共用守門（埠 2000／3306、非 loopback、`config/server.properties`、非 `_lito_exp` 的 schema、非 `scratch\m4\run-<UTC>` 的目錄）一律退出碼非 0。

### 6. CI

新增 `.github/workflows/acceptance-tools.yml`（只在相關路徑變動時執行）：

- `scripts`（windows-latest，Windows PowerShell 5.1）：`m4-selftest.ps1`＋`ci/dry-run.ps1`（假 JAR、手寫快照，跑完 prepare、wal-check、sql-diff、evidence-index 的檔案流程）。
- `sql`（ubuntu，MySQL 8.0、埠 43324、伺服器選項同 P24Lab 的 `mysql.ini`）：守門必須中止 → verify 先 FAIL → prepare 兩次（第二次不得有變化）→ verify PASS → 以實驗帳號核對最小權限 → seed 兩次 → verify → 整庫授權模式 → 用真的 `mysql --batch` 輸出驗證快照 SQL 與 `m4-sql-diff.ps1`。資料是 repo 的 CI fixture 改名，不是 P24Lab 的資料。

CI 找到並已修正的三個問題（都是本機無法發現的）：

| 問題 | 修正 |
|---|---|
| MySQL 8 對不存在的庫回報 `Unknown database`，原本放在表名的守門原因看不到 | 原因改放在庫名 |
| MySQL 8 的 `information_schema` 欄位帶 EXPLICIT 定序（`utf8mb3_tolower_ci`），再加 `COLLATE` 會 `Illegal mix of collations` | I_S 比對不加明確定序 |
| Windows PowerShell 5.1 的 `"a=b".Split(@('='), 2)` 會選到 `StringSplitOptions` 多載而失敗 | 改用 `IndexOf` |

## 怎麼驗證

### 已驗證

- `scan_db_access.py`、`gen_grants.py`、`m4-schema-precheck.py` 在本機實際執行；`--update-sql` 重跑結果與提交內容逐位元組相同。
- 預檢結果的資料列（42501、40088 等）有回頭用文字搜尋傾印檔核對。
- `review.json` 改行號前後的「啟動即讀」逐表比對：修正後與 main 版一致，只多兩張新表。
- README 與本報告引用的類別行號：其中約 45 處用文字搜尋對過原始碼（修正了 3 處）；RUNBOOK 轉引的行號（表內註明「RUNBOOK 引」）沒有另外複核。
- `*.ps1`：Windows PowerShell 5.1 語法解析 0 錯誤；`m4-selftest.ps1` **156／156** 通過（本機 5.1、CI 的 5.1 與 PowerShell 7）。
- 本機實際呼叫各腳本的守門拒絕路徑 14 種（3306、2000、`port+6=3306`、`0.0.0.0`、`380_db`、`config\server.properties`、非 run 目錄…），全部退出碼 1、沒有產生任何檔案。
- CI `acceptance-tools`：見下方「CI 紀錄」。
- P24Lab 的 `mysql.ini`（唯讀讀取，不含帳密）：`port=43324`、`bind-address=127.0.0.1`、`sql-mode=NO_ENGINE_SUBSTITUTION`、`collation-server=utf8mb4_unicode_ci`、`skip-name-resolve`；CI 的 MySQL 用同一組選項。

### 推測

- `p24_accounts_lito_exp` 的結構與 `380_ac.sql` 相同（表數都是 70；#83 在其中成功建立帳號與角色）。沒有連線確認。
- P24Lab 的 `information_schema` 定序行為與 CI 的 MySQL 8.0.46 相同（P24Lab 是 8.0.40，Windows 預設 `lower_case_table_names=1`，CI 也設 1）。
- 伺服器每個來源開四個監聽（`Main.java:71-80`、`SourceLogin.java:42-50`）。T127 與本任務各自讀程式得到同樣的結論，但都沒有實機確認。
- README 用的管理身分用戶端設定檔是 `D:\BinEngine\environments\p24\private-root.cnf` 這一類檔案（只看到檔名，沒有讀內容），格式是否為 `[client]` 區段未確認。
- seed 的角色（等級 52、`Exp=0`、自訂能力值）能正常登入與進場；只填 `Clanname` 就能讓兩個角色被視為同血盟。

### 待驗證（交 Codex，需要 P24Lab 或實機）

1. 三個 SQL 檔對 P24Lab 實際執行（CI 只證明語法、守門與冪等，在 MySQL 8.0.46 與 repo fixture 上）。
2. `380_db.sql` 匯入 P24Lab 後，新 JAR 的各載入器值域檢查是否通過、能否到 `[LiTo] ready`。靜態比對只看表與欄位。
3. `p24_accounts_lito_exp`、`p24_lito_lito_exp` 的實際結構（`m4-db-verify.sql` 的 V2–V6）。
4. `m4-sql-snapshot.ps1` 以 `Start-Process` 重導 `mysql.exe` 的輸入輸出（CI 用 bash 重導驗證了 SQL 與解析，沒有驗證這段 PowerShell 呼叫）。
5. `m4-listen-check.ps1` 對真的 Java 程序：實際是不是四個監聽。
6. `m4-prepare.ps1` 用真的 `target/`（CI 用假 JAR）；`start-server.cmd` 的啟動命令。
7. seed 的資料在遊戲內是否可用：八角色翻頁、龍騎士／幻術師圖示、`.polyme`、GM 魔杖、魔法娃娃、藍水、血之渴望、同血盟。

## 與任務說明不同的地方

- **監聽斷言改成四個。** 任務原文是兩個；T127 依原始碼把 CHECKLIST 02／RUNBOOK A02 改成四個並要求腳本照做，本機獨立讀程式的結論相同。任務原文的判定保留為 `-LegacyTwoOnly`。
- **授權預設逐表**（任務寫「GRANT 清單依步驟 1」），另留整庫模式對應 #284 裁定。
- **多做的**：CI workflow 與 `ci/` 三個檔。理由是本機規定不能實跑，SQL 是最容易在 lease 時間內出錯的部分；實際也抓到三個問題。
- **沒做的**：`m4-start-client`（見「未完成事項」）；`docs/design/database.md` 內文的舊行號沒有逐一更新（只加註記）。

## 產出路徑

- `tools/acceptance/`：`README.md`、`m4-schema-precheck.py`、`m4-precheck-rules.json`、`generated/m4-required-columns.tsv`、`m4-db-prepare.sql`、`m4-db-verify.sql`、`m4-seed-exp.sql`、`m4-seed-vars.example.sql`、`m4-common.ps1`、`m4-prepare.ps1`、`m4-sql-snapshot.ps1`、`m4-sql-diff.ps1`、`m4-listen-check.ps1`、`m4-wal-check.ps1`、`m4-evidence-index.ps1`、`m4-selftest.ps1`、`ci/`
- `docs/acceptance/m4-schema-precheck.md`
- `tools/db-grants/review.json`、`tools/db-grants/generated/*`、`tools/db-grants/README.md`、`docs/design/database.md`
- `.github/workflows/acceptance-tools.yml`
- 本報告

## 給下一棒的提示

- **Codex（#284／T137）**：照 `tools/acceptance/README.md` 的 A–F。`db-verify-2` 必須是 `M4-VERIFY: PASS` 才啟動；不是 PASS 就把 `status=FAIL` 的列貼進報告（去掉帳號與角色名稱）。伺服器回報某張靜態表 `SELECT command denied` 時，那是存取清單漏列：記下表名，改用 `@m4_grant_mode='schema'` 繼續，並回報給主控補 `tools/db-grants`。
- **結算、死亡、斷線測試不要用 GM 帳號**：`access_level≥200` 的帳號受致命傷害會回滿血（`NativePlayerVitals.java:12-30`）。用帳號 A 的 `M4DK` 對帳號 B 的 `M4IL`。
- 改用快照建控制庫時，收據表本來就有 1 筆正式環境的舊收據（不影響實驗，`drainAll` 只處理本來源）。
- **T129**：C 部需要的資料列改 `m4-seed-exp.sql` 的 S3–S5（每段是一個 `UNION ALL` 清單），並在 `ci/run-sql-checks.sh` 更新預期筆數。
- **主控**：(1) `m4-start-client` 由誰做；(2) `tools/db-grants/review.json` 的行號區間是脆弱點，之後 `LegacyMySqlRepository.java` 在 `healthCheck` 之前有大改時要重對（README 已加說明）；(3) `docs/design/database.md` 內文舊行號可另開小任務更新；(4) RUNBOOK「資料前置」表的「現況」欄可以改成指向 `db-verify` 的 V9／V9b。
- `acceptance-tools` workflow 只在 `tools/acceptance/**`、`tools/db-grants/**`、`db/**`、`UnifiedConfiguration.java`、`server.properties.example` 變動時執行；`KEYS` 有增減時它會變紅，提醒同步更新設定範本。

## CI 紀錄

`acceptance-tools` 在本分支開發期間暫時加了 `task/379` 的 push 觸發來跑（最後一個 commit 已拿掉，合併後只在 PR 與 main 觸發）：

| run | 結果 | 說明 |
|---|---|---|
| 43369251908 | 失敗 | `Split` 多載（5.1）；守門原因看不到（MySQL 8） |
| 43369578094 | 失敗 | `Illegal mix of collations`（I_S 的 EXPLICIT 定序） |
| 43370038400 | 失敗 | CI 腳本自己的問題：`mysql … \| grep -q` 在 `pipefail` 下被 SIGPIPE 判失敗（SQL 本身已全數通過） |
| 43370389351 | **成功** | 兩個 job 全綠 |
| 43371299350 | **成功** | 對齊 T127 之後（seed 加 D9、D13、40088；verify 加 V4c、V9b；監聽改四個），commit `2373f8f5`，兩個 job 全綠 |

run 43370389351 的關鍵輸出（43371299350 的步驟相同，seed 的預期筆數改為 5 個帳號、11 個角色、13 列道具）：`M4-SELFTEST: PASS`（5.1 與 7）、`M4-CI-DRY-RUN: PASS`、9 個守門案例全部中止、prepare 前 `M4-VERIFY: FAIL`（110 項）、prepare 後 `M4-VERIFY: PASS`、以實驗帳號執行也 PASS、逐表模式下未列入清單的靜態表讀不到、seed 兩次結果相同、`M4-SQL-DIFF: PASS`（正確結算）與 `FAIL`（竄改勝場）、`M4-CI-SQL: PASS`。

`build` workflow（Server／Launcher）只在 PR 觸發，本分支沒有改 `src/`、`pom.xml` 或登入器；結果以 PR 的檢查為準。
