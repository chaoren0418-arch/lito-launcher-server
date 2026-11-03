# 驗收現況

2026-10-23：正式 0.6.71 新增 L1PolyMe，沿用 0.6.70 核心；34 項變身與既有傳送回歸通過。[驗證範圍](docs/maintenance/gm-polyme-0.6.71.md)。以下保留各候選當時的驗收界線，不代表所有候選皆已發布。

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

## 保留的既有驗收限制

| 項目 | 限制與證據 |
| --- | --- |
| 斷線結算 | 冪等與清理已有測試；不代表長時間壓力／災難復原與雙玩家人工斷線情境全驗收。見[結算證據](tools/durable-settlement/evidence.json)。 |
| Ctrl 強制攻擊 | 封包與伺服器測試通過；雙視窗人工操作仍須核對。[0.6.67 記錄](docs/maintenance/forced-combat-0.6.67.md) |
| GM 座標／Storm_Walk | 權限、座標與協定測試通過；原生滑鼠點選魔杖仍有人工驗收限制。[0.6.68 記錄](docs/maintenance/gm-travel-0.6.68.md) |

發布前必須在**同一最終候選 SHA**完成整合回歸、正式 SQL 啟動、關閉清理及原生介面驗證。此次候選續作未改動正式部署或來源資料庫。

較早逐階段結果保留於[報告封存](docs/history/combat-reports-before-simplification-20261019.zip)及[歷史索引](docs/HISTORY.md)，不再重複貼入本頁。
