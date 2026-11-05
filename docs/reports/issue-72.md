# Issue #72 — A01 正式 0.6.71 暫存（最新重驗）

## 結論

2026-11-06，延續審查版本 `task/72 @ 2ac8ba5`，合併 `origin/main @ 9678e97f4bff1dfab7cc820297e3ea6ff533375c`（merge commit `2633b68061ab2bb55deefbeade074a56ee9a9156`）。目前 main 含 #174、#176、#186；#186 已由 PR #202 合併（`bccc2cb`）。保留前輪技能欄位移除、五項方法偏離與巢狀 lambda 表達修正。

**183 SAME／1 DEVIATION／1 DIFF；185 個 entry，missingLeft=[]、extraLeft=[]。驗收未通過，不可 A-land 或部署。** 這是含 #186、尚未含 #203 的已合併工具全量重驗結果。剩餘 DIFF 為 NativeDeInstanceState 的 169 個可見性 bridge 及巢狀 lambda 配對。

依 [最新主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6663641218)，**裁定已完成，等待 #203 合併及全量重驗**：bridge 由 #203 第 3 項驗證，lambda 由第 1 項配對，兩者 Java 均不改，不改父類可見性、不新增整類豁免，也不再要求 needs:claude 裁定。2026-11-06 02:32（UTC+08:00）以 GitHub REST API 查證：#203 OPEN、PR #215 OPEN、`merged_at=null`（head `1f5900d7c7cb292e3e4cd724aeebb1379f74cbfe`），審查 #219 OPEN。依賴尚未滿足，本輪不能宣稱完成包含 #203 的最終驗收。

## 做了什麼

- `NativeDeCharacterFields.java`：移除 SKU1／SKU2／SKD1／SKD2／SKD3 五個空 HashMap 宣告（相應 clinit 初始化也消失）。`CombatSkillDerived.compute` 保留原六個區域值初始化、Iterable 迭代、null 元素 continue；原首個 GETSTATIC SKU1 改為 `throw new NoSuchFieldError("SKU1")`。其後四個欄位存取永遠不可達，沒有補表或引入假資料。外層類別已 SAME。
- `deviations.txt`／`deviations.md`：前輪僅追加已核准的 compute 與四個公開 Name 建構子 `method-diff`；保留既有 private 建構子及 ctor-prologue-order 清單。兩邊追加項在合併 main 的衝突中均保留，沒有移除其他批次核准。
- `NativeDeInstanceState.java`：巢狀排程外層 Runnable 使用 `this` 呼叫 LiveAiScope，內層 Supplier 明確捕獲 actor，產生與正式一致的 static 描述子。原先 35 個 adapter 的 Java 未改；它們已無差異回報。前輪只修這一處巢狀 lambda，本輪保持來源不變。
- 逐一核對 bridge 及一般方法描述子。**124 個非 synthetic、非建構子方法的描述子全與正式相同，沒有可修正的回傳／參數型別差異。169 個 bridge 的目標描述子也各與 bridge 完全相同，沒有 CHECKCAST／協變回傳適配。** 因此沒有為消掉 bridge 而擅改公開 API、父類可見性或 class bytes。

以上來源與核准項是前輪已完成內容，本輪沒有修改任何 staged Java 或精確核准清單；只更新本報告及 deviations.md 的驗證狀態。暫存稽核程式／證據位於 worktree 的 scratch/issue-72，建置產物位於其中 target/ 或既有工具 target/。未手動修改 src/main、pom.xml、docs/REPORT.md、比較工具與其他批次 Java；合併帶入的 main 變更照原樣保留。沒有從正式 JAR 複製 class 覆蓋編譯產物，也沒有 ASM 後處理。

## 已驗證：169 個 bridge

掃描 SHA-256 固定正式 JAR 的全部 **2,554 個 class**。Instance 是 public；169 個 bridge 均為 `0x1041 ACC_PUBLIC|ACC_BRIDGE|ACC_SYNTHETIC`，內容為依序 LOAD 參數、`INVOKESPECIAL NativeDeNpcConstructionState.<同名同描述子>`、對應 RETURN。正式 Instance 不宣告這 169 個方法；它們解析到非 public 祖先：CharacterFields 112、NpcTemplateState 20、ObjectFields 18、NpcVitalState 8、PerceptionState 8、CorpseState／DeletionState／DeletionDeState 各 1。NpcConstructionState 自身為 public，不能將問題簡化成直接父類非 public。

這符合 javac 為非公開祖先的繼承公開方法產生存取 bridge 的形狀，**不是泛型或協變描述子差異**。最新主控已確認這些 bridge 轉向同一實作，派送行為相同，差別在反射宣告方法清單；核准由 #203 的嚴格形狀與祖先解析規則驗證。本批保留 Java，等待已合併工具的驗收結果。

反向掃描的 receiver owner 包含 Instance、全部父類及遞迴實作介面。找到 **426 個參照**：352 invokevirtual、70 invokeinterface、3 invokespecial、1 method handle；91 個描述子有參照、78 個沒有。附錄逐項列出描述子、目標、正式宣告 owner、參照數，以及一個實際 javap 呼叫點；所有 426 筆另完整列出。

**證明界線**：「有呼叫端」指正式 class 中符號 receiver owner 可接收 Instance 的呼叫／handle，保守包含祖先型別及介面；不是已證明每個呼叫在執行時都持有 Instance。特別是父型別也可能持有其他子類。invokespecial 另列，並非虛擬派送。零筆只表示此固定 JAR 沒有相符的靜態參照，不排除反射、外部模組或未來呼叫；#203 的驗收條件依 bridge 形狀與解析結果，不依零參照數。

## 已驗證：巢狀 lambda

正式外層 `lambda$scheduleAiTick$0(JJZ)V` 為 instance，兩次 ALOAD 0：一次給 LiveAiScope.call 的 actor，一次供內層 Supplier 捕獲；**本體確實使用 this**。正式內層 `lambda$nativeAiScope$35(Instance;JJZ)Object` 為 static，將顯式 Instance 參數傳給 `sourceAi$lambda$scheduleAiTick$0(JJZ)V`；後者仍為 instance 並呼叫 onAiTimer。

本輪外層 `lambda$sourceAi$scheduleAiTick$22(JJZ)V` 為 instance，內層 `$21(Instance;JJZ)Object` 為 static，內層內容 hash 已與正式相同。外層只多出 actor 的 ASTORE／ALOAD，工具仍用線性內容 hash 配對 synthetic lambda，因此一左一右方法仍無法配對，連帶 sourceAi$scheduleAiTick 的 invokedynamic handle DIFF。

直接對指定外層兩方法呼叫本版 PathComparison：**same=true，path multisets equal (3)**。這是診斷，不取代 CLI 驗收；正式結果仍 DIFF。完整 javap、正規化內容及差異見附錄。最新主控已指派 [#203 呼叫點配對 lambda](https://github.com/chaoren0418-arch/lito-launcher-server/issues/203) 處理，Java 保持不變，待該工具合併後重驗。

## 怎麼驗證

Microsoft JDK 17.0.17（`C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin`）、Python 3.12（`C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe`）。ASM 9.7 使用 Maven 快取的 asm／asm-tree 及 scratch/issue-72/target/dependency/asm-analysis-9.7.jar。先重新編譯目前 main 的工具及 9 份 compile-stubs，再編譯 A01 清單中的全部 49 份 staged Java；stub 只供編譯，未打包。

輸入是 a-batches.json 的 A01.classes 與 A01.verify_entries；jar entry 精確比對 185 項，不只檢查工具 exit 0。實際使用裁切至 A01 的 access-widen 與 deviations，effects 清單本批沒有適用項，未新增核准。access-widen 僅 `NativeDeDeletionState#get_showId()I`（staged public／正式 package-private）。沒有關閉預設正規化或使用 diagnostic 開關放寬驗收。

重現命令（repo worktree 根目錄；下方附完整程式，可還原到相同 scratch 路徑）：

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-72/final-reproduce.py
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-72/audit-current.py
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-72/evidence-current.py
```

各步退出碼：stubs、工具 javac、A01 javac、semantic-diff、BridgeAudit 編譯／執行、javap、Dump／LambdaAudit 編譯及執行全部 **0**。CLI 的 0 僅表示比對完成。

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
SUMMARY SAME=183 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=1 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=18 ALLOW_METHODS=0
wrote D:\lineage開發\3-lito開發\scratch\wt\task-72\scratch\issue-72\final-reproduce.json
wrote D:\lineage開發\3-lito開發\scratch\wt\task-72\scratch\issue-72\final-reproduce.md
missingLeft=[]
extraLeft=[]
ACCEPTANCE False
```

## 未驗證／推測

- 未執行 staged 應用程式、單元測試、伺服器、登入器、遊戲或 DB；本批依規定在 #78 A-land 統一測試。compute 的錯誤行為與建構順序在此是來源及 class 靜態證據，不冒充執行測試。
- bridge 與 lambda 的處理方向已裁定；尚未以包含 #186、#203 的已合併工具完成 185 entries 全量驗收，不能把主控核准方向視為工具已通過。
- #203 應能消除剩餘 DIFF 是預期；本輪未使用未合併的 task/203 工具，也未把 DIFF 手動改為通過。
- 前輪已核准的 construction 證明（12 層、18 個建構子、正式 JAR 無子類、父鏈可達方法不讀 construction）本輪來源未改；完整證據保留在 [前輪報告 f4b20cb](https://github.com/chaoren0418-arch/lito-launcher-server/blob/f4b20cb/docs/reports/issue-72.md)。不將舊 182 SAME／3 DIFF 混為本輪結果。

## 產出路徑／給下一棒的提示

- 交付來源：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeCharacterFields.java`、`NativeDeInstanceState.java`。
- 核准紀錄：`docs/combat-merge/deviations.txt`、`deviations.md`；主報告：`docs/reports/issue-72.md`。
- 本機原始證據：`scratch/issue-72/final-reproduce.{json,md}`、`target/final-reproduce.jar`、`audit/{summary.txt,bridges.tsv,calls.tsv}`、`*-instance.javap.txt`、`formal-callers.javap.txt`、`lambda-audit.txt`、`remaining-full.diff`。不加入 git；重要結果及再現程式已嵌入本報告。
- **PR 描述同步受阻**：已將修訂描述以 UTF-8 寫入 `scratch/issue-72/pr-body.md`，執行 `gh pr edit 99 --repo chaoren0418-arch/lito-launcher-server --body-file scratch/issue-72/pr-body.md` 回傳 HTTP 401；REST PATCH 同樣 401，既有 Git credential helper 亦未提供可用認證。分支推送成功，但 PR #99 描述尚未更新；請具備 GitHub 寫入認證的調度器／本機執行器依本報告同步，移除過時的「待主控裁定」要求。
- **調度器／#203 審查與合併流程**：本輪受依賴阻擋，先等待 #203／PR #215 合併；不要重新派回 bridge／lambda 裁定。合併後再派本機執行器更新 main，以含 #186、#203 的工具及核准清單直接編譯 49 份 staged Java、重驗 185 entries，更新同一份報告的工具版本、命令、雜湊及逐項結果。每項須為 SAME／SAME_EFFECTS／SAME_PATHS／DEVIATION，DIFF=0、missingLeft/extraLeft=[]，才可送 A01 完成審查。
- **#78 A-land**：在 A01 全部驗收通過後，測試 construction 父鏈觀測、四個 Name overload 的求值／例外／旗標順序，以及 compute 空集合／全 null／非 null 元素錯誤行為。技能根因追蹤 #184，本批不修正式 bug。

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
DIFF local/lito381/server/NativeDeInstanceState.class
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

## 附錄 B：169 個 bridge 逐項清單

`N` 完整 owner 為 `local/lito381/server/NativeDeNpcConstructionState`；每行目標都是 **INVOKESPECIAL**，方法名稱及描述子完整保留。正式 Instance 均無宣告；表內正式宣告 owner 省略共同套件 `local/lito381/server/`。L 行號指 `formal-callers.javap.txt`，不是來源行號。參照數包含上述保守範圍與 handle。

| 證據 | bridge 完整描述子 | 轉呼叫目標 | 正式解析宣告 | 正式參照數 | 正式呼叫端 javap 證據（每項一例） |
|---|---|---|---|---:|---|
| B001 | `addAc(I)V` | `N#addAc(I)V` | `NativeDeCharacterFields#addAc(I)V` | 0 | 未找到 |
| B002 | `addBowDmgup(I)V` | `N#addBowDmgup(I)V` | `NativeDeCharacterFields#addBowDmgup(I)V` | 0 | 未找到 |
| B003 | `addBowHitup(I)V` | `N#addBowHitup(I)V` | `NativeDeCharacterFields#addBowHitup(I)V` | 0 | 未找到 |
| B004 | `addCha(I)V` | `N#addCha(I)V` | `NativeDeCharacterFields#addCha(I)V` | 0 | 未找到 |
| B005 | `addCon(I)V` | `N#addCon(I)V` | `NativeDeCharacterFields#addCon(I)V` | 0 | 未找到 |
| B006 | `addDex(I)V` | `N#addDex(I)V` | `NativeDeCharacterFields#addDex(I)V` | 0 | 未找到 |
| B007 | `addDmgup(I)V` | `N#addDmgup(I)V` | `NativeDeCharacterFields#addDmgup(I)V` | 3 | `local/lito381/server/NativeDeCharacterActor#addDmgup(I)V`；javap L236: `5: invokevirtual #100                // Method local/lito381/server/NativeDeCharacterFields.addDmgup:(I)V` |
| B008 | `addEarth(I)V` | `N#addEarth(I)V` | `NativeDeCharacterFields#addEarth(I)V` | 2 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19545: `623: invokevirtual #294                // Method addEarth:(I)V` |
| B009 | `addFire(I)V` | `N#addFire(I)V` | `NativeDeCharacterFields#addFire(I)V` | 2 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19549: `632: invokevirtual #297                // Method addFire:(I)V` |
| B010 | `addHitup(I)V` | `N#addHitup(I)V` | `NativeDeCharacterFields#addHitup(I)V` | 3 | `local/lito381/server/NativeDeCharacterActor#addHitup(I)V`；javap L245: `5: invokevirtual #103                // Method local/lito381/server/NativeDeCharacterFields.addHitup:(I)V` |
| B011 | `addInt(I)V` | `N#addInt(I)V` | `NativeDeCharacterFields#addInt(I)V` | 0 | 未找到 |
| B012 | `addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V` | `N#addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V` | `NativeDeDeletionPorts$ObjectRef;)V` | 0 | 未找到 |
| B013 | `addMaxHp(I)V` | `N#addMaxHp(I)V` | `NativeDeCharacterFields#addMaxHp(I)V` | 0 | 未找到 |
| B014 | `addMaxMp(I)V` | `N#addMaxMp(I)V` | `NativeDeCharacterFields#addMaxMp(I)V` | 0 | 未找到 |
| B015 | `addMr(I)V` | `N#addMr(I)V` | `NativeDeCharacterFields#addMr(I)V` | 0 | 未找到 |
| B016 | `addRegistBlind(I)V` | `N#addRegistBlind(I)V` | `NativeDeCharacterFields#addRegistBlind(I)V` | 0 | 未找到 |
| B017 | `addRegistSleep(I)V` | `N#addRegistSleep(I)V` | `NativeDeCharacterFields#addRegistSleep(I)V` | 0 | 未找到 |
| B018 | `addRegistStone(I)V` | `N#addRegistStone(I)V` | `NativeDeCharacterFields#addRegistStone(I)V` | 0 | 未找到 |
| B019 | `addRegistStun(I)V` | `N#addRegistStun(I)V` | `NativeDeCharacterFields#addRegistStun(I)V` | 0 | 未找到 |
| B020 | `addRegistSustain(I)V` | `N#addRegistSustain(I)V` | `NativeDeCharacterFields#addRegistSustain(I)V` | 0 | 未找到 |
| B021 | `addSp(I)V` | `N#addSp(I)V` | `NativeDeCharacterFields#addSp(I)V` | 2 | `local/lito381/server/NativeDeCharacterActor#addSp(I)V`；javap L227: `5: invokevirtual #97                 // Method local/lito381/server/NativeDeCharacterFields.addSp:(I)V` |
| B022 | `addStr(I)V` | `N#addStr(I)V` | `NativeDeCharacterFields#addStr(I)V` | 0 | 未找到 |
| B023 | `addStunSuccess(I)V` | `N#addStunSuccess(I)V` | `NativeDeCharacterFields#addStunSuccess(I)V` | 0 | 未找到 |
| B024 | `addWater(I)V` | `N#addWater(I)V` | `NativeDeCharacterFields#addWater(I)V` | 2 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19553: `641: invokevirtual #300                // Method addWater:(I)V` |
| B025 | `addWeaponBreakStack()V` | `N#addWeaponBreakStack()V` | `NativeDeNpcVitalState#addWeaponBreakStack()V` | 0 | 未找到 |
| B026 | `addWind(I)V` | `N#addWind(I)V` | `NativeDeCharacterFields#addWind(I)V` | 2 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19557: `650: invokevirtual #303                // Method addWind:(I)V` |
| B027 | `addWis(I)V` | `N#addWis(I)V` | `NativeDeCharacterFields#addWis(I)V` | 0 | 未找到 |
| B028 | `add_regist_freeze(I)V` | `N#add_regist_freeze(I)V` | `NativeDeCharacterFields#add_regist_freeze(I)V` | 0 | 未找到 |
| B029 | `broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `N#broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `NativeDeDeletionPorts$Viewer;)V` | 1 | `local/lito381/server/NativeDeVisibilityAccess#L1Character$broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`；javap L23484: `5: invokevirtual #55                 // Method local/lito381/server/NativeDeCharacterFields.broadcastPacketHP:(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` |
| B030 | `claimExpiredCorpseDeletion(J)Z` | `N#claimExpiredCorpseDeletion(J)Z` | `NativeDeNpcVitalState#claimExpiredCorpseDeletion(J)Z` | 2 | `local/lito381/server/NativeDeCorpseCore$Port#claim(Llocal/lito381/server/NativeDeCorpseCore$Npc;JLjava/lang/Object;)Z`；javap L7054: `2: invokeinterface #7,  3            // InterfaceMethod local/lito381/server/NativeDeCorpseCore$Npc.claimExpiredCorpseDeletion:(J)Z` |
| B031 | `destroyed()Z` | `N#destroyed()Z` | `NativeDeNpcVitalState#destroyed()Z` | 17 | `local/lito381/server/NativeDeChatJobs#admits(Llocal/lito381/server/NativeDeChatJobs$Owner;Llocal/lito381/server/NativeDeNpcConstructionState;)Z`；javap L5225: `31: invokevirtual #50                 // Method local/lito381/server/NativeDeNpcConstructionState.destroyed:()Z` |
| B032 | `forward(I)V` | `N#forward(I)V` | `NativeDeObjectFields#forward(I)V` | 0 | 未找到 |
| B033 | `getAc()I` | `N#getAc()I` | `NativeDeCharacterFields#getAc()I` | 0 | 未找到 |
| B034 | `getAddAttrKind()I` | `N#getAddAttrKind()I` | `NativeDeCharacterFields#getAddAttrKind()I` | 0 | 未找到 |
| B035 | `getAtkspeed()I` | `N#getAtkspeed()I` | `NativeDeNpcTemplateState#getAtkspeed()I` | 1 | `local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V`；javap L15277: `74: invokevirtual #1081               // Method getAtkspeed:()I` |
| B036 | `getBowDmgup()I` | `N#getBowDmgup()I` | `NativeDeCharacterFields#getBowDmgup()I` | 0 | 未找到 |
| B037 | `getBowHitup()I` | `N#getBowHitup()I` | `NativeDeCharacterFields#getBowHitup()I` | 0 | 未找到 |
| B038 | `getBraveSpeed()I` | `N#getBraveSpeed()I` | `NativeDeCharacterFields#getBraveSpeed()I` | 1 | `local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState;`；javap L22891: `108: invokevirtual #51                 // Method local/lito381/server/NativeDePerceptionState.getBraveSpeed:()I` |
| B039 | `getCha()S` | `N#getCha()S` | `NativeDeCharacterFields#getCha()S` | 0 | 未找到 |
| B040 | `getChaLightSize()I` | `N#getChaLightSize()I` | `NativeDeCharacterFields#getChaLightSize()I` | 1 | `local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V`；javap L22524: `147: invokevirtual #97                 // Method local/lito381/server/NativeDePerceptionState.getChaLightSize:()I` |
| B041 | `getCon()S` | `N#getCon()S` | `NativeDeCharacterFields#getCon()S` | 0 | 未找到 |
| B042 | `getCurrentHp()I` | `N#getCurrentHp()I` | `NativeDeCharacterFields#getCurrentHp()I` | 9 | `local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt#<init>(Llocal/lito381/server/NativeDeCharacterFields;)V`；javap L4908: `11: invokevirtual #13                 // Method local/lito381/server/NativeDeCharacterFields.getCurrentHp:()I` |
| B043 | `getCurrentMp()I` | `N#getCurrentMp()I` | `NativeDeCharacterFields#getCurrentMp()I` | 0 | 未找到 |
| B044 | `getDex()S` | `N#getDex()S` | `NativeDeCharacterFields#getDex()S` | 0 | 未找到 |
| B045 | `getDmgup()I` | `N#getDmgup()I` | `NativeDeCharacterFields#getDmgup()I` | 0 | 未找到 |
| B046 | `getDolls()Ljava/util/Map;` | `N#getDolls()Ljava/util/Map;` | `Map;` | 6 | `local/lito381/server/NativeDeCharacterActor#getDolls()Ljava/util/Map;`；javap L480: `4: invokeinterface #188,  1          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.getDolls:()Ljava/util/Map;` |
| B047 | `getEarth()I` | `N#getEarth()I` | `NativeDeCharacterFields#getEarth()I` | 0 | 未找到 |
| B048 | `getExp()J` | `N#getExp()J` | `NativeDeCharacterFields#getExp()J` | 0 | 未找到 |
| B049 | `getFire()I` | `N#getFire()I` | `NativeDeCharacterFields#getFire()I` | 0 | 未找到 |
| B050 | `getGfxId()I` | `N#getGfxId()I` | `NativeDeCharacterFields#getGfxId()I` | 0 | 未找到 |
| B051 | `getHeading()I` | `N#getHeading()I` | `NativeDeCharacterFields#getHeading()I` | 3 | `local/lito381/server/NativeDeCharacterActor#getHeading()I`；javap L488: `4: invokeinterface #192,  1          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.getHeading:()I` |
| B052 | `getHitup()I` | `N#getHitup()I` | `NativeDeCharacterFields#getHitup()I` | 0 | 未找到 |
| B053 | `getId()I` | `N#getId()I` | `NativeDeCorpseState#getId()I` | 76 | `local/lito381/server/NativeDeCharacterActor#<init>(Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeCharacterFields$Snapshot;)V`；javap L23: `21: invokeinterface #23,  1           // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.getId:()I` |
| B054 | `getInt()S` | `N#getInt()S` | `NativeDeCharacterFields#getInt()S` | 5 | `local/lito381/server/NativeDeCharacterActor#getInt()S`；javap L200: `4: invokevirtual #87                 // Method local/lito381/server/NativeDeCharacterFields.getInt:()S` |
| B055 | `getKarma()I` | `N#getKarma()I` | `NativeDeCharacterFields#getKarma()I` | 0 | 未找到 |
| B056 | `getKnownObjects()Ljava/util/List;` | `N#getKnownObjects()Ljava/util/List;` | `List;` | 1 | `local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$getKnownObjects(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/util/List;`；javap L23696: `4: invokevirtual #190                // Method local/lito381/server/NativeDeCharacterFields.getKnownObjects:()Ljava/util/List;` |
| B057 | `getKnownPlayers()Ljava/util/List;` | `N#getKnownPlayers()Ljava/util/List;` | `List;` | 0 | 未找到 |
| B058 | `getLawful()I` | `N#getLawful()I` | `NativeDeCharacterFields#getLawful()I` | 1 | `local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V`；javap L22535: `168: invokevirtual #103                // Method local/lito381/server/NativeDePerceptionState.getLawful:()I` |
| B059 | `getLevel()I` | `N#getLevel()I` | `NativeDeCharacterFields#getLevel()I` | 6 | `local/lito381/server/NativeDeCharacterActor#getLevel()I`；javap L102: `4: invokevirtual #54                 // Method local/lito381/server/NativeDeCharacterFields.getLevel:()I` |
| B060 | `getLightSize()I` | `N#getLightSize()I` | `NativeDeNpcTemplateState#getLightSize()I` | 0 | 未找到 |
| B061 | `getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D` | `N#getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D` | `NativeDeObjectFields;)D` | 0 | 未找到 |
| B062 | `getLocation()Llocal/lito381/server/NativeDeWorldLocation;` | `N#getLocation()Llocal/lito381/server/NativeDeWorldLocation;` | `NativeDeWorldLocation;` | 22 | `local/lito381/server/NativeDeObjectBindings#attachLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeWorldContext;IIII)Llocal/lito381/server/NativeDeWorldLocation;`；javap L20834: `8: invokevirtual #26                 // Method local/lito381/server/NativeDeObjectFields.getLocation:()Llocal/lito381/server/NativeDeWorldLocation;` |
| B063 | `getMap()Llocal/lito381/server/NativeDeWorldMap;` | `N#getMap()Llocal/lito381/server/NativeDeWorldMap;` | `NativeDeWorldMap;` | 3 | `local/lito381/server/NativeDeInstanceState#sourceAi$checkSpawnLocation()V`；javap L15413: `1: invokevirtual #1143               // Method getMap:()Llocal/lito381/server/NativeDeWorldMap;` |
| B064 | `getMaxHp()I` | `N#getMaxHp()I` | `NativeDeCharacterFields#getMaxHp()I` | 9 | `local/lito381/server/NativeDeCharacterFields#commitCurrentHpLocked(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;`；javap L1416: `26: invokevirtual #545                // Method getMaxHp:()I` |
| B065 | `getMaxMp()I` | `N#getMaxMp()I` | `NativeDeCharacterFields#getMaxMp()I` | 2 | `local/lito381/server/NativeDeCharacterFields#commitCurrentMpLocked(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;`；javap L1465: `26: invokevirtual #561                // Method getMaxMp:()I` |
| B066 | `getMoveSpeed()I` | `N#getMoveSpeed()I` | `NativeDeCharacterFields#getMoveSpeed()I` | 4 | `local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V`；javap L22528: `155: invokevirtual #100                // Method local/lito381/server/NativeDePerceptionState.getMoveSpeed:()I` |
| B067 | `getMr()I` | `N#getMr()I` | `NativeDeCharacterFields#getMr()I` | 0 | 未找到 |
| B068 | `getNameId()Ljava/lang/String;` | `N#getNameId()Ljava/lang/String;` | `String;` | 17 | `local/lito381/server/NativeDeCharacterActor#getNameId()Ljava/lang/String;`；javap L339: `4: invokeinterface #137,  1          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.getNameId:()Ljava/lang/String;` |
| B069 | `getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;` | `N#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;` | `NativeDeNpcTemplate;` | 7 | `local/lito381/server/NativeDeChatAccess#L1NpcInstance$getNpcTemplate(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeNpcTemplate;`；javap L5074: `4: invokevirtual #24                 // Method local/lito381/server/NativeDeNpcConstructionState.getNpcTemplate:()Llocal/lito381/server/NativeDeNpcTemplate;` |
| B070 | `getOwnLightSize()I` | `N#getOwnLightSize()I` | `NativeDeCharacterFields#getOwnLightSize()I` | 0 | 未找到 |
| B071 | `getPassispeed()I` | `N#getPassispeed()I` | `NativeDeNpcTemplateState#getPassispeed()I` | 1 | `local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V`；javap L15266: `43: invokevirtual #1073               // Method getPassispeed:()I` |
| B072 | `getPetList()Ljava/util/Map;` | `N#getPetList()Ljava/util/Map;` | `Map;` | 4 | `local/lito381/server/NativeDeDeletionDeState#sourceDeleteDe()V`；javap L8061: `13: invokevirtual #97                 // Method getPetList:()Ljava/util/Map;` |
| B073 | `getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;` | `N#getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;` | `NativeDePerceptionPorts$Poison;` | 2 | `local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V`；javap L22452: `17: invokevirtual #42                 // Method local/lito381/server/NativeDePerceptionState.getPoison:()Llocal/lito381/server/NativeDePerceptionPorts$Poison;` |
| B074 | `getRegistBlind()I` | `N#getRegistBlind()I` | `NativeDeCharacterFields#getRegistBlind()I` | 0 | 未找到 |
| B075 | `getRegistFreeze()I` | `N#getRegistFreeze()I` | `NativeDeCharacterFields#getRegistFreeze()I` | 0 | 未找到 |
| B076 | `getRegistSleep()I` | `N#getRegistSleep()I` | `NativeDeCharacterFields#getRegistSleep()I` | 0 | 未找到 |
| B077 | `getRegistStone()I` | `N#getRegistStone()I` | `NativeDeCharacterFields#getRegistStone()I` | 0 | 未找到 |
| B078 | `getRegistStun()I` | `N#getRegistStun()I` | `NativeDeCharacterFields#getRegistStun()I` | 0 | 未找到 |
| B079 | `getRegistSustain()I` | `N#getRegistSustain()I` | `NativeDeCharacterFields#getRegistSustain()I` | 0 | 未找到 |
| B080 | `getSp()I` | `N#getSp()I` | `NativeDeCharacterFields#getSp()I` | 0 | 未找到 |
| B081 | `getStatus()I` | `N#getStatus()I` | `NativeDeCharacterFields#getStatus()I` | 1 | `local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V`；javap L22516: `131: invokevirtual #91                 // Method local/lito381/server/NativeDePerceptionState.getStatus:()I` |
| B082 | `getStr()S` | `N#getStr()S` | `NativeDeCharacterFields#getStr()S` | 0 | 未找到 |
| B083 | `getStunSuccess()I` | `N#getStunSuccess()I` | `NativeDeCharacterFields#getStunSuccess()I` | 0 | 未找到 |
| B084 | `getTempCharGfx()I` | `N#getTempCharGfx()I` | `NativeDeCharacterFields#getTempCharGfx()I` | 6 | `local/lito381/server/NativeDeCharacterActor#getTempCharGfx()I`；javap L262: `4: invokevirtual #109                // Method local/lito381/server/NativeDeCharacterFields.getTempCharGfx:()I` |
| B085 | `getTempLawful()I` | `N#getTempLawful()I` | `NativeDeNpcTemplateState#getTempLawful()I` | 0 | 未找到 |
| B086 | `getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I` | `N#getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I` | `NativeDeObjectFields;)I` | 0 | 未找到 |
| B087 | `getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I` | `N#getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I` | `NativeDeObjectFields;)I` | 0 | 未找到 |
| B088 | `getTitle()Ljava/lang/String;` | `N#getTitle()Ljava/lang/String;` | `String;` | 1 | `local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V`；javap L22543: `184: invokevirtual #114                // Method local/lito381/server/NativeDePerceptionState.getTitle:()Ljava/lang/String;` |
| B089 | `getTrueMr()I` | `N#getTrueMr()I` | `NativeDeCharacterFields#getTrueMr()I` | 0 | 未找到 |
| B090 | `getTrueSp()I` | `N#getTrueSp()I` | `NativeDeCharacterFields#getTrueSp()I` | 1 | `local/lito381/server/NativeDeCharacterFields#getSp()I`；javap L2200: `1: invokevirtual #787                // Method getTrueSp:()I` |
| B091 | `getWater()I` | `N#getWater()I` | `NativeDeCharacterFields#getWater()I` | 0 | 未找到 |
| B092 | `getWeaponBreakStack()I` | `N#getWeaponBreakStack()I` | `NativeDeNpcVitalState#getWeaponBreakStack()I` | 0 | 未找到 |
| B093 | `getWind()I` | `N#getWind()I` | `NativeDeCharacterFields#getWind()I` | 0 | 未找到 |
| B094 | `getWis()S` | `N#getWis()S` | `NativeDeCharacterFields#getWis()S` | 0 | 未找到 |
| B095 | `get_deadTimerTemp()I` | `N#get_deadTimerTemp()I` | `NativeDeNpcVitalState#get_deadTimerTemp()I` | 0 | 未找到 |
| B096 | `get_fishX()I` | `N#get_fishX()I` | `NativeDePerceptionState#get_fishX()I` | 1 | `local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`；javap L23244: `164: invokevirtual #234                // Method get_fishX:()I` |
| B097 | `get_fishY()I` | `N#get_fishY()I` | `NativeDePerceptionState#get_fishY()I` | 1 | `local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`；javap L23246: `168: invokevirtual #237                // Method get_fishY:()I` |
| B098 | `get_showId()I` | `N#get_showId()I` | `NativeDeDeletionState#get_showId()I` | 6 | `local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V`；javap L9260: `619: invokevirtual #626                // Method get_showId:()I` |
| B099 | `hasSkillEffect(I)Z` | `N#hasSkillEffect(I)Z` | `NativeDePerceptionState#hasSkillEffect(I)Z` | 17 | `local/lito381/server/NativeDeCharacterFields#getMr()I`；javap L4495: `4: invokevirtual #1167               // Method hasSkillEffect:(I)Z` |
| B100 | `isAgro()Z` | `N#isAgro()Z` | `NativeDeNpcTemplateState#isAgro()Z` | 0 | 未找到 |
| B101 | `isAgrocoi()Z` | `N#isAgrocoi()Z` | `NativeDeNpcTemplateState#isAgrocoi()Z` | 0 | 未找到 |
| B102 | `isAgrososc()Z` | `N#isAgrososc()Z` | `NativeDeNpcTemplateState#isAgrososc()Z` | 0 | 未找到 |
| B103 | `isAiRunning()Z` | `N#isAiRunning()Z` | `NativeDePerceptionState#isAiRunning()Z` | 1 | `local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`；javap L23267: `212: invokevirtual #243                // Method isAiRunning:()Z` |
| B104 | `isDead()Z` | `N#isDead()Z` | `NativeDeCharacterFields#isDead()Z` | 29 | `local/lito381/server/NativeDeCharacterFields#commitCurrentHpLocked(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;`；javap L1422: `40: invokevirtual #548                // Method isDead:()Z` |
| B105 | `isFishing()Z` | `N#isFishing()Z` | `NativeDePerceptionState#isFishing()Z` | 1 | `local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`；javap L23235: `146: invokevirtual #229                // Method isFishing:()Z` |
| B106 | `isInvisble()Z` | `N#isInvisble()Z` | `NativeDeCharacterFields#isInvisble()Z` | 3 | `local/lito381/server/NativeDeCharacterFields#getChaLightSize()I`；javap L4650: `1: invokevirtual #1194               // Method isInvisble:()Z` |
| B107 | `isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z` | `N#isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z` | `NativeDeDeletionState;)Z` | 0 | 未找到 |
| B108 | `isParalyzed()Z` | `N#isParalyzed()Z` | `NativeDeCharacterFields#isParalyzed()Z` | 1 | `local/lito381/server/NativeDeInstanceState#sourceAi$runAiTick(J)V`；javap L13508: `58: invokevirtual #716                // Method isParalyzed:()Z` |
| B109 | `isShop()Z` | `N#isShop()Z` | `NativeDePerceptionState#isShop()Z` | 1 | `local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`；javap L23204: `79: invokevirtual #205                // Method isShop:()Z` |
| B110 | `isSleeped()Z` | `N#isSleeped()Z` | `NativeDeCharacterFields#isSleeped()Z` | 1 | `local/lito381/server/NativeDeInstanceState#sourceAi$runAiTick(J)V`；javap L13511: `65: invokevirtual #719                // Method isSleeped:()Z` |
| B111 | `knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` | `N#knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` | `NativeDeDeletionPorts$ObjectRef;)Z` | 2 | `local/lito381/server/NativeDeNpcHateList#removeInvalidCharacter(Llocal/lito381/server/NativeDeDeletionState;)V`；javap L18260: `304: invokevirtual #163                // Method local/lito381/server/NativeDeDeletionState.knownsObject:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` |
| B112 | `onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `N#onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `NativeDeDeletionPorts$Viewer;)V` | 0 | 未找到 |
| B113 | `onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `N#onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `NativeDeDeletionPorts$Viewer;)V` | 3 | `local/lito381/server/NativeDePerceptionJobs$Task#lambda$run$0()V`；javap L22406: `34: invokevirtual #124                // Method local/lito381/server/NativeDePerceptionState.onPerceive:(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` |
| B114 | `onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `N#onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V` | `NativeDeDeletionPorts$Viewer;)V` | 0 | 未找到 |
| B115 | `rebindWorldMap(S)V` | `N#rebindWorldMap(S)V` | `NativeDeObjectFields#rebindWorldMap(S)V` | 0 | 未找到 |
| B116 | `removeAllKnownObjects()V` | `N#removeAllKnownObjects()V` | `NativeDeCharacterFields#removeAllKnownObjects()V` | 2 | `local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V`；javap L9541: `1319: invokevirtual #734                // Method removeAllKnownObjects:()V` |
| B117 | `removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V` | `N#removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V` | `NativeDeDeletionPorts$ObjectRef;)V` | 1 | `local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V`；javap L23798: `5: invokevirtual #242                // Method local/lito381/server/NativeDeCharacterFields.removeKnownObject:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V` |
| B118 | `removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` | `N#removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` | `NativeDeDeletionPorts$ObjectRef;)Z` | 2 | `local/lito381/server/NativeDeCharacterFields#removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V`；javap L3821: `2: invokevirtual #1114               // Method removeKnownObjectIfPresent:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` |
| B119 | `resurrect(I)V` | `N#resurrect(I)V` | `NativeDeNpcVitalState#resurrect(I)V` | 3 | `local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V`；javap L9136: `331: invokevirtual #547                // Method resurrect:(I)V` |
| B120 | `setAc(I)V` | `N#setAc(I)V` | `NativeDeCharacterFields#setAc(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setAc(I)V`；javap L147: `5: invokevirtual #69                 // Method local/lito381/server/NativeDeCharacterFields.setAc:(I)V` |
| B121 | `setAddAttrKind(I)V` | `N#setAddAttrKind(I)V` | `NativeDeCharacterFields#setAddAttrKind(I)V` | 0 | 未找到 |
| B122 | `setAgro(Z)V` | `N#setAgro(Z)V` | `NativeDeNpcTemplateState#setAgro(Z)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19490: `503: invokevirtual #276                // Method setAgro:(Z)V` |
| B123 | `setAgrocoi(Z)V` | `N#setAgrocoi(Z)V` | `NativeDeNpcTemplateState#setAgrocoi(Z)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19494: `511: invokevirtual #282                // Method setAgrocoi:(Z)V` |
| B124 | `setAgrososc(Z)V` | `N#setAgrososc(Z)V` | `NativeDeNpcTemplateState#setAgrososc(Z)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19498: `519: invokevirtual #288                // Method setAgrososc:(Z)V` |
| B125 | `setAtkspeed(I)V` | `N#setAtkspeed(I)V` | `NativeDeNpcTemplateState#setAtkspeed(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setAtkspeed(I)V`；javap L427: `5: invokeinterface #168,  2          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.setAtkspeed:(I)V` |
| B126 | `setBraveSpeed(I)V` | `N#setBraveSpeed(I)V` | `NativeDeCharacterFields#setBraveSpeed(I)V` | 4 | `local/lito381/server/NativeDeCharacterActor#setBraveSpeed(I)V`；javap L409: `5: invokeinterface #162,  2          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.setBraveSpeed:(I)V` |
| B127 | `setCha(I)V` | `N#setCha(I)V` | `NativeDeCharacterFields#setCha(I)V` | 3 | `local/lito381/server/NativeDeCharacterActor#setCha(I)V`；javap L218: `5: invokevirtual #94                 // Method local/lito381/server/NativeDeCharacterFields.setCha:(I)V` |
| B128 | `setChaLightSize(I)V` | `N#setChaLightSize(I)V` | `NativeDeCharacterFields#setChaLightSize(I)V` | 0 | 未找到 |
| B129 | `setCon(I)V` | `N#setCon(I)V` | `NativeDeCharacterFields#setCon(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setCon(I)V`；javap L183: `5: invokevirtual #81                 // Method local/lito381/server/NativeDeCharacterFields.setCon:(I)V` |
| B130 | `setDex(I)V` | `N#setDex(I)V` | `NativeDeCharacterFields#setDex(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setDex(I)V`；javap L174: `5: invokevirtual #78                 // Method local/lito381/server/NativeDeCharacterFields.setDex:(I)V` |
| B131 | `setExp(J)V` | `N#setExp(J)V` | `NativeDeCharacterFields#setExp(J)V` | 2 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19595: `730: invokevirtual #328                // Method setExp:(J)V` |
| B132 | `setGfxId(I)V` | `N#setGfxId(I)V` | `NativeDeCharacterFields#setGfxId(I)V` | 3 | `local/lito381/server/NativeDeCharacterActor#setGfxId(I)V`；javap L271: `5: invokevirtual #112                // Method local/lito381/server/NativeDeCharacterFields.setGfxId:(I)V` |
| B133 | `setGfxidInStatus(I)V` | `N#setGfxidInStatus(I)V` | `NativeDeNpcTemplateState#setGfxidInStatus(I)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19587: `714: invokevirtual #318                // Method setGfxidInStatus:(I)V` |
| B134 | `setHeading(I)V` | `N#setHeading(I)V` | `NativeDeCharacterFields#setHeading(I)V` | 0 | 未找到 |
| B135 | `setId(I)V` | `N#setId(I)V` | `NativeDeObjectFields#setId(I)V` | 0 | 未找到 |
| B136 | `setInt(I)V` | `N#setInt(I)V` | `NativeDeCharacterFields#setInt(I)V` | 7 | `local/lito381/server/NativeDeCharacterActor#setInt(I)V`；javap L192: `5: invokevirtual #84                 // Method local/lito381/server/NativeDeCharacterFields.setInt:(I)V` |
| B137 | `setKarma(I)V` | `N#setKarma(I)V` | `NativeDeCharacterFields#setKarma(I)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19660: `856: invokevirtual #358                // Method setKarma:(I)V` |
| B138 | `setLawful(I)V` | `N#setLawful(I)V` | `NativeDeCharacterFields#setLawful(I)V` | 4 | `local/lito381/server/NativeDeCharacterActor#setLawful(I)V`；javap L156: `5: invokevirtual #72                 // Method local/lito381/server/NativeDeCharacterFields.setLawful:(I)V` |
| B139 | `setLevel(I)V` | `N#setLevel(I)V` | `NativeDeCharacterFields#setLevel(I)V` | 6 | `local/lito381/server/NativeDeCharacterActor#setLevel(I)V`；javap L94: `5: invokevirtual #50                 // Method local/lito381/server/NativeDeCharacterFields.setLevel:(I)V` |
| B140 | `setLightSize(I)V` | `N#setLightSize(I)V` | `NativeDeNpcTemplateState#setLightSize(I)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19664: `864: invokevirtual #364                // Method setLightSize:(I)V` |
| B141 | `setLocation(III)V` | `N#setLocation(III)V` | `NativeDeObjectFields#setLocation(III)V` | 1 | `local/lito381/server/NativeDeWorldAccess#L1Object$setLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;III)V`；javap L25228: `7: invokevirtual #98                 // Method local/lito381/server/NativeDeObjectFields.setLocation:(III)V` |
| B142 | `setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V` | `N#setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V` | `NativeDeWorldLocation;)V` | 1 | `local/lito381/server/NativeDeObjectBindings#attachLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeWorldContext;IIII)Llocal/lito381/server/NativeDeWorldLocation;`；javap L20845: `30: invokevirtual #39                 // Method local/lito381/server/NativeDeObjectFields.setLocation:(Llocal/lito381/server/NativeDeWorldLocation;)V` |
| B143 | `setMap(Llocal/lito381/server/NativeDeWorldMap;)V` | `N#setMap(Llocal/lito381/server/NativeDeWorldMap;)V` | `NativeDeWorldMap;)V` | 0 | 未找到 |
| B144 | `setMap(S)V` | `N#setMap(S)V` | `NativeDeObjectFields#setMap(S)V` | 0 | 未找到 |
| B145 | `setMaxHp(I)V` | `N#setMaxHp(I)V` | `NativeDeCharacterFields#setMaxHp(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setMaxHp(I)V`；javap L111: `5: invokevirtual #57                 // Method local/lito381/server/NativeDeCharacterFields.setMaxHp:(I)V` |
| B146 | `setMaxMp(I)V` | `N#setMaxMp(I)V` | `NativeDeCharacterFields#setMaxMp(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setMaxMp(I)V`；javap L129: `5: invokevirtual #63                 // Method local/lito381/server/NativeDeCharacterFields.setMaxMp:(I)V` |
| B147 | `setMoveSpeed(I)V` | `N#setMoveSpeed(I)V` | `NativeDeCharacterFields#setMoveSpeed(I)V` | 3 | `local/lito381/server/NativeDeCharacterActor#setMoveSpeed(I)V`；javap L400: `5: invokeinterface #159,  2          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.setMoveSpeed:(I)V` |
| B148 | `setMr(I)V` | `N#setMr(I)V` | `NativeDeCharacterFields#setMr(I)V` | 2 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19399: `338: invokevirtual #242                // Method setMr:(I)V` |
| B149 | `setName(Ljava/lang/String;)V` | `N#setName(Ljava/lang/String;)V` | `String;)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19228: `17: invokevirtual #125                // Method setName:(Ljava/lang/String;)V` |
| B150 | `setNameId(Ljava/lang/String;)V` | `N#setNameId(Ljava/lang/String;)V` | `String;)V` | 4 | `local/lito381/server/NativeDeCharacterActor#setNameId(Ljava/lang/String;)V`；javap L331: `5: invokeinterface #133,  2          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.setNameId:(Ljava/lang/String;)V` |
| B151 | `setOwnLightSize(I)V` | `N#setOwnLightSize(I)V` | `NativeDeCharacterFields#setOwnLightSize(I)V` | 0 | 未找到 |
| B152 | `setPassispeed(I)V` | `N#setPassispeed(I)V` | `NativeDeNpcTemplateState#setPassispeed(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setPassispeed(I)V`；javap L418: `5: invokeinterface #165,  2          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.setPassispeed:(I)V` |
| B153 | `setPickupItem(Z)V` | `N#setPickupItem(Z)V` | `NativeDeNpcTemplateState#setPickupItem(Z)V` | 1 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19646: `828: invokevirtual #346                // Method setPickupItem:(Z)V` |
| B154 | `setStatus(I)V` | `N#setStatus(I)V` | `NativeDeCharacterFields#setStatus(I)V` | 12 | `local/lito381/server/NativeDeCharacterActor#setStatus(I)V`；javap L280: `5: invokevirtual #115                // Method local/lito381/server/NativeDeCharacterFields.setStatus:(I)V` |
| B155 | `setStr(I)V` | `N#setStr(I)V` | `NativeDeCharacterFields#setStr(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setStr(I)V`；javap L165: `5: invokevirtual #75                 // Method local/lito381/server/NativeDeCharacterFields.setStr:(I)V` |
| B156 | `setTempCharGfx(I)V` | `N#setTempCharGfx(I)V` | `NativeDeCharacterFields#setTempCharGfx(I)V` | 3 | `local/lito381/server/NativeDeCharacterActor#setTempCharGfx(I)V`；javap L254: `5: invokevirtual #106                // Method local/lito381/server/NativeDeCharacterFields.setTempCharGfx:(I)V` |
| B157 | `setTempLawful(I)V` | `N#setTempLawful(I)V` | `NativeDeNpcTemplateState#setTempLawful(I)V` | 2 | `local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L19619: `780: invokevirtual #340                // Method setTempLawful:(I)V` |
| B158 | `setTitle(Ljava/lang/String;)V` | `N#setTitle(Ljava/lang/String;)V` | `String;)V` | 3 | `local/lito381/server/NativeDeCharacterActor#setTitle(Ljava/lang/String;)V`；javap L463: `5: invokeinterface #181,  2          // InterfaceMethod local/lito381/server/NativeDeInitializerKernel$Actor.setTitle:(Ljava/lang/String;)V` |
| B159 | `setWeaponBreakStack(I)V` | `N#setWeaponBreakStack(I)V` | `NativeDeNpcVitalState#setWeaponBreakStack(I)V` | 5 | `local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V`；javap L9040: `98: invokevirtual #513                // Method setWeaponBreakStack:(I)V` |
| B160 | `setWis(I)V` | `N#setWis(I)V` | `NativeDeCharacterFields#setWis(I)V` | 5 | `local/lito381/server/NativeDeCharacterActor#setWis(I)V`；javap L209: `5: invokevirtual #91                 // Method local/lito381/server/NativeDeCharacterFields.setWis:(I)V` |
| B161 | `setX(I)V` | `N#setX(I)V` | `NativeDeObjectFields#setX(I)V` | 0 | 未找到 |
| B162 | `setXY(II)V` | `N#setXY(II)V` | `NativeDeObjectFields#setXY(II)V` | 0 | 未找到 |
| B163 | `setY(I)V` | `N#setY(I)V` | `NativeDeObjectFields#setY(I)V` | 0 | 未找到 |
| B164 | `set_deadTimerTemp(I)V` | `N#set_deadTimerTemp(I)V` | `NativeDeNpcVitalState#set_deadTimerTemp(I)V` | 0 | 未找到 |
| B165 | `set_showId(I)V` | `N#set_showId(I)V` | `NativeDeObjectFields#set_showId(I)V` | 1 | `local/lito381/server/NativeDeObjectBindings#attachLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeWorldContext;IIII)Llocal/lito381/server/NativeDeWorldLocation;`；javap L20848: `37: invokevirtual #43                 // Method local/lito381/server/NativeDeObjectFields.set_showId:(I)V` |
| B166 | `setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V` | `N#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V` | `NativeDeNpcTemplate;)V` | 2 | `local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V`；javap L17171: `254: invokevirtual #172                // Method setting_template:(Llocal/lito381/server/NativeDeNpcTemplate;)V` |
| B167 | `sourceDeleteDe()V` | `N#sourceDeleteDe()V` | `NativeDeDeletionDeState#sourceDeleteDe()V` | 1 | `local/lito381/server/NativeDeDeletionDeState#deleteMe()V @insn=3 HANDLE H5 local/lito381/server/NativeDeDeletionDeState#sourceDeleteDe()V`；formal-handle.javap.txt L50: `#38 = MethodHandle       5:#37         // REF_invokeVirtual local/lito381/server/NativeDeDeletionDeState.sourceDeleteDe:()V` |
| B168 | `start_shop()V` | `N#start_shop()V` | `NativeDePerceptionState#start_shop()V` | 1 | `local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`；javap L23175: `20: invokevirtual #172                // Method start_shop:()V` |
| B169 | `tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` | `N#tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` | `NativeDeDeletionPorts$ObjectRef;)Z` | 2 | `local/lito381/server/NativeDeCharacterFields#addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V`；javap L3744: `2: invokevirtual #1100               // Method tryAddKnownObject:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z` |

### 全部 bridge 的 javap flags 與轉呼叫指令

以下逐項從本輪 javac 產物 `javap -c -p -s -v` 擷取，未以 ASM 分析結果冒充 javap。無轉型指令；其餘僅按描述子載入 receiver／參數與對應 return。

```text
B001 addAc(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8495
  2: invokespecial #1237               // Method local/lito381/server/NativeDeNpcConstructionState.addAc:(I)V

B002 addBowDmgup(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7653
  2: invokespecial #1024               // Method local/lito381/server/NativeDeNpcConstructionState.addBowDmgup:(I)V

B003 addBowHitup(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7630
  2: invokespecial #1018               // Method local/lito381/server/NativeDeNpcConstructionState.addBowHitup:(I)V

B004 addCha(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8355
  2: invokespecial #1201               // Method local/lito381/server/NativeDeNpcConstructionState.addCha:(I)V

B005 addCon(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8425
  2: invokespecial #1219               // Method local/lito381/server/NativeDeNpcConstructionState.addCon:(I)V

B006 addDex(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8390
  2: invokespecial #1210               // Method local/lito381/server/NativeDeNpcConstructionState.addDex:(I)V

B007 addDmgup(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8262
  2: invokespecial #1181               // Method local/lito381/server/NativeDeNpcConstructionState.addDmgup:(I)V

B008 addEarth(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7837
  2: invokespecial #1072               // Method local/lito381/server/NativeDeNpcConstructionState.addEarth:(I)V

B009 addFire(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7860
  2: invokespecial #1078               // Method local/lito381/server/NativeDeNpcConstructionState.addFire:(I)V

B010 addHitup(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8239
  2: invokespecial #1175               // Method local/lito381/server/NativeDeNpcConstructionState.addHitup:(I)V

B011 addInt(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8320
  2: invokespecial #1196               // Method local/lito381/server/NativeDeNpcConstructionState.addInt:(I)V

B012 addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8000
  2: invokespecial #1116               // Method local/lito381/server/NativeDeNpcConstructionState.addKnownObject:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V

B013 addMaxHp(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8565
  2: invokespecial #1255               // Method local/lito381/server/NativeDeNpcConstructionState.addMaxHp:(I)V

B014 addMaxMp(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8530
  2: invokespecial #1246               // Method local/lito381/server/NativeDeNpcConstructionState.addMaxMp:(I)V

B015 addMr(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7573
  2: invokespecial #1003               // Method local/lito381/server/NativeDeNpcConstructionState.addMr:(I)V

B016 addRegistBlind(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7676
  2: invokespecial #1030               // Method local/lito381/server/NativeDeNpcConstructionState.addRegistBlind:(I)V

B017 addRegistSleep(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7745
  2: invokespecial #1048               // Method local/lito381/server/NativeDeNpcConstructionState.addRegistSleep:(I)V

B018 addRegistStone(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7768
  2: invokespecial #1054               // Method local/lito381/server/NativeDeNpcConstructionState.addRegistStone:(I)V

B019 addRegistStun(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7791
  2: invokespecial #1060               // Method local/lito381/server/NativeDeNpcConstructionState.addRegistStun:(I)V

B020 addRegistSustain(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7699
  2: invokespecial #1036               // Method local/lito381/server/NativeDeNpcConstructionState.addRegistSustain:(I)V

B021 addSp(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8205
  2: invokespecial #1166               // Method local/lito381/server/NativeDeNpcConstructionState.addSp:(I)V

B022 addStr(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8460
  2: invokespecial #1228               // Method local/lito381/server/NativeDeNpcConstructionState.addStr:(I)V

B023 addStunSuccess(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7607
  2: invokespecial #1012               // Method local/lito381/server/NativeDeNpcConstructionState.addStunSuccess:(I)V

B024 addWater(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7883
  2: invokespecial #1084               // Method local/lito381/server/NativeDeNpcConstructionState.addWater:(I)V

B025 addWeaponBreakStack()V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7333
  1: invokespecial #945                // Method local/lito381/server/NativeDeNpcConstructionState.addWeaponBreakStack:()V

B026 addWind(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7906
  2: invokespecial #1090               // Method local/lito381/server/NativeDeNpcConstructionState.addWind:(I)V

B027 addWis(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8285
  2: invokespecial #1187               // Method local/lito381/server/NativeDeNpcConstructionState.addWis:(I)V

B028 add_regist_freeze(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7722
  2: invokespecial #1042               // Method local/lito381/server/NativeDeNpcConstructionState.add_regist_freeze:(I)V

B029 broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7355
  2: invokespecial #951                // Method local/lito381/server/NativeDeNpcConstructionState.broadcastPacketHP:(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V

B030 claimExpiredCorpseDeletion(J)Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7286
  2: invokespecial #935                // Method local/lito381/server/NativeDeNpcConstructionState.claimExpiredCorpseDeletion:(J)Z

B031 destroyed()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7310
  1: invokespecial #941                // Method local/lito381/server/NativeDeNpcConstructionState.destroyed:()Z

B032 forward(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8765
  2: invokespecial #1302               // Method local/lito381/server/NativeDeNpcConstructionState.forward:(I)V

B033 getAc()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8519
  1: invokespecial #1243               // Method local/lito381/server/NativeDeNpcConstructionState.getAc:()I

B034 getAddAttrKind()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7826
  1: invokespecial #1069               // Method local/lito381/server/NativeDeNpcConstructionState.getAddAttrKind:()I

B035 getAtkspeed()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7082
  1: invokespecial #885                // Method local/lito381/server/NativeDeNpcConstructionState.getAtkspeed:()I

B036 getBowDmgup()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7665
  1: invokespecial #1027               // Method local/lito381/server/NativeDeNpcConstructionState.getBowDmgup:()I

B037 getBowHitup()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7642
  1: invokespecial #1021               // Method local/lito381/server/NativeDeNpcConstructionState.getBowHitup:()I

B038 getBraveSpeed()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7493
  1: invokespecial #983                // Method local/lito381/server/NativeDeNpcConstructionState.getBraveSpeed:()I

B039 getCha()S
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8379
  1: invokespecial #1207               // Method local/lito381/server/NativeDeNpcConstructionState.getCha:()S

B040 getChaLightSize()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7424
  1: invokespecial #965                // Method local/lito381/server/NativeDeNpcConstructionState.getChaLightSize:()I

B041 getCon()S
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8449
  1: invokespecial #1225               // Method local/lito381/server/NativeDeNpcConstructionState.getCon:()S

B042 getCurrentHp()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8634
  1: invokespecial #1269               // Method local/lito381/server/NativeDeNpcConstructionState.getCurrentHp:()I

B043 getCurrentMp()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8623
  1: invokespecial #1266               // Method local/lito381/server/NativeDeNpcConstructionState.getCurrentMp:()I

B044 getDex()S
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8414
  1: invokespecial #1216               // Method local/lito381/server/NativeDeNpcConstructionState.getDex:()S

B045 getDmgup()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8274
  1: invokespecial #1184               // Method local/lito381/server/NativeDeNpcConstructionState.getDmgup:()I

B046 getDolls()Ljava/util/Map;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8080
  1: invokespecial #1139               // Method local/lito381/server/NativeDeNpcConstructionState.getDolls:()Ljava/util/Map;

B047 getEarth()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7849
  1: invokespecial #1075               // Method local/lito381/server/NativeDeNpcConstructionState.getEarth:()I

B048 getExp()J
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8058
  1: invokespecial #1132               // Method local/lito381/server/NativeDeNpcConstructionState.getExp:()J

B049 getFire()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7872
  1: invokespecial #1081               // Method local/lito381/server/NativeDeNpcConstructionState.getFire:()I

B050 getGfxId()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8114
  1: invokespecial #1146               // Method local/lito381/server/NativeDeNpcConstructionState.getGfxId:()I

B051 getHeading()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7539
  1: invokespecial #994                // Method local/lito381/server/NativeDeNpcConstructionState.getHeading:()I

B052 getHitup()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8251
  1: invokespecial #1178               // Method local/lito381/server/NativeDeNpcConstructionState.getHitup:()I

B053 getId()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7252
  1: invokespecial #926                // Method local/lito381/server/NativeDeNpcConstructionState.getId:()I

B054 getInt()S
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8344
  1: invokespecial #1200               // Method local/lito381/server/NativeDeNpcConstructionState.getInt:()S

B055 getKarma()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7459
  1: invokespecial #974                // Method local/lito381/server/NativeDeNpcConstructionState.getKarma:()I

B056 getKnownObjects()Ljava/util/List;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8023
  1: invokespecial #1123               // Method local/lito381/server/NativeDeNpcConstructionState.getKnownObjects:()Ljava/util/List;

B057 getKnownPlayers()Ljava/util/List;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8012
  1: invokespecial #1119               // Method local/lito381/server/NativeDeNpcConstructionState.getKnownPlayers:()Ljava/util/List;

B058 getLawful()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8160
  1: invokespecial #1156               // Method local/lito381/server/NativeDeNpcConstructionState.getLawful:()I

B059 getLevel()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8612
  1: invokespecial #1265               // Method local/lito381/server/NativeDeNpcConstructionState.getLevel:()I

B060 getLightSize()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6944
  1: invokespecial #852                // Method local/lito381/server/NativeDeNpcConstructionState.getLightSize:()I

B061 getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8693
  2: invokespecial #1283               // Method local/lito381/server/NativeDeNpcConstructionState.getLineDistance:(Llocal/lito381/server/NativeDeObjectFields;)D

B062 getLocation()Llocal/lito381/server/NativeDeWorldLocation;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8705
  1: invokespecial #1287               // Method local/lito381/server/NativeDeNpcConstructionState.getLocation:()Llocal/lito381/server/NativeDeWorldLocation;

B063 getMap()Llocal/lito381/server/NativeDeWorldMap;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8716
  1: invokespecial #1291               // Method local/lito381/server/NativeDeNpcConstructionState.getMap:()Llocal/lito381/server/NativeDeWorldMap;

B064 getMaxHp()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8589
  1: invokespecial #1261               // Method local/lito381/server/NativeDeNpcConstructionState.getMaxHp:()I

B065 getMaxMp()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8554
  1: invokespecial #1252               // Method local/lito381/server/NativeDeNpcConstructionState.getMaxMp:()I

B066 getMoveSpeed()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7516
  1: invokespecial #989                // Method local/lito381/server/NativeDeNpcConstructionState.getMoveSpeed:()I

B067 getMr()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7596
  1: invokespecial #1009               // Method local/lito381/server/NativeDeNpcConstructionState.getMr:()I

B068 getNameId()Ljava/lang/String;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7036
  1: invokespecial #876                // Method local/lito381/server/NativeDeNpcConstructionState.getNameId:()Ljava/lang/String;

B069 getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7047
  1: invokespecial #877                // Method local/lito381/server/NativeDeNpcConstructionState.getNpcTemplate:()Llocal/lito381/server/NativeDeNpcTemplate;

B070 getOwnLightSize()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7401
  1: invokespecial #959                // Method local/lito381/server/NativeDeNpcConstructionState.getOwnLightSize:()I

B071 getPassispeed()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7105
  1: invokespecial #887                // Method local/lito381/server/NativeDeNpcConstructionState.getPassispeed:()I

B072 getPetList()Ljava/util/Map;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8091
  1: invokespecial #1140               // Method local/lito381/server/NativeDeNpcConstructionState.getPetList:()Ljava/util/Map;

B073 getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8069
  1: invokespecial #1135               // Method local/lito381/server/NativeDeNpcConstructionState.getPoison:()Llocal/lito381/server/NativeDePerceptionPorts$Poison;

B074 getRegistBlind()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7688
  1: invokespecial #1033               // Method local/lito381/server/NativeDeNpcConstructionState.getRegistBlind:()I

B075 getRegistFreeze()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7734
  1: invokespecial #1045               // Method local/lito381/server/NativeDeNpcConstructionState.getRegistFreeze:()I

B076 getRegistSleep()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7757
  1: invokespecial #1051               // Method local/lito381/server/NativeDeNpcConstructionState.getRegistSleep:()I

B077 getRegistStone()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7780
  1: invokespecial #1057               // Method local/lito381/server/NativeDeNpcConstructionState.getRegistStone:()I

B078 getRegistStun()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7803
  1: invokespecial #1063               // Method local/lito381/server/NativeDeNpcConstructionState.getRegistStun:()I

B079 getRegistSustain()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7711
  1: invokespecial #1039               // Method local/lito381/server/NativeDeNpcConstructionState.getRegistSustain:()I

B080 getSp()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8228
  1: invokespecial #1172               // Method local/lito381/server/NativeDeNpcConstructionState.getSp:()I

B081 getStatus()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8183
  1: invokespecial #1162               // Method local/lito381/server/NativeDeNpcConstructionState.getStatus:()I

B082 getStr()S
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8484
  1: invokespecial #1234               // Method local/lito381/server/NativeDeNpcConstructionState.getStr:()S

B083 getStunSuccess()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7619
  1: invokespecial #1015               // Method local/lito381/server/NativeDeNpcConstructionState.getStunSuccess:()I

B084 getTempCharGfx()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8137
  1: invokespecial #1152               // Method local/lito381/server/NativeDeNpcConstructionState.getTempCharGfx:()I

B085 getTempLawful()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6921
  1: invokespecial #846                // Method local/lito381/server/NativeDeNpcConstructionState.getTempLawful:()I

B086 getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8669
  2: invokespecial #1276               // Method local/lito381/server/NativeDeNpcConstructionState.getTileDistance:(Llocal/lito381/server/NativeDeObjectFields;)I

B087 getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8681
  2: invokespecial #1280               // Method local/lito381/server/NativeDeNpcConstructionState.getTileLineDistance:(Llocal/lito381/server/NativeDeObjectFields;)I

B088 getTitle()Ljava/lang/String;
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7562
  1: invokespecial #1000               // Method local/lito381/server/NativeDeNpcConstructionState.getTitle:()Ljava/lang/String;

B089 getTrueMr()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7585
  1: invokespecial #1006               // Method local/lito381/server/NativeDeNpcConstructionState.getTrueMr:()I

B090 getTrueSp()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8217
  1: invokespecial #1169               // Method local/lito381/server/NativeDeNpcConstructionState.getTrueSp:()I

B091 getWater()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7895
  1: invokespecial #1087               // Method local/lito381/server/NativeDeNpcConstructionState.getWater:()I

B092 getWeaponBreakStack()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7344
  1: invokespecial #948                // Method local/lito381/server/NativeDeNpcConstructionState.getWeaponBreakStack:()I

B093 getWind()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7918
  1: invokespecial #1093               // Method local/lito381/server/NativeDeNpcConstructionState.getWind:()I

B094 getWis()S
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8309
  1: invokespecial #1193               // Method local/lito381/server/NativeDeNpcConstructionState.getWis:()S

B095 get_deadTimerTemp()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7263
  1: invokespecial #929                // Method local/lito381/server/NativeDeNpcConstructionState.get_deadTimerTemp:()I

B096 get_fishX()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7184
  1: invokespecial #907                // Method local/lito381/server/NativeDeNpcConstructionState.get_fishX:()I

B097 get_fishY()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7173
  1: invokespecial #904                // Method local/lito381/server/NativeDeNpcConstructionState.get_fishY:()I

B098 get_showId()I
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7241
  1: invokespecial #923                // Method local/lito381/server/NativeDeNpcConstructionState.get_showId:()I

B099 hasSkillEffect(I)Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7206
  2: invokespecial #913                // Method local/lito381/server/NativeDeNpcConstructionState.hasSkillEffect:(I)Z

B100 isAgro()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7013
  1: invokespecial #870                // Method local/lito381/server/NativeDeNpcConstructionState.isAgro:()Z

B101 isAgrocoi()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6990
  1: invokespecial #864                // Method local/lito381/server/NativeDeNpcConstructionState.isAgrocoi:()Z

B102 isAgrososc()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6967
  1: invokespecial #858                // Method local/lito381/server/NativeDeNpcConstructionState.isAgrososc:()Z

B103 isAiRunning()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7151
  1: invokespecial #898                // Method local/lito381/server/NativeDeNpcConstructionState.isAiRunning:()Z

B104 isDead()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8194
  1: invokespecial #1165               // Method local/lito381/server/NativeDeNpcConstructionState.isDead:()Z

B105 isFishing()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7195
  1: invokespecial #910                // Method local/lito381/server/NativeDeNpcConstructionState.isFishing:()Z

B106 isInvisble()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7470
  1: invokespecial #977                // Method local/lito381/server/NativeDeNpcConstructionState.isInvisble:()Z

B107 isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7952
  2: invokespecial #1102               // Method local/lito381/server/NativeDeNpcConstructionState.isKnownNpcViewerConsistent:(Llocal/lito381/server/NativeDeDeletionState;)Z

B108 isParalyzed()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7367
  1: invokespecial #954                // Method local/lito381/server/NativeDeNpcConstructionState.isParalyzed:()Z

B109 isShop()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7140
  1: invokespecial #895                // Method local/lito381/server/NativeDeNpcConstructionState.isShop:()Z

B110 isSleeped()Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7378
  1: invokespecial #955                // Method local/lito381/server/NativeDeNpcConstructionState.isSleeped:()Z

B111 knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8034
  2: invokespecial #1126               // Method local/lito381/server/NativeDeNpcConstructionState.knownsObject:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z

B112 onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8657
  2: invokespecial #1273               // Method local/lito381/server/NativeDeNpcConstructionState.onAction:(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V

B113 onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7218
  2: invokespecial #917                // Method local/lito381/server/NativeDeNpcConstructionState.onPerceive:(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V

B114 onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8645
  2: invokespecial #1270               // Method local/lito381/server/NativeDeNpcConstructionState.onTalkAction:(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V

B115 rebindWorldMap(S)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8814
  2: invokespecial #1315               // Method local/lito381/server/NativeDeNpcConstructionState.rebindWorldMap:(S)V

B116 removeAllKnownObjects()V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7941
  1: invokespecial #1099               // Method local/lito381/server/NativeDeNpcConstructionState.removeAllKnownObjects:()V

B117 removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7976
  2: invokespecial #1110               // Method local/lito381/server/NativeDeNpcConstructionState.removeKnownObject:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V

B118 removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7964
  2: invokespecial #1106               // Method local/lito381/server/NativeDeNpcConstructionState.removeKnownObjectIfPresent:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z

B119 resurrect(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7298
  2: invokespecial #938                // Method local/lito381/server/NativeDeNpcConstructionState.resurrect:(I)V

B120 setAc(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8507
  2: invokespecial #1240               // Method local/lito381/server/NativeDeNpcConstructionState.setAc:(I)V

B121 setAddAttrKind(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7814
  2: invokespecial #1066               // Method local/lito381/server/NativeDeNpcConstructionState.setAddAttrKind:(I)V

B122 setAgro(Z)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7001
  2: invokespecial #867                // Method local/lito381/server/NativeDeNpcConstructionState.setAgro:(Z)V

B123 setAgrocoi(Z)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6978
  2: invokespecial #861                // Method local/lito381/server/NativeDeNpcConstructionState.setAgrocoi:(Z)V

B124 setAgrososc(Z)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6955
  2: invokespecial #855                // Method local/lito381/server/NativeDeNpcConstructionState.setAgrososc:(Z)V

B125 setAtkspeed(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7070
  2: invokespecial #884                // Method local/lito381/server/NativeDeNpcConstructionState.setAtkspeed:(I)V

B126 setBraveSpeed(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7481
  2: invokespecial #980                // Method local/lito381/server/NativeDeNpcConstructionState.setBraveSpeed:(I)V

B127 setCha(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8367
  2: invokespecial #1204               // Method local/lito381/server/NativeDeNpcConstructionState.setCha:(I)V

B128 setChaLightSize(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7412
  2: invokespecial #962                // Method local/lito381/server/NativeDeNpcConstructionState.setChaLightSize:(I)V

B129 setCon(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8437
  2: invokespecial #1222               // Method local/lito381/server/NativeDeNpcConstructionState.setCon:(I)V

B130 setDex(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8402
  2: invokespecial #1213               // Method local/lito381/server/NativeDeNpcConstructionState.setDex:(I)V

B131 setExp(J)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8046
  2: invokespecial #1129               // Method local/lito381/server/NativeDeNpcConstructionState.setExp:(J)V

B132 setGfxId(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8102
  2: invokespecial #1143               // Method local/lito381/server/NativeDeNpcConstructionState.setGfxId:(I)V

B133 setGfxidInStatus(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7116
  2: invokespecial #888                // Method local/lito381/server/NativeDeNpcConstructionState.setGfxidInStatus:(I)V

B134 setHeading(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7527
  2: invokespecial #991                // Method local/lito381/server/NativeDeNpcConstructionState.setHeading:(I)V

B135 setId(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8850
  2: invokespecial #1325               // Method local/lito381/server/NativeDeNpcConstructionState.setId:(I)V

B136 setInt(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8332
  2: invokespecial #1199               // Method local/lito381/server/NativeDeNpcConstructionState.setInt:(I)V

B137 setKarma(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7447
  2: invokespecial #971                // Method local/lito381/server/NativeDeNpcConstructionState.setKarma:(I)V

B138 setLawful(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8148
  2: invokespecial #1153               // Method local/lito381/server/NativeDeNpcConstructionState.setLawful:(I)V

B139 setLevel(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8600
  2: invokespecial #1264               // Method local/lito381/server/NativeDeNpcConstructionState.setLevel:(I)V

B140 setLightSize(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6932
  2: invokespecial #849                // Method local/lito381/server/NativeDeNpcConstructionState.setLightSize:(I)V

B141 setLocation(III)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8739
  4: invokespecial #1295               // Method local/lito381/server/NativeDeNpcConstructionState.setLocation:(III)V

B142 setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8753
  2: invokespecial #1299               // Method local/lito381/server/NativeDeNpcConstructionState.setLocation:(Llocal/lito381/server/NativeDeWorldLocation;)V

B143 setMap(Llocal/lito381/server/NativeDeWorldMap;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8826
  2: invokespecial #1319               // Method local/lito381/server/NativeDeNpcConstructionState.setMap:(Llocal/lito381/server/NativeDeWorldMap;)V

B144 setMap(S)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8838
  2: invokespecial #1323               // Method local/lito381/server/NativeDeNpcConstructionState.setMap:(S)V

B145 setMaxHp(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8577
  2: invokespecial #1258               // Method local/lito381/server/NativeDeNpcConstructionState.setMaxHp:(I)V

B146 setMaxMp(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8542
  2: invokespecial #1249               // Method local/lito381/server/NativeDeNpcConstructionState.setMaxMp:(I)V

B147 setMoveSpeed(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7504
  2: invokespecial #986                // Method local/lito381/server/NativeDeNpcConstructionState.setMoveSpeed:(I)V

B148 setMr(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7435
  2: invokespecial #968                // Method local/lito381/server/NativeDeNpcConstructionState.setMr:(I)V

B149 setName(Ljava/lang/String;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7929
  2: invokespecial #1096               // Method local/lito381/server/NativeDeNpcConstructionState.setName:(Ljava/lang/String;)V

B150 setNameId(Ljava/lang/String;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7024
  2: invokespecial #873                // Method local/lito381/server/NativeDeNpcConstructionState.setNameId:(Ljava/lang/String;)V

B151 setOwnLightSize(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7389
  2: invokespecial #956                // Method local/lito381/server/NativeDeNpcConstructionState.setOwnLightSize:(I)V

B152 setPassispeed(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7093
  2: invokespecial #886                // Method local/lito381/server/NativeDeNpcConstructionState.setPassispeed:(I)V

B153 setPickupItem(Z)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7058
  2: invokespecial #881                // Method local/lito381/server/NativeDeNpcConstructionState.setPickupItem:(Z)V

B154 setStatus(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8171
  2: invokespecial #1159               // Method local/lito381/server/NativeDeNpcConstructionState.setStatus:(I)V

B155 setStr(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8472
  2: invokespecial #1231               // Method local/lito381/server/NativeDeNpcConstructionState.setStr:(I)V

B156 setTempCharGfx(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8125
  2: invokespecial #1149               // Method local/lito381/server/NativeDeNpcConstructionState.setTempCharGfx:(I)V

B157 setTempLawful(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:6909
  2: invokespecial #843                // Method local/lito381/server/NativeDeNpcConstructionState.setTempLawful:(I)V

B158 setTitle(Ljava/lang/String;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7550
  2: invokespecial #997                // Method local/lito381/server/NativeDeNpcConstructionState.setTitle:(Ljava/lang/String;)V

B159 setWeaponBreakStack(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7321
  2: invokespecial #942                // Method local/lito381/server/NativeDeNpcConstructionState.setWeaponBreakStack:(I)V

B160 setWis(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8297
  2: invokespecial #1190               // Method local/lito381/server/NativeDeNpcConstructionState.setWis:(I)V

B161 setX(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8802
  2: invokespecial #1312               // Method local/lito381/server/NativeDeNpcConstructionState.setX:(I)V

B162 setXY(II)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8777
  3: invokespecial #1305               // Method local/lito381/server/NativeDeNpcConstructionState.setXY:(II)V

B163 setY(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8790
  2: invokespecial #1309               // Method local/lito381/server/NativeDeNpcConstructionState.setY:(I)V

B164 set_deadTimerTemp(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7274
  2: invokespecial #932                // Method local/lito381/server/NativeDeNpcConstructionState.set_deadTimerTemp:(I)V

B165 set_showId(I)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:8727
  2: invokespecial #1292               // Method local/lito381/server/NativeDeNpcConstructionState.set_showId:(I)V

B166 setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7128
  2: invokespecial #891                // Method local/lito381/server/NativeDeNpcConstructionState.setting_template:(Llocal/lito381/server/NativeDeNpcTemplate;)V

B167 sourceDeleteDe()V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7230
  1: invokespecial #920                // Method local/lito381/server/NativeDeNpcConstructionState.sourceDeleteDe:()V

B168 start_shop()V
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7162
  1: invokespecial #901                // Method local/lito381/server/NativeDeNpcConstructionState.start_shop:()V

B169 tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  staged-instance.javap.txt:7988
  2: invokespecial #1113               // Method local/lito381/server/NativeDeNpcConstructionState.tryAddKnownObject:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
```

### 正式全部 426 個相符參照

`@insn` 是 ASM 的有效指令序號；opcode 182=invokevirtual、185=invokeinterface、183=invokespecial，HANDLE H5 為 virtual method handle。本表完整保留 caller 與 receiver owner，避免把介面或父型別參照宣稱成執行期 Instance 證明。

```text
addDmgup(I)V	local/lito381/server/NativeDeCharacterActor#addDmgup(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#addDmgup(I)V
addDmgup(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=572 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#addDmgup(I)V
addDmgup(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=262 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addDmgup(I)V
addEarth(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=293 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addEarth(I)V
addEarth(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=309 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addEarth(I)V
addFire(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=297 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addFire(I)V
addFire(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=313 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addFire(I)V
addHitup(I)V	local/lito381/server/NativeDeCharacterActor#addHitup(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#addHitup(I)V
addHitup(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=577 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#addHitup(I)V
addHitup(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=256 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addHitup(I)V
addSp(I)V	local/lito381/server/NativeDeCharacterActor#addSp(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#addSp(I)V
addSp(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=568 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#addSp(I)V
addWater(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=301 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addWater(I)V
addWater(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=317 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addWater(I)V
addWind(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=305 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addWind(I)V
addWind(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=321 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#addWind(I)V
broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V	local/lito381/server/NativeDeVisibilityAccess#L1Character$broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
claimExpiredCorpseDeletion(J)Z	local/lito381/server/NativeDeCorpseCore$Port#claim(Llocal/lito381/server/NativeDeCorpseCore$Npc;JLjava/lang/Object;)Z @insn=2 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#claimExpiredCorpseDeletion(J)Z
claimExpiredCorpseDeletion(J)Z	local/lito381/server/NativeDeCorpseTimers$Owner$1#claim(Llocal/lito381/server/NativeDeCorpseCore$Npc;JLjava/lang/Object;)Z @insn=27 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#claimExpiredCorpseDeletion(J)Z
destroyed()Z	local/lito381/server/NativeDeChatJobs#admits(Llocal/lito381/server/NativeDeChatJobs$Owner;Llocal/lito381/server/NativeDeNpcConstructionState;)Z @insn=13 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeChatWire#requirePublication(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V @insn=26 OPCODE=182 local/lito381/server/NativeDeDeletionState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeCorpseCore#run()V @insn=48 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeCorpseCore$Task#run()V @insn=5 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeDeletionState#sourceAuditDeletionStalls()V @insn=108 OPCODE=182 local/lito381/server/NativeDeDeletionState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeInstanceState#isCurrentDeathGeneration(J)Z @insn=14 OPCODE=182 local/lito381/server/NativeDeInstanceState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeInstanceState#sourceAi$startAI()V @insn=4 OPCODE=182 local/lito381/server/NativeDeInstanceState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeInstanceState#sourceAi$runAiTick(J)V @insn=17 OPCODE=182 local/lito381/server/NativeDeInstanceState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeLiveAiJobs#admits(Llocal/lito381/server/NativeDeInstanceState;)Z @insn=23 OPCODE=182 local/lito381/server/NativeDeInstanceState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeLiveAiJobs#retireDeleted(Llocal/lito381/server/NativeDeInstanceState;)V @insn=4 OPCODE=182 local/lito381/server/NativeDeInstanceState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeNpcKnowledge#admitted(Llocal/lito381/server/NativeDeDeletionState;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z @insn=9 OPCODE=182 local/lito381/server/NativeDeDeletionState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeNpcVitalState#startHpRegeneration()V @insn=1 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeNpcVitalState#startMpRegeneration()V @insn=1 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDePerceptionJobs#current(Llocal/lito381/server/NativeDePerceptionState;Llocal/lito381/server/NativeDeClientViewer;)Z @insn=4 OPCODE=182 local/lito381/server/NativeDePerceptionState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDePerceptionOutput#current(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeDePerceptionState;)Z @insn=20 OPCODE=182 local/lito381/server/NativeDePerceptionState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeVisibilityAccess#L1NpcInstance$destroyed(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z @insn=2 OPCODE=182 local/lito381/server/NativeDeDeletionState#destroyed()Z
destroyed()Z	local/lito381/server/NativeDeVitalState#isCurrentDeathGeneration(J)Z @insn=14 OPCODE=182 local/lito381/server/NativeDeVitalState#destroyed()Z
getAtkspeed()I	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=34 OPCODE=182 local/lito381/server/NativeDeInstanceState#getAtkspeed()I
getBraveSpeed()I	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=52 OPCODE=182 local/lito381/server/NativeDePerceptionState#getBraveSpeed()I
getChaLightSize()I	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=83 OPCODE=182 local/lito381/server/NativeDePerceptionState#getChaLightSize()I
getCurrentHp()I	local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt#<init>(Llocal/lito381/server/NativeDeCharacterFields;)V @insn=7 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=613 OPCODE=182 local/lito381/server/NativeDeDeletionState#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeInstanceState#lambda$commitDamageState$1(I)Ljava/lang/Long; @insn=7 OPCODE=182 local/lito381/server/NativeDeInstanceState#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeInstanceState#sourceAi$startAI()V @insn=7 OPCODE=182 local/lito381/server/NativeDeInstanceState#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeInstanceState#sourceAi$runAiTick(J)V @insn=20 OPCODE=182 local/lito381/server/NativeDeInstanceState#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeNpcVitalState#startHpRegeneration()V @insn=8 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeNpcVitalState#startMpRegeneration()V @insn=8 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeVisibilityRuntime#hp(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Z)I @insn=72 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getCurrentHp()I
getCurrentHp()I	local/lito381/server/NativeDeVitalState#lambda$commitDamageState$0(I)Ljava/lang/Long; @insn=7 OPCODE=182 local/lito381/server/NativeDeVitalState#getCurrentHp()I
getDolls()Ljava/util/Map;	local/lito381/server/NativeDeCharacterActor#getDolls()Ljava/util/Map; @insn=2 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getDolls()Ljava/util/Map;
getDolls()Ljava/util/Map;	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=287 OPCODE=182 local/lito381/server/NativeDeDeletionState#getDolls()Ljava/util/Map;
getDolls()Ljava/util/Map;	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=291 OPCODE=182 local/lito381/server/NativeDeDeletionState#getDolls()Ljava/util/Map;
getDolls()Ljava/util/Map;	local/lito381/server/NativeDeInitializerKernel#initDoll(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;)V @insn=1 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getDolls()Ljava/util/Map;
getDolls()Ljava/util/Map;	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=45 OPCODE=182 local/lito381/server/NativeDeInstanceState#getDolls()Ljava/util/Map;
getDolls()Ljava/util/Map;	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=49 OPCODE=182 local/lito381/server/NativeDeInstanceState#getDolls()Ljava/util/Map;
getHeading()I	local/lito381/server/NativeDeCharacterActor#getHeading()I @insn=2 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getHeading()I
getHeading()I	local/lito381/server/NativeDeInitializerKernel#initPet(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;)V @insn=53 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getHeading()I
getHeading()I	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=79 OPCODE=182 local/lito381/server/NativeDePerceptionState#getHeading()I
getId()I	local/lito381/server/NativeDeCharacterActor#<init>(Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeCharacterFields$Snapshot;)V @insn=11 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeCharacterActor#<init>(Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V @insn=11 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeCharacterActor#<init>(Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V @insn=11 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeCharacterActor#getId()I @insn=2 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getId()I
getId()I	local/lito381/server/NativeDeCharacterFields#resurrect(I)V @insn=35 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getId()I
getId()I	local/lito381/server/NativeDeChatAccess#L1NpcInstance$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#getId()I
getId()I	local/lito381/server/NativeDeChatAccess#L1PcInstance$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getId()I
getId()I	local/lito381/server/NativeDeChatWire#content(Llocal/lito381/server/NativeDeVisibilityPacket;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)[B @insn=48 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getId()I
getId()I	local/lito381/server/NativeDeClientViewer#sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V @insn=59 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeClientViewer#sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V @insn=103 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore#run()V @insn=107 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore#dispatch(Llocal/lito381/server/NativeDeCorpseCore$Task;I)V @insn=65 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore#dispatch(Llocal/lito381/server/NativeDeCorpseCore$Task;I)V @insn=117 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore#dispatch(Llocal/lito381/server/NativeDeCorpseCore$Task;I)V @insn=154 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore#dispatch(Llocal/lito381/server/NativeDeCorpseCore$Task;I)V @insn=195 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore$Task#run()V @insn=27 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore$Task#run()V @insn=82 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseCore$Task#discardOnShutdown()V @insn=16 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getId()I
getId()I	local/lito381/server/NativeDeCorpseState#getId()I @insn=1 OPCODE=183 local/lito381/server/NativeDeResurrectableState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceBeginDeletionObservation()V @insn=11 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceResetDeletionObservation()V @insn=4 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceFinishDeletionObservation(Llocal/lito381/server/NativeDeDeletionVisibility$Phase;)V @insn=7 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceAuditDeletionStalls()V @insn=26 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceAuditDeletionStalls()V @insn=58 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceAuditDeletionStalls()V @insn=74 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceAuditDeletionStalls()V @insn=152 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceSnapshotRemovalRecipients()Ljava/util/List; @insn=55 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=26 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=54 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=86 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=94 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=152 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=158 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=174 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=190 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=254 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=277 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=321 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=367 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=419 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=453 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=500 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=519 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=553 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=556 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=607 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=727 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=753 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=801 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeDeletionState#sourceAbortFailedWorldRegistration()V @insn=14 OPCODE=182 local/lito381/server/NativeDeDeletionState#getId()I
getId()I	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=289 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=319 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=389 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=702 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=750 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getId()I
getId()I	local/lito381/server/NativeDeInstanceScene#visual(II)Llocal/lito381/server/NativeDeInitializerKernel$Packet; @insn=3 OPCODE=182 local/lito381/server/NativeDeInstanceState#getId()I
getId()I	local/lito381/server/NativeDeInventoryIdentity#requireWorldIdentity(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V @insn=10 OPCODE=182 local/lito381/server/NativeDeObjectFields#getId()I
getId()I	local/lito381/server/NativeDeNpcVitalState#resurrect(I)V @insn=13 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#getId()I
getId()I	local/lito381/server/NativeDeNpcVitalState#resurrect(I)V @insn=51 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#getId()I
getId()I	local/lito381/server/NativeDeNpcVitalState#startDeleteTimer(I)V @insn=13 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#getId()I
getId()I	local/lito381/server/NativeDeObjectFields#setId(I)V @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getId()I
getId()I	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=67 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionOutput#send(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDePerceptionPacket;)V @insn=44 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionOutput#send(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDePerceptionPacket;)V @insn=96 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=50 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=62 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=74 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionState#sendSpeedStatusTo(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=10 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionState#sendSpeedStatusTo(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=51 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDePerceptionState#sendSpeedStatusTo(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=63 OPCODE=182 local/lito381/server/NativeDePerceptionState#getId()I
getId()I	local/lito381/server/NativeDeVisibilityAccess#L1Character$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getId()I
getId()I	local/lito381/server/NativeDeVisibilityAccess#L1NpcInstance$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getId()I
getId()I	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getId()I
getId()I	local/lito381/server/NativeDeVisibilityOutput#send(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V @insn=110 OPCODE=182 local/lito381/server/NativeDeObjectFields#getId()I
getId()I	local/lito381/server/NativeDeWorldAccess#id(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getId()I
getInt()S	local/lito381/server/NativeDeCharacterActor#getInt()S @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getInt()S
getInt()S	local/lito381/server/NativeDeCharacterFields#getMagicBonus()I @insn=1 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getInt()S
getInt()S	local/lito381/server/NativeDeInitializerKernel#start(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=13 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getInt()S
getInt()S	local/lito381/server/NativeDeInstanceState#startNpc()V @insn=15 OPCODE=182 local/lito381/server/NativeDeInstanceState#getInt()S
getInt()S	local/lito381/server/NativeDeInstanceState#getMagicBonus()I @insn=1 OPCODE=182 local/lito381/server/NativeDeInstanceState#getInt()S
getKnownObjects()Ljava/util/List;	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$getKnownObjects(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/util/List; @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getKnownObjects()Ljava/util/List;
getLawful()I	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=94 OPCODE=182 local/lito381/server/NativeDePerceptionState#getLawful()I
getLevel()I	local/lito381/server/NativeDeCharacterActor#getLevel()I @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getLevel()I
getLevel()I	local/lito381/server/NativeDeCharacterFields#getMagicLevel()I @insn=1 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getLevel()I
getLevel()I	local/lito381/server/NativeDeCharacterFields#snapshot()Llocal/lito381/server/NativeDeCharacterFields$Snapshot; @insn=10 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getLevel()I
getLevel()I	local/lito381/server/NativeDeInitializerKernel#start(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=7 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getLevel()I
getLevel()I	local/lito381/server/NativeDeInstanceState#startNpc()V @insn=9 OPCODE=182 local/lito381/server/NativeDeInstanceState#getLevel()I
getLevel()I	local/lito381/server/NativeDeInstanceState#getMagicLevel()I @insn=1 OPCODE=182 local/lito381/server/NativeDeInstanceState#getLevel()I
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectBindings#attachLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeWorldContext;IIII)Llocal/lito381/server/NativeDeWorldLocation; @insn=4 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectBindings#x(Llocal/lito381/server/NativeDeObjectFields;)I @insn=1 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectBindings#y(Llocal/lito381/server/NativeDeObjectFields;)I @insn=1 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectBindings#mapId(Llocal/lito381/server/NativeDeObjectFields;)S @insn=1 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectFields#getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D @insn=1 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectFields#getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D @insn=3 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectFields#getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I @insn=1 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectFields#getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I @insn=3 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectFields#getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I @insn=1 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeObjectFields#getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I @insn=3 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeVisibilityAccess#L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation; @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation; @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation; @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1Object$getMap(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldMap; @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1Object$getX(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1Object$getY(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1PcInstance$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation; @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1PcInstance$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1TrapInstance$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1TrapInstance$getX(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getLocation()Llocal/lito381/server/NativeDeWorldLocation;	local/lito381/server/NativeDeWorldAccess#L1TrapInstance$getY(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#getLocation()Llocal/lito381/server/NativeDeWorldLocation;
getMap()Llocal/lito381/server/NativeDeWorldMap;	local/lito381/server/NativeDeInstanceState#sourceAi$checkSpawnLocation()V @insn=1 OPCODE=182 local/lito381/server/NativeDeInstanceState#getMap()Llocal/lito381/server/NativeDeWorldMap;
getMap()Llocal/lito381/server/NativeDeWorldMap;	local/lito381/server/NativeDeInstanceState#sourceAi$checkSpawnLocation()V @insn=39 OPCODE=182 local/lito381/server/NativeDeInstanceState#getMap()Llocal/lito381/server/NativeDeWorldMap;
getMap()Llocal/lito381/server/NativeDeWorldMap;	local/lito381/server/NativeDeLiveAiJobs#pulse(Llocal/lito381/server/NativeDeLiveAiJobs$Owner;)V @insn=64 OPCODE=182 local/lito381/server/NativeDeInstanceState#getMap()Llocal/lito381/server/NativeDeWorldMap;
getMaxHp()I	local/lito381/server/NativeDeCharacterFields#commitCurrentHpLocked(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; @insn=16 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeCharacterFields#lambda$adjustCurrentHpIfAlive$4(JI)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; @insn=24 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeCharacterFields#lambda$adjustCurrentHp$3(JI)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeCorpseCore#run()V @insn=24 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=137 OPCODE=182 local/lito381/server/NativeDeDeletionState#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=619 OPCODE=182 local/lito381/server/NativeDeDeletionState#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeInstanceState#lambda$commitDamageState$1(I)Ljava/lang/Long; @insn=42 OPCODE=182 local/lito381/server/NativeDeInstanceState#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeVisibilityRuntime#hp(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Z)I @insn=69 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getMaxHp()I
getMaxHp()I	local/lito381/server/NativeDeVitalState#lambda$commitDamageState$0(I)Ljava/lang/Long; @insn=42 OPCODE=182 local/lito381/server/NativeDeVitalState#getMaxHp()I
getMaxMp()I	local/lito381/server/NativeDeCharacterFields#commitCurrentMpLocked(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation; @insn=16 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getMaxMp()I
getMaxMp()I	local/lito381/server/NativeDeCharacterFields#lambda$adjustCurrentMp$5(J)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation; @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getMaxMp()I
getMoveSpeed()I	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=87 OPCODE=182 local/lito381/server/NativeDePerceptionState#getMoveSpeed()I
getMoveSpeed()I	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=1 OPCODE=182 local/lito381/server/NativeDePerceptionState#getMoveSpeed()I
getMoveSpeed()I	local/lito381/server/NativeDePerceptionState#sendSpeedStatusTo(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=4 OPCODE=182 local/lito381/server/NativeDePerceptionState#getMoveSpeed()I
getMoveSpeed()I	local/lito381/server/NativeDePerceptionState#sendSpeedStatusTo(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=12 OPCODE=182 local/lito381/server/NativeDePerceptionState#getMoveSpeed()I
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeCharacterActor#getNameId()Ljava/lang/String; @insn=2 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeChatAccess#L1NpcInstance$getNameId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/lang/String; @insn=2 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeDeletionDeState#sourceDeleteDe()V @insn=61 OPCODE=182 local/lito381/server/NativeDeDeletionDeState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeDeletionDeState#sourceDeleteDe()V @insn=105 OPCODE=182 local/lito381/server/NativeDeDeletionDeState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=601 OPCODE=182 local/lito381/server/NativeDeDeletionState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=701 OPCODE=182 local/lito381/server/NativeDeDeletionState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=358 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=361 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=368 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=381 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=386 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=726 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInstanceState#sourceAi$getName()Ljava/lang/String; @insn=1 OPCODE=182 local/lito381/server/NativeDeInstanceState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInstanceState#sourceAi$getName()Ljava/lang/String; @insn=4 OPCODE=182 local/lito381/server/NativeDeInstanceState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeInstanceState#sourceAi$getName()Ljava/lang/String; @insn=8 OPCODE=182 local/lito381/server/NativeDeInstanceState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=98 OPCODE=182 local/lito381/server/NativeDePerceptionState#getNameId()Ljava/lang/String;
getNameId()Ljava/lang/String;	local/lito381/server/NativeDeWorldAccess#L1DeInstance$getNameId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/lang/String; @insn=2 OPCODE=182 local/lito381/server/NativeDeDeletionDeState#getNameId()Ljava/lang/String;
getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;	local/lito381/server/NativeDeChatAccess#L1NpcInstance$getNpcTemplate(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeNpcTemplate; @insn=2 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;	local/lito381/server/NativeDeNpcMobSkillUse#<init>(Llocal/lito381/server/NativeDeNpcTemplateState;)V @insn=25 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;	local/lito381/server/NativeDeNpcMobSkillUse#getDelayNpcId()I @insn=5 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;	local/lito381/server/NativeDeNpcMobSkillUse#getDelayNpcId()I @insn=11 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;	local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeDeletionPorts$Template; @insn=1 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=284 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=288 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
getPassispeed()I	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=23 OPCODE=182 local/lito381/server/NativeDeInstanceState#getPassispeed()I
getPetList()Ljava/util/Map;	local/lito381/server/NativeDeDeletionDeState#sourceDeleteDe()V @insn=7 OPCODE=182 local/lito381/server/NativeDeDeletionDeState#getPetList()Ljava/util/Map;
getPetList()Ljava/util/Map;	local/lito381/server/NativeDeDeletionDeState#sourceDeleteDe()V @insn=11 OPCODE=182 local/lito381/server/NativeDeDeletionDeState#getPetList()Ljava/util/Map;
getPetList()Ljava/util/Map;	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=329 OPCODE=182 local/lito381/server/NativeDeDeletionState#getPetList()Ljava/util/Map;
getPetList()Ljava/util/Map;	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=333 OPCODE=182 local/lito381/server/NativeDeDeletionState#getPetList()Ljava/util/Map;
getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=11 OPCODE=182 local/lito381/server/NativeDePerceptionState#getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=14 OPCODE=182 local/lito381/server/NativeDePerceptionState#getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
getStatus()I	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=75 OPCODE=182 local/lito381/server/NativeDePerceptionState#getStatus()I
getTempCharGfx()I	local/lito381/server/NativeDeCharacterActor#getTempCharGfx()I @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getTempCharGfx()I
getTempCharGfx()I	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=637 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getTempCharGfx()I
getTempCharGfx()I	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=646 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#getTempCharGfx()I
getTempCharGfx()I	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=2 OPCODE=182 local/lito381/server/NativeDeInstanceState#getTempCharGfx()I
getTempCharGfx()I	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=14 OPCODE=182 local/lito381/server/NativeDeInstanceState#getTempCharGfx()I
getTempCharGfx()I	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=71 OPCODE=182 local/lito381/server/NativeDePerceptionState#getTempCharGfx()I
getTitle()Ljava/lang/String;	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=102 OPCODE=182 local/lito381/server/NativeDePerceptionState#getTitle()Ljava/lang/String;
getTrueSp()I	local/lito381/server/NativeDeCharacterFields#getSp()I @insn=1 OPCODE=182 local/lito381/server/NativeDeCharacterFields#getTrueSp()I
get_fishX()I	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=77 OPCODE=182 local/lito381/server/NativeDePerceptionState#get_fishX()I
get_fishY()I	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=79 OPCODE=182 local/lito381/server/NativeDePerceptionState#get_fishY()I
get_showId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=262 OPCODE=182 local/lito381/server/NativeDeDeletionState#get_showId()I
get_showId()I	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=649 OPCODE=182 local/lito381/server/NativeDeDeletionState#get_showId()I
get_showId()I	local/lito381/server/NativeDeVisibilityAccess#L1Object$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#get_showId()I
get_showId()I	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#get_showId()I
get_showId()I	local/lito381/server/NativeDeWorldAccess#L1NpcInstance$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#get_showId()I
get_showId()I	local/lito381/server/NativeDeWorldAccess#L1Object$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I @insn=2 OPCODE=182 local/lito381/server/NativeDeObjectFields#get_showId()I
hasSkillEffect(I)Z	local/lito381/server/NativeDeCharacterFields#getMr()I @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDeCharacterFields#isInvisble()Z @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDeCharacterFields#isInvisble()Z @insn=6 OPCODE=182 local/lito381/server/NativeDeCharacterFields#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=127 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=10 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=14 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=18 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=31 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=35 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=39 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=43 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=50 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=57 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=64 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=67 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDePerceptionSpeed#stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState; @insn=70 OPCODE=182 local/lito381/server/NativeDePerceptionState#hasSkillEffect(I)Z
hasSkillEffect(I)Z	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$hasSkillEffect(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;I)Z @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#hasSkillEffect(I)Z
isAiRunning()Z	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=100 OPCODE=182 local/lito381/server/NativeDePerceptionState#isAiRunning()Z
isDead()Z	local/lito381/server/NativeDeCharacterFields#commitCurrentHpLocked(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; @insn=22 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isDead()Z
isDead()Z	local/lito381/server/NativeDeCharacterFields#lambda$resurrect$18(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; @insn=1 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isDead()Z
isDead()Z	local/lito381/server/NativeDeCharacterFields#lambda$debitCurrentMpUpToIfAlive$7(J)Llocal/lito381/server/NativeDeCharacterFields$MpDebitMutation; @insn=1 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isDead()Z
isDead()Z	local/lito381/server/NativeDeCharacterFields#lambda$adjustCurrentHpIfAlive$4(JI)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; @insn=4 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isDead()Z
isDead()Z	local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt#<init>(Llocal/lito381/server/NativeDeCharacterFields;)V @insn=19 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isDead()Z
isDead()Z	local/lito381/server/NativeDeChatAccess#L1NpcInstance$isDead(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z @insn=2 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#isDead()Z
isDead()Z	local/lito381/server/NativeDeCorpseCore$Task#run()V @insn=9 OPCODE=185 local/lito381/server/NativeDeCorpseCore$Npc#isDead()Z
isDead()Z	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=35 OPCODE=182 local/lito381/server/NativeDeDeletionState#isDead()Z
isDead()Z	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=131 OPCODE=182 local/lito381/server/NativeDeDeletionState#isDead()Z
isDead()Z	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=140 OPCODE=182 local/lito381/server/NativeDeDeletionState#isDead()Z
isDead()Z	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=570 OPCODE=182 local/lito381/server/NativeDeDeletionState#isDead()Z
isDead()Z	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=625 OPCODE=182 local/lito381/server/NativeDeDeletionState#isDead()Z
isDead()Z	local/lito381/server/NativeDeInstanceState#isCurrentDeathGeneration(J)Z @insn=11 OPCODE=182 local/lito381/server/NativeDeInstanceState#isDead()Z
isDead()Z	local/lito381/server/NativeDeInstanceState#lambda$commitDamageState$1(I)Ljava/lang/Long; @insn=1 OPCODE=182 local/lito381/server/NativeDeInstanceState#isDead()Z
isDead()Z	local/lito381/server/NativeDeInstanceState#lambda$setDead$2(Z)V @insn=3 OPCODE=182 local/lito381/server/NativeDeInstanceState#isDead()Z
isDead()Z	local/lito381/server/NativeDeInstanceState#lambda$setDead$2(Z)V @insn=15 OPCODE=182 local/lito381/server/NativeDeInstanceState#isDead()Z
isDead()Z	local/lito381/server/NativeDeInstanceState#sourceAi$startAI()V @insn=1 OPCODE=182 local/lito381/server/NativeDeInstanceState#isDead()Z
isDead()Z	local/lito381/server/NativeDeInstanceState#sourceAi$runAiTick(J)V @insn=14 OPCODE=182 local/lito381/server/NativeDeInstanceState#isDead()Z
isDead()Z	local/lito381/server/NativeDeLiveAiJobs#pulse(Llocal/lito381/server/NativeDeLiveAiJobs$Owner;)V @insn=60 OPCODE=182 local/lito381/server/NativeDeInstanceState#isDead()Z
isDead()Z	local/lito381/server/NativeDeNpcHateList#removeInvalidCharacter(Llocal/lito381/server/NativeDeDeletionState;)V @insn=15 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isDead()Z
isDead()Z	local/lito381/server/NativeDeNpcVitalState#startHpRegeneration()V @insn=4 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeNpcVitalState#startMpRegeneration()V @insn=4 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeNpcVitalState#claimExpiredCorpseDeletion(J)Z @insn=16 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeNpcVitalState#lambda$resurrect$1(I)Ljava/lang/Boolean; @insn=8 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeNpcVitalState#lambda$resurrect$1(I)Ljava/lang/Boolean; @insn=28 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeVitalState#isCurrentDeathGeneration(J)Z @insn=11 OPCODE=182 local/lito381/server/NativeDeVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeVitalState#lambda$setDead$1(Z)V @insn=3 OPCODE=182 local/lito381/server/NativeDeVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeVitalState#lambda$setDead$1(Z)V @insn=15 OPCODE=182 local/lito381/server/NativeDeVitalState#isDead()Z
isDead()Z	local/lito381/server/NativeDeVitalState#lambda$commitDamageState$0(I)Ljava/lang/Long; @insn=1 OPCODE=182 local/lito381/server/NativeDeVitalState#isDead()Z
isFishing()Z	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=68 OPCODE=182 local/lito381/server/NativeDePerceptionState#isFishing()Z
isInvisble()Z	local/lito381/server/NativeDeCharacterFields#getChaLightSize()I @insn=1 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isInvisble()Z
isInvisble()Z	local/lito381/server/NativeDeCharacterFields#getOwnLightSize()I @insn=1 OPCODE=182 local/lito381/server/NativeDeCharacterFields#isInvisble()Z
isInvisble()Z	local/lito381/server/NativeDePerceptionNPCPack_De#<init>(Llocal/lito381/server/NativeDePerceptionState;)V @insn=23 OPCODE=182 local/lito381/server/NativeDePerceptionState#isInvisble()Z
isParalyzed()Z	local/lito381/server/NativeDeInstanceState#sourceAi$runAiTick(J)V @insn=30 OPCODE=182 local/lito381/server/NativeDeInstanceState#isParalyzed()Z
isShop()Z	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=37 OPCODE=182 local/lito381/server/NativeDePerceptionState#isShop()Z
isSleeped()Z	local/lito381/server/NativeDeInstanceState#sourceAi$runAiTick(J)V @insn=33 OPCODE=182 local/lito381/server/NativeDeInstanceState#isSleeped()Z
knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z	local/lito381/server/NativeDeNpcHateList#removeInvalidCharacter(Llocal/lito381/server/NativeDeDeletionState;)V @insn=129 OPCODE=182 local/lito381/server/NativeDeDeletionState#knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V	local/lito381/server/NativeDePerceptionJobs$Task#lambda$run$0()V @insn=14 OPCODE=182 local/lito381/server/NativeDePerceptionState#onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V	local/lito381/server/NativeDeVisibilityPorts$1#perceive(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=8 OPCODE=182 local/lito381/server/NativeDeInstanceState#onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V	local/lito381/server/NativeDeVisibilityPorts$1#perceive(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=21 OPCODE=182 local/lito381/server/NativeDeObjectFields#onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
removeAllKnownObjects()V	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=543 OPCODE=182 local/lito381/server/NativeDeDeletionState#removeAllKnownObjects()V
removeAllKnownObjects()V	local/lito381/server/NativeDeNpcKnowledge#retireWorld(Llocal/lito381/server/NativeDeWorldContext;)V @insn=44 OPCODE=182 local/lito381/server/NativeDeDeletionState#removeAllKnownObjects()V
removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z	local/lito381/server/NativeDeCharacterFields#removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z	local/lito381/server/NativeDeVisibilityAccess#L1PcInstance$removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
resurrect(I)V	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=138 OPCODE=182 local/lito381/server/NativeDeDeletionState#resurrect(I)V
resurrect(I)V	local/lito381/server/NativeDeNpcVitalState#lambda$resurrect$1(I)Ljava/lang/Boolean; @insn=6 OPCODE=183 local/lito381/server/NativeDeCharacterFields#resurrect(I)V
resurrect(I)V	local/lito381/server/NativeDeNpcVitalState#lambda$resurrect$1(I)Ljava/lang/Boolean; @insn=26 OPCODE=183 local/lito381/server/NativeDeCharacterFields#resurrect(I)V
setAc(I)V	local/lito381/server/NativeDeCharacterActor#setAc(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setAc(I)V
setAc(I)V	local/lito381/server/NativeDeCharacterFields#addAc(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setAc(I)V
setAc(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=525 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setAc(I)V
setAc(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=136 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setAc(I)V
setAc(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=156 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setAc(I)V
setAgro(Z)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=274 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setAgro(Z)V
setAgrocoi(Z)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=278 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setAgrocoi(Z)V
setAgrososc(Z)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=282 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setAgrososc(Z)V
setAtkspeed(I)V	local/lito381/server/NativeDeCharacterActor#setAtkspeed(I)V @insn=3 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setAtkspeed(I)V
setAtkspeed(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=656 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setAtkspeed(I)V
setAtkspeed(I)V	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=11 OPCODE=182 local/lito381/server/NativeDeInstanceState#setAtkspeed(I)V
setAtkspeed(I)V	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=38 OPCODE=182 local/lito381/server/NativeDeInstanceState#setAtkspeed(I)V
setAtkspeed(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=270 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setAtkspeed(I)V
setBraveSpeed(I)V	local/lito381/server/NativeDeCharacterActor#setBraveSpeed(I)V @insn=3 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setBraveSpeed(I)V
setBraveSpeed(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=634 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setBraveSpeed(I)V
setBraveSpeed(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=400 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setBraveSpeed(I)V
setBraveSpeed(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=404 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setBraveSpeed(I)V
setCha(I)V	local/lito381/server/NativeDeCharacterActor#setCha(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setCha(I)V
setCha(I)V	local/lito381/server/NativeDeCharacterFields#addCha(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setCha(I)V
setCha(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=564 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setCha(I)V
setCon(I)V	local/lito381/server/NativeDeCharacterActor#setCon(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setCon(I)V
setCon(I)V	local/lito381/server/NativeDeCharacterFields#addCon(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setCon(I)V
setCon(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=546 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setCon(I)V
setCon(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=167 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setCon(I)V
setCon(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=206 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setCon(I)V
setDex(I)V	local/lito381/server/NativeDeCharacterActor#setDex(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setDex(I)V
setDex(I)V	local/lito381/server/NativeDeCharacterFields#addDex(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setDex(I)V
setDex(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=540 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setDex(I)V
setDex(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=171 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setDex(I)V
setDex(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=217 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setDex(I)V
setExp(J)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=343 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setExp(J)V
setExp(J)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=356 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setExp(J)V
setGfxId(I)V	local/lito381/server/NativeDeCharacterActor#setGfxId(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setGfxId(I)V
setGfxId(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=625 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setGfxId(I)V
setGfxId(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=332 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setGfxId(I)V
setGfxidInStatus(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=335 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setGfxidInStatus(I)V
setInt(I)V	local/lito381/server/NativeDeCharacterActor#setInt(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setInt(I)V
setInt(I)V	local/lito381/server/NativeDeCharacterFields#addInt(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setInt(I)V
setInt(I)V	local/lito381/server/NativeDeInitializerKernel#start(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=17 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setInt(I)V
setInt(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=552 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setInt(I)V
setInt(I)V	local/lito381/server/NativeDeInstanceState#startNpc()V @insn=19 OPCODE=182 local/lito381/server/NativeDeInstanceState#setInt(I)V
setInt(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=175 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setInt(I)V
setInt(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=228 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setInt(I)V
setKarma(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=408 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setKarma(I)V
setLawful(I)V	local/lito381/server/NativeDeCharacterActor#setLawful(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setLawful(I)V
setLawful(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=528 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setLawful(I)V
setLawful(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=363 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setLawful(I)V
setLawful(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=387 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setLawful(I)V
setLevel(I)V	local/lito381/server/NativeDeCharacterActor#setLevel(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setLevel(I)V
setLevel(I)V	local/lito381/server/NativeDeInitializerKernel#start(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=11 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setLevel(I)V
setLevel(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=449 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setLevel(I)V
setLevel(I)V	local/lito381/server/NativeDeInstanceState#startNpc()V @insn=13 OPCODE=182 local/lito381/server/NativeDeInstanceState#setLevel(I)V
setLevel(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=30 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setLevel(I)V
setLevel(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=61 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setLevel(I)V
setLightSize(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=412 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setLightSize(I)V
setLocation(III)V	local/lito381/server/NativeDeWorldAccess#L1Object$setLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;III)V @insn=5 OPCODE=182 local/lito381/server/NativeDeObjectFields#setLocation(III)V
setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V	local/lito381/server/NativeDeObjectBindings#attachLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeWorldContext;IIII)Llocal/lito381/server/NativeDeWorldLocation; @insn=15 OPCODE=182 local/lito381/server/NativeDeObjectFields#setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
setMaxHp(I)V	local/lito381/server/NativeDeCharacterActor#setMaxHp(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setMaxHp(I)V
setMaxHp(I)V	local/lito381/server/NativeDeCharacterFields#lambda$addMaxHp$13(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setMaxHp(I)V
setMaxHp(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=482 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setMaxHp(I)V
setMaxHp(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=68 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setMaxHp(I)V
setMaxHp(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=92 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setMaxHp(I)V
setMaxMp(I)V	local/lito381/server/NativeDeCharacterActor#setMaxMp(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setMaxMp(I)V
setMaxMp(I)V	local/lito381/server/NativeDeCharacterFields#lambda$addMaxMp$15(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setMaxMp(I)V
setMaxMp(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=488 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setMaxMp(I)V
setMaxMp(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=102 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setMaxMp(I)V
setMaxMp(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=126 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setMaxMp(I)V
setMoveSpeed(I)V	local/lito381/server/NativeDeCharacterActor#setMoveSpeed(I)V @insn=3 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setMoveSpeed(I)V
setMoveSpeed(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=631 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setMoveSpeed(I)V
setMoveSpeed(I)V	local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=119 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#setMoveSpeed(I)V
setMr(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=183 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setMr(I)V
setMr(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=250 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setMr(I)V
setName(Ljava/lang/String;)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=12 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setName(Ljava/lang/String;)V
setNameId(Ljava/lang/String;)V	local/lito381/server/NativeDeCharacterActor#setNameId(Ljava/lang/String;)V @insn=3 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setNameId(Ljava/lang/String;)V
setNameId(Ljava/lang/String;)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=356 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setNameId(Ljava/lang/String;)V
setNameId(Ljava/lang/String;)V	local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=15 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#setNameId(Ljava/lang/String;)V
setNameId(Ljava/lang/String;)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=23 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setNameId(Ljava/lang/String;)V
setPassispeed(I)V	local/lito381/server/NativeDeCharacterActor#setPassispeed(I)V @insn=3 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setPassispeed(I)V
setPassispeed(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=653 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setPassispeed(I)V
setPassispeed(I)V	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=21 OPCODE=182 local/lito381/server/NativeDeInstanceState#setPassispeed(I)V
setPassispeed(I)V	local/lito381/server/NativeDeInstanceState#sourceAi$setNpcSpeed()V @insn=27 OPCODE=182 local/lito381/server/NativeDeInstanceState#setPassispeed(I)V
setPassispeed(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=266 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setPassispeed(I)V
setPickupItem(Z)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=394 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setPickupItem(Z)V
setStatus(I)V	local/lito381/server/NativeDeCharacterActor#setStatus(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeCharacterFields#lambda$resurrect$18(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; @insn=14 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeInitializerKernel$Actor#status(I)V @insn=2 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeInstanceState#lambda$commitDamageState$1(I)Ljava/lang/Long; @insn=29 OPCODE=182 local/lito381/server/NativeDeInstanceState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeInstanceState#lambda$commitDamageState$1(I)Ljava/lang/Long; @insn=58 OPCODE=182 local/lito381/server/NativeDeInstanceState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=116 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=125 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeNpcTemplateState#setGfxidInStatus(I)V @insn=4 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeNpcTemplateState#setGfxidInStatus(I)V @insn=8 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeNpcTemplateState#setGfxidInStatus(I)V @insn=12 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeVitalState#lambda$commitDamageState$0(I)Ljava/lang/Long; @insn=29 OPCODE=182 local/lito381/server/NativeDeVitalState#setStatus(I)V
setStatus(I)V	local/lito381/server/NativeDeVitalState#lambda$commitDamageState$0(I)Ljava/lang/Long; @insn=58 OPCODE=182 local/lito381/server/NativeDeVitalState#setStatus(I)V
setStr(I)V	local/lito381/server/NativeDeCharacterActor#setStr(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setStr(I)V
setStr(I)V	local/lito381/server/NativeDeCharacterFields#addStr(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setStr(I)V
setStr(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=534 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setStr(I)V
setStr(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=163 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setStr(I)V
setStr(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=195 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setStr(I)V
setTempCharGfx(I)V	local/lito381/server/NativeDeCharacterActor#setTempCharGfx(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setTempCharGfx(I)V
setTempCharGfx(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=622 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setTempCharGfx(I)V
setTempCharGfx(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=329 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setTempCharGfx(I)V
setTempLawful(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=367 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setTempLawful(I)V
setTempLawful(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=390 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setTempLawful(I)V
setTitle(Ljava/lang/String;)V	local/lito381/server/NativeDeCharacterActor#setTitle(Ljava/lang/String;)V @insn=3 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setTitle(Ljava/lang/String;)V
setTitle(Ljava/lang/String;)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=694 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setTitle(Ljava/lang/String;)V
setTitle(Ljava/lang/String;)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=698 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setTitle(Ljava/lang/String;)V
setWeaponBreakStack(I)V	local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V @insn=48 OPCODE=182 local/lito381/server/NativeDeDeletionState#setWeaponBreakStack(I)V
setWeaponBreakStack(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=482 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setWeaponBreakStack(I)V
setWeaponBreakStack(I)V	local/lito381/server/NativeDeNpcVitalState#resurrect(I)V @insn=32 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V
setWeaponBreakStack(I)V	local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V @insn=11 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V
setWeaponBreakStack(I)V	local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V @insn=32 OPCODE=182 local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V
setWis(I)V	local/lito381/server/NativeDeCharacterActor#setWis(I)V @insn=3 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setWis(I)V
setWis(I)V	local/lito381/server/NativeDeCharacterFields#addWis(I)V @insn=5 OPCODE=182 local/lito381/server/NativeDeCharacterFields#setWis(I)V
setWis(I)V	local/lito381/server/NativeDeInitializerKernel#init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V @insn=558 OPCODE=185 local/lito381/server/NativeDeInitializerKernel$Actor#setWis(I)V
setWis(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=179 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setWis(I)V
setWis(I)V	local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=239 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setWis(I)V
set_showId(I)V	local/lito381/server/NativeDeObjectBindings#attachLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeWorldContext;IIII)Llocal/lito381/server/NativeDeWorldLocation; @insn=18 OPCODE=182 local/lito381/server/NativeDeObjectFields#set_showId(I)V
setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V	local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=141 OPCODE=182 local/lito381/server/NativeDeNpcConstructionState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V	local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V @insn=36 OPCODE=182 local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
sourceDeleteDe()V	local/lito381/server/NativeDeDeletionDeState#deleteMe()V @insn=3 HANDLE H5 local/lito381/server/NativeDeDeletionDeState#sourceDeleteDe()V
start_shop()V	local/lito381/server/NativeDePerceptionState#sourceOnPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V @insn=8 OPCODE=182 local/lito381/server/NativeDePerceptionState#start_shop()V
tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z	local/lito381/server/NativeDeCharacterFields#addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V @insn=2 OPCODE=182 local/lito381/server/NativeDeCharacterFields#tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z	local/lito381/server/NativeDeNpcKnowledge#admitted(Llocal/lito381/server/NativeDeDeletionState;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z @insn=81 OPCODE=182 local/lito381/server/NativeDeDeletionState#tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
```

### class／方法描述子稽核摘要

```text
formalClasses=2554
receiverOwners=[java/io/Serializable, java/lang/Object, java/lang/Runnable, local/lito381/server/NativeDeCharacterFields, local/lito381/server/NativeDeCorpseCore$Npc, local/lito381/server/NativeDeCorpseState, local/lito381/server/NativeDeDeletionDeState, local/lito381/server/NativeDeDeletionPorts$ObjectRef, local/lito381/server/NativeDeDeletionState, local/lito381/server/NativeDeInitializerKernel$Actor, local/lito381/server/NativeDeInstanceState, local/lito381/server/NativeDeNpcConstructionState, local/lito381/server/NativeDeNpcTemplateState, local/lito381/server/NativeDeNpcVitalState, local/lito381/server/NativeDeObjectFields, local/lito381/server/NativeDePerceptionState, local/lito381/server/NativeDeResurrectableState, local/lito381/server/NativeDeVitalState, local/lito381/server/NativeDeWeaponKernel$Target]
class local/lito381/server/NativeDeCharacterFields access=0x20
class local/lito381/server/NativeDeCorpseCore$Npc access=0x600
class local/lito381/server/NativeDeCorpseState access=0x20
class local/lito381/server/NativeDeDeletionDeState access=0x20
class local/lito381/server/NativeDeDeletionPorts$ObjectRef access=0x600
class local/lito381/server/NativeDeDeletionState access=0x20
class local/lito381/server/NativeDeInitializerKernel$Actor access=0x600
class local/lito381/server/NativeDeInstanceState access=0x21
class local/lito381/server/NativeDeNpcConstructionState access=0x1
class local/lito381/server/NativeDeNpcTemplateState access=0x20
class local/lito381/server/NativeDeNpcVitalState access=0x20
class local/lito381/server/NativeDeObjectFields access=0x420
class local/lito381/server/NativeDePerceptionState access=0x20
class local/lito381/server/NativeDeResurrectableState access=0x20
class local/lito381/server/NativeDeVitalState access=0x20
class local/lito381/server/NativeDeWeaponKernel$Target access=0x600
nonSyntheticNonConstructorCommonDescriptors=124
nonSyntheticNonConstructorAbsentFormal=[]
bridges=169
formalMatchingReferences=426
```

## 附錄 C：巢狀 lambda 完整證據

### javap 原始方法本體

staged L9079：

```text
  private void lambda$sourceAi$scheduleAiTick$22(long, long, boolean);
    descriptor: (JJZ)V
    flags: (0x1002) ACC_PRIVATE, ACC_SYNTHETIC
    Code:
      stack=7, locals=7, args_size=4
         0: aload_0
         1: astore        6
         3: aload_0
         4: aload         6
         6: lload_1
         7: lload_3
         8: iload         5
        10: invokedynamic #1379,  0           // InvokeDynamic #42:get:(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier;
        15: invokestatic  #352                // Method local/lito381/server/NativeDeLiveAiScope.call:(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
        18: pop
        19: return
```

staged L9100：

```text
  private static java.lang.Object lambda$sourceAi$scheduleAiTick$21(local.lito381.server.NativeDeInstanceState, long, long, boolean);
    descriptor: (Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;
    flags: (0x100a) ACC_PRIVATE, ACC_STATIC, ACC_SYNTHETIC
    Code:
      stack=6, locals=6, args_size=4
         0: aload_0
         1: lload_1
         2: lload_3
         3: iload         5
         5: invokevirtual #1380               // Method sourceAi$lambda$scheduleAiTick$0:(JJZ)V
         8: aconst_null
         9: areturn
```

formal L6324：

```text
  private void lambda$scheduleAiTick$0(long, long, boolean);
    descriptor: (JJZ)V
    flags: (0x1002) ACC_PRIVATE, ACC_SYNTHETIC
    Code:
      stack=7, locals=6, args_size=4
         0: aload_0
         1: aload_0
         2: lload_1
         3: lload_3
         4: iload         5
         6: invokedynamic #1238,  0           // InvokeDynamic #42:get:(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier;
        11: invokestatic  #493                // Method local/lito381/server/NativeDeLiveAiScope.call:(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
        14: pop
        15: return

```

formal L6311：

```text
  private static java.lang.Object lambda$nativeAiScope$35(local.lito381.server.NativeDeInstanceState, long, long, boolean);
    descriptor: (Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;
    flags: (0x100a) ACC_PRIVATE, ACC_STATIC, ACC_SYNTHETIC
    Code:
      stack=6, locals=6, args_size=4
         0: aload_0
         1: lload_1
         2: lload_3
         3: iload         5
         5: invokevirtual #1234               // Method sourceAi$lambda$scheduleAiTick$0:(JJZ)V
         8: aconst_null
         9: areturn

```

formal L6299：

```text
  private void sourceAi$lambda$scheduleAiTick$0(long, long, boolean);
    descriptor: (JJZ)V
    flags: (0x1002) ACC_PRIVATE, ACC_SYNTHETIC
    Code:
      stack=6, locals=6, args_size=4
         0: aload_0
         1: lload_1
         2: lload_3
         3: iload         5
         5: invokevirtual #1231               // Method onAiTimer:(JJZ)V
         8: return

```

### 外層完整正規化方法與逐路徑診斷

```text
LEFT lambda$sourceAi$scheduleAiTick$22(JJZ)V flags=0x1002
ALOAD 0
ASTORE 6
ALOAD 0
ALOAD 6
LLOAD 1/2
LLOAD 3/4
ILOAD 5
INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6e34c385d8f1d059b9d55cd4e13b09c43db4fe7ba283054930f7d1ae77e48eae(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
POP
RETURN
RIGHT lambda$scheduleAiTick$0(JJZ)V flags=0x1002
ALOAD 0
ALOAD 0
LLOAD 1/2
LLOAD 3/4
ILOAD 5
INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6e34c385d8f1d059b9d55cd4e13b09c43db4fe7ba283054930f7d1ae77e48eae(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
POP
RETURN
DIRECT_PATH_COMPARISON Result[same=true, reason=path multisets equal (3)]
This diagnostic is not the CLI acceptance result.
```

### 原始正規化差異全文

bridge 已逐項列於附錄 B，已核准建構子另列 deviations。以下是線性正規化後的完整差異，也包含 CLI 後續已證明相同的 35 個 adapter 暫存；不是每段 diff 都代表最終 DIFF。CLI 尚未通過的非 bridge 方法只有上列外層 lambda 一對，以及 sourceAi$scheduleAiTick 的 handle。

```diff
--- staged
+++ formal
@@ -169,9 +169,7 @@
 method beginAiTick(J)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method beginAiTick(J)Z default=
 method beginAiTick(J)Z ALOAD 0
-method beginAiTick(J)Z ASTORE 3
 method beginAiTick(J)Z ALOAD 0
-method beginAiTick(J)Z ALOAD 3
 method beginAiTick(J)Z LLOAD 1/2
 method beginAiTick(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$8d31e72a99e5d175e5aaeb1f2849132c352a173b36ad8621108bc19f77ea9e0d(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method beginAiTick(J)Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -184,9 +182,7 @@
 method beginBackgroundTick(J)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method beginBackgroundTick(J)Z default=
 method beginBackgroundTick(J)Z ALOAD 0
-method beginBackgroundTick(J)Z ASTORE 3
 method beginBackgroundTick(J)Z ALOAD 0
-method beginBackgroundTick(J)Z ALOAD 3
 method beginBackgroundTick(J)Z LLOAD 1/2
 method beginBackgroundTick(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$f99408d8bac39023575cea63a7d03bdf8e5654d54d06704026e4d96f4e8dc741(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method beginBackgroundTick(J)Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -211,9 +207,7 @@
 method calcSleepTime(II)I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method calcSleepTime(II)I default=
 method calcSleepTime(II)I ALOAD 0
-method calcSleepTime(II)I ASTORE 3
 method calcSleepTime(II)I ALOAD 0
-method calcSleepTime(II)I ALOAD 3
 method calcSleepTime(II)I ILOAD 1
 method calcSleepTime(II)I ILOAD 2
 method calcSleepTime(II)I INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;II)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$766818ef16eb340fda5d2ea0ea4b631ca4e69147414167f7ccca1c858564c7be(Llocal/lito381/server/NativeDeInstanceState;II)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -227,9 +221,7 @@
 method checkNeedActive()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method checkNeedActive()Z default=
 method checkNeedActive()Z ALOAD 0
-method checkNeedActive()Z ASTORE 1
 method checkNeedActive()Z ALOAD 0
-method checkNeedActive()Z ALOAD 1
 method checkNeedActive()Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$a0f5465ee770176efce7d178895c1447fa0149a5431f950503b9c702f72a07f0(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method checkNeedActive()Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method checkNeedActive()Z CHECKCAST java/lang/Boolean
@@ -241,9 +233,7 @@
 method checkSpawnLocation()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method checkSpawnLocation()V default=
 method checkSpawnLocation()V ALOAD 0
-method checkSpawnLocation()V ASTORE 1
 method checkSpawnLocation()V ALOAD 0
-method checkSpawnLocation()V ALOAD 1
 method checkSpawnLocation()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$e0fa1531638dcdc178cb183d27df504914f183c7e9e2899abf15edb8acf257c0(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method checkSpawnLocation()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method checkSpawnLocation()V POP
@@ -285,9 +275,7 @@
 method completeAiStopLocked(J)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method completeAiStopLocked(J)Z default=
 method completeAiStopLocked(J)Z ALOAD 0
-method completeAiStopLocked(J)Z ASTORE 3
 method completeAiStopLocked(J)Z ALOAD 0
-method completeAiStopLocked(J)Z ALOAD 3
 method completeAiStopLocked(J)Z LLOAD 1/2
 method completeAiStopLocked(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$04fe5ddf9af7afd1067fa248c317ce2d1338029eadee0c84bb7d2da50836d18a(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method completeAiStopLocked(J)Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -349,9 +337,7 @@
 method dispatchAiWork(JZ)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method dispatchAiWork(JZ)V default=
 method dispatchAiWork(JZ)V ALOAD 0
-method dispatchAiWork(JZ)V ASTORE 4
 method dispatchAiWork(JZ)V ALOAD 0
-method dispatchAiWork(JZ)V ALOAD 4
 method dispatchAiWork(JZ)V LLOAD 1/2
 method dispatchAiWork(JZ)V ILOAD 3
 method dispatchAiWork(JZ)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$31704940456f4ced120aa80e85dc692477dbdb516bc5a30b5fb8bbeda8a4d6eb(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -364,9 +350,7 @@
 method ensureAiScheduled()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method ensureAiScheduled()V default=
 method ensureAiScheduled()V ALOAD 0
-method ensureAiScheduled()V ASTORE 1
 method ensureAiScheduled()V ALOAD 0
-method ensureAiScheduled()V ALOAD 1
 method ensureAiScheduled()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$17595e90c9e7076d36fdcce3a8f82105a9fdaa7185058aba41249ea2925fdcb3(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method ensureAiScheduled()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method ensureAiScheduled()V POP
@@ -377,9 +361,7 @@
 method enterBackgroundMode(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method enterBackgroundMode(J)V default=
 method enterBackgroundMode(J)V ALOAD 0
-method enterBackgroundMode(J)V ASTORE 3
 method enterBackgroundMode(J)V ALOAD 0
-method enterBackgroundMode(J)V ALOAD 3
 method enterBackgroundMode(J)V LLOAD 1/2
 method enterBackgroundMode(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$052406114ab86bc5e468300ef193026d003db6475e0ec08ac5e0c266c0ce56e5(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method enterBackgroundMode(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -407,9 +389,7 @@
 method failAiTimer(JJ)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method failAiTimer(JJ)V default=
 method failAiTimer(JJ)V ALOAD 0
-method failAiTimer(JJ)V ASTORE 5
 method failAiTimer(JJ)V ALOAD 0
-method failAiTimer(JJ)V ALOAD 5
 method failAiTimer(JJ)V LLOAD 1/2
 method failAiTimer(JJ)V LLOAD 3/4
 method failAiTimer(JJ)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6d225bfd869d02f7f03f08c23bc4470c45ffc6394c615c7a4e118f1ac25f282e(Llocal/lito381/server/NativeDeInstanceState;JJ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -422,9 +402,7 @@
 method finishAiTick(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method finishAiTick(J)V default=
 method finishAiTick(J)V ALOAD 0
-method finishAiTick(J)V ASTORE 3
 method finishAiTick(J)V ALOAD 0
-method finishAiTick(J)V ALOAD 3
 method finishAiTick(J)V LLOAD 1/2
 method finishAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$0ea923374607d223a236b57f8aeca588e4c509ca30ceb1b881d7113792bad6a3(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method finishAiTick(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -540,9 +518,7 @@
 method getName()Ljava/lang/String; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method getName()Ljava/lang/String; default=
 method getName()Ljava/lang/String; ALOAD 0
-method getName()Ljava/lang/String; ASTORE 1
 method getName()Ljava/lang/String; ALOAD 0
-method getName()Ljava/lang/String; ALOAD 1
 method getName()Ljava/lang/String; INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$7f3a1d3a2bcdb1bfdc41b01d9b24d6ef1c2b7ccbfc759bc6e409c1afe1a6239c(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method getName()Ljava/lang/String; INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method getName()Ljava/lang/String; CHECKCAST java/lang/String
@@ -553,9 +529,7 @@
 method getSleepTime()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method getSleepTime()I default=
 method getSleepTime()I ALOAD 0
-method getSleepTime()I ASTORE 1
 method getSleepTime()I ALOAD 0
-method getSleepTime()I ALOAD 1
 method getSleepTime()I INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$b17d5aefdad5a574e8469cefacf8f9c22a7db13e26f5a407e27955e5d10d7437(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method getSleepTime()I INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method getSleepTime()I CHECKCAST java/lang/Integer
@@ -583,9 +557,7 @@
 method isBusy()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method isBusy()Z default=
 method isBusy()Z ALOAD 0
-method isBusy()Z ASTORE 1
 method isBusy()Z ALOAD 0
-method isBusy()Z ALOAD 1
 method isBusy()Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$d8ae5c61fbc3b0d6410c96d3a8258a88f5c06ff31ab2fb8850887ce2d07de464(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method isBusy()Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method isBusy()Z CHECKCAST java/lang/Boolean
@@ -622,9 +594,7 @@
 method isDeAiChain(J)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method isDeAiChain(J)Z default=
 method isDeAiChain(J)Z ALOAD 0
-method isDeAiChain(J)Z ASTORE 3
 method isDeAiChain(J)Z ALOAD 0
-method isDeAiChain(J)Z ALOAD 3
 method isDeAiChain(J)Z LLOAD 1/2
 method isDeAiChain(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$0d9fbd9062a728230a2e173ea5fb543d98a8e17d9fc11683eccbb46748e27bb0(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method isDeAiChain(J)Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -743,6 +713,20 @@
 method lambda$content$31704940456f4ced120aa80e85dc692477dbdb516bc5a30b5fb8bbeda8a4d6eb(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object; INVOKESPECIAL local/lito381/server/NativeDeInstanceState.sourceAi$dispatchAiWork(JZ)V
 method lambda$content$31704940456f4ced120aa80e85dc692477dbdb516bc5a30b5fb8bbeda8a4d6eb(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object; CONST null
 method lambda$content$31704940456f4ced120aa80e85dc692477dbdb516bc5a30b5fb8bbeda8a4d6eb(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object; ARETURN
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V access=0x2
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V signature=null
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V exceptions=
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V default=
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V ALOAD 0
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V ALOAD 0
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V LLOAD 1/2
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V LLOAD 3/4
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V ILOAD 5
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6e34c385d8f1d059b9d55cd4e13b09c43db4fe7ba283054930f7d1ae77e48eae(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V POP
+method lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V RETURN
 method lambda$content$374a7feeea95d07b6724df256e4d062097c4470c331b242e2911021affa29852(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; access=0xa
 method lambda$content$374a7feeea95d07b6724df256e4d062097c4470c331b242e2911021affa29852(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; signature=null
 method lambda$content$374a7feeea95d07b6724df256e4d062097c4470c331b242e2911021affa29852(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; exceptions=
@@ -1093,22 +1077,6 @@
 method lambda$content$dcdc0eeca0b7c94436c21d81cf5758824531592f0d454098d8c9cb86f3e20a90(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object; INVOKESPECIAL local/lito381/server/NativeDeInstanceState.sourceAi$setAiRunning(Z)V
 method lambda$content$dcdc0eeca0b7c94436c21d81cf5758824531592f0d454098d8c9cb86f3e20a90(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object; CONST null
 method lambda$content$dcdc0eeca0b7c94436c21d81cf5758824531592f0d454098d8c9cb86f3e20a90(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object; ARETURN
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V access=0x2
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V signature=null
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V exceptions=
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V default=
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V ALOAD 0
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V ASTORE 6
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V ALOAD 0
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V ALOAD 6
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V LLOAD 1/2
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V LLOAD 3/4
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V ILOAD 5
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6e34c385d8f1d059b9d55cd4e13b09c43db4fe7ba283054930f7d1ae77e48eae(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V POP
-method lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V RETURN
 method lambda$content$e0fa1531638dcdc178cb183d27df504914f183c7e9e2899abf15edb8acf257c0(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object; access=0xa
 method lambda$content$e0fa1531638dcdc178cb183d27df504914f183c7e9e2899abf15edb8acf257c0(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object; signature=null
 method lambda$content$e0fa1531638dcdc178cb183d27df504914f183c7e9e2899abf15edb8acf257c0(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object; exceptions=
@@ -1162,9 +1130,7 @@
 method nextAiRetryDelay(JZ)I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method nextAiRetryDelay(JZ)I default=
 method nextAiRetryDelay(JZ)I ALOAD 0
-method nextAiRetryDelay(JZ)I ASTORE 4
 method nextAiRetryDelay(JZ)I ALOAD 0
-method nextAiRetryDelay(JZ)I ALOAD 4
 method nextAiRetryDelay(JZ)I LLOAD 1/2
 method nextAiRetryDelay(JZ)I ILOAD 3
 method nextAiRetryDelay(JZ)I INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$a21cb609ea3950369f7481f10c054f8ac56e76b313f70769268262b3d505793e(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -1178,9 +1144,7 @@
 method nextAiTimerTokenLocked()J annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method nextAiTimerTokenLocked()J default=
 method nextAiTimerTokenLocked()J ALOAD 0
-method nextAiTimerTokenLocked()J ASTORE 1
 method nextAiTimerTokenLocked()J ALOAD 0
-method nextAiTimerTokenLocked()J ALOAD 1
 method nextAiTimerTokenLocked()J INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6d56864644b6d5cc34c84feccb49eb9e323a8e2ba1af52632409a13d777916bb(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method nextAiTimerTokenLocked()J INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method nextAiTimerTokenLocked()J CHECKCAST java/lang/Long
@@ -1192,9 +1156,7 @@
 method normalizeDeSleepTime(J)I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method normalizeDeSleepTime(J)I default=
 method normalizeDeSleepTime(J)I ALOAD 0
-method normalizeDeSleepTime(J)I ASTORE 3
 method normalizeDeSleepTime(J)I ALOAD 0
-method normalizeDeSleepTime(J)I ALOAD 3
 method normalizeDeSleepTime(J)I LLOAD 1/2
 method normalizeDeSleepTime(J)I INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$0078114c2ede166c9951182c2da584c0e9748177f9cf39842c86dac921df5c4e(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method normalizeDeSleepTime(J)I INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1207,9 +1169,7 @@
 method onAiTimer(JJZ)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method onAiTimer(JJZ)V default=
 method onAiTimer(JJZ)V ALOAD 0
-method onAiTimer(JJZ)V ASTORE 6
 method onAiTimer(JJZ)V ALOAD 0
-method onAiTimer(JJZ)V ALOAD 6
 method onAiTimer(JJZ)V LLOAD 1/2
 method onAiTimer(JJZ)V LLOAD 3/4
 method onAiTimer(JJZ)V ILOAD 5
@@ -1223,9 +1183,7 @@
 method onBackgroundTick()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method onBackgroundTick()V default=
 method onBackgroundTick()V ALOAD 0
-method onBackgroundTick()V ASTORE 1
 method onBackgroundTick()V ALOAD 0
-method onBackgroundTick()V ALOAD 1
 method onBackgroundTick()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$9be4c9ea054447f893fa047fde4d698e9270cf256f597168ce4ccce8344f411f(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method onBackgroundTick()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method onBackgroundTick()V POP
@@ -1267,9 +1225,7 @@
 method run()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method run()V default=
 method run()V ALOAD 0
-method run()V ASTORE 1
 method run()V ALOAD 0
-method run()V ALOAD 1
 method run()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$f352210bd9a19674b51b6d30f17c988ef8312d5816947e2bb4dc30b7cf0e3371(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method run()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method run()V POP
@@ -1280,9 +1236,7 @@
 method runAiTick(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method runAiTick(J)V default=
 method runAiTick(J)V ALOAD 0
-method runAiTick(J)V ASTORE 3
 method runAiTick(J)V ALOAD 0
-method runAiTick(J)V ALOAD 3
 method runAiTick(J)V LLOAD 1/2
 method runAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$88bf759f1c82ee3d3ed850c68681aa12496a3e22df985b027e57a483fb6c09b4(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method runAiTick(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1294,9 +1248,7 @@
 method runBackgroundTick(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method runBackgroundTick(J)V default=
 method runBackgroundTick(J)V ALOAD 0
-method runBackgroundTick(J)V ASTORE 3
 method runBackgroundTick(J)V ALOAD 0
-method runBackgroundTick(J)V ALOAD 3
 method runBackgroundTick(J)V LLOAD 1/2
 method runBackgroundTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$68068ba960deb8d30240f82f4244b44ab137274153e8c1e538613683482f4ece(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method runBackgroundTick(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1308,9 +1260,7 @@
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; default=
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ASTORE 4
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ALOAD 4
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; LLOAD 1/2
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ILOAD 3
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JI)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$5b037b66674c43515d30e1dd0d9d8da867c4e5b3cf71612c28b539f3d9eb8288(Llocal/lito381/server/NativeDeInstanceState;JI)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -1323,9 +1273,7 @@
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; default=
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ASTORE 5
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ALOAD 5
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; LLOAD 1/2
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ILOAD 3
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ILOAD 4
@@ -1339,9 +1287,7 @@
 method scheduleNextAiTick(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method scheduleNextAiTick(J)V default=
 method scheduleNextAiTick(J)V ALOAD 0
-method scheduleNextAiTick(J)V ASTORE 3
 method scheduleNextAiTick(J)V ALOAD 0
-method scheduleNextAiTick(J)V ALOAD 3
 method scheduleNextAiTick(J)V LLOAD 1/2
 method scheduleNextAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$945e2896e0b6148f449dcdfedd66bff45c1998e8f530bfccfcd1b0734661d5f1(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method scheduleNextAiTick(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1353,9 +1299,7 @@
 method setAiRunning(Z)V annotations=@Ljava/lang/Deprecated;||||pvis=|pinv=|lvtvis=|lvtinv=
 method setAiRunning(Z)V default=
 method setAiRunning(Z)V ALOAD 0
-method setAiRunning(Z)V ASTORE 2
 method setAiRunning(Z)V ALOAD 0
-method setAiRunning(Z)V ALOAD 2
 method setAiRunning(Z)V ILOAD 1
 method setAiRunning(Z)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$dcdc0eeca0b7c94436c21d81cf5758824531592f0d454098d8c9cb86f3e20a90(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method setAiRunning(Z)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1495,9 +1439,7 @@
 method setNpcSpeed()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method setNpcSpeed()V default=
 method setNpcSpeed()V ALOAD 0
-method setNpcSpeed()V ASTORE 1
 method setNpcSpeed()V ALOAD 0
-method setNpcSpeed()V ALOAD 1
 method setNpcSpeed()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$d82832a2183f640244a3f998eab3fa3efe3f5e16e2654e965fc7b8bf47481356(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method setNpcSpeed()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method setNpcSpeed()V POP
@@ -3285,7 +3227,7 @@
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; LLOAD 1/2
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; LLOAD 6/7
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ILOAD 4
-method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/NativeDeInstanceState.lambda$content$ddf3fa5fa9cea94022d8bc9316f36679ae594967bc8cbdce754b697aeb990b7d(JJZ)V, type:()V]
+method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/NativeDeInstanceState.lambda$content$351481d0b3c78d336e51b1beac1da6879a4988c6e81a04d1533c212df2c5d781(JJZ)V, type:()V]
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; CONST Integer:0
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ILOAD 3
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; INVOKESTATIC java/lang/Math.max(II)I
@@ -3957,9 +3899,7 @@
 method startAI()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method startAI()V default=
 method startAI()V ALOAD 0
-method startAI()V ASTORE 1
 method startAI()V ALOAD 0
-method startAI()V ALOAD 1
 method startAI()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$afd00721cf0943ac2ed68f035f4231ea8970fc1542fd7d31a86cee32c324db62(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method startAI()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method startAI()V POP
@@ -4020,9 +3960,7 @@
 method stopAI()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method stopAI()V default=
 method stopAI()V ALOAD 0
-method stopAI()V ASTORE 1
 method stopAI()V ALOAD 0
-method stopAI()V ALOAD 1
 method stopAI()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$5afda35f2420fabd33fb5acc42db84107fe8853ce8eb11d130ea44ed35dc70d4(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method stopAI()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method stopAI()V POP
@@ -4033,9 +3971,7 @@
 method stopAiOwner(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method stopAiOwner(J)V default=
 method stopAiOwner(J)V ALOAD 0
-method stopAiOwner(J)V ASTORE 3
 method stopAiOwner(J)V ALOAD 0
-method stopAiOwner(J)V ALOAD 3
 method stopAiOwner(J)V LLOAD 1/2
 method stopAiOwner(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$4bfea0865f313a078f4054e7d8259859d6bd0ec8891bda62fd729e29b968bd20(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method stopAiOwner(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -4047,9 +3983,7 @@
 method submitAiTick(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method submitAiTick(J)V default=
 method submitAiTick(J)V ALOAD 0
-method submitAiTick(J)V ASTORE 3
 method submitAiTick(J)V ALOAD 0
-method submitAiTick(J)V ALOAD 3
 method submitAiTick(J)V LLOAD 1/2
 method submitAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$374a7feeea95d07b6724df256e4d062097c4470c331b242e2911021affa29852(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method submitAiTick(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -4061,9 +3995,7 @@
 method updateNextActionTime(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method updateNextActionTime(I)V default=
 method updateNextActionTime(I)V ALOAD 0
-method updateNextActionTime(I)V ASTORE 2
 method updateNextActionTime(I)V ALOAD 0
-method updateNextActionTime(I)V ALOAD 2
 method updateNextActionTime(I)V ILOAD 1
 method updateNextActionTime(I)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$99b1717b29e0f8c927fe237f95a0924b7ee57d051661db44be1f9cc6f40b2381(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method updateNextActionTime(I)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -4075,9 +4007,7 @@
 method wakeup()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method wakeup()V default=
 method wakeup()V ALOAD 0
-method wakeup()V ASTORE 1
 method wakeup()V ALOAD 0
-method wakeup()V ALOAD 1
 method wakeup()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$dacb1b16918bcb3894b7dcb010ed8f5153164ccb7bb9558fff23db42ea0369c5(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method wakeup()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method wakeup()V POP
@@ -4088,9 +4018,7 @@
 method wakeup(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method wakeup(J)V default=
 method wakeup(J)V ALOAD 0
-method wakeup(J)V ASTORE 3
 method wakeup(J)V ALOAD 0
-method wakeup(J)V ALOAD 3
 method wakeup(J)V LLOAD 1/2
 method wakeup(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$f33796a7920f59e63d9e9558e6379e9bd319c64d3a7474e5e9bdee0102c7317d(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method wakeup(J)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
```

## 附錄 D：輸入與證據 SHA-256

本輪工具來源與 main `9678e97` 相同，包含 #186、未包含 #203；JSON generatedAt 為 `2026-11-05T18:31:58Z`。

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `scratch/issue-72/target/final-reproduce.jar` | `63e970a0ed1bd26f94586b79df3de840e6231b46977ce63215d5427e55363808` |
| `scratch/issue-72/final-reproduce.json` | `de7c5b2a04094e8d259fa69c0d5c4387cc16a1ccc4bd6e515358a559fbd9d911` |
| `scratch/issue-72/final-reproduce.md` | `80f6e8aa8e8eee41e98b01da30c5bfb6891f8804fb0a5ca7e21b620b3b6b2323` |
| `scratch/issue-72/staged-instance.javap.txt` | `d25eceb12d285baed5880c49de8e05a347e40a341d36a1a9886db9c746277122` |
| `scratch/issue-72/formal-instance.javap.txt` | `5aee326c850f7d71af3c39c77aab7373de3dbd3c649a01c4cea02221bce529ee` |
| `scratch/issue-72/formal-callers.javap.txt` | `c45d54785e9bfc6f894f1abaa38d31fb06f126734ae0cc89b9521871862b21ab` |
| `scratch/issue-72/audit/bridges.tsv` | `a769e24699c04cb4c7ea1ec405dd17b2923a58299bb9931b060448ded83c74a9` |
| `scratch/issue-72/audit/calls.tsv` | `4acd5d7833426987ddfae9a6c6a98a0899335ecd9593d7cfd14cdba2a0acb1d0` |
| `scratch/issue-72/lambda-audit.txt` | `1362078962842385e74b17e5c3b815558f7d8c935a1f762ddcdf18089dcaa59d` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/BlockExpressions.java` | `f5d93c6a34f3d3b9b1cb769749b7be6cc6df598d53e106ef34c04f3e3e13a5dc` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/ClassComparer.java` | `32fe3a8dce53cbcc2955dbcab27634f719736a71343641195bd2ddfb468ea6d1` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/FourthNormalizations.java` | `f2994f27816d427575b1d65d7e33502dd0006b49938a46d5e1633396e3e0a0ce` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/JarIndex.java` | `78219bb9c1aca74e275450046313b4dd3eacfaa26e6140aead2f9259911d73f5` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/LayoutComparison.java` | `c3c31be673b6088eacb2fcbb4da71d6523951be2bcc9d2e7c2325778178c6078` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/LocalTransport.java` | `bd98cfc3303dbb170595bc63143c5be95c3483b720a912861708c5448d4643d8` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/LocalWebs.java` | `564d8dbc0256890d64520c46815bfa00b5c93075b9e65052121488bdcf9fd608` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/NonNullChecks.java` | `2dbd1a43a1c2324dda5af4d3de510f8472daf880676892bf2788ff106578d923` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/Normalizations.java` | `6094fa299e2b17538a6a7050464e624ffec75c7cecf8566d667fc11058e79dec` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/PathComparison.java` | `a36626687f3177ef1fe38ce5aa68d5ecf9f27927fc3fca0788add0098d1162ad` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/SemanticDiff.java` | `29234a4a092712401e85245e73b95bd5c48d85fbe82c8810b52ceb90233c4bfd` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/TransportComparison.java` | `fa05cf2ea262c8d6b23a2fcbdcf0fb1d76057519978e081e5433067679df4fa2` |
| `scratch/issue-72/formal-handle.javap.txt` | `a3d0613df04d43d3ed24e17880ea1914215bcd3b837b1b7ccda663382d3d7d6f` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/RethrowHandlers.java` | `29f7f291cd6bf5cecfe77653fc3267e80decea857d3ea782878802727a37215e` |
| `docs/combat-merge/a-batches.json` | `ac1c17ec1a446564c6fb912235ecd9222d06bef1a3a91cdcb3836fda158ad65d` |
| `docs/combat-merge/deviations.txt` | `15d68d4115fa4eeece0f206972a19049f6eeebf3f5055cc28f3c852c4d5475f9` |
| `docs/combat-merge/access-widen-approved.txt` | `4bf5066b4ba996472337b173330e9fb5306b4378847512d15fe510d9b666ac74` |
| `docs/combat-merge/effects-approved.txt` | `b8ecf18b0595beadd278534735b6df6279ae8aa9cc3d8a256d7cd72e7a29c11f` |
| `scratch/issue-72/verify_entries.txt` | `8c883895819b752ae9c9b70b2cef771ce7f44bc23cacfc25c19c197109c79499` |
| `scratch/issue-72/deviations.txt` | `a69185542cbe0af49e8bb2287f9745d6d0640322a533a47cd99ab7671ac10dbb` |
| `scratch/issue-72/access-widen-approved.txt` | `c91befb77d5968cfe9a58779c819f410b8e817712e9c660b233805733c1faed3` |
| `scratch/issue-72/final-reproduce.py` | `cc9ece2eaf36dd427b0d4900f9c5fbb433cd2aa3683ccae96a30990955bffbb1` |
| `scratch/issue-72/final-semantic.log` | `38c1a4e26b00c587225ed60807b00d12a19ea69cc58a02c5f5381d050d30fbcd` |

## 附錄 E：完整重現與稽核程式

以下程式只在本 worktree 建置與讀取固定 JAR；不載入應用程式類別。把各區塊還原到所列 scratch 路徑後執行前述命令。

### scratch/issue-72/final-reproduce.py

```python
from pathlib import Path
import subprocess, json, zipfile, hashlib
root=Path(__file__).resolve().parents[2]
s=root/'lito伺服器/LiTo381-Server'; w=root/'scratch/issue-72'
j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
batch=next(b for b in json.loads((root/'docs/combat-merge/a-batches.json').read_text(encoding='utf8'))['batches'] if b['id']=='A01')
st=s/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
asm=';'.join(str(Path('D:/dev-cache/maven/repository/org/ow2/asm')/n/'9.7'/f'{n}-9.7.jar') for n in ['asm','asm-tree'])+';'+str(w/'target/dependency/asm-analysis-9.7.jar')
tc=s/'tools/semantic-diff/target/classes'
def run(args,name):
    r=subprocess.run([str(x) for x in args],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (w/name).write_bytes(r.stdout)
    print(name,'exit',r.returncode)
    assert r.returncode==0, r.stdout
run([j/'javac.exe','--release','17','-encoding','UTF-8','-d',s/'target/legacy-stubs',*sorted((s/'compile-stubs').rglob('*.java'))],'final-stubs.log')
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',asm,'-d',tc,*sorted((s/'tools/semantic-diff/src/main/java').rglob('*.java'))],'final-tool.log')
out=w/'target/final-reproduce'; out.mkdir(parents=True,exist_ok=True)
assert out.resolve().is_relative_to(w.resolve())
for p in out.rglob('*.class'): p.unlink()
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',f'{s}/reference/base-0.6.71.jar;{s}/target/legacy-stubs;{s}/lib/*','-d',out,*[st/(n+'.java') for n in batch['classes']]],'final-javac.log')
jar=w/'target/final-reproduce.jar'
with zipfile.ZipFile(jar,'w') as z:
    for p in sorted(out.rglob('*.class')):
        z.writestr(zipfile.ZipInfo(p.relative_to(out).as_posix(),(2026,1,1,0,0,0)),p.read_bytes())
with zipfile.ZipFile(jar) as z: actual=set(z.namelist())
expected=set(batch['verify_entries'])
assert actual==expected,(expected-actual,actual-expected)
assert not any(n.startswith('com/lineage/') for n in actual)
entries=w/'verify_entries.txt'; entries.write_bytes(('\n'.join(batch['verify_entries'])+'\n').encode())
flags=[]
for flag,file in [('effects-methods','effects-approved.txt'),('access-widen','access-widen-approved.txt'),('deviations','deviations.txt')]:
    lines=[]
    for line in (root/'docs/combat-merge'/file).read_text(encoding='utf-8-sig').splitlines():
        key=line.split(' #')[0].strip()
        if not key or key.startswith('#'): continue
        if next((v for v in key.split() if '#' in v), key.split()[0]).split('#')[0]+'.class' in expected: lines.append(line)
    if lines:
        path=w/file; path.write_bytes(('\n'.join(lines)+'\n').encode());flags+=['--'+flag,path]
run([j/'java.exe','-Dfile.encoding=UTF-8','-cp',f'{tc};{asm}','local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',s/'reference/base-0.6.71.jar','--left-classpath',s/'reference/base-0.6.71.jar','--classes',entries,*flags,'--json',w/'final-reproduce.json','--md',w/'final-reproduce.md'],'final-semantic.log')
r=json.loads((w/'final-reproduce.json').read_text(encoding='utf8'))
assert r['classSelection']=={'missingLeft':[],'extraLeft':[]}


print(json.dumps(r['counts']));print('sha256',hashlib.sha256(jar.read_bytes()).hexdigest())
print('ACCEPTANCE',all(c['status'] in {'SAME','SAME_EFFECTS','SAME_PATHS','DEVIATION'} for c in r['classes']))

```

### scratch/issue-72/BridgeAudit.java

```java
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class BridgeAudit {
  static final String INSTANCE = "local/lito381/server/NativeDeInstanceState";
  static Map<String, ClassNode> read(String file) throws Exception {
    Map<String, ClassNode> all = new TreeMap<>();
    try (JarFile jar = new JarFile(file)) {
      for (var e : Collections.list(jar.entries())) if (e.getName().endsWith(".class")) {
        ClassNode c = new ClassNode(); new ClassReader(jar.getInputStream(e)).accept(c, 0); all.put(c.name, c);
      }
    }
    return all;
  }
  static String key(MethodNode m) { return m.name + m.desc; }
  static void ancestors(String name, Map<String,ClassNode> all, Set<String> names) {
    if (name == null || !names.add(name)) return;
    ClassNode c = all.get(name); if (c == null) return;
    ancestors(c.superName, all, names); for(String i:c.interfaces) ancestors(i, all, names);
  }
  static String resolve(String owner, String name, String desc, Map<String,ClassNode> all) {
    ClassNode c = all.get(owner); if (c == null) return "unresolved:" + owner;
    for (MethodNode m:c.methods) if (m.name.equals(name) && m.desc.equals(desc)) return c.name + "#" + key(m);
    return resolve(c.superName, name, desc, all);
  }
  static void handle(Object arg, String caller, int index, Set<String> owners, Map<String,List<String>> refs) {
    if (arg instanceof Handle h && owners.contains(h.getOwner()) && refs.containsKey(h.getName()+h.getDesc()))
      refs.get(h.getName()+h.getDesc()).add(caller+" @insn="+index+" HANDLE H"+h.getTag()+" "+h.getOwner()+"#"+h.getName()+h.getDesc());
    if (arg instanceof ConstantDynamic c) {
      handle(c.getBootstrapMethod(),caller,index,owners,refs);
      for(int i=0;i<c.getBootstrapMethodArgumentCount();i++) handle(c.getBootstrapMethodArgument(i),caller,index,owners,refs);
    }
  }
  public static void main(String[] args) throws Exception {
    var left=read(args[0]); var right=read(args[1]); Path out=Path.of(args[2]); Files.createDirectories(out);
    var l=left.get(INSTANCE);var r=right.get(INSTANCE);
    Set<String> owners=new TreeSet<>();ancestors(INSTANCE,right,owners);
    List<String> summary=new ArrayList<>(); summary.add("formalClasses="+right.size());
    summary.add("receiverOwners="+owners);
    for(String n:owners) if(right.containsKey(n)) summary.add("class "+n+" access=0x"+Integer.toHexString(right.get(n).access));
    Set<String> formal=new TreeSet<>();for(var m:r.methods)formal.add(key(m));
    Map<String,List<String>> refs=new TreeMap<>();Map<String,MethodNode> bridges=new TreeMap<>();
    for(var m:l.methods) if((m.access&Opcodes.ACC_BRIDGE)!=0) {bridges.put(key(m),m);refs.put(key(m),new ArrayList<>());}
    int common=0;List<String> changed=new ArrayList<>();
    for(var m:l.methods) if((m.access&(Opcodes.ACC_BRIDGE|Opcodes.ACC_SYNTHETIC))==0 && !m.name.equals("<init>") && !m.name.equals("<clinit>")) {
      if(formal.contains(key(m)))common++;else changed.add(key(m));
    }
    summary.add("nonSyntheticNonConstructorCommonDescriptors="+common);summary.add("nonSyntheticNonConstructorAbsentFormal="+changed);
    for(var c:right.values())for(var m:c.methods) {
      String caller=c.name+"#"+key(m); int index=0;
      for(var insn:m.instructions) {
        if(insn instanceof MethodInsnNode x && owners.contains(x.owner) && refs.containsKey(x.name+x.desc))
          refs.get(x.name+x.desc).add(caller+" @insn="+index+" OPCODE="+x.getOpcode()+" "+x.owner+"#"+x.name+x.desc);
        if(insn instanceof InvokeDynamicInsnNode x) {handle(x.bsm,caller,index,owners,refs);for(Object a:x.bsmArgs)handle(a,caller,index,owners,refs);}
        if(insn instanceof LdcInsnNode x)handle(x.cst,caller,index,owners,refs);
        if(insn.getOpcode()>=0)index++;
      }
    }
    List<String> rows=new ArrayList<>(), calls=new ArrayList<>();
    rows.add("bridge\tflags\ttarget\tresolvedFormal\tformalDeclares\tcallCount");
    for(var e:bridges.entrySet()) {
      MethodNode m=e.getValue();List<MethodInsnNode> targets=new ArrayList<>();
      for(var x:m.instructions) if(x instanceof MethodInsnNode mi)targets.add(mi);
      if(targets.size()!=1)throw new IllegalStateException(e.getKey());var t=targets.get(0);
      if(!e.getKey().equals(t.name+t.desc))throw new IllegalStateException("descriptor adaptation "+e.getKey());
      rows.add(e.getKey()+"\t0x"+Integer.toHexString(m.access)+"\t"+t.owner+"#"+t.name+t.desc+"\t"+resolve(t.owner,t.name,t.desc,right)+"\t"+formal.contains(e.getKey())+"\t"+refs.get(e.getKey()).size());
      for(String ref:refs.get(e.getKey()))calls.add(e.getKey()+"\t"+ref);
    }
    summary.add("bridges="+bridges.size());summary.add("formalMatchingReferences="+calls.size());
    Files.write(out.resolve("summary.txt"),summary);Files.write(out.resolve("bridges.tsv"),rows);Files.write(out.resolve("calls.tsv"),calls);
    System.out.println(String.join("\n",summary));
  }
}

```

### scratch/issue-72/audit-current.py

```python
exec(open('scratch/issue-72/final-reproduce.py',encoding='utf8').read().split("run([j/'javac.exe'")[0])
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',asm,'-d',w/'target/diagnostics',w/'BridgeAudit.java'],'bridge-build.log')
run([j/'java.exe','-Dfile.encoding=UTF-8','-cp',f'{w}/target/diagnostics;{asm}','BridgeAudit',w/'target/final-reproduce.jar',s/'reference/base-0.6.71.jar',w/'audit'],'bridge-audit.log')
for side,jar in [('staged',w/'target/final-reproduce.jar'),('formal',s/'reference/base-0.6.71.jar')]:
    run([j/'javap.exe','-J-Dfile.encoding=UTF-8','-classpath',jar,'-c','-p','-s','-v','local.lito381.server.NativeDeInstanceState'],f'{side}-instance.javap.txt')
print((w/'audit/summary.txt').read_text(encoding='utf8'))

```

### scratch/issue-72/Dump.java

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public class Dump {
  static Object call(String name, Object... args) throws Exception {
    for (Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name) && m.getParameterCount()==args.length) {
      m.setAccessible(true); return m.invoke(null,args);
    }
    throw new IllegalArgumentException(name);
  }
  static void add(List<String> out,String key,Object value) {out.add(key+"="+String.valueOf(value));}
  public static void main(String[] args) throws Exception {
    JarIndex j=JarIndex.load(Path.of(args[0])); j.addClasspath(Path.of(args[3]));
    Map<String,Set<String>> exact=new TreeMap<>();
    for(String line:Files.readAllLines(Path.of(args[4]))) {
      String key=line.split(" #")[0].trim(); if(key.isEmpty()||key.startsWith("#"))continue;
      key=Arrays.stream(key.split(" ")).filter(v->v.contains("#")).findFirst().orElseThrow(); int hash=key.indexOf('#');
      exact.computeIfAbsent(key.substring(0,hash),k->new TreeSet<>()).add(key.substring(hash+1));
    }
    j.preserveExactMethods(exact);
    Path out=Path.of(args[2]); Files.createDirectories(out);
    for(String entry:Files.readAllLines(Path.of(args[1]))) {
      String name=entry.replace(".class",""); ClassNode c=j.readTree(name); if(c==null)continue;
      List<String> lines=new ArrayList<>();
      for(String key:List.of("access","superName","signature","outerClass","outerMethod","outerMethodDesc","nestHostClass"))add(lines,"class "+key,ClassNode.class.getField(key).get(c));
      for(String key:List.of("interfaces","nestMembers","permittedSubclasses"))add(lines,"class "+key,call("joinSorted",ClassNode.class.getField(key).get(c)));
      add(lines,"class annotations",call("annotations",c));
      add(lines,"class innerClasses",call("innerClasses",c,false,Set.of(),c));
      add(lines,"class recordComponents",call("records",c,j));
      for(FieldNode f:new TreeMap<String,FieldNode>((Map<String,FieldNode>)call("fieldMap",c,j)).values()) {
        String k="field "+f.name+":"+f.desc+" ";
        add(lines,k+"access",call("memberAccess",f.access));add(lines,k+"signature",f.signature);
        add(lines,k+"value",call("value",f.value));add(lines,k+"annotations",call("fieldAnns",f));
      }
      for(var e:new TreeMap<String,MethodNode>((Map<String,MethodNode>)call("methodMap",c,j)).entrySet()) {
        MethodNode m=e.getValue();String k="method "+e.getKey()+" ";
        add(lines,k+"access",call("memberAccess",m.access));add(lines,k+"signature",m.signature);
        add(lines,k+"exceptions",call("joinSorted",m.exceptions));add(lines,k+"annotations",call("methodAnns",m,call("labelIds",m),j));
        add(lines,k+"default",call("defaultValue",m.annotationDefault,j));
        for(String t:(List<String>)call("tryCatch",m,j))lines.add(k+t);
        for(String i:ClassComparer.instructions(m,j))lines.add(k+i);
      }
      Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),lines,StandardCharsets.UTF_8);
    }
  }
}

```

### scratch/issue-72/LambdaAudit.java

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class LambdaAudit {
  static MethodNode find(ClassNode c,String name) {return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
  public static void main(String[] args) throws Exception {
    String owner="local/lito381/server/NativeDeInstanceState";
    JarIndex l=JarIndex.load(Path.of(args[0])),r=JarIndex.load(Path.of(args[1]));l.addClasspath(Path.of(args[1]));
    ClassNode lc=l.readTree(owner),rc=r.readTree(owner);
    MethodNode lm=find(lc,"lambda$sourceAi$scheduleAiTick$22"),rm=find(rc,"lambda$scheduleAiTick$0");
    System.out.println("LEFT "+lm.name+lm.desc+" flags=0x"+Integer.toHexString(lm.access));
    System.out.println(String.join("\n",ClassComparer.instructions(lm,l)));
    System.out.println("RIGHT "+rm.name+rm.desc+" flags=0x"+Integer.toHexString(rm.access));
    System.out.println(String.join("\n",ClassComparer.instructions(rm,r)));
    System.out.println("DIRECT_PATH_COMPARISON "+PathComparison.compare(lm,rm,l,r));
    System.out.println("This diagnostic is not the CLI acceptance result.");
  }
}

```

### scratch/issue-72/evidence-current.py

```python
exec(open('scratch/issue-72/final-reproduce.py',encoding='utf8').read().split("run([j/'javac.exe'")[0])
import difflib, re, collections
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',f'{tc};{asm}','-d',w/'target/diagnostics',w/'Dump.java',w/'LambdaAudit.java'],'diagnostics-build.log')
run([j/'java.exe','-Dfile.encoding=UTF-8','-cp',f'{w}/target/diagnostics;{tc};{asm}','local.lito381.semanticdiff.LambdaAudit',w/'target/final-reproduce.jar',s/'reference/base-0.6.71.jar'],'lambda-audit.txt')
for side,jar in [('left',w/'target/final-reproduce.jar'),('right',s/'reference/base-0.6.71.jar')]:
    run([j/'java.exe','-Dfile.encoding=UTF-8','-cp',f'{w}/target/diagnostics;{tc};{asm}','local.lito381.semanticdiff.Dump',jar,w/'verify_entries.txt',w/f'normalized-{side}',s/'reference/base-0.6.71.jar',w/'deviations.txt'],f'normalized-{side}.log')
name='NativeDeInstanceState.txt'
a=(w/'normalized-left'/name).read_text(encoding='utf8').splitlines(True)
b=(w/'normalized-right'/name).read_text(encoding='utf8').splitlines(True)
(w/'remaining-full.diff').write_text(''.join(difflib.unified_diff(a,b,fromfile='staged',tofile='formal',n=2)),encoding='utf8')
calls=(w/'audit/calls.tsv').read_text(encoding='utf8').splitlines()
owners=sorted({line.split('\t')[1].split('#')[0] for line in calls})
run([j/'javap.exe','-J-Dfile.encoding=UTF-8','-classpath',s/'reference/base-0.6.71.jar','-c','-p','-s',*[o.replace('/','.') for o in owners]],'formal-callers.javap.txt')
print((w/'lambda-audit.txt').read_text(encoding='utf8'))
print('callerClasses',len(owners))
run([j/'javap.exe','-J-Dfile.encoding=UTF-8','-classpath',s/'reference/base-0.6.71.jar','-c','-p','-s','-v','local.lito381.server.NativeDeDeletionDeState'],'formal-handle.javap.txt')

```
