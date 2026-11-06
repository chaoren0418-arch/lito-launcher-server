# Issue #72 — A01 正式 0.6.71 暫存（#203 合併後最終重驗）

## 結論

2026-11-06（UTC+08:00），**A01 本批靜態驗收通過：183 SAME、2 DEVIATION、0 DIFF；185 個 entry，missingLeft=[]、extraLeft=[]，ONLY_LEFT=0、ONLY_RIGHT=0。** 方法證明為 **169 VISIBILITY_BRIDGE、1 LAMBDA_PAIRED_BY_CALLSITE、0 SAME_CFG**。這些方法包含在 NativeDeInstanceState 之內；該類因既有建構子偏離而列 DEVIATION，不能把方法數當成額外的 entry 數。

已由 GitHub 連接工具查證 [PR #215](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215) merged=true，合併時間 `2026-11-05T20:19:36Z`，merge commit `5ae1eac20d75b75b2bf9fbf302e8d878a32c62bd`。本輪 fetch 後將 `origin/main @ 567b0cb7762864b10ef876f90749e74a66e9bb01` 合併至 task/72，合併提交 `c294ca806aded5e4f5cfaccca165dd30c3650f33`，使用其中已合併的 #203 工具重新建置與全量驗收。

本報告取代前輪「183 SAME／1 DEVIATION／1 DIFF、等待 #203」狀態；deviations.md 中前輪重驗段落保留原文作核准歷程，最新驗收以本報告為準。尚未執行 #78 A-land、執行期測試或部署。

## 做了什麼

- 保留 49 份 staged Java；本輪未修改 Java、父類可見性、src/main、pom.xml 或比較器。NativeDeInstanceState／NativeDeCharacterFields 相對前輪提交 `1bc050a` 無變更。
- main 合併時只有 `docs/combat-merge/deviations.txt`、`deviations.md` 衝突；解法為保留雙方既有內容（#72 五項 method-diff 與 #75 Eye 核准），未新增或刪除核准識別。本輪自行編輯的交付內容只有此報告；docs/REPORT.md 僅隨 main 合併帶入，未手動修改。
- 重新編譯 compile-stubs、已合併 semantic-diff 及全部 49 份 staged Java。staged 輸出使用全新空目錄；JAR 僅封裝這次 javac 的 185 個 class，逐 entry 核對 bytes 與 class 檔相同；未封裝 stub，未複製正式 class 或進行 ASM 後處理。
- 逐一檢視七份核准清單，依 A01 的精確 entry 裁切再傳入工具，避免清單外類別被擴大核准；沒有診斷停用或放寬旗標。

前輪已完成且保留：compute 在首個非 null 元素處明確拋出 NoSuchFieldError("SKU1")、移除五個假空表欄位、四個 Name 建構子委派及五項 method-diff、private 建構子／construction 的既有核准、巢狀 lambda 使用 this。來源與核准歷程、12 層父鏈／18 建構子及 426 個正式呼叫參照的舊稽核可由 [前輪報告](https://github.com/chaoren0418-arch/lito-launcher-server/blob/1bc050a95ba52e0090432f11a0920b8fb074f130/docs/reports/issue-72.md) 與其引用的 f4b20cb 追溯；本輪不把舊診斷當作新工具驗收。

## 已驗證

實際產生時間：`2026-11-05T20:35:26Z`。Microsoft JDK 17.0.17、Python 3.12、ASM 9.7；工具來源即上述 main 的 `tools/semantic-diff/src/main/java`，逐檔 SHA-256 見附錄。javac 只有 unchecked 提示，沒有編譯錯誤。

| 類別狀態 | 數量 |
|---|---:|
| SAME | 183 |
| DEVIATION | 2 |
| SAME_EFFECTS／SAME_PATHS／SAME_CFG／LAMBDA_PAIRED_BY_CALLSITE／VISIBILITY_BRIDGE | 各 0 |
| DIFF／ONLY_LEFT／ONLY_RIGHT／SAME_LAYOUT／ORPHAN_IGNORED | 各 0 |

兩個 DEVIATION 為 `NativeDeCharacterFields$CombatSkillDerived`（compute）及 `NativeDeInstanceState`（既有建構子核准）。其餘方法、欄位與 metadata 均通過。

依 [主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6663641218)：

- **169 個 bridge** 全由 #203 的嚴格可見性 bridge 規則認定，逐方法結果列 VISIBILITY_BRIDGE；不是新增方法豁免。包含 `get_showId()I`，仍使用既有的精確 access-widen 核准。
- **巢狀 lambda**：`lambda$sourceAi$scheduleAiTick$22(JJZ)V` 對上 `lambda$scheduleAiTick$0(JJZ)V`；逐方法為 LAMBDA_PAIRED_BY_CALLSITE，detail 為 `MethodResult[status=SAME, detail=linear/normalized body equal]`。新工具經呼叫點配對後完整正規化內容相同，本輪不再僅引用先前 3 條路徑診斷。
- classSelection 精確為 `{"missingLeft": [], "extraLeft": []}`；左 JAR 185 個 class、右正式 JAR 2,554 個 class，比較集合恰為 A01 的 185 項。

## 核准清單

| 清單 | A01 適用行數 |
|---|---:|
| effects-approved.txt | 0 |
| access-widen-approved.txt | 1 |
| deviations.txt | 11 |
| exceptions-exempt.txt | 0 |
| extra-methods-approved.txt | 0 |
| metadata-exempt.txt | 0 |
| layout-approved.txt | 0 |

deviations 的 11 行為五個 ctor-prologue-order、一個 private 建構子 left-only，以及五個 method-diff；access-widen 僅一行。其餘五份清單無 A01 適用項，依 CLI 不接受空清單的規則未傳空檔。完整實際核准如下：

```text
# access-widen-approved.txt
local/lito381/server/NativeDeDeletionState#get_showId()I # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/153
# deviations.txt
ctor-prologue-order local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935
ctor-prologue-order local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935
ctor-prologue-order local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935
ctor-prologue-order local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935
ctor-prologue-order local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V method-diff # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6657400173
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V method-diff # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6657400173
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V method-diff # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6657400173
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V method-diff # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6657400173
local/lito381/server/NativeDeCharacterFields$CombatSkillDerived#compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; method-diff # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6657400173
```

## 怎麼驗證

在 worktree 根目錄執行：

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-72/verify-203.py
```

完整再現程式見附錄 D；實際 argv／exit／log 收據為 `scratch/issue-72/verify-203/commands.json`。程式固定讀取 a-batches.json 的 A01，包含 javac --release 17 -encoding UTF-8、Windows 分號 classpath、全部核准清單篩選、JAR entry／bytes 檢查、semantic-diff 及最終驗收斷言。重新執行需使用新的輸出目錄，程式故意拒絕重用已有 class 目錄。

`stubs.log`：exit 0

```text
(無輸出)
```

`tool.log`：exit 0

```text
(無輸出)
```

`javac.log`：exit 0

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

`semantic.log`：exit 0

```text
SUMMARY SAME=183 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_CFG=0 LAMBDA_PAIRED_BY_CALLSITE=0 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=2 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=18 ALLOW_METHODS=0
wrote D:\lineage開發\3-lito開發\scratch\wt\task-72\scratch\issue-72\verify-203\result.json
wrote D:\lineage開發\3-lito開發\scratch\wt\task-72\scratch\issue-72\verify-203\result.md
```

## 未驗證／推測

- 未執行 staged 應用程式、單元測試、伺服器、登入器、遊戲或資料庫；本批依任務規定只做編譯與靜態比對。
- DEVIATION 是已核准交付差異，並非無條件執行期等價或 bug 已修好。compute 保留正式缺失欄位的錯誤路徑，根因仍追蹤 #184。
- 本輪不對實際遊戲行為作新增推測。建構子的父鏈可觀測性與初始化／例外順序須由 #78 的實際測試確認。

## 產出路徑／給下一棒的提示

- 交付：`docs/reports/issue-72.md`，及 PR #99 原有 49 份 `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/*.java`、既有 deviations 核准紀錄。整份 PR 相對 main 為 52 檔（49 Java、此報告、兩份 deviations），不含建置產物。
- 本機證據：`scratch/issue-72/verify-203.py` 與 `scratch/issue-72/verify-203/{commands.json,approvals.json,result.json,result.md,sha256.json,*.log}`；直接 javac 產物為其 `target/classes/`，封裝為 `target/staged.jar`，工具類別為 `target/tool/`。均不進 git。
- **Claude 審查**：本批 185 entries 已通過，不需重開 bridge／lambda 裁定或加 needs:claude。核對本報告的最新結果及 PR #99；PR 描述本輪同步更新，先前 gh 401 阻擋改由可用 GitHub 連接工具處理。
- **#78 A-land**：各 A 批審查完成後再落地；測試 construction 父鏈觀測、四個 Name overload 的求值／例外／旗標順序，以及 compute 空集合／全 null／非 null 元素行為。本批沒有代替 A-land 的整合或回歸驗收。

## 附錄 A：185 個 entry 完整結果

```text
SAME local/lito381/server/NativeDeActorState.class
SAME local/lito381/server/NativeDeActorState$1.class
SAME local/lito381/server/NativeDeActorState$Effect.class
SAME local/lito381/server/NativeDeActorState$Lease.class
SAME local/lito381/server/NativeDeActorState$Profile.class
SAME local/lito381/server/NativeDeActorState$Snapshot.class
SAME local/lito381/server/NativeDeActorState$Transition.class
SAME local/lito381/server/NativeDeAiState.class
SAME local/lito381/server/NativeDeAiState$1.class
SAME local/lito381/server/NativeDeAiState$2.class
SAME local/lito381/server/NativeDeAiState$Effect.class
SAME local/lito381/server/NativeDeAiState$Facts.class
SAME local/lito381/server/NativeDeAiState$Holder.class
SAME local/lito381/server/NativeDeAiState$Kind.class
SAME local/lito381/server/NativeDeAiState$Lease.class
SAME local/lito381/server/NativeDeAiState$Op.class
SAME local/lito381/server/NativeDeAiState$Request.class
SAME local/lito381/server/NativeDeAiState$Result.class
SAME local/lito381/server/NativeDeAiState$View.class
SAME local/lito381/server/NativeDeAiTimeline.class
SAME local/lito381/server/NativeDeAiTimeline$Port.class
SAME local/lito381/server/NativeDeAiTimeline$State.class
SAME local/lito381/server/NativeDeAiTimerProtocol.class
SAME local/lito381/server/NativeDeAiTimerProtocol$Failure.class
SAME local/lito381/server/NativeDeAiTimerProtocol$Port.class
SAME local/lito381/server/NativeDeAiTimers.class
SAME local/lito381/server/NativeDeAiTimers$1.class
SAME local/lito381/server/NativeDeAiTimers$2.class
SAME local/lito381/server/NativeDeAiTimers$Credit.class
SAME local/lito381/server/NativeDeAiTimers$Entry.class
SAME local/lito381/server/NativeDeAiTimers$Live.class
SAME local/lito381/server/NativeDeAiTimers$Registry.class
SAME local/lito381/server/NativeDeAiTimers$Snapshot.class
SAME local/lito381/server/NativeDeAiTimers$Work.class
SAME local/lito381/server/NativeDeAiWorkerProtocol.class
SAME local/lito381/server/NativeDeAiWorkerProtocol$Port.class
SAME local/lito381/server/NativeDeAiWorkers.class
SAME local/lito381/server/NativeDeAiWorkers$1.class
SAME local/lito381/server/NativeDeAiWorkers$Snapshot.class
SAME local/lito381/server/NativeDeAiWorkers$Task.class
SAME local/lito381/server/NativeDeAiWorkers$Ticket.class
SAME local/lito381/server/NativeDeCachedFactoryServices.class
SAME local/lito381/server/NativeDeCharacterActor.class
SAME local/lito381/server/NativeDeCharacterControls.class
SAME local/lito381/server/NativeDeCharacterFields.class
DEVIATION local/lito381/server/NativeDeCharacterFields$CombatSkillDerived.class
SAME local/lito381/server/NativeDeCharacterFields$CurrentHpMutation.class
SAME local/lito381/server/NativeDeCharacterFields$CurrentMpMutation.class
SAME local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt.class
SAME local/lito381/server/NativeDeCharacterFields$ErrorLog.class
SAME local/lito381/server/NativeDeCharacterFields$MpDebitMutation.class
SAME local/lito381/server/NativeDeCharacterFields$SkillEffectSnapshot.class
SAME local/lito381/server/NativeDeCharacterFields$Snapshot.class
SAME local/lito381/server/NativeDeCharacterFields$ViewerHpState.class
SAME local/lito381/server/NativeDeChatBroadcast.class
SAME local/lito381/server/NativeDeChatChat.class
SAME local/lito381/server/NativeDeChatChatGlobal.class
SAME local/lito381/server/NativeDeChatChatShouting.class
SAME local/lito381/server/NativeDeChatJobs.class
SAME local/lito381/server/NativeDeChatJobs$Owner.class
SAME local/lito381/server/NativeDeChatJobs$Ticket.class
SAME local/lito381/server/NativeDeChatResources.class
SAME local/lito381/server/NativeDeChatResources$Snapshot.class
SAME local/lito381/server/NativeDeChatScope.class
SAME local/lito381/server/NativeDeChatScope$Clock.class
SAME local/lito381/server/NativeDeChatScope$Data.class
SAME local/lito381/server/NativeDeChatScope$Logs.class
SAME local/lito381/server/NativeDeChatScope$Pool.class
SAME local/lito381/server/NativeDeChatScope$Pool$ShutdownDiscardable.class
SAME local/lito381/server/NativeDeChatTable.class
SAME local/lito381/server/NativeDeChatTimer.class
SAME local/lito381/server/NativeDeChatTimer$RunSequenceTask.class
SAME local/lito381/server/NativeDeChatWire.class
SAME local/lito381/server/NativeDeClientViewer.class
SAME local/lito381/server/NativeDeControlCatalog.class
SAME local/lito381/server/NativeDeControlCatalog$Abilities.class
SAME local/lito381/server/NativeDeControlCatalog$Catalog.class
SAME local/lito381/server/NativeDeControlCatalog$Definition.class
SAME local/lito381/server/NativeDeControlCatalog$Names.class
SAME local/lito381/server/NativeDeControlCatalog$Reporter.class
SAME local/lito381/server/NativeDeControlCatalog$Settings.class
SAME local/lito381/server/NativeDeCorpseCore.class
SAME local/lito381/server/NativeDeCorpseCore$Npc.class
SAME local/lito381/server/NativeDeCorpseCore$Port.class
SAME local/lito381/server/NativeDeCorpseCore$Snapshot.class
SAME local/lito381/server/NativeDeCorpseCore$Task.class
SAME local/lito381/server/NativeDeCorpseTimers.class
SAME local/lito381/server/NativeDeCorpseTimers$Deletion.class
SAME local/lito381/server/NativeDeCorpseTimers$Job.class
SAME local/lito381/server/NativeDeCorpseTimers$Owner.class
SAME local/lito381/server/NativeDeCorpseTimers$Owner$1.class
SAME local/lito381/server/NativeDeCorpseTimers$Snapshot.class
SAME local/lito381/server/NativeDeCorpseTimers$Ticket.class
SAME local/lito381/server/NativeDeDeletionDeState.class
SAME local/lito381/server/NativeDeDeletionOwner.class
SAME local/lito381/server/NativeDeDeletionPorts.class
SAME local/lito381/server/NativeDeDeletionPorts$Boss.class
SAME local/lito381/server/NativeDeDeletionPorts$DeWorld.class
SAME local/lito381/server/NativeDeDeletionPorts$DeadTimer.class
SAME local/lito381/server/NativeDeDeletionPorts$DeleteTimer.class
SAME local/lito381/server/NativeDeDeletionPorts$DigestTimer.class
SAME local/lito381/server/NativeDeDeletionPorts$Doll.class
SAME local/lito381/server/NativeDeDeletionPorts$Hate.class
SAME local/lito381/server/NativeDeDeletionPorts$HprTimer.class
SAME local/lito381/server/NativeDeDeletionPorts$Illusory.class
SAME local/lito381/server/NativeDeDeletionPorts$Inventory.class
SAME local/lito381/server/NativeDeDeletionPorts$Log.class
SAME local/lito381/server/NativeDeDeletionPorts$MobGroup.class
SAME local/lito381/server/NativeDeDeletionPorts$Monster.class
SAME local/lito381/server/NativeDeDeletionPorts$MprTimer.class
SAME local/lito381/server/NativeDeDeletionPorts$ObjectRef.class
SAME local/lito381/server/NativeDeDeletionPorts$OtherList.class
SAME local/lito381/server/NativeDeDeletionPorts$Packet.class
SAME local/lito381/server/NativeDeDeletionPorts$Quest.class
SAME local/lito381/server/NativeDeDeletionPorts$RemoveObject.class
SAME local/lito381/server/NativeDeDeletionPorts$RestTimer.class
SAME local/lito381/server/NativeDeDeletionPorts$Services.class
SAME local/lito381/server/NativeDeDeletionPorts$Spawn.class
SAME local/lito381/server/NativeDeDeletionPorts$Summon.class
SAME local/lito381/server/NativeDeDeletionPorts$Template.class
SAME local/lito381/server/NativeDeDeletionPorts$Viewer.class
SAME local/lito381/server/NativeDeDeletionPorts$WorkTimer.class
SAME local/lito381/server/NativeDeDeletionPorts$World.class
SAME local/lito381/server/NativeDeDeletionScope.class
SAME local/lito381/server/NativeDeDeletionState.class
SAME local/lito381/server/NativeDeDeletionVisibility.class
SAME local/lito381/server/NativeDeDeletionVisibility$1.class
SAME local/lito381/server/NativeDeDeletionVisibility$Marker.class
SAME local/lito381/server/NativeDeDeletionVisibility$Phase.class
SAME local/lito381/server/NativeDeDeletionVisibility$RepairKind.class
SAME local/lito381/server/NativeDeDeletionVisibility$Snapshot.class
SAME local/lito381/server/NativeDeEquipment.class
SAME local/lito381/server/NativeDeEquipment$1.class
SAME local/lito381/server/NativeDeEquipment$Holder.class
SAME local/lito381/server/NativeDeEquipment$Lease.class
SAME local/lito381/server/NativeDeEquipment$Op.class
SAME local/lito381/server/NativeDeEquipment$Request.class
SAME local/lito381/server/NativeDeEquipment$Template.class
SAME local/lito381/server/NativeDeEquipment$Values.class
SAME local/lito381/server/NativeDeEquipment$View.class
SAME local/lito381/server/NativeDeFactoryControls.class
SAME local/lito381/server/NativeDeFactoryControls$1.class
SAME local/lito381/server/NativeDeFactoryControls$Snapshot.class
SAME local/lito381/server/NativeDeInitializerKernel.class
SAME local/lito381/server/NativeDeInitializerKernel$Ability.class
SAME local/lito381/server/NativeDeInitializerKernel$Actor.class
SAME local/lito381/server/NativeDeInitializerKernel$Clan.class
SAME local/lito381/server/NativeDeInitializerKernel$Emblem.class
SAME local/lito381/server/NativeDeInitializerKernel$Log.class
SAME local/lito381/server/NativeDeInitializerKernel$Name.class
SAME local/lito381/server/NativeDeInitializerKernel$Packet.class
SAME local/lito381/server/NativeDeInitializerKernel$Pet.class
SAME local/lito381/server/NativeDeInitializerKernel$PetInfo.class
SAME local/lito381/server/NativeDeInitializerKernel$Services.class
SAME local/lito381/server/NativeDeInitializerKernel$Setting.class
SAME local/lito381/server/NativeDeInitializerKernel$Template.class
SAME local/lito381/server/NativeDeInstanceBroadcast.class
SAME local/lito381/server/NativeDeInstanceName.class
SAME local/lito381/server/NativeDeInstanceNames.class
SAME local/lito381/server/NativeDeInstanceNames$Services.class
SAME local/lito381/server/NativeDeInstanceScene.class
SAME local/lito381/server/NativeDeInstanceScene$Visual.class
SAME local/lito381/server/NativeDeInstanceScope.class
SAME local/lito381/server/NativeDeInstanceScope$World.class
DEVIATION local/lito381/server/NativeDeInstanceState.class
SAME local/lito381/server/NativeDeInstanceState$DeAiWorkerTask.class
SAME local/lito381/server/NativeDeInstanceState$DeRepathThrottle.class
SAME local/lito381/server/NativeDeItemAbility.class
SAME local/lito381/server/NativeDeItemAbility$AbilityType.class
SAME local/lito381/server/NativeDeItemApplication.class
SAME local/lito381/server/NativeDeItemApplication$Owner.class
SAME local/lito381/server/NativeDeItemBlessStatsTable.class
SAME local/lito381/server/NativeDeItemCommonEnchantTable.class
SAME local/lito381/server/NativeDeItemDefinitionsResources.class
SAME local/lito381/server/NativeDeItemDefinitionsResources$Owner.class
SAME local/lito381/server/NativeDeItemDefinitionsResources$Snapshot.class
SAME local/lito381/server/NativeDeItemDefinitionsScope.class
SAME local/lito381/server/NativeDeItemDefinitionsScope$Context.class
SAME local/lito381/server/NativeDeItemDefinitionsScope$Data.class
SAME local/lito381/server/NativeDeItemDefinitionsScope$Database.class
SAME local/lito381/server/NativeDeItemDefinitionsScope$Logs.class
SAME local/lito381/server/NativeDeItemDefinitionsScope$TableLog.class
SAME local/lito381/server/NativeDeItemEnchantBonusTable.class
SAME local/lito381/server/NativeDeItemEquipProc.class
SAME local/lito381/server/NativeDeItemEquipProcTable.class
```

## 附錄 B：逐方法證據與完整核准差異

以下直接取自本輪 result.json 的 methodResults，保留方法名稱、狀態及完整 detail（含五項 method-diff 的左右正規化內容），未手動改判。

### local/lito381/server/NativeDeCharacterFields$CombatSkillDerived

```json
[
  {
    "method": "compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;",
    "status": "DEVIATION",
    "detail": "method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; insn[17]: left=IFEQ L30 right=IFEQ L93\nLEFT\n(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;\n0x9\n(Ljava/lang/Iterable<Ljava/lang/Integer;>;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[CONST Integer:0, ISTORE 1, CONST Integer:0, ISTORE 2, CONST Integer:0, ISTORE 3, CONST Integer:0, ISTORE 4, CONST Integer:0, ISTORE 5, CONST Integer:0, ISTORE 6, ALOAD 0, INVOKEINTERFACE java/lang/Iterable.iterator()Ljava/util/Iterator; itf, ASTORE 7, ALOAD 7, INVOKEINTERFACE java/util/Iterator.hasNext()Z itf, IFEQ L30, ALOAD 7, INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf, CHECKCAST java/lang/Integer, ASTORE 8, ALOAD 8, IFNONNULL L25, GOTO L15, NEW java/lang/NoSuchFieldError, DUP, CONST str:\"SKU1\", INVOKESPECIAL java/lang/NoSuchFieldError.<init>(Ljava/lang/String;)V, ATHROW, NEW local/lito381/server/NativeDeCharacterFields$CombatSkillDerived, DUP, ILOAD 1, ILOAD 2, ILOAD 3, ILOAD 4, ILOAD 5, ILOAD 6, INVOKESPECIAL local/lito381/server/NativeDeCharacterFields$CombatSkillDerived.<init>(IIIIIZ)V, ARETURN]\nRIGHT\n(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;\n0x9\n(Ljava/lang/Iterable<Ljava/lang/Integer;>;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[CONST Integer:0, ISTORE 1, CONST Integer:0, ISTORE 2, CONST Integer:0, ISTORE 3, CONST Integer:0, ISTORE 4, CONST Integer:0, ISTORE 5, CONST Integer:0, ISTORE 6, ALOAD 0, INVOKEINTERFACE java/lang/Iterable.iterator()Ljava/util/Iterator; itf, ASTORE 7, ALOAD 7, INVOKEINTERFACE java/util/Iterator.hasNext()Z itf, IFEQ L93, ALOAD 7, INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf, CHECKCAST java/lang/Integer, ASTORE 8, ALOAD 8, IFNONNULL L25, GOTO L15, GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKU1:Ljava/util/HashMap;, ALOAD 8, INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, ASTORE 9, ALOAD 9, IFNULL L37, ILOAD 1, ALOAD 9, INVOKEVIRTUAL java/lang/Integer.intValue()I, IADD, ISTORE 1, GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKU2:Ljava/util/HashMap;, ALOAD 8, INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, ASTORE 10, ALOAD 10, IFNULL L49, ILOAD 2, ALOAD 10, INVOKEVIRTUAL java/lang/Integer.intValue()I, IADD, ISTORE 2, GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKD1:Ljava/util/HashMap;, ALOAD 8, INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, ASTORE 11, ALOAD 11, IFNULL L61, ILOAD 3, ALOAD 11, INVOKEVIRTUAL java/lang/Integer.intValue()I, IADD, ISTORE 3, GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKD2:Ljava/util/HashMap;, ALOAD 8, INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, ASTORE 12, ALOAD 12, IFNULL L73, ILOAD 4, ALOAD 12, INVOKEVIRTUAL java/lang/Integer.intValue()I, IADD, ISTORE 4, GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKD3:Ljava/util/HashMap;, ALOAD 8, INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, ASTORE 13, ALOAD 13, IFNULL L92, ALOAD 13, ALOAD 8, INVOKEVIRTUAL java/lang/Integer.equals(Ljava/lang/Object;)Z, IFEQ L87, CONST Integer:1, ISTORE 6, GOTO L92, ILOAD 5, ALOAD 13, INVOKEVIRTUAL java/lang/Integer.intValue()I, IADD, ISTORE 5, GOTO L15, NEW local/lito381/server/NativeDeCharacterFields$CombatSkillDerived, DUP, ILOAD 1, ILOAD 2, ILOAD 3, ILOAD 4, ILOAD 5, ILOAD 6, INVOKESPECIAL local/lito381/server/NativeDeCharacterFields$CombatSkillDerived.<init>(IIIIIZ)V, ARETURN]"
  }
]
```

### local/lito381/server/NativeDeInstanceState

```json
[
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V",
    "status": "DEVIATION",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V insn[0]: left=ALOAD 0 right=ALOAD 1\nLEFT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 0, ALOAD 1, ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]\nRIGHT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ASTORE 3, ALOAD 0, ALOAD 1, PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;, ALOAD 0, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, NEW java/lang/Object, DUP, INVOKESPECIAL java/lang/Object.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z, ALOAD 0, INVOKESTATIC java/lang/System.currentTimeMillis()J, PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J, ALOAD 0, NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle, DUP, INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J, ALOAD 0, NEW java/util/concurrent/atomic/AtomicLong, DUP, INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;, ALOAD 0, ALOAD 3, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]"
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V",
    "status": "DEVIATION",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V insn[0]: left=ALOAD 0 right=ALOAD 1\nLEFT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 0, ALOAD 1, ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, ILOAD 4, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]\nRIGHT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ASTORE 3, ALOAD 0, ALOAD 1, PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;, ALOAD 0, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, NEW java/lang/Object, DUP, INVOKESPECIAL java/lang/Object.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z, ALOAD 0, INVOKESTATIC java/lang/System.currentTimeMillis()J, PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J, ALOAD 0, NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle, DUP, INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J, ALOAD 0, NEW java/util/concurrent/atomic/AtomicLong, DUP, INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;, ALOAD 0, ALOAD 3, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, ILOAD 4, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]"
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V",
    "status": "DEVIATION",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V insn[0]: left=ALOAD 0 right=ALOAD 1\nLEFT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 0, ALOAD 1, ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, ILOAD 4, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, ALOAD 5, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]\nRIGHT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ASTORE 3, ALOAD 0, ALOAD 1, PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;, ALOAD 0, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, NEW java/lang/Object, DUP, INVOKESPECIAL java/lang/Object.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z, ALOAD 0, INVOKESTATIC java/lang/System.currentTimeMillis()J, PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J, ALOAD 0, NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle, DUP, INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J, ALOAD 0, NEW java/util/concurrent/atomic/AtomicLong, DUP, INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;, ALOAD 0, ALOAD 3, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, ILOAD 4, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, ALOAD 5, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]"
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V",
    "status": "DEVIATION",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V insn[0]: left=ALOAD 0 right=ALOAD 1\nLEFT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 0, ALOAD 1, ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, ILOAD 4, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, ILOAD 5, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, ALOAD 6, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]\nRIGHT\n(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 1, ALOAD 3, INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;, ASTORE 3, ALOAD 0, ALOAD 1, PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;, ALOAD 0, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;, ALOAD 2, INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V, ALOAD 0, NEW java/lang/Object, DUP, INVOKESPECIAL java/lang/Object.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z, ALOAD 0, INVOKESTATIC java/lang/System.currentTimeMillis()J, PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J, ALOAD 0, NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle, DUP, INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, CONST null, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I, ALOAD 0, CONST Integer:0, PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I, ALOAD 0, CONST Long:0, PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J, ALOAD 0, NEW java/util/concurrent/atomic/AtomicLong, DUP, INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V, PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;, ALOAD 0, ALOAD 3, PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;, ALOAD 0, ILOAD 4, PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z, ALOAD 0, ILOAD 5, PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z, ALOAD 0, ALOAD 6, PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;, ALOAD 0, INVOKESPECIAL local/lito381/server/NativeDeInstanceState.startNpc()V, RETURN]"
  },
  {
    "method": "addAc(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addBowDmgup(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addBowHitup(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addCha(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addCon(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addDex(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addDmgup(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addEarth(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addFire(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addHitup(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addInt(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addMaxHp(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addMaxMp(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addMr(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addRegistBlind(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addRegistSleep(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addRegistStone(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addRegistStun(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addRegistSustain(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addSp(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addStr(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addStunSuccess(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addWater(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addWeaponBreakStack()V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addWind(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "addWis(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "add_regist_freeze(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "claimExpiredCorpseDeletion(J)Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "destroyed()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "forward(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getAc()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getAddAttrKind()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getAtkspeed()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getBowDmgup()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getBowHitup()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getBraveSpeed()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getCha()S",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getChaLightSize()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getCon()S",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getCurrentHp()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getCurrentMp()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getDex()S",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getDmgup()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getDolls()Ljava/util/Map;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getEarth()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getExp()J",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getFire()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getGfxId()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getHeading()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getHitup()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getId()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getInt()S",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getKarma()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getKnownObjects()Ljava/util/List;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getKnownPlayers()Ljava/util/List;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getLawful()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getLevel()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getLightSize()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getLocation()Llocal/lito381/server/NativeDeWorldLocation;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getMap()Llocal/lito381/server/NativeDeWorldMap;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getMaxHp()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getMaxMp()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getMoveSpeed()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getMr()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getNameId()Ljava/lang/String;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getOwnLightSize()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getPassispeed()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getPetList()Ljava/util/Map;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getRegistBlind()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getRegistFreeze()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getRegistSleep()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getRegistStone()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getRegistStun()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getRegistSustain()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getSp()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getStatus()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getStr()S",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getStunSuccess()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getTempCharGfx()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getTempLawful()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getTitle()Ljava/lang/String;",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getTrueMr()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getTrueSp()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getWater()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getWeaponBreakStack()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getWind()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "getWis()S",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "get_deadTimerTemp()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "get_fishX()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "get_fishY()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "get_showId()I",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "hasSkillEffect(I)Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isAgro()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isAgrocoi()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isAgrososc()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isAiRunning()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isDead()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isFishing()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isInvisble()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isParalyzed()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isShop()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "isSleeped()Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "lambda$sourceAi$scheduleAiTick$22(JJZ)V",
    "status": "LAMBDA_PAIRED_BY_CALLSITE",
    "detail": "lambda$scheduleAiTick$0(JJZ)V; MethodResult[status=SAME, detail=linear/normalized body equal]"
  },
  {
    "method": "onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "rebindWorldMap(S)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "removeAllKnownObjects()V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "resurrect(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setAc(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setAddAttrKind(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setAgro(Z)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setAgrocoi(Z)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setAgrososc(Z)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setAtkspeed(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setBraveSpeed(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setCha(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setChaLightSize(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setCon(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setDex(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setExp(J)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setGfxId(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setGfxidInStatus(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setHeading(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setId(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setInt(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setKarma(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setLawful(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setLevel(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setLightSize(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setLocation(III)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setMap(Llocal/lito381/server/NativeDeWorldMap;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setMap(S)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setMaxHp(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setMaxMp(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setMoveSpeed(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setMr(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setName(Ljava/lang/String;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setNameId(Ljava/lang/String;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setOwnLightSize(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setPassispeed(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setPickupItem(Z)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setStatus(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setStr(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setTempCharGfx(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setTempLawful(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setTitle(Ljava/lang/String;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setWeaponBreakStack(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setWis(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setX(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setXY(II)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setY(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "set_deadTimerTemp(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "set_showId(I)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "sourceDeleteDe()V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "start_shop()V",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  },
  {
    "method": "tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z",
    "status": "VISIBILITY_BRIDGE",
    "detail": "javac bridge to inherited public method declared by non-public ancestor"
  }
]
```

## 附錄 C：SHA-256

包含全部 49 份來源、比較工具、ASM、清單及輸出。

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeActorState.java` | `b87bc641c07097631f2893a0ab08d9c8723a478bd5adc38a97b8a18e25ed21b3` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeAiState.java` | `d6d65bc23435cc72a23503896a91b41f9abbb10ae9b553b59fe241757004f272` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeAiTimeline.java` | `83302cb273762b5d26b69250b6e74ba97babc5195ede615d84c286d29480e279` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeAiTimerProtocol.java` | `71070396be711549636ef4a8c970005c46bf6be5dcf719795abe875eb7475f26` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeAiTimers.java` | `20265ddf2f93a76ce646ce920d215bdabb307edccc64512e035f880076ef25ea` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeAiWorkerProtocol.java` | `6c780f62c2c40981cdea805e693030157dedc220a874f2dadc1818ae254f1400` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeAiWorkers.java` | `25ed9a1edb48ed34722134152651c8a029ba3511e3cb8a870180021b5c21e6d1` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeCachedFactoryServices.java` | `14cc3d85bd786bb7b962695dc0b872fb296dc28b7a99d45e9326736ff988f045` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeCharacterActor.java` | `a89af199a6c5d4eeedd254ce3ad91e95e192c2167914a8213c8db65af6512812` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeCharacterControls.java` | `19bff307f68d465d00bc05c894164231c5e5c9304be2c6cdb3ed2e291dd81b9d` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeCharacterFields.java` | `7be81c55e1b68e1895675d5dd23078ce78460e1a8191909d437085863d3c0c6e` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatBroadcast.java` | `5c35eb1ba41a4d70b214f2a44088091bb31ea3f79724dfce909e39cdcc268793` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatChat.java` | `f5c33e8732f35333fd24edaa79d50262da36652b9ff5051606d7f227e5a42114` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatChatGlobal.java` | `545034d96a62eeae48108af76d0223c569a4cd48cc74548d212aada4f31828cf` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatChatShouting.java` | `0a09f6980ef3a4f5ce238f8242cbdfbd663cae1c6089d72c91c0c08f3e80ace9` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatJobs.java` | `099871d126aed3b8947da559812679b8cb6119c3d476bbe0f16e81d31ff3bcb5` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatResources.java` | `1763b8758ca9da91d96182ee2c23a3787d6a0f7ac7f66b1f5d4d473a0d9bd5a2` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatScope.java` | `21291a988914be016750f350f014acc35943e2970d136e6f1d7ef4abb05cb7fd` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatTable.java` | `f222726adb4a08845d44e8f351cfc924b2eeae5712f51c7f56185a1a42223990` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatTimer.java` | `9bf7a2e8afeae71c71f18b1308fb2d79363f2768541c442fadb2ac3f39d8f945` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeChatWire.java` | `5c5d969eb7400ed5a793e87a34b71d4791d4daae4cdf2de4270d1d1cf07f6254` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeClientViewer.java` | `71ba9f3973802104b27c58d6d1fc365d0420810eb0ef17fe6fb6def2a343a48c` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeControlCatalog.java` | `56b343e52cf95748104d972bdcb0f2322f3b6f6606999f1b90c0c010e9f0a2c7` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeCorpseCore.java` | `d1a54c031264d3a579983fc559c65ed70b3cbaac7d4b1646f3eae6a70b7a6774` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeCorpseTimers.java` | `ce8fa4542c28b6e6ef77bf2366a7d967cb96a81b39810f6c556dcc10d7acc1fc` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeDeletionDeState.java` | `f37af510a1752b170334246cee987725a015ed234ccaf8fcb4e542211ab85b59` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeDeletionOwner.java` | `c60a3345429a3733312ae97ceb5a8a98ea93f57a8c4c26dbd51d32f19f413a33` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeDeletionPorts.java` | `2f6759433142b4e8e4c31066795a29bf986673d42c475a2e451de65d0d70cd4d` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeDeletionScope.java` | `6ab3e11741ef59a75b6eae055b323f90ece2e348acdb51818c9ed33e8ba9fa59` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeDeletionState.java` | `a25e1826b5ba3623caf33b275f5f3a096929fba27138dd48bef2eff05596e0be` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeDeletionVisibility.java` | `d177cc5f5f55c591667f25470aaf733419040a6914ea876ca76574200c8ad862` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeEquipment.java` | `04d8d3ed0a767ac4dacd77f5a9db6e0333285c1f36c03f75848492d521a47d7f` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeFactoryControls.java` | `548c1daf3e83f6ff9afa024c4aa336885526d46692ebd128d1c5164f1870413c` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeInitializerKernel.java` | `074c6346062bd3da289a4bf3e29cf14db5e652b8ba656f065faee1d817247037` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeInstanceBroadcast.java` | `5bea4bbe7d006163f825a0a5f274f16695dfa50f71830bf184d8829247845857` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeInstanceName.java` | `522d1faeb8e42a902a2c60da7644686e055a9244ed8c0bf4114626b1c6cd368e` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeInstanceNames.java` | `8ec0d51122b770d5f993afa1bf5386576a26dcadd2cd42fbdeafcf7185a673d5` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeInstanceScene.java` | `c114821c074272a92fb38cadbdf9900b0e2d38d95d80be0134df8cee90b6b9c5` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeInstanceScope.java` | `7313466ba9fdf795d299bf876054d61a5dd7e23bb0a9f4d5e6b14acb7a93d5a1` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeInstanceState.java` | `06dd9d3ca628841b08c3af2e65c0e06b1a257cb7b7a88ed7d0b2b77eaa18537c` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemAbility.java` | `95318699a6a1ec5138273e1b5f432a6259871642985ccb188e49c8059404deb4` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemApplication.java` | `f82186cf0316af2d34b93a8556a41c463c7b76765bb0d8ccf9dc95f74fddf365` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemBlessStatsTable.java` | `e5993245084b5a5518657f0d21cc63f9114983530c9fa1802c340f84a2a823be` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemCommonEnchantTable.java` | `9eba99cc5d4e30d45369a93fdb7a2b1934e610644b42cb25e780f0b4f64b417b` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemDefinitionsResources.java` | `a7ccc5ac23960ff68816420d10f49276755251187e7113ea0c8b3095c5e0cb2d` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemDefinitionsScope.java` | `61ad5c556d00de47611fd175d512f5727033f57fef8a20be2e860a9361d05b90` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemEnchantBonusTable.java` | `5de9298c229c0061471dc9bcbb4e89fc5197d7b5e1f7d0bf2e87cfb7500d4a74` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemEquipProc.java` | `a51514e6fa80a01eeb7f07695bb15b0f05491701afeaed653dc007a86ffa5008` |
| `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemEquipProcTable.java` | `61f16509797bd94002d54aff5b39f62c44b73389aef81fbafe5d73ac78325ceb` |
| `D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar` | `adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d` |
| `D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar` | `62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1` |
| `scratch/issue-72/target/dependency/asm-analysis-9.7.jar` | `7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f` |
| `scratch/issue-72/verify-203/target/staged.jar` | `63e970a0ed1bd26f94586b79df3de840e6231b46977ce63215d5427e55363808` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `scratch/issue-72/verify-203/verify_entries.txt` | `66a1fc3e810da7480488c8f78f741b2c963f3f4bae9b02a645af9f0add86de21` |
| `scratch/issue-72/verify-203.py` | `7866457aafcb3bf598a47c19d051adc4bc42451717555968543b738e5dbfef47` |
| `docs/combat-merge/a-batches.json` | `ac1c17ec1a446564c6fb912235ecd9222d06bef1a3a91cdcb3836fda158ad65d` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/BlockExpressions.java` | `72e6185eb75243175226975f6afebcebb506ff9665a2494d470a863973eb1858` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/CfgComparison.java` | `d7659b1e43daadbfe9a97d3769bc5737e71fd3fbac447edb2c331c1aca64160e` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/CfgTransport.java` | `d93333fe2ad4e661309d05ce2d119c3168ae18dbd046f9073c01f171801d7537` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/ClassComparer.java` | `ccd7c6ad21f6b9dafc84f5291d579c967c252e86440f2d2a08ffc467e5e6f970` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/FourthNormalizations.java` | `f2994f27816d427575b1d65d7e33502dd0006b49938a46d5e1633396e3e0a0ce` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/JarIndex.java` | `38d05275799fc0dcad67bd87881c37f786db8469b0ce78d6756592530aa25f7e` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/LambdaCallsites.java` | `2e35c92fc54eca659dcf99c4afc2e60f7b3cfd8a3591b9c5b506fe736fa33888` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/LayoutComparison.java` | `c3c31be673b6088eacb2fcbb4da71d6523951be2bcc9d2e7c2325778178c6078` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/LocalTransport.java` | `bd98cfc3303dbb170595bc63143c5be95c3483b720a912861708c5448d4643d8` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/LocalWebs.java` | `564d8dbc0256890d64520c46815bfa00b5c93075b9e65052121488bdcf9fd608` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/NonNullChecks.java` | `2dbd1a43a1c2324dda5af4d3de510f8472daf880676892bf2788ff106578d923` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/Normalizations.java` | `6094fa299e2b17538a6a7050464e624ffec75c7cecf8566d667fc11058e79dec` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/PathComparison.java` | `a36626687f3177ef1fe38ce5aa68d5ecf9f27927fc3fca0788add0098d1162ad` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/RethrowHandlers.java` | `29f7f291cd6bf5cecfe77653fc3267e80decea857d3ea782878802727a37215e` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/SemanticDiff.java` | `4af7f887ac4bdce2957838a113a703ada405e15d1a08674eabfc05a8fe879017` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/TransportComparison.java` | `fa05cf2ea262c8d6b23a2fcbdcf0fb1d76057519978e081e5433067679df4fa2` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/VisibilityBridge.java` | `d9d963d325097556e8c860705a9879703c123d9c00fe0ef3478238e43e260090` |
| `docs/combat-merge/effects-approved.txt` | `b48431e80e508f795e2dd9c26c316a335e2149f82bbb4269326f132e85953e58` |
| `docs/combat-merge/access-widen-approved.txt` | `4bf5066b4ba996472337b173330e9fb5306b4378847512d15fe510d9b666ac74` |
| `docs/combat-merge/deviations.txt` | `688df9cdf64013937e689ab3adc84215bebd4f90ef9221b5bd78509570ca95e5` |
| `docs/combat-merge/exceptions-exempt.txt` | `4bd6bd215abde490fcf222e19caaaa9c53019f3588dbc68d5d440fe738fecb4b` |
| `docs/combat-merge/extra-methods-approved.txt` | `8151304a004b559589a33f1fb003443e6a21ec8840e247ce19276cf53220e48d` |
| `docs/combat-merge/metadata-exempt.txt` | `f6289ee43b323de1cc91ba7860ccc782e114d03391f6ab5f3a01c413be0fb456` |
| `docs/combat-merge/layout-approved.txt` | `280a45a28d8e636db43b94b21d67213c865b667bbb738c891422b7b459eb4c11` |
| `scratch/issue-72/verify-203/commands.json` | `5b18e0743098ab409f5d941fbcd20836c2143cb9d687629b97b5e493f31a67cb` |
| `scratch/issue-72/verify-203/approvals.json` | `b87ffe95a31f968720e2313c77abbbed8f3c97421c2b6f02cf3140053c9c9309` |
| `scratch/issue-72/verify-203/result.json` | `43b85278363105d7ff8548c013ea9a4f67e9da26f74622bd1f2e96bec56c8373` |
| `scratch/issue-72/verify-203/result.md` | `b0679a839efaaae66b0c43a0fe675f15f8a0360aa2fb6079ea1d7c82228817dd` |
| `scratch/issue-72/verify-203/semantic.log` | `41d831184fa568d9efea719acde027b85520cfa79426554ae2e008e153c33fcf` |
| `scratch/issue-72/verify-203/javac.log` | `373c917b895fa8f625556c0b4b359c0d4fcb2a78b8bd66390f8a0a0f001bfe3a` |
| `scratch/issue-72/verify-203/stubs.log` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
| `scratch/issue-72/verify-203/tool.log` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |

## 附錄 D：完整重現程式

還原為 `scratch/issue-72/verify-203.py`；依本機環境調整 JDK／Python 與 ASM 路徑，驗收規則保持不變。

```python
from pathlib import Path
import subprocess, json, zipfile, hashlib, collections

root = Path(__file__).resolve().parents[2]
s = root / 'lito伺服器/LiTo381-Server'
w = root / 'scratch/issue-72/verify-203'
w.mkdir(parents=True, exist_ok=True)
j = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
batch = next(b for b in json.loads((root/'docs/combat-merge/a-batches.json').read_text(encoding='utf8'))['batches'] if b['id']=='A01')
st = s/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
sources = [st/(n+'.java') for n in batch['classes']]
expected = set(batch['verify_entries'])
assert len(sources)==49 and len(expected)==185
deps = [Path('D:/dev-cache/maven/repository/org/ow2/asm')/n/'9.7'/f'{n}-9.7.jar' for n in ['asm','asm-tree']]
deps += [root/'scratch/issue-72/target/dependency/asm-analysis-9.7.jar']
asm = ';'.join(map(str,deps))
tc = w/'target/tool'
out = w/'target/classes'
assert not out.exists(), 'Use a fresh output directory; do not reuse compiled classes'
out.mkdir(parents=True)
commands = []
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args, name):
    args = list(map(str,args))
    r = subprocess.run(args,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (w/name).write_bytes(r.stdout)
    commands.append({'argv':args,'exit':r.returncode,'log':name})
    (w/'commands.json').write_text(json.dumps(commands,ensure_ascii=False,indent=2),encoding='utf8')
    print(name,'exit',r.returncode,flush=True)
    assert r.returncode==0, r.stdout
run([j/'javac.exe','--release','17','-encoding','UTF-8','-d',s/'target/legacy-stubs',*sorted((s/'compile-stubs').rglob('*.java'))],'stubs.log')
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',asm,'-d',tc,*sorted((s/'tools/semantic-diff/src/main/java').rglob('*.java'))],'tool.log')
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',f'{s}/reference/base-0.6.71.jar;{s}/target/legacy-stubs;{s}/lib/*','-d',out,*sources],'javac.log')
jar = w/'target/staged.jar'
with zipfile.ZipFile(jar,'w') as z:
    for p in sorted(out.rglob('*.class')):
        z.writestr(zipfile.ZipInfo(p.relative_to(out).as_posix(),(2026,1,1,0,0,0)),p.read_bytes())
with zipfile.ZipFile(jar) as z:
    assert set(z.namelist())==expected
    assert all(z.read(n)==(out/n).read_bytes() for n in expected)
assert not any(n.startswith('com/lineage/') for n in expected)
entries = w/'verify_entries.txt'
entries.write_text('\n'.join(batch['verify_entries'])+'\n',encoding='utf8')
flags = []
selection = {}
for flag,file in [('effects-methods','effects-approved.txt'),('access-widen','access-widen-approved.txt'),('deviations','deviations.txt'),('exceptions-exempt','exceptions-exempt.txt'),('extra-methods','extra-methods-approved.txt'),('metadata-exempt','metadata-exempt.txt'),('layout-methods','layout-approved.txt')]:
    lines = []
    for line in (root/'docs/combat-merge'/file).read_text(encoding='utf-8-sig').splitlines():
        key = line.split(' #')[0].strip()
        if not key or key.startswith('#'): continue
        owner = next((v for v in key.split() if '#' in v),key.split()[0]).split('#')[0]
        if owner.removesuffix('.class')+'.class' in expected: lines.append(line)
    selection[file] = lines
    if lines:
        path = w/file
        path.write_text('\n'.join(lines)+'\n',encoding='utf8')
        flags += ['--'+flag,path]
(w/'approvals.json').write_text(json.dumps(selection,ensure_ascii=False,indent=2),encoding='utf8')
run([j/'java.exe','-Dfile.encoding=UTF-8','-cp',f'{tc};{asm}','local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',s/'reference/base-0.6.71.jar','--left-classpath',s/'reference/base-0.6.71.jar','--classes',entries,*flags,'--json',w/'result.json','--md',w/'result.md'],'semantic.log')
r = json.loads((w/'result.json').read_text(encoding='utf8'))
assert r['classSelection']=={'missingLeft':[],'extraLeft':[]}
assert len(r['classes'])==185
accepted = {'SAME','SAME_EFFECTS','SAME_PATHS','SAME_CFG','DEVIATION','LAMBDA_PAIRED_BY_CALLSITE','VISIBILITY_BRIDGE'}
assert all(c['status'] in accepted for c in r['classes'])
files = sources+deps+[jar,s/'reference/base-0.6.71.jar',entries,Path(__file__),root/'docs/combat-merge/a-batches.json']
files += sorted((s/'tools/semantic-diff/src/main/java').rglob('*.java'))
files += [root/'docs/combat-merge'/name for name in selection]
files += [w/name for name in ['commands.json','approvals.json','result.json','result.md','semantic.log','javac.log','stubs.log','tool.log']]
(w/'sha256.json').write_text(json.dumps({str(p):sha(p) for p in files},ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(r['counts']))
print('proofMethodCounts',r.get('proofMethodCounts'))
print('approvals', {k:len(v) for k,v in selection.items()})
print('staged_sha256',sha(jar))
print('ACCEPTANCE PASS')
```
