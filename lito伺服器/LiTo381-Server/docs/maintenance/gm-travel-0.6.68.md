# 0.6.68：GM 同地圖傳送及 Storm_Walk

本輪按使用者確認只加入 `.loc`、同地圖 `.move` 及 `wand.Storm_Walk`。不新增跨地圖勘查模式，不變更房間地圖或結算。

## 使用

進入假人房間並開始遊戲後，在聊天輸入：

| 操作 | 說明 |
| --- | --- |
| `.loc` | 顯示目前 X、Y、地圖編號／名稱與方向 |
| `.move X Y` | 傳送到目前地圖的指定座標 |
| `.move X Y 目前地圖ID` | 顯式指定目前地圖；不同地圖會提示並拒絕 |
| `.item 42501` | 使用既有建立道具指令取得「GM瞬間移動」 |
| 使用「GM瞬間移動」並點地面 | 以 `wand.Storm_Walk` 核心執行同地圖傳送 |

`.loc` 與 `.move` 定義位於 `380_lito.commands`，執行器分別為 `L1Loc`、`L1Move`，本次新增門檻 200。只依目前來源帳號的 `accounts.access_level` 判斷，且至少為正數；沒有帳號名稱白名單。SQL 讀取失敗、指令缺列或執行器不符均拒絕。重啟／重新登入後可依既有權限快照規則更新。

道具核心依共用靜態庫 `etcitem.classname='wand.Storm_Walk'` 在啟動時載入 ID 集合，目前查到 42501、`use_type=spell_long`（原生 useType 5）。其他道具 ID 使用相同 classname 也適用，變更後須重啟 LiTo。只接受背包中實際持有的物件 ID。沿用來源道具本身不檢查 GM 等級的語義；發放道具仍受 `.item` 權限控制。

## 行為與邊界

- 只在戰鬥遊戲世界使用；大廳、等待房間、觀戰及死亡不執行傳送。租用城鎮不在本輪範圍。
- 檢查原生地圖已載入範圍、可站立地面、玩家／召喚物／假人／守護塔／怪物／服務 NPC 與門障礙；無效落點不偷偷替換為其他座標。
- 麻痺或場景切換尚未完成時拒絕；同一傳送未完成不重複啟動。
- `S_PORTAL → C_GOTO_PORTAL` 沿用現有換景流程；收到確認後才更新座標、重建角色與可見物件，通知同場玩家。
- Storm_Walk 不消耗數量、使用次數、HP 或 MP。此次只傳送位置，不改技能或裝備效果。
- 不寫回來源角色座標／背包；本次 SQL 只新增 LiTo 專用的兩列指令，既有同名列保留。

## 證據與驗證

參照 `1-伺服器/src/com/lineage/server/command/executor/L1Loc.java`、`L1Move.java`，以及 `data/item_etcitem/wand/Storm_Walk.java` 和 `C_ItemUSe.java` 的 useType 5：道具物件 ID 後為目標 ID、X、Y，Storm_Walk 使用座標而非目標身分。來源中的 test 帳號跨圖特例不移植，本輪限定同圖；加入落點驗證以避免卡點。

實作 `GmTravel.java`；窄範圍修補由 `tools/resource-database/GmTravelPatch.java` 接入既有 GM／道具入口。部署 JAR 相對 0.6.67 只有 ClientSession、ResourceDatabase、GmTravel 三個 class 改變，客戶端 DLL 不變。

`run_travel_test.py` 已驗證實際 SQL classname／useType、可見座標提示、無權限／缺指令／SQL 失敗、跨圖／越界／佔用／無效座標拒絕、傳送確認前後位置、角色重建、同場通知、傳送後移動、道具不消耗及重複／錯誤封包拒絕。既有六項資源／房間／結算／清單／移動回歸及 Ctrl 強制攻擊回歸通過。原生啟動結果見同名 JSON，遊戲內滑鼠操作仍待使用者驗收。

安裝 SQL：`db/lito_gm_travel.sql`。維護執行器 `tools/resource-database/TravelDatabase.java` 只增缺列；執行期不自動建表。共同來源與 0.6.66 基準 ZIP 不變；未建立逐版整包備份。測試輸出集中 `tools/resource-database/build`、`runtime/gm-travel-regression`，沒有複製遊戲資源。
