# Issue #418 — T142：回歸工具的歷史 class 位元組守門改成 Maven 相容

分支：`task/418`｜階段：code｜執行：claude-local（本機只做靜態修改、`py_compile` 與唯讀 Python 解析；單元測試交給 CI）

## 結論

- 新增**明確選用**的 `--class-identity {exact,maven-normalized}`（預設 `exact`）。候選（不加旗標）的行為與原本相同：位元組完全相同才過。
- Maven 模式要明確指定，而且會寫進收據。兩道守門的作法：
  - **D1**（GameStateStore 觀察 overlay）：overlay 改成由**受測 JAR 自身**產生，並驗證 overlay 只多出探針 hook。
  - **D2**（corpse／vital fixture allowlist）：改用**正規化 class 比對**。原 allowlist 斷言照原文保留。
- 靜態預測（以 #341 實際的 Maven JAR、歷史 fixture 解析得出）：
  - D1：新模式下預期**通過**。
  - D2：`NativeDeCorpseClock$1` 正規化後相同，**通過**；corpse／resurrection／vital 三組通過守門。
  - D2 的 character／composition 兩組會**照樣失敗**，失敗點在 `NativeDeCachedFactoryServices`。這是真實的原始碼差異：`src/main` 的 `NativeDeInitializerKernel.Name` 少了 `final`，不是工具問題（詳見下文）。

## 做了什麼

| 檔案 | 變更 |
|---|---|
| `tools/combat-continuation/class_identity.py`（新） | 純 Python 的 class 檔解碼與正規化，不需 JVM：常數池索引全部換成符號值；`ldc_w`／`goto_w`／`jsr_w` 併入窄版；分支、switch、例外表偏移改成指令序號；只丟掉 `SourceFile`、`SourceDebugExtension`、`LineNumberTable`、`LocalVariableTable`、`LocalVariableTypeTable`、`StackMapTable`；`NestMembers`／`PermittedSubclasses`／`Exceptions`／`InnerClasses`／`BootstrapMethods` 只忽略表的順序，條目內容（含 inner class flags）照比。提供 `compare()` 與 `compare_overlay()`（剝除對指定探針類別的 `invokestatic` 後比對）。 |
| `tools/combat-continuation/regression_support.py` | `Options` 新增 `--class-identity`，並由 `child_args()` 轉傳。`build()` 在 Maven 模式下：必須有 `--jar`；JAR SHA 若等於該 stage 的 build.json 收據就拒絕（候選必須走 exact）；在 build 寫入 `class_identity{mode, stage_receipt_runtime, rule}`。`adapt_driver()` 新增 `adapt_class_identity()` 改寫（見 D1）。新增 `overlay_source`、`assert_class_identity`、`class_identity_changed`、`adapt_components_identity`。 |
| `tools/combat-continuation/full_regression.py` | 對 `replay-native-de-item-state-components.py` 的**改寫副本**呼叫 `adapt_components_identity`。manifest 記錄 `class_identity_mode`，每列附 `class_identity` 收據的 pin。 |
| `tools/combat-continuation/test_class_identity.py`（新） | 離線單元測試，用合成 class 與 zip（見「怎麼驗證」）。 |
| `.github/workflows/build.yml`（`combat-regression-tools` job） | `py_compile` 加入兩個新檔；新增「Offline class identity guard tests」與「Entry points accept --class-identity」兩步。 |

**沒有改的**：`src/main`；任何 probe 的戰鬥斷言或故障注入；`cancellation-battle-runtime/test_boundaries.py` 等 41 個含 GameStateStore 守門的原始驅動（改寫只發生在 `adapt_driver` 產生的副本）；`--resources` 下未入 git 的 `tools/resource-database/build/replay-native-de-item-state-components.py`。

## 裁定：Maven 產物的身分驗證方式

守門原意是：probe 只替換預期的 class，其餘與 base 相同。

### 候選：`exact`（預設，不變）
- `assert_class_identity` 在 exact 模式就是原式 `assert a.read(n)==z.read(n)`。overlay 來源仍是 `old`（歷史 base）。
- `class_identity_changed` 在 exact 模式就是原式 `sha256(current)!=row['sha256']`。
- 不寫額外收據，也不改 build.json 內容。
- 補充：#341 用的候選 `50635f7e` 並不是 `cancellation-battle-runtime/build/build.json` 收據（`8d26c9d7`）。所以模式不能用「`--jar` 是否等於收據」自動推斷，否則候選會被誤判成 Maven。因此改成**明確旗標**；反過來，Maven 模式拒絕收據 JAR。

### D1：GameStateStore 觀察 overlay → Maven 模式「以受測 JAR 自身為 base」
- 原流程：`ObserveDeItemTransaction` 從**歷史 base** 的 GameStateStore 產生插樁 overlay，放在 classpath 最前面蓋掉受測 JAR 的同名 class。所以必須確認受測 JAR 的 GameStateStore 與 base 相同，否則 overlay 等於偷換 class。
- 靜態解析（已驗證，見下）顯示 Maven 的 GameStateStore 比 base 多了 `rankingPeriodKeyAt(J)I`、`validRankingPeriodKey(I)Z`，`SettlementSql`、`SharedDatabase` 會呼叫這兩個方法。所以「正規化比對 base」也會失敗。而且從 base 產生的 overlay 確實會把這兩個方法蓋掉，舊守門擋得對。
- 新作法（只在 Maven 模式）：
  - 改寫副本把 `'ObserveDeItemTransaction',old,overlay` 換成 `overlay_source(old,jar,build)`，Maven 模式回傳 `jar`。
  - 原 assert 改成 `assert_class_identity(a,z,name,build,run/'class-identity.json',overlay)`，檢查兩件事：
    1. overlay 目錄的檔案清單必須**剛好**只有 `local/lito381/server/GameStateStore.class`，否則 `overlay-replaces-unexpected-classes`。
    2. overlay class 剝除對 `local/lito381/server/DeItemTransactionProbe` 的 `invokestatic` 之後，必須與受測 JAR 自己的 GameStateStore 正規化相同，否則 `overlay-differs-beyond-probe-hooks`。至少要剝除到一個 hook，否則 `overlay-has-no-probe-hook`。比對時只排除 `max_stack`（ASM `COMPUTE_MAXS` 會重算）；`max_locals`、每條指令、運算元、分支目標與例外表都照比。
- **為何仍抓得到 probe 意外替換非預期 class**：overlay 只要多一個檔就失敗。overlay 的 GameStateStore 只要有 hook 以外的任何差異就失敗；例如有人又用 base 產生 overlay 去蓋 Maven 的 class，會因為少了 Maven 的那兩個方法而失敗，單元測試有這一案。`ObserveDeItemTransaction` 本身的反向檢查（移除 hook 後必須等於來源 class）也仍在。
- base 與 Maven 的正規化差異寫進收據的 `historical_base_comparison` 留存備查，不作為放行依據。

### D2：corpse／vital fixture allowlist → `maven-normalized`
- 採用**包裝層改寫**：`full_regression.py` 原本就逐一改寫 `resources` 下 20 個驅動的副本，這裡加一處 `adapt_components_identity`，把 `changed=current!=row['sha256']` 換成 `changed=class_identity_changed(native.read(name),qualified.read(name),row['sha256'],name,mode,receipt)`。字串必須恰好出現一次，否則立刻失敗。
- Maven 模式的 `changed` 定義為「與 qualified 歷史 class 正規化後仍不同」。先確認 qualified class 的 SHA 等於 fixture 紀錄。原本的 `if changed:assert name in [...3 個 helper...]` **原文保留**，所以不在清單內的 class 只要正規化後仍不同就照樣失敗。
- 不影響候選的理由：exact 模式的改寫結果在語意上等同原式，而且不寫收據。候選不帶旗標，`mode='exact'` 直接寫死在副本裡。
- 未入 git 的原始腳本沒有改，也沒有複製進 git。

### 收據格式（Maven 模式才會產生）
- D1：`<out>/targeted/<stage>/boundaries/class-identity.json`
- D2：`<qualification-run>/replay-native-de-item-state-components.py.class-identity.json`，manifest 每列以 `class_identity` pin 住。

```json
{"class_identity_mode":"maven-normalized","rule":"…","status":"passed|failed|recorded",
 "entries":[{"name":"…","mode":"…","reference_sha256":"…","compared_sha256":"…",
   "reference_normalized_sha256":"…","compared_normalized_sha256":"…",
   "result":"byte-identical|normalized-identical|normalized-differ|runtime-class-plus-probe-hooks-only|…",
   "differences":["method X differs", "..."],"gating":"assert|caller-allowlist"}]}
```

D2 的 `status` 是 `recorded`：是否放行由驅動裡原本的 allowlist 斷言決定。D1 的 `status` 是 `passed`／`failed`，失敗時 AssertionError 會列出差異方法與收據路徑。

## D1／D2 在新模式下的預期判定（靜態推論）

解析時只用 Python 讀取既有 JAR 與 fixture，未執行 JVM。腳本在 `scratch/issue-418/inspect_identity*.py`，結果在 `scratch/issue-418/identity-static*.json`（不進 git）。

| 守門 | class | 歷史 SHA | Maven SHA | 新模式判定 |
|---|---|---|---|---|
| D1 歷史比對（只記錄） | GameStateStore | `5dea2cee…b633`（base `f8e1ac47…a366`） | `17e501ce…9f99` | `normalized-differ`：Maven 多了 `rankingPeriodKeyAt(J)I`、`validRankingPeriodKey(I)Z` |
| D1 overlay（作為放行依據） | GameStateStore | — | — | **預期通過**（overlay 由 Maven JAR 產生；檢查邏輯已用 #341 實際 ASM overlay 驗證） |
| D2 corpse | NativeDeCorpseClock$1 | `6bb3feac…c5d0` | `a25fb98b…91ac` | `normalized-identical` → 通過 |
| D2 corpse／resurrection／vital（19／16／16 個打包 class） | 其餘 | — | — | 非 allowlist 的 class 全部 byte- 或 normalized-identical；只有 allowlist 的 3 個仍不同 → **通過守門** |
| D2 character／composition（各 10 個） | NativeDeCachedFactoryServices | `1ee60e46…ffd9` | `0e9d85e9…f15f` | **`normalized-differ`（class attribute InnerClasses）→ 照樣失敗** |

最後一列的原因：歷史 class 對 `NativeDeInitializerKernel$Name` 記錄的 inner flags 是 `0x18`（static final），Maven 是 `0x08`（static）。`src/main/java/local/lito381/server/NativeDeInitializerKernel.java:566` 寫的是 `static class Name {`，少了 `final`。這是 `src/main` 與歷史 base 的真實原始碼差異，本任務不得改 `src/main`，也不自行豁免。

### 已驗證
- 正規化器能解析 base `f8e1ac47…` 與 Maven `64f4e3eb…` 兩個 JAR 的全部 class，解析錯誤 0。
- hook 剝除邏輯：#341 兩份實際 overlay（`retest/results/{candidate,maven}/…/memory-observer/`，都由 base 產生）對 base GameStateStore 判定為 `runtime-class-plus-probe-hooks-only`，剝除了 `beforeSave`、`beforeFlush` 各 1 個。
- 同一份 overlay 對 Maven GameStateStore 判定為 `overlay-differs-beyond-probe-hooks`，差異是那兩個方法。這證明新守門能抓到「用 base overlay 偷換 Maven class」。
- 上表各 class 的 SHA 與正規化結果，以及 `NativeDeInitializerKernel.Name` 少 `final`。
- `py_compile`：`regression_support.py`、`class_identity.py`、`test_class_identity.py`、`full_regression.py`、`candidate_regression.py`、`test_receipt_entrypoints.py`、`test_boundaries.py` 全部通過。
- 靜態掃描：41 個含舊守門的驅動，改寫後 `build`／`jar`／`old`／`run` 都已在前文定義，exact 模式不會產生 NameError。
- `git diff` 沒有任何 assert 或故障注入被刪除。`full_regression.py` 只有兩行被改寫成加欄位，原 assert 仍在 `test_boundaries.py:29`。

### 推測
- D1 實跑會通過：前提是 `ObserveDeItemTransaction` 能在 Maven 的 GameStateStore 找到唯一的 `saveEconomyState(String,long,Map,Map)` 與 `save()` 呼叫。`src/main` 版本還沒逐條確認，該工具找不到時會自己 AssertionError。
- 12 案 CancellationBattleFault 的行為結果未知，由 T145 決定。
- D2 會比 #341 多跑完 corpse／resurrection／vital 三個 probe，接著停在 character 組。

### 待驗證（交給 CI／T145）
- CI `combat-regression-tools` job 的新測試（本機依規定未執行 unittest）。
- T145 用 Maven JAR 實跑 05（targeted）、08（existing-affected）時的實際收據與判定。

## T145 的指令差異

只有 **Maven** 的各入口加上 `--class-identity maven-normalized`；**候選指令完全不變**。#341 的 `run_retest.py` 形式：

```diff
- [python, -B, -X, utf8, <driver>, *extra, --jar, target/LiTo381-Server.jar, --out, …, --resources, …, --config, …]
+ [python, -B, -X, utf8, <driver>, *extra, --jar, target/LiTo381-Server.jar, --out, …, --resources, …, --config, …, --class-identity, maven-normalized]
```

`candidate_regression.py` 會經由 `child_args()` 自動轉傳給 `full_regression.py`、`affected_regressions.py`、`reconcile_replay.py`。

## 給下一棒的提示

1. **T145（Codex／本機實跑）**：
   - Maven 加上述旗標，候選照舊。
   - 檢查 `targeted/cancellation-battle-runtime/boundaries/class-identity.json`，`result` 應為 `runtime-class-plus-probe-hooks-only`。
   - 檢查第 4 組 components 收據。預期在 character 組因 `NativeDeCachedFactoryServices` 失敗，錯誤是原 allowlist 的 `AssertionError`，收據 `differences` 為 `class attribute InnerClasses`。
2. **需要裁定（建議 Claude 雲端開 code 任務）**：要讓 D2 的 character／composition 組在 Maven 下通過，正途是把 `src/main/.../NativeDeInitializerKernel.java:566` 改為 `static final class Name`，回到歷史語意。改後要重建 Maven JAR，並確認 `NativeDeInitializerKernel$Name` 本身也與歷史一致。不建議在工具端放寬 InnerClasses flags。
3. 若 T145 發現 Maven 的 `saveEconomyState` 結構不同，導致 `ObserveDeItemTransaction` 失敗：那是 `src/main` 與歷史 GameStateStore 的差異，不要改 probe。

## 產出路徑
- `lito伺服器/LiTo381-Server/tools/combat-continuation/class_identity.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/test_class_identity.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/regression_support.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/full_regression.py`
- `.github/workflows/build.yml`
- `docs/reports/issue-418.md`
- 本機唯讀證據（不進 git）：`scratch/issue-418/`
