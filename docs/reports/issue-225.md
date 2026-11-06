# #225 / T050 — src/main 候選建置與 M0 前置核對

結論：**blocked；M0 尚未完成**。本輪完成獨立來源、建置及靜態路由核對；沒有啟動 P24Lab、連接實際 DB、執行預檢／smoke 或登入大廳，沒有三張實機 PNG。跨專案排他安排缺少可核對的持有人與租約，不是要求使用者重複授權。

## 來源與範圍

- 本輪派工：[2026-11-06 #225](https://github.com/chaoren0418-arch/lito-launcher-server/issues/225#issuecomment-6676914115)。開始與交付前核對 open / runner:dot / task:running 及沒有同輪 RESULT。
- 從當時最新 `origin/main` 的 `a2bd63fa2f968f59f72d8b3a683c48fc4e88107c` 建立獨立 `task/225` worktree；沒有切換共用主目錄或覆蓋其他任務。
- #78 PR #264 合併 `4d622702eec9835cea159727bbe4c9d38ca21298`；#83 PR #280 合併 `e2b790bd57efab5847769e091d7646737b218a5d` 均通過 `git merge-base --is-ancestor <SHA> HEAD`。
- #78 頭版本 `1015196c8b4957dcbf20554693c8b4472a32c8db` 至本輪基準的 src、pom、compile-stubs 無差異；tools 的差異只在 semantic-diff，不是 Maven 生產編譯流程。
- 未讀取／修改正式設定、私密憑證內容、#83 的正式匯出 dump/datadir、正式 JAR、原 BIN/client、封存或 `docs/REPORT.md`。只交付本報告；建置產物與完整日誌留在本任務 target/scratch。

## 建置與單元測試

- `mvn-test-final.log`：exit 0 / BUILD SUCCESS；完成時間 `2026-11-06T13:03:14+08:00`；SHA-256 `969f295fee51cbf769b2bddf4b17716b9f707250353d5bbb128226c5b12f934f`。
  - 最終完整測試：`Tests run: 6050, Failures: 0, Errors: 0, Skipped: 110`（與 Surefire XML 總計核對一致）。
- `mvn-package.log`：exit 0 / BUILD SUCCESS；完成時間 `2026-11-06T13:06:16+08:00`；SHA-256 `67552330ce4dbaf4ae3142d09e609b1fdd4b1dbc4e311b3ba2031d5c828fcf41`。
- 工具：Microsoft OpenJDK 17.0.17 / Maven 3.9.16。`-DskipTests package` 是測試成功後執行的獨立打包，不能當作額外測試通過。完整日誌位於本 worktree 的 `scratch/t050/`。

首輪完整測試（`scratch/t050/mvn-test-authorized.log`）：6050 tests / 0 failures / 2 errors / 110 skipped，exit 1。兩項錯誤分別是 `GameHubVirtualPlayerTest.databaseDumpsGrantTestGmAndRetainDatabaseCommandPolicy` 找不到 `db/380_ac_1.sql`，及 `ProtocolCoverageTransitiveBuilderAuditTest.generatedCoverageRecognizesProductionBuyListEmitter` 找不到 `SERVER-PROTOCOL-COVERAGE.json`。原因是本輪 sparse checkout 遺漏測試輔助檔；從同一 HEAD 補齊該 JSON 與三份 Git 已追蹤 SQL fixture 後完整重跑。沒有改測試、假造 fixture、連 DB 或執行 SQL。

執行前檢查 pom 與測試：真實 MySQL fixture 以 `LITO_MODERN_MYSQL_HOME` opt-in；legacy integration 以 system property 啟用；外部資源由 `LITO_SHARED_RESOURCE_ROOT` 提供。本輪子程序清除所有 `LITO_*`、`JAVA_TOOL_OPTIONS`、`JDK_JAVA_OPTIONS`、`MAVEN_ARGS`，未提供 DB opt-in 或秘密設定。測試所需模擬 socket 使用自行建立的 loopback ephemeral listener；沒有把正式埠號字串誤認為已連線。Git 的三份歷史測試 fixture 依 `db/README.zh-TW.md` 供純文字斷言使用，未執行或匯入 SQL。Maven repository、TEMP/TMP 與 java.io.tmpdir 都放本任務 scratch，未改全域設定。

第一次 sandbox 執行因 Central 依賴下載 `Permission denied: getsockopt` 失敗，尚未執行測試；使用核准的相同 Maven 流程取得網路權限後執行。這不是驗收成功證據，保留原始失敗日誌 `scratch/t050/mvn-test.log`。

## #78 語意證據與本輪 SHA

沿用 [#78 報告](https://github.com/chaoren0418-arch/lito-launcher-server/blob/a2bd63fa2f968f59f72d8b3a683c48fc4e88107c/docs/reports/issue-78.md) 與同目錄 `issue-78-evidence.json`，本輪沒有讀正式 JAR 或重做語意驗收：A 範圍 692 entries，DIFF / ONLY_LEFT / ONLY_RIGHT 均 0，包含已核准的 6 DEVIATION 與 1 SAME_LAYOUT。全 JAR 實際差異為 B 範圍 DIFF=186、ONLY_RIGHT=1；來源專有 ONLY_LEFT=1382；其餘 class 差異 0。這是 #78 實際數字，不以 Issue 的約 205／1380 代替。1366 個 non-class entries 相同，META-INF 3 差異已有 #78 說明。

- #78 候選 SHA-256：`b56e85f570f6ae9cfe947b3a3e1afb452ed93beadfd1e76d40b3537bfc1e7861`。
- 本輪 `target/LiTo381-Server.jar` SHA-256：`7f31cb3dc2b2f6548469a62de946322fe8fca5e63ccf952ddee1c7b6fec47843`。
- 本輪 JAR 共 5304 個檔案 entries、3935 個 class；與打包後當前 target/classes 的 class 差異 0；禁止打包前綴數 0。本輪未另存測試當時 class 雜湊，因此這項比對僅代表當前打包輸出，不冒稱精確測試時位元組核對。
- **整檔 SHA 與 #78 不同，Issue 的 SHA 一致條件未達成**；沒有修改或重封裝 JAR 來迎合雜湊。
- 以本機保留的 #78 候選（先驗證整檔 SHA 與報告相符）進行 ZIP entry 比對：only-old=0, only-new=0, 解壓後內容不同=0，entry timestamp 不同=5304。這是兩顆來源候選的比對，未讀正式 JAR。
- pom 沒有固定 outputTimestamp；上述內容與封裝 metadata 分開核對。請主控決定可重現建置修正或接受的候選識別方式；不能將 entry 內容相同擅自視為精確 SHA 條件已通過。
- 新候選及 runtime lib 副本保留在 `scratch/t050/target/`；未複製私密實驗設定、未覆蓋正式 JAR、未將建置產物放進 Git。

## 靜態路由核對（不是線上驗收）

以下行號均屬本輪基準的 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`：

- `AccountRepository.java:520–523` **已有** `SharedDatabase.enabled/open`；`ServerConfig.java:912` 已接 `SharedDatabase.applySources`。因此 #83 舊 checkout 的缺入口判斷不可直接沿用。
- `UnifiedConfiguration.java:47–55, 68–85` 從 `lito_source_server.character_database` 產生動態庫路由；`SharedDatabase.java:47–53` 用該庫建立唯讀 `legacy_mysql` repository。依 #83 已交付資料，預期來源應是 `p24_accounts_lito_exp`，不是 `380_db`。本輪沒有查詢實際資料或讀私密設定，不能聲稱線上接通。
- `SharedDatabase.open` 明確傳入 `legacy_mysql`，所以不能直接把 UnifiedConfiguration 的 `lito_mysql` 別名推斷成本次必然執行 DDL。沒有以執行 DDL 或擴權測試此事。
- **設定載入相容性仍需核對**：`Accounts.java:4199–4202` 直接讀取原始 properties，`:4224–4226` 要求 legacy URL／登入欄位，沒有呼叫 `UnifiedConfiguration.aliases`。`UnifiedConfiguration.java:38–45` 的別名轉換只在記憶體進行，`:72` 的連線設定路徑仍指向 canonical 檔。因此不能從「SharedDatabase 入口存在」推定單一設定已可成功建構 repository；本輪沒有讀取私密設定，請主控核對／修正載入方式，不增權或修改 DB。
- **仍需主控修正／確認的控制表路由**：`Accounts.java:12932–12936` 的 `resourceTable` 直接回傳 `resourceDatabase + "." + table`；該檔沒有 SharedDatabase 呼叫。`SharedDatabase.java:21–22, 64–67` 提供把 battle/control 表轉往控制庫的路由，但目前 helper 未從此方法呼叫。`tools/shared-database/PatchShared.java:28–34` 的歷史補丁包含這條接線，pom 沒有執行該補丁。這是靜態缺口，尚無本輪 SQL 錯誤可引用；不得靠增加 DB 權限或複製資料表掩蓋。
- [#83 報告](https://github.com/chaoren0418-arch/lito-launcher-server/blob/a2bd63fa2f968f59f72d8b3a683c48fc4e88107c/docs/reports/issue-83.md) 記載候選遊戲埠 42083；本輪委派僅准 P24Lab 43324/42061。請主控釐清候選端點與隔離安排。本輪沒有讀取或更動實驗設定來自行解決。
- 此版本未追蹤 `1.啟動伺服器.bat`／`Start-Server.ps1`。`SharedResourceMain` 呼叫 `Main`；`Main.java:17–25` 第一參數為設定路徑，後續僅支援 `--service-mode=`，**不可直接把 `--check`／`--smoke` 傳給 JAR 冒充預檢**。待前置解決後，需核對允許的本機啟動 wrapper，再提供等效的隔離實驗腳本與完整輸出。

## 本輪候選內地圖清單（靜態）

ZIP 內共 169 個 maps entries；以下數字均代表 `maps/<ID>.l38.gz`，全部 entries 均為此格式。`maps/12000.l38.gz` 不存在。這只能證明打包狀態，不能證明本輪預檢已重現 map12000 錯誤；本輪沒有查詢其 DB 定義來源。

```text
0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20
21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 35, 36, 37, 43, 44, 45, 46
47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 59, 60, 61, 62, 63, 64, 65, 66, 67, 70
75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110
209, 213, 237, 240, 241, 242, 243, 244, 252, 253, 254, 300, 301, 302, 303, 304, 305, 307, 308, 309
310, 320, 330, 350, 400, 401, 410, 420, 430, 440, 441, 442, 443, 444, 445, 446, 447, 450, 480, 481
482, 483, 484, 541, 542, 543, 550, 551, 552, 553, 554, 555, 556, 557, 558, 600, 601, 602, 603, 604
605, 606, 607, 613, 666, 778, 779, 1000, 1001, 1011, 2000, 16384, 16896, 17408, 17920, 18432, 18944, 19456, 19968, 20480
20992, 21504, 22016, 22528, 23040, 23552, 24064, 24576, 25088
```

若之後預檢出現 `source map physical asset not packaged: 12000 -> 12000`，依 [主控備註](https://github.com/chaoren0418-arch/lito-launcher-server/issues/225#issuecomment-6675002028) 停止並由主控處理 U0-6，不複製 pak/map 或改 DB。

## 未執行與下一棒

| 項目 | 本輪狀態 | 下一步 |
|---|---|---|
| 跨專案 P24Lab 排他 | blocked | 調度器提供涵蓋三 repo 的可核對租約、owner、有效期與取得／釋放機制 |
| 候選 SHA 一致性 | 未達成 | 主控處理可重現封裝或明定可接受的候選識別方式 |
| 實驗設定與候選端點 | 未線上驗證 | 主控釐清埠、設定載入與上列控制表路由；沿用受限帳號，不擴權、不重建帳號 |
| `--check` | 未執行，無輸出 | 排他與路由條件滿足後才核對 wrapper 並執行 |
| `--smoke` | 未執行，無輸出 | 預檢成功後執行 |
| 真實大廳／三張 PNG | 未執行 | 上述均通過後才做實機驗收 |
| map12000 DB 定義 | 未查詢 | 若預檢觸發已知錯誤，依主控備註停下，取得允許來源的唯讀表定義再回報 |

[#77 主控阻塞說明](https://github.com/chaoren0418-arch/tw-bin-engine/issues/77#issuecomment-6676853312) 要求不要自動重排；只有同 repo workdir mutex 或 #77 queued/blocked，不構成跨 repo 排他證據。上層 04:29–04:30 UTC 巡查也未取得跨 repo 持有人／租約；本輪沒有啟動 lab。已加 `needs:claude`。請主控／orchestrator 保持阻塞直到技術前置解決，逾時不要預設執行，也不要另建重複會話。本輪無自行啟動的 lab 程序需收尾；沒有把歷史「已關閉」當作本輪重新查核。

## 可重跑的獨立建置指令（cmd，每行一條）

在相同基準的獨立 worktree 根目錄，用 JDK 17.0.17 與 Maven 3.9.16；`mvn` 需已在 PATH；工作樹需包含上述三份 db fixture 與 `SERVER-PROTOCOL-COVERAGE.json`。以下只重跑本輪獨立建置，**不啟動實驗服務**：

```cmd
mkdir scratch\t050\tmp
mkdir scratch\t050\m2
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "TEMP=%CD%\scratch\t050\tmp"
set "TMP=%TEMP%"
set "MAVEN_OPTS=-Djava.io.tmpdir=%TEMP%"
set "JAVA_TOOL_OPTIONS="
set "JDK_JAVA_OPTIONS="
set "MAVEN_ARGS="
for /f "tokens=1 delims==" %V in ('set LITO_ 2^>nul') do set "%V="
call mvn -f lito伺服器/LiTo381-Server/pom.xml -B "-Dmaven.repo.local=%CD%\scratch\t050\m2" "-DargLine=-Djava.io.tmpdir=%TEMP%" test > scratch\t050\mvn-test-rerun.log 2>&1
call mvn -f lito伺服器/LiTo381-Server/pom.xml -B "-Dmaven.repo.local=%CD%\scratch\t050\m2" -DskipTests package > scratch\t050\mvn-package-rerun.log 2>&1
certutil -hashfile lito伺服器\LiTo381-Server\target\LiTo381-Server.jar SHA256
```

若存成 `.cmd`，迴圈變數 `%V` 改成 `%%V`。以上未省略已有執行的預檢／smoke 輸出：兩項根本尚未執行。安全且可用的 runtime 重跑命令仍是未完成交付，不提供猜測參數，也不把單元測試／文件交付當作 M0。
