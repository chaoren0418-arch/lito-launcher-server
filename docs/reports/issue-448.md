# Issue #448（T153）：登入器資源預檢不再釘選 `TW13081901.txt` 的 SHA

依使用者 2026-11-14 在 #447 的裁定 C：`TW13081901.txt` 是 sprite 動作／速率資料檔，可依既定規則修改，登入器忽略它的內容。不還原客戶端、不重產 catalog、不發新版。

## 結論

正式 helper `development_startup.py` 的 `check_source`（正式 standard 模式與 `--external-server-profile` 外部入口共用同一個函式）已把 `0-客戶端\TW13081901.txt` 從 SHA 釘選表 `PINS` 移出，改為只檢查「存在、是一般檔、可讀、路徑不含 junction／symlink」。其他 32 個交付檔、`LiTo.bin`、`litoddraw.dll`、`lito381-compat.ini` 的檢查完全沒放寬。

## 移除／修改的每一處（檔案:行號）

| 位置 | 改動 |
|---|---|
| `lito登入器/LiTo381-Launcher/src/development_startup.py:35`（舊） | **移除** `PINS` 中 `D:\lineage開發\0-客戶端\TW13081901.txt → 7EBE2B46…4008`（PINS 33 → 32 筆） |
| `lito登入器/LiTo381-Launcher/src/development_startup.py:53-56`（新） | 新增 `UNPINNED = (CLIENT/'TW13081901.txt',)` 與原因註解 |
| `lito登入器/LiTo381-Launcher/src/development_startup.py:60-76`（新） | 抽出 `no_reparse()`（原 `exact()` 內的 junction／symlink 檢查，行為不變）；新增 `readable()`：不存在／不是檔案 → `Startup file missing`，開檔讀 1 byte 失敗 → `Startup file unreadable` |
| `lito登入器/LiTo381-Launcher/src/development_startup.py:85`（新） | `check_source` 在 PINS 比對之後加 `for path in UNPINNED: readable(path)` |
| `lito登入器/LiTo381-Launcher/app/runtime-pins.json:18`（舊） | **移除** 同一筆 `TW13081901.txt` 釘選（這檔是 `tools/build_current.py` 由 PINS 產生的紀錄，執行期沒有程式讀它；保持一致） |
| `lito登入器/LiTo381-Launcher/src/DevelopmentStartup.cs:10` | `EntrySha` `ADFF1EDC…C9CD` → `56532A2FF06DFCF2C1569681D997E84410C8EA68CB4F65B4ED9BCA0D7CDA8189` |
| `lito登入器/LiTo381-Launcher/src/external_server_startup.py:27` | `FORMAL_HELPER_SHA` 同上更新 |
| `lito登入器/LiTo381-Launcher/src/ExternalServerStartup.cs:14` | `HelperSha` `7E9EE3D8…7C2F` → `BCB588542BDD3D9D1BD52806389F2A0ECD5F661347155F1FDC4CBC360D56F3DD` |
| `lito登入器/LiTo381-Launcher/tests/test_external_server_startup.py` | 原 #443 斷言「正式 helper 仍釘選 TW13081901.txt」改為斷言**不再釘選**且列在 `UNPINNED`；新增 `FormalCheckSourceTests`（見下） |
| `docs/acceptance/launcher-external-server.md` | 客戶端檔案表拆出 `TW13081901.txt` 列；新增「`TW13081901.txt` 不釘選（T153／Issue #448）」一節寫明規則、原因、影響；更新兩個 helper 的 SHA 與常見失敗訊息表 |

### 刻意沒改的地方（不是登入器預檢）

- `tools/world-resource-compat/rebuild_animation.py:17-50`：重產 `common_catalog.bin` 的工具，比對編碼器 TXT 與 manifest 的 `source_sha256`，是「重現已釘選 catalog」用的；裁定不重產 catalog，所以不動。
- `app/common_catalog_manifest.json`／`common_catalog.bin`：仍是 07-21 版（`7EBE…`）產物，**照舊在 PINS 裡釘選**。
- `server-patches/repair-032～034/build_launcher*.py:30`、`tools/caption-diagnostic/development_startup.py:38`、`tools/world-resource-compat/inputs/development_startup-0.6.69.py:38`：歷史版本的建置腳本／封存副本，不在目前建置或執行路徑上。
- `ClientResourceAudit.cs` 檢查的是 `TW13081901.bin`／`.pak`（舊版模式），不是 `.txt`，沒動。
- 伺服器端（`lito伺服器/`）沒有任何改動。

## 已驗證

- `dotnet build lito登入器/LiTo381-Launcher/src/LiTo381Launcher.csproj -c Release`：建置成功，0 警告 0 錯誤。輸出目錄的 `development_startup.py` SHA = `56532A2F…8189`、`external_server_startup.py` SHA = `BCB58854…F3DD`，與 C# 常數一致（`Get-FileHash`）。
- `python -m py_compile` 三個 .py（兩個 helper＋測試）通過。
- 換行：`development_startup.py` 維持 CRLF（`git ls-files --eol`：`i/crlf w/crlf`），`external_server_startup.py` 維持 LF＋`-text`，指紋在 CI checkout 後不會變。
- 依本機規定**沒有在本機執行單元測試**；改由 PR #450 的 CI（build run `43559743634`）驗證：
  - 「Launcher (.NET 10 / WinForms)」成功：`dotnet build`、`dotnet test` 通過；`test_external_server_startup.py` **Ran 30 tests … OK**（原 22 個，含改寫斷言後的 #443 原始碼測試＋新增 `FormalCheckSourceTests` 8 個）。
  - 「Server (Java 17 / Maven)」、「Server tests」（`mvn -B test`）、「Server reproducible JAR」、「Combat regression tools」、「Semantic gap」成功。
  - 「Server tests (CI MySQL fixture, non-blocking)」失敗；main 最新 build run `43553710184` 的同一個 job 也失敗，與本變更（只改登入器）無關。

### 新增單元測試 `FormalCheckSourceTests`（載入真正的 `development_startup.py`，把 PINS／CLIENT 等指到暫存目錄）

- `test_tw_txt_content_change_still_passes`：TW13081901.txt 換成三種不同內容（含空檔）→ `check_source` 通過。
- `test_other_pinned_file_change_still_refused`：`common_catalog.bin`、共用資源 JAR 內容變動 → 仍以 `Startup file changed` 拒絕，且不會走到 `world_resource_preflight.configure`。
- `test_lito_bin_change_still_refused`、`test_other_client_directory_still_refused`：LiTo.bin 指紋與客戶端目錄限制不變。
- `test_tw_txt_missing_is_refused`、`test_tw_txt_directory_is_refused`：檔案不存在或變成目錄仍拒絕。
- `test_real_pin_table_no_longer_contains_tw_txt_but_keeps_the_rest`：真實 PINS 不含 TW13081901.txt、共 32 筆、`common_catalog*`／`litoddraw.dll`／JAR／`world_resource_preflight.py` 都還在；`BIN_SHA` 不變。

## 推測

- 外部入口與以本分支 `dotnet build` 出來的登入器，在目前客戶端（`28C4…` 版 TXT）下應能通過 `check_source`，往下走到 `litoddraw.dll`／ini 核對與端點檢查。
- **已部署的 0.6.71 正式版（`release/current`）沒有更新**（裁定不發新版），仍是舊 helper（`ADFF1EDC…`），正式 standard 模式用那份 exe 啟動時仍會以 `Startup file changed: …TW13081901.txt` 拒絕。若要讓正式版也忽略，需另開任務發布（或依使用者另行裁定）。
- 新版 TXT 多出的 30 個 sprite 不在 LiTo 共用目錄（`common_catalog.bin` 30000 筆）裡；原生端以共用目錄為準，預期不影響登入→大廳。

## 待驗證

- 實機（交 Codex／使用者）：#414 項 08 重測——以本分支建置的登入器 `--external-server-profile` 啟動，確認不再出現 `Startup file changed: …TW13081901.txt`，並能進大廳。

## 產出路徑

- 程式：`lito登入器/LiTo381-Launcher/src/development_startup.py`、`src/external_server_startup.py`、`src/DevelopmentStartup.cs`、`src/ExternalServerStartup.cs`、`app/runtime-pins.json`
- 測試：`lito登入器/LiTo381-Launcher/tests/test_external_server_startup.py`
- 文件：`docs/acceptance/launcher-external-server.md`、本報告
- 分支：`task/448`

## 給下一棒的提示

- 主控：CI 綠燈合併後，開 #414 項 08 的重測任務（`runner: codex-local`、`stage: test`），步驟照 `docs/acceptance/launcher-external-server.md`，helper SHA 以文件第 1 步表格為準（`56532A2F…`／`BCB58854…`）。
- 以後再改 `development_startup.py`，要同步三個常數：`DevelopmentStartup.EntrySha`、`external_server_startup.FORMAL_HELPER_SHA`，以及因後者改變而變的 `ExternalServerStartup.HelperSha`。`tools/build_current.py` 會由 PINS 重寫釘選表，不會把 TW13081901.txt 加回去。
- 正式 0.6.71 `release/current` 仍釘選舊值；若使用者要正式 standard 模式也恢復，需要發布新版（目前裁定不發）。
