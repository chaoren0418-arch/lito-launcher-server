# Issue #378 — T127：M4 驗收準備（CHECKLIST 改新 JAR 版＋M4-RUNBOOK A、B 部）

分支 `task/378`。執行者 `claude-local`。只寫文件；沒有啟動任何程式、沒有連任何 DB、沒有執行 `mvn`。

## 結論

- `docs/acceptance/CHECKLIST.md` 已改成「最新 main 的 Maven JAR」版；21 步編號不變，截圖檔名補成固定名稱。
- 新增 `docs/acceptance/M4-RUNBOOK.md`：A 部 01–21、B 部 G19–G27 與 G02，每步六欄；另有前置 P1–P7、資料前置 D1–D14、命名契約。
- `customer-gaps.md` 的 G19–G27（與 G02）「建議」欄已加 RUNBOOK 連結。
- **寫手冊時查到一個會卡住整個 M4 的問題：正式 0.6.71 登入器不能連外部啟動的伺服器**（詳見下方「需要主控處理」第 1 點）。#284、#285、T137、T138 都會碰到。

## 做了什麼

### 1. CHECKLIST
- 交付基準改為最新 `main` 的 `mvn package` 產物；前置加「00 建 JAR 並記 SHA-256」（命令、產物路徑依 `pom.xml:41,166-168,176-190`）。
- 執行目錄改為命名契約的 `scratch/m4/run-<UTC>/`；啟動命令沿用 `issue-284/run-experiment.ps1:39` 的 JVM 參數，JAR 檔名改成 `LiTo381-Server.jar`。
- `database.static.database` 改 `p24_380db_lito_exp`（#284 的 02:43 裁定）。
- 戰績 SELECT 加上 `ranking_points`、`disconnects`、`monthly_period`、`monthly_ranking_points`、`monthly_wins`、`monthly_losses`；新增「戰績預期值」表：勝 +250（`gameplay.md:293`）、一般敗 +50（`:294`）、斷線敗 −50 且最低 0（`:295`）、月計與跨月（`:297,302`）。
- 新增前置列：`character_lito` 新欄位檢查（缺欄位新 JAR 拒絕啟動）、客戶端啟動方式。
- 步驟 02 的監聽預期從兩個改成四個（見「與既有文件不一致的地方」）。
- 註明 `docs/reports/issue-284/` 的四支腳本寫死正式 JAR 的 SHA、檔名與舊 schema，M4 不能直接用。

### 2. M4-RUNBOOK
- 共用資訊：啟動 console 各行、`frames.jsonl` 的路徑規則與格式、object id 的對照方式。
- A 部：01–21 每步六欄，封包欄寫 opcode 名稱、數值、方向與處理方法的行號。
- B 部：G19（＝A 部）、G20–G26 各有步驟；G27 留 T130 回填欄位並寫好兩種結論各自的做法；G24 的實體拔線寫成「阻塞條件＋誰能解除」。
- G02：四案（自施／對友方 × 按不按 Ctrl）、capture 檔名規則、目標 object id 的位置（`plain[3..6]`，little-endian）、Ctrl 尾碼，以及依 capture 與 console 判斷原因的判讀表。

### 3. 其他
- `customer-gaps.md`：G02、G19–G27 共 10 列加連結（RUNBOOK 用 `<a id>` 錨點 `#g19`…`#g27`、`#g02`）。
- `TASKS.md`：T127 填上 #378、狀態 review。

## 怎麼驗證

### 已驗證
- **讀過的文件**：CHECKLIST 舊版、`issue-83.md`、`issue-284.md` 與 #284 的全部裁定留言（用 `gh api` 取回，含 2026-11-07 02:43 與 2026-11-11 14:08）、`customer-gaps.md`、`gameplay.md`、`issue-335.md`、`issue-357.md`、`issue-376.md`、`docs/plans/issue-376.json` 的 T128–T130、T137、T138 內文、`db/lito_*.sql`。
- **自己讀程式確認的行號**：`pom.xml`、`UnifiedConfiguration.java` 全檔、`SettlementSql.java` 全檔、`CaptureLog.java` 全檔、`Main.java:30-219`、`SourceLogin.java:28-89`、`DurableBattleSettlement.java:1-60`、`ClientSession.java:36256-36325`（技能 26 的准入）、`ServerConfig.java:254-259,578-579,1121-1126`、`development_startup.py:8-17,50-139,210-259`、`verify_pair.py` 全檔、`forced_combat.h`／`.inc`、`named_ddraw.py:14-22`。
- **由搜尋代理查出、我再抽查的行號**：四個 Explore 代理各回報一批「事實＋檔案行號」；我用 PowerShell 逐行印出其中約 150 個行號比對（`Protocol.java` 的全部 `C_LITO_*`／`S_LITO_*` 常數、RUNBOOK 引用的 `ClientSession`、`GameHub`、`GmPolyMe`、`GmTravel`、`LegacyMySqlRepository`、`NativeRoomRules`、`SettlementJournal` 等），抽查到的都相符。
- `gameplay.md` 的引用行（`:47-61`、`:165-166`、`:227`、`:238`、`:244`、`:266-271`、`:281`、`:293-302`、`:326`、`:330`）對過本次讀到的內容。
- `customer-gaps.md` 的改動是 10 列各改一行（`git diff --stat`：10 增 10 減）。
- `S/` 內搜尋 `12000`：4 處，都不是地圖編號。

### 推測
- RUNBOOK §P5 的「直接跑 probe 連實驗端點」做法：依 `verify_pair.py` 的先例與 `development_startup.py` 的參數推出，沒有執行過。
- G23 替代版與 G02 對友方用 Time Attack 讓兩個帳號互為友方：依「非 PvP 模式不能打玩家」推論；自建房強制「雙方人數一致」會不會擋住 Time Attack 開戰沒有查。
- `target/lib/` 的內容與 #284 表列的 4 個 JAR 相同（HikariCP、mysql-connector-j、protobuf-java、slf4j-api）：後兩個是遞移依賴，沒有實際建置確認。
- Windows 本機建出的 JAR 與 CI（Linux）同 SHA：#357 也列為未驗證；CHECKLIST 寫成「不一致照實記錄，不阻塞」。
- 代理回報、我沒有逐一抽查的行號（例如 `ClientSession.java:13187-13226`、`:57587-57593`、`LiTo381Launcher` 的 `FormalStandardSources.cs`／`MainForm.cs` 各行）可能有一兩行偏移；RUNBOOK 開頭已註明以方法名與字串為準。
- 客戶端畫面上的按鈕名稱、位置：沒有開遊戲，RUNBOOK 一律用功能描述。

### 待驗證（全部要實機，交 Codex）
- RUNBOOK 的每一步。
- 關視窗時客戶端送的是 `C_QUIT` 還是直接斷線（A16 兩種 log 都寫了）。
- G23 替代版第 4 列：非 PvP 模式按 Ctrl 能不能打到玩家（程式只排除寵物競賽，文件沒寫）。
- G25 一般 BUFF 的到期變色要等 1200 秒；RUNBOOK 允許填「到期未觀察」。

## 需要主控處理

1. **正式登入器不能接外部伺服器（M4 的阻塞點）**。0.6.71 登入器的預設模式一定自己啟動 `app/server/LiTo381-SharedResources-candidate.jar`（路徑寫死、比對雜湊，`FormalStandardSources.cs:12`），設定固定讀正式 `config/server.properties`（`SourceServerChoice.cs:9`），埠固定 32000。沒有「只開客戶端」的參數；複製一份登入器資料夾也沒用（helper 用絕對路徑）。所以 #284「用新 JAR 啟動到大廳」、#285、T137、T138 都需要另一種開客戶端的方法。RUNBOOK §P5 寫了建議做法（直接跑 `app/lito_runtime_probe_named.py`，`verify_pair.py` 有連非預設埠的先例），但需要一支腳本。請決定：
   - (a) 併入 T128（#379）：在 `tools/acceptance/` 加 `m4-start-client`；或
   - (b) 另開一張任務；或
   - (c) 改登入器加「外部伺服器」模式（要重建登入器，交付物就不是正式 0.6.71 EXE）。
   建議 (a)。在這之前 A03 起需要遊戲視窗的步驟都會是「阻塞：P5」。
2. **G24 實體拔線做不到**：伺服器只接受 loopback（`ServerConfig.java:256-257`），而且讀取沒有逾時、沒有 keepalive（`ClientSession.java:1278`）。RUNBOOK 用「強制結束客戶端程序」替代。請決定是否接受替代，或另開任務評估加閒置逾時（真實環境玩家拔線時，伺服器要等 OS 的 TCP 重傳放棄才會判斷線）。
3. **G25 魔法娃娃圖示**：`src/main` 刻意不送圖示（`ClientSession.java:20298-20313`，理由是客戶端沒有接收器）；登入器文件 `world-resource-compat-current.zh-TW.md:49` 寫的 `FA→7B` 是未發布的隔離候選。RUNBOOK 的預期寫成「沒有圖示」。要不要圖示需要決定。
4. #285 的內文仍寫「正式 0.6.71」與 runner `dot`（#376 報告已提過）；本張合併後請改成「依新版 CHECKLIST 與 M4-RUNBOOK A 部」。

## 與既有文件不一致的地方（本張沒有改那些文件）

| 位置 | 文件寫的 | 程式實際 | 處理 |
|---|---|---|---|
| 舊 CHECKLIST 步驟 02、T128 內文的 `m4-listen-check.ps1` | 監聽只有 `127.0.0.1:42083` 與 `127.0.0.2:(42083+source_id)` | 四個：`127.0.0.1:42083`、`127.0.0.2:42083`（`Main.java:71-80`），每個來源在兩個位址各開 `42083+source_id`（`SourceLogin.java:42-50`） | CHECKLIST、RUNBOOK 已寫四個；**T128 的腳本請照四個** |
| `gameplay.md:304` | 在 `config/server.properties` 加 `battle.reward.win`／`.loss` 可改 | 統一設定只接受 22 個鍵，多的鍵會拒絕啟動（`UnifiedConfiguration.java:7,24`）；這兩個鍵不在裡面 | CHECKLIST 註明實驗設定不能寫這兩個鍵、積分固定 250／50。`gameplay.md` 建議另外修正 |
| `gameplay.md` 多處 `S/ClientSession.java:877xx`、`:20453` 等 | — | 行號已位移（例：`recordBattleResult` 現在在 `:56886`） | RUNBOOK 用本次查到的行號 |
| 舊 CHECKLIST 步驟 17「變身 7200」 | 像是外型代號 | 7200 是持續秒數（`GmPolyMe.java:8`）；文件範例代號是 6080 | 已改寫 |
| 舊 CHECKLIST「正式登入器實驗副本」 | 由 dot 準備副本 | 複製登入器無法改變它連的伺服器 | 改成 §P5 |

## 資料前置缺口清單（交 T128 的 `m4-seed-exp.sql`；完整表在 RUNBOOK「資料前置」）

| # | 缺什麼 | 影響的步驟 |
|---|---|---|
| D1 | A、B 角色等級 ≥ 15（#83 沒寫等級） | A04 起全部 |
| D2 | 一個帳號有 5～8 個角色 | A05 |
| D3 | GM 帳號（`accounts.access_level` ≥ 200） | A12、A17、A18、G21、G22 |
| D4 | `p24_lito_lito_exp.commands` 有 `polyme`／`loc`／`move`／`item`／`de` | 同上 |
| D5 | 靜態庫 `etcitem` 有 `classname='wand.Storm_Walk'`（42501） | A18、G22 |
| D6 | 龍騎士（`Type=5`）、幻術師（`Type=6`）各一，分屬兩個帳號 | G20、G25-4 |
| D7 | A 已學技能 4、有近戰武器 | A10、A11、G23 |
| D8 | A 已學技能 26、MP ≥ 50 | G02、G25-1 |
| D9 | A、B 同一個非空血盟 | G02 對友方 |
| D10 | 一個角色已學負面技能（建議 29） | G23 |
| D11 | 龍騎士已學技能 186 | G25-4 |
| D12 | 背包有魔法娃娃道具 | G25-2 |
| D13 | 第 3、4 個帳號（2 對 2） | G23 完整版、G02 對友方的 PvP 版 |
| D14 | `lito_tournament_shop` 有 40015、40088 | G25-3、A12 替代 |

另有兩項不是資料列、但同樣要在開機前備妥：`p24_accounts_lito_exp.character_lito` 的 6 個積分欄位（缺的話新 JAR 拒絕啟動）、`p24_380db_lito_exp` 的匯入與授權（02:43 裁定）。兩項都已列在 T128 內文。

## 未驗證什麼／沒做什麼

- 沒有實機執行任何步驟；沒有建 JAR（本機執行限制：`mvn package` 會跑編譯出來的 AsRecord 工具）。
- 沒有寫腳本（T128）、沒有整理 M3 系列待驗證（T129）、沒有查 map 12000 的客戶端資產（T130）。
- 沒有修改 `gameplay.md`、T128 的 Issue 內文或 #285 的內文。
- 沒有改 `docs/REPORT.md`。

## 產出路徑

- `docs/acceptance/CHECKLIST.md`（改寫）
- `docs/acceptance/M4-RUNBOOK.md`（新）
- `docs/reports/customer-gaps.md`（G02、G19–G27 加連結）
- `TASKS.md`（T127 列）
- `docs/reports/issue-378.md`（本檔）

## 給下一棒的提示

- **T128（#379）**：照 CHECKLIST 的命名契約與 RUNBOOK 的 D1–D14；`m4-listen-check.ps1` 斷言四個監聽；`m4-prepare.ps1` 的 22 鍵檢查仍適用（`UnifiedConfiguration.KEYS` 沒有增減）；若主控同意，加 `m4-start-client`（細節見 RUNBOOK §P5，要設 `LITO_NAMED_DDRAW_SHA256`、`LITO_NATIVE_LOGIN_HOST`、`LITO_NATIVE_SOURCES`，第二視窗加三組 `--conditional-patch`，不寫入 `0-客戶端`）。
- **T129（#380）**：C 部接在 RUNBOOK 最後的「C 部」標題下，沿用六欄與 `V<序>-<短名>.png`；雙方同時斷線、強制結束伺服器後重啟已在 G24 註明留給 C 部。
- **T130（#381）**：結論回填 RUNBOOK G27 的表（四個欄位），並把 `customer-gaps.md` G27 一併更新。
- **Codex（#284／#285／T137）**：先看 RUNBOOK 的 P 部；沒有客戶端啟動腳本時，只有 A01、A02、A20 的伺服器部分與 A21 做得了。`frames.jsonl` 沒有 session 欄位，抓封包時一次只操作一個視窗並記時間。
