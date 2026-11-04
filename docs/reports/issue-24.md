# Issue #24 / T020 — 戰鬥候選合併策略（設計）

## 做了什麼
- 新增 `docs/design/T020-combat-merge.md`：盤點候選 patch 鏈、決定「類別層級差異併入」策略、風險表、任務表。
- 新增 `docs/plans/issue-24.json`：後續任務 T020a（resource, codex-local）、T020c（code, codex-local）、T020b（design, Claude, 依賴 T020a）、T040（code, cursor-cloud）。
- `TASKS.md`：T005 → done（Issue #3 由 #12/#20 達成）、T020 → doing、T040 已拆；補 Issue／TASKS 編號撞號註記。
- 不改 `src/main`、不部署候選、不碰正式 JAR／設定／DB；`docs/REPORT.md` 未改。

## 怎麼驗證
- 已驗證（repo 靜態盤點，Linux 腳本）：解析 `tools/combat-continuation/*/build.py` 的父層 JAR 引用，得 66 層線性鏈、無孤立層；`integration-lifecycle/` 有 252 支 `build_*.py`、1,257 個 `.java`；399 支腳本使用 ASM splice；1,573 支 `.py` 中 1,008 支硬編 `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`、417 支硬編 `D:\`。
- 已驗證：根 `candidate-combat-buff.jar` 的父層為 `tools/resource-database` 的 `native-item-weapon-material`（`df9464f0…`），不是正式 0.6.71。
- 已驗證（審查回饋後更正）：正式合併基準為 `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`，main @ `9b9b09f`、blob `d59ae2e4…`，`git show origin/main:<path> | sha256sum` = `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`（本機部署檔經審查核對同 SHA）。第一版誤寫的 `75da69b0…` 是 `continuation-current.json`（2026-10-20）的歷史紀錄，已在設計中區分。
- 已驗證：`continuation-current.json` 記錄最後同 SHA 全回歸通過者為 `8d26c9d7…`；`50635f7e…` 僅有針對性案例（`docs/maintenance/combat-integration-lifecycle.zh-TW.md`）。
- `docs/plans/issue-24.json` 以 `json.load` 驗證格式。

## 審查退回後的修正（第 2 輪）
1. [P1] 正式基準改為不可變的 reference 檔＋commit＋完整 SHA，T020a 規定 SHA 不符即停止；歷史 `75da69b0…` 另列。
2. [P2] T020c 範圍補上 `replay_targeted.py`、`replay_player_cancellation.py`、`replay_composed_materials.py`、`test_curse_clock_boundary.py`、`seal_validation.py` 與其子驅動／fixture；驗收改成各套件獨立收據與案例數對照（2,400／5,164／20＋18／24）；stage 改 `test`（本機）。設計 §2-4 加套件表。
3. 分支已 rebase 到 main @ `9b9b09f`。

## 可重現指令（來源 commit：main @ `9b9b09f` 上的 `task/24`；Linux bash，於 `lito伺服器/LiTo381-Server/tools/combat-continuation`）
```bash
# 66 層線性鏈：解析每層 build.py 的 R.parent/'<layer>' 父層引用並串接（無孤立層）
for d in */; do [ -f "${d}build.py" ] && echo "$d"; done | wc -l   # 66（含 build.py 的層目錄；其父層引用串成單一線性鏈）
ls integration-lifecycle/build_*.py | wc -l                                     # 252
ls integration-lifecycle/*.java | wc -l                                         # 1257
find . -name '*.py' -not -path '*/build/*' | wc -l                              # 1573
grep -rl "'splice'" --include=*.py . | wc -l                                    # 399
grep -rl "C:/Program Files/Microsoft/jdk" --include=*.py . | wc -l              # 1008
grep -rlE 'D:/|D:\\\\' --include=*.py . | wc -l                                # 417（含正斜線與跳脫反斜線）
git show origin/main:lito伺服器/LiTo381-Server/reference/base-0.6.71.jar | sha256sum   # e1498871…
```

## 未驗證什麼
- 實際類別差異數量、正式 0.6.71 與候選根的分歧點（需本機 JAR → T020a）。
- 回歸工具在非 `C:` JDK 下能否重現既有收據（→ T020c）。
- 本分支沒有 CI 執行（workflow 只在 main push／PR 觸發），不宣稱 CI 綠。

## 產出路徑
`docs/design/T020-combat-merge.md`、`docs/plans/issue-24.json`、`TASKS.md`、`docs/reports/issue-24.md`

## 給下一棒的提示
T020a 與 T020c 可並行；T020b 等 T020a 的 `docs/combat-merge/class-diff.json`。
