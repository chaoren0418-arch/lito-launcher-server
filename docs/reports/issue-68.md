# Issue #68 — T020-B07 戰鬥併入暫存（existing-combat-material，23 類）

標籤建議：`needs:claude`（20 筆 entry 寫不出與候選完全等價的 javac Java，交主控裁定）。

## 做了什麼

本批 B07：23 個外層類別（`Combat*`、`ForcedCombat`、`*MaterialAction`），驗收 56 個 class entry。做法是三方移植：以 `src/main` 現有原始碼為底，對照 CFR 正式版（`reference/base-0.6.71.jar`）與候選版（`reference/candidate-50635f7e.jar`）差異，把候選行為寫進 staged Java。未動 `src/main`、`pom.xml`、`docs/REPORT.md`；未覆蓋候選／正式 `.class` bytes。

移植重點（候選常見 ASM trampoline，未簡化 `c::player` 等行為）：

- 公開方法改呼叫 `Native*` helper，原本體改名 `native*Original*`／`native*Body`。
- `CombatWorldCommit$Plan` 增加 `NativeSourceCubeNpcQueue.Ticket`、`NativeSourcePoisonNpc.Ticket`、`NativeSourcePhysicalNpc.Ticket` permits。
- `CombatRules` 補 4 個 SKILL_KEYS、loader 走 `NativeCombatConfigOrigin`／`NativePetControls`／`NativeSourceOtherControls`／`NativeSourceSkillDefaults`。
- `CombatDatabase.load` 加 `NativeSkillTargetMetadata`／`NativePolymorphSource`／`NativeNpcPhysicalSource`／`NativeCancellationNpcRules` read+publish。
- `NativeResurrectionMaterialAction$Target` 新增（基線 ONLY_RIGHT）。
- `NativeAreaMaterialAction$Context` 補候選額外欄位、inherit 鏈、captureOutput trampoline 鏈；lambda 維持 `NativeSourceIndirectPlayerProof.player`，未改成 method reference。

暫存腳本（不進 git）：`scratch/issue-68/pipeline.sh`。

## 怎麼驗證

### 已驗證

編譯（JDK 21、`--release 17`，候選 JAR 在前、正式 JAR 在後；本環境無 `lib/*`，與先前批次相同）：

```text
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d scratch/issue-68/javac-out \
  merge-inputs/50635f7e/staged/local/lito381/server/*.java
```

| 項目 | 結果 |
|---|---|
| `java` | openjdk 21.0.12.1 |
| `javac` | javac 21.0.12.1 |
| exit | **0** |
| stderr | `Note: Some input files use unchecked or unsafe operations.` |
| 產出 entry | **56／56**，missing=[] extra=[] |

語意（T020d，left 為本次 javac 打包 `scratch/issue-68/left.jar`，right 為候選 JAR，`--classes` 為 B07 `verify_entries`）：

```text
SUMMARY SAME=36 DIFF=20 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=2 ALLOW_METHODS=0
```

清單內左側缺少 0、清單外多出 0。產物僅來自 staged Java，沒有用候選／正式 class bytes 覆蓋。

**SAME（36）**：`CombatAccess`（含 `$1`）、`CombatCompanionSpells${Attempt,Batch,Hit}`、`CombatDatabase${Skill,Snapshot}`、`CombatHealing`、`CombatNpcDebuffs$Focus`、`CombatPlayerDamage`、`CombatPlayerRecovery`、`CombatPoisonLifecycle` 除外層外的 14 個內部類、`CombatRules$1`／`$Snapshot`、`CombatSpecialBuffs`、`CombatWorldCommit${Attempt,Doll,Physical}`、`ForcedCombat$Intent`、`NativeAreaMaterialAction$Npc`、`NativeGroundMaterialAction`（含 `$Rejected`）、`NativeResurrectionMaterialAction$Target`。

### 推測

其餘 20 筆 DIFF 多半不是漏移植公開 API，而是 javac 無法重現候選 ASM 的編碼／合成名稱／InnerClasses。T020d 對 `lambda$xxx$N` 只正規化數字、**保留 enclosing method 名稱**，因此 ASM 把原方法改名後仍留下 `lambda$battle$0` 的 entry，和 javac 編出的 `lambda$nativeSceneOriginal*$0` 對不上。

### 待驗證

- 實機伺服器／登入器／遊戲／正式 DB。
- B-land 後 `mvn test` 與戰鬥回歸。
- 下列 20 筆在執行期是否已與候選等價（工具仍判 DIFF）。

## 未完成／交主控裁定（needs:claude）

目標是整類（含 `$` 內部類）與候選 SAME。下列最小差異寫不出等價 Java；**沒有**改用 class-bytes 覆蓋來通過驗收。

| entry | 第一處差異 | 理由 |
|---|---|---|
| `CombatCompanionSpells` | `lambda$nativeShockCompanionBody$#0` only left | 本體改名後 javac 把 lambda 掛在新方法名；候選 lambda 仍叫原方法名 |
| `NativeCancellationMaterialAction` | `lambda$battle$#0` only right | 同上（候選 trampoline 後仍留原名 lambda） |
| `NativeEarthBindMaterialAction` | `lambda$nativeSceneOriginalTarget$#0` only left | 同上 |
| `NativeNegativeMaterialAction` | `entangle` INVOKEDYNAMIC 目標名 | 同上 |
| `NativePoisonMaterialAction` | `lambda$nativeSourceArenaOriginalBattle$#0` only left | 同上 |
| `NativeResurrectionMaterialAction` | `lambda$nativeSceneOriginalWorld$#0` only left | 候選為 `lambda$world$2`（ASM 改名未改 lambda） |
| `NativeReturnNatureMaterialAction` | `lambda$captureOutput$#0` only right | 候選 lambda 仍掛 `captureOutput`／`cast` |
| `NativeTargetNegativeMaterialAction` | `lambda$cast$#0` only right | 同上 |
| `NativeWeaponBreakMaterialAction` | `lambda$battle$#0` only right | 同上 |
| `NativeReturnNatureMaterialAction$Target` | 欄位 `nativeSummonDeletionContext:Z` only right | record 額外實例欄位（另有 `nativeSummonOwnerContext`、`nativeSummonPublication`）javac 無法發出 ACC_RECORD + 非 component 欄位 |
| `NativePetMaterialAction` | `BIPUSH 36` vs `LDC Integer:36` | `--release 17` 對 int 36 發 BIPUSH；候選為 ASM `ldc` |
| `CombatDatabase` | extra InnerClasses（`NativePolymorphSource$Snapshot` 等） | 候選常數池有該巢狀型別的 Methodref，但 InnerClasses **沒列**；javac 一引用就會寫入 |
| `CombatPoisonLifecycle` | InnerClasses | 同上（`NativeCombatOrigin$Clock` 等） |
| `CombatWorldCommit$Plan` | InnerClasses | permits 三個 `Ticket` 後 javac InnerClasses 與 ASM 排序／集合不同 |
| `NativeAreaMaterialAction`／`$Context` | InnerClasses | Context 多欄位／多 ctor；候選對 `players`／`bindings`／`nativeSourceMagicSnapshot` 在 `this()` 之後寫 **final** 欄位（非法 Java） |
| `CombatNpcDebuffs` | `nativeMaterialControlledNegativeBody` ALOAD 4 vs 10 | 區域變數槽；候選 CFR 把 `focus` 再存成 `focus2` 並插入 `NativeSkillConsumptionBridges` |
| `CombatRules` | `load` ALOAD 5 vs 8 | 區域變數槽（多了 characterProperties／NativePetControls 區域） |
| `CombatWorldCommit` | `publish` try 標籤序 | `NativeCombatAttacker.owner` 包住後例外表編號不同 |
| `ForcedCombat` | `nativeSelectedObjectIntent` IF_ACMPEQ vs IF_ACMPNE | 布林短路編譯形狀 |

## 產出路徑

- staged Java：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`（本批 23 個 `.java`）
- 報告：`docs/reports/issue-68.md`
- 本機驗證產物（不進 git）：`scratch/issue-68/{javac-out,left.jar,b07-diff.json,b07-diff.md,logs/}`

## 給下一棒的提示

1. 主控（`needs:claude`）裁定這 20 筆：接受為 javac 不可達並在 B-land 豁免、或允許對 lambda／InnerClasses 做有限 ASM 後處理（本批依 #62 未做）。
2. 不要用候選 class bytes 覆蓋 staged 編譯產物來「湊 SAME」。
3. `NativeAreaMaterialAction$Context` 的 Commit／Inherited 建構子在候選位元組碼裡會對 final 欄位二次賦值；若要 SAME，只能 ASM，不能靠 javac。
4. B-land 才把 staged 搬進 `src/main`；本批未跑單元測試／回歸。
5. 未改 `docs/REPORT.md`。
