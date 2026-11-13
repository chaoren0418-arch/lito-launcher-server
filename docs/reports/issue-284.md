# Issue #284：T014 新 JAR 實驗重測 06–08

**結論：部分通過（✅ 9／❌ 1／⚠️ 0）。** 2026-11-14 00:15–00:28（UTC+8）：06 啟動與 07 四端點通過；08 被登入器設定覆寫及資源預檢阻擋，未登入、未到大廳，**不放行 #285**。未修改產品程式。01–05、09–10 沿用前次結果，僅做本次啟動必要的建置、備份、資料保護及程序清理。

## 已驗證

全程使用既有 `task/284`，同步 main `c258cbb4148a4ccc773b4d0b73b6196e3050d4e2`，建置 HEAD `6bb426b932b7de90aacaf0ffb883b7ba5c091e94`。`docs/REPORT.md` 僅由 merge 帶入，與 main 一致。

以下 `E = docs/reports/issue-284/`；`R = scratch/t014/run-20261113T161934Z/` 保存本次私密設定與冷備份；成功啟動目錄 `S = scratch/t014/run-20261113T162233Z/`。兩次伺服器嘗試各用全新空 runtime，未刪除舊資料。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 通過（沿用） | 前次 JAR／lib、登入器建置通過；本次因同步 main 重新 package 成功，登入器沿用前次本 worktree 成品。 | 執行下方 Maven 命令。 | E/retest-maven-package.txt、retest-artifact-hashes.json；前次 launcher-build.log |
| 02 | ✅ 通過（沿用） | 統一 22 鍵驗證通過；本次補六鍵 combat 入口，引用來源檔唯讀，前後雜湊一致。 | run-experiment.cmd -PrepareOnly。 | E/retest-config-public.properties、retest-combat.properties、retest-combat-source-hashes.json、retest-replay-prepare-validation.txt |
| 03 | ✅ 通過（沿用） | 啟動前 lease holder 與派工相同、到期 17:00:37Z；完整冷備份 1,722 檔／278,882,680 bytes，逐檔 SHA 相同。 | 停機備份後核對 UUID、datadir 與 43324。 | E/retest-lease-observation.json、retest-p24-backup.json、retest-before.txt |
| 04 | ✅ 通過（沿用） | 保留前次遷移；本次補 17 張實驗裝備表的 731 個中文欄位，均由原欄位複製，各表值差異 0。 | 備份後執行 retest-equipment-migration.sql，再執行 retest-equipment-check.sql。 | E/retest-equipment-migration-plan.json、retest-equipment-columns-before.txt、retest-equipment-data-after.tsv |
| 05 | ✅ 通過（沿用） | 路由仍為 source_id=1、p24_accounts_lito_exp、enabled=1，未改該列。 | 唯讀 SELECT lito_source_server。 | E/retest-counts-before.txt、retest-counts-after.txt |
| 06 | ✅ 通過（本次） | 補 combat 與裝備欄位後，Java PID 6124 約 3 秒內 ready，無 WAL replay。 | 在 S 執行 java -jar，觀察 120 秒內 ready。 | E/retest-server-console.txt；修正前 E/retest-server-before-migration.txt |
| 07 | ✅ 通過（本次） | 127.0.0.1、127.0.0.2 各開 42083、42084，沒有多餘監聽。 | m4-listen-check.ps1 -ProcessId 6124；不傳 RunDirectory，另存 stdout。 | E/retest-listen.txt |
| 08 | ❌ 失敗（本次） | GUI 覆寫實驗 profile 為 32000、強制 StartLocalServer=true；相容資源預檢失敗，啟動按鈕停用，沒有遊戲／大廳；視窗擷取兩次逾時，無 PNG。 | 本次登入器 --legacy-formal-mode，設定檔指向 42083／StartLocalServer=false，觀察 GUI 預檢。 | E/launcher-validation-retest.txt、retest-launcher-profile.json、capture-errors.txt |
| 09 | ✅ 通過（沿用） | 本次 character_lito 2→2、lito_battle_settlement 0→0；原三庫 270 表 checksum 前後完全一致。 | 啟動前與關閉 Java 後 COUNT(*)、CHECKSUM TABLE EXTENDED。 | E/retest-counts-before.txt、retest-counts-after.txt、retest-original-checksum-before.tsv、retest-original-checksum-after.tsv |
| 10 | ✅ 通過（沿用） | GUI 正常關閉；Java 以 Ctrl+C 結束；P24 正常 SHUTDOWN，四個自有 PID 均消失，實驗埠無監聽。 | 核對 PID／建立時間／實例身分，無其他 DB 客戶端後 SHUTDOWN。 | E/retest-process-cleanup.json、retest-p24-shutdown-verification.json |

### JAR、設定與重跑

```powershell
mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package
```

JAR SHA-256：**`0F3762CE8C8E94F3B91D5404CC03B2BF815B4E329D0CBDCBC20F24F6B8686205`**（15,092,477 bytes）。Main-Class：`local.lito381.server.SharedResourceMain`；四個執行 lib 及 SHA 見 [成品清單](issue-284/retest-artifact-hashes.json)。依指定命令未跑單元測試。

[公開設定](issue-284/retest-config-public.properties) 已遮蔽帳密：bind `127.0.0.1`、game.bind `127.0.0.2`、port `42083`、JDBC `127.0.0.1:43324`、靜態庫 `p24_380db_lito_exp`（沿用快照裁定）、控制庫 `p24_lito_lito_exp`。capture／logs 指向各次新 run；憑證只讀 #83 私密交接檔，未讀寫正式 `config/server.properties`。

依[combat 裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6857803556)補 [combat.properties](issue-284/retest-combat.properties)，四個 source 路徑指向 `D:/lineage開發/1-伺服器/`，保留 arena.bot.con／wis=1；額外相依 `config/other.properties` 亦存在且唯讀。主控台實際載入 69 控制值、807 技能、364 武器、1,422 防具。

無帳密入口已更新本次 SHA、combat 範本複製與來源雜湊核對，`-PrepareOnly` 實際通過；`verify-run.ps1` 語法解析通過。先取得有效 lease、完成備份、啟動並核對 P24，再執行：

```bat
docs\reports\issue-284\run-experiment.cmd -CandidateDirectory "D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch\t014\run-20261113T161934Z"
```

入口每次建立新 run，不啟停 MySQL、不自動變更 DB。120 秒啟動時限須由操作者觀察；成功後結束以 Ctrl+C。實測成功主控台以 cmd 原生 stdout/stderr 合併存檔，未再經 PowerShell 的錯誤格式化。Java 關閉時最後留下不完整的 `Exception in thread "main"` 字樣，無堆疊；PID 與埠均已釋放，**未據此宣稱應用層所有 shutdown hook 已驗證**。

T128 的 `-RunDirectory` 只接受 `scratch/m4/run-*`，會拒絕本任務指定的 `scratch/t014/run-*`；本次省略該參數，正常完成相同的四端點驗收並另存輸出，未修改 T128。

### 實驗資料庫變更與還原

完整冷備份與逐檔 manifest 在 `R/private-cold-backup/`、`R/private-backup-manifest.json`（私密，不提交）。未改權限、原 P24 三庫或 tw-bin 測試資料；沒有 seed 帳號或假資料。

依[實驗結構裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6855340338)，只在 `p24_380db_lito_exp` 依 `NativeSourceEquipmentColumns.QUERIES` 的精確 `中文 AS 英文` 對應補欄位，型別取既有英文欄位；保留原欄位並複製真實值。17 表／731 欄及每表原筆數見 [變更清單](issue-284/retest-equipment-migration-plan.json)；[原結構](issue-284/retest-equipment-columns-before.txt)、[遷移 SQL](issue-284/retest-equipment-migration.sql)、[前後值比對 SQL](issue-284/retest-equipment-check.sql)、[實際比對結果](issue-284/retest-equipment-data-after.tsv)互相對應。兩張原本 0 筆的表仍為 0，未填資料。

本次[還原 SQL](issue-284/retest-equipment-rollback.sql)只移除新增的 731 欄，**未執行**；只適用尚未在新欄位產生獨有資料的狀態，還原前另備份現況。前次遷移、備份位置與 [rollback.sql](issue-284/rollback.sql) 保留，由 git `7cf833da` 的報告追溯，不重做。

### 08 的具體失敗邊界

本次自建登入器 PID 9676 以獨立 `LITO_LAUNCHER_SETTINGS_DIRECTORY=R/launcher-settings` 啟動；設定要求 `127.0.0.1:42083`、`StartLocalServer=false`。`MainForm.cs:289–292` 與 `364–367` 卻強制改成隨附 profile／自行啟動本機服；GUI 文字實際顯示 `endpoint=127.0.0.1:32000`。未按停用的啟動按鈕，**沒有連上 32000**。

客戶端僅以 `0-客戶端` 做唯讀預檢；未建立可執行的隔離相容資源集。GUI 顯示 LTtext 指紋不符、缺 litolauncher-c.pak、Text／Tile／Sprite 世代不合、缺隔離 ddraw；原 `client-isolated` 位置也不存在。這些是本次部署的未備妥項目，不能當作遊戲執行失敗或伺服器登入協定失敗。未改 shared BIN、TW／LHZ、封存、硬連結或 worktree 結構。

## 推測

伺服器 ready 與四端點已成立；08 的直接阻礙在登入器實驗部署／連線入口。修正後能否通過帳密驗證、角色選擇與大廳，仍未知。

## 待驗證／給下一棒的提示

交 **Claude 主控（needs:claude）**處理只連既有實驗服 `42083`、不強制啟動其他服的登入器入口與合法唯讀資源接線；不得為通過舊驗證而改 shared 資源或繞過檢查。圖像工具兩次擷取逾時另記 [capture-errors.txt](issue-284/capture-errors.txt)。

修正後由 **Codex 只重測 08**（測試帳號登入→大廳 PNG）；沿用 01–07、09–10，仍執行必要的備份、有效 lease 與程序清理。未完成大廳截圖、登入驗證或任何戰鬥測試。未動正式 2000／3306、原始專案檔案或系統設定。
