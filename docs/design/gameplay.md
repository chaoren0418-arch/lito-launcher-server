# LiTo 玩法設計規格（T100，Issue #301）

更新：2026-11-11。本文把**目前正式程式（`src/main`，與正式 0.6.71 JAR 語意等價，見 #78）裡的玩法規則**寫成規格。每條規則後面附程式出處；改規則時請同步更新本文與程式。
本文不改程式；第 7 節列出需要使用者決定的地方，決定後由 T103（#310）等任務實作。

## 讀法

**路徑縮寫**（與 `docs/reports/lito-capability-current.md` 相同）
- `S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`
- `D/` = `lito伺服器/LiTo381-Server/`
- `M/` = `lito伺服器/LiTo381-Server/docs/maintenance/`

**標記**
- 無標記：讀過程式，行號已確認（已驗證・程式）。
- 〔推測〕：從程式推論，沒有執行。
- 〔待驗證〕：要實機、DB 或雙人連線才能確認。
- 〔不一致〕：程式內或程式與文件互相矛盾，列入第 7 節。

**名詞**
- **參戰者**（combatant）：在房間裡、不是觀戰者的玩家。
- **隊伍**（force）：0＝紅隊、1＝藍隊（`S/GameHub.java:9765-9768` 只接受 0／1）。
- **結算記錄**：寫入 `character_lito` 的一筆勝或敗。

---

## 1. 總覽

| 項目 | 規則 | 出處 |
|---|---|---|
| 產品範圍 | 只做 LiTo 競技場（房間對戰）；天堂世界的 PK、安全區、死亡、持久化**不照搬** | `M/combat-parity-0.6.68.md:26` |
| 角色來源 | 唯讀讀取天堂來源庫的角色、裝備、技能；戰鬥內狀態只在記憶體 | `D/README.zh-TW.md:31`；`S/GameStateStore.java:7618-7619` |
| 唯一寫回 | 各來源庫的 `character_lito`（勝、敗、版本、最後結果 ID） | `S/SettlementSql.java:25-47` |
| 模式數 | 6 個戰鬥模式（wire 0～5）＋ 1 個非戰鬥場景（wire 6 城鎮） | `S/BattleRules.java:35-71` |
| 模式切換順序 | 房間左右箭頭依「寵物競賽→死鬥→Honor→Quest→Time Attack→攻城」循環（wire 0,1,5,2,4,3） | `S/BattleRules.java:119-143` |
| 規則覆寫 | `lito_battle_rule` 可依「模式＋地圖」覆寫時間、目標數、最少人數，或停用該地圖 | `S/BattleControlCatalog.java:204-215` |

---

## 2. 戰鬥模式

### 2.1 模式總表

欄位意思：**隊伍**＝分紅藍兩隊；**PvP**＝玩家可以互打；**時間**＝預設時限（秒）；**最少**＝開戰最少真人參戰者；**上限**＝參戰者上限（房間容量 2～100 再取小）。

| wire | 模式 | 隊伍 | PvP | 時間 | 最少 | 上限 | 地圖 | 出處 |
|---|---|---|---|---|---|---|---|---|
| 0 | 寵物競賽 Pet Race | 否 | 否 | 300 | 2 | 10 | 565、566、569、570 | `S/BattleRules.java:39-41,213`；`S/BattlePetRaceCourseCatalog.java:19-20`；`S/TournamentRoomPolicy.java:24` |
| 1 | 死鬥 Death Match | 是 | 是 | 180 | 2 | 房間容量 | 564、514、509、508、551、504、505、552、506、526、525、516、531、534、529 | `S/BattleRules.java:42-43,213` |
| 2 | Quest Battle | 是 | 否 | 600 | 2 | 房間容量 | 554、553、513、527、518、519、520、521、522 | `S/BattleRules.java:48-49,213` |
| 3 | 攻城 Siege War | 是 | 是 | 1200 | 30（每隊 15） | 100（每隊 50） | 573、563、547 | `S/BattleRules.java:28-33,53-55,212`；`S/NativeLitoSiegePolicy.java:21-23` |
| 4 | Time Attack | 否（合作） | 否 | 420 | 1 | 60 | 540 | `S/BattleRules.java:59-60,215`；`S/BattleTimeAttackEncounter.java:58` |
| 5 | Honor Match | 是 | 是 | 600 | 2 | 房間容量 | 510、528、532、512、517、524、530、535 | `S/BattleRules.java:63-64,213` |
| 6 | 城鎮 Town | — | — | — | 0 | — | 500、560 | `S/BattleRules.java:70-71,112`；不是戰鬥，不能開戰（`S/GameHub.java:9690-9695`） |

共通規則：
- 隊伍模式開戰時，紅藍兩隊都要有真人；房間勾「雙方人數一致」時兩隊人數必須相等（`S/NativeRoomRules.java:87-94`）。
- 最少人數 = `max(模式最少人數, 隊伍模式 ? 2 : 1)`；觀戰者與假人不計（`S/NativeRoomRules.java:82-86`）。
- 只有 PvP 模式能打玩家；隊伍模式不能打同隊、任何模式不能打自己（`S/BattleRules.java:337-343`）。
- 同一個玩家的死亡只算第一次（重複死亡封包不重複計分）；擊殺者 +1 擊殺數（`S/BattleRules.java:345-357`）。
- 進場時 HP／MP 一律補滿（見 §5.5）。

### 2.2 寵物競賽（wire 0）

| 項目 | 規則 | 出處 |
|---|---|---|
| 目標 | 依序通過檢查點跑完 **4 圈**；檢查點順序錯誤不算 | `S/BattleRules.java:495-507`；`S/BattlePetRaceCourseCatalog.java:19,152-154` |
| 開場 | 清除所有 buff／變身／料理，所有人換成伺服器選的同一種寵物外觀 | `S/ClientSession.java:11734-11791` |
| 賽道面板 | 變身面板、加速面板有冷卻；落後者拿到的變身不同 | `S/BattlePetRaceCourseCatalog.java:182-218` |
| 收尾窗口 | 第一名跑完後再給 **10 秒**（不超過總時限 300 秒），窗口結束才結算 | `S/BattlePetRaceCourseCatalog.java:21,160-165,255-257`；`S/GameHub.java:10264-10268` |
| 勝者人數 | 2～4 人：第 1 名；5～7 人：前 2 名；8～10 人：前 3 名；必須在窗口內跑完 4 圈 | `S/BattlePetRaceCourseCatalog.java:228-249` |
| 敗者 | 其餘參戰者全部記敗 | `S/BattleRules.java:561-569` |
| 死亡 | 不出局，回到出生點繼續（`playerDefeated` 回傳復活） | `S/BattleRules.java:359` |
| 逾時 | 沒有人跑完 → 無勝者，**全員記敗** | `S/BattleRules.java:525-540,561-569`；`S/GameHub.java:10266-10268` |
| 斷線 | 跑者斷線不中止比賽；斷線者若是第一名跑完者記勝，否則記斷線敗 | `S/BattleRules.java:452-460`；`S/GameHub.java:13070-13093` |
| 〔不一致〕 | `Mode` 表的 respawn 欄是 `false`，但死亡處理回傳「復活」；實際行為以死亡處理為準 | `S/BattleRules.java:39` 對 `:359` |

### 2.3 死鬥（wire 1）

| 項目 | 規則 | 出處 |
|---|---|---|
| 目標 | 消滅對方全隊 | `S/BattleRules.java:219` |
| 死亡 | **不復活**；死亡即出局 | `S/BattleRules.java:42,360` |
| 勝利 | 開場有 ≥2 隊、只剩 1 隊有人活著 → 該隊全員勝（含已陣亡的隊友） | `S/BattleRules.java:579-590,571-577` |
| 全滅 | 所有人都死 → 無勝者，全員記敗 | `S/BattleRules.java:580` |
| 逾時 180 秒 | 不比較擊殺數，無勝者，**全員記敗** | `S/BattleRules.java:531-534` |
| 測試房 | 頻道預設房（Local Lobby）的單人死鬥會加入 ArenaBot（§3.7） | `S/NativeRoomRules.java:51-53` |

### 2.4 Quest Battle（wire 2）

| 項目 | 規則 | 出處 |
|---|---|---|
| 形式 | 兩隊各自打 PvE、比誰先完成；玩家之間不能互打 | `S/BattleRules.java:48`（PvP＝false） |
| 目標 | 一般地圖：清完 4 個怪物房，再打掉最終守護塔；部分地圖：每個怪物房各有一個象徵物要打掉 | `S/BattleRules.java:221-223` |
| 進度 | 清一波怪只記隊伍進度，**不會**直接結束比賽 | `S/BattleRules.java:403-411` |
| 勝利 | 先打掉最終守護塔／最後象徵物的那一隊全員勝 | `S/BattleRules.java:379-401` |
| 死亡 | 會復活，死亡不影響勝負 | `S/BattleRules.java:362-367` |
| 逾時 600 秒 | 無勝者，全員記敗 | `S/BattleRules.java:531-534` |

### 2.5 攻城（wire 3）

| 項目 | 規則 | 出處 |
|---|---|---|
| 人數 | 每隊 15～50 人（`native.siege.roster.enforced=true` 時強制）；換隊時對方已滿 50 人會拒絕 | `S/NativeRoomRules.java:95-97`；`S/GameHub.java:9770-9777`；`D/config/server.properties.example:53` |
| 分隊 | 加入時自動進人數較少的一隊 | `S/GameHub.java:9803-9807,1614-1616` |
| 攻守 | 每場開戰時決定攻方隊伍（可隨機），與紅藍無固定關係 | `S/NativeLitoSiegePolicy.java:9-15,52-62`；`S/GameHub.java:10253-10257` |
| 目標 | 攻方：攻下據點（需先打掉 2 座外塔）；守方：撐到時間結束 | `S/NativeLitoSiegePolicy.java:24`；`S/BattleRules.java:225,473-480` |
| 勝利 | 攻方攻下據點 → 攻方全員勝；逾時 1200 秒 → **守方全員勝** | `S/BattleRules.java:473-480,535-536` |
| 死亡 | 會復活 | `S/BattleRules.java:373` |
| 未取得的原版規則 | 人數分級門檻、定時補給、MP 回復圈：程式明確標示「未取得」，目前沒有做 | `S/NativeLitoSiegePolicy.java:40-50` |

### 2.6 Time Attack（wire 4）

| 項目 | 規則 | 出處 |
|---|---|---|
| 形式 | 合作 PvE；1～60 人；房主為隊長 | `S/BattleRules.java:59,215,309-310`；`S/GameHub.java:10242-10244` |
| 計時起點 | 房主打掉守護塔才開始計 420 秒（之前不計時） | `S/BattleRules.java:313-318,427-433` |
| 目標 | 依序清完各波怪物與最終 Boss（有機率出現龍） | `S/BattleRules.java:224`；`S/GameHub.java:10213-10222` |
| 勝利 | 打倒最終 Boss／龍 → **當時活著的成員**勝；已陣亡的成員記敗 | `S/BattleRules.java:417-425` |
| 失敗 | 隊長陣亡、或全隊陣亡 → 全員記敗；隊員陣亡不復活但比賽繼續 | `S/BattleRules.java:368-372` |
| 逾時 | 無勝者，全員記敗 | `S/BattleRules.java:531-534` |
| 斷線 | 斷線者記斷線敗，其餘人繼續；隊長斷線時交給第一位在場隊員 | `S/BattleRules.java:435-450,462-467`；`S/GameHub.java:13041-13068` |

### 2.7 Honor Match（wire 5）

| 項目 | 規則 | 出處 |
|---|---|---|
| 目標 | 打掉對方守護塔 | `S/BattleRules.java:220` |
| 勝利 | 打掉守護塔的一方全員勝 | `S/BattleRules.java:379-389` |
| 死亡 | **5 秒後在死亡位置復活**（不回出生點） | `S/GameHub.java:23-24,9932-9944`；`S/ClientSession.java:26734-26767` |
| 逾時 600 秒 | 無勝者，全員記敗 | `S/BattleRules.java:531-534` |

### 2.8 復活規則一覽

| 模式 | 死亡後 | 出處 |
|---|---|---|
| 寵物競賽、Quest、攻城 | 回本隊出生點、HP 補滿 | `S/BattleRules.java:359,366-367,373`；`S/ClientSession.java:26769-26779` |
| Honor Match | 5 秒後原地復活、HP 補滿 | `S/ClientSession.java:26734-26767` |
| 死鬥、Time Attack | 不復活 | `S/BattleRules.java:360,368-372` |

---

## 3. 房間與對戰流程

### 3.1 頻道

| 項目 | 規則 | 出處 |
|---|---|---|
| 頻道分級 | 0 DeathKnight（52 級以上）、1 Baphomet（48～51）、2 Succubus（40～47）、3 Ogre（15～39）；每頻道 100 人 | `S/TournamentPresence.java:19,24-30` |
| 未滿 15 級 | 沒有對應頻道，不能建房也不能進房 | `S/TournamentRoomPolicy.java:9-18` |
| 建房 | 只能在自己等級的頻道或**更高等級**的頻道建房 | `S/TournamentRoomPolicy.java:9-12` |
| 進房 | 同上；高等級玩家要進低等級頻道的房間，房間必須勾「允許上位」（0x10） | `S/TournamentRoomPolicy.java:14-18`；`S/GameHub.java:1371-1381` |
| 觀戰目錄 | 頻道 99（`/argo`）是觀戰目錄，看得到已開戰的房間 | `S/NativeRoomAdmission.java:22-32` |

### 3.2 建房與房間設定

| 項目 | 規則 | 出處 |
|---|---|---|
| 建房 | 玩家建的房間拿新 ID；最後一人離開就刪除 | `S/GameHub.java:1392-1400,1627-1641` |
| 容量 | 2～100；實際參戰上限再依模式取小（§2.1） | `S/TournamentRoomPolicy.java:7,20-29` |
| 屬性位元 | 0x04 有密碼、0x08 允許觀戰、0x10 允許上位、0x20 雙方人數一致 | `S/GameHub.java:91-98` |
| 活動房限制 | 限職業（5 職業位元遮罩；3.81 後加的職業 5／6 無法勾選）、限來源伺服器（最多 8 個來源 ID）、限隊伍名（隊伍名＝血盟名，與房名相同才能進） | `S/GameHub.java:147-186,1591-1604` |
| 房主 | 第一個進房的人；房主離開時交給下一位 | `S/GameHub.java:1612,13181-13183` |
| 房主權限 | 換地圖（換地圖會清掉所有人的準備）、開始／取消 | `S/GameHub.java:9734-9740,9874-9884` |
| 踢人 | 被踢者不能再進同一個房間 | `S/GameHub.java:1582-1584` |

### 3.3 進房檢查（依序）

1. 房間屬於自己的頻道（`S/GameHub.java:1573-1576`）
2. 沒被踢過（`:1582-1584`）
3. 房間不在結果畫面中（`:1585-1587`）
4. 已開戰時只有觀戰者能進（`:1588-1590`）
5. 活動房的隊伍名／職業／來源限制（`:1591-1604`）
6. 參戰者未滿（`:1605-1609`）
7. 隊伍模式自動分到人數較少的一隊（`:1614-1616`）

### 3.4 準備與開戰

| 項目 | 規則 | 出處 |
|---|---|---|
| 準備 | 參戰者按準備；倒數中再按一次＝取消準備；觀戰者不能準備 | `S/GameHub.java:9685-9706` |
| 開始倒數 | 所有參戰者都準備、且通過 §2.1 的人數與分隊檢查 → 倒數 3 秒 | `S/GameHub.java:9707-9716` |
| 取消倒數 | 倒數中有人取消準備、換隊、離開或轉觀戰，條件不再成立就取消 | `S/GameHub.java:9717-9722,9762-9786,9755-9760,13167-13179` |
| 開不了時 | 伺服器用系統訊息說明原因（人數不足、缺一隊、人數不一致、攻城每隊 15～50） | `S/NativeRoomRules.java:70-114` |
| 開戰 | 倒數到 0 再檢查一次，固定參戰名單與隊伍，重置地圖物件，所有人先準備好再一起進場 | `S/GameHub.java:10156-10259`；`S/GameHub.java:10117-10124` |
| 開戰後 | 房間從大廳列表隱藏；只有觀戰者能再進 | `S/NativeRoomAdmission.java:22-43`；`S/GameHub.java:1588-1590` |

### 3.5 觀戰

| 項目 | 規則 | 出處 |
|---|---|---|
| 條件 | 房間要允許觀戰（0x08）；預設房與自動配對房都允許 | `S/GameHub.java:1436,1529,13185-13193` |
| 權限 | 觀戰者不佔參戰名額、不能準備、不算開戰人數 | `S/GameHub.java:1548,9687`；`S/NativeRoomRules.java:84-85` |
| 結算 | 觀戰者不寫戰績、不進斷線判定 | `S/DurableBattleSettlement.java:45`；`S/GameHub.java:12971`；`S/ClientSession.java:87746` |

### 3.6 結束、結果畫面與離開

| 項目 | 規則 | 出處 |
|---|---|---|
| 結束時機 | 勝負成立（§2）、逾時、或斷線判定結束（§3.8） | `S/GameHub.java:10260-10278,13095-13149` |
| 結算順序 | **先凍結結果、寫入可重試的日誌，再送結果畫面**；日誌沒存好，房間不能清除 | `S/ClientSession.java:11809-11810`；`S/DurableBattleSettlement.java:33-58`；`S/GameHub.java:6993-6994` |
| 結果畫面 | 結果畫面期間房間不開放進入；所有人確認離開後才回到可用狀態 | `S/GameHub.java:7002-7005` |
| 本場物品 | 開啟原生經濟時：結算移除本場買的消耗品；回大廳時重新給固定金幣（預設 20,000） | `S/ClientSession.java:20453-20470`；`S/ServerConfig.java:583-585`；`D/config/server.properties.example:55-56` |
| 離開房間 | 開戰前離開：清掉準備與隊伍、交出房主、空房刪除（預設房保留） | `S/GameHub.java:1627-1641` |

### 3.7 測試房與 ArenaBot

| 項目 | 規則 | 出處 |
|---|---|---|
| 測試房 | 每個頻道一個預設房「Local Lobby」：死鬥、容量 8、允許觀戰；由 `lito_room_policy.test_room_enabled` 控制 | `S/GameHub.java:12891-12896,13185-13193`；`D/db/lito_room_policy.sql` |
| 不計戰績 | 測試房的結果**不寫** `character_lito`、不加積分；進房時系統訊息提示 | `S/DurableBattleSettlement.java:45,69`；`S/ClientSession.java:87743`；`S/NativeRoomRules.java:115-119` |
| 自動配對 | 自動配對永遠不會選到測試房 | `S/GameHub.java:1424-1427` |
| ArenaBot 出現條件 | 測試房 ＋ 死鬥 ＋ `test_bot_enabled=1` ＋ 只有 1 位真人 ＋ 沒有 GM 假人 | `S/NativeRoomRules.java:51-62` |
| ArenaBot 規則 | 藍隊、HP 100；真人必須選紅隊才能開始；真人陣亡＝藍隊勝 | `S/GameHub.java:32,9851-9858`；`S/NativeRoomRules.java:66-68,77-81` |
| 正式房 | 正式房永遠不放 ArenaBot；只有真人或 GM 以 `.de` 建立的假人 | `S/BattleRules.java:107-110`；`S/GameHub.java:215-216` |
| 〔待驗證〕 | 範例設定 `battle.ai.enabled=false`（ArenaBot 戰鬥 AI 關閉），此時 ArenaBot 是否只站著挨打要實機確認 | `D/config/server.properties.example:43-44` |

### 3.8 斷線

| 項目 | 規則 | 出處 |
|---|---|---|
| 合併窗口 | 300 毫秒內的多個斷線當成同一事件判定 | `S/GameHub.java:22,12969-12982` |
| 全員斷線 | 2 人以上全部斷線 → 比賽**無效**，沒有人記勝敗 | `S/TeamDisconnectSettlement.java:7` |
| 還能打 | PvP：還有 ≥2 隊（隊伍模式）或 ≥2 人（個人）留下；PvE：還有人留下 → 比賽繼續，斷線者記斷線敗 | `S/TeamDisconnectSettlement.java:9-12,16-23` |
| 打不下去 | 留下的人全部記勝，斷線者記斷線敗，比賽結束 | `S/TeamDisconnectSettlement.java:12` |
| 寵物競賽、Time Attack | 有專用規則（§2.2、§2.6） | `S/GameHub.java:13041-13093` |
| 重試 | 判定失敗時 2 秒後重試；日誌背景每 2 秒重送 | `S/TeamDisconnectSettlement.java:28-32`；`S/DurableBattleSettlement.java:22,111-122` |

### 3.9 自動配對（Match Making）

| 項目 | 規則 | 出處 |
|---|---|---|
| 依據 | 2003 年原版說明：找勝率相近的玩家 | `S/NativeLitoMatchMakingPolicy.java:3-13` |
| 相容條件 | 同頻道、同請求欄位（固定選項、頻道限制、配對方式、人數限制） | `S/GameHub.java:1515-1523` |
| 排序 | 勝率差（勝÷(勝＋敗)）由小到大；兩人都沒戰績＝差 0，只有一人沒戰績＝差最大；同差距先到先配 | `S/NativeLitoMatchMakingPolicy.java:39-57` |
| 成局 | 湊滿該模式最少人數就建「Auto Match」房（允許觀戰；隊伍模式加「雙方人數一致」）；最少人數 1 的模式直接建房 | `S/GameHub.java:1467-1492,1525-1537` |
| 不考慮來源 | 配對**不看**來源伺服器 | `S/GameHub.java:1515-1523`（條件內沒有來源） |
| 原版未取得 | 最大允許勝率差、零戰績處理、同分規則都是本專案自訂 | `S/NativeLitoMatchMakingPolicy.java:27-37` |

---

## 4. 結算與排名

### 4.1 誰會被記錄

| 情況 | 記錄 | 出處 |
|---|---|---|
| 勝者 | 勝 +1 | `S/BattleRules.java:561-569`；`S/SettlementSql.java:38-39` |
| 參戰但不是勝者（含逾時無勝者、陣亡、Time Attack 已陣亡者） | 敗 +1 | `S/BattleRules.java:564-565` |
| 斷線 | 敗 +1，並標記「斷線」 | `S/DurableBattleSettlement.java:48`；`S/GameStateStore.java:5603-5608` |
| 全員斷線（無效場） | 不記 | `S/TeamDisconnectSettlement.java:7` |
| 觀戰者、測試房、GM 假人、ArenaBot | 不記 | `S/DurableBattleSettlement.java:45`；`S/ClientSession.java:87743-87746` |

### 4.2 `character_lito` 寫回（持久）

| 項目 | 規則 | 出處 |
|---|---|---|
| 位置 | 玩家**自己來源庫**的 `character_lito`（跨來源同場時各寫各的） | `S/DurableBattleSettlement.java:32,46-48`；`S/SettlementSql.java:25-47` |
| 欄位 | `wins`、`losses`、`version`、`last_result_id`、`char_name`；**沒有**積分、斷線數、月計 | `S/SettlementSql.java:35-39` |
| 冪等 | 結果 ID＝SHA-256(場次｜資料庫｜角色)；先寫收據表 `lito_battle_settlement`（必須 InnoDB），再用 `GET_LOCK`＋`last_result_id` 更新計數；重送不會重複加 | `S/SettlementSql.java:7,8-20,25-47` |
| 身分保護 | 角色名與 objid 對不上（角色被改名／刪除）就拒絕寫入 | `S/SettlementSql.java:33` |

### 4.3 積分（ranking_points）

| 項目 | 規則 | 出處 |
|---|---|---|
| 勝 | +`battle.reward.win`（預設 **250**） | `S/ClientSession.java:87748`；`S/ServerConfig.java:578` |
| 一般敗 | +`battle.reward.loss`（預設 **50**；輸了也加分） | `S/ClientSession.java:87748`；`S/ServerConfig.java:579`；`S/GameStateStore.java:5610-5614` |
| 斷線敗 | −`battle.reward.loss`（預設 **−50**），最低 0 | `S/GameStateStore.java:5613-5618` |
| 依據 | 程式註解：「2007 年規則：一般敗場給較少正分，只有強制斷線扣分；數值由營運設定」 | `S/GameStateStore.java:5610-5612` |
| 月計 | 另外累計本月積分、本月勝敗；月份以「台北時間每天 05:00」為日界，跨月第一次結算時歸零 | `S/GameStateStore.java:1097-1098,5599,5966-5994` |
| 排行榜 | 總榜：積分→勝場→名字；月榜：本月積分→本月勝場→總積分→名字；榜單快照每天 05:00 更新 | `S/GameStateStore.java:5811-5854`；`:1098` |
| 經驗與等級 | 勝 +100、敗 +25（料理可加成），LiTo 內部等級＝1＋經驗÷1000；**只在記憶體**，不寫 `characters` | `S/GameStateStore.java:5619-5624` |
| **持久性** | 積分、斷線數、月計**不跨伺服器重啟**：從 `character_lito` 重新載入時只有勝敗，積分等欄位一律從 0 開始 | `S/SharedDatabase.java:169-175`；`S/GameStateStore.java:819-822`；`D/README.zh-TW.md:31` |
| 〔待驗證〕 | 範例設定沒有列出 `battle.reward.*`；營運是否能改、正式機實際值為何，要看正式 `server.properties` | `D/config/server.properties.example` |
| 「榮譽點數」 | 程式中沒有獨立的榮譽點數欄位（LC B17） | — |

### 4.4 Tournament 商店

| 項目 | 規則 | 出處 |
|---|---|---|
| 金幣 | 每場固定 20,000，不累積；只能買本場用的消耗品 | `S/TournamentShopCatalog.java:12-19`；`S/ClientSession.java:20466-20470` |
| 品項 | 16 項（藥水、箭、魔法寶石、變身卷軸、磨刀石等）；可由資料庫覆寫 | `S/TournamentShopCatalog.java:38-54,83-97` |
| 賣回 | 全額退回原價 | `S/TournamentShopCatalog.java:107-112` |
| 來源說明 | 價目是同系列 2003 年 LiTo 資料，不是 3.81C 原服抓到的清單 | `S/TournamentShopCatalog.java:12-16` |
| 開關 | `native.tournament.economy.enabled` 預設 `false`；關閉時商店點數改用勝敗累積的點數 | `S/ServerConfig.java:583-585`；`S/ClientSession.java:87754-87756` |

---

## 5. 跨界對戰（多來源同房）

| 項目 | 規則 | 出處 |
|---|---|---|
| 來源 | 一個 LiTo 程式最多接 8 個來源（`lito_source_server`，ID 1～8）；每個來源一個登入埠（基準埠＋source_id） | LC X1–X5；`D/config/server.properties.example:16-17` |
| 同房 | 不同來源的玩家進同一個頻道、同一個房間、同一場戰鬥，沒有限制 | `S/GameHub.java:1569-1619`（只有活動房的來源限制） |
| 限制來源 | 只有活動房勾「限制伺服器」時，才只讓清單內的來源 ID 進場 | `S/GameHub.java:184-186,1601-1604` |
| 顯示來源 | 房間與戰鬥內顯示「帳號(來源名)」，總長不超過 27 bytes，太長就截短來源名 | `S/ClientSession.java:87790-87804` |
| 排行榜 | 名字帶來源名 | `S/GameStateStore.java:6001-6002` |
| 配對 | 不看來源、不做來源間平衡 | `S/GameHub.java:1515-1523` |
| 結算 | 每位玩家寫回**自己來源庫**的 `character_lito` | `S/DurableBattleSettlement.java:32,46-48` |
| 已知風險 | 戰鬥內玩家 ID 直接用來源 objid；兩個來源撞號時的處理未實作（T105 #306） | LC X10；`S/ClientSession.java:88170-88174` |
| 不做 | 不同 LiTo 程式之間互連（跨程式對戰） | LC X15 |

---

## 6. 刻意不做的項目（明文規則）

下列是**設計上不做**，不是缺口。要改變任何一項，先改本節並經使用者決定。

| # | 項目 | 規則 | 依據 |
|---|---|---|---|
| N1 | 死亡懲罰 | 戰鬥中死亡沒有經驗損失、掉裝、負面狀態；只影響本場勝負 | `M/combat-parity-0.6.68.md:26`；LC B22 |
| N2 | 天堂經驗 | 不增加角色本體經驗；LiTo 內部經驗只在記憶體（§4.3） | `S/GameStateStore.java:5619-5624`；LC A12 |
| N3 | 寫回角色本體 | 不寫 `characters`、技能、任務、`character_items`；只寫 `character_lito` 勝敗 | LC A1、A14、I13；`D/README.zh-TW.md:31` |
| N4 | 捲軸強化／爆裝 | 不做；裝備用角色既有強化值。唯一例外是技能「創造魔法武器」把 +0 變 +1（戰鬥內） | LC I6；`docs/reports/server-capability-matrix.md:61,92` |
| N5 | 讀存檔 HP／MP | 不讀存檔的 CurHp／CurMp；每場進場與每次復活都補滿 | `S/ClientSession.java:11792,12575-12589,26742,26770`；LC A3 |
| N6 | 購買與消耗持久化 | 商店購買、藥水消耗、本場金幣都只在本場（記憶體） | LC I13；`S/ClientSession.java:20455-20470` |
| N7 | 正式房假人 | 正式房不放 ArenaBot | `S/BattleRules.java:107-110` |
| N8 | 天堂世界規則 | PK、粉名、安全區、地獄等世界規則不套到競技場 | `M/combat-parity-0.6.68.md:26`；`docs/reports/server-capability-matrix.md:22` |

注意：N5 目前是「程式就是這樣」，是否正式定為規則列在 D2 待決定。

---

## 7. 需要使用者決定

每項列出選項與建議。決定後回填本表「決定」欄，並由對應任務實作。

| # | 問題 | 選項 | 建議 | 實作任務 | 決定 |
|---|---|---|---|---|---|
| D1 | 積分公式 | A. 維持現狀：勝 +250、敗 +50、斷線 −50<br>B. 輸了不加分：勝 +N、敗 0、斷線 −N<br>C. 依對手強弱加減（Elo 類） | **A**：程式已有，且註解有 2007 年規則依據（敗場也給小額正分、只有斷線扣分）。先把預設值寫進 `server.properties.example`，讓營運看得到、改得了 | T103 #310 | |
| D2 | 進場 HP／MP | A. 每場補滿（現狀）<br>B. 讀存檔 CurHp／CurMp | **A**：競技場重視公平；而且來源庫唯讀，B 只能讀不能寫回，打完 HP 也無法保存，意義不大。決定後把 N5 定為正式規則 | T103 #310 | |
| D3 | 積分是否要持久 | A. 不持久（現狀：重啟歸零，月榜跟著歸零）<br>B. 在 `character_lito` 加欄位（ranking_points、disconnects、月計）<br>C. 另建中央表 | **B**：月榜與排行榜在重啟後歸零，玩家看得到差異。B 沿用現有 `last_result_id` 冪等機制，改動最小；需配合 T104 確認 MyISAM 下的寫法 | T103 #310、T104 #302 | |
| D4 | 逾時無勝者時記敗 | A. 全員記敗（現狀：寵物競賽、死鬥、Quest、Honor、Time Attack）<br>B. 逾時視為平手，不記勝敗<br>C. 依分數判勝（擊殺數、塔血量、進度） | **B**：A 會讓雙方都 +1 敗並都 +50 積分，戰績失真；C 沒有原版判勝規則可依（程式註解明寫「未取得」）。攻城維持守方勝 | T103 #310 | |
| D5 | PvE 範圍 | A. 只做競技場內 PvE（Quest、Time Attack 的怪物與 Boss）<br>B. 加上城鎮場景（wire 6）<br>C. 加上天堂世界（野外、副本） | **A**：產品是競技場；588 列非玩家技能只需涵蓋 Quest／Time Attack 用到的怪物。B、C 留在 Backlog | T107–T109 範圍 | |
| D6 | 跨來源配對 | A. 不看來源（現狀）<br>B. 優先同來源<br>C. 跨來源時兩隊來源平均 | **A**：人數少時任何限制都會拉長等待；跨來源是賣點。先完成 T105 撞號再開放大量跨來源 | T105 #306 | |
| D7 | 攻城人數下限 | A. 每隊 15 人（現狀，依 2005 年資料）<br>B. 降低（例如每隊 5 人）方便測試與小規模營運 | 依營運實際人數決定；15v15 需要 30 人同時在線。若人數不足，**建議 B** 並保留開關 `native.siege.roster.enforced` | 新任務 | |
| D8 | 寵物競賽 respawn 欄位不一致 | A. 修正 `Mode` 表為 `true`（與實際行為一致）<br>B. 維持 | **A**：只是資料一致，不改行為 | T103 或 B-land 後小修 | |
| D9 | ArenaBot 戰鬥 AI | A. 預設開（`battle.ai.enabled=true`）<br>B. 預設關（現狀範例） | 先實機看 B 的行為（§3.7 待驗證）；若 ArenaBot 不會還手，**建議 A**，否則單人測試意義不大 | 新任務（測試） | |
| D10 | Tournament 商店價目 | A. 維持 2003 同系列價目<br>B. 改由營運在 DB 自訂 | **A**；需要時用 DB 覆寫（程式已支援），沒有 3.81C 原服價目可依 | — | |

---

## 已驗證／推測／待驗證

- **已驗證（讀程式，行號確認）**：§1～§6 未加標記的條目。
- **推測**：
  - 積分重啟歸零：依 `SharedDatabase.java:169-175` 載入時把積分設 0，以及 README:31 的說明推論；沒有實際重啟確認。
  - 自動配對最少人數 30 的攻城實務上難以成局（依 §3.9 規則推論）。
- **待驗證（實機／DB）**：
  - ArenaBot 在 `battle.ai.enabled=false` 時的行為（§3.7）。
  - 正式機 `battle.reward.win/loss` 實際值（§4.3）。
  - 跨來源雙人同房實機（LC X11、X13）。
  - 斷線合併 300 毫秒在雙人實機的表現（LC B18）。
