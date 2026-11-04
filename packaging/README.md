# 打包腳本（T040）

從 repo 根目錄組出廠商安裝包：`release/package-<version>/` 與 `release/package-<version>.zip`。

## 前置

| 工具 | 解析方式 |
|---|---|
| JDK 17 `java` | PATH（或 `JAVA_HOME` 已由呼叫端設定）；不寫死 `C:\Program Files\...` |
| Maven 3.9 `mvn` / `mvn.cmd` | PATH |
| `dotnet` | PATH；本腳本**不重建**登入器，只在 `release/current/` 存在時收入 |

路徑全部相對 repo 根（`packaging/Build-Package.ps1` 的上一層）。請在 repo 根執行。

## 執行

Windows PowerShell 5.1：

```powershell
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File packaging\Build-Package.ps1
```

PowerShell 7（含 Linux CI 的 `pwsh`）：

```powershell
pwsh -NoProfile -File packaging/Build-Package.ps1
```

本腳本會執行 `mvn -B -DskipTests -f lito伺服器/LiTo381-Server/pom.xml package`。若只要重打包既有 `target/` 產物，可加 `-SkipMaven`（驗收仍應跑完整一次）。

## 產出

```
release/package-<pom.version>/
  INSTALL.md
  MANIFEST.txt
  server/
    LiTo381-Server.jar
    lib/                          # Maven copy-dependencies，JAR Class-Path 需要
    db/*.sql                      # 僅 db 根層，不含 db/candidates/
    config/server.properties.example
    1.啟動伺服器.bat
    Start-LiTo381-Server.bat
  launcher/                       # 僅當 lito登入器/LiTo381-Launcher/release/current/ 存在
release/package-<pom.version>.zip
```

`release/` 與 `*.zip` 不進 git。

## 登入器缺檔

T003（Issue #4）已完成，但本雲端／CI 環境通常沒有 `release/current/`。腳本會：

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

打包後會再掃描 zip 條目；命中即失敗。

## 限制

- Maven 產物 **不是** 正式 0.6.71 candidate JAR（來源缺口見 `docs/server-source-gap.md`）。`--check`／`--smoke` 目前仍依本機 `Start-LiToServer.ps1`（正式 JAR、`deployment/release-inputs.json`、真實 `server.properties`）。
- 收入的批次檔保留 repo 相對路徑，解壓後不能單獨當成完整啟動器。啟動步驟見套件內／`docs/INSTALL.md`。
- 不啟停資料庫、不啟動遊戲、不複製客戶端資源。
