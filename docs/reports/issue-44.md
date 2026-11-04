# Issue #44 / T020d — ASM 語意比對工具＋CI 語意差異報告

## 做了什麼

新增獨立工具 `lito伺服器/LiTo381-Server/tools/semantic-diff/`（Java 17＋ASM 9.7 `asm`／`asm-tree`，**未改**伺服器 `pom.xml` runtime 依賴、**未改** `src/main`、**未改** `docs/REPORT.md`）。

| 路徑 | 作用 |
|---|---|
| `tools/semantic-diff/pom.xml` | 工具自己的 Maven 專案；`mvn package` 產出 `semantic-diff.jar` |
| `tools/semantic-diff/semantic-diff` | CLI 進入點：`--left`／`--right`／`--classes`／`--json`／`--md` |
| `tools/semantic-diff/src/main/java/local/lito381/semanticdiff/*` | JAR 載入、`lambda$`／`access$` 依出現順序正規化、ASM tree 比對、JSON／MD 輸出 |
| `tools/semantic-diff/selftest.sh` | 驗收三項自我測試 |
| `tools/semantic-diff/build.sh` | Maven 或 javac＋已下載 ASM jar |
| `.github/workflows/build.yml` | 新增非阻斷 job `Semantic gap (non-blocking)`（`continue-on-error: true`） |
| `docs/server-semantic-gap.md` | 本輪對 Maven 產物 vs `reference/base-0.6.71.jar` 的快照 |
| `docs/reports/issue-44.md` | 本報告 |

正規化：`ClassReader.SKIP_DEBUG | SKIP_FRAMES` 去掉 LineNumberTable／LocalVariableTable／LocalVariableTypeTable／SourceFile／SourceDebugExtension；不比較 `max_stack`／`max_locals`／常數池順序；指令運算元已由 ASM tree 解析。合成名 `lambda$xxx$N`、`access$NNN` 在該類方法出現順序下改成 `#0`、`#1`…，同一 JAR 內跨類參照一併套用。

## 怎麼驗證

### 自我測試（已驗證，本機 OpenJDK 21 以 `--release 17` 編譯工具與 fixture）

```text
=== SELFTEST 1: same JAR vs itself ===
SUMMARY SAME=2554 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0
PASS SELFTEST 1 SAME=2554 DIFF=0

=== SELFTEST 1b: fixture JAR vs itself ===
SUMMARY SAME=2 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0
PASS SELFTEST 1b SAME=2 DIFF=0

=== SELFTEST 2: -g vs -g:none ===
SUMMARY SAME=2 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0
PASS SELFTEST 2 SAME=2 DIFF=0

=== SELFTEST 3: constant change must be detected ===
SUMMARY SAME=1 DIFF=1 ONLY_LEFT=0 ONLY_RIGHT=0
DIFF classes:
  semanticdiff/fixture/Fixture field ANSWER:I insn None left=Integer:42 right=Integer:43
PASS SELFTEST 3 DIFF=1
```

重跑：`bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh`（需 `java`／`javac`／`jar`；有 `mvn` 則用 Maven 建工具）。

### Maven 產物 vs 正式 JAR（已驗證，本輪快照）

```bash
mvn -B -DskipTests -Dmaven.test.skip=true package   # 在 LiTo381-Server；testCompile 仍屬 T005
bash lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left lito伺服器/LiTo381-Server/target/LiTo381-Server.jar \
  --right lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  --json /tmp/server-semantic-gap.json \
  --md docs/server-semantic-gap.md
```

輸出：`SUMMARY SAME=2042 DIFF=496 ONLY_LEFT=1369 ONLY_RIGHT=16`。  
交集 SAME+DIFF=2538，與 T007 `jar tf` 交集一致。ONLY_RIGHT=16、ONLY_LEFT=1369 與 `docs/server-source-gap.md` 一致。

`--classes` 抽樣：`SharedResourceMain,NativeDeVisibilitySwitch` → `SAME=1 ONLY_RIGHT=1`。

### CI

Job 名稱 `Semantic gap (non-blocking)`：先 `selftest.sh`，再 `mvn -B -DskipTests -Dmaven.test.skip=true package`，再跑上列比對，上傳 artifact `server-semantic-gap`（md＋json）。本 PR 的 GitHub Actions 跑完後才算 CI 已驗證；此處標待驗證。

## 與 #43 粗估（約 430）的關係

| 口徑 | 數量 |
|---|---:|
| 交集（T007 `jar tf`） | 2,538 |
| class bytes 全同（T020a） | 1,008 |
| javap 粗估仍不同 | ~430 |
| 本工具 SAME | 2,042 |
| 本工具 DIFF | 496 |

- SAME−1,008=**1,034**：位元組不同，但去掉 debug／合成名／常數池編號後語意相同。這就是「1,008 bytes 全同、javap 仍看到很多字面差」中的假差異。
- DIFF=496，比 430 **多 66**。javap `-c` 主要看反組譯文字；本工具還比 innerClasses／nestMembers／access／欄位常數。第一處差異：method 337、field 24、class 屬性 135（innerClasses 117、nestMembers 16、access 2）。
- 430 **不是**工作清單；T020e 應以本快照的 496 筆 DIFF 為準（其中頂層 272、inner／匿名 224）。

## 未驗證什麼

- 本 PR 的 GitHub Actions 尚未跑（提交後才有 artifact）。
- 語意 SAME ≠ 行為已回歸；未跑 `1.啟動伺服器.bat --check`／`--smoke`，未連正式 DB，未啟動遊戲。
- 匿名類 `$1`／`$2` **沒有**依出現順序改名（任務只要求 `lambda$`／`access$`）。這類差異會落在 ONLY_* 或 outer 的 innerClasses／nestMembers。
- 跨編譯器的區域變數槽位重編不正規化（任務未要求）；`-g` vs `-g:none` 不影響槽位，自我測試已過。
- 未對候選 `50635f7e` JAR 跑比對（JAR 不在 repo，屬 T020f）。

## 產出路徑

- `lito伺服器/LiTo381-Server/tools/semantic-diff/`
- `.github/workflows/build.yml`
- `docs/server-semantic-gap.md`
- `docs/reports/issue-44.md`

未改：`src/main`、伺服器 `pom.xml`、`docs/REPORT.md`、`config/server.properties`、封存、`releases/`、`LiTo.bin`。

## 給下一棒的提示

- **Claude（T020e，design）**：用 `docs/server-semantic-gap.md` 的 DIFF 表訂第一階段（src/main→正式 0.6.71）修補批次，不要再用 430。可先把「第一處差異＝innerClasses／nestMembers」的外層類（約 135）標成 inner 形狀差，與真指令差（method 337）分開排。每批用 `--classes` 驗收。
- **codex-local（T020f）**：候選 `50635f7e` JAR 入 `reference/` 後，同一支 CLI 對候選跑第二階段驗收。
- **Cursor 後續 B 批次**：`--classes file` 支援一行一個內部名或 `Foo.class`；JSON 的 `firstDiff.member`＋`insnIndex` 可當移植起點。
- **使用者**：不需要操作；真機驗收仍等合併批次之後。
