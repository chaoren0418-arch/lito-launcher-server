# Issue #338（T118）魔法計算補驗：魔眼 +1 SP、SKD3 技能減免、傷害為 0 狀態

分支：`task/338`。對照表寫在 `docs/design/formulas-magic.md` §9（並更新 §1 M3／M11／M17 與 §7）。

## 做了什麼

1. **魔眼 +1 SP（M3）**：1-伺服器 `src/com/lineage/server/model/L1MagicMode.java:103-116` 對風（6686）、生命（6687）、形象（6689）之魔眼在 `getTargetSp` +1，只用在 `L1MagicPc.java:1035-1036` 的魔法傷害。LiTo 早已在 `CombatSpecialBuffs.addScalar(...,"SP")` 加上（#304 時沒查到）。
   - 發現差異：LiTo 的 SP 面板（`S_MAGIC_STATUS`）與延伸角色資訊也用同一個 `spellPowerModifier`，魔眼期間面板多顯示 1；1-伺服器 `serverpackets/S_SPMR.java:37` 寫 `getSp()-getTrueSp()`，不含這個 +1。
   - **已修正**：抽出 `CombatSpecialBuffs.EYE_MAGIC_SP_BUFFS`／`eyeMagicSpellPower`，`ClientSession` 新增 `displaySpellPowerModifier`（扣掉魔眼 +1），面板兩處改用它；魔法傷害不變。
2. **SKD3（M11）**：1-伺服器 `L1AttackList.java:256-286` 31 列逐筆對照：28 個料理狀態（特製 −5 ×24、4 階 −2 ×4）、龍之護鎧 −10、堅忍 −15、聖結界減半，全部一致；主菜＋甜點相加也一致。`CombatSourceReductions` 的 15／10 改成具名常數（行為不變）以便測試。
3. **傷害為 0（M17，SKM0）**：`L1AttackList.java:289-294`／`L1Character.java:1797-1808` 6 項（凍結狀態 4000、絕對屏障 78、冰矛 50、冰雪颶風 80、冰凍氣息 194、大地屏障 157）對上 LiTo `CombatPlayerMagic.protectedZero`（絕對屏障‖大地屏障‖凍結租約），三個冰凍技能共用同一凍結租約。
4. 新測試 `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/MagicReductionServer1ParityTest.java`（7 個測試）。

## 怎麼驗證

### 已驗證
- 1-伺服器 原始碼與 `db/380_db.sql` 逐行讀取（唯讀），行號見設計文件 §9。
- `etcitem_buff藥水能力` 欄位表（`sp` 是第 15 個數值欄）：7 個魔眼的 `sp` 都是 0，LiTo `BattleDragonEyeCatalog` 其他能力值與表一致；+1 SP 是寫死在程式裡，不是表值。
- 本機 `mvn -B -o -DskipTests test-compile` 通過（main＋test 都編得過）。
- 依本機執行限制，測試**沒有在本機跑**，交給 PR 的 GitHub Actions CI（`mvn -B test`）。

### 推測
- `STATUS_FREEZE`（4000，怪物凍結技能／方塊）在 LiTo 也寫進同一個 `freezeActive` 租約：怪物對玩家的凍結來源沒有逐條追。
- 打 NPC 的「傷害為 0」：世界怪物（`CombatWorldMagic.java:20`）與房間召喚物（`CombatLineSpells.java:47`）已確認，其他 NPC 呼叫點沒有全數列舉。
- `effectivePatiencePhysicalReduction` 不為 0 ⇔ 堅忍生效（只讀了 `ClientSession` 分派到 `GameHub`，沒讀 `GameHub` 細節）。

### 待驗證
- CI 結果（push 後看 PR）。
- 實機：使用風／生命／形象之魔眼時，SP 面板**不再**多 1，但魔法傷害仍含 +1（交 Codex）。

## 產出路徑
- `docs/design/formulas-magic.md`（§1 M3／M11／M17、§7、新增 §9）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/CombatSpecialBuffs.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`（`displaySpellPowerModifier`）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/CombatSourceReductions.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/MagicReductionServer1ParityTest.java`
- 本報告 `docs/reports/issue-338.md`

## 給下一棒的提示
- §7 只剩 P1（千分比迴避）與實機項目未結。
- NPC 施法（`L1MagicNpc.java:568` 也讀 SKD3）不在本任務範圍；若要做 NPC→玩家 魔法對照，可直接重用本測試的 `refSkd3()`。
- 面板 SP 修正只扣魔眼 +1；若之後有其他「只進傷害、不進面板」的 SP 來源，也要加進 `displaySpellPowerModifier`。
