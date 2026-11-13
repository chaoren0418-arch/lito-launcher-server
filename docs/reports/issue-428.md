# Issue #428 — T146：#421「只有 Maven 失敗」的 D2 class 守門與 F8 probe 陣列越界

分支：`task/428`｜階段：code｜執行：claude-local（本機只做靜態解析、`javac` 編譯檢查、`py_compile`；單元測試與實跑交給 CI／T145 類重測）

## 結論

| 項目 | 根因 | 處置 | 是否產品差異 |
|---|---|---|---|
| **D2**：`NativeDeCachedFactoryServices` 的 `InnerClasses` | 歷史 class 把 `NativeDeInitializerKernel$Name` 的 inner flags 記成 `0x18`（static final），Maven 是 `0x08`（static）。這一列描述的是**別的類別**（`Name`），不是本類別。 | 工具端加一條**窄化**規則：只容許「描述他類的列」只差 ACC_FINAL，而且比對值必須等於受測 runtime 內 `Name` 自己宣告的 flags。 | 否。本類別的程式、欄位、方法全部正規化相同。 |
| **D2 附帶**：`NativeDeInitializerKernel.Name` | `Name` 不能加回 `final`：`src/main` 的 `NativeDeInstanceName extends NativeDeInitializerKernel.Name`，候選 `50635f7e` 也有同一子類別，`Name` 在候選裡同樣是 `0x20`（非 final）。 | **不改 `src/main`**。#418 報告建議改成 `static final class Name`，照做會編譯失敗。 | `Name` 本身確實演進了（多了 setter、非 final），但候選與 Maven 一致，也不在 fixture 守門的 class 清單內。 |
| **F8**：`NativeLightCastProbe.collect` 的 `Index 139 out of bounds for length 139` | fixture 的競態：mode 18 用真實排程時鐘。`tick()` 在 hub 鎖內移除 lease，**放鎖之後**才在 clock 執行緒上 publish 移除封包。probe 只等到「lease 為空」就讀 wire，剛好讀到寫到一半的 frame（只寫了 1 byte 長度欄）。 | 修 fixture：在讀 wire 之前，對單執行緒的 clock 送出一個空任務並等它完成，當作 happens-before 屏障。原斷言一條沒改，也沒有吞例外。 | 否。寫出路徑上的類別／方法兩邊正規化相同（見下）。 |

## 做了什麼

| 檔案 | 變更 |
|---|---|
| `tools/combat-continuation/class_identity.py` | 新增 `ACC_FINAL`、`INNER_CLASS_FLAG_RULE`、`declared_inner_flags()`、`inner_class_flag_evidence()`。`compare()` 新增可選參數 `runtime`（受測 JAR 的 ZipFile）。只有當正規化後**唯一**的差異是 `class attribute InnerClasses` 時才會走新規則；通過時結果為 `normalized-identical-inner-class-flags`，並附 `inner_class_flag_evidence`；不通過時保留 `normalized-differ`，另附 `inner_class_flag_check` 說明原因。 |
| `tools/combat-continuation/regression_support.py` | `assert_class_identity` 與 `class_identity_changed`（新增 `runtime=None` 參數）把 runtime 傳給 `compare`。`adapt_components_identity` 的改寫副本改傳 `runtime=native`。`CLASS_IDENTITY_RULE` 收據文字附上新規則。 |
| `tools/combat-continuation/test_class_identity.py` | `make_class(inner=...)` 可產生 `InnerClasses`。新增 `InnerClassFlagTests`（11 個斷言點，正例與反例）與 components 迴圈測試一案（見下）。 |
| `tools/combat-continuation/cancellation-battle-runtime/CancellationBattleReplacementProbe.java` | mode 18 在原本的 `"real scheduled expiry"` 斷言之後加一行：`need(clock.submit(()->{}).get(4,TimeUnit.SECONDS)==null,"real expiry publication drained before collecting wires")`。 |

**沒有改的**：`src/main`；任何 probe 斷言（mode 18 的 `a.contains(removed)`、`b.contains(removed)` 等照舊）；`--resources` 下未入 git 的 `NativeLightCastProbe.java` 與 components 原始驅動；exact 模式的行為（候選照舊比位元組）。

## D2 規則（何時視為相同）

在 maven-normalized 模式下，以下條件**全部**成立才放行：

1. 正規化後只差 `InnerClasses` 一項（任何方法、欄位、flags、其他屬性有差異，就不會套用此規則）。
2. 兩邊的 (inner, outer, simple name) 集合完全相同，不可多也不可少；只有表的順序可以不同（#418 原本就忽略順序）。
3. 每一個 flags 不同的列，`inner` 都不是本類別自己。本類別自身的列（例如 `NativeDeInitializerKernel$Name.class` 自己那一列）**永遠不放行**。
4. flags 的 XOR 只能是 `0x0010`（ACC_FINAL）。public／private／protected、static、interface、abstract、synthetic、annotation、enum 都必須相同，也就是可見性相同。
5. 受測 runtime 必須真的含有該 inner class，而且它在自己 `InnerClasses` 中宣告的 flags 等於比對列的值。這一點證明比對列只是 runtime 實際類別的忠實副本；ASM patch 的候選留下的是舊副本。

理由：JVMS §4.7.6 規定，參照類別裡描述他類的 InnerClasses 列不參與本類別的語意；`Class.getModifiers()` 讀的是 inner class 自己的那一列。守門要確認的是「本類別沒變」，而條件 1 已經保證本類別的程式碼與宣告完全相同。

### 單元測試（離線，合成 class，CI `combat-regression-tools` 的 `unittest -v test_class_identity` 會跑）

- 正例：只差 ACC_FINAL、表順序反轉、加 debug／常數池重排，且 runtime 的 `Name` 宣告 `0x08` → `normalized-identical-inner-class-flags`，evidence 欄位逐項斷言。
- 正例（不受影響）：只有表的順序不同 → 仍是一般的 `normalized-identical`；exact 模式 → `bytes-differ`。
- 反例：
  - 沒給 runtime → 拒絕。
  - runtime 缺 `Name` → 拒絕。
  - runtime 的 `Name` 仍是 `0x18`（比對列不是它的副本）→ 拒絕。
  - 可見性改變（`0x09`→`0x08`、`0x18`→`0x09`、`0x18`→`0x0a`）→ 拒絕。
  - inner class 集合多一列 → 拒絕。
  - 本類別自身的列只差 final → 拒絕。
  - InnerClasses 加方法也有差異 → `normalized-differ` 並列出方法。
- components 迴圈（`adapt_components_identity` 改寫後實際 `exec`）：
  - Maven + runtime 含非 final 的 `Name` → `changed=False`。
  - runtime 的 `Name` 仍 final → 原 allowlist 斷言 AssertionError。
  - exact 模式 → AssertionError。

## F8 證據

- **陣列長度從哪來**：`collect` 對 `wire.raw.toByteArray()` 逐 frame 解析。每個 frame 讀 `raw[at]`、`raw[at+1]` 當長度。長度 139、索引 139，代表 `at=138` 時尾端只剩 1 byte。它不是 fixture 預期的封包數或欄位數，而是**半個 frame header**。
- **為何會有半個 header**：`Protocol.writeFrame` 依序做 `write(total&0xFF)`、`write(total>>>8)`、`write(payload)`、`flush()`，`SourceActionPairProbe.Wire` 每次 `write` 都即時 append 到 `raw`。所以另一執行緒寫到一半時去快照，就會看到 1 byte 的 header。
- **誰在寫**：`NativeCancellationBattleRuntime.tick`（`tools/.../NativeCancellationBattleRuntime.java:57-68`）在 `synchronized(s.hub)` 內 `it.remove()` lease，接著 `stopEmpty`，離開鎖之後才 `outputs.forEach(Notice::publish)`。`Notice.publish` 若持有 hub 鎖會直接拋 `IllegalStateException`，所以「鎖外發布」是產品的設計不變式。publish 透過 `NativeRolePublication.completion` 同步在 clock 執行緒上執行（`src/main/.../NativeRolePublication.java:18-39`），沒有再轉交給其他執行緒。
- **probe 的同步點不足**：原本 mode 18 只等 `state.leases.isEmpty()`（在 hub 鎖內輪詢，每 10 ms 一次），接著立刻 `collect`。在 lease 移除到 publish 寫完之間有一段時間窗，兩顆 JAR 都有；候選這次沒踩到，Maven 這次踩到了。
- **兩顆 JAR 的寫出路徑相同**（用本 repo 的 `class_identity` 正規化比對 `reference/candidate-50635f7e.jar` 與 #421 的 Maven `target/LiTo381-Server.jar`，SHA `84ba0cde…`）：
  - `NativeCancellationBattleRuntime` 及其 `$Lease`／`$Notice`／`$State`／`$Viewer`、`NativeCancellationBattleAction`、`NativeCancellationBattleAction$Target`：全部 `normalized-identical`。
  - `Protocol.writeFrame`、`ClientSession.sendEncryptedFrame`、`ClientSession.sendEncrypted`：方法正規化相同。
  - `ClientSession.nativeSelfOriginalSend`：唯一差異是 Maven 多一個未使用的區域變數（`dup; istore 4`），呼叫與分支完全相同。
  - `docs/combat-merge/b09-shape-diffs.md` 沒有收錄上述任何方法（搜尋 `nativeSelfOriginalSend|NativeRolePublication|NativeSourcePlayerActionLock|writeFrame`，0 筆）。
- **修法為何不是放寬**：Clock 是 `ScheduledThreadPoolExecutor(1)`，單一執行緒依序執行。到期 tick 正在跑時，送進去的空任務一定排在它之後；`Future.get` 返回時，那次 tick（含 publish）已經結束，`raw` 也已完整。逾時 4 秒會拋 `TimeoutException`，不吞。之後原本的內容比對（`a.contains(removed)`、peer 依 lane 判定）全部照舊。

## 已驗證

- D2 實際差異（唯讀 Python 解析）：
  - 歷史 character 組的 qualified runtime 是 `candidate-NATIVE-DE-CHARACTER-experimental.jar`（`20f2c3a5…`）。其中 `NativeDeCachedFactoryServices` 為 `1ee60e46…`，Maven 為 `0e9d85e9…`。
  - 13 列 InnerClasses 中，只有 `NativeDeInitializerKernel$Name` 一列不同（`0x18` → `0x08`）。
  - 候選 `50635f7e` 的 `NativeDeCachedFactoryServices` 與歷史位元組相同（仍記 `0x18`），但候選自己的 `NativeDeInitializerKernel$Name.class` access 是 `0x20`、自身列是 `0x08`，和 Maven 一致。
- `Name` 有子類別：`src/main/java/local/lito381/server/NativeDeInstanceName.java:10` `extends NativeDeInitializerKernel.Name`；Maven 與候選 JAR 都含 `NativeDeInstanceName.class`，歷史 qualified JAR 沒有。
- 用新的 `class_identity` 對 Maven JAR 靜態重演 components 五組守門（`scratch/issue-428/predict_components.py`，輸出 `predict-maven.txt`）：corpse 19、resurrection 16、vital 16、character 10、composition 10，**FAIL 全部為 0**。
  - character／composition 的 `NativeDeCachedFactoryServices` 判為 `normalized-identical-inner-class-flags`。
  - 其餘不同的只有 allowlist 內的 3 個 helper。
- F8：上一節所列 class／方法的正規化比對結果。
- 編譯（只編譯、未執行）：修改後的 `CancellationBattleReplacementProbe.java` 與 `CancellationMaterialProbe.java`，以 `test.py` 相同的 fixture classpath 分別對 Maven JAR 與候選 JAR 執行 `javac -g:none -implicit:none`，兩次 exit 0。
- `py_compile`：`class_identity.py`、`regression_support.py`、`test_class_identity.py` 通過。

## 推測

- F8 改後 Maven 的 276 案會通過 cancellation-battle-runtime tests，並繼續往 targeted 後續組前進。前提是 Maven 在 mode 18 之後沒有其他真實差異。
- D2 改後 Maven 的 08（existing-affected）第 4 組會通過，往第 5 組以後前進；之後是否卡在其他組（例如候選也失敗的 F5）未知。

## 待驗證（交給 CI／下一輪本機重測）

1. CI `combat-regression-tools` 的 `unittest -v test_class_identity`（本機依規定未執行 unittest）。`mvn -B test` 不受影響，因為沒有改 `src/main`。
2. 實跑重測（T145 形式，Maven 加 `--class-identity maven-normalized`）：
   - 05 targeted：`cancellation-battle-runtime/tests` 應為 276 案通過。
   - 08：components 收據中 `NativeDeCachedFactoryServices` 應為 `normalized-identical-inner-class-flags`，`inner_class_flag_evidence` 的 `runtime_declared_flags=8`。
3. 候選也要用修改後的 probe 重跑 05，確認屏障不改變候選結果（預期仍 276 案通過）。

## 給下一棒的提示

- **不要**照 #418 報告的建議把 `Name` 改成 `static final class`：`NativeDeInstanceName` 繼承它，加 final 會編譯失敗；候選也是非 final。
- 若之後其他 fixture 守門又遇到「只有 InnerClasses 不同」，先看收據的 `inner_class_flag_check`：寫出的原因（可見性、集合、本類別列、runtime 宣告不符）都是真差異，不要再放寬規則。
- `NativeLightCastProbe.collect` 遇到半個 header 會拋 AIOOBE，而不是 `need("complete original Light frame")`；該檔在未入 git 的 `tools/resource-database/build/`，本任務沒有改。其他使用真實排程時鐘、並在鎖外 publish 的 probe，也應該用同樣的 clock 屏障再讀 wire。
- 本機唯讀證據（不進 git）：`scratch/issue-428/`（`inspect_inner.py`、`predict_components.py`、`predict-maven.txt`、`dump_methods.py`、`diff_classes.py`、`javac-f8*/`）。

## 產出路徑

- `lito伺服器/LiTo381-Server/tools/combat-continuation/class_identity.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/regression_support.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/test_class_identity.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-runtime/CancellationBattleReplacementProbe.java`
- `docs/reports/issue-428.md`
