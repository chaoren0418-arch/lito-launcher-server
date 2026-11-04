# Issue #32 / T040 — 安裝手冊＋打包腳本

## 做了什麼

主控 2026-11-04 核准擴大範圍後，在既有 `task/32`／PR #33 補回核准檔，並只修本輪兩項。未改 `docs/REPORT.md`、未碰 `config/server.properties`、封存、`LiTo.bin`、pak／map／sprite、官方啟動腳本、Server tests。

| 路徑 | 作用 |
|---|---|
| `docs/INSTALL.md` | 需求、建庫（`lito_resource_compact.sql` **僅首次**）、套件入口 vs 官方 BAT |
| `packaging/Build-Package.ps1` | 組包、假 java `--smoke` 引號探測、禁止檔掃描 |
| `packaging/Start-PackagedServer.ps1` | 套件入口：PATH `java` + Maven JAR；`--smoke` 對 `Start-Process -ArgumentList` 逐項加引號 |
| `packaging/啟動套件伺服器.bat` | 呼叫 `tools/Start-PackagedServer.ps1` |
| `packaging/scan-zip.py` | 獨立 zip 掃描 |
| `packaging/README.md` | 產出樹、gitignore、`package` workflow |
| `.github/workflows/package.yml` | CI 跑 `Build-Package.ps1`、`scan-zip.py`、套件 `--check`，上傳 artifact `lito-package` |
| `.gitignore` | 僅忽略 `/release/package-*`（另有既有 `*.zip`） |
| `docs/reports/issue-32.md` | 本報告 |

**本輪 1**：`--smoke` 將 `$jar`／`$properties` 逐項加引號後再傳 `Start-Process -ArgumentList`（與官方 `Start-LiToServer.ps1` 相同）。驗證用假 `java` 放在 PATH 前面，只寫 argv 後 sleep，不啟動 LiTo、不連 DB。兩種解壓根：含空白 `LiTo Test`、含中文 `中文路徑`。`--check` 不經過這段。

**本輪 2**：更正舊本機 zip 大小筆誤；本機數值標「執行者回報」；CI 用固定 run／artifact 連結，與本機分開列，不手改湊一致。

## 怎麼驗證

### 可重跑指令（repo 根）

```bash
pwsh -NoProfile -File packaging/Build-Package.ps1
python3 packaging/scan-zip.py release/package-0.6.71.zip
pwsh -NoProfile -File release/package-0.6.71/tools/Start-PackagedServer.ps1 --check
```

獨立核實：GitHub Actions → workflow **package** → job **Build-Package.ps1 zip** → artifact **`lito-package`**。  
`build.yml` 的 Server／Launcher **不是**打包驗收。上一輪 Server tests 不穩定另案處理，本 PR 不改該測試。zip／`release/package-*` 不進 git。

### A. 歷史 CI（獨立核實；head `ef44e7e`，不是本輪 zip）

- Run：https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41827854378 （success）
- Artifact：`lito-package` id `13319274922`  
  https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41827854378/artifacts/13319274922
- Artifact 上傳包 SHA-256（Actions 對 artifact zip 的 digest，**不是**內層 `package-0.6.71.zip`）：`e479ea86902400f4f853cf5bd7e76055d29fa15ca41b3e1fef644a851c80383c`
- 該 run 內層 zip：`ZIP_BYTES=15812615`、`ZIP_ENTRY_COUNT=27`、`FORBIDDEN_SCAN=pass`、Maven `BUILD SUCCESS`、`LAUNCHER_STATUS=server-only`、套件 `--check` `[PASS]`

本 head 的新 package job 以該 PR 的 **Build-Package.ps1 zip** 為準（與上列歷史 run 不必同 SHA）。

### B. 本輪本機（執行者回報；pwsh 7.4.6、OpenJDK 21、`-SkipMaven`；未連 DB、未啟動遊戲）

先前同一份本機 zip 曾誤寫 16 886 500 與 15 886 500；依當時 `scan-zip.py` 原始輸出，正確為 **15 886 500**（`ZIP_BYTES=15886500`）。那是執行者回報，不是獨立核實。

本輪因入口腳本加引號，zip 已不同：

```
ZIP_BYTES=15886970
ZIP_SHA256=b18dd08b28e563f64d7a5eda827cf01c177188fd3a72a6f6daaafb9742c133d2
ZIP_SHA256_SCOPE=executor-reported for this machine; not a CI artifact digest
ZIP_ENTRY_COUNT=27
FORBIDDEN_SCAN=pass
LAUNCHER_STATUS=server-only
```

### C. 假 java `--smoke` 引號探測（不是 `--check`；不啟動 LiTo／DB）

指令（由 `Build-Package.ps1` 執行；等價手動）：

```text
PATH=<repo>/scratch/issue-32-quote-probe/fake-java:$PATH
pwsh -NoProfile -File "<解壓根>/tools/Start-PackagedServer.ps1" --smoke
```

解壓根 1（含空白）：`scratch/issue-32-quote-probe/LiTo Test`  
解壓根 2（含中文）：`scratch/issue-32-quote-probe/中文路徑`  
假 java：只把 argv 寫到 `LITO_QUOTE_PROBE_OUT` 後 sleep 12 秒。

含空白原始輸出：

```
QUOTE_PROBE_CMD=PATH=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/fake-java:… /tmp/tools/pwsh/pwsh -NoProfile -File /agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test/tools/Start-PackagedServer.ps1 --smoke
JAVA=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/fake-java/java
QUOTE_PROBE_space=pass
QUOTE_PROBE_space_JAR=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test/lito伺服器/LiTo381-Server/LiTo381-Server.jar
QUOTE_PROBE_space_PROPS=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test/lito伺服器/LiTo381-Server/config/server.properties
QUOTE_PROBE_space_ARGV_BEGIN
-Dfile.encoding=UTF-8
-Djava.net.preferIPv4Stack=true
-jar
/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test/lito伺服器/LiTo381-Server/LiTo381-Server.jar
/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test/lito伺服器/LiTo381-Server/config/server.properties
--service-mode=standard
QUOTE_PROBE_space_ARGV_END
```

含中文原始輸出：

```
QUOTE_PROBE_CMD=PATH=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/fake-java:… /tmp/tools/pwsh/pwsh -NoProfile -File /agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/中文路徑/tools/Start-PackagedServer.ps1 --smoke
QUOTE_PROBE_chinese=pass
QUOTE_PROBE_chinese_JAR=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/中文路徑/lito伺服器/LiTo381-Server/LiTo381-Server.jar
QUOTE_PROBE_chinese_PROPS=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/中文路徑/lito伺服器/LiTo381-Server/config/server.properties
QUOTE_PROBE_chinese_ARGV_BEGIN
-Dfile.encoding=UTF-8
-Djava.net.preferIPv4Stack=true
-jar
/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/中文路徑/lito伺服器/LiTo381-Server/LiTo381-Server.jar
/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/中文路徑/lito伺服器/LiTo381-Server/config/server.properties
--service-mode=standard
QUOTE_PROBE_chinese_ARGV_END
QUOTE_PROBE=pass (fake java on PATH; --smoke Start-Process; no LiTo/DB)
```

JAR／properties 各為單一 argv，空白與「中文」未被拆開。

### D. `python3 packaging/scan-zip.py release/package-0.6.71.zip`（本輪本機，執行者回報）

```
ZIP=release/package-0.6.71.zip
ZIP_BYTES=15886970
ZIP_ENTRY_COUNT=27
ZIP_ENTRIES_BEGIN
package-0.6.71/INSTALL.md
package-0.6.71/MANIFEST.txt
package-0.6.71/lito伺服器/1.啟動伺服器.bat
package-0.6.71/lito伺服器/LiTo381-Server/LiTo381-Server.jar
package-0.6.71/lito伺服器/LiTo381-Server/Start-LiTo381-Server.bat
package-0.6.71/lito伺服器/LiTo381-Server/config/server.properties.example
package-0.6.71/lito伺服器/LiTo381-Server/db/380_ac_1.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/380_ac_2.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/380_lito_seed_example.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_battle_settlement.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_commands.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_gm_polyme.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_gm_travel.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_resource_compact.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_room_policy.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_source_server.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/lito_static.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/modern_match_settlement.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/modern_pet_race_settlement.sql
package-0.6.71/lito伺服器/LiTo381-Server/db/modern_world_policy.sql
package-0.6.71/lito伺服器/LiTo381-Server/lib/HikariCP-5.1.0.jar
package-0.6.71/lito伺服器/LiTo381-Server/lib/mysql-connector-j-8.0.33.jar
package-0.6.71/lito伺服器/LiTo381-Server/lib/protobuf-java-3.21.9.jar
package-0.6.71/lito伺服器/LiTo381-Server/lib/slf4j-api-1.7.36.jar
package-0.6.71/lito伺服器/LiTo381-Server/tools/Start-LiToServer.ps1
package-0.6.71/tools/Start-PackagedServer.ps1
package-0.6.71/啟動套件伺服器.bat
ZIP_ENTRIES_END
FORBIDDEN_SCAN=pass
FORBIDDEN_ABSENT=LiTo.bin,server.properties,pak/map/sprite,db/candidates
HAS_EXAMPLE=True
HAS_PACKAGE_BAT=True
HAS_PACKAGE_PS1=True
HAS_WRAPPER_BAT=True
HAS_START_BAT=True
HAS_OFFICIAL_START_PS1=True
HAS_SERVER_JAR=True
STARTUP_CHAIN=ok
```

14 個根層 `db/*.sql` 都在；無 `candidates/`；只有 `server.properties.example`。

### E. 套件入口 `--check`（不連 DB；不是引號探測）

```
MODE=check
LIB_JAR_COUNT=4
CONFIG=missing
[PASS] 套件入口檢查完成：PATH 上的 java、Maven JAR、lib/、server.properties.example。
[PASS] --check 不要求已填的 server.properties；啟動／--smoke 才需要。
```

結束碼 0。

## 推測

- Linux pwsh 的 `Start-Process -ArgumentList` 同樣以空白串接；逐項加引號後假 java 收到完整路徑。Windows 5.1 預期相同，尚未在 5.1 實跑。
- 官方 BAT 相對路徑完整，但硬編 JDK／candidate JAR／catalog 仍在，只解壓本包跑官方 BAT 預期失敗。

## 待驗證

- 本 head 的 package workflow **Build-Package.ps1 zip**（送出後看該 run）。
- `build.yml` Server／Launcher（Server tests 不穩定另案，本 PR 不改）。
- Windows PowerShell 5.1 完整打包與假 java 探測。
- 真實 `release/current/`（含 EXE）。
- 套件對**真** `java` 的 `--smoke`／無參數啟動、官方 BAT、建庫、實機登入。

## 產出路徑

- 原始碼：上表「做了什麼」各檔
- 建置產物（不進 git）：`release/package-0.6.71/`、`release/package-0.6.71.zip`
- 歷史 CI：artifact `lito-package`（run 41827854378）
- 本 head CI：同一 artifact 名，以新 package job 為準

## 給下一棒的提示

1. Claude／本機：等 PR #33 的 **package** job 綠燈後下載 `lito-package`，與本機 `ZIP_BYTES=15886970` 分開看，不必同 SHA。Server tests flake 不要在本 PR 修。
2. 本機 5.1 可再跑完整打包；有登入器 release 時確認 EXE 有進。不要 `git add release/`。
3. 使用者：套件入口見 INSTALL §4.1；正式 `--check`／`--smoke` 見 §4.2。不要重跑 `lito_resource_compact.sql` 去 DROP 既有表。
4. 調度器合併後自行索引 `docs/REPORT.md`；`TASKS.md` T040 可改 review／done。
