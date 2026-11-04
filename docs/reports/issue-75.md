# Issue #75：A04 正式 0.6.71 暫存對齊（待主控裁定）

## 結論與已驗證

本輪 Java 17 編譯成功，核准的 `@AsRecord` 轉換成功；25 份 staged Java 產生恰好 86 個驗收 entry，缺少／多出皆為空。嚴格比對為 **81 SAME／5 DIFF**，**尚未達到 86 SAME，不可進 A-land**。沒有啟用 `--effects-methods`、`--allow-methods`，也沒有以正式 class bytes 覆蓋產物。

工作基準：`33f3f5c5570a4d8fc4633e5b62e5ff617a2401ef`，分支 `task/75`。目前工作樹從 origin/main 建立；前次 A04 工作提交 `dc42bcf53acf23c72495d247773c44ec24ca7871` 已逐檔取出、與本次 src/main 比較，再補本輪修正。提交時會保留兩邊歷史，不強制推送遠端。

A04 的 25 類在 `a-batches.json` 全部是原始碼（decompiled=false）。沿用以 src/main 為底的前次修改及原註解；本輪另外以 CFR 0.152 反編譯正式 A04 作核對，沒有把反編譯結果整批替換原始碼。CFR 產物不能當正確性證據，例如 Target.take 的 CFR 輸出重複建立 Target，實際位元碼只建立一次，本輪保留同一物件。

正式 JAR SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。
CFR 0.152 SHA-256：`f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`。
環境：Windows／Microsoft OpenJDK 17.0.17／repo 既有 ASM 9.7 工具；#102、#132 已包含於基準。原始專案、正式程序、DB、機密設定、封存及硬連結均未修改。

## 做了什麼

- 暫存 A04 全部 25 類，保留前次對正式規則、技能數值入口、隱身 lease、動畫資源、賽跑 runner、NPC queue 等修補。本輪既有 src/main 原樣編譯試算為 55 SAME／29 DIFF／2 ONLY_RIGHT，另多出 BattleActionCodes；套用修補後 entry 集合已完全吻合。
- `CombatCurseLifecycle.Removal.drain` 每次送出前補回 `context.valid() && lease.target.valid(context.pc)`。前次報告稱「不是漏搬邏輯」不正確；這是真實缺漏，已修正。
- `Target` 改用已合併的 `@AsRecord`；建構子改為 private、八個 accessor 改為 public，保留 private volatile nativeRoleOwner。轉換器修復 record 結構，但 take 的堆疊形狀仍不同，不能宣稱整個 Target SAME。
- 完整差異揭露 `CombatMonsterCurseLifecycle` 的其他缺漏，已補回 `NativeProcHealingWorld.allow`、`NativeCombatAttacker.resetWorld`、poll 路徑的 `NativeNpcLifetime.offer(..., hub)`；並以明確私有靜態方法保留正式 patch 留下的原 lambda 方法體。此類及其全部 inner 現為 SAME。未修改比對器的 synthetic 正規化規則。
- `CombatPoisonAtomic.player` 的 occupied 分支先離開狀態 monitor，再呼叫 leaveInstall，修正前次 leaveInstall 還在 monitor 內的順序。原始正常／例外清理均保留，完整例外範圍差異仍待裁定。
- `BattlePolymorphCatalog.randomForm` 先回傳非 null 的原生選取結果，再走 packaged fallback；仍無法以目前 Java 寫法產生正式的 DUP／POP。

## 怎麼驗證

只編譯 staged，不編譯整個 src/main。`AsRecord.java` 另編至 annotation classpath，不混入本批產物。ASM 依賴由 #102 工具從 `D:/dev-cache/maven/repository` 唯讀取用並驗 SHA；找不到時只下載到 worktree 內工具 target。沒有使用私人 stub。

執行順序：javac `--release 17 -encoding UTF-8` → #102 `as-record` → 只打包本批 class → T020d 全部 86 verify_entries → 獨立 entry 集合比對。javac、轉換器與 semantic-diff 均退出 0；semantic-diff 退出 0 只表示報告成功，不代表驗收通過。編譯唯一診斷為 CombatMonsterCurseLifecycle 原有 unchecked/unsafe operations 提示。

實際輸出：

```text
javac exit=0
as-record: scanned=86 converted=1
SUMMARY SAME=81 SAME_EFFECTS=0 DIFF=5 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=13 ALLOW_METHODS=0
classSelection: {"missingLeft": [], "extraLeft": []}
ENTRY SET expected=86 actual=86 missing=[] extra=[]
INPUT COMMIT 33f3f5c5570a4d8fc4633e5b62e5ff617a2401ef
FORMAL SHA256 e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498
OUTPUT SHA256 1880ca8ecebb557035f9c1ef157defb5369919705fbd41d74422c575ffce7b79
CLASS CONTENT SHA256 86a38a3990ce8c06d5f2037413ef7cac94a43b13ad52c39698bc039b75240b33
ACCEPTANCE NOT MET
```

輸出 JAR 雜湊包含 ZIP 時間，重跑可能改變；CLASS CONTENT SHA256 是依 entry 名稱排序後，串接 UTF-8 名稱、NUL、class bytes 的雜湊。下方來源雜湊鎖定本次未提交狀態的實際驗收輸入；INPUT COMMIT 是其工作基準，並不表示編譯的是未修改 HEAD。

## 待裁定／未驗證什麼

| entry（省略 local/lito381/server/） | 全部剩餘差異與已嘗試的表達 | 狀態 |
| --- | --- | --- |
| BattleDragonEyeCatalog$Eye | canOverwrite()Z：正式直接將 boolean 欄位作為 int 參數傳給 eyeField，直接 IRETURN；Java 必須寫 `? 1 : 0` 及 `!= 0`。新增兩段條件跳躍。Java 型別不允許直接轉成相同指令；未核准新後處理。不能假定 eyeField 非 0/1 回傳值也等價。 | DIFF，需型別／值域裁定 |
| BattlePolymorphCatalog | randomForm：局部變數儲存／載入對正式 DUP／POP。已由三元式改 early return，縮小分支差異；沒有改成重複呼叫 selectOrNull。 | DIFF，需堆疊形狀裁定 |
| CombatCurseLifecycle$Removal | drain：身分檢查已完整補回；正式在檢查前把 Release receiver 留在堆疊，無效時 POP；Java 在條件成立後才載入 receiver，因此少一段 GOTO／POP，後續例外標籤亦位移。 | DIFF，需完整控制流程裁定 |
| CombatCurseLifecycle$Target | take：正式 new Target 後 DUP、設定 owner、ARETURN；Java 以局部變數儲存同一 Target 再設定／回傳。其他方法、欄位、record 結構、存取旗標完全相同。CFR 的重複 new 寫法不採用。 | DIFF，#102 結構轉換已成功，仍需 take 裁定 |
| CombatPoisonAtomic | 多一個 NativeCombatOrigin$Install InnerClasses 項；npc/player 的局部槽、保留回傳值、monitor／catch 範圍不同。修正 occupied 清理順序後，仍無法產生正式把回傳值留在堆疊跨 leaveInstall 的形式。 | DIFF，需例外／同步行為與 metadata 裁定 |

以上是本次 Java 表達與正式位元碼的實際差異，**不是已證明等價，也不宣稱所有 Java 寫法都不可能**。沒有自行增加 effects 核准清單或放寬驗收。僅 firstDiff 會遮住後續缺漏，因此下附五個 entry 的完整正規化 unified diff；其他 81 個 entry 的全量正規化輸出相同。

單元測試／回歸依本批規定不適用：未動 src/main、pom.xml，暫存區不會執行；交 A-land 統一處理。未啟動遊戲／登入器／伺服器、未連 DB、未部署，也未驗證實際遊戲行為。

## 產出路徑與給下一棒的提示

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`：A04 清單內 25 份 Java。
- `docs/reports/issue-75.md`：本報告，含完整差異及可直接重建的診斷程式。
- 未提交：`scratch/issue-75/` 的清單、日誌、正規化輸出、CFR 輸出；`lito伺服器/LiTo381-Server/target/issue-75/` 的 class/JAR；as-record 工具 target。沒有強制加入 ignored 檔案。

請主控 Claude 依 #72 與 #75 的裁定流程處理 `needs:claude`，審閱五類完整差異，決定後續來源改寫或另案正規化／後處理授權。現有 effects-approved.txt 沒有 A04 方法，不能沿用 B09 的核准。不得把本批標成 86 SAME 或交 A-land。調度器應等裁定／新證據再安排審查，避免重複審查同一未達標結果。docs/REPORT.md 未改。

## 可重現步驟

從此 worktree 根目錄執行以下 PowerShell，會從本報告抽出三份附錄腳本到 scratch，再重跑。無須前次機器的暫存檔。JDK／Python 路徑可改為同版本的本機位置；Python 腳本內的 JAVA_HOME 亦須對應。所有寫入均在 worktree。

```powershell
$taskText = Get-Content -Raw -Encoding UTF8 docs/reports/issue-75.md
$taskOut = Join-Path (Get-Location) 'scratch/issue-75'
New-Item -ItemType Directory -Force $taskOut | Out-Null
$taskUtf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($taskName in @('verify.py', 'DumpNormalized.java', 'dump.py')) {
    $taskPattern = '(?s)<!-- FILE: ' + [regex]::Escape($taskName) + ' -->\s*```[^\r\n]*\r?\n(.*?)\r?\n```'
    $taskMatch = [regex]::Match($taskText, $taskPattern)
    if (-not $taskMatch.Success) { throw "找不到附錄 $taskName" }
    [IO.File]::WriteAllText((Join-Path $taskOut $taskName), $taskMatch.Groups[1].Value + "`n", $taskUtf8)
}
& 'D:/dev-orchestrator/tools/python/python.exe' -B -X utf8 scratch/issue-75/verify.py
if ($LASTEXITCODE -ne 0) { throw '編譯或比對執行失敗' }
& 'D:/dev-orchestrator/tools/python/python.exe' -B -X utf8 scratch/issue-75/dump.py
if ($LASTEXITCODE -ne 0) { throw '完整正規化診斷失敗' }
# 目前 verify 最後應顯示 ACCEPTANCE NOT MET；不能把工具 exit 0 當通過。
```

驗證腳本不改 staged；只清除已核對位於本 worktree `target/issue-75/classes` 中的可重建 `.class`。DumpNormalized 使用同一版 ClassComparer 的指令、tryCatch、class／field／method metadata 正規化函式，只讀 JAR 寫文字，不變更比對規則或驗收產物。

## 本次來源 SHA-256

| Java | SHA-256 |
| --- | --- |
| BattleAnimationCatalog.java | `1c348553a0ca7c2d7fd5fcc48c44d04f23cbbf04be9db66204ee1d899300ba8c` |
| BattleArmBreakerStatus.java | `6b51a037b0001ee185fa6cf139e9b18b7eae229b05c01f872f4d53d09c3725dd` |
| BattleArmorBreak.java | `8cc1158d6681072db0207ca2505b5e206bdef1a95236b1873332595a23a59297` |
| BattleClearMindStatus.java | `aa99549aa2e80a11e274a0d2162262e51127375eb175705ebcb90453d2777c17` |
| BattleDragonEyeCatalog.java | `8763d6a02a9b634254ca7bfe02824828856ac74a3672e13481ca2248aaa9698b` |
| BattleEarthBlessStatus.java | `2a203a17bd456b77bb170c47d15d68433eb6d88b0d7a83ccc25e730c908385b8` |
| BattleFreezeEffects.java | `ece006e9ebeaec12b89bda8af45bbeb9bac6627d792364305e7197e763c2cddb` |
| BattleInvisibilityStatus.java | `f09fabc5e974124101a51a30f4b5404f85552f48460a60208f79f78c6a11d149` |
| BattleIronSkinStatus.java | `820199a8b723abfbcccd5df84acec6b04bf63f16fdeb57d215a3bb4a8348407c` |
| BattleJoyOfPain.java | `fce4531a4ec4e9246b53ea14eb955a0dd382f0a6dcbe4ef23d668408a6a3a1a9` |
| BattleMortalBody.java | `b57810c00babc8fc0db35c595c15d2442d82044cb01d0c76aaefb2fc2f260593` |
| BattlePatienceStatus.java | `678fc5d7309bb93afbbdfcb2e40a1ec40218bae767224479d365aa86206c5b29` |
| BattlePetRaceCourseCatalog.java | `2643d6d4c152a5743a52726c1d288ff1aa530c22e6e13fb4fb514c4d1e2e178d` |
| BattlePhysicalDefense.java | `94793b280dbf610e5e3900ff40d3b3af5cdfdf26d4894fa23721c0675bcf77ca` |
| BattlePhysicalProcBuff.java | `7e1719bb3fa630674130e5d41077cc3e47b0b2aacb8e375ba751db938792928f` |
| BattlePolymorphCatalog.java | `62945bad014fde12493de91790e0032ed6f3a0b1ea4550bff21588e981489365` |
| BattleRules.java | `18653d9e823f18e62b9163feead143f59ab31156c24a2dbd17fee27c4a666c97` |
| BattleShockStunEffects.java | `8842131374ce50889b18a1c030cf9b6a8a9d456df645a32f7eecf15f8216abd6` |
| BattleStrikerGale.java | `9b7dd6becf943a252f086a86739a395a4c837f6465faf6bcca9f8670bb26f0f9` |
| BattleTurnUndead.java | `b5faf3e19ef4e84d90b3f5bc152b397a22667eb7eef313a2ae2a2cc6344d0f8b` |
| BattleWeaponBreak.java | `468f05fdca64e1f56c8ad7471135dfc4c3620ebff3e6670f141d002724b3fcec` |
| CombatCurseLifecycle.java | `b00561e2de7776dc1f2ca9797331fde834c9e3bcf7b3fc081b2f04670a4be612` |
| CombatMonsterCurseLifecycle.java | `6a6a84a1f300a4dd1afd9de670ae99c2e5af56f08bc54fce0d92bf6e50ed94dc` |
| CombatNpcDamageQueue.java | `97ef4e2d278462a50c1f2a16c2a46e496c7ebf66cd30d51a9e92510da68b8ea2` |
| CombatPoisonAtomic.java | `f9d64610e0aaaa6ebd60352e93cfbec70269d67bf029fbe5023d119bef270237` |

## 完整正規化差異

`-` 是本次 staged javac＋核准轉換器，`+` 是正式 JAR；包含每個不同 hunk，無省略其他差異。所有 86 類的 class／field／method metadata、try-catch 與指令都經同一診斷程式比較。

```diff
--- staged/BattleDragonEyeCatalog$Eye.txt
+++ formal/BattleDragonEyeCatalog$Eye.txt
@@ -255,21 +255,9 @@
 annotationDefault
 ALOAD 0
 GETFIELD local/lito381/server/BattleDragonEyeCatalog$Eye.canOverwrite:Z
-IFEQ L0
-CONST Integer:1
-GOTO L1
-LABEL L0
-CONST Integer:0
-LABEL L1
 ALOAD 0
 CONST str:"canOverwrite"
 INVOKESTATIC local/lito381/server/CombatSpecialBuffs.eyeField(ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I
-IFEQ L2
-CONST Integer:1
-GOTO L3
-LABEL L2
-CONST Integer:0
-LABEL L3
 IRETURN
 END METHOD
 METHOD castGfx()I

--- staged/BattlePolymorphCatalog.txt
+++ formal/BattlePolymorphCatalog.txt
@@ -458,12 +458,11 @@
 annotationDefault
 ILOAD 0
 INVOKESTATIC local/lito381/server/NativePolymorphSource.selectOrNull(I)Llocal/lito381/server/BattlePolymorphCatalog$Form;
-ASTORE 1
-ALOAD 1
+DUP
 IFNULL L0
-ALOAD 1
-ARETURN
-LABEL L0
+ARETURN
+LABEL L0
+POP
 ILOAD 0
 INVOKESTATIC local/lito381/server/BattlePolymorphCatalog.nativePackagedRandomForm(I)Llocal/lito381/server/BattlePolymorphCatalog$Form;
 ARETURN

--- staged/CombatCurseLifecycle$Removal.txt
+++ formal/CombatCurseLifecycle$Removal.txt
@@ -139,13 +139,13 @@
 TRY L38 L39 L50 java/lang/Throwable
 TRY L45 L49 L50 java/lang/Throwable
 TRY L50 L51 L50 java/lang/Throwable
-TRY L56 L57 L58 java/lang/Throwable
-TRY L58 L59 L58 java/lang/Throwable
-TRY L7 L39 L61 java/lang/Throwable
-TRY L45 L55 L61 java/lang/Throwable
-TRY L63 L64 L65 java/lang/Throwable
-TRY L65 L66 L65 java/lang/Throwable
-TRY L61 L62 L61 java/lang/Throwable
+TRY L57 L58 L59 java/lang/Throwable
+TRY L59 L60 L59 java/lang/Throwable
+TRY L7 L39 L62 java/lang/Throwable
+TRY L45 L56 L62 java/lang/Throwable
+TRY L64 L65 L66 java/lang/Throwable
+TRY L66 L67 L66 java/lang/Throwable
+TRY L62 L63 L62 java/lang/Throwable
 ALOAD 0
 DUP
 ASTORE 2
@@ -535,11 +535,12 @@
 LABEL L53
 ALOAD 3
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L55
+IFEQ L56
 ALOAD 3
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/CombatCurseLifecycle$Release
 ASTORE 4
+ALOAD 4
 ALOAD 0
 GETFIELD local/lito381/server/CombatCurseLifecycle$Removal.context:Llocal/lito381/server/CombatCurseLifecycle$Context;
 INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Context.valid()Z
@@ -552,58 +553,60 @@
 GETFIELD local/lito381/server/CombatCurseLifecycle$Context.pc:Llocal/lito381/server/ClientSession;
 INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Target.valid(Llocal/lito381/server/ClientSession;)Z
 IFEQ L54
-ALOAD 4
 INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Release.send()V
+GOTO L55
 LABEL L54
+POP
+LABEL L55
 GOTO L53
-LABEL L55
+LABEL L56
 ALOAD 0
 DUP
 ASTORE 2
 MONITORENTER
-LABEL L56
+LABEL L57
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/CombatCurseLifecycle$Removal.draining:Z
 ALOAD 2
 MONITOREXIT
-LABEL L57
-GOTO L60
 LABEL L58
+GOTO L61
+LABEL L59
 ASTORE 9
 ALOAD 2
 MONITOREXIT
-LABEL L59
+LABEL L60
 ALOAD 9
 ATHROW
-LABEL L60
-GOTO L68
 LABEL L61
+GOTO L69
+LABEL L62
 ASTORE 10
-LABEL L62
+LABEL L63
 ALOAD 0
 DUP
 ASTORE 11
 MONITORENTER
-LABEL L63
+LABEL L64
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/CombatCurseLifecycle$Removal.draining:Z
 ALOAD 11
 MONITOREXIT
-LABEL L64
-GOTO L67
 LABEL L65
+GOTO L68
+LABEL L66
 ASTORE 12
 ALOAD 11
 MONITOREXIT
-LABEL L66
+LABEL L67
 ALOAD 12
 ATHROW
-LABEL L67
+LABEL L68
 ALOAD 10
 ATHROW
-LABEL L68
+LABEL L69
 RETURN
 END METHOD
 METHOD fast()V

--- staged/CombatCurseLifecycle$Target.txt
+++ formal/CombatCurseLifecycle$Target.txt
@@ -221,12 +221,10 @@
 CONST str:"classicWorldInstanceId"
 INVOKESTATIC local/lito381/server/CombatCurseLifecycle.number(Ljava/lang/Object;Ljava/lang/String;)J
 INVOKESPECIAL local/lito381/server/CombatCurseLifecycle$Target.<init>(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;JJJ)V
-ASTORE 1
-ALOAD 1
+DUP
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.owner(Llocal/lito381/server/ClientSession;)Ljava/lang/Object;
 PUTFIELD local/lito381/server/CombatCurseLifecycle$Target.nativeRoleOwner:Ljava/lang/Object;
-ALOAD 1
 ARETURN
 END METHOD
 METHOD toString()Ljava/lang/String;

--- staged/CombatPoisonAtomic.txt
+++ formal/CombatPoisonAtomic.txt
@@ -13,7 +13,6 @@
 innerClass local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind|local/lito381/server/ClassicWorldMonsterPoisonAttack|Kind|0x4018
 innerClass local/lito381/server/CombatPoisonAtomic$NpcState|local/lito381/server/CombatPoisonAtomic|NpcState|0x18
 innerClass local/lito381/server/CombatPoisonLifecycle$Scope|local/lito381/server/CombatPoisonLifecycle|Scope|0x18
-innerClass local/lito381/server/NativeCombatOrigin$Install|local/lito381/server/NativeCombatOrigin|Install|0x18
 recordComponents
 FIELD C:Ljava/lang/String;
 access 0x1a
@@ -264,17 +263,16 @@
 exceptions
 annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault
-TRY L3 L5 L6 java/lang/Throwable
-TRY L6 L7 L6 java/lang/Throwable
-TRY L0 L1 L10 java/lang/Throwable
-TRY L2 L9 L10 java/lang/Throwable
+TRY L2 L4 L5 java/lang/Throwable
+TRY L5 L6 L5 java/lang/Throwable
+TRY L0 L7 L7 java/lang/Throwable
 ALOAD 0
 LLOAD 1
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.enterNpc(Llocal/lito381/server/ClientSession;J)Llocal/lito381/server/NativeCombatOrigin$Install;
-ASTORE 10
+ASTORE 13
 LABEL L0
 INVOKESTATIC local/lito381/server/CombatRules.loaded()Z
-IFNE L2
+IFNE L1
 ALOAD 0
 LLOAD 1
 ILOAD 3
@@ -283,19 +281,16 @@
 LLOAD 6
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.npcState(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;
-ASTORE 11
-ALOAD 10
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ALOAD 11
-LABEL L1
-ARETURN
-LABEL L2
+ALOAD 13
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+ARETURN
+LABEL L1
 ALOAD 0
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
 DUP
-ASTORE 12
+ASTORE 10
 MONITORENTER
-LABEL L3
+LABEL L2
 ALOAD 0
 LLOAD 1
 ILOAD 3
@@ -307,7 +302,7 @@
 ASTORE 11
 ALOAD 11
 INVOKEVIRTUAL local/lito381/server/CombatPoisonAtomic$NpcState.applied()Z
-IFEQ L4
+IFEQ L3
 ALOAD 0
 CONST str:"poisonAttacker"
 CONST null
@@ -316,29 +311,26 @@
 ALOAD 9
 LLOAD 6
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.installed(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;J)V
+LABEL L3
+ALOAD 11
+ALOAD 10
+MONITOREXIT
 LABEL L4
+ALOAD 13
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+ARETURN
+LABEL L5
+ASTORE 12
+ALOAD 10
+MONITOREXIT
+LABEL L6
 ALOAD 12
-MONITOREXIT
-LABEL L5
-GOTO L8
-LABEL L6
-ASTORE 13
-ALOAD 12
-MONITOREXIT
+ATHROW
 LABEL L7
+ASTORE 14
 ALOAD 13
-ATHROW
-LABEL L8
-ALOAD 10
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ALOAD 11
-LABEL L9
-ARETURN
-LABEL L10
-ASTORE 11
-ALOAD 10
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ALOAD 11
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+ALOAD 14
 ATHROW
 END METHOD
 METHOD object(Llocal/lito381/server/ClientSession;J)V
@@ -451,42 +443,43 @@
 exceptions
 annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault
-TRY L3 L6 L7 java/lang/Throwable
+TRY L2 L3 L7 java/lang/Throwable
+TRY L4 L6 L7 java/lang/Throwable
 TRY L7 L8 L7 java/lang/Throwable
-TRY L0 L1 L11 java/lang/Throwable
-TRY L2 L10 L11 java/lang/Throwable
+TRY L0 L9 L9 java/lang/Throwable
 ALOAD 5
 ALOAD 6
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.enterPlayer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeCombatOrigin$Install;
+ASTORE 11
+LABEL L0
+INVOKESTATIC local/lito381/server/CombatRules.loaded()Z
+IFNE L1
+ALOAD 0
+LLOAD 1
+ILOAD 3
+ILOAD 4
+ALOAD 5
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.activatePlayer(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;)Z
+ALOAD 11
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+IRETURN
+LABEL L1
+ALOAD 0
+DUP
 ASTORE 8
-LABEL L0
-INVOKESTATIC local/lito381/server/CombatRules.loaded()Z
-IFNE L2
-ALOAD 0
-LLOAD 1
-ILOAD 3
-ILOAD 4
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.activatePlayer(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;)Z
-ISTORE 9
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ILOAD 9
-LABEL L1
-IRETURN
-LABEL L2
-ALOAD 0
-DUP
-ASTORE 10
 MONITORENTER
-LABEL L3
+LABEL L2
 ALOAD 5
 LLOAD 1
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.occupied(Llocal/lito381/server/ClientSession;J)Z
 IFEQ L4
 CONST Integer:0
-ISTORE 9
-GOTO L5
+ALOAD 8
+MONITOREXIT
+LABEL L3
+ALOAD 11
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+IRETURN
 LABEL L4
 ALOAD 0
 LLOAD 1
@@ -508,28 +501,25 @@
 LLOAD 1
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.installed(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;J)V
 LABEL L5
+ILOAD 9
+ALOAD 8
+MONITOREXIT
+LABEL L6
+ALOAD 11
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+IRETURN
+LABEL L7
+ASTORE 10
+ALOAD 8
+MONITOREXIT
+LABEL L8
 ALOAD 10
-MONITOREXIT
-LABEL L6
-GOTO L9
-LABEL L7
-ASTORE 11
-ALOAD 10
-MONITOREXIT
-LABEL L8
+ATHROW
+LABEL L9
+ASTORE 12
 ALOAD 11
-ATHROW
-LABEL L9
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ILOAD 9
-LABEL L10
-IRETURN
-LABEL L11
-ASTORE 9
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ALOAD 9
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+ALOAD 12
 ATHROW
 END METHOD
 METHOD poll(Llocal/lito381/server/BattleTimedStatus;JILlocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus$PoisonPoll;
```

## 附錄：verify.py

<!-- FILE: verify.py -->
```python
from pathlib import Path
import json, subprocess, os, sys, zipfile, hashlib
ROOT=Path.cwd(); SERVER=ROOT/'lito伺服器/LiTo381-Server'; WORK=ROOT/'scratch/issue-75'; OUT=SERVER/'target/issue-75'; OUT.mkdir(parents=True,exist_ok=True)
os.environ['JAVA_HOME']='C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
os.environ['AS_RECORD_MAVEN_REPO']='D:/dev-cache/maven/repository'
os.environ['TEMP']=os.environ['TMP']=str(OUT)
sys.dont_write_bytecode=True
sys.path.insert(0,str(SERVER/'tools/as-record'))
import as_record as tool
BATCH=next(b for b in json.loads((ROOT/'docs/combat-merge/a-batches.json').read_text('utf-8'))['batches'] if b['id']=='A04')
STAGED=SERVER/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
def run(args):
 p=subprocess.run([str(a) for a in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 print(p.stdout.decode('utf-8',errors='replace')); p.check_returncode(); return p.stdout
if __name__=='__main__':
 STAGED.mkdir(parents=True,exist_ok=True)
 for name in BATCH['classes']:
  dest=STAGED/(name+'.java')
  if not dest.exists(): raise RuntimeError('Missing staged source: '+str(dest))
 deps=tool.dependencies()
 tool.compile_java([SERVER/'src/main/java/local/lito381/server/AsRecord.java'],OUT/'annotation','')
 tool.compile_java(sorted((SERVER/'tools/semantic-diff/src/main/java').rglob('*.java')),OUT/'diff-tool',deps)
 (WORK/'entries.txt').write_text('\n'.join(BATCH['verify_entries'])+'\n','utf-8')
 classes=OUT/'classes'; classes.mkdir(exist_ok=True)
 # Remove only generated class files in this exact worktree output directory.
 assert classes.resolve().is_relative_to(ROOT.resolve())
 for p in classes.rglob('*.class'): p.unlink()
 log=run([tool.jdk('javac'),'-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-cp',str(SERVER/'reference/base-0.6.71.jar')+';'+str(SERVER/'lib/*')+';'+str(OUT/'annotation'),'-d',classes,*[STAGED/(n+'.java') for n in BATCH['classes']]])
 (WORK/'javac.log').write_bytes(log)
 log=run([tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',tool.build(),'local.lito381.asrecord.AsRecord',classes]); (WORK/'as-record.log').write_bytes(log)
 jar=OUT/'a04.jar'
 with zipfile.ZipFile(jar,'w') as z:
  for p in sorted(classes.rglob('*.class')): z.write(p,p.relative_to(classes).as_posix())
 log=run([tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',str(OUT/'diff-tool')+';'+deps,'local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',SERVER/'reference/base-0.6.71.jar','--classes',WORK/'entries.txt','--json',WORK/'diff.json','--md',WORK/'diff.md']); (WORK/'semantic.log').write_bytes(log)
 data=json.loads((WORK/'diff.json').read_text('utf-8'))
 print(json.dumps(data['counts'])); print(json.dumps(data['classSelection']))
 for c in data['classes']:
  if c['status']!='SAME': print(json.dumps(c,ensure_ascii=False))

 actual={p.relative_to(classes).as_posix() for p in classes.rglob('*.class')}
 expected=set(BATCH['verify_entries'])
 assert actual==expected, {'missing':sorted(expected-actual),'extra':sorted(actual-expected)}
 print('ENTRY SET expected='+str(len(expected))+' actual='+str(len(actual))+' missing=[] extra=[]')
 print('INPUT COMMIT '+subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip())
 for label,path in [('FORMAL',SERVER/'reference/base-0.6.71.jar'),('OUTPUT',jar)]:
  print(label+' SHA256 '+hashlib.sha256(path.read_bytes()).hexdigest())
 content=hashlib.sha256()
 for entry in sorted(expected):
  content.update(entry.encode('utf-8')+b'\0'+(classes/entry).read_bytes())
 print('CLASS CONTENT SHA256 '+content.hexdigest())
 print('ACCEPTANCE '+('PASS' if data['counts']['SAME']==86 and not any(data['counts'][k] for k in ['DIFF','ONLY_LEFT','ONLY_RIGHT','SAME_EFFECTS']) else 'NOT MET'))
```

## 附錄：DumpNormalized.java

<!-- FILE: DumpNormalized.java -->
```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public class DumpNormalized {
 static Object call(String name, Object... args) throws Exception {
  for(Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(null,args);}
  throw new IllegalArgumentException(name);
 }
 static void line(List<String> o,String k,Object v){o.add(k+" "+v);}
 public static void main(String[] args) throws Exception {
  JarIndex jar=JarIndex.load(Path.of(args[0]));Path dest=Path.of(args[2]);Files.createDirectories(dest);
  for(String name:Files.readAllLines(Path.of(args[1]))){name=name.replace(".class","").trim();if(name.isEmpty())continue;ClassNode c=jar.readTree(name);if(c==null)continue;
   List<String> o=new ArrayList<>();
   line(o,"CLASS",name);line(o,"access",c.access);line(o,"super",c.superName);line(o,"signature",c.signature);line(o,"interfaces",call("joinSorted",c.interfaces));line(o,"outerClass",c.outerClass);line(o,"outerMethod",c.outerMethod+" "+c.outerMethodDesc);line(o,"nestHost",c.nestHostClass);line(o,"nestMembers",call("joinSorted",c.nestMembers));line(o,"permittedSubclasses",call("joinSorted",c.permittedSubclasses));line(o,"annotations",call("annotations",c));
   for(String s:call("innerClasses",c).toString().split("; "))line(o,"innerClass",s);
   line(o,"recordComponents",call("records",c,jar));
   Map<String,FieldNode> fields=(Map<String,FieldNode>)call("fieldMap",c);
   for(var e:fields.entrySet()){FieldNode f=e.getValue();line(o,"FIELD",e.getKey());line(o,"access",call("memberAccess",f.access));line(o,"signature",f.signature);line(o,"value",call("value",f.value));line(o,"annotations",call("fieldAnns",f));}
   Map<String,MethodNode> methods=(Map<String,MethodNode>)call("methodMap",c,jar);
   for(var e:methods.entrySet()){MethodNode m=e.getValue();line(o,"METHOD",e.getKey());line(o,"access",call("memberAccess",m.access));line(o,"signature",m.signature);line(o,"exceptions",call("joinSorted",m.exceptions));line(o,"annotations",call("methodAnns",m,call("labelIds",m),jar));line(o,"annotationDefault",call("defaultValue",m.annotationDefault,jar));o.addAll((List<String>)call("tryCatch",m,jar));o.addAll(ClassComparer.instructions(m,jar));o.add("END METHOD");}
   Files.write(dest.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),o);
  }
 }
}
```

## 附錄：dump.py

<!-- FILE: dump.py -->
```python
import sys,difflib
sys.path.insert(0,'scratch/issue-75')
from verify import *
p=WORK/'DumpNormalized.java';p.write_text(p.read_text('utf-8-sig'),'utf-8')
cp=str(OUT/'diff-tool')+';'+tool.dependencies()
tool.compile_java([p],OUT/'diff-tool',cp)
for side,jar in [('left',OUT/'a04.jar'),('right',SERVER/'reference/base-0.6.71.jar')]:
 run([tool.jdk('java'),'-cp',cp,'local.lito381.semanticdiff.DumpNormalized',jar,WORK/'entries.txt',WORK/side])
result=[]
for p in sorted((WORK/'right').glob('*.txt')):
 a=(WORK/'left'/p.name).read_text('utf-8').splitlines(True);b=p.read_text('utf-8').splitlines(True)
 diff=''.join(difflib.unified_diff(a,b,fromfile='staged/'+p.name,tofile='formal/'+p.name))
 if diff:result.append(diff)
(WORK/'full.diff').write_text('\n'.join(result),'utf-8');print('\n'.join(result))
```
