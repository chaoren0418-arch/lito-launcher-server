# Issue #436 — T150：M2 指定入口回歸補完

**結論：部分通過（✅ 13／❌ 2／⚠️ 62；以逐項表 77 列計）。08 的 8 組雙邊完成，7 組一致、existing 15 仍不同；5,070 個待補內部案例已完成其所屬入口嘗試，新增雙邊原案例斷言裁定 198 案（相同 197、不同 1），剩餘 4872 案待驗證。**

## 已驗證

- 本張全程在既有 `task/436`，基線為當時最新 `origin/main` `5334f93529ce8c1c5eec9741cec12b00b5c8530d`。`mvn -B -DskipTests package` 成功，耗時 93 秒；JDK 17.0.17.10、Maven 3.9.16。沒有執行 Maven 單元測試。
- Maven JAR SHA-256：`84ba0cde7abace28456232b86922b22dd7fbff98e8b44e8b7bec5f995a79edcc`；候選：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。Maven 產物與 #433 相同；本次主要驗證 #435 的工具變更。
- #433 剩餘清單未合併到 main，從唯讀的 `../task-433/docs/reports/issue-433/remaining-groups.tsv` 取得，來源 commit `bd43ef9a777e5c2d880de7bef799f8a94c7e7439`。逐案母體只選 #433 的「無法判定／覆蓋不同」5,070 列，未將其已裁定的 2,310 案重新計入。清單與 JAR 雜湊固定於 `E/evidence/baseline.json`。
- `p24lab` lease 持有人為 `chaoren0418-arch/lito-launcher-server#436`，到期 `2026-11-14T04:37:29Z`；P24 由本張啟動，受測 DB 僅 `127.0.0.1:43324`，身分為 `lito83_exp@127.0.0.1`、UUID `e3443e40-d090-11f1-8040-4cedfb679789`。
- `E`＝本 worktree 的 `scratch/issue-436/`；`R`＝`E/results/`。資源視圖含 260 個唯讀用途 junction，**不得遞迴刪除**。未修改原專案、歷史 fixture、產品程式、行為斷言、正式設定或 `docs/REPORT.md`。

### 執行與計數方式

75 個指定入口（08：8；12：67），共 150 次雙邊執行。所有呼叫均帶 `--continue-per-case`；候選使用 `exact`、Maven 使用 `maven-normalized`。scratch 選擇器只篩選原工具的入口，保留 #435 `CaseRunner`、守門、案例函式及其斷言。player boundaries 只選尚未完成的 Lifecycle 56 案，略過已完成的 Fault 8 案。每個入口只有指定的一支 driver／probe，因此收據的 `case_summary.planned=1`；不能把原工具既有的整套 passed 字串解讀成 20／18 組全套資格。

S01–S75 對應 `E/run_selected.py` 的 `plans()` 順序。精確命令、PID、起訖與 exit code 在 `R/runs.json`；單列重現為同檔對應 `command`（保留其 `--jar`、`--resources`、`--config`、`--continue-per-case` 與側別守門設定）。比較由 `python -B -X utf8 E/map_prefixes.py`、`E/compare.py` 執行。

內部逐案採兩種證據：(1) 完整 probe 的原斷言、observations 案數與真實案例目錄數相符；(2) 凍結 source SHA 相符、main 的同步 for-loop 每個目錄恰好呼叫一次 exercise、loop 內沒有 catch／continue／break／return、主執行緒確實在該 probe 的 exercise 拋出斷言、目錄建立時間唯一且位於本次執行區間。第二種才可由下一迭代已進入證明前一迭代已完成，並把最後一個目錄對應到實際首個失敗。完整 source／堆疊／迴圈與映射保存在 `E/evidence/prefix-proofs.json`；多個凍結 source 版本或堆疊不符者不推定。僅有目錄存在不算通過，首個失敗不代表其後所有案例；此處通過指原案例斷言完成，不聲稱整個 probe 最後的 golden／整體檢查通過。

本張選擇器最初把 player Lifecycle 的 AST 篩選放在 `adapt_driver` 前，重排字句而影響 Maven 守門匹配；該筆無效執行留在 `E/evidence/invalid-player-selection/`，未計入結果。修正為先套原 Maven 適配再篩選，只補跑該 Maven 入口，未改工具、fixture 或斷言。有效資料仍是 150 次；額外無效嘗試 1 次。S19 Maven 正確證據為 `R/19-maven-corrected.log`、`R/receipts/19-maven-corrected.json`。

按指示執行「剩餘案例所屬入口」時，`mass-teleport-lifetime/races` 仍從第 1 案走到已知失敗點，`earth-bind-material/WorldNpc` 也會遇到已裁定首案；fixture 沒有內部案例過濾參數。本張未單獨重測 #433 的 9 案，也不重新裁定該 9 案；不可避免的入口前綴執行不計入本張待補母體。

### 逐項紀錄

✅ 表示本張對照作業一致或作業完成；兩邊相同失敗不表示產品功能通過。❌ 表示兩邊結果／首因不同。⚠️ 表示入口已嘗試，但仍缺內部逐案結果。S 編號的完整重現命令見 `R/runs.json`。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據 |
|---|---|---|---|---|
| 00 | ✅ | 最新 main Maven 建置成功，產物雜湊固定 | Server：`mvn -B -DskipTests package` | `E/maven-package.log`、`E/evidence/jars.json` |
| 08-01 | ✅ | `existing/13`：雙邊同樣失敗：Unknown column 'id' in 'field list' | S01 | `R/01-{candidate,maven}.log`、`R/receipts/01-{candidate,maven}.json` |
| 08-02 | ❌ | `existing/15`：兩邊首因不同，見差異表 | S02 | `R/02-{candidate,maven}.log`、`R/receipts/02-{candidate,maven}.json` |
| 08-03 | ✅ | `existing/16`：雙邊通過 | S03 | `R/03-{candidate,maven}.log`、`R/receipts/03-{candidate,maven}.json` |
| 08-04 | ✅ | `affected/4`：雙邊同樣失敗：Unknown column 'id' in 'field list' | S04 | `R/04-{candidate,maven}.log`、`R/receipts/04-{candidate,maven}.json` |
| 08-05 | ✅ | `affected/5`：雙邊同樣失敗：preaccepted material commits once across STOPPED | S05 | `R/05-{candidate,maven}.log`、`R/receipts/05-{candidate,maven}.json` |
| 08-06 | ✅ | `affected/7`：雙邊同樣失敗：Unknown column 'id' in 'field list' | S06 | `R/06-{candidate,maven}.log`、`R/receipts/06-{candidate,maven}.json` |
| 08-07 | ✅ | `affected/12`：雙邊同樣失敗：Unknown column 'id' in 'field list' | S07 | `R/07-{candidate,maven}.log`、`R/receipts/07-{candidate,maven}.json` |
| 08-08 | ✅ | `affected/16`：雙邊同樣失敗：Unknown column 'id' in 'field list' | S08 | `R/08-{candidate,maven}.log`、`R/receipts/08-{candidate,maven}.json` |
| 12-09 | ⚠️ | `targeted/mass-teleport-lifetime/races`：雙邊同樣失敗：only live or already admitted shutdown reply commits mode=12；本張待補 43 案，仍待驗證 43 案 | S09 | `R/09-{candidate,maven}.log`、`R/receipts/09-{candidate,maven}.json` |
| 12-10 | ⚠️ | `targeted/mass-teleport-lifetime/material-replay`：雙邊同樣失敗：rejected teleport unlock exactly once；本張待補 96 案，仍待驗證 90 案 | S10 | `R/10-{candidate,maven}.log`、`R/receipts/10-{candidate,maven}.json` |
| 12-11 | ⚠️ | `targeted/area-sleep-slow/tests`：雙邊同樣失敗：area MP once skill=66 world=false mode=0；本張待補 144 案，仍待驗證 143 案 | S11 | `R/11-{candidate,maven}.log`、`R/receipts/11-{candidate,maven}.json` |
| 12-12 | ⚠️ | `targeted/area-sleep-slow/boundaries`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 16 案，仍待驗證 15 案 | S12 | `R/12-{candidate,maven}.log`、`R/receipts/12-{candidate,maven}.json` |
| 12-13 | ⚠️ | `targeted/area-sleep-slow/lifecycle`：雙邊同樣失敗：admission and cost follow valid frozen membership mode=3；本張待補 96 案，仍待驗證 92 案 | S13 | `R/13-{candidate,maven}.log`、`R/receipts/13-{candidate,maven}.json` |
| 12-14 | ⚠️ | `targeted/area-sleep-slow/npc`：雙邊同樣失敗：no retired/failed NPC effect；本張待補 64 案，仍待驗證 48 案 | S14 | `R/14-{candidate,maven}.log`、`R/receipts/14-{candidate,maven}.json` |
| 12-15 | ⚠️ | `targeted/area-sleep-slow/companions`：雙邊同樣失敗：actual companion area effect; slow cancels haste first；本張待補 128 案，仍待驗證 111 案 | S15 | `R/15-{candidate,maven}.log`、`R/receipts/15-{candidate,maven}.json` |
| 12-16 | ⚠️ | `targeted/area-material/tests`：雙邊同樣失敗：area MP once skill=167 world=false mode=0；本張待補 144 案，仍待驗證 134 案 | S16 | `R/16-{candidate,maven}.log`、`R/receipts/16-{candidate,maven}.json` |
| 12-17 | ⚠️ | `targeted/area-material/boundaries`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 16 案，仍待驗證 14 案 | S17 | `R/17-{candidate,maven}.log`、`R/receipts/17-{candidate,maven}.json` |
| 12-18 | ⚠️ | `targeted/area-material/lifecycle`：雙邊同樣失敗：java.lang.AssertionError: area effects precede unlocked publication；本張待補 96 案，仍待驗證 86 案 | S18 | `R/18-{candidate,maven}.log`、`R/receipts/18-{candidate,maven}.json` |
| 12-19 | ⚠️ | `player/cancellation-probability/boundaries`：雙邊同樣失敗：caster still receives committed MP after target retirement；本張待補 56 案，仍待驗證 52 案 | S19 | `R/19-{candidate,maven}.log`、`R/receipts/19-{candidate,maven}.json` |
| 12-20 | ⚠️ | `player/cancellation-probability/probability-lifecycle`：雙邊同樣失敗：caster still receives committed MP after target retirement；本張待補 56 案，仍待驗證 52 案 | S20 | `R/20-{candidate,maven}.log`、`R/receipts/20-{candidate,maven}.json` |
| 12-21 | ❌ | `composed/water-life-material/Material`：雙邊同樣失敗：Source resources and no HP change；已定位內部結果不同 1 案；本張待補 64 案，仍待驗證 63 案 | S21 | `R/21-{candidate,maven}.log`、`R/receipts/21-{candidate,maven}.json` |
| 12-22 | ⚠️ | `composed/water-life-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 8 案，仍待驗證 7 案 | S22 | `R/22-{candidate,maven}.log`、`R/receipts/22-{candidate,maven}.json` |
| 12-23 | ⚠️ | `composed/water-life-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 56 案，仍待驗證 56 案 | S23 | `R/23-{candidate,maven}.log`、`R/receipts/23-{candidate,maven}.json` |
| 12-24 | ⚠️ | `composed/protection-material/Material`：雙邊同樣失敗：Source resources and no HP change；本張待補 104 案，仍待驗證 104 案 | S24 | `R/24-{candidate,maven}.log`、`R/receipts/24-{candidate,maven}.json` |
| 12-25 | ⚠️ | `composed/protection-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 8 案，仍待驗證 7 案 | S25 | `R/25-{candidate,maven}.log`、`R/receipts/25-{candidate,maven}.json` |
| 12-26 | ⚠️ | `composed/protection-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 56 案，仍待驗證 56 案 | S26 | `R/26-{candidate,maven}.log`、`R/receipts/26-{candidate,maven}.json` |
| 12-27 | ✅ | `composed/armor-state/Fault`：雙邊通過；本張待補 8 案，仍待驗證 0 案 | S27 | `R/27-{candidate,maven}.log`、`R/receipts/27-{candidate,maven}.json` |
| 12-28 | ⚠️ | `composed/armor-state/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 56 案，仍待驗證 56 案 | S28 | `R/28-{candidate,maven}.log`、`R/receipts/28-{candidate,maven}.json` |
| 12-29 | ⚠️ | `composed/negative-material/Material`：雙邊同樣失敗：Source resources mode=0 skill=29 world=false；本張待補 312 案，仍待驗證 311 案 | S29 | `R/29-{candidate,maven}.log`、`R/receipts/29-{candidate,maven}.json` |
| 12-30 | ⚠️ | `composed/negative-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 24 案，仍待驗證 23 案 | S30 | `R/30-{candidate,maven}.log`、`R/receipts/30-{candidate,maven}.json` |
| 12-31 | ⚠️ | `composed/negative-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 168 案，仍待驗證 167 案 | S31 | `R/31-{candidate,maven}.log`、`R/receipts/31-{candidate,maven}.json` |
| 12-32 | ⚠️ | `composed/negative-material/Companion`：雙邊同樣失敗：actual companion receives accepted negative state；本張待補 192 案，仍待驗證 192 案 | S32 | `R/32-{candidate,maven}.log`、`R/receipts/32-{candidate,maven}.json` |
| 12-33 | ⚠️ | `composed/weakness-material/Material`：雙邊同樣失敗：Source resources mode=0 skill=47 world=false；本張待補 208 案，仍待驗證 208 案 | S33 | `R/33-{candidate,maven}.log`、`R/receipts/33-{candidate,maven}.json` |
| 12-34 | ⚠️ | `composed/weakness-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 16 案，仍待驗證 15 案 | S34 | `R/34-{candidate,maven}.log`、`R/receipts/34-{candidate,maven}.json` |
| 12-35 | ⚠️ | `composed/weakness-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 112 案，仍待驗證 111 案 | S35 | `R/35-{candidate,maven}.log`、`R/receipts/35-{candidate,maven}.json` |
| 12-36 | ✅ | `composed/ground-material/Fault`：雙邊通過；本張待補 8 案，仍待驗證 0 案 | S36 | `R/36-{candidate,maven}.log`、`R/receipts/36-{candidate,maven}.json` |
| 12-37 | ⚠️ | `composed/ground-material/Lifecycle`：雙邊同樣失敗：stale caster rejects; owned STOPPED receipt finishes once；本張待補 96 案，仍待驗證 93 案 | S37 | `R/37-{candidate,maven}.log`、`R/receipts/37-{candidate,maven}.json` |
| 12-38 | ✅ | `composed/ground-material/Clock`：雙邊通過；本張待補 48 案，仍待驗證 0 案 | S38 | `R/38-{candidate,maven}.log`、`R/receipts/38-{candidate,maven}.json` |
| 12-39 | ⚠️ | `composed/silence-material/Material`：雙邊同樣失敗：Source resources mode=0 skill=64 world=false；本張待補 104 案，仍待驗證 104 案 | S39 | `R/39-{candidate,maven}.log`、`R/receipts/39-{candidate,maven}.json` |
| 12-40 | ⚠️ | `composed/silence-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 8 案，仍待驗證 7 案 | S40 | `R/40-{candidate,maven}.log`、`R/receipts/40-{candidate,maven}.json` |
| 12-41 | ⚠️ | `composed/silence-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 56 案，仍待驗證 55 案 | S41 | `R/41-{candidate,maven}.log`、`R/receipts/41-{candidate,maven}.json` |
| 12-42 | ⚠️ | `composed/decay-material/Material`：雙邊同樣失敗：Source resources mode=0 skill=71 world=false；本張待補 96 案，仍待驗證 96 案 | S42 | `R/42-{candidate,maven}.log`、`R/receipts/42-{candidate,maven}.json` |
| 12-43 | ⚠️ | `composed/decay-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 8 案，仍待驗證 7 案 | S43 | `R/43-{candidate,maven}.log`、`R/receipts/43-{candidate,maven}.json` |
| 12-44 | ⚠️ | `composed/decay-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 56 案，仍待驗證 55 案 | S44 | `R/44-{candidate,maven}.log`、`R/receipts/44-{candidate,maven}.json` |
| 12-45 | ⚠️ | `composed/world-potion/`：雙邊同樣失敗：real targeted Decay Potion applied before drinking；本張待補 64 案，仍待驗證 63 案 | S45 | `R/45-{candidate,maven}.log`、`R/receipts/45-{candidate,maven}.json` |
| 12-46 | ⚠️ | `composed/target-negative-material/Material`：雙邊同樣失敗：Source resources mode=0 skill=173 world=false；本張待補 224 案，仍待驗證 224 案 | S46 | `R/46-{candidate,maven}.log`、`R/receipts/46-{candidate,maven}.json` |
| 12-47 | ⚠️ | `composed/target-negative-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 16 案，仍待驗證 15 案 | S47 | `R/47-{candidate,maven}.log`、`R/receipts/47-{candidate,maven}.json` |
| 12-48 | ⚠️ | `composed/target-negative-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 112 案，仍待驗證 111 案 | S48 | `R/48-{candidate,maven}.log`、`R/receipts/48-{candidate,maven}.json` |
| 12-49 | ⚠️ | `composed/blind-material/Material`：雙邊同樣失敗：Source resources mode=0 skill=40 world=false；本張待補 208 案，仍待驗證 208 案 | S49 | `R/49-{candidate,maven}.log`、`R/receipts/49-{candidate,maven}.json` |
| 12-50 | ⚠️ | `composed/blind-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 16 案，仍待驗證 15 案 | S50 | `R/50-{candidate,maven}.log`、`R/receipts/50-{candidate,maven}.json` |
| 12-51 | ⚠️ | `composed/blind-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 112 案，仍待驗證 111 案 | S51 | `R/51-{candidate,maven}.log`、`R/receipts/51-{candidate,maven}.json` |
| 12-52 | ⚠️ | `composed/blind-material/WorldNpc`：雙邊同樣失敗：actual population effect follows Source roll；本張待補 64 案，仍待驗證 55 案 | S52 | `R/52-{candidate,maven}.log`、`R/receipts/52-{candidate,maven}.json` |
| 12-53 | ⚠️ | `composed/resurrection-ingress/Material`：雙邊同樣失敗：resurrection Source MP mode=0 skill=61 world=false；本張待補 256 案，仍待驗證 255 案 | S53 | `R/53-{candidate,maven}.log`、`R/receipts/53-{candidate,maven}.json` |
| 12-54 | ⚠️ | `composed/resurrection-ingress/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 8 案，仍待驗證 7 案 | S54 | `R/54-{candidate,maven}.log`、`R/receipts/54-{candidate,maven}.json` |
| 12-55 | ⚠️ | `composed/resurrection-ingress/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 48 案，仍待驗證 47 案 | S55 | `R/55-{candidate,maven}.log`、`R/receipts/55-{candidate,maven}.json` |
| 12-56 | ⚠️ | `composed/resurrection-ingress/Numeric`：雙邊同樣失敗：accepted question except original MP overflow rejecting S_MANA_POINT before the question；本張待補 48 案，仍待驗證 47 案 | S56 | `R/56-{candidate,maven}.log`、`R/receipts/56-{candidate,maven}.json` |
| 12-57 | ⚠️ | `composed/resurrection-ingress/AnswerLifetime`：雙邊同樣失敗：actual native cast installed question before transition；本張待補 64 案，仍待驗證 63 案 | S57 | `R/57-{candidate,maven}.log`、`R/receipts/57-{candidate,maven}.json` |
| 12-58 | ⚠️ | `composed/resurrection-ingress/AnswerRace`：雙邊同樣失敗：actual cast installs question before queued reply；本張待補 112 案，仍待驗證 111 案 | S58 | `R/58-{candidate,maven}.log`、`R/receipts/58-{candidate,maven}.json` |
| 12-59 | ⚠️ | `composed/poison-publication/Material`：雙邊同樣失敗：Source resources mode=0 skill=11 world=false；本張待補 192 案，仍待驗證 191 案 | S59 | `R/59-{candidate,maven}.log`、`R/receipts/59-{candidate,maven}.json` |
| 12-60 | ⚠️ | `composed/poison-publication/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 16 案，仍待驗證 15 案 | S60 | `R/60-{candidate,maven}.log`、`R/receipts/60-{candidate,maven}.json` |
| 12-61 | ⚠️ | `composed/poison-publication/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 112 案，仍待驗證 111 案 | S61 | `R/61-{candidate,maven}.log`、`R/receipts/61-{candidate,maven}.json` |
| 12-62 | ⚠️ | `composed/poison-publication/Clock`：雙邊同樣失敗：actual first socket write delayed；本張待補 8 案，仍待驗證 7 案 | S62 | `R/62-{candidate,maven}.log`、`R/receipts/62-{candidate,maven}.json` |
| 12-63 | ⚠️ | `composed/poison-publication/Consumer`：雙邊同樣失敗：actual first socket write delayed；本張待補 8 案，仍待驗證 7 案 | S63 | `R/63-{candidate,maven}.log`、`R/receipts/63-{candidate,maven}.json` |
| 12-64 | ⚠️ | `composed/poison-publication/Timer`：雙邊同樣失敗：actual first socket write delayed；本張待補 8 案，仍待驗證 7 案 | S64 | `R/64-{candidate,maven}.log`、`R/receipts/64-{candidate,maven}.json` |
| 12-65 | ⚠️ | `composed/poison-publication/CurseConsumer`：雙邊同樣失敗：actual first socket write delayed；本張待補 24 案，仍待驗證 23 案 | S65 | `R/65-{candidate,maven}.log`、`R/receipts/65-{candidate,maven}.json` |
| 12-66 | ⚠️ | `composed/earth-bind-material/Material`：雙邊同樣失敗：Source resources mode=0 skill=157 world=false；本張待補 112 案，仍待驗證 112 案 | S66 | `R/66-{candidate,maven}.log`、`R/receipts/66-{candidate,maven}.json` |
| 12-67 | ⚠️ | `composed/earth-bind-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 8 案，仍待驗證 7 案 | S67 | `R/67-{candidate,maven}.log`、`R/receipts/67-{candidate,maven}.json` |
| 12-68 | ⚠️ | `composed/earth-bind-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 56 案，仍待驗證 56 案 | S68 | `R/68-{candidate,maven}.log`、`R/receipts/68-{candidate,maven}.json` |
| 12-69 | ⚠️ | `composed/earth-bind-material/WorldNpc`：首因為 #433 已接受的差異，本張不重新裁定；本張待補 31 案，仍待驗證 31 案 | S69 | `R/69-{candidate,maven}.log`、`R/receipts/69-{candidate,maven}.json` |
| 12-70 | ⚠️ | `composed/polymorph-material/Material`：雙邊同樣失敗：Source resources mode=0 world=false；本張待補 128 案，仍待驗證 128 案 | S70 | `R/70-{candidate,maven}.log`、`R/receipts/70-{candidate,maven}.json` |
| 12-71 | ⚠️ | `composed/polymorph-material/Fault`：雙邊同樣失敗：candidate failure preserves canonical identity and outputs exactly299；本張待補 8 案，仍待驗證 7 案 | S71 | `R/71-{candidate,maven}.log`、`R/receipts/71-{candidate,maven}.json` |
| 12-72 | ⚠️ | `composed/polymorph-material/Lifecycle`：雙邊同樣失敗：actual target producer reserves and waits for ordered player lock；本張待補 56 案，仍待驗證 55 案 | S72 | `R/72-{candidate,maven}.log`、`R/receipts/72-{candidate,maven}.json` |
| 12-73 | ⚠️ | `composed/polymorph-material/Menu`：雙邊同樣失敗：actual cast creates menu token；本張待補 80 案，仍待驗證 80 案 | S73 | `R/73-{candidate,maven}.log`、`R/receipts/73-{candidate,maven}.json` |
| 12-74 | ✅ | `composed/return-nature-material/Fault`：雙邊通過；本張待補 8 案，仍待驗證 0 案 | S74 | `R/74-{candidate,maven}.log`、`R/receipts/74-{candidate,maven}.json` |
| 12-75 | ⚠️ | `composed/return-nature-material/Lifecycle`：雙邊同樣失敗：only valid original target or owned STOPPED cast pays；本張待補 36 案，仍待驗證 30 案 | S75 | `R/75-{candidate,maven}.log`、`R/receipts/75-{candidate,maven}.json` |
| 14 | ✅ | 自有 P24 正常關閉，DB 核對及正式 JAR 前後相同；自有程序無殘留 | ReadOnlyProbe 後執行 `E/stop-p24.ps1`，核對 PID／埠 | `E/evidence/final-verification.json`、`shutdown.json` |

### 只列不同的結果

- 08：一致 **7 組**，其中雙邊通過 1 組、同樣失敗 6 組；不同 **1 組**。7 組 Maven overlay 守門均通過，證據 `E/evidence/guard-results.json`。
- existing 15：候選停在 `actual Main reaches ready`（SQL 缺 `id`）；Maven 停在 `Main.class` normalized 守門，差異方法為 `accept(...)`、`main(...)`。本次未越過 Main 守門，不能宣稱 SourceLogin 已實跑通過或失敗。
- 12 的 67 個入口：首因一致 **66**、不同 **1**、未完成雙邊嘗試 **0**。該首因差異是 #433 已由主控接受的 EarthBindWorldNpc 首案，不重新列為新失敗；其後 31 案仍列待驗證。入口統計與內部逐案統計分開。
- 本張 5,070 案中：雙邊原案例斷言裁定 **198**，相同 **197**（雙邊通過 151、同樣失敗 46）、結果不同 **1**；未完整裁定 **4872**（只有一邊取得完整結果 32、兩邊都缺 4840）。未取得結果不算相同失敗。
- 新差異 `composed/water-life-material/Material/false-false-false-0`：候選通過該迭代並進入 mode 1；Maven 在 mode 0 失敗於 `Source resources and no HP change`。Maven log 另記錄 Water Life 因 `required=4 elf-attr=0` 被拒；此為實際觀察，是否符合 M3 設計交主控裁定。原碼 SHA `45448ac79ec439aed7bf4301bd20a90c546b2817d6a8d7ac779834519bb83235`、雙邊 main／exercise 堆疊與目錄順序相符。雖兩邊入口首因文字相同，發生於不同案例，故不能以入口一致遮掉此逐案差異。候選 mode 1 的失敗與 Maven 尚未到達 mode 1 不列為結果不同，而留待驗證。
- #433 已裁定 **2,310 案（相同 2,301；主控接受 M3 有意差異 9）**沿用，不重列。

明細：[入口差異](issue-436/group-differences.tsv)、[逐案結果差異](issue-436/case-differences.tsv)、[新差異取證](issue-436/case-evidence.json)、[觀測欄位差異](issue-436/measurement-differences.tsv)。相同案例只列數量。剩餘範圍見 [剩餘清單](issue-436/remaining-groups.tsv)；完整逐案與入口證據留在 `R/all-case-results.tsv`、`R/all-group-results.tsv`，不把缺結果當成語意差異。

### 環境與收尾

本張沒有執行改資料／schema 的 SQL，也沒有修補實驗 DB 來掩蓋失敗。啟動時曾嘗試額外完整備份，但 P24 未附 `mysqldump.exe`，未產生備份；沒有進行需還原的 DB 修改，因此還原 SQL 不適用。前後驗證涵蓋連線身分、授權及兩張結算表列數，不宣稱全庫逐列不變。

正式 JAR 前後皆 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。只依本張紀錄的 PID 及啟動時間關閉 P24；未連正式 2000／3306，未改原 BIN、玩家實表、系統服務或權限。採最後指示的 **60 分鐘**時間盒，單入口上限 590 秒；實際起訖、是否逾時與關閉證據見 `E/evidence/final-verification.json`。

## 推測

同樣 SQL 缺欄位或相同首個行為斷言可能遮住後續差異；不能從首因一致推論整組語意等價。existing 15 可能需從 Maven 自身 class 重產 observer overlay，但本張只記實測，不放寬守門或修改產品。

## 待驗證／給下一棒的提示

1. **主控**：先裁定新定位的 WaterLifeMaterial mode 0 差異是否為有意的 M3 改動；本張未自行歸因。**工具維護者**接手 existing 15 的 Main overlay，依 #435 建議從 Maven 自身 class 重產並證明只差 hook；本張未自行修程式。
2. **主控／fixture 維護者**：4872 案仍需取得內部逐案結果。#435 的續跑單位是一支 driver／probe；Java probe 首個斷言仍會中止 JVM。先決定如何提供內部案例選擇／獨立執行且維持原斷言，再交本機只測剩餘清單；原樣再跑相同入口不能保證增加覆蓋。
3. **本機執行器**：只沿用本張剩餘清單與未裁定差異，不重跑已完成的相同案例或 #433 已由主控接受的 9 案；修正若改變前提，交主控決定必要擴測範圍。
4. **調度器**：保全 `E/results/`、`E/evidence/`、`E/selection-sources/` 與 scratch 腳本；`E/inputs/` 含私密設定，不公開。本張未部署、未取得 seal 資格、未刪除 worktree。

## 產出路徑

本報告 `docs/reports/issue-436.md`；差異與剩餘清單 `docs/reports/issue-436/`；本機原始證據 `scratch/issue-436/`。分支 `task/436`。
