# Issue #24 / T020 — 戰鬥候選合併策略（設計）

## 做了什麼
- 新增 `docs/design/T020-combat-merge.md`：盤點候選 patch 鏈、決定「類別層級差異併入」策略、風險表、任務表。
- 新增 `docs/plans/issue-24.json`：後續任務 T020a（resource, codex-local）、T020c（code, codex-local）、T020b（design, Claude, 依賴 T020a）、T040（code, cursor-cloud）。
- `TASKS.md`：T005 → done（Issue #3 由 #12/#20 達成）、T020 → doing、T040 已拆；補 Issue／TASKS 編號撞號註記。
- 不改 `src/main`、不部署候選、不碰正式 JAR／設定／DB；`docs/REPORT.md` 未改。

## 怎麼驗證
- 已驗證（repo 靜態盤點，Linux 腳本）：解析 `tools/combat-continuation/*/build.py` 的父層 JAR 引用，得 66 層線性鏈、無孤立層；`integration-lifecycle/` 有 252 支 `build_*.py`、1,257 個 `.java`；399 支腳本使用 ASM splice；1,573 支 `.py` 中 1,008 支硬編 `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`、417 支硬編 `D:\`。
- 已驗證：根 `candidate-combat-buff.jar` 的父層為 `tools/resource-database` 的 `native-item-weapon-material`（`df9464f0…`），與正式 0.6.71（`75da69b0…`）不同。
- 已驗證：`continuation-current.json` 記錄最後同 SHA 全回歸通過者為 `8d26c9d7…`；`50635f7e…` 僅有針對性案例（`docs/maintenance/combat-integration-lifecycle.zh-TW.md`）。
- `docs/plans/issue-24.json` 以 `json.load` 驗證格式。

## 未驗證什麼
- 實際類別差異數量、正式 0.6.71 與候選根的分歧點（需本機 JAR → T020a）。
- 回歸工具在非 `C:` JDK 下能否重現既有收據（→ T020c）。

## 產出路徑
`docs/design/T020-combat-merge.md`、`docs/plans/issue-24.json`、`TASKS.md`、`docs/reports/issue-24.md`

## 給下一棒的提示
T020a 與 T020c 可並行；T020b 等 T020a 的 `docs/combat-merge/class-diff.json`。
