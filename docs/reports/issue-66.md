# Issue #66：T020-B05 戰鬥併入暫存

**結論：未通過全部 SAME 驗收，已標 `needs:claude` 待主控裁定。** 本輪完成 48 份 staged Java 的整批編譯；核准的 `@AsRecord` 轉換後，120 entries 為 **110 SAME／10 DIFF**，無缺少或多出。不得據此落地 B-land。

## 做了什麼

從凍結 `merge-inputs/50635f7e/sources/local/lito381/server/` 複製 B05 的全部 48 檔作為起點，逐檔對照 CFR 0.152 反編譯的候選與凍結編譯結果，將差異移植回 Java。涵蓋指定 25 個 asm-only entry 的 13 個外層家族，以及驗證額外發現的 Proc68Icon、ProcScalarSkills、TargetIconBatch／Work、TargetMask、TamedWorldLifecycle／Respawn 改動。

移植內容包括：保留最初玩家／NPC 生命週期與受眾、目標與材料失敗判定、控制 NPC elemental 施法、毒傷傳輸、原始傷害工廠、武器與負向效果最終送包檢查、靜態 NPC 退役／重生／場景更新及召喚刪除受眾。`NativeSourceRawDamage.Output` 使用 `@AsRecord(components={"pc","binding","packet","note"}, finalFields={"nativeSourceRawAudience"})`，明寫原有 accessors／建構子，沒有自訂 Object 三方法。

只交付本批 48 個 staged `.java` 與本報告。未修改 src/main、pom.xml、docs/REPORT.md、工具或允許清單。未取用舊分支的 overlay，未複製候選方法／class 到驗收產物。候選 class 的抽取僅供 CFR 分析，與 `target/issue-66/classes` 完全分開。

## 已驗證

工作目錄：`D:/lineage開發/3-lito開發/scratch/wt/task-66`；來源基準 `1d27dfa65c390a15c7f6c7b843b149341a115263`；分支 `task/66`。環境為 Windows、Microsoft OpenJDK／javac 17.0.17、ASM 9.7、CFR 0.152。使用此基準既有 T020d 與 #102 工具，未自行改動正規化規則。

```text
javac（一次傳入全部 48 份 staged Java）exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record: scanned=120 converted=1
as-record exit 0
MISSING [] EXTRA []
PROVENANCE PASS: only Output converted; JAR equals complete classes directory
SUMMARY SAME=110 DIFF=10 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
semantic-diff exit 0（工具有 DIFF 仍回傳 0，不代表驗收通過）
```

逐 class 比較轉換前後 SHA-256：僅 `NativeSourceRawDamage$Output.class` 改變；其餘 119 個 javac 產物 bytes 保持原樣。比較 JAR 包含 classes 目錄全部 120 個檔案，逐 entry bytes 一致，未篩掉多出項或補入缺少項。`Output` 本身為 SAME。

初次直接編譯凍結來源失敗，原因包括 Member／Output 新欄位、Output 五參數建構子、RawDamage 原始方法改名，以及 TargetIconBatch.deferTarget 缺少；本輪全批編譯已消除這些錯誤。舊 PR 的逐檔編譯及 overlay 結論不作為本次證據。

## 可重現指令

在上述 worktree 根目錄將下列 Python 存成 `scratch/issue-66/verify.py`，執行 `D:/dev-orchestrator/tools/python/python.exe -X utf8 scratch/issue-66/verify.py`。使用新的 `target/issue-66-recheck` 輸出目錄，避免沿用舊 classes；如已存在請改成另一個全新 target 子目錄。

`AsRecord` 註解先單獨編到 annotation classpath，不放進本批 classes；這是題目 javac classpath 唯一增加的建置依賴。worktree 沒有 lib 目錄，保留 `lib/*` 與指定 classpath 形式。

```python
import hashlib, json, os, subprocess, sys, zipfile
from pathlib import Path
sys.dont_write_bytecode = True
root = Path.cwd()
server = root / "lito伺服器/LiTo381-Server"
out = server / "target/issue-66-recheck"
out.mkdir(parents=True, exist_ok=False)
scratch = root / "scratch/issue-66"
scratch.mkdir(parents=True, exist_ok=True)
os.environ["JAVA_HOME"] = "C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot"
os.environ["AS_RECORD_MAVEN_REPO"] = "D:/dev-cache/maven/repository"
os.environ["TEMP"] = os.environ["TMP"] = str(out)
sys.path.insert(0, str(server / "tools/as-record"))
import as_record as tool
b = next(x for x in json.loads((root / "docs/combat-merge/batches.json").read_text("utf-8"))["batches"] if x["id"] == "B05")
candidate = server / "reference/candidate-50635f7e.jar"
expected = set(b["verify_entries"])
entries = scratch / "verify-entries.txt"
entries.write_text("\n".join(b["verify_entries"]) + "\n", encoding="utf-8")
staged = server / "merge-inputs/50635f7e/staged/local/lito381/server"
classes = out / "classes"
tool.compile_java([server / "src/main/java/local/lito381/server/AsRecord.java"], out / "annotation", "")
cp = os.pathsep.join(map(str, [candidate, server / "reference/base-0.6.71.jar", server / "lib/*", out / "annotation"]))
# compile_java 實際命令：javac --release 17 -encoding UTF-8 -cp <cp> -d <classes> <全部 48 檔>
tool.compile_java([staged / (n + ".java") for n in b["classes"]], classes, cp)
def hashes():
    return {p.relative_to(classes).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in classes.rglob("*.class")}
before = hashes()
tool.run([tool.jdk("java"), "-cp", tool.build(), "local.lito381.asrecord.AsRecord", classes])
after = hashes()
assert set(after) == set(before) == expected
assert [e for e in before if before[e] != after[e]] == ["local/lito381/server/NativeSourceRawDamage$Output.class"]
jar = out / "batch.jar"
with zipfile.ZipFile(jar, "w") as z:
    for p in sorted(classes.rglob("*.class")):
        z.write(p, p.relative_to(classes).as_posix())
with zipfile.ZipFile(jar) as z:
    assert set(z.namelist()) == expected
    assert all(z.read(e) == (classes / e).read_bytes() for e in expected)
tool.compile_java(sorted((server / "tools/semantic-diff/src/main/java").rglob("*.java")), out / "semantic-diff", tool.dependencies())
result = scratch / "recheck.json"
tool.run([tool.jdk("java"), "-cp", str(out / "semantic-diff") + os.pathsep + tool.dependencies(),
          "local.lito381.semanticdiff.SemanticDiff", "--left", jar, "--right", candidate,
          "--classes", entries, "--json", result, "--md", scratch / "recheck.md"])
data = json.loads(result.read_text("utf-8"))
print(data["counts"], data["classSelection"])
assert not data["classSelection"]["missingLeft"] and not data["classSelection"]["extraLeft"]
# 本輪此處預期失敗；不能以 CLI exit 0 充當驗收。
assert data["counts"]["SAME"] == 120 and all(data["counts"][k] == 0 for k in ["DIFF", "ONLY_LEFT", "ONLY_RIGHT"])
```

## 推測與待裁定

下表是 10 個尚未 SAME 的 entry（共同前綴 `local/lito381/server/`，後綴 `.class`）。所有差異均以本輪實際輸出為準；「行為等價」仍需主控裁定，並非已驗證結論。本輪不新增 effects／metadata 豁免。#132／#135 的後續正規化應合併後再由執行器重跑，不推定它們會消除全部差異。

| entry | 原因與界線 |
|---|---|
| `NativeSourcePoisonNpc` | 候選重用同內容的 lambda（recipients→members、transport task→status）；javac 17 為各語法位置產生不同 synthetic 方法。原始 status task 在 ASM 改接後仍存在，以常數 false 分支保留 javac 合成方法，該分支無執行指令。剩餘差異為重複方法及 bootstrap target，不冒稱 SAME。 |
| `NativeSourceProcScalarSkills` | expiry(I)Ljava/lang/String;：候選以 DUP／IFNULL／POP 保留一次 extras.expiry 結果；Java 用區域變數確保只呼叫一次，產生 ASTORE／ALOAD。不得重呼叫可能有副作用的方法來湊指令。 |
| `NativeSourceRawDamage$Action` | 建構子 explicitPeer 的 lambda method handle：候選 H7（REF_invokeSpecial），javac 17 H5（REF_invokeVirtual）；實際目標為同一 private instance lambda，其餘完整正規化內容一致。Java lambda 語法無法指定 bootstrap handle tag。 |
| `NativeSourceRawDamage$Member` | nativeSourceNpcHpObserver 保持 final transient。候選三／五參數建構子未賦值，六參數建構子先 this(五參數) 再寫 final；javac 的確定賦值規則不接受。Java 三／五參數明確賦 null，六參數展開欄位初始化。不能將本非 record 類別套 @AsRecord，否則父類／record metadata 與 equals 等契約會改變。需主控決定合法來源表示或另設受控方案。 |
| `NativeSourceSignedRecovery` | member(...): fromActor 的 actor／life 輸入，候選 DUP 複用堆疊值，Java 使用兩次 ALOAD 同一區域變數；實際呼叫參數與順序相同，但現行工具不消除此差異。 |
| `NativeSourceTargetIconBatch` | 唯一差異為引用 NativeSourceAcceptedPlayers$Participant 的 InnerClasses 條目：javac 因新增方法參數自動記錄，ASM 候選沒有。方法／欄位正規化內容全部一致。#135 的引用 metadata 規則預期可處理，尚未在本基準驗證。 |
| `NativeSourceTargetIconBatch$Work` | 兩個建構子使用同一 task body；ASM 複製建構子時重用原 lambda，javac 17 產生兩份相同 synthetic 方法。需 lambda 等價正規化／主控裁定，不能覆寫方法或改產物。 |
| `NativeSourceWindTargets` | run(...) 在 ASM 中重新賦值 request 後仍由 lambda 捕獲。Java lambda 需要 effectively-final，因此增加 selectedRequest 別名，出現 ALOAD／ASTORE 及後續槽位位移；單純一對一槽位重新編號不一定足夠，尚待裁定。 |
| `NativeStaticNpcMotion` | ASM 將 dialog／talk 方法改名，但保留 lambda$dialog／lambda$talk 名稱；Java 依新方法名稱產生 lambda$nativeStaticRespawnOriginalDialog／lambda$nativeStaticReplacementOriginalTalk。方法本體對應相同；現行工具只正規化數字尾碼，不處理此前綴改名。 |
| `NativeStaticNpcScene` | heading／transform／restore／turn 的 ASM 在 MONITOREXIT 後、RETURN 前使用仍留在堆疊的結果呼叫 motion callback。Java 改用區域變數與監視器外呼叫，維持正常／例外路徑的鎖邊界；因此 locals、分支、exception table 與 LABEL 不同。prune 的 ASM 改名也保留舊 lambda 前綴。完整差異如下，控制流程等價仍需主控審查，不能只憑首差異豁免。 |

## 未驗證什麼／給下一棒的提示

- **Claude 雲端主控**：先裁定非 record final 欄位的合法來源表示、lambda 重用／名稱／H5-H7 差異、DUP 與 locals 表示，以及監視器外 callback 的控制流程差異。Issue #66 已標 `needs:claude`；PR #88 以待裁定狀態交接，不能自動認定完成。
- **下一輪本機執行器**：取得裁定及 #132／#135 已合併版本後，仍以完整 48 檔一次 javac、唯一 #102 後處理、完整 120-entry 比對重跑。禁止恢復先前 overlay；不要沿用舊「120 SAME」宣告。
- **B-land**：本批未通過，暫不搬入 src/main。單元測試／整合回歸依任務於 B-land 統一執行；本輪未執行遊戲、登入器、伺服器或 DB，也未做正式部署。
- 本輪取代遠端舊嘗試 `0d5fc06748d6b82f60e4dd6cfb23fd3954ccd3e9` 的交付。以保留本輪樹的歷史合併接回舊分支再一般推送，不強制改寫遠端；舊 overlay 不在最終交付樹，舊提交僅供追溯。

## 產出與證據

- 交付：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`（B05 清單中的 48 檔）、`docs/reports/issue-66.md`。
- 未入 git 本機證據：`scratch/issue-66/` 的 javac.log、as-record.log、semantic-diff.json／md／log、provenance.json、CFR 前後原始碼與逐類 diff、left／right 完整正規化輸出；分析腳本同目錄。建置產物位於伺服器 `target/issue-66/` 與既有工具 `tools/as-record/target/`。
- 完整差異已嵌入下方，不依賴審查者能存取 ignored scratch；共同部分省略，但所有不相同的正規化行均保留，沒有首差異截斷。

凍結來源 bytes 未變：30 檔；移植修改：18 檔。修改清單：

```text
NativeSourcePoisonNpc
NativeSourceProc68Icon
NativeSourceProcScalarSkills
NativeSourceRawDamage
NativeSourceRemainingMaterials
NativeSourceSignedRecovery
NativeSourceSingleNpc
NativeSourceTargetIconBatch
NativeSourceTargetMask
NativeSourceTargetNegativeLife
NativeSourceWeaponBuffTargets
NativeSourceWeaponFinalFrame
NativeSourceWindTargets
NativeStaticCancellationReplacement
NativeStaticNpcMotion
NativeStaticNpcScene
NativeSummonGroundTransfer
NativeTamedWorldLifecycle
```

- candidate SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。
- base SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。
- CFR SHA-256：`f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`。
- batch.jar SHA-256：`79e9462b75590bb9cdb8767ebf333e69a0c8ff95bed0b1239edb11d97d753374`。

## 全部正規化差異

以現有 ClassComparer 的 instructions、tryCatch、methodAnns、fieldAnns、innerClasses、records 等邏輯輸出，未修改驗收工具；方法按 JarIndex 的既有合成名稱正規化後排序，`-` 是本輪 staged、`+` 是候選。包含類別屬性、全部欄位與方法的差異。

### NativeSourcePoisonNpc

```diff
--- staged/NativeSourcePoisonNpc.txt
+++ candidate/NativeSourcePoisonNpc.txt
@@ -1831,14 +1831,6 @@
 CHECKCAST local/lito381/server/NativePlayerCommit$Binding
 INVOKESTATIC local/lito381/server/NativeSourcePoisonNpc.receiver(Llocal/lito381/server/NativeSourcePoisonNpc$Commit;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;)Z
 IRETURN
-METHOD lambda$nativeSourceStatusTransportTask$#0(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
-access=4106 signature=null exceptions=[java/io/IOException]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSelfOriginalSend([BLjava/lang/String;)V
-RETURN
 METHOD lambda$output$#0(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourcePoisonNpc$Output;Ljava/lang/String;)V
 access=4106 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -1871,18 +1863,6 @@
 ATHROW
 LABEL L5
 RETURN
-METHOD lambda$recipients$#0(Llocal/lito381/server/NativeSourcePoisonNpc$Commit;Llocal/lito381/server/ClientSession;)Z
-access=4106 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-ALOAD 0
-GETFIELD local/lito381/server/NativeSourcePoisonNpc$Commit.viewers:Ljava/util/Map;
-ALOAD 1
-INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativePlayerCommit$Binding
-INVOKESTATIC local/lito381/server/NativeSourcePoisonNpc.receiver(Llocal/lito381/server/NativeSourcePoisonNpc$Commit;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;)Z
-IRETURN
 METHOD lambda$status$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourcePoisonNpc$Status;Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
 access=4106 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -2068,7 +2048,7 @@
 ALOAD 2
 ALOAD 3
 ALOAD 4
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourcePoisonNpc.lambda$nativeSourceStatusTransportTask$#0(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourcePoisonNpc.lambda$status$#1(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeAreaMaterialAction.owned(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 LABEL L2
 ALOAD 6
@@ -2737,7 +2717,7 @@
 GETFIELD local/lito381/server/NativeSourcePoisonNpc$Commit.recipients:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 4
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeSourcePoisonNpc$Commit;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourcePoisonNpc.lambda$recipients$#0(Llocal/lito381/server/NativeSourcePoisonNpc$Commit;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeSourcePoisonNpc$Commit;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourcePoisonNpc.lambda$members$#0(Llocal/lito381/server/NativeSourcePoisonNpc$Commit;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
 GOTO L1
```

### NativeSourceProcScalarSkills

```diff
--- staged/NativeSourceProcScalarSkills.txt
+++ candidate/NativeSourceProcScalarSkills.txt
@@ -308,12 +308,11 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ILOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceProcScalarExtras.expiry(I)Ljava/lang/String;
-ASTORE 1
-ALOAD 1
-IFNULL L0
-ALOAD 1
+DUP
+IFNULL L0
 ARETURN
 LABEL L0
+POP
 ILOAD 0
 LOOKUPSWITCH default=L5 keys=31,32,71,78 labels=L1,L2,L3,L4
 LABEL L1
```

### NativeSourceRawDamage$Action

```diff
--- staged/NativeSourceRawDamage$Action.txt
+++ candidate/NativeSourceRawDamage$Action.txt
@@ -228,7 +228,7 @@
 GETFIELD local/lito381/server/NativeSourceRawDamage$Action.members:Ljava/util/IdentityHashMap;
 ALOAD 5
 ALOAD 0
-INVOKEDYNAMIC apply(Llocal/lito381/server/NativeSourceRawDamage$Action;)Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H5 local/lito381/server/NativeSourceRawDamage$Action.lambda$new$#0(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSourceRawDamage$Member;, type:(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSourceRawDamage$Member;]
+INVOKEDYNAMIC apply(Llocal/lito381/server/NativeSourceRawDamage$Action;)Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H7 local/lito381/server/NativeSourceRawDamage$Action.lambda$new$#0(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSourceRawDamage$Member;, type:(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSourceRawDamage$Member;]
 INVOKEVIRTUAL java/util/IdentityHashMap.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;
 POP
 LABEL L11
```

### NativeSourceRawDamage$Member

```diff
--- staged/NativeSourceRawDamage$Member.txt
+++ candidate/NativeSourceRawDamage$Member.txt
@@ -56,9 +56,6 @@
 LDC str:"positionY"
 INVOKESTATIC local/lito381/server/NativeSourceRawDamage.number(Ljava/lang/Object;Ljava/lang/String;)I
 PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.y:I
-ALOAD 0
-ACONST_NULL
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.nativeSourceNpcHpObserver:Ljava/lang/Boolean;
 RETURN
 METHOD <init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;II)V
 access=0 signature=null exceptions=[]
@@ -83,33 +80,17 @@
 ALOAD 0
 ILOAD 5
 PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.y:I
-ALOAD 0
-ACONST_NULL
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.nativeSourceNpcHpObserver:Ljava/lang/Boolean;
 RETURN
 METHOD <init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILjava/lang/Boolean;)V
 access=0 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
-INVOKESPECIAL java/lang/Object.<init>()V
-ALOAD 0
 ALOAD 1
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.pc:Llocal/lito381/server/ClientSession;
-ALOAD 0
 ALOAD 2
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 0
-ALOAD 0
 ALOAD 3
-DUP_X1
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.acceptedLife:Ljava/lang/Object;
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.life:Ljava/lang/Object;
-ALOAD 0
 ILOAD 4
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.x:I
-ALOAD 0
 ILOAD 5
-PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.y:I
+INVOKESPECIAL local/lito381/server/NativeSourceRawDamage$Member.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;II)V
 ALOAD 0
 ALOAD 6
 PUTFIELD local/lito381/server/NativeSourceRawDamage$Member.nativeSourceNpcHpObserver:Ljava/lang/Boolean;
```

### NativeSourceSignedRecovery

```diff
--- staged/NativeSourceSignedRecovery.txt
+++ candidate/NativeSourceSignedRecovery.txt
@@ -324,7 +324,7 @@
 ALOAD 0
 ALOAD 1
 ALOAD 3
-ALOAD 3
+DUP
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
 INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
 GOTO L7
```

### NativeSourceTargetIconBatch

```diff
--- staged/NativeSourceTargetIconBatch.txt
+++ candidate/NativeSourceTargetIconBatch.txt
@@ -9,7 +9,7 @@
 CLASS nestMembers [local/lito381/server/NativeSourceTargetIconBatch$Work]
 CLASS permittedSubclasses null
 CLASS annotations |||
-CLASS innerClasses local/lito381/server/NativeDeItemApplication$Owner|local/lito381/server/NativeDeItemApplication|Owner|0x18; local/lito381/server/NativeSelfCastAction$Task|local/lito381/server/NativeSelfCastAction|Task|0x608; local/lito381/server/NativeSourceAcceptedPlayers$Participant|local/lito381/server/NativeSourceAcceptedPlayers|Participant|0x18; local/lito381/server/NativeSourceProc68Icon$Output|local/lito381/server/NativeSourceProc68Icon|Output|0x18; local/lito381/server/NativeSourceProc68Icon$Proof|local/lito381/server/NativeSourceProc68Icon|Proof|0x18; local/lito381/server/NativeSourceTargetIconBatch$Work|local/lito381/server/NativeSourceTargetIconBatch|Work|0x18
+CLASS innerClasses local/lito381/server/NativeDeItemApplication$Owner|local/lito381/server/NativeDeItemApplication|Owner|0x18; local/lito381/server/NativeSelfCastAction$Task|local/lito381/server/NativeSelfCastAction|Task|0x608; local/lito381/server/NativeSourceProc68Icon$Output|local/lito381/server/NativeSourceProc68Icon|Output|0x18; local/lito381/server/NativeSourceProc68Icon$Proof|local/lito381/server/NativeSourceProc68Icon|Proof|0x18; local/lito381/server/NativeSourceTargetIconBatch$Work|local/lito381/server/NativeSourceTargetIconBatch|Work|0x18
 CLASS records
 METHOD <init>()V
 access=2 signature=null exceptions=[]
```

### NativeSourceTargetIconBatch$Work

```diff
--- staged/NativeSourceTargetIconBatch$Work.txt
+++ candidate/NativeSourceTargetIconBatch$Work.txt
@@ -60,29 +60,11 @@
 INVOKEVIRTUAL local/lito381/server/NativeSourceProc68Icon$Proof.target()Llocal/lito381/server/NativeSourceAcceptedPlayers$Participant;
 INVOKEVIRTUAL local/lito381/server/NativeSourceAcceptedPlayers$Participant.pc()Llocal/lito381/server/ClientSession;
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeSelfCastAction$Task;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceTargetIconBatch$Work.lambda$new$#1(Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeSelfCastAction$Task;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceTargetIconBatch$Work.lambda$new$#0(Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeRolePublication.nativeSourceTargetIconCompletion(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)Ljava/lang/Runnable;
 PUTFIELD local/lito381/server/NativeSourceTargetIconBatch$Work.publication:Ljava/lang/Runnable;
 RETURN
 METHOD lambda$new$#0(Llocal/lito381/server/NativeSelfCastAction$Task;)V
-access=4106 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L0 L1 L2 java/io/IOException
-LABEL L0
-ALOAD 0
-INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
-LABEL L1
-GOTO L3
-LABEL L2
-ASTORE 1
-NEW java/io/UncheckedIOException
-DUP
-ALOAD 1
-INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
-ATHROW
-LABEL L3
-RETURN
-METHOD lambda$new$#1(Llocal/lito381/server/NativeSelfCastAction$Task;)V
 access=4106 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 TRY L0 L1 L2 java/io/IOException
```

### NativeSourceWindTargets

```diff
--- staged/NativeSourceWindTargets.txt
+++ candidate/NativeSourceWindTargets.txt
@@ -883,8 +883,6 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$AttackSpellRequest.skillId()I
 INVOKESTATIC local/lito381/server/NativeSourceNoneTargetRequest.request(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;I)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ASTORE 4
-ALOAD 4
-ASTORE 6
 ALOAD 5
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.phase:Ljava/lang/String;
 LDC str:"WORLD"
@@ -904,24 +902,24 @@
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeAreaMaterialAction$Npc
-ASTORE 7
+ASTORE 6
 ALOAD 5
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 5
-ALOAD 6
+ALOAD 4
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/Protocol$AttackSpellRequest;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceWindTargets.lambda$run$#2(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.findFirst()Ljava/util/Optional; itf
 ACONST_NULL
 INVOKEVIRTUAL java/util/Optional.orElse(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 8
-ALOAD 8
+ASTORE 7
+ALOAD 7
 IFNONNULL L5
-ALOAD 7
+ALOAD 6
 IFNULL L4
-ALOAD 7
+ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.kind()I
 ICONST_3
 IF_ICMPEQ L5
@@ -933,38 +931,38 @@
 INVOKESTATIC local/lito381/server/NativeSourceWindTargets.original(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V
 RETURN
 LABEL L5
-ALOAD 8
+ALOAD 7
 IFNULL L6
 ACONST_NULL
-ASTORE 7
+ASTORE 6
 LABEL L6
 NEW local/lito381/server/NativeSourceWindTargets$Aim
 DUP
 ALOAD 5
-ALOAD 7
-ALOAD 8
+ALOAD 6
+ALOAD 7
 INVOKESPECIAL local/lito381/server/NativeSourceWindTargets$Aim.<init>(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;)V
-ASTORE 9
+ASTORE 8
 NEW local/lito381/server/NativeCompanionTargets$Scope
 DUP
 ALOAD 5
 INVOKESPECIAL local/lito381/server/NativeCompanionTargets$Scope.<init>(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-ASTORE 10
+ASTORE 9
 LABEL L7
-ALOAD 10
+ALOAD 9
 GETFIELD local/lito381/server/NativeCompanionTargets$Scope.valid:Z
 IFNE L9
 LABEL L8
-ALOAD 10
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeCompanionTargets$Scope.close()V
 RETURN
 LABEL L9
 ALOAD 5
 LDC str:"nativeCompanionTargetScope"
-ALOAD 10
+ALOAD 9
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 5
-ALOAD 9
+ALOAD 8
 PUTFIELD local/lito381/server/NativeAreaMaterialAction$Context.nativeSourceWindAim:Ljava/lang/Object;
 ALOAD 5
 LDC str:"nativeWeaponBreakTarget"
@@ -974,16 +972,16 @@
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeAreaMaterialAction$Context
-ASTORE 11
+ASTORE 10
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
 ALOAD 5
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 LABEL L10
 ALOAD 0
 SIPUSH 167
+ALOAD 8
+ALOAD 5
 ALOAD 9
-ALOAD 5
-ALOAD 10
 ALOAD 0
 ALOAD 1
 ALOAD 2
@@ -992,20 +990,20 @@
 ALOAD 5
 INVOKESTATIC local/lito381/server/NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
 LABEL L11
-ALOAD 11
+ALOAD 10
 IFNONNULL L12
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
 GOTO L13
 LABEL L12
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
-ALOAD 11
+ALOAD 10
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 LABEL L13
-ALOAD 9
+ALOAD 8
 GETFIELD local/lito381/server/NativeSourceWindTargets$Aim.registered:Z
 IFNE L14
-ALOAD 9
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeSourceWindTargets$Aim.close()V
 LABEL L14
 ALOAD 5
@@ -1014,48 +1012,48 @@
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 GOTO L20
 LABEL L15
-ASTORE 12
+ASTORE 11
 LABEL L16
-ALOAD 11
+ALOAD 10
 IFNONNULL L17
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
 GOTO L18
 LABEL L17
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
-ALOAD 11
+ALOAD 10
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 LABEL L18
-ALOAD 9
+ALOAD 8
 GETFIELD local/lito381/server/NativeSourceWindTargets$Aim.registered:Z
 IFNE L19
-ALOAD 9
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeSourceWindTargets$Aim.close()V
 LABEL L19
 ALOAD 5
 LDC str:"nativeCompanionTargetScope"
 ACONST_NULL
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-ALOAD 12
+ALOAD 11
 ATHROW
 LABEL L20
-ALOAD 10
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeCompanionTargets$Scope.close()V
 GOTO L26
 LABEL L21
-ASTORE 11
+ASTORE 10
 LABEL L22
-ALOAD 10
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeCompanionTargets$Scope.close()V
 LABEL L23
 GOTO L25
 LABEL L24
-ASTORE 12
+ASTORE 11
+ALOAD 10
 ALOAD 11
-ALOAD 12
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
 LABEL L25
-ALOAD 11
+ALOAD 10
 ATHROW
 LABEL L26
 RETURN
```

### NativeStaticNpcMotion

```diff
--- staged/NativeStaticNpcMotion.txt
+++ candidate/NativeStaticNpcMotion.txt
@@ -1511,6 +1511,46 @@
 ATHROW
 LABEL L15
 RETURN
+METHOD lambda$dialog$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;J)V
+access=4106 signature=null exceptions=[java/io/IOException]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+TRY L0 L2 L3
+TRY L3 L4 L3
+ALOAD 0
+DUP
+ASTORE 6
+MONITORENTER
+LABEL L0
+ALOAD 1
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.alive(ZLlocal/lito381/server/GameHub;)Z
+IFEQ L1
+ALOAD 3
+IFNULL L1
+ALOAD 0
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.location(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClientSession;)Ljava/lang/Object;
+ALOAD 3
+IF_ACMPNE L1
+ALOAD 2
+LLOAD 4
+INVOKESTATIC local/lito381/server/NativeStaticCancellationReplacement.dialog(Llocal/lito381/server/ClientSession;J)V
+LABEL L1
+ALOAD 6
+MONITOREXIT
+LABEL L2
+GOTO L5
+LABEL L3
+ASTORE 7
+ALOAD 6
+MONITOREXIT
+LABEL L4
+ALOAD 7
+ATHROW
+LABEL L5
+RETURN
 METHOD lambda$inScreen$#0(IILlocal/lito381/server/ClientSession;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z
 access=4106 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -1565,54 +1605,6 @@
 MONITOREXIT
 LABEL L4
 ALOAD 10
-ATHROW
-LABEL L5
-RETURN
-METHOD lambda$nativeStaticReplacementOriginalTalk$#0(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/NativeStaticNpcScene$Key;)Llocal/lito381/server/NativeStaticNpcMotion$Motion;
-access=4106 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-NEW local/lito381/server/NativeStaticNpcMotion$Motion
-DUP
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeStaticNpcMotion$Motion.<init>(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V
-ARETURN
-METHOD lambda$nativeStaticRespawnOriginalDialog$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;J)V
-access=4106 signature=null exceptions=[java/io/IOException]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L0 L2 L3
-TRY L3 L4 L3
-ALOAD 0
-DUP
-ASTORE 6
-MONITORENTER
-LABEL L0
-ALOAD 1
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.alive(ZLlocal/lito381/server/GameHub;)Z
-IFEQ L1
-ALOAD 3
-IFNULL L1
-ALOAD 0
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.location(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClientSession;)Ljava/lang/Object;
-ALOAD 3
-IF_ACMPNE L1
-ALOAD 2
-LLOAD 4
-INVOKESTATIC local/lito381/server/NativeStaticCancellationReplacement.dialog(Llocal/lito381/server/ClientSession;J)V
-LABEL L1
-ALOAD 6
-MONITOREXIT
-LABEL L2
-GOTO L5
-LABEL L3
-ASTORE 7
-ALOAD 6
-MONITOREXIT
-LABEL L4
-ALOAD 7
 ATHROW
 LABEL L5
 RETURN
@@ -1649,6 +1641,14 @@
 INVOKEVIRTUAL local/lito381/server/GameHub.classicWorldMembers(II)Ljava/util/List;
 INVOKEINTERFACE java/util/List.isEmpty()Z itf
 IRETURN
+METHOD lambda$talk$#0(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/NativeStaticNpcScene$Key;)Llocal/lito381/server/NativeStaticNpcMotion$Motion;
+access=4106 signature=null exceptions=[]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+NEW local/lito381/server/NativeStaticNpcMotion$Motion
+DUP
+ALOAD 0
+INVOKESPECIAL local/lito381/server/NativeStaticNpcMotion$Motion.<init>(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V
+ARETURN
 METHOD lambda$tick$#0(Ljava/util/LinkedHashMap;Llocal/lito381/server/NativeStaticNpcScene$Key;)Z
 access=4106 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -2244,7 +2244,7 @@
 GETFIELD local/lito381/server/NativeStaticNpcMotion$State.actors:Ljava/util/Map;
 ALOAD 7
 ALOAD 1
-INVOKEDYNAMIC apply(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeStaticNpcMotion.lambda$nativeStaticReplacementOriginalTalk$#0(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/NativeStaticNpcScene$Key;)Llocal/lito381/server/NativeStaticNpcMotion$Motion;, type:(Llocal/lito381/server/NativeStaticNpcScene$Key;)Llocal/lito381/server/NativeStaticNpcMotion$Motion;]
+INVOKEDYNAMIC apply(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeStaticNpcMotion.lambda$talk$#0(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/NativeStaticNpcScene$Key;)Llocal/lito381/server/NativeStaticNpcMotion$Motion;, type:(Llocal/lito381/server/NativeStaticNpcScene$Key;)Llocal/lito381/server/NativeStaticNpcMotion$Motion;]
 INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeStaticNpcMotion$Motion
 ASTORE 8
@@ -2457,7 +2457,7 @@
 ALOAD 0
 ALOAD 6
 LLOAD 1
-INVOKEDYNAMIC run(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;J)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeStaticNpcMotion.lambda$nativeStaticRespawnOriginalDialog$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;J)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;J)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeStaticNpcMotion.lambda$dialog$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;J)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeLightAction.commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
 METHOD occupies(IIILlocal/lito381/server/ClassicWorldMonsterPopulation;)Z
```

### NativeStaticNpcScene

```diff
--- staged/NativeStaticNpcScene.txt
+++ candidate/NativeStaticNpcScene.txt
@@ -217,19 +217,19 @@
 LABEL L0
 ALOAD 0
 DUP
+ASTORE 3
+MONITORENTER
+LABEL L1
+ALOAD 0
+ALOAD 1
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.instance(Llocal/lito381/server/ClientSession;)I
+ICONST_0
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.actor(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IZ)Llocal/lito381/server/NativeStaticNpcScene$Actor;
 ASTORE 4
-MONITORENTER
-LABEL L1
-ALOAD 0
-ALOAD 1
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.instance(Llocal/lito381/server/ClientSession;)I
-ICONST_0
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.actor(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IZ)Llocal/lito381/server/NativeStaticNpcScene$Actor;
-ASTORE 5
-ALOAD 5
+ALOAD 4
 IFNULL L2
-ALOAD 5
+ALOAD 4
 GETFIELD local/lito381/server/NativeStaticNpcScene$Actor.heading:Ljava/lang/Integer;
 IFNONNULL L3
 LABEL L2
@@ -237,29 +237,25 @@
 INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.heading()I
 GOTO L4
 LABEL L3
-ALOAD 5
+ALOAD 4
 GETFIELD local/lito381/server/NativeStaticNpcScene$Actor.heading:Ljava/lang/Integer;
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 LABEL L4
-ISTORE 3
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L5
-GOTO L8
+ALOAD 0
+ALOAD 1
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.heading(ILlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
+IRETURN
 LABEL L6
-ASTORE 6
-ALOAD 4
+ASTORE 5
+ALOAD 3
 MONITOREXIT
 LABEL L7
-ALOAD 6
-ATHROW
-LABEL L8
-ILOAD 3
-ALOAD 0
-ALOAD 1
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.heading(ILlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
-IRETURN
+ALOAD 5
+ATHROW
 METHOD inScene(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;II)Z
 access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -318,7 +314,7 @@
 DUP
 INVOKESPECIAL local/lito381/server/NativeStaticNpcScene$Actor.<init>()V
 ARETURN
-METHOD lambda$nativeStaticRespawnOriginalPrune$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Key;)Z
+METHOD lambda$prune$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Key;)Z
 access=4106 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
@@ -420,7 +416,7 @@
 GETFIELD local/lito381/server/NativeStaticNpcScene$State.actors:Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.keySet()Ljava/util/Set; itf
 ALOAD 0
-INVOKEDYNAMIC test(Llocal/lito381/server/GameHub;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeStaticNpcScene.lambda$nativeStaticRespawnOriginalPrune$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Key;)Z, type:(Llocal/lito381/server/NativeStaticNpcScene$Key;)Z]
+INVOKEDYNAMIC test(Llocal/lito381/server/GameHub;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeStaticNpcScene.lambda$prune$#0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Key;)Z, type:(Llocal/lito381/server/NativeStaticNpcScene$Key;)Z]
 INVOKEINTERFACE java/util/Set.removeIf(Ljava/util/function/Predicate;)Z itf
 POP
 ALOAD 2
@@ -455,7 +451,8 @@
 METHOD restore(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z
 access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L1 L6 L7
+TRY L1 L3 L7
+TRY L4 L6 L7
 TRY L7 L8 L7
 ALOAD 1
 ALOAD 3
@@ -465,15 +462,13 @@
 ALOAD 1
 ILOAD 2
 INVOKEVIRTUAL local/lito381/server/GameHub.restoreClassicWorldNpcGfx(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;I)Z
-ISTORE 4
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 4
-IRETURN
-LABEL L0
-ALOAD 0
-DUP
-ASTORE 5
+IRETURN
+LABEL L0
+ALOAD 0
+DUP
+ASTORE 4
 MONITORENTER
 LABEL L1
 ALOAD 1
@@ -497,53 +492,52 @@
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.gfx(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
 ILOAD 2
-IF_ICMPEQ L3
-LABEL L2
-ICONST_0
-ISTORE 4
-GOTO L5
-LABEL L3
+IF_ICMPEQ L4
+LABEL L2
+ICONST_0
+ALOAD 4
+MONITOREXIT
+LABEL L3
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
+LABEL L4
 ALOAD 0
 ALOAD 1
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.instance(Llocal/lito381/server/ClientSession;)I
 ICONST_0
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.actor(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IZ)Llocal/lito381/server/NativeStaticNpcScene$Actor;
-ASTORE 6
-ALOAD 6
-IFNULL L4
-ALOAD 6
+ASTORE 5
+ALOAD 5
+IFNULL L5
+ALOAD 5
 ACONST_NULL
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.gfx:Ljava/lang/Integer;
-ALOAD 6
+ALOAD 5
 ACONST_NULL
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.appearance:Llocal/lito381/server/NativeCancellationNpcStatic$Appearance;
 ALOAD 0
 ALOAD 1
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.instance(Llocal/lito381/server/ClientSession;)I
+ALOAD 5
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.trim(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/NativeStaticNpcScene$Actor;)V
+LABEL L5
+ICONST_1
+ALOAD 4
+MONITOREXIT
+LABEL L6
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
+LABEL L7
+ASTORE 6
+ALOAD 4
+MONITOREXIT
+LABEL L8
 ALOAD 6
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.trim(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/NativeStaticNpcScene$Actor;)V
-LABEL L4
-ICONST_1
-ISTORE 4
-LABEL L5
-ALOAD 5
-MONITOREXIT
-LABEL L6
-GOTO L9
-LABEL L7
-ASTORE 7
-ALOAD 5
-MONITOREXIT
-LABEL L8
-ALOAD 7
-ATHROW
-LABEL L9
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 4
-IRETURN
+ATHROW
 METHOD scoped(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)Z
 access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -598,7 +592,8 @@
 METHOD transform(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IILlocal/lito381/server/ClientSession;)Z
 access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L1 L5 L6
+TRY L1 L3 L6
+TRY L4 L5 L6
 TRY L6 L7 L6
 ALOAD 1
 ALOAD 4
@@ -609,15 +604,13 @@
 ILOAD 2
 ILOAD 3
 INVOKEVIRTUAL local/lito381/server/GameHub.transformClassicWorldNpcGfx(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;II)Z
-ISTORE 5
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 5
-IRETURN
-LABEL L0
-ALOAD 0
-DUP
-ASTORE 6
+IRETURN
+LABEL L0
+ALOAD 0
+DUP
+ASTORE 5
 MONITORENTER
 LABEL L1
 ALOAD 1
@@ -644,12 +637,16 @@
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.gfx(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
 ILOAD 2
-IF_ICMPEQ L3
-LABEL L2
-ICONST_0
-ISTORE 5
-GOTO L4
-LABEL L3
+IF_ICMPEQ L4
+LABEL L2
+ICONST_0
+ALOAD 5
+MONITOREXIT
+LABEL L3
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
+LABEL L4
 ALOAD 0
 ALOAD 1
 ALOAD 4
@@ -660,24 +657,19 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.gfx:Ljava/lang/Integer;
 ICONST_1
-ISTORE 5
-LABEL L4
+ALOAD 5
+MONITOREXIT
+LABEL L5
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
+LABEL L6
+ASTORE 6
+ALOAD 5
+MONITOREXIT
+LABEL L7
 ALOAD 6
-MONITOREXIT
-LABEL L5
-GOTO L8
-LABEL L6
-ASTORE 7
-ALOAD 6
-MONITOREXIT
-LABEL L7
-ALOAD 7
-ATHROW
-LABEL L8
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 5
-IRETURN
+ATHROW
 METHOD trim(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/NativeStaticNpcScene$Actor;)V
 access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -776,10 +768,16 @@
 ILOAD 2
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.heading:Ljava/lang/Integer;
-ALOAD 4
-MONITOREXIT
-LABEL L4
-GOTO L7
+ICONST_1
+ALOAD 4
+MONITOREXIT
+LABEL L4
+ALOAD 0
+ALOAD 1
+ILOAD 2
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.turned(ZLlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z
+IRETURN
 LABEL L5
 ASTORE 5
 ALOAD 4
@@ -787,11 +785,3 @@
 LABEL L6
 ALOAD 5
 ATHROW
-LABEL L7
-ICONST_1
-ALOAD 0
-ALOAD 1
-ILOAD 2
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.turned(ZLlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z
-IRETURN
```
