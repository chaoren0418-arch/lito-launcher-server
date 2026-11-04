# 打包腳本（T040）

從 repo 根目錄組出廠商安裝包：`release/package-<version>/` 與 `release/package-<version>.zip`。

本目錄只保留 `Build-Package.ps1` 與本 README（Issue #32 核准範圍）。套件自帶入口（`啟動套件伺服器.bat`、`tools/Start-PackagedServer.ps1`）由打包腳本**寫進產出目錄**，不作為 repo 來源檔。

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

PowerShell 7（含 Linux `pwsh`）：

```powershell
pwsh -NoProfile -File packaging/Build-Package.ps1
pwsh -NoProfile -File release/package-0.6.71/tools/Start-PackagedServer.ps1 --check
```

腳本會：

1. 跑 `mvn -B -DskipTests -f lito伺服器/LiTo381-Server/pom.xml package`（`-SkipMaven` 可沿用既有 `target/`）
2. 產生套件入口（PATH `java` + 包內 Maven JAR）
3. 用不啟動伺服器／DB 的擷取程式驗證 `Start-Process -ArgumentList` 對含空白與中文路徑的引號（`-SkipQuoteProbe` 可略過）
4. 組 zip，並列出內容、`ZIP_BYTES`／`ZIP_SHA256`（執行者本機回報）與禁止檔掃描

既有 `build.yml` 的 Server／Launcher job **不是**打包驗收。

## 產出

```
release/package-<pom.version>/
  INSTALL.md
  MANIFEST.txt
  啟動套件伺服器.bat                 # 腳本產生：PATH java + Maven JAR
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

- `*.zip`：zip 不進 git
- `lito登入器/LiTo381-Launcher/release/`：登入器自己的發布目錄
- **沒有**忽略 repo 根的 `release/package-*`（sql／bat／MANIFEST 副本可能被 `git add`）

不要 `git add release/`。zip 與目錄產物都不應進 git。

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

打包後 `Assert-ZipClean` 再掃 zip。命中即失敗。

## 啟動鏈與相容性

- **套件自帶入口** `啟動套件伺服器.bat`：使用包內 Maven JAR。`--check` 不連 DB。`--smoke` 的 `Start-Process` 會對路徑加引號。
- **官方 BAT**：相對路徑在套件內是完整的（含 `tools/Start-LiToServer.ps1`），但該腳本仍要求完整 repo 的 candidate JAR、`common_catalog.bin`、`deployment/release-inputs.json`、硬編 JDK。只解壓本包不能當成正式 `--check`／`--smoke`。
- Maven 產物 **不是** 正式 0.6.71 candidate JAR（`docs/server-source-gap.md`）。
- 不啟停資料庫、不啟動遊戲、不複製客戶端資源。不改正式啟動腳本、機密設定、封存或 `releases/`。
