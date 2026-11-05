# Issue #75：A04 正式 0.6.71 暫存驗收通過

## 結論／做了什麼

2026-11-06：**83 SAME／1 SAME_EFFECTS／1 SAME_PATHS／1 DEVIATION，DIFF／ONLY_LEFT／ONLY_RIGHT 均為 0**。25 份 staged Java 編譯成功，`@AsRecord` 轉換 1 類，產物恰為 86 個 entry，missingLeft／extraLeft 與獨立路徑集合檢查都為空。通過本批暫存驗收，沒有部署或搬入 src/main。

依 [主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/75#issuecomment-6657773411)，在既有 `task/75` 合併最新 `origin/main`，沒有 checkout／建分支。來源起點 `0d8cd8a`，main 為 `c72453957ebe47e00ad024e874bbe31859457799`，合併提交 `3111907165a220fb164254b360cd43996cd3fa67`，含 #174、#176。本輪只直接修改本報告與 `docs/combat-merge/deviations.txt`／`deviations.md`；`docs/REPORT.md` 等變化只來自必要 main 合併。

- 保留全部 A04 staged Java、`CombatCurseLifecycle$Target` 的 `@AsRecord`、前輪修正與 drain 的 effects 核准。本輪沒有修改 Java、比較器或建置設定。
- PoisonAtomic 的兩個 Scope overload 由預設逐路徑比對通過，沒有新增 effects／deviation，也沒有改成一般 finally。
- `BattleDragonEyeCatalog$Eye#canOverwrite()Z` 已完成 javap 值域分析，依裁定新增唯一 `method-diff`；其餘 Eye 內容仍由工具驗收。

## 怎麼驗證／已驗證

Microsoft OpenJDK 17.0.17，ASM 9.7；從合併後的 tools 原始碼重新編譯 semantic-diff 與 AsRecord。既有 Maven cache 唯讀取用 asm／asm-tree，asm-analysis 在 worktree target；三者由腳本驗 SHA-256。沒有從正式 JAR 複製 class bytes 覆蓋產物。

1. 從 `a-batches.json` 取 A04 的 25 類與 86 個 verify_entries，獨立編譯 AsRecord annotation。
2. `javac --release 17 -encoding UTF-8 -cp <formal.jar>;<annotation>;<lib/*> -d <classes> <25 staged files>`，退出 0；既有 CombatMonsterCurseLifecycle unchecked 診斷保留。
3. 呼叫 tools/as-record CLI：`scanned=86 converted=1`，退出 0。只打包這 86 個 class，ZIP entry 時間固定為 1980-01-01。
4. semantic-diff 使用 `--left <a04.jar> --right <formal.jar> --left-classpath <formal.jar> --classes <entries>`，以及裁切到 A04 的 `--effects-methods`、`--deviations`。前者只有既有 drain，後者只有本輪 canOverwrite。其他核准清單沒有本批適用項，不啟用 allow-right／layout。
5. 檢查 JSON 全部 86 個 entry 的狀態都屬於 SAME／SAME_EFFECTS／SAME_PATHS／DEVIATION，並獨立檢查 classes 路徑集合。工具 exit 0 本身不當成驗收通過。

| 模式 | SAME | SAME_EFFECTS | SAME_PATHS | DEVIATION | DIFF |
|---|---:|---:|---:|---:|---:|
| 前輪工具（歷史報告） | 83 | 1 | 0 | 0 | 2 |
| 新工具＋既有 effects，未加 deviation | 83 | 1 | 1 | 0 | 1 |
| 本輪正式核准清單 | 83 | 1 | 1 | 1 | 0 |

最終原始摘要（編譯、轉換、比較與驗收 wrapper 均 exit 0）：

```text
as-record: scanned=86 converted=1
SUMMARY SAME=83 SAME_EFFECTS=1 SAME_PATHS=1 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=13 ALLOW_METHODS=0
classSelection: {"missingLeft": [], "extraLeft": []}
ENTRY SET expected=86 actual=86 missing=[] extra=[]
ACCEPTANCE PASS
```

已從本報告取出兩份內嵌腳本實際重跑，編譯、轉換、比較、驗收 wrapper 與 javap 稽核皆退出 0；86-entry 計數、JAR／class-content 雜湊一致。`git diff --check` 通過。

### PoisonAtomic 的逐路徑證據

`npc(..., Scope)` 為 `SAME_PATHS: path multisets equal (26)`；`player(..., Scope)` 為 `SAME_PATHS: path multisets equal (29)`。這是同一個 class 的兩個方法，因此類別計數為 1 SAME_PATHS。

新版 `PathComparison.Proof` 在列舉前檢查全部一般 CFG 分量無 cycle；成功比對有序呼叫及引數、分支走向、monitor 進出、leaveInstall、正常／例外出口，以及各可能拋例外指令涵蓋的 handler 型別與優先序。比較的是含重複次數的路徑多重集合。成功結果證明兩個方法滿足本次工具的無迴圈前提；只對已證明同一 lock 成對取得的標準 self-covering monitor handler 忽略自身例外邊。沒有將一般 exception cycle 當成無迴圈。

正常路徑 leaveInstall 仍受原本外層 catch 保護；不把有條件核准誤解成可以改成 finally。這是靜態有界路徑證明，A-land 執行期測試仍待補。

### canOverwrite 的每個 IRETURN 值域

完整證明已寫入 [deviations.md](../combat-merge/deviations.md)，下方附實際 javap 指令。`eyeField` 有三個回傳位置，**不是所有用途都只回傳 boolean**：

| offset | 一般輸入值域（保守） | 本 consumer 的可達值域 |
|---:|---|---|
| 10 | 第 0 個 int 參數 | `{0,1}`，來自 Z 欄位 |
| 42 | ability 回傳的任意 int | 空集合；不可修改 map 沒有 canOverwrite key |
| 390 | 七個 int accessor 或 ICONST_0／ICONST_1 | `{0,1}`，固定字串只走 case 7 的顯式 0／1 分支 |

正式 consumer 的 GETFIELD Z 引數與 staged 的 `? 1 : 0` 一致；回傳值 `{0,1}` 經 `!= 0` 與 [JVMS 17 ireturn 的最低位元規則](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-6.html#jvms-6.5.ireturn) 相同。因此符合裁定條件，另列 DEVIATION，不混入 SAME。若 producer／map 變更，必須重做此證明。

## 未驗證什麼／推測

單元測試／回歸依本批規定不適用，留到 #78 A-land。未啟動遊戲、登入器、伺服器或 DB；未讀寫機密設定、封存、玩家實表與正式資源。未以 CI 全綠替代本次 86-entry 驗收。沒有尚待裁定的本批差異，也沒有把未執行的實機行為列為已驗證。

推測：javac 的暫存與出口配置造成 PoisonAtomic 的靜態形狀差；本次驗收依據為實際 SAME_PATHS 證明，不依靠此成因推測。

## 產出路徑／給下一棒的提示

- `docs/reports/issue-75.md`：本報告與完整重現腳本。
- `docs/combat-merge/deviations.txt`／`deviations.md`：新增 Eye 精確 method-diff 與 javap 證明。
- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`：原有 A04 25 類維持不變。
- 未提交生成檔：`scratch/issue-75/` 的驗收 JSON／Markdown／javap／腳本；`lito伺服器/LiTo381-Server/target/issue-75/` 的 classes／a04.jar／工具／依賴，以及 tools/as-record/target。

下一棒交 Claude 審查本批，經使用者合併後由 #78 A-land 接手。**A-land 必補 `CombatPoisonAtomic` 的 `leaveInstall` 拋例外時 monitor 必須釋放的測試**，並核對正式版與移植版的例外傳播／清理次數。不可因本批 SAME_PATHS 而取消。沒有需要追加 needs:claude 的失敗分支。

## 輸入與輸出雜湊

正式 JAR：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

重建 a04.jar：`b38e15c5bf978981a53b4f5bf729b733b0fc4bfe545d1667fcabaef4dc7a3f2b`。

class-content：`aef7fbccd5ca3356e0da7096e8ca081c84534596d7761bb2240a6d3a2cdbbd80`（entry 排序後串接 UTF-8 名稱、NUL、class bytes）。兩者與前輪完全相同，確認 Java／產物沒有改動。

| 輸入 | SHA-256 |
|---|---|
| BattleAnimationCatalog.java | `1c348553a0ca7c2d7fd5fcc48c44d04f23cbbf04be9db66204ee1d899300ba8c` |
| BattleArmBreakerStatus.java | `6b51a037b0001ee185fa6cf139e9b18b7eae229b05c01f872f4d53d09c3725dd` |
| BattleArmorBreak.java | `8cc1158d6681072db0207ca2505b5e206bdef1a95236b1873332595a23a59297` |
| BattleClearMindStatus.java | `aa99549aa2e80a11e274a0d2162262e51127375eb175705ebcb90453d2777c17` |
| BattleDragonEyeCatalog.java | `8763d6a02a9b634254ca7bfe02824828856ac74a3672e13481ca2248aaa9698b` |
| BattleEarthBlessStatus.java | `2a203a17bd456b77bb170c47d15d68433eb6d88b0d7a83ccc25e730c908385b8` |
| BattleFreezeEffects.java | `ece006e9ebeaec12b89bda8af45bbeb9bac6627d792364305e7197e763c2cddb` |
| BattleInvisibilityStatus.java | `f09fabc5e974124101a51a30f4b5404f85552f48460a60208f79f78c6a11d149` |
| BattleIronSkinStatus.java | `820199a8b723abfbcccd5df84acec6b04bf63f16fdeb57d215a3bb4a8348407c` |
| BattleJoyOfPain.java | `fce4531a4ec4e9246b53ea14eb955a0dd382f0a6dcbe4ef23d668408a6a3a1a9` |
| BattleMortalBody.java | `b57810c00babc8fc0db35c595c15d2442d82044cb01d0c76aaefb2fc2f260593` |
| BattlePatienceStatus.java | `678fc5d7309bb93afbbdfcb2e40a1ec40218bae767224479d365aa86206c5b29` |
| BattlePetRaceCourseCatalog.java | `2643d6d4c152a5743a52726c1d288ff1aa530c22e6e13fb4fb514c4d1e2e178d` |
| BattlePhysicalDefense.java | `94793b280dbf610e5e3900ff40d3b3af5cdfdf26d4894fa23721c0675bcf77ca` |
| BattlePhysicalProcBuff.java | `7e1719bb3fa630674130e5d41077cc3e47b0b2aacb8e375ba751db938792928f` |
| BattlePolymorphCatalog.java | `62945bad014fde12493de91790e0032ed6f3a0b1ea4550bff21588e981489365` |
| BattleRules.java | `18653d9e823f18e62b9163feead143f59ab31156c24a2dbd17fee27c4a666c97` |
| BattleShockStunEffects.java | `8842131374ce50889b18a1c030cf9b6a8a9d456df645a32f7eecf15f8216abd6` |
| BattleStrikerGale.java | `9b7dd6becf943a252f086a86739a395a4c837f6465faf6bcca9f8670bb26f0f9` |
| BattleTurnUndead.java | `b5faf3e19ef4e84d90b3f5bc152b397a22667eb7eef313a2ae2a2cc6344d0f8b` |
| BattleWeaponBreak.java | `468f05fdca64e1f56c8ad7471135dfc4c3620ebff3e6670f141d002724b3fcec` |
| CombatCurseLifecycle.java | `5f42dbae2764204a0e76b1049416517e44425e785836d1bf99aa2a205e592413` |
| CombatMonsterCurseLifecycle.java | `6a6a84a1f300a4dd1afd9de670ae99c2e5af56f08bc54fce0d92bf6e50ed94dc` |
| CombatNpcDamageQueue.java | `97ef4e2d278462a50c1f2a16c2a46e496c7ebf66cd30d51a9e92510da68b8ea2` |
| CombatPoisonAtomic.java | `f9d64610e0aaaa6ebd60352e93cfbec70269d67bf029fbe5023d119bef270237` |
| a-batches.json | `ac1c17ec1a446564c6fb912235ecd9222d06bef1a3a91cdcb3836fda158ad65d` |
| effects-approved.txt | `b48431e80e508f795e2dd9c26c316a335e2149f82bbb4269326f132e85953e58` |
| deviations.txt | `f344071ad84328de84570267e076dde82d4d116f467a20ba0b73867c07ca2d5c` |

## 86-entry 結果

共同前綴 `local/lito381/server/`，下列名稱均為 class entry。

| Entry | 結果 |
|---|---|
| BattleAnimationCatalog.class | SAME |
| BattleAnimationCatalog$Animation.class | SAME |
| BattleAnimationCatalog$Holder.class | SAME |
| BattleArmBreakerStatus.class | SAME |
| BattleArmBreakerStatus$Activation.class | SAME |
| BattleArmorBreak.class | SAME |
| BattleArmorBreak$Candidate.class | SAME |
| BattleClearMindStatus.class | SAME |
| BattleClearMindStatus$Snapshot.class | SAME |
| BattleDragonEyeCatalog.class | SAME |
| BattleDragonEyeCatalog$Ability.class | SAME |
| BattleDragonEyeCatalog$Eye.class | DEVIATION |
| BattleEarthBlessStatus.class | SAME |
| BattleEarthBlessStatus$Snapshot.class | SAME |
| BattleFreezeEffects.class | SAME |
| BattleFreezeEffects$Effect.class | SAME |
| BattleFreezeEffects$Placement.class | SAME |
| BattleInvisibilityStatus.class | SAME |
| BattleInvisibilityStatus$Lease.class | SAME |
| BattleInvisibilityStatus$Snapshot.class | SAME |
| BattleIronSkinStatus.class | SAME |
| BattleIronSkinStatus$Snapshot.class | SAME |
| BattleJoyOfPain.class | SAME |
| BattleMortalBody.class | SAME |
| BattleMortalBody$Decision.class | SAME |
| BattlePatienceStatus.class | SAME |
| BattlePatienceStatus$Activation.class | SAME |
| BattlePetRaceCourseCatalog.class | SAME |
| BattlePetRaceCourseCatalog$Course.class | SAME |
| BattlePetRaceCourseCatalog$Panel.class | SAME |
| BattlePetRaceCourseCatalog$PanelTransition.class | SAME |
| BattlePetRaceCourseCatalog$PanelType.class | SAME |
| BattlePetRaceCourseCatalog$Runner.class | SAME |
| BattlePetRaceCourseCatalog$State.class | SAME |
| BattlePetRaceCourseCatalog$Transition.class | SAME |
| BattlePetRaceCourseCatalog$Zone.class | SAME |
| BattlePhysicalDefense.class | SAME |
| BattlePhysicalDefense$NpcOutcome.class | SAME |
| BattlePhysicalDefense$PlayerOutcome.class | SAME |
| BattlePhysicalProcBuff.class | SAME |
| BattlePhysicalProcBuff$Outcome.class | SAME |
| BattlePolymorphCatalog.class | SAME |
| BattlePolymorphCatalog$Catalog.class | SAME |
| BattlePolymorphCatalog$Form.class | SAME |
| BattleRules.class | SAME |
| BattleRules$1.class | SAME |
| BattleRules$MatchState.class | SAME |
| BattleRules$Mode.class | SAME |
| BattleRules$Phase.class | SAME |
| BattleRules$Resolution.class | SAME |
| BattleRules$RuleSet.class | SAME |
| BattleRules$Snapshot.class | SAME |
| BattleShockStunEffects.class | SAME |
| BattleShockStunEffects$Effect.class | SAME |
| BattleShockStunEffects$Placement.class | SAME |
| BattleStrikerGale.class | SAME |
| BattleTurnUndead.class | SAME |
| BattleWeaponBreak.class | SAME |
| CombatCurseLifecycle.class | SAME |
| CombatCurseLifecycle$Context.class | SAME |
| CombatCurseLifecycle$Curse.class | SAME |
| CombatCurseLifecycle$Palette.class | SAME |
| CombatCurseLifecycle$Release.class | SAME |
| CombatCurseLifecycle$Removal.class | SAME_EFFECTS |
| CombatCurseLifecycle$Scope.class | SAME |
| CombatCurseLifecycle$Snapshot.class | SAME |
| CombatCurseLifecycle$Target.class | SAME |
| CombatMonsterCurseLifecycle.class | SAME |
| CombatMonsterCurseLifecycle$Actor.class | SAME |
| CombatMonsterCurseLifecycle$Context.class | SAME |
| CombatMonsterCurseLifecycle$Curse.class | SAME |
| CombatMonsterCurseLifecycle$Damage.class | SAME |
| CombatMonsterCurseLifecycle$DamageDispatch.class | SAME |
| CombatMonsterCurseLifecycle$DamageTask.class | SAME |
| CombatMonsterCurseLifecycle$DamageUpdates.class | SAME |
| CombatMonsterCurseLifecycle$Delivery.class | SAME |
| CombatMonsterCurseLifecycle$Paralysis.class | SAME |
| CombatMonsterCurseLifecycle$Recipient.class | SAME |
| CombatMonsterCurseLifecycle$Removal.class | SAME |
| CombatMonsterCurseLifecycle$Scope.class | SAME |
| CombatMonsterCurseLifecycle$Token.class | SAME |
| CombatNpcDamageQueue.class | SAME |
| CombatNpcDamageQueue$Event.class | SAME |
| CombatNpcDamageQueue$Queue.class | SAME |
| CombatPoisonAtomic.class | SAME_PATHS |
| CombatPoisonAtomic$NpcState.class | SAME |

## 非 SAME 方法原始結果

```json
[
  {
    "name": "local/lito381/server/BattleDragonEyeCatalog$Eye",
    "status": "DEVIATION",
    "byteSame": false,
    "allowMethodsApplied": false,
    "methodResults": [
      {
        "method": "canOverwrite()Z",
        "status": "DEVIATION",
        "detail": "method canOverwrite()Z insn[2]: left=IFEQ L5 right=ALOAD 0\nLEFT\n()Z\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 0, GETFIELD local/lito381/server/BattleDragonEyeCatalog$Eye.canOverwrite:Z, IFEQ L5, CONST Integer:1, GOTO L6, CONST Integer:0, ALOAD 0, CONST str:\"canOverwrite\", INVOKESTATIC local/lito381/server/CombatSpecialBuffs.eyeField(ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I, IFEQ L12, CONST Integer:1, GOTO L13, CONST Integer:0, IRETURN]\nRIGHT\n()Z\n0x1\n\n\n||||pvis=|pinv=|lvtvis=|lvtinv=\n\n[]\n[ALOAD 0, GETFIELD local/lito381/server/BattleDragonEyeCatalog$Eye.canOverwrite:Z, ALOAD 0, CONST str:\"canOverwrite\", INVOKESTATIC local/lito381/server/CombatSpecialBuffs.eyeField(ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I, IRETURN]"
      }
    ],
    "effectsMethods": [],
    "approvedMethods": []
  },
  {
    "name": "local/lito381/server/CombatCurseLifecycle$Removal",
    "status": "SAME_EFFECTS",
    "byteSame": false,
    "allowMethodsApplied": false,
    "methodResults": [],
    "effectsMethods": [
      {
        "method": "drain(Z)V",
        "status": "SAME_EFFECTS",
        "detail": null
      }
    ],
    "approvedMethods": []
  },
  {
    "name": "local/lito381/server/CombatPoisonAtomic",
    "status": "SAME_PATHS",
    "byteSame": false,
    "allowMethodsApplied": false,
    "methodResults": [
      {
        "method": "npc(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;",
        "status": "SAME_PATHS",
        "detail": "path multisets equal (26)"
      },
      {
        "method": "player(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Z",
        "status": "SAME_PATHS",
        "detail": "path multisets equal (29)"
      }
    ],
    "effectsMethods": [],
    "approvedMethods": []
  }
]
```

## 保留的 drain 完整正規化差異

前輪新增 effects 核准的證據保留如下；本輪 Java 與該條目未變，重驗仍為 SAME_EFFECTS。差異為送出 receiver 在 stack 暫存與分支表示；其他方法內容相同。

```diff
--- staged/CombatCurseLifecycle$Removal.txt
+++ formal/CombatCurseLifecycle$Removal.txt
@@ -535,10 +535,11 @@
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/CombatCurseLifecycle$Release
 ASTORE 21
+ALOAD 21
 ALOAD 0
 GETFIELD local/lito381/server/CombatCurseLifecycle$Removal.context:Llocal/lito381/server/CombatCurseLifecycle$Context;
 INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Context.valid()Z
-IFEQ L61
+IFEQ L62
 ALOAD 0
 GETFIELD local/lito381/server/CombatCurseLifecycle$Removal.lease:Llocal/lito381/server/CombatCurseLifecycle$Curse;
 GETFIELD local/lito381/server/CombatCurseLifecycle$Curse.target:Llocal/lito381/server/CombatCurseLifecycle$Target;
@@ -546,11 +547,11 @@
 GETFIELD local/lito381/server/CombatCurseLifecycle$Removal.context:Llocal/lito381/server/CombatCurseLifecycle$Context;
 GETFIELD local/lito381/server/CombatCurseLifecycle$Context.pc:Llocal/lito381/server/ClientSession;
 INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Target.valid(Llocal/lito381/server/ClientSession;)Z
-IFNE L62
-GOTO L61
+IFEQ L62
+INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Release.send()V
+GOTO L63
 LABEL L62
-ALOAD 21
-INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Release.send()V
+POP
 LABEL L63
 GOTO L61
 LABEL L64

--- staged/CombatCurseLifecycle$Target.txt
+++ formal/CombatCurseLifecycle$Target.txt
@@ -220,12 +220,10 @@
 CONST str:"classicWorldInstanceId"
 INVOKESTATIC local/lito381/server/CombatCurseLifecycle.number(Ljava/lang/Object;Ljava/lang/String;)J
 INVOKESPECIAL local/lito381/server/CombatCurseLifecycle$Target.<init>(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;JJJ)V
-ASTORE 1
-ALOAD 1
+DUP
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.owner(Llocal/lito381/server/ClientSession;)Ljava/lang/Object;
 PUTFIELD local/lito381/server/CombatCurseLifecycle$Target.nativeRoleOwner:Ljava/lang/Object;
-ALOAD 1
 ARETURN
 END METHOD
 METHOD toString()Ljava/lang/String;
```

## javap 原始證據

### 正式 consumer

```text
public boolean canOverwrite();

    descriptor: ()Z

    Code:

       0: aload_0

       1: getfield      #89                 // Field canOverwrite:Z

       4: aload_0

       5: ldc           #189                // String canOverwrite

       7: invokestatic  #106                // Method local/lito381/server/CombatSpecialBuffs.eyeField:(ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I

      10: ireturn
```

### eyeField 全部指令（含三個 IRETURN）

```text
static int eyeField(int, local.lito381.server.BattleDragonEyeCatalog$Eye, java.lang.String);

    descriptor: (ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I

    Code:

       0: aload_1

       1: invokestatic  #326                // Method config:(Llocal/lito381/server/BattleDragonEyeCatalog$Eye;)Llocal/lito381/server/SourceEyeControls$Config;

       4: astore_3

       5: aload_3

       6: ifnonnull     11

       9: iload_0

      10: ireturn

      11: getstatic     #330                // Field EYE_ABILITIES:Ljava/util/Map;

      14: aload_2

      15: invokeinterface #88,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;

      20: checkcast     #148                // class java/lang/String

      23: astore        4

      25: aload         4

      27: ifnull        43

      30: invokestatic  #334                // Method local/lito381/server/CombatDatabase.controls:()Llocal/lito381/server/SourceSkillControls$Snapshot;

      33: aload_1

      34: invokevirtual #337                // Method local/lito381/server/BattleDragonEyeCatalog$Eye.buffId:()I

      37: aload         4

      39: invokestatic  #340                // Method local/lito381/server/SourceSkillControls.ability:(Llocal/lito381/server/SourceSkillControls$Snapshot;ILjava/lang/String;)I

      42: ireturn

      43: aload_2

      44: astore        5

      46: iconst_m1

      47: istore        6

      49: aload         5

      51: invokevirtual #346                // Method java/lang/String.hashCode:()I

      54: lookupswitch  { // 8


           -1494062693: 248


           -1378119474: 128


            -477174197: 145


            -185703274: 230


             -55997436: 213


             247674763: 179


             555311034: 162


            1257868761: 196

               default: 263

          }

     128: aload         5

     130: ldc_w         #349                // String buffId

     133: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     136: ifeq          263

     139: iconst_0

     140: istore        6

     142: goto          263

     145: aload         5

     147: ldc_w         #350                // String durationSeconds

     150: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     153: ifeq          263

     156: iconst_1

     157: istore        6

     159: goto          263

     162: aload         5

     164: ldc_w         #352                // String castGfx

     167: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     170: ifeq          263

     173: iconst_2

     174: istore        6

     176: goto          263

     179: aload         5

     181: ldc_w         #354                // String statusIcon

     184: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     187: ifeq          263

     190: iconst_3

     191: istore        6

     193: goto          263

     196: aload         5

     198: ldc_w         #356                // String sharedDelayId

     201: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     204: ifeq          263

     207: iconst_4

     208: istore        6

     210: goto          263

     213: aload         5

     215: ldc_w         #358                // String sharedDelayMillis

     218: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     221: ifeq          263

     224: iconst_5

     225: istore        6

     227: goto          263

     230: aload         5

     232: ldc_w         #360                // String itemDelayMillis

     235: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     238: ifeq          263

     241: bipush        6

     243: istore        6

     245: goto          263

     248: aload         5

     250: ldc_w         #362                // String canOverwrite

     253: invokevirtual #147                // Method java/lang/String.equals:(Ljava/lang/Object;)Z

     256: ifeq          263

     259: bipush        7

     261: istore        6

     263: iload         6

     265: tableswitch   { // 0 to 7


                     0: 312


                     1: 319


                     2: 326


                     3: 333


                     4: 340


                     5: 347


                     6: 354


                     7: 361

               default: 376

          }

     312: aload_3

     313: invokevirtual #364                // Method local/lito381/server/SourceEyeControls$Config.buff:()I

     316: goto          390

     319: aload_3

     320: invokevirtual #367                // Method local/lito381/server/SourceEyeControls$Config.seconds:()I

     323: goto          390

     326: aload_3

     327: invokevirtual #370                // Method local/lito381/server/SourceEyeControls$Config.gfx:()I

     330: goto          390

     333: aload_3

     334: invokevirtual #373                // Method local/lito381/server/SourceEyeControls$Config.icon:()I

     337: goto          390

     340: aload_3

     341: invokevirtual #376                // Method local/lito381/server/SourceEyeControls$Config.sharedDelay:()I

     344: goto          390

     347: aload_3

     348: invokevirtual #379                // Method local/lito381/server/SourceEyeControls$Config.sharedMillis:()I

     351: goto          390

     354: aload_3

     355: invokevirtual #382                // Method local/lito381/server/SourceEyeControls$Config.itemMillis:()I

     358: goto          390

     361: aload_3

     362: invokevirtual #385                // Method local/lito381/server/SourceEyeControls$Config.overwrite:()Z

     365: ifeq          372

     368: iconst_1

     369: goto          390

     372: iconst_0

     373: goto          390

     376: new           #388                // class java/lang/IllegalArgumentException

     379: dup

     380: aload_2

     381: invokedynamic #390,  0            // InvokeDynamic #5:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;

     386: invokespecial #393                // Method java/lang/IllegalArgumentException."<init>":(Ljava/lang/String;)V

     389: athrow

     390: ireturn
```

### 不可修改 map 的完整初始化

```text
static {};

    descriptor: ()V

    Code:

       0: sipush        4009

       3: invokestatic  #25                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;

       6: sipush        4010

       9: invokestatic  #25                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;

      12: sipush        4016

      15: invokestatic  #25                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;

      18: invokestatic  #437                // InterfaceMethod java/util/List.of:(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;

      21: putstatic     #190                // Field BLESSINGS:Ljava/util/List;

      24: ldc_w         #500                // String meleeDamage

      27: ldc_w         #456                // String DMG

      30: ldc_w         #501                // String magicHit

      33: ldc_w         #503                // String MAGIC_HIT

      36: ldc_w         #505                // String damageReduction

      39: ldc_w         #433                // String DMG_REDUX

      42: ldc_w         #507                // String evasionRate

      45: ldc_w         #481                // String ER

      48: ldc_w         #509                // String stoneResistance

      51: ldc_w         #511                // String REGIST_STONE

      54: ldc_w         #513                // String sleepResistance

      57: ldc_w         #515                // String REGIST_SLEEP

      60: ldc_w         #517                // String freezeResistance

      63: ldc_w         #519                // String REGIST_FREEZE

      66: ldc_w         #521                // String poisonResistance

      69: ldc_w         #523                // String POISON_RESIST

      72: invokestatic  #525                // InterfaceMethod java/util/Map.of:(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;

      75: putstatic     #330                // Field EYE_ABILITIES:Ljava/util/Map;

      78: return

}
```

### Config.overwrite accessor

```text
public boolean overwrite();

    descriptor: ()Z

    Code:

       0: aload_0

       1: getfield      #35                 // Field overwrite:Z

       4: ireturn
```

## 可重現命令

從此 worktree 根目錄執行。下列命令將兩份內嵌腳本取到 scratch，重編工具與 25 個來源、轉換、打包、比較，再產生 javap 證據。JDK／Python 路徑可換成同版工具位置。所有寫入均在 worktree；清理僅逐檔移除經 resolve 驗證位於 classes 目錄的可重建 class。

```powershell
$taskText = Get-Content -Raw -Encoding UTF8 docs/reports/issue-75.md
$taskOut = Join-Path (Get-Location) 'scratch/issue-75'
New-Item -ItemType Directory -Force $taskOut | Out-Null
$taskUtf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($taskName in @('verify.py','evidence.py')) {
    $taskPattern = '(?s)<!-- FILE: ' + [regex]::Escape($taskName) + ' -->\s*```[^\r\n]*\r?\n(.*?)\r?\n```'
    $taskMatch = [regex]::Match($taskText, $taskPattern)
    if (-not $taskMatch.Success) { throw "找不到附錄 $taskName" }
    [IO.File]::WriteAllText((Join-Path $taskOut $taskName), $taskMatch.Groups[1].Value + "`n", $taskUtf8)
}
& 'D:/dev-orchestrator/tools/python/python.exe' -B -X utf8 scratch/issue-75/verify.py --effects --deviations
if ($LASTEXITCODE -ne 0) { throw 'A04 驗收失敗' }
& 'D:/dev-orchestrator/tools/python/python.exe' -B -X utf8 scratch/issue-75/evidence.py
if ($LASTEXITCODE -ne 0) { throw 'javap 證據檢查失敗' }
```

### verify.py

<!-- FILE: verify.py -->
```python
from pathlib import Path
import json, subprocess, os, sys, zipfile, hashlib, urllib.request
ROOT=Path.cwd(); SERVER=ROOT/'lito伺服器/LiTo381-Server'; WORK=ROOT/'scratch/issue-75'; OUT=SERVER/'target/issue-75'; OUT.mkdir(parents=True,exist_ok=True)
os.environ['JAVA_HOME']='C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
os.environ['AS_RECORD_MAVEN_REPO']='D:/dev-cache/maven/repository'
os.environ['TEMP']=os.environ['TMP']=str(OUT)
sys.dont_write_bytecode=True
sys.path.insert(0,str(SERVER/'tools/as-record'))
import as_record as tool
BATCH=next(b for b in json.loads((ROOT/'docs/combat-merge/a-batches.json').read_text('utf-8'))['batches'] if b['id']=='A04')
STAGED=SERVER/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
def run(args):
 p=subprocess.run([str(a) for a in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 print(p.stdout.decode('utf-8',errors='replace')); print('EXIT',p.returncode); p.check_returncode(); return p.stdout
if __name__=='__main__':
 STAGED.mkdir(parents=True,exist_ok=True)
 for name in BATCH['classes']:
  dest=STAGED/(name+'.java')
  if not dest.exists(): raise RuntimeError('Missing staged source: '+str(dest))
 analysis=OUT/'dependency/asm-analysis-9.7.jar'
 analysis.parent.mkdir(parents=True,exist_ok=True)
 if not analysis.exists(): analysis.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar',timeout=60).read())
 assert hashlib.sha256(analysis.read_bytes()).hexdigest()=='7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
 deps=tool.dependencies()+os.pathsep+str(analysis)
 tool.compile_java([SERVER/'src/main/java/local/lito381/server/AsRecord.java'],OUT/'annotation','')
 tool.compile_java(sorted((SERVER/'tools/semantic-diff/src/main/java').rglob('*.java')),OUT/'diff-tool',deps)
 (WORK/'entries.txt').write_text('\n'.join(BATCH['verify_entries'])+'\n','utf-8')
 classes=OUT/'classes'; classes.mkdir(exist_ok=True)
 # Remove only generated class files in this exact worktree output directory.
 assert classes.resolve().is_relative_to(ROOT.resolve())
 for p in classes.rglob('*.class'): p.unlink()
 log=run([tool.jdk('javac'),'-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-cp',str(SERVER/'reference/base-0.6.71.jar')+';'+str(SERVER/'lib/*')+';'+str(OUT/'annotation'),'-d',classes,*[STAGED/(n+'.java') for n in BATCH['classes']]])
 (WORK/'javac.log').write_bytes(log)
 log=run([tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',tool.build(),'local.lito381.asrecord.AsRecord',classes]); (WORK/'as-record.log').write_bytes(log)
 jar=OUT/'a04.jar'
 with zipfile.ZipFile(jar,'w') as z:
  for p in sorted(classes.rglob('*.class')):
   z.writestr(zipfile.ZipInfo(p.relative_to(classes).as_posix(),(1980,1,1,0,0,0)),p.read_bytes())
 flags=[]
 owners={e.removesuffix('.class') for e in BATCH['verify_entries']}
 if '--effects' in sys.argv:
  owners={e.removesuffix('.class') for e in BATCH['verify_entries']}
  approved=[line for line in (ROOT/'docs/combat-merge/effects-approved.txt').read_text('utf-8-sig').splitlines() if line.split('#',1)[0].strip() in owners]
  assert approved, 'No A04 effects approvals'
  (WORK/'effects.txt').write_text('\n'.join(approved)+'\n','utf-8')
  flags=['--effects-methods',WORK/'effects.txt']
 if '--deviations' in sys.argv:
  approved=[line for line in (ROOT/'docs/combat-merge/deviations.txt').read_text('utf-8-sig').splitlines() if line.split('#',1)[0].strip() in owners]
  assert len(approved)==1 and 'canOverwrite()Z method-diff' in approved[0]
  (WORK/'deviations.txt').write_text('\n'.join(approved)+'\n','utf-8')
  flags += ['--deviations',WORK/'deviations.txt']
 log=run([tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',str(OUT/'diff-tool')+';'+deps,'local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',SERVER/'reference/base-0.6.71.jar','--left-classpath',SERVER/'reference/base-0.6.71.jar','--classes',WORK/'entries.txt',*flags,'--json',WORK/'diff.json','--md',WORK/'diff.md']); (WORK/'semantic.log').write_bytes(log)
 data=json.loads((WORK/'diff.json').read_text('utf-8'))
 print(json.dumps(data['counts'])); print(json.dumps(data['classSelection']))
 for c in data['classes']:
  if c['status']!='SAME': print(json.dumps(c,ensure_ascii=False))

 actual={p.relative_to(classes).as_posix() for p in classes.rglob('*.class')}
 expected=set(BATCH['verify_entries'])
 assert actual==expected, {'missing':sorted(expected-actual),'extra':sorted(actual-expected)}
 print('ENTRY SET expected='+str(len(expected))+' actual='+str(len(actual))+' missing=[] extra=[]')
 print('INPUT COMMIT '+subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip())
 for label,path in [('FORMAL',SERVER/'reference/base-0.6.71.jar'),('OUTPUT',jar)]:
  print(label+' SHA256 '+hashlib.sha256(path.read_bytes()).hexdigest())
 content=hashlib.sha256()
 for entry in sorted(expected):
  content.update(entry.encode('utf-8')+b'\0'+(classes/entry).read_bytes())
 print('CLASS CONTENT SHA256 '+content.hexdigest())
 passed=len(data['classes'])==86 and all(c['status'] in {'SAME','SAME_EFFECTS','SAME_PATHS','DEVIATION'} for c in data['classes']) and all(not v for v in data['classSelection'].values())
 print('ACCEPTANCE '+('PASS' if passed else 'NOT MET'))
 sys.exit(0 if passed else 2)
```

### evidence.py

<!-- FILE: evidence.py -->
```python
from pathlib import Path
import subprocess, json, hashlib, re
ROOT=Path.cwd(); WORK=ROOT/'scratch/issue-75'
SERVER=ROOT/'lito伺服器/LiTo381-Server'
JDK=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
JAR=SERVER/'reference/base-0.6.71.jar'
assert hashlib.sha256(JAR.read_bytes()).hexdigest()=='e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
texts={}
for name in ['CombatSpecialBuffs','BattleDragonEyeCatalog$Eye','SourceEyeControls$Config','CombatPoisonAtomic']:
    args=[str(JDK/'javap.exe'),'-c','-p','-s','-classpath',str(JAR),'local.lito381.server.'+name]
    p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    p.check_returncode()
    text=p.stdout.decode('utf-8'); texts[name]=text
    (WORK/(name+'.javap.txt')).write_text(text,'utf-8')
    print('javap',name,'exit=0')
def method(text, signature):
    start=text.index('  '+signature)
    end=text.find('\n  ',start+3)
    # Member declarations have exactly two leading spaces; code lines have more.
    match=re.search(r'\n  \S',text[start+3:])
    return text[start:start+3+match.start()] if match else text[start:]
producer=method(texts['CombatSpecialBuffs'],'static int eyeField(')
assert re.findall(r'^\s+(\d+): ireturn',producer,re.M)==['10','42','390']
clinit=method(texts['CombatSpecialBuffs'],'static {};')
assert '// String canOverwrite' not in clinit
assert texts['CombatSpecialBuffs'].count('// Field EYE_ABILITIES:')==2
assert 'private static final java.util.Map<java.lang.String, java.lang.String> EYE_ABILITIES;' in texts['CombatSpecialBuffs']
assert 'InterfaceMethod java/util/Map.of:' in clinit
sections=[('正式 consumer',method(texts['BattleDragonEyeCatalog$Eye'],'public boolean canOverwrite();')),
          ('eyeField 全部指令（含三個 IRETURN）',producer),
          ('不可修改 map 的完整初始化',clinit),
          ('Config.overwrite accessor',method(texts['SourceEyeControls$Config'],'public boolean overwrite();'))]
(WORK/'javap-evidence.md').write_text('\n\n'.join('### '+title+'\n\n```text\n'+text.strip()+'\n```' for title,text in sections)+'\n','utf-8')
print('eyeField IRETURN offsets=[10,42,390]; EYE_ABILITIES accesses=2; canOverwrite key absent')
data=json.loads((WORK/'diff.json').read_text('utf-8'))
assert data['counts']['DIFF']==0
assert next(c for c in data['classes'] if c['name'].endswith('/CombatPoisonAtomic'))['status']=='SAME_PATHS'
for c in data['classes']:
    if c['name'].endswith('/CombatPoisonAtomic'):
        for m in c['methodResults']:
            assert m['status']=='SAME_PATHS'
            print(m['method'],m['detail'])
```
