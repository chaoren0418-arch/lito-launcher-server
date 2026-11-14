# Issue #458 — T157 原始 checkout 同步最新 main

## 已驗證

2026-11-14 完成原始 checkout 的 fast-forward；先前因未追蹤檔案／lease 停止的報告由本次結果取代。依本輪明確裁定處理 9 個未追蹤檔案，本張不使用 P24Lab，也不以 lease API 的列舉結果阻擋。

### 做了什麼與前後 HEAD

- 原始 checkout：`D:\lineage開發\3-lito開發`，前後皆為 `main`。
- 同步前 HEAD：`6bb4a27bb6cfbef1639d155700b09c6e1bfbf34f`。
- 執行 `git fetch origin` 後，以 `git merge-base --is-ancestor` 確認可快轉，再執行 `git merge --ff-only origin/main`，exit code 0。
- 同步後 HEAD／本次取得的 `origin/main`：`b972e23df7a6ad787c7cf9e3b12e641806df3df1`。
- 任務 worktree 維持 `task/458`，未 checkout、未新建分支；`git merge --ff-only origin/main` 回覆 Already up to date。
- 初始 `git status --porcelain --untracked-files=all` 無已追蹤檔案變更，只有下表 9 個 `??` 檔案，沒有額外未追蹤檔案。
- 先核對更新範圍既有路徑，未發現 junction、symlink 或多重硬連結；未遞迴刪除、未移除 worktree。

### 備份與未追蹤檔處置

備份根目錄：`D:\lineage開發\3-lito開發\scratch\t157-local-agent-backup`。9 個檔案均先複製到此根目錄下的 `tools/local-agent/...`，逐檔核對 SHA-256。3 個內容不同的原檔再移至 `originals/tools/local-agent/...`，不覆寫第一次備份，移動後再次核對 SHA。

| 相對原始 repo 路徑 | 原始 bytes | 原始／備份 SHA-256 | 處置 |
|---|---:|---|---|
| `tools/local-agent/README.md` | 2797 | `AD4412A5F9B93F813027C46CE363B6229DEFFF612F0F918A6063E9AD9DE6DFC3` | 備份核對後移除未追蹤原檔 |
| `tools/local-agent/local-agent.ps1` | 6328 | `D2F809CF59D42F4505A662723E56B2D78BC6F266104A56C15AE3673436572084` | 原檔移至 originals/ 下相對路徑 |
| `tools/local-agent/local-agent2.ps1` | 6842 | `616E973D4D207748B8B2BDD617774F3F97223BBC740E2CD77D27FD8905419814` | 保留原地 |
| `tools/local-agent/logs/tw-bin-engine-2.comment.md` | 461 | `40819CFD3DFF177298FA7096FC82DEACB837D11DDB6D01FEE04E17A1912A8BB2` | 保留原地 |
| `tools/local-agent/logs/tw-bin-engine-2.prompt.md` | 1717 | `FD2BA4E91547681098DF0609F4EA8499C1E8DF09FB896DF0FBBCF04E10C2B861` | 保留原地 |
| `tools/local-agent/setup-labels.cmd` | 408 | `EDD1DF252AD49CB5AC702F74287B9D582B270A6C621604DB4FD9FDB5A54283FF` | 原檔移至 originals/ 下相對路徑 |
| `tools/local-agent/setup-labels2.cmd` | 391 | `D5A1561C7D623B941B694324DD93CC7F72DC21A80CB227B4084B4A8C4429A9AD` | 保留原地 |
| `tools/local-agent/start-local-agent.cmd` | 124 | `09D8B91B0B79113181BA2AE94CB0CC28D6E3DB443D504FC85408ADCC021965D1` | 原檔移至 originals/ 下相對路徑 |
| `tools/local-agent/start-local-agent2.cmd` | 124 | `09D8B91B0B79113181BA2AE94CB0CC28D6E3DB443D504FC85408ADCC021965D1` | 保留原地 |

以 `git show origin/main:tools/local-agent/<檔名>` 的原始 bytes 計算 SHA，未經 PowerShell 文字重編碼：

| 路徑 | origin/main bytes | origin/main SHA-256 | 比較 |
|---|---:|---|---|
| `tools/local-agent/README.md` | 2797 | `AD4412A5F9B93F813027C46CE363B6229DEFFF612F0F918A6063E9AD9DE6DFC3` | 相同 |
| `tools/local-agent/local-agent.ps1` | 6842 | `616E973D4D207748B8B2BDD617774F3F97223BBC740E2CD77D27FD8905419814` | 不同 |
| `tools/local-agent/setup-labels.cmd` | 391 | `D5A1561C7D623B941B694324DD93CC7F72DC21A80CB227B4084B4A8C4429A9AD` | 不同 |
| `tools/local-agent/start-local-agent.cmd` | 195 | `8FC0C27C5073F9FB5D49A43249C4CEBA11677483323F4C1BB78546DF26BBBE71` | 不同 |

差異摘要：

- `README.md`：bytes 與 SHA 完全一致，備份核對後移除原本的未追蹤檔，再由 Git 放入追蹤版本。
- `local-agent.ps1`：遠端改用 `${n}` 插值、調整 Codex 呼叫參數與 stdin 提示詞輸入、增加 last-message 檔案擷取／摘要選擇，並替 `--json` 參數加引號。
- `setup-labels.cmd`：原始 bytes／SHA 不同，但分別以 UTF-8／CP950 解碼後的逐行文字相同，屬編碼差異。
- `start-local-agent.cmd`：遠端由單次執行加 pause 改為 `-Once` 輪詢、60 秒間隔與 `chcp 950`。
- 保留的 5 個檔案在同步後 SHA 均與備份一致。兩個 logs 仍存在，只因新版 `tools/local-agent/.gitignore:1` 的 `logs/` 規則而不再出現在一般 status；其餘 3 個仍為未追蹤。

### helper SHA 核對

路徑：`lito登入器/LiTo381-Launcher/app/world_resource_preflight.py`（核對的是原始 checkout 絕對路徑中的實體檔案）。

| 階段 | bytes | SHA-256 |
|---|---:|---|
| 同步前 | 3705 | `353C362EC12D3443C790D707FBC26B50944081BC2D1DA9967A75D471CB289F34` |
| 同步後 | 11044 | `D8AB4990D1AA40AC366B47DC9CE90D96351B2ED518B56BBE18BE3BE0C4B83EEA` |

同步後內容與 `git show origin/main:<路徑>` 完全一致，符合任務要求的 **11,044 bytes、D8AB4990…**。

### fast-forward 檔案清單摘要

`git diff --name-status <同步前 HEAD> <同步後 HEAD>` 共 **3,048 筆檔案變更**：新增 2,429、修改 615、刪除 3、重新命名 1（重新命名以一筆計算）。Git 更新進度顯示 3,049 個路徑，包含重新命名的兩端。

| 頂層路徑 | 變更筆數 |
|---|---:|
| `.cursor` | 1 |
| `.gitattributes` | 1 |
| `.github` | 5 |
| `.gitignore` | 1 |
| `AGENTS.md` | 1 |
| `CLAUDE.md` | 1 |
| `PLAN.md` | 1 |
| `TASKS.md` | 1 |
| `docs` | 196 |
| `lito伺服器` | 2781 |
| `lito登入器` | 20 |
| `packaging` | 5 |
| `tools` | 34 |

完整清單可用上述兩個固定 HEAD 重建；本機取證檔為任務 worktree 的 `scratch/t157-changed-files.txt`。`docs/REPORT.md` 僅隨原始 checkout 快轉取得上游版本，本任務沒有編輯或提交其索引。

同步後原始 checkout 的 status：

```text
?? tools/local-agent/local-agent2.ps1
?? tools/local-agent/setup-labels2.cmd
?? tools/local-agent/start-local-agent2.cmd
```

已追蹤檔案的 index／worktree 均乾淨。未執行 helper、登入器、遊戲、伺服器、本機測試或建置產物；未連 DB、未啟停正式程序、未變更客戶端／封存／機密設定。

## 推測

絕對路徑 helper 讀到舊版本的前置原因已排除；這只能證明檔案版本一致，不能據此宣稱 #454 的 08 已通過。

## 待驗證

- #454 的 08 尚未重跑；由調度器交回後續驗收流程，遵守本機程式執行限制。
- 本次報告提交的 GitHub Actions 結果於 push 後在既有 PR #460 核對；前輪 CI 結果不當作本次結果。

## 產出路徑與給下一棒的提示

- 版本化產出：`docs/reports/issue-458.md`；PR：<https://github.com/chaoren0418-arch/lito-launcher-server/pull/460>。
- 非版本化備份：`D:\lineage開發\3-lito開發\scratch\t157-local-agent-backup`；不得把 local-agent logs／提示詞備份加入 Git。
- Claude 審查本報告與 PR；調度器可解除本張先前的未追蹤檔阻擋，接續安排 #454 的 08。此任務已按明確處置規則完成，不再需要等待未追蹤檔案或 lease 裁定。
