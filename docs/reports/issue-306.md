# Issue #306 — T105：跨來源角色 ID 撞號處理

分支 `task/306`；階段 code；日期 2026-11-11。
路徑縮寫：`S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`，`T/` = 同層 `src/test/java/local/lito381/server/`。

## 做了什麼

1. **盤點**以角色 objid 當 key 的地方（完整表在 `docs/design/database.md` §10）：
   - 戰鬥內的「玩家是誰」全部走 `ClientSession.playerObjectId()`（src/main 約 1,545 處：房間名單、目標查找、封包 objectId、`BattleRules.Resolution.winners/losers`、斷線結算）。撞號時兩人被當成同一物件 → 勝負集合同時含兩人。
   - 結算收據與 `character_lito` 回寫本來就用 `(route.database, 原 objid)`（`S/DurableBattleSettlement.java:71-74`、`S/SettlementSql.java`），戰績名單每個 session 各自一份、排名以「來源名＋角色名」為 key，都不需改。
   - 找到一處誤用：盟屋競標 `persistHouseBid(this.playerObjectId(), …)`（`S/ClientSession.java:6919`）把執行期 ID 拿去寫庫，改成原 objid。
2. **實作來源區隔**：
   - 新增 `S/SourceObjectIds.java`：每個 `GameHub` 一張登記表，key `(來源名小寫, 原 objid)`。第一個佔用的來源保留原 objid；同來源同角色共用；其他來源撞號時配別名 `0x7800_0000..0x78FF_FFFF`（盤點過所有內部配號區段後選的空段，≤ `0x7FFF_FFFF`，封包仍是合法 32 位元）。別名在 session 存活期間不重配。
   - `ClientSession.playerObjectId()`（`S/ClientSession.java:57100-57106`）改查登記表；單一來源或 objid 超出 1..0x7FFF_FFFF 時直接回原值 → **單來源部署行為不變**。新增 `sourceCharacterObjectId()`（`:57109`）給資料庫用。
   - `DurableBattleSettlement.freeze`：凍結前 `requireDistinct`（`S/DurableBattleSettlement.java:47,64-69`），同一執行期 ID 對到不同（來源庫, objid）就拒絕凍結；收據建構抽成純函式 `entry(...)`（`:71-74`），勝負用執行期 ID，寫回用來源庫＋原 objid。
3. **單元測試** `T/SourceObjectIdsTest.java`（6 個）：
   - 登記表：先到保留原值、別名為 32 位元、同來源共用、別名不與後來的原 objid 撞、釋放與換角色、非法 objid。
   - 結算身分：兩來源同 objid → 收據 database 各自、`character` 都是原 objid、key 不同；舊行為（同 ID 對兩個來源）被 `requireDistinct` 拒絕。
   - 整合：CI 自測設定改成兩來源 `Sun,Moon`，兩位玩家 objid 都是 `0x10002A2A`，同房 DEATHMATCH，`hub.playerDefeated` → `winners={Sun 的 ID}`、`losers={Moon 的 ID}`，`GameStateStore` 戰績 Sun 1 勝 0 敗、Moon 0 勝 1 敗，再由 `entry(...)` 產出各自來源庫的收據。
4. **設計文件**：`docs/design/database.md` 新增 §10；差距矩陣 `docs/reports/server-capability-matrix.md` 第 51 行更新狀態。

## 怎麼驗證

- 本機：`mvn -B -q -o -DskipTests test-compile`（JDK 17）編譯通過，未在本機執行測試（依本機執行限制）。
- CI：`mvn -B test` 結果見下方「CI 結果」。

## CI 結果

（push 後補）

## 已驗證／推測／待驗證

**已驗證**
- 盤點表的行號逐一讀過；結算回寫原本就是「來源庫＋原 objid」。
- 程式編譯通過；CI 測試結果見上。

**推測**
- 所有送給客戶端的玩家 objectId 都經過 `playerObjectId()`（grep `Protocol.xxx(...).objectId()` 未找到直接用角色原 objid 的封包；`selectedCharacterSummary().objectId()` 的用法都是資料庫讀取或測試夾具判斷）。
- 正式 JAR 的 patch 類（tools/*/build.py）若有自己讀角色原 objid 當戰鬥 ID 的地方，本修改不會涵蓋；src/main 已併入的 patch 原始碼都走 `playerObjectId()`。

**待驗證**
- 真機：兩個來源各一個同 objid 角色，同房對戰（看得到彼此、能互打、結算畫面勝負正確），再查兩個來源庫 `character_lito` 各自 +1 勝／+1 敗（交給 Codex／使用者驗收；需要真實 MySQL 兩個來源庫，CI 只有 memory 模式）。
- 正式 0.6.71 JAR 未含本修改；要等 T002（src/main 建正式 JAR）或另行 patch 後才會生效。

## 產出路徑

- `S/SourceObjectIds.java`（新）
- `S/ClientSession.java`（`playerObjectId`、`sourceCharacterObjectId`、盟屋競標）
- `S/DurableBattleSettlement.java`（`Identity`、`requireDistinct`、`entry`）
- `T/SourceObjectIdsTest.java`（新）
- `docs/design/database.md` §10、`docs/reports/server-capability-matrix.md`

## 給下一棒的提示

- 新程式碼要拿「玩家在戰鬥裡的 ID」一律用 `playerObjectId()`；要查／寫來源庫一律用 `sourceCharacterObjectId()` 或 `CharacterSummary.objectId()`，兩者在多來源撞號時**不相等**。
- 若之後有其他程式用 `0x78xx_xxxx` 配號，要先改 `SourceObjectIds.ALIAS_FIRST/LAST`。
- 真機驗收：準備兩個來源庫（例如 `380_ac`、`380_ac_1`）各有一個 objid 相同的角色，啟用兩個 `lito_source_server` 列，兩個登入器分別選不同來源進同一房對戰。
