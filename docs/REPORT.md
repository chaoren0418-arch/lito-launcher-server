# REPORT — lito-launcher-server

更新：2026-11-04。本檔記錄各輪已落地變更與下一步；歷史細節見 git 與 `docs/reports/`。

## 本輪變更
- Issue #3 / T005：36 個 `Legacy*IntegrationTest` 加 `@BeforeAll` `Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"))`；CI 新增無 DB 的 `server-test`（`mvn test`）job。為讓測試能編譯，另修 2 個範圍外編譯錯誤（`GameHubChannelTest` 缺 `assertNotEquals`、`NativeExternalControlV2Fixture` 建構子參數）。詳見 `docs/reports/issue-3.md`。

## 下一步
- Issue #3：全套 `mvn test` 仍紅（332 類、約 822 個 failure/error），主因是範圍外測試硬讀本機 `config/server.properties`、`1-伺服器` 來源與 `LiTo.bin`，不是 Legacy MySQL 守門。建議下一棒（Claude 審查或另開任務）決定要不要再為這些測試加 assume／路徑守門，否則 CI `server-test` 會紅。
