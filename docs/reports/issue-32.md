# Issue #32 / T040 — 安裝手冊＋打包腳本

## 做了什麼

審查退回後（PR #33 / `357c1dc`）在同一分支 `task/32` 修正，未改 `docs/REPORT.md`、未碰 `config/server.properties`、封存、`LiTo.bin`、pak／map／sprite。

| 路徑 | 作用 |
|---|---|
| `docs/INSTALL.md` | 需求、建庫（`lito_resource_compact.sql` **僅首次**）、套件自帶入口 vs 官方 BAT 補充包 |
| `packaging/Build-Package.ps1` | 依官方相對路徑組目錄；產生套件入口；禁止檔掃描 |
| `packaging/Start-PackagedServer.ps1`、`packaging/啟動套件伺服器.bat` | 套件自帶入口：PATH 上的 `java` + 包內 Maven JAR |
| `packaging/scan-zip.py` | 與打包腳本獨立的 zip 掃描 |
| `packaging/README.md` | 產出樹、gitignore 實況、artifact 名稱 |
| `.github/workflows/package.yml` | CI 跑 `Build-Package.ps1`、`scan-zip.py`、套件 `--check`，上傳 artifact `lito-package` |
| `.gitignore` | 補 `/release/`，避免 sql／bat 副本被 `git add` |
| `docs/reports/issue-32.md` | 本報告 |

**P1 啟動鏈**：官方 `1.啟動伺服器.bat` 放到 `lito伺服器/`，使其呼叫的 `LiTo381-Server/Start-LiTo381-Server.bat` 與 `tools/Start-LiToServer.ps1` 都在套件內（官方腳本**未改字**）。另產生 `啟動套件伺服器.bat` → `tools/Start-PackagedServer.ps1`，用 PATH 的 java 跑包內 `LiTo381-Server.jar`。INSTALL 寫明：只解壓本包時用套件入口；官方 `--check`／`--smoke` 要完整 repo 的 candidate JAR、catalog、`deployment/release-inputs.json`。

**P2 SQL**：`lito_resource_compact.sql` 改標「僅首次」；已有 `lito_resource_*` 就跳過，不要 DROP。

**P3 證據**：新增 workflow `package`。既有 `build.yml` 三個 job **不是**打包驗收。

## 怎麼驗證

### 可重跑指令（repo 根）

```bash
pwsh -NoProfile -File packaging/Build-Package.ps1
python3 packaging/scan-zip.py release/package-0.6.71.zip
pwsh -NoProfile -File release/package-0.6.71/tools/Start-PackagedServer.ps1 --check
```

獨立核實位置（本 PR 合併前）：GitHub Actions → workflow **package** → job **Build-Package.ps1 zip** → artifact **`lito-package`**（內含 zip、`MANIFEST.txt`、`ZIP-LISTING.txt`、`package-scan.txt`）。  
**不要**把 `build.yml` 的 Server／Server tests／Launcher 綠燈當成打包驗收（那些 job 不跑 `Build-Package.ps1`）。zip／`release/` 不進 git（`/release/` 與 `*.zip`）。

本輪雲端（pwsh 7.4.6、OpenJDK 21 編 `--release` 17、已有 target JAR 故用 `-SkipMaven`；未連 DB、未啟動遊戲）：

### 1. 打包腳本輸出（節錄）

```
STARTUP_CHAIN=ok (wrapper bat → LiTo381-Server/Start-LiTo381-Server.bat → tools/Start-LiToServer.ps1)
PACKAGE_ENTRY=ok (啟動套件伺服器.bat → tools/Start-PackagedServer.ps1 → Maven JAR)
[WARN] 找不到登入器 lito登入器/LiTo381-Launcher/release/current。本次只打包伺服器。…
LAUNCHER_INCLUDED=False
ZIP_ENTRY_COUNT=27
FORBIDDEN_SCAN=pass (no LiTo.bin, server.properties, pak/map/sprite)
LAUNCHER_STATUS=server-only
```

本機此次 zip SHA-256：`3ececcd596de34d40bc02375b11055b6e03553fefcd0a194e0aafe40ce8d0005`（16 886 500 bytes）。CI 會重跑 `mvn package`，**artifact 內 zip 的 SHA 以該 run 為準**，不要用本行取代 CI 產物。

### 2. `python3 packaging/scan-zip.py release/package-0.6.71.zip` 輸出

```
ZIP=release/package-0.6.71.zip
ZIP_BYTES=15886500
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

### 3. 套件入口 `--check`（不連 DB）

```
MODE=check
LIB_JAR_COUNT=4
CONFIG=missing
[PASS] 套件入口檢查完成：PATH 上的 java、Maven JAR、lib/、server.properties.example。
[PASS] --check 不要求已填的 server.properties；啟動／--smoke 才需要。
```

結束碼 0。

### 4. 禁止檔略過（假登入器，已刪）

暫放 `LiTo.bin`、`server.properties`、`pak/demo.pak` 於 gitignored `release/current/`。腳本印 `[WARN] 略過禁止檔案`；`scan-zip.py` 仍 `FORBIDDEN_SCAN=pass`。測完已刪。

## 推測

- 腳本避開 PowerShell 7 專用語法並帶 UTF-8 BOM；本機審查曾在 5.1 **解析**通過，完整 5.1 打包仍待本機跑。
- 官方 `Start-LiToServer.ps1` 原樣收入後，相對路徑不再缺檔，但硬編 JDK／candidate JAR／catalog 仍在，解壓目錄單獨執行官方 BAT 預期失敗。

## 待驗證

- Windows PowerShell 5.1 完整 `Build-Package.ps1`（含 Maven）。
- 真實 `Build.ps1` 的 `release/current/`（含 EXE）。
- 套件 `--smoke`／無參數啟動 Maven JAR（可能因來源缺口或沒有 catalog 失敗）。
- 官方 `1.啟動伺服器.bat --check`／`--smoke`、建庫、實機登入。
- 本報告送出時 workflow `package` 尚未跑；請用該 run 的 artifact 核對 zip，不要用上一輪 `build.yml` run 41823755950。

## 產出路徑

- 原始碼：上表「做了什麼」各檔
- 建置產物（不進 git）：`release/package-0.6.71/`、`release/package-0.6.71.zip`
- CI 證據：artifact `lito-package`

## 給下一棒的提示

1. Claude／本機審查：等 PR #33 的 **package** workflow 綠燈後，下載 artifact `lito-package`，重跑 `python3 packaging/scan-zip.py <artifact.zip>`，確認 27 條與禁止檔。不要把 Server／Launcher CI 當成打包驗收。
2. 本機 5.1 可再跑一次完整打包；有登入器 release 時確認 EXE 有進、客戶端資源沒進。
3. 使用者：套件入口見 INSTALL §4.1；正式 `--check`／`--smoke` 見 §4.2。不要重跑 `lito_resource_compact.sql` 去 DROP 既有表。
4. 調度器合併後自行索引 `docs/REPORT.md`；`TASKS.md` T040 可改 review／done。
