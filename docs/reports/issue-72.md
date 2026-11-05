# Issue #72 — T020-A01 正式 0.6.71 暫存（審查退回修正）

## 結論

2026-11-05，本輪以 `6f8a87b73f810f65c36f89839c1c8ec76ef4aa28` 為起點，在既有 `task/72` 工作樹修正 19 份 A01 staged Java。49 份來源可直接編譯，產物恰好包含 A01 的 185 個 entry：**152 SAME／33 DIFF／0 ONLY_LEFT／0 ONLY_RIGHT**，SAME_EFFECTS=0、BYTE_SAME=18，missingLeft／extraLeft 皆空。相較前輪 137／48，新增 15 個 SAME，沒有原本 SAME 的 entry 退回 DIFF。

**本任務仍未完成，不可合併進 A-land、不可部署。** 這次是修正過程的診斷重跑，並非依賴合併後的最終驗收。查詢 GitHub 時 #135 與 #153 仍 OPEN，尚未取得合併後工具；依主控留言，必須待兩案合併後再做 185 SAME 驗收。本輪已重新加上 `needs:claude`，原因是非 Object 建構子及下述未涵蓋的結構問題；不是要求把所有分支／try 差異當成例外。

依據：[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6644325505)、[本輪退回](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6645753281)、設計 `docs/design/T020-combat-merge.md` §7。

## 做了什麼

- 修正審查點名的 `NativeDeInstanceScene.publish`、`CombatSkillDerived.compute`、`NativeDeItemApplication.install`／`Owner.refreshFields`、`NativeDeItemDefinitionsResources.freeze`；也修正聊天／物品資料表 catch 的 continue、控制資料載入、corpse timer、名字綁定、初始化失敗分支與角色同步／return 結構。
- 修正已從正式指令確認的行為差異：`NativeDeItemDefinitionsResources.read` 在沒有前一個例外時不再對自己呼叫 `addSuppressed`；`NativeDeItemApplication.replace` 恢復正式版的直接轉型與驗證；`NativeDeCorpseCore.run` 的 finally 不再以 continue 吞掉原有例外；`NativeDeInstanceState.sourceAi$nextAiTimerTokenLocked` 恢復先遞增一次、僅結果為 0 才再遞增。
- `NativeDeCharacterFields.setStatus` 恢復 Runnable overload；`drainVitalCompletionsAfterActionUnlock` 恢復正式版的 try／finally 迴圈範圍。這些方法的完整正規化差異已消除。其他仍不同的方法照實列在附錄。
- 調整 `NativeDeDeletionVisibility.Snapshot` 在來源中的位置，使 javac switch map 序號與正式相同；仍保留合成欄位名稱 DIFF，等待 #153，未改名或後處理 class。
- 使用前輪從 SHA 固定正式 JAR 產生的 CFR 0.152／Vineflower 1.11.2 來源作逐方法對照；本輪沒有重新下載或重新執行反編譯器。49 份 staged 都保留 DECOMPILED 標頭。
- `src/main`、`pom.xml`、`docs/REPORT.md`、正規化工具與核准清單均未修改。沒有套用 allow／effects／exclude，沒有 javac 後 ASM，也沒有用正式 class 覆蓋輸出。

新增整個 entry SAME 的 15 項：

`NativeDeCharacterFields$CombatSkillDerived`, `NativeDeCharacterFields$DamageMutationReceipt`, `NativeDeChatWire`, `NativeDeCorpseCore`, `NativeDeCorpseTimers$Job`, `NativeDeCorpseTimers$Owner`, `NativeDeCorpseTimers$Owner$1`, `NativeDeCorpseTimers$Ticket`, `NativeDeInitializerKernel`, `NativeDeInstanceNames`, `NativeDeInstanceScene`, `NativeDeInstanceScope$World`, `NativeDeItemApplication`, `NativeDeItemApplication$Owner`, `NativeDeItemDefinitionsResources`。

## 已驗證：非 Object 父類與建構子時序

正式 JAR SHA-256 已重新核對。對正式版執行 `javap -p -c -v`，並比對兩側正規化全文：

```text
NativeDeInstanceState:
  super_class: #4 // local/lito381/server/NativeDeNpcConstructionState
left/right class superName=local/lito381/server/NativeDeNpcConstructionState
NativeDeNpcConstructionState:
  super_class: #4 // local/lito381/server/NativeDeNpcTemplateState
```

正式版最基本建構子完整描述子：

```text
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V
```

其起始指令是 `ALOAD 0; ALOAD 1; PUTFIELD NativeDeInstanceState.construction`，隨後取 construction 的 id／effects／owner／perception／template／npc，再 `INVOKESPECIAL NativeDeNpcConstructionState.<init>`。staged 則先 super、後寫 construction。另四個含 Name 的建構子，正式版在父建構子前呼叫 `NativeDeInstanceNames.name`，staged 移到 super 後。五個建構子的完整正規化差異均收錄於附錄，沒有只列 firstDiff。

父建構子不是無作用的 Object 建構子。正式 `NativeDeNpcConstructionState` 建構子中可見以下 bytecode offset：

```text
  9: invokespecial NativeDeNpcTemplateState.<init>(...)
 15: invokestatic java/util/Objects.requireNonNull(Object)
 21: putfield nativeServices
 26: invokevirtual setNameId(String)
 30: new NativeDeNpcInventory
 38: invokespecial NativeDeNpcInventory.<init>(Services)
 41: putfield NativeDeDeletionState.inventory
 45: iconst_1
 46: putfield firstFound
 90: new java/util/concurrent/atomic/AtomicLong
 98: putfield _aiGeneration
```

**已驗證**：實際父類、欄位初始化與方法呼叫、super 前後的時序差異。**推測／風險**：父鏈或虛擬呼叫可能在 construction 尚未賦值時觀察到不同狀態，Name 的錯誤也可能改變父類是否執行；沒有執行建構子，沒有宣稱已重現特定執行期錯誤。**待裁定**：#153 的 Object-only 規則不適用此類；請主控另外裁定可接受的 Java 表達／交付策略，不得直接放寬比較。

正式 `NativeDeCharacterFields$CombatSkillDerived.compute` 的相關欄位引用（指令內容來自正式正規化全文）：

```text
GETSTATIC local/lito381/server/NativeDeCharacterFields.SKU1:Ljava/util/HashMap;
GETSTATIC local/lito381/server/NativeDeCharacterFields.SKU2:Ljava/util/HashMap;
GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD1:Ljava/util/HashMap;
GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD2:Ljava/util/HashMap;
GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD3:Ljava/util/HashMap;
```

## 已裁定事項與新增未決事項

| 項目 | 本輪結論與下一步 |
|---|---|
| #108 編譯依賴 | 已完成；使用 repo 的 compile-stubs，不再要求裁定私人 stub。 |
| InnerClasses、ACC_SUPER、預設建構子、槽位 | 等 #135 合併後套用正式工具重跑。 |
| lambda 內容與 handle、switch-map 名稱 | 等 #153 合併後重跑；名稱之外的方法內容仍須相同。 |
| `local/lito381/server/NativeDeDeletionState#get_showId()I` | 已核准放寬為 public，完整識別提供給 #153 的 access-widen-approved.txt；本批不越界修改清單。 |
| 非 Object 的 Instance 建構子 | 重新標 needs:claude；不在既有 Object-only 裁定內。 |
| `NativeDeCharacterFields` 的 SKU1／SKU2／SKD1／SKD2／SKD3 | 前輪 staged 新增五個空 HashMap 欄位，正式版及其父類 NativeDeObjectFields 均未宣告這五個欄位，而正式 CombatSkillDerived.compute 仍以 GETSTATIC 引用它們。正式父鏈止於 Object；本輪靜態查核確認矛盾，未執行該方法。附錄列出五個多出欄位、clinit 及正式引用。空 map 不可當成功移植，尚需主控指定正式欄位來源／處理方式；本輪未擴大此補法。 |
| `NativeDeInstanceState` 額外橋接方法 | 直接 javac 產物有 **169 個** access=0x41（public／bridge）方法在正式版缺少；例如 `getAc()I`。本輪未在來源新增這些 wrapper。#135 的 private-static 額外方法清單不涵蓋這些 public bridge，不能直接豁免，附錄保留所有方法全文差異。 |
| `NativeDeClientViewer.sendPackets` | 正式 bytecode 將 receiver／字串／陣列先留在 operand stack，再呼叫 void requirePublication，之後才呼叫 CombatAccess.call；目前 Java 以區域陣列保存參數，仍 DIFF。未自行新增 helper 或 effects 核准，請主控決定此特定表達障礙。 |

上述未決問題不免除仍可修改的來源差異。`NativeDeDeletionState.sourceDeleteNpc`、`NativeDeInstanceState` 的其餘同步／欄位 owner／分支差異仍待本機續修；沒有宣稱全部都是 javac 無法表達。完整剩餘集合為 25 個只有類別／欄位差異、8 個包含方法差異。

## 編譯與中間診斷結果

環境：Microsoft JDK 17.0.17、Python 3.12、Windows。一般沙箱無法初始化，已經核准的提升執行僅在工作樹寫檔。未啟動遊戲、登入器、伺服器，未連正式 DB；未讀機密設定、封存或客戶端。stub 僅供編譯，不代表執行期實作。

stub build、semantic-diff javac、49 份 staged javac、打包、semantic-diff、Dump.java 及第二次獨立 reproduce 執行皆 exit 0。語意工具 exit 0 只表示報告產生成功，不是驗收成功。

```text
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
entries 185 missing [] extra []
SUMMARY SAME=152 SAME_EFFECTS=0 DIFF=33 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=18 ALLOW_METHODS=0
classSelection.missingLeft=[]
classSelection.extraLeft=[]
a01.jar SHA == reproduce.jar SHA
```

| 輸入／產出 | SHA-256 |
|---|---|
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `scratch/issue-72/target/a01.jar` 與 `target/reproduce.jar` | `ba50147a61ce3f50a08aae1ea0f35aba09fd19efdb1a44b3db37d4c4d7fcea49` |
| A01 verify_entries（UTF-8 LF） | `8c883895819b752ae9c9b70b2cef771ce7f44bc23cacfc25c19c197109c79499` |
| 49 份來源 SHA 清單 | `c0d078324b75560ff104b78aceb9696cb425c8ac9953531ea575b4c7431c0c7f` |
| semantic-diff ClassComparer.java | `40d6e0bb103120e454e3de2edaf312625001b361dc331673ff2c35c3610253b6` |
| 正式 Instance javap 全文 | `503b25e739005da07126e8a648b2c9c752324a9ab954794c0e358a6f0c5f303c` |
| 正式父類 javap 全文 | `f857674af532206b12db372cb7e4c0252fe62a43858d9e95df9907960794b284` |

## 可直接重現的步驟

在 repo 根目錄執行（所有生成內容位於目前工作樹；不需要原專案私有資源）：

```powershell
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
New-Item -ItemType Directory -Force scratch/issue-72 | Out-Null
& ./lito伺服器/LiTo381-Server/tools/legacy-stubs/build.ps1
if ($LASTEXITCODE -ne 0) { throw 'stub build failed' }
```

將以下程式存為 `scratch/issue-72/reproduce.py`，執行 `py -3 -B -X utf8 scratch/issue-72/reproduce.py`。ASM 9.7 來自既有本機 Maven cache；正常工具建置入口亦使用相同版本。最後斷言僅檢查本輪現況，正式閘門仍須 SAME=185、DIFF=0。

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
assert r['counts']['SAME']==152 and r['counts']['DIFF']==33
```

## 產出路徑與給下一棒的提示

- Git 產出：本報告 `docs/reports/issue-72.md` 及指定 staged 目錄內 19 份修改；整批共有 49 份來源。未修改 `docs/REPORT.md`。
- 本機證據：`scratch/issue-72/` 下的 `semantic.json`、`reproduce.json`、`left/`、`right/`、`full.diff`、`javac.log`、`verify_entries.txt`、`source-sha256.txt`、五份 `*.javap.txt`；classes／JAR 在 `target/`。這些 ignored 證據沒有強加進 git；必要全文在本報告附錄。
- 主控 Claude：先裁定非 Object 建構子，並處理上表三項新揭露的結構／表達問題。已核准的 get_showId、switch-map 與 metadata 不必重新裁定。
- 調度器：本批依賴應包含 #135、#153；尚未合併前不得因 CI success 或編譯成功標示完成，也不應立即重複派驗收。
- 本機執行器：依賴合併後沿用 task/72，取得正式工具與核准清單，再處理附錄中剩餘分支／try／欄位 owner 差異。用 staged Java 直接編出的產物重驗全部 185 SAME，更新本報告與 PR #99 描述；不得以覆蓋 class 或後處理達標。
- 未驗證：依賴合併後的最終語意閘門、單元測試／回歸及實機；本批未改 src/main，單元／回歸依規定在 A-land 統一執行。未合併 PR、未關閉 Issue。

## 剩餘 33 個 entry

| Entry（省略 local/lito381/server/ 與 .class） | 不同方法數 | 分類 |
|---|---:|---|
| `NativeDeCachedFactoryServices` | 0 | 僅類別／欄位屬性 |
| `NativeDeCharacterActor` | 0 | 僅類別／欄位屬性 |
| `NativeDeCharacterFields$ViewerHpState` | 0 | 僅類別／欄位屬性 |
| `NativeDeCharacterFields` | 4 | 含方法差異 |
| `NativeDeChatBroadcast` | 1 | 含方法差異 |
| `NativeDeChatChat` | 0 | 僅類別／欄位屬性 |
| `NativeDeChatChatGlobal` | 0 | 僅類別／欄位屬性 |
| `NativeDeChatChatShouting` | 0 | 僅類別／欄位屬性 |
| `NativeDeChatTable` | 0 | 僅類別／欄位屬性 |
| `NativeDeChatTimer$RunSequenceTask` | 0 | 僅類別／欄位屬性 |
| `NativeDeChatTimer` | 0 | 僅類別／欄位屬性 |
| `NativeDeClientViewer` | 1 | 含方法差異 |
| `NativeDeControlCatalog$Names` | 0 | 僅類別／欄位屬性 |
| `NativeDeControlCatalog$Reporter` | 0 | 僅類別／欄位屬性 |
| `NativeDeControlCatalog` | 0 | 僅類別／欄位屬性 |
| `NativeDeDeletionDeState` | 0 | 僅類別／欄位屬性 |
| `NativeDeDeletionPorts$Services` | 0 | 僅類別／欄位屬性 |
| `NativeDeDeletionState` | 2 | 含方法差異 |
| `NativeDeDeletionVisibility$1` | 1 | 含方法差異 |
| `NativeDeDeletionVisibility$Snapshot` | 1 | 含方法差異 |
| `NativeDeDeletionVisibility` | 1 | 含方法差異 |
| `NativeDeFactoryControls` | 0 | 僅類別／欄位屬性 |
| `NativeDeInitializerKernel$Actor` | 0 | 僅類別／欄位屬性 |
| `NativeDeInitializerKernel$Services` | 0 | 僅類別／欄位屬性 |
| `NativeDeInstanceBroadcast` | 0 | 僅類別／欄位屬性 |
| `NativeDeInstanceName` | 0 | 僅類別／欄位屬性 |
| `NativeDeInstanceState$DeAiWorkerTask` | 0 | 僅類別／欄位屬性 |
| `NativeDeInstanceState` | 319 | 含方法差異 |
| `NativeDeItemAbility` | 0 | 僅類別／欄位屬性 |
| `NativeDeItemBlessStatsTable` | 0 | 僅類別／欄位屬性 |
| `NativeDeItemCommonEnchantTable` | 0 | 僅類別／欄位屬性 |
| `NativeDeItemEnchantBonusTable` | 0 | 僅類別／欄位屬性 |
| `NativeDeItemEquipProcTable` | 0 | 僅類別／欄位屬性 |

## 完整正規化差異

以下使用目前 repo 的 ClassComparer 原有正規化，按方法列出全部 unified diff（含方法屬性與 try 表，三行共同上下文）；未將本輪自行推測的等價規則加入工具。`-` 是 staged，`+` 是正式版。附錄僅去除展示行尾空白，原始文字證據仍保留；只有名稱不同的 lambda 仍照實顯示 ONLY_LEFT／ONLY_RIGHT 方法內容，但類別狀態維持 DIFF。全 33 個 DIFF 的集合已與 semantic.json 精確核對。

### `NativeDeCachedFactoryServices`

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

### `NativeDeCharacterFields$ViewerHpState`

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

### `NativeDeChatBroadcast`

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

### `NativeDeChatTimer$RunSequenceTask`

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

### `NativeDeClientViewer`

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

#### `method sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V`

```diff
--- staged
+++ formal
@@ -105,6 +105,9 @@
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L8
+ALOAD 4
+CONST str:"sendEncryptedFrame"
+CONST str:"([BLjava/lang/String;)V"
 CONST Integer:2
 ANEWARRAY java/lang/Object
 DUP
@@ -118,13 +121,8 @@
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
 ALOAD 6
@@ -132,11 +130,11 @@
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

### `NativeDeControlCatalog$Names`

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

### `NativeDeDeletionDeState`

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

#### `method sourceDeleteNpc()V`

```diff
--- staged
+++ formal
@@ -5,23 +5,23 @@
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
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.releasePairedCorpseClaim()V
-ALOAD 0
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.getMaxHp()I
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.resurrect(I)V
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.isDead()Z
-IFNE L16
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.sourceResetDeletionObservation()V
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.deletionClaimFlag()Ljava/util/concurrent/atomic/AtomicBoolean;
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

### `NativeDeDeletionVisibility$1`

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
@@ -57,7 +57,7 @@
 LABEL L11
 ASTORE 0
 LABEL L12
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.REMOVE_PUBLISHED:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:5
@@ -67,7 +67,7 @@
 LABEL L14
 ASTORE 0
 LABEL L15
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$Phase.ACTIVE:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
 CONST Integer:6
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

#### `method deletionInProgress()Z`

```diff
--- staged
+++ formal
@@ -3,7 +3,7 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 default=
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeDeletionVisibility$Snapshot._phase:Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
```

### `NativeDeDeletionVisibility`

#### `method classifyRepair(Llocal/lito381/server/NativeDeDeletionVisibility$Snapshot;ZJ)Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;`

```diff
--- staged
+++ formal
@@ -8,7 +8,7 @@
 GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.LEAK:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
 ARETURN
 LABEL L0
-GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I
+GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Snapshot.phase()Llocal/lito381/server/NativeDeDeletionVisibility$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$Phase.ordinal()I
```

### `NativeDeFactoryControls`

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

### `NativeDeInstanceBroadcast`

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

### `NativeDeInstanceState$DeAiWorkerTask`

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

### `NativeDeItemBlessStatsTable`

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

### `NativeDeItemCommonEnchantTable`

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

### `NativeDeItemEnchantBonusTable`

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

### `NativeDeItemEquipProcTable`

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

## 正規化全文擷取方式

將以下內容存為 `scratch/issue-72/Dump.java`；此工具只讀 JAR，呼叫現有 ClassComparer，再寫文字，沒有 class bytes 寫入功能。

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

執行下列 Python（根目錄執行；reproduce.py 建立的檔案必須已存在）：

```python
from pathlib import Path
import subprocess
s = Path('lito伺服器/LiTo381-Server').resolve()
w = Path('scratch/issue-72').resolve()
j = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
asm = ';'.join(str(Path('D:/dev-cache/maven/repository/org/ow2/asm')/n/'9.7'/f'{n}-9.7.jar') for n in ['asm','asm-tree'])
tc = s/'tools/semantic-diff/target/classes'
subprocess.run([str(j/'javac.exe'),'--release','17','-encoding','UTF-8','-cp',f'{tc};{asm}','-d',str(w/'target/diagnostics'),str(w/'Dump.java')],check=True)
for side, jar in [('left',w/'target/reproduce.jar'),('right',s/'reference/base-0.6.71.jar')]:
    subprocess.run([str(j/'java.exe'),'-cp',f'{w}/target/diagnostics;{tc};{asm}','local.lito381.semanticdiff.Dump',str(jar),str(w/'verify_entries.txt'),str(w/side)],check=True)
```
