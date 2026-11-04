# T020 戰鬥候選併入 `src/main` — 合併策略（設計）

Issue #24。2026-11-04，Claude 主控。本文只做決策與拆任務，不改 `src/main`、不部署候選。

## 1. 現況（已驗證：以 repo 內容盤點）

| 項目 | 事實 |
|---|---|
| 正式版 | 0.6.71 JAR `75da69b0…`（`lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar`）。T002／T007 後 `mvn package` 可從 `src/main` 建出，CI 用 `jar tf` 與 `reference/base-0.6.71.jar` 比對類別清單。 |
| `src/main` | 940 個 `.java`，其中 266 個是 `// DECOMPILED`（T008 確認找不到原始碼）。 |
| 候選鏈根 | `tools/combat-continuation/build/candidate-combat-buff.jar`（由 `integrate_buff.py` 疊出），它的父層是 `tools/resource-database` 的 `native-item-weapon-material`（`df9464f0…`）。**不是正式 0.6.71 JAR**。 |
| 第一段鏈 | 66 層，線性：`solid-control → invisibility-control → triple-control → bind-control → invisibility-leases → elemental-material → elemental-lifetime → summon-material → summon-parity → summon-ring → water-life-material → protection-material → armor-material → armor-state → negative-material → weakness-material → ground-material → silence-material → decay-material → world-potion → target-negative-material → blind-material → resurrection-material → resurrection-lifetime → resurrection-companion → resurrection-ingress → poison-material → poison-scope → poison-owner → poison-tick → poison-retirement → poison-publication → earth-bind-material → pet-controls → polymorph-material → return-nature-material → return-nature-transfer → polymorph-source → pet-material → pet-world-lifetime → pet-world-publication → weapon-break-numeric → npc-physical-source → weapon-break-material → pet-world-policy → weapon-break-lifetime → npc-physical-size → cancellation-material → cancellation-speed-immunity → cancellation-debuff-immunity → weight-material → cancellation-probability → cancellation-npc → cancellation-npc-static → cancellation-npc-replacement → cancellation-npc-material → cancellation-npc-runtime → area-material → area-sleep-slow → combat-config-origin → mass-teleport-material → mass-teleport-lifetime → call-nature-material → cancellation-battle-material → cancellation-battle-runtime → cancellation-static-runtime`。 |
| 第二段鏈 | `integration-lifecycle/`：252 支 `build_*.py`、1,257 個 `.java`，每支讀本機 `build/*-current.json` 找父層。`50635f7e…`（negative final frame）在這一段。 |
| 每層做法 | 兩種混用：(a) 把某類別的**完整 `.java`** 複製後字串替換，再用 javac 編進去；(b) 用 ASM 工具（`*Methods.java` 的 `splice`）直接改 bytecode，**沒有對應的原始碼**（例：`ClientSession`）。399 支腳本用了 splice。 |
| 可重現性 | 1,573 支 `.py` 裡有 1,008 支硬編 `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`，417 支硬編 `D:\` 絕對路徑；中間 JAR 和收據都在本機 `build/`，不在 git。 |
| 驗證狀態 | 最後一個**同 SHA 全回歸**通過的候選是 `8d26c9d7…`（第 65 層 cancellation-battle-runtime：2,400 案／45,156 檢查、5,164 組合、20＋18 組回歸）。`50635f7e…` 只跑了針對性案例，沒做全回歸。 |

## 2. 決策

1. **不重播 patch 鏈**。320 步依賴本機中間產物和硬編路徑，搬進 Maven 的成本比收益大，而且會把「疊 patch」永久帶進主線。
2. **改做類別層級的差異併入**：以最終候選 JAR 對正式 0.6.71 JAR（以及 `mvn package` 產物）逐類別比對，對每個有差異的類別：
   - **有原始碼**（某層 `sources/*.java` 或 `inputs/*.java` 的最後一版）→ 直接用該版取代 `src/main` 對應檔，再把正式版後來才有的改動（T029 清單）合回去。
   - **只有 ASM splice**（沒有原始碼）→ 以 CFR 反編譯該類別在候選 JAR 中的版本，和 `src/main` 現版做三方比對（正式版／候選版／`src/main`），手工移植成可讀的原始碼，標註 `// ported from candidate <sha>, layer <name>`。
   - **新類別** → 優先用原始碼；沒有原始碼才反編譯，並標註 `DECOMPILED`。
3. **併入目標**：以 `50635f7e…` 為最終內容，但**先併 `8d26c9d7…`**（已做全回歸），讓第一批有完整驗收基準；`8d26c9d7 → 50635f7e` 的差異列為第二批。
4. **驗收方式**：把候選回歸工具改成可以吃任意 JAR 路徑、用 PATH 中的 JDK，然後對 `mvn package` 產物重跑 2,400 案；結果必須和該候選 SHA 的既有收據一致。
5. **候選工具鏈**：合併完成且回歸一致後才停用（標 deprecated，不刪，歷史留 git）。

## 3. 風險

| 風險 | 影響 | 對策 |
|---|---|---|
| 候選根不是正式 0.6.71：正式版的 L1PolyMe、職業圖示、`litoddraw.dll`、GM7200 等改動可能不在候選裡 | 直接拿候選類別覆蓋會**倒退正式功能** | T020a 一併列出「正式有、候選沒有」的差異，三方比對時保留；T029 驗證。 |
| ASM splice 類別（如 `ClientSession`）無原始碼 | 手工移植可能漏行為 | 三方比對＋回歸逐案一致；移植檔案獨立 commit 方便審查。 |
| 266 個 DECOMPILED 檔與候選同一類別衝突 | 合併時以為是原始碼 | T020a 標出哪些差異類別在 `src/main` 是 DECOMPILED。 |
| 回歸工具硬編 Windows 路徑 | CI 無法跑 | T020c 只改回歸工具；回歸在本機（codex-local）跑，CI 只跑 `mvn test`。 |
| `50635f7e` 未全回歸 | 第二批沒有基準 | 第二批併入後對 mvn 產物跑全回歸，結果寫成新基準（T030 的前置）。 |
| 中間產物只在本機 | 雲端代理做不了 | 需要候選 JAR 的任務一律派 `codex-local`。 |

## 4. 拆出的任務（`docs/plans/issue-24.json`）

| ID | 階段 | 執行 | 內容 | 依賴 |
|---|---|---|---|---|
| T020a | resource | codex-local | 本機類別差異盤點（候選 `8d26c9d7`、`50635f7e` × 正式 0.6.71 × `mvn package` 產物），每類別分類並找最後一版原始碼 | — |
| T020c | code | codex-local | 回歸工具可吃任意 JAR、JDK 走 PATH；對 `8d26c9d7` 重跑結果與既有收據一致 | — |
| T020b | design | Claude | 依 T020a 結果訂批次（每批 ≤ 30 類別）並拆 code 任務 | T020a |
| T040 | code | cursor-cloud | `docs/INSTALL.md`＋打包腳本 | — |

T020b 完成後，T021–T029 的缺口改成在合併後的 `src/main` 上逐項做，不再另疊候選層。

## 5. 未驗證

- 候選 JAR 的實際類別差異數量（需本機 JAR，交 T020a）。
- 正式 0.6.71 與候選根的分歧點（交 T020a）。
- 回歸工具在非 `C:` JDK 下是否還能重現既有收據（交 T020c）。
