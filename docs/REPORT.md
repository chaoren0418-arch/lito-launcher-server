# REPORT

本檔由各輪任務更新「本輪變更」與「下一步」。歷史細節見 `docs/reports/`。

## 本輪變更

- Issue #2（T007）：`pom.xml` 版本改 0.6.71，預設 Main-Class 改 `local.lito381.server.SharedResourceMain`；CI 改 `mvn package` 並用 `jar tf` 產出／上傳 `docs/server-source-gap.md`。只在 reference 的 16 個類別已分類（待補 1：`NativeDeVisibilitySwitch`；刻意排除 15 個 inner／匿名類；Modern* 0）。PR #7 CI 已綠（Server + Launcher）。

## 下一步

- Issue #2：PR #7 CI 已綠（Server + Launcher）。下一步由 Claude 審合併，並把 `TASKS.md` T007 標 review／done；T008 補 `NativeDeVisibilitySwitch`；T005 修 testCompile 後再拿掉 `-Dmaven.test.skip=true`。
- 2026-11-04 — #12 T009: 測試依賴本機資源的全部加 assume 守門，讓 mvn test 全綠（code，codex-local，PR #15 已合併）→ [報告](reports/issue-12.md)
- 2026-11-04 — #20 T010: 修復 T009 清單上的真實測試失敗（約 30 案）（code，cursor-cloud，PR #22 已合併）→ [報告](reports/issue-20.md)
- 2026-11-04 — #24 T020: 戰鬥候選併入 src/main 的合併策略（設計）＋ M3 T040 拆任務（design，claude-chat，分支 task/24）→ [報告](reports/issue-24.md)
- 2026-11-04 — #4 T003: 登入器建置腳本去硬編路徑（code，codex-local，PR #8 已合併）→ [報告](reports/issue-4.md)
- 2026-11-04 — #32 T040: 安裝手冊＋打包腳本（code，cursor-cloud，PR #33 已合併）→ [報告](reports/issue-32.md)
