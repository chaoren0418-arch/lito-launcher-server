# 客戶端協定與資源需求（角色／道具／戰鬥／技能）

> Issue #293（T092，M1 能力盤點第 3 項）。基準客戶端：`D:\lineage開發\0-客戶端`（`LiTo.bin` SHA CE6EE68A…）。
> 全程唯讀：未修改 `0-客戶端`、`LiTo.bin`、shared BIN；未複製 pak／map／sprite 進 repo；未啟動遊戲或伺服器。
> 產生日期 2026-11-11；伺服器程式行號以 `origin/main`（fe38ce40）為準。

## 0. 讀法

### 0.1 縮寫
| 縮寫 | 指向 |
|---|---|
| `P:n` | `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/Protocol.java` 第 n 行（parser／writer 方法起點） |
| `CS:n` | 同目錄 `ClientSession.java` 第 n 行（`case Protocol.C_xxx ->` 分派在 1551–1713 行） |
| `COV` | `lito伺服器/LiTo381-Server/SERVER-PROTOCOL-COVERAGE.json` 的 `clientToServer`／`serverToClient` 條目（`status` + `nativeProducer`／`nativeDispatch`） |
| `脫殼:檔名` | `lito脫殼資源/` 下的契約檔 |
| `sub_XXXXXX` | `LiTo.bin` 靜態分析位址（多數取自 Protocol.java 註解，少數取自 `lito脫殼資源` 契約） |

### 0.2 欄位格式
`c`=1 byte、`h`=2 byte LE、`d`=4 byte LE、`s`=CP950 NUL 結尾字串、`S`=固定長字串區、`raw`=長度前綴的原始 bytes。格式**含開頭的 opcode byte**（例如 `cdhh` = opcode + d + h + h）。

### 0.3 狀態欄
- **已處理**：`COV` 標 `handled`（C 封包）或 `emitted`（S 封包），且本表列出的 handler／writer 行號存在。「已處理」只代表有解析／送出程式，**不代表**玩法已實測（見 `SERVER-PROTOCOL-COVERAGE.zh-TW.md`「後續驗收規則」）。
- **客戶端不送**：`COV` 標 `native-producer-absent`（`LiTo.bin` 找不到這個 C 封包的 producer），伺服器不需要處理。
- **客戶端不收**：`COV` 標 `native-dispatch-absent`（`LiTo.bin` 的接收 dispatcher 沒有這個 opcode），伺服器送了也沒有作用。
- **只診斷**：`COV` 標 `diagnostic-only`（有 receiver，伺服器只在診斷路徑送）。

### 0.4 證據等級
- **A（已驗證）**：`lito脫殼資源` 有獨立的原始 bytes／指令斷言契約，與伺服器格式一致。
- **B（已驗證：伺服器端）**：伺服器 parser／writer 寫死這個格式，註解引用了客戶端的 receiver／producer 位址；本輪**沒有**另外反組譯客戶端重新確認。
- **C（推測）**：只有 opcode 名稱和 `COV` 狀態，欄位語意靠同系列 3.80 的慣例推定。

---

## 1. 角色

### 1.1 客戶端 → 伺服器
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0xC8 | C_LITO_LIN_CHARACTER_INFO | `cs`（來源伺服器名，取自 72-byte S_SERVER_LIST 記錄、0x40 byte 名稱欄） | 已處理 | CS:1667 → CS:8015；P:13981 | sub_48FAA0（LOGIN 快取分支）、sub_4AC4C0（S_F5 重送分支） | B |
| 0xC9 | C_LITO_SELECT_CHAR | `cs`（角色名） | 已處理 | CS:1668 → CS:8250；P:14407 | sub_4928C0、sub_4A7550；脫殼:`S1E-receive-selection-boundary.json`（owner 建立／reset 後才送 C9）、`Modern-character-ui-reachability-contract.json`（0..8 列可達） | A |
| 0x70 | C_CREATE_CUSTOM_CHARACTER | `cscccccccccc`（名稱＋職業／性別／能力值 bytes） | 已處理 | CS:1650 → CS:15326；P:11607 | sub_492580 | B |
| 0x43 | C_REQUEST_ROLL | `cc`（職業） | 已處理 | CS:1624；P:11493 | sub_4949C0 | B |
| 0x07 | C_DELETE_CHARACTER | 名稱字串 | 已處理 | CS:1557 → CS:15429 | COV present | C |
| 0x05 | C_ENTER_WORLD | — | 已處理（伺服器保留），**客戶端無 producer**（`nativeProducer: not_found`） | CS:1555 → CS:15391 | COV | C |
| 0x04 | C_CREATE_CHARACTER | — | 客戶端不送（native-producer-absent） | — | COV | B |
| 0x0A | C_MOVE | `chhc`（目前 x、y、八方向 heading；目的地由 heading 推算） | 已處理 | CS:1568 → CS:26843；P:8891 | P:8891 註解 | B |
| 0x09 | C_CHANGE_DIRECTION | `cc` | 已處理 | CS:1567 → CS:32096 | COV present | C |
| 0x2B | C_TITLE | `css` | 已處理 | CS:1606；P:11465 | sub_4044A0 | B |
| 0x10 | C_RESTART | — | 已處理 | CS:1562 | COV present | C |
| 0x0F | C_QUIT | `c` | 已處理 | CS:1561 | 脫殼:`D5-CF-receive-contract.json`（CF primary default → C_QUIT） | A |
| 0xD8 | C_LITO_CHANGE_OPTION | — | 已處理，**客戶端無 producer**（not_found） | CS:1684 | COV | C |

### 1.2 伺服器 → 客戶端
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0x03 | S_NUM_CHARACTER | `cc`（角色數） | 已處理 | P:11705 | sub_4A7450 | B |
| 0xCB | S_LITO_CHARACTER_INFO | `c`＋`sscchhhcccccccccddddddddddcdddddc`（名稱、血盟、職業、性別、HP/MP、能力值、場數／勝／敗／斷線等） | 已處理 | P:11727 | sub_4A7550、sub_4928C0；脫殼:`CD2-selected-record-source-contract.json`（record 62FAA0 + index·180 + 3E）、`CD2-directory-index-storage-contract.json` | A |
| 0xC8 | S_LITO_SELCHAR_RESULT | `cc` | 已處理 | P:14449 | sub_4A76D0 | B |
| 0x05 | S_NEW_CHAR_INFO | `c`＋角色記錄 | 已處理 | P:11663 | sub_4A74F0 | B |
| 0x04 | S_CHARACTER_INFO | `c` | 只診斷（`verifiedNegativeContracts[0]`：只有 ltcharsvr／lteventcsvr 特殊模式才用，一般選角不得送） | P:11695 | sub_4A74F0 | B |
| 0x48 | S_ROLL_RESULT | 能力值骰點結果 | 已處理 | P:11581 | sub_4AEA80 | B |
| 0x0C | S_STATUS | 36 參數 bytes／22 欄位（等級、EXP、能力值、HP/MP、AC、時間、負重、陣營等） | 已處理 | P:10711 | sub_4A36F0；脫殼:`S0C-status-owner-contract.json`（21 段 raw、70 項指令斷言，與伺服器 characterStatus 一致） | A |
| 0x0D | S_HIT_POINT | `chh`（目前／最大 HP） | 已處理 | CS:12198 等多處直接寫入 | COV present | B |
| 0x4D | S_MANA_POINT | `chh`（目前 MP i16→i32、最大 MP WORD） | 已處理 | P:10691 | sub_4AEFB0；脫殼:`S4D-mana-point-contract.json` | A |
| 0x50 | S_ABILITY_SCORES | `cdddddccccc`（STR/INT/WIS/DEX/CON/CHA＋負重 byte 0..29） | 已處理 | P:11081 | sub_4AF0A0；脫殼:`status-family-50-51-52-56-contract.json` | A |
| 0x51 | S_EXP | `ccd`（等級、總 EXP） | 已處理 | P:11171 | sub_4AF0E0；同上契約 | A |
| 0x52 | S_AC | `cdddccc`（AC＋火水風地抗性） | 已處理 | P:11197 | sub_4AF100；同上契約 | A |
| 0x0B | S_PUT_OBJECT | `chhdhccccdhsscdssccccshc`（x、y、objectId、gfx、動作、heading、亮度、速度、經驗、名稱、稱號、狀態、血盟、主人、HP 比例…） | 已處理 | P:14895（角色）、P:15989（地面物品同格式） | sub_4A3990 | B |
| 0x12 | S_MOVE_OBJECT | `cdhhc`（objectId、來源 x、y、heading） | 已處理 | CS:29995 等 | COV present | B |
| 0x1C | S_CHANGE_DIRECTION | `cdc` | 已處理 | P:11389 | COV present | B |
| 0x27 | S_POLYMORPH | `cdhc`（objectId、gfx、動作） | 已處理 | P:12251 | COV present | B |
| 0x2F | S_TITLE | `cds` | 已處理 | P:11519 | sub_4A73F0 | B |
| 0x59 | S_CHANGE_ALIGNMENT | `cdh` | 已處理 | P:8315 | sub_4AF470 | B |
| 0x15 | S_REMOVE_OBJECT | `cd` | 已處理 | P:12307 | COV present | B |
| 0xCD | S_LITO_ROOM_USER | `cdscccdddcscsdccdc`（房內角色列：名稱、職業、勝敗統計四元組…） | 已處理 | P:2419 | sub_459AE0、sub_4928C0、sub_4934F0 | B |
| 0x75 | S_CHANGE_LEVEL | — | 客戶端不收（native-dispatch-absent） | — | COV | B |

---

## 2. 道具

### 2.1 客戶端 → 伺服器
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0x1C | C_USE_ITEM | 共同 `cd(objectId)`，後面依 useType 不同：預設 `cds`（P:4529，sub_445BD0）、照明 `cdd`（P:4629，sub_445C10）、回家 `cdhhh`（P:4659，sub_407570）、鑑定 `cdd`（P:4693，sub_407760）、選擇 `cdd`（P:4717，sub_407890）、指定地點 `cddhh`（P:4745，sub_406D80）、船票 `cdc`（P:4775，sub_447B50）、信紙 `cdhsSS`（P:4561，sub_479680） | 已處理 | CS:1580 → CS:23082（`GmTravel.useItem` → useInventoryItemBeforeTravel） | 如左 | B |
| 0x0C | C_DROP | `chhdd`（x、y、物件 id、數量） | 已處理 | CS:1574 → CS:21430；P:4483 | sub_443FF0 | B |
| 0x0B | C_GET | `chhddc`（x 是 `(worldX-0x8000)>>1+0x8000`、y、物件 id、數量、輔助 byte） | 已處理 | CS:1573 → CS:21273；P:4367 | sub_4062E0、sub_406620、sub_4066F0 | B |
| 0x11 | C_GIVE | `cdhhdd` | 已處理 | CS:1563 → CS:21998；P:4505 | sub_4440F0、sub_444B40 | B |
| 0x76 | C_DESTROY_ITEM | 物件 id | 已處理 | CS:1654 → CS:22447 | COV present | C |
| 0xDA | C_LITO_BUY_LIST | `c` | 已處理 | CS:1686 → CS:20121 | COV present | B |
| 0xDE | C_LITO_SELL_LIST | `c` | 已處理 | CS:1689 → CS:20138 | COV present | B |
| 0xDB | C_LITO_BUY_SELL | `c c(操作) h(列數) + 列數×[d(item_id) d(數量)]` ＋ 8 byte 不透明尾；phase 4（個人商店）在操作 byte 前多一個 context `d` | 已處理 | CS:1687 → CS:20154；P:13481 | sub_4D9A20（producer）、sub_4D9930（列序列化） | B |
| 0x28 | C_BUY_SELL | `cdcccdd`（傳統 NPC 商店） | 已處理 | CS:1591；P:12797 | sub_4DA270、sub_4EBCB0 | B |
| 0x63 | C_CHECK_INVENTORY | — | 已處理 | CS:1652 | COV present | C |
| 0x44／0x45 | C_FIX／C_FIXABLE_ITEM | `cdd`／`c` | 已處理 | CS:1625–1626；P:7527、P:7511 | sub_4D9B80、sub_407C70 | B |
| 0x7E | C_WAREHOUSE_CONTROL | `ccdd` | 已處理 | CS:1641；P:8029 | sub_4EAD50 | B |
| 0x7F | C_NPC_ITEM_CONTROL | `ccdc` | 已處理 | CS:1642；P:8073 | sub_445C10、sub_4B6B40 | B |
| 0x80／0x81 | C_PERSONAL_SHOP／C_QUERY_PERSONAL_SHOP | `cchh`／`ccd` | 已處理 | CS:1660–1661；P:13211、P:13321 | sub_42BC20、sub_407C70 | B |
| 0x34／0x38／0x36／0x37 | 交易 ASK／ADD／CANCEL／ACCEPT_XCHG | `cd`／`cdd`／`c`／`c` | 已處理 | CS:1593–1596；P:8493、P:8519 | COV present | B |

### 2.2 伺服器 → 客戶端
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0x16 | S_ADD_INVENTORY | `c d(objectId) c(useType) c(次數) h(icon) c(祝福 0..3) d(數量) c(已鑑定) s(名稱) c(len) raw(detail)` | 已處理 | P:12039、記錄格式 P:12741 | sub_4A4D90 | B |
| 0x7C | S_ADD_INVENTORY_BATCH | `cc(數量)` + 數量×S_16 記錄（選角完成訊號兼大廳背包） | 已處理 | P:12053；CS:8896 附近 `sendInventoryBootstrap` | sub_4B2650 | B |
| 0x17 | S_REMOVE_INVENTORY | `cd` | 已處理 | P:12071 | COV present | B |
| 0x18 | S_CHANGE_ITEM_DESC | `cds` | 已處理 | P:12081 | COV present | B |
| 0x6F | S_CHANGE_ITEM_DESC_EX | `cdsdc` + raw(detail) | 已處理 | P:12427 | sub_4A58B0 | B |
| 0x19 | S_CHANGE_ITEM_USE | `cdc`（剩餘次數） | 已處理 | P:12093 | COV present | B |
| 0x1A | S_CHANGE_COUNT | `cdd` | 已處理 | P:12317 | COV present | B |
| 0x0E | S_CHANGE_ITEM_BLESS | `cdc` | 已處理 | P:12343 | sub_4A59F0 | B |
| 0x1D | S_WIELD | `cdc`（裝備中旗標） | 已處理 | P:12563 | sub_4A5A30 | B |
| 0x3F | S_IDENTIFY_CODE | `chhc` + s×參數數 | 已處理 | P:12371 | sub_4A4CB0 | B |
| 0xDF | S_LITO_BUY_LIST | `ch(列數)` + 列數×[`d(catalogId) h(icon) d(價格) s(名稱) c(len) raw(detail)`] | 已處理 | CS:20121–20135 | COV present；`LiToItemDetails.java:62` 也改寫這個 opcode | B |
| 0xE0 | S_LITO_SELL_LIST | `ch(列數)` + 列數×[`d(objectId) d(價格)`] | 已處理 | CS:20138–20152 | COV present | B |
| 0x2B／0x2C | S_BUY_LIST／S_SELL_LIST | `cdhhdsc`／`cdhd` | 已處理 | P:12885、P:12943 | COV present | B |
| 0x0B | S_PUT_OBJECT（地面物品） | 同 §1.2 S_PUT_OBJECT；ground gfx 的 sprite class 9 才能被 C_GET 撿起 | 已處理 | P:15989 | sub_4A3990、sub_4E89F0 | B |
| 0x76 | S_CHANGE_ITEM_TYPE | — | 客戶端不收 | — | COV | B |
| 0xFA | S_PACKET_BOX（魔法娃娃圖示，`cch`／`cchhhh`） | 見 §5 風險 R1 | **opcode 衝突**：3.81C 的 0xFA 是 S_LITO_CLAN | P:15055、P:15071；CS:21959 `sendNativeMagicDollIcon` | S_LITO_CLAN receiver sub_4AC1C0（P:17411） | B |

---

## 3. 戰鬥

### 3.1 客戶端 → 伺服器
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0x17 | C_ATTACK | `cdhh`（目標 id、目標 x、y） | 已處理 | CS:1578 → CS:74619；P:8849 | COV present | B |
| 0x18 | C_FAR_ATTACK | `cdhh` | 已處理 | CS:1579；P:8849 | COV present | B |
| 0x7A | C_ALT_ATTACK | `cdhh` | 已處理 | CS:1659；P:8849 | COV present | B |
| 0x1F | C_ACTION | 動作 id | 已處理 | CS:1582 → CS:32130 | COV present | C |
| 0x0E | C_OPEN | `chhd`（門） | 已處理 | CS:1576；P:4423 | sub_4062E0 | B |
| 0xD2 | C_LITO_READY_GAME | `cc`（state 0/1/2） | 已處理 | CS:1677；P:16643 | 脫殼:`CD2-ready-outbound-contract.json`（7 個 callsite 都是 `cc`/D2+BYTE）、`CD2-ready-guard-source-contract.json` | A |
| 0xD3 | C_LITO_SET_FORCE | `cc`（陣營） | 已處理 | CS:1678；P:16675 | COV present | B |
| 0xE1 | C_LITO_SET_OBSERVER | `cc` | 已處理 | CS:1691；P:13753 | COV present | B |
| 0xE0 | C_LITO_CONFIRM_DEATH | `c` | 已處理 | CS:1690 → CS:26655；P:13771 | COV present | B |
| 0xD9 | C_LITO_QUIT_FIGHT | `c` | 已處理 | CS:1685 → CS:14902 | COV present | B |
| 0xD1 | C_LITO_GAME_INFO | `ccch`（房間設定：地圖／模式步進） | 已處理 | CS:1676；P:14351 | sub_464AD0、sub_46B7D0；脫殼:`CD1-map-outbound-contract.json`、`CD1-mode-step-contract.json` | A |
| 0x19 | C_THROW | — | 客戶端不送 | — | COV | B |
| 0x74 | C_DUEL | — | 客戶端不送 | — | COV | B |
| 0x78 | C_DEAD_RESTART | — | 客戶端不送（LiTo 用 C_LITO_CONFIRM_DEATH） | — | COV | B |

### 3.2 伺服器 → 客戶端
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0x28 → 0xD2 | S_WORLD → S_LITO_START_GAME | S_WORLD 必須先送（0x28 記錄 arena id，解析 dword_75A898；0xD2 立刻用它查 arena 表） | 已處理 | P:2209 `battleBootstrap` | dword_75A898 | B |
| 0xE9 | S_LITO_TIMELIMIT | `cd` | 已處理 | P:2223 | COV present | B |
| 0xE1 | S_LITO_COUNTDOWN | `cc` | 已處理 | CS:11702 | COV present | B |
| 0x23 | S_ATTACK | 物理：`c` + `cddccd`（動作、攻擊者、目標、命中標記 10／0、heading、序號 0 結尾）；遠程／法術：序號非 0 時再接 `hchhhhcs`（投射物 gfx、座標…） | 已處理 | P:9501、P:9551、P:9645、P:9721、P:9787 | sub_4A5ED0 | B |
| 0x39 | S_ATTACK_MANY | `ccdhhcdhccshc`（type 0 以施法者為中心、type 8 方向／目標中心） | 已處理 | P:9875 | COV present | B |
| 0x2E | S_ATTACK_ALL | `ccdhc` | 只診斷 | P:9597 | sub_4A63F0 | B |
| 0x20 | S_ACTION | `cdc` | 已處理 | P:15907 | sub_4A5A90 | B |
| 0x68 | S_HIT_RATIO | `cdc`（頭頂血條 0..100；0xFF 只能出現在 S_PUT_OBJECT 尾） | 已處理 | P:16051 | sub_4A3870 | B |
| 0x0D | S_HIT_POINT | `chh` | 已處理 | CS:12198 | COV present | B |
| 0xDB | S_LITO_POINT | `c s(名稱) d(積分) d(擊殺) d(死亡) d(助攻) c(陣營) c(陣營)` | 已處理 | CS:14571 | COV present | B |
| 0xE3 | S_LITO_CONFIRM_DEATH | `c`（無 body） | 已處理 | CS:80219 | COV present | B |
| 0xD3 | S_LITO_END_GAME | `cd`（**勝方陣營**，-1 = 平手；不是每個客戶端各自的勝敗 byte） | 已處理 | P:2255 | sub_4A9A60 | B |
| 0xF0 | S_LITO_QUIT_FIGHT | `cc` | 已處理 | CS:14934 等 | COV present | B |
| 0x11 | S_RESURRECT | `cdcdh`（目標、動作、來源、gfx） | 已處理 | P:16171 | sub_4A6F70 | B |
| 0xD4 | S_LITO_SET_FORCE | `csc` | 已處理 | P:2265 | COV present | B |
| 0xD1 | S_LITO_READY_GAME | `csc` | 已處理 | P:16609 | 脫殼:`CD2-ready-outbound-contract.json`（既有 AD368 frame） | A |
| 0xE2 | S_LITO_OBSERVER_BOX | `chhhh` | 已處理 | P:14785 | sub_48B640、sub_4AC5E0 | B |
| 0x32 | S_POISON | `cdcc`（0 正常、1 綠色中毒、2 灰色麻痺） | 已處理 | P:12145 | sub_4ACCB0 | B |
| 0x25 | S_PARALYSE | `cc`（signed mode） | 已處理 | P:12175 | COV present | B |
| 0x4A | S_SOUND_EFFECT | `cch` | 已處理 | P:12625 | sub_4AEE10 | B |
| 0xE4 | S_LITO_SET_GHOST | — | 客戶端不收 | — | COV | B |
| 0xDC／0xDD／0xDE | S_LITO_ROOM_INFO_MAP／OWNER／BATTLE | — | 客戶端不收（房間資訊改走 S_LITO_GAME_INFO 0xD5 子型別） | — | COV；脫殼:`D5-*-receive-contract.json` | A |

---

## 4. 技能

### 4.1 客戶端 → 伺服器
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0x14 | C_USE_SPELL | 共同 `ccc`（row、column；技能 id =(row<<3)+column+1，column ≤7、id ≤220）；尾巴依技能而定：<br>• 無尾（Shield）`ccc` P:8965，loc_4CBBD4<br>• 自身＋tick `cccd`（Invisibility skill60）P:9003，loc_4CBAAD<br>• 地面 `ccchh` P:9043<br>• 指定物件 `cccd`（治癒 1/19）P:9083<br>• 傳送 `ccchhh`（skill5）P:9125、集體傳送 P:9165<br>• 呼喚血盟 `cccs` P:9205、P:9259<br>• 召喚 `cccd` P:9311，loc_40A08B<br>• 攻擊法術 `cccdhh` P:9347，sub_4C9E40／sub_4CA0B0／sub_4CA6E0 | 已處理 | CS:1577 → CS:32172（`DragonSkillRequests.dispatch`）；P:8933 | 如左；`COV.verifiedPositiveContracts`（196 條，多數是技能逐項契約） | B |
| 0x49 | C_BUYABLE_SPELL | `cd(npc)` | 已處理 | CS:1631；P:7599 | sub_407C70 | B |
| 0x4A | C_BUY_SPELL | `chd` + d[] | 已處理 | CS:1632；P:7635 | sub_4D9BD0 | B |
| 0x52／0x53 | C_EXCHANGEABLE_SPELL／C_EXCHANGE_SPELL | `cd`／`chd` | 已處理 | CS:1634–1635；P:7721、P:7753 | sub_407C70、sub_4D9BD0 | B |
| 0x39 | C_SUMMON | `cscddd` | 已處理 | CS:1597；P:10245 | COV present | B |
| 0x4E | C_RETURN_SUMMON | `cc` | 已處理 | CS:1572；P:10557 | sub_4AF280、sub_4E2150 | B |
| 0x4F | C_SLAVE_CONTROL | `cdcd` | 已處理 | CS:1600；P:8679 | sub_40AFE0 | B |
| 0x5A | C_BLINK | `c`（傳送游標解鎖） | 已處理 | CS:1569；P:14549 | sub_4AFBA0 | B |
| 0x79 | C_START_CASTING | — | 客戶端不送 | — | COV | B |

### 4.2 伺服器 → 客戶端
| opcode | 名稱 | 欄位 | 狀態 | 伺服器位置 | 客戶端證據 | 等級 |
|---|---|---|---|---|---|---|
| 0x1E | S_ADD_SPELL | `cc(byte 數)` + 每 byte 8 個技能 bitmap（skill1 = byte0 bit0）；大廳技能窗與戰鬥共用，選角後就要送 | 已處理 | P:9433；CS:8901（選角）、CS:12070（開戰） | 脫殼:`S1E-initialization-gates.json`、`S1E-receive-selection-boundary.json`、`launcher-skill-initialization.json`、`class4-slot-provenance.json`；`server-packet-delta.json`（S1E 既知補證） | A |
| 0x1F | S_REMOVE_SPELL | 同 bitmap 格式 | 已處理 | P:9443；CS:8548（換角清除） | 同上 | A |
| 0x37 | S_EFFECT | `cdh`（物件 id、gfx） | 已處理 | P:16117 | sub_4ADDA0、sub_44EDE0 | B |
| 0x53 | S_EFFECT_LOC | `chhh`（x、y、gfx；IDA 誤認的 off_6344E8 其實是格式字串 "hhh"） | 已處理 | P:16089 | sub_4ADF60 | B |
| 0x23 | S_ATTACK（法術投射） | 見 §3.2 | 已處理 | P:9645 | sub_4A5ED0 | B |
| 0x39 | S_ATTACK_MANY（範圍） | 見 §3.2 | 已處理 | P:9875 | — | B |
| 0x29 | S_SPEED | `cdch`（0 正常、1 加速、2 緩速、秒數） | 已處理 | P:12585 | sub_4A7240 | B |
| 0x62 | S_EMOTION（勇敢藥水類） | `cdch` | 已處理 | P:12719 | sub_4AF9F0 | B |
| 0x34 | S_INVISIBLE | `cdc` | 已處理 | P:12289 | sub_4ACE30 | B |
| 0x6B／0x6C／0x6D | S_MAGE_STRENGTH／DEXTERITY／SHIELD | `chccc`／`chcc`／`chc` | 已處理 | P:10995、P:11027、P:11055 | sub_4AFE90、sub_4AFEE0、sub_4AFF30 | B |
| 0x56 | S_MAGIC_STATUS | `ccc`（SP、MR 修正） | 已處理 | P:10965 | sub_4AF320；脫殼:`status-family-50-51-52-56-contract.json` | A |
| 0x5D | S_NOT_ENOUGH_FOR_SPELL | `cd` | 已處理 | P:7829 | sub_4AF8E0 | B |
| 0x10 | S_MESSAGE_CODE | `chc`（例：1385 覺醒中不能用） | 已處理 | P:14599 | sub_4A4670 | B |
| 0x4E | S_BUYABLE_SPELL_LIST | `cdh` + … | 已處理 | P:7679 | sub_4AEC60 | B |
| 0x5C | S_EXCHANGEABLE_SPELL_LIST | `ch` + … | 已處理 | P:7797 | sub_4AF7E0 | B |
| 0x55 | S_REQUEST_SUMMON | `cccd` | 已處理 | P:10591 | sub_4AF280 | B |
| 0x57 | S_SLAVE_CONTROL | `cdc` | 已處理 | P:8717 | sub_4AF340 | B |
| 0x63 | S_BLINK | `c`（傳送游標鎖） | 已處理 | P:14533 | sub_4AFBA0 | B |

---

## 5. 協定風險（推測，待實機驗證）

- **R1：S_PACKET_BOX 與 S_LITO_CLAN 共用 0xFA。** `Protocol.java:501` 的 `S_PACKET_BOX = 0xFA` 和 `Protocol.java:1547` 的 `S_LITO_CLAN = 0xFA` 數值相同。`ClientSession.java:86378` 的註解自己寫明「Selected LiTo opcode 0xFA is S_LITO_CLAN, not the legacy PacketBox icon carrier」，Counter Magic／Absolute Barrier 的圖示也因此刻意不送；但 `ClientSession.java:21959 sendNativeMagicDollIcon` 仍會送 `S_PACKET_BOX(56|154, …)`。3.81C 客戶端會用 sub_4AC1C0（`P:17411` 的 S_LITO_CLAN `cddsd` 格式）解讀這個封包，結果可能是血盟資訊錯亂，或者被忽略。**待驗證**：召喚魔法娃娃時，觀察血盟 UI 與 log。建議開 code 任務改成靜默（比照 Counter Magic）。
- **R2：C_ENTER_WORLD（0x05）、C_LITO_CHANGE_OPTION（0xD8）、C_LITO_SERVER_LIST（0xF9）、C_LITO_LOGIN_NO_OTP（0xFA）** 伺服器有 handler，但 `COV` 的 `nativeProducer` 是 `not_found`。客戶端可能根本不送，屬於死碼或保留路徑；不影響功能。
- **R3：欄位格式等級 B 占大多數。** 本表格式取自伺服器 parser／writer 與註解所記的客戶端位址，本輪沒有逐一反組譯重新驗證。只有 S_STATUS、S_MANA_POINT、S50/51/52/56、S_ADD_SPELL、C_C9、S_CB、C_D2、C_D1、D5 系列有 `lito脫殼資源` 的獨立 raw-byte 契約（等級 A）。

---

## 6. 資源需求與存在性（PakViewer 全 pak 查核）

### 6.1 查核方法（已驗證）
- 工具：`D:\lineage開發\PakViewer-main\publish\win-x64\PakViewer.exe -cli list <idx>`（只用 list／export，未用 import），每個 idx 的清單輸出到 `scratch/t293/paklist/*.txt`（不進 git）。
- 比對腳本：`scratch/t293/resource_check.py`（只讀清單、`TW13081901.txt`、匯出的 `LTlist.spr`），結果在 `scratch/t293/resource-check.json`。
- Sprite 別名：客戶端 sprite 宣告表 `TW13081901.txt`（30000 筆宣告，其中 2594 筆是 `#id parts=base` 別名）與 LiTo `LTlist.spr`（285 筆）中，`=base` 表示沿用 base 的 .spr 檔。檢查時先解析別名，**沒有自己檔案的別名 id 不算缺檔**。
- 已查過的 pak（24 個 idx，共 303,038 筆成員；`launcher\TW13081901.pak` 沒有對應的 .idx，PakViewer 無法列出，未納入）：

| idx | 成員數 | 用途 |
|---|---|---|
| `Sprite.idx` | 92,356 | 共用 sprite／圖示（.spr／.tbt／.img／.png） |
| `Sprite00.idx`…`Sprite15.idx` | 10,184–10,972（16 個 bucket，合計 167,141） | 依 43FFC0 檔名雜湊 mod16 分 bucket 的 sprite |
| `Text.idx` | 37,498 | 文字表（spelldesc、ItemDesc、polymorph 等） |
| `Tile.idx` | 5,446 | 地圖 tile |
| `LiTo\LTicon.idx` | 227 | LiTo 技能圖示（226 個 .lbt + LTlist.ico） |
| `LiTo\LTsprite.idx` | 469 | LiTo 專屬 sprite（LTlist.spr id 0..284） |
| `LiTo\LTsurf.idx` | 1,623 | LiTo UI 面板（.img 1,613、.spr 10） |
| `LiTo\LTtext.idx` | 353 | LiTo 文字表（html 256、tbl 88） |
| `LiTo\LTtile.idx` | 1,210 | LiTo 地圖 tile |

### 6.2 各領域資源

| 領域 | 客戶端需要的資源 | 來源 pak／檔名 | 伺服器指向的編號 | 查核結果 | 等級 |
|---|---|---|---|---|---|
| 角色 | 職業基本外觀 sprite（S_PUT_OBJECT／S_POLYMORPH 的 gfx） | `Sprite*.idx`、`LTsprite.idx` 的 `<gfx>-<n>.spr` | L1 標準 0、1、61、48、138、37、734、1186、2786、2796、6658、6661、6671、6650 | 全部找到（王族／騎士／妖精同時在 LTsprite 與 Sprite） | 已驗證（存在）；職業↔gfx 對應是推測 |
| 角色 | 變身外觀 | 同上 | `src/main/resources/battle/polymorph-native-380.csv` 104 個 gfx | 104／104 解析得到檔案（含別名，例：#8859 bugbear morph = 152） | 已驗證 |
| 角色 | 選角畫面背景、肖像 | `LTsurf.idx` | 6314.img、7482.img（脫殼:`Native-background-scene-contract.json`） | 6314.img（LTsurf＋Sprite09）、7482.img（LTsurf）都在；79／80 號與肖像 PNG bank 對應仍未知（脫殼:`Native-selection-resource-gap-contract.json`） | 存在已驗證；綁定關係待驗證 |
| 道具 | 背包圖示（S_ADD_INVENTORY／S_LITO_BUY_LIST 的 `h icon`） | `Sprite.idx` 的 `<icon>.tbt`（另有同號 .img／.png） | 租借道具表 `rental-item-definitions-380.tsv` 的 invgfx 56 個；Java 註解另引 441、470 | 56／56 都在；441.tbt、470.tbt 都在 | 已驗證 |
| 道具 | 地面圖（S_PUT_OBJECT 地面物品的 gfx） | `Sprite*.idx` `<grdgfx>-<n>.spr` | 同一表的 grdgfx 35 個 | 35／35 都在 | 已驗證 |
| 道具 | 道具說明文字 | `Text.idx` `ItemDesc-c.tbl`；`LTtext.idx` `LTpitem-c.tbl`、`LTItemSet*-*` | `itemdesc_id`（例：142、790） | 表都在；逐筆 id 對應未查 | 存在已驗證；內容待驗證 |
| 戰鬥 | 攻擊／動作動畫（S_ATTACK／S_ACTION 的動作編號 → sprite 動作） | `Sprite*.idx`＋宣告表 | `spr-action-tw13081901.csv` 7,394 個 sprite id（伺服器的動作時長表） | 6,784 個有檔；268 個是 LiTo 命名空間（30000+id）→ 對到 `LTsprite.idx` 有 267 個，唯一缺 **LTlist #266「Petracring Flag C」**（伺服器 Java 沒有引用）；其餘 342 個在 TW 宣告表中有宣告但沒有 .spr 檔，Java 沒有直接引用這些 id | 已驗證（存在性）；342 筆是否會被送出待驗證 |
| 戰鬥 | 音效（S_SOUND_EFFECT `h`） | `0-客戶端\Sound\`（散檔 5,732 個，不在 pak 內） | 未逐一比對 | — | 待驗證 |
| 技能 | 技能／特效動畫（S_EFFECT、S_EFFECT_LOC、S_ATTACK 投射物的 gfx） | `Sprite*.idx` `.spr` | Java 原始碼與註解引用的 gfx 182 個（例：11722 治癒、167 光箭（TW 宣告名 energy bolt）、168 火牆、2231 生命之泉、5935 娃娃召喚） | 182／182 解析得到檔案（含別名，例：#871 cure poison → 192、#2510 → 149、#10701 counter magic → 2174、#6975/6976 → 6922）；Java 自動比對時 441／470 被標成缺 .spr，查證後是道具圖示 .tbt，屬誤報 | 已驗證 |
| 技能 | 技能圖示（技能窗） | `Sprite.idx` `<n>.tbt`＋`list.ico`（43F6C0 → list.ico / %d.tbt 路由）；LiTo 原生 `LTicon.idx` | 脫殼:`skill-initialization/shared-icon-resources.json` 149 個 | 149／149 仍在原 archive；`LTicon` 226 個 .lbt（0..232，**缺 173..179 共 7 個**，與 `閱讀入口.md` 記載一致）；`list.ico` 在 Sprite 與 Sprite05 | 已驗證 |
| 技能 | 技能說明／持續時間 | `LTtext.idx` `LTSpellDesc-c.tbl`（176 槽、147 筆非空、29 筆 NULL，脫殼:`skill-initialization/LTSpellDesc-c-indexed.json`）、`LTspelldur.tbl`；`Text.idx` `spelldesc-c.tbl`、`spelldur.tbl` | — | 4 個表都在 | 已驗證（存在） |
| 技能 | 技能面板框架 | `LTsurf.idx` 933.img（條件層）、934.img（主框 207×289）、935.img（rect 層）；fallback 圖示 `Sprite.idx` 91.tbt | 脫殼:`skill-pane-frame-coordinate-contract.json`、`skill-pane-layer-composition-contract.json`、`native-icon-draw-contract.json` | 全部都在（933/934/935 在 LTsurf 與 Sprite 各一份，以 LTsurf 為準） | 已驗證（存在）；GUI parity 待驗證 |

### 6.3 結論：沒有確認缺少的必要資源
伺服器實際引用的外觀、變身、道具圖示／地面圖、技能特效、技能圖示、說明表、面板框架，在上述 24 個 idx 中**全部找得到**（含別名解析）。只有以下兩項不是 100% 齊全，兩者都不影響目前伺服器功能：
1. `LTicon` 缺 173..179（原始客戶端就有的缺口，伺服器沒有引用）；
2. `LTlist.spr #266` 沒有 .spr 檔（伺服器沒有引用）。

### 6.4 證據漂移提醒（已驗證）
`shared-icon-resources.json`（2026-10-05）記錄的 `Sprite01/04/05/07/10.pak` 大小，與目前 `0-客戶端` 不同（例：Sprite01 420,402,578 → 422,714,941 bytes），`Sprite.pak` 則相同。149 個圖示**名稱**仍在原 archive，但當時記下的 row／offset 對這 5 個 pak 可能已經失效；之後要重新抽檔時，請以 PakViewer 現況為準。
