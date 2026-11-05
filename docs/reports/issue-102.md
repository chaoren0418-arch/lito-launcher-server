# Issue #102：T020g @AsRecord 轉換器 CLI

## 做了什麼

在既有 `task/102` worktree 完成 Java＋ASM 原地轉換 CLI。唯一加入伺服器 `src/main` 的檔案為 `local.lito381.server.AsRecord`：CLASS retention、TYPE target，包含 `components()` 與預設空陣列的 `finalFields()`。未修改 `pom.xml`、`docs/REPORT.md` 或正式部署資源。

轉換器還原 `java/lang/Record` 父類別、建構子的 super 呼叫、元件順序／描述子／generic signature、class final、指定欄位 final，以及 javac 形式的三個 `public final` ObjectMethods invokedynamic 方法；移除標註。補上 javac 的 `MethodHandles.Lookup` metadata，保留其他方法、欄位與註解，未標註檔案不寫回。所有標註驗證成功後才開始寫檔，驗證失敗不會留下部分轉換結果。

## 已驗證

環境：Windows、Microsoft OpenJDK 17.0.17、Maven 3.9.16、ASM 9.7、CFR 0.152。工作基準 `7c6bee7`，分支 `task/102`。

| 驗收項目 | 結果 |
| --- | --- |
| javac record 對照：generic、空元件、指定元件順序、static nested | `SAME=4 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0` |
| `finalFields` 對照：獨立 ASM 加入 final 欄位，另有未賦值建構子 | `SAME=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0` |
| 額外欄位／跨類別 getfield、putfield | `Class.isRecord()` true；反射只列元件；equals/hashCode/toString 不含額外欄位 |
| 建構鏈、建構子中的 `new Object()`、其他方法／註解、generic reflection | `-Xverify:all` 行為測試通過 |
| 未標註類別及第二次轉換 | bytes 完全不變 |
| 8 組無效輸入，包含不存在的 finalFields | 退出非零、指出原因、整批 bytes 不變 |
| Windows `.cmd` CLI，含中文與空白的 classes 路徑 | `as-record: scanned=0 converted=0` |
| 無 Maven 快取的依賴下載 | ASM／asm-tree 9.7 下載至工具 target，兩個 SHA-256 校驗通過，CLI 成功 |
| 候選 `NativeReturnNatureMaterialAction$Target` | `SAME=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0`，且選取 entry 無缺少／多餘 |
| 伺服器完整 `mvn -B test` | 6,033 run／0 failures／0 errors／110 skipped，`BUILD SUCCESS` |

自測終端摘要：

```text
runtime: isRecord/components/object methods/extra getfield-putfield/finalFields/generic/nested PASS
independent ASM finalFields reference generated
SUMMARY SAME=4 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
SUMMARY SAME=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
as-record: scanned=9 converted=6
as-record: scanned=9 converted=0
unannotated bytes unchanged; second pass bytes unchanged PASS
javac-record: semantic-diff SAME
final-fields-asm: semantic-diff SAME
SELFTEST PASS
```

候選輸入 SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。CFR JAR SHA-256：`f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`。

`candidate-selftest.py` 從候選只抽出指定 entry，用 CFR 反編譯；保留 CFR 的欄位、建構子內容、`owned/valid/canPublish` 與 accessors，改成 `@AsRecord` private static final class，移除三個 Object 方法。外層類別僅供維持 NestHost／InnerClasses 編譯上下文，不冒充已移植外層實作；比較 JAR 僅包含真實待驗證的 Target entry，依賴直接使用候選 JAR，沒有替代依賴 stub。

```text
as-record: scanned=3 converted=1
SUMMARY SAME=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
CANDIDATE PASS: NativeReturnNatureMaterialAction$Target semantic-diff SAME
```

Maven 全套於 2026-11-05 05:56:23（UTC+08:00）完成，退出碼 0；1,266 份 Surefire XML 彙總與終端一致：

```text
Tests run: 6033, Failures: 0, Errors: 0, Skipped: 110
BUILD SUCCESS
Total time: 09:53 min
```

## 怎麼驗證（可重現指令）

以下從 worktree 根目錄執行；`$py` 是本機 Python，其他機器可改成 `python`。ASM 共用快取僅讀取；CLI 依賴與編譯產物均寫入工具 `target/`。

```powershell
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
$env:AS_RECORD_MAVEN_REPO = 'D:/dev-cache/maven/repository'
$py = 'D:/dev-orchestrator/tools/python/python.exe'
$tool = 'lito伺服器/LiTo381-Server/tools/as-record'
& $py "$tool/selftest.py"
# CFR 不列入 git；本輪從 Maven Central 的 org/benf/cfr/0.152 下載至 target/dependency。
& $py "$tool/candidate-selftest.py" --cfr "$tool/target/dependency/cfr-0.152.jar"
```

Maven 指令：先用 `robocopy D:/dev-cache/maven/repository scratch/as-record/maven-repository /E` 建立 worktree 內快取副本（本輪複製成功），再執行：

```powershell
$taskRoot = (Get-Location).Path
$taskTmp = Join-Path $taskRoot 'lito伺服器/LiTo381-Server/target/issue-102-tmp'
$taskRepo = Join-Path $taskRoot 'scratch/as-record/maven-repository'
New-Item -ItemType Directory -Force $taskTmp | Out-Null
$env:TEMP = $taskTmp
$env:TMP = $taskTmp
$env:JAVA_TOOL_OPTIONS = $null
$env:MAVEN_OPTS = '-Djava.io.tmpdir=' + $taskTmp
Set-Location 'lito伺服器/LiTo381-Server'
& 'D:/lineage開發/tools/apache-maven-3.9.16/bin/mvn.cmd' -B test "-Dmaven.repo.local=$taskRepo" "-DargLine=-Djava.io.tmpdir=$taskTmp"
```

沒有啟用 `lito.legacy.integration` 或正式 DB 設定。`java.io.tmpdir`／TEMP／TMP 均指向 worktree；未啟動遊戲、登入器或正式伺服器／DB。

初次執行使用 `JAVA_TOOL_OPTIONS` 設定暫存路徑，JVM 的 `Picked up JAVA_TOOL_OPTIONS` 訊息污染子行程 stdout，造成 `ModernEquipmentOverrideStoreTest`／`ModernMatchOutboxTest`／`RentalBattleHandoffStoreTest` 的協定比對失敗（6,033 run／2 failures／5 errors／110 skipped）。修正為上述不輸出診斷訊息的 `MAVEN_OPTS`、Surefire `argLine`、TEMP/TMP 組合；沒有修改任何既有測試或業務程式。

修正環境後，先以同一 Maven 指令加 `-Dtest=ModernEquipmentOverrideStoreTest,ModernMatchOutboxTest` 定向重跑：`Tests run: 44, Failures: 0, Errors: 0, Skipped: 0`、`BUILD SUCCESS`（1 分 40 秒）。接續重跑整套測試；定向證據保留在 `scratch/as-record/maven-targeted.log`，初次統計在 `maven-initial-summary.txt`。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/AsRecord.java`
- `lito伺服器/LiTo381-Server/tools/as-record/`：Java 核心、跨平台 CLI、自測與使用說明。
- `docs/reports/issue-102.md`：本報告。
- 未入 git 的證據：`scratch/as-record/selftest.log`、`selftest/*.json`、`candidate.log`、`candidate/semantic-diff.json`、`candidate/NativeReturnNatureMaterialAction.java`、`candidate/cfr/`、`maven-test.log`。
- class/JAR：工具 `target/selftest/`、`target/candidate/`；Maven 報告：伺服器 `target/surefire-reports/`。

## 推測

相同來源契約應可用於其他有額外實例欄位的 record；目前沒有將單一候選 entry 的 SAME 推廣成全部 39 個 entry 都已驗證。

## 待驗證／未驗證什麼

- Maven 接線、JSON 掃描與批次自動挑選交給 #134，本次範圍不包含。
- 其他候選／正式 record 尚未逐類改寫比對，應由各批次驗收。
- 未進行正式部署、遊戲／登入真機驗收。
- 本輪在 Windows 執行，未實跑 Linux／Bash wrapper；此機提供的 Git 工具沒有可用的 Bash 入口。
- CLI 不提供寫回階段 I/O 失敗的整批交易回復；請只作用於可重建 classes。欄位註解不會推導成 record component 註解。

## 給下一棒的提示

請 Claude 審查本分支；合併由使用者決定。#134 可在 javac 後、打包／語意比對前呼叫本 CLI，或直接使用 Java 主類別 `local.lito381.asrecord.AsRecord`；ASM 只需是建置工具依賴，不需放入伺服器 runtime。後續批次保留 private final 元件與 accessors、額外欄位的原旗標，不手寫 Object 方法；若需 `finalFields`，務必確認與候選旗標一致。一般自測與候選自測可獨立重跑，無須原始專案的未追蹤資源。
