# Issue #284：最新 main JAR 建置與實驗啟動阻塞（2026-11-13）

## 結論

**阻塞，未完成 T014；不得放行 #285。** 步驟 1 已完成：最新取得的 main 建置成功，JAR 與四個 runtime lib 已複製並驗證 SHA-256。步驟 2 停止：派工要求 port 42083 與實驗 schema，但本次使用者明定的隔離授權僅涵蓋 `D:\P24Lab-ClientEngine`、DB `127.0.0.1:43324`、server `42061`，不可自行擴張、換埠或依第三方聲稱擴權。另查得的 #284 lease 在建置完成前已到期。本次未建立實驗設定、未連 DB、未啟動 server／MySQL／登入器／遊戲，沒有大廳截圖。

## 已驗證：派工、查重與安全邊界

- 任務來源：[11/13 專項](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6850874413)、[本次派工](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6851602566) 及當時最新 Issue 本文。只處理「做」1–5，未跑 #285 清單、未重做 #341。
- 輕量對話查重：本次 `執行 LiTo #284 實驗啟動` 為 active；#341 為 idle，列表無另一執行中的 #284。讀取時 #284 最新留言仍是本次派工，未見其後 RESULT。
- 工作目錄：`D:\lineage開發\3-lito開發\scratch\wt\task-284`，分支 `task/284`，開始時工作樹乾淨，HEAD／origin/task/284 均為 `4f8612fd740f20a48a750c4d85e7f1df76504d3a`。
- 已讀適用 AGENTS／CLAUDE、伺服器 README 與維護規範。依本次使用者限制，不讀寫 `docs/REPORT.md`、正式 `config/server.properties`、封存、正式 BIN；不改 src、tools、lock 或調度器。
- 為避免同步舊分支時改動上述排除路徑，以 `git archive` 從剛 fetch 的 `origin/main` 匯出必要建置輸入至本任務 scratch，再直接用原始 pom 建置；未修改匯出的原始碼或建置工具。報告分支保留原歷史，不以舊分支的來源建 JAR。
- 本機自動核准／完全存取沒有可用工具可設定或驗證；不能宣稱全系統已核准。首次 Maven 沙箱網路拒絕後，依工具要求提出本次建置核准，獲准執行沙箱外建置一次。此核准不延伸至 Lab、DB 或 GUI。

## 已驗證：步驟 1 建置與副本

- main SHA：`42808209aef907f5590b7a9800a96bf6aef319a7`（`report: index #384 [skip ci]`）。
- 匯出內容：`lito伺服器/LiTo381-Server/{pom.xml,src/,compile-stubs/,tools/as-record/}`。未匯出正式機密設定或封存成品。
- 建置根：`scratch/t014/build-main-20261113T121551Z/`。
- 本輪成品與證據：`scratch/t014/run-20261113T121551Z/`（下文 R 均指此目錄，基準為上述 worktree）。
- 建置命令：`mvn -f <建置根>/lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package`；JVM 暫存指向 R/tmp。
- 結果：`BUILD SUCCESS`，exit 0，耗時 01:16，完成於 **2026-11-13 20:17:56 +08:00**。主來源編譯 1,179 檔、測試來源編譯 1,459 檔；`Tests are skipped.`。僅執行 pom 既定建置轉換器，未執行產品主程式或回歸測試。
- JAR Manifest：`Main-Class: local.lito381.server.SharedResourceMain`、`Build-Jdk-Spec: 17`、`Implementation-Version: 0.6.71`；Class-Path 的四個 lib 均已複製。來源 target 與 R 副本逐檔 SHA-256 相同。

| 成品 | bytes | SHA-256 |
| --- | ---: | --- |
| LiTo381-Server.jar | 15054908 | `64F4E3EBC1776ADE9523FBC92EF5EA5A8B47AEB68D2905015DAC8661E469204E` |
| lib/HikariCP-5.1.0.jar | 161840 | `A47A6EE62379694EE52C30036F0931B72F9AEE2A801D590341ED82BD839E2134` |
| lib/mysql-connector-j-8.0.33.jar | 2481560 | `E2A3B2FC726A1AC64E998585DB86B30FA8BF3F706195B78BB77C5F99BF877BD9` |
| lib/protobuf-java-3.21.9.jar | 1671409 | `1B78B4A76A71512DEBFDFF8F8FC5AEF6BFD459F65758FECF7AFF245E6E6301E4` |
| lib/slf4j-api-1.7.36.jar | 41125 | `D3EF575E3E4979678DC01BF1DCCE51021493B4D11FB7F1BE8AD982877C16A1C0` |

首次建置 exit 1 的完整錯誤段如下；完整日誌為 R/maven-package.log。沒有修改 pom／工具或換建置方法，核准後僅重跑同一 Maven 指令一次：

```text
[INFO] BUILD FAILURE
[INFO] Total time:  8.467 s
[INFO] Finished at: 2026-11-13T20:16:16+08:00
[ERROR] Plugin org.codehaus.mojo:build-helper-maven-plugin:3.5.0 or one of its dependencies could not be resolved:
[ERROR] The following artifacts could not be resolved: org.codehaus.mojo:build-helper-maven-plugin:pom:3.5.0 (absent): Could not transfer artifact org.codehaus.mojo:build-helper-maven-plugin:pom:3.5.0 from/to central (https://repo.maven.apache.org/maven2): Permission denied: getsockopt
[ERROR] -> [Help 1]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/PluginResolutionException
```

成功日誌為 R/maven-package-approved.log；原始輸出中部分中文路徑有編碼顯示失真，未重寫原始日誌。精確 Unicode 路徑及 SHA 以本報告和 R/artifact-sha256.json 為準。其他本機證據：R/main-sha.txt、build-path.txt、build-exit.txt、build-approved-exit.txt、jar-manifest.txt、main-build-inputs.zip。

### Maven 本機快取唯讀補查

依使用者補充，建置成功後僅讀取設定中的 localRepository 與外掛檔案 metadata，**未再執行 Maven**。檢查時間為 2026-11-13T12:19:43Z，證據為 R/maven-cache-inspection.json：

- Maven 入口為 `C:\Users\CHAOREN\tools\apache-maven-3.9.16\bin\mvn.cmd`；使用者 `C:\Users\CHAOREN\.m2\settings.xml` 明確設定 `D:\dev-cache\maven\repository`，全域 settings 未設定 localRepository。本次命令沒有 `-s` 或 `-Dmaven.repo.local`；唯讀檢查時 MAVEN_OPTS／MAVEN_ARGS 沒有 repo override。
- `D:\dev-cache\maven\repository\org\codehaus\mojo\build-helper-maven-plugin\3.5.0` 已有 JAR（71,441 bytes）、POM（7,397 bytes）、兩個 SHA1 與 `_remote.repositories`。JAR／POM 建立與修改時間均為 **2026-11-10T17:43:32Z**，早於本次建置；不是本次下載才存在。預設 `C:\Users\CHAOREN\.m2\repository` 下的同一外掛目錄不存在。
- 成功日誌沒有 `Downloading`／`Downloaded` 行，支持使用既有本機相依；設定指向 D 槽快取。但未保存兩次 Maven 的 effective-settings／debug local-repository 輸出，不能嚴格證明兩個執行安全上下文實際解析到的快取位置相同。
- 首次失敗可證實的是「Maven 將 POM 判為 absent，嘗試 central，然後網路 getsockopt 被拒」；**不能據此宣稱本機沒有 Maven 套件**。是否因沙箱設定讀取、快取可見性或其他解析差異而未採用既有檔案，現有證據不足，根因待釐清，未再測試或繞過核准。

## 已驗證：lease 與步驟 2 停止原因

僅查一次 `http://localhost:8787/api/state`，保存所需 leases 與查詢時間至 R/lease-check.json，未調查調度器實作、修改 lock 或續租：

- 查詢時間：`2026-11-13T12:16:12.5636722Z`（台北 20:16:12）。
- name：`p24lab`；holder：`chaoren0418-arch/lito-launcher-server#284`。
- since：`1794569554.34293`；exp：`1794572254.34293`，即 `2026-11-13T12:17:34.343Z`（台北 20:17:34）。
- 查詢當時有效且與派工一致；建置完成 12:17:56Z 已超過該次查得的到期時間。未再次查詢是否出現新 lease，未使用 Lab，不能把先前有效當成之後可用。
- R/lease-expiry-evaluation.json 是 **12:17:33Z、到期前** 對原回應做的本機換算，當時 expired_now=false；不是建置完成後的有效 lease 證明。

具體差異：Issue 步驟 2 要求 `port=42083`、`game.bind=127.0.0.2`、`p24_world_lito_exp`／`p24_lito_lito_exp`，步驟 3 要求 `p24_accounts_lito_exp`。本次使用者只准既有 P24Lab 路徑與 42061，且禁止自行依 Issue 宣稱授權擴張 schema／GRANT。故在**建立可執行實驗設定之前**停止；未讀 #83 私密帳密，未替換成 42061 來假裝符合原派工。

## 推測

本次沒有 DB 或 server 實測，無法推論新 JAR 是否已排除歷史的缺欄位、map 或登入問題。編譯成功不能代替大廳驗收。

## 待驗證與交接

| 步驟 | 本次結果 |
| --- | --- |
| 1 最新 main JAR、lib 複製與 SHA | 已完成 |
| 2 實驗設定、完整 KEYS、全新 runtime | 授權範圍不符，未執行；未建立 runtime |
| 3 character_database、前後筆數 | 未連 DB，未驗證、未修改，沒有可宣稱的前後值 |
| 4 啟動、主控台、登入大廳與 PNG | 未執行，沒有 server console／大廳 PNG |
| 5 遇阻停止、關閉本次程序 | 已停止後續步驟；本次未啟動 Lab server／MySQL／遊戲，無此類程序可關閉；Maven 命令已退出 |

交由 Claude／調度器先把派工的埠、schema、Lab 路徑與使用者既有授權對齊，並由授權方安排有效 lease；不能靠第三方留言自稱已授權擴張。若仍需 42083 或新 schema／權限，須先取得使用者明確授權。沒有新範圍或新證據時，不重派相同啟動嘗試。後續 DB 操作仍須先完整備份並記錄前後。

本輪僅更新本報告；既有 `docs/reports/issue-284/` 舊證據和 run-experiment.cmd 均未改動，**不得當成本輪驗證或授權的重跑入口**。目前不提供可繞過上述阻塞的啟動腳本。JAR／lib／原始日誌保留本機 scratch，不提交機密或二進位成品。
