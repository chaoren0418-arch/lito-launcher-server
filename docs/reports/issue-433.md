# Issue #433 — T148：08 獨立逐組與 10／12 直接對照

**結論：部分通過（✅ 3／❌ 2／⚠️ 1；下表六項為計數單位）。08 的 38 組均已雙邊獨立執行；12 完成 90/90 個入口雙邊嘗試，確認 9 案結果不同，另有 5,070 案未取得完整雙邊裁定。10 已直接執行，兩邊均未取得 seal 資格。**

## 已驗證

- 在既有 `task/433` 執行，未切換／建立分支、未刪除 worktree 或任何目錄。
- 建置基線為最新 `origin/main` `a753de2b151c1c9f824781228b194d715ac54694`。`mvn -B -DskipTests package` 成功（77 秒）；JDK 17.0.17.10、Maven 3.9.16；未執行 Maven 單元測試。
- Maven JAR SHA-256：`84ba0cde7abace28456232b86922b22dd7fbff98e8b44e8b7bec5f995a79edcc`，與 #429 相同。
- 候選 JAR SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。
- `p24lab` lease 核實屬於 `chaoren0418-arch/lito-launcher-server#433`，到期 `2026-11-14T02:15:17Z`。本張啟動 P24 MySQL parent PID 3424、listener PID 14852；只連 `127.0.0.1:43324`，UUID `e3443e40-d090-11f1-8040-4cedfb679789`、受限帳號 `lito83_exp@127.0.0.1`。
- `E` 代表本 worktree 的 `scratch/issue-433/`；`R` 代表 `E/results/`。原專案只提供唯讀 fixture；`E/resources/` 有 260 個 junction，禁止沿連結遞迴刪除。私密設定留在 ignored `E/inputs/`，不公開。

### 執行與判讀方式

08 的 20 個 existing、18 個 affected 入口均由 scratch 腳本獨立啟動，各 JAR 各一次，序列執行；單組失敗後繼續下一組。腳本取用原工具的單組迴圈內容及既有 `adapt_driver`，只選擇入口、隔離輸出與設定；產品、正式工具、Java fixture 與行為斷言均未修改。候選使用 `exact`，Maven 使用既有 `maven-normalized`。

12 沿用 #429 已完整配對的 904 案，補跑剩餘 6,476 案所屬入口。剩餘 player 112 案實為 boundaries 的 Lifecycle 56 案與 probability-lifecycle 56 案，分開執行；已通過的 player tests 176、Fault 8、curse-clock 24 不重跑。原工具收據保持單次執行的真實內容，未合成全套 passed 收據來滿足 seal。

「一致」依主控標準比較通過／失敗及首個失敗訊息，兩邊同樣失敗不等於功能通過。逐案表以實際案例目錄、原始斷言完成結果與 observations 案數核對；未取得結果不當成失敗，也不當成一致。彙總 observations 欄位另外比對，不宣稱所有內部狀態或 wire 逐 byte 等價。

### 逐項結果

✅ 以本張「兩邊結果一致」或指定作業完成判定，不表示失敗的功能已修復。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據 |
|---|---|---|---|---|
| 00 | ✅ | 最新 main 建置成功，SHA 與 #429 相同 | Server 執行 `mvn -B -DskipTests package` | `E/maven-package.log`、`E/evidence/jars.json` |
| 08 | ❌ | 38 組共 76 次；30 組一致、8 組不同 | `python -B -X utf8 E/run_groups.py` | `R/groups.json`、`R/all-group-results.tsv`、下表 |
| 10 | ✅ | 直接執行，雙邊 exit 1、同樣拒絕失敗總收據；結果一致，未取得資格 | 依實際單組結果彙整失敗收據後執行 `E/run_seal.py` | `R/seal.json`、`R/10-seal-{candidate,maven}.log` |
| 12a | ❌ | 9 案結果不同（8 案 golden、1 案 MP 斷言）；另有 8 個彙總觀測欄位不同 | `E/run_remaining.py` → `E/map_weakness.py` → `E/compare.py` | 下列三份差異 TSV、兩份逐案映射 JSON |
| 12b | ⚠️ | 5,070 案缺完整雙邊裁定，不能以首個失敗替整組案例下結論 | 同上；未改斷言使組內強行續跑 | [剩餘清單](issue-433/remaining-groups.tsv)、`R/all-case-results.tsv` |
| 14 | ✅ | 自有 P24 正常關閉、無遺留本張程序；正式 JAR 雜湊及 DB 核對檔前後相同 | ReadOnlyProbe 後執行 `E/stop-p24.ps1`，核對 PID／實驗埠 | `E/evidence/shutdown.json`、`final-verification.json` |

### 08：38 組、76 次均已執行

一致 **30 組**：雙邊通過 15 組、相同首因失敗 15 組。不同 **8 組**如下，沒有漏掉任何入口。完整各組兩邊結果與首因見 `R/all-group-results.tsv`；原始命令、PID、耗時及收據見 `R/groups.json`。

| 組別 | 候選首個結果 | Maven 首個結果 |
|---|---|---|
| existing 13 `probe-player-action-authority.py` | 歷史 parent probe：SQL `Unknown column 'id' in 'field list'` | `GameStateStore.class` 原始位元組相等斷言失敗 |
| existing 15 `replay-player-action-main-owned.py` | `actual Main reaches ready` 失敗；SQL 缺 `id` | `Main.class`／`SourceLogin.class` 原始位元組相等斷言失敗 |
| existing 16 `replay-player-action-transaction.py` | 通過 | `GameStateStore.class` 與 parent 位元組相等斷言失敗 |
| affected 04 `timed-weapon-probe-native-cast-material-preflight.py` | SQL 缺 `id` | `GameStateStore.class` 位元組相等斷言失敗 |
| affected 05 `timed-weapon-probe-native-cast-material-authority.py` | `preaccepted material commits once across STOPPED` | 同上 |
| affected 07 `timed-weapon-probe-native-cast-material-fault.py` | SQL 缺 `id` | 同上 |
| affected 12 `timed-weapon-probe-native-cube-material-fault.py` | SQL 缺 `id` | 同上 |
| affected 16 `timed-probe-native-item-weapon-fault.py` | SQL 缺 `id` | 同上 |

這 8 組中，7 組為兩邊失敗首因不同，1 組為候選通過／Maven 失敗。失敗訊息只正規化輸出根目錄、JAR 側別與時間戳，不消除斷言內容差異。部分入口的原工具仍有未接入 normalized 模式的 byte-equality 檢查；本張原樣記錄，未放寬守門。

獨立執行沒有消除入口內的依賴：existing 19／20 都因缺 `regression/replay-current.json` 而失敗，兩邊一致，但沒有取得其後行為證據。F5 所在第 7 組本輪雙邊通過；posture 第 11 組雙邊同樣失敗於 `actual C_HACTION preserves native posture world=false kind=ARENA action=aggressive`，不再缺候選結果。

### 12：已確認的逐案差異

`WeaknessWorldNpc` 兩邊 Java probe 都完成 64 案，候選通過整組；Maven 在最後的 golden 檔相等斷言失敗。凍結 Java 原碼 SHA `be523b2fde35a1ff2c40f0523141c7be923bbce27fbe9a9cd96398fbb82e9d71` 與原收據、目前 fixture 原碼相符。依原迴圈次序、16 行 golden 的 skill／mode 前綴與兩邊 64 個實際案例目錄交叉核對，定位 **8 案結果不同**：`{false,true}-true-{false,true}-47-{0,1}`。

這 8 案 Maven 的 self／peer 輸出多出 `590200103ff6ff`，候選沒有。完整原始行及案例 ID 見 [golden 差異表](issue-433/golden-differences.tsv)。另 8 行 golden 相同；其餘 48 案僅確認原 Java 斷言完成，沒有聲稱完整封包相等。映射依據與完整 64 案裁定存於 `R/weakness-golden-cases.json`，腳本為 `E/map_weakness.py`，未改 probe。

另有 **EarthBindWorldNpc 1 案不同**：`false-true-false-157-0`。候選完整 32 案通過，Maven 在第一案 `world NPC Source MP mode=0 skill=157` 失敗；log 同時記錄 Earth Bind 因 `required=1 elf-attr=0` 被拒。凍結原碼 SHA `85efe98dd71d381f26df947de2555754625828a5a33f4d98000d4cc008762468`、第一輪迴圈、唯一已建立的案例目錄與 exercise 堆疊互相吻合，故只裁定這一案失敗，其後 31 案保留未取得結果。證據：`R/earth-bind-first-failure.json` 與逐案差異表。

### 12：覆蓋與差異統計

- 本張母體 **7,380 案**：targeted 2,104＋剩餘 player 112＋composed 5,164。沿用 #429 的 904 案，其餘本輪直接獨立執行；兩邊各 90 次，共 180 次入口執行。不是前序套件全過才續跑。
- 90 個待補入口：首個結果相同 **72**、不同 **18**、未完成雙邊嘗試 **0**。這與逐案完成數分開計算。
- 完成雙邊逐案裁定 **2,310 案**（本輪 1,406＋沿用 904）：結果相同 **2,301**（通過 2,300、相同失敗 1），結果不同 **9**。
- 缺完整雙邊裁定 **5,070 案**：只有一邊取得可裁定結果 **103**，兩邊都缺完整逐案結果 **4,967**。均列入剩餘清單；不是這些案例全部失敗，也不是全部一致。
- 另有 **4 組、8 個** observations 欄位不同；保留實測值，未自行歸因為亂數、時序或已核准偏離。

[組別首因差異](issue-433/group-differences.tsv)（含 08／12）、[逐案結果與覆蓋差異](issue-433/case-differences.tsv)、[觀測欄位差異](issue-433/measurement-differences.tsv) 僅列不同項。完整 7,380 列保存在 `R/all-case-results.tsv`，相同結果不重複刊表。所有案例 ID 均由真實歷史案例目錄與本輪產物核對，沒有用流水號補造未映射案例。

### 10 與收尾

各入口執行完成後，`E/aggregate.py` 將真實失敗／未完成結果彙整為總指標，保留原單組收據及 `*.last-entry.json`；未建立全套 passed 收據。直接執行原版 `seal_validation.py`，雙邊均在 targeted `rows['status']=='passed'` 條件拒絕，輸出 `status=failed`、`same_sha_backend_regression_passed=false`。相同拒絕是本張對照結果，不是發布資格。

本張未執行 DDL／DML 或改 schema；無資料變更與還原 SQL。DB 核對為身分、授權與兩張結算表列數相同，不宣稱全庫逐列 checksum。於 `2026-11-14T01:33:47Z` 依核實的自有 listener PID 14852 正常 SHUTDOWN，parent PID 3424 同步退出；43324／42061／42083／42084 無 listener。本張 JVM／Python 無殘留。正式 JAR 前後皆 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`；未連正式 2000／3306，未改原 BIN、正式設定或 `docs/REPORT.md`。依最後的 60 分鐘時間盒完成所有入口嘗試，無逾時中止；08／12 單次最長分別 71.62／35.48 秒。

## 推測

F5 本輪雙邊通過、#429 候選失敗，可能存在觀測時序影響；本輪各一次不足以證明穩定修復。Maven 的 class 守門失敗與候選的歷史 parent／SQL 前置失敗，不能直接當成受測產品行為差異。

## 待驗證／給下一棒的提示

1. **主控／Claude**：優先裁定 WeaknessWorldNpc 的 8 案 golden 差異、EarthBindWorldNpc 的 1 案 MP 差異，以及 08 的 8 組不同首因；先區分歷史 parent／SQL 前置與 Maven class 守門，不能直接認定全部為產品語意退化。
2. **工具／fixture 維護者**：接手差異 TSV 中尚未接入 Maven 相容模式的 class 守門，以及 existing 19／20 的前置收據依賴；保留原行為斷言，不靠放寬斷言或修改實驗 DB 掩蓋。
3. **本機重測**：只接手明列不同的 9 個案例及 [剩餘 5,070 案](issue-433/remaining-groups.tsv) 所屬入口；本輪原斷言在組內提早停止的案例仍未完整觀測。已完成且一致的案例、D2／F8、既有 player 184 與 curse-clock 24 不原樣重跑。若產品／fixture 修正影響其前提，再由主控決定必要回歸。
4. **調度器**：保全 `E/results/`、`E/evidence/`、scratch 腳本；本張未部署、未取得 seal 資格。所有實際命令與耗時在 `groups.json`／`remaining-runs.json`，沒有因一組失敗而取消後續入口。

## 產出路徑

- 本報告：`docs/reports/issue-433.md`。
- 差異明細：同目錄 `issue-433/` 下 TSV；相同結果只在本報告列數量。
- 完整本機證據：`E/results/`、`E/evidence/`；重現腳本 `E/run_groups.py`、`run_remaining.py`、`run_seal.py`、`compare.py`。調度器處理 worktree 前須保全證據；不可公開 `E/inputs/`。
