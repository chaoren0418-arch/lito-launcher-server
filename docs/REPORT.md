# 工作報告

本檔給調度器與下一棒交接用。同一主題的細節在 `docs/reports/`，此處只留最新摘要。

## 本輪變更

- Issue #4 / T003：`Build.ps1` 改走 `py -3.12` 或 PATH 的 `python`；`build_current.py` 改走 PATH 的 `dotnet`；`deployment/release-inputs.json` 補上相對 repo 根的 `server_jar`／`catalog`。未改 publish 參數與 C# 來源。詳見 `docs/reports/issue-4.md`。
- Issue #4：審查退回後已 `git rm --cached` 拿掉 `deployment/release-inputs.json`（每機 `java_runtime` pin 不同，不可進 git）。`verify_current.py` 沒有 `server_jar` 欄時仍用預設相對路徑。

## 下一步

- Issue #4：CI 已綠（PR #8 launcher + server）。下一步由本機 test 在 Windows clone 後跑 `Build.ps1` 直到 publish，核對 `release/current` 產物。
- Issue #4：Claude 複審 PR #8（確認 `release-inputs.json` 已不在 diff）；通過後本機 test 接手 `Build.ps1` publish。
