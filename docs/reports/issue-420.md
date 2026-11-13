# Issue #420 — T144：#341 F1–F5 根因定位與修正

## 結論

- **F1、F2、F4（以及 F3 的實際失敗點）有同一個根因，已用靜態證據確認**：候選 `50635f7e` 與 Maven `src/main` 都有 **G16 明確意圖閘** `NativeSelectedPlayerIntent`，歷史資格 runtime `8d26c9d7` 沒有。在 BATTLE 階段，玩家對同房間**敵方玩家**施放 `target_to & 13 != 0` 的技能時，封包若沒有 LiTo 客戶端的 Ctrl 擴充（LFC1 尾碼），這個閘會**靜默丟棄**該次施法：不扣 MP、不扣材料、不擲骰、不送封包、也不印 log。#341 的 probe 是照 8d26 寫的，送的是不帶 Ctrl 的原始 `C_USE_SPELL`。
- 分類：**(C) probe／driver 寫死了歷史 runtime 的輸入**。不歸 (A)：T143 證明 fixture、向量、技能列都相同；P24 的環境差異也解釋不了「整個施法消失」。不歸 (D)：這個閘是已核可的 G16 政策（`docs/reports/customer-gaps.md:46`、#356、`SkillTimingRangeGapsTest.g16*`），B09 方法本身沒有缺陷。
- **修正內容**：只改 probe 的**輸入**。新增的 helper 只在三個條件同時成立時，在封包後面附上 Ctrl 擴充：受測 JAR 含這個閘、目前是 BATTLE、目標是同房間的其他玩家。其他情況照舊送原始封包；用 8d26 重現歷史結果時，送出的位元組也完全不變。**沒有修改、刪除或放寬任何 `need(` 斷言，也沒有改任何預期值**。新增的 CI 守門會把 7 個檔案的 `need(` 內容全部鎖定。
- **還沒修好的**：F3 的 composed 重播用的是凍結、預先編譯好的 probe class，要另開任務重建才能修。F5 是另一條路徑（裝備 proc 治療的事件與 HP 順序），根因還沒找到。T145 重測時，這兩項預期仍會失敗，見下文。

## F1–F5 逐項

| 項 | #341 現象 | 分類 | 證據 | 修正位置 | 預期 T145 會看到 |
|---|---|---|---|---|---|
| F1 Call Nature 165 mode0 | `MP actual=1000 expectedCost=50`；log 沒有任何 Call-of-Nature 行 | **(C)**（已驗證） | 閘：`NativeSelectedPlayerIntent.java:7-14`；呼叫點 `ClientSession.java:71049`；165 的 `target_to=3`（T143 `skills.tsv` 第 4 列）；歷史 8d26 log 有 `Call-of-Nature prompt … mp-cost=50`，#341 完全沒有這行 | `call-nature-material/CallNatureMaterialProbe.java` 的 `cast`；`CallNatureLifecycleProbe.java:13` | targeted 會越過 call-nature 首案；之後的 stage 見「風險」 |
| F2 Cancellation 44 mode0 | 沒有 `Source Cancellation probability` 行 | **(C)**（已驗證） | 呼叫點 `ClientSession.java:67262`；44 的 `target_to=3`（`skills.tsv` 第 3 列）；歷史 log 有 `chance=50 roll=81` | `cancellation-probability/CancellationProbabilityProbe.java`、`CancellationMaterialProbe.java` 的 `cast`；`CancellationLifecycleProbe.java:13`、`CancellationProbabilityLifecycleProbe.java:20` | player 三支 driver 的首案會出現機率行並繼續往下跑 |
| F3 Water Life 170 | 斷言 `Source resources and no HP change` | **(C)＋凍結 build**（根因已驗證；尚未修正） | log 顯示 **mode0（目標是自己）已通過**，印出 `mp=999/1000`，與歷史相同；接著 mode1（目標是 peer）沒有施法行，MP 仍為 1000，與預期的 999 不符，所以斷言失敗。呼叫點 `ClientSession.java:68151`；170 的 `target_to=1`（`skills.tsv` 第 5 列）。`replay_composed_materials.py` 的說明是「frozen probes … no rebuild」，class 取自 `<stage>/build/<folder>/classes`（未入 git） | **本張沒改**：composed 的 probe 是凍結的 class，要另開任務從 git 原始碼加上 G16 意圖後重建（見提示 2） | composed 仍會停在 WaterLife mode1 |
| F4 Curse 33 | `actual first socket write delayed` | **(C)**（已驗證） | 呼叫點 `ClientSession.java:66658`（`attack` 版本）；33 的 `target_to=3`（`skills.tsv` 第 2 列）；歷史 log 有 `C_USE_SPELL paralysis skill=33 … applied=true`，#341 沒有任何施法行，socket 一次都沒寫 | `cancellation-battle-runtime/test_curse_clock_boundary.py`：在它產生的 `CurseClockBoundaryProbe` 裡加入同名的 static `cast`，遮蔽父類別的版本。釘住 SHA 的 `PoisonScopeCurseConsumerProbe.java` 沒動 | 首次 socket 寫入會被延遲，往下進到 target lock 與 deadline 的斷言 |
| F5 ActorState normal 第 7 組 | `child encounter value event is queued before HP`；前一行是 `accepted PC packet failed … ClientSession.sendEncrypted failed` | **未定**：不是 (A)／(B)；在 (C) 與 (D) 之間，**待驗證** | T143 `historical-env.json` 的 `F5_pinned_input_comparison`：向量、support、58 個 fixture、10 個 class 都相同。斷言在凍結的 `NativeDeActorStateRegression.java:138`（#341 stack；所讀 `de-ai-workers-development/regressions/20261016T170128892877Z/inputs/` 副本中位於 :136，`rawFire` 之後要求 child event `pending==1` 且 `hp==40`）。`accepted PC packet failed` 這段字串在兩顆 runtime JAR 裡都找不到，所以是 fixture harness 自己印的：harness 攔到 runtime 送 GFX 時拋出的例外。這是 8d26→50635 在裝備 proc 治療的發布順序或執行緒上的差異，**與 G16 無關** | 本張沒改 | 第 7 組仍會失敗；需要另開任務（見提示 3） |

### 為什麼歷史資格當時能通過（已驗證）

用 Python `zipfile` 唯讀列出 JAR 內容：

| JAR | `NativeSelectedPlayerIntent.class` |
|---|---|
| 歷史 `candidate-cancellation-battle-runtime.jar`（SHA `8d26c9d7…`） | **沒有** |
| `base-0.6.71.jar` | 沒有 |
| `candidate-50635f7e.jar` | 有（`b462d4bf29e5…`），另有 `NativeSkillTargetMetadata`／`$Row` |
| Maven `src/main` | 有，`NativeSelectedPlayerIntent.java`（#138 從 50635 移植） |

這個閘是候選鏈上的 `build_selected_player_intent.py` 加進來的，當時記錄的狀態是 `built-competition-selected-player-explicit-intent-unqualified`。T143 也確認 50635 **從沒跑過**同一套 F1–F5 資格測試。所以這些 probe 只在沒有這個閘的 8d26 上通過過，換成 50635 或 Maven，同一批首案就會一起失敗。

### 排除的環境假說

- 來源名稱 `公牛/SignedHp1` 與 `P24LiToExp/SignedHp1` 不同：只影響 log 上顯示的名字。F3 mode0 自我施法在 P24 上的 MP 計算與歷史一模一樣（999/1000），所以來源 profile 與 MP 起始值都不是原因。（已驗證）
- `weight.rate=1.0`（#83／P24）與正式預設 `100.0` 不同：probe 用的是 `MemoryAccountRepository(...,0,1,40,52)`，自己帶的 rate 就是 1；而且 Water Life 在檢查目標之前會先檢查超重，mode0 已經通過這一關。（推測：不是原因）
- 技能列消耗：170 在 P24 的 `mpConsume=1`，log 也是 `mp-cost=1`，與歷史相同。33 與 44 的消耗與 F 項無關，因為施法在閘那裡就被丟掉了，還沒走到扣消耗。（已驗證）
- 未入 git 的 fixture 過期：T143 `fixtures.tsv` 顯示 probe class 與歷史版本同 SHA。（已驗證，不是 (B)）

因此**不需要改實驗 DB 資料**，本張不標 `needs:local`。

## 改動前後對照（只改輸入刺激，不動預期值）

| 位置 | 改動前 | 改動後 |
|---|---|---|
| `CallNatureMaterialProbe.cast` | `call(pc,"handle",…,packet)` | `call(pc,"handle",…,g16Intent(pc,targetId,packet))` |
| `CancellationProbabilityProbe.cast`、`CancellationMaterialProbe.cast` | 同上 | 同上 |
| `CallNatureLifecycleProbe:13`（只改 handle 路徑） | `…,packet)` | `…,g16Intent(pc,id,packet))`。直接呼叫 `useCallOfNatureSpell` 的路徑保持原始封包 |
| `CancellationLifecycleProbe:13`、`CancellationProbabilityLifecycleProbe:20` | `…,packet)` | `…,g16Intent(pc,target.playerObjectId(),packet))` |
| `test_curse_clock_boundary.py` 產生檔 | 繼承 `PoisonScopeMaterialProbe.cast` | 同名 static `cast`，封包版面與父類別相同，最後一步改走 `g16Intent`。收據多記一欄 `selected_player_intent` |
| `g16Intent` 判定 | — | 條件與 `NativeSelectedPlayerIntent.allowed` 一致：JAR 有這個 class、`phase==BATTLE`、目標在 `hub.roomCombatants(room)` 裡而且不是自己 → 附上 `{4c 46 43 31, op, 01, 67, a5}`。尾碼位元組與 `ForcedCombat.nativeForestOriginalUnwrap` 的要求相同，也與 integration-lifecycle 既有的 `forced()` 一致。NPC、屍體、無效 ID、WORLD 都送原始封包 |

`CallNatureBattleLifecycleProbe` 的目標是 NPC 屍體，閘本來就放行，所以沒改。

## 驗證

**已驗證（本機，靜態）**

- 對 7 個 probe（含釘住 SHA 的 curse 原始檔）解析出全部 `need(` 的參數文字，與 base commit `dfda9593` 逐字相同，數量分別是 16／11／25／12／21／23／16。`git diff -U0` 裡含 `need(` 的只有註解行。
- 三份 helper 與 curse driver 產生的 helper，去掉註解後逐字相同；產生檔只有一個 `static void cast(`；釘住的 SHA `a1ae0b3a…` 沒變。
- `py_compile`：`test_selected_player_intent.py`、`test_curse_clock_boundary.py`、`regression_support.py` 都通過。
- 已同步 `origin/main`（含 T142 #423），`build.yml` 的衝突用「兩邊都保留」解決；`regression_support.py` 本張沒改，與 T142 沒有衝突。

**交給 CI（`combat-regression-tools` 新增步驟 `test_selected_player_intent`）**：`need(` 指紋、尾碼與 `ForcedCombat` 對照、helper 與閘的判定一致，以及用 `JavacTask.parse()` 對 7 個 Java 檔加 curse 產生檔做**語法**解析。本機依規定沒跑 javac、JVM、probe 或回歸入口。

**推測**

- `get`／`call` 這兩個反射 helper（來自未入 git 的 `NativeLightCastProbe`、`NativeDirectoryRegression`）可以對 `GameHub.roomCombatants(long)` 正常運作，因為現有 probe 已經用同一組 helper 呼叫 private 方法與 `get(hub,…)`。CI 只做語法解析，無法驗證符號解析，要等 T145 實跑。
- 加上 Ctrl 只會讓意圖閘放行，不會改變伺服器送出的封包，所以 goldens 與 wire 斷言應與歷史一致。

**待驗證**

- T145 實跑時 F1／F2／F4 是否真的通過，以及 targeted 後續 stage（area-sleep-slow、area-material、cancellation-npc-runtime、cancellation-battle-material，#341 都還沒跑到）是否也撞到同一個閘。這些 stage 的 probe 本張沒改：沒有失敗證據，不做推測性修改。
- F3（composed）與 F5 見下方提示。
- 真實客戶端是不是對所有技能都會送 LFC1（G16 列在客戶端的 G02 項，仍待現場封包確認）。

## T145 需要的步驟

1. 本 PR 合併後，**先更新原始專案 main checkout**（`D:\lineage開發\3-lito開發`，`git pull`）。所有 driver 都從 `--resources`（`options.root`）讀 probe 的 `.java` 現場編譯，不是從 worktree 讀。用 `test_curse_clock_boundary.py` 產生的 `CurseClockBoundaryProbe.java` 有沒有 `static void cast(` 來確認更新成功。
2. 照 #341 的命令與隔離設定重跑（`game.bind` 不同、帶上同目錄的 `combat.properties`），輸出到全新目錄。DB 與資料不用改。
3. 結果怎麼判讀：player（F2）、curse-clock（F4）、targeted 的 call-nature 首案（F1）應該會通過；composed（F3）與 existing 第 7 組（F5）預期**仍失敗**，要等後續任務，不算這次修正退步。

## 給下一棒的提示

1. **T145（codex-local）**：照上一節做。targeted 若在後面的 area-* stage 停下，又沒有施法 log，先看是不是同一個 G16 閘（目標是敵方玩家、BATTLE），再回報。
2. **新任務：composed 凍結 probe 重建（code＋resource）**：`replay_composed_materials.py` 用的是各 stage `build/<folder>/classes` 裡凍結的 class（約 15 個 stage、數十支 probe），對敵方玩家施法的案例在 50635 與 Maven 上都會被閘擋下。需要從 git 原始碼加上與本張相同的 `g16Intent`，在本機重編、更新 `compiled_inputs` 釘值，goldens 照舊比對。這份工作太大，本張不做。
3. **新任務：F5 定位**：從 `NativeDeActorStateRegression.java` 的 `rawFire`（所讀副本 :32）與失敗斷言（stack :138）開始，比對 8d26 與 50635 在 `serverMonsterAiTick` → 裝備 proc 治療 → GFX 送出 → encounter child event 這一段的順序與執行緒。可參考 memory：Quest／TA 怪物 AI 走 `nativeSourceNpcPlayerFirstAppend`。需要實跑時抓 stack，才能判斷是 harness 過時 (C) 還是 runtime 真的拋例外 (D)。
4. **政策確認（雲端／使用者）**：G16 閘目前套用在所有 `target_to & 13 != 0` 的技能（包括 33、44、165、170），不只當初驗證的 7 個技能。customer-gaps 寫的政策是「敵方玩家需要明確意圖」，所以本張把它當作正確行為。如果要縮小範圍，要另開 (D) 任務改 `src/main`。

## 產出

- `lito伺服器/LiTo381-Server/tools/combat-continuation/`：6 支 probe（`call-nature-material/` 2 支、`cancellation-probability/` 4 支）、`cancellation-battle-runtime/test_curse_clock_boundary.py`、新增 `test_selected_player_intent.py`
- `.github/workflows/build.yml`：`combat-regression-tools` 加一個步驟
- 本報告。暫存的分析腳本都在 `scratch/i420/`（不進 git）
