# Issue #32 / T040 — 安裝手冊＋打包腳本

## 做了什麼

第 3 次：在既有 `task/32`／PR #33 處理審查退回。未改 `docs/REPORT.md`、未碰 `config/server.properties`、封存、`LiTo.bin`、pak／map／sprite、官方啟動腳本。

核准範圍僅四檔（相對 `main` 淨差異）：

| 路徑 | 作用 |
|---|---|
| `docs/INSTALL.md` | 需求、建庫（`lito_resource_compact.sql` **僅首次**）、套件自帶入口 vs 官方 BAT 補充包 |
| `packaging/Build-Package.ps1` | 組包、**產生**套件入口、`Start-Process` 引號探測、禁止檔掃描與 zip 清單 |
| `packaging/README.md` | 產出樹、gitignore 實況（不宣稱根 `release/` 被忽略） |
| `docs/reports/issue-32.md` | 本報告 |

前一提交越界檔已收回／刪除：`.github/workflows/package.yml`、`.gitignore` 的 `/release/`、`packaging/Start-PackagedServer.ps1`、`packaging/scan-zip.py`、`packaging/啟動套件伺服器.bat`。啟動鏈與禁止檔掃描改由允許的 `Build-Package.ps1` 產生與列印，不另開 workflow。

**啟動鏈**：官方 `1.啟動伺服器.bat` 放到套件 `lito伺服器/`（相對路徑完整，`Start-LiToServer.ps1` 原樣收入未改字）。套件自帶入口由腳本寫入 `啟動套件伺服器.bat` → `tools/Start-PackagedServer.ps1`（PATH `java` + 包內 Maven JAR）。`--smoke` 對 `Start-Process -ArgumentList` 逐項加引號。

**SQL**：`lito_resource_compact.sql` 僅首次；已有 `lito_resource_*` 則跳過，不要 DROP。

## 怎麼驗證

### 可重跑指令（repo 根）

```bash
pwsh -NoProfile -File packaging/Build-Package.ps1
pwsh -NoProfile -File release/package-0.6.71/tools/Start-PackagedServer.ps1 --check
```

`--check` **不**經過 `Start-Process`。引號探測在打包腳本內自動跑（`-SkipQuoteProbe` 可略過），使用擷取程式、含空白與中文的路徑，不啟動伺服器／DB。

zip／`release/package-*` **不進 git**。根 `.gitignore` 忽略 `*.zip` 與登入器 `lito登入器/LiTo381-Launcher/release/`，**沒有**忽略 repo 根 `release/`；不要 `git add release/`。

既有 `build.yml` 三個 job **不是**打包驗收。本輪已移除 `package.yml`（範圍外）；下列 CI 是**上一 head** 的歷史獨立證據，與本輪本機 zip 不是同一檔。

### A. 歷史 CI（獨立核實；head `ef44e7e`，不是本輪 zip）

- Run：https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41827854378 （success；PR merge checkout `855a827`）
- Artifact：`lito-package` id `13319274922`，下載 https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41827854378/artifacts/13319274922
- Artifact 上傳包 SHA-256（Actions 對 artifact zip 的 digest，**不是**內層 `package-0.6.71.zip`）：`e479ea86902400f4f853cf5bd7e76055d29fa15ca41b3e1fef644a851c80383c`
- Artifact 大小：15 699 800 bytes（包裝後的 artifact；內層 zip 見下）

`scan-zip.py` 原始輸出（該 run 日誌）：

```
ZIP_BYTES=15812615
ZIP_ENTRY_COUNT=27
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

同 run 另有 Maven `BUILD SUCCESS`、`LAUNCHER_STATUS=server-only`、套件 `--check` `[PASS]`。此 CI zip（15 812 615 bytes）與任何本機 zip **不必**同 SHA 或大小。

### B. 本輪本機（執行者回報；pwsh 7.4.6、OpenJDK 21、`-SkipMaven`；未連 DB、未啟動遊戲）

**不是**獨立核實。未重跑 `mvn package`（沿用既有 `target/` JAR）。修正前一報告筆誤：同一份舊本機 zip 曾誤寫 16 886 500 與 15 886 500；正確為 **15 886 500**（`ZIP_BYTES=15886500`）。本輪因產生腳本內容變更，zip 已不同：

```
GENERATED_SMOKE_QUOTING=ok (Start-Process uses quoted ArgumentList)
QUOTE_JOIN=ok (space-joined Start-Process command line keeps quoted paths with space+測試)
QUOTE_PROBE=pass host=/tmp/tools/pwsh/pwsh (Start-Process --smoke ArgumentList; no LiTo/DB)
QUOTE_PROBE_JAR=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test 測試/LiTo381-Server.jar
QUOTE_PROBE_PROPS=/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test 測試/server.properties
STARTUP_CHAIN=ok (wrapper bat → LiTo381-Server/Start-LiTo381-Server.bat → tools/Start-LiToServer.ps1)
PACKAGE_ENTRY=ok (generated 啟動套件伺服器.bat → tools/Start-PackagedServer.ps1 → Maven JAR)
[WARN] 找不到登入器 lito登入器/LiTo381-Launcher/release/current。本次只打包伺服器。…
LAUNCHER_INCLUDED=False
ZIP_ENTRY_COUNT=27
FORBIDDEN_SCAN=pass (no LiTo.bin, server.properties, pak/map/sprite)
LAUNCHER_STATUS=server-only
ZIP_BYTES=15886780
ZIP_SHA256=712a83975bb602f9009e8673d587438ffa4abbfc8958cdbfebc4af1be80a2135
ZIP_SHA256_SCOPE=executor-reported for this machine; not a CI artifact digest
```

擷取程式 `args.txt`（`--smoke` 同等 `Start-Process` argv，路徑含空白與 `測試`）：

```
-Dfile.encoding=UTF-8
-Djava.net.preferIPv4Stack=true
-jar
/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test 測試/LiTo381-Server.jar
/agent/repos/lito-launcher-server/scratch/issue-32-quote-probe/LiTo Test 測試/server.properties
--service-mode=standard
```

zip 內容（腳本 `ZIP_ENTRIES_*` 原始輸出，27 條）：

```
package-0.6.71/INSTALL.md
package-0.6.71/lito伺服器/1.啟動伺服器.bat
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
package-0.6.71/lito伺服器/LiTo381-Server/LiTo381-Server.jar
package-0.6.71/lito伺服器/LiTo381-Server/Start-LiTo381-Server.bat
package-0.6.71/lito伺服器/LiTo381-Server/tools/Start-LiToServer.ps1
package-0.6.71/MANIFEST.txt
package-0.6.71/tools/Start-PackagedServer.ps1
package-0.6.71/啟動套件伺服器.bat
```

14 個根層 `db/*.sql` 都在；無 `candidates/`；只有 `server.properties.example`。`HAS_*=True`。獨立 Python 再掃同一 zip：`HITS none`、`ZIP_BYTES 15886780`。

### C. 套件入口 `--check`（不連 DB；不是引號探測）

```
MODE=check
LIB_JAR_COUNT=4
CONFIG=missing
[PASS] 套件入口檢查完成：PATH 上的 java、Maven JAR、lib/、server.properties.example。
[PASS] --check 不要求已填的 server.properties；啟動／--smoke 才需要。
```

結束碼 0。

## 推測

- `Start-Process -ArgumentList` 在 Linux pwsh 7.4.6 同樣以空白串接（未加引號時 `-File` 路徑在空白處被拆開，exit 64）。逐項加引號後擷取程式拿到完整含空白／中文路徑。Windows 5.1 預期相同，尚未在 5.1 實跑。
- 官方 `Start-LiToServer.ps1` 相對路徑不再缺檔，但硬編 JDK／candidate JAR／catalog 仍在，只解壓本包跑官方 BAT 預期失敗。

## 待驗證

- Windows PowerShell 5.1 完整 `Build-Package.ps1`（含 Maven 與引號探測）。
- 真實 `Build.ps1` 的 `release/current/`（含 EXE）。
- 套件 `--smoke`／無參數啟動 Maven JAR（可能因來源缺口或沒有 catalog 失敗）。引號探測**不是** `--smoke` 實機。
- 官方 `1.啟動伺服器.bat --check`／`--smoke`、建庫、實機登入。
- 本輪沒有新的 package workflow；獨立下載仍用歷史 run 41827854378。不要把 `build.yml` 綠燈當成打包驗收。

## 產出路徑

- 原始碼：上表四檔（相對 `main`）
- 建置產物（不進 git）：`release/package-0.6.71/`、`release/package-0.6.71.zip`
- 歷史 CI 證據：artifact `lito-package`（run 41827854378，head `ef44e7e`）

## 給下一棒的提示

1. Claude／本機審查：`git diff main` 應只有核准四檔。下載歷史 artifact 只代表上一 head；本輪 zip 以腳本 stdout 的 `ZIP_BYTES`／清單為準，標為執行者回報。
2. 本機 5.1 可再跑一次完整打包與引號探測；有登入器 release 時確認 EXE 有進、客戶端資源沒進。不要 `git add release/`。
3. 使用者：套件入口見 INSTALL §4.1；正式 `--check`／`--smoke` 見 §4.2。不要重跑 `lito_resource_compact.sql` 去 DROP 既有表。
4. 調度器合併後自行索引 `docs/REPORT.md`；`TASKS.md` T040 可改 review／done。
