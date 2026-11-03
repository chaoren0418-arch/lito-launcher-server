# 0.6.63 資源資料庫精簡

2026-10-14 已完成實際遷移：`380_lito` 的 `lito_resource_*` 由 24 表／36,071 列縮為 **10 表／651 列**。其他既有 LiTo 表、來源角色庫、勝敗與結算收據不變。

## 正式表

| 完整表名 | 筆數 | 用途 |
| --- | ---: | --- |
| lito_resource_skill_value | 364 | 已確認的技能動作與呈現特效欄位 |
| lito_resource_buff | 13 | BUFF 封包對應，11 組採用、2 組衝突停用 |
| lito_resource_projectile | 1 | 實體箭矢資源與飛行封包類型 |
| lito_resource_map | 47 | 原生地圖目錄及名稱，不等於開放全部玩法 |
| lito_resource_zone | 2 | 區域範圍及色彩 |
| lito_resource_portal | 186 | 傳送資料，僅 56 條既有城鎮路徑接線 |
| lito_resource_npc_identity | 13 | 原生 NPC 身分名稱，不推定生成座標與 AI |
| lito_resource_map_page | 2 | 已確認的原生地圖頁面 |
| lito_resource_release | 1 | 資料資格化版本與架構備註 |
| lito_resource_pending | 22 | 統一待補、封存索引與衝突備註 |

前 9 表在啟動時讀取。pending 只驗證欄位結構，備註內容不作戰鬥規則。ResourceDatabase 明確驗證這 10 張表及統一備註 schema，不再依賴已移除的研究表。資料載入不可變記憶體快照，戰鬥中不新增資料庫查詢或寫入。

## 統一待補／備註表

- `category`：pending（6 項原待補問題）、archive（14 張移除表索引）、conflict（2 組 BUFF 衝突）。
- `source_table`、`record_key`：原表與資料項目；被封存的名稱是追溯標籤，不是現存外鍵。
- `evidence_status`、`evidence_note`：狀態與中文說明。
- `known_evidence`、`missing_evidence`、`required_evidence`：已知、缺少及如何補證。
- `archive_ref`：完整原資料的封存路徑。

```sql
SELECT category,source_table,record_key,evidence_status,evidence_note
FROM 380_lito.lito_resource_pending ORDER BY category,id;
```

14 張已移除表：source、provenance、skill、skill_phase、skill_request、map_item、map_effect、item_resource、npc_service、rental_offer、rental_mapping、rental_currency、config_reference、lineage_group（原本均有 lito_resource_ 前綴）。原始未知值、停用標記及逐欄來源未丟棄，完整保存在封存內。

## 封存及安全界線

封存：`backups/resource-database-before-0.6.63.zip`，SHA-256：
`ffa06a7c9a48f50d1c72f764382c8a4e4dfc5490cf52445f4cb4c89e8e157ed1`

包含原 24 表的 schema、逐列 JSON、各表內容 digest 及 restore.sql，共 36,071 列。匯出使用一致性唯讀快照；Python 獨立重算 digest，ZIP 解壓比對通過。刪表前再核對現場表與快照完全一致，才替換 pending 並移除精確列出的 14 表。未動玩家表。

restore.sql 是明確的歷史還原材料；請還原到空的研究庫查閱，不可直接覆蓋現行正式表。現行表仍保留的 source_id／原來源文字指向此封存證據，不要求已刪研究表重新出現。

## 程式、安裝及驗證

- 正式部署更新為 0.6.63；啟動 helper、JAR 雜湊及登入器同步。無客戶端 DLL 更換。
- `tools/resource-database/build.py` 繼續使用固定 0.6.61 基底，保留既有技能、房間與結算修正。
- 新安裝資料為 `db/lito_resource_compact.sql`，只有 10 表，不含 DROP／REPLACE。請先選定目的 LiTo 庫；既有安裝不要直接重匯。
- 原 24 表 ImportResources 已停用。prepare_import.py 改成驗證現行十表並輸出十表 SQL，不會重新建立研究表。
- `verify_import.py`：對照封存比對 9 張執行表的 629 筆資料（僅排除已改寫的備註文字），全部一致；核對十表集合、22 則備註與衝突停用。
- `test.py`：技能／BUFF／箭矢、NPC 名稱、傳送、測試房、人數規則、多人斷線結算通過。
- `resource-compact-063-smoke`：正式 SQL 載入、READY、端點與測試程序清理通過。登入器 pins 與正式輸入檢查通過。
- 遊戲內人工測試未執行；未宣稱新增未證實的玩法。

0.6.60 基準保留。0.6.62 程式回復材料在 `tools/resource-database/build/compact-063/deployment-before-063.zip`；不能只恢復 JAR 而忽略發布雜湊與資料庫版本。`runtime/battle-settlements` 不屬精簡範圍。
