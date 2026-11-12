# 受控 NPC（寵物、召喚物）語意——候選差異清單（T123／Issue #354）

範圍：差距矩陣戰鬥列「受控 NPC 語意未完成（LC B23）」，以及 `docs/reports/customer-gaps.md` 的 G13、G14、G15、G18。
基準：`src/main`（B-land #138 已併入候選 `50635f7e…` 的 B 範圍）。所有行號指本分支 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。

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
| **G15** Source 效果身分對照、NPC 範圍／計次等收尾 | `046c58c5…` | **沒有**（29 個 `NativeCloseout*` 在 `src/main` 皆不存在） | 見下表 | 部分（見下表） | 不移植，列清單 |
| **G18** 受控 NPC 材料帳本／成對生命週期 | `3e853214…`（173／174）、`7a982b61…`（87）、`246ef565…`（BATTLE44）、`c6611ff7…`／`f213c87f…`（161） | **已有**：5 顆候選相對 base 新增的外層類別（42／43／55／34／35 類）在 `50635f7e…` 與 `src/main` **全部存在**；部分 class 位元組不同，屬 `50635f7e…` 之前的祖先版本被後續候選再改寫 | `NativeControlledNegativeMaterial.java:5`（173／174）、`NativeShockCompanionAction.java:4`（87）、`NativeBattleControlledCancellation.java:4`（44）、`NativeCompanionForestArea.java:5`、`NativeCompanionForestCancellation.java:3`（161）、共用 `NativeCompanionNegative.java:4`、`NativeCompanionTargets.java:4` | 帳本為內部；成對生命週期已隨 50635f7e 進來 | 不需移植 |

### G15 的 29 個 `NativeCloseout*`（皆「沒有」、皆無測試收據）

| 群組 | 類別 | 用途（類別註解） | 與受控 NPC 關係／玩家可見 |
|---|---|---|---|
| 受控 NPC 資料與恢復 | `NativeCloseoutControlledNpcData`、`NativeCloseoutCompanionRecoveryData`、`NativeCloseoutCompanionRecovery` | 寵物／召喚物出生時凍結 NPC 樣板；Source 回血回魔節奏與主人畫面 HP 條 | 受控 NPC；**可見**（寵物回復速度、HP 條） |
| 受控 NPC 技能 | `NativeCloseoutCompanionSkills`、`NativeCloseoutMobSkillSelection` | 寵物／召喚物自身 mobskill 狀態與 Source `L1MobSkillUse` 選技 | 受控 NPC；**可見**（召喚物會放技能） |
| 受控 NPC 吸血／進化 | `NativeCloseoutCompanionMagicHeal`、`NativeCloseoutCompanionEvolution` | 受控 NPC 吸取類魔法的實際施法者；進化前的樣板證明 | 受控 NPC；可見性小（吸血回復對象、進化） |
| 範圍主目標擴充 | `NativeCloseoutAreaNatural`、`NativeCloseoutAreaOutput`、`NativeCloseoutAreaCommit`、`NativeCloseoutAreaPlayerCommit` | 76／167 首次身分延伸到自然 NPC／玩家主目標，獨立輸出與狀態監視器內檢查 | 非受控 NPC 主目標；內部守衛為主 |
| 自然 NPC 範圍魔法 | `NativeCloseoutNpcAreaAttempt`、`…AreaCast`、`…AreaCost`、`…AreaDoors`、`…AreaOutput`、`…AreaScene`、`…AreaTargets` | Source `areaMagicAttack`／`TYPE_GMBUFF` | 怪物 AI，非受控 NPC |
| 自然 NPC 數值 | `NativeCloseoutNpcAttribute`、`…NpcAuxCost`、`…NpcCounts`、`…NpcRecovery`、`…NpcReuse`、`…NpcSkillValues`、`…NpcSpellAdmission` | 怪物屬性、成本、計次、回復、冷卻、准入 | 怪物 AI，非受控 NPC |
| 地形／視線 | `NativeCloseoutBattleLineOfSight`、`NativeCloseoutSourceLineOfSight` | BATTLE 靜態 Source 地形 LOS | 全體 |
| 數值／效果 | `NativeCloseoutWorldMagicValues`、`NativeCloseoutSkillEffects` | WORLD 成本骰值、效果身分對照 | 全體；`combat-closeout-current.zh-TW.md:15` 寫明「消費端仍待實作」 |

## 判定與取捨

- **移植 G14**：唯一「沒有」且玩家帶召喚物／寵物時看得到影響（等待鎖期間寵物死亡／被收回／主人離房，舊版仍扣費擲骰並送包；次目標重生後仍被擲骰）、且有同 SHA 收據（主目標 100 次等待／1,402 斷言、次目標 14 次等待／224 斷言，`build_source_area_pair_first.py:72-76`）。
- **G13、G18 不移植**：已在 `src/main`。
- **G15 不移植**（交主控排序）：
  - 雖有受控 NPC 可見項目（恢復、mobskill），但候選本身「未執行單元／整合／回歸」，且改動 `BattleCompanion`、`GameHub`、`ClientSession` 等 23 個既有類別，量超過一張 PR。
  - 純內部守衛（Area*Commit、帳本）依任務規則只列清單。
