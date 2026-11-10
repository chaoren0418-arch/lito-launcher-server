# Issue #302：T104 多來源資料庫規格、完整權限清單與中文欄位對照

**結論**：權限清單改成由程式掃描產生，共 **98 張表**（380_db 54、380_lito 19、每個來源角色庫 25），比 #83 的 80 張多 18 張。多出來的表裡有 9 張在 `ResourceDatabase.load` 啟動鏈上，G01 的 `角色_智力成長` 是其中第一張被讀到的，其餘 8 張在 #83 的授權下也會讓啟動失敗。GRANT 範本、`lito_source_server` 權威說明、MyISAM 結算分析、weapon／armor 中英欄位對照都寫在 `docs/design/database.md`。另外發現：**1-伺服器 現行 380_db 的 weapon／armor 與 `裝備_*` 表已改成中文欄位，LiTo 讀的英文欄位在那個庫都不存在**，LiTo 不能直接指向它。

## 做了什麼

1. `tools/db-grants/scan_db_access.py`：解析 `src/main` 947 個 Java 檔的字串字面值（含 `\uXXXX`、text block、`%s`／`.formatted()`），擷取表、讀寫、欄位；用 `db/*.sql` 傾印當表名白名單，補抓 `checkTable(..., "accounts")` 這類只傳表名的寫法；從 `SharedResourceMain` 做類別可達性。產出 860 筆存取紀錄。
2. `tools/db-grants/review.json`（人工核對，每項附行號）＋ `gen_grants.py`：排除 Modern*、`lito_mysql` 限定區段與 16 張擴充表、只是字串相同的誤判；寫入只放行 `character_lito`、`lito_battle_settlement`；產生 `grants.sql`、`table-inventory.tsv`、`excluded.tsv`，並把清單與範本寫進設計文件的 GENERATED 區塊。
3. `tools/db-grants/jar_tables.py`：解析正式 0.6.71 JAR（SHA-256 `E1498871…D498`）的 class 常數池做交叉檢查。抓到 src/main 看不到的 `道具_物品詳細說明`（正式 JAR 的 `ClientSession` 才呼叫 `LiToItemTemplates`），也讓我修正掃描器漏掉的 `resourceDatabase + ".\`skills_魔屏無效技能\`"` 寫法。修正後，JAR 多出的 20 個表名都已經解釋清楚。
4. `docs/design/database.md`：三類庫與連線方式、清單產生流程、與 #83 的差異表、表與欄位清單、GRANT 範本、刻意不授權的部分、`lito_source_server` 規格與啟用步驟、`character_lito` 在 MyISAM 下的正確性分析與建議、weapon／armor 對照表（用 setter 名稱對齊 LiTo `NativeDeItemTable` 與 1-伺服器 `ItemTable` 產生）。
5. 修正不一致（以 SQL 為準）：`lito伺服器/LiTo381-Server/README.zh-TW.md:12` 原本寫「公牛、天使啟用、水蛇停用」，改成種子實際內容（太陽神／月亮神／伺服器3），並寫明「實際部署值由營運設定，以現場表為準」；`db/lito_source_server.sql` 加兩行註解，說明 INSERT 是初始種子，並指向設計文件。

## 怎麼驗證

- 掃描可重跑且結果固定：連跑兩次，`db-access.json` 的 SHA-256 相同。
- 和 #83 已實測的 80 表授權逐表比對：80 張全部包含在內，另外多出 18 張，每張都附讀取點（設計文件 §2）。
- 正式 JAR 常數池交叉檢查：JAR 引用 118 個白名單表名，扣掉清單的 98 張，剩下 20 張都能解釋（16 張 `lito_mysql` 擴充表、`character_buff_snapshot` 條件分支、`server_info` 只有寫入、`exp`／`petitem` 是欄位名或字串）。清單裡沒有任何一張在 JAR 裡找不到。
- 和 1-伺服器 現行傾印唯讀比對（`gen_grants.py --ref-schema`）：380_lito 19 表的欄位都存在；380_ac 只缺 `server_紀錄_獎勵` 一張表（範本已改成選用並註解），`character_buff.snapshot_version` 沒有這欄（所以不授權 `character_buff_snapshot`）；380_db 的 weapon（73 欄）、armor（78 欄）的英文欄位全部不存在，另有 17 張 `裝備_*` 表也缺 LiTo 讀的英文欄位。
- weapon／armor 對照表：1-伺服器 `ItemTable.java:405-506,537-640` 與 LiTo `NativeDeItemTable.java:160-260,284-390` 依 setter 名稱逐行配對；`classname`、`set_id`、`item_id` 透過區域變數讀取，是人工補上的。
- MyISAM 分析是逐行讀 `SettlementSql.java`、`SharedDatabase.java`、`DurableBattleSettlement.java`、`SettlementJournal.java` 得出；鎖名是 64 字元的 SHA-256 hex，正好等於 MySQL `GET_LOCK` 的名稱上限。
- 沒改 Java。`mvn -B test` 依本機限制不在本機跑，交給 PR 的 CI。

## 已驗證／推測／待驗證

**已驗證**
- 98 表清單、讀寫分類、啟動即讀標記，以及和 #83、正式 JAR 的比對結果。
- 寫入只發生在 `character_lito`、`lito_battle_settlement`；其餘 20 張表的寫入程式都在唯讀／`lito_mysql` 守門後面（`Accounts.java:4234,4842,4897,…`）。
- `lito_source_server` 結構、檢查規則、種子值；README 與 SQL 現在一致。
- 1-伺服器 現行 380_db 改成中文欄位、380_ac 沒有 `server_紀錄_獎勵`（以傾印檔為準）。
- MyISAM 下的寫回邏輯：單列單敘述＋CAS（version＋last_result_id）＋InnoDB 收據，重試、回覆遺失、標記失敗都不會重複加。

**推測**
- 補齊清單的權限後正式 JAR 能啟動到 ready：#284 只走到第 4 個載入器，後面沒有實際跑過。
- 並發 drain、外部改表、MyISAM 表鎖等待時的行為；MyISAM 斷電只會少算、不會多算。
- `lito_mysql` 擴充表的私有輔助方法只會被守門後的呼叫端用到：沒有逐一追完呼叫端，依據是 healthCheck 的分區和 #83 的運作證據。

**待驗證**
- 在實驗庫用範本建帳號：所有表 `SELECT … LIMIT 0` 都要通過，寫入測試要被拒，再用正式 JAR 啟動到 `[LiTo] ready`。
- 現場 `lito_source_server` 目前啟用了哪些來源。
- 現場的 380_db 是英文欄位版，還是 1-伺服器 的中文欄位版。

## 產出路徑

- `docs/design/database.md`
- `tools/db-grants/{README.md, scan_db_access.py, gen_grants.py, jar_tables.py, review.json}`
- `tools/db-grants/generated/{db-access.json, db-access.tsv, table-inventory.tsv, excluded.tsv, grants.sql}`
- 修改：`lito伺服器/LiTo381-Server/README.zh-TW.md`、`lito伺服器/LiTo381-Server/db/lito_source_server.sql`（只加註解）

## 給下一棒的提示

1. **#284／#285 重跑前**（G01）：用 `tools/db-grants/generated/grants.sql` 補齊實驗帳號 `lito83_exp` 的權限。相對 #83 要補 380_db 15 張、380_ac 3 張；`server_紀錄_獎勵` 只在表存在時才授權。交給 Codex／本機測試任務（stage: test，runner: codex-local）。
2. **中文欄位版 380_db**：0.6.71 只支援英文欄位。如果現場或客戶的庫是 1-伺服器 的中文欄位版，就要另開設計任務做欄名對應層，可以直接用 §8 的對照表當規格。這件事會影響部署，建議標 `needs:user` 確認客戶實際用的是哪一版。
3. **`character_lito` 改 InnoDB**：建議改，但要由營運決定（現場 DDL），可放進 T104 後續的部署清單。
4. 伺服器 SQL 有改動時，重跑 `scan_db_access.py` 和 `gen_grants.py --update-doc`。之後可以考慮在 CI 加一步「重產後 `git diff --exit-code tools/db-grants/generated`」（這次沒做，不在任務範圍）。
5. 本次範圍外但看到的：`lito登入器/LiTo381-Launcher/tools/battle-flow/verify_pair.py:14` 等舊驗證腳本還寫死「公牛／天使」，屬歷史測試資料，沒改。
