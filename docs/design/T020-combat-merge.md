# T020 戰鬥候選併入 `src/main` — 合併策略（設計）

Issue #24。2026-11-04，Claude 主控。本文只做決策與拆任務，不改 `src/main`、不部署候選。

## 1. 現況（已驗證：以 repo 內容盤點）

| 項目 | 事實 |
|---|---|
| 正式版（合併目標基準） | **`lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`**，main @ `9b9b09f33639f100b7ce02f42a4722ec3f9f27cf`、blob `d59ae2e4c56f599456e764a205a0e09a825975d3`，SHA256 **`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`**。本機部署檔 `lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar` 經審查核對同 SHA。T002／T007 後 `mvn package` 可從 `src/main` 建出，CI 用 `jar tf` 與此 reference 比對類別清單。 |
| 歷史紀錄（**不是**合併基準） | `continuation-current.json`（2026-10-20）記的正式 SHA `75da69b0…` 是當時候選驗證所對照的正式檔，之後正式版已更新為 `e1498871…`。T020a 只能拿它來說明候選驗證當時的對照基準，不得當作比較輸入。 |
| `src/main` | 940 個 `.java`，其中 266 個是 `// DECOMPILED`（T008 確認找不到原始碼）。 |
| 候選鏈根 | `tools/combat-continuation/build/candidate-combat-buff.jar`（由 `integrate_buff.py` 疊出），它的父層是 `tools/resource-database` 的 `native-item-weapon-material`（`df9464f0…`）。**不是正式 0.6.71 JAR**。 |
| 第一段鏈 | 66 層，線性：`solid-control → invisibility-control → triple-control → bind-control → invisibility-leases → elemental-material → elemental-lifetime → summon-material → summon-parity → summon-ring → water-life-material → protection-material → armor-material → armor-state → negative-material → weakness-material → ground-material → silence-material → decay-material → world-potion → target-negative-material → blind-material → resurrection-material → resurrection-lifetime → resurrection-companion → resurrection-ingress → poison-material → poison-scope → poison-owner → poison-tick → poison-retirement → poison-publication → earth-bind-material → pet-controls → polymorph-material → return-nature-material → return-nature-transfer → polymorph-source → pet-material → pet-world-lifetime → pet-world-publication → weapon-break-numeric → npc-physical-source → weapon-break-material → pet-world-policy → weapon-break-lifetime → npc-physical-size → cancellation-material → cancellation-speed-immunity → cancellation-debuff-immunity → weight-material → cancellation-probability → cancellation-npc → cancellation-npc-static → cancellation-npc-replacement → cancellation-npc-material → cancellation-npc-runtime → area-material → area-sleep-slow → combat-config-origin → mass-teleport-material → mass-teleport-lifetime → call-nature-material → cancellation-battle-material → cancellation-battle-runtime → cancellation-static-runtime`。 |
| 第二段鏈 | `integration-lifecycle/`：252 支 `build_*.py`、1,257 個 `.java`，每支讀本機 `build/*-current.json` 找父層。`50635f7e…`（negative final frame）在這一段。 |
| 每層做法 | 兩種混用：(a) 把某類別的**完整 `.java`** 複製後字串替換，再用 javac 編進去；(b) 用 ASM 工具（`*Methods.java` 的 `splice`）直接改 bytecode，**沒有對應的原始碼**（例：`ClientSession`）。399 支腳本用了 splice。 |
| 可重現性 | 1,573 支 `.py` 裡有 1,008 支硬編 `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`，417 支含 `D:` 絕對路徑（口徑：檔內出現 `D:/` 或 `D:\\` 任一形式，含正斜線與跳脫反斜線；見報告的指令）；中間 JAR 和收據都在本機 `build/`，不在 git。 |
| 驗證狀態 | 最後一個**同 SHA 全回歸**通過的候選是 `8d26c9d7…`（第 65 層 cancellation-battle-runtime）。完整收據由 `cancellation-battle-runtime/seal_validation.py` 判定，組成見 §2-4 的套件表。`50635f7e…` 只跑了針對性案例，沒做全回歸。 |

## 2. 決策

1. **不重播 patch 鏈**。320 步依賴本機中間產物和硬編路徑，搬進 Maven 的成本比收益大，而且會把「疊 patch」永久帶進主線。
2. **改做類別層級的差異併入**：以最終候選 JAR 對正式 0.6.71 JAR（以及 `mvn package` 產物）逐類別比對，對每個有差異的類別：
   - **有原始碼**（某層 `sources/*.java` 或 `inputs/*.java` 的最後一版）→ 直接用該版取代 `src/main` 對應檔，再把正式版後來才有的改動（T029 清單）合回去。
   - **只有 ASM splice**（沒有原始碼）→ 以 CFR 反編譯該類別在候選 JAR 中的版本，和 `src/main` 現版做三方比對（正式版／候選版／`src/main`），手工移植成可讀的原始碼，標註 `// ported from candidate <sha>, layer <name>`。
   - **新類別** → 優先用原始碼；沒有原始碼才反編譯，並標註 `DECOMPILED`。
3. **併入目標**：以 `50635f7e…` 為最終內容，但**先併 `8d26c9d7…`**（已做全回歸），讓第一批有完整驗收基準；`8d26c9d7 → 50635f7e` 的差異列為第二批。
4. **驗收方式**：把候選回歸工具改成可以吃任意 JAR 路徑、用 PATH 中的 JDK，然後對 `mvn package` 產物重跑**下表全部套件**；每個套件的案例數與通過狀態都必須和 `8d26c9d7` 的既有收據一致。只跑 20＋18 組**不算**完整回歸。

   | 套件 | 入口 | 既有收據（`8d26c9d7`） | 驗收數量 |
   |---|---|---|---|
   | targeted | `cancellation-battle-runtime/replay_targeted.py` | `cancellation-battle-runtime/build/targeted/receipt.json` | 27 組 |
   | player-cancellation | `cancellation-battle-runtime/replay_player_cancellation.py` | `.../build/player-cancellation/receipt.json` | 3 組；與 targeted 合計 **2,400 案** |
   | composed | `replay_composed_materials.py`（combat-continuation 根目錄） | `.../build/composed-replay/current.json` | 71 組合計 **5,164 案** |
   | existing_20／affected_18 | `full_regression.py`、`candidate_regression.py`、`affected_regressions.py` | `build/cancellation-battle-runtime-regression/<sha12>/{qualification,affected}-current.json` | 20＋18 組 |
   | curse-clock | `cancellation-battle-runtime/test_curse_clock_boundary.py` | `.../build/curse-clock-boundary/receipt.json` | 24 案 |
   | 封存判定 | `cancellation-battle-runtime/seal_validation.py` | 讀上列全部收據 | 全部 assert 通過 |

   SQL 啟動／清理收據（`sql_startup_and_owned_cleanup`）會啟動伺服器連 DB，不在 T020c 範圍，維持既有收據核對。
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
| T020a | resource | codex-local | 本機類別差異盤點（候選 `8d26c9d7`、`50635f7e` × 正式 reference `e1498871…` × `mvn package` 產物），每類別分類並找最後一版原始碼；SHA 不符即停止 | — |
| T020c | test | codex-local | §2-4 全部套件可吃同一個 `--jar`、JDK 走 PATH、收據可輸出到獨立目錄；對 `8d26c9d7` 重跑，各套件案例數與既有收據一致 | — |
| T020b | design | Claude | 依 T020a 結果訂批次（每批 ≤ 30 類別）並拆 code 任務 | T020a |
| T040 | code | cursor-cloud | `docs/INSTALL.md`＋打包腳本 | — |

T020b 完成後，T021–T029 的缺口改成在合併後的 `src/main` 上逐項做，不再另疊候選層。

## 5. 未驗證

- 候選 JAR 的實際類別差異數量（需本機 JAR，交 T020a）。
- 正式 0.6.71 與候選根的分歧點（交 T020a）。
- 回歸工具在非 `C:` JDK 下是否還能重現既有收據（交 T020c）。

## 6. 合併批次（T020b，Issue #43）

依據：#30 T020a 的 `docs/combat-merge/class-diff.json`（分支 `task/30`，主控以 review:pass 結案，豁免 1 筆歷史來源）。批次的類別清單見 `docs/combat-merge/batches.json`。

### 6-1. T020a 改寫了前提（已驗證）

| 比較 | 結果 | 含意 |
|---|---|---|
| `8d26c9d7` vs 正式 `e1498871` | 只差 4 CHANGED＋4 ONLY_IN_FORMAL | **正式 0.6.71 已包含第一批**（66 層），差的 8 筆都是正式版後來加的（GmPolyMe、SelectionRecovery、SharedPresentationPackets、職業 0..6、魔法娃娃 opcode 123、NativeArmorStatus >255）。§2-3 的「先併 8d26」實際上不用做。 |
| `50635f7e` vs `8d26c9d7` | 140 CHANGED＋483 NEW（380 筆完整 Java、243 筆 asm-only）→ **303 個外層類別** | 這才是要併入的戰鬥內容（第二批）。 |
| Maven 產物 vs 正式 | bytes 只有 1,008 相同；雲端以 `javap -c -p` 去行號／常數池編號／空白後試算，仍約 430 類別不同（上限估計，未扣除 lambda 編號等編譯器雜訊） | **`src/main` 與正式 0.6.71 不等價**。T002／T007 只比對了類別清單。這是第一階段要補的缺口。 |

### 6-2. 兩階段

- **第一階段（A）**：讓 `src/main` 在語意上等於正式 0.6.71。先做比對工具（T020d），依結果再訂修補批次（T020e，主控）。
- **第二階段（B）**：把第二批 303 個外層類別併入。B 批次的驗收用同一支比對工具：**被動到的每個類別，在 Maven 產物中必須與 `50635f7e` 候選語意相同**（T029 保留清單除外，那些要等於「候選＋正式改動」的合成版）。這個驗收和第一階段無關，所以 B 可以和 A 並行；B3 動到的 85 個既有類別，在批次內一併校正到候選版。

### 6-3. 共用前置

| ID | 階段 | 執行 | 內容 |
|---|---|---|---|
| T020d | code | cursor-cloud | ASM 語意比對工具 `lito伺服器/LiTo381-Server/tools/semantic-diff/`：去除 LineNumberTable／LocalVariableTable／SourceFile 等除錯屬性，正規化常數池、lambda 與合成方法命名後，逐類別比較；可用 `--classes` 指定範圍。CI 加一個非阻斷 job：Maven 產物 vs `reference/base-0.6.71.jar`，產出 `docs/server-semantic-gap.md`。 |
| T020f | resource | codex-local | 把候選 `50635f7e` JAR 放進 `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar`（比照 base-0.6.71.jar 的 `.gitignore` 例外），並把 380 筆凍結 Java 原始碼匯出到 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/sources/`，附 manifest（路徑、SHA、對應 class SHA）。這樣 B 批次可以在雲端做。 |

### 6-4. 第二階段批次（每批一個 PR，依序：B2 → B1a–d → B3-*）

| 批次 | 外層類別數 | 來源 | 做法 |
|---|---:|---|---|
| B2 | 17 | asm-only 新類別 | 用 CFR 反編譯候選 JAR 中的版本，檔頭標 `// DECOMPILED from candidate-50635f7e.jar, TODO review` |
| B1a／B1b／B1c／B1d | 51／51／51／48 | 有完整 Java 的新類別（含 45 個外層 Java＋內部 ASM 的混合類別；混合者內部類別以外層 Java 為準，與候選比對驗證） | 從 `merge-inputs/50635f7e/sources/` 搬入 `src/main/java/local/lito381/server/` |
| B3-battle | 29 | 既有 `Battle*` 類別的 ASM 修改 | 三方移植：以正式版（= 8d26 版）為基準、候選版為目標，用 CFR 反編譯兩者取差異，套到 `src/main` 原始碼；`src/main` 本身與正式不同時，先校正到正式版再套差異 |
| B3-combat-material | 23 | `Combat*`、`ForcedCombat`、`*MaterialAction` | 同上 |
| B3-native | 30 | 其他 `Native*` 等（其中 19 個在 `src/main` 是 DECOMPILED） | 同上；DECOMPILED 檔可直接以候選反編譯版取代，保留標頭 |
| B3-core | 3 | `ClientSession`、`Protocol`、`GameHub` | 同上，**加上 T029 保留項**：GmPolyMe.exitRoom、SelectionRecovery.finish、SharedPresentationPackets.bluePotion、七職業 sendRoomParticipant、Protocol 職業 0..6、魔法娃娃 opcode 123＋h(0)。這三個類別的驗收目標是「候選＋正式改動」的合成版 |

**相依規則**：新類別會用到既有類別新增的方法，既有類別的修改也會呼叫新類別，所以某些批次單獨無法編譯。允許一個批次從後續批次**提前帶入最小必要的類別或方法**，讓它能編譯；被帶入的部分必須同樣通過語意比對，並在 PR 列出。提前帶入的類別在原批次中標為已完成。

**每批共同驗收**：`mvn -B test` 綠（不得停用、跳過或放寬測試）；T020d 工具 `--classes <本批外層類別及內部類別>` 對 `reference/candidate-50635f7e.jar` 全部語意相同（B3-core 對合成版）；PR 列出搬入／移植／提前帶入的類別。

**第二階段收尾**（B3-core 之後）：本機對 Maven 產物跑 T020c 全套回歸（#31，`--jar` 指向 Maven 產物），各套件案例數與 `8d26c9d7` 既有收據一致，並補跑 `50635f7e` 的 negative final frame 針對性案例。結果作為新基準，接 T030。

### 6-5. 不在本批次的事

- T021–T028（技能目標、Source26 自施、PET／SUMMON 語意等缺口）：在合併後的 `src/main` 上逐項做，不再另疊候選層。
- T029 的非 JAR 部分（`litoddraw.dll`、LTicon／LTdesc）：另由 T029 處理。
