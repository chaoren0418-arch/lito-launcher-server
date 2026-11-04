# 工作報告

本檔給調度器與下一棒交接用。同一主題的細節在 `docs/reports/`，此處只留最新摘要。

## 本輪變更

- Issue #4 / T003：`Build.ps1` 改走 `py -3.12` 或 PATH 的 `python`；`build_current.py` 改走 PATH 的 `dotnet`；`deployment/release-inputs.json` 補上相對 repo 根的 `server_jar`／`catalog`。未改 publish 參數與 C# 來源。詳見 `docs/reports/issue-4.md`。

## 下一步

- Issue #4：本機 test 任務在 Windows clone 後跑 `Build.ps1` 直到 publish；核對 `release/current` 產物。CI launcher job 由 PR 檢查。
