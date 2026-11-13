# Issue #341 — T083 Maven／50635f7e 離線戰鬥回歸

## 結論

**結論：部分通過（✅ 6／❌ 1／⚠️ 7）。Maven 建置通過；戰鬥回歸仍阻塞，完成的戰鬥案例為 0，不能驗收 M2／B09。**

2026-11-14 00:49–00:53（Asia/Taipei）在指定 worktree／`task/341` 執行。兩顆 JAR 各跑 targeted、player、composed、20＋18、curse-clock、seal，共 12 次入口呼叫，皆 exit 1。主要原因是 P24Lab `127.0.0.1:43324` 沒有監聽，JDBC 回報 `Connection refused: connect`；另實際發現 #31 targeted／player 收據程式的重複 keyword 錯誤。未修改工具或產品程式來繞過。

## 已驗證

### 版本、建置與輸入

- 已讀根目錄 `AGENTS.md`、`docs/REPORT.md`、伺服器規則、T083 與 #398 的入口交接；沒有 checkout／建立分支。
- `git fetch origin` 後合併 `origin/main`，無衝突。main 為 `c258cbb4148a4ccc773b4d0b73b6196e3050d4e2`；本輪建置所在 merge commit 為 `ca3b0638a3454be75733f974c99d2fd5161e0a24`，伺服器原始碼／工具與該 main 相同。
- 在 `lito伺服器/LiTo381-Server` 執行 `mvn -B -DskipTests package`，exit 0、`BUILD SUCCESS`、耗時 01:28；這次跳過 Maven 單元測試，不能將 package 當作戰鬥驗收。
- 直接讀取 #83 的隔離設定 `D:/lineage開發/3-lito開發/scratch/wt/task-83/scratch/t020c/isolated-test.properties`，僅核對必要連線欄位，未輸出密碼、未複製／修改設定。帳號 `lito83_exp`，JDBC `jdbc:mysql://127.0.0.1:43324/`，static／lito schema 分別為 `p24_world_lito_exp`、`p24_lito_lito_exp`。連線失敗，**本輪未驗證登入與實際 DB grants**。
- 最終 `2026-11-13T16:53:17.9552138Z` 的 `/api/state` 確認 `p24lab` holder 為本 Issue、expiry 為 `2026-11-13T17:49:34.269911Z`，與派工相符且未過期。最初查詢曾回傳空 leases，只記錄而未因此停止；不將鎖列為最終阻塞。
- 43324 沒有 listener；未啟動或搬動 P24Lab datadir。此 worktree 的寫入界線不包含原專案與外部 DB 資料目錄，依「跑不起來記錄原因、不改工具繞過」交回環境前置。未連正式 2000／3306、未執行資料寫入，因此無資料前後異動或還原 SQL。

| 輸入 | SHA-256 |
|---|---|
| Maven `lito伺服器/LiTo381-Server/target/LiTo381-Server.jar` | `0f3762ce8c8e94f3b91d5404cc03b2bf815b4e329d0cbdcbc20f24f6b8686205` |
| 候選 `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 現行正式 JAR／reference | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| #83 隔離設定（只記雜湊） | `06d310113db2b8912b8e3d28cc8c83d98c78f6b5d12d4a69aae1d26b26bfc49a` |

### 逐項結果

下表 `E` 為 worktree 內 `scratch/issue-341/current/`；`{maven,candidate}` 代表兩份都存在。步驟 A–F 見後面的實際命令。✅ 僅表示該列查核通過，不代表遊戲行為通過。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ | 最新 main 伺服器 package 成功 | 在 Server 執行 Maven 命令 | `scratch/issue-341/maven-package.log` |
| 02 | ✅ | 兩顆 JAR 已實測 SHA，候選符合 50635f7e | `Get-FileHash -Algorithm SHA256 <jar>` | `E/{maven,candidate}-execution.json` |
| 03 | ✅ | 隔離設定靜態欄位為指定帳號、43324 與實驗庫；認證未成功 | 唯讀核對設定必要鍵 | 本報告上列非秘密欄位、設定 SHA、實際命令 |
| 04 | ✅ | 最終 lease holder／expiry 相符 | `Invoke-RestMethod http://localhost:8787/api/state` 只取 leases | `E/environment.json` |
| 05 | ⚠️ | targeted 兩顆皆在第一個 CancellationBattleReplacementProbe 讀 SQL 失敗 | A，各跑一顆 JAR | `E/{maven,candidate}-targeted.log` |
| 06 | ⚠️ | player 兩顆皆在第一個 CancellationProbabilityProbe 讀 SQL 失敗 | B | `E/{maven,candidate}-player.log` |
| 07 | ⚠️ | composed 兩顆皆在第一個 WaterLifeMaterialProbe 讀 SQL 失敗 | C | `E/{maven,candidate}-composed.log` 與各 `composed-replay/*/WaterLifeMaterial/probe.log` |
| 08 | ⚠️ | existing 第 1／20 組 source replay 讀 SQL 失敗，affected 18 組未進入；reconcile 隨後 assertion 失敗 | D | `E/{maven,candidate}-existing-affected.log` 與各 `regression/qualification-runs/*/manifest.json` |
| 09 | ⚠️ | curse-clock 編譯後在 SQL 載入失敗，無 24 案 observations | E | `E/{maven,candidate}/curse-clock-boundary/{probe.log,receipt.json}` |
| 10 | ⚠️ | seal 拒絕尚未 passed 的 targeted 收據，未產生成功資格 | F | `E/{maven,candidate}-seal.log` 與各 `qualification-current.json` |
| 11 | ❌ | targeted／player 結尾寫收據出現重複 keyword TypeError，收據殘留 running | A／B 的失敗清理路徑 | 同 05／06 log；各 `targeted/receipt.json`、`player-cancellation/receipt.json` |
| 12 | ⚠️ | 無逐案 observations，7,588 案均無法比較，B09 方法／例外覆蓋未驗證 | 檢查新產物 `observations.json` 與收據 | `E/` 沒有 observations；下方對照表 |
| 13 | ✅ | composed、20＋18、curse-clock、seal 各自保存的 formal_protection 前後 SHA 一致 | 核對 before／after／reference 與 passed 狀態 | 各 suite manifest／receipt，尤以 `E/{maven,candidate}/curse-clock-boundary/receipt.json` |
| 14 | ✅ | 本輪啟動的 Maven／Python／Java 子程序均已結束，無殘留 task-341 程序 | 按 CommandLine／PID 查詢 Java、javac、Python | `E/environment.json` 的 `remaining_task_processes=[]` |

### 兩顆 JAR 的套件與逐案對照

| 套件 | Maven exit／耗時 | 候選 exit／耗時 | 比較結論 |
|---|---|---|---|
| `targeted` | 1／2.55 秒 | 1／3.67 秒 | 同類阻塞，不能判定行為相同 |
| `player` | 1／2.03 秒 | 1／2.31 秒 | 同類阻塞，不能判定行為相同 |
| `composed` | 1／0.89 秒 | 1／1.27 秒 | 同類阻塞，不能判定行為相同 |
| `existing-affected` | 1／18.8 秒 | 1／29.91 秒 | 同類阻塞，不能判定行為相同 |
| `curse-clock` | 1／1.78 秒 | 1／2.02 秒 | 同類阻塞，不能判定行為相同 |
| `seal` | 1／0.41 秒 | 1／1.0 秒 | 同類阻塞，不能判定行為相同 |

| 案例集合 | 預期案例數 | 兩邊皆有完整結果 | 已證實相同 | 已證實不同 | 無法判定 |
|---|---:|---:|---:|---:|---:|
| targeted＋player（27＋3 組） | 2,400 | 0 | 0 | 0 | 2,400 |
| composed（71 組） | 5,164 | 0 | 0 | 0 | 5,164 |
| curse-clock | 24 | 0 | 0 | 0 | 24 |
| 合計 | 7,588 | 0 | 0 | 0 | 7,588 |

20＋18 組另列為資格回歸，不將未知或重疊案例數加入上表。`0 個已證實不同` **不是**「兩顆相同」；兩邊都在資源載入階段失敗，沒有足夠資料列出逐案行為差異。沒有拿歷史通過收據當成本輪結果。

### 實際錯誤與 B09／deviations 對應

1. 五套行為入口各自最先執行的 probe（兩顆共 10 次）均出現：

   ```text
   com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
   Caused by: java.net.ConnectException: Connection refused: connect
   ```

   這是實驗 SQL 輸入未就緒，不能歸因到 `docs/combat-merge/b09-shape-diffs.md` 的任何方法，也不屬於 #138 核准偏離。尚未進入指定的戰鬥例外／迴圈案例。
2. 工具的獨立失敗：`cancellation-battle-runtime/replay_targeted.py:38` 與 `replay_player_cancellation.py:32`：

   ```text
   TypeError: dict() got multiple values for keyword argument 'formal_deployment_changed'
   ```

   `formal_protection` 已將該鍵寫進 `protection`，最終 `dict(formal_deployment_changed=False, **protection)` 又傳一次；兩套最初寫下的 `status=running, tests=[]` 收據無法被 finally 改成 failed。此為 Python 收據錯誤，不屬於 B09 Java 方法或 #138 deviations。未自行修正。
3. `candidate_regression.py:21` 的 reconciliation 僅接受失敗 driver 為 `replay-native-de-item-state.py`；實際第 1 組 `replay-native-de-item-state-source.py` 已失敗，所以再出現 `AssertionError`。原始 SQL 錯誤保存在內層 replay log；不能以後面的 assertion 取代根因。
4. seal 在 `seal_validation.py:31` 拒絕 `status=running` 的 targeted 收據，是未完成行為回歸的結果。其正式檔保護 passed 只證明檔案未變，不代表 seal／戰鬥通過。

## 實際命令

所有執行從本 worktree 進行。先 `git fetch origin`、`git merge origin/main`；Maven 工作目錄為 `lito伺服器/LiTo381-Server`：

```powershell
mvn -B -DskipTests package
```

以下等價於本輪 orchestration 的完整 CLI；實際每次 argv、PID、開始 UTC、exit、耗時與 log SHA 都在 `E/{maven,candidate}-execution.json`。每顆 JAR 的輸出根分開，未重跑已完成入口；整體未達 10 分鐘單步上限。

```powershell
$drivers = 'lito伺服器/LiTo381-Server/tools/combat-continuation'
$resources = 'D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server'
$config = 'D:/lineage開發/3-lito開發/scratch/wt/task-83/scratch/t020c/isolated-test.properties'
$env:PYTHONDONTWRITEBYTECODE = '1'
foreach ($label in @('candidate', 'maven')) {
    $jar = if ($label -eq 'candidate') {
        'lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar'
    } else {
        'lito伺服器/LiTo381-Server/target/LiTo381-Server.jar'
    }
    $argsForJar = @('--jar', $jar, '--out', "scratch/issue-341/current/$label", '--resources', $resources, '--config', $config)
    # A
    python -B -X utf8 "$drivers/cancellation-battle-runtime/replay_targeted.py" @argsForJar
    # B
    python -B -X utf8 "$drivers/cancellation-battle-runtime/replay_player_cancellation.py" @argsForJar
    # C
    python -B -X utf8 "$drivers/replay_composed_materials.py" cancellation-battle-runtime @argsForJar
    # D
    python -B -X utf8 "$drivers/candidate_regression.py" cancellation-battle-runtime @argsForJar
    # E
    python -B -X utf8 "$drivers/cancellation-battle-runtime/test_curse_clock_boundary.py" @argsForJar
    # F
    python -B -X utf8 "$drivers/cancellation-battle-runtime/seal_validation.py" @argsForJar
}
```

## 推測

- P24Lab 就緒後才能知道是否還有 schema、帳號授權、fixture 與 Maven／候選行為差異；不能推定修好連線就會通過。
- 收據重複鍵是獨立工具缺陷，靜態控制流顯示成功完成行為後也可能在 finally 觸發；本輪只實證了失敗路徑。

## 待驗證與給下一棒的提示

1. **本機環境維護／調度器**：交付正在監聽 43324 的 P24Lab 與有效 lease；本輪隔離設定已存在，不要再把「缺設定」列為阻塞。不得把正式 3306 作為替代。若需啟動既有 DB，需由允許寫入其 datadir 的環境任務處理，這張 worktree 任務沒有寫入原專案或其他目錄。
2. **回歸工具維護者**：只修本表 11 的兩個重複 keyword，保留所有行為斷言、正式保護與失敗收據；另評估 candidate reconciliation 的錯誤呈現，避免根因被掩蓋。本輪產品／工具沒有改動。
3. **本機重測執行器**：只重測 05–12 的 ❌／⚠️（兩顆 JAR、所有指定案例），使用全新輸出目錄，補逐案相同／不同、差異到 B09 或核准偏離的對照，以及約 113 個例外／迴圈方法的實際覆蓋；不能拿編譯成功或目前的環境失敗宣稱 B09 等價。若改用更新 main，必須另記新 JAR SHA。
4. **Claude 審查**：本報告只完成查核、實際失敗證據與交接，T083／M2 行為驗收仍未完成。沒有任何戰鬥案例被標為已通過；已通過的建置／輸入查核不應原樣重做。

## 產出與證據

提交產出只有本報告 `docs/reports/issue-341.md`；未手動修改 `docs/REPORT.md`（其合併更新來自 main）。建置 JAR、一次性 orchestration／報告生成腳本、原始 log 與工具收據留在 worktree 的 target／scratch，不進 git。未刪除任何目錄、worktree、junction 或原專案檔案。scratch 證據由調度器清理 worktree 前保全；關鍵錯誤、命令、SHA 與零案例結論已完整寫進本報告。

原始入口 log 均位於 `scratch/issue-341/current/`：

| log | SHA-256 |
|---|---|
| `maven-targeted.log` | `6d1c176b39395210c958471d7522274bf52b53eceebf63b374127e1b45224ba8` |
| `maven-player.log` | `756529459a2a047d3535a589535fe8a9b3867e36a53aa10a1c61b424229a2880` |
| `maven-composed.log` | `b3d86c35b9cc5005eeb73e0804d745dd106ae6e073c64d7d37810379d111719e` |
| `maven-existing-affected.log` | `6ff4323e94523953cfe3bccecf81a2e19d46e64be589cc02ef756456bbf15497` |
| `maven-curse-clock.log` | `b1e727bffde4f54b803a65d54238bab53a8e0e8dc5fc0db447ad05dbe7dfe8e1` |
| `maven-seal.log` | `876255739d7debb9f80a3d20a49c190fd141f60babc2452d2e53189dd8de6009` |
| `candidate-targeted.log` | `2f73a60ec6930e2004ae8cd209c96fbac63d3999fe0feb249e6e9d347f22f984` |
| `candidate-player.log` | `7379c2fad69df759c9421693b181ee954c5a6bffabaa9c8327c9347de6fc8b24` |
| `candidate-composed.log` | `ae7c68505161a5736d95317f1feb0760d92f0891c3b897a3acdfd73c4436364e` |
| `candidate-existing-affected.log` | `ce124188477a4c63d1d00144cdd084bdb237d238c393bf44672329d043d969f5` |
| `candidate-curse-clock.log` | `29bc2d121a7cc1aa2978e7d8b05827e951f324a923304beb8e13426691cabe1d` |
| `candidate-seal.log` | `876255739d7debb9f80a3d20a49c190fd141f60babc2452d2e53189dd8de6009` |
