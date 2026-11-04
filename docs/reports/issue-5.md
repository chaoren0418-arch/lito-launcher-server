# Issue #5 / T004 — 文件版本號統一 0.6.71、VALIDATION 瘦身

## 做了什麼

只改 T004 列出的入口文件，把「正式 0.6.69／0.6.70」殘句改成現行 **0.6.71**。`VALIDATION.md` 只留 2026-10-23 最新一節與仍有效的驗收限制；舊段落原文搬到 `lito伺服器/LiTo381-Server/docs/HISTORY.md`，未改寫歷史內容。

| 檔案 | 變更 |
|---|---|
| `lito登入器/README.md` | 發佈版本與桌面捷徑名稱 0.6.70 → 0.6.71 |
| `lito登入器/LiTo381-Launcher/README.zh-TW.md` | 刪「沿用 0.6.70」殘句（本頁其餘已是 0.6.71） |
| `lito伺服器/README.md` | 桌面正式啟動器 0.6.69 → 0.6.71 |
| `lito伺服器/LiTo381-Server/README.zh-TW.md` | 「0.6.70 正式 JAR」「正式 0.6.69」→ 0.6.71 |
| `lito伺服器/LiTo381-Server/VALIDATION.md` | 只留 0.6.71 最新一節與既有驗收限制；刪「沿用 0.6.70 核心」與 2026-10-20／已發布 0.6.69／候選表 |
| `lito伺服器/LiTo381-Server/docs/HISTORY.md` | 追加 VALIDATION 舊段落原文（2026-10-20 候選界線、正式 0.6.69、已發布、候選表） |

未改：`releases/`、封存、`config/server.properties`、pak／map／sprite、`LiTo.bin`、正式 JAR、`PLAN.md`／`TASKS.md`／`AGENTS.md` 等範圍外文件。

repo 根目錄原本沒有 `VALIDATION.md` 與 `docs/HISTORY.md`；T004 簡寫對應既有的 `lito伺服器/LiTo381-Server/` 兩檔。

第 2 次嘗試：`main` 已合入 Issue #2（PR #7），與本分支的 `docs/REPORT.md` 發生 add/add 衝突。依調度器指示**不改** `docs/REPORT.md`，合併時保留 main 版（Issue #2 摘要）；交接寫在本檔。

## 怎麼驗證

**已驗證**

- 範圍六檔中，`grep -n "0\.6\.69\|0\.6\.70"` 只剩 `lito伺服器/LiTo381-Server/docs/HISTORY.md`。
- 四份 README 現行入口均寫 0.6.71。
- HISTORY 追加段與搬移前 VALIDATION 舊段落逐字相同（僅加搬移說明標題）。
- 未碰機密設定、封存目錄、硬連結。

**推測**

- 桌面捷徑實際顯示名稱以本機捷徑為準；文件已改，未核對使用者桌面。
- 全球 `grep -rn "0\.6\.69\|0\.6\.70" --include=*.md` 仍會打到範圍外的歷史維護稿、`PLAN.md`、`TASKS.md`、`AGENTS.md`、`docs/acceptance/CHECKLIST.md`。那些是當時紀錄或建置鏈事實（如 `base-0.6.70.jar`），不在 T004 範圍，改寫會違反「不要改寫歷史內容」。

## 未驗證什麼

- 未啟動遊戲、登入器、伺服器，未連正式 DB。
- 未改 `TASKS.md` T004 狀態（範圍未列）。
- 未核對桌面捷徑或 `release/current` 視窗標題。
- 第 1 次推送後 PR #11 CI 已綠；本輪只解與 `main` 的文件衝突，合併後 CI 會再跑一次。

## 產出路徑

- `lito登入器/README.md`
- `lito登入器/LiTo381-Launcher/README.zh-TW.md`
- `lito伺服器/README.md`
- `lito伺服器/LiTo381-Server/README.zh-TW.md`
- `lito伺服器/LiTo381-Server/VALIDATION.md`
- `lito伺服器/LiTo381-Server/docs/HISTORY.md`
- `docs/reports/issue-5.md`

（`docs/REPORT.md` 不再由本任務改；合併後由調度器加索引。）

## 給下一棒的提示

1. **使用者合併**：Claude 主控已審查通過（範圍 6 檔版本號統一、VALIDATION 舊段原文在 HISTORY）；閘門曾通過。本輪只把 `task/5` 跟上 `main`，`docs/REPORT.md` 維持 main／Issue #2 內容。
2. 合後把 `TASKS.md` T004 標 review／done。不要再手改 `docs/REPORT.md`。
3. 範圍外歷史稿、`PLAN.md`／`TASKS.md`／`AGENTS.md`、`docs/acceptance/CHECKLIST.md` 仍含 0.6.69／0.6.70，是當時紀錄或真實檔名（`base-0.6.70.jar`），不要當漏改。
4. 桌面捷徑名稱若仍寫舊版，由本機改捷徑，不在本 repo。
