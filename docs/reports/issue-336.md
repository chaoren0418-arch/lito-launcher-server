# Issue #336 報告：T116 法師技能效果數值補驗

- 分支：`task/336`（code，第 1 次嘗試）
- 設計文件：`docs/design/skills-wizard.md` §6（新增），並更新 §2、§3（10、28、39、44、58、66、76）、§5
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/WizardSkillEffectServer1ParityTest.java`（新增，12 個測試）
- 程式：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`（2 處）
- 參考（唯讀）：`D:\lineage開發\1-伺服器`，行號為 2026-11-11 版本

## 做了什麼

| 項目 | 結果 | 1-伺服器 | LiTo |
|---|---|---|---|
| 10／28 吸血 | 一致 | `src/com/lineage/server/model/skill/L1SkillUse.java:3034-3035`、`:781-782`、`:898-899` | `BattleSpellCatalog.java:8403`、`ClientSession.java:46444`、`:89837` |
| 39 吸魔量 | 一致 | `L1SkillUse.java:3168-3174`、`src/com/lineage/server/model/combat/MagicHitCommitPlan.java:41-56` | `BattleManaDrain.java:27`、`ClientSession.java:92633`、`:92674` |
| 58 火牢每跳 | **已修正**（擲骰用的 INT） | `src/com/lineage/server/timecontroller/skill/EffectFirewallTimer.java:197`、`:244`、`:293-304`；`src/com/lineage/server/model/L1AttackList.java:256-294` | `ClientSession.java:56308`（改）、`BattleElementalResistance.java:13`、`NativeSourceGroundPlayer.java:6-39` |
| 44 相消清單 | 法師技能一致；「加速道具速度歸 0」待使用者裁定 | `src/com/lineage/server/model/skill/skillmode/CANCELLATION.java:51-86`、`L1SkillMode.java:144-155`、`db/380_db.sql:48847-48858` | `SourceSkillControls.java:82`、`ClientSession.java:66972`、`NativeCancellationSpeedImmunity.java:10` |
| 29／76 緩速免疫 | **已修正**（76 BATTLE 漏擋） | `L1SkillUse.java:3053-3061` | `ClientSession.java:61314`（新增）、`:60886`、`:63186` |
| 66 自施 | 不一致，**待使用者裁定** | `L1SkillUse.java:4079-4083`、`:2779`、`:2833` | `ClientSession.java:60085`、`:59698` |

### 程式變更
1. **58 火牢**：`ClientSession.fireWallDamage()` 原本用 `CharacterSummary.intelligence()`（資料庫存的基礎 INT），1-伺服器 `EffectFirewallTimer` 用 `user.getInt()`（含裝備、增益）。改成用 `nativeSkillConsumptionIntelligence()`（和 Source 火牢路徑、MP 消耗同一個有效 INT）；裝備快照還沒載入時退回原本的基礎 INT。影響 BATTLE 非 Source 路徑、WORLD 玩家與怪物目標。
2. **76 BATTLE 緩速免疫**：`nativeAreaBattleSlowBody` 對玩家目標在「反制魔法」之後、`activateSlow` 之前，加上「裝備加速防具或強化勇水（brave mode 5）就跳過」，計為失敗、不改速度。29（`nativeMaterialEntangleBody`）與 76 WORLD（`nativeAreaWorldSlowBody`）原本就有同樣判斷。

### 測試（`WizardSkillEffectServer1ParityTest`）
- 數值逐值比對（以 1-伺服器 算式寫成參照函式）：吸魔量（INT 0..127 × 10 種骰 × 5 種目標 MP）、火牢減傷（INT 1..127 × 擲骰 × 火屬性 −128..127，兩條 LiTo 路徑）、火牢 SKD3 修正值與減半順序、吸血技能清單（1..250 只有 10、28）。
- 原始碼順序／條件鎖定（同 `SkillTargetingDecisionsTest` 的作法）：吸血整筆回復且上限 maxHp、吸魔先夾目標 MP 再給施法者、火牢擲骰用有效 INT、三個緩速本體都在 `activateSlow` 前擋免疫、相消免疫表同一張且 12／21／78／79 不在清除清單、66 自施在扣費前拒絕（鎖住現況）。

## 怎麼驗證

### 已驗證
- 1-伺服器 原始碼逐行閱讀（上表行號）。
- 本機 `mvn -B -q -DskipTests test-compile`（只編譯 main 與 test，不執行）：exit 0。
- 本機用 Python 靜態讀檔（`scratch/t336/check_strings.py`，不進 git）確認測試裡每一段原始碼字串都在對應方法範圍內、順序正確：全部 OK。
- **本機沒有執行任何測試**（依規定）；`mvn -B test` 交給 PR 的 GitHub Actions CI，結果見 PR。

### 推測
- 76 BATTLE 免疫目標計為「失敗」：若範圍內全部目標都免疫，LiTo 會送失敗訊息；1-伺服器 免疫目標 `continue` 但仍留在清單，可能不送失敗訊息。只影響訊息，未改。
- 火牢舊路徑（BATTLE 非 Source、WORLD 玩家）沒有直接套 SKD3 的 181／211／68 與 SKM0 的 78；可能在後續傷害提交處理，未逐行追到，未改。
- 1-伺服器 `getHasteItemEquipped` 含武器；LiTo 只看防具（`RuntimeInventory.java:1256`）。3.81 加速武器極少。
- 相消時 12／21／78／79 在 LiTo 是結構上不清（不在清除清單），不讀 DB；只有日後改 DB 清單才會和 1-伺服器 不同。

### 待驗證（交給 Codex／實機）
- 10／28 打中後施法者 HP 回復等於傷害（滿血時不超過上限）。
- 39 雙方 MP 封包：目標扣、施法者加，目標 MP 不足時只吸到剩餘量。
- 58 每秒一跳，穿戴加 INT 裝備後每跳上限變高（WORLD 與 BATTLE）。
- 76 在競技場對穿戴加速防具或喝強化勇水的玩家不會被緩速、原本加速不被抵銷。
- 44 相消後 12／21／78／79 仍在；8／48、3／26／42、43／54 消失。

## 待使用者裁定（本 PR 不改）
| 項目 | 1-伺服器 | LiTo 現況 | 建議 |
|---|---|---|---|
| 66 以自己為主目標 | 扣 MP、播動作、無人生效 | 扣費前拒絕，不扣 MP | 比照 T114 的 76：對齊 1-伺服器（扣費、不生效）；或維持 LiTo（與 #321 S3「扣 MP 沒效果是缺陷」的原意一致） |
| 44 對裝備加速道具的玩家 | 移動速度歸 0，要重新穿脫才恢復 | 加速道具速度不受影響 | 影響 PvP 平衡且需要新狀態設計；建議先維持 LiTo，若要對齊另開 design 任務 |

## 產出路徑
- `docs/design/skills-wizard.md`
- `docs/reports/issue-336.md`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/WizardSkillEffectServer1ParityTest.java`

## 給下一棒的提示
- 10、20、27、28、40、41、66、77 的正義值缺口已由 #325 補上（本分支已合併 main）；66 自施若裁定對齊 1-伺服器，要把 `nativeAreaBattleSleepBody`／`nativeAreaWorldSleepBody` 的扣費（含 #325 的 `applySpellLawfulDelta`）移到主目標判定之前，並改 `fogOfSleepingSelfPrimaryIsRejectedBeforeCostAndSelfIsNeverATarget`。
- 火牢有兩條路徑：`NativeSourceGroundPlayer.pulse`（Source、只有 BATTLE PC）與 `ClientSession.serverGroundPulse`／`serverClassicWorldGroundPlayerPulse`／`serverClassicWorldGroundMonsterPulse`（舊）。之後要再對齊 SKD3／SKM0，建議讓舊路徑也呼叫 `NativeSourceGroundPlayer.resolve`。
- 緩速免疫判斷目前散在 3 個本體（`ClientSession.java:60886`、`:61314`、`:63186`），若要抽成共用方法請一起改 `WizardSkillEffectServer1ParityTest.slowSpellsSkipHasteItemAndStrengthenedBraveTargets`。
