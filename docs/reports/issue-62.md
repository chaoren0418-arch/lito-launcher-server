# Issue #62 / T020-B01 — 戰鬥併入暫存（new-asm，17 類）

## 結論

B01 的 17 個外層類別已放進暫存區，標頭 `// DECOMPILED from candidate-50635f7e.jar, TODO review`，**javac 成功**，`verify_entries` **22 個 class 無缺漏、無多出**。T020d semantic-diff 對候選 JAR：**12 SAME／10 DIFF**。剩餘 10 筆都是「原始 Java + 後續 ASM splice」在 **javac 無法發出的 bytecode 形狀**（record 額外欄位、indy Handle 改寫、lambda 合成名、checked exception 無 try、DUP 取代 ALOAD、try 標籤序）。**尚未達到「每個 entry 都 SAME」的驗收。**

## 做了什麼

範圍只新增 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 下 B01 的 17 個 `.java`，以及本報告。未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

來源不是純 CFR 終態（CFR／Vineflower 反編譯含 splice，但 record 額外欄位非法、lambda capture 遺失，約 30 處編譯錯）。改以 `tools/combat-continuation/integration-lifecycle/` 的原始 Java 為底，把 **能用 Java 表達** 的 ASM splice 寫進原始碼，再以 Temurin 17 編譯。

已套用、且對應 entry 已 SAME 的 splice 包括：

| 類別 | 源碼層 splice |
|---|---|
| `NativePairedCastSafety` | `run` 改名 `nativeSourcePairedScopeOriginalRun`，wrapper 轉 `NativeSourcePairedScope.run` |
| `NativeCompanionRefScene` | `capture` 本體改名，wrapper 轉 `NativeSourceNpcOrigins.ref` |
| `NativeSourceBatchTerminal` | `finish` 尾呼叫 `NativeSourceSelfFinalFrame.release` |
| `NativeSourcePendingAdmission` | `offer` 改名，wrapper 轉 `NativeSourceDamageCapacity.offer` |
| `NativeSummonTerminalLifecycle` | `combatDamage` 兩處 `Binding.capture` → `NativeCompanionReflectionScene.capture` |
| `NativeSourceCubePlacement` | `prepare` 每個回傳包 `NativeSourceCubeTemplate.ready`；`cadence` 用 `source?1000L:1000L`（保留 ASM 三元形）；`publish` 末呼叫 `NativeSourceCubeVisibility.seal` |
| `NativeSourceDollSkills`／`$View` | `apply` 改呼叫 `NativeSourceDollSlow.slow`；View 加 `nativeSourceDollOutputAudience`；audience／send 改走 `NativeSourceDollAudience`／`NativeSourceDollOutput` |
| `NativeSourceDollSlow` | `allowFrame` 改名＋wrapper 轉 `NativeSourceDollSlowWire.allowFrame`；`slow` 封包走 `NativeSourceDollSlowWire.packet` |
| `NativeSourceDollAccepted$Pending` | 欄位 `nativeSourceDollCast` + `stamp`／`valid` |
| `NativeSourceDollNpc$Scope` | 同上 |
| `NativeSourceHealingCast$Cast` | `volatile Object nativeSourceControlledHealing`；`close` 清欄位 |

## 怎麼驗證（已驗證）

候選 JAR SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`（與 T020f 入庫值相同）。

JDK：Temurin 17.0.21（`/tmp/jdk17/bin/javac`）。

```bash
cd lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d /tmp/b01-classes \
  merge-inputs/50635f7e/staged/local/lito381/server/*.java
# 輸出：Note: Some input files use unchecked or unsafe operations.
#       Note: Recompile with -Xlint:unchecked for details.
# exit 0

jar cf /tmp/b01-left.jar -C /tmp/b01-classes .
bash tools/semantic-diff/semantic-diff \
  --left /tmp/b01-left.jar \
  --right reference/candidate-50635f7e.jar \
  --classes <B01 verify_entries 22 行> \
  --json scratch/b01-out/result.json --md scratch/b01-out/result.md
```

本次實測（`scratch/b01-out/result.json`）：

```
SUMMARY SAME=12 DIFF=10 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0
classSelection.missingLeft []
classSelection.extraLeft []
```

**SAME（12）**：`NativeCompanionRefScene`、`NativePairedCastSafety`、`NativeSourceBatchTerminal`、`NativeSourceCubePlacement`、`NativeSourceDollAccepted$Pending`、`NativeSourceDollNpc$Scope`、`NativeSourceDollSkills`、`NativeSourceDollSkills$View`、`NativeSourceDollSlow`、`NativeSourceHealingCast$Cast`、`NativeSourcePendingAdmission`、`NativeSummonTerminalLifecycle`。

**DIFF（10）** 見下節。

## 剩餘 DIFF（已驗證原因；javac 無法 SAME）

B01 分類是 new-asm：原始 `.java` 編成 class 後，integration-lifecycle 的 `*Methods.java` 再用 ASM 改 bytecode。下列差異對過候選 javap／T020d firstDiff 與對應 splice 腳本。

| Entry | firstDiff | 原因（已驗證） |
|---|---|---|
| `NativeBlindSceneAction` | `companion` INVOKEDYNAMIC Handle | `SourceIndirectPlayerProofMethods` 把 `c::player` 的 impl Handle 改成 `NativeSourceIndirectPlayerProof.player(Context,ClientSession)`。Java 沒有「綁定第一參數的靜態方法參照」語法；寫 lambda 會多出 `lambda$` 方法。 |
| `NativeCompanionForestArea` | `apply` 同上 | 同上 |
| `NativeCompanionWindAction` | `publish` 同上 | 同上 |
| `NativeSourceCubePlacement$Draft` | 欄位 `nativeSourceCubeTemplate` only right | `SourceCubeTemplateMethods` 在 **record** 上注入非 component 的 `final Object` 欄位，並在 `<init>` 末呼叫 `NativeSourceCubeTemplate.freeze`。Java 17 record 禁止額外 instance 欄位。 |
| `NativeSourceDollAccepted` | `physical` `ALOAD` vs `DUP` | `SourceAcceptedPlayersMethods` 在既有 `Actor.life()` 前插入 `DUP`，再把 `new Member(...)` 改成 `fromActor`。javac 對同一區域變數發兩次 `ALOAD`，不發 `DUP`。 |
| `NativeSourceDollNpc` | `lambda$nativeSourcePhysicalOriginalNatural$#0` only left | ASM 先以方法名 `natural` 編譯（lambda 名 `lambda$natural$N`），再把方法改名。javac 若本體就叫 `nativeSourcePhysicalOriginalNatural`，合成名會帶新方法名；T020d 依 `lambda$方法名$` 前綴正規化，對不上。 |
| `NativeSourceHealingCast` | `run` `ALOAD 7` vs `ALOAD 11` | `SourceControlledHealingMethods` 在 **owned 參數已推上 stack 之後**、`INVOKESTATIC owned` 之前插入 `capture`。Java 無法在「引數求值」與「呼叫」之間插入陳述。 |
| `NativeWeaponBreakTargetLifecycle` | `captureOutput` tryCatch length 1 vs 0 | 候選在無 `throws`、無 try 的方法裡直接呼叫 `NativeSourceWeaponFinalFrame.capture`（該方法 `throws IOException`）。合法 Java 必須 catch 或宣告 throws，前者多一個 try、後者 exceptions 表不同。 |
| `NativeWeaponBreakBattleCompanion` | `innerClasses` 多 `NativeAreaMaterialAction$Context` | `nativeSourceWeaponFirstPrepare` 由 ASM copy 產生，**不更新** innerClasses。javac 只要方法描述子提到 `$Context` 就會寫入 innerClasses。 |
| `NativeControlledNegativeMaterial` | `cast` TRY 標籤 `L9–L11` vs `L11–L13` | 此類沒有對自身的方法 rename splice；差異是 Microsoft JDK 17.0.17（原始建置）與 Temurin 17.0.21 的例外表／標籤配置。 |

**推測**：若在 javac 之後重放上述 `*Methods.patch`（或對這 10 個 entry 做等價 ASM），semantic-diff 可到 22 SAME。這一步不在本批允許的「只 javac staged `.java`」驗收指令內。

## 未驗證／待驗證

- 實機登入／戰鬥（本批規定不啟動遊戲、不連正式 DB）。
- 單元測試／回歸（未動 `src/main`，B-land 再跑）。
- 用原始 Windows `jdk-17.0.17.10-hotspot` 重編是否能消掉 `NativeControlledNegativeMaterial` 的 try 標籤差。
- 後 javac ASM 重放是否真能 22 SAME（推測，待下一棒）。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/Native{BlindSceneAction,CompanionForestArea,CompanionRefScene,CompanionWindAction,ControlledNegativeMaterial,PairedCastSafety,SourceBatchTerminal,SourceCubePlacement,SourceDollAccepted,SourceDollNpc,SourceDollSkills,SourceDollSlow,SourceHealingCast,SourcePendingAdmission,SummonTerminalLifecycle,WeaponBreakBattleCompanion,WeaponBreakTargetLifecycle}.java`
- `docs/reports/issue-62.md`
- 本機驗證產物（不進 git）：`scratch/b01-out/{left.jar,result.json,result.md,javac.log}`

## 給下一棒的提示

1. **不要再從 CFR／VF 重頭反編譯**；暫存區已是「原始 Java + 可表達 splice」，12 個 entry 已 SAME。
2. 要滿足「22 個 SAME」，需要 **javac 之後的 ASM 後處理**（重放 `SourceIndirectPlayerProofMethods`、`SourceCubeTemplateMethods` 的 Draft 欄位、`SourceAcceptedPlayersMethods` 的 DUP、`SourceControlledHealingMethods` 的 capture 插入、`SourcePhysicalNpcMethods` 的 rename（保留 `lambda$natural$`）、以及 captureOutput 的無 try 呼叫）。建議新開 code 任務：scratch 腳本對本批 class 做 post-javac splice，驗證 22 SAME；**或** 由 Claude 裁定 B01 驗收改為「javac 成功 + Java 可表達部分 SAME + 其餘以 ASM 腳本重放」。
3. 調度器若只跑 javac + semantic-diff、不跑 ASM，會看到本報告的 10 DIFF，這不是漏檔。
4. 不要改 `docs/REPORT.md`。B02 起是完整 Java，不應再踩 B01 這類 record／indy 硬點。
