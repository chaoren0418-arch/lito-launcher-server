# Issue #341 — T083 Maven／50635f7e 離線戰鬥回歸（重測）

## 結論

**結論：部分通過（✅ 7／❌ 5／⚠️ 2），T083／M2／B09 尚未驗收。**

2026-11-14 05:19–05:35（Asia/Taipei）於指定 worktree、`task/341` 執行本次測試及程序清理。沿用前次 01–04、13–14 六項通過結果，只重測 05–12；同步 main 後另重建本次對照 JAR。#407 的失敗收據修正已實證通過。兩顆 JAR 各完成六套入口呼叫，全部 exit 1，未修改產品／回歸工具／斷言來取得通過。

兩邊均通過 276 案替換測試，這份 observations 的所有欄位一致。另有 13 份既有回歸 observations 一致。Maven 被兩道歷史 class 位元組檢查阻擋；候選也有真實斷言失敗。因此**不能宣稱 7,588 案逐案相同，也不能將守門失敗視為 Java 行為退化**。

## 已驗證

### 版本與輸入

已讀根目錄 `AGENTS.md`、`docs/REPORT.md`、伺服器規範、T083、#398／#407 交接及 #284 啟停腳本。沒有 checkout／建分支，未手動修改 `docs/REPORT.md`。

- `git fetch origin`、`git merge origin/main` 無衝突；main：`ebb82b512dd94f140e5ff520c2a4b8e57032ca67`；測試 merge commit：`cb34c29616ef2ea075abaaed9c01b2753ffab993`。
- `mvn -B -DskipTests package`：exit 0、BUILD SUCCESS、02:05；Maven 單元測試未執行。

| 輸入 | SHA-256 |
|---|---|
| Maven `target/LiTo381-Server.jar` | `84ba0cde7abace28456232b86922b22dd7fbff98e8b44e8b7bec5f995a79edcc` |
| 候選 `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| #83 原隔離設定（只讀，未修改） | `06d310113db2b8912b8e3d28cc8c83d98c78f6b5d12d4a69aae1d26b26bfc49a` |
| 本次 scratch 隔離設定 | `4693f0d1b41595b484b39decc3bcb2351083ddf9566a8c3a0686f8ac4dc7d00e` |
| #83 `combat.properties` 副本 | `e55834d386ec2fa473ada4731a7ebc2ff23bf66016c9de29f9c611e9fbbf3eaa` |

設定準備：#83 原設定的 `bind`／`game.bind` 都是 `127.0.0.1`，候選入口先被 `ServerConfig.load` 的 `bind and game.bind must differ` 拒絕。參照 #284，在本 worktree 的 `scratch/issue-341/retest/inputs/` 建立獨立副本，只改 `game.bind=127.0.0.2` 及 captures／logs 輸出路徑，並普通複製同目錄必需的 `combat.properties`。帳號、密碼、JDBC、schema 與其他戰鬥設定沿用；私密副本由 Git 忽略，不提交。第一次副本欠缺 sibling `combat.properties` 的失敗亦保留於 `retest/compatible/`，沒有拿那些前置失敗當作戰鬥結果。最終結果一律來自 `retest/results/`。

### P24Lab 與程序生命週期

- 有效 lease：holder `chaoren0418-arch/lito-launcher-server#341`，expiry `1794608385.8320994`（2026-11-13 22:19:45.832099Z）。開始／結束收據均在 `E/evidence/`；首次 API 曾回傳空 leases，稍後取得相符有效資料，未因此中止。
- 沿用 #284 的 `start-p24.ps1`／`stop-p24.ps1`，只將本次收據位置與 holder 改到 #341；使用既有 `mysql.ini`，未編輯 datadir／設定、未匯入資料、未執行 DML／DDL／GRANT。mysqld 正常啟停的內部持久化屬既有實例生命週期，未搬動或手工修改資料檔。
- 本輪啟動監督 PID **8600**、子程序 PID **17420**；43324 listener 屬後者，父子關係已保存。啟動包裝只比對監督 PID，曾誤報 timeout；實際 MySQL ready、listener 與 JDBC 身分均確認，不重複啟動。
- 實際 JDBC：`lito83_exp@127.0.0.1`、43324、UUID `e3443e40-d090-11f1-8040-4cedfb679789`、datadir `D:/BinEngine/environments/p24/db-data/`、bind `127.0.0.1`。只以 #83 帳號讀取 `*_lito_exp` 實驗資料；沒有連正式 2000／3306。
- `SHOW GRANTS` 顯示帳號並非純 SELECT：對實驗 `character_lito`、`lito_battle_settlement` 另有既存 INSERT／UPDATE。本輪未使用這些寫入權限；戰鬥 probe 使用既有 `MemoryAccountRepository`／記憶體 state，SQL 載入資源。未替換 probe、未捏造 fixture 或資料。
- 測試前補充 SELECT：`character_lito=2`、`lito_battle_settlement=0`。測後輔助 Java 指令因 PowerShell 未將 `-Dfile.encoding=UTF-8` 作為完整引數傳入而失敗，未取得後置筆數；**不宣稱前後資料逐值相同**。沒有資料修改作業，無還原 SQL；未為診斷查詢重新啟動已關閉的 DB。
- 21:34:31Z，既有管理腳本核實相同實例、無其他客戶連線後正常 SHUTDOWN，exit 0；PID 8600／17420 均退出，43324／42061／42083／42084 無 listener。本輪 Maven／Python／Java 子程序亦無殘留。

### 逐項結果

`E`＝本 worktree 的 `scratch/issue-341/retest/`，`R`＝`E/results/`；`{maven,candidate}` 表示兩份。A–F 命令見下節。沿用項不是再次執行相同戰鬥測試；表內補列本輪必要的新版本與清理證據。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ | 沿用建置通過；同步新 main 後 package 亦成功 | Server 目錄執行 Maven | `E/maven-package.log` |
| 02 | ✅ | 沿用輸入查核；本次新 Maven SHA 與候選 SHA 如上 | `Get-FileHash -Algorithm SHA256 <jar>` | `E/summary.json` |
| 03 | ✅ | 沿用設定靜態查核；本輪另確認指定帳號及實驗 DB 身分，權限細節如上 | #83 設定＋唯讀 JDBC 身分／grants 查詢 | `E/evidence/db-identity-before.log` |
| 04 | ✅ | 沿用鎖查核；本輪 lease holder／expiry 相符有效 | `/api/state` 取 leases | `E/evidence/lease.json`、`lease-final.json` |
| 05 | ❌ | targeted：兩邊 276 案通過；Maven 後續卡 GameStateStore 位元組檢查，候選 12 fault 案通過後卡 CallNature MP 斷言 | A | `R/{maven,candidate}-targeted.log`、各 `targeted/receipt.json` |
| 06 | ❌ | player：兩邊第一案皆缺少預期的一次機率計算紀錄 | B | `R/{maven,candidate}-player.log` |
| 07 | ❌ | composed：兩邊第一組 WaterLife 第一案皆資源斷言失敗 | C | `R/{maven,candidate}-composed.log` |
| 08 | ❌ | 20＋18：Maven 前 3 組過、第 4 組 class guard 失敗；候選前 6 組過、第 7 組事件順序斷言失敗；兩邊 affected 18 組均未進入 | D | `R/{maven,candidate}/regression/qualification-runs/*/manifest.json`、各 driver log |
| 09 | ❌ | curse-clock：兩邊第一案皆 `actual first socket write delayed` 斷言失敗，無 24 案完成收據 | E | `R/{maven,candidate}/curse-clock-boundary/probe.log`、`receipt.json` |
| 10 | ⚠️ | seal 如實拒絕 targeted failed，未給出資格 | F | `R/{maven,candidate}-seal.log`、各 `qualification-current.json` |
| 11 | ✅ | #407 實際失敗路徑收據皆寫成 failed，無重複 keyword TypeError；reconciliation 明確列出失敗 driver／log | A／B／D 的失敗收尾 | 各 `targeted/receipt.json`、`player-cancellation/receipt.json`、`*-existing-affected.log` |
| 12 | ⚠️ | 7,588 案只有 276 案取得雙邊完整 observations；尚不足逐案驗收，差異／未覆蓋見下 | 配對本次 observations，逐欄比較 JSON | `R/comparison.json`、本報告 |
| 13 | ✅ | 沿用正式檔保護；本次全部六套 guard 亦 passed、formal_deployment_changed=false | 核對 suite receipts before／after | `E/summary.json`、各 receipt／manifest |
| 14 | ✅ | 沿用程序清理查核；本輪自啟 P24Lab 已正常關閉，無本輪子程序或實驗監聽殘留 | 既有 stop 腳本核實 PID／身分、SHUTDOWN 後檢查 | `E/evidence/shutdown.json`、`remaining-processes.json` |

### 入口對照

| 入口 | Maven exit／秒 | 候選 exit／秒 | 結果關係 |
|---|---:|---:|---|
| targeted | 1／83.53 | 1／98.25 | 中斷位置不同：class guard／CallNature 斷言 |
| player | 1／4.99 | 1／4.92 | 同一機率紀錄斷言失敗 |
| composed | 1／3.91 | 1／4.33 | 同一資源斷言失敗 |
| existing-affected | 1／126.05 | 1／248.06 | 中斷位置不同：第 4／7 組 |
| curse-clock | 1／4.97 | 1／4.95 | 同一 socket 延遲斷言失敗 |
| seal | 1／0.44 | 1／0.44 | 均未取得 seal |

### 7,588 案覆蓋與比較限制

| 集合 | 要求數 | Maven 完整 observations 案數 | 候選完整 observations 案數 | 雙邊配對彙總一致 | 未取得雙邊完整 observations |
|---|---:|---:|---:|---:|---:|
| targeted＋player | 2,400 | 276 | 288 | 276 | 2,124 |
| composed | 5,164 | 0 | 0 | 0 | 5,164 |
| curse-clock | 24 | 0 | 0 | 0 | 24 |
| 合計 | 7,588 | 276 | 288 | 276 | 7,312 |

276 案的 assertions 全部通過、彙總 JSON 完全一致：checks=7,605、actual_frames=3,476、Source_window_bypasses=0、locked_write_calls=0、production_PC_overlays=0。工具沒有輸出這 276 案每案完整的狀態／封包 trace，故這裡的「一致」只指既有斷言結果與彙總欄位，不擴張成逐案所有可觀察行為等價。

候選另完成 12 個實際 memory fault 案：checks=549、actual_memory_faults=12、failed_material_casts_with_partial_commit=0、locked_write_calls=0；Maven 在執行這些案例前被 guard 阻擋。沒有放寬 guard 或使用候選 overlay 冒充 Maven。

20＋18 的額外資格測試不併入 7,588：兩邊前 3 組合計 **13 份 observations／7,470 案**所有 JSON 欄位完全相同（含有輸出的 vector hashes）；候選第 4–6 組另通過，Maven 未進入。完整配對索引在 `R/comparison.json`：14 份 aggregate-equal、13 份 unpaired、0 份 aggregate-different。這 13 份 unpaired 包含第 7 組失敗前的局部輸出，不能全部當作該組通過。

### 差異、首個失敗案例與 B09／#138 對應

| 識別 | Maven | 候選 | 對應／判定 |
|---|---|---|---|
| D1：12 案 CancellationBattleFault | 未執行，`test_boundaries.py` 要求 `GameStateStore.class` 與歷史 base 完全相同，assert 失敗 | 12 案通過 | **工具相容性 guard 差異**，不是 12 個已證實行為差異；GameStateStore 非 B09 ClientSession 方法，也未列 #138 此輪 6 類核准偏離 |
| D2：existing 第 4 組 corpse overlay | 第一個不在允許變更清單的 class：`NativeDeCorpseClock$1.class`；尚未執行 corpse probe | 該組通過 | `replay-native-de-item-state-components.py` 的歷史位元組 allowlist；非 B09 方法，也非 #138 該輪偏離，不能自行豁免 |
| F1：CallNature，flag/world/app=false，skill=165，mode=0 | 因 D1 未進入 | `resurrection Source MP actual=1000 expectedCost=50 mode=0 skill=165 world=false` | 待查路徑 `ClientSession.nativeMaterialCallNatureBody`（B09 清單），只有候選結果，不是已證實 Maven 差異 |
| F2：CancellationProbability，flag/world/app=false，skill=44，mode=0 | 失敗 | 同斷言失敗 | `one actual probability evaluation after material admission`；待查 `nativeMaterialCancellationBody`（B09）。同一失敗結果，根因尚未證明 |
| F3：WaterLifeMaterial，flag/world/app=false，mode=0（技能 170，自身目標） | 失敗 | 同斷言失敗 | `Source resources and no HP change`；待查 `nativeMaterialWaterLifeBody`（B09） |
| F4：CurseClockBoundary，flag/world/app=false，skill=33，mode=0 | 失敗 | 同斷言失敗 | `actual first socket write delayed`；尚未到刻意延遲 250ms 的 target lock 邊界。待查 `nativeMaterialParalysisScopeBody`（B09），不是完成 24 案 |
| F5：existing 第 7 組 `NativeDeActorStateRegression` normal | 因 D2 未進入 | `child encounter value event is queued before HP`（line 138）；前有 `accepted PC packet failed, HP remains committed` | fixture 事件順序斷言；堆疊未定位至單一 B09 production 方法，**尚無已證實對應**，不能套用 #138 偏離 |

上述 B09 方法是查核入口的對應，並非由覆蓋工具證明已進入該方法。約 113 個例外／迴圈方法未取得逐方法、逐 handler 的覆蓋證據。#138 偏離清單已核對，沒有將共同失敗或 class hash 差異自動視為已核准的行為改變。

class guard 的實測 SHA-256：

| class | 歷史／候選 | Maven |
|---|---|---|
| GameStateStore | `5dea2cee44c2bd82c017b48bbad104e2dbc6307422169241a58eed38d335b633` | `17e501ceeaa6662fd6f74ec6a842c87bc056dce9ef2b821c4639f180a6154f99` |
| NativeDeCorpseClock$1 | `6bb3feac18538b03fc6cf778acc32d78a43b54c5eb4818de7d83b33e77f0c5d0`（歷史 fixture） | `a25fb98be264eeabc785b55576cce717b6ab68844b145f2fb056554810a191ac` |

GameStateStore 歷史 base JAR 為 `f8e1ac47131db493c74451e005118005345ff61d8a55847e640fd80668c6a366`；該 class 與候選一致。差異定位只讀取既有 ZIP／manifest，未重新執行或修改測試。

## 實際命令

先同步 main，在 `lito伺服器/LiTo381-Server` 執行 `mvn -B -DskipTests package`。啟停使用本次 scratch 的 #284 腳本副本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scratch/issue-341/retest/start-p24.ps1
# 以下所有回歸完成後：
powershell -NoProfile -ExecutionPolicy Bypass -File scratch/issue-341/retest/stop-p24.ps1
```

本輪 regression argv、開始 UTC、PID、exit、耗時、log SHA 均在 `R/{candidate,maven}-execution.json`。最終兩顆依序執行，等價 CLI 如下；每個入口不論成功／失敗都繼續下一個，單入口外層上限 590 秒，本輪未觸發 timeout：

```powershell
$drivers = 'lito伺服器/LiTo381-Server/tools/combat-continuation'
$resources = 'D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server'
$config = 'scratch/issue-341/retest/inputs/isolated-test.properties'
$env:PYTHONDONTWRITEBYTECODE = '1'
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
foreach ($label in @('candidate', 'maven')) {
    $jar = if ($label -eq 'candidate') {
        'lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar'
    } else { 'lito伺服器/LiTo381-Server/target/LiTo381-Server.jar' }
    $argsForJar = @('--jar', $jar, '--out', "scratch/issue-341/retest/results/$label",
        '--resources', $resources, '--config', $config)
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

重測時需用全新輸出目錄，不能覆寫本輪收據；也不能只帶 server 設定而漏掉 sibling `combat.properties`。未啟動完整遊戲／登入器／正式伺服器，本輪是既有離線 fixture 的戰鬥回歸。

## 推測

- Maven 的兩道 byte-identical guard 可能不適合重新編譯產物；目前只證明 class bytes 不同，尚未證明這兩個 class 的語意相同或不同。
- 候選也在共同技能案例失敗，可能涉及現行 P24 資源／設定／fixture 與歷史資格條件的差異；不能直接歸因於 B09，也不能推定修好前置就全部通過。

## 待驗證與給下一棒的提示

1. **Claude／工具維護者**：先裁定 D1、D2 的 Maven 相容驗證方式，保留真正的故障注入與候選 class 身分守門；不要直接刪 assert 或借用歷史通過收據。本輪不修工具。
2. **戰鬥／fixture 維護者**：按 F1–F5 分別定位，先確認候選在現行隔離資源下的可重現失敗；需要的 SQL 查詢限 `lito83_exp`／43324／實驗 schema。沒有因測試失敗擴權、補資料或改正式檔。
3. **本機重測執行器**：只重測 05–10、12 的未通過部分。276 案雙邊通過、前 3 組既有回歸及 #407 項 11 沿用本輪結果；未完成的 7,312 案、affected 18 組、完整逐案差異與 113 方法例外覆蓋仍待驗證。工具目前入口會遇錯中止；若要精確跳過已過子項，需工具維護任務提供合適入口，勿在驗收時自行繞過。
4. **調度器保全證據**：報告是唯一提交產出；JAR／私密設定／log／收據／一次性腳本留於 worktree 的 target／scratch，不進 git。清理 worktree 前保全 `scratch/issue-341/retest/`；私密設定不應附到公開 Issue。worktree／junction／封存均未刪除。

## 產出與 log 校驗

提交：`docs/reports/issue-341.md`。以下均為最終完整設定的實際新執行，不混用前置失敗或歷史成功資料。

| log（相對 R） | SHA-256 |
|---|---|
| `candidate-targeted.log` | `017c935ffe2583d55cfc3204805500893db982d7bb1bbc27c70db6c71256afb4` |
| `candidate-player.log` | `b6e6af922a0752919d6c717053d92e4866ff90865b821f1cff1eed4c83d0c08f` |
| `candidate-composed.log` | `1ec72845e2c6af0958df784df029f227f6633e15721ec8318c54ec74667e8609` |
| `candidate-existing-affected.log` | `9304ba579498e489544d96cb3f656c0ae9587cc923dc29d47d6834e50c4ad44b` |
| `candidate-curse-clock.log` | `b886459185067d608e516fe8022af8e814f2c878156f741a03f064a0ad7bccea` |
| `candidate-seal.log` | `876255739d7debb9f80a3d20a49c190fd141f60babc2452d2e53189dd8de6009` |
| `maven-targeted.log` | `5799aec02bd3d0e52f826a7566000f72bf3bd8a0a24990a27c9735a9ccbbc062` |
| `maven-player.log` | `02834369a88c7e8c47bb88a4b328e985774548bbda58da9e69d551e608c4f5a3` |
| `maven-composed.log` | `04c252134795ffb0050d9c668bf8e7701536bce34c62d0a147112f98ea159605` |
| `maven-existing-affected.log` | `d778adfffd11606555ae6461508b9f872e6855c50a5ba121792300439fa1fbce` |
| `maven-curse-clock.log` | `6ecc2109b33f7c840dee7f33520637b58d59017049ac332f87106aa449ecc119` |
| `maven-seal.log` | `876255739d7debb9f80a3d20a49c190fd141f60babc2452d2e53189dd8de6009` |
