# Issue #62／T020-B01 審查回覆與 #203 依賴阻塞

## 結論

**B01 正式驗收尚未通過；依最新主控裁定，Java 不改，等待 #186、#203 合併後重驗。** 本輪（2026-11-06）已補回 Forest 的 B 類完整正規化差異與核准證據，更新下一棒提示。前輪正式 CLI 結果維持 **20 SAME、1 SAME_EFFECTS、0 SAME_PATHS、1 DIFF**，不是本輪重新執行所得。

剩餘 `NativeWeaponBreakTargetLifecycle` 的 lambda 呼叫點配對已由[最新主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6662268131)交給 #203，無須再次要求相同裁定。直接診斷的 87 條路徑相同不能取代正式 CLI 全批通過。

## 本輪審查回覆（已驗證）

- 在既有 `task/62` 從 `e2753408400367b14306a79f2e0403304537952b` 合併最新 `origin/main`：`422f92e563c8de2201f7caaf5fcc228bd5623438`，合併提交 `f94c24d7729837d7b54bcfb7257ea6d450980552`；未 checkout 或建立分支。
- `2026-11-05T17:12:06.772929+00:00` 查核 GitHub：#203 為 OPEN／task:queued，`task/203` 的全狀態 PR 查詢為 `[]`；#186 為 OPEN／task:review。最新 main 尚不含 #203。原始查詢命令、退出碼與回應保存在 `scratch/issue-62/dependency-check.json`。
- #62 本文仍列 `depends_on: [102, 132, 135, 153, 166, 174, 176]`，尚未加入最新裁定所列 186、203。這是派工依賴缺口，交主控／調度器修正。
- 本輪只修改本報告；17 個 B01 Java、核准清單與 semantic-diff／AsRecord 工具相對 `e275340` 均無差異，因此未用舊工具重跑未滿足依賴的正式驗收。
- 已核對前輪 JAR 與 classes：精確 22 個 entries，逐檔 bytes 相同，無缺漏或多出；17 個來源標頭齊全。前輪原始 JSON 的 `missingLeft`／`extraLeft` 均為空，Forest `apply` 逐方法狀態為 SAME_EFFECTS，原始命令包含 `--effects-methods` 與 `--exceptions-exempt`。
- 補回附錄 E：完整 Forest diff、B 類分類、核准行、逐方法 JSON 與原始證據 SHA-256。Forest Java 與核准範圍均未擴大。
- 本 PR 對最新 main 的完整範圍仍為 19 檔：B01 的 17 個 staged Java、`docs/reports/issue-62.md`、`docs/combat-merge/effects-approved.txt`。PR #79 描述應與本次推送 head 一併更新，明確保留驗收未通過及 #203 依賴。

以下編譯命令、日誌、診斷、雜湊及附錄 A–D 均是 **`e275340` 前輪原始證據**；本輪保留並核對，未宣稱是含 #203 的重驗結果。

## 前輪做了什麼（已驗證）

工作目錄為 `D:\lineage開發\3-lito開發\scratch\wt\task-62`，使用既有 `task/62`。開始時 HEAD 為 `63da8a2`，fetch 後合併 `origin/main` 的 `c72453957ebe47e00ad024e874bbe31859457799`，合併提交為 `151c5e959232e73df2f00ce4f61ab5f798666e48`。工具包含 #174 與 #176 第二次修正。fetch／merge 經提權更新共用 Git 中繼資料。

| 檔案 | 本輪修改 |
|---|---|
| `NativeSourceDollAccepted.java` | `binding()` 使用獨立的 `plan.caster` 讀取，actor 引數內再賦值給 `casterActor`，供 `life()` 共用。target 同樣處理。 |
| `NativeWeaponBreakTargetLifecycle.java` | 兩個 monitor 分支均先通過 current 與 player 短路條件，再取得 targets stream、呼叫 `Objects.requireNonNull(context)`、建立 proof lambda 與執行 allMatch，最後檢查 npc。保留 `throws IOException` 並更新核准註解。 |
| `NativeCompanionWindAction.java` | `publish` 恢復一般 varargs，完整 stream/filter 運算放在第一個實參，讓 javac 的陣列配置維持候選順序。 |
| `docs/reports/issue-62.md` | 更新為本輪驗證、完整剩餘差異及下一棒提示。 |

本輪自行選定的 Java 表達方式包括 WeaponBreak 的條件區塊與 stream 區域變數，以及 Wind 將 stream 內嵌並移除已被 `c.players` 取用證明多餘的 null 檢查。Wind 正式驗證為 SAME。WeaponBreak 的順序、短路與 monitor 範圍以候選 javap 核對，直接逐方法診斷詳見下節。

17 個外層類均重新用 CFR 0.152 反編譯候選作為核對，17 次退出碼皆為 0。全部 staged 檔均保有指定 DECOMPILED 標頭。`NativeSourceCubePlacement$Draft` 維持 `@AsRecord`，轉換後為 SAME。

## 前輪怎麼驗證（已驗證）

使用 Microsoft JDK 17.0.17，重新編譯 main 中 semantic-diff 原始碼。註解獨立編譯，staged 只編譯 B01 的 17 個 Java，接著執行 `tools/as-record/as_record.py` CLI，最後打包 22 個 entry 進驗收 JAR。逐一核對 class 目錄與 JAR entry 都精確等於 `B01.verify_entries`。

所有正規化與逐路徑比較均使用預設啟用狀態。左側 classpath 指向候選。逐檔裁切 effects、extra-methods、metadata-exempt、exceptions-exempt、access-widen、layout、deviations 核准清單至 B01，只命中以下兩筆。

- `effects-approved.txt` 既有的 `NativeCompanionForestArea#apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V`。
- `exceptions-exempt.txt` 既有的 `NativeWeaponBreakTargetLifecycle#captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z`。

本輪核准清單採既有條目。`SAME_LAYOUT`、`DEVIATION`、`ORPHAN_IGNORED` 均為 0。CLI 退出碼為 0，同時明確回報 DIFF=1。

### 固定輸入及產物 SHA-256

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/target/issue-62/final-174-176/staged.jar` | `e05d3f432413ecc65aa3f4f5a1d68b196de634f954935230763b443bfa92501f` |
| `lito伺服器/LiTo381-Server/target/issue-62/dependency/cfr-0.152.jar` | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |

### 編譯、轉換與語意工具原始輸出

#### `jdk.log`

```text
openjdk version "17.0.17" 2025-10-21 LTS

OpenJDK Runtime Environment Microsoft-12574423 (build 17.0.17+10-LTS)

OpenJDK 64-Bit Server VM Microsoft-12574423 (build 17.0.17+10-LTS, mixed mode, sharing)


exit=0
```

#### `final-174-176-annotation.log`

```text
exit=0
```

#### `final-174-176-javac.log`

```text
Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.


exit=0
```

#### `final-174-176-as-record.log`

```text
as-record: scanned=22 converted=1


exit=0
```

#### `final-174-176-jar.log`

```text
exit=0
```

#### `final-174-176-tool.log`

```text
exit=0
```

#### `final-174-176-semantic.log`

```text
SUMMARY SAME=20 SAME_EFFECTS=1 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=1 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\final-174-176.json

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\final-174-176.md


exit=0
```

### 全部 22 個 entry

共同前綴為 `local/lito381/server/`。

| Entry | 狀態 |
|---|---|
| `NativeBlindSceneAction.class` | SAME |
| `NativeCompanionForestArea.class` | SAME_EFFECTS |
| `NativeCompanionRefScene.class` | SAME |
| `NativeCompanionWindAction.class` | SAME |
| `NativeControlledNegativeMaterial.class` | SAME |
| `NativePairedCastSafety.class` | SAME |
| `NativeSourceBatchTerminal.class` | SAME |
| `NativeSourceCubePlacement.class` | SAME |
| `NativeSourceCubePlacement$Draft.class` | SAME |
| `NativeSourceDollAccepted.class` | SAME |
| `NativeSourceDollAccepted$Pending.class` | SAME |
| `NativeSourceDollNpc.class` | SAME |
| `NativeSourceDollNpc$Scope.class` | SAME |
| `NativeSourceDollSkills.class` | SAME |
| `NativeSourceDollSkills$View.class` | SAME |
| `NativeSourceDollSlow.class` | SAME |
| `NativeSourceHealingCast.class` | SAME |
| `NativeSourceHealingCast$Cast.class` | SAME |
| `NativeSourcePendingAdmission.class` | SAME |
| `NativeSummonTerminalLifecycle.class` | SAME |
| `NativeWeaponBreakBattleCompanion.class` | SAME |
| `NativeWeaponBreakTargetLifecycle.class` | DIFF |

### 剩餘 DIFF 的診斷

正式 CLI 的輸出 Runnable 左側為 `lambda$captureOutput$8`，右側為 `lambda$captureOutput$6`，兩者完整描述子相同。`JarIndex.remapMethod` 使用 `ClassComparer.methodIdentity` 的內容雜湊作為 lambda 名稱，identity 包含線性指令及例外涵蓋形狀。補上指定 null 檢查所需的條件區塊與暫存，造成 identity 不同，三層 lambda 及其上層 captureOutput handle 因而連帶不相同。

另外在 scratch 建立 package-local 診斷程式，從同一對 JAR 以完整描述子取出輸出 Runnable，直接呼叫現有 `PathComparison.compare`，回傳 same=true、87 條路徑相同。此診斷只讀 class metadata 與 ASM 指令。正式驗收仍保留 DIFF=1。

```text
LEFT lambda$captureOutput$8(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V

RIGHT lambda$captureOutput$6(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V

IDENTITY_EQUAL false

DIRECT_PATHS Result[same=true, reason=path multisets equal (87)]


exit=0
```

#### 正式 CLI 的逐方法診斷

### `local/lito381/server/NativeWeaponBreakTargetLifecycle#lambda$captureOutput$6(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V` — ONLY_RIGHT

```text
only right
```

### `local/lito381/server/NativeWeaponBreakTargetLifecycle#lambda$captureOutput$7(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V` — ONLY_RIGHT

```text
only right
```

### `local/lito381/server/NativeWeaponBreakTargetLifecycle#lambda$captureOutput$8(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V` — ONLY_RIGHT

```text
only right
```

### `local/lito381/server/NativeWeaponBreakTargetLifecycle#captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z` — DIFF

```text
method captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z insn[133]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$…; path event multisets differ (left=156, right=156)
```

### `local/lito381/server/NativeWeaponBreakTargetLifecycle#lambda$captureOutput$10(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V` — ONLY_LEFT

```text
only left
```

### `local/lito381/server/NativeWeaponBreakTargetLifecycle#lambda$captureOutput$8(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V` — ONLY_LEFT

```text
only left
```

### `local/lito381/server/NativeWeaponBreakTargetLifecycle#lambda$captureOutput$9(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V` — ONLY_LEFT

```text
only left
```


## 推測

主控預期 #203 的 lambda 呼叫點配對修正可讓剩餘 DIFF 歸零；因尚未合併，這仍是待正式 CLI 重驗確認的預期。

## 待驗證／未完成事項

- P1 尚未解除：等 #186、#203 合併後重編 B01 17 檔，執行核准 AsRecord 轉換與全部核准清單，再驗收 22 entries 全為 SAME／SAME_EFFECTS／SAME_PATHS，DIFF／ONLY_LEFT／ONLY_RIGHT=0，missingLeft／extraLeft 為空。
- 單元測試與行為回歸依任務規定留待 B-land；本輪未啟動伺服器、登入器、遊戲或連 DB。
- 報告證據與 PR 描述修復不等同 B01 驗收完成，不能因此合併落地。

## 產出路徑

- 前輪修正來源為 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 下上述三個 Java；本輪保持不變。
- 正式報告為 `docs/reports/issue-62.md`。
- 本機建置證據為 `lito伺服器/LiTo381-Server/target/issue-62/final-174-176/`。
- 本機工具輸出為 `scratch/issue-62/final-174-176.{json,md}`，完整正規化輸出在 `scratch/issue-62/final-174-176/{left,right}/`。
- 本機重跑腳本為 `scratch/issue-62/verify.py`、`decompile.py`、`dump.py`、`probe-weapon-paths.py` 與 `ProbeWeaponPaths.java`。以下附命令收據與診斷程式，供雲端審查。

## 給下一棒的提示

主控／調度器先將派工依賴補上 #186、#203，避免在依賴未合併時重派。最新裁定已確定 **Java 不改、工具修正交 #203**，不必重做相同裁定。#203 合併後交本機執行器在 `task/62` 合併 main，重新編譯全部 17 檔、轉換 Draft、重驗全部 22 entries，更新本報告與 PR #79 的結果、原始輸出及雜湊。`captureOutput` 必須保留 `--exceptions-exempt`，Forest 必須保留既有 `--effects-methods` 與附錄 E 證據。全批正式通過後才交審查及 B-land。

## 附錄 A　實際命令收據

每筆 argv 使用陣列保留完整路徑與參數。工作目錄為本 worktree 的 `lito伺服器/LiTo381-Server`，`JAVA_HOME` 為 `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`，`AS_RECORD_MAVEN_REPO` 為 `D:/dev-cache/maven/repository`。

```json
[
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
      "-J-Dfile.encoding=UTF-8",
      "--release",
      "17",
      "-encoding",
      "UTF-8",
      "-d",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\annotation",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\src\\main\\java\\local\\lito381\\server\\AsRecord.java"
    ],
    "log": "final-174-176-annotation.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
      "-J-Dfile.encoding=UTF-8",
      "--release",
      "17",
      "-encoding",
      "UTF-8",
      "-cp",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\annotation;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\lib\\*",
      "-d",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\classes",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeBlindSceneAction.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeCompanionForestArea.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeCompanionRefScene.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeCompanionWindAction.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeControlledNegativeMaterial.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativePairedCastSafety.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceBatchTerminal.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceCubePlacement.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollAccepted.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollNpc.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollSkills.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollSlow.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceHealingCast.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourcePendingAdmission.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSummonTerminalLifecycle.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeWeaponBreakBattleCompanion.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeWeaponBreakTargetLifecycle.java"
    ],
    "log": "final-174-176-javac.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Users\\CHAOREN\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\python\\python.exe",
      "-B",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\as-record\\as_record.py",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\classes"
    ],
    "log": "final-174-176-as-record.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\jar.exe",
      "--create",
      "--file",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\staged.jar",
      "--no-manifest",
      "-C",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\classes",
      "."
    ],
    "log": "final-174-176-jar.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
      "-J-Dfile.encoding=UTF-8",
      "--release",
      "17",
      "-encoding",
      "UTF-8",
      "-cp",
      "D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar",
      "-d",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\BlockExpressions.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\ClassComparer.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\FourthNormalizations.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\JarIndex.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LayoutComparison.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalTransport.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalWebs.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\NonNullChecks.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\Normalizations.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\PathComparison.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\SemanticDiff.java",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\TransportComparison.java"
    ],
    "log": "final-174-176-tool.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
      "-Dfile.encoding=UTF-8",
      "-cp",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar",
      "local.lito381.semanticdiff.SemanticDiff",
      "--left",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\staged.jar",
      "--right",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar",
      "--left-classpath",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar",
      "--classes",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\entries.txt",
      "--json",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\final-174-176.json",
      "--md",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\final-174-176.md",
      "--effects-methods",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\effects-approved.txt",
      "--exceptions-exempt",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\exceptions-exempt.txt"
    ],
    "log": "final-174-176-semantic.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
      "-J-Dfile.encoding=UTF-8",
      "--release",
      "17",
      "-encoding",
      "UTF-8",
      "-cp",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar",
      "-d",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dump-tool",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\DumpNormalized.java"
    ],
    "log": "final-174-176-dump-build.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
      "-Dfile.encoding=UTF-8",
      "-cp",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dump-tool;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar",
      "local.lito381.semanticdiff.DumpNormalized",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\staged.jar",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\entries.txt",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\final-174-176\\left",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar"
    ],
    "log": "final-174-176-dump-left.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
      "-Dfile.encoding=UTF-8",
      "-cp",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dump-tool;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar",
      "local.lito381.semanticdiff.DumpNormalized",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\entries.txt",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\final-174-176\\right",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar"
    ],
    "log": "final-174-176-dump-right.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
      "-J-Dfile.encoding=UTF-8",
      "--release",
      "17",
      "-encoding",
      "UTF-8",
      "-cp",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar",
      "-d",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\probe-weapon-paths",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\ProbeWeaponPaths.java"
    ],
    "log": "probe-weapon-paths-build.log",
    "exit": 0
  },
  {
    "cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server",
    "argv": [
      "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
      "-Dfile.encoding=UTF-8",
      "-cp",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\probe-weapon-paths;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar",
      "local.lito381.semanticdiff.ProbeWeaponPaths",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\staged.jar",
      "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar"
    ],
    "log": "probe-weapon-paths.log",
    "exit": 0
  }
]
```

## 附錄 B　直接逐方法診斷程式

```java
package local.lito381.semanticdiff;

import java.nio.file.Path;
import org.objectweb.asm.tree.MethodNode;

public final class ProbeWeaponPaths {
    public static void main(String[] args) throws Exception {
        JarIndex left = JarIndex.load(Path.of(args[0]));
        JarIndex right = JarIndex.load(Path.of(args[1]));
        left.addClasspath(Path.of(args[1]));
        String owner = "local/lito381/server/NativeWeaponBreakTargetLifecycle";
        String descriptor = "(Llocal/lito381/server/NativeAreaMaterialAction$Context;"
                + "Llocal/lito381/server/NativePlayerCommit$Binding;"
                + "Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V";
        MethodNode leftMethod = left.readTree(owner).methods.stream()
                .filter(m -> m.name.startsWith("lambda$captureOutput$") && m.desc.equals(descriptor))
                .findFirst().orElseThrow();
        MethodNode rightMethod = right.readTree(owner).methods.stream()
                .filter(m -> m.name.startsWith("lambda$captureOutput$") && m.desc.equals(descriptor))
                .findFirst().orElseThrow();
        System.out.println("LEFT " + leftMethod.name + leftMethod.desc);
        System.out.println("RIGHT " + rightMethod.name + rightMethod.desc);
        System.out.println("IDENTITY_EQUAL " + ClassComparer.methodIdentity(leftMethod, left)
                .equals(ClassComparer.methodIdentity(rightMethod, right)));
        System.out.println("DIRECT_PATHS " + PathComparison.compare(leftMethod, rightMethod, left, right));
    }
}
```

## 附錄 C　剩餘類別的完整正規化差異

方向為 staged → candidate。使用本輪工具的 `readTree`、`remapMethod`、`instructions` 與 `tryCatch` 產生，所有差異 hunk 均列出。此 raw dump 同時列出已獲豁免的 Exceptions 屬性，正式 CLI 的核准套用情形見上文及 JSON。方法依內容 hash 排序，因此 hash 改變會使方法區塊移位。

```diff
--- staged/NativeWeaponBreakTargetLifecycle.txt
+++ candidate/NativeWeaponBreakTargetLifecycle.txt
@@ -84,7 +84,7 @@
 INVOKESTATIC local/lito381/server/NativeWeaponBreakMaterialAction.nativeTargetOriginalAdmit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;III)Z
 IRETURN

-METHOD captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z access=8 signature=null exceptions=[java/io/IOException]
+METHOD captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
 ALOAD 1
@@ -219,7 +219,7 @@
 LLOAD 10/11
 ALOAD 6
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$b1b16ba1c81851b850e136c02340d6e6b57fc34a94441c794c0135a976983127(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$56c27fca56ba29c7648e26e00cf9e56129d5daa7ffec4f35913eb3564ef061b8(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeLightAction.deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V
 CONST Integer:1
 IRETURN
@@ -335,7 +335,53 @@
 CONST Integer:0
 IRETURN

-METHOD lambda$content$44f8379cfcb681fd08e9d82c1a2c11b532abcb7b20c3448ddfc861e902d0c53a(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
+METHOD lambda$content$56c27fca56ba29c7648e26e00cf9e56129d5daa7ffec4f35913eb3564ef061b8(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[java/io/IOException]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+CONST Integer:1
+ANEWARRAY java/lang/Runnable
+ASTORE 9
+ALOAD 0
+ALOAD 1
+ALOAD 0
+ALOAD 9
+ALOAD 2
+ALOAD 3
+ILOAD 4
+LLOAD 5/6
+ALOAD 7
+ALOAD 8
+INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$d702c09cb808c472cec809e6ccae1d494c2f182dfdf1a62aef8c0710d0f4d085(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeSourcePlayerActionLock.run(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
+ALOAD 9
+CONST Integer:0
+AALOAD
+IFNULL L23
+ALOAD 9
+CONST Integer:0
+AALOAD
+INVOKEINTERFACE java/lang/Runnable.run()V itf
+RETURN
+
+METHOD lambda$content$acb8e80cffc637cf79b912c6a823ecec3d65b6daacf3ad383004c8d8b8832ad6(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V access=10 signature=null exceptions=[java/io/IOException]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+ALOAD 0
+CONST Integer:1
+CONST Integer:27
+ILOAD 1
+ALOAD 2
+ALOAD 0
+ALOAD 3
+ALOAD 4
+LLOAD 5/6
+ALOAD 7
+ALOAD 8
+ALOAD 9
+ALOAD 10
+INVOKEDYNAMIC run(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$0700fe5f7bb2f9f8ee3e69ac6faf4e3e5d37d298551ee2b9b246ac8ad86490c9(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeWorldMaterialPolicy.run(Llocal/lito381/server/ClientSession;ZILlocal/lito381/server/NativeSelfCastAction$Task;)V
+RETURN
+
+METHOD lambda$content$d702c09cb808c472cec809e6ccae1d494c2f182dfdf1a62aef8c0710d0f4d085(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
 ALOAD 1
@@ -352,181 +398,9 @@
 LLOAD 6/7
 ALOAD 8
 ALOAD 9
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$9275bbd215f844f866ce0d356d1b561d12a7fe5b1130367613e83a0440f0974e(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$f4ce92e0a78322eee880a1ba03c8fafb257aed9d759407f5cb5885bd716e22db(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeRolePublication.completion(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)Ljava/lang/Runnable;
 AASTORE
-RETURN
-
-METHOD lambda$content$9275bbd215f844f866ce0d356d1b561d12a7fe5b1130367613e83a0440f0974e(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-6:[83:java/lang/Throwable  ]
-9:[83:java/lang/Throwable  ]
-12:[83:java/lang/Throwable  ]
-15:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-19:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-22:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-26:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-27:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-33:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-43:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-47:[45:java/lang/Throwable  , 83:java/lang/Throwable  ]
-49:[83:java/lang/Throwable  ]
-53:[83:java/lang/Throwable  ]
-57:[83:java/lang/Throwable  ]
-60:[83:java/lang/Throwable  ]
-64:[83:java/lang/Throwable  ]
-65:[83:java/lang/Throwable  ]
-71:[83:java/lang/Throwable  ]
-81:[83:java/lang/Throwable  ]
-85:[83:java/lang/Throwable  ]
-93:[95:java/io/IOException  ]
-ALOAD 0
-GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.hub:Llocal/lito381/server/GameHub;
-DUP
-ASTORE 9
-MONITORENTER
-ALOAD 0
-GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.population:Llocal/lito381/server/ClassicWorldMonsterPopulation;
-IFNULL L51
-ALOAD 0
-GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.population:Llocal/lito381/server/ClassicWorldMonsterPopulation;
-DUP
-ASTORE 10
-MONITORENTER
-ALOAD 1
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L40
-ALOAD 0
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.player(Llocal/lito381/server/ClientSession;)Z
-IFEQ L40
-ALOAD 3
-INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 11
-ALOAD 11
-ALOAD 0
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
-INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
-IFEQ L37
-ILOAD 4
-IFEQ L35
-ALOAD 0
-LLOAD 5/6
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.npc(J)Z
-IFEQ L37
-CONST Integer:1
-GOTO L38
-CONST Integer:0
-ISTORE 12
-GOTO L42
-CONST Integer:0
-ISTORE 12
-ALOAD 10
-MONITOREXIT
-GOTO L50
-ASTORE 13
-ALOAD 10
-MONITOREXIT
-ALOAD 13
-ATHROW
-GOTO L80
-ALOAD 1
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L78
-ALOAD 0
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.player(Llocal/lito381/server/ClientSession;)Z
-IFEQ L78
-ALOAD 3
-INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 14
-ALOAD 14
-ALOAD 0
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
-INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
-IFEQ L75
-ILOAD 4
-IFEQ L73
-ALOAD 0
-LLOAD 5/6
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.npc(J)Z
-IFEQ L75
-CONST Integer:1
-GOTO L76
-CONST Integer:0
-ISTORE 12
-GOTO L80
-CONST Integer:0
-ISTORE 12
-ALOAD 9
-MONITOREXIT
-GOTO L88
-ASTORE 15
-ALOAD 9
-MONITOREXIT
-ALOAD 15
-ATHROW
-ILOAD 12
-IFEQ L101
-ALOAD 2
-ALOAD 7
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSelfOriginalSend([BLjava/lang/String;)V
-GOTO L101
-ASTORE 16
-NEW java/io/UncheckedIOException
-DUP
-ALOAD 16
-INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
-ATHROW
-RETURN
-
-METHOD lambda$content$acb8e80cffc637cf79b912c6a823ecec3d65b6daacf3ad383004c8d8b8832ad6(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V access=10 signature=null exceptions=[java/io/IOException]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-CONST Integer:1
-CONST Integer:27
-ILOAD 1
-ALOAD 2
-ALOAD 0
-ALOAD 3
-ALOAD 4
-LLOAD 5/6
-ALOAD 7
-ALOAD 8
-ALOAD 9
-ALOAD 10
-INVOKEDYNAMIC run(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$0700fe5f7bb2f9f8ee3e69ac6faf4e3e5d37d298551ee2b9b246ac8ad86490c9(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V, type:()V]
-INVOKESTATIC local/lito381/server/NativeWorldMaterialPolicy.run(Llocal/lito381/server/ClientSession;ZILlocal/lito381/server/NativeSelfCastAction$Task;)V
-RETURN
-
-METHOD lambda$content$b1b16ba1c81851b850e136c02340d6e6b57fc34a94441c794c0135a976983127(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[java/io/IOException]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-CONST Integer:1
-ANEWARRAY java/lang/Runnable
-ASTORE 9
-ALOAD 0
-ALOAD 1
-ALOAD 0
-ALOAD 9
-ALOAD 2
-ALOAD 3
-ILOAD 4
-LLOAD 5/6
-ALOAD 7
-ALOAD 8
-INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$44f8379cfcb681fd08e9d82c1a2c11b532abcb7b20c3448ddfc861e902d0c53a(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
-INVOKESTATIC local/lito381/server/NativeSourcePlayerActionLock.run(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
-ALOAD 9
-CONST Integer:0
-AALOAD
-IFNULL L23
-ALOAD 9
-CONST Integer:0
-AALOAD
-INVOKEINTERFACE java/lang/Runnable.run()V itf
 RETURN

 METHOD lambda$content$df4a7f0c1090897fcdff50781c314dadb3052d6407d81bb25cda81b2f17b3871(Llocal/lito381/server/NativeAreaMaterialAction$Context;ZLlocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/SourceNpcPhysicalStats;)V access=10 signature=null exceptions=[java/io/IOException]
@@ -579,6 +453,122 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 8
+ATHROW
+RETURN
+
+METHOD lambda$content$f4ce92e0a78322eee880a1ba03c8fafb257aed9d759407f5cb5885bd716e22db(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+6:[73:java/lang/Throwable  ]
+9:[73:java/lang/Throwable  ]
+12:[73:java/lang/Throwable  ]
+15:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+19:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+22:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+24:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+25:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+31:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+38:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+42:[40:java/lang/Throwable  , 73:java/lang/Throwable  ]
+44:[73:java/lang/Throwable  ]
+48:[73:java/lang/Throwable  ]
+52:[73:java/lang/Throwable  ]
+55:[73:java/lang/Throwable  ]
+57:[73:java/lang/Throwable  ]
+58:[73:java/lang/Throwable  ]
+64:[73:java/lang/Throwable  ]
+71:[73:java/lang/Throwable  ]
+75:[73:java/lang/Throwable  ]
+83:[85:java/io/IOException  ]
+ALOAD 0
+GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.hub:Llocal/lito381/server/GameHub;
+DUP
+ASTORE 9
+MONITORENTER
+ALOAD 0
+GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.population:Llocal/lito381/server/ClassicWorldMonsterPopulation;
+IFNULL L46
+ALOAD 0
+GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.population:Llocal/lito381/server/ClassicWorldMonsterPopulation;
+DUP
+ASTORE 10
+MONITORENTER
+ALOAD 1
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
+IFEQ L35
+ALOAD 0
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.player(Llocal/lito381/server/ClientSession;)Z
+IFEQ L35
+ALOAD 3
+INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
+ALOAD 0
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
+IFEQ L35
+ILOAD 4
+IFEQ L33
+ALOAD 0
+LLOAD 5/6
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.npc(J)Z
+IFEQ L35
+CONST Integer:1
+GOTO L36
+CONST Integer:0
+ISTORE 11
+ALOAD 10
+MONITOREXIT
+GOTO L45
+ASTORE 12
+ALOAD 10
+MONITOREXIT
+ALOAD 12
+ATHROW
+GOTO L70
+ALOAD 1
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
+IFEQ L68
+ALOAD 0
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.player(Llocal/lito381/server/ClientSession;)Z
+IFEQ L68
+ALOAD 3
+INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
+ALOAD 0
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
+IFEQ L68
+ILOAD 4
+IFEQ L66
+ALOAD 0
+LLOAD 5/6
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.npc(J)Z
+IFEQ L68
+CONST Integer:1
+GOTO L69
+CONST Integer:0
+ISTORE 11
+ALOAD 9
+MONITOREXIT
+GOTO L78
+ASTORE 13
+ALOAD 9
+MONITOREXIT
+ALOAD 13
+ATHROW
+ILOAD 11
+IFEQ L91
+ALOAD 2
+ALOAD 7
+ALOAD 8
+INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSelfOriginalSend([BLjava/lang/String;)V
+GOTO L91
+ASTORE 14
+NEW java/io/UncheckedIOException
+DUP
+ALOAD 14
+INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
 ATHROW
 RETURN
```

## 附錄 D　正式工具完整 JSON

```json
{
  "tool": "semantic-diff",
  "generatedAt": "2026-11-05T15:26:59Z",
  "left": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final-174-176\\staged.jar",
  "right": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar",
  "leftClasspath": [
    "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar"
  ],
  "rightClasspath": [],
  "allowRight": null,
  "effectsMethodsFile": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\effects-approved.txt",
  "extraMethodsFile": null,
  "metadataExemptFile": null,
  "innerAccessWiden": [],
  "accessWiden": [],
  "layoutMethods": [],
  "methodDiffs": [],
  "ctorPrologueOrder": [],
  "exceptionsExempt": [
    "local/lito381/server/NativeWeaponBreakTargetLifecycle#captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z"
  ],
  "deviations": [],
  "extraMethods": [],
  "metadataExempt": [],
  "leftClassCount": 22,
  "rightClassCount": 3033,
  "comparedClassCount": 22,
  "classSelection": {
    "missingLeft": [],
    "extraLeft": []
  },
  "byteSameCount": 0,
  "counts": {
    "SAME": 20,
    "SAME_EFFECTS": 1,
    "SAME_LAYOUT": 0,
    "SAME_PATHS": 0,
    "ORPHAN_IGNORED": 0,
    "DEVIATION": 0,
    "DIFF": 1,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 0
  },
  "classes": [
    {
      "name": "local/lito381/server/NativeBlindSceneAction",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeCompanionForestArea",
      "status": "SAME_EFFECTS",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [
        {
          "method": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V",
          "status": "SAME_EFFECTS",
          "detail": null
        }
      ],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeCompanionRefScene",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeCompanionWindAction",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeControlledNegativeMaterial",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativePairedCastSafety",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceBatchTerminal",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceCubePlacement",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceCubePlacement$Draft",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollAccepted",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollAccepted$Pending",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollNpc",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollNpc$Scope",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollSkills",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollSkills$View",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollSlow",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceHealingCast",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceHealingCast$Cast",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourcePendingAdmission",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSummonTerminalLifecycle",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeWeaponBreakBattleCompanion",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [],
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeWeaponBreakTargetLifecycle",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "methodResults": [
        {
          "method": "lambda$captureOutput$6(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V",
          "status": "ONLY_RIGHT",
          "detail": "only right"
        },
        {
          "method": "lambda$captureOutput$7(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V",
          "status": "ONLY_RIGHT",
          "detail": "only right"
        },
        {
          "method": "lambda$captureOutput$8(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V",
          "status": "ONLY_RIGHT",
          "detail": "only right"
        },
        {
          "method": "captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z",
          "status": "DIFF",
          "detail": "method captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z insn[133]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$…; path event multisets differ (left=156, right=156)"
        },
        {
          "method": "lambda$captureOutput$10(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V",
          "status": "ONLY_LEFT",
          "detail": "only left"
        },
        {
          "method": "lambda$captureOutput$8(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V",
          "status": "ONLY_LEFT",
          "detail": "only left"
        },
        {
          "method": "lambda$captureOutput$9(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V",
          "status": "ONLY_LEFT",
          "detail": "only left"
        }
      ],
      "effectsMethods": [],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z",
        "insnIndex": 133,
        "detail": "left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$…; path event multisets differ (left=156, right=156)"
      }
    }
  ]
}
```

## 附錄 E　Forest B 類核准的完整證據（本輪補回）

方法：`NativeCompanionForestArea#apply(ClientSession, AreaSilenceSpell, int, int, int, int, long)`，完整描述子如下核准行。依[主控對 Forest 的裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6647804482)及[本次審查回饋](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6664074935)，沿用 B 類 `dup-pop-local` 核准；本輪沒有新增或擴大豁免。

完整正規化差異顯示 staged 在 `List.stream()` 後多出 `ASTORE 21`／`ALOAD 21`；候選把相同 stream 留在 stack。這使 `toList()` 結果槽位由候選的 21 成為 staged 的 22，後續例外暫存槽位由 22–24 成為 23–25；指令索引及 handler／跳躍目標相應位移兩條。`NativeSourceIndirectPlayerProof.player`、filter、toList、publish 呼叫及 monitor 退出結構仍保留。這是區域暫存與 stack 形狀差，屬既有 B 類分類；正式工具以精確方法核准得到 SAME_EFFECTS，並非把整個類別忽略。下方保留全部 hunks，含例外涵蓋與 monitor 區塊差異。

核准行（`docs/combat-merge/effects-approved.txt`）：

```text
local/lito381/server/NativeCompanionForestArea#apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V # batch: B01, category: dup-pop-local, pr: https://github.com/chaoren0418-arch/lito-launcher-server/pull/79
```

前輪原始 JSON 中的逐方法證據（附錄 D 保留完整 JSON）：

```json
{
  "name": "local/lito381/server/NativeCompanionForestArea",
  "status": "SAME_EFFECTS",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [],
  "effectsMethods": [
    {
      "method": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V",
      "status": "SAME_EFFECTS",
      "detail": null
    }
  ],
  "approvedMethods": []
}
```

原始命令收據已附於附錄 A，其中 `final-174-176-semantic.log` 對應的 argv 含 `--effects-methods scratch/issue-62/effects-approved.txt` 及 `--exceptions-exempt scratch/issue-62/exceptions-exempt.txt`（實際使用絕對路徑）。本輪以 `difflib.unified_diff(..., n=3)` 從原始 left／right dump 重新計算，確認與原始 `.diff` 逐字相同，並核對 JAR／classes 22 個 entry 的 bytes；核對腳本為 `scratch/issue-62/update-followup.py`，核對結果為 `scratch/issue-62/followup-validation.json`。下方文件只將 diff 的空白上下文行呈現為空行，全部指令與 hunks 均保留；原始 diff 未改動。

| 原始證據路徑 | SHA-256 |
|---|---|
| `scratch/issue-62/final-174-176.json` | `8446c4e3fc11ab8713fc4f089e7096aad9252157343122a507755f2f4e775487` |
| `scratch/issue-62/final-174-176/left/NativeCompanionForestArea.txt` | `06d5ed6ee3438d36a979bb898b3f1f6d9510520ce939851f35c67486db80e6fa` |
| `scratch/issue-62/final-174-176/right/NativeCompanionForestArea.txt` | `9d569caf7c34036e0bef6de839c20e05390961a28f4075b767a970c3a2731b4b` |
| `scratch/issue-62/final-174-176/NativeCompanionForestArea.txt.diff` | `ba297cbf782c826f6c3fa380d6b4f1f9053fb4d89f3ee1b0e096f9731bc5449c` |
| `lito伺服器/LiTo381-Server/target/issue-62/final-174-176/staged.jar` | `e05d3f432413ecc65aa3f4f5a1d68b196de634f954935230763b443bfa92501f` |
| `scratch/issue-62/dependency-check.json` | `0f466f5d77183b0c7251d5a3f1f918d45003f65b049f0d581693b67667c0130f` |

### 完整正規化差異

```diff
--- staged/NativeCompanionForestArea.txt
+++ candidate/NativeCompanionForestArea.txt
@@ -12,65 +12,65 @@

 METHOD apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-54:[213:java/lang/Throwable  ]
-55:[213:java/lang/Throwable  ]
-60:[213:java/lang/Throwable  ]
-62:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-63:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-66:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-69:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-71:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-76:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-79:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-81:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-83:[213:java/lang/Throwable  ]
-86:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-90:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-92:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-97:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-102:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-104:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-107:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-111:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-112:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-118:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-121:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-125:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-131:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-133:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-135:[213:java/lang/Throwable  ]
-140:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-141:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-144:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-145:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-149:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-150:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-151:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-157:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-161:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-165:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-166:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-167:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-171:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-172:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-173:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-178:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-179:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-183:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-184:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-185:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-189:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-190:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-191:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-192:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-195:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-199:[197:java/lang/Throwable  , 205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-201:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-203:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-207:[205:java/lang/Throwable  , 213:java/lang/Throwable  ]
-209:[213:java/lang/Throwable  ]
-211:[213:java/lang/Throwable  ]
-215:[213:java/lang/Throwable  ]
+54:[211:java/lang/Throwable  ]
+55:[211:java/lang/Throwable  ]
+60:[211:java/lang/Throwable  ]
+62:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+63:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+66:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+69:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+71:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+76:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+79:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+81:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+83:[211:java/lang/Throwable  ]
+86:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+90:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+92:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+97:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+102:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+104:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+107:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+111:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+112:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+118:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+121:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+125:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+131:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+133:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+135:[211:java/lang/Throwable  ]
+140:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+141:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+144:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+145:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+147:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+148:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+149:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+155:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+159:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+163:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+164:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+165:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+169:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+170:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+171:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+176:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+177:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+181:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+182:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+183:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+187:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+188:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+189:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+190:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+193:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+197:[195:java/lang/Throwable  , 203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+199:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+201:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+205:[203:java/lang/Throwable  , 211:java/lang/Throwable  ]
+207:[211:java/lang/Throwable  ]
+209:[211:java/lang/Throwable  ]
+213:[211:java/lang/Throwable  ]
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeAreaMaterialAction$Context
@@ -106,7 +106,7 @@
 ASTORE 9
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L219
+IFEQ L217
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeAreaMaterialAction$Npc
@@ -114,7 +114,7 @@
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.kind()I
 CONST Integer:3
-IF_ICMPNE L218
+IF_ICMPNE L216
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.actor()Ljava/lang/Object;
 CHECKCAST local/lito381/server/BattleCompanion
@@ -217,13 +217,11 @@
 ALOAD 8
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 21
-ALOAD 21
 ALOAD 8
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
-ASTORE 22
+ASTORE 21
 ALOAD 0
 CONST str:"publishAreaSilenceEffect"
 CONST str:"(Ljava/util/List;JILlocal/lito381/server/ClientSession$Phase;II)V"
@@ -231,7 +229,7 @@
 ANEWARRAY java/lang/Object
 DUP
 CONST Integer:0
-ALOAD 22
+ALOAD 21
 AASTORE
 DUP
 CONST Integer:1
@@ -267,27 +265,27 @@
 POP
 ALOAD 16
 MONITOREXIT
-GOTO L202
+GOTO L200
+ASTORE 22
+ALOAD 16
+MONITOREXIT
+ALOAD 22
+ATHROW
+ALOAD 14
+MONITOREXIT
+GOTO L208
 ASTORE 23
-ALOAD 16
+ALOAD 14
 MONITOREXIT
 ALOAD 23
 ATHROW
-ALOAD 14
-MONITOREXIT
-GOTO L210
+ALOAD 12
+MONITOREXIT
+GOTO L216
 ASTORE 24
-ALOAD 14
+ALOAD 12
 MONITOREXIT
 ALOAD 24
-ATHROW
-ALOAD 12
-MONITOREXIT
-GOTO L218
-ASTORE 25
-ALOAD 12
-MONITOREXIT
-ALOAD 25
 ATHROW
 GOTO L33
 RETURN
```
