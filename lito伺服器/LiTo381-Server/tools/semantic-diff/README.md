# ASM 語意比對

使用 Java 17。Bash 入口優先使用 Maven，無 Maven 時以 javac＋通過 SHA-256 校驗的 ASM 9.7 建置；不影響伺服器 runtime 依賴。

```bash
bash tools/semantic-diff/semantic-diff --left A.jar --right B.jar \
  --classes entries.txt --exclude owned-by-B.txt \
  --allow-methods methods.txt --allow-right C.jar \
  --json scratch/result.json --md scratch/result.md
```

所有篩選／允許旗標皆可省略。`--classes`／`--exclude` 接受檔案或逗號清單，類名可用斜線或點號並可帶 `.class`。`--classes` 精確選取 entry；`--exclude` 同時排除指定外層類及其 `$` 內部類。

使用 `--classes` 時，JSON 的 `classSelection.missingLeft`／`extraLeft` 和 Markdown 會另列左側缺少的指定 entry／左側多出的清單外 entry（套用 exclude）。多出項不加入語意比較計數；批次要求輸出恰好等於清單時，兩個陣列也必須為空。清單 entry 兩邊都不存在仍會直接報錯。

`--allow-methods` 每行為 `類別#方法名稱描述子`，方法名必須精確匹配 C.jar 的原始名稱（合成名稱於匹配後才正規化）。指定方法改對 C 比較，其他方法及類別／欄位屬性仍對 B 比較。指定方法只在 C 存在、A 尚未移植時會產生 DIFF；C 其他未指定方法不會被加入比較。清單拼錯、C 缺少方法、類別不存在或被 `--classes`／`--exclude` 排除都以非零退出，不產生成功摘要。共用大清單應先裁切成本批的允許方法。

成功產生報告時退出碼為 0（包括有 DIFF）；輸入無效或執行失敗為 1。批次驗收須檢查 JSON 的 DIFF／ONLY_LEFT／ONLY_RIGHT 計數皆為 0，不能只看退出碼。JSON 的 `firstDiff` 包含方法名稱描述子及從 0 起算的正規化指令位置（含 LABEL）；類別／欄位屬性或方法缺漏時位置為 null。`BYTE_SAME` 是原始 A/B 的 bytes 比較；套用 C 後即使 A/B bytes 相同仍可能 DIFF。

```bash
bash tools/semantic-diff/selftest.sh
bash tools/semantic-diff/selftest-fallback.sh
```

後者在獨立 PATH（確實找不到 mvn）及含空白路徑內，冷啟動 CLI、下載與校驗依賴、javac 編譯、執行 SAME／DIFF 比較並驗證 build.sh。CI 會同時執行兩支測試。

工具依賴位於 `target/dependency/`，工具 classes 位於 `target/classes/`，測試 class/JAR 位於 `target/selftest/`；fixture、清單、JSON 與日誌位於 repo `scratch/semantic-diff-*`。fallback 的隔離工具及 fixture 位於 repo scratch，其編譯與依賴仍在隔離工具的 `target/`。這些生成內容不提交 git。
