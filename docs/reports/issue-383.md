# Issue #383（T132）競技場 PvE 怪物魔法收尾：反制魔法、安息喊話、多人換目標

分支：`task/383`。設計：`docs/design/skills-npc.md` §6（本次新增）。行為以 `D:\lineage開發\1-伺服器`（唯讀）為準。

## 做了什麼

### 1. 反制魔法擋 NPC 魔法（C11）
- 1-伺服器：`L1SkillUse.java:2767-2775`（硬編碼表＋DB 表合併）、`:2868-2874`（`TYPE_ATTACK` 在算傷害前先問）、`:4243-4256`（擋下就移除狀態、播特效）。
- DB 表 `skills_魔屏無效技能`（`db/380_db.sql:49020-49030`）共 11 列：44、87、112、132、153、161、173、174、187、188、192。內容與「是否也在硬編碼表」寫進 `skills-npc.md` §6.1。這 11 個都不是競技場怪物會放的技能，所以對本範圍 18 列怪物魔法沒有影響；仍照抄成常數，避免之後加怪時漏掉。
- LiTo：Quest 與 Time Attack 的怪物魔法流程（`ClientSession.serverQuestMonsterMagicAttack`、`serverTimeAttackMonsterMagicAttack`）在每個目標擲傷害骰之前呼叫 `consumeCounterMagicAgainstNpcSkill`。玩家開著反制魔法時：擋掉這一次、狀態消耗、送既有的反制魔法特效、不受傷，也不做後續的反射／吸血。18 列怪物魔法中 15 列會被擋；安息 10027／10028／10029 在例外表內，不擋、也不消耗狀態。
- 玩家對玩家的反制魔法程式沒有動；消耗用的是同一個 `BattleTimedStatus.consumeCounterMagic`。

### 2. 安息 10027／10028／10029 的 `$3717` 喊話
- 1-伺服器：`L1SkillUse.java:3037-3041`，每個目標送一次 `S_NpcChat(npc, "$3717")`（`S_NpcChat.java:20-26`：color 0、npc id、`nameId + ": " + chat`），範圍 8 格（`L1NpcInstance.java:4138-4140`）。
- **LiTo 有對應封包**：`Protocol.sayCode`＝`S_SAY_CODE 0x2D`（`cdshh`，程式註解記錄的客戶端 receiver `sub_4A3140`），精靈守護者警告與修理 NPC 台詞已經在用，沒有自創 opcode。`docs/reference/client-protocol-needs.md` 沒有這個 opcode 的列（該文件沒收聊天類封包）；本次沒有另外反組譯 `LiTo.bin`。
- 客戶端文字：用 PakViewer CLI 從 `D:\lineage開發\0-客戶端\Text.pak` 解出 `desc-c.tbl`（輸出在 `scratch/t132/`，不進 git），`$3717`＝「給你安息」，`$1116`／`$1426`／`$1605`＝安塔瑞斯／法利昂／巴拉卡斯。先解的 `string-c.tbl` 是系統訊息表，不是 `$` 名稱表（只有 3484 列，沒有 3717）。
- LiTo：`ClientSession.publishNpcRestShout`，龍放安息命中時對 8 格內的收件人送 `sayCode(0, 龍 id, "<nameId>: $3717", x, y)`。Quest 與 Time Attack 兩個流程都接了（目前只有 Time Attack 的龍有這三個技能）。

### 3. ChangeTarget=3（C9）
- 1-伺服器：`L1MobSkillUse.java:206-213`（該列先換目標再做觸發檢查）、`:777-842`（候選＝觸發距離內、視線可及、在仇恨名單、沒死的玩家；`nextInt(size*100)/100`；沒有候選用原目標）。
- 8 列：45682 act 4／5／6、45683 act 3／4、45684 act 4／5／6（`db/380_db.sql:34616`、`:34617`、`:34613`、`:34625`、`:34618`、`:34911`、`:35106`、`:35107`）。
- LiTo：
  - `GameHub.nativeSourceNpcPlayerFirstAppend`（實際在用的 AI 入口）在 Time Attack 分支把房間內可被鎖定的玩家連同視線結果傳給 `BattleTimeAttackEncounter.aiAttackWithChangeTarget`。
  - encounter 新增每隻怪的仇恨名單（AI 鎖定過的玩家＋`damageTimeAttackActor`／`nativeSourcePhysicalTimeDamage` 打過牠的玩家），對 ChangeTarget=3 的列逐列過濾候選並抽一位；該列的觸發距離、施法距離、面向都用抽到的玩家。
  - 抽到別人時，`MonsterAiTick` 的目標、面向、來源檢查物件一起換（`NativeSourceNpcPlayerView.retarget`），後面的傷害流程不變。
  - 單人：候選只有原目標，結果與修改前相同。
- `GameHub.appendMonsterAiTick` 與 `nativeSourceNpcFirstViewAppend` 沒有任何呼叫端（已搜尋），沒有接。任務描述寫的 `appendMonsterAiTick` 是舊入口。

### 4. 文件
- `docs/design/skills-npc.md`：C9、C11、三列安息的結論改為已實作；統計更新；新增 §6（含 DB 例外表內容與剩餘差異）；`CAT:` 行號因目錄檔插入程式而位移，已整批更新。
- `docs/reports/server-capability-matrix.md`：技能列與 Backlog 列更新。
- `docs/acceptance/M4-RUNBOOK.md`：C 部加 V39（T129 已合併，直接改 RUNBOOK），時間盒 C3 一併更新。
- `TASKS.md`：T132 列填 #383、狀態 review。

## 怎麼驗證

### 已驗證
- 1-伺服器 的 `L1SkillUse.java`、`L1MobSkillUse.java`、`L1MobSkill.java`、`L1MonsterInstance.java`、`S_NpcChat.java`、`SkillCounterMagicExceptionTable.java`、`db/380_db.sql` 逐行讀過，行號寫在設計文件與測試註解。
- 本機 `mvn -B -o -q -DskipTests test-compile` 通過（main＋test）。
- 客戶端字串表以 PakViewer CLI 唯讀解出核對（見上）。
- 新增單元測試（期望值都附 1-伺服器 檔名與行號）：
  - `BattleTimeAttackMagicSkillCatalogTest.counterMagicAbsorbsNpcAttackSkillsExceptRetainedExceptionList`：DB 表 11 列；18 列怪物魔法只有 10027–10029 不可擋；可擋技能擋一次並消耗、例外技能不擋不消耗、沒有狀態不擋。
  - `…restSkillsShoutMessage3717ThroughNativeNpcSpeechPacket`：只有安息會喊；字串 `$1116: $3717`；封包 bytes（opcode 0x2D、type 0）；8 格範圍。
  - `…changeTargetRandomRowsAndIndexMatchRetainedMobskill`：9 隻怪 33 列的 ChangeTarget 值（8 列為 3）；索引公式。
  - `BattleTimeAttackEncounterTest.changeTargetRandomPicksAnotherHatedPlayerInsideTriggerRange`、`…ExcludesUnhatedOutOfRangeAndBlockedPlayers`、`…AppliesPerRowAndLeavesOtherRowsOnAiTarget`、`…KeepsSinglePlayerBehaviour`：兩人時抽到另一人、擲到原目標不換、不在仇恨名單／距離外／視線被擋不列入、觸發距離對換過的目標檢查、物理列也換、ChangeTarget=0 的列不換、單人 8 列逐一與修改前相同。
- 依本機執行限制，**測試沒有在本機跑**；交給 PR 的 GitHub Actions（`mvn -B test`）。CI 結果見文末「CI」。

### 推測
- 反制魔法與喊話在 `ClientSession` 兩個流程裡的接線、`GameHub` 把換過的目標交給 tick 的接線，沒有單元測試直接覆蓋（現有測試沒有任何一個會跑 `serverMonsterAiTick`）；測試覆蓋的是它們呼叫的判定函式與 encounter。接線正確性靠編譯與讀程式。
- `NativeSourceNpcPlayerView.retarget` 在正式戰鬥規則載入（`CombatRules.loaded()`）時用同一次掃描已記下的成員資料重建來源物件；這條路徑測試環境不會走到。若重建失敗，程式會退回原目標（不會換人，也不會出錯）。
- `S_SAY_CODE` type 0 在客戶端的顯示位置與顏色沒有查。

### 待驗證（實機，已寫進 `M4-RUNBOOK.md` V39）
1. 法師開反制魔法後被 Quest／Time Attack 怪物魔法打：第一次不扣血、狀態消失、有特效；下一次照常扣血。
2. 龍放安息：頭上出現「<龍名>: 給你安息」；開著反制魔法也照樣扣血且狀態不消耗。
3. 兩人以上的 Time Attack 龍戰：龍的 8 列技能有時打向不是最近的玩家；單人時不變。
- V39 的資料前置缺口：`m4-seed-exp.sql` 的法師 `M4A4` 沒有技能 31（反制魔法），本次沒有改 seed。

## 未驗證／沒有做
- 30004 火牢是否對齊、C10（先選列後檢查 MP）、距離 2 的攻擊位置擲幣：依任務說明不做，留在 Backlog。
- 與 1-伺服器 仍不同的地方（`skills-npc.md` §6.1、§6.3）：被反制魔法擋下的目標仍留在攻擊封包的目標清單（命中旗標 0）；換目標的候選只有玩家、不含隱身玩家；毒與立方造成的傷害不記仇恨；Quest Battle 沒有接換目標（Quest 的怪沒有這種列）。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleTimeAttackMagicSkillCatalog.java`
- `…/BattleTimeAttackEncounter.java`
- `…/ClientSession.java`
- `…/GameHub.java`
- `…/NativeSourceNpcPlayerView.java`
- `…/NativeSourcePhysicalNpc.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/BattleTimeAttackMagicSkillCatalogTest.java`
- `…/BattleTimeAttackEncounterTest.java`
- `docs/design/skills-npc.md`、`docs/reports/server-capability-matrix.md`、`docs/acceptance/M4-RUNBOOK.md`、`TASKS.md`
- 本報告 `docs/reports/issue-383.md`

## 給下一棒的提示
1. V39 要先替法師補技能 31（`m4-seed-exp.sql` 的 E2 沒有）；補 seed 時記得同步 `tools/acceptance/ci/run-sql-checks.sh` 的預期筆數。
2. 實機若看到龍換目標後「有動作沒傷害」，先查 console 有沒有該拍的 `Time Attack monster magic … target=`；沒有的話是 `retarget` 退回或 tick 被來源檢查擋掉，從 `NativeSourceNpcPlayer.validTick` 查起。
3. 毒／立方傷害要記仇恨的話，入口是 `GameHub.nativeSourcePoisonTimeDamage`、`nativeSourceCubeTimeDamage`（兩者的 commit 物件裡有施放者，本次沒有追）。
4. `client-protocol-needs.md` 沒有聊天類封包（`S_SAY 0x13`、`S_SAY_CODE 0x2D`）的列；要補的話證據在 `Protocol.java` 的 `say`／`sayCode` 註解與 `ProtocolNpcSpeechTest`。
5. `$` 開頭的名稱編號查 `Text.pak` 的 `desc-c.tbl`（第 N+2 行＝`$N`，CP950）；`string-c.tbl` 是系統訊息表。

## CI
（push 後回填）
