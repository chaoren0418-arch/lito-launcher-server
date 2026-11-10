# Issue #301 報告：T100 LiTo 玩法設計文件

## 做了什麼
- 新增 `docs/design/gameplay.md`：從 `src/main` 整理出現行玩法規則，每條附「相對路徑：行號」。內容：
  1. 六個戰鬥模式（＋城鎮場景）的人數、時間、地圖、勝負條件、復活、斷線（§2）；
  2. 頻道分級、建房、進房檢查、準備／倒數／開戰、觀戰、結果畫面、離開、斷線、自動配對、測試房與 ArenaBot（§3）；
  3. `character_lito` 寫回、ranking_points 加減、月計、排行榜排序、Tournament 商店（§4）；
  4. 跨來源同房、顯示來源、配對、結算（§5）；
  5. 刻意不做清單 N1～N8（死亡懲罰、經驗、寫回角色本體、捲軸強化、讀存檔 HP／MP 等）（§6）；
  6. 需要使用者決定 D1～D10，每項附選項與建議（§7）。
- 沒有改任何程式、SQL、設定。

## 整理時發現的重點（給使用者）
- **逾時無勝者＝全員記敗**：寵物競賽、死鬥、Quest、Honor、Time Attack 逾時，所有參戰者都 +1 敗、+50 積分（`BattleRules.java:525-540,561-569`）。列為 D4。
- **積分不持久**：`character_lito` 只有勝敗；重啟後積分、斷線數、月計都從 0 開始（`SharedDatabase.java:169-175`、`README.zh-TW.md:31`）。列為 D3。
- 積分預設值勝 +250／敗 +50／斷線 −50（`ServerConfig.java:578-579`、`GameStateStore.java:5610-5618`），但範例設定檔沒列出這兩個鍵。
- 寵物競賽 `Mode` 表 respawn=false，但死亡處理實際是復活（`BattleRules.java:39` 對 `:359`）。列為 D8。
- LC A3 引用的 `ClientSession.java:11400` 是租屋城鎮的補滿；戰鬥進場補滿實際在 `resetBattleVitals`（`ClientSession.java:12575-12589`，由 `:11792` 呼叫）。本文用後者。

## 怎麼驗證
- 逐條讀程式確認行號：`BattleRules`、`GameHub`（建房、進房、準備、倒數、開戰、斷線、結束）、`NativeRoomRules`、`NativeRoomAdmission`、`TournamentRoomPolicy`、`TournamentPresence`、`TeamDisconnectSettlement`、`DurableBattleSettlement`、`SettlementSql`、`GameStateStore`（recordResult、月計、排行榜）、`SharedDatabase`、`NativeLitoSiegePolicy`、`NativeLitoMatchMakingPolicy`、`BattlePetRaceCourseCatalog`、`TournamentShopCatalog`、`BattleControlCatalog`、`ServerConfig`、`ClientSession`（結算、顯示名、進場、復活）、`db/lito_room_policy.sql`、`config/server.properties.example`、`README.zh-TW.md`。
- 本任務只改文件，沒有在本機執行 `mvn test`（本機規定測試交給 CI）；PR 的 CI 結果即為 `mvn -B test` 驗證。

## 已驗證／推測／待驗證
- **已驗證**：`gameplay.md` 中未加標記的條目（讀過程式、行號確認）。
- **推測**：積分重啟歸零（依載入程式與 README 推論，沒實際重啟）；攻城自動配對需 30 人才成局，實務難成局。
- **待驗證**：ArenaBot 在 `battle.ai.enabled=false` 時的行為；正式機 `battle.reward.*` 實際值；跨來源雙人同房實機；300 毫秒斷線合併的雙人實機表現。

## 沒做／未驗證
- 沒有參考 `1-伺服器`（S1 沒有競技場模式，差距矩陣已註明不需要）。
- 沒有查 50635 等候選版的規則差異（Quest／Time Attack 遭遇戰在候選有改版，LC B21）；本文只描述正式 `src/main`。

## 產出路徑
- `docs/design/gameplay.md`
- `docs/reports/issue-301.md`

## 給下一棒的提示
- 使用者回覆 §7 的 D1～D10 後，把「決定」欄填上；T103（#310）依 D1～D4、D8 實作，T104（#302）配合 D3 決定 `character_lito` 是否加欄位。
- D5 的決定會影響 T107～T109 是否要涵蓋 588 列非玩家技能。
- B-land（#138）後，Quest／Time Attack 若換成 50635 候選的遭遇戰，§2.4、§2.6 要重新核對行號。
- 本文的行號依 `origin/main` 8e64d48e；B-land 會大幅移動 `GameHub`／`ClientSession` 行號，屆時整份要重掃一次。
