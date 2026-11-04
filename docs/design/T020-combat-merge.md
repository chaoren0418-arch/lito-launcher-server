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

## 6. 合併批次（T020b，Issue #43；第 4 版，回應 2026-11-04 14:30 審查）

> 本節取代 §2-3（先併 8d26）、§2-4 的回歸套件用法（改在 §6-6 統一執行）與 §4 任務表中 T020b 的「每批 ≤30 類別」規則（改為 §6-4 每批 ≤60）。§1–5 其餘內容不變。

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

### 6-4. 批次（第 3 版：相依分析結果改變做法）

`tools/semantic-diff/plan_batches.py` 對候選 JAR（`50635f7e…`，`task/45`）與正式 JAR（`e1498871…`）做常數池分析，結果寫在 `docs/combat-merge/batches.json` 的 `dependency_analysis`：303 個外層類別有 16 個 SCC，**最大的一個有 282 類**；拿掉 ClientSession／Protocol／GameHub 後仍有 248 類互相依賴。候選中有 107 個既有 class entry（62 個外層類別）新增了共 518 個欄位／方法，171 個外層類別用到這些新成員。候選 JAR 中這 303 個外層類別共有 **1,384 個 class entry**（含內部類別）。

結論：**不可能讓每批在 `src/main` 單獨編譯**，任何「依相依順序裝箱」或「提前帶入」都會退化成一整批。因此改成：

- **每批只在暫存區工作**：移植好的原始碼放 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，不動 `src/main`。
- **每批獨立驗證**：`javac --release 17 -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar:lib/* -d <tmp> <本批 staged 檔案>`。本批以外的類別由候選 JAR 提供；正式專有的 GmPolyMe／SelectionRecovery／SharedPresentationPackets 由 base 提供（候選在前）。編出的類別用 T020d 工具與候選比對。這樣**批次之間沒有順序依賴**，可以並行。
- **最後一次落地**（B-land）：把 staged 全部搬進 `src/main`，一次合入。

批次依 `class-diff.json` 分類固定分組（每批 ≤60，組內依名稱）：

| 批次 | 類別數 | 分類 | 來源與做法 |
|---|---:|---|---|
| B01 | 17 | 新類別，asm-only | CFR 反編譯候選版，標頭 `// DECOMPILED from candidate-50635f7e.jar, TODO review`，再修到能編譯且語意相同 |
| B02–B05 | 51／51／51／48 | 新類別，完整 Java（含外層 Java＋內部 ASM 的混合者） | 從 `merge-inputs/50635f7e/sources/` 複製；混合者的內部類別由外層 Java 編出 |
| B06 | 29 | 既有 `Battle*` | 三方移植：CFR 反編譯正式版與候選版取差異，套到 `src/main` 現有原始碼的複本（放 staged）；`src/main` 為 DECOMPILED 者可直接用候選反編譯版 |
| B07 | 23 | 既有 `Combat*`、`ForcedCombat`、`*MaterialAction` | 同上 |
| B08 | 30 | 其他既有類別（19 個 DECOMPILED） | 同上 |
| B09 | 3 | ClientSession／Protocol／GameHub | 同上，加 T029 保留（§6-5） |

每批清單（`batches.json` 的 `batches[]`）：
- `classes`：外層類別。
- `verify_entries`：**驗收範圍**。由候選 JAR 展開該批外層類別及其全部 `$` 內部類別（9 批合計 1,384）。批次輸出必須剛好是這些 entry（無缺漏、無多出），且全部與候選 SAME。
- `history_entries`：T020a 的歷史差異列（合計 623），只供參考，不是驗收範圍。
- `remove_entries`：正式有、候選沒有的同批 entry（目前 9 批皆 0）；B-land 時必須消失。

`--classes` 一律傳 `verify_entries` 的完整清單，不依賴工具自動展開內部類別。

### 6-5. 每批驗收（B01–B09）

| 項目 | 規則 |
|---|---|
| 範圍 | 只新增／修改 `merge-inputs/50635f7e/staged/` 下該批類別的 `.java`，與 `docs/reports/issue-<N>.md`。不動 `src/main`、`pom.xml`。 |
| 編譯 | 上述 `javac` 指令成功，指令與輸出附在報告。 |
| 語意 | `semantic-diff --left <tmp 打包的 jar> --right reference/candidate-50635f7e.jar --classes <該批 verify_entries>`：每個 entry 都 SAME，且 tmp 中沒有清單外的 entry、清單內沒有缺少的 entry。 |
| T029（只 B09） | `docs/combat-merge/t029-methods.txt` 列 8 個方法，由 `plan_batches.py` 以正規化 javap（去常數池編號、偏移、跳轉目標、例外表範圍）比對正式與候選後產生，分類記在 `batches.json` 的 `t029.methods`：<br>• **whole-formal（7）**：ClientSession `run()`、`exitRoom(Z)`、`sendRoomParticipant(J…ClientSession…)`、`sourcePoisonBody$useInventoryItemBeforeTravel`；Protocol `litoRoomParticipant`、`magicDollIcon`、`magicDollCustomIcon`。正式與候選在這些方法的差異**只有** T029 項（職業上限 6／4、opcode 123／250 與 `h(0)`、呼叫 SelectionRecovery.finish／SharedPresentationPackets.bluePotion／GmPolyMe.exitRoom），候選第二批沒有改動它們的戰鬥邏輯，所以整個方法採正式版。<br>• **formal-only-method（1）**：ClientSession `exitRoomBeforePolyMe(Z)`。正式版把原本的 `exitRoom(Z)` 本體改名為它，再讓 `exitRoom(Z)` 轉呼叫 `GmPolyMe.exitRoom`；已驗證正式 `exitRoomBeforePolyMe(Z)` 與候選 `exitRoom(Z)` 正規化後完全相同。<br>因此**沒有需要人工合成的方法**。驗收指令加 `--allow-methods docs/combat-merge/t029-methods.txt --allow-right reference/base-0.6.71.jar`：這 8 個方法必須與正式版 SAME（不在候選中的 `exitRoomBeforePolyMe` 只和正式比）；其他方法必須與候選 SAME。 |
| 單元測試 | 不適用（未動 `src/main`）；在 B-land 統一處理。 |
| 回歸 | 不適用，理由：暫存區不會被執行；與候選語意相同即代表行為等同候選。行為回歸在 B-land 後統一跑（§6-6）。 |

### 6-6. 落地（B-land）與收尾（T020-close）

- **B-land**（code，cursor-cloud）：依賴 B01–B09 全部完成。把 staged 搬進 `src/main/java/local/lito381/server/`（覆蓋 85 個既有類別、新增 218 個）；`mvn -B test` 綠。舊測試若因戰鬥行為改變而失敗，比照 T010 逐案判斷「測試過時／程式缺陷」並在報告列出，不得停用或跳過；`semantic-diff` 對 Maven 產物 `--classes <9 批 verify_entries 合併的 1,384 個 entry>` vs 候選全部 SAME（T029 依允許清單），並確認 `remove_entries` 不存在。
- **T020-close**（test，codex-local）：依賴 B-land、#31（T020c）、**A 階段全部修補任務合併**（T020e 產生的清單全部 done）。先跑 T020d 總比對：Maven 產物中 1,384 個 B entry 對候選（T029 依允許清單對正式）、其餘 entry 對正式（`--exclude` 這 1,384 個 entry），兩者皆 0 差異；再對 Maven 產物跑 #31 全套（2,400／5,164／20＋18／24）加 `50635f7e` negative final frame 針對性案例。結果作為新基準，接 T030。

### 6-7. 不在本批次的事

- T021–T028：在合併後的 `src/main` 上逐項做，不再另疊候選層。
- T029 的非 JAR 部分（`litoddraw.dll`、LTicon／LTdesc）：另由 T029 處理。

## 7. A 階段：`src/main` → 正式 0.6.71（T020e，Issue #59）

依據：#44 T020d 的 `docs/server-semantic-gap.md`（CI run 41852194064，Temurin 17.0.21）。Maven 產物 vs `reference/base-0.6.71.jar`：SAME 2,064、DIFF 485、ONLY_LEFT 1,380、ONLY_RIGHT 5。

### 7-1. 範圍（已驗證，`plan_a_batches.py` 產生）

| 項目 | 數量 |
|---|---:|
| DIFF＋ONLY_RIGHT 中屬 `owned_by_B` 的 entry（交給 B，A 不處理） | 203＋2 |
| A 的目標 entry | **285** |
| A 的外層類別 | **224**（其中 171 個在 `src/main` 是 `// DECOMPILED`） |
| A 的驗收 entry（正式 JAR 中這 224 類的外層＋全部 `$`） | **692** |
| 第一處差異種類 | method 163、class 屬性 102、field 17、ONLY_RIGHT 3 |

ONLY_LEFT 1,380 是 `src/main` 才有的類別（`Modern*` 等），T007 已列為刻意排除，不在 A 範圍。

### 7-2. 相依分析

以正式 JAR 常數池分析 224 個 A 類別：127 個 SCC，**最大 89 類**。和 B 相同，無法逐批在 `src/main` 單獨編譯。

### 7-3. 做法：暫存區＋正式 JAR

- 每批只把修好的原始碼放進 `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`，不動 `src/main`。
- 驗證：`javac --release 17 -encoding UTF-8 -cp reference/base-0.6.71.jar:lib/* -d <tmp> <本批 staged 檔>`；`semantic-diff --left <tmp 打包> --right reference/base-0.6.71.jar --classes <本批 verify_entries>`，每個 entry 都 SAME，且 tmp 中沒有清單外 entry、清單內沒有缺少的 entry。
- 來源：
  - `src/main` 為 `// DECOMPILED` 的類別：用 CFR（或 Vineflower）重新反編譯正式 JAR 的版本，修到能編譯且 SAME，保留 `// DECOMPILED from base-0.6.71.jar, TODO review` 標頭。
  - `src/main` 為原始碼的類別：以 `src/main` 現檔為底，對照正式版反編譯結果修正差異（保留原始碼的可讀性與註解）。
  - `sources` 為 null 的類別（AccountRepository、MemoryAccountRepository、SourceEquipmentPhysicalStats 由其他檔案一起定義；NativeDeVisibilitySwitch 在 `src/main` 缺）：依 Maven class 的 `SourceFile` 找到定義檔；缺檔者新增。
- 與 B 的關係：A、B 的類別不重疊（`owned_by_B` 排除在外）。候選 `50635f7e` 對 A 類別與正式相同（候選只改了 `owned_by_B`），所以兩邊落地後互相相容；兩邊可並行。

### 7-4. 批次（`docs/combat-merge/a-batches.json`）

| 批次 | 外層類別 | 驗收 entry | 分類 |
|---|---:|---:|---|
| A01 | 49 | 185 | NativeDe*（第 1／3） |
| A02 | 49 | 104 | NativeDe*（第 2／3） |
| A03 | 47 | 88 | NativeDe*（第 3／3，含缺檔的 NativeDeVisibilitySwitch） |
| A04 | 25 | 86 | Battle*／Combat* |
| A05 | 27 | 160 | 其他（第 1／2，含 InventoryCatalog 的 T029 項 NativeArmorStatus） |
| A06 | 27 | 69 | 其他（第 2／2） |

每批的 `diff_entries` 列出 T020d 回報的第一處差異，`sources` 列出 `src/main` 對應檔與是否為 DECOMPILED。

### 7-5. 每批驗收（A01–A06）

| 項目 | 規則 |
|---|---|
| 範圍 | 只新增／修改 `merge-inputs/formal-0.6.71/staged/` 下本批類別的 `.java` 與報告；不動 `src/main`、`pom.xml`。 |
| 編譯／語意 | 依 §7-3，指令與輸出附在報告。 |
| 單元測試／回歸 | 不適用（未動 src/main，暫存區不執行）；在 A-land 統一處理。 |

### 7-6. 落地（A-land）

依賴 A01–A06 全部完成。把 staged 搬進 `src/main`（取代 224 個外層類別的定義，必要時拆／合檔以符合原檔結構）；`mvn -B test` 綠。既有測試是對著 `src/main` 現行行為寫的，改成正式版行為後若失敗，比照 T010 逐案判斷「測試過時（以正式 0.6.71 為準）／程式缺陷」並列出，不得停用或跳過；`semantic-diff` 對 Maven 產物 `--classes <A 的 692 個 verify_entries>` vs 正式全部 SAME。A-land 與 B-land 誰先都可以；後落地的一方在合入前要 rebase 並重跑兩邊的語意比對。T020-close（§6-6）依賴 A-land。
