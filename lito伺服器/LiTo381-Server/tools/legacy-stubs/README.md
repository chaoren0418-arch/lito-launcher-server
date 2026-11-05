# 舊伺服器編譯期簽名

這些 `com.lineage.*` 原始碼僅供隔離批次編譯，不是舊伺服器實作，不能放入 runtime classpath 或成品 JAR。所有手寫／產生的方法與私有防誤用建構子都拋出 `UnsupportedOperationException("compile-only stub")`。Java enum 必需的常數、`values/valueOf` 與繼承的 `ordinal/name` 由 javac／JDK 提供，不能覆寫成拋錯版本。

從 repo 根目錄執行（輸入／輸出預設路徑以腳本位置決定）：

```powershell
py -3 -B -X utf8 lito伺服器/LiTo381-Server/tools/legacy-stubs/scan.py
py -3 -B -X utf8 lito伺服器/LiTo381-Server/tools/legacy-stubs/scan.py --check
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot'
& lito伺服器/LiTo381-Server/tools/legacy-stubs/build.ps1
py -3 -B -X utf8 lito伺服器/LiTo381-Server/tools/legacy-stubs/selftest.py
```

上例相對路徑命令在 repo 根目錄執行；從其他目錄請給腳本絕對路徑。Java 17 以上可用；`JAVA_HOME` 未設定時使用 PATH 的 `javac`。Linux／Bash 入口：

```bash
python3 -B tools/legacy-stubs/scan.py --check
bash tools/legacy-stubs/build.sh
javac --release 17 -cp 'reference/base-0.6.71.jar:target/legacy-stubs:lib/*' \
  -d target/batch /path/to/scratch/Sample.java
```

這組 Bash 命令在 `lito伺服器/LiTo381-Server` 執行。Windows classpath 分隔符改用 `;`。`build.*` 只呼叫 javac，輸出 `target/legacy-stubs`，沒有打包步驟。Maven source root、打包排除與 CI 接線交 #134；本任務未改 `pom.xml`。

## 清單與生成方式

`scan.py` 僅使用 Python 標準函式庫，預設掃描 `reference/base-0.6.71.jar` 與 `reference/candidate-50635f7e.jar`，輸出 repo `docs/combat-merge/legacy-stubs.json` 及 server `compile-stubs/`。可用 `--jars A.jar B.jar --output path.json --stubs dir` 指定隔離輸入／輸出。

- 掃描全部 class 的常數池 Class／Fieldref／Methodref／InterfaceMethodref／MethodHandle、描述子與 Code。處理 `wide`、`tableswitch`、`lookupswitch`，避免把操作數誤當 opcode。
- 每個引用保留 JAR 名稱、class entry、CP 索引或方法與 bytecode offset；輸入記錄 SHA-256。不把任意字串／除錯路徑當類別依賴。
- `invokeinterface`／InterfaceMethodref 推導 interface；自型別 `getstatic` 配合 enum API／switch map／ACC_ENUM 推導 enum，避免將一般 singleton 欄位誤判為列舉。其餘依證據生成 class，不猜實際繼承、泛型、常數值或未引用方法。
- `$SwitchMap$com$lineage$...` 中的舊名稱與實際 `values()` 呼叫關聯；依 runtime enum 的巢狀深度還原 `/` 與 `$`，從同一方法對 runtime enum 的 `getstatic` 取得常數名。這些列標記 `switch-map`／`switch-map-alias`，不是偽稱 JAR 直接呼叫舊 enum。此推導假設重映射保留巢狀深度；未識別的初始化形狀會失敗，不猜常數名。
- 本輪直接 CP 類別 9 個；switch map 額外推得 5 個 enum，另補 2 個外層宣告，共 16 個型別、9 個 Java 檔、26 個常數。額外涵蓋 `NpcDeletionVisibilityState.Phase`、`PlayerFilter`、`World.SpatialReconcileResult`，都有 JAR 證據。只有 metadata 引用的 `ItemGenerationContext.Source` 等暫按 class 輸出，不能據此聲稱部署端實際型別種類。
- `--check` 完全不寫檔，驗證重新計算的內容與 Java 檔案集合；輸出有多餘 Java 時報錯，需人工審查，不自行刪除。未知 static／instance 或不能安全生成的簽名亦報錯。
- 私有拋錯建構子用來抑制未引用 class 的隱含公開建構子；列舉常數依名稱排序，順序不代表舊伺服器 ordinal。禁止執行這批 stub。

`selftest.py` 的 Java 都是明確標示的合成測試材料，寫入 repo `scratch/issue-108/selftest/`，class／JAR 在其 `target/`。驗證 interface、enum、singleton、欄位、靜態／實例方法、method handle、陣列描述子、switch／wide 對齊、截斷輸入拒絕、消費端重編及拋錯；不讀舊伺服器原始碼。

驗收樣本、已知語意差異及下一棒處理事項見 `docs/reports/issue-108.md`（repo 根目錄）。
