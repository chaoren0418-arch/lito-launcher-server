# Issue #458／T157：原始 checkout 同步前檢查（阻擋）

## 結論

原始 checkout 位於 `main`，但有 9 個未追蹤檔案，觸發 Issue #458 第 2 步的停止條件。本輪未執行原始 checkout 的 fast-forward，未 stash、覆寫或清理檔案；已將 Issue #458 加上 `needs:claude`。此報告完成的是阻擋取證，不代表 T157 同步成功。

## 已驗證

- 檢查日期：2026-11-14（UTC 07:11–07:13）。
- 任務 worktree：`D:\lineage開發\3-lito開發\scratch\wt\task-458`，分支 `task/458`，起始 HEAD 為 `b972e23df7a6ad787c7cf9e3b12e641806df3df1`，起始狀態乾淨；未 checkout 或建立分支。
- 已讀取 worktree 的 `AGENTS.md`、`docs/REPORT.md`，以及原始 checkout 的 `AGENTS.md`；未修改規範或報告索引。
- `http://localhost:8787/api/state` 的 `leases` 確認 `p24lab` 持有人為 `chaoren0418-arch/lito-launcher-server#458`，到期值 `1794645651.5058854`，換算 UTC 為 `2026-11-14T08:40:51.506Z`，與任務所列時間在秒精度一致，檢查時有效。

### 原始 checkout 前後狀態

目錄：`D:\lineage開發\3-lito開發`。

| 項目 | 檢查結果 |
|---|---|
| 分支 | `main` |
| 操作前 HEAD | `6bb4a27bb6cfbef1639d155700b09c6e1bfbf34f` |
| 停止後 HEAD | `6bb4a27bb6cfbef1639d155700b09c6e1bfbf34f`（未變動） |
| `git status --porcelain` | `?? tools/` |
| 已追蹤檔案的未提交變更 | 本次 status 未列出 |
| 未追蹤檔案 | 9 個，完整清單如下 |
| 本次 fast-forward 改動檔案數 | 0（未執行 fast-forward） |

使用 `git -c core.quotepath=false status --porcelain --untracked-files=all` 展開的清單：

```text
?? tools/local-agent/README.md
?? tools/local-agent/local-agent.ps1
?? tools/local-agent/local-agent2.ps1
?? tools/local-agent/logs/tw-bin-engine-2.comment.md
?? tools/local-agent/logs/tw-bin-engine-2.prompt.md
?? tools/local-agent/setup-labels.cmd
?? tools/local-agent/setup-labels2.cmd
?? tools/local-agent/start-local-agent.cmd
?? tools/local-agent/start-local-agent2.cmd
```

僅取得路徑清單，未修改上述檔案。原始 checkout 的 Git 唯讀命令使用 `GIT_OPTIONAL_LOCKS=0`，避免 status 更新索引。

### 預檢檔 SHA 核對

相對路徑：`lito登入器/LiTo381-Launcher/app/world_resource_preflight.py`。以 `Get-Item` 與 `Get-FileHash -Algorithm SHA256` 讀取實際檔案。

| 位置 | 大小（bytes） | SHA-256 | 核對 |
|---|---:|---|---|
| 原始 checkout（停止後仍相同） | 3,705 | `353C362EC12D3443C790D707FBC26B50944081BC2D1DA9967A75D471CB289F34` | 符合背景所述舊版，不符合目標 |
| 任務 worktree | 11,044 | `D8AB4990D1AA40AC366B47DC9CE90D96351B2ED518B56BBE18BE3BE0C4B83EEA` | 符合目標大小及 SHA 前綴 |

為提交本報告，在任務 worktree 執行 `git fetch origin`；隨後 `origin/main` 為 `b972e23df7a6ad787c7cf9e3b12e641806df3df1`，與報告修改前的任務分支相同（`git rev-list --left-right --count HEAD...origin/main` 為 `0 0`）。該 fetch 更新共用 Git 遠端資訊，沒有更新原始 checkout 的 `main` 或工作檔案。原始 checkout 未執行 fetch／merge 流程。

另以 Git 樹差異唯讀確認：目前 `origin/main` 已追蹤 `tools/local-agent/README.md`、`local-agent.ps1`、`setup-labels.cmd`、`start-local-agent.cmd` 四個同名路徑；未比較其內容，不能直接認定可覆寫。

## 推測

原始 checkout 的舊版預檢檔與任務描述的 helper 版本不一致相符；本輪只核對靜態檔案，沒有重現 #454 的 08，不能據此宣稱登入流程已修復。

## 待驗證／未完成

- 9 個未追蹤檔案的來源、保留方式，以及同名檔案是否有需保留的本機修改，待 Claude 裁定。
- 未執行原始 checkout 的 `git merge --ff-only origin/main`，故未驗證 fast-forward 可行性及同步後完整檔案差異。
- 原始預檢檔尚未達到 11,044 bytes／`D8AB4990…`。
- 本輪僅文件與唯讀取證，未建置、未執行測試程式，未啟動登入器／遊戲／伺服器，未連接 DB，也未操作客戶端、P24Lab、正式 2000／3306、封存或 junction。
- PR CI 由 GitHub Actions 執行；不能以文件檢查代替 #454 的 08 實機驗收。

## 產出與給下一棒的提示

- 唯一提交產出：`docs/reports/issue-458.md`；不修改 `docs/REPORT.md`。
- Claude 先處理 `needs:claude`：裁定上述未追蹤檔案的保留方式，並釐清本次指令中「原始目錄只讀」與「原始 checkout fast-forward」的寫入範圍。執行器不可自行 stash、刪除、覆寫或忽略這些檔案。
- 阻擋解除後，由本機執行器在有效資源鎖下重新檢查 main／乾淨狀態，再依任務執行 fetch → ff-only；記錄前後 HEAD、實際變更檔案數及完整 SHA。
- 同步與 SHA 核對成功後，再交原 #454 驗收流程重測 08；本報告不可當作該項通過證據。
