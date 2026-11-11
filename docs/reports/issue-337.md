# Issue #337 報告：T117 其他職業技能效果數值補驗

- 分支：`task/337`（code，第 1 次嘗試；已合併 origin/main，含 #336）
- 設計文件：`docs/design/skills-others.md` §6（新增），並更新 §3（112、116、118、181、205、206、210、211、215、220）、§5、統計列
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/OtherClassSkillEffectServer1ParityTest.java`（新增，13 個）；`NativeWorldRunClanSourceAuditTest.java` 改 1 行
- 程式：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`、`GameHub.java`
- 參考（唯讀）：`D:\lineage開發\1-伺服器`（2026-11-11 版本）；縮寫見設計文件 §6 開頭

## 做了什麼

| 項目 | 結果 | 1-伺服器 | LiTo |
|---|---|---|---|
| 112 ×1.58 | 倍率一致；**已修正**料理減傷順序 | `src/com/lineage/server/model/L1AttackPc.java:1021-1034` | `BattleArmorBreak.java:75-79`、`ClientSession.combatDamageAfterCooking` |
| 181／211 減傷 | 數值正式一致（10／15）；**已修正**怪物魔法漏扣 | `src/com/lineage/server/model/L1AttackList.java:284-285`、`src/com/lineage/server/model/L1MagicNpc.java:526`、`:554-586` | `CombatSourceReductions.java:5-6`、新增 `ClientSession.sourceNpcMagicSkillReduction` |
| 206 MPR+2 | 一致（逐值） | `src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java:72-73`、`:159-213` | `BattleNaturalMpRegen.java` |
| 185／190／195 覺醒扣 MP | 一致：S1 設定 `config/charsettings.properties:105` 為 `false`（不扣），LiTo 正式讀同一份檔 | `src/com/lineage/server/timecontroller/skill/Skill_Awake_Timer.java:106-123`、`:163-168`、`src/com/lineage/config/ConfigCharSetting.java:83-99` | `CombatAwaken.java:6-12`、`CombatRules.java:44-47` |
| 205／210／215／220 立方 | 數值一致；**已修正**經典世界 2 處 | `src/com/lineage/server/model/L1Cube.java:181-319`、`src/com/lineage/server/timecontroller/skill/EffectCubeExecutor.java:53-134` | `BattleCube*Effects.java`、`GameHub.java`（經典世界 Alliance）、`ClientSession.effectiveCubeShockMagicResistanceModifier` |
| 116／118 | 116 一致；**已修正** 118 旗區；118 被拒扣 MP **待使用者裁定** | `src/com/lineage/server/model/skill/skillmode/RUN_CLAN.java:31-53`、`src/com/lineage/server/clientpackets/C_Attr.java:1301-1390`、`src/com/lineage/server/model/skill/L1SkillUse.java:979-983`、`:1196-1198` | `ClientSession.useRunClanSpell`、`runClanClassicWorldDestinationAllowed` |

### 程式變更（5 處）
1. **PC 物理減傷順序**（`combatDamageAfterCooking`）：料理減傷原本在最前面，移到破壞盔甲 ×1.58 之後。S1 的料理減傷屬 SKD3，在 ×1.58 之後才扣。只在「目標中破壞盔甲＋有料理減傷」時改變結果（例：100 傷害、料理 5，150 → 153）。其他路徑沒有乘法，順序不影響結果。
2. **怪物魔法打 PC 漏扣 181／211**：新增 `sourceNpcMagicSkillReduction(now)`（只在 `CombatRules.loaded()` 時生效），加進經典世界、任務、限時三條怪物魔法路徑在 MR 之前的減免總和。正式值：龍之護鎧 10、耐力 15。
3. **118 旗區**：`runClanClassicWorldDestinationAllowed` 拿掉「攻城戰中可進旗區」的例外。S1 的 `RUN_CLAN` 用 `checkInAllWarArea`，任何時候都拒絕；戰爭例外只屬於 116 的回答路徑。
4. **經典世界立方燃燒／地裂**：`GameHub` 的 Alliance 判斷原本先看安全區，再看盟友，導致盟友在安全區拿不到屬性 +30。改成和衝擊、和諧一樣先判盟友（S1 `EffectCubeExecutor.java:72-98`）。
5. **經典世界衝擊立方 MR−10**：S1 只存標記、沒有任何讀取點，MR 不變；LiTo 正式戰鬥房已回 0，但經典世界仍套 −10。在 WORLD 分支補上 `CombatRules.loaded()` 閘門。離線後備的 −10 是 LiTo 刻意「修復」，保留。

### 測試（`OtherClassSkillEffectServer1ParityTest`，13 個）
- 數值逐值比對（以 S1 算式寫參照函式）：×1.58（0..5000 × 14 種武器）；PC 物理減傷整串（傷害 0..400 × 等級 × 料理 × 16 種狀態組合），同時證明改前順序與 S1 不同；MPR 回復（7 職業 × WIS × 原始 WIS × 裝備 × 狀態 × 料理）；覺醒每 4 秒 1 拍；立方常數。
- 原始碼順序／條件鎖定：NPC 目標不放大、181／211 正式值、三條怪物魔法路徑在 MR 前扣、立方盟友先於安全區、衝擊在正式環境不改 MR、118 旗區無戰爭例外、118 拒絕在扣費前（鎖現況，待裁定）。

## 怎麼驗證

### 已驗證
- 1-伺服器 原始碼逐行閱讀（上表與設計文件 §6 行號）。立方與 116／118 先由兩個唯讀子代理比對，其中所有被採用的不一致項（118 旗區、118 扣 MP 時機、立方安全區、衝擊 MR）我都親自重讀雙方原始碼確認過。
- 本機 `mvn -B -q -DskipTests test-compile`（只編譯）：合併 main 前後皆 exit 0。
- 本機用 Python 靜態讀檔（`scratch/t337/check.py`，不進 git）確認測試裡的多行原始碼字串都存在、順序正確：全部 OK。
- **本機沒有執行任何測試**（依規定）；`mvn -B test` 交給 PR 的 GitHub Actions CI。

### 推測
- S1 `L1AttackPc` 的 `dmg` 是 double，×1.58 前可能已有小數；LiTo 此處輸入是 int。只差截斷，屬 T113。
- 怪物魔法打 PC：S1 的料理減傷與聖結界 ÷2 也在 MR 之前，LiTo 仍在 MR 之後。料理不在本任務範圍，未改。
- 火牢舊路徑沒有套 181／211（S1 `EffectFirewallTimer.java:251-260` 有套）；屬法師技能，T116 已列，未改。
- 攻城戰中，立方對安全區內的敵人仍略過（S1 會當敵人）；只有攻城戰＋安全區＋經典世界同時成立時才有差，未改。
- `NativeWorldRunClanSourceAuditTest` 的字串是舊變數名（`target`），因找不到 `../1-伺服器` 而一直被 assume 跳過；本 PR 只把戰爭例外那一行改成反向斷言。

### 待驗證（交給 Codex／實機）
- 破壞盔甲＋料理時的實際傷害；怪物魔法打開著龍之護鎧／耐力的玩家，傷害少 10／15。
- 經典世界：安全區內的隊友吃得到燃燒／地裂立方的屬性 +30；衝擊立方不再讓 MR −10。
- 118 在攻城戰期間對旗區內的盟友使用 → 1192 並解除傳送鎖定。
- 116 的 729 對話框文字、傳送畫面；118 傳送後面向 5。
- 立方圖示／特效、地裂束縛封包。

## 待使用者裁定（本 PR 不改）
| 項目 | 1-伺服器 | LiTo 現況 | 建議 |
|---|---|---|---|
| 118 被 647／1192 拒絕 | 扣 MP 30、播動作、無效果（`L1SkillUse.java:979-983` 先執行 mode，`:1196-1198` 再扣） | 扣費前拒絕，不扣 MP | 與 T116 的 66 同類。要對齊 S1，就把兩個拒絕移到扣費之後；要維持 LiTo，理由是 #321 S3「扣 MP 沒效果是缺陷」 |
| 116 額外限制 | 無邀請逾時；施放時不檢查目標狀態 | 30 秒逾時；施放時要求同房／同世界（629，不扣 MP） | 配合競技場房間，建議維持 |

## 產出路徑
- `docs/design/skills-others.md`
- `docs/reports/issue-337.md`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/GameHub.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/OtherClassSkillEffectServer1ParityTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NativeWorldRunClanSourceAuditTest.java`

## 給下一棒的提示
- 181／211 的減傷散在 5 個地方：`combatDamageAfterCooking`（物理）、`CombatNpcPlayerDamage`（寵物物理）、`CombatPlayerMagic.reductions`（玩家魔法）、`sourceNpcMagicSkillReduction`（怪物魔法）、火牢舊路徑（仍缺）。之後若要抽成一個 SKD3 方法，請一起改 `OtherClassSkillEffectServer1ParityTest` 的 `monsterMagicSubtractsDragonSkinAndPatienceBeforeMagicResistance` 與 `playerPhysicalReductionOrderMatchesL1AttackPc`。
- 118 若裁定對齊 S1，要把 `useRunClanSpell` 的 647／1192 移到扣費與 `S_ACTION` 之後，並改 `runClanMapRejectionsCurrentlyHappenBeforeCost`。
- 經典世界立方的關係判斷在 `GameHub` 四個 Alliance lambda；戰鬥房走 `NativeSourceCubeRelation`。攻城戰中安全區的敵人例外若要補，四個 lambda 都要改。
- 覺醒扣 MP 由 `charsettings.properties` 的 `DragonKnightAwakeClassicMode` 決定；1-伺服器 目前是 `false`，所以實機看不到扣 MP 是正確結果。
