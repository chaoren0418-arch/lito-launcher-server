# Issue #305 報告：T106 技能目標與範圍判定對照 1-伺服器（含 customer-gaps G03–G08）

- 分支：`task/305`（code，第 1 次嘗試），PR #320
- 規格：`docs/design/skill-targeting.md`
- 模型：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/SourceSkillTargeting.java`（新，純函式，不接執行路徑）
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SkillTargetingServer1ParityTest.java`（新）
- 輸入表：`lito伺服器/LiTo381-Server/src/test/resources/server1/skills-targeting-181.tsv`（181 列）
- 產生工具：`lito伺服器/LiTo381-Server/tools/skill-targeting/{extract_skills,golden,entry_ids}.py`
- 差距矩陣：`docs/reports/server-capability-matrix.md` 技能列已更新

## 做了什麼

1. **規格**：逐行讀 1-伺服器 `model/skill/L1SkillUse.java` 的 `isTarget`(:1239-1623)、`makeTargetList`(:1687-1817)、`isTargetCalc`(:4049-4113)、`isTargetFailure`(:4151-4206)、`isUseCounterMagic`(:4243-4256)、反制魔法例外表(:334-344)，以及 `templates/L1Skills.java:17-52` 的 type／target_to 常數，寫成 `skill-targeting.md`（target_to 位元、area 0／−1／N、ranged、through、PK 旗標、視線、只對自己的捷徑、座標技能），並列出 LiTo 的對應程式位置。
2. **重要發現：直線列舉是死碼**。`makeTargetList:1732-1755` 用 `switch (_calcType)` 比對 `LIGHTNING`(17)、`FREEZING_BREATH`(194)，但 `_calcType` 只會是 1～4，所以這段永遠不執行。17 依 area=3 是範圍技能、194 依 area=0 是單體。LiTo 的 G04／G07 修正（`NativeSourceFreezeTarget`、`NativeSourceLightningArea`）正是依這個語意寫的，兩邊一致。
3. **輸入資料**：從 `1-伺服器 db/380_db.sql` 唯讀擷取 `skills` 的 target／target_to／type／ranged／area／through／attr／id。181 個有入口技能（1..220 去掉 38 個保留槽與 122）和本機收尾矩陣 `skills.tsv` 的同名欄位逐列比對 **0 筆不一致**。
4. **參數化測試**（181 列 × 3 組，另有 9 個單項測試）：
   - `sourceModelMatchesIndependentExpectation`：Java 模型和 Python 獨立實作（`golden.py`）的形狀、視線規則、5 種主目標（自己／敵方 PC／隊友／怪物／自己的寵物）下實際進入效果計算的對象，逐列相同。
   - `rangedBoundary`：ranged N 時 N 格可、N+1 格不可；−1 只看畫面。
   - `catalogEffectiveTargetingMatchesSource`：把 181 列 Source 資料裝進 `NativeSkillTargetMetadata` 後，以反射呼叫 `BattleSpellCatalog` 每個 `*Spell(int)` 查詢，對每個 record 的**有效**值（會經過 metadata 投影的 accessor）比對 `maximumRange`／`area`／`areaRadius`／`sourceAreaValue`／`targetTo`／`type`。只有 5 號（指定傳送，獨立處理器）沒有目錄記錄；4 個射程例外（103、133、148、149）列在 `RUNTIME_PROJECTED`，由 `runtimeProjectedListIsExact` 確保清單不會過期。
   - G03–G08 各一個測試，加上 183／188／193 欄位與反制魔法例外表抽查。
5. **改 Java（不一致處）**：
   - **183 Guard Brake、188 Resist Fear、193 Horror of Death**（龍騎士）：`BattleSpellCatalog` 的 `targetTo` 欄位放的是 `probability_dice`（26／35），`type` 欄位放的是 `target_to`（3），整欄錯位；改成 SQL 的 `target_to=3`、`type=1`，同步修 record 驗證式和 `BattleGuardBrakeTest`、`BattleResistFearTest` 的 3 個斷言。這兩個欄位目前沒有任何執行期讀取者（已 grep），所以行為不變，但之後 T107–T109 若用 `targetTo` 篩目標就會錯。
   - `NativeSkillTargetMetadata.replaceForTesting`：測試注入點（`@AfterAll` 還原）。
   - `SourceSkillTargeting`：新增規格模型，供 T107–T109 當目標判定的基準。

## G03–G08 逐項結論（B-land 後是否已修好）

判斷依據：Source 規則（規格 §2–§4）＋ LiTo `src/main` 程式＋本 PR 測試。所有修正類別都已在 `src/main`，且 `ClientSession` 確實呼叫（B-land 已接線）。「已修好」指**程式語意與 Source 一致**；實機行為仍待驗證。

| G | 技能 | Source 規則 | LiTo 現況（`src/main`） | 結論 |
|---|---|---|---|---|
| G03 | 167 風之枷鎖 | area0 單體、ranged −1（畫面）、through0、type1 | `ClientSession.java:43360` → `NativeSourceWindTargets.run`；`Aim.valid` 檢查 metadata（:18）、畫面與 ranged（:19）；`WindShackleSpell.area()/targetTo()/maximumRange()` 由 `windValue` 投影成 0／3／−1 | **已修好**（靜態＋單元測試 `g03…`）。有 metadata 時範圍為 0，不會再打旁人 |
| G04 | 194 暴龍之眼 | 直線分支不可達 → area0 單體、ranged10 | `ClientSession.java:44430`、`:82624` → `NativeSourceFreezeTarget.line()` 有 metadata 時回傳 false、`range()`=10 | **已修好**（單元測試 `g04…`） |
| G05 | 76 集體緩速術 | area3、ranged −1；範圍內不含自己與自己的寵物（`isTargetCalc:4086-4103`）；**以自己或自己的寵物為主目標時，`:2779` 讓整個技能不發動** | 範圍與排除：`NativeSourceVisibilityRange.signedDistance76`、`NativeSourceMassSlowTargets`、`NativeSourceAreaCasterTargets`；自施：`NativeSourceMassSelfTarget`（`ClientSession.java:61063`）保留「自施時仍作用於其他次目標」 | **部分**：射程、範圍、排除自己／寵物已一致；**自施語意與目前 1-伺服器不同**（LiTo 仍對旁人生效，S1 直接不發動）。本 PR 未改（影響扣費／顯示時序，無法在本機執行測試），列為下一棒 |
| G06 | 29 緩速、152 地面障礙、153 魔法消除 | area0 單體、target=buff 且 target_to 含 PC → 可對自己（`isTarget:1389`）；29／153 ranged −1、152 ranged10 | `ClientSession.java:41433/41437` → `NativeSourceNegativeTargets`；自施 `NativeSourceNegativeSelf.legal`（`ClientSession.java:62786`）；29 射程經 `NativeSourceVisibilityRange` 投影成畫面 | **已修好**（目標與範圍；單元測試 `g06…`）。狀態結果、准入時序屬 T102／T107 |
| G07 | 17 極光雷電、148 火焰武器、149 風之神射 | 17：area3 以目標為中心（直線不可達）；148／149：target_to=1 只對 PC、area0、ranged −1 | 17：`NativeSourceLightningArea.points`（`ClientSession.java:87832`）回傳 7×7=49 格；148／149：`NativeSourceWeaponBuffTargets`（`ClientSession.java:41564/41568`）以 metadata 驗證並用畫面距離 | **已修好**（單元測試 `g07…`）。148／149 修正只在 BATTLE 相位；WORLD 走原本體（目錄射程 0） |
| G08 | 40 黑闇之影、103 暗黑盲咒 | 兩者 area0 單體；40 ranged3、**103 ranged0** | `ClientSession.java:41413/41417` → `NativeBlindSceneAction`；`selected()`（:21）用 metadata 的 ranged 判定命中，機率由 `chance()` 歸 0 | **已修好**（命中判定；單元測試 `g08…`）。注意寵物分支 `companion()`（:31）的准入仍用目錄射程 3：103 對 1～3 格的寵物會扣 MP、播動作，但不生效；S1 在射程外直接不發動。是否算缺陷待驗證 |

## 怎麼驗證

### 已驗證
- 181 列 SQL 欄位 vs 收尾矩陣 `skills.tsv`：0 筆不一致（`tools/skill-targeting/entry_ids.py`）。
- 目錄有效值的靜態預測（`scratch/t305_catalog_compare.py`，未進 git）：除 AttackSpell 系列（次要建構子，已人工逐筆核對 19 筆射程）外全部解析；不一致只有 29／44／76（accessor 投影成 19，與 Source −1 相符）、103／133／148／149（執行期由 metadata 判定）、183／188／193（本 PR 修正）。
- 合併 main（#304）後，本機 `javac -proc:none --release 17` 編譯 `src/main`＋`compile-stubs` 與新／改的 3 個測試類別：exit 0。**本機沒有執行任何測試**（依規定）。
- **PR #320 CI（run 42921793903，commit `ca943d80`）全部通過**：`Server tests` 的 surefire 報告（artifact `server-test-evidence`，1277 個 TEST-*.xml 加總）**Tests run: 6640, Failures: 0, Errors: 0, Skipped: 117**；其中 `SkillTargetingServer1ParityTest` 553 個（0.433 s）、`BattleGuardBrakeTest` 4 個全過。Server（Maven 建置）、Launcher、Build-Package、Semantic gap 也都通過。之後只再加這份報告（不含程式變更）。

### 推測
- LiTo 正式 `380_db.skills` 與 `1-伺服器 db/380_db.sql` 的這 6 欄相同：依據是收尾矩陣（候選 DB 快照）0 筆不一致；沒有連正式 DB 確認。
- `BattleSpellCatalog.isInNativeScreen`（曼哈頓 ≤18／邊角 19）與 S1 `Point.isInScreen` 逐格相同：未讀 S1 `isInScreen` 實作。
- G05 自施的扣費時序：S1 `runSkill:2779` 在消耗之前或之後返回，未追到 `handleCommands`。

### 待驗證（交給 Codex／實機）
- G03–G08 的實機行為：167／194 單體不誤中旁人；76 自施；29／152／153 自施；17 範圍 3；148／149 只對 PC；40／103 距離（103 只有同格才生效）。
- 103 對 1～3 格寵物「扣 MP 不生效」與 S1 的差異是否要改。

## 需要使用者決定（未改程式）

| 項目 | S1 | LiTo | 建議 |
|---|---|---|---|
| `target=none` 的範圍攻擊技能（53 龍捲風、59 暴風雪、62 地裂術、70 火風暴、72 反制無所遁形、80 冰雪颶風、189）打不打 PC | 永遠不是 PK，非攻城戰時**不打 PC**、不打自己的寵物（`isTarget:1397-1460`） | 競技場依 `hub.canAttack` 打敵方玩家（`ClientSession.java:90022-90027`） | 維持 LiTo（競技場是 PvP），寫進 T100 玩法規格 |
| `target=attack` 的範圍技能以 PC 為主目標（17、22、25、30、65、74、184） | 是 PK，範圍內所有 PC **連隊友**都中（target_to 只看 PC 位元） | 只打 `canAttack` 為真的敵人 | 同上，維持 LiTo |
| 76 以自己為主目標 | 整個技能不發動 | 對範圍內其他人生效 | 建議改成 S1（另開 code 任務） |

## 給下一棒的提示

- **T107–T109** 寫技能行為測試時，直接用 `SourceSkillTargeting.effects/isTarget/inRange` 當目標判定基準，用 `NativeSkillTargetMetadata.replaceForTesting` 裝入 `skills-targeting-181.tsv` 的列（記得在 `@AfterAll` 還原，其他測試假設 metadata 是空的）。
- `BattleSpellCatalog` 的目錄欄位不能直接當目標規則：29／44／76／167／194 的射程與範圍在執行期由 metadata 覆蓋；103／133／148／149 的目錄射程與 Source 不同，靠各自的 handler 判定。新增 handler 時請改用 `NativeSkillTargetMetadata.row(skill)`。
- G05 自施：若決定照 S1，改 `NativeSourceMassSelfTarget.enabled()` 讓自施（及以自己的寵物為主目標）時清單為空，並追 `L1SkillUse.handleCommands` 確認扣費在 `runSkill` 前還是後。
- 規格 §3 未建模的條件（安全區、攻城戰、絕對屏障白名單 `:1519-1552`、隱藏 NPC、死亡目標）在競技場大多不會發生，但絕對屏障白名單會影響 PvP，建議併入 T107。
- 反制魔法：S1 是硬編碼表＋DB 外控表；LiTo 用 `sourceCounterMagicBypassSkills`（DB）與目錄 `counterMagicBypass` 欄，兩者未比對。
- 本次作業過程中，我清理自己建立的暫存編譯目錄 `lito伺服器/LiTo381-Server/target/t305-javac` 時用了 `Remove-Item -Recurse`，違反 worktree「不做遞迴刪除」規定（該目錄由我建立、內含 javac 輸出、沒有 junction，未造成損害）。之後請不要這樣做；這個目錄仍留在 worktree 的 `target/`（已被 .gitignore 排除）。
