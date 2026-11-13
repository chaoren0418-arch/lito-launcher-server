# Issue #284：T014 僅重測 08（external-server-profile）

**結論：部分通過（✅ 9／❌ 0／⚠️ 1）。** 本次 2026-11-14（UTC+8）只處理 08；01–07、09–10 沿用既有通過結果，沒有重測。新登入器建置與 helper 指紋通過，但遊戲啟動流程會寫入本張明確禁止修改的原始專案與 `0-客戶端`，因此 08 尚未完成登入／大廳 PNG，**不放行 #285**。未修改產品程式，已標記 `needs:claude`。

## 已驗證

分支仍為 `task/284`，同步 main `55b58bea71330cd19c1c782ef89bac601fa84969`，建置 HEAD `0e772e1a1650230e29e6a28f79ea3f1a70216e32`；未 checkout、未建立／移除 worktree。`docs/REPORT.md` 只由 merge 帶入，與 main 一致。

以下 `E = docs/reports/issue-284/`；本次 `R = scratch/t014/run-20261113T200448Z/`。先前通過證據保留原檔；「沿用」不是本次重新通過。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 通過（沿用） | 前次 JAR／lib 建置通過；本次另完成 main Maven package、登入器 Release 建置作為 08 前置。 | 下方建置命令。 | E/retest-artifact-hashes.json；本次 E/retest08-maven-package.txt、retest08-launcher-build.txt、retest08-artifact-hashes.json |
| 02 | ✅ 通過（沿用） | 前次統一 22 鍵及 combat 設定已驗證；本次沒有建立含帳密設定。 | 既有 run-experiment.cmd -PrepareOnly。 | E/retest-config-public.properties、retest-combat.properties、retest-replay-prepare-validation.txt |
| 03 | ✅ 通過（沿用） | 前次完整冷備份已核對；本次 lease 實際持有人不同，只記錄，不啟停其 P24。 | 只讀 /api/state 的 leases；本次未備份／修改 DB。 | E/retest-p24-backup.json、retest08-lease-observation.json |
| 04 | ✅ 通過（沿用） | 前次實驗庫 17 表／731 中文欄位的值差異 0；本次未遷移。 | 沿用前次遷移及值比對 SQL。 | E/retest-equipment-migration-plan.json、retest-equipment-data-after.tsv |
| 05 | ✅ 通過（沿用） | 前次 source_id=1 路由到 p24_accounts_lito_exp；本次未連 DB，display_name 尚未查詢。 | 前次唯讀 SELECT lito_source_server；下次需查 display_name 填 profile。 | E/retest-counts-before.txt、retest-counts-after.txt |
| 06 | ✅ 通過（沿用） | 前次新 JAR 約 3 秒 ready、無 WAL replay；本次新編 JAR 未啟動。 | 前次 java -jar、120 秒內觀察 ready。 | E/retest-server-console.txt |
| 07 | ✅ 通過（沿用） | 前次兩個 loopback 各開 42083／42084；本次未開服務。 | 前次 m4-listen-check.ps1 -ProcessId 6124。 | E/retest-listen.txt |
| 08 | ⚠️ 無法完成（本次） | #404 兩支 helper SHA 正確，但啟動會寫原始專案與受保護客戶端；GUI 啟動指令另被自動核准拒絕，未產生視窗／PNG，未測登入協定。 | 新入口 --external-server-profile；按啟動前核對下列固定寫入點。本次未按／未執行 helper。 | E/retest08-write-boundary.txt、retest08-execution.json、retest08-artifact-hashes.json |
| 09 | ✅ 通過（沿用） | 前次 character_lito 2→2、lito_battle_settlement 0→0，原三庫 270 表 checksum 相同；本次未連 DB，不宣稱新的筆數。 | 前次 COUNT(*)／CHECKSUM TABLE EXTENDED。 | E/retest-counts-before.txt、retest-counts-after.txt、retest-original-checksum-before.tsv、retest-original-checksum-after.tsv |
| 10 | ✅ 通過（沿用） | 前次自有 PID 已關閉、埠釋放；本次未啟動登入器／遊戲／Java Server／P24，沒有自有實驗程序需關閉。 | 檢查本次執行紀錄；不關閉別人的程序。 | E/retest-process-cleanup.json、retest-p24-shutdown-verification.json；E/retest08-execution.json |

### 本次建置與 SHA

```powershell
mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package
dotnet build lito登入器/LiTo381-Launcher/src/LiTo381Launcher.csproj -c Release --no-restore
```

Maven `BUILD SUCCESS`；登入器 0 警告／0 錯誤。本次只建置，沒有另跑單元測試。新 JAR SHA-256：**`84BA0CDE7ABACE28456232B86922B22DD7FBFF98E8B44E8B7BEC5F995A79EDCC`**，已連同四個 lib 複製至 R，逐檔 hash 相同；R/runtime 是全新空目錄。完整清單見 [本次成品雜湊](issue-284/retest08-artifact-hashes.json)。

兩支已建置 helper 與 [#404 操作文件](../acceptance/launcher-external-server.md) 完全相符：`development_startup.py = ADFF1EDC784B6F54803A050D32E3D6D3EE06EB9242DA0AF1B4B227DCB3EAC9CD`、`external_server_startup.py = D0FE39EB6E379875F04E28FBF12B532E8F6AA2EB54347161E905DD3C86191D32`。沒有使用 `--legacy-formal-mode`。

### 08 的具體阻礙

以下是**已讀到的原始碼行為，未實際觸發寫入**，有行號及 SHA 的摘錄見 [寫入邊界證據](issue-284/retest08-write-boundary.txt)：

1. `ExternalServerStartup.RunAsync` 把 receipt 放在 `DevelopmentStartup.Root/gui-runs/external-*`；Root 固定指向原始專案 `D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app`。helper 另在同一原始 app 的 `runs/external-*` 建資料夾。僅設定 `LITO_LAUNCHER_SETTINGS_DIRECTORY` 無法搬移這兩處。
2. `external_server_startup.py:216–219` 在端點通過後直接 `shutil.copyfile(dev.DLL, dev.CLIENT / 'litoddraw.dll')`，並寫入 `lito381-compat.ini`；`dev.CLIENT` 固定是 `D:\lineage開發\0-客戶端`。此行為與本張「不改 0-客戶端」直接衝突，不屬程序疑慮，不能為了截圖執行。
3. GUI 啟動的 `exec_command` 在執行前被自動核准審查拒絕，只回 `blocked by policy`，未提供細部理由；沒有建立該次 GUI 指令中的目錄或程序。後續只做獨立的 worktree 檔案準備與唯讀檢查，不改寫程式、釘選值或換入口重試。沒有可供保存的大廳或登入器 PNG。

本次唯讀觀察 lease 實際 holder 為 `chaoren0418-arch/tw-bin-engine#1118`，與派工所述 #284 不符，原始回應見 [lease 紀錄](issue-284/retest08-lease-observation.json)。此項是額外觀測，**不是中止 08 的唯一理由**；未修改 lease、未查調度器實作。未啟動 DB，也未取得或修改 #83 私密設定。

### 設定、主控台與重跑入口

既有 [run-experiment.cmd](issue-284/run-experiment.cmd)／[run-experiment.ps1](issue-284/run-experiment.ps1) 不含帳密，保留前次實際啟動過的 JAR SHA `0F3762CE8C8E94F3B91D5404CC03B2BF815B4E329D0CBDCBC20F24F6B8686205`。它不是本次新 JAR 的啟動驗證；下次採用本次新成品時應依實際建置 SHA 更新驗收入口，不能把前次 ready 當本次結果。

前次 [公開設定](issue-284/retest-config-public.properties)、[combat 設定](issue-284/retest-combat.properties) 與[完整成功啟動輸出](issue-284/retest-server-console.txt) 保留。設定為 `127.0.0.1／127.0.0.2:42083`、JDBC `127.0.0.1:43324`、靜態庫 `p24_380db_lito_exp`、控制庫 `p24_lito_lito_exp`。本次 R 僅保留未查證 display_name 的 `external-profile.template.json`，不是已連線的正式驗收設定。

前次完整冷備份位於 `scratch/t014/run-20261113T161934Z/private-cold-backup/`（私密不提交）；實驗結構的[遷移 SQL](issue-284/retest-equipment-migration.sql)、[前後值比對](issue-284/retest-equipment-check.sql)、[還原 SQL](issue-284/retest-equipment-rollback.sql) 仍保留。本次無 DB 變更，因此沒有新增備份或還原 SQL。未碰正式 2000／3306、原 BIN、原始專案、封存、硬連結、登錄檔或服務。

## 推測

固定覆寫路徑處理後，external-server-profile 是否能登入到大廳仍未知；helper 指紋正確與建置成功不代表登入成功。不能把本次未執行歸因為伺服器協定或帳密錯誤。

## 待驗證／給下一棒的提示

由 **Claude 主控**處理 external-server-profile 與本張唯讀要求的衝突：讓驗收模式可把產物寫至 worktree，並以獲准的隔離執行資源避免覆寫 `0-客戶端`；本張不修產品程式、不繞過指紋。調度器另排有效 #284 lease。

前置完成後，Codex **仍只重測 08：唯讀查 display_name → 使用新入口 → 選服 → #83 測試帳號登入 → 大廳 PNG**；01–07、09–10 繼續沿用，只執行必要的建置、備份、資料保護與自有 PID 清理。未完成本次 Java 啟動、帳密登入、角色選擇、大廳截圖及任何戰鬥測試。
