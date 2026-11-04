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

repo 根目錄原本沒有 `VALIDATION.md` 與 `docs/HISTORY.md`；T004 簡寫對應既有的 `lito伺服器/LiTo381-Server/` 兩檔。`docs/REPORT.md` 在 main 亦不存在，本輪新建。

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
- 未跑 GitHub Actions（本 PR 只動 markdown）。

## 產出路徑

- `lito登入器/README.md`
- `lito登入器/LiTo381-Launcher/README.zh-TW.md`
- `lito伺服器/README.md`
- `lito伺服器/LiTo381-Server/README.zh-TW.md`
- `lito伺服器/LiTo381-Server/VALIDATION.md`
- `lito伺服器/LiTo381-Server/docs/HISTORY.md`
- `docs/reports/issue-5.md`
- `docs/REPORT.md`

## 給下一棒的提示

1. **Claude 審查**：核對 HISTORY 舊段落是否與搬移前 VALIDATION 一致；PR 合後把 `TASKS.md` T004 標 review／done。
2. 若調度器要用全球 grep 當驗收：範圍外仍有約 17 個 `.md` 含 0.6.69／0.6.70（維護稿、PLAN／TASKS／AGENTS、CHECKLIST、`docs-AGENTS-history.md`）。那些不應在本任務改寫；另開任務再決定要不要做索引或加「歷史章節」標註。
3. `docs/acceptance/CHECKLIST.md` 的「0.6.70 驗過／0.6.69 驗過」是實機核對備註，不是正式版本殘句。
4. 使用者合併後，桌面捷徑名稱若仍寫 0.6.69／0.6.70，由本機改捷徑，不在本 repo。
