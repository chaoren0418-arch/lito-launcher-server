# Issue #292 — T091：LiTo 現況能力矩陣

## 做了什麼
- 新增 `docs/reports/lito-capability-current.md`，分角色（16 列）、道具（15 列）、戰鬥（24 列）、技能（807 列統計＋職業分組＋G03–G18 對照）、跨界（15 列）五個領域。每一列都附「路徑:行號」證據和版本標記（正式／正式（patch）／50635／3ccbb／046c／未實作），找不到的寫「未找到」。
- 技能統計是用兩份底稿重算的：`combat-coverage-current.json` 和本機收尾矩陣 `skills.tsv`（046c58c5），腳本在 `scratch/t091/skill_stats.py`、`tsv_stats.py`。結果：181 個有入口、0 個已驗證、181 個待驗證、38 個 SQL 保留槽＋1 個 catalog 保留槽、588 個未實作（非玩家列）。
- 新發現：
  1. coverage 標「處理器待查」的 6 個技能（192／202／203／207／208／213），在 src/main 其實有入口：走 `useSelectorOnlyTargetedAttackSpell`（`ClientSession.java:60420`）。
  2. 0.6.68 報告說缺的 AC 額外減傷，現在已經補上（`BattlePhysicalDefense.java:112`）。
  3. 生命的祝福 +100：文件和程式的說法互相矛盾。
  4. 來源帳號隔離、職業倍率、技能機率這三項，在正式 JAR 是 bytecode patch 接上的，src/main 原始碼裡沒有呼叫點。
  5. 跨來源的角色 objid 撞號沒有處理。
  6. `lito_source_server` 的種子 SQL（3 個啟用）和 README（公牛、天使）不一致。

## 怎麼驗證
- 角色／道具、戰鬥、跨界由 3 個唯讀子代理搜尋。我抽查了其中 8 個關鍵行號，都對得上：`SourceLogin.java:88-95` 沒有呼叫點、`lito_source_server.sql:13-21`、`BattlePhysicalDefense.java:112`、`CombatRules.java:82-86`、`GameStateStore.java:7618-7619`、`Accounts.java:4977-4980`、`TournamentShopCatalog.java:12-19`、`PatchSourceLogin.java`。
- 技能的 6 個 selector-only 入口、G16 的 7 個技能 ID（本機收據），都是我自己讀檔確認的。

## 未驗證什麼
- 沒有啟動伺服器、沒有連 DB、沒有開遊戲。真角色載入、181 技能的行為、跨來源實機對戰都列在「待驗證」。
- patch 接線在正式 JAR 是否生效，只依據 patch 原始碼判斷，沒有反編譯正式 JAR 逐點核對。
- 子代理給的行號，除了上面抽查的以外，沒有逐一重讀。

## 產出路徑
- `docs/reports/lito-capability-current.md`
- `docs/reports/issue-292.md`
- 暫存腳本與輸出放在 `scratch/t091/`，不進 git。

## 給下一棒的提示
- T093 對照 #291 時，技能領域的主要差距是「有入口但 0 驗證」，不是「缺 handler」。588 個 NPC／道具技能列要先決定在不在產品範圍內。
- B-land（#138）時要把 patch 接線改成原始碼呼叫，不能漏：X6 `SourceLogin.account`、B8 `CombatClassDamage`、B10 `CombatProbability`。#281 的 5 個掛鉤只在候選的 `LegacyMySqlRepository`。
- B12（生命的祝福 +100）要拿 `1-伺服器` 的 `ConfigSkill` 實際載入結果來裁定。
- 跨來源 objid 撞號（X10）建議在 M3 跨界任務處理。
