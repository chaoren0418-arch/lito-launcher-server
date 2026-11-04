# Issue #44 / T020d — ASM 語意比對工具＋CI 語意差異報告

## 做了什麼

獨立工具 `lito伺服器/LiTo381-Server/tools/semantic-diff/`（Java 17＋ASM 9.7，**未改**伺服器 `pom.xml` runtime 依賴、**未改** `src/main`、**未改** `docs/REPORT.md`）。第 2 次依審查補 `--exclude`／`--allow-methods`／`--allow-right`、`--classes` 副檔名與未命中報錯、註解結構化比較。

| 路徑 | 作用 |
|---|---|
| `tools/semantic-diff/pom.xml` | 工具 Maven 專案 |
| `tools/semantic-diff/semantic-diff` | CLI：`--left`／`--right`／`--classes`／`--exclude`／`--allow-methods`／`--allow-right`／`--json`／`--md` |
| `src/main/java/local/lito381/semanticdiff/*` | JAR 載入、合成名正規化、ASM tree 比對、註解／型別註解、方法 overlay |
| `tools/semantic-diff/selftest.sh` | 自我測試（原三項＋exclude／allow-methods／classes／註解） |
| `.github/workflows/build.yml` | 非阻斷 job `Semantic gap (non-blocking)` |
| `docs/server-semantic-gap.md` | Maven 產物 vs `reference/base-0.6.71.jar` 快照 |
| `docs/reports/issue-44.md` | 本報告 |

`--exclude`：清單中的外層類別名連同 `outer$…` 內部類別一併跳過。  
`--allow-methods`：每行 `類別#方法名稱描述子`；這些方法改跟 `--allow-right` JAR 比，其餘方法仍跟 `--right` 比。類別層欄位／存取旗標仍對 `--right`。  
`--classes`：先去掉 `.class` 後綴再把 `.` 換成 `/`；指定但兩邊都沒有的名字以 `missing --classes entries:` 非 0 結束，不再靜默空比較。

註解：`annotationDefault` 用遞迴結構化值（enum `String[]`、巢狀 `AnnotationNode`、List），不再 `Object.toString()`。指令 `visible/invisibleTypeAnnotations`、方法 local-variable type annotations、record component type annotations 納入比較。SUMMARY 另含本輪實測 `BYTE_SAME`（同一交集 raw bytes 全同的數量）。

## 怎麼驗證

重跑：`bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh`

### 自我測試（已驗證，本機 OpenJDK 21、`--release 17`；CI 須重跑本檔才算五項＋classes／註解都有 Actions 輸出）

```text
SELFTEST 1   正式 JAR 對自己     SAME=2554 DIFF=0 BYTE_SAME=2554
SELFTEST 1b  fixture 對自己      SAME=3 DIFF=0
SELFTEST 2   -g vs -g:none       SAME=3 DIFF=0 BYTE_SAME=0
SELFTEST 3   常數 42→43          DIFF=1（Fixture field ANSWER:I）
SELFTEST 4   --exclude Fixture   不含 Fixture／Fixture$Inner；剩下 Extra SAME
SELFTEST 5   --allow-methods Extra#extra()I --allow-right=left
             Extra SAME（overlay）；Fixture 仍 DIFF（未列入的常數差被抓出）
SELFTEST 6a  --classes Fixture.class     SAME=1
SELFTEST 6b  逗號清單 Fixture,Extra      SAME=2
SELFTEST 6c  檔案清單                    SAME=2
SELFTEST 6d  DoesNotExist                exit≠0，stderr 含 missing --classes entries
SELFTEST 7   enum／巢狀 annotationDefault self-vs-self  SAME=6 DIFF=0
SELFTEST 8   只改 method／insn／record 型別註解  各 DIFF=1
```

第 1 次 CI（run 41843825080）只證明當時的三項舊測試＋語意計數；**不能**代表本輪 exclude／allow-methods／classes／註解已在 Actions 跑過。那些要等本 commit 的 job。

### 語意缺口快照

上一輪 CI Temurin 17（run 41843825080）對 Maven 產物 vs `reference/base-0.6.71.jar`：

`SUMMARY SAME=2064 DIFF=485 ONLY_LEFT=1380 ONLY_RIGHT=5`（left 3929／right 2554／交集 2549）。

該輪工具**還沒**輸出 BYTE_SAME。本 commit 起 SUMMARY 含 `BYTE_SAME=`；快照裡 BYTE_SAME 列標待下一輪 CI 寫入，避免再用歷史 1,008 去減。

## 與 #43 粗估（約 430）的關係

| 口徑 | 來源 | 可否與本輪直接相減 |
|---|---|---|
| bytes 全同 1,008 | T020a，JDK 21 Maven 產物 vs 正式 JAR | 否（不同 javac、不同類別集合） |
| javap 約 430 | 同上，去行號／常數池編號／空白，未正規化 lambda$／access$ | 否 |
| SAME=2064 DIFF=485 | CI Temurin 17，本工具 | 本輪語意計數 |
| BYTE_SAME | 須同一對 JAR 本輪重測 | 待本 commit 的 CI |

已撤回「2064−1008=1056 全是消除雜訊」與「485−430=55 全是 innerClasses／欄位常數」的斷言。沒有在同一交集上逐類對照 javap 與本工具，不能做那種歸因。

本輪能確定的：這一對 Temurin 17 JAR 的 DIFF=485（第一處差異 method 343、field 27、class 屬性 115——那是本工具先看到哪裡不同）。T020e 工作清單用 **485**，不要用 430。bytes 不同但語意相同的數量等 BYTE_SAME 出來後用 `SAME − BYTE_SAME`（同一輪）。

## 未驗證什麼

- 本 commit 的 GitHub Actions 尚未跑（exclude／allow-methods／BYTE_SAME 的 CI 輸出待下一輪）。
- 語意 SAME ≠ 行為回歸；未跑 `--check`／`--smoke`，未連 DB，未啟動遊戲。
- 匿名類 `$1`／`$2` 不依出現順序改名（任務只要求 `lambda$`／`access$`）。
- 區域變數槽位不重編號。
- 未對候選 `50635f7e` JAR 比對（T020f）。
- 後續批次驗收須 JDK 17，不可混用 JDK 21 編出的類別集合。

## 產出路徑

- `lito伺服器/LiTo381-Server/tools/semantic-diff/`
- `.github/workflows/build.yml`
- `docs/server-semantic-gap.md`
- `docs/reports/issue-44.md`

未改：`src/main`、伺服器 `pom.xml`、`docs/REPORT.md`、`config/server.properties`、封存、`releases/`、`LiTo.bin`。

## 給下一棒的提示

- **Claude（T020e）**：工作清單用 CI DIFF **485**。B3-core 用 `--allow-methods`＋`--allow-right` 指向「候選＋正式改動」合成 JAR；T029 整類排除用 `--exclude`。`--classes Foo.class` 可用。指定不存在的類會失敗，不要依賴靜默 0。
- **codex-local（T020f）**：候選 JAR 入庫後同一 CLI 對候選跑第二階段。
- **Cursor 後續批次**：allow-methods 格式 `local/lito381/server/ClientSession#sendRoomParticipant(...)V`（`#` 後是方法名＋描述子）。
- **使用者**：無需操作。
