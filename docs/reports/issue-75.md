# Issue #75 / T020-A04 — src/main 對齊正式 0.6.71 暫存（battle-combat，25 類）

## 做了什麼

以 `src/main` 現有原始碼為底，對照 `reference/base-0.6.71.jar` 的 CFR／Vineflower 反編譯與 T020d 第一處差異，把正式版行為寫進暫存區。未改 `src/main`、`pom.xml`、`docs/REPORT.md`。驗收產物是 staged Java 經 JDK 17 `javac --release 17` 直接編成的 class，沒有覆蓋正式 JAR bytes。

`CombatCurseLifecycle$Target` 依主控裁定寫成 `@AsRecord(components = {id, source, account, name, phase, room, map, instance}) final class`，含 ASM 欄位 `nativeRoleOwner`，不寫 equals／hashCode／toString。#102 轉換器尚未合併，此 entry **不能宣告完成**。

從 `BattleAnimationCatalog.java` 拿掉同檔定義的 `BattleActionCodes`（正式 JAR 已有獨立 class），避免 tmp 多出清單外 entry。

## 產出路徑

`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 下本批 25 檔：

`BattleAnimationCatalog.java`、`BattleArmBreakerStatus.java`、`BattleArmorBreak.java`、`BattleClearMindStatus.java`、`BattleDragonEyeCatalog.java`、`BattleEarthBlessStatus.java`、`BattleFreezeEffects.java`、`BattleInvisibilityStatus.java`、`BattleIronSkinStatus.java`、`BattleJoyOfPain.java`、`BattleMortalBody.java`、`BattlePatienceStatus.java`、`BattlePetRaceCourseCatalog.java`、`BattlePhysicalDefense.java`、`BattlePhysicalProcBuff.java`、`BattlePolymorphCatalog.java`、`BattleRules.java`、`BattleShockStunEffects.java`、`BattleStrikerGale.java`、`BattleTurnUndead.java`、`BattleWeaponBreak.java`、`CombatCurseLifecycle.java`、`CombatMonsterCurseLifecycle.java`、`CombatNpcDamageQueue.java`、`CombatPoisonAtomic.java`

報告：`docs/reports/issue-75.md`

## 怎麼驗證

工作目錄：repo 根。須用 **JDK 17** 的 `javac`（本環境 `17.0.20.1`）。JDK 21 的 `javac --release 17` 會把 `BattleRules` 的 enum switch 編成 tableswitch，與正式 JAR 的 `$SwitchMap$…Mode` 不同。

`@AsRecord` 註解尚未進 `src/main`（#102）。編譯前在任意不進 git 的目錄放同套件 stub（本輪用 `scratch/issue-75/asrecord/`，不提交）：

```java
package local.lito381.server;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@interface AsRecord { String[] components(); }
```

```bash
SERVER="lito伺服器/LiTo381-Server"
STAGED="$SERVER/merge-inputs/formal-0.6.71/staged/local/lito381/server"
JAR="$SERVER/reference/base-0.6.71.jar"
python3 -c "import json; b=next(x for x in json.load(open('docs/combat-merge/a-batches.json'))['batches'] if x['id']=='A04'); open('scratch/issue-75/verify_entries.txt','w').write('\n'.join(b['verify_entries'])+'\n')"
JAVAC=/usr/lib/jvm/java-17-openjdk-amd64/bin/javac   # Windows: 改 PATH 中的 JDK 17 javac
"$JAVAC" --release 17 -encoding UTF-8 -d scratch/issue-75/asrecord-classes scratch/issue-75/asrecord/local/lito381/server/AsRecord.java
rm -rf scratch/issue-75/a04-classes && mkdir -p scratch/issue-75/a04-classes
"$JAVAC" --release 17 -encoding UTF-8 -cp "$JAR:scratch/issue-75/asrecord-classes" -d scratch/issue-75/a04-classes "$STAGED"/*.java
# Windows classpath 用 ;
jar cf scratch/issue-75/a04-left.jar -C scratch/issue-75/a04-classes .
bash "$SERVER/tools/semantic-diff/semantic-diff" \
  --left scratch/issue-75/a04-left.jar --right "$JAR" \
  --classes scratch/issue-75/verify_entries.txt \
  --json scratch/issue-75/diff.json --md scratch/issue-75/diff.md
```

**已驗證（本輪執行者回報）**

- `javac` exit 0（unchecked note 來自 `CombatMonsterCurseLifecycle`，與 src/main 相同）
- 產出剛好 86 個 class，與 `verify_entries` 一致：缺 0、多 0（`classSelection.missingLeft`／`extraLeft` 皆空）
- T020d vs `reference/base-0.6.71.jar`：**SAME=76／DIFF=10／ONLY_LEFT=0／ONLY_RIGHT=0／BYTE_SAME=14**
- 產物是 staged Java 直接編成，沒有覆蓋正式 `.class` bytes

基準（修補前 src/main 原樣編譯）：SAME=53 DIFF=31 ONLY_RIGHT=2 extra `BattleActionCodes`。

## 剩餘 10 個 DIFF（javac 不可達或等候 #102）

請主控加 `needs:claude`。未裁定前本批不能宣告「每個 entry 都 SAME」。

| entry | firstDiff | 理由 |
|---|---|---|
| `BattleArmBreakerStatus` | `meleeDamageModifier(J)I` insn[5] `SIPUSH 213` vs `LDC Integer:213` | javac 對 short 範圍整數常數發 SIPUSH；正式版為 LDC。Java 寫 `213` 無法改成 LDC。 |
| `BattleClearMindStatus` | `snapshot(J)` insn[54] `SIPUSH 137` vs `LDC Integer:137` | 同上（`CombatBuffLeases.value(..., 137, "WIS")`） |
| `BattleEarthBlessStatus` | `snapshot(J)` insn[51] `SIPUSH 159` vs `LDC Integer:159` | 同上 |
| `BattleIronSkinStatus` | `snapshot(J)` insn[51] `SIPUSH 168` vs `LDC Integer:168` | 同上 |
| `BattleDragonEyeCatalog$Eye` | `canOverwrite()Z` insn[2] `IFEQ` vs `ALOAD 0` | 正式版把 boolean 欄位當 int 傳進 `eyeField(I,Eye,String)`（ASM）。javac 不能把 boolean 當 int 傳，必須 `? 1 : 0`，因而多 IFEQ。語意等價。 |
| `BattlePolymorphCatalog` | `randomForm(I)` insn[2] `ASTORE 1` vs `DUP` | 正式版 `selectOrNull` 結果 DUP／ifnull／areturn；javac 一定先 ASTORE。語意等價。 |
| `CombatCurseLifecycle$Removal` | `drain(Z)V` TRY 標籤 `L55–L57` vs `L57–L59` | 方法前段 bytecode 一致；例外表後半 from/to 因 try-finally 標號偏移。不是漏搬邏輯。 |
| `CombatCurseLifecycle$Target` | class access `0x30` vs `0x10030` | 差在 `ACC_RECORD`。已寫 `@AsRecord`＋`nativeRoleOwner`；等 #102 轉換器。 |
| `CombatMonsterCurseLifecycle` | `lambda$submitDamage$0(...)` only right | `submitDamage` 本體已是 `NativeNpcLifetime.submit`（與正式 SAME）。正式 JAR 另留 ASM 替換前的合成 lambda（`submitDamage(task, retry+1)`），javac 無法在不改 `submitDamage` 本體的情況下發同名殘留方法。 |
| `CombatPoisonAtomic` | InnerClasses 多 `NativeCombatOrigin$Install` | 正式版用 ASM 接上 `enterNpc`／`leaveInstall`，未更新 InnerClasses。javac 只要寫到該巢狀型就會列入。方法語意已對齊 `NativeCombatOrigin`。 |

## 未驗證什麼

- 實機、登入器、單元測試、回歸（未動 `src/main`；A-land 才跑）
- #102 轉換器跑完後 `CombatCurseLifecycle$Target` 是否 SAME
- JDK 21 `--release 17`（已知 `BattleRules` switch-map 會多 DIFF，不作為本批驗收編譯器）

## 已驗證／推測／待驗證

- **已驗證**：86／86 entry 對得上清單；javac 17 exit 0；76 SAME／10 DIFF；沒有覆蓋 class bytes。
- **推測**：剩餘 10 DIFF 皆 javac 發不出的正式 JAR 形狀（常數型別、InnerClasses、合成 lambda、record 旗標），不是漏搬 Java 可寫行為。
- **待驗證**：#102 後 Target；A-land 測試。

## 給下一棒的提示

- 不要把 staged 搬進 `src/main`（A-land 才做）。
- `CombatCurseLifecycle$Target` 等 #102 合併後：javac → 轉換器 CLI → semantic-diff。這是唯一核准的後處理。
- 驗收請用 JDK 17 javac，不要用 JDK 21 的 `--release 17`。
- 其餘 9 個 DIFF 請主控裁定：接受為 javac 形狀差，或允許（目前禁止的）ASM 後處理。
- A05／A06 可沿用本批手法：`CombatRules.replace*`／`CombatBuffLeases.value`／`Native*` 包裝、拿掉同檔多出來的頂層 class、enum switch 用 JDK 17 編。
