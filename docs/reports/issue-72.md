# Issue #72 — T020-A01 正式 0.6.71 暫存

## 結論

2026-11-05 本機重新驗證：49 份 staged Java 可編譯，產出恰好 185 個 A01 entry，**137 SAME／48 DIFF／0 ONLY_LEFT／0 ONLY_RIGHT**，SAME_EFFECTS=0、BYTE_SAME=18。**未達 185 SAME，不可進 A-land，不可部署。** 已依任務要求在 Issue #72 加上 `needs:claude`；本報告提供每個待裁定 entry 的完整正規化差異，不以 CI 綠或編譯成功冒充完成。

分支沿用 `task/72`，起點 `33f3f5c`。為保存已有遠端工作，將 `origin/task/72` 的 `94d28539c3c7b4b339d8ffd2367283a690b30095` 以 `git merge --no-commit --no-ff` 合入，沒有 checkout／重建分支，也不需要 force push。本報告取代前次報告；123 SAME／62 DIFF 是本機重現的修正前基準。

## 做了什麼

- 讀取根目錄與伺服器 AGENTS、docs/REPORT.md、設計 §7、A01 清單、#108 報告及 #72 主控／退回留言。49 類全為 DECOMPILED，均保留指定標頭。
- 唯讀重新反編譯固定正式 JAR：CFR 0.152 及 Vineflower 1.11.2。保留已通過部分，修復泛型、捕捉名稱、冗餘區域槽、同步鎖、finally 返回與分支方向；以完整差異選擇較接近正式版的來源。
- **修正真正行為錯誤**：舊 staged 的 `NativeDeAiTimeline.nextTimer` 每次執行兩次遞增；正式版先 `++timerSequence`，只有結果為 0 時再遞增。修正後整個 entry SAME。
- AI timer／worker、chat scope／jobs、deletion scope／owner、definitions scope 等完整 entry 已對齊。`NativeDeItemEquipProc` 使用 #108 可重現的編譯期依賴，重驗 SAME。
- `NativeDeInstanceState` 改用正式版新 CFR 來源，移除前次沿用的 src/main wrapper 寫法；但正式建構子的 pre-super 賦值無法在 Java 17 直接表達，已在來源與下文標明仍有行為風險。`NativeDeDeletionState` 的縮小存取權限也仍需裁定。
- 只提交 A01 49 份 `.java` 與本報告。`src/main`、`pom.xml`、`docs/REPORT.md`、固定 JAR 未修改。沒有 javac 後 ASM、沒有覆蓋正式 class，也沒有 effects／allow／exclude 豁免。

## 已驗證

環境：Windows、Microsoft JDK `17.0.17`、Python 3.12。一般 exec 沙箱初始化失敗，改用已核准的提升執行；寫入仍限本 worktree，原專案只讀。未啟動伺服器／登入器／遊戲，未連 DB，未讀機密設定、封存或客戶端。

| 檔案／內容 | SHA-256 |
|---|---|
| 正式 `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 驗收 `scratch/issue-72/target/a01.jar` | `7a6d259928fb53e00a8c826d3d65ef9bdba11753008a242892cb291668556b86` |
| A01 verify_entries（UTF-8、LF） | `8c883895819b752ae9c9b70b2cef771ce7f44bc23cacfc25c19c197109c79499` |
| 49 份來源 SHA 清單（下方重現程式可產生） | `0d34a6343d76562bb9fcdc20901e94380c48e91141b97c26925d7ef67e971aae` |
| CFR 0.152 | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |
| Vineflower 1.11.2 | `e1e2415e7f78b34960402c4beddfc88e033d7842a23ecd132a8ec2eadd54f6bf` |

來源不依賴原專案私有 stub；使用已合併 #108 的 `compile-stubs` 9 份 Java，編成 `target/legacy-stubs`，classpath 為正式 JAR、stub classes、`lib/*`。stub 的 16 個 class 只作編譯依賴，**驗收 JAR 內沒有 com/lineage 或任何 A01 清單外 class**。

實際命令結果：stub build、semantic 工具 javac、A01 javac、打包及 semantic-diff 均 exit 0；semantic-diff exit 0 只表示成功產生報告，不表示無 DIFF。

```text
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
javac: Note: Some input files use unchecked or unsafe operations.
entries 185 missing [] extra []
SUMMARY SAME=137 SAME_EFFECTS=0 DIFF=48 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=18 ALLOW_METHODS=0
classSelection.missingLeft=[]
classSelection.extraLeft=[]
```

## 可直接重現的編譯／打包／比對

在 repo 根目錄先執行：

```powershell
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
New-Item -ItemType Directory -Force scratch/issue-72 | Out-Null
& ./lito伺服器/LiTo381-Server/tools/legacy-stubs/build.ps1
if ($LASTEXITCODE -ne 0) { throw 'stub build failed' }
```

將下段存為 `scratch/issue-72/reproduce.py`，執行 `py -3 -B -X utf8 scratch/issue-72/reproduce.py`。此段直接由 JSON 取得 A01，不用佔位清單；ASM 9.7 使用本機 Maven cache，可由工具既有 build 入口取得相同版本。固定 JAR 與來源均從目前提交讀取，不需重新下載反編譯器才能驗收。

```python
from pathlib import Path
import subprocess, json, zipfile, hashlib
root = Path(__file__).resolve().parents[2]
s = root / 'lito伺服器/LiTo381-Server'
w = root / 'scratch/issue-72'
j = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
b = next(x for x in json.loads((root/'docs/combat-merge/a-batches.json').read_text(encoding='utf-8'))['batches'] if x['id']=='A01')
st = s/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
asm = ';'.join(str(Path('D:/dev-cache/maven/repository/org/ow2/asm')/n/'9.7'/f'{n}-9.7.jar') for n in ['asm','asm-tree'])
tc = s/'tools/semantic-diff/target/classes'
def run(args): subprocess.run([str(x) for x in args], cwd=root, check=True)
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',asm,'-d',tc,*sorted((s/'tools/semantic-diff/src/main/java').rglob('*.java'))])
out = w/'target/reproduce'; out.mkdir(parents=True, exist_ok=True)
assert out.resolve().is_relative_to(w.resolve())
for p in out.rglob('*.class'): p.unlink()
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',f'{s}/reference/base-0.6.71.jar;{s}/target/legacy-stubs;{s}/lib/*','-d',out,*[st/(n+'.java') for n in b['classes']]])
jar = w/'target/reproduce.jar'
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(out.rglob('*.class')):
        info = zipfile.ZipInfo(p.relative_to(out).as_posix(), (2026,1,1,0,0,0))
        z.writestr(info,p.read_bytes())
with zipfile.ZipFile(jar) as z: actual = set(z.namelist())
expected = set(b['verify_entries'])
assert actual == expected, (expected-actual,actual-expected)
assert not any(n.startswith('com/lineage/') for n in actual)
(w/'verify_entries.txt').write_bytes(('\n'.join(b['verify_entries'])+'\n').encode())
manifest = ''.join(hashlib.sha256((st/(n+'.java')).read_bytes()).hexdigest()+'  '+n+'.java\n' for n in b['classes'])
(w/'source-sha256.txt').write_bytes(manifest.encode())
run([j/'java.exe','-cp',f'{tc};{asm}','local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',s/'reference/base-0.6.71.jar','--classes',w/'verify_entries.txt','--json',w/'reproduce.json','--md',w/'reproduce.md'])
r = json.loads((w/'reproduce.json').read_text(encoding='utf-8'))
assert r['classSelection'] == {'missingLeft':[], 'extraLeft':[]}
print(r['counts'])
print('JAR SHA256',hashlib.sha256(jar.read_bytes()).hexdigest())
print('SOURCE MANIFEST SHA256',hashlib.sha256(manifest.encode()).hexdigest())
# 這是現況再現斷言，並非 A01 完成驗收；正式閘門仍要求 DIFF=0、SAME=185。
assert r['counts']['SAME']==137 and r['counts']['DIFF']==48
```

## 待裁定範圍／未驗證

以下 48 個 entry，其中 **20 個僅 metadata／欄位結構差異、28 個仍含方法差異**。所有 DIFF 都維持原工具結果；沒有自行放寬驗收，也不把未解差異稱為語意等價。

| Entry（共同前綴 local/lito381/server/，副檔名 .class） | 有差異的方法數 | 分類 |
|---|---:|---|
| `NativeDeCachedFactoryServices` | 0 | 僅 metadata／欄位 |
| `NativeDeCharacterActor` | 0 | 僅 metadata／欄位 |
| `NativeDeCharacterFields$CombatSkillDerived` | 1 | 含方法差異 |
| `NativeDeCharacterFields$DamageMutationReceipt` | 2 | 含方法差異 |
| `NativeDeCharacterFields$ViewerHpState` | 0 | 僅 metadata／欄位 |
| `NativeDeCharacterFields` | 21 | 含方法差異 |
| `NativeDeChatBroadcast` | 1 | 含方法差異 |
| `NativeDeChatChat` | 0 | 僅 metadata／欄位 |
| `NativeDeChatChatGlobal` | 0 | 僅 metadata／欄位 |
| `NativeDeChatChatShouting` | 0 | 僅 metadata／欄位 |
| `NativeDeChatTable` | 1 | 含方法差異 |
| `NativeDeChatTimer$RunSequenceTask` | 0 | 僅 metadata／欄位 |
| `NativeDeChatTimer` | 0 | 僅 metadata／欄位 |
| `NativeDeChatWire` | 3 | 含方法差異 |
| `NativeDeClientViewer` | 5 | 含方法差異 |
| `NativeDeControlCatalog$Names` | 0 | 僅 metadata／欄位 |
| `NativeDeControlCatalog$Reporter` | 0 | 僅 metadata／欄位 |
| `NativeDeControlCatalog` | 2 | 含方法差異 |
| `NativeDeCorpseCore` | 1 | 含方法差異 |
| `NativeDeCorpseTimers$Job` | 1 | 含方法差異 |
| `NativeDeCorpseTimers$Owner$1` | 3 | 含方法差異 |
| `NativeDeCorpseTimers$Owner` | 2 | 含方法差異 |
| `NativeDeCorpseTimers$Ticket` | 1 | 含方法差異 |
| `NativeDeDeletionDeState` | 0 | 僅 metadata／欄位 |
| `NativeDeDeletionPorts$Services` | 0 | 僅 metadata／欄位 |
| `NativeDeDeletionState` | 5 | 含方法差異 |
| `NativeDeDeletionVisibility$1` | 1 | 含方法差異 |
| `NativeDeDeletionVisibility$Snapshot` | 1 | 含方法差異 |
| `NativeDeDeletionVisibility` | 1 | 含方法差異 |
| `NativeDeFactoryControls` | 0 | 僅 metadata／欄位 |
| `NativeDeInitializerKernel$Actor` | 0 | 僅 metadata／欄位 |
| `NativeDeInitializerKernel$Services` | 0 | 僅 metadata／欄位 |
| `NativeDeInitializerKernel` | 3 | 含方法差異 |
| `NativeDeInstanceBroadcast` | 0 | 僅 metadata／欄位 |
| `NativeDeInstanceName` | 0 | 僅 metadata／欄位 |
| `NativeDeInstanceNames` | 2 | 含方法差異 |
| `NativeDeInstanceScene` | 1 | 含方法差異 |
| `NativeDeInstanceScope$World` | 1 | 含方法差異 |
| `NativeDeInstanceState$DeAiWorkerTask` | 0 | 僅 metadata／欄位 |
| `NativeDeInstanceState` | 320 | 含方法差異 |
| `NativeDeItemAbility` | 0 | 僅 metadata／欄位 |
| `NativeDeItemApplication$Owner` | 3 | 含方法差異 |
| `NativeDeItemApplication` | 3 | 含方法差異 |
| `NativeDeItemBlessStatsTable` | 2 | 含方法差異 |
| `NativeDeItemCommonEnchantTable` | 2 | 含方法差異 |
| `NativeDeItemDefinitionsResources` | 3 | 含方法差異 |
| `NativeDeItemEnchantBonusTable` | 2 | 含方法差異 |
| `NativeDeItemEquipProcTable` | 0 | 僅 metadata／欄位 |

- **已驗證的表達障礙**：NativeDeInstanceState 的 pre-super 實例欄位寫入／前置 name 呼叫、NativeDeDeletionState 的縮小存取、DeletionVisibility switch map 的舊名稱。兩種反編譯器也都不能原樣編成 Java 17。相關原始 javac 訊息為 `call to super must be first statement in constructor`、`attempting to assign weaker access privileges; was public`。這些障礙不授權略過同類其他方法差異。
- **推測**：InnerClasses 缺列／旗標與 switch 欄位舊名來自歷史 ASM 搬遷；本輪沒有重播歷史鏈，不宣稱已證明歷史來源。其他槽位、正反分支、try 邊界、lambda 重建有些可能只屬編譯形狀，但尚未逐一證明行為等價。
- **待驗證**：剩餘 48 entry 的最終可接受表達與驗收；Java 17 建構子順序調整的實際影響；所有 staged 類的整合行為。單元測試／完整回歸依任務不適用，A-land 才執行。沒有將本輪產物移入 src/main 或正式部署。

## 產出路徑／給下一棒的提示

- 交付來源：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`，精確為 A01 49 檔；報告：`docs/reports/issue-72.md`。
- 本機重建證據（不入 git）：`scratch/issue-72/` 的 cfr、vf、vfclean、semantic.json、semantic.md、verify_entries.txt、javac.log、left/right 正規化全文及 full.diff；class/JAR 均在 target 子目錄。原始碼與完整必要差異已在此提交，不依賴審查者取得 ignored scratch。
- **Claude 主控接手**：Issue 已有 `needs:claude`。請先裁定三類 Java 表達障礙，再按下方完整 diff 指定續修或批准明確例外；不要把整個類別豁免，也不要因 firstDiff 只顯示 metadata 而漏掉後續方法差異。本批應保持未完成，不能直接放行 A-land。
- **後續本機執行器**：使用 #108 的 compile-stubs；保留已對齊的 137 entries。原工具預設比較必須保留，若主控授權工具擴充則另案處理；禁止私自做 javac 後 ASM 或搬正式 bytes。本機 JDK 17.0.17 的初始數量與前次報告所載 JDK 17.0.21 相同；本輪未另行執行 JDK 17.0.21，不將版本差異視為已證明的原因。
- 不修改 docs/REPORT.md；不用使用者啟動遊戲驗證這些尚未等價的暫存來源。

## 完整正規化差異（48 entries）

以下由**未修改的 ClassComparer** 與 JarIndex 讀取兩個驗收 JAR，在 scratch 診斷類中呼叫同一組正規化函式匯出全部比較欄位。沒有寫回 class、沒有把診斷結果當驗收豁免。逐類差異集合與 semantic.json 的 48 DIFF 完全一致；其餘 137 類完整 dump 一致。包含所有 class／field 屬性與所有方法 metadata、try-catch、指令；不是只列 firstDiff。

每段 `--- staged`／`+++ formal`，hunk 行號是該比較項正規化序列中的 1-based 行號，`method` 標題保留完整描述子；共同的 method 前綴移到標題以便閱讀，**沒有刪掉任何差異 hunk**。完全相同的方法不重複列出。內嵌長字串未截斷。

### `NativeDeCachedFactoryServices`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Abilities|local/lito381/server/NativeDeControlCatalog|Abilities|0x18; local/lito381/server/NativeDeControlCatalog$Catalog|local/lito381/server/NativeDeControlCatalog|Catalog|0x18; local/lito381/server/NativeDeControlCatalog$Definition|local/lito381/server/NativeDeControlCatalog|Definition|0x18; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeControlCatalog$Settings|local/lito381/server/NativeDeControlCatalog|Settings|0x18; local/lito381/server/NativeDeFactoryControls$Snapshot|local/lito381/server/NativeDeFactoryControls|Snapshot|0x18; local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Services|local/lito381/server/NativeDeInitializerKernel|Services|0x608; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18; local/lito381/server/NativeDeSpriteCatalog$Catalog|local/lito381/server/NativeDeSpriteCatalog|Catalog|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Abilities|local/lito381/server/NativeDeControlCatalog|Abilities|0x18; local/lito381/server/NativeDeControlCatalog$Catalog|local/lito381/server/NativeDeControlCatalog|Catalog|0x18; local/lito381/server/NativeDeControlCatalog$Definition|local/lito381/server/NativeDeControlCatalog|Definition|0x18; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeControlCatalog$Settings|local/lito381/server/NativeDeControlCatalog|Settings|0x18; local/lito381/server/NativeDeFactoryControls$Snapshot|local/lito381/server/NativeDeFactoryControls|Snapshot|0x18; local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Services|local/lito381/server/NativeDeInitializerKernel|Services|0x608; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18; local/lito381/server/NativeDeSpriteCatalog$Catalog|local/lito381/server/NativeDeSpriteCatalog|Catalog|0x18
 class recordComponents=
 field controls:Llocal/lito381/server/NativeDeFactoryControls$Snapshot; access=0x12
 field controls:Llocal/lito381/server/NativeDeFactoryControls$Snapshot; signature=null
```

### `NativeDeCharacterActor`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeCharacterFields$Snapshot|local/lito381/server/NativeDeCharacterFields|Snapshot|0x18; local/lito381/server/NativeDeCorpseTimers$Owner|local/lito381/server/NativeDeCorpseTimers|Owner|0x18; local/lito381/server/NativeDeInitializerKernel$Actor|local/lito381/server/NativeDeInitializerKernel|Actor|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem|local/lito381/server/NativeDeInitializerKernel|Emblem|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeInitializerKernel$Packet|local/lito381/server/NativeDeInitializerKernel|Packet|0x608
+class innerClasses=local/lito381/server/NativeDeCharacterFields$Snapshot|local/lito381/server/NativeDeCharacterFields|Snapshot|0x18; local/lito381/server/NativeDeCorpseTimers$Owner|local/lito381/server/NativeDeCorpseTimers|Owner|0x18; local/lito381/server/NativeDeInitializerKernel$Actor|local/lito381/server/NativeDeInitializerKernel|Actor|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem|local/lito381/server/NativeDeInitializerKernel|Emblem|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18; local/lito381/server/NativeDeInitializerKernel$Packet|local/lito381/server/NativeDeInitializerKernel|Packet|0x608
 class recordComponents=
 field fields:Llocal/lito381/server/NativeDeCharacterFields; access=0x12
 field fields:Llocal/lito381/server/NativeDeCharacterFields; signature=null
```

### `NativeDeCharacterFields$CombatSkillDerived`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;`

```diff
--- staged
+++ formal
@@ -21,88 +21,90 @@
 LABEL L0
 ALOAD 7
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L7
+IFEQ L8
 ALOAD 7
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Integer
 ASTORE 8
 ALOAD 8
-IFNULL L6
+IFNONNULL L1
+GOTO L0
+LABEL L1
 GETSTATIC local/lito381/server/NativeDeCharacterFields.SKU1:Ljava/util/HashMap;
 ALOAD 8
 INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 ASTORE 9
 ALOAD 9
-IFNULL L1
+IFNULL L2
 ILOAD 1
 ALOAD 9
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 IADD
 ISTORE 1
-LABEL L1
+LABEL L2
 GETSTATIC local/lito381/server/NativeDeCharacterFields.SKU2:Ljava/util/HashMap;
 ALOAD 8
 INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 ASTORE 10
 ALOAD 10
-IFNULL L2
+IFNULL L3
 ILOAD 2
 ALOAD 10
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 IADD
 ISTORE 2
-LABEL L2
+LABEL L3
 GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD1:Ljava/util/HashMap;
 ALOAD 8
 INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 ASTORE 11
 ALOAD 11
-IFNULL L3
+IFNULL L4
 ILOAD 3
 ALOAD 11
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 IADD
 ISTORE 3
-LABEL L3
+LABEL L4
 GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD2:Ljava/util/HashMap;
 ALOAD 8
 INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 ASTORE 12
 ALOAD 12
-IFNULL L4
+IFNULL L5
 ILOAD 4
 ALOAD 12
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 IADD
 ISTORE 4
-LABEL L4
+LABEL L5
 GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD3:Ljava/util/HashMap;
 ALOAD 8
 INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 ASTORE 13
 ALOAD 13
-IFNULL L6
+IFNULL L7
 ALOAD 13
 ALOAD 8
 INVOKEVIRTUAL java/lang/Integer.equals(Ljava/lang/Object;)Z
-IFEQ L5
+IFEQ L6
 CONST Integer:1
 ISTORE 6
-GOTO L6
-LABEL L5
+GOTO L7
+LABEL L6
 ILOAD 5
 ALOAD 13
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 IADD
 ISTORE 5
-LABEL L6
+LABEL L7
 GOTO L0
-LABEL L7
+LABEL L8
 NEW local/lito381/server/NativeDeCharacterFields$CombatSkillDerived
 DUP
 ILOAD 1
```

### `NativeDeCharacterFields$DamageMutationReceipt`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method recordDead(ZZI)V`

```diff
--- staged
+++ formal
@@ -5,7 +5,9 @@
 default=
 ILOAD 1
 ILOAD 2
-IF_ICMPEQ L0
+IF_ICMPNE L0
+RETURN
+LABEL L0
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._deathSeen:Z
@@ -14,12 +16,12 @@
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._deadAfter:Z
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpSeen:Z
-IFNE L0
+IFNE L1
 ALOAD 0
 ILOAD 3
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpBefore:I
 ALOAD 0
 ILOAD 3
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpAfter:I
-LABEL L0
+LABEL L1
 RETURN
```

#### `method recordHp(II)V`

```diff
--- staged
+++ formal
@@ -5,17 +5,19 @@
 default=
 ILOAD 1
 ILOAD 2
-IF_ICMPEQ L3
+IF_ICMPNE L0
+RETURN
+LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpSeen:Z
-IFNE L0
+IFNE L1
 ALOAD 0
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpBefore:I
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpSeen:Z
-LABEL L0
+LABEL L1
 ALOAD 0
 ILOAD 2
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpAfter:I
@@ -24,13 +26,12 @@
 GETFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpReduced:Z
 ILOAD 2
 ILOAD 1
-IF_ICMPGE L1
+IF_ICMPGE L2
 CONST Integer:1
-GOTO L2
-LABEL L1
+GOTO L3
+LABEL L2
 CONST Integer:0
-LABEL L2
+LABEL L3
 IOR
 PUTFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._hpReduced:Z
-LABEL L3
 RETURN
```

### `NativeDeCharacterFields$ViewerHpState`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeCharacterFields$ViewerHpState|local/lito381/server/NativeDeCharacterFields|ViewerHpState|0x1a; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+class innerClasses=local/lito381/server/NativeDeCharacterFields$ViewerHpState|local/lito381/server/NativeDeCharacterFields|ViewerHpState|0x1a
 class recordComponents=
 field lastHpRatio:Ljava/util/concurrent/atomic/AtomicInteger; access=0x12
 field lastHpRatio:Ljava/util/concurrent/atomic/AtomicInteger; signature=null
```

### `NativeDeCharacterFields`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=local/lito381/server/NativeDeCharacterFields$CombatSkillDerived,local/lito381/server/NativeDeCharacterFields$CurrentHpMutation,local/lito381/server/NativeDeCharacterFields$CurrentMpMutation,local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt,local/lito381/server/NativeDeCharacterFields$ErrorLog,local/lito381/server/NativeDeCharacterFields$MpDebitMutation,local/lito381/server/NativeDeCharacterFields$SkillEffectSnapshot,local/lito381/server/NativeDeCharacterFields$Snapshot,local/lito381/server/NativeDeCharacterFields$ViewerHpState
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/Map$Entry|java/util/Map|Entry|0x609; local/lito381/server/NativeDeCharacterFields$CombatSkillDerived|local/lito381/server/NativeDeCharacterFields|CombatSkillDerived|0x19; local/lito381/server/NativeDeCharacterFields$CurrentHpMutation|local/lito381/server/NativeDeCharacterFields|CurrentHpMutation|0xc; local/lito381/server/NativeDeCharacterFields$CurrentMpMutation|local/lito381/server/NativeDeCharacterFields|CurrentMpMutation|0xc; local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt|local/lito381/server/NativeDeCharacterFields|DamageMutationReceipt|0x1c; local/lito381/server/NativeDeCharacterFields$ErrorLog|local/lito381/server/NativeDeCharacterFields|ErrorLog|0x1a; local/lito381/server/NativeDeCharacterFields$MpDebitMutation|local/lito381/server/NativeDeCharacterFields|MpDebitMutation|0x1a; local/lito381/server/NativeDeCharacterFields$SkillEffectSnapshot|local/lito381/server/NativeDeCharacterFields|SkillEffectSnapshot|0x1a; local/lito381/server/NativeDeCharacterFields$Snapshot|local/lito381/server/NativeDeCharacterFields|Snapshot|0x18; local/lito381/server/NativeDeCharacterFields$ViewerHpState|local/lito381/server/NativeDeCharacterFields|ViewerHpState|0x1a; local/lito381/server/NativeDeConstructionPorts$CubeLease|local/lito381/server/NativeDeConstructionPorts|CubeLease|0x608; local/lito381/server/NativeDeConstructionPorts$DamageQueue|local/lito381/server/NativeDeConstructionPorts|DamageQueue|0x608; local/lito381/server/NativeDeConstructionPorts$Follower|local/lito381/server/NativeDeConstructionPorts|Follower|0x608; local/lito381/server/NativeDeConstructionPorts$ItemDelayTimer|local/lito381/server/NativeDeConstructionPorts|ItemDelayTimer|0x608; local/lito381/server/NativeDeConstructionPorts$Paralysis|local/lito381/server/NativeDeConstructionPorts|Paralysis|0x608; local/lito381/server/NativeDeConstructionPorts$SkillTimer|local/lito381/server/NativeDeConstructionPorts|SkillTimer|0x608; local/lito381/server/NativeDeDeletionPorts$Doll|local/lito381/server/NativeDeDeletionPorts|Doll|0x408; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDePerceptionPorts$PoisonTransition|local/lito381/server/NativeDePerceptionPorts|PoisonTransition|0x408; local/lito381/server/NativeDePerceptionPorts$Poison|local/lito381/server/NativeDePerceptionPorts|Poison|0x408; local/lito381/server/NativeDeResurrectionEffects$RemoveObject|local/lito381/server/NativeDeResurrectionEffects|RemoveObject|0x19; local/lito381/server/NativeDeResurrectionEffects$Viewer|local/lito381/server/NativeDeResurrectionEffects|Viewer|0x609
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/Map$Entry|java/util/Map|Entry|0x609; local/lito381/server/NativeDeCharacterFields$CombatSkillDerived|local/lito381/server/NativeDeCharacterFields|CombatSkillDerived|0x19; local/lito381/server/NativeDeCharacterFields$CurrentHpMutation|local/lito381/server/NativeDeCharacterFields|CurrentHpMutation|0xc; local/lito381/server/NativeDeCharacterFields$CurrentMpMutation|local/lito381/server/NativeDeCharacterFields|CurrentMpMutation|0xc; local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt|local/lito381/server/NativeDeCharacterFields|DamageMutationReceipt|0x1c; local/lito381/server/NativeDeCharacterFields$ErrorLog|local/lito381/server/NativeDeCharacterFields|ErrorLog|0x1a; local/lito381/server/NativeDeCharacterFields$MpDebitMutation|local/lito381/server/NativeDeCharacterFields|MpDebitMutation|0x1a; local/lito381/server/NativeDeCharacterFields$SkillEffectSnapshot|local/lito381/server/NativeDeCharacterFields|SkillEffectSnapshot|0x1a; local/lito381/server/NativeDeCharacterFields$Snapshot|local/lito381/server/NativeDeCharacterFields|Snapshot|0x18; local/lito381/server/NativeDeCharacterFields$ViewerHpState|local/lito381/server/NativeDeCharacterFields|ViewerHpState|0x1a; local/lito381/server/NativeDeResurrectionEffects$RemoveObject|local/lito381/server/NativeDeResurrectionEffects|RemoveObject|0x19; local/lito381/server/NativeDeResurrectionEffects$Viewer|local/lito381/server/NativeDeResurrectionEffects|Viewer|0x609
 class recordComponents=
 field DAMAGE_MUTATION_RECEIPTS:Ljava/lang/ThreadLocal; access=0x1a
 field DAMAGE_MUTATION_RECEIPTS:Ljava/lang/ThreadLocal; signature=Ljava/lang/ThreadLocal<Ljava/util/ArrayDeque<Llocal/lito381/server/NativeDeCharacterFields$DamageMutationReceipt;>;>;
@@ -23,26 +23,6 @@
 field MAX_HP_MP_STANDARD:I signature=null
 field MAX_HP_MP_STANDARD:I value=Integer:32767
 field MAX_HP_MP_STANDARD:I annotations=|||
-field SKD1:Ljava/util/HashMap; access=0x18
-field SKD1:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKD1:Ljava/util/HashMap; value=
-field SKD1:Ljava/util/HashMap; annotations=|||
-field SKD2:Ljava/util/HashMap; access=0x18
-field SKD2:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKD2:Ljava/util/HashMap; value=
-field SKD2:Ljava/util/HashMap; annotations=|||
-field SKD3:Ljava/util/HashMap; access=0x18
-field SKD3:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKD3:Ljava/util/HashMap; value=
-field SKD3:Ljava/util/HashMap; annotations=|||
-field SKU1:Ljava/util/HashMap; access=0x18
-field SKU1:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKU1:Ljava/util/HashMap; value=
-field SKU1:Ljava/util/HashMap; annotations=|||
-field SKU2:Ljava/util/HashMap; access=0x18
-field SKU2:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKU2:Ljava/util/HashMap; value=
-field SKU2:Ljava/util/HashMap; annotations=|||
 field _ac:I access=0x2
 field _ac:I signature=null
 field _ac:I value=
```

#### `method <clinit>()V`

```diff
--- staged
+++ formal
@@ -3,26 +3,6 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-NEW java/util/HashMap
-DUP
-INVOKESPECIAL java/util/HashMap.<init>()V
-PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKU1:Ljava/util/HashMap;
-NEW java/util/HashMap
-DUP
-INVOKESPECIAL java/util/HashMap.<init>()V
-PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKU2:Ljava/util/HashMap;
-NEW java/util/HashMap
-DUP
-INVOKESPECIAL java/util/HashMap.<init>()V
-PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKD1:Ljava/util/HashMap;
-NEW java/util/HashMap
-DUP
-INVOKESPECIAL java/util/HashMap.<init>()V
-PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKD2:Ljava/util/HashMap;
-NEW java/util/HashMap
-DUP
-INVOKESPECIAL java/util/HashMap.<init>()V
-PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKD3:Ljava/util/HashMap;
 NEW java/lang/ThreadLocal
 DUP
 INVOKESPECIAL java/lang/ThreadLocal.<init>()V
```

#### `method drainVitalCompletionsAfterActionUnlock()V`

```diff
--- staged
+++ formal
@@ -3,103 +3,99 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L2 L3 L4 java/lang/Throwable
-TRY L4 L5 L4 java/lang/Throwable
+TRY L3 L4 L5 java/lang/Throwable
+TRY L5 L6 L5 java/lang/Throwable
 TRY L9 L10 L11 java/lang/Throwable
-TRY L1 L8 L13 java/lang/Throwable
-TRY L9 L12 L13 java/lang/Throwable
-TRY L13 L14 L13 java/lang/Throwable
+TRY L2 L13 L14 java/lang/Throwable
+TRY L14 L15 L14 java/lang/Throwable
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.vitalMutationLock()Ljava/util/concurrent/locks/ReentrantLock;
 ASTORE 1
 LABEL L0
 ALOAD 1
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.isHeldByCurrentThread()Z
-IFNE L16
+IFNE L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._vitalCompletionDraining:Ljava/util/concurrent/atomic/AtomicBoolean;
 CONST Integer:0
 CONST Integer:1
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.compareAndSet(ZZ)Z
-IFEQ L16
+IFNE L2
 LABEL L1
+RETURN
+LABEL L2
 NEW java/util/ArrayDeque
 DUP
 INVOKESPECIAL java/util/ArrayDeque.<init>()V
 ASTORE 2
 ALOAD 1
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
-LABEL L2
+LABEL L3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._vitalCompletions:Ljava/util/concurrent/ConcurrentLinkedQueue;
 INVOKEVIRTUAL java/util/concurrent/ConcurrentLinkedQueue.poll()Ljava/lang/Object;
 CHECKCAST java/lang/Runnable
 DUP
 ASTORE 3
-IFNULL L3
+IFNULL L4
 ALOAD 2
 ALOAD 3
 INVOKEVIRTUAL java/util/ArrayDeque.addLast(Ljava/lang/Object;)V
-GOTO L2
-LABEL L3
+GOTO L3
+LABEL L4
 ALOAD 1
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L6
-LABEL L4
+GOTO L7
+LABEL L5
 ASTORE 4
-LABEL L5
+LABEL L6
 ALOAD 1
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 4
 ATHROW
-LABEL L6
+LABEL L7
 ALOAD 2
 INVOKEVIRTUAL java/util/ArrayDeque.isEmpty()Z
-IFNE L12
-LABEL L7
+IFEQ L8
+GOTO L13
+LABEL L8
 ALOAD 2
 INVOKEVIRTUAL java/util/ArrayDeque.pollFirst()Ljava/lang/Object;
 CHECKCAST java/lang/Runnable
 DUP
 ASTORE 3
-IFNONNULL L9
-LABEL L8
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeCharacterFields._vitalCompletionDraining:Ljava/util/concurrent/atomic/AtomicBoolean;
-CONST Integer:0
-INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.set(Z)V
-GOTO L1
+IFNULL L12
 LABEL L9
 ALOAD 3
 INVOKEINTERFACE java/lang/Runnable.run()V itf
 LABEL L10
-GOTO L7
+GOTO L8
 LABEL L11
 ASTORE 4
 ALOAD 0
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.reportCompletionFailure(Ljava/lang/Throwable;)V
-GOTO L7
+GOTO L8
 LABEL L12
+GOTO L2
+LABEL L13
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._vitalCompletionDraining:Ljava/util/concurrent/atomic/AtomicBoolean;
 CONST Integer:0
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.set(Z)V
-GOTO L15
-LABEL L13
+GOTO L16
+LABEL L14
 ASTORE 5
-LABEL L14
+LABEL L15
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._vitalCompletionDraining:Ljava/util/concurrent/atomic/AtomicBoolean;
 CONST Integer:0
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.set(Z)V
 ALOAD 5
 ATHROW
-LABEL L15
+LABEL L16
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._vitalCompletions:Ljava/util/concurrent/ConcurrentLinkedQueue;
 INVOKEVIRTUAL java/util/concurrent/ConcurrentLinkedQueue.isEmpty()Z
 IFEQ L0
 RETURN
-LABEL L16
-RETURN
```

#### `method getChaLightSize()I`

```diff
--- staged
+++ formal
@@ -7,9 +7,8 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.isInvisble()Z
 IFEQ L0
 CONST Integer:0
-GOTO L1
+IRETURN
 LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._chaLightSize:I
-LABEL L1
 IRETURN
```

#### `method getMr()I`

```diff
--- staged
+++ formal
@@ -6,14 +6,14 @@
 ALOAD 0
 CONST Integer:153
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.hasSkillEffect(I)Z
-IFEQ L0
+CONST Integer:1
+IF_ICMPNE L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._mr:I
 CONST Integer:2
 ISHR
-GOTO L1
+IRETURN
 LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._mr:I
-LABEL L1
 IRETURN
```

#### `method getOwnLightSize()I`

```diff
--- staged
+++ formal
@@ -7,9 +7,8 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.isInvisble()Z
 IFEQ L0
 CONST Integer:0
-GOTO L1
+IRETURN
 LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._ownLightSize:I
-LABEL L1
 IRETURN
```

#### `method hpMeterRatio()I`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 ILOAD 1
 IFGT L0
 CONST Integer:100
-GOTO L1
+IRETURN
 LABEL L0
 CONST Integer:100
 ALOAD 0
@@ -17,5 +17,4 @@
 IMUL
 ILOAD 1
 IDIV
-LABEL L1
 IRETURN
```

#### `method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z`

```diff
--- staged
+++ formal
@@ -3,22 +3,26 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L0 L3 L4 java/lang/Throwable
-TRY L4 L5 L4 java/lang/Throwable
+TRY L2 L5 L6 java/lang/Throwable
+TRY L6 L7 L6 java/lang/Throwable
 ALOAD 0
 ASTORE 3
 ALOAD 3
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
-IFEQ L7
+IFEQ L0
 ALOAD 3
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
 ASTORE 2
 ALOAD 1
-IFNULL L7
+IFNONNULL L1
+LABEL L0
+CONST Integer:1
+IRETURN
+LABEL L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
-LABEL L0
+LABEL L2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjects:Ljava/util/concurrent/CopyOnWriteArrayList;
 ALOAD 1
@@ -26,29 +30,24 @@
 ALOAD 1
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.isViewerRegistered(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Z
-IF_ICMPNE L1
+IF_ICMPNE L3
 CONST Integer:1
-GOTO L2
-LABEL L1
+GOTO L4
+LABEL L3
 CONST Integer:0
-LABEL L2
+LABEL L4
 ISTORE 3
-LABEL L3
+LABEL L5
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L6
-LABEL L4
+ILOAD 3
+IRETURN
+LABEL L6
 ASTORE 4
-LABEL L5
+LABEL L7
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 4
 ATHROW
-LABEL L6
-ILOAD 3
-IRETURN
-LABEL L7
-CONST Integer:1
-IRETURN
```

#### `method lambda$adjustCurrentHpIfAlive$#0(JI)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;`

```diff
--- staged
+++ formal
@@ -8,7 +8,19 @@
 IFLE L0
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.isDead()Z
-IFNE L0
+IFEQ L1
+LABEL L0
+ALOAD 0
+NEW local/lito381/server/NativeDeCharacterFields$CurrentHpMutation
+DUP
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeCharacterFields._currentHp:I
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeCharacterFields._currentHp:I
+INVOKESPECIAL local/lito381/server/NativeDeCharacterFields$CurrentHpMutation.<init>(II)V
+INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;
+ARETURN
+LABEL L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._currentHp:I
 LLOAD 1
@@ -26,14 +38,3 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.commitCurrentHpLocked(I)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;
 ARETURN
-LABEL L0
-ALOAD 0
-NEW local/lito381/server/NativeDeCharacterFields$CurrentHpMutation
-DUP
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeCharacterFields._currentHp:I
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeCharacterFields._currentHp:I
-INVOKESPECIAL local/lito381/server/NativeDeCharacterFields$CurrentHpMutation.<init>(II)V
-INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;
-ARETURN
```

#### `method lambda$broadcastPacketHP$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Ljava/lang/Integer;Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;)Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;`

```diff
--- staged
+++ formal
@@ -0,0 +1,20 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 2
+IFNULL L0
+ALOAD 2
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields$ViewerHpState.belongsTo(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Z
+IFEQ L0
+ALOAD 2
+GOTO L1
+LABEL L0
+NEW local/lito381/server/NativeDeCharacterFields$ViewerHpState
+DUP
+ALOAD 0
+INVOKESPECIAL local/lito381/server/NativeDeCharacterFields$ViewerHpState.<init>(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
+LABEL L1
+ARETURN
```

#### `method lambda$setStatus$#0(I)Ljava/lang/Integer;`

```diff
--- staged
+++ formal
@@ -1,11 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-DUP_X1
-PUTFIELD local/lito381/server/NativeDeCharacterFields._status:I
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ARETURN
```

#### `method lambda$setStatus$#0(I)V`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0x2
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+ILOAD 1
+PUTFIELD local/lito381/server/NativeDeCharacterFields._status:I
+RETURN
```

#### `method lambda$visibilitySource$broadcastPacketHP$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Ljava/lang/Integer;Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;)Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;`

```diff
--- staged
+++ formal
@@ -1,20 +0,0 @@
-access=0xa
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 2
-IFNULL L0
-ALOAD 2
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields$ViewerHpState.belongsTo(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Z
-IFEQ L0
-ALOAD 2
-GOTO L1
-LABEL L0
-NEW local/lito381/server/NativeDeCharacterFields$ViewerHpState
-DUP
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeCharacterFields$ViewerHpState.<init>(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-LABEL L1
-ARETURN
```

#### `method recordDamageDeathMutation(ZZ)V`

```diff
--- staged
+++ formal
@@ -5,20 +5,24 @@
 default=
 ILOAD 1
 ILOAD 2
-IF_ICMPEQ L2
+IF_ICMPNE L0
+RETURN
+LABEL L0
 GETSTATIC local/lito381/server/NativeDeCharacterFields.DAMAGE_MUTATION_RECEIPTS:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST java/util/ArrayDeque
 ASTORE 3
 ALOAD 3
-IFNULL L2
+IFNONNULL L1
+RETURN
+LABEL L1
 ALOAD 3
 INVOKEVIRTUAL java/util/ArrayDeque.iterator()Ljava/util/Iterator;
 ASTORE 4
-LABEL L0
+LABEL L2
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L2
+IFEQ L4
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt
@@ -26,7 +30,7 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._target:Llocal/lito381/server/NativeDeCharacterFields;
 ALOAD 0
-IF_ACMPNE L1
+IF_ACMPNE L3
 ALOAD 5
 ILOAD 1
 ILOAD 2
@@ -34,7 +38,7 @@
 GETFIELD local/lito381/server/NativeDeCharacterFields._currentHp:I
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt.recordDead(ZZI)V
 RETURN
-LABEL L1
-GOTO L0
-LABEL L2
+LABEL L3
+GOTO L2
+LABEL L4
 RETURN
```

#### `method recordDamageHpMutation(II)V`

```diff
--- staged
+++ formal
@@ -5,20 +5,24 @@
 default=
 ILOAD 1
 ILOAD 2
-IF_ICMPEQ L2
+IF_ICMPNE L0
+RETURN
+LABEL L0
 GETSTATIC local/lito381/server/NativeDeCharacterFields.DAMAGE_MUTATION_RECEIPTS:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST java/util/ArrayDeque
 ASTORE 3
 ALOAD 3
-IFNULL L2
+IFNONNULL L1
+RETURN
+LABEL L1
 ALOAD 3
 INVOKEVIRTUAL java/util/ArrayDeque.iterator()Ljava/util/Iterator;
 ASTORE 4
-LABEL L0
+LABEL L2
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L2
+IFEQ L4
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt
@@ -26,13 +30,13 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt._target:Llocal/lito381/server/NativeDeCharacterFields;
 ALOAD 0
-IF_ACMPNE L1
+IF_ACMPNE L3
 ALOAD 5
 ILOAD 1
 ILOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt.recordHp(II)V
 RETURN
-LABEL L1
-GOTO L0
-LABEL L2
+LABEL L3
+GOTO L2
+LABEL L4
 RETURN
```

#### `method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z`

```diff
--- staged
+++ formal
@@ -18,7 +18,7 @@
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjects:Ljava/util/concurrent/CopyOnWriteArrayList;
 ALOAD 1
 INVOKEVIRTUAL java/util/concurrent/CopyOnWriteArrayList.remove(Ljava/lang/Object;)Z
-ISTORE 3
+ISTORE 2
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
 IFEQ L2
@@ -29,38 +29,36 @@
 POP
 LABEL L2
 ALOAD 0
-ASTORE 6
-ALOAD 6
+ASTORE 5
+ALOAD 5
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
 IFEQ L3
-ALOAD 6
+ALOAD 5
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
-ASTORE 4
+ASTORE 3
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeDeletionState
 IFEQ L3
 ALOAD 1
 CHECKCAST local/lito381/server/NativeDeDeletionState
-ASTORE 5
-ALOAD 5
+ASTORE 4
 ALOAD 4
+ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.unregisterViewer(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
 LABEL L3
-ILOAD 3
-ISTORE 2
+ILOAD 2
+ISTORE 3
 LABEL L4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L7
+ILOAD 3
+IRETURN
 LABEL L5
-ASTORE 7
+ASTORE 6
 LABEL L6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ALOAD 7
+ALOAD 6
 ATHROW
-LABEL L7
-ILOAD 2
-IRETURN
```

#### `method setStatus(I)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,6 @@
 ALOAD 0
 ALOAD 0
 ILOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeCharacterFields;I)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeCharacterFields.lambda$setStatus$#0(I)Ljava/lang/Integer;, type:()Ljava/lang/Integer;]
-INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object;
-POP
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeCharacterFields;I)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H5 local/lito381/server/NativeDeCharacterFields.lambda$setStatus$#0(I)V, type:()V]
+INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.withVitalMutation(Ljava/lang/Runnable;)V
 RETURN
```

#### `method snapshot()Llocal/lito381/server/NativeDeCharacterFields$Snapshot;`

```diff
--- staged
+++ formal
@@ -235,7 +235,8 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._npcVitalMutationLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L3
+ALOAD 2
+ARETURN
 LABEL L2
 ASTORE 3
 ALOAD 0
@@ -243,6 +244,3 @@
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 3
 ATHROW
-LABEL L3
-ALOAD 2
-ARETURN
```

#### `method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z`

```diff
--- staged
+++ formal
@@ -3,9 +3,9 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L1 L4 L7 java/lang/Throwable
-TRY L5 L6 L7 java/lang/Throwable
-TRY L7 L8 L7 java/lang/Throwable
+TRY L1 L2 L6 java/lang/Throwable
+TRY L3 L5 L6 java/lang/Throwable
+TRY L6 L7 L6 java/lang/Throwable
 ALOAD 1
 IFNONNULL L0
 CONST Integer:0
@@ -16,63 +16,60 @@
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
 LABEL L1
 ALOAD 0
-ASTORE 5
-ALOAD 5
+ASTORE 4
+ALOAD 4
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
-IFEQ L2
-ALOAD 5
+IFEQ L3
+ALOAD 4
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
-ASTORE 3
+ASTORE 2
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeDeletionState
-IFEQ L2
+IFEQ L3
 ALOAD 1
 CHECKCAST local/lito381/server/NativeDeDeletionState
-ASTORE 4
-ALOAD 4
+ASTORE 3
 ALOAD 3
+ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.registerViewer(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Z
-IFEQ L5
+IFNE L3
+CONST Integer:0
+ISTORE 4
 LABEL L2
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+ILOAD 4
+IRETURN
+LABEL L3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjects:Ljava/util/concurrent/CopyOnWriteArrayList;
 ALOAD 1
 INVOKEVIRTUAL java/util/concurrent/CopyOnWriteArrayList.addIfAbsent(Ljava/lang/Object;)Z
-IFEQ L3
+IFEQ L4
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
-IFEQ L3
+IFEQ L4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownPlayer:Ljava/util/concurrent/CopyOnWriteArrayList;
 ALOAD 1
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
 INVOKEVIRTUAL java/util/concurrent/CopyOnWriteArrayList.addIfAbsent(Ljava/lang/Object;)Z
 POP
-LABEL L3
+LABEL L4
 CONST Integer:1
-ISTORE 5
-LABEL L4
+ISTORE 2
+LABEL L5
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ILOAD 5
+ILOAD 2
 IRETURN
-LABEL L5
-CONST Integer:0
-ISTORE 2
 LABEL L6
+ASTORE 5
+LABEL L7
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L9
-LABEL L7
-ASTORE 6
-LABEL L8
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ALOAD 6
+ALOAD 5
 ATHROW
-LABEL L9
-ILOAD 2
-IRETURN
```

#### `method visibilitySource$broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`

```diff
--- staged
+++ formal
@@ -31,7 +31,7 @@
 ILOAD 3
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 1
-INVOKEDYNAMIC apply(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Ljava/util/function/BiFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeCharacterFields.lambda$visibilitySource$broadcastPacketHP$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Ljava/lang/Integer;Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;)Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;, type:(Ljava/lang/Integer;Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;)Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;]
+INVOKEDYNAMIC apply(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Ljava/util/function/BiFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeCharacterFields.lambda$broadcastPacketHP$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Ljava/lang/Integer;Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;)Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;, type:(Ljava/lang/Integer;Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;)Llocal/lito381/server/NativeDeCharacterFields$ViewerHpState;]
 INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.compute(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeCharacterFields$ViewerHpState
 ASTORE 4
```

#### `method withKnownLock(Ljava/util/function/Supplier;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -15,7 +15,8 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCharacterFields._knownObjectLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L3
+ALOAD 2
+ARETURN
 LABEL L2
 ASTORE 3
 ALOAD 0
@@ -23,6 +24,3 @@
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 3
 ATHROW
-LABEL L3
-ALOAD 2
-ARETURN
```

#### `method withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -3,8 +3,8 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L1 L2 L3 java/lang/Throwable
-TRY L3 L4 L3 java/lang/Throwable
+TRY L1 L2 L4 java/lang/Throwable
+TRY L4 L5 L4 java/lang/Throwable
 ALOAD 1
 IFNONNULL L0
 NEW java/lang/NullPointerException
@@ -27,23 +27,22 @@
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 2
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.isHeldByCurrentThread()Z
-IFNE L6
+IFNE L3
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.drainVitalCompletionsAfterActionUnlock()V
-GOTO L6
 LABEL L3
+ALOAD 3
+ARETURN
+LABEL L4
 ASTORE 4
-LABEL L4
+LABEL L5
 ALOAD 2
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 2
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.isHeldByCurrentThread()Z
-IFNE L5
+IFNE L6
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.drainVitalCompletionsAfterActionUnlock()V
-LABEL L5
+LABEL L6
 ALOAD 4
 ATHROW
-LABEL L6
-ALOAD 3
-ARETURN
```

### `NativeDeChatBroadcast`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,5 +9,5 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+class innerClasses=
 class recordComponents=
```

#### `method <init>()V`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x1
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL java/lang/Object.<init>()V
-RETURN
```

### `NativeDeChatChat`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 class recordComponents=
 field _byte:[B access=0x2
 field _byte:[B signature=null
```

### `NativeDeChatChatGlobal`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 class recordComponents=
 field _byte:[B access=0x2
 field _byte:[B signature=null
```

### `NativeDeChatChatShouting`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 class recordComponents=
 field _byte:[B access=0x2
 field _byte:[B signature=null
```

### `NativeDeChatTable`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeChatScope$Data|local/lito381/server/NativeDeChatScope|Data|0x18; local/lito381/server/NativeDeChatScope$Logs|local/lito381/server/NativeDeChatScope|Logs|0x18; local/lito381/server/NativeDeTerrainScope$Database|local/lito381/server/NativeDeTerrainScope|Database|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 class recordComponents=
 field _allTimeValues:Ljava/util/Collection; access=0x2
 field _allTimeValues:Ljava/util/Collection; signature=Ljava/util/Collection<Llocal/lito381/server/NativeDeChatData;>;
```

#### `method load()V`

```diff
--- staged
+++ formal
@@ -173,6 +173,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (npcid: \u0001)"]
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L10
 GOTO L3
 LABEL L11
```

### `NativeDeChatTimer$RunSequenceTask`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeChatScope$Pool$ShutdownDiscardable|local/lito381/server/NativeDeChatScope$Pool|ShutdownDiscardable|0x608; local/lito381/server/NativeDeChatScope$Pool|local/lito381/server/NativeDeChatScope|Pool|0x18; local/lito381/server/NativeDeChatTimer$RunSequenceTask|local/lito381/server/NativeDeChatTimer|RunSequenceTask|0x12
+class innerClasses=local/lito381/server/NativeDeChatScope$Pool$ShutdownDiscardable|local/lito381/server/NativeDeChatScope$Pool|ShutdownDiscardable|0x609; local/lito381/server/NativeDeChatTimer$RunSequenceTask|local/lito381/server/NativeDeChatTimer|RunSequenceTask|0x12
 class recordComponents=
 field this$0:Llocal/lito381/server/NativeDeChatTimer; access=0x10
 field this$0:Llocal/lito381/server/NativeDeChatTimer; signature=null
```

### `NativeDeChatTimer`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=local/lito381/server/NativeDeChatTimer$RunSequenceTask
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeChatScope$Clock|local/lito381/server/NativeDeChatScope|Clock|0x18; local/lito381/server/NativeDeChatScope$Logs|local/lito381/server/NativeDeChatScope|Logs|0x18; local/lito381/server/NativeDeChatScope$Pool|local/lito381/server/NativeDeChatScope|Pool|0x18; local/lito381/server/NativeDeChatTimer$RunSequenceTask|local/lito381/server/NativeDeChatTimer|RunSequenceTask|0x12; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeChatTimer$RunSequenceTask|local/lito381/server/NativeDeChatTimer|RunSequenceTask|0x12
 class recordComponents=
 field ACTIVE_COUNT:Ljava/util/concurrent/atomic/AtomicLong; access=0x1a
 field ACTIVE_COUNT:Ljava/util/concurrent/atomic/AtomicLong; signature=null
```

### `NativeDeChatWire`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method content(Llocal/lito381/server/NativeDeVisibilityPacket;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)[B`

```diff
--- staged
+++ formal
@@ -3,8 +3,8 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L0 L6 L7 java/lang/Throwable
-TRY L7 L8 L7 java/lang/Throwable
+TRY L0 L5 L6 java/lang/Throwable
+TRY L6 L7 L6 java/lang/Throwable
 ALOAD 0
 DUP
 ASTORE 2
@@ -68,18 +68,16 @@
 LABEL L3
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/Protocol$Reader.remaining()I
-IFLE L5
+IFLE L4
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/Protocol$Reader.c()I
-IFNE L4
-GOTO L3
-LABEL L4
+IFEQ L3
 NEW java/lang/IllegalArgumentException
 DUP
 CONST str:"source NPC chat trailing bytes"
 INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L5
+LABEL L4
 ALOAD 1
 CHECKCAST local/lito381/server/NativeDeCharacterFields
 ASTORE 9
@@ -99,12 +97,12 @@
 ALOAD 10
 ALOAD 2
 MONITOREXIT
+LABEL L5
+ARETURN
 LABEL L6
-ARETURN
-LABEL L7
 ASTORE 11
 ALOAD 2
 MONITOREXIT
-LABEL L8
+LABEL L7
 ALOAD 11
 ATHROW
```

#### `method requirePublication(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V`

```diff
--- staged
+++ formal
@@ -5,37 +5,35 @@
 default=
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeChatWire.subject(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
-ASTORE 3
-ALOAD 3
+ASTORE 2
+ALOAD 2
 IFNONNULL L0
 RETURN
 LABEL L0
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeDeWorldContext.entry(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldContext$Entry;
+ASTORE 3
 ALOAD 3
-INVOKESTATIC local/lito381/server/NativeDeWorldContext.entry(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldContext$Entry;
-ASTORE 4
-ALOAD 4
 GETFIELD local/lito381/server/NativeDeWorldContext$Entry.context:Llocal/lito381/server/NativeDeWorldContext;
 GETFIELD local/lito381/server/NativeDeWorldContext.closed:Z
 IFNE L1
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeWorldContext.entry(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldContext$Entry;
 GETFIELD local/lito381/server/NativeDeWorldContext$Entry.context:Llocal/lito381/server/NativeDeWorldContext;
-ALOAD 4
+ALOAD 3
 GETFIELD local/lito381/server/NativeDeWorldContext$Entry.context:Llocal/lito381/server/NativeDeWorldContext;
 IF_ACMPNE L1
-ALOAD 3
+ALOAD 2
 INSTANCEOF local/lito381/server/NativeDeDeletionState
 IFEQ L2
-ALOAD 3
+ALOAD 2
 CHECKCAST local/lito381/server/NativeDeDeletionState
-DUP
-ASTORE 2
-CHECKCAST local/lito381/server/NativeDeNpcVitalState
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.destroyed()Z
+ASTORE 4
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.destroyed()Z
 IFNE L1
-ALOAD 2
-CHECKCAST local/lito381/server/NativeDeNpcVitalState
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.deletionStarted()Z
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.deletionStarted()Z
 IFEQ L2
 LABEL L1
 NEW java/util/concurrent/RejectedExecutionException
@@ -44,14 +42,13 @@
 INVOKESPECIAL java/util/concurrent/RejectedExecutionException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L2
-ALOAD 4
+ALOAD 3
 GETFIELD local/lito381/server/NativeDeWorldContext$Entry.context:Llocal/lito381/server/NativeDeWorldContext;
 INVOKESTATIC local/lito381/server/NativeDeChatJobs.find(Llocal/lito381/server/NativeDeWorldContext;)Llocal/lito381/server/NativeDeChatJobs$Owner;
-ASTORE 2
-ALOAD 2
+ASTORE 4
+ALOAD 4
 IFNULL L3
-ALOAD 2
-CHECKCAST local/lito381/server/NativeDeChatJobs$Owner
+ALOAD 4
 GETFIELD local/lito381/server/NativeDeChatJobs$Owner.closed:Z
 IFEQ L4
 LABEL L3
```

#### `method subject(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;`

```diff
--- staged
+++ formal
@@ -8,10 +8,9 @@
 IFEQ L0
 ALOAD 0
 CHECKCAST local/lito381/server/NativeDeChatChat
-ASTORE 2
-ALOAD 2
+ASTORE 3
+ALOAD 3
 GETFIELD local/lito381/server/NativeDeChatChat.nativeSubject:Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
-ASTORE 1
 GOTO L3
 LABEL L0
 ALOAD 0
@@ -22,7 +21,6 @@
 ASTORE 2
 ALOAD 2
 GETFIELD local/lito381/server/NativeDeChatChatGlobal.nativeSubject:Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
-ASTORE 1
 GOTO L3
 LABEL L1
 ALOAD 0
@@ -30,14 +28,11 @@
 IFEQ L2
 ALOAD 0
 CHECKCAST local/lito381/server/NativeDeChatChatShouting
-ASTORE 2
-ALOAD 2
+ASTORE 1
+ALOAD 1
 GETFIELD local/lito381/server/NativeDeChatChatShouting.nativeSubject:Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
-ASTORE 1
 GOTO L3
 LABEL L2
 CONST null
-ASTORE 1
 LABEL L3
-ALOAD 1
 ARETURN
```

### `NativeDeClientViewer`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeCorpseTimers$Owner|local/lito381/server/NativeDeCorpseTimers|Owner|0x18; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$OtherList|local/lito381/server/NativeDeDeletionPorts|OtherList|0x608; local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8; local/lito381/server/NativeDeDeletionPorts$RemoveObject|local/lito381/server/NativeDeDeletionPorts|RemoveObject|0x18; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativePlayerCommit$Binding|local/lito381/server/NativePlayerCommit|Binding|0x18; local/lito381/server/NativePlayerLifetime$Mode|local/lito381/server/NativePlayerLifetime|Mode|0x4018
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeCorpseTimers$Owner|local/lito381/server/NativeDeCorpseTimers|Owner|0x18; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$OtherList|local/lito381/server/NativeDeDeletionPorts|OtherList|0x608; local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8; local/lito381/server/NativeDeDeletionPorts$RemoveObject|local/lito381/server/NativeDeDeletionPorts|RemoveObject|0x18; local/lito381/server/NativePlayerCommit$Binding|local/lito381/server/NativePlayerCommit|Binding|0x18; local/lito381/server/NativePlayerLifetime$Mode|local/lito381/server/NativePlayerLifetime|Mode|0x4018
 class recordComponents=
 field FIELD:Ljava/lang/String; access=0x18
 field FIELD:Ljava/lang/String; signature=null
```

#### `method current()Llocal/lito381/server/ClientSession;`

```diff
--- staged
+++ formal
@@ -5,21 +5,25 @@
 default=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeClientViewer.retired:Z
-IFNE L3
+IFNE L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeClientViewer.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.STOPPED:Llocal/lito381/server/NativePlayerLifetime$Mode;
-IF_ACMPEQ L3
+IF_ACMPNE L1
+LABEL L0
+CONST null
+ARETURN
+LABEL L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeClientViewer.hub:Llocal/lito381/server/GameHub;
 INVOKEVIRTUAL local/lito381/server/GameHub.connectedSessionsSnapshot()Ljava/util/List;
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 1
-LABEL L0
+LABEL L2
 ALOAD 1
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L2
+IFEQ L4
 ALOAD 1
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -28,14 +32,11 @@
 GETFIELD local/lito381/server/NativeDeClientViewer.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-IFEQ L1
+IFEQ L3
 ALOAD 2
 ARETURN
-LABEL L1
-GOTO L0
-LABEL L2
+LABEL L3
+GOTO L2
+LABEL L4
 CONST null
 ARETURN
-LABEL L3
-CONST null
-ARETURN
```

#### `method objectId(J)I`

```diff
--- staged
+++ formal
@@ -10,13 +10,14 @@
 LLOAD 0
 CONST Long:4294967295
 LCMP
-IFGT L0
-LLOAD 0
-L2I
-IRETURN
+IFLE L1
 LABEL L0
 NEW java/lang/IllegalArgumentException
 DUP
 CONST str:"source viewer object ID outside unsigned32"
 INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
+LABEL L1
+LLOAD 0
+L2I
+IRETURN
```

#### `method retireClient(Llocal/lito381/server/ClientSession;)V`

```diff
--- staged
+++ formal
@@ -4,13 +4,15 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 ALOAD 0
-IFNULL L0
+IFNONNULL L0
+RETURN
+LABEL L0
 ALOAD 0
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
 ASTORE 1
 ALOAD 1
-IFNULL L0
+IFNULL L1
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeClientViewer.retire(Llocal/lito381/server/BattleTimedStatus;)V
-LABEL L0
+LABEL L1
 RETURN
```

#### `method sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V`

```diff
--- staged
+++ formal
@@ -11,7 +11,7 @@
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeVisibilityOutput.send(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V
-GOTO L12
+RETURN
 LABEL L0
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDePerceptionPacket
@@ -20,7 +20,7 @@
 ALOAD 1
 CHECKCAST local/lito381/server/NativeDePerceptionPacket
 INVOKESTATIC local/lito381/server/NativeDePerceptionOutput.send(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDePerceptionPacket;)V
-GOTO L12
+RETURN
 LABEL L1
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$RemoveObject
@@ -97,7 +97,17 @@
 CONST str:"sourceDeKnownViewer"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ALOAD 0
-IF_ACMPNE L7
+IF_ACMPEQ L8
+LABEL L7
+NEW java/lang/IllegalStateException
+DUP
+CONST str:"stale native viewer publication"
+INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
+ATHROW
+LABEL L8
+ALOAD 4
+CONST str:"sendEncryptedFrame"
+CONST str:"([BLjava/lang/String;)V"
 CONST Integer:2
 ANEWARRAY java/lang/Object
 DUP
@@ -111,33 +121,20 @@
 INVOKESTATIC java/lang/Integer.toUnsignedLong(I)J
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_REMOVE_OBJECT source-De deletion id=\u0001"]
 AASTORE
-ASTORE 7
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeVisibilityRuntime.requirePublication(Llocal/lito381/server/NativeDeClientViewer;)V
-ALOAD 4
-CONST str:"sendEncryptedFrame"
-CONST str:"([BLjava/lang/String;)V"
-ALOAD 7
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
-GOTO L8
-LABEL L7
-NEW java/lang/IllegalStateException
-DUP
-CONST str:"stale native viewer publication"
-INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
-ATHROW
-LABEL L8
 ALOAD 6
 MONITOREXIT
 LABEL L9
 GOTO L12
 LABEL L10
-ASTORE 8
+ASTORE 7
 ALOAD 6
 MONITOREXIT
 LABEL L11
-ALOAD 8
+ALOAD 7
 ATHROW
 LABEL L12
 RETURN
```

#### `method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z`

```diff
--- staged
+++ formal
@@ -25,7 +25,7 @@
 GETFIELD local/lito381/server/NativeDeClientViewer.life:Llocal/lito381/server/NativePlayerLifetime;
 IF_ACMPEQ L1
 CONST Integer:0
-GOTO L2
+IRETURN
 LABEL L1
 ALOAD 0
 ALOAD 0
@@ -35,5 +35,4 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeClientViewer.withKnownLock(Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-LABEL L2
 IRETURN
```

### `NativeDeControlCatalog$Names`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18
 class recordComponents=byName:Ljava/util/Map; sig=Ljava/util/Map<Ljava/lang/String;Llocal/lito381/server/NativeDeInitializerKernel$Name;>; anns=| typeAnns=|; byId:Ljava/util/Map; sig=Ljava/util/Map<Ljava/lang/Integer;Llocal/lito381/server/NativeDeInitializerKernel$Name;>; anns=| typeAnns=|; history:Ljava/util/List; sig=Ljava/util/List<Llocal/lito381/server/NativeDeInitializerKernel$Name;>; anns=| typeAnns=|; selection:Ljava/util/List; sig=Ljava/util/List<Llocal/lito381/server/NativeDeInitializerKernel$Name;>; anns=| typeAnns=|; rows:I sig= anns=| typeAnns=|; invalid:I sig= anns=| typeAnns=|; duplicates:Ljava/util/List; sig=Ljava/util/List<Ljava/lang/Integer;>; anns=| typeAnns=|; sqlFailure:Z sig= anns=| typeAnns=|
 field byId:Ljava/util/Map; access=0x12
 field byId:Ljava/util/Map; signature=Ljava/util/Map<Ljava/lang/Integer;Llocal/lito381/server/NativeDeInitializerKernel$Name;>;
```

### `NativeDeControlCatalog$Reporter`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,5 +9,5 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeControlCatalog$Reporter|local/lito381/server/NativeDeControlCatalog|Reporter|0x608; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8
+class innerClasses=local/lito381/server/NativeDeControlCatalog$Reporter|local/lito381/server/NativeDeControlCatalog|Reporter|0x608; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18
 class recordComponents=
```

### `NativeDeControlCatalog`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=local/lito381/server/NativeDeControlCatalog$Abilities,local/lito381/server/NativeDeControlCatalog$Catalog,local/lito381/server/NativeDeControlCatalog$Definition,local/lito381/server/NativeDeControlCatalog$Names,local/lito381/server/NativeDeControlCatalog$Reporter,local/lito381/server/NativeDeControlCatalog$Settings
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Abilities|local/lito381/server/NativeDeControlCatalog|Abilities|0x18; local/lito381/server/NativeDeControlCatalog$Catalog|local/lito381/server/NativeDeControlCatalog|Catalog|0x18; local/lito381/server/NativeDeControlCatalog$Definition|local/lito381/server/NativeDeControlCatalog|Definition|0x18; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeControlCatalog$Reporter|local/lito381/server/NativeDeControlCatalog|Reporter|0x608; local/lito381/server/NativeDeControlCatalog$Settings|local/lito381/server/NativeDeControlCatalog|Settings|0x18; local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Abilities|local/lito381/server/NativeDeControlCatalog|Abilities|0x18; local/lito381/server/NativeDeControlCatalog$Catalog|local/lito381/server/NativeDeControlCatalog|Catalog|0x18; local/lito381/server/NativeDeControlCatalog$Definition|local/lito381/server/NativeDeControlCatalog|Definition|0x18; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeControlCatalog$Reporter|local/lito381/server/NativeDeControlCatalog|Reporter|0x608; local/lito381/server/NativeDeControlCatalog$Settings|local/lito381/server/NativeDeControlCatalog|Settings|0x18; local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18
 class recordComponents=
 field ABILITIES:Ljava/lang/String; access=0x18
 field ABILITIES:Ljava/lang/String; signature=null
```

#### `method names(Ljava/sql/Connection;Llocal/lito381/server/NativeDeControlCatalog$Names;Llocal/lito381/server/NativeDeControlCatalog$Reporter;)Llocal/lito381/server/NativeDeControlCatalog$Names;`

```diff
--- staged
+++ formal
@@ -4,11 +4,11 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 TRY L9 L10 L11 java/sql/SQLException
-TRY L4 L14 L15 java/lang/Throwable
-TRY L16 L17 L18 java/lang/Throwable
-TRY L3 L20 L21 java/lang/Throwable
-TRY L22 L23 L24 java/lang/Throwable
-TRY L2 L26 L27 java/sql/SQLException
+TRY L4 L15 L16 java/lang/Throwable
+TRY L17 L18 L19 java/lang/Throwable
+TRY L3 L21 L22 java/lang/Throwable
+TRY L23 L24 L25 java/lang/Throwable
+TRY L2 L27 L28 java/sql/SQLException
 NEW java/util/concurrent/ConcurrentHashMap
 DUP
 INVOKESPECIAL java/util/concurrent/ConcurrentHashMap.<init>()V
@@ -71,37 +71,37 @@
 LABEL L4
 ALOAD 11
 INVOKEINTERFACE java/sql/ResultSet.next()Z itf
-IFEQ L14
+IFEQ L15
 IINC var=6 incr=1
 ALOAD 11
 CONST str:"deobjid"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ISTORE 13
+ISTORE 12
 ALOAD 11
 CONST str:"denamebig5"
 INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+ASTORE 13
+ALOAD 11
+CONST str:"detitlebig5"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
 ASTORE 14
-ALOAD 11
-CONST str:"detitlebig5"
+ALOAD 14
+IFNONNULL L5
+CONST str:""
+ASTORE 14
+LABEL L5
+ALOAD 11
+CONST str:"deshopbig5"
 INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
 ASTORE 15
 ALOAD 15
-IFNONNULL L5
+IFNONNULL L6
 CONST str:""
 ASTORE 15
-LABEL L5
-ALOAD 11
-CONST str:"deshopbig5"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-DUP
-ASTORE 12
-IFNONNULL L6
-CONST str:""
-ASTORE 12
 LABEL L6
-ALOAD 14
+ALOAD 13
 IFNULL L7
-ALOAD 14
+ALOAD 13
 INVOKEVIRTUAL java/lang/String.trim()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
 IFEQ L8
@@ -109,7 +109,7 @@
 IINC var=7 incr=1
 ALOAD 2
 CONST str:"invalid-name"
-ILOAD 13
+ILOAD 12
 CONST null
 INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
 GOTO L4
@@ -142,27 +142,27 @@
 LABEL L12
 NEW local/lito381/server/NativeDeInitializerKernel$Name
 DUP
-ILOAD 13
-ALOAD 14
+ILOAD 12
+ALOAD 13
 ILOAD 16
 ILOAD 17
 ILOAD 18
 ILOAD 19
+ALOAD 14
 ALOAD 15
-ALOAD 12
 INVOKESPECIAL local/lito381/server/NativeDeInitializerKernel$Name.<init>(ILjava/lang/String;IIIILjava/lang/String;Ljava/lang/String;)V
 ASTORE 20
 ALOAD 3
-ALOAD 14
+ALOAD 13
 INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
 IFNONNULL L13
 ALOAD 3
-ALOAD 14
+ALOAD 13
 ALOAD 20
 INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 POP
 ALOAD 4
-ILOAD 13
+ILOAD 12
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 20
 INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
@@ -174,68 +174,69 @@
 ALOAD 2
 ALOAD 20
 INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.registered(Llocal/lito381/server/NativeDeInitializerKernel$Name;)V itf
-GOTO L4
+GOTO L14
 LABEL L13
 ALOAD 8
-ILOAD 13
+ILOAD 12
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
 POP
 ALOAD 2
 CONST str:"duplicate-name-write-suppressed"
-ILOAD 13
+ILOAD 12
 CONST null
 INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
+LABEL L14
 GOTO L4
-LABEL L14
+LABEL L15
+ALOAD 11
+IFNULL L21
+ALOAD 11
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+GOTO L21
+LABEL L16
+ASTORE 12
 ALOAD 11
 IFNULL L20
+LABEL L17
 ALOAD 11
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
+LABEL L18
 GOTO L20
-LABEL L15
-ASTORE 12
-ALOAD 11
-IFNULL L19
-LABEL L16
-ALOAD 11
-INVOKEINTERFACE java/sql/ResultSet.close()V itf
-LABEL L17
-GOTO L19
-LABEL L18
+LABEL L19
 ASTORE 13
 ALOAD 12
 ALOAD 13
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L19
+LABEL L20
 ALOAD 12
 ATHROW
-LABEL L20
+LABEL L21
+ALOAD 10
+IFNULL L27
+ALOAD 10
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+GOTO L27
+LABEL L22
+ASTORE 11
 ALOAD 10
 IFNULL L26
+LABEL L23
 ALOAD 10
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+LABEL L24
 GOTO L26
-LABEL L21
-ASTORE 11
-ALOAD 10
-IFNULL L25
-LABEL L22
-ALOAD 10
-INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-LABEL L23
-GOTO L25
-LABEL L24
+LABEL L25
 ASTORE 12
 ALOAD 11
 ALOAD 12
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L25
+LABEL L26
 ALOAD 11
 ATHROW
-LABEL L26
-GOTO L28
 LABEL L27
+GOTO L29
+LABEL L28
 ASTORE 10
 CONST Integer:1
 ISTORE 9
@@ -244,7 +245,7 @@
 CONST Integer:0
 ALOAD 10
 INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
-LABEL L28
+LABEL L29
 NEW local/lito381/server/NativeDeControlCatalog$Names
 DUP
 ALOAD 3
```

#### `method settings(Ljava/sql/Connection;Llocal/lito381/server/NativeDeControlCatalog$Reporter;)Llocal/lito381/server/NativeDeControlCatalog$Settings;`

```diff
--- staged
+++ formal
@@ -3,15 +3,14 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L6 L7 L10 java/lang/NumberFormatException
-TRY L8 L9 L10 java/lang/NumberFormatException
-TRY L3 L13 L14 java/lang/Exception
-TRY L17 L18 L19 java/lang/NumberFormatException
-TRY L2 L25 L26 java/lang/Throwable
-TRY L27 L28 L29 java/lang/Throwable
-TRY L1 L31 L32 java/lang/Throwable
-TRY L33 L34 L35 java/lang/Throwable
-TRY L0 L37 L38 java/lang/Exception
+TRY L6 L7 L8 java/lang/NumberFormatException
+TRY L3 L12 L13 java/lang/Exception
+TRY L16 L17 L18 java/lang/NumberFormatException
+TRY L2 L24 L25 java/lang/Throwable
+TRY L26 L27 L28 java/lang/Throwable
+TRY L1 L30 L31 java/lang/Throwable
+TRY L32 L33 L34 java/lang/Throwable
+TRY L0 L36 L37 java/lang/Exception
 NEW java/util/HashMap
 DUP
 INVOKESPECIAL java/util/HashMap.<init>()V
@@ -36,91 +35,89 @@
 LABEL L2
 ALOAD 7
 INVOKEINTERFACE java/sql/ResultSet.next()Z itf
-IFEQ L24
+IFEQ L23
 IINC var=4 incr=1
 ALOAD 7
 CONST str:"id"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ISTORE 10
+ISTORE 8
 ALOAD 7
 CONST str:"class_id"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ISTORE 11
+ISTORE 9
 ALOAD 7
 CONST str:"weapon_type"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ISTORE 12
+ISTORE 10
 ALOAD 7
 CONST str:"poly_list"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+ASTORE 11
+ALOAD 11
+CONST Integer:0
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKESTATIC java/util/List.of(Ljava/lang/Object;)Ljava/util/List; itf
+INVOKESTATIC local/lito381/server/NativeDeControlCatalog.tolerant(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
+ASTORE 12
+ALOAD 7
+CONST str:"doll_list"
 INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
 ASTORE 13
 ALOAD 13
-CONST Integer:0
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKESTATIC java/util/List.of(Ljava/lang/Object;)Ljava/util/List; itf
+INVOKESTATIC java/util/List.of()Ljava/util/List; itf
 INVOKESTATIC local/lito381/server/NativeDeControlCatalog.tolerant(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
 ASTORE 14
 ALOAD 7
-CONST str:"doll_list"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-ASTORE 15
-ALOAD 15
-INVOKESTATIC java/util/List.of()Ljava/util/List; itf
-INVOKESTATIC local/lito381/server/NativeDeControlCatalog.tolerant(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
-ASTORE 16
-ALOAD 7
 CONST str:"use_skill"
 INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
 CONST null
 INVOKESTATIC local/lito381/server/NativeDeControlCatalog.strictTokens(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
-ASTORE 17
+ASTORE 15
 ALOAD 7
 CONST str:"use_item_Gfx"
 INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
 INVOKESTATIC java/util/List.of()Ljava/util/List; itf
 INVOKESTATIC local/lito381/server/NativeDeControlCatalog.strictTokens(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
+ASTORE 16
+ALOAD 7
+CONST str:"De_pet"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+ASTORE 17
+NEW java/util/ArrayList
+DUP
+INVOKESPECIAL java/util/ArrayList.<init>()V
 ASTORE 18
-ALOAD 7
-CONST str:"De_pet"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-ASTORE 19
-NEW java/util/ArrayList
-DUP
-INVOKESPECIAL java/util/ArrayList.<init>()V
-ASTORE 20
-ALOAD 19
-IFNULL L15
-ALOAD 19
+ALOAD 17
+IFNULL L14
+ALOAD 17
 CONST str:"0"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L15
-ALOAD 19
+IFNE L14
+ALOAD 17
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L15
+IFNE L14
 LABEL L3
-ALOAD 19
+ALOAD 17
 CONST str:"\\s+"
 CONST str:""
 INVOKEVIRTUAL java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
-ASTORE 9
-ALOAD 9
+ASTORE 19
+ALOAD 19
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L13
+IFNE L12
 NEW java/util/StringTokenizer
 DUP
-ALOAD 9
+ALOAD 19
 CONST str:","
 INVOKESPECIAL java/util/StringTokenizer.<init>(Ljava/lang/String;Ljava/lang/String;)V
-ASTORE 8
+ASTORE 20
 CONST Integer:0
 ISTORE 21
 LABEL L4
-ALOAD 8
-CHECKCAST java/util/StringTokenizer
+ALOAD 20
 INVOKEVIRTUAL java/util/StringTokenizer.hasMoreTokens()Z
-IFEQ L12
-ALOAD 8
-CHECKCAST java/util/StringTokenizer
+IFEQ L11
+ALOAD 20
 INVOKEVIRTUAL java/util/StringTokenizer.nextToken()Ljava/lang/String;
 ASTORE 22
 ALOAD 22
@@ -139,7 +136,7 @@
 ALOAD 23
 INVOKEVIRTUAL java/util/StringTokenizer.countTokens()I
 CONST Integer:3
-IF_ICMPLT L11
+IF_ICMPLT L9
 LABEL L6
 ALOAD 23
 INVOKEVIRTUAL java/util/StringTokenizer.nextToken()Ljava/lang/String;
@@ -161,11 +158,8 @@
 ILOAD 25
 IFLE L7
 ILOAD 26
-IFGT L8
-LABEL L7
-GOTO L4
-LABEL L8
-ALOAD 20
+IFLE L7
+ALOAD 18
 NEW local/lito381/server/NativeDeInitializerKernel$PetInfo
 DUP
 ILOAD 24
@@ -175,67 +169,67 @@
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
 POP
 IINC var=21 incr=1
+LABEL L7
+GOTO L10
+LABEL L8
+ASTORE 24
+ALOAD 1
+CONST str:"pet-number"
+CONST Integer:0
+ALOAD 24
+INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
+GOTO L10
 LABEL L9
-GOTO L4
+ALOAD 1
+CONST str:"pet-short"
+CONST Integer:0
+CONST null
+INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
 LABEL L10
-ASTORE 24
-ALOAD 1
-CONST str:"pet-number"
-CONST Integer:0
-ALOAD 24
-INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
 GOTO L4
 LABEL L11
-ALOAD 1
-CONST str:"pet-short"
-CONST Integer:0
-CONST null
-INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
-GOTO L4
-LABEL L12
 ILOAD 21
-IFLE L13
+IFLE L12
 ALOAD 1
 CONST str:"pet-info"
 ILOAD 21
 CONST null
 INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
+LABEL L12
+GOTO L14
 LABEL L13
-GOTO L15
+ASTORE 19
+ALOAD 1
+CONST str:"pet-error"
+CONST Integer:0
+ALOAD 19
+INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
 LABEL L14
-ASTORE 21
-ALOAD 1
-CONST str:"pet-error"
-CONST Integer:0
-ALOAD 21
-INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
-LABEL L15
 ALOAD 7
 CONST str:"akk_skill"
 INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-DUP
-ASTORE 9
-IFNULL L21
-ALOAD 9
+ASTORE 19
+ALOAD 19
+IFNULL L20
+ALOAD 19
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L21
+IFNE L20
 NEW java/util/StringTokenizer
 DUP
-ALOAD 9
+ALOAD 19
 CONST str:","
 INVOKESPECIAL java/util/StringTokenizer.<init>(Ljava/lang/String;Ljava/lang/String;)V
 ASTORE 21
 NEW java/util/ArrayList
 DUP
 INVOKESPECIAL java/util/ArrayList.<init>()V
-ASTORE 8
-LABEL L16
+ASTORE 20
+LABEL L15
 ALOAD 21
 INVOKEVIRTUAL java/util/StringTokenizer.hasMoreTokens()Z
-IFEQ L20
-LABEL L17
-ALOAD 8
-CHECKCAST java/util/List
+IFEQ L19
+LABEL L16
+ALOAD 20
 ALOAD 21
 INVOKEVIRTUAL java/util/StringTokenizer.nextToken()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.trim()Ljava/lang/String;
@@ -243,118 +237,115 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
+LABEL L17
+GOTO L15
 LABEL L18
-GOTO L16
+ASTORE 22
+ALOAD 20
+CONST Integer:0
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
+POP
+GOTO L15
 LABEL L19
-ASTORE 22
-ALOAD 8
-CHECKCAST java/util/List
-CONST Integer:0
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
-POP
-GOTO L16
+GOTO L21
 LABEL L20
-GOTO L22
+CONST Integer:0
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKESTATIC java/util/List.of(Ljava/lang/Object;)Ljava/util/List; itf
+ASTORE 20
 LABEL L21
-CONST Integer:0
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKESTATIC java/util/List.of(Ljava/lang/Object;)Ljava/util/List; itf
-ASTORE 8
-LABEL L22
 NEW local/lito381/server/NativeDeControlCatalog$Definition
 DUP
 NEW local/lito381/server/NativeDeInitializerKernel$Setting
 DUP
+ILOAD 8
+ILOAD 9
 ILOAD 10
-ILOAD 11
-ILOAD 12
 INVOKESPECIAL local/lito381/server/NativeDeInitializerKernel$Setting.<init>(III)V
+ALOAD 11
+ALOAD 12
 ALOAD 13
 ALOAD 14
 ALOAD 15
 ALOAD 16
-ALOAD 17
 ALOAD 18
 ALOAD 20
-ALOAD 8
-CHECKCAST java/util/List
 INVOKESPECIAL local/lito381/server/NativeDeControlCatalog$Definition.<init>(Llocal/lito381/server/NativeDeInitializerKernel$Setting;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;)V
 ASTORE 21
 ALOAD 2
-ILOAD 11
-ILOAD 12
+ILOAD 9
+ILOAD 10
 INVOKEDYNAMIC makeConcatWithConstants(II)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"\u0001_\u0001"]
 ALOAD 21
 INVOKEVIRTUAL java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ILOAD 10
-IFGT L23
+ILOAD 8
+IFLE L22
+ALOAD 3
+ILOAD 8
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 21
+INVOKEVIRTUAL java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
+POP
+LABEL L22
 GOTO L2
 LABEL L23
-ALOAD 3
-ILOAD 10
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ALOAD 21
-INVOKEVIRTUAL java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
-POP
-GOTO L2
-LABEL L24
 ALOAD 1
 CONST str:"settings-info"
 ALOAD 2
 INVOKEVIRTUAL java/util/HashMap.size()I
 CONST null
 INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
+LABEL L24
+ALOAD 7
+IFNULL L30
+ALOAD 7
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+GOTO L30
 LABEL L25
-ALOAD 7
-IFNULL L31
+ASTORE 8
+ALOAD 7
+IFNULL L29
+LABEL L26
 ALOAD 7
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
-GOTO L31
-LABEL L26
-ASTORE 8
-ALOAD 7
-IFNULL L30
 LABEL L27
-ALOAD 7
-INVOKEINTERFACE java/sql/ResultSet.close()V itf
+GOTO L29
 LABEL L28
-GOTO L30
-LABEL L29
 ASTORE 9
 ALOAD 8
 ALOAD 9
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L30
+LABEL L29
 ALOAD 8
 ATHROW
+LABEL L30
+ALOAD 6
+IFNULL L36
+ALOAD 6
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+GOTO L36
 LABEL L31
-ALOAD 6
-IFNULL L37
+ASTORE 7
+ALOAD 6
+IFNULL L35
+LABEL L32
 ALOAD 6
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-GOTO L37
-LABEL L32
-ASTORE 7
-ALOAD 6
-IFNULL L36
 LABEL L33
-ALOAD 6
-INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+GOTO L35
 LABEL L34
-GOTO L36
-LABEL L35
 ASTORE 8
 ALOAD 7
 ALOAD 8
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L35
+ALOAD 7
+ATHROW
 LABEL L36
-ALOAD 7
-ATHROW
+GOTO L38
 LABEL L37
-GOTO L39
-LABEL L38
 ASTORE 6
 CONST Integer:1
 ISTORE 5
@@ -363,7 +354,7 @@
 CONST Integer:0
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeControlCatalog$Reporter.event(Ljava/lang/String;ILjava/lang/Exception;)V itf
-LABEL L39
+LABEL L38
 NEW local/lito381/server/NativeDeControlCatalog$Settings
 DUP
 ALOAD 2
```

### `NativeDeCorpseCore`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method run()V`

```diff
--- staged
+++ formal
@@ -3,14 +3,12 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L3 L4 L12 java/lang/RuntimeException
-TRY L6 L7 L12 java/lang/RuntimeException
-TRY L9 L10 L12 java/lang/RuntimeException
-TRY L3 L4 L15 java/lang/Throwable
-TRY L6 L7 L15 java/lang/Throwable
-TRY L9 L10 L15 java/lang/Throwable
-TRY L12 L13 L15 java/lang/Throwable
-TRY L15 L16 L15 java/lang/Throwable
+TRY L3 L4 L7 java/lang/RuntimeException
+TRY L5 L6 L7 java/lang/RuntimeException
+TRY L3 L4 L9 java/lang/Throwable
+TRY L5 L6 L9 java/lang/Throwable
+TRY L7 L8 L9 java/lang/Throwable
+TRY L9 L10 L9 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.auditDeletionStalls()V itf
@@ -31,7 +29,7 @@
 LABEL L1
 ALOAD 3
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L19
+IFEQ L13
 ALOAD 3
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeCorpseCore$Npc
@@ -61,43 +59,27 @@
 LLOAD 1
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.claim(Llocal/lito381/server/NativeDeCorpseCore$Npc;JLjava/lang/Object;)Z itf
-IFNE L9
+IFNE L5
 ALOAD 4
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Npc.destroyed()Z itf
-IFNE L6
+IFEQ L4
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeCorpseCore.tracked:Ljava/util/Set;
+ALOAD 4
+INVOKEINTERFACE java/util/Set.remove(Ljava/lang/Object;)Z itf
+POP
 LABEL L4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.retained(Ljava/lang/Object;)Z itf
-IFEQ L5
-GOTO L1
-LABEL L5
+IFNE L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.release(Ljava/lang/Object;)V itf
 GOTO L1
-LABEL L6
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeCorpseCore.tracked:Ljava/util/Set;
-ALOAD 4
-INVOKEINTERFACE java/util/Set.remove(Ljava/lang/Object;)Z itf
-POP
-LABEL L7
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
-ALOAD 5
-INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.retained(Ljava/lang/Object;)Z itf
-IFEQ L8
-GOTO L1
-LABEL L8
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
-ALOAD 5
-INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.release(Ljava/lang/Object;)V itf
-GOTO L1
-LABEL L9
+LABEL L5
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.tracked:Ljava/util/Set;
 ALOAD 4
@@ -116,20 +98,18 @@
 INVOKESPECIAL local/lito381/server/NativeDeCorpseCore$Task.<init>(Llocal/lito381/server/NativeDeCorpseCore;Llocal/lito381/server/NativeDeCorpseCore$Npc;Ljava/lang/Object;)V
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseCore.dispatch(Llocal/lito381/server/NativeDeCorpseCore$Task;I)V
-LABEL L10
+LABEL L6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.retained(Ljava/lang/Object;)Z itf
-IFEQ L11
-GOTO L1
-LABEL L11
+IFNE L12
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.release(Ljava/lang/Object;)V itf
-GOTO L18
-LABEL L12
+GOTO L12
+LABEL L7
 ASTORE 6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.deleteFailures:Ljava/util/concurrent/atomic/AtomicLong;
@@ -181,36 +161,33 @@
 ALOAD 6
 AASTORE
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.log(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V itf
-LABEL L13
+LABEL L8
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.retained(Ljava/lang/Object;)Z itf
-IFEQ L14
-GOTO L1
-LABEL L14
+IFNE L12
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.release(Ljava/lang/Object;)V itf
-GOTO L18
-LABEL L15
+GOTO L12
+LABEL L9
 ASTORE 9
-LABEL L16
+LABEL L10
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.retained(Ljava/lang/Object;)Z itf
-IFEQ L17
-GOTO L1
-LABEL L17
+IFNE L11
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseCore.port:Llocal/lito381/server/NativeDeCorpseCore$Port;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeCorpseCore$Port.release(Ljava/lang/Object;)V itf
+LABEL L11
 ALOAD 9
 ATHROW
-LABEL L18
+LABEL L12
 GOTO L1
-LABEL L19
+LABEL L13
 RETURN
```

### `NativeDeCorpseTimers$Job`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method run()V`

```diff
--- staged
+++ formal
@@ -17,20 +17,20 @@
 TRY L22 L23 L22 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Job.ticket:Llocal/lito381/server/NativeDeCorpseTimers$Ticket;
-ASTORE 2
-ALOAD 2
+ASTORE 1
+ALOAD 1
 DUP
 ASTORE 3
 MONITORENTER
 LABEL L0
-ALOAD 2
+ALOAD 1
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.closed:Z
 IFNE L1
-ALOAD 2
+ALOAD 1
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.job:Llocal/lito381/server/NativeDeCorpseTimers$Job;
 ALOAD 0
 IF_ACMPNE L1
-ALOAD 2
+ALOAD 1
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.owner:Llocal/lito381/server/NativeDeCorpseTimers$Owner;
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
@@ -41,18 +41,16 @@
 LABEL L1
 CONST Integer:0
 LABEL L2
-DUP
-ISTORE 1
-ISTORE 4
-ILOAD 1
+ISTORE 2
+ILOAD 2
 IFEQ L3
-ALOAD 2
+ALOAD 1
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.running:Z
-ALOAD 2
+ALOAD 1
 CONST Integer:3
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.phase:I
-ALOAD 2
+ALOAD 1
 CONST null
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.job:Llocal/lito381/server/NativeDeCorpseTimers$Job;
 LABEL L3
@@ -61,36 +59,36 @@
 LABEL L4
 GOTO L7
 LABEL L5
-ASTORE 5
+ASTORE 4
 ALOAD 3
 MONITOREXIT
 LABEL L6
-ALOAD 5
+ALOAD 4
 ATHROW
 LABEL L7
-ILOAD 1
+ILOAD 2
 IFNE L8
-ALOAD 2
+ALOAD 1
 CONST Integer:1
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
 RETURN
 LABEL L8
-ALOAD 2
+ALOAD 1
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.owner:Llocal/lito381/server/NativeDeCorpseTimers$Owner;
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Job.task:Llocal/lito381/server/NativeDeCorpseCore$Task;
 INVOKEVIRTUAL local/lito381/server/NativePlayerLifetime.executeCommitted(Ljava/lang/Runnable;)V
 LABEL L9
-ALOAD 2
+ALOAD 1
 DUP
 ASTORE 3
 MONITORENTER
 LABEL L10
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.running:Z
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.phase:I
 ALOAD 3
@@ -98,14 +96,14 @@
 LABEL L11
 GOTO L14
 LABEL L12
-ASTORE 6
+ASTORE 5
 ALOAD 3
 MONITOREXIT
 LABEL L13
-ALOAD 6
+ALOAD 5
 ATHROW
 LABEL L14
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
 GOTO L29
@@ -114,15 +112,15 @@
 ALOAD 3
 INVOKESTATIC local/lito381/server/CombatNpcDamageQueue.failed(Ljava/lang/Throwable;)V
 LABEL L16
-ALOAD 2
+ALOAD 1
 DUP
 ASTORE 3
 MONITORENTER
 LABEL L17
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.running:Z
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.phase:I
 ALOAD 3
@@ -130,47 +128,47 @@
 LABEL L18
 GOTO L21
 LABEL L19
-ASTORE 7
+ASTORE 6
 ALOAD 3
 MONITOREXIT
 LABEL L20
-ALOAD 7
+ALOAD 6
 ATHROW
 LABEL L21
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
 GOTO L29
 LABEL L22
+ASTORE 7
+LABEL L23
+ALOAD 1
+DUP
 ASTORE 8
-LABEL L23
-ALOAD 2
-DUP
-ASTORE 9
 MONITORENTER
 LABEL L24
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.running:Z
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.phase:I
-ALOAD 9
+ALOAD 8
 MONITOREXIT
 LABEL L25
 GOTO L28
 LABEL L26
-ASTORE 10
-ALOAD 9
+ASTORE 9
+ALOAD 8
 MONITOREXIT
 LABEL L27
-ALOAD 10
+ALOAD 9
 ATHROW
 LABEL L28
-ALOAD 2
+ALOAD 1
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
-ALOAD 8
+ALOAD 7
 ATHROW
 LABEL L29
 RETURN
```

### `NativeDeCorpseTimers$Owner$1`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method lambda$retry$#0(Llocal/lito381/server/NativeDeCorpseTimers$Ticket;JLlocal/lito381/server/NativePlayerLifetime;Ljava/lang/Runnable;)V`

```diff
--- staged
+++ formal
@@ -35,9 +35,7 @@
 LABEL L1
 CONST Integer:0
 LABEL L2
-DUP
 ISTORE 6
-ISTORE 8
 ILOAD 6
 IFEQ L3
 ALOAD 1
@@ -52,11 +50,11 @@
 LABEL L4
 GOTO L7
 LABEL L5
-ASTORE 9
+ASTORE 8
 ALOAD 7
 MONITOREXIT
 LABEL L6
-ALOAD 9
+ALOAD 8
 ATHROW
 LABEL L7
 ILOAD 6
```

#### `method retry(Llocal/lito381/server/NativeDeCorpseCore$Task;Ljava/lang/Runnable;J)Ljava/util/concurrent/ScheduledFuture;`

```diff
--- staged
+++ formal
@@ -7,20 +7,20 @@
 TRY L4 L5 L4 java/lang/Throwable
 TRY L9 L13 L14 java/lang/Throwable
 TRY L14 L15 L14 java/lang/Throwable
+TRY L6 L7 L19 java/lang/RuntimeException
 TRY L6 L7 L19 java/lang/Error
-TRY L6 L7 L19 java/lang/RuntimeException
+TRY L8 L18 L19 java/lang/RuntimeException
 TRY L8 L18 L19 java/lang/Error
-TRY L8 L18 L19 java/lang/RuntimeException
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeCorpseCore$Task.credit:Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeCorpseTimers$Ticket
-ASTORE 7
-ALOAD 7
+ASTORE 5
+ALOAD 5
 DUP
 ASTORE 8
 MONITORENTER
 LABEL L0
-ALOAD 7
+ALOAD 5
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.closed:Z
 IFNE L1
 ALOAD 0
@@ -33,7 +33,7 @@
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.OPEN:Llocal/lito381/server/NativePlayerLifetime$Mode;
 IF_ACMPEQ L2
 LABEL L1
-ALOAD 7
+ALOAD 5
 CONST Integer:1
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
 NEW java/util/concurrent/RejectedExecutionException
@@ -42,20 +42,20 @@
 INVOKESPECIAL java/util/concurrent/RejectedExecutionException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L2
-ALOAD 7
+ALOAD 5
 ALOAD 1
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.task:Llocal/lito381/server/NativeDeCorpseCore$Task;
-ALOAD 7
+ALOAD 5
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.phase:I
-ALOAD 7
+ALOAD 5
 DUP
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.epoch:J
 CONST Long:1
 LADD
 DUP2_X1
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.epoch:J
-LSTORE 5
+LSTORE 6
 ALOAD 8
 MONITOREXIT
 LABEL L3
@@ -72,8 +72,8 @@
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner$1.val$life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.retry:Ljava/util/concurrent/ScheduledThreadPoolExecutor;
 ALOAD 0
-ALOAD 7
-LLOAD 5
+ALOAD 5
+LLOAD 6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner$1.val$life:Llocal/lito381/server/NativePlayerLifetime;
 ALOAD 2
@@ -81,31 +81,31 @@
 LLOAD 3
 GETSTATIC java/util/concurrent/TimeUnit.MILLISECONDS:Ljava/util/concurrent/TimeUnit;
 INVOKEVIRTUAL java/util/concurrent/ScheduledThreadPoolExecutor.schedule(Ljava/lang/Runnable;JLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/ScheduledFuture;
-ASTORE 9
-ALOAD 9
+ASTORE 8
+ALOAD 8
 IFNONNULL L8
-ALOAD 7
+ALOAD 5
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
 CONST null
 LABEL L7
 ARETURN
 LABEL L8
-ALOAD 7
+ALOAD 5
 DUP
 ASTORE 10
 MONITORENTER
 LABEL L9
-ALOAD 7
+ALOAD 5
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.closed:Z
 IFNE L10
-ALOAD 7
+ALOAD 5
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.phase:I
 CONST Integer:1
 IF_ICMPNE L10
-ALOAD 7
+ALOAD 5
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.epoch:J
-LLOAD 5
+LLOAD 6
 LCMP
 IFNE L10
 CONST Integer:1
@@ -113,13 +113,11 @@
 LABEL L10
 CONST Integer:0
 LABEL L11
-DUP
-ISTORE 8
-ISTORE 11
-ILOAD 8
+ISTORE 9
+ILOAD 9
 IFEQ L12
-ALOAD 7
-ALOAD 9
+ALOAD 5
+ALOAD 8
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.future:Ljava/util/concurrent/ScheduledFuture;
 LABEL L12
 ALOAD 10
@@ -127,26 +125,26 @@
 LABEL L13
 GOTO L16
 LABEL L14
-ASTORE 12
+ASTORE 11
 ALOAD 10
 MONITOREXIT
 LABEL L15
-ALOAD 12
+ALOAD 11
 ATHROW
 LABEL L16
-ILOAD 8
+ILOAD 9
 IFNE L17
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner$1.this$0:Llocal/lito381/server/NativeDeCorpseTimers$Owner;
-ALOAD 9
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Owner.cancel(Ljava/util/concurrent/ScheduledFuture;)V
 LABEL L17
-ALOAD 9
+ALOAD 8
 LABEL L18
 ARETURN
 LABEL L19
 ASTORE 8
-ALOAD 7
+ALOAD 5
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
 ALOAD 8
```

#### `method tryExecute(Llocal/lito381/server/NativeDeCorpseCore$Task;)Z`

```diff
--- staged
+++ formal
@@ -11,8 +11,8 @@
 TRY L12 L13 L17 java/lang/Throwable
 TRY L14 L16 L17 java/lang/Throwable
 TRY L17 L18 L17 java/lang/Throwable
+TRY L9 L10 L20 java/lang/RuntimeException
 TRY L9 L10 L20 java/lang/Error
-TRY L9 L10 L20 java/lang/RuntimeException
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeCorpseCore$Task.credit:Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeCorpseTimers$Ticket
```

### `NativeDeCorpseTimers$Owner`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method close(Z)V`

```diff
--- staged
+++ formal
@@ -7,38 +7,37 @@
 TRY L2 L3 L2 java/lang/Throwable
 TRY L7 L11 L12 java/lang/Throwable
 TRY L12 L13 L12 java/lang/Throwable
-TRY L6 L15 L18 java/lang/Throwable
-TRY L16 L17 L18 java/lang/Throwable
+TRY L6 L15 L16 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
 DUP
-ASTORE 3
+ASTORE 2
 MONITORENTER
 LABEL L0
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Owner.closed:Z
-ALOAD 3
+ALOAD 2
 MONITOREXIT
 LABEL L1
 GOTO L4
 LABEL L2
-ASTORE 4
-ALOAD 3
+ASTORE 3
+ALOAD 2
 MONITOREXIT
 LABEL L3
-ALOAD 4
+ALOAD 3
 ATHROW
 LABEL L4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.ticker:Ljava/util/concurrent/atomic/AtomicReference;
 CONST null
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicReference.getAndSet(Ljava/lang/Object;)Ljava/lang/Object;
+CHECKCAST java/util/concurrent/ScheduledFuture
 ASTORE 2
 ALOAD 0
 ALOAD 2
-CHECKCAST java/util/concurrent/ScheduledFuture
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Owner.cancel(Ljava/util/concurrent/ScheduledFuture;)V
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.core:Llocal/lito381/server/NativeDeCorpseCore;
@@ -58,7 +57,7 @@
 LABEL L5
 ILOAD 5
 ILOAD 4
-IF_ICMPGE L20
+IF_ICMPGE L18
 ALOAD 3
 ILOAD 5
 AALOAD
@@ -98,23 +97,20 @@
 ATHROW
 LABEL L14
 ILOAD 7
-IFEQ L16
-LABEL L15
-GOTO L19
-LABEL L16
+IFNE L15
 ALOAD 6
 CONST Integer:1
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Ticket.close(Z)V
-LABEL L17
-GOTO L19
-LABEL L18
+LABEL L15
+GOTO L17
+LABEL L16
 ASTORE 7
 ALOAD 7
 INVOKESTATIC local/lito381/server/CombatNpcDamageQueue.failed(Ljava/lang/Throwable;)V
-LABEL L19
+LABEL L17
 IINC var=5 incr=1
 GOTO L5
-LABEL L20
+LABEL L18
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Owner.removeIfIdle()V
 RETURN
```

#### `method track(Llocal/lito381/server/NativeDeNpcVitalState;)V`

```diff
--- staged
+++ formal
@@ -3,64 +3,61 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L3 L6 L7 java/lang/Throwable
-TRY L7 L8 L7 java/lang/Throwable
+TRY L2 L5 L6 java/lang/Throwable
+TRY L6 L7 L6 java/lang/Throwable
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeCorpseState
-IFNE L0
-GOTO L1
-LABEL L0
+IFEQ L0
 ALOAD 1
 CHECKCAST local/lito381/server/NativeDeCorpseState
 ASTORE 2
 ALOAD 2
 GETFIELD local/lito381/server/NativeDeCorpseState.owner:Llocal/lito381/server/NativeDeCorpseTimers$Owner;
 ALOAD 0
-IF_ACMPNE L1
-GOTO L2
-LABEL L1
+IF_ACMPEQ L1
+LABEL L0
 NEW java/lang/IllegalArgumentException
 DUP
 CONST str:"corpse state belongs to another application owner"
 INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L2
+LABEL L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
 DUP
 ASTORE 3
 MONITORENTER
-LABEL L3
+LABEL L2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.closed:Z
-IFNE L4
+IFNE L3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.OPEN:Llocal/lito381/server/NativePlayerLifetime$Mode;
-IF_ACMPEQ L5
-LABEL L4
+IF_ACMPEQ L4
+LABEL L3
 NEW java/util/concurrent/RejectedExecutionException
 DUP
 CONST str:"corpse application admission closed"
 INVOKESPECIAL java/util/concurrent/RejectedExecutionException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L5
+LABEL L4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.core:Llocal/lito381/server/NativeDeCorpseCore;
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeCorpseCore.track(Llocal/lito381/server/NativeDeCorpseCore$Npc;)V
 ALOAD 3
 MONITOREXIT
+LABEL L5
+GOTO L8
 LABEL L6
-GOTO L9
-LABEL L7
 ASTORE 4
 ALOAD 3
 MONITOREXIT
-LABEL L8
+LABEL L7
 ALOAD 4
 ATHROW
-LABEL L9
+LABEL L8
 RETURN
```

### `NativeDeCorpseTimers$Ticket`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method close(Z)V`

```diff
--- staged
+++ formal
@@ -37,13 +37,13 @@
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.closed:Z
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.future:Ljava/util/concurrent/ScheduledFuture;
-ASTORE 3
+ASTORE 2
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.future:Ljava/util/concurrent/ScheduledFuture;
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.job:Llocal/lito381/server/NativeDeCorpseTimers$Job;
-ASTORE 2
+ASTORE 3
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.job:Llocal/lito381/server/NativeDeCorpseTimers$Job;
@@ -76,15 +76,15 @@
 LABEL L9
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.owner:Llocal/lito381/server/NativeDeCorpseTimers$Owner;
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Owner.cancel(Ljava/util/concurrent/ScheduledFuture;)V
 ALOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeDeCorpseTimers$Owner.cancel(Ljava/util/concurrent/ScheduledFuture;)V
-ALOAD 2
 IFNULL L10
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Ticket.owner:Llocal/lito381/server/NativeDeCorpseTimers$Owner;
 GETFIELD local/lito381/server/NativeDeCorpseTimers$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.workers:Ljava/util/concurrent/ThreadPoolExecutor;
-ALOAD 2
+ALOAD 3
 INVOKEVIRTUAL java/util/concurrent/ThreadPoolExecutor.remove(Ljava/lang/Runnable;)Z
 POP
 LABEL L10
```

### `NativeDeDeletionDeState`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,5 +9,5 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$DeWorld|local/lito381/server/NativeDeDeletionPorts|DeWorld|0x408; local/lito381/server/NativeDeDeletionPorts$Inventory|local/lito381/server/NativeDeDeletionPorts|Inventory|0x608; local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeDeletionPorts$Summon|local/lito381/server/NativeDeDeletionPorts|Summon|0x408; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18
 class recordComponents=
```

### `NativeDeDeletionPorts$Services`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,5 +9,5 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeDeletionPorts$Boss|local/lito381/server/NativeDeDeletionPorts|Boss|0x408; local/lito381/server/NativeDeDeletionPorts$DeWorld|local/lito381/server/NativeDeDeletionPorts|DeWorld|0x408; local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Quest|local/lito381/server/NativeDeDeletionPorts|Quest|0x408; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeDeletionPorts$World|local/lito381/server/NativeDeDeletionPorts|World|0x408; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8
+class innerClasses=local/lito381/server/NativeDeDeletionPorts$Boss|local/lito381/server/NativeDeDeletionPorts|Boss|0x408; local/lito381/server/NativeDeDeletionPorts$DeWorld|local/lito381/server/NativeDeDeletionPorts|DeWorld|0x408; local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Quest|local/lito381/server/NativeDeDeletionPorts|Quest|0x408; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeDeletionPorts$World|local/lito381/server/NativeDeDeletionPorts|World|0x408; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18
 class recordComponents=
```

### `NativeDeDeletionState`

正式 get_showId 是 package-private，但固定 classpath 的 NativeDeObjectFields 同簽名為 public；javac 明確拒絕縮小存取。暫存保留 public 以供編譯，非 SAME；其餘方法仍有分支、區域槽、例外表差異，尚未證明等價。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeCharacterFields$Snapshot|local/lito381/server/NativeDeCharacterFields|Snapshot|0x18; local/lito381/server/NativeDeCorpseTimers$Owner|local/lito381/server/NativeDeCorpseTimers|Owner|0x18; local/lito381/server/NativeDeDeletionPorts$Boss|local/lito381/server/NativeDeDeletionPorts|Boss|0x408; local/lito381/server/NativeDeDeletionPorts$DeadTimer|local/lito381/server/NativeDeDeletionPorts|DeadTimer|0x8; local/lito381/server/NativeDeDeletionPorts$DeleteTimer|local/lito381/server/NativeDeDeletionPorts|DeleteTimer|0x8; local/lito381/server/NativeDeDeletionPorts$DigestTimer|local/lito381/server/NativeDeDeletionPorts|DigestTimer|0x8; local/lito381/server/NativeDeDeletionPorts$Doll|local/lito381/server/NativeDeDeletionPorts|Doll|0x408; local/lito381/server/NativeDeDeletionPorts$Hate|local/lito381/server/NativeDeDeletionPorts|Hate|0x608; local/lito381/server/NativeDeDeletionPorts$HprTimer|local/lito381/server/NativeDeDeletionPorts|HprTimer|0x8; local/lito381/server/NativeDeDeletionPorts$Illusory|local/lito381/server/NativeDeDeletionPorts|Illusory|0x408; local/lito381/server/NativeDeDeletionPorts$Inventory|local/lito381/server/NativeDeDeletionPorts|Inventory|0x608; local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608; local/lito381/server/NativeDeDeletionPorts$MobGroup|local/lito381/server/NativeDeDeletionPorts|MobGroup|0x608; local/lito381/server/NativeDeDeletionPorts$Monster|local/lito381/server/NativeDeDeletionPorts|Monster|0x408; local/lito381/server/NativeDeDeletionPorts$MprTimer|local/lito381/server/NativeDeDeletionPorts|MprTimer|0x8; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$OtherList|local/lito381/server/NativeDeDeletionPorts|OtherList|0x608; local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8; local/lito381/server/NativeDeDeletionPorts$Quest|local/lito381/server/NativeDeDeletionPorts|Quest|0x408; local/lito381/server/NativeDeDeletionPorts$RemoveObject|local/lito381/server/NativeDeDeletionPorts|RemoveObject|0x18; local/lito381/server/NativeDeDeletionPorts$RestTimer|local/lito381/server/NativeDeDeletionPorts|RestTimer|0x8; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeDeletionPorts$Spawn|local/lito381/server/NativeDeDeletionPorts|Spawn|0x608; local/lito381/server/NativeDeDeletionPorts$Summon|local/lito381/server/NativeDeDeletionPorts|Summon|0x408; local/lito381/server/NativeDeDeletionPorts$Template|local/lito381/server/NativeDeDeletionPorts|Template|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeDeletionPorts$WorkTimer|local/lito381/server/NativeDeDeletionPorts|WorkTimer|0x8; local/lito381/server/NativeDeDeletionPorts$World|local/lito381/server/NativeDeDeletionPorts|World|0x408; local/lito381/server/NativeDeDeletionVisibility$Phase|local/lito381/server/NativeDeDeletionVisibility|Phase|0x4019; local/lito381/server/NativeDeDeletionVisibility$Snapshot|local/lito381/server/NativeDeDeletionVisibility|Snapshot|0x19; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeCharacterFields$Snapshot|local/lito381/server/NativeDeCharacterFields|Snapshot|0x18; local/lito381/server/NativeDeCorpseTimers$Owner|local/lito381/server/NativeDeCorpseTimers|Owner|0x18; local/lito381/server/NativeDeDeletionPorts$Hate|local/lito381/server/NativeDeDeletionPorts|Hate|0x608; local/lito381/server/NativeDeDeletionPorts$Inventory|local/lito381/server/NativeDeDeletionPorts|Inventory|0x608; local/lito381/server/NativeDeDeletionPorts$MobGroup|local/lito381/server/NativeDeDeletionPorts|MobGroup|0x608; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeDeletionPorts$Spawn|local/lito381/server/NativeDeDeletionPorts|Spawn|0x608; local/lito381/server/NativeDeDeletionPorts$Template|local/lito381/server/NativeDeDeletionPorts|Template|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeDeletionVisibility$Snapshot|local/lito381/server/NativeDeDeletionVisibility|Snapshot|0x19; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18
 class recordComponents=
 field _combatDeathCause:Z access=0x40
 field _combatDeathCause:Z signature=null
```

#### `method get_showId()I`

```diff
--- staged
+++ formal
@@ -1,4 +1,4 @@
-access=0x1
+access=0x0
 signature=null
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
```

#### `method sourceAbortFailedWorldRegistration()V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.require(Llocal/lito381/server/NativeDeDeletionState;)V
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
 CONST Integer:1
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.set(Z)V
 ALOAD 0
```

#### `method sourceAuditDeletionStalls()V`

```diff
--- staged
+++ formal
@@ -3,32 +3,40 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L3 L4 L5 java/lang/RuntimeException
-TRY L1 L7 L8 java/lang/RuntimeException
+TRY L9 L10 L11 java/lang/RuntimeException
+TRY L2 L3 L14 java/lang/RuntimeException
+TRY L4 L5 L14 java/lang/RuntimeException
+TRY L6 L7 L14 java/lang/RuntimeException
+TRY L8 L13 L14 java/lang/RuntimeException
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETIONS_IN_FLIGHT()Ljava/util/concurrent/ConcurrentHashMap;
 INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.isEmpty()Z
-IFNE L12
+IFEQ L0
+RETURN
+LABEL L0
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETIONS_IN_FLIGHT()Ljava/util/concurrent/ConcurrentHashMap;
 INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.values()Ljava/util/Collection;
 INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
 ASTORE 0
-LABEL L0
+LABEL L1
 ALOAD 0
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L12
+IFEQ L18
 ALOAD 0
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeDeletionState
 ASTORE 1
-LABEL L1
+LABEL L2
 ALOAD 1
-IFNULL L7
+IFNONNULL L4
+LABEL L3
+GOTO L1
+LABEL L4
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceDeletionVisibilitySnapshot()Llocal/lito381/server/NativeDeDeletionVisibility$Snapshot;
 ASTORE 2
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Snapshot.deletionInProgress()Z
-IFNE L2
+IFNE L6
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETIONS_IN_FLIGHT()Ljava/util/concurrent/ConcurrentHashMap;
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getId()I
@@ -36,8 +44,9 @@
 ALOAD 1
 INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.remove(Ljava/lang/Object;Ljava/lang/Object;)Z
 POP
-GOTO L7
-LABEL L2
+LABEL L5
+GOTO L1
+LABEL L6
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Snapshot.deleteAgeMillis()J
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETION_STALL_MILLIS()J
@@ -48,20 +57,23 @@
 CONST Integer:0
 CONST Integer:1
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.compareAndSet(ZZ)Z
-IFEQ L7
+IFNE L8
+LABEL L7
+GOTO L1
+LABEL L8
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETION_STALL_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 LSTORE 3
 CONST Integer:0
 ISTORE 5
-LABEL L3
+LABEL L9
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$World.get()Llocal/lito381/server/NativeDeDeletionPorts$World;
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.isObjectRegistered(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
 ISTORE 5
-LABEL L4
-GOTO L6
-LABEL L5
+LABEL L10
+GOTO L12
+LABEL L11
 ASTORE 6
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] deletion stall world-state read failed: objectId={} npcId={} phase={}"
@@ -75,7 +87,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Snapshot.phase()Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L6
+LABEL L12
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] deletion stalled: objectId={} npcId={} phase={} deleteAgeMs={} phaseAgeMs={} worldRegistered={} destroyed={} viewers={} map={} x={} y={} count={}"
 CONST Integer:12
@@ -151,9 +163,9 @@
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 AASTORE
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;[Ljava/lang/Object;)V itf
-LABEL L7
-GOTO L11
-LABEL L8
+LABEL L13
+GOTO L17
+LABEL L14
 ASTORE 2
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETION_AUDIT_FAILURE_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
@@ -161,19 +173,19 @@
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] deletion stall audit failed: objectId={} count={}"
 ALOAD 1
-IFNULL L9
+IFNULL L15
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getId()I
-GOTO L10
-LABEL L9
+GOTO L16
+LABEL L15
 CONST Integer:-1
-LABEL L10
+LABEL L16
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 LLOAD 3
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L11
-GOTO L0
-LABEL L12
+LABEL L17
+GOTO L1
+LABEL L18
 RETURN
```

#### `method sourceDeleteNpc()V`

```diff
--- staged
+++ formal
@@ -5,27 +5,27 @@
 default=
 TRY L3 L4 L5 java/lang/RuntimeException
 TRY L9 L10 L11 java/lang/RuntimeException
-TRY L13 L15 L20 java/lang/RuntimeException
-TRY L16 L19 L20 java/lang/RuntimeException
-TRY L21 L22 L23 java/lang/RuntimeException
-TRY L26 L28 L29 java/lang/RuntimeException
-TRY L31 L32 L33 java/lang/RuntimeException
-TRY L34 L37 L38 java/lang/RuntimeException
-TRY L39 L42 L43 java/lang/RuntimeException
-TRY L45 L46 L47 java/lang/Throwable
-TRY L47 L48 L47 java/lang/Throwable
-TRY L50 L51 L52 java/lang/Throwable
-TRY L52 L53 L52 java/lang/Throwable
-TRY L44 L55 L56 java/lang/RuntimeException
-TRY L58 L60 L61 java/lang/RuntimeException
-TRY L64 L65 L66 java/lang/RuntimeException
-TRY L71 L72 L73 java/lang/RuntimeException
-TRY L74 L83 L84 java/lang/Exception
-TRY L85 L86 L87 java/lang/RuntimeException
+TRY L13 L16 L21 java/lang/RuntimeException
+TRY L17 L20 L21 java/lang/RuntimeException
+TRY L22 L23 L24 java/lang/RuntimeException
+TRY L27 L29 L30 java/lang/RuntimeException
+TRY L32 L33 L34 java/lang/RuntimeException
+TRY L35 L38 L39 java/lang/RuntimeException
+TRY L40 L43 L44 java/lang/RuntimeException
+TRY L46 L47 L48 java/lang/Throwable
+TRY L48 L49 L48 java/lang/Throwable
+TRY L51 L52 L53 java/lang/Throwable
+TRY L53 L54 L53 java/lang/Throwable
+TRY L45 L56 L57 java/lang/RuntimeException
+TRY L59 L61 L62 java/lang/RuntimeException
+TRY L65 L66 L67 java/lang/RuntimeException
+TRY L72 L73 L74 java/lang/RuntimeException
+TRY L75 L84 L85 java/lang/Exception
+TRY L86 L87 L88 java/lang/RuntimeException
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.require(Llocal/lito381/server/NativeDeDeletionState;)V
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
 CONST Integer:0
 CONST Integer:1
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.compareAndSet(ZZ)Z
@@ -67,7 +67,7 @@
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
 LABEL L1
-GOTO L99
+RETURN
 LABEL L2
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceBeginDeletionObservation()V
@@ -140,51 +140,55 @@
 ASTORE 2
 LABEL L13
 ILOAD 1
-IFEQ L19
+IFEQ L20
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$World.get()Llocal/lito381/server/NativeDeDeletionPorts$World;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.getVisibleObjects(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/util/ArrayList;
+ASTORE 3
+ALOAD 3
 INVOKEVIRTUAL java/util/ArrayList.iterator()Ljava/util/Iterator;
-ASTORE 3
+ASTORE 4
 LABEL L14
-ALOAD 3
+ALOAD 4
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L19
-ALOAD 3
+IFEQ L20
+ALOAD 4
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$ObjectRef
-ASTORE 4
-ALOAD 4
+ASTORE 5
+ALOAD 5
 INSTANCEOF local/lito381/server/NativeDeDeletionState
-IFEQ L18
-ALOAD 4
+IFEQ L19
+ALOAD 5
 CHECKCAST local/lito381/server/NativeDeDeletionState
-ASTORE 5
-ALOAD 5
+ASTORE 6
+ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getNpcId()I
 ILOAD 1
-IF_ICMPNE L18
-ALOAD 5
+IF_ICMPEQ L15
+GOTO L14
+LABEL L15
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.isDead()Z
+IFNE L18
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.releasePairedCorpseClaim()V
+ALOAD 0
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getMaxHp()I
+INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.resurrect(I)V
+ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.isDead()Z
 IFNE L17
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.releasePairedCorpseClaim()V
-ALOAD 0
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getMaxHp()I
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.resurrect(I)V
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.isDead()Z
-IFNE L16
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceResetDeletionObservation()V
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
 CONST Integer:0
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.set(Z)V
-LABEL L15
+LABEL L16
 RETURN
-LABEL L16
+LABEL L17
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] paired resurrection failed; continuing corpse removal: objectId={} npcId={} pairedObjectId={}"
 ALOAD 0
@@ -193,23 +197,23 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getNpcId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ALOAD 5
+ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-GOTO L18
-LABEL L17
-ALOAD 5
-GETFIELD local/lito381/server/NativeDeDeletionState._destroyed:Z
-IFNE L18
-ALOAD 5
-ASTORE 2
 GOTO L19
 LABEL L18
+ALOAD 6
+GETFIELD local/lito381/server/NativeDeDeletionState._destroyed:Z
+IFNE L19
+ALOAD 6
+ASTORE 2
+GOTO L20
+LABEL L19
 GOTO L14
-LABEL L19
-GOTO L21
 LABEL L20
+GOTO L22
+LABEL L21
 ASTORE 3
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] paired NPC lookup failed; continuing deletion: objectId={} npcId={} targetNpcId={}"
@@ -223,12 +227,12 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 3
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L21
+LABEL L22
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.onDeleteClaimed()V
-LABEL L22
-GOTO L24
 LABEL L23
+GOTO L25
+LABEL L24
 ASTORE 3
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] subtype cleanup failed; continuing central deletion: objectId={} npcId={} type={}"
@@ -243,7 +247,7 @@
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
 ALOAD 3
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L24
+LABEL L25
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeDeletionState._destroyed:Z
@@ -266,7 +270,7 @@
 ASTORE 4
 ALOAD 4
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Monster
-IFEQ L25
+IFEQ L26
 ALOAD 4
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Monster
 ASTORE 3
@@ -274,7 +278,7 @@
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$HprTimer.remove(Llocal/lito381/server/NativeDeDeletionPorts$Monster;)V
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$MprTimer.remove(Llocal/lito381/server/NativeDeDeletionPorts$Monster;)V
-LABEL L25
+LABEL L26
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.cancelNpcChatTimer()V
 ALOAD 0
@@ -283,22 +287,22 @@
 ALOAD 0
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.VIEWERS_CAPTURED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceTransitionDeletion(Llocal/lito381/server/NativeDeDeletionVisibility$Phase;)V
-LABEL L26
+LABEL L27
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getInventory()Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
-IFNULL L27
+IFNULL L28
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getInventory()Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Inventory.clearItems()V itf
-LABEL L27
+LABEL L28
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.allTargetClear()V
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeDeletionState._master:Llocal/lito381/server/NativeDeCharacterFields;
-LABEL L28
-GOTO L30
 LABEL L29
+GOTO L31
+LABEL L30
 ASTORE 4
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] owned state cleanup failed: objectId={} npcId={}"
@@ -310,22 +314,22 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 4
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L30
+LABEL L31
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.get_showId()I
 ISTORE 4
-LABEL L31
+LABEL L32
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$Quest.get()Llocal/lito381/server/NativeDeDeletionPorts$Quest;
 ILOAD 4
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Quest.isQuest(I)Z
-IFEQ L32
+IFEQ L33
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$Quest.get()Llocal/lito381/server/NativeDeDeletionPorts$Quest;
 ILOAD 4
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Quest.remove(ILlocal/lito381/server/NativeDeDeletionState;)V
-LABEL L32
-GOTO L34
 LABEL L33
+GOTO L35
+LABEL L34
 ASTORE 5
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] quest registry cleanup failed: objectId={} npcId={} showId={}"
@@ -339,11 +343,11 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L34
+LABEL L35
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getDolls()Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.isEmpty()Z itf
-IFNE L37
+IFNE L38
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getDolls()Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
@@ -354,10 +358,10 @@
 ISTORE 6
 CONST Integer:0
 ISTORE 7
-LABEL L35
+LABEL L36
 ILOAD 7
 ILOAD 6
-IF_ICMPGE L37
+IF_ICMPGE L38
 ALOAD 5
 ILOAD 7
 AALOAD
@@ -366,15 +370,15 @@
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Doll
 ASTORE 9
 ALOAD 9
-IFNULL L36
+IFNULL L37
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Doll.deleteDoll()V
-LABEL L36
+LABEL L37
 IINC var=7 incr=1
-GOTO L35
-LABEL L37
-GOTO L39
+GOTO L36
 LABEL L38
+GOTO L40
+LABEL L39
 ASTORE 5
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] doll cleanup failed: objectId={} npcId={}"
@@ -386,11 +390,11 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L39
+LABEL L40
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getPetList()Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.isEmpty()Z itf
-IFNE L42
+IFNE L43
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getPetList()Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
@@ -401,10 +405,10 @@
 ISTORE 6
 CONST Integer:0
 ISTORE 7
-LABEL L40
+LABEL L41
 ILOAD 7
 ILOAD 6
-IF_ICMPGE L42
+IF_ICMPGE L43
 ALOAD 5
 ILOAD 7
 AALOAD
@@ -414,18 +418,18 @@
 ASTORE 9
 ALOAD 9
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Summon
-IFEQ L41
+IFEQ L42
 ALOAD 9
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Summon
 ASTORE 10
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Summon.deleteMe()V
-LABEL L41
+LABEL L42
 IINC var=7 incr=1
-GOTO L40
-LABEL L42
-GOTO L44
+GOTO L41
 LABEL L43
+GOTO L45
+LABEL L44
 ASTORE 5
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] summon cleanup failed: objectId={} npcId={}"
@@ -437,7 +441,7 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L44
+LABEL L45
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._hateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Hate.clear()V itf
@@ -447,50 +451,50 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
-LABEL L45
+LABEL L46
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._targetItemList:Ljava/util/List;
 INVOKEINTERFACE java/util/List.clear()V itf
-LABEL L46
+LABEL L47
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L49
-LABEL L47
+GOTO L50
+LABEL L48
 ASTORE 11
-LABEL L48
+LABEL L49
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 11
 ATHROW
-LABEL L49
+LABEL L50
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._digestItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
-LABEL L50
+LABEL L51
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._del_map:Ljava/util/HashMap;
 INVOKEVIRTUAL java/util/HashMap.clear()V
-LABEL L51
+LABEL L52
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._digestItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L54
-LABEL L52
+GOTO L55
+LABEL L53
 ASTORE 12
-LABEL L53
+LABEL L54
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._digestItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 12
 ATHROW
-LABEL L54
+LABEL L55
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$DigestTimer.remove(Llocal/lito381/server/NativeDeDeletionState;)V
-LABEL L55
-GOTO L57
 LABEL L56
+GOTO L58
+LABEL L57
 ASTORE 5
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] target state cleanup failed: objectId={} npcId={}"
@@ -502,34 +506,34 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L57
+LABEL L58
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$World.get()Llocal/lito381/server/NativeDeDeletionPorts$World;
 ASTORE 5
-LABEL L58
+LABEL L59
 ALOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.removeObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
 ALOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.isObjectRegistered(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IFEQ L59
+IFEQ L60
 ALOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.removeVisibleObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
 ALOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.removeObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-LABEL L59
+LABEL L60
 ALOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.isObjectRegistered(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IFNE L60
+IFNE L61
 ALOAD 0
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.WORLD_REMOVED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceTransitionDeletion(Llocal/lito381/server/NativeDeDeletionVisibility$Phase;)V
-LABEL L60
-GOTO L62
 LABEL L61
+GOTO L63
+LABEL L62
 ASTORE 6
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] world removal failed: objectId={} npcId={} map={} x={} y={}"
@@ -550,21 +554,21 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L62
+LABEL L63
 ALOAD 3
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 6
-LABEL L63
+LABEL L64
 ALOAD 6
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L70
+IFEQ L71
 ALOAD 6
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
 ASTORE 7
-LABEL L64
+LABEL L65
 ALOAD 7
-IFNULL L65
+IFNULL L66
 ALOAD 7
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Viewer.removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
@@ -577,7 +581,7 @@
 ALOAD 7
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Viewer.knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IFEQ L65
+IFEQ L66
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 LSTORE 8
@@ -595,9 +599,9 @@
 LLOAD 8
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L65
-GOTO L69
 LABEL L66
+GOTO L70
+LABEL L67
 ASTORE 8
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
@@ -611,21 +615,21 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getNpcId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 7
-IFNULL L67
+IFNULL L68
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Viewer.getId()I
-GOTO L68
-LABEL L67
+GOTO L69
+LABEL L68
 CONST Integer:-1
-LABEL L68
+LABEL L69
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 LLOAD 9
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 ALOAD 8
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L69
-GOTO L63
 LABEL L70
+GOTO L64
+LABEL L71
 ALOAD 0
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.REMOVE_PUBLISHED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceTransitionDeletion(Llocal/lito381/server/NativeDeDeletionVisibility$Phase;)V
@@ -635,13 +639,13 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.removeAllKnownObjects()V
 ALOAD 2
-IFNULL L74
-LABEL L71
+IFNULL L75
+LABEL L72
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.deleteMe()V
-LABEL L72
-GOTO L74
 LABEL L73
+GOTO L75
+LABEL L74
 ASTORE 6
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] paired NPC deletion failed: objectId={} pairedObjectId={}"
@@ -653,28 +657,28 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L74
+LABEL L75
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getNpcTemplate()Llocal/lito381/server/NativeDeDeletionPorts$Template;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Template.isBoss()Z itf
-IFEQ L83
+IFEQ L84
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceIsCombatDeathCause()Z
-IFEQ L75
+IFEQ L76
 CONST str:"COMBAT_CORPSE_CLEANUP"
-GOTO L77
-LABEL L75
+GOTO L78
+LABEL L76
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.isDead()Z
-IFEQ L76
+IFEQ L77
 CONST str:"NON_COMBAT_FORCED_DEAD_DELETE"
-GOTO L77
-LABEL L76
+GOTO L78
+LABEL L77
 CONST str:"NON_COMBAT_LIVE_DELETE"
-LABEL L77
+LABEL L78
 ASTORE 6
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.BOSS_DEATH_AUDIT_ENABLE()Z
-IFEQ L82
+IFEQ L83
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[BossDeathAudit] event=BOSS_REMOVE cause={} npcId={} name=\"{}\" nameId=\"{}\" objectId={} hp={}/{} dead={} mapId={} x={} y={} showId={} spawnTable={} spawnId={} spawnNumber={} timedExist={} remainingExistSeconds={}"
 CONST Integer:17
@@ -753,27 +757,27 @@
 CONST Integer:12
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
-IFNULL L78
+IFNULL L79
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Spawn.getSpawnConfigTable()Ljava/lang/String; itf
-GOTO L79
-LABEL L78
+GOTO L80
+LABEL L79
 CONST str:"none"
-LABEL L79
+LABEL L80
 AASTORE
 DUP
 CONST Integer:13
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
-IFNULL L80
+IFNULL L81
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Spawn.getId()I itf
-GOTO L81
-LABEL L80
+GOTO L82
+LABEL L81
 CONST Integer:-1
-LABEL L81
+LABEL L82
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 AASTORE
 DUP
@@ -795,41 +799,41 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 AASTORE
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.info(Ljava/lang/String;[Ljava/lang/Object;)V itf
-LABEL L82
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
-IFNULL L83
+LABEL L83
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
+IFNULL L84
 INVOKESTATIC local/lito381/server/NativeDeDeletionPorts$Boss.get()Llocal/lito381/server/NativeDeDeletionPorts$Boss;
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getNameId()Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Boss.onBossDeath(Llocal/lito381/server/NativeDeDeletionPorts$Spawn;Ljava/lang/String;)V
-LABEL L83
-GOTO L85
 LABEL L84
+GOTO L86
+LABEL L85
 ASTORE 6
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 ALOAD 6
 INVOKEVIRTUAL java/lang/Exception.getLocalizedMessage()Ljava/lang/String;
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-LABEL L85
+LABEL L86
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getMobGroupInfo()Llocal/lito381/server/NativeDeDeletionPorts$MobGroup;
 ASTORE 6
 ALOAD 6
-IFNULL L86
+IFNULL L87
 ALOAD 6
 ALOAD 0
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$MobGroup.removeMember(Llocal/lito381/server/NativeDeDeletionState;)I itf
-IFNE L86
+IFNE L87
 ALOAD 0
 CONST null
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.setMobGroupInfo(Llocal/lito381/server/NativeDeDeletionPorts$MobGroup;)V
-LABEL L86
-GOTO L88
 LABEL L87
+GOTO L89
+LABEL L88
 ASTORE 6
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope._log()Llocal/lito381/server/NativeDeDeletionPorts$Log;
 CONST str:"[NpcLifecycle] mob group cleanup failed: objectId={} npcId={}"
@@ -841,19 +845,19 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-LABEL L88
+LABEL L89
 ALOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$World.isObjectRegistered(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IFNE L89
+IFNE L90
 CONST Integer:1
-GOTO L90
-LABEL L89
+GOTO L91
+LABEL L90
 CONST Integer:0
-LABEL L90
+LABEL L91
 ISTORE 6
 ILOAD 6
-IFNE L95
+IFNE L96
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 LSTORE 7
@@ -870,24 +874,24 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
-IFNULL L91
+IFNULL L92
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Spawn.getSpawnConfigTable()Ljava/lang/String; itf
-GOTO L92
-LABEL L91
+GOTO L93
+LABEL L92
 CONST str:"none"
-LABEL L92
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
-IFNULL L93
+LABEL L93
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
+IFNULL L94
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Spawn.getId()I itf
-GOTO L94
-LABEL L93
+GOTO L95
+LABEL L94
 CONST Integer:-1
-LABEL L94
+LABEL L95
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawnNumber:I
@@ -895,22 +899,22 @@
 LLOAD 7
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-GOTO L99
-LABEL L95
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
-IFNULL L96
+RETURN
+LABEL L96
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
+IFNULL L97
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
 ALOAD 0
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Spawn.releaseActiveNpc(Llocal/lito381/server/NativeDeDeletionState;)V itf
-LABEL L96
+LABEL L97
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.isReSpawn()Z
-IFEQ L98
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
-IFNONNULL L97
+IFEQ L99
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
+IFNONNULL L98
 INVOKESTATIC local/lito381/server/NativeDeDeletionScope.DELETE_VERIFY_FAILURE_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 LSTORE 7
@@ -928,17 +932,16 @@
 LLOAD 7
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-GOTO L98
-LABEL L97
+GOTO L99
+LABEL L98
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawn:Llocal/lito381/server/NativeDeDeletionPorts$Spawn;
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._spawnNumber:I
 CONST Integer:0
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Spawn.executeSpawnTask(II)V itf
-LABEL L98
+LABEL L99
 ALOAD 0
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.TERMINAL:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceFinishDeletionObservation(Llocal/lito381/server/NativeDeDeletionVisibility$Phase;)V
-LABEL L99
 RETURN
```

#### `method sourceRegisterViewer(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Z`

```diff
--- staged
+++ formal
@@ -12,11 +12,18 @@
 LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._destroyed:Z
-IFNE L2
+IFNE L1
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.get()Z
-IFNE L2
+IFEQ L2
+LABEL L1
+INVOKESTATIC local/lito381/server/NativeDeDeletionScope.LATE_VIEWER_REJECTED_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
+INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
+POP2
+CONST Integer:0
+IRETURN
+LABEL L2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._registeredViewers:Ljava/util/concurrent/ConcurrentHashMap;
 ALOAD 1
@@ -27,14 +34,12 @@
 POP
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._destroyed:Z
-IFNE L1
+IFNE L3
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.get()Z
-IFNE L1
-CONST Integer:1
-IRETURN
-LABEL L1
+IFEQ L4
+LABEL L3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionState._registeredViewers:Ljava/util/concurrent/ConcurrentHashMap;
 ALOAD 1
@@ -48,9 +53,6 @@
 POP2
 CONST Integer:0
 IRETURN
-LABEL L2
-INVOKESTATIC local/lito381/server/NativeDeDeletionScope.LATE_VIEWER_REJECTED_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
-INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
-POP2
-CONST Integer:0
+LABEL L4
+CONST Integer:1
 IRETURN
```

### `NativeDeDeletionVisibility$1`

正式 switch-map 欄位保留 com.lineage 名稱，但實際 values/ordinal 使用 NativeDeDeletionVisibility.Phase；javac 依 enum 擁有者產生不同欄位名稱。需主控裁定如何表達／驗收，尚無本批例外；本 entry 其他差異也全部列出。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -11,7 +11,7 @@
 class annotations=|||
 class innerClasses=local/lito381/server/NativeDeDeletionVisibility$1|||0x1008; local/lito381/server/NativeDeDeletionVisibility$Phase|local/lito381/server/NativeDeDeletionVisibility|Phase|0x4019
 class recordComponents=
-field $SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I access=0x18
-field $SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I signature=null
-field $SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I value=
-field $SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I annotations=|||
+field $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I access=0x18
+field $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I signature=null
+field $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I value=
+field $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I annotations=|||
```

#### `method <clinit>()V`

```diff
--- staged
+++ formal
@@ -15,9 +15,9 @@
 INVOKESTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.values()[Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 ARRAYLENGTH
 NEWARRAY 10
-PUTSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+PUTSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 LABEL L0
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.DELETE_CLAIMED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:1
@@ -27,7 +27,7 @@
 LABEL L2
 ASTORE 0
 LABEL L3
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.DESTROYED_MARKED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:2
@@ -37,7 +37,7 @@
 LABEL L5
 ASTORE 0
 LABEL L6
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.VIEWERS_CAPTURED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:3
@@ -47,7 +47,7 @@
 LABEL L8
 ASTORE 0
 LABEL L9
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.WORLD_REMOVED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:4
@@ -57,8 +57,8 @@
 LABEL L11
 ASTORE 0
 LABEL L12
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.ACTIVE:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.REMOVE_PUBLISHED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:5
 IASTORE
@@ -67,8 +67,8 @@
 LABEL L14
 ASTORE 0
 LABEL L15
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.REMOVE_PUBLISHED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.ACTIVE:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:6
 IASTORE
@@ -77,7 +77,7 @@
 LABEL L17
 ASTORE 0
 LABEL L18
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.TERMINAL:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:7
@@ -87,7 +87,7 @@
 LABEL L20
 ASTORE 0
 LABEL L21
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.FAILED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:8
@@ -97,7 +97,7 @@
 LABEL L23
 ASTORE 0
 LABEL L24
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.REGISTRATION_ABORTED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:9
```

### `NativeDeDeletionVisibility$Snapshot`

正式 switch-map 欄位保留 com.lineage 名稱，但實際 values/ordinal 使用 NativeDeDeletionVisibility.Phase；javac 依 enum 擁有者產生不同欄位名稱。需主控裁定如何表達／驗收，尚無本批例外；本 entry 其他差異也全部列出。

#### `method deletionInProgress()Z`

```diff
--- staged
+++ formal
@@ -3,12 +3,12 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionVisibility$Snapshot._phase:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 IALOAD
-TABLESWITCH min=1 max=6 default=L1 labels=L0,L0,L0,L0,L1,L0
+TABLESWITCH min=1 max=5 default=L1 labels=L0,L0,L0,L0,L0
 LABEL L0
 CONST Integer:1
 GOTO L2
```

### `NativeDeDeletionVisibility`

正式 switch-map 欄位保留 com.lineage 名稱，但實際 values/ordinal 使用 NativeDeDeletionVisibility.Phase；javac 依 enum 擁有者產生不同欄位名稱。需主控裁定如何表達／驗收，尚無本批例外；本 entry 其他差異也全部列出。

#### `method classifyRepair(Llocal/lito381/server/NativeDeDeletionVisibility$Snapshot;ZJ)Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;`

```diff
--- staged
+++ formal
@@ -8,12 +8,12 @@
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.LEAK:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
 ARETURN
 LABEL L0
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Snapshot.phase()Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 IALOAD
-TABLESWITCH min=1 max=9 default=L1 labels=L2,L2,L2,L2,L5,L7,L7,L7,L7
+TABLESWITCH min=1 max=9 default=L1 labels=L2,L2,L2,L2,L7,L5,L7,L7,L7
 LABEL L1
 NEW java/lang/IncompatibleClassChangeError
 DUP
```

### `NativeDeFactoryControls`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=local/lito381/server/NativeDeFactoryControls$1,local/lito381/server/NativeDeFactoryControls$Snapshot
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/System$Logger|java/lang/System|Logger|0x609; java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Abilities|local/lito381/server/NativeDeControlCatalog|Abilities|0x18; local/lito381/server/NativeDeControlCatalog$Catalog|local/lito381/server/NativeDeControlCatalog|Catalog|0x18; local/lito381/server/NativeDeControlCatalog$Definition|local/lito381/server/NativeDeControlCatalog|Definition|0x18; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeControlCatalog$Reporter|local/lito381/server/NativeDeControlCatalog|Reporter|0x608; local/lito381/server/NativeDeControlCatalog$Settings|local/lito381/server/NativeDeControlCatalog|Settings|0x18; local/lito381/server/NativeDeFactoryControls$1|||0x0; local/lito381/server/NativeDeFactoryControls$Snapshot|local/lito381/server/NativeDeFactoryControls|Snapshot|0x18; local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18
+class innerClasses=java/lang/System$Logger|java/lang/System|Logger|0x609; java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeControlCatalog$Abilities|local/lito381/server/NativeDeControlCatalog|Abilities|0x18; local/lito381/server/NativeDeControlCatalog$Catalog|local/lito381/server/NativeDeControlCatalog|Catalog|0x18; local/lito381/server/NativeDeControlCatalog$Definition|local/lito381/server/NativeDeControlCatalog|Definition|0x18; local/lito381/server/NativeDeControlCatalog$Names|local/lito381/server/NativeDeControlCatalog|Names|0x18; local/lito381/server/NativeDeControlCatalog$Reporter|local/lito381/server/NativeDeControlCatalog|Reporter|0x608; local/lito381/server/NativeDeControlCatalog$Settings|local/lito381/server/NativeDeControlCatalog|Settings|0x18; local/lito381/server/NativeDeFactoryControls$1|||0x0; local/lito381/server/NativeDeFactoryControls$Snapshot|local/lito381/server/NativeDeFactoryControls|Snapshot|0x18; local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18
 class recordComponents=
 field LOG:Ljava/lang/System$Logger; access=0x1a
 field LOG:Ljava/lang/System$Logger; signature=null
```

### `NativeDeInitializerKernel$Actor`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,5 +9,5 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeInitializerKernel$Actor|local/lito381/server/NativeDeInitializerKernel|Actor|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem|local/lito381/server/NativeDeInitializerKernel|Emblem|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeInitializerKernel$Packet|local/lito381/server/NativeDeInitializerKernel|Packet|0x608; local/lito381/server/NativeDeWeaponKernel$Target|local/lito381/server/NativeDeWeaponKernel|Target|0x608
+class innerClasses=local/lito381/server/NativeDeInitializerKernel$Actor|local/lito381/server/NativeDeInitializerKernel|Actor|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem|local/lito381/server/NativeDeInitializerKernel|Emblem|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18; local/lito381/server/NativeDeInitializerKernel$Packet|local/lito381/server/NativeDeInitializerKernel|Packet|0x608; local/lito381/server/NativeDeWeaponKernel$Target|local/lito381/server/NativeDeWeaponKernel|Target|0x608
 class recordComponents=
```

### `NativeDeInitializerKernel$Services`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,5 +9,5 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Actor|local/lito381/server/NativeDeInitializerKernel|Actor|0x608; local/lito381/server/NativeDeInitializerKernel$Clan|local/lito381/server/NativeDeInitializerKernel|Clan|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem|local/lito381/server/NativeDeInitializerKernel|Emblem|0x18; local/lito381/server/NativeDeInitializerKernel$Log|local/lito381/server/NativeDeInitializerKernel|Log|0x608; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeInitializerKernel$Packet|local/lito381/server/NativeDeInitializerKernel|Packet|0x608; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Pet|local/lito381/server/NativeDeInitializerKernel|Pet|0x608; local/lito381/server/NativeDeInitializerKernel$Services|local/lito381/server/NativeDeInitializerKernel|Services|0x608; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18; local/lito381/server/NativeDeInitializerKernel$Template|local/lito381/server/NativeDeInitializerKernel|Template|0x18
+class innerClasses=local/lito381/server/NativeDeInitializerKernel$Ability|local/lito381/server/NativeDeInitializerKernel|Ability|0x18; local/lito381/server/NativeDeInitializerKernel$Actor|local/lito381/server/NativeDeInitializerKernel|Actor|0x608; local/lito381/server/NativeDeInitializerKernel$Clan|local/lito381/server/NativeDeInitializerKernel|Clan|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem|local/lito381/server/NativeDeInitializerKernel|Emblem|0x18; local/lito381/server/NativeDeInitializerKernel$Log|local/lito381/server/NativeDeInitializerKernel|Log|0x608; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18; local/lito381/server/NativeDeInitializerKernel$Packet|local/lito381/server/NativeDeInitializerKernel|Packet|0x608; local/lito381/server/NativeDeInitializerKernel$PetInfo|local/lito381/server/NativeDeInitializerKernel|PetInfo|0x18; local/lito381/server/NativeDeInitializerKernel$Pet|local/lito381/server/NativeDeInitializerKernel|Pet|0x608; local/lito381/server/NativeDeInitializerKernel$Services|local/lito381/server/NativeDeInitializerKernel|Services|0x608; local/lito381/server/NativeDeInitializerKernel$Setting|local/lito381/server/NativeDeInitializerKernel|Setting|0x18; local/lito381/server/NativeDeInitializerKernel$Template|local/lito381/server/NativeDeInitializerKernel|Template|0x18
 class recordComponents=
```

### `NativeDeInitializerKernel`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V`

```diff
--- staged
+++ formal
@@ -8,12 +8,12 @@
 TRY L1 L15 L16 java/lang/Exception
 TRY L28 L30 L31 java/lang/Throwable
 TRY L31 L32 L31 java/lang/Throwable
-TRY L37 L38 L39 java/lang/Throwable
-TRY L39 L40 L39 java/lang/Throwable
-TRY L0 L35 L62 java/lang/Exception
-TRY L36 L53 L62 java/lang/Exception
-TRY L54 L57 L62 java/lang/Exception
-TRY L58 L61 L62 java/lang/Exception
+TRY L42 L43 L44 java/lang/Throwable
+TRY L44 L45 L44 java/lang/Throwable
+TRY L0 L35 L65 java/lang/Exception
+TRY L36 L40 L65 java/lang/Exception
+TRY L41 L50 L65 java/lang/Exception
+TRY L51 L64 L65 java/lang/Exception
 LABEL L0
 CONST null
 ASTORE 5
@@ -349,12 +349,39 @@
 LABEL L36
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
-IFNULL L58
+IFNULL L37
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.trim()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L58
+IFEQ L41
+LABEL L37
+ALOAD 0
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.log()Llocal/lito381/server/NativeDeInitializerKernel$Log; itf
+CONST str:"[假人名稱驗證][DB名稱無效] deobjid={}, denamebig5={}, npcObjectId={}"
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_deobjid()I
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
+IFNONNULL L38
+CONST str:"<NULL>"
+GOTO L39
+LABEL L38
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
+LABEL L39
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getId()I itf
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
+ALOAD 1
+CONST Integer:1
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setInitFailed(Z)V itf
+LABEL L40
+RETURN
+LABEL L41
 ALOAD 1
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.set_deName(Llocal/lito381/server/NativeDeInitializerKernel$Name;)V itf
@@ -366,71 +393,103 @@
 ASTORE 8
 ALOAD 8
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
-LABEL L37
+LABEL L42
 ALOAD 7
 ALOAD 2
 INVOKEVIRTUAL java/util/ArrayList.contains(Ljava/lang/Object;)Z
-IFNE L38
+IFNE L43
 ALOAD 7
 ALOAD 2
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
 POP
-LABEL L38
+LABEL L43
 ALOAD 8
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L41
-LABEL L39
+GOTO L46
+LABEL L44
 ASTORE 16
-LABEL L40
+LABEL L45
 ALOAD 8
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 16
 ATHROW
-LABEL L41
+LABEL L46
 ALOAD 1
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setNameId(Ljava/lang/String;)V itf
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
-IFNULL L54
+IFNULL L47
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
 INVOKEVIRTUAL java/lang/String.trim()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L54
+IFNE L47
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFEQ L54
-ALOAD 5
-IFNULL L42
+IFNE L51
+LABEL L47
+ALOAD 0
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.log()Llocal/lito381/server/NativeDeInitializerKernel$Log; itf
+CONST str:"[假人名稱驗證][套用流程錯誤] deobjid={}, sourceName={}, npcNameId={}, npcObjectId={}"
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_deobjid()I
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
+IFNONNULL L48
+CONST str:"<NULL>"
+GOTO L49
+LABEL L48
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
+LABEL L49
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getId()I itf
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
+ALOAD 1
+CONST Integer:1
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setInitFailed(Z)V itf
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.removeFromDeNameList()V itf
+LABEL L50
+RETURN
+LABEL L51
+ALOAD 5
+IFNULL L52
 ALOAD 5
 GETFIELD local/lito381/server/NativeDeInitializerKernel$Setting.classId:I
 ISTORE 9
-GOTO L43
-LABEL L42
+GOTO L53
+LABEL L52
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_classId()I
 ISTORE 9
-LABEL L43
+LABEL L53
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_sex()I
 ISTORE 10
 ILOAD 10
 CONST Integer:1
-IF_ICMPLE L44
+IF_ICMPLE L54
 CONST Integer:1
 ISTORE 10
-LABEL L44
+LABEL L54
 ILOAD 9
 CONST Integer:6
-IF_ICMPLE L45
+IF_ICMPLE L55
 CONST Integer:0
 ISTORE 9
-LABEL L45
+LABEL L55
 GETSTATIC local/lito381/server/NativeDeInitializerKernel._class_list:[[I
 ILOAD 10
 AALOAD
@@ -442,7 +501,7 @@
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.getAbility(I)Llocal/lito381/server/NativeDeInitializerKernel$Ability; itf
 ASTORE 12
 ALOAD 12
-IFNULL L46
+IFNULL L56
 ALOAD 0
 CONST Integer:1
 ALOAD 12
@@ -589,9 +648,9 @@
 CONST Integer:3
 IDIV
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.addHitup(I)V itf
-LABEL L46
-ALOAD 5
-IFNULL L47
+LABEL L56
+ALOAD 5
+IFNULL L57
 ALOAD 5
 GETFIELD local/lito381/server/NativeDeInitializerKernel$Setting.weaponType:I
 ISTORE 13
@@ -602,13 +661,13 @@
 GETFIELD local/lito381/server/NativeDeInitializerKernel$Setting.id:I
 INVOKEDYNAMIC makeConcatWithConstants(II)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"DeInitializer: 強制鎖定武器類型=\u0001 (來自 Setting ID=\u0001)"]
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Log.debug(Ljava/lang/String;)V itf
-GOTO L48
-LABEL L47
+GOTO L58
+LABEL L57
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_weaponType()I
 ISTORE 13
 ILOAD 13
-IFNE L48
+IFNE L58
 ILOAD 9
 ALOAD 0
 DUP
@@ -617,21 +676,21 @@
 INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/NativeDeInitializerKernel$Services;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H9 local/lito381/server/NativeDeInitializerKernel$Services.nextBoolean()Z itf, type:()Z]
 INVOKESTATIC local/lito381/server/NativeDeWeaponKernel.random(ILjava/util/function/BooleanSupplier;)I
 ISTORE 13
-LABEL L48
+LABEL L58
 ILOAD 11
 ISTORE 14
 ILOAD 3
-IFEQ L49
+IFEQ L59
 ALOAD 0
 ILOAD 9
 ILOAD 13
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.getRandomPoly(II)I itf
 ISTORE 15
 ILOAD 15
-IFLE L49
+IFLE L59
 ILOAD 15
 ISTORE 14
-LABEL L49
+LABEL L59
 ALOAD 1
 ILOAD 9
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setClassId(I)V itf
@@ -674,7 +733,7 @@
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setAtkspeed(I)V itf
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_clanid()I
-IFEQ L50
+IFEQ L60
 ALOAD 1
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_clanid()I
@@ -685,7 +744,7 @@
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.clan(I)Llocal/lito381/server/NativeDeInitializerKernel$Clan; itf
 ASTORE 17
 ALOAD 17
-IFNULL L50
+IFNULL L60
 ALOAD 1
 ALOAD 17
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Clan.getClanName()Ljava/lang/String; itf
@@ -698,25 +757,26 @@
 ALOAD 1
 ALOAD 18
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.set_emblem(Llocal/lito381/server/NativeDeInitializerKernel$Emblem;)V itf
-LABEL L50
+LABEL L60
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.getTitle()Ljava/lang/String;
 ASTORE 17
 ALOAD 17
-IFNULL L51
+IFNULL L61
 ALOAD 17
 INVOKEVIRTUAL java/lang/String.trim()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L51
+IFEQ L62
+LABEL L61
+ALOAD 1
+CONST str:""
+INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setTitle(Ljava/lang/String;)V itf
+GOTO L63
+LABEL L62
 ALOAD 1
 ALOAD 17
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setTitle(Ljava/lang/String;)V itf
-GOTO L52
-LABEL L51
-ALOAD 1
-CONST str:""
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setTitle(Ljava/lang/String;)V itf
-LABEL L52
+LABEL L63
 ALOAD 1
 ALOAD 0
 ALOAD 1
@@ -725,93 +785,36 @@
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getCurrentWeapon()I itf
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.visual(II)Llocal/lito381/server/NativeDeInitializerKernel$Packet; itf
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.broadcastPacketAll(Llocal/lito381/server/NativeDeInitializerKernel$Packet;)V itf
-LABEL L53
-RETURN
-LABEL L54
-ALOAD 0
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.log()Llocal/lito381/server/NativeDeInitializerKernel$Log; itf
-CONST str:"[假人名稱驗證][套用流程錯誤] deobjid={}, sourceName={}, npcNameId={}, npcObjectId={}"
+LABEL L64
+GOTO L78
+LABEL L65
+ASTORE 5
+ALOAD 2
+IFNULL L66
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_deobjid()I
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+GOTO L67
+LABEL L66
+CONST Integer:0
+LABEL L67
+ISTORE 6
+ALOAD 2
+IFNULL L68
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
+GOTO L69
+LABEL L68
+CONST null
+LABEL L69
+ASTORE 7
+ALOAD 1
+IFNULL L70
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
-IFNONNULL L55
-CONST str:"<NULL>"
-GOTO L56
-LABEL L55
-ALOAD 1
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
-LABEL L56
-ALOAD 1
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getId()I itf
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-ALOAD 1
-CONST Integer:1
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setInitFailed(Z)V itf
-ALOAD 1
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.removeFromDeNameList()V itf
-LABEL L57
-RETURN
-LABEL L58
-ALOAD 0
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.log()Llocal/lito381/server/NativeDeInitializerKernel$Log; itf
-CONST str:"[假人名稱驗證][DB名稱無效] deobjid={}, denamebig5={}, npcObjectId={}"
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_deobjid()I
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
-IFNONNULL L59
-CONST str:"<NULL>"
-GOTO L60
-LABEL L59
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
-LABEL L60
-ALOAD 1
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getId()I itf
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-ALOAD 1
-CONST Integer:1
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setInitFailed(Z)V itf
-LABEL L61
-RETURN
-LABEL L62
-ASTORE 5
-ALOAD 2
-IFNULL L63
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_deobjid()I
-GOTO L64
-LABEL L63
-CONST Integer:0
-LABEL L64
-ISTORE 6
-ALOAD 2
-IFNULL L65
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
-GOTO L66
-LABEL L65
+GOTO L71
+LABEL L70
 CONST null
-LABEL L66
-ASTORE 7
-ALOAD 1
-IFNULL L67
-ALOAD 1
-INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getNameId()Ljava/lang/String; itf
-GOTO L68
-LABEL L67
-CONST null
-LABEL L68
+LABEL L71
 ASTORE 8
 ALOAD 0
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.log()Llocal/lito381/server/NativeDeInitializerKernel$Log; itf
@@ -819,38 +822,38 @@
 ILOAD 6
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 7
-IFNONNULL L69
+IFNONNULL L72
 CONST str:"<NULL>"
-GOTO L70
-LABEL L69
+GOTO L73
+LABEL L72
 ALOAD 7
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
-LABEL L70
-ALOAD 8
-IFNONNULL L71
+LABEL L73
+ALOAD 8
+IFNONNULL L74
 CONST str:"<NULL>"
-GOTO L72
-LABEL L71
+GOTO L75
+LABEL L74
 ALOAD 8
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[\u0001]"]
-LABEL L72
-ALOAD 1
-IFNULL L73
+LABEL L75
+ALOAD 1
+IFNULL L76
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getId()I itf
-GOTO L74
-LABEL L73
+GOTO L77
+LABEL L76
 CONST Integer:0
-LABEL L74
+LABEL L77
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Log.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
 ALOAD 1
-IFNULL L75
+IFNULL L78
 ALOAD 1
 CONST Integer:1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.setInitFailed(Z)V itf
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.removeFromDeNameList()V itf
-LABEL L75
+LABEL L78
 RETURN
```

#### `method initDoll(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;II)V`

```diff
--- staged
+++ formal
@@ -11,7 +11,8 @@
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.getRandomDoll(II)I itf
 ISTORE 4
 ILOAD 4
-IFLE L5
+IFGT L0
+RETURN
 LABEL L0
 ALOAD 0
 CONST Integer:71082
```

#### `method initPet(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;)V`

```diff
--- staged
+++ formal
@@ -3,8 +3,8 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L0 L1 L6 java/lang/Exception
-TRY L2 L5 L6 java/lang/Exception
+TRY L0 L1 L7 java/lang/Exception
+TRY L2 L6 L7 java/lang/Exception
 LABEL L0
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getClassId()I itf
@@ -34,14 +34,16 @@
 ILOAD 5
 ALOAD 4
 GETFIELD local/lito381/server/NativeDeInitializerKernel$PetInfo.count:I
-IF_ICMPGE L5
+IF_ICMPGE L6
 ALOAD 0
 ALOAD 4
 GETFIELD local/lito381/server/NativeDeInitializerKernel$PetInfo.npcId:I
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.template(I)Llocal/lito381/server/NativeDeInitializerKernel$Template; itf
 ASTORE 6
 ALOAD 6
-IFNULL L4
+IFNONNULL L4
+GOTO L5
+LABEL L4
 ALOAD 0
 ALOAD 6
 ALOAD 1
@@ -63,17 +65,17 @@
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Actor.getHeading()I itf
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Pet.setHeading(I)V itf
-LABEL L4
+LABEL L5
 IINC var=5 incr=1
 GOTO L3
-LABEL L5
-GOTO L7
 LABEL L6
+GOTO L8
+LABEL L7
 ASTORE 2
 ALOAD 0
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Services.log()Llocal/lito381/server/NativeDeInitializerKernel$Log; itf
 CONST str:"假人寵物 initPet 異常"
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/NativeDeInitializerKernel$Log.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-LABEL L7
+LABEL L8
 RETURN
```

### `NativeDeInstanceBroadcast`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,5 +9,5 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeInitializerKernel$Log|local/lito381/server/NativeDeInitializerKernel|Log|0x608; local/lito381/server/NativeDeInstanceScope$World|local/lito381/server/NativeDeInstanceScope|World|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 class recordComponents=
```

### `NativeDeInstanceName`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8
+class innerClasses=
 class recordComponents=
 field _clanid:I access=0x2
 field _clanid:I signature=null
```

### `NativeDeInstanceNames`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;`

```diff
--- staged
+++ formal
@@ -15,20 +15,22 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;
 GETFIELD local/lito381/server/NativeDeDeletionOwner.services:Llocal/lito381/server/NativeDeDeletionPorts$Services;
+ASTORE 3
+ALOAD 3
+INSTANCEOF local/lito381/server/NativeDeWorldContext
+IFEQ L2
+ALOAD 3
+CHECKCAST local/lito381/server/NativeDeWorldContext
 ASTORE 2
-ALOAD 2
-INSTANCEOF local/lito381/server/NativeDeWorldContext
-IFNE L2
+GOTO L3
+LABEL L2
 NEW java/lang/IllegalArgumentException
 DUP
 CONST str:"source mutable names require a concrete World"
 INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L2
+LABEL L3
 ALOAD 2
-CHECKCAST local/lito381/server/NativeDeWorldContext
-ASTORE 3
-ALOAD 3
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeWorldContext;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ARETURN
```

#### `method name(Llocal/lito381/server/NativeDeWorldContext;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;`

```diff
--- staged
+++ formal
@@ -60,26 +60,24 @@
 INVOKEVIRTUAL java/util/IdentityHashMap.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeInitializerKernel$Name
 ASTORE 5
-ALOAD 5
-ASTORE 6
 ALOAD 3
 MONITOREXIT
 LABEL L6
 ALOAD 2
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
-ALOAD 6
+ALOAD 5
 ARETURN
 LABEL L7
-ASTORE 7
+ASTORE 6
 ALOAD 3
 MONITOREXIT
 LABEL L8
-ALOAD 7
+ALOAD 6
 ATHROW
 LABEL L9
-ASTORE 8
+ASTORE 7
 LABEL L10
 ALOAD 2
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
-ALOAD 8
+ALOAD 7
 ATHROW
```

### `NativeDeInstanceScene`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method publish(Llocal/lito381/server/NativeDeInstanceState;Llocal/lito381/server/NativeDeInitializerKernel$Packet;)V`

```diff
--- staged
+++ formal
@@ -22,13 +22,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceScene$Visual.packet()Llocal/lito381/server/NativeDePerceptionCharVisualUpdate;
 GETFIELD local/lito381/server/NativeDePerceptionCharVisualUpdate.subject:Llocal/lito381/server/NativeDePerceptionState;
 ALOAD 1
-IF_ACMPNE L0
-ALOAD 1
-CONST Integer:-1
-ALOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceScene$Visual.packet()Llocal/lito381/server/NativeDePerceptionCharVisualUpdate;
-INVOKESTATIC local/lito381/server/NativeDeInstanceBroadcast.toVisiblePlayers(Llocal/lito381/server/NativeDeObjectFields;ILlocal/lito381/server/NativeDePerceptionPacket;)V
-GOTO L1
+IF_ACMPEQ L1
 LABEL L0
 NEW java/lang/IllegalArgumentException
 DUP
@@ -36,4 +30,9 @@
 INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L1
+ALOAD 1
+CONST Integer:-1
+ALOAD 3
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceScene$Visual.packet()Llocal/lito381/server/NativeDePerceptionCharVisualUpdate;
+INVOKESTATIC local/lito381/server/NativeDeInstanceBroadcast.toVisiblePlayers(Llocal/lito381/server/NativeDeObjectFields;ILlocal/lito381/server/NativeDePerceptionPacket;)V
 RETURN
```

### `NativeDeInstanceScope$World`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method forEachNearbyPlayer(Llocal/lito381/server/NativeDeObjectFields;ILlocal/lito381/server/NativeDeWorldPlayerFilter;Ljava/util/function/Consumer;)V`

```diff
--- staged
+++ formal
@@ -18,6 +18,14 @@
 ALOAD 7
 CHECKCAST local/lito381/server/NativeDeWorldContext
 ASTORE 6
+GOTO L1
+LABEL L0
+NEW java/lang/IllegalArgumentException
+DUP
+CONST str:"De broadcast requires the owning concrete World"
+INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
+ATHROW
+LABEL L1
 ALOAD 6
 CONST str:"forEachNearbyPlayer"
 CONST str:"(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;ILlocal/lito381/server/NativeDeWorldPlayerFilter;Ljava/util/function/Consumer;)V"
@@ -42,12 +50,4 @@
 AASTORE
 INVOKEVIRTUAL local/lito381/server/NativeDeWorldContext.call(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
-GOTO L1
-LABEL L0
-NEW java/lang/IllegalArgumentException
-DUP
-CONST str:"De broadcast requires the owning concrete World"
-INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
-ATHROW
-LABEL L1
 RETURN
```

### `NativeDeInstanceState$DeAiWorkerTask`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=local/lito381/server/NativeDeInstanceState$DeAiWorkerTask|local/lito381/server/NativeDeInstanceState|DeAiWorkerTask|0x12; local/lito381/server/NativeDeLiveAiScope$Pool$ShutdownDiscardable|local/lito381/server/NativeDeLiveAiScope$Pool|ShutdownDiscardable|0x608; local/lito381/server/NativeDeLiveAiScope$Pool|local/lito381/server/NativeDeLiveAiScope|Pool|0x18
+class innerClasses=local/lito381/server/NativeDeInstanceState$DeAiWorkerTask|local/lito381/server/NativeDeInstanceState|DeAiWorkerTask|0x12; local/lito381/server/NativeDeLiveAiScope$Pool$ShutdownDiscardable|local/lito381/server/NativeDeLiveAiScope$Pool|ShutdownDiscardable|0x609
 class recordComponents=
 field _background:Z access=0x12
 field _background:Z signature=null
```

### `NativeDeInstanceState`

正式建構子在 super 前寫 construction，4 個多載還會先呼叫 name；Java 17 拒絕。暫存將這些敘述移到 super 後，這會改變初始化／例外時序，**不是等價修復，不得落地或部署**。完整差異還包含 lambda 名稱／wrapper、同步、指令與例外範圍，不能只豁免 constructor。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=local/lito381/server/NativeDeInstanceState$DeAiWorkerTask,local/lito381/server/NativeDeInstanceState$DeRepathThrottle
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8; local/lito381/server/NativeDeDeletionPorts$RemoveObject|local/lito381/server/NativeDeDeletionPorts|RemoveObject|0x18; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeInitializerKernel$Actor|local/lito381/server/NativeDeInitializerKernel|Actor|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem|local/lito381/server/NativeDeInitializerKernel|Emblem|0x18; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeInitializerKernel$Packet|local/lito381/server/NativeDeInitializerKernel|Packet|0x608; local/lito381/server/NativeDeInstancePorts$Combat|local/lito381/server/NativeDeInstancePorts|Combat|0x608; local/lito381/server/NativeDeInstancePorts$Construction|local/lito381/server/NativeDeInstancePorts|Construction|0x18; local/lito381/server/NativeDeInstancePorts$Scene|local/lito381/server/NativeDeInstancePorts|Scene|0x608; local/lito381/server/NativeDeInstancePorts$Shop|local/lito381/server/NativeDeInstancePorts|Shop|0x608; local/lito381/server/NativeDeInstanceState$DeAiWorkerTask|local/lito381/server/NativeDeInstanceState|DeAiWorkerTask|0x12; local/lito381/server/NativeDeInstanceState$DeRepathThrottle|local/lito381/server/NativeDeInstanceState|DeRepathThrottle|0x19; local/lito381/server/NativeDeLiveAiPorts$Combat|local/lito381/server/NativeDeLiveAiPorts|Combat|0x608; local/lito381/server/NativeDeLiveAiPorts$Doll|local/lito381/server/NativeDeLiveAiPorts|Doll|0x608; local/lito381/server/NativeDeLiveAiPorts$Fishing|local/lito381/server/NativeDeLiveAiPorts|Fishing|0x608; local/lito381/server/NativeDeLiveAiScope$Clock|local/lito381/server/NativeDeLiveAiScope|Clock|0x18; local/lito381/server/NativeDeLiveAiScope$Manager|local/lito381/server/NativeDeLiveAiScope|Manager|0x18; local/lito381/server/NativeDeLiveAiScope$Pool|local/lito381/server/NativeDeLiveAiScope|Pool|0x18; local/lito381/server/NativeDeLiveAiScope$Sprites|local/lito381/server/NativeDeLiveAiScope|Sprites|0x18; local/lito381/server/NativeDeLiveAiScope$World|local/lito381/server/NativeDeLiveAiScope|World|0x18; local/lito381/server/NativeDeNpcConstructionPorts$Services|local/lito381/server/NativeDeNpcConstructionPorts|Services|0x608; local/lito381/server/NativeDeNpcTemplatePorts$Services|local/lito381/server/NativeDeNpcTemplatePorts|Services|0x608; local/lito381/server/NativeDePerceptionPorts$Fishing|local/lito381/server/NativeDePerceptionPorts|Fishing|0x608; local/lito381/server/NativeDePerceptionPorts$Poison|local/lito381/server/NativeDePerceptionPorts|Poison|0x408; local/lito381/server/NativeDePerceptionPorts$Services|local/lito381/server/NativeDePerceptionPorts|Services|0x608; local/lito381/server/NativeDePerceptionPorts$Shop|local/lito381/server/NativeDePerceptionPorts|Shop|0x608; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
+class innerClasses=local/lito381/server/NativeDeInstanceState$DeAiWorkerTask|local/lito381/server/NativeDeInstanceState|DeAiWorkerTask|0x12; local/lito381/server/NativeDeInstanceState$DeRepathThrottle|local/lito381/server/NativeDeInstanceState|DeRepathThrottle|0x19
 class recordComponents=
 field _autoBuffChance:I access=0x2
 field _autoBuffChance:I signature=null
```

#### `method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V`

```diff
--- staged
+++ formal
@@ -3,6 +3,9 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
+ALOAD 0
+ALOAD 1
+PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
@@ -18,9 +21,6 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
-ALOAD 0
-ALOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 NEW java/lang/Object
 DUP
@@ -58,7 +58,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 ALOAD 0
 INVOKESTATIC java/lang/System.currentTimeMillis()J
 PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
@@ -93,19 +93,19 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._clanid:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState._clan_name:Ljava/lang/String;
+PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
```

#### `method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V`

```diff
--- staged
+++ formal
@@ -3,6 +3,13 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
+ALOAD 1
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+ASTORE 3
+ALOAD 0
+ALOAD 1
+PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
@@ -18,13 +25,6 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
-ALOAD 1
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
-ASTORE 3
-ALOAD 0
-ALOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 NEW java/lang/Object
 DUP
@@ -62,7 +62,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 ALOAD 0
 INVOKESTATIC java/lang/System.currentTimeMillis()J
 PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
@@ -97,19 +97,19 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._clanid:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState._clan_name:Ljava/lang/String;
+PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
@@ -144,7 +144,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
 ALOAD 0
 ALOAD 3
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.startNpc()V
 RETURN
```

#### `method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V`

```diff
--- staged
+++ formal
@@ -3,6 +3,13 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
+ALOAD 1
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+ASTORE 3
+ALOAD 0
+ALOAD 1
+PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
@@ -18,13 +25,6 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
-ALOAD 1
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
-ASTORE 3
-ALOAD 0
-ALOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 NEW java/lang/Object
 DUP
@@ -62,7 +62,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 ALOAD 0
 INVOKESTATIC java/lang/System.currentTimeMillis()J
 PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
@@ -97,19 +97,19 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._clanid:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState._clan_name:Ljava/lang/String;
+PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
@@ -144,7 +144,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
 ALOAD 0
 ALOAD 3
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 ILOAD 4
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
```

#### `method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V`

```diff
--- staged
+++ formal
@@ -3,6 +3,13 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
+ALOAD 1
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+ASTORE 3
+ALOAD 0
+ALOAD 1
+PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
@@ -18,13 +25,6 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
-ALOAD 1
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
-ASTORE 3
-ALOAD 0
-ALOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 NEW java/lang/Object
 DUP
@@ -62,7 +62,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 ALOAD 0
 INVOKESTATIC java/lang/System.currentTimeMillis()J
 PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
@@ -97,19 +97,19 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._clanid:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState._clan_name:Ljava/lang/String;
+PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
@@ -144,7 +144,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
 ALOAD 0
 ALOAD 3
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 ILOAD 4
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
```

#### `method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V`

```diff
--- staged
+++ formal
@@ -3,6 +3,13 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
+ALOAD 1
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+ASTORE 3
+ALOAD 0
+ALOAD 1
+PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
@@ -18,13 +25,6 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
-ALOAD 1
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
-ASTORE 3
-ALOAD 0
-ALOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 0
 NEW java/lang/Object
 DUP
@@ -62,7 +62,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 ALOAD 0
 INVOKESTATIC java/lang/System.currentTimeMillis()J
 PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
@@ -97,19 +97,19 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._clanid:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState._clan_name:Ljava/lang/String;
+PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
 ALOAD 0
 CONST null
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
@@ -144,7 +144,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
 ALOAD 0
 ALOAD 3
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 ILOAD 4
 PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
```

#### `method addAc(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addAc(I)V
-RETURN
```

#### `method addBowDmgup(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addBowDmgup(I)V
-RETURN
```

#### `method addBowHitup(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addBowHitup(I)V
-RETURN
```

#### `method addCha(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addCha(I)V
-RETURN
```

#### `method addCon(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addCon(I)V
-RETURN
```

#### `method addDex(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addDex(I)V
-RETURN
```

#### `method addDmgup(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addDmgup(I)V
-RETURN
```

#### `method addEarth(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addEarth(I)V
-RETURN
```

#### `method addFire(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addFire(I)V
-RETURN
```

#### `method addHitup(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addHitup(I)V
-RETURN
```

#### `method addInt(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addInt(I)V
-RETURN
```

#### `method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-RETURN
```

#### `method addMaxHp(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addMaxHp(I)V
-RETURN
```

#### `method addMaxMp(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addMaxMp(I)V
-RETURN
```

#### `method addMr(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addMr(I)V
-RETURN
```

#### `method addRegistBlind(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistBlind(I)V
-RETURN
```

#### `method addRegistSleep(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistSleep(I)V
-RETURN
```

#### `method addRegistStone(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistStone(I)V
-RETURN
```

#### `method addRegistStun(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistStun(I)V
-RETURN
```

#### `method addRegistSustain(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistSustain(I)V
-RETURN
```

#### `method addSp(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addSp(I)V
-RETURN
```

#### `method addStr(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addStr(I)V
-RETURN
```

#### `method addStunSuccess(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addStunSuccess(I)V
-RETURN
```

#### `method addWater(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWater(I)V
-RETURN
```

#### `method addWeaponBreakStack()V`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWeaponBreakStack()V
-RETURN
```

#### `method addWind(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWind(I)V
-RETURN
```

#### `method addWis(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWis(I)V
-RETURN
```

#### `method add_regist_freeze(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.add_regist_freeze(I)V
-RETURN
```

#### `method beginAiTick(J)Z`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$beginAiTick$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#13(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
```

#### `method beginBackgroundTick(J)Z`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$beginBackgroundTick$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#14(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
```

#### `method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
```

#### `method calcSleepTime(II)I`

```diff
--- staged
+++ formal
@@ -7,7 +7,7 @@
 ALOAD 0
 ILOAD 1
 ILOAD 2
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;II)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$calcSleepTime$#0(II)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;II)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#2(Llocal/lito381/server/NativeDeInstanceState;II)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
```

#### `method checkNeedActive()Z`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$checkNeedActive$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#6(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
```

#### `method checkSpawnLocation()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$checkSpawnLocation$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#34(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method claimExpiredCorpseDeletion(J)Z`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.claimExpiredCorpseDeletion(J)Z
-IRETURN
```

#### `method completeAiStopLocked(J)Z`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$completeAiStopLocked$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#30(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
```

#### `method destroyed()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.destroyed()Z
-IRETURN
```

#### `method dispatchAiWork(JZ)V`

```diff
--- staged
+++ formal
@@ -7,7 +7,7 @@
 ALOAD 0
 LLOAD 1
 ILOAD 3
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$dispatchAiWork$#0(JZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#23(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method ensureAiScheduled()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$ensureAiScheduled$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#26(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method enterBackgroundMode(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$enterBackgroundMode$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#16(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method failAiTimer(JJ)V`

```diff
--- staged
+++ formal
@@ -7,7 +7,7 @@
 ALOAD 0
 LLOAD 1
 LLOAD 3
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$failAiTimer$#0(JJ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#22(Llocal/lito381/server/NativeDeInstanceState;JJ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method finishAiTick(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$finishAiTick$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#15(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method forward(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.forward(I)V
-RETURN
```

#### `method getAc()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getAc()I
-IRETURN
```

#### `method getAddAttrKind()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getAddAttrKind()I
-IRETURN
```

#### `method getAiType()I`

```diff
--- staged
+++ formal
@@ -4,5 +4,5 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+GETFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 IRETURN
```

#### `method getAtkspeed()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getAtkspeed()I
-IRETURN
```

#### `method getBowDmgup()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getBowDmgup()I
-IRETURN
```

#### `method getBowHitup()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getBowHitup()I
-IRETURN
```

#### `method getBraveSpeed()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getBraveSpeed()I
-IRETURN
```

#### `method getCha()S`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCha()S
-IRETURN
```

#### `method getChaLightSize()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getChaLightSize()I
-IRETURN
```

#### `method getClanid()I`

```diff
--- staged
+++ formal
@@ -4,5 +4,5 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._clanid:I
+GETFIELD local/lito381/server/NativeDePerceptionState._clanid:I
 IRETURN
```

#### `method getClanname()Ljava/lang/String;`

```diff
--- staged
+++ formal
@@ -4,5 +4,5 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._clan_name:Ljava/lang/String;
+GETFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
 ARETURN
```

#### `method getCon()S`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCon()S
-IRETURN
```

#### `method getCurrentHp()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCurrentHp()I
-IRETURN
```

#### `method getCurrentMp()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCurrentMp()I
-IRETURN
```

#### `method getDex()S`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getDex()S
-IRETURN
```

#### `method getDmgup()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getDmgup()I
-IRETURN
```

#### `method getDolls()Ljava/util/Map;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getDolls()Ljava/util/Map;
-ARETURN
```

#### `method getEarth()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getEarth()I
-IRETURN
```

#### `method getExp()J`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getExp()J
-LRETURN
```

#### `method getFire()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getFire()I
-IRETURN
```

#### `method getGfxId()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getGfxId()I
-IRETURN
```

#### `method getHeading()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getHeading()I
-IRETURN
```

#### `method getHitup()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getHitup()I
-IRETURN
```

#### `method getId()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getId()I
-IRETURN
```

#### `method getInt()S`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getInt()S
-IRETURN
```

#### `method getKarma()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getKarma()I
-IRETURN
```

#### `method getKnownObjects()Ljava/util/List;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getKnownObjects()Ljava/util/List;
-ARETURN
```

#### `method getKnownPlayers()Ljava/util/List;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getKnownPlayers()Ljava/util/List;
-ARETURN
```

#### `method getLawful()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLawful()I
-IRETURN
```

#### `method getLevel()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLevel()I
-IRETURN
```

#### `method getLightSize()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLightSize()I
-IRETURN
```

#### `method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D
-DRETURN
```

#### `method getLocation()Llocal/lito381/server/NativeDeWorldLocation;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLocation()Llocal/lito381/server/NativeDeWorldLocation;
-ARETURN
```

#### `method getMap()Llocal/lito381/server/NativeDeWorldMap;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMap()Llocal/lito381/server/NativeDeWorldMap;
-ARETURN
```

#### `method getMaxHp()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMaxHp()I
-IRETURN
```

#### `method getMaxMp()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMaxMp()I
-IRETURN
```

#### `method getMoveSpeed()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMoveSpeed()I
-IRETURN
```

#### `method getMr()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMr()I
-IRETURN
```

#### `method getName()Ljava/lang/String;`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$getName$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#0(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/String
 ARETURN
```

#### `method getNameId()Ljava/lang/String;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getNameId()Ljava/lang/String;
-ARETURN
```

#### `method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
-ARETURN
```

#### `method getOwnLightSize()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getOwnLightSize()I
-IRETURN
```

#### `method getPassispeed()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getPassispeed()I
-IRETURN
```

#### `method getPetList()Ljava/util/Map;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getPetList()Ljava/util/Map;
-ARETURN
```

#### `method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
-ARETURN
```

#### `method getRegistBlind()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistBlind()I
-IRETURN
```

#### `method getRegistFreeze()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistFreeze()I
-IRETURN
```

#### `method getRegistSleep()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistSleep()I
-IRETURN
```

#### `method getRegistStone()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistStone()I
-IRETURN
```

#### `method getRegistStun()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistStun()I
-IRETURN
```

#### `method getRegistSustain()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistSustain()I
-IRETURN
```

#### `method getSleepTime()I`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$getSleepTime$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#33(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
```

#### `method getSp()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getSp()I
-IRETURN
```

#### `method getStatus()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getStatus()I
-IRETURN
```

#### `method getStr()S`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getStr()S
-IRETURN
```

#### `method getStunSuccess()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getStunSuccess()I
-IRETURN
```

#### `method getTempCharGfx()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTempCharGfx()I
-IRETURN
```

#### `method getTempLawful()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTempLawful()I
-IRETURN
```

#### `method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-IRETURN
```

#### `method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-IRETURN
```

#### `method getTitle()Ljava/lang/String;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTitle()Ljava/lang/String;
-ARETURN
```

#### `method getTrueMr()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTrueMr()I
-IRETURN
```

#### `method getTrueSp()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTrueSp()I
-IRETURN
```

#### `method getWater()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWater()I
-IRETURN
```

#### `method getWeaponBreakStack()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWeaponBreakStack()I
-IRETURN
```

#### `method getWind()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWind()I
-IRETURN
```

#### `method getWis()S`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWis()S
-IRETURN
```

#### `method get_deName()Llocal/lito381/server/NativeDeInitializerKernel$Name;`

```diff
--- staged
+++ formal
@@ -4,5 +4,5 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+GETFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ARETURN
```

#### `method get_deadTimerTemp()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_deadTimerTemp()I
-IRETURN
```

#### `method get_fishX()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_fishX()I
-IRETURN
```

#### `method get_fishY()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_fishY()I
-IRETURN
```

#### `method get_showId()I`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_showId()I
-IRETURN
```

#### `method hasSkillEffect(I)Z`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.hasSkillEffect(I)Z
-IRETURN
```

#### `method isAgro()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAgro()Z
-IRETURN
```

#### `method isAgrocoi()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAgrocoi()Z
-IRETURN
```

#### `method isAgrososc()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAgrososc()Z
-IRETURN
```

#### `method isAiRunning()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAiRunning()Z
-IRETURN
```

#### `method isBusy()Z`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$isBusy$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#32(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
```

#### `method isDeAiChain(J)Z`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$isDeAiChain$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#12(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
```

#### `method isDead()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isDead()Z
-IRETURN
```

#### `method isFishing()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isFishing()Z
-IRETURN
```

#### `method isInvisble()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isInvisble()Z
-IRETURN
```

#### `method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z
-IRETURN
```

#### `method isParalyzed()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isParalyzed()Z
-IRETURN
```

#### `method isShop()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isShop()Z
-IRETURN
```

#### `method isSleeped()Z`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isSleeped()Z
-IRETURN
```

#### `method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IRETURN
```

#### `method lambda$beginAiTick$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$beginAiTick(J)Z
-INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
-ARETURN
```

#### `method lambda$beginBackgroundTick$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$beginBackgroundTick(J)Z
-INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
-ARETURN
```

#### `method lambda$calcSleepTime$#0(II)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,11 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-ILOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$calcSleepTime(II)I
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ARETURN
```

#### `method lambda$checkNeedActive$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$checkNeedActive()Z
-INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
-ARETURN
```

#### `method lambda$checkSpawnLocation$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$checkSpawnLocation()V
-CONST null
-ARETURN
```

#### `method lambda$completeAiStopLocked$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$completeAiStopLocked(J)Z
-INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
-ARETURN
```

#### `method lambda$dispatchAiWork$#0(JZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,11 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-ILOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$dispatchAiWork(JZ)V
-CONST null
-ARETURN
```

#### `method lambda$ensureAiScheduled$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$ensureAiScheduled()V
-CONST null
-ARETURN
```

#### `method lambda$enterBackgroundMode$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$enterBackgroundMode(J)V
-CONST null
-ARETURN
```

#### `method lambda$failAiTimer$#0(JJ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,11 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-LLOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$failAiTimer(JJ)V
-CONST null
-ARETURN
```

#### `method lambda$finishAiTick$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$finishAiTick(J)V
-CONST null
-ARETURN
```

#### `method lambda$getName$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$getName()Ljava/lang/String;
-ARETURN
```

#### `method lambda$getSleepTime$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$getSleepTime()I
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ARETURN
```

#### `method lambda$isBusy$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$isBusy()Z
-INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
-ARETURN
```

#### `method lambda$isDeAiChain$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$isDeAiChain(J)Z
-INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
-ARETURN
```

#### `method lambda$nativeAiScope$#0(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,8 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$getName()Ljava/lang/String;
+ARETURN
```

#### `method lambda$nativeAiScope$#1(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+ILOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$updateNextActionTime(I)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#10(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$runAiTick(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#11(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$normalizeDeSleepTime(J)I
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ARETURN
```

#### `method lambda$nativeAiScope$#12(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$isDeAiChain(J)Z
+INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
+ARETURN
```

#### `method lambda$nativeAiScope$#13(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$beginAiTick(J)Z
+INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
+ARETURN
```

#### `method lambda$nativeAiScope$#14(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$beginBackgroundTick(J)Z
+INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
+ARETURN
```

#### `method lambda$nativeAiScope$#15(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$finishAiTick(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#16(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$enterBackgroundMode(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#17(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$submitAiTick(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#18(Llocal/lito381/server/NativeDeInstanceState;JI)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+ILOAD 3
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture;
+ARETURN
```

#### `method lambda$nativeAiScope$#19(Llocal/lito381/server/NativeDeInstanceState;JIZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,11 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+ILOAD 3
+ILOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture;
+ARETURN
```

#### `method lambda$nativeAiScope$#2(Llocal/lito381/server/NativeDeInstanceState;II)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,11 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+ILOAD 1
+ILOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$calcSleepTime(II)I
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ARETURN
```

#### `method lambda$nativeAiScope$#20(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$nextAiTimerTokenLocked()J
+INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
+ARETURN
```

#### `method lambda$nativeAiScope$#21(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,12 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+LLOAD 3
+ILOAD 5
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$onAiTimer(JJZ)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#22(Llocal/lito381/server/NativeDeInstanceState;JJ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,11 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+LLOAD 3
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$failAiTimer(JJ)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#23(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,11 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+ILOAD 3
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$dispatchAiWork(JZ)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#24(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,11 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+ILOAD 3
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$nextAiRetryDelay(JZ)I
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ARETURN
```

#### `method lambda$nativeAiScope$#25(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$scheduleNextAiTick(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#26(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$ensureAiScheduled()V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#27(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$stopAI()V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#28(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+ILOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$setAiRunning(Z)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#29(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$stopAiOwner(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#3(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$wakeup()V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#30(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$completeAiStopLocked(J)Z
+INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
+ARETURN
```

#### `method lambda$nativeAiScope$#31(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$setNpcSpeed()V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#32(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$isBusy()Z
+INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
+ARETURN
```

#### `method lambda$nativeAiScope$#33(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$getSleepTime()I
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ARETURN
```

#### `method lambda$nativeAiScope$#34(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$checkSpawnLocation()V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#35(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,12 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+LLOAD 3
+ILOAD 5
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$lambda$scheduleAiTick$0(JJZ)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#4(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$wakeup(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#5(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$startAI()V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#6(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$checkNeedActive()Z
+INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
+ARETURN
```

#### `method lambda$nativeAiScope$#7(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$onBackgroundTick()V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#8(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,10 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+LLOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$runBackgroundTick(J)V
+CONST null
+ARETURN
```

#### `method lambda$nativeAiScope$#9(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -0,0 +1,9 @@
+access=0xa
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$run()V
+CONST null
+ARETURN
```

#### `method lambda$nextAiRetryDelay$#0(JZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,11 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-ILOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$nextAiRetryDelay(JZ)I
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ARETURN
```

#### `method lambda$nextAiTimerTokenLocked$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$nextAiTimerTokenLocked()J
-INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
-ARETURN
```

#### `method lambda$normalizeDeSleepTime$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$normalizeDeSleepTime(J)I
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ARETURN
```

#### `method lambda$onAiTimer$#0(JJZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,12 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-LLOAD 3
-ILOAD 5
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$onAiTimer(JJZ)V
-CONST null
-ARETURN
```

#### `method lambda$onBackgroundTick$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$onBackgroundTick()V
-CONST null
-ARETURN
```

#### `method lambda$run$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$run()V
-CONST null
-ARETURN
```

#### `method lambda$runAiTick$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$runAiTick(J)V
-CONST null
-ARETURN
```

#### `method lambda$runBackgroundTick$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$runBackgroundTick(J)V
-CONST null
-ARETURN
```

#### `method lambda$scheduleAiTick$#0(JIZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,11 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-ILOAD 3
-ILOAD 4
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture;
-ARETURN
```

#### `method lambda$scheduleAiTick$#0(JJZ)V`

```diff
--- staged
+++ formal
@@ -0,0 +1,14 @@
+access=0x2
+signature=null
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+default=
+ALOAD 0
+ALOAD 0
+LLOAD 1
+LLOAD 3
+ILOAD 5
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#35(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
+POP
+RETURN
```

#### `method lambda$scheduleAiTick$#1(JI)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-ILOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture;
-ARETURN
```

#### `method lambda$scheduleNextAiTick$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$scheduleNextAiTick(J)V
-CONST null
-ARETURN
```

#### `method lambda$setAiRunning$#0(Z)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$setAiRunning(Z)V
-CONST null
-ARETURN
```

#### `method lambda$setDead$#0(Z)V`

```diff
--- staged
+++ formal
@@ -16,7 +16,7 @@
 ISTORE 2
 ALOAD 0
 ILOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceNpcSetDead(Z)V
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.sourceNpcSetDead(Z)V
 ILOAD 2
 IFEQ L2
 ALOAD 0
```

#### `method lambda$setNpcSpeed$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$setNpcSpeed()V
-CONST null
-ARETURN
```

#### `method lambda$sourceAi$scheduleAiTick$#0(JJZ)V`

```diff
--- staged
+++ formal
@@ -1,14 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 0
-LLOAD 1
-LLOAD 3
-ILOAD 5
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$sourceAi$scheduleAiTick$#1(JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
-INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
-POP
-RETURN
```

#### `method lambda$sourceAi$scheduleAiTick$#1(JJZ)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,12 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-LLOAD 3
-ILOAD 5
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$lambda$scheduleAiTick$0(JJZ)V
-CONST null
-ARETURN
```

#### `method lambda$startAI$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$startAI()V
-CONST null
-ARETURN
```

#### `method lambda$stopAI$#0()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$stopAI()V
-CONST null
-ARETURN
```

#### `method lambda$stopAiOwner$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$stopAiOwner(J)V
-CONST null
-ARETURN
```

#### `method lambda$submitAiTick$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$submitAiTick(J)V
-CONST null
-ARETURN
```

#### `method lambda$updateNextActionTime$#0(I)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$updateNextActionTime(I)V
-CONST null
-ARETURN
```

#### `method lambda$wakeup$#0(J)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$wakeup(J)V
-CONST null
-ARETURN
```

#### `method lambda$wakeup$#1()Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x2
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$wakeup()V
-CONST null
-ARETURN
```

#### `method nextAiRetryDelay(JZ)I`

```diff
--- staged
+++ formal
@@ -7,7 +7,7 @@
 ALOAD 0
 LLOAD 1
 ILOAD 3
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$nextAiRetryDelay$#0(JZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#24(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
```

#### `method nextAiTimerTokenLocked()J`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$nextAiTimerTokenLocked$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#20(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
```

#### `method normalizeDeSleepTime(J)I`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$normalizeDeSleepTime$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#11(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
```

#### `method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
```

#### `method onAiTimer(JJZ)V`

```diff
--- staged
+++ formal
@@ -8,7 +8,7 @@
 LLOAD 1
 LLOAD 3
 ILOAD 5
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$onAiTimer$#0(JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#21(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method onBackgroundTick()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$onBackgroundTick$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#7(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
```

#### `method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
```

#### `method rebindWorldMap(S)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.rebindWorldMap(S)V
-RETURN
```

#### `method removeAllKnownObjects()V`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.removeAllKnownObjects()V
-RETURN
```

#### `method removeFromDeNameList()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 TRY L0 L1 L2 java/lang/Throwable
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+GETFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 IFNULL L3
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeInstanceInitialization.namesLock(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/concurrent/locks/ReentrantLock;
@@ -14,7 +14,7 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeInstanceInitialization.names(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/ArrayList;
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+GETFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 INVOKEVIRTUAL java/util/ArrayList.remove(Ljava/lang/Object;)Z
 POP
 LABEL L1
```

#### `method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-RETURN
```

#### `method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IRETURN
```

#### `method resurrect(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.resurrect(I)V
-RETURN
```

#### `method run()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$run$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#9(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method runAiTick(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$runAiTick$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#10(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method runBackgroundTick(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$runBackgroundTick$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#8(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture;`

```diff
--- staged
+++ formal
@@ -7,7 +7,7 @@
 ALOAD 0
 LLOAD 1
 ILOAD 3
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JI)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$scheduleAiTick$#1(JI)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JI)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#18(Llocal/lito381/server/NativeDeInstanceState;JI)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/util/concurrent/ScheduledFuture
 ARETURN
```

#### `method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture;`

```diff
--- staged
+++ formal
@@ -8,7 +8,7 @@
 LLOAD 1
 ILOAD 3
 ILOAD 4
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JIZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$scheduleAiTick$#0(JIZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JIZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#19(Llocal/lito381/server/NativeDeInstanceState;JIZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 CHECKCAST java/util/concurrent/ScheduledFuture
 ARETURN
```

#### `method scheduleNextAiTick(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$scheduleNextAiTick$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#25(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method setAc(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAc(I)V
-RETURN
```

#### `method setAddAttrKind(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAddAttrKind(I)V
-RETURN
```

#### `method setAgro(Z)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAgro(Z)V
-RETURN
```

#### `method setAgrocoi(Z)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAgrocoi(Z)V
-RETURN
```

#### `method setAgrososc(Z)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAgrososc(Z)V
-RETURN
```

#### `method setAiRunning(Z)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 ILOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$setAiRunning$#0(Z)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#28(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method setAiType(I)V`

```diff
--- staged
+++ formal
@@ -5,5 +5,5 @@
 default=
 ALOAD 0
 ILOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 RETURN
```

#### `method setAtkspeed(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAtkspeed(I)V
-RETURN
```

#### `method setBraveSpeed(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setBraveSpeed(I)V
-RETURN
```

#### `method setCha(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setCha(I)V
-RETURN
```

#### `method setChaLightSize(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setChaLightSize(I)V
-RETURN
```

#### `method setCon(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setCon(I)V
-RETURN
```

#### `method setDex(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setDex(I)V
-RETURN
```

#### `method setExp(J)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-LLOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setExp(J)V
-RETURN
```

#### `method setGfxId(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setGfxId(I)V
-RETURN
```

#### `method setGfxidInStatus(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setGfxidInStatus(I)V
-RETURN
```

#### `method setHeading(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setHeading(I)V
-RETURN
```

#### `method setId(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setId(I)V
-RETURN
```

#### `method setInt(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setInt(I)V
-RETURN
```

#### `method setKarma(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setKarma(I)V
-RETURN
```

#### `method setLawful(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLawful(I)V
-RETURN
```

#### `method setLevel(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLevel(I)V
-RETURN
```

#### `method setLightSize(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLightSize(I)V
-RETURN
```

#### `method setLocation(III)V`

```diff
--- staged
+++ formal
@@ -1,11 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-ILOAD 2
-ILOAD 3
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLocation(III)V
-RETURN
```

#### `method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
-RETURN
```

#### `method setMap(Llocal/lito381/server/NativeDeWorldMap;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMap(Llocal/lito381/server/NativeDeWorldMap;)V
-RETURN
```

#### `method setMap(S)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMap(S)V
-RETURN
```

#### `method setMaxHp(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMaxHp(I)V
-RETURN
```

#### `method setMaxMp(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMaxMp(I)V
-RETURN
```

#### `method setMoveSpeed(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMoveSpeed(I)V
-RETURN
```

#### `method setMr(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMr(I)V
-RETURN
```

#### `method setName(Ljava/lang/String;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setName(Ljava/lang/String;)V
-RETURN
```

#### `method setNameId(Ljava/lang/String;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setNameId(Ljava/lang/String;)V
-RETURN
```

#### `method setNpcSpeed()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$setNpcSpeed$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#31(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method setOwnLightSize(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setOwnLightSize(I)V
-RETURN
```

#### `method setPassispeed(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setPassispeed(I)V
-RETURN
```

#### `method setPickupItem(Z)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setPickupItem(Z)V
-RETURN
```

#### `method setStatus(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setStatus(I)V
-RETURN
```

#### `method setStr(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setStr(I)V
-RETURN
```

#### `method setTempCharGfx(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setTempCharGfx(I)V
-RETURN
```

#### `method setTempLawful(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setTempLawful(I)V
-RETURN
```

#### `method setTitle(Ljava/lang/String;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setTitle(Ljava/lang/String;)V
-RETURN
```

#### `method setWeaponBreakStack(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setWeaponBreakStack(I)V
-RETURN
```

#### `method setWis(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setWis(I)V
-RETURN
```

#### `method setX(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setX(I)V
-RETURN
```

#### `method setXY(II)V`

```diff
--- staged
+++ formal
@@ -1,10 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-ILOAD 2
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setXY(II)V
-RETURN
```

#### `method setY(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setY(I)V
-RETURN
```

#### `method set_clanid(I)V`

```diff
--- staged
+++ formal
@@ -5,5 +5,5 @@
 default=
 ALOAD 0
 ILOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState._clanid:I
+PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
 RETURN
```

#### `method set_clanname(Ljava/lang/String;)V`

```diff
--- staged
+++ formal
@@ -5,5 +5,5 @@
 default=
 ALOAD 0
 ALOAD 1
-PUTFIELD local/lito381/server/NativeDeInstanceState._clan_name:Ljava/lang/String;
+PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
 RETURN
```

#### `method set_deName(Llocal/lito381/server/NativeDeInitializerKernel$Name;)V`

```diff
--- staged
+++ formal
@@ -4,11 +4,11 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 ALOAD 0
-ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
-DUP
 ASTORE 1
-PUTFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+ALOAD 0
+ALOAD 1
+PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 RETURN
```

#### `method set_deadTimerTemp(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.set_deadTimerTemp(I)V
-RETURN
```

#### `method set_showId(I)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.set_showId(I)V
-RETURN
```

#### `method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
-RETURN
```

#### `method sourceAi$beginAiTick(J)Z`

```diff
--- staged
+++ formal
@@ -8,10 +8,8 @@
 TRY L5 L6 L5 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 3
-ALOAD 3
-DUP
-ASTORE 4
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -42,7 +40,7 @@
 IFEQ L3
 LABEL L1
 CONST Integer:0
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L2
 IRETURN
@@ -57,14 +55,14 @@
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 CONST Integer:1
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L4
 IRETURN
 LABEL L5
-ASTORE 5
-ALOAD 4
+ASTORE 4
+ALOAD 3
 MONITOREXIT
 LABEL L6
-ALOAD 5
+ALOAD 4
 ATHROW
```

#### `method sourceAi$beginBackgroundTick(J)Z`

```diff
--- staged
+++ formal
@@ -8,10 +8,8 @@
 TRY L5 L6 L5 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 3
-ALOAD 3
-DUP
-ASTORE 4
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -42,7 +40,7 @@
 IFEQ L3
 LABEL L1
 CONST Integer:0
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L2
 IRETURN
@@ -57,14 +55,14 @@
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 CONST Integer:1
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L4
 IRETURN
 LABEL L5
-ASTORE 5
-ALOAD 4
+ASTORE 4
+ALOAD 3
 MONITOREXIT
 LABEL L6
-ALOAD 5
+ALOAD 4
 ATHROW
```

#### `method sourceAi$checkNeedActive()Z`

```diff
--- staged
+++ formal
@@ -4,7 +4,7 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+GETFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 CONST Integer:7
 IF_ICMPNE L0
 ALOAD 0
@@ -30,19 +30,19 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.isBusy()Z
 ISTORE 4
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+GETFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 CONST Integer:2
 IF_ICMPEQ L3
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+GETFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 CONST Integer:5
 IF_ICMPEQ L3
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+GETFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 CONST Integer:6
 IF_ICMPEQ L3
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._aiType:I
+GETFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 CONST Integer:7
 IF_ICMPNE L4
 ALOAD 0
```

#### `method sourceAi$checkSpawnLocation()V`

```diff
--- staged
+++ formal
@@ -3,10 +3,10 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L0 L1 L14 java/lang/Exception
-TRY L2 L6 L14 java/lang/Exception
-TRY L7 L9 L14 java/lang/Exception
-TRY L10 L13 L14 java/lang/Exception
+TRY L0 L1 L13 java/lang/Exception
+TRY L2 L5 L13 java/lang/Exception
+TRY L6 L8 L13 java/lang/Exception
+TRY L9 L12 L13 java/lang/Exception
 LABEL L0
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getMap()Llocal/lito381/server/NativeDeWorldMap;
@@ -25,13 +25,13 @@
 LABEL L3
 ILOAD 1
 CONST Integer:3
-IF_ICMPGT L12
+IF_ICMPGT L11
 CONST Integer:0
 ISTORE 2
 LABEL L4
 ILOAD 2
 CONST Integer:8
-IF_ICMPGE L11
+IF_ICMPGE L10
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getX()I
 GETSTATIC local/lito381/server/NativeDeLiveAiScope.HEADING_TABLE_X:[B
@@ -56,9 +56,7 @@
 ILOAD 4
 CONST null
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.isPassable(Llocal/lito381/server/NativeDeWorldMap;IILlocal/lito381/server/NativeDeCharacterFields;)Z
-IFNE L5
-GOTO L10
-LABEL L5
+IFEQ L9
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$World.get()Llocal/lito381/server/NativeDeLiveAiScope$World;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$World.getRecognizePlayer(Llocal/lito381/server/NativeDeObjectFields;)Ljava/util/ArrayList;
@@ -70,7 +68,7 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getMapId()S
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$World.relocateVisibleObject(Llocal/lito381/server/NativeDeObjectFields;III)Z
-IFNE L7
+IFNE L6
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.log()Llocal/lito381/server/NativeDeWorldLog;
 CONST str:"假人出生點修正失敗 name={} target=({}, {}, map={})"
 ALOAD 0
@@ -83,16 +81,16 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getMapId()S
 INVOKESTATIC java/lang/Short.valueOf(S)Ljava/lang/Short;
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
+LABEL L5
+RETURN
 LABEL L6
-RETURN
+ALOAD 5
+INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
+ASTORE 6
 LABEL L7
-ALOAD 5
-INVOKEVIRTUAL java/util/ArrayList.iterator()Ljava/util/Iterator;
-ASTORE 6
-LABEL L8
 ALOAD 6
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L9
+IFEQ L8
 ALOAD 6
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
@@ -108,22 +106,22 @@
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.removeKnown(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeObjectFields;)V
 ALOAD 7
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.update(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-GOTO L8
+GOTO L7
+LABEL L8
+RETURN
 LABEL L9
-RETURN
-LABEL L10
 IINC var=2 incr=1
 GOTO L4
-LABEL L11
+LABEL L10
 IINC var=1 incr=1
 GOTO L3
-LABEL L12
+LABEL L11
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getHomeX()I
-IFEQ L13
+IFEQ L12
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getHomeY()I
-IFEQ L13
+IFEQ L12
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$World.get()Llocal/lito381/server/NativeDeLiveAiScope$World;
 ALOAD 0
 ALOAD 0
@@ -133,7 +131,7 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getMapId()S
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$World.relocateVisibleObject(Llocal/lito381/server/NativeDeObjectFields;III)Z
-IFNE L13
+IFNE L12
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.log()Llocal/lito381/server/NativeDeWorldLog;
 CONST str:"假人出生點回復失敗 name={} home=({}, {}, map={})"
 ALOAD 0
@@ -148,13 +146,13 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getMapId()S
 INVOKESTATIC java/lang/Short.valueOf(S)Ljava/lang/Short;
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
+LABEL L12
+GOTO L14
 LABEL L13
-GOTO L15
-LABEL L14
 ASTORE 1
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.log()Llocal/lito381/server/NativeDeWorldLog;
 CONST str:"檢查出生點異常"
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-LABEL L15
+LABEL L14
 RETURN
```

#### `method sourceAi$completeAiStopLocked(J)Z`

```diff
--- staged
+++ formal
@@ -51,7 +51,7 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 ALOAD 0
 CONST Long:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J
```

#### `method sourceAi$dispatchAiWork(JZ)V`

```diff
--- staged
+++ formal
@@ -8,10 +8,8 @@
 TRY L5 L6 L5 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 4
-ALOAD 4
-DUP
-ASTORE 5
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -42,21 +40,21 @@
 LCMP
 IFEQ L3
 LABEL L1
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L2
 RETURN
 LABEL L3
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L4
 GOTO L7
 LABEL L5
-ASTORE 6
-ALOAD 5
+ASTORE 5
+ALOAD 4
 MONITOREXIT
 LABEL L6
-ALOAD 6
+ALOAD 5
 ATHROW
 LABEL L7
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$Pool.get()Llocal/lito381/server/NativeDeLiveAiScope$Pool;
@@ -73,7 +71,7 @@
 CONST str:"AI_SUBMIT_REJECTED_COUNT"
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.counter(Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
-LSTORE 5
+LSTORE 4
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$Pool.get()Llocal/lito381/server/NativeDeLiveAiScope$Pool;
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$Pool.isShutdown()Z
 IFEQ L9
@@ -86,8 +84,8 @@
 LLOAD 1
 ILOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.nextAiRetryDelay(JZ)I
-ISTORE 7
-ILOAD 7
+ISTORE 6
+ILOAD 6
 IFGE L10
 RETURN
 LABEL L10
@@ -95,12 +93,12 @@
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.counter(Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
-LLOAD 5
+LLOAD 4
 CONST Long:1
 LCMP
 IFEQ L11
-LLOAD 5
-LLOAD 5
+LLOAD 4
+LLOAD 4
 CONST Long:1
 LSUB
 LAND
@@ -112,15 +110,15 @@
 CONST str:"[L1DeInstance] NPC AI worker saturated; retrying: name={} delayMs={} rejected={}"
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getName()Ljava/lang/String;
-ILOAD 7
+ILOAD 6
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-LLOAD 5
+LLOAD 4
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
 LABEL L12
 ALOAD 0
 LLOAD 1
-ILOAD 7
+ILOAD 6
 ILOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture;
 POP
```

#### `method sourceAi$ensureAiScheduled()V`

```diff
--- staged
+++ formal
@@ -22,10 +22,8 @@
 ISTORE 3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 4
-ALOAD 4
-DUP
-ASTORE 5
 MONITORENTER
 LABEL L1
 ALOAD 0
@@ -41,7 +39,7 @@
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
 IFEQ L4
 LABEL L2
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L3
 RETURN
@@ -52,7 +50,7 @@
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L5
 RETURN
@@ -69,16 +67,16 @@
 CONST Integer:1
 ISTORE 3
 LABEL L7
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L8
 GOTO L11
 LABEL L9
-ASTORE 6
-ALOAD 5
+ASTORE 5
+ALOAD 4
 MONITOREXIT
 LABEL L10
-ALOAD 6
+ALOAD 5
 ATHROW
 LABEL L11
 ILOAD 3
```

#### `method sourceAi$enterBackgroundMode(J)V`

```diff
--- staged
+++ formal
@@ -8,10 +8,8 @@
 TRY L5 L6 L5 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 3
-ALOAD 3
-DUP
-ASTORE 4
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -27,14 +25,14 @@
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
 IFEQ L3
 LABEL L1
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L2
 RETURN
 LABEL L3
 ALOAD 0
 CONST Integer:1
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z
@@ -50,16 +48,16 @@
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$Manager.get()Llocal/lito381/server/NativeDeLiveAiScope$Manager;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$Manager.add(Llocal/lito381/server/NativeDeInstanceState;)V
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L4
 GOTO L7
 LABEL L5
-ASTORE 5
-ALOAD 4
+ASTORE 4
+ALOAD 3
 MONITOREXIT
 LABEL L6
-ALOAD 5
+ALOAD 4
 ATHROW
 LABEL L7
 RETURN
```

#### `method sourceAi$failAiTimer(JJ)V`

```diff
--- staged
+++ formal
@@ -9,10 +9,8 @@
 ISTORE 5
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 6
-ALOAD 6
-DUP
-ASTORE 7
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -38,16 +36,16 @@
 CONST Integer:1
 ISTORE 5
 LABEL L1
-ALOAD 7
+ALOAD 6
 MONITOREXIT
 LABEL L2
 GOTO L5
 LABEL L3
-ASTORE 8
-ALOAD 7
+ASTORE 7
+ALOAD 6
 MONITOREXIT
 LABEL L4
-ALOAD 8
+ALOAD 7
 ATHROW
 LABEL L5
 ILOAD 5
```

#### `method sourceAi$finishAiTick(J)V`

```diff
--- staged
+++ formal
@@ -12,10 +12,8 @@
 ISTORE 4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 5
-ALOAD 5
-DUP
-ASTORE 6
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -23,7 +21,7 @@
 LLOAD 1
 LCMP
 IFEQ L2
-ALOAD 6
+ALOAD 5
 MONITOREXIT
 LABEL L1
 RETURN
@@ -51,7 +49,7 @@
 IFNE L4
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$Manager.get()Llocal/lito381/server/NativeDeLiveAiScope$Manager;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$Manager.remove(Llocal/lito381/server/NativeDeInstanceState;)V
@@ -64,16 +62,16 @@
 CONST Integer:1
 ISTORE 3
 LABEL L4
-ALOAD 6
+ALOAD 5
 MONITOREXIT
 LABEL L5
 GOTO L8
 LABEL L6
-ASTORE 7
-ALOAD 6
+ASTORE 6
+ALOAD 5
 MONITOREXIT
 LABEL L7
-ALOAD 7
+ALOAD 6
 ATHROW
 LABEL L8
 ILOAD 4
```

#### `method sourceAi$getName()Ljava/lang/String;`

```diff
--- staged
+++ formal
@@ -15,10 +15,10 @@
 ARETURN
 LABEL L0
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+GETFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 IFNULL L1
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+GETFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 INVOKEVIRTUAL local/lito381/server/NativeDeInitializerKernel$Name.get_name()Ljava/lang/String;
 ARETURN
 LABEL L1
```

#### `method sourceAi$isBusy()Z`

```diff
--- staged
+++ formal
@@ -13,8 +13,7 @@
 INVOKEINTERFACE local/lito381/server/NativeDeLiveAiPorts$Combat.hasTarget()Z itf
 IFEQ L0
 CONST Integer:1
-GOTO L1
+IRETURN
 LABEL L0
 CONST Integer:0
-LABEL L1
 IRETURN
```

#### `method sourceAi$isDeAiChain(J)Z`

```diff
--- staged
+++ formal
@@ -14,10 +14,8 @@
 LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 3
-ALOAD 3
-DUP
-ASTORE 4
 MONITORENTER
 LABEL L1
 ALOAD 0
@@ -37,14 +35,14 @@
 LABEL L2
 CONST Integer:0
 LABEL L3
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L4
 IRETURN
 LABEL L5
-ASTORE 5
-ALOAD 4
+ASTORE 4
+ALOAD 3
 MONITOREXIT
 LABEL L6
-ALOAD 5
+ALOAD 4
 ATHROW
```

#### `method sourceAi$nextAiRetryDelay(JZ)I`

```diff
--- staged
+++ formal
@@ -8,10 +8,8 @@
 TRY L5 L6 L5 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 4
-ALOAD 4
-DUP
-ASTORE 5
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -35,7 +33,7 @@
 IF_ICMPEQ L3
 LABEL L1
 CONST Integer:-1
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L2
 IRETURN
@@ -54,20 +52,20 @@
 CONST Integer:1
 ISUB
 INVOKESTATIC java/lang/Math.min(II)I
-ISTORE 6
+ISTORE 5
 CONST Integer:1000
 CONST Integer:25
-ILOAD 6
+ILOAD 5
 ISHL
 INVOKESTATIC java/lang/Math.min(II)I
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L4
 IRETURN
 LABEL L5
-ASTORE 7
-ALOAD 5
+ASTORE 6
+ALOAD 4
 MONITOREXIT
 LABEL L6
-ALOAD 7
+ALOAD 6
 ATHROW
```

#### `method sourceAi$nextAiTimerTokenLocked()J`

```diff
--- staged
+++ formal
@@ -8,21 +8,20 @@
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
 CONST Long:1
 LADD
+DUP2_X1
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
-ALOAD 0
-DUP
-GETFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
-DUP2_X1
-CONST Long:1
-LADD
-PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
-DUP2
 LSTORE 1
+LLOAD 1
 CONST Long:0
 LCMP
 IFNE L0
 ALOAD 0
+DUP
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
+CONST Long:1
+LADD
+DUP2_X1
+PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
 LSTORE 1
 LABEL L0
 LLOAD 1
```

#### `method sourceAi$onAiTimer(JJZ)V`

```diff
--- staged
+++ formal
@@ -11,10 +11,8 @@
 ISTORE 6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 7
-ALOAD 7
-DUP
-ASTORE 8
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -42,7 +40,7 @@
 ILOAD 5
 IF_ICMPEQ L3
 LABEL L1
-ALOAD 8
+ALOAD 7
 MONITOREXIT
 LABEL L2
 RETURN
@@ -62,23 +60,23 @@
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
-ALOAD 8
+ALOAD 7
 MONITOREXIT
 LABEL L4
 RETURN
 LABEL L5
 CONST Integer:1
 ISTORE 6
-ALOAD 8
+ALOAD 7
 MONITOREXIT
 LABEL L6
 GOTO L9
 LABEL L7
-ASTORE 9
-ALOAD 8
+ASTORE 8
+ALOAD 7
 MONITOREXIT
 LABEL L8
-ALOAD 9
+ALOAD 8
 ATHROW
 LABEL L9
 ILOAD 6
```

#### `method sourceAi$onBackgroundTick()V`

```diff
--- staged
+++ formal
@@ -12,10 +12,8 @@
 ISTORE 3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 4
-ALOAD 4
-DUP
-ASTORE 5
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -31,7 +29,7 @@
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
 IFNE L1
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+GETFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 IFEQ L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z
@@ -48,16 +46,16 @@
 CONST Integer:1
 ISTORE 3
 LABEL L1
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L2
 GOTO L5
 LABEL L3
-ASTORE 6
-ALOAD 5
+ASTORE 5
+ALOAD 4
 MONITOREXIT
 LABEL L4
-ALOAD 6
+ALOAD 5
 ATHROW
 LABEL L5
 ILOAD 3
```

#### `method sourceAi$runAiTick(J)V`

```diff
--- staged
+++ formal
@@ -73,11 +73,11 @@
 ILOAD 3
 IFEQ L12
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+GETFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 IFEQ L8
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$Manager.get()Llocal/lito381/server/NativeDeLiveAiScope$Manager;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$Manager.remove(Llocal/lito381/server/NativeDeInstanceState;)V
@@ -92,21 +92,21 @@
 I2L
 LSTORE 4
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._shopBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Shop;
+GETFIELD local/lito381/server/NativeDePerceptionState._shopBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Shop;
 CHECKCAST local/lito381/server/NativeDeInstancePorts$Shop
 IFNULL L9
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._shopBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Shop;
+GETFIELD local/lito381/server/NativeDePerceptionState._shopBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Shop;
 CHECKCAST local/lito381/server/NativeDeInstancePorts$Shop
 INVOKEINTERFACE local/lito381/server/NativeDeInstancePorts$Shop.start()V itf
 GOTO L11
 LABEL L9
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._fishingBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Fishing;
+GETFIELD local/lito381/server/NativeDePerceptionState._fishingBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Fishing;
 CHECKCAST local/lito381/server/NativeDeLiveAiPorts$Fishing
 IFNULL L10
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._fishingBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Fishing;
+GETFIELD local/lito381/server/NativeDePerceptionState._fishingBehavior:Llocal/lito381/server/NativeDePerceptionPorts$Fishing;
 CHECKCAST local/lito381/server/NativeDeLiveAiPorts$Fishing
 INVOKEINTERFACE local/lito381/server/NativeDeLiveAiPorts$Fishing.execute()I itf
 I2L
```

#### `method sourceAi$runBackgroundTick(J)V`

```diff
--- staged
+++ formal
@@ -18,7 +18,7 @@
 RETURN
 LABEL L0
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+GETFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 IFEQ L1
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.checkNeedActive()Z
```

#### `method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture;`

```diff
--- staged
+++ formal
@@ -69,10 +69,10 @@
 ARETURN
 LABEL L6
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.nextAiTimerTokenLocked()J
-DUP2
 LSTORE 5
+ALOAD 0
+LLOAD 5
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J
 ALOAD 7
 MONITOREXIT
@@ -91,7 +91,7 @@
 LLOAD 1
 LLOAD 5
 ILOAD 4
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$sourceAi$scheduleAiTick$#0(JJZ)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$scheduleAiTick$#0(JJZ)V, type:()V]
 CONST Integer:0
 ILOAD 3
 INVOKESTATIC java/lang/Math.max(II)I
@@ -105,10 +105,8 @@
 ISTORE 8
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 9
-ALOAD 9
-DUP
-ASTORE 10
 MONITORENTER
 LABEL L11
 ALOAD 0
@@ -134,16 +132,16 @@
 CONST Integer:1
 ISTORE 8
 LABEL L12
-ALOAD 10
+ALOAD 9
 MONITOREXIT
 LABEL L13
 GOTO L16
 LABEL L14
-ASTORE 11
+ASTORE 10
+ALOAD 9
+MONITOREXIT
+LABEL L15
 ALOAD 10
-MONITOREXIT
-LABEL L15
-ALOAD 11
 ATHROW
 LABEL L16
 ILOAD 8
```

#### `method sourceAi$scheduleNextAiTick(J)V`

```diff
--- staged
+++ formal
@@ -17,10 +17,8 @@
 ISTORE 3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 4
-ALOAD 4
-DUP
-ASTORE 5
 MONITORENTER
 LABEL L1
 ALOAD 0
@@ -52,16 +50,16 @@
 CONST Integer:1
 ISTORE 3
 LABEL L2
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L3
 GOTO L6
 LABEL L4
-ASTORE 6
-ALOAD 5
+ASTORE 5
+ALOAD 4
 MONITOREXIT
 LABEL L5
-ALOAD 6
+ALOAD 5
 ATHROW
 LABEL L6
 ILOAD 3
```

#### `method sourceAi$setNpcSpeed()V`

```diff
--- staged
+++ formal
@@ -3,7 +3,7 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L0 L6 L7 java/lang/Exception
+TRY L0 L5 L6 java/lang/Exception
 LABEL L0
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$Sprites.get()Llocal/lito381/server/NativeDeLiveAiScope$Sprites;
 ALOAD 0
@@ -54,7 +54,7 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getDolls()Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.isEmpty()Z itf
-IFNE L6
+IFNE L5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getDolls()Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
@@ -68,7 +68,7 @@
 LABEL L3
 ILOAD 5
 ILOAD 4
-IF_ICMPGE L6
+IF_ICMPGE L5
 ALOAD 3
 ILOAD 5
 AALOAD
@@ -77,22 +77,20 @@
 CHECKCAST local/lito381/server/NativeDeLiveAiPorts$Doll
 ASTORE 7
 ALOAD 7
-IFNONNULL L4
-GOTO L5
-LABEL L4
+IFNULL L4
 ALOAD 7
 INVOKEINTERFACE local/lito381/server/NativeDeLiveAiPorts$Doll.setNpcMoveSpeed()V itf
-LABEL L5
+LABEL L4
 IINC var=5 incr=1
 GOTO L3
+LABEL L5
+GOTO L7
 LABEL L6
-GOTO L8
-LABEL L7
 ASTORE 1
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.log()Llocal/lito381/server/NativeDeWorldLog;
 ALOAD 1
 INVOKEVIRTUAL java/lang/Exception.getLocalizedMessage()Ljava/lang/String;
 ALOAD 1
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-LABEL L8
+LABEL L7
 RETURN
```

#### `method sourceAi$startAI()V`

```diff
--- staged
+++ formal
@@ -20,10 +20,8 @@
 LABEL L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 3
-ALOAD 3
-DUP
-ASTORE 4
 MONITORENTER
 LABEL L2
 ALOAD 0
@@ -46,7 +44,7 @@
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z
 LABEL L3
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L4
 RETURN
@@ -83,17 +81,17 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
-ALOAD 4
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
+ALOAD 3
 MONITOREXIT
 LABEL L6
 GOTO L9
 LABEL L7
-ASTORE 5
-ALOAD 4
+ASTORE 4
+ALOAD 3
 MONITOREXIT
 LABEL L8
-ALOAD 5
+ALOAD 4
 ATHROW
 LABEL L9
 ALOAD 0
```

#### `method sourceAi$stopAiOwner(J)V`

```diff
--- staged
+++ formal
@@ -14,10 +14,8 @@
 LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 3
-ALOAD 3
-DUP
-ASTORE 4
 MONITORENTER
 LABEL L1
 ALOAD 0
@@ -30,7 +28,7 @@
 LCMP
 IFEQ L4
 LABEL L2
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L3
 RETURN
@@ -55,16 +53,16 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
-ASTORE 5
+ASTORE 4
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
-ALOAD 5
+ALOAD 4
 IFNULL L5
-ALOAD 5
+ALOAD 4
 INVOKEINTERFACE java/util/concurrent/ScheduledFuture.isDone()Z itf
 IFNE L5
-ALOAD 5
+ALOAD 4
 CONST Integer:0
 INVOKEINTERFACE java/util/concurrent/ScheduledFuture.cancel(Z)Z itf
 POP
@@ -77,16 +75,16 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.completeAiStopLocked(J)Z
 POP
 LABEL L6
-ALOAD 4
+ALOAD 3
 MONITOREXIT
 LABEL L7
 GOTO L10
 LABEL L8
-ASTORE 6
-ALOAD 4
+ASTORE 5
+ALOAD 3
 MONITOREXIT
 LABEL L9
-ALOAD 6
+ALOAD 5
 ATHROW
 LABEL L10
 RETURN
```

#### `method sourceAi$updateNextActionTime(I)V`

```diff
--- staged
+++ formal
@@ -3,14 +3,14 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-ALOAD 0
 INVOKESTATIC java/lang/System.currentTimeMillis()J
 CONST Integer:0
 ILOAD 1
 INVOKESTATIC java/lang/Math.max(II)I
 I2L
 LADD
-DUP2
 LSTORE 2
+ALOAD 0
+LLOAD 2
 PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J
 RETURN
```

#### `method sourceAi$wakeup(J)V`

```diff
--- staged
+++ formal
@@ -12,10 +12,8 @@
 ISTORE 3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+DUP
 ASTORE 4
-ALOAD 4
-DUP
-ASTORE 5
 MONITORENTER
 LABEL L0
 ALOAD 0
@@ -31,14 +29,14 @@
 GETFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
 IFEQ L3
 LABEL L1
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L2
 RETURN
 LABEL L3
 ALOAD 0
 CONST Integer:0
-PUTFIELD local/lito381/server/NativeDeInstanceState._isBackgroundMode:Z
+PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope$Manager.get()Llocal/lito381/server/NativeDeLiveAiScope$Manager;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiScope$Manager.remove(Llocal/lito381/server/NativeDeInstanceState;)V
@@ -48,7 +46,7 @@
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L4
 RETURN
@@ -63,7 +61,7 @@
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
 LABEL L6
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L7
 RETURN
@@ -76,16 +74,16 @@
 PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z
 CONST Integer:1
 ISTORE 3
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L9
 GOTO L12
 LABEL L10
-ASTORE 6
-ALOAD 5
+ASTORE 5
+ALOAD 4
 MONITOREXIT
 LABEL L11
-ALOAD 6
+ALOAD 5
 ATHROW
 LABEL L12
 ILOAD 3
```

#### `method sourceDeleteDe()V`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.sourceDeleteDe()V
-RETURN
```

#### `method startAI()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$startAI$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#5(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method startNpc()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeInstanceState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+GETFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
 ALOAD 0
```

#### `method start_shop()V`

```diff
--- staged
+++ formal
@@ -1,8 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.start_shop()V
-RETURN
```

#### `method stopAI()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$stopAI$#0()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#27(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method stopAiOwner(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$stopAiOwner$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#29(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method submitAiTick(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$submitAiTick$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#17(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z`

```diff
--- staged
+++ formal
@@ -1,9 +0,0 @@
-access=0x41
-signature=null
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-default=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IRETURN
```

#### `method updateNextActionTime(I)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 ILOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$updateNextActionTime$#0(I)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#1(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method wakeup()V`

```diff
--- staged
+++ formal
@@ -5,7 +5,7 @@
 default=
 ALOAD 0
 ALOAD 0
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$wakeup$#1()Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#3(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

#### `method wakeup(J)V`

```diff
--- staged
+++ formal
@@ -6,7 +6,7 @@
 ALOAD 0
 ALOAD 0
 LLOAD 1
-INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H5 local/lito381/server/NativeDeInstanceState.lambda$wakeup$#0(J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$nativeAiScope$#4(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 RETURN
```

### `NativeDeItemAbility`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=local/lito381/server/NativeDeItemAbility$AbilityType
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019; local/lito381/server/NativeDeItemMetadata$Logs|local/lito381/server/NativeDeItemMetadata|Logs|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019
 class recordComponents=
 field _abilities:Ljava/util/Map; access=0x12
 field _abilities:Ljava/util/Map; signature=Ljava/util/Map<Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/lang/Integer;>;
```

### `NativeDeItemApplication$Owner`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method refreshFields()V`

```diff
--- staged
+++ formal
@@ -3,8 +3,8 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L7 L11 L12 java/lang/Throwable
-TRY L12 L13 L12 java/lang/Throwable
+TRY L6 L10 L11 java/lang/Throwable
+TRY L11 L12 L11 java/lang/Throwable
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.session:Ljava/lang/ref/WeakReference;
 INVOKEVIRTUAL java/lang/ref/WeakReference.get()Ljava/lang/Object;
@@ -48,59 +48,57 @@
 LABEL L5
 ILOAD 4
 CONST Integer:4
-IF_ICMPGE L15
+IF_ICMPGE L14
 ALOAD 1
 ILOAD 4
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeInventory(I)Llocal/lito381/server/RuntimeInventory;
 ASTORE 5
 ALOAD 5
-IFNONNULL L6
-GOTO L14
-LABEL L6
+IFNULL L13
 ALOAD 5
 DUP
 ASTORE 6
 MONITORENTER
-LABEL L7
+LABEL L6
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.nativeDeItems()Ljava/util/Map;
 ASTORE 7
 ALOAD 7
 INSTANCEOF local/lito381/server/NativeDeItemInventory
-IFEQ L10
+IFEQ L9
 ALOAD 7
 CHECKCAST local/lito381/server/NativeDeItemInventory
 ASTORE 8
 ALOAD 8
 GETFIELD local/lito381/server/NativeDeItemInventory.owner:Llocal/lito381/server/NativeDeItemApplication$Owner;
 ALOAD 0
-IF_ACMPNE L10
+IF_ACMPNE L9
 ALOAD 8
 ALOAD 5
 ALOAD 3
-IF_ACMPNE L8
+IF_ACMPNE L7
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.objectId:J
 L2I
-GOTO L9
+GOTO L8
+LABEL L7
+CONST Integer:0
 LABEL L8
-CONST Integer:0
+INVOKEVIRTUAL local/lito381/server/NativeDeItemInventory.ownerFields(I)V
 LABEL L9
-INVOKEVIRTUAL local/lito381/server/NativeDeItemInventory.ownerFields(I)V
-LABEL L10
 ALOAD 6
 MONITOREXIT
+LABEL L10
+GOTO L13
 LABEL L11
-GOTO L14
-LABEL L12
 ASTORE 9
 ALOAD 6
 MONITOREXIT
-LABEL L13
+LABEL L12
 ALOAD 9
 ATHROW
-LABEL L14
+LABEL L13
 IINC var=4 incr=1
 GOTO L5
-LABEL L15
+LABEL L14
 RETURN
```

#### `method requireActive(Llocal/lito381/server/RuntimeInventory;)V`

```diff
--- staged
+++ formal
@@ -7,31 +7,31 @@
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.application:Ljava/lang/ref/WeakReference;
 INVOKEVIRTUAL java/lang/ref/WeakReference.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeItemApplication
-ASTORE 3
+ASTORE 2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.session:Ljava/lang/ref/WeakReference;
 INVOKEVIRTUAL java/lang/ref/WeakReference.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 4
+ASTORE 3
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.active:Z
 IFEQ L0
-ALOAD 3
+ALOAD 2
 IFNULL L0
-ALOAD 3
+ALOAD 2
 GETFIELD local/lito381/server/NativeDeItemApplication.closed:Z
 IFNE L0
-ALOAD 3
+ALOAD 2
 GETFIELD local/lito381/server/NativeDeItemApplication.life:Llocal/lito381/server/NativePlayerLifetime;
 INVOKEVIRTUAL local/lito381/server/NativePlayerLifetime.admits()Z
 IFEQ L0
-ALOAD 4
+ALOAD 3
 IFNULL L0
 ALOAD 0
-ALOAD 4
+ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.matches(Llocal/lito381/server/ClientSession;)Z
 IFEQ L0
-ALOAD 4
+ALOAD 3
 CONST str:"sourceItemOwner"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ALOAD 0
@@ -43,13 +43,13 @@
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L1
-ALOAD 4
+ALOAD 3
 CONST str:"rentalTownRoomState"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
 IFEQ L2
-ALOAD 4
+ALOAD 3
 CONST str:"phase"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
@@ -61,19 +61,17 @@
 LABEL L2
 CONST Integer:0
 LABEL L3
-ISTORE 5
-ILOAD 5
+ISTORE 4
+ILOAD 4
 IFEQ L4
 CONST Integer:1
 GOTO L5
 LABEL L4
 CONST Integer:0
 LABEL L5
-DUP
-ISTORE 2
-ISTORE 6
-ALOAD 4
-ILOAD 2
+ISTORE 5
+ALOAD 3
+ILOAD 5
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeInventory(I)Llocal/lito381/server/RuntimeInventory;
 ALOAD 1
 IF_ACMPEQ L6
```

#### `method retire()V`

```diff
--- staged
+++ formal
@@ -3,8 +3,8 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L2 L3 L4 java/lang/Throwable
-TRY L4 L5 L4 java/lang/Throwable
+TRY L1 L2 L3 java/lang/Throwable
+TRY L3 L4 L3 java/lang/Throwable
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/NativeDeItemApplication$Owner.active:Z
@@ -14,39 +14,37 @@
 CHECKCAST local/lito381/server/ClientSession
 ASTORE 1
 ALOAD 1
-IFNULL L7
+IFNULL L6
 CONST Integer:0
 ISTORE 2
 LABEL L0
 ILOAD 2
 CONST Integer:4
-IF_ICMPGE L7
+IF_ICMPGE L6
 ALOAD 1
 ILOAD 2
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeInventory(I)Llocal/lito381/server/RuntimeInventory;
 ASTORE 3
 ALOAD 3
-IFNONNULL L1
-GOTO L6
-LABEL L1
+IFNULL L5
 ALOAD 3
 DUP
 ASTORE 4
 MONITORENTER
-LABEL L2
+LABEL L1
 ALOAD 4
 MONITOREXIT
+LABEL L2
+GOTO L5
 LABEL L3
-GOTO L6
-LABEL L4
 ASTORE 5
 ALOAD 4
 MONITOREXIT
-LABEL L5
+LABEL L4
 ALOAD 5
 ATHROW
-LABEL L6
+LABEL L5
 IINC var=2 incr=1
 GOTO L0
-LABEL L7
+LABEL L6
 RETURN
```

### `NativeDeItemApplication`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method install(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeDeItemTemplatesResources$Snapshot;Llocal/lito381/server/NativeDeItemDefinitionsResources$Snapshot;)Llocal/lito381/server/NativeDeItemApplication;`

```diff
--- staged
+++ formal
@@ -6,9 +6,9 @@
 TRY L0 L4 L7 java/lang/Throwable
 TRY L5 L6 L7 java/lang/Throwable
 TRY L7 L8 L7 java/lang/Throwable
-TRY L12 L15 L16 java/lang/Throwable
-TRY L16 L17 L16 java/lang/Throwable
-TRY L9 L20 L21 java/lang/Throwable
+TRY L11 L14 L15 java/lang/Throwable
+TRY L15 L16 L15 java/lang/Throwable
+TRY L9 L19 L20 java/lang/Throwable
 ALOAD 1
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
@@ -91,7 +91,7 @@
 LABEL L10
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L19
+IFEQ L18
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -101,48 +101,46 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication.owner(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeDeItemApplication$Owner;
 ASTORE 6
 ALOAD 6
-IFNONNULL L11
-GOTO L10
-LABEL L11
+IFNULL L17
 ALOAD 6
 DUP
 ASTORE 7
 MONITORENTER
-LABEL L12
+LABEL L11
 CONST Integer:0
 ISTORE 8
-LABEL L13
+LABEL L12
 ILOAD 8
 CONST Integer:4
-IF_ICMPGE L14
+IF_ICMPGE L13
 ALOAD 6
 ALOAD 5
 ILOAD 8
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeInventory(I)Llocal/lito381/server/RuntimeInventory;
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.adopt(Llocal/lito381/server/RuntimeInventory;)V
 IINC var=8 incr=1
-GOTO L13
-LABEL L14
+GOTO L12
+LABEL L13
 ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.refreshFields()V
 ALOAD 7
 MONITOREXIT
+LABEL L14
+GOTO L17
 LABEL L15
-GOTO L18
-LABEL L16
 ASTORE 9
 ALOAD 7
 MONITOREXIT
-LABEL L17
+LABEL L16
 ALOAD 9
 ATHROW
+LABEL L17
+GOTO L10
 LABEL L18
-GOTO L10
+ALOAD 3
 LABEL L19
-ALOAD 3
+ARETURN
 LABEL L20
-ARETURN
-LABEL L21
 ASTORE 4
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication.close()V
```

#### `method publicationOwner(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeDeItemApplication$Owner;`

```diff
--- staged
+++ formal
@@ -49,22 +49,22 @@
 CONST str:"sourceItemOwner"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeItemApplication$Owner
-ASTORE 6
-ALOAD 6
+ASTORE 4
+ALOAD 4
 IFNULL L5
-ALOAD 6
+ALOAD 4
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.active:Z
 IFEQ L5
-ALOAD 6
+ALOAD 4
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.application:Ljava/lang/ref/WeakReference;
 INVOKEVIRTUAL java/lang/ref/WeakReference.get()Ljava/lang/Object;
 ALOAD 2
 IF_ACMPNE L5
-ALOAD 6
+ALOAD 4
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.matches(Llocal/lito381/server/ClientSession;)Z
 IFEQ L5
-ALOAD 6
+ALOAD 4
 ALOAD 3
 MONITOREXIT
 LABEL L4
@@ -84,10 +84,10 @@
 ALOAD 2
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication.owner(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeDeItemApplication$Owner;
-ASTORE 7
-ALOAD 7
+ASTORE 5
+ALOAD 5
 IFNULL L9
-ALOAD 7
+ALOAD 5
 ALOAD 3
 MONITOREXIT
 LABEL L8
@@ -115,12 +115,12 @@
 LABEL L12
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
-DUP2
-LSTORE 4
+LSTORE 5
+LLOAD 5
 CONST Long:0
 LCMP
 IFLE L13
-LLOAD 4
+LLOAD 5
 CONST Long:4294967295
 LCMP
 IFLE L15
@@ -131,9 +131,9 @@
 LABEL L14
 ARETURN
 LABEL L15
-ALOAD 6
+ALOAD 4
 IFNULL L16
-ALOAD 6
+ALOAD 4
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.retire()V
 LABEL L16
 NEW local/lito381/server/NativeDeItemApplication$Owner
```

#### `method replace(Llocal/lito381/server/ClientSession;Llocal/lito381/server/RuntimeInventory;I)V`

```diff
--- staged
+++ formal
@@ -3,42 +3,40 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L11 L13 L14 java/lang/Throwable
-TRY L14 L15 L14 java/lang/Throwable
-TRY L10 L19 L20 java/lang/Throwable
-TRY L20 L21 L20 java/lang/Throwable
-TRY L5 L8 L24 java/lang/Throwable
-TRY L9 L23 L24 java/lang/Throwable
-TRY L24 L25 L24 java/lang/Throwable
+TRY L11 L12 L13 java/lang/Throwable
+TRY L13 L14 L13 java/lang/Throwable
+TRY L10 L18 L19 java/lang/Throwable
+TRY L19 L20 L19 java/lang/Throwable
+TRY L5 L8 L23 java/lang/Throwable
+TRY L9 L22 L23 java/lang/Throwable
+TRY L23 L24 L23 java/lang/Throwable
 ALOAD 0
 CONST str:"hub"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/GameHub
-ASTORE 4
-ALOAD 4
+ASTORE 3
+ALOAD 3
 IFNONNULL L0
 CONST null
 GOTO L1
 LABEL L0
-ALOAD 4
+ALOAD 3
 CONST str:"sourceItemApplication"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeItemApplication
 LABEL L1
-ASTORE 5
-ALOAD 5
+ASTORE 4
+ALOAD 4
 IFNONNULL L2
 CONST null
 GOTO L3
 LABEL L2
-ALOAD 5
+ALOAD 4
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication.owner(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeDeItemApplication$Owner;
 LABEL L3
-DUP
-ASTORE 3
-ASTORE 6
-ALOAD 3
+ASTORE 5
+ALOAD 5
 IFNONNULL L4
 ALOAD 0
 ILOAD 2
@@ -46,15 +44,15 @@
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeSetInventory(ILlocal/lito381/server/RuntimeInventory;)V
 RETURN
 LABEL L4
-ALOAD 3
+ALOAD 5
 DUP
-ASTORE 8
+ASTORE 6
 MONITORENTER
 LABEL L5
-ALOAD 3
+ALOAD 5
 GETFIELD local/lito381/server/NativeDeItemApplication$Owner.active:Z
 IFEQ L6
-ALOAD 3
+ALOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.matches(Llocal/lito381/server/ClientSession;)Z
 IFNE L7
@@ -65,98 +63,90 @@
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L7
-ALOAD 3
+ALOAD 5
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.adopt(Llocal/lito381/server/RuntimeInventory;)V
 ALOAD 0
 ILOAD 2
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeInventory(I)Llocal/lito381/server/RuntimeInventory;
-ASTORE 9
-ALOAD 9
+ASTORE 7
+ALOAD 7
 IFNONNULL L9
 ALOAD 0
 ILOAD 2
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeSetInventory(ILlocal/lito381/server/RuntimeInventory;)V
-ALOAD 3
+ALOAD 5
 INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.refreshFields()V
-ALOAD 8
+ALOAD 6
 MONITOREXIT
 LABEL L8
 RETURN
 LABEL L9
-ALOAD 9
+ALOAD 7
 DUP
-ASTORE 11
+ASTORE 8
 MONITORENTER
 LABEL L10
 ALOAD 1
-IFNULL L17
+IFNULL L16
 ALOAD 1
 DUP
-ASTORE 12
+ASTORE 9
 MONITORENTER
 LABEL L11
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.nativeDeItems()Ljava/util/Map;
-ASTORE 13
-ALOAD 13
-INSTANCEOF local/lito381/server/NativeDeItemInventory
-IFEQ L12
-ALOAD 13
 CHECKCAST local/lito381/server/NativeDeItemInventory
-ASTORE 14
-ALOAD 14
-ALOAD 9
+ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeItemInventory.validateReplacement(Llocal/lito381/server/RuntimeInventory;)V
-LABEL L12
 ALOAD 0
 ILOAD 2
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeSetInventory(ILlocal/lito381/server/RuntimeInventory;)V
-ALOAD 12
+ALOAD 9
 MONITOREXIT
+LABEL L12
+GOTO L15
 LABEL L13
-GOTO L16
+ASTORE 10
+ALOAD 9
+MONITOREXIT
 LABEL L14
-ASTORE 15
-ALOAD 12
-MONITOREXIT
+ALOAD 10
+ATHROW
 LABEL L15
-ALOAD 15
-ATHROW
+GOTO L17
 LABEL L16
-GOTO L18
-LABEL L17
 ALOAD 0
 ILOAD 2
 CONST null
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeDeSetInventory(ILlocal/lito381/server/RuntimeInventory;)V
-LABEL L18
-ALOAD 11
-MONITOREXIT
-LABEL L19
-GOTO L22
-LABEL L20
-ASTORE 16
-ALOAD 11
-MONITOREXIT
-LABEL L21
-ALOAD 16
-ATHROW
-LABEL L22
-ALOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.refreshFields()V
+LABEL L17
 ALOAD 8
 MONITOREXIT
-LABEL L23
-GOTO L26
-LABEL L24
-ASTORE 17
+LABEL L18
+GOTO L21
+LABEL L19
+ASTORE 11
 ALOAD 8
 MONITOREXIT
+LABEL L20
+ALOAD 11
+ATHROW
+LABEL L21
+ALOAD 5
+INVOKEVIRTUAL local/lito381/server/NativeDeItemApplication$Owner.refreshFields()V
+ALOAD 6
+MONITOREXIT
+LABEL L22
+GOTO L25
+LABEL L23
+ASTORE 12
+ALOAD 6
+MONITOREXIT
+LABEL L24
+ALOAD 12
+ATHROW
 LABEL L25
-ALOAD 17
-ATHROW
-LABEL L26
 RETURN
```

### `NativeDeItemBlessStatsTable`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019; local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019
 class recordComponents=
 field _armorBlessStats:Ljava/util/Map; access=0x12
 field _armorBlessStats:Ljava/util/Map; signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/Map<Ljava/lang/Integer;Llocal/lito381/server/NativeDeItemAbility;>;>;
```

#### `method loadArmorBlessStats()V`

```diff
--- staged
+++ formal
@@ -132,6 +132,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (bless_id: \u0001)"]
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L8
 GOTO L3
 LABEL L9
```

#### `method loadWeaponBlessStats()V`

```diff
--- staged
+++ formal
@@ -85,6 +85,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (bless_id: \u0001)"]
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L7
 GOTO L3
 LABEL L8
```

### `NativeDeItemCommonEnchantTable`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019; local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019
 class recordComponents=
 field _armorCommonBonus:Ljava/util/Map; access=0x12
 field _armorCommonBonus:Ljava/util/Map; signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/Map<Ljava/lang/Integer;Llocal/lito381/server/NativeDeItemAbility;>;>;
```

#### `method loadArmorCommonBonus()I`

```diff
--- staged
+++ formal
@@ -141,6 +141,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (over_enchant: \u0001)"]
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L8
 GOTO L3
 LABEL L9
```

#### `method loadWeaponCommonBonus()I`

```diff
--- staged
+++ formal
@@ -100,6 +100,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (over_enchant: \u0001)"]
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L7
 GOTO L3
 LABEL L8
```

### `NativeDeItemDefinitionsResources`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `method freeze(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V`

```diff
--- staged
+++ formal
@@ -3,8 +3,8 @@
 exceptions=java/sql/SQLException
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-TRY L0 L7 L8 java/lang/ReflectiveOperationException
-TRY L0 L7 L8 java/lang/RuntimeException
+TRY L0 L6 L7 java/lang/ReflectiveOperationException
+TRY L0 L6 L7 java/lang/RuntimeException
 LABEL L0
 NEW java/util/IdentityHashMap
 DUP
@@ -18,7 +18,7 @@
 LABEL L1
 ALOAD 2
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L6
+IFEQ L5
 ALOAD 2
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 ASTORE 3
@@ -34,7 +34,7 @@
 LABEL L2
 ILOAD 6
 ILOAD 5
-IF_ICMPGE L5
+IF_ICMPGE L4
 ALOAD 4
 ILOAD 6
 AALOAD
@@ -42,14 +42,12 @@
 ALOAD 7
 INVOKEVIRTUAL java/lang/reflect/Field.getModifiers()I
 INVOKESTATIC java/lang/reflect/Modifier.isStatic(I)Z
-IFNE L4
+IFNE L3
 CONST type:Ljava/util/Map;
 ALOAD 7
 INVOKEVIRTUAL java/lang/reflect/Field.getType()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.isAssignableFrom(Ljava/lang/Class;)Z
-IFNE L3
-GOTO L4
-LABEL L3
+IFEQ L3
 ALOAD 7
 CONST Integer:1
 INVOKEVIRTUAL java/lang/reflect/Field.setAccessible(Z)V
@@ -61,12 +59,12 @@
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsResources.freezeValue(Ljava/lang/Object;Ljava/util/IdentityHashMap;)Ljava/lang/Object;
 INVOKEVIRTUAL java/lang/reflect/Field.set(Ljava/lang/Object;Ljava/lang/Object;)V
-LABEL L4
+LABEL L3
 IINC var=6 incr=1
 GOTO L2
+LABEL L4
+GOTO L1
 LABEL L5
-GOTO L1
-LABEL L6
 ALOAD 0
 NEW java/util/LinkedHashMap
 DUP
@@ -75,9 +73,9 @@
 INVOKESPECIAL java/util/LinkedHashMap.<init>(Ljava/util/Map;)V
 INVOKESTATIC java/util/Collections.unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;
 PUTFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.tables:Ljava/util/Map;
+LABEL L6
+GOTO L8
 LABEL L7
-GOTO L9
-LABEL L8
 ASTORE 1
 NEW java/sql/SQLException
 DUP
@@ -85,5 +83,5 @@
 ALOAD 1
 INVOKESPECIAL java/sql/SQLException.<init>(Ljava/lang/String;Ljava/lang/Throwable;)V
 ATHROW
-LABEL L9
+LABEL L8
 RETURN
```

#### `method lambda$read$#0(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)Ljava/lang/Object;`

```diff
--- staged
+++ formal
@@ -29,11 +29,10 @@
 INVOKESTATIC java/util/Set.of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Set; itf
 ALOAD 2
 INVOKEINTERFACE java/util/Set.contains(Ljava/lang/Object;)Z itf
-IFNE L1
-GOTO L0
-LABEL L1
+IFEQ L1
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsResources.invokeLoad(Ljava/lang/Object;)V
+LABEL L1
 GOTO L0
 LABEL L2
 CONST null
```

#### `method read(Ljava/lang/String;Llocal/lito381/server/NativeDeTerrainScope$Sql;Llocal/lito381/server/NativeDeWorldLog;)Llocal/lito381/server/NativeDeItemDefinitionsResources$Snapshot;`

```diff
--- staged
+++ formal
@@ -58,6 +58,7 @@
 IFNONNULL L5
 ALOAD 6
 ASTORE 5
+GOTO L6
 LABEL L5
 ALOAD 5
 ALOAD 6
@@ -82,6 +83,7 @@
 IFNONNULL L11
 ALOAD 6
 ASTORE 5
+GOTO L12
 LABEL L11
 ALOAD 5
 ALOAD 6
@@ -104,6 +106,7 @@
 IFNONNULL L17
 ALOAD 8
 ASTORE 5
+GOTO L18
 LABEL L17
 ALOAD 5
 ALOAD 8
```

### `NativeDeItemEnchantBonusTable`

CFR／Vineflower 及局部重寫後仍有下列完整方法／結構差異；未證明 Java 無法再改善，也未證明行為等價。請主控依完整 diff 決定續修範圍或明確例外，不可只忽略 firstDiff。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019; local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019
 class recordComponents=
 field _enchantBonuses:Ljava/util/Map; access=0x12
 field _enchantBonuses:Ljava/util/Map; signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/Map<Ljava/lang/Integer;Llocal/lito381/server/NativeDeItemAbility;>;>;
```

#### `method loadArmorBonuses()I`

```diff
--- staged
+++ formal
@@ -135,6 +135,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (item_id: \u0001)"]
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L8
 GOTO L3
 LABEL L9
```

#### `method loadWeaponBonuses()I`

```diff
--- staged
+++ formal
@@ -100,6 +100,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (item_id: \u0001)"]
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L7
 GOTO L3
 LABEL L8
```

### `NativeDeItemEquipProcTable`

本輪完整比較僅 metadata／欄位結構不同，方法指令、例外表與方法屬性均一致。多數為 InnerClasses 缺列或旗標不同；javac 依實際巢狀型別生成，尚未取得本批豁免。此觀察不代表 metadata 對所有反射／存取情境無影響。

#### `class / fields`

```diff
--- staged
+++ formal
@@ -9,7 +9,7 @@
 class nestMembers=
 class permittedSubclasses=
 class annotations=|||
-class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18; local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
+class innerClasses=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 class recordComponents=
 field ENCHANT_ANY_LEVEL:I access=0x1a
 field ENCHANT_ANY_LEVEL:I signature=null
```

## 差異匯出器的完整來源（可重建證據）

以下存為 `scratch/issue-72/Dump.java`。這是診斷輸出器，透過 reflection 讀原工具私有正規化函式；不更動工具，也不載入／執行遊戲類別。

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
    JarIndex j=JarIndex.load(Path.of(args[0])); Path out=Path.of(args[2]); Files.createDirectories(out);
    for(String entry:Files.readAllLines(Path.of(args[1]))) {
      String name=entry.replace(".class",""); ClassNode c=j.readTree(name); if(c==null)continue;
      List<String> lines=new ArrayList<>();
      for(String key:List.of("access","superName","signature","outerClass","outerMethod","outerMethodDesc","nestHostClass"))add(lines,"class "+key,ClassNode.class.getField(key).get(c));
      for(String key:List.of("interfaces","nestMembers","permittedSubclasses"))add(lines,"class "+key,call("joinSorted",ClassNode.class.getField(key).get(c)));
      add(lines,"class annotations",call("annotations",c));
      add(lines,"class innerClasses",call("innerClasses",c));
      add(lines,"class recordComponents",call("records",c,j));
      for(FieldNode f:new TreeMap<String,FieldNode>((Map<String,FieldNode>)call("fieldMap",c)).values()) {
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

先執行前述 reproduce.py，以下 PowerShell 會輸出完整左右正規化文字：

```powershell
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$server = 'lito伺服器/LiTo381-Server'
$asm = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar'
$cp = "$server/tools/semantic-diff/target/classes;$asm"
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $cp -d scratch/issue-72/target/diagnostics scratch/issue-72/Dump.java
if ($LASTEXITCODE -ne 0) { throw 'diagnostic build failed' }
& "$jdk/java.exe" -cp "scratch/issue-72/target/diagnostics;$cp" local.lito381.semanticdiff.Dump scratch/issue-72/target/reproduce.jar scratch/issue-72/verify_entries.txt scratch/issue-72/left
& "$jdk/java.exe" -cp "scratch/issue-72/target/diagnostics;$cp" local.lito381.semanticdiff.Dump "$server/reference/base-0.6.71.jar" scratch/issue-72/verify_entries.txt scratch/issue-72/right
```

可用 Python `difflib.unified_diff` 對兩側同名文字檔逐行比較；報告分段時依每行 `method <名稱及描述子> ` 分組，其餘列為 class／fields；每組 n=3。原始正規化 TRY 列的尾端空白於 Markdown 排版移除，不影響左右比較值或任何 opcode／運算元。
