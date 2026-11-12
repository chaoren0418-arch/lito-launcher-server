# M2 客戶缺口清單（T070 第一版）

更新：2026-11-10（Issue #288）。交付基準＝正式 0.6.71（登入器 EXE `0BF5DDF2…`＋伺服器 JAR `E1498871…`）。

> **來源 (a)（M1 實機回歸的失敗／異常）尚未併入。** #284 的正式 JAR 啟動預檢阻塞、#285（T011 跑表）尚未執行；等 #285 完成後，依 [`docs/acceptance/CHECKLIST.md`](../acceptance/CHECKLIST.md#L104) 的「失敗／異常記錄」補進本表並重新排序，屆時 M2 主線才封頂。本版只含來源 (b) 合約要求與來源 (c) 候選已修項。

## 判讀規則

- **來源 b**：`CLAUDE.md`「目標」的合約項目（登入、大廳、技能、進房、戰鬥結算、正常退出）及 CHECKLIST 每一步，找「正式 0.6.71 已知做不到」或「從未驗證」的證據。
- **來源 c**：隔離候選有**同 SHA 收據**、修的是**玩家看得到的行為**、而正式 0.6.71 沒有的項目。純內部重構、只在競態下出現、WORLD 專屬（產品只有競技場）或沒有測試收據者一律進 Backlog。
- **建議「修」**只給有證據的項目；「從未驗證」的項目沒有失敗證據，一律標 `Backlog（待 #285）`，等 M1 結果再決定升級與否。
- 排序＝影響範圍（全部玩家＞某職業／技能玩家＞GM＞營運部署）× 發生頻率（每次啟動／每次施放＞特定時機＞競態）。
- 證據等級：**已驗證**＝文件或收據有直接紀錄；**推測**＝由差異推論、尚未在正式版重現；**待驗證**＝需實機或正式版重現。

## 關鍵背景（已驗證）

- 正式 0.6.71 的戰鬥核心 ≈ 候選 `8d26c9d7…`：類別比對 `8d26 → 正式` 為 SAME 2546／CHANGED 4／只在正式 4（[issue-30.md:22](issue-30.md#L22)）；正式獨有的是 GmTravel、NativeArmorStatus（AC 修正）、ClientSession、Protocol 等（[issue-30.md:56](issue-30.md#L56)）。因此 `8d26` 以前的候選修正**已在正式版**，本表只看 `8d26` 之後的修正。
- `50635f7e…` 相對 `8d26`：CHANGED 140／NEW 483／只在 8d26 0（[issue-30.md:23](issue-30.md#L23)），是 `8d26` 之後各增量候選的上層。
- 使用者確認 **LiTo 只有競技場、沒有 WORLD 地圖**，WORLD 專屬修正不列產品門檻（[combat-continuation-current.zh-TW.md:14](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L14)）。
- 候選測試收據**只適用各自 SHA**，不跨版本當通過（[combat-closeout-current.zh-TW.md:11](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-closeout-current.zh-TW.md#L11)）。

## M2 主線候選（前 8 項）

| 編號 | 現象（玩家看到什麼） | 影響誰 | 證據 | 來源 | 建議 | 最小修改的位置推測 |
|---|---|---|---|---|---|---|
| G01 | 營運用「逐表 SELECT」的受限 DB 帳號部署時，伺服器在資源初始化讀 `角色_智力成長` 被拒，**1.6 秒內以退出碼 1 結束**，玩家完全無法登入。INSTALL 只寫「來源庫 SELECT」，#83 逐表授權清單（依 Accounts.java 盤點）漏了此表。〔已驗證：實際啟動失敗；其他漏表待驗證〕 | 營運（間接：全部玩家） | [issue-284.md:5](issue-284.md#L5)、[issue-284.md:90-108](issue-284.md#L90)（`NativeProcHealBonus.read` 堆疊）；[issue-83.md:49](issue-83.md#L49)（只列精神／體質／力量／敏捷成長）；[INSTALL.md:56](../INSTALL.md#L56) | b | 修 | 不改程式：補「正式 JAR 實際讀取表」完整清單（從正式 JAR 的 `ResourceDatabase.load`／`NativeProcHealBonus` 等 SQL 字串靜態盤點），更新 `docs/INSTALL.md` 第 10 步與實驗庫授權。與 PLAN U1-2 重疊，可能直接在 M1 處理 |
| G02 | 技能 26 **通暢氣脈術**（CLAN 目標）對自己或友方施放時，**必須按 Ctrl 才放得出來**（使用者回報）。正式 0.6.71 與候選的伺服器合成封包 16 案都能正常自施，代表原因多半在客戶端游標／送包，**至今未修**。〔已驗證：回報存在；原因推測〕 | 玩家（使用技能 26 的職業） | [combat-continuation-current.zh-TW.md:21](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L21)、[:76](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L76)、[:77](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L77)；[combat-integration-lifecycle.zh-TW.md:285](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-integration-lifecycle.zh-TW.md#L285) | b | 修（先在 M1 重現並抓現場封包） | 登入器 `native/forced_combat.inc`（0x4C6B27 送出點）與客戶端選取物件 ID；伺服器側 `ForcedCombat`／`GameHub` 敵友判定僅在現場封包顯示錯誤時才動 |
| G03 | 技能 167 本應**單體**（Source area0／through0），卻會**打到旁邊沒選定的角色或 NPC**。候選 `4627ff77…` 同 SHA 626 案，非選定誤中降為 0。〔修正已驗證；正式版有此缺陷屬推測〕 | 玩家（競技場對戰） | [combat-continuation-current.zh-TW.md:36](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L36)；本機收據 `tools/combat-continuation/integration-lifecycle/build/wind-source-target-progress-current.json` | c | 修（先在正式版重現） | 正式 patch 鏈上加一層（以 0.6.71 為 base）：只改 167 的目標清單建構（`GameHub` 技能目標選取／807 列目標快照中的 167 列）；若依賴候選生命週期類別則屬 U2-2 |
| G04 | 技能 194 本應單體、射程 10，卻會**誤中非指定玩家**（正常與 Ctrl 施放皆然）。候選 `5945812d…` 同 SHA 1,434 案，誤中 8→0。〔修正已驗證；正式版有此缺陷屬推測〕 | 玩家 | [combat-continuation-current.zh-TW.md:46](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L46)；本機收據 `…/build/freeze-source-target-progress-current.json` | c | 修（先在正式版重現） | 同 G03：194 的目標建構（area0／through0／ranged10）；可與 G03 併成一個「單體技能誤中」任務 |
| G05 | 技能 76（範圍緩速類）以自己為主目標時：**多扣費 16 次、顯示錯 40 次、狀態錯 24 次、範圍誤中 24 次**。候選 `b2ebe000…` 同 SHA 3,814 案歸 0。〔修正已驗證；正式版有此缺陷屬推測〕 | 玩家 | [combat-continuation-current.zh-TW.md:50](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L50)；本機收據 `…/build/source-mass-slow-pipeline-progress-current.json` | c | 修（先在正式版重現） | 候選「只改 PC 與既有 helper」，可試著只移植 76 的主目標計算 helper；同 SHA 收據顯示原數值／扣費方法保留 |
| G06 | 技能 29／152／153（負面狀態類）的**施放准入差 72 案、狀態結果差 138 案**；153 遠距 36 案、自施 12 案行為與 Source 不同。候選同 SHA 3,636 案歸 0。〔修正已驗證；正式版有此缺陷屬推測〕 | 玩家 | [combat-continuation-current.zh-TW.md:20](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L20)；本機收據 `…/build/source-negative-progress-current.json`；先前規劃 [issue-231.md:25](issue-231.md#L25) | c | 修（先在正式版重現） | 29／152／153 的准入與狀態判定；同頁也含「PC 移動等待漏洞」屬候選自身結構，移植時只取准入／狀態規則 |
| G07 | 技能 17 範圍與 Source **差 66 案**、148／149 **目標語意差 32 案**（打到不該打的或漏打）。同 SHA 640 案歸 0。〔修正已驗證；收據頁未寫 SHA，需從收據取；正式版有此缺陷屬推測〕 | 玩家 | [combat-continuation-current.zh-TW.md:17](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L17)；本機收據 `…/build/source-area-weapon-progress-current.json` | c | 修（先在正式版重現） | 17／148／149 的範圍與目標建構 |
| G08 | 技能 103 在**遠距離仍誤生效**（玩家 16／NPC 4 次）；技能 40／103 **漏找目標 132 次**。候選 `17d49e19…` 同 SHA 1,128 案歸 0。〔修正已驗證；正式版有此缺陷屬推測〕 | 玩家 | [combat-continuation-current.zh-TW.md:38](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L38)；本機收據 `…/build/blind-scene-progress-current.json` | c | 修（先在正式版重現） | 40／103 的距離判定與目標搜尋；相消別名部分屬材料路徑，可留 Backlog |

## Backlog 候選

| 編號 | 現象（玩家看到什麼） | 影響誰 | 證據 | 來源 | 建議 | 最小修改的位置推測 |
|---|---|---|---|---|---|---|
| G09 | 技能 29／44／76 的視野射程（−1）與 Source 不同，可能在邊界距離放得出／放不出。候選 `26552f69…` 同 SHA 1,842 案。 | 玩家 | [combat-continuation-current.zh-TW.md:47](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L47) | c | **#356 已處理**：候選 helper 已在 `src/main`（`NativeSourceVisibilityRange`，29／44／76 共 13 個呼叫點）；另對照 1-伺服器 `types/Point.java:157-177` 補上第 19 格反向邊界（原本 (19,0)／(0,19)／(18,−1)／(−1,18) 四格會拒絕 Source 允許的施法），76 NPC 選取同步。測試 `SkillTimingRangeGapsTest.g09*`。實機邊界施放待驗證 | `NativeSourceVisibilityRange.inSourceScreen` |
| G10 | 技能 87 暈眩：**HP 還沒扣就先出現暈眩狀態／特效**（33 次→0）。候選 `4828aad9…` 同 SHA 2,489 案。 | 玩家 | [combat-continuation-current.zh-TW.md:42](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L42) | c | **#356 確認已含**：`NativeAcceptedShockMutation` 已在 `src/main` 並接上 BATTLE／WORLD 兩個 87 路徑；暈眩狀態／特效只在 HP 幀被接受、非致死、HP>0 後才發。測試 `SkillTimingRangeGapsTest.g10*`（退化路徑＋接線鎖）。實際時序（含鎖等待）需實機或整合測試，待驗證 | `NativeAcceptedShockMutation`、`NativeAcceptedHpCompletion` |
| G11 | 技能 50 冰凍：提早消魔 16／提早冰凍 13／舊封包 8；期限重載錯讀 8 次。候選 `f4d1e83a…` 同 SHA 1,074 案。 | 玩家 | [combat-continuation-current.zh-TW.md:44](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L44) | c | **#356 確認已含**：`NativeAcceptedFreezeMutation` 已在 `src/main`；消魔與冰凍延到 HP 幀被接受（非致死、傷害>0）後才套用，佇列拒收的舊事件會 abandon，期限重載改讀擷取的秒數。測試 `SkillTimingRangeGapsTest.g11*`。競態下的實際次數待整合測試／實機驗證 | `NativeAcceptedFreezeMutation` |
| G12 | 技能 80 範圍冰凍：過期扣 MP 4、動畫前先扣 HP 4 等。候選 `619ed21e…` 同 SHA 1,202 案。 | 玩家 | [combat-continuation-current.zh-TW.md:45](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L45) | c | **#356 確認已含**：`NativeAcceptedAreaFreezeMutation` 已在 `src/main`；施放前檢查生命週期為 OPEN 且施法者未移動（避免過期扣 MP），範圍內玩家的 HP 先暫存、等施法動畫輸出後才發布。測試 `SkillTimingRangeGapsTest.g12*`。競態下的實際次數待整合測試／實機驗證 | `NativeAcceptedAreaFreezeMutation` |
| G13 | 候選 `50635f7e…`：受控 NPC 施放 29／152／153 時，目標或觀察者中途退役，舊版會送錯觀察者封包或丟掉 NPC 自身 MP／HP／方向更新。同 SHA 300 次最終等待／2,760 斷言。 | 玩家（帶召喚物／寵物時） | [source-negative-final-frame.zh-TW.md:3-9](../../lito伺服器/LiTo381-Server/docs/maintenance/source-negative-final-frame.zh-TW.md#L3)；[combat-continuation-current.zh-TW.md:336](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L336) | c | Backlog：只在「施法中目標退役」的競態出現；修補建立在候選的首次身分／final frame 結構上，正式版沒有可對應的最小插點；同頁明列 58 材料、後端、MySQL、實機未完成（[:11](../../lito伺服器/LiTo381-Server/docs/maintenance/source-negative-final-frame.zh-TW.md#L11)） | 需整組合併（原 M2 同源化，已移 Backlog） |
| G14 | 候選 `3ccbb6f5…`：受控 NPC 76／167 範圍主目標與次目標在等待期間退役時，舊次目標仍被擲骰。驗證 manifest 記錄主目標 100 次等待／1,402 斷言、次目標 14 次等待／224 斷言。 | 玩家（帶召喚物時） | [combat-closeout-current.zh-TW.md:11](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-closeout-current.zh-TW.md#L11)；本機收據 `…/build/source-area-pair-first-verification-current.json` → `source-area-pair-first-validation/20261027T070141945928Z/manifest.json`（status `…-common-integration-open`） | c | Backlog：競態；收據狀態自稱 integration open | 同 G13 |
| G15 | 候選 `046c58c5…`：Source 效果身分對照、NPC 範圍／計次等收尾改動。 | — | [combat-closeout-current.zh-TW.md:3](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-closeout-current.zh-TW.md#L3)、[:76](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-closeout-current.zh-TW.md#L76)（「未執行單元／整合／回歸」） | c | Backlog：**沒有任何測試收據**（已實作、待測） | — |
| G16 | 候選 `323d708e…`：7 個技能採新 Ctrl 政策（合法自身／友方輔助不需 Ctrl、敵方玩家需明確意圖）。同 SHA 1,096 案。 | 玩家 | [combat-continuation-current.zh-TW.md:52](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L52) | c | **#356 確認已含**：`NativeSelectedPlayerIntent` 已在 `src/main`（33 個 object＋48 個 attack 解碼端點）；7 技能 1／4／26／42／50／76／167 的 target_to 都觸發此閘：自身或明確 Ctrl→放行，否則只有「不可攻擊」的對象（友方）放行。測試 `SkillTimingRangeGapsTest.g16*`。伺服器對 26 自施／友方不要求 Ctrl，G02 原因仍在客戶端，需現場封包，待驗證 | `NativeSelectedPlayerIntent` |
| G17 | WORLD 專屬修正（角色生命、共用 Ref、特化 Proc、隨從 MP 提交／轉移、反射 HP 等，`104716cd…`～`3386173a…`）。 | — | [combat-continuation-current.zh-TW.md:28-33](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L28) | c | Backlog：產品只有競技場（[:14](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L14)） | — |
| G18 | 受控 NPC 的材料帳本／成對生命週期（173／174 `3e853214…`、87 `7a982b61…`、BATTLE44 `246ef565…`、161 `c6611ff7…`／`f213c87f…`）。 | 玩家（帶召喚物時） | [combat-continuation-current.zh-TW.md:34-40](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L34)、[:48](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L48) | c | Backlog：多為內部帳本／鎖順序，玩家可見影響未證明，且依賴候選框架 | — |
| G19 | 正式 0.6.71 從未有「登入→大廳→進房→技能→戰鬥→結算→正常退出」逐步截圖與 `character_lito` 前後對照；使用者只口頭回報登入、建地圖、建戰鬥室正常。 | 全部 | [PLAN.md:20](../../PLAN.md#L20)；[CHECKLIST.md:46-67](../acceptance/CHECKLIST.md#L46)（全部「待執行」） | b | Backlog（待 #285） | — |
| G20 | 結算畫面的龍騎士／幻術師職業圖示：只有原生繪製測試，沒有人工回報（CHECKLIST 14）。 | 玩家 | [world-resource-compat-current.zh-TW.md:11](../../lito登入器/LiTo381-Launcher/docs/maintenance/world-resource-compat-current.zh-TW.md#L11) | b | Backlog（待 #285） | — |
| G21 | GM `.polyme` 遊戲內變身外觀未實測；GM 變身不帶 Source 外掛的變身能力（CHECKLIST 12／17）。 | GM | [release-0.6.71.json:162-163](../../lito登入器/LiTo381-Launcher/docs/maintenance/release-0.6.71.json#L162)；[gm-polyme-0.6.71.md:13](../../lito伺服器/LiTo381-Server/docs/maintenance/gm-polyme-0.6.71.md#L13) | b | Backlog（待 #285）；變身能力屬設計限制 | — |
| G22 | GM 魔杖（Storm_Walk）滑鼠點地傳送未人工驗收（CHECKLIST 18）。 | GM | [VALIDATION.md:11](../../lito伺服器/LiTo381-Server/VALIDATION.md#L11)；[gm-travel-0.6.68.md:36](../../lito伺服器/LiTo381-Server/docs/maintenance/gm-travel-0.6.68.md#L36) | b | Backlog（待 #285） | — |
| G23 | Ctrl 強制攻擊／光箭／負面技能：雙視窗人工操作未驗收（CHECKLIST 19）。 | 玩家 | [VALIDATION.md:10](../../lito伺服器/LiTo381-Server/VALIDATION.md#L10)；[forced-combat-0.6.67.md:24](../../lito伺服器/LiTo381-Server/docs/maintenance/forced-combat-0.6.67.md#L24) | b | Backlog（待 #285） | — |
| G24 | 戰鬥中斷線的結算：冪等與清理有測試，但雙玩家人工斷線未全驗收（CHECKLIST 16）。 | 玩家 | [VALIDATION.md:9](../../lito伺服器/LiTo381-Server/VALIDATION.md#L9) | b | Backlog（待 #285） | — |
| G25 | BUFF 圖示、魔法娃娃、藍色藥水、血之渴望的狀態圖示與到期顯示：沒有逐項人工回報。 | 玩家 | [buff-schema-and-character-pages-20261018.zh-TW.md:43](../../lito伺服器/LiTo381-Server/docs/maintenance/buff-schema-and-character-pages-20261018.zh-TW.md#L43)；[world-resource-compat-current.zh-TW.md:72-76](../../lito登入器/LiTo381-Launcher/docs/maintenance/world-resource-compat-current.zh-TW.md#L72) | b | Backlog（待 #285） | — |
| G26 | 重複登入（`S_LOGIN_CHECK/0x16`）訊息後回到空白帳號頁：實際畫面待驗收。 | 玩家 | [world-resource-compat-current.zh-TW.md:36](../../lito登入器/LiTo381-Launcher/docs/maintenance/world-resource-compat-current.zh-TW.md#L36) | b | Backlog（待 #285） | — |
| G27 | map 12000 資產缺口：#83 引述的前輪觀察（候選環境），正式版是否受影響未知。本報告**未**宣稱缺資源，也未執行 PakViewer 查核。 | 玩家（若該圖被選到） | [issue-83.md:7](issue-83.md#L7)、[issue-83.md:148](issue-83.md#L148)；[issue-284.md:110](issue-284.md#L110) | b | Backlog（待 #285；若 M1 碰到，須先用 PakViewer 查遍客戶端 pak 再定性） | — |
| G28 | 技能 131 傳送在競技場停用（現行規則）。 | 玩家 | [combat-continuation-current.zh-TW.md:22](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L22)、[:84](../../lito伺服器/LiTo381-Server/docs/maintenance/combat-continuation-current.zh-TW.md#L84) | b | Backlog：刻意的規則，不是缺陷；要開放需使用者決定 | — |

## 已核對、不列為缺口

| 項目 | 理由與證據 |
|---|---|
| 八角色選單翻頁、進大廳、返回角色選單 | 0.6.69 使用者實測通過，分頁 DLL 沿用至 0.6.71（[buff-schema-and-character-pages-20261018.zh-TW.md:39](../../lito伺服器/LiTo381-Server/docs/maintenance/buff-schema-and-character-pages-20261018.zh-TW.md#L39)）；M1 仍會重驗（CHECKLIST 05）。 |
| 大廳／戰鬥室職業圖示 | 0.6.70 使用者確認（[world-resource-compat-current.zh-TW.md:5](../../lito登入器/LiTo381-Launcher/docs/maintenance/world-resource-compat-current.zh-TW.md#L5)）。 |
| 相容層 `ddraw.dll`／`litoddraw.dll` 入口 | 0.6.71 發布收據 `no_generic_client_ddraw: true`（[release-0.6.71.json:133](../../lito登入器/LiTo381-Launcher/docs/maintenance/release-0.6.71.json#L133)）；10-21 時「正式舊入口仍用通用 ddraw.dll」的紀錄（[world-resource-compat-current.zh-TW.md:31](../../lito登入器/LiTo381-Launcher/docs/maintenance/world-resource-compat-current.zh-TW.md#L31)）已被 0.6.70 取代。實際載入仍由 CHECKLIST 06 驗。 |
| AC −888 角色載入中斷 | 正式版獨有 NativeArmorStatus 修正；反而要注意**候選帶舊 AC 限制**，移植候選修正時不得覆蓋（[issue-30.md:56](issue-30.md#L56)）。 |
| `8d26c9d7…` 以前的候選修正（相消機率、範圍睡眠緩速、NPC 替換等） | 已在正式核心內（[issue-30.md:22](issue-30.md#L22)）。註：[HISTORY.md:108](../../lito伺服器/LiTo381-Server/docs/HISTORY.md#L108) 寫「正式版未替換」為 2026-10-20 當時狀態，後被 0.6.70 配對候選取代（[world-resource-compat-current.zh-TW.md:50](../../lito登入器/LiTo381-Launcher/docs/maintenance/world-resource-compat-current.zh-TW.md#L50)）。 |

## 已驗證／推測／待驗證

**已驗證**（有文件或收據直接記錄）
- G01 啟動失敗的堆疊與原因表；G02 的使用者回報與「伺服器合成封包正常」；G03–G18 各候選 SHA 的同 SHA 收據數字（本表數字逐一取自引用行）。
- 正式核心 ≈ `8d26`（issue-30 類別比對）；`3ccbb6f5…` 驗證 manifest 的等待／斷言數（本機唯讀讀取）。

**推測**
- G03–G08：正式 0.6.71 也有同一缺陷。依據是缺陷屬「與 Source 語意不同」而非候選新結構，且正式核心早於修正 SHA；**尚未在正式 JAR 重現**。若 M1 或重現任務證明正式版沒有，該項降為 Backlog。
- G03–G08 的修正都已含在 `50635f7e…`：依 issue-30 的上層關係推論，各收據只證明各自 SHA。
- G02 原因在客戶端游標／送包。
- 各項「最小修改位置」：未讀候選差異碼，只依文件描述推估；任何一項若需要候選的生命週期框架，即屬 PLAN U2-2。

**待驗證**
- 來源 (a)：#285 跑表結果。
- G19–G27：都需要實機（交 Codex／dot 依 CHECKLIST 執行）。
- G01：補表後是否還有其他漏授權表、map 12000 是否影響正式 JAR。

## 下一步

1. #285 完成後併入來源 (a)，重新排序並封頂 M2 主線（PLAN U2-1：超過 8 項時第 9 項起交使用者決定）。
2. 主線每項開一張任務；G03–G08 的第一步是「在正式 JAR 重現（修前截圖）」，重現不到就降為 Backlog，不直接移植。
