# Issue #134：T020g-2 Maven 接線與 record 額外欄位掃描

## 做了什麼

沿用既有 `task/134` worktree，基準 `33f3f5c`。`pom.xml` 加入 `maven-antrun-plugin`，在 `process-classes` 編譯並執行 #102 Java 轉換器，在 `process-test-classes` 轉換測試 fixture。ASM／asm-tree 9.7 僅為外掛依賴，工具 classes 獨立放 `target/as-record-tool`；一般 Maven 建置不需要 Python。

以 `build-helper-maven-plugin` 加入 #108 的 `compile-stubs` 來源，並在共用 `maven-jar-plugin` 設定排除 `com/lineage/**`、ASM 與轉換器。因此主 JAR、modern JAR、測試 JAR 均不含這些類別，`copy-runtime` 也不會帶入 ASM。這是本任務允許的「額外 source root＋打包排除」方案：`target/classes` 與單元測試 classpath **仍含 stub**，部署必須使用成品 JAR。stub 不代表舊伺服器 API 已實作。

新增測試用 `AsRecordBuildFixture`、2 個 JUnit 測試，以及只包含 fixture 的 `LiTo381-Server-as-record-tests.jar`。另有可重跑隔離模組自測，使用同一份 POM 驗證主類別在 `process-classes` 轉換後確實封裝為 record；不在伺服器 `src/main` 加入測試類別。

## 已驗證

環境：Windows、Microsoft OpenJDK 17.0.17、Maven 3.9.16、Python 3.12.10。命令沙箱初始化失敗，已改用工具核准的提升權限模式；所有建置、暫存與依賴下載仍在此 worktree。

### 掃描筆數與來源

| 輸入 | SHA-256 |
|---|---|
| `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |

原始掃描符合「父類別為 Record 且實例欄位多於 component」的 entry 為 **44 筆：候選 39、正式 5**。這不是 44 個不同類別，而是 **39 個類別、5 個在兩份 JAR 都符合條件的版本**。依批次歸屬選 B 的候選版本、A 的正式版本，主清單正好為 **39 筆：候選 38＋正式 1**。

| 兩份 JAR 都有的類別 | 批次 | 主清單版本 | 另一版本證據 |
|---|---|---|---|
| `ClassicWorldMonsterPopulation$Snapshot` | B08 | 候選 | 正式 |
| `ClientSession$PendingPetRename` | B09 | 候選 | 正式 |
| `ClientSession$PendingSlaveTarget` | B09 | 候選 | 正式 |
| `GameHub$MonsterAiTick` | B09 | 候選 | 正式 |
| `CombatCurseLifecycle$Target` | A04 | 正式 | 候選 |

JSON 的 `records` 為 39 筆批次目標，`other_jar_variants` 保留另外 5 份完整 component／欄位／存取證據；`raw_total`／`raw_counts` 明確揭露原始 44 筆。全部目標均能對應批次。每個額外欄位附名稱、描述子、numeric access、旗標及同 JAR 實際 GETFIELD／PUTFIELD 的類別、方法、描述子、opcode；未將未使用常數池引用、反射或 method handle 當成直接指令存取。

```text
GENERATED: total=39 counts={'base-0.6.71.jar': 1, 'candidate-50635f7e.jar': 38}
RAW: total=44 counts={'base-0.6.71.jar': 5, 'candidate-50635f7e.jar': 39}
CHECK: total=39 counts={'base-0.6.71.jar': 1, 'candidate-50635f7e.jar': 38}
```

`--check` 重新編譯掃描器並掃描兩個 JAR，完整 JSON bytes 相同。JSON 無時間戳，記錄 JAR 與兩份批次表 SHA-256。

### 隔離 Maven 自測

自測模組複製實際 POM、轉換器及 stub，僅使用測試來源，執行 `clean package -Dmaven.test.skip=false` 與 `clean package -Dmaven.test.skip=true`，兩者成功。主類別與測試類別各轉換 1 個 record，2 個 JUnit 測試通過。

獨立 JVM 使用 platform classloader 作為 parent，只從指定 JAR 載入 fixture，確認 `Class.isRecord()`、父類別、component、額外欄位讀寫、equals/hashCode/toString；ASM、stub、轉換器類別均載入失敗。掃描 fixture 也確認 static 欄位被排除，額外欄位 access 正確，且找到巢狀 Access 類別真正的 GETFIELD／PUTFIELD。

```text
as-record: scanned=19 converted=1
as-record: scanned=3 converted=1
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
JAR RECORD PASS: local.lito381.server.AsRecordBuildFixture
PACKAGE PASS: main/modern/test JAR and runtime lib contain no ASM/stubs/converter
JAR RECORD PASS: local.lito381.server.AsRecordMainFixture
SCAN FIXTURE PASS: components/extra flags/static exclusion/direct field instructions
MAVEN SELFTEST PASS
```

### 全套 Maven 驗證

第一輪 `mvn -B package` 已產生三個成品 JAR；1,267 份 Surefire XML 彙總：**6,035 tests／0 failures／0 errors／110 skipped**。110 個 skip 為既有環境守門，未變更測試跳過條件。實際三個成品與 `target/lib` 的檢查結果：

```text
JAR RECORD PASS: local.lito381.server.AsRecordBuildFixture
PACKAGE PASS: main/modern/test JAR and runtime lib contain no ASM/stubs/converter
LiTo381-Server-as-record-tests.jar com/lineage=0 ASM=0
LiTo381-Server-modern.jar com/lineage=0 ASM=0
LiTo381-Server.jar com/lineage=0 ASM=0
```

第一輪 PowerShell 暫存 wrapper 使用 `ErrorActionPreference=Stop`，將既有 SLF4J 缺少 logger binding 的 stderr 提示誤判為 NativeCommandError，造成 wrapper 退出 1 且後半段日誌未保存；未將這個 wrapper 結果宣稱為 Maven 退出 0。已修正暫存執行方式，後續測試另外保留真正 Maven JVM 的退出碼，最終打包以 Python subprocess 合併 stdout／stderr 完整記錄。此修正不涉及任何專案業務程式或既有測試。

最終獨立 `mvn -B test` 的 Maven JVM **退出碼 0**（`scratch/issue-134/maven-test-exit.txt`）。1,267 份 XML 全部於本輪 07:56:43–08:04:34 更新，彙總仍為 **6,035 tests／0 failures／0 errors／110 skipped**；新增的 2 案均通過。該輪 wrapper 在啟動時已載入舊的 Stop 設定，故仍退出 1；上述退出 0 是監看真正 Maven JVM process handle 取得，未以 wrapper 退出碼混充。

完整測試通過後，最終以 `mvn -B package -DskipTests` 重新編譯、轉換並封裝最新版測試 fixture，避免再重跑同一套長測試。2026-11-05 08:06:38（UTC+08:00）完成，**BUILD SUCCESS／退出碼 0**，耗時 1 分 6 秒。主目錄 `scanned=3946 converted=0`，測試目錄 `scanned=2031 converted=1`；這次只略過 Surefire 執行，沒有略過 testCompile／轉換／test-jar。封裝後再執行 JAR 載入及所有成品／runtime lib 排除檢查，仍全部 PASS。

## 怎麼驗證

從 worktree 根目錄執行；Maven 共用快取只讀，先複製到 worktree（`robocopy` 回傳碼 0–7 表示正常）。

```powershell
$taskRoot = (Get-Location).Path
$taskRepo = Join-Path $taskRoot 'scratch/issue-134/maven-repository'
$taskTmp = Join-Path $taskRoot 'scratch/issue-134/tmp'
New-Item -ItemType Directory -Force $taskTmp | Out-Null
robocopy D:/dev-cache/maven/repository $taskRepo /E /NFL /NDL /NJH /NJS /NP
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
$env:TEMP = $taskTmp
$env:TMP = $taskTmp
$env:JAVA_TOOL_OPTIONS = $null
$env:MAVEN_OPTS = '-Djava.io.tmpdir=' + $taskTmp
$mvn = 'D:/lineage開發/tools/apache-maven-3.9.16/bin/mvn.cmd'
Push-Location lito伺服器/LiTo381-Server
& $mvn -B package "-Dmaven.repo.local=$taskRepo" "-DargLine=-Djava.io.tmpdir=$taskTmp"
& $mvn -B test "-Dmaven.repo.local=$taskRepo" "-DargLine=-Djava.io.tmpdir=$taskTmp"
# 全套測試後若需重建封裝，可略過重複執行測試；仍會編譯及轉換測試 fixture。
& $mvn -B package -DskipTests "-Dmaven.repo.local=$taskRepo" "-DargLine=-Djava.io.tmpdir=$taskTmp"
$env:AS_RECORD_MAVEN_REPO = $taskRepo
py -3 -B -X utf8 tools/as-record/scan-record-fields.py --check
py -3 -B -X utf8 tools/as-record/maven-selftest.py --maven $mvn --maven-repo $taskRepo
Pop-Location
```

## 推測

後續 A-land／B-land 使用相同 `@AsRecord` 契約時可由這條 Maven 接線轉換；尚未逐一將 39 個類別移植到 `src/main`，不能把 fixture 成功視為全部戰鬥類別已等價。

## 待驗證／未驗證什麼

- Linux／Bash 入口尚未在本機執行，保留給後續 Linux CI；本任務不修改範圍外的工作流檔案。
- 未進行遊戲、登入器、正式伺服器或正式 DB 操作；未讀機密設定、封存或客戶端。
- 未修改 `docs/REPORT.md`、其他 `src/main`、正式部署檔或原始專案工作目錄內容。

## 產出路徑

- `lito伺服器/LiTo381-Server/pom.xml`：Maven 接線與成品排除。
- `lito伺服器/LiTo381-Server/tools/as-record/`：掃描 CLI／ASM 掃描器、Maven 自測、JAR 載入檢查與說明。
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/AsRecordBuildFixture.java`、`AsRecordBuildTest.java`。
- `docs/combat-merge/record-asm-fields.json`、`docs/reports/issue-134.md`。
- 不提交：`scratch/issue-134/` 日誌、隔離 Maven 快取與自測模組；伺服器及工具 `target/` 建置產物與 Surefire XML。

## 給下一棒的提示

由 Claude／主控審查 `task/134`，合併交使用者。A／B 批次執行器以 `records` 的批次目標版本移植，`other_jar_variants` 只作另一版本的來源證據；不要混用正式與候選欄位旗標。A-land／B-land 仍須逐批 semantic-diff 與完整回歸。Linux 執行器可依上述 CLI 重跑 Maven 自測；#108 的 Bash `build.sh` 驗證仍需 Linux CI 承接。
