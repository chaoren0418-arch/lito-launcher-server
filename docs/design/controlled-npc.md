# 受控 NPC（寵物、召喚物）語意——候選差異清單（T123／Issue #354）與 G15 前兩組移植（T135／Issue #386）

範圍：差距矩陣戰鬥列「受控 NPC 語意未完成（LC B23）」，以及 `docs/reports/customer-gaps.md` 的 G13、G14、G15、G18。
基準：`src/main`（B-land #138 已併入候選 `50635f7e…` 的 B 範圍）。所有行號指本分支 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。

G15 的前兩組（恢復、mobskill 選列與狀態）已在 T135（#386）移植，設計與逐項對照見文末「[G15 前兩組移植設計](#g15-前兩組移植設計t135issue-386)」。本檔前半段的盤點內容維持 #354 當時的寫法，只更新 G15 的狀態欄。

## 盤點方法（已驗證）

1. 三顆候選 JAR 以 SHA-256 核對身分（`scratch/t354/jars/`，只讀複製）：
   - `50635f7e…`＝`reference/candidate-50635f7e.jar`
   - `3ccbb6f5…`＝本機 `tools/combat-continuation/integration-lifecycle/build/source-area-pair-first-probes/20261027T065515298731Z/candidate-area-pair-first-unqualified.jar`
   - `046c58c5…`＝本機 `…/build/development-skill-effects/20261028T220433235913Z/candidate-skill-effects-unqualified.jar`
2. 逐 entry 比對 class 位元組雜湊（`scratch/t354/jardiff.py`）。
3. G18 的 5 顆候選（`3e853214…`、`7a982b61…`、`246ef565…`、`c6611ff7…`、`f213c87f…`，路徑取自各自 `*-progress-current.json` 的 `runtime.path`）：列出相對 `base-0.6.71.jar` 新增的外層類別，再查 `50635f7e…` 與 `src/main` 是否存在（`scratch/t354/ancestry.py`）。
4. 候選原始碼對照：`tools/combat-continuation/integration-lifecycle/NativeSourceAreaPairFirst.java`、`SourceAreaPairFirstMethods.java`（3ccbb6f5 的 bytecode 拼接埠）、`closeout-src/*.java`（046c58c5）。

## 候選相對 `50635f7e…` 的差異

### `3ccbb6f5…`（新增 3、改動 6 個 class entry）

| entry | 種類 | 內容（`SourceAreaPairFirstMethods.java` 的埠） |
|---|---|---|
| `NativeSourceAreaPairFirst`、`$First`、`$Cursor` | 新增 | 76／167 受控主目標首次身分＋次目標 PC 狀態快照 |
| `NativeAreaMaterialAction$Context` | 改動 | 新增 transient 欄位 `nativeSourceAreaPairFirst` |
| `NativeSourceMassSlowTargets$Aim`、`NativeSourceWindTargets$Aim` | 改動 | `<init>` 結尾 `capture`、`valid()` 開頭 `current`、`close()` 開頭 `close` |
| `NativeAreaMaterialAction` | 改動 | `nativeCompanionAreaCapturedRun` 的 `retry`／`scene` 兩個呼叫改走 `NativeSourceAreaPairFirst` |
| `NativeSourceAreaPlayerProof` | 改動 | `player()` 開頭先擋「狀態已被替換的次目標」 |
| `ClientSession` | 改動 | `nativeAreaBattleSlowBody` 的 PC 迴圈 iterator 外包 `secondaryIterator` |

### `046c58c5…`（新增 62、改動 36 個 class entry；外層新增 33 類）

- 包含 `3ccbb6f5…` 的全部內容，但 `NativeSourceAreaPairFirst` 換成擴充版（`closeout-src/NativeSourceAreaPairFirst.java`）：首次身分也涵蓋 BATTLE 自然 NPC（kind 1／2）與玩家主目標。
- 新增 29 個 `NativeCloseout*` 類別（見下方 G15），另 3 個外層類別 `GmPolyMe`、`SelectionRecovery`、`SharedPresentationPackets` 在 `src/main` 已存在（0.6.71 相容層）。
- 改動 23 個既有外層類別，含 `BattleCompanion`、`ClassicWorldMonsterPopulation`、`ClientSession`、`GameHub`、`Protocol`、`CombatDatabase`、`NativeSourceNpcSpellTiming` 等（`scratch/t354/jars/c50635f7.jar.diff.json`）。
- 收據：`combat-closeout-current.zh-TW.md:3`「已實作、待測」、`:76`「本次未執行單元／整合／回歸」。

## 逐項狀態

| 項目 | 候選 | `src/main` 狀態 | 類別與行號 | 玩家可見 | 本 PR |
|---|---|---|---|---|---|
| **G13** 受控 NPC 施放 29／152／153 時目標或觀察者中途退役（最終送包不可變首次值） | `50635f7e…` | **已有**（B-land #138 併入；r4 語意比對 B 範圍 1,384 entry 除 `ClientSession` 外全 SAME，見 `docs/reports/issue-138.md`） | `NativeSourceNegativeFinalFrame.java:4`（57 行）、`NativeSourceNegativePairFirst.java:3`、`NativeSourceControlledNegativeFinal.java:4`、`NativeSourceTargetNegativeLife.java:3`、入口 `NativeSourceNegativeTargets.java:10`（呼叫點 `:61`） | 是 | 不需移植 |
| **G14** 受控 NPC 76／167 範圍主目標與次目標在等待期間退役 | `3ccbb6f5…` | 移植前**沒有**（`NativeSourceAreaPairFirst` 不存在）→ 本 PR 補齊 | 新增 `NativeSourceAreaPairFirst.java:19`；插點 `NativeAreaMaterialAction.java:60`（欄位）、`:397`（retry）、`:407`（scene）；`NativeSourceMassSlowTargets.java:55`（capture）、`:59`（close）、`:66`（valid）；`NativeSourceWindTargets.java:13`、`:15`、`:16`；`NativeSourceAreaPlayerProof.java:5`；`ClientSession.java:61361` | 是（帶召喚物／寵物被 76／167 指定時） | **已移植**＋測試 |
| G14 延伸：046 版自然 NPC／玩家主目標 | `046c58c5…` | 沒有 | `closeout-src/NativeSourceAreaPairFirst.java` | 非受控 NPC | 不在本任務範圍；無測試收據，列清單 |
| **G15** Source 效果身分對照、NPC 範圍／計次等收尾 | `046c58c5…` | #354 當時**沒有**（29 個 `NativeCloseout*` 皆不存在）；T135（#386）移植其中 5 個，其餘 24 個仍沒有 | 見下表 | 部分（見下表） | #354 不移植；T135 移植前兩組 |
| **G18** 受控 NPC 材料帳本／成對生命週期 | `3e853214…`（173／174）、`7a982b61…`（87）、`246ef565…`（BATTLE44）、`c6611ff7…`／`f213c87f…`（161） | **已有**：5 顆候選相對 base 新增的外層類別（42／43／55／34／35 類）在 `50635f7e…` 與 `src/main` **全部存在**；部分 class 位元組不同，屬 `50635f7e…` 之前的祖先版本被後續候選再改寫 | `NativeControlledNegativeMaterial.java:5`（173／174）、`NativeShockCompanionAction.java:4`（87）、`NativeBattleControlledCancellation.java:4`（44）、`NativeCompanionForestArea.java:5`、`NativeCompanionForestCancellation.java:3`（161）、共用 `NativeCompanionNegative.java:4`、`NativeCompanionTargets.java:4` | 帳本為內部；成對生命週期已隨 50635f7e 進來 | 不需移植 |

### G15 的 29 個 `NativeCloseout*`（候選皆無測試收據；狀態欄為 T135／#386 之後）

| 群組 | 類別 | 用途（類別註解） | 與受控 NPC 關係／玩家可見 | `src/main` 狀態 |
|---|---|---|---|---|
| 受控 NPC 資料與恢復 | `NativeCloseoutControlledNpcData`、`NativeCloseoutCompanionRecoveryData`、`NativeCloseoutCompanionRecovery` | 寵物／召喚物出生時凍結 NPC 樣板；Source 回血回魔節奏與主人畫面 HP 條 | 受控 NPC；**可見**（寵物回復速度、HP 條） | **已移植**（T135）＋測試。3 個類別都在；`…RecoveryData` 改為只放節奏資料與預設值（原本的查表併進 `…ControlledNpcData`，與候選最終版相同） |
| 受控 NPC 技能 | `NativeCloseoutCompanionSkills`、`NativeCloseoutMobSkillSelection` | 寵物／召喚物自身 mobskill 狀態與 Source `L1MobSkillUse` 選技 | 受控 NPC；**可見**（召喚物會放技能） | **已移植選列與狀態**（T135）＋測試。**玩家仍看不到**：候選沒有「受控 NPC 攻擊時選列並施放」的入口與效果轉接，`src/main` 也沒有（見文末「還缺什麼」） |
| 受控 NPC 吸血／進化 | `NativeCloseoutCompanionMagicHeal`、`NativeCloseoutCompanionEvolution` | 受控 NPC 吸取類魔法的實際施法者；進化前的樣板證明 | 受控 NPC；可見性小（吸血回復對象、進化） | 沒有 |
| 範圍主目標擴充 | `NativeCloseoutAreaNatural`、`NativeCloseoutAreaOutput`、`NativeCloseoutAreaCommit`、`NativeCloseoutAreaPlayerCommit` | 76／167 首次身分延伸到自然 NPC／玩家主目標，獨立輸出與狀態監視器內檢查 | 非受控 NPC 主目標；內部守衛為主 | 沒有 |
| 自然 NPC 範圍魔法 | `NativeCloseoutNpcAreaAttempt`、`…AreaCast`、`…AreaCost`、`…AreaDoors`、`…AreaOutput`、`…AreaScene`、`…AreaTargets` | Source `areaMagicAttack`／`TYPE_GMBUFF` | 怪物 AI，非受控 NPC | 沒有 |
| 自然 NPC 數值 | `NativeCloseoutNpcAttribute`、`…NpcAuxCost`、`…NpcCounts`、`…NpcRecovery`、`…NpcReuse`、`…NpcSkillValues`、`…NpcSpellAdmission` | 怪物屬性、成本、計次、回復、冷卻、准入 | 怪物 AI，非受控 NPC | 沒有（`NativeCloseoutNpcRecovery` 的 `Data`／`bounded` 兩個小元件改放在 `…CompanionRecoveryData`／`…CompanionRecovery`，見文末） |
| 地形／視線 | `NativeCloseoutBattleLineOfSight`、`NativeCloseoutSourceLineOfSight` | BATTLE 靜態 Source 地形 LOS | 全體 | 沒有 |
| 數值／效果 | `NativeCloseoutWorldMagicValues`、`NativeCloseoutSkillEffects` | WORLD 成本骰值、效果身分對照 | 全體；`combat-closeout-current.zh-TW.md:15` 寫明「消費端仍待實作」 | 沒有 |

## 判定與取捨

- **移植 G14**：唯一「沒有」且玩家帶召喚物／寵物時看得到影響（等待鎖期間寵物死亡／被收回／主人離房，舊版仍扣費擲骰並送包；次目標重生後仍被擲骰）、且有同 SHA 收據（主目標 100 次等待／1,402 斷言、次目標 14 次等待／224 斷言，`build_source_area_pair_first.py:72-76`）。
- **G13、G18 不移植**：已在 `src/main`。
- **G15 在 #354 不移植**（交主控排序；之後由 #376 開成 T135，前兩組已移植，見文末）：
  - 雖有受控 NPC 可見項目（恢復、mobskill），但候選本身「未執行單元／整合／回歸」，且改動 `BattleCompanion`、`GameHub`、`ClientSession` 等 23 個既有類別，量超過一張 PR。
  - 純內部守衛（Area*Commit、帳本）依任務規則只列清單。

## G15 前兩組移植設計（T135／Issue #386）

範圍：G15 表的「受控 NPC 資料與恢復」「受控 NPC 技能」兩組共 5 個類別。其餘 24 個不動。

路徑縮寫：
- `C/` = `lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/closeout-src/`（候選 `046c58c5…` 的原始碼）；`M/` = 同層的 `Closeout*Methods.java`（候選對既有類別的 bytecode 插點）。
- `R/` = `D:\lineage開發\1-伺服器\src\com\lineage\server\`（對照基準，唯讀）。
- `S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`（本 PR 之後的行號）。

規則：候選與 `1-伺服器` 不一致時以 `1-伺服器` 為準。逐項對照後，**數值與判定沒有找到不一致**；不一致只出現在「候選沒做的部分」（列在「還缺什麼」）。

### 第 1 組：回血回魔與主人 HP 條

`1-伺服器` 的實際回復類別：`L1PetInstance`／`L1SummonInstance` 本身沒有回復計時器，出生時由父類別 `L1NpcInstance` 打開旗標（`R/model/Instance/L1NpcInstance.java:602-603`），實際加血加魔的是 `R/timecontroller/pet/` 的四個每秒時鐘（`PetHprTimer`、`PetMprTimer`、`SummonHprTimer`、`SummonMprTimer`，由 `R/timecontroller/StartTimer_Pet.java:83-92` 啟動），它們各自走訪全部寵物／召喚物並呼叫 `HprPet.hpUpdate`／`MprPet.mpUpdate`。

| # | 項目 | 候選做法 | `1-伺服器` 做法 | 一致 | `src/main` |
|---|---|---|---|---|---|
| 1 | 時鐘 | 每個 Hub 一個時鐘，`scheduleAtFixedRate(clock, 0, 1 秒)`（`C/NativeCloseoutCompanionRecovery.java:50-51`） | 四個時鐘，起始延遲 0、週期 1000 ms（`R/timecontroller/StartTimer_Pet.java:83-92`） | 是（四個計數器同時從 0 起跳，數值永遠相同，合成一個不影響結果） | `S/NativeCloseoutCompanionRecovery.java:93-113`；掛在既有的 `battleClock`，不新增執行緒 |
| 2 | 計數 | 先 `++seconds` 再用（`C/…Recovery.java:34`），第一次執行是 tick 1 | 先 `_time++` 再用（`R/timecontroller/pet/PetHprTimer.java:92`、`PetMprTimer.java:92`） | 是 | `S/…Recovery.java:80` |
| 3 | HP 間隔 | 樣板 `hprinterval`，≤0 時 20 秒（`C/NativeCloseoutControlledNpcData.java:33`） | 同（`R/timecontroller/pet/HprPet.java:44-47`） | 是 | `S/NativeCloseoutCompanionRecoveryData.java:24-30` |
| 4 | HP 回復量 | 樣板 `hpr`，≤0 時 1 點（`C/…ControlledNpcData.java:34`） | 同（`R/…/HprPet.java:50-53`） | 是 | 同上 |
| 5 | MP 間隔 | 樣板 `mprinterval`，≤0 時 20 秒（`C/…ControlledNpcData.java:34`） | 同（`R/timecontroller/pet/MprPet.java:48-51`） | 是 | 同上 |
| 6 | MP 回復量 | 樣板 `mpr`，≤0 時 1 點（`C/…ControlledNpcData.java:35`） | 同（`R/…/MprPet.java:54-57`） | 是 | 同上 |
| 6a | 間隔的單位 | 直接用樣板 getter 的值當秒數（`C/…ControlledNpcData.java:33-34`） | `npc` 表存毫秒，樣板 setter 除以 1000 變成秒（`R/templates/L1Npc.java:472-474`、`:492-494`）；不足 1000 的值變 0，落到預設 20 秒 | 是：LiTo 的樣板 setter 同樣除以 1000（`S/NativeDeNpcTemplate.java:395-397`、`:411-413`） | 不需改動 |
| 7 | 觸發 | `tick % 間隔 == 0`（`C/…Recovery.java:146,149`） | `(time % interval) == 0`（`R/…/HprPet.java:49`、`MprPet.java:53`） | 是 | `S/…Recovery.java:280,284` |
| 8 | 不回 HP 的條件 | 不在場、HP ≤ 0（`C/…Recovery.java:89`）、最大 HP ≤ 0（`:144`）、HP 已滿（`:146`） | 最大 HP ≤ 0、HP ≤ 0、已死、已銷毀、HP 已滿（`R/…/HprPet.java:24-42`） | 是 | `S/…Recovery.java:161-162,278,280` |
| 9 | 不回 MP 的條件 | 同上，另加最大 MP ≤ 0、MP 已滿（`C/…Recovery.java:149`） | 最大 HP ≤ 0、最大 MP ≤ 0、已死、已銷毀、HP ≤ 0、MP 已滿（`R/…/MprPet.java:24-46`） | 是 | `S/…Recovery.java:278,284` |
| 10 | 上限 | 加完不超過最大值（`C/NativeCloseoutNpcRecovery.java:101` 的 `bounded`） | `adjustCurrentHp`／`adjustCurrentMp`（`R/…/HprPet.java:73`、`MprPet.java:76`） | 是 | `S/…Recovery.java:315-317` |
| 11 | 回復開關 | 沒有對應欄位 | `isHpRegenerationX()`＝`_hprRunning` 且不是「馴服捕捉進行中」（`R/model/Instance/L1NpcInstance.java:1869-1871`、`:1940-1941`） | 不影響結果：`_hprRunning` 出生即開（`:602-603`），只在「馴服捕捉失敗清理」關掉（`:3498-3499`）；LiTo 沒有「捕捉進行中」這個狀態，死亡已由第 8 項排除 | 不需要 |
| 12 | HP 條對象 | 只送主人（`C/…Recovery.java:174` 的 `value.owner()`） | 只送主人（`R/model/Instance/L1PetInstance.java:1081,1093`、`L1SummonInstance.java:1463-1465`） | 是 | `S/…Recovery.java:355-359` |
| 13 | HP 條時機 | HP 有變才送（`C/…Recovery.java:151`）；只回 MP 不送 | `mutation.changed()` 才送（`R/…/L1PetInstance.java:1756-1762`、`L1SummonInstance.java:1458-1461`）；MP 沒有對主人的封包 | 是 | `S/…Recovery.java:286-303` |
| 14 | HP 條數值 | `100 × hp ÷ 最大 HP`，整數除法，夾在 0–100（`C/…Recovery.java:162`） | `100 * getCurrentHp() / maxHp`（`R/…/L1PetInstance.java:1093`；召喚物分母用 `Math.max(1, maxHp)`，`L1SummonInstance.java:1464`） | 是（最大 HP > 0 時相同） | `S/…Recovery.java:320-322` |
| 15 | HP 條封包 | `Protocol.hitRatio(id, ratio, 100)`：opcode、object id（4 位元組）、比例（1 位元組） | `S_HPMeter`：`writeC(opcode)`、`writeD(objId)`、`writeC(hpRatio)`（`R/serverpackets/S_HPMeter.java:39-43`） | 是 | `S/…Recovery.java:340-342`（`Protocol.java:16006`） |
| 16 | 哪些受控 NPC | 工廠戳記 kind 5（L1Pet）或 6（L1Summon）且樣板編號與目前外形一致（`C/…Recovery.java:89-91`） | `WorldPet` 的全部寵物、`WorldSummons` 的全部召喚物（`R/…/PetHprTimer.java:94`、`SummonHprTimer.java:94`） | 是；LiTo 把馴服怪（`Kind.TAMED`）也算 L1Summon（`S/NativeSourceNpcFactoryType.java:221-224`，既有對應） | `S/…Recovery.java:158-167` |

實際資料的樣子（`1-伺服器` `db/380_db.sql` 的 `npc` 表，欄位順序 `hprinterval, hpr, mprinterval, mpr`）：牧羊犬 45034 是 `5000, 5, 0, 0`（`:39107`）→ 每 5 秒回 5 HP、每 20 秒回 1 MP；召喚物狂暴蜥蜴人 81222 是 `5000, 8, 5000, 5`（`:38102`）→ 每 5 秒回 8 HP 與 5 MP。LiTo 實驗環境的靜態庫是否同值沒有查（不連 DB）。

LiTo 專屬、`1-伺服器` 沒有對應概念的部分（照候選保留）：

- 只有「主人在進行中的戰鬥房（已開戰、沒有斷線結算待決、`RUNNING`）」或「主人已登記在經典世界」時才回復（`S/…Recovery.java:128-134,140-150`）。`1-伺服器` 沒有房間，寵物只要存在就回復。
- 回復前後各比對一次身分（主人、房間、寵物生命、外形、工廠戳記、所在場景），防止「等鎖期間寵物死亡／被收回／換主人」之後還加血或送 HP 條（`S/…Recovery.java:204-239,324-338`）。最終送包再由既有的 `NativeSourceNpcMagicEffects.allowFrame` 把關（`S/NativeSourceNpcMagicEffects.java:35` → `S/…Recovery.java:374-385`）。

### 第 2 組：mobskill 選列與使用狀態

`1-伺服器` 的對應：每個 `L1NpcInstance`（含寵物、召喚物）出生時建立一個 `L1MobSkillUse`（`R/model/Instance/L1NpcInstance.java:2148`），攻擊時由 `attack()` 呼叫（`:889-926`）。

| # | 項目 | 候選做法 | `1-伺服器` 做法 | 一致 | `src/main` |
|---|---|---|---|---|---|
| 1 | 資料來源 | 出生時把樣板的全部 mobskill 列凍結成 `Row`（`C/NativeCloseoutControlledNpcData.java:25-32`） | `MobSkillTable.getTemplate(npcId)`（`R/model/L1MobSkillUse.java:60-61`） | 是 | `S/NativeCloseoutControlledNpcData.java:66-109` |
| 2 | 表是否啟用 | 第 0 列是 `TYPE_NONE` 就整張不用（`C/NativeCloseoutMobSkillSelection.java:38,48`） | 同（`R/…/L1MobSkillUse.java:152-156,189-193`） | 是 | `S/NativeCloseoutMobSkillSelection.java:95,109` |
| 3 | 掃描終點 | 遇到 `TYPE_NONE` 停（`C/…Selection.java:41,52`） | 迴圈條件 `getType(i) != TYPE_NONE`（`R/…:159-160,203-204`） | 是 | `S/…Selection.java:99,114` |
| 4 | 冷卻中的列 | 先看冷卻，冷卻中直接跳過，不擲骰（`C/…Selection.java:78`） | `isSkillDelay` 最先判斷（`R/…:684-686`） | 是 | `S/…Selection.java:153` |
| 5 | 機率（TriRnd） | `nextInt(100)+1 < TriRnd` 才可用（`C/…Selection.java:81-82`） | 同（`R/…:692-697`）。嚴格小於：TriRnd=30 實際是 29%，TriRnd=1 永遠不發動 | 是 | `S/…Selection.java:154-160` |
| 6 | 何時擲 TriRnd | `triggerRandom` 為真，或類型是召喚／變身（`C/…Selection.java:79`） | `isTriRnd || TYPE_SUMMON || TYPE_POLY`（`R/…:688-690`） | 是 | `S/…Selection.java:154` |
| 7 | 自身 HP 條件 | `hp×100÷maxHp ≤ TriHp`（`C/…Selection.java:85-88`） | 同（`R/…:701-708`） | 是 | `S/…Selection.java:161-165` |
| 8 | 同族 HP 條件 | 視野內同 family 的 NPC 中 HP 比例最低、且低於 100% 者；`≤ TriCompanionHp` 時可用並把目標換成牠（`C/…Selection.java:89-94,106-113`） | 同（`R/…:710-724,749-769`） | 是 | `S/…Selection.java:166-174,193-206` |
| 9 | 距離條件 | 負值＝距離 ≤ 絕對值；正值＝距離 ≥ 該值；0＝不看（`C/…Selection.java:95-97,103-105`） | 同（`R/…:726-736`、`R/templates/L1MobSkill.java:265-273`） | 是 | `S/…Selection.java:175-178,187-190` |
| 10 | 次數條件 | 已用次數 < `TriCount` 才可用（`C/…Selection.java:98-100`） | 同（`R/…:738-745`） | 是 | `S/…Selection.java:179-182` |
| 11 | 任一條件不符 | 立刻判不可用；全部沒設定也不可用（`C/…Selection.java:77,101`） | `useble` 初值 `false`（`R/…:681,746`） | 是 | `S/…Selection.java:152,183` |
| 12 | 換目標：自己 | `CHANGE_TARGET_ME` → 施放者（`C/…Selection.java:115`） | 同（`R/…:781-783`） | 是 | `S/…Selection.java:211` |
| 13 | 換目標：隨機 | 視野內的玩家／寵物／召喚物，依序排除距離不符、視線不通、不在仇恨表、已死、玩家幽靈；索引 `nextInt(數量×100)÷100`；沒有候選就維持原目標（`C/…Selection.java:116-126`） | 同（`R/…:785-835`） | 是 | `S/…Selection.java:212-225` |
| 14 | 換目標：其他值 | 維持上一列留下的目標（`C/…Selection.java:116`） | `default: target = _target`（`R/…:837-839`） | 是 | `S/…Selection.java:212` |
| 15 | 多列可用時 | 從可用列均勻抽一列：`nextInt(可用列數)`（`C/…Selection.java:57`） | `_rnd.nextInt(skillSizeCounter)`（`R/…:224-226`） | 是 | `S/…Selection.java:119-121` |
| 16 | 抽中列的目標 | 用「掃描結束時留下的目標」，不是該列當時的目標（`C/…Selection.java:56-57`） | `_target` 是欄位，掃完才抽列（`R/…:186-226`）；這是 `1-伺服器` 的既有行為，照搬 | 是 | `S/…Selection.java:118-121` |
| 17 | 類型分派 | 1 物理、2 魔法、3 召喚、4 變身、5／6／7 群體衝暈／相消／壞物；其他值不施放（`C/…Selection.java:63-72`） | 同（`R/…:237-289`） | 是 | `S/…Selection.java:131-141` |
| 18 | 成功後記帳 | 效果成功才次數 +1 並起算冷卻（`C/…Selection.java:73`） | `skillUseCountUp` 與 `L1SkillDelayforMob.onSkillUse` 都在成功後（`R/…:240-292`） | 是 | `S/…Selection.java:142-145` |
| 19 | 冷卻長度 | `ReuseDelay` 毫秒；≤0 不起算；冷卻中不延長（`C/NativeCloseoutCompanionSkills.java:42-48`） | 同（`R/model/skill/L1SkillDelayforMob.java:16-22`；到期由計時任務清旗標，`:46-48`） | 是（候選改記「到期時刻」，不另開計時任務；到期點相同） | `S/NativeCloseoutCompanionSkills.java:62-82` |
| 20 | AI 結束 | 次數全部歸零，冷卻不動（`C/…CompanionSkills.java:65-70`） | `finishAI` → `resetAllSkillUseCount`，不碰 `_skillDelay`（`R/model/Instance/NpcAI.java:252-260`、`R/model/L1MobSkillUse.java:120-128`） | 是 | `S/…CompanionSkills.java:106-112` |
| 21 | 什麼算 AI 結束 | 死亡／不在場（`C/…CompanionSkills.java:71-74`）；姿態「休息」且沒有目標（`:80-81`） | `AIProcess()` 回真或最大 HP ≤ 0（`R/…/NpcAI.java:234-245`）；寵物／召喚物「休息」（狀態 3）時 `noTarget()` 回真（`R/…/L1PetInstance.java:119-122`、`L1SummonInstance.java:249-252`） | **部分**：候選自述其餘 noTarget 結束分支未接（`M/../CloseoutCompanionSkillsAudit.java:59`）。已接的兩種與 `1-伺服器` 相同 | `S/…CompanionSkills.java:114-129` |
| 22 | 狀態歸屬 | 每隻受控 NPC 一份，掛在 `BattleCompanion` 上，沒有全域登記表（`C/…CompanionSkills.java:51-56`） | 每個 `L1NpcInstance` 一個 `_mobSkill`（`R/…/L1NpcInstance.java:487,2148`） | 是 | `S/BattleCompanion.java:574,594` |

候選多出來、`1-伺服器` 沒有對應的保護（照候選保留）：`Lease`（`S/…CompanionSkills.java:36-82`）。取用次數與冷卻前要先拿租約，租約在寵物死亡、被收回、AI 結束、換了生命或工廠戳記之後失效，失效後任何讀寫都丟例外。用途是「選列之後、施放之前寵物死了」時不會記到帳。

### 還缺什麼（候選沒有，本 PR 也沒有做）

1. **受控 NPC 實際施放 mobskill 的入口。** `1-伺服器` 在 `L1NpcInstance.attack()`（`R/model/Instance/L1NpcInstance.java:889-926`）：到攻擊距離時先 `isSkillTrigger`，成立後一半機率普攻、一半 `skillUse(target, true)`；不在攻擊距離但視線通時直接 `skillUse(target, true)`。候選沒有任何地方呼叫 `NativeCloseoutMobSkillSelection.trigger`／`use`（`combat-closeout-current.zh-TW.md` 下一步第 3 點：「把 mobskill 選取核心與已接入的實際 actor 狀態連至可見目標、成本／效果及攻擊 tick」）。所以**移植後召喚物仍不會放自己的技能**。
2. **七種效果的轉接**（`Ports.physical`／`magic`／`summon`／`poly`／`shockStun`／`cancellation`／`weaponBreak`）。`1-伺服器` 在 `R/model/L1MobSkillUse.java:304-675`。候選只有介面。
3. **施放後的硬直時間**（`_sleepTime`，`R/…/L1MobSkillUse.java:321,519-522,673`）。`Data` 已帶 `attackMagicMillis`／`supportMagicMillis`，沒有消費端。
4. **進化後的樣板更新。** 候選在 `evolve` 也重裝一次狀態（`M/CloseoutCompanionSkillsMethods.java:16-18`），前提是 `NativeCloseoutCompanionEvolution` 先把工廠戳記換成新樣板。`src/main` 的 `nativeSourceNpcFactoryType` 是 `final`（`S/BattleCompanion.java:572`），進化不換戳記；進化後戳記的樣板編號與外形不同，第 1 組會跳過這隻寵物（`S/…Recovery.java:165`），**進化後到重新召喚前不回血回魔**。`1-伺服器` 進化後照新樣板回復。屬 #354 優先 3（`NativeCloseoutCompanionEvolution`），不在本任務範圍。
5. **其餘 AI 結束分支**（第 2 組第 21 項）。

### 插點（既有類別的改動，只取這兩組需要的）

| 既有類別 | 候選的插點 | `src/main` 的改動 | 差異 |
|---|---|---|---|
| `NativeSourceNpcFactoryType.Stamp` | 新欄位 `nativeCloseoutNpcRecovery`（`M/CloseoutNpcRecoveryMethods.java:12`，屬自然 NPC 那一組）與 `nativeCloseoutControlledNpcData`（`M/CloseoutControlledNpcDataMethods.java:12`），各加一個建構子 | 兩個欄位＋一個建構子（`S/NativeSourceNpcFactoryType.java:19-20,35-38,140-152`） | 只加受控 NPC 用的建構子；自然 NPC 的 `row()` 不動 |
| `NativeSourceNpcFactoryType.controlled` | 兩個分支各呼叫一次 `NativeCloseoutControlledNpcData.capture`（`M/CloseoutControlledNpcDataMethods.java:18-24`） | 同（`S/…FactoryType.java:216-226`） | 無 |
| `GameHub` | 欄位 `nativeCloseoutCompanionRecoveryClock`；建構子在 `NativeCloseoutNpcRecovery.init` 之後呼叫 `init`；`close()` 開頭呼叫 `close`（`M/CloseoutCompanionRecoveryMethods.java:20-24`） | 欄位（`S/GameHub.java:1041`）；`init` 接在 `NativePlayerLifetime.openHub` 之後（`:1499`）；`close`（`:13032`） | `NativeCloseoutNpcRecovery` 不在範圍，改接在它原本的位置（`openHub` 之後，`M/CloseoutNpcRecoveryMethods.java:25`） |
| `BattleCompanion` | `profile` 加 `volatile`（`M/CloseoutCompanionRecoveryMethods.java:10-13`）；欄位 `nativeCloseoutMobSkillState`；工廠戳記寫入後 `install`（建構子＋`evolve`，共 2 處）；HP／active 寫入後 `terminal`（4 處）（`M/CloseoutCompanionSkillsMethods.java:11-24`） | `profile`（`S/BattleCompanion.java:499`）；欄位（`:574`）；`install` 1 處（`:594`）；`terminal` 4 處（`deactivate` `:978`、`applyDamage` `:1671`／`:1689`、`apply("dismiss")` `:1729`） | `install` 少 `evolve` 那一處（見「還缺什麼」4） |
| `NativeCompanionPlacement.State` | `pending` 加 `volatile`（`M/CloseoutCompanionRecoveryMethods.java:10-13`） | 同（`S/NativeCompanionPlacement.java:14`） | 無 |
| `NativeSourceNpcMagicEffects.allowFrame` | 回傳前再過 `NativeCloseoutCompanionRecovery.allowFrame`（`M/CloseoutCompanionRecoveryMethods.java:15-18`） | 同（`S/NativeSourceNpcMagicEffects.java:35`） | 無 |
| `ClientSession` | `serverCompanionTick(BattleCompanion,…)` 與 `serverClassicWorldCompanionTick(BattleCompanion,…)` 開頭呼叫 `tick`（`M/CloseoutCompanionSkillsMethods.java:25-31`） | 同（`S/ClientSession.java:51950`、`:52316`） | 無 |

類別本身與候選的差異：

- `NativeCloseoutCompanionRecoveryData`：候選版是 `capture(npcId)` 查表（`C/NativeCloseoutCompanionRecoveryData.java:5-16`），但候選最終的 JAR 已把這個呼叫換成 `NativeCloseoutControlledNpcData.capture`（`M/CloseoutControlledNpcDataMethods.java:19-21`），原方法沒有人呼叫。`src/main` 版改成放節奏資料 `Data` 與預設值 `of(樣板)`，由 `…ControlledNpcData.capture` 使用，避免留一個沒人用的方法。
- `Data` 與 `bounded`：候選放在 `NativeCloseoutNpcRecovery`（自然 NPC 回復，不在範圍）。`src/main` 分別放在 `…CompanionRecoveryData.Data` 與 `…CompanionRecovery.bounded`。日後移植 `NativeCloseoutNpcRecovery` 時可以直接共用。
- `NativeCloseoutCompanionRecovery`：多一個 `packet(Value)`，把組封包從 `send` 抽出來讓測試能比對位元組；流程不變。
- `NativeCloseoutCompanionSkills.terminal`：開頭多一行「沒有狀態就直接返回」，預設競技場同伴（沒有戳記）受傷時少兩次反射讀取；結果不變。
- `NativeCloseoutControlledNpcData`：多一個 `capture(snapshot, npcId)` 多載，供測試直接給樣板快照；`capture(npcId)` 轉呼叫它。

### 測試（`src/test/java/local/lito381/server/`）

| 測試類別 | 涵蓋 |
|---|---|
| `NativeCloseoutCompanionRecoveryTest` | 樣板節奏與預設值；HP／MP 各自的間隔與回復量；不超過上限、滿了不再送；寵物／召喚物／馴服怪；主人 HP 條的封包位元組與「只給主人」；死亡、收回、移出名單後不回復；房間與主人的准入條件；等主人鎖期間退役（12 種）；HP 條算好後才退役（8 種）；時鐘計數、停止、解除安裝 |
| `NativeCloseoutMobSkillSelectionTest` | TriRnd 的 100 種骰值；召喚／變身一定擲骰；HP、距離、次數條件；冷卻跳過與成功才記帳；均勻抽列；`TYPE_NONE` 終止；七種類型分派；同族 HP 換目標；換目標自己／隨機的過濾；最終掃描目標 |
| `NativeCloseoutCompanionSkillsTest` | 出生時每隻一份狀態與完整列資料；沒有樣板或不是寵物／召喚物時沒有狀態；租約需要的鎖；冷卻的起算、到期、不延長、≤0 不起算、長時間不溢位；休息且無目標時次數歸零而冷卻保留；死亡／收回／到期後租約失效且拿不到新租約；選列接上真實狀態後的冷卻與次數上限 |

fixture：重用 `NativeSourceAreaPairFirstTest.Scene`（Hub、lifetime、主人），另加一個進行中的房間與時鐘；寵物／召喚物／馴服怪用正式工廠方法（`BattleCompanion.pet`／`summoned`／`tamed`）建立。不開 socket／DB／執行緒池，沒有 sleep；時間以明確的 tick 編號或奈秒值傳入。