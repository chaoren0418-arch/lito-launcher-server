# REPORT

本檔由各輪任務更新「本輪變更」與「下一步」。歷史細節見 `docs/reports/`。

## 本輪變更

- Issue #2（T007）：`pom.xml` 版本改 0.6.71，預設 Main-Class 改 `local.lito381.server.SharedResourceMain`；CI 改 `mvn package` 並用 `jar tf` 產出／上傳 `docs/server-source-gap.md`。只在 reference 的 16 個類別已分類（待補 1：`NativeDeVisibilitySwitch`；刻意排除 15 個 inner／匿名類；Modern* 0）。PR #7 CI 已綠（Server + Launcher）。
- Issue #12（T009）：本機資源測試加統一 `LocalResources` assume 守門（330 類：properties 81／lineage 253／LiTo.bin 95／ida 169），CI 新增 blocking `server-test`（無 `continue-on-error`）。無 DB／無本機檔 `mvn test`：5995 run、0 fail、162 skipped。詳見 `docs/reports/issue-12.md`。

## 下一步

- Issue #2：PR #7 CI 已綠（Server + Launcher）。下一步由 Claude 審合併，並把 `TASKS.md` T007 標 review／done；T008 補 `NativeDeVisibilitySwitch`；T005 修 testCompile 後再拿掉 `-Dmaven.test.skip=true`。
- Issue #12：等 GitHub Actions `server-test` 綠後由 Claude 審合併；注意與 PR #10（T005）的 workflow 衝突。`TASKS.md` 尚無 T009，合併後補條目。原生 catalog pin 仍靠 `-Dlito.native.catalog.pins=true` 選擇性跑。
