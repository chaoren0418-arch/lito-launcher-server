# @AsRecord 暫存批次轉換器

Issue #102／T020g：將 Java 可編譯的 `final class` 原地轉成保留額外實例欄位的 JVM record。伺服器 `pom.xml` 未接線；Maven 生命週期與 JSON 掃描交給 #134。

## 使用

需要 JDK 17+、Python 3.9+。`JAVA_HOME` 可指定 JDK；ASM 9.7 的 `asm`／`asm-tree` 優先讀取 `AS_RECORD_MAVEN_REPO`（預設 `~/.m2/repository`），否則下載至本工具 `target/dependency/`。兩種來源均驗 SHA-256；不寫入 Maven 共用快取。

```bash
bash tools/as-record/as-record <classes目錄>
# 或跨平台直接呼叫
python tools/as-record/as_record.py <classes目錄>
```

Windows 可使用 `tools\as-record\as-record.cmd <classes目錄>`，`PYTHON` 環境變數可指定 Python 執行檔。CLI 每次重新編譯工具，避免沿用過期 classes；產物僅在本工具 `target/`。已建置時可直接呼叫 Java：

```text
java -cp <target/classes及兩個ASM-JAR的classpath> local.lito381.asrecord.AsRecord <classes目錄>
```

成功輸出 `as-record: scanned=N converted=M`，退出碼 0；無效目錄、標註或 I/O 錯誤為 1。目錄會遞迴掃描，不跟隨目錄 symlink。先驗證整批再寫回；驗證錯誤不會部分改寫，但寫回階段的 I/O 中斷不提供整批交易回復。因此只對可重建的暫存 classes 使用。未標註檔案不寫回，重複執行 bytes 不變。

## 原始碼契約

註解位於 `src/main/java/local/lito381/server/AsRecord.java`（CLASS retention）。只列出欄位名稱，描述子與 generic signature 由實際欄位取得，Record 元件與 bootstrap handles 按 `components` 的順序產生。

```java
@AsRecord(components = {"id"}, finalFields = {"stamp"})
final class Snapshot {
    private final int id;
    private Object stamp;
    int extra;

    Snapshot(int id, Object stamp) { this.id = id; this.stamp = stamp; }
    Snapshot(int id) { this.id = id; } // stamp 保留 JVM 預設值，轉換時補 final
    public int id() { return id; }
}
```

- 類別宣告 `final`，巢狀類別必要時加 `static`；元件為 `private final`，並自行提供原有 public accessor 與建構子。
- 額外欄位依候選的可見性／旗標宣告。僅在 javac 無法通過確定賦值檢查時，先宣告為非 final 並列入 `finalFields`；這份名單是呼叫端對原始位元碼的明確描述。
- 不宣告 `equals(Object)`、`hashCode()`、`toString()`，轉換器遇到這些方法會報錯，不覆寫使用者方法。
- `finalFields` 必須是存在、非 static、非 volatile 的實例欄位；重複名稱、缺元件、缺 public accessor、錯誤父類別、抽象類別等會報錯。
- 父類別／泛型父類別改為 `Record`，加入 `ACC_FINAL`、Record 屬性及 javac 的 `ObjectMethods.bootstrap` 方法與 `MethodHandles.Lookup` metadata。建構子只改 `ALOAD 0; Object.<init>()`，保留其他 `new Object()` 與 `this(...)`。
- 其餘方法與欄位不改，保留其他註解、frames、debug 資訊；移除類別的 `@AsRecord` 屬性。欄位註解不會自動複製到 record component，這個 CLI 沒有元件註解的來源描述功能。

## 驗證

```bash
python tools/as-record/selftest.py
# CFR 0.152 JAR 由呼叫端提供，候選預設為 reference/candidate-50635f7e.jar
python tools/as-record/candidate-selftest.py --cfr <cfr-0.152.jar>
```

自測以 javac 編譯一般 record／相同內容的標註類別，使用現有 `tools/semantic-diff` 比較。包含一般／generic／空元件／巢狀／元件排序、額外欄位與跨類別 getfield/putfield、建構鏈與 `new Object()`、獨立 ASM `finalFields` 對照、未標註 bytes 不變、重複轉換不變、錯誤輸入整批不寫回。JVM 行為測試開啟 `-Xverify:all`。

fixture 原始碼、JSON 放 repo `scratch/as-record/selftest/`，class/JAR 放本工具 `target/selftest/`，皆不提交。候選 `NativeReturnNatureMaterialAction$Target` 的 CFR 實測指令、結果與交接說明見 `docs/reports/issue-102.md`。
