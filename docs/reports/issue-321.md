# Issue #321 報告：T114 技能目標判定 3 項依使用者決定處理

- 分支：`task/321`（code，第 1 次嘗試）
- 決定來源：Issue #321 留言。使用者沒有回覆，調度器採逾時預設：**S1 維持 LiTo；S2、S3 對齊 1-伺服器**。主控補充：S2 的 1-伺服器 行為是「扣 MP、播施法動作、不生效」（依 #307 查證）。
- 參考 `D:\lineage開發\1-伺服器\src\com\lineage\server\model\skill\L1SkillUse.java`（唯讀，行號為 2026-11-11 版本）。

## 做了什麼

| # | 項目 | 決定 | 結果 |
|---|---|---|---|
| S1 | 競技場範圍攻擊 | 維持 LiTo | 程式不改。`docs/design/gameplay.md` 新增 **§2.9 範圍攻擊**（誰會中、`target=none`／`target=attack` 與 1-伺服器 的差異、Ctrl 強制攻擊只放寬被選目標）與 §6 **N9**（刻意不做：1-伺服器 範圍 PK 規則） |
| S2 | G05 技能 76 以自己／自己的寵物為主目標 | 對齊 1-伺服器 | **改程式**：扣 MP、播動作，但不對任何人生效（原本仍會緩速旁人） |
| S3 | 技能 103 對 1～3 格的寵物 | 對齊 1-伺服器 | **查證後 LiTo 現況已與 1-伺服器 相同，程式不改**（Issue 前提「1-伺服器 射程外直接不發動」不成立，見下） |

### S2 程式變更
`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`：
- `NativeSourceMassSelfTarget.java`：新增 `selfPrimaryEffects(List)`（固定回傳空迭代器）；`iterator()` 在「自施／自己寵物為主目標」（`enabled()`）時改用它；刪掉只供舊行為使用的 `ordered()`（原本把自己排第一、保留旁人）。
- `NativeSourceAreaCasterTargets.slowIterator`（BATTLE 玩家清單）、`NativeSourceSlowFailureMetadata.iterator`（BATTLE quest／time-attack NPC 清單）：同一條件下也改用 `selfPrimaryEffects`。WORLD 相位走 `NativeSourceMassSlowTargets.calculationIterator` → `NativeSourceMassSelfTarget.iterator`，同樣生效。
- 扣費與動作不受影響：`ClientSession.nativeAreaBattleSlowBody` 在 `:61247` 扣 MP、`:61258-61262` 送 S_ACTION，之後 `:61267` 才跑目標迭代器；WORLD `nativeAreaWorldSlowBody` 是 `:60813` 扣 MP、`:60826` S_ACTION、`:60837` 迭代器。
- 1-伺服器 依據：`handleCommands` 的 `TYPE_NORMAL`（`:1121-1140`）先 `commitMaterialAndAcquireCastLocks`、`sendCastMotion`，再 `runSkill`；`runSkill:2779` 的 `isTargetCalc(_target)` 對 MASS_SLOW 排除自己與自己的寵物／召喚獸（`:4086-4103`），不通過就返回；回到 `applySideEffectsAfterNormalCast`（`:1196-1199`）照常 `useConsume` 扣 MP。

### S3 查證（為什麼不改）
- 1-伺服器：103 是 `target=buff`、`type=4`、`ranged=0`。距離 > 0 時 `makeTargetList`（`:1703-1714`）直接 `return`，清單為空，但 `checkUseSkill` 仍回傳 true（`:528-542` 不看清單，只有 NPC 施法者才看）。`TYPE_NORMAL` 的條件 `!_isGlanceCheckFail || …`（`_isGlanceCheckFail` 只在 `isTarget` 視線檢查設定，這裡沒跑到）成立 → 扣材料、送動作、`runSkill`（清單空，不生效）→ `useConsume` 扣 MP → `sendFailMessageHandle`（`:1829-1837`：非攻擊型、target 不是 none、清單空）送失敗訊息。**也就是 1-伺服器 同樣「扣 MP、播動作、不生效、送失敗訊息」**。
- LiTo：寵物分支 `NativeBlindSceneAction.companion` 用目錄射程 3 准入 → 扣 MP、送動作 → `selected()` 依 metadata `ranged=0` 判定不中 → 送失敗訊息。玩家目標走 `ClientSession:71081` 同樣用目錄射程 3 准入、`chance()` 歸 0。兩邊在 1～3 格的表現相同。
- 剩下的差異（本 PR 未改，列給使用者）：**4 格以上、畫面內**的目標，1-伺服器 仍會扣 MP（ranged=0 的射程外一律走上述流程），LiTo 直接拒絕、不扣 MP。若要「完全對齊」反而要讓 LiTo 在更遠距離也扣 MP，和使用者原本「扣了 MP 卻沒效果屬於缺陷」的意圖相反，所以沒有做，留待使用者決定。

### 測試
- 新增 `src/test/java/local/lito381/server/SkillTargetingDecisionsTest.java`（5 個測試）：
  - `s2SelfPrimaryEffectListIsEmpty`：`selfPrimaryEffects` 對非空清單也回傳空、null 丟 NPE。
  - `s2SourceModelHasNoEffectForSelfOrOwnPetPrimary`：規格模型 `SourceSkillTargeting.effects(76, …)` 主目標自己／寵物為空、敵方有效。
  - `s2AllSelfPrimaryBranchesUseEmptyEffects`：3 個分支都改用 `selfPrimaryEffects`，`ordered(` 已不存在。
  - `s2CostAndMotionPrecedeTargetCalculation`：BATTLE／WORLD 本體中「扣 MP → S_ACTION → 目標迭代器」的順序（確保清單為空時仍扣 MP、播動作）。
  - `s3DarkBlindPetDistanceStaysAlignedWithSource`：103 `ranged=0` 時 1～3 格不在射程、目錄射程 3、寵物分支「准入 → 扣 MP → 命中判定 → 失敗訊息」的順序。
- `SkillTargetingServer1ParityTest.g05…` 只改註解（原文「整個技能不發動」不正確）。

### 文件
- `docs/design/gameplay.md`：§2.9、N9。
- `docs/design/skill-targeting.md`：§4 更正 `:2779` 的語意（不是整個不發動，是扣費後不生效）；§6 的 76、40／103、範圍攻擊三列更新為本次決定與出處。

## 怎麼驗證

### 已驗證
- 本機 `mvn -B -q -DskipTests test-compile`（只編譯 main 與 test，不執行）：exit 0。
- 1-伺服器 原始碼逐行閱讀（上列行號），以及 LiTo `ClientSession` 扣費／動作／迭代器的順序（行號如上）。
- **本機沒有執行任何測試**（依規定）；`mvn -B test` 交給 PR 的 GitHub Actions CI，結果見 PR。

### 推測
- 76 自施時 LiTo 仍會送 `S_EFFECT Mass Slow primary=`（`visualCount` 對 76 固定回 1）。1-伺服器 `sendGrfx(true)` 也在 `runSkill` 返回後照常送施法圖示；未逐行比對 `sendGrfx` 的範圍技能分支，判斷兩邊都有圖示。
- 76 自施時 LiTo 不送 `Mass-Slow-failure` 訊息（因為 `visualCount>0`）；1-伺服器 `sendFailMessageHandle` 在清單非空時也不送。兩者應相同，未實機確認。

### 待驗證（交給 Codex／實機）
- 76 以自己為主目標（BATTLE 與 WORLD）：MP 有扣、播施法動作、旁邊的敵人／怪物**不會**被緩速。以自己的寵物為主目標同樣。
- 76 以敵人為主目標：行為不變（敵人與範圍內合法目標被緩速）。
- 103 對 1～3 格的寵物／玩家：扣 MP、播動作、出現失敗訊息（行為不變）。
- 死鬥中龍捲風、暴風雪打得到敵方、打不到隊友（S1 規則，行為不變）。

## 未完成／需要使用者決定
- **S3 前提更正**：1-伺服器 在 103 射程外也扣 MP（不是「直接不發動」）。1～3 格 LiTo 已一致；4 格以上（畫面內）1-伺服器 扣 MP、LiTo 不扣，是否要改請使用者決定（建議維持 LiTo，避免多扣 MP）。已在 Issue 留言說明。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeSourceMassSelfTarget.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeSourceAreaCasterTargets.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeSourceSlowFailureMetadata.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SkillTargetingDecisionsTest.java`（新）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SkillTargetingServer1ParityTest.java`（註解）
- `docs/design/gameplay.md`、`docs/design/skill-targeting.md`、`docs/reports/issue-321.md`

## 給下一棒的提示
- 66 沉睡之霧在 1-伺服器 `isTargetCalc` 也排除自己（`:4079-4083`）：以自己為主目標時同樣是「扣費不生效」。本 PR 沒有檢查 LiTo 66 的自施行為，T107–T109 寫範圍技能測試時請一併比對。
- `NativeSourceMassSelfTarget.self()`／`excludeSelf()` 仍會把施法者放進 76 自施時的玩家候選清單（舊行為的遺留），現在因迭代器為空不影響結果；若之後有人改迭代器，記得這個清單裡有施法者自己。
- 本 PR 沒有做任何遞迴刪除；沒有建立暫存目錄（只有 Maven 的 `target/`）。
