# Issue #284：正式 0.6.71／P24 實驗啟動（第 2 次嘗試）

## 結論

**blocked／未完成，需 `needs:claude`。** 2026-11-07，本輪依最新授權補足三個實驗 schema 的 SELECT 後，以指定正式 0.6.71 JAR 執行一次啟動。前次「角色_智力成長」權限錯誤已不再出現；本次在 `NativeArmorProc.load` 因 **`Unknown column 'id' in 'field list'`** 退出（工具回傳退出碼 1），未出現 `[LiTo] ready`，42083／42084 無監聽。

已依遇錯停止規則停止，沒有改程式、欄位、複製資料或追加寫入權限。登入器、大廳登入與 PNG 均未執行；**T014 仍不是已完成前置，不可因本報告合併就放行 #285／T011**。本輪啟動的 P24Lab 已正常關閉。

## 已驗證：授權與隔離

- 工作目錄：`D:\lineage開發\3-lito開發\scratch\wt\task-284`；維持 `task/284`，未 checkout／建分支。已讀 `AGENTS.md`、`docs/REPORT.md`、TASKS 的 T014、#83 報告與既有 P24Lab 啟停腳本；未修改 `docs/REPORT.md`。
- 透過調度器 `/api/state` 核實 `p24lab` lease：持有人 `chaoren0418-arch/lito-launcher-server#284`，取得 `2026-11-07T02:33:51Z`，到期 `2026-11-07T03:18:51Z`；核對 exp 為 `1794021531.409907`。DB 使用全在有效期內，未自行修改調度器 SQLite。
- 本輪依任務附上的[最新裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6700746563)，只增加三個實驗 schema 的 SELECT。原先禁止擴權條文的本次例外限於這三項；沒有以歷史留言另行擴大寫入、補表或修欄位。
- 正式 JAR／lib 從原始專案唯讀普通複製至 `scratch/t014/run-20261107T023503Z/`，未用硬連結、未修改正式檔案。未讀正式 `config/server.properties`，未連正式 2000／3306，未啟動遊戲。
- #83 的 `isolated-test.properties` 只用於私密實驗設定交接；未印出帳密、未讀測試登入密碼。私密設定保留在 Git 忽略目錄。

### 本次實際 GRANT

MySQL `partial_revokes=0`，schema 授權中的底線有萬用字元語意，故以反斜線跳脫，精確限制三個名稱：

```sql
GRANT SELECT ON `p24\_world\_lito\_exp`.* TO 'lito83_exp'@'127.0.0.1';
GRANT SELECT ON `p24\_accounts\_lito\_exp`.* TO 'lito83_exp'@'127.0.0.1';
GRANT SELECT ON `p24\_lito\_lito\_exp`.* TO 'lito83_exp'@'127.0.0.1';
```

[完整 SHOW GRANTS 前值](issue-284/grants-before.txt) 與 [完整後值](issue-284/grants-after.txt) 的差異**僅以上三項**；81 行增加至 84 行。核對同名帳號只有 Host `127.0.0.1`。原 80 張表級 SELECT 保留；INSERT／UPDATE 仍僅 `p24_accounts_lito_exp.character_lito` 與 `p24_lito_lito_exp.lito_battle_settlement`。全域仍僅 USAGE，沒有新增 DDL、DELETE、GRANT OPTION 或管理權限，沒有給原 P24Lab schema 權限。關閉 DB 不會撤銷這三項持續授權。

## 已驗證：成品、設定與 runtime

來源：`D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server`。來源與副本逐檔 SHA-256 一致；交付前再次核對正式 JAR 雜湊未變。

| 檔案 | bytes | SHA-256 |
| --- | ---: | --- |
| `LiTo381-SharedResources-candidate.jar` | 20054975 | `E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498` |
| `lib/HikariCP-5.1.0.jar` | 161840 | `A47A6EE62379694EE52C30036F0931B72F9AEE2A801D590341ED82BD839E2134` |
| `lib/mysql-connector-j-8.0.33.jar` | 2481560 | `E2A3B2FC726A1AC64E998585DB86B30FA8BF3F706195B78BB77C5F99BF877BD9` |
| `lib/protobuf-java-3.21.9.jar` | 1671409 | `1B78B4A76A71512DEBFDFF8F8FC5AEF6BFD459F65758FECF7AFF245E6E6301E4` |
| `lib/slf4j-api-1.7.36.jar` | 41125 | `D3EF575E3E4979678DC01BF1DCCE51021493B4D11FB7F1BE8AD982877C16A1C0` |

以正式 JAR 的 `UnifiedConfiguration.KEYS`／`validate` 驗證完整 22 鍵通過。`config/experiment.properties` 設定如下，路徑相對本輪 config：

```properties
battle.ai.enabled=false
battle.natural.mp.regen.enabled=true
battle.physical.hit.enabled=true
bind=127.0.0.1
capture.directory=../captures
cpu.limit=4
database.jdbc.base.url=jdbc:mysql://127.0.0.1:43324/
database.jdbc.options=?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
database.lito.database=p24_lito_lito_exp
database.password=[REDACTED]
database.static.database=p24_world_lito_exp
database.user=[REDACTED]
game.bind=127.0.0.2
logs.directory=../logs
memory.initial=512m
memory.maximum=2048m
native.siege.roster.enforced=true
native.tournament.allowance=20000
port=42083
server.name=LiTo T014 P24 Experiment
startup.timeout.seconds=120
weight.rate=100.0
```

runtime 為全新空目錄，啟動前項目數 0；退出後只有 `battle-settlements/owner.lock`，沒有 `.pending` journal，獨占開啟 owner.lock 成功，鎖已釋放。沒有刪除舊 runtime、備份、worktree 或 junction。

## 已驗證：P24Lab 與前後資料

依 #83 既有流程，在埠無衝突、沒有既有 P24 mysqld、離線 UUID 相符後，建立冷備份 `D:\BinEngine\environments\p24\backups\issue-284-run-20261107T023503Z\db-data`，1,407 檔逐檔 SHA-256 一致；再啟動 P24Lab（本輪監督 PID 1372）。冷備份與啟動使用工具權限提升，未寫回原始專案工作目錄。

線上前後核對 port `43324`、bind `127.0.0.1`、UUID `e3443e40-d090-11f1-8040-4cedfb679789`、datadir `D:\BinEngine\environments\p24\db-data\`，以及受限帳號 `CURRENT_USER()` 均相符。

`lito_source_server` 僅一列：`source_id=1`、`character_database=p24_accounts_lito_exp`、`enabled=1`。前後相同，**未修改路由**。

| 核對 | 啟動前（02:37:23Z） | 退出後（02:38:36Z） |
| --- | ---: | ---: |
| `p24_accounts_lito_exp.character_lito` | 2 | 2 |
| `p24_lito_lito_exp.lito_battle_settlement` | 0 | 0 |

原 `p24_world`、`p24_accounts`、`p24_lito` 共 270 張表，GRANT 前與伺服器退出後的 `CHECKSUM TABLE EXTENDED` 全部一致；本輪未對原庫或 tw-bin 測試資料執行 DML／DDL。筆數一致本身不代表逐欄驗證實驗角色戰績，亦不代表結算功能通過。

02:39:27Z 以既有管理入口正常 `SHUTDOWN`（exit=0）。關閉前核對本輪 PID／啟動時間／實例身分，且無其他客戶連線；關閉後本輪程序退出，43324／42061／42083／42084 均無監聽。未強殺任何程序。

## 已驗證：啟動輸出與欄位差異

Java 17：`C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe`。工作目錄為本輪 run，02:38:25Z 開始，工具在約 4.4 秒回傳退出碼 1，未達 120 秒逾時。實際 JVM 參數：

```bat
java -Xms512m -Xmx2048m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8 "-Djava.io.tmpdir=%CD%\tmp" -jar LiTo381-SharedResources-candidate.jar config/experiment.properties
```

[完整 stdout／stderr](issue-284/server-console.txt) 包含：

```text
Exception in thread "main" java.lang.IllegalStateException: Cannot load LiTo room policy or qualified resource tables; inspect the nested database error
Caused by: java.sql.SQLSyntaxErrorException: Unknown column 'id' in 'field list'
    at local.lito381.server.NativeArmorProc.load(NativeArmorProc.java:14)
```

唯讀檢查正式 JAR 的 `NativeArmorProc` bytecode，實際 SELECT 為：

```sql
SELECT id,item_id,enchant_level,proc_group,on_attack_chance,on_hurt_chance,
       flat_damage,rand_damage,heal_hp,heal_hp_rand,heal_mp,heal_mp_rand,
       heal_mode,trigger_skill,skill_time_sec,skill_target,gfx_id,gfx_mode
FROM `裝備_armor_魔法`
ORDER BY item_id,enchant_level,proc_group,id
```

[欄位 metadata](issue-284/armor-columns.tsv) 顯示原 `p24_world` 與 `p24_world_lito_exp` 都有該表，欄名為「編號、道具編號、備註、強化值、觸發效果群組……」，沒有 `id`。這是已核實的欄位格式不相容，不是缺表或本次 SELECT 授權不足。沒有新增 alias、view、欄位或資料。

一般沙箱首次嘗試 cmd 輸出重新導向時顯示 `Access is denied`，沒有建立主控台檔、也未啟動 JVM；權限提升後才進行上述唯一一次伺服器啟動。該次臨時 cmd 的 exit-code.txt 寫入結果為 `ECHO is off.`，**不採用該檔作退出碼證據**；退出碼 1 依 exec 回傳，另記於 `logs/server-exit.json`。完整 Java 輸出保存在 server-console.log。

## 推測

P24Lab 使用的中文欄位 schema 與正式 JAR 預期的英文欄位 schema 之間，可能需要有明確語義的相容處理。但不能僅憑欄位名稱直接決定對應，也不能推論只補 `id` 就可啟動。map 12000、其他載入器、登入與結算仍可能存在後續缺口。

## 待驗證、重跑入口與給下一棒的提示

1. **Claude 接手裁定欄位相容方案**，先確認正式 JAR 上述 18 欄的資料語義，再另派經授權的實驗 schema 修復或其他方案。本輪停止於缺欄位，未繼續修補／重啟。#284 保持 blocked；#285／T011 等待真正 ready 與大廳證據。
2. 未啟動登入器／遊戲，沒有大廳 PNG、登入驗證、技能或結算驗收；不得用假圖或既有圖替代。本報告更新與授權完成不等於 T014 完成。
3. [run-experiment.cmd](issue-284/run-experiment.cmd)／[PowerShell 入口](issue-284/run-experiment.ps1) 不含帳密，每次新建 run、複製正式成品、驗證設定與 runtime、執行唯讀 Probe，再啟動 JAR。需先取得當時有效的 #284 lease，依 P24Lab 規範冷備份及啟動；入口不自動管理 DB，也不執行 GRANT。
4. 本輪修正 [verify-run.ps1](issue-284/verify-run.ps1) 在 Windows PowerShell 5.1 的 Split overload 不相容（明確指定 char[]、count、StringSplitOptions），修後實際預檢通過；Prepare／Probe 的 JVM 暫存亦固定到 run/tmp。沒有產品程式碼變更。
5. 更新後 `run-experiment.cmd -PrepareOnly` 在 DB 已關閉時通過，生成 `run-20261107T024005Z/`，22 鍵通過；沒有為驗證腳本再次啟動伺服器。重跑完整包裝尚未端到端重測；本輪實際執行過 Prepare、Probe、verify-run 與同參數的 java -jar。
6. 入口為有人值守的主控台命令：120 秒未 ready 即 Ctrl+C 正常停止，不因設定值就假設自動逾時。若 Ctrl+C 中止 PowerShell 管線，須另以 Probe 補做退出後筆數，並依既有流程正常關閉本次 P24Lab。不要接手或停止他人的程序。
7. 公開交付：本報告、完整 console、GRANT 前後、欄位 metadata、重跑 cmd／ps1、verify-run.ps1、Prepare.java、Probe.java。原始證據留在 `scratch/t014/run-20261107T023503Z/`，包括 artifact-hashes、lease、preparation、config-public、before／after、grants-applied、schema-privileges-after、original-checksum 前後、p24-backup／identity／owned-start／shutdown 與 logs。私密設定、JAR、lib、冷備份及原始資料不提交。
8. 交付檢查：10 份公開檔案以本輪實際 DB 密碼比對掃描通過；兩份私密實驗設定皆被 Git 忽略。PowerShell 語法解析及 `git diff --check` 通過；公開 console 僅將換行正規化為 LF，與原始完整輸出逐字相符。沒有產品程式碼變更，未做 Maven 建置或額外服務重啟。
