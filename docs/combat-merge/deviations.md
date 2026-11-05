# 核准的刻意偏離

只有 `deviations.txt` 的精確類別、方法與描述子可使用。這是交付決策的紀錄，不代表正式版本來就有相同行為，也不代表單元測試已完成。比較報表另列 `DEVIATION`；其餘方法與屬性仍驗證。

## NativeDeWorldLog.debug

識別：`local/lito381/server/NativeDeWorldLog#debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V`。

- **與正式版的差異**：正式介面缺少這個方法；A-land 交付版補上 `default` 實作，記錄 debug log。本任務只建立比較規則與清單，不修改介面來源。
- **理由**：依 #73／#153 主控裁定，正式 `NativeDeItemGreaterTierTable` 在選用欄位讀取失敗時會呼叫此方法，因正式介面沒有它而可能拋出 `NoSuchMethodError`。核准交付版將此路徑改為只記錄 log。
- **單元測試要求（A-land，待執行）**：模擬選用欄位讀取失敗，確認呼叫 default debug、保留適當訊息與參數，不拋出 `NoSuchMethodError` 或其他例外；另驗證正常讀取路徑不多記錯誤、必要欄位失敗仍走原有失敗處理。
- **比較界線**：只在方法存在左側、有效比較目標缺少時豁免；兩側皆存在仍比較內容，整個介面缺失不豁免。
- **核准**：[Issue #153](https://github.com/chaoren0418-arch/lito-launcher-server/issues/153)，依 #73。

## NativeDeInstanceState 的建構子欄位賦值順序

下列五個精確建構子使用 `ctor-prologue-order`。類別共同前綴為 `local/lito381/server/`，完整描述子以清單為準；以下 C、T、N 分別指 `NativeDeInstancePorts$Construction`、`NativeDeNpcTemplate`、`NativeDeInitializerKernel$Name`。

| 建構子參數 | 與正式版的差異 | A-land 單元測試要求（待執行） |
|---|---|---|
| `(C,T)` | 允許 `this.construction` 的欄位賦值由 super 前移至後 | 核對建構完成後 construction 相同；覆寫父鏈可觀測的回呼，確認不依賴提前賦值 |
| `(C,T,N)` | 同上；不允許移動 `NativeDeInstanceNames.name` | 同上，加 Name 解析成功與失敗，確認失敗時父建構子執行順序 |
| `(C,T,N,boolean)` | 同上 | 分別測 true/false，核對名稱及初始化旗標；確認 Name 失敗順序 |
| `(C,T,N,boolean,String)` | 同上 | 覆蓋字串有效、空值及既有邊界；確認 Name 呼叫順序與父鏈觀測 |
| `(C,T,N,boolean,boolean,String)` | 同上 | 覆蓋兩布林的組合，核對 construction、名稱及父鏈副作用順序 |

**理由**：Java 建構子語法無法在非 Object 的 super 呼叫前直接賦值自身欄位；主控核准有限的交付偏離。父類 `NativeDeNpcConstructionState` 的建構子具有副作用，不能將這項偏離稱為無條件語意等價。

**比較界線**：只有自有欄位接收已計算好的區域值或簡單常數的賦值可移動；其他指令保留。方向為左側在 super 後、右側在 super 前；反向不豁免。`NativeDeInstanceNames.name(...)` 的呼叫位置不在例外內，四個含 Name 建構子的現有差異仍須來源端處理。未核准的建構子不套用此規則。

**核准**：[#72 主控留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935)。

## 驗證狀態

- **已驗證**：比較器 fixture 涵蓋左側額外方法／清單外方法／兩側同名內容不同、核准建構子賦值前後移動／反向／額外呼叫／Name 呼叫位置改變。
- **推測**：以上來源端修補可滿足交付需求；比較器結果不證明執行期父鏈回呼不會觀察到差異。
- **待驗證**：本文件列出的 A-land 執行期單元測試，以及介面 default 實作與五個建構子的最終來源。

## #72：Name 建構子的 Java 17 委派表達

依[同一核准留言第 2 項](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935)，追加一個 private 建構子：

```text
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V
```

四個公開 Name overload 先執行 `this(construction, NativeDeInstanceNames.name(construction, name), nativeDeNpcTemplate)`。private 建構子執行 super、寫入 construction 與共用初始化，公開 overload 再寫入各自旗標並呼叫 startNpc。Name 失敗時不會進入 private／父建構子。

新增建構子以一般 left-only deviation 列管；原五個 `ctor-prologue-order` 不擴大。新版比較器尚不展開建構子委派，因此四個公開 overload 仍為 DIFF，不代表已通過驗收。完整建構子鏈、正式 JAR 的子類清單（0 個）、虛擬方法解析與剩餘差異見 `docs/reports/issue-72.md`。A-land #78 仍須執行核准留言第 4 項兩個測試。
## #176：PhysicalNpc 的 int／boolean 方法偏離

兩條均由 [#65 主控留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/65#issuecomment-6655613888) 核准，僅作用於下列完整方法識別：

- `local/lito381/server/NativeSourcePhysicalNpc#sourceCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z`
- `local/lito381/server/NativeSourcePhysicalNpc#targetCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z`

**理由**：候選呼叫 int producer 後，以 `DUP; IFLT; IRETURN` 直接回傳非負 int；Java boolean 方法必須表達成 `state != 0`。負值兩側均先走既有 fallback。這是指定候選的方法級核准，並非一般 int／boolean 正規化。

**已驗證**：以 JDK 17 `javap -c -p -s` 檢查 SHA-256 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` 的候選，並逐一反向追蹤每個 `IRETURN` 的所有一般前驅，結果如下。沒有從方法名稱或返回型別推測值域。

| 對應核准方法 | producer | IRETURN offset | 所有可能運算元 |
|---|---|---:|---|
| sourceCurrent | `NativeSourceRawNpc.sourceState(Origin)I` | 25 | -1（offset 24 ICONST_M1） |
| sourceCurrent | 同上 | 224 | 0（223）、1（219 → GOTO 224） |
| targetCurrent | `NativeSourceGroundNpc.targetState(Origin)I` | 21 | -1（20 ICONST_M1） |
| targetCurrent | 同上 | 85 | 0（84）、1（80 → GOTO 85） |

兩個 producer 都只有上述兩個 IRETURN。其他路徑可能丟例外，沒有其他正常回傳值。兩個 consumer 的 offset 5 均為 `IFLT 9`，非負路徑 offset 8 IRETURN，offset 9 POP 後進 fallback，因此直接回傳的值域恰為 0／1。

**比較界線**：傳入 `--deviations` 才啟用 `method-diff`；精確匹配兩側原名與描述子，方法缺少、清單拼錯、未附核准 URL 均拒絕。整類其他方法與 metadata 照比；JSON `methodResults` 與 Markdown 另列 DEVIATION 和左右完整正規化方法內容，不混進 SAME。更換候選後應重做上述 producer 證明，不能沿用值域結論。

**推測／待驗證**：指定值域下兩種 boolean 表達相同；B-land 仍須依主控要求補 `sourceState`／`targetState` 回傳 0／1 時兩個 consumer 與候選一致的單元測試。未載入應用類別、啟動伺服器或連接資料庫。命令、稽核腳本與批次結果見 [issue-176 報告](../reports/issue-176.md)。

## #72：四個公開 Name 建構子與 compute 的 method-diff（2026-11-06）

依 [主控最新裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6657400173)，僅追加下列五個完整方法識別。前述舊紀錄中「四個公開 overload 仍為 DIFF」是舊工具的歷史狀態；本輪四個公開 overload 已依此裁定列為 `method-diff`。原有 `ctor-prologue-order` 與新增 private 建構子的 left-only 核准均保留。

```text
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V
local/lito381/server/NativeDeCharacterFields$CombatSkillDerived#compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;
```

- **四個 Name 建構子的理由**：依 #72 裁定，`NativeDeInstanceNames.name(construction, name)` 在 super 前求值；`this(...)` 委派給 private 建構子執行父建構、construction 賦值及共用初始化，公開建構子再執行各自的旗標設定與 startNpc。Java 的委派形狀不同，主控核准為方法級刻意偏離；construction 賦值由 super 前移後仍是原先有限核准，不宣稱字節碼或所有父鏈觀測無條件等價。
- **compute 的理由**：正式版參照不存在的 SKU1／SKU2／SKD1／SKD2／SKD3 欄位，Java 無法直接表達未解析欄位引用。保留六個區域值初始化、Iterable 迭代及 null 元素略過，在首個非 null 元素原 GETSTATIC SKU1 處明確 `throw new NoSuchFieldError("SKU1")`。後四個 GETSTATIC 永遠不可達，Java 不保留不可達敘述，也不補空表或替代技能值。根本原因另由 #184 調查。
- **比較界線**：`method-diff` 比較精確名稱及描述子，左右方法均須存在；其餘方法、欄位、metadata 與 entry 完整性照常驗證。169 個 public bridge 及 lambda 差異沒有加入任何豁免。
- **已驗證**：49 份 staged Java 以 Java 17 編譯，185 個 entry 完整；NativeDeCharacterFields 為 SAME，CombatSkillDerived 為 DEVIATION，四個公開 Name 建構子逐方法 DEVIATION。整個 Instance 類別仍為 DIFF，詳見 issue-72 報告。
- **待驗證（#78 A-land）**：construction 的父鏈可觀測性；四個 Name overload 的成功／失敗求值與初始化順序、各布林組合及字串邊界；compute 空集合及全 null 元素正常回傳，含非 null 元素拋 NoSuchFieldError。未執行 staged 應用類別、伺服器、遊戲或資料庫。

**2026-11-06 重驗狀態更新**：依 [最新主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6663641218)，169 個 bridge 與巢狀 lambda 的裁定已完成，兩者 Java 不改，分別交 #203 第 3、1 項工具規則驗證；不增加任何精確核准識別或整類豁免。本輪合併 main `9678e97`（含 #186）後，直接編譯全部 49 份 staged Java，185 entries 為 183 SAME／1 DEVIATION／1 DIFF，missingLeft/extraLeft 皆空，Instance 仍 DIFF。PR #215 於 2026-11-06 02:32（UTC+08:00）仍 OPEN、merged_at=null，因此等待 #203 合併及全量重驗，尚未通過最終驗收。既有五項 method-diff、private 建構子及 ctor-prologue-order 的精確核准識別均保留；#78 測試要求不變，詳見 [issue-72 報告](../reports/issue-72.md)。
## #75：Eye.canOverwrite 的 int／boolean 方法偏離

識別：`local/lito381/server/BattleDragonEyeCatalog$Eye#canOverwrite()Z`，類型 `method-diff`。依 [#75 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/75#issuecomment-6657773411) 保留 staged Java，僅核准此精確方法的表示差異。

**已驗證（2026-11-06）**：Microsoft JDK 17.0.17 的 `javap -c -p -s`，正式 JAR SHA-256 為 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。正式 consumer 的 offset 1 為 `GETFIELD canOverwrite:Z`，5 為字串常數 `"canOverwrite"`，7 呼叫 `CombatSpecialBuffs.eyeField(ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I`，10 直接 IRETURN。staged 將第一個參數寫為 `this.canOverwrite ? 1 : 0`，回傳寫為 `eyeField(...) != 0`。

`eyeField` 有且只有三個 IRETURN。它是多用途 int 方法，以下明確區分一般輸入與本次 consumer 的可達值域：

| IRETURN offset | 一般輸入的所有來源（保守值域） | `field="canOverwrite"` 的可達值域與理由 |
|---:|---|---|
| 10 | offset 9 的 ILOAD 0，任意 int | `{0,1}`；來自 `canOverwrite:Z` 欄位，staged 的條件轉換亦為 `{0,1}` |
| 42 | offset 39 的 `SourceSkillControls.ability(...)I`，不主張受限於 boolean | 不可達；`EYE_ABILITIES.get("canOverwrite") == null`，offset 27 必到 43 |
| 390 | case 0–6 在 313／320／327／334／341／348／355 呼叫的七個 int accessor（保守為任意 int），或 368 的 ICONST_1／372 的 ICONST_0 | `{0,1}`；字串 hash 分派到 248，253 的 equals 成功，259–261 設 case 7；265 的 switch 到 361，362 呼叫 `Config.overwrite()Z`，365 的 IFEQ 選擇 368 的 1 或 372 的 0，兩者都到 390 |

default 376–389 丟 `IllegalArgumentException`，沒有第四個回傳出口；本字串不會走 default。`config` 或其他呼叫丟例外時也沒有新增正常回傳值。

不可達證據：`EYE_ABILITIES` 為 private static final；javap 中只有 `eyeField` 的一次 GETSTATIC 與 `<clinit>` 的一次 PUTSTATIC。`<clinit>` offset 24–69 的八組 key/value 是 `meleeDamage/DMG`、`magicHit/MAGIC_HIT`、`damageReduction/DMG_REDUX`、`evasionRate/ER`、`stoneResistance/REGIST_STONE`、`sleepResistance/REGIST_SLEEP`、`freezeResistance/REGIST_FREEZE`、`poisonResistance/POISON_RESIST`，72 呼叫不可修改的 `Map.of`，75 寫入欄位；沒有 `canOverwrite` key，也沒有將此 map 暴露給其他方法。這項證明不把其餘 int accessor 或 ability 的結果誤判為 `{0,1}`。

依 [JVMS 17 §6.5 ireturn](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-6.html#jvms-6.5.ireturn)，boolean 回傳採 `value & 1`；[putfield](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-6.html#jvms-6.5.putfield) 寫入 Z 欄位也會收斂為最低位元。因此本 consumer 的可達集合 `{0,1}` 中，0 的兩種表達都為 false，1 都為 true；輸入端 `? 1 : 0` 亦保持欄位值。**證明成立，僅限這份正式 JAR 與這個呼叫的固定字串；不宣稱一般 int 的 `!= 0` 等同最低位元。**

**比較界線與待驗證**：只有上述方法列 DEVIATION，其餘 Eye 方法、欄位與 metadata 照比；沒有為 PoisonAtomic 增加豁免。更換 producer 或 map 後須重做值域證明。未執行遊戲或 DB；A-land 仍需補 PoisonAtomic 的 `leaveInstall` 拋例外時 monitor 釋放測試。可重現命令、原始輸出與全批結果見 [issue-75 報告](../reports/issue-75.md)。

## #73：NativeDeItemWeaponProcTable.reload 保留缺欄位失敗

識別：`local/lito381/server/NativeDeItemWeaponProcTable#reload()V`，類型 `method-diff`。依 [#73 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/73#issuecomment-6668320896)，移除 staged 額外的 `_instance` 欄位，不修復正式版的潛伏 bug；根本原因歸入 #184，不另開任務。

**已驗證（靜態）**：正式 JAR SHA-256 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` 的欄位表沒有 `_instance`，父類為 Object，但 `reload()` 在 offset 14 PUTSTATIC、17 GETSTATIC 仍引用它。staged 保留前面的 `requireLoading(this.nativeDefinitions)` 與 `new NativeDeItemWeaponProcTable()`，再明確 `throw new NoSuchFieldError("_instance")`。javap 確認 offset 0–11 的操作與順序相同；之後 POP 丟棄新物件並配置／拋出錯誤，與正式缺欄位解析的 bytecode 不同，因此只將精確 `reload()V` 列為 DEVIATION，其他欄位、方法、metadata 仍比較。

**呼叫調查**：掃描正式 JAR 全部 2,554 個 class 的直接呼叫、LDC method handle、invokedynamic bootstrap／參數及遞迴 ConstantDynamic，目標 `reload()V` 的呼叫／handle 均為 0。`NativeDeItemDefinitionsResources.lambda$read$0` 建立此表後經 `invokeLoad` 反射呼叫固定字串 `load`，沒有呼叫 `reload`。依這份 JAR 的靜態證據，正常啟動／遊玩不會走到本方法；外部呼叫或動態組合反射名稱不在此結論內。完整調查見 [issue-73 報告](../reports/issue-73.md)。

**待驗證（#78 A-land）**：在有效 loading context 下呼叫 `reload()`，斷言拋出 `NoSuchFieldError` 且訊息為 `_instance`；前置檢查及建構須按正式順序執行。若沒有相符 context、data 已發布或 SQL context 缺少，正式 `requireLoading` 會先丟 `IllegalStateException`，不可略過前置守門或把所有呼叫都斷言為 `NoSuchFieldError`。本輪未執行應用類別、DB 或此執行期測試。

## #235／#73：private 遮蔽祖先方法的改名配對

依 [#73 主控修正裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/73#issuecomment-6668602650)，新增唯一條目：

```text
local/lito381/server/NativeDeNpcConstructionState#cancelNpcChatTimer$own()V  private-rename  cancelNpcChatTimer  # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/73#issuecomment-6668602650
```

Java 無法用 private 遮蔽祖先的非 private 同簽名方法；正式 bytecode 的 private 方法不構成覆寫。左側 Java 改名為 `cancelNpcChatTimer$own()V`，同類的 `sourceChat$startChat(I)V` 呼叫新名字，並移除子類原名宣告，祖先 `NativeDeDeletionState` 的原名呼叫仍派送祖先版本，派送行為與正式版相同。不得改成 package／protected 或新增覆寫。

**已驗證**：兩側 private、祖先非 private 同簽名、左側無任何原名方法等守門通過後，仍完整比較本體與 metadata；只有成功配對列 DEVIATION。本類 INVOKEVIRTUAL／INVOKESPECIAL 呼叫與遞迴依配對比較，祖先／外類 owner 與 handle 不改名。#73 完整 49 份來源（其中一份使用審查者既有改名試編來源）重新編成 104 entry 後，該類為 DEVIATION，呼叫者為 SAME_PATHS，整批 0 DIFF／ONLY_*。隔離 ClassLoader 反例實際證明覆寫版本的派送結果不同，詳見 [issue-235 報告](../reports/issue-235.md)。

**推測／待驗證**：比較限於固定輸入與工具模型；未實機測試或連 DB。#78 仍須補「刪除 NPC 時呼叫 chat retire」的應用層派送測試，本條目不豁免該驗收。
