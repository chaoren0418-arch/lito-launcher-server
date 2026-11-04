# Issue #67 / T020-B06 — 戰鬥併入暫存（existing-battle，29 類）

## 結論

B06 的 29 個外層類別已三方移植進暫存區，`javac --release 17` 成功產出剛好 323 個 class entry（與 `verify_entries` 一致：無缺漏、無多出）。T020d 對候選 `50635f7e` 的語意比對：**SAME=253、DIFF=70、ONLY_LEFT=0、ONLY_RIGHT=0**。剩餘 70 項皆為 **javac 無法表達的 ASM 拼接**（DUP-before-PUTFIELD、record 額外 instance field／ctor、DUP2、部分方法體插樁），不是漏搬 Java 可寫的欄位或方法。

未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

## 做了什麼

範圍僅：

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 下本批 29 個 `.java`
- 本報告

做法（三方移植）：

1. **簡單狀態類**（Icons1／CombatBuffLeases）：以候選 CFR 為底，補 `volatile transient long nativeSourceIcons1Version` 與 `NativeSourceIcons1Epoch.bump`。`BattleHealingRecovery`、`BattleHitReactionSuppressCatalog` 整類 **SAME**。
2. **BattleAbilityStatus／BattleTimedStatus／BattleTournamentNpcPhysicalDamage**：候選 CFR 清理後放入 staged（去掉 record 上非法的重複 instance field）。AbilityStatus 內部 record 除 `Protection.applyTo` 外皆 SAME；TimedStatus 內部 record 全 SAME。
3. **BattleSpellCatalog**：保留 `src/main` record 結構，插入 700+ 個 `ResourceDatabase.value`／`CombatDatabase.longValue` 存取器、`healDiceBonus`／`applyLawfulRevision` 的 `NativeSourceHealingFormula` 閘門、`nativeSource*Range`、FreezeAttack 的 `NativeSourceFreezeTarget.range`。SpellCatalog 相關 DIFF 從約 123 降到 20。
4. **BattleCompanion／四個 Cube／Ground／Quest／TimeAttack**：在 **非 record** 類別上補候選多出的 `Object` 欄位、`nativeMaterialPlace`、`nativeSource*` AI／治療／傷害委派方法，ctor 呼叫 `NativeSourceCubeRemainder.reset`／`NativeSourceCubeStatus.reset`。record（`Cube`／`Event`／`Snapshot`／`Pulse`）上的額外 instance field **無法用 Java 表達**（javac 拒絕「field declaration must be static」），故保留 src/main record 形狀。

## 怎麼驗證（已驗證）

環境：OpenJDK 21.0.12.1，`javac --release 17`。本機無 `lib/` 目錄，classpath 僅候選＋正式 JAR。

```bash
cd lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d /tmp/b06-out \
  merge-inputs/50635f7e/staged/local/lito381/server/Battle*.java
# exit 0；stdout／stderr 皆空；產出 323 個 .class
```

```bash
jar cf /tmp/b06.jar -C /tmp/b06-out .
lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left /tmp/b06.jar \
  --right lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
  --classes docs/combat-merge/batches.json 的 B06.verify_entries
```

（本輪實際以 `scratch/issue-67/verify_entries.txt` 傳完整 323 條；JSON `classSelection.missingLeft=[]`、`extraLeft=[]`。）

| 項目 | 結果 |
|---|---|
| 編譯 | exit 0，323 class |
| entry 完整性 | 323／323，無多出、無缺少 |
| 語意 | SAME=**253** DIFF=**70** BYTE_SAME=6 |
| 整類 SAME 的外層 | `BattleHealingRecovery`、`BattleHitReactionSuppressCatalog` |

本輪比對 JSON／Markdown 在工作機 `scratch/issue-67/b06-diff.json`（不進 git）。

## 剩餘 DIFF 分類（已驗證：皆 javac 不可達）

| 類型 | 約略筆數 | 說明 |
|---|---:|---|
| Icons1 `bump` 的 DUP vs ALOAD | ~15 外層 `activate`/`clear` | 候選在 PUTFIELD 前 DUP；javac 編成 `ASTORE` 區域變數再 `ALOAD`。方法已呼叫 `NativeSourceIcons1Epoch.bump`。 |
| record 額外 instance field | Cube／Event／Snapshot／Pulse／Ground$Pulse 等 | 候選 ASM 在 ACC_RECORD 類上加 0x10／0x90 `Object` 欄位與額外 ctor；javac 拒絕非 static record 欄位。 |
| Tournament `DUP2` vs `DSTORE` | 1 | `NativeNpcWeaponBreakDamage.sourceExact` 之後的 double 暫存。 |
| SpellCatalog 其餘方法體 | 20 | `<clinit>` 常數 ICONST_M1／0、部分 probability／damage／itemConsume 插樁；存取器本身已對齊。 |
| Cube／Quest／TimeAttack 方法體插樁 | ~12 | `advance` 改走額外 Event ctor、`aiAttackFor` 插 `NativeSourceNpc*`。 |
| innerClasses 屬性 | Companion、TimedStatus | CFR／javac 對 nest／Lookup 屬性編碼不同；內部 record 本體已 SAME。 |
| `Protection.applyTo` | 1 | 候選在 IDIV 處 POP divisor 再 `CombatRules.protectedDamage`；Java 寫成 `divisor==0 ? damage : protectedDamage(damage)`。 |

這些與 B 階段其他 asm-only 批相同：語意工具比的是 instruction 序列，不是「有沒有呼叫同一個 helper」。

## 推測

- 若要 323／323 全 SAME，需在 B-land 前對剩餘方法做 ASM 後處理（或接受 T020-close 的允許清單）。單靠 javac 無法發出 DUP-before-PUTFIELD 與 record 額外欄位。
- SpellCatalog `<clinit>` 的 ICONST_M1 vs ICONST_0 來自 src/main 靜態表與候選常數池的既有差異，不是本批漏搬 accessor。

## 未驗證

- 實機登入／戰鬥（本任務禁止啟動遊戲與正式 DB）。
- 單元測試／回歸（未動 `src/main`；B-land 統一處理）。
- 本批 staged 與 `src/main` 並存時的 Maven 全量編譯（設計規定本批只用候選 JAR 當 classpath）。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/Battle{AbilityStatus,BurningWeaponStatus,Companion,CounterMirrorStatus,CubeHarmonyEffects,CubeIgnitionEffects,CubeQuakeEffects,CubeShockEffects,DragonSkinStatus,ElementalFallDownStatus,EraseMagicStatus,FireBlessStatus,FireWeaponStatus,GroundEffects,GuardBrakeStatus,HealingRecovery,HitReactionSuppressCatalog,NaturesTouchStatus,QuestEncounter,ResistFearStatus,SpellCatalog,StormEyeStatus,StormShotStatus,TimeAttackEncounter,TimedStatus,TournamentNpcPhysicalDamage,WeightStatus,WindShackleStatus,WindShotStatus}.java`
- `docs/reports/issue-67.md`
- 分支：`task/67`

## 給下一棒的提示

- 不要把 staged 再搬進 `src/main`（那是 B-land）。
- 下一批 B07（Combat／Material）可與本批並行；本批剩餘 70 個 DIFF 不要用 stub 假 SAME。
- 若調度器堅持 B06 必須 323 SAME：需要 ASM weave 工具（對 bump 的 PUTFIELD 改 DUP、對 record 寫 extra field）。那超出本批「只改 staged Java」的範圍，應另開 code 任務，不要改 `docs/REPORT.md`。
- `BattleSpellCatalog` 很大；後續若重跑 accessor 插入，record 存取器必須 `public`，且 `implements X` 的 record 在 `)` 與 `{` 之間有介面子句。
