# Issue #43 / T020b — 戰鬥合併批次（設計）

## 第 3 版（T020b-2：用候選 JAR 產生批次）
- 從 `task/45` 取得 `reference/candidate-50635f7e.jar`（SHA256 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`，已核對），執行
  `python3 lito伺服器/LiTo381-Server/tools/semantic-diff/plan_batches.py --candidate <jar> --owned docs/combat-merge/batches.json --diff <task/30 的 class-diff.json> --max 60 --out docs/combat-merge/batches.json`。
- **已驗證（相依分析）**：303 個外層類別、16 個 SCC，最大 SCC 282 類；拿掉三個核心類別後最大 SCC 仍 248 類。171 個類別用到 107 個既有類別在候選中新增的 518 個成員。所以第 2 版「依相依閉包分批、每批在 src/main 單獨編譯」不可行，做法改為 §6-4 第 3 版：每批在暫存區以候選 JAR 當 classpath 編譯與比對，最後一次 B-land 落地。這也回答了審查 P1-1（跨批引用、只帶入方法的衝突）：暫存驗證下每批都可獨立編譯，整個類別以候選版為目標。
- **已驗證（T029 允許清單）**：對正式版與候選版的 `ClientSession`、`Protocol` 執行 `javap -c -p`，取正式版中引用 GmPolyMe／SelectionRecovery／SharedPresentationPackets、或名稱為 sendRoomParticipant／litoRoomParticipant／magicDollIcon／magicDollCustomIcon 的方法共 9 個；其中 2 個 sendRoomParticipant 多載與候選相同，其餘 7 個不同，寫入 `docs/combat-merge/t029-methods.txt`（JVM 描述子取自 `javap -s -p`）。
- 批次：B01（17，asm-only 新類別）、B02–B05（51／51／51／48，有原始碼的新類別）、B06（29，Battle*）、B07（23，Combat*／MaterialAction）、B08（30，其他既有）、B09（3，核心＋T029）。`docs/plans/issue-43.json` 每批 text 寫明範圍、做法、編譯／語意驗收與不適用理由。

## 第 2 版（回應 2026-11-04 14:02 審查退回）
| 審查項 | 處理 |
|---|---|
| [P1-1] 相依閉包未固定 | 改為 §6-4 的確定性程序（常數池相依圖 → SCC → 拓撲排序 → 每批 ≤60 裝箱）；同一 SCC 不拆、整個類別以候選版為目標，取消「只帶入方法」。實際批次需要候選 JAR，在 #45 入庫後以 `tools/semantic-diff/plan_batches.py` 產生（本 Issue 第 2 步，T020b-2）。§4 的 ≤30 改為 ≤60，理由寫在 §6-4。 |
| [P1-2] A／B 重疊 | §6-2：B 擁有 303 個外層類別（`batches.json` 的 `owned_by_B`），A 不得修改；73 個重疊類別在 B3 直接以候選版為目標。T020-close 依賴補上 #31 與「A 修補任務全部完成」（§6-6）。 |
| [P1-3] 逐批回歸子集與 B3-core 基準 | §6-5：逐項定義範圍、來源、語意、T029、單元測試、回歸子集（新類別批次無適用並說明理由；含既有類別批次跑 targeted＋player 2,400 案）。B3-core 改用方法層級允許清單（`--allow-methods`）對正式版比對，不需要另產合成 JAR；兩邊都改的方法列為手動合成並附 javap 差異。允許清單由 T020b-2 產生。 |
| [P2-4] 試算證據 | 試算降為「推測，待 T020d」。原始輸入與指令見下。 |
| [P2-5] 範圍 | 任務擁有者（雲端主控）調整 Issue 範圍：加入 `docs/combat-merge/batches.json`、`docs/combat-merge/t029-methods.txt`、`tools/semantic-diff/plan_batches.py`；繼承自 `task/24` 的檔案（`TASKS.md`、`docs/plans/issue-24.json`、`docs/reports/issue-24.md`、設計 §1–5）在 PR #29 合併前隨本分支存在，PR #29 合併後本分支只剩授權差異。 |

## 做了什麼
- `docs/design/T020-combat-merge.md` §6 改寫（第 2 版）。
- `docs/combat-merge/batches.json`：確定 `owned_by_B`（303），`batches` 待 T020b-2。
- `docs/plans/issue-43.json`：T020d（#44）、T020f（#45）、T020b-2、T020e、T020-close。

## 怎麼驗證
- 已驗證（審查重算）：303 個外層類別、218 新／85 既有、19 DECOMPILED。
- 推測：Maven 產物與正式可能有語意差異。雲端試算條件：repo main @ `b6dc4e6`；`LC_ALL=C.UTF-8 mvn -B -q -DskipTests -Dmaven.test.skip=true package`（OpenJDK **21.0.12**，非 17），產物 `target/LiTo381-Server.jar` SHA256 `702194c491c831b0389188f83278c47449ae82c8e24282a8257e4c30e5b6e826`；對 `reference/base-0.6.71.jar`（`e1498871…`）以每批 200 類別執行 `javap -c -p -constants`，把 `#數字` 換成 `#`、連續空白壓成一格後逐類別比較：2,538 共同類別、593 不同、162 解析失敗。輸出僅存在雲端工作階段的暫存區，未提交（依規則，實驗輸出不進 git）。JDK 版本不同、lambda／合成名稱未正規化，因此不能當作已驗證。

## 未驗證什麼
- 實際批次與每批可否單獨編譯（T020b-2，需候選 JAR）。
- A 階段實際修補量（T020d）。

## 給下一棒的提示
#45 入庫後由雲端主控執行 T020b-2，產生批次與允許清單並建 B 批次 Issue；#44 需支援 `--exclude`、`--allow-methods`。
