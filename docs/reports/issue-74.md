# Issue #74 — A03 正式 0.6.71 暫存最終驗收

## 結論

2026-11-06（Asia/Taipei）已合併包含 #186 的最新 main，直接重新編譯 A03 的 47 份 staged Java，完整驗收 **88 entry：87 SAME、1 DEVIATION、DIFF 0，acceptancePass=true**。missing／extra、ONLY_LEFT／ONLY_RIGHT 均為空或 0。這是本輪實測結果，取代前輪 85 SAME＋1 DEVIATION＋2 DIFF；歷史證據由 git 保留。

依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/74#issuecomment-6657652171)，本輪 Java 不改。`NativeDeVisibilitySwitch` 與 `NativeDeVisibilityPc` 均已為 SAME；不需新的差異裁定或新增 needs:claude。唯一 DEVIATION 為核准的 `NativeDeWorldLog.debug(String,Object,Object)`。

## 做了什麼

- 在原有 `task/74` 合併最新 `origin/main`，無衝突；未切換或建立分支。#186 已於 2026-11-05 17:21:31 UTC 由 [PR #202](https://github.com/chaoren0418-arch/lito-launcher-server/pull/202) 合併。
- 以固定 main 快照的正式工具及七份核准清單重驗；核准清單依 A03 的 verify_entries 裁切，未新增條目。
- 本輪所有 47 份 Java 與合併前 `031a757` 的內容一致，均保有 `DECOMPILED from base-0.6.71.jar` 標頭。完整 PR 相對 main 為 47 份本批 staged Java 加本報告，共 48 檔。
- 本輪手動追蹤檔變更僅本報告，並同步 PR #100 描述。`src/main`、`pom.xml`、`docs/REPORT.md` 相對 main 無本批差異；合併帶入的主線索引不是本批自行修改。

既有 Java 修正保留：VisibilityCoordinator 的條件更新、VisibilityPc 的 List 呼叫型別及布林返回、WorldContext 的 monitor／read lock 邊界、WorldCore 的 int map key／裝箱與控制流程、VisibilitySwitch 的原欄位名稱及初始化順序；本輪無編譯後 class 改寫或以正式 bytes 覆蓋產物。

## 已驗證：來源與依賴

| 項目 | 固定提交 |
|---|---|
| 合併前 task/74 | `031a757fe09c6801db5c78db6570647ed4dc2123` |
| 本輪 main／工具與核准清單快照 | `9678e97f4bff1dfab7cc820297e3ea6ff533375c` |
| 合併後驗收來源 | `98af7214b2f3994c200ee2885200a2088187668a` |
| 工具 Java 最後變更 | `7554762bd0267d937d798ab7aff39b9d3b623384` |
| #174／PR #178 合併 | `6b769a4` |
| #176／PR #193 合併 | `a32d30d` |
| #186／PR #202 合併 | `bccc2cb746e408bc8a496cf46c224d61616b9076` |

`git merge-base --is-ancestor bccc2cb746e408bc8a496cf46c224d61616b9076 HEAD` 通過。依據為設計 §7、`a-batches.json` A03、[#62 通用規則](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6644529572)。本輪不需 B 類副作用豁免。

## 已驗證：編譯與語意

Microsoft OpenJDK 17.0.17+10-LTS、Python 3.12、ASM 9.7。重新編譯 9 份 compile-only stub、13 份正式工具來源及 A03 的 47 份 Java；stub 僅供 classpath，未打包。A03 JAR 的 88 個 class entry 精確等於 verify_entries，且每一 entry bytes 與直接 javac 輸出相同。JAR 固定打包日期 `2026-11-05T00:00:00Z`，SHA-256 與前輪相同。

正式 CLI 退出碼 0 表示報表產生成功；本輪另以逐 entry 狀態、數量、集合及 JAR/class bytes 檢查判定通過。原始結果產生時間為 `2026-11-05T17:53:15Z`（臺灣 2026-11-06 01:53:15）。

```text
SUMMARY SAME=87 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=0
entries=88 missing=[] extra=[]
acceptancePass=true
```

### 核准範圍

七份清單均從上述同一 main 快照讀取；實際交集如下，其餘五份為空，不啟用不相關豁免。

```json
{
  "effects-approved.txt": [],
  "extra-methods-approved.txt": [],
  "metadata-exempt.txt": [
    "local/lito381/server/NativeDeVisibilitySwitch # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/108#issuecomment-6643057522"
  ],
  "access-widen-approved.txt": [],
  "layout-approved.txt": [],
  "deviations.txt": [
    "local/lito381/server/NativeDeWorldLog#debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/153"
  ],
  "exceptions-exempt.txt": []
}
```

`NativeDeWorldLog` 僅保留指定的單一 default debug，呼叫 `emit("debug", message, new Object[]{a, b})`。正式版缺少此方法，依 #73／#153 的 `deviations.txt`／`deviations.md` 保留，不能將 DEVIATION 誤列為 SAME。其他方法及類別屬性仍完整驗證。

## 未驗證什麼／推測／給下一棒的提示

- 本輪沒有需要用推測代替的驗收結果；A03 編譯與靜態語意驗收已通過。
- 未執行單元測試、執行期回歸及真機驗收，依 §7 留待 A-land；未啟動遊戲／登入器／伺服器、未連正式 DB。
- Claude 審查者可依 PR #100、本報告、固定工具提交及原始收據進行最終審查；調度器不必再等待 #186，也不需重派 Java 修改。PR 合併由使用者依流程處理。
- A-land 執行器統一落地與回歸；依 `deviations.md` 模擬選用欄位讀取失敗，確認 default debug 保留訊息與參數、不拋 NoSuchMethodError，並驗證正常路徑與必要欄位失敗處理。

## 產出路徑與 SHA-256

- 追蹤報告：`docs/reports/issue-74.md`。
- 交付來源：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`，精確 47 檔見附錄 javac argv。
- 本輪原始收據：worktree 下 `scratch/issue-74/final/`，包含正式 JSON／Markdown、完整 argv、stdout/stderr、退出碼、選取集合與輸入輸出逐檔雜湊。產物位於其 `target/`，不進 git。
- 重驗腳本：`scratch/issue-74/final.py`；前輪 `latest/` 不當作本輪結果。

| 檔案 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `scratch/issue-74/final.py` | `bf5fa48fa5e1289aa4992d84326ef9075a83778eebbf657e7b2f4608fc9904e5` |
| `scratch/issue-74/final/target/a03.jar` | `9f3555fed2db3e6d61db3213189f74f0b5d283e21997774982be2bd92ac3ea45` |
| `scratch/issue-74/final/semantic.json` | `61ab66fa544bf2c2a216acfad38196d195722e1757a4c148770fbcc143d24b53` |
| `scratch/issue-74/final/semantic.md` | `4e809f63af1e23528417b177315dbfa289740706b0ecdb4076513cf9f8da7c19` |
| `scratch/issue-74/final/semantic.log` | `167660cbfd81b4221e700a629a1e22807cece40beda997ff453e6e3008a50c73` |
| `scratch/issue-74/final/acceptance.json` | `8f051e63b8f6e91e8636929ecc17b1ea009b255342a60cb27958a7c9ad2c402d` |
| `scratch/issue-74/final/selection.json` | `cbe7d560d3f89d33c21f0757263dc213e2cc2c2d915081544dab49d24f2a830a` |
| `scratch/issue-74/final/entries.txt` | `6e5f2f766d5158a5ebc2dc028c5c0e47d2e9cad2dcebe23bc8b9ca30a0f79831` |
| `scratch/issue-74/final/provenance.json` | `6c649facf8fe235a5afd8a0ef663f4be8856aa63f1298a107797e6eae97b60f2` |
| `scratch/issue-74/final/inputs-sha256.json` | `1d68684c8425299dbc93b845e86723969456e2995d030d5c83f1cec6e3d84368` |
| `scratch/issue-74/final/outputs-sha256.json` | `81b4bf2a55cdb94e8a5c0814633f25338256d5d608adcd0c49d932693c283cd6` |

已逐檔核對 83 項輸入、173 項輸出的 SHA-256 均符合收據；`git diff --check` 通過。

## 附錄 A：本輪完整命令與原始輸出

命令以 JSON argv 保留每個原始引數（不省略 47 份來源），cwd 為本 worktree；stubs 使用 JAVA_HOME 指向同一 JDK。各工具來源由固定 main commit 的 `git show <commit>:<path>` 匯出到 tool-source，內容未修改。

### jdk-version（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
  "-version"
]
```

```text
openjdk version "17.0.17" 2025-10-21 LTS
OpenJDK Runtime Environment Microsoft-12574423 (build 17.0.17+10-LTS)
OpenJDK 64-Bit Server VM Microsoft-12574423 (build 17.0.17+10-LTS, mixed mode, sharing)
```

### stubs（exit 0）

```json
[
  "powershell",
  "-NoProfile",
  "-ExecutionPolicy",
  "Bypass",
  "-File",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\tools\\legacy-stubs\\build.ps1"
]
```

```text
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
```

### tool-build（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-cp",
  "D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\reverify\\target\\dependency\\asm-analysis-9.7.jar",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\target\\tool",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\BlockExpressions.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\ClassComparer.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\FourthNormalizations.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\JarIndex.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\LayoutComparison.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\LocalTransport.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\LocalWebs.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\NonNullChecks.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\Normalizations.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\PathComparison.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\RethrowHandlers.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\SemanticDiff.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\tool-source\\local\\lito381\\semanticdiff\\TransportComparison.java"
]
```

```text
（無輸出）
```

### javac（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-cp",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\target\\legacy-stubs;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\lib\\*",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\target\\classes",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeShopResources.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeShopTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeSpriteCatalog.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeTerrainPoint.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeTerrainResources.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeTerrainScope.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeTerrainTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeTerrainText.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeTerrainV1.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeTerrainWorld.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityAccess.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityCheck.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityCoordinator.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityHp.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityJobs.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityOutput.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityPacket.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityPc.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityRuntime.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityScope.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilitySwitch.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeVisibilityTimer.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldContext.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldControls.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldCore.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldKinds.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldLocation.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldLog.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldPlayerFilter.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldPoint.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryBow.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryCrown.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryDarkelf.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryDoll.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryDragonKnight.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryEffect.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryElf.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryIllusionist.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryItem.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryKnight.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryMob.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryPet.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryQuest.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistrySummons.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryTrap.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldRegistryWizard.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldScope.java"
]
```

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

### jar（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\jar.exe",
  "--create",
  "--file",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\target\\a03.jar",
  "--date",
  "2026-11-05T00:00:00Z",
  "-C",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\target\\classes",
  "."
]
```

```text
（無輸出）
```

### semantic（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
  "-Dfile.encoding=UTF-8",
  "-cp",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\target\\tool;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\reverify\\target\\dependency\\asm-analysis-9.7.jar",
  "local.lito381.semanticdiff.SemanticDiff",
  "--left",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\target\\a03.jar",
  "--right",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar",
  "--left-classpath",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar",
  "--classes",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\entries.txt",
  "--metadata-exempt",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\metadata-exempt.txt",
  "--deviations",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\deviations.txt",
  "--json",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\semantic.json",
  "--md",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-74\\scratch\\issue-74\\final\\semantic.md"
]
```

```text
SUMMARY SAME=87 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=0
wrote D:\lineage開發\3-lito開發\scratch\wt\task-74\scratch\issue-74\final\semantic.json
wrote D:\lineage開發\3-lito開發\scratch\wt\task-74\scratch\issue-74\final\semantic.md
```

## 附錄 B：全部 88 個驗收 entry

以下名稱皆加前綴 `local/lito381/server/` 與後綴 `.class`；與 A03.verify_entries 精確相同。

| entry | 狀態 |
|---|---|
| `NativeDeShopResources` | SAME |
| `NativeDeShopResources$Snapshot` | SAME |
| `NativeDeShopTable` | SAME |
| `NativeDeSpriteCatalog` | SAME |
| `NativeDeSpriteCatalog$Catalog` | SAME |
| `NativeDeSpriteCatalog$Mutable` | SAME |
| `NativeDeSpriteCatalog$Reporter` | SAME |
| `NativeDeSpriteCatalog$Sprite` | SAME |
| `NativeDeTerrainPoint` | SAME |
| `NativeDeTerrainResources` | SAME |
| `NativeDeTerrainResources$1` | SAME |
| `NativeDeTerrainResources$Definition` | SAME |
| `NativeDeTerrainResources$Snapshot` | SAME |
| `NativeDeTerrainScope` | SAME |
| `NativeDeTerrainScope$Asset` | SAME |
| `NativeDeTerrainScope$Context` | SAME |
| `NativeDeTerrainScope$Database` | SAME |
| `NativeDeTerrainScope$Logs` | SAME |
| `NativeDeTerrainScope$Sql` | SAME |
| `NativeDeTerrainTable` | SAME |
| `NativeDeTerrainText` | SAME |
| `NativeDeTerrainV1` | SAME |
| `NativeDeTerrainWorld` | SAME |
| `NativeDeTerrainWorld$Domain` | SAME |
| `NativeDeTerrainWorld$Key` | SAME |
| `NativeDeTerrainWorld$Owner` | SAME |
| `NativeDeVisibilityAccess` | SAME |
| `NativeDeVisibilityCheck` | SAME |
| `NativeDeVisibilityCoordinator` | SAME |
| `NativeDeVisibilityHp` | SAME |
| `NativeDeVisibilityJobs` | SAME |
| `NativeDeVisibilityJobs$Credit` | SAME |
| `NativeDeVisibilityJobs$Dispatch` | SAME |
| `NativeDeVisibilityJobs$Ticket` | SAME |
| `NativeDeVisibilityOutput` | SAME |
| `NativeDeVisibilityPacket` | SAME |
| `NativeDeVisibilityPc` | SAME |
| `NativeDeVisibilityRuntime` | SAME |
| `NativeDeVisibilityRuntime$Owner` | SAME |
| `NativeDeVisibilityScope` | SAME |
| `NativeDeVisibilityScope$Logs` | SAME |
| `NativeDeVisibilitySwitch` | SAME |
| `NativeDeVisibilityTimer` | SAME |
| `NativeDeWorldContext` | SAME |
| `NativeDeWorldContext$1` | SAME |
| `NativeDeWorldContext$2` | SAME |
| `NativeDeWorldContext$Entry` | SAME |
| `NativeDeWorldControls` | SAME |
| `NativeDeWorldCore` | SAME |
| `NativeDeWorldCore$1` | SAME |
| `NativeDeWorldCore$SpatialIndexAuditSnapshot` | SAME |
| `NativeDeWorldCore$SpatialReconcileResult` | SAME |
| `NativeDeWorldCore$VisibleBinding` | SAME |
| `NativeDeWorldKinds` | SAME |
| `NativeDeWorldKinds$Bow` | SAME |
| `NativeDeWorldKinds$Doll` | SAME |
| `NativeDeWorldKinds$Dwarf` | SAME |
| `NativeDeWorldKinds$Effect` | SAME |
| `NativeDeWorldKinds$Item` | SAME |
| `NativeDeWorldKinds$Item$EquipmentTimerLease` | SAME |
| `NativeDeWorldKinds$Item$LastStatus` | SAME |
| `NativeDeWorldKinds$Merchant` | SAME |
| `NativeDeWorldKinds$Npc` | SAME |
| `NativeDeWorldKinds$Pet` | SAME |
| `NativeDeWorldKinds$Trap` | SAME |
| `NativeDeWorldLocation` | SAME |
| `NativeDeWorldLog` | DEVIATION |
| `NativeDeWorldPlayerFilter` | SAME |
| `NativeDeWorldPlayerFilter$1` | SAME |
| `NativeDeWorldPoint` | SAME |
| `NativeDeWorldRegistryBow` | SAME |
| `NativeDeWorldRegistryCrown` | SAME |
| `NativeDeWorldRegistryDarkelf` | SAME |
| `NativeDeWorldRegistryDoll` | SAME |
| `NativeDeWorldRegistryDragonKnight` | SAME |
| `NativeDeWorldRegistryEffect` | SAME |
| `NativeDeWorldRegistryElf` | SAME |
| `NativeDeWorldRegistryIllusionist` | SAME |
| `NativeDeWorldRegistryItem` | SAME |
| `NativeDeWorldRegistryKnight` | SAME |
| `NativeDeWorldRegistryMob` | SAME |
| `NativeDeWorldRegistryPet` | SAME |
| `NativeDeWorldRegistryQuest` | SAME |
| `NativeDeWorldRegistrySummons` | SAME |
| `NativeDeWorldRegistryTrap` | SAME |
| `NativeDeWorldRegistryTrap$TrapCell` | SAME |
| `NativeDeWorldRegistryWizard` | SAME |
| `NativeDeWorldScope` | SAME |
