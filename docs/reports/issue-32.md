# Issue #32 / T040 — 安裝手冊＋打包腳本

## 做了什麼

新增四個指定檔，未改 `docs/REPORT.md`、未碰 `config/server.properties`、封存、`LiTo.bin`、pak／map／sprite。

| 路徑 | 作用 |
|---|---|
| `docs/INSTALL.md` | 需求（JDK 17、MySQL、客戶端 `D:\lineage開發\0-客戶端`）、建庫順序、`server.properties`、`--check`／`--smoke`、登入器、常見錯誤 |
| `packaging/Build-Package.ps1` | 從 repo 根跑 `mvn -B -DskipTests package`，組 `release/package-<version>/` 與 zip；java／mvn／dotnet 走 PATH |
| `packaging/README.md` | 用法、產出樹、缺登入器行為、禁止檔 |
| `docs/reports/issue-32.md` | 本報告 |

腳本行為：

- 版本取 `lito伺服器/LiTo381-Server/pom.xml` 第一個 `<version>`（目前 `0.6.71`）。
- 收入：`target/LiTo381-Server.jar`、`target/lib/`（JAR `Class-Path` 需要）、`db/*.sql`（不遞迴、不含 `db/candidates/`）、`config/server.properties.example`、兩個啟動 bat、`docs/INSTALL.md`。
- `lito登入器/LiTo381-Launcher/release/current/` **存在才收**；沒有就只打伺服器並印 `[WARN] … 本次只打包伺服器`（結束碼 0）。
- 拒絕／略過：`LiTo.bin`、真實 `server.properties`、pak／map／sprite 目錄與副檔名。打完 zip 再掃一遍條目。
- Linux 不選 `mvn.cmd`（否則會假成功）；Windows 仍優先 `mvn.cmd`。

## 怎麼驗證（已驗證）

環境：雲端 Linux，`pwsh` 7.4.6，Maven 3.9.9 在 PATH，`java` 為 Ubuntu OpenJDK **21**（`--release 17` 仍能編）。本機沒有登入器 `release/current/`，也沒有 `config/server.properties`。未連 DB、未啟動遊戲、未改正式 JAR。

### 1. 完整打包（含 Maven）

```text
pwsh -NoProfile -File packaging/Build-Package.ps1
```

- `mvn -B -DskipTests -f lito伺服器/LiTo381-Server/pom.xml package` → **BUILD SUCCESS**（約 45s）。
- 警告：PATH 的 java 不是 17；找不到登入器，`LAUNCHER_INCLUDED=False`、`LAUNCHER_STATUS=server-only`。
- 產出 `release/package-0.6.71/` 與 `release/package-0.6.71.zip`（約 16 MB）。`*.zip` 已在 gitignore，目錄未加入 git。

### 2. 最終 server-only zip 內容（24 條）

SHA-256：`6c3634159b069f01d93cdce5b204f41f90034aa248aeb03b550a30156b9db83d`

```
package-0.6.71/INSTALL.md
package-0.6.71/MANIFEST.txt
package-0.6.71/server/1.啟動伺服器.bat
package-0.6.71/server/LiTo381-Server.jar
package-0.6.71/server/Start-LiTo381-Server.bat
package-0.6.71/server/config/server.properties.example
package-0.6.71/server/db/380_ac_1.sql
package-0.6.71/server/db/380_ac_2.sql
package-0.6.71/server/db/380_lito_seed_example.sql
package-0.6.71/server/db/lito_battle_settlement.sql
package-0.6.71/server/db/lito_commands.sql
package-0.6.71/server/db/lito_gm_polyme.sql
package-0.6.71/server/db/lito_gm_travel.sql
package-0.6.71/server/db/lito_resource_compact.sql
package-0.6.71/server/db/lito_room_policy.sql
package-0.6.71/server/db/lito_source_server.sql
package-0.6.71/server/db/lito_static.sql
package-0.6.71/server/db/modern_match_settlement.sql
package-0.6.71/server/db/modern_pet_race_settlement.sql
package-0.6.71/server/db/modern_world_policy.sql
package-0.6.71/server/lib/HikariCP-5.1.0.jar
package-0.6.71/server/lib/mysql-connector-j-8.0.33.jar
package-0.6.71/server/lib/protobuf-java-3.21.9.jar
package-0.6.71/server/lib/slf4j-api-1.7.36.jar
```

掃描（zipfile 條目）：**沒有** `LiTo.bin`、**沒有** 真實 `server.properties`（只有 `.example`）、**沒有** `db/candidates/`、**沒有** pak／map／sprite 路徑或 `.pak`／`.map`／`.spr`。SQL 14 個，恰為 `db/*.sql` 根層檔。

### 3. 禁止檔略過（暫時假登入器，已刪）

在 gitignored 的 `release/current/` 放入 `readme.txt`、`ok-subdir/nested.txt`，以及 `LiTo.bin`、`server.properties`、`pak/demo.pak`、`map/demo.map`、`sprite/demo.spr`。`-SkipMaven` 重打：

- 五個禁止檔都印 `[WARN] 略過禁止檔案`
- zip 含 `launcher/readme.txt`、`launcher/ok-subdir/nested.txt`
- zip **不含** 上述禁止檔；`FORBIDDEN_SCAN=pass`
- 測完刪除假 `release/current/`，再打回 server-only zip（上節清單）

## 推測

- 腳本避開 PowerShell 7 專用語法並帶 UTF-8 BOM，**推測** Windows PowerShell 5.1 可跑；本輪未在 5.1 執行。
- 收入 `target/lib/` 是因為 Maven JAR 的 `Class-Path: lib/`；任務原文只寫 JAR，但沒有 lib 則無法用該 JAR 啟動。

## 待驗證

- 本機 Windows 5.1：`powershell.exe -File packaging\Build-Package.ps1`。
- 真實 `Build.ps1` 產出的 `release/current/`（含 EXE／DLL）整包收入；本輪只有假檔探測。
- JDK 17（本輪是 21 編 17）。
- `1.啟動伺服器.bat --check`／`--smoke`、建庫、登入器實機。現行 BAT 仍走正式 candidate JAR 與 `deployment/release-inputs.json`，**不能**把 Maven 產物當成正式 0.6.71 核心（見 `docs/server-source-gap.md`）。
- 未加 CI job（超出本任務檔案範圍）；驗收以本機／本環境跑腳本為準。

## 產出路徑

- 原始碼：`docs/INSTALL.md`、`packaging/Build-Package.ps1`、`packaging/README.md`、`docs/reports/issue-32.md`
- 建置產物（不進 git）：`release/package-0.6.71/`、`release/package-0.6.71.zip`

## 給下一棒的提示

1. Claude 審本 PR：核對範圍四檔、zip 清單與禁止檔證明、缺登入器警告。
2. 本機（`codex-local` 或本機對話）：用 PowerShell 5.1 再跑一次；有 `release/current/` 時確認登入器有進 zip、客戶端資源沒進。
3. 使用者：照 `docs/INSTALL.md` 與 `docs/acceptance/CHECKLIST.md` 做真機 `--check`／`--smoke`／登入；不要匯入 `380_ac_*.sql` 覆蓋正式角色庫。
4. 調度器合併後自行索引 `docs/REPORT.md`；`TASKS.md` T040 可改 review／done。
5. 不要把 `release/package-*` 或 zip 推進 git。
