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

## T020i-4 預設正規化（Issue #166）

以下六項預設開啟，不需要新增核准條目；修改只發生在比較器記憶體，不輸出或改寫應用程式 class。

1. **轉呼叫 lambda**：標準 `LambdaMetafactory.metafactory` 的 implementation handle 若指向本類 `private static synthetic` 方法，其有效指令必須恰為依參數描述子載入所有參數、一次 static／virtual／interface 呼叫、對應回傳。接收者型別、引數及回傳型別精確一致；同步／其他旗標、註解、try、額外指令、重排引數均拒絕。整個輸入 JAR 與明確 classpath 中若還有直接呼叫、LDC／condy handle、其他 bootstrap 或外類參照，整個轉換拒絕。只有全部參照均被替換才移除方法；核准清單指定的精確方法保留。
2. **相鄰 try 合併**：先執行 #153 邊界收斂，合併相同例外型別與 handler、有效指令邊界相接的區間；不得跨過有效指令，也不得越過會改變例外表優先序的重疊 handler。有型別註解時保持原狀。
3. **stack／local 暫存**：先嘗試下述局部改寫，改寫後仍比較完整指令／try 表或既有精確核准的 CFG。未通過時再用原有 singleton 資料流證明：ASM 確認 local web 恰有一次寫入且每次 LOAD 均來自該寫入，才將 STORE／LOAD、DUP／DUP2 與純 stack 丟棄轉為內部運送關係；逐一核對運算、呼叫、欄位、monitor、分支、回傳的有序來源、每個來源的複本數、其餘指令順序及正常／例外控制邊。此第二條證明仍要求每個保留 operand 與被複製值恰有一個已知 producer；多來源 union 一律拒絕，包含 layout 核准模式。沒有恢復 union 集合放行。覆寫、錯誤取值位置、呼叫順序改變與複本數不符仍 DIFF；JSR／RET、未解析值與 local 型別註解保守拒絕。一般多來源跨分支暫存尚不支援，不能推論所有實際等價方法都會通過。
4. **捕獲參數排列**：僅處理本類 private static lambda；所有使用點必須是標準 metafactory，捕獲值之前必須是連續純 xLOAD，不能跨標籤或含方法／欄位求值。按本體首次使用捕獲參數的順序，同步置換本體參數槽、implementation 描述子、呼叫點描述子及 LOAD。沒有使用的捕獲參數、額外參照、metadata 或描述子碰撞時不改。
5. **local webs**：以 ASM reaching definitions 合併同一讀取可達的定義，包含分支、迴圈與例外邊；各 web 按首次出現順序獨立編號，保留參數入口與 wide 寬度。重用槽與另開槽只有在值依賴一致時相同。無法證明、舊式 subroutine 與 local 型別註解保守維持原規則。精確核准建構子仍使用實體槽，避免干擾 #153 跨 super 的寫入守門；原有核准 effects 摘要也使用 web 切分前的指令。
6. **field owner**：按本側 classpath 的宣告、介面、父類次序解析；只有解析成功才使用宣告 owner，保留名稱、描述子與 static／instance 類別。子類遮蔽會解析到不同宣告，缺少祖先則以 `unresolved(原 owner)` 表示，避免未解析符號偶然等於對側已解析宣告而誤判。JDK `java/*` 只讀取平台 class metadata，不載入或初始化應用類別。

`LocalTransport` 的兩個局部證明均要求非參數 web、唯一寫入、所有讀取的 reaching definition 恰為該 STORE、型別匹配，且沒有重疊 wide 存取、IINC 或被移除指令的註解：

- **單次讀取的直線區段**：`xSTORE n; ...; xLOAD n` 之間不可有正常分支或額外的分支／handler 入口（LOAD 位置也不可是入口）。將區段入口 stack 清空後，以 ASM Frame 逐指令執行；不得 underflow，抵達 LOAD 時必須再次為空。這證明中間每個 operand 都由區段內產生，完全不碰 STORE 下方的 stack 前綴；移除 STORE／LOAD 後，保存值只會留在前綴上方，恰在原 LOAD 處供後續使用。呼叫、欄位與 monitor 保持原順序。例外會在兩種形狀都清空 stack，唯一讀取限制保證 handler 無法觀察被移除的 local。STORE 的值可以來自多個前驅：不合併或重排那些前驅，而是保留完整原 CFG。此規則涵蓋 `Main#main` 跨 `SourceLogin.start` 的 `System.out`。
- **null 檢查後立即回傳**：只將連續 `ASTORE n; ALOAD n; IFNULL target; ALOAD n; ARETURN; target: ...` 換為 `DUP; IFNULL target; ARETURN; target: POP; ...`。n 恰有上述兩次讀取，中間不可有其他入口，target 必須緊接 ARETURN，且其唯一正常前驅就是該 IFNULL，亦不可是 handler（包含同位置的別名標籤）。非 null 路徑回傳同一值；null 路徑丟棄複本後恢復原 stack。後面的 switch／分支匯合完整保留，因此 `expiry` 可通過，而互換 switch arm 或取值的反例仍 DIFF。

每次改寫後重新分析 frames；分析失敗不構成成功證明。兩側完成局部改寫後，預設須逐指令與 try 表完全相同；只有原本列入 `--layout-methods` 的方法才使用保留分支角色、例外順序及槽位雙射的全圖比對。局部改寫不使用多來源 union 判定兩值相等，也不把一般方法自動升格為 layout 核准。`-Dsemanticdiff.disable=transport` 同時停用這兩條證明。

左側 staged JAR 不包含父類時，必須提供它的實際編譯／執行依賴。左右側不會自動借用對側 JAR；依賴不增加比較 entry，也不能覆蓋本側主 JAR 的類別。旗標可重複，先出現的依賴優先；接受 JAR 或 class 目錄。`--classpath` 是 `--left-classpath` 的別名。JSON／Markdown 記錄使用的 classpath。

```bash
bash tools/semantic-diff/semantic-diff --left staged.jar --right reference.jar \
  --left-classpath reference.jar --classes entries.txt \
  --json scratch/result.json --md scratch/result.md
```

ASM 9.7 新增 `asm-analysis` 依賴，Maven 與無 Maven fallback 都有接線及 SHA-256 校驗。`selftest.sh` 會執行 `FourthSelfTest`，包含六項正反例、跨類參照、例外表優先序、per-value 複本數、wide、diamond／loop／handler webs，以及隔離 ClassLoader 下實際執行的 JVM fixtures。

第三次修正新增 CLI 回歸：expiry 四路匯合、STORE 收到合併值、Main 暫存＋layout 皆含左右交換與停用 transport 的 DIFF 對照；Main 未核准仍 DIFF。switch arm 互換、呼叫／欄位順序變更、額外前驅、消耗 stack 前綴、帶註解的 STORE 均有反例，並執行局部改寫後的正常／例外路徑。三套 Java 自測共 709 項（517＋73＋119）；固定四批重跑證據見 `docs/reports/issue-166.md`。

診斷歸因可直接用 Java 的 `-Dsemanticdiff.disable=forward,try,transport,captures,webs,fields` 關閉指定規則；預設沒有關閉任何規則。此屬性僅供重跑前後對照，不是核准清單或忽略差異的入口。未知名稱不影響任何規則。

## T020i-5 預設正規化（Issue #174）

1. **例外涵蓋正規形**取代逐段 try 表比較（涵蓋 #153 邊界裁切與 #166 相鄰合併）。以有效指令位置識別 branch／handler 進入點，對每條可能拋例外指令列出依表順序涵蓋的 handler、例外型別及註解。未被涵蓋以空清單表示。區域變數讀寫、一般常數、stack shuffle、IINC、NOP、GOTO、條件跳轉、switch、非整數除法／餘數的算術與數值轉換不產生例外邊；無 MONITORENTER 的方法忽略 RETURN 系列覆蓋。IDIV／LDIV／IREM／LREM、欄位、呼叫、陣列、monitor、CHECKCAST 等仍保留。LDC 的 class／method handle／method type／condy 可能解析失敗，保守視為會拋例外。只用於 debug 的標籤不再改變指令身份。既有裁切／相鄰合併仍是前處理，但不再決定預設 try 相等性。
2. **多餘 null 檢查**僅移除 `DUP` 或 `ALOAD n`＋精確 `Objects.requireNonNull(Object)`＋`POP`。來源分析保留參數、local、stack 複製的值身份；控制流程固定點以交集計算「所有路徑都已成功取用」的證據。GETFIELD／PUTFIELD、實例呼叫、陣列操作、monitor 的成功正常邊可建立證據，例外邊只傳入原證據，不能把失敗的 receiver 取用當成成功。重新執行 producer（迴圈）會清掉舊值證據；覆寫 local 後使用新的來源。多來源 union 不作相同值證明；建構子、JSR／RET、local 型別註解、檢查三條指令間的額外入口及指令註解均保守拒絕。帶訊息／Supplier overload 不移除。
3. **區塊內表示式比較**：以分支目的地、分支後續及 handler 入口切基本區塊，按原順序比較每個區塊，沒有 CFG 重排或跨區塊值合併。入口 stack 依位置表示，入口 local 使用全方法一致的槽位雙射；每個非純運送操作成為依序編號的節點，包含有序 operands、正常分支、例外 handler。LOAD 使用當下 local 值，STORE／IINC 產生新的值版本，故跨覆寫取值仍不同。只有非參數 web 恰有一次 STORE、全部匹配 LOAD 位於同區塊且在 STORE 之後、沒有 IINC／註解／wide 重疊，才展開為被存入的表示式並移除運送。全方法掃描確保不存在區塊外或 handler 讀取；沒有讀取的 STORE 不刪。DUP／DUP2 展開同一節點並逐值核對複本數，出口 stack 保留順序，防止 predecessor／value 互換。其他運算、POP、呼叫、欄位讀寫、monitor 與會拋例外指令都保持順序；不合併兩次 GETFIELD。JSR／RET、未支援的 shuffle、不可達區塊與分析失敗回退既有比較。先嘗試原方法，再嘗試既有 #166 局部改寫的結果；不放寬原 singleton 多來源守門。

新診斷開關為 `-Dsemanticdiff.disable=coverage,nonnull,blocks`。`transport` 也會停用新區塊證明；各條證明可能重疊，單項停用後仍成功不能據此認定該規則沒有作用。

`--exceptions-exempt <file>` 預設不啟用。每行必須是 `類別#原名及完整描述子  # approved: https://核准連結`，可有空白行、整行註解及 UTF-8 BOM。僅忽略精確方法的 Exceptions（throws）屬性；指令、handler、方法存取、signature、註解及其他方法均照常比較。類別不在比較集合、方法任一側不存在、拼錯或缺少核准 URL 都報錯；不把 overload／其他合成方法視為核准對象。JSON 的 `exceptionsExempt` 與 Markdown 受控例外清單會列出實際使用條目。初始清單在 `docs/combat-merge/exceptions-exempt.txt`，只含 #62 核准的 `captureOutput`。

`selftest.sh` 新增 `FifthSelfTest`。正反例的 framed class bytes 同時用於實際 CLI 比對與隔離 ClassLoader 執行；含 try gap／呼叫覆蓋／handler 與優先序、null 全路徑／缺路徑／覆寫／例外回復／迴圈新值、LOAD 跨 IINC／STORE、暫存複本數／副作用順序／live-out，以及多來源區塊入口但不合併來源的案例。既有 #153／#166 審查反例繼續執行。
