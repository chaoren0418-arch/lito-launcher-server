# Issue #354 — T123：受控 NPC（寵物、召喚物）完整語意——候選差異盤點與移植

分支 `task/354`，PR #362。

## 結論

- 差異清單：`docs/design/controlled-npc.md`。
- 各項狀態：
  - **G13**（`50635f7e…` 29／152／153 最終送包）：`src/main` **已有**。
  - **G18**（173／174、87、BATTLE44、161 成對生命週期）：`src/main` **已有**。5 顆候選相對 base 新增的外層類別在 `50635f7e…` 與 `src/main` 全部存在。
  - **G14**（`3ccbb6f5…` 76／167 受控主次目標退役）：`src/main` 原本**沒有**，本 PR 已移植並附 13 項確定性測試。
  - **G15**（`046c58c5…` 29 個 `NativeCloseout*`）：**沒有**，未移植，清單交主控。
- `mvn -B test`：CI 綠燈（新測試 13/13）。

## 做了什麼

1. 用 SHA-256 核對三顆候選身分，逐 class 比對位元組（`scratch/t354/jardiff.py`）。
   - `3ccbb6f5…` 相對 `50635f7e…`：新增 3、改動 6 個 entry。
   - `046c58c5…` 相對 `50635f7e…`：新增 62、改動 36 個 entry。
2. G18 的 5 顆候選逐一查新增類別是否存在於 `50635f7e…`／`src/main`（`scratch/t354/ancestry.py`）：缺 0。
3. 移植 G14：新增 `NativeSourceAreaPairFirst.java`（取自 `tools/combat-continuation/integration-lifecycle/NativeSourceAreaPairFirst.java`，3ccbb6f5 版，只涵蓋受控 NPC）。插點與 `SourceAreaPairFirstMethods.java` 的 6 個 owner、10 個呼叫埠一一對應：
   - `NativeAreaMaterialAction$Context`：新增 transient 欄位 `nativeSourceAreaPairFirst`（`NativeAreaMaterialAction.java:60`）。
   - `nativeCompanionAreaCapturedRun`：`retry`／`scene` 兩個呼叫改走新 helper（`:397`、`:407`）。
   - `NativeSourceMassSlowTargets.Aim`：建構子結尾 `capture`（`:55`）、`close()` 開頭（`:59`）、`valid()` 開頭（`:66`）。
   - `NativeSourceWindTargets.Aim`：同上三處（`:13`、`:15`、`:16`）。
   - `NativeSourceAreaPlayerProof.player`：開頭先擋狀態已替換的次目標（`:5`）。
   - `ClientSession.nativeAreaBattleSlowBody`：76 的 PC 迴圈外包 `secondaryIterator`（`:61361`），位置在原 wake／機率／RNG 之前。
4. 測試 `src/test/java/local/lito381/server/NativeSourceAreaPairFirstTest.java`（13 項）：
   - fixture：`Unsafe.allocateInstance` 建記憶體 BATTLE 場景（施法者、寵物主人、旁觀 PC、主人的受控寵物），讓 `current()` 走完整真路徑。不開 socket／DB／執行緒池。
   - 競態：用 `RetiringLock`（覆寫 `tryLock()`，第一次呼叫時同步執行退役再回報忙碌），讓退役恰好落在原 roster 鎖的等待窗內。沒有 sleep，也沒有第二條執行緒。
   - 涵蓋 13 種主目標退役 × 2 技能：寵物死亡、移出場景、被替換、生命 token 換新、timedStatus 換新、主人死亡、重登、離房、施法者死亡、移動、狀態換新、伺服器 lifetime STOPPED、lifetime 換新。逐一斷言：
     - `current()` 由真轉假；
     - 原 `nativeCompanionAreaCapturedRun` 在第一次 retry 就離開；
     - 本體（扣費、RNG、送包）沒有執行；
     - 已取得的鎖全部釋放。
   - 次目標：76 等待期間狀態被替換、或快照後才加入的 PC 不擲骰；167 與未捕捉時維持原路徑。
   - 只有次目標退役時，不取消整個施法。
   - Aim 接線：`valid()` 先被首次身分擋下；`close()` 會釋放快照。
   - `capture()` 在 WORLD、自然 NPC、無目標時都不動作。

## 怎麼驗證

### 已驗證
- 本機 `mvn -B -q -DskipTests compile`、`test-compile` 成功（依本機規定不執行測試）。
- 三顆候選 SHA-256 與文件一致（`046C58C5…`、`3CCBB6F5…`、`50635F7E…`）。
- G18 祖先關係：5 顆候選新增的外層類別在 `50635f7e…` 缺 0、在 `src/main` 缺 0。
- CI（PR #362，run 43140469366，commit 第 1 版程式）：
  - `Server tests`（`mvn -B test`）**通過**，`NativeSourceAreaPairFirstTest` 13 項：Failures 0、Errors 0。
  - `Server (Java 17 / Maven)`、`Launcher`、`Build-Package`、`Semantic gap` 通過。
  - `Server tests (CI MySQL fixture, non-blocking)` 失敗（`DedicatedServerLayoutTest`、`OriginSourceConfigTest`、`ClientSession*IntegrationTest` 等）。這項在 main（run 43137284899）同樣失敗，屬既有問題，與本 PR 無關；本 PR 的 13 項在該 job 也全部通過。

### 推測
- 移植前的玩家可見症狀：等待主人鎖的期間寵物死亡／被收回、主人離房，舊流程仍會進入本體（扣 MP、擲骰、送 S_SPEED）；次目標重生後仍被擲骰。依據是候選收據（主目標 100 次等待／1,402 斷言、次目標 14 次等待／224 斷言，`build_source_area_pair_first.py:72-76`）與讀程式，本 PR 沒有重跑候選 harness。

### 待驗證
- `capture()` 的真實首次捕捉：需要 `CombatRules.loaded()` 的 BATTLE，也就是 DB 規則表。測試直接建構與 `capture()` 同形的 `First` 值，沒有走 `NativeSourceMassTeleportExit.capture`／`NativeSourceTargetNegativeLife.capture` 的真實資料。
- 實機：競技房帶召喚物／寵物，被對手以 76／167 指定，同時讓寵物死亡或收回，看施法是否放棄、不扣 MP。交 Codex／使用者。

## 未做（交主控，建議另開任務）

| 優先 | 項目 | 理由 |
|---|---|---|
| 1 | G15 受控 NPC 恢復：`NativeCloseoutControlledNpcData`、`…CompanionRecoveryData`、`…CompanionRecovery` | 玩家可見（寵物回復節奏、HP 條）。但候選無任何測試收據，且改動 `BattleCompanion`／`GameHub` |
| 2 | G15 受控 NPC 技能：`NativeCloseoutCompanionSkills`、`NativeCloseoutMobSkillSelection` | 玩家可見（召喚物放技能）。無收據 |
| 3 | G15 `NativeCloseoutCompanionMagicHeal`、`…CompanionEvolution` | 可見性小 |
| 4 | 046 版 `NativeSourceAreaPairFirst` 擴充（自然 NPC／玩家主目標）＋ `NativeCloseoutArea*` 4 類 | 非受控 NPC，內部守衛為主；無收據 |
| — | 其餘 18 個 `NativeCloseoutNpc*`／LOS／數值類 | 怪物 AI／全體數值，不屬受控 NPC，依規則只列清單 |

## 產出路徑
- `docs/design/controlled-npc.md`（差異清單）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeSourceAreaPairFirst.java`（新）
- 改動：`ClientSession.java`、`NativeAreaMaterialAction.java`、`NativeSourceAreaPlayerProof.java`、`NativeSourceMassSlowTargets.java`、`NativeSourceWindTargets.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NativeSourceAreaPairFirstTest.java`（新）
- 本機暫存（不進 git）：`scratch/t354/`（jardiff.py、ancestry.py、ancestry.json、jars/）

## 給下一棒的提示
- 要移植 G15，從 `tools/combat-continuation/integration-lifecycle/closeout-src/*.java` 取原始碼；插點看對應的 `build_closeout_*.py`／`Closeout*Methods.java`。重建順序見 `combat-closeout-current.zh-TW.md:19`。這些候選全部「No tests」，移植時要自己補收據。
- 本 PR 的測試 fixture（`NativeSourceAreaPairFirstTest.Scene`）可以重用在其他受控 NPC 首次身分測試：它已滿足 `NativeSourceSelfFinalFrame.current`、`NativeSourceTargetNegativeLife.player`、`NativeSourceNpcFirstLife.first`、`NativeSourceControlledDisplay.attached` 的全部條件。
- 語意比對（Semantic gap，非阻擋）會把 6 個改動類別列為對 base 的差異，這是本 PR 有意的移植。
