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

## 6. 合併批次（T020b，Issue #43；第 2 版，回應 2026-11-04 14:02 審查）

依據：#30 T020a 的 `docs/combat-merge/class-diff.json`（分支 `task/30` @ `31b5233`，主控以 review:pass 結案、豁免 1 筆歷史來源）。

### 6-1. T020a 改寫了前提

| 比較 | 結果 | 狀態 | 含意 |
|---|---|---|---|
| `8d26c9d7` vs 正式 `e1498871` | 4 CHANGED＋4 ONLY_IN_FORMAL | 已驗證（T020a，審查重算） | 正式 0.6.71 已包含第一批；差的 8 筆都是正式版後加的（T029 保留清單）。§2-3「先併 8d26」不必做。 |
| `50635f7e` vs `8d26c9d7` | 623 entries → **303 個外層類別**（新 218：純 Java 156、Java＋內部 ASM 45、asm-only 17；既有改動 85：其中 19 個在 `src/main` 為 DECOMPILED） | 已驗證（T020a，審查重算） | 這是要併入的第二批。 |
| Maven 產物 vs 正式 | class bytes 只有 1,008 相同 | 已驗證（T020a） | bytes 不同不代表語意不同（除錯資訊、編譯器版本都會影響）。 |
| 同上，語意層級 | 雲端試算：javap 去常數池編號／空白後 2,538 共同類別中 593 筆不同，其中 162 筆解析失敗 | **推測，待 T020d 確認** | 試算用 JDK 21 編譯（正式／CI 為 17）、未正規化 lambda／合成名稱，雜訊未排除。只能說「可能有語意缺口」。 |

### 6-2. 兩階段與類別歸屬

- **B 階段（併入候選）擁有 303 個外層類別**（清單見 `docs/combat-merge/batches.json` 的 `owned_by_B`）。最終目標：與 `reference/candidate-50635f7e.jar` 語意相同；例外是 T029 保留方法（§6-5）。
- **A 階段（`src/main`→正式 0.6.71）擁有其餘全部類別**。最終目標：與 `reference/base-0.6.71.jar` 語意相同。T020e 產生的 A 修補任務**不得修改** `owned_by_B` 的任何檔案；T020d 工具在 A 任務驗收時以 `--exclude owned_by_B` 執行。
- B3 的 85 個既有類別中，有 73 個在 T020a 的 Maven vs 正式 bytes 差異裡。這些類別歸 B：B3 批次直接以候選版為目標，**不需要**先校正到正式版；若 `src/main` 現版與正式版有語意差異，該差異在 B3 批次內一併消除（因為候選 = 正式 + 戰鬥修改，正式缺口會一起補上）。
- 因為歸屬不重疊，A 與 B 可並行；兩邊都動不到對方的檔案。整體最終目標＝「B 類別等於候選（含 T029 例外）＋其他類別等於正式」。

### 6-3. 共用前置

| ID | Issue | 階段 | 執行 | 內容 |
|---|---|---|---|---|
| T020d | #44 | code | cursor-cloud | ASM 語意比對工具（含 `--classes`、`--exclude`、方法層級 `--allow-methods`），CI 非阻斷 job 產出 `docs/server-semantic-gap.md`。 |
| T020f | #45 | resource | codex-local | `reference/candidate-50635f7e.jar` 與 380 筆凍結 Java（`merge-inputs/50635f7e/`）入庫。 |
| T020b-2 | #43（本 Issue 第 2 步） | design | Claude | #45 完成後，以 §6-4 的確定性程序從候選 JAR 產生固定批次，寫入 `docs/combat-merge/batches.json` 與 `docs/plans/issue-43.json`。 |

### 6-4. 批次產生程序（固定、可重現）

輸入：`reference/candidate-50635f7e.jar`（SHA `50635f7e…`）、`class-diff.json` 的 303 個外層類別、腳本 `tools/semantic-diff/plan_batches.py`（T020b-2 提交）。

1. **相依圖**：對 303 個外層類別（含其 `$` 內部類別），讀候選 JAR 的常數池 `CONSTANT_Class`／`Methodref`／`Fieldref`／`InterfaceMethodref`，只保留指向另外 302 個類別的邊。
2. **強連通分量（SCC）**：Tarjan 演算法。同一 SCC 必須在同一批，不得拆分。
3. **拓撲排序**：依「被依賴者在前」排序 SCC；同層以類別名排序以保證可重現。
4. **裝箱**：依拓撲順序把 SCC 依序放入批次，每批上限 **60 個外層類別**（取代 §4 的 ≤30；理由：B1 的新類別多半是小型單一職責類別，60 個仍可人工審查）；單一 SCC 超過 60 時自成一批並在 PR 標註。B3-core（ClientSession／Protocol／GameHub）不論所在 SCC 都放在最後一批。
5. **結果**：每批是「它依賴的類別全部在前面批次或同一批」的閉包，所以**每批可單獨編譯**，不需要提前帶入。若某批仍需要尚未併入的既有類別新增的方法（候選中既有類別新增的 API），該既有類別也在相依圖中，會被排到同批或前批，不會出現「只帶入方法」的情況；**整個類別一律以最終候選版為目標**。

### 6-5. 每批驗收

| 項目 | 規則 |
|---|---|
| 範圍 | 只動該批 `batches.json` 列出的外層類別檔案（`src/main/java/local/lito381/server/`），及為它們新增／修改的單元測試。 |
| 來源 | 純 Java、混合：從 `merge-inputs/50635f7e/sources/` 搬入（混合類別的內部類別由外層 Java 編出，與候選比對驗證）。asm-only 新類別：CFR 反編譯候選版，標頭 `// DECOMPILED from candidate-50635f7e.jar, TODO review`。asm-only 既有類別：CFR 反編譯候選版與正式版，取差異套到 `src/main`（`src/main` 為 DECOMPILED 者可直接以候選反編譯版取代）。 |
| 語意 | `semantic-diff --left target/LiTo381-Server.jar --right reference/candidate-50635f7e.jar --classes <該批全部外層＋內部類別>` 全部 SAME。 |
| T029 保留（只在含 ClientSession／Protocol／GameHub 的批次） | 同一指令加 `--allow-methods docs/combat-merge/t029-methods.txt`：允許清單內的方法改為和 `reference/base-0.6.71.jar` 比對且必須 SAME；清單外方法仍須等於候選。允許清單由 T020b-2 從 T020a `formal_preservation` 的 javap 證據產生並提交：ClientSession（呼叫 GmPolyMe.exitRoom、SelectionRecovery.finish、SharedPresentationPackets.bluePotion 的方法、七職業 sendRoomParticipant）、Protocol（litoRoomParticipant 職業 0..6、magicDollIcon／magicDollCustomIcon opcode 123＋h(0)）。若某方法兩邊都改（候選改了戰鬥邏輯、正式改了 T029 項），該方法列為「手動合成」，驗收改為：與正式版的差異只在戰鬥部分、與候選版的差異只在 T029 部分，PR 附兩份 javap 差異供審查。 |
| 單元測試 | `mvn -B test` 綠，不得停用、跳過或放寬。 |
| 回歸子集 | **新類別批次（不含任何既有類別）：無適用的戰鬥回歸**。理由：新類別在被既有類別呼叫前不會執行，行為回歸要等呼叫端併入才有意義；以語意比對＋單元測試為準。**含既有類別的批次**：本機跑 T020c（#31）的 `targeted`＋`player-cancellation`（2,400 案）對 Maven 產物，須與 `8d26c9d7` 既有收據的案例數與結果一致；批次內若沒有既有類別被戰鬥入口呼叫，報告說明並改跑 `affected_18`。前提：#31 已完成，否則該批停在等待。 |

### 6-6. 收尾閘門（T020-close）

依賴：最後一個 B 批次、#31（T020c）完成、**A 階段所有修補任務合併**（T020e 產生的任務清單全部 done，不是只有 T020e 設計完成）、T020d 對 Maven 產物 vs 「B 類別取候選＋其他取正式」的總比對 0 差異（T029 允許清單除外）。執行：本機對 Maven 產物跑 #31 全套（2,400／5,164／20＋18／24），加 `50635f7e` negative final frame 針對性案例；結果寫成新基準，接 T030。

### 6-7. 不在本批次的事

- T021–T028：在合併後的 `src/main` 上逐項做，不再另疊候選層。
- T029 的非 JAR 部分（`litoddraw.dll`、LTicon／LTdesc）：另由 T029 處理。
