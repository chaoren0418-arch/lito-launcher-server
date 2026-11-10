# Issue #70 — B09 戰鬥併入暫存（existing-core，3 類）

## 結論

**依 2026-11-10 主控裁定（使用者選 C）完成本輪。** 24 個有逐路徑順序或型別差異的方法已改 Java；其餘 174 個 DIFF 方法與 9 個 ONLY_LEFT lambda，呼叫與欄位讀寫的多重集合都和候選相同，已逐條列進 `docs/combat-merge/b09-shape-diffs.md`，留給 M2 離線回歸驗收。T029 八方法仍然通過。

剩下 1 個例外是 `sourceApplicationRun()V`。它多一次 `rethrowUnchecked` 呼叫，這是前輪已核准的原樣轉拋 helper。Java 原始碼沒有不加這個呼叫的寫法（理由見下），需要主控確認是否接受。

| 層級 | 本輪結果（已驗證） |
|---|---|
| 362 entries | 360 SAME、1 SAME_CFG（GameHub）、1 DIFF（ClientSession） |
| 類別 missing／extra／ONLY_LEFT／ONLY_RIGHT | 全部為 0 |
| 方法 | 175 DIFF（174 列入形狀清單＋`sourceApplicationRun`）、9 ONLY_LEFT（列入形狀清單）、0 ONLY_RIGHT、84 SAME_CFG、50 SAME_PATHS、6 ORPHAN_IGNORED |
| T029 | 6 SAME、2 SAME_EFFECTS（`exitRoomBeforePolyMe`、`sourcePoisonBody$useInventoryItemBeforeTravel`） |
| 驗證 JAR SHA-256 | `aa94ee447b870509035c1cb947d30085d91eb380321cbc0a0aef311ffed0b679` |

前輪（retry5）為 181 DIFF、83 SAME_CFG、49 SAME_PATHS；本輪 DIFF 減少 6 個，SAME_CFG、SAME_PATHS 各增加 1 個。

## 做了什麼

1. 合併最新 `origin/main`（`e50e6127`）。合併沒有改到 `lito伺服器/` 與 `tools/`，合併後 JAR 與前輪 bytes 相同（SHA `f45f5b0a…`）。
2. 新增診斷工具 `scratch/issue-70/EventOrder.java`，做「逐事件後繼集合」比對。它把方法建成指令級 CFG，CFG 包含一般邊，以及呼叫、indy、monitor、athrow 的例外邊。對每個事件（呼叫、欄位讀寫、indy、monitor）求下一步可能遇到的事件集合，再比較兩側的多重集合。它取代前輪「線性呼叫序列不同」的粗分類，用來分辨真的順序差異和單純的區塊排列不同。
   - 第一次跑：181 個 DIFF 中有 25 個 `ORDER_DIFF`，156 個 `LAYOUT_ONLY`。
   - 改完 Java 後：只剩 `sourceApplicationRun` 1 個 `ORDER_DIFF`，其餘 175 個（含 auto-select 父方法）都是 `LAYOUT_ONLY`。
3. 改 Java（只動 `merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java`），共 24 個方法：

| 方法 | 原本的差異 | 修正 |
|---|---|---|
| `serverHonorRespawnDue` | 候選的 try 涵蓋到整段復活處理（`advance`／`last`／`battleGfx`），staged 只涵蓋 `honorRespawnPending`，所以失敗時不會呼叫 `exitRespawn(throwable)`。另外候選先讀 `playerMaxHp` 才呼叫 `advance` | try 改成涵蓋整段，`playerMaxHp` 先存進區域變數（同 `respawnAtBattleSpawn` 的寫法） |
| `classicWorldCompanionSnapshots` | 候選在 `synchronized` 裡只做 `stream…toList()`，`NativeCompanionPlacement.snapshots` 在離開鎖之後才呼叫；staged 在鎖內呼叫 | 改成鎖內取 list、鎖外呼叫 `snapshots` |
| `sourcePlayerBattleCommit` | 候選在「讀欄位」和「呼叫該欄位的方法」之間插入 `requireCurrent()`；staged 把多次 `requireCurrent()` 擠在前面，共 17 處 | 依候選拆成「`requireCurrent`→讀欄位→`requireCurrent`→呼叫」 |
| `sourcePoisonBody$sourceDeathCancellation` | 同上，`requireReceiver(this)`，共 37 處 | 同上 |
| `sourcePoisonBody$applyFatalityCancellation` | 候選先讀 `timedStatus` 再呼叫 `silenceNotice` | 先存欄位 |
| `attackClassicWorldPlayer` | 候選先讀 `foeSlayerSequence` 再呼叫 `CombatKiringku.hit` | 同一檔案 47862 行既有寫法 |
| `resolveClassicWorldScarecrowHit` | 候選先呼叫 `heading` 才讀 `hub` | 拆成兩行 |
| `sourcePoisonBody$takeClassicWorldDirectDamage` | 候選每個 return 先讀 `NON_FATAL` 再呼叫 `endCalc`，而且 `endCalc` 在 try 涵蓋內 | 改成先存回傳值、再 `endCalc`、再 return |
| `nativeMaterialAreaWindBody` | 候選 log 的 `present=` 以 boolean 串接（印 `true`／`false`），staged 印 `1`／`0` | 改為 `(var21 != 0)`。`NativeCompanionWindAction.select` 的 `[0]` 只會是 0 或 1（已讀原始碼確認） |
| `handleIvoryTowerElfAttributeAction`、`nativeMaterialArmorBreakBody`、`nativeMaterialAwakenBody`、`nativeMaterialBloodlustBody`、`nativeSelfWeightBody`、`useBounceAttackSpell`、`useBurningSlashSpell`、`useCounterBarrierSpell`、`useDragonSkinSpell`、`useFoeSlayerSpell`、`useGuardBrakeSpell`、`useMortalBodySpell`、`useReductionArmorSpell`、`useResistFearSpell`、`useSolidCarriageSpell` | VF 把 int 訊息碼宣告成 `short`／`byte`，字串串接的 indy 描述子是 `S`／`B`，候選是 `I` | 15 個區域變數改回 `int`。印出的數字相同，描述子與候選一致 |

修改前後的例子（`sourcePlayerBattleCommit`）：

```java
// 修改前
NativeAcceptedState.requireCurrent();
NativeAcceptedState.requireCurrent();
this.dragonSkinStatus.clear();
// 修改後（與候選 bytecode 的事件順序一致）
NativeAcceptedState.requireCurrent();
BattleDragonSkinStatus battleDragonSkinStatus = this.dragonSkinStatus;
NativeAcceptedState.requireCurrent();
battleDragonSkinStatus.clear();
```

4. 產生 `docs/combat-merge/b09-shape-diffs.md`，內容如下：
   - 收錄 174 個 DIFF 方法，每項有類別、差異類型、比較器第一差異點與代表 hunk；另有 9 個 ONLY_LEFT lambda 的逐條檢查。
   - 檔案由 `scratch/issue-70/r6-shapedoc.py` 產生。腳本會先 assert，每個收錄方法都要符合「呼叫多重集合相同、欄位多重集合相同、事件後繼 `LAYOUT_ONLY`」，才寫進清單。
5. 沒有改比較器、核准清單、`src/main`、`pom.xml`。

## 已驗證

- 編譯：Java 17.0.17，`javac --release 17 -encoding UTF-8` 編譯三個 staged 外層類別，AsRecord 結果為 `scanned=362 converted=6`。產物集合與 B09 `verify_entries` 完全相同（`entries=362 missing=0 extra=0`）。
- 語意比較：用 `scratch/issue-70/verify-259.py r6-b` 從工作樹重編比較器與 AsRecord（ASM 9.7）。指令與傳入的核准清單（T029、effects、extra-methods、exceptions、metadata）全文在 `scratch/issue-70/latest-command.json`，結果在 `r6-b-diff.json`／`.md`。
- 呼叫與欄位多重集合（`retry5-classify.py r6-b`）：
  - 175 個 DIFF 中，欄位讀寫多重集合不同的方法數為 0。
  - 呼叫多重集合不同的只有 `sourceApplicationRun`（多 `rethrowUnchecked` 1 次）。
- 常數／NEW／CHECKCAST 等運算的多重集合，有 7 個方法不同。逐一看過，都是布林具體化（DUP 對 ICONST 分支）或 DUP2 堆疊形狀，例如 `nativeMassTeleportAnswerBody` 是候選以 `DUP2` 重用 `this, null`。
- 事件後繼集合（`r6-eventorder.py r6-b`）：175 個 DIFF 加 auto-select lambda，174 個 `LAYOUT_ONLY`，`sourceApplicationRun` 是 `ORDER_DIFF`。
- 稽核（`evidence259.py`，結果在 `r6-b-audit.txt`）：
  - T029：`exitRoom`、`run`、`sendRoomParticipant`、`litoRoomParticipant`、`magicDollCustomIcon`、`magicDollIcon` 都是 SAME；`exitRoomBeforePolyMe`、`useInventoryItemBeforeTravel` 是 SAME_EFFECTS。
  - `nativeLightCommit` 是 SAME_PATHS（`LIGHT_PATHS left=90 right=90 equal=true`，兩側 CHECKCAST 相同）。
  - `GameHub#battleDisconnectWork` 是 SAME，`ClientSession$1#<clinit>` 是 SAME。
  - auto-select lambda 本體以同名直接比對為 SAME。
- 類別層級：四個 healing record、`ClientSession$1` switch-map 都是 SAME。
- wrapper 證明（`retry5-proof.py`，結果在 `r6-b-wrapper-proof.tsv`）：
  - 8 個 wrapper 都通過「參數依序載入、唯一同類別 private 呼叫（opcode 182）、無 handler、與候選 indy 目標相同」。
  - 用這 8 條試跑 `--extra-methods`，比較器仍以「requires private static」拒絕（`r6-b-extra-rejection.log`）。依裁定不改比較器。
- 格式：`git diff --check origin/main` 檢查完整 PR 累積差異（三個 Java、exceptions／extra-methods 清單、`b09-shape-diffs.md`、本報告），結果沒有輸出。`Protocol.java` 檔尾為單一換行（`}\n`），沒有多餘空白行。提交後另跑 `git diff --check origin/main...HEAD`。

## 推測（未經實機）

- 前面表格中的 24 個修正方法，修正後與候選事件順序一致。其中下列 4 項在原本 staged 下有可觀察的行為差異，這是根據 bytecode 的推論：
  - `serverHonorRespawnDue` 失敗時沒有釋放 respawn scope。
  - `classicWorldCompanionSnapshots` 持鎖時間較長。
  - `requireCurrent`／`requireReceiver` 的檢查時點不同。
  - Wind Shackle 拒絕 log 文字不同。
- 174 個形狀差異方法通過的是局部（一步）後繼檢查，可以排除相鄰事件互換，以及呼叫跨出或跨入 try／synchronized；但這不是完整的路徑等價證明。

## 待驗證／待裁定

1. **`sourceApplicationRun`（需主控裁定）**：候選直接呼叫 `SourceLogin.allowed`，該方法宣告 `throws Exception`，外面沒有 try。`sourceApplicationRun` 與 `run()` 都沒有 throws 宣告。Java 只有三種寫法：
   - (1) catch 後呼叫 helper 轉拋：就是目前的 `rethrowUnchecked`，多 1 次呼叫。
   - (2) 宣告 `throws Exception`：`run()` 覆寫 `Runnable.run`，必須再加 catch，會破壞 T029 `run()` SAME。
   - (3) `throw (RuntimeException) e`：checked 例外會變成 ClassCastException，行為不同。

   本輪保留 (1)，`rethrowUnchecked` 只做 `throw (E) t`。若主控認可，請把這個方法併入形狀清單。
2. 174 個形狀差異方法與 9 個 ONLY_LEFT lambda，由 M2 第 2 點的離線戰鬥回歸驗收。
3. 單元測試／回歸依任務安排在 B-land 統一處理；本機規定不執行編譯出的程式，所以本輪只做靜態編譯、bytecode 比較與 CI。

## 產出路徑與重現

- Java：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java`
- 形狀清單：`docs/combat-merge/b09-shape-diffs.md`
- 本報告：`docs/reports/issue-70.md`
- 重現：在 worktree 根目錄依序執行以下指令。產物在 `scratch/issue-70/r6-b-*` 與 `scratch/issue-70/target/r6-b*`。

```
py scratch/issue-70/verify-259.py <新標籤>
py scratch/issue-70/evidence259.py
py scratch/issue-70/retry5-classify.py <標籤>
py scratch/issue-70/r6-status.py <標籤>
py scratch/issue-70/r6-eventorder.py <標籤> scratch/issue-70/<標籤>-diff-keys.txt scratch/issue-70/<標籤>-eventorder.txt
py scratch/issue-70/retry5-proof.py
py scratch/issue-70/r6-shapedoc.py <標籤>
```

## 給下一棒的提示

1. 主控：請裁定 `sourceApplicationRun` 的 `rethrowUnchecked`（上方三種寫法比較）。若接受，B09 就符合「除清單外全部可接受」，可以送審 PR #95。
2. M2 回歸驗收：`b09-shape-diffs.md` 的差異類型中，「例外表涵蓋＋含迴圈」佔 113 個，主要是 VF 的 try／synchronized 切法，回歸時優先覆蓋例外路徑（技能失敗、斷線、死亡取消）。
3. `EventOrder.java` 是 scratch 診斷工具，沒有進正式比較器。之後若要把「事件後繼集合」做成正式模式，應另開比較器任務（對應 PLAN U2-1 的 A 路線）。
