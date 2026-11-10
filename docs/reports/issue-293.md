# Issue #293 — T092：客戶端協定與資源需求（lito脫殼資源＋0-客戶端，唯讀）

## 做了什麼
- 讀了 `lito脫殼資源/閱讀入口.md` 和相關契約：`S0C-status-owner`、`S4D-mana-point`、`status-family-50-51-52-56`、`S1E-*`、`skill-initialization/*`、`skill-pane-*`、`native-icon-draw`、`CD1/CD2/D5/CDD-*`、`Native-*`、`server-packet-delta.json`。
- 讀了 `lito伺服器/LiTo381-Server/SERVER-PROTOCOL-COVERAGE.json`（C 182／S 183 個 opcode 的狀態），以及 `Protocol.java`、`ClientSession.java` 的分派（1551–1713 行）和 parser／writer。用唯讀腳本把 305 個 parser／writer 方法的行號、欄位序列和註解中的客戶端位址抽成表（`scratch/t293/protocol-methods.json`）。
- 用 PakViewer CLI（`-cli list`，只列清單、不解包）列出 `0-客戶端` 全部 24 個 .idx（303,038 筆成員），寫到 `scratch/t293/paklist/`。另匯出 `LTtext` 的 `LTlist.spr` 到 scratch，供別名解析使用。
- 用唯讀腳本 `scratch/t293/resource_check.py` 拿伺服器引用的 sprite、變身、道具圖示／地面圖、技能特效、技能圖示、說明表、面板框架編號去比對 pak 清單，並解析 `TW13081901.txt`／`LTlist.spr` 的 `#id n=base` 別名。
- 產出 `docs/reference/client-protocol-needs.md`：角色、道具、戰鬥、技能四個領域，每個領域分 C→S、S→C 兩張封包表（opcode、名稱、欄位、狀態、伺服器行號、客戶端證據、等級），加上協定風險與資源存在性表。

## 怎麼驗證
- opcode 名稱和狀態逐一取自 `SERVER-PROTOCOL-COVERAGE.json`。handler 行號用 Select-String／Grep 實際對過（例：`CS:1577 C_USE_SPELL`、`P:8933 spellSelector`、`P:12741 appendInventoryRecord`、`P:9501 physicalAttack`、`CS:20121 sendBuyList`、`CS:14571 S_LITO_POINT`）。
- 資源存在性用 PakViewer 清單比對。第一次比對時，沒有自己檔案的 id 很多（3064 個），查證後發現是 sprite 別名（例：`#8859 56=152`）。加入別名解析後重跑，再把剩下的逐一分類：LiTo 30000+ 命名空間、TW 有宣告但無檔、道具圖示 .tbt 誤報。
- LTicon 缺口用清單重新算過：226 個 .lbt，id 0..232，缺 173..179，與 `閱讀入口.md` 記載一致。

## 已驗證／推測／待驗證
- **已驗證**
  - 四個領域的 C／S 封包都列在報告中，`COV` 狀態與伺服器行號都對過。客戶端會送的 C 封包，伺服器都有 handler（`COV.unhandledNativeReachableClientPackets = 0`）。
  - 伺服器實際引用的資源全部在 pak 裡找得到（含別名解析）：變身 104／104、道具 invgfx 56／56、grdgfx 35／35、Java 引用的 gfx 182／182、技能共用圖示 149／149、技能面板 933／934／935、說明表 4 個、選角背景 6314／7482。
  - 不完整的只有兩項，伺服器都沒有引用：`LTicon` 缺 173..179；`LTlist.spr #266` 沒有 .spr 檔。
  - 5 個 Sprite pak（01/04/05/07/10）的大小，與 2026-10-05 `shared-icon-resources.json` 的記錄不同；舊的 row／offset 可能已經失效。
  - `S_PACKET_BOX` 和 `S_LITO_CLAN` 在 `Protocol.java` 都定義成 0xFA（501、1547 行）。
- **推測**
  - R1：`sendNativeMagicDollIcon`（CS:21959）送出的 0xFA，會被 3.81C 客戶端當成 S_LITO_CLAN 解析。伺服器其他地方（CS:86378）已經因為同一原因停送圖示。
  - 職業 ↔ 基本 gfx 對應採用 L1 標準編號。
  - 等級 B 的欄位格式：只來自伺服器實作和註解位址，本輪沒有重新反組譯客戶端。
- **待驗證**（需要實機，交給 Codex／使用者）
  - R1 魔法娃娃召喚時，血盟 UI 會不會錯亂。
  - TW 宣告表有宣告但沒有 .spr 的 342 個 id，伺服器會不會在動態路徑送出。
  - S_SOUND_EFFECT 的音效 id 與 `0-客戶端\Sound\` 散檔的對應。
  - 選角肖像 PNG bank、79／80 號資源的綁定。
  - 技能面板 GUI parity。

## 未做／限制
- `launcher\TW13081901.pak` 沒有 .idx，PakViewer 無法列出，沒有查。
- 沒有用 IDA 重新反組譯 receiver。等級 A 只引用 `lito脫殼資源` 既有的契約。
- 沒有連 DB，所以道具／技能的完整編號只比對了 repo 內的表（`rental-item-definitions-380.tsv`、`polymorph-native-380.csv`、`spr-action-tw13081901.csv`、Java 常數），沒有比對 `380_db` 全表。

## 產出路徑
- `docs/reference/client-protocol-needs.md`
- `docs/reports/issue-293.md`
- 本機中間檔（不進 git）：`scratch/t293/paklist/*.txt`、`scratch/t293/protocol-methods.json`、`scratch/t293/resource-check.json`、`scratch/t293/extract_protocol.py`、`scratch/t293/resource_check.py`

## 給下一棒的提示
- **code**：處理 R1。`sendNativeMagicDollIcon` 在 LiTo 模式改成不送 0xFA，比照 Counter Magic／Absolute Barrier 的作法，並補一個單元測試，斷言不會送出 opcode 0xFA 的 PacketBox。
- **test（Codex 實機）**：召喚魔法娃娃時觀察血盟 UI；跑過大廳技能窗（S_ADD_SPELL）、背包圖示、商店列表、戰鬥特效，確認畫面與本表一致。
- **resource**：如果要重新抽技能圖示，以 PakViewer 現況重抽 Sprite01/04/05/07/10，不要沿用 `shared-icon-resources.json` 的 offset。
- 若要把等級 B 升為 A：可以依本表的 `sub_XXXXXX` 位址，用 IDA 逐一補 raw-byte 契約。建議優先順序：S_ADD_INVENTORY sub_4A4D90、S_ATTACK sub_4A5ED0、C_USE_SPELL 各個 producer、C_LITO_BUY_SELL sub_4D9A20。
- `docs/reference/server1-inventory.md`（#291）是 1-伺服器 的對照，可以和本表交叉使用。
