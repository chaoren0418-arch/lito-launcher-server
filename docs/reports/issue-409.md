# Issue #409：#284 實機驗收失敗項分流

**結論：** #284 第 5 輪（2026-11-14，`task/284`／`c34b83e6`）的結果是 ✅ 9、❌ 0、⚠️ 1，沒過的只有 08。08 有兩個成因：程式缺陷（開 T139）和測試環境／資料缺口（開 T140）。完成後由 T141 只重測 08，並附帶回歸 06、07、10。計畫檔在 `docs/plans/issue-409.json`。本張沒有改程式，也沒有執行程式。

## 做了什麼

1. 讀了 #284 全部留言（最新 `=== RESULT ===` 在 2026-11-14）、`task/284` 分支的 `docs/reports/issue-284.md` 和 `retest08-write-boundary.txt`，以及 #409 的主控留言（A／B 選項交使用者決定，逾時預設「不執行」）。
2. 唯讀查了原始碼與 `0-客戶端`（只算 hash、讀 ini，沒有寫入）。

## 已通過（✅）的項目：之後沿用，不重測

| 編號 | 內容 | 沿用依據 |
|---|---|---|
| 01 | JAR／lib 與登入器建置 | `issue-284.md` 第 5 輪，`retest08-maven-package.txt`、`retest08-launcher-build.txt` |
| 02 | 22 鍵實驗設定、combat 設定 | `retest-config-public.properties`、`retest-combat.properties` |
| 03 | lease、P24 身分、冷備份 | `retest-p24-backup.json` |
| 04 | 實驗 schema 遷移，值差異為 0 | `retest-equipment-data-after.tsv` |
| 05 | 角色路由 `p24_accounts_lito_exp` | `retest-counts-*.txt`（display_name 的查詢移到 T140） |
| 06 | 新 JAR 在 3 秒內 ready | `retest-server-console.txt` |
| 07 | loopback 端點監聽 | `retest-listen.txt` |
| 09 | 戰績 2→2、收據 0→0，原 270 表 checksum 不變 | `retest-original-checksum-*.tsv` |
| 10 | 自有程序已關、埠已釋放 | `retest-process-cleanup.json` |

**需要附帶回歸的 ✅ 項目，以及原因：**
- **06、07**：08 的重測一定要啟動實驗伺服器。第 5 輪新建的 JAR（`84BA0CDE…EDCC`）從沒啟動過，第 4 輪通過的是另一個 SHA。T141 會用最新 main 的產物重新開到 ready 並檢查監聽。
- **10**：重測會啟動自有的 Java、P24、登入器和遊戲程序，結束後必須重新確認都已清理。
- 09 不重新判定。登入可能寫入 `character_lito`，所以 T141 只把筆數和 checksum 再比一次，當作資料保護檢查。

## 失敗項目對應

| 項目 | 現象 | 分類 | 對應 |
|---|---|---|---|
| 08-a | `external_server_startup.py:216–219` 無條件把 `litoddraw.dll` 和 `lito381-compat.ini` 寫進 `D:\lineage開發\0-客戶端`，違反 `launcher-external-server.md:27` | (a) 程式缺陷 | **T139** |
| 08-b | `ExternalServerStartup.cs:112` 的收據路徑和 helper 的 `runs/` 都寫死在原始專案 `app\`，無法改到 worktree | (a) 程式缺陷，與 08-a 同一個根因：外部入口沒有寫入邊界 | **T139**（合成一張） |
| 08-c | codex-local 自動核准拒絕 GUI 啟動，只回 `blocked by policy` | (b) 測試環境 | **T140** 第 2 點（還是不放行就標 `needs:orchestrator`） |
| 08-d | profile 的 `display_name` 還沒依實驗庫填 | (b) 測試資料 | **T140** 第 1 點 |
| 08-e | 遊戲本身（LiTo.bin／litoddraw.dll）執行時會在客戶端目錄寫 `lito381-compat.log`（`ddraw_proxy.cpp:180`）和 `LiToError.txt` | (c) 已知限制：只要執行客戶端就一定會發生，登入器改不掉 | 不開任務。T140 先做基準快照，T141 前後比對並如實記錄 |
| 08-f | lease holder 是 tw-bin-engine#1118，與派工描述不符 | 調度器的觀測項，不是 08 的失敗原因 | 不開任務（調度器已在 #284 修過防餓死規則） |

## 已驗證

- `0-客戶端\litoddraw.dll` 的 SHA-256 是 `E47BD6C6EC9979C8747A523E4457133802D19F673C8DC1E67707ABFA96216A8A`，和 `app\compat\litoddraw.dll`、`development_startup.py:36` 的釘選完全相同。
- `0-客戶端\lito381-compat.ini` 目前的內容（`ScalePercent=200`、`Logging=1`、`Schema=4`）就是 helper 在縮放 200 時要寫的內容。
- 所以只要選相同縮放，T139 把「覆寫」改成「核對一致就不寫」不會影響功能。
- 目前 main 的 `ExternalServerStartup.cs` 和 `external_server_startup.py` 的 SHA 和 #284 證據相同（`03CA58FB…`、`D0FE39EB…`），問題仍然存在。
- 計畫 JSON 用 python `json.load` 解析過：3 項，依賴關係是 T141→T139、T140。

## 推測

- 方案 B（使用隔離的客戶端副本）會和 CLAUDE.md 的「不複製 pak/map/sprite 進本專案」以及「只寫本專案」衝突，所以 T139 改走「登入器零寫入」，在 A、B 之外不必等使用者決定。遊戲本身寫的執行期日誌（08-e）仍是使用者規定的灰色地帶。T141 把這兩個日誌設成「允許變動但必須記錄」；使用者如果不接受，要在 #409 或 T141 留言改判。

## 待驗證

- T139 改完後，外部入口是否真的能登入到大廳，要等 T141 實機確認。
- codex-local 的 GUI 放行要由 T140 實測。

## 產出路徑

- `docs/plans/issue-409.json`
- `docs/reports/issue-409.md`

## 給下一棒的提示

- **T139（claude-local）**：改了 helper 之後要重算 C# 裡的 helper SHA 釘選。0.6.71 正式流程（`development_startup.py`）不要動。
- **T140（codex-local）**：先處理 GUI 放行；不放行就直接標 `needs:orchestrator`，不必做後面的步驟。
- **T141**：縮放一定要選 200（和現有 ini 相同），否則 T139 的核對會拒絕啟動。這是設計上的行為，不是 bug。
- #409 的主控留言把 A／B 交給使用者決定。本分流選的 T139 方向不需要寫入 `0-客戶端`，如果主控或使用者另有決定，請直接改 T139 的內文。
