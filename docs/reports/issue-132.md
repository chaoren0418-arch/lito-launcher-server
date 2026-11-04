# Issue #132 — T020i semantic-diff 正規化與核准副作用比較

## 做了什麼

- 數值常數依型別／值正規化：ICONST、BIPUSH、SIPUSH、LDC，以及 long／float／double 對應形式。浮點數保留正負零與 NaN payload。
- 方法及欄位比較只遮掉 `ACC_SYNTHETIC`；類別旗標及其他旗標維持比較。
- catch-any 與 `java/lang/Throwable` 統一，catch 順序、其他類型仍比較。
- 新增明確指定的 `--effects-methods <file>`：依序比呼叫、欄位讀寫、ASTORE／陣列寫入、NEW、ATHROW、MONITOR*、RETURN、CHECKCAST／INSTANCEOF；另比常數多重集合、catch 數量與類型、分支數。保留方法宣告與註解比較。IINC 增量及 switch case 值亦納入常數集合。
- JSON／stdout／Markdown 分開統計 SAME 與 SAME_EFFECTS；JSON `classes[].effectsMethods` 及 Markdown 表格獨立列核准方法，即使類別因其他差異而 DIFF 也不短路省略。
- 與 `--allow-methods` 同列時依 allow-right 比較；精確原始方法名、描述子、選取範圍、目標方法存在性都必須成立。清單不會豁免缺少方法。
- 依[主控 #70 裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/70#issuecomment-6640488851)初始化指定兩條核准條目，沒有新增其他正式核准方法。

## 已驗證

### 本機建置與自我測試

- 基底提交：`7c6bee713ca1422cd904f88a2ad39121977dc9e3`；工作分支 `task/132`。
- Microsoft OpenJDK 17.0.17、Maven 3.9.16、ASM 9.7。
- 工具 Maven `-o -B -DskipTests package`：exit 0，BUILD SUCCESS。
- 伺服器 Maven `-o -B -DskipTests package`：exit 0，BUILD SUCCESS；含 testCompile，僅建置，沒有啟動伺服器或執行伺服器測試。
- `NormalizationSelfTest`：exit 0，`NORMALIZATION SELFTEST ALL PASS checks=275`。測試經真實 JAR 輸入及 CLI parser／JSON／Markdown 輸出，覆蓋每個正規化正反例、未核准與同類別未列入方法、DUP2／SWAP、順序／常數次數／型別／catch／分支／宣告差異、全部陣列寫入 opcode、錯誤清單、allow-right 疊加及精確合成原名守門。
- 正式基準 JAR 自比對且啟用正式核准清單：`SAME=2553 SAME_EFFECTS=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=2554`；ClientSession 的兩個方法各為 SAME_EFFECTS。類別數 1 與方法數 2 明確分開。
- `git diff --check` 通過。修改範圍為比較器、自測、核准清單、本報告及任務驗收指定的 `docs/server-semantic-gap.md`。

本機 Maven repository 複製至本 worktree `scratch/issue-132/maven-repository` 後離線使用，避免寫原始專案／外部快取。測試 fixture 是 ASM 比較器用資料；部分反例刻意含 JVM 不可執行的 opcode 組合，只驗證結構比較，不宣稱通過 JVM verifier 或遊戲執行驗證。

### 遠端 CI 證據

程式提交 `bdefa7bf1342af5a4b6fb8f5225b942213c4ce62` 的 [Semantic gap job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41920216393/job/126890425780) 已實際 SUCCESS。已核對步驟與完整日誌，不只看 non-blocking 工作流總狀態：

```text
NORMALIZATION SELFTEST ALL PASS checks=275
=== SELFTEST ALL PASS ===
=== FALLBACK ALL PASS ===
SUMMARY SAME=2064 SAME_EFFECTS=0 DIFF=485 ONLY_LEFT=1380 ONLY_RIGHT=5 BYTE_SAME=1008 ALLOW_METHODS=0
```

原始證據見 [server-semantic-gap artifact](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41920216393/artifacts/13366218347)。既有 workflow 的全 JAR 比對未傳 effects 清單，所以上列為預設正規化統計；effects 的正反例由該 job 的 selftest 9 驗證，正式清單的本機結果見下表。全套建置／伺服器測試／打包的最後提交狀態以 [PR #142 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/142/checks) 為準。

### 同一對 JAR 的正規化前後比較

本輪重新編譯一次伺服器 JAR，再分別用基底提交的舊比較器、新比較器預設、新比較器加核准清單，三次皆 exit 0。

| 比較器 | SAME | SAME_EFFECTS | DIFF | ONLY_LEFT | ONLY_RIGHT | BYTE_SAME |
|---|---:|---:|---:|---:|---:|---:|
| 修改前 | 2064 | 不支援 | 485 | 1380 | 5 | 1008 |
| 新預設正規化 | 2064 | 0 | 485 | 1380 | 5 | 1008 |
| 新預設＋核准 effects | 2064 | 0 | 485 | 1380 | 5 | 1008 |

類別狀態變更 **0**；36 個類別的首差異文字改變（包含常數表示正規化），不代表消除 36 個類別差異。不能將測試 fixture 的改善當成目前 src/main 已移植完成。

| 輸入 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/target/LiTo381-Server.jar` | `caf84dbc46e5090b89129c2ec1f7ba3d5f02d87681f297708ca9b8a9d366bcd5` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |

目前 Maven src/main 對正式版的核准方法結果仍為 DIFF：

- `exitRoomBeforePolyMe(Z)V`：第一個欄位讀取為 `rentalTownRoomState`，正式版為 `phase`，副作用序列不同。
- `sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V`：左側缺少方法。

這是現有 src/main 對正式版的真實缺口，不是本工具驗收失敗，也不是 B09 staged 的驗收結果。

### 重跑方式

在 repo 根目錄的 Bash 環境執行（CI 已有這兩支自測入口）：

```bash
tool='lito伺服器/LiTo381-Server/tools/semantic-diff'
bash "$tool/selftest.sh"
bash "$tool/selftest-fallback.sh"
mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package
bash "$tool/semantic-diff" \
  --left lito伺服器/LiTo381-Server/target/LiTo381-Server.jar \
  --right lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  --effects-methods docs/combat-merge/effects-approved.txt \
  --json scratch/issue-132/effects.json --md docs/server-semantic-gap.md
```

本機 Windows 不具 Bash；本輪新測試直接以 javac／java 執行，既有完整 Bash selftest 與冷啟動 fallback 交由 PR CI 執行。Windows 直接重跑新測試（repo 根目錄，JDK 17 在 PATH）：

```powershell
$tool = 'lito伺服器/LiTo381-Server/tools/semantic-diff'
$sources = Get-ChildItem "$tool/src/main/java" -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
javac --release 17 -encoding UTF-8 -cp "$tool/target/dependency/*" -d "$tool/target/classes" $sources
javac --release 17 -encoding UTF-8 -cp "$tool/target/classes;$tool/target/dependency/*" -d "$tool/target/test-classes" "$tool/src/test/java/local/lito381/semanticdiff/NormalizationSelfTest.java"
java '-Dfile.encoding=UTF-8' -cp "$tool/target/classes;$tool/target/test-classes;$tool/target/dependency/*" local.lito381.semanticdiff.NormalizationSelfTest scratch/issue-132/selftest "$tool/target/normalization-selftest"
```

前後比對可在 Bash 先取舊版本三個 Java 檔，不切分支、不更動工作樹程式碼：

```bash
mkdir -p scratch/issue-132/before-src "$tool/target/before"
for file in ClassComparer JarIndex SemanticDiff; do
  git show "7c6bee713ca1422cd904f88a2ad39121977dc9e3:$tool/src/main/java/local/lito381/semanticdiff/$file.java" \
    > "scratch/issue-132/before-src/$file.java"
done
javac --release 17 -encoding UTF-8 -cp "$tool/target/dependency/*" \
  -d "$tool/target/before" scratch/issue-132/before-src/*.java
java -cp "$tool/target/before:$tool/target/dependency/*" local.lito381.semanticdiff.SemanticDiff \
  --left lito伺服器/LiTo381-Server/target/LiTo381-Server.jar \
  --right lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  --json scratch/issue-132/before.json --md scratch/issue-132/before.md
```

新預設比較使用新版 CLI、相同 left／right，省略 `--effects-methods`，輸出 `after.json/md`。JAR 包裝時間會影響重新建置的檔案雜湊，前後三輪必須共用同一次建置產物。

## 推測

類別首差異仍有 innerClasses／未移植方法等項目，因此單純正規化不一定改變類別 SAME／DIFF 計數。只有後續各批 staged 真正完成內容移植後，才能驗證核准方法對目標是否達到 SAME_EFFECTS。

## 待驗證／未驗證什麼

- 未重建或驗收 #70 staged、未跑伺服器啟動／登入／遊戲、未連正式 DB。
- SAME_EFFECTS 只證明核准摘要一致，不證明資料流、呼叫參數、分支目的地或迴圈行為等價。

## 產出路徑與給下一棒的提示

- 程式與自測：`lito伺服器/LiTo381-Server/tools/semantic-diff/`。
- 正式核准清單：`docs/combat-merge/effects-approved.txt`。
- 更新快照：`docs/server-semantic-gap.md`；本報告：`docs/reports/issue-132.md`。
- 本機證據：`scratch/issue-132/{before,after,effects,reference-self}.json`、`normalization-selftest.log`、`{tool,server}-package.log`；編譯與 fixture JAR 在各工具／伺服器 `target/`。生成物不入 git。
- Claude 審查本 PR 後，由使用者合併；#70／B09 執行器在相依工具合併後，以真正 staged 產物、核准的兩條 effects 清單重跑，不得將目前 Maven gap 或正式 JAR 自比對當作 staged 通過。
- 同時使用 `--allow-methods` 時，先裁切 effects 清單至本批；驗收必須檢查 DIFF／ONLY_* 與 entry 完整性，並獨立看核准方法結果，不能只看退出碼或 SAME_EFFECTS 類別數。
