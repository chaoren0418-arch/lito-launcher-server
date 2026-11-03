# 戰鬥開發收尾進度（2026-10-29）

目標已由 create_goal 建立，目前 active。**開發仍進行中；本次已整合項目為「已實作、待測」；不是產品驗收或發布。**

## 候選與沿用成果

目前候選：`046c58c5a3609404a8a673e16d0e98366207850f43de2523e0c032f0fc69e76f`。

[隔離 JAR](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-skill-effects/20261028T220433235913Z/candidate-skill-effects-unqualified.jar) · [建置與依賴 SHA](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-skill-effects/20261028T220433235913Z/manifest.json) · [結構化交接](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-closeout-current.json)

沿用共同 50635… 的29／152／153初次身分、最終輸出與清理，以及73自身與69退出成果；核對較新3ccbb…的76／167主目標與76 PC次目標修補。新純編譯入口重建3ccbb…相同SHA；既有測試收據只適用各自SHA，未重跑、未跨版本當通過。歷史 current／共同進度不覆寫。

## 實際改動

- 補齊Source效果身分對照：53個模式登記與807技能逐列解析已凍結；7個效果參照中4個有模式、3個走無模式狀態路徑。效果參照不是最終寫入狀態：49014→CONFUSION202的PC模式寫SILENCE64並送1339訊息，NPC模式無效果；49017→CURSE_PARALYZE33使用外控準備／麻痺時間及兩階段狀態。裝備proc自訂秒數大於0時直接寫原技能ID，不使用effect_skill_id；0秒才走原技能TYPE_GMBUFF。這是來源語意與盤點工具更新，候選JAR未變，消費端仍待實作。

[完整改動與證據紀錄](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-closeout-progress/20261028T222200402677Z/manifest.json)；以下矩陣保留全部12項範圍，未完成項目不因本輪修補而縮減。

重建順序（只編譯／打包及靜態檢查）：`build_development_closeout.py` → `build_closeout_release_compat.py` → `build_closeout_area_primary.py` → `build_closeout_area_output.py` → `build_closeout_area_commit.py` → `build_closeout_area_player_commit.py` → `build_closeout_npc_skill_values.py` → `build_closeout_npc_reuse.py` → `build_closeout_npc_attribute.py` → `build_closeout_npc_aux_cost.py` → `build_closeout_npc_recovery.py` → `build_closeout_companion_recovery_data.py` → `build_closeout_companion_recovery.py` → `build_closeout_world_magic_values.py` → `build_closeout_companion_evolution.py` → `build_closeout_controlled_npc_data.py` → `build_closeout_companion_skills.py` → `build_closeout_companion_magic_heal.py` → `build_closeout_npc_spell_admission.py` → `build_closeout_npc_area_cost.py` → `build_closeout_npc_area_scene.py` → `build_closeout_battle_line_of_sight.py` → `build_closeout_npc_area_targets.py` → `build_closeout_npc_area_doors.py` → `build_closeout_npc_area_cast.py` → `build_closeout_npc_area_output.py` → `build_closeout_npc_area_attempt.py` → `build_closeout_npc_counts.py` → `build_closeout_skill_effects.py` → `audit_closeout_controls.py` → `audit_closeout_skills.py` → `audit_closeout_npc_numeric.py` → `checkpoint_development_closeout.py`；均位於 tools/combat-continuation/integration-lifecycle。不要以舊 build_source_area_pair_first.py 代替，該入口包含執行期測試。

## 逐項狀態

| 項目 | 狀態 | 已接與剩餘範圍 |
| --- | --- | --- |
| 首次身分與生命期 | 部分已接／待補 | 29／152／153、76／167等已接首次身分及輸出保護；其餘生產／接收路徑仍缺 |
| 材料與施法准入 | 部分已接／待補 | 58材料列持續核對；完整成本、回滾、關係、範圍與各目標種類仍缺 |
| Proc分派 | 部分已接／待補 | 部分接受後效果與自訂時長已接；一般SkillUse及娃娃／魔法石／NPC等仍缺 |
| HP與完成事件 | 部分已接／待補 | 部分首次狀態、生命期、完成輸出已接；其餘生產端及重入／延後路徑仍缺 |
| 數值、狀態與LOS | 部分已接／待補 | NPC成本／骰值／屬性、Source靜態LOS與範圍門已接；完整公式、狀態及其他門消費端仍缺 |
| NPC工廠與AI | 部分已接／待補 | 出生資料／回復、空目標自施及部分計次／AI結束已接；其他成功計次、非空自施、完整選列及受控施法仍缺 |
| 封包與顯示 | 部分已接／待補 | 部分獨立目標輸出與加密邊界已接；其餘圖像、OwnCharStatus及實際畫面仍缺 |
| 外控與資料讀取 | 盤點完成／接線未完 | 252外控已盤點；可選效果參照已讀取，消費端及141列適用性／28列來源仍待審查 |
| 正式0.6.71相容 | 已整合／待測 | 隔離整合4類別／11方法；正式資源SHA保留，功能與畫面待驗收 |
| 最終同SHA後端驗證 | 另行安排 | 依指示待程式完成後安排；包含Main、STOPPED、生命期與記憶體矩陣 |
| 真實中文MySQL | 另行安排 | 依指示另行安排唯讀角色／裝備／加成重載驗證 |
| 客戶端與游標 | 另行安排 | 依指示另行安排；技能26正常自施、Ctrl、自友敵及多視窗仍待實證 |

[NPC範圍列分類](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-npc-numeric-review/20261028T222050257720Z/npc-area-routes.tsv)：離線SQL 33列，其中11列為已接的空目標自施分支；不是實際DB驗收。

[效果參照逐列矩陣](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-npc-numeric-review/20261028T222050257720Z/skill-effect-references.tsv)：離線7列、7個效果ID；讀取已接，執行消費端未完成。
[Source模式與逐技能身分](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-npc-numeric-review/20261028T222050257720Z/skill-mode-resolution.tsv)：53個模式登記、807列；7個參照為4個模式及3個無模式狀態。模式鍵不等於實際狀態ID；正自訂秒數proc保留原技能ID，0秒才走TYPE_GMBUFF。僅來源契約與盤點，執行接線仍缺。

[逐技能矩陣](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-closeout-progress/20261028T222200402677Z/skills.tsv)：807列，其中58材料列；181玩家技能均有目前入口與靜態分派路徑，38占位列及588非玩家資料列繼續消費端審查。仍需逐項審查效果，不以入口結果代替。

[逐外控矩陣](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/development-closeout-progress/20261028T222200402677Z/controls.tsv)：252盤點列，52列已有精確字面定位，另31列動態鍵已定位，141列有Source引用仍待適用性審查，28列未找到合格Source引用仍待審查；不等同252缺失。既有31項技能預設處理沿用，與31個動態鍵分開計數。

## 下一步與未知資料

- 依已凍結53項模式登記與逐列身分契約接TYPE_GMBUFF及一般施法：4個別名模式與3個無模式狀態分別實作；模式本體可能寫不同狀態或用外控時間。proc須先分治癒／正自訂秒數原ID／0秒原技能GMBUFF；不得把effect_skill_id套到正自訂秒數分支。成本／時長／圖像／目標仍取原技能資料
- 自然NPC出生triggerCount、召喚上限、空目標成功計次與無可見PC歸零已接；續接其他實際施法成功計次、完整選列、其他AI結束原因、非空自施與視覺／地面分支
- 把mobskill選取核心與已接入的實際actor狀態連至可見目標、成本／效果及攻擊tick；BATTLE回吸完成端已接實際受控actor，仍須接施法入口與WORLD回吸，並補其餘noTarget結束分支，補裝備SP消費端；NPC目標適用性與De實際actor／數值綁定、其他NPC回復及AI／RNG語意
- 181玩家技能按實際模型逐項核對效果、正常／Ctrl及自友敵關係，588非玩家資料追查NPC／特殊消費端
- NPC areaMagicAttack兩個傷害入口已接可見目標與靜態LOS；續補視覺／地面入口、明確GM隱身／ghost轉接與Source空間遍歷順序、其他LOS消費端的動態門與地圖專屬箭線覆蓋、各玩家自施TYPE_GMBUFF與成功後排程規則（成功依handleCommands回傳，不以造成傷害替代；地圖93拒絕已接入）及一般區域沉默適用性；其餘生產端、狀態、NPC、娃娃、地面／立方體與外控消費端收尾
- 252外控中尚待適用性審查的消費端與資料讀取接線
- 真角色裝備／INT／成長與加成重載需日後唯讀 DB 驗證；此階段不連 DB。
- 原生游標與Ctrl送包實證仍缺，通暢氣脈術自施尚不能宣告修復。
- 252屬性列尚須逐列判定別名、消費端與適用性，不能當成252缺失。

## 後續待測

- 效果身分分流：原模式優先、單次別名、mode鍵與實際狀態不同；49014的PC模式SILENCE64／訊息1339及NPC模式無效果；49017的CURSE_READY_TIME／CURSE_TIME、雙階段狀態與凍結排除；proc治癒優先、正自訂秒數原ID不轉別名、0秒原技能GMBUFF；既有效果不刷新與原技能圖像／時長
- 效果參照讀取：新／舊schema、缺欄／NULL／0／負值、自指／循環／不存在mode、原模式優先、原skills.id不變、資料重載快照一致、失敗不得部分發布；別名不得重取被參照技能成本／時長／圖像
- NPC次數：出生上限與重載隔離、0／負值不限次數、上限拒絕仍保留原機率抽取、召喚不重複計次、空目標零成功不計次／成功僅一次；無敵方但有同圖20格PC不得歸零，已死／觀戰／隱身PC、不同force地圖、睡眠／定身／動作尚未到期、重啟／重生與停止拒絕；歸零保留reuse
- 空目標同步施法：全失敗保持魔法冷卻不變並走同次普攻，成功僅一次sleep／reuse；首次玩家／NPC退役、停止／重啟、門變更、主次目標、metadata重載、重送與巢狀／異常出口；後續入口不得重施，結果須於輸出前清除
- 空目標自施NPC動作：零成功不得發送；成功採原ActId或19、10格同地圖名冊、觀戰／已死接收者與普通隱身策略、首次NPC移動／移除／重生／換工廠及接收者換房／換force／生命期於八條加密前拒絕；結果清理、重送及異常還原
- TYPE_GMBUFF空目標：TYPE_ATTACK自施且target非none應成功但零傷害／零RNG／零玩家效果；失效首次參與者拒絕；target=none、非攻擊型、光亮／火牆／生命之泉與四立方體不得誤入；成功結果與HP接受互不代替
- NPC範圍動態門：關門中心／兩側格、正45度與非45度、目標格是門、開門還原原tile、兩房同arena隔離、門在射線期間開關／重設、不同Quest force地圖、停止／重啟房間及既有靜態LOS退路
- NPC範圍入口：方向／小半徑外但可見玩家、主目標距離不代替可見名單、一般隱身、不同實際地圖、無首次證明、玩家移動／換身／離房及NPC移動／移除／重生／遭遇重設於RNG前拒絕；原一般分支保留
- Source LOS直線／1:3與1:5斜率／正45度單側開放／兩側封閉／中間格箭線位元／目標tile3、地圖複製與快照重載、動態門拒絕及所有原共用LOS消費端
- 最終同SHA後端／Main／STOPPED／記憶體與生命週期矩陣（含受控10／28回吸接受／拒絕／致死／重送與換主／進化／延遲輸出、寵物mobskill狀態隔離／AI重啟／冷卻保留／舊租約拒絕、回復、進化模板更新、主死亡、換主／移除、輸出延遲與關閉）
- 全技能合法與拒絕、成本／RNG／回滾／重送與主次目標；一般NPC選中沉默／HP不足／MP恰足與不足時的解除、備援、零扣款／零冷卻及areaMagicAttack零NPC成本／保留NPC沉默、無可見成功目標／地圖93／LOS拒絕與成功後排程分流
- 真角色唯讀載入／加成重載
- 原生游標、正常／Ctrl自施友方敵方、前景多視窗與大廳
- 0.6.71選服、七職業圖示、BUFF、GM7200與結算清理

本次未執行單元／整合／回歸／Main／客戶端或UI測試，未連DB、未寫Source／玩家資料／schema／WAL，未更動正式JAR／DLL／啟動雜湊／捷徑、未部署或建立新基準。
