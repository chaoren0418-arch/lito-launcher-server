# Issue #138 — T020-B-land：B 暫存區併入 src/main

## 結論

B 擁有的 303 個外層類別已全部併入 `src/main/java/local/lito381/server/`；`mvn -DskipTests package` 在本機成功，AsRecord 轉換了 54 個類別。
語意比對結果：
- **B 範圍**：1,384 個 entry 無缺漏，也沒有多出或該移除的 entry。DIFF 只剩 1 類，就是 `ClientSession`；它的 184 個差異方法和 #70 主控裁定交給 M2 離線回歸的 `b09-shape-diffs.md` 清單**完全一致**。
- **T029**：8 個方法對 `base-0.6.71.jar` 為 SAME（其中 7 個）或 SAME_EFFECTS（1 個）。
- **A 範圍**：A-land（#78）已合入，本輪重跑，692 個 entry 全部可接受，DIFF＝0。
- **整顆 JAR**：其他類別的差異為 0。

**`mvn -B test` 尚未全綠，本任務未完成驗收。** 依本機限制不在本機跑測試，結果以 PR #315 的 CI 為準：
- 第 2 輪 CI（完整跑完）：6,048 項，Failures 38、Errors 11，分布在 21 個測試類別。
- 本輪已逐案修正其中 7 類／14 項（見「測試逐案判斷」）。
- 剩下的項目中，有一部分是**候選 JAR 本身的程式缺陷**，修正就會讓 B 類別偏離候選，必須先由主控裁定；其餘是綁死舊實作文字的原始碼稽核與行為測試，尚待逐案分析。

需要主控裁定（needs:claude）：
1. **候選缺陷：未登入就斷線的 session 會拋例外。** 候選 `GameHub.disconnect` 新增了 `NativeSummonRetirement.beforeDisconnect`，它會取 `sourceAccountIdentity()`。登入前就斷線（登入失敗、掃 port）的 session 沒有這個身分，會拋 IAE，而且是在 `ClientSession.run()` 的 finally 裡拋，後續清理全被跳過。正式 0.6.71 的 `disconnect` 沒有這個呼叫。影響 5 項測試。
2. **候選缺陷：馴服怪物的釋放必定失敗。** 候選 `ClassicWorldMonsterPopulation$TamedRelease` 的檢查是 `lcmp; ifeq → throw`（objectId 相同就拋），正式版是 `ifne`。`releaseTamed` 傳入的兩個快照來自同一個 actor，objectId 必然相同，所以候選版每次釋放都會拋 IAE。影響 1 項測試。
3. **潛在死鎖（推測）。** CI thread dump 顯示候選版 monster tick 的取鎖順序是 population → hub（`NativeStaticCancellationReplacement.live`），companion tick 則是 hub → population。正式環境中兩者都在同一條 `lito381-battle-clock` 執行緒上，不會彼此鎖死；但如果有 client 執行緒以 hub → population 的順序呼叫，就可能死鎖。測試已改成和正式環境一樣在 battle clock 上 tick；正式環境是否真的有風險，交 M2 驗證。
4. **原始碼文字稽核要怎麼處理（政策）。** 約 7 類 `*SourceAuditTest` 用字串綁死舊 src/main 的實作文字。候選版是反編譯來的（變數名如 `var237`、補上 `this.`、結構也改了），要逐條重寫並重新確認語意。請裁定：是逐條改寫成對候選文字的稽核，還是改用行為測試取代。
5. 依 #69 第 3 點新增的 23 條 throws-only `exceptions-exempt` 條目，以及 4 條 #69 補同步的條目，請確認。
6. 舊 GameHub「native 與 modern adapter 共用 TournamentPresence」的接縫（只有測試用到，Main 從來沒接上）在候選版中不存在。相關 2 個測試已改寫，請裁定這項功能要不要補回。

## 做了什麼

| 項目 | 內容 |
|---|---|
| 搬入 | `merge-inputs/50635f7e/staged/local/lito381/server/*.java` 303 份複製進 `src/main`：覆蓋 84 個同名檔、新增 219 個檔（`docs/reports/issue-138/land.py`） |
| 第 85 個既有類別 | `LegacyMySqlRepository` 原本共置在 A 擁有的 `Accounts.java` 尾端（第 4153–13309 行）。現在改成 staged 的獨立檔；`Accounts.java` 只刪掉這個類別，其餘 A 內容逐字未動 |
| checked exception 鏈 | 詳見下一節；23 個方法各補一個 `throws IOException`，方法本體不變 |
| 核准清單同步 | `exceptions-exempt.txt` 新增 #69 漏同步的 3 條，以及本輪的 23 條；`extra-methods-approved.txt` 新增 #69 漏同步的 `NativeLightAction$Batch#rethrowCandidateFailure` 1 條 |
| ModernCaptureLog | 依 #78 主控備註：候選 `ClientSession` 用回 `CaptureLog` 之後，`ModernCaptureLog` 就沒有呼叫者了，所以刪除它和 `ModernCaptureLogTest`（2 項），並重新產生 `docs/server-source-gap.md` |
| `b09-shape-diffs.md` | 依 #70 主控備註補上 `ClientSession#sourceApplicationRun()V` 一列，並把該節標題從「待裁定」改成「已核准」 |
| 測試 | 修了 3 個檔，見「測試逐案判斷」 |

沒有碰：A 擁有的其他類別、`pom.xml`、機密設定、封存、`releases/`、`docs/REPORT.md`。

### checked exception 鏈（主控請確認）

B 批次各自對候選 JAR 編譯，每批只看得到其他類別的**位元組碼**，而位元組碼不檢查 checked 例外。
`NativeWeaponBreakTargetLifecycle#captureOutput` 依 #62 核准宣告了 `throws IOException`。可是在候選中，呼叫它的 `NativeAreaMaterialAction#nativeShockOriginalCaptureOutput` 沒有 throws，這條委派鏈一路往上的 22 個方法也都沒有。
所以全部放進同一個原始碼樹後，`javac` 一層一層報 `unreported exception java.io.IOException`。

處理方式：每輪編譯後，在出錯位置所屬的方法宣告補上 `throws IOException`（`throws_fix.py`；lambda 內出錯會另外標示，這次沒有出現），共 23 輪後 BUILD SUCCESS。

我沒有用 try/catch 包起來：那樣會改變方法本體與例外型別，屬於行為差異。
只補 throws 宣告則只影響 Exceptions 屬性，正好符合 [#69 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373) 第 3 點：「若差異只在 `throws` 宣告，而且 Java 不宣告就無法編譯，可以加入 `exceptions-exempt.txt`，並附 javac 失敗證據」。
語意比對確認這 23 個方法除了 Exceptions 屬性之外都與候選相同：只套用舊清單時的 firstDiff 都是 `left=java/io/IOException right=`；加入清單後全部變成 SAME 或 SAME_PATHS。

javac 失敗證據（`mvn -B -DskipTests -Dmaven.test.skip=true compile`，每輪只報 1 個錯誤）：

```text
round 1 : NativeAreaMaterialAction.java:[439,62] unreported exception java.io.IOException; must be caught or declared to be thrown
round 2 : NativeShockCompanionAction.java:[27,184] unreported exception java.io.IOException; ...
round 3 : NativeAreaMaterialAction.java:[443,56] ...
round 4 : NativeShockPlayerAction.java:[50,205] ...
round 5 : NativeAreaMaterialAction.java:[447,53] ...
round 6 : NativeAcceptedShockMutation.java:[16,331] ...
round 7 : NativeAreaMaterialAction.java:[451,57] ...
round 8 : NativeStaticNpcRespawn.java:[13,175] ...
round 9 : NativeAreaMaterialAction.java:[455,52] ...
round 10 : NativeAcceptedFreezeMutation.java:[31,205] ...
round 11 : NativeAcceptedAreaFreezeMutation.java:[16,179] ...
round 12 : NativeAcceptedFreezeMutation.java:[48,128] ...
round 13–22 : NativeAreaMaterialAction.java:[459|466|473|480|487|494|501|508|515|522,…] ...
round 23 : NativeLightAction.java:[422,51] ...
round 24 : BUILD SUCCESS
```

補了 throws 的 23 個方法，描述子都是 `(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z`：
`NativeAcceptedAreaFreezeMutation#captureOutput`、`NativeAcceptedFreezeMutation#{nativeAreaFreezeOriginalCaptureOutput,captureOutput}`、`NativeAcceptedShockMutation#captureOutput`、
`NativeAreaMaterialAction#{nativeShock,nativeShockPlayer,nativeAcceptedShock,nativeStaticRespawn,nativeFreezeAccepted,nativeBattleControlled,nativeSourceMassSlow,nativeSourceWind,nativeSourceNegative,nativeSourceLightning,nativeSourceWeaponBuff,nativeSourceRemaining,nativeSourceControlled,nativeRaw}OriginalCaptureOutput`、`NativeAreaMaterialAction#captureOutput`、
`NativeLightAction#deferSelfSend`、`NativeShockCompanionAction#captureOutput`、`NativeShockPlayerAction#captureOutput`、`NativeStaticNpcRespawn#captureOutput`。

## 已驗證

### 建置（本機，只編譯）

環境：Microsoft OpenJDK 17.0.17、Apache Maven 3.9.9。Maven 從 Maven Central 下載到 `scratch/issue-138/tools/`，SHA-512 與官方 `.sha512` 相符；本機原本沒有 mvn。TEMP 和 java.io.tmpdir 都指向 `scratch/issue-138/tmp`。

```powershell
# 從 worktree 根目錄執行；mvn.ps1 設定 JAVA_HOME／TEMP 並在 LiTo381-Server 執行 mvn -B
scratch/issue-138/mvn.ps1 compile24.log "-DskipTests" "-Dmaven.test.skip=true" compile   # exit 0
scratch/issue-138/mvn.ps1 testcompile2.log test-compile                                   # exit 0
scratch/issue-138/mvn.ps1 package1.log "-DskipTests" package                             # exit 0
#   as-record: scanned=4433 converted=54 ; as-record(test): scanned=2039 converted=1 ; BUILD SUCCESS
```

`target/LiTo381-Server.jar` SHA-256：`55f63a7e3ad308d3793dd0e516bf8fb86f4c3cb06f712cd61f14328159112bda`。

### 語意比對（`semantic-diff`，靜態位元組碼分析）

工具從本分支的 `tools/semantic-diff/src` 重新編譯，使用 ASM 9.7（asm／asm-tree／asm-analysis，SHA-256 與 `build-support.sh` 釘定的值相同）。
指令：`py -I scratch/issue-138/verify.py r2`（腳本副本在 `docs/reports/issue-138/verify.py`，要放回 `scratch/issue-138/` 執行）。完整 argv 在 `docs/reports/issue-138/semantic-commands.json`，摘要在 `semantic-summary.json`。
每份核准清單都先裁切到本輪比較範圍內的 owner（做法同 #78），用不到的條目另外記錄。B 範圍實際傳入的 exceptions 清單是 `docs/reports/issue-138/b-approved-exceptions-exempt.txt`。

| 比較 | 右側 | 範圍 | SAME | SAME_EFFECTS | SAME_PATHS | SAME_CFG | LAMBDA_PAIRED | ORPHAN | SAME_LAYOUT | DEVIATION | DIFF | ONLY_LEFT | ONLY_RIGHT |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| B | candidate-50635f7e（T029 → base） | `--classes` 1,384 | 1351 | 3 | 15 | 9 | 2 | 1 | 0 | 2 | **1** | 0 | 0 |
| A（rebase 後重跑） | base-0.6.71 | `--classes` 692 | 665 | 4 | 16 | 0 | 0 | 0 | 1 | 6 | **0** | 0 | 0 |
| 整顆 JAR | base-0.6.71 | 全部 | 2381 | 4 | 17 | 0 | 0 | 0 | 1 | 6 | 145 | 1863 | 0 |

- **B**：`classSelection.missingLeft=[]`。`extraLeft` 都是非 B 的 entry，也就是 JAR 裡的其他類別，不算入比較。
  - 唯一的 DIFF 是 `ClientSession`：DIFF 175 個方法（174 個形狀差異＋`sourceApplicationRun`），ONLY_LEFT 9 個 lambda。
  - `shape_check.py` 確認這 184 個方法與 `b09-shape-diffs.md` 列出的 184 項**一一對應**（不在清單內的為 0）。依 #70 主控裁定，這些不再追求靜態 DIFF=0，交給 M2 離線回歸。
  - 依 #69 主控裁定，SAME_PATHS、SAME_CFG、LAMBDA_PAIRED_BY_CALLSITE、ORPHAN_IGNORED 都算通過。2 個 DEVIATION 是已核准的 `NativeLightAction$Batch#emit()V` method-diff，以及 B09 的 deviation。
- **T029**：`--allow-methods docs/combat-merge/t029-methods.txt --allow-right reference/base-0.6.71.jar`，8 個方法都套用了。7 個為 SAME；`exitRoomBeforePolyMe(Z)V` 為 SAME_EFFECTS（`effects-approved.txt` 的既有條目）。
- **remove_entries**：9 批宣告 0 個，JAR 中也確實不存在（`remove_entries_present=[]`）。
- **整顆 JAR 分類**：B `{DIFF:145, ONLY_LEFT:483}`。這些是候選相對正式版本來就有的戰鬥修改與新類別，B 範圍已經對候選驗收過。
  - SOURCE_ONLY `{ONLY_LEFT:1380}`：來源專有的 Modern 等類別，原本 1,382 個，少的 2 個是刪掉的 ModernCaptureLog。
  - **OTHER＝0**，代表 A 與其他類別沒有引入任何差異。

第一輪（`r1`）只用 main 上的共用清單，B 的 DIFF 為 11 類：
- 1 類是 ClientSession（形狀差異）；
- 9 類是 23 個 throws 方法；
- 1 類是 `LegacyMySqlRepository` 3 個 throws（#69 已核准，但沒同步到共用清單）；
- 1 類是 `NativeLightAction$Batch`（`rethrowCandidateFailure` 同樣漏同步）。

`approvals_gap.py` 掃描 B01–B09 報告中所有 `# approved:` 條目與共用清單的差集，只找到上面這 4 條；同步之後，第二輪（`r2`）只剩 ClientSession。

### 主控備註逐項

| 備註 | 結果 |
|---|---|
| #69：手寫 `NativeLightAction#lambda$deferWeaponTargetOutput$0` 會不會和 javac 產生的 lambda 撞名 | **不撞名**。編譯產物（javap）中手寫的 `lambda$deferWeaponTargetOutput$0` 保留原名；javac 為 `deferWeaponTargetOutput` 裡的 lambda 產生 `lambda$deferWeaponTargetOutput$11`。語意比對中 NativeLightAction 為可接受狀態（LAMBDA_PAIRED／SAME 系列）。 |
| #69：保留六個 AsRecord、治療 monitor／vitalLock finally、Output 可見性、helper／Exceptions／emit 核准；`finish(null)` 不放進 emit 核心 try | staged 原始碼原樣搬入、未修改；NativeLightAction$Batch 的 emit 為核准 DEVIATION，`nativeSourceBatchOriginalEmit` 可接受；`access-widen` 的 `NativeLightAction$Output` 有套用 |
| #78：ModernCaptureLog 已無呼叫者就刪除並更新 `server-source-gap.md` | 已刪除（含 `ModernCaptureLog$Connection`），並用 CI 同一段 jar tf 程式重新產生（轉成 LF） |
| #70：`b09-shape-diffs.md` 補 `ClientSession#sourceApplicationRun()V` | 已補 |
| #70：帳號隔離／職業傷害倍率／技能機率的呼叫點要在 src/main 原始碼裡 | 見下表，都已是原始碼呼叫 |

| 能力 | 正式 JAR 的做法 | B-land 後 src/main 原始碼中的呼叫點 |
|---|---|---|
| X6 帳號隔離（`SourceLogin`） | `PatchSourceLogin` 插入 ClientSession（建構子 configuration／repository、`run()` 的 allowed 守門、isAccountBanned／banAccount／GameHub handoff 的 account）與 GameHub（acceptAuthenticatedAccount／releaseAuthenticatedAccountForTransfer 的 account、`close`） | `ClientSession.java`：`SourceLogin.configuration` ×1、`repository` ×1、`allowed` ×1、`account` ×19；`GameHub.java`：`SourceLogin.account` ×2、`close` ×1；`Main.java`：`SourceLogin.start` ×1 |
| B8 職業傷害倍率（`CombatClassDamage`） | `ClassDamagePatch` | `ClientSession.java:47950, 48850, 86176` 呼叫 `CombatClassDamage.hit`；另有 `CombatRules`（7）、`CombatKiringku`（2）、`CombatCompanionDamage`（1） |
| B10 技能機率（`CombatProbability`） | patch | `ClientSession.java` 共 189 處（`bossGate` ×103、`dispatch` ×62、`counterDecision` ×8、`npcThunderDuration` ×6、`npcMortalDecision` ×3、`mortalDecision`／`confusionSucceeds`／`confusionChance` 各 ×2、`thunderSucceeds` ×1），另有 14 個 Combat*／Native* 類別 |

### 測試逐案判斷（比照 T010；沒有停用或跳過任何測試）

`mvn test-compile` 起初失敗 3 處：

| 測試 | 判斷 | 處理 |
|---|---|---|
| `TournamentPresenceTest#nativeGameHubDerivesCapacityAndReservationsFromTheInjectedCore` | **測試過時（以候選為準）**。它用的是舊 GameHub 的 4 參數建構子 `GameHub(config, state, accounts, TournamentPresence)` 和 `selectCharacter(session, sourceId, source, row)`。這兩個是 src/main 專有的測試接縫：正式程式 `Main` 只用 3 參數建構子，也從沒有讓 native 和 modern 共用同一個 presence。候選 GameHub（B09，必須與候選相同）沒有這兩個 API。 | 改寫成 `nativeGameHubCapacityIsIndependentOfAModernPresenceCoreAfterBLand`：鎖定候選的契約。modern core 坐滿 100 人不影響 native；native 自己的 `CHANNEL_CAPACITY` 仍會拒絕第 101 人，第 101 人改進頻道 1 也不影響 core。第 2 輪 CI 發現 fixture 是未登入的 session，對它呼叫 `hub.disconnect` 會碰到缺陷 1（見結論），所以拿掉了 disconnect 的步驟。 |
| `ModernNavigationSessionTest#nativeAndModernAdaptersActuallyCompeteForTheSameCharacterAndChannelCapacity` | 同上，**測試過時**。 | 改寫成 `nativeHubAndModernAdapterKeepSeparateCapacityAfterBLand`：保留 modern TLS 的 hello／login／roster／選角／進頻道／離開大廳完整流程，驗證 native 頻道坐滿時 modern 仍能進入，且兩邊人數互不影響。 |
| `ClientSessionLitoRoomInvitationTest`（第 108、180 行） | **測試過時**。A-land 為了配合舊 ClientSession 改成 `ModernCaptureLog`，候選 ClientSession 只接受 `CaptureLog`。 | 改回 A-land 之前的原文 `CaptureLog`，斷言完全不動。 |
| `ModernCaptureLogTest`（2 項） | 受測類別依 #78 主控備註刪除 | 隨類別一起刪除；不是因為失敗才停用 |

#### 第 2 輪 CI（run 42882305652，完整跑完）的 49 項逐案

CI 指令：`mvn -B test`（`.github/workflows/build.yml` 的 Server tests job）。結果：`Tests run: 6048, Failures: 38, Errors: 11, Skipped: 117`，BUILD FAILURE，耗時 12:42。
完整日誌在 artifact `server-test-evidence`，我下載到本機 `scratch/issue-138/ci2/`，未入庫。

第 1 輪（run 42876069149）卡在 `ClientSessionClassicWorldMonsterAggressionIntegrationTest` 超過 24 分鐘，我手動取消。
第 2 輪我暫時在該類別加上 `@Timeout(SEPARATE_THREAD)` 和 thread dump：逾時的測試仍判為 ERROR，不是跳過。取得 dump 之後已移除這段暫時診斷。

| 類別（項數） | 失敗內容 | 判斷 | 本輪處理 |
|---|---|---|---|
| `NativeMagicDollTest`（2） | 期望 opcode 250，實際是 123 | **測試過時**：依 T029，`Protocol.magicDollIcon／magicDollCustomIcon` 採正式版；語意比對已驗證這兩個方法與正式版 SAME | 期望值改成正式封包（`123,56,h,h(0)`；custom 為 `123,154,…`） |
| `ProtocolBattleTest`（3） | ① summon 的 choice=0、② ALT_ATTACK 的 target=0 沒有拋 IAE；③ `sendBattleObject(2, "ArenaBot"…` 的文字出現次數是 1，期望 2 | ①② **測試過時**：javap 確認正式版與候選版都沒有這兩個檢查，屬舊 src/main 專有的嚴格驗證。③ **測試過時（文字形狀）**：候選版第 10744 行是反編譯形式 `this.sendBattleObject(2L, "ArenaBot", "ArenaBot", point.x()…`，語意仍在 | ①② 改成驗證會被接受；③ 改成計數穩定的引數對 `"ArenaBot", "ArenaBot", `（仍要求 2 次） |
| `BattleSpellCatalogTest`（1） | Wind Shackle 的 `area` 期望 -1，實際是 0 | **測試過時**：候選把該列資料改成 0（B 以候選為準） | 期望改成 0 |
| `ClientSessionClassicWorldMonsterAggressionIntegrationTest`（2，逾時） | 死鎖 | **測試過時（執行緒模型）**：測試從測試執行緒直接 tick，與 live battle clock 併發；正式環境的 monster tick 和 companion tick 都排在同一條 battle clock 上 | `Fixture.tick(...)` 改成提交到 hub 的 `battleClock` 執行（clock 已停止時才直接呼叫）。正式環境的潛在風險見結論第 3 點 |
| `NativeHotelSqlFirstPersistenceSourceAuditTest`（1）、`NativeCastleSecuritySettlementSourceAuditTest`（1） | 字串找不到 | **測試過時（拆檔）**：這兩個稽核要找的 LegacyMySqlRepository 字串，已隨類別搬到獨立檔 | 改成讀 `Accounts.java`＋`LegacyMySqlRepository.java`。`audit_probe.py` 靜態確認其他字串都還在 |
| `WaterDragonSqlFirstPersistenceSourceAuditTest`（1）、`PcActionRewardSqlFirstSourceAuditTest`（3） | 字串找不到 | 拆檔部分同上，已處理。另外綁死的 ClientSession 文字，如 `inventory = next;`、`selectedClassicServerName, passObjects`、`inventory = afterInventory`，在候選反編譯後已改名或改寫；PcActionReward 還檢查客戶端 HTML 資源雜湊 | **待分析**（結論第 4 點）。本輪只修了拆檔部分，預期仍會失敗 |
| `NativeHonorRespawnSourceAuditTest`（1）、`NativeTournamentMatchEndEconomySourceAuditTest`（1）、`NativeWorldGroupHealingSourceAuditTest`（2） | 舊實作字串找不到，例如 `positionX = deathX;`、`phase = Phase.RESULT;`、`long spellRequestNow`、`useGroupHealingSpell(payload, worldGroupHealing)` | 原始碼文字形狀改變。候選裡有對應的呼叫（如 `this.useGroupHealingSpell(`，第 29981 行），但變數名與結構都不同，需要逐條確認順序語意 | **待分析**（結論第 4 點） |
| `GameHubCharacterTest`（1）、`GameHubChannelTest`（1 E）、`GameHubAccountOwnershipTest`（1）、`ClientSessionNativeRentalTownCitizenshipTest`（1） | `invalid source-qualified character identity`，stack 為 `GameHub.disconnect → NativeSummonRetirement.beforeDisconnect → Binding.capture → sourceAccountIdentity` | **程式缺陷（候選），待裁定**（結論第 1 點）。正式 0.6.71 的 disconnect 沒有這段呼叫；正式流程中登入前就斷線也會走到這裡 | 未改。改測試 fixture 只會掩蓋缺陷 |
| `ClassicWorldMonsterCatalogTest`（1） | `invalid classic monster release` | **程式缺陷（候選），待裁定**（結論第 2 點）。javap：候選 `ifeq`、正式 `ifne` | 未改 |
| `GameHubChannelTest`（5 F） | matchmaking 結果 0/1/2/3 不符；`opcodeOnlyNativeStart` 期望 `COUNTDOWN_STARTED`，實際 `NONE` | **待分析**：候選 GameHub（B09）的配對／開局規則有變。要對照正式版判斷是測試過時（正式版同候選）還是候選缺陷 | 未改 |
| `ClientSessionClassicWorldDeathPenaltyIntegrationTest`（16） | 封包序列缺少期望的封包；經驗值不同（例如 239718 對 244276、157572 對 161020）；掉落件數 0/1 | **待分析**：死亡懲罰、經驗值與掉落屬 B（ClientSession），要對照正式版與候選的公式 | 未改 |
| `ClientSessionRentalTownDirectoryTest`（2）、`NativeLitoRentalTeleportEndpointCatalogTest`（1） | `SocketTimeoutException: Read timed out`（等不到期望的封包） | **待分析**：可能是候選版租屋城鎮流程的封包不同，也可能是缺陷 1 的連鎖效應 | 未改 |
| `ClientSessionWorldCkewEntryTest`（1）、`ClientSessionWorldRescueFollowerTest`（1） | `native first talk starts quest without HTML` 等斷言失敗 | **待分析** | 未改 |
| `TournamentPresenceTest`（1）、`ModernNavigationSessionTest`（1） | 我改寫的版本碰到缺陷 1 | 改寫測試的 fixture 是未登入的 session | 拿掉對未登入 fixture 的 disconnect（第 2 次 commit） |

小計：
- 已修或預期已修：7 類 14 項，另加我自己改寫的 2 項。
- 候選缺陷待裁定：6 項。
- 待分析：約 33 項，含 WaterDragon／PcActionReward 中非拆檔的部分。
- 第 3 輪 CI（commit `562bdb9f`）結果見「CI 結果」。

## 推測

- 結論第 3 點的死鎖風險：只是從 thread dump 的取鎖順序推論出來的；正式環境中有沒有 hub → population 的 client 執行緒路徑，還沒有逐一查證。
- 「待分析」各項中，有一部分可能只是缺陷 1 的連鎖效應（例如 Read timed out），修好缺陷 1 之後要重跑才能確認。
- 第 3 輪預期仍會失敗的：WaterDragon／PcActionReward（非拆檔部分）、待分析各項、候選缺陷 6 項。

## 待驗證

- `mvn -B test` 全綠：**未達成**。
- 實機回歸：登入、大廳、技能、進房、戰鬥結算、退出，以及 `b09-shape-diffs.md` 的 184 項形狀差異，交 M2 離線回歸與 T020-close（#31 全套），由使用者或 Codex 在本機執行。
- 未啟動伺服器、登入器、遊戲；未連任何 DB。

## CI 結果

| run | commit | Server tests | 備註 |
|---|---|---|---|
| 42876069149 | `0e957635` | 取消（卡住超過 24 分鐘） | 主分支同一個 job 約 8.5 分鐘；跑到第 1,005／1,271 個測試類別時死鎖 |
| 42882305652 | `28247dd8` | **失敗**：6048 run，38 F，11 E，117 skipped | 含暫時診斷；thread dump 定位死鎖 |
| （第 3 輪） | `562bdb9f` | 執行中，結果見 PR #315 | 本輪的逐案修正 |

其他 job：`Server (Java 17 / Maven)`、`Launcher`、`Build-Package.ps1 zip`、`Semantic gap (non-blocking)` 兩輪都是 pass。

## 產出路徑

- 程式：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`（303 個 B 類別、Accounts.java 拆分、刪除 ModernCaptureLog.java）
- 測試：`TournamentPresenceTest`、`ModernNavigationSessionTest`、`ClientSessionLitoRoomInvitationTest`、`NativeMagicDollTest`、`ProtocolBattleTest`、`BattleSpellCatalogTest`、`ClientSessionClassicWorldMonsterAggressionIntegrationTest`、`NativeHotelSqlFirstPersistenceSourceAuditTest`、`NativeCastleSecuritySettlementSourceAuditTest`、`WaterDragonSqlFirstPersistenceSourceAuditTest`、`PcActionRewardSqlFirstSourceAuditTest`；刪除 `ModernCaptureLogTest`
- 清單：`docs/combat-merge/{exceptions-exempt,extra-methods-approved}.txt`、`docs/combat-merge/b09-shape-diffs.md`、`docs/server-source-gap.md`
- 證據：`docs/reports/issue-138/`（land.py、throws_fix.py、verify.py、shape_check.py、approvals_gap.py、mvn.ps1、semantic-summary.json、semantic-commands.json、b-approved-exceptions-exempt.txt）
- 分支 `task/138`，PR #315

## 給下一棒的提示

1. **主控**：先裁定結論中的 6 點。缺陷 1、2 若決定修，B 類別會偏離候選，需要 deviation／method-diff 核准。建議做法：
   - 缺陷 1：`beforeDisconnect` 在 session 沒有選擇伺服器或帳號時直接略過。
   - 缺陷 2：把 `ifeq` 改回正式版的 `!=` 檢查。
2. **下一輪 code**：
   - 依第 3 輪 CI 結果，處理「待分析」各項：先修缺陷 1，再看 Read timed out 類是否跟著消失。
   - DeathPenalty（16 項）與 GameHubChannel（5 項）要對照正式 JAR 的行為：用 javap 比對 `ClientSession`／`GameHub` 的對應方法在正式版與候選版是否相同。相同就判為測試過時，不同就判為候選修改，再看是否符合戰鬥同源化的目標。
3. **本機限制下的除錯方法**：CI 卡住時，可以像本輪一樣在該測試類別暫時加上 `@Timeout(threadMode = SEPARATE_THREAD)`，並在 `@AfterEach` 用 `ThreadMXBean.dumpAllThreads` 取得 dump，取得後要移除。日誌在 artifact `server-test-evidence` 的 `mvn-test.log`；`gh run view --log` 會被超長十六進位行截斷，不要用它看。
4. **T020-close**：B 範圍對候選的比對用 `verify.py` 的 `b-approved` 模式。整顆 JAR 對正式版比對時，只能套用非 B owner 的核准條目，否則 `NativeLightAction$Batch#emit()V` 的 method-diff 會對不到，工具 exit 1。
5. B 批次是各自對候選 JAR 編譯的，**只靠批次驗收看不出跨批次 checked exception 的不一致**。之後若再用暫存區流程，最後要整體跑一次 `javac`。
