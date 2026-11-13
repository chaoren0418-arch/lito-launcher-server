# tools/acceptance — M4 實驗環境預檢與自動化腳本（T128／Issue #379）

給 Codex 在 P24Lab 實驗環境跑 M4 驗收（#284、#285、T137、T138）用。開機前能查的先查，重複性操作寫成腳本。
步驟內容與預期畫面在 [`docs/acceptance/M4-RUNBOOK.md`](../../docs/acceptance/M4-RUNBOOK.md)（T127）；本目錄對應它的 P3、P4、P6、P7、A02、A15、A16、A20 與「資料前置」D1–D14。
結果文件：[`docs/acceptance/m4-schema-precheck.md`](../../docs/acceptance/m4-schema-precheck.md)；任務報告：[`docs/reports/issue-379.md`](../../docs/reports/issue-379.md)。

## 驗證狀態（先看這一段）

| 項目 | 狀態 |
|---|---|
| `m4-schema-precheck.py` | **已在本機執行**（python 靜態，讀 `D:\BinEngine\environments\p24\sql\` 三份傾印） |
| `*.ps1` | 本機只做語法解析與 `m4-selftest.ps1`（純函式 156 項）；只碰檔案的流程在 CI 的 Windows PowerShell 5.1 用假檔案跑過（`ci/dry-run.ps1`） |
| `m4-db-prepare.sql`、`m4-db-verify.sql`、`m4-seed-exp.sql` | 只在 CI 的拋棄式 MySQL 8.0（埠 43324、伺服器選項同 P24Lab `mysql.ini`、資料是 repo 的 CI fixture）跑過（`ci/run-sql-checks.sh`）；**沒有對 P24Lab 執行過** |
| 連 P24Lab、啟動 Java、`m4-sql-snapshot.ps1` 實際連線、`m4-listen-check.ps1` 對真的 Java 程序 | **未實跑**，由 Codex 執行 |

## 檔案

| 檔案 | 作用 | 會不會寫東西 |
|---|---|---|
| `m4-schema-precheck.py` | 靜態比對：`tools/db-grants` 的存取清單 × 三份 SQL 傾印 → 缺表／缺欄位／資料列檢查；`--update-sql` 同步更新兩個 SQL 檔的 GENERATED 區塊 | 只寫 repo 內的結果檔 |
| `m4-precheck-rules.json` | 預檢的人工規則：可容忍欄位、判讀備註、資料列檢查（每項附類別與行號） | — |
| `generated/m4-required-columns.tsv` | 每個「庫別／表／欄位」一列的需求清單 | — |
| `m4-db-verify.sql` | 連線核對（只有 SELECT）：schema、表與欄位、引擎、資源表、來源路由、GM 指令列、實驗帳號權限、資料前置狀態；最後一列 `M4-VERIFY: PASS／FAIL` | 否 |
| `m4-db-prepare.sql` | 補 `character_lito` 欄位、確認收據表、來源路由指向實驗角色庫、授權實驗帳號 | 只寫 `*_lito_exp` |
| `m4-seed-exp.sql`＋`m4-seed-vars.example.sql` | 資料前置（RUNBOOK D1–D14）：GM 帳號、八角色、龍騎士／幻術師、技能、同血盟、道具、GM 指令列、可選的第 3／4 個帳號 | 只寫 `*_lito_exp` |
| `m4-prepare.ps1` | 建 `scratch/m4/run-<UTC>/`：複製 JAR 與 lib、逐檔 SHA-256、空的 `runtime/`、`evidence/`、不含帳密的設定範本、22 鍵對照 | 只寫 `scratch/m4/` |
| `m4-sql-snapshot.ps1` | 戰績／收據快照（只有 SELECT）→ `evidence/sql-<Label>.txt` | 只寫 run 目錄 |
| `m4-sql-diff.ps1` | 比對兩份快照，判定勝敗、積分、收據 | 只寫 run 目錄 |
| `m4-listen-check.ps1` | 列出 Java PID 的監聽位址並核對（每個來源四個，同 RUNBOOK A02） | 只寫 run 目錄 |
| `m4-wal-check.ps1` | 列 `runtime/battle-settlements/`，回報 `.pending`／`.tmp` | 只寫 run 目錄 |
| `m4-evidence-index.ps1` | 依命名契約掃截圖與證據，產生結果檔骨架，缺檔列「未執行」 | `docs/acceptance/m4-<sha8>.md`（不存在時） |
| `m4-common.ps1`、`m4-selftest.ps1` | 共用守門函式與自我檢查 | 否 |
| `ci/` | 只給 GitHub Actions 用（`.github/workflows/acceptance-tools.yml`） | — |

## 共用守門

任何腳本遇到下列情況一律拒絕，退出碼非 0：

- 埠 **2000／3306**（含 `port`＋1…8 落在這兩個埠）；資料庫埠不是 **43324**。
- **非 loopback** 位址（只接受 127.0.0.0/8、`::1`、`localhost`）。
- 路徑是 **`config/server.properties`**（`.example` 可以讀）。
- schema 名稱不以 **`_lito_exp`** 結尾；三個 SQL 檔另外用 `SELECT @@port` 斷言 43324、核對 `@@server_uuid`（#83／#284 記錄的 `e3443e40-d090-11f1-8040-4cedfb679789`）。
- run 目錄不是 `…\scratch\m4\run-<UTC>`。

SQL 檔守門失敗時，mysql 用戶端會停在 `ERROR 1049 … Unknown database 'm4_guard__<原因>'`（原因寫在這個不存在的庫名裡，例如 `m4_guard__port_is_not_43324_not_p24lab`、`m4_guard__schemas_must_be_distinct_and_end_with_lito_exp`）。**不要加 `--force`**；即使加了，後面每一句寫入也各自檢查 `@m4_guard`，不會執行。

## Codex 的執行順序（一行一條命令）

前置（本目錄不提供）：向調度器取得 `p24lab` lease；依 #83／#284 既有流程冷備份並啟動 P24Lab（`127.0.0.1:43324`）。
工作目錄：本次任務的 worktree 根目錄；以下全部在 **Windows PowerShell** 輸入。有 `<…>` 的地方先換成實際值。

**A. 變數**

```powershell
$mysql = 'D:\BinEngine\environments\p24\mysql\bin\mysql.exe'
$admin = '<P24Lab 管理身分的 mysql 用戶端設定檔，[client] 區段含 user／password；不要印出內容>'
$conn = "--defaults-extra-file=$admin --protocol=TCP --host=127.0.0.1 --port=43324 --default-character-set=utf8mb4"
$vars = 'scratch\m4\private\m4-vars.sql'
```

**B. 建 JAR 與 run 目錄**

```powershell
mvn -B -DskipTests -Dmaven.test.skip=true -f "lito伺服器\LiTo381-Server\pom.xml" package
powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-prepare.ps1
$run = '<上一行印出的 Run= 路徑>'
New-Item -ItemType Directory -Force scratch\m4\private
Copy-Item tools\acceptance\m4-seed-vars.example.sql $vars
notepad $vars
```

`$vars` 是私密檔（`scratch/` 被 git 忽略）：把 `__M4_…__` 換成實際值；帳號 A、B 取自 #83 私密交接檔，GM 帳號自訂；帳號 C、D（RUNBOOK D13，2 對 2 用）不需要就維持佔位符。存檔用 UTF-8（無 BOM 較保險）。要改用 `380_lito.sql` 快照建的控制庫時，在檔內加一行 `SET @m4_lito = 'p24_380lito_lito_exp';`，並在上面 `m4-prepare.ps1` 加 `-LitoDatabase p24_380lito_lito_exp`。

**C. 資料庫（管理身分）**

靜態庫快照只在 `p24_380db_lito_exp` 還不存在時匯入（#284 主控 2026-11-07 02:43 裁定；傾印檔內沒有 `CREATE DATABASE`／`USE`，目標完全由命令列決定）：

```powershell
& $mysql "--defaults-extra-file=$admin" --protocol=TCP --host=127.0.0.1 --port=43324 -e "CREATE DATABASE p24_380db_lito_exp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
cmd /c "$mysql $conn p24_380db_lito_exp < D:\BinEngine\environments\p24\sql\380_db.sql"
```

第一行在 schema 已存在時會失敗（`ERROR 1007`），這時**不要**執行第二行（傾印含 `DROP TABLE IF EXISTS`）。然後：

```powershell
cmd /c "type $vars tools\acceptance\m4-db-verify.sql | $mysql $conn --table > $run\evidence\db-verify-1.txt"
cmd /c "type $vars tools\acceptance\m4-db-prepare.sql | $mysql $conn --table > $run\evidence\db-prepare.txt"
cmd /c "type $vars tools\acceptance\m4-db-verify.sql | $mysql $conn --table > $run\evidence\db-verify-2.txt"
cmd /c "type $vars tools\acceptance\m4-seed-exp.sql | $mysql $conn --table > $run\evidence\db-seed.txt"
cmd /c "type $vars tools\acceptance\m4-db-verify.sql | $mysql $conn --table > $run\evidence\db-verify-3.txt"
Select-String -Path $run\evidence\db-verify-3.txt -Pattern 'M4-VERIFY|FAIL|WARN'
```

- 每一行執行完看 `$LASTEXITCODE`：非 0 就停，把主控台的錯誤訊息記進報告。
- `db-verify-1` 預期是 FAIL（缺 `character_lito` 新欄位、缺授權…）；`db-verify-2` 必須是 **`M4-VERIFY: PASS`** 才可以啟動伺服器。
- `db-verify-*.txt` 不含密碼，但含帳號名稱與角色名稱：留在 run 目錄，不要原樣貼進公開報告。
- 靜態庫授權預設逐表（同正式部署清單 `tools/db-grants/generated/grants.sql`）。伺服器若回報某張靜態表 `SELECT command denied`：把表名記進報告（代表存取清單漏列），在 `$vars` 加 `SET @m4_grant_mode = 'schema';` 後重跑 prepare（#284 02:43 裁定第 2 點的整庫 SELECT）。
- `p24_lito_lito_exp` 在 `db-verify-2` 仍有 V2／V4 的 FAIL（結構不相容）時，依 #284 裁定第 4 點改用快照：`CREATE DATABASE p24_380lito_lito_exp …`、匯入 `380_lito.sql`、在 `$vars` 設 `@m4_lito`，再從 `db-verify-1` 那一行重跑。

**D. 設定檔與啟動**

```powershell
Copy-Item $run\config\experiment.properties.template $run\config\experiment.properties
notepad $run\config\experiment.properties
notepad $run\config\mysql-client.cnf
```

`experiment.properties` 只改 `database.user`、`database.password` 兩行（值取自 #83 的 `isolated-test.properties`）。`mysql-client.cnf` 內容是三行：`[client]`、`user=<實驗帳號>`、`password=<實驗帳號密碼>`（給快照用）。兩個檔都在 run 目錄，不進 git。

另開一個 PowerShell 視窗啟動伺服器（主控台輸出同時存證據；120 秒內沒有 `[LiTo] ready` 就 Ctrl+C）：

```powershell
Set-Location $run
cmd /c start-server.cmd | Tee-Object -FilePath evidence\server-console.log
```

回到原視窗：

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*LiTo381-Server.jar*' } | Select-Object ProcessId, CommandLine
powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-listen-check.ps1 -ProcessId <PID> -SourceId <V5 列出的 enabled source_id> -RunDirectory $run
```

**E. 每個會改戰績的步驟（CHECKLIST 15、16；RUNBOOK 指定的項目）**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-sql-snapshot.ps1 -RunDirectory $run -Label before-15 -DefaultsFile $run\config\mysql-client.cnf
powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-sql-snapshot.ps1 -RunDirectory $run -Label after-15 -DefaultsFile $run\config\mysql-client.cnf
powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-sql-diff.ps1 -RunDirectory $run -Step 15 -ExpectWinner <勝者角色名> -ExpectLoser <敗者角色名>
```

`before` 在開打前、`after` 在結算畫面出現後執行。Label 的步驟部分照命名契約寫 `NN`、`G<編號>-<序>` 或 `V<序>`（例：`before-G24-1`）；RUNBOOK P7 的基線是 `-Label before-00`。控制庫不是 `p24_lito_lito_exp` 時，快照加 `-LitoDatabase p24_380lito_lito_exp`。`m4-sql-diff.ps1` 退出碼 0＝PASS、1＝FAIL、3＝沒有新收據。

**F. 結束**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-wal-check.ps1 -RunDirectory $run
powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-evidence-index.ps1 -RunDirectory $run
```

`m4-wal-check.ps1` 在伺服器 Ctrl+C 正常退出**之後**執行（伺服器還在跑時加 `-AllowLocked`）。截圖先依命名契約放進 `docs/acceptance/m4-<JAR SHA 前 8 碼>/`（`m4-prepare.ps1` 會印出目錄名），再跑 `m4-evidence-index.ps1`；它只在結果檔不存在時建立骨架，「通過／失敗／阻塞」由執行者填。最後依 #83 流程正常關閉自己啟動的 P24Lab 並釋放 lease。

## 靜態預檢的結論（2026-11-13 執行）

全文在 `docs/acceptance/m4-schema-precheck.md`。重點：

1. **`380_db.sql` 快照對新 JAR 沒有結構缺口**：程式需要的 56 張靜態表與欄位都在，包含 `spawnlist_time`、`系統_受擊無動作gfx`、`裝備_armor_魔法.id`、`etcitem.classname`。
2. **`380_lito.sql` 快照結構沒有缺口**，但 `commands` 只有 `1`／`2`／`3`／`de`／`loc` 五列（沒有 `polyme`、`move`、`item`、`spawn`），`lito_source_server` 指向正式來源庫名；前者由 seed 補、後者由 prepare 改。
3. **`character_lito` 缺 T103b 的 6 個欄位**（`380_ac.sql` 參考傾印是這樣；`p24_accounts_lito_exp` 是 #83 在 T103b 之前建的，預期相同）。不補的話伺服器會以 `character_lito … lacks ranking columns` 拒絕啟動。prepare 的 P1c 會補。
4. `p24_accounts_lito_exp`、`p24_lito_lito_exp` 沒有傾印可比，實況以 `m4-db-verify.sql` 為準。

## 資料前置（`m4-seed-exp.sql` 寫什麼、依據是什麼）

對應 `docs/acceptance/M4-RUNBOOK.md`「資料前置」的 D1–D14（T127）。路徑 `S/`＝`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。「現況」要連線才知道，看 `db-verify` 的 V6、V9、V9b。

| RUNBOOK | 需要的資料 | 判斷依據（類別：行號） | seed 怎麼處理 |
|---|---|---|---|
| D1 等級 ≥ 15 | A、B 用來對戰的角色等級 15 以上 | RUNBOOK 引 `S/TournamentRoomPolicy.java:9-18` | seed 的角色全部等級 52。#83 既有角色的等級**不改**（不 UPDATE）；V9 的 `characters_below_level_15` 不是 0 時，改用 seed 的角色 |
| D2 八角色翻頁 | 同一帳號 5–8 個 `Banned=0` 的角色。清單固定 `ORDER BY char_name LIMIT 8`，不看 `accounts.character_slot` | `S/LegacyMySqlRepository.java:969-983` | S3：帳號 A 加 7 個（連同既有 1 個共 8 個） |
| D3 GM 帳號 | `accounts.access_level` ≥ 指令門檻（200）。只看帳號，**不看** `characters.AccessLevel`；權限在登入後首次使用時快取，改完要重新登入。登入本身只讀 `login`／`password`（**明文比對**）／`banned` | `S/LegacyMySqlRepository.java:694-701、707-718`、`S/SourceAdministration.java:20-33`、`S/GmPolyMe.java:23-30`、`S/GmTravel.java:36-40`、`S/LiToGmCommands.java:26-35` | S1：新建 GM 帳號（200）與角色 `M4GM` |
| D4 GM 指令列 | 控制庫 `commands`：`polyme`／`L1PolyMe`、`loc`／`L1Loc`、`move`／`L1Move`、`item`／`L1CreateItem`、`spawn`／`L1SpawnCmd`、`de`／`L1SpawnDe`，`system=0` | `S/LegacyMySqlRepository.java:720-723`；內容同 `db/lito_commands.sql`、`db/lito_gm_polyme.sql`、`db/lito_gm_travel.sql` | S2：只補缺少的名稱。`380_lito.sql` 快照只有 `1`／`2`／`3`／`de`／`loc` |
| （A12／G21） | `.polyme` 另讀靜態庫 `polymorphs`；有該 `polyid` 的列時 `cause` 必須含 2，沒有列視為允許。快照內 `cause` 是 2／3／6／7 的列有 614 筆，例如 945（牛）、947（鹿） | `S/GmPolyMe.java:113-125` | 不需要 seed |
| D5 GM 魔杖樣板 | 靜態庫 `etcitem.classname='wand.Storm_Walk'`（快照內是 42501）；背包要有這個道具；使用魔杖本身不檢查 GM 權限，只能在戰鬥中用 | `S/GmTravel.java:16-17、59-74` | S5：放進 `M4GM` 背包（item_id 執行時從靜態庫查，不寫死） |
| D6 龍騎士／幻術師 | `characters.Type`＝5／6，分屬兩個帳號；`Class` 是外型 6658／6671（男）。結算畫面圖示只送 `Type` | `S/BattleClassGfx.java:5-6`、`S/BattleChainswordWeakness.java:17` | S3：`M4DK`（帳號 A）、`M4IL`（帳號 B） |
| D7 光箭＋近戰武器 | `character_skills` 有技能 4，且靜態庫 `skills.skill_level` 在 1..28 | `S/LegacyMySqlRepository.java:2688-2691`、`S/NativeSkillMastery.java:14-23` | S4：`M4A4`（帳號 A 的法師）與 `M4GM` 有技能 4。**近戰武器沒有放**（見下方「沒有做的項目」） |
| D8 技能 26 | 同上；`M4A4` 的 MP 300 | 同上；`S/ClientSession.java:36304-36307` | S4：`M4A4`、`M4GM` |
| D9 同血盟 | 兩個角色的 `characters.Clanname` 相同且非空。伺服器在角色進場時把原始血盟名稱匯入記憶體，`sameClan` 比的是這個 | `S/ClientSession.java:94074` → `S/GameHub.java:5172-5194`；`S/GameHub.java:5274-5279` | S3：`M4A4`（帳號 A）與 `M4B2`（帳號 B 的騎士）的 `Clanname`＝`M4CLAN`。只填名稱、`ClanID` 留 0、沒有 `clan_data` 列——**是否足夠是推測，待實機**。其他角色不入盟，避免影響一般對戰 |
| D10 負面技能 | 技能 29 緩速術，材料 40318 魔法寶石 ×1（靜態庫 `skills.itemConsumeId`） | 預檢 §5 的 `skills` 列 | S4＋S5：`M4A4` 有技能 29 與魔法寶石 ×30 |
| D11 血之渴望 | 技能 186，材料 49157 刻印的骨頭碎片 ×1＋HP 30 | 同上；`S/BattleSpellCatalog.java:218` | S4＋S5：`M4DK` 有技能 186 與碎片 ×30 |
| D12 魔法娃娃 | 背包有娃娃道具（`etcitem_doll_type.itemid`），召喚時扣 `need`／`count`：55000 要 40308 金幣 ×1000；`powers` 的編號必須在 `etcitem_doll_power_new` | `S/LegacyMySqlRepository.java:8992、9030-9033、6241-6244`、`S/ClientSession.java:19935-19976` | S5：`M4DK`、`M4IL` 各 55000 ×1＋金幣 100000 |
| D13 第 3、4 個帳號 | 兩個額外的普通帳號各一個角色 | RUNBOOK 引 `gameplay.md:56` | S1＋S3：`$vars` 填了 `@m4_c_login`／`@m4_d_login` 才建立（`M4C1` 騎士、`M4D1` 妖精）；維持佔位符就略過 |
| D14 商店藍水／變形卷軸 | 控制庫 `lito_tournament_shop` 有 40015、40088 | RUNBOOK 引 `S/TournamentShopCatalog.java:39-54` | 不寫商店表。`380_lito.sql` 快照有這兩列（預檢 §5）；實驗庫現況看 V9b。替代：S5 直接把 40015（五個角色各 10 瓶）與 40088（`M4A4` ×5）放進背包 |
| 背包列的硬性規則 | `count` 必須 1..2³¹−1、`item_id` 必須對得到樣板，否則**該角色整個背包載入失敗** | `S/LegacyMySqlRepository.java:6181-6188`、`S/LegacyInventoryWeightLoader.java:46-48` | S5 用 JOIN 樣板的方式寫入，樣板不存在就不寫 |
| 戰績列 | 不需要預先建立：`character_lito` 以 LEFT JOIN 讀取，第一場結算時由 `SettlementSql.drain` 新增 | `S/SharedDatabase.java:98`、`S/SettlementSql.java:98` | 不寫 |

**RUNBOOK C 部的追加（T129，#380；需求表在 `docs/acceptance/M4-RUNBOOK.md`「C 部資料前置」E1–E14）**

| 角色 | 追加內容 | 用在 |
|---|---|---|
| `M4A4`（A 的法師） | 技能 3、10、12、17、21、28、39、40、43、44、47、50、58、76、80；治癒藥水 40010 ×10、自我加速藥水 40013 ×5、勇敢藥水 40014 ×5；魔法寶石 40318 由 30 改成 100（40／44／47／76 都要用） | V07、V09–V16 |
| `M4GM`（GM 的法師） | 技能 3、12、21、43 | V12：當魔法相消術的目標（3、12、21 只能對自己施放，所以目標本身要是法師） |
| `M4A3`（A 的妖精） | `ElfAttr`＝8（風）；技能 148、149、152、153、167；精靈玉 40319 ×30。靜態庫裡這五個技能只有 167 的 `skills.attr` 不是 0（＝8），所以要風屬性才放得出來（`S/SourceElfAttributeGate.java:20-24`） | V15 |
| `M4A5`（A 的黑妖） | 技能 103；二級黑魔石 40321 ×30 | V15 |
| `M4DK` | 技能 194 | V15 |
| `M4D1`（可選帳號 D 的妖精，`ElfAttr`＝0） | 技能 167；精靈玉 ×5 | V28：屬性不符時客戶端會不會卡住 |

沒有放進 seed 的 C 部需求：E4 的近戰武器、E8（加速裝備或強化勇水）、E9（召喚物／寵物）、E12（月計是 UPDATE）、E13（第二個來源庫）、E14 的魔眼與幻象武器。理由寫在 RUNBOOK 的 E 表與 `docs/reports/issue-380.md`。背包多了藥水與 100 顆魔法寶石後 `M4A4`（STR 8）會不會超重，待實機。

seed 建立的角色（objid 190379001–190379012，背包列 id 190379101–190379119）：

| 帳號 | 角色 | 職業 | 用途 |
|---|---|---|---|
| GM | `M4GM` | 法師 | GM 指令、GM 魔杖（A12、A17、A18、G21、G22） |
| A | `M4DK` | 龍騎士 | G20、G25（血之渴望、魔法娃娃、藍水） |
| A | `M4A4` | 法師，血盟 `M4CLAN` | A11、G02、G23（技能 4、26、29） |
| A | `M4A2`、`M4A3`、`M4A5`、`M4A6`、`M4A7` | 騎士、妖精、黑妖、王族、女騎士 | 八角色翻頁（`M4A2` 另有技能 87） |
| B | `M4IL` | 幻術師 | G20、G25 |
| B | `M4B2` | 騎士，血盟 `M4CLAN` | G02 對友方的對象 |
| C、D（可選） | `M4C1`、`M4D1` | 騎士、妖精 | 2 對 2 |

**使用時要注意**

- **結算、死亡、斷線測試用帳號 A、B，不要用 GM 帳號**：`accounts.access_level≥200` 的帳號受到致命傷害時會回滿血而不是死亡（`S/NativePlayerVitals.java:12-30`、`S/NativePlayerCommit.java:788、808`；只追到排隊傷害這一條路徑）。
- 帳號 A 原本若已有 2 個以上角色，加 7 個後超過 8 個，依名稱排序第 9 個起不會出現在清單。#83 報告寫每個帳號一個角色；V9 會列出 `max_characters_per_account`。
- seed 不改既有帳號：帳號 A、B 已存在時密碼與權限不動；GM 帳號名稱若剛好已存在也不會被改成 200（S0、S1 的輸出會顯示）。

**做不到或沒有做的項目**

| 項目 | 原因 |
|---|---|
| 客戶端啟動腳本 `m4-start-client`（RUNBOOK P5） | 沒有做。T127 報告寫的是「若主控同意」才加；它要啟動 `LiTo.bin`、設三個環境變數、對第二視窗做記憶體內修補，本機規定不能執行也無法驗證，而且不在 T128 原任務範圍。**A03 之後需要遊戲視窗的步驟仍然是「阻塞：P5」**，請主控決定由誰做 |
| 近戰武器、防具（D7 的後半） | 沒有放。裝備要同時符合 `weapon`／`armor` 樣板、職業限制與 `is_equipped` 的載入規則，這部分沒有查完。A10 是對 B 按左鍵攻擊，空手是否算數待實機；需要武器時用 GM 帳號 `.item` 取得（只在本場記憶體） |
| 幻術師的技能 | 沒有放。G20 只需要職業圖示。妖精、黑妖的技能由 T129（#380）依 RUNBOOK C 部補上，見上方「RUNBOOK C 部的追加」 |
| 血之渴望能否由龍騎士外型施放 | seed 給了技能列與材料，但施放還要求外型支援對應動作（`BattleAnimationCatalog.supportsTimedAction`），沒有逐外型確認，待實機 |
| 同血盟（D9）只靠 `Clanname` | 見上表，推測可行、待實機。不行的話 RUNBOOK G02 的第 3、4 案照舊標阻塞 |
| 角色數值是否合理 | 等級 52、HP／MP／能力值是自訂的整數，`Exp` 留 0。登入用的查詢不檢查等級與經驗是否一致（`S/LegacyMySqlRepository.java:969-983`），進場後的行為待實機 |
| 清除 seed | 沒有提供刪除腳本（規則：不刪資料）。seed 的列有固定 objid／id 範圍，日後要清由管理者依 #83 的清理流程處理 |

## 與任務說明不同的地方

- **監聽位址**：T128 任務原文寫「只有 `127.0.0.1:42083` 與 `127.0.0.2:(42083+source_id)`」。T127 依原始碼把 CHECKLIST 02／RUNBOOK A02 改成四個（另有 `127.0.0.2:42083`，`S/Main.java:71-80`；`127.0.0.1:(42083+source_id)`，`S/SourceLogin.java:42-50`），並要求腳本照四個；本機獨立讀程式的結論相同。`m4-listen-check.ps1` 預設斷言四個；`-LegacyTwoOnly` 是任務原文的兩個。四個也還是讀程式的結論，實際以 Codex 第一次的輸出為準。
- **授權範圍**：#284 的裁定是整庫 SELECT；本目錄預設逐表（順便驗證正式部署的 GRANT 清單），保留 `@m4_grant_mode='schema'`。prepare 只加授權、不撤權，所以 #284 已授的整庫 SELECT 仍在；`db-verify` 的 V7 接受表級、schema 級或全域任一種授權，不會把它當成問題（V7c 只列 SELECT 以外的寫入權限）。
- **#284 的 `Probe.java`／`verify-run.ps1` 不沿用**（CHECKLIST 也已註明）：它們寫死正式 JAR 的 SHA、`p24_world_lito_exp` 與「收據必須 0 筆」。

## 維護

改了伺服器的 SQL 之後：

```
py -3 tools/db-grants/scan_db_access.py
py -3 tools/db-grants/gen_grants.py --update-doc docs/design/database.md
py -3 tools/acceptance/m4-schema-precheck.py --update-sql
```

三行的輸出都要一起提交。第三行在沒有 `D:\BinEngine\…` 傾印的機器上改用 `--static= --lito= --character=`（只更新 SQL 的 GENERATED 區塊與 tsv；結果檔會全部標成未比對，所以這種情況加 `--out <暫存路徑>`，不要覆蓋 `docs/acceptance/m4-schema-precheck.md`）。CI（`acceptance-tools`）會檢查 GENERATED 區塊與 `tools/db-grants` 是否同步。

`UnifiedConfiguration.KEYS` 有增減時 `m4-selftest.ps1` 的「KEYS：與 #284 的 22 鍵相同」會失敗：更新 `m4-common.ps1` 的 `$M4BaselineConfigKeys` 與 `server.properties.example`。2026-11-13 的 `src/main` 仍是同樣的 22 鍵。
