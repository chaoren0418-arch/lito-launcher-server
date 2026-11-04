# Issue #135 — T020i-2 semantic-diff 正規化第二批

## 做了什麼

- 預設只比較本類別作為 inner 或 outer 的 InnerClasses 條目；引用其他巢狀類別不算差異。NestHost／NestMembers 仍比較。
- 遮掉類別 ACC_SUPER。右側沒有任何建構子時，只忽略左側唯一、無額外宣告 metadata／try-catch／指令的 Object 空建構子；反向與 overload 不豁免。
- 方法的非參數實體槽位依首次指令引用順序做一對一重編號，涵蓋 VarInsn、IINC、RET 及區域變數型別註解。this、所有參數格固定；long／double 兩格各有對應，輸出兩格 ID 以保留重疊與重用關係。
- 新增明確指定的 `--extra-methods`、`--metadata-exempt`，驗證精確原名／描述子、存取旗標與選取範圍；拒絕空清單、拼錯及萬用字元。JSON／Markdown 列出使用的例外。
- metadata 豁免只遮掉核准類別的 ACC_SYNTHETIC、EnclosingMethod、NestHost／NestMembers、自身 InnerClasses；其他屬性、直接內部類描述、欄位、方法照常比較。
- 依 [#77 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257)及 [#108 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/108#issuecomment-6643057522)初始化清單，沒有擴張核准範圍。

精確核准條目：

| 模式 | 類別與方法 |
|---|---|
| extra | `local/lito381/server/ServerConfig#rethrowSourceFailure(Ljava/lang/Throwable;)V` |
| metadata | `local/lito381/server/NativeDeVisibilitySwitch` |
| effects | `local/lito381/server/RuntimeInventory#copy()Llocal/lito381/server/RuntimeInventory;` |
| effects | `local/lito381/server/RuntimeInventory#sourceEquipmentOnly()Llocal/lito381/server/RuntimeInventory;` |
| effects | `local/lito381/server/RuntimeInventory#withoutObjects(Ljava/util/Set;)Llocal/lito381/server/RuntimeInventory;` |
| effects | `local/lito381/server/ServerConfig#applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V` |
| effects | `local/lito381/server/ServerConfig#load(Ljava/nio/file/Path;Ljava/lang/String;)Llocal/lito381/server/ServerConfig;` |

RuntimeInventory 為 **3 個方法、4 處 return**，withoutObjects 有兩處；依 #77 完整報告與審查留言核對，未虛構第四方法。

## 已驗證

### 本機編譯與自測

- 基底 `e76e1fe3009eb900cc0af3214e40ff5f1cd5bea6`，原有分支 `task/135`，未 checkout／建分支。
- Microsoft OpenJDK 17.0.17、ASM 9.7；javac 編譯工具與測試成功。
- `NormalizationSelfTest`：`NORMALIZATION SELFTEST ALL PASS checks=457`。原有 275 項加新增 182 項檢查，經真實 fixture JAR、CLI parser、JSON／Markdown 驗證。
- 新測試含每項正反例：自有 InnerClasses 差異、其他旗標、建構子額外指令／方向／overload／註解、參數固定、一對一槽位、wide 第二格別名、IINC／RET／型別註解、未列入 helper、非 private static、精確合成原名、allow-right 疊加、metadata 豁免後 `<clinit>`／欄位／直接子類描述仍有 DIFF。
- 伺服器 `mvn -o -B -DskipTests package` 成功（包含 testCompile，未執行伺服器測試）。Maven 3.9.16，快取先複製到本 worktree `scratch/issue-135/maven-repository` 後離線使用，沒有寫原始專案或外部快取。
- `git diff --check` 通過；沒有修改伺服器 src/main、pom.xml、docs/REPORT.md、機密設定、封存、正式資源或 release(s)。

ASM fixture 只測試結構比較；部分負向 opcode 組合刻意不滿足 JVM verifier，不宣稱是可執行遊戲程式。

### 同一對完整 JAR 的前後比較

以下三輪共用本次同一次 Maven 建置產物；「修改前」為基底 #132 比較器，非未正規化原始 byte diff。

| 比較模式 | SAME | SAME_EFFECTS | DIFF | ONLY_LEFT | ONLY_RIGHT | BYTE_SAME |
|---|---:|---:|---:|---:|---:|---:|
| 修改前 | 2064 | 0 | 485 | 1381 | 5 | 1008 |
| 新版預設 | 2122 | 0 | 427 | 1381 | 5 | 1008 |
| 新版＋effects＋metadata 清單 | 2122 | 0 | 427 | 1381 | 5 | 1008 |

58 個類別由 DIFF 變為 SAME，沒有反向退化；逐類變動收據在 `scratch/issue-135/status-changes.txt`。`docs/server-semantic-gap.md` 重產為新版預設模式，與 CI 未傳例外旗標的方式一致。Maven 原始產物尚無核准 helper，因此第三輪沒有傳 extra 清單；NativeDeVisibilitySwitch 缺少左側仍為 ONLY_RIGHT，metadata 清單不會掩蓋缺類。

| 輸入 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/target/LiTo381-Server.jar` | `a0ce642d7cae56a356e0be4bc1fa3829cf7c319303fb42ce7bce157a74aef2f3` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |

### #77 留存 staged JAR 的額外診斷

唯讀取得 `scratch/wt/task-77/lito伺服器/LiTo381-Server/target/a06/left.jar`，複製到本 worktree 工具 `target/a06-evidence.jar` 後重跑；雜湊 `feabd1d4219edd2fbdb54291233cf8c3ec47f9d26b15a06406a5443791152c66` 符合 #77 `b6d3f919184c585256963bc0e43b2fbc5be1c3a9` 收據。本輪未重新編譯 staged，這是工具診斷，**不是 #77 已完成移植的驗收**。

| 模式 | SAME | SAME_EFFECTS | DIFF | ONLY_LEFT | ONLY_RIGHT | BYTE_SAME |
|---|---:|---:|---:|---:|---:|---:|
| 修改前 | 61 | 0 | 8 | 0 | 0 | 3 |
| 新版預設 | 66 | 0 | 3 | 0 | 0 | 3 |
| 新版＋本批 effects＋extra | 66 | 1 | 2 | 0 | 0 | 3 |

三輪均精確 69 entries，missingLeft／extraLeft 皆空。五個預設改善類別為 NativePcActionAuthority、NativeSourceSkillDelay、NativeSourceSkillDelay$Lease、RuntimeInventory$Item、NativeSkillConsumptionBridges。

- RuntimeInventory 三個核准方法均 SAME_EFFECTS，類別亦 SAME_EFFECTS。
- ServerConfig.load 為 SAME_EFFECTS；applyExternalSourceServers 仍 DIFF：副作用序列第 2 格（從 0 起算）左側 ASTORE、右側 INVOKESTATIC ServerConfig.optional。核准「使用 effects 比較」不等於忽略 ASTORE／catch／呼叫差異；未放寬 #132 摘要規則。
- SourceLogin 仍 DIFF：lambda capture 描述子不同。該項主控未核准，照常保留。

### CI

程式提交 `088edc128986c5b16c32fa17a4f792b79390e00e` 的 [Semantic gap job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41926253216/job/126911314043) 已實際 SUCCESS；已核對每步狀態及完整日誌，不只看 non-blocking 工作流總狀態：

```text
NORMALIZATION SELFTEST ALL PASS checks=457
=== SELFTEST ALL PASS ===
=== FALLBACK ALL PASS ===
SUMMARY SAME=2122 SAME_EFFECTS=0 DIFF=427 ONLY_LEFT=1381 ONLY_RIGHT=5 BYTE_SAME=1008 ALLOW_METHODS=0
```

證據見 [server-semantic-gap artifact](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41926253216/artifacts/13369602199)。Server／Launcher 建置同輪已成功；此後補報告與清單 LF 行尾，沒有變更 Java 程式。最後提交的全部建置／Server tests／打包狀態以 [PR #152 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/152/checks) 為準。

## 推測

58 個完整 JAR 類別改善符合預設 metadata／槽位正規化預期；此數字只代表本次固定輸入。剩餘 DIFF 需要各 staged 批次修正，不能以比較器放寬代替移植。SAME_EFFECTS 只驗證主控核准摘要，不證明參數資料流與分支目的地等價。

## 待驗證／未驗證什麼

- 本機未具 Bash；完整 shell 自測與無 Maven 冷啟動 fallback 已由上述 Linux CI 實跑通過。
- 未重建 #77 staged、未驗收其 69 entry 全通過；ServerConfig／SourceLogin 的兩類差異仍由 #77 接手。
- 未啟動遊戲、登入器、伺服器，未連正式 DB；真機與整合回歸留既定任務。

## 重跑方式

在 repo 根目錄的 Bash／CI：

```bash
tool='lito伺服器/LiTo381-Server/tools/semantic-diff'
bash "$tool/selftest.sh"
bash "$tool/selftest-fallback.sh"
mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package
bash "$tool/semantic-diff" \
  --left lito伺服器/LiTo381-Server/target/LiTo381-Server.jar \
  --right lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  --json scratch/issue-135/after.json --md docs/server-semantic-gap.md
```

前後對照不必切分支：

```bash
mkdir -p scratch/issue-135/before-src "$tool/target/before"
for file in ClassComparer JarIndex SemanticDiff; do
  git show "e76e1fe3009eb900cc0af3214e40ff5f1cd5bea6:$tool/src/main/java/local/lito381/semanticdiff/$file.java" \
    > "scratch/issue-135/before-src/$file.java"
done
# selftest 已備妥 ASM 依賴於 target/dependency。
javac --release 17 -encoding UTF-8 -cp "$tool/target/dependency/*" \
  -d "$tool/target/before" scratch/issue-135/before-src/*.java
java -cp "$tool/target/before:$tool/target/dependency/*" local.lito381.semanticdiff.SemanticDiff \
  --left lito伺服器/LiTo381-Server/target/LiTo381-Server.jar \
  --right lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  --json scratch/issue-135/before.json --md scratch/issue-135/before.md
```

本機直接 javac／java 的 PowerShell 入口沿用 `docs/reports/issue-132.md`，將 scratch/issue-132 改為 scratch/issue-135；ASM classpath 可指向本機唯讀 Maven 9.7 JAR。重建 JAR 時封裝時間可能改變雜湊，前後必須共用同一份產物。

## 產出路徑與給下一棒的提示

- 程式／README／自測：`lito伺服器/LiTo381-Server/tools/semantic-diff/`。
- 清單：`docs/combat-merge/extra-methods-approved.txt`、`metadata-exempt.txt`、`effects-approved.txt`。
- 更新快照：`docs/server-semantic-gap.md`；任務報告：`docs/reports/issue-135.md`。
- 本機證據：`scratch/issue-135/{before,after,effects,a06-before,a06-after,a06-approved}.json` 及同名 md/log、`normalization-selftest.log`、`server-package.log`。生成物不入 git。
- Claude 審查本工具與核准邊界；#77 執行器依本報告修正剩餘兩類，若需要擴大 effects 摘要必須另交主控裁定，不能將「已核准使用 effects」誤寫成 SAME_EFFECTS 已通過。
- #108／A03 使用精確 metadata 清單重跑 NativeDeVisibilitySwitch；清單不豁免 `<clinit>`、缺類或直接內部類描述。各批先裁切清單至實際選取範圍，再驗證 DIFF／ONLY_* 與 entry 完整性。
