# 打包腳本（T040）

從 repo 根目錄組出廠商安裝包：`release/package-<version>/` 與 `release/package-<version>.zip`。

## 前置

| 工具 | 解析方式 |
|---|---|
| JDK 17 `java` | PATH；不寫死 `C:\Program Files\...` |
| Maven 3.9 `mvn` / `mvn.cmd` | PATH |
| `dotnet` | PATH；本腳本**不重建**登入器，只在 `release/current/` 存在時收入 |

路徑全部相對 repo 根（`packaging/Build-Package.ps1` 的上一層）。請在 repo 根執行。

## 執行

Windows PowerShell 5.1：

```powershell
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File packaging\Build-Package.ps1
```

PowerShell 7（含 GitHub `ubuntu-latest` 的 `pwsh`）：

```powershell
pwsh -NoProfile -File packaging/Build-Package.ps1
python3 packaging/scan-zip.py release/package-0.6.71.zip
pwsh -NoProfile -File release/package-0.6.71/tools/Start-PackagedServer.ps1 --check
```

本腳本會執行 `mvn -B -DskipTests -f lito伺服器/LiTo381-Server/pom.xml package`。若只要重打包既有 `target/` 產物，可加 `-SkipMaven`（驗收仍應跑完整一次）。

打包時會把假 `java` 放到 PATH 前面，對解壓到「含空白」與「含中文」的目錄跑套件 `--smoke`（只印 argv、不啟動 LiTo／不連 DB）。`-SkipQuoteProbe` 可略過。

獨立核實：workflow **package**（`.github/workflows/package.yml`）跑上述腳本，並上傳 artifact **`lito-package`**（zip、MANIFEST、ZIP-LISTING、scan 輸出）。既有 `build.yml` 的 Server／Launcher job **不是**打包驗收。

## 產出

```
release/package-<pom.version>/
  INSTALL.md
  MANIFEST.txt
  啟動套件伺服器.bat                 # 套件自帶入口（PATH java + Maven JAR）
  tools/Start-PackagedServer.ps1
  lito伺服器/
    1.啟動伺服器.bat                 # 官方包裝：呼叫 LiTo381-Server\Start-LiTo381-Server.bat
    LiTo381-Server/
      Start-LiTo381-Server.bat       # 官方：呼叫 tools\Start-LiToServer.ps1
      tools/Start-LiToServer.ps1     # 官方腳本原樣收入；未改內容
      LiTo381-Server.jar
      lib/
      db/*.sql                       # 僅 db 根層，不含 db/candidates/
      config/server.properties.example
  launcher/                          # 僅當 lito登入器/LiTo381-Launcher/release/current/ 存在
release/package-<pom.version>.zip
```

如何使用包內 Maven JAR、官方 BAT 的目的地與既有檔清單：見套件內／`docs/INSTALL.md` 第 4 節。

## gitignore（不要把產出推進 git）

根 `.gitignore`：

- `/release/package-*`：本打包目錄與 zip（T040）
- `*.zip`：zip 不進 git
- `lito登入器/LiTo381-Launcher/release/`：登入器自己的發布目錄，與本打包目錄不是同一條規則

不要 `git add release/`。zip 證據放 GitHub Actions artifact `lito-package`。

## 登入器缺檔

T003（Issue #4）已完成，但雲端／CI 通常沒有 `release/current/`。腳本會：

- 繼續打包伺服器
- 印出 `[WARN] 找不到登入器 … 本次只打包伺服器`
- `MANIFEST.txt` 寫 `launcher_included=False`
- 結束碼仍為 0

本機有登入器時，先跑 `lito登入器/LiTo381-Launcher/Build.ps1`（dotnet 走 PATH）再打包。

## 禁止收入（腳本會拒絕或略過）

- `LiTo.bin`
- `config/server.properties`（真實設定，含密碼）
- `pak`／`map`／`sprite` 目錄與 `*.pak`／`*.map`／`*.spr`／`*.til`／`*.idx`
- `db/candidates/`

打包後 `Assert-ZipClean` 再掃 zip；CI 另跑 `packaging/scan-zip.py`。命中即失敗。

## 啟動鏈與相容性

- **套件自帶入口** `啟動套件伺服器.bat`：使用包內 Maven JAR。`--check` 不連 DB。`--smoke` 的 `Start-Process` 會對路徑加引號。
- **官方 BAT**：相對路徑在套件內是完整的（含 `tools/Start-LiToServer.ps1`），但該腳本仍要求完整 repo 的 candidate JAR、`common_catalog.bin`、`deployment/release-inputs.json`、硬編 JDK。只解壓本包不能當成正式 `--check`／`--smoke`。
- Maven 產物 **不是** 正式 0.6.71 candidate JAR（`docs/server-source-gap.md`）。
- 不啟停資料庫、不啟動遊戲、不複製客戶端資源。不改正式啟動腳本、機密設定、封存或 `releases/`。
