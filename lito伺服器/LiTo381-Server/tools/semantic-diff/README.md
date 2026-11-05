# ASM 語意比對

## T020i-3（Issue #153）

預設新增以下正規化，所有轉換只在讀取的 ASM tree 進行，不改寫輸入 JAR：

- `lambda$…$N` 用描述子與正規化內容的 SHA-256 識別，包含方法 metadata 與 try 表；相同內容的重複方法合併，呼叫及 bootstrap handle 同步引用內容識別。名稱前綴、數字與已遮罩的 SYNTHETIC 不影響識別。不同本體或不同呼叫目標仍 DIFF；循環 lambda 參照會報錯，避免遞迴失控。精確核准的原名仍固定，不會藉內容配對擴大授權。
- invokedynamic 的 bootstrap 參數只對「同一類別、private、非 static、非建構子」的實例 method handle 統一 H5/H7；外部 owner、public、一般呼叫指令與 LDC handle 保持原狀。
- Object 子類建構子的 `ALOAD 0; Object.<init>()V` 前移至開頭。直線前綴以堆疊追蹤確認 this 只作自身已宣告欄位的 PUTFIELD receiver，其餘計算不得使用 this；無法安全解析、分支或非法 this 用法報錯。非 Object 父類不套用。
- 每類 `$SwitchMap$` 合成欄位依 class 欄位表首次出現的順序重命名，宣告及跨類別欄位引用／handle 共用映射。不是各方法各自重編，因此交換兩個 switch map 的使用仍 DIFF。
- try 區間向內裁掉 LABEL／LINE／FRAME、NOP、GOTO、區域 xLOAD／xSTORE、數字／字串常數，以及方法內沒有 MONITORENTER 時的 return 系列。Class／MethodType／MethodHandle／condy 常數保留可能拋例外的邊界。空區間刪除，handler 不移動；未使用的標籤移除，同位置標籤共用識別。

新增三個必須明確傳入的精確清單：

```text
--access-widen docs/combat-merge/access-widen-approved.txt
--layout-methods docs/combat-merge/layout-approved.txt
--deviations docs/combat-merge/deviations.txt
```

一般格式為 `類別#方法描述子 # approved: <url>`；deviations 的建構子條目加前綴 `ctor-prologue-order `。接受 BOM、空白與註解，拒絕空清單、範圍外類別、拼錯或缺少的必要方法。先裁切到本批；不自動擴大到內部類。allow-right 仍優先決定有效比較目標。

`access-widen` 只允許左側較寬的可見性，其他 flags、內容與 metadata 照比。一般 deviation 只允許方法在左側多出；兩側皆有時照比，整類缺漏不豁免。`ctor-prologue-order` 只允許右側 super 前的自身欄位簡單賦值在左側改到 super 後，其他呼叫／計算仍保留；不包含 `NativeDeInstanceNames.name`。

layout 比較使用細分至指令的 CFG，收縮無註解的 GOTO 接邊，匹配指令、具分支角色的正常後繼、依表順序的例外後繼及 handler 型別／註解。保留不可達區塊並驗證全圖同構，區域槽位採全圖一對一映射、參數與 wide 別名固定；不忽略新增 ASTORE／ALOAD。搜尋超過上限或遇到舊式 JSR／RET 子程序時明確報錯。effects 與 layout／建構子例外重疊會拒絕。

`effects` 額外允許對側存在 DUP 時，省去單次寫入、單次讀取、同一直線片段且中間無其他寫入的 ASTORE 暫存。讀取兩次、沒有對應 DUP、標註的 store 均不放寬。這仍是明確核准方法的摘要比較，不是任意堆疊等價證明。

JSON／Markdown 分列 `SAME_LAYOUT`、`DEVIATION` 與逐方法 `approvedMethods`，即使整類 DIFF 也列出。類別狀態優先序為 DIFF、DEVIATION、SAME_LAYOUT、SAME_EFFECTS、SAME；各方法結果完整保留。交集須加總這五項，不可只算舊三項。`AdvancedSelfTest` 已接入 `selftest.sh`；無法達成 #76 預期 0 DIFF 的固定輸入差異與規格衝突見 `docs/reports/issue-153.md`。

使用 Java 17。Bash 入口優先使用 Maven，無 Maven 時以 javac＋通過 SHA-256 校驗的 ASM 9.7 建置；不影響伺服器 runtime 依賴。

```bash
bash tools/semantic-diff/semantic-diff --left A.jar --right B.jar \
  --classes entries.txt --exclude owned-by-B.txt \
  --allow-methods methods.txt --allow-right C.jar \
  --effects-methods effects-approved.txt \
  --json scratch/result.json --md scratch/result.md
```

所有篩選／允許旗標皆可省略。`--classes`／`--exclude` 接受檔案或逗號清單，類名可用斜線或點號並可帶 `.class`。`--classes` 精確選取 entry；`--exclude` 同時排除指定外層類及其 `$` 內部類。

使用 `--classes` 時，JSON 的 `classSelection.missingLeft`／`extraLeft` 和 Markdown 會另列左側缺少的指定 entry／左側多出的清單外 entry（套用 exclude）。多出項不加入語意比較計數；批次要求輸出恰好等於清單時，兩個陣列也必須為空。清單 entry 兩邊都不存在仍會直接報錯。

`--allow-methods` 每行為 `類別#方法名稱描述子`，方法名必須精確匹配 C.jar 的原始名稱（合成名稱於匹配後才正規化）。指定方法改對 C 比較，其他方法及類別／欄位屬性仍對 B 比較。指定方法只在 C 存在、A 尚未移植時會產生 DIFF；C 其他未指定方法不會被加入比較。清單拼錯、C 缺少方法、類別不存在或被 `--classes`／`--exclude` 排除都以非零退出，不產生成功摘要。共用大清單應先裁切成本批的允許方法。

成功產生報告時退出碼為 0（包括有 DIFF）；輸入無效或執行失敗為 1。批次驗收須檢查 JSON 的 DIFF／ONLY_LEFT／ONLY_RIGHT 計數皆為 0，不能只看退出碼。JSON 的 `firstDiff` 包含方法名稱描述子及從 0 起算的正規化指令位置（含 LABEL）；類別／欄位屬性或方法缺漏時位置為 null。`BYTE_SAME` 是原始 A/B 的 bytes 比較；套用 C 後即使 A/B bytes 相同仍可能 DIFF。

## T020i 正規化與核准副作用比較

預設將 ICONST／BIPUSH／SIPUSH／LDC 整數依值統一，LCONST／FCONST／DCONST 與同型別 LDC 亦同。型別不同仍不同；浮點數使用原始 bits，保留正負零與 NaN payload。只遮掉方法／欄位的 `ACC_SYNTHETIC`，類別旗標除下述 T020i-2 規則外，其餘旗標照常比較。catch-any（null）與 `java/lang/Throwable` 統一，其餘 catch 型別不變。

`--effects-methods` 必須明確指定檔案，預設不啟用。每行精確為 `類別#原始方法名稱描述子`，可有空白行、整行註解、UTF-8 BOM，以及行尾 `# approved: <url>`；不接受萬用字元。正式核准清單為 repo 根目錄 `docs/combat-merge/effects-approved.txt`，僅主控可新增條目。清單拼錯、目標缺少方法或類別不在本輪選取範圍時非零退出；批次使用前先裁切清單。左側缺少核准方法仍為 DIFF／ONLY_RIGHT，不會被豁免。

核准方法保留存取旗標、signature、exceptions 與註解比較，方法體改比：

- 依序的 INVOKE*（含 opcode、owner/name/desc；invokedynamic 保留 bootstrap 與參數）、欄位讀寫（opcode、owner/name）、ASTORE 與所有陣列 *ASTORE（保留 opcode、忽略本機槽）、NEW、ATHROW、MONITOR*、*RETURN、CHECKCAST／INSTANCEOF 的類型。
- 常數多重集合，保留型別、值、次數；涵蓋推入常數、IINC 增量與 switch case 值，後兩項各有標記。常數出現順序不比較。
- 依序的 try-catch 型別與區塊數，忽略 handler／範圍標籤；保留型別註解。
- 分支指令數，含條件跳躍、GOTO／JSR／RET、TABLESWITCH／LOOKUPSWITCH（每個 switch 計一次）。

與 `--allow-methods` 同列的方法依 C.jar 比較，其餘核准方法依 B.jar 比較。原始方法名稱必須精確相同，不因合成名稱重映射擴大授權。

核准方法成功一律標記 `SAME_EFFECTS`，不列入 `SAME`。JSON `classes[].effectsMethods` 與 Markdown「核准方法副作用結果」逐方法列結果，即使類別整體 DIFF 也不省略。類別只有在其餘比較亦通過時才是 `SAME_EFFECTS`；交集為 SAME + SAME_EFFECTS + DIFF，`counts` 為類別數，方法數應另查 `effectsMethods`。

此模式只確認核准摘要一致，不證明堆疊資料流、呼叫參數、分支目的地或迴圈行為等價。因此只可用於主控明確核准的方法；未列入清單的方法仍比較完整指令。BYTE_SAME 為原始 A/B 指標，不能直接用 SAME 減去 BYTE_SAME 推導套用 C／effects 後的結果。

```bash
bash tools/semantic-diff/selftest.sh
bash tools/semantic-diff/selftest-fallback.sh
```

後者在獨立 PATH（確實找不到 mvn）及含空白路徑內，冷啟動 CLI、下載與校驗依賴、javac 編譯、執行 SAME／DIFF 比較並驗證 build.sh。CI 會同時執行兩支測試。

工具依賴位於 `target/dependency/`，工具 classes 位於 `target/classes/`，測試 class/JAR 位於 `target/selftest/`；fixture、清單、JSON 與日誌位於 repo `scratch/semantic-diff-*`。fallback 的隔離工具及 fixture 位於 repo scratch，其編譯與依賴仍在隔離工具的 `target/`。這些生成內容不提交 git。

## T020i-2 預設正規化與受控例外

以下預設啟用：InnerClasses 只比較 `inner_class == 本類別` 或 `outer_class == 本類別` 的條目；引用到其他巢狀類別的描述條目忽略。NestHost／NestMembers 照常比較。類別存取旗標只額外遮掉 `ACC_SUPER`。

右側完全沒有 `<init>`、左側只有一個 `<init>()V` 時，僅允許其有效指令恰為 `ALOAD 0; INVOKESPECIAL java/lang/Object.<init>()V; RETURN`。不得含 try-catch、宣告例外、註解、額外指令或特殊存取旗標；方向相反、兩側都有建構子或有其他 overload 仍照常比較。

每個方法將非參數實體槽位依首次指令引用順序做一對一重編號；this 與所有參數格（含 long／double 的第二格）固定。VarInsn、IINC、RET 與區域變數型別註解共用對應。wide 指令同時輸出兩格的 canonical ID，保留第二格與其他槽位的重疊／重用關係；不會把一格映成兩格或合併不同槽位。不改變指令順序、型別或 IINC 增量。

兩個清單均須明確指定檔案，預設不啟用：

- `--extra-methods <file>`：每行 `類別#原始方法名稱描述子 # approved: <url>`。只忽略左側多出的精確 `private static` 方法；兩側都有時仍完整比較。指定方法缺少、不是 private static、類別被篩掉或拼錯會報錯，不能靠合成名稱重映射擴大授權；整個類別 ONLY_LEFT 不會被豁免。`--allow-methods` 指定的方法仍以 C.jar 驗收，不會被額外方法豁免。
- `--metadata-exempt <file>`：每行 `類別 # approved: <url>`。只對精確類別忽略類別 `ACC_SYNTHETIC`、EnclosingMethod、NestHost／NestMembers、InnerClasses 指向自己的條目；自己作為 outer 的其他條目、其餘類別屬性、欄位、方法仍比較。不自動展開 `$` 內部類，也不豁免缺少的類別。

兩份清單接受空白行、整行／行尾註解與 UTF-8 BOM；空清單、不存在的檔案、萬用字元及範圍外類別均拒絕。JSON 與 Markdown 列出使用的例外清單，結果仍需檢查所有 DIFF／ONLY_*，不是看 CLI exit 0。

核准清單中的精確原名＋描述子在 A／B／C 三側固定原名，不參與內容去重或合成名稱編號；其餘 lambda 依內容比較，access$ 依剩餘出現順序比較。呼叫與 method handle 使用同一對應。因此兩側同原名的核准方法仍比較內容，左側未核准方法不能借用核准方法的編號被消去；同名但不同描述子的 overload 不受該條目豁免。

主控核准檔為 `docs/combat-merge/{extra-methods-approved,metadata-exempt,effects-approved}.txt`。批次重跑前先裁切到本批實際具備的類別／方法，不能把缺少 helper 的 Maven 原始產物套用完整 extra 清單後當作成功。#77 的 RuntimeInventory 為 3 個方法、4 處 return；ServerConfig 為 2 個 effects 方法及 1 個 extra helper。這些清單只記錄核准範圍，不保證 staged 已達 SAME_EFFECTS；若 catch、呼叫序列或其他摘要不同仍會 DIFF。

```bash
bash tools/semantic-diff/semantic-diff --left staged.jar --right reference.jar \
  --classes entries.txt --effects-methods batch-effects.txt \
  --extra-methods batch-extra.txt --metadata-exempt batch-metadata.txt \
  --json scratch/result.json --md scratch/result.md
```

`selftest.sh` 的 NormalizationSelfTest 同時涵蓋 T020i 與 T020i-2。新增正反例含直接內部類、Nest metadata、非 SUPER 旗標、建構子方向／overload／註解、參數與 wide 槽位別名、IINC／RET／型別註解、清單 typo／排除／非 private static，以及 metadata 豁免後 `<clinit>`、欄位與子類描述仍不同的情況。
