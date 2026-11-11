# Issue #310 — T103：HP／MP 與積分規則實作（依 T100 的決定）

更新：2026-11-11　分支：`task/310`

## 決定來源

`docs/design/gameplay.md` §7 原本「決定」欄是空的。#310 留言列出 D1～D10 的建議，使用者 15 分鐘未回覆，調度器依常設授權採用**逾時預設＝全部照建議**（#310 留言）。本任務依此實作，並把決定回填到 §7。使用者若不同意可在 #310 更正。

| # | 決定 | 本任務處理 |
|---|---|---|
| D1 積分公式 | A：勝 +250、敗 +50、斷線 −50（下限 0） | 程式不變；預設值寫進 `config/server.properties.example`；補測試 |
| D2 進場 HP／MP | A：每場補滿 | 程式不變；N5 定為正式規則（文件） |
| D3 積分持久 | B：在 `character_lito` 加欄位 | **未實作**，拆成後續任務（見下方原因） |
| D4 逾時無勝者 | B：平手，不記勝敗；攻城仍守方勝 | **已實作** |
| D8 寵物競賽 respawn 欄位 | A：改成 `true` | **已實作**（只改資料，行為不變） |
| D5～D7、D9、D10 | 不屬本任務範圍 | 只回填 §7 決定欄 |

## 做了什麼

1. `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleRules.java`
   - `MatchState.timeout`：寵物競賽、死鬥、Honor、Quest、Time Attack 逾時改呼叫新的 `finishDraw`，回傳**勝者、敗者都空**的結束結果（reason 不變，例如 `deathmatch-timeout`）。攻城有攻方時維持守方勝；攻城還沒指定攻方就逾時（邊界情況）也是平手。
   - `Resolution.draw(reason)`、`Resolution.isDraw()`：新增。
   - `Mode.PET_RACE` 的 respawn 欄 `false` → `true`（D8）。全專案只有 `BattleRules.decode`／`BattleControlCatalog.apply` 把它複製進 `RuleSet`，沒有其他讀取點，行為不變。
2. `lito伺服器/LiTo381-Server/config/server.properties.example`：新增 `battle.reward.win=250`、`battle.reward.loss=50` 與中文說明（D1）。值與 `ServerConfig.java:578-579` 的預設一致。
3. 測試 `src/test/java/local/lito381/server/GameplayDecisionRulesTest.java`（11 個，不需本機資源）：
   - D4：死鬥／Honor／Quest、寵物競賽、Time Attack（含有隊員陣亡）逾時都是平手；攻城逾時守方勝、敗者是攻方；攻城未指定攻方逾時是平手；決定性結果不是平手；平手時 `DurableBattleSettlement.entry` 回傳 null（不產生結算列）。
   - D8：`Mode.PET_RACE.respawn()` 為 true，且所有戰鬥模式的 respawn 欄都與 `playerDefeated` 的回傳一致。
   - D1：範例設定的兩個值是 250／50；積分公式（勝 +250、敗 +50、斷線 −50、從 0 斷線仍為 0）。
4. 文件：`docs/design/gameplay.md`（§2.1 共通規則、§2.2～§2.7 逾時列、§4.1、§4.3、N5、§7 決定欄）、`docs/reports/server-capability-matrix.md`（結算與排名、HP／MP、積分／排名三列）。
5. `docs/plans/issue-310.json`：拆出 D3 後續任務 T103b。

## 為什麼平手不需要改結算程式（已驗證・讀程式）

結算只處理「在勝者或敗者集合裡」的人，兩個都空時自然不寫：
- `DurableBattleSettlement.java:37`（`prepare` 只對 winners／losers 呼叫 `serverRecordBattleResult`）
- `DurableBattleSettlement.java:48`、`:72`（不在兩集合內就不產生日誌列）
- `ClientSession.java:75006-75012`（結果畫面時只對 winners／losers 記錄）
- 結果畫面：`GameHub.winningForce`（`GameHub.java:9672-9692`）兩集合都空時回 −1，`ClientSession.sendBattleResultWarning`（`ClientSession.java:10631-10641`）在 −1 時本來就送「此場對決雙方交戰平手」。

這和既有的「全員斷線＝無效場」（`TeamDisconnectSettlement.java:7`）走同一條路。

## D3 為什麼沒有在本任務實作

D3 要改的不只是程式：
- 需要對**每個來源庫**的 `character_lito` 做 `ALTER TABLE`（schema 遷移由 SQL 管理者執行；AI 不得碰正式 3306）。
- 結算日誌（`SettlementJournal` 的檔案格式）、收據表、`SettlementSql.drain` 的同列冪等更新、`SharedDatabase` 另一條 `character_lito` 寫入路徑、`DurableBattleSettlement.record` 的 baseline 計算都要一起改，否則會重複加分或重啟時讀錯。
- 這些路徑只有 `*RealMysqlTest` 才測得到，CI 沒有 MySQL。

在 code 階段把這些一起改、又無法實測，風險大於收益，所以拆成 T103b（`docs/plans/issue-310.json`），把要改的檔案與驗收列清楚。目前行為維持：積分、斷線數、月計重啟後歸零（gameplay.md §4.3）。

## 怎麼驗證

- **已驗證**
  - 本機 `mvn -B -o -DskipTests test-compile`（JDK 17）通過：main 與 test 都能編譯。依本機規定，本機不跑測試。
  - 讀程式確認：平手結果不會產生結算列、結果畫面顯示平手（見上節行號）；`Mode.respawn` 沒有其他讀取點。
  - CI 結果：見下方「CI」一節（push 後補）。
- **推測**
  - 單人測試房（ArenaBot）死鬥逾時：以前真人記敗、畫面顯示藍隊勝；現在顯示平手。測試房本來就不記戰績，只影響畫面文字。
  - Modern* 系列（`ModernBattleEngine` 等）的逾時結算沒有改；它們沒有正式工廠（`ModernMatchSettlementJdbc.java:30`），不在正式路徑上。
- **待驗證（實機，交給 Codex）**
  - 死鬥 180 秒逾時：兩人結果畫面都顯示「平手」，大廳戰績勝敗都不變、積分不變。
  - 攻城逾時：守方記勝、攻方記敗。
  - 寵物競賽 300 秒沒人跑完：全員不記勝敗。
  - 正式機 `server.properties` 的 `battle.reward.*` 實際值。

## CI

（push 後補上 PR 與結果）

## 產出路徑

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleRules.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/GameplayDecisionRulesTest.java`
- `lito伺服器/LiTo381-Server/config/server.properties.example`
- `docs/design/gameplay.md`、`docs/reports/server-capability-matrix.md`
- `docs/plans/issue-310.json`、`docs/reports/issue-310.md`

## 給下一棒的提示

- **T103b（D3 持久化）**：照 `docs/plans/issue-310.json` 的範圍做。關鍵是積分增量必須和 `last_result_id` 寫在**同一個** `character_lito` UPDATE（MyISAM 沒有交易），重送才不會重複加；先決定收據要不要凍結 `point_delta`（避免營運改設定後重送算出不同分數）。
- 若使用者推翻 D4（想回到全員記敗），只要把 `BattleRules.MatchState.timeout` 改回 `finish(Set.of(), reason)`，並刪掉 `GameplayDecisionRulesTest` 的 D4 測試。
- Codex：執行上面「待驗證」的實機項目。
