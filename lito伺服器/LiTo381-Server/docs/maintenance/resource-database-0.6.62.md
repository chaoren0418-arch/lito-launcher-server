# 資源資料庫接線 0.6.62

> 歷史紀錄：現行已由 [0.6.63](resource-database-0.6.63.md) 精簡為 10 表。以下 24 表與舊匯入工具不是現行架構；目前 build/import.sql 已改為十表。

2026-10-14：依使用者授權，已匯入實際 `380_lito`，並發布原生正式部署 0.6.62。新增 24 張 `lito_resource_*` 表、36,071 列。既有戰鬥規則、來源設定、玩家資料與結算收據未覆寫。

## 程式讀取範圍

| 資料表（省略 lito_resource_） | 已接線用途 | 界線 |
| --- | --- | --- |
| skill_value（364）／skill | 既有技能動作與呈現特效欄位 | 不改傷害、技能授予、目標規則；非完整魔法流程重寫 |
| buff | 11 組已確認封包 selector | 技能 3、151 有語義衝突，停用 SQL 覆寫，保留既有行為 |
| projectile | 實體箭矢 SPR 66、封包 flight type 127 | impact_gfx 未知，保持 NULL |
| map（47）／zone（2） | 地圖名稱、目錄與區域色彩 | 地形存在不等於已驗證玩法；gameplay_confirmed 保持 NULL |
| portal | 56 條已存在的城鎮傳送（500／560） | 其他 130 筆不接線；既有 slotName 與終點均核對，不猜測新路徑 |
| npc_identity（13） | LTdesc 身分名稱 | 不以資源索引當 actor GFX；原生成座標、HP、AI 等保持未知 |
| map_page | 原生頁面 1、2 的可用性 | 頁 3～18 尚缺原生頁圖對應，回覆可見提示；未建立物品 ID 跨版本對應 |

`ResourceDatabase` 在正式伺服器啟動時 SELECT 載入不可變快照；戰鬥執行緒不查詢這批新表。修改資料後需重啟。資源表沒有遊戲執行期寫回；玩家仍只有結算勝敗寫回政策。

`enabled=1` 的意義依資料類型：地圖／NPC 名稱為目錄可用，不代表新地圖或 NPC 玩法已開放。技能呈現的停用表示不採用該 SQL 覆寫值，不會移除既有技能。資源表不是任意開放未實作玩法的開關。

## 缺證與衝突如何查

原始 21 張表皆有 `evidence_status`、`evidence_note` 中文備註。`lito_resource_pending` 集中 6 組待補項，記錄已知證據、缺少資料及所需驗證。`source` 和 `provenance` 可追到來源檔與欄位位置。

以下保留停用／NULL：租賃物品及貨幣跨版本 ID、NPC 原生 actor／座標／AI、Layer8 視覺資料轉成生成資料、NPC 交易服務端語義、未證實傳送准入與原生選取分支。資源串接的命中特效不另送重複封包。34 個保留技能槽不授予。

日期化 config_reference 是歷史操作快照，不覆蓋現行設定。lineage_group、item_resource、skill_request 等可用於追溯，不宣稱已接入所有玩法。

```sql
SELECT * FROM 380_lito.lito_resource_pending;
SELECT skill_id, evidence_status, evidence_note
FROM 380_lito.lito_resource_buff WHERE enabled=0;
SELECT * FROM 380_lito.lito_resource_skill_value WHERE skill_id=1;
```

## 匯入與證據

- 原候選包 `db/candidates/resource-enrichment-0.6.60` 保持原樣。它的 SQL 產生器漏列 `sha256`，不能直接當正式匯入檔。此問題在 MySQL 暫存匯入被拒絕時發現，正式表尚未發布；修正工具從原始 JSON 按 schema 重新產生全部欄位。
- 正式匯入產物：`tools/resource-database/build/import.sql`。`ImportResources.java` 僅建立新暫存表，嚴格模式檢查、列數與缺證停用斷言通過後，一次 RENAME 發布。既存正式表會拒絕覆寫，不可重跑當更新器。
- 596 個來源已核對。Sprite09.idx／pak 容器有變更，但本次引用的 196 個資產逐一重讀後內容雜湊一致；保留原 sha256，另外保存 current_qualified_sha256，不宣稱整個容器未變。
- 候選資料取自 0.6.60；已與 0.6.61 正式核心比對，相關呈現 class 相同。release 表 qualified_version=0.6.61 是證據基底，不是目前登入器版本。

## 建置、驗證與回復

現行窄幅建置入口為 `tools/resource-database/build.py`，固定基底 `build/base-061.jar`。ASM 保留 0.6.61 房間修正及 0.6.60 結算流程，不從可能已分歧的完整原始碼直接覆蓋正式核心。

- `verify_import.py`：用原始 JSON 重新產生逐欄 digest，再以唯讀帳號比對 MySQL；21 張表共 35,700 列完整比對通過，含來源 SHA。
- `test.py`：實際 SQL 讀取、所有既有技能數值前後一致、NPC 名稱／傳送欄位一致、箭矢／BUFF 封包一致、SQL 快照確實被 consumer 使用；測試房規則與多人斷線結算通過。
- 新表合計：35,700 原資料＋364 執行欄位＋6 待補清單＋1 發布紀錄＝36,071 列。
- 原生正式 SQL 啟動 `resource-db-062-smoke`：READY、端點及測試程序清理均通過；stdout 明確記錄 364／11／47／56／13 的載入數量。啟動器 pins／正式輸入 gate 通過。
- 未宣稱已完成新版遊戲內逐技能與逐地圖人工驗收。此次沒有替換客戶端 DLL。

保留 0.6.60 基準 ZIP；本次精簡回復材料為 `build/base-061.jar` 與 `build/deployment-before-062.zip`。若回退程式，必須同步發布雜湊並重建登入器；新增資料表可保留，不必刪除。不可刪除 `runtime/battle-settlements`。
