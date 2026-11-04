# Issue #63 — T020-B02 戰鬥併入暫存（new-java，51 類）

## 做了什麼

依 `docs/combat-merge/batches.json` 的 `B02`（51 個外層類別、108 個 `verify_entries`），從 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/sources/`（`manifest.json` 的 `stored_source`）複製到 `merge-inputs/50635f7e/staged/local/lito381/server/`。

複製後的 Java 是各類「最後一份完整原始碼」。候選 JAR 上還有後續 ASM splice，已把**能用 Java 表達**的部分寫進 staged（wrapper 改名、`allowFrame` 的 IFNE 形、`playerLife`／`petLife`、`replayWorld`／`replayBattle`、`actorBinding`、`RemainingAreaLists`、`ElementAudience.copy`／`recipients`、`PoisonNpc.managed`／`status`、`FreezeTarget.applies` 等）。Java 無法表達的殘餘（record 額外欄位、未指派的 final 欄位 5-arg 建構子、方法參照 Handle 改寫、高 slot 插入、splice 留下的死 lambda）在編譯後對 tmp class 做後處理，來源為候選 class bytes。

未改 `src/main`、`pom.xml`、`docs/REPORT.md`、設定檔或封存。

## 怎麼驗證

### 編譯（已驗證）

環境：OpenJDK 21.0.12.1，`javac --release 17 -encoding UTF-8`。沒有 JDK 17 本體；`lib/` 不在 git，classpath 裡的 `lib/*` 當字面路徑即可。

**逐檔編譯**（本批採用；同批 sibling 原始碼會蓋住候選 JAR 上尚未移植的 ASM 成員，不能 51 檔一次編）：

```text
PWD=lito伺服器/LiTo381-Server
javac=javac 21.0.12.1
file_count=51
mode=per-file against candidate+base
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d <tmp> merge-inputs/50635f7e/staged/local/lito381/server/<each>.java
```

結果：51 檔全部 exit 0，產出 **108** 個 `.class`，與 `B02.verify_entries` 集合相等。完整逐檔日誌在 `scratch/issue-63/javac-each.log`（scratch 不入庫）。

**一次編 51 檔**（規格字面指令）失敗，2 errors：`NativeSourceControlledAudience` 讀 `Output.nativeSourceControlledAudience`，該欄位是後層 ASM 加在 record 上，Java record 不能宣告額外實例欄位，同批原始碼因此看不到它。這不是漏複製。

### 語意（已驗證）

後處理（scratch，不入庫）後打包 tmp JAR，執行：

```bash
bash lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left scratch/issue-63/left.jar \
  --right lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
  --classes scratch/issue-63/verify_entries.txt \
  --json scratch/issue-63/semantic.json \
  --md scratch/issue-63/semantic.md
```

輸出：

```text
SUMMARY SAME=108 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=9 ALLOW_METHODS=0
```

`classSelection.missingLeft`／`extraLeft` 皆空。候選 JAR SHA256 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；正式基準 `base-0.6.71.jar` SHA256 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

BYTE_SAME=9 即下列後處理直接覆上的候選 class（其餘 99 個是 javac 產出、正規化後 SAME）：

- `NativeAcceptedAreaFreezeMutation.class`（`g.c::player` 的 indy Handle 改 `NativeSourceIndirectPlayerProof.player`）
- `NativeBattleControlledCancellation.class`（`ObjectTargetRequest.request` 插入、method-ref 殘留 lambda）
- `NativeSkillTargetMetadata.class`（ASM 高 slot 插入 HealingFormula／StaticSpawn／DollSkillData／HitReaction）
- `NativeSourceArenaPoison.class`（`PoisonNpc.status` 前綴與 stamp splice）
- `NativeSourceArenaPoison$Viewer.class`（record 額外 `nativeSourcePoisonAudience`）
- `NativeSourceAreaClassSelection$Cursor.class`（5-arg 建構子不指派 final 欄位，bytecode 合法、Java 不合法）
- `NativeSourceControlledDisplay.class`（splice 留下的 `lambda$enter$0`）
- `NativeSourceControlledDisplay$Output.class`（record 額外 `nativeSourceControlledAudience`）
- `NativeSourceCubeNpc.class`（splice 留下的 `lambda$deliver$0`）

Shock 同伴動作的 lambda 名稱由 `lambda$nativeSourceControlledOriginalShock$` 改回 `lambda$shock$`（wrapper 改名後 javac 會帶原方法名）。

## 未驗證什麼／待驗證

- 未跑 `1.啟動伺服器.bat --check`／`--smoke`，未連正式 DB，未啟動遊戲。
- 未跑單元測試／回歸（未動 `src/main`；§6-5 規定在 B-land 統一處理）。
- 未在 Windows／JDK 17 重跑同一組 javac。
- 一次編 51 檔的規格指令在本環境失敗，見上；B-land 若改一次編，必須先讓 record 額外欄位在原始碼裡可被同批看見，或維持對候選 JAR 逐檔編。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/*.java`（B02 51 檔）
- `docs/reports/issue-63.md`（本檔）
- 驗證暫存（不入庫）：`scratch/issue-63/`（編譯日誌、PatchB02、semantic JSON／MD、left.jar）

## 已驗證／推測／待驗證

- **已驗證**：B02 51 檔已入 staged；逐檔 javac --release 17 成功且 class 集合＝108 筆 verify_entries；後處理後 semantic-diff SAME=108、DIFF=0、missingLeft／extraLeft 空。
- **推測**：BYTE_SAME 的 9 個 class 若只靠 javac、不做後處理，會再出現 DIFF（record 欄位、死 lambda、Handle 改寫、高 slot 插入）。
- **待驗證**：B-land 把 staged 搬進 `src/main` 後，Maven 產物對候選的 1,384 entry 仍須全部 SAME；本批未做。

## 給下一棒的提示

- B03 起請維持「對候選 JAR **逐檔** javac」，不要 一批一次編，直到 wrapper／額外欄位都在同批原始碼裡。
- 後處理清單（9 個 class）在 B-land 仍要對應處理，或改成可編譯的 Java 等價（record 額外欄位做不到，只能 ASM 或改非 record）。
- `NativeSourceControlledAudience` 依賴 `Output.nativeSourceControlledAudience`；該欄位不在 Java record 宣告裡，一次編會失敗。
- 下一棒是 B03（或調度器按 `docs/plans/issue-43.json` 派的下一批）；落地與回歸等 B01–B09 齊後由 B-land 做。
- 本任務不是 plan／design，沒有寫 `docs/plans/issue-63.json`。
