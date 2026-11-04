# 歷史資源與維護索引

目前正式版本：**0.6.71**；封存遊戲基準：**0.6.66**。本頁提供現存資料的查找入口；舊文件內的版本、資料庫名稱與逐版 ZIP 路徑僅代表當時狀態，不可作為現行部署命令。

施工中候選的現行結果與未完成範圍見 [戰鬥技能主要進度表](maintenance/combat-continuation-current.zh-TW.md)；尚未部署，不改變本頁正式版本或封存基準。

最近發布：[0.6.71 L1PolyMe](maintenance/gm-polyme-0.6.71.md)；0.6.70 職業圖示發布收據位於相鄰登入器 docs/maintenance/release-0.6.70.json。

先前發布：[0.6.69 BUFF 相容與八角色分頁、驗收及精簡回復包](maintenance/buff-schema-and-character-pages-20261018.zh-TW.md)。

## 現存回復與歷史資料

| 位置 | 用途 |
| --- | --- |
| [戰鬥報告精簡前封存](history/combat-reports-before-simplification-20261019.zip) | 2026-10-19 三份原報告、原歷史索引與舊報告產生器；附原路徑及 SHA-256。現行進度以主要進度表為準 |
| 相鄰登入器 `backups/baseline-0.6.66.zip` | 兩個專案的封存遊戲基準；原始碼、發布檔、設定、固定建置輸入 |
| 相鄰登入器 `backups/0.6.26-before-shared-resources-20261011/` | 共用資源改造前特別保留的回復點 |
| `backups/resource-database-before-0.6.63.zip` | 原 24 張資源表的結構、資料與還原 SQL |
| `backups/history/server-development-evidence.zip` | 早期開發、設定、分析與驗收原文 |
| `backups/history/layout-before-20261012.zip` | 舊目錄、入口、設定、協定與主文件證據 |
| `backups/history/diagnostic-tools-20261012.zip` | 已退休的分析與診斷工具；需依原版本在隔離副本使用 |
| 相鄰登入器 `backups/history/through-0.6.60-evidence.zip`、`0.6.61-0.6.66-evidence.zip` | 舊版本獨有來源、文件與備份清單 |
| 登入器上層 `docs/history/retired-references-and-runs-20261014.zip` | 公開參考原始碼、完成的舊驗收與偵錯場次；原路徑索引、SHA-256 內容去重 |
| 登入器上層 `docs/history/maintenance-20261014.zip` | 本輪更新的規範、說明及維護工具補充包；不重寫遊戲基準 |
| `docs/history/readme-before-baseline-0.6.66.md` | 精簡前完整首頁與逐版敘述；歷史狀態不能覆蓋目前文件 |

逐版完整備份不再全部保留。0.6.35／0.6.60 舊完整 ZIP 及 `before-*.zip` 的獨有文字、來源與檔案清單已集中歷史包；現行回復請使用 0.6.66。封存資料可能含歷史連線設定，按本機備份管理，不公開上傳。

## 更新脈絡

| 階段 | 重點 |
| --- | --- |
| 0.6.26 | 保留共用資源改造前可運作的原生版本 |
| 0.6.27～0.6.35 | 共用 `0-客戶端`，技能與箭矢資源對應、道具說明及 GM 工具 |
| 0.6.36～0.6.42 | 先選服再登入、來源隔離、即時戰績與排行、單一設定、SQL 管理來源及 GM 權限 |
| 0.6.43～0.6.51 | 龍騎士／幻術師選角、技能指向、連擊與原生 BUFF 圖示 |
| 0.6.52～0.6.60 | 多開、房間同步、玩家移動／進出場與可重試結算 |
| 0.6.61～0.6.64 | SQL 測試房規則、原生資源接線、24 表精簡為 10 表、假人人數顯示 |
| 0.6.65～0.6.66 | 裝備切換狀態修正、移動校正回覆後釋放原生動作鎖定 |
| 2026-10-14 整理 | 0.6.66 聯合基準、分離固定輸入、精簡主文件、去重封存歷史來源及完成場次 |

個別修正的證據及限制請讀 `docs/maintenance/` 與 [VALIDATION.md](../VALIDATION.md)，本表不取代測試結果。

## 維護與回復

共同規範：[原生 LiTo 專案維護規範](../../../lito登入器/LiTo381-Launcher/docs/PROJECT-MAINTENANCE.zh-TW.md)。

登入器 `tools/project_hygiene.py find --match 關鍵字` 可找原路徑，`extract --match 關鍵字 --to 全新目錄` 可解壓指定資料。0.6.66 還原由登入器 `tools/restore_current.py` 驗證及解壓至全新目錄，禁止覆蓋結算 WAL 或 SQL 資料。

0.6.26 特別回復仍由 `tools/Restore-LegacyRuntime.py` 與登入器 `tools/restore_rollback26.py` 原配方管理。它是歷史相容性回復，不是目前啟動入口。

## VALIDATION.md 舊段落（原文搬移，未改寫）

以下各段自 `VALIDATION.md` 原文搬出，原文內的相對連結保留未改寫；搬移後須由下方補充索引開啟證據。

### 搬移段落的證據連結補充索引

下列連結以本頁所在的 `LiTo381-Server/docs/` 為基準，依原文出現順序對應 9 個證據入口。

| 原文段落／連結 | 可點擊的證據入口 |
| --- | --- |
| 跨角色材料技能／驗證與限制 | [跨角色材料技能驗證與限制](maintenance/native-pair-material-progress.zh-TW.md) |
| 隱身施法外控／證據與限制 | [隱身施法外控證據與限制](maintenance/native-invisibility-cast-policy-progress.zh-TW.md) |
| 三重矢倍率外控／證據與限制 | [三重矢倍率外控證據與限制](maintenance/native-triple-arrow-policy-progress.zh-TW.md) |
| 奪命之雷傳送外控／證據與限制 | [奪命之雷傳送外控證據與限制](maintenance/native-thunder-teleport-policy-progress.zh-TW.md) |
| 堅固防護外控／證據與限制 | [堅固防護外控證據與限制](maintenance/native-solid-carriage-policy-progress.zh-TW.md) |
| 技能73材料／證據與限制 | [技能73材料證據與限制](maintenance/native-magical-weapon-material-progress.zh-TW.md) |
| 2026-10-20 更新／主要進度表 | [戰鬥技能主要進度表](maintenance/combat-continuation-current.zh-TW.md) |
| 已發布／發布與驗收記錄 | [BUFF 相容與八角色分頁發布與驗收記錄](maintenance/buff-schema-and-character-pages-20261018.zh-TW.md) |
| 候選／候選索引 | [候選索引](../tools/combat-continuation/continuation-current.json) |

### 搬移原文

跨角色材料技能（2026-10-20，隔離候選）：技能68通過216案施放、72案Ctrl／成對鎖定／退場與IO邊界、Main10案，技能73 Main8案與1068案既有材料／Light回歸通過。材料邊界25／58，其餘33個及完整Source／範圍語意仍開放；競技與資料安全限制保留，未部署。詳見[驗證與限制](docs/maintenance/native-pair-material-progress.zh-TW.md)。

隱身施法外控（2026-10-20，隔離候選）：INVIS_CAST_BREAK接入共同施法及60／97重施判斷；448／448案的64個差異歸零，9例解析與兩次重建通過。斗篷匯入、雙隱身lease、關閉timer重排程及其餘34材料技能仍開放，未部署。詳見[證據與限制](docs/maintenance/native-invisibility-cast-policy-progress.zh-TW.md)。

三重矢倍率外控（2026-10-20，隔離候選）：Triple_Arrow_Dmg接入三條原生producer；108／108案的36個偏差歸零，150例數值、14例解析、例外後一般攻擊及兩次凍結重建通過。其餘34材料技能及隱身外控仍開放，未部署。詳見[證據與限制](docs/maintenance/native-triple-arrow-policy-progress.zh-TW.md)。

奪命之雷傳送外控（2026-10-20，隔離候選）：技能5／131接入SLAY_BREAK_NOT_TELEPORT，256／256案的32次偏差歸零；原其他麻痺與移動限制保留，9例Source解析及兩次凍結重建通過。材料技能仍24個、其餘34個與隱身／三重矢外控尚未完成，未部署。詳見[證據與限制](docs/maintenance/native-thunder-teleport-policy-progress.zh-TW.md)。

堅固防護外控（2026-10-20，隔離候選）：SOLID_CARRIAGE_MODE接入裝備資格與移除檢查；160／160案的24次模式偏差歸零，11例Source解析與兩次凍結重建通過，原mode=1封包一致。此收據限裝備模式，native臂甲匯入、完整技能90與其餘34材料技能／Source外控仍開放，未部署。詳見[證據與限制](docs/maintenance/native-solid-carriage-policy-progress.zh-TW.md)。

技能73材料（2026-10-20，隔離候選）：材料與物品強化同筆提交，GM免材料仍提交效果；實際Main正常／中斷8例、STOPPED16例與角色／場景／送包故障32例通過。修正政策等待中角色切換的舊批次輸出；同SHA 34組driver、完整20／生命週期14／控制10、兩次凍結重建與2177項進度稽核通過。累計24材料技能，其餘34與完整Source外控仍開放，未部署。詳見[證據與限制](docs/maintenance/native-magical-weapon-material-progress.zh-TW.md)。

更新：2026-10-20。正式 **0.6.69**；戰鬥同源化仍為候選。完整進度與待辦集中在[主要進度表](docs/maintenance/combat-continuation-current.zh-TW.md)，本頁只列驗收界線。

## 已發布

0.6.69 的 BUFF 新單表／舊快照唯讀相容、八角色分頁已通過啟動與關閉檢查；使用者確認角色顯示、翻頁、進大廳、返回角色選單正常。詳見[發布與驗收記錄](docs/maintenance/buff-schema-and-character-pages-20261018.zh-TW.md)。

## 候選，尚未發布

| 候選 | 已有證據 | 界線 |
| --- | --- | --- |
| `cancellation-static-runtime`／`743b35d1…` | 靜態 NPC 副本／角色場景／清理、材料故障及既有 BATTLE 重播共 684 案／15,639 檢查；36 案故障均無部分提交，重建一致 | 僅增量專項通過；AI／TAMED Source 差異與最終完整回歸仍待完成。未發布、未啟動介面 |
| `npc-physical-size`／`9636aaa6…` | 4,138 筆 NPC 模板；5,164 案組合與 20＋18 組回歸通過 | 前層候選，不能替代後續版本驗收 |
| `cancellation-material`／`08b49d26…` | 玩家相消 96 案、8 案故障、56 案生命週期 | 不含成功機率或完整 NPC 轉換 |
| `cancellation-speed-immunity`／`345640f9…` | 240 案，208 案材料／故障等重播 | 僅列明的速度狀態 |
| `cancellation-debuff-immunity`／`9eb812b8…` | 240 案，208 案重播及 240 案速度免疫重播 | 僅 47／56／71／173／174 狀態 |
| `weight-material`／`f2217019…` | 56 案自訂材料、8 案故障、40 案生命週期；重建一致 | 尚缺此 SHA 的完整整合、SQL／關閉及原生介面驗收 |
| `cancellation-probability`／`2ae86db7…` | 176 案封包、8 案故障、112 案生命週期、103,063 項公式比對；5,164 案組合及 20＋18 組回歸通過 | 同 SHA 候選驗證；最終 SQL／關閉／原生介面仍未驗收 |
| NPC 轉換分層／`792e12b5…`、`f6d98acd…`、`51ad61a6…` | 15 案唯讀快照、9 案來源順序；靜態外觀 120＋12＋84 案；替換底層 24 案 | 早期分層；type2 後續成果見主要進度表，副本隔離及完整工廠仍待補 |
| `cancellation-npc-material`／`94d971cc…` | WORLD 怪物 32 案、PET／SUMMON 64 案、故障 4 案、NPC 生命週期 28 案；重建一致 | 未完成此 SHA 的整合回歸；其他場景、毒性與召喚物競態仍待驗證 |

| `area-sleep-slow`／`8d44fac4…` | 四技能與 NPC 替換同 SHA 共 912 案專項；重建一致 | 69／165、完整組合及原生介面未驗收 |
| `combat-config-origin`／`aae91ba8…` | 912 案專項、5,164 案與 20＋18 組回歸、外控路徑 20 項及 SQL 啟動／所屬程序清理通過 | 原生介面由使用者指定集中測試；未發布 |
| NPC／範圍整合候選／`8d26c9d7…` | 同 SHA：2,400 案專項／45,156 檢查、5,164 案組合、20＋18 組回歸、SQL 啟動／所屬程序清理與重建一致通過；另 24 案延遲送包通過 | 原生介面依使用者安排集中實測；普通 NPC 替換已補，靜態 NPC／特殊核心等餘項見進度表，正式版未替換 |

完整路徑、SHA 與測試收據見[候選索引](tools/combat-continuation/continuation-current.json)。本輪相消機率、WORLD／BATTLE 普通 NPC 替換及四項範圍技能、69／165 已納入同 SHA 自動驗證；其餘場景缺口保留於主要進度表，不能寫成全技能完成。
