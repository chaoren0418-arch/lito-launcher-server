# Issue #283：T013 實驗環境驗收表

## 結論／做了什麼

已將 `docs/acceptance/CHECKLIST.md` 改為「正式 0.6.71＋P24 實驗 schema」的待執行範本，操作者為 dot，使用者觀看驗收。本次只編修文件及唯讀核對，不代表實機驗收通過。

- 固定正式 JAR／登入器來源並記錄實際 SHA-256；JAR 由後續執行者複製到本次 worktree 的 `scratch/t014/`，核對副本，不覆蓋原件。
- 明訂 `p24lab` lease、loopback 位址、42083 主埠與 `42083 + source_id` 來源埠、43324 JDBC 及三個 P24 實驗 schema；私密帳密只由 #83 交接，不放入文件。
- 以明確帶入實驗設定的 `java -jar` 啟動流程取代不存在的預檢／短啟動選項，保存 stdout／stderr 到啟動完成並持續採證。
- 21 個步驟皆有預期、證據及結果欄；近戰技能、魔法技能、`.polyme` 各要求獨立截圖，補入 GM 魔杖滑鼠點選與兩種 DLL 入口名稱唯讀核對。
- SQL 改為完整限定的實驗庫名稱，戰績欄位修正為 `wins`／`losses`，按角色比對收據；保留斷線結算、職業圖示、Ctrl 強制攻擊及正常退出。
- 增加逐項「現象／步驟／證據」失敗清單供 M2 盤點；非阻斷失敗繼續執行，其餘明列阻塞，不把前置不足當通過。

## 已驗證／怎麼驗證

2026-11-06，於既有 `task/283` worktree 執行：

1. 已讀根目錄 `AGENTS.md`、`docs/REPORT.md`、`TASKS.md` 的 M1／T013，以及 `docs/reports/issue-83.md`。依本 Issue 明確指定的正式成品與 dot 操作方式撰寫，未變更其他治理文件。
2. 唯讀 `Get-FileHash -Algorithm SHA256` 核對原始專案指定成品，結果如下；本次未複製、修改或啟動成品。

   | 成品 | 大小（bytes） | SHA-256 |
   |---|---|---|
   | `lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar` | 20054975 | `E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498` |
   | `lito登入器/LiTo381-Launcher/release/current/LiTo381Launcher.exe` | 132096 | `0BF5DDF2E479EF0B38F3416FF47CFF981AF5A6F4B5095719E24DC3BA9F624280` |

3. PowerShell 靜態檢查：CHECKLIST 中六個禁止字串 `config/server.properties`、`380_db`、`380_lito`、`3306`、`--check`、`--smoke` 全部為零；16 個必要設定／驗收項存在；01–21 連號且每步預期、證據非空；三個 SELECT 的 FROM 只引用 `p24_accounts_lito_exp`／`p24_lito_lito_exp`。
4. 讀取既有 `UnifiedConfiguration.java`、`SettlementSql.java`、`DurableBattleSettlement.java`、`SettlementJournal.java`，核對統一格式鍵名、來源埠計算、戰績／收據欄位與 WAL 相對路徑。設定安排於 `scratch/t014/settings/experiment.properties`，避免結算 WAL 因取設定檔上兩層目錄而落到 `scratch/t014/` 外；關閉時區分鎖釋放與 `owner.lock` 檔案存在。
5. `git diff --check` 與變更範圍檢查通過；僅修改驗收表、新增本報告。未變更程式碼，因此未執行建置或程式測試。

## 推測

- 依 #83 報告，兩組帳號只有普通權限且各一角色；GM 指令、GM 魔杖及八角色翻頁可能缺少前置，已在表內明列條件與阻塞處理。未讀私密設定或連 DB，不能宣稱目前狀態仍與 #83 完全相同。
- 程式碼閱讀可支持設定路徑與查詢欄位的文件安排，但不等於已驗證指定正式 JAR 在 P24Lab 能完整啟動、登入或結算。

## 待驗證／未驗證什麼

- 全部實機步驟、P24Lab 現況與 lease、正式 JAR 副本啟動、實際監聽與來源路由、登入器隔離配套、角色技能／GM 前置、DLL 實際載入及 SQL 前後差異，留待 dot 的後續實機任務驗證。
- 本輪未取得 lease、未啟停 DB／Java／登入器／遊戲、未連任何資料庫、未讀機密設定、未接觸封存／備份或修改硬連結；未產出實機截圖或填寫通過結果。

## 產出路徑

- [實驗環境 CHECKLIST](../acceptance/CHECKLIST.md)
- 本報告：`docs/reports/issue-283.md`

## 給下一棒的提示

1. 調度器／Claude 審查本次文件；合併後由調度器更新回報索引，本輪不改 `docs/REPORT.md`。
2. 由調度器安排 dot 實機回歸及 `p24lab` lease。dot 在自己的執行 worktree 準備 `scratch/t014/`、正式成品副本與 #83 私密設定；不要在原始專案寫檔，不以任意 Maven 成品取代正式基準。
3. #83 並未建立 GM／八角色前置，也未驗證遊戲技能。先記錄缺口，交調度器另派實驗資料準備；不得自行提升權限、擴充 DB 授權或修改正式資料來消除阻塞。
4. dot 依表採證、使用者觀看驗收，回填 `docs/acceptance/0.6.71.md`；每個異常一列保留「現象／步驟／證據」，供 M2 盤點。保留實驗資料與秘密交接，不把關機當作可刪庫、撤權或刪憑證的授權。
