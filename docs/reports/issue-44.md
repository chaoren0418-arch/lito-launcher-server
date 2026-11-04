# Issue #44 / T020d — ASM 語意比對工具＋CI 語意差異報告

## 做了什麼

本輪完成第 3 次審查修正，延續 PR #48；只改工具、CI job、此報告與語意快照。工作分支維持 `task/44`，將既有遠端提交合入目前 worktree，未重新 checkout／建立分支。

- **允許方法不再漏比**：比較鍵包含 left／right 方法與經驗證的指定 allow-right 方法；A、B 都沒有而 C 有的指定方法判 DIFF。每項先以 C 的原始方法名稱＋描述子精確驗證，再正規化合成名稱。拼錯、C 缺少方法、類別不存在／未選取／被排除都非零退出，不輸出成功 SUMMARY。
- **方法替換仍受比對約束**：只有指定方法改跟 C 比；同類別其他方法及類別／欄位屬性仍跟 B 比。方法只在 A、C 有而 B 沒有可判 SAME；A 與 C 不同必須 DIFF，沒有人工豁免。
- **無 Maven fallback**：建置邏輯共用於 `build-support.sh`；下載與 SHA-256 診斷送 stderr，`classpath()` stdout 只含 classpath。冷啟動 CLI 與 build.sh 均可使用 javac。CLI 偵測 Java 原始碼較新時重建。
- **產物路徑**：fixture／清單／日誌／JSON 位於 repo `scratch/semantic-diff-*`；工具依賴／classes 在工具 `target/dependency`／`target/classes`，測試 class/JAR 在 `target/selftest`。隔離 fallback 位於 scratch，其產物仍位於自己的 target。已同步 ASM_CP，沒有再下載到 lib 或生成 build/selftest。
- **完整 entry 清單**：依 Issue 擁有者補充與 #43 第 4 版，`--classes` 精確選取含 `$` 的 entry、不自動展開內部類；JSON `classSelection.missingLeft`／`extraLeft` 與 Markdown 列出左側缺少／多出項目。指定 entry 兩邊都不存在仍明確報錯。
- **CI**：保留非阻斷 `Semantic gap (non-blocking)`，加入無 Maven PATH 的真實測試，使用 `mvn -B -DskipTests package`；artifact 同時保留 JSON／Markdown、測試輸出、CI checkout SHA、JDK 與輸入 JAR SHA-256。

## 怎麼驗證（已驗證）

### 本輪 CI 證據

- 程式提交：`ccaeb9855a37c174156ff9832d0cf5979c9da634`。
- [build run 41852194064](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41852194064)。
- [Semantic gap job 126651146719](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41852194064/job/126651146719)：success；兩支自我測試、伺服器 package、比較與上傳步驟全部 success。
- [artifact server-semantic-gap 13335192893](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41852194064/artifacts/13335192893)。ZIP SHA-256：`b21b48477b55537406ad5c07f9bc4011e38a437f743083bf75313737706c2719`；本機下載後已核對相同。
- CI 實際 checkout 為 PR 合併 SHA `9db2c8f126fd15c747009ca03527476f48294a1f`，不是把 branch SHA 冒充執行版本；見 artifact 的 `scratch/semantic-diff-revision.txt`。
- JDK：Temurin `17.0.21+9`；建置指令 `mvn -B -DskipTests package`。只宣稱上述 job 已成功，不把仍執行中的其他 job 當成通過。

輸入與原始報告 SHA-256：

| 檔案 | SHA-256 |
|---|---|
| Maven `target/LiTo381-Server.jar` | `150d72e7e12f2b8dce34576bac61f41d7062af43221830cf6005caa1fe41baf6` |
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| artifact `docs/server-semantic-gap.md` | `922668f4ec50a32fa9a15c4f704c426fc9bcd39ec029086f646d3d1af39e7eac` |
| artifact `scratch/server-semantic-gap.json` | `7f572f2ddfba8d177f8431e58850ed0aa20ce18d2d96c4897676859a86ed6c9e` |

提交快照只在 artifact Markdown 標題後插入來源註記，其餘逐字相同；JSON 有 485 個 DIFF，Markdown 的完整 DIFF 表同為 485 列。快照更新提交只包含文件，工具與 workflow 維持上述已測版本。

### 自我測試輸出

重跑（repo 根目錄）：

```bash
bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh
bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest-fallback.sh
```

以下取自本輪 artifact 的 `scratch/semantic-diff-selftest.log` 與 `scratch/semantic-diff-fallback.log`；逐案 JSON、失敗輸入 stderr 也包含於 artifact。

```text
PASS SELFTEST 1 SAME=2554 DIFF=0
PASS SELFTEST 1b SAME=3 DIFF=0
PASS SELFTEST 2 SAME=3 DIFF=0
PASS SELFTEST 3 DIFF=1
PASS SELFTEST 4 SAME=1 DIFF=0 (Fixture+Inner excluded)
PASS SELFTEST 5 Extra SAME via allow-right; Fixture DIFF still caught (SUMMARY SAME=2 DIFF=1 ONLY_LEFT=0 ONLY_RIGHT=0)
PASS SELFTEST 5b allow-right-only added()I DIFF=1 despite identical left/right bytes
PASS SELFTEST 5c unmatched allow entry exits non-zero without SUMMARY
PASS SELFTEST 5d same-class unlisted()I DIFF=1 insn=0 uses right, not allow-right
PASS SELFTEST 5e added method present in left SAME=1
PASS SELFTEST 5f absent/excluded/unselected allow classes exit non-zero
PASS SELFTEST 5g listed method differs from allow-right DIFF=1 (no waiver)
PASS SELFTEST 6a suffix SAME=1
PASS SELFTEST 6b comma SAME=2
PASS SELFTEST 6c file SAME=2
PASS SELFTEST 6d missing class exits non-zero
PASS SELFTEST 6e exact inner SAME=1 missing-left ONLY_RIGHT=1 extra-left Fixture reported
PASS SELFTEST 7 SAME=6 DIFF=0
PASS SELFTEST 8a method type-ann DIFF=1
PASS SELFTEST 8b insn type-ann DIFF=1
PASS SELFTEST 8c record type-ann DIFF=1
=== SELFTEST ALL PASS ===
PASS FALLBACK 1 Maven absent from isolated PATH
PASS FALLBACK 2 cold CLI downloads/checks ASM and compiles with javac; --help exits 0
PASS FALLBACK 3 actual comparisons SAME=1 / DIFF=1; checksum diagnostics only on stderr
PASS FALLBACK 4 build.sh also succeeds without Maven
=== FALLBACK ALL PASS ===
```

fallback 測試使用不含 mvn 的獨立 PATH、沒有 target 的工具複本及含空白路徑；實際下載／校驗 ASM、javac 編譯、CLI `--help`、相同 JAR 比較、改常數比較、build.sh 皆成功。不是使用已有 Maven JAR 冒充 fallback。

本機另以 Microsoft JDK `17.0.17` 編譯工具，核對兩個 ASM JAR 雜湊並執行 reference 自比對：`SAME=2554 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=2554`。本機實際建置 Foo 三方 fixture，驗證 only-C 指定方法 DIFF、同類別未指定方法 DIFF、移植後 SAME、拼錯清單非零退出。完整 Bash 路徑的驗證來自上列 CI，本機未安裝 Bash／Maven。

### 語意缺口計數

```text
SUMMARY SAME=2064 DIFF=485 ONLY_LEFT=1380 ONLY_RIGHT=5 BYTE_SAME=1008 ALLOW_METHODS=0
```

left 3929 類、right 2554 類、交集 2549。SAME 2064 與 BYTE_SAME 1008 來自同一對 JAR，bytes 不同但語意相同為 **1056**。DIFF 第一處位置：method 343、field 27、class 屬性 115。完整清單見 [語意缺口快照](../server-semantic-gap.md)。

## 與 #43 粗估的關係（已驗證／推測）

- **已驗證**：#43 §6-1 記錄 JDK 21／javap 粗估：共同類別 2538、593 筆不同，其中 162 筆解析失敗，約 430 只是待確認數。該流程未正規化 lambda／access 名稱。本輪使用 JDK 17＋ASM，交集為 2549；基準、解析方式與比較項目不同。
- **已驗證**：本工具另外比較欄位常數、註解、內部類／nest 等結構，且去除除錯資訊、常數池排序與合成名稱編號雜訊。BYTE_SAME=1008 是本輪重新量測，沒有挪用歷史值當證據。
- **推測**：編譯器版本、反編譯後的來源形狀、正規化深度及比較範圍共同造成數量不同。尚未在相同輸入與交集逐類對照舊 javap 結果，因此不能把 485−430 歸因為 55 個特定屬性差異。1056 也沒有逐類拆成某一種雜訊。

## 未驗證什麼／限制

- SAME 表示本工具正規化項目一致，不等於已做行為回歸；DIFF 也不直接等於行為缺陷。
- 未啟動伺服器、登入器或遊戲，未執行 `--check`／`--smoke`，未連正式 DB。
- 未對候選 `50635f7e` 執行完整比對／回歸；交後續批次。
- 匿名類 `$1`／`$2` 不重新命名，區域變數槽位不重編號；不證明所有 JVM 程式的行為等價。
- CLI 有差異時仍以 0 結束（報告生成成功）；驗收必須檢查 DIFF／ONLY_*，使用完整 entry 清單的批次還須檢查 missingLeft／extraLeft 均空。此約定已寫入工具 README。

## 產出路徑

- `lito伺服器/LiTo381-Server/tools/semantic-diff/`：Java、獨立 pom、CLI、建置共用程式、兩支自我測試、README。
- `.github/workflows/build.yml`：語意 job 與 artifact 證據。
- `docs/server-semantic-gap.md`：本輪 CI 快照。
- `docs/reports/issue-44.md`：本報告。

未修改伺服器 `src/main`、伺服器 `pom.xml`、`docs/REPORT.md`、機密設定、封存或 releases；未提交生成 class/JAR、未新增硬連結。

## 給下一棒的提示

- **Claude 審查**：核對 PR #48 的本輪程式提交、上述 run／job／artifact；四項退回問題及擁有者補充皆有測試證據。由使用者決定合併，這輪未合併 PR 或關閉 Issue。
- **T020e／A 階段**：以本輪 DIFF 485 為原始盤點，依 #43 `owned_by_B` 以 `--exclude` 排除 B 所屬外層及內部類後再安排 A 修補；485 不是全部都歸 A。
- **B01–B09**：`--classes` 傳完整 `verify_entries`；語意計數及 `classSelection` 兩份清單都要檢查。共同允許清單先裁切成此次選取範圍，避免未比較的規則被默認接受。
- **B09／T029**：依 #43 第 4 版，用 `--allow-methods docs/combat-merge/t029-methods.txt --allow-right reference/base-0.6.71.jar`；其餘方法對候選。無需「候選＋正式改動」人工合成 JAR，也不要把 T029 整類排除。
- **使用者**：本輪不需實機操作；後續行為驗收另依任務安排。
