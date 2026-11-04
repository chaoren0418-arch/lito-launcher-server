# REPORT

本檔由各輪任務更新「本輪變更」與「下一步」。歷史細節見 `docs/reports/`。

## 本輪變更

- Issue #2（T007）：`pom.xml` 版本改 0.6.71，預設 Main-Class 改 `local.lito381.server.SharedResourceMain`；CI 改 `mvn package` 並用 `jar tf` 產出／上傳 `docs/server-source-gap.md`。只在 reference 的 16 個類別已分類（待補 1：`NativeDeVisibilitySwitch`；刻意排除 15 個 inner／匿名類；Modern* 0）。PR #7 CI 已綠（Server + Launcher）。

## 下一步

- Issue #2：PR #7 CI 已綠（Server + Launcher）。下一步由 Claude 審合併，並把 `TASKS.md` T007 標 review／done；T008 補 `NativeDeVisibilitySwitch`；T005 修 testCompile 後再拿掉 `-Dmaven.test.skip=true`。
