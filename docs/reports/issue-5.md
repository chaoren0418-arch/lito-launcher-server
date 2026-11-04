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
- HISTORY 搬移原文與搬移前 VALIDATION 舊段落逐字相同；補充索引獨立放在原文之前。
- 未碰機密設定、封存目錄、硬連結。

- 全域 `grep -rn "0\.6\.69\|0\.6\.70" --include=*.md` 仍會打到範圍外的歷史維護稿、`PLAN.md`、`TASKS.md`、`AGENTS.md`、`docs/acceptance/CHECKLIST.md`。那些是當時紀錄或建置鏈事實（如 `base-0.6.70.jar`），不在 T004 範圍，改寫會違反「不要改寫歷史內容」。本輪 `git grep -n -E '0\.6\.(69|70)' -- '*.md'` 實測共 42 行命中（含本報告的版本變更說明）；原始全域字面驗收未達，不宣稱通過。依前次審查接受的範圍與歷史／真實檔名例外保留。

## 第 3 次嘗試：證據導航修正與重驗

保留 worktree 已有的 HISTORY 索引修正：前 8 個入口改從 `maintenance/` 開啟，第 9 個從 `../tools/combat-continuation/continuation-current.json` 開啟。搬移說明明示原文內連結須由補充索引開啟；搬移原文完全保留。這次只提交 HISTORY 與本報告的差異，未修改 `docs/REPORT.md`。

在 worktree 根目錄以 PowerShell 執行以下目的檔檢查：

```powershell
$server = 'lito伺服器/LiTo381-Server'
$history = Get-Content -Raw -Encoding UTF8 "$server/docs/HISTORY.md"
$index = ($history -split '### 搬移段落的證據連結補充索引\r?\n', 2)[1] -split '### 搬移原文', 2
$links = [regex]::Matches($index[0], '\]\(([^)]+)\)')
if ($links.Count -ne 9) { throw 'Expected 9 links' }
foreach ($link in $links) {
  $target = Join-Path "$server/docs" $link.Groups[1].Value
  $exists = Test-Path -LiteralPath $target -PathType Leaf
  "$exists $target"
  if (!$exists) { throw "Missing $target" }
}
```

結果：**9/9 True**（8 份 maintenance Markdown 與 1 份候選索引 JSON 均存在）。另依原文出現順序，用 `[IO.Path]::GetFullPath` 比較原 `LiTo381-Server/` 與新 `LiTo381-Server/docs/` 基準解析的目的路徑，**9/9 相同**。

接續執行原文比對，直接讀搬移前 Git blob，避免 shell 重新編碼；只去除所擷取區塊末尾的換行分隔，不替換內文、空白或段內換行：

```powershell
$process = New-Object System.Diagnostics.Process
$process.StartInfo.FileName = 'git'
$process.StartInfo.Arguments = 'show a374081675039d866e070d2641845126dba57bb9:lito伺服器/LiTo381-Server/VALIDATION.md'
$process.StartInfo.UseShellExecute = $false
$process.StartInfo.RedirectStandardOutput = $true
$process.StartInfo.StandardOutputEncoding = New-Object System.Text.UTF8Encoding($false)
$null = $process.Start()
$original = $process.StandardOutput.ReadToEnd()
$process.WaitForExit()
if ($process.ExitCode -ne 0) { throw 'git show failed' }
$begin = $original.IndexOf('跨角色材料技能（')
$end = $original.IndexOf('## 保留的既有驗收限制')
$expected = $original.Substring($begin, $end - $begin).TrimEnd([char]13, [char]10)
$actual = $history.Substring($history.IndexOf('跨角色材料技能（')).TrimEnd([char]13, [char]10)
$identical = [string]::Equals($expected, $actual, [StringComparison]::Ordinal)
"Historical text identical: $identical"
if (!$identical) { throw 'Historical text changed' }
```

結果：**Historical text identical: True**，3,238 字元／5,934 UTF-8 bytes。

其他檢查：`git diff --check` 通過；`git diff --exit-code -- docs/REPORT.md` 無差異；`fsutil hardlink list` 對本輪兩檔均只列出各自在 worktree 的單一路徑。六份指定文件的舊版本搜尋只有 HISTORY 命中，四份 README 均含現行版本 0.6.71。

## 未驗證什麼

- 未啟動遊戲、登入器、伺服器，未連正式 DB。
- 未改 `TASKS.md` T004 狀態（範圍未列）。
- 未核對桌面捷徑或 `release/current` 視窗標題。
- 本輪未執行建置或實機測試；最新提交的 CI 狀態由 PR #11 查閱，前次 CI 綠不代表本輪已驗證。

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

1. **Claude 重新審查 PR #11**：前次 review:fail 指出的 9 個證據連結已補索引，目的檔與原文比對結果見上方。請針對本輪提交重新審查，通過後由使用者合併；不要沿用先前「已通過」狀態。
2. 合後把 `TASKS.md` T004 標 review／done。不要再手改 `docs/REPORT.md`。
3. 範圍外歷史稿、`PLAN.md`／`TASKS.md`／`AGENTS.md`、`docs/acceptance/CHECKLIST.md` 仍含 0.6.69／0.6.70，是當時紀錄或真實檔名（`base-0.6.70.jar`），不要當漏改。
4. 桌面捷徑名稱若仍寫舊版，由本機改捷徑，不在本 repo。
