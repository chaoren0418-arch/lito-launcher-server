# 技能目標與範圍判定規格（T106，Issue #305）

- 權威來源：`1-伺服器`（唯讀）。下文 `S1/` 代表 `D:\lineage開發\1-伺服器\src\com\lineage\server\`；`L1SkillUse` 是 `S1/model/skill/L1SkillUse.java`。行號是 2026-11-11 的原始碼。
- 資料：`1-伺服器 db/380_db.sql` 的 `skills` 表（`CREATE TABLE` 在第 47987 行）。LiTo 在啟動時從正式 `380_db.skills` 讀同樣的欄位（`NativeSkillTargetMetadata.java:12`，由 `CombatDatabase.java:80` 在唯讀啟動交易中呼叫）。
- 181 個有入口技能的欄位，和本機收尾矩陣 `development-closeout-progress/20261028T222200402677Z/skills.tsv` 的 target／target_to／range／area／through／type 逐列比對：**0 筆不一致**（`tools/skill-targeting/entry_ids.py`）。
- 程式模型：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/SourceSkillTargeting.java`（純函式，不接任何執行路徑）；測試：`SkillTargetingServer1ParityTest.java`；輸入表：`src/test/resources/server1/skills-targeting-181.tsv`。

## 1. 欄位語意

| 欄位 | 值 | 意義 | Source 位置 |
|---|---|---|---|
| `target` | `attack`／`buff`／`none` | `attack`：攻擊型目標（決定 PK 旗標與「非 PK 不打 PC」規則）；`none`：不需要合法主目標，範圍技能也不把主目標本身加進清單 | `L1SkillUse:514-518,1347-1460,1718,1772` |
| `target_to` | 位元 | 0=只對自己（ME）、1=PC、2=NPC、4=血盟、8=隊伍、16=自己的寵物／召喚獸、32=座標（PLACE）；可相加 | `S1/templates/L1Skills.java:34-52` |
| `type` | 位元值 | 1=機率、2=變化（buff）、4=詛咒、8=死亡、16=治療、32=復活、64=攻擊、128=其他 | `L1Skills.java:17-31` |
| `ranged` | −1／0／N | 主目標距離上限。N：施法者到主目標的格線距離（Chebyshev，`getTileLineDistance`）必須 ≤ N；−1：改看主目標是否在施法者畫面內（`isInScreen`） | `L1SkillUse:1703-1716` |
| `area` | 0／−1／N | 0：單體；−1：施法者整個畫面（`getVisibleObjects(user)`）；N：以主目標為中心 N 格（`getVisibleObjects(target, N)`） | `L1SkillUse:1758,1785-1791` |
| `through` | 0／1 | 1＝非攻擊技能可以穿過障礙物；攻擊技能（type 含 64）一律要看視線 | `L1SkillUse:1462-1476` |

## 2. 目標清單怎麼建（`makeTargetList`，`L1SkillUse:1687-1817`）

依序判斷：

1. **座標技能**：58 火牢、63 治癒能量風暴在進到 `makeTargetList` 前就返回，改用座標施放（`:457-459`）。形狀記為 `PLACE`。
2. **只對自己**：`target_to == 0`、type 不含 64、技能不是 68 聖結界 → 清單只有自己，不檢查射程與視線（`:1695-1700`）。形狀 `SELF_ONLY`。
3. **射程**（非 Proc）：`ranged != -1` 時距離 > ranged 就整個不發動；`ranged == -1` 時主目標不在畫面內就不發動（`:1703-1716`）。
4. **主目標合法性**：`target != "none"` 且主目標不符合 `isTarget` → 不發動（`:1718-1729`）。
5. **直線列舉不可達**：`:1732-1755` 的 `switch (_calcType)` 用技能 ID `LIGHTNING`(17)、`FREEZING_BREATH`(194) 當 case，但 `_calcType` 只會是 1～4（`:207-210`），所以**這段永遠不會執行**。17 與 194 都直接往下走 area 規則：17 是 area 3 範圍、194 是 area 0 單體。
6. **單體**（area 0）：只放主目標。攻擊技能視線被擋時仍放主目標，但標記「不計算傷害」（`:1758-1768`）。
7. **範圍**（area ≠ 0）：
   - `target != "none"` → 先放主目標；
   - 技能不是 49 全體治癒、而且不是攻擊型（`target != "attack"` 且 `type != 64`）→ 再放施法者自己；
   - 範圍內每個角色通過 `isTarget` 就加入（`:1771-1811`）。area −1 時中心是施法者，area N 時中心是主目標，中心物件本身不在列舉結果內。

## 3. 誰算合法目標（`isTarget`，`L1SkillUse:1239-1623`）

施法者是 PC 時，與 LiTo 有關的規則（依程式順序）：

| # | 條件 | 結果 | 行 |
|---|---|---|---|
| a | 主目標是一般 NPC（非寵物、召喚獸），候選是 PC 或寵物／召喚獸 | 否 | 1325-1336 |
| b | type == 64 且候選是自己 | 否 | 1378-1381 |
| c | 技能 49 且候選是自己 | 否 | 1384-1387 |
| d | target_to 含 PC／血盟／隊伍，候選是自己 | **是** | 1389-1394 |
| e | 攻擊型、非 PK，候選是自己的寵物／召喚獸 | 否 | 1397-1415 |
| f | 攻擊型、非 PK、主目標是 PC，候選不是怪物 | 否（攻城戰同戰爭例外） | 1417-1460 |
| g | 視線被擋：非 Proc 時，攻擊型一律剔除；非攻擊型若 through=0 且 type 不是 2／32 也剔除 | 否 | 1462-1476 |
| h | 36 馴服、41 創造殭屍：候選不是怪物 | 否 | 1497-1500 |
| i | 41／61／75／165 需要死亡目標，其他技能不能對死亡目標 | 否 | 1501-1511 |
| j | target_to 含 PC 且候選是 PC；或含 NPC 且候選是 NPC（含寵物）；或含 PET 且候選是自己的寵物 | 是 | 1571-1604 |
| k | 主目標是 PC：target_to 含血盟且同盟；含隊伍且同隊 | **是** | 1606-1620 |

PK 旗標：主目標不是怪物、`target == "attack"`、主目標不是自己 → `_isPK = true`（`:514-518`）。所以 `target = "none"` 的攻擊型範圍技能（53、59、62、70、72、80、189）**永遠不是 PK**，依 f 規則在非攻城戰時不打 PC；`target = "attack"` 的範圍技能以 PC 為主目標時是 PK，會連同隊友一起打（j 規則不看隊伍）。

未建模（不影響 181 列的分類，但實作時要知道）：副本 ID、`CheckUtil.checkAttackSkill`、門／娃娃、鬼魂與 GM 隱身、安全區、攻城戰、冰矛／冰暴／大地束縛重複施放、守護塔、絕對屏障白名單（`:1519-1552`）、地下／飛行 NPC（`:1554-1569`）、NPC 施法者路徑。

## 4. 效果計算前的再過濾

- `isTargetCalc`（`:4049-4113`）：先用在主目標（`:2779`，不通過就在逐目標計算前返回：**MP、動作已在 `:1121-1140`／`:1196-1199` 照常處理，只是不對任何人生效**），再用在清單每個人（`:2833`）。
  - `target == "attack"`（18 除外）：施法者或目標在安全區（怪物、稻草人除外）、或 Non-PvP 設定 → 否。
  - 66 沉睡之霧：不含自己。76 集體緩速術：不含自己、不含自己的寵物／召喚獸。69 集體傳送術：只含自己。
- `isTargetFailure`（`:4151-4206`）：機率／詛咒型對守護塔、門必定失敗；18／29／39／76／152／153／167 對不可消魔的怪物必定失敗等（屬機率範圍，T102）。
- `isUseCounterMagic`（`:4243-4256`）：目標有反制魔法且技能不在例外表 → 消耗反制魔法並從清單移除。例外表是 `:334-344` 硬編碼加上 DB 外控表 `SkillCounterMagicExceptionTable`（`:2767-2775`）。`SourceSkillTargeting.counterMagicApplies` 只收硬編碼部分。

## 5. 測試情境與 181 列總表

測試模型（`SourceSkillTargeting.effects`）的情境：PC 施法、一般施放、全員存活、同副本、不在安全區、沒有攻城戰、不是 GM、沒有絕對屏障／隱藏狀態、視線無阻；主目標在射程內；候選種類 SELF（自己）、ENEMY（無隊無盟的 PC）、PARTY（同隊 PC）、CLAN（同盟 PC）、MONSTER、PET（自己的寵物）全都在範圍內與畫面內。每列記錄主目標為 SELF／ENEMY／PARTY／MONSTER／PET 時實際進入效果計算的種類。

預期值由 `tools/skill-targeting/golden.py` 依本文件**另外用 Python 實作**一次產生，Java 模型必須逐列相同，兩邊各自依原始碼實作，用來互相核對。

| 形狀 | 數量 | 技能 |
|---|---|---|
| SINGLE（area 0） | 88 | 1,4,6–11,15,16,18–20,23,26–29,33–48,50,55–57,61,64,67,68,71,75,77,79,87,103,108,112,113,132,133,145,148,149,151–153,157,158,160,165,167,170,173,174,183,187,188,192–194,202–204,206–209,211–214,216–219 |
| SELF_ONLY | 60 | 2,3,5,12,14,21,31,32,51,52,54,60,73,78,88–91,97–102,104–107,109–111,116,118,129–131,134,137,138,146,147,150,154,162,163,166,168,169,171,172,175,176,181,182,185,186,190,191,195,201 |
| AREA（area N） | 20 | 17,22,25,30,53,59,62,65,66,69,70,74,76,80,184,189,205,210,215,220 |
| SCREEN（area −1） | 11 | 13,49,72,114,115,117,155,156,159,161,164 |
| PLACE | 2 | 58,63 |

射程（不含 SELF_ONLY、PLACE）：−1（看畫面）37 列；0 有 23 列（多為 `target=none` 以自己為中心，及 103）；1：11,20,87,108,112,203；2：33,187；3：25 列；4：27,28,39；6：17,22,25,34,38,50；8：30,45,192；10：4,6,7,15,16,18,65,66,74,77,152,165,184,194。`through=1` 只有 161。

完整逐列內容（含每種主目標的結果）見 `src/test/resources/server1/skills-targeting-181.tsv`。

## 6. LiTo 對應的程式位置

路徑相對於 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`，行號為本 PR 版本。

| Source 規則 | LiTo 位置 | 狀態 |
|---|---|---|
| 讀 `skills` 目標欄位 | `NativeSkillTargetMetadata.java:12-17`；`CombatDatabase.java:80` | 一致（同欄位、同驗證範圍） |
| ranged −1 = 畫面 | `BattleSpellCatalog.isInNativeScreen`（`BattleSpellCatalog.java:3406`，曼哈頓 ≤18 或邊角 19）；`NativeSourceVisibilityRange.java:4,9` 把 −1 投影成 19／`Integer.MAX_VALUE` | 一致；畫面形狀與 S1 `isInScreen` 逐格一致**未驗證** |
| ranged N（Chebyshev） | 各 handler 的 `Math.max(|dx|,|dy|) > ranged`（例：`NativeBlindSceneAction.java:21`、`NativeSourceWindTargets.java:19`、`NativeSourceRemainingMaterials.java:11`） | 一致 |
| 目錄寫死的射程／範圍／target_to／type | `BattleSpellCatalog` 各 `*Spell` record；29／44／76 的 `maximumRange()` 由 `NativeSourceVisibilityRange.value` 投影，167 由 `NativeSkillTargetMetadata.windValue`（`:23`），194 由 `NativeSourceFreezeTarget.range` | 測試逐列比對有效值；103、133、148、149 目錄原值不同但執行期由 metadata 判定（見報告） |
| 單體／不可達的直線分支 | 194：`NativeSourceFreezeTarget.java:6`（有 metadata 時 `line()` 回傳 false）；17：`NativeSourceLightningArea.java:5-10`（area3 共 49 格） | 一致（G04、G07） |
| 視線（g） | `NativeSourceTargetLos.java:7`；`NativeBlindSceneAction.java:21`（`through && 非攻擊` 才略過） | 與 g 一致，但 LiTo 沒有 type 2／32 的豁免（只影響 buff／復活，181 列中 through=0 的 buff 走各自 handler，**未逐一驗證**） |
| 自施（d） | `NativeSourceNegativeSelf.java:4-10`（29／152／153）；`NativeSelectedPlayerIntent.java:8` | 一致（G06） |
| 76 排除自己與寵物；以自己／自己的寵物為主目標 | `NativeSourceMassSelfTarget.java:5,14-15`（`selfPrimaryEffects` 回傳空清單）、`NativeSourceAreaCasterTargets.java:25`、`NativeSourceSlowFailureMetadata.java:9`、`NativeSourceMassSlowTargets.java:78,326-350` | 一致（G05；自施部分 T114 #321 改成扣 MP、播動作、不生效） |
| 148／149 只對 PC、畫面距離 | `NativeSourceWeaponBuffTargets.java:9,19-21` | 一致（G07，僅 BATTLE） |
| 40／103 距離與目標 | `NativeBlindSceneAction.java:21`（`selected`） | 一致（G08）。103 對 1～3 格（玩家或寵物）：LiTo 准入、扣 MP、不生效、送失敗訊息；S1 射程外清單為空，`TYPE_NORMAL` 仍扣費並送失敗訊息（`:1121-1140,1196-1199,1829-1837`），兩邊相同（#321 S3 查證） |
| 範圍攻擊打誰（e、f、j、k） | 競技場：`GameHub.java:9695-9698`（`hub.canAttack`）→ `BattleRules.java:337-343` | **刻意不同**（使用者決定 #321 S1：維持 LiTo），規則見 `docs/design/gameplay.md` §2.9 |
| 反制魔法例外表 | `ClientSession` 欄位 `sourceCounterMagicBypassSkills`（DB 載入）；目錄 `counterMagicBypass` 欄 | 未在本任務比對（T107–T109） |
