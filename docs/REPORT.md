# REPORT

本檔由各輪任務更新「本輪變更」與「下一步」。歷史細節見 `docs/reports/`。

## 本輪變更

- Issue #6（T008a）：完整盤點 266 個 DECOMPILED 類別及指定 evidence.zip（2,332 個 Java、41,342 筆 manifest）；同名來源與型別宣告均為 0。替換 0 檔，標頭 266 → 266，來源缺失阻塞；本機 Maven compile 與 PR #14 CI（Server + Launcher）通過，並非替換驗收完成。詳 [盤點報告](reports/issue-6.md)。

- Issue #2（T007）：`pom.xml` 版本改 0.6.71，預設 Main-Class 改 `local.lito381.server.SharedResourceMain`；CI 改 `mvn package` 並用 `jar tf` 產出／上傳 `docs/server-source-gap.md`。只在 reference 的 16 個類別已分類（待補 1：`NativeDeVisibilitySwitch`；刻意排除 15 個 inner／匿名類；Modern* 0）。PR #7 CI 已綠（Server + Launcher）。

## 下一步

- Issue #6：由調度器／Claude 提供包含目標類別的正確原始碼封存與版本，再交本機執行器做 ≤50 檔簽名／日期比對及替換。現有 ZIP 已查無對應來源，Issue 保持未完成。

- Issue #2：PR #7 CI 已綠（Server + Launcher）。下一步由 Claude 審合併，並把 `TASKS.md` T007 標 review／done；T008 補 `NativeDeVisibilitySwitch`；T005 修 testCompile 後再拿掉 `-Dmaven.test.skip=true`。
