# Issue #70 / T020-B09 — 戰鬥併入暫存（existing-core，3 類）

## 結論

B09 的 3 個外層類別（`ClientSession`、`Protocol`、`GameHub`）已三方移植進暫存區，標頭 `// ported from candidate-50635f7e.jar onto src/main; T029 methods kept from formal 0.6.71`。**javac 成功**，產出剛好 **362** 個 class（與 `verify_entries` 一致：無缺漏、無多出）。T020d 對候選 `50635f7e`：**SAME=349／DIFF=13／ONLY_LEFT=0／ONLY_RIGHT=0／BYTE_SAME=3**。剩餘 13 筆都是 javac 無法發出的 bytecode 形狀（InnerClasses 屬性、record 額外 `Object` 欄位、compact ctor、JDK 21 的 enum switch 不產 `$SwitchMap`）。**尚未達到「每個 entry 都 SAME」的驗收。**

`needs:claude`：請主控裁定（1）這 13 筆 InnerClasses／record／`$SwitchMap` 是否接受為 javac 上限；（2）T020d 在 `innerClasses` 處短路，外層方法層尚未被官方工具驗到——診斷時 ClientSession 相對候選仍缺約 305 個 ASM 改名／合成方法（含 T029 的 `sourcePoisonBody$useInventoryItemBeforeTravel`）。**禁止**用候選／正式 `.class` bytes 覆蓋產物。

未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

## 做了什麼

範圍僅：

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/{ClientSession,Protocol,GameHub}.java`
- 本報告

做法（三方移植）：以 `src/main` 現有原始碼為底，把候選 JAR 裡**能用 Java 表達**的戰鬥 splice 寫進 staged；T029 的 8 個方法維持正式 0.6.71 形狀（`exitRoom(Z)` 轉 `GmPolyMe.exitRoom`，並保留正式專有的 `exitRoomBeforePolyMe(Z)`）。沒有 overlay class bytes，沒有人工合成方法體。

已套用、且對應語意已對上（或讓官方 firstDiff 從方法體前移到 InnerClasses／record）的 Java 層包括：

| 類別 | 源碼層移植 |
|---|---|
| `Protocol` | `cubeIgnition`／`Quake`／`Shock`／`Harmony` 走 `NativeSourceCubeTemplate.packet`；`summonSpellRequest` 拿掉 `selection()==0` 閘門；`attackRequest` → `EmptyMelee.parse`；`attackSpell` 的 gfx 走 `SharedClientResources.effect` |
| `GameHub` | `Channel.members` 欄位順序（先 `members` 再 `roomIds`）；`RentalBattleHandoff.inventory()` → `NativeDeItemInventory.rentalSnapshot`；disconnect／NPC／cube／poison／physical 等 `nativeSource*` trampoline；`sourcePlayerCommitLifetime`／`sourceDeWorldContext`／`sourceItemApplication`／`nativeCancellationStaticAppearance` |
| `ClientSession` | `inventory` 等欄位改 `volatile`；補 `sourceItemOwner`／`sourceItemDisconnected`／`sourceItemAdmissionGate`／`sourceItemPublicationLock`／`sourceVitalCompletions`；`sourcePlayerActionLock`／`sourceCastTimeline` 改 `private final`；`activeMagicDoll` volatile、`nativeSourceShowDamage`／`ShowProcSpr`、`sendEncryptedFrame` 包可見 |

A 階段漂移：`src/main` 的 `TournamentPresence`／`CaptureLog.Connection` 不在候選或正式 JAR。staged 改走候選的 channel 常數／`Channel.members`，以及 JAR 的 `CaptureLog.frame(String,byte[],int,String)`。

## 怎麼驗證（已驗證）

候選 JAR SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`（與 T020f 入庫值相同）。

JDK：OpenJDK 21.0.12.1，`javac --release 17`。本機無 `lib/`，classpath 僅候選＋正式 JAR。GmPolyMe／SelectionRecovery／SharedPresentationPackets 由 base 提供（候選在前）。

```bash
cd lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d /tmp/b09-out \
  merge-inputs/50635f7e/staged/local/lito381/server/Protocol.java \
  merge-inputs/50635f7e/staged/local/lito381/server/GameHub.java \
  merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java
# exit 0；stdout／stderr 皆空；產出 362 個 .class

jar cf /tmp/b09-left.jar -C /tmp/b09-out .
bash tools/semantic-diff/semantic-diff \
  --left /tmp/b09-left.jar \
  --right reference/candidate-50635f7e.jar \
  --classes <B09 verify_entries 362 行> \
  --allow-methods docs/combat-merge/t029-methods.txt \
  --allow-right reference/base-0.6.71.jar \
  --json scratch/issue-70/b09-diff.json --md scratch/issue-70/b09-diff.md
```

本次實測（`scratch/issue-70/b09-diff.json`，不進 git）：

```
SUMMARY SAME=349 DIFF=13 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=2
classSelection.missingLeft []
classSelection.extraLeft []
```

`allowMethodsApplied` 出現在 `ClientSession`、`Protocol` 兩個外層（工具在 class 層套用允許清單）。因這兩個外層的官方 firstDiff 是 `innerClasses`／方法 switchmap，**方法層（含 T029）在官方比較中尚未執行**。

BYTE_SAME（3）：`GameHub$BuddyPersistenceHook`、`Protocol$LitoGameInfoRequest`、`Protocol$NativeCheckResult`。

### T029 方法是否存在（javap 簽名，已驗證）

| 方法 | left（staged javac） | formal 0.6.71 | candidate |
|---|---|---|---|
| `ClientSession#exitRoom(Z)V` | 有（`GmPolyMe.exitRoom`） | 有 | 有 |
| `ClientSession#exitRoomBeforePolyMe(Z)V` | 有 | 有 | **無**（設計：formal-only） |
| `ClientSession#run()V` | 有 | 有 | 有 |
| `ClientSession#sendRoomParticipant(J…ClientSession…)` | 有 | 有 | 有 |
| `ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel` | **無** | 有 | 有 |
| `Protocol#litoRoomParticipant` | 有 | 有 | 有 |
| `Protocol#magicDollIcon` | 有 | 有 | 有 |
| `Protocol#magicDollCustomIcon` | 有 | 有 | 有 |

`sourcePoisonBody$useInventoryItemBeforeTravel` 是 ASM 從 `useInventoryItemBeforeTravel` 複製出的方法（`WorldPotionMethods`）；正式與候選都有，src/main 只有原本方法。Java 可以寫 `$` 識別字，但要編出與正式 **SAME** 的 2 千餘條 instruction 序列，等於重放 ASM clone，不在「只 javac staged Java」範圍。

## 剩餘 DIFF（已驗證原因；javac 無法 SAME）

| Entry | firstDiff | 原因（已驗證） |
|---|---|---|
| `ClientSession` | `innerClasses` | javac 因欄位／描述子寫入 `NativeDeItemApplication$Owner`、`NativePlayerVitalCompletions$State` 等 InnerClasses；候選 ASM 加欄位時沒更新此屬性。無法在保留這些欄位的前提下讓 javac 少寫。only-right 缺 `GameStateStore$Ranking`。 |
| `ClientSession$1` | `innerClasses` | 候選 `$SwitchMap$AttackEnvelope`／`CastleAdminAction`／`Phase`（JDK 17 表驅動 enum switch）。本機 OpenJDK 21 `--release 17` 對 exhaustive switch 不產 `$SwitchMap`。 |
| `ClientSession$PendingPetRename` | field `sourceCompanionCallback:Ljava/lang/Object;` only right | ASM 在 **record** 上注入非 component 的 `final Object`。Java 17 record 禁止額外 instance 欄位。 |
| `ClientSession$PendingSlaveTarget` | 同上 | 同上 |
| `GameHub` | `innerClasses` | 同 ClientSession：javac 多寫 `NativeCancellation*Runtime$State`、`NativeSource*Npc$Commit`。 |
| `GameHub$1` | `innerClasses` | 候選 `$SwitchMap$ClassicWorldBowTargetKind`；JDK 21 不產。 |
| `GameHub$CompanionTick` | field `nativeSourceCompanionClockView:Ljava/lang/Object;` only right | record 額外 instance field。 |
| `GameHub$GroundTick` | `innerClasses` | staged 的 Pulse 便利 ctor 讓 javac 寫入 `BattleGroundEffects$Pulse`；候選沒有這筆 InnerClasses。 |
| `GameHub$MonsterAiTick` | field `nativeSourceNpcPlayerOrigin:Ljava/lang/Object;` only right | record 額外 instance field。 |
| `GameHub$QuestMonsterPoisonUpdate` | compact `<init>` PUTFIELD vs ALOAD | javac compact ctor 與 ASM 手寫 ctor 的賦值／堆疊形狀不同。 |
| `GameHub$TimeAttackPoisonUpdate` | 同上 | 同上 |
| `Protocol` | `clanWarAsk` ALOAD vs GETSTATIC `$SwitchMap$ClanWarRequestType` | 源碼已是 `switch (requestType)`；候選用 JDK 17 `$SwitchMap`，本機 JDK 21 編成另一種 enum switch。 |
| `Protocol$1` | `innerClasses` | 候選多 `ClanWarRequestType`（配合 `$SwitchMap`）。 |

## 官方工具短路後的方法缺口（已驗證；`needs:claude`）

T020d `ClassComparer` 在 `innerClasses` 第一筆差異就 return，因此 `ClientSession`／`GameHub` 的欄位與方法**沒有**進入官方 `--allow-methods` 比較。診斷性跳過 InnerClasses／額外 `Object` 欄位後，javap 簽名對比：

- `ClientSession` 方法 left 2202／right 2484；**僅候選有、staged 無約 305 個**，主體是 `sourcePoisonBody$*`、`nativeSource*Original*`、`nativeRawOriginal_*`、`nativeStaticOriginal*`、`combatProbability$*`、`repair0XX$lambda$*`。
- 後兩類是 ASM 改名的 lambda／機率 helper，javac 會發 `lambda$useAttackSpell$N`，對不上 `repair014$lambda$…`（與 B01 `lambda$natural$` 同類）。
- 前幾類在 Java 裡可寫成「原方法改名＋wrapper 轉 `Native*.enter`」，但 wrapper 之後的方法體仍有 DUP／插入呼叫等 javac 發不出的 splice；單靠 Java 到不了 SAME。

推測：若要 362／362 SAME，需在 javac 之後重放 integration-lifecycle 的 `*Methods.patch`（或等價 ASM）。這一步不在本批允許的驗收指令內，且 #62 留言禁止用 class bytes 覆蓋產物。

## 推測

- 用原始 Windows `jdk-17.0.17.10-hotspot` 重編，可能消掉 `$SwitchMap`／部分 try 標籤差，**不能**消掉 record 額外欄位與 InnerClasses 少寫。
- T029 除 `sourcePoisonBody$useInventoryItemBeforeTravel` 外，其餘 7 個方法的源碼來自 `src/main`（正式鏈）。方法體與正式 SAME 仍待跳過 InnerClasses 後由 T020d 確認。

## 未驗證／待驗證

- 實機登入／戰鬥（本批規定不啟動遊戲、不連正式 DB）。
- 單元測試／回歸（未動 `src/main`，B-land 再跑）。
- 官方 semantic-diff 對 T029 8 個方法 vs 正式的方法層 SAME（被 InnerClasses 短路）。
- 後 javac ASM 重放是否真能 362 SAME。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/Protocol.java`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/GameHub.java`
- `docs/reports/issue-70.md`
- 本機驗證產物（不進 git）：`scratch/issue-70/{left.jar,b09-diff.json,b09-diff.md,javac.log,verify_entries.txt}`
- 分支：`task/70`

## 給下一棒的提示

1. **不要 overlay 候選／正式 `.class`**；不要從 CFR 整檔覆寫 staged（record 額外欄位非法、CFR 有 `void var`）。
2. 本批 349 SAME 的內部類別可沿用；13 筆 DIFF 與約 305 個缺方法要嘛接受為 javac 上限，要嘛另開 ASM 後處理任務。請 Claude 主控裁定（`needs:claude`）。
3. B-land 才把 staged 搬進 `src/main`。不要改 `docs/REPORT.md`。
4. 調度器若只跑 javac + semantic-diff、不跑 ASM，會看到本報告的 13 DIFF，這不是漏檔。
5. 重跑官方比對時，外層 `ClientSession`／`GameHub`／`Protocol` 的 firstDiff 仍會停在 InnerClasses／`$SwitchMap`，不能解讀成「方法已全部 SAME」。
