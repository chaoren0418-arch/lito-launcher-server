# Issue #284：T014 新 JAR 實機實驗

**結論：部分通過（✅ 7／❌ 1／⚠️ 2）。** 2026-11-13 23:31–23:39（UTC+8）完成建置、備份、實驗 schema 遷移及兩次啟動；最後因缺少 `config/combat.properties` 退出，未到大廳，**不得放行 #285**。依派工規則停止非 DB 錯誤重試，交 `needs:claude`；未改產品程式。

## 已驗證

沿用 `task/284`，同步 main `1cc1f7d3549ac3271811f1e92ac24846775baf46` 後建置 HEAD 為 `fcd2bbe74e856a8414d71dd33241954dca8cabdf`。`docs/REPORT.md` 僅由 merge 帶入，與 main 一致。

以下 `E = docs/reports/issue-284/`；本機原始／私密證據在 `R = scratch/t014/run-20261113T153406Z/`，第二次啟動在 `R2 = scratch/t014/run-20261113T153818Z/`。兩次各用全新空 runtime，沒有清空或刪除舊目錄。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 通過 | Maven package 成功，JAR／四個 lib 複製雜湊一致；登入器亦從本工作樹建置成功。 | 執行下方建置命令並比對 SHA。 | E/maven-package.log、artifact-sha256.json、launcher-build.log |
| 02 | ✅ 通過 | 22 鍵經 JAR 的 KEYS／validate 核對；憑證只讀 #83 私密檔，兩次啟動前 runtime 均為 0 項。 | Prepare.java 產生設定，靜態庫依裁定改為快照庫。 | E/config-public.properties、preparation-current.json |
| 03 | ✅ 通過 | lease holder／到期符合派工；P24 全 datadir 冷備份 1,722 檔、278,877,657 bytes，逐檔 SHA 相同。 | 停機時備份至 R/private-cold-backup，再啟動核對實例身分。 | E/lease-before-start.json、p24-backup.json |
| 04 | ✅ 通過 | 預檢 8 個阻塞補齊後為 PASS／0；另補防具表中文欄位，54 筆原值對照 0 差異，第二次確實載入 54 筆。 | 備份後跑 m4-db-prepare.sql，再跑 E/armor-migration.sql。 | E/db-verify-summary.tsv、columns-before.tsv、columns-after.tsv、armor-data-after.tsv、server-console.txt |
| 05 | ✅ 通過 | 路由前後都是 source_id=1、p24_accounts_lito_exp、enabled=1，無資料列變更。 | SELECT lito_source_server。 | E/before.txt、after.txt |
| 06 | ❌ 失敗 | 第二次 java -jar 約 4 秒內 exit 1：缺 config/combat.properties，無 [LiTo] ready。 | 用下方 run-experiment.cmd 重跑。 | E/server-console-first.txt、server-console.txt、server-exit.json |
| 07 | ⚠️ 無法判定 | ready 前退出，無存活 Java PID 可做 T128 四端點驗收。 | 修正 06 後對存活 PID 跑 m4-listen-check.ps1。 | E/server-exit.json、p24-shutdown-verification.json |
| 08 | ⚠️ 跳過 | 伺服器未就緒，未開 GUI／遊戲、未登入測試帳號，沒有大廳 PNG。 | 06、07 通過後，登入器連 127.0.0.1:42083 到大廳截圖。 | E/server-console.txt |
| 09 | ✅ 通過 | character_lito 2→2、lito_battle_settlement 0→0；原三庫 270 表 checksum 全部相同。 | 前後 COUNT(*) 與原庫 CHECKSUM TABLE EXTENDED。 | E/before.txt、after.txt、original-checksum-before.tsv、original-checksum-after.tsv |
| 10 | ✅ 通過 | 兩次 Java 自行退出；P24 正常 SHUTDOWN，PID 9520／19532 結束，實驗埠無監聽。 | 核對自有 PID／建立時間／實例身分，確認無其他連線後 SHUTDOWN。 | E/p24-shutdown-verification.json、process-cleanup.json |

### 成品與設定

```powershell
mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package
```

JAR SHA-256：**`FF4EAD8B222E7682A09C1C55979753A943B10402320E36C3F27B8B0E383F7064`**（15,086,958 bytes）；Main-Class 為 `local.lito381.server.SharedResourceMain`。lib：HikariCP 5.1.0、mysql-connector-j 8.0.33、protobuf-java 3.21.9、slf4j-api 1.7.36。依指定參數未跑單元測試。

完整遮蔽設定見 [config-public.properties](issue-284/config-public.properties)：`bind=127.0.0.1`、`game.bind=127.0.0.2`、`port=42083`、JDBC `127.0.0.1:43324`、靜態庫 `p24_380db_lito_exp`、控制庫 `p24_lito_lito_exp`。capture／logs 相對設定檔指向本次 run。沒有讀寫正式 `config/server.properties`。

### 資料庫變更與還原

依[快照裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6701189885)及[補結構裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6855340338)執行，未重匯快照、未 seed 帳號或改原庫：

- `p24_accounts_lito_exp.character_lito` 新增六個排名／月計欄位，定義與預設值見 columns 前後檔。
- `m4-db-prepare.sql` 新增 58 個表級 SELECT 授權目標：56 個靜態表（原已有該庫整庫 SELECT）及 `clan_emblem`、`server_盟屋_紀錄`；其他既有權限保留，完整 grants 前後存證。
- `p24_380db_lito_exp.裝備_armor_魔法` 按 `NativeArmorProc.java:40` 的 SQL 別名，新增 18 個同型別中文欄位，由英文欄位複製真實值；原欄位保留，54 筆零差異，未填假資料。
- 路由、兩張結算相關表筆數、原三庫及 tw-bin 資料均未變。

[rollback.sql](issue-284/rollback.sql) 提供本輪欄位與增量授權還原，**未執行**；只適用尚未產生新排名資料的狀態，還原前先備份現況。完整冷備份及逐檔 manifest 留在 R 私密目錄，不提交。後續繼承本輪已完成的實驗遷移。

### 失敗與重跑入口

[完整主控台](issue-284/server-console.txt) 最後為 `NoSuchFileException: ...\config\combat.properties`，呼叫鏈 `CombatRules.read:180 → CombatRules.load:32 → CombatDatabase.load:53`。第一次錯誤是防具欄位 `編號` 不存在，見 [第一次主控台](issue-284/server-console-first.txt)，已解決。PowerShell 原始輸出部分中文路徑編碼失真，公開副本僅轉 UTF-8。

無帳密重跑入口（需當次有效 lease、已備份且正常啟動的 P24Lab，以及本機 #83 私密檔）：

```bat
docs\reports\issue-284\run-experiment.cmd -CandidateDirectory "D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch\t014\run-20261113T153818Z"
```

入口另建 run、複製 JAR／lib、讀 #83 憑證、檢查空 runtime／實驗身分，不啟停 MySQL、不改 DB。目前會重現缺 combat 設定，並非可到大廳的交付。120 秒是操作時限，既有入口沒有自動終止；就緒後須正常關閉本次程序。

## 推測

T128 預檢漏列防具中文欄位及 combat 來源鏈，因此 SQL 預檢 PASS 不代表 ready；補入口後是否還有資源缺口尚未知。

## 待驗證／給下一棒的提示

由 **Claude 主控**裁定並交 resource 任務補實驗 combat 配置。`CombatRules.java:31–48` 要求 `source.skill.file`、`source.damage.file`、`source.character.file`、`source.map.directory`、`arena.bot.con`、`arena.bot.wis` 六鍵；須核對真實來源及實驗邊界，不得用空設定假通過或引用正式連線。驗收工具維護者另補 T128 漏檢。

後續 **Codex 僅重測 06–08**；01–05、09–10 結果沿用，新啟動程序仍須做必要清理及資料保護核對。未做戰鬥、沒有大廳截圖；未動正式 2000／3306、原 BIN、封存、硬連結或 worktree 結構。
